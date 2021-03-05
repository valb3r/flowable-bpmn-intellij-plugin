package com.valb3r.bpmn.intellij.plugin.core

import com.valb3r.bpmn.intellij.plugin.core.actions.currentRemoveActionHandler
import com.valb3r.bpmn.intellij.plugin.core.render.Canvas
import com.valb3r.bpmn.intellij.plugin.core.render.uieventbus.ZoomInEvent
import com.valb3r.bpmn.intellij.plugin.core.render.uieventbus.ZoomOutEvent
import com.valb3r.bpmn.intellij.plugin.core.render.uieventbus.currentUiEventBus
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
        if (e.isControlDown) {
            when (e.keyChar) {
                '+' -> currentUiEventBus().publish(ZoomInEvent())
                '-' -> currentUiEventBus().publish(ZoomOutEvent())
                else -> handleKeyboardKeys(e, ARROW_BUTTON_BIG_STEP)
            }
        } else {
            handleKeyboardKeys(e, ARROW_BUTTON_STEP)
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
