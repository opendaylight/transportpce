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

import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
import java.util.Optional;

import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.platform.rev180130.platform.component.top.Components;
import org.opendaylight.yang.gen.v1.http.org.openroadm.pm.rev181019.CurrentPmList;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.fd.honeycomb.transportpce.binding.converter.XMLDataObjectConverter;
import io.fd.honeycomb.transportpce.test.common.DataStoreContext;

/**
 * Factory for creating the default device from the XML stored in classpath.
 *
 * @authors Gilles THOUENON and Christophe BETOULE ( gilles.thouenon@orange.com,
 *          christophe.betoule@orange.com )
 */
public class DefaultOcPlatformFactory {

    private static final Logger LOG = LoggerFactory.getLogger(DefaultOcPlatformFactory.class);

    /**
     * Returns a new instance of {@link CurrentPmlist} from the loaded XML stored in
     * File.
     *
     * @return {@link CurrentPmlist}
     */
    public Components createDefaultComponents(DataStoreContext dataStoreContextUtil, File oc_platform_data) {
        Components result = null;
        if (oc_platform_data.exists()) {
            String oper = oc_platform_data.getName();
            LOG.info("file '{}' exists at location : {}", oper, oc_platform_data.getAbsolutePath());
            InputStream targetStream;
            try {
                targetStream = new FileInputStream(oc_platform_data);
                Optional<NormalizedNode<?, ?>> transformIntoNormalizedNode = null;
                transformIntoNormalizedNode = XMLDataObjectConverter.createWithDataStoreUtil(dataStoreContextUtil)
                        .transformIntoNormalizedNode(targetStream);
                if (!transformIntoNormalizedNode.isPresent()) {
                    throw new IllegalStateException(
                            String.format("Could not transform the input %s into normalized nodes", oper));
                }
                Optional<DataObject> dataObject = XMLDataObjectConverter.createWithDataStoreUtil(dataStoreContextUtil)
                        .getDataObject(transformIntoNormalizedNode.get(), Components.QNAME);
                if (!dataObject.isPresent()) {
                    LOG.warn("Could not transform normalized nodes into data object");
                    return null;
                }
                result = (Components) dataObject.get();
            } catch (FileNotFoundException e) {
                LOG.error("File not found : {} at {}", e.getMessage(), e.getLocalizedMessage());
            } catch (IllegalStateException e) {
                LOG.warn("Could not transform the input OCPlatform into normalized nodes");
                return null;
            }
        } else {
            LOG.info("xml file not existed at : '{}'", oc_platform_data.getAbsolutePath());
        }
        return result;
    }

    /**
     * Returns a new instance of {@link CurrentPmlist} from the loaded XML stored in
     * String.
     *
     * @return {@link CurrentPmlist}
     */
    public Components createDefaultComponents(DataStoreContext dataStoreContextUtil, String oc_platform_data) {
        Components result = null;
        if (oc_platform_data != null) {
            LOG.info("openconfig platform data string is ok ");
            LOG.info(oc_platform_data);
            InputStream targetStream;
            try {
                targetStream = new ByteArrayInputStream(oc_platform_data.getBytes());
                LOG.info("targetStream = {}", targetStream);
                Optional<NormalizedNode<?, ?>> transformIntoNormalizedNode = null;
                transformIntoNormalizedNode = XMLDataObjectConverter.createWithDataStoreUtil(dataStoreContextUtil)
                        .transformIntoNormalizedNode(targetStream);
                if (!transformIntoNormalizedNode.isPresent()) {
                    throw new IllegalStateException(
                            String.format("Could not transform the input into normalized nodes"));
                }
                Optional<DataObject> dataObject = XMLDataObjectConverter.createWithDataStoreUtil(dataStoreContextUtil)
                        .getDataObject(transformIntoNormalizedNode.get(), Components.QNAME);
                if (!dataObject.isPresent()) {
                    LOG.warn("Could not transform normalized nodes into data object");
                }
                result = (Components) dataObject.get();
            } catch (IllegalStateException e) {
                LOG.warn("Could not transform the input OCPlatform into normalized nodes");
                return null;
            }
        } else {
            LOG.info("openconfig platform data string is null!");
        }
        return result;
    }

    /**
     * create an XML String from an instance of {@link CurrentPmlist}.
     *
     */
    public void createXMLFromComponents(DataStoreContext dataStoreContextUtil, Components components, String output) {
        if (components != null) {
            Optional<NormalizedNode<?, ?>> transformIntoNormalizedNode = null;
            transformIntoNormalizedNode = XMLDataObjectConverter.createWithDataStoreUtil(dataStoreContextUtil)
                    .toNormalizedNodes(components, Components.class);
            if (!transformIntoNormalizedNode.isPresent()) {
                throw new IllegalStateException(
                        String.format("Could not transform the input %s into normalized nodes", components));
            }
            XMLDataObjectConverter createWithDataStoreUtil = XMLDataObjectConverter
                    .createWithDataStoreUtil(dataStoreContextUtil);
            Writer writerFromDataObject = createWithDataStoreUtil.writerFromDataObject(components, Components.class,
                    createWithDataStoreUtil.dataContainer());
            try {
                BufferedWriter writer = new BufferedWriter(new FileWriter(output));
                writer.write(writerFromDataObject.toString());
                writer.close();
            } catch (IOException e) {
                LOG.error("Bufferwriter error ");
            }
            LOG.info("openconf platform xml : {}", writerFromDataObject.toString());
        }
    }
}
