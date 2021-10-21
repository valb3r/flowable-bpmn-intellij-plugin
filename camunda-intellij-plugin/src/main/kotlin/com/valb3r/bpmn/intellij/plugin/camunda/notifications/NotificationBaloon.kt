package com.valb3r.bpmn.intellij.activiti.plugin.notifications

import com.intellij.notification.NotificationType
import com.intellij.openapi.project.Project
import com.valb3r.bpmn.intellij.plugin.core.ui.components.notifications.genericShowNotificationBalloon

fun showNotificationBalloon(project: Project, text: String, notificationType: NotificationType) {
    genericShowNotificationBalloon(project, "Camunda BPMN Editor Notification group", text, notificationType)
}