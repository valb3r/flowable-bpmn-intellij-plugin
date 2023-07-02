package com.valb3r.bpmn.intellij.plugin.core.properties

import com.intellij.ui.JBColor
import java.awt.Color
import javax.swing.*

class PropertyInputVerifier(
    private val initialValue: Any?,
    private val message: String,
    private val verifier: (JComponent, Any?) -> Boolean
): InputVerifier() {

    private var background: Color? = null
    private var activePopup: Popup? = null

    override fun verify(input: JComponent): Boolean {
        return verifier(input, initialValue)
    }

    fun onStopEditing(input: JComponent): Boolean {
        if (verifier(input, initialValue)) {
            hidePopupIfOpen()
            input.background = this.background
            return true
        }

        input.background = JBColor.PINK
        val popup = PopupFactory.getSharedInstance().getPopup(
            input,
            JLabel(message),
            input.locationOnScreen.x,
            input.locationOnScreen.y + input.height
        )
        hidePopupIfOpen()
        popup.show()
        this.activePopup = popup

        val timer = Timer(5_000) { popup.hide() }
        timer.isRepeats = false
        timer.start()
        return false
    }

    fun hidePopupIfOpen() {
        activePopup?.hide()
    }
}