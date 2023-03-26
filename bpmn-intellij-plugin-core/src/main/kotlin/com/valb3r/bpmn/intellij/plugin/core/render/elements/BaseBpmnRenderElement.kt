package com.valb3r.bpmn.intellij.plugin.core.render.elements

import com.valb3r.bpmn.intellij.plugin.bpmn.api.PropertyTable
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.BpmnElementId
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.WithBpmnId
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.WithParentId
import com.valb3r.bpmn.intellij.plugin.bpmn.api.diagram.DiagramElementId
import com.valb3r.bpmn.intellij.plugin.bpmn.api.diagram.elements.ShapeElement
import com.valb3r.bpmn.intellij.plugin.bpmn.api.events.Event
import com.valb3r.bpmn.intellij.plugin.core.events.BpmnElementRemovedEvent
import com.valb3r.bpmn.intellij.plugin.core.events.BpmnShapeObjectAddedEvent

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

    open fun onElementCreatedOnTopThis(newElement: WithBpmnId, shape: ShapeElement, properties: PropertyTable): MutableList<Event>  {
        return mutableListOf(BpmnShapeObjectAddedEvent(WithParentId(this.bpmnElementId, newElement), shape, properties))
    }
}