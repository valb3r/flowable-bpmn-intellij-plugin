package com.valb3r.bpmn.intellij.plugin.activiti.parser

import com.valb3r.bpmn.intellij.plugin.activiti.parser.testevents.BpmnShapeObjectAddedEvent
import com.valb3r.bpmn.intellij.plugin.bpmn.api.BpmnFileObject
import com.valb3r.bpmn.intellij.plugin.bpmn.api.PropertyTable
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.BpmnElementId
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
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.subprocess.BpmnEventSubprocess
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.subprocess.BpmnSubProcess
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.subprocess.BpmnTransactionalSubProcess
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.tasks.*
import com.valb3r.bpmn.intellij.plugin.bpmn.api.diagram.DiagramElementId
import com.valb3r.bpmn.intellij.plugin.bpmn.api.diagram.elements.BoundsElement
import com.valb3r.bpmn.intellij.plugin.bpmn.api.diagram.elements.ShapeElement
import com.valb3r.bpmn.intellij.plugin.bpmn.api.events.EventPropagatableToXml
import com.valb3r.bpmn.intellij.plugin.bpmn.api.info.Property
import com.valb3r.bpmn.intellij.plugin.bpmn.api.info.PropertyType
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldHaveSingleItem
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.Test
import java.util.*
import kotlin.reflect.KClass
import kotlin.reflect.full.primaryConstructor

internal class XmlUpdateEventBpmnObjectAddedTest {

    private val processId = BpmnElementId("simple-nested")
    private val parser = ActivitiParser()

    val id = BpmnElementId(UUID.randomUUID().toString())
    val diagramId = DiagramElementId(UUID.randomUUID().toString())
    val nameOnProp = "A prop name"

    @Test
    fun `Added start event works`() {
        val updatedProcess = readAndUpdateProcess(generateUpdateEvent(BpmnStartEvent::class))

        updatedProcess.processes[0].body!!.startEvent!!.filter { it.id == id }.shouldHaveSingleItem().name.shouldBeEqualTo(nameOnProp)
    }

    @Test
    fun `Added start timer event works`() {
        val updatedProcess = readAndUpdateProcess(generateUpdateEvent(BpmnStartTimerEvent::class))

        updatedProcess.processes[0].body!!.timerStartEvent!!.filter { it.id == id }.shouldHaveSingleItem().name.shouldBeEqualTo(nameOnProp)
    }

    @Test
    fun `Added start signal event works`() {
        val updatedProcess = readAndUpdateProcess(generateUpdateEvent(BpmnStartSignalEvent::class))

        updatedProcess.processes[0].body!!.signalStartEvent!!.filter { it.id == id }.shouldHaveSingleItem().name.shouldBeEqualTo(nameOnProp)
    }

    @Test
    fun `Added start message event works`() {
        val updatedProcess = readAndUpdateProcess(generateUpdateEvent(BpmnStartMessageEvent::class))

        updatedProcess.processes[0].body!!.messageStartEvent!!.filter { it.id == id }.shouldHaveSingleItem().name.shouldBeEqualTo(nameOnProp)
    }

    @Test
    fun `Added start error event works`() {
        val updatedProcess = readAndUpdateProcess(generateUpdateEvent(BpmnStartErrorEvent::class))

        updatedProcess.processes[0].body!!.errorStartEvent!!.filter { it.id == id }.shouldHaveSingleItem().name.shouldBeEqualTo(nameOnProp)
    }

    @Test
    fun `Added start conditional event works`() {
        val updatedProcess = readAndUpdateProcess(generateUpdateEvent(BpmnStartConditionalEvent::class))

        updatedProcess.processes[0].body!!.conditionalStartEvent!!.filter { it.id == id }.shouldHaveSingleItem().name.shouldBeEqualTo(nameOnProp)
    }

    @Test
    fun `Added start escalation event works`() {
        val updatedProcess = readAndUpdateProcess(generateUpdateEvent(BpmnStartEscalationEvent::class))

        updatedProcess.processes[0].body!!.escalationStartEvent!!.filter { it.id == id }.shouldHaveSingleItem().name.shouldBeEqualTo(nameOnProp)
    }

    @Test
    fun `Added end event works`() {
        val updatedProcess = readAndUpdateProcess(generateUpdateEvent(BpmnEndEvent::class))

        updatedProcess.processes[0].body!!.endEvent!!.filter { it.id == id }.shouldHaveSingleItem().name.shouldBeEqualTo(nameOnProp)
    }

    @Test
    fun `Added end termination event works`() {
        val updatedProcess = readAndUpdateProcess(generateUpdateEvent(BpmnEndTerminateEvent::class))

        updatedProcess.processes[0].body!!.terminateEndEvent!!.filter { it.id == id }.shouldHaveSingleItem().name.shouldBeEqualTo(nameOnProp)
    }

    @Test
    fun `Added end escalation event works`() {
        val updatedProcess = readAndUpdateProcess(generateUpdateEvent(BpmnEndEscalationEvent::class))

        updatedProcess.processes[0].body!!.escalationEndEvent!!.filter { it.id == id }.shouldHaveSingleItem().name.shouldBeEqualTo(nameOnProp)
    }

    @Test
    fun `Added end cancel event works`() {
        val updatedProcess = readAndUpdateProcess(generateUpdateEvent(BpmnEndCancelEvent::class))

        updatedProcess.processes[0].body!!.cancelEndEvent!!.filter { it.id == id }.shouldHaveSingleItem().name.shouldBeEqualTo(nameOnProp)
    }

    @Test
    fun `Added end error event works`() {
        val updatedProcess = readAndUpdateProcess(generateUpdateEvent(BpmnEndErrorEvent::class))

        updatedProcess.processes[0].body!!.errorEndEvent!!.filter { it.id == id }.shouldHaveSingleItem().name.shouldBeEqualTo(nameOnProp)
    }

    @Test
    fun `Added boundary cancel event works`() {
        val updatedProcess = readAndUpdateProcess(generateUpdateEvent(BpmnBoundaryCancelEvent::class))

        updatedProcess.processes[0].body!!.boundaryCancelEvent!!.filter { it.id == id }.shouldHaveSingleItem().name.shouldBeEqualTo(nameOnProp)
    }

    @Test
    fun `Added boundary compensation event works`() {
        val updatedProcess = readAndUpdateProcess(generateUpdateEvent(BpmnBoundaryCompensationEvent::class))

        updatedProcess.processes[0].body!!.boundaryCompensationEvent!!.filter { it.id == id }.shouldHaveSingleItem().name.shouldBeEqualTo(nameOnProp)
    }

    @Test
    fun `Added boundary condtional event works`() {
        val updatedProcess = readAndUpdateProcess(generateUpdateEvent(BpmnBoundaryConditionalEvent::class))

        updatedProcess.processes[0].body!!.boundaryConditionalEvent!!.filter { it.id == id }.shouldHaveSingleItem().name.shouldBeEqualTo(nameOnProp)
    }

    @Test
    fun `Added boundary error event works`() {
        val updatedProcess = readAndUpdateProcess(generateUpdateEvent(BpmnBoundaryErrorEvent::class))

        updatedProcess.processes[0].body!!.boundaryErrorEvent!!.filter { it.id == id }.shouldHaveSingleItem().name.shouldBeEqualTo(nameOnProp)
    }

    @Test
    fun `Added boundary escalation event works`() {
        val updatedProcess = readAndUpdateProcess(generateUpdateEvent(BpmnBoundaryEscalationEvent::class))

        updatedProcess.processes[0].body!!.boundaryEscalationEvent!!.filter { it.id == id }.shouldHaveSingleItem().name.shouldBeEqualTo(nameOnProp)
    }

    @Test
    fun `Added boundary message event works`() {
        val updatedProcess = readAndUpdateProcess(generateUpdateEvent(BpmnBoundaryMessageEvent::class))

        updatedProcess.processes[0].body!!.boundaryMessageEvent!!.filter { it.id == id }.shouldHaveSingleItem().name.shouldBeEqualTo(nameOnProp)
    }

    @Test
    fun `Added boundary signal event works`() {
        val updatedProcess = readAndUpdateProcess(generateUpdateEvent(BpmnBoundarySignalEvent::class))

        updatedProcess.processes[0].body!!.boundarySignalEvent!!.filter { it.id == id }.shouldHaveSingleItem().name.shouldBeEqualTo(nameOnProp)
    }

    @Test
    fun `Added boundary timer event works`() {
        val updatedProcess = readAndUpdateProcess(generateUpdateEvent(BpmnBoundaryTimerEvent::class))

        updatedProcess.processes[0].body!!.boundaryTimerEvent!!.filter { it.id == id }.shouldHaveSingleItem().name.shouldBeEqualTo(nameOnProp)
    }

    @Test
    fun `Added intermediate timer catching event works`() {
        val updatedProcess = readAndUpdateProcess(generateUpdateEvent(BpmnIntermediateTimerCatchingEvent::class))

        updatedProcess.processes[0].body!!.intermediateTimerCatchingEvent!!.filter { it.id == id }.shouldHaveSingleItem().name.shouldBeEqualTo(nameOnProp)
    }

    @Test
    fun `Added intermediate message catching event works`() {
        val updatedProcess = readAndUpdateProcess(generateUpdateEvent(BpmnIntermediateMessageCatchingEvent::class))

        updatedProcess.processes[0].body!!.intermediateMessageCatchingEvent!!.filter { it.id == id }.shouldHaveSingleItem().name.shouldBeEqualTo(nameOnProp)
    }

    @Test
    fun `Added intermediate signal catching event works`() {
        val updatedProcess = readAndUpdateProcess(generateUpdateEvent(BpmnIntermediateSignalCatchingEvent::class))

        updatedProcess.processes[0].body!!.intermediateSignalCatchingEvent!!.filter { it.id == id }.shouldHaveSingleItem().name.shouldBeEqualTo(nameOnProp)
    }

    @Test
    fun `Added intermediate conditional catching event works`() {
        val updatedProcess = readAndUpdateProcess(generateUpdateEvent(BpmnIntermediateConditionalCatchingEvent::class))

        updatedProcess.processes[0].body!!.intermediateConditionalCatchingEvent!!.filter { it.id == id }.shouldHaveSingleItem().name.shouldBeEqualTo(nameOnProp)
    }

    @Test
    fun `Added intermediate none throwing event works`() {
        val updatedProcess = readAndUpdateProcess(generateUpdateEvent(BpmnIntermediateNoneThrowingEvent::class))

        updatedProcess.processes[0].body!!.intermediateNoneThrowingEvent!!.filter { it.id == id }.shouldHaveSingleItem().name.shouldBeEqualTo(nameOnProp)
    }

    @Test
    fun `Added intermediate signal throwing event works`() {
        val updatedProcess = readAndUpdateProcess(generateUpdateEvent(BpmnIntermediateSignalThrowingEvent::class))

        updatedProcess.processes[0].body!!.intermediateSignalThrowingEvent!!.filter { it.id == id }.shouldHaveSingleItem().name.shouldBeEqualTo(nameOnProp)
    }

    @Test
    fun `Added intermediate escalation throwing event works`() {
        val updatedProcess = readAndUpdateProcess(generateUpdateEvent(BpmnIntermediateEscalationThrowingEvent::class))

        updatedProcess.processes[0].body!!.intermediateEscalationThrowingEvent!!.filter { it.id == id }.shouldHaveSingleItem().name.shouldBeEqualTo(nameOnProp)
    }

    @Test
    fun `Added user task event works`() {
        val updatedProcess = readAndUpdateProcess(generateUpdateEvent(BpmnUserTask::class))

        updatedProcess.processes[0].body!!.userTask!!.filter { it.id == id }.shouldHaveSingleItem().name.shouldBeEqualTo(nameOnProp)
    }

    @Test
    fun `Added script task event works`() {
        val updatedProcess = readAndUpdateProcess(generateUpdateEvent(BpmnScriptTask::class))

        updatedProcess.processes[0].body!!.scriptTask!!.filter { it.id == id }.shouldHaveSingleItem().name.shouldBeEqualTo(nameOnProp)
    }

    @Test
    fun `Added service task event works`() {
        val updatedProcess = readAndUpdateProcess(generateUpdateEvent(BpmnServiceTask::class))

        updatedProcess.processes[0].body!!.serviceTask!!.filter { it.id == id }.shouldHaveSingleItem().name.shouldBeEqualTo(nameOnProp)
    }

    @Test
    fun `Added business rule task event works`() {
        val updatedProcess = readAndUpdateProcess(generateUpdateEvent(BpmnBusinessRuleTask::class))

        updatedProcess.processes[0].body!!.businessRuleTask!!.filter { it.id == id }.shouldHaveSingleItem().name.shouldBeEqualTo(nameOnProp)
    }

    @Test
    fun `Added receive task event works`() {
        val updatedProcess = readAndUpdateProcess(generateUpdateEvent(BpmnReceiveTask::class))

        updatedProcess.processes[0].body!!.receiveTask!!.filter { it.id == id }.shouldHaveSingleItem().name.shouldBeEqualTo(nameOnProp)
    }

    @Test
    fun `Added camel task event works`() {
        val updatedProcess = readAndUpdateProcess(generateUpdateEvent(BpmnCamelTask::class))

        updatedProcess.processes[0].body!!.camelTask!!.filter { it.id == id }.shouldHaveSingleItem().name.shouldBeEqualTo(nameOnProp)
    }

    @Test
    fun `Added http task event works`() {
        val updatedProcess = readAndUpdateProcess(generateUpdateEvent(BpmnHttpTask::class))

        updatedProcess.processes[0].body!!.httpTask!!.filter { it.id == id }.shouldHaveSingleItem().name.shouldBeEqualTo(nameOnProp)
    }

    @Test
    fun `Added mule task event works`() {
        val updatedProcess = readAndUpdateProcess(generateUpdateEvent(BpmnMuleTask::class))

        updatedProcess.processes[0].body!!.muleTask!!.filter { it.id == id }.shouldHaveSingleItem().name.shouldBeEqualTo(nameOnProp)
    }

    @Test
    fun `Added decision task event works`() {
        val updatedProcess = readAndUpdateProcess(generateUpdateEvent(BpmnDecisionTask::class))

        updatedProcess.processes[0].body!!.decisionTask!!.filter { it.id == id }.shouldHaveSingleItem().name.shouldBeEqualTo(nameOnProp)
    }

    @Test
    fun `Added shell task event works`() {
        val updatedProcess = readAndUpdateProcess(generateUpdateEvent(BpmnShellTask::class))

        updatedProcess.processes[0].body!!.shellTask!!.filter { it.id == id }.shouldHaveSingleItem().name.shouldBeEqualTo(nameOnProp)
    }

    @Test
    fun `Added call activity event works`() {
        val updatedProcess = readAndUpdateProcess(generateUpdateEvent(BpmnCallActivity::class))

        updatedProcess.processes[0].body!!.callActivity!!.filter { it.id == id }.shouldHaveSingleItem().name.shouldBeEqualTo(nameOnProp)
    }

    @Test
    fun `Added sub process event works`() {
        val updatedProcess = readAndUpdateProcess(generateUpdateEvent(BpmnSubProcess::class))

        updatedProcess.processes[0].body!!.subProcess!!.filter { it.id == id }.shouldHaveSingleItem().name.shouldBeEqualTo(nameOnProp)
    }

    @Test
    fun `Added event sub process event works`() {
        val updatedProcess = readAndUpdateProcess(generateUpdateEvent(BpmnEventSubprocess::class))

        updatedProcess.processes[0].body!!.eventSubProcess!!.filter { it.id == id }.shouldHaveSingleItem().name.shouldBeEqualTo(nameOnProp)
    }

    @Test
    fun `Added adhoc sub process event works`() {
        val updatedProcess = readAndUpdateProcess(generateUpdateEvent(BpmnAdHocSubProcess::class))

        updatedProcess.processes[0].body!!.adHocSubProcess!!.filter { it.id == id }.shouldHaveSingleItem().name.shouldBeEqualTo(nameOnProp)
    }

    @Test
    fun `Added transactional sub process event works`() {
        val updatedProcess = readAndUpdateProcess(generateUpdateEvent(BpmnTransactionalSubProcess::class))

        updatedProcess.processes[0].body!!.transaction!!.filter { it.id == id }.shouldHaveSingleItem().name.shouldBeEqualTo(nameOnProp)
    }

    @Test
    fun `Added exclusive gateway works`() {
        val updatedProcess = readAndUpdateProcess(generateUpdateEvent(BpmnExclusiveGateway::class))

        updatedProcess.processes[0].body!!.exclusiveGateway!!.filter { it.id == id }.shouldHaveSingleItem().name.shouldBeEqualTo(nameOnProp)
    }

    @Test
    fun `Added parallel gateway works`() {
        val updatedProcess = readAndUpdateProcess(generateUpdateEvent(BpmnParallelGateway::class))

        updatedProcess.processes[0].body!!.parallelGateway!!.filter { it.id == id }.shouldHaveSingleItem().name.shouldBeEqualTo(nameOnProp)
    }

    @Test
    fun `Added inclusive gateway works`() {
        val updatedProcess = readAndUpdateProcess(generateUpdateEvent(BpmnInclusiveGateway::class))

        updatedProcess.processes[0].body!!.inclusiveGateway!!.filter { it.id == id }.shouldHaveSingleItem().name.shouldBeEqualTo(nameOnProp)
    }

    @Test
    fun `Added event gateway works`() {
        val updatedProcess = readAndUpdateProcess(generateUpdateEvent(BpmnEventGateway::class))

        updatedProcess.processes[0].body!!.eventBasedGateway!!.filter { it.id == id }.shouldHaveSingleItem().name.shouldBeEqualTo(nameOnProp)
    }

    private fun <T: WithBpmnId> generateUpdateEvent(clazz: KClass<T>): EventPropagatableToXml {
        return BpmnShapeObjectAddedEvent(
                WithParentId(processId, createClass(clazz)),
                ShapeElement(diagramId, id, BoundsElement(0.0f, 0.0f, 10.0f, 10.0f)),
                PropertyTable(mutableMapOf(Pair(PropertyType.ID, mutableListOf(Property(id.id))), Pair(PropertyType.NAME, mutableListOf(Property(nameOnProp)))))
        )
    }

    private fun <T: Any> createClass(clazz: KClass<T>): T {
        val ctor = clazz.primaryConstructor!!
        val args = mutableListOf<Any?>()
        ctor.parameters.forEach {
            if (it.type.classifier == BpmnElementId::class) {
                args += BpmnElementId(UUID.randomUUID().toString())
            } else if (it.type.classifier == Boolean::class && !it.isOptional) {
                args.add(true)
            } else {
                args.add(null)
            }
        }
        return ctor.call(*args.toTypedArray())
    }

    private fun readAndUpdateProcess(event: EventPropagatableToXml): BpmnFileObject {
        val updated = parser.update(
                "simple-nested.bpmn20.xml".asResource()!!,
                listOf(event)
        )

        updated.shouldNotBeNull()

        return parser.parse(updated)
    }
}
