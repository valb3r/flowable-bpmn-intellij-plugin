package com.valb3r.bpmn.intellij.plugin.flowable.parser.nodes

interface BpmnMappable<T> {

    fun toElement(): T
}