package com.valb3r.bpmn.intellij.plugin.render

import com.valb3r.bpmn.intellij.plugin.bpmn.api.diagram.DiagramElementId
import java.awt.geom.Area
import java.awt.geom.Point2D

const val DEFAULT_Z_INDEX = 10000
const val ANCHOR_Z_INDEX = 1000

data class AreaWithZindex(
        val area: Area,
        val dragCenter: Point2D.Float,
        val anchors: MutableSet<Point2D.Float> = mutableSetOf(),
        val index: Int = DEFAULT_Z_INDEX,
        val parentToSelect: DiagramElementId? = null
)