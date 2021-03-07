package com.valb3r.bpmn.intellij.plugin.core.settings

import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBTextField
import com.intellij.util.ui.FormBuilder
import javax.swing.JComponent
import javax.swing.JPanel


class BpmnPluginSettingsComponent {
    val panel: JPanel
    val preferredFocusedComponent: JComponent
        get() = myUserNameText

    private val myUserNameText: JBTextField = JBTextField()
    private val myIdeaUserStatus: JBCheckBox = JBCheckBox("Do you use IntelliJ IDEA? ")

    var state: BpmnPluginSettingsState.PluginStateData? = null
    set(value) {
        field = value
    }

    init {
        panel = FormBuilder.createFormBuilder()
            .addLabeledComponent(JBLabel("Enter user name: "), myUserNameText, 1, false)
            .addComponent(myIdeaUserStatus, 1)
            .addComponentFillVertically(JPanel(), 0)
            .getPanel()
    }
}