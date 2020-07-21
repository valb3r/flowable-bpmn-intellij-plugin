package com.valb3r.bpmn.intellij.plugin.render

import com.intellij.openapi.util.IconLoader
import java.nio.charset.StandardCharsets
import java.util.concurrent.atomic.AtomicReference
import javax.swing.Icon

interface IconProvider {
    val undo: Icon
    val redo: Icon
    val gear: Icon
    val script: Icon
    val businessRule: Icon
    val receive: Icon
    val manual: Icon
    val user: Icon
    val camel: Icon
    val http: Icon
    val mail: Icon
    val mule: Icon
    val decision: Icon
    val shell: Icon
    val tilde: Icon
    val dragToResizeTop: Icon
    val dragToResizeBottom: Icon
    val sequence: Icon
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
    val startEvent: String
    val conditionalStartEvent: String
    val messageStartEvent: String
    val errorStartEvent: String
    val escalationStartEvent: String
    val timerStartEvent: String
    val signalStartEvent: String
    val boundaryCancelEvent: String
    val boundaryCompensationEvent: String
    val boundaryConditionalEvent: String
    val boundaryErrorEvent: String
    val boundaryEscalationEvent: String
    val boundaryMessageEvent: String
    val boundarySignalEvent: String
    val boundaryTimerEvent: String
    val recycleBin: String
}

private val currentIconProvider = AtomicReference<IconProvider>()

fun currentIconProvider(): IconProvider {
    return currentIconProvider.updateAndGet {
        if (null == it) {
            return@updateAndGet IconProviderImpl()
        }

        return@updateAndGet it
    }
}

private fun String.asResource(): String? = DefaultBpmnProcessRenderer::class.java.classLoader.getResource(this)?.readText(StandardCharsets.UTF_8)

data class IconProviderImpl(
        override val undo: Icon = IconLoader.getIcon("/icons/actions/undo.png"),
        override val redo: Icon = IconLoader.getIcon("/icons/actions/redo.png"),
        override val gear: Icon = IconLoader.getIcon("/icons/ui-icons/gear.png"),
        override val script: Icon = IconLoader.getIcon("/icons/ui-icons/script.png"),
        override val businessRule: Icon = IconLoader.getIcon("/icons/ui-icons/business-rule.png"),
        override val receive: Icon = IconLoader.getIcon("/icons/ui-icons/receive.png"),
        override val manual: Icon = IconLoader.getIcon("/icons/ui-icons/manual.png"),
        override val user: Icon = IconLoader.getIcon("/icons/ui-icons/user.png"),
        override val camel: Icon = IconLoader.getIcon("/icons/ui-icons/camel.png"),
        override val http: Icon = IconLoader.getIcon("/icons/ui-icons/http.png"),
        override val mail: Icon = IconLoader.getIcon("/icons/ui-icons/mail.png"),
        override val mule: Icon = IconLoader.getIcon("/icons/ui-icons/mule.png"),
        override val decision: Icon = IconLoader.getIcon("/icons/ui-icons/decision.png"),
        override val shell: Icon = IconLoader.getIcon("/icons/ui-icons/shell.png"),
        override val tilde: Icon = IconLoader.getIcon("/icons/ui-icons/tilde.png"),
        override val dragToResizeTop: Icon = IconLoader.getIcon("/icons/ui-icons/drag-to-resize-top.png"),
        override val dragToResizeBottom: Icon = IconLoader.getIcon("/icons/ui-icons/drag-to-resize-bottom.png"),
        override val sequence: Icon = IconLoader.getIcon("/icons/ui-icons/sequence.png"),
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
        override val startEvent: String = "/icons/ui-icons/svg/start-event.svg".asResource()!!,
        override val conditionalStartEvent: String = "/icons/ui-icons/svg/conditional-start-event.svg".asResource()!!,
        override val messageStartEvent: String = "/icons/ui-icons/svg/message-start-event.svg".asResource()!!,
        override val errorStartEvent: String = "/icons/ui-icons/svg/error-start-event.svg".asResource()!!,
        override val escalationStartEvent: String = "/icons/ui-icons/svg/escalation-start-event.svg".asResource()!!,
        override val timerStartEvent: String = "/icons/ui-icons/svg/timer-start-event.svg".asResource()!!,
        override val signalStartEvent: String = "/icons/ui-icons/svg/signal-start-event.svg".asResource()!!,
        override val boundaryCancelEvent: String = "/icons/ui-icons/svg/cancel-boundary-event.svg".asResource()!!,
        override val boundaryCompensationEvent: String = "/icons/ui-icons/svg/compensation-boundary-event.svg".asResource()!!,
        override val boundaryConditionalEvent: String = "/icons/ui-icons/svg/conditional-boundary-event.svg".asResource()!!,
        override val boundaryErrorEvent: String = "/icons/ui-icons/svg/error-boundary-event.svg".asResource()!!,
        override val boundaryEscalationEvent: String = "/icons/ui-icons/svg/escalation-boundary-event.svg".asResource()!!,
        override val boundaryMessageEvent: String = "/icons/ui-icons/svg/message-boundary-event.svg".asResource()!!,
        override val boundarySignalEvent: String = "/icons/ui-icons/svg/signal-boundary-event.svg".asResource()!!,
        override val boundaryTimerEvent: String = "/icons/ui-icons/svg/timer-boundary-event.svg".asResource()!!,
        override val recycleBin: String = "/icons/ui-icons/svg/recycle-bin.svg".asResource()!!
): IconProvider