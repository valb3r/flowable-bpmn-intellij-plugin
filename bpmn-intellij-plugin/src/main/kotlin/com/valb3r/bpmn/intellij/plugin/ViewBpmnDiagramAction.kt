package com.valb3r.bpmn.intellij.plugin

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.wm.ToolWindowManager

class ViewBpmnDiagramAction: AnAction() {

    override fun actionPerformed(anActionEvent: AnActionEvent) {
        val project = anActionEvent.project ?: return
        val file = psiElement(anActionEvent)?.containingFile ?: return
        if (!file.name.contains("bpmn20.xml")) {
            return
        }

        ToolWindowManager.getInstance(project)
                .getToolWindow("BPMN-Flowable-Diagram")
                .activate {
                    ServiceManager.getService(project, BpmnPluginToolWindowProjectService::class.java)
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
            anActionEvent.getData(CommonDataKeys.PSI_ELEMENT)
}
