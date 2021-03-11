package com.valb3r.bpmn.intellij.plugin.core.settings

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage

fun currentSettingsState(): BpmnPluginSettingsState {
    // This is required to access state this way, because ServiceManagerImpl.getComponentInstance -> ComponentStoreImpl.initComponent are
    // responsible for loading from XML
    return BpmnPluginSettingsState.instance
}

fun currentSettings(): BpmnPluginSettingsState.PluginStateData {
    // This is required to access state this way, because ServiceManagerImpl.getComponentInstance -> ComponentStoreImpl.initComponent are
    // responsible for loading from XML
    return currentSettingsState().pluginState
}

@State(name = "BpmnPluginSettingsState", storages = [Storage("valb3r-bpmn-editor-plugin.xml")]) // fancy XML name to avoid collisions
class BpmnPluginSettingsState: PersistentStateComponent<BpmnPluginSettingsState.PluginStateData> {

    var pluginState: PluginStateData = PluginStateData()

    companion object {
        val instance: BpmnPluginSettingsState
            get() = ServiceManager.getService(BpmnPluginSettingsState::class.java)
    }

    override fun getState(): PluginStateData {
        return pluginState
    }

    override fun loadState(stateBpmnPlugin: PluginStateData) {
        pluginState = stateBpmnPlugin
    }

    class PluginStateData {
        var zoomMin: Float = 0.3f
        var zoomMax: Float = 2.0f
        var zoomFactor: Float = 1.2f
        var keyboardSmallStep = 5.0f
        var keyboardLargeStep = 50.0f
        // UI
        var lineThickness = 2.0f

        var uiFontSize = 10
        var uiFontName = "Courier"

        var dataFontSize = 12
        var dataFontName = "Consolas"

        fun stateEquals(other: PluginStateData): Boolean {
            if (zoomMin != other.zoomMin) return false
            if (zoomMax != other.zoomMax) return false
            if (zoomFactor != other.zoomFactor) return false
            if (keyboardSmallStep != other.keyboardSmallStep) return false
            if (keyboardLargeStep != other.keyboardLargeStep) return false
            if (lineThickness != other.lineThickness) return false
            if (uiFontSize != other.uiFontSize) return false
            if (uiFontName != other.uiFontName) return false

            return true
        }
    }
}
