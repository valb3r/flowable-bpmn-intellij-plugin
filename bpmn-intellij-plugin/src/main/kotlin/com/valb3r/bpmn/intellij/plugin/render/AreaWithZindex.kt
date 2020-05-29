package com.valb3r.bpmn.intellij.plugin.render

import com.valb3r.bpmn.intellij.plugin.bpmn.api.diagram.DiagramElementId
import java.awt.geom.Area
import java.awt.geom.Point2D

const val SUBPROCESS_Z_INDEX = 20000
const val DEFAULT_Z_INDEX = 10000
const val ANCHOR_Z_INDEX = 1000

enum class AreaType {
    POINT,
    SHAPE
}

data class AreaWithZindex(
        val area: Area,
        val dragCenter: Point2D.Float,
        val areaType: AreaType,
        val anchorsForWaypoints: MutableSet<Point2D.Float> = mutableSetOf(),
        val anchorsForShape: MutableSet<Point2D.Float> = mutableSetOf(),
        val index: Int = DEFAULT_Z_INDEX,
        val parentToSelect: DiagramElementId? = null
)