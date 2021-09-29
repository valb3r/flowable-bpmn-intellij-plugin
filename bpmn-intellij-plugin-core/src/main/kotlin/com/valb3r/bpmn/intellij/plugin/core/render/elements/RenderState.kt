package com.valb3r.bpmn.intellij.plugin.core.render.elements

import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.BpmnElementId
import com.valb3r.bpmn.intellij.plugin.bpmn.api.diagram.DiagramElementId
import com.valb3r.bpmn.intellij.plugin.core.render.IconProvider
import com.valb3r.bpmn.intellij.plugin.core.render.RenderContext
import com.valb3r.bpmn.intellij.plugin.core.render.elements.viewtransform.NullViewTransform
import com.valb3r.bpmn.intellij.plugin.core.render.elements.viewtransform.ViewTransform
import com.valb3r.bpmn.intellij.plugin.core.state.CurrentState

data class RenderState(
    val elemMap: Map<DiagramElementId, BaseDiagramRenderElement>,
    val viewTransforms: MutableMap<DiagramElementId, ViewTransform>,
    val currentState: CurrentState,
    val history: List<BpmnElementId>,
    val ctx: RenderContext,
    val icons: IconProvider,
    val baseTransform: ViewTransform = NullViewTransform()
) {
    fun viewTransform(elem: DiagramElementId): ViewTransform {
        return viewTransforms.getOrDefault(elem, baseTransform)
    }
}