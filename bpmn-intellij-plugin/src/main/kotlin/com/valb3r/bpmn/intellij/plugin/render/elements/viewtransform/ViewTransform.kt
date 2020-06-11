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

data class ResizeViewTransform(val cx: Float, val cy: Float, val coefW: Float, val coefH: Float): ViewTransform {

    override fun transform(rect: Rectangle2D.Float): Rectangle2D.Float {
        val left = Point2D.Float(rect.x, rect.y)
        val right = Point2D.Float(rect.x + rect.width, rect.y + rect.height)
        val newLeft = transform(left)
        val newRight = transform(right)

        return Rectangle2D.Float(newLeft.x, newLeft.y, newRight.x - newLeft.x, newRight.y - newLeft.y)
    }

    override fun transform(point: Point2D.Float): Point2D.Float {
        return Point2D.Float(cx + (point.x - cx) * coefW, cy + (point.y - cy) * coefH)
    }
}