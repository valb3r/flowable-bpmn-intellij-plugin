package com.valb3r.bpmn.intellij.plugin.camunda.settings

import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.valb3r.bpmn.intellij.plugin.core.settings.BaseBpmnPluginSettingsState

@State(name = "CamundaBpmnPluginSettingsState", storages = [Storage("valb3r-camunda-bpmn-editor-plugin.xml")]) // fancy XML name to avoid collisions
class CamundaBpmnPluginSettingsState: BaseBpmnPluginSettingsState()