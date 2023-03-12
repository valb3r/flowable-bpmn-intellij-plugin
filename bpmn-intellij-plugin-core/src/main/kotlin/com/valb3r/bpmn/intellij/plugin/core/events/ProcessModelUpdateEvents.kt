package com.valb3r.bpmn.intellij.plugin.core.events

import com.google.common.hash.Hashing
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.valb3r.bpmn.intellij.plugin.bpmn.api.BpmnParser
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.BpmnElementId
import com.valb3r.bpmn.intellij.plugin.bpmn.api.diagram.DiagramElementId
import com.valb3r.bpmn.intellij.plugin.bpmn.api.events.*
import java.nio.charset.StandardCharsets
import java.util.*
import java.util.concurrent.CopyOnWriteArrayList


private val updateEvents = Collections.synchronizedMap(WeakHashMap<Project,  ProcessModelUpdateEvents>())

fun initializeUpdateEventsRegistry(project: Project, committer: FileCommitter) {
    updateEvents[project] = ProcessModelUpdateEvents(committer, ArrayList())
}

fun updateEventsRegistry(project: Project): ProcessModelUpdateEvents {
    return updateEvents[project]!!
}

interface FileCommitter {
    fun executeCommitAndGetHash(content: String?, events: List<EventPropagatableToXml>, hasher: (String) -> String, updateHash: (String) -> Unit)
}

class NoOpFileCommitter: FileCommitter {

    override fun executeCommitAndGetHash(
        content: String?,
        events: List<EventPropagatableToXml>,
        hasher: (String) -> String,
        updateHash: (String) -> Unit
    ) {
        // NOP
    }
}

class IntelliJFileCommitter(private val parser: BpmnParser, private val project: Project, private val file: VirtualFile): FileCommitter {

    override fun executeCommitAndGetHash(content: String?, events: List<EventPropagatableToXml>, hasher: (String) -> String, updateHash: (String) -> Unit) {
        var hash: String?
        val doc = FileDocumentManager.getInstance().getDocument(file)!!
        WriteCommandAction.runWriteCommandAction(project) {
            val newText = parser.update(
                    content ?: doc.text,
                    events
            )

            hash = hasher(newText)
            doc.replaceString(0, doc.textLength, newText)
            updateHash(hash!!)
        }
        FileDocumentManager.getInstance().saveDocument(doc)
    }

}

/**
 * Responsible for model update (persistent events).
 */
class ProcessModelUpdateEvents(private val committer: FileCommitter, private val updates: MutableList<Order<out Event>>) {

    var allBeforeThis: Int = 0
        private set

    private var baseFileContent: String? = null
    private var expectedFileHash: String = ""

    private val fileCommitListeners: MutableList<Any> = ArrayList()
    private val propertyUpdatesByStaticId: MutableMap<BpmnElementId, MutableList<Order<out Event>>> = HashMap()
    private val newShapeElements: MutableList<Order<BpmnShapeObjectAddedEvent>> = ArrayList()
    private val newDiagramElements: MutableList<Order<BpmnEdgeObjectAddedEvent>> = ArrayList()
    private val deletionsByStaticId: MutableMap<DiagramElementId, MutableList<Order<out Event>>> = HashMap()
    private val deletionsByStaticBpmnId: MutableMap<BpmnElementId, MutableList<Order<out Event>>> = HashMap()

    @Synchronized
    fun fileStateMatches(currentContent: String): Boolean {
        return expectedFileHash == hashData(currentContent)
    }

    @Synchronized
    fun reset(fileContent: String) {
        allBeforeThis = 0
        updates.clear()
        fileCommitListeners.clear()
        propertyUpdatesByStaticId.clear()
        newShapeElements.clear()
        newDiagramElements.clear()
        deletionsByStaticId.clear()
        deletionsByStaticBpmnId.clear()
        expectedFileHash = hashData(fileContent)
        baseFileContent = fileContent
    }

    @Synchronized
    fun undo() {
        allBeforeThis = if (allBeforeThis > 0) {
            updates[allBeforeThis - 1].block?.let { allBeforeThis - it.size } ?: allBeforeThis - 1
        } else {
            allBeforeThis
        }

        commitToFile()
    }

    @Synchronized
    fun redo() {
        allBeforeThis = if (allBeforeThis < updates.size) {
            updates[allBeforeThis].block?.let { allBeforeThis + it.size } ?: allBeforeThis + 1
        } else {
            allBeforeThis
        }

        commitToFile()
    }

    @Synchronized
    fun undoRedoStatus(): Set<UndoRedo> {
        val hasUndo = allBeforeThis > 0
        val hasRedo = allBeforeThis < updates.size
        val result = mutableSetOf<UndoRedo>()
        if (hasUndo) result.add(UndoRedo.UNDO)
        if (hasRedo) result.add(UndoRedo.REDO)

        return result
    }

    @Synchronized
    fun commitToFile() {
        committer.executeCommitAndGetHash(
                baseFileContent,
                updates.filterIndexed { index, _ -> index < allBeforeThis }.map { it.event }.filterIsInstance<EventPropagatableToXml>(),
                { hashData(it) },
                { expectedFileHash = it}
        )
    }

    @Synchronized
    fun getUpdateEventList(): List<Order<out Event>> {
        return updates.filterIndexed { index, _ ->  index < allBeforeThis }.toList()
    }

    @Synchronized
    fun addPropertyUpdateEvent(event: PropertyUpdateWithId) {
        disableRedo()
        val toStore = Order(allBeforeThis, event)
        allBeforeThis++
        updates.add(toStore)
        propertyUpdatesByStaticId.computeIfAbsent(event.bpmnElementId) { CopyOnWriteArrayList() } += toStore
        commitToFile()
    }

    @Synchronized
    fun addEvents(events: List<Event>) {
        disableRedo()
        val current = allBeforeThis
        allBeforeThis += events.size
        events.forEachIndexed {index, event ->
            val toStore = Order(current + index, event, EventBlock(events.size))
            updates.add(toStore)
            when (event) {
                is PropertyUpdateWithId -> propertyUpdatesByStaticId.computeIfAbsent(event.bpmnElementId) { CopyOnWriteArrayList() } += toStore
                is LocationUpdateWithId, is BpmnShapeResizedAndMoved, is NewWaypoints, is BpmnParentChanged, is EventUiOnly -> { /*NOP*/ }
                is BpmnShapeObjectAddedEvent -> addObjectShapeEvent(toStore as Order<BpmnShapeObjectAddedEvent>)
                is BpmnEdgeObjectAddedEvent -> addObjectEdgeEvent(toStore as Order<BpmnEdgeObjectAddedEvent>)
                is BpmnElementRemovedEvent -> removeBpmnElement(event.bpmnElementId , toStore as Order<BpmnElementRemovedEvent> )
                is BpmnElementTypeChangeEvent -> changeBpmnElement(event.elementId , toStore as Order<BpmnElementRemovedEvent>, toStore as Order<BpmnEdgeObjectAddedEvent>)
                else -> throw IllegalArgumentException("Can't bulk add: " + event::class.qualifiedName)
            }
        }

        commitToFile()
    }

    private fun changeBpmnElement(
        bpmnElementId: BpmnElementId,
        orderRemove: Order<BpmnElementRemovedEvent>,
        orderAdd: Order<BpmnEdgeObjectAddedEvent>
    ) {
        removeBpmnElement(bpmnElementId, orderRemove)
        addObjectEdgeEvent(orderAdd)
    }

    @Synchronized
    fun addElementRemovedEvent(diagram: List<DiagramElementRemovedEvent>, bpmn: List<BpmnElementRemovedEvent>, other: List<PropertyUpdateWithId> = emptyList()) {
        disableRedo()
        val current = allBeforeThis
        val blockSize = diagram.size + bpmn.size + other.size
        allBeforeThis += blockSize

        diagram.forEachIndexed {index, event ->
            val toStore = Order(current + index, event, EventBlock(blockSize))
            updates.add(toStore)
            deletionsByStaticId.computeIfAbsent(event.elementId) { CopyOnWriteArrayList() } += toStore
        }

        bpmn.forEachIndexed {index, event ->
            val toStore = Order(current + index, event, EventBlock(blockSize))
            updates.add(toStore)
            removeBpmnElement(event.bpmnElementId, toStore)
        }

        other.forEachIndexed { index, event ->
            val toStore = Order(current + index, event, EventBlock(blockSize))
            updates.add(toStore)
            propertyUpdatesByStaticId.computeIfAbsent(event.bpmnElementId) { CopyOnWriteArrayList() } += toStore
        }

        commitToFile()
    }

    private fun removeBpmnElement(
        bpmnElement: BpmnElementId,
        toStore: Order<BpmnElementRemovedEvent>
    ) {
        deletionsByStaticBpmnId.computeIfAbsent(bpmnElement) { CopyOnWriteArrayList() } += toStore
    }

    @Synchronized
    fun addObjectEvent(event: BpmnShapeObjectAddedEvent) {
        val toStore = advanceCursor(event)
        updates.add(toStore)
        newShapeElements.add(toStore)
        commitToFile()
    }

    private fun addObjectShapeEvent(toStore: Order<BpmnShapeObjectAddedEvent>) {
        newShapeElements.add(toStore)
    }

    private fun addObjectEdgeEvent(toStore: Order<BpmnEdgeObjectAddedEvent>) {
        newDiagramElements.add(toStore)
    }

    @Synchronized
    fun currentPropertyUpdateEventList(elementId: BpmnElementId): List<EventOrder<PropertyUpdateWithId>> {
        val cursorValue = allBeforeThis
        val latestRemoval = lastDeletion(elementId)
        return propertyUpdatesByStaticId
                .getOrDefault(elementId, emptyList<Order<PropertyUpdateWithId>>())
                .filterIsInstance<Order<PropertyUpdateWithId>>()
                .filter { it.order < cursorValue }
                .filter { it.order >  latestRemoval.order}
    }

    private fun <T: Event> advanceCursor(event: T): Order<T> {
        disableRedo()
        val toStore = Order(allBeforeThis, event)
        allBeforeThis++
        return toStore
    }

    private fun disableRedo() {
        val targetList = updates.subList(0, allBeforeThis).toList()
        updates.clear()
        updates.addAll(targetList)
    }

    private fun hashData(data: String): String {
        // Normalize line endings for hashing
        val normalizedData = data.replace("\r\n", "\n")
        return Hashing.goodFastHash(32).hashString(normalizedData, StandardCharsets.UTF_8).toString()
    }

    private fun lastDeletion(elementId: BpmnElementId): Order<out Event> {
        val cursorValue = allBeforeThis
        return deletionsByStaticBpmnId[elementId]?.filter { it.order < cursorValue }
            ?.maxBy { it: Order<out Event> -> it.order }
            ?: Order(-1, NullEvent(elementId.id))
    }

    data class Order<T: Event>(override val order: Int, override val event: T, override val block: EventBlock? = null): EventOrder<T>
    data class NullEvent(val forId: String): EventUiOnly

    enum class UndoRedo {
        UNDO,
        REDO
    }
}
