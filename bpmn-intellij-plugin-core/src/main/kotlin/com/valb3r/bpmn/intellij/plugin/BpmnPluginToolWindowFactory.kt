package com.valb3r.bpmn.intellij.plugin

import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.ContentFactory

class BpmnPluginToolWindowFactory: ToolWindowFactory {

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val bpmnWindow = BpmnPluginToolWindow()

        // register the call graph tool window as a project service, so it can be accessed by editor menu actions.
        val windowService = ServiceManager.getService(project, BpmnPluginToolWindowProjectService::class.java)
        windowService.bpmnToolWindow = bpmnWindow

        // register the tool window content
        val content = ContentFactory.SERVICE.getInstance().createContent(
                bpmnWindow.getContent(),
                "",
                false
        )
        toolWindow.contentManager.addContent(content)
    }
}
