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

import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.terminal.device.rev170708.terminal.device.top.TerminalDevice;
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
public class DefaultOcTerminalDeviceFactory {

    private static final Logger LOG = LoggerFactory.getLogger(DefaultOcTerminalDeviceFactory.class);

    /**
     * Returns a new instance of {@link TerminalDevice} from the loaded XML stored in
     * File.
     *
     * @return {@link TerminalDevice}
     */
    public TerminalDevice createDefaultTerminalDevice(DataStoreContext dataStoreContextUtil, File oc_terminal_device_data) {
        TerminalDevice result = null;
        if (oc_terminal_device_data.exists()) {
            String oper = oc_terminal_device_data.getName();
            LOG.info("file '{}' exists at location : {}", oper, oc_terminal_device_data.getAbsolutePath());
            InputStream targetStream;
            try {
                targetStream = new FileInputStream(oc_terminal_device_data);
                Optional<NormalizedNode<?, ?>> transformIntoNormalizedNode = null;
                transformIntoNormalizedNode = XMLDataObjectConverter.createWithDataStoreUtil(dataStoreContextUtil)
                        .transformIntoNormalizedNode(targetStream);
                if (!transformIntoNormalizedNode.isPresent()) {
                    throw new IllegalStateException(
                            String.format("Could not transform the input %s into normalized nodes", oper));
                }
                Optional<DataObject> dataObject = XMLDataObjectConverter.createWithDataStoreUtil(dataStoreContextUtil)
                        .getDataObject(transformIntoNormalizedNode.get(), TerminalDevice.QNAME);
                if (!dataObject.isPresent()) {
                    LOG.warn("Could not transform normalized nodes into data object");
                    return null;
                }
                result = (TerminalDevice) dataObject.get();
            } catch (FileNotFoundException e) {
                LOG.error("File not found : {} at {}", e.getMessage(), e.getLocalizedMessage());
            } catch (IllegalStateException e) {
                LOG.warn("Could not transform the input OcTerminalDevice into normalized nodes");
                return null;
            }
        } else {
            LOG.info("xml file not existed at : '{}'", oc_terminal_device_data.getAbsolutePath());
        }
        return result;
    }

    /**
     * Returns a new instance of {@link TerminalDevice} from the loaded XML stored in
     * String.
     *
     * @return {@link TerminalDevice}
     */
    public TerminalDevice createDefaultTerminalDevice(DataStoreContext dataStoreContextUtil, String oc_terminal_device_data) {
        TerminalDevice result = null;
        if (oc_terminal_device_data != null) {
            LOG.info("openconfig-terminal-device data string is ok ");
            InputStream targetStream;
            try {
                targetStream = new ByteArrayInputStream(oc_terminal_device_data.getBytes());
                Optional<NormalizedNode<?, ?>> transformIntoNormalizedNode = null;
                transformIntoNormalizedNode = XMLDataObjectConverter.createWithDataStoreUtil(dataStoreContextUtil)
                        .transformIntoNormalizedNode(targetStream);
                if (!transformIntoNormalizedNode.isPresent()) {
                    throw new IllegalStateException(
                            String.format("Could not transform the input %s into normalized nodes"));
                }
                Optional<DataObject> dataObject = XMLDataObjectConverter.createWithDataStoreUtil(dataStoreContextUtil)
                        .getDataObject(transformIntoNormalizedNode.get(), TerminalDevice.QNAME);
                if (!dataObject.isPresent()) {
                    LOG.warn("Could not transform normalized nodes into data object");
                }
                result = (TerminalDevice) dataObject.get();
            } catch (IllegalStateException e) {
                LOG.warn("Could not transform the input OcTerminalDevice into normalized nodes");
                return null;
            }
        } else {
            LOG.info("openconfig-terminal-device data string is null!");
        }
        return result;
    }

    /**
     * create an XML String from an instance of {@link TerminalDevice}.
     *
     */
    public void createXMLFromTerminalDevice(DataStoreContext dataStoreContextUtil, TerminalDevice terminalDevice, String output) {
        if (terminalDevice != null) {
            Optional<NormalizedNode<?, ?>> transformIntoNormalizedNode = null;
            transformIntoNormalizedNode = XMLDataObjectConverter.createWithDataStoreUtil(dataStoreContextUtil)
                    .toNormalizedNodes(terminalDevice, TerminalDevice.class);
            if (!transformIntoNormalizedNode.isPresent()) {
                throw new IllegalStateException(
                        String.format("Could not transform the input %s into normalized nodes", terminalDevice));
            }
            XMLDataObjectConverter createWithDataStoreUtil = XMLDataObjectConverter
                    .createWithDataStoreUtil(dataStoreContextUtil);
            Writer writerFromDataObject = createWithDataStoreUtil.writerFromDataObject(terminalDevice, TerminalDevice.class,
                    createWithDataStoreUtil.dataContainer());
            try {
                BufferedWriter writer = new BufferedWriter(new FileWriter(output));
                writer.write(writerFromDataObject.toString());
                writer.close();
            } catch (IOException e) {
                LOG.error("Bufferwriter error ");
            }
            LOG.info("openconfig-terminal-device xml : {}", writerFromDataObject.toString());
        }
    }
}
