package com.valb3r.bpmn.intellij.plugin

import com.valb3r.bpmn.intellij.plugin.bpmn.api.diagram.DiagramElementId
import com.valb3r.bpmn.intellij.plugin.bpmn.api.events.EdgeWithIdentifiableWaypoints
import com.valb3r.bpmn.intellij.plugin.core.events.updateEventsRegistry
import com.valb3r.bpmn.intellij.plugin.core.newelements.registerNewElementsFactory
import com.valb3r.bpmn.intellij.plugin.core.render.MIN_ORTHOGONAL_WAYPOINTS
import com.valb3r.bpmn.intellij.plugin.core.render.elements.anchors.orthoIconIdPrefix
import com.valb3r.bpmn.intellij.plugin.core.state.currentStateProvider
import com.valb3r.bpmn.intellij.plugin.core.tests.BaseUiTest
import com.valb3r.bpmn.intellij.plugin.flowable.parser.FlowableObjectFactory
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.awt.geom.Point2D

internal class OrthogonalSequenceFlowTest : BaseUiTest() {

    @BeforeEach
    fun prepare() {
        registerNewElementsFactory(project, FlowableObjectFactory())
        prepareExclusiveGatewayAttachedToServiceTaskWithFlowSequence()
    }

    @Test
    fun `Sequence action provides two intermediate waypoints and can be undone and redone in one step`() {
        val before = edge()
        val properties = currentStateProvider(project).currentState().elemPropertiesByStaticElementId
        clickOnId(sequenceFlowDiagramId)
        clickOnId(DiagramElementId(orthoIconIdPrefix + sequenceFlowDiagramId.id))

        val converted = edge()
        axisAligned(converted).shouldBeEqualTo(true)
        converted.waypoint.count { it.physical }.shouldBeEqualTo(MIN_ORTHOGONAL_WAYPOINTS)
        endpoints(converted).shouldBeEqualTo(endpoints(before))
        converted.epoch.shouldBeEqualTo(before.epoch + 1)
        currentStateProvider(project).currentState().elemPropertiesByStaticElementId.shouldBeEqualTo(properties)
        renderResult!!.areas.containsKey(DiagramElementId(orthoIconIdPrefix + sequenceFlowDiagramId.id)).shouldBeEqualTo(false)

        updateEventsRegistry(project).undo()
        edge().shouldBeEqualTo(before)
        updateEventsRegistry(project).redo()
        edge().shouldBeEqualTo(converted)
    }

    @Test
    fun `Conversion is a one time action and later waypoint edits remain free form`() {
        clickOnId(sequenceFlowDiagramId)
        clickOnId(DiagramElementId(orthoIconIdPrefix + sequenceFlowDiagramId.id))
        val before = edge()
        val corner = before.waypoint.filter { it.physical }[1]
        val point = clickOnId(corner.id)
        dragToButDontStop(point, Point2D.Float(point.x + 40f, point.y + 40f))
        canvas.stopDragOrSelect()
        canvas.paintComponent(graphics)

        axisAligned(edge()).shouldBeEqualTo(false)
        edge().waypoint.count { it.physical }.shouldBeEqualTo(before.waypoint.count { it.physical })
        endpoints(edge()).shouldBeEqualTo(endpoints(before))
        updateEventsRegistry(project).undo()
        edge().shouldBeEqualTo(before)
    }

    private fun edge() = currentStateProvider(project).currentState().edges.first { it.id == sequenceFlowDiagramId }

    private fun endpoints(edge: EdgeWithIdentifiableWaypoints) = listOf(edge.waypoint.first(), edge.waypoint.last()).map { it.asWaypointElement() }

    private fun axisAligned(edge: EdgeWithIdentifiableWaypoints) = edge.waypoint.filter { it.physical }.zipWithNext().all { (start, end) ->
        (start.x == end.x) xor (start.y == end.y)
    }
}
