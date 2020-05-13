package com.valb3r.bpmn.intellij.plugin.ui.components.popupmenu

import com.intellij.openapi.ui.JBMenuItem
import com.intellij.openapi.ui.JBPopupMenu
import com.intellij.openapi.util.IconLoader
import java.awt.geom.Point2D
import java.util.concurrent.atomic.AtomicReference
import javax.swing.JMenu


private val popupMenuProvider = AtomicReference<CanvasPopupMenuProvider>()

fun popupMenuProvider(): CanvasPopupMenuProvider {
    return popupMenuProvider.updateAndGet {
        if (null == it) {
            return@updateAndGet CanvasPopupMenuProvider()
        }

        return@updateAndGet it
    }
}

class CanvasPopupMenuProvider {

    private val START_EVENT = IconLoader.getIcon("/icons/popupmenu/start-event.png")
    private val SERVICE_TASK = IconLoader.getIcon("/icons/popupmenu/service-task.png")
    private val CALL_ACTIVITY = IconLoader.getIcon("/icons/popupmenu/call-activity.png")
    private val EXCLUSIVE_GATEWAY = IconLoader.getIcon("/icons/popupmenu/exclusive-gateway.png")
    private val END_EVENT = IconLoader.getIcon("/icons/popupmenu/end-event.png")

    fun popupMenu(screenLocation: Point2D.Float): JBPopupMenu {
        val popup = JBPopupMenu()
        popup.add(startEvents())
        popup.add(activities())
        popup.add(structural())
        popup.add(gateways())
        popup.add(endEvents())
        return popup
    }

    private fun startEvents(): JMenu {
        val menu = JMenu("Start events")
        menu.add(JBMenuItem("Start event", START_EVENT))
        return menu
    }

    private fun activities(): JMenu {
        val menu = JMenu("Activities")
        menu.add(JBMenuItem("Service task", SERVICE_TASK))
        return menu
    }

    private fun structural(): JMenu {
        val menu = JMenu("Structural")
        menu.add(JBMenuItem("Call activity", CALL_ACTIVITY))
        return menu
    }

    private fun gateways(): JMenu {
        val menu = JMenu("Gateways")
        menu.add(JBMenuItem("Exclusive gateway", EXCLUSIVE_GATEWAY))
        return menu
    }

    private fun endEvents(): JMenu {
        val menu = JMenu("End events")
        menu.add(JBMenuItem("End event", END_EVENT))
        return menu
    }
}