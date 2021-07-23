package com.valb3r.bpmn.intellij.plugin.core.actions

import com.intellij.openapi.project.Project
import com.valb3r.bpmn.intellij.plugin.core.render.currentCanvas
import java.io.File
import javax.imageio.ImageIO

fun saveDiagramToPng(project: Project) {
    val canvas = currentCanvas(project)
    val image = canvas.renderToBitmap() ?: return
    ImageIO.write(image, "png", File("/home/valb3r/test.png"))
}