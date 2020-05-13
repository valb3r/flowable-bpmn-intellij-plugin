package com.valb3r.bpmn.intellij.plugin.newelements

import com.valb3r.bpmn.intellij.plugin.bpmn.api.BpmnObjectFactory
import java.util.concurrent.atomic.AtomicReference

private val newElements = AtomicReference<NewElementsProvider>()

fun newElementsFactory(factory: BpmnObjectFactory): NewElementsProvider {
    return newElements.updateAndGet {
        if (null == it) {
            return@updateAndGet NewElementsProvider(factory)
        }

        return@updateAndGet it
    }
}

fun newElementsFactory(): NewElementsProvider {
    return newElements.get()!!
}


class NewElementsProvider(private val factory: BpmnObjectFactory): BpmnObjectFactory by factory
