package com.valb3r.bpmn.intellij.plugin.bpmn.api.info

import org.junit.jupiter.api.Test

internal class PropertyTypeTest {

    @Test
    fun `Validate that there is no duplicate ids or paths`() {
        val seenPaths = mutableSetOf<String>()
        PropertyType.values().forEach {
            if (seenPaths.contains(it.path)) {
                throw IllegalStateException("Duplicate path ${it.path} on ${it.name}")
            }
            seenPaths += it.path
        }
    }
}