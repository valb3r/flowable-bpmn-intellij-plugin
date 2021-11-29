package com.valb3r.bpmn.intellij.plugin.camunda.parser.collaboration

import com.valb3r.bpmn.intellij.plugin.bpmn.api.BpmnFileObject
import com.valb3r.bpmn.intellij.plugin.camunda.parser.CamundaParser
import com.valb3r.bpmn.intellij.plugin.camunda.parser.asResource
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.Test

internal class CamundaParserCollaborationTest {
    
    @Test
    fun `XML process with swimlane Camunda elements is parseable without error`() {
        val processObject: BpmnFileObject?

        processObject = CamundaParser().parse("swimlanes.bpmn".asResource()!!)

        processObject.shouldNotBeNull()
    }
}
