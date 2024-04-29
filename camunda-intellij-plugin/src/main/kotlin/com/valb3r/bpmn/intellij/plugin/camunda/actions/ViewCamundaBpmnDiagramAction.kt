package com.valb3r.bpmn.intellij.plugin.camunda.actions

import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import com.valb3r.bpmn.intellij.plugin.camunda.CamundaBpmnPluginToolWindowProjectService
import com.valb3r.bpmn.intellij.plugin.camunda.notifications.showNotificationBalloon
import com.valb3r.bpmn.intellij.plugin.camunda.settings.CamundaBpmnPluginSettingsState
import com.valb3r.bpmn.intellij.plugin.core.BpmnActionContext
import com.valb3r.bpmn.intellij.plugin.core.actions.BaseViewBpmnDiagramAction
import com.valb3r.bpmn.intellij.plugin.core.settings.currentSettingsStateProvider

class ViewCamundaBpmnDiagramAction : BaseViewBpmnDiagramAction() {

    override val toolWindowName: String
        get() = "BPMN-Camunda-Diagram"

    override fun generateContent(project: Project, file: PsiFile) {
        val window = ServiceManager.getService(project, CamundaBpmnPluginToolWindowProjectService::class.java).bpmnToolWindow
        window.hackFixForMacOsScrollbars()
        window.openFileAndRender(file, BpmnActionContext(project))
    }

    override fun notificationBalloon(project: Project, message: String, type: NotificationType) {
        showNotificationBalloon(project, message, type)
    }

    override fun update(anActionEvent: AnActionEvent) {
        // FIXME - Is rather a hack, as PreloadingActivity is invoked only on plugin start and is mostly for caching, correct alternative to initialize plugin is 'DynamicPluginListener' but it appears on 2020+ IDEs
        currentSettingsStateProvider.compareAndSet(null) { ServiceManager.getService(CamundaBpmnPluginSettingsState::class.java) }
        val project = anActionEvent.project
        val psiElement = psiElement(anActionEvent)
        anActionEvent.presentation.isEnabledAndVisible = project != null && isValidFileName(psiElement?.containingFile?.name)
    }

    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.BGT
    }
}
