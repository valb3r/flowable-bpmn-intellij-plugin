package com.valb3r.bpmn.intellij.plugin.camunda.popupmenu

import com.valb3r.bpmn.intellij.plugin.core.tests.BaseCopyPastePopurriTest
import com.valb3r.bpmn.intellij.plugin.camunda.parser.CamundaObjectFactory
import com.valb3r.bpmn.intellij.plugin.camunda.parser.CamundaParser

internal class CopyPastePopurriTest : BaseCopyPastePopurriTest() {

    override val bpmnParser = CamundaParser()
    override val bpmnFactory = CamundaObjectFactory()
    override val resourceName = "popurri.bpmn"
    override val defaultFlowOwnerId = "exclusiveGateway"
    override val defaultFlowId = "Flow_0umzo4t"
    override val messageEventIds = listOf("messageStartEvent", "copyPasteBoundaryMessageEvent", "messageIntermediateCatchEvent")
    override val messageReference = "CopyPasteMessage"

    override fun fixtureXml(): String = super.fixtureXml().replace(
        "<bpmn:process ",
        "<bpmn:message id=\"CopyPasteMessage\" name=\"Copy/paste message\"/><bpmn:process "
    ).replace(
        "</bpmn:process>",
        """<bpmn:boundaryEvent id="copyPasteBoundaryMessageEvent" attachedToRef="serviceTask" cancelActivity="true">
            <bpmn:messageEventDefinition messageRef="CopyPasteMessage"/>
        </bpmn:boundaryEvent></bpmn:process>"""
    ).replace(
        "</bpmndi:BPMNPlane>",
        """<bpmndi:BPMNShape id="copyPasteBoundaryMessageEvent_di" bpmnElement="copyPasteBoundaryMessageEvent">
            <dc:Bounds x="452" y="922" width="36" height="36"/>
        </bpmndi:BPMNShape></bpmndi:BPMNPlane>"""
    )
}
