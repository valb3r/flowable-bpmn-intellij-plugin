package com.valb3r.bpmn.intellij.plugin.ui.components

import com.intellij.openapi.ui.ComboBox
import com.intellij.ui.EditorTextField
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBTextField
import com.intellij.ui.table.JBTable
import com.intellij.util.ui.AbstractTableCellEditor
import java.awt.Component
import java.awt.Font
import javax.swing.JLabel
import javax.swing.JTable
import javax.swing.table.DefaultTableModel
import javax.swing.table.TableCellEditor
import javax.swing.table.TableCellRenderer
import javax.swing.table.TableModel
import kotlin.math.max

class MultiEditJTable(tableModel: TableModel): JBTable(tableModel) {
    val intraCellSpacingWidth = 10

    override fun getCellEditor(row: Int, column: Int): TableCellEditor {
        val value = modelValue(row, column)

        return when (value) {
            is EditorTextField -> EditorTextFieldCellEditor(value)
            is JBTextField -> JBTextFieldCellEditor(value)
            is JBCheckBox -> JBCheckBoxCellEditor(value)
            is ComboBox<*> -> JBComboBoxCellEditor(value)
            else -> super.getCellEditor(row, column)
        }
    }

    override fun getCellRenderer(row: Int, column: Int): TableCellRenderer {
        val value = modelValue(row, column)

        return when (value) {
            is EditorTextField -> EditorTextFieldCellRenderer(value)
            is JBTextField -> JBTextFieldCellRenderer(value)
            is JBCheckBox -> JBCheckBoxCellRenderer(value)
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
        tableColumn.preferredWidth = max(
                rendererWidth + intercellSpacing.width + intraCellSpacingWidth,
                tableColumn.preferredWidth
        )
        return component
    }

    private fun modelValue(row: Int, column: Int): Any? {
        val modelRow = convertRowIndexToModel(row)
        val modelColumn = convertColumnIndexToModel(column)
        return super.dataModel.getValueAt(modelRow, modelColumn)
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
        label.font = Font("Consolas", Font.BOLD, 12);
        return label
    }
}

class FirstColumnReadOnlyModel: DefaultTableModel() {

    override fun isCellEditable(row: Int, column: Int): Boolean {
        return column != 0
    }
}