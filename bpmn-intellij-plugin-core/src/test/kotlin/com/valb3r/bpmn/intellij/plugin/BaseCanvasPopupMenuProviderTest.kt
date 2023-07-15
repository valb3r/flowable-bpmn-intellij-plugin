package com.valb3r.bpmn.intellij.plugin

import ShapeCreator
import ShapeTypeChange
import com.intellij.openapi.project.Project
import com.nhaarman.mockitokotlin2.mock
import com.valb3r.bpmn.intellij.plugin.bpmn.api.bpmn.BpmnElementId
import com.valb3r.bpmn.intellij.plugin.core.popupmenu.BaseCanvasPopupMenuProvider
import com.valb3r.bpmn.intellij.plugin.core.popupmenu.MenuItemDef
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.Test
import java.awt.geom.Point2D
import javax.swing.JMenu
import javax.swing.JPopupMenu
import kotlin.reflect.KClass
import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.javaField

internal class BaseCanvasPopupMenuProviderTest {

    @Test
    fun validateProperMappingOfMenuItems() {
        val target = BaseCanvasPopupMenuProviderTestable(mock())

        val props = BaseCanvasPopupMenuProviderTestable::class.java.kotlin.memberProperties

        props.filter { it.javaField?.type == MenuItemDef::class.java }.forEach { prop ->
            val field = BaseCanvasPopupMenuProvider::class.java.getDeclaredField(prop.name)
            field.isAccessible = true
            val value = field.get(target)
            val newElement = (value as MenuItemDef).newElementListener.invoke(mock(), mock(), mock())
            val mutatedElement = value.elementMutationListener.invoke(mock(), mock())

            val shapeCreator = ShapeCreator::class.java.getDeclaredField("clazz")
            shapeCreator.isAccessible = true
            val shapeMutator = ShapeTypeChange::class.java.getDeclaredField("clazz")
            shapeMutator.isAccessible = true

            val newShapeClass = shapeCreator.get(newElement) as KClass<*>
            val mutatedShapeClass = shapeMutator.get(mutatedElement) as KClass<*>

            newShapeClass.simpleName!!.shouldBeEqualTo(mutatedShapeClass.simpleName!!)
            newShapeClass.simpleName!!.toLowerCase().replace("bpmn", "")
                .shouldBeEqualTo(prop.name.replace("_", "").toLowerCase())
        }
    }

    internal class BaseCanvasPopupMenuProviderTestable(project: Project) : BaseCanvasPopupMenuProvider(project) {
        override fun startEvents(sceneLocation: Point2D.Float, focus: BpmnElementId): JMenu {
            TODO("Not yet implemented")
        }

        override fun activities(sceneLocation: Point2D.Float, focus: BpmnElementId): JMenu {
            TODO("Not yet implemented")
        }

        override fun structural(sceneLocation: Point2D.Float, focus: BpmnElementId): JMenu {
            TODO("Not yet implemented")
        }

        override fun gateways(sceneLocation: Point2D.Float, focus: BpmnElementId): JMenu {
            TODO("Not yet implemented")
        }

        override fun boundaryEvents(sceneLocation: Point2D.Float, focus: BpmnElementId): JMenu {
            TODO("Not yet implemented")
        }

        override fun intermediateCatchingEvents(sceneLocation: Point2D.Float, focus: BpmnElementId): JMenu {
            TODO("Not yet implemented")
        }

        override fun intermediateThrowingEvents(sceneLocation: Point2D.Float, focus: BpmnElementId): JMenu {
            TODO("Not yet implemented")
        }

        override fun endEvents(sceneLocation: Point2D.Float, focus: BpmnElementId): JMenu {
            TODO("Not yet implemented")
        }

        override fun mutateStartEvent(popup: JPopupMenu, focus: BpmnElementId) {
            TODO("Not yet implemented")
        }

        override fun mutateBoundaryEvents(popup: JPopupMenu, focus: BpmnElementId) {
            TODO("Not yet implemented")
        }

        override fun mutateEndEvent(popup: JPopupMenu, focus: BpmnElementId) {
            TODO("Not yet implemented")
        }

        override fun mutateIntermediateThrowingEvent(popup: JPopupMenu, focus: BpmnElementId) {
            TODO("Not yet implemented")
        }

        override fun mutateIntermediateCatchingEvent(popup: JPopupMenu, focus: BpmnElementId) {
            TODO("Not yet implemented")
        }

        override fun mutateStructuralElement(popup: JPopupMenu, focus: BpmnElementId) {
            TODO("Not yet implemented")
        }

        override fun mutateGateway(popup: JPopupMenu, focus: BpmnElementId) {
            TODO("Not yet implemented")
        }

        override fun mutateTask(popup: JPopupMenu, focus: BpmnElementId) {
            TODO("Not yet implemented")
        }

    }
}