package com.valb3r.bpmn.intellij.plugin.core.settings

import com.intellij.openapi.options.Configurable
import javax.swing.JComponent


class BpmnPluginSettingsConfigurable : Configurable {

    private var pluginBpmnPluginSettingsComponent: BpmnPluginSettingsComponent? = null

    override fun isModified(): Boolean {
        return currentSettings().stateEquals(pluginBpmnPluginSettingsComponent!!.state!!)
    }

    override fun getDisplayName(): String {
        return "BPMN plugin settings";
    }

    override fun getPreferredFocusedComponent(): JComponent {
        return pluginBpmnPluginSettingsComponent!!.preferredFocusedComponent
    }

    override fun createComponent(): JComponent {
        pluginBpmnPluginSettingsComponent = BpmnPluginSettingsComponent()
        return pluginBpmnPluginSettingsComponent!!.panel
    }

    override fun apply() {
        val bpmnPluginSettings: BpmnPluginSettingsState = currentSettingsState()
        bpmnPluginSettings.pluginState = pluginBpmnPluginSettingsComponent!!.state!!
    }

    override fun reset() {
        val bpmnPluginSettings: BpmnPluginSettingsState = currentSettingsState()
        pluginBpmnPluginSettingsComponent!!.state = bpmnPluginSettings.pluginState
    }

    override fun disposeUIResources() {
        pluginBpmnPluginSettingsComponent = null
    }
}