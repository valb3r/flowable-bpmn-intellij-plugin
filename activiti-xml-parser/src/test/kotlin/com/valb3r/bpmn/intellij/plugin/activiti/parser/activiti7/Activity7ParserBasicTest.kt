package com.valb3r.bpmn.intellij.plugin.activiti.parser.activiti7

import com.valb3r.bpmn.intellij.plugin.activiti.parser.Activiti7ObjectFactory
import com.valb3r.bpmn.intellij.plugin.activiti.parser.ActivitiObjectFactory
import com.valb3r.bpmn.intellij.plugin.activiti.parser.ActivitiParser
import com.valb3r.bpmn.intellij.plugin.activiti.parser.asResource
import com.valb3r.bpmn.intellij.plugin.bpmn.api.BpmnProcessObject
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.Test

internal class Activity7ParserBasicTest {
    
    @Test
    fun `Activiti 7 process should be parseable and its properties readable`() {
        val processObject: BpmnProcessObject?

        processObject = ActivitiParser().parse("activiti7/simple-activiti7-process.bpmn20.xml".asResource()!!)

        processObject.shouldNotBeNull()
        val props = BpmnProcessObject(processObject.process, null, processObject.diagram).toView(Activiti7ObjectFactory()).elemPropertiesByElementId
        props.shouldNotBeNull()
    }
}

