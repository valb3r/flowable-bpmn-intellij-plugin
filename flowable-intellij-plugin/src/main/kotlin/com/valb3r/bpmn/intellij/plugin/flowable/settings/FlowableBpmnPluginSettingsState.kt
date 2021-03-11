package com.valb3r.bpmn.intellij.plugin.flowable.settings

import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.valb3r.bpmn.intellij.plugin.core.settings.BaseBpmnPluginSettingsState

@State(name = "FlowableBpmnPluginSettingsState", storages = [Storage("valb3r-flowable-bpmn-editor-plugin.xml")]) // fancy XML name to avoid collisions
class FlowableBpmnPluginSettingsState: BaseBpmnPluginSettingsState()