package com.valb3r.bpmn.intellij.plugin.core.render

import java.awt.geom.Point2D
import java.util.concurrent.atomic.AtomicInteger

val gridIcons = listOf(
    {currentIconProvider().noGrid},
    {currentIconProvider().gridDense}
)

val gridSteps = listOf(5, 0)
val gridState = AtomicInteger()

fun snapToGridIfNecessary(x: Float, y: Float): Point2D.Float {
    val gridSize = gridSteps[gridState.get()]
    if (0 == gridSize) {
        return Point2D.Float(x, y)
    }
    return Point2D.Float((x / gridSize).toInt() * gridSize.toFloat(), (y / gridSize).toInt() * gridSize.toFloat())
}