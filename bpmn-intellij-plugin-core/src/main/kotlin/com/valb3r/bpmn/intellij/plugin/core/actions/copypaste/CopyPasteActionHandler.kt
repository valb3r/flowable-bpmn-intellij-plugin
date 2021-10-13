package com.valb3r.bpmn.intellij.plugin.core.actions.copypaste

import com.fasterxml.jackson.annotation.JsonAutoDetect
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.google.common.annotations.VisibleForTesting
import com.intellij.openapi.project.Project
import com.valb3r.bpmn.intellij.plugin.bpmn.api.PropertyTable
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.BpmnElementId
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.WithBpmnId
import com.valb3r.bpmn.intellij.plugin.bpmn.api.diagram.DiagramElementId
import com.valb3r.bpmn.intellij.plugin.bpmn.api.diagram.elements.BoundsElement
import com.valb3r.bpmn.intellij.plugin.bpmn.api.diagram.elements.EdgeElement
import com.valb3r.bpmn.intellij.plugin.bpmn.api.diagram.elements.ShapeElement
import com.valb3r.bpmn.intellij.plugin.bpmn.api.events.EdgeWithIdentifiableWaypoints
import com.valb3r.bpmn.intellij.plugin.bpmn.api.info.Property
import com.valb3r.bpmn.intellij.plugin.bpmn.api.info.PropertyType
import com.valb3r.bpmn.intellij.plugin.core.events.*
import com.valb3r.bpmn.intellij.plugin.core.render.EdgeElementState
import com.valb3r.bpmn.intellij.plugin.core.render.elements.BaseDiagramRenderElement
import com.valb3r.bpmn.intellij.plugin.core.render.elements.RenderState
import com.valb3r.bpmn.intellij.plugin.core.render.elements.anchors.PhysicalWaypoint
import com.valb3r.bpmn.intellij.plugin.core.render.elements.edges.BaseEdgeRenderElement
import com.valb3r.bpmn.intellij.plugin.core.render.elements.shapes.ShapeRenderElement
import java.awt.Toolkit
import java.awt.datatransfer.*
import java.awt.geom.Point2D
import java.io.IOException
import java.util.*
import kotlin.math.min

private val copyPasteActionHandler = Collections.synchronizedMap(WeakHashMap<Project,  CopyPasteActionHandler>())

fun copyPasteActionHandler(project: Project): CopyPasteActionHandler {
    return copyPasteActionHandler.computeIfAbsent(project) {
        CopyPasteActionHandler(DefaultSystemClipboard(Toolkit.getDefaultToolkit().systemClipboard))
    }
}

@VisibleForTesting
fun setCopyPasteActionHandler(project: Project, handler: CopyPasteActionHandler) {
    copyPasteActionHandler[project] = handler
}

val DATA_FLAVOR = DataFlavor("text/flowable-alike-bpmn-plugin-intellij", "Flowable (plugin family) BPMN IntelliJ editor plugin clipboard data")

data class ClipboardContext(
        val shapes: MutableList<BpmnShapeObjectAddedEvent>,
        val edges: MutableList<BpmnEdgeObjectAddedEvent>,
        val selectElements: List<DiagramElementId>
)

interface SystemClipboard {

    fun setContents(contents: Transferable, owner: ClipboardOwner?)
    fun isDataFlavorAvailable(flavor: DataFlavor?): Boolean
    fun getData(flavor: DataFlavor): Any
}

class DefaultSystemClipboard(private val clipboard: Clipboard): SystemClipboard {

    override fun setContents(contents: Transferable, owner: ClipboardOwner?) {
        clipboard.setContents(contents, owner)
    }

    override fun isDataFlavorAvailable(flavor: DataFlavor?): Boolean {
        return clipboard.isDataFlavorAvailable(flavor)
    }

    override fun getData(flavor: DataFlavor): Any {
        return clipboard.getData(flavor)
    }
}

class CopyPasteActionHandler(private val clipboard: SystemClipboard) {

    private val ROOT_NAME = "__:-:-:ROOT"

    private val mapper = constructMapper()

    fun copy(ctx: RenderState, elementsById: Map<BpmnElementId, BaseDiagramRenderElement>) {
        val toCopy = extractDataToCopy(ctx, elementsById)
        clipboard.setContents(ClipboardFlavor(mapper.writeValueAsString(toCopy)), null)
    }

    fun cut(ctx: RenderState, updateEvents: ProcessModelUpdateEvents, elementsById: Map<BpmnElementId, BaseDiagramRenderElement>) {
        val toCopy = extractDataToCopy(ctx, elementsById)

        val alreadyRemovedBpmn = mutableSetOf<BpmnElementId>()
        val elemsToDelete = elementIdsToCopyOrCut(ctx)
                .mapNotNull { ctx.currentState.elementByDiagramId[it] }
                .filter { if (alreadyRemovedBpmn.contains(it)) false else { alreadyRemovedBpmn += it; true } }
                .mapNotNull { elementsById[it] }

        val bpmnToRemove = mutableListOf<BpmnElementRemovedEvent>()
        val diagramToRemove = mutableListOf<DiagramElementRemovedEvent>()

        elemsToDelete.forEach { bpmnToRemove += it.getEventsToDeleteElement() }
        elemsToDelete.forEach { diagramToRemove += it.getEventsToDeleteDiagram() }

        updateEvents.addElementRemovedEvent(diagramToRemove, bpmnToRemove)
        clipboard.setContents(ClipboardFlavor(mapper.writeValueAsString(toCopy)), null)
    }

    fun hasDataToPaste(): Boolean {
        return clipboard.isDataFlavorAvailable(DATA_FLAVOR)
    }

    fun paste(sceneLocation: Point2D.Float, parent: BpmnElementId): ClipboardContext? {
        return try {
            val data = clipboard.getData(DATA_FLAVOR) as String
            val context = mapper.readValue(data, ClipboardContext::class.java)
            val updatedIds = mutableMapOf(BpmnElementId(ROOT_NAME) to parent)
            val updatedDiagramIds = mutableMapOf<DiagramElementId, DiagramElementId>()

            val minX = context.shapes.map { it.shape.rectBounds().x }.min()
                ?: context.edges.map { min(it.edge.waypoint[0].x, it.edge.waypoint[it.edge.waypoint.size - 1].x) }
                    .min() ?: 0.0f
            val minY = context.shapes.map { it.shape.rectBounds().y }.min()
                ?: context.edges.map { min(it.edge.waypoint[0].y, it.edge.waypoint[it.edge.waypoint.size - 1].y) }
                    .min()
                ?: 0.0f
            val delta = Point2D.Float(sceneLocation.x - minX, sceneLocation.y - minY)

            val updatedShapes = updateShapes(delta, context.shapes, updatedIds, updatedDiagramIds)
            val updatedEdges = updateEdges(delta, context.edges, updatedIds, updatedDiagramIds)
            val updatedSelectedElems = computeElementsToSelect(context, updatedEdges, updatedDiagramIds)

            context.copy(shapes = updatedShapes, edges = updatedEdges, selectElements = updatedSelectedElems)
        } catch (ex: Exception) {
            null
        }
    }

    private fun computeElementsToSelect(
            context: ClipboardContext,
            updatedEdges: MutableList<BpmnEdgeObjectAddedEvent>,
            updatedDiagramIds: MutableMap<DiagramElementId, DiagramElementId>): List<DiagramElementId> {
        val newEdges: Map<DiagramElementId, BpmnEdgeObjectAddedEvent> = updatedEdges.map { Pair(it.edge.id, it) }.toMap()
        val result = mutableListOf<DiagramElementId?>()

        // Select all items of edges (paste'd)
        for (elem in context.selectElements) {
            val edge = newEdges[updatedDiagramIds[elem]]
            if (null == edge) {
                result += updatedDiagramIds[elem]
                continue
            }

            result += edge.edge.id
            result += edge.edge.waypoint.map { it.id }
        }

        return result.filterNotNull()
    }

    private fun extractDataToCopy(ctx: RenderState, elementsById: Map<BpmnElementId, BaseDiagramRenderElement>): ClipboardContext {
        val idsToCopy = elementIdsToCopyOrCut(ctx)
        val orderedIds = ensureRootElementsComeFirst(idsToCopy.toMutableList(), ctx, elementsById)
        val toCopy = ClipboardContext(mutableListOf(), mutableListOf(), idsToCopy)
        val idReplacements = mutableMapOf<BpmnElementId, BpmnElementId>()
        val processedElementIds = mutableSetOf<BpmnElementId>()
        for (diagramId in orderedIds) {
            elementToAddEvents(ctx, diagramId, elementsById, toCopy, false, idReplacements, processedElementIds)
        }
        return toCopy
    }

    private fun copiedExistsOrEmpty(objId: BpmnElementId, updatedIds: MutableMap<BpmnElementId, BpmnElementId>): String {
        return updatedIds.get(objId)?.id ?: ""
    }

    private fun copied(objId: BpmnElementId, updatedIds: MutableMap<BpmnElementId, BpmnElementId>): BpmnElementId {
        return updatedIds.computeIfAbsent(objId) {BpmnElementId("sid-" + UUID.randomUUID().toString())}
    }

    private fun copied(obj: WithBpmnId, updatedIds: MutableMap<BpmnElementId, BpmnElementId>): WithBpmnId {
        val id = updatedIds.computeIfAbsent(obj.id) {BpmnElementId("sid-" + UUID.randomUUID().toString())}
        return obj.updateBpmnElemId(id)
    }

    private fun copied(
            shape: ShapeElement,
            delta: Point2D.Float,
            updatedIds: MutableMap<BpmnElementId, BpmnElementId>,
            updatedDiagramIds: MutableMap<DiagramElementId, DiagramElementId>): ShapeElement {
        val diagramId = DiagramElementId("sid-" + UUID.randomUUID().toString())
        val bpmnElementId = updatedIds[shape.bpmnElement]!!
        updatedDiagramIds[shape.id] = diagramId
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

    private fun copied(
            edge: EdgeWithIdentifiableWaypoints,
            delta: Point2D.Float,
            updatedIds: MutableMap<BpmnElementId, BpmnElementId>,
            updatedDiagramIds: MutableMap<DiagramElementId, DiagramElementId>): EdgeWithIdentifiableWaypoints {
        val diagramId = DiagramElementId("sid-" + UUID.randomUUID().toString())
        val bpmnElementId = updatedIds[edge.bpmnElement]
        updatedDiagramIds[edge.id] = diagramId

        return EdgeElementState(EdgeElement(diagramId, bpmnElementId, edge.waypoint.filter { it.physical }.map { it.asWaypointElement().copyAndTranslate(delta.x, delta.y) }) )
    }

    private fun copied(props: PropertyTable, updatedIds: MutableMap<BpmnElementId, BpmnElementId>): PropertyTable {
        val result = PropertyTable(mutableMapOf())
        props.forEach { k, v ->
            when {
                v.value == null -> result.add(k, v)
                PropertyType.ID == k -> result[k] = Property(copied(BpmnElementId(v.value as String), updatedIds).id)
                PropertyType.ID == k.updatedBy -> result[k] = Property(copiedExistsOrEmpty(BpmnElementId(v.value as String), updatedIds))
                else -> result.add(k, v)
            }
        }
        return result
    }

    private fun updateShapes(
            delta: Point2D.Float,
            shapes: MutableList<BpmnShapeObjectAddedEvent>,
            updatedIds: MutableMap<BpmnElementId, BpmnElementId>,
            updatedDiagramIds: MutableMap<DiagramElementId, DiagramElementId>): MutableList<BpmnShapeObjectAddedEvent> {
        val result = shapes.map {
            it.copy(
                    bpmnObject = it.bpmnObject.copy(
                            parent = copied(it.bpmnObject.parent, updatedIds),
                            element = copied(it.bpmnObject.element, updatedIds),
                            parentIdForXml = copied(it.bpmnObject.parentIdForXml, updatedIds)
                    )
            )
        }

        return result.map { it.copy(props = copied(it.props, updatedIds), shape = copied(it.shape, delta, updatedIds, updatedDiagramIds)) }.toMutableList()
    }

    private fun updateEdges(
            delta: Point2D.Float,
            edges: MutableList<BpmnEdgeObjectAddedEvent>,
            updatedIds: MutableMap<BpmnElementId, BpmnElementId>,
            updatedDiagramIds: MutableMap<DiagramElementId, DiagramElementId>): MutableList<BpmnEdgeObjectAddedEvent> {
        val result = edges.map {
            it.copy(
                    bpmnObject = it.bpmnObject.copy(
                            parent = copied(it.bpmnObject.parent, updatedIds),
                            element = copied(it.bpmnObject.element, updatedIds),
                            parentIdForXml = copied(it.bpmnObject.parentIdForXml, updatedIds)
                    )
            )
        }

        return result.map { it.copy(props = copied(it.props, updatedIds), edge = copied(it.edge, delta, updatedIds, updatedDiagramIds)) }.toMutableList()
    }

    private fun ensureRootElementsComeFirst(idsToCopy: MutableList<DiagramElementId>, ctx: RenderState, elementsById: Map<BpmnElementId, BaseDiagramRenderElement>): MutableList<DiagramElementId> {
        return idsToCopy
                .sortedByDescending { ctx.currentState.elementByDiagramId[it]?.let {id -> elementsById[id] }?.zIndex() ?: 0 }
                .toMutableList()
    }

    private fun elementIdsToCopyOrCut(state: RenderState): List<DiagramElementId> {
        val idsToWorkWith = mutableListOf<DiagramElementId>()
        idsToWorkWith += state.ctx.selectedIds.mapNotNull { getElementToBeIncluded(it, state) }
        idsToWorkWith += state.ctx.interactionContext.draggedIds.mapNotNull { getElementToBeIncluded(it, state) }
        return idsToWorkWith
    }

    private fun getElementToBeIncluded(id: DiagramElementId, state: RenderState): DiagramElementId? {
        // For sequence elements - handle them specially
        val elem = state.elemMap[id] ?: return null
        return when (elem) {
            is PhysicalWaypoint -> elem.owningEdgeId
            else -> elem.elementId
        }
    }

    private fun elementToAddEvents(
            ctx: RenderState,
            diagramId: DiagramElementId,
            elementsById: Map<BpmnElementId, BaseDiagramRenderElement>,
            events: ClipboardContext,
            preserveRoot: Boolean,
            idReplacements: MutableMap<BpmnElementId, BpmnElementId>,
            processedElementIds: MutableSet<BpmnElementId>
    ) {
        val bpmnId = ctx.currentState.elementByDiagramId[diagramId] ?: return
        val withParentId = ctx.currentState.elementByBpmnId[bpmnId] ?: return
        val props = ctx.currentState.elemPropertiesByStaticElementId[bpmnId] ?: return
        if (processedElementIds.contains(bpmnId)) {
            return
        }

        val renderElem = elementsById[bpmnId] ?: return
        fun detachParentIfNeeded() = if (preserveRoot) {
            withParentId.copy(
                    parent = idReplacements[withParentId.parent] ?: withParentId.parent,
                    parentIdForXml = idReplacements[withParentId.parentIdForXml] ?: withParentId.parentIdForXml
            )
        } else {
            val rootId = BpmnElementId(ROOT_NAME)
            idReplacements[withParentId.parent] = rootId
            withParentId.copy(parent = rootId, parentIdForXml = rootId)
        }

        when (renderElem) {
            is ShapeRenderElement -> {
                events.shapes += BpmnShapeObjectAddedEvent(
                        detachParentIfNeeded(),
                        renderElem.shapeElem,
                        props
                )

                renderElem.children.forEach { elementToAddEvents(ctx, it.elementId, elementsById, events, true, idReplacements, processedElementIds) }
            }
            is BaseEdgeRenderElement -> {
                events.edges += BpmnEdgeObjectAddedEvent(
                        detachParentIfNeeded(),
                        renderElem.edgeElem,
                        props
                )
            }
        }
        processedElementIds += bpmnId
    }

    private fun constructMapper(): ObjectMapper {
        val builtMapper: ObjectMapper = ObjectMapper().registerKotlinModule()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        builtMapper.setVisibility(builtMapper.serializationConfig.defaultVisibilityChecker
                .withFieldVisibility(JsonAutoDetect.Visibility.ANY)
                .withGetterVisibility(JsonAutoDetect.Visibility.NONE)
                .withSetterVisibility(JsonAutoDetect.Visibility.NONE)
                .withCreatorVisibility(JsonAutoDetect.Visibility.NONE))
        return builtMapper
    }

    @VisibleForTesting
    class ClipboardFlavor(private val data: String): Transferable, ClipboardOwner {

        override fun getTransferDataFlavors(): Array<DataFlavor>? {
            return arrayOf(DATA_FLAVOR)
        }

        override fun isDataFlavorSupported(flavor: DataFlavor): Boolean {
            return DATA_FLAVOR.equals(flavor)
        }

        @Throws(UnsupportedFlavorException::class, IOException::class)
        override fun getTransferData(flavor: DataFlavor): Any? {
            return data
        }

        override fun lostOwnership(clipboard: Clipboard?, contents: Transferable?) {}
    }
}
