package com.valb3r.bpmn.intellij.plugin

import com.valb3r.bpmn.intellij.plugin.core.render.orthogonalRoute
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.Test
import java.awt.geom.Point2D

internal class OrthogonalRoutingTest {

    @Test
    fun `Diagonal routes in every direction retain endpoints and contain only right angle bends`() {
        listOf(120f to 40f, 40f to 120f, -120f to 40f, 40f to -120f, -120f to -40f).forEach { (x, y) ->
            val start = Point2D.Float(10f, 20f)
            val end = Point2D.Float(x, y)
            val route = orthogonalRoute(listOf(start, end))

            route.first().shouldBeEqualTo(start)
            route.last().shouldBeEqualTo(end)
            route.size.shouldBeEqualTo(4)
            assertRightAngles(route)
        }
    }

    @Test
    fun `Two existing corners are aligned to the closest orientation without adding waypoints`() {
        val start = Point2D.Float(0f, 0f)
        val end = Point2D.Float(100f, 100f)

        val horizontalFirst = orthogonalRoute(listOf(start, Point2D.Float(40f, 5f), Point2D.Float(60f, 90f), end))
        horizontalFirst.shouldBeEqualTo(listOf(start, Point2D.Float(50f, 0f), Point2D.Float(50f, 100f), end))
        assertRightAngles(horizontalFirst)

        val verticalFirst = orthogonalRoute(listOf(start, Point2D.Float(5f, 40f), Point2D.Float(90f, 60f), end))
        verticalFirst.shouldBeEqualTo(listOf(start, Point2D.Float(0f, 50f), Point2D.Float(100f, 50f), end))
        assertRightAngles(verticalFirst)
    }

    @Test
    fun `A single existing corner is supplemented with a second intermediate waypoint`() {
        val start = Point2D.Float(0f, 0f)
        val end = Point2D.Float(100f, 100f)
        listOf(Point2D.Float(80f, 10f), Point2D.Float(100f, 0f)).forEach { corner ->
            val route = orthogonalRoute(listOf(start, corner, end))
            route.size.shouldBeEqualTo(4)
            route.first().shouldBeEqualTo(start)
            route.last().shouldBeEqualTo(end)
            assertRightAngles(route)
        }
    }

    @Test
    fun `Straight flows retain two intermediate waypoints`() {
        val start = Point2D.Float(0f, 0f)
        val end = Point2D.Float(90f, 0f)
        val route = orthogonalRoute(listOf(start, end))
        route.shouldBeEqualTo(listOf(start, Point2D.Float(30f, 0f), Point2D.Float(60f, 0f), end))
        orthogonalRoute(route).shouldBeEqualTo(route)
    }

    @Test
    fun `Several existing corners are aligned without increasing the waypoint count`() {
        val points = listOf(Point2D.Float(0f, 0f), Point2D.Float(30f, 10f), Point2D.Float(60f, 70f), Point2D.Float(100f, 100f))
        val route = orthogonalRoute(points)

        route.size.shouldBeEqualTo(points.size)
        route.first().shouldBeEqualTo(points.first())
        route.last().shouldBeEqualTo(points.last())
        assertRightAngles(route)
    }

    @Test
    fun `A closed loop reuses existing corners when there are enough waypoints`() {
        val start = Point2D.Float(0f, 0f)
        val points = listOf(start, Point2D.Float(90f, 10f), Point2D.Float(110f, 100f), Point2D.Float(10f, 90f), start)
        val route = orthogonalRoute(points)

        route.size.shouldBeEqualTo(points.size)
        route.first().shouldBeEqualTo(start)
        route.last().shouldBeEqualTo(start)
        assertRightAngles(route)
    }

    @Test
    fun `Duplicate and collinear waypoints are removed without changing a loop`() {
        val route = listOf(
            Point2D.Float(0f, 0f), Point2D.Float(10f, 0f), Point2D.Float(10f, 10f),
            Point2D.Float(0f, 10f), Point2D.Float(0f, 0f)
        )
        orthogonalRoute(route).shouldBeEqualTo(route)
        orthogonalRoute(listOf(route[0], route[0], Point2D.Float(5f, 0f)) + route.drop(1)).shouldBeEqualTo(route)
        assertRightAngles(route)
    }

    @Test
    fun `A closed diagonal route remains a visible loop`() {
        val start = Point2D.Float(0f, 0f)
        val route = orthogonalRoute(listOf(start, Point2D.Float(100f, 100f), start))
        route.first().shouldBeEqualTo(start)
        route.last().shouldBeEqualTo(start)
        route.size.shouldBeEqualTo(5)
        assertRightAngles(route)
    }

    @Test
    fun `Aligned endpoints keep a detour with two intermediate waypoints`() {
        val route = orthogonalRoute(listOf(Point2D.Float(0f, 0f), Point2D.Float(100f, 100f), Point2D.Float(0f, 200f)))
        route.first().shouldBeEqualTo(Point2D.Float(0f, 0f))
        route.last().shouldBeEqualTo(Point2D.Float(0f, 200f))
        route.size.shouldBeEqualTo(4)
        assertRightAngles(route)
    }

    private fun assertRightAngles(route: List<Point2D.Float>) {
        (route.size >= 4).shouldBeEqualTo(true)
        route.zipWithNext().forEach { (start, end) ->
            ((start.x == end.x) xor (start.y == end.y)).shouldBeEqualTo(true)
        }
        route.windowed(3).forEach { (start, bend, end) ->
            ((bend.x - start.x) * (end.x - bend.x) + (bend.y - start.y) * (end.y - bend.y)).shouldBeEqualTo(0f)
            ((start.x == bend.x) != (bend.x == end.x)).shouldBeEqualTo(true)
        }
    }
}
