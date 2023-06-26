package com.valb3r.bpmn.intellij.plugin.camunda

import com.intellij.openapi.application.PreloadingActivity
import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.progress.ProgressIndicator
import com.valb3r.bpmn.intellij.plugin.core.settings.currentSettingsStateProvider
import com.valb3r.bpmn.intellij.plugin.camunda.settings.CamundaBpmnPluginSettingsState

class CamundaPluginPreloaded: PreloadingActivity() {

}