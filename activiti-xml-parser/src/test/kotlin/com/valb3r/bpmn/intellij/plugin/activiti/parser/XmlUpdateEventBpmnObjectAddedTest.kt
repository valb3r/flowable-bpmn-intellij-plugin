package com.valb3r.bpmn.intellij.plugin.activiti.parser

import com.valb3r.bpmn.intellij.plugin.activiti.parser.testevents.BpmnShapeObjectAddedEvent
import com.valb3r.bpmn.intellij.plugin.bpmn.api.BpmnProcessObject
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

        updatedProcess.process.body!!.startEvent!!.filter { it.id == id }.shouldHaveSingleItem().name.shouldBeEqualTo(nameOnProp)
    }

    @Test
    fun `Added start timer event works`() {
        val updatedProcess = readAndUpdateProcess(generateUpdateEvent(BpmnStartTimerEvent::class))

        updatedProcess.process.body!!.timerStartEvent!!.filter { it.id == id }.shouldHaveSingleItem().name.shouldBeEqualTo(nameOnProp)
    }

    @Test
    fun `Added start signal event works`() {
        val updatedProcess = readAndUpdateProcess(generateUpdateEvent(BpmnStartSignalEvent::class))

        updatedProcess.process.body!!.signalStartEvent!!.filter { it.id == id }.shouldHaveSingleItem().name.shouldBeEqualTo(nameOnProp)
    }

    @Test
    fun `Added start message event works`() {
        val updatedProcess = readAndUpdateProcess(generateUpdateEvent(BpmnStartMessageEvent::class))

        updatedProcess.process.body!!.messageStartEvent!!.filter { it.id == id }.shouldHaveSingleItem().name.shouldBeEqualTo(nameOnProp)
    }

    @Test
    fun `Added start error event works`() {
        val updatedProcess = readAndUpdateProcess(generateUpdateEvent(BpmnStartErrorEvent::class))

        updatedProcess.process.body!!.errorStartEvent!!.filter { it.id == id }.shouldHaveSingleItem().name.shouldBeEqualTo(nameOnProp)
    }

    @Test
    fun `Added start conditional event works`() {
        val updatedProcess = readAndUpdateProcess(generateUpdateEvent(BpmnStartConditionalEvent::class))

        updatedProcess.process.body!!.conditionalStartEvent!!.filter { it.id == id }.shouldHaveSingleItem().name.shouldBeEqualTo(nameOnProp)
    }

    @Test
    fun `Added start escalation event works`() {
        val updatedProcess = readAndUpdateProcess(generateUpdateEvent(BpmnStartEscalationEvent::class))

        updatedProcess.process.body!!.escalationStartEvent!!.filter { it.id == id }.shouldHaveSingleItem().name.shouldBeEqualTo(nameOnProp)
    }

    @Test
    fun `Added end event works`() {
        val updatedProcess = readAndUpdateProcess(generateUpdateEvent(BpmnEndEvent::class))

        updatedProcess.process.body!!.endEvent!!.filter { it.id == id }.shouldHaveSingleItem().name.shouldBeEqualTo(nameOnProp)
    }

    @Test
    fun `Added end termination event works`() {
        val updatedProcess = readAndUpdateProcess(generateUpdateEvent(BpmnEndTerminateEvent::class))

        updatedProcess.process.body!!.terminateEndEvent!!.filter { it.id == id }.shouldHaveSingleItem().name.shouldBeEqualTo(nameOnProp)
    }

    @Test
    fun `Added end escalation event works`() {
        val updatedProcess = readAndUpdateProcess(generateUpdateEvent(BpmnEndEscalationEvent::class))

        updatedProcess.process.body!!.escalationEndEvent!!.filter { it.id == id }.shouldHaveSingleItem().name.shouldBeEqualTo(nameOnProp)
    }

    @Test
    fun `Added end cancel event works`() {
        val updatedProcess = readAndUpdateProcess(generateUpdateEvent(BpmnEndCancelEvent::class))

        updatedProcess.process.body!!.cancelEndEvent!!.filter { it.id == id }.shouldHaveSingleItem().name.shouldBeEqualTo(nameOnProp)
    }

    @Test
    fun `Added end error event works`() {
        val updatedProcess = readAndUpdateProcess(generateUpdateEvent(BpmnEndErrorEvent::class))

        updatedProcess.process.body!!.errorEndEvent!!.filter { it.id == id }.shouldHaveSingleItem().name.shouldBeEqualTo(nameOnProp)
    }

    @Test
    fun `Added boundary cancel event works`() {
        val updatedProcess = readAndUpdateProcess(generateUpdateEvent(BpmnBoundaryCancelEvent::class))

        updatedProcess.process.body!!.boundaryCancelEvent!!.filter { it.id == id }.shouldHaveSingleItem().name.shouldBeEqualTo(nameOnProp)
    }

    @Test
    fun `Added boundary compensation event works`() {
        val updatedProcess = readAndUpdateProcess(generateUpdateEvent(BpmnBoundaryCompensationEvent::class))

        updatedProcess.process.body!!.boundaryCompensationEvent!!.filter { it.id == id }.shouldHaveSingleItem().name.shouldBeEqualTo(nameOnProp)
    }

    @Test
    fun `Added boundary condtional event works`() {
        val updatedProcess = readAndUpdateProcess(generateUpdateEvent(BpmnBoundaryConditionalEvent::class))

        updatedProcess.process.body!!.boundaryConditionalEvent!!.filter { it.id == id }.shouldHaveSingleItem().name.shouldBeEqualTo(nameOnProp)
    }

    @Test
    fun `Added boundary error event works`() {
        val updatedProcess = readAndUpdateProcess(generateUpdateEvent(BpmnBoundaryErrorEvent::class))

        updatedProcess.process.body!!.boundaryErrorEvent!!.filter { it.id == id }.shouldHaveSingleItem().name.shouldBeEqualTo(nameOnProp)
    }

    @Test
    fun `Added boundary escalation event works`() {
        val updatedProcess = readAndUpdateProcess(generateUpdateEvent(BpmnBoundaryEscalationEvent::class))

        updatedProcess.process.body!!.boundaryEscalationEvent!!.filter { it.id == id }.shouldHaveSingleItem().name.shouldBeEqualTo(nameOnProp)
    }

    @Test
    fun `Added boundary message event works`() {
        val updatedProcess = readAndUpdateProcess(generateUpdateEvent(BpmnBoundaryMessageEvent::class))

        updatedProcess.process.body!!.boundaryMessageEvent!!.filter { it.id == id }.shouldHaveSingleItem().name.shouldBeEqualTo(nameOnProp)
    }

    @Test
    fun `Added boundary signal event works`() {
        val updatedProcess = readAndUpdateProcess(generateUpdateEvent(BpmnBoundarySignalEvent::class))

        updatedProcess.process.body!!.boundarySignalEvent!!.filter { it.id == id }.shouldHaveSingleItem().name.shouldBeEqualTo(nameOnProp)
    }

    @Test
    fun `Added boundary timer event works`() {
        val updatedProcess = readAndUpdateProcess(generateUpdateEvent(BpmnBoundaryTimerEvent::class))

        updatedProcess.process.body!!.boundaryTimerEvent!!.filter { it.id == id }.shouldHaveSingleItem().name.shouldBeEqualTo(nameOnProp)
    }

    @Test
    fun `Added intermediate timer catching event works`() {
        val updatedProcess = readAndUpdateProcess(generateUpdateEvent(BpmnIntermediateTimerCatchingEvent::class))

        updatedProcess.process.body!!.intermediateTimerCatchingEvent!!.filter { it.id == id }.shouldHaveSingleItem().name.shouldBeEqualTo(nameOnProp)
    }

    @Test
    fun `Added intermediate message catching event works`() {
        val updatedProcess = readAndUpdateProcess(generateUpdateEvent(BpmnIntermediateMessageCatchingEvent::class))

        updatedProcess.process.body!!.intermediateMessageCatchingEvent!!.filter { it.id == id }.shouldHaveSingleItem().name.shouldBeEqualTo(nameOnProp)
    }

    @Test
    fun `Added intermediate signal catching event works`() {
        val updatedProcess = readAndUpdateProcess(generateUpdateEvent(BpmnIntermediateSignalCatchingEvent::class))

        updatedProcess.process.body!!.intermediateSignalCatchingEvent!!.filter { it.id == id }.shouldHaveSingleItem().name.shouldBeEqualTo(nameOnProp)
    }

    @Test
    fun `Added intermediate conditional catching event works`() {
        val updatedProcess = readAndUpdateProcess(generateUpdateEvent(BpmnIntermediateConditionalCatchingEvent::class))

        updatedProcess.process.body!!.intermediateConditionalCatchingEvent!!.filter { it.id == id }.shouldHaveSingleItem().name.shouldBeEqualTo(nameOnProp)
    }

    @Test
    fun `Added intermediate none throwing event works`() {
        val updatedProcess = readAndUpdateProcess(generateUpdateEvent(BpmnIntermediateNoneThrowingEvent::class))

        updatedProcess.process.body!!.intermediateNoneThrowingEvent!!.filter { it.id == id }.shouldHaveSingleItem().name.shouldBeEqualTo(nameOnProp)
    }

    @Test
    fun `Added intermediate signal throwing event works`() {
        val updatedProcess = readAndUpdateProcess(generateUpdateEvent(BpmnIntermediateSignalThrowingEvent::class))

        updatedProcess.process.body!!.intermediateSignalThrowingEvent!!.filter { it.id == id }.shouldHaveSingleItem().name.shouldBeEqualTo(nameOnProp)
    }

    @Test
    fun `Added intermediate escalation throwing event works`() {
        val updatedProcess = readAndUpdateProcess(generateUpdateEvent(BpmnIntermediateEscalationThrowingEvent::class))

        updatedProcess.process.body!!.intermediateEscalationThrowingEvent!!.filter { it.id == id }.shouldHaveSingleItem().name.shouldBeEqualTo(nameOnProp)
    }

    @Test
    fun `Added user task event works`() {
        val updatedProcess = readAndUpdateProcess(generateUpdateEvent(BpmnUserTask::class))

        updatedProcess.process.body!!.userTask!!.filter { it.id == id }.shouldHaveSingleItem().name.shouldBeEqualTo(nameOnProp)
    }

    @Test
    fun `Added script task event works`() {
        val updatedProcess = readAndUpdateProcess(generateUpdateEvent(BpmnScriptTask::class))

        updatedProcess.process.body!!.scriptTask!!.filter { it.id == id }.shouldHaveSingleItem().name.shouldBeEqualTo(nameOnProp)
    }

    @Test
    fun `Added service task event works`() {
        val updatedProcess = readAndUpdateProcess(generateUpdateEvent(BpmnServiceTask::class))

        updatedProcess.process.body!!.serviceTask!!.filter { it.id == id }.shouldHaveSingleItem().name.shouldBeEqualTo(nameOnProp)
    }

    @Test
    fun `Added business rule task event works`() {
        val updatedProcess = readAndUpdateProcess(generateUpdateEvent(BpmnBusinessRuleTask::class))

        updatedProcess.process.body!!.businessRuleTask!!.filter { it.id == id }.shouldHaveSingleItem().name.shouldBeEqualTo(nameOnProp)
    }

    @Test
    fun `Added receive task event works`() {
        val updatedProcess = readAndUpdateProcess(generateUpdateEvent(BpmnReceiveTask::class))

        updatedProcess.process.body!!.receiveTask!!.filter { it.id == id }.shouldHaveSingleItem().name.shouldBeEqualTo(nameOnProp)
    }

    @Test
    fun `Added camel task event works`() {
        val updatedProcess = readAndUpdateProcess(generateUpdateEvent(BpmnCamelTask::class))

        updatedProcess.process.body!!.camelTask!!.filter { it.id == id }.shouldHaveSingleItem().name.shouldBeEqualTo(nameOnProp)
    }

    @Test
    fun `Added http task event works`() {
        val updatedProcess = readAndUpdateProcess(generateUpdateEvent(BpmnHttpTask::class))

        updatedProcess.process.body!!.httpTask!!.filter { it.id == id }.shouldHaveSingleItem().name.shouldBeEqualTo(nameOnProp)
    }

    @Test
    fun `Added mule task event works`() {
        val updatedProcess = readAndUpdateProcess(generateUpdateEvent(BpmnMuleTask::class))

        updatedProcess.process.body!!.muleTask!!.filter { it.id == id }.shouldHaveSingleItem().name.shouldBeEqualTo(nameOnProp)
    }

    @Test
    fun `Added decision task event works`() {
        val updatedProcess = readAndUpdateProcess(generateUpdateEvent(BpmnDecisionTask::class))

        updatedProcess.process.body!!.decisionTask!!.filter { it.id == id }.shouldHaveSingleItem().name.shouldBeEqualTo(nameOnProp)
    }

    @Test
    fun `Added shell task event works`() {
        val updatedProcess = readAndUpdateProcess(generateUpdateEvent(BpmnShellTask::class))

        updatedProcess.process.body!!.shellTask!!.filter { it.id == id }.shouldHaveSingleItem().name.shouldBeEqualTo(nameOnProp)
    }

    @Test
    fun `Added call activity event works`() {
        val updatedProcess = readAndUpdateProcess(generateUpdateEvent(BpmnCallActivity::class))

        updatedProcess.process.body!!.callActivity!!.filter { it.id == id }.shouldHaveSingleItem().name.shouldBeEqualTo(nameOnProp)
    }

    @Test
    fun `Added sub process event works`() {
        val updatedProcess = readAndUpdateProcess(generateUpdateEvent(BpmnSubProcess::class))

        updatedProcess.process.body!!.subProcess!!.filter { it.id == id }.shouldHaveSingleItem().name.shouldBeEqualTo(nameOnProp)
    }

    @Test
    fun `Added event sub process event works`() {
        val updatedProcess = readAndUpdateProcess(generateUpdateEvent(BpmnEventSubprocess::class))

        updatedProcess.process.body!!.eventSubProcess!!.filter { it.id == id }.shouldHaveSingleItem().name.shouldBeEqualTo(nameOnProp)
    }

    @Test
    fun `Added adhoc sub process event works`() {
        val updatedProcess = readAndUpdateProcess(generateUpdateEvent(BpmnAdHocSubProcess::class))

        updatedProcess.process.body!!.adHocSubProcess!!.filter { it.id == id }.shouldHaveSingleItem().name.shouldBeEqualTo(nameOnProp)
    }

    @Test
    fun `Added transactional sub process event works`() {
        val updatedProcess = readAndUpdateProcess(generateUpdateEvent(BpmnTransactionalSubProcess::class))

        updatedProcess.process.body!!.transaction!!.filter { it.id == id }.shouldHaveSingleItem().name.shouldBeEqualTo(nameOnProp)
    }

    @Test
    fun `Added exclusive gateway works`() {
        val updatedProcess = readAndUpdateProcess(generateUpdateEvent(BpmnExclusiveGateway::class))

        updatedProcess.process.body!!.exclusiveGateway!!.filter { it.id == id }.shouldHaveSingleItem().name.shouldBeEqualTo(nameOnProp)
    }

    @Test
    fun `Added parallel gateway works`() {
        val updatedProcess = readAndUpdateProcess(generateUpdateEvent(BpmnParallelGateway::class))

        updatedProcess.process.body!!.parallelGateway!!.filter { it.id == id }.shouldHaveSingleItem().name.shouldBeEqualTo(nameOnProp)
    }

    @Test
    fun `Added inclusive gateway works`() {
        val updatedProcess = readAndUpdateProcess(generateUpdateEvent(BpmnInclusiveGateway::class))

        updatedProcess.process.body!!.inclusiveGateway!!.filter { it.id == id }.shouldHaveSingleItem().name.shouldBeEqualTo(nameOnProp)
    }

    @Test
    fun `Added event gateway works`() {
        val updatedProcess = readAndUpdateProcess(generateUpdateEvent(BpmnEventGateway::class))

        updatedProcess.process.body!!.eventBasedGateway!!.filter { it.id == id }.shouldHaveSingleItem().name.shouldBeEqualTo(nameOnProp)
    }

    private fun <T: WithBpmnId> generateUpdateEvent(clazz: KClass<T>): EventPropagatableToXml {
        return BpmnShapeObjectAddedEvent(
                WithParentId(processId, createClass(clazz)),
                ShapeElement(diagramId, id, BoundsElement(0.0f, 0.0f, 10.0f, 10.0f)),
                mutableMapOf(Pair(PropertyType.ID, Property(id.id)), Pair(PropertyType.NAME, Property(nameOnProp)))
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

    private fun readAndUpdateProcess(event: EventPropagatableToXml): BpmnProcessObject {
        val updated = parser.update(
                "simple-nested.bpmn20.xml".asResource()!!,
                listOf(event)
        )

        updated.shouldNotBeNull()

        return parser.parse(updated)
    }
}