package com.valb3r.bpmn.intellij.plugin.flowable.parser

import java.nio.charset.StandardCharsets

fun String.asResource(): String? = object {}::class.java.classLoader.getResource(this)?.readText(StandardCharsets.UTF_8)