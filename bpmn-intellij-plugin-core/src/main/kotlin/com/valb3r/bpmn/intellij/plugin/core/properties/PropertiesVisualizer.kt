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
import com.valb3r.bpmn.intellij.plugin.core.ui.components.FirstLastColumnReadOnlyModel
import java.util.*
import javax.swing.*
import javax.swing.plaf.basic.BasicArrowButton
import javax.swing.table.TableModel
import javax.swing.table.TableRowSorter
import kotlin.math.max

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
                            buttonFactory: (id: BpmnElementId, type: FunctionalGroupType) -> JButton,
                            arrowButtonFactory: (id: BpmnElementId) -> BasicArrowButton): PropertiesVisualizer {
    val newVisualizer = PropertiesVisualizer(project, table, dropDownFactory, classEditorFactory, editorFactory, textFieldFactory, checkboxFieldFactory, buttonFactory, arrowButtonFactory)
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
        private val buttonFactory: (id: BpmnElementId, type: FunctionalGroupType) -> JButton,
        private val arrowButtonFactory: (id: BpmnElementId) -> BasicArrowButton) {

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
        val sorter = TableRowSorter(table.model)

        val orderedControls = state[bpmnElementId]?.view()?.entries
            ?.flatMap { it.value.map { v -> Pair(it.key, v) } }
            ?.sortedBy { computePropertyKey(it) } ?: listOf()

        createControls(model, state, bpmnElementId, orderedControls, filter, sorter)

        filter.build()
        sorter.rowFilter = filter
        table.rowSorter = sorter
        model.fireTableDataChanged()
    }

    private fun createControls(
        model: FirstLastColumnReadOnlyModel,
        state: Map<BpmnElementId, PropertyTable>,
        bpmnElementId: BpmnElementId,
        controls: List<Pair<PropertyType, Property>>,
        filter: RowExpansionFilter,
        sorter: TableRowSorter<TableModel>) {
        val seenIndexes = mutableSetOf<CollapsedIndex>()
        for (control in controls) {
            val groupType = control.first.group?.lastOrNull()
            val isExpandButton = control.first.name == groupType?.actionResult?.propertyType
            val controlGroupIndex = CollapsedIndex(
                if (isExpandButton) control.first.group?.getOrNull(control.first.group!!.size - 2) else groupType,
                control.second.index?.take(max(0, control.first.group!!.size - if (isExpandButton) 1 else 0))?.joinToString() ?: ""
            )

            if (null != groupType && isExpandButton && !seenIndexes.contains(controlGroupIndex)) {
                addCurrentRowToCollapsedSectionIfNeeded(controlGroupIndex, filter, model)
                model.addRow(arrayOf("", groupType.groupCaption))
                addCurrentRowToCollapsedSectionIfNeeded(controlGroupIndex, filter, model)
                model.addRow(arrayOf("", buildButtonField(state, bpmnElementId, groupType)))
                seenIndexes.add(controlGroupIndex)
            }

            if (control.first.hideIfNullOrEmpty && (null == control.second.value || (control.second.value is String && (control.second.value as String).isBlank()))) {
                continue
            }

            var row = when (control.first.valueType) {
                STRING -> arrayOf(control.first.caption, buildTextField(state, bpmnElementId, control.first, control.second))
                BOOLEAN -> arrayOf(control.first.caption, buildCheckboxField(bpmnElementId, control.first, control.second))
                CLASS -> arrayOf(control.first.caption, buildClassField(state, bpmnElementId, control.first, control.second))
                EXPRESSION -> arrayOf(control.first.caption, buildExpressionField(state, bpmnElementId, control.first, control.second))
                ATTACHED_SEQUENCE_SELECT -> arrayOf(control.first.caption, buildDropDownSelectFieldForTargettedIds(state, bpmnElementId, control.first, control.second))
            }

            if (isExpandButton) {
                val controlExpandsGroupIndex = CollapsedIndex(
                    groupType,
                    control.second.index?.joinToString() ?: ""
                )
                val button = buildArrowExpansionButton(bpmnElementId, filter, controlExpandsGroupIndex, sorter)
                row += button
                filter.setCollapseGroupControl(controlGroupIndex, controlExpandsGroupIndex, button)
            }

            addCurrentRowToCollapsedSectionIfNeeded(controlGroupIndex, filter, model)
            model.addRow(row)
        }
    }

    private fun addCurrentRowToCollapsedSectionIfNeeded(
        controlGroupIndex: CollapsedIndex,
        filter: RowExpansionFilter,
        model: FirstLastColumnReadOnlyModel
    ) {
        if (null != controlGroupIndex.type) {
            filter.addControl(controlGroupIndex, model.rowCount)
        }
    }

    private fun computePropertyKey(entry: Pair<PropertyType, Property>): String {
        return entry.first.group?.mapIndexed { index, type -> type.name + entry.second.index?.getOrElse(index) {""} }?.joinToString() ?: ""
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

    private fun buildArrowExpansionButton(bpmnElementId: BpmnElementId, filter: RowExpansionFilter, identifier: CollapsedIndex, sorter: TableRowSorter<TableModel>): BasicArrowButton {
        val button = arrowButtonFactory(bpmnElementId)
        button.addActionListener {
            val isExpanded = SwingConstants.NORTH == button.direction
            if (isExpanded) {
                button.direction = arrowButtonDirection(true)
                filter.collapse(identifier)
                sorter.sort()
            } else {
                button.direction = arrowButtonDirection(false)
                filter.expand(identifier)
                sorter.sort()
            }
        }
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
            val allPropsOfType = state[bpmnElementId]!!.getAll(propType).map { it.index?.joinToString() }.toSet()
            val countFields = allPropsOfType.size
            val fieldName = (countFields..maxFields).map { type.actionResult.valuePattern.format(it) }.firstOrNull { !allPropsOfType.contains(it) } ?: UUID.randomUUID().toString()
            val events = mutableListOf<Event>(StringValueUpdatedEvent(bpmnElementId, propType, fieldName, propertyIndex = listOf(fieldName)))
            events += type.actionUiOnlyResult.map { UiOnlyValueAddedEvent(bpmnElementId, propertyType(it.propertyType), it.valuePattern, propertyIndex = listOf(fieldName)) }
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
                cascades += IndexUiOnlyValueUpdatedEvent(event.bpmnElementId, k, event.propertyIndex!!, event.propertyIndex.dropLast(1) + event.newValue)
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

    private fun prepareTable(): FirstLastColumnReadOnlyModel {
        notifyDeFocusElement()
        // drop and re-create table model
        val model = FirstLastColumnReadOnlyModel()
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

private class RowExpansionFilter: RowFilter<TableModel, Any>() {

    private val groups: MutableMap<CollapsedIndex, MutableSet<Int>> = mutableMapOf()
    private val collapsed: MutableSet<Int> = mutableSetOf()
    private val inverseGroups: MutableMap<Int, CollapsedIndex> = mutableMapOf()
    private val collapseControls: MutableMap<CollapsedIndex, MutableSet<Pair<BasicArrowButton, CollapsedIndex>>> = mutableMapOf()

    fun addControl(control: CollapsedIndex, rowIndex: Int) {
        groups.computeIfAbsent(control) { mutableSetOf() }.add(rowIndex)
    }

    fun setCollapseGroupControl(control: CollapsedIndex, group: CollapsedIndex, button: BasicArrowButton) {
        collapseControls.computeIfAbsent(control) { mutableSetOf() }.add(Pair(button, group))
    }

    fun build() {
        collapsed.addAll(groups.values.flatten())
        groups.forEach { (k, v) -> v.forEach { inverseGroups[it] = k }}
    }

    override fun include(entry: Entry<out TableModel, out Any>?): Boolean {
        return !collapsed.contains(entry?.identifier)
    }

    fun expand(group: CollapsedIndex) {
        collapsed.removeAll((groups[group] ?: mutableSetOf()).toSet())
    }

    fun collapse(group: CollapsedIndex) {
        collapse(group, mutableSetOf())
    }

    private fun collapse(group: CollapsedIndex, seen: MutableSet<CollapsedIndex>) {
        val collapseTargets = (groups[group] ?: mutableSetOf()).toSet()
        val nestedCollapse = collapseTargets
            .asSequence()
            .mapNotNull { inverseGroups[it] }
            .mapNotNull { collapseControls[it] }
            .flatten()
            .toSet()

        collapsed.addAll(collapseTargets)
        seen += group
        nestedCollapse.forEach { it.first.direction = arrowButtonDirection(true)}
        nestedCollapse.map { it.second }.filter { !seen.contains(it) }.forEach { collapse(it, seen) }
    }
}

private fun arrowButtonDirection(isCollapsed: Boolean) = if (isCollapsed) SwingConstants.SOUTH else SwingConstants.NORTH

private data class CollapsedIndex(val type: FunctionalGroupType?, val index: String)
