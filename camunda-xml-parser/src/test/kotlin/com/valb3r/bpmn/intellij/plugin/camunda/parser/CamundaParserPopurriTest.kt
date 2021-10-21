package com.valb3r.bpmn.intellij.plugin.camunda.parser

import com.valb3r.bpmn.intellij.plugin.bpmn.api.BpmnProcessObject
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.BpmnElementId
import com.valb3r.bpmn.intellij.plugin.bpmn.api.diagram.DiagramElementId
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.Test
import java.util.*


internal class CamundaParserPopurriTest {
    
    @Test
    fun `XML process with all Camunda elements is parseable without error`() {
        val processObject: BpmnProcessObject?

        processObject = CamundaParser().parse("popurri.bpmn".asResource()!!)

        processObject.shouldNotBeNull()
    }
}

