package com.valb3r.bpmn.intellij.plugin.commons.actions

import com.intellij.database.dataSource.connection.DGDepartment
import com.intellij.database.model.DasNamespace
import com.intellij.database.psi.DbElement
import com.intellij.database.remote.jdbc.RemoteConnection
import com.intellij.database.remote.jdbc.RemotePreparedStatement
import com.intellij.database.util.DbImplUtil
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.LangDataKeys
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.BpmnElementId
import com.valb3r.bpmn.intellij.plugin.bpmn.api.info.PropertyType
import com.valb3r.bpmn.intellij.plugin.core.CANVAS_PAINT_TOPIC
import com.valb3r.bpmn.intellij.plugin.core.debugger.BpmnDebugger
import com.valb3r.bpmn.intellij.plugin.core.debugger.ExecutedElements
import com.valb3r.bpmn.intellij.plugin.core.debugger.detachDebugger
import com.valb3r.bpmn.intellij.plugin.core.debugger.prepareDebugger
import com.valb3r.bpmn.intellij.plugin.core.state.currentStateProvider
import java.sql.Connection
import java.time.Duration
import java.time.Instant
import java.util.concurrent.ForkJoinPool
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

abstract class DefaultAttachBpmnDebuggerToDbAction(
    private val debugger: (schema: DbElement, selectionId: String?, processId: String, processName: String?) -> BpmnDebugger
) : AnAction() {

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

    override fun actionPerformed(anActionEvent: AnActionEvent) {
        val project = anActionEvent.project ?: return
        val schema = properElem(anActionEvent) ?: return
        val state = currentStateProvider(project).currentState()
        val processId = state.processId.id
        if (processId.isBlank()) {
            Messages.showErrorDialog(project, "Open a BPMN process before attaching the debugger.", "No BPMN Process")
            return
        }
        val processName = state.elemPropertiesByStaticElementId[state.processId]
            ?.get(PropertyType.NAME)?.value as? String
        val selectionId = Messages.showInputDialog(
            project,
            "Enter a process instance ID or execution ID for:\n${processName ?: processId}\nBPMN process ID: $processId\nLeave blank to use the latest instance:",
            "Select BPMN Execution",
            null
        )?.trim() ?: return
        val selectedExecutionId = selectionId.takeIf { it.isNotEmpty() }

        prepareDebugger(project, debugger(schema, selectedExecutionId, processId, processName?.takeIf { it.isNotBlank() }))
        ApplicationManager.getApplication().invokeLater {
            project.messageBus.syncPublisher(CANVAS_PAINT_TOPIC).repaint()
        }
    }

    override fun update(anActionEvent: AnActionEvent) {
        val project = anActionEvent.project
        anActionEvent.presentation.isEnabledAndVisible = project != null && properElem(anActionEvent) != null
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

abstract class IntelliJBpmnDebugger(
    private val schema: DbElement,
    private val selectionId: String?,
    private val expectedProcessId: String,
    private val expectedProcessName: String?
): BpmnDebugger {

    private val cacheTTL = Duration.ofSeconds(1)
    private val worker: ForkJoinPool = ForkJoinPool(1)
    private val cachedResult = AtomicReference<ExecutedElements?>()
    private val cachedAtTime = AtomicReference<Instant?>()
    private val queryInProgress = AtomicBoolean(false)
    private val mismatchReported = AtomicBoolean(false)

    override fun executionSequence(project: Project, processId: String): ExecutedElements? {
        if (processId != expectedProcessId) {
            return null
        }

        val cachedExpiry = cachedAtTime.get()?.plus(cacheTTL)
        if (cachedExpiry?.isAfter(Instant.now()) == true) {
            return cachedResult.get()
        }

        if (queryInProgress.compareAndSet(false, true)) {
            worker.submit {
                try {
                    val result = fetchFromDb()
                    cachedResult.set(result)
                    if (result == null) {
                        detachDebugger(project)
                        reportSelectionMismatch(project)
                    }
                } catch (ex: RuntimeException) {
                    detachDebugger(project)
                } finally {
                    cachedAtTime.set(Instant.now())
                    queryInProgress.set(false)
                    ApplicationManager.getApplication().invokeLater {
                        if (!project.isDisposed) {
                            project.messageBus.syncPublisher(CANVAS_PAINT_TOPIC).repaint()
                        }
                    }
                }
            }
        }

        return cachedResult.get()
    }

    private fun fetchFromDb(): ExecutedElements? {
        val connectionProvider = DbImplUtil.getDatabaseConnection(schema, DGDepartment.INTROSPECTION)?.get()
        val remoteConnection = connectionProvider?.remoteConnection ?: return null
        try {
            return readExecutionIds { statement, selectedId -> listIds(statement, selectedId, remoteConnection) }
        } finally {
            remoteConnection.close()
        }
    }

    private fun readExecutionIds(idsFetch: (statement: String, selectionId: String?) -> List<DbActivity>): ExecutedElements? {
        val checkProcessName = expectedProcessName != null
        val historicalActivities = idsFetch(statementForHistoricalSelection(schema.name, selectionId, checkProcessName), selectionId)
        val runtimeActivities = idsFetch(statementForRuntimeSelection(schema.name, selectionId, checkProcessName), selectionId)
        return toExecutedElements(historicalActivities + runtimeActivities, selectionId == null)
    }

    private fun toExecutedElements(activities: List<DbActivity>, selectLatestInstance: Boolean): ExecutedElements? {
        val processInstanceId = if (selectLatestInstance) {
            activities.groupBy { it.processInstanceId }
                .maxByOrNull { (_, instanceActivities) -> instanceActivities.minOf { it.startedAt } }
                ?.key
        } else {
            activities.firstOrNull()?.processInstanceId
        } ?: return null
        val activityIds = activities.asSequence()
            .filter { it.processInstanceId == processInstanceId }
            .sortedBy { it.startedAt }
            .map { it.activityId }
            .map(::BpmnElementId)
            .toList()
        return ExecutedElements(activityIds, processInstanceId)
    }

    private fun listIds(statement: String, selectionId: String?, connection: Connection): List<DbActivity> {
        val query = connection.prepareStatement(statement)
        bindSelection(query, selectionId)
        val result = query.executeQuery()
        return result.use {
            generateSequence { if (result.next()) DbActivity(result.getString(1), result.getString(2), result.getTimestamp(3).toInstant()) else null }.toList()
        }
    }

    private fun listIds(statement: String, selectionId: String?, connection: RemoteConnection): List<DbActivity> {
        val query = connection.prepareStatement(statement)
        bindSelection(query, selectionId)
        val result = query.executeQuery()
        return generateSequence { if (result.next()) DbActivity(result.getString(1), result.getString(2), result.getTimestamp(3).toInstant()) else null }.toList()
    }

    private fun bindSelection(query: java.sql.PreparedStatement, selectionId: String?) {
        bindSelection(selectionId) { index, value -> query.setString(index, value) }
    }

    private fun bindSelection(query: RemotePreparedStatement, selectionId: String?) {
        bindSelection(selectionId) { index, value -> query.setString(index, value) }
    }

    private fun bindSelection(selectionId: String?, setString: (index: Int, value: String) -> Unit) {
        if (selectionId == null) {
            setString(1, expectedProcessId)
            expectedProcessName?.let { setString(2, it) }
            setString(if (expectedProcessName == null) 2 else 3, expectedProcessId)
            expectedProcessName?.let { setString(4, it) }
        } else {
            setString(1, selectionId)
            setString(2, selectionId)
            setString(3, expectedProcessId)
            expectedProcessName?.let { setString(4, it) }
        }
    }

    private fun reportSelectionMismatch(project: Project) {
        if (!mismatchReported.compareAndSet(false, true)) {
            return
        }

        ApplicationManager.getApplication().invokeLater {
            if (!project.isDisposed) {
                val processLabel = expectedProcessName?.let { "$it ($expectedProcessId)" } ?: expectedProcessId
                val selectedLabel = selectionId?.let { "ID '$it'" } ?: "the latest process instance"
                Messages.showErrorDialog(
                    project,
                    "Could not find $selectedLabel for BPMN process '$processLabel'.",
                    "BPMN Execution Not Found"
                )
            }
        }
    }

    protected abstract fun statementForRuntimeSelection(schema: String, selectionId: String?, checkProcessName: Boolean): String
    protected abstract fun statementForHistoricalSelection(schema: String, selectionId: String?, checkProcessName: Boolean): String
}

private data class DbActivity(val processInstanceId: String, val activityId: String, val startedAt: Instant)
