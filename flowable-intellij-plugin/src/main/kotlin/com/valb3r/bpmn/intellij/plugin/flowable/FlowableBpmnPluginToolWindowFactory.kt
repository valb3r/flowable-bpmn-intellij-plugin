package com.valb3r.bpmn.intellij.plugin.flowable

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
import com.valb3r.bpmn.intellij.plugin.flowable.parser.FlowableObjectFactory
import com.valb3r.bpmn.intellij.plugin.flowable.parser.FlowableParser
import com.valb3r.bpmn.intellij.plugin.flowable.settings.FlowableBpmnPluginSettingsState
import com.valb3r.bpmn.intellij.plugin.flowable.ui.components.popupmenu.FlowableCanvasPopupMenuProvider
import com.valb3r.bpmn.intellij.plugin.flowable.xmlnav.FlowableXmlNavigator

class FlowableBpmnPluginToolWindowFactory: ToolWindowFactory {

    private val log = Logger.getInstance(FlowableBpmnPluginToolWindowFactory::class.java)

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        log.info("Creating tool window content")
        currentSettingsStateProvider.set { ServiceManager.getService(FlowableBpmnPluginSettingsState::class.java) }

        val bpmnWindow = BpmnPluginToolWindow(
            project,
            { showNotificationBalloon(project, it, NotificationType.ERROR) },
            {
                registerPopupMenuProvider(project, FlowableCanvasPopupMenuProvider(project))
                registerParser(project, FlowableParser())
                registerNewElementsFactory(project, FlowableObjectFactory())
            }
        ) {
            registerCurrentFile(project, it)
            registerXmlNavigator(project, FlowableXmlNavigator(project))
        }

        // register the call graph tool window as a project service, so it can be accessed by editor menu actions.
        val windowService = ServiceManager.getService(project, FlowableBpmnPluginToolWindowProjectService::class.java)
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
