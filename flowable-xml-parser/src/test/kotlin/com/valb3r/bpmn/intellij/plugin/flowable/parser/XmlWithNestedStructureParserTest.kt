package com.valb3r.bpmn.intellij.plugin.flowable.parser

import com.valb3r.bpmn.intellij.plugin.bpmn.api.BpmnProcessObject
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.BpmnElementId
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.BpmnProcessBody
import org.amshove.kluent.*
import org.junit.jupiter.api.Test
import java.nio.charset.StandardCharsets

/**
 * Test to validate that XML file structure is parsed correctly.
 */
internal class XmlWithNestedStructureParserTest {

    @Test
    fun `XML file nested process structure parsing test`() {
        val processObject: BpmnProcessObject?

        processObject = FlowableParser().parse("nested-interlaced.bpmn20.xml".asResource()!!)

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
                        "sid-E256FA9F-E663-49B5-B15A-6C1BA641C61A",
                        "sid-4F47ED8C-967A-4774-AC42-0DD33A0F5FA7",
                        "sid-80D672A9-3435-4FBC-9E9D-9D0399B88198",
                        "sid-86067FAE-FBC2-4888-A540-E65D7DE8D84F",
                        "sid-97C9B973-F379-4DFC-B70C-4C612ED35A2D",
                        "sid-57CF2914-575D-4404-BD6E-87C35A53904A"
                )
        )

        processObject.process.body!!.subProcess!!.shouldHaveSingleItem().id.id.shouldBeEqualTo("sid-C4389D7E-1083-47D2-BECC-99479E63D18B")
        processObject.process.body!!.adHocSubProcess!!.shouldHaveSingleItem().id.id.shouldBeEqualTo("sid-5EEB495F-ACAC-4C04-99E1-691D906B3A30")
        processObject.process.body!!.transaction!!.shouldHaveSingleItem().id.id.shouldBeEqualTo("sid-1BB4FA80-C87F-4A05-95DF-753D06EE7424")
        processObject.process.children!!.keys.map { it.id }.shouldContainSame(
                listOf(
                        "sid-C4389D7E-1083-47D2-BECC-99479E63D18B",
                        "sid-5EEB495F-ACAC-4C04-99E1-691D906B3A30",
                        "sid-1BB4FA80-C87F-4A05-95DF-753D06EE7424",
                        "sid-3AD3FAD5-389C-4066-8CB0-C4090CA91F6D",
                        "sid-775FFB07-8CFB-4F82-A6EA-AB0E9BBB79A6"
                )
        )

        validateDirectChildSubProcess(processObject.process.children!![BpmnElementId("sid-C4389D7E-1083-47D2-BECC-99479E63D18B")]!!)
        validateInSubProcessNestedChildSubProcess(processObject.process.children!![BpmnElementId("sid-775FFB07-8CFB-4F82-A6EA-AB0E9BBB79A6")]!!)
        validateDirectAdhocSubProcess(processObject.process.children!![BpmnElementId("sid-5EEB495F-ACAC-4C04-99E1-691D906B3A30")]!!)
        validateInAdHocNestedChildSubProcess(processObject.process.children!![BpmnElementId("sid-3AD3FAD5-389C-4066-8CB0-C4090CA91F6D")]!!)
        validateDirectTransactionSubProcess(processObject.process.children!![BpmnElementId("sid-1BB4FA80-C87F-4A05-95DF-753D06EE7424")]!!)

        othersAreEmpty(processObject.process.body!!)
    }

    private fun validateDirectChildSubProcess(subProcess: BpmnProcessBody) {
        subProcess.startEvent!!.shouldHaveSingleItem().id.id.shouldBeEqualTo("startInActivity")
        subProcess.endEvent!!.shouldHaveSingleItem().id.id.shouldBeEqualTo("endInActivity")
        subProcess.serviceTask!!.shouldHaveSingleItem().id.id.shouldBeEqualTo("nestedServiceTask")

        subProcess.sequenceFlow!!.map { it.id.id }.shouldContainSame(
                listOf(
                        "sid-1FCC7913-358C-4C81-878D-40BB6E351415",
                        "sid-C7200B1B-E3F1-483B-8640-661B1BFFA6C8",
                        "sid-33FCDE0A-FC4A-4F9E-9AEF-CB88046B4FBE"
                )
        )

        subProcess.subProcess!!.shouldHaveSingleItem().id.id.shouldBeEqualTo("sid-775FFB07-8CFB-4F82-A6EA-AB0E9BBB79A6")
        othersAreEmpty(subProcess)
    }

    private fun validateInSubProcessNestedChildSubProcess(subProcess: BpmnProcessBody) {
        subProcess.startEvent!!.shouldHaveSingleItem().id.id.shouldBeEqualTo("startInNested")
        subProcess.endEvent!!.shouldHaveSingleItem().id.id.shouldBeEqualTo("endInNested")
        subProcess.serviceTask!!.map { it.id.id }.shouldContainSame(listOf("nestedNestedServiceTask", "nestedServiceTaskInterlaced"))

        subProcess.sequenceFlow!!.map { it.id.id }.shouldContainSame(
                listOf(
                        "sid-603C30D3-9B86-4E04-8C86-7F20FDB69504",
                        "sid-567BD2D6-393E-4949-8026-3EFE690F9790"
                )
        )
        othersAreEmpty(subProcess)
    }

    private fun validateDirectAdhocSubProcess(adHoc: BpmnProcessBody) {
        adHoc.startEvent!!.shouldHaveSingleItem().id.id.shouldBeEqualTo("startInAdHoc")
        adHoc.endEvent!!.shouldHaveSingleItem().id.id.shouldBeEqualTo("endInAdHoc")
        adHoc.serviceTask!!.map { it.id.id }.shouldContainSame(listOf("sid-49F41812-EBEB-4084-AF8A-11184E33CBC1", "nestedServiceTaskInterlacedOther"))
        adHoc.userTask!!.shouldHaveSingleItem().id.id.shouldBeEqualTo("sid-EDD2CFE3-D620-47B1-9632-9F5ABE9A68E4")

        adHoc.sequenceFlow!!.map { it.id.id }.shouldContainSame(
                listOf(
                        "sid-0D51954C-D49F-41FD-BCDC-6F7EB4102834",
                        "sid-907EFDBB-5066-4386-BAC6-8A9DA889FE86",
                        "sid-E062317B-40BC-47E4-89F5-56EE4FC6F9EB"
                )
        )

        adHoc.subProcess!!.shouldHaveSingleItem().id.id.shouldBeEqualTo("sid-3AD3FAD5-389C-4066-8CB0-C4090CA91F6D")
        othersAreEmpty(adHoc, true)
    }

    private fun validateInAdHocNestedChildSubProcess(subprocess: BpmnProcessBody) {
        subprocess.startEvent!!.shouldHaveSingleItem().id.id.shouldBeEqualTo("sid-0E2068A3-FEF1-46A1-AD2B-7DCD0003AA65")
        subprocess.endEvent!!.shouldHaveSingleItem().id.id.shouldBeEqualTo("sid-64B56D4F-E27F-43F7-AF94-AC858CCFE0D5")
        subprocess.serviceTask!!.shouldHaveSingleItem().id.id.shouldBeEqualTo("sid-57A163D8-81CB-4B71-B74C-DD4A152B6653")

        subprocess.sequenceFlow!!.map { it.id.id }.shouldContainSame(
                listOf(
                        "sid-D6B4FE9C-ECC8-4F25-810A-C74DEAA909D2",
                        "sid-37D81430-A654-4425-9BF5-9935CB3AAD45"
                )
        )
        othersAreEmpty(subprocess)
    }

    private fun validateDirectTransactionSubProcess(transaction: BpmnProcessBody) {
        transaction.serviceTask!!.map { it.id.id }.shouldContainSame(listOf("sid-7ED7A92A-C30E-445C-8A50-3E5183BDF318", "nestedServiceTaskInterlacedYetOther"))
        transaction.userTask!!.shouldHaveSingleItem().id.id.shouldBeEqualTo("sid-370B2F2F-D1E9-4CDE-A230-EEC9572D4244")

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

    private fun String.asResource(): String? = object {}::class.java.classLoader.getResource(this)?.readText(StandardCharsets.UTF_8)
}