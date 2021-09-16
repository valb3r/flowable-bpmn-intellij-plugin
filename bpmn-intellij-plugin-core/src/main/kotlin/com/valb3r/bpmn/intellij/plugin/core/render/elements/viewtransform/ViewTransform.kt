package com.valb3r.bpmn.intellij.plugin.core.render.elements.viewtransform

import com.valb3r.bpmn.intellij.plugin.bpmn.api.diagram.DiagramElementId
import com.valb3r.bpmn.intellij.plugin.core.render.AreaType
import com.valb3r.bpmn.intellij.plugin.core.render.elements.EPSILON
import java.awt.geom.Point2D
import java.awt.geom.Rectangle2D
import kotlin.math.abs
import kotlin.random.Random.Default.nextFloat

data class RectangleTransformationIntrospection(val rect: Rectangle2D.Float, val type: AreaType, val attachedTo: DiagramElementId? = null)
data class PointTransformationIntrospection(val attachedTo: DiagramElementId? = null)

interface PreTransformable {
    fun preTransform(elementId: DiagramElementId, rectTransformationIntrospection: RectangleTransformationIntrospection): Rectangle2D.Float
    fun preTransform(elementId: DiagramElementId, point: Point2D.Float): Point2D.Float
    fun preTransform(elementId: DiagramElementId, point: Point2D.Float, introspection: PointTransformationIntrospection): Point2D.Float
    fun addPreTransform(viewTransform: ViewTransform)
    fun <T: ViewTransform> listTransformsOfType(type: Class<T>): List<T>
}

interface ViewTransform: PreTransformable {
    fun transform(elementId: DiagramElementId, rectTransformationIntrospection: RectangleTransformationIntrospection): Rectangle2D.Float
    fun transform(elementId: DiagramElementId, point: Point2D.Float): Point2D.Float
    fun transform(elementId: DiagramElementId, point: Point2D.Float, introspection: PointTransformationIntrospection): Point2D.Float
}

class PreTransformHandler(private val preTransforms: MutableList<ViewTransform> = mutableListOf()): PreTransformable {

    override fun preTransform(elementId: DiagramElementId, rectTransformationIntrospection: RectangleTransformationIntrospection): Rectangle2D.Float {
        val transformed = ViewTransformBatch(preTransforms).transform(elementId, rectTransformationIntrospection)
        return Rectangle2D.Float(transformed.x, transformed.y, transformed.width, transformed.height)
    }

    override fun preTransform(elementId: DiagramElementId, point: Point2D.Float): Point2D.Float {
        return ViewTransformBatch(preTransforms).transform(elementId, point)
    }

    override fun preTransform(elementId: DiagramElementId, point: Point2D.Float, introspection: PointTransformationIntrospection): Point2D.Float {
        return ViewTransformBatch(preTransforms).transform(elementId, point, introspection)
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

    override fun transform(elementId: DiagramElementId, rectTransformationIntrospection: RectangleTransformationIntrospection): Rectangle2D.Float {
        return preTransform(elementId, rectTransformationIntrospection)
    }

    override fun transform(elementId: DiagramElementId, point: Point2D.Float): Point2D.Float {
        return preTransform(elementId, point, PointTransformationIntrospection())
    }

    override fun transform(elementId: DiagramElementId, point: Point2D.Float, introspection: PointTransformationIntrospection): Point2D.Float {
        return preTransform(elementId, point, introspection)
    }
}

data class DragViewTransform(
        val dx: Float, val dy: Float, private val preTransform: PreTransformHandler = PreTransformHandler()
): ViewTransform, PreTransformable by preTransform {

    override fun transform(elementId: DiagramElementId, rectTransformationIntrospection: RectangleTransformationIntrospection): Rectangle2D.Float {
        val preTransformed = preTransform(elementId, rectTransformationIntrospection)
        return Rectangle2D.Float(preTransformed.x + dx, preTransformed.y + dy, preTransformed.width, preTransformed.height)
    }

    override fun transform(elementId: DiagramElementId, point: Point2D.Float): Point2D.Float {
        return transform(elementId, point, PointTransformationIntrospection())
    }

    override fun transform(elementId: DiagramElementId, point: Point2D.Float, introspection: PointTransformationIntrospection): Point2D.Float {
        val preTransformed = preTransform(elementId, point, introspection)
        return Point2D.Float(preTransformed.x + dx, preTransformed.y + dy)
    }
}

data class ResizeViewTransform(
        val cx: Float, val cy: Float, val coefW: Float, val coefH: Float, private val preTransform: PreTransformHandler = PreTransformHandler()
): ViewTransform, PreTransformable by preTransform {

    override fun transform(elementId: DiagramElementId, rectTransformationIntrospection: RectangleTransformationIntrospection): Rectangle2D.Float {
        val preTransformed = preTransform(elementId, rectTransformationIntrospection)
        val left = Point2D.Float(preTransformed.x, preTransformed.y)
        val right = Point2D.Float(preTransformed.x + preTransformed.width, preTransformed.y + preTransformed.height)
        val newLeft = transformPoint(left)
        val newRight = transformPoint(right)

        return Rectangle2D.Float(newLeft.x, newLeft.y, newRight.x - newLeft.x, newRight.y - newLeft.y)
    }

    override fun transform(elementId: DiagramElementId, point: Point2D.Float): Point2D.Float {
        return transform(elementId, point, PointTransformationIntrospection())
    }

    override fun transform(elementId: DiagramElementId, point: Point2D.Float, introspection: PointTransformationIntrospection): Point2D.Float {
        return transformPoint(preTransform(elementId, point, introspection))
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

    private val quirkEpsilon = 1.0f
    private val quirkForRectangles = mutableMapOf<DiagramElementId, RectangleQuirk>()

    override fun transform(elementId: DiagramElementId, rectTransformationIntrospection: RectangleTransformationIntrospection): Rectangle2D.Float {
        val rect = rectTransformationIntrospection.rect
        val transformed = preTransform(elementId, rectTransformationIntrospection)
        if (excludeIds.contains(elementId)) {
            fillRectangleQuirk(elementId, rect, Point2D.Float())
            return transformed
        }

        val halfWidth = transformed.width / 2.0f
        val halfHeight = transformed.height / 2.0f

        val center = transformPoint(Point2D.Float(transformed.x + halfWidth, transformed.y  + halfHeight))

        if (rectTransformationIntrospection.type == AreaType.POINT) {
            val quirkFound = quirkForRectangles[rectTransformationIntrospection.attachedTo]
            if (null != quirkFound) {
                return Rectangle2D.Float(transformed.x + quirkFound.displacement.x, transformed.y + quirkFound.displacement.y, rect.width, rect.height)
            }
        }

        if (elementId == expandedElementId) {
            val left = transformPoint(Point2D.Float(transformed.x, transformed.y))
            val right = transformPoint(Point2D.Float(transformed.x + rect.width, transformed.y  + rect.height))
            return Rectangle2D.Float(left.x, left.y, right.x - left.x, right.y - left.y)
        }

        fillRectangleQuirkIfNeeded(rectTransformationIntrospection, elementId, rect, center, transformed, halfWidth, halfHeight)
        return Rectangle2D.Float(center.x - halfWidth, center.y - halfHeight, rect.width, rect.height)
    }

    private fun fillRectangleQuirkIfNeeded(rectTransformationIntrospection: RectangleTransformationIntrospection, elementId: DiagramElementId, rect: Rectangle2D.Float, center: Point2D.Float, transformed: Rectangle2D.Float, halfWidth: Float, halfHeight: Float) {
        if (rectTransformationIntrospection.type.nests || rectTransformationIntrospection.type == AreaType.SHAPE) {
            fillRectangleQuirk(elementId, rect, Point2D.Float(center.x - transformed.x - halfWidth, center.y - transformed.y - halfHeight))
        }
    }

    private fun fillRectangleQuirk(elementId: DiagramElementId, rect: Rectangle2D.Float, delta: Point2D.Float) {
        quirkForRectangles[elementId] = RectangleQuirk(
                Rectangle2D.Float(
                        rect.x - quirkEpsilon,
                        rect.y - quirkEpsilon,
                        rect.width + quirkEpsilon,
                        rect.height + quirkEpsilon
                ),
                delta
        )
    }

    override fun transform(elementId: DiagramElementId, point: Point2D.Float): Point2D.Float {
        return transform(elementId, point, PointTransformationIntrospection())
    }

    override fun transform(elementId: DiagramElementId, point: Point2D.Float, introspection: PointTransformationIntrospection): Point2D.Float {
        val transformed = preTransform(elementId, point, introspection)
        if (excludeIds.contains(elementId)) {
            return transformed
        }

        quirkForRectangles[elementId]?.apply {
            return transformPoint(transformed)
        }

        val quirkFound = quirkForRectangles[introspection.attachedTo]
        if (null != quirkFound) {
            return Point2D.Float(transformed.x + quirkFound.displacement.x, transformed.y + quirkFound.displacement.y)
        }

        return transformPoint(transformed)
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
            point.x <= cx && point.y <= cy -> Point2D.Float(point.x - dx, point.y - dy)
            // top-right
            point.x >= cx && point.y <= cy -> Point2D.Float(point.x + dx, point.y - dy)
            // bottom-left
            point.x <= cx && point.y >= cy -> Point2D.Float(point.x - dx, point.y + dy)
            // bottom-right
            point.x >= cx && point.y >= cy -> Point2D.Float(point.x + dx, point.y + dy)

            else -> throw IllegalStateException("Unexpected point value: $point for $cx,$cy expand view")
        }
    }

    private data class RectangleQuirk(val originalRectangle2D: Rectangle2D, val displacement: Point2D.Float)
}

class ViewTransformBatch(private val transforms: List<ViewTransform>) {

    fun transform(elementId: DiagramElementId, point: Point2D.Float): Point2D.Float {
        return transform(elementId, point, PointTransformationIntrospection())
    }

    fun transform(elementId: DiagramElementId, point: Point2D.Float, introspection: PointTransformationIntrospection): Point2D.Float {
        val delta = Point2D.Float()
        for (transform in transforms) {
            val transformed = transform.transform(elementId, point, introspection)
            delta.x += transformed.x - point.x
            delta.y += transformed.y - point.y
        }
        return Point2D.Float(point.x + delta.x, point.y + delta.y)
    }

    fun transform(elementId: DiagramElementId, rectTransformationIntrospection: RectangleTransformationIntrospection): Rectangle2D.Float {
        val rect = rectTransformationIntrospection.rect
        val delta = Point2D.Float()
        val sizeDelta = Point2D.Float()
        for (transform in transforms) {
            val transformed = transform.transform(elementId, rectTransformationIntrospection)
            delta.x += transformed.x - rect.x
            delta.y += transformed.y - rect.y
            sizeDelta.x += transformed.width - rect.width
            sizeDelta.y += transformed.height - rect.height
        }
        return Rectangle2D.Float(rect.x + delta.x, rect.y + delta.y, rect.width + sizeDelta.x, rect.height + sizeDelta.y)
    }

    fun isEmpty(): Boolean {
        return transforms.isEmpty()
    }
}

class ViewTransformInverter {
    private val initialStepSize = 1.0f
    private val successMultiplier = 2.0f
    private val failMultiplier = 10.0f
    private val diffStep = 1.0f
    private val epsilon = 1.0f
    private val maxIter = 10
    private val randomInitializationGuesses = 10
    private val randomInitializationAreaSpan = 100.0f

    /**
     * Minimizes (rect.x - transform(return.x)) ^ 2 + (rect.y - transform(return.y)) ^ 2 metric
     * shape changes are ignored so far
     */
    fun invert(elementId: DiagramElementId, target: Point2D.Float, initialGuess: Point2D.Float, batch: ViewTransformBatch): Point2D.Float {
        if (batch.isEmpty()) {
            return target
        }

        return invert(elementId, target, initialGuess, batch, PointTransformationIntrospection())
    }

    /**
     * Minimizes (rect.x - transform(return.x)) ^ 2 + (rect.y - transform(return.y)) ^ 2 metric
     * shape changes are ignored so far
     */
    private fun invert(elementId: DiagramElementId, target: Point2D.Float, initialGuess: Point2D.Float, batch: ViewTransformBatch, introspection: PointTransformationIntrospection): Point2D.Float {
        val range = 1..randomInitializationGuesses
        fun randomFromAreaRange(): Float {
            return (nextFloat() * 2.0f - 1.0f) * randomInitializationAreaSpan
        }

        val result = range.map {
            val guess = Point2D.Float(initialGuess.x - randomFromAreaRange(), initialGuess.y - randomFromAreaRange())
            minimizeGradientDescent(elementId, target, guess, batch, introspection)
        }

        return result.minByOrNull { it: PointWithResidual -> it.residual }!!.point
    }

    private fun minimizeGradientDescent(elementId: DiagramElementId, target: Point2D.Float, initialGuess: Point2D.Float, batch: ViewTransformBatch, introspection: PointTransformationIntrospection): PointWithResidual {
        var currentX = initialGuess.x
        var currentY = initialGuess.y

        var stepSize = initialStepSize
        var residual = metric(elementId, target, batch, currentX, currentY, introspection)
        for (i in 0..maxIter) {
            if (residual < epsilon) {
                break
            }

            val dMetricDx = (metric(elementId, target, batch, currentX + diffStep, currentY, introspection) - residual) / diffStep
            val dMetricDy = (metric(elementId, target, batch, currentX, currentY + diffStep, introspection) - residual) / diffStep

            val newX = currentX - dMetricDx * stepSize
            val newY = currentY - dMetricDy * stepSize
            val newResidual = metric(elementId, target, batch, newX, newY, introspection)
            if (newResidual < residual) {
                currentX = newX
                currentY = newY
                stepSize *= successMultiplier
                residual = newResidual
            } else {
                stepSize /= failMultiplier
            }
        }

        return PointWithResidual(Point2D.Float(currentX, currentY), residual)
    }

    private fun metric(elementId: DiagramElementId, target: Point2D.Float, batch: ViewTransformBatch, currentX: Float, currentY: Float, introspection: PointTransformationIntrospection): Float {
        val transformed = batch.transform(elementId, Point2D.Float(currentX, currentY), introspection)
        val dx = (target.x - transformed.x)
        val dy = (target.y - transformed.y)
        return dx * dx + dy * dy
    }

    private data class PointWithResidual(val point: Point2D.Float, val residual: Float)
}