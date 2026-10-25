package com.valb3r.bpmn.intellij.plugin.core.tests

import com.valb3r.bpmn.intellij.plugin.bpmn.api.BpmnObjectFactory
import com.valb3r.bpmn.intellij.plugin.bpmn.api.BpmnParser
import com.valb3r.bpmn.intellij.plugin.bpmn.api.BpmnProcessObject
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.BpmnElementId
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.BpmnAssociation
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.WithBpmnId
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.WithParentId
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.subprocess.BpmnCollapsedSubprocess
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.subprocess.BpmnTransactionCollapsedSubprocess
import com.valb3r.bpmn.intellij.plugin.bpmn.api.diagram.DiagramElementId
import com.valb3r.bpmn.intellij.plugin.bpmn.api.diagram.elements.BoundsElement
import com.valb3r.bpmn.intellij.plugin.bpmn.api.diagram.elements.ShapeElement
import com.valb3r.bpmn.intellij.plugin.bpmn.api.info.PropertyType
import com.valb3r.bpmn.intellij.plugin.core.actions.copypaste.*
import com.valb3r.bpmn.intellij.plugin.core.events.StringValueUpdatedEvent
import com.valb3r.bpmn.intellij.plugin.core.events.BpmnDiagramAddedEvent
import com.valb3r.bpmn.intellij.plugin.core.events.BpmnShapeObjectAddedEvent
import com.valb3r.bpmn.intellij.plugin.core.events.initializeUpdateEventsRegistry
import com.valb3r.bpmn.intellij.plugin.core.events.updateEventsRegistry
import com.valb3r.bpmn.intellij.plugin.core.render.ElementInteractionContext
import com.valb3r.bpmn.intellij.plugin.core.render.RenderContext
import com.valb3r.bpmn.intellij.plugin.core.render.elements.BaseDiagramRenderElement
import com.valb3r.bpmn.intellij.plugin.core.render.elements.RenderState
import com.valb3r.bpmn.intellij.plugin.core.render.elements.edges.BaseEdgeRenderElement
import com.valb3r.bpmn.intellij.plugin.core.render.elements.shapes.ShapeRenderElement
import com.valb3r.bpmn.intellij.plugin.core.state.currentStateProvider
import com.valb3r.bpmn.intellij.plugin.core.newelements.registerNewElementsFactory
import org.amshove.kluent.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.*
import java.awt.geom.Point2D
import java.util.concurrent.atomic.AtomicReference

abstract class BaseCopyPastePopurriTest : BaseUiTest() {

    protected abstract val bpmnParser: BpmnParser
    protected abstract val bpmnFactory: BpmnObjectFactory
    protected abstract val resourceName: String
    protected abstract val defaultFlowOwnerId: String
    protected abstract val defaultFlowId: String
    protected abstract val messageEventIds: List<String>
    protected abstract val messageReference: String

    @BeforeEach
    fun initializeCopyPaste() {
        registerNewElementsFactory(project, bpmnFactory)
        val buffer = AtomicReference<String>()
        val clipboard = mock<SystemClipboard>()
        doAnswer { buffer.get() }.whenever(clipboard).getData(any())
        doAnswer { true }.whenever(clipboard).isDataFlavorAvailable(any())
        doAnswer {
            buffer.set(it.getArgument(0, CopyPasteActionHandler.ClipboardFlavor::class.java).getTransferData(DATA_FLAVOR) as String)
        }.whenever(clipboard).setContents(any(), anyOrNull())
        setCopyPasteActionHandler(project, CopyPasteActionHandler(clipboard))
    }

    protected open fun fixtureXml(): String = checkNotNull(javaClass.classLoader.getResource(resourceName)).readText()

    private fun popurriXml(): String = bpmnParser.update(fixtureXml(), listOf(
        StringValueUpdatedEvent(BpmnElementId(defaultFlowOwnerId), PropertyType.DEFAULT_FLOW, defaultFlowId),
        StringValueUpdatedEvent(BpmnElementId(defaultFlowId), PropertyType.EXECUTION_LISTENER_CLASS, "CopyPasteListener", propertyIndex = listOf("CopyPasteListener")),
        StringValueUpdatedEvent(BpmnElementId(defaultFlowId), PropertyType.EXECUTION_LISTENER_EVENT, "take", propertyIndex = listOf("CopyPasteListener"))
    ))

    @Test
    fun `Popurri can be copied and pasted with default flows and message events`() {
        val xml = popurriXml()
        val original = bpmnParser.parse(xml)
        val view = original.toView(bpmnFactory)
        val pasted = copyAndPaste(original)
        val originalShapes = original.diagram.flatMap { it.bpmnPlane.bpmnShape.orEmpty() }
        val shapes = originalShapes.filter { view.elementByStaticId.getValue(it.bpmnElement).element !is BpmnAssociation }
        val edges = original.diagram.flatMap { it.bpmnPlane.bpmnEdge.orEmpty() }

        pasted.shapes.shouldHaveSize(shapes.size)
        pasted.edges.shouldHaveSize(edges.size)
        val pastedObjects = pasted.shapes.map { it.bpmnObject to it.props } + pasted.edges.map { it.bpmnObject to it.props }
        val replacements = (shapes.map { it.bpmnElement } + edges.map { it.bpmnElement!! }).associateWith { oldId ->
            val source = view.elementByStaticId.getValue(oldId).element
            pastedObjects.single { (element, _) -> element.element == source.updateBpmnElemId(element.id) }.first.id
        }
        replacements.values.toSet().shouldHaveSize(replacements.size)
        replacements.values.forEach { view.elementByStaticId.keys.shouldNotContain(it) }

        val oldIds = replacements.entries.associate { (old, new) -> new to old }
        pastedObjects.forEach { (element, props) ->
            val oldId = oldIds.getValue(element.id)
            val source = view.elementByStaticId.getValue(oldId)
            element.element.shouldBeEqualTo(source.element.updateBpmnElemId(element.id))
            element.parent.shouldBeEqualTo(replacements[source.parent] ?: original.process.id)
            element.parentIdForXml.shouldBeEqualTo(replacements[source.parentIdForXml] ?: original.process.id)
            view.elemPropertiesByElementId.getValue(oldId).forEach { type, prop ->
                val expected = when {
                    type == PropertyType.ID -> prop.copy(value = element.id.id)
                    type.updatedBy == PropertyType.ID && prop.value != null -> {
                        val value = replacements[BpmnElementId(prop.value as String)]?.id ?: ""
                        prop.copy(value = value, index = if (type.indexInGroupArrayName == "@") prop.index?.map { value } else prop.index)
                    }
                    else -> prop
                }
                props.getAll(type).shouldContain(expected)
            }
        }

        val updated = bpmnParser.parse(bpmnParser.update(xml, pasted.events())).toView(bpmnFactory)
        val updatedShapes = updated.diagram.flatMap { it.bpmnPlane.bpmnShape.orEmpty() }
        updatedShapes.shouldHaveSize(originalShapes.size + shapes.size)
        updatedShapes.shouldContainAll(originalShapes)
        updated.diagram.flatMap { it.bpmnPlane.bpmnEdge.orEmpty() }.shouldHaveSize(edges.size * 2)
        pastedObjects.forEach { (element, props) ->
            updated.elementByStaticId.getValue(element.id).element::class.shouldBeEqualTo(element.element::class)
            listOf(PropertyType.BPMN_INCOMING, PropertyType.BPMN_OUTGOING).filter { it in props.keys }.forEach { type ->
                val expected = props.getAll(type).filter { !((it.value as String?).isNullOrEmpty()) }
                val actual = updated.elemPropertiesByElementId.getValue(element.id).getAll(type)
                    .filter { !((it.value as String?).isNullOrEmpty()) }
                actual.shouldBeEqualTo(expected)
            }
        }
        val gateway = replacements.getValue(BpmnElementId(defaultFlowOwnerId))
        updated.elemPropertiesByElementId.getValue(gateway)[PropertyType.DEFAULT_FLOW]!!.value
            .shouldBeEqualTo(replacements.getValue(BpmnElementId(defaultFlowId)).id)
        pasted.edges.forEach { edge ->
            val sourceId = oldIds.getValue(edge.bpmnObject.id)
            val originalProps = view.elemPropertiesByElementId.getValue(sourceId)
            val updatedProps = updated.elemPropertiesByElementId.getValue(edge.bpmnObject.id)
            listOf(PropertyType.SOURCE_REF, PropertyType.TARGET_REF).forEach { type ->
                val originalReference = originalProps[type].shouldNotBeNull().value as String
                val expectedReference = replacements.getValue(BpmnElementId(originalReference)).id
                updatedProps[type].shouldNotBeNull().value.shouldBeEqualTo(expectedReference)
            }
        }
        val originalFlowProps = view.elemPropertiesByElementId.getValue(BpmnElementId(defaultFlowId))
        val pastedFlowProps = updated.elemPropertiesByElementId.getValue(replacements.getValue(BpmnElementId(defaultFlowId)))
        listOf(PropertyType.EXECUTION_LISTENER_CLASS, PropertyType.EXECUTION_LISTENER_EVENT).forEach { type ->
            val expected = originalFlowProps.getAll(type)
            expected.shouldHaveSingleItem()
            pastedFlowProps.getAll(type).shouldBeEqualTo(expected)
        }
        messageEventIds.forEach { id ->
            val oldId = BpmnElementId(id)
            val newId = replacements.getValue(oldId)
            updated.elemPropertiesByElementId.getValue(newId)[PropertyType.MESSAGE_REF]
                .shouldBeEqualTo(view.elemPropertiesByElementId.getValue(oldId)[PropertyType.MESSAGE_REF])
        }
    }

    @Test
    fun `Collapsed subprocess paste emits dedicated diagram events in the same undo block`() {
        val xml = fixtureXml()
        val original = bpmnParser.parse(xml)
        val elements = listOf<WithBpmnId>(
            BpmnCollapsedSubprocess(BpmnElementId("copyPasteCollapsed"), transactionalSubprocess = false),
            BpmnTransactionCollapsedSubprocess(BpmnElementId("copyPasteCollapsedTransaction"), transactionalSubprocess = true),
        )
        val shapes = elements.map { element ->
            BpmnShapeObjectAddedEvent(
                WithParentId(original.process.id, element),
                ShapeElement(DiagramElementId("shape_${element.id.id}"), element.id, BoundsElement(100f, 100f, 100f, 80f)),
                bpmnFactory.propertiesOf(element),
            )
        }.toMutableList()
        val seed = ClipboardContext(shapes, mutableListOf(), shapes.map { it.shape.id })

        // Shape messages must not implicitly create a diagram.
        bpmnParser.parse(bpmnParser.update(xml, shapes)).diagram.map { it.id to it.bpmnPlane.bpmnElement }
            .shouldBeEqualTo(original.diagram.map { it.id to it.bpmnPlane.bpmnElement })

        val sourceXml = bpmnParser.update(xml, seed.events())
        val source = bpmnParser.parse(sourceXml)
        val pasted = copyAndPaste(source, shapes.map { it.shape.id }.toSet())
        val events = pasted.events()
        val diagrams = events.filterIsInstance<BpmnDiagramAddedEvent>()
        diagrams.shouldHaveSize(2)
        diagrams.map { it.bpmnElementId }.toSet().shouldBeEqualTo(pasted.shapes.map { it.bpmnObject.id }.toSet())
        diagrams.map { it.diagramId }.toSet().intersect(source.diagram.map { it.id }.toSet()).shouldBeEmpty()
        diagrams.map { it.planeId }.toSet().intersect(source.diagram.map { it.bpmnPlane.id }.toSet()).shouldBeEmpty()

        val updated = bpmnParser.parse(bpmnParser.update(sourceXml, events))
        updated.diagram.shouldHaveSize(source.diagram.size + 2)
        val view = updated.toView(bpmnFactory)
        pasted.shapes.forEach { shape ->
            view.elementByStaticId.getValue(shape.bpmnObject.id).element::class.shouldBeEqualTo(shape.bpmnObject.element::class)
            val diagram = updated.diagram.single { it.bpmnPlane.bpmnElement == shape.bpmnObject.id }
            diagram.id.shouldBeEqualTo(diagrams.single { it.bpmnElementId == shape.bpmnObject.id }.diagramId)
        }

        val registry = updateEventsRegistry(project)
        registry.addEvents(events)
        currentStateProvider(project).currentState().elementByBpmnId.keys
            .shouldContainAll(pasted.shapes.map { it.bpmnObject.id })
        registry.undo()
        registry.getUpdateEventList().shouldBeEmpty()
        currentStateProvider(project).currentState().elementByBpmnId.keys
            .intersect(pasted.shapes.map { it.bpmnObject.id }.toSet()).shouldBeEmpty()
        registry.redo()
        registry.getUpdateEventList().map { it.event }.shouldBeEqualTo(events)
        currentStateProvider(project).currentState().elementByBpmnId.keys
            .shouldContainAll(pasted.shapes.map { it.bpmnObject.id })
    }

    @Test
    fun `Message events can be copied individually with empty and populated references`() {
        val xml = popurriXml()
        messageEventIds.forEach { id ->
            listOf("", messageReference).forEach { reference ->
                val sourceXml = bpmnParser.update(xml, listOf(StringValueUpdatedEvent(BpmnElementId(id), PropertyType.MESSAGE_REF, reference)))
                val source = bpmnParser.parse(sourceXml)
                val sourceView = source.toView(bpmnFactory)
                val sourceShape = source.diagram.flatMap { it.bpmnPlane.bpmnShape.orEmpty() }.single { it.bpmnElement.id == id }
                val pasted = copyAndPaste(source, setOf(sourceShape.id))
                val shape = pasted.shapes.shouldHaveSingleItem()
                pasted.edges.shouldBeEmpty()
                shape.bpmnObject.id.shouldNotBeEqualTo(BpmnElementId(id))
                val updated = bpmnParser.parse(bpmnParser.update(sourceXml, pasted.events())).toView(bpmnFactory)
                updated.elementByStaticId.getValue(shape.bpmnObject.id).element::class
                    .shouldBeEqualTo(sourceView.elementByStaticId.getValue(BpmnElementId(id)).element::class)
                updated.elemPropertiesByElementId.getValue(shape.bpmnObject.id)[PropertyType.MESSAGE_REF]
                    .shouldBeEqualTo(sourceView.elemPropertiesByElementId.getValue(BpmnElementId(id))[PropertyType.MESSAGE_REF])
            }
        }
    }

    @Test
    fun `Copying a gateway without its default flow clears the reference`() {
        val xml = popurriXml()
        val source = bpmnParser.parse(xml)
        val gateway = source.diagram.flatMap { it.bpmnPlane.bpmnShape.orEmpty() }
            .single { it.bpmnElement.id == defaultFlowOwnerId }

        val pasted = copyAndPaste(source, setOf(gateway.id))

        pasted.edges.shouldBeEmpty()
        pasted.shapes.shouldHaveSingleItem().props[PropertyType.DEFAULT_FLOW]!!.value.shouldBeEqualTo("")
    }

    private fun copyAndPaste(process: BpmnProcessObject, selectedIds: Set<DiagramElementId>? = null): ClipboardContext {
        initializeUpdateEventsRegistry(project, fileCommitter)
        currentStateProvider(project).resetStateTo("", process.toView(bpmnFactory))
        val current = currentStateProvider(project).currentState()
        val elements = mutableMapOf<BpmnElementId, BaseDiagramRenderElement>()
        // Match DefaultBpmnProcessRenderer: associations are rendered and copied as edges.
        current.shapes.filter { current.elementByBpmnId[it.bpmnElement]?.element !is BpmnAssociation }.forEach { shape ->
            elements[shape.bpmnElement] = mock<ShapeRenderElement> {
                whenever(it.elementId).thenReturn(shape.id)
                whenever(it.shapeElem).thenReturn(shape)
                whenever(it.children).thenReturn(mutableListOf())
            }
        }
        current.edges.forEach { edge ->
            elements[edge.bpmnElement!!] = mock<BaseEdgeRenderElement> {
                whenever(it.elementId).thenReturn(edge.id)
                whenever(it.edgeElem).thenReturn(edge)
            }
        }
        current.elementByBpmnId.forEach { (id, element) ->
            elements[element.parent]?.children?.add(elements[id] ?: return@forEach)
        }
        val selected = selectedIds ?: elements.filterKeys { current.elementByBpmnId.getValue(it).parent == process.process.id }
            .values.map { it.elementId }.toSet()
        val interaction = mock<ElementInteractionContext> {
            whenever(it.draggedIds).thenReturn(emptySet())
        }
        val context = mock<RenderContext> {
            whenever(it.selectedIds).thenReturn(selected)
            whenever(it.interactionContext).thenReturn(interaction)
        }
        val state = RenderState(elements.values.associateBy { it.elementId }, mutableMapOf(), current, emptyList(), context, icons)
        val handler = copyPasteActionHandler(project)
        handler.copy(state, elements)
        return handler.paste(Point2D.Float(-1000.0f, -1000.0f), process.process.id).shouldNotBeNull()
    }
}

