package com.valb3r.bpmn.intellij.activiti.plugin.settings

import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.valb3r.bpmn.intellij.plugin.core.settings.BaseBpmnPluginSettingsState

@State(name = "ActivitiBpmnPluginSettingsState", storages = [Storage("valb3r-activiti-bpmn-editor-plugin.xml")]) // fancy XML name to avoid collisions
class ActivitiBpmnPluginSettingsState: BaseBpmnPluginSettingsState()