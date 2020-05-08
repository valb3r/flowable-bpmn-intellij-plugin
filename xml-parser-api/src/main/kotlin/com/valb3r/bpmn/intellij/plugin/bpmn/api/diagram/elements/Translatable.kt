package com.valb3r.bpmn.intellij.plugin.bpmn.api.diagram.elements

interface Translatable<T> {
    fun copyAndTranslate(dx: Float, dy: Float): T
}