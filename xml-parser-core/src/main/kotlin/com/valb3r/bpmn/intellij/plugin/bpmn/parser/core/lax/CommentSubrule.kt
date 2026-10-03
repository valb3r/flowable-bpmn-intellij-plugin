package com.valb3r.bpmn.intellij.plugin.bpmn.parser.core.lax

/** Skips XML comments, including an unfinished comment through end of input. */
class CommentSubrule : LaxXmlSubrule {

    override fun match(input: String, offset: Int): LaxXmlSubruleMatch? {
        if (!input.startsWith("<!--", offset)) return null
        val close = input.indexOf("-->", offset + 4)
        return LaxXmlSubruleMatch.Skip(if (close < 0) input.length else close + 3)
    }
}
