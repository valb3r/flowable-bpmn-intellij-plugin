package com.valb3r.bpmn.intellij.plugin.core.xmlnav

import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.BpmnElementId
import java.util.concurrent.atomic.AtomicReference

interface XmlNavigator {

    fun jumpTo(id: BpmnElementId)
}

private val xmlNavigator = AtomicReference<XmlNavigator>()

fun newXmlNavigator(navigator: XmlNavigator) {
    xmlNavigator.set(navigator)
}

fun xmlNavigator(): XmlNavigator {
    return xmlNavigator.get()!!
}