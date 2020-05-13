package com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements

import com.github.pozo.KotlinBuilder
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.BpmnElementId

@KotlinBuilder
data class BpmnSequenceFlow(
        override val id: BpmnElementId,
        val name: String?,
        val documentation: String?,
        val sourceRef: String,
        val targetRef: String,
        val conditionExpression: ConditionExpression?
): WithBpmnId

@KotlinBuilder
data class ConditionExpression(
        val type: String,
        val text: String
)