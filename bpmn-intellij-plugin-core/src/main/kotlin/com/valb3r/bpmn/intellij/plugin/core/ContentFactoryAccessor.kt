package com.valb3r.bpmn.intellij.plugin.core

import com.intellij.ui.content.ContentFactory

// TODO !COMPATIBILITY: (< 2022) IntelliJ provides only ContentFactory.SERVICE.getInstance(). This method is scheduled for removal
fun getContentFactory(): ContentFactory {
    return try {
        val serviceClazz = ContentFactory::class.java.classes.find { it.simpleName == "SERVICE" }!!
        return serviceClazz.getDeclaredMethod("getInstance").invoke(null) as ContentFactory
    } catch (ex: Exception) {
        // New IntelliJ provides only ContentFactory.getInstance()
        val contentFactorySupplier = ContentFactory::class.java.getMethod("getInstance")
        contentFactorySupplier.invoke(null) as ContentFactory
    }
}