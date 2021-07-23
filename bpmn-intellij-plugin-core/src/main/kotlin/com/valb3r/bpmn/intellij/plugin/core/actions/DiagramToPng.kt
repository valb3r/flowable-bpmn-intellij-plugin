package com.valb3r.bpmn.intellij.plugin.core.actions

import com.intellij.openapi.fileChooser.FileChooserFactory
import com.intellij.openapi.fileChooser.FileSaverDescriptor
import com.intellij.openapi.project.Project
import com.valb3r.bpmn.intellij.plugin.core.render.currentCanvas
import javax.imageio.ImageIO

fun saveDiagramToPng(project: Project) {
    val canvas = currentCanvas(project)
    val image = canvas.renderToBitmap() ?: return
    val descriptor = FileSaverDescriptor("Save Diagram To", "Save diagram to file", "png")
    val dialog = FileChooserFactory.getInstance().createSaveFileDialog(descriptor, project)
    val dest = dialog.save(null, "diagram") ?: return
    ImageIO.write(image, "png", dest.file)
}