package com.valb3r.bpmn.intellij.plugin.render

import com.intellij.openapi.util.IconLoader
import java.nio.charset.StandardCharsets
import javax.swing.Icon

interface IconProvider {
    val undo: Icon
    val redo: Icon
    val gear: Icon
    val script: Icon
    val businessRule: Icon
    val receive: Icon
    val user: Icon
    val camel: Icon
    val http: Icon
    val mule: Icon
    val decision: Icon
    val shell: Icon
    val tilde: Icon
    val exclusiveGateway: String
    val parallelGateway: String
    val inclusiveGateway: String
    val eventGateway: String
    val timerCatchEvent: String
    val messageCatchEvent: String
    val signalCatchEvent: String
    val conditionalCatchEvent: String
    val noneThrowEvent: String
    val signalThrowEvent: String
    val escalationThrowEvent: String
    val endEvent: String
    val errorEndEvent: String
    val cancelEndEvent: String
    val escalationEndEvent: String
    val terminateEndEvent: String
    val sequence: String
    val recycleBin: String
}

private fun String.asResource(): String? = DefaultBpmnProcessRenderer::class.java.classLoader.getResource(this)?.readText(StandardCharsets.UTF_8)

data class IconProviderImpl(
        override val undo: Icon = IconLoader.getIcon("/icons/actions/undo.png"),
        override val redo: Icon = IconLoader.getIcon("/icons/actions/redo.png"),
        override val gear: Icon = IconLoader.getIcon("/icons/ui-icons/gear.png"),
        override val script: Icon = IconLoader.getIcon("/icons/ui-icons/script.png"),
        override val businessRule: Icon = IconLoader.getIcon("/icons/ui-icons/business-rule.png"),
        override val receive: Icon = IconLoader.getIcon("/icons/ui-icons/receive.png"),
        override val user: Icon = IconLoader.getIcon("/icons/ui-icons/user.png"),
        override val camel: Icon = IconLoader.getIcon("/icons/ui-icons/camel.png"),
        override val http: Icon = IconLoader.getIcon("/icons/ui-icons/http.png"),
        override val mule: Icon = IconLoader.getIcon("/icons/ui-icons/mule.png"),
        override val decision: Icon = IconLoader.getIcon("/icons/ui-icons/decision.png"),
        override val shell: Icon = IconLoader.getIcon("/icons/ui-icons/shell.png"),
        override val tilde: Icon = IconLoader.getIcon("/icons/ui-icons/tilde.png"),
        override val exclusiveGateway: String = "/icons/ui-icons/svg/exclusive-gateway.svg".asResource()!!,
        override val parallelGateway: String = "/icons/ui-icons/svg/parallel-gateway.svg".asResource()!!,
        override val inclusiveGateway: String = "/icons/ui-icons/svg/inclusive-gateway.svg".asResource()!!,
        override val eventGateway: String = "/icons/ui-icons/svg/event-gateway.svg".asResource()!!,
        override val timerCatchEvent: String = "/icons/ui-icons/svg/timer-catch-event.svg".asResource()!!,
        override val messageCatchEvent: String = "/icons/ui-icons/svg/message-catch-event.svg".asResource()!!,
        override val signalCatchEvent: String = "/icons/ui-icons/svg/signal-catch-event.svg".asResource()!!,
        override val conditionalCatchEvent: String = "/icons/ui-icons/svg/conditional-catch-event.svg".asResource()!!,
        override val noneThrowEvent: String = "/icons/ui-icons/svg/none-throw-event.svg".asResource()!!,
        override val signalThrowEvent: String = "/icons/ui-icons/svg/signal-throw-event.svg".asResource()!!,
        override val endEvent: String = "/icons/ui-icons/svg/end-event.svg".asResource()!!,
        override val errorEndEvent: String = "/icons/ui-icons/svg/error-end-event.svg".asResource()!!,
        override val cancelEndEvent: String = "/icons/ui-icons/svg/cancel-end-event.svg".asResource()!!,
        override val escalationEndEvent: String = "/icons/ui-icons/svg/escalation-end-event.svg".asResource()!!,
        override val terminateEndEvent: String = "/icons/ui-icons/svg/terminate-end-event.svg".asResource()!!,
        override val escalationThrowEvent: String = "/icons/ui-icons/svg/escalation-throw-event.svg".asResource()!!,
        override val sequence: String = "/icons/ui-icons/svg/sequence.svg".asResource()!!,
        override val recycleBin: String = "/icons/ui-icons/svg/recycle-bin.svg".asResource()!!
): IconProvider