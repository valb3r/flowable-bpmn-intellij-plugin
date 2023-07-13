package com.valb3r.bpmn.intellij.plugin.core

import com.intellij.notification.NotificationType
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.openapi.vfs.newvfs.BulkFileListener
import com.intellij.openapi.vfs.newvfs.events.VFileContentChangeEvent
import com.intellij.openapi.vfs.newvfs.events.VFileEvent
import com.intellij.util.messages.MessageBusConnection
import com.intellij.util.messages.Topic
import com.valb3r.bpmn.intellij.plugin.bpmn.api.BpmnParser
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.BpmnElementId
import com.valb3r.bpmn.intellij.plugin.bpmn.api.exceptions.IgnorableParserException
import com.valb3r.bpmn.intellij.plugin.bpmn.api.info.FunctionalGroupType
import com.valb3r.bpmn.intellij.plugin.bpmn.api.info.PropertyType
import com.valb3r.bpmn.intellij.plugin.core.advertisement.showTryPolyBpmnAdvertisementSwimpoolNotification
import com.valb3r.bpmn.intellij.plugin.core.events.FileCommitter
import com.valb3r.bpmn.intellij.plugin.core.events.initializeUpdateEventsRegistry
import com.valb3r.bpmn.intellij.plugin.core.events.updateEventsRegistry
import com.valb3r.bpmn.intellij.plugin.core.newelements.newElementsFactory
import com.valb3r.bpmn.intellij.plugin.core.properties.SelectedValueAccessor
import com.valb3r.bpmn.intellij.plugin.core.properties.TextValueAccessor
import com.valb3r.bpmn.intellij.plugin.core.properties.newPropertiesVisualizer
import com.valb3r.bpmn.intellij.plugin.core.render.BpmnProcessRenderer
import com.valb3r.bpmn.intellij.plugin.core.render.Canvas
import com.valb3r.bpmn.intellij.plugin.core.ui.components.notifications.genericShowNotificationBalloon
import java.nio.charset.StandardCharsets.UTF_8
import java.util.*
import javax.swing.JButton
import javax.swing.JTable
import javax.swing.plaf.basic.BasicArrowButton

interface PaintTopicListener: EventListener {
    fun repaint()
}

val CANVAS_PAINT_TOPIC = Topic("BPMN Flowable (plugin family) plugin repaint topic", PaintTopicListener::class.java)

class CanvasBuilder(
    private val bpmnProcessRenderer: BpmnProcessRenderer,
    private val onBadContentErrorCallback: ((String) -> Unit)? = null,
    private val onBadContentWarningCallback: ((String) -> Unit)? = null
) {

    private var currentVfsConnection: MessageBusConnection? = null
    private var currentPaintConnection: MessageBusConnection? = null

    fun build(
        committerFactory: (BpmnParser) -> FileCommitter, parser: BpmnParser, properties: JTable,
        dropDownFactory: (id: BpmnElementId, type: PropertyType, value: String, availableValues: Set<String>) -> TextValueAccessor,
        classEditorFactory: (id: BpmnElementId, type: PropertyType, value: String) -> TextValueAccessor,
        editorFactory: (id: BpmnElementId, type: PropertyType, value: String) -> TextValueAccessor,
        textFieldFactory: (id: BpmnElementId, type: PropertyType, value: String) -> TextValueAccessor,
        multiLineExpandableTextFieldFactory: (id: BpmnElementId, type: PropertyType, value: String) -> TextValueAccessor,
        checkboxFieldFactory: (id: BpmnElementId, type: PropertyType, value: Boolean) -> SelectedValueAccessor,
        buttonFactory: (id: BpmnElementId,  type: FunctionalGroupType) -> JButton,
        arrowButtonFactory: (id: BpmnElementId) -> BasicArrowButton,
        canvas: Canvas,
        project: Project,
        bpmnFile: VirtualFile
    ) {
        if (assertFileContentAndShowErrorOrWarning(parser, bpmnFile, onBadContentErrorCallback, onBadContentWarningCallback)) return

        initializeUpdateEventsRegistry(project, committerFactory.invoke(parser))
        val data = readFile(bpmnFile)
        val process = parser.parse(data)
        if (data.contains("collaboration")) showTryPolyBpmnAdvertisementSwimpoolNotification(project)
        newPropertiesVisualizer(project, properties, dropDownFactory, classEditorFactory, editorFactory, textFieldFactory, multiLineExpandableTextFieldFactory, checkboxFieldFactory, buttonFactory, arrowButtonFactory)
        val mappedProcess = process.toView(newElementsFactory(project))
        canvas.reset(data, mappedProcess, bpmnProcessRenderer)
        if (mappedProcess.suppressedExceptions.isNotEmpty()) {
            // Do not show too long errors:
            val aggregatedErrors = mappedProcess.suppressedExceptions.take(3).map { "- ${it.message}\n" }.toSet().joinToString()
            genericShowNotificationBalloon(project, "XML parsing error", aggregatedErrors, NotificationType.WARNING)
        }

        currentVfsConnection?.let { it.disconnect(); it.dispose() }
        currentPaintConnection?.let { it.disconnect(); it.dispose() }
        currentVfsConnection = attachFileChangeListener(project, bpmnFile) {
            build(committerFactory, parser, properties, dropDownFactory, classEditorFactory, editorFactory, textFieldFactory, multiLineExpandableTextFieldFactory, checkboxFieldFactory, buttonFactory, arrowButtonFactory, canvas, project, it)
        }
        currentPaintConnection = attachPaintListener(project, canvas)
    }

    fun assertFileContentAndShowErrorOrWarning(
        parser: BpmnParser,
        bpmnFile: VirtualFile,
        onBadContentErrorCallback: ((String) -> Unit)?,
        onBadContentWarningCallback: ((String) -> Unit)?
    ): Boolean {
        val errors = parser.validateForErrors(readFile(bpmnFile))
        if (null != errors) {
            onBadContentErrorCallback?.invoke(errors)
            return true
        }

        val warnings = parser.validateForWarnings(readFile(bpmnFile))
        if (null != warnings) {
            onBadContentWarningCallback?.invoke(warnings)
        }
        return false
    }

    private fun attachFileChangeListener(project: Project, bpmnFile: VirtualFile, onUpdate: ((bpmnFile: VirtualFile) -> Unit)): MessageBusConnection {
        val connection = project.messageBus.connect()
        val registry = updateEventsRegistry(project)

        connection.subscribe(VirtualFileManager.VFS_CHANGES, object : BulkFileListener {
            override fun after(events: MutableList<out VFileEvent>) {
                super.after(events)
                val event = events
                        .filter { it.file == bpmnFile }
                        .filterIsInstance<VFileContentChangeEvent>()
                        .lastOrNull()
                        ?: return

                if (!registry.fileStateMatches(readFile(event.file))) {
                    onUpdate(event.file)
                }
            }
        })

        return connection
    }

    private fun attachPaintListener(project: Project, canvas: Canvas): MessageBusConnection {
        val connection = project.messageBus.connect()

        connection.subscribe(CANVAS_PAINT_TOPIC, object : PaintTopicListener {
            override fun repaint() {
                canvas.repaint()
            }
        })

        return connection
    }

    private fun readFile(bpmnFile: VirtualFile) = String(bpmnFile.contentsToByteArray(), UTF_8)
}