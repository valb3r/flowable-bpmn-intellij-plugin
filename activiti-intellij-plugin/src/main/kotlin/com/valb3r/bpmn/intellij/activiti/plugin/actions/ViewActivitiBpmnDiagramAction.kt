package com.valb3r.bpmn.intellij.activiti.plugin.actions

import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.wm.ToolWindowManager
import com.valb3r.bpmn.intellij.activiti.plugin.ActivitiBpmnPluginToolWindowProjectService
import com.valb3r.bpmn.intellij.activiti.plugin.notifications.showNotificationBalloon
import com.valb3r.bpmn.intellij.plugin.core.BpmnActionContext
import com.valb3r.bpmn.intellij.plugin.core.settings.currentSettings

class ViewActivitiBpmnDiagramAction : AnAction() {

    override fun actionPerformed(anActionEvent: AnActionEvent) {
        val project = anActionEvent.project ?: return
        val file = psiElement(anActionEvent)?.containingFile ?: return
        if (!isValidFileName(file.name)) {
            showNotificationBalloon(project, "Invalid file name: ${file.name}", NotificationType.ERROR)
            return
        }

        val toolWindow = ToolWindowManager.getInstance(project).getToolWindow("BPMN-Activiti-Diagram")!!
        toolWindow.title = file.name
        toolWindow.activate {
            val window = ServiceManager.getService(project, ActivitiBpmnPluginToolWindowProjectService::class.java).bpmnToolWindow
            window.hackFixForMacOsScrollbars()
            window.run(file, BpmnActionContext(project))
        }
    }

    override fun update(anActionEvent: AnActionEvent) {
        val project = anActionEvent.project
        val psiElement = psiElement(anActionEvent)
        anActionEvent.presentation.isEnabledAndVisible = project != null && isValidFileName(psiElement?.containingFile?.name)
    }

    private fun psiElement(anActionEvent: AnActionEvent) = anActionEvent.getData(CommonDataKeys.PSI_FILE)

    private fun isValidFileName(fileName: String?): Boolean {
        val name = fileName ?: return false
        val allowedExt = currentSettings().openExtensions
        return allowedExt.any { name.endsWith(it) }
    }
}
