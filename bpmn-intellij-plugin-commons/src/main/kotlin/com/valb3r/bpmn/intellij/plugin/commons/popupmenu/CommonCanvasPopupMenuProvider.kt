
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.BpmnElementId
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.WithBpmnId
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.WithParentId
import com.valb3r.bpmn.intellij.plugin.bpmn.api.diagram.elements.BoundsElement
import com.valb3r.bpmn.intellij.plugin.bpmn.api.diagram.elements.ShapeElement
import com.valb3r.bpmn.intellij.plugin.core.events.BpmnShapeObjectAddedEvent
import com.valb3r.bpmn.intellij.plugin.core.events.updateEventsRegistry
import com.valb3r.bpmn.intellij.plugin.core.newelements.newElementsFactory
import java.awt.event.ActionEvent
import java.awt.event.ActionListener
import java.awt.geom.Point2D
import kotlin.reflect.KClass

private fun <T: WithBpmnId> newShapeElement(sceneLocation: Point2D.Float, forObject: T): ShapeElement {
    val templateShape = newElementsFactory().newDiagramObject(ShapeElement::class, forObject)

    val bounds = templateShape.rectBounds()
    return templateShape.copy(
            bounds = BoundsElement(
                    sceneLocation.x,
                    sceneLocation.y,
                    bounds.width,
                    bounds.height
            )
    )
}

class ShapeCreator<T : WithBpmnId> (private val clazz: KClass<T>, private val sceneLocation: Point2D.Float, private val parent: BpmnElementId): ActionListener {

    override fun actionPerformed(e: ActionEvent?) {
        val newObject = newElementsFactory().newBpmnObject(clazz)
        val shape = newShapeElement(sceneLocation, newObject)

        updateEventsRegistry().addObjectEvent(
                BpmnShapeObjectAddedEvent(WithParentId(parent, newObject), shape, newElementsFactory().propertiesOf(newObject))
        )
    }
}