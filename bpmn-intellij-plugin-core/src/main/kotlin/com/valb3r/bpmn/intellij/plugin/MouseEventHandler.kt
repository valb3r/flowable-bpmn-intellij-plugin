package com.valb3r.bpmn.intellij.plugin

import com.valb3r.bpmn.intellij.plugin.render.Canvas
import com.valb3r.bpmn.intellij.plugin.ui.components.popupmenu.popupMenuProvider
import java.awt.event.*
import java.awt.geom.Point2D
import javax.swing.SwingUtilities

class MouseEventHandler(private val canvas: Canvas): MouseListener, MouseMotionListener, MouseWheelListener {

    private val popupMenuProvider = popupMenuProvider()
    private var prevMousePosition: Point2D.Float? = null

    override fun mouseClicked(event: MouseEvent) {
        val point2D = Point2D.Float(event.x.toFloat(), event.y.toFloat())

        if (SwingUtilities.isRightMouseButton(event)) {
            popupMenuProvider.popupMenu(
                    canvas.fromCameraView(point2D),
                    canvas.parentableElementAt(point2D)
            ).show(event.component, event.x, event.y)
            return
        }

        this.canvas.click(point2D)
    }

    override fun mousePressed(event: MouseEvent) {
        val point2D = Point2D.Float(event.x.toFloat(), event.y.toFloat())
        if (SwingUtilities.isLeftMouseButton(event) && event.isShiftDown) {
            this.canvas.startCanvasDragWithButton(point2D)
            return
        }
        if (SwingUtilities.isLeftMouseButton(event)) {
            this.canvas.startSelectionOrSelectedDrag(point2D)
            return
        }
    }

    override fun mouseReleased(event: MouseEvent) {
        this.canvas.stopDragOrSelect()
        prevMousePosition = null
    }

    override fun mouseEntered(e: MouseEvent?) {
        // NOP
    }

    override fun mouseExited(e: MouseEvent?) {
        // NOP
    }

    override fun mouseDragged(event: MouseEvent) {
        val currentMousePosition = Point2D.Float(event.x.toFloat(), event.y.toFloat())
        val prevMousePos = prevMousePosition ?: currentMousePosition

        if (SwingUtilities.isMiddleMouseButton(event)) {
            this.canvas.dragWithWheel(prevMousePos, currentMousePosition)
        } else if (SwingUtilities.isLeftMouseButton(event)) {
            this.canvas.dragOrSelectWithLeftButton(prevMousePos, currentMousePosition)
        }

        prevMousePosition = currentMousePosition
    }

    override fun mouseMoved(event: MouseEvent) {
        // NOP
    }

    override fun mouseWheelMoved(event: MouseWheelEvent) {
        val currentMousePosition = Point2D.Float(event.x.toFloat(), event.y.toFloat())
        this.canvas.zoom(currentMousePosition, event.wheelRotation)
    }
}
