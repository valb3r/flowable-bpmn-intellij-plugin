package com.valb3r.bpmn.intellij.plugin.render.elements

import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.BpmnElementId
import com.valb3r.bpmn.intellij.plugin.bpmn.api.diagram.DiagramElementId
import com.valb3r.bpmn.intellij.plugin.events.BpmnElementRemovedEvent
import com.valb3r.bpmn.intellij.plugin.render.elements.viewtransform.NullViewTransform
import com.valb3r.bpmn.intellij.plugin.render.elements.viewtransform.ViewTransform

abstract class BaseBpmnRenderElement(
        override val elementId: DiagramElementId,
        open val bpmnElementId: BpmnElementId,
        override val state: RenderState,
        override var viewTransform: ViewTransform = NullViewTransform()
): BaseDiagramRenderElement(elementId, state, viewTransform) {

    override fun getEventsToDeleteElement(): List<BpmnElementRemovedEvent> {
        val delete = mutableListOf<BpmnElementRemovedEvent>()
        children.forEach {delete += it.getEventsToDeleteElement()}
        delete += BpmnElementRemovedEvent(bpmnElementId)
        return delete
    }
}