package com.valb3r.bpmn.intellij.plugin

import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.fileTypes.StdFileTypes
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.ComboBox
import com.intellij.psi.*
import com.intellij.ui.EditorTextField
import com.intellij.ui.JavaReferenceEditorUtil
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextField
import com.valb3r.bpmn.intellij.plugin.bpmn.api.BpmnParser
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.BpmnElementId
import com.valb3r.bpmn.intellij.plugin.bpmn.api.info.PropertyType
import com.valb3r.bpmn.intellij.plugin.core.BpmnActionContext
import com.valb3r.bpmn.intellij.plugin.core.CanvasBuilder
import com.valb3r.bpmn.intellij.plugin.core.MouseEventHandler
import com.valb3r.bpmn.intellij.plugin.core.events.IntelliJFileCommitter
import com.valb3r.bpmn.intellij.plugin.core.properties.SelectedValueAccessor
import com.valb3r.bpmn.intellij.plugin.core.properties.TextValueAccessor
import com.valb3r.bpmn.intellij.plugin.core.render.Canvas
import com.valb3r.bpmn.intellij.plugin.core.render.DefaultBpmnProcessRenderer
import com.valb3r.bpmn.intellij.plugin.core.render.currentCanvas
import com.valb3r.bpmn.intellij.plugin.core.render.currentIconProvider
import com.valb3r.bpmn.intellij.plugin.core.ui.components.MultiEditJTable
import javax.swing.*
import javax.swing.table.DefaultTableModel


class BpmnPluginToolWindow(private val bpmnParser: BpmnParser, private val onFileOpenCallback: (PsiFile) -> Unit) {

    private lateinit var canvasAndProperties: JSplitPane
    private lateinit var propertiesPanel: JPanel
    private lateinit var mainToolWindowForm: JPanel
    private lateinit var canvasPanel: JPanel
    private lateinit var canvasVScroll: JScrollBar
    private lateinit var canvasHScroll: JScrollBar

    private val canvasBuilder = CanvasBuilder(DefaultBpmnProcessRenderer(currentIconProvider()))
    private val canvas: Canvas = currentCanvas()

    init {
        // attach event listeners to canvas
        val mouseEventHandler = MouseEventHandler(this.canvas)
        this.canvas.addMouseListener(mouseEventHandler)
        this.canvas.addMouseMotionListener(mouseEventHandler)
        this.canvas.addMouseWheelListener(mouseEventHandler)
        this.canvasPanel.add(this.canvas)
        canvasAndProperties.dividerLocation = (canvasAndProperties.height * 0.8f).toInt()
    }

    fun getContent() = this.mainToolWindowForm

    fun run(bpmnFile: PsiFile, context: BpmnActionContext) {
        val table = MultiEditJTable(DefaultTableModel())
        table.autoResizeMode = JTable.AUTO_RESIZE_OFF
        table.rowHeight = 24

        val scrollPane = JBScrollPane(table)
        propertiesPanel.removeAll()
        propertiesPanel.add(scrollPane)
        canvasAndProperties.dividerLocation = (canvasAndProperties.height * 0.8f).toInt()

        setupUiBeforeRun()
        val virtualFile = bpmnFile.virtualFile
        onFileOpenCallback(bpmnFile)
        this.canvasBuilder.build(
                { IntelliJFileCommitter(it, context.project, virtualFile) },
                bpmnParser,
                table,
                { _: BpmnElementId, _: PropertyType, value: String, allowableValues: Set<String> -> createDropdown(value, allowableValues) },
                { _: BpmnElementId, _: PropertyType, value: String -> createEditorForClass(context.project, bpmnFile, value) },
                { _: BpmnElementId, _: PropertyType, value: String -> createEditor(context.project, bpmnFile, value) },
                { _: BpmnElementId, _: PropertyType, value: String -> createTextField(value) },
                { _: BpmnElementId, _: PropertyType, value: Boolean -> createCheckboxField(value) },
                canvas,
                bpmnFile.project,
                virtualFile
        )
        setupUiAfterRun()
    }

    fun createTextField(value: String): TextValueAccessor {
        val textField = JBTextField(value)

        return object: TextValueAccessor {
            override val text: String
                get() = textField.text
            override val component: JComponent
                get() = textField
        }
    }

    fun createCheckboxField(value: Boolean): SelectedValueAccessor {
        val checkBox = JBCheckBox(null, value)

        return object: SelectedValueAccessor {
            override val isSelected: Boolean
                get() = checkBox.isSelected
            override val component: JComponent
                get() = checkBox
        }
    }

    fun createEditor(project: Project, bpmnFile: PsiFile, text: String): TextValueAccessor {
        val factory = JavaCodeFragmentFactory.getInstance(project)
        val fragment: JavaCodeFragment = factory.createExpressionCodeFragment(text, bpmnFile, PsiType.CHAR, true)
        fragment.visibilityChecker = JavaCodeFragment.VisibilityChecker.EVERYTHING_VISIBLE
        val document = PsiDocumentManager.getInstance(project).getDocument(fragment)!!

        val textField: EditorTextField = JavaEditorTextField(document, project)
        textField.setOneLineMode(true)

        return object: TextValueAccessor {
            override val text: String
                get() = textField.text
            override val component: JComponent
                get() = textField
        }
    }

    fun createDropdown(text: String, allowableValues: Set<String>): TextValueAccessor {
        val dropDownField = ComboBox(allowableValues.toTypedArray())
        dropDownField.selectedItem = text
        return object: TextValueAccessor {
            override val text: String
                get() = dropDownField.selectedItem as String
            override val component: JComponent
                get() = dropDownField
        }
    }

    // JavaCodeFragmentFactory - important
    protected fun createEditorForClass(project: Project, bpmnFile: PsiFile, text: String?): TextValueAccessor {
        val document = JavaReferenceEditorUtil.createDocument(text, project, true)!!
        val textField: EditorTextField = JavaEditorTextField(document, project)
        textField.setOneLineMode(true)

        return object: TextValueAccessor {
            override val text: String
                get() = textField.text
            override val component: JComponent
                get() = textField
        }
    }

    private fun setupUiBeforeRun() {
        this.canvasPanel.isEnabled = false
        // clear the canvas panel, ready for new graph
        this.canvas.isVisible = false
    }

    private fun setupUiAfterRun() {
        // show the rendered canvas
        this.canvas.isVisible = true
        this.canvasPanel.updateUI()
        this.canvasPanel.isEnabled = true
    }

    class JavaEditorTextField(document: Document, project: Project): EditorTextField(document, project, StdFileTypes.JAVA) {
        override fun createEditor(): EditorEx {
            val editorEx: EditorEx = super.createEditor()
            return editorEx
        }
    }
}