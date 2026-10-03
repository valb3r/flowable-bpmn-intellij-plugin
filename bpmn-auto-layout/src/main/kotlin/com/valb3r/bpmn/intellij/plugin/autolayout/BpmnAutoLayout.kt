package com.valb3r.bpmn.intellij.plugin.autolayout

import com.valb3r.bpmn.intellij.plugin.bpmn.api.BpmnProcessObject
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.BpmnElementId
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.BpmnProcessBody
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.BpmnAssociation
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.BpmnSequenceFlow
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.WithBpmnId
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.events.boundary.*
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.types.BpmnBoundaryEventAlike
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.types.BpmnGatewayAlike
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.types.BpmnStartEventAlike
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.types.BpmnStructuralElementAlike
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.types.BpmnTaskAlike
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.types.EndEventAlike
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.types.IntermediateCatchingEventAlike
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.types.IntermediateThrowingEventAlike
import com.valb3r.bpmn.intellij.plugin.bpmn.api.diagram.DiagramElement
import com.valb3r.bpmn.intellij.plugin.bpmn.api.diagram.DiagramElementId
import com.valb3r.bpmn.intellij.plugin.bpmn.api.diagram.elements.BoundsElement
import com.valb3r.bpmn.intellij.plugin.bpmn.api.diagram.elements.EdgeElement
import com.valb3r.bpmn.intellij.plugin.bpmn.api.diagram.elements.PlaneElement
import com.valb3r.bpmn.intellij.plugin.bpmn.api.diagram.elements.ShapeElement
import com.valb3r.bpmn.intellij.plugin.bpmn.api.diagram.elements.WaypointElement
import org.eclipse.elk.alg.layered.options.LayeredMetaDataProvider
import org.eclipse.elk.alg.layered.options.LayeredOptions
import org.eclipse.elk.core.RecursiveGraphLayoutEngine
import org.eclipse.elk.core.data.LayoutMetaDataService
import org.eclipse.elk.core.options.CoreOptions
import org.eclipse.elk.core.options.Direction as ElkDirection
import org.eclipse.elk.core.options.EdgeRouting
import org.eclipse.elk.core.options.PortConstraints
import org.eclipse.elk.core.options.PortSide
import org.eclipse.elk.core.util.BasicProgressMonitor
import org.eclipse.elk.graph.ElkConnectableShape
import org.eclipse.elk.graph.ElkEdge
import org.eclipse.elk.graph.ElkNode
import org.eclipse.elk.graph.ElkPort
import org.eclipse.elk.graph.util.ElkGraphUtil

enum class BpmnLayoutDirection {
    RIGHT,
    LEFT,
    DOWN,
    UP,
}

/**
 * Generates BPMN DI for the shared BPMN model using ELK's layered algorithm.
 *
 * This module intentionally consumes only [BpmnProcessObject], the dialect-neutral model produced by
 * the parser modules. It does not read or write dialect-specific XML. Each process scope is emitted
 * as its own BPMN diagram, including flattened subprocess scopes in [BpmnProcessObject.process.children].
 *
 * The first version lays out flow nodes, sequence flows, text annotations, and associations. It uses
 * fixed default node sizes and docks attached boundary events to the corresponding activity. Node
 * spacing controls distances within a layer; layer spacing controls distances between successive
 * layers and leaves room for routed connectors.
 */
class BpmnAutoLayout(
    private val direction: BpmnLayoutDirection = BpmnLayoutDirection.RIGHT,
    private val nodeSpacing: Double = DEFAULT_NODE_SPACING,
    private val edgeSpacing: Double = DEFAULT_EDGE_SPACING,
    private val diagramMargin: Double = DEFAULT_DIAGRAM_MARGIN,
    private val layerSpacing: Double = DEFAULT_LAYER_SPACING,
) {
    data class LayoutResult(
        val laidOutProcess: BpmnProcessObject,
        val layoutApplied: Boolean,
    )

    init {
        require(nodeSpacing >= 0.0) { "Node spacing must be non-negative" }
        require(edgeSpacing >= 0.0) { "Edge spacing must be non-negative" }
        require(diagramMargin >= 0.0) { "Diagram margin must be non-negative" }
        require(layerSpacing >= 0.0) { "Layer spacing must be non-negative" }
    }

    /** Returns true when a scope or one of its supported BPMN elements lacks BPMN DI. */
    fun isRequired(processObject: BpmnProcessObject): Boolean {
        if (processObject.diagram.isEmpty()) return true

        val scopes = processScopes(processObject)
        return scopes.any { (scopeId, body) ->
            val diagram = processObject.diagram.firstOrNull { it.bpmnPlane.bpmnElement == scopeId }
                ?: return@any true
            val shapeIds = diagram.bpmnPlane.bpmnShape.orEmpty().mapTo(HashSet()) { it.bpmnElement.id }
            val flowNodes = body.flowNodes()
            if (flowNodes.any { it.id.id !in shapeIds }) return@any true

            val flowNodeIds = flowNodes.mapTo(HashSet()) { it.id.id }
            val edgeIds = diagram.bpmnPlane.bpmnEdge.orEmpty().mapNotNullTo(HashSet()) { it.bpmnElement?.id }
            body.connections().any { connection ->
                connection.sourceRef?.let { it in flowNodeIds } == true &&
                    connection.targetRef?.let { it in flowNodeIds } == true &&
                    connection.id.id !in edgeIds
            }
        }
    }

    /** Returns the process to render, applying constrained or full layout when needed. */
    fun layoutIfRequired(processObject: BpmnProcessObject, forceFullLayout: Boolean = false): LayoutResult {
        if (!forceFullLayout && !isRequired(processObject)) {
            return LayoutResult(processObject, layoutApplied = false)
        }

        val layoutInput = if (forceFullLayout) processObject.copy(diagram = emptyList()) else processObject
        return LayoutResult(layout(layoutInput), layoutApplied = true)
    }

    /** Returns a copy of [processObject] with missing BPMN DI shapes and edges allocated. */
    fun layout(processObject: BpmnProcessObject): BpmnProcessObject {
        registerElkLayeredAlgorithm()

        val scopes = processScopes(processObject)
        val ids = DiagramIdAllocator(processObject, scopes)
        val generatedLayouts = scopes.map { (scopeId, body) -> layoutScope(scopeId, body, ids) }
        return processObject.copy(diagram = mergeGeneratedLayouts(processObject.diagram, scopes, generatedLayouts))
    }

    private fun processScopes(processObject: BpmnProcessObject): List<Pair<BpmnElementId, BpmnProcessBody>> = buildList {
        processObject.process.body?.let { add(processObject.process.id to it) }
        processObject.process.children?.forEach { (id, body) -> add(id to body) }
    }

    private fun mergeGeneratedLayouts(
        existingDiagrams: List<DiagramElement>,
        scopes: List<Pair<BpmnElementId, BpmnProcessBody>>,
        generatedLayouts: List<DiagramElement>,
    ): List<DiagramElement> {
        val generatedByScope = generatedLayouts.associateBy { it.bpmnPlane.bpmnElement.id }
        val bodyByScope = scopes.associate { (scopeId, body) -> scopeId.id to body }
        val firstExistingIndexByScope = existingDiagrams
            .mapIndexed { index, diagram -> diagram.bpmnPlane.bpmnElement.id to index }
            .distinctBy { it.first }
            .toMap()

        val mergedExistingDiagrams = existingDiagrams.mapIndexed { index, existing ->
            val scopeId = existing.bpmnPlane.bpmnElement.id
            val generated = generatedByScope[scopeId]
            if (firstExistingIndexByScope[scopeId] == index && generated != null) {
                mergeMissingLayout(existing, generated, bodyByScope.getValue(scopeId))
            } else {
                existing
            }
        }
        val layoutsForNewScopes = generatedLayouts.filter { it.bpmnPlane.bpmnElement.id !in firstExistingIndexByScope }
        return mergedExistingDiagrams + layoutsForNewScopes
    }

    private fun mergeMissingLayout(
        existing: DiagramElement,
        generated: DiagramElement,
        body: BpmnProcessBody,
    ): DiagramElement {
        val context = createMissingLayoutContext(existing, generated, body) ?: return existing
        val placements = initialShapePlacements(context)

        placeShapesFromSequenceHints(context, placements)
        refineSequenceHintPositions(context, placements)
        relaxUnallocatedShapes(
            placements.placedShapes,
            placements.fixedShapePositions.keys,
            context.generatedShapesById,
            context.body,
        )
        placeBoundaryEvents(context, placements)

        val updatedEdges = mergeAndRerouteEdges(context, placements.placedShapes)
        val existingPlane = existing.bpmnPlane
        return existing.copy(
            bpmnPlane = existingPlane.copy(
                bpmnShape = context.existingShapes + context.missingShapesInPlacementOrder.map {
                    placements.placedShapes.getValue(it.bpmnElement.id)
                },
                bpmnEdge = updatedEdges,
            ),
        )
    }

    private fun createMissingLayoutContext(
        existing: DiagramElement,
        generated: DiagramElement,
        body: BpmnProcessBody,
    ): MissingLayoutContext? {
        val existingShapes = existing.bpmnPlane.bpmnShape.orEmpty()
        val generatedShapes = generated.bpmnPlane.bpmnShape.orEmpty()
        val existingEdges = existing.bpmnPlane.bpmnEdge.orEmpty()
        val generatedEdges = generated.bpmnPlane.bpmnEdge.orEmpty()
        val existingShapeIds = existingShapes.mapTo(HashSet()) { it.bpmnElement }
        val existingEdgeIds = existingEdges.mapNotNullTo(HashSet()) { it.bpmnElement }
        val missingShapes = generatedShapes.filterNot { it.bpmnElement in existingShapeIds }
        val missingEdges = generatedEdges.filter { it.bpmnElement != null && it.bpmnElement !in existingEdgeIds }
        if (missingShapes.isEmpty() && missingEdges.isEmpty()) return null

        val bodyElementsById = body.flowNodes().associateBy { it.id.id }
        val missingShapesInPlacementOrder = missingShapes.partition { bodyElementsById[it.bpmnElement.id]?.isBoundaryEvent() != true }
            .let { (regularShapes, boundaryShapes) -> regularShapes + boundaryShapes }
        return MissingLayoutContext(
            body = body,
            existingShapes = existingShapes,
            existingEdges = existingEdges,
            missingEdges = missingEdges,
            missingShapeIds = missingShapes.mapTo(HashSet()) { it.bpmnElement.id },
            generatedShapesById = generatedShapes.associateBy { it.bpmnElement.id },
            existingEdgesById = existingEdges.mapNotNull { edge -> edge.bpmnElement?.id?.let { it to edge } }.toMap(),
            generatedEdgesById = generatedEdges.mapNotNull { edge -> edge.bpmnElement?.id?.let { it to edge } }.toMap(),
            bodyElementsById = bodyElementsById,
            connectionsById = body.connections().associateBy { it.id.id },
            missingShapesInPlacementOrder = missingShapesInPlacementOrder,
            originallyAllocatedShapeIds = existingShapeIds.mapTo(HashSet()) { it.id },
            fallbackOffset = unanchoredOffset(existingShapes, generatedShapes),
        )
    }

    private fun initialShapePlacements(context: MissingLayoutContext): ShapePlacementState {
        val placedShapes = LinkedHashMap<String, ShapeElement>()
        context.existingShapes.forEach { placedShapes[it.bpmnElement.id] = it }
        return ShapePlacementState(
            placedShapes = placedShapes,
            fixedShapePositions = LinkedHashMap(placedShapes),
        )
    }

    private fun placeShapesFromSequenceHints(context: MissingLayoutContext, placements: ShapePlacementState) {
        val pendingShapes = context.missingShapesInPlacementOrder
            .filterNot { context.bodyElementsById[it.bpmnElement.id]?.isBoundaryEvent() == true }
            .toMutableList()

        while (pendingShapes.isNotEmpty()) {
            val nextAnchoredIndex = pendingShapes.indexOfFirst { shape ->
                inferShapePlacementFromSequences(shape, context, placements.fixedShapePositions) != null
            }
            val shape = pendingShapes.removeAt(if (nextAnchoredIndex >= 0) nextAnchoredIndex else 0)
            val placement = inferShapePlacementFromSequences(shape, context, placements.fixedShapePositions)
            if (placement == null) {
                placements.placedShapes[shape.bpmnElement.id] = shape.copyAndTranslate(context.fallbackOffset)
            } else {
                placements.placedShapes[shape.bpmnElement.id] = placement
                placements.fixedShapePositions[shape.bpmnElement.id] = placement
            }
        }
    }

    /** Reconcile sequence-hinted shapes after every initially hinted neighbor has been placed. */
    private fun refineSequenceHintPositions(context: MissingLayoutContext, placements: ShapePlacementState) {
        val allPositionHints = LinkedHashMap(placements.fixedShapePositions)
        val missingShapesById = context.missingShapesInPlacementOrder.associateBy { it.bpmnElement.id }
        allPositionHints.keys.filterNot { it in context.originallyAllocatedShapeIds }.forEach { id ->
            val shape = missingShapesById.getValue(id)
            val refinedPlacement = inferShapePlacementFromSequences(shape, context, allPositionHints)
                ?: return@forEach
            placements.fixedShapePositions[id] = refinedPlacement
            placements.placedShapes[id] = refinedPlacement
        }
    }

    private fun placeBoundaryEvents(context: MissingLayoutContext, placements: ShapePlacementState) {
        context.missingShapesInPlacementOrder
            .filter { context.bodyElementsById[it.bpmnElement.id]?.isBoundaryEvent() == true }
            .forEach { boundaryShape ->
                val attachedToId = context.bodyElementsById[boundaryShape.bpmnElement.id]
                    ?.takeIf { it.isBoundaryEvent() }
                    ?.attachedToRef()
                    ?.id
                val generatedHost = attachedToId?.let(context.generatedShapesById::get)
                val placedHost = attachedToId?.let(placements.placedShapes::get)
                val placedBoundary = if (generatedHost != null && placedHost != null) {
                    dockShapeToHost(boundaryShape, generatedHost, placedHost)
                } else {
                    boundaryShape.copyAndTranslate(context.fallbackOffset)
                }
                placements.placedShapes[boundaryShape.bpmnElement.id] = placedBoundary
            }
    }

    private fun mergeAndRerouteEdges(
        context: MissingLayoutContext,
        placedShapes: Map<String, ShapeElement>,
    ): List<EdgeElement> {
        val refreshedExistingEdges = context.existingEdges.map { edge ->
            if (edgeTouchesMissingShape(edge, context)) rerouteEdge(edge, placedShapes, context) else edge
        }
        val newEdges = context.missingEdges.map { edge -> rerouteEdge(edge, placedShapes, context) }
        return refreshedExistingEdges + newEdges
    }

    private fun edgeTouchesMissingShape(edge: EdgeElement, context: MissingLayoutContext): Boolean {
        val connection = edge.bpmnElement?.let { context.connectionsById[it.id] } ?: return false
        return connection.sourceRef?.let { it in context.missingShapeIds } == true ||
            connection.targetRef?.let { it in context.missingShapeIds } == true
    }

    private fun rerouteEdge(
        edge: EdgeElement,
        placedShapes: Map<String, ShapeElement>,
        context: MissingLayoutContext,
    ): EdgeElement {
        val connection = edge.bpmnElement?.let { context.connectionsById[it.id] }
        val source = connection?.sourceRef?.let(placedShapes::get)
        val target = connection?.targetRef?.let(placedShapes::get)
        return if (source != null && target != null) {
            edge.copy(waypoint = routeNewEdge(source, target))
        } else {
            edge.copy(waypoint = edge.waypoint.orEmpty().map { it.translated(context.fallbackOffset) })
        }
    }

    private fun ShapeElement.copyAndTranslate(offset: Pair<Float, Float>): ShapeElement =
        copyAndTranslate(offset.first, offset.second)

    private fun WaypointElement.translated(offset: Pair<Float, Float>): WaypointElement =
        copy(x = x + offset.first, y = y + offset.second)

    private data class MissingLayoutContext(
        val body: BpmnProcessBody,
        val existingShapes: List<ShapeElement>,
        val existingEdges: List<EdgeElement>,
        val missingEdges: List<EdgeElement>,
        val missingShapeIds: Set<String>,
        val generatedShapesById: Map<String, ShapeElement>,
        val existingEdgesById: Map<String, EdgeElement>,
        val generatedEdgesById: Map<String, EdgeElement>,
        val bodyElementsById: Map<String, WithBpmnId>,
        val connectionsById: Map<String, WithBpmnId>,
        val missingShapesInPlacementOrder: List<ShapeElement>,
        val originallyAllocatedShapeIds: Set<String>,
        val fallbackOffset: Pair<Float, Float>,
    )

    private data class ShapePlacementState(
        val placedShapes: LinkedHashMap<String, ShapeElement>,
        val fixedShapePositions: LinkedHashMap<String, ShapeElement>,
    )

    private fun inferShapePlacementFromSequences(
        shape: ShapeElement,
        context: MissingLayoutContext,
        allocatedShapes: Map<String, ShapeElement>,
    ): ShapeElement? {
        val shapeId = shape.bpmnElement.id
        if (shapeId !in context.generatedShapesById) return null
        val sequenceFlows = context.connectionsById.values.filterIsInstance<BpmnSequenceFlow>()
        val predecessors = sequenceFlows
            .filter { it.targetRef == shapeId }
            .mapNotNull { it.sourceRef?.let(allocatedShapes::get) }
            .distinctBy { it.bpmnElement.id }
        val successors = sequenceFlows
            .filter { it.sourceRef == shapeId }
            .mapNotNull { it.targetRef?.let(allocatedShapes::get) }
            .distinctBy { it.bpmnElement.id }
        val neighbors = (predecessors + successors).distinctBy { it.bpmnElement.id }

        val edgeEndpointPlacement = positionFromSequenceEdgeEndpoints(
            shape,
            sequenceAttachmentPoints(
                shapeId,
                allocatedShapes,
                context.generatedShapesById,
                context.connectionsById,
                context.existingEdgesById,
                context.generatedEdgesById,
            ),
        )
        return edgeEndpointPlacement ?: positionFromAllocatedSequenceNeighbors(
            shape,
            context.generatedShapesById,
            predecessors,
            successors,
            neighbors,
        )
    }

    private fun positionFromSequenceEdgeEndpoints(
        shape: ShapeElement,
        attachmentPoints: List<SequenceAttachmentPoint>,
    ): ShapeElement? {
        val bounds = shape.rectBounds()
        val hintedCenters = attachmentPoints.mapNotNull { attachment ->
            when (attachment.side) {
                SequenceAttachmentSide.LEFT -> (attachment.point.x + bounds.width / 2.0f) to attachment.point.y
                SequenceAttachmentSide.RIGHT -> (attachment.point.x - bounds.width / 2.0f) to attachment.point.y
                SequenceAttachmentSide.TOP -> attachment.point.x to (attachment.point.y + bounds.height / 2.0f)
                SequenceAttachmentSide.BOTTOM -> attachment.point.x to (attachment.point.y - bounds.height / 2.0f)
                null -> null
            }
        }
        if (hintedCenters.isEmpty()) return null

        // Average multiple route hints to reconcile small inconsistencies without moving their anchors.
        val centerX = hintedCenters.map { it.first }.average().toFloat()
        val centerY = hintedCenters.map { it.second }.average().toFloat()
        return shape.copyAndTranslate(centerX - bounds.centerX.toFloat(), centerY - bounds.centerY.toFloat())
    }

    private fun positionFromAllocatedSequenceNeighbors(
        shape: ShapeElement,
        generatedShapes: Map<String, ShapeElement>,
        predecessors: List<ShapeElement>,
        successors: List<ShapeElement>,
        neighbors: List<ShapeElement>,
    ): ShapeElement? {
        if (neighbors.isEmpty()) return null

        val axisSign = flowAxisSign()
        val hintOffsets = neighbors.mapNotNull { allocatedNeighbor ->
            generatedShapes[allocatedNeighbor.bpmnElement.id]?.let { generatedNeighbor ->
                flowAxisCenter(allocatedNeighbor) - flowAxisCenter(generatedNeighbor)
            }
        }
        val averageHintOffset = hintOffsets.takeIf { it.isNotEmpty() }?.average()?.toFloat() ?: 0.0f
        val hintAxisCenter = flowAxisCenter(shape) + averageHintOffset
        return positionBetweenSequenceNeighbors(shape, predecessors, successors, neighbors, axisSign, hintAxisCenter)
    }

    private fun positionBetweenSequenceNeighbors(
        shape: ShapeElement,
        predecessors: List<ShapeElement>,
        successors: List<ShapeElement>,
        neighbors: List<ShapeElement>,
        axisSign: Float,
        hintAxisCenter: Float,
    ): ShapeElement? {
        val bounds = shape.rectBounds()
        val halfSize = flowAxisHalfSize(shape)
        val minimumCenter = predecessors.maxOfOrNull { predecessor ->
            flowAxisCenter(predecessor) + flowAxisHalfSize(predecessor) + layerSpacing.toFloat() + halfSize
        }
        val maximumCenter = successors.minOfOrNull { successor ->
            flowAxisCenter(successor) - flowAxisHalfSize(successor) - layerSpacing.toFloat() - halfSize
        }
        val targetAxisCenter = constrainedAxisCenter(hintAxisCenter, minimumCenter, maximumCenter) ?: return null
        val crossAxisCenter = neighbors.map(::crossAxisCenter).average().toFloat()
        val targetCenter = centerAtAxis(targetAxisCenter * axisSign, crossAxisCenter)
        return shape.copyAndTranslate(
            targetCenter.first - bounds.centerX.toFloat(),
            targetCenter.second - bounds.centerY.toFloat(),
        )
    }

    private fun constrainedAxisCenter(hint: Float, minimum: Float?, maximum: Float?): Float? = when {
        minimum != null && maximum != null && minimum <= maximum -> hint.coerceIn(minimum, maximum)
        minimum != null && maximum != null -> (minimum + maximum) / 2.0f
        minimum != null -> minimum
        maximum != null -> maximum
        else -> null
    }

    private fun flowAxisSign(): Float = when (direction) {
        BpmnLayoutDirection.RIGHT, BpmnLayoutDirection.DOWN -> 1.0f
        BpmnLayoutDirection.LEFT, BpmnLayoutDirection.UP -> -1.0f
    }

    private fun flowAxisCenter(shape: ShapeElement): Float {
        val bounds = shape.rectBounds()
        val center = when (direction) {
            BpmnLayoutDirection.RIGHT, BpmnLayoutDirection.LEFT -> bounds.centerX.toFloat()
            BpmnLayoutDirection.DOWN, BpmnLayoutDirection.UP -> bounds.centerY.toFloat()
        }
        return center * flowAxisSign()
    }

    private fun flowAxisHalfSize(shape: ShapeElement): Float {
        val bounds = shape.rectBounds()
        return when (direction) {
            BpmnLayoutDirection.RIGHT, BpmnLayoutDirection.LEFT -> bounds.width / 2.0f
            BpmnLayoutDirection.DOWN, BpmnLayoutDirection.UP -> bounds.height / 2.0f
        }
    }

    private fun crossAxisCenter(shape: ShapeElement): Double {
        val bounds = shape.rectBounds()
        return when (direction) {
            BpmnLayoutDirection.RIGHT, BpmnLayoutDirection.LEFT -> bounds.centerY
            BpmnLayoutDirection.DOWN, BpmnLayoutDirection.UP -> bounds.centerX
        }
    }

    private fun centerAtAxis(axisCenter: Float, crossAxisCenter: Float): Pair<Float, Float> = when (direction) {
        BpmnLayoutDirection.RIGHT, BpmnLayoutDirection.LEFT -> axisCenter to crossAxisCenter
        BpmnLayoutDirection.DOWN, BpmnLayoutDirection.UP -> crossAxisCenter to axisCenter
    }

    private fun sequenceAttachmentPoints(
        shapeId: String,
        allocatedShapes: Map<String, ShapeElement>,
        generatedShapes: Map<String, ShapeElement>,
        connections: Map<String, WithBpmnId>,
        existingEdges: Map<String, EdgeElement>,
        generatedEdges: Map<String, EdgeElement>,
    ): List<SequenceAttachmentPoint> = connections.values
        .filterIsInstance<BpmnSequenceFlow>()
        .mapNotNull { flow ->
            sequenceAttachmentPoint(
                shapeId,
                flow,
                allocatedShapes,
                generatedShapes,
                existingEdges,
                generatedEdges,
            )
        }

    private fun sequenceAttachmentPoint(
        shapeId: String,
        flow: BpmnSequenceFlow,
        allocatedShapes: Map<String, ShapeElement>,
        generatedShapes: Map<String, ShapeElement>,
        existingEdges: Map<String, EdgeElement>,
        generatedEdges: Map<String, EdgeElement>,
    ): SequenceAttachmentPoint? {
        val (otherId, attachedEndpointIndex) = when (shapeId) {
            flow.targetRef -> (flow.sourceRef ?: return null) to -1
            flow.sourceRef -> (flow.targetRef ?: return null) to 0
            else -> return null
        }

        val existingWaypoints = existingEdges[flow.id.id]?.waypoint.orEmpty()
        val existingPoint = existingWaypoints.endpointAt(attachedEndpointIndex)

        if (existingPoint != null) {
            val generatedOtherBounds = generatedShapes[otherId]?.rectBounds()
            val actualOtherBounds = allocatedShapes[otherId]?.rectBounds()
            val translatedPoint = if (generatedOtherBounds != null && actualOtherBounds != null) {
                existingPoint.copy(
                    x = existingPoint.x + actualOtherBounds.x - generatedOtherBounds.x,
                    y = existingPoint.y + actualOtherBounds.y - generatedOtherBounds.y,
                )
            } else {
                existingPoint
            }
            return SequenceAttachmentPoint(translatedPoint, existingWaypoints.sideAtEndpoint(attachedEndpointIndex))
        }

        // Generated routes can hint only when the opposite shape has a known placement.
        if (otherId !in allocatedShapes) return null
        val generatedWaypoints = generatedEdges[flow.id.id]?.waypoint.orEmpty()
        val generatedPoint = generatedWaypoints.endpointAt(attachedEndpointIndex) ?: return null
        val generatedOtherBounds = generatedShapes[otherId]?.rectBounds() ?: return null
        val actualOtherBounds = allocatedShapes.getValue(otherId).rectBounds()
        val offsetX = actualOtherBounds.x - generatedOtherBounds.x
        val offsetY = actualOtherBounds.y - generatedOtherBounds.y
        return SequenceAttachmentPoint(
            WaypointElement(generatedPoint.x + offsetX, generatedPoint.y + offsetY),
            generatedWaypoints.sideAtEndpoint(attachedEndpointIndex),
        )
    }

    private fun List<WaypointElement>.endpointAt(index: Int): WaypointElement? = when {
        isEmpty() -> null
        index == 0 -> first()
        else -> last()
    }

    private fun List<WaypointElement>.sideAtEndpoint(index: Int): SequenceAttachmentSide? {
        if (size < 2) return null
        val endpointIndex = if (index == 0) 0 else lastIndex
        val endpoint = this[endpointIndex]
        val adjacent = if (endpointIndex == 0) {
            drop(1).firstOrNull { it.x != endpoint.x || it.y != endpoint.y }
        } else {
            dropLast(1).lastOrNull { it.x != endpoint.x || it.y != endpoint.y }
        } ?: return null
        val dx = adjacent.x - endpoint.x
        val dy = adjacent.y - endpoint.y
        if (dx == 0.0f && dy == 0.0f) return null
        return if (kotlin.math.abs(dx) >= kotlin.math.abs(dy)) {
            if (dx > 0.0f) SequenceAttachmentSide.RIGHT else SequenceAttachmentSide.LEFT
        } else {
            if (dy > 0.0f) SequenceAttachmentSide.BOTTOM else SequenceAttachmentSide.TOP
        }
    }

    private data class SequenceAttachmentPoint(
        val point: WaypointElement,
        val side: SequenceAttachmentSide?,
    )

    private enum class SequenceAttachmentSide { LEFT, RIGHT, TOP, BOTTOM }

    private fun relaxUnallocatedShapes(
        shapes: MutableMap<String, ShapeElement>,
        fixedShapeIds: Set<String>,
        generatedShapes: Map<String, ShapeElement>,
        body: BpmnProcessBody,
    ) {
        val layout = createForceLayout(shapes, fixedShapeIds, generatedShapes, body)
        if (layout.movableIds.isEmpty()) return

        repeat(FORCE_LAYOUT_ITERATIONS) {
            val forces = zeroForces(layout.positions.keys)
            applySequenceFlowForces(layout, forces)
            applyShapeOverlapForces(layout, forces)
            moveShapesOneForceStep(layout, forces)
        }
        writeForcePositionsBack(shapes, layout)
    }

    private fun createForceLayout(
        shapes: Map<String, ShapeElement>,
        fixedShapeIds: Set<String>,
        generatedShapes: Map<String, ShapeElement>,
        body: BpmnProcessBody,
    ): ForceLayout {
        // Allocated and sequence-hinted shapes stay fixed; the force pass moves the rest.
        val positions = shapes.mapValuesTo(LinkedHashMap()) { (_, shape) ->
            val bounds = shape.rectBounds()
            ForcePoint(bounds.centerX, bounds.centerY)
        }
        val movableIds = positions.keys.filterNot { it in fixedShapeIds }.toSet()
        val initialPositions = positions.mapValues { (_, point) -> ForcePoint(point.x, point.y) }
        val boundsById = shapes.mapValues { (_, shape) -> shape.rectBounds() }
        val sequenceConstraints = body.sequenceFlow.orEmpty().mapNotNull { flow ->
            createSequenceFlowConstraint(flow.sourceRef, flow.targetRef, positions.keys, movableIds, generatedShapes)
        }
        return ForceLayout(
            movableIds = movableIds,
            orderedMovableIds = movableIds.sorted(),
            positions = positions,
            initialPositions = initialPositions,
            boundsById = boundsById,
            sequenceConstraints = sequenceConstraints,
        )
    }

    private fun createSequenceFlowConstraint(
        sourceId: String?,
        targetId: String?,
        allocatedIds: Set<String>,
        movableIds: Set<String>,
        generatedShapes: Map<String, ShapeElement>,
    ): SequenceFlowConstraint? {
        val source = sourceId ?: return null
        val target = targetId ?: return null
        if (source !in allocatedIds || target !in allocatedIds || (source !in movableIds && target !in movableIds)) return null

        val sourceBounds = generatedShapes[source]?.rectBounds() ?: return null
        val targetBounds = generatedShapes[target]?.rectBounds() ?: return null
        val rawDx = (targetBounds.centerX - sourceBounds.centerX).toDouble()
        val rawDy = (targetBounds.centerY - sourceBounds.centerY).toDouble()
        val axisSign = flowAxisSign().toDouble()
        val rawAxisDelta = when (direction) {
            BpmnLayoutDirection.RIGHT, BpmnLayoutDirection.LEFT -> rawDx
            BpmnLayoutDirection.DOWN, BpmnLayoutDirection.UP -> rawDy
        }
        val sourceAxisHalf = flowAxisHalfSize(sourceBounds)
        val targetAxisHalf = flowAxisHalfSize(targetBounds)
        val preferredSign = if (rawAxisDelta * axisSign >= 0.0) axisSign else -axisSign
        val axisDelta = preferredSign * maxOf(
            kotlin.math.abs(rawAxisDelta),
            sourceAxisHalf + targetAxisHalf + layerSpacing,
        )
        val (restX, restY) = when (direction) {
            BpmnLayoutDirection.RIGHT, BpmnLayoutDirection.LEFT -> axisDelta to rawDy
            BpmnLayoutDirection.DOWN, BpmnLayoutDirection.UP -> rawDx to axisDelta
        }
        return SequenceFlowConstraint(source, target, restX, restY)
    }

    private fun flowAxisHalfSize(bounds: java.awt.geom.Rectangle2D): Double = when (direction) {
        BpmnLayoutDirection.RIGHT, BpmnLayoutDirection.LEFT -> bounds.width / 2.0
        BpmnLayoutDirection.DOWN, BpmnLayoutDirection.UP -> bounds.height / 2.0
    }

    private fun zeroForces(shapeIds: Set<String>): Map<String, ForcePoint> =
        shapeIds.associateWith { ForcePoint(0.0, 0.0) }

    private fun applySequenceFlowForces(layout: ForceLayout, forces: Map<String, ForcePoint>) {
        layout.sequenceConstraints.forEach { constraint ->
            val source = layout.positions.getValue(constraint.sourceId)
            val target = layout.positions.getValue(constraint.targetId)
            val errorX = (target.x - source.x) - constraint.restX
            val errorY = (target.y - source.y) - constraint.restY
            if (constraint.sourceId in layout.movableIds) {
                val force = forces.getValue(constraint.sourceId)
                force.x += errorX * FORCE_SPRING_STRENGTH
                force.y += errorY * FORCE_SPRING_STRENGTH
            }
            if (constraint.targetId in layout.movableIds) {
                val force = forces.getValue(constraint.targetId)
                force.x -= errorX * FORCE_SPRING_STRENGTH
                force.y -= errorY * FORCE_SPRING_STRENGTH
            }
        }
    }

    private fun applyShapeOverlapForces(layout: ForceLayout, forces: Map<String, ForcePoint>) {
        val allShapeIds = layout.positions.keys.toList()
        for (movableId in layout.orderedMovableIds) {
            for (otherId in allShapeIds) {
                if (movableId == otherId || (otherId in layout.movableIds && movableId > otherId)) continue
                applyPairwiseOverlapForce(movableId, otherId, layout, forces)
            }
        }
    }

    private fun applyPairwiseOverlapForce(
        firstId: String,
        secondId: String,
        layout: ForceLayout,
        forces: Map<String, ForcePoint>,
    ) {
        val first = layout.positions.getValue(firstId)
        val second = layout.positions.getValue(secondId)
        val firstBounds = layout.boundsById.getValue(firstId)
        val secondBounds = layout.boundsById.getValue(secondId)
        val deltaX = second.x - first.x
        val deltaY = second.y - first.y
        val overlapX = (firstBounds.width + secondBounds.width) / 2.0 + nodeSpacing - kotlin.math.abs(deltaX)
        val overlapY = (firstBounds.height + secondBounds.height) / 2.0 + nodeSpacing - kotlin.math.abs(deltaY)
        if (overlapX <= 0.0 || overlapY <= 0.0) return

        val forceX = overlapX < overlapY
        val delta = if (forceX) deltaX else deltaY
        val sign = when {
            delta > 0.01 -> 1.0
            delta < -0.01 -> -1.0
            firstId < secondId -> 1.0
            else -> -1.0
        }
        val magnitude = (if (forceX) overlapX else overlapY) * FORCE_REPULSION_STRENGTH
        val firstForce = forces.getValue(firstId)
        if (forceX) firstForce.x -= sign * magnitude else firstForce.y -= sign * magnitude
        if (secondId in layout.movableIds) {
            val secondForce = forces.getValue(secondId)
            if (forceX) secondForce.x += sign * magnitude else secondForce.y += sign * magnitude
        }
    }

    private fun moveShapesOneForceStep(layout: ForceLayout, forces: Map<String, ForcePoint>) {
        layout.movableIds.forEach { id ->
            val point = layout.positions.getValue(id)
            val initial = layout.initialPositions.getValue(id)
            val force = forces.getValue(id)
            force.x += (initial.x - point.x) * FORCE_ANCHOR_STRENGTH
            force.y += (initial.y - point.y) * FORCE_ANCHOR_STRENGTH
            val forceLength = kotlin.math.hypot(force.x, force.y)
            val stepScale = if (forceLength > FORCE_MAX_STEP) FORCE_MAX_STEP / forceLength else 1.0
            point.x += force.x * stepScale
            point.y += force.y * stepScale
        }
    }

    private fun writeForcePositionsBack(shapes: MutableMap<String, ShapeElement>, layout: ForceLayout) {
        layout.movableIds.forEach { id ->
            val shape = shapes.getValue(id)
            val bounds = shape.rectBounds()
            val position = layout.positions.getValue(id)
            shapes[id] = shape.copyAndTranslate(
                (position.x - bounds.centerX).toFloat(),
                (position.y - bounds.centerY).toFloat(),
            )
        }
    }

    private data class ForcePoint(var x: Double, var y: Double)

    private data class SequenceFlowConstraint(
        val sourceId: String,
        val targetId: String,
        val restX: Double,
        val restY: Double,
    )

    private data class ForceLayout(
        val movableIds: Set<String>,
        val orderedMovableIds: List<String>,
        val positions: LinkedHashMap<String, ForcePoint>,
        val initialPositions: Map<String, ForcePoint>,
        val boundsById: Map<String, java.awt.geom.Rectangle2D>,
        val sequenceConstraints: List<SequenceFlowConstraint>,
    )

    private fun routeNewEdge(
        source: ShapeElement,
        target: ShapeElement,
    ): List<WaypointElement> {
        val start = connectionPoint(source, isSource = true)
        val end = connectionPoint(target, isSource = false)
        val crossAxisAligned = when (direction) {
            BpmnLayoutDirection.RIGHT, BpmnLayoutDirection.LEFT -> kotlin.math.abs(start.y - end.y) < 0.01f
            BpmnLayoutDirection.DOWN, BpmnLayoutDirection.UP -> kotlin.math.abs(start.x - end.x) < 0.01f
        }
        val forward = when (direction) {
            BpmnLayoutDirection.RIGHT -> start.x <= end.x
            BpmnLayoutDirection.LEFT -> start.x >= end.x
            BpmnLayoutDirection.DOWN -> start.y <= end.y
            BpmnLayoutDirection.UP -> start.y >= end.y
        }
        if (crossAxisAligned && forward) return listOf(start, end)

        val spacing = edgeSpacing.toFloat()
        val middle = when (direction) {
            BpmnLayoutDirection.RIGHT -> if (start.x <= end.x) (start.x + end.x) / 2.0f else maxOf(start.x, end.x) + spacing
            BpmnLayoutDirection.LEFT -> if (start.x >= end.x) (start.x + end.x) / 2.0f else minOf(start.x, end.x) - spacing
            BpmnLayoutDirection.DOWN -> if (start.y <= end.y) (start.y + end.y) / 2.0f else maxOf(start.y, end.y) + spacing
            BpmnLayoutDirection.UP -> if (start.y >= end.y) (start.y + end.y) / 2.0f else minOf(start.y, end.y) - spacing
        }
        return when (direction) {
            BpmnLayoutDirection.RIGHT, BpmnLayoutDirection.LEFT -> listOf(
                start,
                WaypointElement(middle, start.y),
                WaypointElement(middle, end.y),
                end,
            )
            BpmnLayoutDirection.DOWN, BpmnLayoutDirection.UP -> listOf(
                start,
                WaypointElement(start.x, middle),
                WaypointElement(end.x, middle),
                end,
            )
        }.distinct()
    }

    private fun connectionPoint(shape: ShapeElement, isSource: Boolean): WaypointElement {
        val bounds = shape.rectBounds()
        val centerX = bounds.centerX.toFloat()
        val centerY = bounds.centerY.toFloat()
        // Circular BPMN events and rectangular activities share cardinal extrema on their bounds.
        return when (direction) {
            BpmnLayoutDirection.RIGHT -> WaypointElement(if (isSource) bounds.maxX.toFloat() else bounds.x, centerY)
            BpmnLayoutDirection.LEFT -> WaypointElement(if (isSource) bounds.x else bounds.maxX.toFloat(), centerY)
            BpmnLayoutDirection.DOWN -> WaypointElement(centerX, if (isSource) bounds.maxY.toFloat() else bounds.y)
            BpmnLayoutDirection.UP -> WaypointElement(centerX, if (isSource) bounds.y else bounds.maxY.toFloat())
        }
    }

    private fun unanchoredOffset(
        existingShapes: List<ShapeElement>,
        generatedShapes: List<ShapeElement>,
    ): Pair<Float, Float> {
        if (existingShapes.isEmpty() || generatedShapes.isEmpty()) return 0.0f to 0.0f

        val existingBounds = existingShapes.map { it.rectBounds() }
        val generatedBounds = generatedShapes.map { it.rectBounds() }
        val existingLeft = existingBounds.minOf { it.x }
        val existingTop = existingBounds.minOf { it.y }
        val existingRight = existingBounds.maxOf { it.maxX.toFloat() }
        val existingBottom = existingBounds.maxOf { it.maxY.toFloat() }
        val generatedLeft = generatedBounds.minOf { it.x }
        val generatedTop = generatedBounds.minOf { it.y }
        val generatedRight = generatedBounds.maxOf { it.maxX.toFloat() }
        val generatedBottom = generatedBounds.maxOf { it.maxY.toFloat() }
        return when (direction) {
            BpmnLayoutDirection.RIGHT -> existingRight + nodeSpacing.toFloat() - generatedLeft to existingTop - generatedTop
            BpmnLayoutDirection.LEFT -> existingLeft - nodeSpacing.toFloat() - generatedRight to existingTop - generatedTop
            BpmnLayoutDirection.DOWN -> existingLeft - generatedLeft to existingBottom + nodeSpacing.toFloat() - generatedTop
            BpmnLayoutDirection.UP -> existingLeft - generatedLeft to existingTop - nodeSpacing.toFloat() - generatedBottom
        }
    }

    private fun dockShapeToHost(shape: ShapeElement, generatedHost: ShapeElement, actualHost: ShapeElement): ShapeElement {
        val shapeBounds = shape.rectBounds()
        val generatedBounds = generatedHost.rectBounds()
        val actualBounds = actualHost.rectBounds()
        val centerX = shapeBounds.centerX.toFloat()
        val centerY = shapeBounds.centerY.toFloat()
        val alongHost = when (direction) {
            BpmnLayoutDirection.RIGHT, BpmnLayoutDirection.LEFT ->
                ((centerY - generatedBounds.y) / generatedBounds.height).coerceIn(0.0f, 1.0f)
            BpmnLayoutDirection.DOWN, BpmnLayoutDirection.UP ->
                ((centerX - generatedBounds.x) / generatedBounds.width).coerceIn(0.0f, 1.0f)
        }
        val targetCenterX = when (direction) {
            BpmnLayoutDirection.RIGHT -> actualBounds.maxX.toFloat()
            BpmnLayoutDirection.LEFT -> actualBounds.x
            BpmnLayoutDirection.DOWN, BpmnLayoutDirection.UP -> actualBounds.x + alongHost * actualBounds.width
        }
        val targetCenterY = when (direction) {
            BpmnLayoutDirection.RIGHT, BpmnLayoutDirection.LEFT -> actualBounds.y + alongHost * actualBounds.height
            BpmnLayoutDirection.DOWN -> actualBounds.maxY.toFloat()
            BpmnLayoutDirection.UP -> actualBounds.y
        }
        return shape.copyAndTranslate(targetCenterX - centerX, targetCenterY - centerY)
    }

    private fun layoutScope(
        scopeId: BpmnElementId,
        body: BpmnProcessBody,
        ids: DiagramIdAllocator,
    ): DiagramElement {
        val scopedElements = collectScopedElements(body)
        val elkGraph = buildElkGraph(scopedElements)
        RecursiveGraphLayoutEngine().layout(elkGraph.graph, BasicProgressMonitor())

        val generatedShapes = createDiagramShapes(scopeId, ids, elkGraph)
        val generatedEdges = createDiagramEdges(scopeId, ids, elkGraph, generatedShapes.boundsByElementId)
        val plane = PlaneElement(
            id = DiagramElementId(ids.allocate("AUTO_PLANE_${scopeId.id}")),
            bpmnElement = scopeId,
            bpmnShape = generatedShapes.shapes,
            bpmnEdge = generatedEdges,
        )
        return DiagramElement(
            id = DiagramElementId(ids.allocate("AUTO_DIAGRAM_${scopeId.id}")),
            bpmnPlane = plane,
        )
    }

    private fun collectScopedElements(body: BpmnProcessBody): ScopedElements {
        val elements = body.flowNodes().distinctBy { it.id.id }
        val elementIds = elements.mapTo(HashSet()) { it.id.id }
        val connections = body.connections()
            .filter { connection ->
                val source = connection.sourceRef
                val target = connection.targetRef
                source != null && target != null && source in elementIds && target in elementIds
            }
            .distinctBy { it.id.id }
        return ScopedElements(elements, connections)
    }

    private fun buildElkGraph(elements: ScopedElements): ElkScopeGraph {
        val graph = ElkGraphUtil.createGraph()
        configureElkGraph(graph)

        val elementsById = elements.nodes.associateBy { it.id.id }
        val attachedBoundaryHosts = findAttachedBoundaryHosts(elements.nodes, elementsById)
        val elkNodes = createElkNodes(graph, elements.nodes, attachedBoundaryHosts.keys)
        val elkPorts = createBoundaryEventPorts(elkNodes, attachedBoundaryHosts)
        val elkConnectables = LinkedHashMap<String, ElkConnectableShape>().apply {
            elkNodes.forEach { (id, node) -> put(id, node) }
            elkPorts.forEach { (id, port) -> put(id, port) }
        }
        val elkEdges = createElkEdges(graph, elements.connections, elkConnectables)
        return ElkScopeGraph(
            graph = graph,
            nodes = elements.nodes,
            connections = elements.connections,
            elkNodes = elkNodes,
            elkPorts = elkPorts,
            attachedBoundaryHosts = attachedBoundaryHosts,
            elkEdges = elkEdges,
        )
    }

    private fun configureElkGraph(graph: ElkNode) {
        graph.setProperty(CoreOptions.ALGORITHM, LAYERED_ALGORITHM)
        graph.setProperty(CoreOptions.DIRECTION, direction.toElkDirection())
        graph.setProperty(CoreOptions.EDGE_ROUTING, EdgeRouting.ORTHOGONAL)
        graph.setProperty(CoreOptions.SPACING_NODE_NODE, nodeSpacing)
        graph.setProperty(CoreOptions.SPACING_EDGE_NODE, edgeSpacing)
        graph.setProperty(LayeredOptions.SPACING_NODE_NODE_BETWEEN_LAYERS, layerSpacing)
    }

    private fun findAttachedBoundaryHosts(
        elements: List<WithBpmnId>,
        elementsById: Map<String, WithBpmnId>,
    ): Map<String, String> = elements.mapNotNull { element ->
        val attachedTo = if (element.isBoundaryEvent()) element.attachedToRef() else null
        val hostId = attachedTo?.id?.takeIf { id ->
            id != element.id.id && elementsById[id]?.isBoundaryEvent() == false
        }
        hostId?.let { element.id.id to it }
    }.toMap()

    private fun createElkNodes(
        graph: ElkNode,
        elements: List<WithBpmnId>,
        attachedBoundaryIds: Set<String>,
    ): LinkedHashMap<String, ElkNode> = LinkedHashMap<String, ElkNode>().apply {
        elements.filterNot { it.id.id in attachedBoundaryIds }.forEach { element ->
            val size = defaultSize(element)
            val node = ElkGraphUtil.createNode(graph)
            node.identifier = element.id.id
            node.setDimensions(size.width, size.height)
            put(element.id.id, node)
        }
    }

    private fun createBoundaryEventPorts(
        elkNodes: Map<String, ElkNode>,
        attachedBoundaryHosts: Map<String, String>,
    ): LinkedHashMap<String, ElkPort> = LinkedHashMap<String, ElkPort>().apply {
        attachedBoundaryHosts.forEach { (boundaryId, hostId) ->
            val host = elkNodes.getValue(hostId)
            val port = ElkGraphUtil.createPort(host)
            port.identifier = boundaryId
            port.setDimensions(0.0, 0.0)
            port.setProperty(CoreOptions.PORT_SIDE, direction.toPortSide())
            host.setProperty(CoreOptions.PORT_CONSTRAINTS, PortConstraints.FIXED_SIDE)
            host.setProperty(CoreOptions.SPACING_PORT_PORT, EVENT_SIZE + BOUNDARY_EVENT_PORT_GAP)
            put(boundaryId, port)
        }
    }

    private fun createElkEdges(
        graph: ElkNode,
        connections: List<WithBpmnId>,
        connectables: Map<String, ElkConnectableShape>,
    ): LinkedHashMap<String, ElkEdge> = LinkedHashMap<String, ElkEdge>().apply {
        connections.forEach { connection ->
            val edge = ElkGraphUtil.createEdge(graph)
            edge.sources.add(connectables.getValue(connection.sourceRef!!))
            edge.targets.add(connectables.getValue(connection.targetRef!!))
            put(connection.id.id, edge)
        }
    }

    private fun createDiagramShapes(
        scopeId: BpmnElementId,
        ids: DiagramIdAllocator,
        elkGraph: ElkScopeGraph,
    ): GeneratedShapeLayout {
        val boundsByElementId = LinkedHashMap<String, BoundsElement>()
        val shapes = elkGraph.nodes.map { element ->
            val shapeBounds = shapeBoundsAfterElkLayout(element, elkGraph)
            boundsByElementId[element.id.id] = shapeBounds
            ShapeElement(
                id = DiagramElementId(ids.allocate("AUTO_SHAPE_${scopeId.id}_${element.id.id}")),
                bpmnElement = element.id,
                bounds = shapeBounds,
            )
        }
        return GeneratedShapeLayout(shapes, boundsByElementId)
    }

    private fun shapeBoundsAfterElkLayout(element: WithBpmnId, elkGraph: ElkScopeGraph): BoundsElement {
        val offset = diagramMargin
        val port = elkGraph.elkPorts[element.id.id]
        val host = elkGraph.attachedBoundaryHosts[element.id.id]?.let(elkGraph.elkNodes::getValue)
        if (port != null && host != null) {
            val size = defaultSize(element)
            val centerX = host.x + port.x + port.width / 2.0
            val centerY = host.y + port.y + port.height / 2.0
            return BoundsElement(
                (centerX - size.width / 2.0 + offset).toFloat(),
                (centerY - size.height / 2.0 + offset).toFloat(),
                size.width.toFloat(),
                size.height.toFloat(),
            )
        }

        val node = requireNotNull(elkGraph.elkNodes[element.id.id])
        return BoundsElement(
            (node.x + offset).toFloat(),
            (node.y + offset).toFloat(),
            node.width.toFloat(),
            node.height.toFloat(),
        )
    }

    private fun createDiagramEdges(
        scopeId: BpmnElementId,
        ids: DiagramIdAllocator,
        elkGraph: ElkScopeGraph,
        shapeBoundsById: Map<String, BoundsElement>,
    ): List<EdgeElement> = elkGraph.connections.map { connection ->
        val sourceId = connection.sourceRef!!
        val targetId = connection.targetRef!!
        val sourceNode = elkGraph.elkNodes[sourceId] ?: elkGraph.elkNodes.getValue(elkGraph.attachedBoundaryHosts.getValue(sourceId))
        val targetNode = elkGraph.elkNodes[targetId] ?: elkGraph.elkNodes.getValue(elkGraph.attachedBoundaryHosts.getValue(targetId))
        EdgeElement(
            id = DiagramElementId(ids.allocate("AUTO_EDGE_${scopeId.id}_${connection.id.id}")),
            bpmnElement = connection.id,
            waypoint = edgeWaypoints(
                elkGraph.elkEdges.getValue(connection.id.id),
                sourceNode,
                targetNode,
                diagramMargin,
                sourceBoundary = shapeBoundsById[sourceId].takeIf { sourceId in elkGraph.attachedBoundaryHosts },
                targetBoundary = shapeBoundsById[targetId].takeIf { targetId in elkGraph.attachedBoundaryHosts },
            ),
        )
    }

    private data class ScopedElements(
        val nodes: List<WithBpmnId>,
        val connections: List<WithBpmnId>,
    )

    private data class ElkScopeGraph(
        val graph: ElkNode,
        val nodes: List<WithBpmnId>,
        val connections: List<WithBpmnId>,
        val elkNodes: Map<String, ElkNode>,
        val elkPorts: Map<String, ElkPort>,
        val attachedBoundaryHosts: Map<String, String>,
        val elkEdges: Map<String, ElkEdge>,
    )

    private data class GeneratedShapeLayout(
        val shapes: List<ShapeElement>,
        val boundsByElementId: Map<String, BoundsElement>,
    )

    private fun edgeWaypoints(
        edge: ElkEdge,
        source: ElkNode,
        target: ElkNode,
        offset: Double,
        sourceBoundary: BoundsElement?,
        targetBoundary: BoundsElement?,
    ): List<WaypointElement> {
        val section = edge.sections.firstOrNull()
        val waypoints = if (section == null) {
            fallbackWaypoints(source, target, offset)
        } else {
            buildList {
                add(WaypointElement((section.startX + offset).toFloat(), (section.startY + offset).toFloat()))
                section.bendPoints.forEach { bend ->
                    add(WaypointElement((bend.x + offset).toFloat(), (bend.y + offset).toFloat()))
                }
                add(WaypointElement((section.endX + offset).toFloat(), (section.endY + offset).toFloat()))
            }
        }.distinct()

        return waypoints
            .trimBoundaryEndpoint(sourceBoundary, atStart = true)
            .trimBoundaryEndpoint(targetBoundary, atStart = false)
    }

    private fun List<WaypointElement>.trimBoundaryEndpoint(
        bounds: BoundsElement?,
        atStart: Boolean,
    ): List<WaypointElement> = reanchorEndpoint(bounds, atStart, circular = true)

    private fun List<WaypointElement>.reanchorEndpoint(
        bounds: BoundsElement?,
        atStart: Boolean,
        circular: Boolean,
    ): List<WaypointElement> {
        if (bounds == null || size < 2) return this

        val centerX = bounds.x + bounds.width / 2.0f
        val centerY = bounds.y + bounds.height / 2.0f
        val towards = (if (atStart) drop(1).firstOrNull { it.x != centerX || it.y != centerY }
        else dropLast(1).lastOrNull { it.x != centerX || it.y != centerY }) ?: return this
        val dx = towards.x - centerX
        val dy = towards.y - centerY
        val radiusX = bounds.width / 2.0
        val radiusY = bounds.height / 2.0
        if (radiusX <= 0.0 || radiusY <= 0.0) return this

        val scale = if (circular) {
            1.0 / kotlin.math.sqrt(dx * dx / (radiusX * radiusX) + dy * dy / (radiusY * radiusY))
        } else {
            minOf(
                if (dx == 0.0f) Double.POSITIVE_INFINITY else radiusX / kotlin.math.abs(dx),
                if (dy == 0.0f) Double.POSITIVE_INFINITY else radiusY / kotlin.math.abs(dy),
            )
        }
        val endpoint = WaypointElement(
            (centerX + dx * scale).toFloat(),
            (centerY + dy * scale).toFloat(),
        )
        return toMutableList().also { points ->
            points[if (atStart) 0 else lastIndex] = endpoint
        }
    }

    private fun fallbackWaypoints(source: ElkNode, target: ElkNode, offset: Double): List<WaypointElement> {
        val sourceCenterX = source.x + source.width / 2.0
        val sourceCenterY = source.y + source.height / 2.0
        val targetCenterX = target.x + target.width / 2.0
        val targetCenterY = target.y + target.height / 2.0
        val dx = targetCenterX - sourceCenterX
        val dy = targetCenterY - sourceCenterY

        if (dx == 0.0 && dy == 0.0) {
            return listOf(
                WaypointElement((sourceCenterX + offset).toFloat(), (source.y + offset).toFloat()),
                WaypointElement((sourceCenterX + offset).toFloat(), (source.y + offset - LOOP_EDGE_CLEARANCE).toFloat()),
                WaypointElement((targetCenterX + offset).toFloat(), (target.y + offset).toFloat()),
            )
        }

        val sourceScale = minOf(
            if (dx == 0.0) Double.POSITIVE_INFINITY else source.width / 2.0 / kotlin.math.abs(dx),
            if (dy == 0.0) Double.POSITIVE_INFINITY else source.height / 2.0 / kotlin.math.abs(dy),
        )
        val targetScale = minOf(
            if (dx == 0.0) Double.POSITIVE_INFINITY else target.width / 2.0 / kotlin.math.abs(dx),
            if (dy == 0.0) Double.POSITIVE_INFINITY else target.height / 2.0 / kotlin.math.abs(dy),
        )
        return listOf(
            WaypointElement((sourceCenterX + dx * sourceScale + offset).toFloat(), (sourceCenterY + dy * sourceScale + offset).toFloat()),
            WaypointElement((targetCenterX - dx * targetScale + offset).toFloat(), (targetCenterY - dy * targetScale + offset).toFloat()),
        )
    }

    private fun defaultSize(element: WithBpmnId): NodeSize = when (element) {
        is BpmnGatewayAlike -> NodeSize(GATEWAY_SIZE, GATEWAY_SIZE)
        is BpmnStartEventAlike,
        is EndEventAlike,
        is IntermediateCatchingEventAlike,
        is IntermediateThrowingEventAlike,
        is BpmnBoundaryEvent,
        is BpmnBoundaryEventAlike -> NodeSize(EVENT_SIZE, EVENT_SIZE)
        is BpmnStructuralElementAlike -> NodeSize(SUBPROCESS_WIDTH, SUBPROCESS_HEIGHT)
        is BpmnTaskAlike -> NodeSize(TASK_WIDTH, TASK_HEIGHT)
        else -> NodeSize(ANNOTATION_WIDTH, ANNOTATION_HEIGHT)
    }

    private data class NodeSize(val width: Double, val height: Double)

    private fun BpmnLayoutDirection.toElkDirection(): ElkDirection = when (this) {
        BpmnLayoutDirection.RIGHT -> ElkDirection.RIGHT
        BpmnLayoutDirection.LEFT -> ElkDirection.LEFT
        BpmnLayoutDirection.DOWN -> ElkDirection.DOWN
        BpmnLayoutDirection.UP -> ElkDirection.UP
    }

    private fun BpmnLayoutDirection.toPortSide(): PortSide = when (this) {
        BpmnLayoutDirection.RIGHT -> PortSide.EAST
        BpmnLayoutDirection.LEFT -> PortSide.WEST
        BpmnLayoutDirection.DOWN -> PortSide.SOUTH
        BpmnLayoutDirection.UP -> PortSide.NORTH
    }

    private fun registerElkLayeredAlgorithm() {
        ElkMetadataRegistration.ensureRegistered()
    }

    private object ElkMetadataRegistration {
        @Volatile
        private var registered = false

        @Synchronized
        fun ensureRegistered() {
            if (!registered) {
                LayoutMetaDataService.getInstance().registerLayoutMetaDataProviders(
                    CoreOptions(),
                    LayeredMetaDataProvider(),
                )
                registered = true
            }
        }
    }

    private class DiagramIdAllocator(
        processObject: BpmnProcessObject,
        scopes: List<Pair<BpmnElementId, BpmnProcessBody>>,
    ) {
        private val used = buildSet {
            add(processObject.process.id.id)
            scopes.forEach { (scopeId, body) ->
                add(scopeId.id)
                body.flowNodes().forEach { add(it.id.id) }
                body.connections().forEach { add(it.id.id) }
            }
            processObject.diagram.forEach { diagram ->
                add(diagram.id.id)
                add(diagram.bpmnPlane.id.id)
                diagram.bpmnPlane.bpmnShape.orEmpty().forEach { add(it.id.id) }
                diagram.bpmnPlane.bpmnEdge.orEmpty().forEach { add(it.id.id) }
            }
        }.toMutableSet()

        fun allocate(candidate: String): String {
            val base = candidate.replace(INVALID_XML_ID_CHARS, "_").let {
                if (it.firstOrNull()?.let(::isValidXmlIdStart) == true) it else "AUTO_$it"
            }
            var id = base
            var suffix = 1
            while (!used.add(id)) {
                id = "${base}_$suffix"
                suffix++
            }
            return id
        }

        private fun isValidXmlIdStart(char: Char): Boolean = char.isLetter() || char == '_'
    }

    private companion object {
        const val LAYERED_ALGORITHM = "org.eclipse.elk.layered"
        const val DEFAULT_NODE_SPACING = 80.0
        const val DEFAULT_EDGE_SPACING = 30.0
        const val DEFAULT_LAYER_SPACING = 100.0
        const val DEFAULT_DIAGRAM_MARGIN = 40.0
        const val EVENT_SIZE = 36.0
        const val BOUNDARY_EVENT_PORT_GAP = 20.0
        const val FORCE_LAYOUT_ITERATIONS = 300
        const val FORCE_SPRING_STRENGTH = 0.08
        const val FORCE_ANCHOR_STRENGTH = 0.015
        const val FORCE_REPULSION_STRENGTH = 0.2
        const val FORCE_MAX_STEP = 12.0
        const val LOOP_EDGE_CLEARANCE = 30.0
        const val GATEWAY_SIZE = 50.0
        const val TASK_WIDTH = 120.0
        const val TASK_HEIGHT = 80.0
        const val SUBPROCESS_WIDTH = 160.0
        const val SUBPROCESS_HEIGHT = 100.0
        const val ANNOTATION_WIDTH = 120.0
        const val ANNOTATION_HEIGHT = 60.0
        val INVALID_XML_ID_CHARS = Regex("[^A-Za-z0-9_.-]")
    }
}

private fun WithBpmnId.attachedToRef(): BpmnElementId? = when (this) {
    is BpmnBoundaryEvent -> attachedToRef
    is BpmnBoundaryCancelEvent -> attachedToRef
    is BpmnBoundaryCompensationEvent -> attachedToRef
    is BpmnBoundaryConditionalEvent -> attachedToRef
    is BpmnBoundaryErrorEvent -> attachedToRef
    is BpmnBoundaryEscalationEvent -> attachedToRef
    is BpmnBoundaryMessageEvent -> attachedToRef
    is BpmnBoundarySignalEvent -> attachedToRef
    is BpmnBoundaryTimerEvent -> attachedToRef
    else -> null
}

private fun WithBpmnId.isBoundaryEvent(): Boolean = this is BpmnBoundaryEvent || this is BpmnBoundaryEventAlike

private fun BpmnProcessBody.flowNodes(): List<WithBpmnId> {
    val nodes = mutableListOf<WithBpmnId>()

    fun <T : WithBpmnId> add(elements: List<T>?) {
        elements?.let { nodes.addAll(it) }
    }

    add(startEvent)
    add(timerStartEvent)
    add(signalStartEvent)
    add(messageStartEvent)
    add(errorStartEvent)
    add(escalationStartEvent)
    add(conditionalStartEvent)
    add(endEvent)
    add(errorEndEvent)
    add(escalationEndEvent)
    add(cancelEndEvent)
    add(terminateEndEvent)
    add(boundaryEvent)
    add(boundaryCancelEvent)
    add(boundaryCompensationEvent)
    add(boundaryConditionalEvent)
    add(boundaryErrorEvent)
    add(boundaryEscalationEvent)
    add(boundaryMessageEvent)
    add(boundarySignalEvent)
    add(boundaryTimerEvent)
    add(intermediateCatchEvent)
    add(intermediateTimerCatchingEvent)
    add(intermediateMessageCatchingEvent)
    add(intermediateSignalCatchingEvent)
    add(intermediateConditionalCatchingEvent)
    add(intermediateLinkCatchingEvent)
    add(intermediateThrowEvent)
    add(intermediateNoneThrowingEvent)
    add(intermediateSignalThrowingEvent)
    add(intermediateEscalationThrowingEvent)
    add(intermediateLinkThrowingEvent)
    add(task)
    add(userTask)
    add(scriptTask)
    add(serviceTask)
    add(businessRuleTask)
    add(manualTask)
    add(sendTask)
    add(receiveTask)
    add(camelTask)
    add(httpTask)
    add(externalTask)
    add(mailTask)
    add(muleTask)
    add(decisionTask)
    add(shellTask)
    add(sendEventTask)
    add(callActivity)
    add(subProcess)
    add(eventSubProcess)
    add(transaction)
    add(adHocSubProcess)
    add(collapsedSubProcess)
    add(collapsedTransaction)
    add(exclusiveGateway)
    add(parallelGateway)
    add(inclusiveGateway)
    add(eventBasedGateway)
    add(complexGateway)
    add(textAnnotation)

    return nodes
}

private fun BpmnProcessBody.connections(): List<WithBpmnId> = buildList {
    addAll(sequenceFlow.orEmpty())
    addAll(association.orEmpty())
}

private val WithBpmnId.sourceRef: String?
    get() = when (this) {
        is BpmnSequenceFlow -> sourceRef
        is BpmnAssociation -> sourceRef
        else -> null
    }

private val WithBpmnId.targetRef: String?
    get() = when (this) {
        is BpmnSequenceFlow -> targetRef
        is BpmnAssociation -> targetRef
        else -> null
    }
