package com.valb3r.bpmn.intellij.plugin.flowable.parser

import com.valb3r.bpmn.intellij.plugin.bpmn.api.info.PropertyType
import org.junit.jupiter.api.Test

internal class PropertyTypeDetailsTest {

    @Test
    fun `Validate that there is no duplicate sourced property type`() {
        val seenPaths = mutableSetOf<PropertyType>()
        PropertyTypeDetails.values().forEach {
            if (seenPaths.contains(it.propertyType)) {
                throw IllegalStateException("Duplicate source property type ${it.propertyType} on ${it.name}")
            }
            seenPaths += it.propertyType
        }
    }
}