package com.valb3r.bpmn.intellij.activiti.plugin.actions

import com.intellij.notification.NotificationType
import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import com.valb3r.bpmn.intellij.activiti.plugin.ActivitiBpmnPluginToolWindowProjectService
import com.valb3r.bpmn.intellij.activiti.plugin.notifications.showNotificationBalloon
import com.valb3r.bpmn.intellij.plugin.core.BpmnActionContext
import com.valb3r.bpmn.intellij.plugin.core.actions.BaseViewBpmnDiagramAction

class ViewActivitiBpmnDiagramAction : BaseViewBpmnDiagramAction() {

    override val toolWindowName: String
        get() = "BPMN-Activiti-Diagram"

    override fun generateContent(project: Project, file: PsiFile) {
        val window = ServiceManager.getService(project, ActivitiBpmnPluginToolWindowProjectService::class.java).bpmnToolWindow
        window.hackFixForMacOsScrollbars()
        window.openFileAndRender(file, BpmnActionContext(project))
    }

    override fun notificationBalloon(project: Project, message: String, type: NotificationType) {
        showNotificationBalloon(project, message, type)
    }
}
