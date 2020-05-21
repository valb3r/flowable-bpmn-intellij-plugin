package com.valb3r.bpmn.intellij.plugin.properties

import com.intellij.ui.EditorTextField
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBTextField
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.BpmnElementId
import com.valb3r.bpmn.intellij.plugin.bpmn.api.events.PropertyUpdateWithId
import com.valb3r.bpmn.intellij.plugin.bpmn.api.info.Property
import com.valb3r.bpmn.intellij.plugin.bpmn.api.info.PropertyType
import com.valb3r.bpmn.intellij.plugin.bpmn.api.info.PropertyValueType.*
import com.valb3r.bpmn.intellij.plugin.events.BooleanValueUpdatedEvent
import com.valb3r.bpmn.intellij.plugin.events.StringValueUpdatedEvent
import com.valb3r.bpmn.intellij.plugin.events.updateEventsRegistry
import com.valb3r.bpmn.intellij.plugin.ui.components.FirstColumnReadOnlyModel
import java.util.concurrent.atomic.AtomicReference
import javax.swing.JTable

class PropertiesVisualizer(val table: JTable, val editorFactory: (value: String) -> EditorTextField) {

    // Using order as ID property change should fire last for this view, otherwise other property change values
    // will use wrong ID as an anchor
    // Listeners with their order
    private var listenersForCurrentView: MutableMap<Int, MutableList<() -> Unit>> = mutableMapOf()

    @Synchronized
    fun clear() {
        notifyDeFocusElement()

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
        notifyDeFocusElement()

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

    private fun notifyDeFocusElement() {
        // Fire de-focus to move changes to memory (Using order as ID property), component listeners doesn't seem to work with EditorTextField
        listenersForCurrentView.toSortedMap().flatMap { it.value }.forEach { it() }
        listenersForCurrentView.clear()
    }

    private fun buildTextField(bpmnElementId: BpmnElementId, type: PropertyType, value: Property): JBTextField {
        val fieldValue =  lastStringValueFromRegistry(bpmnElementId, type, value.value as String?) ?: (value.value as String? ?: "")
        val field = JBTextField(fieldValue)
        val initialValue = field.text

        listenersForCurrentView.computeIfAbsent(type.updateOrder) { mutableListOf()}.add {
            if (initialValue != field.text) {
                updateEventsRegistry().addPropertyUpdateEvent(
                        StringValueUpdatedEvent(
                                bpmnElementId,
                                type,
                                field.text,
                                if (type.cascades) initialValue else null,
                                if (type == PropertyType.ID) BpmnElementId(field.text) else null
                        )
                )
            }
        }
        return field
    }

    private fun buildCheckboxField(bpmnElementId: BpmnElementId, type: PropertyType, value: Property): JBCheckBox {
        val fieldValue =  lastBooleanValueFromRegistry(bpmnElementId, type) ?: (value.value as Boolean? ?: false)
        val field = JBCheckBox(null, fieldValue)
        val initialValue = field.isSelected

        listenersForCurrentView.computeIfAbsent(type.updateOrder) { mutableListOf()}.add {
            if (initialValue != field.isSelected) {
                updateEventsRegistry().addPropertyUpdateEvent(BooleanValueUpdatedEvent(bpmnElementId, type, field.isSelected))
            }
        }
        return field
    }

    private fun buildClassField(bpmnElementId: BpmnElementId, type: PropertyType, value: Property): EditorTextField {
        val fieldValue =  lastStringValueFromRegistry(bpmnElementId, type, value.value as String?) ?: (value.value as String? ?: "")
        val field = editorFactory(fieldValue)
        addEditorTextListener(field, bpmnElementId, type)
        return field
    }

    private fun buildExpressionField(bpmnElementId: BpmnElementId, type: PropertyType, value: Property): EditorTextField {
        val fieldValue =  lastStringValueFromRegistry(bpmnElementId, type, value.value as String?) ?: (value.value as String? ?: "")
        val field = editorFactory( "\"${fieldValue}\"")
        addEditorTextListener(field, bpmnElementId, type)
        return field
    }

    private fun addEditorTextListener(field: EditorTextField, bpmnElementId: BpmnElementId, type: PropertyType) {
        val initialValue = field.text
        listenersForCurrentView.computeIfAbsent(type.updateOrder) { mutableListOf()}.add {
            if (initialValue != field.text) {
                updateEventsRegistry().addPropertyUpdateEvent(StringValueUpdatedEvent(bpmnElementId, type, removeQuotes(field.text)))
            }
        }
    }

    private fun removeQuotes(value: String): String {
        return value.replace("^\"".toRegex(), "").replace("\"$".toRegex(), "")
    }

    private fun lastStringValueFromRegistry(bpmnElementId: BpmnElementId, type: PropertyType, currentValue: String?): String? {
        val computedValue = AtomicReference<String>(currentValue)
        return (updateEventsRegistry().currentPropertyUpdateEventListWithCascaded(bpmnElementId)
                .map { it.event }
                .filter {
                    checkCascadedApplied(computedValue, bpmnElementId, type, it)
                            || (bpmnElementId == it.bpmnElementId && it.property.id == type.id)
                }
                .lastOrNull { it is StringValueUpdatedEvent } as StringValueUpdatedEvent?)
                ?.newValue
    }

    private fun lastBooleanValueFromRegistry(bpmnElementId: BpmnElementId, type: PropertyType): Boolean? {
        // It is not possible to handle boolean cascades due to ambiguities
        return (updateEventsRegistry().currentPropertyUpdateEventList(bpmnElementId)
                .map { it.event }
                .filter { it.property.id == type.id }
                .lastOrNull { it is BooleanValueUpdatedEvent } as BooleanValueUpdatedEvent?)
                ?.newValue
    }

    private fun checkCascadedApplied(computedValue: AtomicReference<String>, elementId: BpmnElementId, type: PropertyType, possibleCascade: PropertyUpdateWithId): Boolean {
        if ((elementId == possibleCascade.bpmnElementId || possibleCascade.property == type) || null == possibleCascade.referencedValue) {
            return false
        }

        if (!possibleCascade.property.cascades) {
            return true
        }

        if (possibleCascade.property == type.updatedBy && possibleCascade.newValue is String && possibleCascade.referencedValue == computedValue.get()) {
            computedValue.set(possibleCascade.newValue as String)
            return true
        }

        return false
    }
}