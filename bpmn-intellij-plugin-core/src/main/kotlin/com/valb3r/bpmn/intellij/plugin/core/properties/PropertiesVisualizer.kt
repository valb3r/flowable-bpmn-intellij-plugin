package com.valb3r.bpmn.intellij.plugin.core.properties

import com.intellij.openapi.project.Project
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.BpmnElementId
import com.valb3r.bpmn.intellij.plugin.bpmn.api.events.PropertyUpdateWithId
import com.valb3r.bpmn.intellij.plugin.bpmn.api.info.Property
import com.valb3r.bpmn.intellij.plugin.bpmn.api.info.PropertyType
import com.valb3r.bpmn.intellij.plugin.bpmn.api.info.PropertyValueType.*
import com.valb3r.bpmn.intellij.plugin.core.events.BooleanValueUpdatedEvent
import com.valb3r.bpmn.intellij.plugin.core.events.StringValueUpdatedEvent
import com.valb3r.bpmn.intellij.plugin.core.events.updateEventsRegistry
import com.valb3r.bpmn.intellij.plugin.core.ui.components.FirstColumnReadOnlyModel
import java.util.*
import javax.swing.JComponent
import javax.swing.JTable

private val visualizer = Collections.synchronizedMap(WeakHashMap<Project,  PropertiesVisualizer>())

interface TextValueAccessor {
    val text: String
    val component: JComponent
}

interface SelectedValueAccessor {
    val isSelected: Boolean
    val component: JComponent
}

fun newPropertiesVisualizer(
                            project: Project,
                            table: JTable,
                            dropDownFactory: (id: BpmnElementId, type: PropertyType, value: String, availableValues: Set<String>) -> TextValueAccessor,
                            classEditorFactory: (id: BpmnElementId, type: PropertyType, value: String) -> TextValueAccessor,
                            editorFactory: (id: BpmnElementId, type: PropertyType, value: String) -> TextValueAccessor,
                            textFieldFactory: (id: BpmnElementId, type: PropertyType, value: String) -> TextValueAccessor,
                            checkboxFieldFactory: (id: BpmnElementId, type: PropertyType, value: Boolean) -> SelectedValueAccessor): PropertiesVisualizer {
    return visualizer.computeIfAbsent(project) {
        PropertiesVisualizer(project, table, dropDownFactory, classEditorFactory, editorFactory, textFieldFactory, checkboxFieldFactory)
    }
}

fun propertiesVisualizer(project: Project): PropertiesVisualizer {
    return visualizer[project]!!
}

class PropertiesVisualizer(
        private val project: Project,
        val table: JTable,
        val dropDownFactory: (id: BpmnElementId, type: PropertyType, value: String, availableValues: Set<String>) -> TextValueAccessor,
        val classEditorFactory: (id: BpmnElementId, type: PropertyType, value: String) -> TextValueAccessor,
        val editorFactory: (id: BpmnElementId, type: PropertyType, value: String) -> TextValueAccessor,
        private val textFieldFactory: (id: BpmnElementId, type: PropertyType, value: String) -> TextValueAccessor,
        private val checkboxFieldFactory: (id: BpmnElementId, type: PropertyType, value: Boolean) -> SelectedValueAccessor) {

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
    fun visualize(state: Map<BpmnElementId, Map<PropertyType, Property>>, bpmnElementId: BpmnElementId) {
        notifyDeFocusElement()

        // drop and re-create table model
        val model = FirstColumnReadOnlyModel()
        model.addColumn("")
        model.addColumn("")
        table.model = model
        table.columnModel.getColumn(1).preferredWidth = 500

        state[bpmnElementId]?.forEach {
            when(it.key.valueType) {
                STRING -> model.addRow(arrayOf(it.key.caption, buildTextField(state, bpmnElementId, it.key, it.value)))
                BOOLEAN -> model.addRow(arrayOf(it.key.caption, buildCheckboxField(bpmnElementId, it.key, it.value)))
                CLASS -> model.addRow(arrayOf(it.key.caption, buildClassField(state, bpmnElementId, it.key, it.value)))
                EXPRESSION -> model.addRow(arrayOf(it.key.caption, buildExpressionField(state, bpmnElementId, it.key, it.value)))
                ATTACHED_SEQUENCE_SELECT -> model.addRow(arrayOf(it.key.caption, buildDropDownSelectFieldForTargettedIds(state, bpmnElementId, it.key, it.value)))
            }
        }
        model.fireTableDataChanged()
    }

    private fun notifyDeFocusElement() {
        // Fire de-focus to move changes to memory (Using order as ID property), component listeners doesn't seem to work with EditorTextField
        listenersForCurrentView.toSortedMap().flatMap { it.value }.forEach { it() }
        listenersForCurrentView.clear()
    }

    private fun buildTextField(state: Map<BpmnElementId, Map<PropertyType, Property>>, bpmnElementId: BpmnElementId, type: PropertyType, value: Property): JComponent {
        val fieldValue =  lastStringValueFromRegistry(bpmnElementId, type) ?: (value.value as String? ?: "")
        val field = textFieldFactory.invoke(bpmnElementId, type, fieldValue)
        val initialValue = field.text

        listenersForCurrentView.computeIfAbsent(type.updateOrder) { mutableListOf()}.add {
            if (initialValue != field.text) {
                emitStringUpdateWithCascadeIfNeeded(
                        state,
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
        return field.component
    }

    private fun buildCheckboxField(bpmnElementId: BpmnElementId, type: PropertyType, value: Property): JComponent {
        val fieldValue =  lastBooleanValueFromRegistry(bpmnElementId, type) ?: (value.value as Boolean? ?: false)
        val field = checkboxFieldFactory.invoke(bpmnElementId, type, fieldValue)
        val initialValue = field.isSelected

        listenersForCurrentView.computeIfAbsent(type.updateOrder) { mutableListOf()}.add {
            if (initialValue != field.isSelected) {
                updateEventsRegistry(project).addPropertyUpdateEvent(BooleanValueUpdatedEvent(bpmnElementId, type, field.isSelected))
            }
        }
        return field.component
    }

    private fun buildClassField(state: Map<BpmnElementId, Map<PropertyType, Property>>, bpmnElementId: BpmnElementId, type: PropertyType, value: Property): JComponent {
        val fieldValue = lastStringValueFromRegistry(bpmnElementId, type) ?: (value.value as String? ?: "")
        val field = classEditorFactory(bpmnElementId, type, fieldValue)
        addEditorTextListener(state, field, bpmnElementId, type)
        return field.component
    }

    private fun buildExpressionField(state: Map<BpmnElementId, Map<PropertyType, Property>>, bpmnElementId: BpmnElementId, type: PropertyType, value: Property): JComponent {
        val fieldValue =  lastStringValueFromRegistry(bpmnElementId, type) ?: (value.value as String? ?: "")
        val field = editorFactory(bpmnElementId, type, "\"${fieldValue}\"")
        addEditorTextListener(state, field, bpmnElementId, type)
        return field.component
    }

    private fun buildDropDownSelectFieldForTargettedIds(state: Map<BpmnElementId, Map<PropertyType, Property>>, bpmnElementId: BpmnElementId, type: PropertyType, value: Property): JComponent {
        val fieldValue =  lastStringValueFromRegistry(bpmnElementId, type) ?: (value.value as String? ?: "")
        val field = dropDownFactory(bpmnElementId, type, fieldValue, findCascadeTargetIds(bpmnElementId, type, state))
        addEditorTextListener(state, field, bpmnElementId, type)
        return field.component
    }

    private fun findCascadeTargetIds(forId: BpmnElementId, type: PropertyType, state: Map<BpmnElementId, Map<PropertyType, Property>>): Set<String> {
        if (null == type.updatedBy) {
            throw IllegalArgumentException("Type $type should be cascadable")
        }

        val result = mutableSetOf("")

        state.forEach { (_, props) ->
            props.forEach {property ->
                if (property.key == type.updatedBy && props[PropertyType.SOURCE_REF]?.value == forId.id) {
                    props[PropertyType.ID]?.value?.let { result += it as String }
                }
            }
        }

        return result
    }

    private fun addEditorTextListener(state: Map<BpmnElementId, Map<PropertyType, Property>>, field: TextValueAccessor, bpmnElementId: BpmnElementId, type: PropertyType) {
        val initialValue = field.text
        listenersForCurrentView.computeIfAbsent(type.updateOrder) { mutableListOf()}.add {
            if (initialValue != field.text) {
                emitStringUpdateWithCascadeIfNeeded(state, StringValueUpdatedEvent(bpmnElementId, type, removeQuotes(field.text)))
            }
        }
    }

    private fun emitStringUpdateWithCascadeIfNeeded(state: Map<BpmnElementId, Map<PropertyType, Property>>, event: StringValueUpdatedEvent) {
        val cascades = mutableListOf<PropertyUpdateWithId>()
        if (null != event.referencedValue) {
            state.forEach { (id, props) ->
                props.filter { it.key.updatedBy == event.property }.filter { it.value.value == event.referencedValue }.forEach {prop ->
                    cascades += StringValueUpdatedEvent(id, prop.key, event.newValue, event.referencedValue, null)
                }
            }
        }

        updateEventsRegistry(project).addEvents(listOf(event) + cascades)
    }

    private fun removeQuotes(value: String): String {
        return value.replace("^\"".toRegex(), "").replace("\"$".toRegex(), "")
    }

    private fun lastStringValueFromRegistry(bpmnElementId: BpmnElementId, type: PropertyType): String? {
        return (updateEventsRegistry(project).currentPropertyUpdateEventList(bpmnElementId)
                .map { it.event }
                .filter {
                    bpmnElementId == it.bpmnElementId && it.property.id == type.id
                }
                .lastOrNull { it is StringValueUpdatedEvent } as StringValueUpdatedEvent?)
                ?.newValue
    }

    private fun lastBooleanValueFromRegistry(bpmnElementId: BpmnElementId, type: PropertyType): Boolean? {
        // It is not possible to handle boolean cascades due to ambiguities
        return (updateEventsRegistry(project).currentPropertyUpdateEventList(bpmnElementId)
                .map { it.event }
                .filter { it.property.id == type.id }
                .lastOrNull { it is BooleanValueUpdatedEvent } as BooleanValueUpdatedEvent?)
                ?.newValue
    }
}