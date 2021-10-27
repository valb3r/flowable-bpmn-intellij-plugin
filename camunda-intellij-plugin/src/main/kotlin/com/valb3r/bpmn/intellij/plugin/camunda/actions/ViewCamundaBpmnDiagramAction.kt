package com.valb3r.bpmn.intellij.plugin.camunda.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.wm.ToolWindowManager
import com.valb3r.bpmn.intellij.plugin.core.BpmnActionContext
import com.valb3r.bpmn.intellij.plugin.camunda.CamundaBpmnPluginToolWindowProjectService
import com.valb3r.bpmn.intellij.plugin.camunda.settings.CamundaBpmnPluginSettingsState
import com.valb3r.bpmn.intellij.plugin.core.settings.currentSettings
import com.valb3r.bpmn.intellij.plugin.core.settings.currentSettingsStateProvider

class ViewCamundaBpmnDiagramAction : AnAction() {

    override fun actionPerformed(anActionEvent: AnActionEvent) {
        val project = anActionEvent.project ?: return
        val file = psiElement(anActionEvent)?.containingFile ?: return
        if (!isValidFileName(file.name)) {
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
        // FIXME - Is rather a hack, as PreloadingActivity is invoked only on plugin start and is mostly for caching, correct alternative to initialize plugin is 'DynamicPluginListener' but it appears on 2020+ IDEs
        currentSettingsStateProvider.compareAndSet(null) { ServiceManager.getService(CamundaBpmnPluginSettingsState::class.java) }
        val project = anActionEvent.project
        val psiElement = psiElement(anActionEvent)
        anActionEvent.presentation.isEnabledAndVisible = project != null && isValidFileName(psiElement?.containingFile?.name)
    }

    private fun psiElement(anActionEvent: AnActionEvent) =
            anActionEvent.getData(CommonDataKeys.PSI_FILE)

    private fun isValidFileName(fileName: String?): Boolean {
        val name = fileName ?: return false
        val allowedExt = currentSettings().openExtensions
        return allowedExt.any { name.endsWith(it) }
    }
}
