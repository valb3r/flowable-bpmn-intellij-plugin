package com.valb3r.bpmn.intellij.plugin.camunda.parser.fields

import com.valb3r.bpmn.intellij.plugin.bpmn.api.BpmnProcessObject
import com.valb3r.bpmn.intellij.plugin.bpmn.api.info.PropertyType
import com.valb3r.bpmn.intellij.plugin.camunda.parser.CamundaParser
import com.valb3r.bpmn.intellij.plugin.camunda.parser.asResource
import com.valb3r.bpmn.intellij.plugin.camunda.parser.propsOf
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeNull
import org.amshove.kluent.shouldHaveSingleItem
import org.junit.jupiter.api.Test

internal class StartEventFormKeyParseableTest {

    @Test
    fun `StartEvent formKey is parseable`() {
        val processObject: BpmnProcessObject?

        processObject = CamundaParser().parse("fields/FBP-327-formKey-on-start-evt.bpmn20.xml".asResource()!!)

        val startEvent = processObject.process.body!!.startEvent!!.shouldHaveSingleItem()
        startEvent.id.id.shouldBeEqualTo("startEvent1")
        startEvent.formKey.shouldBeEqualTo("formKey")
        processObject.propsOf("startEvent1")[PropertyType.FORM_KEY]!!.value.shouldBeEqualTo("formKey")
        processObject.propsOf("startEvent1")[PropertyType.FORM_FIELD_VALIDATION].shouldBeNull()
    }
}