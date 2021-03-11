package com.valb3r.bpmn.intellij.plugin.core.ui.components.popupmenu

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.JBPopupMenu
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.BpmnElementId
import com.valb3r.bpmn.intellij.plugin.core.id
import java.awt.geom.Point2D
import java.util.concurrent.ConcurrentHashMap


private val popupMenuProvider = ConcurrentHashMap<String, CanvasPopupMenuProvider>()

fun popupMenuProvider(project: Project): CanvasPopupMenuProvider {
    return popupMenuProvider[project.id()]!!
}

fun registerPopupMenuProvider(project: Project, provider: CanvasPopupMenuProvider) {
    popupMenuProvider[project.id()] = provider
}

interface CanvasPopupMenuProvider {
    fun popupMenu(sceneLocation: Point2D.Float, parent: BpmnElementId): JBPopupMenu
}