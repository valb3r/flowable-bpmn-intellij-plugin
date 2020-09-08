package com.valb3r.bpmn.intellij.plugin.render.elements.viewtransform

import com.valb3r.bpmn.intellij.plugin.bpmn.api.diagram.DiagramElementId
import com.valb3r.bpmn.intellij.plugin.render.elements.EPSILON
import java.awt.geom.Line2D
import java.awt.geom.Point2D
import java.awt.geom.Rectangle2D
import kotlin.math.abs
import kotlin.math.max

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
        val transformed = ViewTransformBatch(preTransforms).transform(elementId, Point2D.Float(rect.x, rect.y))
        return Rectangle2D.Float(transformed.x, transformed.y, rect.width, rect.height)
    }

    override fun preTransform(elementId: DiagramElementId, point: Point2D.Float): Point2D.Float {
        return ViewTransformBatch(preTransforms).transform(elementId, point)
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
        if (excludeIds.contains(elementId)) {
            return point
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

    private fun undoTransformPoint(point: Point2D.Float): Point2D.Float {
        return Point2D.Float()
    }

    /**
     * Computed displacement by interpolating original point position against original shape
     * (intersection between shape rectangle and point-shape center line) - original, applying
     * expansion view transform to that point (expanded) and using original-expanded as displacement vector
     */
    private fun computeDisplacementVector(point: Point2D.Float, shapePoints: List<Point2D.Float>, expandedShapePoints: List<Point2D.Float>): Point2D.Float {
        val shapeCenter = Point2D.Float(shape.x + shape.width / 2.0f, shape.y + shape.height / 2.0f)
        val centroid = Line2D.Float(shapeCenter, point)


        val intersectionsOnInnerLineByTandUbest = mutableListOf<CentroidWithRectanglesIntersection>()
        fun tCloseness(t: Float): Float {
            if (t < 0.0f) {
                return abs(t - 10.0f)
            }
            if (t > 1.0f) {
                return abs(t + 10.0f)
            }

            return abs(t)
        }
        shapePoints.forEachIndexed { index, startPoint ->
            val endIndex = if (index == shapePoints.size - 1) -1 else index
            val line = Line2D.Float(startPoint, shapePoints[endIndex + 1])
            val expandedLine = Line2D.Float(expandedShapePoints[index], expandedShapePoints[endIndex + 1])
            val tValue = computeIntersections(line, centroid)
            val uValue = computeIntersections(centroid, line)
            // using uValue as the metric, because it has better bounds - never can be 0.0 and almost never 1.0
            intersectionsOnInnerLineByTandUbest += CentroidWithRectanglesIntersection(expandedLine, line, tValue, max(tCloseness(tValue), tCloseness(uValue)))
        }

        val bestIntersection = intersectionsOnInnerLineByTandUbest.minBy { it.metric }!!
        val expandedPointOnOuterRect = pointFromTvalue(bestIntersection.expandedLine, bestIntersection.tValueOnOriginal)
        val intersectionPointOnInnerRect = pointFromTvalue(bestIntersection.originalLine, bestIntersection.tValueOnOriginal)

        return Point2D.Float(expandedPointOnOuterRect.x - intersectionPointOnInnerRect.x, expandedPointOnOuterRect.y - intersectionPointOnInnerRect.y)
    }

    private fun pointFromTvalue(line: Line2D.Float, tValue: Float): Point2D.Float {
        return Point2D.Float(
                line.x1 + (line.x2 - line.x1) * tValue,
                line.y1 + (line.y2 - line.y1) * tValue
        )
    }

    // t-parameter value of the intersection on lineOne
    private fun computeIntersections(lineOne: Line2D.Float, lineTwo: Line2D.Float): Float {
        val dLineTwoY = lineTwo.y1 - lineTwo.y2
        val dLineTwoX = lineTwo.x1 - lineTwo.x2

        return ((lineOne.x1 - lineTwo.x1) * dLineTwoY - (lineOne.y1 - lineTwo.y1) * dLineTwoX) / ((lineOne.x1 - lineOne.x2) * dLineTwoY - (lineOne.y1 - lineOne.y2) * dLineTwoX)
    }

    private data class CentroidWithRectanglesIntersection(val expandedLine: Line2D.Float, val originalLine: Line2D.Float, val tValueOnOriginal: Float, val metric: Float)
}
class ViewTransformBatch(private val transforms: List<ViewTransform>) {

    fun transform(elementId: DiagramElementId, point: Point2D.Float): Point2D.Float {
        val delta = Point2D.Float()
        for (transform in transforms) {
            val transformed = transform.transform(elementId, point)
            delta.x += transformed.x - point.x
            delta.y += transformed.y - point.y
        }
        return Point2D.Float(point.x + delta.x, point.y + delta.y)
    }
}

class ViewTransformInverter {
    private val initialStepSize = 1.0f
    private val successMultiplier = 2.0f
    private val failMultiplier = 10.0f
    private val diffStep = 1.0f
    private val epsilon = 1.0f
    private val maxIter = 10

    /**
     * Minimizes (rect.x - transform(return.x)) ^ 2 + (rect.y - transform(return.y)) ^ 2 metric
     * shape changes are ignored so far
     */
    fun invert(elementId: DiagramElementId, target: Point2D.Float, batch: ViewTransformBatch): Point2D.Float {
        return minimizeGradientDescent(elementId, target, batch)
    }

    private fun minimizeGradientDescent(elementId: DiagramElementId, target: Point2D.Float, batch: ViewTransformBatch): Point2D.Float {
        var currentX = target.x
        var currentY = target.y

        var stepSize = initialStepSize
        var residual = metric(elementId, target, batch, currentX, currentY)
        for (i in 0..maxIter) {
            if (residual < epsilon) {
                break
            }

            val dx = (metric(elementId, target, batch, currentX + diffStep, currentY) - residual) / diffStep
            val dy = (metric(elementId, target, batch, currentX, currentY + diffStep) - residual) / diffStep

            val newX = currentX - dx * stepSize
            val newY = currentY - dy * stepSize
            val newResidual = metric(elementId, target, batch, newX, newY)
            if (newResidual < residual) {
                currentX = newX
                currentY = newY
                stepSize *= successMultiplier
                residual = newResidual
            } else {
                stepSize /= failMultiplier
            }
        }

        return Point2D.Float(currentX, currentY)
    }

    private fun metric(elementId: DiagramElementId, target: Point2D.Float, batch: ViewTransformBatch, currentX: Float, currentY: Float): Float {
        val transformed = batch.transform(elementId, Point2D.Float(currentX, currentY))
        val dx = (target.x - transformed.x)
        val dy = (target.y - transformed.y)
        return dx * dx + dy * dy
    }
}