package com.valb3r.bpmn.intellij.plugin.render.elements.viewtransform

import com.valb3r.bpmn.intellij.plugin.bpmn.api.diagram.DiagramElementId
import com.valb3r.bpmn.intellij.plugin.render.elements.EPSILON
import java.awt.geom.Point2D
import java.awt.geom.Rectangle2D
import kotlin.math.abs

interface PreTransformable {
    fun preTransform(elementId: DiagramElementId, rect: Rectangle2D.Float): Rectangle2D.Float
    fun preTransform(elementId: DiagramElementId, point: Point2D.Float): Point2D.Float
    fun addPreTransform(viewTransform: ViewTransform)
    fun <T: ViewTransform> listTransformsOfType(type: Class<T>): List<T>
}

interface ViewTransform: PreTransformable {
    fun transform(elementId: DiagramElementId, rect: Rectangle2D.Float): Rectangle2D.Float
    fun transform(elementId: DiagramElementId, point: Point2D.Float): Point2D.Float
}

class PreTransformHandler(private val preTransforms: MutableList<ViewTransform> = mutableListOf()): PreTransformable {

    override fun preTransform(elementId: DiagramElementId, rect: Rectangle2D.Float): Rectangle2D.Float {
        var curr = rect
        for (transform in preTransforms) {
            curr = transform.transform(elementId, rect)
        }
        return curr
    }

    override fun preTransform(elementId: DiagramElementId, point: Point2D.Float): Point2D.Float {
        var curr = point
        for (transform in preTransforms) {
            curr = transform.transform(elementId, point)
        }
        return curr
    }

    override fun addPreTransform(viewTransform: ViewTransform) {
        preTransforms += viewTransform
    }

    override fun <T : ViewTransform> listTransformsOfType(type: Class<T>): List<T> {
        val result = mutableListOf<T>()
        result += preTransforms.filterIsInstance(type)
        result += preTransforms.flatMap { it.listTransformsOfType(type) }
        return result
    }
}

class NullViewTransform(private val preTransform: PreTransformHandler = PreTransformHandler()): ViewTransform, PreTransformable by preTransform {

    override fun transform(elementId: DiagramElementId, rect: Rectangle2D.Float): Rectangle2D.Float {
        return preTransform(elementId, rect)
    }

    override fun transform(elementId: DiagramElementId, point: Point2D.Float): Point2D.Float {
        return preTransform(elementId, point)
    }
}

data class DragViewTransform(
        val dx: Float, val dy: Float, private val preTransform: PreTransformHandler = PreTransformHandler()
): ViewTransform, PreTransformable by preTransform {

    override fun transform(elementId: DiagramElementId, rect: Rectangle2D.Float): Rectangle2D.Float {
        val preTransformed = preTransform(elementId, rect)
        return Rectangle2D.Float(preTransformed.x + dx, preTransformed.y + dy, preTransformed.width, preTransformed.height)
    }

    override fun transform(elementId: DiagramElementId, point: Point2D.Float): Point2D.Float {
        val preTransformed = preTransform(elementId, point)
        return Point2D.Float(preTransformed.x + dx, preTransformed.y + dy)
    }
}

data class ResizeViewTransform(
        val cx: Float, val cy: Float, val coefW: Float, val coefH: Float, private val preTransform: PreTransformHandler = PreTransformHandler()
): ViewTransform, PreTransformable by preTransform {

    override fun transform(elementId: DiagramElementId, rect: Rectangle2D.Float): Rectangle2D.Float {
        val preTransformed = preTransform(elementId, rect)
        val left = Point2D.Float(preTransformed.x, preTransformed.y)
        val right = Point2D.Float(preTransformed.x + preTransformed.width, preTransformed.y + preTransformed.height)
        val newLeft = transformPoint(left)
        val newRight = transformPoint(right)

        return Rectangle2D.Float(newLeft.x, newLeft.y, newRight.x - newLeft.x, newRight.y - newLeft.y)
    }

    override fun transform(elementId: DiagramElementId, point: Point2D.Float): Point2D.Float {
        return transformPoint(preTransform(elementId, point))
    }

    private fun transformPoint(point: Point2D.Float): Point2D.Float {
        return Point2D.Float(cx + (point.x - cx) * coefW, cy + (point.y - cy) * coefH)
    }
}

class ExpandViewTransform(
        private val expandedElementId: DiagramElementId,
        private val shape: Rectangle2D.Float,
        private val cx: Float,
        private val cy: Float,
        private val dx: Float,
        private val dy: Float,
        private val excludeIds: Set<DiagramElementId>,
        private val preTransform: PreTransformHandler = PreTransformHandler()
): ViewTransform, PreTransformable by preTransform {

    override fun transform(elementId: DiagramElementId, rect: Rectangle2D.Float): Rectangle2D.Float {
        val transformed = preTransform(elementId, rect)
        if (excludeIds.contains(elementId)) {
             return transformed
        }

        val halfWidth = transformed.width / 2.0f
        val halfHeight = transformed.height / 2.0f

        val center = transformPoint(Point2D.Float(transformed.x + halfWidth, transformed.y  + halfHeight))

        if (elementId == expandedElementId) {
            val left = transformPoint(Point2D.Float(transformed.x, transformed.y))
            val right = transformPoint(Point2D.Float(transformed.x + rect.width, transformed.y  + rect.height))
            return Rectangle2D.Float(left.x, left.y, right.x - left.x, right.y - left.y)
        }

        return Rectangle2D.Float(center.x - halfWidth, center.y - halfHeight, rect.width, rect.height)
    }

    override fun transform(elementId: DiagramElementId, point: Point2D.Float): Point2D.Float {
        val transformed = preTransform(elementId, point)
        if (excludeIds.contains(elementId)) {
            return transformed
        }

        return transformPoint(transformed)
    }

    fun undoTransform(elementId: DiagramElementId, point: Point2D.Float): Point2D.Float {
        val transformed = preTransform(elementId, point)
        if (excludeIds.contains(elementId)) {
            return transformed
        }

        return undoTransformPoint(point)
    }

    private fun transformPoint(point: Point2D.Float): Point2D.Float {
        // assuming left-right, top-down coordinate system
        return when {
            abs(point.x - cx) < EPSILON && abs(point.y - cy) < EPSILON -> point

            // rectangle edges:
            // top-left
            abs(point.x - shape.x) < EPSILON && abs(point.y - shape.y) < EPSILON -> Point2D.Float(point.x - dx, point.y - dy)
            // bottom-left
            abs(point.x - shape.x) < EPSILON && abs(point.y - shape.y - shape.height) < EPSILON -> Point2D.Float(point.x - dx, point.y + dy)
            // top-right
            abs(point.x - shape.x - shape.width) < EPSILON && abs(point.y - shape.y) < EPSILON -> Point2D.Float(point.x + dx, point.y - dy)
            // bottom-right
            abs(point.x - shape.x - shape.width) < EPSILON && abs(point.y - shape.y - shape.height) < EPSILON -> Point2D.Float(point.x + dx, point.y + dy)

            // rectangle forming lines cases:
            // top-line
            point.y <= shape.y && shape.x <= point.x && shape.x + shape.width >= point.x -> Point2D.Float(point.x, point.y - dy)
            // bottom-line
            point.y >= shape.y + shape.height && shape.x <= point.x && shape.x + shape.width >= point.x -> Point2D.Float(point.x, point.y + dy)
            // left-line
            point.x <= shape.x && shape.y <= point.y && shape.y + shape.width >= point.y -> Point2D.Float(point.x - dx, point.y)
            // right-line
            point.x >= shape.x + shape.width && shape.y <= point.y && shape.y + shape.width >= point.y -> Point2D.Float(point.x + dx, point.y)

            // quarters, if can be simplified as edge cases are already handled
            // top-left
            point.x < cx && point.y < cy -> Point2D.Float(point.x - dx, point.y - dy)
            // top-right
            point.x > cx && point.y < cy -> Point2D.Float(point.x + dx, point.y - dy)
            // bottom-left
            point.x < cx && point.y > cy -> Point2D.Float(point.x - dx, point.y + dy)
            // bottom-right
            point.x > cx && point.y > cy -> Point2D.Float(point.x + dx, point.y + dy)

            else -> throw IllegalStateException("Unexpected point value: $point for $cx,$cy expand view")
        }
    }

    private fun undoTransformPoint(point: Point2D.Float): Point2D.Float {
        // assuming left-right, top-down coordinate system
        return when {
            abs(point.x - cx) < EPSILON && abs(point.y - cy) < EPSILON -> point

            // rectangle edges:
            // top-left
            abs(point.x - shape.x) < EPSILON && abs(point.y - shape.y) < EPSILON -> Point2D.Float(point.x + dx, point.y + dy)
            // bottom-left
            abs(point.x - shape.x) < EPSILON && abs(point.y - shape.y - shape.height) < EPSILON -> Point2D.Float(point.x + dx, point.y - dy)
            // top-right
            abs(point.x - shape.x - shape.width) < EPSILON && abs(point.y - shape.y) < EPSILON -> Point2D.Float(point.x - dx, point.y + dy)
            // bottom-right
            abs(point.x - shape.x - shape.width) < EPSILON && abs(point.y - shape.y - shape.height) < EPSILON -> Point2D.Float(point.x - dx, point.y - dy)

            // rectangle forming lines cases:
            // top-line
            point.y <= shape.y && shape.x <= point.x && shape.x + shape.width >= point.x -> Point2D.Float(point.x, point.y + dy)
            // bottom-line
            point.y >= shape.y + shape.height && shape.x <= point.x && shape.x + shape.width >= point.x -> Point2D.Float(point.x, point.y - dy)
            // left-line
            point.x <= shape.x && shape.y <= point.y && shape.y + shape.width >= point.y -> Point2D.Float(point.x + dx, point.y)
            // right-line
            point.x >= shape.x + shape.width && shape.y <= point.y && shape.y + shape.width >= point.y -> Point2D.Float(point.x - dx, point.y)

            // quarters, if can be simplified as edge cases are already handled
            // top-left
            point.x < cx && point.y < cy -> Point2D.Float(point.x + dx, point.y + dy)
            // top-right
            point.x > cx && point.y < cy -> Point2D.Float(point.x - dx, point.y + dy)
            // bottom-left
            point.x < cx && point.y > cy -> Point2D.Float(point.x + dx, point.y - dy)
            // bottom-right
            point.x > cx && point.y > cy -> Point2D.Float(point.x - dx, point.y - dy)

            else -> throw IllegalStateException("Unexpected point value: $point for $cx,$cy expand view")
        }
    }
}