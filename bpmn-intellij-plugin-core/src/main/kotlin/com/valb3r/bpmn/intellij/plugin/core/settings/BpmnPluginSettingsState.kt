package com.valb3r.bpmn.intellij.plugin.core.settings

import com.intellij.openapi.components.PersistentStateComponent
import java.util.concurrent.atomic.AtomicReference

val currentSettingsStateProvider = AtomicReference<() -> BaseBpmnPluginSettingsState>() // Not map as is global

fun currentSettingsState(): BaseBpmnPluginSettingsState {
    // This is required to access state this way, because ServiceManagerImpl.getComponentInstance -> ComponentStoreImpl.initComponent are
    // responsible for loading from XML
    return currentSettingsStateProvider.get()()
}

fun currentSettings(): BaseBpmnPluginSettingsState.PluginStateData {
    // This is required to access state this way, because ServiceManagerImpl.getComponentInstance -> ComponentStoreImpl.initComponent are
    // responsible for loading from XML
    return currentSettingsState().pluginState
}

// Due to class name collision each plugin implementation should reference its own class
abstract class BaseBpmnPluginSettingsState: PersistentStateComponent<BaseBpmnPluginSettingsState.PluginStateData> {

    var pluginState: PluginStateData = PluginStateData()

    override fun getState(): PluginStateData {
        return pluginState
    }

    override fun loadState(stateBpmnPlugin: PluginStateData) {
        pluginState = stateBpmnPlugin
    }

    // TODO investigate possibility to use data class
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
        var allowOpeningBpmnExtension = false

        fun stateEquals(other: PluginStateData): Boolean {
            if (zoomMin != other.zoomMin) return false
            if (zoomMax != other.zoomMax) return false
            if (zoomFactor != other.zoomFactor) return false
            if (keyboardSmallStep != other.keyboardSmallStep) return false
            if (keyboardLargeStep != other.keyboardLargeStep) return false
            if (lineThickness != other.lineThickness) return false
            if (uiFontSize != other.uiFontSize) return false
            if (uiFontName != other.uiFontName) return false
            if (dataFontSize != other.dataFontSize) return false
            if (dataFontName != other.dataFontName) return false
            if (allowOpeningBpmnExtension != other.allowOpeningBpmnExtension) return false

            return true
        }

        fun copy(): PluginStateData {
            val data = PluginStateData()
            data.zoomMin = zoomMin
            data.zoomMax = zoomMax
            data.zoomFactor = zoomFactor
            data.keyboardSmallStep = keyboardSmallStep
            data.keyboardLargeStep = keyboardLargeStep
            data.lineThickness = lineThickness
            data.uiFontSize = uiFontSize
            data.uiFontName = uiFontName
            data.dataFontSize = dataFontSize
            data.dataFontName = dataFontName
            data.allowOpeningBpmnExtension = allowOpeningBpmnExtension
            return data
        }
    }
}
