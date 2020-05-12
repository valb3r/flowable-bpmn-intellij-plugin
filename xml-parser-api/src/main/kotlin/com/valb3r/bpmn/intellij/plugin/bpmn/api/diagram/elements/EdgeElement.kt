package com.valb3r.bpmn.intellij.plugin.bpmn.api.diagram.elements

import com.github.pozo.KotlinBuilder
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.BpmnElementId
import com.valb3r.bpmn.intellij.plugin.bpmn.api.diagram.DiagramElementId

@KotlinBuilder
data class EdgeElement  (
        val id: DiagramElementId,
        val bpmnElement: BpmnElementId?,
        val waypoint: List<WaypointElement>?
)

@KotlinBuilder
data class WaypointElement (
        val x: Float,
        val y: Float
): Translatable<WaypointElement> {

    override fun copyAndTranslate(dx: Float, dy: Float): WaypointElement {
        return WaypointElement(x + dx, y + dy)
    }
}