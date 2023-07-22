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
import org.jetbrains.uast.UastFacade.language
import java.util.*

private val currentFile = Collections.synchronizedMap(WeakHashMap<Project,  PsiFile>())

fun registerCurrentFile(project: Project, file: PsiFile) {
    currentFile[project] = file
}

fun getCurrentFile(project: Project): PsiFile {
    return currentFile[project]!!
}

abstract class DefaultDelegateExpressionUiInjector: MultiHostInjector {

    override fun elementsToInjectIn(): MutableList<out Class<out PsiElement>> {
        return mutableListOf(PsiLiteralExpression::class.java)
    }

    override fun getLanguagesToInject(registrar: MultiHostRegistrar, context: PsiElement) {
        if (context !is PsiLanguageInjectionHost) {
            return
        }

        val expectedFile = currentFile.get(context.project) ?: return

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
        val language = Language.getRegisteredLanguages().firstOrNull { it.id == "SpEL" } ?:
            Language.getRegisteredLanguages().firstOrNull { it.id == "JAVA" } ?: return
        registrar.startInjecting(language)
        registrar.addPlace("", "", context, TextRange(3, text.length - 2))
        registrar.doneInjecting()
    }
}