
import com.intellij.openapi.project.Project
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.BpmnElementId
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.WithBpmnId
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.WithParentId
import com.valb3r.bpmn.intellij.plugin.bpmn.api.diagram.DiagramElementId
import com.valb3r.bpmn.intellij.plugin.bpmn.api.diagram.elements.BoundsElement
import com.valb3r.bpmn.intellij.plugin.bpmn.api.diagram.elements.ShapeElement
import com.valb3r.bpmn.intellij.plugin.core.actions.currentRemoveActionHandler
import com.valb3r.bpmn.intellij.plugin.core.events.BpmnElementRemovedEvent
import com.valb3r.bpmn.intellij.plugin.core.events.BpmnShapeObjectAddedEvent
import com.valb3r.bpmn.intellij.plugin.core.events.DiagramElementRemovedEvent
import com.valb3r.bpmn.intellij.plugin.core.events.updateEventsRegistry
import com.valb3r.bpmn.intellij.plugin.core.newelements.newElementsFactory
import com.valb3r.bpmn.intellij.plugin.core.render.snapToGridIfNecessary
import com.valb3r.bpmn.intellij.plugin.core.state.currentStateProvider
import java.awt.event.ActionEvent
import java.awt.event.ActionListener
import java.awt.geom.Point2D
import java.awt.geom.Rectangle2D
import kotlin.reflect.KClass

private fun <T: WithBpmnId> newShapeElement(project: Project, sceneLocation: Point2D.Float, forObject: T): ShapeElement {
    val templateShape = newElementsFactory(project).newDiagramObject(ShapeElement::class, forObject)

    val bounds = templateShape.rectBounds()
    val snappedLocation = snapToGridIfNecessary(sceneLocation.x, sceneLocation.y)
    return templateShape.copy(
            bounds = BoundsElement(
                    snappedLocation.x,
                    snappedLocation.y,
                    bounds.width,
                    bounds.height
            )
    )
}

private fun <T: WithBpmnId> newShapeElementWithBounds(project: Project, bounds: BoundsElement, forObject: T): ShapeElement {
    val templateShape = newElementsFactory(project).newDiagramObject(ShapeElement::class, forObject)

    return templateShape.copy(
        bounds = bounds
        )
}

class ShapeCreator<T : WithBpmnId> (private val project: Project, private val clazz: KClass<T>, private val sceneLocation: Point2D.Float, private val parent: BpmnElementId): ActionListener {

    override fun actionPerformed(e: ActionEvent?) {
        val newObject = newElementsFactory(project).newBpmnObject(clazz)
        val shape = newShapeElement(project, sceneLocation, newObject)

        updateEventsRegistry(project).addObjectEvent(
                BpmnShapeObjectAddedEvent(WithParentId(parent, newObject), shape, newElementsFactory(project).propertiesOf(newObject))
        )
    }
}

class ShapeChange<T : WithBpmnId> (private val project: Project, private val clazz: KClass<T>, val fromElementId: BpmnElementId, val rect: Rectangle2D.Float, private val parent: BpmnElementId): ActionListener {

    override fun actionPerformed(e: ActionEvent?) {
        var propertyTable = currentStateProvider(project).currentState().elemPropertiesByStaticElementId[fromElementId]
//        currentRemoveActionHandler(project).deleteElem()
        val newObject = newElementsFactory(project).newBpmnObject(clazz)
        val shape = newShapeElementWithBounds(project, BoundsElement(rect.x, rect.y, rect.width, rect.height), newObject)
        val idNewObject = newObject.id
        var newObjectPropTable = currentStateProvider(project).currentState().elemPropertiesByStaticElementId[idNewObject]

        updateEventsRegistry(project).addEvents(listOf(
            BpmnElementRemovedEvent(fromElementId),
            BpmnShapeObjectAddedEvent(WithParentId(parent, newObject), shape, newElementsFactory(project).propertiesOf(newObject))
        ))
    }
}