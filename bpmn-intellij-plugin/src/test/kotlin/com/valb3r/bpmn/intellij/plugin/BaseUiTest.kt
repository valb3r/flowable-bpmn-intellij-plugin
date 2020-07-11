package com.valb3r.bpmn.intellij.plugin

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.messages.MessageBus
import com.intellij.util.messages.MessageBusConnection
import com.nhaarman.mockitokotlin2.*
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.mock
import com.valb3r.bpmn.intellij.plugin.bpmn.api.BpmnParser
import com.valb3r.bpmn.intellij.plugin.bpmn.api.BpmnProcessObject
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.BpmnElementId
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.BpmnProcess
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.BpmnProcessBodyBuilder
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.BpmnSequenceFlow
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.WithParentId
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.events.boundary.BpmnBoundaryErrorEvent
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.subprocess.BpmnSubProcess
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.tasks.BpmnServiceTask
import com.valb3r.bpmn.intellij.plugin.bpmn.api.diagram.DiagramElement
import com.valb3r.bpmn.intellij.plugin.bpmn.api.diagram.DiagramElementId
import com.valb3r.bpmn.intellij.plugin.bpmn.api.diagram.elements.*
import com.valb3r.bpmn.intellij.plugin.bpmn.api.events.Event
import com.valb3r.bpmn.intellij.plugin.bpmn.api.info.Property
import com.valb3r.bpmn.intellij.plugin.bpmn.api.info.PropertyType
import com.valb3r.bpmn.intellij.plugin.events.BpmnEdgeObjectAddedEvent
import com.valb3r.bpmn.intellij.plugin.events.BpmnShapeObjectAddedEvent
import com.valb3r.bpmn.intellij.plugin.events.FileCommitter
import com.valb3r.bpmn.intellij.plugin.events.updateEventsRegistry
import com.valb3r.bpmn.intellij.plugin.properties.SelectedValueAccessor
import com.valb3r.bpmn.intellij.plugin.properties.TextValueAccessor
import com.valb3r.bpmn.intellij.plugin.properties.propertiesVisualizer
import com.valb3r.bpmn.intellij.plugin.render.*
import com.valb3r.bpmn.intellij.plugin.state.currentStateProvider
import org.amshove.kluent.*
import org.junit.jupiter.api.BeforeEach
import org.mockito.ArgumentMatchers
import java.awt.FontMetrics
import java.awt.Graphics2D
import java.awt.font.FontRenderContext
import java.awt.geom.AffineTransform
import java.awt.geom.Point2D
import java.awt.geom.Rectangle2D
import java.nio.charset.StandardCharsets
import java.util.*
import javax.swing.Icon
import javax.swing.JTable
import javax.swing.table.TableColumn
import javax.swing.table.TableColumnModel


// These are global singletons as i.e. `initializeNewElementsFactory` is
private val textFieldsConstructed: MutableMap<Pair<BpmnElementId, PropertyType>, TextValueAccessor> = mutableMapOf()
private val boolFieldsConstructed: MutableMap<Pair<BpmnElementId, PropertyType>, SelectedValueAccessor> = mutableMapOf()
private val comboboxFactory = { id: BpmnElementId, type: PropertyType, value: String, allowedValues: Set<String> -> textFieldsConstructed.computeIfAbsent(Pair(id, type)) {
    val res = mock<TextValueAccessor>()
    whenever(res.text).thenReturn(value)
    return@computeIfAbsent res
} }
private val editorFactory = { id: BpmnElementId, type: PropertyType, value: String -> textFieldsConstructed.computeIfAbsent(Pair(id, type)) {
    val res = mock<TextValueAccessor>()
    whenever(res.text).thenReturn(value)
    return@computeIfAbsent res
} }
private val checkboxFieldFactory = { id: BpmnElementId, type: PropertyType, value: Boolean -> boolFieldsConstructed.computeIfAbsent(Pair(id, type)) {
    val res = mock<SelectedValueAccessor>()
    whenever(res.isSelected).thenReturn(value)
    return@computeIfAbsent res
} }
internal abstract class BaseUiTest {

    protected val newLink = "NEW-SEQUENCE"
    protected val doDel = "DEL"

    protected val icon = "dummy-icon.svg".asResource()

    protected val startElemX = 0.0f
    protected val startElemY = 0.0f
    protected val serviceTaskSize = 60.0f
    protected val boundaryEventSize = 15.0f
    protected val subProcessElemX = 0.0f
    protected val subProcessElemY = 0.0f
    protected val subProcessSize = 200.0f

    protected val endElemX = 10 * serviceTaskSize
    protected val endElemY = 0.0f
    protected val endElemMidY = serviceTaskSize / 2.0f

    protected val diagramMainElementId = DiagramElementId("diagramMainElement")
    protected val diagramMainPlaneElementId = DiagramElementId("diagramMainPlaneElement")

    protected val parentProcessBpmnId = BpmnElementId("processElem")
    protected val optionalBoundaryErrorEventBpmnId = BpmnElementId("boundaryErrorEvent")
    protected val subprocessBpmnId = BpmnElementId("subProcess")
    protected val subprocessInSubProcessBpmnId = BpmnElementId("nestedSubProcess")
    protected val serviceTaskStartBpmnId = BpmnElementId("startServiceTask")
    protected val serviceTaskEndBpmnId = BpmnElementId("endServiceTask")
    protected val sequenceFlowBpmnId = BpmnElementId("sequenceFlow")

    protected val optionalBoundaryErrorEventDiagramId = DiagramElementId("DIAGRAM-boundaryErrorEvent")
    protected val subprocessInSubProcessDiagramId = DiagramElementId("DIAGRAM-nestedSubProcess")
    protected val subprocessDiagramId = DiagramElementId("DIAGRAM-subProcess")
    protected val serviceTaskStartDiagramId = DiagramElementId("DIAGRAM-startServiceTask")
    protected val serviceTaskEndDiagramId = DiagramElementId("DIAGRAM-endServiceTask")
    protected val sequenceFlowDiagramId = DiagramElementId("DIAGRAM-sequenceFlow")

    protected val bpmnServiceTaskStart = BpmnServiceTask(serviceTaskStartBpmnId, null, null, null, null, null, null, null, null, null, null, null, null)
    protected val bpmnSubProcess = BpmnSubProcess(subprocessBpmnId, null, null, null, null, false, false)
    protected val bpmnNestedSubProcess = BpmnSubProcess(subprocessInSubProcessBpmnId, null, null, null, null, false, false)
    protected val bpmnServiceTaskEnd = BpmnServiceTask(serviceTaskEndBpmnId, null, null, null, null, null, null, null, null, null, null, null, null)
    protected val bpmnSequenceFlow = BpmnSequenceFlow(sequenceFlowBpmnId, null, null, null, null, null)
    protected val diagramServiceTaskStart = ShapeElement(serviceTaskStartDiagramId, bpmnServiceTaskStart.id, BoundsElement(startElemX, startElemY, serviceTaskSize, serviceTaskSize))
    protected val diagramServiceTaskEnd = ShapeElement(serviceTaskEndDiagramId, bpmnServiceTaskEnd.id, BoundsElement(endElemX, endElemY, serviceTaskSize, serviceTaskSize))
    protected val diagramSubProcess = ShapeElement(subprocessDiagramId, subprocessBpmnId, BoundsElement(subProcessElemX, subProcessElemY, subProcessSize, subProcessSize))
    protected val diagramNestedSubProcess = ShapeElement(subprocessInSubProcessDiagramId, subprocessInSubProcessBpmnId, BoundsElement(subProcessElemX, subProcessElemY, subProcessSize / 2.0f, subProcessSize / 2.0f))
    protected val diagramSequenceFlow = EdgeElement(sequenceFlowDiagramId, sequenceFlowBpmnId, listOf(WaypointElement(endElemX, endElemY), WaypointElement(endElemX - 20.0f, endElemY - 20.0f), WaypointElement(endElemX - 30.0f, endElemY - 30.0f)))

    protected val icons = mock<IconProvider>()
    protected val renderer = spy(DefaultBpmnProcessRenderer(icons))
    protected val canvasBuilder = CanvasBuilder(renderer)
    protected val canvas = setCanvas(Canvas(DefaultCanvasConstants().copy(baseCursorSize = 3.0f))) // Using small cursor size for clarity
    protected var renderResult: Map<DiagramElementId, AreaWithZindex>? = null

    protected val basicProcess = BpmnProcessObject(
            BpmnProcess(
                    parentProcessBpmnId,
                    "mainProcess",
                    null,
                    null,
                    null,
                    null
            ),
            mutableListOf()
    )

    protected val sequenceIcon = mock<Icon>()
    protected val graphics = mock<Graphics2D>()
    protected val fontMetrics = mock<FontMetrics>()
    protected val messageBus = mock<MessageBus>()
    protected val messageBusConnection = mock<MessageBusConnection>()
    protected val parser = mock<BpmnParser>()
    protected val fileCommitter = mock<FileCommitter>()
    protected val project = mock<Project>()
    protected val virtualFile = mock<VirtualFile>()
    protected val columnModel = mock<TableColumnModel>()
    protected val tableColumn = mock<TableColumn>()
    protected val propertiesTable = mock<JTable>()

    @BeforeEach
    fun setupMocks() {
        textFieldsConstructed.clear()
        boolFieldsConstructed.clear()

        whenever(propertiesTable.columnModel).thenReturn(columnModel)
        whenever(columnModel.getColumn(ArgumentMatchers.anyInt())).thenReturn(tableColumn)
        prepareGraphics(graphics)
        whenever(virtualFile.contentsToByteArray()).thenReturn(ByteArray(0))
        whenever(project.messageBus).thenReturn(messageBus)
        whenever(messageBus.connect()).thenReturn(messageBusConnection)

        whenever(sequenceIcon.iconWidth).thenReturn(15)
        whenever(sequenceIcon.iconHeight).thenReturn(15)
        whenever(icons.sequence).thenReturn(sequenceIcon)
        whenever(icons.recycleBin).thenReturn(icon)
        whenever(icons.exclusiveGateway).thenReturn(icon)
        whenever(icons.boundaryErrorEvent).thenReturn(icon)
        whenever(icons.gear).thenReturn(mock())
        whenever(icons.redo).thenReturn(mock())
        whenever(icons.undo).thenReturn(mock())
        whenever(icons.dragToResizeBottom).thenReturn(mock())
        whenever(icons.dragToResizeTop).thenReturn(mock())

        doAnswer {
            val result = it.callRealMethod()!! as Map<DiagramElementId, AreaWithZindex>
            renderResult = result
            return@doAnswer result
        }.whenever(renderer).render(any())
    }

    protected fun prepareGraphics(graphics2D: Graphics2D) {
        whenever(graphics2D.fontMetrics).thenReturn(fontMetrics)
        val frc = mock<FontRenderContext>()
        whenever(frc.transform).thenReturn(AffineTransform())
        whenever(graphics2D.fontRenderContext).thenReturn(frc)
        whenever(graphics2D.transform).thenReturn(AffineTransform())
        whenever(fontMetrics.getStringBounds(any(), eq(graphics2D))).thenReturn(Rectangle2D.Float())
        whenever(graphics2D.create()).thenReturn(graphics2D)
    }

    protected fun changeIdViaPropertiesVisualizer(diagramElementId: DiagramElementId, elementId: BpmnElementId, newId: String) {
        val id = Pair(elementId, PropertyType.ID)
        clickOnId(diagramElementId)
        propertiesVisualizer().visualize(
                currentStateProvider().currentState().elemPropertiesByStaticElementId,
                elementId
        )
        whenever(textFieldsConstructed[id]!!.text).thenReturn(newId)
        propertiesVisualizer().clear()
    }

    protected fun newServiceTask(intermediateX: Float, intermediateY: Float): BpmnElementId {
        val task = bpmnServiceTaskStart.copy(id = BpmnElementId("sid-" + UUID.randomUUID().toString()))
        val shape = diagramServiceTaskStart.copy(
                id = DiagramElementId("sid-" + UUID.randomUUID().toString()),
                bounds = BoundsElement(intermediateX, intermediateY, serviceTaskSize, serviceTaskSize)
        )
        updateEventsRegistry().addObjectEvent(
                BpmnShapeObjectAddedEvent(WithParentId(basicProcess.process.id, task), shape, mapOf(PropertyType.ID to Property(task.id)))
        )

        return task.id
    }

    protected fun dragToAndVerifyButDontStop(point: Point2D.Float, target: Point2D.Float, lastEndpointId: DiagramElementId) {
        dragToButDontStop(point, target)
        val dragAboutToFinishArea = elemAreaById(lastEndpointId)
        dragAboutToFinishArea.area.bounds2D.centerX.shouldBeNear(target.x.toDouble(), 0.1)
        dragAboutToFinishArea.area.bounds2D.centerY.shouldBeNear(target.y.toDouble(), 0.1)
    }

    protected fun dragToButDontStop(point: Point2D.Float, target: Point2D.Float) {
        canvas.startSelectionOrDrag(point)
        canvas.paintComponent(graphics)
        canvas.dragOrSelectWithLeftButton(point, target)
        canvas.paintComponent(graphics)
    }

    protected fun addSequenceElementOnFirstTaskAndValidateCommittedExactOnce(): BpmnEdgeObjectAddedEvent {
        addSequenceElementOnFirstTask()

        argumentCaptor<List<Event>>().let {
            verify(fileCommitter).executeCommitAndGetHash(any(), it.capture(), any(), any())
            it.firstValue.shouldHaveSize(1)
            return it.firstValue.filterIsInstance<BpmnEdgeObjectAddedEvent>().shouldHaveSingleItem()
        }
    }

    protected fun addSequenceElementOnFirstTaskAndValidateCommittedAtLeastOnceAndSelectOne(): BpmnEdgeObjectAddedEvent {
        clickOnId(serviceTaskStartDiagramId)
        verifyServiceTasksAreDrawn()
        val newLink = findExactlyOneNewLinkElem().shouldNotBeNull()
        val newLinkLocation = clickOnId(newLink)
        dragToButDontStop(newLinkLocation, elementCenter(serviceTaskEndDiagramId))
        canvas.paintComponent(graphics)
        canvas.stopDragOrSelect()
        canvas.paintComponent(graphics)

        argumentCaptor<List<Event>>().let {
            verify(fileCommitter, atLeastOnce()).executeCommitAndGetHash(any(), it.capture(), any(), any())
            return it.lastValue.filterIsInstance<BpmnEdgeObjectAddedEvent>().last().shouldNotBeNull()
        }
    }

    protected fun addSequenceElementOnFirstTask() {
        clickOnId(serviceTaskStartDiagramId)
        verifyServiceTasksAreDrawn()
        val newLink = findExactlyOneNewLinkElem().shouldNotBeNull()
        val newLinkLocation = clickOnId(newLink)
        dragToButDontStop(newLinkLocation, elementCenter(serviceTaskEndDiagramId))
        canvas.paintComponent(graphics)
        canvas.stopDragOrSelect()
        canvas.paintComponent(graphics)
    }

    protected fun clickOnId(elemId: DiagramElementId): Point2D.Float {
        val point = elementCenter(elemId)
        canvas.click(point)
        canvas.paintComponent(graphics)
        return point
    }

    protected fun elementCenter(elemId: DiagramElementId): Point2D.Float {
        val area = renderResult?.get(elemId).shouldNotBeNull()
        val bounds = area.area.bounds2D.shouldNotBeNull()
        return Point2D.Float(bounds.centerX.toFloat(), bounds.centerY.toFloat())
    }

    protected fun initializeCanvas() {
        canvasBuilder.build({ fileCommitter }, parser, propertiesTable, comboboxFactory, editorFactory, editorFactory, editorFactory, checkboxFieldFactory, canvas, project, virtualFile)
        canvas.paintComponent(graphics)
    }

    protected fun verifyServiceTasksAreDrawn() {
        renderResult.shouldNotBeNull().shouldHaveKey(serviceTaskStartDiagramId)
        renderResult.shouldNotBeNull().shouldHaveKey(serviceTaskEndDiagramId)
        renderResult?.get(serviceTaskStartDiagramId)!!.area.bounds2D.shouldBeEqualTo(Rectangle2D.Float(startElemX, startElemY, serviceTaskSize, serviceTaskSize))
        renderResult?.get(serviceTaskEndDiagramId)!!.area.bounds2D.shouldBeEqualTo(Rectangle2D.Float(endElemX, endElemY, serviceTaskSize, serviceTaskSize))
    }

    protected fun prepareOneSubProcessView() {
        val process = basicProcess.copy(
                basicProcess.process.copy(
                        body = BpmnProcessBodyBuilder.builder()
                                .setSubProcess(listOf(bpmnSubProcess))
                                .create()
                ),
                listOf(DiagramElement(
                        diagramMainElementId,
                        PlaneElement(diagramMainPlaneElementId, basicProcess.process.id, listOf(diagramSubProcess), listOf()))
                )
        )
        whenever(parser.parse("")).thenReturn(process)
        initializeCanvas()
    }

    protected fun prepareOneSubProcessWithTwoServiceTasksView() {
        val process = basicProcess.copy(
                basicProcess.process.copy(
                        body = BpmnProcessBodyBuilder.builder()
                                .setServiceTask(listOf(bpmnServiceTaskStart, bpmnServiceTaskEnd))
                                .setSubProcess(listOf(bpmnSubProcess))
                                .create(),
                        children = mapOf(
                                subprocessBpmnId to BpmnProcessBodyBuilder.builder()
                                        .setServiceTask(listOf(bpmnServiceTaskStart, bpmnServiceTaskEnd))
                                        .create()
                        )
                ),
                listOf(
                        DiagramElement(
                                diagramMainElementId,
                                PlaneElement(
                                        diagramMainPlaneElementId,
                                        basicProcess.process.id,
                                        listOf(diagramSubProcess, diagramServiceTaskStart, diagramServiceTaskEnd),
                                        listOf()
                                )
                        )
                )
        )
        whenever(parser.parse("")).thenReturn(process)
        initializeCanvas()
    }

    protected fun prepareOneSubProcessThenNestedSubProcessWithOneServiceTaskView() {
        val process = basicProcess.copy(
                basicProcess.process.copy(
                        body = BpmnProcessBodyBuilder.builder()
                                .setServiceTask(listOf(bpmnServiceTaskStart, bpmnServiceTaskEnd))
                                .setSubProcess(listOf(bpmnSubProcess))
                                .create(),
                        children = mapOf(
                                subprocessBpmnId to BpmnProcessBodyBuilder.builder()
                                        .setSubProcess(listOf(bpmnNestedSubProcess))
                                        .create(),
                                subprocessInSubProcessBpmnId to BpmnProcessBodyBuilder.builder()
                                        .setServiceTask(listOf(bpmnServiceTaskStart))
                                        .create()
                        )
                ),
                listOf(
                        DiagramElement(
                                diagramMainElementId,
                                PlaneElement(
                                        diagramMainPlaneElementId,
                                        basicProcess.process.id,
                                        listOf(diagramSubProcess, diagramNestedSubProcess, diagramServiceTaskStart),
                                        listOf()
                                )
                        )
                )
        )
        whenever(parser.parse("")).thenReturn(process)
        initializeCanvas()
    }

    protected fun prepareOneSubProcessThenNestedSubProcessWithReversedChildParentOrder() {
        val process = basicProcess.copy(
                basicProcess.process.copy(
                        body = BpmnProcessBodyBuilder.builder()
                                .setSubProcess(listOf(bpmnSubProcess))
                                .create(),
                        children = mapOf(
                                subprocessBpmnId to BpmnProcessBodyBuilder.builder()
                                        .setSubProcess(listOf(bpmnNestedSubProcess))
                                        .create(),
                                subprocessInSubProcessBpmnId to BpmnProcessBodyBuilder.builder()
                                        .create()
                        )
                ),
                listOf(
                        DiagramElement(
                                diagramMainElementId,
                                PlaneElement(
                                        diagramMainPlaneElementId,
                                        basicProcess.process.id,
                                        listOf(diagramNestedSubProcess, diagramSubProcess),
                                        listOf()
                                )
                        )
                )
        )
        whenever(parser.parse("")).thenReturn(process)
        initializeCanvas()
    }


    protected fun prepareTwoServiceTaskView() {
        val process = basicProcess.copy(
                basicProcess.process.copy(
                        body = BpmnProcessBodyBuilder.builder()
                                .setServiceTask(listOf(bpmnServiceTaskStart, bpmnServiceTaskEnd))
                                .create()
                ),
                listOf(DiagramElement(
                        diagramMainElementId,
                        PlaneElement(diagramMainPlaneElementId, basicProcess.process.id, listOf(diagramServiceTaskStart, diagramServiceTaskEnd), listOf()))
                )
        )
        whenever(parser.parse("")).thenReturn(process)
        initializeCanvas()
    }

    protected fun prepareServiceTaskWithAttachedBoundaryEventView() {
        val boundaryEventOnServiceTask = BpmnBoundaryErrorEvent(optionalBoundaryErrorEventBpmnId, null, serviceTaskStartBpmnId, null)
        val boundaryEventOnServiceTaskShape = ShapeElement(
                optionalBoundaryErrorEventDiagramId,
                optionalBoundaryErrorEventBpmnId,
                BoundsElement(startElemX + serviceTaskSize / 5.0f, startElemX + serviceTaskSize / 5.0f, boundaryEventSize, boundaryEventSize)
        )

        val process = basicProcess.copy(
                basicProcess.process.copy(
                        body = BpmnProcessBodyBuilder.builder()
                                .setServiceTask(listOf(bpmnServiceTaskStart))
                                .setBoundaryErrorEvent(listOf(boundaryEventOnServiceTask))
                                .create()
                ),
                listOf(DiagramElement(
                        diagramMainElementId,
                        PlaneElement(diagramMainPlaneElementId, basicProcess.process.id, listOf(diagramServiceTaskStart, boundaryEventOnServiceTaskShape), listOf()))
                )
        )
        whenever(parser.parse("")).thenReturn(process)
        initializeCanvas()
    }

    protected fun prepareOneSubProcessServiceTaskWithAttachedBoundaryEventView() {
        val boundaryEventOnServiceTask = BpmnBoundaryErrorEvent(optionalBoundaryErrorEventBpmnId, null, serviceTaskStartBpmnId, null)
        val boundaryEventOnServiceTaskShape = ShapeElement(
                optionalBoundaryErrorEventDiagramId,
                optionalBoundaryErrorEventBpmnId,
                BoundsElement(startElemX + serviceTaskSize / 5.0f, startElemX + serviceTaskSize / 5.0f, boundaryEventSize, boundaryEventSize)
        )

        val process = basicProcess.copy(
                basicProcess.process.copy(
                        body = BpmnProcessBodyBuilder.builder()
                                .setSubProcess(listOf(bpmnSubProcess))
                                .create(),
                        children = mapOf(
                                subprocessBpmnId to BpmnProcessBodyBuilder.builder()
                                        .setServiceTask(listOf(bpmnServiceTaskStart))
                                        .setBoundaryErrorEvent(listOf(boundaryEventOnServiceTask))
                                        .create()
                        )
                ),
                listOf(DiagramElement(
                        diagramMainElementId,
                        PlaneElement(diagramMainPlaneElementId, basicProcess.process.id, listOf(diagramSubProcess, diagramServiceTaskStart, boundaryEventOnServiceTaskShape), listOf()))
                )
        )
        whenever(parser.parse("")).thenReturn(process)
        initializeCanvas()
    }

    protected fun prepareOneSubProcessWithServiceTaskAndAttachedBoundaryEventOneNestedSubprocessAndServiceTaskWithSequence() {
        val boundaryEventOnServiceTask = BpmnBoundaryErrorEvent(optionalBoundaryErrorEventBpmnId, null, serviceTaskStartBpmnId, null)
        val boundaryEventOnServiceTaskShape = ShapeElement(
                optionalBoundaryErrorEventDiagramId,
                optionalBoundaryErrorEventBpmnId,
                BoundsElement(startElemX + serviceTaskSize / 5.0f, startElemX + serviceTaskSize / 5.0f, boundaryEventSize, boundaryEventSize)
        )

        val process = basicProcess.copy(
                basicProcess.process.copy(
                        body = BpmnProcessBodyBuilder.builder()
                                .setSubProcess(listOf(bpmnSubProcess))
                                .create(),
                        children = mapOf(
                                subprocessBpmnId to BpmnProcessBodyBuilder.builder()
                                        .setSubProcess(listOf(bpmnNestedSubProcess))
                                        .setServiceTask(listOf(bpmnServiceTaskStart))
                                        .setBoundaryErrorEvent(listOf(boundaryEventOnServiceTask))
                                        .create(),
                                subprocessInSubProcessBpmnId to BpmnProcessBodyBuilder.builder()
                                        .setServiceTask(listOf(bpmnServiceTaskEnd))
                                        .setSequenceFlow(listOf(bpmnSequenceFlow))
                                        .create()
                        )
                ),
                listOf(
                        DiagramElement(
                                diagramMainElementId,
                                PlaneElement(
                                        diagramMainPlaneElementId,
                                        basicProcess.process.id,
                                        listOf(diagramNestedSubProcess, diagramSubProcess, diagramServiceTaskStart, diagramServiceTaskEnd, boundaryEventOnServiceTaskShape),
                                        listOf(diagramSequenceFlow)
                                )
                        )
                )
        )
        whenever(parser.parse("")).thenReturn(process)
        initializeCanvas()
    }

    protected fun elemAreaById(id: DiagramElementId) = renderResult?.get(id)!!

    protected fun findFirstNewLinkElem() = renderResult?.keys?.firstOrNull { it.id.contains(newLink) }
    protected fun findFirstDeleteElem() = renderResult?.keys?.firstOrNull { it.id.contains(doDel) }

    protected fun findExactlyOneNewLinkElem() = renderResult?.keys?.filter { it.id.contains(newLink) }?.shouldHaveSize(1)?.first()
    protected fun findExactlyOneDeleteElem() = renderResult?.keys?.filter { it.id.contains(doDel) }?.shouldHaveSize(1)?.first()

    protected fun String.asResource(): String? = UiEditorLightE2ETest::class.java.classLoader.getResource(this)?.readText(StandardCharsets.UTF_8)
}