package com.valb3r.bpmn.intellij.plugin.bpmn.parser.core.lax

/** A single, isolated rule for recognizing one form of incomplete XML. */
interface LaxXmlRule {
    fun findHunks(input: String): List<IntRange>
}
