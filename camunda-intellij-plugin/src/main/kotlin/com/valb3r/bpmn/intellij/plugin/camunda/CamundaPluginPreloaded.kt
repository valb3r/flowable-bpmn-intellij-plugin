package com.valb3r.bpmn.intellij.plugin.camunda

import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.StartupActivity
import com.valb3r.bpmn.intellij.plugin.camunda.settings.CamundaBpmnPluginSettingsState
import com.valb3r.bpmn.intellij.plugin.core.settings.currentSettingsStateProvider

class CamundaPluginPreloaded: StartupActivity {

    override fun runActivity(project: Project) {
        currentSettingsStateProvider.set { ServiceManager.getService(CamundaBpmnPluginSettingsState::class.java) }
    }
}