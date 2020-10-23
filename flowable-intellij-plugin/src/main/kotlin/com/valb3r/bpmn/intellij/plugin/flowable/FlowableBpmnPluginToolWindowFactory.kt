package com.valb3r.bpmn.intellij.plugin.flowable

import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.ContentFactory
import com.valb3r.bpmn.intellij.plugin.BpmnPluginToolWindow
import com.valb3r.bpmn.intellij.plugin.commons.langinjection.registerCurrentFile
import com.valb3r.bpmn.intellij.plugin.core.newelements.registerNewElementsFactory
import com.valb3r.bpmn.intellij.plugin.core.ui.components.popupmenu.registerPopupMenuProvider
import com.valb3r.bpmn.intellij.plugin.core.xmlnav.registerXmlNavigator
import com.valb3r.bpmn.intellij.plugin.flowable.parser.FlowableObjectFactory
import com.valb3r.bpmn.intellij.plugin.flowable.parser.FlowableParser
import com.valb3r.bpmn.intellij.plugin.flowable.ui.components.popupmenu.FlowableCanvasPopupMenuProvider
import com.valb3r.bpmn.intellij.plugin.flowable.xmlnav.FlowableXmlNavigator

class FlowableBpmnPluginToolWindowFactory: ToolWindowFactory {

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        registerPopupMenuProvider(FlowableCanvasPopupMenuProvider())
        registerNewElementsFactory(FlowableObjectFactory())
        val bpmnWindow = BpmnPluginToolWindow(FlowableParser()) {
            registerCurrentFile(it)
            registerXmlNavigator(FlowableXmlNavigator(project))
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
    }
}
