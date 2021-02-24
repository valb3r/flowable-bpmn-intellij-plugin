package com.valb3r.bpmn.intellij.plugin.core.render.uieventbus

import com.valb3r.bpmn.intellij.plugin.core.render.Camera

data class CameraChangeEvent(val camera: Camera): UiEvent
