package com.valb3r.bpmn.intellij.plugin.core.parser

import com.intellij.openapi.project.Project
import com.valb3r.bpmn.intellij.plugin.bpmn.api.BpmnParser
import java.util.*

private val currentParsers = Collections.synchronizedMap(WeakHashMap<Project,  BpmnParser>())

fun registerParser(project: Project, parser: BpmnParser): BpmnParser {
    currentParsers[project] = parser
    return parser
}

fun currentParser(project: Project): BpmnParser {
    return currentParsers[project]!!
}
