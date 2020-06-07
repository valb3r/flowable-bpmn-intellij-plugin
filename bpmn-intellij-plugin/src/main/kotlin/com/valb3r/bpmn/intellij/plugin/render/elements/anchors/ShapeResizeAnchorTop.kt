package com.valb3r.bpmn.intellij.plugin.render.elements.anchors

import com.valb3r.bpmn.intellij.plugin.bpmn.api.diagram.DiagramElementId
import com.valb3r.bpmn.intellij.plugin.render.Camera
import com.valb3r.bpmn.intellij.plugin.render.elements.BaseRenderElement
import com.valb3r.bpmn.intellij.plugin.render.elements.State
import com.valb3r.bpmn.intellij.plugin.state.CurrentState
import java.awt.geom.Point2D
import java.awt.geom.Rectangle2D
import javax.swing.Icon

class ShapeResizeAnchorTop(
        val id: DiagramElementId,
        private val bottomPoint: Point2D.Float,
        elemState: State,
        state: CurrentState,
        parent: BaseRenderElement?
): AnchorElement(id, bottomPoint, elemState, state, parent) {

    override fun currentRect(camera: Camera): Rectangle2D.Float {
        val icon = icon()
        val iconLeft = camera.fromCameraView(Point2D.Float(0.0f, 0.0f))
        val iconRight = camera.fromCameraView(Point2D.Float(icon.iconWidth.toFloat(), icon.iconHeight.toFloat()))
        val width = iconRight.x - iconLeft.x
        val height = iconRight.y - iconLeft.y

        return Rectangle2D.Float(
                bottomPoint.x - width,
                bottomPoint.y - height,
                width,
                height
        )
    }

    override fun icon(): Icon {
        return state.iconProvider.dragToResizeTop
    }
}