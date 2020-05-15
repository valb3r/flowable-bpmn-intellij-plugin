package com.valb3r.bpmn.intellij.plugin.render

import com.google.common.hash.Hashing
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.BpmnElementId
import com.valb3r.bpmn.intellij.plugin.bpmn.api.diagram.DiagramElementId
import com.valb3r.bpmn.intellij.plugin.bpmn.api.diagram.elements.EdgeElement
import com.valb3r.bpmn.intellij.plugin.bpmn.api.diagram.elements.WaypointElement
import com.valb3r.bpmn.intellij.plugin.bpmn.api.events.EdgeWithIdentifiableWaypoints
import com.valb3r.bpmn.intellij.plugin.bpmn.api.events.IdentifiableWaypoint
import java.nio.charset.StandardCharsets.UTF_8

data class EdgeElementState  (
        override val id: DiagramElementId,
        override val bpmnElement: BpmnElementId?,
        override val waypoint: MutableList<IdentifiableWaypoint> = mutableListOf(),
        override val epoch: Int
): EdgeWithIdentifiableWaypoints {
    constructor(elem: EdgeElement): this(elem.id, elem.bpmnElement, ArrayList(), 0) {
        elem.waypoint?.withIndex()?.forEach {
            when {
                it.index == 0 -> waypoint += WaypointElementState(waypointId(epoch, 0), it.value, 0)
                it.index > 0 -> {
                    val midpointX = (elem.waypoint!![it.index - 1].x + it.value.x) / 2.0f
                    val midpointY = (elem.waypoint!![it.index - 1].y + it.value.y) / 2.0f
                    val next = WaypointElementState(waypointId(epoch, it.index), it.value, it.index)
                    waypoint += WaypointElementState(childWaypointId(waypoint[it.index - 1], next), midpointX, midpointY, it.index)
                    waypoint += next
                }
            }
        }
    }

    constructor(toCopy: EdgeWithIdentifiableWaypoints, newPhysicalWaypoints: List<IdentifiableWaypoint>, newEpoch: Int): this(toCopy.id, toCopy.bpmnElement, ArrayList(), newEpoch) {
        newPhysicalWaypoints.withIndex().forEach {
            when {
                it.index == 0 -> waypoint += WaypointElementState(waypointId(epoch, 0), it.value.asWaypointElement(), 0)
                it.index > 0 -> {
                    val midpointX = (newPhysicalWaypoints[it.index - 1].x + it.value.x) / 2.0f
                    val midpointY = (newPhysicalWaypoints[it.index - 1].y + it.value.y) / 2.0f
                    val next = WaypointElementState(waypointId(epoch, it.index), it.value.asWaypointElement(), it.index)
                    waypoint += WaypointElementState(childWaypointId(waypoint[it.index - 1], next), midpointX, midpointY, it.index)
                    waypoint += next
                }
            }
        }
    }

    fun waypointId(currentEpoch: Int, internalOrder: Int): String {
        return Hashing.md5().hashString(currentEpoch.toString() + ":" + id.id + ":" + internalOrder, UTF_8).toString()
    }

    fun childWaypointId(start: IdentifiableWaypoint, end: IdentifiableWaypoint): String {
        return Hashing.md5().hashString(start.id.id + ":" + end.id.id, UTF_8).toString()
    }
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

    constructor(id: String, elem: WaypointElement, internalPos: Int): this(DiagramElementId(id), elem.x, elem.y, elem.x, elem.y, true, internalPos)
    constructor(id: String, x: Float, y: Float, internalPos: Int): this(DiagramElementId(id), x, y, x, y, false, internalPos)

    override fun copyAndTranslate(dx: Float, dy: Float): WaypointElementState {
        return WaypointElementState(id, x + dx, y + dy, origX, origY, physical, internalPhysicalPos)
    }

    override fun moveTo(dx: Float, dy: Float): WaypointElementState {
        return WaypointElementState(id, x + dx, y + dy, x + dx, y + dy, physical, internalPhysicalPos)
    }

    override fun asPhysical(): WaypointElementState {
        return WaypointElementState(id, x, y, origX, origY, true, internalPhysicalPos)
    }

    override fun originalLocation(): WaypointElementState {
        return WaypointElementState(id, origX, origY, origX, origY, true, internalPhysicalPos)
    }

    override fun asWaypointElement(): WaypointElement {
        return WaypointElement(x, y)
    }
}