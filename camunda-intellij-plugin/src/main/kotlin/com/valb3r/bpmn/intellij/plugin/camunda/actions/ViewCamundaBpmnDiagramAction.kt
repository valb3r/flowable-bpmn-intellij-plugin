package com.valb3r.bpmn.intellij.plugin.camunda.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.wm.ToolWindowManager
import com.valb3r.bpmn.intellij.plugin.core.BpmnActionContext
import com.valb3r.bpmn.intellij.plugin.camunda.CamundaBpmnPluginToolWindowProjectService

class ViewCamundaBpmnDiagramAction : AnAction() {

    override fun actionPerformed(anActionEvent: AnActionEvent) {
        val project = anActionEvent.project ?: return
        val file = psiElement(anActionEvent)?.containingFile ?: return
        if (!file.name.contains("bpmn")) {
            return
        }

        val toolWindow = ToolWindowManager.getInstance(project).getToolWindow("BPMN-Camunda-Diagram")!!
        toolWindow.title = file.name
        toolWindow.activate {
            ServiceManager.getService(project, CamundaBpmnPluginToolWindowProjectService::class.java)
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
                && (psiElement?.containingFile?.name?.contains("bpmn") ?: false)
    }

    private fun psiElement(anActionEvent: AnActionEvent) =
            anActionEvent.getData(CommonDataKeys.PSI_FILE)
}
