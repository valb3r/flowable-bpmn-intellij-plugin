package com.valb3r.bpmn.intellij.plugin.flowable.parser.nodes.process

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonMerge
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.MultiInstanceLoopCharacteristics as BpmnMultiInstanceLoopCharacteristics
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.VariableAggregation as BpmnVariableAggregation
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.VariableAggregationDefinition as BpmnVariableAggregationDefinition

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
    @JsonProperty("elementIndexVariable")
    @JacksonXmlProperty(localName = "elementIndexVariable", isAttribute = true)
    var elementIndexVariable: String? = null
    @JsonProperty("noWaitStatesAsyncLeave")
    @JacksonXmlProperty(localName = "noWaitStatesAsyncLeave", isAttribute = true)
    var noWaitStatesAsyncLeave: Boolean? = null
    @JsonProperty("loopDataInputRef")
    @JacksonXmlProperty(localName = "loopDataInputRef")
    var loopDataInputRef: String? = null
    @JsonProperty("inputDataItem")
    @JacksonXmlProperty(localName = "inputDataItem")
    var inputDataItem: InputDataItem? = null
    @JsonProperty("extensionElements")
    @JacksonXmlProperty(localName = "extensionElements")
    var extensionElements: MultiInstanceExtensionElements? = null

    fun toElement() = BpmnMultiInstanceLoopCharacteristics(
        isSequential = isSequential,
        collection = collection,
        elementVariable = elementVariable,
        loopCardinality = loopCardinality,
        completionCondition = completionCondition,
        elementIndexVariable = elementIndexVariable,
        noWaitStatesAsyncLeave = noWaitStatesAsyncLeave,
        loopDataInputRef = loopDataInputRef,
        inputDataItem = inputDataItem?.name,
        variableAggregations = extensionElements?.variableAggregations?.map { it.toElement() },
    )

    class Mapping {
        fun convertToDto(input: MultiInstanceLoopCharacteristics?): BpmnMultiInstanceLoopCharacteristics? = input?.toElement()
    }
}

class InputDataItem {
    @JacksonXmlProperty(isAttribute = true)
    var name: String? = null
}

class MultiInstanceExtensionElements {
    @JsonMerge
    @JacksonXmlProperty(localName = "variableAggregation")
    @JacksonXmlElementWrapper(useWrapping = false)
    var variableAggregations: List<VariableAggregation>? = null
}

class VariableAggregation {
    @JacksonXmlProperty(isAttribute = true)
    var target: String? = null
    @JacksonXmlProperty(isAttribute = true)
    var targetExpression: String? = null
    @JacksonXmlProperty(isAttribute = true)
    var delegateExpression: String? = null
    @JacksonXmlProperty(localName = "class", isAttribute = true)
    var clazz: String? = null
    @JacksonXmlProperty(isAttribute = true)
    var createOverviewVariable: Boolean? = null
    @JacksonXmlProperty(isAttribute = true)
    var storeAsTransientVariable: Boolean? = null
    @JsonMerge
    @JacksonXmlProperty(localName = "variable")
    @JacksonXmlElementWrapper(useWrapping = false)
    var definitions: List<VariableAggregationDefinition>? = null

    fun toElement() = BpmnVariableAggregation(
        target = target,
        targetExpression = targetExpression,
        delegateExpression = delegateExpression,
        clazz = clazz,
        createOverviewVariable = createOverviewVariable,
        storeAsTransientVariable = storeAsTransientVariable,
        definitions = definitions?.map { it.toElement() },
    )
}

class VariableAggregationDefinition {
    @JacksonXmlProperty(isAttribute = true)
    var source: String? = null
    @JacksonXmlProperty(isAttribute = true)
    var sourceExpression: String? = null
    @JacksonXmlProperty(isAttribute = true)
    var target: String? = null
    @JacksonXmlProperty(isAttribute = true)
    var targetExpression: String? = null

    fun toElement() = BpmnVariableAggregationDefinition(source, sourceExpression, target, targetExpression)
}
