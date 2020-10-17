package com.valb3r.bpmn.intellij.plugin.activiti.parser.nodes

interface BpmnMappable<T> {

    fun toElement(): T
}