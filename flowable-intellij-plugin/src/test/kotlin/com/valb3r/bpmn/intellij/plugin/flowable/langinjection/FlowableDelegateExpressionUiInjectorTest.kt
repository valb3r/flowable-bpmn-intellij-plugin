package com.valb3r.bpmn.intellij.plugin.flowable.langinjection

import com.intellij.diagnostic.ImplementationConflictException
import com.intellij.lang.Language
import com.intellij.lang.injection.MultiHostRegistrar
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiLanguageInjectionHost
import com.nhaarman.mockitokotlin2.*
import com.valb3r.bpmn.intellij.plugin.flowable.langinjection.FlowableDelegateExpressionUiInjector
import com.valb3r.bpmn.intellij.plugin.flowable.langinjection.registerCurrentFile
import org.amshove.kluent.any
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class FlowableDelegateExpressionUiInjectorTest {

    private val unDesiredFile = mock<PsiFile>()
    private val desiredFile = mock<PsiFile>()
    private val registrar = mock<MultiHostRegistrar>()
    private val context = mock<PsiLanguageInjectionHost>()
    private val nestedContext = mock<PsiElement>()
    private val nestedFile = mock<PsiFile>()

    private val tested = FlowableDelegateExpressionUiInjector()

    @BeforeEach
    fun init() {
        try {
            TestableLanguage() // used field for getRegisteredLanguages()
        } catch (ex: ImplementationConflictException) {
            // NOP
        }
    }

    @Test
    fun `Injection applies to opened file with direct context`() {
        whenever(context.containingFile).doReturn(desiredFile)
        whenever(context.text).doReturn("\${someBean}")
        registerCurrentFile(desiredFile)

        tested.getLanguagesToInject(registrar, context)

        verify(registrar).startInjecting(any())
    }

    @Test
    fun `Injection applies to opened file with nested context`() {
        whenever(context.containingFile).doReturn(nestedFile)
        whenever(nestedFile.context).doReturn(nestedContext)
        whenever(context.text).doReturn("\${someBean}")
        whenever(nestedContext.containingFile).doReturn(desiredFile)
        registerCurrentFile(desiredFile)

        tested.getLanguagesToInject(registrar, context)

        verify(registrar).startInjecting(any())
    }

    @Test
    fun `Injection does not apply to non-matching file`() {
        whenever(context.containingFile).doReturn(unDesiredFile)
        whenever(context.text).doReturn("\${someBean}")
        registerCurrentFile(desiredFile)

        tested.getLanguagesToInject(registrar, context)

        verify(registrar, never()).startInjecting(any())
    }

    @Test
    fun `Injection does not apply to non-matching nested file`() {
        whenever(context.containingFile).doReturn(nestedFile)
        whenever(nestedFile.context).doReturn(nestedContext)
        whenever(context.text).doReturn("\${someBean}")
        whenever(nestedContext.containingFile).doReturn(unDesiredFile)
        registerCurrentFile(desiredFile)

        tested.getLanguagesToInject(registrar, context)

        verify(registrar, never()).startInjecting(any())
    }

    class TestableLanguage: Language("SpEL")
}