package com.valb3r.bpmn.intellij.plugin.activiti.parser

import com.valb3r.bpmn.intellij.plugin.bpmn.api.PropertyTable
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.BpmnElementId
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.WithParentId
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.tasks.BpmnServiceTask
import com.valb3r.bpmn.intellij.plugin.bpmn.api.diagram.DiagramElementId
import com.valb3r.bpmn.intellij.plugin.bpmn.api.diagram.elements.BoundsElement
import com.valb3r.bpmn.intellij.plugin.bpmn.api.diagram.elements.ShapeElement
import com.valb3r.bpmn.intellij.plugin.bpmn.api.events.EventPropagatableToXml
import com.valb3r.bpmn.intellij.plugin.activiti.parser.testevents.BpmnShapeObjectAddedEvent
import com.valb3r.bpmn.intellij.plugin.activiti.parser.testevents.DraggedToEvent
import com.valb3r.bpmn.intellij.plugin.bpmn.api.info.Property
import com.valb3r.bpmn.intellij.plugin.bpmn.api.info.PropertyType
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldContain
import org.amshove.kluent.shouldHaveSingleItem
import org.junit.jupiter.api.Test

internal class XmlUpdateEventScriptFormatTest {

    @Test
    fun `Moving a script task preserves script whitespace`() {
        assertScriptFormattingPreserved(
            DraggedToEvent(DiagramElementId("BPMNShape_scriptTask"), 25.0f, 30.0f, null, null)
        )
    }

    @Test
    fun `Adding a component preserves existing script whitespace`() {
        val newId = BpmnElementId("newServiceTask")
        assertScriptFormattingPreserved(
            BpmnShapeObjectAddedEvent(
                WithParentId(BpmnElementId("process"), BpmnServiceTask(newId)),
                ShapeElement(DiagramElementId("BPMNShape_newServiceTask"), newId, BoundsElement(0.0f, 0.0f, 100.0f, 80.0f)),
                PropertyTable(mutableMapOf(Pair(PropertyType.ID, mutableListOf(Property(newId.id)))))
            )
        )
    }

    private fun assertScriptFormattingPreserved(event: EventPropagatableToXml) {
        val script = "\n  def message = 'Hello  world'\n\n  if (message) {\n\tprintln message\n  }\n"
        listOf(ActivitiParser() to "", Activiti7Parser() to "bpmn2:").forEach { (parser, prefix) ->
            listOf(script, "<![CDATA[$script]]>").forEach { content ->
                val scriptElement = "<${prefix}script>$content</${prefix}script>"
                val input = """
                    <${prefix}definitions xmlns${if (prefix.isEmpty()) "" else ":bpmn2"}="http://www.omg.org/spec/BPMN/20100524/MODEL"
                        xmlns:activiti="http://activiti.org/bpmn"
                        xmlns:bpmndi="http://www.omg.org/spec/BPMN/20100524/DI"
                        xmlns:dc="http://www.omg.org/spec/DD/20100524/DC"
                        targetNamespace="http://example.com/script-formatting">
                        <${prefix}process id="process" isExecutable="true">
                            <${prefix}scriptTask id="scriptTask" scriptFormat="groovy">
                                SCRIPT_CONTENT
                            </${prefix}scriptTask>
                        </${prefix}process>
                        <bpmndi:BPMNDiagram id="diagram">
                            <bpmndi:BPMNPlane id="plane" bpmnElement="process">
                                <bpmndi:BPMNShape id="BPMNShape_scriptTask" bpmnElement="scriptTask">
                                    <dc:Bounds x="240.0" y="138.0" width="100.0" height="80.0"/>
                                </bpmndi:BPMNShape>
                            </bpmndi:BPMNPlane>
                        </bpmndi:BPMNDiagram>
                    </${prefix}definitions>
                """.trimIndent().replace("SCRIPT_CONTENT", scriptElement)

                val updated = parser.update(input, listOf(event))

                updated.shouldContain(scriptElement)
                parser.parse(updated).process.body!!.scriptTask!!.shouldHaveSingleItem()
                    .scriptBody.shouldBeEqualTo(script)
            }
        }
    }
}

