package com.valb3r.bpmn.intellij.plugin.core.render.uieventbus

import com.google.common.annotations.VisibleForTesting
import com.intellij.openapi.project.Project
import com.valb3r.bpmn.intellij.plugin.core.id
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.reflect.KClass

private val uiEventBus = ConcurrentHashMap<String, UiEventBus>()

fun currentUiEventBus(project: Project): UiEventBus {
    return uiEventBus.computeIfAbsent(project.id()) {
        UiEventBus()
    }
}

@VisibleForTesting
fun setUiEventBus(project: Project, eventBus: UiEventBus): UiEventBus {
    uiEventBus[project.id()] = eventBus
    return eventBus
}

/**
 * Used for non-persistent events that should be processed within frame render.
 */
class UiEventBus {

    private val subscribers: ConcurrentMap<KClass<out UiEvent>, MutableList<SubscriptionEntry>> = ConcurrentHashMap()

    fun <T: UiEvent> subscribe(event: KClass<T>, owner: Any? = null, subscription: (T) -> Unit) {
        subscribers.computeIfAbsent(event) { CopyOnWriteArrayList() }.add(SubscriptionEntry(owner, subscription as (UiEvent) -> Unit))
    }

    fun <T: UiEvent> publish(event: T) {
        subscribers[event::class]?.forEach { it.handler(event) }
    }

    fun clearSubscriptions() {
        subscribers.clear()
    }

    fun <T: UiEvent> clearSubscriptionsOf(event: KClass<T>, owner: Any? = null) {
        if (null == owner) {
            subscribers.remove(event)
            return
        }

        subscribers[event]?.apply {
            this.removeAll(this.filter { it.owner == owner })
        }
    }

    private data class SubscriptionEntry(val owner: Any?, val handler: (UiEvent) -> Unit)
}