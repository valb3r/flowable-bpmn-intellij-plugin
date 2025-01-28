package com.valb3r.bpmn.intellij.plugin.core.render

import com.google.common.annotations.VisibleForTesting
import com.google.common.cache.Cache
import com.google.common.cache.CacheBuilder
import com.google.common.collect.EvictingQueue
import com.google.common.math.Quantiles.percentiles
import com.intellij.openapi.project.Project
import com.intellij.util.ui.UIUtil
import com.valb3r.bpmn.intellij.plugin.bpmn.api.BpmnProcessObjectView
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.BpmnElementId
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.BpmnSequenceFlow
import com.valb3r.bpmn.intellij.plugin.bpmn.api.diagram.DiagramElementId
import com.valb3r.bpmn.intellij.plugin.core.Colors
import com.valb3r.bpmn.intellij.plugin.core.events.updateEventsRegistry
import com.valb3r.bpmn.intellij.plugin.core.newelements.newElementsFactory
import com.valb3r.bpmn.intellij.plugin.core.properties.PropertiesVisualizer
import com.valb3r.bpmn.intellij.plugin.core.properties.propertiesVisualizer
import com.valb3r.bpmn.intellij.plugin.core.render.elements.BaseBpmnRenderElement
import com.valb3r.bpmn.intellij.plugin.core.render.elements.edges.BaseEdgeRenderElement
import com.valb3r.bpmn.intellij.plugin.core.render.uieventbus.*
import com.valb3r.bpmn.intellij.plugin.core.settings.currentSettings
import com.valb3r.bpmn.intellij.plugin.core.state.currentStateProvider
import java.awt.Color
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.RenderingHints
import java.awt.geom.Point2D
import java.awt.geom.Rectangle2D
import java.awt.image.BufferedImage
import java.util.*
import java.util.concurrent.TimeUnit
import javax.swing.JPanel
import kotlin.collections.LinkedHashMap
import kotlin.math.*

private val currentCanvas = Collections.synchronizedMap(WeakHashMap<Project,  Canvas>())

fun currentCanvas(project: Project): Canvas {
    return currentCanvas.computeIfAbsent(project) {
        Canvas(project, DefaultCanvasConstants())
    }
}

fun allCanvas(): Collection<Canvas> {
    return currentCanvas.values
}

@VisibleForTesting
fun setCanvas(project: Project, canvas: Canvas): Canvas {
    currentCanvas[project] = canvas
    return canvas
}

open class Canvas(private val project: Project, private val settings: CanvasConstants) : JPanel() {
    private val fpsCircularBuffer = EvictingQueue.create<Int>(30)
    private var cachedTreeState: TreeState? = null

    private val stateProvider = currentStateProvider(project)
    private val closeAnchorRadius = 100.0f

    private var selectedElements: MutableSet<DiagramElementId> = mutableSetOf()
    private var camera = Camera(settings.defaultCameraOrigin, Point2D.Float(settings.defaultZoomRatio, settings.defaultZoomRatio))

    private var interactionCtx: ElementInteractionContext = ElementInteractionContext(emptySet(), emptySet(), mutableMapOf(), null, mutableMapOf(), mutableMapOf(), null, Point2D.Float(), Point2D.Float())
    private var renderer: BpmnProcessRenderer? = null
    private var areaByElement: Map<DiagramElementId, AreaWithZindex>? = null
    private var propsVisualizer: PropertiesVisualizer? = null

    private val cachedIcons = CacheBuilder.newBuilder()
            .expireAfterAccess(10L, TimeUnit.SECONDS)
            .maximumSize(100)
            .build<Long, BufferedImage>()

    private var latestOnScreenModelDimensions: Rectangle2D.Float? = null

    init {
        currentUiEventBus(project).subscribe(ZoomInEvent::class) {
            zoom(Point2D.Float(width / 2.0f, height / 2.0f), 1)
        }

        currentUiEventBus(project).subscribe(ZoomOutEvent::class) {
            zoom(Point2D.Float(width / 2.0f, height / 2.0f), -1)
        }

        currentUiEventBus(project).subscribe(CenterModelEvent::class) {
            latestOnScreenModelDimensions?.let {
                camera = camera.copy(origin = cameraOriginToPinCenter(it))
                repaint()
            }
        }

        currentUiEventBus(project).subscribe(ResetAndCenterEvent::class) {
            val dimensions = latestOnScreenModelDimensions ?: return@subscribe
            val modelOrigSt = camera.fromCameraView(Point2D.Float(dimensions.x, dimensions.y))
            val modelOrigEn = camera.fromCameraView(Point2D.Float(dimensions.x + dimensions.width, dimensions.y + dimensions.height))
            val zoomRatio = min(currentSettings().zoomMax, max(currentSettings().zoomMin, min(width / (modelOrigEn.x - modelOrigSt.x + 1e-6f), height / (modelOrigEn.y - modelOrigSt.y + 1e-6f))))
            val zoom = Point2D.Float(zoomRatio, zoomRatio)
            camera = camera.copy(origin = cameraOriginToPinCenter(dimensions, zoom), zoom = zoom)
            repaint()
        }

        currentUiEventBus(project).subscribe(ViewRectangleChangeEvent::class) {
            if (null == latestOnScreenModelDimensions) {
                latestOnScreenModelDimensions = it.onScreenModel
                currentUiEventBus(project).publish(ResetAndCenterEvent())
            }
            latestOnScreenModelDimensions = it.onScreenModel
        }

        currentUiEventBus(project).subscribe(SelectElements::class) {
            selectElements(it.elementIds)
            repaint()
            updatePropertyVisualizer(it.elementIds)
        }
    }

    @VisibleForTesting
    public override fun paintComponent(graphics: Graphics) {
        withFps(graphics) {
            super.paintComponent(graphics)

            val graphics2D = setupGraphics(graphics)
            // TODO: make immutable and not shallow:
            interactionCtx = interactionCtx.copy(dragEndCallbacks = mutableMapOf(), clickCallbacks = mutableMapOf())
            val shallowCopyOfCtx = interactionCtx.copy()
            val result = renderer?.render(
                RenderContext(
                    project,
                    buildPainter(graphics2D, camera.copy(), cachedIcons),
                    selectedElements.toSet(),
                    shallowCopyOfCtx,
                    stateProvider,
                    cachedTreeState
                )
            )
            cachedTreeState = result?.state
            areaByElement = result?.areas
        }
    }

    open protected fun buildPainter(graphics2D: Graphics2D, camera: Camera, cache: Cache<Long, BufferedImage>)
            = CanvasPainter(graphics2D, camera, cache)

    fun renderToBitmap() : BufferedImage? {
        val doRender = { image: BufferedImage, ctx: ElementInteractionContext, camera: Camera ->
            val graphics = setupGraphicsAntialiasing(image.createGraphics())
            // fill the background for entire canvas
            graphics.color = Colors.TRANSPARENT.color
            graphics.fillRect(0, 0, image.width, image.height)
            renderer?.renderOnlyDiagram(
                RenderContext(
                    project,
                    CanvasPainter(graphics, camera, cachedIcons),
                    setOf(),
                    ctx,
                    stateProvider
                )
            )
        }
        val interactionContext = ElementInteractionContext(emptySet(), emptySet(), mutableMapOf(), null, mutableMapOf(), mutableMapOf(), null, Point2D.Float(), Point2D.Float())
        val dummyImage = UIUtil.createImage(1, 1, BufferedImage.TYPE_INT_RGB)
        val dimensions = doRender(dummyImage, interactionContext, Camera(Point2D.Float(0.0f, 0.0f), Point2D.Float(1.0f, 1.0f)))?.areas ?: return null
        val maxX = dimensions.map { it.value.area.bounds2D.maxX }.max()?.toInt() ?: return null
        val minX = dimensions.map { it.value.area.bounds2D.minX }.min()?.toInt() ?: return null
        val maxY = dimensions.map { it.value.area.bounds2D.maxY }.max()?.toInt() ?: return null
        val minY = dimensions.map { it.value.area.bounds2D.minY }.min()?.toInt() ?: return null
        val width = maxX - minX
        val height = maxY - minY

        val borderSpaceCoeff = 1.05f
        val renderedImage = UIUtil.createImage(
            (width * borderSpaceCoeff).toInt(),
            (height * borderSpaceCoeff).toInt(),
            BufferedImage.TYPE_INT_ARGB
        )
        doRender(
            renderedImage,
            interactionContext,
            Camera(
                Point2D.Float(width * (1.0f - borderSpaceCoeff) / 2.0f + minX, height * (1.0f - borderSpaceCoeff) / 2.0f + minY),
                Point2D.Float(1.0f, 1.0f)
            )
        )
        return renderedImage
    }

    fun reset(fileContent: String, processObject: BpmnProcessObjectView, renderer: BpmnProcessRenderer) {
        this.cachedTreeState = null
        this.renderer = renderer
        this.latestOnScreenModelDimensions = null
        this.camera = Camera(settings.defaultCameraOrigin, Point2D.Float(settings.defaultZoomRatio, settings.defaultZoomRatio))
        this.propsVisualizer = propertiesVisualizer(project)
        this.propsVisualizer?.clear()
        this.stateProvider.resetStateTo(fileContent, processObject)
        selectedElements = mutableSetOf()
        repaint()
    }

    fun click(location: Point2D.Float) {
        propsVisualizer?.clear()
        val clickedElements = elemUnderCursor(location)
        clickedElements.forEach { interactionCtx.clickCallbacks[it]?.invoke(updateEventsRegistry(project)) }
        val invokeAfter = clickedElements.mapNotNull { interactionCtx.postClickCallbacks[it] }

        updateSelectedElements(clickedElements)

        repaint()

        val propertiesForElement =
            if (selectedElements.isEmpty()) elemUnderCursor(location, setOf()) else selectedElements
        updatePropertyVisualizer(propertiesForElement)
        invokeAfter.forEach { it.invoke(updateEventsRegistry(project)) }
    }

    fun dragCanvas(start: Point2D.Float, current: Point2D.Float) {
        val newCameraOrigin = Point2D.Float(
                camera.origin.x - current.x + start.x,
                camera.origin.y - current.y + start.y
        )
        camera = Camera(newCameraOrigin, Point2D.Float(camera.zoom.x, camera.zoom.y))
        repaint()
    }

    fun startCanvasDragWithButton(current: Point2D.Float) {
        val point = camera.fromCameraView(current)
        val elemsUnderCursor = elemUnderCursor(current)
        if (selectedElements.intersect(elemsUnderCursor).isEmpty()) {
            interactionCtx = ElementInteractionContext(emptySet(), emptySet(), mutableMapOf(), null, mutableMapOf(), mutableMapOf(), null, point, point)
            return
        }
    }

    fun startSelectionOrDrag(current: Point2D.Float) {
        val point = camera.fromCameraView(current)
        val elemsUnderCursor = elemUnderCursor(current).toSet()

        if (elemsUnderCursor.isEmpty()) {
            interactionCtx = ElementInteractionContext(emptySet(), emptySet(), mutableMapOf(), SelectionRect(current, current), mutableMapOf(), mutableMapOf(), null, point, point)
            return
        }

        if (selectedElements.intersect(elemsUnderCursor).isEmpty() && elemsUnderCursor.isNotEmpty()) {
            this.selectedElements.clear()
            this.selectedElements.addAll(elemsUnderCursor)
        }

        if (selectedElements.intersect(elemsUnderCursor).isNotEmpty()) {
            dragSelectedElements(point)
            return
        }

        startRectangleSelection(point, current)
    }

    fun attractToAnchors(ctx: ElementInteractionContext): ElementInteractionContext {
        val dragged = draggedElement(ctx)

        val draggedArea = areaByElement?.get(dragged) ?: return ctx
        val draggedType =  draggedArea.areaType
        val draggedAnchors = if (AreaType.SHAPE == draggedType) {
            draggedArea.anchorsForShape
        } else {
            draggedArea.anchorsForWaypoints
        }

        if (draggedType == AreaType.SELECTS_DRAG_TARGET) {
            val target = selectDragTarget(dragged!!, ctx)
            return ctx.copy(dragTargetedIds = if (null != target) setOf(target) else emptySet())
        }

        val anchorsToSearchIn = areaByElement
                ?.filter { !ctx.draggedIds.contains(it.key) }
                // shape is not affected by waypoints
                ?.filter {
                    if (draggedType == AreaType.SHAPE || draggedType == AreaType.SHAPE_THAT_NESTS) {
                        it.value.areaType == AreaType.SHAPE || it.value.areaType == AreaType.SHAPE_THAT_NESTS
                    } else {
                        true
                    }
                }

        val anchors: MutableSet<AnchorDetails> = mutableSetOf()
        val closeAnchors: MutableSet<Point2D.Float> = mutableSetOf()
        for ((_, searchIn) in anchorsToSearchIn?.filter { it.value.index <= draggedArea.index }.orEmpty()) {
            for (draggedAnchor in draggedAnchors) {
                val targetAnchors = if (AreaType.SHAPE == draggedType) searchIn.anchorsForShape else searchIn.anchorsForWaypoints
                for (anchor in targetAnchors) {
                    if (anchor.point.distance(draggedAnchor.point) < closeAnchorRadius) {
                        closeAnchors.add(anchor.point)
                    }

                    val attractsX = abs(draggedAnchor.point.x - anchor.point.x) < settings.anchorAttractionThreshold
                    val attractsY = abs(draggedAnchor.point.y - anchor.point.y) < settings.anchorAttractionThreshold

                    if (attractsX && attractsY) {
                        anchors += AnchorDetails(anchor.point, draggedAnchor.point, AnchorType.POINT)
                    } else if (attractsX) {
                        anchors += AnchorDetails(anchor.point, draggedAnchor.point, AnchorType.HORIZONTAL)
                    } else if (attractsY) {
                        anchors += AnchorDetails(anchor.point, draggedAnchor.point, AnchorType.VERTICAL)
                    }
                }
            }
        }

        val pointAnchor = anchors.filter { it.type == AnchorType.POINT }
            .minBy { it.anchor.distance(it.objectAnchor) }
        val anchorX = anchors.filter { it.type == AnchorType.HORIZONTAL }
            .minBy { it.anchor.distance(it.objectAnchor) }
        val anchorY = anchors.filter { it.type == AnchorType.VERTICAL }
            .minBy { it.anchor.distance(it.objectAnchor) }

        val selectedAnchors: AnchorHit = if (null == pointAnchor) {
            applyOrthoOrNoneAnchors(anchorX, anchorY, ctx)
        } else {
            applyPointAnchor(pointAnchor, ctx)
        }
        val allAnchors = selectedAnchors.copy(closeAnchors = closeAnchors.toList())

        return ctx.copy(
                dragCurrent = Point2D.Float(selectedAnchors.dragged.x, selectedAnchors.dragged.y),
                anchorsHit = allAnchors
        )
    }

    private fun draggedElement(ctx: ElementInteractionContext): DiagramElementId? {
        val cameraPoint = camera.toCameraView(ctx.dragCurrent)
        val dragged = ctx.draggedIds.minBy {
            val bounds = areaByElement?.get(it)?.area?.bounds2D ?: Rectangle2D.Float()
            return@minBy Point2D.Float(bounds.centerX.toFloat(), bounds.centerY.toFloat()).distance(cameraPoint)
        }
        return dragged
    }

    fun clearSelection() {
        this.selectedElements.clear()
        interactionCtx = ElementInteractionContext(emptySet(), emptySet(), mutableMapOf(), null, mutableMapOf(), mutableMapOf(), null, Point2D.Float(), Point2D.Float())
    }

    fun selectElements(elements: Set<DiagramElementId>) {
        clearSelection()
        this.selectedElements.addAll(elements)
    }

   fun selectAllElements() {
        clearSelection()
        this.selectedElements.addAll(
            elemsUnderRect(
                null,
                excludeAreas = setOf(AreaType.PARENT_PROCESS_SHAPE, AreaType.SELECTS_DRAG_TARGET)
            )
        )
        repaint()
    }

    private fun selectDragTarget(dragged: DiagramElementId, ctx: ElementInteractionContext): DiagramElementId? {
        val areas = areaByElement!!
        val area = areas[dragged]!!.area
        val point = Point2D.Float(area.bounds2D.centerX.toFloat(), area.bounds2D.centerY.toFloat())
        return dragTargettableElements(cursorRect(point))
            .filter { !ctx.draggedIds.contains(it) }
            .filter { areas[it]?.areaType == AreaType.SHAPE || areas[it]?.areaType == AreaType.SHAPE_THAT_NESTS }
            .maxBy { areas[it]?.index ?: ICON_Z_INDEX }
    }

    fun dragWithWheel(previous: Point2D.Float, current: Point2D.Float) {
        dragCanvas(previous, current)
    }

    fun dragOrSelectWithLeftButton(previous: Point2D.Float, current: Point2D.Float) {
        val point = camera.fromCameraView(current)
        if (null != interactionCtx.dragSelectionRect) {
            selectElementsWithRectangle(current)
            return
        }

        if (selectedElements.isEmpty() && interactionCtx.draggedIds.isEmpty()) {
            dragCanvas(previous, current)
            return
        }

        interactionCtx = interactionCtx.copy(dragCurrent = point)
        interactionCtx = attractToAnchors(interactionCtx)
        repaint()
    }

    fun stopDragOrSelect() {
        if (null != interactionCtx.dragSelectionRect) {
            interactionCtx = interactionCtx.copy(dragSelectionRect = null)
            repaint()
            return
        }

        if (interactionCtx.draggedIds.isNotEmpty() && (interactionCtx.dragCurrent.distance(interactionCtx.dragStart) > settings.epsilon)) {
            interactionCtx = attractToAnchors(interactionCtx)
            val dx = interactionCtx.dragCurrent.x - interactionCtx.dragStart.x
            val dy = interactionCtx.dragCurrent.y - interactionCtx.dragStart.y
            updateEventsRegistry(project).addEvents(interactionCtx.draggedIds.flatMap {
                val droppedOn = bpmnElemsUnderCurrentCursorForDrag()
                interactionCtx.dragEndCallbacks[it]?.invoke(
                        dx,
                        dy,
                        if (droppedOn.isEmpty()) null else droppedOn.keys.first(),
                        droppedOn
                ) ?: emptyList()
            })
            // Drag End may cause property(s) change, therefore forcing visualizer to rebuild on current state
            updatePropertyVisualizer(selectedElements)
        }

        interactionCtx = interactionCtx.copy(draggedIds = emptySet(), dragTargetedIds = mutableSetOf(), dragCurrent = interactionCtx.dragStart, dragSelectionRect = null)
        repaint()
    }

    fun zoom(anchor: Point2D.Float, factor: Int) {
        val scale = currentSettings().zoomFactor.toDouble().pow(factor.toDouble()).toFloat()

        if (min(camera.zoom.x, camera.zoom.y) * scale < currentSettings().zoomMin || max(camera.zoom.x, camera.zoom.y) * scale > currentSettings().zoomMax) {
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

    fun fromCameraView(point: Point2D.Float): Point2D.Float {
        return camera.fromCameraView(point)
    }

    fun parentableElementAt(point: Point2D.Float): BpmnElementId {
        return parentableElemUnderCursor(point)
    }

    private fun updatePropertyVisualizer(propertiesForElement: Collection<DiagramElementId>) {
        val elementIdForPropertiesTable = propertiesForElement.firstOrNull()
        val state = stateProvider.currentState()
        state
            .elementByDiagramId[elementIdForPropertiesTable]
            ?.let { elemId ->
                state.elemPropertiesByStaticElementId[elemId]?.let {
                    propsVisualizer?.visualize(
                        newElementsFactory(
                            project
                        ), state.elemPropertiesByStaticElementId, elemId
                    )
                }
            } ?: propsVisualizer?.clear()
    }

    private fun updateSelectedElements(clickedElements: Collection<DiagramElementId>) {
        this.selectedElements.clear()
        interactionCtx = ElementInteractionContext(emptySet(), emptySet(), mutableMapOf(), null, mutableMapOf(), mutableMapOf(), null, Point2D.Float(), Point2D.Float())
        this.selectedElements.addAll(clickedElements)
    }

    private fun applyOrthoOrNoneAnchors(anchorX: AnchorDetails?, anchorY: AnchorDetails?, ctx: ElementInteractionContext): AnchorHit {
        val selectedAnchors: MutableMap<AnchorType, Point2D.Float> = mutableMapOf()
        val snappedDelta = snapToGridIfNecessary(ctx.dragCurrent.x - ctx.dragStart.x, ctx.dragCurrent.y - ctx.dragStart.y)
        val targetX = anchorX?.let { ctx.dragCurrent.x + it.anchor.x - it.objectAnchor.x } ?: (ctx.dragStart.x + snappedDelta.x)
        val targetY = anchorY?.let { ctx.dragCurrent.y + it.anchor.y - it.objectAnchor.y } ?: (ctx.dragStart.y + snappedDelta.y)

        val objectAnchorX = anchorX?.objectAnchor?.x ?: ctx.dragCurrent.x
        val objectAnchorY = anchorY?.objectAnchor?.y ?: ctx.dragCurrent.y
        val verticalAnchors = verticalAnchorsEnabled.get()
        if (verticalAnchors) {
            anchorX?.apply { selectedAnchors[AnchorType.HORIZONTAL] = this.anchor }
            anchorY?.apply { selectedAnchors[AnchorType.VERTICAL] = this.anchor }
        }
        return AnchorHit(Point2D.Float(targetX, targetY), Point2D.Float(objectAnchorX, objectAnchorY), selectedAnchors, emptyList())
    }

    private fun applyPointAnchor(anchor: AnchorDetails, ctx: ElementInteractionContext): AnchorHit {
        val selectedAnchors: MutableMap<AnchorType, Point2D.Float> = mutableMapOf()
        val targetX = ctx.dragCurrent.x + anchor.anchor.x - anchor.objectAnchor.x
        val targetY = ctx.dragCurrent.y + anchor.anchor.y - anchor.objectAnchor.y
        selectedAnchors[AnchorType.POINT] = anchor.anchor
        return AnchorHit(Point2D.Float(targetX, targetY), Point2D.Float(targetX, targetY), selectedAnchors, emptyList())
    }

    private fun bpmnElemsUnderCurrentCursorForDrag(): Map<BpmnElementId, AreaWithZindex> {
        val onScreen = camera.toCameraView(interactionCtx.dragCurrent)
        val cursor = cursorRect(onScreen)
        // Correct order would require non-layered but computed z-index
        val elems = areaByElement
                ?.filter { it.value.area.intersects(cursor) }
                ?.toList()
                ?.sortedByDescending { it.second.index }
                ?.filter { !interactionCtx.draggedIds.contains(it.first) && !selectedElements.contains(it.first) } ?: emptyList()

        val childrenOfDragged = lastRenderedState(project)?.allChildrenOf(interactionCtx.draggedIds + selectedElements) ?: emptySet()

        val result = linkedMapOf<BpmnElementId, AreaWithZindex>()
        val elementByDiagramId = stateProvider.currentState().elementByDiagramId
        val elementByBpmnId = stateProvider.currentState().elementByBpmnId

        for (elem in elems) {
            val elemId = elem.second.bpmnElementId
            if (null == elemId || result.containsKey(elemId) || childrenOfDragged.contains(elem.first)) {
                continue
            }

            val bpmnId = setOf(elementByDiagramId[elem.first], elem.second.bpmnElementId).filterNotNull().firstOrNull() ?: continue
            val bpmnElem = elementByBpmnId[bpmnId]
            if (bpmnElem?.element is BpmnSequenceFlow) {
                continue
            }
            result[bpmnId] = elem.second
        }

        enforceHighlightedElementPreference(elementByDiagramId, result)
        return result
    }

    private fun enforceHighlightedElementPreference(
        elementByDiagramId: Map<DiagramElementId, BpmnElementId>,
        result: LinkedHashMap<BpmnElementId, AreaWithZindex>
    ) {
        val dragged = draggedElement(interactionCtx)
        val preferredTarget = selectDragTarget(dragged!!, interactionCtx)

        preferredTarget?.let {
            val area = areaByElement?.get(it) ?: return@let
            val bpmnId = elementByDiagramId[it] ?: return@let
            result[bpmnId] = area
        }
    }

    private fun parentableElemUnderCursor(cursorPoint: Point2D.Float): BpmnElementId {
        val withinRect = cursorRect(cursorPoint)
        val intersection = areaByElement?.filter { it.value.area.intersects(withinRect) }
        val shapesThatCanParent = setOf(AreaType.PARENT_PROCESS_SHAPE, AreaType.SHAPE_THAT_NESTS)

        return intersection
                ?.filter { null != it.value.bpmnElementId }
                ?.filter { shapesThatCanParent.contains(it.value.areaType) }
                ?.toList()
                ?.sortedByDescending { it.second.index }
                ?.map { it.second.bpmnElementId }
                ?.filterNotNull()
                ?.first()!!
    }

    private fun elemUnderCursor(cursorPoint: Point2D.Float, excludeAreas: Set<AreaType> = setOf(AreaType.PARENT_PROCESS_SHAPE)): List<DiagramElementId> {
        val withinRect = cursorRect(cursorPoint)
        val intersection = areaByElement?.filter { it.value.area.intersects(withinRect) }
        val maxZindex = intersection?.maxBy { it: Map.Entry<DiagramElementId, AreaWithZindex> -> it.value.index }
        val result = mutableListOf<DiagramElementId>()
        val centerRect = Point2D.Float(withinRect.centerX.toFloat(), withinRect.centerY.toFloat())
        intersection
            ?.filter { !excludeAreas.contains(it.value.areaType) }
            ?.filter { it.value.index == maxZindex?.value?.index }
            ?.minBy { it: Map.Entry<DiagramElementId, AreaWithZindex> ->
                Point2D.Float(
                    it.value.area.bounds2D.centerX.toFloat(),
                    it.value.area.bounds2D.centerY.toFloat()
                ).distance(centerRect)
            }
            ?.let { result += it.key; it.value.parentToSelect?.apply { result += this } }
        return result
    }

    private fun elemsUnderRect(withinRect: Rectangle2D?, excludeAreas: Set<AreaType> = setOf(AreaType.PARENT_PROCESS_SHAPE)): List<DiagramElementId> {
        val intersection = if (null == withinRect) areaByElement else areaByElement?.filter { withinRect.contains(it.value.area.bounds2D) }

        val result = mutableListOf<DiagramElementId>()
        intersection
                ?.filter { !excludeAreas.contains(it.value.areaType) }
                ?.forEach { result += it.key; it.value.parentToSelect?.apply { result += this } }

        fun removeSubprocessChildren() {
            val childExclusions = intersection?.let { lastRenderedState(project)?.allChildrenOf(it.filter { it.value.areaType == AreaType.SHAPE_THAT_NESTS }.keys) }
                    ?: emptySet()
            result.removeAll(childExclusions)
        }

        removeSubprocessChildren()

        // avoid the situation when one selects elements from two subprocesses or processes of different level
        // as this causes parent ambiguity - which parent to use onDragEnd
        fun selectMajorityOfElementsWithSameParent() {
            val groupedByParent = result.groupBy {
                val parent = lastRenderedState(project)?.state?.elemMap?.get(it)?.parents?.getOrNull(0)
                // For anchor points return not edge, but edge parent
                if (parent is BaseEdgeRenderElement) {
                    return@groupBy parent.parents[0]
                }

                return@groupBy parent
            }
            val maxSize =
                groupedByParent.maxBy { it: Map.Entry<BaseBpmnRenderElement?, List<DiagramElementId>> -> it.value.size }
            result.clear()
            result.addAll(maxSize?.value ?: emptyList())
        }

        selectMajorityOfElementsWithSameParent()

        return result
    }

    private fun dragTargettableElements(withinRect: Rectangle2D, excludeAreas: Set<AreaType> = setOf(AreaType.PARENT_PROCESS_SHAPE)): List<DiagramElementId> {
        val intersection = areaByElement?.filter { withinRect.intersects(it.value.area.bounds2D) }

        val result = mutableListOf<DiagramElementId>()
        intersection
                ?.filter { !excludeAreas.contains(it.value.areaType) }
                ?.forEach { result += it.key; it.value.parentToSelect?.apply { result += this } }
        return result
    }


    private fun setupGraphicsAntialiasing(graphics: Graphics): Graphics2D {
        // set up the drawing panel
        val graphics2D = graphics as Graphics2D
        graphics2D.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        graphics2D.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)
        if (isJreHiDPIEnabledOnWindows()) {
            graphics2D.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY)
            graphics2D.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE)
        }
        return graphics2D
    }

    private fun setupGraphics(graphics: Graphics): Graphics2D {
        val graphics2D = setupGraphicsAntialiasing(graphics)

        // fill the background for entire canvas
        graphics2D.color = Colors.BACKGROUND_COLOR.color
        graphics2D.fillRect(0, 0, this.width, this.height)

        return graphics2D
    }

    // to handle small area shapes
    private fun cursorRect(location: Point2D.Float): Rectangle2D {
        return Rectangle2D.Float(
                location.x - settings.baseCursorSize / 2.0f,
                location.y - settings.baseCursorSize / 2.0f,
                settings.baseCursorSize,
                settings.baseCursorSize
        )
    }

    private fun startRectangleSelection(point: Point2D.Float, current: Point2D.Float) {
        interactionCtx = interactionCtx.copy(
                draggedIds = selectedElements,
                dragTargetedIds = emptySet(),
                dragStart = point,
                dragCurrent = point,
                dragSelectionRect = SelectionRect(current, current)
        )
    }

    private fun dragSelectedElements(point: Point2D.Float) {
        interactionCtx = interactionCtx.copy(
                draggedIds = selectedElements,
                dragTargetedIds = emptySet(),
                dragStart = point,
                dragCurrent = point
        )
    }

    private fun selectElementsWithRectangle(current: Point2D.Float) {
        interactionCtx = interactionCtx.copy(
                dragSelectionRect = SelectionRect(interactionCtx.dragSelectionRect!!.start, current)
        )

        this.selectedElements.clear()
        this.selectedElements.addAll(
                elemsUnderRect(
                        interactionCtx.dragSelectionRect!!.toRect(),
                        excludeAreas = setOf(AreaType.PARENT_PROCESS_SHAPE, AreaType.SELECTS_DRAG_TARGET)
                )
        )

        repaint()
    }

    private fun cameraOriginToPinCenter(modelRect: Rectangle2D.Float, zoom: Point2D.Float = camera.zoom): Point2D.Float {
        val modelCenter = camera.fromCameraView(Point2D.Float(modelRect.x + modelRect.width / 2.0f, modelRect.y + modelRect.height / 2.0f))
        val screenCenter = Point2D.Float(width / 2.0f, height / 2.0f)
        return Point2D.Float(zoom.x * modelCenter.x - screenCenter.x, zoom.y * modelCenter.y - screenCenter.y)
    }

    private fun withFps(graphics: Graphics, frameProducer: () -> Unit) {
        if (!currentSettings().enableFps) {
            frameProducer()
            return
        }

        val startTime = System.nanoTime()
        frameProducer()
        val endTime = System.nanoTime()

        val oldColor = graphics.color
        graphics.color = Color.BLACK
        fpsCircularBuffer.add(round(1.0e9f / (endTime - startTime).toFloat()).toInt())
        val stats = percentiles().indexes(10, 50, 90).compute(fpsCircularBuffer)
        graphics.drawString("FPS: ${stats[10]}/${stats[50]}/${stats[90]}", 0, this.height - 20)
        graphics.color = oldColor
    }

    private data class AnchorDetails(
            val anchor: Point2D.Float,
            val objectAnchor: Point2D.Float,
            val type: AnchorType
    )
}
