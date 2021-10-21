package com.valb3r.bpmn.intellij.plugin.camunda.langinjection.xml

import com.intellij.psi.PsiLanguageInjectionHost
import com.valb3r.bpmn.intellij.plugin.commons.langinjection.xml.DefaultXmlInjector

class CamundaXmlCamundaInjector: DefaultXmlInjector() {

    override fun invalidXmlFileExtension(context: PsiLanguageInjectionHost): Boolean {
        return !context.containingFile.name.endsWith("bpmn") && context.containingFile?.context?.containingFile?.name?.endsWith("bpmn") != true
    }
}