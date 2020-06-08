package com.valb3r.bpmn.intellij.plugin.render

import com.valb3r.bpmn.intellij.plugin.Colors
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.BpmnElementId
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.BpmnSequenceFlow
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.WithBpmnId
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.WithParentId
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.activities.BpmnCallActivity
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.events.begin.*
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.events.boundary.*
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.events.catching.BpmnIntermediateConditionalCatchingEvent
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.events.catching.BpmnIntermediateMessageCatchingEvent
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.events.catching.BpmnIntermediateSignalCatchingEvent
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.events.catching.BpmnIntermediateTimerCatchingEvent
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.events.end.*
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.events.throwing.BpmnIntermediateEscalationThrowingEvent
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.events.throwing.BpmnIntermediateNoneThrowingEvent
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.events.throwing.BpmnIntermediateSignalThrowingEvent
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.gateways.BpmnEventGateway
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.gateways.BpmnExclusiveGateway
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.gateways.BpmnInclusiveGateway
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.gateways.BpmnParallelGateway
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.subprocess.BpmnAdHocSubProcess
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.subprocess.BpmnSubProcess
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.subprocess.BpmnTransactionalSubProcess
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.tasks.*
import com.valb3r.bpmn.intellij.plugin.bpmn.api.diagram.DiagramElementId
import com.valb3r.bpmn.intellij.plugin.bpmn.api.diagram.elements.*
import com.valb3r.bpmn.intellij.plugin.bpmn.api.events.EdgeWithIdentifiableWaypoints
import com.valb3r.bpmn.intellij.plugin.bpmn.api.events.Event
import com.valb3r.bpmn.intellij.plugin.bpmn.api.events.IdentifiableWaypoint
import com.valb3r.bpmn.intellij.plugin.bpmn.api.info.Property
import com.valb3r.bpmn.intellij.plugin.bpmn.api.info.PropertyType
import com.valb3r.bpmn.intellij.plugin.bpmn.api.info.PropertyType.*
import com.valb3r.bpmn.intellij.plugin.debugger.currentDebugger
import com.valb3r.bpmn.intellij.plugin.events.*
import com.valb3r.bpmn.intellij.plugin.newelements.newElementsFactory
import com.valb3r.bpmn.intellij.plugin.render.elements.BaseRenderElement
import com.valb3r.bpmn.intellij.plugin.render.elements.RenderState
import com.valb3r.bpmn.intellij.plugin.render.elements.edges.EdgeRenderElement
import com.valb3r.bpmn.intellij.plugin.render.elements.planes.PlaneRenderElement
import com.valb3r.bpmn.intellij.plugin.render.elements.shapes.*
import com.valb3r.bpmn.intellij.plugin.state.CurrentState
import java.awt.BasicStroke
import java.awt.Color
import java.awt.geom.Area
import java.awt.geom.Ellipse2D
import java.awt.geom.Point2D
import java.awt.geom.Rectangle2D
import kotlin.math.max
import kotlin.math.min

interface BpmnProcessRenderer {
    fun render(ctx: RenderContext): Map<DiagramElementId, AreaWithZindex>
}

class DefaultBpmnProcessRenderer(val icons: IconProvider) : BpmnProcessRenderer {

    private val minShapeSize = 40.0f
    private val transactionalBoundaryMargin = 5.0f
    private val undoRedoStartMargin = 20.0f
    private val waypointLen = 40.0f
    private val activityToolBoxGap = 8.0f
    private val anchorRadius = 5f
    private val nodeRadius = 3f
    private val actionsIcoSize = 15f
    private val actionsMargin = 5f

    private val undoId = DiagramElementId("UNDO")
    private val redoId = DiagramElementId("REDO")

    private val ANCHOR_STROKE = BasicStroke(1.0f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0.0f, floatArrayOf(5.0f), 0.0f)
    private val ACTION_AREA_STROKE = BasicStroke(2.0f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0.0f, floatArrayOf(2.0f), 0.0f)

    override fun render(ctx: RenderContext): Map<DiagramElementId, AreaWithZindex> {
        val state = RenderState(
                ctx.stateProvider.currentState(),
                ctx,
                icons
        )
        
        val elements = mutableListOf<BaseRenderElement>()
        val elementsById = mutableMapOf<BpmnElementId, BaseRenderElement>()
        val root = createRootProcessElem(state, elements, elementsById)
        createShapes(state, elements, elementsById)
        createEdges(state, elements, elementsById)
        linkChildrenToParent(state, elementsById)
        // Not all elements have DiagramElementId
        val elementsByDiagramId = mutableMapOf<DiagramElementId, BaseRenderElement>()
        linkDiagramElementId(root, elementsByDiagramId)

        root.applyContextChanges(elementsByDiagramId)
        val rendered = root.render()
        // Overlay system elements on top of rendered
        drawSelectionRect(ctx)
        return rendered
    }

    private fun createRootProcessElem(state: RenderState, elements: MutableList<BaseRenderElement>, elementsById: MutableMap<BpmnElementId, BaseRenderElement>): BaseRenderElement {
        val processElem = PlaneRenderElement(DiagramElementId(state.currentState.processId.id), state, mutableListOf())
        elements += processElem
        elementsById[state.currentState.processId] = processElem
        return processElem
    }

    private fun createShapes(state: RenderState, elements: MutableList<BaseRenderElement>, elementsById: MutableMap<BpmnElementId, BaseRenderElement>) {
        state.currentState.shapes.forEach {
            val elem = state.currentState.elementByBpmnId[it.bpmnElement]
            elem?.let { bpmn ->
                mapFromShape(state, it.id, it, bpmn.element)?.let { shape ->
                    elements += shape
                    elementsById[bpmn.id] = shape
                }
            }
        }
    }

    private fun createEdges(state: RenderState, elements: MutableList<BaseRenderElement>, elementsById: MutableMap<BpmnElementId, BaseRenderElement>) {
        state.currentState.edges.forEach {
            val edge = EdgeRenderElement(it.id, it, state)
            elements += edge
            elementsById[it.bpmnElement!!] = edge
        }
    }

    private fun linkChildrenToParent(state: RenderState, elementsById: MutableMap<BpmnElementId, BaseRenderElement>) {
        elementsById.forEach { (id, renderElem) ->
            val elem = state.currentState.elementByBpmnId[id]
            elem?.parent?.let { elementsById[it]?.children?.add(renderElem) }
        }
    }

    private fun linkDiagramElementId(root: BaseRenderElement, elementsByDiagramId: MutableMap<DiagramElementId, BaseRenderElement>) {
        elementsByDiagramId[root.elementId] = root
        root.children.forEach { linkDiagramElementId(it, elementsByDiagramId)}
    }

    private fun mapFromShape(state: RenderState, id: DiagramElementId, shape: ShapeElement, bpmn: WithBpmnId): BaseRenderElement? {
        val icons = state.icons
        return when (bpmn) {
            is BpmnStartEvent -> EllipticIconOnLayerShape(id, icons.startEvent, shape, state, Colors.START_EVENT)
            is BpmnStartEscalationEvent -> EllipticIconOnLayerShape(id, icons.escalationStartEvent, shape, state, Colors.START_EVENT)
            is BpmnStartConditionalEvent -> EllipticIconOnLayerShape(id, icons.conditionalStartEvent, shape, state, Colors.START_EVENT)
            is BpmnStartErrorEvent -> EllipticIconOnLayerShape(id, icons.errorStartEvent, shape, state, Colors.START_EVENT)
            is BpmnStartMessageEvent -> EllipticIconOnLayerShape(id, icons.messageStartEvent, shape, state, Colors.START_EVENT)
            is BpmnStartSignalEvent -> EllipticIconOnLayerShape(id, icons.signalStartEvent, shape, state, Colors.START_EVENT)
            is BpmnStartTimerEvent -> EllipticIconOnLayerShape(id, icons.timerStartEvent, shape, state, Colors.START_EVENT)
            is BpmnBoundaryCancelEvent -> IconShape(id, icons.boundaryCancelEvent, shape, state)
            is BpmnBoundaryCompensationEvent -> IconShape(id, icons.boundaryCompensationEvent, shape, state)
            is BpmnBoundaryConditionalEvent -> IconShape(id, icons.boundaryConditionalEvent, shape, state)
            is BpmnBoundaryErrorEvent -> IconShape(id, icons.boundaryErrorEvent, shape, state)
            is BpmnBoundaryEscalationEvent -> IconShape(id, icons.boundaryEscalationEvent, shape, state)
            is BpmnBoundaryMessageEvent -> IconShape(id, icons.boundaryMessageEvent, shape, state)
            is BpmnBoundarySignalEvent -> IconShape(id, icons.boundarySignalEvent, shape, state)
            is BpmnBoundaryTimerEvent -> IconShape(id, icons.boundaryTimerEvent, shape, state)
            is BpmnUserTask -> TopLeftIconShape(id, icons.user, shape, state)
            is BpmnScriptTask -> TopLeftIconShape(id, icons.script, shape, state)
            is BpmnServiceTask -> TopLeftIconShape(id, icons.gear, shape, state)
            is BpmnBusinessRuleTask -> TopLeftIconShape(id, icons.businessRule, shape, state)
            is BpmnReceiveTask -> TopLeftIconShape(id, icons.receive, shape, state)
            is BpmnCamelTask -> TopLeftIconShape(id, icons.camel, shape, state)
            is BpmnHttpTask -> TopLeftIconShape(id, icons.http, shape, state)
            is BpmnMuleTask -> TopLeftIconShape(id, icons.mule, shape, state)
            is BpmnDecisionTask -> TopLeftIconShape(id, icons.decision, shape, state)
            is BpmnShellTask -> TopLeftIconShape(id, icons.shell, shape, state)
            is BpmnSubProcess -> NoIconShape(id, shape, state, Colors.PROCESS_COLOR, Colors.ELEMENT_BORDER_COLOR, Colors.SUBPROCESS_TEXT_COLOR)
            is BpmnTransactionalSubProcess -> NoIconDoubleBorderShape(id, shape, state)
            is BpmnCallActivity -> NoIconShape(id, shape, state)
            is BpmnAdHocSubProcess -> BottomMiddleIconShape(id, icons.tilde, shape, state)
            is BpmnExclusiveGateway -> IconShape(id, icons.exclusiveGateway, shape, state)
            is BpmnParallelGateway -> IconShape(id, icons.parallelGateway, shape, state)
            is BpmnInclusiveGateway -> IconShape(id, icons.inclusiveGateway, shape, state)
            is BpmnEventGateway -> IconShape(id, icons.eventGateway, shape, state)
            is BpmnEndEvent -> EllipticIconOnLayerShape(id, icons.endEvent, shape, state, Colors.END_EVENT)
            is BpmnEndCancelEvent -> EllipticIconOnLayerShape(id, icons.cancelEndEvent, shape, state, Colors.END_EVENT)
            is BpmnEndErrorEvent -> EllipticIconOnLayerShape(id, icons.errorEndEvent, shape, state, Colors.END_EVENT)
            is BpmnEndEscalationEvent -> EllipticIconOnLayerShape(id, icons.escalationEndEvent, shape, state, Colors.END_EVENT)
            is BpmnEndTerminateEvent -> EllipticIconOnLayerShape(id, icons.terminateEndEvent, shape, state, Colors.END_EVENT)
            is BpmnIntermediateTimerCatchingEvent -> IconShape(id, icons.timerCatchEvent, shape, state)
            is BpmnIntermediateMessageCatchingEvent -> IconShape(id, icons.messageCatchEvent, shape, state)
            is BpmnIntermediateSignalCatchingEvent -> IconShape(id, icons.signalCatchEvent, shape, state)
            is BpmnIntermediateConditionalCatchingEvent -> IconShape(id, icons.conditionalCatchEvent, shape, state)
            is BpmnIntermediateNoneThrowingEvent -> IconShape(id, icons.noneThrowEvent, shape, state)
            is BpmnIntermediateSignalThrowingEvent -> IconShape(id, icons.signalThrowEvent, shape, state)
            is BpmnIntermediateEscalationThrowingEvent -> IconShape(id, icons.escalationThrowEvent, shape, state)
            else -> throw IllegalArgumentException("Unknown shape: ${bpmn.javaClass}")
        }
    }

    private fun drawSelectionRect(ctx: RenderContext) {
        ctx.interactionContext.dragSelectionRect?.let {
            val rect = it.toRect()
            ctx.canvas.drawRectNoCameraTransform(Point2D.Float(rect.x, rect.y), rect.width, rect.height, ACTION_AREA_STROKE, Colors.ACTIONS_BORDER_COLOR.color)
        }
    }
}

class DefaultBpmnProcessRendererOld(val icons: IconProvider) : BpmnProcessRenderer {

    private val minShapeSize = 40.0f
    private val transactionalBoundaryMargin = 5.0f
    private val undoRedoStartMargin = 20.0f
    private val waypointLen = 40.0f
    private val activityToolBoxGap = 8.0f
    private val anchorRadius = 5f
    private val nodeRadius = 3f
    private val actionsIcoSize = 15f
    private val actionsMargin = 5f

    private val undoId = DiagramElementId("UNDO")
    private val redoId = DiagramElementId("REDO")

    private val ANCHOR_STROKE = BasicStroke(1.0f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0.0f, floatArrayOf(5.0f), 0.0f)
    private val ACTION_AREA_STROKE = BasicStroke(2.0f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0.0f, floatArrayOf(2.0f), 0.0f)

    override fun render(ctx: RenderContext): Map<DiagramElementId, AreaWithZindex> {
        val state = ctx.stateProvider.currentState()
        val areaByElement: MutableMap<DiagramElementId, AreaWithZindex> = HashMap()
        val renderMeta = RenderMetadata(
                ctx.interactionContext,
                ctx.selectedIds,
                state.elementByDiagramId,
                state.elementByBpmnId,
                state.elemPropertiesByStaticElementId,
                computeCascadables(ctx, state)
        )

        // Shapes are drawn first as they can cause cascaded location updates in edges
        dramBpmnElements(state.shapes, areaByElement, ctx.canvas, renderMeta)
        drawBpmnEdges(state.edges, areaByElement, ctx.canvas, renderMeta)
        ctx.interactionContext.anchorsHit?.apply { drawAnchorsHit(ctx.canvas, this) }

        drawUndoRedo(ctx, state, renderMeta, areaByElement)
        //drawSelectionRect(ctx)
        drawMultiremovalRect(ctx, renderMeta, areaByElement)
        drawDebugElements(state, ctx, areaByElement)
        return areaByElement
    }

    private fun drawDebugElements(ctx: CurrentState, renderCtx: RenderContext, renderedArea: Map<DiagramElementId, AreaWithZindex>) {
        currentDebugger()?.executionSequence(ctx.processId.id)?.history?.let { history ->
            val elemToDiagramId = mutableMapOf<BpmnElementId, MutableSet<DiagramElementId>>()
            ctx.elementByDiagramId.forEach { elemToDiagramId.computeIfAbsent(it.value) { mutableSetOf() }.add(it.key) }
            val elemOccurs = mutableMapOf<BpmnElementId, MutableList<Int>>()
            history.forEachIndexed { index, elem -> elemOccurs.computeIfAbsent(elem) { mutableListOf() }.add(index) }

            elemOccurs.forEach { (elem, indexes) ->
                val targetElem = elemToDiagramId[elem]?.firstOrNull()
                renderedArea[targetElem]?.apply {
                    val bounds = this.area.bounds2D
                    renderCtx.canvas.drawTextNoCameraTransform(Point2D.Float(bounds.x.toFloat(), bounds.y.toFloat()), indexes.toString(), Colors.INNER_TEXT_COLOR.color)
                }
            }

        }
    }



    private fun drawMultiremovalRect(renderCtx: RenderContext, ctx: RenderMetadata, areaByElement: MutableMap<DiagramElementId, AreaWithZindex>) {
        if (null != ctx.interactionContext.dragSelectionRect || ctx.selectedIds.size <= 1) {
            return
        }

        val parentChild = ctx.selectedIds.flatMap { setOf(it, areaByElement[it]?.parentToSelect) }.filterNotNull()

        if (ctx.selectedIds.size == 2 && ctx.selectedIds.containsAll(parentChild)) {
            return
        }

        val areas = ctx.selectedIds.map { areaByElement[it] }.filterNotNull()

        val minX = areas.map { it.area.bounds2D.minX }.min()?.toFloat()
        val minY = areas.map { it.area.bounds2D.minY }.min()?.toFloat()
        val maxX = areas.map { it.area.bounds2D.maxX }.max()?.toFloat()
        val maxY = areas.map { it.area.bounds2D.maxY }.max()?.toFloat()

        if (null != minX && null != minY && null != maxX && null != maxY) {
            val ownerId = ctx.selectedIds.joinToString { it.id }
            renderCtx.canvas.drawRectNoCameraTransform(Point2D.Float(minX, minY), maxX - minX, maxY - minY, ACTION_AREA_STROKE, Colors.ACTIONS_BORDER_COLOR.color)
            val delId = DiagramElementId("DEL:$ownerId")
            val deleteIconArea = renderCtx.canvas.drawIconNoCameraTransform(BoundsElement(maxX, minY, actionsIcoSize, actionsIcoSize), icons.recycleBin)
            ctx.interactionContext.clickCallbacks[delId] = { dest ->
                val targetIds = ctx.selectedIds.filter { areaByElement[it]?.areaType == AreaType.SHAPE || areaByElement[it]?.areaType == AreaType.EDGE }
                dest.addElementRemovedEvent(
                        targetIds.map { DiagramElementRemovedEvent(it) },
                        targetIds.mapNotNull { ctx.elementByDiagramId[it] }.map { BpmnElementRemovedEvent(it) }
                )
            }
            areaByElement[delId] = AreaWithZindex(deleteIconArea, AreaType.POINT, mutableSetOf(), mutableSetOf(), ANCHOR_Z_INDEX, null)
        }
    }

    private fun drawUndoRedo(ctx: RenderContext, state: CurrentState, meta: RenderMetadata, areaByElement: MutableMap<DiagramElementId, AreaWithZindex>) {
        val start = Point2D.Float(undoRedoStartMargin, undoRedoStartMargin)
        var locationX = start.x
        val locationY = start.y

        if (state.undoRedo.contains(ProcessModelUpdateEvents.UndoRedo.UNDO)) {
            val color = if (isActive(undoId, meta)) Colors.SELECTED_COLOR else null
            val areaUndo = color?.let { ctx.canvas.drawIconAtScreen(Point2D.Float(locationX, locationY), icons.undo, it.color) }
                    ?: ctx.canvas.drawIconAtScreen(Point2D.Float(locationX, locationY), icons.undo)
            areaByElement[undoId] = AreaWithZindex(areaUndo, AreaType.SHAPE)
            locationX += icons.undo.iconWidth + undoRedoStartMargin
            ctx.interactionContext.clickCallbacks[undoId] = { dest -> dest.undo() }
        }

        if (state.undoRedo.contains(ProcessModelUpdateEvents.UndoRedo.REDO)) {
            val color = if (isActive(redoId, meta)) Colors.SELECTED_COLOR else null
            val areaRedo = color?.let { ctx.canvas.drawIconAtScreen(Point2D.Float(locationX, locationY), icons.redo, it.color) }
                    ?: ctx.canvas.drawIconAtScreen(Point2D.Float(locationX, locationY), icons.redo)
            areaByElement[redoId] = AreaWithZindex(areaRedo, AreaType.SHAPE)
            locationX += icons.redo.iconWidth + undoRedoStartMargin
            ctx.interactionContext.clickCallbacks[redoId] = { dest -> dest.redo() }
        }
    }

    private fun computeCascadables(ctx: RenderContext, state: CurrentState): Set<CascadeTranslationToWaypoint> {
        val idCascadesTo = setOf(SOURCE_REF, TARGET_REF)
        val result = mutableSetOf<CascadeTranslationToWaypoint>()
        val elemToDiagramId = mutableMapOf<BpmnElementId, MutableSet<DiagramElementId>>()
        state.elementByDiagramId.forEach { elemToDiagramId.computeIfAbsent(it.value) { mutableSetOf() }.add(it.key) }
        ctx.interactionContext.draggedIds.mapNotNull { state.elementByDiagramId[it] }.filter { state.elementByBpmnId[it]?.element !is BpmnSequenceFlow }.forEach { parent ->
            state.elemPropertiesByStaticElementId.forEach { (owner, props) ->
                idCascadesTo.intersect(props.keys).filter { props[it]?.value == parent.id }.forEach { type ->
                    when (state.elementByBpmnId[owner]?.element) {
                        is BpmnSequenceFlow -> result += computeCascadeToWaypoint(state, parent, owner, type)
                    }
                }

            }
        }
        return result
    }

    private fun computeCascadeToWaypoint(state: CurrentState, cascadeTrigger: BpmnElementId, owner: BpmnElementId, type: PropertyType): Collection<CascadeTranslationToWaypoint> {
        return state.edges
                .filter { it.bpmnElement == owner }
                .map {
                    val index = if (type == SOURCE_REF) 0 else it.waypoint.size - 1
                    val waypoint = it.waypoint[index]
                    CascadeTranslationToWaypoint(cascadeTrigger, waypoint.id, Point2D.Float(waypoint.x, waypoint.y) , it.id, waypoint.internalPhysicalPos)
                }
    }

    private fun drawBpmnEdges(shapes: List<EdgeWithIdentifiableWaypoints>, areaByElement: MutableMap<DiagramElementId, AreaWithZindex>, canvas: CanvasPainter, renderMeta: RenderMetadata) {
        shapes.forEach {
            mergeArea(it.id, areaByElement, drawEdgeElement(canvas, it, renderMeta))
            if (isActive(it.id, renderMeta)) {
                drawWaypointElements(canvas, it, renderMeta).forEach { waypoint ->
                    mergeArea(waypoint.key, areaByElement, waypoint.value)
                }

                val deleteCallback = { dest: ProcessModelUpdateEvents ->
                    val bpmnRemoves = mutableListOf<BpmnElementRemovedEvent>()
                    val diagramRemoves = mutableListOf<DiagramElementRemovedEvent>()
                    it.bpmnElement?.apply { bpmnRemoves += BpmnElementRemovedEvent(this) }
                    diagramRemoves += DiagramElementRemovedEvent(it.id)
                    dest.addElementRemovedEvent(diagramRemoves, bpmnRemoves)
                }
                if (isDeepStructureWithActions(renderMeta)) {
                    val actionsElem = drawActionsElement(canvas, it, renderMeta.interactionContext, mutableMapOf(Actions.DELETE to deleteCallback))
                    areaByElement += actionsElem
                }
            }
        }
    }

    private fun isDeepStructure(renderMeta: RenderMetadata): Boolean {
        return null == renderMeta.interactionContext.dragSelectionRect
    }

    private fun isDeepStructureWithActions(renderMeta: RenderMetadata): Boolean {
        return renderMeta.selectedIds.size == 1 && null == renderMeta.interactionContext.dragSelectionRect
    }

    private fun dramBpmnElements(shapes: List<ShapeElement>, areaByElement: MutableMap<DiagramElementId, AreaWithZindex>, canvas: CanvasPainter, renderMeta: RenderMetadata) {
        shapes.forEach {
            val anchors = it.bounds()
            val shapeRect = computeShapeRect(renderMeta, anchors, it)

            mergeArea(it.id, areaByElement, drawShapeElement(canvas, it, shapeRect, renderMeta))
            if (isActive(it.id, renderMeta)) {
                val deleteCallback = { dest: ProcessModelUpdateEvents ->
                    dest.addElementRemovedEvent(listOf(DiagramElementRemovedEvent(it.id)), listOf(BpmnElementRemovedEvent(it.bpmnElement)))
                }
                val newSequenceCallback = { dest: ProcessModelUpdateEvents ->
                    val elem = renderMeta.elementById[it.bpmnElement]
                    if (null != elem) {
                        val newSequenceBpmn = newElementsFactory().newOutgoingSequence(elem)
                        val bounds = it.bounds()
                        val width = bounds.second.x - bounds.first.x
                        val height = bounds.second.y - bounds.first.y

                        val newSequenceDiagram = newElementsFactory().newDiagramObject(EdgeElement::class, newSequenceBpmn)
                                .copy(waypoint = listOf(
                                        WaypointElement(bounds.first.x + width, bounds.first.y + height / 2.0f),
                                        WaypointElement(bounds.first.x + width + waypointLen, bounds.first.y + height / 2.0f)
                                ))
                        dest.addObjectEvent(BpmnEdgeObjectAddedEvent(WithParentId(BpmnElementId("PARENT"), newSequenceBpmn), EdgeElementState(newSequenceDiagram), newElementsFactory().propertiesOf(newSequenceBpmn)))
                    }
                }
                if (isDeepStructureWithActions(renderMeta)) {
                    val bounds = it.rectBounds()
                    val actionsElem = drawActionsElement(canvas, it.id, bounds, renderMeta.interactionContext, mutableMapOf(Actions.DELETE to deleteCallback, Actions.NEW_LINK to newSequenceCallback))
                    areaByElement += actionsElem
                }

                if (isDeepStructure(renderMeta)) {
                    areaByElement += drawResizeIconTop(canvas, shapeRect, it.rectBounds(), anchors.first, renderMeta)
                    areaByElement += drawResizeIconBottom(canvas, shapeRect, anchors.second, renderMeta)
                }

                if (!isActive(anchors.first.id, renderMeta) && !isActive(anchors.second.id, renderMeta)) {
                    renderMeta.interactionContext.dragEndCallbacks[it.id] = OnDragEnd@{ dx: Float, dy: Float, _: BpmnElementId? ->
                        val events = mutableListOf<DraggedToEvent>()
                        events += DraggedToEvent(it.id, dx, dy, null, null)
                        events += renderMeta
                                .cascadedTransalationOf
                                .filter { target -> target.cascadeSource == it.bpmnElement }
                                .filter { target -> !renderMeta.interactionContext.draggedIds.contains(target.waypointId) }
                                .map { cascadeTo -> DraggedToEvent(cascadeTo.waypointId, dx, dy, cascadeTo.parentEdgeId, cascadeTo.internalId) }
                        return@OnDragEnd events
                    }
                }
            }
        }
    }

    private fun drawAnchorsHit(canvas: CanvasPainter, anchors: AnchorHit) {
        anchors.anchors.forEach {
            when (it.key) {
                AnchorType.VERTICAL, AnchorType.HORIZONTAL -> canvas.drawZeroAreaLine(it.value, anchors.objectAnchor, ANCHOR_STROKE, Colors.ANCHOR_COLOR.color)
                AnchorType.POINT -> canvas.drawCircle(it.value, anchorRadius, Colors.ANCHOR_COLOR.color)
            }
        }
    }

    private fun mergeArea(id: DiagramElementId, areas: MutableMap<DiagramElementId, AreaWithZindex>, area: AreaWithZindex) {
        val target = areas[id] ?: AreaWithZindex(Area(), area.areaType)
        target.area.add(area.area)
        target.anchorsForShape += area.anchorsForShape
        target.anchorsForWaypoints += area.anchorsForWaypoints
        areas[id] = AreaWithZindex(target.area, target.areaType, target.anchorsForWaypoints, target.anchorsForShape, min(target.index, area.index), target.parentToSelect
                ?: area.parentToSelect)
    }

    private fun drawEdgeElement(canvas: CanvasPainter, shape: EdgeWithIdentifiableWaypoints, meta: RenderMetadata): AreaWithZindex {
        val active = isActive(shape.id, meta)
        val area = Area()

        val color = color(active, Colors.ARROW_COLOR)
        val trueWaypoints = calculateTrueWaypoints(shape, meta)
        trueWaypoints.forEachIndexed { index, it ->
            when {
                index == trueWaypoints.size - 1 -> area.add(drawEdge(canvas, trueWaypoints[index - 1], it, meta, color, true))
                index > 0 -> area.add(drawEdge(canvas, trueWaypoints[index - 1], it, meta, color, false))
            }
        }

        return AreaWithZindex(area, AreaType.EDGE, if (active) mutableSetOf() else trueWaypoints.map { Point2D.Float(it.x, it.y) }.toMutableSet())
    }

    private fun drawWaypointElements(canvas: CanvasPainter, shape: EdgeWithIdentifiableWaypoints, meta: RenderMetadata): Map<DiagramElementId, AreaWithZindex> {
        val area = HashMap<DiagramElementId, AreaWithZindex>()
        val trueWaypoints = calculateTrueWaypoints(shape, meta)
        // draw all endpoints only if none virtual is dragged and not physical
        val isVirtualDragged = meta.interactionContext.draggedIds.intersect(shape.waypoint.filter { !it.physical }.map { it.id }).isNotEmpty()
        val isPhysicalDragged = meta.interactionContext.draggedIds.intersect(shape.waypoint.filter { it.physical }.map { it.id }).isNotEmpty()
        val waypoints = if (!isDeepStructure(meta) || (isVirtualDragged || isPhysicalDragged)) trueWaypoints else shape.waypoint
        waypoints.forEachIndexed { index, it ->
            when {
                index == waypoints.size - 1 -> area.putAll(drawWaypointAnchors(canvas, waypoints[index - 1], it, shape, meta, true, index))
                index > 0 -> area.putAll(drawWaypointAnchors(canvas, waypoints[index - 1], it, shape, meta, false, index))
            }
        }

        return area
    }

    private fun drawWaypointAnchors(canvas: CanvasPainter, begin: IdentifiableWaypoint, end: IdentifiableWaypoint, parent: EdgeWithIdentifiableWaypoints, meta: RenderMetadata, isLast: Boolean, endWaypointIndex: Int): Map<DiagramElementId, AreaWithZindex> {
        val result = HashMap<DiagramElementId, AreaWithZindex>()

        val dragCallback = DragCallback@{ dx: Float, dy: Float, elem: IdentifiableWaypoint, droppedOn: BpmnElementId? ->
            val events = mutableListOf<Event>()
            val index = parent.waypoint.indexOf(elem)
            if (elem.physical) {
                events += DraggedToEvent(elem.id, dx, dy, parent.id, elem.internalPhysicalPos)
                if (null != droppedOn && null != parent.bpmnElement
                        && meta.interactionContext.draggedIds.containsAll(setOf(elem.id, parent.id))
                        && meta.interactionContext.draggedIds.size == 2) {
                    if (parent.waypoint.size - 1 == index) {
                        events += StringValueUpdatedEvent(parent.bpmnElement!!, TARGET_REF, droppedOn.id)
                    } else if (0 == index) {
                        events += StringValueUpdatedEvent(parent.bpmnElement!!, SOURCE_REF, droppedOn.id)
                    }
                }
            } else {
                events += NewWaypointsEvent(
                        parent.id,
                        parent.waypoint
                                .filter { it.physical || it.id == elem.id }
                                .map { if (it.id == elem.id && !it.physical) it.moveTo(dx, dy).asPhysical() else it }
                                .toList(),
                        parent.epoch + 1
                )
            }
            return@DragCallback events
        }

        val drawNode = { node: IdentifiableWaypoint, index: Int ->
            val translatedNode = transformElementIfNeeded(meta, node)
            val active = isActive(node.id, meta)
            val color = color(active, if (node.physical) Colors.WAYPOINT_COLOR else Colors.MID_WAYPOINT_COLOR)
            result[node.id] = AreaWithZindex(
                    canvas.drawCircle(translatedNode.asWaypointElement(), nodeRadius, color),
                    AreaType.POINT,
                    mutableSetOf(Point2D.Float(translatedNode.x, translatedNode.y)),
                    mutableSetOf(),
                    ANCHOR_Z_INDEX,
                    parent.id
            )

            meta.interactionContext.dragEndCallbacks[node.id] = { dx: Float, dy: Float, droppedOn: BpmnElementId? -> dragCallback(dx, dy, node, droppedOn) }

            if (active && node.physical && index > 0 && (index < parent.waypoint.size - 1)) {
                val callback = { dest: ProcessModelUpdateEvents ->
                    dest.addEvents(listOf(NewWaypointsEvent(
                            parent.id,
                            parent.waypoint
                                    .filter { it.physical }
                                    .filter { it.id != node.id }
                                    .toList(),
                            parent.epoch + 1
                    )))
                }
                if (active && onlyWaypointAndEdgeSelected(meta, node, parent)) {
                    result += drawActionsElement(canvas, translatedNode, meta.interactionContext, mapOf(Actions.DELETE to callback))
                }
            }
        }

        if (isLast) {
            drawNode(begin, endWaypointIndex - 1)
            drawNode(end, endWaypointIndex)
            return result
        }

        drawNode(begin, endWaypointIndex - 1)
        meta.interactionContext.dragEndCallbacks[begin.id] = { dx: Float, dy: Float, droppedOn: BpmnElementId? -> dragCallback(dx, dy, begin, droppedOn) }
        return result
    }

    private fun onlyWaypointAndEdgeSelected(meta: RenderMetadata, node: IdentifiableWaypoint, parent: EdgeWithIdentifiableWaypoints) =
            meta.selectedIds.containsAll(setOf(node.id, parent.id)) && meta.selectedIds.size == 2

    private fun calculateTrueWaypoints(shape: EdgeWithIdentifiableWaypoints, meta: RenderMetadata): List<IdentifiableWaypoint> {
        return shape.waypoint.filter { it.physical || isActive(it.id, meta) }
    }

    private fun drawEdge(canvas: CanvasPainter, begin: IdentifiableWaypoint, end: IdentifiableWaypoint, meta: RenderMetadata, color: Color, isLast: Boolean): Area {
        val translatedBegin = transformElementIfNeeded(meta, begin)
        val translatedEnd = transformElementIfNeeded(meta, end)
        if (isLast) {
            return canvas.drawLineWithArrow(translatedBegin.asWaypointElement(), translatedEnd.asWaypointElement(), color)
        }

        return canvas.drawLine(translatedBegin.asWaypointElement(), translatedEnd.asWaypointElement(), color)
    }

    private fun drawShapeElement(canvas: CanvasPainter, bpmnShape: ShapeElement, targetRect: Rectangle2D.Float, meta: RenderMetadata): AreaWithZindex {
        val elem = meta.elementById[bpmnShape.bpmnElement]
        val props = meta.elemPropertiesByElementId[bpmnShape.bpmnElement]
        val name = props?.get(NAME)?.value as String?
        val active = isActive(bpmnShape.id, meta)

        return when (elem) {
            null -> defaultElementRender(canvas, targetRect, name, active)
            is BpmnStartEvent -> drawStartEvent(canvas, targetRect, active)
            is BpmnStartEscalationEvent -> drawStartEscalationEvent(canvas, targetRect, active)
            is BpmnStartConditionalEvent -> drawStartConditionalEvent(canvas, targetRect, active)
            is BpmnStartErrorEvent -> drawStartErrorEvent(canvas, targetRect, active)
            is BpmnStartMessageEvent -> drawStartMessageEvent(canvas, targetRect, active)
            is BpmnStartSignalEvent -> drawStartSignalEvent(canvas, targetRect, active)
            is BpmnStartTimerEvent -> drawStartTimerEvent(canvas, targetRect, active)
            is BpmnBoundaryCancelEvent -> drawBoundaryCancelEvent(canvas, targetRect, active)
            is BpmnBoundaryCompensationEvent -> drawBoundaryCompensationEvent(canvas, targetRect, active)
            is BpmnBoundaryConditionalEvent -> drawBoundaryConditionalEvent(canvas, targetRect, active)
            is BpmnBoundaryErrorEvent -> drawBoundaryErrorEvent(canvas, targetRect, active)
            is BpmnBoundaryEscalationEvent -> drawBoundaryEscalationEvent(canvas, targetRect, active)
            is BpmnBoundaryMessageEvent -> drawBoundaryMessageEvent(canvas, targetRect, active)
            is BpmnBoundarySignalEvent -> drawBoundarySignalEvent(canvas, targetRect, active)
            is BpmnBoundaryTimerEvent -> drawBoundaryTimerEvent(canvas, targetRect, active)
            is BpmnUserTask -> drawUserTask(canvas, targetRect, name, active)
            is BpmnScriptTask -> drawScriptTask(canvas, targetRect, name, active)
            is BpmnServiceTask -> drawServiceTask(canvas, targetRect, name, active)
            is BpmnBusinessRuleTask -> drawBusinessRuleTask(canvas, targetRect, name, active)
            is BpmnReceiveTask -> drawReceiveRuleTask(canvas, targetRect, name, active)
            is BpmnCamelTask -> drawCamelTask(canvas, targetRect, name, active)
            is BpmnHttpTask -> drawHttpTask(canvas, targetRect, name, active)
            is BpmnMuleTask -> drawMuleTask(canvas, targetRect, name, active)
            is BpmnDecisionTask -> drawDecisionTask(canvas, targetRect, name, active)
            is BpmnShellTask -> drawShellTask(canvas, targetRect, name, active)
            is BpmnSubProcess -> drawSubProcess(canvas, targetRect, name, active)
            is BpmnTransactionalSubProcess -> drawTransactionSubProcess(canvas, targetRect, name, active)
            is BpmnCallActivity -> drawCallActivity(canvas, targetRect, name, active)
            is BpmnAdHocSubProcess -> drawAdHocSubProcess(canvas, targetRect, name, active)
            is BpmnExclusiveGateway -> drawExclusiveGateway(canvas, targetRect, active)
            is BpmnParallelGateway -> drawParallelGateway(canvas, targetRect, active)
            is BpmnInclusiveGateway -> drawInclusiveGateway(canvas, targetRect, active)
            is BpmnEventGateway -> drawEventGateway(canvas, targetRect, active)
            is BpmnEndEvent -> drawEndEvent(canvas, targetRect, active)
            is BpmnEndCancelEvent -> drawCancelEndEvent(canvas, targetRect, active)
            is BpmnEndErrorEvent -> drawErrorEndEvent(canvas, targetRect, active)
            is BpmnEndEscalationEvent -> drawEscalationEndEvent(canvas, targetRect, active)
            is BpmnEndTerminateEvent -> drawTerminateEndEvent(canvas, targetRect, active)
            is BpmnIntermediateTimerCatchingEvent -> drawTimerCatchEvent(canvas, targetRect, active)
            is BpmnIntermediateMessageCatchingEvent -> drawMessageCatchEvent(canvas, targetRect, active)
            is BpmnIntermediateSignalCatchingEvent -> drawSignalCatchEvent(canvas, targetRect, active)
            is BpmnIntermediateConditionalCatchingEvent -> drawConditionalCatchEvent(canvas, targetRect, active)
            is BpmnIntermediateNoneThrowingEvent -> drawNoneThrowEvent(canvas, targetRect, active)
            is BpmnIntermediateSignalThrowingEvent -> drawSignalThrowEvent(canvas, targetRect, active)
            is BpmnIntermediateEscalationThrowingEvent -> drawEscalationThrowEvent(canvas, targetRect, active)
            else -> AreaWithZindex(Area(), AreaType.SHAPE)
        }
    }

    private fun computeShapeRect(meta: RenderMetadata, bounds: Pair<ShapeBoundsAnchorElement, ShapeBoundsAnchorElement>, bpmnShape: ShapeElement): Rectangle2D.Float {
        when {
            shouldTransform(meta, bounds.first.id) -> {
                val left = transformElementIfNeeded(meta, bounds.first)
                val right = bounds.second
                return Rectangle2D.Float(
                        min(right.x - minShapeSize, left.x),
                        min(right.y - minShapeSize, left.y),
                        max(minShapeSize, right.x - left.x),
                        max(minShapeSize, right.y - left.y))
            }

            shouldTransform(meta, bounds.second.id) -> {
                val left = bounds.first
                val right = transformElementIfNeeded(meta, bounds.second)
                return Rectangle2D.Float(
                        left.x,
                        left.y,
                        max(minShapeSize, right.x - left.x),
                        max(minShapeSize, right.y - left.y))
            }

            else -> {
                val shape = transformElementIfNeeded(meta, bpmnShape)
                return shape.rectBounds()
            }
        }
    }

    private fun <T> transformElementIfNeeded(meta: RenderMetadata, elem: T): T where T : Translatable<T>, T : WithDiagramId {
        return if (shouldTransform(meta, elem.id)) {
            elem.copyAndTranslate(
                    meta.interactionContext.dragCurrent.x - meta.interactionContext.dragStart.x,
                    meta.interactionContext.dragCurrent.y - meta.interactionContext.dragStart.y
            )
        } else {
            elem
        }
    }

    private fun shouldTransform(meta: RenderMetadata, elemId: DiagramElementId) =
            meta.interactionContext.draggedIds.contains(elemId) || meta.cascadedTransalationOf.map { it.waypointId }.contains(elemId)

    private fun drawBoundaryCancelEvent(canvas: CanvasPainter, shape: Rectangle2D.Float, active: Boolean): AreaWithZindex {
        val area = canvas.drawWrappedIcon(shape, icons.boundaryCancelEvent, active, Colors.SELECTED_COLOR.color)
        return AreaWithZindex(area, AreaType.SHAPE, ellipseOrDiamondAnchors(shape), centerAnchor(shape))
    }

    private fun drawBoundaryCompensationEvent(canvas: CanvasPainter, shape: Rectangle2D.Float, active: Boolean): AreaWithZindex {
        val area = canvas.drawWrappedIcon(shape, icons.boundaryCompensationEvent, active, Colors.SELECTED_COLOR.color)
        return AreaWithZindex(area, AreaType.SHAPE, ellipseOrDiamondAnchors(shape), centerAnchor(shape))
    }

    private fun drawBoundaryConditionalEvent(canvas: CanvasPainter, shape: Rectangle2D.Float, active: Boolean): AreaWithZindex {
        val area = canvas.drawWrappedIcon(shape, icons.boundaryConditionalEvent, active, Colors.SELECTED_COLOR.color)
        return AreaWithZindex(area, AreaType.SHAPE, ellipseOrDiamondAnchors(shape), centerAnchor(shape))
    }

    private fun drawBoundaryErrorEvent(canvas: CanvasPainter, shape: Rectangle2D.Float, active: Boolean): AreaWithZindex {
        val area = canvas.drawWrappedIcon(shape, icons.boundaryErrorEvent, active, Colors.SELECTED_COLOR.color)
        return AreaWithZindex(area, AreaType.SHAPE, ellipseOrDiamondAnchors(shape), centerAnchor(shape))
    }

    private fun drawBoundaryEscalationEvent(canvas: CanvasPainter, shape: Rectangle2D.Float, active: Boolean): AreaWithZindex {
        val area = canvas.drawWrappedIcon(shape, icons.boundaryEscalationEvent, active, Colors.SELECTED_COLOR.color)
        return AreaWithZindex(area, AreaType.SHAPE, ellipseOrDiamondAnchors(shape), centerAnchor(shape))
    }

    private fun drawBoundaryMessageEvent(canvas: CanvasPainter, shape: Rectangle2D.Float, active: Boolean): AreaWithZindex {
        val area = canvas.drawWrappedIcon(shape, icons.boundaryMessageEvent, active, Colors.SELECTED_COLOR.color)
        return AreaWithZindex(area, AreaType.SHAPE, ellipseOrDiamondAnchors(shape), centerAnchor(shape))
    }

    private fun drawBoundarySignalEvent(canvas: CanvasPainter, shape: Rectangle2D.Float, active: Boolean): AreaWithZindex {
        val area = canvas.drawWrappedIcon(shape, icons.boundarySignalEvent, active, Colors.SELECTED_COLOR.color)
        return AreaWithZindex(area, AreaType.SHAPE, ellipseOrDiamondAnchors(shape), centerAnchor(shape))
    }

    private fun drawBoundaryTimerEvent(canvas: CanvasPainter, shape: Rectangle2D.Float, active: Boolean): AreaWithZindex {
        val area = canvas.drawWrappedIcon(shape, icons.boundaryTimerEvent, active, Colors.SELECTED_COLOR.color)
        return AreaWithZindex(area, AreaType.SHAPE, ellipseOrDiamondAnchors(shape), centerAnchor(shape))
    }

    private fun drawEndEvent(canvas: CanvasPainter, shape: Rectangle2D.Float, active: Boolean): AreaWithZindex {
        val area = canvas.drawWrappedIconWithLayer(shape, icons.endEvent, active, Colors.SELECTED_COLOR.color, { Ellipse2D.Float(it.x, it.y, it.width, it.height) }, color(active, Colors.END_EVENT))
        return AreaWithZindex(area, AreaType.SHAPE, ellipseOrDiamondAnchors(shape), centerAnchor(shape))
    }

    private fun drawCancelEndEvent(canvas: CanvasPainter, shape: Rectangle2D.Float, active: Boolean): AreaWithZindex {
        val area = canvas.drawWrappedIconWithLayer(shape, icons.cancelEndEvent, active, Colors.SELECTED_COLOR.color, { Ellipse2D.Float(it.x, it.y, it.width, it.height) }, color(active, Colors.END_EVENT))
        return AreaWithZindex(area, AreaType.SHAPE, ellipseOrDiamondAnchors(shape), centerAnchor(shape))
    }

    private fun drawEscalationEndEvent(canvas: CanvasPainter, shape: Rectangle2D.Float, active: Boolean): AreaWithZindex {
        val area = canvas.drawWrappedIconWithLayer(shape, icons.escalationEndEvent, active, Colors.SELECTED_COLOR.color, { Ellipse2D.Float(it.x, it.y, it.width, it.height) }, color(active, Colors.END_EVENT))
        return AreaWithZindex(area, AreaType.SHAPE, ellipseOrDiamondAnchors(shape), centerAnchor(shape))
    }

    private fun drawErrorEndEvent(canvas: CanvasPainter, shape: Rectangle2D.Float, active: Boolean): AreaWithZindex {
        val area = canvas.drawWrappedIconWithLayer(shape, icons.errorEndEvent, active, Colors.SELECTED_COLOR.color, { Ellipse2D.Float(it.x, it.y, it.width, it.height) }, color(active, Colors.END_EVENT))
        return AreaWithZindex(area, AreaType.SHAPE, ellipseOrDiamondAnchors(shape), centerAnchor(shape))
    }

    private fun drawTerminateEndEvent(canvas: CanvasPainter, shape: Rectangle2D.Float, active: Boolean): AreaWithZindex {
        val area = canvas.drawWrappedIconWithLayer(shape, icons.terminateEndEvent, active, Colors.SELECTED_COLOR.color, { Ellipse2D.Float(it.x, it.y, it.width, it.height) }, color(active, Colors.END_EVENT))
        return AreaWithZindex(area, AreaType.SHAPE, ellipseOrDiamondAnchors(shape), centerAnchor(shape))
    }

    private fun drawExclusiveGateway(canvas: CanvasPainter, shape: Rectangle2D.Float, active: Boolean): AreaWithZindex {
        val area = canvas.drawWrappedIcon(shape, icons.exclusiveGateway, active, Colors.SELECTED_COLOR.color)
        return AreaWithZindex(area, AreaType.SHAPE, ellipseOrDiamondAnchors(shape), centerAnchor(shape))
    }

    private fun drawParallelGateway(canvas: CanvasPainter, shape: Rectangle2D.Float, active: Boolean): AreaWithZindex {
        val area = canvas.drawWrappedIcon(shape, icons.parallelGateway, active, Colors.SELECTED_COLOR.color)
        return AreaWithZindex(area, AreaType.SHAPE, ellipseOrDiamondAnchors(shape), centerAnchor(shape))
    }

    private fun drawEventGateway(canvas: CanvasPainter, shape: Rectangle2D.Float, active: Boolean): AreaWithZindex {
        val area = canvas.drawWrappedIcon(shape, icons.eventGateway, active, Colors.SELECTED_COLOR.color)
        return AreaWithZindex(area, AreaType.SHAPE, ellipseOrDiamondAnchors(shape), centerAnchor(shape))
    }

    private fun drawInclusiveGateway(canvas: CanvasPainter, shape: Rectangle2D.Float, active: Boolean): AreaWithZindex {
        val area = canvas.drawWrappedIcon(shape, icons.inclusiveGateway, active, Colors.SELECTED_COLOR.color)
        return AreaWithZindex(area, AreaType.SHAPE, ellipseOrDiamondAnchors(shape), centerAnchor(shape))
    }

    private fun drawCallActivity(canvas: CanvasPainter, shape: Rectangle2D.Float, name: String?, active: Boolean): AreaWithZindex {
        val area = canvas.drawRoundedRect(shape, name, color(active, Colors.CALL_ACTIVITY_COLOR), Colors.ELEMENT_BORDER_COLOR.color, Colors.INNER_TEXT_COLOR.color)
        return AreaWithZindex(area, AreaType.SHAPE, rectangleAnchors(shape), centerAnchor(shape))
    }

    private fun drawServiceTask(canvas: CanvasPainter, shape: Rectangle2D.Float, name: String?, active: Boolean): AreaWithZindex {
        val area = canvas.drawRoundedRectWithIconAtCorner(shape, icons.gear, name, color(active, Colors.SERVICE_TASK_COLOR), Colors.ELEMENT_BORDER_COLOR.color, Colors.INNER_TEXT_COLOR.color)
        return AreaWithZindex(area, AreaType.SHAPE, rectangleAnchors(shape), centerAnchor(shape))
    }

    private fun drawUserTask(canvas: CanvasPainter, shape: Rectangle2D.Float, name: String?, active: Boolean): AreaWithZindex {
        val area = canvas.drawRoundedRectWithIconAtCorner(shape, icons.user, name, color(active, Colors.SERVICE_TASK_COLOR), Colors.ELEMENT_BORDER_COLOR.color, Colors.INNER_TEXT_COLOR.color)
        return AreaWithZindex(area, AreaType.SHAPE, rectangleAnchors(shape), centerAnchor(shape))
    }

    private fun drawScriptTask(canvas: CanvasPainter, shape: Rectangle2D.Float, name: String?, active: Boolean): AreaWithZindex {
        val area = canvas.drawRoundedRectWithIconAtCorner(shape, icons.script, name, color(active, Colors.SERVICE_TASK_COLOR), Colors.ELEMENT_BORDER_COLOR.color, Colors.INNER_TEXT_COLOR.color)
        return AreaWithZindex(area, AreaType.SHAPE, rectangleAnchors(shape), centerAnchor(shape))
    }

    private fun drawBusinessRuleTask(canvas: CanvasPainter, shape: Rectangle2D.Float, name: String?, active: Boolean): AreaWithZindex {
        val area = canvas.drawRoundedRectWithIconAtCorner(shape, icons.businessRule, name, color(active, Colors.SERVICE_TASK_COLOR), Colors.ELEMENT_BORDER_COLOR.color, Colors.INNER_TEXT_COLOR.color)
        return AreaWithZindex(area, AreaType.SHAPE, rectangleAnchors(shape), centerAnchor(shape))
    }

    private fun drawReceiveRuleTask(canvas: CanvasPainter, shape: Rectangle2D.Float, name: String?, active: Boolean): AreaWithZindex {
        val area = canvas.drawRoundedRectWithIconAtCorner(shape, icons.receive, name, color(active, Colors.SERVICE_TASK_COLOR), Colors.ELEMENT_BORDER_COLOR.color, Colors.INNER_TEXT_COLOR.color)
        return AreaWithZindex(area, AreaType.SHAPE, rectangleAnchors(shape), centerAnchor(shape))
    }

    private fun drawCamelTask(canvas: CanvasPainter, shape: Rectangle2D.Float, name: String?, active: Boolean): AreaWithZindex {
        val area = canvas.drawRoundedRectWithIconAtCorner(shape, icons.camel, name, color(active, Colors.SERVICE_TASK_COLOR), Colors.ELEMENT_BORDER_COLOR.color, Colors.INNER_TEXT_COLOR.color)
        return AreaWithZindex(area, AreaType.SHAPE, rectangleAnchors(shape), centerAnchor(shape))
    }

    private fun drawHttpTask(canvas: CanvasPainter, shape: Rectangle2D.Float, name: String?, active: Boolean): AreaWithZindex {
        val area = canvas.drawRoundedRectWithIconAtCorner(shape, icons.http, name, color(active, Colors.SERVICE_TASK_COLOR), Colors.ELEMENT_BORDER_COLOR.color, Colors.INNER_TEXT_COLOR.color)
        return AreaWithZindex(area, AreaType.SHAPE, rectangleAnchors(shape), centerAnchor(shape))
    }

    private fun drawMuleTask(canvas: CanvasPainter, shape: Rectangle2D.Float, name: String?, active: Boolean): AreaWithZindex {
        val area = canvas.drawRoundedRectWithIconAtCorner(shape, icons.mule, name, color(active, Colors.SERVICE_TASK_COLOR), Colors.ELEMENT_BORDER_COLOR.color, Colors.INNER_TEXT_COLOR.color)
        return AreaWithZindex(area, AreaType.SHAPE, rectangleAnchors(shape), centerAnchor(shape))
    }

    private fun drawDecisionTask(canvas: CanvasPainter, shape: Rectangle2D.Float, name: String?, active: Boolean): AreaWithZindex {
        val area = canvas.drawRoundedRectWithIconAtCorner(shape, icons.decision, name, color(active, Colors.SERVICE_TASK_COLOR), Colors.ELEMENT_BORDER_COLOR.color, Colors.INNER_TEXT_COLOR.color)
        return AreaWithZindex(area, AreaType.SHAPE, rectangleAnchors(shape), centerAnchor(shape))
    }

    private fun drawShellTask(canvas: CanvasPainter, shape: Rectangle2D.Float, name: String?, active: Boolean): AreaWithZindex {
        val area = canvas.drawRoundedRectWithIconAtCorner(shape, icons.shell, name, color(active, Colors.SERVICE_TASK_COLOR), Colors.ELEMENT_BORDER_COLOR.color, Colors.INNER_TEXT_COLOR.color)
        return AreaWithZindex(area, AreaType.SHAPE, rectangleAnchors(shape), centerAnchor(shape))
    }

    private fun drawSubProcess(canvas: CanvasPainter, shape: Rectangle2D.Float, name: String?, active: Boolean): AreaWithZindex {
        val area = canvas.drawRoundedRect(shape, name, color(active, Colors.PROCESS_COLOR), Colors.ELEMENT_BORDER_COLOR.color, Colors.SUBPROCESS_TEXT_COLOR.color)
        return AreaWithZindex(area, AreaType.SHAPE, rectangleAnchors(shape), centerAnchor(shape), index = SUBPROCESS_Z_INDEX)
    }

    private fun drawTransactionSubProcess(canvas: CanvasPainter, shape: Rectangle2D.Float, name: String?, active: Boolean): AreaWithZindex {
        val area = canvas.drawRoundedRect(shape, null, color(active, Colors.PROCESS_COLOR), Colors.ELEMENT_BORDER_COLOR.color, Colors.SUBPROCESS_TEXT_COLOR.color)
        canvas.drawRoundedRect(wrapInto(shape, transactionalBoundaryMargin), name, color(active, Colors.TRANSACTION_COLOR), Colors.TRANSACTION_ELEMENT_BORDER_COLOR.color, Colors.SUBPROCESS_TEXT_COLOR.color)
        return AreaWithZindex(area, AreaType.SHAPE, rectangleAnchors(shape), centerAnchor(shape), index = SUBPROCESS_Z_INDEX)
    }

    private fun wrapInto(target: Rectangle2D.Float, margin: Float): Rectangle2D.Float {
        return Rectangle2D.Float(target.x + margin, target.y + margin, target.width - 2.0f * margin, target.height - 2.0f * margin)
    }

    private fun drawAdHocSubProcess(canvas: CanvasPainter, shape: Rectangle2D.Float, name: String?, active: Boolean): AreaWithZindex {
        val area = canvas.drawRoundedRectWithIconAtBottom(shape, icons.tilde, name, color(active, Colors.TRANSACTION_COLOR), Colors.ELEMENT_BORDER_COLOR.color, Colors.SUBPROCESS_TEXT_COLOR.color)
        return AreaWithZindex(area, AreaType.SHAPE, rectangleAnchors(shape), centerAnchor(shape), index = SUBPROCESS_Z_INDEX)
    }

    private fun drawStartEvent(canvas: CanvasPainter, shape: Rectangle2D.Float, active: Boolean): AreaWithZindex {
        val area = canvas.drawWrappedIconWithLayer(shape, icons.startEvent, active, Colors.SELECTED_COLOR.color, { Ellipse2D.Float(it.x, it.y, it.width, it.height) }, color(active, Colors.START_EVENT))
        return AreaWithZindex(area, AreaType.SHAPE, ellipseOrDiamondAnchors(shape), centerAnchor(shape))
    }

    private fun drawStartConditionalEvent(canvas: CanvasPainter, shape: Rectangle2D.Float, active: Boolean): AreaWithZindex {
        val area = canvas.drawWrappedIconWithLayer(shape, icons.conditionalStartEvent, active, Colors.SELECTED_COLOR.color, { Ellipse2D.Float(it.x, it.y, it.width, it.height) }, color(active, Colors.START_EVENT))
        return AreaWithZindex(area, AreaType.SHAPE, ellipseOrDiamondAnchors(shape), centerAnchor(shape))
    }

    private fun drawStartMessageEvent(canvas: CanvasPainter, shape: Rectangle2D.Float, active: Boolean): AreaWithZindex {
        val area = canvas.drawWrappedIconWithLayer(shape, icons.messageStartEvent, active, Colors.SELECTED_COLOR.color, { Ellipse2D.Float(it.x, it.y, it.width, it.height) }, color(active, Colors.START_EVENT))
        return AreaWithZindex(area, AreaType.SHAPE, ellipseOrDiamondAnchors(shape), centerAnchor(shape))
    }

    private fun drawStartErrorEvent(canvas: CanvasPainter, shape: Rectangle2D.Float, active: Boolean): AreaWithZindex {
        val area = canvas.drawWrappedIconWithLayer(shape, icons.errorStartEvent, active, Colors.SELECTED_COLOR.color, { Ellipse2D.Float(it.x, it.y, it.width, it.height) }, color(active, Colors.START_EVENT))
        return AreaWithZindex(area, AreaType.SHAPE, ellipseOrDiamondAnchors(shape), centerAnchor(shape))
    }

    private fun drawStartEscalationEvent(canvas: CanvasPainter, shape: Rectangle2D.Float, active: Boolean): AreaWithZindex {
        val area = canvas.drawWrappedIconWithLayer(shape, icons.escalationStartEvent, active, Colors.SELECTED_COLOR.color, { Ellipse2D.Float(it.x, it.y, it.width, it.height) }, color(active, Colors.START_EVENT))
        return AreaWithZindex(area, AreaType.SHAPE, ellipseOrDiamondAnchors(shape), centerAnchor(shape))
    }

    private fun drawStartSignalEvent(canvas: CanvasPainter, shape: Rectangle2D.Float, active: Boolean): AreaWithZindex {
        val area = canvas.drawWrappedIconWithLayer(shape, icons.signalStartEvent, active, Colors.SELECTED_COLOR.color, { Ellipse2D.Float(it.x, it.y, it.width, it.height) }, color(active, Colors.START_EVENT))
        return AreaWithZindex(area, AreaType.SHAPE, ellipseOrDiamondAnchors(shape), centerAnchor(shape))
    }

    private fun drawStartTimerEvent(canvas: CanvasPainter, shape: Rectangle2D.Float, active: Boolean): AreaWithZindex {
        val area = canvas.drawWrappedIconWithLayer(shape, icons.timerStartEvent, active, Colors.SELECTED_COLOR.color, { Ellipse2D.Float(it.x, it.y, it.width, it.height) }, color(active, Colors.START_EVENT))
        return AreaWithZindex(area, AreaType.SHAPE, ellipseOrDiamondAnchors(shape), centerAnchor(shape))
    }

    private fun defaultElementRender(canvas: CanvasPainter, shape: Rectangle2D.Float, name: String?, active: Boolean): AreaWithZindex {
        val area = canvas.drawRoundedRect(shape, name, color(active, Colors.SERVICE_TASK_COLOR), Colors.ELEMENT_BORDER_COLOR.color, Colors.INNER_TEXT_COLOR.color)
        return AreaWithZindex(area, AreaType.SHAPE, rectangleAnchors(shape), centerAnchor(shape))
    }

    private fun drawTimerCatchEvent(canvas: CanvasPainter, shape: Rectangle2D.Float, active: Boolean): AreaWithZindex {
        val area = canvas.drawWrappedIcon(shape, icons.timerCatchEvent, active, Colors.SELECTED_COLOR.color)
        return AreaWithZindex(area, AreaType.SHAPE, ellipseOrDiamondAnchors(shape), centerAnchor(shape))
    }

    private fun drawMessageCatchEvent(canvas: CanvasPainter, shape: Rectangle2D.Float, active: Boolean): AreaWithZindex {
        val area = canvas.drawWrappedIcon(shape, icons.messageCatchEvent, active, Colors.SELECTED_COLOR.color)
        return AreaWithZindex(area, AreaType.SHAPE, ellipseOrDiamondAnchors(shape), centerAnchor(shape))
    }

    private fun drawSignalCatchEvent(canvas: CanvasPainter, shape: Rectangle2D.Float, active: Boolean): AreaWithZindex {
        val area = canvas.drawWrappedIcon(shape, icons.signalCatchEvent, active, Colors.SELECTED_COLOR.color)
        return AreaWithZindex(area, AreaType.SHAPE, ellipseOrDiamondAnchors(shape), centerAnchor(shape))
    }

    private fun drawConditionalCatchEvent(canvas: CanvasPainter, shape: Rectangle2D.Float, active: Boolean): AreaWithZindex {
        val area = canvas.drawWrappedIcon(shape, icons.conditionalCatchEvent, active, Colors.SELECTED_COLOR.color)
        return AreaWithZindex(area, AreaType.SHAPE, ellipseOrDiamondAnchors(shape), centerAnchor(shape))
    }

    private fun drawNoneThrowEvent(canvas: CanvasPainter, shape: Rectangle2D.Float, active: Boolean): AreaWithZindex {
        val area = canvas.drawWrappedIcon(shape, icons.noneThrowEvent, active, Colors.SELECTED_COLOR.color)
        return AreaWithZindex(area, AreaType.SHAPE, ellipseOrDiamondAnchors(shape), centerAnchor(shape))
    }

    private fun drawSignalThrowEvent(canvas: CanvasPainter, shape: Rectangle2D.Float, active: Boolean): AreaWithZindex {
        val area = canvas.drawWrappedIcon(shape, icons.signalThrowEvent, active, Colors.SELECTED_COLOR.color)
        return AreaWithZindex(area, AreaType.SHAPE, ellipseOrDiamondAnchors(shape), centerAnchor(shape))
    }

    private fun drawEscalationThrowEvent(canvas: CanvasPainter, shape: Rectangle2D.Float, active: Boolean): AreaWithZindex {
        val area = canvas.drawWrappedIcon(shape, icons.escalationThrowEvent, active, Colors.SELECTED_COLOR.color)
        return AreaWithZindex(area, AreaType.SHAPE, ellipseOrDiamondAnchors(shape), centerAnchor(shape))
    }

    private fun centerAnchor(shape: Rectangle2D.Float): MutableSet<Point2D.Float> {
        val cx = shape.bounds.x + shape.bounds.width / 2.0f
        val cy = shape.bounds.y + shape.bounds.height / 2.0f
        return mutableSetOf(
                Point2D.Float(cx, cy)
        )
    }

    private fun ellipseOrDiamondAnchors(shape: Rectangle2D.Float): MutableSet<Point2D.Float> {
        val halfWidth = shape.bounds.width / 2.0f
        val halfHeight = shape.bounds.height / 2.0f

        val cx = shape.bounds.x + shape.bounds.width / 2.0f
        val cy = shape.bounds.y + shape.bounds.height / 2.0f
        return mutableSetOf(
                Point2D.Float(cx, cy),

                Point2D.Float(cx - halfWidth, cy),
                Point2D.Float(cx + halfWidth, cy),
                Point2D.Float(cx, cy - halfHeight),
                Point2D.Float(cx, cy + halfHeight)
        )
    }

    private fun rectangleAnchors(shape: Rectangle2D.Float): MutableSet<Point2D.Float> {
        val halfWidth = shape.bounds.width / 2.0f
        val halfHeight = shape.bounds.height / 2.0f

        val cx = shape.bounds.x + shape.bounds.width / 2.0f
        val cy = shape.bounds.y + shape.bounds.height / 2.0f
        return mutableSetOf(
                Point2D.Float(cx, cy),

                Point2D.Float(cx - halfWidth, cy),
                Point2D.Float(cx + halfWidth, cy),
                Point2D.Float(cx, cy - halfHeight),
                Point2D.Float(cx, cy + halfHeight),

                Point2D.Float(cx - halfWidth / 2.0f, cy - halfHeight),
                Point2D.Float(cx + halfWidth / 2.0f, cy - halfHeight),
                Point2D.Float(cx - halfWidth / 2.0f, cy + halfHeight),
                Point2D.Float(cx + halfWidth / 2.0f, cy + halfHeight),

                Point2D.Float(cx - halfWidth, cy - halfHeight / 2.0f),
                Point2D.Float(cx - halfWidth, cy + halfHeight / 2.0f),
                Point2D.Float(cx + halfWidth, cy - halfHeight / 2.0f),
                Point2D.Float(cx + halfWidth, cy + halfHeight / 2.0f)
        )
    }

    private fun color(active: Boolean, color: Colors): Color {
        return if (active) Colors.SELECTED_COLOR.color else color.color
    }

    private fun isActive(elemId: DiagramElementId, meta: RenderMetadata): Boolean {
        return elemId.let { meta.selectedIds.contains(it) }
    }

    private fun drawActionsElement(canvas: CanvasPainter, edge: EdgeWithIdentifiableWaypoints, ctx: ElementInteractionContext, actions: Map<Actions, (dest: ProcessModelUpdateEvents) -> Unit>): Map<DiagramElementId, AreaWithZindex> {
        val minX = edge.waypoint.minBy { it.x }?.x ?: 0.0f
        val minY = edge.waypoint.minBy { it.y }?.y ?: 0.0f
        val maxX = edge.waypoint.maxBy { it.x }?.x ?: 0.0f
        val maxY = edge.waypoint.maxBy { it.y }?.y ?: 0.0f
        return drawActionsElement(
                canvas,
                edge.id,
                Point2D.Float(minX - activityToolBoxGap, minY - activityToolBoxGap),
                maxX - minX + activityToolBoxGap * 2.0f,
                maxY - minY + activityToolBoxGap * 2.0f,
                ctx,
                actions
        )
    }

    private fun drawActionsElement(canvas: CanvasPainter, waypoint: IdentifiableWaypoint, ctx: ElementInteractionContext, actions: Map<Actions, (dest: ProcessModelUpdateEvents) -> Unit>): Map<DiagramElementId, AreaWithZindex> {
        return drawActionsElement(
                canvas,
                waypoint.id,
                Point2D.Float(waypoint.x - activityToolBoxGap, waypoint.y - activityToolBoxGap),
                activityToolBoxGap * 2.0f,
                activityToolBoxGap * 2.0f,
                ctx,
                actions
        )
    }

    private fun drawActionsElement(canvas: CanvasPainter, ownerId: DiagramElementId, shape: Rectangle2D.Float, ctx: ElementInteractionContext, actions: Map<Actions, (dest: ProcessModelUpdateEvents) -> Unit>): Map<DiagramElementId, AreaWithZindex> {
        return drawActionsElement(
                canvas,
                ownerId,
                Point2D.Float(shape.x - activityToolBoxGap, shape.y - activityToolBoxGap),
                shape.width + activityToolBoxGap * 2.0f,
                shape.height + activityToolBoxGap * 2.0f,
                ctx,
                actions
        )
    }

    private fun drawActionsElement(
            canvas: CanvasPainter,
            ownerId: DiagramElementId,
            location: Point2D.Float,
            width: Float,
            height: Float,
            ctx: ElementInteractionContext,
            actions: Map<Actions, (dest: ProcessModelUpdateEvents) -> Unit>
    ): Map<DiagramElementId, AreaWithZindex> {
        val result = HashMap<DiagramElementId, AreaWithZindex>()
        canvas.drawRectNoFill(location, width, height, ACTION_AREA_STROKE, Colors.ACTIONS_BORDER_COLOR.color)
        var yLocation = location.y
        actions.forEach {
            when (it.key) {
                Actions.DELETE -> {
                    val delId = DiagramElementId("DEL:$ownerId")
                    val deleteIconArea = canvas.drawIcon(BoundsElement(location.x + width + actionsMargin, yLocation, actionsIcoSize, actionsIcoSize), icons.recycleBin)
                    ctx.clickCallbacks[delId] = it.value
                    result[delId] = AreaWithZindex(deleteIconArea, AreaType.POINT, mutableSetOf(), mutableSetOf(), ANCHOR_Z_INDEX, ownerId)
                    yLocation += actionsIcoSize + actionsMargin
                }
                Actions.NEW_LINK -> {
                    val newLinkId = DiagramElementId("NEWLINK:$ownerId")
                    val newSequence = canvas.drawIcon(BoundsElement(location.x + width + actionsMargin, yLocation, actionsIcoSize, actionsIcoSize), icons.sequence)
                    ctx.clickCallbacks[newLinkId] = it.value
                    result[newLinkId] = AreaWithZindex(newSequence, AreaType.POINT, mutableSetOf(), mutableSetOf(), ANCHOR_Z_INDEX, ownerId)
                    yLocation += actionsIcoSize + actionsMargin
                }
            }
        }

        return result
    }

    private fun drawResizeIconTop(canvas: CanvasPainter, rect: Rectangle2D.Float, origRect: Rectangle2D.Float, resizeElem: ShapeBoundsAnchorElement, renderMeta: RenderMetadata): Map<DiagramElementId, AreaWithZindex> {
        val iconLeft = canvas.camera.fromCameraView(Point2D.Float(0.0f, 0.0f))
        val iconRight = canvas.camera.fromCameraView(Point2D.Float(icons.dragToResizeTop.iconWidth.toFloat(), icons.dragToResizeTop.iconHeight.toFloat()))
        val width = iconRight.x - iconLeft.x
        val height = iconRight.y - iconLeft.y

        val active = isActive(resizeElem.id, renderMeta)
        val area = canvas.drawIcon(Point2D.Float(rect.x - width - actionsMargin, rect.y - height - actionsMargin), icons.dragToResizeTop, if (active) Colors.SELECTED_COLOR.color else null)

        renderMeta.interactionContext.dragEndCallbacks[resizeElem.id] = OnDragEnd@{ dx: Float, dy: Float, _: BpmnElementId? ->
            val events = mutableListOf<Event>()
            events += ShapeRectUpdatedEvent(resizeElem.id, dx, dy, rect.width, rect.height)
            val cx = origRect.x + origRect.width
            val cy = origRect.y + origRect.height
            events += renderMeta
                    .cascadedTransalationOf
                    .filter { target -> target.cascadeSource == resizeElem.bpmnElement }
                    .filter { target -> !renderMeta.interactionContext.draggedIds.contains(target.waypointId) }
                    .map { cascadeTo ->
                        val newX = cx + (cascadeTo.location.x - cx) / origRect.width * rect.width
                        val newY = cy + (cascadeTo.location.y - cy) / origRect.height * rect.height
                        DraggedToEvent(cascadeTo.waypointId, newX - cascadeTo.location.x, newY - cascadeTo.location.y, cascadeTo.parentEdgeId, cascadeTo.internalId)
                    }
            return@OnDragEnd events
        }

        return mapOf(resizeElem.id to AreaWithZindex(area, AreaType.POINT, parentToSelect = resizeElem.parentId))
    }

    private fun drawResizeIconBottom(canvas: CanvasPainter, rect: Rectangle2D.Float, resizeElem: ShapeBoundsAnchorElement, renderMeta: RenderMetadata): Map<DiagramElementId, AreaWithZindex> {
        val active = isActive(resizeElem.id, renderMeta)
        val area = canvas.drawIcon(Point2D.Float(rect.x + rect.width + actionsMargin, rect.y + rect.height + actionsMargin), icons.dragToResizeBottom, if (active) Colors.SELECTED_COLOR.color else null)
        return mapOf(resizeElem.id to AreaWithZindex(area, AreaType.POINT, parentToSelect = resizeElem.parentId))
    }

    private data class RenderMetadata(
            val interactionContext: ElementInteractionContext,
            val selectedIds: Set<DiagramElementId>,
            val elementByDiagramId: Map<DiagramElementId, BpmnElementId>,
            val elementById: Map<BpmnElementId, WithBpmnId>,
            val elemPropertiesByElementId: Map<BpmnElementId, Map<PropertyType, Property>>,
            val cascadedTransalationOf: Set<CascadeTranslationToWaypoint>
    )

    private data class CascadeTranslationToWaypoint(val cascadeSource: BpmnElementId, val waypointId: DiagramElementId, val location: Point2D.Float, val parentEdgeId: DiagramElementId, val internalId: Int)

    private enum class Actions {
        DELETE,
        NEW_LINK
    }
}