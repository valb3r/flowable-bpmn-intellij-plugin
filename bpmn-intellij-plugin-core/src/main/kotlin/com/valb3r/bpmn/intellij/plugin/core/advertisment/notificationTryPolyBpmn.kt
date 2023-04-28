package com.valb3r.bpmn.intellij.plugin.core.advertisment

import com.intellij.notification.NotificationType
import com.intellij.openapi.project.Project
import com.valb3r.bpmn.intellij.plugin.core.ui.components.notifications.genericShowNotificationBalloon
import java.time.LocalDate

fun showTryPolyBpmnNotification(project: Project) {
    val checkDate = AdvertisementCache.getInstance(project).lastShow
    val now = LocalDate.now()
    val frequencyCheck = 30L
    if(checkDate == LocalDate.MIN || checkDate.plusDays(frequencyCheck).isBefore(now)) {
        AdvertisementCache.getInstance(project).lastShow = now
        genericShowNotificationBalloon(project, "Advertisement", "Try <a href='https://plugins.jetbrains.com/plugin/21361-polybpmn-visualizer'>PolyBPMN plugin</a>. <br> PolyBPMN's upgraded split editor to each file, revision file comparison and a wider selection of elements and properties..", NotificationType.INFORMATION, "Do not show again"){
            AdvertisementCache.getInstance(project).lastShow = LocalDate.MAX.minusDays(frequencyCheck).minusDays(1)
        }
    }
}