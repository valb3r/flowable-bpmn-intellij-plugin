package com.valb3r.bpmn.intellij.plugin

import java.awt.event.*
import java.awt.geom.Point2D
import javax.swing.SwingUtilities

class MouseEventHandler(private val canvas: Canvas): MouseListener, MouseMotionListener, MouseWheelListener {

    var lastMousePosition: Point2D.Float? = null

    override fun mouseClicked(event: MouseEvent) {
        this.lastMousePosition = Point2D.Float(event.x.toFloat(), event.y.toFloat())
        this.canvas.click(event.point)
    }

    override fun mousePressed(event: MouseEvent) {
        this.lastMousePosition = Point2D.Float(event.x.toFloat(), event.y.toFloat())
    }

    override fun mouseReleased(event: MouseEvent) {
    }

    override fun mouseEntered(event: MouseEvent) {
    }

    override fun mouseExited(event: MouseEvent) {
    }

    override fun mouseDragged(event: MouseEvent) {
        val currentMousePosition = Point2D.Float(event.x.toFloat(), event.y.toFloat())
        if (currentMousePosition == this.lastMousePosition) {
            return
        }

        if (SwingUtilities.isMiddleMouseButton(event)) {
            lastMousePosition?.let { this.canvas.drag(it, currentMousePosition) }
        }

        this.lastMousePosition = Point2D.Float(event.x.toFloat(), event.y.toFloat())
    }

    override fun mouseMoved(event: MouseEvent) {
        this.lastMousePosition = Point2D.Float(event.x.toFloat(), event.y.toFloat())
    }

    override fun mouseWheelMoved(event: MouseWheelEvent) {
        this.lastMousePosition = Point2D.Float(event.x.toFloat(), event.y.toFloat())
        this.canvas.zoom(event.wheelRotation)
    }
}
