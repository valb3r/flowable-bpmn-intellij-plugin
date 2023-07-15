package com.valb3r.bpmn.intellij.plugin.activiti.parser.nodes.process

import com.fasterxml.jackson.annotation.JsonMerge
import com.fasterxml.jackson.annotation.JsonSetter
import com.fasterxml.jackson.annotation.Nulls
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty
import com.valb3r.bpmn.intellij.plugin.activiti.parser.nodes.BpmnMappable
import com.valb3r.bpmn.intellij.plugin.activiti.parser.nodes.process.nested.definitions.ConditionalEventDefinition
import com.valb3r.bpmn.intellij.plugin.activiti.parser.nodes.process.nested.definitions.MessageEventDefinition
import com.valb3r.bpmn.intellij.plugin.activiti.parser.nodes.process.nested.definitions.TimerEventDefinition
import com.valb3r.bpmn.intellij.plugin.activiti.parser.nodes.process.nested.formprop.ExtensionElement
import com.valb3r.bpmn.intellij.plugin.activiti.parser.nodes.process.nested.formprop.FormProperty
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.ExtensionFormProperty
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.events.begin.BpmnStartEvent
import org.mapstruct.Mapper
import org.mapstruct.factory.Mappers

data class StartEventNode(
    @JacksonXmlProperty(isAttribute = true) val id: String,
    @JacksonXmlProperty(isAttribute = true) val name: String?,
    val documentation: String?,
    @JsonSetter(nulls = Nulls.AS_EMPTY) val timerEventDefinition: TimerEventDefinition?,
    @JsonSetter(nulls = Nulls.AS_EMPTY) val signalEventDefinition: SignalEventDefinition?,
    @JsonSetter(nulls = Nulls.AS_EMPTY) val messageEventDefinition: MessageEventDefinition?,
    @JsonSetter(nulls = Nulls.AS_EMPTY) val errorEventDefinition: ErrorEventDefinition?,
    @JsonSetter(nulls = Nulls.AS_EMPTY) val escalationEventDefinition: EscalationEventDefinition?,
    @JsonSetter(nulls = Nulls.AS_EMPTY) val conditionalEventDefinition: ConditionalEventDefinition?,
    @JsonMerge @JacksonXmlElementWrapper(useWrapping = true) val extensionElements: List<ExtensionElement>? = null
): BpmnMappable<BpmnStartEvent> {

    override fun toElement(): BpmnStartEvent {
        return Mappers.getMapper(StartEventNodeMapping::class.java).convertToDto(this)
    }

    data class SignalEventDefinition(
            val signalRef: String? = null
    )

    data class ErrorEventDefinition(
            val errorRef: String? = null
    )

    data class EscalationEventDefinition(
            val escalationRef: String? = null
    )


    @Mapper(uses = [BpmnElementIdMapper::class])
    abstract class StartEventNodeMapping {

        fun convertToDto(input: StartEventNode) : BpmnStartEvent {
            val task = doConvertToDto(input)
            return task.copy(
                formPropertiesExtension = input.extensionElements?.filterIsInstance<FormProperty>()?.map { mapFormProperty(it) }
            )
        }

        protected abstract fun doConvertToDto(input: StartEventNode) : BpmnStartEvent

        protected abstract fun mapFormProperty(input: FormProperty) : ExtensionFormProperty
    }
}