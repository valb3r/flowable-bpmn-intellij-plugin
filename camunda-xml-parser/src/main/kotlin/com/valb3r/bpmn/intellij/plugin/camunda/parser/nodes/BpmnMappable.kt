package com.valb3r.bpmn.intellij.plugin.camunda.parser.nodes

interface BpmnMappable<T> {

    fun toElement(): T
}