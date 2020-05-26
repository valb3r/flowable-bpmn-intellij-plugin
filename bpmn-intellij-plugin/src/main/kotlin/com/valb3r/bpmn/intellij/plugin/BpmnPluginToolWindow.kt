package com.valb3r.bpmn.intellij.plugin

import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.fileTypes.StdFileTypes
import com.intellij.openapi.project.Project
import com.intellij.psi.JavaCodeFragment
import com.intellij.psi.JavaCodeFragmentFactory
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiFile
import com.intellij.ui.EditorTextField
import com.intellij.ui.JavaReferenceEditorUtil
import com.intellij.ui.components.JBScrollPane
import com.valb3r.bpmn.intellij.plugin.flowable.parser.FlowableParser
import com.valb3r.bpmn.intellij.plugin.render.Canvas
import com.valb3r.bpmn.intellij.plugin.render.DefaultBpmnProcessRenderer
import com.valb3r.bpmn.intellij.plugin.render.IconProviderImpl
import com.valb3r.bpmn.intellij.plugin.ui.components.MultiEditJTable
import javax.swing.JPanel
import javax.swing.JSplitPane
import javax.swing.JTable
import javax.swing.table.DefaultTableModel


class BpmnPluginToolWindow {

    private lateinit var canvasAndProperties: JSplitPane
    private lateinit var propertiesPanel: JPanel
    private lateinit var mainToolWindowForm: JPanel
    private lateinit var canvasPanel: JPanel

    private val canvasBuilder = CanvasBuilder(DefaultBpmnProcessRenderer(IconProviderImpl()))
    private val canvas: Canvas = Canvas()

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
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF)
        table.rowHeight = 24

        val scrollPane = JBScrollPane(table)
        propertiesPanel.removeAll()
        propertiesPanel.add(scrollPane)
        canvasAndProperties.dividerLocation = (canvasAndProperties.height * 0.8f).toInt()

        setupUiBeforeRun()
        val virtualFile = bpmnFile.virtualFile
        this.canvasBuilder.build(
                FlowableParser(),
                table,
                { createEditor(context.project, bpmnFile, it) },
                canvas,
                bpmnFile.project,
                virtualFile
        )
        setupUiAfterRun()
    }

    protected fun createEditor(project: Project, bpmnFile: PsiFile, text: String): EditorTextField {
        val factory = JavaCodeFragmentFactory.getInstance(project)
        val fragment: JavaCodeFragment = factory.createCodeBlockCodeFragment(text, bpmnFile, true)
        fragment.visibilityChecker = JavaCodeFragment.VisibilityChecker.EVERYTHING_VISIBLE
        val document = PsiDocumentManager.getInstance(project).getDocument(fragment)!!

        val textField: EditorTextField = JavaEditorTextField(document, project)
        textField.setOneLineMode(true)
        return textField
    }

    // JavaCodeFragmentFactory - important
    protected fun createEditorForClass(project: Project, bpmnFile: PsiFile, text: String?): EditorTextField {
        val type = StdFileTypes.JAVA
        val document = JavaReferenceEditorUtil.createDocument("", project, true)

        val textField: EditorTextField = object : EditorTextField(document, project, type) {
            override fun createEditor(): EditorEx {
                val editorEx: EditorEx = super.createEditor()
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

    class JavaEditorTextField(document: Document, project: Project): EditorTextField(document, project, StdFileTypes.JAVA) {
        override fun createEditor(): EditorEx {
            val editorEx: EditorEx = super.createEditor()
            return editorEx
        }
    }
}