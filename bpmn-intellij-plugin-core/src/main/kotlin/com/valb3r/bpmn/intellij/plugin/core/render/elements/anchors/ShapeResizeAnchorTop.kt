package com.valb3r.bpmn.intellij.plugin.core.render.elements.anchors

import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.BpmnElementId
import com.valb3r.bpmn.intellij.plugin.bpmn.api.diagram.DiagramElementId
import com.valb3r.bpmn.intellij.plugin.bpmn.api.events.Event
import com.valb3r.bpmn.intellij.plugin.core.render.*
import com.valb3r.bpmn.intellij.plugin.core.render.elements.RenderState
import com.valb3r.bpmn.intellij.plugin.core.render.elements.viewtransform.RectangleTransformationIntrospection
import java.awt.geom.Point2D
import java.awt.geom.Rectangle2D
import javax.swing.Icon

// TODO code duplication with ShapeResizeAnchorBottom
class ShapeResizeAnchorTop(
        elementId: DiagramElementId,
        private val parent: DiagramElementId,
        private val bottomPoint: Point2D.Float,
        private val onDragEndCallback: (() -> MutableList<Event>),
        state: () -> RenderState
) : IconAnchorElement(elementId, parent, bottomPoint, state) {

    override fun currentOnScreenRect(camera: Camera): Rectangle2D.Float {
        val icon = icon()
        val iconLeft = camera.fromCameraView(Point2D.Float(0.0f, 0.0f))
        val iconRight = camera.fromCameraView(Point2D.Float(icon.iconWidth.toFloat(), icon.iconHeight.toFloat()))
        val width = iconRight.x - iconLeft.x
        val height = iconRight.y - iconLeft.y

        return state().viewTransform(elementId).transform(
                elementId,
                RectangleTransformationIntrospection(
                        Rectangle2D.Float(
                                bottomPoint.x - width,
                                bottomPoint.y - height,
                                width,
                                height
                        ),
                        AreaType.SHAPE,
                        parents.map { it.elementId },
                        parent
                )
        )
    }

    override fun doOnDragEndWithoutChildren(dx: Float, dy: Float, droppedOn: BpmnElementId?, allDroppedOnAreas: Map<BpmnElementId, AreaWithZindex>): MutableList<Event> {
        val events = mutableListOf<Event>()
        events += super.doOnDragEndWithoutChildren(dx, dy, droppedOn, allDroppedOnAreas)
        events += onDragEndCallback()
        return events
    }

    override fun doRenderWithoutChildren(ctx: RenderContext): Map<DiagramElementId, AreaWithZindex> {
        if (!isVisible() || multipleElementsSelected()) {
            return mutableMapOf()
        }

        return super.doRenderWithoutChildren(ctx)
    }

    override fun icon(): Icon {
        return state().icons.dragToResizeTop
    }

    override fun acceptsInternalEvents(): Boolean {
        return false
    }

    override fun zIndex(): Int {
        return ICON_Z_INDEX
    }
}
