package com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements

/** Common BPMN multi-instance configuration available on activities. */
data class MultiInstanceLoopCharacteristics(
    val isSequential: Boolean? = null,
    val collection: String? = null,
    val elementVariable: String? = null,
    val loopCardinality: String? = null,
    val completionCondition: String? = null,
)
