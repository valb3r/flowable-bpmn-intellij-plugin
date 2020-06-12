package com.valb3r.bpmn.intellij.plugin.render.elements.edges

import com.valb3r.bpmn.intellij.plugin.Colors
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.BpmnElementId
import com.valb3r.bpmn.intellij.plugin.bpmn.api.diagram.DiagramElementId
import com.valb3r.bpmn.intellij.plugin.bpmn.api.diagram.elements.BoundsElement
import com.valb3r.bpmn.intellij.plugin.bpmn.api.events.EdgeWithIdentifiableWaypoints
import com.valb3r.bpmn.intellij.plugin.events.BpmnElementRemovedEvent
import com.valb3r.bpmn.intellij.plugin.events.DiagramElementRemovedEvent
import com.valb3r.bpmn.intellij.plugin.render.AreaType
import com.valb3r.bpmn.intellij.plugin.render.AreaWithZindex
import com.valb3r.bpmn.intellij.plugin.render.ICON_Z_INDEX
import com.valb3r.bpmn.intellij.plugin.render.elements.ACTIONS_ICO_SIZE
import com.valb3r.bpmn.intellij.plugin.render.elements.RenderState

class EdgeRenderElement(
        override val elementId: DiagramElementId,
        override val bpmnElementId: BpmnElementId,
        edge: EdgeWithIdentifiableWaypoints,
        state: RenderState
): BaseEdgeRenderElement(elementId, bpmnElementId, edge, Colors.ARROW_COLOR, state) {

    override fun drawActions(x: Float, y: Float): Map<DiagramElementId, AreaWithZindex> {
        val delId = DiagramElementId("DEL:$elementId")
        val deleteIconArea = state.ctx.canvas.drawIcon(BoundsElement(x, y, ACTIONS_ICO_SIZE, ACTIONS_ICO_SIZE), state.icons.recycleBin)
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