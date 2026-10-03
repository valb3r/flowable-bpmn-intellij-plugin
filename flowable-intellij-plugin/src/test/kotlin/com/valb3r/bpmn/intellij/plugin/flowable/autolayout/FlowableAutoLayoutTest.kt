package com.valb3r.bpmn.intellij.plugin.flowable.autolayout

import com.valb3r.bpmn.intellij.plugin.autolayout.BpmnAutoLayout
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.BpmnElementId
import com.valb3r.bpmn.intellij.plugin.flowable.parser.FlowableParser
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import java.nio.file.Files
import java.nio.file.Path

class FlowableAutoLayoutTest {

    companion object {
        private const val EMF_REFERENCE_CLEANER_PROPERTY = "org.eclipse.emf.common.util.ReferenceClearingQueue"
        private var previousEmfReferenceCleanerSetting: String? = null

        @JvmStatic
        @BeforeAll
        fun disableEmfReferenceCleanerThreadForIdeTest() {
            previousEmfReferenceCleanerSetting = System.getProperty(EMF_REFERENCE_CLEANER_PROPERTY)
            // EMF's background cleaner otherwise fails IntelliJ's per-test thread leak check.
            System.setProperty(EMF_REFERENCE_CLEANER_PROPERTY, "false")
        }

        @JvmStatic
        @AfterAll
        fun restoreEmfReferenceCleanerSetting() {
            previousEmfReferenceCleanerSetting?.let {
                System.setProperty(EMF_REFERENCE_CLEANER_PROPERTY, it)
            } ?: System.clearProperty(EMF_REFERENCE_CLEANER_PROPERTY)
        }
    }

    @Test
    fun `layouts Flowable XML and writes generated BPMN DI back to XML`() {
        val inputXml = resource("test-layout-1.bpmn20.xml")
        val parser = FlowableParser()
        assertNull(parser.validateForErrors(inputXml))

        val parsed = parser.parse(inputXml)
        assertTrue(parsed.diagram.isEmpty())
        val laidOut = BpmnAutoLayout().layout(parsed)
        val outputXml = parser.updateDiagram(inputXml, laidOut.diagram)

        assertNull(parser.validateForErrors(outputXml))
        assertTrue(outputXml.contains("<bpmndi:BPMNShape"))
        assertTrue(outputXml.contains("<bpmndi:BPMNEdge"))
        assertTrue(outputXml.contains("flowable:assignee=\"demo\""))

        val writtenDiagram = parser.parse(outputXml).diagram.single().bpmnPlane
        val laidOutPlane = laidOut.diagram.single().bpmnPlane
        val writtenShapes = writtenDiagram.bpmnShape.orEmpty().associateBy { it.bpmnElement }
        laidOutPlane.bpmnShape.orEmpty().forEach { expected ->
            val actual = writtenShapes.getValue(expected.bpmnElement).rectBounds()
            val expectedBounds = expected.rectBounds()
            assertEquals(expectedBounds.x, actual.x, 0.01f)
            assertEquals(expectedBounds.y, actual.y, 0.01f)
            assertEquals(expectedBounds.width, actual.width, 0.01f)
            assertEquals(expectedBounds.height, actual.height, 0.01f)
        }

        val startBounds = writtenShapes.getValue(BpmnElementId("start")).rectBounds()
        val taskBounds = writtenShapes.getValue(BpmnElementId("task")).rectBounds()
        assertTrue(taskBounds.x - (startBounds.x + startBounds.width) >= 99.0f)

        val writtenEdges = writtenDiagram.bpmnEdge.orEmpty().associateBy { it.bpmnElement }
        laidOutPlane.bpmnEdge.orEmpty().forEach { expected ->
            val actualWaypoints = writtenEdges.getValue(expected.bpmnElement).waypoint.orEmpty()
            val expectedWaypoints = expected.waypoint.orEmpty()
            assertEquals(expectedWaypoints.size, actualWaypoints.size)
            expectedWaypoints.zip(actualWaypoints).forEach { (expectedPoint, actualPoint) ->
                assertEquals(expectedPoint.x, actualPoint.x, 0.01f)
                assertEquals(expectedPoint.y, actualPoint.y, 0.01f)
            }
        }
    }

    @Disabled("Enable manually to generate an XML file for layout debugging")
    @Test
    fun `writes laid out Flowable XML fixture for debugging`() {
        val inputXml = resource("in-test-layout.bpmn20.xml")
        val parser = FlowableParser()
        val process = parser.parse(inputXml)
        val laidOut = BpmnAutoLayout().layout(process)
        val outputXml = parser.updateDiagram(inputXml, laidOut.diagram)

        val output = Path.of("build", "out-test-layout.bpmn20.xml")
        Files.createDirectories(output.parent)
        Files.writeString(output, outputXml)
    }

    private fun resource(name: String): String =
        checkNotNull(javaClass.classLoader.getResource(name)) { "Missing test resource: $name" }.readText()
}
