package com.valb3r.bpmn.intellij.plugin

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.EditorTextField
import com.intellij.util.messages.MessageBus
import com.intellij.util.messages.MessageBusConnection
import com.nhaarman.mockitokotlin2.*
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.mock
import com.valb3r.bpmn.intellij.plugin.bpmn.api.BpmnParser
import com.valb3r.bpmn.intellij.plugin.bpmn.api.BpmnProcessObject
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.BpmnElementId
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.BpmnProcess
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.BpmnSequenceFlow
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.BpmnServiceTask
import com.valb3r.bpmn.intellij.plugin.bpmn.api.diagram.DiagramElement
import com.valb3r.bpmn.intellij.plugin.bpmn.api.diagram.DiagramElementId
import com.valb3r.bpmn.intellij.plugin.bpmn.api.diagram.elements.BoundsElement
import com.valb3r.bpmn.intellij.plugin.bpmn.api.diagram.elements.PlaneElement
import com.valb3r.bpmn.intellij.plugin.bpmn.api.diagram.elements.ShapeElement
import com.valb3r.bpmn.intellij.plugin.bpmn.api.events.Event
import com.valb3r.bpmn.intellij.plugin.bpmn.api.info.Property
import com.valb3r.bpmn.intellij.plugin.bpmn.api.info.PropertyType
import com.valb3r.bpmn.intellij.plugin.events.*
import com.valb3r.bpmn.intellij.plugin.render.AreaWithZindex
import com.valb3r.bpmn.intellij.plugin.render.Canvas
import com.valb3r.bpmn.intellij.plugin.render.DefaultBpmnProcessRenderer
import com.valb3r.bpmn.intellij.plugin.render.IconProvider
import org.amshove.kluent.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.anyInt
import java.awt.Graphics2D
import java.awt.geom.Point2D
import java.awt.geom.Rectangle2D
import java.nio.charset.StandardCharsets
import java.util.*
import javax.swing.JTable
import javax.swing.table.TableColumn
import javax.swing.table.TableColumnModel


internal class UiEditorLightE2ETest {

    private val newLink = "NEWLINK"
    private val doDel = "DEL"

    private val icon = "dummy-icon.svg".asResource()

    private val startElemX = 0.0f
    private val startElemY = 0.0f
    private val serviceTaskSize = 60.0f

    private val endElemX = 10 * serviceTaskSize
    private val endElemY = 0.0f
    private val endElemMidY = serviceTaskSize / 2.0f

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
    private val fileCommitter = mock<FileCommitter>()
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

        initializeCanvas()

        verifyServiceTasksAreDrawn()
    }

    @Test
    fun `Action elements are shown when service task is selected`() {
        prepareTwoServiceTaskView()

        initializeCanvas()
        clickOnId(serviceTaskStartDiagramId)

        verifyServiceTasksAreDrawn()
        findExactlyOneNewLinkElem().shouldNotBeNull()
        findExactlyOneDeleteElem().shouldNotBeNull()
    }

    @Test
    fun `Service task can be removed`() {
        prepareTwoServiceTaskView()

        initializeCanvas()
        clickOnId(serviceTaskStartDiagramId)

        val deleteElem = findExactlyOneDeleteElem().shouldNotBeNull()
        clickOnId(deleteElem)

        renderResult.shouldNotBeNull().shouldNotHaveKey(serviceTaskStartDiagramId)
        findFirstNewLinkElem().shouldBeNull()
        findFirstDeleteElem().shouldBeNull()
        renderResult.shouldNotBeNull().shouldHaveKey(serviceTaskEndDiagramId)

        argumentCaptor<List<Event>>().apply {
            verify(fileCommitter).executeCommitAndGetHash(any(), capture(), any())
            firstValue.shouldContainSame(listOf(
                    DiagramElementRemovedEvent(serviceTaskStartDiagramId),
                    BpmnElementRemovedEvent(serviceTaskStartBpmnId))
            )
        }
    }

    @Test
    fun `New edge element should be addable`() {
        prepareTwoServiceTaskView()
        initializeCanvas()

        val addedEdge = addSequenceElementOnFirstTask()

        val intermediateX = 100.0f
        val intermediateY = 100.0f
        val newTaskId = newServiceTask(intermediateX, intermediateY)

        clickOnId(addedEdge.edge.id)
        val lastEndpointId = addedEdge.edge.waypoint.last().id
        val point = clickOnId(lastEndpointId)
        dragToEndTaskButDontStop(point, Point2D.Float(intermediateX, intermediateY + serviceTaskSize / 2.0f), lastEndpointId)
        canvas.stopDragOrSelect()

        argumentCaptor<List<Event>>().apply {
            verify(fileCommitter, times(3)).executeCommitAndGetHash(any(), capture(), any())
            lastValue.shouldHaveSize(4)
            val edgeBpmn = lastValue.filterIsInstance<BpmnEdgeObjectAddedEvent>().shouldHaveSingleItem()
            val shapeBpmn = lastValue.filterIsInstance<BpmnShapeObjectAddedEvent>().shouldHaveSingleItem()
            val draggedTo = lastValue.filterIsInstance<DraggedToEvent>().shouldHaveSingleItem()
            val propUpdated = lastValue.filterIsInstance<StringValueUpdatedEvent>().shouldHaveSingleItem()
            lastValue.shouldContainSame(listOf(edgeBpmn, shapeBpmn, draggedTo, propUpdated))

            val sequence = edgeBpmn.bpmnObject.shouldBeInstanceOf<BpmnSequenceFlow>()
            sequence.sourceRef.shouldBe(serviceTaskStartBpmnId.id)
            sequence.targetRef.shouldBe("")

            draggedTo.diagramElementId.shouldBe(lastEndpointId)
            draggedTo.dx.shouldBeNear(intermediateX - point.x, 0.1f)
            draggedTo.dy.shouldBeNear(intermediateY + serviceTaskSize / 2.0f - point.y, 0.1f)

            propUpdated.bpmnElementId.shouldBe(edgeBpmn.bpmnObject.id)
            propUpdated.property.shouldBe(PropertyType.TARGET_REF)
            propUpdated.newValue.shouldBeEqualTo(newTaskId.id)
        }
    }

    private fun newServiceTask(intermediateX: Float, intermediateY: Float): BpmnElementId {
        val task = bpmnServiceTaskStart.copy(id = BpmnElementId("sid-" + UUID.randomUUID().toString()))
        val shape = diagramServiceTaskStart.copy(
                id = DiagramElementId("sid-" + UUID.randomUUID().toString()),
                bounds = BoundsElement(intermediateX, intermediateY, serviceTaskSize, serviceTaskSize)
        )
        updateEventsRegistry().addObjectEvent(
                BpmnShapeObjectAddedEvent(task, shape, mapOf(PropertyType.ID to Property(task.id)))
        )

        return task.id
    }

    @Test
    fun `New service task can be added and sequence flow can be dropped directly on it`() {
        prepareTwoServiceTaskView()

        val addedEdge = addSequenceElementOnFirstTask()
        clickOnId(addedEdge.edge.id)
        val lastEndpointId = addedEdge.edge.waypoint.last().id
        val point = clickOnId(lastEndpointId)

        dragToEndTaskButDontStop(point, Point2D.Float(endElemX, endElemMidY), lastEndpointId)
        canvas.stopDragOrSelect()

        argumentCaptor<List<Event>>().apply {
            verify(fileCommitter, times(2)).executeCommitAndGetHash(any(), capture(), any())
            lastValue.shouldHaveSize(3)
            val edgeBpmn = lastValue.filterIsInstance<BpmnEdgeObjectAddedEvent>().shouldHaveSingleItem()
            val draggedTo = lastValue.filterIsInstance<DraggedToEvent>().shouldHaveSingleItem()
            val propUpdated = lastValue.filterIsInstance<StringValueUpdatedEvent>().shouldHaveSingleItem()
            lastValue.shouldContainSame(listOf(edgeBpmn, draggedTo, propUpdated))

            val sequence = edgeBpmn.bpmnObject.shouldBeInstanceOf<BpmnSequenceFlow>()
            sequence.sourceRef.shouldBe(serviceTaskStartBpmnId.id)
            sequence.targetRef.shouldBe("")

            draggedTo.diagramElementId.shouldBe(lastEndpointId)
            draggedTo.dx.shouldBeNear(endElemX - point.x, 0.1f)
            draggedTo.dy.shouldBeNear(0.0f, 0.1f)

            propUpdated.bpmnElementId.shouldBe(edgeBpmn.bpmnObject.id)
            propUpdated.property.shouldBe(PropertyType.TARGET_REF)
            propUpdated.newValue.shouldBe(serviceTaskEndBpmnId.id)
        }
    }

    @Test
    fun `For new edge ending waypoint element can be directly dragged to target`() {
        prepareTwoServiceTaskView()

        val addedEdge = addSequenceElementOnFirstTask()
        clickOnId(addedEdge.edge.id)
        val lastEndpointId = addedEdge.edge.waypoint.last().id
        val point = clickOnId(lastEndpointId)

        dragToEndTaskButDontStop(point, Point2D.Float(endElemX, endElemMidY), lastEndpointId)
        canvas.stopDragOrSelect()

        argumentCaptor<List<Event>>().apply {
            verify(fileCommitter, times(2)).executeCommitAndGetHash(any(), capture(), any())
            lastValue.shouldHaveSize(3)
            val edgeBpmn = lastValue.filterIsInstance<BpmnEdgeObjectAddedEvent>().shouldHaveSingleItem()
            val draggedTo = lastValue.filterIsInstance<DraggedToEvent>().shouldHaveSingleItem()
            val propUpdated = lastValue.filterIsInstance<StringValueUpdatedEvent>().shouldHaveSingleItem()
            lastValue.shouldContainSame(listOf(edgeBpmn, draggedTo, propUpdated))

            val sequence = edgeBpmn.bpmnObject.shouldBeInstanceOf<BpmnSequenceFlow>()
            sequence.sourceRef.shouldBe(serviceTaskStartBpmnId.id)
            sequence.targetRef.shouldBe("")

            draggedTo.diagramElementId.shouldBe(lastEndpointId)
            draggedTo.dx.shouldBeNear(endElemX - point.x, 0.1f)
            draggedTo.dy.shouldBeNear(0.0f, 0.1f)

            propUpdated.bpmnElementId.shouldBe(edgeBpmn.bpmnObject.id)
            propUpdated.property.shouldBe(PropertyType.TARGET_REF)
            propUpdated.newValue.shouldBe(serviceTaskEndBpmnId.id)
        }
    }

    @Test
    fun `For new edge ending waypoint element can be dragged with intermediate stop to target`() {
        prepareTwoServiceTaskView()

        val addedEdge = addSequenceElementOnFirstTask()
        clickOnId(addedEdge.edge.id)
        val lastEndpointId = addedEdge.edge.waypoint.last().id
        val point = clickOnId(lastEndpointId)

        val midPoint = Point2D.Float(endElemX / 2.0f, 100.0f)
        dragToEndTaskButDontStop(point, midPoint, lastEndpointId)
        canvas.stopDragOrSelect()
        dragToEndTaskButDontStop(midPoint, Point2D.Float(endElemX, endElemMidY), lastEndpointId)
        canvas.stopDragOrSelect()

        argumentCaptor<List<Event>>().apply {
            verify(fileCommitter, times(3)).executeCommitAndGetHash(any(), capture(), any())
            lastValue.shouldHaveSize(4)
            val edgeBpmn = lastValue.filterIsInstance<BpmnEdgeObjectAddedEvent>().shouldHaveSingleItem()
            val draggedToMid = lastValue.filterIsInstance<DraggedToEvent>().first().shouldNotBeNull()
            val draggedToTarget = lastValue.filterIsInstance<DraggedToEvent>().last().shouldNotBeNull()
            val propUpdated = lastValue.filterIsInstance<StringValueUpdatedEvent>().shouldHaveSingleItem()
            lastValue.shouldContainSame(listOf(edgeBpmn, draggedToMid, draggedToTarget, propUpdated))

            val sequence = edgeBpmn.bpmnObject.shouldBeInstanceOf<BpmnSequenceFlow>()
            sequence.sourceRef.shouldBe(serviceTaskStartBpmnId.id)
            sequence.targetRef.shouldBe("")

            draggedToMid.diagramElementId.shouldBeEqualTo(lastEndpointId)
            draggedToMid.dx.shouldBeNear(midPoint.x - point.x, 0.1f)
            draggedToMid.dy.shouldBeNear(midPoint.y - point.y, 0.1f)

            draggedToTarget.diagramElementId.shouldBeEqualTo(lastEndpointId)
            draggedToTarget.dx.shouldBeNear(endElemX - point.x - draggedToMid.dx, 0.1f)
            draggedToTarget.dy.shouldBeNear(-draggedToMid.dy, 0.1f)

            propUpdated.bpmnElementId.shouldBe(edgeBpmn.bpmnObject.id)
            propUpdated.property.shouldBe(PropertyType.TARGET_REF)
            propUpdated.newValue.shouldBe(serviceTaskEndBpmnId.id)
        }
    }

    @Test
    fun `For new edge intermediate waypoint can be added`() {
        prepareTwoServiceTaskView()

        val addedEdge = addSequenceElementOnFirstTask()
        clickOnId(addedEdge.edge.id)
        val newWaypointAnchor = addedEdge.edge.waypoint.first { !it.physical }.id
        val point = clickOnId(newWaypointAnchor)
        dragToEndTaskButDontStop(point, Point2D.Float(100.0f, 100.0f), newWaypointAnchor)
        canvas.stopDragOrSelect()

        argumentCaptor<List<Event>>().apply {
            verify(fileCommitter, times(2)).executeCommitAndGetHash(any(), capture(), any())
            lastValue.shouldHaveSize(2)
            val edgeBpmn = lastValue.filterIsInstance<BpmnEdgeObjectAddedEvent>().shouldHaveSingleItem()
            val newWaypoint = lastValue.filterIsInstance<NewWaypointsEvent>().shouldHaveSingleItem()
            lastValue.shouldContainSame(listOf(edgeBpmn, newWaypoint))

            val sequence = edgeBpmn.bpmnObject.shouldBeInstanceOf<BpmnSequenceFlow>()
            sequence.sourceRef.shouldBe(serviceTaskStartBpmnId.id)
            sequence.targetRef.shouldBe("")

            newWaypoint.edgeElementId.shouldBeEqualTo(addedEdge.edge.id)
            newWaypoint.waypoints.shouldHaveSize(3)
            newWaypoint.waypoints.filter { it.physical }.shouldHaveSize(3)
            newWaypoint.waypoints.map { it.x }.shouldContainSame(listOf(60.0f, 100.0f, 100.0f))
            newWaypoint.waypoints.map { it.y }.shouldContainSame(listOf(30.0f, 100.0f, 30.0f))
        }
    }

    private fun dragToEndTaskButDontStop(point: Point2D.Float, target: Point2D.Float, lastEndpointId: DiagramElementId) {
        canvas.startSelectionOrDrag(point)
        canvas.paintComponent(graphics)
        canvas.dragOrSelectWithLeftButton(point, target)
        canvas.paintComponent(graphics)
        val dragAboutToFinishArea = elemAreaById(lastEndpointId)
        dragAboutToFinishArea.area.bounds2D.centerX.shouldBeNear(target.x.toDouble(), 0.1)
        dragAboutToFinishArea.area.bounds2D.centerY.shouldBeNear(target.y.toDouble(), 0.1)
    }

    private fun addSequenceElementOnFirstTask(): BpmnEdgeObjectAddedEvent {
        initializeCanvas()
        clickOnId(serviceTaskStartDiagramId)
        verifyServiceTasksAreDrawn()
        val newLink = findExactlyOneNewLinkElem().shouldNotBeNull()
        clickOnId(newLink)

        argumentCaptor<List<Event>>().let {
            verify(fileCommitter).executeCommitAndGetHash(any(), it.capture(), any())
            it.firstValue.shouldHaveSize(1)
            val edgeBpmn = it.firstValue.filterIsInstance<BpmnEdgeObjectAddedEvent>().shouldHaveSingleItem()
            return edgeBpmn
        }
    }

    private fun clickOnId(elemId: DiagramElementId): Point2D.Float {
        val area = renderResult?.get(elemId).shouldNotBeNull()
        val bounds = area.area.bounds2D.shouldNotBeNull()
        val point = Point2D.Float(bounds.centerX.toFloat(), bounds.centerY.toFloat())
        canvas.click(point)
        canvas.paintComponent(graphics)
        return point
    }

    private fun initializeCanvas() {
        canvasBuilder.build({ fileCommitter }, parser, propertiesTable, editorFieldFactory, canvas, project, virtualFile)
        canvas.paintComponent(graphics)
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

    private fun elemAreaById(id: DiagramElementId) = renderResult?.get(id)!!

    private fun findFirstNewLinkElem() = renderResult?.keys?.firstOrNull { it.id.contains(newLink) }
    private fun findFirstDeleteElem() = renderResult?.keys?.firstOrNull { it.id.contains(doDel) }

    private fun findExactlyOneNewLinkElem() = renderResult?.keys?.filter { it.id.contains(newLink) }?.shouldHaveSize(1)?.first()
    private fun findExactlyOneDeleteElem() = renderResult?.keys?.filter { it.id.contains(doDel) }?.shouldHaveSize(1)?.first()

    private fun String.asResource(): String? = UiEditorLightE2ETest::class.java.classLoader.getResource(this)?.readText(StandardCharsets.UTF_8)
}