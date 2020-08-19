/*
 * Copyright © 2018 Orange, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.pce.gnpy;

import com.google.common.collect.FluentIterable;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Collections;
import java.util.Optional;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.binding.runtime.api.BindingRuntimeContext;
import org.opendaylight.binding.runtime.spi.BindingRuntimeHelpers;
//import org.opendaylight.mdsal.binding.dom.codec.api.BindingNormalizedNodeSerializer;
//import org.opendaylight.mdsal.binding.dom.codec.impl.BindingCodecContext;
import org.opendaylight.mdsal.binding.spec.reflect.BindingReflections;
import org.opendaylight.transportpce.common.DataStoreContext;
import org.opendaylight.transportpce.common.converter.XMLDataObjectConverter;
import org.opendaylight.transportpce.common.network.NetworkTransactionService;
import org.opendaylight.yang.gen.v1.gnpy.path.rev200202.Result;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.org.openroadm.device.container.OrgOpenroadmDevice;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.YangModuleInfo;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.api.schema.stream.NormalizedNodeStreamWriter;
import org.opendaylight.yangtools.yang.data.codec.gson.JSONCodecFactorySupplier;
import org.opendaylight.yangtools.yang.data.codec.gson.JSONNormalizedNodeStreamWriter;
import org.opendaylight.yangtools.yang.data.codec.gson.JsonWriterFactory;
import org.opendaylight.yangtools.yang.model.api.EffectiveModelContext;
import org.opendaylight.yangtools.yang.model.api.SchemaPath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@edu.umd.cs.findbugs.annotations.SuppressFBWarnings(
    value = "DLS_DEAD_LOCAL_STORE",
    justification = "FIXME API aluminium migration pending")
public class ServiceDataStoreOperationsImpl implements ServiceDataStoreOperations {

    private static final Logger LOG = LoggerFactory.getLogger(ServiceDataStoreOperationsImpl.class);

    public ServiceDataStoreOperationsImpl(NetworkTransactionService networkTransactionService) {
    }

    @Override
    public void createXMLFromDevice(DataStoreContext dataStoreContextUtil, OrgOpenroadmDevice device, String output)
        throws GnpyException {

        if (device != null) {
            Optional<NormalizedNode<?, ?>> transformIntoNormalizedNode = null;
            XMLDataObjectConverter cwDsU = XMLDataObjectConverter.createWithDataStoreUtil(dataStoreContextUtil);
            transformIntoNormalizedNode = cwDsU.toNormalizedNodes(device, OrgOpenroadmDevice.class);
            if (!transformIntoNormalizedNode.isPresent()) {
                throw new GnpyException(String.format(
                    "In ServiceDataStoreOperationsImpl: Cannot transform the device %s into normalized nodes",
                    device.toString()));
            }
            Writer writerFromDataObject =
                cwDsU.writerFromDataObject(device, OrgOpenroadmDevice.class,cwDsU.dataContainer());
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(output,StandardCharsets.UTF_8))) {
                writer.write(writerFromDataObject.toString());
            } catch (IOException e) {
                throw new GnpyException(
                    String.format("In ServiceDataStoreOperationsImpl : Bufferwriter error %s",e));
            }
            LOG.debug("GNPy: device xml : {}", writerFromDataObject);
        }
    }

    @Override
    public String createJsonStringFromDataObject(final InstanceIdentifier<?> id, DataObject object)
        throws GnpyException, Exception {

        final SchemaPath scPath = SchemaPath.create(FluentIterable
                .from(id.getPathArguments())
                .transform(input -> BindingReflections.findQName(input.getType())), true);

        // Prepare the variables
        // Create the schema context
        Collection<? extends YangModuleInfo> moduleInfos = Collections.singleton(BindingReflections
                .getModuleInfo(Result.class));
        @NonNull
        EffectiveModelContext schemaContext = BindingRuntimeHelpers.createEffectiveModel(moduleInfos);

        // Create the binding binding normalized node codec registry
        //BindingRuntimeContext bindingContext = BindingRuntimeHelpers.createRuntimeContext();
        //final BindingNormalizedNodeSerializer codecRegistry = new BindingCodecContext(bindingContext);

        /*
         * This function needs : - context - scPath.getParent() -
         * scPath.getLastComponent().getNamespace(), -
         * JsonWriterFactory.createJsonWriter(writer)
         */
        final Writer writer = new StringWriter();

        try (NormalizedNodeStreamWriter domWriter = JSONNormalizedNodeStreamWriter.createExclusiveWriter(
                JSONCodecFactorySupplier.DRAFT_LHOTKA_NETMOD_YANG_JSON_02.createSimple(schemaContext),
                scPath.getParent(), scPath.getLastComponent().getNamespace(),
                JsonWriterFactory.createJsonWriter(writer, 2));) {
            // The write part
            //FIXME
            //codecRegistry.getSerializer(id.getTargetType()).serialize(object, codecRegistry.newWriter(id, domWriter));
        } catch (IOException e) {
            throw new GnpyException("In ServiceDataStoreOperationsImpl: exception during json file creation",e);
        }
        return writer.toString();
    }

    // Write the json as a string in a file
    @Override
    public void writeStringFile(String jsonString, String fileName) throws GnpyException {
        try (FileWriter file = new FileWriter(fileName,StandardCharsets.UTF_8)) {
            file.write(jsonString);
        } catch (IOException e) {
            throw new GnpyException("In ServiceDataStoreOperationsImpl : exception during file writing",e);
        }
    }
}
