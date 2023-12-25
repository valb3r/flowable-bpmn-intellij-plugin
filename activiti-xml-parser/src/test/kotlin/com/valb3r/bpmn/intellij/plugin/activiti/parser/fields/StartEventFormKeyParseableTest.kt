package com.valb3r.bpmn.intellij.plugin.activiti.parser.fields

import com.valb3r.bpmn.intellij.plugin.activiti.parser.ActivitiParser
import com.valb3r.bpmn.intellij.plugin.activiti.parser.asResource
import com.valb3r.bpmn.intellij.plugin.activiti.parser.propsOf
import com.valb3r.bpmn.intellij.plugin.bpmn.api.BpmnProcessObject
import com.valb3r.bpmn.intellij.plugin.bpmn.api.info.PropertyType
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldHaveSingleItem
import org.junit.jupiter.api.Test

internal class StartEventFormKeyParseableTest {

    @Test
    fun `StartEvent formKey is parseable`() {
        val processObject: BpmnProcessObject?

        processObject = ActivitiParser().parse("fields/FBP-327-formKey-on-start-evt.bpmn20.xml".asResource()!!)

        val startEvent = processObject.process.body!!.startEvent!!.shouldHaveSingleItem()
        startEvent.id.id.shouldBeEqualTo("startEvent1")
        startEvent.formKey.shouldBeEqualTo("formKey")
        startEvent.formFieldValidation.shouldBeEqualTo(true)
        processObject.propsOf("startEvent1")[PropertyType.FORM_KEY]!!.value.shouldBeEqualTo("formKey")
        processObject.propsOf("startEvent1")[PropertyType.FORM_FIELD_VALIDATION]!!.value.shouldBeEqualTo(true)
    }
}