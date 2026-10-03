package com.valb3r.bpmn.intellij.plugin.autolayout

import com.valb3r.bpmn.intellij.plugin.bpmn.api.BpmnProcessObject
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.BpmnElementId
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.BpmnProcess
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.BpmnProcessBody
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.BpmnSequenceFlow
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.activities.BpmnCallActivity
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.events.boundary.BpmnBoundaryTimerEvent
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.events.begin.BpmnStartEvent
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.events.end.BpmnEndEvent
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.gateways.BpmnParallelGateway
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.tasks.BpmnServiceTask
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.tasks.BpmnTask
import com.valb3r.bpmn.intellij.plugin.bpmn.api.diagram.DiagramElementId
import com.valb3r.bpmn.intellij.plugin.bpmn.api.diagram.elements.WaypointElement
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeFalse
import org.amshove.kluent.shouldBeTrue
import org.junit.jupiter.api.Test
import kotlin.math.abs
import kotlin.math.hypot

class BpmnAutoLayoutTest {

    @Test
    fun `generates BPMN shapes and routed waypoints for shared process elements`() {
        val startEvent = BpmnStartEvent(BpmnElementId("start"))
        val task = BpmnTask(BpmnElementId("task"))
        val sequenceFlow = BpmnSequenceFlow(
            id = BpmnElementId("flow"),
            sourceRef = startEvent.id.id,
            targetRef = task.id.id,
        )
        val input = BpmnProcessObject(
            process = BpmnProcess(
                id = BpmnElementId("process"),
                name = null,
                documentation = null,
                isExecutable = null,
                body = bodyWith(startEvent, task, sequenceFlow),
                children = null,
            ),
            diagram = emptyList(),
        )

        val result = BpmnAutoLayout().layout(input)

        (input !== result).shouldBeTrue()
        input.diagram.isEmpty().shouldBeTrue()
        result.diagram.size.shouldBeEqualTo(1)

        val plane = result.diagram.single().bpmnPlane
        plane.bpmnElement.shouldBeEqualTo(BpmnElementId("process"))

        val shapesByElementId = plane.bpmnShape.orEmpty().associateBy { it.bpmnElement }
        shapesByElementId.keys.shouldBeEqualTo(setOf(startEvent.id, task.id))
        shapesByElementId.values.forEach { shape ->
            val bounds = shape.rectBounds()
            (bounds.x >= 0.0f && bounds.y >= 0.0f).shouldBeTrue()
            (bounds.width > 0.0f && bounds.height > 0.0f).shouldBeTrue()
        }
        val startBounds = shapesByElementId.getValue(startEvent.id).rectBounds()
        val taskBounds = shapesByElementId.getValue(task.id).rectBounds()
        (taskBounds.x - (startBounds.x + startBounds.width) >= 99.0f).shouldBeTrue()

        val edge = plane.bpmnEdge.orEmpty().single()
        edge.bpmnElement.shouldBeEqualTo(sequenceFlow.id)
        val waypoints = edge.waypoint.orEmpty()
        (waypoints.size >= 2).shouldBeTrue()
        waypoints.any { it.x.isNaN() || it.y.isNaN() || it.x.isInfinite() || it.y.isInfinite() }.shouldBeFalse()
    }

    @Test
    fun `layoutIfRequired returns the laid out process and reports whether layout was applied`() {
        val startEvent = BpmnStartEvent(BpmnElementId("start"))
        val task = BpmnTask(BpmnElementId("task"))
        val flow = BpmnSequenceFlow(BpmnElementId("flow"), sourceRef = "start", targetRef = "task")
        val process = BpmnProcessObject(
            process = BpmnProcess(
                id = BpmnElementId("process"),
                name = null,
                documentation = null,
                isExecutable = null,
                body = bodyWith(startEvent, task, flow),
                children = null,
            ),
            diagram = emptyList(),
        )
        val autoLayout = BpmnAutoLayout()

        val initialLayout = autoLayout.layoutIfRequired(process)
        initialLayout.layoutApplied.shouldBeTrue()
        initialLayout.laidOutProcess.diagram.size.shouldBeEqualTo(1)

        val completeLayout = autoLayout.layoutIfRequired(initialLayout.laidOutProcess)
        completeLayout.layoutApplied.shouldBeFalse()
        completeLayout.laidOutProcess.shouldBeEqualTo(initialLayout.laidOutProcess)

        val originalDiagram = initialLayout.laidOutProcess.diagram.single()
        val movedStart = originalDiagram.bpmnPlane.bpmnShape.orEmpty()
            .single { it.bpmnElement == startEvent.id }
            .copyAndTranslate(500.0f, 300.0f)
        val movedDiagram = originalDiagram.copy(
            bpmnPlane = originalDiagram.bpmnPlane.copy(bpmnShape = listOf(movedStart)),
        )
        val forcedLayout = autoLayout.layoutIfRequired(
            process.copy(diagram = listOf(movedDiagram)),
            forceFullLayout = true,
        )

        forcedLayout.layoutApplied.shouldBeTrue()
        val forcedStart = forcedLayout.laidOutProcess.diagram.single().bpmnPlane.bpmnShape.orEmpty()
            .single { it.bpmnElement == startEvent.id }
        val movedBounds = movedStart.rectBounds()
        val forcedBounds = forcedStart.rectBounds()
        (forcedBounds.x != movedBounds.x || forcedBounds.y != movedBounds.y).shouldBeTrue()
    }

    @Test
    fun `keeps parallel flow nodes visibly separated`() {
        val startEvent = BpmnStartEvent(BpmnElementId("start"))
        val split = BpmnParallelGateway(BpmnElementId("split"))
        val taskA = BpmnTask(BpmnElementId("task-a"))
        val taskB = BpmnTask(BpmnElementId("task-b"))
        val endEvent = BpmnEndEvent(BpmnElementId("end"))
        val flows = listOf(
            BpmnSequenceFlow(BpmnElementId("start-to-split"), sourceRef = "start", targetRef = "split"),
            BpmnSequenceFlow(BpmnElementId("split-to-task-a"), sourceRef = "split", targetRef = "task-a"),
            BpmnSequenceFlow(BpmnElementId("split-to-task-b"), sourceRef = "split", targetRef = "task-b"),
            BpmnSequenceFlow(BpmnElementId("task-a-to-end"), sourceRef = "task-a", targetRef = "end"),
            BpmnSequenceFlow(BpmnElementId("task-b-to-end"), sourceRef = "task-b", targetRef = "end"),
        )
        val input = BpmnProcessObject(
            process = BpmnProcess(
                id = BpmnElementId("process"),
                name = null,
                documentation = null,
                isExecutable = null,
                body = bodyWith(
                    startEvent,
                    taskA,
                    flows.first(),
                    tasks = listOf(taskA, taskB),
                    endEvents = listOf(endEvent),
                    parallelGateways = listOf(split),
                    sequenceFlows = flows,
                ),
                children = null,
            ),
            diagram = emptyList(),
        )

        val plane = BpmnAutoLayout().layout(input).diagram.single().bpmnPlane
        val taskBounds = listOf("task-a", "task-b")
            .map { id -> plane.bpmnShape.orEmpty().single { it.bpmnElement.id == id }.rectBounds() }
            .sortedBy { it.y }

        (taskBounds[1].y - (taskBounds[0].y + taskBounds[0].height) >= 79.0f).shouldBeTrue()
    }

    @Test
    fun `docks attached boundary events to the host activity`() {
        val startEvent = BpmnStartEvent(BpmnElementId("start"))
        val task = BpmnTask(BpmnElementId("task"))
        val boundaryEvent = BpmnBoundaryTimerEvent(
            id = BpmnElementId("boundary-timer"),
            attachedToRef = task.id,
        )
        val endEvent = BpmnEndEvent(BpmnElementId("end"))
        val flows = listOf(
            BpmnSequenceFlow(BpmnElementId("start-to-task"), sourceRef = "start", targetRef = "task"),
            BpmnSequenceFlow(BpmnElementId("task-to-end"), sourceRef = "task", targetRef = "end"),
            BpmnSequenceFlow(BpmnElementId("boundary-to-end"), sourceRef = "boundary-timer", targetRef = "end"),
        )
        val input = BpmnProcessObject(
            process = BpmnProcess(
                id = BpmnElementId("process"),
                name = null,
                documentation = null,
                isExecutable = null,
                body = bodyWith(
                    startEvent,
                    task,
                    flows.first(),
                    endEvents = listOf(endEvent),
                    sequenceFlows = flows,
                    boundaryTimerEvents = listOf(boundaryEvent),
                ),
                children = null,
            ),
            diagram = emptyList(),
        )

        val plane = BpmnAutoLayout().layout(input).diagram.single().bpmnPlane
        val shapesByElementId = plane.bpmnShape.orEmpty().associateBy { it.bpmnElement }
        val hostBounds = shapesByElementId.getValue(task.id).rectBounds()
        val boundaryBounds = shapesByElementId.getValue(boundaryEvent.id).rectBounds()

        (boundaryBounds.x + boundaryBounds.width / 2.0f)
            .shouldBeWithin(hostBounds.x + hostBounds.width, 0.01f)
        (boundaryBounds.y < hostBounds.y + hostBounds.height &&
            boundaryBounds.y + boundaryBounds.height > hostBounds.y).shouldBeTrue()
        val boundaryFlow = plane.bpmnEdge.orEmpty().single { it.bpmnElement == flows.last().id }
        val flowStart = boundaryFlow.waypoint.orEmpty().first()
        hypot(
                (flowStart.x - (boundaryBounds.x + boundaryBounds.width / 2.0f)).toDouble(),
                (flowStart.y - (boundaryBounds.y + boundaryBounds.height / 2.0f)).toDouble(),
            ).shouldBeWithin(boundaryBounds.width / 2.0, 0.01)
    }

    @Test
    fun `allocates missing shape and reattaches its existing edge`() {
        val startEvent = BpmnStartEvent(BpmnElementId("start"))
        val task = BpmnTask(BpmnElementId("task"))
        val sequenceFlow = BpmnSequenceFlow(
            id = BpmnElementId("flow"),
            sourceRef = startEvent.id.id,
            targetRef = task.id.id,
        )
        val process = BpmnProcessObject(
            process = BpmnProcess(
                id = BpmnElementId("process"),
                name = null,
                documentation = null,
                isExecutable = null,
                body = bodyWith(startEvent, task, sequenceFlow),
                children = null,
            ),
            diagram = emptyList(),
        )
        val completeLayout = BpmnAutoLayout().layout(process)
        val generatedDiagram = completeLayout.diagram.single()
        val generatedPlane = generatedDiagram.bpmnPlane
        val movedStart = generatedPlane.bpmnShape.orEmpty()
            .single { it.bpmnElement == startEvent.id }
            .copyAndTranslate(300.0f, 120.0f)
        val staleEdge = generatedPlane.bpmnEdge.orEmpty().single { it.bpmnElement == sequenceFlow.id }
        val existingDiagram = generatedDiagram.copy(
            id = DiagramElementId("existing-diagram"),
            bpmnPlane = generatedPlane.copy(
                id = DiagramElementId("existing-plane"),
                bpmnShape = listOf(movedStart),
                bpmnEdge = listOf(staleEdge),
            ),
        )

        val updated = BpmnAutoLayout().layout(process.copy(diagram = listOf(existingDiagram)))
        val updatedDiagram = updated.diagram.single()
        val updatedPlane = updatedDiagram.bpmnPlane
        val shapesByElement = updatedPlane.bpmnShape.orEmpty().associateBy { it.bpmnElement }
        val preservedBounds = shapesByElement.getValue(startEvent.id).rectBounds()
        val allocatedTaskBounds = shapesByElement.getValue(task.id).rectBounds()

        updatedDiagram.id.id.shouldBeEqualTo("existing-diagram")
        updatedPlane.id.id.shouldBeEqualTo("existing-plane")
        preservedBounds.x.shouldBeWithin(movedStart.rectBounds().x, 0.01f)
        preservedBounds.y.shouldBeWithin(movedStart.rectBounds().y, 0.01f)
        (allocatedTaskBounds.x > preservedBounds.x + preservedBounds.width).shouldBeTrue()
        val reattachedEdge = updatedPlane.bpmnEdge.orEmpty().single()
        reattachedEdge.bpmnElement.shouldBeEqualTo(sequenceFlow.id)
        val waypoints = reattachedEdge.waypoint.orEmpty()
        waypoints.first().x.shouldBeWithin(preservedBounds.maxX.toFloat(), 0.01f)
        waypoints.first().y.shouldBeWithin(preservedBounds.centerY.toFloat(), 0.01f)
        waypoints.last().x.shouldBeWithin(allocatedTaskBounds.x, 0.01f)
        waypoints.last().y.shouldBeWithin(allocatedTaskBounds.centerY.toFloat(), 0.01f)
        BpmnAutoLayout().layout(updated).diagram.shouldBeEqualTo(updated.diagram)
    }

    @Test
    fun `places added service task and call activity from sequence endpoints`() {
        val startEvent = BpmnStartEvent(BpmnElementId("start"))
        val serviceTask = BpmnServiceTask(BpmnElementId("service"))
        val callActivity = BpmnCallActivity(BpmnElementId("call"))
        val endEvent = BpmnEndEvent(BpmnElementId("end"))
        val flows = listOf(
            BpmnSequenceFlow(BpmnElementId("start-to-service"), sourceRef = "start", targetRef = "service"),
            BpmnSequenceFlow(BpmnElementId("service-to-call"), sourceRef = "service", targetRef = "call"),
            BpmnSequenceFlow(BpmnElementId("call-to-end"), sourceRef = "call", targetRef = "end"),
        )
        val process = BpmnProcessObject(
            process = BpmnProcess(
                id = BpmnElementId("process"),
                name = null,
                documentation = null,
                isExecutable = null,
                body = bodyWith(
                    startEvent,
                    task = null,
                    sequenceFlow = flows.first(),
                    tasks = emptyList(),
                    endEvents = listOf(endEvent),
                    serviceTasks = listOf(serviceTask),
                    callActivities = listOf(callActivity),
                    sequenceFlows = flows,
                ),
                children = null,
            ),
            diagram = emptyList(),
        )
        val complete = BpmnAutoLayout().layout(process).diagram.single()
        val originalPlane = complete.bpmnPlane
        val startShape = originalPlane.bpmnShape.orEmpty().single { it.bpmnElement == startEvent.id }
            .copyAndTranslate(0.0f, 200.0f)
        val endShape = originalPlane.bpmnShape.orEmpty().single { it.bpmnElement == endEvent.id }
        val partialDiagram = complete.copy(
            bpmnPlane = originalPlane.copy(
                bpmnShape = listOf(startShape, endShape),
                bpmnEdge = emptyList(),
            ),
        )

        val updatedPlane = BpmnAutoLayout().layout(process.copy(diagram = listOf(partialDiagram))).diagram.single().bpmnPlane
        val shapes = updatedPlane.bpmnShape.orEmpty().associateBy { it.bpmnElement }
        val startBounds = shapes.getValue(startEvent.id).rectBounds()
        val serviceBounds = shapes.getValue(serviceTask.id).rectBounds()
        val callBounds = shapes.getValue(callActivity.id).rectBounds()
        val endBounds = shapes.getValue(endEvent.id).rectBounds()

        (serviceBounds.x > startBounds.x + startBounds.width).shouldBeTrue()
        (callBounds.x > serviceBounds.x + serviceBounds.width).shouldBeTrue()
        (endBounds.x > callBounds.x + callBounds.width).shouldBeTrue()
        (serviceBounds.x - startBounds.maxX >= 99.0).shouldBeTrue()
        (callBounds.x - serviceBounds.maxX >= 99.0).shouldBeTrue()
        (endBounds.x - callBounds.maxX >= 99.0).shouldBeTrue()
        startBounds.x.shouldBeWithin(startShape.rectBounds().x, 0.01f)
        startBounds.y.shouldBeWithin(startShape.rectBounds().y, 0.01f)
        endBounds.x.shouldBeWithin(endShape.rectBounds().x, 0.01f)
        endBounds.y.shouldBeWithin(endShape.rectBounds().y, 0.01f)

        val edges = updatedPlane.bpmnEdge.orEmpty().associateBy { it.bpmnElement }
        listOf(flows[1], flows[2]).forEach { flow ->
            val waypoints = edges.getValue(flow.id).waypoint.orEmpty()
            (waypoints.size >= 4).shouldBeTrue()
            waypoints.zipWithNext().forEach { (from, to) ->
                (from.x == to.x || from.y == to.y).shouldBeTrue()
            }
        }
    }

    @Test
    fun `deduces missing shape position from sequence endpoints and keeps it fixed`() {
        val startEvent = BpmnStartEvent(BpmnElementId("start"))
        val gateway = BpmnParallelGateway(BpmnElementId("gateway"))
        val task = BpmnTask(BpmnElementId("task"))
        val flows = listOf(
            BpmnSequenceFlow(BpmnElementId("start-to-task"), sourceRef = "start", targetRef = "task"),
            BpmnSequenceFlow(BpmnElementId("gateway-to-task"), sourceRef = "gateway", targetRef = "task"),
        )
        val process = BpmnProcessObject(
            process = BpmnProcess(
                id = BpmnElementId("process"),
                name = null,
                documentation = null,
                isExecutable = null,
                body = bodyWith(
                    startEvent,
                    task,
                    flows.first(),
                    parallelGateways = listOf(gateway),
                    sequenceFlows = flows,
                ),
                children = null,
            ),
            diagram = emptyList(),
        )
        val complete = BpmnAutoLayout().layout(process).diagram.single()
        val completePlane = complete.bpmnPlane
        val allocatedShapes = completePlane.bpmnShape.orEmpty()
            .filter { it.bpmnElement == startEvent.id || it.bpmnElement == gateway.id }
        val attachmentRoutes = mapOf(
            flows[0].id to listOf(WaypointElement(200.0f, 0.0f), WaypointElement(200.0f, 140.0f)),
            flows[1].id to listOf(WaypointElement(500.0f, 280.0f), WaypointElement(320.0f, 280.0f)),
        )
        val allocatedEdges = completePlane.bpmnEdge.orEmpty()
            .filter { edge -> edge.bpmnElement?.let { it in attachmentRoutes } == true }
            .map { edge ->
                edge.copy(waypoint = attachmentRoutes.getValue(edge.bpmnElement!!))
            }
        val partialDiagram = complete.copy(
            bpmnPlane = completePlane.copy(
                bpmnShape = allocatedShapes,
                bpmnEdge = allocatedEdges,
            ),
        )

        val updatedPlane = BpmnAutoLayout()
            .layout(process.copy(diagram = listOf(partialDiagram)))
            .diagram.single().bpmnPlane
        val updatedShapes = updatedPlane.bpmnShape.orEmpty().associateBy { it.bpmnElement }
        val taskBounds = updatedShapes.getValue(task.id).rectBounds()
        allocatedShapes.forEach { fixedShape ->
            val originalBounds = fixedShape.rectBounds()
            val updatedBounds = updatedShapes.getValue(fixedShape.bpmnElement).rectBounds()
            updatedBounds.x.shouldBeWithin(originalBounds.x, 0.01f)
            updatedBounds.y.shouldBeWithin(originalBounds.y, 0.01f)
        }
        taskBounds.centerX.toFloat().shouldBeWithin(230.0f, 0.01f)
        taskBounds.centerY.toFloat().shouldBeWithin(230.0f, 0.01f)
    }

    @Test
    fun `uses configured direction and ELK spacing when filling an existing diagram`() {
        val startEvent = BpmnStartEvent(BpmnElementId("start"))
        val task = BpmnTask(BpmnElementId("task"))
        val flow = BpmnSequenceFlow(BpmnElementId("flow"), sourceRef = "start", targetRef = "task")
        val process = BpmnProcessObject(
            process = BpmnProcess(
                id = BpmnElementId("process"),
                name = null,
                documentation = null,
                isExecutable = null,
                body = bodyWith(startEvent, task, flow),
                children = null,
            ),
            diagram = emptyList(),
        )
        val autoLayout = BpmnAutoLayout(
            direction = BpmnLayoutDirection.DOWN,
            nodeSpacing = 25.0,
            edgeSpacing = 18.0,
            layerSpacing = 240.0,
        )
        val generatedPlane = autoLayout.layout(process).diagram.single().bpmnPlane
        val generatedShapes = generatedPlane.bpmnShape.orEmpty().associateBy { it.bpmnElement }
        val originalStart = generatedShapes.getValue(startEvent.id)
        val movedStart = originalStart.copyAndTranslate(175.0f, 30.0f)
        val partialDiagram = autoLayout.layout(process).diagram.single().copy(
            bpmnPlane = generatedPlane.copy(
                bpmnShape = listOf(movedStart),
                bpmnEdge = emptyList(),
            ),
        )

        val updatedPlane = autoLayout.layout(process.copy(diagram = listOf(partialDiagram))).diagram.single().bpmnPlane
        val updatedShapes = updatedPlane.bpmnShape.orEmpty().associateBy { it.bpmnElement }
        val generatedStartBounds = originalStart.rectBounds()
        val movedStartBounds = updatedShapes.getValue(startEvent.id).rectBounds()
        val generatedTaskBounds = generatedShapes.getValue(task.id).rectBounds()
        val updatedTaskBounds = updatedShapes.getValue(task.id).rectBounds()
        val generatedLayerGap = generatedTaskBounds.y - generatedStartBounds.maxY
        val actualLayerGap = updatedTaskBounds.y - movedStartBounds.maxY

        (updatedTaskBounds.y > movedStartBounds.y).shouldBeTrue()
        actualLayerGap.shouldBeWithin(generatedLayerGap, 0.01f)
        (actualLayerGap >= 239.0f).shouldBeTrue()
    }

    private fun bodyWith(
        startEvent: BpmnStartEvent,
        task: BpmnTask?,
        sequenceFlow: BpmnSequenceFlow,
        tasks: List<BpmnTask> = listOfNotNull(task),
        serviceTasks: List<BpmnServiceTask>? = null,
        callActivities: List<BpmnCallActivity>? = null,
        endEvents: List<BpmnEndEvent>? = null,
        parallelGateways: List<BpmnParallelGateway>? = null,
        boundaryTimerEvents: List<BpmnBoundaryTimerEvent>? = null,
        sequenceFlows: List<BpmnSequenceFlow> = listOf(sequenceFlow),
    ) = BpmnProcessBody(
        startEvent = listOf(startEvent),
        timerStartEvent = null,
        signalStartEvent = null,
        messageStartEvent = null,
        errorStartEvent = null,
        escalationStartEvent = null,
        conditionalStartEvent = null,
        endEvent = endEvents,
        errorEndEvent = null,
        escalationEndEvent = null,
        cancelEndEvent = null,
        terminateEndEvent = null,
        boundaryEvent = null,
        boundaryCancelEvent = null,
        boundaryCompensationEvent = null,
        boundaryConditionalEvent = null,
        boundaryErrorEvent = null,
        boundaryEscalationEvent = null,
        boundaryMessageEvent = null,
        boundarySignalEvent = null,
        boundaryTimerEvent = boundaryTimerEvents,
        intermediateCatchEvent = null,
        intermediateTimerCatchingEvent = null,
        intermediateMessageCatchingEvent = null,
        intermediateSignalCatchingEvent = null,
        intermediateConditionalCatchingEvent = null,
        intermediateLinkCatchingEvent = null,
        intermediateThrowEvent = null,
        intermediateNoneThrowingEvent = null,
        intermediateSignalThrowingEvent = null,
        intermediateEscalationThrowingEvent = null,
        intermediateLinkThrowingEvent = null,
        task = tasks,
        userTask = null,
        scriptTask = null,
        serviceTask = serviceTasks,
        businessRuleTask = null,
        manualTask = null,
        sendTask = null,
        receiveTask = null,
        camelTask = null,
        httpTask = null,
        externalTask = null,
        mailTask = null,
        muleTask = null,
        decisionTask = null,
        shellTask = null,
        sendEventTask = null,
        callActivity = callActivities,
        subProcess = null,
        eventSubProcess = null,
        transaction = null,
        adHocSubProcess = null,
        collapsedSubProcess = null,
        collapsedTransaction = null,
        exclusiveGateway = null,
        parallelGateway = parallelGateways,
        inclusiveGateway = null,
        eventBasedGateway = null,
        complexGateway = null,
        sequenceFlow = sequenceFlows,
    )
}

private fun Float.shouldBeWithin(expected: Float, tolerance: Float) {
    (abs(this - expected) <= tolerance).shouldBeTrue()
}

private fun Double.shouldBeWithin(expected: Double, tolerance: Double) {
    (abs(this - expected) <= tolerance).shouldBeTrue()
}
