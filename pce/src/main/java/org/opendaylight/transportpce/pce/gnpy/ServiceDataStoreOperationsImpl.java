/*
 * Copyright © 2018 Orange, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.pce.gnpy;

import com.google.common.collect.FluentIterable;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonWriter;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.opendaylight.binding.runtime.spi.BindingRuntimeHelpers;
import org.opendaylight.mdsal.binding.dom.codec.spi.BindingDOMCodecServices;
import org.opendaylight.mdsal.binding.spec.reflect.BindingReflections;
import org.opendaylight.transportpce.common.DataStoreContext;
import org.opendaylight.transportpce.common.converter.XMLDataObjectConverter;
import org.opendaylight.yang.gen.v1.gnpy.gnpy.api.rev190103.GnpyApi;
import org.opendaylight.yang.gen.v1.gnpy.path.rev200909.Result;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.org.openroadm.device.container.OrgOpenroadmDevice;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.YangModuleInfo;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.api.schema.stream.NormalizedNodeStreamWriter;
import org.opendaylight.yangtools.yang.data.api.schema.stream.NormalizedNodeWriter;
import org.opendaylight.yangtools.yang.data.codec.gson.JSONCodecFactory;
import org.opendaylight.yangtools.yang.data.codec.gson.JSONCodecFactorySupplier;
import org.opendaylight.yangtools.yang.data.codec.gson.JSONNormalizedNodeStreamWriter;
import org.opendaylight.yangtools.yang.data.codec.gson.JsonWriterFactory;
import org.opendaylight.yangtools.yang.model.api.EffectiveModelContext;
import org.opendaylight.yangtools.yang.model.api.SchemaPath;
import org.opendaylight.yangtools.yang.model.parser.api.YangParserFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@edu.umd.cs.findbugs.annotations.SuppressFBWarnings(
    value = "DLS_DEAD_LOCAL_STORE",
    justification = "FIXME API aluminium migration pending")
public class ServiceDataStoreOperationsImpl implements ServiceDataStoreOperations {

    private static final Logger LOG = LoggerFactory.getLogger(ServiceDataStoreOperationsImpl.class);
    private static final JsonParser PARSER = new JsonParser();
    private EffectiveModelContext schemaContext;
    private BindingDOMCodecServices bindingDOMCodecServices;

    @SuppressWarnings({"checkstyle:illegalcatch","checkstyle:AvoidHidingCauseException"})
    public ServiceDataStoreOperationsImpl(YangParserFactory parserFactory,
            BindingDOMCodecServices bindingDOMCodecServices) throws GnpyException {
        // Prepare the variables
        // Create the schema context
        List<YangModuleInfo> moduleInfos = new ArrayList<>();
        try {
            moduleInfos.add(BindingReflections
                    .getModuleInfo(Result.class));
            moduleInfos.add(BindingReflections
                    .getModuleInfo(GnpyApi.class));
            schemaContext = BindingRuntimeHelpers.createEffectiveModel(parserFactory, moduleInfos);
            this.bindingDOMCodecServices = bindingDOMCodecServices;
        } catch (Exception e) {
            LOG.error("Cannot create ServiceDataStoreOperations", e);
            throw new GnpyException("Something went wrong while init ServiceDataStoreOperations");
        }
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
    public String createJsonStringFromDataObject(final InstanceIdentifier<GnpyApi> id, GnpyApi object)
        throws GnpyException {
        final SchemaPath scPath = SchemaPath.create(FluentIterable
                .from(id.getPathArguments())
                .transform(input -> BindingReflections.findQName(input.getType())), true);
        /*
         * This function needs : - context - scPath.getParent() -
         * scPath.getLastComponent().getNamespace(), -
         * JsonWriterFactory.createJsonWriter(writer)
         */

        JSONCodecFactory codecFactory = JSONCodecFactorySupplier.DRAFT_LHOTKA_NETMOD_YANG_JSON_02
                .getShared(schemaContext);
        try (Writer writer = new StringWriter();
                JsonWriter jsonWriter = JsonWriterFactory.createJsonWriter(writer, 2);) {
            NormalizedNodeStreamWriter jsonStreamWriter = JSONNormalizedNodeStreamWriter.createExclusiveWriter(
                    codecFactory, scPath.getParent(), scPath.getLastComponent().getNamespace(), jsonWriter);
            try (NormalizedNodeWriter nodeWriter = NormalizedNodeWriter.forStreamWriter(jsonStreamWriter)) {
                nodeWriter.write(bindingDOMCodecServices.toNormalizedNode(id, object).getValue());
                nodeWriter.flush();
            }
            JsonObject asJsonObject = PARSER.parse(writer.toString()).getAsJsonObject();
            return new Gson().toJson(asJsonObject);
        } catch (IOException e) {
            throw new GnpyException("Cannot convert data to Json string", e);
        }
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
