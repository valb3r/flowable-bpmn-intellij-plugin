package com.valb3r.bpmn.intellij.plugin.render.elements

import com.valb3r.bpmn.intellij.plugin.render.IconProvider
import com.valb3r.bpmn.intellij.plugin.render.RenderContext
import com.valb3r.bpmn.intellij.plugin.state.CurrentState

data class RenderState(val currentState: CurrentState, val ctx: RenderContext, val icons: IconProvider)