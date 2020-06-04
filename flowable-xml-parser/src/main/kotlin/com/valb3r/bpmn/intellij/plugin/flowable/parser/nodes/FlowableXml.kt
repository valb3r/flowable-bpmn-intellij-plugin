package com.valb3r.bpmn.intellij.plugin.flowable.parser.nodes

import com.fasterxml.jackson.annotation.JsonMerge
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.BpmnProcess
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.events.begin.*
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.events.boundary.*
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.events.catching.*
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.events.end.*
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.events.throwing.BpmnIntermediateEscalationThrowingEvent
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.events.throwing.BpmnIntermediateNoneThrowingEvent
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.events.throwing.BpmnIntermediateSignalThrowingEvent
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.events.throwing.BpmnIntermediateThrowingEvent
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.tasks.*
import com.valb3r.bpmn.intellij.plugin.bpmn.api.diagram.DiagramElement
import com.valb3r.bpmn.intellij.plugin.flowable.parser.nodes.diagram.DiagramElementIdMapper
import com.valb3r.bpmn.intellij.plugin.flowable.parser.nodes.diagram.Plane
import com.valb3r.bpmn.intellij.plugin.flowable.parser.nodes.process.*
import org.mapstruct.Mapper
import org.mapstruct.factory.Mappers

// For mixed lists in XML we need to have JsonSetter/JsonMerge on field
// https://github.com/FasterXML/jackson-dataformat-xml/issues/363
// unfortunately this has failed with Kotlin 'data' classes
class BpmnFile(
        @JacksonXmlProperty(localName = "message")
        @JsonMerge
        @JacksonXmlElementWrapper(useWrapping = false)
        var messages: List<MessageNode>? = null,

        @JacksonXmlProperty(localName = "process")
        @JsonMerge
        @JacksonXmlElementWrapper(useWrapping = false)
        var processes: List<ProcessNode>,

        @JacksonXmlProperty(localName = "BPMNDiagram")
        @JsonMerge
        @JacksonXmlElementWrapper(useWrapping = false)
        var diagrams: List<DiagramNode>? = null
)

data class MessageNode(val id: String, var name: String?)

open class ProcessBody {
    
    // Events
    @JsonMerge @JacksonXmlElementWrapper(useWrapping = false)
    var startEvent: List<StartEventNode>? = null
    @JsonMerge @JacksonXmlElementWrapper(useWrapping = false)
    var endEvent: List<EndEventNode>? = null
    @JsonMerge @JacksonXmlElementWrapper(useWrapping = false)
    var boundaryEvent: List<BoundaryEvent>? = null
    // Events-intermediate
    @JsonMerge @JacksonXmlElementWrapper(useWrapping = false)
    var intermediateCatchEvent: List<IntermediateCatchEvent>? = null
    @JsonMerge @JacksonXmlElementWrapper(useWrapping = false)
    var intermediateThrowEvent: List<IntermediateThrowEvent>? = null

    // Service task alike:
    @JsonMerge @JacksonXmlElementWrapper(useWrapping = false)
    var userTask: List<UserTask>? = null
    @JsonMerge @JacksonXmlElementWrapper(useWrapping = false)
    var scriptTask: List<ScriptTask>? = null
    @JsonMerge @JacksonXmlElementWrapper(useWrapping = false)
    var serviceTask: List<ServiceTask>? = null
    @JsonMerge @JacksonXmlElementWrapper(useWrapping = false)
    var businessRuleTask: List<BusinessRuleTask>? = null
    @JsonMerge @JacksonXmlElementWrapper(useWrapping = false)
    var receiveTask: List<ReceiveTask>? = null

    // Sub process alike
    @JsonMerge @JacksonXmlElementWrapper(useWrapping = false)
    var callActivity: List<CallActivity>? = null
    @JsonMerge @JacksonXmlElementWrapper(useWrapping = false)
    var subProcess: List<SubProcess>? = null
    @JsonMerge @JacksonXmlElementWrapper(useWrapping = false)
    var transaction: List<Transaction>? = null
    @JsonMerge @JacksonXmlElementWrapper(useWrapping = false)
    var adHocSubProcess: List<AdHocSubProcess>? = null

    // Gateways
    @JsonMerge @JacksonXmlElementWrapper(useWrapping = false)
    var exclusiveGateway: List<ExclusiveGateway>? = null
    @JsonMerge @JacksonXmlElementWrapper(useWrapping = false)
    var parallelGateway: List<ParallelGateway>? = null
    @JsonMerge @JacksonXmlElementWrapper(useWrapping = false)
    var inclusiveGateway: List<InclusiveGateway>? = null
    @JsonMerge @JacksonXmlElementWrapper(useWrapping = false)
    var eventBasedGateway: List<EventBasedGateway>? = null

    // Linking elements
    @JsonMerge @JacksonXmlElementWrapper(useWrapping = false)
    var sequenceFlow: List<SequenceFlow>? = null
}

// For mixed lists in XML we need to have JsonSetter/JsonMerge on field
// https://github.com/FasterXML/jackson-dataformat-xml/issues/363
// unfortunately this has failed with Kotlin 'data' classes
class ProcessNode: BpmnMappable<BpmnProcess>, ProcessBody() {

    @JacksonXmlProperty(isAttribute = true) var id: String? = null // it is false - it is non-null
    @JacksonXmlProperty(isAttribute = true) var name: String? = null // it is false - it is non-null
    var documentation: String? = null
    @JacksonXmlProperty(isAttribute = true) var isExecutable: Boolean? = null

    override fun toElement(): BpmnProcess {
        var result = Mappers.getMapper(Mapping::class.java).convertToDto(this)
        result = applyServiceTaskCustomizationByType(result)
        result = applyIntermediateCatchEventCustomizationByType(result)
        result = applyIntermediateThrowingEventCustomizationByType(result)
        result = applyEndEventCustomizationByType(result)
        result = applyStartEventCustomizationByType(result)
        result = applyBoundaryEventCustomizationByType(result)
        return result
    }

    private fun applyServiceTaskCustomizationByType(process: BpmnProcess): BpmnProcess {
        var result = process
        result = extractTasksBasedOnType(result, "camel",  Mappers.getMapper(CamelMapper::class.java)) { updates, target -> target.copy(camelTask = updates) }
        result = extractTasksBasedOnType(result, "http",  Mappers.getMapper(HttpMapper::class.java)) { updates, target -> target.copy(httpTask = updates) }
        result = extractTasksBasedOnType(result, "mule",  Mappers.getMapper(MuleMapper::class.java)) { updates, target -> target.copy(muleTask = updates) }
        result = extractTasksBasedOnType(result, "dmn",  Mappers.getMapper(DecisionMapper::class.java)) { updates, target -> target.copy(decisionTask = updates) }
        result = extractTasksBasedOnType(result, "shell",  Mappers.getMapper(ShellMapper::class.java)) { updates, target -> target.copy(shellTask = updates) }
        return result
    }

    private fun applyIntermediateCatchEventCustomizationByType(process: BpmnProcess): BpmnProcess {
        var result = process
        result = extractIntermediateCatchEventsBasedOnType(result, { null != it.timerEventDefinition },  Mappers.getMapper(TimerCatchingMapper::class.java)) { updates, target -> target.copy(intermediateTimerCatchingEvent = updates) }
        result = extractIntermediateCatchEventsBasedOnType(result, { null != it.signalEventDefinition },  Mappers.getMapper(SignalCatchingMapper::class.java)) { updates, target -> target.copy(intermediateSignalCatchingEvent = updates) }
        result = extractIntermediateCatchEventsBasedOnType(result, { null != it.messageEventDefinition },  Mappers.getMapper(MessageCatchingMapper::class.java)) { updates, target -> target.copy(intermediateMessageCatchingEvent = updates) }
        result = extractIntermediateCatchEventsBasedOnType(result, { null != it.conditionalEventDefinition },  Mappers.getMapper(ConditionalCatchingMapper::class.java)) { updates, target -> target.copy(intermediateConditionalCatchingEvent = updates) }
        return result
    }

    private fun applyIntermediateThrowingEventCustomizationByType(process: BpmnProcess): BpmnProcess {
        var result = process
        result = extractIntermediateThrowingEventsBasedOnType(result, { null == it.escalationEventDefinition && null == it.signalEventDefinition },  Mappers.getMapper(NoneThrowMapper::class.java)) { updates, target -> target.copy(intermediateNoneThrowingEvent = updates) }
        result = extractIntermediateThrowingEventsBasedOnType(result, { null != it.signalEventDefinition },  Mappers.getMapper(SignalThrowMapper::class.java)) { updates, target -> target.copy(intermediateSignalThrowingEvent = updates) }
        result = extractIntermediateThrowingEventsBasedOnType(result, { null != it.escalationEventDefinition },  Mappers.getMapper(EscalationThrowMapper::class.java)) { updates, target -> target.copy(intermediateEscalationThrowingEvent = updates) }
        return result
    }

    private fun applyEndEventCustomizationByType(process: BpmnProcess): BpmnProcess {
        var result = process
        result = extractEndEventsBasedOnType(result, { null != it.errorEventDefinition },  Mappers.getMapper(EndErrorMapper::class.java)) { updates, target -> target.copy(errorEndEvent = updates) }
        result = extractEndEventsBasedOnType(result, { null != it.escalationEventDefinition },  Mappers.getMapper(EndEscalationMapper::class.java)) { updates, target -> target.copy(escalationEndEvent = updates) }
        result = extractEndEventsBasedOnType(result, { null != it.cancelEventDefinition },  Mappers.getMapper(EndCancelMapper::class.java)) { updates, target -> target.copy(cancelEndEvent = updates) }
        result = extractEndEventsBasedOnType(result, { null != it.terminateEventDefinition },  Mappers.getMapper(EndTerminationMapper::class.java)) { updates, target -> target.copy(terminateEndEvent = updates) }
        return result
    }

    private fun applyStartEventCustomizationByType(process: BpmnProcess): BpmnProcess {
        var result = process
        result = extractStartEventsBasedOnType(result, { null != it.conditionalEventDefinition },  Mappers.getMapper(StartConditionalMapper::class.java)) { updates, target -> target.copy(conditionalStartEvent = updates) }
        result = extractStartEventsBasedOnType(result, { null != it.errorEventDefinition },  Mappers.getMapper(StartErrorMapper::class.java)) { updates, target -> target.copy(errorStartEvent = updates) }
        result = extractStartEventsBasedOnType(result, { null != it.escalationEventDefinition },  Mappers.getMapper(StartEscalationMapper::class.java)) { updates, target -> target.copy(escalationStartEvent = updates) }
        result = extractStartEventsBasedOnType(result, { null != it.messageEventDefinition },  Mappers.getMapper(StartMessageMapper::class.java)) { updates, target -> target.copy(messageStartEvent = updates) }
        result = extractStartEventsBasedOnType(result, { null != it.signalEventDefinition },  Mappers.getMapper(StartSignalMapper::class.java)) { updates, target -> target.copy(signalStartEvent = updates) }
        result = extractStartEventsBasedOnType(result, { null != it.timerEventDefinition },  Mappers.getMapper(StartTimerMapper::class.java)) { updates, target -> target.copy(timerStartEvent = updates) }
        return result
    }

    private fun applyBoundaryEventCustomizationByType(process: BpmnProcess): BpmnProcess {
        var result = process
        result = extractBoundaryEventsBasedOnType(result, { null != it.cancelEventDefinition },  Mappers.getMapper(BoundaryCancelMapper::class.java)) { updates, target -> target.copy(boundaryCancelEvent = updates) }
        result = extractBoundaryEventsBasedOnType(result, { null != it.compensateEventDefinition },  Mappers.getMapper(BoundaryCompensationMapper::class.java)) { updates, target -> target.copy(boundaryCompensationEvent = updates) }
        result = extractBoundaryEventsBasedOnType(result, { null != it.conditionalEventDefinition },  Mappers.getMapper(BoundaryConditionalMapper::class.java)) { updates, target -> target.copy(boundaryConditionalEvent = updates) }
        result = extractBoundaryEventsBasedOnType(result, { null != it.errorEventDefinition },  Mappers.getMapper(BoundaryErrorMapper::class.java)) { updates, target -> target.copy(boundaryErrorEvent = updates) }
        result = extractBoundaryEventsBasedOnType(result, { null != it.escalationEventDefinition },  Mappers.getMapper(BoundaryEscalationMapper::class.java)) { updates, target -> target.copy(boundaryEscalationEvent = updates) }
        result = extractBoundaryEventsBasedOnType(result, { null != it.messageEventDefinition },  Mappers.getMapper(BoundaryMessageMapper::class.java)) { updates, target -> target.copy(boundaryMessageEvent = updates) }
        result = extractBoundaryEventsBasedOnType(result, { null != it.signalEventDefinition },  Mappers.getMapper(BoundarySignalMapper::class.java)) { updates, target -> target.copy(boundarySignalEvent = updates) }
        result = extractBoundaryEventsBasedOnType(result, { null != it.timerEventDefinition },  Mappers.getMapper(BoundaryTimerMapper::class.java)) { updates, target -> target.copy(boundaryTimerEvent = updates) }
        return result
    }

    private fun <T> extractTasksBasedOnType(process: BpmnProcess, type: String, mapper: ServiceTaskMapper<T>, update: (List<T>?, BpmnProcess) -> BpmnProcess): BpmnProcess {
        process.serviceTask?.apply {
            val byTypeGroup = this.groupBy { it.type == type }
            val newProcess = process.copy(serviceTask = byTypeGroup[false])
            return update(byTypeGroup[true]?.map { mapper.convertToDto(it) }, newProcess)
        }

        return process
    }

    private fun <T> extractIntermediateCatchEventsBasedOnType(process: BpmnProcess, filter: (BpmnIntermediateCatchingEvent) -> Boolean, mapper: IntermediateCatchEventMapper<T>, update: (List<T>?, BpmnProcess) -> BpmnProcess): BpmnProcess {
        process.intermediateCatchEvent?.apply {
            val byTypeGroup = this.groupBy { filter(it) }
            val newProcess = process.copy(intermediateCatchEvent = byTypeGroup[false])
            return update(byTypeGroup[true]?.map { mapper.convertToDto(it) }, newProcess)
        }

        return process
    }

    private fun <T> extractIntermediateThrowingEventsBasedOnType(process: BpmnProcess, filter: (BpmnIntermediateThrowingEvent) -> Boolean, mapper: IntermediateThrowEventMapper<T>, update: (List<T>?, BpmnProcess) -> BpmnProcess): BpmnProcess {
        process.intermediateThrowEvent?.apply {
            val byTypeGroup = this.groupBy { filter(it) }
            val newProcess = process.copy(intermediateThrowEvent = byTypeGroup[false])
            return update(byTypeGroup[true]?.map { mapper.convertToDto(it) }, newProcess)
        }

        return process
    }

    private fun <T> extractEndEventsBasedOnType(process: BpmnProcess, filter: (BpmnEndEvent) -> Boolean, mapper: EndEventMapper<T>, update: (List<T>?, BpmnProcess) -> BpmnProcess): BpmnProcess {
        process.endEvent?.apply {
            val byTypeGroup = this.groupBy { filter(it) }
            val newProcess = process.copy(endEvent = byTypeGroup[false])
            return update(byTypeGroup[true]?.map { mapper.convertToDto(it) }, newProcess)
        }

        return process
    }

    private fun <T> extractStartEventsBasedOnType(process: BpmnProcess, filter: (BpmnStartEvent) -> Boolean, mapper: StartEventMapper<T>, update: (List<T>?, BpmnProcess) -> BpmnProcess): BpmnProcess {
        process.startEvent?.apply {
            val byTypeGroup = this.groupBy { filter(it) }
            val newProcess = process.copy(startEvent = byTypeGroup[false])
            return update(byTypeGroup[true]?.map { mapper.convertToDto(it) }, newProcess)
        }

        return process
    }

    private fun <T> extractBoundaryEventsBasedOnType(process: BpmnProcess, filter: (BpmnBoundaryEvent) -> Boolean, mapper: BoundaryEventMapper<T>, update: (List<T>?, BpmnProcess) -> BpmnProcess): BpmnProcess {
        process.boundaryEvent?.apply {
            val byTypeGroup = this.groupBy { filter(it) }
            val newProcess = process.copy(boundaryEvent = byTypeGroup[false])
            return update(byTypeGroup[true]?.map { mapper.convertToDto(it) }, newProcess)
        }

        return process
    }


    @Mapper(uses = [BpmnElementIdMapper::class])
    interface Mapping {
        fun convertToDto(input: ProcessNode): BpmnProcess
    }

    @Mapper
    interface CamelMapper: ServiceTaskMapper<BpmnCamelTask>

    @Mapper
    interface HttpMapper: ServiceTaskMapper<BpmnHttpTask>

    @Mapper
    interface MuleMapper: ServiceTaskMapper<BpmnMuleTask>

    @Mapper
    interface DecisionMapper: ServiceTaskMapper<BpmnDecisionTask>

    @Mapper
    interface ShellMapper: ServiceTaskMapper<BpmnShellTask>

    interface ServiceTaskMapper<T> {
        fun convertToDto(input: BpmnServiceTask): T
    }

    @Mapper
    interface TimerCatchingMapper: IntermediateCatchEventMapper<BpmnIntermediateTimerCatchingEvent>

    @Mapper
    interface SignalCatchingMapper: IntermediateCatchEventMapper<BpmnIntermediateSignalCatchingEvent>

    @Mapper
    interface MessageCatchingMapper: IntermediateCatchEventMapper<BpmnIntermediateMessageCatchingEvent>

    @Mapper
    interface ConditionalCatchingMapper: IntermediateCatchEventMapper<BpmnIntermediateConditionalCatchingEvent>

    interface IntermediateCatchEventMapper<T> {
        fun convertToDto(input: BpmnIntermediateCatchingEvent): T
    }

    @Mapper
    interface NoneThrowMapper: IntermediateThrowEventMapper<BpmnIntermediateNoneThrowingEvent>

    @Mapper
    interface SignalThrowMapper: IntermediateThrowEventMapper<BpmnIntermediateSignalThrowingEvent>

    @Mapper
    interface EscalationThrowMapper: IntermediateThrowEventMapper<BpmnIntermediateEscalationThrowingEvent>

    interface IntermediateThrowEventMapper<T> {
        fun convertToDto(input: BpmnIntermediateThrowingEvent): T
    }

    @Mapper
    interface EndCancelMapper: EndEventMapper<BpmnEndCancelEvent>

    @Mapper
    interface EndErrorMapper: EndEventMapper<BpmnEndErrorEvent>

    @Mapper
    interface EndEscalationMapper: EndEventMapper<BpmnEndEscalationEvent>

    @Mapper
    interface EndTerminationMapper: EndEventMapper<BpmnEndTerminateEvent>

    interface EndEventMapper<T> {
        fun convertToDto(input: BpmnEndEvent): T
    }

    @Mapper
    interface StartTimerMapper: StartEventMapper<BpmnStartTimerEvent>

    @Mapper
    interface StartSignalMapper: StartEventMapper<BpmnStartSignalEvent>

    @Mapper
    interface StartMessageMapper: StartEventMapper<BpmnStartMessageEvent>

    @Mapper
    interface StartErrorMapper: StartEventMapper<BpmnStartErrorEvent>

    @Mapper
    interface StartEscalationMapper: StartEventMapper<BpmnStartEscalationEvent>

    @Mapper
    interface StartConditionalMapper: StartEventMapper<BpmnStartConditionalEvent>

    interface StartEventMapper<T> {
        fun convertToDto(input: BpmnStartEvent): T
    }

    @Mapper
    interface BoundaryCancelMapper: BoundaryEventMapper<BpmnBoundaryCancelEvent>

    @Mapper
    interface BoundaryCompensationMapper: BoundaryEventMapper<BpmnBoundaryCompensationEvent>

    @Mapper
    interface BoundaryConditionalMapper: BoundaryEventMapper<BpmnBoundaryConditionalEvent>

    @Mapper
    interface BoundaryErrorMapper: BoundaryEventMapper<BpmnBoundaryErrorEvent>

    @Mapper
    interface BoundaryEscalationMapper: BoundaryEventMapper<BpmnBoundaryEscalationEvent>

    @Mapper
    interface BoundaryMessageMapper: BoundaryEventMapper<BpmnBoundaryMessageEvent>

    @Mapper
    interface BoundarySignalMapper: BoundaryEventMapper<BpmnBoundarySignalEvent>

    @Mapper
    interface BoundaryTimerMapper: BoundaryEventMapper<BpmnBoundaryTimerEvent>

    interface BoundaryEventMapper<T> {
        fun convertToDto(input: BpmnBoundaryEvent): T
    }
}

data class DiagramNode(
        @JacksonXmlProperty(isAttribute = true) val id: String,
        @JacksonXmlProperty(localName = "BPMNPlane") val bpmnPlane: Plane
) : BpmnMappable<DiagramElement> {

    override fun toElement(): DiagramElement {
        return Mappers.getMapper(Mapping::class.java).convertToDto(this)
    }

    @Mapper(uses = [DiagramElementIdMapper::class, BpmnElementIdMapper::class])
    interface Mapping {
        fun convertToDto(input: DiagramNode): DiagramElement
    }
}