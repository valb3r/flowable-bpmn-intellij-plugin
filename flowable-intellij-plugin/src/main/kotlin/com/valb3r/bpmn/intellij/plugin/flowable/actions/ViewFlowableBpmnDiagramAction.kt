package com.valb3r.bpmn.intellij.plugin.flowable.actions

import com.intellij.notification.NotificationType
import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import com.valb3r.bpmn.intellij.activiti.plugin.notifications.showNotificationBalloon
import com.valb3r.bpmn.intellij.plugin.core.BpmnActionContext
import com.valb3r.bpmn.intellij.plugin.core.actions.BaseViewBpmnDiagramAction
import com.valb3r.bpmn.intellij.plugin.flowable.FlowableBpmnPluginToolWindowProjectService

class ViewFlowableBpmnDiagramAction : BaseViewBpmnDiagramAction() {

    override val toolWindowName: String
        get() = "BPMN-Flowable-Diagram"

    override fun generateContent(project: Project, file: PsiFile) {
        val window = ServiceManager.getService(project, FlowableBpmnPluginToolWindowProjectService::class.java).bpmnToolWindow
        window.hackFixForMacOsScrollbars()
        window.openFileAndRender(file, BpmnActionContext(project))
    }

    override fun notificationBalloon(project: Project, message: String, type: NotificationType) {
        showNotificationBalloon(project, message, type)
    }
}
