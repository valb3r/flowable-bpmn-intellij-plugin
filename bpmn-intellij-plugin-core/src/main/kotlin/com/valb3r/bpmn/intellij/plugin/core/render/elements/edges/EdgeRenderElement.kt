package com.valb3r.bpmn.intellij.plugin.core.render.elements.edges

import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.BpmnElementId
import com.valb3r.bpmn.intellij.plugin.bpmn.api.diagram.DiagramElementId
import com.valb3r.bpmn.intellij.plugin.bpmn.api.diagram.elements.BoundsElement
import com.valb3r.bpmn.intellij.plugin.bpmn.api.events.EdgeWithIdentifiableWaypoints
import com.valb3r.bpmn.intellij.plugin.core.Colors
import com.valb3r.bpmn.intellij.plugin.core.events.BpmnElementRemovedEvent
import com.valb3r.bpmn.intellij.plugin.core.events.DiagramElementRemovedEvent
import com.valb3r.bpmn.intellij.plugin.core.render.AreaType
import com.valb3r.bpmn.intellij.plugin.core.render.AreaWithZindex
import com.valb3r.bpmn.intellij.plugin.core.render.ICON_Z_INDEX
import com.valb3r.bpmn.intellij.plugin.core.render.elements.ACTIONS_ICO_SIZE
import com.valb3r.bpmn.intellij.plugin.core.render.elements.RenderState
import com.valb3r.bpmn.intellij.plugin.core.render.elements.elemIdToRemove

class EdgeRenderElement(
        elementId: DiagramElementId,
        bpmnElementId: BpmnElementId,
        edge: EdgeWithIdentifiableWaypoints,
        state: RenderState
): BaseEdgeRenderElement(elementId, bpmnElementId, edge, Colors.ARROW_COLOR, state) {

    override fun drawActionsRight(x: Float, y: Float): Map<DiagramElementId, AreaWithZindex> {
        val delId = elementId.elemIdToRemove()
        val deleteIconArea = state.ctx.canvas.drawIcon(BoundsElement(x, y - ACTIONS_ICO_SIZE, ACTIONS_ICO_SIZE, ACTIONS_ICO_SIZE), state.icons.recycleBin)
        state.ctx.interactionContext.clickCallbacks[delId] = { dest ->
            val bpmnRemoves = mutableListOf<BpmnElementRemovedEvent>()
            val diagramRemoves = mutableListOf<DiagramElementRemovedEvent>()
            edge.bpmnElement?.let { bpmnRemoves += BpmnElementRemovedEvent(it) }
            diagramRemoves += DiagramElementRemovedEvent(elementId)
            dest.addElementRemovedEvent(diagramRemoves, bpmnRemoves)
        }

        return mutableMapOf(delId to AreaWithZindex(deleteIconArea, AreaType.POINT, mutableSetOf(), mutableSetOf(), ICON_Z_INDEX, elementId))
    }
}