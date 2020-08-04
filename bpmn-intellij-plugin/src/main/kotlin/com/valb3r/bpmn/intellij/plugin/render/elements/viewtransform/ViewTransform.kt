package com.valb3r.bpmn.intellij.plugin.render.elements.viewtransform

import java.awt.geom.Point2D
import java.awt.geom.Rectangle2D

interface PreTransform {
    fun preTransform(rect: Rectangle2D.Float): Rectangle2D.Float
    fun preTransform(point: Point2D.Float): Point2D.Float
    fun addPreTransform(viewTransform: ViewTransform)
}

interface ViewTransform: PreTransform {
    fun transform(rect: Rectangle2D.Float): Rectangle2D.Float
    fun transform(point: Point2D.Float): Point2D.Float
}

class PreTransformHandler(private val preTransforms: MutableList<ViewTransform> = mutableListOf()): PreTransform {

    override fun preTransform(rect: Rectangle2D.Float): Rectangle2D.Float {
        var curr = rect
        for (transform in preTransforms) {
            curr = transform.transform(rect)
        }
        return curr
    }

    override fun preTransform(point: Point2D.Float): Point2D.Float {
        var curr = point
        for (transform in preTransforms) {
            curr = transform.transform(point)
        }
        return curr
    }

    override fun addPreTransform(viewTransform: ViewTransform) {
        preTransforms += viewTransform
    }
}

class NullViewTransform(private val preTransform: PreTransformHandler = PreTransformHandler()): ViewTransform, PreTransform by preTransform {

    override fun transform(rect: Rectangle2D.Float): Rectangle2D.Float {
        return preTransform(rect)
    }

    override fun transform(point: Point2D.Float): Point2D.Float {
        return preTransform(point)
    }
}

data class DragViewTransform(
        val dx: Float, val dy: Float, private val preTransform: PreTransformHandler = PreTransformHandler()
): ViewTransform, PreTransform by preTransform {

    override fun transform(rect: Rectangle2D.Float): Rectangle2D.Float {
        val preTransformed = preTransform(rect)
        return Rectangle2D.Float(preTransformed.x + dx, preTransformed.y + dy, preTransformed.width, preTransformed.height)
    }

    override fun transform(point: Point2D.Float): Point2D.Float {
        val preTransformed = preTransform(point)
        return Point2D.Float(preTransformed.x + dx, preTransformed.y + dy)
    }
}

data class ResizeViewTransform(
        val cx: Float, val cy: Float, val coefW: Float, val coefH: Float, private val preTransform: PreTransformHandler = PreTransformHandler()
): ViewTransform, PreTransform by preTransform {

    override fun transform(rect: Rectangle2D.Float): Rectangle2D.Float {
        val preTransformed = preTransform(rect)
        val left = Point2D.Float(preTransformed.x, preTransformed.y)
        val right = Point2D.Float(preTransformed.x + preTransformed.width, preTransformed.y + preTransformed.height)
        val newLeft = transformPoint(left)
        val newRight = transformPoint(right)

        return Rectangle2D.Float(newLeft.x, newLeft.y, newRight.x - newLeft.x, newRight.y - newLeft.y)
    }

    override fun transform(point: Point2D.Float): Point2D.Float {
        return transformPoint(preTransform(point))
    }

    private fun transformPoint(point: Point2D.Float): Point2D.Float {
        return Point2D.Float(cx + (point.x - cx) * coefW, cy + (point.y - cy) * coefH)
    }
}