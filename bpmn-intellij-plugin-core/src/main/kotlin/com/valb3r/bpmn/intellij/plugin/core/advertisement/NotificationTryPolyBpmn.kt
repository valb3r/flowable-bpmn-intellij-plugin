package com.valb3r.bpmn.intellij.plugin.core.advertisement

import com.intellij.notification.NotificationType
import com.intellij.openapi.project.Project
import com.valb3r.bpmn.intellij.plugin.core.ui.components.notifications.genericShowNotificationBalloon
import java.time.LocalDate

fun showTryPolyBpmnAdvertisementNotification(project: Project) {
    val checkDate = currentAdvertisementState().lastDisplayDateGlobal
    val now = LocalDate.now()
    val showOnceInDays = 30L
    if (shouldShow(checkDate, showOnceInDays, now)) {
        currentAdvertisementState().lastDisplayDateGlobal = now
        genericShowNotificationBalloon(project, "Advertisement", "Try <a href='https://plugins.jetbrains.com/plugin/21361-polybpmn-visualizer'>PolyBPMN plugin</a>. <br> PolyBPMN's has upgraded split editor for each file, diagram history and a wider selection of elements and properties.", NotificationType.INFORMATION, "Do not show again") {
            doNotShowAgain(showOnceInDays) // set maximum date
        }
    }
}

fun showTryPolyBpmnAdvertisementSwimpoolNotification(project: Project) {
    val checkDate =currentAdvertisementState().lastDisplayDateSwimpoolAd
    val now = LocalDate.now()
    val showOnceInDays = 30L
    if (shouldShow(checkDate, showOnceInDays, now)) {
        currentAdvertisementState().lastDisplayDateSwimpoolAd = now
        genericShowNotificationBalloon(project, "Advertisement Swimming pool", "Unlock the full potential of diagram reading and editing with the <a href='https://plugins.jetbrains.com/plugin/21361-polybpmn-visualizer'>PolyBPMN plugin</a>. Seamlessly visualize and interpret swimming pools and swimming lanes with ease. Download from <a href='https://plugins.jetbrains.com/plugin/21361-polybpmn-visualizer'>here</a>", NotificationType.INFORMATION, "Do not show again") {
            doNotShowAgain(showOnceInDays) // set maximum date
        }
    }
}

private fun shouldShow(checkDate: LocalDate, showOnceInDays: Long, now: LocalDate?): Boolean {
    val neverShown = checkDate == LocalDate.MIN
    val shownLongTimeAgo = checkDate.plusDays(showOnceInDays).isBefore(now)
    return neverShown || shownLongTimeAgo
}


private fun doNotShowAgain(showOnceInDays: Long) {
    currentAdvertisementState().lastDisplayDateGlobal = LocalDate.MAX.minusDays(showOnceInDays).minusDays(1)
}