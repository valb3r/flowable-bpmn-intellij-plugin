package com.valb3r.bpmn.intellij.plugin

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.wm.ToolWindowManager
import java.nio.file.Paths

class ViewBpmnDiagramAction: AnAction() {

    override fun actionPerformed(anActionEvent: AnActionEvent) {
        val project = anActionEvent.project ?: return

        ToolWindowManager.getInstance(project)
                .getToolWindow("BPMN-Flowable-Diagram")
                .activate {
                    ServiceManager.getService(project, BpmnPluginToolWindowProjectService::class.java)
                            .bpmnToolWindow
                            .run(Paths.get("/home/valb3r/IdeaProjects/flowable-intellij/flowable-xml-parser/src/main/resources/hbci-list-accounts.bpmn20.xml").toFile())
                }
    }

    override fun update(anActionEvent: AnActionEvent) {
        val project = anActionEvent.project
    }
}
