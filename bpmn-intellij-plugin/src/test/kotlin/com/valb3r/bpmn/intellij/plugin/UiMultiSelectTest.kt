package com.valb3r.bpmn.intellij.plugin

import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.Test

internal class UiMultiSelectTest: BaseUiTest() {

    // Is affected by multiselect feature, this is why it is here
    @Test
    fun `New service task can be created inside subprocess with correct parent`() {
        prepareOneSubProcessServiceTaskWithAttachedBoundaryEventView()
        val subprocessCenter = elementCenter(subprocessDiagramId)

        // currently it is enough to check that canvas will provide correct parent
        canvas.parentableElementAt(subprocessCenter).shouldBeEqualTo(subprocessBpmnId)
    }
}