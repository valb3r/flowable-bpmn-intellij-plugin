package com.valb3r.bpmn.intellij.activiti.plugin

import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.StartupActivity
import com.valb3r.bpmn.intellij.activiti.plugin.settings.ActivitiBpmnPluginSettingsState
import com.valb3r.bpmn.intellij.plugin.core.settings.currentSettingsStateProvider

class ActivitiPluginPreloaded: StartupActivity {

    override fun runActivity(project: Project) {
        currentSettingsStateProvider.set { ServiceManager.getService(ActivitiBpmnPluginSettingsState::class.java) }
    }
}