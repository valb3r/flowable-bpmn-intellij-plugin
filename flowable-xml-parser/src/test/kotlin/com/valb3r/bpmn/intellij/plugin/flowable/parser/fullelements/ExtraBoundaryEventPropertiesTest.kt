package com.valb3r.bpmn.intellij.plugin.flowable.parser.fullelements

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.jayway.jsonpath.JsonPath
import com.valb3r.bpmn.intellij.plugin.bpmn.api.BpmnProcessObject
import com.valb3r.bpmn.intellij.plugin.bpmn.api.PropertyTable
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.BpmnElementId
import com.valb3r.bpmn.intellij.plugin.bpmn.api.info.Property
import com.valb3r.bpmn.intellij.plugin.bpmn.api.info.PropertyType
import com.valb3r.bpmn.intellij.plugin.flowable.parser.*
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldHaveSingleItem
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource


private const val FILE = "fullelements/full-boundary-events-and-timer.bpmn20.xml"

internal class ExtraBoundaryEventPropertiesTest {

    private val parser = FlowableParser()

    @ParameterizedTest
    @CsvSource(
        "process.body.timerStartEvent[?(@.id.id == 'timerStartDurationEvent')].timerEventDefinition.timeDuration.timeDuration,timerStartDurationEvent,TIME_DURATION,PT5M",
        "process.body.intermediateTimerCatchingEvent[?(@.id.id == 'timerIntermediateDurationEvent')].timerEventDefinition.timeDuration.timeDuration,timerIntermediateDurationEvent,TIME_DURATION,PT5M",
        "process.body.boundaryTimerEvent[?(@.id.id == 'TimerBoundaryDurationEvent')].timerEventDefinition.timeDuration.timeDuration,TimerBoundaryDurationEvent,TIME_DURATION,PT5M"
    )
    fun `TimerStartEvent,TimerBoundaryEvent,TimerIntermediateEvent with date extra essential properties are parseable and updatable`(jsonPath: String, id: String, prop: PropertyType, expectedValue: String) {
        verifyObject(jsonPath, expectedValue, id, prop)
    }

    @ParameterizedTest
    @CsvSource(
        "process.body.conditionalStartEvent[?(@.id.id == 'ConditionalStartEvent')].conditionalEventDefinition.condition.script,ConditionalStartEvent,EVENT_CONDITION,Condition1",
        "process.body.intermediateConditionalCatchingEvent[?(@.id.id == 'ConditionalIntermediateCatchEvent')].conditionalEventDefinition.condition.script,ConditionalIntermediateCatchEvent,EVENT_CONDITION,Condition1",
        "process.body.boundaryConditionalEvent[?(@.id.id == 'ConditionalBoundaryExpressionEvent')].conditionalEventDefinition.condition.script,ConditionalBoundaryExpressionEvent,EVENT_CONDITION,Condition1",
    )
    fun `ConditionalStartEvent,ConditionalBoundaryEvent,ConditionalIntermediateEvent with date extra essential properties are parseable and updatable`(jsonPath: String, id: String, prop: PropertyType, expectedValue: String) {
        verifyObject(jsonPath, expectedValue, id, prop)
    }

    @ParameterizedTest
    @CsvSource(
        "process.body.messageStartEvent[?(@.id.id == 'MessageStartEvent')].messageEventDefinition.messageRef,MessageStartEvent,MESSAGE_REF,Message1",
        "process.body.boundaryMessageEvent[?(@.id.id == 'MessageBoundaryEvent')].messageEventDefinition.messageRef,MessageBoundaryEvent,MESSAGE_REF,Message1",
        "process.body.intermediateMessageCatchingEvent[?(@.id.id == 'MessageIntermediateCatchEvent')].messageEventDefinition.messageRef,MessageIntermediateCatchEvent,MESSAGE_REF,Message1",
    )
    fun `MessageStartEvent,MessageBoundaryEvent,MessageIntermediateCatchingEvent with date extra essential properties are parseable and updatable`(jsonPath: String, id: String, prop: PropertyType, expectedValue: String) {
        verifyObject(jsonPath, expectedValue, id, prop)
    }

    @ParameterizedTest
    @CsvSource(
        "process.body.boundaryEscalationEvent[?(@.id.id == 'EscalationBoundaryEvent')].escalationEventDefinition.escalationRef,EscalationBoundaryEvent,ESCALATION_REF,Escalation1",
        "process.body.intermediateEscalationThrowingEvent[?(@.id.id == 'EscalationIntermediateThrowEvent')].escalationEventDefinition.escalationRef,EscalationIntermediateThrowEvent,ESCALATION_REF,Escalation1",
        "process.body.escalationEndEvent[?(@.id.id == 'EscalationEndEvent')].escalationEventDefinition.escalationRef,EscalationEndEvent,ESCALATION_REF,Escalation1",
    )
    fun `EscalationStartEvent,EscalationBoundaryEvent,EscalationIntermediateCatchingEvent with date extra essential properties are parseable and updatable`(jsonPath: String, id: String, prop: PropertyType, expectedValue: String) {
        verifyObject(jsonPath, expectedValue, id, prop)
    }

    @ParameterizedTest
    @CsvSource(
        "process.body.boundaryErrorEvent[?(@.id.id == 'ErrorBoundaryEvent')].errorEventDefinition.errorRef,ErrorBoundaryEvent,ERROR_REF,Error1",
        "process.body.errorEndEvent[?(@.id.id == 'ErrorEndEvent')].errorEventDefinition.errorRef,ErrorEndEvent,ERROR_REF,Error1",
    )
    fun `ErrorStartEvent,ErrorBoundaryEvent,ErrorIntermediateCatchingEvent with date extra essential properties are parseable and updatable`(jsonPath: String, id: String, prop: PropertyType, expectedValue: String) {
        verifyObject(jsonPath, expectedValue, id, prop)
    }

    @ParameterizedTest
    @CsvSource(
        "process.body.signalStartEvent[?(@.id.id == 'SignalStartEvent')].signalEventDefinition.signalRef,SignalStartEvent,SIGNAL_REF,Signal1",
        "process.body.boundarySignalEvent[?(@.id.id == 'SignalBoundaryEvent')].signalEventDefinition.signalRef,SignalBoundaryEvent,SIGNAL_REF,Signal1",
        "process.body.intermediateSignalCatchingEvent[?(@.id.id == 'SignalIntermediateCatchEvent')].signalEventDefinition.signalRef,SignalIntermediateCatchEvent,SIGNAL_REF,Signal1",
        "process.body.intermediateSignalThrowingEvent[?(@.id.id == 'SignalIntermediateThrowEvent')].signalEventDefinition.signalRef,SignalIntermediateThrowEvent,SIGNAL_REF,Signal1",
    )
    fun `SignalStartEvent,SignalBoundaryEvent,SignalIntermediateThrowEvent,SignalIntermediateCatchingEvent with date extra essential properties are parseable and updatable`(jsonPath: String, id: String, prop: PropertyType, expectedValue: String) {
        verifyObject(jsonPath, expectedValue, id, prop)
    }

    private fun verifyObject(jsonPath: String, expectedValue: String, id: String, prop: PropertyType) {
        val processObject = parser.parse(FILE.asResource()!!)

        val valueFromBpmn: List<String> = JsonPath.read(jacksonObjectMapper().writeValueAsString(processObject), jsonPath)
        valueFromBpmn.shouldHaveSingleItem().shouldBeEqualTo(expectedValue)
        val props = processObject.propsOf(id)

        props[prop]!!.shouldBeEqualTo(Property(expectedValue))
        readAndUpdate(id, prop, "TEST").propsOf(id)[prop]!!.shouldBeEqualTo(Property("TEST"))
    }

    private fun readAndUpdate(elemId: String, property: PropertyType, newValue: String): BpmnProcessObject {
        return readAndUpdateProcess(parser, FILE, updateEvt(elemId, property, newValue))
    }
}