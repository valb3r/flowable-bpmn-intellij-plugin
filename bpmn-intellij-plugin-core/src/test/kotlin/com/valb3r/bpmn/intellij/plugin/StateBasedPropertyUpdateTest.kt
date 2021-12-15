package com.valb3r.bpmn.intellij.plugin

import com.valb3r.bpmn.intellij.plugin.bpmn.api.info.Property
import com.valb3r.bpmn.intellij.plugin.bpmn.api.info.PropertyType
import com.valb3r.bpmn.intellij.plugin.core.events.IndexUiOnlyValueUpdatedEvent
import com.valb3r.bpmn.intellij.plugin.core.events.StringValueUpdatedEvent
import com.valb3r.bpmn.intellij.plugin.core.events.updateEventsRegistry
import com.valb3r.bpmn.intellij.plugin.core.newelements.registerNewElementsFactory
import com.valb3r.bpmn.intellij.plugin.core.state.currentStateProvider
import com.valb3r.bpmn.intellij.plugin.core.tests.BaseUiTest
import com.valb3r.bpmn.intellij.plugin.flowable.parser.FlowableObjectFactory
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class StateBasedPropertyUpdateTest: BaseUiTest() {

    @BeforeEach
    fun `Prepare object factory`() {
        registerNewElementsFactory(project, FlowableObjectFactory())
    }

    @Test
    fun `Group based update event updates state properly`() {
        prepareTwoServiceTaskView()
        currentStateProvider(project).currentState().elemPropertiesByStaticElementId[serviceTaskStartBpmnId]!![PropertyType.FIELD_NAME]
            ?.value?.shouldBeNull()

        updateEventsRegistry(project).addPropertyUpdateEvent(StringValueUpdatedEvent(serviceTaskStartBpmnId, PropertyType.FIELD_NAME, "1", propertyIndex = null))
        currentStateProvider(project).currentState().elemPropertiesByStaticElementId[serviceTaskStartBpmnId]!![PropertyType.FIELD_NAME]
            ?.shouldBeEqualTo(Property("1", null))

        updateEventsRegistry(project).addPropertyUpdateEvent(StringValueUpdatedEvent(serviceTaskStartBpmnId, PropertyType.FIELD_NAME, "", propertyIndex = null))
        currentStateProvider(project).currentState().elemPropertiesByStaticElementId[serviceTaskStartBpmnId]!![PropertyType.FIELD_NAME]
            ?.shouldBeEqualTo(Property("", null))
    }

    @Test
    fun `Group based multiple update event updates state properly`() {
        prepareTwoServiceTaskView()
        currentStateProvider(project).currentState().elemPropertiesByStaticElementId[serviceTaskStartBpmnId]!![PropertyType.FIELD_NAME]
            ?.value?.shouldBeNull()

        updateEventsRegistry(project).addPropertyUpdateEvent(StringValueUpdatedEvent(serviceTaskStartBpmnId, PropertyType.FIELD_EXPRESSION, "expression 1", propertyIndex = null))
        updateEventsRegistry(project).addPropertyUpdateEvent(StringValueUpdatedEvent(serviceTaskStartBpmnId, PropertyType.FIELD_NAME, "new name", propertyIndex = null))
        updateEventsRegistry(project).addEvents(listOf(IndexUiOnlyValueUpdatedEvent(serviceTaskStartBpmnId, PropertyType.FIELD_EXPRESSION, listOf(), listOf("new name"))))
        currentStateProvider(project).currentState().elemPropertiesByStaticElementId[serviceTaskStartBpmnId]!![PropertyType.FIELD_NAME]
            ?.shouldBeEqualTo(Property("new name", null))
        currentStateProvider(project).currentState().elemPropertiesByStaticElementId[serviceTaskStartBpmnId]!![PropertyType.FIELD_EXPRESSION]
            ?.shouldBeEqualTo(Property("expression 1", listOf("new name")))
    }
}
