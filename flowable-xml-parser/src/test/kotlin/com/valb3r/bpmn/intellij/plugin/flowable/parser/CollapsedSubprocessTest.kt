package com.valb3r.bpmn.intellij.plugin.flowable.parser

import com.valb3r.bpmn.intellij.plugin.bpmn.api.BpmnFileObject
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldHaveSingleItem
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.Test

internal class CollapsedSubprocessTest {

    @Test
    fun `Collapsed subprocess is readable and mapped`() {
        val processObject: BpmnFileObject?

        processObject = FlowableParser().parse("simple-collapsed-subprocess.bpmn20.xml".asResource()!!)

        processObject.shouldNotBeNull()
        processObject.processes[0].body!!.subProcess!!.shouldHaveSingleItem().id.id.shouldBeEqualTo("subProcess")
        processObject.processes[0].body!!.collapsedSubProcess!!.shouldHaveSingleItem().id.id.shouldBeEqualTo("collapsedSubProcess")
    }

    @Test
    fun `Collapsed transactional subprocess is readable and mapped`() {
        val processObject: BpmnFileObject?

        processObject = FlowableParser().parse("transactional-collapsed-subprocess.bpmn20.xml".asResource()!!)

        processObject.shouldNotBeNull()
        processObject.processes[0].body!!.transaction!!.shouldHaveSingleItem().id.id.shouldBeEqualTo("transactionalSubprocess")
        processObject.processes[0].body!!.collapsedTransaction!!.shouldHaveSingleItem().id.id.shouldBeEqualTo("transactionalCollapsedSubprocess")
    }
}
