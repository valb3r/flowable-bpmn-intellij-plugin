package com.valb3r.bpmn.intellij.plugin.core.tests

import com.google.common.hash.Hashing
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.JBMenuItem
import com.intellij.openapi.ui.JBPopupMenu
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.messages.MessageBus
import com.intellij.util.messages.MessageBusConnection
import com.nhaarman.mockitokotlin2.*
import com.valb3r.bpmn.intellij.plugin.bpmn.api.BpmnParser
import com.valb3r.bpmn.intellij.plugin.bpmn.api.BpmnProcessObject
import com.valb3r.bpmn.intellij.plugin.bpmn.api.PropertyTable
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.BpmnElementId
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.BpmnProcess
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.BpmnProcessBody
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.*
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.events.boundary.BpmnBoundaryErrorEvent
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.subprocess.BpmnSubProcess
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.tasks.BpmnSendEventTask
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.tasks.BpmnServiceTask
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.tasks.BpmnUserTask
import com.valb3r.bpmn.intellij.plugin.bpmn.api.diagram.DiagramElement
import com.valb3r.bpmn.intellij.plugin.bpmn.api.diagram.DiagramElementId
import com.valb3r.bpmn.intellij.plugin.bpmn.api.diagram.elements.*
import com.valb3r.bpmn.intellij.plugin.bpmn.api.events.EventPropagatableToXml
import com.valb3r.bpmn.intellij.plugin.bpmn.api.info.FunctionalGroupType
import com.valb3r.bpmn.intellij.plugin.bpmn.api.info.Property
import com.valb3r.bpmn.intellij.plugin.bpmn.api.info.PropertyType
import com.valb3r.bpmn.intellij.plugin.core.CanvasBuilder
import com.valb3r.bpmn.intellij.plugin.core.events.*
import com.valb3r.bpmn.intellij.plugin.core.newelements.newElementsFactory
import com.valb3r.bpmn.intellij.plugin.core.popupmenu.currentPopupMenuItemUiComponentSupplier
import com.valb3r.bpmn.intellij.plugin.core.popupmenu.currentPopupMenuUiComponentSupplier
import com.valb3r.bpmn.intellij.plugin.core.properties.*
import com.valb3r.bpmn.intellij.plugin.core.render.*
import com.valb3r.bpmn.intellij.plugin.core.render.uieventbus.UiEventBus
import com.valb3r.bpmn.intellij.plugin.core.render.uieventbus.setUiEventBus
import com.valb3r.bpmn.intellij.plugin.core.settings.BaseBpmnPluginSettingsState
import com.valb3r.bpmn.intellij.plugin.core.settings.currentSettingsStateProvider
import com.valb3r.bpmn.intellij.plugin.core.state.currentStateProvider
import com.valb3r.bpmn.intellij.plugin.core.ui.components.popupmenu.CanvasPopupMenuProvider
import com.valb3r.bpmn.intellij.plugin.core.ui.components.popupmenu.registerPopupMenuProvider
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
import javax.swing.JButton
import javax.swing.JTable
import javax.swing.SwingConstants
import javax.swing.plaf.basic.BasicArrowButton
import javax.swing.table.TableColumn
import javax.swing.table.TableColumnModel

abstract class BaseUiTest {

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
    protected val propertiesTable = spy(JTable())
    protected var popupMenuProvider = mock<CanvasPopupMenuProvider>()

    protected val newLink = "NEW-SEQUENCE"
    protected val doDel = "DEL"
    protected val doChangeType = "CHANGE-TYPE"

    protected val icon = "dummy-icon.svg".asResource()

    protected val userTaskElemX = 100.0f
    protected val userTaskElemY = 100.0f
    protected val startElemX = 0.0f
    protected val startElemY = 0.0f
    protected val taskSize = 60.0f
    protected val boundaryEventSize = 15.0f
    protected val subProcessElemX = 0.0f
    protected val subProcessElemY = 0.0f

    protected val endElemX = 10 * taskSize
    protected val endElemY = 0.0f
    protected val endElemMidY = taskSize / 2.0f

    protected val subProcessSize = endElemX + taskSize * 2
    protected val nestedSubProcessSize = subProcessSize / 2.0f

    protected val diagramMainElementId = DiagramElementId("diagramMainElement")
    protected val diagramMainPlaneElementId = DiagramElementId("diagramMainPlaneElement")

    protected val parentProcessBpmnId = BpmnElementId("processElem")
    protected val optionalBoundaryErrorEventBpmnId = BpmnElementId("boundaryErrorEvent")
    protected val subprocessBpmnId = BpmnElementId("subProcess")
    protected val subprocessInSubProcessBpmnId = BpmnElementId("nestedSubProcess")
    protected val serviceTaskStartBpmnId = BpmnElementId("startServiceTask")
    protected val serviceTaskEndBpmnId = BpmnElementId("endServiceTask")
    protected val userTaskBpmnId = BpmnElementId("userTask")
    protected val sendEventTaskBpmnId = BpmnElementId("sendEventTask")
    protected val sendEventTaskBpmnIdFilled = BpmnElementId("sendEventTaskFilled")
    protected val sequenceFlowBpmnId = BpmnElementId("sequenceFlow")

    protected val optionalBoundaryErrorEventDiagramId = DiagramElementId("DIAGRAM-boundaryErrorEvent")
    protected val subprocessInSubProcessDiagramId = DiagramElementId("DIAGRAM-nestedSubProcess")
    protected val subprocessDiagramId = DiagramElementId("DIAGRAM-subProcess")
    protected val serviceTaskStartDiagramId = DiagramElementId("DIAGRAM-startServiceTask")
    protected val serviceTaskEndDiagramId = DiagramElementId("DIAGRAM-endServiceTask")
    protected val userTaskDiagramId = DiagramElementId("DIAGRAM-userTask")
    protected val sendEventTaskDiagramId = DiagramElementId("DIAGRAM-sendEventTask")

    protected val sequenceFlowDiagramId = DiagramElementId("DIAGRAM-sequenceFlow")
    protected var bpmnSendEventTask = BpmnSendEventTask(sendEventTaskBpmnId, eventExtensionElements = listOf())
    protected val bpmnServiceTaskStart = BpmnServiceTask(serviceTaskStartBpmnId, "Start service task", "Start service task docs")
    protected val bpmnUserTask = BpmnUserTask(userTaskBpmnId, "Name user task", formPropertiesExtension = listOf(ExtensionFormProperty("Property ID", "Name property", null
        , null, null, null, null, value = listOf(
        ExtensionFormPropertyValue("formPropertyValueId", "formPropertyValueName")
    ))))
    protected val bpmnSubProcess = BpmnSubProcess(subprocessBpmnId, triggeredByEvent = false, transactionalSubprocess = false)
    protected val bpmnNestedSubProcess = BpmnSubProcess(subprocessInSubProcessBpmnId, triggeredByEvent = false, transactionalSubprocess = false)
    protected val bpmnServiceTaskEnd = BpmnServiceTask(serviceTaskEndBpmnId)
    protected val bpmnSequenceFlow = BpmnSequenceFlow(sequenceFlowBpmnId)
    protected val diagramServiceTaskStart = ShapeElement(serviceTaskStartDiagramId, bpmnServiceTaskStart.id, BoundsElement(startElemX, startElemY, taskSize, taskSize))
    protected val diagramServiceTaskEnd = ShapeElement(serviceTaskEndDiagramId, bpmnServiceTaskEnd.id, BoundsElement(endElemX, endElemY, taskSize, taskSize))
    protected val diagramUserTask = ShapeElement(userTaskDiagramId, bpmnUserTask.id, BoundsElement(userTaskElemX, userTaskElemY, taskSize, taskSize))
    protected val diagramSendEventTask = ShapeElement(sendEventTaskDiagramId, bpmnSendEventTask.id, BoundsElement(startElemX, startElemY, taskSize, taskSize))
    protected val diagramSubProcess = ShapeElement(subprocessDiagramId, subprocessBpmnId, BoundsElement(subProcessElemX, subProcessElemY, subProcessSize, subProcessSize))
    protected val diagramNestedSubProcess = ShapeElement(subprocessInSubProcessDiagramId, subprocessInSubProcessBpmnId, BoundsElement(subProcessElemX, subProcessElemY, nestedSubProcessSize, nestedSubProcessSize))
    protected val diagramSequenceFlow = EdgeElement(sequenceFlowDiagramId, sequenceFlowBpmnId, listOf(WaypointElement(endElemX, endElemY), WaypointElement(endElemX - 20.0f, endElemY - 20.0f), WaypointElement(endElemX - 30.0f, endElemY - 30.0f)))

    protected val icons = mock<IconProvider>()
    protected val renderer = spy(DefaultBpmnProcessRenderer(project, icons))
    protected val canvasBuilder = CanvasBuilder(renderer)
    protected var canvas = setCanvas(project, Canvas(project, DefaultCanvasConstants().copy(baseCursorSize = 3.0f))) // Using small cursor size for clarity
    protected val uiEventBus = setUiEventBus(project, UiEventBus())
    protected var renderResult: RenderResult? = null

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

    protected val basicProcessBody = BpmnProcessBody(null, null, null, null,
        null, null, null, null, null, null,
        null, null, null, null, null, null,
        null, null, null, null, null, null,
        null, null, null, null, null,
        null, null, null, null, null, null,
        null, null, null, null, null, null, null, null, null,
        null, null, null, null, null, null, null,
        null, null, null, null, null, null ,null, null, null, null, null
    )

    protected val textFieldsConstructed: MutableMap<Pair<BpmnElementId, PropertyType>, TextValueAccessor> = mutableMapOf()
    protected val multiLineTextFieldsConstructed: MutableMap<Pair<BpmnElementId, PropertyType>, TextValueAccessor> = mutableMapOf()
    protected val boolFieldsConstructed: MutableMap<Pair<BpmnElementId, PropertyType>, SelectedValueAccessor> = mutableMapOf()
    protected val buttonsConstructed: MutableMap<Pair<BpmnElementId, FunctionalGroupType>, JButton> = mutableMapOf()
    protected val arrowButtonsConstructed: MutableMap<BpmnElementId, BasicArrowButton> = mutableMapOf()

    protected val popupsConstructed: MutableMap<String, JBPopupMenu> = mutableMapOf()
    protected val popupItemsConstructed: MutableMap<String, JBMenuItem> = mutableMapOf()

    protected val comboboxFactory = { id: BpmnElementId, type: PropertyType, value: String, allowedValues: Set<String> -> textFieldsConstructed.computeIfAbsent(Pair(id, type)) {
        val res = mock<TextValueAccessor>()
        whenever(res.text).thenReturn(value)
        return@computeIfAbsent res
    } }
    protected val editorFactory = { id: BpmnElementId, type: PropertyType, value: String -> textFieldsConstructed.computeIfAbsent(Pair(id, type)) {
        val res = mock<TextValueAccessor>()
        whenever(res.text).thenReturn(value)
        return@computeIfAbsent res
    } }
    protected val multiLineEditorFactory = { id: BpmnElementId, type: PropertyType, value: String -> multiLineTextFieldsConstructed.computeIfAbsent(Pair(id, type)) {
        val res = mock<TextValueAccessor>()
        whenever(res.text).thenReturn(value)
        return@computeIfAbsent res
    } }
    protected val checkboxFieldFactory = { id: BpmnElementId, type: PropertyType, value: Boolean -> boolFieldsConstructed.computeIfAbsent(Pair(id, type)) {
        val res = mock<SelectedValueAccessor>()
        whenever(res.isSelected).thenReturn(value)
        return@computeIfAbsent res
    } }
    protected val buttonFactory = { id: BpmnElementId, type: FunctionalGroupType -> buttonsConstructed.computeIfAbsent(Pair(id, type)) {
        return@computeIfAbsent JButton(type.actionCaption)
    } }
    protected val arrowButtonFactory = { id: BpmnElementId -> arrowButtonsConstructed.computeIfAbsent(id) {
        return@computeIfAbsent BasicArrowButton(SwingConstants.SOUTH)
    } }

    private fun createButton(caption: String): JButton {
        return JButton(caption)
    }

    protected val popupsFactory = { id: String -> popupsConstructed.computeIfAbsent(id) {
        return@computeIfAbsent mock<JBPopupMenu>()
    } }
    protected val popupsMenuItemFactory = { name: String -> popupItemsConstructed.computeIfAbsent(name) {
        return@computeIfAbsent JBMenuItem(name)
    } }


    @BeforeEach
    fun setupMocks() {
        currentSettingsStateProvider.set{ object: BaseBpmnPluginSettingsState() {} }
        registerPopupMenuProvider(project, popupMenuProvider)
        whenever(popupMenuProvider.popupChangeShapeType(any())).thenReturn(mock())
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
        whenever(icons.rightAngle).thenReturn(icon)
        whenever(icons.selectParentSequence).thenReturn(icon)
        whenever(icons.wrench).thenReturn(icon)
        whenever(icons.gear).thenReturn(mock())
        whenever(icons.envelope).thenReturn(mock())
        whenever(icons.user).thenReturn(mock())
        whenever(icons.redo).thenReturn(mock())
        whenever(icons.undo).thenReturn(mock())
        whenever(icons.dragToResizeBottom).thenReturn(mock())
        whenever(icons.dragToResizeTop).thenReturn(mock())
        whenever(icons.zoomIn).thenReturn(mock())
        whenever(icons.zoomOut).thenReturn(mock())
        whenever(icons.zoomReset).thenReturn(mock())
        whenever(icons.centerImage).thenReturn(mock())
        whenever(icons.triggered).thenReturn(mock())
        whenever(icons.anchorOff).thenReturn(mock())
        whenever(icons.anchorOn).thenReturn(mock())

        doAnswer {
            val result = it.callRealMethod()!! as RenderResult
            renderResult = result
            return@doAnswer result
        }.whenever(renderer).render(any())

        currentPopupMenuUiComponentSupplier.set { popupsFactory(it) }
        currentPopupMenuItemUiComponentSupplier.set { name, _ -> popupsMenuItemFactory(name) }
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
        propertiesVisualizer(project).visualize(
                newElementsFactory(project),
                currentStateProvider(project).currentState().elemPropertiesByStaticElementId,
                elementId
        )
        whenever(textFieldsConstructed[id]!!.text).thenReturn(newId)
        propertiesVisualizer(project).clear()
    }

    protected fun changeSelectedIdViaPropertiesVisualizer(elementId: BpmnElementId, newId: String) {
        val id = Pair(elementId, PropertyType.ID)
        propertiesVisualizer(project).visualize(
                newElementsFactory(project),
                currentStateProvider(project).currentState().elemPropertiesByStaticElementId,
                elementId
        )
        whenever(textFieldsConstructed[id]!!.text).thenReturn(newId)
        propertiesVisualizer(project).clear()
    }

    protected fun changePropertySelectedElementVisualizer(elementId: BpmnElementId, type: PropertyType, newProp: String) {
        val property = Pair(elementId, type)
        propertiesVisualizer(project).visualize(
            newElementsFactory(project),
            currentStateProvider(project).currentState().elemPropertiesByStaticElementId,
            elementId
        )
        whenever(textFieldsConstructed[property]!!.text).thenReturn(newProp)
        propertiesVisualizer(project).clear()
    }


    protected fun newServiceTask(intermediateX: Float, intermediateY: Float): BpmnElementId {
        val task = bpmnServiceTaskStart.copy(id = BpmnElementId("sid-" + UUID.randomUUID().toString()))
        val shape = diagramServiceTaskStart.copy(
                id = DiagramElementId("sid-" + UUID.randomUUID().toString()),
                bounds = BoundsElement(intermediateX, intermediateY, taskSize, taskSize)
        )
        updateEventsRegistry(project).addObjectEvent(
                BpmnShapeObjectAddedEvent(WithParentId(basicProcess.process.id, task), shape, PropertyTable(mutableMapOf(PropertyType.ID to mutableListOf(Property(task.id)))))
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
        addSequenceElementOnFirstTaskToSecondTask()

        argumentCaptor<List<EventPropagatableToXml>>().let {
            verify(fileCommitter).executeCommitAndGetHash(any(), it.capture(), any(), any())
            it.firstValue.shouldHaveSize(3)
            it.firstValue.filterIsInstance<StringValueUpdatedEvent>().shouldHaveSize(2).map { value -> value.property }.toSet().shouldContainSame(arrayOf(PropertyType.BPMN_INCOMING, PropertyType.BPMN_OUTGOING))
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

        argumentCaptor<List<EventPropagatableToXml>>().let {
            verify(fileCommitter, atLeastOnce()).executeCommitAndGetHash(any(), it.capture(), any(), any())
            return it.lastValue.filterIsInstance<BpmnEdgeObjectAddedEvent>().last().shouldNotBeNull()
        }
    }

    protected fun addSequenceElementOnFirstTaskToSecondTask() {
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
        // imitate real event cycle
        canvas.startSelectionOrDrag(point)
        canvas.stopDragOrSelect()
        canvas.click(point)
        canvas.paintComponent(graphics)
        return point
    }

    protected fun elementCenter(elemId: DiagramElementId): Point2D.Float {
        val area = renderResult?.areas?.get(elemId).shouldNotBeNull()
        val bounds = area.area.bounds2D.shouldNotBeNull()
        return Point2D.Float(bounds.centerX.toFloat(), bounds.centerY.toFloat())
    }

    protected fun initializeCanvas() {
        canvasBuilder.build(
            { fileCommitter },
            parser, propertiesTable, comboboxFactory, editorFactory, editorFactory, editorFactory, multiLineEditorFactory, checkboxFieldFactory, buttonFactory, arrowButtonFactory, canvas, project, virtualFile
        )
        canvas.paintComponent(graphics)
    }



    protected fun verifyServiceTasksAreDrawn() {
        renderResult.shouldNotBeNull().areas.shouldHaveKey(serviceTaskStartDiagramId)
        renderResult.shouldNotBeNull().areas.shouldHaveKey(serviceTaskEndDiagramId)
        renderResult?.areas?.get(serviceTaskStartDiagramId)!!.area.bounds2D.shouldBeEqualTo(Rectangle2D.Float(startElemX, startElemY, taskSize, taskSize))
        renderResult?.areas?.get(serviceTaskEndDiagramId)!!.area.bounds2D.shouldBeEqualTo(Rectangle2D.Float(endElemX, endElemY, taskSize, taskSize))
    }

    protected fun prepareOneSubProcessView() {
        val process = basicProcess.copy(
                basicProcess.process.copy(
                        body = basicProcessBody.copy(subProcess = listOf(bpmnSubProcess))
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
                        body = basicProcessBody.copy(serviceTask = listOf(bpmnServiceTaskStart, bpmnServiceTaskEnd), subProcess = listOf(bpmnSubProcess)),
                        children = mapOf(
                                subprocessBpmnId to basicProcessBody.copy(serviceTask = listOf(bpmnServiceTaskStart, bpmnServiceTaskEnd))
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

    protected fun prepareOneSubProcessWithTwoLinkedServiceTasksView() {
        val process = basicProcess.copy(
                basicProcess.process.copy(
                        body = basicProcessBody.copy(subProcess = listOf(bpmnSubProcess)),
                        children = mapOf(
                                subprocessBpmnId to basicProcessBody.copy(serviceTask = listOf(bpmnServiceTaskStart, bpmnServiceTaskEnd))
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

        addSequenceElementOnFirstTaskToSecondTask()
    }

    protected fun prepareOneSubProcessThenNestedSubProcessWithOneServiceTaskView() {
        val process = basicProcess.copy(
                basicProcess.process.copy(
                        body = basicProcessBody.copy(serviceTask = listOf(bpmnServiceTaskStart, bpmnServiceTaskEnd), subProcess = listOf(bpmnSubProcess)),
                        children = mapOf(
                                subprocessBpmnId to basicProcessBody.copy(subProcess = listOf(bpmnNestedSubProcess)),
                                subprocessInSubProcessBpmnId to basicProcessBody.copy(serviceTask = listOf(bpmnServiceTaskStart))
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

    protected fun prepareOneSubProcessThenNestedSubProcessWithOneServiceTaskViewWithBoundaryErrorOnRoot() {
        val boundaryEventOnRoot = BpmnBoundaryErrorEvent(optionalBoundaryErrorEventBpmnId, null, parentProcessBpmnId, null)
        val boundaryEventOnRootShape = ShapeElement(
            optionalBoundaryErrorEventDiagramId,
            optionalBoundaryErrorEventBpmnId,
            BoundsElement(startElemX + taskSize * 50.0f, startElemX + taskSize * 50.0f, boundaryEventSize, boundaryEventSize)
        )

        val process = basicProcess.copy(
            basicProcess.process.copy(
                body = basicProcessBody.copy(serviceTask = listOf(bpmnServiceTaskStart, bpmnServiceTaskEnd), subProcess = listOf(bpmnSubProcess), boundaryErrorEvent = listOf(boundaryEventOnRoot)),
                children = mapOf(
                    subprocessBpmnId to basicProcessBody.copy(subProcess = listOf(bpmnNestedSubProcess)),
                    subprocessInSubProcessBpmnId to basicProcessBody.copy(serviceTask = listOf(bpmnServiceTaskStart))
                )
            ),
            listOf(
                DiagramElement(
                    diagramMainElementId,
                    PlaneElement(
                        diagramMainPlaneElementId,
                        basicProcess.process.id,
                        listOf(diagramSubProcess, diagramNestedSubProcess, diagramServiceTaskStart, boundaryEventOnRootShape),
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
                        body = basicProcessBody.copy(subProcess = listOf(bpmnSubProcess)),
                        children = mapOf(
                                subprocessBpmnId to basicProcessBody.copy(subProcess = listOf(bpmnNestedSubProcess)),
                                subprocessInSubProcessBpmnId to basicProcessBody.copy()
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

    protected fun prepareSendEventTask(){
        val process = basicProcess.copy(
            basicProcess.process.copy(
                body = basicProcessBody.copy(sendEventTask = listOf(bpmnSendEventTask))
            ),
            listOf(DiagramElement(
                diagramMainElementId,
                PlaneElement(diagramMainPlaneElementId, basicProcess.process.id, listOf(diagramSendEventTask), listOf()))
            )
        )
        whenever(parser.parse("")).thenReturn(process)
        initializeCanvas()
    }

    protected fun fillGroupsSendEventTask(){
        val extensionElementsMappingPayloadToEvent: List<ExtensionEventPayload> = listOf(
            ExtensionEventPayload("source", "target", "string")
        )
        val extensionElementsMappingPayloadFromEvent: List<ExtensionEventPayload> = listOf(
            ExtensionEventPayload("source", "target", "string")
        )
        val executionListener: List<ExeсutionListener> = listOf(
            ExeсutionListener("class", "start", listOf(
                ListenerField("listener filed name", "listener field string")
        )))
        bpmnSendEventTask = bpmnSendEventTask.copy(
            extensionElementsMappingPayloadToEvent = extensionElementsMappingPayloadToEvent,
            extensionElementsMappingPayloadFromEvent = extensionElementsMappingPayloadFromEvent,
            executionListener = executionListener)
    }
    protected fun prepareUserTaskView() {
        prepareUserTask(bpmnUserTask)
    }

    protected fun prepareUserTask(task: BpmnUserTask) {
        val process = basicProcess.copy(
            basicProcess.process.copy(
                body = basicProcessBody.copy(userTask = listOf(task))
            ),
            listOf(DiagramElement(
                diagramMainElementId,
                PlaneElement(diagramMainPlaneElementId, basicProcess.process.id, listOf(diagramUserTask), listOf()))
            )
        )
        whenever(parser.parse("")).thenReturn(process)
        initializeCanvas()
    }

    protected fun prepareTwoServiceTaskView() {
        prepareTwoServiceTaskView(bpmnServiceTaskStart, bpmnServiceTaskEnd)
    }

    protected fun prepareTwoServiceTaskView(one: BpmnServiceTask, two: BpmnServiceTask) {
        val process = basicProcess.copy(
            basicProcess.process.copy(
                body = basicProcessBody.copy(serviceTask = listOf(one, two))
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
                BoundsElement(startElemX + taskSize / 5.0f, startElemX + taskSize / 5.0f, boundaryEventSize, boundaryEventSize)
        )

        val process = basicProcess.copy(
                basicProcess.process.copy(
                    body = basicProcessBody.copy(serviceTask = listOf(bpmnServiceTaskStart), boundaryErrorEvent = listOf(boundaryEventOnServiceTask))
                ),
                listOf(DiagramElement(
                        diagramMainElementId,
                        PlaneElement(diagramMainPlaneElementId, basicProcess.process.id, listOf(diagramServiceTaskStart, boundaryEventOnServiceTaskShape), listOf()))
                )
        )
        whenever(parser.parse("")).thenReturn(process)
        initializeCanvas()
    }

    protected fun prepareServiceTaskWithBoundaryEventOnRootView() {
        val boundaryEventOnRoot = BpmnBoundaryErrorEvent(optionalBoundaryErrorEventBpmnId, null, parentProcessBpmnId, null)
        val boundaryEventOnRootShape = ShapeElement(
                optionalBoundaryErrorEventDiagramId,
                optionalBoundaryErrorEventBpmnId,
                BoundsElement(startElemX + 3.0f * taskSize, startElemX + 3.0f * taskSize, boundaryEventSize, boundaryEventSize)
        )

        val process = basicProcess.copy(
                basicProcess.process.copy(
                        body = basicProcessBody.copy(serviceTask = listOf(bpmnServiceTaskStart), boundaryErrorEvent = listOf(boundaryEventOnRoot))
                ),
                listOf(DiagramElement(
                        diagramMainElementId,
                        PlaneElement(diagramMainPlaneElementId, basicProcess.process.id, listOf(diagramServiceTaskStart, boundaryEventOnRootShape), listOf()))
                )
        )
        whenever(parser.parse("")).thenReturn(process)
        initializeCanvas()
    }

    protected fun prepareServiceTaskInSubprocessWithBoundaryEventOnRootView() {
        val boundaryEventOnRoot = BpmnBoundaryErrorEvent(optionalBoundaryErrorEventBpmnId, null, parentProcessBpmnId, null)
        val boundaryEventOnRootShape = ShapeElement(
                optionalBoundaryErrorEventDiagramId,
                optionalBoundaryErrorEventBpmnId,
                BoundsElement(startElemX + 3.0f * taskSize, startElemX + 3.0f * taskSize, boundaryEventSize, boundaryEventSize)
        )

        val process = basicProcess.copy(
                basicProcess.process.copy(
                        body = basicProcessBody.copy(subProcess = listOf(bpmnSubProcess), boundaryErrorEvent = listOf(boundaryEventOnRoot)),
                        children = mapOf(
                                subprocessBpmnId to basicProcessBody.copy(serviceTask = listOf(bpmnServiceTaskStart))
                        )
                ),
                listOf(DiagramElement(
                        diagramMainElementId,
                        PlaneElement(diagramMainPlaneElementId, basicProcess.process.id, listOf(diagramSubProcess, diagramServiceTaskStart, boundaryEventOnRootShape), listOf()))
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
                BoundsElement(startElemX + taskSize / 5.0f, startElemX + taskSize / 5.0f, boundaryEventSize, boundaryEventSize)
        )

        val process = basicProcess.copy(
                basicProcess.process.copy(
                        body = basicProcessBody.copy(subProcess = listOf(bpmnSubProcess)),
                        children = mapOf(
                                subprocessBpmnId to basicProcessBody.copy(serviceTask = listOf(bpmnServiceTaskStart), boundaryErrorEvent = listOf(boundaryEventOnServiceTask))
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

    protected fun prepareOneSubProcessAndOnRootServiceTaskWithAttachedBoundaryEventView() {
        val boundaryEventOnServiceTask = BpmnBoundaryErrorEvent(optionalBoundaryErrorEventBpmnId, null, serviceTaskStartBpmnId, null)
        val boundaryEventOnServiceTaskShape = ShapeElement(
            optionalBoundaryErrorEventDiagramId,
            optionalBoundaryErrorEventBpmnId,
            BoundsElement(startElemX + taskSize / 5.0f, startElemX + taskSize / 5.0f, boundaryEventSize, boundaryEventSize)
        )

        val process = basicProcess.copy(
            basicProcess.process.copy(
                body = basicProcessBody.copy(serviceTask = listOf(bpmnServiceTaskStart), boundaryErrorEvent = listOf(boundaryEventOnServiceTask), subProcess = listOf(bpmnSubProcess)),
                children = mapOf(subprocessBpmnId to basicProcessBody)
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
                BoundsElement(startElemX + taskSize / 5.0f, startElemX + taskSize / 5.0f, boundaryEventSize, boundaryEventSize)
        )

        val process = basicProcess.copy(
                basicProcess.process.copy(
                        body = basicProcessBody.copy(subProcess = listOf(bpmnSubProcess)),
                        children = mapOf(
                                subprocessBpmnId to basicProcessBody.copy(
                                    subProcess = listOf(bpmnNestedSubProcess),
                                    serviceTask = listOf(bpmnServiceTaskStart),
                                    boundaryErrorEvent = listOf(boundaryEventOnServiceTask)
                                ),
                                subprocessInSubProcessBpmnId to basicProcessBody.copy(
                                    serviceTask = listOf(bpmnServiceTaskEnd),
                                    sequenceFlow = listOf(bpmnSequenceFlow)
                                )
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

    protected fun elemAreaById(id: DiagramElementId) = renderResult?.areas?.get(id)!!

    protected fun findFirstNewLinkElem() = renderResult?.areas?.keys?.firstOrNull { it.id.contains(newLink) }
    protected fun findFirstDeleteElem() = renderResult?.areas?.keys?.firstOrNull { it.id.contains(doDel) }

    protected fun findExactlyOneNewLinkElem() = renderResult?.areas?.keys?.filter { it.id.contains(newLink) }?.shouldHaveSize(1)?.first()
    protected fun findExactlyOneDeleteElem() = renderResult?.areas?.keys?.filter { it.id.contains(doDel) }?.shouldHaveSize(1)?.first()

    protected fun findExactlyOneTypeChangeElem() = renderResult?.areas?.keys?.filter { it.id.contains(doChangeType) }?.shouldHaveSize(1)?.first()

    protected fun String.asResource(): SvgIcon {
        val txt = BaseUiTest::class.java.classLoader.getResource(this)?.readText(StandardCharsets.UTF_8)!!
        return SvgIcon(txt, Hashing.goodFastHash(64).hashString(txt, StandardCharsets.UTF_8).asLong())
    }
}
