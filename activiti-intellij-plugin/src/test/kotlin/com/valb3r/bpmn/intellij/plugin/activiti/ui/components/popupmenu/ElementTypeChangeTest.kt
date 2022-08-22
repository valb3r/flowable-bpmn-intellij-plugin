package com.valb3r.bpmn.intellij.plugin.activiti.ui.components.popupmenu

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.argumentCaptor
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.tasks.BpmnUserTask
import com.valb3r.bpmn.intellij.plugin.bpmn.api.events.EventPropagatableToXml
import com.valb3r.bpmn.intellij.plugin.bpmn.api.info.PropertyType.*
import com.valb3r.bpmn.intellij.plugin.core.events.BpmnElementTypeChangeEvent
import com.valb3r.bpmn.intellij.plugin.core.popupmenu.SHAPE_CHANGE_POPUP_MENU
import com.valb3r.bpmn.intellij.plugin.core.state.currentStateProvider
import org.amshove.kluent.*
import org.junit.jupiter.api.Test

class ElementTypeChangeTest: ActivitiBaseUiTest() {

    @Test
    fun `Service task shape changes to User task`() {
        prepareTwoServiceTaskView()
        clickOnId(serviceTaskStartDiagramId)
        val serviceTaskTypeChange = findExactlyOneTypeChangeElem()
        clickOnId(serviceTaskTypeChange!!)

        verify(popupMenuProvider).popupChangeShapeType(serviceTaskStartBpmnId)
        popupsConstructed.shouldHaveKey(SHAPE_CHANGE_POPUP_MENU)
        popupItemsConstructed["User Task"]!!.actionListeners[0].actionPerformed(mock())

        clickOnId(serviceTaskStartDiagramId) // Now it should be user task
        currentStateProvider(project).currentState().elementByBpmnId[serviceTaskStartBpmnId]!!.element.shouldBeInstanceOf<BpmnUserTask>()
        val props = currentStateProvider(project).currentState().elemPropertiesByStaticElementId[serviceTaskStartBpmnId]!!
        props[NAME]!!.value.shouldBe("Start service task")
        props[DOCUMENTATION]!!.value.shouldBe("Start service task docs")
        val userTaskProps = listOf(
            ID, NAME, DOCUMENTATION, IS_FOR_COMPENSATION,
            ASYNC, ASSIGNEE, CANDIDATE_USERS, CANDIDATE_GROUPS,
            DUE_DATE, FORM_KEY, PRIORITY, FORM_PROPERTY_ID,
            FORM_PROPERTY_NAME, FORM_PROPERTY_TYPE, FORM_PROPERTY_VARIABLE,
            FORM_PROPERTY_DEFAULT, FORM_PROPERTY_EXPRESSION, FORM_PROPERTY_DATE_PATTERN,
            EXECUTION_LISTENER_CLASS, EXECUTION_LISTENER_EVENT
        )
        props.keys.shouldContainSame(userTaskProps)
        argumentCaptor<List<EventPropagatableToXml>>().let {
            verify(fileCommitter).executeCommitAndGetHash(any(), it.capture(), any(), any())
            it.firstValue.shouldHaveSize(1)
            val changedType = it.firstValue.filterIsInstance<BpmnElementTypeChangeEvent>().shouldHaveSingleItem()
            changedType.props.keys.shouldContainSame(userTaskProps)
            changedType.props[ID]!!.value.shouldBe(serviceTaskStartBpmnId.id)
            changedType.props[NAME]!!.value.shouldBe("Start service task")
            changedType.props[DOCUMENTATION]!!.value.shouldBe("Start service task docs")
        }
    }
}