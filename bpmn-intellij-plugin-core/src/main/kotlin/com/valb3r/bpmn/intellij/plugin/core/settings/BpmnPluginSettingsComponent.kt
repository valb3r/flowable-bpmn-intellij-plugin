package com.valb3r.bpmn.intellij.plugin.core.settings

import org.jdesktop.swingx.autocomplete.AutoCompleteDecorator
import java.awt.Font
import java.awt.GraphicsEnvironment
import java.awt.event.FocusEvent
import java.awt.event.FocusListener
import javax.swing.*

private const val DELIMITER = ","

class BpmnPluginSettingsComponent() {
    lateinit var settingsPanel: JPanel
    var preferredFocusedComponent: JComponent
    var state = currentSettings().copy()

    private lateinit var zoomMin: JSlider
    private lateinit var zoomMax: JSlider
    private lateinit var zoomFactor: JSlider
    private lateinit var keyboardSmallStep: JSlider
    private lateinit var keyboardLargeStep: JSlider
    private lateinit var lineThickness: JSlider
    private lateinit var uiFontName: JComboBox<String>
    private lateinit var uiFontSize: JSpinner
    private lateinit var dataFontName: JComboBox<String>
    private lateinit var dataFontSize: JSpinner
    private lateinit var openExtensions: JTextField
    private lateinit var enableFps: JCheckBox

    init {
        AutoCompleteDecorator.decorate(uiFontName)
        AutoCompleteDecorator.decorate(dataFontName)

        preferredFocusedComponent = zoomMin
        uiFontSize.model = SpinnerNumberModel(10, 6, 32, 1)
        dataFontSize.model = SpinnerNumberModel(10, 6, 32, 1)

        bindDataFromModel()
        attachListeners()
    }

    fun isValid(): String? {
        if (openExtensions.text.isBlank()) {
            return "Extension list should not be blank"
        }

        if (extensions().any { it.isBlank() }) {
            return "Extension should not be blank"
        }

        return null
    }

    private fun bindDataFromModel() {
        val actualUiFont = Font(state.uiFontName, 0, 10)
        val actualDataFont = Font(state.dataFontName, 0, 10)
        populateFontComboboxes(actualUiFont, actualDataFont)
        zoomMin.value = state.zoomMin.asSlider()
        zoomMax.value = state.zoomMax.asSlider()
        zoomFactor.value = state.zoomFactor.asSlider()
        keyboardSmallStep.value = state.keyboardSmallStep.asSlider()
        keyboardLargeStep.value = state.keyboardLargeStep.asSlider()
        lineThickness.value = state.lineThickness.asSlider()
        uiFontSize.value = state.uiFontSize
        dataFontSize.value = state.dataFontSize
        openExtensions.text = state.openExtensions.joinToString(DELIMITER)
        enableFps.isSelected = state.enableFps
    }

    private fun populateFontComboboxes(actualUiFont: Font, actualDataFont: Font) {
        var uiFontFound = false
        var displayFontFound = false
        GraphicsEnvironment.getLocalGraphicsEnvironment().allFonts.forEachIndexed { pos, font ->
            uiFontName.addItem(font.name)
            dataFontName.addItem(font.name)
            if (actualUiFont.name == font.name) {
                uiFontFound = true
                uiFontName.selectedIndex = pos
            }
            if (actualDataFont.name == font.name) {
                displayFontFound = true
                dataFontName.selectedIndex = pos
            }
        }
        // In case exact font name was not matched
        GraphicsEnvironment.getLocalGraphicsEnvironment().allFonts.forEachIndexed { pos, font ->
            if (!uiFontFound && font.family.contains(actualUiFont.family)) {
                uiFontName.selectedIndex = pos
            }
            if (!displayFontFound && font.family.contains(actualDataFont.family)) {
                dataFontName.selectedIndex = pos
            }
        }
    }

    private fun attachListeners() {
        zoomMin.addChangeListener { state.zoomMin = zoomMin.value.fromSlider() }
        zoomMax.addChangeListener { state.zoomMax = zoomMax.value.fromSlider() }
        zoomFactor.addChangeListener { state.zoomFactor = zoomFactor.value.fromSlider() }
        keyboardSmallStep.addChangeListener { state.keyboardSmallStep = keyboardSmallStep.value.fromSlider() }
        keyboardLargeStep.addChangeListener { state.keyboardLargeStep = keyboardLargeStep.value.fromSlider() }
        lineThickness.addChangeListener { state.lineThickness = lineThickness.value.fromSlider() }
        uiFontName.addActionListener { state.uiFontName = uiFontName.selectedItem as String }
        uiFontSize.addChangeListener { state.uiFontSize = uiFontSize.value as Int }
        dataFontName.addActionListener { state.dataFontName = dataFontName.selectedItem as String }
        dataFontSize.addChangeListener { state.dataFontSize = dataFontSize.value as Int }
        openExtensions.addFocusListener(object : FocusListener {
            override fun focusGained(e: FocusEvent?) { /* NOP */}
            override fun focusLost(e: FocusEvent?) { state.openExtensions = extensions().toMutableSet()
            }
        })
        enableFps.addChangeListener { state.enableFps = enableFps.isSelected }
    }

    private fun extensions() = openExtensions.text.split(DELIMITER).toSet()

    private fun Float.asSlider(): Int = (this * 100.0f).toInt()
    private fun Int.fromSlider(): Float = this / 100.0f
}