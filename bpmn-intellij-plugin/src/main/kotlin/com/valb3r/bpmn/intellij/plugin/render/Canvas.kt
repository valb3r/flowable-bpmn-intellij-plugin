package com.valb3r.bpmn.intellij.plugin.render

import com.google.common.cache.CacheBuilder
import com.intellij.ui.EditorTextField
import com.valb3r.bpmn.intellij.plugin.Colors
import com.valb3r.bpmn.intellij.plugin.bpmn.api.BpmnProcessObjectView
import com.valb3r.bpmn.intellij.plugin.bpmn.api.diagram.DiagramElementId
import com.valb3r.bpmn.intellij.plugin.events.updateEventsRegistry
import com.valb3r.bpmn.intellij.plugin.properties.PropertiesVisualizer
import com.valb3r.bpmn.intellij.plugin.state.currentStateProvider
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.RenderingHints
import java.awt.geom.Point2D
import java.awt.geom.Rectangle2D
import java.awt.image.BufferedImage
import java.util.concurrent.TimeUnit
import javax.swing.JPanel
import javax.swing.JTable
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

class Canvas: JPanel() {

    private val epsilon = 0.1f
    private val anchorAttractionThreshold = 10.0f
    private val anchorPointElasticity = 0.3f
    private val anchorPointForceThreshold = 3.0f
    private val anchorShapeElasticity = 0.5f
    private val anchorShapeForceThreshold = 30.0f
    private val zoomFactor = 1.2f
    private val cursorSize = 3
    private val defaultCameraOrigin = Point2D.Float(0f, 0f)
    private val defaultZoomRatio = 1f
    private val stateProvider = currentStateProvider()
    private val updateEvents = updateEventsRegistry()

    private var selectedElements: MutableSet<DiagramElementId> = mutableSetOf()
    private var camera = Camera(defaultCameraOrigin, Point2D.Float(defaultZoomRatio, defaultZoomRatio))
    private var interactionCtx: ElementInteractionContext = ElementInteractionContext(mutableSetOf(), mutableMapOf(), mutableMapOf(), emptySet(), Point2D.Float(), Point2D.Float())
    private var processObject: BpmnProcessObjectView? = null
    private var renderer: BpmnProcessRenderer? = null
    private var areaByElement: Map<DiagramElementId, AreaWithZindex>? = null
    private var propertiesVisualizer: PropertiesVisualizer? = null

    private val cachedIcons = CacheBuilder.newBuilder()
            .expireAfterAccess(10L, TimeUnit.SECONDS)
            .maximumSize(100)
            .build<String, BufferedImage>()

    override fun paintComponent(graphics: Graphics) {
        super.paintComponent(graphics)

        val graphics2D = setupGraphics(graphics)
        areaByElement = renderer?.render(
                RenderContext(
                        CanvasPainter(graphics2D, camera.copy(), cachedIcons),
                        selectedElements.toSet(),
                        interactionCtx.copy(),
                        stateProvider
                )
        )
    }

    fun reset(properties: JTable, editorFactory: (value: String) -> EditorTextField, processObject: BpmnProcessObjectView, renderer: BpmnProcessRenderer) {
        this.processObject = processObject
        this.renderer = renderer
        this.camera = Camera(defaultCameraOrigin, Point2D.Float(defaultZoomRatio, defaultZoomRatio))
        this.propertiesVisualizer = PropertiesVisualizer(properties, editorFactory)
        this.stateProvider.resetStateTo(processObject)
    }

    fun click(location: Point2D.Float) {
        val clickedElements = elemsUnderCursor(location)
        clickedElements.forEach { interactionCtx.clickCallbacks.get(it)?.invoke(updateEvents) }

        this.selectedElements.clear()
        interactionCtx = ElementInteractionContext(mutableSetOf(), mutableMapOf(), mutableMapOf(), emptySet(), Point2D.Float(), Point2D.Float())
        this.selectedElements.addAll(clickedElements)

        repaint()

        val elementIdForPropertiesTable = selectedElements.firstOrNull()
        processObject?.elementByDiagramId
                ?.get(elementIdForPropertiesTable)
                ?.let { elemId ->
                    processObject?.elemPropertiesByElementId?.get(elemId)?.let { propertiesVisualizer?.visualize(elemId, it) }
                }
    }

    fun dragCanvas(start: Point2D.Float, current: Point2D.Float) {
        val newCameraOrigin = Point2D.Float(
                camera.origin.x - current.x + start.x,
                camera.origin.y - current.y + start.y
        )
        camera = Camera(newCameraOrigin, Point2D.Float(camera.zoom.x, camera.zoom.y))
        repaint()
    }

    fun startDragWithButton(current: Point2D.Float) {
        val elemsUnderCursor = elemsUnderCursor(current)
        if (selectedElements.intersect(elemsUnderCursor).isEmpty()) {
            interactionCtx = ElementInteractionContext(emptySet(), mutableMapOf(), mutableMapOf(), emptySet(), camera.fromCameraView(current), camera.fromCameraView(current))
            return
        }

        interactionCtx = interactionCtx.copy(
                draggedIds = selectedElements.toMutableSet(),
                start = camera.fromCameraView(current),
                current = camera.fromCameraView(current)
        )
    }

    fun attractToAnchors(ctx: ElementInteractionContext): ElementInteractionContext {
        val anchors: MutableSet<AnchorDetails> = mutableSetOf()
        val elementsWithAnchors = mutableSetOf<DiagramElementId>()

        for (dragged in ctx.draggedIds) {
            val draggedType = areaByElement?.get(dragged)?.areaType ?: AreaType.SHAPE

            val draggedAnchors = if (AreaType.SHAPE == draggedType) {
                areaByElement?.get(dragged)?.anchorsForShape.orEmpty()
            } else {
                areaByElement?.get(dragged)?.anchorsForWaypoints.orEmpty()
            }

            val anchorsToSearchIn = areaByElement
                    ?.filter { !ctx.draggedIds.contains(it.key) }
                    // shape is not affected by waypoints
                    ?.filter { if (draggedType == AreaType.SHAPE) it.value.areaType == AreaType.SHAPE else true }

            val elem = areaByElement?.get(dragged)
            val shapeX = elem?.dragCenter?.x
            val shapeY = elem?.dragCenter?.y
            // Compensates dragging start location difference from element origin
            val dx = if (null != shapeX) ctx.start.x - shapeX else 0.0f
            val dy = if (null != shapeY) ctx.start.y - shapeY else 0.0f

            for ((elemId, searchIn) in anchorsToSearchIn.orEmpty()) {
                for (draggedAnchor in draggedAnchors) {
                    val targetAnchors = if (AreaType.SHAPE == draggedType) searchIn.anchorsForShape else searchIn.anchorsForWaypoints
                    for (anchor in targetAnchors) {
                        if (abs(draggedAnchor.x - anchor.x) < anchorAttractionThreshold) {
                            anchors += AnchorDetails(Point2D.Float(anchor.x, anchor.y), Point2D.Float(draggedAnchor.x, draggedAnchor.y), Point2D.Float(dx, dy), Achors.HORIZONTAL, elem?.areaType)
                            elementsWithAnchors += elemId
                        } else if (abs(draggedAnchor.y - anchor.y) < anchorAttractionThreshold) {
                            anchors += AnchorDetails(Point2D.Float(anchor.x, anchor.y), Point2D.Float(draggedAnchor.x, draggedAnchor.y), Point2D.Float(dx, dy), Achors.VERTICAL, elem?.areaType)
                            elementsWithAnchors += elemId
                        }
                    }
                }
            }
        }

        val anchorX = anchors.filter { it.type == Achors.HORIZONTAL }.minBy { it.anchor.distance(Point2D.Float( ctx.current.x - it.dragCenterCompensation.x,  ctx.current.y - it.dragCenterCompensation.y)) }
        val anchorY = anchors.filter { it.type == Achors.VERTICAL }.minBy { it.anchor.distance(Point2D.Float(ctx.current.x - it.dragCenterCompensation.x,  ctx.current.y - it.dragCenterCompensation.y)) }

        val selectedAnchors: MutableSet<Pair<Point2D.Float, Point2D.Float>> = mutableSetOf()
        val selectedX = anchorX?.anchor?.x ?: ctx.current.x + (anchorX?.dragCenterCompensation?.x ?: 0.0f)
        val selectedY = anchorY?.anchor?.y ?: ctx.current.y + (anchorY?.dragCenterCompensation?.y ?: 0.0f)

        val dist = Point2D.Float(selectedX, selectedY).distance(ctx.current)
        var targetX = ctx.current.x
        var targetY = ctx.current.y

        if (dist * anchorPointElasticity < anchorPointForceThreshold && (anchorX?.areaType == AreaType.POINT || anchorY?.areaType == AreaType.POINT)) {
            anchorX?.apply { selectedAnchors += Pair(anchor, objectAnchor)}
            anchorY?.apply { selectedAnchors += Pair(anchor, objectAnchor)}
            // compensate drag start position
            targetX = selectedX
            targetY = selectedY
        } else if (dist * anchorShapeElasticity < anchorShapeForceThreshold && (anchorX?.areaType == AreaType.SHAPE || anchorY?.areaType == AreaType.SHAPE)) {
            anchorX?.apply { selectedAnchors += Pair(anchor, objectAnchor)}
            anchorY?.apply { selectedAnchors += Pair(anchor, objectAnchor)}
            // compensate drag start position
            targetX = selectedX
            targetY = selectedY
        }

        return ctx.copy(
                current = Point2D.Float(targetX, targetY),
                anchorsHit = selectedAnchors
        )
    }

    fun dragWithWheel(previous: Point2D.Float, current: Point2D.Float) {
        dragCanvas(previous, current)
    }

    fun dragWithLeftButton(previous: Point2D.Float, current: Point2D.Float) {
        if (selectedElements.isEmpty() || interactionCtx.draggedIds.isEmpty()) {
            dragCanvas(previous, current)
            return
        }

        interactionCtx = interactionCtx.copy(current = camera.fromCameraView(current))
        interactionCtx = attractToAnchors(interactionCtx)
        repaint()
    }

    fun stopDrag() {
        if (interactionCtx.draggedIds.isNotEmpty() && (interactionCtx.current.distance(interactionCtx.start) > epsilon)) {
            val dx = interactionCtx.current.x - interactionCtx.start.x
            val dy = interactionCtx.current.y - interactionCtx.start.y
            interactionCtx.draggedIds.forEach { interactionCtx.dragEndCallbacks[it]?.invoke(dx, dy, updateEvents) }
        }

        interactionCtx = interactionCtx.copy(draggedIds = emptySet())
        repaint()
    }

    fun zoom(anchor: Point2D.Float, factor: Int) {
        val scale = Math.pow(zoomFactor.toDouble(), factor.toDouble()).toFloat()

        if (min(camera.zoom.x, camera.zoom.y) * scale < 0.3f || max(camera.zoom.x, camera.zoom.y) * scale > 2.0f) {
            return
        }

        val scenePoint = camera.fromCameraView(anchor)
        camera = Camera(
                Point2D.Float(
                        camera.origin.x + scenePoint.x * camera.zoom.x * (scale - 1.0f),
                        camera.origin.y + scenePoint.y * camera.zoom.y * (scale - 1.0f)
                ),
                Point2D.Float(camera.zoom.x * scale, camera.zoom.y * scale)
        )
        repaint()
    }

    private fun elemsUnderCursor(cursorPoint: Point2D.Float): List<DiagramElementId> {
        val cursor = cursorRect(cursorPoint)
        val intersection = areaByElement?.filter { it.value.area.intersects(cursor) }
        val minZindex = intersection?.minBy { it.value.index }
        val result = mutableListOf<DiagramElementId>()
        // Force elements of only one dominating Z-Index and their parents
        intersection
                ?.filter { it.value.index == minZindex?.value?.index }
                ?.forEach { result += it.key; it.value.parentToSelect?.apply { result += this }  }
        return result
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

    // to handle small area shapes
    private fun cursorRect(location: Point2D.Float): Rectangle2D {
        val left = Point2D.Float(location.x - cursorSize, location.y - cursorSize)
        val right = Point2D.Float(location.x + cursorSize, location.y + cursorSize)

        return Rectangle2D.Float(
                left.x,
                left.y,
                right.x - left.x,
                right.y - left.y
        )
    }

    private enum class Achors {
        VERTICAL,
        HORIZONTAL
    }

    private data class AnchorDetails(
            val anchor: Point2D.Float,
            val objectAnchor: Point2D.Float,
            val dragCenterCompensation: Point2D.Float,
            val type: Achors,
            val areaType: AreaType?
    )
}