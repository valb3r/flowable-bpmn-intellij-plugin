package com.valb3r.bpmn.intellij.plugin

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.never
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import com.valb3r.bpmn.intellij.plugin.bpmn.api.info.PropertyType
import com.valb3r.bpmn.intellij.plugin.core.newelements.registerNewElementsFactory
import com.valb3r.bpmn.intellij.plugin.core.properties.propertiesVisualizer
import com.valb3r.bpmn.intellij.plugin.core.tests.BaseUiTest
import com.valb3r.bpmn.intellij.plugin.flowable.parser.FlowableObjectFactory
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import javax.swing.JTextField

internal class ValidationTest: BaseUiTest() {

    @BeforeEach
    fun prepare() {
        registerNewElementsFactory(project, FlowableObjectFactory())
        prepareTwoServiceTaskView()
    }

    @Test
    fun `Invalid ID (duplicate) not propagated`() {
        clickOnId(serviceTaskEndDiagramId)
        val idField = textFieldsConstructed[Pair(serviceTaskEndBpmnId, PropertyType.ID)]!!
        whenever(idField.text).thenReturn(serviceTaskStartBpmnId.id)
        (idField.component as JTextField).text = serviceTaskStartBpmnId.id

        propertiesVisualizer(project).clear()

        verify(fileCommitter, never()).executeCommitAndGetHash(any(), any(), any(), any())
    }

    @Test
    fun `Valid ID (not duplicate) propagated`() {
        clickOnId(serviceTaskEndDiagramId)
        val idField = textFieldsConstructed[Pair(serviceTaskEndBpmnId, PropertyType.ID)]!!
        whenever(idField.text).thenReturn(serviceTaskStartBpmnId.id + '1')
        (idField.component as JTextField).text = serviceTaskStartBpmnId.id + '1'

        propertiesVisualizer(project).clear()

        verify(fileCommitter).executeCommitAndGetHash(any(), any(), any(), any())
    }
}