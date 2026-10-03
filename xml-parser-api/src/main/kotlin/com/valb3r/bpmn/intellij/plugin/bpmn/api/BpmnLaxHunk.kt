package com.valb3r.bpmn.intellij.plugin.bpmn.api

/**
 * An XML fragment omitted from parsing because it matches an explicit lax XML rule.
 * Offsets and line/column coordinates are zero-based UTF-16 code units with half-open ends.
 */
data class BpmnLaxHunk(
    val lineStart: Int,
    val charStart: Int,
    val lineEnd: Int,
    val charEnd: Int,
    val startOffset: Int,
    val endOffset: Int,
    val text: String
)
