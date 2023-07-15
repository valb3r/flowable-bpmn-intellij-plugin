package com.valb3r.bpmn.intellij.plugin.flowable.parser.nodes.process.nested.definitions

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlCData
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlText
import com.valb3r.bpmn.intellij.plugin.bpmn.parser.core.CDATA_FIELD

data class TimerEventDefinition(
    val timeDate: TimeDate? = null,
    val timeDuration: TimeDuration? = null,
    val timeCycle: TimeCycle? = null
) {
    data class TimeDate(
        @JacksonXmlProperty(isAttribute = true) val type: String? = null,
        @JsonProperty(CDATA_FIELD) @JacksonXmlText @JacksonXmlCData val timeDate: String? = null
    )
    data class TimeDuration(
        @JacksonXmlProperty(isAttribute = true) val type: String? = null,
        @JsonProperty(CDATA_FIELD) @JacksonXmlText @JacksonXmlCData val timeDuration: String? = null)
    data class TimeCycle(
        @JacksonXmlProperty(isAttribute = true) val type: String? = null,
        @JsonProperty(CDATA_FIELD) @JacksonXmlText @JacksonXmlCData val timeCycle: String? = null
    )
}