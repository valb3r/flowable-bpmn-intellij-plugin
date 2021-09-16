package com.valb3r.bpmn.intellij.plugin.bpmn.parser.core;

import org.dom4j.Element;
import org.dom4j.Node;
import org.dom4j.Text;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.XMLWriter;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;

// Customized to fix https://github.com/valb3r/flowable-bpmn-intellij-plugin/issues/233
public class CustomizedXmlWriter extends XMLWriter {
    private static final String PAD_TEXT = " ";

    private final OutputFormat format;

    public CustomizedXmlWriter(OutputStream out, OutputFormat format) throws UnsupportedEncodingException {
        super(out, format);
        this.format = format;
    }

    // Almost exact copy from parent class
    @Override
    protected void writeElementContent(Element element) throws IOException {
        boolean trim = format.isTrimText();
        boolean oldPreserve = preserve;

        if (trim) { // verify we have to before more expensive test
            // Customized to fix https://github.com/valb3r/flowable-bpmn-intellij-plugin/issues/233:
            preserve = isElementSpacePreserved(element) || element.getName().equals("documentation");
            trim = !preserve;
        }

        if (trim) {
            // concatenate adjacent text nodes together
            // so that whitespace trimming works properly
            Text lastTextNode = null;
            StringBuilder buff = null;
            boolean textOnly = true;

            for (Node node : element.content()) {
                if (node instanceof Text) {
                    if (lastTextNode == null) {
                        lastTextNode = (Text) node;
                    } else {
                        if (buff == null) {
                            buff = new StringBuilder(lastTextNode.getText());
                        }

                        buff.append((node).getText());
                    }
                } else {
                    if (!textOnly && format.isPadText()) {
                        // only add the PAD_TEXT if the text itself starts with
                        // whitespace
                        char firstChar = 'a';
                        if (buff != null) {
                            firstChar = buff.charAt(0);
                        } else if (lastTextNode != null) {
                            firstChar = lastTextNode.getText().charAt(0);
                        }

                        if (Character.isWhitespace(firstChar)) {
                            writer.write(PAD_TEXT);
                        }
                    }

                    if (lastTextNode != null) {
                        if (buff != null) {
                            writeString(buff.toString());
                            buff = null;
                        } else {
                            writeString(lastTextNode.getText());
                        }

                        if (format.isPadText()) {
                            // only add the PAD_TEXT if the text itself ends
                            // with whitespace
                            char lastTextChar = 'a';
                            if (buff != null) {
                                lastTextChar = buff.charAt(buff.length() - 1);
                            } else if (lastTextNode != null) {
                                String txt = lastTextNode.getText();
                                lastTextChar = txt.charAt(txt.length() - 1);
                            }

                            if (Character.isWhitespace(lastTextChar)) {
                                writer.write(PAD_TEXT);
                            }
                        }

                        lastTextNode = null;
                    }

                    textOnly = false;
                    writeNode(node);
                }
            }

            if (lastTextNode != null) {
                if (!textOnly && format.isPadText()) {
                    // only add the PAD_TEXT if the text itself starts with
                    // whitespace
                    char firstChar = 'a';
                    if (buff != null) {
                        firstChar = buff.charAt(0);
                    } else {
                        firstChar = lastTextNode.getText().charAt(0);
                    }

                    if (Character.isWhitespace(firstChar)) {
                        writer.write(PAD_TEXT);
                    }
                }

                if (buff != null) {
                    writeString(buff.toString());
                    buff = null;
                } else {
                    writeString(lastTextNode.getText());
                }

                lastTextNode = null;
            }
        } else {
            Node lastTextNode = null;

            for (Node node : element.content()) {
                if (node instanceof Text) {
                    writeNode(node);
                    lastTextNode = node;
                } else {
                    if ((lastTextNode != null) && format.isPadText()) {
                        // only add the PAD_TEXT if the text itself ends with
                        // whitespace
                        String txt = lastTextNode.getText();
                        char lastTextChar = txt.charAt(txt.length() - 1);

                        if (Character.isWhitespace(lastTextChar)) {
                            writer.write(PAD_TEXT);
                        }
                    }

                    writeNode(node);

                    // if ((lastTextNode != null) && format.isPadText()) {
                    // writer.write(PAD_TEXT);
                    // }

                    lastTextNode = null;
                }
            }
        }

        preserve = oldPreserve;
    }
}
