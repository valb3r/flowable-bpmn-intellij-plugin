package com.valb3r.bpmn.intellij.plugin.activiti.parser

import com.valb3r.bpmn.intellij.plugin.bpmn.api.info.PropertyType
import org.junit.jupiter.api.Test

internal class PropertyTypeDetailsTest {

    @Test
    fun `Validate that there is no duplicate sourced property type`() {
        val seenPaths = mutableSetOf<PropertyType>()
        ActivitiPropertyTypeDetails.values().forEach {
            if (seenPaths.contains(it.details.propertyType)) {
                throw IllegalStateException("Duplicate source property type ${it.details.propertyType} on ${it.name}")
            }
            seenPaths += it.details.propertyType
        }
    }
}