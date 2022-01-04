package com.valb3r.bpmn.intellij.plugin.camunda.parser.bugfix

import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.BpmnElementId
import com.valb3r.bpmn.intellij.plugin.bpmn.api.info.PropertyType
import com.valb3r.bpmn.intellij.plugin.camunda.parser.CamundaParser
import com.valb3r.bpmn.intellij.plugin.camunda.parser.asResource
import com.valb3r.bpmn.intellij.plugin.camunda.parser.testevents.StringValueUpdatedEvent
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.Test

private const val FILE = "bugfix/fbp-176-doc-tag-first.bpmn"

class DocumentationTagMustBeFirst {

    private val processElem = BpmnElementId("test")
    private val processTag = "<process id=\"test\" name=\"测试流程\" isExecutable=\"true\">"

    @Test
    fun `Camunda Added or edited documentation tag must be first`() {
        val parser = CamundaParser()
        val event = StringValueUpdatedEvent(processElem, PropertyType.DOCUMENTATION, "Some docs")

        val updated = parser.update(FILE.asResource()!!, listOf(event)).replace(Regex("^\\s+", RegexOption.MULTILINE), "")

        val procIndex = updated.indexOf(processTag)
        val docIndex = updated.indexOf("<documentation>Some docs</documentation>")
        docIndex.shouldBeEqualTo(procIndex + processTag.length + System.lineSeparator().length)
    }
}
