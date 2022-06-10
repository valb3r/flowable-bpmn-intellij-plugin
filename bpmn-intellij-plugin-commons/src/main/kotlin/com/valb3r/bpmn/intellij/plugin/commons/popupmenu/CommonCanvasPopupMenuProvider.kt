
import com.intellij.openapi.project.Project
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.BpmnElementId
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.WithBpmnId
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.elements.WithParentId
import com.valb3r.bpmn.intellij.plugin.bpmn.api.diagram.elements.BoundsElement
import com.valb3r.bpmn.intellij.plugin.bpmn.api.diagram.elements.ShapeElement
import com.valb3r.bpmn.intellij.plugin.bpmn.api.events.PropertyUpdateWithId
import com.valb3r.bpmn.intellij.plugin.bpmn.api.info.PropertyType
import com.valb3r.bpmn.intellij.plugin.bpmn.api.utils.NameElementWithTypeForXml
import com.valb3r.bpmn.intellij.plugin.core.events.*
import com.valb3r.bpmn.intellij.plugin.core.newelements.newElementsFactory
import com.valb3r.bpmn.intellij.plugin.core.render.snapToGridIfNecessary
import com.valb3r.bpmn.intellij.plugin.core.state.currentStateProvider
import java.awt.event.ActionEvent
import java.awt.event.ActionListener
import java.awt.geom.Point2D
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

class ShapeChange<T : WithBpmnId>(
    private val project: Project,
    private val clazz: KClass<T>,
    private val elementId: BpmnElementId) : ActionListener {
    constructor(project: Project,
                clazz: KClass<T>,
                elementId: BpmnElementId,
                nameElemWithTypeForXml: NameElementWithTypeForXml<out WithBpmnId>
    ) : this(project, clazz, elementId){
        this.nameElemWithTypeForXml = nameElemWithTypeForXml
    }

    private var nameElemWithTypeForXml : NameElementWithTypeForXml<out WithBpmnId>? = null
    override fun actionPerformed(e: ActionEvent?) {
        val elementsFactory = newElementsFactory(project)
        val newBpmnElement = elementsFactory.newBpmnObject(clazz).updateBpmnElemId(elementId)
        val currentElement = currentStateProvider(project).currentState().elementByBpmnId[elementId]!!.element
        if(newBpmnElement.javaClass == currentElement.javaClass) return
        val oldPropertyTable = currentStateProvider(project).currentState().elemPropertiesByStaticElementId[elementId]
        val newPropertyTable = elementsFactory.propertiesOf(newBpmnElement)
        val propertiesToRemove = mutableListOf<RemovePropertyEvent>()
        val nestedPropertiesRemove = mutableListOf<PropertyUpdateWithId>()
        oldPropertyTable!!.view().forEach { (t, u) ->
            if (null == newPropertyTable[t]) {
                if (t == PropertyType.FORM_PROPERTY_ID || t == PropertyType.FORM_PROPERTY_NAME){                                        //FIXME doesn't remove nested elements
                    nestedPropertiesRemove += StringValueUpdatedEvent(elementId, t, "", propertyIndex = listOf("Property 1"))
                }else {
                    propertiesToRemove += RemovePropertyEvent(elementId, t)
                }
            } else {
                newPropertyTable[t] = u.toMutableList()
            }
        }

        val prepareChangeEvent: BpmnElementChangeEvent = if (null == nameElemWithTypeForXml){
            BpmnElementChangeEvent(elementId, newBpmnElement)
        } else {
            BpmnElementChangeEvent(elementId, newBpmnElement, Pair(
                elementsFactory.newBpmnObject(nameElemWithTypeForXml!!.clazz),
                nameElemWithTypeForXml!!.type))
        }

        updateEventsRegistry(project).addEvents(
            propertiesToRemove +
                    nestedPropertiesRemove +
                    listOf(
                        UpdatePropertyTableEvent(elementId, newPropertyTable),
                        prepareChangeEvent
                    )
        )
    }
}
