package com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.events.props

data class BpmnErrorEventDefinition(
    val errorRef: String?,
    val errorVariableName: String?,
    val errorVariableLocalScope: Boolean?,
    val errorVariableTransient: Boolean?
) {
}