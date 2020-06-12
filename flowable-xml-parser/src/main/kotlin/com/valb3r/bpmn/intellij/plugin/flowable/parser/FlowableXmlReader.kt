package com.valb3r.bpmn.intellij.plugin.flowable.parser

import com.valb3r.bpmn.intellij.plugin.flowable.parser.nodes.BpmnFile
import org.dom4j.*
import org.dom4j.io.DocumentSource
import org.dom4j.tree.DefaultElement
import javax.xml.bind.JAXBContext
import javax.xml.bind.Unmarshaller


class FlowableXmlReader {

    fun parse(doc: Document): BpmnFile {
        doc.accept(NameSpaceCleaner())
        val jc: JAXBContext = JAXBContext.newInstance(BpmnFile::class.java)
        val marshaller: Unmarshaller = jc.createUnmarshaller()
        return marshaller.unmarshal(DocumentSource(doc)) as BpmnFile
    }

    internal class NameSpaceCleaner : VisitorSupport() {
        override fun visit(document: Document) {
            (document.rootElement as DefaultElement).namespace = Namespace.NO_NAMESPACE
            document.rootElement.additionalNamespaces().clear()
        }

        override fun visit(namespace: Namespace) {
            namespace.detach()
        }

        override fun visit(node: Attribute) {
            if (node.toString().contains("xmlns")
                    || node.toString().contains("xsi:")) {
                node.detach()
            }
        }

        override fun visit(node: Element) {
            if (node is DefaultElement) {
                node.setNamespace(Namespace.NO_NAMESPACE)
            }
        }
    }
}