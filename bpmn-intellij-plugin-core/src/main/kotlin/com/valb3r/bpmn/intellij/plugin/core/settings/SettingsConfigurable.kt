package com.valb3r.bpmn.intellij.plugin.core.settings

import com.intellij.openapi.options.Configurable
import javax.swing.JComponent


class SettingsConfigurable : Configurable {
    private var mySettingsComponent: SettingsComponent? = null

    override fun isModified(): Boolean {
        val settings: SettingsState = SettingsState.getInstance()!!
        return (mySettingsComponent!!.ideaUserStatus != settings.ideaStatus)
    }

    override fun getDisplayName(): String {
        return "SDK: Application Settings Example";
    }

    override fun getPreferredFocusedComponent(): JComponent {
        return mySettingsComponent!!.preferredFocusedComponent
    }

    override fun createComponent(): JComponent {
        mySettingsComponent = SettingsComponent()
        return mySettingsComponent!!.panel
    }

    override fun apply() {
        val settings: SettingsState = SettingsState.getInstance()!!
        settings.ideaStatus = mySettingsComponent!!.ideaUserStatus
    }

    override fun reset() {
        val settings: SettingsState = SettingsState.getInstance()!!
        mySettingsComponent!!.ideaUserStatus = settings.ideaStatus
    }

    override fun disposeUIResources() {
        mySettingsComponent = null
    }
}