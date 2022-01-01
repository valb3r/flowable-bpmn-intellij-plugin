package com.valb3r.bpmn.intellij.activiti.plugin.schemas

import com.intellij.openapi.module.Module
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.psi.PsiFile
import com.intellij.psi.xml.XmlFile
import com.intellij.xml.XmlSchemaProvider
import com.valb3r.bpmn.intellij.plugin.core.settings.currentSettings


class ActivitiBpmnFileSchemasProvider: XmlSchemaProvider() {

    private val BPMN20Schema = "/xsds/BPMN20.xsd"

    override fun isAvailable(file: XmlFile): Boolean {
        return currentSettings().openExtensions.any { file.name.endsWith(it) }
    }

    override fun getSchema(url: String, module: Module?, baseFile: PsiFile): XmlFile? {
        val resource = ActivitiBpmnFileSchemasProvider::class.java.getResource(BPMN20Schema) ?: return null
        val fileByURL = VfsUtil.findFileByURL(resource) ?: return null
        val result = baseFile.manager.findFile(fileByURL)
        return if (result is XmlFile) { result.copy() as XmlFile } else null
    }
}