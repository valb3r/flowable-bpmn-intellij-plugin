package com.valb3r.bpmn.intellij.plugin.camunda

import com.intellij.notification.NotificationType
import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.ContentFactory
import com.valb3r.bpmn.intellij.activiti.plugin.notifications.showNotificationBalloon
import com.valb3r.bpmn.intellij.plugin.commons.langinjection.registerCurrentFile
import com.valb3r.bpmn.intellij.plugin.core.BpmnPluginToolWindow
import com.valb3r.bpmn.intellij.plugin.core.newelements.registerNewElementsFactory
import com.valb3r.bpmn.intellij.plugin.core.parser.registerParser
import com.valb3r.bpmn.intellij.plugin.core.settings.currentSettingsStateProvider
import com.valb3r.bpmn.intellij.plugin.core.ui.components.popupmenu.registerPopupMenuProvider
import com.valb3r.bpmn.intellij.plugin.core.xmlnav.registerXmlNavigator
import com.valb3r.bpmn.intellij.plugin.camunda.parser.CamundaObjectFactory
import com.valb3r.bpmn.intellij.plugin.camunda.parser.CamundaParser
import com.valb3r.bpmn.intellij.plugin.camunda.settings.CamundaBpmnPluginSettingsState
import com.valb3r.bpmn.intellij.plugin.camunda.ui.components.popupmenu.CamundaCanvasPopupMenuProvider
import com.valb3r.bpmn.intellij.plugin.camunda.xmlnav.CamundaXmlNavigator

class CamundaBpmnPluginToolWindowFactory: ToolWindowFactory {

    private val log = Logger.getInstance(CamundaBpmnPluginToolWindowFactory::class.java)

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        log.info("Creating tool window content")
        currentSettingsStateProvider.set { ServiceManager.getService(CamundaBpmnPluginSettingsState::class.java) }

        val bpmnWindow = BpmnPluginToolWindow(
            project,
            { showNotificationBalloon(project, it, NotificationType.ERROR) },
            {
                registerPopupMenuProvider(project, CamundaCanvasPopupMenuProvider(project))
                registerParser(project, CamundaParser())
                registerNewElementsFactory(project, CamundaObjectFactory())
            }
        ) {
            registerCurrentFile(project, it)
            registerXmlNavigator(project, CamundaXmlNavigator(project))
        }

        // register the call graph tool window as a project service, so it can be accessed by editor menu actions.
        val windowService = ServiceManager.getService(project, CamundaBpmnPluginToolWindowProjectService::class.java)
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
