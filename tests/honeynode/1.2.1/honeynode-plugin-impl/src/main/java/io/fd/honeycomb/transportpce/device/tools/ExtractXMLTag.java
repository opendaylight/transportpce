/*
 * Copyright (c) 2018 Orange and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at:
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.fd.honeycomb.transportpce.device.tools;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * @author Martial Coulibaly ( martial.coulibaly@gfi.com ) on behalf of Orange
 *
 */
public class ExtractXMLTag {
    private static final Logger LOG = LoggerFactory.getLogger(ExtractXMLTag.class);
    private static ClassLoader classLoader = Thread.currentThread().getContextClassLoader();

    private ExtractXMLTag() {

    }

    /**
     * Extract tag xml element
     * from xml file.
     *
     * @param oper_path path to operational file
     * @param tag XML tag to be extracted
     * @param namespace XML tag namespace (optional)
     * @return String XML extract element
     */
    public static String extractTagElement(String oper_path, String tag, String namespace) {
        String result = null;
        LOG.info("Getting {} xml data", tag);
        DocumentBuilderFactory dbfac = DocumentBuilderFactory.newInstance();
        try {
            DocumentBuilder docBuilder = dbfac.newDocumentBuilder();
            Document doc = docBuilder.parse(new File(classLoader.getResource(oper_path).getFile()));
            Document extract = extractDom(doc, tag, namespace);
            if (extract != null) {
                result = getStringFromDocument(extract);
            } else {
                throw new NullPointerException("Failed to extract data from document !");
            }
        } catch (ParserConfigurationException | SAXException | IOException | TransformerException e) {
            LOG.error("failed to extract data from document", e);
        }
        return result;
    }

    public static Document extractDom(Document doc, String tag, String namespace) throws ParserConfigurationException {
        NodeList nodeList = doc.getElementsByTagName(tag);
        if (nodeList.getLength() > 0) {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = dbf.newDocumentBuilder();
            Document result = builder.newDocument();
            Element root = result.createElementNS("urn:ietf:params:xml:ns:netconf:base:1.0", "data");
            result.appendChild(root);
            Node newNode = result.importNode(nodeList.item(0), true);
            result.getDocumentElement().appendChild(newNode);
            return result;
        } else {
            LOG.warn("no {} object present in doc",tag);
            return null;
        }
    }

    public static String getStringFromDocument(Document doc) throws TransformerException {
        DOMSource domSource = new DOMSource(doc);
        StringWriter writer = new StringWriter();
        StreamResult result = new StreamResult(writer);
        TransformerFactory tf = TransformerFactory.newInstance();
        Transformer transformer = tf.newTransformer();
        transformer.transform(domSource, result);
        return writer.toString();
    }

}
