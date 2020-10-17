package com.valb3r.bpmn.intellij.plugin

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.argumentCaptor
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import com.valb3r.bpmn.intellij.plugin.bpmn.api.events.EventPropagatableToXml
import com.valb3r.bpmn.intellij.plugin.core.events.BpmnEdgeObjectAddedEvent
import com.valb3r.bpmn.intellij.plugin.core.events.BpmnParentChangedEvent
import com.valb3r.bpmn.intellij.plugin.core.events.DraggedToEvent
import com.valb3r.bpmn.intellij.plugin.core.newelements.registerNewElementsFactory
import com.valb3r.bpmn.intellij.plugin.core.render.lastRenderedState
import com.valb3r.bpmn.intellij.plugin.core.tests.BaseUiTest
import com.valb3r.bpmn.intellij.plugin.flowable.parser.FlowableObjectFactory
import org.amshove.kluent.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.awt.geom.Point2D

internal class UiMultiSelectTest: BaseUiTest() {

    @BeforeEach
    fun `Prepare object factory`() {
        registerNewElementsFactory(FlowableObjectFactory())
    }

    // Is affected by multiselect feature, this is why it is here
    @Test
    fun `New service task can be created inside subprocess with correct parent`() {
        prepareOneSubProcessServiceTaskWithAttachedBoundaryEventView()
        val subprocessCenter = elementCenter(subprocessDiagramId)

        // currently it is enough to check that canvas will provide correct parent
        canvas.parentableElementAt(subprocessCenter).shouldBeEqualTo(subprocessBpmnId)
    }

    @Test
    fun `When selecting elements from multiple levels only elements from one level are chosen`() {
        prepareOneSubProcessWithServiceTaskAndAttachedBoundaryEventOneNestedSubprocessAndServiceTaskWithSequence()

        val selectionStart = Point2D.Float(startElemX - 10.0f, startElemY - 10.0f)
        canvas.startSelectionOrSelectedDrag(selectionStart)
        canvas.paintComponent(graphics)
        canvas.dragOrSelectWithLeftButton(selectionStart, Point2D.Float(endElemX + serviceTaskSize + 10.0f, endElemY + serviceTaskSize + 10.0f))
        canvas.paintComponent(graphics)
        canvas.stopDragOrSelect()
        canvas.paintComponent(graphics)

        lastRenderedState()!!.state.ctx.selectedIds.shouldHaveSize(2)
        lastRenderedState()!!.state.ctx.selectedIds.shouldContain(serviceTaskEndDiagramId)
        lastRenderedState()!!.state.ctx.selectedIds.shouldNotContain(serviceTaskStartDiagramId)
    }

    @Test
    fun `Multiple elements can be dragged out from subprocess and will get parent process as the parent`() {
        prepareOneSubProcessWithTwoLinkedServiceTasksView()

        val selectionStart = Point2D.Float(startElemX - 10.0f, startElemY - 10.0f)
        canvas.click(selectionStart)
        canvas.startSelectionOrSelectedDrag(selectionStart)
        canvas.paintComponent(graphics)
        canvas.dragOrSelectWithLeftButton(selectionStart, Point2D.Float(endElemX + serviceTaskSize, endElemX + serviceTaskSize))
        canvas.paintComponent(graphics)
        canvas.stopDragOrSelect()
        canvas.paintComponent(graphics)

        val dragBegin = elementCenter(serviceTaskStartDiagramId)
        val delta = 10000.0f
        canvas.startSelectionOrSelectedDrag(dragBegin)
        canvas.paintComponent(graphics)
        canvas.dragOrSelectWithLeftButton(dragBegin, Point2D.Float(dragBegin.x + delta, dragBegin.x + delta))
        canvas.paintComponent(graphics)
        canvas.stopDragOrSelect()

        argumentCaptor<List<EventPropagatableToXml>>().apply {
            verify(fileCommitter, times(2)).executeCommitAndGetHash(any(), capture(), any(), any())
            val newEdge = lastValue.filterIsInstance<BpmnEdgeObjectAddedEvent>().shouldHaveSingleItem()
            val dragStartTask = lastValue.filterIsInstance<DraggedToEvent>().filter { it.diagramElementId == serviceTaskStartDiagramId }.shouldHaveSingleItem()
            val dragEndTask = lastValue.filterIsInstance<DraggedToEvent>().filter { it.diagramElementId == serviceTaskEndDiagramId }.shouldHaveSingleItem()
            val dragEdgeWaypointStart = lastValue.filterIsInstance<DraggedToEvent>().filter { it.diagramElementId == newEdge.edge.waypoint[0].id }.shouldHaveSingleItem()
            val dragEdgeWaypointEnd = lastValue.filterIsInstance<DraggedToEvent>().filter { it.diagramElementId == newEdge.edge.waypoint[2].id }.shouldHaveSingleItem()
            val parentChangeStartTask = lastValue.filterIsInstance<BpmnParentChangedEvent>().filter { it.bpmnElementId == serviceTaskStartBpmnId }.shouldHaveSingleItem()
            val parentChangeEndTask = lastValue.filterIsInstance<BpmnParentChangedEvent>().filter { it.bpmnElementId == serviceTaskEndBpmnId }.shouldHaveSingleItem()
            val parentChangeEdge = lastValue.filterIsInstance<BpmnParentChangedEvent>().filter { it.bpmnElementId == newEdge.bpmnObject.id }.shouldHaveSize(2)
            lastValue.shouldHaveSize(9)

            dragStartTask.dx.shouldBeEqualTo(delta)
            dragStartTask.dy.shouldBeEqualTo(delta)
            dragEndTask.dx.shouldBeEqualTo(delta)
            dragEndTask.dy.shouldBeEqualTo(delta)
            dragEdgeWaypointStart.dx.shouldBeEqualTo(delta)
            dragEdgeWaypointStart.dy.shouldBeEqualTo(delta)
            dragEdgeWaypointEnd.dx.shouldBeEqualTo(delta)
            dragEdgeWaypointEnd.dy.shouldBeEqualTo(delta)

            parentChangeStartTask.newParentId.shouldBeEqualTo(parentProcessBpmnId)
            parentChangeEndTask.newParentId.shouldBeEqualTo(parentProcessBpmnId)
            parentChangeEdge[0].newParentId.shouldBeEqualTo(parentProcessBpmnId)
            parentChangeEdge[1].newParentId.shouldBeEqualTo(parentProcessBpmnId)
        }
    }

    @Test
    fun `Multiple elements can be dragged inside subprocess with subprocess itself and will keep subprocess as parent`() {
        prepareOneSubProcessWithTwoLinkedServiceTasksView()

        val selectionStart = Point2D.Float(startElemX - 10.0f, startElemY - 10.0f)
        canvas.click(selectionStart)
        canvas.startSelectionOrSelectedDrag(selectionStart)
        canvas.paintComponent(graphics)
        canvas.dragOrSelectWithLeftButton(selectionStart, Point2D.Float(subProcessSize * 2.0f, subProcessSize * 2.0f))
        canvas.paintComponent(graphics)
        canvas.stopDragOrSelect()
        canvas.paintComponent(graphics)

        val dragBegin = elementCenter(subprocessDiagramId) // child elements are also standalone click capturing players
        val delta = 10.0f
        canvas.startSelectionOrSelectedDrag(dragBegin)
        canvas.paintComponent(graphics)
        canvas.dragOrSelectWithLeftButton(dragBegin, Point2D.Float(dragBegin.x + delta, dragBegin.x + delta))
        canvas.paintComponent(graphics)
        canvas.stopDragOrSelect()

        argumentCaptor<List<EventPropagatableToXml>>().apply {
            verify(fileCommitter, times(2)).executeCommitAndGetHash(any(), capture(), any(), any())
            val newEdge = lastValue.filterIsInstance<BpmnEdgeObjectAddedEvent>().shouldHaveSingleItem()
            val dragSubprocess = lastValue.filterIsInstance<DraggedToEvent>().filter { it.diagramElementId == subprocessDiagramId }.shouldHaveSingleItem()
            val dragStartTask = lastValue.filterIsInstance<DraggedToEvent>().filter { it.diagramElementId == serviceTaskStartDiagramId }.shouldHaveSingleItem()
            val dragEndTask = lastValue.filterIsInstance<DraggedToEvent>().filter { it.diagramElementId == serviceTaskEndDiagramId }.shouldHaveSingleItem()
            val dragEdgeWaypointStart = lastValue.filterIsInstance<DraggedToEvent>().filter { it.diagramElementId == newEdge.edge.waypoint[0].id }.shouldHaveSingleItem()
            val dragEdgeWaypointEnd = lastValue.filterIsInstance<DraggedToEvent>().filter { it.diagramElementId == newEdge.edge.waypoint[2].id }.shouldHaveSingleItem()
            lastValue.shouldHaveSize(6)

            dragSubprocess.dx.shouldBeEqualTo(delta)
            dragSubprocess.dy.shouldBeEqualTo(delta)
            dragStartTask.dx.shouldBeEqualTo(delta)
            dragStartTask.dy.shouldBeEqualTo(delta)
            dragEndTask.dx.shouldBeEqualTo(delta)
            dragEndTask.dy.shouldBeEqualTo(delta)
            dragEdgeWaypointStart.dx.shouldBeEqualTo(delta)
            dragEdgeWaypointStart.dy.shouldBeEqualTo(delta)
            dragEdgeWaypointEnd.dx.shouldBeEqualTo(delta)
            dragEdgeWaypointEnd.dy.shouldBeEqualTo(delta)
        }
    }

    @Test
    fun `Subprocess and its children can be dragged correctly when both are selected`() {
        prepareOneSubProcessWithTwoLinkedServiceTasksView()

        val selectionStart = Point2D.Float(startElemX - 10.0f, startElemY - 10.0f)
        canvas.click(selectionStart)
        canvas.startSelectionOrSelectedDrag(selectionStart)
        canvas.paintComponent(graphics)
        canvas.dragOrSelectWithLeftButton(selectionStart, Point2D.Float(subProcessSize + 100.0f, subProcessSize + 100.0f))
        canvas.paintComponent(graphics)
        canvas.stopDragOrSelect()
        canvas.paintComponent(graphics)

        val dragBegin = elementCenter(subprocessDiagramId) // child elements are also standalone click capturing players
        val delta = 10.0f
        canvas.startSelectionOrSelectedDrag(dragBegin)
        canvas.paintComponent(graphics)
        canvas.dragOrSelectWithLeftButton(dragBegin, Point2D.Float(dragBegin.x + delta, dragBegin.x + delta))
        canvas.paintComponent(graphics)
        canvas.stopDragOrSelect()

        argumentCaptor<List<EventPropagatableToXml>>().apply {
            verify(fileCommitter, times(2)).executeCommitAndGetHash(any(), capture(), any(), any())
            val newEdge = lastValue.filterIsInstance<BpmnEdgeObjectAddedEvent>().shouldHaveSingleItem()
            val dragSubProcess = lastValue.filterIsInstance<DraggedToEvent>().filter { it.diagramElementId == subprocessDiagramId }.shouldHaveSingleItem()
            val dragStartTask = lastValue.filterIsInstance<DraggedToEvent>().filter { it.diagramElementId == serviceTaskStartDiagramId }.shouldHaveSingleItem()
            val dragEndTask = lastValue.filterIsInstance<DraggedToEvent>().filter { it.diagramElementId == serviceTaskEndDiagramId }.shouldHaveSingleItem()
            val dragEdgeWaypointStart = lastValue.filterIsInstance<DraggedToEvent>().filter { it.diagramElementId == newEdge.edge.waypoint[0].id }.shouldHaveSingleItem()
            val dragEdgeWaypointEnd = lastValue.filterIsInstance<DraggedToEvent>().filter { it.diagramElementId == newEdge.edge.waypoint[2].id }.shouldHaveSingleItem()
            lastValue.shouldHaveSize(6)

            dragSubProcess.dx.shouldBeEqualTo(delta)
            dragSubProcess.dy.shouldBeEqualTo(delta)
            dragStartTask.dx.shouldBeEqualTo(delta)
            dragStartTask.dy.shouldBeEqualTo(delta)
            dragEndTask.dx.shouldBeEqualTo(delta)
            dragEndTask.dy.shouldBeEqualTo(delta)
            dragEdgeWaypointStart.dx.shouldBeEqualTo(delta)
            dragEdgeWaypointStart.dy.shouldBeEqualTo(delta)
            dragEdgeWaypointEnd.dx.shouldBeEqualTo(delta)
            dragEdgeWaypointEnd.dy.shouldBeEqualTo(delta)
        }
    }

    @Test
    fun `Subprocess within subprocess and its children can be dragged correctly when both are selected`() {
        prepareOneSubProcessThenNestedSubProcessWithOneServiceTaskView()

        val selectionStart = Point2D.Float(startElemX - 10.0f, startElemY - 10.0f)
        canvas.click(selectionStart)
        canvas.startSelectionOrSelectedDrag(selectionStart)
        canvas.paintComponent(graphics)
        canvas.dragOrSelectWithLeftButton(selectionStart, Point2D.Float(nestedSubProcessSize + 20.0f, nestedSubProcessSize + 20.0f))
        canvas.paintComponent(graphics)
        canvas.stopDragOrSelect()
        canvas.paintComponent(graphics)

        val dragBegin = elementCenter(subprocessInSubProcessDiagramId)
        val delta = 10.0f
        canvas.startSelectionOrSelectedDrag(dragBegin)
        canvas.paintComponent(graphics)
        canvas.dragOrSelectWithLeftButton(dragBegin, Point2D.Float(dragBegin.x + delta, dragBegin.x + delta))
        canvas.paintComponent(graphics)
        canvas.stopDragOrSelect()

        argumentCaptor<List<EventPropagatableToXml>>().apply {
            verify(fileCommitter, times(1)).executeCommitAndGetHash(any(), capture(), any(), any())
            val dragSubProcess = lastValue.filterIsInstance<DraggedToEvent>().filter { it.diagramElementId == subprocessInSubProcessDiagramId }.shouldHaveSingleItem()
            val dragStartTask = lastValue.filterIsInstance<DraggedToEvent>().filter { it.diagramElementId == serviceTaskStartDiagramId }.shouldHaveSingleItem()
            lastValue.shouldHaveSize(2)

            dragSubProcess.dx.shouldBeEqualTo(delta)
            dragSubProcess.dy.shouldBeEqualTo(delta)
            dragStartTask.dx.shouldBeEqualTo(delta)
            dragStartTask.dy.shouldBeEqualTo(delta)
        }
    }

    @Test
    fun `Root subprocess with subprocess and its children can be dragged correctly when all are selected`() {
        prepareOneSubProcessThenNestedSubProcessWithOneServiceTaskView()

        val selectionStart = Point2D.Float(startElemX - 10.0f, startElemY - 10.0f)
        canvas.click(selectionStart)
        canvas.startSelectionOrSelectedDrag(selectionStart)
        canvas.paintComponent(graphics)
        canvas.dragOrSelectWithLeftButton(selectionStart, Point2D.Float(subProcessSize + 100.0f, subProcessSize + 100.0f))
        canvas.paintComponent(graphics)
        canvas.stopDragOrSelect()
        canvas.paintComponent(graphics)

        val dragBegin = elementCenter(subprocessDiagramId) // child elements are also standalone click capturing players
        val delta = 10.0f
        canvas.startSelectionOrSelectedDrag(dragBegin)
        canvas.paintComponent(graphics)
        canvas.dragOrSelectWithLeftButton(dragBegin, Point2D.Float(dragBegin.x + delta, dragBegin.x + delta))
        canvas.paintComponent(graphics)
        canvas.stopDragOrSelect()

        argumentCaptor<List<EventPropagatableToXml>>().apply {
            verify(fileCommitter, times(1)).executeCommitAndGetHash(any(), capture(), any(), any())
            val dragRootSubProcess = lastValue.filterIsInstance<DraggedToEvent>().filter { it.diagramElementId == subprocessDiagramId }.shouldHaveSingleItem()
            val dragNestedSubProcess = lastValue.filterIsInstance<DraggedToEvent>().filter { it.diagramElementId == subprocessInSubProcessDiagramId }.shouldHaveSingleItem()
            val dragStartTask = lastValue.filterIsInstance<DraggedToEvent>().filter { it.diagramElementId == serviceTaskStartDiagramId }.shouldHaveSingleItem()
            lastValue.shouldHaveSize(3)

            dragRootSubProcess.dx.shouldBeEqualTo(delta)
            dragRootSubProcess.dy.shouldBeEqualTo(delta)
            dragNestedSubProcess.dx.shouldBeEqualTo(delta)
            dragNestedSubProcess.dy.shouldBeEqualTo(delta)
            dragStartTask.dx.shouldBeEqualTo(delta)
            dragStartTask.dy.shouldBeEqualTo(delta)
        }
    }
}