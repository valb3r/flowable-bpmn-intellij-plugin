package com.valb3r.bpmn.intellij.plugin.core.newelements

import com.valb3r.bpmn.intellij.plugin.bpmn.api.BpmnObjectFactory
import java.util.concurrent.atomic.AtomicReference

private val newElements = AtomicReference<NewElementsProvider>()

fun registerNewElementsFactory(factory: BpmnObjectFactory): NewElementsProvider {
    return newElements.updateAndGet {
        return@updateAndGet NewElementsProvider(factory)
    }
}

fun newElementsFactory(): NewElementsProvider {
    return newElements.get()!!
}


class NewElementsProvider(private val factory: BpmnObjectFactory): BpmnObjectFactory by factory
