package com.valb3r.bpmn.intellij.plugin

import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.fileTypes.StdFileTypes
import com.intellij.openapi.project.Project
import com.intellij.structuralsearch.StructuralSearchProfile
import com.intellij.structuralsearch.StructuralSearchUtil
import com.intellij.structuralsearch.plugin.ui.StructuralSearchDialog.STRUCTURAL_SEARCH
import com.intellij.ui.EditorTextField
import com.intellij.ui.components.JBScrollPane
import com.intellij.util.textCompletion.TextCompletionUtil
import java.io.File
import javax.swing.JPanel
import javax.swing.JSplitPane


class BpmnPluginToolWindow {

    private lateinit var canvasAndProperties: JSplitPane
    private lateinit var propertiesPanel: JPanel
    private lateinit var mainToolWindowForm: JPanel
    private lateinit var canvasPanel: JPanel

    private val canvasBuilder = CanvasBuilder()
    private val canvas: Canvas = Canvas(this)

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

    fun run(bpmnFile: File, context: BpmnActionContext) {
        val myInput = createEditor(context.project, "")
        val model = FirstColumnReadOnlyModel()
        model.addColumn("")
        model.addColumn("")
        model.addRow(arrayOf("EditableProperty", myInput))
        val table = MultiEditJTable(model)
        table.rowHeight = 20
        val scrollPane = JBScrollPane(table)
        propertiesPanel.removeAll()
        propertiesPanel.add(scrollPane)
        canvasAndProperties.dividerLocation = (canvasAndProperties.height * 0.8f).toInt()

        setupUiBeforeRun()
        this@BpmnPluginToolWindow.canvasBuilder.build(canvas, bpmnFile)
        setupUiAfterRun()
    }

    protected fun createEditor(project: Project, text: String?): EditorTextField {
        val type = StdFileTypes.JAVA
        val profile: StructuralSearchProfile = StructuralSearchUtil.getProfileByFileType(type)!!
        val document: Document = profile.createDocument(project, type, null, text)
        /**document.addDocumentListener(object : DocumentListener() {
            fun documentChanged(event: DocumentEvent?) {
                initiateValidation()
            }
        })**/
        val textField: EditorTextField = object : EditorTextField(document, project, type) {
            override fun createEditor(): EditorEx {
                val editorEx: EditorEx = super.createEditor()
                TextCompletionUtil.installCompletionHint(editorEx)
                editorEx.putUserData(STRUCTURAL_SEARCH, null)
                return editorEx
            }
        }
        return textField
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
}