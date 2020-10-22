package com.valb3r.bpmn.intellij.plugin.activiti.parser.customservicetasks

import com.valb3r.bpmn.intellij.plugin.activiti.parser.ActivitiParser
import com.valb3r.bpmn.intellij.plugin.activiti.parser.asResource
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldNotBeEmpty
import org.junit.jupiter.api.Test

private const val FILE = "custom-service-tasks/service-task-with-extension.bpmn20.xml"

internal class ActivityServiceTaskWithExtensionElementsTest {

    private val parser = ActivitiParser()

    @Test
    fun `Service task with failedJobRetryTimeCycle is parseable`() {
        val processObject = parser.parse(FILE.asResource()!!)

        processObject.process.body!!.serviceTask!![0].extensionElements!!.shouldNotBeEmpty()
        processObject.process.body!!.serviceTask!![0].extensionElements!![0].failedJobRetryTimeCycle.shouldBeEqualTo("R10/PT5M")
    }
}