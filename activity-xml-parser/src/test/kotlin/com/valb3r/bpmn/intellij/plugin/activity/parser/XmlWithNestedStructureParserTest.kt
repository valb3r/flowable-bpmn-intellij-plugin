package com.valb3r.bpmn.intellij.plugin.activity.parser

import com.valb3r.bpmn.intellij.plugin.bpmn.api.BpmnProcessObject
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.BpmnElementId
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.BpmnProcessBody
import org.amshove.kluent.*
import org.junit.jupiter.api.Test

/**
 * Test to validate that XML file structure is parsed correctly.
 */
internal class XmlWithNestedStructureParserTest {

    @Test
    fun `XML file nested process structure parsing test`() {
        val processObject: BpmnProcessObject?

        processObject = ActivityParser().parse("nested-interlaced.bpmn20.xml".asResource()!!)

        // Assert the process structure
        processObject.shouldNotBeNull()
        processObject.process.id.shouldBeEqualTo(BpmnElementId("nested-test"))
        processObject.process.body.shouldNotBeNull()
        processObject.process.body!!.startEvent!!.shouldHaveSingleItem().id.id.shouldBeEqualTo("startGlobal")
        processObject.process.body!!.endEvent!!.shouldHaveSingleItem().id.id.shouldBeEqualTo("endGlobal")
        processObject.process.body!!.serviceTask!!.map { it.id.id }.shouldContainSame(
                listOf("parentInterlaceBeginServiceTask", "parentInterlaceEndServiceTask")
        )
        processObject.process.body!!.exclusiveGateway!!.shouldHaveSingleItem().id.id.shouldBeEqualTo("basicGateway")
        processObject.process.body!!.sequenceFlow!!.map { it.id.id }.shouldContainSame(
                listOf(
                        "sid-96EFCF3C-548C-4556-B36C-2F10C675DD3E",
                        "sid-B87070EE-2490-4FEC-AC02-099A30CFD986",
                        "sid-2498FD34-935C-4816-AAEE-63B3338BBB44",
                        "sid-3026ADA4-A1C3-4F68-A3D6-9BA522A6B22D",
                        "sid-DC8402FF-1C0C-46AF-9EC3-41D78EFEE460",
                        "sid-102BE6F5-4C32-4812-8EBF-A37911970FD9"
                )
        )

        processObject.process.body!!.subProcess!!.map { it.id.id }.shouldContainAll(
                listOf("sid-9DBEBCA6-7BE8-4170-ACC3-4548A2244C40", "sid-0B5D0923-5542-44DA-B86D-C3E4B2883DC2")
        )
        processObject.process.body!!.adHocSubProcess.shouldBeNull() // Activity does not support Ad-Hoc subprocess
        processObject.process.body!!.transaction!!.shouldHaveSingleItem().id.id.shouldBeEqualTo("sid-77F95F37-ADC3-4EBB-8F21-AEF1C015D5EB")
        processObject.process.children!!.keys.map { it.id }.shouldContainSame(
                listOf(
                        "sid-9DBEBCA6-7BE8-4170-ACC3-4548A2244C40",
                        "sid-9E62AF47-D4DF-4492-BA2F-E531CEB29A03",
                        "sid-0B5D0923-5542-44DA-B86D-C3E4B2883DC2",
                        "sid-1334170C-BA4D-4387-99BD-44229D18942C",
                        "sid-77F95F37-ADC3-4EBB-8F21-AEF1C015D5EB"
                )
        )

        validateDirectChildSubProcess(processObject.process.children!![BpmnElementId("sid-9DBEBCA6-7BE8-4170-ACC3-4548A2244C40")]!!)
        validateInSubProcessNestedChildSubProcess(processObject.process.children!![BpmnElementId("sid-9E62AF47-D4DF-4492-BA2F-E531CEB29A03")]!!)
        validateAnotherDirectSubProcess(processObject.process.children!![BpmnElementId("sid-0B5D0923-5542-44DA-B86D-C3E4B2883DC2")]!!)
        validateAnotherDirectSubProcessNestedChildSubProcess(processObject.process.children!![BpmnElementId("sid-1334170C-BA4D-4387-99BD-44229D18942C")]!!)
        validateDirectTransactionSubProcess(processObject.process.children!![BpmnElementId("sid-77F95F37-ADC3-4EBB-8F21-AEF1C015D5EB")]!!)

        othersAreEmpty(processObject.process.body!!)
    }

    private fun validateDirectChildSubProcess(subProcess: BpmnProcessBody) {
        subProcess.startEvent!!.shouldHaveSingleItem().id.id.shouldBeEqualTo("startInActivity")
        subProcess.endEvent!!.shouldHaveSingleItem().id.id.shouldBeEqualTo("endInActivity")
        subProcess.serviceTask!!.shouldHaveSingleItem().id.id.shouldBeEqualTo("nestedServiceTask")

        subProcess.sequenceFlow!!.map { it.id.id }.shouldContainSame(
                listOf(
                        "sid-7717FAC0-CC9B-4751-8DB8-586C58CA4D4A",
                        "sid-51246E7C-B34C-4D26-8572-059DCBA320E6",
                        "sid-D08DBAC7-C7F2-4D02-9059-C43F5CF80B44"
                )
        )

        subProcess.subProcess!!.shouldHaveSingleItem().id.id.shouldBeEqualTo("sid-9E62AF47-D4DF-4492-BA2F-E531CEB29A03")
        othersAreEmpty(subProcess)
    }

    private fun validateInSubProcessNestedChildSubProcess(subProcess: BpmnProcessBody) {
        subProcess.startEvent!!.shouldHaveSingleItem().id.id.shouldBeEqualTo("startInNested")
        subProcess.endEvent!!.shouldHaveSingleItem().id.id.shouldBeEqualTo("endInNested")
        subProcess.serviceTask!!.map { it.id.id }.shouldContainSame(listOf("nestedNestedServiceTask", "nestedServiceTaskInterlaced"))

        subProcess.sequenceFlow!!.map { it.id.id }.shouldContainSame(
                listOf(
                        "sid-E9C9051E-D3E7-4813-987C-76935BC96A3E",
                        "sid-2CAAFFF8-A7B8-43D2-A273-893CF30F6FAD"
                )
        )
        othersAreEmpty(subProcess)
    }

    private fun validateAnotherDirectSubProcess(adHoc: BpmnProcessBody) {
        adHoc.startEvent!!.shouldHaveSingleItem().id.id.shouldBeEqualTo("startInAnotherDirect")
        adHoc.endEvent!!.shouldHaveSingleItem().id.id.shouldBeEqualTo("endInAnotherDirect")
        adHoc.serviceTask!!.map { it.id.id }.shouldContainSame(listOf("sid-7C6C7BBC-9EA3-4640-A983-9654FC24790F", "nestedServiceTaskInterlacedOther"))
        adHoc.userTask!!.shouldHaveSingleItem().id.id.shouldBeEqualTo("sid-290A9845-3F95-4B0D-893F-BB9E17396104")

        adHoc.sequenceFlow!!.map { it.id.id }.shouldContainSame(
                listOf(
                        "sid-8DF33714-DB22-4C59-A4E1-1BBBA27945EC",
                        "sid-2E7D5EF5-8CE2-4A90-B8A9-1F26139AB3BB",
                        "sid-F6CF1E0C-F6C5-40E0-9B57-AABD1E17C74F"
                )
        )

        adHoc.subProcess!!.shouldHaveSingleItem().id.id.shouldBeEqualTo("sid-1334170C-BA4D-4387-99BD-44229D18942C")
        othersAreEmpty(adHoc, true)
    }

    private fun validateAnotherDirectSubProcessNestedChildSubProcess(subprocess: BpmnProcessBody) {
        subprocess.startEvent!!.shouldHaveSingleItem().id.id.shouldBeEqualTo("sid-C4B5DA0B-B84F-4EA1-8292-EB3C888D3453")
        subprocess.endEvent!!.shouldHaveSingleItem().id.id.shouldBeEqualTo("sid-11D45B2C-134A-4D23-970E-556835A13A07")
        subprocess.serviceTask!!.shouldHaveSingleItem().id.id.shouldBeEqualTo("sid-CE70DFAB-0C85-4910-9E9C-197BDCE5F8C5")

        subprocess.sequenceFlow!!.map { it.id.id }.shouldContainSame(
                listOf(
                        "sid-AE54A4F6-D7D8-4CD9-B92C-167890158CEC",
                        "sid-1026A447-8C82-43CB-9DAD-5186126D7873"
                )
        )
        othersAreEmpty(subprocess)
    }

    private fun validateDirectTransactionSubProcess(transaction: BpmnProcessBody) {
        transaction.serviceTask!!.map { it.id.id }.shouldContainSame(listOf("sid-2F51F641-275F-48B5-B886-6C28E9C7C04E", "nestedServiceTaskInterlacedYetOther"))
        transaction.userTask!!.shouldHaveSingleItem().id.id.shouldBeEqualTo("sid-5320D81D-09E8-4BCB-842A-E3150FC08B6C")

        transaction.sequenceFlow.shouldBeNull()
        transaction.subProcess.shouldBeNull()
        othersAreEmpty(transaction, true)
    }


    private fun othersAreEmpty(body: BpmnProcessBody, hasUserTask: Boolean = false) {
        body.timerStartEvent.shouldBeNull()
        body.signalStartEvent.shouldBeNull()
        body.messageStartEvent.shouldBeNull()
        body.errorStartEvent.shouldBeNull()
        body.escalationStartEvent.shouldBeNull()
        body.conditionalStartEvent.shouldBeNull()

        body.errorEndEvent.shouldBeNull()
        body.escalationEndEvent.shouldBeNull()
        body.cancelEndEvent.shouldBeNull()
        body.terminateEndEvent.shouldBeNull()

        body.boundaryEvent.shouldBeNull()
        body.boundaryCancelEvent.shouldBeNull()
        body.boundaryCompensationEvent.shouldBeNull()
        body.boundaryConditionalEvent.shouldBeNull()
        body.boundaryErrorEvent.shouldBeNull()
        body.boundaryEscalationEvent.shouldBeNull()
        body.boundaryMessageEvent.shouldBeNull()
        body.boundarySignalEvent.shouldBeNull()
        body.boundaryTimerEvent.shouldBeNull()

        body.intermediateCatchEvent.shouldBeNull()
        body.intermediateTimerCatchingEvent.shouldBeNull()
        body.intermediateMessageCatchingEvent.shouldBeNull()
        body.intermediateSignalCatchingEvent.shouldBeNull()
        body.intermediateConditionalCatchingEvent.shouldBeNull()

        if (!hasUserTask) {
            body.userTask.shouldBeNull()
        }
        body.scriptTask.shouldBeNull()
        body.businessRuleTask.shouldBeNull()
        body.receiveTask.shouldBeNull()

        body.callActivity.shouldBeNull()
        body.eventSubProcess.shouldBeNull()

        body.parallelGateway.shouldBeNull()
        body.inclusiveGateway.shouldBeNull()
        body.eventBasedGateway.shouldBeNull()
    }
}