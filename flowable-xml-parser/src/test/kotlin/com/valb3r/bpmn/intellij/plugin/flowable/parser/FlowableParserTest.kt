package com.valb3r.bpmn.intellij.plugin.flowable.parser

import com.valb3r.bpmn.intellij.plugin.bpmn.api.BpmnProcessObject
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.Test
import java.nio.charset.StandardCharsets.UTF_8


internal class FlowableParserTest {

    @Test
    fun `XML process with all Flowable elements is parseable without error`() {
        val processObject: BpmnProcessObject?

        processObject = FlowableParser().parse("popurri.bpmn20.xml".asResource()!!)

        processObject.shouldNotBeNull()
    }

    fun String.asResource(): String? = object {}::class.java.classLoader.getResource(this)?.readText(UTF_8)
}