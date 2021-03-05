package com.valb3r.bpmn.intellij.plugin.core

import com.valb3r.bpmn.intellij.plugin.core.actions.copypaste.copyToClipboard
import com.valb3r.bpmn.intellij.plugin.core.actions.copypaste.cutToClipboard
import com.valb3r.bpmn.intellij.plugin.core.actions.copypaste.pasteFromClipboard
import com.valb3r.bpmn.intellij.plugin.core.actions.currentRemoveActionHandler
import com.valb3r.bpmn.intellij.plugin.core.events.ProcessModelUpdateEvents
import com.valb3r.bpmn.intellij.plugin.core.events.updateEventsRegistry
import com.valb3r.bpmn.intellij.plugin.core.render.Canvas
import com.valb3r.bpmn.intellij.plugin.core.render.currentCanvas
import com.valb3r.bpmn.intellij.plugin.core.render.uieventbus.ZoomInEvent
import com.valb3r.bpmn.intellij.plugin.core.render.uieventbus.ZoomOutEvent
import com.valb3r.bpmn.intellij.plugin.core.render.uieventbus.currentUiEventBus
import com.valb3r.bpmn.intellij.plugin.core.ui.components.popupmenu.popupMenuProvider
import java.awt.event.KeyEvent
import java.awt.event.KeyListener
import java.awt.geom.Point2D


private const val ARROW_BUTTON_STEP = 5.0f
private const val ARROW_BUTTON_BIG_STEP = 50.0f

class KeyboardEventHandler(private val canvas: Canvas): KeyListener {

    override fun keyTyped(e: KeyEvent) {
        // NOP
    }

    override fun keyPressed(e: KeyEvent?) {
        // NOP
    }

    override fun keyReleased(e: KeyEvent) {
        when {
            e.isControlDown -> handleKeyWithControl(e)
            e.isShiftDown -> handleKeyWithShift(e)
            else -> handleKeyboardKeys(e, ARROW_BUTTON_STEP)
        }
    }

    private fun handleKeyWithShift(e: KeyEvent) {
        when (e.keyChar) {
            'N' -> currentCanvas().let { canvas ->
                currentMouseEventHandler().lastPosition()?.let { pos ->
                    popupMenuProvider().popupMenu(canvas.fromCameraView(pos), canvas.parentableElementAt(pos)).show(e.component, pos.x.toInt(), pos.y.toInt())
                }
            }
        }
    }

    private fun handleKeyWithControl(e: KeyEvent) {
        when (e.keyCode) {
            KeyEvent.VK_Y -> if (updateEventsRegistry().undoRedoStatus().contains(ProcessModelUpdateEvents.UndoRedo.REDO)) {
                updateEventsRegistry().redo()
                currentCanvas().repaint()
            }
            KeyEvent.VK_Z -> if (updateEventsRegistry().undoRedoStatus().contains(ProcessModelUpdateEvents.UndoRedo.UNDO)) {
                updateEventsRegistry().undo()
                currentCanvas().repaint()
            }
            KeyEvent.VK_C -> copyToClipboard()
            KeyEvent.VK_X -> cutToClipboard()
            KeyEvent.VK_V -> currentCanvas().let { canvas ->
                currentMouseEventHandler().lastPosition()?.let { pos ->
                    pasteFromClipboard(canvas.fromCameraView(pos), canvas.parentableElementAt(pos))
                }
            }
        }
        when (e.keyChar) {
            '+' -> currentUiEventBus().publish(ZoomInEvent())
            '-' -> currentUiEventBus().publish(ZoomOutEvent())
            else -> handleKeyboardKeys(e, ARROW_BUTTON_BIG_STEP)
        }
    }

    private fun handleKeyboardKeys(e: KeyEvent, step: Float) {
        val start = Point2D.Float(0.0f, 0.0f)
        when (e.keyCode) {
            KeyEvent.VK_UP -> canvas.dragCanvas(start, Point2D.Float(0.0f, step))
            KeyEvent.VK_DOWN -> canvas.dragCanvas(start, Point2D.Float(0.0f, -step))
            KeyEvent.VK_LEFT -> canvas.dragCanvas(start, Point2D.Float(step, 0.0f))
            KeyEvent.VK_RIGHT -> canvas.dragCanvas(start, Point2D.Float(-step, 0.0f))
            KeyEvent.VK_DELETE -> currentRemoveActionHandler().deleteElem()
        }
    }
}
