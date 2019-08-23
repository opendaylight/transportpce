/*
 * Copyright © 2018 Orange, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.pce.gnpy;

import com.google.common.base.Function;
import com.google.common.collect.FluentIterable;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Collections;
import java.util.Optional;

import org.opendaylight.mdsal.binding.dom.codec.impl.BindingNormalizedNodeCodecRegistry;
import org.opendaylight.mdsal.binding.generator.impl.ModuleInfoBackedContext;
import org.opendaylight.mdsal.binding.generator.util.BindingRuntimeContext;
import org.opendaylight.mdsal.binding.spec.reflect.BindingReflections;
import org.opendaylight.transportpce.common.DataStoreContext;
import org.opendaylight.transportpce.common.converter.XMLDataObjectConverter;
import org.opendaylight.transportpce.common.network.NetworkTransactionService;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.org.openroadm.device.container.OrgOpenroadmDevice;
import org.opendaylight.yangtools.yang.binding.BindingStreamEventWriter;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier.PathArgument;
import org.opendaylight.yangtools.yang.binding.YangModuleInfo;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.api.schema.stream.NormalizedNodeStreamWriter;
import org.opendaylight.yangtools.yang.data.codec.gson.JSONCodecFactorySupplier;
import org.opendaylight.yangtools.yang.data.codec.gson.JSONNormalizedNodeStreamWriter;
import org.opendaylight.yangtools.yang.data.codec.gson.JsonWriterFactory;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.opendaylight.yangtools.yang.model.api.SchemaPath;
import org.opendaylight.yangtools.yang.model.parser.api.YangSyntaxErrorException;
import org.opendaylight.yangtools.yang.parser.spi.meta.ReactorException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ServiceDataStoreOperationsImpl implements ServiceDataStoreOperations {

    private static final Logger LOG = LoggerFactory.getLogger(ServiceDataStoreOperationsImpl.class);

    public ServiceDataStoreOperationsImpl(NetworkTransactionService networkTransactionService) {
    }

    public void createXMLFromDevice(DataStoreContext dataStoreContextUtil, OrgOpenroadmDevice device, String output) {
        if (device != null) {
            Optional<NormalizedNode<?, ?>> transformIntoNormalizedNode = null;
            XMLDataObjectConverter cwDsU = XMLDataObjectConverter.createWithDataStoreUtil(dataStoreContextUtil);
            transformIntoNormalizedNode = cwDsU.toNormalizedNodes(device, OrgOpenroadmDevice.class);
            if (!transformIntoNormalizedNode.isPresent()) {
                throw new IllegalStateException(
                        String.format("Could not transform the input %s into normalized nodes", device));
            }
            Writer writerFromDataObject =
                cwDsU.writerFromDataObject(device, OrgOpenroadmDevice.class,cwDsU.dataContainer());
            try {
                BufferedWriter writer = new BufferedWriter(new FileWriter(output));
                writer.write(writerFromDataObject.toString());
                writer.close();
            } catch (IOException e) {
                LOG.error("Bufferwriter error ");
            }
            LOG.debug("GNPy: device xml : {}", writerFromDataObject.toString());
        }
    }

    public String createJsonStringFromDataObject(final InstanceIdentifier<?> id, DataObject object) throws Exception {

        final SchemaPath scPath = SchemaPath
                .create(FluentIterable.from(id.getPathArguments()).transform(new Function<PathArgument, QName>() {
                    @Override
                    public QName apply(final PathArgument input) {
                        return BindingReflections.findQName(input.getType());
                    }
                }), true);
        final Writer writer = new StringWriter();
        NormalizedNodeStreamWriter domWriter;

        try {
            // Prepare the variables
            final ModuleInfoBackedContext moduleContext = ModuleInfoBackedContext.create();
            Iterable<? extends YangModuleInfo> moduleInfos = Collections
                    .singleton(BindingReflections.getModuleInfo(object.getClass()));
            moduleContext.addModuleInfos(moduleInfos);
            SchemaContext schemaContext = moduleContext.tryToCreateSchemaContext().get();
            BindingRuntimeContext bindingContext;
            bindingContext = BindingRuntimeContext.create(moduleContext, schemaContext);
            final BindingNormalizedNodeCodecRegistry codecRegistry =
                new BindingNormalizedNodeCodecRegistry(bindingContext);

            /*
             * This function needs : - context - scPath.getParent() -
             * scPath.getLastComponent().getNamespace(), -
             * JsonWriterFactory.createJsonWriter(writer)
             */
            domWriter = JSONNormalizedNodeStreamWriter.createExclusiveWriter(
                JSONCodecFactorySupplier.DRAFT_LHOTKA_NETMOD_YANG_JSON_02.createSimple(schemaContext),
                scPath.getParent(), scPath.getLastComponent().getNamespace(),
                JsonWriterFactory.createJsonWriter(writer, 2));
            // The write part
            final BindingStreamEventWriter bindingWriter = codecRegistry.newWriter(id, domWriter);
            codecRegistry.getSerializer(id.getTargetType()).serialize(object, bindingWriter);
            domWriter.close();
            writer.close();
        } catch (IOException e) {
            LOG.error("GNPy: writer error ");
        } catch (YangSyntaxErrorException e) {
            LOG.warn("GNPy: exception {} occured during json file creation", e.getMessage(), e);
        } catch (ReactorException e) {
            LOG.warn("GNPy: exception {} occured during json file creation", e.getMessage(), e);
        }
        return writer.toString();
    }

    // Write the json as a string in a file
    public void writeStringFile(String jsonString, String fileName) {
        try {
            FileWriter file = new FileWriter(fileName);
            file.write(jsonString);
            file.close();
        } catch (IOException e) {
            LOG.error("GNPy: writer error ");
        }
    }
}
