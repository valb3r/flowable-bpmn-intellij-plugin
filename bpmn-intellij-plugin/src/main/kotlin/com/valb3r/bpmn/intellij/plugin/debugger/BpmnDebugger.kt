package com.valb3r.bpmn.intellij.plugin.debugger

import com.intellij.database.dataSource.connection.DGDepartment
import com.intellij.database.psi.DbElement
import com.intellij.database.util.DbImplUtil
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.BpmnElementId
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import java.sql.Connection
import java.time.Duration
import java.time.Instant
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

class IntelliJBpmnDebugger(private val schema: DbElement): BpmnDebugger {

    private val cacheTTL = Duration.ofSeconds(1)

    private var cachedResult: ExecutedElements? = null
    private var cachedAtTime: Instant? = null
    private var result: Deferred<ExecutedElements?>? = null

    override fun executionSequence(processId: String): ExecutedElements? {
        val cachedExpiry = cachedAtTime?.plus(cacheTTL)
        if (cachedExpiry?.isAfter(Instant.now()) == true) {
            return cachedResult
        }

        if (true == result?.isCompleted) {
            cachedResult = result!!.getCompleted()
            cachedAtTime = Instant.now()
            result = null
            return cachedResult
        }

        if (true == result?.isActive) {
            return cachedResult
        }

        result?.cancel()
        result = GlobalScope.async {
            try {
                val conn = DbImplUtil.getDatabaseConnection(schema, DGDepartment.INTROSPECTION)?.get()
                conn?.jdbcConnection?.use { jdbcConn ->
                    val ruIds = listIds(processId, statementForRuntimeSelection(schema.name), jdbcConn)
                    if (ruIds.isNotEmpty()) {
                        return@async ExecutedElements(ruIds.map { BpmnElementId(it) })
                    }

                    val hiIds = listIds(processId, statementForHistoricalSelection(schema.name), jdbcConn)
                    if (hiIds.isNotEmpty()) {
                        return@async ExecutedElements(hiIds.map { BpmnElementId(it) })
                    }

                    return@async null
                }
            } catch (ex: Exception) {
                bpmnDebugger.set(null)
            }
            return@async null
        }
        return cachedResult
    }

    private fun listIds(processId: String, statement: String, conn: Connection): List<String> {
        val ruQuery = conn.prepareStatement(statement)
        ruQuery.setString(1, processId)
        val result = ruQuery.executeQuery()
        return result.use { generateSequence { if (result.next()) result.getString(1) else null }.toList() }
    }

    private fun statementForRuntimeSelection(schema: String): String {
        return """
                SELECT re.act_id_ FROM ${"$schema."}act_ru_actinst re JOIN ${"$schema."}act_re_procdef def ON re.proc_def_id_ = def.id_
                WHERE re.execution_id_ = (
                    SELECT re.execution_id_ FROM ${"$schema."}act_ru_actinst re JOIN ${"$schema."}act_re_procdef def ON re.proc_def_id_ = def.id_
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
                WHERE re.execution_id_ = (
                    SELECT re.execution_id_ FROM ${"$schema."}act_hi_actinst re JOIN ${"$schema."}act_re_procdef def ON re.proc_def_id_ = def.id_
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