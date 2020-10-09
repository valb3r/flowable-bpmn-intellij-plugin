package com.valb3r.bpmn.intellij.plugin.render.elements

import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.BpmnElementId
import com.valb3r.bpmn.intellij.plugin.bpmn.api.diagram.DiagramElementId
import com.valb3r.bpmn.intellij.plugin.render.IconProvider
import com.valb3r.bpmn.intellij.plugin.render.RenderContext
import com.valb3r.bpmn.intellij.plugin.render.elements.viewtransform.NullViewTransform
import com.valb3r.bpmn.intellij.plugin.render.elements.viewtransform.ViewTransform
import com.valb3r.bpmn.intellij.plugin.state.CurrentState

data class RenderState(
        val elemMap: Map<DiagramElementId, BaseDiagramRenderElement>,
        val currentState: CurrentState,
        val history: List<BpmnElementId>,
        val ctx: RenderContext,
        val icons: IconProvider,
        val baseTransform: ViewTransform = NullViewTransform()
)