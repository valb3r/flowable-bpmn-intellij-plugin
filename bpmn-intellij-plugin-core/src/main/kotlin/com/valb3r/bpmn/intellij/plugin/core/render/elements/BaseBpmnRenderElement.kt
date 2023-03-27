package com.valb3r.bpmn.intellij.plugin.core.render.elements

import com.valb3r.bpmn.intellij.plugin.bpmn.api.BpmnObjectFactory
import com.valb3r.bpmn.intellij.plugin.bpmn.api.PropertyTable
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.BpmnElementId
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.WithBpmnId
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.WithParentId
import com.valb3r.bpmn.intellij.plugin.bpmn.api.diagram.DiagramElementId
import com.valb3r.bpmn.intellij.plugin.bpmn.api.diagram.elements.ShapeElement
import com.valb3r.bpmn.intellij.plugin.bpmn.api.events.Event
import com.valb3r.bpmn.intellij.plugin.core.events.BpmnElementRemovedEvent
import com.valb3r.bpmn.intellij.plugin.core.events.BpmnShapeObjectAddedEvent
import kotlin.reflect.KClass

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

    open fun <T: WithBpmnId> onElementCreatedOnTopThis(clazz: KClass<T>, factory: BpmnObjectFactory, newShape: (T) -> ShapeElement): MutableList<Event>  {
        val elem = factory.newBpmnObject(clazz)
        val shape = newShape(elem)
        val props = factory.propertiesOf(elem)
        return mutableListOf(BpmnShapeObjectAddedEvent(WithParentId(this.bpmnElementId, elem), shape, props))
    }
}