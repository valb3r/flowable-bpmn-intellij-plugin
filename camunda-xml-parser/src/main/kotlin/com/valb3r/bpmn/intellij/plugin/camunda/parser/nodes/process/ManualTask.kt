package com.valb3r.bpmn.intellij.plugin.camunda.parser.nodes.process

import com.fasterxml.jackson.annotation.JsonMerge
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.ExeсutionListener
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.ListenerField
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.tasks.BpmnManualTask
import com.valb3r.bpmn.intellij.plugin.camunda.parser.nodes.BpmnMappable
import com.valb3r.bpmn.intellij.plugin.camunda.parser.nodes.process.nested.formprop.ExecutionListener
import com.valb3r.bpmn.intellij.plugin.camunda.parser.nodes.process.nested.formprop.ExtensionElement
import org.mapstruct.Mapper
import org.mapstruct.Mapping
import org.mapstruct.factory.Mappers

data class ManualTask(
    @JacksonXmlProperty(isAttribute = true) val id: String,
    @JacksonXmlProperty(isAttribute = true) val name: String?,
    @JacksonXmlProperty(isAttribute = true) val documentation: String?,
    @JacksonXmlProperty(isAttribute = true) val asyncBefore: Boolean?,
    @JacksonXmlProperty(isAttribute = true) val asyncAfter: Boolean?,
    @JacksonXmlProperty(isAttribute = true) val isForCompensation: Boolean?,
    @JsonMerge @JacksonXmlElementWrapper(useWrapping = false) val incoming: List<String>?,
    @JsonMerge @JacksonXmlElementWrapper(useWrapping = false) val outgoing: List<String>?,
    @JsonMerge @JacksonXmlElementWrapper(useWrapping = true) val extensionElements: List<ExtensionElement>? = null
): BpmnMappable<BpmnManualTask> {

    override fun toElement(): BpmnManualTask {
        return Mappers.getMapper(ManualTaskMapping::class.java).convertToDto(this)
    }

    @Mapper(uses = [BpmnElementIdMapper::class])
    abstract class ManualTaskMapping {

        @Mapping(source = "forCompensation", target = "isForCompensation")
        protected abstract fun doConvertToDto(input: ManualTask) : BpmnManualTask

        fun convertToDto(input: ManualTask) : BpmnManualTask {
            val task = doConvertToDto(input)
            return task.copy(
                executionListener = input.extensionElements?.filterIsInstance<ExecutionListener>()?.map { ExeсutionListener(it.clazz, it.event, it.fields?.map { ListenerField(it.name, it.string) }) },
            )
        }
    }
}