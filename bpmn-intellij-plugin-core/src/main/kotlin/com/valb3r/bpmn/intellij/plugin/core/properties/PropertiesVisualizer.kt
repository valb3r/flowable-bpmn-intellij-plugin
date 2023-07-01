package com.valb3r.bpmn.intellij.plugin.core.properties

import com.intellij.database.util.toFixString
import com.intellij.openapi.project.Project
import com.valb3r.bpmn.intellij.plugin.bpmn.api.PropertyTable
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.BpmnElementId
import com.valb3r.bpmn.intellij.plugin.bpmn.api.events.Event
import com.valb3r.bpmn.intellij.plugin.bpmn.api.info.FunctionalGroupType
import com.valb3r.bpmn.intellij.plugin.bpmn.api.info.Property
import com.valb3r.bpmn.intellij.plugin.bpmn.api.info.PropertyType
import com.valb3r.bpmn.intellij.plugin.bpmn.api.info.PropertyValueType.*
import com.valb3r.bpmn.intellij.plugin.core.events.BooleanValueUpdatedEvent
import com.valb3r.bpmn.intellij.plugin.core.events.StringValueUpdatedEvent
import com.valb3r.bpmn.intellij.plugin.core.events.UiOnlyValueAddedEvent
import com.valb3r.bpmn.intellij.plugin.core.events.updateEventsRegistry
import com.valb3r.bpmn.intellij.plugin.core.newelements.NewElementsProvider
import com.valb3r.bpmn.intellij.plugin.core.newelements.newElementsFactory
import com.valb3r.bpmn.intellij.plugin.core.state.currentStateProvider
import com.valb3r.bpmn.intellij.plugin.core.ui.components.FirstLastColumnReadOnlyModel
import org.jetbrains.annotations.VisibleForTesting
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
                            multiLineExpandableTextFieldFactory: (id: BpmnElementId, type: PropertyType, value: String) -> TextValueAccessor,
                            checkboxFieldFactory: (id: BpmnElementId, type: PropertyType, value: Boolean) -> SelectedValueAccessor,
                            buttonFactory: (id: BpmnElementId, type: FunctionalGroupType) -> JButton,
                            arrowButtonFactory: (id: BpmnElementId) -> BasicArrowButton): PropertiesVisualizer {
    val newVisualizer = PropertiesVisualizer(project, table, dropDownFactory, classEditorFactory, editorFactory, textFieldFactory, multiLineExpandableTextFieldFactory, checkboxFieldFactory, buttonFactory, arrowButtonFactory)
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
        private val multiLineExpandableTextFieldFactory: (id: BpmnElementId, type: PropertyType, value: String) -> TextValueAccessor,
        private val checkboxFieldFactory: (id: BpmnElementId, type: PropertyType, value: Boolean) -> SelectedValueAccessor,
        private val buttonFactory: (id: BpmnElementId, type: FunctionalGroupType) -> JButton,
        private val arrowButtonFactory: (id: BpmnElementId) -> BasicArrowButton) {

    // Using order as ID property change should fire last for this view, otherwise other property change values
    // will use wrong ID as an anchor
    // Listeners with their order
    private var listenersForCurrentView: MutableMap<Int, MutableList<() -> Unit>> = mutableMapOf()

    private val expandedElems: MutableSet<ElementIndex> = mutableSetOf()

    @Synchronized
    fun clear() {
        expandedElems.clear()
        val model = prepareTable()
        model.fireTableDataChanged()
    }

    @Synchronized
    fun visualize(newElemsProvider: NewElementsProvider, state: Map<BpmnElementId, PropertyTable>, bpmnElementId: BpmnElementId, elemsToExpand: Set<ElementIndex> = emptySet()) {
        expandedElems.clear()
        val model = prepareTable()
        val filter = RowExpansionFilter()
        val sorter = TableRowSorter(table.model)

        val orderedControls = state[bpmnElementId]?.view()?.entries
            ?.filter { it.key.visible }
            ?.flatMap { it.value.map { v -> Pair(it.key, v) } }
            ?.sortedBy { computePropertyKey(it) } ?: listOf()

        val restoreVisualState = createControls(newElemsProvider, model, state, bpmnElementId, orderedControls, filter, sorter, elemsToExpand)

        filter.build()
        sorter.rowFilter = filter
        table.rowSorter = sorter
        model.fireTableDataChanged()
        restoreVisualState.forEach { it.doClick() }
    }

    private fun createControls(
        newElemsProvider: NewElementsProvider,
        model: FirstLastColumnReadOnlyModel,
        state: Map<BpmnElementId, PropertyTable>,
        bpmnElementId: BpmnElementId,
        controls: List<Pair<PropertyType, Property>>,
        filter: RowExpansionFilter,
        sorter: TableRowSorter<TableModel>,
        elemsToExpand: Set<ElementIndex>): Set<BasicArrowButton> {
        val seenIndexes = mutableSetOf<ElementIndex>()
        val buttonsToClick = mutableSetOf<BasicArrowButton>()
        for (control in controls) {
            if (control.first.caption.isEmpty()) {
                continue
            }
            val groupType = control.first.group?.lastOrNull()
            val isExpandButton = control.first.name == groupType?.actionResult?.propertyType
            val isAlwaysVisible = control.first.group?.size == 1 && isExpandButton
            val controlGroupIndex = ElementIndex(
                if (isExpandButton && control.first.isNestedProperty()) control.first.group?.getOrNull(control.first.group!!.size - 2) else groupType,
                control.second.index?.take(max(0, control.first.group!!.size - if (isExpandButton) 1 else 0))?.joinToString() ?: ""
            )
            val lengthInnerPad = control.second.index?.let {(it.size - 1) * 2} ?: 0
            val paddGroup = "".padStart(lengthInnerPad * 2)
            if (null != groupType && isExpandButton && !seenIndexes.contains(controlGroupIndex) && groupType.createExpansionButton) {
                addCurrentRowToCollapsedSectionIfNeeded(controlGroupIndex, filter, model, isAlwaysVisible)
                model.addRow(arrayOf(
                    paddGroup + groupType.groupCaption,
                    buildButtonField(newElemsProvider, state, bpmnElementId, groupType, control.second.index?.dropLast(1) ?: listOf())
                ))
                seenIndexes.add(controlGroupIndex)
            }

            if (control.first.hideIfNullOrEmpty && (null == control.second.value || (control.second.value is String && (control.second.value as String).isBlank()))) {
                continue
            }
            val nestedGroupLength = (2 + (if(!isExpandButton) 2 else 0) - (if (groupType?.createExpansionButton == false) 2 else 0))
            val padd = paddGroup + "".padStart(if(groupType == null) 0 else nestedGroupLength)
            val caption = padd + control.first.caption
            var row = when (control.first.valueType) {
                STRING -> arrayOf(caption, buildTextField(state, bpmnElementId, control.first, control.second))
                BOOLEAN -> arrayOf(caption, buildCheckboxField(state, bpmnElementId, control.first, control.second))
                CLASS -> arrayOf(caption, buildClassField(state, bpmnElementId, control.first, control.second))
                EXPRESSION -> arrayOf(caption, buildExpressionField(state, bpmnElementId, control.first, control.second))
                ATTACHED_SEQUENCE_SELECT -> arrayOf(caption, buildDropDownSelectFieldForTargettedIds(state, bpmnElementId, control.first, control.second))
                LIST_SELECT -> arrayOf(caption, buildDropDownSelect(state, bpmnElementId, control.first, control.second))
            }

            if (isExpandButton) {
                val controlExpandsGroupIndex = ElementIndex(
                    groupType,
                    control.second.index?.joinToString() ?: ""
                )
                val button = buildArrowExpansionButton(bpmnElementId, filter, controlExpandsGroupIndex, sorter)
                row += button
                if (elemsToExpand.contains(controlExpandsGroupIndex)) {
                    buttonsToClick.add(button)
                }
                filter.setCollapseGroupControl(controlGroupIndex, controlExpandsGroupIndex, button)
            }

            addCurrentRowToCollapsedSectionIfNeeded(controlGroupIndex, filter, model, isAlwaysVisible)
            model.addRow(row)
        }

        return buttonsToClick
    }

    private fun addCurrentRowToCollapsedSectionIfNeeded(
        controlGroupIndex: ElementIndex,
        filter: RowExpansionFilter,
        model: FirstLastColumnReadOnlyModel,
        isAlwaysVisible: Boolean = false
    ) {
        if (null == controlGroupIndex.type || isAlwaysVisible) return
        filter.addControl(controlGroupIndex, model.rowCount)
    }

    private fun computePropertyKey(entry: Pair<PropertyType, Property>): String {
        return entry.first.group?.mapIndexed { index, type ->
            type.name + entry.second.index?.getOrElse(index) {""} + entry.first.positionInGroup.toFixString(4)
        }?.joinToString() ?: ""
    }

    private fun notifyDeFocusElement() {
        // Fire de-focus to move changes to memory (Using order as ID property), component listeners doesn't seem to work with EditorTextField
        listenersForCurrentView.toSortedMap().flatMap { it.value }.forEach { it() }
        listenersForCurrentView.clear()
    }

    private fun buildTextField(state: Map<BpmnElementId, PropertyTable>, bpmnElementId: BpmnElementId, type: PropertyType, value: Property): JComponent {
        val fieldValue = extractString(value)
        val field = if (type.multiline) multiLineExpandableTextFieldFactory.invoke(bpmnElementId, type, fieldValue) else textFieldFactory.invoke(bpmnElementId, type, fieldValue)
        val initialValue = field.text
        listenersForCurrentView.computeIfAbsent(type.updateOrder) { mutableListOf()}.add {
            if (initialValue != field.text) {
                emitStringUpdateWithCascadeIfNeeded(
                    state,
                    stringValueUpdatedTemplate(bpmnElementId, type, field.text, initialValue, value),
                    project
                )
            }
        }

        return field.component
    }

    private fun buildCheckboxField(state: Map<BpmnElementId, PropertyTable>, bpmnElementId: BpmnElementId, type: PropertyType, value: Property): JComponent {
        val fieldValue = extractBoolean(value)
        val field = checkboxFieldFactory.invoke(bpmnElementId, type, fieldValue)
        val initialValue = field.isSelected

        listenersForCurrentView.computeIfAbsent(type.updateOrder) { mutableListOf()}.add {
            if (initialValue != field.isSelected) {
                handleBooleanValueUpdate(state, bpmnElementId, type, field.isSelected)
            }
        }
        return field.component
    }

    private fun handleBooleanValueUpdate(state: Map<BpmnElementId, PropertyTable>, bpmnElementId: BpmnElementId, type: PropertyType, value: Boolean) {
        val elementValueUpdate = BooleanValueUpdatedEvent(bpmnElementId, type, value)
        if (null == type.externalProperty) {
            updateEventsRegistry(project).addPropertyUpdateEvent(elementValueUpdate)
            return
        }

        val externalProp = type.externalProperty!!
        val elemProps = state[bpmnElementId]!!
        val referencedValue = externalProp.externalValueReference(elemProps)
        val updatedValue = externalProp.castToExternalValue(elemProps, value)
        if (null == referencedValue) {
            updateEventsRegistry(project).addPropertyUpdateEvent(elementValueUpdate)
            return
        }
        // TODO: No type check currently, hardcoded to String
        updateEventsRegistry(project).addEvents(listOf(elementValueUpdate, StringValueUpdatedEvent(referencedValue.first, referencedValue.second, updatedValue as String? ?: "")))
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

    private fun buildDropDownSelect(state: Map<BpmnElementId, PropertyTable>, bpmnElementId: BpmnElementId, type: PropertyType, value: Property): JComponent {
        val fieldValue = extractString(value)
        val field = dropDownFactory(bpmnElementId, type, fieldValue, type.setForSelect!!)
        addEditorTextListener(state, field, bpmnElementId, type, value)
        return field.component
    }

    private fun buildButtonField(newElemsProvider: NewElementsProvider, state: Map<BpmnElementId, PropertyTable>, bpmnElementId: BpmnElementId, type: FunctionalGroupType, parentIndex: List<String>): JComponent {
        val button = buttonFactory(bpmnElementId, type)
        addButtonListener(newElemsProvider, state, button, bpmnElementId, type, parentIndex)
        return button
    }

    private fun buildArrowExpansionButton(bpmnElementId: BpmnElementId, filter: RowExpansionFilter, identifier: ElementIndex, sorter: TableRowSorter<TableModel>): BasicArrowButton {
        val button = arrowButtonFactory(bpmnElementId)
       button.addActionListener {
            val isExpanded = button.isExpanded()
           if (isExpanded) {
                button.direction = arrowButtonDirection(true)
                filter.collapse(identifier)
                sorter.sort()
            } else {
                button.direction = arrowButtonDirection(false)
                expandedElems.add(identifier)
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
                    stringValueUpdatedTemplate(bpmnElementId, type,  removeQuotes(field.text), initialValue, value),
                    project
                )
            }
        }
    }

    private fun addButtonListener(newElemsProvider: NewElementsProvider, state: Map<BpmnElementId, PropertyTable>, field: JButton, bpmnElementId: BpmnElementId, type: FunctionalGroupType, parentIndex: List<String>) {
        fun propertyType(name: String) = PropertyType.values().find { it.name == name }!!

        field.addActionListener {
            val propType = propertyType(type.actionResult.propertyType)
            val allPropsOfType = state[bpmnElementId]!!.getAll(propType).map { it.index?.joinToString() }.toSet()
            val countFields = allPropsOfType.size
            val fieldName = (countFields..maxFields).map { type.actionResult.valuePattern.format(it) }.firstOrNull { !allPropsOfType.contains(it) } ?: UUID.randomUUID().toString()
            val propertyIndex = parentIndex + fieldName
            val events = mutableListOf<Event>(StringValueUpdatedEvent(bpmnElementId, propType, fieldName, propertyIndex = propertyIndex))
            val supportedTypes = newElemsProvider.propertyTypes().map { it.name }.toSet()
            events += type.actionUiOnlyResult.filter { supportedTypes.contains(it.propertyType) }.map { UiOnlyValueAddedEvent(bpmnElementId, propertyType(it.propertyType), it.valuePattern, propertyIndex = propertyIndex + it.uiOnlyaddedIndex) }
            updateEventsRegistry(project).addEvents(events)
            visualize(newElementsFactory(project), currentStateProvider(project).currentState().elemPropertiesByStaticElementId, bpmnElementId, expandedElems.toSet())
        }
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

public class RowExpansionFilter: RowFilter<TableModel, Any>() {

    private val groups: MutableMap<ElementIndex, MutableSet<Int>> = mutableMapOf()
    private val collapsed: MutableSet<Int> = mutableSetOf()
    private val inverseGroups: MutableMap<Int, ElementIndex> = mutableMapOf()
    private val collapseControls: MutableMap<ElementIndex, MutableSet<Pair<BasicArrowButton, ElementIndex>>> = mutableMapOf()

    @VisibleForTesting
    fun getCollapsed(): MutableSet<Int> {
        return collapsed
    }

    fun addControl(control: ElementIndex, rowIndex: Int) {
        groups.computeIfAbsent(control) { mutableSetOf() }.add(rowIndex)
    }

    fun setCollapseGroupControl(control: ElementIndex, group: ElementIndex, button: BasicArrowButton) {
        collapseControls.computeIfAbsent(control) { mutableSetOf() }.add(Pair(button, group))
    }

    fun build() {
        collapsed.addAll(groups.values.flatten())
        groups.forEach { (k, v) -> v.forEach { inverseGroups[it] = k }}
    }

    override fun include(entry: Entry<out TableModel, out Any>?): Boolean {
        return !collapsed.contains(entry?.identifier)
    }

    fun expand(group: ElementIndex) {
        collapsed.removeAll((groups[group] ?: mutableSetOf()).toSet())
    }

    fun collapse(group: ElementIndex) {
        collapse(group, mutableSetOf())
    }

    private fun collapse(group: ElementIndex, seen: MutableSet<ElementIndex>) {
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

private fun BasicArrowButton.isExpanded() = SwingConstants.NORTH == this.direction

private fun arrowButtonDirection(isCollapsed: Boolean) = if (isCollapsed) SwingConstants.SOUTH else SwingConstants.NORTH

data class ElementIndex(val type: FunctionalGroupType?, val index: String)