package com.valb3r.bpmn.intellij.plugin.commons.langinjection

import com.intellij.lang.injection.MultiHostInjector
import com.intellij.lang.injection.MultiHostRegistrar
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiLanguageInjectionHost
import com.intellij.psi.PsiLiteralExpression
import com.valb3r.bpmn.intellij.plugin.commons.langinjection.InjectionUtil.injectSpel
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

        injectSpel(context, registrar, true)
    }
}