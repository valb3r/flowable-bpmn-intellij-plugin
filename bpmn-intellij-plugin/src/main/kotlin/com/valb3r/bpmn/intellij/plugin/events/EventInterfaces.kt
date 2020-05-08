package com.valb3r.bpmn.intellij.plugin.events

import com.valb3r.bpmn.intellij.plugin.bpmn.api.info.PropertyType

interface Event

interface UpdateWithId: Event {
    val elementId: String
    val property: PropertyType
}