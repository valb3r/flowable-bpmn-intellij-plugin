package com.valb3r.bpmn.intellij.plugin.render

import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.BpmnElementId
import com.valb3r.bpmn.intellij.plugin.bpmn.api.diagram.DiagramElementId
import com.valb3r.bpmn.intellij.plugin.events.ProcessModelUpdateEvents
import java.awt.geom.Point2D

data class ElementInteractionContext(
        val draggedIds: Set<DiagramElementId>,
        val dragEndCallbacks: MutableMap<DiagramElementId, (dx: Float, dy: Float, dest: ProcessModelUpdateEvents, droppedOn: BpmnElementId?) -> Unit>,
        val clickCallbacks: MutableMap<DiagramElementId, (dest: ProcessModelUpdateEvents) -> Unit>,
        val anchorsHit: AnchorHit?,
        val start: Point2D.Float,
        val current: Point2D.Float
)

data class AnchorHit(val dragged: Point2D.Float, val anchors: Map<AnchorType, Point2D.Float>)

enum class AnchorType {
    VERTICAL,
    HORIZONTAL,
    POINT
}