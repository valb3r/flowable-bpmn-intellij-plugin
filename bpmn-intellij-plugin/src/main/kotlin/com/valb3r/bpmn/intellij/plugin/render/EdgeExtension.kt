package com.valb3r.bpmn.intellij.plugin.render

import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.BpmnElementId
import com.valb3r.bpmn.intellij.plugin.bpmn.api.diagram.DiagramElementId
import com.valb3r.bpmn.intellij.plugin.bpmn.api.diagram.elements.EdgeElement
import com.valb3r.bpmn.intellij.plugin.bpmn.api.diagram.elements.WaypointElement
import com.valb3r.bpmn.intellij.plugin.bpmn.api.events.EdgeWithIdentifiableWaypoints
import com.valb3r.bpmn.intellij.plugin.bpmn.api.events.IdentifiableWaypoint
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
                it.index == 0 -> waypoint += WaypointElementState(it.value)
                it.index > 0 -> {
                    val midpointX = (elem.waypoint!![it.index - 1].x + it.value.x) / 2.0f
                    val midpointY = (elem.waypoint!![it.index - 1].y + it.value.y) / 2.0f
                    val next = WaypointElementState(it.value)
                    waypoint += WaypointElementState(waypoint[it.index - 1].id.id + ":" + next.id.id, midpointX, midpointY)
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
                    waypoint += WaypointElementState(waypoint[it.index - 1].id.id + ":" + next.id.id, midpointX, midpointY)
                    waypoint += next
                }
            }
        }
    }
}

data class WaypointElementState (
        override val id: DiagramElementId,
        override val x: Float,
        override val y: Float,
        override val origX: Float,
        override val origY: Float,
        override val physical: Boolean
): IdentifiableWaypoint {

    constructor(elem: WaypointElement): this(DiagramElementId(UUID.randomUUID().toString()), elem.x, elem.y, elem.x, elem.y, true)
    constructor(id: String, x: Float, y: Float): this(DiagramElementId(id), x, y, x, y, false)

    override fun copyAndTranslate(dx: Float, dy: Float): WaypointElementState {
        return WaypointElementState(id, x + dx, y + dy, origX, origY, physical)
    }

    override fun moveTo(dx: Float, dy: Float): WaypointElementState {
        return WaypointElementState(id, x + dx, y + dy, x + dx, y + dy, physical)
    }

    override fun asPhysical(): WaypointElementState {
        return WaypointElementState(id, x, y, origX, origY, true)
    }

    override fun originalLocation(): WaypointElementState {
        return WaypointElementState(id, origX, origY, origX, origY, true)
    }

    override fun asWaypointElement(): WaypointElement {
        return WaypointElement(x, y)
    }
}