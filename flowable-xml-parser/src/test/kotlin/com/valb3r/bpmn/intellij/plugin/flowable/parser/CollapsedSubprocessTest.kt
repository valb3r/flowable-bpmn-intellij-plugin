package com.valb3r.bpmn.intellij.plugin.flowable.parser

import com.valb3r.bpmn.intellij.plugin.bpmn.api.BpmnProcessObject
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldHaveSingleItem
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.Test

internal class CollapsedSubprocessTest {

    @Test
    fun `Collapsed subprocess is readable and mapped`() {
        val processObject: BpmnProcessObject?

        processObject = FlowableParser().parse("simple-collapsed-subprocess.bpmn20.xml".asResource()!!)

        processObject.shouldNotBeNull()
        processObject.process.body!!.subProcess!!.shouldHaveSingleItem().id.id.shouldBeEqualTo("subProcess")
        processObject.process.body!!.collapsedSubProcess!!.shouldHaveSingleItem().id.id.shouldBeEqualTo("collapsedSubProcess")
    }

    @Test
    fun `Collapsed transactional subprocess is readable and mapped`() {
        val processObject: BpmnProcessObject?

        processObject = FlowableParser().parse("transactional-collapsed-subprocess.bpmn20.xml".asResource()!!)

        processObject.shouldNotBeNull()
        processObject.process.body!!.transaction!!.shouldHaveSingleItem().id.id.shouldBeEqualTo("transactionalSubprocess")
        processObject.process.body!!.collapsedTransaction!!.shouldHaveSingleItem().id.id.shouldBeEqualTo("transactionalCollapsedSubprocess")
    }
}