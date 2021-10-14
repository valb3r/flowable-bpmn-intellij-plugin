package com.valb3r.bpmn.intellij.plugin.core.ui.components

import com.intellij.openapi.ui.ComboBox
import com.intellij.ui.EditorTextField
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBTextField
import com.intellij.ui.table.JBTable
import com.intellij.util.ui.AbstractTableCellEditor
import com.valb3r.bpmn.intellij.plugin.core.settings.currentSettings
import java.awt.Component
import java.awt.Font
import java.awt.event.ActionEvent
import javax.swing.*
import javax.swing.table.DefaultTableModel
import javax.swing.table.TableCellEditor
import javax.swing.table.TableCellRenderer
import javax.swing.table.TableModel
import kotlin.math.max


class MultiEditJTable(tableModel: TableModel): JBTable(tableModel) {
    private val intraCellSpacingWidth = 10

    init {
        attachShiftAndShiftTabActions()
    }

    override fun getCellEditor(row: Int, column: Int): TableCellEditor {
        return when (val value = modelValue(row, column)) {
            is EditorTextField -> EditorTextFieldCellEditor(value)
            is JBTextField -> JBTextFieldCellEditor(value)
            is JBCheckBox -> JBCheckBoxCellEditor(value)
            is JButton -> JButtonCellEditor(value)
            is ComboBox<*> -> JBComboBoxCellEditor(value)
            else -> super.getCellEditor(row, column)
        }
    }

    override fun getCellRenderer(row: Int, column: Int): TableCellRenderer {
        return when (val value = modelValue(row, column)) {
            is EditorTextField -> EditorTextFieldCellRenderer(value)
            is JBTextField -> JBTextFieldCellRenderer(value)
            is JBCheckBox -> JBCheckBoxCellRenderer(value)
            is JButton -> JButtonCellRenderer(value)
            is ComboBox<*> -> JBComboBoxCellRenderer(value)
            is String -> LabelTextFieldCellRenderer(value)
            else -> super.getCellRenderer(row, column)
        }
    }

    // Auto-resize
    override fun prepareRenderer(renderer: TableCellRenderer, row: Int, column: Int): Component {
        val component = super.prepareRenderer(renderer, row, column)
        val rendererWidth = component.preferredSize.width
        val tableColumn = getColumnModel().getColumn(column)
        tableColumn.preferredWidth = max(rendererWidth + intercellSpacing.width + intraCellSpacingWidth, tableColumn.preferredWidth)
        return component
    }

    private fun modelValue(row: Int, column: Int): Any? {
        val modelRow = convertRowIndexToModel(row)
        val modelColumn = convertColumnIndexToModel(column)
        return super.dataModel.getValueAt(modelRow, modelColumn)
    }

    private fun attachShiftAndShiftTabActions() {
        val nextRow = this.getInputMap(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).get(KeyStroke.getKeyStroke("TAB"))
        val prevRow = this.getInputMap(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).get(KeyStroke.getKeyStroke("shift pressed TAB"))
        if (null == nextRow || null == prevRow) {
            return
        }

        this.actionMap.put(nextRow, object : AbstractAction() {
            override fun actionPerformed(e: ActionEvent) {
                val newRow = if (selectedRow == rowCount - 1) 0 else selectedRow + 1
                changeSelection(newRow, selectedColumn, false, false)
                editCellAt(newRow, selectedColumn)
            }
        })
        this.actionMap.put(prevRow, object : AbstractAction() {
            override fun actionPerformed(e: ActionEvent) {
                val newRow = if (selectedRow == 0) rowCount - 1 else selectedRow - 1
                changeSelection(newRow, selectedColumn, false, false)
                editCellAt(newRow, selectedColumn)
            }
        })
    }
}

class EditorTextFieldCellEditor(val field: EditorTextField): AbstractTableCellEditor() {

    override fun getTableCellEditorComponent(table: JTable?, value: Any?, isSelected: Boolean, row: Int, column: Int): Component {
        return field
    }

    override fun getCellEditorValue(): Any {
        return field
    }
}

class JBTextFieldCellEditor(val field: JBTextField): AbstractTableCellEditor() {

    override fun getTableCellEditorComponent(table: JTable?, value: Any?, isSelected: Boolean, row: Int, column: Int): Component {
        return field
    }

    override fun getCellEditorValue(): Any {
        return field
    }
}

class JBCheckBoxCellEditor(val field: JBCheckBox): AbstractTableCellEditor() {

    override fun getTableCellEditorComponent(table: JTable?, value: Any?, isSelected: Boolean, row: Int, column: Int): Component {
        return field
    }

    override fun getCellEditorValue(): Any {
        return field
    }
}

class JButtonCellEditor(val field: JButton): AbstractTableCellEditor() {

    override fun getTableCellEditorComponent(table: JTable?, value: Any?, isSelected: Boolean, row: Int, column: Int): Component {
        return field
    }

    override fun getCellEditorValue(): Any {
        return field
    }
}

class JBComboBoxCellEditor(val field: ComboBox<*>): AbstractTableCellEditor() {

    override fun getTableCellEditorComponent(table: JTable?, value: Any?, isSelected: Boolean, row: Int, column: Int): Component {
        return field
    }

    override fun getCellEditorValue(): Any {
        return field
    }
}

class EditorTextFieldCellRenderer(val field: EditorTextField): TableCellRenderer {

    override fun getTableCellRendererComponent(
            table: JTable?, value: Any?, isSelected: Boolean, hasFocus: Boolean, row: Int, column: Int): Component {
        return field
    }
}

class JBTextFieldCellRenderer(val field: JBTextField): TableCellRenderer {

    override fun getTableCellRendererComponent(
            table: JTable?, value: Any?, isSelected: Boolean, hasFocus: Boolean, row: Int, column: Int): Component {
        return field
    }
}

class JBCheckBoxCellRenderer(val field: JBCheckBox): TableCellRenderer {

    override fun getTableCellRendererComponent(
            table: JTable?, value: Any?, isSelected: Boolean, hasFocus: Boolean, row: Int, column: Int): Component {
        return field
    }
}

class JButtonCellRenderer(val field: JButton): TableCellRenderer {

    override fun getTableCellRendererComponent(
        table: JTable?, value: Any?, isSelected: Boolean, hasFocus: Boolean, row: Int, column: Int): Component {
        return field
    }
}

class JBComboBoxCellRenderer(val field: ComboBox<*>): TableCellRenderer {

    override fun getTableCellRendererComponent(
            table: JTable?, value: Any?, isSelected: Boolean, hasFocus: Boolean, row: Int, column: Int): Component {
        return field
    }
}


class LabelTextFieldCellRenderer(val field: String): TableCellRenderer {

    override fun getTableCellRendererComponent(
            table: JTable?, value: Any?, isSelected: Boolean, hasFocus: Boolean, row: Int, column: Int): Component {
        val label = JLabel(field)
        label.font = Font(currentSettings().dataFontName, Font.BOLD, currentSettings().dataFontSize)
        return label
    }
}

class FirstLastColumnReadOnlyModel: DefaultTableModel() {

    override fun isCellEditable(row: Int, column: Int): Boolean {
        return column != 0 && (column != columnCount - 1 || getValueAt(row, column) is JButton)
    }
}
