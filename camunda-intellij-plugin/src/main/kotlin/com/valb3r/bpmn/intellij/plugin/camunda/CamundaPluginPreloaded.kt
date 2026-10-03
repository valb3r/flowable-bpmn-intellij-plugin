package com.valb3r.bpmn.intellij.plugin.camunda

import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.StartupActivity
import com.valb3r.bpmn.intellij.plugin.camunda.advertisement.CamundaAdvertisementState
import com.valb3r.bpmn.intellij.plugin.camunda.settings.CamundaBpmnPluginSettingsState
import com.valb3r.bpmn.intellij.plugin.core.advertisement.currentAdvertisementStateProvider
import com.valb3r.bpmn.intellij.plugin.core.settings.currentSettingsStateProvider
import java.util.concurrent.atomic.AtomicBoolean

class CamundaPluginPreloaded: StartupActivity.Background {

    private val isLoaded = AtomicBoolean()

    override fun runActivity(project: Project) {
        if (isLoaded.compareAndSet(false, true)) {
            currentSettingsStateProvider.set { ServiceManager.getService(CamundaBpmnPluginSettingsState::class.java) }
            currentAdvertisementStateProvider.set { ServiceManager.getService(CamundaAdvertisementState::class.java) }
        }
    }
}