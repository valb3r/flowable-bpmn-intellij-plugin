package com.valb3r.bpmn.intellij.activiti.plugin.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.wm.ToolWindowManager
import com.valb3r.bpmn.intellij.activiti.plugin.ActivitiBpmnPluginToolWindowProjectService
import com.valb3r.bpmn.intellij.plugin.core.BpmnActionContext

class ViewActivitiBpmnDiagramAction : AnAction() {

    override fun actionPerformed(anActionEvent: AnActionEvent) {
        val project = anActionEvent.project ?: return
        val file = psiElement(anActionEvent)?.containingFile ?: return
        if (!file.name.contains("bpmn20.xml")) {
            return
        }

        val toolWindow = ToolWindowManager.getInstance(project).getToolWindow("BPMN-Activiti-Diagram")!!
        toolWindow.title = file.name
        toolWindow.activate {
            ServiceManager.getService(project, ActivitiBpmnPluginToolWindowProjectService::class.java)
                    .bpmnToolWindow
                    .run(
                            file,
                            BpmnActionContext(project)
                    )
        }
    }

    override fun update(anActionEvent: AnActionEvent) {
        val project = anActionEvent.project
        val psiElement = psiElement(anActionEvent)
        anActionEvent.presentation.isEnabledAndVisible = project != null
                && (psiElement?.containingFile?.name?.contains("bpmn20.xml") ?: false)
    }

    private fun psiElement(anActionEvent: AnActionEvent) =
            anActionEvent.getData(CommonDataKeys.PSI_FILE)
}
