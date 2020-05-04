package com.valb3r.bpmn.intellij.plugin

import java.awt.Dimension
import java.io.File
import javax.swing.JLabel
import javax.swing.JPanel

class BpmnPluginToolWindow {

    private lateinit var mainToolWindowForm: JPanel
    private lateinit var elementPropertiesPanel: JPanel
    private lateinit var actionsPanel: JPanel
    private lateinit var canvasPanel: JPanel
    private lateinit var actionsTitleLabel: JLabel
    private lateinit var propertiesLabel: JLabel

    private val canvasBuilder = CanvasBuilder()
    private val canvas: Canvas = Canvas(this)

    init {
        // attach event listeners to canvas
        val mouseEventHandler = MouseEventHandler(this.canvas)
        this.canvas.addMouseListener(mouseEventHandler)
        this.canvas.addMouseMotionListener(mouseEventHandler)
        this.canvas.addMouseWheelListener(mouseEventHandler)
        this.canvasPanel.add(this.canvas)
    }

    fun getContent() = this.mainToolWindowForm
    fun getCanvasSize(): Dimension = this.canvasPanel.size

    fun run(bpmnFile: File) {
        setupUiBeforeRun()
        this@BpmnPluginToolWindow.canvasBuilder.build(canvas, bpmnFile)
        setupUiAfterRun()
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