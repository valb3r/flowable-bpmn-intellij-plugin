package com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.events.props

data class BpmnTimerEventDefinition(
    val timeDate: TimeDate?,
    val timeDuration: TimeDuration?,
    val timeCycle: TimeCycle?
) {
    data class TimeDate(val type: String?, val timeDate: String?)
    data class TimeDuration(val type: String?, val timeDuration: String?)
    data class TimeCycle(val type: String?, val timeCycle: String?)
}