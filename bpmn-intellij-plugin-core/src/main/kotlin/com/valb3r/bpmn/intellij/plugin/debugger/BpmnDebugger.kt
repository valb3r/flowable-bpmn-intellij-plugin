package com.valb3r.bpmn.intellij.plugin.debugger

import com.intellij.database.dataSource.connection.DGDepartment
import com.intellij.database.psi.DbElement
import com.intellij.database.remote.jdbc.RemoteConnection
import com.intellij.database.util.DbImplUtil
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.BpmnElementId
import java.sql.Connection
import java.time.Duration
import java.time.Instant
import java.util.concurrent.ForkJoinPool
import java.util.concurrent.atomic.AtomicReference


private val bpmnDebugger = AtomicReference<BpmnDebugger>()

fun prepareDebugger(schema: DbElement): BpmnDebugger {
    val debugger = IntelliJBpmnDebugger(schema)
    bpmnDebugger.set(debugger)
    return debugger
}

fun detachDebugger() {
    bpmnDebugger.set(null)
}


fun currentDebugger(): BpmnDebugger? {
    return bpmnDebugger.get()
}

interface BpmnDebugger {
    fun executionSequence(processId: String): ExecutedElements?
}

// FIXME - Extract
class IntelliJBpmnDebugger(private val schema: DbElement): BpmnDebugger {

    private val cacheTTL = Duration.ofSeconds(1)
    private val worker: ForkJoinPool = ForkJoinPool(1)

    private var cachedResult: AtomicReference<ExecutedElements?> = AtomicReference()
    private var cachedAtTime: AtomicReference<Instant?> = AtomicReference()

    override fun executionSequence(processId: String): ExecutedElements? {
        val cachedExpiry = cachedAtTime.get()?.plus(cacheTTL)
        if (cachedExpiry?.isAfter(Instant.now()) == true) {
            return cachedResult.get()
        }

        worker.submit {
            cachedResult.set(fetchFromDb(processId))
            cachedAtTime.set(Instant.now())
        }

        return cachedResult.get()
    }

    private fun fetchFromDb(processId: String): ExecutedElements? {
        try {
            val connProvider = DbImplUtil.getDatabaseConnection(schema, DGDepartment.INTROSPECTION)?.get()
            // Old IntelliJ provides only getJdbcConnection
            try {
                val jdbcSupplier = connProvider?.javaClass?.getMethod("getJdbcConnection")
                if (true != jdbcSupplier?.isAccessible) {
                    jdbcSupplier?.isAccessible = true
                }
                (jdbcSupplier?.invoke(connProvider) as Connection?)?.use {
                    return readExecutionIds { stmt -> listIds(processId, stmt, it) }
                }
            } catch (ex: NoSuchMethodException) {
                // New IntelliJ provides only getRemoteConnection
                val connSupplier = connProvider?.javaClass?.getMethod("getRemoteConnection")
                if (true != connSupplier?.isAccessible) {
                    connSupplier?.isAccessible = true
                }
                val remoteConn = (connSupplier?.invoke(connProvider) as RemoteConnection?)
                try {
                    remoteConn?.let {return readExecutionIds { stmt -> listIds(processId, stmt, it) }}
                } finally {
                    remoteConn?.close()
                }
            }
        } catch (ex: RuntimeException) {
            bpmnDebugger.set(null)
        }

        return null
    }

    private fun readExecutionIds(idsFetch: (statement: String) -> List<String>): ExecutedElements? {
        val ruIds = idsFetch(statementForRuntimeSelection(schema.name))
        if (ruIds.isNotEmpty()) {
            return ExecutedElements(ruIds.map { BpmnElementId(it) })
        }

        val hiIds = idsFetch(statementForHistoricalSelection(schema.name))
        if (hiIds.isNotEmpty()) {
            return ExecutedElements(hiIds.map { BpmnElementId(it) })
        }

        return null
    }

    private fun listIds(processId: String, statement: String, conn: Connection): List<String> {
        val ruQuery = conn.prepareStatement(statement)
        ruQuery.setString(1, processId)
        val result = ruQuery.executeQuery()
        return result.use {generateSequence { if (result.next()) result.getString(1) else null }.toList()}
    }

    private fun listIds(processId: String, statement: String, conn: RemoteConnection): List<String> {
        val ruQuery = conn.prepareStatement(statement)
        ruQuery.setString(1, processId)
        val result = ruQuery.executeQuery()
        return generateSequence { if (result.next()) result.getString(1) else null }.toList()
    }

    private fun statementForRuntimeSelection(schema: String): String {
        return """
                SELECT re.act_id_ FROM ${"$schema."}act_ru_actinst re JOIN ${"$schema."}act_re_procdef def ON re.proc_def_id_ = def.id_
                WHERE re.proc_inst_id_ = (
                    SELECT re.proc_inst_id_ FROM ${"$schema."}act_ru_actinst re JOIN ${"$schema."}act_re_procdef def ON re.proc_def_id_ = def.id_
                    WHERE re.start_time_ =
                          (
                              SELECT MAX(re.start_time_) FROM ${"$schema."}act_ru_actinst re JOIN ${"$schema."}act_re_procdef def ON re.proc_def_id_ = def.id_ WHERE def.key_ = ?
                          )
                    LIMIT 1
                ) ORDER BY re.start_time_, re.id_

            """.trimIndent()
    }

    private fun statementForHistoricalSelection(schema: String): String {
        return """
                SELECT re.act_id_ FROM ${"$schema."}act_hi_actinst re JOIN ${"$schema."}act_re_procdef def ON re.proc_def_id_ = def.id_
                WHERE re.proc_inst_id_ = (
                    SELECT re.proc_inst_id_ FROM ${"$schema."}act_hi_actinst re JOIN ${"$schema."}act_re_procdef def ON re.proc_def_id_ = def.id_
                    WHERE re.start_time_ =
                          (
                              SELECT MAX(re.start_time_) FROM ${"$schema."}act_hi_actinst re JOIN ${"$schema."}act_re_procdef def ON re.proc_def_id_ = def.id_ WHERE def.key_ = ?
                          )
                    LIMIT 1
                ) ORDER BY re.start_time_, re.id_

            """.trimIndent()
    }
}

data class ExecutedElements(val history: List<BpmnElementId>)