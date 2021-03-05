package com.valb3r.bpmn.intellij.plugin.core

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.ScrollType
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.fileTypes.StdFileTypes
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.wm.IdeFocusManager
import com.intellij.psi.*
import com.intellij.ui.EditorTextField
import com.intellij.ui.JavaReferenceEditorUtil
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextField
import com.valb3r.bpmn.intellij.plugin.bpmn.api.BpmnParser
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.BpmnElementId
import com.valb3r.bpmn.intellij.plugin.bpmn.api.info.PropertyType
import com.valb3r.bpmn.intellij.plugin.core.events.IntelliJFileCommitter
import com.valb3r.bpmn.intellij.plugin.core.properties.SelectedValueAccessor
import com.valb3r.bpmn.intellij.plugin.core.properties.TextValueAccessor
import com.valb3r.bpmn.intellij.plugin.core.render.Canvas
import com.valb3r.bpmn.intellij.plugin.core.render.DefaultBpmnProcessRenderer
import com.valb3r.bpmn.intellij.plugin.core.render.currentCanvas
import com.valb3r.bpmn.intellij.plugin.core.render.currentIconProvider
import com.valb3r.bpmn.intellij.plugin.core.render.uieventbus.ViewRectangleChangeEvent
import com.valb3r.bpmn.intellij.plugin.core.render.uieventbus.currentUiEventBus
import com.valb3r.bpmn.intellij.plugin.core.ui.components.MultiEditJTable
import java.awt.event.AdjustmentEvent
import java.awt.event.AdjustmentListener
import java.awt.geom.Point2D
import java.awt.geom.Rectangle2D
import javax.swing.*
import javax.swing.table.DefaultTableModel
import kotlin.math.abs


class BpmnPluginToolWindow(private val bpmnParser: BpmnParser, private val onFileOpenCallback: (PsiFile) -> Unit) {

    private val log = Logger.getInstance(BpmnPluginToolWindow::class.java)

    private lateinit var canvasAndProperties: JSplitPane
    private lateinit var propertiesPanel: JPanel
    private lateinit var mainToolWindowForm: JPanel
    private lateinit var canvasPanel: JPanel
    private lateinit var canvasVScroll: JScrollBar
    private lateinit var canvasHScroll: JScrollBar

    private val canvasBuilder = CanvasBuilder(DefaultBpmnProcessRenderer(currentIconProvider()))
    private val canvas: Canvas = currentCanvas()
    private lateinit var scrollHandler: ScrollBarInteractionHandler

    init {
        log.info("BPMN plugin started")
        // attach event listeners to canvas
        val mouseEventHandler = setCurrentMouseEventHandler(this.canvas)
        this.canvas.addMouseListener(mouseEventHandler)
        this.canvas.addMouseMotionListener(mouseEventHandler)
        this.canvas.addMouseWheelListener(mouseEventHandler)
        this.canvas.isFocusable = true
        this.canvas.addKeyListener(KeyboardEventHandler(canvas))
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
        attachScrollListenersAndClearSubs()
    }

    private fun setupUiAfterRun() {
        // show the rendered canvas
        this.canvas.isVisible = true
        this.canvasPanel.updateUI()
        this.canvasPanel.isEnabled = true
    }

    private fun attachScrollListenersAndClearSubs() {
        if (this::scrollHandler.isInitialized) {
            currentUiEventBus().clearSubscriptionsOf(ViewRectangleChangeEvent::class, this.scrollHandler)
        }
        this.scrollHandler = ScrollBarInteractionHandler(canvas, canvasPanel, canvasHScroll, canvasVScroll)
    }

    class JavaEditorTextField(document: Document, project: Project): EditorTextField(document, project, StdFileTypes.JAVA) {

        override fun createEditor(): EditorEx {
            val editorEx: EditorEx = super.createEditor()
            return editorEx
        }

        // If not overridden causes NPE when calling editCellAt of JTable (when changing selected cell with TAB fast)
        override fun requestFocus() {
            if (editor != null) {
                IdeFocusManager.getGlobalInstance().doWhenFocusSettlesDown {
                    val currEditor = editor
                    if (currEditor != null) {
                        IdeFocusManager.getGlobalInstance().requestFocus(currEditor.contentComponent, true)
                    }
                }
                val currEditor = editor
                currEditor?.scrollingModel?.scrollToCaret(ScrollType.RELATIVE)
            }
        }
    }
}

class ScrollBarInteractionHandler(private val canvas: Canvas, private val canvasPanel: JPanel, private val canvasHScroll: JScrollBar, private val canvasVScroll: JScrollBar) {

    private val hListener = ScrollListener { prev, current ->
        canvas.dragCanvas(
            Point2D.Float(current, 0.0f),
            Point2D.Float(prev, 0.0f)
        )
    }
    private val vListener = ScrollListener { prev, current ->
        canvas.dragCanvas(
            Point2D.Float(0.0f, current),
            Point2D.Float(0.0f, prev)
        )
    }

    init {
        currentUiEventBus().subscribe(ViewRectangleChangeEvent::class, this) { updateScrollBars(it.onScreenModel) }
        canvasHScroll.adjustmentListeners.forEach { canvasHScroll.removeAdjustmentListener(it) }
        canvasHScroll.addAdjustmentListener(hListener)

        canvasVScroll.adjustmentListeners.forEach { canvasVScroll.removeAdjustmentListener(it) }
        canvasVScroll.addAdjustmentListener(vListener)
    }

    private fun updateScrollBars(onScreenModel: Rectangle2D.Float) {
        if (vListener.scrolling || hListener.scrolling) {
            return
        }

        canvasHScroll.minimum = if (-onScreenModel.x < 0.0f) -onScreenModel.x.toInt() else 0
        canvasHScroll.maximum = if (-onScreenModel.x >= onScreenModel.width) -onScreenModel.x.toInt() else onScreenModel.width.toInt()
        canvasHScroll.value = -onScreenModel.x.toInt()
        canvasHScroll.visibleAmount = minOf(canvasPanel.width, abs(canvasHScroll.maximum), abs(onScreenModel.width.toInt() + onScreenModel.x.toInt()))
        canvasVScroll.minimum = if (-onScreenModel.y < 0.0f) -onScreenModel.y.toInt() else 0
        canvasVScroll.maximum = if (-onScreenModel.y >= onScreenModel.height) -onScreenModel.y.toInt() else onScreenModel.height.toInt()
        canvasVScroll.value = -onScreenModel.y.toInt()
        canvasVScroll.visibleAmount = minOf(canvasPanel.height, abs(canvasVScroll.maximum), abs(onScreenModel.height.toInt() + onScreenModel.y.toInt()))
    }

    class ScrollListener(private val onScroll: (Float, Float) -> Unit): AdjustmentListener {

        private var prevValue: Int? = null
        var scrolling = false
            private set

        override fun adjustmentValueChanged(e: AdjustmentEvent) {
            scrolling = true
            if (e.valueIsAdjusting && null == prevValue) {
                prevValue = e.value
            }

            prevValue?.apply {
                onScroll(this.toFloat(), e.value.toFloat())
                prevValue = e.value
            }

            if (!e.valueIsAdjusting) {
                prevValue = null
                scrolling = false
            }
        }
    }
}