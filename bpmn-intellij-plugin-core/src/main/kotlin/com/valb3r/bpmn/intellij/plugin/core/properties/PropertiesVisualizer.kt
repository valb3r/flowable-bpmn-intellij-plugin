package com.valb3r.bpmn.intellij.plugin.core.properties

import com.intellij.openapi.project.Project
import com.valb3r.bpmn.intellij.plugin.bpmn.api.PropertyTable
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.BpmnElementId
import com.valb3r.bpmn.intellij.plugin.bpmn.api.events.Event
import com.valb3r.bpmn.intellij.plugin.bpmn.api.info.FunctionalGroupType
import com.valb3r.bpmn.intellij.plugin.bpmn.api.info.Property
import com.valb3r.bpmn.intellij.plugin.bpmn.api.info.PropertyType
import com.valb3r.bpmn.intellij.plugin.bpmn.api.info.PropertyValueType.*
import com.valb3r.bpmn.intellij.plugin.core.events.*
import com.valb3r.bpmn.intellij.plugin.core.state.currentStateProvider
import com.valb3r.bpmn.intellij.plugin.core.ui.components.FirstColumnReadOnlyModel
import java.util.*
import javax.swing.*
import javax.swing.plaf.basic.BasicArrowButton
import javax.swing.table.TableRowSorter

private const val maxFields = 9999

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
                            checkboxFieldFactory: (id: BpmnElementId, type: PropertyType, value: Boolean) -> SelectedValueAccessor,
                            buttonFactory: (id: BpmnElementId, type: FunctionalGroupType) -> JButton): PropertiesVisualizer {
    val newVisualizer = PropertiesVisualizer(project, table, dropDownFactory, classEditorFactory, editorFactory, textFieldFactory, checkboxFieldFactory, buttonFactory)
    visualizer[project] = newVisualizer
    return newVisualizer
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
        private val checkboxFieldFactory: (id: BpmnElementId, type: PropertyType, value: Boolean) -> SelectedValueAccessor,
        private val buttonFactory: (id: BpmnElementId, type: FunctionalGroupType) -> JButton) {

    // Using order as ID property change should fire last for this view, otherwise other property change values
    // will use wrong ID as an anchor
    // Listeners with their order
    private var listenersForCurrentView: MutableMap<Int, MutableList<() -> Unit>> = mutableMapOf()

    @Synchronized
    fun clear() {
        val model = prepareTable()
        model.fireTableDataChanged()
    }

    @Synchronized
    fun visualize(state: Map<BpmnElementId, PropertyTable>, bpmnElementId: BpmnElementId) {
        val model = prepareTable()
        val filter = RowExpansionFilter()

        val groupedEntries = state[bpmnElementId]?.view()?.entries
            ?.groupBy { it.key.group }
            ?.toSortedMap(Comparator.comparingInt { it?.name?.length ?: 0 }) ?: emptyMap()
        
        for ((groupType, entries) in groupedEntries) {
            if (null != groupType) {
                model.addRow(arrayOf("", groupType.groupCaption))
            }

            val byIndex = entries.flatMap { it.value }.filter { null != it.value }.groupBy { it.index ?: "" }
            entries
                .flatMap { it.value.map { v -> Pair(it.key, v) } }
                .filter { null == groupType || true == byIndex[it.second.index ?: ""]?.isNotEmpty() }
                .sortedBy { it.second.index }
                .forEach {
                    when(it.first.valueType) {
                        STRING -> model.addRow(arrayOf(it.first.caption, buildTextField(state, bpmnElementId, it.first, it.second), BasicArrowButton(SwingConstants.EAST)))
                        BOOLEAN -> model.addRow(arrayOf(it.first.caption, buildCheckboxField(bpmnElementId, it.first, it.second)))
                        CLASS -> model.addRow(arrayOf(it.first.caption, buildClassField(state, bpmnElementId, it.first, it.second)))
                        EXPRESSION -> model.addRow(arrayOf(it.first.caption, buildExpressionField(state, bpmnElementId, it.first, it.second)))
                        ATTACHED_SEQUENCE_SELECT -> model.addRow(arrayOf(it.first.caption, buildDropDownSelectFieldForTargettedIds(state, bpmnElementId, it.first, it.second)))
                    }
                }

            if (null != groupType) {
                model.addRow(arrayOf("", buildButtonField(state, bpmnElementId, groupType)))
            }
        }
        
        model.fireTableDataChanged()
        val sorter = TableRowSorter(model)
        sorter.rowFilter = filter
    }

    private fun notifyDeFocusElement() {
        // Fire de-focus to move changes to memory (Using order as ID property), component listeners doesn't seem to work with EditorTextField
        listenersForCurrentView.toSortedMap().flatMap { it.value }.forEach { it() }
        listenersForCurrentView.clear()
    }

    private fun buildTextField(state: Map<BpmnElementId, PropertyTable>, bpmnElementId: BpmnElementId, type: PropertyType, value: Property): JComponent {
        val fieldValue = extractString(value)
        val field = textFieldFactory.invoke(bpmnElementId, type, fieldValue)
        val initialValue = field.text

        listenersForCurrentView.computeIfAbsent(type.updateOrder) { mutableListOf()}.add {
            if (initialValue != field.text) {
                emitStringUpdateWithCascadeIfNeeded(
                    state,
                    stringValueUpdatedTemplate(bpmnElementId, type, field.text, initialValue, value)
                )
            }
        }
        return field.component
    }

    private fun buildCheckboxField(bpmnElementId: BpmnElementId, type: PropertyType, value: Property): JComponent {
        val fieldValue = extractBoolean(value)
        val field = checkboxFieldFactory.invoke(bpmnElementId, type, fieldValue)
        val initialValue = field.isSelected

        listenersForCurrentView.computeIfAbsent(type.updateOrder) { mutableListOf()}.add {
            if (initialValue != field.isSelected) {
                updateEventsRegistry(project).addPropertyUpdateEvent(BooleanValueUpdatedEvent(bpmnElementId, type, field.isSelected))
            }
        }
        return field.component
    }

    private fun buildClassField(state: Map<BpmnElementId, PropertyTable>, bpmnElementId: BpmnElementId, type: PropertyType, value: Property): JComponent {
        val fieldValue = extractString(value)
        val field = classEditorFactory(bpmnElementId, type, fieldValue)
        addEditorTextListener(state, field, bpmnElementId, type, value)
        return field.component
    }

    private fun buildExpressionField(state: Map<BpmnElementId, PropertyTable>, bpmnElementId: BpmnElementId, type: PropertyType, value: Property): JComponent {
        val fieldValue =  extractString(value)
        val field = editorFactory(bpmnElementId, type, "\"${fieldValue}\"")
        addEditorTextListener(state, field, bpmnElementId, type, value)
        return field.component
    }

    private fun buildDropDownSelectFieldForTargettedIds(state: Map<BpmnElementId, PropertyTable>, bpmnElementId: BpmnElementId, type: PropertyType, value: Property): JComponent {
        val fieldValue = extractString(value)
        val field = dropDownFactory(bpmnElementId, type, fieldValue, findCascadeTargetIds(bpmnElementId, type, state))
        addEditorTextListener(state, field, bpmnElementId, type, value)
        return field.component
    }

    private fun buildButtonField(state: Map<BpmnElementId, PropertyTable>, bpmnElementId: BpmnElementId, type: FunctionalGroupType): JComponent {
        val button = buttonFactory(bpmnElementId, type)
        addButtonListener(state, button, bpmnElementId, type)
        return button
    }

    private fun findCascadeTargetIds(forId: BpmnElementId, type: PropertyType, state: Map<BpmnElementId, PropertyTable>): Set<String> {
        if (null == type.updatedBy) {
            throw IllegalArgumentException("Type $type should be cascadable")
        }

        val result = mutableSetOf("")

        state.forEach { (_, props) ->
            props.forEach { k, _ ->
                if (k == type.updatedBy && props[PropertyType.SOURCE_REF]?.value == forId.id) {
                    props[PropertyType.ID]?.value?.let { result += it as String }
                }
            }
        }

        return result
    }

    private fun addEditorTextListener(state: Map<BpmnElementId, PropertyTable>, field: TextValueAccessor, bpmnElementId: BpmnElementId, type: PropertyType, value: Property) {
        val initialValue = field.text
        listenersForCurrentView.computeIfAbsent(type.updateOrder) { mutableListOf()}.add {
            if (initialValue != field.text) {
                emitStringUpdateWithCascadeIfNeeded(
                    state,
                    stringValueUpdatedTemplate(bpmnElementId, type,  removeQuotes(field.text), initialValue, value)
                )
            }
        }
    }

    private fun addButtonListener(state: Map<BpmnElementId, PropertyTable>, field: JButton, bpmnElementId: BpmnElementId, type: FunctionalGroupType) {
        fun propertyType(name: String) = PropertyType.values().find { it.name == name }!!

        field.addActionListener {
            val propType = propertyType(type.actionResult.propertyType)
            val allPropsOfType = state[bpmnElementId]!!.getAll(propType).map { it.index }.toSet()
            val countFields = allPropsOfType.size
            val fieldName = (countFields..maxFields).map { type.actionResult.valuePattern.format(it) }.firstOrNull { !allPropsOfType.contains(it) } ?: UUID.randomUUID().toString()
            val events = mutableListOf<Event>(StringValueUpdatedEvent(bpmnElementId, propType, fieldName, propertyIndex = fieldName))
            events += type.actionUiOnlyResult.map { UiOnlyValueAddedEvent(bpmnElementId, propertyType(it.propertyType), it.valuePattern, propertyIndex = fieldName) }
            updateEventsRegistry(project).addEvents(events)
            visualize(currentStateProvider(project).currentState().elemPropertiesByStaticElementId, bpmnElementId)
        }
    }

    private fun emitStringUpdateWithCascadeIfNeeded(state: Map<BpmnElementId, PropertyTable>, event: StringValueUpdatedEvent) {
        val cascades = mutableListOf<Event>()
        if (null != event.referencedValue) {
            state.forEach { (id, props) ->
                props.filter { k, _ -> k.updatedBy == event.property }.filter { it.second.value == event.referencedValue }.forEach { prop ->
                    cascades += StringValueUpdatedEvent(id, prop.first, event.newValue, event.referencedValue, null)
                }
            }
        }
        if (event.property.indexCascades) {
            state[event.bpmnElementId]?.view()?.filter { it.key.indexInGroupArrayName == event.property.indexInGroupArrayName }?.forEach { (k, _) ->
                if (event.newValue.isBlank()) {
                    cascades += UiOnlyValueRemovedEvent(event.bpmnElementId, k, event.propertyIndex!!)
                }
                cascades += IndexUiOnlyValueUpdatedEvent(event.bpmnElementId, k, event.propertyIndex!!, event.newValue)
            }
        }

        updateEventsRegistry(project).addEvents(listOf(event) + cascades)
    }

    private fun stringValueUpdatedTemplate(
        bpmnElementId: BpmnElementId,
        type: PropertyType,
        value: String,
        initialValue: String,
        property: Property
    ) = StringValueUpdatedEvent(
        bpmnElementId,
        type,
        value,
        if (type.cascades) initialValue else null,
        if (type == PropertyType.ID) BpmnElementId(value) else null,
        property.index
    )

    private fun prepareTable(): FirstColumnReadOnlyModel {
        notifyDeFocusElement()
        // drop and re-create table model
        val model = FirstColumnReadOnlyModel()
        model.addColumn("")
        model.addColumn("")
        model.addColumn("")
        table.model = model
        table.rowSorter = null
        table.columnModel.getColumn(1).preferredWidth = 500
        table.columnModel.getColumn(2).preferredWidth = 20
        return model
    }

    private fun removeQuotes(value: String): String {
        return value.replace("^\"".toRegex(), "").replace("\"$".toRegex(), "")
    }

    private fun extractString(prop: Property?): String {
        return extractValue(prop, "")
    }

    private fun extractBoolean(prop: Property?): Boolean {
        return extractValue(prop, false)
    }

    private fun <T> extractValue(prop: Property?, defaultValue: T): T {
        if (prop == null) {
            return defaultValue
        }

        return prop.value as T? ?: defaultValue
    }
}

private class RowExpansionFilter(val collapsed: MutableSet<Any> = mutableSetOf()): RowFilter<FirstColumnReadOnlyModel, Any>() {

    override fun include(entry: Entry<out FirstColumnReadOnlyModel, out Any>?): Boolean {
        return !collapsed.contains(entry?.getValue(0))
    }
}
