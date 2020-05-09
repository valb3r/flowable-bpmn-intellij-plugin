package com.valb3r.bpmn.intellij.plugin.render

import com.valb3r.bpmn.intellij.plugin.state.CurrentStateProvider

data class RenderContext(
        val canvas: CanvasPainter,
        val selectedIds: Set<String>,
        val dragContext: ElementDragContext,
        val stateProvider: CurrentStateProvider
)