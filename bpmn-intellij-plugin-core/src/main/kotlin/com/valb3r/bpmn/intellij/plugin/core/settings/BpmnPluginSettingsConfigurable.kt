package com.valb3r.bpmn.intellij.plugin.core.settings

import com.intellij.openapi.options.Configurable
import com.intellij.openapi.options.ConfigurationException
import com.valb3r.bpmn.intellij.plugin.core.render.allCanvas
import javax.swing.JComponent


class BpmnPluginSettingsConfigurable : Configurable {

    private var pluginBpmnPluginSettingsComponent: BpmnPluginSettingsComponent? = null

    override fun isModified(): Boolean {
        return !currentSettings().stateEquals(pluginBpmnPluginSettingsComponent!!.state)
    }

    override fun getDisplayName(): String {
        return "BPMN plugin settings";
    }

    override fun getPreferredFocusedComponent(): JComponent {
        return pluginBpmnPluginSettingsComponent!!.preferredFocusedComponent
    }

    override fun createComponent(): JComponent {
        pluginBpmnPluginSettingsComponent = BpmnPluginSettingsComponent()
        return pluginBpmnPluginSettingsComponent!!.settingsPanel
    }

    override fun apply() {
        val bpmnPluginSettings: BaseBpmnPluginSettingsState = currentSettingsState()
        val errors = pluginBpmnPluginSettingsComponent!!.isValid()
        if (null != errors) {
            throw ConfigurationException(errors)
        }

        bpmnPluginSettings.pluginState = pluginBpmnPluginSettingsComponent!!.state.copy()
        allCanvas().forEach { it.repaint() }
    }

    override fun reset() {
        pluginBpmnPluginSettingsComponent!!.state = currentSettings().copy()
        allCanvas().forEach { it.repaint() }
    }

    override fun disposeUIResources() {
        pluginBpmnPluginSettingsComponent = null
    }
}