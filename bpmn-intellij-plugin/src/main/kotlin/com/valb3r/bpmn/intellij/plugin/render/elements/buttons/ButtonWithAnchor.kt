package com.valb3r.bpmn.intellij.plugin.render.elements.buttons

import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.BpmnElementId
import com.valb3r.bpmn.intellij.plugin.bpmn.api.diagram.DiagramElementId
import com.valb3r.bpmn.intellij.plugin.bpmn.api.events.Event
import com.valb3r.bpmn.intellij.plugin.render.*
import com.valb3r.bpmn.intellij.plugin.render.elements.Anchor
import com.valb3r.bpmn.intellij.plugin.render.elements.RenderState
import com.valb3r.bpmn.intellij.plugin.render.elements.anchors.IconAnchorElement
import com.valb3r.bpmn.intellij.plugin.render.elements.viewtransform.RectangleWithType
import com.valb3r.bpmn.intellij.plugin.render.elements.viewtransform.TransformationIntrospection
import java.awt.geom.Point2D
import java.awt.geom.Rectangle2D
import javax.swing.Icon

class ButtonWithAnchor(
        override val elementId: DiagramElementId,
        private val bottomPoint: Point2D.Float,
        private val icon: Icon,
        private val onClick: (() -> MutableList<Event>),
        state: RenderState
) : IconAnchorElement(elementId, bottomPoint, state) {

    override fun render(): MutableMap<DiagramElementId, AreaWithZindex> {
        state.ctx.interactionContext.clickCallbacks[elementId] = { dest -> dest.addEvents(onClick()) }
        return super.render()
    }

    override fun currentOnScreenRect(camera: Camera): Rectangle2D.Float {
        val icon = icon()
        val left = camera.fromCameraView(Point2D.Float(0.0f, 0.0f))
        val right = camera.fromCameraView(Point2D.Float(icon.iconWidth.toFloat(), icon.iconHeight.toFloat()))
        val imageWidth = right.x - left.x
        val imageHeight = right.y - left.y

        return viewTransform.transform(
                elementId,
                RectangleWithType(
                        Rectangle2D.Float(
                                bottomPoint.x - imageWidth,
                                bottomPoint.y - imageHeight,
                                imageWidth,
                                imageHeight
                        ),
                        AreaType.POINT
                ),
                TransformationIntrospection(setOf(), setOf())
        )
    }

    override fun doOnDragEndWithoutChildren(dx: Float, dy: Float, droppedOn: BpmnElementId?, allDroppedOnAreas: Map<BpmnElementId, AreaWithZindex>): MutableList<Event> {
        return mutableListOf()
    }

    override fun doRenderWithoutChildren(ctx: RenderContext): Map<DiagramElementId, AreaWithZindex> {
        val result = super.doRenderWithoutChildren(ctx).toMutableMap()
        result[elementId]?.let {
            result[elementId] = it.copy(areaType = AreaType.POINT, anchorsForWaypoints = waypointAnchors(ctx.canvas.camera))
        }
        return result
    }

    override fun icon(): Icon {
        return icon
    }

    override fun acceptsInternalEvents(): Boolean {
        return false
    }

    override fun zIndex(): Int {
        return ICON_Z_INDEX
    }

    override fun waypointAnchors(camera: Camera): MutableSet<Anchor> {
        return mutableSetOf(Anchor(transformedLocation))
    }
}