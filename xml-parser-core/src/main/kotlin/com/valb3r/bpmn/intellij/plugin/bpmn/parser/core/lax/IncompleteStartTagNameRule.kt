package com.valb3r.bpmn.intellij.plugin.bpmn.parser.core.lax

/**
 * Finds an unfinished start-tag name such as `<serviceT`.
 * It skips other XML lexical constructs through separate [LaxXmlSubrule] implementations.
 * Attributes and arbitrary malformed XML are outside this rule's scope.
 */
class IncompleteStartTagNameRule(
    private val subrules: List<LaxXmlSubrule> = listOf(
        CommentSubrule(),
        CdataSubrule(),
        ProcessingInstructionSubrule(),
        StartTagOpeningSubrule()
    )
) : LaxXmlRule {

    override fun findHunks(input: String): List<IntRange> {
        val result = mutableListOf<IntRange>()
        var offset = 0

        while (offset < input.length) {
            val match = subrules.firstNotNullOfOrNull { it.match(input, offset) }
            when (match) {
                null -> offset++
                is LaxXmlSubruleMatch.Skip -> offset = match.nextOffset
                is LaxXmlSubruleMatch.StartTagOpening -> {
                    val candidate = inspectStartTagCandidate(input, offset, match.nameStartOffset)
                    candidate.hunk?.let(result::add)
                    offset = candidate.nextOffset
                }
            }
        }

        return result
    }

    private data class StartTagCandidateScan(val hunk: IntRange?, val nextOffset: Int)

    private fun inspectStartTagCandidate(input: String, tagStartOffset: Int, nameStartOffset: Int): StartTagCandidateScan {
        val nameEndOffset = findStartTagNameEndOffset(input, nameStartOffset)
        val closingBracketOffset = findUnquotedStartTagClosingBracketOffset(input, nameEndOffset)
        if (closingBracketOffset >= 0) {
            return StartTagCandidateScan(null, closingBracketOffset + 1)
        }

        val nextMarkupOffset = findNextMarkupOffset(input, nameEndOffset)
        val suffixEndOffset = nextMarkupOffset ?: input.length
        val suffixContainsOnlyWhitespace = containsOnlyWhitespace(input, nameEndOffset, suffixEndOffset)
        val hunk = if (suffixContainsOnlyWhitespace) tagStartOffset until nameEndOffset else null

        // Resume at the next markup boundary so independent matches are still discoverable.
        return StartTagCandidateScan(hunk, nextMarkupOffset ?: input.length)
    }

    private fun findStartTagNameEndOffset(input: String, nameStartOffset: Int): Int {
        var cursor = nameStartOffset
        while (cursor < input.length && isXmlNameCharacter(input[cursor])) cursor++
        return cursor
    }

    /** Returns the unquoted closing `>` offset, or -1 when the start tag is incomplete. */
    private fun findUnquotedStartTagClosingBracketOffset(input: String, afterNameOffset: Int): Int {
        var quote: Char? = null
        var cursor = afterNameOffset
        while (cursor < input.length) {
            val ch = input[cursor]
            if (null != quote) {
                if (ch == quote) quote = null
            } else {
                when (ch) {
                    '\'', '"' -> quote = ch
                    '>' -> return cursor
                    '<' -> return -1
                }
            }
            cursor++
        }
        return -1
    }

    private fun findNextMarkupOffset(input: String, fromOffset: Int): Int? =
        input.indexOf('<', fromOffset).takeIf { it >= 0 }

    private fun containsOnlyWhitespace(input: String, startOffset: Int, endOffset: Int): Boolean =
        (startOffset until endOffset).all { input[it].isWhitespace() }

    private fun isXmlNameCharacter(ch: Char): Boolean =
        ch == '_' || ch == ':' || ch.isLetter() || ch == '-' || ch == '.' || ch.isDigit()
}
