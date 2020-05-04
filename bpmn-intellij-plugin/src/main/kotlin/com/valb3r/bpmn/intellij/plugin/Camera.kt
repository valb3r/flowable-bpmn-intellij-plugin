package com.valb3r.bpmn.intellij.plugin

import java.awt.geom.Point2D

data class Camera(val origin: Point2D.Float, val zoom: Point2D.Float) {

    fun toCameraView(point: Point2D.Float): Point2D.Float {
        return Point2D.Float(
                this.zoom.x * point.x - this.origin.x,
                this.zoom.y * point.y - this.origin.y
        )
    }
}