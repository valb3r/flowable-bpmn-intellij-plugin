package com.valb3r.bpmn.intellij.plugin.core.newelements

import com.intellij.openapi.project.Project
import com.valb3r.bpmn.intellij.plugin.bpmn.api.BpmnObjectFactory
import java.util.*

private val newElements = Collections.synchronizedMap(WeakHashMap<Project,  NewElementsProvider>())

fun registerNewElementsFactory(project: Project, factory: BpmnObjectFactory): NewElementsProvider {
    return newElements.computeIfAbsent(project) {
        NewElementsProvider(factory)
    }
}

fun newElementsFactory(project: Project): NewElementsProvider {
    return newElements[project]!!
}


class NewElementsProvider(private val factory: BpmnObjectFactory): BpmnObjectFactory by factory
