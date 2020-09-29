package com.valb3r.bpmn.intellij.plugin.activity.parser.nodes

interface BpmnMappable<T> {

    fun toElement(): T
}