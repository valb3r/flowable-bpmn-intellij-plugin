package com.valb3r.bpmn.intellij.plugin.core.render.elements.shapes

import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.BpmnElementId
import com.valb3r.bpmn.intellij.plugin.bpmn.api.diagram.DiagramElementId
import com.valb3r.bpmn.intellij.plugin.bpmn.api.diagram.elements.ShapeElement
import com.valb3r.bpmn.intellij.plugin.core.render.SvgIcon
import com.valb3r.bpmn.intellij.plugin.core.render.elements.RenderState

class UnknownShape(
    elementId: DiagramElementId, bpmnElementId: BpmnElementId, icon: SvgIcon, shape: ShapeElement,
    state: () -> RenderState
) : IconShape(
    elementId, bpmnElementId,
    icon,
    shape, state
)