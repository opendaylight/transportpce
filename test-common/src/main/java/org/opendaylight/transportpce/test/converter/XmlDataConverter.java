/*
 * Copyright © 2025 Orange Innovation, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.test.converter;

import java.io.BufferedReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.Set;
import javax.xml.XMLConstants;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import org.opendaylight.mdsal.binding.dom.adapter.ConstantAdapterContext;
import org.opendaylight.yangtools.binding.DataObject;
import org.opendaylight.yangtools.binding.DataObjectIdentifier;
import org.opendaylight.yangtools.binding.meta.YangModuleInfo;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNodes;
import org.opendaylight.yangtools.yang.data.api.schema.stream.NormalizedNodeStreamWriter;
import org.opendaylight.yangtools.yang.data.api.schema.stream.NormalizedNodeWriter;
import org.opendaylight.yangtools.yang.data.codec.xml.XMLStreamNormalizedNodeStreamWriter;
import org.opendaylight.yangtools.yang.data.codec.xml.XmlCodecFactory;
import org.opendaylight.yangtools.yang.data.codec.xml.XmlParserStream;
import org.opendaylight.yangtools.yang.data.impl.schema.ImmutableNormalizedNodeStreamWriter;
import org.opendaylight.yangtools.yang.data.impl.schema.NormalizationResultHolder;
import org.opendaylight.yangtools.yang.model.util.SchemaInferenceStack;
import org.opendaylight.yangtools.yang.model.util.SchemaInferenceStack.Inference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class XmlDataConverter extends AbstractDataConverter {

    private static final Logger LOG = LoggerFactory.getLogger(XmlDataConverter.class);
    private final XmlCodecFactory codecFactory;
    private final ConstantAdapterContext codec;

    public XmlDataConverter(Set<YangModuleInfo> models) {
        super(models);
        this.codecFactory = XmlCodecFactory.create(getBindingRuntimeContext().modelContext());
        this.codec = new ConstantAdapterContext(getBindingCodecContext());
    }

    @Override
    public String serialize(DataObjectIdentifier id, DataObject dataContainer) throws ProcessingException {
        LOG.debug("Calling writer for {}", dataContainer);
        try (Writer writer = new StringWriter();) {
            XMLOutputFactory factory = XMLOutputFactory.newFactory();
            factory.setProperty(XMLOutputFactory.IS_REPAIRING_NAMESPACES, true);
            XMLStreamWriter xmlStreamWriter = factory.createXMLStreamWriter(writer);
            NormalizedNodeStreamWriter streamWriter = XMLStreamNormalizedNodeStreamWriter
                    .create(xmlStreamWriter, getBindingRuntimeContext().modelContext());
            NormalizedNodeWriter nodeWriter = NormalizedNodeWriter.forStreamWriter(streamWriter);
            nodeWriter.write(codec.currentSerializer().toNormalizedDataObject(id, dataContainer).node());
            nodeWriter.flush();
            return writer.toString();
        } catch (XMLStreamException | IOException e) {
            throw new ProcessingException("Error serializing a DataObject to the output stream", e);
        }
    }

    @Override
    public void serializeToFile(DataObjectIdentifier id, DataObject dataContainer, String filename)
            throws ProcessingException {
        try (FileWriter fileWriter = new FileWriter(filename, StandardCharsets.UTF_8)) {
            String output = serialize(id, dataContainer);
            fileWriter.write(output);
        } catch (IOException e) {
            throw new ProcessingException("Error serializing a DataObject to the output file", e);
        }
    }

    @Override
    public DataObject deserialize(String xmlValue, QName object) throws ProcessingException {
        LOG.debug("Calling writer for {}", xmlValue);

        final NormalizationResultHolder resultHolder = new NormalizationResultHolder();
        final NormalizedNodeStreamWriter writer = ImmutableNormalizedNodeStreamWriter.from(resultHolder);
        YangInstanceIdentifier path = YangInstanceIdentifier.builder().node(object).build();
        Inference schema = SchemaInferenceStack.of(codecFactory.modelContext()).toInference();

        try (XmlParserStream xmlParser = XmlParserStream.create(writer, codecFactory, schema)) {
            XMLInputFactory inputFactory = XMLInputFactory.newInstance();
            inputFactory.setProperty(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            inputFactory.setProperty(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
            String value = "<data xmlns=\"urn:ietf:params:xml:ns:netconf:base:1.0\">" + xmlValue + "</data>";
            XMLStreamReader reader = inputFactory.createXMLStreamReader(new StringReader(value));
            xmlParser.parse(reader);
            xmlParser.flush();

            Optional<NormalizedNode> nodeOpt = Optional.ofNullable(resultHolder.getResult().data());
            NormalizedNode node = nodeOpt.orElseThrow();
            Optional<NormalizedNode> data = NormalizedNodes.getDirectChild(node,
                    YangInstanceIdentifier.of(object).getLastPathArgument());
            DataObject result = getBindingCodecContext().fromNormalizedNode(path, data.orElseThrow()).getValue();
            return result;
        } catch (IOException | XMLStreamException e) {
            throw new ProcessingException("Error deserializing an XML string to the DataObject", e);
        }
    }

    @Override
    public DataObject deserialize(Reader xmlValue, QName object) throws ProcessingException {
        LOG.debug("Calling writer for {}", xmlValue);

        final NormalizationResultHolder resultHolder = new NormalizationResultHolder();
        final NormalizedNodeStreamWriter writer = ImmutableNormalizedNodeStreamWriter.from(resultHolder);
        YangInstanceIdentifier path = YangInstanceIdentifier.builder().node(object).build();
        Inference schema = SchemaInferenceStack.of(codecFactory.modelContext()).toInference();

        try (XmlParserStream xmlParser = XmlParserStream.create(writer, schema)) {
            XMLInputFactory inputFactory = XMLInputFactory.newInstance();
            inputFactory.setProperty(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            inputFactory.setProperty(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
            String value = createXmlString(xmlValue);
            XMLStreamReader reader = inputFactory.createXMLStreamReader(new StringReader(value));
            xmlParser.parse(reader);
            xmlParser.flush();

            Optional<NormalizedNode> nodeOpt = Optional.ofNullable(resultHolder.getResult().data());
            NormalizedNode node = nodeOpt.orElseThrow();
            Optional<NormalizedNode> data = NormalizedNodes.getDirectChild(node,
                    YangInstanceIdentifier.of(object).getLastPathArgument());
            DataObject result = getBindingCodecContext().fromNormalizedNode(path, data.orElseThrow()).getValue();
            return result;
        } catch (IOException | XMLStreamException e) {
            throw new ProcessingException("Error deserializing a reader to the output DataObject", e);
        }
    }

    private static String createXmlString(Reader reader) {
        String value = getStringFromReader(reader);
        return "<data xmlns=\"urn:ietf:params:xml:ns:netconf:base:1.0\">" + value + "</data>";
    }

    private static String getStringFromReader(Reader reader) {
        StringBuilder stringBuilder = new StringBuilder();
        try (BufferedReader bufferedReader = new BufferedReader(reader)) {
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                stringBuilder.append(line).append(System.lineSeparator());
            }
        } catch (IOException e) {
            LOG.error("Error getting String from Reader", e);
        }
        return stringBuilder.toString().trim();
    }
}
