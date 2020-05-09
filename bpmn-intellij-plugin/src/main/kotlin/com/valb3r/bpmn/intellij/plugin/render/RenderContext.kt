package com.valb3r.bpmn.intellij.plugin.render

import com.valb3r.bpmn.intellij.plugin.bpmn.api.diagram.DiagramElementId
import com.valb3r.bpmn.intellij.plugin.state.CurrentStateProvider

data class RenderContext(
        val canvas: CanvasPainter,
        val selectedIds: Set<DiagramElementId>,
        val dragContext: ElementDragContext,
        val stateProvider: CurrentStateProvider
)