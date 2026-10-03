package com.valb3r.bpmn.intellij.plugin.bpmn.parser.core.lax

/** Skips CDATA sections, including an unfinished section through end of input. */
class CdataSubrule : LaxXmlSubrule {

    override fun match(input: String, offset: Int): LaxXmlSubruleMatch? {
        if (!input.startsWith("<![CDATA[", offset)) return null
        val close = input.indexOf("]]>", offset + 9)
        return LaxXmlSubruleMatch.Skip(if (close < 0) input.length else close + 3)
    }
}
