package com.valb3r.bpmn.intellij.plugin.flowable.parser.fullelements

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.jayway.jsonpath.JsonPath
import com.valb3r.bpmn.intellij.plugin.bpmn.api.BpmnProcessObject
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.BpmnElementId
import com.valb3r.bpmn.intellij.plugin.bpmn.api.info.Property
import com.valb3r.bpmn.intellij.plugin.bpmn.api.info.PropertyType
import com.valb3r.bpmn.intellij.plugin.flowable.parser.*
import com.valb3r.bpmn.intellij.plugin.flowable.parser.testevents.BooleanValueUpdatedEvent
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldHaveSingleItem
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.util.stream.Stream

private const val FILE = "fullelements/error-definition-process.bpmn20.xml"

internal class ErrorDefinitionEventPropertiesTest {

    private val parser = FlowableParser()

    @ParameterizedTest
    @MethodSource("errorDefinitionEventAttributes")
    fun `Error event definition read and write attributes`(jsonPath: String, id: String, prop: PropertyType, expectedValue: Any, newValue: Any) {
        verifyAttribute(jsonPath, expectedValue, id, prop, newValue)
    }

    private fun verifyAttribute(jsonPath: String, expectedValue: Any, id: String, prop: PropertyType, newValue: Any) {
        val processObject = parser.parse(FILE.asResource()!!)

        val valueFromBpmn: List<Map<String, Any>> =
            JsonPath.read(jacksonObjectMapper().writeValueAsString(processObject), jsonPath)
        valueFromBpmn.shouldHaveSingleItem().shouldBeEqualTo(expectedValue)
        val props = processObject.propsOf(id)

        props[prop]!!.shouldBeEqualTo(Property(expectedValue))
        readAndUpdate(id, prop, newValue).propsOf(id)[prop]!!.shouldBeEqualTo(Property(newValue))
    }

    private fun readAndUpdate(elemId: String, property: PropertyType, newValue: Any): BpmnProcessObject {
        when (newValue) {
            is String -> {
                return readAndUpdateProcess(parser, FILE, updateEvt(elemId, property, newValue))
            }

            is Boolean -> {
                return readAndUpdateProcess(
                    parser,
                    FILE,
                    BooleanValueUpdatedEvent(BpmnElementId(elemId), property, newValue)
                )
            }

            else -> {
                throw RuntimeException("Unsupported newValue")
            }
        }
    }


    companion object {
        @JvmStatic
        fun errorDefinitionEventAttributes(): Stream<Arguments> {
            return Stream.of(
                Arguments.of("process.body.errorStartEvent[?(@.id.id == 'StartErrorEvent')].errorEventDefinition.errorRef","StartErrorEvent",
                    PropertyType.ERROR_REF, "errCode1", "newValue"),
                Arguments.of("process.body.errorStartEvent[?(@.id.id == 'StartErrorEvent')].errorEventDefinition.errorVariableName","StartErrorEvent",
                    PropertyType.ERROR_VARIABLE_NAME, "errVarName1", "newValue"),
                Arguments.of("process.body.errorStartEvent[?(@.id.id == 'StartErrorEvent')].errorEventDefinition.errorVariableLocalScope","StartErrorEvent",
                    PropertyType.ERROR_VARIABLE_LOCAL_SCOPE, true, false),
                Arguments.of("process.body.errorStartEvent[?(@.id.id == 'StartErrorEvent')].errorEventDefinition.errorVariableTransient","StartErrorEvent",
                    PropertyType.ERROR_VARIABLE_TRANSIENT, true, false),
                Arguments.of("process.body.boundaryErrorEvent[?(@.id.id == 'IntermediateErrorEventBoundary')].errorEventDefinition.errorRef","IntermediateErrorEventBoundary",
                    PropertyType.ERROR_REF, "errCode2", "newValue"),
                Arguments.of("process.body.boundaryErrorEvent[?(@.id.id == 'IntermediateErrorEventBoundary')].errorEventDefinition.errorVariableName","IntermediateErrorEventBoundary",
                    PropertyType.ERROR_VARIABLE_NAME, "errVarName2", "newValue"),
                Arguments.of("process.body.boundaryErrorEvent[?(@.id.id == 'IntermediateErrorEventBoundary')].errorEventDefinition.errorVariableLocalScope","IntermediateErrorEventBoundary",
                    PropertyType.ERROR_VARIABLE_LOCAL_SCOPE, true, false),
                Arguments.of("process.body.boundaryErrorEvent[?(@.id.id == 'IntermediateErrorEventBoundary')].errorEventDefinition.errorVariableTransient","IntermediateErrorEventBoundary",
                    PropertyType.ERROR_VARIABLE_TRANSIENT, true, false)
            )
        }
    }

}