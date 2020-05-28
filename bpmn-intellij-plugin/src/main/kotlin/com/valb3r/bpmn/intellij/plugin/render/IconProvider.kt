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
    val exclusiveGateway: String
    val sequence: String
    val recycleBin: String
}

private fun String.asResource(): String? = DefaultBpmnProcessRenderer::class.java.classLoader.getResource(this)?.readText(StandardCharsets.UTF_8)

data class IconProviderImpl(
        override val undo: Icon = IconLoader.getIcon("/icons/actions/undo.png"),
        override val redo: Icon = IconLoader.getIcon("/icons/actions/redo.png"),
        override val gear: Icon = IconLoader.getIcon("/icons/render/gear.png"),
        override val script: Icon = IconLoader.getIcon("/icons/popupmenu/script.png"),
        override val businessRule: Icon = IconLoader.getIcon("/icons/popupmenu/business-rule.png"),
        override val receive: Icon = IconLoader.getIcon("/icons/popupmenu/receive.png"),
        override val user: Icon = IconLoader.getIcon("/icons/popupmenu/user.png"),
        override val exclusiveGateway: String = "/icons/ui-icons/svg/exclusive-gateway.svg".asResource()!!,
        override val sequence: String = "/icons/ui-icons/svg/sequence.svg".asResource()!!,
        override val recycleBin: String = "/icons/ui-icons/svg/recycle-bin.svg".asResource()!!
): IconProvider