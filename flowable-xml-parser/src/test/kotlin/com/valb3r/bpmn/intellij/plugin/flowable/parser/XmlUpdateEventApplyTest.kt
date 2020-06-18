package com.valb3r.bpmn.intellij.plugin.flowable.parser

import com.valb3r.bpmn.intellij.plugin.bpmn.api.BpmnProcessObject
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.BpmnElementId
import com.valb3r.bpmn.intellij.plugin.bpmn.api.diagram.DiagramElementId
import com.valb3r.bpmn.intellij.plugin.bpmn.api.events.Event
import com.valb3r.bpmn.intellij.plugin.bpmn.api.info.PropertyType
import com.valb3r.bpmn.intellij.plugin.flowable.parser.testevents.BooleanValueUpdatedEvent
import com.valb3r.bpmn.intellij.plugin.flowable.parser.testevents.DraggedToEvent
import com.valb3r.bpmn.intellij.plugin.flowable.parser.testevents.StringValueUpdatedEvent
import org.amshove.kluent.*
import org.junit.jupiter.api.Test
import java.util.*

private val EPSILON: Float = 1.0e-6f

internal class XmlUpdateEventApplyTest {

    private val parentDiagramElementId = DiagramElementId("BPMNDiagram_simple-nested")
    private val flatEdgeDiagramId = DiagramElementId("BPMNEdge_linkToSubprocess")
    private val flatServiceTaskDiagramId = DiagramElementId("BPMNShape_serviceTask")

    private val startEventId = BpmnElementId("startEvent")
    private val subProcessId = BpmnElementId("subProcess")
    private val flatServiceTaskId = BpmnElementId("serviceTask")
    private val nestedServiceTaskFirstId = BpmnElementId("nestedServiceTaskFirst")

    private val parser = FlowableParser()

    @Test
    fun `String value update event on flat element works (attribute)`() {
        val newValue = "Start task name"
        val updatedProcess = readAndUpdateProcess(StringValueUpdatedEvent(startEventId, PropertyType.NAME, newValue))

        updatedProcess.process.body!!.startEvent!!.filter { it.id == startEventId }.shouldHaveSingleItem().name.shouldBeEqualTo(newValue)
    }

    @Test
    fun `String value update event on nested element works (attribute)`() {
        val newValue = "Nested service task name"
        val updatedProcess = readAndUpdateProcess(StringValueUpdatedEvent(nestedServiceTaskFirstId, PropertyType.NAME, newValue))

        updatedProcess.process.children!![subProcessId]!!.serviceTask!!.filter { it.id == nestedServiceTaskFirstId }.shouldHaveSingleItem().name.shouldBeEqualTo(newValue)
    }

    @Test
    fun `ID value update event on flat element works (attribute)`() {
        val newValue = "newAwesomeStartEventId"
        val updatedProcess = readAndUpdateProcess(StringValueUpdatedEvent(startEventId, PropertyType.ID, newValue))

        updatedProcess.process.body!!.startEvent!!.filter { it.id == BpmnElementId(newValue) }.shouldHaveSingleItem()
        updatedProcess.process.body!!.startEvent!!.filter { it.id == startEventId }.shouldBeEmpty()
    }

    @Test
    fun `ID value update event on nested element works (attribute)`() {
        val newValue = "newAwesomeNestedServiceId"
        val updatedProcess = readAndUpdateProcess(StringValueUpdatedEvent(nestedServiceTaskFirstId, PropertyType.ID, newValue))

        updatedProcess.process.children!![subProcessId]!!.serviceTask!!.filter { it.id == BpmnElementId(newValue) }.shouldHaveSingleItem()
        updatedProcess.process.children!![subProcessId]!!.serviceTask!!.filter { it.id == nestedServiceTaskFirstId }.shouldBeEmpty()
    }

    @Test
    fun `String value update event on flat element works (CDATA)`() {
        val newValue = "Some new docs"
        val updatedProcess = readAndUpdateProcess(StringValueUpdatedEvent(startEventId, PropertyType.DOCUMENTATION, newValue))

        updatedProcess.process.body!!.startEvent!!.filter { it.id == startEventId }.shouldHaveSingleItem().documentation.shouldBeEqualTo(newValue)
    }

    @Test
    fun `String value update event on nested element works (CDATA)`() {
        val newValue = "Some new docs"
        val updatedProcess = readAndUpdateProcess(StringValueUpdatedEvent(nestedServiceTaskFirstId, PropertyType.DOCUMENTATION, newValue))

        updatedProcess.process.children!![subProcessId]!!.serviceTask!!.filter { it.id == nestedServiceTaskFirstId }.shouldHaveSingleItem().documentation.shouldBeEqualTo(newValue)
    }

    @Test
    fun `Boolean value update event on flat element works (attribute)`() {
        val updatedProcess = readAndUpdateProcess(BooleanValueUpdatedEvent(flatServiceTaskId, PropertyType.ASYNC, true))

        updatedProcess.process.body!!.serviceTask!!.filter { it.id == flatServiceTaskId }.shouldHaveSingleItem().async.shouldBeEqualTo(true)
    }

    @Test
    fun `Boolean value update event on nested element works (attribute)`() {
        val updatedProcess = readAndUpdateProcess(BooleanValueUpdatedEvent(nestedServiceTaskFirstId, PropertyType.ASYNC, true))

        updatedProcess.process.children!![subProcessId]!!.serviceTask!!.filter { it.id == nestedServiceTaskFirstId }.shouldHaveSingleItem().async.shouldBeEqualTo(true)
    }

    // Nesting does not apply to diagram
    @Test
    fun `Dragged to event on flat shape element works`() {
        val updatedProcess = readAndUpdateProcess(DraggedToEvent(flatServiceTaskDiagramId, 10.0f, 10.0f, null, null))

        val draggedElem = updatedProcess.diagram.filter { it.id == parentDiagramElementId }.shouldHaveSingleItem()
                .bpmnPlane.bpmnShape!!.filter {it.id == flatServiceTaskDiagramId}.shouldHaveSingleItem()

        draggedElem.rectBounds().x.shouldBeNear(595.0f, EPSILON)
        draggedElem.rectBounds().y.shouldBeNear(204.5f, EPSILON)
        draggedElem.rectBounds().width.shouldBeNear(100.0f, EPSILON)
        draggedElem.rectBounds().height.shouldBeNear(80.0f, EPSILON)
    }

    // Nesting does not apply to diagram
    @Test
    fun `Dragged to event on flat edge element works`() {
        val updatedProcess = readAndUpdateProcess(
                DraggedToEvent(DiagramElementId(UUID.randomUUID().toString()),  // It is not used by parser
                        10.0f,
                        10.0f,
                        flatEdgeDiagramId,
                        0
                )
        )

        val draggedElem = updatedProcess.diagram.filter { it.id == parentDiagramElementId }.shouldHaveSingleItem()
                .bpmnPlane.bpmnEdge!!.filter {it.id == flatEdgeDiagramId}.shouldHaveSingleItem()

        draggedElem.waypoint!![0].x.shouldBeNear(114.95f, EPSILON)
        draggedElem.waypoint!![0].y.shouldBeNear(244.5f, EPSILON)
    }

    @Test
    fun `New waypoints event on flat element works`() {

    }

    @Test
    fun `New waypoints event on nested element works`() {

    }

    // Nesting does not apply to diagram
    @Test
    fun `Diagram element removed event on flat element works`() {

    }
    

    @Test
    fun `BPMN element removed event on flat element works`() {

    }

    @Test
    fun `BPMN element removed event on nested element works`() {

    }

    // Object added is tested in XmlUpdateEventBpmnObjectAdded

    @Test
    fun `BPMN edge object added event on flat element works`() {

    }

    @Test
    fun `BPMN edge object added event on nested element works`() {

    }

    @Test
    fun `BPMN resize and move event on flat element works`() {

    }

    @Test
    fun `BPMN resize and move event on nested element works`() {

    }

    @Test
    fun `BPMN parent changed event on flat element works`() {

    }

    @Test
    fun `BPMN parent changed event on nested element works`() {

    }

    private fun readAndUpdateProcess(event: Event): BpmnProcessObject {
        val updated = parser.update(
                "simple-nested.bmpn20.xml".asResource()!!,
                listOf(event)
        )

        updated.shouldNotBeNull()

        return parser.parse(updated)
    }
}