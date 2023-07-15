package com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements

import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.BpmnElementId

data class BpmnSequenceFlow(
        override val id: BpmnElementId,
        val name: String? = null,
        val documentation: String? = null,
        val sourceRef: String? = null, // can't be null in reality, but malformed XMLs should be editable too
        val targetRef: String? = null, // can't be null in reality, but malformed XMLs should be editable too
        val conditionExpression: ConditionExpression? = null,
        val executionListener: List<ExeсutionListener>? = null
): WithBpmnId {

    override fun updateBpmnElemId(newId: BpmnElementId): WithBpmnId {
        return copy(id = newId)
    }
}

data class ConditionExpression(
        val type: String? = null, // can't be null in reality, but malformed XMLs should be editable too
        val text: String? // can't be null in reality, but malformed XMLs should be editable too
)