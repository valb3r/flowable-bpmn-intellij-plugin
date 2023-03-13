package com.valb3r.bpmn.intellij.plugin.core.schemas

import com.intellij.openapi.module.Module
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.psi.PsiFile
import com.intellij.psi.xml.XmlFile
import com.intellij.xml.XmlSchemaProvider
import com.valb3r.bpmn.intellij.plugin.core.settings.currentSettings


open class BpmnFileSchemasProvider: XmlSchemaProvider() {

    protected open val BPMN20Schemas = mapOf(
        "http://www.omg.org/spec/BPMN/20100524/MODEL" to "/xsds/BPMN20.xsd",
        "http://www.omg.org/spec/BPMN/20100524/DI" to "/xsds/BPMNDI.xsd",
        "http://www.omg.org/spec/DD/20100524/DC" to "/xsds/DC.xsd",
        "http://www.omg.org/spec/DD/20100524/DI" to "/xsds/DI.xsd",
    )

    override fun isAvailable(file: XmlFile): Boolean {
        return !currentSettings().disableXsdSchema && currentSettings().openExtensions.any { file.name.endsWith(it) }
    }

    override fun getSchema(url: String, module: Module?, baseFile: PsiFile): XmlFile? {
        val fileName = BPMN20Schemas[url] ?: return null
        val resource = BpmnFileSchemasProvider::class.java.getResource(fileName) ?: return null
        val fileByURL = VfsUtil.findFileByURL(resource) ?: return null
        val result = baseFile.manager.findFile(fileByURL)
        return if (result is XmlFile) { result.copy() as XmlFile } else null
    }
}