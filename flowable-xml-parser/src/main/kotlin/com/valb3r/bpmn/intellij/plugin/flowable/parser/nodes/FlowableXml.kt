package com.valb3r.bpmn.intellij.plugin.flowable.parser.nodes

import com.fasterxml.jackson.annotation.JsonMerge
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.BpmnElementId
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.BpmnProcess
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.BpmnProcessBody
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.events.begin.*
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.events.boundary.*
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.events.catching.*
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.events.end.*
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.events.throwing.BpmnIntermediateEscalationThrowingEvent
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.events.throwing.BpmnIntermediateNoneThrowingEvent
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.events.throwing.BpmnIntermediateSignalThrowingEvent
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.events.throwing.BpmnIntermediateThrowingEvent
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.subprocess.BpmnEventSubprocess
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.subprocess.BpmnSubProcess
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.tasks.*
import com.valb3r.bpmn.intellij.plugin.bpmn.api.diagram.DiagramElement
import com.valb3r.bpmn.intellij.plugin.flowable.parser.nodes.diagram.DiagramElementIdMapper
import com.valb3r.bpmn.intellij.plugin.flowable.parser.nodes.diagram.Plane
import com.valb3r.bpmn.intellij.plugin.flowable.parser.nodes.process.*
import org.mapstruct.Mapper
import org.mapstruct.Mapping
import org.mapstruct.Mappings
import org.mapstruct.factory.Mappers
import java.util.concurrent.ConcurrentHashMap


const val EXTENSION_ELEM_STREAM = "java(null == input.getExtensionElements() ? null : input.getExtensionElements().stream()"
const val EXTENSION_STRING_EXTRACTOR = ".map(it -> it.getString()).filter(java.util.Objects::nonNull).findFirst().orElse(null))"
const val EXTENSION_EXPRESSION_EXTRACTOR = ".map(it -> it.getExpression()).filter(java.util.Objects::nonNull).findFirst().orElse(null))"
const val EXTENSION_BOOLEAN_EXTRACTOR = ".map(it -> Boolean.valueOf(it.getString())).findFirst().orElse(null))"

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
    var manualTask: List<ManualTask>? = null
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
    private val mappers = ConcurrentHashMap<Class<*>, Any>()
    
    @JacksonXmlProperty(isAttribute = true) var id: String? = null // it is false - it is non-null
    @JacksonXmlProperty(isAttribute = true) var name: String? = null
    var documentation: String? = null
    @JacksonXmlProperty(isAttribute = true) var isExecutable: Boolean? = null

    override fun toElement(): BpmnProcess {
        val result = cachedMapper(ProcessNodeMapping::class.java).convertToDto(this)
        val mappedBody = mapBody(this)

        return result.copy(
                body = mappedBody,
                children = extractNestedProcesses(this)
        )
    }

    private fun extractNestedProcesses(body: ProcessBody): Map<BpmnElementId, BpmnProcessBody>? {
        val children = mutableMapOf<BpmnElementId, BpmnProcessBody>()

        // Using null-unwrapping as for self-nested elements we need POJO without constructor
        body.adHocSubProcess?.forEach { mapChildren(BpmnElementId(it.id!!), it, children) }
        body.subProcess?.forEach { mapChildren(BpmnElementId(it.id!!), it, children) }
        body.transaction?.forEach { mapChildren(BpmnElementId(it.id!!), it, children) }

        if (children.isNotEmpty()) {
            return children
        }

        return null
    }

    private fun mapChildren(id: BpmnElementId, body: ProcessBody, children: MutableMap<BpmnElementId, BpmnProcessBody>) {
        children[id] = mapBody(body)
        extractNestedProcesses(body)?.let { nested -> children += nested }
    }

    private fun mapBody(body: ProcessBody): BpmnProcessBody {
        val bodyMapper = cachedMapper(BodyMapping::class.java)
        return fillBodyWithDedicatedElements(bodyMapper.convertToDto(body))
    }

    private fun fillBodyWithDedicatedElements(processBody: BpmnProcessBody): BpmnProcessBody {
        var body = processBody
        body = applySubprocessCustomizationByEventTrigger(body)
        body = applyServiceTaskCustomizationByType(body)
        body = applyIntermediateCatchEventCustomizationByType(body)
        body = applyIntermediateThrowingEventCustomizationByType(body)
        body = applyEndEventCustomizationByType(body)
        body = applyStartEventCustomizationByType(body)
        return applyBoundaryEventCustomizationByType(body)
    }

    private fun applyServiceTaskCustomizationByType(process: BpmnProcessBody): BpmnProcessBody {
        var result = process
        result = extractTasksBasedOnType(result, "camel",  cachedMapper(CamelMapper::class.java)) { updates, target -> target.copy(camelTask = updates) }
        result = extractTasksBasedOnType(result, "http",  cachedMapper(HttpMapper::class.java)) { updates, target -> target.copy(httpTask = updates) }
        result = extractTasksBasedOnType(result, "external",  cachedMapper(ExternalTaskMapper::class.java)) { updates, target -> target.copy(externalTask = updates) }
        result = extractTasksBasedOnType(result, "mail",  cachedMapper(MailMapper::class.java)) { updates, target -> target.copy(mailTask = updates) }
        result = extractTasksBasedOnType(result, "mule",  cachedMapper(MuleMapper::class.java)) { updates, target -> target.copy(muleTask = updates) }
        result = extractTasksBasedOnType(result, "dmn",  cachedMapper(DecisionMapper::class.java)) { updates, target -> target.copy(decisionTask = updates) }
        result = extractTasksBasedOnType(result, "shell",  cachedMapper(ShellMapper::class.java)) { updates, target -> target.copy(shellTask = updates) }
        return result
    }

    private fun applySubprocessCustomizationByEventTrigger(process: BpmnProcessBody): BpmnProcessBody {
        val mapper = cachedMapper(EventSubProcessMapper::class.java)
        process.subProcess?.apply {
            val byTypeGroup = this.groupBy { it.triggeredByEvent == true }
            return process.copy(subProcess = byTypeGroup[false], eventSubProcess = byTypeGroup[true]?.map { mapper.convertToDto(it) })
        }
        return process
    }

    private fun applyIntermediateCatchEventCustomizationByType(process: BpmnProcessBody): BpmnProcessBody {
        var result = process
        result = extractIntermediateCatchEventsBasedOnType(result, { null != it.timerEventDefinition },  cachedMapper(TimerCatchingMapper::class.java)) { updates, target -> target.copy(intermediateTimerCatchingEvent = updates) }
        result = extractIntermediateCatchEventsBasedOnType(result, { null != it.signalEventDefinition },  cachedMapper(SignalCatchingMapper::class.java)) { updates, target -> target.copy(intermediateSignalCatchingEvent = updates) }
        result = extractIntermediateCatchEventsBasedOnType(result, { null != it.messageEventDefinition },  cachedMapper(MessageCatchingMapper::class.java)) { updates, target -> target.copy(intermediateMessageCatchingEvent = updates) }
        result = extractIntermediateCatchEventsBasedOnType(result, { null != it.conditionalEventDefinition },  cachedMapper(ConditionalCatchingMapper::class.java)) { updates, target -> target.copy(intermediateConditionalCatchingEvent = updates) }
        return result
    }

    private fun applyIntermediateThrowingEventCustomizationByType(process: BpmnProcessBody): BpmnProcessBody {
        var result = process
        result = extractIntermediateThrowingEventsBasedOnType(result, { null == it.escalationEventDefinition && null == it.signalEventDefinition },  cachedMapper(NoneThrowMapper::class.java)) { updates, target -> target.copy(intermediateNoneThrowingEvent = updates) }
        result = extractIntermediateThrowingEventsBasedOnType(result, { null != it.signalEventDefinition },  cachedMapper(SignalThrowMapper::class.java)) { updates, target -> target.copy(intermediateSignalThrowingEvent = updates) }
        result = extractIntermediateThrowingEventsBasedOnType(result, { null != it.escalationEventDefinition },  cachedMapper(EscalationThrowMapper::class.java)) { updates, target -> target.copy(intermediateEscalationThrowingEvent = updates) }
        return result
    }

    private fun applyEndEventCustomizationByType(process: BpmnProcessBody): BpmnProcessBody {
        var result = process
        result = extractEndEventsBasedOnType(result, { null != it.errorEventDefinition },  cachedMapper(EndErrorMapper::class.java)) { updates, target -> target.copy(errorEndEvent = updates) }
        result = extractEndEventsBasedOnType(result, { null != it.escalationEventDefinition },  cachedMapper(EndEscalationMapper::class.java)) { updates, target -> target.copy(escalationEndEvent = updates) }
        result = extractEndEventsBasedOnType(result, { null != it.cancelEventDefinition },  cachedMapper(EndCancelMapper::class.java)) { updates, target -> target.copy(cancelEndEvent = updates) }
        result = extractEndEventsBasedOnType(result, { null != it.terminateEventDefinition },  cachedMapper(EndTerminationMapper::class.java)) { updates, target -> target.copy(terminateEndEvent = updates) }
        return result
    }

    private fun applyStartEventCustomizationByType(process: BpmnProcessBody): BpmnProcessBody {
        var result = process
        result = extractStartEventsBasedOnType(result, { null != it.conditionalEventDefinition },  cachedMapper(StartConditionalMapper::class.java)) { updates, target -> target.copy(conditionalStartEvent = updates) }
        result = extractStartEventsBasedOnType(result, { null != it.errorEventDefinition },  cachedMapper(StartErrorMapper::class.java)) { updates, target -> target.copy(errorStartEvent = updates) }
        result = extractStartEventsBasedOnType(result, { null != it.escalationEventDefinition },  cachedMapper(StartEscalationMapper::class.java)) { updates, target -> target.copy(escalationStartEvent = updates) }
        result = extractStartEventsBasedOnType(result, { null != it.messageEventDefinition },  cachedMapper(StartMessageMapper::class.java)) { updates, target -> target.copy(messageStartEvent = updates) }
        result = extractStartEventsBasedOnType(result, { null != it.signalEventDefinition },  cachedMapper(StartSignalMapper::class.java)) { updates, target -> target.copy(signalStartEvent = updates) }
        result = extractStartEventsBasedOnType(result, { null != it.timerEventDefinition },  cachedMapper(StartTimerMapper::class.java)) { updates, target -> target.copy(timerStartEvent = updates) }
        return result
    }

    private fun applyBoundaryEventCustomizationByType(process: BpmnProcessBody): BpmnProcessBody {
        var result = process
        result = extractBoundaryEventsBasedOnType(result, { null != it.cancelEventDefinition },  cachedMapper(BoundaryCancelMapper::class.java)) { updates, target -> target.copy(boundaryCancelEvent = updates) }
        result = extractBoundaryEventsBasedOnType(result, { null != it.compensateEventDefinition },  cachedMapper(BoundaryCompensationMapper::class.java)) { updates, target -> target.copy(boundaryCompensationEvent = updates) }
        result = extractBoundaryEventsBasedOnType(result, { null != it.conditionalEventDefinition },  cachedMapper(BoundaryConditionalMapper::class.java)) { updates, target -> target.copy(boundaryConditionalEvent = updates) }
        result = extractBoundaryEventsBasedOnType(result, { null != it.errorEventDefinition },  cachedMapper(BoundaryErrorMapper::class.java)) { updates, target -> target.copy(boundaryErrorEvent = updates) }
        result = extractBoundaryEventsBasedOnType(result, { null != it.escalationEventDefinition },  cachedMapper(BoundaryEscalationMapper::class.java)) { updates, target -> target.copy(boundaryEscalationEvent = updates) }
        result = extractBoundaryEventsBasedOnType(result, { null != it.messageEventDefinition },  cachedMapper(BoundaryMessageMapper::class.java)) { updates, target -> target.copy(boundaryMessageEvent = updates) }
        result = extractBoundaryEventsBasedOnType(result, { null != it.signalEventDefinition },  cachedMapper(BoundarySignalMapper::class.java)) { updates, target -> target.copy(boundarySignalEvent = updates) }
        result = extractBoundaryEventsBasedOnType(result, { null != it.timerEventDefinition },  cachedMapper(BoundaryTimerMapper::class.java)) { updates, target -> target.copy(boundaryTimerEvent = updates) }
        return result
    }

    private fun <T> extractTasksBasedOnType(process: BpmnProcessBody, type: String, mapper: ServiceTaskMapper<T>, update: (List<T>?, BpmnProcessBody) -> BpmnProcessBody): BpmnProcessBody {
        process.serviceTask?.apply {
            val byTypeGroup = this.groupBy { it.type == type }
            val newProcess = process.copy(serviceTask = byTypeGroup[false])
            return update(byTypeGroup[true]?.map { mapper.convertToDto(it) }, newProcess)
        }

        return process
    }

    private fun <T> extractIntermediateCatchEventsBasedOnType(process: BpmnProcessBody, filter: (BpmnIntermediateCatchingEvent) -> Boolean, mapper: IntermediateCatchEventMapper<T>, update: (List<T>?, BpmnProcessBody) -> BpmnProcessBody): BpmnProcessBody {
        process.intermediateCatchEvent?.apply {
            val byTypeGroup = this.groupBy { filter(it) }
            val newProcess = process.copy(intermediateCatchEvent = byTypeGroup[false])
            return update(byTypeGroup[true]?.map { mapper.convertToDto(it) }, newProcess)
        }

        return process
    }

    private fun <T> extractIntermediateThrowingEventsBasedOnType(process: BpmnProcessBody, filter: (BpmnIntermediateThrowingEvent) -> Boolean, mapper: IntermediateThrowEventMapper<T>, update: (List<T>?, BpmnProcessBody) -> BpmnProcessBody): BpmnProcessBody {
        process.intermediateThrowEvent?.apply {
            val byTypeGroup = this.groupBy { filter(it) }
            val newProcess = process.copy(intermediateThrowEvent = byTypeGroup[false])
            return update(byTypeGroup[true]?.map { mapper.convertToDto(it) }, newProcess)
        }

        return process
    }

    private fun <T> extractEndEventsBasedOnType(process: BpmnProcessBody, filter: (BpmnEndEvent) -> Boolean, mapper: EndEventMapper<T>, update: (List<T>?, BpmnProcessBody) -> BpmnProcessBody): BpmnProcessBody {
        process.endEvent?.apply {
            val byTypeGroup = this.groupBy { filter(it) }
            val newProcess = process.copy(endEvent = byTypeGroup[false])
            return update(byTypeGroup[true]?.map { mapper.convertToDto(it) }, newProcess)
        }

        return process
    }

    private fun <T> extractStartEventsBasedOnType(process: BpmnProcessBody, filter: (BpmnStartEvent) -> Boolean, mapper: StartEventMapper<T>, update: (List<T>?, BpmnProcessBody) -> BpmnProcessBody): BpmnProcessBody {
        process.startEvent?.apply {
            val byTypeGroup = this.groupBy { filter(it) }
            val newProcess = process.copy(startEvent = byTypeGroup[false])
            return update(byTypeGroup[true]?.map { mapper.convertToDto(it) }, newProcess)
        }

        return process
    }

    private fun <T> extractBoundaryEventsBasedOnType(process: BpmnProcessBody, filter: (BpmnBoundaryEvent) -> Boolean, mapper: BoundaryEventMapper<T>, update: (List<T>?, BpmnProcessBody) -> BpmnProcessBody): BpmnProcessBody {
        process.boundaryEvent?.apply {
            val byTypeGroup = this.groupBy { filter(it) }
            val newProcess = process.copy(boundaryEvent = byTypeGroup[false])
            return update(byTypeGroup[true]?.map { mapper.convertToDto(it) }, newProcess)
        }

        return process
    }

    private fun <T> cachedMapper(mapper: Class<T>): T {
        return mappers.computeIfAbsent(mapper) { Mappers.getMapper(mapper) as Any } as T
    }
    
    @Mapper(uses = [BpmnElementIdMapper::class, BodyMapping::class])
    interface ProcessNodeMapping {
        fun convertToDto(input: ProcessNode): BpmnProcess
    }

    @Mapper(uses = [
        BpmnElementIdMapper::class,
        SubProcess.SubProcessMapping::class,
        Transaction.TransactionMapping::class,
        BusinessRuleTask.BusinessRuleTaskMapping::class,
        ServiceTask.ServiceTaskMapping::class,
        ManualTask.ManualTaskMapping::class,
        ReceiveTask.ReceiveTaskMapping::class,
        ScriptTask.ScriptTaskMapping::class,
        UserTask.UserTaskMapping::class,
        StartEventNode.StartEventNodeMapping::class
    ])
    interface BodyMapping {

        @Mappings(
                Mapping(source = "subProcess", target = "subProcess"),
                Mapping(source = "subProcess", target = "collapsedSubProcess"), // will be post-filtered
                Mapping(source = "transaction", target = "transaction"),
                Mapping(source = "transaction", target = "collapsedTransaction") // will be post-filtered
        )
        fun convertToDto(input: ProcessBody): BpmnProcessBody
    }

    @Mapper
    interface EventSubProcessMapper: SubProcessMapper<BpmnEventSubprocess>

    @Mapper
    interface CamelMapper: ServiceTaskMapper<BpmnCamelTask> {

        @Mappings(
            Mapping(source = "forCompensation", target = "isForCompensation"),
            Mapping(expression = "$EXTENSION_ELEM_STREAM.filter(it -> \"camelContext\".equals(it.getName()))$EXTENSION_STRING_EXTRACTOR",
                    target = "camelContext")
        )
        override fun convertToDto(input: BpmnServiceTask): BpmnCamelTask
    }

    @Mapper
    interface HttpMapper: ServiceTaskMapper<BpmnHttpTask> {

        @Mappings(
                Mapping(source = "forCompensation", target = "isForCompensation"),
                Mapping(expression = "$EXTENSION_ELEM_STREAM.filter(it -> \"requestMethod\".equals(it.getName()))$EXTENSION_STRING_EXTRACTOR",
                        target = "requestMethod"),
                Mapping(expression = "$EXTENSION_ELEM_STREAM.filter(it -> \"requestUrl\".equals(it.getName()))$EXTENSION_STRING_EXTRACTOR",
                        target = "requestUrl"),
                Mapping(expression = "$EXTENSION_ELEM_STREAM.filter(it -> \"requestHeaders\".equals(it.getName()))$EXTENSION_STRING_EXTRACTOR",
                        target = "requestHeaders"),
                Mapping(expression = "$EXTENSION_ELEM_STREAM.filter(it -> \"requestBody\".equals(it.getName()))$EXTENSION_STRING_EXTRACTOR",
                        target = "requestBody"),
                Mapping(expression = "$EXTENSION_ELEM_STREAM.filter(it -> \"requestBodyEncoding\".equals(it.getName()))$EXTENSION_STRING_EXTRACTOR",
                        target = "requestBodyEncoding"),
                Mapping(expression = "$EXTENSION_ELEM_STREAM.filter(it -> \"requestTimeout\".equals(it.getName()))$EXTENSION_STRING_EXTRACTOR",
                        target = "requestTimeout"),
                Mapping(expression = "$EXTENSION_ELEM_STREAM.filter(it -> \"disallowRedirects\".equals(it.getName()))$EXTENSION_BOOLEAN_EXTRACTOR",
                        target = "disallowRedirects"),
                Mapping(expression = "$EXTENSION_ELEM_STREAM.filter(it -> \"failStatusCodes\".equals(it.getName()))$EXTENSION_STRING_EXTRACTOR",
                        target = "failStatusCodes"),
                Mapping(expression = "$EXTENSION_ELEM_STREAM.filter(it -> \"handleStatusCodes\".equals(it.getName()))$EXTENSION_STRING_EXTRACTOR",
                        target = "handleStatusCodes"),
                Mapping(expression = "$EXTENSION_ELEM_STREAM.filter(it -> \"responseVariableName\".equals(it.getName()))$EXTENSION_STRING_EXTRACTOR",
                        target = "responseVariableName"),
                Mapping(expression = "$EXTENSION_ELEM_STREAM.filter(it -> \"ignoreException\".equals(it.getName()))$EXTENSION_STRING_EXTRACTOR",
                        target = "ignoreException"),
                Mapping(expression = "$EXTENSION_ELEM_STREAM.filter(it -> \"saveRequestVariables\".equals(it.getName()))$EXTENSION_STRING_EXTRACTOR",
                        target = "saveRequestVariables"),
                Mapping(expression = "$EXTENSION_ELEM_STREAM.filter(it -> \"saveResponseParameters\".equals(it.getName()))$EXTENSION_STRING_EXTRACTOR",
                        target = "saveResponseParameters"),
                Mapping(expression = "$EXTENSION_ELEM_STREAM.filter(it -> \"resultVariablePrefix\".equals(it.getName()))$EXTENSION_STRING_EXTRACTOR",
                        target = "resultVariablePrefix"),
                Mapping(expression = "$EXTENSION_ELEM_STREAM.filter(it -> \"saveResponseParametersTransient\".equals(it.getName()))$EXTENSION_STRING_EXTRACTOR",
                        target = "saveResponseParametersTransient"),
                Mapping(expression = "$EXTENSION_ELEM_STREAM.filter(it -> \"saveResponseVariableAsJson\".equals(it.getName()))$EXTENSION_STRING_EXTRACTOR",
                        target = "saveResponseVariableAsJson")
        )
        override fun convertToDto(input: BpmnServiceTask): BpmnHttpTask
    }

    @Mapper
    interface ExternalTaskMapper: ServiceTaskMapper<BpmnExternalTask> {
//        ???

        @Mappings(
            Mapping(expression = "$EXTENSION_ELEM_STREAM.filter(it -> \"jobTopic\".equals(it.getName()))$EXTENSION_STRING_EXTRACTOR",
                target = "jobTopic"),
        )
        override fun convertToDto(input: BpmnServiceTask): BpmnExternalTask
    }

    @Mapper
    interface MailMapper: ServiceTaskMapper<BpmnMailTask> {

        @Mappings(
                Mapping(source = "forCompensation", target = "isForCompensation"),
                Mapping(expression = "$EXTENSION_ELEM_STREAM.filter(it -> \"headers\".equals(it.getName()))$EXTENSION_STRING_EXTRACTOR",
                        target = "headers"),
                Mapping(expression = "$EXTENSION_ELEM_STREAM.filter(it -> \"to\".equals(it.getName()))$EXTENSION_STRING_EXTRACTOR",
                        target = "to"),
                Mapping(expression = "$EXTENSION_ELEM_STREAM.filter(it -> \"from\".equals(it.getName()))$EXTENSION_STRING_EXTRACTOR",
                        target = "from"),
                Mapping(expression = "$EXTENSION_ELEM_STREAM.filter(it -> \"subject\".equals(it.getName()))$EXTENSION_STRING_EXTRACTOR",
                        target = "subject"),
                Mapping(expression = "$EXTENSION_ELEM_STREAM.filter(it -> \"cc\".equals(it.getName()))$EXTENSION_STRING_EXTRACTOR",
                        target = "cc"),
                Mapping(expression = "$EXTENSION_ELEM_STREAM.filter(it -> \"bcc\".equals(it.getName()))$EXTENSION_STRING_EXTRACTOR",
                        target = "bcc"),
                Mapping(expression = "$EXTENSION_ELEM_STREAM.filter(it -> \"text\".equals(it.getName()))$EXTENSION_STRING_EXTRACTOR",
                        target = "text"),
                Mapping(expression = "$EXTENSION_ELEM_STREAM.filter(it -> \"html\".equals(it.getName()))$EXTENSION_STRING_EXTRACTOR",
                        target = "html"),
                Mapping(expression = "$EXTENSION_ELEM_STREAM.filter(it -> \"charset\".equals(it.getName()))$EXTENSION_STRING_EXTRACTOR",
                        target = "charset")
        )
        override fun convertToDto(input: BpmnServiceTask): BpmnMailTask
    }

    @Mapper
    interface MuleMapper: ServiceTaskMapper<BpmnMuleTask> {
        @Mappings(
                Mapping(source = "forCompensation", target = "isForCompensation"),
                Mapping(expression = "$EXTENSION_ELEM_STREAM.filter(it -> \"endpointUrl\".equals(it.getName()))$EXTENSION_STRING_EXTRACTOR",
                        target = "endpointUrl"),
                Mapping(expression = "$EXTENSION_ELEM_STREAM.filter(it -> \"language\".equals(it.getName()))$EXTENSION_STRING_EXTRACTOR",
                        target = "language"),
                Mapping(expression = "$EXTENSION_ELEM_STREAM.filter(it -> \"payloadExpression\".equals(it.getName()))$EXTENSION_EXPRESSION_EXTRACTOR",
                        target = "payloadExpression"),
                Mapping(expression = "$EXTENSION_ELEM_STREAM.filter(it -> \"resultVariable\".equals(it.getName()))$EXTENSION_STRING_EXTRACTOR",
                        target = "resultVariableCdata")
        )
        override fun convertToDto(input: BpmnServiceTask): BpmnMuleTask
    }

    @Mapper
    interface DecisionMapper: ServiceTaskMapper<BpmnDecisionTask> {

        @Mappings(
                Mapping(source = "forCompensation", target = "isForCompensation"),
                Mapping(expression = "$EXTENSION_ELEM_STREAM.filter(it -> \"decisionTableReferenceKey\".equals(it.getName()))$EXTENSION_STRING_EXTRACTOR",
                        target = "decisionTableReferenceKey"),
                Mapping(expression = "$EXTENSION_ELEM_STREAM.filter(it -> \"decisionTaskThrowErrorOnNoHits\".equals(it.getName()))$EXTENSION_BOOLEAN_EXTRACTOR",
                        target = "decisionTaskThrowErrorOnNoHits"),
                Mapping(expression = "$EXTENSION_ELEM_STREAM.filter(it -> \"fallbackToDefaultTenant\".equals(it.getName()))$EXTENSION_BOOLEAN_EXTRACTOR",
                        target = "fallbackToDefaultTenantCdata")
        )
        override fun convertToDto(input: BpmnServiceTask): BpmnDecisionTask
    }

    @Mapper
    interface ShellMapper: ServiceTaskMapper<BpmnShellTask> {

        @Mappings(
                Mapping(source = "forCompensation", target = "isForCompensation"),
                Mapping(expression = "$EXTENSION_ELEM_STREAM.filter(it -> \"command\".equals(it.getName()))$EXTENSION_STRING_EXTRACTOR",
                        target = "command"),
                Mapping(expression = "$EXTENSION_ELEM_STREAM.filter(it -> \"arg1\".equals(it.getName()))$EXTENSION_STRING_EXTRACTOR",
                        target = "arg1"),
                Mapping(expression = "$EXTENSION_ELEM_STREAM.filter(it -> \"arg2\".equals(it.getName()))$EXTENSION_STRING_EXTRACTOR",
                        target = "arg2"),
                Mapping(expression = "$EXTENSION_ELEM_STREAM.filter(it -> \"arg3\".equals(it.getName()))$EXTENSION_STRING_EXTRACTOR",
                        target = "arg3"),
                Mapping(expression = "$EXTENSION_ELEM_STREAM.filter(it -> \"arg4\".equals(it.getName()))$EXTENSION_STRING_EXTRACTOR",
                        target = "arg4"),
                Mapping(expression = "$EXTENSION_ELEM_STREAM.filter(it -> \"arg5\".equals(it.getName()))$EXTENSION_STRING_EXTRACTOR",
                        target = "arg5"),
                Mapping(expression = "$EXTENSION_ELEM_STREAM.filter(it -> \"wait\".equals(it.getName()))$EXTENSION_STRING_EXTRACTOR",
                        target = "wait"),
                Mapping(expression = "$EXTENSION_ELEM_STREAM.filter(it -> \"cleanEnv\".equals(it.getName()))$EXTENSION_STRING_EXTRACTOR",
                        target = "cleanEnv"),
                Mapping(expression = "$EXTENSION_ELEM_STREAM.filter(it -> \"errorCodeVariable\".equals(it.getName()))$EXTENSION_STRING_EXTRACTOR",
                        target = "errorCodeVariable"),
                Mapping(expression = "$EXTENSION_ELEM_STREAM.filter(it -> \"outputVariable\".equals(it.getName()))$EXTENSION_STRING_EXTRACTOR",
                        target = "outputVariable"),
                Mapping(expression = "$EXTENSION_ELEM_STREAM.filter(it -> \"directory\".equals(it.getName()))$EXTENSION_STRING_EXTRACTOR",
                        target = "directory")
        )
        override fun convertToDto(input: BpmnServiceTask): BpmnShellTask
    }

    interface ServiceTaskMapper<T> {
        @Mapping(source = "forCompensation", target = "isForCompensation")
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

    interface SubProcessMapper<T> {
        fun convertToDto(input: BpmnSubProcess): T
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