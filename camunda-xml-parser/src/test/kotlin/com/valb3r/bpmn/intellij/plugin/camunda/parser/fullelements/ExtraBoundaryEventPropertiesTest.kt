package com.valb3r.bpmn.intellij.plugin.camunda.parser.fullelements

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.jayway.jsonpath.JsonPath
import com.valb3r.bpmn.intellij.plugin.bpmn.api.BpmnProcessObject
import com.valb3r.bpmn.intellij.plugin.bpmn.api.PropertyTable
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.BpmnElementId
import com.valb3r.bpmn.intellij.plugin.bpmn.api.info.Property
import com.valb3r.bpmn.intellij.plugin.bpmn.api.info.PropertyType
import com.valb3r.bpmn.intellij.plugin.camunda.parser.*
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldHaveSingleItem
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

private const val FILE = "fullelements/full-boundary-events-and-timer.bpmn"

internal class ExtraBoundaryEventPropertiesTest {

    private val parser = CamundaParser()
    private val formalDefinition = "bpmn:tFormalExpression"

    @ParameterizedTest
    @CsvSource(
        "process.body.timerStartEvent[?(@.id.id == 'timerStartDateEvent')].timerEventDefinition.timeDate.timeDate,timerStartDateEvent,TIME_DATE,TIME_DATE_EXPRESSION_TYPE,2019-10-01T12:00Z",
        "process.body.timerStartEvent[?(@.id.id == 'timerStartDurationEvent')].timerEventDefinition.timeDuration.timeDuration,timerStartDurationEvent,TIME_DURATION,TIME_DURATION_EXPRESSION_TYPE,PT15S",
        "process.body.timerStartEvent[?(@.id.id == 'timerStartCycleEvent')].timerEventDefinition.timeCycle.timeCycle,timerStartCycleEvent,TIME_CYCLE,TIME_CYCLE_EXPRESSION_TYPE,R5/PT10S"
    )
    fun `TimerStartEvent with date extra essential properties are parseable and updatable`(jsonPath: String, id: String, prop: PropertyType, propType: PropertyType, expectedValue: String) {
        val processObject = parser.parse(FILE.asResource()!!)

        val valueFromBpmn: List<String> = JsonPath.read(jacksonObjectMapper().writeValueAsString(processObject), jsonPath)
        valueFromBpmn.shouldHaveSingleItem().shouldBeEqualTo(expectedValue)
        val props = processObject.propsOf(id)

        props[prop]!!.shouldBeEqualTo(Property(expectedValue))
        props[propType]!!.shouldBeEqualTo(Property(formalDefinition))
        readAndUpdate(id, prop, "TEST").propsOf(id)[prop]!!.shouldBeEqualTo(Property("TEST"))
        readAndUpdate(id, propType, "TEST").propsOf(id)[propType]!!.shouldBeEqualTo(Property("TEST"))
    }

    @ParameterizedTest
    @CsvSource(
        "process.body.boundaryTimerEvent[?(@.id.id == 'TimerBoundaryDateEvent')].timerEventDefinition.timeDate.timeDate,TimerBoundaryDateEvent,TIME_DATE,TIME_DATE_EXPRESSION_TYPE,2019-10-01T12:00Z",
        "process.body.boundaryTimerEvent[?(@.id.id == 'TimerBoundaryDurationEvent')].timerEventDefinition.timeDuration.timeDuration,TimerBoundaryDurationEvent,TIME_DURATION,TIME_DURATION_EXPRESSION_TYPE,PT15S",
        "process.body.boundaryTimerEvent[?(@.id.id == 'TimerBoundaryCycleEvent')].timerEventDefinition.timeCycle.timeCycle,TimerBoundaryCycleEvent,TIME_CYCLE,TIME_CYCLE_EXPRESSION_TYPE,R5/PT10S"
    )
    fun `TimerBoundaryEvent with date extra essential properties are parseable and updatable`(jsonPath: String, id: String, prop: PropertyType, propType: PropertyType, expectedValue: String) {
        val processObject = parser.parse(FILE.asResource()!!)

        val valueFromBpmn: List<String> = JsonPath.read(jacksonObjectMapper().writeValueAsString(processObject), jsonPath)
        valueFromBpmn.shouldHaveSingleItem().shouldBeEqualTo(expectedValue)
        val props = processObject.propsOf(id)

        props[prop]!!.shouldBeEqualTo(Property(expectedValue))
        props[propType]!!.shouldBeEqualTo(Property(formalDefinition))
        readAndUpdate(id, prop, "TEST").propsOf(id)[prop]!!.shouldBeEqualTo(Property("TEST"))
        readAndUpdate(id, propType, "TEST").propsOf(id)[propType]!!.shouldBeEqualTo(Property("TEST"))
    }

    @ParameterizedTest
    @CsvSource(
        "process.body.intermediateTimerCatchingEvent[?(@.id.id == 'timerIntermediateDateEvent')].timerEventDefinition.timeDate.timeDate,TimerBoundaryDateEvent,TIME_DATE,TIME_DATE_EXPRESSION_TYPE,2019-10-01T12:00Z",
        "process.body.intermediateTimerCatchingEvent[?(@.id.id == 'timerIntermediateDurationEvent')].timerEventDefinition.timeDuration.timeDuration,timerIntermediateDurationEvent,TIME_DURATION,TIME_DURATION_EXPRESSION_TYPE,PT15S",
        "process.body.intermediateTimerCatchingEvent[?(@.id.id == 'timerIntermediateCycleEvent')].timerEventDefinition.timeCycle.timeCycle,timerIntermediateCycleEvent,TIME_CYCLE,TIME_CYCLE_EXPRESSION_TYPE,R5/PT10S"
    )
    fun `TimerIntermediateEvent with date extra essential properties are parseable and updatable`(jsonPath: String, id: String, prop: PropertyType, propType: PropertyType, expectedValue: String) {
        val processObject = parser.parse(FILE.asResource()!!)

        val valueFromBpmn: List<String> = JsonPath.read(jacksonObjectMapper().writeValueAsString(processObject), jsonPath)
        valueFromBpmn.shouldHaveSingleItem().shouldBeEqualTo(expectedValue)
        val props = processObject.propsOf(id)

        props[prop]!!.shouldBeEqualTo(Property(expectedValue))
        props[propType]!!.shouldBeEqualTo(Property(formalDefinition))
        readAndUpdate(id, prop, "TEST").propsOf(id)[prop]!!.shouldBeEqualTo(Property("TEST"))
        readAndUpdate(id, propType, "TEST").propsOf(id)[propType]!!.shouldBeEqualTo(Property("TEST"))
    }

    @ParameterizedTest
    @CsvSource(
        "process.body.conditionalStartEvent[?(@.id.id == 'ConditionalStartEvent')].conditionalEventDefinition.condition.script,ConditionalStartEvent,EVENT_CONDITION,EVENT_CONDITION_TYPE,anExpr",
        "process.body.conditionalStartEvent[?(@.id.id == 'ConditionalStartEventScript')].conditionalEventDefinition.condition.script,ConditionalStartEventScript,EVENT_CONDITION,EVENT_CONDITION_TYPE,aScript",
    )
    fun `ConditionalStartEvent with date extra essential properties are parseable and updatable`(jsonPath: String, id: String, prop: PropertyType, propType: PropertyType, expectedValue: String) {
        val processObject = parser.parse(FILE.asResource()!!)

        val valueFromBpmn: List<String> = JsonPath.read(jacksonObjectMapper().writeValueAsString(processObject), jsonPath)
        valueFromBpmn.shouldHaveSingleItem().shouldBeEqualTo(expectedValue)
        val props = processObject.propsOf(id)

        props[prop]!!.shouldBeEqualTo(Property(expectedValue))
        props[propType]!!.shouldBeEqualTo(Property(formalDefinition))
        readAndUpdate(id, prop, "TEST").propsOf(id)[prop]!!.shouldBeEqualTo(Property("TEST"))
        readAndUpdate(id, propType, "TEST").propsOf(id)[propType]!!.shouldBeEqualTo(Property("TEST"))
    }

    @ParameterizedTest
    @CsvSource(
        "process.body.boundaryConditionalEvent[?(@.id.id == 'ConditionalBoundaryExpressionEvent')].conditionalEventDefinition.condition.script,ConditionalBoundaryExpressionEvent,EVENT_CONDITION,EVENT_CONDITION_TYPE,anExpr",
        "process.body.boundaryConditionalEvent[?(@.id.id == 'ConditionalBoundaryScriptEvent')].conditionalEventDefinition.condition.script,ConditionalBoundaryScriptEvent,EVENT_CONDITION,EVENT_CONDITION_TYPE,aScript",
    )
    fun `ConditionalBoundaryEvent with date extra essential properties are parseable and updatable`(jsonPath: String, id: String, prop: PropertyType, propType: PropertyType, expectedValue: String) {
        val processObject = parser.parse(FILE.asResource()!!)

        val valueFromBpmn: List<String> = JsonPath.read(jacksonObjectMapper().writeValueAsString(processObject), jsonPath)
        valueFromBpmn.shouldHaveSingleItem().shouldBeEqualTo(expectedValue)
        val props = processObject.propsOf(id)

        props[prop]!!.shouldBeEqualTo(Property(expectedValue))
        props[propType]!!.shouldBeEqualTo(Property(formalDefinition))
        readAndUpdate(id, prop, "TEST").propsOf(id)[prop]!!.shouldBeEqualTo(Property("TEST"))
        readAndUpdate(id, propType, "TEST").propsOf(id)[propType]!!.shouldBeEqualTo(Property("TEST"))
    }

    @ParameterizedTest
    @CsvSource(
        "process.body.intermediateConditionalCatchingEvent[?(@.id.id == 'ConditionalIntermediateCatchEvent')].conditionalEventDefinition.condition.script,ConditionalIntermediateCatchEvent,EVENT_CONDITION,EVENT_CONDITION_TYPE,anExpr",
        "process.body.intermediateConditionalCatchingEvent[?(@.id.id == 'ConditionalIntermediateCatchEventScript')].conditionalEventDefinition.condition.script,ConditionalIntermediateCatchEventScript,EVENT_CONDITION,EVENT_CONDITION_TYPE,aScript",
    )
    fun `ConditionalIntermediateEvent with date extra essential properties are parseable and updatable`(jsonPath: String, id: String, prop: PropertyType, propType: PropertyType, expectedValue: String) {
        val processObject = parser.parse(FILE.asResource()!!)

        val valueFromBpmn: List<String> = JsonPath.read(jacksonObjectMapper().writeValueAsString(processObject), jsonPath)
        valueFromBpmn.shouldHaveSingleItem().shouldBeEqualTo(expectedValue)
        val props = processObject.propsOf(id)

        props[prop]!!.shouldBeEqualTo(Property(expectedValue))
        props[propType]!!.shouldBeEqualTo(Property(formalDefinition))
        readAndUpdate(id, prop, "TEST").propsOf(id)[prop]!!.shouldBeEqualTo(Property("TEST"))
        readAndUpdate(id, propType, "TEST").propsOf(id)[propType]!!.shouldBeEqualTo(Property("TEST"))
    }

    @ParameterizedTest
    @CsvSource(
        "process.body.messageStartEvent[?(@.id.id == 'MessageStartEvent')].messageEventDefinition.messageRef,MessageStartEvent,MESSAGE_REF,Message_1q041lj",
        "process.body.boundaryMessageEvent[?(@.id.id == 'MessageBoundaryEvent')].messageEventDefinition.messageRef,MessageBoundaryEvent,MESSAGE_REF,Message_1q041lj",
        "process.body.intermediateMessageCatchingEvent[?(@.id.id == 'MessageIntermediateCatchEvent')].messageEventDefinition.messageRef,MessageIntermediateCatchEvent,MESSAGE_REF,Message_1q041lj",
    )
    fun `MessageStartEvent,MessageBoundaryEvent,MessageIntermediateCatchingEvent with date extra essential properties are parseable and updatable`(jsonPath: String, id: String, prop: PropertyType, expectedValue: String) {
        val processObject = parser.parse(FILE.asResource()!!)

        val valueFromBpmn: List<String> = JsonPath.read(jacksonObjectMapper().writeValueAsString(processObject), jsonPath)
        valueFromBpmn.shouldHaveSingleItem().shouldBeEqualTo(expectedValue)
        val props = processObject.propsOf(id)

        props[prop]!!.shouldBeEqualTo(Property(expectedValue))
        readAndUpdate(id, prop, "TEST").propsOf(id)[prop]!!.shouldBeEqualTo(Property("TEST"))
    }

    @Test
    fun `EscalationBoundaryEvent extra essential properties`() {

    }

    @Test
    fun `ErrorBoundaryEvent extra essential properties`() {

    }

    @Test
    fun `SignalBoundaryEvent extra essential properties`() {

    }

    private fun readAndUpdate(elemId: String, property: PropertyType, newValue: String): BpmnProcessObject {
        return readAndUpdateProcess(parser, FILE, updateEvt(elemId, property, newValue))
    }

    private fun BpmnProcessObject.propsOf(elemId: String): PropertyTable {
        return this.toView(CamundaObjectFactory()).elemPropertiesByElementId[BpmnElementId(elemId)]!!
    }
}