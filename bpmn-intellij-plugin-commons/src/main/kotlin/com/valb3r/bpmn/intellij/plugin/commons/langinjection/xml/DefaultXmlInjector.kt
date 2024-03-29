package com.valb3r.bpmn.intellij.plugin.commons.langinjection.xml

import com.intellij.lang.Language
import com.intellij.lang.injection.MultiHostInjector
import com.intellij.lang.injection.MultiHostRegistrar
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiLanguageInjectionHost
import com.intellij.psi.xml.XmlAttribute
import com.intellij.psi.xml.XmlAttributeValue
import com.intellij.psi.xml.XmlTag
import com.intellij.psi.xml.XmlText
import com.valb3r.bpmn.intellij.plugin.commons.langinjection.InjectionUtil.injectSpel
import com.valb3r.bpmn.intellij.plugin.core.settings.currentSettings

abstract class DefaultXmlInjector: MultiHostInjector {

    override fun elementsToInjectIn(): MutableList<out Class<out PsiElement>> {
        return mutableListOf(XmlAttributeValue::class.java, XmlText::class.java)
    }

    override fun getLanguagesToInject(registrar: MultiHostRegistrar, context: PsiElement) {
        if (context !is PsiLanguageInjectionHost) {
            return
        }

        if (invalidXmlFileExtension(context)) return

        when (context) {
            is XmlAttributeValue -> {
                listOf(
                        { tryToInjectCalledElement(context, context, registrar) },
                        { tryToInjectSkipExpression(context, context, registrar) },
                        { tryToInjectInTaskOfType("serviceTask", context, context, registrar) },
                        { tryToInjectInTaskOfType("sendTask", context, context, registrar) }
                ).map { it() }.firstOrNull { it }
            }
            is XmlText -> listOf(
                { tryToInjectConditionExpression(context, context, registrar)},
                { tryToInjectFieldExpression(context, context, registrar) }
            ).map { it() }.firstOrNull { it }
        }
    }

    protected open fun invalidXmlFileExtension(context: PsiLanguageInjectionHost): Boolean {
        return !isValidFileName(context.containingFile.name)
    }

    private fun isValidFileName(fileName: String?): Boolean {
        val name = fileName ?: return false
        val allowedExt = currentSettings().openExtensions
        return allowedExt.any { name.endsWith(it) }
    }

    private fun tryToInjectSkipExpression(context: XmlAttributeValue, asHost: PsiLanguageInjectionHost, registrar: MultiHostRegistrar): Boolean {
        return injectByAttrName("skipExpression", context, asHost, registrar)
    }

    private fun tryToInjectConditionExpression(context: XmlText, asHost: PsiLanguageInjectionHost, registrar: MultiHostRegistrar): Boolean {
        val parent = context.parent
        if (parent !is XmlTag) {
            return false
        }

        if (parent.localName == "conditionExpression") {
            injectSpel(asHost, registrar)
            return true
        }

        return false
    }

    private fun tryToInjectFieldExpression(context: XmlText, asHost: PsiLanguageInjectionHost, registrar: MultiHostRegistrar): Boolean {
        val parent = context.parent
        if (parent !is XmlTag) {
            return false
        }

        val enclosingParent = parent.parent
        if (enclosingParent !is XmlTag) {
            return false
        }

        if (enclosingParent.localName == "field" && parent.localName == "expression") {
            injectSpel(asHost, registrar)
            return true
        }

        return false
    }

    private fun tryToInjectCalledElement(context: XmlAttributeValue, asHost: PsiLanguageInjectionHost, registrar: MultiHostRegistrar): Boolean {
        return injectByAttrName("calledElement", context, asHost, registrar)
    }

    private fun injectByAttrName(attrName: String, context: XmlAttributeValue, asHost: PsiLanguageInjectionHost, registrar: MultiHostRegistrar): Boolean {
        val parent = context.parent
        if (parent !is XmlAttribute) {
            return false
        }

        if (parent.localName == attrName) {
            if (!asHost.text.contains("[$#]\\{".toRegex()) || !asHost.text.contains("}")) {
                return false
            }

            injectSpel(asHost, registrar)
            return true
        }

        return false
    }

    private fun tryToInjectInTaskOfType(taskType: String, context: XmlAttributeValue, asHost: PsiLanguageInjectionHost, registrar: MultiHostRegistrar): Boolean {
        val parent = context.parent
        if (parent !is XmlAttribute) {
            return false
        }

        if (parent.parent.localName != taskType) {
            return false
        }

        if (parent.localName == "delegateExpression" || parent.localName == "expression") {
            injectSpel(asHost, registrar)
            return true
        }

        if (parent.localName == "class") {
            injectClassName(asHost, registrar)
            return true
        }

        return false
    }

    private fun injectClassName(context: PsiLanguageInjectionHost, registrar: MultiHostRegistrar) {
        val text = context.text
        val javaLang = Language.getRegisteredLanguages().firstOrNull { it.id == "JAVA" } ?: return
        registrar.startInjecting(javaLang)
        registrar.addPlace("import ", ";", context, TextRange(0, text.length))
        registrar.doneInjecting()
    }
}