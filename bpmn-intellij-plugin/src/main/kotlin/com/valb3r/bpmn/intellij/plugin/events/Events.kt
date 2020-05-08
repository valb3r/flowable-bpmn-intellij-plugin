package com.valb3r.bpmn.intellij.plugin.events

import com.valb3r.bpmn.intellij.plugin.bpmn.api.info.PropertyType

data class StringValueUpdatedEvent(override val elementId: String, override val property: PropertyType, val newValue: String): UpdateWithId

data class BooleanValueUpdatedEvent(override val elementId: String, override val property: PropertyType, val newValue: Boolean): UpdateWithId

data class CommittedToFile(val eventCount: Int): Event