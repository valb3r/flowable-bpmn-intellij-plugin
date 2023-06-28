package com.valb3r.bpmn.intellij.plugin.core.advertisement

import com.intellij.notification.NotificationType
import com.intellij.openapi.project.Project
import com.valb3r.bpmn.intellij.plugin.core.ui.components.notifications.genericShowNotificationBalloon
import java.time.LocalDate

fun showTryPolyBpmnAdvertisementNotification(project: Project) {
    val checkDate = AdvertisementState.getInstance().lastShowCommon
    val now = LocalDate.now()
    val frequencyCheck = 30L
    if(checkDate == LocalDate.MIN || checkDate.plusDays(frequencyCheck).isBefore(now)) {
        AdvertisementState.getInstance().lastShowCommon = now
        genericShowNotificationBalloon(project, "Advertisement", "Try <a href='https://plugins.jetbrains.com/plugin/21361-polybpmn-visualizer'>PolyBPMN plugin</a>. <br> PolyBPMN's upgraded split editor to each file, revision file comparison and a wider selection of elements and properties..", NotificationType.INFORMATION, "Do not show again") {
            doNotShowAgain(project, frequencyCheck) // set maximum date
        }
    }
}

fun showTryPolyBpmnAdvertisementSwimpoolNotification(project: Project) {
    val checkDate = AdvertisementState.getInstance().lastShowSwimpoolAd
    val now = LocalDate.now()
    val frequencyCheck = 30L
    if(checkDate == LocalDate.MIN || checkDate.plusDays(frequencyCheck).isBefore(now)) {
        AdvertisementState.getInstance().lastShowSwimpoolAd = now
        genericShowNotificationBalloon(project, "Advertisement Swimpool", "Unlock the full potential of diagram reading and editiong with the PolyBPMN plugin. Seamlessly visualize and interpret swimpools and swimlanes with ease. Dwonload from <a href='https://plugins.jetbrains.com/plugin/21361-polybpmn-visualizer'>here</a>.\n" +
                "Unlock the full potential of diagram reading and editiong with the PolyBPMN plugin. Seamlessly visualize and interpret swimpools and swimlanes with ease. Dwonload from <a href='https://plugins.jetbrains.com/plugin/21361-polybpmn-visualizer'>here</a>.", NotificationType.INFORMATION, "Do not show again") {
            doNotShowAgain(project, frequencyCheck) // set maximum date
        }
    }
}

private fun doNotShowAgain(project: Project, frequencyCheck: Long) {
    AdvertisementState.getInstance().lastShowCommon = LocalDate.MAX.minusDays(frequencyCheck).minusDays(1)
}