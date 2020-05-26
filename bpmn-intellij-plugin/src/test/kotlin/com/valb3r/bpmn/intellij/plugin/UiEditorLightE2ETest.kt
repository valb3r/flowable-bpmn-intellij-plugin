package com.valb3r.bpmn.intellij.plugin

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.EditorTextField
import com.intellij.util.messages.MessageBus
import com.intellij.util.messages.MessageBusConnection
import com.nhaarman.mockitokotlin2.*
import com.valb3r.bpmn.intellij.plugin.bpmn.api.BpmnParser
import com.valb3r.bpmn.intellij.plugin.bpmn.api.BpmnProcessObject
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.BpmnElementId
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.BpmnProcess
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.BpmnServiceTask
import com.valb3r.bpmn.intellij.plugin.bpmn.api.diagram.DiagramElement
import com.valb3r.bpmn.intellij.plugin.bpmn.api.diagram.DiagramElementId
import com.valb3r.bpmn.intellij.plugin.bpmn.api.diagram.elements.BoundsElement
import com.valb3r.bpmn.intellij.plugin.bpmn.api.diagram.elements.PlaneElement
import com.valb3r.bpmn.intellij.plugin.bpmn.api.diagram.elements.ShapeElement
import com.valb3r.bpmn.intellij.plugin.render.AreaWithZindex
import com.valb3r.bpmn.intellij.plugin.render.Canvas
import com.valb3r.bpmn.intellij.plugin.render.DefaultBpmnProcessRenderer
import com.valb3r.bpmn.intellij.plugin.render.IconProvider
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldHaveKey
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.anyInt
import java.awt.Graphics2D
import java.awt.geom.Point2D
import java.awt.geom.Rectangle2D
import java.nio.charset.StandardCharsets
import javax.swing.JTable
import javax.swing.table.TableColumn
import javax.swing.table.TableColumnModel


internal class UiEditorLightE2ETest {

    private val icon = "dummy-icon.svg".asResource()
    private val iconSize = 32

    private val startElemX = 0.0f
    private val startElemY = 0.0f
    private val serviceTaskSize = 60.0f

    private val endElemX = 10 * serviceTaskSize
    private val endElemY = 0.0f

    private val startElemCenter = Point2D.Float(startElemX + serviceTaskSize / 2.0f, startElemY  + serviceTaskSize / 2.0f)
    private val endElemCenter = Point2D.Float(endElemX + serviceTaskSize / 2.0f, endElemY  + serviceTaskSize / 2.0f)

    private val planeElementBpmnId = BpmnElementId("planeElementId")
    private val diagramMainElementId = DiagramElementId("diagramMainElement")
    private val diagramMainPlaneElementId = DiagramElementId("diagramMainPlaneElement")

    private val serviceTaskStartBpmnId = BpmnElementId("startServiceTask")
    private val serviceTaskEndBpmnId = BpmnElementId("endServiceTask")
    private val serviceTaskStartDiagramId = DiagramElementId("DIAGRAM-startServiceTask")
    private val serviceTaskEndDiagramId = DiagramElementId("DIAGRAM-endServiceTask")

    private val bpmnServiceTaskStart = BpmnServiceTask(serviceTaskStartBpmnId, null, null, null, null, null, null, null, null, null)
    private val bpmnServiceTaskEnd = BpmnServiceTask(serviceTaskEndBpmnId, null, null, null, null, null, null, null, null, null)
    private val diagramServiceTaskStart = ShapeElement(serviceTaskStartDiagramId, bpmnServiceTaskStart.id, BoundsElement(startElemX, startElemY, serviceTaskSize, serviceTaskSize))
    private val diagramServiceTaskEnd = ShapeElement(serviceTaskEndDiagramId, bpmnServiceTaskEnd.id, BoundsElement(endElemX, endElemY, serviceTaskSize, serviceTaskSize))

    private val icons = mock<IconProvider>()
    private val renderer = spy(DefaultBpmnProcessRenderer(icons))
    private val canvasBuilder = CanvasBuilder(renderer)
    private val canvas = Canvas()
    private var renderResult: Map<DiagramElementId, AreaWithZindex>? = null

    private val basicProcess = BpmnProcessObject(
            BpmnProcess(BpmnElementId("processElem"), "mainProcess", null, null, null, null, null, null, null, null),
            mutableListOf()
    )

    private val graphics = mock<Graphics2D>()
    private val messageBus = mock<MessageBus>()
    private val messageBusConnection = mock<MessageBusConnection>()
    private val parser = mock<BpmnParser>()
    private val project = mock<Project>()
    private val virtualFile = mock<VirtualFile>()
    private val columnModel = mock<TableColumnModel>()
    private val tableColumn = mock<TableColumn>()
    private val propertiesTable = mock<JTable>()
    private val editorTextField = mock<EditorTextField>()
    private val editorFieldFactory = { id: String -> editorTextField}

    @BeforeEach
    fun setupMocks() {
        whenever(editorTextField.text).thenReturn("")
        whenever(propertiesTable.columnModel).thenReturn(columnModel)
        whenever(columnModel.getColumn(anyInt())).thenReturn(tableColumn)
        whenever(graphics.create()).thenReturn(graphics)
        whenever(virtualFile.contentsToByteArray()).thenReturn(ByteArray(0))
        whenever(project.messageBus).thenReturn(messageBus)
        whenever(messageBus.connect()).thenReturn(messageBusConnection)

        whenever(icons.sequence).thenReturn(icon)
        whenever(icons.recycleBin).thenReturn(icon)
        whenever(icons.exclusiveGateway).thenReturn(icon)
        whenever(icons.gear).thenReturn(mock())
        whenever(icons.redo).thenReturn(mock())
        whenever(icons.undo).thenReturn(mock())

        doAnswer {
            val result = it.callRealMethod()!! as Map<DiagramElementId, AreaWithZindex>
            renderResult = result
            return@doAnswer result
        }.whenever(renderer).render(any());
    }

    @Test
    fun `Ui renders service tasks properly`() {
        prepareTwoServiceTaskView()

        canvasBuilder.build(parser, propertiesTable, editorFieldFactory, canvas, project, virtualFile)
        canvas.paintComponent(graphics)

        verifyServiceTasksAreDrawn()
    }

    @Test
    fun `Action elements are shown when service task is selected`() {
        prepareTwoServiceTaskView()

        canvasBuilder.build(parser, propertiesTable, editorFieldFactory, canvas, project, virtualFile)
        canvas.paintComponent(graphics)
        canvas.click(startElemCenter)
        canvas.paintComponent(graphics)

        verifyServiceTasksAreDrawn()
    }

    private fun verifyServiceTasksAreDrawn() {
        renderResult.shouldNotBeNull().shouldHaveKey(serviceTaskStartDiagramId)
        renderResult.shouldNotBeNull().shouldHaveKey(serviceTaskEndDiagramId)
        renderResult?.get(serviceTaskStartDiagramId)!!.area.bounds2D.shouldBeEqualTo(Rectangle2D.Float(startElemX, startElemY, serviceTaskSize, serviceTaskSize))
        renderResult?.get(serviceTaskEndDiagramId)!!.area.bounds2D.shouldBeEqualTo(Rectangle2D.Float(endElemX, endElemY, serviceTaskSize, serviceTaskSize))
    }

    private fun prepareTwoServiceTaskView() {
        val process = basicProcess.copy(
                basicProcess.process.copy(serviceTask = listOf(bpmnServiceTaskStart, bpmnServiceTaskEnd)),
                listOf(DiagramElement(
                        diagramMainElementId,
                        PlaneElement(diagramMainPlaneElementId, planeElementBpmnId, listOf(diagramServiceTaskStart, diagramServiceTaskEnd), listOf()))
                )
        )
        whenever(parser.parse("")).thenReturn(process)
    }

    private fun String.asResource(): String? = UiEditorLightE2ETest::class.java.classLoader.getResource(this)?.readText(StandardCharsets.UTF_8)
}