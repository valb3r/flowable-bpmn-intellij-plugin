package com.valb3r.bpmn.intellij.activiti.plugin

import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.ContentFactory
import com.valb3r.bpmn.intellij.activiti.plugin.popupmenu.ActivitiCanvasPopupMenuProvider
import com.valb3r.bpmn.intellij.activiti.plugin.xmlnav.ActivitiXmlNavigator
import com.valb3r.bpmn.intellij.plugin.BpmnPluginToolWindow
import com.valb3r.bpmn.intellij.plugin.activiti.parser.ActivitiObjectFactory
import com.valb3r.bpmn.intellij.plugin.activiti.parser.ActivitiParser
import com.valb3r.bpmn.intellij.plugin.commons.langinjection.registerCurrentFile
import com.valb3r.bpmn.intellij.plugin.core.newelements.registerNewElementsFactory
import com.valb3r.bpmn.intellij.plugin.core.ui.components.popupmenu.registerPopupMenuProvider
import com.valb3r.bpmn.intellij.plugin.core.xmlnav.registerXmlNavigator

class ActivitiBpmnPluginToolWindowFactory: ToolWindowFactory {

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        registerPopupMenuProvider(ActivitiCanvasPopupMenuProvider())
        registerNewElementsFactory(ActivitiObjectFactory())
        val bpmnWindow = BpmnPluginToolWindow(ActivitiParser()) {
            registerCurrentFile(it)
            registerXmlNavigator(ActivitiXmlNavigator(project))
        }

        // register the call graph tool window as a project service, so it can be accessed by editor menu actions.
        val windowService = ServiceManager.getService(project, ActivitiBpmnPluginToolWindowProjectService::class.java)
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
