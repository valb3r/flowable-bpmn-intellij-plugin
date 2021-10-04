package com.valb3r.bpmn.intellij.plugin.flowable.parser

import com.valb3r.bpmn.intellij.plugin.bpmn.api.BpmnProcessObject
import com.valb3r.bpmn.intellij.plugin.bpmn.api.PropertyTable
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.BpmnElementId
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.BpmnSequenceFlow
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.WithParentId
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.gateways.BpmnExclusiveGateway
import com.valb3r.bpmn.intellij.plugin.bpmn.api.diagram.DiagramElementId
import com.valb3r.bpmn.intellij.plugin.bpmn.api.diagram.elements.BoundsElement
import com.valb3r.bpmn.intellij.plugin.bpmn.api.diagram.elements.ShapeElement
import com.valb3r.bpmn.intellij.plugin.bpmn.api.info.Property
import com.valb3r.bpmn.intellij.plugin.bpmn.api.info.PropertyType
import com.valb3r.bpmn.intellij.plugin.flowable.parser.testevents.*
import org.amshove.kluent.*
import org.junit.jupiter.api.Test
import java.util.*


internal class FlowableParserDumbTest {

    private val parentElemId = BpmnElementId("duplicates")
    private val bmpnElemId = BpmnElementId(UUID.randomUUID().toString())
    private val diagramElementId = DiagramElementId(UUID.randomUUID().toString())
    
    @Test
    fun `XML process with all Flowable elements is parseable without error`() {
        val processObject: BpmnProcessObject?

        processObject = FlowableParser().parse("popurri.bpmn20.xml".asResource()!!)

        processObject.shouldNotBeNull()
    }

    @Test
    fun `XML process without name should be parseable without error`() {
        val processObject: BpmnProcessObject?

        processObject = FlowableParser().parse("empty-process-name.bpmn20.xml".asResource()!!)

        processObject.shouldNotBeNull()
    }


    @Test
    fun `XML process with interlaced elements of same type should be parseable without error`() {
        val processObject: BpmnProcessObject?

        processObject = FlowableParser().parse("duplicates.bpmn20.xml".asResource()!!)

        processObject.shouldNotBeNull()
    }

    @Test
    fun `XML process with nested subprocess elements of same type should be parseable without error`() {
        val processObject: BpmnProcessObject?

        processObject = FlowableParser().parse("nested.bpmn20.xml".asResource()!!)

        processObject.shouldNotBeNull()
    }

    @Test
    fun `XML process with nested subprocess elements that have interlaced subelems of same type should be parseable without error`() {
        val processObject: BpmnProcessObject?

        processObject = FlowableParser().parse("nested-interlaced.bpmn20.xml".asResource()!!)

        processObject.shouldNotBeNull()
        processObject.process.body!!.serviceTask!!.map { it.id.id }.shouldContainAll(arrayOf("parentInterlaceBeginServiceTask", "parentInterlaceEndServiceTask"))
        processObject.process.children!![BpmnElementId("sid-775FFB07-8CFB-4F82-A6EA-AB0E9BBB79A6")]!!.serviceTask!!.shouldHaveSize(2)
        processObject.process.children!![BpmnElementId("sid-775FFB07-8CFB-4F82-A6EA-AB0E9BBB79A6")]!!.serviceTask!!.map { it.id.id }.shouldContain("nestedServiceTaskInterlaced")
    }

    @Test
    fun `XML process with nested adhoc subprocess elements that have interlaced subelems of same type should be parseable without error`() {
        val processObject: BpmnProcessObject?

        processObject = FlowableParser().parse("nested-interlaced.bpmn20.xml".asResource()!!)

        processObject.shouldNotBeNull()
        processObject.process.body!!.serviceTask!!.map { it.id.id }.shouldContainAll(arrayOf("parentInterlaceBeginServiceTask", "parentInterlaceEndServiceTask"))
        processObject.process.children!![BpmnElementId("sid-5EEB495F-ACAC-4C04-99E1-691D906B3A30")]!!.serviceTask!!.shouldHaveSize(2)
        processObject.process.children!![BpmnElementId("sid-5EEB495F-ACAC-4C04-99E1-691D906B3A30")]!!.serviceTask!!.map { it.id.id }.shouldContain("nestedServiceTaskInterlacedOther")
    }

    @Test
    fun `XML process with nested transactional subprocess elements that have interlaced subelems of same type should be parseable without error`() {
        val processObject: BpmnProcessObject?

        processObject = FlowableParser().parse("nested-interlaced.bpmn20.xml".asResource()!!)

        processObject.shouldNotBeNull()
        processObject.process.body!!.serviceTask!!.map { it.id.id }.shouldContainAll(arrayOf("parentInterlaceBeginServiceTask", "parentInterlaceEndServiceTask"))
        processObject.process.children!![BpmnElementId("sid-1BB4FA80-C87F-4A05-95DF-753D06EE7424")]!!.serviceTask!!.shouldHaveSize(2)
        processObject.process.children!![BpmnElementId("sid-1BB4FA80-C87F-4A05-95DF-753D06EE7424")]!!.serviceTask!!.map { it.id.id }.shouldContain("nestedServiceTaskInterlacedYetOther")
    }

    @Test
    fun `XML process with interlaced elements of same type should be updatable with location without error`() {
        val updated = FlowableParser().update(
                "duplicates.bpmn20.xml".asResource()!!, listOf(
                DraggedToEvent(DiagramElementId("None"), 10.0f, -10.0f, DiagramElementId("BPMNEdge_sid-F270B5BF-127E-422B-BF8D-6A6B7E411D31"), 0)
        ))

        updated.shouldNotBeNull()
    }

    @Test
    fun `XML process with interlaced elements of same type should be updatable with new waypoints without error`() {
        val updated = FlowableParser().update(
                "duplicates.bpmn20.xml".asResource()!!, listOf(
                NewWaypointsEvent(
                        DiagramElementId("BPMNEdge_sid-C3BC8962-12B0-482B-B9B5-DCB6551306BD"),
                        listOf(
                                WaypointElementState(diagramElementId, 10.0f, 20.0f, 0.0f, 0.0f, true, 1),
                                WaypointElementState(diagramElementId, 30.0f, 40.0f, 0.0f, 0.0f, true, 0)
                        ),
                        0
                )
        ))

        updated.shouldNotBeNull()
    }

    @Test
    fun `XML process with interlaced elements of same type should be updatable with diagram element removal without error`() {
        val updated = FlowableParser().update(
                "duplicates.bpmn20.xml".asResource()!!,
                listOf(DiagramElementRemovedEvent(DiagramElementId("BPMNEdge_sid-C3BC8962-12B0-482B-B9B5-DCB6551306BD")))
        )

        updated.shouldNotBeNull()
    }

    @Test
    fun `XML process with interlaced elements of same type should be updatable with BPMN element removal without error`() {
        val updated = FlowableParser().update(
                "duplicates.bpmn20.xml".asResource()!!,
                listOf(BpmnElementRemovedEvent(BpmnElementId("serviceTaskStart"))
        ))

        updated.shouldNotBeNull()
    }

    @Test
    fun `XML process with interlaced elements of same type should be updatable with BPMN element addition without error`() {
        val updated = FlowableParser().update(
                "duplicates.bpmn20.xml".asResource()!!,
                listOf(BpmnShapeObjectAddedEvent(
                        WithParentId(parentElemId, BpmnExclusiveGateway(bmpnElemId, "Exclusive gateway", null, null)),
                        ShapeElement(diagramElementId, bmpnElemId, BoundsElement(0.0f, 0.0f, 30.0f, 40.0f)),
                        PropertyTable(
                            mutableMapOf(
                                PropertyType.ID to mutableListOf(Property(bmpnElemId.id)),
                                PropertyType.CONDITION_EXPR_VALUE to mutableListOf(Property("condition 1")),
                                PropertyType.CONDITION_EXPR_TYPE to mutableListOf(Property("a type"))
                            )
                        )
                )
        ))

        updated.shouldNotBeNull()
    }

    @Test
    fun `XML process with interlaced elements of same type should be updatable with BPMN edge element addition without error`() {
        val updated = FlowableParser().update(
                "duplicates.bpmn20.xml".asResource()!!,
                listOf(
                        BpmnEdgeObjectAddedEvent(
                                WithParentId(parentElemId, BpmnSequenceFlow(bmpnElemId, "Exclusive gateway", null, "source", "target", null)),
                                EdgeElementState(
                                        diagramElementId,
                                        bmpnElemId,
                                        mutableListOf(
                                                WaypointElementState(diagramElementId, 10.0f, 20.0f, 0.0f, 0.0f, true, 1),
                                                WaypointElementState(diagramElementId, 30.0f, 40.0f, 0.0f, 0.0f, true, 0)
                                        ),
                                        0),
                                PropertyTable(
                                    mutableMapOf(
                                        PropertyType.ID to mutableListOf(Property(bmpnElemId.id)),
                                        PropertyType.CONDITION_EXPR_VALUE to mutableListOf(Property("condition 1")),
                                        PropertyType.CONDITION_EXPR_TYPE to mutableListOf(Property("a type"))
                                    )
                                )
                        )
                )
        )

        updated.shouldNotBeNull()
    }

    @Test
    fun `New edge should have correct parent in diagram`() {
        val newId = BpmnElementId("SID-132131231")
        val updated = FlowableParser().update(
                "nested.bpmn20.xml".asResource()!!,
                listOf(
                        BpmnEdgeObjectAddedEvent(
                                WithParentId(
                                        BpmnElementId("sid-3AD3FAD5-389C-4066-8CB0-C4090CA91F6D"),
                                        BpmnSequenceFlow(newId, "Exclusive gateway", null, "source", "target", null)),
                                EdgeElementState(
                                        diagramElementId,
                                        newId,
                                        mutableListOf(
                                                WaypointElementState(diagramElementId, 10.0f, 20.0f, 0.0f, 0.0f, true, 1),
                                                WaypointElementState(diagramElementId, 30.0f, 40.0f, 0.0f, 0.0f, true, 0)
                                        ),
                                        0),
                                PropertyTable(
                                    mutableMapOf(
                                        PropertyType.ID to mutableListOf(Property(newId.id)),
                                        PropertyType.CONDITION_EXPR_VALUE to mutableListOf(Property("condition 1")),
                                        PropertyType.CONDITION_EXPR_TYPE to mutableListOf(Property("a type"))
                                    )
                                )
                        )
                )
        )

        updated.shouldNotBeNull()
        val updatedProcess = FlowableParser().parse(updated)
        updatedProcess.process.children!![BpmnElementId("sid-3AD3FAD5-389C-4066-8CB0-C4090CA91F6D")]!!.sequenceFlow!!.map { it.id }.shouldContain(newId)
    }

    @Test
    fun `XML should properly handle element resize event`() {
        val updated = FlowableParser().update(
                "nested.bpmn20.xml".asResource()!!,
                listOf(BpmnShapeResizedAndMovedEvent(DiagramElementId("BPMNShape_sid-0E2068A3-FEF1-46A1-AD2B-7DCD0003AA65"), 10.0f, -10.0f, 0.5f, 0.5f))
        )

        updated.shouldNotBeNull()
    }

    @Test
    fun `XML should properly handle parent change event`() {
        val updated = FlowableParser().update(
                "nested.bpmn20.xml".asResource()!!,
                listOf(BpmnParentChangedEvent(
                        BpmnElementId("sid-57A163D8-81CB-4B71-B74C-DD4A152B6653"),
                        BpmnElementId("sid-5EEB495F-ACAC-4C04-99E1-691D906B3A30")
                )
                ))

        updated.shouldNotBeNull()
    }

    @Test
    fun `XML process with interlaced elements of same type should be updatable with element type change`() {
        val updated = FlowableParser().update(
                "nested-interlaced.bpmn20.xml".asResource()!!,
                listOf(BooleanValueUpdatedEvent(BpmnElementId("sid-C4389D7E-1083-47D2-BECC-99479E63D18B"), PropertyType.IS_TRANSACTIONAL_SUBPROCESS, true))
        )

        updated.shouldNotBeNull()
        val processObject = FlowableParser().parse(updated)
        processObject.process.body!!.transaction!!.map { it.id.id }.shouldContain("sid-C4389D7E-1083-47D2-BECC-99479E63D18B")
    }

    @Test
    fun `XML process with interlaced elements of same type should be updatable with property update event without error`() {
        val updated = FlowableParser().update(
                "duplicates.bpmn20.xml".asResource()!!,
                listOf(StringValueUpdatedEvent(BpmnElementId("activityStart"), PropertyType.NAME, "A new name"))
        )

        updated.shouldNotBeNull()
    }

    @Test
    fun `Empty CDATA is removed`() {
        val updated = FlowableParser().update(
                "popurri.bpmn20.xml".asResource()!!,
                listOf(StringValueUpdatedEvent(BpmnElementId("onGatewayOk"), PropertyType.CONDITION_EXPR_VALUE, ""))
        )

        updated.shouldNotBeNull()
        val updatedProcess = FlowableParser().parse(updated)
        val sequenceFlow = updatedProcess.process.body!!.sequenceFlow!!.filter { it.id.id == "onGatewayOk"}.shouldHaveSingleItem()
        sequenceFlow.conditionExpression!!.text.shouldBeNull()
    }

    @Test
    fun `Empty text is removed`() {
        val updated = FlowableParser().update(
                "popurri.bpmn20.xml".asResource()!!,
                listOf(StringValueUpdatedEvent(BpmnElementId("onGatewayNokId"), PropertyType.CONDITION_EXPR_VALUE, ""))
        )

        updated.shouldNotBeNull()
        val updatedProcess = FlowableParser().parse(updated)
        val sequenceFlow = updatedProcess.process.body!!.sequenceFlow!!.filter { it.id.id == "onGatewayNokId"}.shouldHaveSingleItem()
        sequenceFlow.conditionExpression!!.text.shouldBeNull()
    }
}

