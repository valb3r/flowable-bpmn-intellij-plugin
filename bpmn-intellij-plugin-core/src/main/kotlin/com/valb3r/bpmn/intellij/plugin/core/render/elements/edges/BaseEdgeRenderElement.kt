package com.valb3r.bpmn.intellij.plugin.core.render.elements.edges

import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.BpmnElementId
import com.valb3r.bpmn.intellij.plugin.bpmn.api.diagram.DiagramElementId
import com.valb3r.bpmn.intellij.plugin.bpmn.api.events.EdgeWithIdentifiableWaypoints
import com.valb3r.bpmn.intellij.plugin.bpmn.api.events.Event
import com.valb3r.bpmn.intellij.plugin.bpmn.api.info.PropertyType
import com.valb3r.bpmn.intellij.plugin.core.Colors
import com.valb3r.bpmn.intellij.plugin.core.render.AreaType
import com.valb3r.bpmn.intellij.plugin.core.render.AreaWithZindex
import com.valb3r.bpmn.intellij.plugin.core.render.Camera
import com.valb3r.bpmn.intellij.plugin.core.render.RenderContext
import com.valb3r.bpmn.intellij.plugin.core.render.elements.Anchor
import com.valb3r.bpmn.intellij.plugin.core.render.elements.BaseBpmnRenderElement
import com.valb3r.bpmn.intellij.plugin.core.render.elements.BaseDiagramRenderElement
import com.valb3r.bpmn.intellij.plugin.core.render.elements.RenderState
import com.valb3r.bpmn.intellij.plugin.core.render.elements.anchors.AnchorElement
import com.valb3r.bpmn.intellij.plugin.core.render.elements.anchors.PhysicalWaypoint
import com.valb3r.bpmn.intellij.plugin.core.render.elements.anchors.VirtualWaypoint
import com.valb3r.bpmn.intellij.plugin.core.render.elements.viewtransform.PointTransformationIntrospection
import com.valb3r.bpmn.intellij.plugin.core.render.elements.viewtransform.RectangleTransformationIntrospection
import java.awt.Color
import java.awt.geom.Area
import java.awt.geom.Point2D
import java.awt.geom.Rectangle2D

abstract class BaseEdgeRenderElement(
        elementId: DiagramElementId,
        bpmnElementId: BpmnElementId,
        protected val edge: EdgeWithIdentifiableWaypoints,
        private val edgeColor: Colors,
        state: () -> RenderState
): BaseBpmnRenderElement(elementId, bpmnElementId, state) {

    private val anchors = computeAnchors()

    override var viewTransformLevel: DiagramElementId? = null
        get() = super.viewTransformLevel
        set(value) {
            super.viewTransformLevel = value
            field = value
            anchors.forEach { it.viewTransformLevel = value}
        }

    override val children: List<BaseDiagramRenderElement> = anchors as MutableList<BaseDiagramRenderElement> + innerElements

    val edgeElem: EdgeWithIdentifiableWaypoints
        get() = edge

    override fun dragTo(dx: Float, dy: Float) {
        if (multipleElementsSelected() && isActiveOrDragged()) {
            return
        }

        super.dragTo(dx, dy)
    }

    override fun doRenderWithoutChildren(ctx: RenderContext): Map<DiagramElementId, AreaWithZindex> {
        val area = Area()

        val activeWaypoints = anchors.filter { it is PhysicalWaypoint || (!multipleElementsSelected() && it.isActiveOrDragged()) }
        val updatedAnchors = activeWaypoints.map { it.transformedLocation }

        updatedAnchors.forEachIndexed {pos, waypoint ->
            when {
                pos == updatedAnchors.size - 1 -> area.add(ctx.canvas.drawLineWithArrow(updatedAnchors[pos - 1], waypoint, color(isActiveEdge(pos, activeWaypoints), edgeColor)))
                pos > 0 -> area.add(ctx.canvas.drawLine(updatedAnchors[pos - 1], waypoint, color(isActiveEdge(pos, activeWaypoints), edgeColor)))
            }
        }

        drawHistoricalLabel()
        drawNameIfAvailable(updatedAnchors, color(isActive(), edgeColor))

        area.add(renderDefaultMarkIfNeeded(ctx, anchors.filterIsInstance<PhysicalWaypoint>().map { it.transformedLocation }))
        return mapOf(elementId to AreaWithZindex(area, AreaType.EDGE, waypointAnchors(ctx.canvas.camera), index = zIndex()))
    }

    override fun doDragToWithoutChildren(dx: Float, dy: Float) {
        // NOP
    }

    override fun doOnDragEndWithoutChildren(dx: Float, dy: Float, droppedOn: BpmnElementId?, allDroppedOnAreas: Map<BpmnElementId, AreaWithZindex>): MutableList<Event> {
        return mutableListOf()
    }

    override fun doResizeWithoutChildren(dw: Float, dh: Float) {
        TODO("Not yet implemented")
    }

    override fun doResizeEndWithoutChildren(dw: Float, dh: Float): MutableList<Event> {
        TODO("Not yet implemented")
    }

    override fun waypointAnchors(camera: Camera): MutableSet<Anchor> {
        return edge.waypoint.filter { it.physical && !state().ctx.selectedIds.contains(it.id) }.map { Anchor(Point2D.Float(it.x, it.y)) }.toMutableSet()
    }

    override fun shapeAnchors(camera: Camera): MutableSet<Anchor> {
        return mutableSetOf()
    }

    override fun currentOnScreenRect(camera: Camera): Rectangle2D.Float {
        val elems = anchors.filterIsInstance<PhysicalWaypoint>().map { it.transformedLocation }
        val stX = elems.minBy { it.x }?.x ?: 0.0f
        val stY = elems.minBy { it.y }?.y ?: 0.0f
        val enX = elems.maxBy { it.x }?.x ?: 0.0f
        val enY = elems.maxBy { it.y }?.y ?: 0.0f

        return Rectangle2D.Float(
            stX,
            stY,
            enX - stX,
            enY - stY
        )
    }

    override fun currentRect(): Rectangle2D.Float {
        return Rectangle2D.Float(edge.waypoint[0].x, edge.waypoint[0].y, 0.0f, 0.0f)
    }

    private fun drawNameIfAvailable(waypoints: List<Point2D.Float>, color: Color) {
        val name = state().currentState.elemPropertiesByStaticElementId[bpmnElementId]?.get(PropertyType.NAME)?.value as String? ?: return
        val longestSegment = waypoints
                .mapIndexedNotNull {pos, it -> if (0 == pos) null else Pair(waypoints[pos - 1], it)}
            .maxBy { it.first.distance(it.second) } ?: return
        state().ctx.canvas.drawWrappedSingleLine(longestSegment.first, longestSegment.second, name, color)
    }

    private fun drawHistoricalLabel() {
        if (!state().history.contains(bpmnElementId)) {
            return
        }

        val indexes = state().history.mapIndexed { pos, id -> if (id == bpmnElementId) pos else null }.filterNotNull()
        val midPoints = anchors.filterIsInstance<VirtualWaypoint>().map { it.transformedLocation }
        state().ctx.canvas.drawTextNoCameraTransform(midPoints[midPoints.size / 2], indexes.toString(), Colors.INNER_TEXT_COLOR.color, Colors.DEBUG_ELEMENT_COLOR.color)
    }

    private fun renderDefaultMarkIfNeeded(ctx: RenderContext, anchors: List<Point2D.Float>): Area {
        val sourceRefOfExists = state().currentState.propertyWithElementByPropertyType[PropertyType.DEFAULT_FLOW]?.any { it.value.value == bpmnElementId.id } ?: false
        if (!sourceRefOfExists) {
            return Area()
        }

        return ctx.canvas.drawLineSlash(anchors[0], anchors[1], color(edgeColor))
    }

    private fun isActiveEdge(endPos: Int, activeAnchors: List<AnchorElement>): Boolean {
        if (isActive()) {
            return true
        }
        val leftAnchorActive = activeAnchors[endPos - 1].isActive()
        val rightAnchorActive = activeAnchors[endPos].isActive()
        val anyAnchorActive = leftAnchorActive || rightAnchorActive

        return (leftAnchorActive && rightAnchorActive) || (anyAnchorActive && endPos == activeAnchors.size - 1) || (anyAnchorActive && endPos - 1 == 0)
    }

    private fun computeAnchors(): MutableList<AnchorElement> {
        val numPhysicals = edge.waypoint.filter { it.physical }.size
        var physicalPos = -1
        return edge.waypoint.map {
            val result = if (it.physical) {
                physicalPos++
                PhysicalWaypoint(it.id, findAttachedToElement(physicalPos, numPhysicals), edge.id, edge.bpmnElement, edge, physicalPos, numPhysicals, Point2D.Float(it.x, it.y), state).let { it.parents.add(this); it }
            } else {
                VirtualWaypoint(it.id, edge.id, edge, Point2D.Float(it.x, it.y), state).let { it.parents.add(this); it }
            }
            result.viewTransformLevel = viewTransformLevel
            return@map result
        }.toMutableList()
    }

    private fun findAttachedToElement(physicalPos: Int, numPhysicals: Int): DiagramElementId? {
        return when (physicalPos) {
            0 -> {
                val bpmnElemId = state().currentState.elemPropertiesByStaticElementId[bpmnElementId]?.get(PropertyType.SOURCE_REF)?.value as String?
                bpmnElemId?.let {state().currentState.diagramByElementId[BpmnElementId(it)] }
            }
            numPhysicals - 1 -> {
                val bpmnElemId = state().currentState.elemPropertiesByStaticElementId[bpmnElementId]?.get(PropertyType.TARGET_REF)?.value as String?
                bpmnElemId?.let {state().currentState.diagramByElementId[BpmnElementId(it)] }
            }
            else -> null
        }
    }
}
