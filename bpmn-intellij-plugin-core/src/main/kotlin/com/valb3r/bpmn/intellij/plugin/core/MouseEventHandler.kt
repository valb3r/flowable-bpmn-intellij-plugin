package com.valb3r.bpmn.intellij.plugin.core

import com.intellij.openapi.project.Project
import com.valb3r.bpmn.intellij.plugin.core.render.Canvas
import com.valb3r.bpmn.intellij.plugin.core.ui.components.popupmenu.popupMenuProvider
import java.awt.event.*
import java.awt.geom.Point2D
import java.util.*
import javax.swing.SwingUtilities

private val mouseEventHandler = Collections.synchronizedMap(WeakHashMap<Project,  MouseEventHandler>())

fun currentMouseEventHandler(project: Project): MouseEventHandler {
    return mouseEventHandler[project]!!
}

fun setCurrentMouseEventHandler(project: Project, canvas: Canvas): MouseEventHandler {
    val handler = MouseEventHandler(project, canvas)
    mouseEventHandler[project] = handler
    return handler
}

class MouseEventHandler(project: Project, private val canvas: Canvas): MouseListener, MouseMotionListener, MouseWheelListener {

    private val popupMenuProvider = popupMenuProvider(project)
    private var prevActionableMousePosition: Point2D.Float? = null
    private var currentMousePosition: Point2D.Float? = null

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
        this.canvas.requestFocus()
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
        prevActionableMousePosition = null
    }

    override fun mouseEntered(e: MouseEvent?) {
        // NOP
    }

    override fun mouseExited(e: MouseEvent?) {
        // NOP
    }

    override fun mouseDragged(event: MouseEvent) {
        val mousePos = Point2D.Float(event.x.toFloat(), event.y.toFloat())
        val prevMousePos = prevActionableMousePosition ?: mousePos

        if (SwingUtilities.isMiddleMouseButton(event)) {
            this.canvas.dragWithWheel(prevMousePos, mousePos)
        } else if (SwingUtilities.isLeftMouseButton(event)) {
            this.canvas.dragOrSelectWithLeftButton(prevMousePos, mousePos)
        }

        prevActionableMousePosition = mousePos
    }

    override fun mouseMoved(event: MouseEvent) {
        currentMousePosition = Point2D.Float(event.x.toFloat(), event.y.toFloat())
    }

    override fun mouseWheelMoved(event: MouseWheelEvent) {
        val currentMousePosition = Point2D.Float(event.x.toFloat(), event.y.toFloat())
        this.canvas.zoom(currentMousePosition, event.wheelRotation)
    }

    fun lastPosition(): Point2D.Float? {
        return currentMousePosition
    }
}
