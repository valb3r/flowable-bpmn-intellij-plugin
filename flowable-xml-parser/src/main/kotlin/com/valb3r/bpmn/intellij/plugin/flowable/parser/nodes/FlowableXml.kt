package com.valb3r.bpmn.intellij.plugin.flowable.parser.nodes

import com.fasterxml.jackson.annotation.JsonMerge
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.BpmnProcess
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

// For mixed lists in XML we need to have JsonSetter/JsonMerge on field
// https://github.com/FasterXML/jackson-dataformat-xml/issues/363
// unfortunately this has failed with Kotlin 'data' classes
class ProcessNode: BpmnMappable<BpmnProcess> {

    @JacksonXmlProperty(isAttribute = true) var id: String? = null // it is false - it is non-null
    @JacksonXmlProperty(isAttribute = true) var name: String? = null // it is false - it is non-null
    var documentation: String? = null
    @JacksonXmlProperty(isAttribute = true) var isExecutable: Boolean? = null

    @JsonMerge @JacksonXmlElementWrapper(useWrapping = false) var startEvent: List<StartEventNode>? = null  // need to validate how there can be multiple, and - it is non-null
    @JsonMerge @JacksonXmlElementWrapper(useWrapping = false) val endEvent: List<EndEventNode>? = null  // need to validate how there can be multiple, and - it is non-null
    @JsonMerge @JacksonXmlElementWrapper(useWrapping = false) var callActivity: List<CallActivity>? = null
    @JsonMerge @JacksonXmlElementWrapper(useWrapping = false) var userTask: List<UserTask>? = null
    @JsonMerge @JacksonXmlElementWrapper(useWrapping = false) var scriptTask: List<ScriptTask>? = null
    @JsonMerge @JacksonXmlElementWrapper(useWrapping = false) var serviceTask: List<ServiceTask>? = null
    @JsonMerge @JacksonXmlElementWrapper(useWrapping = false) var businessRuleTask: List<BusinessRuleTask>? = null
    @JsonMerge @JacksonXmlElementWrapper(useWrapping = false) var receiveTask: List<ReceiveTask>? = null
    @JsonMerge @JacksonXmlElementWrapper(useWrapping = false) var subProcess: List<SubProcess>? = null
    @JsonMerge @JacksonXmlElementWrapper(useWrapping = false) var transaction: List<Transaction>? = null
    @JsonMerge @JacksonXmlElementWrapper(useWrapping = false) var adHocSubProcess: List<AdHocSubProcess>? = null
    @JsonMerge @JacksonXmlElementWrapper(useWrapping = false) var sequenceFlow: List<SequenceFlow>? = null
    @JsonMerge @JacksonXmlElementWrapper(useWrapping = false) var exclusiveGateway: List<ExclusiveGateway>? = null

    override fun toElement(): BpmnProcess {
        var result = Mappers.getMapper(Mapping::class.java).convertToDto(this)
        result = applyServiceTaskCustomizationByType(result)
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

    private fun <T> extractTasksBasedOnType(process: BpmnProcess, type: String, mapper: ServiceTaskMapper<T>, update: (List<T>?, BpmnProcess) -> BpmnProcess): BpmnProcess {
        process.serviceTask?.apply {
            val byTypeGroup = this.groupBy { it.type == type }
            val newProcess = process.copy(serviceTask = byTypeGroup[false])
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