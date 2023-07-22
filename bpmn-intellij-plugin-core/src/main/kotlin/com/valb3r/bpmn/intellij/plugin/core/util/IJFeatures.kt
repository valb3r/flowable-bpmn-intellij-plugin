package com.valb3r.bpmn.intellij.plugin.core.util

import com.intellij.lang.Language
import com.intellij.openapi.project.Project
import com.intellij.psi.JavaCodeFragmentFactory

object IJFeatures {

    fun hasJava(project: Project): Boolean {
        try {
            JavaCodeFragmentFactory.getInstance(project)
        } catch (ex: NoClassDefFoundError) {
            return false
        }

        return true
    }

    fun hasSpel(): Boolean {
        return null != Language.getRegisteredLanguages().firstOrNull { it.id == "SpEL" }
    }
}