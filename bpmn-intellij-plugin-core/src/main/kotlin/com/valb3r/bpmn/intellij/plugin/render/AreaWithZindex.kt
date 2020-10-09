package com.valb3r.bpmn.intellij.plugin.render

import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.BpmnElementId
import com.valb3r.bpmn.intellij.plugin.bpmn.api.diagram.DiagramElementId
import com.valb3r.bpmn.intellij.plugin.render.elements.Anchor
import java.awt.geom.Area

const val ICON_Z_INDEX = 10000
const val ANCHOR_Z_INDEX = 1000

enum class AreaType(val nests: Boolean = false) {
    POINT,
    EDGE,
    SHAPE,
    SELECTS_DRAG_TARGET,
    SHAPE_THAT_NESTS(true),
    PARENT_PROCESS_SHAPE(true);
}

data class AreaWithZindex(
        val area: Area,
        val areaType: AreaType,
        val anchorsForWaypoints: MutableSet<Anchor> = mutableSetOf(),
        val anchorsForShape: MutableSet<Anchor> = mutableSetOf(),
        val index: Int = 0,
        val parentToSelect: DiagramElementId? = null,
        val bpmnElementId: BpmnElementId? = null
)