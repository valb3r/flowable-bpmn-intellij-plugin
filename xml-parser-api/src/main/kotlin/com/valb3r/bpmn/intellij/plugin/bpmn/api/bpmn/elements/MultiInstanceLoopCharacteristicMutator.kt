package com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements

data class MultiInstanceLoopCharacteristics(
    val collection: String?,
    val elementVariable: String?,
)

data class LoopCardinality(
    val type: String?,
    val expression: String?
)

data class CompletionCondition(
    val type: String?,
    val expression: String?
)