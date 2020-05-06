package com.valb3r.bpmn.intellij.plugin.bpmn.api.info

import com.valb3r.bpmn.intellij.plugin.bpmn.api.info.PropertyValueType.*

enum class PropertyType(val id: String, val caption: String, val valueType: PropertyValueType) {

    ID("id", "ID", STRING),
    NAME("name", "Name", STRING),
    DOCUMENTATION("documentation", "Documentation", STRING),
    ASYNC("async", "Asynchronous", BOOLEAN),
    CALLED_ELEM("calledElement", "Called element", EXPRESSION),
    CALLED_ELEM_TYPE("calledElementType", "Called element type", STRING),
    INHERIT_VARS("inheritVariables", "Inherit parent variables", BOOLEAN),
    FALLBACK_TO_DEF_TENANT("fallbackToDefaultTenant", "Fallback to default tenant", BOOLEAN),
    EXCLUSIVE("exclusive", "Exclusive", BOOLEAN),
    DELEGATE_EXPRESSION("delegateExpression", "Delegate expression", EXPRESSION),
    IS_TRIGGERABLE("triggerable", "Is activity triggerable?", BOOLEAN),
    SOURCE_REF("sourceRef","Source reference", STRING),
    TARGET_REF("targetRef", "Target reference", STRING),
    CONDITION_EXPR_VALUE("conditionExpression.text", "Condition expression", EXPRESSION),
    CONDITION_EXPR_TYPE("conditionExpression.type", "Condition expression type", STRING),
    DEFAULT_FLOW("default", "Default flow element", STRING)
}