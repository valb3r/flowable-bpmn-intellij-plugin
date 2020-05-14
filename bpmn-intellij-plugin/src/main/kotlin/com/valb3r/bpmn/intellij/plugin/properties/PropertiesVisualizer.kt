package com.valb3r.bpmn.intellij.plugin.properties

import com.intellij.ui.EditorTextField
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBTextField
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.BpmnElementId
import com.valb3r.bpmn.intellij.plugin.bpmn.api.info.Property
import com.valb3r.bpmn.intellij.plugin.bpmn.api.info.PropertyType
import com.valb3r.bpmn.intellij.plugin.bpmn.api.info.PropertyValueType.*
import com.valb3r.bpmn.intellij.plugin.events.BooleanValueUpdatedEvent
import com.valb3r.bpmn.intellij.plugin.events.StringValueUpdatedEvent
import com.valb3r.bpmn.intellij.plugin.events.updateEventsRegistry
import com.valb3r.bpmn.intellij.plugin.ui.components.FirstColumnReadOnlyModel
import javax.swing.JTable

class PropertiesVisualizer(val table: JTable, val editorFactory: (value: String) -> EditorTextField) {

    private var listenersForCurrentView: MutableList<(() -> Unit)> = mutableListOf()

    @Synchronized
    fun clear() {
        // fire de-focus to move changes to memory, component listeners doesn't seem to work with EditorTextField
        listenersForCurrentView.forEach { it() }
        listenersForCurrentView.clear()

        // drop and re-create table model
        val model = FirstColumnReadOnlyModel()
        model.addColumn("")
        model.addColumn("")
        table.model = model
        table.columnModel.getColumn(1).preferredWidth = 500
        model.fireTableDataChanged()
    }

    @Synchronized
    fun visualize(bpmnElementId: BpmnElementId, properties: Map<PropertyType, Property>) {
        // fire de-focus to move changes to memory, component listeners doesn't seem to work with EditorTextField
        listenersForCurrentView.forEach { it() }
        listenersForCurrentView.clear()

        // drop and re-create table model
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

    private fun buildTextField(bpmnElementId: BpmnElementId, type: PropertyType, value: Property): JBTextField {
        val fieldValue =  lastStringValueFromRegistry(bpmnElementId, type) ?: (value.value as String? ?: "")
        val field = JBTextField(fieldValue)
        val initialValue = field.text

        listenersForCurrentView.add {
            if (initialValue != field.text) {
                updateEventsRegistry().addPropertyUpdateEvent(StringValueUpdatedEvent(bpmnElementId, type, field.text))
            }
        }
        return field
    }

    private fun buildCheckboxField(bpmnElementId: BpmnElementId, type: PropertyType, value: Property): JBCheckBox {
        val fieldValue =  lastBooleanValueFromRegistry(bpmnElementId, type) ?: (value.value as Boolean? ?: false)
        val field = JBCheckBox(null, fieldValue)
        val initialValue = field.isSelected

        listenersForCurrentView.add {
            if (initialValue != field.isSelected) {
                updateEventsRegistry().addPropertyUpdateEvent(BooleanValueUpdatedEvent(bpmnElementId, type, field.isSelected))
            }
        }
        return field
    }

    private fun buildClassField(bpmnElementId: BpmnElementId, type: PropertyType, value: Property): EditorTextField {
        val fieldValue =  lastStringValueFromRegistry(bpmnElementId, type) ?: (value.value as String? ?: "")
        val field = editorFactory( "\"${fieldValue}\"")
        addEditorTextListener(field, bpmnElementId, type)
        return field
    }

    private fun buildExpressionField(bpmnElementId: BpmnElementId, type: PropertyType, value: Property): EditorTextField {
        val fieldValue =  lastStringValueFromRegistry(bpmnElementId, type) ?: (value.value as String? ?: "")
        val field = editorFactory( "\"${fieldValue}\"")
        addEditorTextListener(field, bpmnElementId, type)
        return field
    }

    private fun addEditorTextListener(field: EditorTextField, bpmnElementId: BpmnElementId, type: PropertyType) {
        val initialValue = field.text
        listenersForCurrentView.add {
            if (initialValue != field.text) {
                updateEventsRegistry().addPropertyUpdateEvent(StringValueUpdatedEvent(bpmnElementId, type, removeQuotes(field.text)))
            }
        }
    }

    private fun removeQuotes(value: String): String {
        return value.replace("^\"".toRegex(), "").replace("\"$".toRegex(), "")
    }

    private fun lastStringValueFromRegistry(bpmnElementId: BpmnElementId, type: PropertyType): String? {
        return (updateEventsRegistry().currentPropertyUpdateEventList(bpmnElementId)
                .map { it.event }
                .filter { it.property.id == type.id }
                .lastOrNull { it is StringValueUpdatedEvent } as StringValueUpdatedEvent?)
                ?.newValue
    }

    private fun lastBooleanValueFromRegistry(bpmnElementId: BpmnElementId, type: PropertyType): Boolean? {
        return (updateEventsRegistry().currentPropertyUpdateEventList(bpmnElementId)
                .map { it.event }
                .filter { it.property.id == type.id }
                .lastOrNull { it is BooleanValueUpdatedEvent } as BooleanValueUpdatedEvent?)
                ?.newValue
    }
}