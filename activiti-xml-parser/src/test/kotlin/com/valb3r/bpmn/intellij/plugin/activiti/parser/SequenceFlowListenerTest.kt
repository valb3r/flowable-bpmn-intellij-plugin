package com.valb3r.bpmn.intellij.plugin.activiti.parser

import com.valb3r.bpmn.intellij.plugin.bpmn.api.BpmnProcessObject
import com.valb3r.bpmn.intellij.plugin.bpmn.api.info.PropertyType
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldHaveSingleItem
import org.junit.jupiter.api.Test

internal class SequenceFlowListenerTest {

    @Test
    fun `SequenceFlow listener is parseable`() {
        val processObject: BpmnProcessObject?

        processObject = ActivitiParser().parse("sequence-flow-listeners.bpmn20.xml".asResource()!!)

        val sequenceFlow = processObject.process.body!!.sequenceFlow!!.shouldHaveSingleItem()
        sequenceFlow.id.id.shouldBeEqualTo("sequenceFlow")
        sequenceFlow.executionListener!!.shouldHaveSingleItem().clazz.shouldBeEqualTo("de.test")
        processObject.propsOf("sequenceFlow")[PropertyType.EXECUTION_LISTENER_CLASS]!!.value.shouldBeEqualTo("de.test")
    }
}