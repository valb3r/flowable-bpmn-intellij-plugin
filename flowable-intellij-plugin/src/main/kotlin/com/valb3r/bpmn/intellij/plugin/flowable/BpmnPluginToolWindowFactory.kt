package com.valb3r.bpmn.intellij.plugin.flowable

import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.ContentFactory
import com.valb3r.bpmn.intellij.plugin.BpmnPluginToolWindow
import com.valb3r.bpmn.intellij.plugin.core.BpmnPluginToolWindowProjectService
import com.valb3r.bpmn.intellij.plugin.core.xmlnav.DefaultXmlNavigator
import com.valb3r.bpmn.intellij.plugin.core.xmlnav.newXmlNavigator
import com.valb3r.bpmn.intellij.plugin.flowable.langinjection.registerCurrentFile
import com.valb3r.bpmn.intellij.plugin.flowable.parser.FlowableParser

class BpmnPluginToolWindowFactory: ToolWindowFactory {

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val bpmnWindow = BpmnPluginToolWindow(FlowableParser()) {
            registerCurrentFile(it)
            newXmlNavigator(DefaultXmlNavigator(project))
        }

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
