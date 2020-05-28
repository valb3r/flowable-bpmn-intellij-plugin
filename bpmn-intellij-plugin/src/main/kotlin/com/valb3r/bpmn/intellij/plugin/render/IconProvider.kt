package com.valb3r.bpmn.intellij.plugin.render

import com.intellij.openapi.util.IconLoader
import java.nio.charset.StandardCharsets
import javax.swing.Icon

interface IconProvider {
    val undo: Icon
    val redo: Icon
    val gear: Icon
    val script: Icon
    val businessRule: Icon
    val receive: Icon
    val user: Icon
    val camel: Icon
    val http: Icon
    val mule: Icon
    val exclusiveGateway: String
    val sequence: String
    val recycleBin: String
}

private fun String.asResource(): String? = DefaultBpmnProcessRenderer::class.java.classLoader.getResource(this)?.readText(StandardCharsets.UTF_8)

data class IconProviderImpl(
        override val undo: Icon = IconLoader.getIcon("/icons/actions/undo.png"),
        override val redo: Icon = IconLoader.getIcon("/icons/actions/redo.png"),
        override val gear: Icon = IconLoader.getIcon("/icons/ui-icons/gear.png"),
        override val script: Icon = IconLoader.getIcon("/icons/ui-icons/script.png"),
        override val businessRule: Icon = IconLoader.getIcon("/icons/ui-icons/business-rule.png"),
        override val receive: Icon = IconLoader.getIcon("/icons/ui-icons/receive.png"),
        override val user: Icon = IconLoader.getIcon("/icons/ui-icons/user.png"),
        override val camel: Icon = IconLoader.getIcon("/icons/ui-icons/camel.png"),
        override val http: Icon = IconLoader.getIcon("/icons/ui-icons/http.png"),
        override val mule: Icon = IconLoader.getIcon("/icons/ui-icons/mule.png"),
        override val exclusiveGateway: String = "/icons/ui-icons/svg/exclusive-gateway.svg".asResource()!!,
        override val sequence: String = "/icons/ui-icons/svg/sequence.svg".asResource()!!,
        override val recycleBin: String = "/icons/ui-icons/svg/recycle-bin.svg".asResource()!!
): IconProvider