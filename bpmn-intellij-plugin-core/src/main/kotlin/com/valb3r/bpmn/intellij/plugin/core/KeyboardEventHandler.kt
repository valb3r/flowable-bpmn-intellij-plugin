package com.valb3r.bpmn.intellij.plugin.core

import com.intellij.openapi.project.Project
import com.intellij.openapi.util.SystemInfo
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
import com.valb3r.bpmn.intellij.plugin.core.settings.currentSettings
import com.valb3r.bpmn.intellij.plugin.core.ui.components.popupmenu.popupMenuProvider
import java.awt.event.KeyEvent
import java.awt.event.KeyListener
import java.awt.geom.Point2D

class KeyboardEventHandler(private val project: Project, private val canvas: Canvas): KeyListener {
    val isMac = SystemInfo.isMac

    override fun keyTyped(e: KeyEvent) {
        // NOP
    }

    override fun keyPressed(e: KeyEvent?) {
        // NOP
    }

    override fun keyReleased(e: KeyEvent) {
        when {
            !isMac && e.isControlDown -> handleKeyWithControl(e)
            isMac && e.isMetaDown -> handleKeyWithControl(e)
            e.isShiftDown -> handleKeyWithShift(e)
            else -> handleKeyboardKeys(e, currentSettings().keyboardSmallStep)
        }
    }

    private fun handleKeyWithShift(e: KeyEvent) {
        when (e.keyChar) {
            'N' -> currentCanvas(project).let { canvas ->
                currentMouseEventHandler(project).lastPosition()?.let { pos ->
                    popupMenuProvider(project).popupMenu(canvas.fromCameraView(pos), canvas.parentableElementAt(pos)).show(e.component, pos.x.toInt(), pos.y.toInt())
                }
            }
        }
    }

    private fun handleKeyWithControl(e: KeyEvent) {
        when (e.keyCode) {
            KeyEvent.VK_A -> {
                currentCanvas(project).selectAllElements()
            }
            KeyEvent.VK_Y -> if (updateEventsRegistry(project).undoRedoStatus().contains(ProcessModelUpdateEvents.UndoRedo.REDO)) {
                updateEventsRegistry(project).redo()
                currentCanvas(project).repaint()
            }
            KeyEvent.VK_Z -> if (updateEventsRegistry(project).undoRedoStatus().contains(ProcessModelUpdateEvents.UndoRedo.UNDO)) {
                updateEventsRegistry(project).undo()
                currentCanvas(project).repaint()
            }
            KeyEvent.VK_C -> copyToClipboard(project)
            KeyEvent.VK_X -> cutToClipboard(project)
            KeyEvent.VK_V -> currentCanvas(project).let { canvas ->
                currentMouseEventHandler(project).lastPosition()?.let { pos ->
                    pasteFromClipboard(project, canvas.fromCameraView(pos), canvas.parentableElementAt(pos))
                }
            }
        }
        when (e.keyChar) {
            '+' -> currentUiEventBus(project).publish(ZoomInEvent())
            '-' -> currentUiEventBus(project).publish(ZoomOutEvent())
            else -> handleKeyboardKeys(e, currentSettings().keyboardLargeStep)
        }
    }

    private fun handleKeyboardKeys(e: KeyEvent, step: Float) {
        val start = Point2D.Float(0.0f, 0.0f)
        when (e.keyCode) {
            KeyEvent.VK_UP -> canvas.dragCanvas(start, Point2D.Float(0.0f, step))
            KeyEvent.VK_DOWN -> canvas.dragCanvas(start, Point2D.Float(0.0f, -step))
            KeyEvent.VK_LEFT -> canvas.dragCanvas(start, Point2D.Float(step, 0.0f))
            KeyEvent.VK_RIGHT -> canvas.dragCanvas(start, Point2D.Float(-step, 0.0f))
            KeyEvent.VK_DELETE -> currentRemoveActionHandler(project).deleteElem()
        }
    }
}
