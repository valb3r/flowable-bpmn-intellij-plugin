package com.valb3r.bpmn.intellij.plugin.flowable.parser

import com.valb3r.bpmn.intellij.plugin.bpmn.api.BpmnProcessObject
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.BpmnElementId
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.BpmnExclusiveGateway
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.BpmnSequenceFlow
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.WithBpmnId
import com.valb3r.bpmn.intellij.plugin.bpmn.api.diagram.DiagramElementId
import com.valb3r.bpmn.intellij.plugin.bpmn.api.diagram.elements.BoundsElement
import com.valb3r.bpmn.intellij.plugin.bpmn.api.diagram.elements.ShapeElement
import com.valb3r.bpmn.intellij.plugin.bpmn.api.diagram.elements.WaypointElement
import com.valb3r.bpmn.intellij.plugin.bpmn.api.events.*
import com.valb3r.bpmn.intellij.plugin.bpmn.api.info.Property
import com.valb3r.bpmn.intellij.plugin.bpmn.api.info.PropertyType
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.Test
import java.nio.charset.StandardCharsets.UTF_8
import java.util.*


internal class FlowableParserTest {

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
    fun `XML process with interlaced elements of same type should be updatable with location without error`() {
        val updated = FlowableParser().update("duplicates.bpmn20.xml".asResource()!!, listOf(
                object: LocationUpdateWithId {
                    override val diagramElementId: DiagramElementId
                        get() = DiagramElementId("BPMNEdge_sid-F270B5BF-127E-422B-BF8D-6A6B7E411D31")
                    override val dx: Float
                        get() = 10.0f
                    override val dy: Float
                        get() = -10.0f
                    override val internalPos: Int?
                        get() = 0
                }
        ))

        updated.shouldNotBeNull()
    }

    @Test
    fun `XML process with interlaced elements of same type should be updatable with new waypoints without error`() {
        val updated = FlowableParser().update("duplicates.bpmn20.xml".asResource()!!, listOf(
                object: NewWaypoints {
                    override val edgeElementId: DiagramElementId
                        get() = DiagramElementId("BPMNEdge_sid-C3BC8962-12B0-482B-B9B5-DCB6551306BD")
                    override val waypoints: List<IdentifiableWaypoint>
                        get() = listOf(
                                WaypointElementState(diagramElementId, 10.0f, 20.0f, 0.0f, 0.0f, true, 1),
                                WaypointElementState(diagramElementId, 30.0f, 40.0f, 0.0f, 0.0f, true, 0)
                        )
                }
        ))

        updated.shouldNotBeNull()
    }

    @Test
    fun `XML process with interlaced elements of same type should be updatable with diagram element removal without error`() {
        val updated = FlowableParser().update("duplicates.bpmn20.xml".asResource()!!, listOf(
                object: DiagramElementRemoved {
                    override val elementId: DiagramElementId
                        get() = DiagramElementId("BPMNEdge_sid-C3BC8962-12B0-482B-B9B5-DCB6551306BD")
                }
        ))

        updated.shouldNotBeNull()
    }

    @Test
    fun `XML process with interlaced elements of same type should be updatable with BPMN element removal without error`() {
        val updated = FlowableParser().update("duplicates.bpmn20.xml".asResource()!!, listOf(
                object: BpmnElementRemoved {
                    override val elementId: BpmnElementId
                        get() = BpmnElementId("serviceTaskStart")
                }
        ))

        updated.shouldNotBeNull()
    }

    @Test
    fun `XML process with interlaced elements of same type should be updatable with BPMN element addition without error`() {
        val updated = FlowableParser().update("duplicates.bpmn20.xml".asResource()!!, listOf(
                object: BpmnShapeObjectAdded {
                    override val bpmnObject: WithBpmnId
                        get() = BpmnExclusiveGateway(bmpnElemId, "Exclusive gateway", null, null)
                    override val shape: ShapeElement
                        get() = ShapeElement(diagramElementId, bpmnObject.id, BoundsElement(0.0f, 0.0f, 30.0f, 40.0f))
                    override val props: Map<PropertyType, Property>
                        get() = mapOf(
                                PropertyType.ID to Property(bpmnObject.id.id),
                                PropertyType.CONDITION_EXPR_VALUE to Property("condition 1"),
                                PropertyType.CONDITION_EXPR_TYPE to Property("a type")
                        )
                }
        ))

        updated.shouldNotBeNull()
    }

    @Test
    fun `XML process with interlaced elements of same type should be updatable with BPMN edge element addition without error`() {
        val updated = FlowableParser().update("duplicates.bpmn20.xml".asResource()!!, listOf(
                object: BpmnEdgeObjectAdded {
                    override val bpmnObject: WithBpmnId
                        get() = BpmnSequenceFlow(bmpnElemId, "Exclusive gateway", null, "source", "target", null)
                    override val edge: EdgeWithIdentifiableWaypoints
                        get() = EdgeElementState(
                                diagramElementId,
                                bpmnObject.id,
                                mutableListOf(
                                        WaypointElementState(diagramElementId, 10.0f, 20.0f, 0.0f, 0.0f, true, 1),
                                        WaypointElementState(diagramElementId, 30.0f, 40.0f, 0.0f, 0.0f, true, 0)
                                )
                        )
                    override val props: Map<PropertyType, Property>
                        get() = mapOf(
                                PropertyType.ID to Property(bpmnObject.id.id),
                                PropertyType.CONDITION_EXPR_VALUE to Property("condition 1"),
                                PropertyType.CONDITION_EXPR_TYPE to Property("a type")
                        )
                }
        ))

        updated.shouldNotBeNull()
    }


    @Test
    fun `XML process with interlaced elements of same type should be updatable with property update event without error`() {
        val updated = FlowableParser().update("duplicates.bpmn20.xml".asResource()!!, listOf(
                object: PropertyUpdateWithId {
                    override val bpmnElementId: BpmnElementId
                        get() = BpmnElementId("activityStart")
                    override val property: PropertyType
                        get() = PropertyType.NAME
                    override val newValue: Any
                        get() = "A new name"
                }
        ))

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
        override val waypoint: MutableList<IdentifiableWaypoint> = mutableListOf()
): EdgeWithIdentifiableWaypoints