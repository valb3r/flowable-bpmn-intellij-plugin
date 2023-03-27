package com.valb3r.bpmn.intellij.plugin.core.render.elements.shapes

import com.valb3r.bpmn.intellij.plugin.bpmn.api.BpmnObjectFactory
import com.valb3r.bpmn.intellij.plugin.bpmn.api.PropertyTable
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.BpmnElementId
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.WithBpmnId
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.WithParentId
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.lanes.BpmnFlowNodeRef
import com.valb3r.bpmn.intellij.plugin.bpmn.api.diagram.DiagramElementId
import com.valb3r.bpmn.intellij.plugin.bpmn.api.diagram.elements.ShapeElement
import com.valb3r.bpmn.intellij.plugin.bpmn.api.events.Event
import com.valb3r.bpmn.intellij.plugin.core.Colors
import com.valb3r.bpmn.intellij.plugin.core.events.BpmnFlowNodeRefAddedEvent
import com.valb3r.bpmn.intellij.plugin.core.events.BpmnShapeObjectAddedEvent
import com.valb3r.bpmn.intellij.plugin.core.render.AreaType
import com.valb3r.bpmn.intellij.plugin.core.render.AreaWithZindex
import com.valb3r.bpmn.intellij.plugin.core.render.RenderContext
import com.valb3r.bpmn.intellij.plugin.core.render.elements.RenderState
import java.awt.Stroke
import kotlin.reflect.KClass

class ShapeGroupElement(
        elementId: DiagramElementId,
        bpmnElementId: BpmnElementId,
        shape: ShapeElement,
        state: () -> RenderState,
        private val backgroundColor: Colors = Colors.CALL_ACTIVITY_COLOR,
        private val borderColor: Colors =  Colors.ELEMENT_BORDER_COLOR,
        private val textColor: Colors = Colors.INNER_TEXT_COLOR,
        private val borderStroke: Stroke? = null,
        override val areaType: AreaType = AreaType.SHAPE
) : ResizeableShapeRenderElement(elementId, bpmnElementId, shape, state) {

    override fun doRender(ctx: RenderContext, shapeCtx: ShapeCtx): Map<DiagramElementId, AreaWithZindex> {

        val area = ctx.canvas.drawRoundedRect(
                shapeCtx.shape,
                shapeCtx.name,
                color(backgroundColor),
                borderColor.color,
                textColor.color,
                borderStroke
        )

        return mapOf(shapeCtx.diagramId to AreaWithZindex(area, areaType, waypointAnchors(ctx.canvas.camera), shapeAnchors(ctx.canvas.camera), index = zIndex(), bpmnElementId = shape.bpmnElement))
    }

    override fun onDragEnd(dx: Float, dy: Float, droppedOn: BpmnElementId?, allDroppedOnAreas: Map<BpmnElementId, AreaWithZindex>): MutableList<Event> {
        if (null != droppedOn) {
            return mutableListOf()
        }

        return super.onDragEnd(dx, dy, droppedOn, allDroppedOnAreas)
    }

    override fun <T : WithBpmnId> onElementCreatedOnTopThis(clazz: KClass<T>, factory: BpmnObjectFactory, newShape: (T) -> ShapeElement
    ): MutableList<Event> {
        val newElems = super.onElementCreatedOnTopThis(clazz, factory, newShape)
        val refNode = factory.newFlowRef().copy(ref = (newElems[0] as BpmnShapeObjectAddedEvent).bpmnObject.element.id.id)
        newElems += BpmnFlowNodeRefAddedEvent(refNode)
        return newElems
    }

    override fun zIndex(): Int {
        return (parents.firstOrNull()?.zIndex() ?: -1) + 1
    }
}
