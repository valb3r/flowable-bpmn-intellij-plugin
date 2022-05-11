package com.valb3r.bpmn.intellij.plugin.core.render.elements.shapes


import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.BpmnElementId
import com.valb3r.bpmn.intellij.plugin.bpmn.api.diagram.DiagramElementId
import com.valb3r.bpmn.intellij.plugin.bpmn.api.diagram.elements.ShapeElement
import com.valb3r.bpmn.intellij.plugin.bpmn.api.info.PropertyType
import com.valb3r.bpmn.intellij.plugin.core.Colors
import com.valb3r.bpmn.intellij.plugin.core.render.AreaType
import com.valb3r.bpmn.intellij.plugin.core.render.AreaWithZindex
import com.valb3r.bpmn.intellij.plugin.core.render.ICON_Z_INDEX
import com.valb3r.bpmn.intellij.plugin.core.render.RenderContext
import com.valb3r.bpmn.intellij.plugin.core.render.elements.RenderState
import javax.swing.Icon

class TopLeftIconWithTriggerShape(
    elementId: DiagramElementId,
    bpmnElementId: BpmnElementId,
    icon: Icon,
    shape: ShapeElement,
    state: () -> RenderState,
    backgroundColor: Colors = Colors.SERVICE_TASK_COLOR,
    borderColor: Colors =  Colors.ELEMENT_BORDER_COLOR,
    textColor: Colors = Colors.INNER_TEXT_COLOR
) : TopLeftIconShape(elementId, bpmnElementId, icon, shape, state, backgroundColor, borderColor, textColor) {


    override fun doRender(ctx: RenderContext, shapeCtx: ShapeCtx): Map<DiagramElementId, AreaWithZindex> {
        val area = super.doRender(ctx, shapeCtx).toMutableMap()
        if (!isTriggered()) {
            return area
        }

        val triggerElementId = DiagramElementId("TRIGGER:$elementId")
        val trigger = ctx.canvas.drawTriggered(shapeCtx.shape, state().icons.triggered)
        area[triggerElementId] =
            AreaWithZindex(trigger, AreaType.POINT, mutableSetOf(), mutableSetOf(), ICON_Z_INDEX, elementId)
        return area
    }

    private fun isTriggered(): Boolean {
        return (state().currentState.elemPropertiesByStaticElementId[bpmnElementId]?.get(PropertyType.IS_TRIGGERABLE)?.value as Boolean? ?: false)
    }
}