package com.valb3r.bpmn.intellij.plugin.activiti.ui.components.popupmenu

import com.valb3r.bpmn.intellij.plugin.core.tests.BaseCopyPastePopurriTest
import com.valb3r.bpmn.intellij.plugin.activiti.parser.ActivitiObjectFactory
import com.valb3r.bpmn.intellij.plugin.activiti.parser.ActivitiParser

internal class CopyPastePopurriTest : BaseCopyPastePopurriTest() {

    override val bpmnParser = ActivitiParser()
    override val bpmnFactory = ActivitiObjectFactory()
    override val resourceName = "popurri.bpmn20.xml"
    override val defaultFlowOwnerId = "exclusiveGatewayId"
    override val defaultFlowId = "sid-2CF229A2-6399-4510-AED6-45B5C553458C"
    override val messageEventIds = listOf("startMessageEvent", "boundaryMessageEventId", "intermediateMessageCatchingEventId")
    override val messageReference = "someMessage"
}
