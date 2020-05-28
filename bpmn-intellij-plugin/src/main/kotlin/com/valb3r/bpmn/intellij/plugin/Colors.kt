package com.valb3r.bpmn.intellij.plugin

import com.intellij.ui.JBColor
import java.awt.Color

enum class Colors(val color: JBColor) {
    SERVICE_TASK_COLOR(JBColor(Color(0xF9F9F9), Color(0xF9F9F9))),
    PROCESS_COLOR(JBColor(Color(0xFDFEFF), Color(0x292B2D))),
    CALL_ACTIVITY_COLOR(JBColor(Color(0xF9F9F9), Color(0xF9F9F9))),
    ELEMENT_BORDER_COLOR(JBColor(Color(0xC9C9C9), Color(0xC9C9C9))),
    INNER_TEXT_COLOR(JBColor(Color(0x292B2D), Color(0x292B2D))),
    ARROW_COLOR(JBColor(Color(0x292B2D), Color(0xFDFEFF))),
    ANCHOR_COLOR(JBColor(Color(0xFFAA00), Color(0xFFAA00))),
    WAYPOINT_COLOR(JBColor(Color(0xFF0000), Color(0xFF0000))),
    MID_WAYPOINT_COLOR(JBColor(Color(0x0088FF), Color(0x0088FF))),
    BACKGROUND_COLOR(JBColor(Color(0xFDFEFF), Color(0x292B2D))),
    ACTIONS_BORDER_COLOR(JBColor(Color(0x626466), Color(0x949698))),
    SELECTED_COLOR(JBColor(Color(0x00FF00), Color(0x00D000))),
    START_EVENT(JBColor(Color(0x00FF00), Color(0x00D000))),
    END_EVENT(JBColor(Color(0xFF0000), Color(0xFF0000)))
}
