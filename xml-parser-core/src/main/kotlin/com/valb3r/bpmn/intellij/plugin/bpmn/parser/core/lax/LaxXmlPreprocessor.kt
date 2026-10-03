package com.valb3r.bpmn.intellij.plugin.bpmn.parser.core.lax

import com.valb3r.bpmn.intellij.plugin.bpmn.api.BpmnLaxHunk
import java.util.UUID

data class PreparedLaxXml(val xml: String, val hunks: List<BpmnLaxHunk>)

data class PreparedLaxXmlForUpdate(val xml: String, val markers: Map<String, String>)

/** Runs explicit rules and provides the common source masking used by Jackson and SAX. */
class LaxXmlPreprocessor(
    private val rules: List<LaxXmlRule> = listOf(IncompleteStartTagNameRule())
) {

    fun prepare(input: String): PreparedLaxXml {
        val ranges = mutableListOf<IntRange>()
        for (rule in rules) {
            for (range in rule.findHunks(input).sortedBy { it.first }) {
                require(range.first >= 0 && range.last < input.length) { "Lax rule returned out-of-bounds range: $range" }
                val start = range.first
                val endExclusive = range.last + 1
                require(ranges.none { overlaps(start, endExclusive, it.first, it.last + 1) }) {
                    "Lax XML rules returned overlapping ranges"
                }
                ranges += range
            }
        }

        val hunks = ranges.sortedBy { it.first }.map { range ->
            val start = range.first
            val end = range.last + 1
            val startPosition = lineAndChar(input, start)
            val endPosition = lineAndChar(input, end)
            BpmnLaxHunk(
                lineStart = startPosition.first,
                charStart = startPosition.second,
                lineEnd = endPosition.first,
                charEnd = endPosition.second,
                startOffset = start,
                endOffset = end,
                text = input.substring(start, end)
            )
        }

        return PreparedLaxXml(mask(input, hunks), hunks)
    }

    fun prepareForUpdate(input: String, hunks: List<BpmnLaxHunk>): PreparedLaxXmlForUpdate {
        val applicable = hunks
            .filter { it.startOffset >= 0 && it.endOffset <= input.length && it.startOffset < it.endOffset }
            .filter { input.substring(it.startOffset, it.endOffset) == it.text }
            .sortedBy { it.startOffset }

        val masked = mask(input, applicable)
        val output = StringBuilder(masked)
        val markers = linkedMapOf<String, String>()
        for (hunk in applicable.asReversed()) {
            val marker = "bpmn-lax-${UUID.randomUUID()}"
            val comment = "<!--$marker-->"
            output.replace(hunk.startOffset, hunk.startOffset + (hunk.endOffset - hunk.startOffset), comment)
            markers[comment] = hunk.text
        }
        return PreparedLaxXmlForUpdate(output.toString(), markers)
    }

    fun restore(output: String, markers: Map<String, String>): String =
        markers.entries.fold(output) { content, (marker, text) -> content.replace(marker, text) }

    private fun mask(input: String, hunks: List<BpmnLaxHunk>): String {
        if (hunks.isEmpty()) return input
        val output = StringBuilder(input)
        for (hunk in hunks.asReversed()) {
            require(hunk.startOffset >= 0 && hunk.endOffset <= output.length && hunk.startOffset < hunk.endOffset)
            for (index in hunk.startOffset until hunk.endOffset) {
                if (output[index] != '\n' && output[index] != '\r') output.setCharAt(index, ' ')
            }
        }
        return output.toString()
    }

    private fun overlaps(start: Int, end: Int, otherStart: Int, otherEnd: Int): Boolean =
        start < otherEnd && otherStart < end

    private fun lineAndChar(input: String, offset: Int): Pair<Int, Int> {
        var line = 0
        var lineStart = 0
        for (index in 0 until offset) {
            if (input[index] == '\n') {
                line++
                lineStart = index + 1
            }
        }
        return line to (offset - lineStart)
    }
}
