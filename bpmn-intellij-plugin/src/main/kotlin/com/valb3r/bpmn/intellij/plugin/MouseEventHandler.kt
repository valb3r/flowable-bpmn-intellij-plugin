package com.valb3r.bpmn.intellij.plugin

import java.awt.event.*
import java.awt.geom.Point2D

class MouseEventHandler(private val canvas: Canvas): MouseListener, MouseMotionListener, MouseWheelListener {
    private val lastMousePosition = Point2D.Float()

    override fun mouseClicked(event: MouseEvent) {
        this.canvas.mouseClick(event.point)
    }

    override fun mousePressed(event: MouseEvent) {
        this.lastMousePosition.setLocation(event.x.toFloat(), event.y.toFloat())
    }

    override fun mouseReleased(event: MouseEvent) {
    }

    override fun mouseEntered(event: MouseEvent) {
    }

    override fun mouseExited(event: MouseEvent) {
    }

    override fun mouseDragged(event: MouseEvent) {
    }

    override fun mouseMoved(event: MouseEvent) {
    }

    override fun mouseWheelMoved(event: MouseWheelEvent) {
    }
}
