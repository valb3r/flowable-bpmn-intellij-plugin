package com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements

/** Common BPMN multi-instance configuration available on activities. */
data class MultiInstanceLoopCharacteristics(
    val isSequential: Boolean? = null,
    val collection: String? = null,
    val elementVariable: String? = null,
    val loopCardinality: String? = null,
    val completionCondition: String? = null,
    val elementIndexVariable: String? = null,
    val noWaitStatesAsyncLeave: Boolean? = null,
    val loopDataInputRef: String? = null,
    val inputDataItem: String? = null,
    val variableAggregations: List<VariableAggregation>? = null,
)

/** Flowable variable aggregation configuration on a multi-instance activity. */
data class VariableAggregation(
    val target: String? = null,
    val targetExpression: String? = null,
    val delegateExpression: String? = null,
    val clazz: String? = null,
    val createOverviewVariable: Boolean? = null,
    val storeAsTransientVariable: Boolean? = null,
    val definitions: List<VariableAggregationDefinition>? = null,
)

/** Maps one variable from an individual multi-instance execution into an aggregation. */
data class VariableAggregationDefinition(
    val source: String? = null,
    val sourceExpression: String? = null,
    val target: String? = null,
    val targetExpression: String? = null,
)
