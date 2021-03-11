package com.valb3r.bpmn.intellij.plugin.core.ui.components.popupmenu

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.JBPopupMenu
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.BpmnElementId
import java.awt.geom.Point2D
import java.util.*


private val popupMenuProvider = Collections.synchronizedMap(WeakHashMap<Project,  CanvasPopupMenuProvider>())

fun popupMenuProvider(project: Project): CanvasPopupMenuProvider {
    return popupMenuProvider[project]!!
}

fun registerPopupMenuProvider(project: Project, provider: CanvasPopupMenuProvider) {
    popupMenuProvider[project] = provider
}

interface CanvasPopupMenuProvider {
    fun popupMenu(sceneLocation: Point2D.Float, parent: BpmnElementId): JBPopupMenu
}