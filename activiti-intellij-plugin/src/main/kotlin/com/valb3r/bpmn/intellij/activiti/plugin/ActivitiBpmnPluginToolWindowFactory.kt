package com.valb3r.bpmn.intellij.activiti.plugin

import com.intellij.notification.NotificationType
import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.ContentFactory
import com.valb3r.bpmn.intellij.activiti.plugin.notifications.showNotificationBalloon
import com.valb3r.bpmn.intellij.activiti.plugin.popupmenu.ActivitiCanvasPopupMenuProvider
import com.valb3r.bpmn.intellij.activiti.plugin.xmlnav.ActivitiXmlNavigator
import com.valb3r.bpmn.intellij.plugin.activiti.parser.Activiti7ObjectFactory
import com.valb3r.bpmn.intellij.plugin.activiti.parser.Activiti7Parser
import com.valb3r.bpmn.intellij.plugin.activiti.parser.ActivitiObjectFactory
import com.valb3r.bpmn.intellij.plugin.activiti.parser.ActivitiParser
import com.valb3r.bpmn.intellij.plugin.commons.langinjection.registerCurrentFile
import com.valb3r.bpmn.intellij.plugin.core.BpmnPluginToolWindow
import com.valb3r.bpmn.intellij.plugin.core.newelements.registerNewElementsFactory
import com.valb3r.bpmn.intellij.plugin.core.parser.registerParser
import com.valb3r.bpmn.intellij.plugin.core.ui.components.popupmenu.registerPopupMenuProvider
import com.valb3r.bpmn.intellij.plugin.core.xmlnav.registerXmlNavigator
import java.nio.charset.StandardCharsets

class ActivitiBpmnPluginToolWindowFactory: ToolWindowFactory {

    private val log = Logger.getInstance(ActivitiBpmnPluginToolWindowFactory::class.java)

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        log.info("Creating tool window content")

        val bpmnWindow = BpmnPluginToolWindow(
            project,
            { showNotificationBalloon(project, it, NotificationType.ERROR) },
            {
                registerPopupMenuProvider(project, ActivitiCanvasPopupMenuProvider(project))
                if (isActiviti7(it.virtualFile)) {
                    registerParser(project, Activiti7Parser())
                    registerNewElementsFactory(project, Activiti7ObjectFactory())
                } else {
                    registerParser(project, ActivitiParser())
                    registerNewElementsFactory(project, ActivitiObjectFactory())
                }
            }
        ) {
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

    private fun isActiviti7(input: VirtualFile): Boolean {
        return String(input.contentsToByteArray(), StandardCharsets.UTF_8).contains("xmlns:bpmn2=\"http://www.omg.org/spec/BPMN/20100524/MODEL\"")
    }
}
