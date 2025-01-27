package com.valb3r.bpmn.intellij.plugin.core.ui.components.notifications

import com.intellij.ide.BrowserUtil
import com.intellij.notification.*
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.Project
import java.util.concurrent.ConcurrentHashMap

private val notificationGroups = ConcurrentHashMap<String, NotificationGroup>()

fun genericShowNotificationBalloon(project: Project, groupId: String, text: String, notificationType: NotificationType, actionText: String? = null, actionCallBack: (() -> Unit)? = null) {
    val notification = notificationGroups.computeIfAbsent(groupId + notificationType.name) {
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
    }.createNotification(text, notificationType)
    if (actionText != null && actionCallBack != null) {
        val action: NotificationAction = object : NotificationAction(actionText) {
            override fun actionPerformed(e: AnActionEvent, notification: Notification) {
                actionCallBack.invoke()
                notification.expire()
            }

            override fun getActionUpdateThread(): ActionUpdateThread {
                return ActionUpdateThread.EDT
            }
        }
        notification.addAction(action)
    }
    notification.setListener { _, event -> BrowserUtil.browse(event.url) }
    notification.notify(project)
}