package com.valb3r.bpmn.intellij.activiti.plugin

import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.ContentFactory
import com.valb3r.bpmn.intellij.activiti.plugin.popupmenu.ActivitiCanvasPopupMenuProvider
import com.valb3r.bpmn.intellij.activiti.plugin.xmlnav.ActivitiXmlNavigator
import com.valb3r.bpmn.intellij.plugin.activiti.parser.ActivitiObjectFactory
import com.valb3r.bpmn.intellij.plugin.activiti.parser.ActivitiParser
import com.valb3r.bpmn.intellij.plugin.commons.langinjection.registerCurrentFile
import com.valb3r.bpmn.intellij.plugin.core.BpmnPluginToolWindow
import com.valb3r.bpmn.intellij.plugin.core.newelements.registerNewElementsFactory
import com.valb3r.bpmn.intellij.plugin.core.ui.components.popupmenu.registerPopupMenuProvider
import com.valb3r.bpmn.intellij.plugin.core.xmlnav.registerXmlNavigator

class ActivitiBpmnPluginToolWindowFactory: ToolWindowFactory {

    private val log = Logger.getInstance(ActivitiBpmnPluginToolWindowFactory::class.java)

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        log.info("Creating tool window content")
        registerPopupMenuProvider(project, ActivitiCanvasPopupMenuProvider(project))
        registerNewElementsFactory(project, ActivitiObjectFactory())
        val bpmnWindow = BpmnPluginToolWindow(project, ActivitiParser()) {
            registerCurrentFile(project, it)
            registerXmlNavigator(project, ActivitiXmlNavigator(project))
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
        log.info("Tool window content created")
    }
}
