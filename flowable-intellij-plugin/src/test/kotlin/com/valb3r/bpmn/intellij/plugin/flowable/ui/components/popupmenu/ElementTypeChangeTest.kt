package com.valb3r.bpmn.intellij.plugin.flowable.ui.components.popupmenu

import com.nhaarman.mockitokotlin2.verify
import org.junit.jupiter.api.Test

class ElementTypeChangeTest: FlowableBaseUiTest() {

    @Test
    fun `Service task shape changes to User task`() {
        prepareTwoServiceTaskView()
        clickOnId(serviceTaskStartDiagramId)
        val serviceTaskTypeChange = findExactlyOneTypeChangeElem()
        clickOnId(serviceTaskTypeChange!!)

        verify(popupMenuProvider).popupChangeShapeType(serviceTaskStartBpmnId)
    }
}