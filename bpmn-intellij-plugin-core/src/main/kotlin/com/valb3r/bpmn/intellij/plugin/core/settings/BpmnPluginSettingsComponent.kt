package com.valb3r.bpmn.intellij.plugin.core.settings

import com.intellij.uiDesigner.core.GridConstraints
import com.intellij.uiDesigner.core.GridLayoutManager
import com.intellij.util.ui.JBUI
import org.jdesktop.swingx.autocomplete.AutoCompleteDecorator
import java.awt.Font
import java.awt.GraphicsEnvironment
import java.awt.Insets
import java.awt.event.FocusEvent
import java.awt.event.FocusListener
import javax.swing.*

private const val DELIMITER = ","

class BpmnPluginSettingsComponent() {
    lateinit var settingsPanel: JPanel
    lateinit var preferredFocusedComponent: JComponent
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
    private lateinit var disableXsdSchema: JCheckBox

    fun createSettingsPanel(): JPanel {
        // Create the root settings panel with the same GridLayoutManager:
        this.settingsPanel = JPanel(
            GridLayoutManager(
                12,        // row-count
                3,         // column-count
                JBUI.emptyInsets(), // same margins as <margin top="0" left="0" bottom="0" right="0"/>
                -1,        // hGap (unused)
                -1         // vGap (unused)
            )
        )

        // 1) “Navigation settings” label (row=0, column=0..2)
        val navigationLabel = JLabel("Navigation settings")
        settingsPanel.add(
            navigationLabel,
            GridConstraints(
                0, 0, 1, 3,
                GridConstraints.ANCHOR_CENTER,
                GridConstraints.FILL_NONE,
                GridConstraints.SIZEPOLICY_FIXED,
                GridConstraints.SIZEPOLICY_FIXED,
                null, null, null, 0, false
            )
        )

        // 2) “Minimum zoom” label (row=1, column=0)
        val labelMinZoom = JLabel("Minimum zoom")
        settingsPanel.add(
            labelMinZoom,
            GridConstraints(
                1, 0, 1, 1,
                GridConstraints.ANCHOR_SOUTHWEST, // anchor="8"
                GridConstraints.FILL_NONE,         // fill="0"
                GridConstraints.SIZEPOLICY_FIXED,  // hsize-policy="0", vsize-policy="0"
                GridConstraints.SIZEPOLICY_FIXED,
                null, null, null, 0, false
            )
        )

        // 3) JSlider "zoomMin" (row=1, column=1..2)
        this.zoomMin = JSlider().apply {
            minimum = 5
            maximum = 50
            value = 30
        }
        settingsPanel.add(
            zoomMin,
            GridConstraints(
                1, 1, 1, 2,
                GridConstraints.ANCHOR_SOUTHWEST,
                GridConstraints.FILL_HORIZONTAL,   // fill="1"
                GridConstraints.SIZEPOLICY_CAN_GROW, // hsize-policy="6" often means "can/want grow"
                GridConstraints.SIZEPOLICY_FIXED,
                null, null, null, 0, false
            )
        )

        // 4) “Maximum zoom” label (row=2, column=0)
        val labelMaxZoom = JLabel("Maximum zoom")
        settingsPanel.add(
            labelMaxZoom,
            GridConstraints(
                2, 0, 1, 1,
                GridConstraints.ANCHOR_SOUTHWEST,
                GridConstraints.FILL_NONE,
                GridConstraints.SIZEPOLICY_FIXED,
                GridConstraints.SIZEPOLICY_FIXED,
                null, null, null, 0, false
            )
        )

        // 5) JSlider "zoomMax" (row=2, column=1..2)
        this.zoomMax = JSlider().apply {
            minimum = 100
            maximum = 500
            value = 200
        }
        settingsPanel.add(
            zoomMax,
            GridConstraints(
                2, 1, 1, 2,
                GridConstraints.ANCHOR_SOUTHWEST,
                GridConstraints.FILL_HORIZONTAL,
                GridConstraints.SIZEPOLICY_CAN_GROW,
                GridConstraints.SIZEPOLICY_FIXED,
                null, null, null, 0, false
            )
        )

        // 6) “Zoom step” label (row=3, column=0)
        val labelZoomFactor = JLabel("Zoom step")
        settingsPanel.add(
            labelZoomFactor,
            GridConstraints(
                3, 0, 1, 1,
                GridConstraints.ANCHOR_SOUTHWEST,
                GridConstraints.FILL_NONE,
                GridConstraints.SIZEPOLICY_FIXED,
                GridConstraints.SIZEPOLICY_FIXED,
                null, null, null, 0, false
            )
        )

        // 7) JSlider "zoomFactor" (row=3, column=1..2)
        this.zoomFactor = JSlider().apply {
            minimum = 105
            maximum = 150
            value = 120
        }
        settingsPanel.add(
            zoomFactor,
            GridConstraints(
                3, 1, 1, 2,
                GridConstraints.ANCHOR_SOUTHWEST,
                GridConstraints.FILL_HORIZONTAL,
                GridConstraints.SIZEPOLICY_CAN_GROW,
                GridConstraints.SIZEPOLICY_FIXED,
                null, null, null, 0, false
            )
        )

        // 8) “Keyboard small step” label (row=4, column=0)
        val labelKeyboardSmall = JLabel("Keyboard small step")
        settingsPanel.add(
            labelKeyboardSmall,
            GridConstraints(
                4, 0, 1, 1,
                GridConstraints.ANCHOR_SOUTHWEST,
                GridConstraints.FILL_NONE,
                GridConstraints.SIZEPOLICY_FIXED,
                GridConstraints.SIZEPOLICY_FIXED,
                null, null, null, 0, false
            )
        )

        // 9) JSlider "keyboardSmallStep" (row=4, column=1..2)
        this.keyboardSmallStep = JSlider().apply {
            minimum = 100
            maximum = 10000
            value = 500
        }
        settingsPanel.add(
            keyboardSmallStep,
            GridConstraints(
                4, 1, 1, 2,
                GridConstraints.ANCHOR_SOUTHWEST,
                GridConstraints.FILL_HORIZONTAL,
                GridConstraints.SIZEPOLICY_CAN_GROW,
                GridConstraints.SIZEPOLICY_FIXED,
                null, null, null, 0, false
            )
        )

        // 10) “Keyboard large step” label (row=5, column=0)
        val labelKeyboardLarge = JLabel("Keyboard large step")
        settingsPanel.add(
            labelKeyboardLarge,
            GridConstraints(
                5, 0, 1, 1,
                GridConstraints.ANCHOR_SOUTHWEST,
                GridConstraints.FILL_NONE,
                GridConstraints.SIZEPOLICY_FIXED,
                GridConstraints.SIZEPOLICY_FIXED,
                null, null, null, 0, false
            )
        )

        // 11) JSlider "keyboardLargeStep" (row=5, column=1..2)
        this.keyboardLargeStep = JSlider().apply {
            minimum = 100
            maximum = 20000
            value = 5000
        }
        settingsPanel.add(
            keyboardLargeStep,
            GridConstraints(
                5, 1, 1, 2,
                GridConstraints.ANCHOR_SOUTHWEST,
                GridConstraints.FILL_HORIZONTAL,
                GridConstraints.SIZEPOLICY_CAN_GROW,
                GridConstraints.SIZEPOLICY_FIXED,
                null, null, null, 0, false
            )
        )

        // 12) “Display Settings” label (row=6, column=0..2)
        val displayLabel = JLabel("Display Settings")
        settingsPanel.add(
            displayLabel,
            GridConstraints(
                6, 0, 1, 3,
                GridConstraints.ANCHOR_CENTER,
                GridConstraints.FILL_NONE,
                GridConstraints.SIZEPOLICY_FIXED,
                GridConstraints.SIZEPOLICY_FIXED,
                null, null, null, 0, false
            )
        )

        // 13) “Connecting arrow thickness” label (row=7, column=0)
        val labelLineThickness = JLabel("Connecting arrow thickness")
        settingsPanel.add(
            labelLineThickness,
            GridConstraints(
                7, 0, 1, 1,
                GridConstraints.ANCHOR_SOUTHWEST,
                GridConstraints.FILL_NONE,
                GridConstraints.SIZEPOLICY_FIXED,
                GridConstraints.SIZEPOLICY_FIXED,
                null, null, null, 0, false
            )
        )

        // 14) JSlider "lineThickness" (row=7, column=1..2)
        this.lineThickness = JSlider().apply {
            minimum = 100
            maximum = 1000
            value = 200
        }
        settingsPanel.add(
            lineThickness,
            GridConstraints(
                7, 1, 1, 2,
                GridConstraints.ANCHOR_SOUTHWEST,
                GridConstraints.FILL_HORIZONTAL,
                GridConstraints.SIZEPOLICY_CAN_GROW,
                GridConstraints.SIZEPOLICY_FIXED,
                null, null, null, 0, false
            )
        )

        // 15) “UI Font” label (row=8, column=0)
        val labelUiFont = JLabel("UI Font")
        settingsPanel.add(
            labelUiFont,
            GridConstraints(
                8, 0, 1, 1,
                GridConstraints.ANCHOR_SOUTHWEST,
                GridConstraints.FILL_NONE,
                GridConstraints.SIZEPOLICY_FIXED,
                GridConstraints.SIZEPOLICY_FIXED,
                null, null, null, 0, false
            )
        )

        // 16) JComboBox "uiFontName" (row=8, column=1)
        this.uiFontName = JComboBox<String>().apply {
            isEditable = true
        }
        settingsPanel.add(
            uiFontName,
            GridConstraints(
                8, 1, 1, 1,
                GridConstraints.ANCHOR_CENTER,
                GridConstraints.FILL_HORIZONTAL,
                GridConstraints.SIZEPOLICY_CAN_GROW,
                GridConstraints.SIZEPOLICY_FIXED,
                null,
                null,
                null,
                0,
                false
            )
        )

        // 17) JSpinner "uiFontSize" (row=8, column=2)
        this.uiFontSize = JSpinner()
        settingsPanel.add(
            uiFontSize,
            GridConstraints(
                8, 2, 1, 1,
                GridConstraints.ANCHOR_WEST,  // anchor="4" in the XML
                GridConstraints.FILL_NONE,
                GridConstraints.SIZEPOLICY_FIXED,
                GridConstraints.SIZEPOLICY_FIXED,
                null, null, null, 0, false
            )
        )

        // 18) “Data table font” label (row=9, column=0)
        val labelDataFont = JLabel("Data table font")
        settingsPanel.add(
            labelDataFont,
            GridConstraints(
                9, 0, 1, 1,
                GridConstraints.ANCHOR_SOUTHWEST,
                GridConstraints.FILL_NONE,
                GridConstraints.SIZEPOLICY_FIXED,
                GridConstraints.SIZEPOLICY_FIXED,
                null, null, null, 0, false
            )
        )

        // 19) JComboBox "dataFontName" (row=9, column=1)
        this.dataFontName = JComboBox<String>().apply {
            isEditable = true
        }
        settingsPanel.add(
            dataFontName,
            GridConstraints(
                9, 1, 1, 1,
                GridConstraints.ANCHOR_CENTER,
                GridConstraints.FILL_HORIZONTAL,
                GridConstraints.SIZEPOLICY_CAN_GROW,
                GridConstraints.SIZEPOLICY_FIXED,
                null,
                null,
                null,
                0,
                false
            )
        )

        // 20) JSpinner "dataFontSize" (row=9, column=2)
        this.dataFontSize = JSpinner()
        settingsPanel.add(
            dataFontSize,
            GridConstraints(
                9, 2, 1, 1,
                GridConstraints.ANCHOR_WEST,
                GridConstraints.FILL_NONE,
                GridConstraints.SIZEPOLICY_FIXED,
                GridConstraints.SIZEPOLICY_FIXED,
                null, null, null, 0, false
            )
        )

        // 21) “Supported extensions(comma-separated):” label (row=10, column=0)
        val extensionsLabel = JLabel("Supported extensions(comma-separated):")
        settingsPanel.add(
            extensionsLabel,
            GridConstraints(
                10, 0, 1, 1,
                GridConstraints.ANCHOR_SOUTHWEST,
                GridConstraints.FILL_NONE,
                GridConstraints.SIZEPOLICY_FIXED,
                GridConstraints.SIZEPOLICY_FIXED,
                null, null, null, 0, false
            )
        )

        // 22) JTextField "openExtensions" (row=10, column=1)
        this.openExtensions = JTextField().apply {
            // e.g. text = "xml,json"
        }
        settingsPanel.add(
            openExtensions,
            GridConstraints(
                10, 1, 1, 1,
                GridConstraints.ANCHOR_SOUTHWEST,
                GridConstraints.FILL_HORIZONTAL,
                GridConstraints.SIZEPOLICY_CAN_GROW,
                GridConstraints.SIZEPOLICY_FIXED,
                null,
                null,
                null,
                0,
                false
            )
        )

        // 23) JCheckBox "enableFps" (row=10, column=2)
        this.enableFps = JCheckBox("FPS")
        settingsPanel.add(
            enableFps,
            GridConstraints(
                10, 2, 1, 1,
                GridConstraints.ANCHOR_SOUTHWEST,
                GridConstraints.FILL_NONE,
                GridConstraints.SIZEPOLICY_CAN_SHRINK or GridConstraints.SIZEPOLICY_CAN_GROW,
                GridConstraints.SIZEPOLICY_FIXED,
                null, null, null, 0, false
            )
        )

        // 24) JCheckBox "disableXsdSchema" (row=11, column=0)
        this.disableXsdSchema = JCheckBox("Disable XSD schemas(requires IDE restart)")
        settingsPanel.add(
            disableXsdSchema,
            GridConstraints(
                11, 0, 1, 1,
                GridConstraints.ANCHOR_SOUTHWEST,
                GridConstraints.FILL_NONE,
                GridConstraints.SIZEPOLICY_CAN_SHRINK or GridConstraints.SIZEPOLICY_CAN_GROW,
                GridConstraints.SIZEPOLICY_FIXED,
                null, null, null, 0, false
            )
        )

        onAfterCreate()
        return settingsPanel
    }

    private fun onAfterCreate() {
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
        disableXsdSchema.isSelected = state.disableXsdSchema
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
        disableXsdSchema.addChangeListener { state.disableXsdSchema = disableXsdSchema.isSelected }
    }

    private fun extensions() = openExtensions.text.split(DELIMITER).toSet()

    private fun Float.asSlider(): Int = (this * 100.0f).toInt()
    private fun Int.fromSlider(): Float = this / 100.0f
}