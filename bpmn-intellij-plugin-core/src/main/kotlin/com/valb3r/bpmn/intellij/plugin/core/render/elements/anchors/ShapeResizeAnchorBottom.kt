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

class ShapeResizeAnchorBottom(
        elementId: DiagramElementId,
        private val parent: DiagramElementId,
        private val topPoint: Point2D.Float,
        private val onDragEndCallback: (() -> MutableList<Event>),
        state: () -> RenderState
) : IconAnchorElement(elementId, parent, topPoint, state) {

    override fun currentOnScreenRect(camera: Camera): Rectangle2D.Float {
        val icon = icon()

        return state().viewTransform(elementId).transform(
                elementId,
                RectangleTransformationIntrospection(
                        Rectangle2D.Float(
                                topPoint.x,
                                topPoint.y,
                                icon.iconWidth.toFloat(),
                                icon.iconHeight.toFloat()
                        ),
                        AreaType.SHAPE,
                        viewTransformLevel,
                        attachedTo = parent
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
        return state().icons.dragToResizeBottom
    }

    override fun acceptsInternalEvents(): Boolean {
        return false
    }

    override fun zIndex(): Int {
        return ICON_Z_INDEX
    }
}
