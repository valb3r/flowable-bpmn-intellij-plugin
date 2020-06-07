package com.valb3r.bpmn.intellij.plugin.render.elements.anchors

import com.valb3r.bpmn.intellij.plugin.bpmn.api.diagram.DiagramElementId
import com.valb3r.bpmn.intellij.plugin.render.Camera
import com.valb3r.bpmn.intellij.plugin.render.elements.BaseRenderElement
import com.valb3r.bpmn.intellij.plugin.render.elements.State
import com.valb3r.bpmn.intellij.plugin.state.CurrentState
import java.awt.geom.Point2D
import java.awt.geom.Rectangle2D
import javax.swing.Icon

class ShapeResizeAnchorBottom(
        id: DiagramElementId,
        private val topPoint: Point2D.Float,
        elemState: State,
        state: CurrentState,
        parent: BaseRenderElement?
): AnchorElement(id, topPoint, elemState, state, parent) {

    override fun currentRect(camera: Camera): Rectangle2D.Float {
        val icon = icon()

        return Rectangle2D.Float(
                topPoint.x,
                topPoint.y,
                icon.iconWidth.toFloat(),
                icon.iconHeight.toFloat()
        )
    }

    override fun icon(): Icon {
        return state.iconProvider.dragToResizeBottom
    }
}