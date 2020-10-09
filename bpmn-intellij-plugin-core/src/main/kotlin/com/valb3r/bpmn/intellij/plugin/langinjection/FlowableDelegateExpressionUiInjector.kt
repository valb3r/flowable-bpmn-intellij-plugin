package com.valb3r.bpmn.intellij.plugin.langinjection

import com.intellij.lang.Language
import com.intellij.lang.injection.MultiHostInjector
import com.intellij.lang.injection.MultiHostRegistrar
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiLanguageInjectionHost
import com.intellij.psi.PsiLiteralExpression
import java.util.concurrent.atomic.AtomicReference

private val currentFile = AtomicReference<PsiFile>()

fun registerCurrentFile(file: PsiFile) {
    currentFile.set(file)
}

fun getCurrentFile(): PsiFile {
    return currentFile.get()!!
}

class FlowableDelegateExpressionUiInjector: MultiHostInjector {

    override fun elementsToInjectIn(): MutableList<out Class<out PsiElement>> {
        return mutableListOf(PsiLiteralExpression::class.java)
    }

    override fun getLanguagesToInject(registrar: MultiHostRegistrar, context: PsiElement) {
        if (context !is PsiLanguageInjectionHost) {
            return
        }

        val expectedFile = currentFile.get() ?: return

        if (context.containingFile != expectedFile && context.containingFile?.context?.containingFile != expectedFile) {
            return
        }

        if (!context.text.contains("[$#]\\{".toRegex()) || !context.text.contains("}")) {
            return
        }

        injectSpel(context, registrar)
    }

    private fun injectSpel(context: PsiLanguageInjectionHost, registrar: MultiHostRegistrar) {
        val text = context.text
        val beanName = Language.getRegisteredLanguages().first { it.id == "SpEL" }!!
        registrar.startInjecting(beanName)
        registrar.addPlace("", "", context, TextRange(3, text.length - 2))
        registrar.doneInjecting()
    }
}