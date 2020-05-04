package com.valb3r.bpmn.intellij.plugin

import com.valb3r.bpmn.intellij.plugin.bpmn.api.BpmnProcessObjectView
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.Point
import java.awt.RenderingHints
import java.awt.geom.Area
import java.awt.geom.Point2D
import java.awt.geom.Rectangle2D
import javax.swing.JPanel

class Canvas(private val pluginToolWindow: BpmnPluginToolWindow): JPanel() {

    private val zoomFactor = 1.2f
    private val cursorSize = 3
    private val defaultCameraOrigin = Point2D.Float(0f, 0f)
    private val defaultZoomRatio = 1f

    private var selectedElements: MutableSet<String> = mutableSetOf()

    private var camera = Camera(defaultCameraOrigin, Point2D.Float(defaultZoomRatio, defaultZoomRatio))
    private var processObject: BpmnProcessObjectView? = null
    private var renderer: BpmnProcessRenderer? = null

    private var areaByElement: Map<String, Area>? = null

    override fun paintComponent(graphics: Graphics) {
        super.paintComponent(graphics)

        val graphics2D = setupGraphics(graphics)
        areaByElement = renderer?.render(CanvasPainter(graphics2D, camera.copy()), selectedElements, processObject)
    }

    fun reset(processObject: BpmnProcessObjectView, renderer: BpmnProcessRenderer) {
        this.processObject = processObject
        this.renderer = renderer
        this.camera = Camera(defaultCameraOrigin, Point2D.Float(defaultZoomRatio, defaultZoomRatio))
    }

    fun click(location: Point) {
        this.selectedElements.clear()
        val cursor = cursorRect(location)
        areaByElement
                ?.filter { it.value.intersects(cursor) }
                ?.forEach { this.selectedElements.add(it.key) }

        repaint()
    }

    fun drag(start: Point2D.Float, current: Point2D.Float) {
        val newCameraOrigin = Point2D.Float(
                camera.origin.x - current.x + start.x,
                camera.origin.y - current.y + start.y
        )
        camera = Camera(newCameraOrigin, Point2D.Float(camera.zoom.x, camera.zoom.y))
        repaint()
    }

    fun zoom(factor: Int) {
        val scale = Math.pow(zoomFactor.toDouble(), factor.toDouble()).toFloat()
        camera = Camera(camera.origin, Point2D.Float(camera.zoom.x * scale, camera.zoom.y * scale))
        repaint()
    }

    private fun setupGraphics(graphics: Graphics): Graphics2D {
        // set up the drawing panel
        val graphics2D = graphics as Graphics2D
        graphics2D.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        graphics2D.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)

        // fill the background for entire canvas
        graphics2D.color = Colors.BACKGROUND_COLOR.color
        graphics2D.fillRect(0, 0, this.width, this.height)

        return graphics2D
    }

    // to handle 0-area shapes
    private fun cursorRect(location: Point): Rectangle2D {
        val left = Point2D.Float(location.x.toFloat() - cursorSize, location.y.toFloat() - cursorSize)
        val right = Point2D.Float(location.x.toFloat() + cursorSize, location.y.toFloat() + cursorSize)

        return Rectangle2D.Float(
                left.x,
                left.y,
                right.x - left.x,
                right.y - left.y
        )
    }
}