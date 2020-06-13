package com.valb3r.bpmn.intellij.plugin.flowable.parser.nodes.process.subelements

import com.tickaroo.tikxml.annotation.Attribute
import com.tickaroo.tikxml.annotation.Element
import com.tickaroo.tikxml.annotation.PropertyElement
import com.tickaroo.tikxml.annotation.Xml

@Xml
data class TimerEventDefinition(
        @Attribute val timeDate: String? = null
)

@Xml
data class SignalEventDefinition(
        @Attribute val signalRef: String? = null
)

@Xml
data class MessageEventDefinition(
        @Attribute val messageRef: String? = null
)

@Xml
data class ErrorEventDefinition(
        @Attribute val errorRef: String? = null
)

@Xml
data class CancelEventDefinition(
        @Attribute val cancelRef: String? = null // TODO - what it cancels?
)

@Xml
data class CompensateEventDefinition(
        @Attribute val activityRef: String? = null
)

@Xml
data class ConditionalEventDefinition(
        @Attribute val condition: String? = null
)

@Xml
data class TerminateEventDefinition(
        @Attribute val terminateAll: Boolean? = null
)

@Xml
data class EscalationEventDefinition(
        @Attribute val escalationRef: String? = null
)


@Xml
data class ExtensionElements(
        @Element val out: List<OutExtensionElement>?
)

@Xml
data class OutExtensionElement(@Attribute val source: String?, @Attribute val target: String?)

@Xml
data class CompletionCondition(
        @PropertyElement(writeAsCData = true) val condition: String? = null
)