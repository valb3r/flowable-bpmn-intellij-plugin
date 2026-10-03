package com.valb3r.bpmn.intellij.plugin.flowable.actions

import com.intellij.database.psi.DbElement
import com.valb3r.bpmn.intellij.plugin.commons.actions.DefaultAttachBpmnDebuggerToDbAction
import com.valb3r.bpmn.intellij.plugin.commons.actions.IntelliJBpmnDebugger

class AttachFlowableBpmnDebuggerToDbAction : DefaultAttachBpmnDebuggerToDbAction({ schema, selectionId, processId, processName ->
    FlowableIntelliJBpmnDebugger(schema, selectionId, processId, processName)
})

class FlowableIntelliJBpmnDebugger(
    schema: DbElement,
    selectionId: String?,
    processId: String,
    processName: String?
): IntelliJBpmnDebugger(schema, selectionId, processId, processName) {

    override fun statementForRuntimeSelection(schema: String, selectionId: String?, checkProcessName: Boolean): String = activitySelection(schema, "act_ru_actinst", selectionId, checkProcessName)

    override fun statementForHistoricalSelection(schema: String, selectionId: String?, checkProcessName: Boolean): String = activitySelection(schema, "act_hi_actinst", selectionId, checkProcessName)

    private fun activitySelection(schema: String, activityTable: String, selectionId: String?, checkProcessName: Boolean): String {
        val selection = if (selectionId == null) """
            WHERE def.key_ = ?
              ${if (checkProcessName) "AND def.name_ = ?" else ""}
              AND re.proc_inst_id_ = (
                  SELECT candidate.proc_inst_id_ FROM ${"$schema."}$activityTable candidate
                  JOIN ${"$schema."}act_re_procdef candidate_def ON candidate.proc_def_id_ = candidate_def.id_
                  WHERE candidate_def.key_ = ?
                    ${if (checkProcessName) "AND candidate_def.name_ = ?" else ""}
                  ORDER BY candidate.start_time_ DESC, candidate.id_ DESC
                  LIMIT 1
              )
        """ else """
            WHERE (re.proc_inst_id_ = ? OR re.execution_id_ = ?)
              AND def.key_ = ?
              ${if (checkProcessName) "AND def.name_ = ?" else ""}
        """

        return """
            SELECT re.proc_inst_id_, re.act_id_, re.start_time_ FROM ${"$schema."}$activityTable re
            JOIN ${"$schema."}act_re_procdef def ON re.proc_def_id_ = def.id_
            $selection
            ORDER BY re.start_time_, re.id_
        """.trimIndent()
    }
}
