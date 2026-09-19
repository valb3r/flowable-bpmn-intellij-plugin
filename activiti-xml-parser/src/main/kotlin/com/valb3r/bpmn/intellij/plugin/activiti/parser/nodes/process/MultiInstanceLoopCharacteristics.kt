package com.valb3r.bpmn.intellij.plugin.activiti.parser.nodes.process

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.MultiInstanceLoopCharacteristics as BpmnMultiInstanceLoopCharacteristics

class MultiInstanceLoopCharacteristics {
    @JsonProperty("isSequential")
    @JacksonXmlProperty(localName = "isSequential", isAttribute = true)
    var isSequential: Boolean? = null
    @JsonProperty("collection")
    @JacksonXmlProperty(localName = "collection", isAttribute = true)
    var collection: String? = null
    @JsonProperty("elementVariable")
    @JacksonXmlProperty(localName = "elementVariable", isAttribute = true)
    var elementVariable: String? = null
    @JsonProperty("loopCardinality")
    @JacksonXmlProperty(localName = "loopCardinality")
    var loopCardinality: String? = null
    @JsonProperty("completionCondition")
    @JacksonXmlProperty(localName = "completionCondition")
    var completionCondition: String? = null

    fun toElement() = BpmnMultiInstanceLoopCharacteristics(
        isSequential, collection, elementVariable, loopCardinality, completionCondition
    )

    class Mapping {
        fun convertToDto(input: MultiInstanceLoopCharacteristics): BpmnMultiInstanceLoopCharacteristics = input.toElement()
    }
}
