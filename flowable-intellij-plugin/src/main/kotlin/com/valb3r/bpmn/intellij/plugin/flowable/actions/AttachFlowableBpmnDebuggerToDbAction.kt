package com.valb3r.bpmn.intellij.plugin.flowable.actions

import com.intellij.database.psi.DbElement
import com.valb3r.bpmn.intellij.plugin.commons.actions.DefaultAttachBpmnDebuggerToDbAction
import com.valb3r.bpmn.intellij.plugin.commons.actions.IntelliJBpmnDebugger

class FlowableAttachBpmnDebuggerToDbAction : DefaultAttachBpmnDebuggerToDbAction({ FlowableIntelliJBpmnDebugger(it) })

class FlowableIntelliJBpmnDebugger(schema: DbElement): IntelliJBpmnDebugger(schema) {

    override fun statementForRuntimeSelection(schema: String): String {
        return """
                SELECT re.act_id_ FROM ${"$schema."}act_ru_actinst re JOIN ${"$schema."}act_re_procdef def ON re.proc_def_id_ = def.id_
                WHERE re.proc_inst_id_ = (
                    SELECT re.proc_inst_id_ FROM ${"$schema."}act_ru_actinst re JOIN ${"$schema."}act_re_procdef def ON re.proc_def_id_ = def.id_
                    WHERE re.start_time_ =
                          (
                              SELECT MAX(re.start_time_) FROM ${"$schema."}act_ru_actinst re JOIN ${"$schema."}act_re_procdef def ON re.proc_def_id_ = def.id_ WHERE def.key_ = ?
                          )
                    LIMIT 1
                ) ORDER BY re.start_time_, re.id_

            """.trimIndent()
    }

    override fun statementForHistoricalSelection(schema: String): String {
        return """
                SELECT re.act_id_ FROM ${"$schema."}act_hi_actinst re JOIN ${"$schema."}act_re_procdef def ON re.proc_def_id_ = def.id_
                WHERE re.proc_inst_id_ = (
                    SELECT re.proc_inst_id_ FROM ${"$schema."}act_hi_actinst re JOIN ${"$schema."}act_re_procdef def ON re.proc_def_id_ = def.id_
                    WHERE re.start_time_ =
                          (
                              SELECT MAX(re.start_time_) FROM ${"$schema."}act_hi_actinst re JOIN ${"$schema."}act_re_procdef def ON re.proc_def_id_ = def.id_ WHERE def.key_ = ?
                          )
                    LIMIT 1
                ) ORDER BY re.start_time_, re.id_

            """.trimIndent()
    }
}
