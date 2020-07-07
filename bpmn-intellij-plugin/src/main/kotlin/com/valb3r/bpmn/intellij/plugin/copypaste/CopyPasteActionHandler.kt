package com.valb3r.bpmn.intellij.plugin.copypaste

import com.fasterxml.jackson.annotation.JsonAutoDetect
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.BpmnElementId
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.WithBpmnId
import com.valb3r.bpmn.intellij.plugin.bpmn.api.diagram.DiagramElementId
import com.valb3r.bpmn.intellij.plugin.bpmn.api.diagram.elements.BoundsElement
import com.valb3r.bpmn.intellij.plugin.bpmn.api.diagram.elements.EdgeElement
import com.valb3r.bpmn.intellij.plugin.bpmn.api.diagram.elements.ShapeElement
import com.valb3r.bpmn.intellij.plugin.bpmn.api.events.EdgeWithIdentifiableWaypoints
import com.valb3r.bpmn.intellij.plugin.bpmn.api.info.Property
import com.valb3r.bpmn.intellij.plugin.bpmn.api.info.PropertyType
import com.valb3r.bpmn.intellij.plugin.events.BpmnEdgeObjectAddedEvent
import com.valb3r.bpmn.intellij.plugin.events.BpmnShapeObjectAddedEvent
import com.valb3r.bpmn.intellij.plugin.events.ProcessModelUpdateEvents
import com.valb3r.bpmn.intellij.plugin.render.EdgeElementState
import com.valb3r.bpmn.intellij.plugin.render.elements.BaseDiagramRenderElement
import com.valb3r.bpmn.intellij.plugin.render.elements.RenderState
import com.valb3r.bpmn.intellij.plugin.render.elements.edges.BaseEdgeRenderElement
import com.valb3r.bpmn.intellij.plugin.render.elements.shapes.ShapeRenderElement
import java.awt.Toolkit
import java.awt.datatransfer.Clipboard
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.StringSelection
import java.awt.geom.Point2D
import java.util.*
import java.util.concurrent.atomic.AtomicReference
import kotlin.math.min

private val copyPasteActionHandler = AtomicReference<CopyPasteActionHandler>()

fun copyPasteActionHandler(): CopyPasteActionHandler {
    return copyPasteActionHandler.updateAndGet {
        if (null == it) {
            return@updateAndGet CopyPasteActionHandler()
        }

        return@updateAndGet it
    }
}

private val DATA_FLAVOR = DataFlavor(String::class.java, "Flowable BPMN IntelliJ editor plugin clipboard data")

data class ClipboardAddEvents(val shapes: MutableList<BpmnShapeObjectAddedEvent>, val edges: MutableList<BpmnEdgeObjectAddedEvent>)

class CopyPasteActionHandler {

    private val ROOT_NAME = "__:-:-:ROOT"

    private val mapper = constructMapper()

    fun copy(idsToCopy: MutableList<DiagramElementId>, ctx: RenderState, elementsById: MutableMap<BpmnElementId, BaseDiagramRenderElement>) {
        val orderedIds = ensureRootElementsComeFirst(idsToCopy, ctx, elementsById)
        val toCopy = ClipboardAddEvents(mutableListOf(), mutableListOf())
        val idReplacements = mutableMapOf<BpmnElementId, BpmnElementId>()
        for (diagramId in orderedIds) {
            elementToAddEvents(ctx, diagramId, elementsById, toCopy, false, idReplacements)
        }

        val clipboard: Clipboard = clipboard()
        clipboard.setContents(FlowableClipboardFlavor(mapper.writeValueAsString(toCopy)), null)
    }

    fun cut(idsToCut: MutableList<DiagramElementId>, ctx: RenderState, updateEvents: ProcessModelUpdateEvents, elementsById: MutableMap<BpmnElementId, BaseDiagramRenderElement>) {
    }

    fun hasDataToPaste(): Boolean {
        return clipboard().isDataFlavorAvailable(DATA_FLAVOR)
    }

    fun paste(sceneLocation: Point2D.Float, parent: BpmnElementId): ClipboardAddEvents? {
        val clipboard: Clipboard = clipboard()
        return try {
            val data = clipboard.getData(DATA_FLAVOR) as String
            val events = mapper.readValue(data, ClipboardAddEvents::class.java)
            val updatedIds = mutableMapOf(BpmnElementId(ROOT_NAME) to parent)

            val minX = events.shapes.map { it.shape.rectBounds().x}.min() ?: events.edges.map { min(it.edge.waypoint[0].x, it.edge.waypoint[it.edge.waypoint.size - 1].x) }.min() ?: 0.0f
            val minY = events.shapes.map { it.shape.rectBounds().y}.min() ?: events.edges.map { min(it.edge.waypoint[0].y, it.edge.waypoint[it.edge.waypoint.size - 1].y) }.min() ?: 0.0f
            val delta = Point2D.Float(sceneLocation.x - minX, sceneLocation.y - minY)

            events.copy(
                    shapes = updateShapes(delta, events.shapes, updatedIds),
                    edges = updateEdges(delta, events.edges, updatedIds))
        } catch (ex: Exception) {
            null
        }
    }

    private fun copied(objId: BpmnElementId, updatedIds: MutableMap<BpmnElementId, BpmnElementId>): BpmnElementId {
        return updatedIds.computeIfAbsent(objId) {BpmnElementId("sid-" + UUID.randomUUID().toString())}
    }

    private fun copied(obj: WithBpmnId, updatedIds: MutableMap<BpmnElementId, BpmnElementId>): WithBpmnId {
        val id = updatedIds.computeIfAbsent(obj.id) {BpmnElementId("sid-" + UUID.randomUUID().toString())}
        return obj.updateBpmnElemId(id)
    }

    private fun copied(shape: ShapeElement, delta: Point2D.Float, updatedIds: MutableMap<BpmnElementId, BpmnElementId>): ShapeElement {
        val diagramId = DiagramElementId("sid-" + UUID.randomUUID().toString())
        val bpmnElementId = updatedIds[shape.bpmnElement]!!
        return shape.copy(
                id = diagramId,
                bpmnElement = bpmnElementId,
                bounds = BoundsElement(
                        shape.rectBounds().x + delta.x,
                        shape.rectBounds().y + delta.y,
                        shape.rectBounds().width,
                        shape.rectBounds().height
                )
        )
    }

    private fun copied(edge: EdgeWithIdentifiableWaypoints, delta: Point2D.Float, updatedIds: MutableMap<BpmnElementId, BpmnElementId>): EdgeWithIdentifiableWaypoints {
        val diagramId = DiagramElementId("sid-" + UUID.randomUUID().toString())
        val bpmnElementId = updatedIds[edge.bpmnElement]

        return EdgeElementState(EdgeElement(diagramId, bpmnElementId, edge.waypoint.map { it.asWaypointElement().copyAndTranslate(delta.x, delta.y) }) )
    }

    private fun copied(props: Map<PropertyType, Property>, updatedIds: MutableMap<BpmnElementId, BpmnElementId>): Map<PropertyType, Property> {
        val result = mutableMapOf<PropertyType, Property>()
        props.forEach {
            when {
                it.value.value == null -> result[it.key] = it.value
                PropertyType.ID == it.key -> result[it.key] = Property(copied(BpmnElementId(it.value.value as String), updatedIds).id)
                PropertyType.ID == it.key.updatedBy -> result[it.key] = Property(copied(BpmnElementId(it.value.value as String), updatedIds).id)
                else -> result[it.key] = it.value
            }
        }
        return result
    }

    private fun updateShapes(delta: Point2D.Float, shapes: MutableList<BpmnShapeObjectAddedEvent>, updatedIds: MutableMap<BpmnElementId, BpmnElementId>): MutableList<BpmnShapeObjectAddedEvent> {
        val result = shapes.map {
            it.copy(
                    bpmnObject = it.bpmnObject.copy(
                            parent = copied(it.bpmnObject.parent, updatedIds),
                            element = copied(it.bpmnObject.element, updatedIds)
                    )
            )
        }

        return result.map { it.copy(props = copied(it.props, updatedIds), shape = copied(it.shape, delta, updatedIds)) }.toMutableList()
    }

    private fun updateEdges(delta: Point2D.Float, edges: MutableList<BpmnEdgeObjectAddedEvent>, updatedIds: MutableMap<BpmnElementId, BpmnElementId>): MutableList<BpmnEdgeObjectAddedEvent> {
        val result = edges.map {
            it.copy(
                    bpmnObject = it.bpmnObject.copy(
                            parent = copied(it.bpmnObject.parent, updatedIds),
                            element = copied(it.bpmnObject.element, updatedIds)
                    )
            )
        }

        return result.map { it.copy(props = copied(it.props, updatedIds), edge = copied(it.edge, delta, updatedIds)) }.toMutableList()
    }

    private fun clipboard(): Clipboard {
        return Toolkit.getDefaultToolkit().systemClipboard
    }

    private fun ensureRootElementsComeFirst(idsToCopy: MutableList<DiagramElementId>, ctx: RenderState, elementsById: MutableMap<BpmnElementId, BaseDiagramRenderElement>): MutableList<DiagramElementId> {
        return idsToCopy
                .sortedByDescending { ctx.currentState.elementByDiagramId[it]?.let {id -> elementsById[id] }?.zIndex() ?: 0 }
                .toMutableList()
    }

    private fun elementToAddEvents(
            ctx: RenderState,
            diagramId: DiagramElementId,
            elementsById: MutableMap<BpmnElementId, BaseDiagramRenderElement>,
            events: ClipboardAddEvents,
            preserveRoot: Boolean,
            idReplacements: MutableMap<BpmnElementId, BpmnElementId>
    ) {
        val bpmnId = ctx.currentState.elementByDiagramId[diagramId] ?: return
        val withParentId = ctx.currentState.elementByBpmnId[bpmnId] ?: return
        val props = ctx.currentState.elemPropertiesByStaticElementId[bpmnId] ?: return
        val renderElem = elementsById[bpmnId] ?: return
        fun detachParentIfNeeded() = if (preserveRoot) {
            withParentId.copy(parent = idReplacements[withParentId.parentIdForCopying] ?: withParentId.parentIdForCopying)
        } else {
            idReplacements[withParentId.parentIdForCopying] = BpmnElementId(ROOT_NAME)
            withParentId.copy(parent = BpmnElementId(ROOT_NAME))
        }

        when (renderElem) {
            is ShapeRenderElement -> {
                events.shapes += BpmnShapeObjectAddedEvent(
                        detachParentIfNeeded(),
                        renderElem.shapeElem,
                        props
                )
                renderElem.children.forEach {elementToAddEvents(ctx, it.elementId, elementsById, events, true, idReplacements)}
            }
            is BaseEdgeRenderElement -> {
                events.edges += BpmnEdgeObjectAddedEvent(
                        detachParentIfNeeded(),
                        renderElem.edgeElem,
                        props
                )
            }
        }
    }

    private fun constructMapper(): ObjectMapper {
        val builtMapper: ObjectMapper = ObjectMapper().registerModule(KotlinModule())
        builtMapper.setVisibility(builtMapper.serializationConfig.defaultVisibilityChecker
                .withFieldVisibility(JsonAutoDetect.Visibility.ANY)
                .withGetterVisibility(JsonAutoDetect.Visibility.NONE)
                .withSetterVisibility(JsonAutoDetect.Visibility.NONE)
                .withCreatorVisibility(JsonAutoDetect.Visibility.NONE))
        return builtMapper
    }

    private class FlowableClipboardFlavor(data: String): StringSelection(data) {

        override fun isDataFlavorSupported(flavor: DataFlavor?): Boolean {
            return DATA_FLAVOR.equals(flavor)
        }
    }
}