package com.valb3r.bpmn.intellij.plugin

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.argumentCaptor
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.tasks.BpmnUserTask
import com.valb3r.bpmn.intellij.plugin.bpmn.api.events.EventPropagatableToXml
import com.valb3r.bpmn.intellij.plugin.bpmn.api.info.FunctionalGroupType
import com.valb3r.bpmn.intellij.plugin.bpmn.api.info.PropertyType
import com.valb3r.bpmn.intellij.plugin.camunda.parser.CamundaObjectFactory
import com.valb3r.bpmn.intellij.plugin.core.events.StringValueUpdatedEvent
import com.valb3r.bpmn.intellij.plugin.core.newelements.registerNewElementsFactory
import com.valb3r.bpmn.intellij.plugin.core.properties.propertiesVisualizer
import com.valb3r.bpmn.intellij.plugin.core.tests.BaseUiTest
import com.valb3r.bpmn.intellij.plugin.flowable.parser.FlowableObjectFactory
import org.amshove.kluent.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource

private val FORM_PROPERTY_ID = PropertyType.FORM_PROPERTY_ID.caption

internal class PropertyTest : BaseUiTest() {

    @BeforeEach
    fun `Prepare object factory`() {
        registerNewElementsFactory(project, FlowableObjectFactory())
    }

    @Test
    fun `UserTasks' Form Property can be added and property list is OK`() {
        val task = BpmnUserTask(userTaskBpmnId, "Name user task", formPropertiesExtension = listOf())
        prepareUserTask(task)
        clickOnId(userTaskDiagramId)
        currentVisibleProperties().filterNotNull().filter { it.contains(FORM_PROPERTY_ID) }.shouldBeEmpty()

        this.buttonsConstructed[Pair(userTaskBpmnId, FunctionalGroupType.ADD_FORM_PROPERTY)]!!.doClick()

        argumentCaptor<List<EventPropagatableToXml>>().apply {
            verify(fileCommitter, times(1)).executeCommitAndGetHash(any(), capture(), any(), any())
            lastValue.shouldHaveSize(1)
            val valueUpdated = lastValue.filterIsInstance<StringValueUpdatedEvent>().shouldHaveSingleItem()
            valueUpdated.bpmnElementId.shouldBeEqualTo(userTaskBpmnId)
            valueUpdated.newValue.shouldBeEqualTo("Property 1")
            valueUpdated.propertyIndex!!.shouldContainSame(arrayOf("Property 1"))
        }

        currentVisibleProperties().filterNotNull().filter { it.contains(FORM_PROPERTY_ID) }.shouldHaveSize(1)
    }

    @Test
    fun `UserTasks' Form Property can be removed and property list is OK`() {
        prepareUserTaskView()
        clickOnId(userTaskDiagramId)
        currentVisibleProperties().filterNotNull().filter { it.contains(FORM_PROPERTY_ID) }.shouldHaveSize(1)

        setTextFieldValueInProperties(textFieldsConstructed[Pair(userTaskBpmnId, PropertyType.FORM_PROPERTY_ID)]!!, "")
        propertiesVisualizer(project).clear()

        argumentCaptor<List<EventPropagatableToXml>>().apply {
            verify(fileCommitter, times(1)).executeCommitAndGetHash(any(), capture(), any(), any())
            lastValue.shouldHaveSize(1)
            val valueUpdated = lastValue.filterIsInstance<StringValueUpdatedEvent>().shouldHaveSingleItem()
            valueUpdated.bpmnElementId.shouldBeEqualTo(userTaskBpmnId)
            valueUpdated.newValue.shouldBeEqualTo("")
            valueUpdated.propertyIndex!!.shouldContainSame(arrayOf("Property ID"))
        }

        currentVisibleProperties().filterNotNull().filter { it.contains(FORM_PROPERTY_ID) }.shouldBeEmpty()
    }

    @ParameterizedTest
    @EnumSource(PropertyType::class, names = ["TIME_DATE", "TIME_DURATION", "TIME_CYCLE"])
    fun `TimerStartEvents'(Flowable) adding time,duration,cycle parameters adds proper events and type`(type: PropertyType) {
        prepareTimerStartEventTask()
        clickOnId(timerStartEventDiagramId)

        setTextFieldValueInProperties(textFieldsConstructed[Pair(timerStartEventBpmnId, type)]!!, "2023")
        propertiesVisualizer(project).clear()

        argumentCaptor<List<EventPropagatableToXml>>().apply {
            verify(fileCommitter, times(1)).executeCommitAndGetHash(any(), capture(), any(), any())
            lastValue.shouldHaveSize(1) // For value and its type
            val txtUpdate = lastValue.filterIsInstance<StringValueUpdatedEvent>().shouldHaveSingleItem()
            txtUpdate.property.shouldBeEqualTo(type)
            txtUpdate.newValue.shouldBeEqualTo("2023")
        }
    }

    @ParameterizedTest
    @EnumSource(PropertyType::class, names = ["TIME_DATE", "TIME_DURATION", "TIME_CYCLE"])
    fun `TimerStartEvents'(Camunda) adding time,duration,cycle parameters adds proper events and type`(type: PropertyType) {
        registerNewElementsFactory(project, CamundaObjectFactory())

        prepareTimerStartEventTask()
        clickOnId(timerStartEventDiagramId)

        setTextFieldValueInProperties(textFieldsConstructed[Pair(timerStartEventBpmnId, type)]!!, "2023")
        propertiesVisualizer(project).clear()

        argumentCaptor<List<EventPropagatableToXml>>().apply {
            verify(fileCommitter, times(1)).executeCommitAndGetHash(any(), capture(), any(), any())
            lastValue.shouldHaveSize(2) // For value and its type
            val updates = lastValue.filterIsInstance<StringValueUpdatedEvent>().shouldHaveSize(2)
            val typeUpdate = updates[0]
            val valueUpdate = updates[1]
            typeUpdate.property.shouldBeEqualTo(PropertyType.values().first { it.updatedByWithinSameElement == type })
            typeUpdate.newValue.shouldBeEqualTo("bpmn:tFormalExpression")
            valueUpdate.property.shouldBeEqualTo(type)
            valueUpdate.newValue.shouldBeEqualTo("2023")
        }
    }
}