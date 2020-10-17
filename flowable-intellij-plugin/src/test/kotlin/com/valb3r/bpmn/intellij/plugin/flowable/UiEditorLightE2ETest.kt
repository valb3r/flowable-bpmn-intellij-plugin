package com.valb3r.bpmn.intellij.plugin.flowable

import com.nhaarman.mockitokotlin2.*
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.mock
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.BpmnElementId
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.BpmnSequenceFlow
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.tasks.BpmnServiceTask
import com.valb3r.bpmn.intellij.plugin.bpmn.api.events.EventPropagatableToXml
import com.valb3r.bpmn.intellij.plugin.bpmn.api.info.PropertyType
import com.valb3r.bpmn.intellij.plugin.core.events.*
import com.valb3r.bpmn.intellij.plugin.core.newelements.registerNewElementsFactory
import com.valb3r.bpmn.intellij.plugin.core.render.AnchorType
import com.valb3r.bpmn.intellij.plugin.core.render.RenderContext
import com.valb3r.bpmn.intellij.plugin.core.render.lastRenderedState
import com.valb3r.bpmn.intellij.plugin.core.state.CurrentState
import com.valb3r.bpmn.intellij.plugin.core.tests.BaseUiTest
import com.valb3r.bpmn.intellij.plugin.flowable.parser.FlowableObjectFactory
import org.amshove.kluent.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.awt.Graphics2D
import java.awt.Shape
import java.awt.font.GlyphVector
import java.awt.geom.Point2D
import java.util.*

internal class UiEditorLightE2ETest: BaseUiTest() {

    @BeforeEach
    fun `Prepare object factory`() {
        registerNewElementsFactory(FlowableObjectFactory())
    }

    @Test
    fun `Ui renders service tasks properly`() {
        prepareTwoServiceTaskView()

        verifyServiceTasksAreDrawn()
    }

    @Test
    fun `Action elements are shown when service task is selected`() {
        prepareTwoServiceTaskView()

        clickOnId(serviceTaskStartDiagramId)

        verifyServiceTasksAreDrawn()
        findExactlyOneNewLinkElem().shouldNotBeNull()
        findExactlyOneDeleteElem().shouldNotBeNull()
    }

    @Test
    fun `Service task can be removed`() {
        prepareTwoServiceTaskView()

        clickOnId(serviceTaskStartDiagramId)

        val deleteElem = findExactlyOneDeleteElem().shouldNotBeNull()
        clickOnId(deleteElem)

        renderResult.shouldNotBeNull().shouldNotHaveKey(serviceTaskStartDiagramId)
        findFirstNewLinkElem().shouldBeNull()
        findFirstDeleteElem().shouldBeNull()
        renderResult.shouldNotBeNull().shouldHaveKey(serviceTaskEndDiagramId)

        argumentCaptor<List<EventPropagatableToXml>>().apply {
            verify(fileCommitter).executeCommitAndGetHash(any(), capture(), any(), any())
            firstValue.shouldContainSame(listOf(
                    DiagramElementRemovedEvent(serviceTaskStartDiagramId),
                    BpmnElementRemovedEvent(serviceTaskStartBpmnId))
            )
        }
    }

    @Test
    fun `New edge element should be addable`() {
        prepareTwoServiceTaskView()

        val addedEdge = addSequenceElementOnFirstTaskAndValidateCommittedExactOnce()

        val intermediateX = 100.0f
        val intermediateY = 100.0f
        val newTaskId = newServiceTask(intermediateX, intermediateY)

        clickOnId(addedEdge.edge.id)
        val lastEndpointId = addedEdge.edge.waypoint.last().id
        val point = clickOnId(lastEndpointId)
        dragToAndVerifyButDontStop(point, Point2D.Float(intermediateX, intermediateY + serviceTaskSize / 2.0f), lastEndpointId)
        canvas.stopDragOrSelect()

        argumentCaptor<List<EventPropagatableToXml>>().apply {
            verify(fileCommitter, times(3)).executeCommitAndGetHash(any(), capture(), any(), any())
            lastValue.shouldHaveSize(4)
            val edgeBpmn = lastValue.filterIsInstance<BpmnEdgeObjectAddedEvent>().shouldHaveSingleItem()
            val shapeBpmn = lastValue.filterIsInstance<BpmnShapeObjectAddedEvent>().shouldHaveSingleItem()
            val draggedTo = lastValue.filterIsInstance<DraggedToEvent>().shouldHaveSingleItem()
            val propUpdated = lastValue.filterIsInstance<StringValueUpdatedEvent>().shouldHaveSingleItem()
            lastValue.shouldContainSame(listOf(edgeBpmn, shapeBpmn, draggedTo, propUpdated))

            shapeBpmn.bpmnObject.element.shouldBeInstanceOf<BpmnServiceTask>()
            shapeBpmn.bpmnObject.parent.shouldBe(basicProcess.process.id)

            val sequence = edgeBpmn.bpmnObject.element.shouldBeInstanceOf<BpmnSequenceFlow>()
            edgeBpmn.bpmnObject.parent.shouldBe(basicProcess.process.id)

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

    @Test
    fun `New service task can be added and sequence flow can be dropped directly on it`() {
        prepareTwoServiceTaskView()

        val addedEdge = addSequenceElementOnFirstTaskAndValidateCommittedExactOnce()
        clickOnId(addedEdge.edge.id)
        val lastEndpointId = addedEdge.edge.waypoint.last().id
        val point = clickOnId(lastEndpointId)

        // Move to the end of service task
        dragToAndVerifyButDontStop(point, Point2D.Float(endElemX + serviceTaskSize, endElemMidY), lastEndpointId)
        canvas.stopDragOrSelect()

        argumentCaptor<List<EventPropagatableToXml>>().apply {
            verify(fileCommitter, times(2)).executeCommitAndGetHash(any(), capture(), any(), any())
            lastValue.shouldHaveSize(3)
            val edgeBpmn = lastValue.filterIsInstance<BpmnEdgeObjectAddedEvent>().shouldHaveSingleItem()
            val draggedTo = lastValue.filterIsInstance<DraggedToEvent>().shouldHaveSingleItem()
            val propUpdated = lastValue.filterIsInstance<StringValueUpdatedEvent>().shouldHaveSingleItem()
            lastValue.shouldContainSame(listOf(edgeBpmn, draggedTo, propUpdated))

            val sequence = edgeBpmn.bpmnObject.element.shouldBeInstanceOf<BpmnSequenceFlow>()
            edgeBpmn.bpmnObject.parent.shouldBe(basicProcess.process.id)
            sequence.sourceRef.shouldBe(serviceTaskStartBpmnId.id)
            sequence.targetRef.shouldBe("")

            draggedTo.diagramElementId.shouldBe(lastEndpointId)
            draggedTo.dx.shouldBeNear(endElemX + serviceTaskSize - point.x, 0.1f)
            draggedTo.dy.shouldBeNear(0.0f, 0.1f)

            propUpdated.bpmnElementId.shouldBe(edgeBpmn.bpmnObject.id)
            propUpdated.property.shouldBe(PropertyType.TARGET_REF)
            propUpdated.newValue.shouldBe(serviceTaskEndBpmnId.id)
        }
    }

    @Test
    fun `For new edge ending waypoint element can be directly dragged to target`() {
        prepareTwoServiceTaskView()

        val addedEdge = addSequenceElementOnFirstTaskAndValidateCommittedExactOnce()
        clickOnId(addedEdge.edge.id)
        val lastEndpointId = addedEdge.edge.waypoint.last().id
        val point = clickOnId(lastEndpointId)

        dragToAndVerifyButDontStop(point, Point2D.Float(endElemX + serviceTaskSize, endElemMidY), lastEndpointId)
        canvas.stopDragOrSelect()

        argumentCaptor<List<EventPropagatableToXml>>().apply {
            verify(fileCommitter, times(2)).executeCommitAndGetHash(any(), capture(), any(), any())
            lastValue.shouldHaveSize(3)
            val edgeBpmn = lastValue.filterIsInstance<BpmnEdgeObjectAddedEvent>().shouldHaveSingleItem()
            val draggedTo = lastValue.filterIsInstance<DraggedToEvent>().shouldHaveSingleItem()
            val propUpdated = lastValue.filterIsInstance<StringValueUpdatedEvent>().shouldHaveSingleItem()
            lastValue.shouldContainSame(listOf(edgeBpmn, draggedTo, propUpdated))

            val sequence = edgeBpmn.bpmnObject.element.shouldBeInstanceOf<BpmnSequenceFlow>()
            edgeBpmn.bpmnObject.parent.shouldBe(basicProcess.process.id)
            sequence.sourceRef.shouldBe(serviceTaskStartBpmnId.id)
            sequence.targetRef.shouldBe("")

            draggedTo.diagramElementId.shouldBe(lastEndpointId)
            draggedTo.dx.shouldBeNear(endElemX + serviceTaskSize - point.x, 0.1f)
            draggedTo.dy.shouldBeNear(0.0f, 0.1f)

            propUpdated.bpmnElementId.shouldBe(edgeBpmn.bpmnObject.id)
            propUpdated.property.shouldBe(PropertyType.TARGET_REF)
            propUpdated.newValue.shouldBe(serviceTaskEndBpmnId.id)
        }
    }

    @Test
    fun `For new edge ending waypoint element can be dragged with intermediate stop to target`() {
        prepareTwoServiceTaskView()

        val addedEdge = addSequenceElementOnFirstTaskAndValidateCommittedExactOnce()
        clickOnId(addedEdge.edge.id)
        val lastEndpointId = addedEdge.edge.waypoint.last().id
        val point = clickOnId(lastEndpointId)

        val midPoint = Point2D.Float(endElemX / 2.0f, 100.0f)
        dragToAndVerifyButDontStop(point, midPoint, lastEndpointId)
        canvas.stopDragOrSelect()
        dragToAndVerifyButDontStop(midPoint, Point2D.Float(endElemX, endElemMidY), lastEndpointId)
        canvas.stopDragOrSelect()

        argumentCaptor<List<EventPropagatableToXml>>().apply {
            verify(fileCommitter, times(3)).executeCommitAndGetHash(any(), capture(), any(), any())
            lastValue.shouldHaveSize(5)
            val edgeBpmn = lastValue.filterIsInstance<BpmnEdgeObjectAddedEvent>().shouldHaveSingleItem()
            val draggedToMid = lastValue.filterIsInstance<DraggedToEvent>().first().shouldNotBeNull()
            val intermediateTargetChangeToParentPropUpd = lastValue.filterIsInstance<StringValueUpdatedEvent>().first().shouldNotBeNull()
            val draggedToTarget = lastValue.filterIsInstance<DraggedToEvent>().last().shouldNotBeNull()
            val propUpdated = lastValue.filterIsInstance<StringValueUpdatedEvent>().last().shouldNotBeNull()
            lastValue.shouldContainSame(listOf(edgeBpmn, draggedToMid, intermediateTargetChangeToParentPropUpd, draggedToTarget, propUpdated))

            val sequence = edgeBpmn.bpmnObject.element.shouldBeInstanceOf<BpmnSequenceFlow>()
            edgeBpmn.bpmnObject.parent.shouldBe(basicProcess.process.id)
            sequence.sourceRef.shouldBe(serviceTaskStartBpmnId.id)
            sequence.targetRef.shouldBe("")

            draggedToMid.diagramElementId.shouldBeEqualTo(lastEndpointId)
            draggedToMid.dx.shouldBeNear(midPoint.x - point.x, 0.1f)
            draggedToMid.dy.shouldBeNear(midPoint.y - point.y, 0.1f)

            intermediateTargetChangeToParentPropUpd.bpmnElementId.shouldBe(edgeBpmn.bpmnObject.id)
            intermediateTargetChangeToParentPropUpd.property.shouldBe(PropertyType.TARGET_REF)
            intermediateTargetChangeToParentPropUpd.newValue.shouldBe(basicProcess.process.id.id)

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

        val addedEdge = addSequenceElementOnFirstTaskAndValidateCommittedExactOnce()
        clickOnId(addedEdge.edge.id)
        val newWaypointAnchor = addedEdge.edge.waypoint.first { !it.physical }.id
        val point = clickOnId(newWaypointAnchor)
        dragToAndVerifyButDontStop(point, Point2D.Float(100.0f, 100.0f), newWaypointAnchor)
        canvas.stopDragOrSelect()

        argumentCaptor<List<EventPropagatableToXml>>().apply {
            verify(fileCommitter, times(2)).executeCommitAndGetHash(any(), capture(), any(), any())
            lastValue.shouldHaveSize(2)
            val edgeBpmn = lastValue.filterIsInstance<BpmnEdgeObjectAddedEvent>().shouldHaveSingleItem()
            val newWaypoint = lastValue.filterIsInstance<NewWaypointsEvent>().shouldHaveSingleItem()
            lastValue.shouldContainSame(listOf(edgeBpmn, newWaypoint))

            val sequence = edgeBpmn.bpmnObject.element.shouldBeInstanceOf<BpmnSequenceFlow>()
            edgeBpmn.bpmnObject.parent.shouldBe(basicProcess.process.id)
            sequence.sourceRef.shouldBe(serviceTaskStartBpmnId.id)
            sequence.targetRef.shouldBe("")

            newWaypoint.edgeElementId.shouldBeEqualTo(addedEdge.edge.id)
            newWaypoint.waypoints.shouldHaveSize(3)
            newWaypoint.waypoints.filter { it.physical }.shouldHaveSize(3)
            newWaypoint.waypoints.map { it.x }.shouldContainSame(listOf(60.0f, 100.0f, 600.0f))
            newWaypoint.waypoints.map { it.y }.shouldContainSame(listOf(30.0f, 100.0f, 30.0f))
        }
    }

    @Test
    fun `For new edge two new intermediate waypoints can be added using coordinates clicks`() {
        prepareTwoServiceTaskView()

        val addedEdge = addSequenceElementOnFirstTaskAndValidateCommittedExactOnce()
        clickOnId(addedEdge.edge.id)
        val edgeStart = addedEdge.edge.waypoint.first()
        val edgeEnd = addedEdge.edge.waypoint.last()
        val dragDelta = Point2D.Float(0.0f, 20.0f)
        val startAtMidpoint = Point2D.Float(edgeStart.x + (edgeEnd.x - edgeStart.x) / 2.0f, edgeStart.y + (edgeEnd.y - edgeStart.y) / 2.0f)
        val afterDragQuarter = Point2D.Float(edgeStart.x + (startAtMidpoint.x - edgeStart.x - dragDelta.x) / 2.0f, edgeStart.y + (startAtMidpoint.y - edgeStart.y - dragDelta.y) / 2.0f)
        // select edge
        canvas.click(startAtMidpoint)
        canvas.paintComponent(graphics)
        // select waypoint
        canvas.click(startAtMidpoint)
        canvas.paintComponent(graphics)
        // drag midpoint
        dragToButDontStop(startAtMidpoint, Point2D.Float(startAtMidpoint.x - dragDelta.x, startAtMidpoint.y - dragDelta.y))
        canvas.stopDragOrSelect()
        canvas.paintComponent(graphics)
        // select edge again
        canvas.click(afterDragQuarter)
        canvas.paintComponent(graphics)
        // select quarter waypoint
        canvas.click(afterDragQuarter)
        canvas.paintComponent(graphics)
        // drag quarter
        dragToButDontStop(afterDragQuarter, Point2D.Float(afterDragQuarter.x - dragDelta.x, afterDragQuarter.y - dragDelta.y))
        canvas.stopDragOrSelect()
        // as a result 2 new waypoints should exist

        argumentCaptor<List<EventPropagatableToXml>>().apply {
            verify(fileCommitter, times(3)).executeCommitAndGetHash(any(), capture(), any(), any())
            lastValue.shouldHaveSize(3)
            val edgeBpmn = lastValue.filterIsInstance<BpmnEdgeObjectAddedEvent>().shouldHaveSingleItem()
            val newMidWaypoint = lastValue.filterIsInstance<NewWaypointsEvent>().first()
            val newQuarterWaypoint = lastValue.filterIsInstance<NewWaypointsEvent>().last()
            lastValue.shouldContainSame(listOf(edgeBpmn, newMidWaypoint, newQuarterWaypoint))

            val sequence = edgeBpmn.bpmnObject.element.shouldBeInstanceOf<BpmnSequenceFlow>()
            edgeBpmn.bpmnObject.parent.shouldBe(basicProcess.process.id)
            sequence.sourceRef.shouldBe(serviceTaskStartBpmnId.id)
            sequence.targetRef.shouldBe("")

            newMidWaypoint.edgeElementId.shouldBeEqualTo(addedEdge.edge.id)
            newMidWaypoint.waypoints.shouldHaveSize(3)
            newMidWaypoint.waypoints.filter { it.physical }.shouldHaveSize(3)
            newMidWaypoint.waypoints.map { it.x }.shouldContainSame(listOf(60.0f, 330.0f, 600.0f))
            newMidWaypoint.waypoints.map { it.y }.shouldContainSame(listOf(30.0f, 10.0f, 30.0f))

            newQuarterWaypoint.edgeElementId.shouldBeEqualTo(addedEdge.edge.id)
            newQuarterWaypoint.waypoints.shouldHaveSize(4)
            newQuarterWaypoint.waypoints.filter { it.physical }.shouldHaveSize(4)
            newQuarterWaypoint.waypoints.map { it.x }.shouldContainSame(listOf(60.0f, 195.0f, 330.0f, 600.0f))
            newQuarterWaypoint.waypoints.map { it.y }.shouldContainSame(listOf(30.0f, 0.0f, 10.0f, 30.0f))
        }
    }

    @Test
    fun `Dragging service task is possible if cursor is inside of it`() {
        prepareTwoServiceTaskView()

        val dragDelta = Point2D.Float(100.0f, 100.0f)
        val point = clickOnId(serviceTaskStartDiagramId)
        dragToButDontStop(point, Point2D.Float(point.x + dragDelta.x, point.y + dragDelta.y))
        canvas.stopDragOrSelect()

        argumentCaptor<List<EventPropagatableToXml>>().apply {
            verify(fileCommitter, times(1)).executeCommitAndGetHash(any(), capture(), any(), any())
            lastValue.shouldHaveSize(1)
            val dragTask = lastValue.filterIsInstance<DraggedToEvent>().first()

            dragTask.diagramElementId.shouldBeEqualTo(serviceTaskStartDiagramId)
            dragTask.dx.shouldBeNear(dragDelta.x, 0.1f)
            dragTask.dy.shouldBeNear(dragDelta.y, 0.1f)
        }
    }

    @Test
    fun `Dragging service task is impossible if cursor is outside of it`() {
        prepareTwoServiceTaskView()

        val dragDelta = Point2D.Float(100.0f, 100.0f)
        val point = clickOnId(serviceTaskEndDiagramId)
        dragToButDontStop(Point2D.Float(-10000.0f, -10000.0f), Point2D.Float(point.x + dragDelta.x, point.y + dragDelta.y))
        canvas.stopDragOrSelect()

        verify(fileCommitter, never()).executeCommitAndGetHash(any(), any(), any(), any())
    }

    @Test
    fun `Dragging service task with sequences attached cascade updates location`() {
        prepareTwoServiceTaskView()

        val dragDelta = Point2D.Float(100.0f, 100.0f)
        val addedEdge = addSequenceElementOnFirstTaskAndValidateCommittedAtLeastOnceAndSelectOne()
        val point = clickOnId(serviceTaskStartDiagramId)
        dragToButDontStop(point, Point2D.Float(point.x + dragDelta.x, point.y + dragDelta.y))
        canvas.stopDragOrSelect()

        argumentCaptor<List<EventPropagatableToXml>>().apply {
            verify(fileCommitter, times(2)).executeCommitAndGetHash(any(), capture(), any(), any())
            lastValue.shouldHaveSize(3)
            val edgeBpmn = lastValue.filterIsInstance<BpmnEdgeObjectAddedEvent>().shouldHaveSingleItem()
            val dragTask = lastValue.filterIsInstance<DraggedToEvent>().first()
            val dragEdge = lastValue.filterIsInstance<DraggedToEvent>().last()
            lastValue.shouldContainSame(listOf(edgeBpmn, dragTask, dragEdge))

            val sequence = edgeBpmn.bpmnObject.element.shouldBeInstanceOf<BpmnSequenceFlow>()
            edgeBpmn.bpmnObject.parent.shouldBe(basicProcess.process.id)
            sequence.sourceRef.shouldBe(serviceTaskStartBpmnId.id)
            sequence.targetRef.shouldBe("")

            dragTask.diagramElementId.shouldBeEqualTo(serviceTaskStartDiagramId)
            dragTask.dx.shouldBeNear(dragDelta.x, 0.1f)
            dragTask.dy.shouldBeNear(dragDelta.y, 0.1f)

            dragEdge.diagramElementId.shouldBeEqualTo(addedEdge.edge.waypoint.first().id)
            dragEdge.dx.shouldBeNear(dragDelta.x, 0.1f)
            dragEdge.dy.shouldBeNear(dragDelta.y, 0.1f)
        }
    }

    @Test
    fun `Renaming element ID cascades to sourceRef and targetRef when changed via PropertiesVisualizer`() {
        prepareTwoServiceTaskView()

        val newId = UUID.randomUUID().toString()
        val addedEdge = addSequenceElementOnFirstTaskAndValidateCommittedAtLeastOnceAndSelectOne()
        changeIdViaPropertiesVisualizer(serviceTaskStartDiagramId, serviceTaskStartBpmnId, newId)

        argumentCaptor<List<EventPropagatableToXml>>().apply {
            verify(fileCommitter, times(2)).executeCommitAndGetHash(any(), capture(), any(), any())
            lastValue.shouldHaveSize(3)
            val edgeBpmn = lastValue.filterIsInstance<BpmnEdgeObjectAddedEvent>().shouldHaveSingleItem()
            val origIdUpdate = lastValue.filterIsInstance<StringValueUpdatedEvent>().first()
            val cascadeIdUpdate = lastValue.filterIsInstance<StringValueUpdatedEvent>().last()
            lastValue.shouldContainSame(listOf(edgeBpmn, origIdUpdate, cascadeIdUpdate))

            val sequence = edgeBpmn.bpmnObject.element.shouldBeInstanceOf<BpmnSequenceFlow>()
            edgeBpmn.bpmnObject.parent.shouldBe(basicProcess.process.id)
            sequence.sourceRef.shouldBe(serviceTaskStartBpmnId.id)
            sequence.targetRef.shouldBe("")

            origIdUpdate.bpmnElementId.shouldBeEqualTo(serviceTaskStartBpmnId)
            origIdUpdate.property.shouldBeEqualTo(PropertyType.ID)
            origIdUpdate.newValue.shouldBeEqualTo(newId)
            origIdUpdate.newIdValue?.id.shouldBeEqualTo(newId)

            cascadeIdUpdate.bpmnElementId.shouldBeEqualTo(addedEdge.edge.bpmnElement)
            cascadeIdUpdate.property.shouldBeEqualTo(PropertyType.SOURCE_REF)
            cascadeIdUpdate.newValue.shouldBeEqualTo(newId)
        }
    }

    @Test
    fun `Cascading position after renaming element ID updates works too`() {
        prepareTwoServiceTaskView()

        val newId = UUID.randomUUID().toString()
        val addedEdge = addSequenceElementOnFirstTaskAndValidateCommittedAtLeastOnceAndSelectOne()
        changeIdViaPropertiesVisualizer(serviceTaskStartDiagramId, serviceTaskStartBpmnId, newId)
        val dragDelta = Point2D.Float(100.0f, 100.0f)
        val point = clickOnId(serviceTaskStartDiagramId)
        dragToButDontStop(point, Point2D.Float(point.x + dragDelta.x, point.y + dragDelta.y))
        canvas.stopDragOrSelect()

        argumentCaptor<List<EventPropagatableToXml>>().apply {
            verify(fileCommitter, times(3)).executeCommitAndGetHash(any(), capture(), any(), any())
            lastValue.shouldHaveSize(5)
            val edgeBpmn = lastValue.filterIsInstance<BpmnEdgeObjectAddedEvent>().shouldHaveSingleItem()
            val origIdUpdate = lastValue.filterIsInstance<StringValueUpdatedEvent>().first()
            val cascadeIdUpdate = lastValue.filterIsInstance<StringValueUpdatedEvent>().last()
            val dragTask = lastValue.filterIsInstance<DraggedToEvent>().first()
            val dragEdge = lastValue.filterIsInstance<DraggedToEvent>().last()
            lastValue.shouldContainSame(listOf(edgeBpmn, origIdUpdate, cascadeIdUpdate, dragTask, dragEdge))

            val sequence = edgeBpmn.bpmnObject.element.shouldBeInstanceOf<BpmnSequenceFlow>()
            edgeBpmn.bpmnObject.parent.shouldBe(basicProcess.process.id)
            sequence.sourceRef.shouldBe(serviceTaskStartBpmnId.id)
            sequence.targetRef.shouldBe("")

            origIdUpdate.bpmnElementId.shouldBeEqualTo(serviceTaskStartBpmnId)
            origIdUpdate.property.shouldBeEqualTo(PropertyType.ID)
            origIdUpdate.newValue.shouldBeEqualTo(newId)
            origIdUpdate.newIdValue?.id.shouldBeEqualTo(newId)

            cascadeIdUpdate.bpmnElementId.shouldBeEqualTo(addedEdge.edge.bpmnElement)
            cascadeIdUpdate.property.shouldBeEqualTo(PropertyType.SOURCE_REF)
            cascadeIdUpdate.newValue.shouldBeEqualTo(newId)

            dragTask.diagramElementId.shouldBeEqualTo(serviceTaskStartDiagramId)
            dragTask.dx.shouldBeNear(dragDelta.x, 0.1f)
            dragTask.dy.shouldBeNear(dragDelta.y, 0.1f)

            dragEdge.diagramElementId.shouldBeEqualTo(addedEdge.edge.waypoint.first().id)
            dragEdge.dx.shouldBeNear(dragDelta.x, 0.1f)
            dragEdge.dy.shouldBeNear(dragDelta.y, 0.1f)
        }
    }

    @Test
    fun `Selecting with rectangle of all elements and dragging them works`() {
        prepareTwoServiceTaskView()

        val begin = Point2D.Float(startElemX - 10.0f, startElemY - 10.0f)
        canvas.paintComponent(graphics)
        canvas.startSelectionOrSelectedDrag(begin)
        canvas.paintComponent(graphics)
        canvas.dragOrSelectWithLeftButton(begin, Point2D.Float(endElemX + serviceTaskSize, endElemY  + serviceTaskSize))
        canvas.paintComponent(graphics)
        canvas.stopDragOrSelect()
        canvas.paintComponent(graphics)

        val startCenterX = startElemX + serviceTaskSize / 2.0f
        val startCenterY = startElemY
        val dragDelta = Point2D.Float(100.0f, 100.0f)
        canvas.startSelectionOrSelectedDrag(Point2D.Float(startCenterX, startCenterY))
        canvas.paintComponent(graphics)
        canvas.dragOrSelectWithLeftButton(begin, Point2D.Float(startCenterX + dragDelta.x, startCenterY + dragDelta.y))
        canvas.paintComponent(graphics)
        canvas.stopDragOrSelect()
        canvas.paintComponent(graphics)

        argumentCaptor<List<EventPropagatableToXml>>().apply {
            verify(fileCommitter, times(1)).executeCommitAndGetHash(any(), capture(), any(), any())
            lastValue.shouldHaveSize(2)
            val dragStart = lastValue.filterIsInstance<DraggedToEvent>().first()
            val dragEnd = lastValue.filterIsInstance<DraggedToEvent>().last()
            lastValue.shouldContainSame(listOf(dragStart, dragEnd))

            dragStart.diagramElementId.shouldBeEqualTo(serviceTaskStartDiagramId)
            dragStart.dx.shouldBeNear(dragDelta.x, 0.1f)
            dragStart.dy.shouldBeNear(dragDelta.y, 0.1f)

            dragEnd.diagramElementId.shouldBeEqualTo(serviceTaskEndDiagramId)
            dragEnd.dx.shouldBeNear(dragDelta.x, 0.1f)
            dragEnd.dy.shouldBeNear(dragDelta.y, 0.1f)
        }
    }

    @Test
    fun `Selecting with rectangle of single and ref on it and dragging them works`() {
        prepareTwoServiceTaskView()

        val addedEdge = addSequenceElementOnFirstTaskAndValidateCommittedExactOnce()

        val begin = Point2D.Float(startElemX - 10.0f, startElemY - 10.0f)
        canvas.click(begin) // unselect service task
        canvas.paintComponent(graphics)
        canvas.startSelectionOrSelectedDrag(begin)
        canvas.paintComponent(graphics)
        canvas.dragOrSelectWithLeftButton(begin, Point2D.Float(startElemX + serviceTaskSize + 10.0f, startElemY + serviceTaskSize + 10.0f))
        canvas.paintComponent(graphics)
        canvas.stopDragOrSelect()
        canvas.paintComponent(graphics)

        val startCenterX = startElemX + serviceTaskSize / 2.0f
        val startCenterY = startElemY
        val dragDelta = Point2D.Float(100.0f, 100.0f)
        canvas.startSelectionOrSelectedDrag(Point2D.Float(startCenterX, startCenterY))
        canvas.paintComponent(graphics)
        canvas.dragOrSelectWithLeftButton(begin, Point2D.Float(startCenterX + dragDelta.x, startCenterY + dragDelta.y))
        canvas.paintComponent(graphics)
        canvas.stopDragOrSelect()
        canvas.paintComponent(graphics)

        argumentCaptor<List<EventPropagatableToXml>>().apply {
            verify(fileCommitter, times(2)).executeCommitAndGetHash(any(), capture(), any(), any())
            lastValue.shouldHaveSize(3)
            val edgeBpmn = lastValue.filterIsInstance<BpmnEdgeObjectAddedEvent>().shouldHaveSingleItem()
            val dragStart = lastValue.filterIsInstance<DraggedToEvent>().first()
            val dragEdge = lastValue.filterIsInstance<DraggedToEvent>().last()
            lastValue.shouldContainSame(listOf(edgeBpmn, dragStart, dragEdge))

            val sequence = edgeBpmn.bpmnObject.element.shouldBeInstanceOf<BpmnSequenceFlow>()
            edgeBpmn.bpmnObject.parent.shouldBe(basicProcess.process.id)
            sequence.sourceRef.shouldBe(serviceTaskStartBpmnId.id)
            sequence.targetRef.shouldBe("")

            dragStart.diagramElementId.shouldBeEqualTo(serviceTaskStartDiagramId)
            dragStart.dx.shouldBeNear(dragDelta.x, 0.1f)
            dragStart.dy.shouldBeNear(dragDelta.y, 0.1f)

            dragEdge.diagramElementId.shouldBeEqualTo(addedEdge.edge.waypoint.first().id)
            dragEdge.dx.shouldBeNear(dragDelta.x, 0.1f)
            dragEdge.dy.shouldBeNear(dragDelta.y, 0.1f)
        }
    }

    @Test
    fun `Selecting element with boundary event using rectangle and dragging them works`() {
        prepareServiceTaskWithAttachedBoundaryEventView()

        selectRectDragAndVerify()
    }

    @Test
    fun `Dragging service task with attached boundary event using rectangle inside subprocess should maintain correct parents`() {
        prepareOneSubProcessServiceTaskWithAttachedBoundaryEventView()

        selectRectDragAndVerify()
    }

    private fun selectRectDragAndVerify() {
        val begin = Point2D.Float(startElemX - 10.0f, startElemY - 10.0f)
        canvas.paintComponent(graphics)
        canvas.startSelectionOrSelectedDrag(begin)
        canvas.paintComponent(graphics)
        canvas.dragOrSelectWithLeftButton(begin, Point2D.Float(endElemX + serviceTaskSize, endElemY + serviceTaskSize))
        canvas.paintComponent(graphics)
        canvas.stopDragOrSelect()
        canvas.paintComponent(graphics)

        val startCenterX = startElemX + serviceTaskSize / 2.0f
        val startCenterY = startElemY
        val dragDelta = Point2D.Float(100.0f, 100.0f)
        canvas.startSelectionOrSelectedDrag(Point2D.Float(startCenterX, startCenterY))
        canvas.paintComponent(graphics)
        canvas.dragOrSelectWithLeftButton(begin, Point2D.Float(startCenterX + dragDelta.x, startCenterY + dragDelta.y))
        canvas.paintComponent(graphics)
        canvas.stopDragOrSelect()
        canvas.paintComponent(graphics)

        argumentCaptor<List<EventPropagatableToXml>>().apply {
            verify(fileCommitter, times(1)).executeCommitAndGetHash(any(), capture(), any(), any())
            lastValue.shouldHaveSize(2)
            val dragStart = lastValue.filterIsInstance<DraggedToEvent>().first()
            val dragEnd = lastValue.filterIsInstance<DraggedToEvent>().last()
            lastValue.shouldContainSame(listOf(dragStart, dragEnd))

            dragStart.diagramElementId.shouldBeEqualTo(serviceTaskStartDiagramId)
            dragStart.dx.shouldBeNear(dragDelta.x, 0.1f)
            dragStart.dy.shouldBeNear(dragDelta.y, 0.1f)

            dragEnd.diagramElementId.shouldBeEqualTo(optionalBoundaryErrorEventDiagramId)
            dragEnd.dx.shouldBeNear(dragDelta.x, 0.1f)
            dragEnd.dy.shouldBeNear(dragDelta.y, 0.1f)
        }
    }

    @Test
    fun `Removing edge element works`() {
        prepareTwoServiceTaskView()

        val addedEdge = addSequenceElementOnFirstTaskAndValidateCommittedExactOnce()
        clickOnId(addedEdge.edge.id)

        val deleteElem = findExactlyOneDeleteElem().shouldNotBeNull()
        clickOnId(deleteElem)

        argumentCaptor<List<EventPropagatableToXml>>().apply {
            verify(fileCommitter, times(2)).executeCommitAndGetHash(any(), capture(), any(), any())
            lastValue.shouldHaveSize(3)
            val edgeBpmn = lastValue.filterIsInstance<BpmnEdgeObjectAddedEvent>().shouldHaveSingleItem()
            val removeEdgeBpmn = lastValue.filterIsInstance<BpmnElementRemovedEvent>().first()
            val removeEdgeDiagram = lastValue.filterIsInstance<DiagramElementRemovedEvent>().first()
            lastValue.shouldContainSame(listOf(edgeBpmn, removeEdgeDiagram, removeEdgeBpmn))

            val sequence = edgeBpmn.bpmnObject.element.shouldBeInstanceOf<BpmnSequenceFlow>()
            edgeBpmn.bpmnObject.parent.shouldBe(basicProcess.process.id)
            sequence.sourceRef.shouldBe(serviceTaskStartBpmnId.id)
            sequence.targetRef.shouldBe("")

            removeEdgeBpmn.bpmnElementId.shouldBeEqualTo(addedEdge.edge.bpmnElement)
            removeEdgeDiagram.elementId.shouldBeEqualTo(addedEdge.edge.id)
        }
    }

    @Test
    fun `Removing waypoint works`() {
        prepareTwoServiceTaskView()

        val newLocation = Point2D.Float(100.0f, 100.0f)
        val addedEdge = addSequenceElementOnFirstTaskAndValidateCommittedExactOnce()
        clickOnId(addedEdge.edge.id)
        val newWaypointAnchor = addedEdge.edge.waypoint.first { !it.physical }.id
        val point = clickOnId(newWaypointAnchor)
        dragToAndVerifyButDontStop(point, newLocation, newWaypointAnchor)
        canvas.stopDragOrSelect()
        canvas.paintComponent(graphics)

        // New renderer does not keep edge selected
        // Select edge:
        canvas.click(newLocation)
        canvas.paintComponent(graphics)
        // Select waypoint:
        canvas.click(newLocation)
        canvas.paintComponent(graphics)
        val deleteElem = findExactlyOneDeleteElem().shouldNotBeNull()
        clickOnId(deleteElem)

        argumentCaptor<List<EventPropagatableToXml>>().apply {
            verify(fileCommitter, times(3)).executeCommitAndGetHash(any(), capture(), any(), any())
            lastValue.shouldHaveSize(3)
            val edgeBpmn = lastValue.filterIsInstance<BpmnEdgeObjectAddedEvent>().shouldHaveSingleItem()
            val newWaypoint = lastValue.filterIsInstance<NewWaypointsEvent>().first()
            val removeWaypoint = lastValue.filterIsInstance<NewWaypointsEvent>().last()
            lastValue.shouldContainSame(listOf(edgeBpmn, newWaypoint, removeWaypoint))

            val sequence = edgeBpmn.bpmnObject.element.shouldBeInstanceOf<BpmnSequenceFlow>()
            edgeBpmn.bpmnObject.parent.shouldBe(basicProcess.process.id)
            sequence.sourceRef.shouldBe(serviceTaskStartBpmnId.id)
            sequence.targetRef.shouldBe("")

            newWaypoint.edgeElementId.shouldBeEqualTo(addedEdge.edge.id)
            newWaypoint.waypoints.shouldHaveSize(3)
            newWaypoint.waypoints.filter { it.physical }.shouldHaveSize(3)
            newWaypoint.waypoints.map { it.x }.shouldContainSame(listOf(60.0f, 100.0f, 600.0f))
            newWaypoint.waypoints.map { it.y }.shouldContainSame(listOf(30.0f, 100.0f, 30.0f))

            removeWaypoint.edgeElementId.shouldBeEqualTo(addedEdge.edge.id)
            removeWaypoint.waypoints.shouldHaveSize(2)
            removeWaypoint.waypoints.filter { it.physical }.shouldHaveSize(2)
            removeWaypoint.waypoints.map { it.x }.shouldContainSame(listOf(60.0f, 600.0f))
            removeWaypoint.waypoints.map { it.y }.shouldContainSame(listOf(30.0f, 30.0f))
        }
    }

    @Test
    fun `Removing with rectangle works`() {
        prepareTwoServiceTaskView()

        val begin = Point2D.Float(-10.0f, -10.0f)
        val addedEdge = addSequenceElementOnFirstTaskAndValidateCommittedExactOnce()
        canvas.click(begin) // de-select rectangle
        canvas.startSelectionOrSelectedDrag(begin)
        canvas.paintComponent(graphics)

        canvas.startSelectionOrSelectedDrag(begin)
        // Stepped selection to allow waypoints to reveal
        canvas.paintComponent(graphics)
        canvas.dragOrSelectWithLeftButton(begin, Point2D.Float(startElemX + serviceTaskSize + 10.0f, startElemX + serviceTaskSize + 10.0f))
        canvas.paintComponent(graphics)
        canvas.dragOrSelectWithLeftButton(begin, Point2D.Float(endElemX + serviceTaskSize + 10.0f, endElemY + serviceTaskSize + 10.0f))
        canvas.paintComponent(graphics)
        canvas.stopDragOrSelect()
        canvas.paintComponent(graphics)
        val deleteElem = findExactlyOneDeleteElem().shouldNotBeNull()
        clickOnId(deleteElem)

        argumentCaptor<List<EventPropagatableToXml>>().apply {
            verify(fileCommitter, times(2)).executeCommitAndGetHash(any(), capture(), any(), any())
            lastValue.shouldHaveSize(7)
            val edgeBpmn = lastValue.filterIsInstance<BpmnEdgeObjectAddedEvent>().shouldHaveSingleItem()
            val diagramRemoved = lastValue.filterIsInstance<DiagramElementRemovedEvent>()
            val bpmnRemoved = lastValue.filterIsInstance<BpmnElementRemovedEvent>()
            lastValue.shouldContainSame(listOf(edgeBpmn) + diagramRemoved + bpmnRemoved)

            val sequence = edgeBpmn.bpmnObject.element.shouldBeInstanceOf<BpmnSequenceFlow>()
            edgeBpmn.bpmnObject.parent.shouldBe(basicProcess.process.id)
            sequence.sourceRef.shouldBe(serviceTaskStartBpmnId.id)
            sequence.targetRef.shouldBe("")

            diagramRemoved.map { it.elementId.id }.shouldContainSame(
                    listOf("DIAGRAM-startServiceTask", addedEdge.edge.id.id, "DIAGRAM-endServiceTask")
            )
            bpmnRemoved.map { it.bpmnElementId.id }.shouldContainSame(
                    listOf("startServiceTask", addedEdge.bpmnObject.id.id, "endServiceTask")
            )
        }
    }

    @Test
    fun `Subprocess is selected as parent`() {
        prepareOneSubProcessView()

        canvas.paintComponent(graphics)

        canvas
                .parentableElementAt(Point2D.Float(subProcessElemX + subProcessSize / 2.0f, subProcessElemY + subProcessSize / 2.0f))
                .shouldBe(subprocessBpmnId)
    }

    @Test
    fun `Removing element from subprocess works`() {
        prepareOneSubProcessWithTwoServiceTasksView()

        clickOnId(serviceTaskStartDiagramId)
        val deleteElem = findExactlyOneDeleteElem().shouldNotBeNull()
        clickOnId(deleteElem)

        renderResult.shouldNotBeNull().shouldNotHaveKey(serviceTaskStartDiagramId)
        findFirstNewLinkElem().shouldBeNull()
        findFirstDeleteElem().shouldBeNull()
        renderResult.shouldNotBeNull().shouldHaveKey(serviceTaskEndDiagramId)

        argumentCaptor<List<EventPropagatableToXml>>().apply {
            verify(fileCommitter).executeCommitAndGetHash(any(), capture(), any(), any())
            firstValue.shouldContainSame(listOf(
                    DiagramElementRemovedEvent(serviceTaskStartDiagramId),
                    BpmnElementRemovedEvent(serviceTaskStartBpmnId))
            )
        }
    }

    @Test
    fun `Adding link to element in subprocess works`() {
        prepareOneSubProcessWithTwoServiceTasksView()

        val addedEdge = addSequenceElementOnFirstTaskAndValidateCommittedExactOnce()

        val intermediateX = 100.0f
        val intermediateY = 100.0f
        val newTaskId = newServiceTask(intermediateX, intermediateY)

        clickOnId(addedEdge.edge.id)
        val lastEndpointId = addedEdge.edge.waypoint.last().id
        val point = clickOnId(lastEndpointId)
        dragToAndVerifyButDontStop(point, Point2D.Float(intermediateX, intermediateY + serviceTaskSize / 2.0f), lastEndpointId)
        canvas.stopDragOrSelect()

        argumentCaptor<List<EventPropagatableToXml>>().apply {
            verify(fileCommitter, times(3)).executeCommitAndGetHash(any(), capture(), any(), any())
            lastValue.shouldHaveSize(4)
            val edgeBpmn = lastValue.filterIsInstance<BpmnEdgeObjectAddedEvent>().shouldHaveSingleItem()
            val shapeBpmn = lastValue.filterIsInstance<BpmnShapeObjectAddedEvent>().shouldHaveSingleItem()
            val draggedTo = lastValue.filterIsInstance<DraggedToEvent>().shouldHaveSingleItem()
            val propUpdated = lastValue.filterIsInstance<StringValueUpdatedEvent>().shouldHaveSingleItem()
            lastValue.shouldContainSame(listOf(edgeBpmn, shapeBpmn, draggedTo, propUpdated))

            shapeBpmn.bpmnObject.element.shouldBeInstanceOf<BpmnServiceTask>()
            shapeBpmn.bpmnObject.parent.shouldBe(basicProcess.process.id)

            val sequence = edgeBpmn.bpmnObject.element.shouldBeInstanceOf<BpmnSequenceFlow>()
            edgeBpmn.bpmnObject.parent.shouldBe(subprocessBpmnId)

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

    @Test
    fun `Subprocess dragging cascades to service tasks and edges`() {
        val dx = 30.0f
        val dy = 30.0f
        val midDx = 20.0f
        val midDy = 20.0f

        prepareOneSubProcessWithTwoServiceTasksView()

        val addedEdge = addSequenceElementOnFirstTaskAndValidateCommittedExactOnce()
        // Add midpoint
        clickOnId(addedEdge.edge.id)
        val newWaypointAnchor = addedEdge.edge.waypoint.first { !it.physical }.id
        val midPoint = clickOnId(newWaypointAnchor)
        dragToAndVerifyButDontStop(midPoint, Point2D.Float(midPoint.x + midDx, midPoint.y + midDy), newWaypointAnchor)
        canvas.stopDragOrSelect()
        canvas.paintComponent(graphics)
        // Click on subprocess
        val subProcessPoint = clickOnId(subprocessDiagramId)
        dragToAndVerifyButDontStop(subProcessPoint, Point2D.Float(subProcessPoint.x + dx, subProcessPoint.y + dy), subprocessDiagramId)
        canvas.stopDragOrSelect()
        canvas.paintComponent(graphics)

        argumentCaptor<List<EventPropagatableToXml>>().apply {
            verify(fileCommitter, times(3)).executeCommitAndGetHash(any(), capture(), any(), any())
            lastValue.shouldHaveSize(8)
            val edgeBpmn = lastValue.filterIsInstance<BpmnEdgeObjectAddedEvent>().shouldHaveSingleItem()
            val newWaypoint = lastValue.filterIsInstance<NewWaypointsEvent>().first()
            val allDraggeds = lastValue.filterIsInstance<DraggedToEvent>()
            val cascadedDrags = allDraggeds.subList(0, allDraggeds.size).shouldHaveSize(6)
            val subprocessDragSelf = cascadedDrags.filter { it.diagramElementId == subprocessDiagramId }.shouldHaveSingleItem()
            val subprocessDragStartServiceTask = cascadedDrags.filter { it.diagramElementId == serviceTaskStartDiagramId }.shouldHaveSingleItem()
            val subprocessDragEndServiceTask = cascadedDrags.filter { it.diagramElementId == serviceTaskEndDiagramId }.shouldHaveSingleItem()
            val subprocessDragEdgeStart = cascadedDrags.filter { it.parentElementId == addedEdge.edge.id && it.internalPos == 0 }.shouldHaveSingleItem()
            val subprocessDragEdgeMid = cascadedDrags.filter { it.parentElementId == addedEdge.edge.id && it.internalPos == 1 }.shouldHaveSingleItem()
            val subprocessDragEdgeEnd = cascadedDrags.filter { it.parentElementId == addedEdge.edge.id && it.internalPos == 2 }.shouldHaveSingleItem()

            edgeBpmn.bpmnObject.parent.shouldBe(subprocessBpmnId)
            edgeBpmn.bpmnObject.id.shouldBe(addedEdge.bpmnObject.id)
            edgeBpmn.props[PropertyType.SOURCE_REF].shouldNotBeNull().value.shouldBe(serviceTaskStartBpmnId.id)

            newWaypoint.edgeElementId.shouldBeEqualTo(addedEdge.edge.id)

            setOf(subprocessDragSelf, subprocessDragStartServiceTask, subprocessDragEndServiceTask, subprocessDragEdgeStart, subprocessDragEdgeMid, subprocessDragEdgeEnd)
                    .forEach {
                        it.dx.shouldBeEqualTo(dx)
                        it.dy.shouldBeEqualTo(dy)
                    }
        }
    }

    @Test
    fun `Single element can be draggable in subprocess`() {
        val dx = 30.0f
        val dy = 30.0f
        prepareOneSubProcessWithTwoServiceTasksView()

        val serviceTaskPoint = clickOnId(serviceTaskStartDiagramId)
        dragToAndVerifyButDontStop(serviceTaskPoint, Point2D.Float(serviceTaskPoint.x + dx, serviceTaskPoint.y + dy), serviceTaskStartDiagramId)
        canvas.stopDragOrSelect()
        canvas.paintComponent(graphics)

        argumentCaptor<List<EventPropagatableToXml>>().apply {
            verify(fileCommitter, times(1)).executeCommitAndGetHash(any(), capture(), any(), any())
            lastValue.shouldHaveSize(1)
            val serviceTaskDragged = lastValue.filterIsInstance<DraggedToEvent>().filter { it.diagramElementId == serviceTaskStartDiagramId }.shouldHaveSingleItem()
            serviceTaskDragged.dx.shouldBeEqualTo(dx)
            serviceTaskDragged.dy.shouldBeEqualTo(dy)
        }
    }

    @Test
    fun `Rectangle selection dragging in subprocess`() {
        val dx = 30.0f
        val dy = 30.0f
        val midDx = 10.0f
        val midDy = 10.0f

        prepareOneSubProcessWithTwoServiceTasksView()

        val addedEdge = addSequenceElementOnFirstTaskAndValidateCommittedExactOnce()
        // Link to second sub-task
        clickOnId(addedEdge.edge.id)
        val lastEndpointId = addedEdge.edge.waypoint.last().id
        val point = clickOnId(lastEndpointId)
        dragToAndVerifyButDontStop(point, Point2D.Float(endElemX + serviceTaskSize, endElemMidY), lastEndpointId)
        canvas.stopDragOrSelect()
        // Add midpoint
        clickOnId(addedEdge.edge.id)
        val newWaypointAnchor = addedEdge.edge.waypoint.first { !it.physical }.id
        val midPoint = clickOnId(newWaypointAnchor)
        dragToAndVerifyButDontStop(midPoint, Point2D.Float(midPoint.x + midDx, midPoint.y + midDy), newWaypointAnchor)
        canvas.stopDragOrSelect()
        // Select rectangle
        val begin = Point2D.Float(-10.0f, -10.0f)
        canvas.click(begin)
        canvas.startSelectionOrSelectedDrag(begin)
        canvas.paintComponent(graphics)
        canvas.dragOrSelectWithLeftButton(begin, Point2D.Float(endElemX + serviceTaskSize + 10.0f, endElemY + serviceTaskSize + 10.0f))
        canvas.stopDragOrSelect()
        canvas.paintComponent(graphics)
        // Drag rectangle
        val dragStart = elementCenter(serviceTaskStartDiagramId)
        canvas.startSelectionOrSelectedDrag(dragStart)
        canvas.dragOrSelectWithLeftButton(dragStart, Point2D.Float(dragStart.x + dx, dragStart.y + dy))
        canvas.stopDragOrSelect()
        canvas.paintComponent(graphics)

        argumentCaptor<List<EventPropagatableToXml>>().apply {
            verify(fileCommitter, times(4)).executeCommitAndGetHash(any(), capture(), any(), any())
            lastValue.shouldHaveSize(9)
            val edgeBpmn = lastValue.filterIsInstance<BpmnEdgeObjectAddedEvent>().shouldHaveSingleItem()
            val draggedToEdge = lastValue.filterIsInstance<DraggedToEvent>().first()
            val propUpdated = lastValue.filterIsInstance<StringValueUpdatedEvent>().shouldHaveSingleItem()
            val newWaypoint = lastValue.filterIsInstance<NewWaypointsEvent>().first()
            val allDraggeds = lastValue.filterIsInstance<DraggedToEvent>()
            val rectDrags = allDraggeds.subList(1, allDraggeds.size).shouldHaveSize(5)

            val rectangleDragStartServiceTask = rectDrags.filter { it.diagramElementId == serviceTaskStartDiagramId }.shouldHaveSingleItem()
            val rectangleDragEndServiceTask = rectDrags.filter { it.diagramElementId == serviceTaskEndDiagramId }.shouldHaveSingleItem()
            val rectangleDragEdgeStart = rectDrags.filter { it.parentElementId == addedEdge.edge.id && it.internalPos == 0 }.shouldHaveSingleItem()
            val rectangleDragEdgeMid = rectDrags.filter { it.parentElementId == addedEdge.edge.id && it.internalPos == 1 }.shouldHaveSingleItem()
            val rectangleDragEdgeEnd = rectDrags.filter { it.parentElementId == addedEdge.edge.id && it.internalPos == 2 }.shouldHaveSingleItem()

            edgeBpmn.bpmnObject.parent.shouldBe(subprocessBpmnId)
            edgeBpmn.bpmnObject.id.shouldBe(addedEdge.bpmnObject.id)
            edgeBpmn.props[PropertyType.SOURCE_REF].shouldNotBeNull().value.shouldBe(serviceTaskStartBpmnId.id)

            draggedToEdge.parentElementId.shouldBe(addedEdge.edge.id)

            propUpdated.bpmnElementId.shouldBe(addedEdge.bpmnObject.id)
            propUpdated.property.shouldBe(PropertyType.TARGET_REF)
            propUpdated.newValue.shouldBeEqualTo(serviceTaskEndBpmnId.id)

            newWaypoint.edgeElementId.shouldBeEqualTo(addedEdge.edge.id)

            setOf(rectangleDragStartServiceTask, rectangleDragEndServiceTask, rectangleDragEdgeStart, rectangleDragEdgeMid, rectangleDragEdgeEnd)
                    .forEach {
                        it.dx.shouldBeEqualTo(dx)
                        it.dy.shouldBeEqualTo(dy)
                    }
        }
    }

    @Test
    fun `Rectangle selection dragging in subprocess within subprocess`() {
        val dx = 30.0f
        val dy = 30.0f

        prepareOneSubProcessThenNestedSubProcessWithOneServiceTaskView()

        // Select rectangle
        val begin = Point2D.Float(-10.0f, -10.0f)
        canvas.click(begin)
        canvas.startSelectionOrSelectedDrag(begin)
        canvas.paintComponent(graphics)
        canvas.dragOrSelectWithLeftButton(begin, Point2D.Float(endElemX + serviceTaskSize + 10.0f, endElemY + serviceTaskSize + 10.0f))
        canvas.stopDragOrSelect()
        canvas.paintComponent(graphics)
        // Drag rectangle
        val dragStart = elementCenter(serviceTaskStartDiagramId)
        canvas.startSelectionOrSelectedDrag(dragStart)
        canvas.dragOrSelectWithLeftButton(dragStart, Point2D.Float(dragStart.x + dx, dragStart.y + dy))
        canvas.stopDragOrSelect()
        canvas.paintComponent(graphics)

        argumentCaptor<List<EventPropagatableToXml>>().apply {
            verify(fileCommitter, times(1)).executeCommitAndGetHash(any(), capture(), any(), any())
            lastValue.shouldHaveSize(1)
            val draggedToEdge = lastValue.filterIsInstance<DraggedToEvent>().first()

            draggedToEdge.diagramElementId.shouldBeEqualTo(serviceTaskStartDiagramId)
            draggedToEdge.dx.shouldBeEqualTo(dx)
            draggedToEdge.dy.shouldBeEqualTo(dy)
        }
    }

    @Test
    fun `Subprocess should ignore its child anchors`() {
        val dx = 30.0f
        val dy = 30.0f

        prepareOneSubProcessThenNestedSubProcessWithOneServiceTaskView()

        // Click on parent subprocess
        val begin = Point2D.Float(subProcessSize - 10.0f, subProcessSize - 10.0f)
        canvas.click(begin)
        canvas.paintComponent(graphics)
        // Drag rectangle
        canvas.startSelectionOrSelectedDrag(begin)
        canvas.dragOrSelectWithLeftButton(begin, Point2D.Float(begin.x + dx, begin.y + dy))
        canvas.paintComponent(graphics)

        argumentCaptor<RenderContext>().apply {
            verify(renderer, atLeastOnce()).render(this.capture())
            lastValue.interactionContext.anchorsHit!!.anchors.shouldBeEmpty()
        }
    }

    @Test
    fun `Removing link from element in subprocess works`() {
        prepareOneSubProcessWithTwoServiceTasksView()

        val addedEdge = addSequenceElementOnFirstTaskAndValidateCommittedExactOnce()
        clickOnId(addedEdge.edge.id)
        val deleteElem = findExactlyOneDeleteElem().shouldNotBeNull()
        clickOnId(deleteElem)

        argumentCaptor<List<EventPropagatableToXml>>().apply {
            verify(fileCommitter, times(2)).executeCommitAndGetHash(any(), capture(), any(), any())
            lastValue.shouldHaveSize(3)
            val edgeBpmn = lastValue.filterIsInstance<BpmnEdgeObjectAddedEvent>().shouldHaveSingleItem()
            val removeShapeBpmn = lastValue.filterIsInstance<BpmnElementRemovedEvent>().shouldHaveSingleItem()
            val removeDiagramBpmn = lastValue.filterIsInstance<DiagramElementRemovedEvent>().shouldHaveSingleItem()

            edgeBpmn.bpmnObject.parent.shouldBe(subprocessBpmnId)
            removeShapeBpmn.bpmnElementId.shouldBe(addedEdge.edge.bpmnElement)
            removeDiagramBpmn.elementId.shouldBe(addedEdge.edge.id)
        }
    }

    @Test
    fun `Virtual (midpoint) edge anchors does not attract other elements`() {
        prepareOneSubProcessWithTwoServiceTasksView()

        val firstEdge = addSequenceElementOnFirstTaskAndValidateCommittedAtLeastOnceAndSelectOne()
        val secondEdge = addSequenceElementOnFirstTaskAndValidateCommittedAtLeastOnceAndSelectOne()
        // Click on sequence element midpoint
        val start = clickOnId(firstEdge.edge.waypoint[1].id)
        val virtualMidpointLocation = elementCenter(secondEdge.edge.waypoint[1].id)
        dragToButDontStop(start, elementCenter(secondEdge.edge.waypoint[1].id))
        canvas.paintComponent(graphics)

        argumentCaptor<RenderContext>().apply {
            verify(renderer, atLeastOnce()).render(capture())
            lastValue.interactionContext.anchorsHit!!.anchors[AnchorType.POINT].shouldBeNull()
            lastValue.interactionContext.anchorsHit!!.anchors[AnchorType.HORIZONTAL].shouldBeNull()
            lastValue.interactionContext.anchorsHit!!.anchors[AnchorType.VERTICAL]!!.distance(virtualMidpointLocation).shouldBeGreaterThan(10.0)
        }
    }

    @Test
    fun `Dragged subprocess element is always visible`() {
        prepareOneSubProcessThenNestedSubProcessWithReversedChildParentOrder()

        val start = clickOnId(diagramNestedSubProcess.id)
        dragToButDontStop(start, Point2D.Float(1000.0f, 1000.0f))
        canvas.paintComponent(graphics)
        canvas.stopDragOrSelect()
        canvas.paintComponent(graphics)

        val draggedStart = clickOnId(diagramNestedSubProcess.id)
        dragToButDontStop(draggedStart, elementCenter(subprocessDiagramId))
        val capturingGraphics = mock<Graphics2D>()
        prepareGraphics(capturingGraphics)
        canvas.paintComponent(capturingGraphics)

        // Child component is rendered after parent (they have different sizes)
        argumentCaptor<Shape>().apply {
            verify(capturingGraphics, atLeastOnce()).fill(capture())
            val parent = allValues
                    .mapIndexedNotNull {pos, it ->
                        if (it.bounds.width == diagramSubProcess.rectBounds().width.toInt()
                                && it.bounds.height == diagramSubProcess.rectBounds().height.toInt()) {
                            return@mapIndexedNotNull pos
                        }
                        return@mapIndexedNotNull null
                    }.shouldHaveSingleItem()

            val subProcess = allValues
                    .mapIndexedNotNull {pos, it ->
                        if (it.bounds.width == diagramNestedSubProcess.rectBounds().width.toInt()
                                && it.bounds.height == diagramNestedSubProcess.rectBounds().height.toInt()) {
                            return@mapIndexedNotNull pos
                        }
                        return@mapIndexedNotNull null
                    }.shouldHaveSingleItem()

            (parent < subProcess).shouldBeTrue()
        }
    }

    @Test
    fun `Name should be rendered on sequence element`() {
        prepareTwoServiceTaskView()
        val addedEdge = addSequenceElementOnFirstTaskAndValidateCommittedExactOnce()
        updateEventsRegistry().addPropertyUpdateEvent(StringValueUpdatedEvent(addedEdge.bpmnObject.id, PropertyType.NAME, "test"))

        val capturingGraphics = mock<Graphics2D>()
        prepareGraphics(capturingGraphics)
        canvas.paintComponent(capturingGraphics)

        argumentCaptor<GlyphVector>().apply {
            verify(capturingGraphics, atLeastOnce()).drawGlyphVector(capture(), any(), any())
            lastValue.numGlyphs.shouldBeEqualTo(4)
        }
    }

    @Test
    fun `Changing element ID twice works`() {
        val newServiceTaskId = "newServiceTaskId"
        val newNewServiceTaskId = "newNewServiceTaskId"
        prepareTwoServiceTaskView()

        clickOnId(serviceTaskStartDiagramId)
        whenever(textFieldsConstructed[Pair(serviceTaskStartBpmnId, PropertyType.ID)]!!.text).thenReturn(newServiceTaskId)
        clickOnId(serviceTaskStartDiagramId)
        whenever(textFieldsConstructed[Pair(BpmnElementId(newServiceTaskId), PropertyType.ID)]!!.text).thenReturn(newNewServiceTaskId)
        clickOnId(serviceTaskStartDiagramId)

        argumentCaptor<List<EventPropagatableToXml>>().apply {
            verify(fileCommitter, times(2)).executeCommitAndGetHash(any(), capture(), any(), any())
            lastValue.shouldHaveSize(2)
            val firstChange = lastValue.filterIsInstance<StringValueUpdatedEvent>().filter { it.bpmnElementId == serviceTaskStartBpmnId }.shouldHaveSingleItem()
            val secondChange = lastValue.filterIsInstance<StringValueUpdatedEvent>().filter { it.bpmnElementId.id == newServiceTaskId }.shouldHaveSingleItem()

            firstChange.referencedValue.shouldBeEqualTo(serviceTaskStartBpmnId.id)
            firstChange.newValue.shouldBeEqualTo(newServiceTaskId)

            secondChange.referencedValue.shouldBeEqualTo(newServiceTaskId)
            secondChange.newValue.shouldBeEqualTo(newNewServiceTaskId)
        }
    }

    @Test
    fun `Root process id changing works`() {
        val newRootProcessId = "new-root-process-id"
        val onlyRootProcessPoint = Point2D.Float(-9999.0f, -9999.0f)

        prepareTwoServiceTaskView()
        canvas.click(onlyRootProcessPoint)
        changeSelectedIdViaPropertiesVisualizer(parentProcessBpmnId, newRootProcessId)

        canvas.paintComponent(graphics)
        verifyServiceTasksAreDrawn()
        canvas.click(onlyRootProcessPoint)
        lastRenderedState()!!.state.ctx.selectedIds.shouldBeEmpty()
        lastRenderedState()!!.state.ctx.stateProvider.currentState()
                .elementByDiagramId[CurrentState.processDiagramId(BpmnElementId(newRootProcessId))].shouldNotBeNull()


        val anotherNewRootProcessId = "another-new-root-process-id"
        canvas.click(onlyRootProcessPoint)
        changeSelectedIdViaPropertiesVisualizer(BpmnElementId(newRootProcessId), anotherNewRootProcessId)
        canvas.paintComponent(graphics)

        verifyServiceTasksAreDrawn()
        canvas.click(onlyRootProcessPoint)
        lastRenderedState()!!.state.ctx.selectedIds.shouldBeEmpty()
        lastRenderedState()!!.state.ctx.stateProvider.currentState()
                .elementByDiagramId[CurrentState.processDiagramId(BpmnElementId(anotherNewRootProcessId))].shouldNotBeNull()
    }
}