package com.valb3r.bpmn.intellij.plugin.core.render

import com.google.common.hash.Hashing
import com.intellij.openapi.util.IconLoader
import java.nio.charset.StandardCharsets
import java.util.concurrent.atomic.AtomicReference
import javax.swing.Icon

interface IconProvider {
    val undo: Icon
    val redo: Icon
    val centerImage: Icon
    val zoomIn: Icon
    val zoomOut: Icon
    val zoomReset: Icon
    val grid: Icon
    val gridDense: Icon
    val noGrid: Icon
    val gear: Icon
    val triggered: Icon
    val external: Icon
    val script: Icon
    val businessRule: Icon
    val receive: Icon
    val manual: Icon
    val user: Icon
    val camel: Icon
    val http: Icon
    val mail: Icon
    val mule: Icon
    val envelope: Icon
    val decision: Icon
    val shell: Icon
    val tilde: Icon
    val plus: Icon
    val minus: Icon
    val dragToResizeTop: Icon
    val dragToResizeBottom: Icon
    val sequence: Icon
    val anchorOn: Icon
    val anchorOff: Icon
    val exclusiveGateway: SvgIcon
    val parallelGateway: SvgIcon
    val inclusiveGateway: SvgIcon
    val eventGateway: SvgIcon
    val complexGateway: SvgIcon
    val timerCatchEvent: SvgIcon
    val messageCatchEvent: SvgIcon
    val signalCatchEvent: SvgIcon
    val conditionalCatchEvent: SvgIcon
    val intermediateLinkCatchEvent: SvgIcon
    val noneThrowEvent: SvgIcon
    val signalThrowEvent: SvgIcon
    val escalationThrowEvent: SvgIcon
    val endEvent: SvgIcon
    val errorEndEvent: SvgIcon
    val cancelEndEvent: SvgIcon
    val escalationEndEvent: SvgIcon
    val terminateEndEvent: SvgIcon
    val startEvent: SvgIcon
    val conditionalStartEvent: SvgIcon
    val messageStartEvent: SvgIcon
    val errorStartEvent: SvgIcon
    val escalationStartEvent: SvgIcon
    val timerStartEvent: SvgIcon
    val signalStartEvent: SvgIcon
    val boundaryCancelEvent: SvgIcon
    val boundaryCompensationEvent: SvgIcon
    val boundaryConditionalEvent: SvgIcon
    val boundaryErrorEvent: SvgIcon
    val boundaryEscalationEvent: SvgIcon
    val boundaryMessageEvent: SvgIcon
    val boundarySignalEvent: SvgIcon
    val boundaryTimerEvent: SvgIcon
    val unknownShape: SvgIcon
    val recycleBin: SvgIcon
    val wrench: SvgIcon
    val rightAngle: SvgIcon
    val selectParentSequence: SvgIcon
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

// Attempt to solve https://github.com/valb3r/flowable-bpmn-intellij-plugin/issues/217
// Use https://github.com/JetBrains/gradle-intellij-plugin/issues/425#issuecomment-524805655
private fun String.asResource(): SvgIcon {
    val icon = DefaultBpmnProcessRenderer::class.java.getResource(this)?.readText(StandardCharsets.UTF_8)!!
    val hash = Hashing.goodFastHash(64).hashString(icon, StandardCharsets.UTF_8)
    return SvgIcon(icon, hash.asLong())
}

data class IconProviderImpl(
        override val undo: Icon = IconLoader.getIcon("/icons/actions/undo.png", IconProvider::class.java),
        override val redo: Icon = IconLoader.getIcon("/icons/actions/redo.png", IconProvider::class.java),
        override val centerImage: Icon = IconLoader.getIcon("/icons/actions/center_image.png", IconProvider::class.java),
        override val zoomIn: Icon = IconLoader.getIcon("/icons/actions/zoom_in.png", IconProvider::class.java),
        override val zoomOut: Icon = IconLoader.getIcon("/icons/actions/zoom_out.png", IconProvider::class.java),
        override val zoomReset: Icon = IconLoader.getIcon("/icons/actions/zoom_reset.png", IconProvider::class.java),
        override val grid: Icon = IconLoader.getIcon("/icons/actions/small-grid.png", IconProvider::class.java),
        override val gridDense: Icon = IconLoader.getIcon("/icons/actions/grid.png", IconProvider::class.java),
        override val noGrid: Icon = IconLoader.getIcon("/icons/actions/no-grid.png", IconProvider::class.java),
        override val gear: Icon = IconLoader.getIcon("/icons/ui-icons/gear.png", IconProvider::class.java),
        override val triggered: Icon = IconLoader.getIcon("/icons/ui-icons/triggired.png", IconProvider::class.java),
        override val external: Icon = IconLoader.getIcon("/icons/ui-icons/external.png", IconProvider::class.java),
        override val script: Icon = IconLoader.getIcon("/icons/ui-icons/script.png", IconProvider::class.java),
        override val businessRule: Icon = IconLoader.getIcon("/icons/ui-icons/business-rule.png", IconProvider::class.java),
        override val receive: Icon = IconLoader.getIcon("/icons/ui-icons/receive.png", IconProvider::class.java),
        override val manual: Icon = IconLoader.getIcon("/icons/ui-icons/manual.png", IconProvider::class.java),
        override val user: Icon = IconLoader.getIcon("/icons/ui-icons/user.png", IconProvider::class.java),
        override val camel: Icon = IconLoader.getIcon("/icons/ui-icons/camel.png", IconProvider::class.java),
        override val http: Icon = IconLoader.getIcon("/icons/ui-icons/http.png", IconProvider::class.java),
        override val mail: Icon = IconLoader.getIcon("/icons/ui-icons/mail.png", IconProvider::class.java),
        override val mule: Icon = IconLoader.getIcon("/icons/ui-icons/mule.png", IconProvider::class.java),
        override val envelope: Icon = IconLoader.getIcon("/icons/ui-icons/envelope.png", IconProvider::class.java),
        override val decision: Icon = IconLoader.getIcon("/icons/ui-icons/decision.png", IconProvider::class.java),
        override val shell: Icon = IconLoader.getIcon("/icons/ui-icons/shell.png", IconProvider::class.java),
        override val tilde: Icon = IconLoader.getIcon("/icons/ui-icons/tilde.png", IconProvider::class.java),
        override val dragToResizeTop: Icon = IconLoader.getIcon("/icons/ui-icons/drag-to-resize-top.png", IconProvider::class.java),
        override val dragToResizeBottom: Icon = IconLoader.getIcon("/icons/ui-icons/drag-to-resize-bottom.png", IconProvider::class.java),
        override val sequence: Icon = IconLoader.getIcon("/icons/ui-icons/sequence.png", IconProvider::class.java),
        override val anchorOn: Icon = IconLoader.getIcon("/icons/actions/anchor.png", IconProvider::class.java),
        override val anchorOff: Icon = IconLoader.getIcon("/icons/actions/anchor-off.png", IconProvider::class.java),
        override val plus: Icon = IconLoader.getIcon("/icons/ui-icons/plus.png", IconProvider::class.java),
        override val minus: Icon = IconLoader.getIcon("/icons/ui-icons/minus.png", IconProvider::class.java),
        override val exclusiveGateway: SvgIcon = "/icons/ui-icons/svg/exclusive-gateway.svg".asResource()!!,
        override val parallelGateway: SvgIcon = "/icons/ui-icons/svg/parallel-gateway.svg".asResource()!!,
        override val inclusiveGateway: SvgIcon = "/icons/ui-icons/svg/inclusive-gateway.svg".asResource()!!,
        override val eventGateway: SvgIcon = "/icons/ui-icons/svg/event-gateway.svg".asResource()!!,
        override val complexGateway: SvgIcon = "/icons/ui-icons/svg/complex-gateway.svg".asResource()!!,
        override val timerCatchEvent: SvgIcon = "/icons/ui-icons/svg/timer-catch-event.svg".asResource()!!,
        override val messageCatchEvent: SvgIcon = "/icons/ui-icons/svg/message-catch-event.svg".asResource()!!,
        override val signalCatchEvent: SvgIcon = "/icons/ui-icons/svg/signal-catch-event.svg".asResource()!!,
        override val conditionalCatchEvent: SvgIcon = "/icons/ui-icons/svg/conditional-catch-event.svg".asResource()!!,
        override val intermediateLinkCatchEvent: SvgIcon = "/icons/ui-icons/svg/intermediate-link-catch-event.svg".asResource()!!,
        override val noneThrowEvent: SvgIcon = "/icons/ui-icons/svg/none-throw-event.svg".asResource()!!,
        override val signalThrowEvent: SvgIcon = "/icons/ui-icons/svg/signal-throw-event.svg".asResource()!!,
        override val endEvent: SvgIcon = "/icons/ui-icons/svg/end-event.svg".asResource()!!,
        override val errorEndEvent: SvgIcon = "/icons/ui-icons/svg/error-end-event.svg".asResource()!!,
        override val cancelEndEvent: SvgIcon = "/icons/ui-icons/svg/cancel-end-event.svg".asResource()!!,
        override val escalationEndEvent: SvgIcon = "/icons/ui-icons/svg/escalation-end-event.svg".asResource()!!,
        override val terminateEndEvent: SvgIcon = "/icons/ui-icons/svg/terminate-end-event.svg".asResource()!!,
        override val escalationThrowEvent: SvgIcon = "/icons/ui-icons/svg/escalation-throw-event.svg".asResource()!!,
        override val startEvent: SvgIcon = "/icons/ui-icons/svg/start-event.svg".asResource()!!,
        override val conditionalStartEvent: SvgIcon = "/icons/ui-icons/svg/conditional-start-event.svg".asResource()!!,
        override val messageStartEvent: SvgIcon = "/icons/ui-icons/svg/message-start-event.svg".asResource()!!,
        override val errorStartEvent: SvgIcon = "/icons/ui-icons/svg/error-start-event.svg".asResource()!!,
        override val escalationStartEvent: SvgIcon = "/icons/ui-icons/svg/escalation-start-event.svg".asResource()!!,
        override val timerStartEvent: SvgIcon = "/icons/ui-icons/svg/timer-start-event.svg".asResource()!!,
        override val signalStartEvent: SvgIcon = "/icons/ui-icons/svg/signal-start-event.svg".asResource()!!,
        override val boundaryCancelEvent: SvgIcon = "/icons/ui-icons/svg/cancel-boundary-event.svg".asResource()!!,
        override val boundaryCompensationEvent: SvgIcon = "/icons/ui-icons/svg/compensation-boundary-event.svg".asResource()!!,
        override val boundaryConditionalEvent: SvgIcon = "/icons/ui-icons/svg/conditional-boundary-event.svg".asResource()!!,
        override val boundaryErrorEvent: SvgIcon = "/icons/ui-icons/svg/error-boundary-event.svg".asResource()!!,
        override val boundaryEscalationEvent: SvgIcon = "/icons/ui-icons/svg/escalation-boundary-event.svg".asResource()!!,
        override val boundaryMessageEvent: SvgIcon = "/icons/ui-icons/svg/message-boundary-event.svg".asResource()!!,
        override val boundarySignalEvent: SvgIcon = "/icons/ui-icons/svg/signal-boundary-event.svg".asResource()!!,
        override val boundaryTimerEvent: SvgIcon = "/icons/ui-icons/svg/timer-boundary-event.svg".asResource()!!,
        override val unknownShape: SvgIcon = "/icons/ui-icons/svg/unknown-shape.svg".asResource()!!,
        override val recycleBin: SvgIcon = "/icons/ui-icons/svg/recycle-bin.svg".asResource()!!,
        override val wrench: SvgIcon = "/icons/ui-icons/svg/wrench.svg".asResource()!!,
        override val rightAngle: SvgIcon = "/icons/ui-icons/svg/angle-right.svg".asResource()!!,
        override val selectParentSequence: SvgIcon = "/icons/ui-icons/svg/select-parent-sequence.svg".asResource()!!,
): IconProvider

data class SvgIcon(val icon: String, val hash: Long)
