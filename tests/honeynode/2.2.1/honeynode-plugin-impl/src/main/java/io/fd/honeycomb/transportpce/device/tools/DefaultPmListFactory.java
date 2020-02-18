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
 * @author Martial COULIBALY ( martial.coulibaly@gfi.com ) on behalf of Orange
 */
public class DefaultPmListFactory {

    private static final Logger LOG = LoggerFactory.getLogger(DefaultPmListFactory.class);

    /**
     * Returns a new instance of {@link CurrentPmlist} from the loaded XML stored in
     * File.
     *
     * @return {@link CurrentPmlist}
     */
    public CurrentPmList createDefaultPmList(DataStoreContext dataStoreContextUtil, File pm_data_config) {
        CurrentPmList result = null;
        if (pm_data_config.exists()) {
            String config = pm_data_config.getName();
            LOG.info("file '{}' exists at location : {}", config, pm_data_config.getAbsolutePath());
            InputStream targetStream;
            try {
                targetStream = new FileInputStream(pm_data_config);
                Optional<NormalizedNode<?, ?>> transformIntoNormalizedNode = null;
                transformIntoNormalizedNode = XMLDataObjectConverter.createWithDataStoreUtil(dataStoreContextUtil)
                        .transformIntoNormalizedNode(targetStream);
                if (!transformIntoNormalizedNode.isPresent()) {
                    throw new IllegalStateException(
                            String.format("Could not transform the input %s into normalized nodes", config));
                }
                Optional<DataObject> dataObject = XMLDataObjectConverter.createWithDataStoreUtil(dataStoreContextUtil)
                        .getDataObject(transformIntoNormalizedNode.get(), CurrentPmList.QNAME);
                if (!dataObject.isPresent()) {
                    LOG.warn("Could not transform normalized nodes into data object");
                    return null;
                }
                result = (CurrentPmList) dataObject.get();
            } catch (FileNotFoundException e) {
                LOG.error("File not found : {} at {}", e.getMessage(), e.getLocalizedMessage());
            } catch (IllegalStateException e) {
                LOG.warn("Could not transform the input PM into normalized nodes");
            }
        } else {
            LOG.info("xml file not existed at : '{}'", pm_data_config.getAbsolutePath());
        }
        return result;
    }

    /**
     * Returns a new instance of {@link CurrentPmlist} from the loaded XML stored in
     * String.
     *
     * @return {@link CurrentPmlist}
     */
    public CurrentPmList createDefaultPmList(DataStoreContext dataStoreContextUtil, String pm_data_config) {
        CurrentPmList result = null;
        if (pm_data_config != null) {
            LOG.info("Pm List data config string is ok ");
            InputStream targetStream;
            try {
                targetStream = new ByteArrayInputStream(pm_data_config.getBytes());
                Optional<NormalizedNode<?, ?>> transformIntoNormalizedNode = null;
                transformIntoNormalizedNode = XMLDataObjectConverter.createWithDataStoreUtil(dataStoreContextUtil)
                        .transformIntoNormalizedNode(targetStream);
                if (!transformIntoNormalizedNode.isPresent()) {
                    throw new IllegalStateException(
                            String.format("Could not transform the input %s into normalized nodes"));
                }
                Optional<DataObject> dataObject = XMLDataObjectConverter.createWithDataStoreUtil(dataStoreContextUtil)
                        .getDataObject(transformIntoNormalizedNode.get(), CurrentPmList.QNAME);
                if (!dataObject.isPresent()) {
                    LOG.warn("Could not transform normalized nodes into data object");
                    return null;
                }
                result = (CurrentPmList) dataObject.get();
            } catch (IllegalStateException e) {
                LOG.warn("Could not transform the input pm into normalized nodes");
            }
        } else {
            LOG.info("device data config string is null!");
        }
        return result;
    }

    /**
     * create an XML String from an instance of {@link CurrentPmlist}.
     *
     */
    public void createXMLFromPmList(DataStoreContext dataStoreContextUtil, CurrentPmList pm_list, String output) {
        if (pm_list != null) {
            Optional<NormalizedNode<?, ?>> transformIntoNormalizedNode = null;
            transformIntoNormalizedNode = XMLDataObjectConverter.createWithDataStoreUtil(dataStoreContextUtil)
                    .toNormalizedNodes(pm_list, CurrentPmList.class);
            if (!transformIntoNormalizedNode.isPresent()) {
                throw new IllegalStateException(
                        String.format("Could not transform the input %s into normalized nodes", pm_list));
            }
            XMLDataObjectConverter createWithDataStoreUtil = XMLDataObjectConverter
                    .createWithDataStoreUtil(dataStoreContextUtil);
            Writer writerFromDataObject = createWithDataStoreUtil.writerFromDataObject(pm_list, CurrentPmList.class,
                    createWithDataStoreUtil.dataContainer());
            try {
                BufferedWriter writer = new BufferedWriter(new FileWriter(output));
                writer.write(writerFromDataObject.toString());
                writer.close();
            } catch (IOException e) {
                LOG.error("Bufferwriter error ");
            }
            LOG.info("device xml : {}", writerFromDataObject.toString());
        }
    }
}
