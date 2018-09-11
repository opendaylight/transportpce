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

import io.fd.honeycomb.transportpce.binding.converter.XMLDataObjectConverter;
import io.fd.honeycomb.transportpce.test.common.DataStoreContext;

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

import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.netmod.notification.rev080714.Netconf;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Factory for creating the {@link Netconf} from the XML stored in
 * classpath.
 *
 * @author Martial COULIBALY ( martial.coulibaly@gfi.com ) on behalf of Orange
 */
public class DefaultNetconfFactory {

    private static final Logger LOG = LoggerFactory.getLogger(DefaultNetconfFactory.class);

    /**
     * Returns a new instance of {@link Netconf} from the loaded XML stored
     * in File.
     *
     * @return {@link Netconf}
     */
    public Netconf createDefaultNetconf(DataStoreContext dataStoreContextUtil, File netconf_data_config) {
        Netconf result = null;
        if (netconf_data_config.exists()) {
            String config = netconf_data_config.getName();
            LOG.info("file '{}' exists at location : {}", config, netconf_data_config.getAbsolutePath());
            InputStream targetStream;
            try {
                targetStream = new FileInputStream(netconf_data_config);
                Optional<NormalizedNode<?, ?>> transformIntoNormalizedNode = null;
                transformIntoNormalizedNode =
                        XMLDataObjectConverter.createWithDataStoreUtil(dataStoreContextUtil)
                        .transformIntoNormalizedNode(targetStream);
                if (!transformIntoNormalizedNode.isPresent()) {
                    throw new IllegalStateException(
                            String.format("Could not transform the input %s into normalized nodes",
                            config));
                }
                Optional<DataObject> dataObject = XMLDataObjectConverter.createWithDataStoreUtil(dataStoreContextUtil)
                        .getDataObject(transformIntoNormalizedNode.get(), Netconf.QNAME);
                if (!dataObject.isPresent()) {
                    throw new IllegalStateException("Could not transform normalized nodes into data object");
                }
                result =  (Netconf) dataObject.get();
            } catch (FileNotFoundException | IllegalStateException e) {
                LOG.error("could not get Netconf !");
            }
        } else {
            LOG.info("netconf file not existed at : '{}'", netconf_data_config.getAbsolutePath());
        }
        return result;
    }
    /**
     * Returns a new instance of {@link Netconf} from the loaded XML stored
     * in String.
     *
     * @return {@link Netconf}
     */
    public Netconf createDefaultNetconf(DataStoreContext dataStoreContextUtil, String netconf_data_config) {
        Netconf result = null;
        if (netconf_data_config != null) {
            LOG.info("Netconf data config string is ok ");
            InputStream targetStream;
            targetStream = new ByteArrayInputStream(netconf_data_config.getBytes());
            Optional<NormalizedNode<?, ?>> transformIntoNormalizedNode = null;
            transformIntoNormalizedNode =
                    XMLDataObjectConverter.createWithDataStoreUtil(dataStoreContextUtil)
                    .transformIntoNormalizedNode(targetStream);
            if (!transformIntoNormalizedNode.isPresent()) {
                throw new IllegalStateException(
                        String.format("Could not transform the input %s into normalized nodes"));
            }
            Optional<DataObject> dataObject = XMLDataObjectConverter.createWithDataStoreUtil(dataStoreContextUtil)
                    .getDataObject(transformIntoNormalizedNode.get(), Netconf.QNAME);
            if (!dataObject.isPresent()) {
                throw new IllegalStateException("Could not transform normalized nodes into data object");
            }
            result =  (Netconf) dataObject.get();
        } else {
            LOG.info("netconf data config string is null!");
        }
        return result;
    }


    /**
     * create an XML String from an instance of {@link Netconf}.
     *
     */
    public void createXMLFromNetconf(DataStoreContext dataStoreContextUtil, Netconf netconf, String output) {
        if (netconf != null) {
            Optional<NormalizedNode<?, ?>> transformIntoNormalizedNode = null;
            transformIntoNormalizedNode =
                    XMLDataObjectConverter.createWithDataStoreUtil(dataStoreContextUtil)
                    .toNormalizedNodes(netconf, Netconf.class);
            if (!transformIntoNormalizedNode.isPresent()) {
                throw new IllegalStateException(String.format("Could not transform the input %s into normalized nodes",
                        netconf));
            }
            XMLDataObjectConverter createWithDataStoreUtil = XMLDataObjectConverter
                    .createWithDataStoreUtil(dataStoreContextUtil);
            Writer writerFromDataObject =
                    createWithDataStoreUtil.writerFromDataObject(netconf, Netconf.class,
                            createWithDataStoreUtil.dataContainer());
            try {
                BufferedWriter writer = new BufferedWriter(new FileWriter(output));
                writer.write(writerFromDataObject.toString());
                writer.close();
            } catch (IOException e) {
                LOG.error("Bufferwriter error ");
            }
            LOG.info("netconf xml : {}", writerFromDataObject.toString());
        }
    }
}
