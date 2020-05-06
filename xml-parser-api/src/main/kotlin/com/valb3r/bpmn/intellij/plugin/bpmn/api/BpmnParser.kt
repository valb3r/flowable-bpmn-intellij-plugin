package com.valb3r.bpmn.intellij.plugin.bpmn.api

import java.io.InputStream

interface BpmnParser {

    fun parse(input: InputStream): BpmnProcessObject
    fun parse(input: String): BpmnProcessObject
}