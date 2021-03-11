package com.valb3r.bpmn.intellij.plugin.core.render

import com.intellij.openapi.project.Project
import com.valb3r.bpmn.intellij.plugin.bpmn.api.diagram.DiagramElementId
import com.valb3r.bpmn.intellij.plugin.core.state.CurrentStateProvider

data class RenderContext(
        val project: Project,
        val canvas: CanvasPainter,
        val selectedIds: Set<DiagramElementId>,
        val interactionContext: ElementInteractionContext,
        val stateProvider: CurrentStateProvider
)