package com.valb3r.bpmn.intellij.plugin.bpmn.parser.core.lax

/** Skips processing instructions, including an unfinished instruction through end of input. */
class ProcessingInstructionSubrule : LaxXmlSubrule {

    override fun match(input: String, offset: Int): LaxXmlSubruleMatch? {
        if (!input.startsWith("<?", offset)) return null
        val close = input.indexOf("?>", offset + 2)
        return LaxXmlSubruleMatch.Skip(if (close < 0) input.length else close + 2)
    }
}
