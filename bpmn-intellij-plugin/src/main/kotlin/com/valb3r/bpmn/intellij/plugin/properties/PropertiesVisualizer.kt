package com.valb3r.bpmn.intellij.plugin.properties

import com.intellij.ui.EditorTextField
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBTextField
import com.valb3r.bpmn.intellij.plugin.bpmn.api.info.Property
import com.valb3r.bpmn.intellij.plugin.bpmn.api.info.PropertyType
import com.valb3r.bpmn.intellij.plugin.bpmn.api.info.PropertyValueType.*
import com.valb3r.bpmn.intellij.plugin.events.BooleanValueUpdatedEvent
import com.valb3r.bpmn.intellij.plugin.events.StringValueUpdatedEvent
import com.valb3r.bpmn.intellij.plugin.events.updateEventsRegistry
import com.valb3r.bpmn.intellij.plugin.ui.components.FirstColumnReadOnlyModel
import java.awt.event.FocusEvent
import java.awt.event.FocusListener
import javax.swing.JTable

class PropertiesVisualizer(val table: JTable, val editorFactory: (value: String) -> EditorTextField) {

    private val updateRegistry = updateEventsRegistry()

    @Synchronized
    fun visualize(bpmnElementId: String, properties: Map<PropertyType, Property>) {
        val model = FirstColumnReadOnlyModel()
        model.addColumn("")
        model.addColumn("")
        table.model = model
        table.columnModel.getColumn(1).preferredWidth = 500

        properties.forEach {
            when(it.key.valueType) {
                STRING -> model.addRow(arrayOf(it.key.caption, buildTextField(bpmnElementId, it.key, it.value)))
                BOOLEAN -> model.addRow(arrayOf(it.key.caption, buildCheckboxField(bpmnElementId, it.key, it.value)))
                CLASS -> model.addRow(arrayOf(it.key.caption, buildClassField(bpmnElementId, it.key, it.value)))
                EXPRESSION -> model.addRow(arrayOf(it.key.caption, buildExpressionField(bpmnElementId, it.key, it.value)))
            }
        }
        model.fireTableDataChanged()
    }

    private fun buildTextField(bpmnElementId: String, type: PropertyType, value: Property): JBTextField {
        val field = JBTextField(value.value as String? ?: "")
        val initialValue = field.text
        field.addFocusListener(
                FocusEventListener {
                    if (initialValue == field.text) {
                        return@FocusEventListener
                    }

                    updateRegistry.addEvent(StringValueUpdatedEvent(bpmnElementId, type, field.text))
                }
        )
        return field
    }

    private fun buildCheckboxField(bpmnElementId: String, type: PropertyType, value: Property): JBCheckBox {
        val field = JBCheckBox(null, value.value as Boolean? ?: false)
        val initialValue = field.isSelected
        field.addFocusListener(
                FocusEventListener {
                    if (initialValue == field.isSelected) {
                        return@FocusEventListener
                    }

                    updateRegistry.addEvent(BooleanValueUpdatedEvent(bpmnElementId, type, field.isSelected))
                }
        )
        return field
    }

    private fun buildClassField(bpmnElementId: String, type: PropertyType, value: Property): EditorTextField {
        val field = editorFactory( "\"${value.value?.toString() ?: ""}\"")
        addEditorTextListener(field, bpmnElementId, type)
        return field
    }

    private fun buildExpressionField(bpmnElementId: String, type: PropertyType, value: Property): EditorTextField {
        val field = editorFactory( "\"${value.value?.toString() ?: ""}\"")
        addEditorTextListener(field, bpmnElementId, type)
        return field
    }

    private fun addEditorTextListener(field: EditorTextField, bpmnElementId: String, type: PropertyType) {
        val initialValue = field.text
        field.addFocusListener(
                FocusEventListener {
                    if (initialValue == field.text) {
                        return@FocusEventListener
                    }

                    updateRegistry.addEvent(StringValueUpdatedEvent(bpmnElementId, type, removeQuotes(field.text)))
                }
        )
    }

    private fun removeQuotes(value: String): String {
        return value.replace("$\"", "").replace("\"^", "")
    }

    private class FocusEventListener(val onFocusGained: ((e: FocusEvent?) -> Unit)): FocusListener {

        override fun focusLost(e: FocusEvent?) {
            onFocusGained(e)
        }

        override fun focusGained(e: FocusEvent?) {
            // NOP
        }
    }
}