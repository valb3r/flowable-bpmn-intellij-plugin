package com.valb3r.bpmn.intellij.plugin.bpmn.parser.core.lax

/** Recognizes `<` followed by the start of an XML element name. */
class StartTagOpeningSubrule : LaxXmlSubrule {

    override fun match(input: String, offset: Int): LaxXmlSubruleMatch? {
        if (offset < 0 || offset + 1 >= input.length || input[offset] != '<' || !isNameStart(input[offset + 1])) {
            return null
        }
        return LaxXmlSubruleMatch.StartTagOpening(offset + 1)
    }

    private fun isNameStart(ch: Char): Boolean = ch == '_' || ch == ':' || ch.isLetter()
}
