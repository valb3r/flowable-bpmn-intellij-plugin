package com.valb3r.bpmn.intellij.plugin.core.ui.components.popupmenu

import com.intellij.openapi.ui.JBPopupMenu
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.BpmnElementId
import java.awt.geom.Point2D
import java.util.concurrent.atomic.AtomicReference


private val popupMenuProvider = AtomicReference<CanvasPopupMenuProvider>()

fun popupMenuProvider(): CanvasPopupMenuProvider {
    return popupMenuProvider.get()
}

fun registerPopupMenuProvider(provider: CanvasPopupMenuProvider) {
    popupMenuProvider.set(provider)
}

interface CanvasPopupMenuProvider {
    fun popupMenu(sceneLocation: Point2D.Float, parent: BpmnElementId): JBPopupMenu
}