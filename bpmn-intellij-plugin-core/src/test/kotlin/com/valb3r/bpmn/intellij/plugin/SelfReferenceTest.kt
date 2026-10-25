package com.valb3r.bpmn.intellij.plugin

import com.valb3r.bpmn.intellij.plugin.bpmn.api.diagram.DiagramElementId
import com.valb3r.bpmn.intellij.plugin.bpmn.api.events.EdgeWithIdentifiableWaypoints
import com.valb3r.bpmn.intellij.plugin.bpmn.api.info.PropertyType
import com.valb3r.bpmn.intellij.plugin.core.newelements.registerNewElementsFactory
import com.valb3r.bpmn.intellij.plugin.core.render.elements.hasSelfReference
import com.valb3r.bpmn.intellij.plugin.core.state.currentStateProvider
import com.valb3r.bpmn.intellij.plugin.core.tests.BaseUiTest
import com.valb3r.bpmn.intellij.plugin.flowable.parser.FlowableObjectFactory
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeGreaterThan
import org.amshove.kluent.shouldHaveSingleItem
import org.amshove.kluent.shouldHaveSize
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class SelfReferenceTest : BaseUiTest() {

    @BeforeEach
    fun prepare() {
        registerNewElementsFactory(project, FlowableObjectFactory())
        prepareTwoServiceTaskView()
    }

    @Test
    fun `New edge creation allows the first self reference`() {
        val edge = addSequence(serviceTaskStartDiagramId, serviceTaskStartDiagramId)
        val properties = currentStateProvider(project).currentState().elemPropertiesByStaticElementId[edge.bpmnElement]!!
        properties[PropertyType.SOURCE_REF]!!.value.shouldBeEqualTo(serviceTaskStartBpmnId.id)
        properties[PropertyType.TARGET_REF]!!.value.shouldBeEqualTo(serviceTaskStartBpmnId.id)
        currentStateProvider(project).currentState().edges.shouldHaveSingleItem().id.shouldBeEqualTo(edge.id)
    }

    @Test
    fun `New self reference forms a loop outside the shape with distinct endpoints and right angle bends`() {
        val edge = addSequence(serviceTaskStartDiagramId, serviceTaskStartDiagramId)
        val bounds = currentStateProvider(project).currentState().shapes.first { it.id == serviceTaskStartDiagramId }.rectBounds()
        val points = edge.waypoint.filter { it.physical }

        points.size.shouldBeGreaterThan(3)
        points.first().x.shouldBeEqualTo(bounds.maxX.toFloat())
        points.first().y.shouldBeEqualTo(bounds.centerY.toFloat())
        points.last().x.shouldBeEqualTo(bounds.centerX.toFloat())
        points.last().y.shouldBeEqualTo(bounds.minY.toFloat())
        (points.first().x != points.last().x || points.first().y != points.last().y).shouldBeEqualTo(true)

        points.zipWithNext().forEach { (start, end) ->
            // Each segment has length, follows one axis, and stays outside the shape.
            ((start.x == end.x) xor (start.y == end.y)).shouldBeEqualTo(true)
            bounds.contains((start.x + end.x) / 2.0, (start.y + end.y) / 2.0).shouldBeEqualTo(false)
        }
        points.windowed(3).forEach { (start, bend, end) ->
            val dotProduct = (bend.x - start.x) * (end.x - bend.x) + (bend.y - start.y) * (end.y - bend.y)
            (dotProduct == 0.0f).shouldBeEqualTo(true)
        }
    }

    @Test
    fun `New edge creation rejects a second self reference without changing edges or properties`() {
        addSequence(serviceTaskStartDiagramId, serviceTaskStartDiagramId)
        val before = currentStateProvider(project).currentState()

        addSequenceElementOnFirstTaskTo(serviceTaskStartDiagramId)

        val after = currentStateProvider(project).currentState()
        after.edges.shouldBeEqualTo(before.edges)
        after.elemPropertiesByStaticElementId.shouldBeEqualTo(before.elemPropertiesByStaticElementId)
    }

    @Test
    fun `Different elements can each have a self reference`() {
        addSequence(serviceTaskStartDiagramId, serviceTaskStartDiagramId)
        addSequence(serviceTaskEndDiagramId, serviceTaskEndDiagramId)

        val state = currentStateProvider(project).currentState()
        state.edges.shouldHaveSize(2)
        hasSelfReference(state.elemPropertiesByStaticElementId, serviceTaskStartBpmnId).shouldBeEqualTo(true)
        hasSelfReference(state.elemPropertiesByStaticElementId, serviceTaskEndBpmnId).shouldBeEqualTo(true)
    }

    @Test
    fun `Multiple connections between different elements remain allowed`() {
        addSequence(serviceTaskStartDiagramId, serviceTaskEndDiagramId)
        addSequence(serviceTaskStartDiagramId, serviceTaskEndDiagramId)

        currentStateProvider(project).currentState().edges.shouldHaveSize(2)
    }

    @Test
    fun `Reconnecting target cannot create a second self reference`() {
        addSequence(serviceTaskStartDiagramId, serviceTaskStartDiagramId)
        val edge = addSequence(serviceTaskStartDiagramId, serviceTaskEndDiagramId)

        dragEndpoint(edge, serviceTaskStartDiagramId, source = false)

        assertEdgeUnchanged(edge)
    }

    @Test
    fun `Reconnecting source cannot create a second self reference`() {
        addSequence(serviceTaskEndDiagramId, serviceTaskEndDiagramId)
        val edge = addSequence(serviceTaskStartDiagramId, serviceTaskEndDiagramId)

        dragEndpoint(edge, serviceTaskEndDiagramId, source = true)

        assertEdgeUnchanged(edge)
    }

    @Test
    fun `Reconnecting target can create the first self reference`() {
        val edge = addSequence(serviceTaskStartDiagramId, serviceTaskEndDiagramId)

        dragEndpoint(edge, serviceTaskStartDiagramId, source = false)

        val properties = currentStateProvider(project).currentState().elemPropertiesByStaticElementId[edge.bpmnElement]!!
        properties[PropertyType.TARGET_REF]!!.value.shouldBeEqualTo(serviceTaskStartBpmnId.id)
    }

    @Test
    fun `Reconnecting source can create the first self reference`() {
        val edge = addSequence(serviceTaskStartDiagramId, serviceTaskEndDiagramId)

        dragEndpoint(edge, serviceTaskEndDiagramId, source = true)

        val properties = currentStateProvider(project).currentState().elemPropertiesByStaticElementId[edge.bpmnElement]!!
        properties[PropertyType.SOURCE_REF]!!.value.shouldBeEqualTo(serviceTaskEndBpmnId.id)
    }

    private fun addSequence(source: DiagramElementId, target: DiagramElementId): EdgeWithIdentifiableWaypoints {
        val previousIds = currentStateProvider(project).currentState().edges.map { it.id }.toSet()
        clickOnId(source)
        val anchor = clickOnId(findExactlyOneNewLinkElem().shouldNotBeNull())
        dragToButDontStop(anchor, elementCenter(target))
        canvas.stopDragOrSelect()
        canvas.paintComponent(graphics)
        return currentStateProvider(project).currentState().edges.filter { it.id !in previousIds }.shouldHaveSingleItem()
    }

    private fun dragEndpoint(edge: EdgeWithIdentifiableWaypoints, target: DiagramElementId, source: Boolean) {
        clickOnId(edge.id)
        val endpoint = if (source) edge.waypoint.first() else edge.waypoint.last()
        val point = clickOnId(endpoint.id)
        dragToButDontStop(point, elementCenter(target))
        canvas.stopDragOrSelect()
        canvas.paintComponent(graphics)
    }

    private fun assertEdgeUnchanged(edge: EdgeWithIdentifiableWaypoints) {
        val state = currentStateProvider(project).currentState()
        state.edges.first { it.id == edge.id }.shouldBeEqualTo(edge)
        val properties = state.elemPropertiesByStaticElementId[edge.bpmnElement]!!
        properties[PropertyType.SOURCE_REF]!!.value.shouldBeEqualTo(serviceTaskStartBpmnId.id)
        properties[PropertyType.TARGET_REF]!!.value.shouldBeEqualTo(serviceTaskEndBpmnId.id)
    }
}
