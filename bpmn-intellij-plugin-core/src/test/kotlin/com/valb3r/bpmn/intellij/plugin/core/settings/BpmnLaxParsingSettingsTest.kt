package com.valb3r.bpmn.intellij.plugin.core.settings

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class BpmnLaxParsingSettingsTest {

    @Test
    fun `lax parsing is enabled by default and included in settings copies`() {
        val defaults = BaseBpmnPluginSettingsState.PluginStateData()

        assertTrue(defaults.enableLaxParsing)
        assertTrue(defaults.stateEquals(defaults.copy()))

        val disabled = defaults.copy().apply { enableLaxParsing = false }
        assertFalse(defaults.stateEquals(disabled))
        assertFalse(disabled.copy().enableLaxParsing)
    }
}
