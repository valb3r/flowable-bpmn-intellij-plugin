package com.valb3r.bpmn.intellij.plugin.render.elements.anchors

import com.valb3r.bpmn.intellij.plugin.bpmn.api.diagram.DiagramElementId
import com.valb3r.bpmn.intellij.plugin.render.AreaWithZindex
import com.valb3r.bpmn.intellij.plugin.render.Camera
import com.valb3r.bpmn.intellij.plugin.render.RenderContext
import com.valb3r.bpmn.intellij.plugin.state.CurrentState
import java.awt.geom.Point2D
import java.awt.geom.Rectangle2D
import javax.swing.Icon

class ShapeResizeAnchorBottom(
        override val elementId: DiagramElementId,
        private val topPoint: Point2D.Float,
        state: CurrentState
): AnchorElement(elementId, topPoint, state) {

    override fun currentRect(camera: Camera): Rectangle2D.Float {
        val icon = icon()

        return Rectangle2D.Float(
                topPoint.x,
                topPoint.y,
                icon.iconWidth.toFloat(),
                icon.iconHeight.toFloat()
        )
    }

    override fun doRender(ctx: RenderContext): Map<DiagramElementId, AreaWithZindex> {
        if (!isActiveOrDragged()) {
            return mutableMapOf()
        }

        return super.render(ctx)
    }

    override fun icon(): Icon {
        return state.iconProvider.dragToResizeBottom
    }
}