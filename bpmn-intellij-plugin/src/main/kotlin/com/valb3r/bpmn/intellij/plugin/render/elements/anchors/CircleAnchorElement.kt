package com.valb3r.bpmn.intellij.plugin.render.elements.anchors

import com.valb3r.bpmn.intellij.plugin.Colors
import com.valb3r.bpmn.intellij.plugin.bpmn.api.diagram.DiagramElementId
import com.valb3r.bpmn.intellij.plugin.render.AreaType
import com.valb3r.bpmn.intellij.plugin.render.AreaWithZindex
import com.valb3r.bpmn.intellij.plugin.render.Camera
import com.valb3r.bpmn.intellij.plugin.render.RenderContext
import com.valb3r.bpmn.intellij.plugin.state.CurrentState
import java.awt.geom.Point2D
import java.awt.geom.Rectangle2D

abstract class CircleAnchorElement(
        override val elementId: DiagramElementId,
        currentLocation: Point2D.Float,
        private val radius: Float,
        private val bodyColor: Colors,
        state: CurrentState
): AnchorElement(elementId, currentLocation, state) {

    override fun currentRect(camera: Camera): Rectangle2D.Float {
        return Rectangle2D.Float(
                location.x - radius,
                location.y - radius,
                2.0f * radius,
                2.0f * radius
        )
    }

    override fun doRender(ctx: RenderContext): Map<DiagramElementId, AreaWithZindex> {
        if (!isActive()) {
            return mutableMapOf()
        }

        val currentColor = color(isActive(), bodyColor)
        val area = ctx.canvas.drawEllipse(
                currentRect(ctx.canvas.camera),
                currentColor,
                currentColor
        )

        return mutableMapOf(elementId to AreaWithZindex(area, AreaType.POINT))
    }
}