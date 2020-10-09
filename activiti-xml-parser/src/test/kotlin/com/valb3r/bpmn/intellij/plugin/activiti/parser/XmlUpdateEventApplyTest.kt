package com.valb3r.bpmn.intellij.plugin.activiti.parser

import com.valb3r.bpmn.intellij.plugin.activiti.parser.testevents.*
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.BpmnElementId
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.BpmnSequenceFlow
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.WithParentId
import com.valb3r.bpmn.intellij.plugin.bpmn.api.diagram.DiagramElementId
import com.valb3r.bpmn.intellij.plugin.bpmn.api.info.Property
import com.valb3r.bpmn.intellij.plugin.bpmn.api.info.PropertyType
import org.amshove.kluent.*
import org.junit.jupiter.api.Test
import java.util.*

private val EPSILON: Float = 1.0e-6f

internal class XmlUpdateEventApplyTest {

    private val parentDiagramElementId = DiagramElementId("BPMNDiagram_simple-nested")
    private val flatEdgeDiagramId = DiagramElementId("BPMNEdge_linkToSubprocess")
    private val flatServiceTaskDiagramId = DiagramElementId("BPMNShape_serviceTask")

    private val processId = BpmnElementId("simple-nested")
    private val startEventId = BpmnElementId("startEvent")
    private val subProcessId = BpmnElementId("subProcess")
    private val flatServiceTaskId = BpmnElementId("serviceTask")
    private val nestedServiceTaskFirstId = BpmnElementId("nestedServiceTaskFirst")
    private val nestedServiceTaskSecondId = BpmnElementId("nestedServiceTaskSecond")

    private val parser = ActivitiParser()

    @Test
    fun `String value update event on flat element works (attribute)`() {
        val newValue = "Start task name"
        val updatedProcess = readAndUpdateProcess(parser, StringValueUpdatedEvent(startEventId, PropertyType.NAME, newValue))

        updatedProcess.process.body!!.startEvent!!.filter { it.id == startEventId }.shouldHaveSingleItem().name.shouldBeEqualTo(newValue)
    }

    @Test
    fun `String value update event on nested element works (attribute)`() {
        val newValue = "Nested service task name"
        val updatedProcess = readAndUpdateProcess(parser, StringValueUpdatedEvent(nestedServiceTaskFirstId, PropertyType.NAME, newValue))

        updatedProcess.process.children!![subProcessId]!!.serviceTask!!.filter { it.id == nestedServiceTaskFirstId }.shouldHaveSingleItem().name.shouldBeEqualTo(newValue)
    }

    @Test
    fun `ID value update event on flat element works (attribute)`() {
        val newValue = "newAwesomeStartEventId"
        val updatedProcess = readAndUpdateProcess(parser, StringValueUpdatedEvent(startEventId, PropertyType.ID, newValue))

        updatedProcess.process.body!!.startEvent!!.filter { it.id == BpmnElementId(newValue) }.shouldHaveSingleItem()
        updatedProcess.process.body!!.startEvent!!.filter { it.id == startEventId }.shouldBeEmpty()
    }

    @Test
    fun `ID value update event on nested element works (attribute)`() {
        val newValue = "newAwesomeNestedServiceId"
        val updatedProcess = readAndUpdateProcess(parser, StringValueUpdatedEvent(nestedServiceTaskFirstId, PropertyType.ID, newValue))

        updatedProcess.process.children!![subProcessId]!!.serviceTask!!.filter { it.id == BpmnElementId(newValue) }.shouldHaveSingleItem()
        updatedProcess.process.children!![subProcessId]!!.serviceTask!!.filter { it.id == nestedServiceTaskFirstId }.shouldBeEmpty()
    }

    @Test
    fun `String value update event on flat element works (CDATA)`() {
        val newValue = "Some new docs"
        val updatedProcess = readAndUpdateProcess(parser, StringValueUpdatedEvent(startEventId, PropertyType.DOCUMENTATION, newValue))

        updatedProcess.process.body!!.startEvent!!.filter { it.id == startEventId }.shouldHaveSingleItem().documentation.shouldBeEqualTo(newValue)
    }

    @Test
    fun `String value update event on nested element works (CDATA)`() {
        val newValue = "Some new docs"
        val updatedProcess = readAndUpdateProcess(parser, StringValueUpdatedEvent(nestedServiceTaskFirstId, PropertyType.DOCUMENTATION, newValue))

        updatedProcess.process.children!![subProcessId]!!.serviceTask!!.filter { it.id == nestedServiceTaskFirstId }.shouldHaveSingleItem().documentation.shouldBeEqualTo(newValue)
    }

    @Test
    fun `Boolean value update event on flat element works (attribute)`() {
        val updatedProcess = readAndUpdateProcess(parser, BooleanValueUpdatedEvent(flatServiceTaskId, PropertyType.ASYNC, true))

        updatedProcess.process.body!!.serviceTask!!.filter { it.id == flatServiceTaskId }.shouldHaveSingleItem().async.shouldBeEqualTo(true)
    }

    @Test
    fun `Boolean value update event on nested element works (attribute)`() {
        val updatedProcess = readAndUpdateProcess(parser, BooleanValueUpdatedEvent(nestedServiceTaskFirstId, PropertyType.ASYNC, true))

        updatedProcess.process.children!![subProcessId]!!.serviceTask!!.filter { it.id == nestedServiceTaskFirstId }.shouldHaveSingleItem().async.shouldBeEqualTo(true)
    }

    @Test
    fun `Element type pdate event on flat element works (attribute)`() {
        val updatedProcess = readAndUpdateProcess(parser, BooleanValueUpdatedEvent(subProcessId, PropertyType.IS_TRANSACTIONAL_SUBPROCESS, true))

        updatedProcess.process.body!!.transaction!!.filter { it.id == subProcessId }.shouldHaveSingleItem()
        updatedProcess.process.body!!.subProcess.shouldBeNull()
        updatedProcess.process.children!![subProcessId]!!.serviceTask!!.shouldHaveSize(2)
        updatedProcess.process.children!![subProcessId]!!.sequenceFlow!!.shouldHaveSize(1)
    }


    // Nesting does not apply to diagram
    @Test
    fun `Dragged to event on flat shape element works`() {
        val updatedProcess = readAndUpdateProcess(parser, DraggedToEvent(flatServiceTaskDiagramId, 10.0f, 10.0f, null, null))

        val draggedElem = updatedProcess.diagram.filter { it.id == parentDiagramElementId }.shouldHaveSingleItem()
                .bpmnPlane.bpmnShape!!.filter {it.id == flatServiceTaskDiagramId}.shouldHaveSingleItem()

        draggedElem.rectBounds().x.shouldBeNear(514.7f, EPSILON)
        draggedElem.rectBounds().y.shouldBeNear(348.0f, EPSILON)
        draggedElem.rectBounds().width.shouldBeNear(100.0f, EPSILON)
        draggedElem.rectBounds().height.shouldBeNear(80.0f, EPSILON)
    }

    // Nesting does not apply to diagram
    @Test
    fun `Dragged to event on flat edge element works`() {
        val updatedProcess = readAndUpdateProcess(parser, 
                DraggedToEvent(DiagramElementId(UUID.randomUUID().toString()),  // It is not used by parser
                        10.0f,
                        10.0f,
                        flatEdgeDiagramId,
                        0
                )
        )

        val draggedElem = updatedProcess.diagram.filter { it.id == parentDiagramElementId }.shouldHaveSingleItem()
                .bpmnPlane.bpmnEdge!!.filter { it.id == flatEdgeDiagramId }.shouldHaveSingleItem()

        draggedElem.waypoint!![0].x.shouldBeNear(140.0f, EPSILON)
        draggedElem.waypoint!![0].y.shouldBeNear(388.0f, EPSILON)
    }

    // Nesting does not apply to diagram
    @Test
    fun `New waypoints event on flat element works`() {
        val updatedProcess = readAndUpdateProcess(parser, 
                NewWaypointsEvent(
                        flatEdgeDiagramId,
                        listOf(
                                WaypointElementState(DiagramElementId(UUID.randomUUID().toString()), 100.0f, 100.0f, 0.0f, 0.0f, true, 0),
                                WaypointElementState(DiagramElementId(UUID.randomUUID().toString()), 110.0f, 110.0f, 0.0f, 0.0f, true, 1),
                                WaypointElementState(DiagramElementId(UUID.randomUUID().toString()), 120.0f, 120.0f, 0.0f, 0.0f, true, 2)
                        ),
                        1
                )
        )

        val updatedElem = updatedProcess.diagram.filter { it.id == parentDiagramElementId }.shouldHaveSingleItem()
                .bpmnPlane.bpmnEdge!!.filter { it.id == flatEdgeDiagramId }.shouldHaveSingleItem()

        updatedElem.waypoint!![0].x.shouldBeNear(100.0f, EPSILON)
        updatedElem.waypoint!![0].y.shouldBeNear(100.0f, EPSILON)
        updatedElem.waypoint!![1].x.shouldBeNear(110.0f, EPSILON)
        updatedElem.waypoint!![1].y.shouldBeNear(110.0f, EPSILON)
        updatedElem.waypoint!![2].x.shouldBeNear(120.0f, EPSILON)
        updatedElem.waypoint!![2].y.shouldBeNear(120.0f, EPSILON)
    }

    // Nesting does not apply to diagram
    @Test
    fun `Diagram element removed event on flat element works`() {
        val updatedProcess = readAndUpdateProcess(parser, DiagramElementRemovedEvent(flatEdgeDiagramId))

        updatedProcess.diagram.filter { it.id == parentDiagramElementId }.shouldHaveSingleItem()
                .bpmnPlane.bpmnEdge!!.filter { it.id == flatEdgeDiagramId }.shouldBeEmpty()
    }


    @Test
    fun `BPMN element removed event on flat element works`() {
        val updatedProcess = readAndUpdateProcess(parser, BpmnElementRemovedEvent(flatServiceTaskId))

        updatedProcess.process.body!!.serviceTask.shouldBeNull()
    }

    @Test
    fun `BPMN element removed event on nested element works`() {
        val updatedProcess = readAndUpdateProcess(parser, BpmnElementRemovedEvent(nestedServiceTaskFirstId))

        updatedProcess.process.body!!.serviceTask!!.filter { it.id == nestedServiceTaskFirstId }.shouldBeEmpty()
    }

    // BPMN Object added is tested in XmlUpdateEventBpmnObjectAdded

    @Test
    fun `BPMN edge object added event on flat element works`() {
        val id = BpmnElementId(UUID.randomUUID().toString())
        val diagramId = DiagramElementId(UUID.randomUUID().toString())
        val nameOnProp = "A prop name"

        val updatedProcess = readAndUpdateProcess(parser, BpmnEdgeObjectAddedEvent(
                WithParentId(processId, BpmnSequenceFlow(BpmnElementId("foo"), null, null, null, null, null)),
                EdgeElementState(
                        diagramId,
                        id,
                        mutableListOf(
                                WaypointElementState(DiagramElementId(UUID.randomUUID().toString()), 100.0f, 100.0f, 0.0f, 0.0f, true, 0),
                                WaypointElementState(DiagramElementId(UUID.randomUUID().toString()), 110.0f, 110.0f, 0.0f, 0.0f, true, 1)
                        ),
                        0),
                mutableMapOf(Pair(PropertyType.ID, Property(id.id)), Pair(PropertyType.NAME, Property(nameOnProp)))
        ))

        updatedProcess.process.body!!.sequenceFlow!!.filter { it.id == id }.shouldHaveSingleItem().name.shouldBeEqualTo(nameOnProp)
    }

    @Test
    fun `BPMN edge object added event on nested element works`() {
        val id = BpmnElementId(UUID.randomUUID().toString())
        val diagramId = DiagramElementId(UUID.randomUUID().toString())
        val nameOnProp = "A prop name"

        val updatedProcess = readAndUpdateProcess(parser, BpmnEdgeObjectAddedEvent(
                WithParentId(subProcessId, BpmnSequenceFlow(BpmnElementId("foo"), null, null, null, null, null)),
                EdgeElementState(
                        diagramId,
                        id,
                        mutableListOf(
                                WaypointElementState(DiagramElementId(UUID.randomUUID().toString()), 100.0f, 100.0f, 0.0f, 0.0f, true, 0),
                                WaypointElementState(DiagramElementId(UUID.randomUUID().toString()), 110.0f, 110.0f, 0.0f, 0.0f, true, 1)
                        ),
                        0),
                mutableMapOf(Pair(PropertyType.ID, Property(id.id)), Pair(PropertyType.NAME, Property(nameOnProp)))
        ))

        updatedProcess.process.children!![subProcessId]!!.sequenceFlow!!.filter { it.id == id }.shouldHaveSingleItem().name.shouldBeEqualTo(nameOnProp)
        val addedEdge = updatedProcess.diagram.filter { it.id == parentDiagramElementId }.shouldHaveSingleItem().bpmnPlane.bpmnEdge!!.filter { it.id == diagramId }.shouldHaveSingleItem()
        addedEdge.waypoint!![0].x.shouldBeNear(100.0f, EPSILON)
        addedEdge.waypoint!![0].y.shouldBeNear(100.0f, EPSILON)
        addedEdge.waypoint!![1].x.shouldBeNear(110.0f, EPSILON)
        addedEdge.waypoint!![1].y.shouldBeNear(110.0f, EPSILON)
    }

    // Nesting does not apply to diagram
    @Test
    fun `BPMN resize and move event on flat element works`() {
        val updatedProcess = readAndUpdateProcess(parser, BpmnShapeResizedAndMovedEvent(flatServiceTaskDiagramId, 0.0f, 0.0f, 2.0f, 2.0f))

        val draggedElem = updatedProcess.diagram.filter { it.id == parentDiagramElementId }.shouldHaveSingleItem()
                .bpmnPlane.bpmnShape!!.filter {it.id == flatServiceTaskDiagramId}.shouldHaveSingleItem()

        draggedElem.rectBounds().x.shouldBeNear(1009.4f, EPSILON)
        draggedElem.rectBounds().y.shouldBeNear(676.0f, EPSILON)
        draggedElem.rectBounds().width.shouldBeNear(200.0f, EPSILON)
        draggedElem.rectBounds().height.shouldBeNear(160.0f, EPSILON)
    }



    @Test
    fun `BPMN parent changed event on flat element works`() {
        val updatedProcess = readAndUpdateProcess(parser, BpmnParentChangedEvent(flatServiceTaskId, subProcessId))

        updatedProcess.process.body!!.serviceTask.shouldBeNull()
        updatedProcess.process.children!![subProcessId]!!.serviceTask!!.filter { it.id == flatServiceTaskId }.shouldHaveSingleItem()
        updatedProcess.process.children!![subProcessId]!!.serviceTask!!.map { it.id }.shouldContainSame(
                listOf(flatServiceTaskId, nestedServiceTaskFirstId, nestedServiceTaskSecondId)
        )
    }

    @Test
    fun `BPMN parent changed event on nested element works`() {
        val updatedProcess = readAndUpdateProcess(parser, BpmnParentChangedEvent(nestedServiceTaskFirstId, processId))

        updatedProcess.process.children!![subProcessId]!!.serviceTask!!.filter { it.id == nestedServiceTaskFirstId }.shouldBeEmpty()
        updatedProcess.process.body!!.serviceTask!!.filter { it.id == nestedServiceTaskFirstId }.shouldHaveSingleItem()
        updatedProcess.process.body!!.serviceTask!!.map { it.id }.shouldContainSame(
                listOf(flatServiceTaskId, nestedServiceTaskFirstId)
        )
    }

    @Test
    fun `BPMN parent changed event without propagation flag on nested element works`() {
        val updatedProcess = readAndUpdateProcess(parser, BpmnParentChangedEvent(nestedServiceTaskFirstId, processId, false))

        updatedProcess.process.children!![subProcessId]!!.serviceTask!!.filter { it.id == nestedServiceTaskFirstId }.shouldHaveSingleItem()
        updatedProcess.process.body!!.serviceTask!!.filter { it.id == nestedServiceTaskFirstId }.shouldBeEmpty()
        updatedProcess.process.body!!.serviceTask!!.filter { it.id == flatServiceTaskId }.shouldHaveSingleItem()
    }
}