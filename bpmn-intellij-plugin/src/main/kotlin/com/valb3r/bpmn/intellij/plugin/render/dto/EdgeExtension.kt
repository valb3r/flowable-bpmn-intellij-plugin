package com.valb3r.bpmn.intellij.plugin.render.dto

import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.BpmnElementId
import com.valb3r.bpmn.intellij.plugin.bpmn.api.diagram.DiagramElementId
import com.valb3r.bpmn.intellij.plugin.bpmn.api.diagram.elements.EdgeElement
import com.valb3r.bpmn.intellij.plugin.bpmn.api.diagram.elements.Translatable
import com.valb3r.bpmn.intellij.plugin.bpmn.api.diagram.elements.WaypointElement
import com.valb3r.bpmn.intellij.plugin.bpmn.api.diagram.elements.WithDiagramId
import java.util.*

data class EdgeElementState  (
        val id: DiagramElementId,
        val bpmnElement: BpmnElementId?,
        val waypoint: List<WaypointElementState> = mutableListOf()
) {
    constructor(elem: EdgeElement): this(elem.id, elem.bpmnElement, elem.waypoint?.map { WaypointElementState(it) } ?: emptyList())
}

data class WaypointElementState (
        override val id: DiagramElementId,
        val x: Float,
        val y: Float
): Translatable<WaypointElementState>, WithDiagramId {

    constructor(elem: WaypointElement): this(DiagramElementId(UUID.randomUUID().toString()), elem.x, elem.y)

    override fun copyAndTranslate(dx: Float, dy: Float): WaypointElementState {
        return WaypointElementState(id, x + dx, y + dy)
    }

    fun asWaypointElement(): WaypointElement {
        return WaypointElement(x, y)
    }
}