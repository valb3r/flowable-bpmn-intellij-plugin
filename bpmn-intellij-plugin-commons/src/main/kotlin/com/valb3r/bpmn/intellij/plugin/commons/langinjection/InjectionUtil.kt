package com.valb3r.bpmn.intellij.plugin.commons.langinjection

import com.intellij.lang.Language
import com.intellij.lang.injection.MultiHostRegistrar
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiLanguageInjectionHost

object InjectionUtil {

    private val spelRange = "[$#]\\{[^}]*}".toRegex()
    private val spelStart = "[$#]\\{".toRegex()

    fun injectSpel(context: PsiLanguageInjectionHost, registrar: MultiHostRegistrar, enclosedInQuotes: Boolean = false) {
        val text = context.text
        val language = Language.getRegisteredLanguages().firstOrNull { it.id == "SpEL" } ?: return

        if (null == spelRange.find(text)) {
            registrar.startInjecting(language)
            val beginIndex = spelStart.find(text)?.range?.last?.let { it + 1 } ?: if (enclosedInQuotes) 1 else 0
            val endIndex = if (enclosedInQuotes) text.length - 1 else text.length
            registrar.addPlace("", "", context, TextRange(beginIndex, endIndex))
            registrar.doneInjecting()
            return
        }

        spelRange.findAll(text).forEach {
            registrar.startInjecting(language)
            registrar.addPlace("", "", context, TextRange(it.range.first + 2, it.range.last))
            registrar.doneInjecting()
        }
    }
}