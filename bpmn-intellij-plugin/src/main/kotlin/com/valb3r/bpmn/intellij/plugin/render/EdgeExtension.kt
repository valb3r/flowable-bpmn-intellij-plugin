package com.valb3r.bpmn.intellij.plugin.render

import com.google.common.hash.Hashing
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.BpmnElementId
import com.valb3r.bpmn.intellij.plugin.bpmn.api.diagram.DiagramElementId
import com.valb3r.bpmn.intellij.plugin.bpmn.api.diagram.elements.EdgeElement
import com.valb3r.bpmn.intellij.plugin.bpmn.api.diagram.elements.WaypointElement
import com.valb3r.bpmn.intellij.plugin.bpmn.api.events.EdgeWithIdentifiableWaypoints
import com.valb3r.bpmn.intellij.plugin.bpmn.api.events.IdentifiableWaypoint
import java.nio.charset.StandardCharsets.UTF_8
import java.util.*
import kotlin.collections.ArrayList

data class EdgeElementState  (
        override val id: DiagramElementId,
        override val bpmnElement: BpmnElementId?,
        override val waypoint: MutableList<IdentifiableWaypoint> = mutableListOf()
): EdgeWithIdentifiableWaypoints {
    constructor(elem: EdgeElement): this(elem.id, elem.bpmnElement, ArrayList()) {
        elem.waypoint?.withIndex()?.forEach {
            when {
                it.index == 0 -> waypoint += WaypointElementState(it.value, it.index)
                it.index > 0 -> {
                    val midpointX = (elem.waypoint!![it.index - 1].x + it.value.x) / 2.0f
                    val midpointY = (elem.waypoint!![it.index - 1].y + it.value.y) / 2.0f
                    val next = WaypointElementState(it.value, it.index)
                    waypoint += WaypointElementState(childWaypointId(waypoint[it.index - 1], next), midpointX, midpointY, it.index)
                    waypoint += next
                }
            }
        }
    }

    constructor(toCopy: EdgeWithIdentifiableWaypoints, newPhysicalWaypoints: List<IdentifiableWaypoint>): this(toCopy.id, toCopy.bpmnElement, ArrayList()) {
        newPhysicalWaypoints.withIndex().forEach {
            when {
                it.index == 0 -> waypoint += it.value
                it.index > 0 -> {
                    val midpointX = (newPhysicalWaypoints[it.index - 1].x + it.value.x) / 2.0f
                    val midpointY = (newPhysicalWaypoints[it.index - 1].y + it.value.y) / 2.0f
                    val next = it.value
                    waypoint += WaypointElementState(childWaypointId(waypoint[it.index - 1], next), midpointX, midpointY, it.index)
                    waypoint += next
                }
            }
        }
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
        override val internalPos: Int
): IdentifiableWaypoint {

    constructor(elem: WaypointElement, internalPos: Int): this(DiagramElementId(UUID.randomUUID().toString()), elem.x, elem.y, elem.x, elem.y, true, internalPos)
    constructor(id: String, x: Float, y: Float, internalPos: Int): this(DiagramElementId(id), x, y, x, y, false, internalPos)

    override fun copyAndTranslate(dx: Float, dy: Float): WaypointElementState {
        return WaypointElementState(id, x + dx, y + dy, origX, origY, physical, internalPos)
    }

    override fun moveTo(dx: Float, dy: Float): WaypointElementState {
        return WaypointElementState(id, x + dx, y + dy, x + dx, y + dy, physical, internalPos)
    }

    override fun asPhysical(): WaypointElementState {
        return WaypointElementState(id, x, y, origX, origY, true, internalPos)
    }

    override fun originalLocation(): WaypointElementState {
        return WaypointElementState(id, origX, origY, origX, origY, true, internalPos)
    }

    override fun asWaypointElement(): WaypointElement {
        return WaypointElement(x, y)
    }
}