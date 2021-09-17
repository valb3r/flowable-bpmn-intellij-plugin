package com.valb3r.bpmn.intellij.plugin.core.ui.components.notifications

import com.intellij.notification.NotificationDisplayType
import com.intellij.notification.NotificationGroup
import com.intellij.notification.NotificationType
import com.intellij.openapi.project.Project
import java.util.concurrent.ConcurrentHashMap

private val notificationGroups = ConcurrentHashMap<String, NotificationGroup>()

fun genericShowNotificationBalloon(project: Project, groupId: String, text: String, notificationType: NotificationType) {
    notificationGroups.computeIfAbsent(groupId + notificationType.name) {
        try {
            val notificationManagerClz = Class.forName("com.intellij.notification.NotificationGroupManager")
            val notificationManager = notificationManagerClz.getMethod("getInstance").invoke(null)
            return@computeIfAbsent notificationManager.javaClass.getMethod("getNotificationGroup", String::class.java).invoke(notificationManager, groupId) as NotificationGroup
        } catch (ex: Exception) {
            // TODO !COMPATIBILITY - pre 2020.3 versions are missing NotificationGroupManager
            val notificationGroupClz = Class.forName("com.intellij.notification.NotificationGroup")
            val ctor = notificationGroupClz.getConstructor(String::class.java, NotificationDisplayType::class.java, Boolean::class.java)
            return@computeIfAbsent ctor.newInstance(groupId, NotificationDisplayType.BALLOON, true) as NotificationGroup
        }
    }.createNotification(text, notificationType).notify(project)
}