package com.valb3r.bpmn.intellij.plugin.render

import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.BpmnElementId
import com.valb3r.bpmn.intellij.plugin.bpmn.api.diagram.DiagramElementId
import java.awt.geom.Area
import java.awt.geom.Point2D

const val OWNING_PROCESS_Z_INDEX = 1000000
const val SUBPROCESS_Z_INDEX = 20000
const val DEFAULT_Z_INDEX = 10000
const val ANCHOR_Z_INDEX = 1000

enum class AreaType(val nests: Boolean = false) {
    POINT,
    EDGE,
    SHAPE,
    SHAPE_THAT_NESTS(true),
    PARENT_PROCESS_SHAPE(true);
}

data class AreaWithZindex(
        val area: Area,
        val areaType: AreaType,
        val anchorsForWaypoints: MutableSet<Point2D.Float> = mutableSetOf(),
        val anchorsForShape: MutableSet<Point2D.Float> = mutableSetOf(),
        val index: Int = DEFAULT_Z_INDEX,
        val parentToSelect: DiagramElementId? = null,
        val bpmnElementId: BpmnElementId? = null
)