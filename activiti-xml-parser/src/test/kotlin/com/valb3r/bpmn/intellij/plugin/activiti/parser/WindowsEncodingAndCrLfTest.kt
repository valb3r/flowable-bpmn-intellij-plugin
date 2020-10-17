package com.valb3r.bpmn.intellij.plugin.activiti.parser

import com.valb3r.bpmn.intellij.plugin.activiti.parser.testevents.StringValueUpdatedEvent
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.BpmnElementId
import com.valb3r.bpmn.intellij.plugin.bpmn.api.info.PropertyType
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.Test
import java.lang.reflect.Field
import java.nio.charset.Charset


class WindowsEncodingAndCrLfTest {

    @Test
    fun `XML process is updatable with custom default charset`() {
        setKoi8DefaultEncoding()

        val initial = "empty-process-name.bpmn20.xml".asResource()!!
        val updateNameTo = "提交请假"
        val ActivityParser = ActivitiParser()
        val updated = ActivityParser.update(initial, listOf(StringValueUpdatedEvent(BpmnElementId("empty-process-name"), PropertyType.NAME, updateNameTo)))
        ActivityParser.parse(updated).process.name.shouldBeEqualTo(updateNameTo)
    }

    private fun setKoi8DefaultEncoding() {
        System.setProperty("file.encoding", "KOI8-R")
        val charset: Field = Charset::class.java.getDeclaredField("defaultCharset")
        charset.isAccessible = true
        charset.set(null, null)

    }
}
