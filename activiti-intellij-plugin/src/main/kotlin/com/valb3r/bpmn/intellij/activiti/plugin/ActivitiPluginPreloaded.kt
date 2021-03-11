package com.valb3r.bpmn.intellij.activiti.plugin

import com.intellij.openapi.application.PreloadingActivity
import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.progress.ProgressIndicator
import com.valb3r.bpmn.intellij.activiti.plugin.settings.ActivitiBpmnPluginSettingsState
import com.valb3r.bpmn.intellij.plugin.core.settings.currentSettingsStateProvider

class ActivitiPluginPreloaded: PreloadingActivity() {

    override fun preload(indicator: ProgressIndicator) {
        currentSettingsStateProvider.set { ServiceManager.getService(ActivitiBpmnPluginSettingsState::class.java) }
    }
}