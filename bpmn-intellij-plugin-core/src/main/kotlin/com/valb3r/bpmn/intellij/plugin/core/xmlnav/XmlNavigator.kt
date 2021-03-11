package com.valb3r.bpmn.intellij.plugin.core.xmlnav

import com.intellij.openapi.project.Project
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.BpmnElementId
import com.valb3r.bpmn.intellij.plugin.core.id
import java.util.concurrent.ConcurrentHashMap

interface XmlNavigator {

    fun jumpTo(id: BpmnElementId)
}

private val xmlNavigator = ConcurrentHashMap<String, XmlNavigator>()

fun registerXmlNavigator(project: Project, navigator: XmlNavigator) {
    xmlNavigator[project.id()] = navigator
}

fun xmlNavigator(project: Project): XmlNavigator {
    return xmlNavigator[project.id()]!!
}