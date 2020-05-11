package com.valb3r.bpmn.intellij.plugin.render

import com.valb3r.bpmn.intellij.plugin.bpmn.api.diagram.DiagramElementId

data class Draggable(val id: DiagramElementId, val onDragEnd: (dx: Float, dy: Float) -> Unit)