package com.valb3r.bpmn.intellij.plugin.render.dto

import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.BpmnElementId
import com.valb3r.bpmn.intellij.plugin.bpmn.api.diagram.DiagramElementId
import com.valb3r.bpmn.intellij.plugin.bpmn.api.diagram.elements.EdgeElement
import com.valb3r.bpmn.intellij.plugin.bpmn.api.diagram.elements.Translatable
import com.valb3r.bpmn.intellij.plugin.bpmn.api.diagram.elements.WaypointElement
import com.valb3r.bpmn.intellij.plugin.bpmn.api.diagram.elements.WithDiagramId
import java.util.*
import kotlin.collections.ArrayList

data class EdgeElementState  (
        val id: DiagramElementId,
        val bpmnElement: BpmnElementId?,
        val waypoint: MutableList<WaypointElementState> = mutableListOf()
) {
    constructor(elem: EdgeElement): this(elem.id, elem.bpmnElement, ArrayList()) {
        elem.waypoint?.withIndex()?.forEach {
            when {
                it.index == 0 -> waypoint += WaypointElementState(it.value)
                it.index > 0 -> {
                    val midpointX = (elem.waypoint!![it.index - 1].x + it.value.x) / 2.0f
                    val midpointY = (elem.waypoint!![it.index - 1].y + it.value.y) / 2.0f
                    waypoint += WaypointElementState(midpointX, midpointY)
                    waypoint += WaypointElementState(it.value)
                }
            }
        }
    }
}

data class WaypointElementState (
        override val id: DiagramElementId,
        val x: Float,
        val y: Float,
        val physical: Boolean
): Translatable<WaypointElementState>, WithDiagramId {

    constructor(elem: WaypointElement): this(DiagramElementId(UUID.randomUUID().toString()), elem.x, elem.y, true)
    constructor(x: Float, y: Float): this(DiagramElementId(UUID.randomUUID().toString()), x, y, false)

    override fun copyAndTranslate(dx: Float, dy: Float): WaypointElementState {
        return WaypointElementState(id, x + dx, y + dy, physical)
    }

    fun asWaypointElement(): WaypointElement {
        return WaypointElement(x, y)
    }
}