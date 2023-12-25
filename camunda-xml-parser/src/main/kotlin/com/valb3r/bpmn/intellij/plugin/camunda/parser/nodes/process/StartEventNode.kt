package com.valb3r.bpmn.intellij.plugin.camunda.parser.nodes.process

import com.fasterxml.jackson.annotation.JsonMerge
import com.fasterxml.jackson.annotation.JsonSetter
import com.fasterxml.jackson.annotation.Nulls
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.ExeсutionListener
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.ListenerField
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.events.begin.BpmnStartEvent
import com.valb3r.bpmn.intellij.plugin.camunda.parser.nodes.BpmnMappable
import com.valb3r.bpmn.intellij.plugin.camunda.parser.nodes.process.nested.definitions.*
import com.valb3r.bpmn.intellij.plugin.camunda.parser.nodes.process.nested.formprop.ExecutionListener
import com.valb3r.bpmn.intellij.plugin.camunda.parser.nodes.process.nested.formprop.ExtensionElement
import com.valb3r.bpmn.intellij.plugin.camunda.parser.nodes.process.nested.formprop.FormDataExtensionElement
import com.valb3r.bpmn.intellij.plugin.camunda.parser.nodes.process.nested.formprop.FormFieldMapper
import org.mapstruct.Mapper
import org.mapstruct.factory.Mappers

data class StartEventNode(
    @JacksonXmlProperty(isAttribute = true) val id: String,
    @JacksonXmlProperty(isAttribute = true) val name: String?,
    val documentation: String?,
    @JacksonXmlProperty(isAttribute = true) val formKey: String?,
    @JacksonXmlProperty(isAttribute = true) val asyncBefore: Boolean?,
    @JacksonXmlProperty(isAttribute = true) val asyncAfter: Boolean?,
    @JsonMerge @JacksonXmlElementWrapper(useWrapping = false) val incoming: List<String>?,
    @JsonMerge @JacksonXmlElementWrapper(useWrapping = false) val outgoing: List<String>?,
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

    @Mapper(uses = [BpmnElementIdMapper::class])
    abstract class StartEventNodeMapping {

        private val mapper = Mappers.getMapper(FormFieldMapper::class.java)

        fun convertToDto(input: StartEventNode) : BpmnStartEvent {
            val task = doConvertToDto(input)
            return task.copy(
                formPropertiesExtension = input.extensionElements?.filterIsInstance<FormDataExtensionElement>()
                    ?.flatMap { it.formField ?: emptyList() }
                    ?.map { mapper.mapFormProperty(it) },
                executionListener = input.extensionElements?.filterIsInstance<ExecutionListener>()?.map { ExeсutionListener(it.clazz, it.event, it.fields?.map { ListenerField(it.name, it.string) }) },
                )
        }

        protected abstract fun doConvertToDto(input: StartEventNode) : BpmnStartEvent
    }
}