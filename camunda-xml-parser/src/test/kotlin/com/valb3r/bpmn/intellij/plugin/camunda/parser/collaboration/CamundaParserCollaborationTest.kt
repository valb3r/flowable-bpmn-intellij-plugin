package com.valb3r.bpmn.intellij.plugin.camunda.parser.collaboration

import com.valb3r.bpmn.intellij.plugin.bpmn.api.BpmnFileObject
import com.valb3r.bpmn.intellij.plugin.camunda.parser.CamundaObjectFactory
import com.valb3r.bpmn.intellij.plugin.camunda.parser.CamundaParser
import com.valb3r.bpmn.intellij.plugin.camunda.parser.asResource
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeNull
import org.amshove.kluent.shouldHaveSize
import org.junit.jupiter.api.Test

internal class CamundaParserCollaborationTest {
    
    @Test
    fun `XML process with swimlane Camunda elements is parseable without error`() {
        val processObject: BpmnFileObject?

        processObject = CamundaParser().parse("swimlanes.bpmn".asResource()!!)

        assertCollaborations(processObject)

        processObject.processes.shouldHaveSize(3)
        assertProcess0(processObject)
        assertProcess1(processObject)
        assertProcess2(processObject)

        val allProps = BpmnFileObject(processObject.processes, processObject.collaborations, processObject.diagram).toView(CamundaObjectFactory())
    }

    private fun assertCollaborations(processObject: BpmnFileObject) {
        processObject.collaborations.shouldHaveSize(1)
        processObject.collaborations[0].participant!!.shouldHaveSize(3)
        processObject.collaborations[0].id.shouldBeEqualTo("laneInParticipant2")
        processObject.collaborations[0].participant!![0].id.shouldBeEqualTo("participant")
        processObject.collaborations[0].participant!![0].name.shouldBeEqualTo("participant1")
        processObject.collaborations[0].participant!![0].processRef.shouldBeEqualTo("Process_1f2g5n1")
        processObject.collaborations[0].participant!![0].documentation.shouldBeNull()
        processObject.collaborations[0].participant!![1].id.shouldBeEqualTo("participant2")
        processObject.collaborations[0].participant!![1].name.shouldBeEqualTo("Participant 2")
        processObject.collaborations[0].participant!![1].processRef.shouldBeEqualTo("Process_07fjg5b")
        processObject.collaborations[0].participant!![1].documentation.shouldBeEqualTo("Some docs")
        processObject.collaborations[0].participant!![2].id.shouldBeEqualTo("participant3")
        processObject.collaborations[0].participant!![2].name.shouldBeEqualTo("Participant 3")
        processObject.collaborations[0].participant!![2].processRef.shouldBeEqualTo("Process_1yzct8l")
        processObject.collaborations[0].messageFlow!!.shouldHaveSize(5)
        processObject.collaborations[0].messageFlow!![0].id.shouldBeEqualTo("participant2ToParticipant1Flow")
        processObject.collaborations[0].messageFlow!![0].name?.shouldBeEqualTo("Participant 2 to participant 1")
        processObject.collaborations[0].messageFlow!![0].documentation?.shouldBeEqualTo("Docs")
        processObject.collaborations[0].messageFlow!![0].sourceRef?.shouldBeEqualTo("task1Participant2")
        processObject.collaborations[0].messageFlow!![0].targetRef?.shouldBeEqualTo("task1")
        processObject.collaborations[0].messageFlow!![1].id.shouldBeEqualTo("participant1ToParticipant2Flow")
        processObject.collaborations[0].messageFlow!![1].name?.shouldBeEqualTo("Participant 1 to Participant 2 flow")
        processObject.collaborations[0].messageFlow!![1].documentation?.shouldBeEqualTo("Some docs")
        processObject.collaborations[0].messageFlow!![1].sourceRef?.shouldBeEqualTo("task2")
        processObject.collaborations[0].messageFlow!![1].targetRef?.shouldBeEqualTo("task2Participant2")
        processObject.collaborations[0].messageFlow!![2].id.shouldBeEqualTo("Flow_1873mlb")
        processObject.collaborations[0].messageFlow!![2].name?.shouldBeNull()
        processObject.collaborations[0].messageFlow!![2].documentation?.shouldBeNull()
        processObject.collaborations[0].messageFlow!![2].sourceRef?.shouldBeEqualTo("task4participant3")
        processObject.collaborations[0].messageFlow!![2].targetRef?.shouldBeEqualTo("participant2")
        processObject.collaborations[0].messageFlow!![3].id.shouldBeEqualTo("Flow_1bq8rro")
        processObject.collaborations[0].messageFlow!![3].name?.shouldBeNull()
        processObject.collaborations[0].messageFlow!![3].documentation?.shouldBeNull()
        processObject.collaborations[0].messageFlow!![3].sourceRef?.shouldBeEqualTo("task3Participant2")
        processObject.collaborations[0].messageFlow!![3].targetRef?.shouldBeEqualTo("participant3")
        processObject.collaborations[0].messageFlow!![4].id.shouldBeEqualTo("task3Participant2ToParticipant1Flow")
        processObject.collaborations[0].messageFlow!![4].name?.shouldBeEqualTo("Task 3 Participant 2 to Participant 1 flow")
        processObject.collaborations[0].messageFlow!![4].documentation?.shouldBeNull()
        processObject.collaborations[0].messageFlow!![4].sourceRef?.shouldBeEqualTo("task3Participant2")
        processObject.collaborations[0].messageFlow!![4].targetRef?.shouldBeEqualTo("participant")
    }

    private fun assertProcess0(processObject: BpmnFileObject) {
        processObject.processes[0].id.id.shouldBeEqualTo("Process_1f2g5n1")
        processObject.processes[0].laneSets!!.shouldHaveSize(1)
        processObject.processes[0].laneSets!![0].id.shouldBeEqualTo("LaneSet_050m5um")
        processObject.processes[0].laneSets!![0].lanes!!.shouldHaveSize(3)
        processObject.processes[0].laneSets!![0].lanes!![0].id.shouldBeEqualTo("lane1")
        processObject.processes[0].laneSets!![0].lanes!![0].name.shouldBeEqualTo("Lane 1")
        processObject.processes[0].laneSets!![0].lanes!![0].documentation.shouldBeEqualTo("Lane 1 docs")
        processObject.processes[0].laneSets!![0].lanes!![0].flowNodeRef!!.shouldHaveSize(2)
        processObject.processes[0].laneSets!![0].lanes!![0].flowNodeRef!![0].ref.shouldBeEqualTo("startEvent")
        processObject.processes[0].laneSets!![0].lanes!![0].flowNodeRef!![1].ref.shouldBeEqualTo("endEvent")
        processObject.processes[0].laneSets!![0].lanes!![1].id.shouldBeEqualTo("Lane2")
        processObject.processes[0].laneSets!![0].lanes!![1].name.shouldBeEqualTo("Lane 2")
        processObject.processes[0].laneSets!![0].lanes!![1].documentation.shouldBeEqualTo("Lane 2 docs")
        processObject.processes[0].laneSets!![0].lanes!![1].flowNodeRef!!.shouldHaveSize(2)
        processObject.processes[0].laneSets!![0].lanes!![1].flowNodeRef!![0].ref.shouldBeEqualTo("task1")
        processObject.processes[0].laneSets!![0].lanes!![1].flowNodeRef!![1].ref.shouldBeEqualTo("task2")
        processObject.processes[0].laneSets!![0].lanes!![2].id.shouldBeEqualTo("lane3")
        processObject.processes[0].laneSets!![0].lanes!![2].name.shouldBeEqualTo("Lane3")
        processObject.processes[0].laneSets!![0].lanes!![2].documentation.shouldBeEqualTo("Lane 3 docs")
        processObject.processes[0].laneSets!![0].lanes!![2].flowNodeRef!!.shouldHaveSize(1)
        processObject.processes[0].laneSets!![0].lanes!![2].flowNodeRef!![0].ref.shouldBeEqualTo("exclusiveGateway")
    }

    private fun assertProcess1(processObject: BpmnFileObject) {
        processObject.processes[1].id.id.shouldBeEqualTo("Process_07fjg5b")
        processObject.processes[1].laneSets!!.shouldHaveSize(1)
        processObject.processes[1].laneSets!![0].id.shouldBeEqualTo("LaneSet_137rl2x")
        processObject.processes[1].laneSets!![0].lanes!!.shouldHaveSize(2)
        processObject.processes[1].laneSets!![0].lanes!![0].id.shouldBeEqualTo("lane1Participant2")
        processObject.processes[1].laneSets!![0].lanes!![0].name.shouldBeEqualTo("Lane 1 Participant 2")
        processObject.processes[1].laneSets!![0].lanes!![0].documentation.shouldBeNull()
        processObject.processes[1].laneSets!![0].lanes!![0].flowNodeRef!!.shouldHaveSize(2)
        processObject.processes[1].laneSets!![0].lanes!![0].flowNodeRef!![0].ref.shouldBeEqualTo("task1Participant2")
        processObject.processes[1].laneSets!![0].lanes!![0].flowNodeRef!![1].ref.shouldBeEqualTo("task2Participant2")
        processObject.processes[1].laneSets!![0].lanes!![1].id.shouldBeEqualTo("lane2Participant2")
        processObject.processes[1].laneSets!![0].lanes!![1].name.shouldBeEqualTo("Lane 2 participant 2")
        processObject.processes[1].laneSets!![0].lanes!![1].documentation.shouldBeNull()
        processObject.processes[1].laneSets!![0].lanes!![1].flowNodeRef!!.shouldHaveSize(1)
        processObject.processes[1].laneSets!![0].lanes!![1].flowNodeRef!![0].ref.shouldBeEqualTo("task3Participant2")
    }

    private fun assertProcess2(processObject: BpmnFileObject) {
        processObject.processes[2].id.id.shouldBeEqualTo("Process_1yzct8l")
        processObject.processes[2].laneSets!!.shouldHaveSize(0)
    }
}
