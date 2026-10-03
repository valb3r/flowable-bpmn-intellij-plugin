package com.valb3r.bpmn.intellij.plugin.bpmn.parser.core.lax

/** Result from an isolated lexical subrule. */
sealed interface LaxXmlSubruleMatch {
    data class Skip(val nextOffset: Int) : LaxXmlSubruleMatch
    data class StartTagOpening(val nameStartOffset: Int) : LaxXmlSubruleMatch
}

/** An isolated lexical recognizer used by a lax rule. */
interface LaxXmlSubrule {
    /** Returns a match at [offset], or null when this subrule does not apply there. */
    fun match(input: String, offset: Int): LaxXmlSubruleMatch?
}
