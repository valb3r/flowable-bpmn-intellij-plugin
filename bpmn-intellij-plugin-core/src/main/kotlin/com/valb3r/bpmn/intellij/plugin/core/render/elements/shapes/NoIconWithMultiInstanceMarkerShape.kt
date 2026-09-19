package com.valb3r.bpmn.intellij.plugin.core.render.elements.shapes

import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.BpmnElementId
import com.valb3r.bpmn.intellij.plugin.bpmn.api.diagram.DiagramElementId
import com.valb3r.bpmn.intellij.plugin.bpmn.api.diagram.elements.ShapeElement
import com.valb3r.bpmn.intellij.plugin.bpmn.api.info.PropertyType
import com.valb3r.bpmn.intellij.plugin.core.render.AreaWithZindex
import com.valb3r.bpmn.intellij.plugin.core.render.RenderContext
import com.valb3r.bpmn.intellij.plugin.core.render.elements.RenderState

class NoIconWithMultiInstanceMarkerShape(
    elementId: DiagramElementId,
    bpmnElementId: BpmnElementId,
    shape: ShapeElement,
    state: () -> RenderState,
) : NoIconShape(elementId, bpmnElementId, shape, state) {

    override fun doRender(ctx: RenderContext, shapeCtx: ShapeCtx): Map<DiagramElementId, AreaWithZindex> {
        val area = super.doRender(ctx, shapeCtx)
        if (isMultiInstance()) {
            ctx.canvas.drawMultiInstance(shapeCtx.shape, state().icons.multiInstance)
        }
        return area
    }

    private fun isMultiInstance(): Boolean {
        val properties = state().currentState.elemPropertiesByStaticElementId[bpmnElementId] ?: return false
        return MULTI_INSTANCE_PROPERTIES.any { properties[it]?.value != null }
    }

    private companion object {
        val MULTI_INSTANCE_PROPERTIES = setOf(
            PropertyType.MULTI_INSTANCE_IS_SEQUENTIAL,
            PropertyType.MULTI_INSTANCE_COLLECTION,
            PropertyType.MULTI_INSTANCE_ELEMENT_VARIABLE,
            PropertyType.MULTI_INSTANCE_LOOP_CARDINALITY,
            PropertyType.MULTI_INSTANCE_COMPLETION_CONDITION,
            PropertyType.MULTI_INSTANCE_ELEMENT_INDEX_VARIABLE,
            PropertyType.MULTI_INSTANCE_NO_WAIT_STATES_ASYNC_LEAVE,
            PropertyType.MULTI_INSTANCE_LOOP_DATA_INPUT_REF,
            PropertyType.MULTI_INSTANCE_INPUT_DATA_ITEM,
            PropertyType.VARIABLE_AGGREGATION_TARGET,
        )
    }
}
