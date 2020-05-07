package com.valb3r.bpmn.intellij.plugin.properties

import com.intellij.ui.EditorTextField
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBTextField
import com.valb3r.bpmn.intellij.plugin.bpmn.api.info.Property
import com.valb3r.bpmn.intellij.plugin.bpmn.api.info.PropertyType
import com.valb3r.bpmn.intellij.plugin.bpmn.api.info.PropertyValueType.*
import javax.swing.JTable
import javax.swing.table.DefaultTableModel

class PropertiesVisualizer(val table: JTable, val editorFactory: (value: String) -> EditorTextField) {

    fun visualize(properties: Map<PropertyType, Property>) {
        val model = table.model as DefaultTableModel

        model.rowCount = 0
        model.fireTableDataChanged()

        properties.forEach {
            when(it.key.valueType) {
                STRING -> model.addRow(arrayOf(it.key.caption, JBTextField(it.value.value as String? ?: "")))
                BOOLEAN -> model.addRow(arrayOf(it.key.caption, JBCheckBox(null, it.value.value as Boolean? ?: false)))
                CLASS -> model.addRow(arrayOf(it.key.caption, editorFactory( "\"${it.value.value?.toString()}\"")))
                EXPRESSION -> model.addRow(arrayOf(it.key.caption, editorFactory( "\"${it.value.value?.toString()}\"")))
            }
        }
        model.fireTableDataChanged()
    }
}