package com.valb3r.bpmn.intellij.plugin.core.render

import java.awt.geom.Point2D

fun snapToGridIfNecessary(x: Float, y: Float): Point2D.Float {
    return Point2D.Float((x / 10.0f).toInt() * 10.0f, (y / 10.0f).toInt() * 10.0f)
}