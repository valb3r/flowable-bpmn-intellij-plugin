package com.valb3r.bpmn.intellij.plugin.camunda.parser

import com.valb3r.bpmn.intellij.plugin.bpmn.api.BpmnFileObject
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.Test


internal class CamundaParserPopurriTest {
    
    @Test
    fun `XML process with all Camunda elements is parseable without error`() {
        val processObject: BpmnFileObject?

        processObject = CamundaParser().parse("popurri.bpmn".asResource()!!)

        processObject.shouldNotBeNull()
    }
}

