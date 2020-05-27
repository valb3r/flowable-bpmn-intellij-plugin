package com.valb3r.bpmn.intellij.plugin.render

import com.intellij.openapi.util.IconLoader
import java.nio.charset.StandardCharsets
import javax.swing.Icon

interface IconProvider {
    val undo: Icon
    val redo: Icon
    val gear: Icon
    val exclusiveGateway: String
    val sequence: String
    val recycleBin: String
}

private fun String.asResource(): String? = DefaultBpmnProcessRenderer::class.java.classLoader.getResource(this)?.readText(StandardCharsets.UTF_8)

data class IconProviderImpl(
        override val undo: Icon = IconLoader.getIcon("/icons/actions/undo.png"),
        override val redo: Icon = IconLoader.getIcon("/icons/actions/redo.png"),
        override val gear: Icon = IconLoader.getIcon("/icons/render/gear.png"),
        override val exclusiveGateway: String = "/icons/exclusive-gateway.svg".asResource()!!,
        override val sequence: String = "/icons/sequence.svg".asResource()!!,
        override val recycleBin: String = "/icons/recycle-bin.svg".asResource()!!
): IconProvider