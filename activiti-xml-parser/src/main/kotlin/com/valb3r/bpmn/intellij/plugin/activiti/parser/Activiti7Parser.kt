package com.valb3r.bpmn.intellij.plugin.activiti.parser

import com.valb3r.bpmn.intellij.plugin.bpmn.api.info.PropertyType
import com.valb3r.bpmn.intellij.plugin.bpmn.parser.core.NS
import com.valb3r.bpmn.intellij.plugin.bpmn.parser.core.PropertyTypeDetails
import com.valb3r.bpmn.intellij.plugin.bpmn.parser.core.XmlType

enum class Activiti7PropertyTypeDetails(val details: PropertyTypeDetails) {
    ID(PropertyTypeDetails(PropertyType.ID, "id", XmlType.ATTRIBUTE)),
    NAME(PropertyTypeDetails(PropertyType.NAME,"name", XmlType.ATTRIBUTE)),
    DOCUMENTATION(PropertyTypeDetails(PropertyType.DOCUMENTATION, "documentation.text", XmlType.CDATA, forceFirst = true)),
    ASYNC(PropertyTypeDetails(PropertyType.ASYNC, "activiti:async", XmlType.ATTRIBUTE)),
    ASSIGNEE(PropertyTypeDetails(PropertyType.ASSIGNEE, "activiti:assignee", XmlType.ATTRIBUTE)),
    CANDIDATE_USERS(PropertyTypeDetails(PropertyType.CANDIDATE_USERS, "activiti:candidateUsers", XmlType.ATTRIBUTE)),
    CANDIDATE_GROUPS(PropertyTypeDetails(PropertyType.CANDIDATE_GROUPS, "activiti:candidateGroups", XmlType.ATTRIBUTE)),
    CALLED_ELEM(PropertyTypeDetails(PropertyType.CALLED_ELEM, "calledElement", XmlType.ATTRIBUTE)),
    CALLED_ELEM_TYPE(PropertyTypeDetails(PropertyType.CALLED_ELEM_TYPE, "activiti:calledElementType", XmlType.ATTRIBUTE)),
    INHERIT_VARS(PropertyTypeDetails(PropertyType.INHERIT_VARS, "activiti:inheritVariables", XmlType.ATTRIBUTE)),
    FALLBACK_TO_DEF_TENANT(PropertyTypeDetails(PropertyType.FALLBACK_TO_DEF_TENANT, "activiti:fallbackToDefaultTenant", XmlType.ATTRIBUTE)),
    EXCLUSIVE(PropertyTypeDetails(PropertyType.EXCLUSIVE,"activiti:exclusive", XmlType.ATTRIBUTE)),
    EXPRESSION(PropertyTypeDetails(PropertyType.EXPRESSION, "activiti:expression", XmlType.ATTRIBUTE)),
    DELEGATE_EXPRESSION(PropertyTypeDetails(PropertyType.DELEGATE_EXPRESSION, "activiti:delegateExpression", XmlType.ATTRIBUTE)),
    CLASS(PropertyTypeDetails(PropertyType.CLASS, "activiti:class", XmlType.ATTRIBUTE)),
    DUE_DATE(PropertyTypeDetails(PropertyType.DUE_DATE, "activiti:dueDate", XmlType.ATTRIBUTE)),
    FORM_KEY(PropertyTypeDetails(PropertyType.FORM_KEY, "activiti:formKey", XmlType.ATTRIBUTE)),
    PRIORITY(PropertyTypeDetails(PropertyType.PRIORITY, "activiti:priority", XmlType.ATTRIBUTE)),
    SCRIPT(PropertyTypeDetails(PropertyType.SCRIPT, "script.text", XmlType.CDATA)),
    SCRIPT_FORMAT(PropertyTypeDetails(PropertyType.SCRIPT_FORMAT, "scriptFormat", XmlType.ATTRIBUTE)),
    RULES(PropertyTypeDetails(PropertyType.RULES, "activiti:rules", XmlType.ATTRIBUTE)),
    RESULT_VARIABLE(PropertyTypeDetails(PropertyType.RESULT_VARIABLE, "activiti:resultVariable", XmlType.ATTRIBUTE)),
    RESULT_VARIABLE_NAME(PropertyTypeDetails(PropertyType.RESULT_VARIABLE_NAME, "activiti:resultVariableName", XmlType.ATTRIBUTE)),
    EXCLUDE(PropertyTypeDetails(PropertyType.EXCLUDE, "activiti:exclude", XmlType.ATTRIBUTE)),
    SOURCE_REF(PropertyTypeDetails(PropertyType.SOURCE_REF,"sourceRef", XmlType.ATTRIBUTE)),
    TARGET_REF(PropertyTypeDetails(PropertyType.TARGET_REF, "targetRef", XmlType.ATTRIBUTE)),
    ATTACHED_TO_REF(PropertyTypeDetails(PropertyType.ATTACHED_TO_REF, "attachedToRef", XmlType.ATTRIBUTE)),
    CONDITION_EXPR_VALUE(PropertyTypeDetails(PropertyType.CONDITION_EXPR_VALUE, "bpmn2:conditionExpression.text", XmlType.CDATA)),
    CONDITION_EXPR_TYPE(PropertyTypeDetails(PropertyType.CONDITION_EXPR_TYPE, "bpmn2:conditionExpression.xsi:type", XmlType.ATTRIBUTE)),
    TIME_DATE(PropertyTypeDetails(PropertyType.TIME_DATE, "timerEventDefinition.timeDate.timeDate", XmlType.CDATA)),
    // TODO: Validate:
    TIME_DATE_EXPRESSION_TYPE(PropertyTypeDetails(PropertyType.TIME_DATE_EXPRESSION_TYPE, "timerEventDefinition.timeDate.type", XmlType.ATTRIBUTE)),
    TIME_DURATION(PropertyTypeDetails(PropertyType.TIME_DURATION, "timerEventDefinition.timeDuration.timeDuration", XmlType.CDATA)),
    TIME_DURATION_EXPRESSION_TYPE(PropertyTypeDetails(PropertyType.TIME_DURATION_EXPRESSION_TYPE, "timerEventDefinition.timeDuration.type", XmlType.ATTRIBUTE)),
    TIME_CYCLE(PropertyTypeDetails(PropertyType.TIME_CYCLE, "timerEventDefinition.timeCycle.timeCycle", XmlType.CDATA)),
    TIME_CYCLE_EXPRESSION_TYPE(PropertyTypeDetails(PropertyType.TIME_CYCLE_EXPRESSION_TYPE, "timerEventDefinition.timeCycle.type", XmlType.ATTRIBUTE)),
    // END TODO
    COMPLETION_CONDITION(PropertyTypeDetails(PropertyType.COMPLETION_CONDITION, "bpmn2:completionCondition.text", XmlType.CDATA)),
    DEFAULT_FLOW(PropertyTypeDetails(PropertyType.DEFAULT_FLOW, "default", XmlType.ATTRIBUTE))
}

class Activiti7Parser : ActivitiParser() {

    override fun propertyTypeDetails(): List<PropertyTypeDetails> {
        return Activiti7PropertyTypeDetails.values().map { it.details }
    }

    override fun modelNs(): NS {
        return NS("bpmn2", "http://www.omg.org/spec/BPMN/20100524/MODEL")
    }

    override fun bpmndiNs(): NS {
        return NS("bpmndi", "http://www.omg.org/spec/BPMN/20100524/DI")
    }

    override fun omgdcNs(): NS {
        return NS("dc", "http://www.omg.org/spec/DD/20100524/DC")
    }

    override fun omgdiNs(): NS {
        return NS("omgdi", "http://www.omg.org/spec/DD/20100524/DI")
    }

    override fun xsiNs(): NS {
        return NS("xsi", "http://www.w3.org/2001/XMLSchema-instance")
    }

    override fun engineNs(): NS {
        return NS("activiti", "http://activiti.org/bpmn")
    }
}
