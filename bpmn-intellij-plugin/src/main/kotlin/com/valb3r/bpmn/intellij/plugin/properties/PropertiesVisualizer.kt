package com.valb3r.bpmn.intellij.plugin.properties

import com.intellij.ui.EditorTextField
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBTextField
import com.valb3r.bpmn.intellij.plugin.bpmn.api.info.Property
import com.valb3r.bpmn.intellij.plugin.bpmn.api.info.PropertyType
import com.valb3r.bpmn.intellij.plugin.bpmn.api.info.PropertyValueType.*
import com.valb3r.bpmn.intellij.plugin.ui.components.FirstColumnReadOnlyModel
import javax.swing.JTable

class PropertiesVisualizer(val table: JTable, val editorFactory: (value: String) -> EditorTextField) {

    @Synchronized
    fun visualize(properties: Map<PropertyType, Property>) {
        val model = FirstColumnReadOnlyModel()
        model.addColumn("")
        model.addColumn("")
        table.model = model
        table.columnModel.getColumn(1).preferredWidth = 500

        properties.forEach {
            when(it.key.valueType) {
                STRING -> model.addRow(arrayOf(it.key.caption, JBTextField(it.value.value as String? ?: "")))
                BOOLEAN -> model.addRow(arrayOf(it.key.caption, JBCheckBox(null, it.value.value as Boolean? ?: false)))
                CLASS -> model.addRow(arrayOf(it.key.caption, editorFactory( "\"${it.value.value?.toString() ?: ""}\"")))
                EXPRESSION -> model.addRow(arrayOf(it.key.caption, editorFactory( "\"${it.value.value?.toString() ?: ""}\"")))
            }
        }
        model.fireTableDataChanged()
    }
}