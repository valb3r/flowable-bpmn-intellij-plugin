package com.valb3r.bpmn.intellij.plugin

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.argumentCaptor
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.tasks.BpmnUserTask
import com.valb3r.bpmn.intellij.plugin.bpmn.api.events.EventPropagatableToXml
import com.valb3r.bpmn.intellij.plugin.bpmn.api.info.FunctionalGroupType
import com.valb3r.bpmn.intellij.plugin.bpmn.api.info.PropertyType
import com.valb3r.bpmn.intellij.plugin.core.events.StringValueUpdatedEvent
import com.valb3r.bpmn.intellij.plugin.core.newelements.registerNewElementsFactory
import com.valb3r.bpmn.intellij.plugin.core.properties.propertiesVisualizer
import com.valb3r.bpmn.intellij.plugin.core.tests.BaseUiTest
import com.valb3r.bpmn.intellij.plugin.flowable.parser.FlowableObjectFactory
import org.amshove.kluent.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

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
}