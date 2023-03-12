package com.valb3r.bpmn.intellij.plugin.flowable.parser

import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.BpmnElementId
import com.valb3r.bpmn.intellij.plugin.bpmn.api.info.PropertyType
import com.valb3r.bpmn.intellij.plugin.flowable.parser.testevents.StringValueUpdatedEvent
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
        val flowableParser = FlowableParser()
        val updated = flowableParser.update(initial, listOf(StringValueUpdatedEvent(BpmnElementId("empty-process-name"), PropertyType.NAME, updateNameTo)))
        flowableParser.parse(updated).processes[0].name.shouldBeEqualTo(updateNameTo)
    }

    private fun setKoi8DefaultEncoding() {
        System.setProperty("file.encoding", "KOI8-R")
        val charset: Field = Charset::class.java.getDeclaredField("defaultCharset")
        charset.isAccessible = true
        charset.set(null, null)

    }
}
