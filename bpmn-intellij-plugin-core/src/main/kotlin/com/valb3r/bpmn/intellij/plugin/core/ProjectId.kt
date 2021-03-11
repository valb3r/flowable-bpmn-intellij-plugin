package com.valb3r.bpmn.intellij.plugin.core

import com.intellij.openapi.project.Project

fun Project?.id(): String = "${this?.name}${this}"