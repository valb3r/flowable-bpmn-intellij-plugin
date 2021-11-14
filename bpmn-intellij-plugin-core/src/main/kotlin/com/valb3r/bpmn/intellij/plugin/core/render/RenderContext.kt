package com.valb3r.bpmn.intellij.plugin.core.render

import com.intellij.openapi.project.Project
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.BpmnElementId
import com.valb3r.bpmn.intellij.plugin.bpmn.api.diagram.DiagramElementId
import com.valb3r.bpmn.intellij.plugin.core.render.elements.BaseBpmnRenderElement
import com.valb3r.bpmn.intellij.plugin.core.render.elements.BaseDiagramRenderElement
import com.valb3r.bpmn.intellij.plugin.core.render.elements.RenderState
import com.valb3r.bpmn.intellij.plugin.core.state.CurrentStateProvider

data class RenderContext(
    val project: Project,
    val canvas: CanvasPainter,
    val selectedIds: Set<DiagramElementId>,
    val interactionContext: ElementInteractionContext,
    val stateProvider: CurrentStateProvider,
    var cachedDom: TreeState? = null
) {
    // Just a FIXME for Kotlin debugger not to get to OutOfMemory
    override fun toString(): String {
        return ""
    }
}
