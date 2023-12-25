package com.valb3r.bpmn.intellij.plugin.flowable.parser.fields

import com.valb3r.bpmn.intellij.plugin.bpmn.api.BpmnProcessObject
import com.valb3r.bpmn.intellij.plugin.bpmn.api.info.PropertyType
import com.valb3r.bpmn.intellij.plugin.flowable.parser.FlowableParser
import com.valb3r.bpmn.intellij.plugin.flowable.parser.asResource
import com.valb3r.bpmn.intellij.plugin.flowable.parser.propsOf
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldHaveSingleItem
import org.junit.jupiter.api.Test

internal class StartEventFormKeyParseableTest {

    @Test
    fun `StartEvent formKey is parseable`() {
        val processObject: BpmnProcessObject?

        processObject = FlowableParser().parse("fields/FBP-327-formKey-on-start-evt.bpmn20.xml".asResource()!!)

        val startEvent = processObject.process.body!!.startEvent!!.shouldHaveSingleItem()
        startEvent.id.id.shouldBeEqualTo("startEvent1")
        startEvent.formKey.shouldBeEqualTo("formKey")
        startEvent.formFieldValidation.shouldBeEqualTo(true)
        processObject.propsOf("startEvent1")[PropertyType.FORM_KEY]!!.value.shouldBeEqualTo("formKey")
        processObject.propsOf("startEvent1")[PropertyType.FORM_FIELD_VALIDATION]!!.value.shouldBeEqualTo(true)
    }
}