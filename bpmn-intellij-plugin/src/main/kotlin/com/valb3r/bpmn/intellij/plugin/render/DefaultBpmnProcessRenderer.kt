package com.valb3r.bpmn.intellij.plugin.render

import com.valb3r.bpmn.intellij.plugin.Colors
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.BpmnElementId
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.WithBpmnId
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
import com.valb3r.bpmn.intellij.plugin.bpmn.api.diagram.elements.BoundsElement
import com.valb3r.bpmn.intellij.plugin.bpmn.api.diagram.elements.ShapeElement
import com.valb3r.bpmn.intellij.plugin.debugger.currentDebugger
import com.valb3r.bpmn.intellij.plugin.events.BpmnElementRemovedEvent
import com.valb3r.bpmn.intellij.plugin.events.DiagramElementRemovedEvent
import com.valb3r.bpmn.intellij.plugin.events.ProcessModelUpdateEvents
import com.valb3r.bpmn.intellij.plugin.render.elements.BaseRenderElement
import com.valb3r.bpmn.intellij.plugin.render.elements.RenderState
import com.valb3r.bpmn.intellij.plugin.render.elements.edges.EdgeRenderElement
import com.valb3r.bpmn.intellij.plugin.render.elements.planes.PlaneRenderElement
import com.valb3r.bpmn.intellij.plugin.render.elements.shapes.*
import java.awt.BasicStroke
import java.awt.geom.Point2D

interface BpmnProcessRenderer {
    fun render(ctx: RenderContext): Map<DiagramElementId, AreaWithZindex>
}

class DefaultBpmnProcessRenderer(val icons: IconProvider) : BpmnProcessRenderer {
    private val undoRedoStartMargin = 20.0f
    private val anchorRadius = 5f
    private val actionsIcoSize = 15f

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
        // Not all elements have BpmnElementId, but they have DiagramElementId
        val elementsByDiagramId = mutableMapOf<DiagramElementId, BaseRenderElement>()
        linkDiagramElementId(root, elementsByDiagramId)

        root.applyContextChanges(elementsByDiagramId)
        val rendered = root.render()

        // Overlay system elements on top of rendered BPMN diagram
        ctx.interactionContext.anchorsHit?.apply { drawAnchorsHit(ctx.canvas, this) }
        drawUndoRedo(state, rendered)
        drawSelectionRect(ctx)
        drawMultiremovalRect(state, rendered)
        drawDebugElements(state, rendered)

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

    private fun drawSelectionRect(state: RenderContext) {
        state.interactionContext.dragSelectionRect?.let {
            val rect = it.toRect()
            state.canvas.drawRectNoCameraTransform(Point2D.Float(rect.x, rect.y), rect.width, rect.height, ACTION_AREA_STROKE, Colors.ACTIONS_BORDER_COLOR.color)
        }
    }

    private fun drawDebugElements(state: RenderState, renderedArea: Map<DiagramElementId, AreaWithZindex>) {
        currentDebugger()?.executionSequence(state.currentState.processId.id)?.history?.let { history ->
            val elemToDiagramId = mutableMapOf<BpmnElementId, MutableSet<DiagramElementId>>()
            state.currentState.elementByDiagramId.forEach { elemToDiagramId.computeIfAbsent(it.value) { mutableSetOf() }.add(it.key) }
            val elemOccurs = mutableMapOf<BpmnElementId, MutableList<Int>>()
            history.forEachIndexed { index, elem -> elemOccurs.computeIfAbsent(elem) { mutableListOf() }.add(index) }

            elemOccurs.forEach { (elem, indexes) ->
                val targetElem = elemToDiagramId[elem]?.firstOrNull()
                renderedArea[targetElem]?.apply {
                    val bounds = this.area.bounds2D
                    state.ctx.canvas.drawTextNoCameraTransform(Point2D.Float(bounds.x.toFloat(), bounds.y.toFloat()), indexes.toString(), Colors.INNER_TEXT_COLOR.color)
                }
            }

        }
    }

    private fun drawMultiremovalRect(state: RenderState, renderedArea: MutableMap<DiagramElementId, AreaWithZindex>) {
        if (null != state.ctx.interactionContext.dragSelectionRect || state.ctx.selectedIds.size <= 1) {
            return
        }

        val parentChild = state.ctx.selectedIds.flatMap { setOf(it, renderedArea[it]?.parentToSelect) }.filterNotNull()

        if (state.ctx.selectedIds.size == 2 && state.ctx.selectedIds.containsAll(parentChild)) {
            return
        }

        val areas = state.ctx.selectedIds.map { renderedArea[it] }.filterNotNull()

        val minX = areas.map { it.area.bounds2D.minX }.min()?.toFloat()
        val minY = areas.map { it.area.bounds2D.minY }.min()?.toFloat()
        val maxX = areas.map { it.area.bounds2D.maxX }.max()?.toFloat()
        val maxY = areas.map { it.area.bounds2D.maxY }.max()?.toFloat()

        if (null != minX && null != minY && null != maxX && null != maxY) {
            val ownerId = state.ctx.selectedIds.joinToString { it.id }
            state.ctx.canvas.drawRectNoCameraTransform(Point2D.Float(minX, minY), maxX - minX, maxY - minY, ACTION_AREA_STROKE, Colors.ACTIONS_BORDER_COLOR.color)
            val delId = DiagramElementId("DEL:$ownerId")
            val deleteIconArea = state.ctx.canvas.drawIconNoCameraTransform(BoundsElement(maxX, minY, actionsIcoSize, actionsIcoSize), icons.recycleBin)
            state.ctx.interactionContext.clickCallbacks[delId] = { dest ->
                val targetIds = state.ctx.selectedIds.filter { renderedArea[it]?.areaType == AreaType.SHAPE || renderedArea[it]?.areaType == AreaType.EDGE }
                dest.addElementRemovedEvent(
                        targetIds.map { DiagramElementRemovedEvent(it) },
                        targetIds.mapNotNull { state.currentState.elementByDiagramId[it] }.map { BpmnElementRemovedEvent(it) }
                )
            }
            renderedArea[delId] = AreaWithZindex(deleteIconArea, AreaType.POINT, mutableSetOf(), mutableSetOf(), ANCHOR_Z_INDEX, null)
        }
    }

    private fun drawUndoRedo(state: RenderState, renderedArea: MutableMap<DiagramElementId, AreaWithZindex>) {
        val start = Point2D.Float(undoRedoStartMargin, undoRedoStartMargin)
        var locationX = start.x
        val locationY = start.y

        if (state.currentState.undoRedo.contains(ProcessModelUpdateEvents.UndoRedo.UNDO)) {
            val color = if (isActive(undoId, state)) Colors.SELECTED_COLOR else null
            val areaUndo = color?.let { state.ctx.canvas.drawIconAtScreen(Point2D.Float(locationX, locationY), icons.undo, it.color) }
                    ?: state.ctx.canvas.drawIconAtScreen(Point2D.Float(locationX, locationY), icons.undo)
            renderedArea[undoId] = AreaWithZindex(areaUndo, AreaType.SHAPE)
            locationX += icons.undo.iconWidth + undoRedoStartMargin
            state.ctx.interactionContext.clickCallbacks[undoId] = { dest -> dest.undo() }
        }

        if (state.currentState.undoRedo.contains(ProcessModelUpdateEvents.UndoRedo.REDO)) {
            val color = if (isActive(redoId, state)) Colors.SELECTED_COLOR else null
            val areaRedo = color?.let { state.ctx.canvas.drawIconAtScreen(Point2D.Float(locationX, locationY), icons.redo, it.color) }
                    ?: state.ctx.canvas.drawIconAtScreen(Point2D.Float(locationX, locationY), icons.redo)
            renderedArea[redoId] = AreaWithZindex(areaRedo, AreaType.SHAPE)
            locationX += icons.redo.iconWidth + undoRedoStartMargin
            state.ctx.interactionContext.clickCallbacks[redoId] = { dest -> dest.redo() }
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

    private fun isActive(elemId: DiagramElementId, state: RenderState): Boolean {
        return elemId.let { state.ctx.selectedIds.contains(it) }
    }
}