package com.valb3r.bpmn.intellij.plugin.activiti.parser.bugfix

import com.valb3r.bpmn.intellij.plugin.activiti.parser.Activiti7Parser
import com.valb3r.bpmn.intellij.plugin.activiti.parser.ActivitiParser
import com.valb3r.bpmn.intellij.plugin.activiti.parser.asResource
import com.valb3r.bpmn.intellij.plugin.activiti.parser.testevents.StringValueUpdatedEvent
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.BpmnElementId
import com.valb3r.bpmn.intellij.plugin.bpmn.api.info.PropertyType
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.Test

private const val FILE = "bugfix/fbp-176-doc-tag-first.bpmn20.xml"

class DocumentationTagMustBeFirst {

    private val processElem = BpmnElementId("test")
    private val processTag = "<process id=\"test\" name=\"测试流程\" isExecutable=\"true\">"

    @Test
    fun `Activiti 6x Added or edited documentation tag must be first`() {
        val parser = ActivitiParser()
        val event = StringValueUpdatedEvent(processElem, PropertyType.DOCUMENTATION, "Some docs")

        val updated = parser.update(FILE.asResource()!!, listOf(event)).replace(Regex("^\\s+", RegexOption.MULTILINE), "")

        val procIndex = updated.indexOf(processTag)
        val docIndex = updated.indexOf("<documentation>Some docs</documentation>")
        docIndex.shouldBeEqualTo(procIndex + processTag.length + System.lineSeparator().length)
    }

    @Test
    fun `Activiti 7x Added or edited documentation tag must be first`() {
        val parser = Activiti7Parser()
        val event = StringValueUpdatedEvent(processElem, PropertyType.DOCUMENTATION, "Some docs")

        val updated = parser.update(FILE.asResource()!!, listOf(event)).replace(Regex("^\\s+", RegexOption.MULTILINE), "")

        val procIndex = updated.indexOf(processTag)
        val docIndex = updated.indexOf("<documentation>Some docs</documentation>")
        docIndex.shouldBeEqualTo(procIndex + processTag.length + System.lineSeparator().length)
    }
}
