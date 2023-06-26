package com.valb3r.bpmn.intellij.plugin.flowable

import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.StartupActivity
import com.valb3r.bpmn.intellij.plugin.core.settings.currentSettingsStateProvider
import com.valb3r.bpmn.intellij.plugin.flowable.settings.FlowableBpmnPluginSettingsState

class FlowablePluginPreloaded: StartupActivity {

    override fun runActivity(project: Project) {
        currentSettingsStateProvider.set { ServiceManager.getService(FlowableBpmnPluginSettingsState::class.java) }
    }
}