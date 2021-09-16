package com.valb3r.bpmn.intellij.plugin.bpmn.api.diagram.elements

import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.BpmnElementId
import com.valb3r.bpmn.intellij.plugin.bpmn.api.diagram.DiagramElementId

data class EdgeElement  (
        override val id: DiagramElementId,
        val bpmnElement: BpmnElementId?,
        val waypoint: List<WaypointElement>?
): WithDiagramId

data class WaypointElement (
        val x: Float,
        val y: Float
): Translatable<WaypointElement> {

    override fun copyAndTranslate(dx: Float, dy: Float): WaypointElement {
        return WaypointElement(x + dx, y + dy)
    }
}