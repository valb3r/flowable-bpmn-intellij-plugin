package com.valb3r.bpmn.intellij.plugin.core.render.uieventbus

import com.google.common.annotations.VisibleForTesting
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicReference
import kotlin.reflect.KClass

private val uiEventBus = AtomicReference<UiEventBus>()

fun currentUiEventBus(): UiEventBus {
    return uiEventBus.updateAndGet {
        if (null == it) {
            return@updateAndGet UiEventBus()
        }

        return@updateAndGet it
    }
}

@VisibleForTesting
fun setUiEventBus(eventBus: UiEventBus): UiEventBus {
    uiEventBus.set(eventBus)
    return eventBus
}

/**
 * Used for non-persistent events that should be processed within frame render.
 */
class UiEventBus {

    private val subscribers: ConcurrentMap<KClass<out UiEvent>, MutableList<(UiEvent) -> Unit>> = ConcurrentHashMap()

    fun <T: UiEvent> subscribe(event: KClass<T>, subscription: (T) -> Unit) {
        subscribers.computeIfAbsent(event) { CopyOnWriteArrayList() }.add(subscription as (UiEvent) -> Unit)
    }

    fun <T: UiEvent> publish(event: T) {
        subscribers[event::class]?.forEach { it(event) }
    }

    fun clearSubscriptions() {
        subscribers.clear()
    }
}