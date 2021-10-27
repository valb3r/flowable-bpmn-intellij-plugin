package com.valb3r.bpmn.intellij.plugin.activiti.parser

import com.valb3r.bpmn.intellij.plugin.bpmn.api.info.Property
import com.valb3r.bpmn.intellij.plugin.bpmn.api.info.PropertyType
import com.valb3r.bpmn.intellij.plugin.bpmn.parser.core.BaseBpmnObjectFactory

open class ActivitiObjectFactory: BaseBpmnObjectFactory() {

    override fun propertyTypes(): List<PropertyType> {
        return ActivitiPropertyTypeDetails.values().map { it.details.propertyType }
    }

    override fun processDtoToPropertyMap(dto: Any): MutableMap<PropertyType, MutableList<Property>> {
        val result = super.processDtoToPropertyMap(dto)
        result.remove(PropertyType.HEADERS)
        result.remove(PropertyType.IS_USE_LOCAL_SCOPE_FOR_RESULT_VARIABLE)
        result.remove(PropertyType.IS_TRIGGERABLE)
        result.remove(PropertyType.DECISION_TASK_THROW_ERROR_ON_NO_HITS)
        result.remove(PropertyType.FALLBACK_TO_DEF_TENANT)
        result.remove(PropertyType.FALLBACK_TO_DEF_TENANT_CDATA)
        result.remove(PropertyType.FORM_FIELD_VALIDATION)
        result.remove(PropertyType.CATEGORY)
        result.remove(PropertyType.SKIP_EXPRESSION)
        return result
    }
}