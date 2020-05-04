package com.valb3r.bpmn.intellij.plugin.flowable.parser

import java.nio.file.Paths

fun main() {
    val target = Paths.get("/home/valb3r/IdeaProjects/flowable-intellij/flowable-xml-parser/src/main/resources/hbci-list-accounts.bpmn20.xml").toFile()

    val process = FlowableParser().parse(target.inputStream())
    System.out.println("HEllo")
}
