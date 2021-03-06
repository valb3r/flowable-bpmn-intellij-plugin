package com.valb3r.bpmn.intellij.plugin.core.settings

import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBTextField
import com.intellij.util.ui.FormBuilder
import org.jetbrains.annotations.NotNull
import javax.swing.JComponent

import javax.swing.JPanel


class SettingsComponent {
    val panel: JPanel
    private val myUserNameText: JBTextField = JBTextField()
    private val myIdeaUserStatus: JBCheckBox = JBCheckBox("Do you use IntelliJ IDEA? ")
    val preferredFocusedComponent: JComponent
        get() = myUserNameText

    @get:NotNull
    var userNameText: String?
        get() = myUserNameText.getText()
        set(newText) {
            myUserNameText.setText(newText)
        }
    var ideaUserStatus: Boolean
        get() = myIdeaUserStatus.isSelected()
        set(newStatus) {
            myIdeaUserStatus.setSelected(newStatus)
        }

    init {
        panel = FormBuilder.createFormBuilder()
            .addLabeledComponent(JBLabel("Enter user name: "), myUserNameText, 1, false)
            .addComponent(myIdeaUserStatus, 1)
            .addComponentFillVertically(JPanel(), 0)
            .getPanel()
    }
}