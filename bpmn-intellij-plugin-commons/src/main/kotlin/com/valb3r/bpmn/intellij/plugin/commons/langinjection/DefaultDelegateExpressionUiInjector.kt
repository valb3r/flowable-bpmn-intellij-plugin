package com.valb3r.bpmn.intellij.plugin.commons.langinjection

import com.intellij.lang.Language
import com.intellij.lang.injection.MultiHostInjector
import com.intellij.lang.injection.MultiHostRegistrar
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiLanguageInjectionHost
import com.intellij.psi.PsiLiteralExpression
import com.valb3r.bpmn.intellij.plugin.core.id
import java.util.concurrent.ConcurrentHashMap

private val currentFile = ConcurrentHashMap<String, PsiFile>()

fun registerCurrentFile(project: Project, file: PsiFile) {
    currentFile[project.id()] = file
}

fun getCurrentFile(project: Project): PsiFile {
    return currentFile[project.id()]!!
}

abstract class DefaultDelegateExpressionUiInjector: MultiHostInjector {

    override fun elementsToInjectIn(): MutableList<out Class<out PsiElement>> {
        return mutableListOf(PsiLiteralExpression::class.java)
    }

    override fun getLanguagesToInject(registrar: MultiHostRegistrar, context: PsiElement) {
        if (context !is PsiLanguageInjectionHost) {
            return
        }

        val expectedFile = currentFile.get(context.project.id()) ?: return

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