package com.valb3r.bpmn.intellij.plugin.commons.actions

import com.intellij.database.dataSource.connection.DGDepartment
import com.intellij.database.model.DasNamespace
import com.intellij.database.psi.DbElement
import com.intellij.database.remote.jdbc.RemoteConnection
import com.intellij.database.util.DbImplUtil
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.LangDataKeys
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.BpmnElementId
import com.valb3r.bpmn.intellij.plugin.core.CANVAS_PAINT_TOPIC
import com.valb3r.bpmn.intellij.plugin.core.debugger.BpmnDebugger
import com.valb3r.bpmn.intellij.plugin.core.debugger.ExecutedElements
import com.valb3r.bpmn.intellij.plugin.core.debugger.detachDebugger
import com.valb3r.bpmn.intellij.plugin.core.debugger.prepareDebugger
import java.sql.Connection
import java.time.Duration
import java.time.Instant
import java.util.concurrent.ForkJoinPool
import java.util.concurrent.atomic.AtomicReference

abstract class DefaultAttachBpmnDebuggerToDbAction(private val debugger: (schema: DbElement) -> BpmnDebugger) : AnAction() {

    override fun actionPerformed(anActionEvent: AnActionEvent) {
        val project = anActionEvent.project ?: return
        val schema = properElem(anActionEvent) ?: return
        prepareDebugger(project, debugger(schema))
        ApplicationManager.getApplication().invokeLater {
            anActionEvent.project!!.messageBus.syncPublisher(CANVAS_PAINT_TOPIC).repaint()
        }
    }

    override fun update(anActionEvent: AnActionEvent) {
        val project = anActionEvent.project
        anActionEvent.presentation.isEnabledAndVisible = project != null && null != properElem(anActionEvent)
    }

    private fun properElem(anActionEvent: AnActionEvent): DbElement? {
        return psiElements(anActionEvent)
                ?.filterIsInstance<DasNamespace>()
                ?.filterIsInstance<DbElement>()
                ?.firstOrNull()
    }

    private fun psiElements(anActionEvent: AnActionEvent) =
            anActionEvent.getData(LangDataKeys.PSI_ELEMENT_ARRAY)
}

abstract class IntelliJBpmnDebugger(private val schema: DbElement): BpmnDebugger {

    private val cacheTTL = Duration.ofSeconds(1)
    private val worker: ForkJoinPool = ForkJoinPool(1)

    private var cachedResult: AtomicReference<ExecutedElements?> = AtomicReference()
    private var cachedAtTime: AtomicReference<Instant?> = AtomicReference()

    override fun executionSequence(project: Project, processId: String): ExecutedElements? {
        val cachedExpiry = cachedAtTime.get()?.plus(cacheTTL)
        if (cachedExpiry?.isAfter(Instant.now()) == true) {
            return cachedResult.get()
        }

        worker.submit {
            cachedResult.set(fetchFromDb(project, processId))
            cachedAtTime.set(Instant.now())
        }

        return cachedResult.get()
    }

    private fun fetchFromDb(project: Project, processId: String): ExecutedElements? {
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
            detachDebugger(project)
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

    protected abstract fun statementForRuntimeSelection(schema: String): String
    protected abstract fun statementForHistoricalSelection(schema: String): String
}
