package com.valb3r.bpmn.intellij.plugin.flowable.parser

import com.valb3r.bpmn.intellij.plugin.bpmn.api.BpmnProcessObject
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.BpmnElementId
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.BpmnSequenceFlow
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.WithParentId
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.gateways.BpmnExclusiveGateway
import com.valb3r.bpmn.intellij.plugin.bpmn.api.diagram.DiagramElementId
import com.valb3r.bpmn.intellij.plugin.bpmn.api.diagram.elements.BoundsElement
import com.valb3r.bpmn.intellij.plugin.bpmn.api.diagram.elements.ShapeElement
import com.valb3r.bpmn.intellij.plugin.bpmn.api.diagram.elements.WaypointElement
import com.valb3r.bpmn.intellij.plugin.bpmn.api.events.*
import com.valb3r.bpmn.intellij.plugin.bpmn.api.info.Property
import com.valb3r.bpmn.intellij.plugin.bpmn.api.info.PropertyType
import org.amshove.kluent.shouldContain
import org.amshove.kluent.shouldContainAll
import org.amshove.kluent.shouldHaveSize
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.Test
import java.awt.geom.Point2D
import java.nio.charset.StandardCharsets.UTF_8
import java.util.*


internal class FlowableParserTest {

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
    fun `XML process with interlaced elements of same type should be updatable with property update event without error`() {
        val updated = FlowableParser().update(
                "duplicates.bpmn20.xml".asResource()!!,
                listOf(StringValueUpdatedEvent(BpmnElementId("activityStart"), PropertyType.NAME, "A new name"))
        )

        updated.shouldNotBeNull()
    }

    fun String.asResource(): String? = object {}::class.java.classLoader.getResource(this)?.readText(UTF_8)
}

data class WaypointElementState (
        override val id: DiagramElementId,
        override val x: Float,
        override val y: Float,
        override val origX: Float,
        override val origY: Float,
        override val physical: Boolean,
        override val internalPhysicalPos: Int
): IdentifiableWaypoint {

    override fun moveTo(dx: Float, dy: Float): IdentifiableWaypoint {
        TODO("Not yet implemented")
    }

    override fun asPhysical(): IdentifiableWaypoint {
        TODO("Not yet implemented")
    }

    override fun originalLocation(): IdentifiableWaypoint {
        TODO("Not yet implemented")
    }

    override fun asWaypointElement(): WaypointElement {
        TODO("Not yet implemented")
    }

    override fun copyAndTranslate(dx: Float, dy: Float): IdentifiableWaypoint {
        TODO("Not yet implemented")
    }
}

data class EdgeElementState  (
        override val id: DiagramElementId,
        override val bpmnElement: BpmnElementId?,
        override val waypoint: MutableList<IdentifiableWaypoint> = mutableListOf(),
        override val epoch: Int
): EdgeWithIdentifiableWaypoints {

    override fun updateBpmnElemId(newId: BpmnElementId): EdgeWithIdentifiableWaypoints {
        TODO("Not yet implemented")
    }
}

data class StringValueUpdatedEvent(override val bpmnElementId: BpmnElementId, override val property: PropertyType, override val newValue: String, override val referencedValue: String? = null, override val newIdValue: BpmnElementId? = null): PropertyUpdateWithId

data class BooleanValueUpdatedEvent(override val bpmnElementId: BpmnElementId, override val property: PropertyType, override val newValue: Boolean, override val referencedValue: Boolean? = null, override val newIdValue: BpmnElementId? = null): PropertyUpdateWithId

data class DraggedToEvent(override val diagramElementId: DiagramElementId, override val dx: Float, override val dy: Float, override val parentElementId: DiagramElementId?, override val internalPos: Int?): LocationUpdateWithId

data class NewWaypointsEvent(override val edgeElementId: DiagramElementId, override val waypoints: List<IdentifiableWaypoint>, override val epoch: Int): NewWaypoints

data class DiagramElementRemovedEvent(override val elementId: DiagramElementId): DiagramElementRemoved

data class BpmnElementRemovedEvent(override val elementId: BpmnElementId): BpmnElementRemoved

data class BpmnShapeObjectAddedEvent(override val bpmnObject: WithParentId, override val shape: ShapeElement, override val props: Map<PropertyType, Property>): BpmnShapeObjectAdded

data class BpmnEdgeObjectAddedEvent(override val bpmnObject: WithParentId, override val edge: EdgeWithIdentifiableWaypoints, override val props: Map<PropertyType, Property>): BpmnEdgeObjectAdded

data class BpmnShapeResizedAndMovedEvent(override val diagramElementId: DiagramElementId, override val cx: Float, override val cy: Float, override val coefW: Float, override val coefH: Float): BpmnShapeResizedAndMoved {

    override fun transform(point: Point2D.Float): Point2D.Float {
        return Point2D.Float(cx + (point.x - cx) * coefW, cy + (point.y - cy) * coefH)
    }
}

data class BpmnParentChangedEvent(override val bpmnElementId: BpmnElementId, override val newParentId: BpmnElementId): BpmnParentChanged