package com.valb3r.bpmn.intellij.plugin.core.xmlnav

import com.intellij.openapi.project.Project
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.BpmnElementId
import java.util.*

interface XmlNavigator {

    fun jumpTo(id: BpmnElementId)
}

private val xmlNavigator = Collections.synchronizedMap(WeakHashMap<Project,  XmlNavigator>())

fun registerXmlNavigator(project: Project, navigator: XmlNavigator) {
    xmlNavigator[project] = navigator
}

fun xmlNavigator(project: Project): XmlNavigator {
    return xmlNavigator[project]!!
}