package com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.events.props

data class BpmnConditionalEventDefinition(
    val condition: Condition?
) {
    data class Condition(
        val type: String?,
        val language: String?,
        val script: String?
    )
}