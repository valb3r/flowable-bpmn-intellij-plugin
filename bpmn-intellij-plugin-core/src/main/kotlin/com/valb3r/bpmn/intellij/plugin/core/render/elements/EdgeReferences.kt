package com.valb3r.bpmn.intellij.plugin.core.render.elements

import com.valb3r.bpmn.intellij.plugin.bpmn.api.PropertyTable
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.BpmnElementId
import com.valb3r.bpmn.intellij.plugin.bpmn.api.info.PropertyType

fun hasSelfReference(
    propertiesByElementId: Map<BpmnElementId, PropertyTable>,
    elementId: BpmnElementId,
    excludingEdgeId: BpmnElementId? = null
): Boolean {
    return propertiesByElementId.any { (edgeId, properties) ->
        edgeId != excludingEdgeId &&
            properties[PropertyType.SOURCE_REF]?.value == elementId.id &&
            properties[PropertyType.TARGET_REF]?.value == elementId.id
    }
}
