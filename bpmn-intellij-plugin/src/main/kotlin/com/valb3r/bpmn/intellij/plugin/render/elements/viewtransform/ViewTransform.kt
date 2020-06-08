package com.valb3r.bpmn.intellij.plugin.render.elements.viewtransform

import java.awt.geom.Point2D
import java.awt.geom.Rectangle2D

interface ViewTransform {
    fun transform(rect: Rectangle2D.Float): Rectangle2D.Float
    fun transform(point: Point2D.Float): Point2D.Float
}


class NullViewTransform: ViewTransform {

    override fun transform(rect: Rectangle2D.Float): Rectangle2D.Float {
        return rect
    }

    override fun transform(point: Point2D.Float): Point2D.Float {
        return point
    }
}

data class DragViewTransform(val dx: Float, val dy: Float): ViewTransform {

    override fun transform(rect: Rectangle2D.Float): Rectangle2D.Float {
        return Rectangle2D.Float(rect.x + dx, rect.y + dy, rect.width, rect.height)
    }

    override fun transform(point: Point2D.Float): Point2D.Float {
        return Point2D.Float(point.x + dx, point.y + dy)
    }
}