package com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements

import com.github.pozo.KotlinBuilder
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.BpmnElementId

@KotlinBuilder
data class BpmnSequenceFlow(
        override val id: BpmnElementId,
        val name: String?,
        val documentation: String?,
        val sourceRef: String?, // can't be null in reality, but malformed XMLs should be editable too
        val targetRef: String?, // can't be null in reality, but malformed XMLs should be editable too
        val conditionExpression: ConditionExpression?
): WithBpmnId

@KotlinBuilder
data class ConditionExpression(
        val type: String?, // can't be null in reality, but malformed XMLs should be editable too
        val text: String? // can't be null in reality, but malformed XMLs should be editable too
)