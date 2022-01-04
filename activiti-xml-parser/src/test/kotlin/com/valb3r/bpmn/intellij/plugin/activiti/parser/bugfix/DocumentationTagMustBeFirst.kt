package com.valb3r.bpmn.intellij.plugin.activiti.parser.bugfix

import com.valb3r.bpmn.intellij.plugin.activiti.parser.ActivitiParser
import com.valb3r.bpmn.intellij.plugin.activiti.parser.asResource
import com.valb3r.bpmn.intellij.plugin.activiti.parser.testevents.StringValueUpdatedEvent
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.BpmnElementId
import com.valb3r.bpmn.intellij.plugin.bpmn.api.info.PropertyType
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.Test

private const val FILE = "bugfix/fbp-176-doc-tag-first.bpmn20.xml"

class DocumentationTagMustBeFirst {

    private val parser = ActivitiParser()
    private val processElem = BpmnElementId("test")
    private val processTag = "<process id=\"test\" name=\"测试流程\" isExecutable=\"true\">"

    @Test
    fun `Added or edited documentation tag must be first`() {
        val event = StringValueUpdatedEvent(processElem, PropertyType.DOCUMENTATION, "Some docs")

        val updated = parser.update(FILE.asResource()!!, listOf(event)).replace(Regex("^\\s+", RegexOption.MULTILINE), "")

        val procIndex = updated.indexOf(processTag)
        val docIndex = updated.indexOf("Some docs")
        docIndex.shouldBeEqualTo(procIndex + processTag.length + System.lineSeparator().length)
    }
}
