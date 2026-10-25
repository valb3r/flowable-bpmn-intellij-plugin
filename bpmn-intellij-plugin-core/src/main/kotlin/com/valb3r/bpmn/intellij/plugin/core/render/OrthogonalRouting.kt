package com.valb3r.bpmn.intellij.plugin.core.render

import com.valb3r.bpmn.intellij.plugin.bpmn.api.diagram.elements.WaypointElement
import com.valb3r.bpmn.intellij.plugin.bpmn.api.events.EdgeWithIdentifiableWaypoints
import com.valb3r.bpmn.intellij.plugin.bpmn.api.events.IdentifiableWaypoint
import java.awt.geom.Point2D
import kotlin.math.abs

const val MIN_ORTHOGONAL_WAYPOINTS = 4

/** Keep endpoints fixed and reuse existing waypoints before adding any new bends. */
fun orthogonalRoute(points: List<Point2D.Float>): List<Point2D.Float> {
    if (points.size < 2) return points
    existingOrthogonalRoute(points)?.let { return it }
    closestRouteUsingExistingWaypoints(points)?.let { return it }

    return createRouteWithIntermediateWaypoints(points)
}

private fun existingOrthogonalRoute(points: List<Point2D.Float>): List<Point2D.Float>? {
    val axisAligned = points.zipWithNext().all { (start, end) -> start.x == end.x || start.y == end.y }
    if (!axisAligned) return null

    return simplifyOrthogonalRoute(points).takeIf { it.size >= MIN_ORTHOGONAL_WAYPOINTS }
}

private data class RouteCandidate(val points: List<Point2D.Float>, val displacement: Double)

private fun closestRouteUsingExistingWaypoints(points: List<Point2D.Float>): List<Point2D.Float>? {
    if (points.size < MIN_ORTHOGONAL_WAYPOINTS) return null
    val candidates = listOf(true, false).mapNotNull { horizontalFirst ->
        val aligned = alignExistingWaypoints(points, horizontalFirst)
        val route = simplifyOrthogonalRoute(aligned)
        if (route.size < MIN_ORTHOGONAL_WAYPOINTS) return@mapNotNull null
        val displacement = aligned.zip(points).sumOf { (moved, original) -> moved.distanceSq(original) }
        RouteCandidate(route, displacement)
    }

    return candidates.minByOrNull { it.displacement }?.points
}

private fun alignExistingWaypoints(points: List<Point2D.Float>, horizontalFirst: Boolean): List<Point2D.Float> {
    val aligned = points.map { Point2D.Float(it.x, it.y) }
    for (index in 0 until aligned.lastIndex) {
        val start = aligned[index]
        val end = aligned[index + 1]
        val horizontal = horizontalFirst == (index % 2 == 0)
        val coordinate = sharedCoordinate(
            if (horizontal) start.y else start.x,
            if (horizontal) end.y else end.x,
            index,
            aligned.lastIndex - 1
        )
        if (horizontal) {
            start.y = coordinate
            end.y = coordinate
        } else {
            start.x = coordinate
            end.x = coordinate
        }
    }

    return aligned
}

/** Keep endpoint coordinates fixed; interior segments share the midpoint coordinate. */
private fun sharedCoordinate(start: Float, end: Float, segmentIndex: Int, lastSegmentIndex: Int): Float {
    return when (segmentIndex) {
        0 -> start
        lastSegmentIndex -> end
        else -> start + (end - start) / 2f
    }
}

private fun createRouteWithIntermediateWaypoints(points: List<Point2D.Float>): List<Point2D.Float> {
    val start = points.first()
    val end = points.last()

    return when {
        start == end -> createClosedLoop(points)
        start.x == end.x -> routeWithVerticalEndpoints(points)
        start.y == end.y -> routeWithHorizontalEndpoints(points)
        abs(end.x - start.x) >= abs(end.y - start.y) -> routeThroughX(start, end, start.x + (end.x - start.x) / 2f)
        else -> routeThroughY(start, end, start.y + (end.y - start.y) / 2f)
    }
}

private fun createClosedLoop(points: List<Point2D.Float>): List<Point2D.Float> {
    // A closed loop needs four nonzero segments, even if fewer waypoints were supplied.
    val start = points.first()
    val x = points.maxByOrNull { abs(it.x - start.x) }!!.x
    val y = points.maxByOrNull { abs(it.y - start.y) }!!.y
    return listOf(start, Point2D.Float(x, start.y), Point2D.Float(x, y), Point2D.Float(start.x, y), start)
}

private fun routeWithVerticalEndpoints(points: List<Point2D.Float>): List<Point2D.Float> {
    val start = points.first()
    val end = points.last()
    val x = points.maxByOrNull { abs(it.x - start.x) }!!.x
    return if (x != start.x) routeThroughX(start, end, x) else straightRoute(start, end)
}

private fun routeWithHorizontalEndpoints(points: List<Point2D.Float>): List<Point2D.Float> {
    val start = points.first()
    val end = points.last()
    val y = points.maxByOrNull { abs(it.y - start.y) }!!.y
    return if (y != start.y) routeThroughY(start, end, y) else straightRoute(start, end)
}

private fun routeThroughX(start: Point2D.Float, end: Point2D.Float, x: Float): List<Point2D.Float> {
    return listOf(start, Point2D.Float(x, start.y), Point2D.Float(x, end.y), end)
}

private fun routeThroughY(start: Point2D.Float, end: Point2D.Float, y: Float): List<Point2D.Float> {
    return listOf(start, Point2D.Float(start.x, y), Point2D.Float(end.x, y), end)
}

private fun straightRoute(start: Point2D.Float, end: Point2D.Float): List<Point2D.Float> {
    val dx = end.x - start.x
    val dy = end.y - start.y
    return listOf(
        start,
        Point2D.Float(start.x + dx / 3f, start.y + dy / 3f),
        Point2D.Float(start.x + 2f * dx / 3f, start.y + 2f * dy / 3f),
        end
    )
}

private fun simplifyOrthogonalRoute(points: List<Point2D.Float>): List<Point2D.Float> {
    val result = mutableListOf<Point2D.Float>()
    points.forEach { point ->
        if (result.lastOrNull() == point) return@forEach
        while (result.size >= 2) {
            val start = result[result.lastIndex - 1]
            val end = result.last()
            if (!areCollinear(start, end, point)) break
            result.removeAt(result.lastIndex)
            if (result.lastOrNull() == point) return@forEach
        }
        result += point
    }

    return result
}

private fun areCollinear(start: Point2D.Float, middle: Point2D.Float, end: Point2D.Float): Boolean {
    return (start.x == middle.x && middle.x == end.x) || (start.y == middle.y && middle.y == end.y)
}

fun EdgeWithIdentifiableWaypoints.orthogonalWaypoints(): List<IdentifiableWaypoint> {
    val points = waypoint.filter { it.physical }
    return orthogonalRoute(points.map { Point2D.Float(it.x, it.y) }).mapIndexed { index, point ->
        WaypointElementState("${id.id}:orthogonal:$index", WaypointElement(point.x, point.y), index)
    }
}

