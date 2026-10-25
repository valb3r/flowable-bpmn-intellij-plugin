package com.valb3r.bpmn.intellij.plugin.flowable.ui.components.popupmenu

import com.valb3r.bpmn.intellij.plugin.core.tests.BaseCopyPastePopurriTest
import com.valb3r.bpmn.intellij.plugin.flowable.parser.FlowableObjectFactory
import com.valb3r.bpmn.intellij.plugin.flowable.parser.FlowableParser

internal class CopyPastePopurriTest : BaseCopyPastePopurriTest() {

    override val bpmnParser = FlowableParser()
    override val bpmnFactory = FlowableObjectFactory()
    override val resourceName = "popurri.bpmn20.xml"
    override val defaultFlowOwnerId = "exclusiveGatewayId"
    override val defaultFlowId = "onGatewayNokId"
    override val messageEventIds = listOf("startMessageEventId", "sid-08D95FA4-D88A-4E65-AF9D-0E764227D5CB", "intermediateMessageCatchingEventId")
    override val messageReference = "MessageId"
}
