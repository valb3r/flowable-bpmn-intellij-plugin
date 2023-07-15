package com.valb3r.bpmn.intellij.plugin.activiti.parser.nodes.process

import com.fasterxml.jackson.annotation.JsonMerge
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty
import com.valb3r.bpmn.intellij.plugin.activiti.parser.nodes.BpmnMappable
import com.valb3r.bpmn.intellij.plugin.activiti.parser.nodes.process.nested.formprop.ExtensionElement
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.activities.BpmnCallActivity
import org.mapstruct.Mapper
import org.mapstruct.factory.Mappers

data class CallActivity(
        @JacksonXmlProperty(isAttribute = true) val id: String,
        @JacksonXmlProperty(isAttribute = true) val name: String?,
        @JacksonXmlProperty(isAttribute = true) val documentation: String?,
        @JacksonXmlProperty(isAttribute = true) val async: Boolean?,
        @JacksonXmlProperty(isAttribute = true) val calledElement: String?,
        @JacksonXmlProperty(isAttribute = true) val calledElementType: String?,
        @JacksonXmlProperty(isAttribute = true) val inheritVariables: Boolean?,
        @JacksonXmlProperty(isAttribute = true) val fallbackToDefaultTenant: Boolean?,
        @JsonMerge @JacksonXmlElementWrapper(useWrapping = true) val extensionElements: List<ExtensionElement>? = null
): BpmnMappable<BpmnCallActivity> {

    override fun toElement(): BpmnCallActivity {
        return Mappers.getMapper(Mapping::class.java).convertToDto(this)
    }

    @Mapper(uses = [BpmnElementIdMapper::class])
    interface Mapping {
        fun convertToDto(input: CallActivity) : BpmnCallActivity
    }
}