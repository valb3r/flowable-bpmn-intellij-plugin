package com.valb3r.bpmn.intellij.plugin.core.render.elements

import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.BpmnElementId
import com.valb3r.bpmn.intellij.plugin.bpmn.api.diagram.DiagramElementId
import com.valb3r.bpmn.intellij.plugin.core.events.BpmnElementRemovedEvent

abstract class BaseBpmnRenderElement(
        elementId: DiagramElementId,
        open val bpmnElementId: BpmnElementId,
        override val state: () -> RenderState,
): BaseDiagramRenderElement(elementId, state) {

    override fun getEventsToDeleteElement(): List<BpmnElementRemovedEvent> {
        val delete = mutableListOf<BpmnElementRemovedEvent>()
        children.forEach {delete += it.getEventsToDeleteElement()}
        delete += BpmnElementRemovedEvent(bpmnElementId)
        return delete
    }

    override fun toString(): String {
        return bpmnElementId.id
    }
}