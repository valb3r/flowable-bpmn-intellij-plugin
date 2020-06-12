package com.valb3r.bpmn.intellij.plugin.flowable.parser.nodes.process.subevents

data class TimerEventDefinition(
        val timeDate: String? = null
)

data class SignalEventDefinition(
        val signalRef: String? = null
)

data class MessageEventDefinition(
        val messageRef: String? = null
)

data class ErrorEventDefinition(
        val errorRef: String? = null
)

data class CancelEventDefinition(
        val cancelRef: String? = null // TODO - what it cancels?
)

data class CompensateEventDefinition(
        val activityRef: String? = null
)

data class ConditionalEventDefinition(
        val condition: String? = null
)

data class EscalationEventDefinition(
        val escalationRef: String? = null
)


data class TerminateEventDefinition(
        val terminateAll: Boolean? = null
)