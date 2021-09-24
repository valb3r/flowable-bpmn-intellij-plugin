package com.valb3r.bpmn.intellij.plugin.activiti.parser

import com.valb3r.bpmn.intellij.plugin.activiti.parser.testevents.*
import com.valb3r.bpmn.intellij.plugin.bpmn.api.BpmnProcessObject
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.BpmnElementId
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.BpmnSequenceFlow
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.WithParentId
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.gateways.BpmnExclusiveGateway
import com.valb3r.bpmn.intellij.plugin.bpmn.api.diagram.DiagramElementId
import com.valb3r.bpmn.intellij.plugin.bpmn.api.diagram.elements.BoundsElement
import com.valb3r.bpmn.intellij.plugin.bpmn.api.diagram.elements.ShapeElement
import com.valb3r.bpmn.intellij.plugin.bpmn.api.info.Property
import com.valb3r.bpmn.intellij.plugin.bpmn.api.info.PropertyType
import org.amshove.kluent.*
import org.junit.jupiter.api.Test
import java.util.*


internal class ActivityParserDumbTest {

    private val parentElemId = BpmnElementId("duplicates")
    private val bmpnElemId = BpmnElementId(UUID.randomUUID().toString())
    private val diagramElementId = DiagramElementId(UUID.randomUUID().toString())
    
    @Test
    fun `XML process with all Activiti elements is parseable without error`() {
        val processObject: BpmnProcessObject?

        processObject = ActivitiParser().parse("popurri.bpmn20.xml".asResource()!!)

        processObject.shouldNotBeNull()
    }

    @Test
    fun `XML process without name should be parseable without error`() {
        val processObject: BpmnProcessObject?

        processObject = ActivitiParser().parse("empty-process-name.bpmn20.xml".asResource()!!)

        processObject.shouldNotBeNull()
    }


    @Test
    fun `XML process with interlaced elements of same type should be parseable without error`() {
        val processObject: BpmnProcessObject?

        processObject = ActivitiParser().parse("duplicates.bpmn20.xml".asResource()!!)

        processObject.shouldNotBeNull()
    }

    @Test
    fun `XML process with nested subprocess elements of same type should be parseable without error`() {
        val processObject: BpmnProcessObject?

        processObject = ActivitiParser().parse("nested.bpmn20.xml".asResource()!!)

        processObject.shouldNotBeNull()
    }

    @Test
    fun `XML process with nested subprocess elements that have interlaced subelems of same type should be parseable without error`() {
        val processObject: BpmnProcessObject?

        processObject = ActivitiParser().parse("nested-interlaced.bpmn20.xml".asResource()!!)

        processObject.shouldNotBeNull()
        processObject.process.body!!.serviceTask!!.map { it.id.id }.shouldContainAll(arrayOf("parentInterlaceBeginServiceTask", "parentInterlaceEndServiceTask"))
        processObject.process.children!![BpmnElementId("sid-9E62AF47-D4DF-4492-BA2F-E531CEB29A03")]!!.serviceTask!!.shouldHaveSize(2)
        processObject.process.children!![BpmnElementId("sid-9E62AF47-D4DF-4492-BA2F-E531CEB29A03")]!!.serviceTask!!.map { it.id.id }.shouldContain("nestedServiceTaskInterlaced")
    }

    @Test
    fun `XML process with nested other subprocess elements that have interlaced subelems of same type should be parseable without error`() {
        val processObject: BpmnProcessObject?

        processObject = ActivitiParser().parse("nested-interlaced.bpmn20.xml".asResource()!!)

        processObject.shouldNotBeNull()
        processObject.process.body!!.serviceTask!!.map { it.id.id }.shouldContainAll(arrayOf("parentInterlaceBeginServiceTask", "parentInterlaceEndServiceTask"))
        processObject.process.children!![BpmnElementId("sid-0B5D0923-5542-44DA-B86D-C3E4B2883DC2")]!!.serviceTask!!.shouldHaveSize(2)
        processObject.process.children!![BpmnElementId("sid-0B5D0923-5542-44DA-B86D-C3E4B2883DC2")]!!.serviceTask!!.map { it.id.id }.shouldContain("nestedServiceTaskInterlacedOther")
    }

    @Test
    fun `XML process with nested transactional subprocess elements that have interlaced subelems of same type should be parseable without error`() {
        val processObject: BpmnProcessObject?

        processObject = ActivitiParser().parse("nested-interlaced.bpmn20.xml".asResource()!!)

        processObject.shouldNotBeNull()
        processObject.process.body!!.serviceTask!!.map { it.id.id }.shouldContainAll(arrayOf("parentInterlaceBeginServiceTask", "parentInterlaceEndServiceTask"))
        processObject.process.children!![BpmnElementId("sid-77F95F37-ADC3-4EBB-8F21-AEF1C015D5EB")]!!.serviceTask!!.shouldHaveSize(2)
        processObject.process.children!![BpmnElementId("sid-77F95F37-ADC3-4EBB-8F21-AEF1C015D5EB")]!!.serviceTask!!.map { it.id.id }.shouldContain("nestedServiceTaskInterlacedYetOther")
    }

    @Test
    fun `XML process with interlaced elements of same type should be updatable with location without error`() {
        val updated = ActivitiParser().update(
                "duplicates.bpmn20.xml".asResource()!!, listOf(
                DraggedToEvent(DiagramElementId("None"), 10.0f, -10.0f, DiagramElementId("BPMNEdge_sid-133977D1-12A2-4FB0-A0C5-2FEB46A73650"), 0)
        ))

        updated.shouldNotBeNull()
    }

    @Test
    fun `XML process with interlaced elements of same type should be updatable with new waypoints without error`() {
        val updated = ActivitiParser().update(
                "duplicates.bpmn20.xml".asResource()!!, listOf(
                NewWaypointsEvent(
                        DiagramElementId("BPMNEdge_sid-03CC5170-53C5-49F5-84F4-F93E4AEE7FAC"),
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
        val updated = ActivitiParser().update(
                "duplicates.bpmn20.xml".asResource()!!,
                listOf(DiagramElementRemovedEvent(DiagramElementId("BPMNEdge_sid-03CC5170-53C5-49F5-84F4-F93E4AEE7FAC")))
        )

        updated.shouldNotBeNull()
    }

    @Test
    fun `XML process with interlaced elements of same type should be updatable with BPMN element removal without error`() {
        val updated = ActivitiParser().update(
                "duplicates.bpmn20.xml".asResource()!!,
                listOf(BpmnElementRemovedEvent(BpmnElementId("serviceTaskStart"))
        ))

        updated.shouldNotBeNull()
    }

    @Test
    fun `XML process with interlaced elements of same type should be updatable with BPMN element addition without error`() {
        val updated = ActivitiParser().update(
                "duplicates.bpmn20.xml".asResource()!!,
                listOf(BpmnShapeObjectAddedEvent(
                        WithParentId(parentElemId, BpmnExclusiveGateway(bmpnElemId, "Exclusive gateway", null, null)),
                        ShapeElement(diagramElementId, bmpnElemId, BoundsElement(0.0f, 0.0f, 30.0f, 40.0f)),
                        mapOf(
                                PropertyType.ID to Property(bmpnElemId.id),
                                PropertyType.CONDITION_EXPR_VALUE to Property("condition 1"),
                                PropertyType.CONDITION_EXPR_TYPE to Property("a type")
                        )
                )
        ))

        updated.shouldNotBeNull()
    }

    @Test
    fun `XML process with interlaced elements of same type should be updatable with BPMN edge element addition without error`() {
        val updated = ActivitiParser().update(
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
                                mapOf(
                                        PropertyType.ID to Property(bmpnElemId.id),
                                        PropertyType.CONDITION_EXPR_VALUE to Property("condition 1"),
                                        PropertyType.CONDITION_EXPR_TYPE to Property("a type")
                                )
                        )
                )
        )

        updated.shouldNotBeNull()
    }

    @Test
    fun `New edge should have correct parent in diagram`() {
        val newId = BpmnElementId("SID-132131231")
        val updated = ActivitiParser().update(
                "nested.bpmn20.xml".asResource()!!,
                listOf(
                        BpmnEdgeObjectAddedEvent(
                                WithParentId(
                                        BpmnElementId("sid-1334170C-BA4D-4387-99BD-44229D18942C"),
                                        BpmnSequenceFlow(newId, "Exclusive gateway", null, "source", "target", null)),
                                EdgeElementState(
                                        diagramElementId,
                                        newId,
                                        mutableListOf(
                                                WaypointElementState(diagramElementId, 10.0f, 20.0f, 0.0f, 0.0f, true, 1),
                                                WaypointElementState(diagramElementId, 30.0f, 40.0f, 0.0f, 0.0f, true, 0)
                                        ),
                                        0),
                                mapOf(
                                        PropertyType.ID to Property(newId.id),
                                        PropertyType.CONDITION_EXPR_VALUE to Property("condition 1"),
                                        PropertyType.CONDITION_EXPR_TYPE to Property("a type")
                                )
                        )
                )
        )

        updated.shouldNotBeNull()
        val updatedProcess = ActivitiParser().parse(updated)
        updatedProcess.process.children!![BpmnElementId("sid-1334170C-BA4D-4387-99BD-44229D18942C")]!!.sequenceFlow!!.map { it.id }.shouldContain(newId)
    }

    @Test
    fun `XML should properly handle element resize event`() {
        val updated = ActivitiParser().update(
                "nested.bpmn20.xml".asResource()!!,
                listOf(BpmnShapeResizedAndMovedEvent(DiagramElementId("BPMNShape_sid-BA8B2192-D3B8-41E3-B36E-E3E3DD720F85"), 10.0f, -10.0f, 0.5f, 0.5f))
        )

        updated.shouldNotBeNull()
    }

    @Test
    fun `XML should properly handle parent change event`() {
        val updated = ActivitiParser().update(
                "nested.bpmn20.xml".asResource()!!,
                listOf(BpmnParentChangedEvent(
                        BpmnElementId("sid-C4B5DA0B-B84F-4EA1-8292-EB3C888D3453"),
                        BpmnElementId("sid-0B5D0923-5542-44DA-B86D-C3E4B2883DC2")
                )
                ))

        updated.shouldNotBeNull()
    }

    @Test
    fun `XML process with interlaced elements of same type should be updatable with element type change`() {
        val updated = ActivitiParser().update(
                "nested-interlaced.bpmn20.xml".asResource()!!,
                listOf(BooleanValueUpdatedEvent(BpmnElementId("sid-9DBEBCA6-7BE8-4170-ACC3-4548A2244C40"), PropertyType.IS_TRANSACTIONAL_SUBPROCESS, true))
        )

        updated.shouldNotBeNull()
        val processObject = ActivitiParser().parse(updated)
        processObject.process.body!!.transaction!!.map { it.id.id }.shouldContain("sid-9DBEBCA6-7BE8-4170-ACC3-4548A2244C40")
    }

    @Test
    fun `XML process with interlaced elements of same type should be updatable with property update event without error`() {
        val updated = ActivitiParser().update(
                "duplicates.bpmn20.xml".asResource()!!,
                listOf(StringValueUpdatedEvent(BpmnElementId("activityStart"), PropertyType.NAME, "A new name"))
        )

        updated.shouldNotBeNull()
    }

    @Test
    fun `Empty CDATA is removed`() {
        val updated = ActivitiParser().update(
                "popurri.bpmn20.xml".asResource()!!,
                listOf(StringValueUpdatedEvent(BpmnElementId("onGatewayOk"), PropertyType.CONDITION_EXPR_VALUE, ""))
        )

        updated.shouldNotBeNull()
        val updatedProcess = ActivitiParser().parse(updated)
        val sequenceFlow = updatedProcess.process.body!!.sequenceFlow!!.filter { it.id.id == "onGatewayOk"}.shouldHaveSingleItem()
        sequenceFlow.conditionExpression!!.text.shouldBeNull()
    }

    @Test
    fun `Empty text is removed`() {
        val updated = ActivitiParser().update(
                "popurri.bpmn20.xml".asResource()!!,
                listOf(StringValueUpdatedEvent(BpmnElementId("onGatewayNokId"), PropertyType.CONDITION_EXPR_VALUE, ""))
        )

        updated.shouldNotBeNull()
        val updatedProcess = ActivitiParser().parse(updated)
        val sequenceFlow = updatedProcess.process.body!!.sequenceFlow!!.filter { it.id.id == "onGatewayNokId"}.shouldHaveSingleItem()
        sequenceFlow.conditionExpression!!.text.shouldBeNull()
    }
}

