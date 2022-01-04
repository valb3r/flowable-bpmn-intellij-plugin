package com.valb3r.bpmn.intellij.plugin.flowable.parser.bugfix

import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.BpmnElementId
import com.valb3r.bpmn.intellij.plugin.bpmn.api.info.PropertyType
import com.valb3r.bpmn.intellij.plugin.flowable.parser.FlowableParser
import com.valb3r.bpmn.intellij.plugin.flowable.parser.asResource
import com.valb3r.bpmn.intellij.plugin.flowable.parser.testevents.StringValueUpdatedEvent
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.Test

private const val FILE = "bugfix/fbp-176-doc-tag-first.bpmn20.xml"

class DocumentationTagMustBeFirst {

    private val processElem = BpmnElementId("test")
    private val processTag = "<process id=\"test\" name=\"测试流程\" isExecutable=\"true\">"

    @Test
    fun `Flowable Added or edited documentation tag must be first`() {
        val parser = FlowableParser()
        val event = StringValueUpdatedEvent(processElem, PropertyType.DOCUMENTATION, "Some docs")

        val updated = parser.update(FILE.asResource()!!, listOf(event)).replace(Regex("^\\s+", RegexOption.MULTILINE), "")

        val procIndex = updated.indexOf(processTag)
        val docIndex = updated.indexOf("<documentation>Some docs</documentation>")
        docIndex.shouldBeEqualTo(procIndex + processTag.length + System.lineSeparator().length)
    }
}
