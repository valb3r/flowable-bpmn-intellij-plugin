package com.valb3r.bpmn.intellij.plugin.render.elements.anchors

import com.valb3r.bpmn.intellij.plugin.Colors
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.BpmnElementId
import com.valb3r.bpmn.intellij.plugin.bpmn.api.diagram.DiagramElementId
import com.valb3r.bpmn.intellij.plugin.render.AreaType
import com.valb3r.bpmn.intellij.plugin.render.AreaWithZindex
import com.valb3r.bpmn.intellij.plugin.render.Camera
import com.valb3r.bpmn.intellij.plugin.render.RenderContext
import com.valb3r.bpmn.intellij.plugin.render.elements.BaseRenderElement
import com.valb3r.bpmn.intellij.plugin.render.elements.State
import com.valb3r.bpmn.intellij.plugin.state.CurrentState
import java.awt.Event
import java.awt.geom.Point2D
import javax.swing.Icon

abstract class AnchorElement(
        private val id: DiagramElementId,
        private val currentLocation: Point2D.Float,
        elemState: State,
        state: CurrentState,
        parent: BaseRenderElement?
): BaseRenderElement(elemState, state, parent) {

    val location: Point2D.Float
        get() = currentLocation

    override fun doRender(ctx: RenderContext): Map<DiagramElementId, AreaWithZindex> {
        val icon = icon()
        val active = isActive()
        val rect = currentRect(ctx.canvas.camera)
        val area = ctx.canvas.drawIcon(Point2D.Float(rect.x, rect.y), icon, if (active) Colors.SELECTED_COLOR.color else null)

        return mutableMapOf(id to AreaWithZindex(area, AreaType.POINT))
    }

    override fun doDragToWithoutChildren(dx: Float, dy: Float, droppedOn: BpmnElementId?) {
        TODO("Not yet implemented")
    }

    override fun doOnDragEndWithoutChildren(dx: Float, dy: Float, droppedOn: BpmnElementId?): MutableList<Event> {
        TODO("Not yet implemented")
    }

    override fun doResizeWithoutChildren(dw: Float, dh: Float) {
        // NOP
    }

    override fun doResizeEndWithoutChildren(dw: Float, dh: Float): MutableList<Event> {
        return mutableListOf()
    }

    override fun waypointAnchors(camera: Camera): MutableSet<Point2D.Float> {
        return mutableSetOf()
    }

    override fun shapeAnchors(camera: Camera): MutableSet<Point2D.Float> {
        return mutableSetOf()
    }

    protected abstract fun icon(): Icon
}