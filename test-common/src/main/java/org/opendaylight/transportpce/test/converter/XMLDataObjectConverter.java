/*
 * Copyright © 2016 AT&T and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.test.converter;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.net.URISyntaxException;
import java.util.Optional;
import javax.annotation.Nonnull;
import javax.xml.XMLConstants;
import javax.xml.parsers.FactoryConfigurationError;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import org.opendaylight.mdsal.binding.dom.codec.api.BindingNormalizedNodeSerializer;
import org.opendaylight.transportpce.test.DataStoreContext;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.api.schema.stream.NormalizedNodeStreamWriter;
import org.opendaylight.yangtools.yang.data.api.schema.stream.NormalizedNodeWriter;
import org.opendaylight.yangtools.yang.data.codec.xml.XMLStreamNormalizedNodeStreamWriter;
import org.opendaylight.yangtools.yang.data.codec.xml.XmlParserStream;
import org.opendaylight.yangtools.yang.data.impl.schema.ImmutableNormalizedNodeStreamWriter;
import org.opendaylight.yangtools.yang.data.impl.schema.NormalizedNodeResult;
import org.opendaylight.yangtools.yang.model.api.EffectiveModelContext;
import org.opendaylight.yangtools.yang.model.api.EffectiveStatementInference;
import org.opendaylight.yangtools.yang.model.api.SchemaNode;
import org.opendaylight.yangtools.yang.model.api.SchemaPath;
import org.opendaylight.yangtools.yang.model.util.SchemaInferenceStack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

public final class XMLDataObjectConverter extends AbstractDataObjectConverter {

    private static final Logger LOG = LoggerFactory.getLogger(XMLDataObjectConverter.class);

    private final XMLInputFactory xmlInputFactory;

    /**
     * This is the default constructor, which should be used.
     *
     * @param schemaContext schema context for converter
     * @param codecRegistry codec registry used for converting
     *
     */
    private XMLDataObjectConverter(EffectiveModelContext schemaContext, BindingNormalizedNodeSerializer codecRegistry) {
        super(schemaContext, codecRegistry);
        this.xmlInputFactory = XMLInputFactory.newInstance();
        // set external DTD and schema to null to avoid vulnerability (sonar report)
        this.xmlInputFactory.setProperty(XMLConstants.ACCESS_EXTERNAL_DTD, "");
        this.xmlInputFactory.setProperty(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
    }

    /**
     * Extract codec and schema context (?).
     *
     * @param dataStoreContextUtil datastore context util used to extract codec and schema context
     * @return {@link AbstractDataObjectConverter}
     */
    public static XMLDataObjectConverter createWithDataStoreUtil(@Nonnull DataStoreContext dataStoreContextUtil) {
        BindingNormalizedNodeSerializer bindingToNormalizedNodeCodec =
                dataStoreContextUtil.getBindingDOMCodecServices();
        return new XMLDataObjectConverter(dataStoreContextUtil.getSchemaContext(), bindingToNormalizedNodeCodec);
    }

    /**
     * Extract codec and schema context (?).
     *
     * @param schemaContext schema context for converter
     * @param codecRegistry codec registry used for converting
     * @return new {@link XMLDataObjectConverter}
     */
    public static XMLDataObjectConverter createWithSchemaContext(@Nonnull EffectiveModelContext schemaContext,
            @Nonnull BindingNormalizedNodeSerializer codecRegistry) {
        return new XMLDataObjectConverter(schemaContext, codecRegistry);
    }

    /**
     * Transforms the XML input stream into normalized nodes.
     *
     * @param inputStream of the given XML
     * @return {@link Optional} instance of {@link NormalizedNode}.
     */
    @Override
    public Optional<NormalizedNode> transformIntoNormalizedNode(
            @Nonnull InputStream inputStream) {
        try {
            XMLStreamReader reader = this.xmlInputFactory.createXMLStreamReader(inputStream);
            return parseInputXML(reader);
        } catch (XMLStreamException e) {
            LOG.warn("XMLStreamException: {}", e.getMessage());
            return Optional.empty();
        }
    }

    public Optional<NormalizedNode> transformIntoNormalizedNode(
            @Nonnull Reader inputReader, SchemaNode parentSchema) {
        try {
            XMLStreamReader reader = this.xmlInputFactory.createXMLStreamReader(inputReader);
            return parseInputXML(reader, parentSchema);
        } catch (XMLStreamException e) {
            LOG.warn("XMLStreamException: {}", e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Transforms the XML input stream into normalized nodes.
     *
     * @param inputReader of the given XML
     * @return {@link Optional} instance of {@link NormalizedNode}.
     */
    @Override
    public Optional<NormalizedNode> transformIntoNormalizedNode(
            @Nonnull Reader inputReader) {
        try {
            XMLStreamReader reader = this.xmlInputFactory.createXMLStreamReader(inputReader);
            return parseInputXML(reader);
        } catch (XMLStreamException e) {
            LOG.warn("XMLStreamException: {}", e.getMessage());
            return Optional.empty();
        }
    }

    @Override
    public <T extends DataObject> Writer writerFromRpcDataObject(@Nonnull DataObject object, Class<T> dataObjectClass,
            ConvertType<T> convertType, QName rpcOutputQName, String rpcName) {
        Writer writer = new StringWriter();
        XMLStreamWriter xmlStreamWriter = createXmlStreamWriter(writer);
        SchemaPath rpcOutputSchemaPath = SchemaPath.create(true, QName.create(rpcOutputQName.getModule(), rpcName),
                rpcOutputQName);
        try (NormalizedNodeWriter normalizedNodeWriter = createWriterBackedNormalizedNodeWriter(xmlStreamWriter,
                rpcOutputSchemaPath)) {
            xmlStreamWriter.writeStartElement(XMLConstants.DEFAULT_NS_PREFIX,
                    rpcOutputQName.getLocalName(), rpcOutputQName.getNamespace().toString());
            xmlStreamWriter.writeDefaultNamespace(rpcOutputQName.getNamespace().toString());
            NormalizedNode rpcOutputNormalizedNode = convertType.toNormalizedNodes(dataObjectClass.cast(object),
                    dataObjectClass).get();
            for (final NormalizedNode child : ((ContainerNode)rpcOutputNormalizedNode).body()) {
                normalizedNodeWriter.write(child);
            }
            normalizedNodeWriter.flush();
            xmlStreamWriter.writeEndElement();
            xmlStreamWriter.flush();
        } catch (IOException | XMLStreamException ioe) {
            throw new IllegalStateException(ioe);
        }
        return writer;
    }

    /**
     * Returns a {@link Writer}.
     *
     * @param convertType converter used of converting into normalized node
     * @param dataObjectClass class of converting object
     * @param object object you want to convert
     *
     */
    @Override
    public <T extends DataObject> Writer writerFromDataObject(@Nonnull DataObject object, Class<T> dataObjectClass,
            ConvertType<T> convertType) {
        Writer writer = new StringWriter();

        try (NormalizedNodeWriter normalizedNodeWriter = createWriterBackedNormalizedNodeWriter(writer, null)) {
            normalizedNodeWriter
                    .write(convertType.toNormalizedNodes(dataObjectClass.cast(object), dataObjectClass).get());
            normalizedNodeWriter.flush();
        } catch (IOException ioe) {
            throw new IllegalStateException(ioe);
        }
        return writer;
    }

    private Optional<NormalizedNode> parseInputXML(
            XMLStreamReader reader) {
        return parseInputXML(reader, getSchemaContext());
    }

    private Optional<NormalizedNode> parseInputXML(
            XMLStreamReader reader, SchemaNode parentSchemaNode) {
        NormalizedNodeResult result = new NormalizedNodeResult();
        EffectiveStatementInference inference = SchemaInferenceStack.ofSchemaPath(getSchemaContext(),
            parentSchemaNode.getPath()).toInference();
        try (NormalizedNodeStreamWriter streamWriter = ImmutableNormalizedNodeStreamWriter.from(result);
            XmlParserStream xmlParser = XmlParserStream
                    .create(streamWriter, inference)) {
//             XmlParserStream xmlParser = XmlParserStream
//                     .create(streamWriter, XmlCodecFactory.create(getSchemaContext()), parentSchemaNode)) {
            xmlParser.parse(reader);
        } catch (XMLStreamException | URISyntaxException | IOException | SAXException e) {
            LOG.warn("An error occured during parsing XML input stream", e);
            return Optional.empty();
        }
        return Optional.ofNullable(result.getResult());
    }

    private NormalizedNodeWriter createWriterBackedNormalizedNodeWriter(Writer backingWriter, SchemaPath pathToParent) {
        XMLStreamWriter createXMLStreamWriter = createXmlStreamWriter(backingWriter);
        NormalizedNodeStreamWriter streamWriter;
        if (pathToParent == null) {
            streamWriter = XMLStreamNormalizedNodeStreamWriter.create(createXMLStreamWriter,
                    getSchemaContext());
        } else {
            streamWriter = XMLStreamNormalizedNodeStreamWriter.create(createXMLStreamWriter,
                    getSchemaContext(), pathToParent);
        }
        return NormalizedNodeWriter.forStreamWriter(streamWriter);
    }

    private NormalizedNodeWriter createWriterBackedNormalizedNodeWriter(XMLStreamWriter backingWriter,
            SchemaPath pathToParent) {
        NormalizedNodeStreamWriter streamWriter;
        if (pathToParent == null) {
            streamWriter = XMLStreamNormalizedNodeStreamWriter.create(backingWriter,
                    getSchemaContext());
        } else {
            streamWriter = XMLStreamNormalizedNodeStreamWriter.create(backingWriter,
                    getSchemaContext(), pathToParent);
        }
        return NormalizedNodeWriter.forStreamWriter(streamWriter);
    }

    private static XMLStreamWriter createXmlStreamWriter(Writer backingWriter) {
        XMLStreamWriter xmlStreamWriter;
        try {
            XMLOutputFactory factory = XMLOutputFactory.newFactory();
            factory.setProperty(XMLOutputFactory.IS_REPAIRING_NAMESPACES, true);
            xmlStreamWriter = factory.createXMLStreamWriter(backingWriter);
        } catch (XMLStreamException | FactoryConfigurationError e) {
            LOG.error("Error while creating XML writer: ", e);
            throw new IllegalStateException(e);
        }
        return xmlStreamWriter;
    }
}
