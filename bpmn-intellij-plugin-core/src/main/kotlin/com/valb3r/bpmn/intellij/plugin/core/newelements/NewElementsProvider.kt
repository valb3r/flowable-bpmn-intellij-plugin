package com.valb3r.bpmn.intellij.plugin.core.newelements

import com.intellij.openapi.project.Project
import com.valb3r.bpmn.intellij.plugin.bpmn.api.BpmnObjectFactory
import com.valb3r.bpmn.intellij.plugin.core.id
import java.util.concurrent.ConcurrentHashMap

private val newElements = ConcurrentHashMap<String, NewElementsProvider>()

fun registerNewElementsFactory(project: Project, factory: BpmnObjectFactory): NewElementsProvider {
    return newElements.computeIfAbsent(project.id()) {
        NewElementsProvider(factory)
    }
}

fun newElementsFactory(project: Project): NewElementsProvider {
    return newElements[project.id()]!!
}


class NewElementsProvider(private val factory: BpmnObjectFactory): BpmnObjectFactory by factory
