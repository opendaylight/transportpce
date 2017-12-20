/*
 * Copyright © 2017 AT&T and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.inventory.job;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Strings;
import com.google.common.io.Files;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

import org.apache.karaf.scheduler.Job;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.mdsal.dom.api.DOMSchemaService;
import org.opendaylight.transportpce.common.InstanceIdentifiers;
import org.opendaylight.transportpce.common.Timeouts;
import org.opendaylight.transportpce.common.device.DeviceTransactionManager;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.org.openroadm.device.container.OrgOpenroadmDevice;
import org.opendaylight.yang.gen.v1.urn.opendaylight.netconf.node.topology.rev150114.NetconfNode;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class which periodically backups the device into a file implements {@link Job}
 * interface which is automatically registered via whitboard pattern into karaf
 *  environment. This job will persist an {@link OrgOpenroadmDevice} capable with
 *  the models provided in transportpce-models
 */
public class PeriodicDeviceBackupJob implements Runnable {

    private static final String TIMESTAMP_FORMAT = "yyyy-MM-dd_HH-mm-ss-SSS";
    private static final String ORG_OPENROADM_DEVICE = "org-openroadm-device";
    private static final Logger LOG = LoggerFactory.getLogger(PeriodicDeviceBackupJob.class);

    private final DataBroker dataBroker;
    private final DeviceTransactionManager deviceTransactionManager;

    private final SchemaContext schemaContext;
    private final SimpleDateFormat filenameFormatter;

    /**
     * Folder property, where the file is placed.
     */
    private String folder;
    /**
     * Prefix of device file.
     */
    private String filePrefix;

    /**
     * Constructor with injected fields.
     *
     * @param dataBroker the datatabroker
     * @param domSchemaService the DOM schema service
     * @param deviceTransactionManager the device transaction manager
     */
    public PeriodicDeviceBackupJob(DataBroker dataBroker, DOMSchemaService domSchemaService,
            DeviceTransactionManager deviceTransactionManager) {
        this.dataBroker = dataBroker;
        this.schemaContext = domSchemaService.getGlobalContext();
        this.deviceTransactionManager = deviceTransactionManager;
        this.filenameFormatter = new SimpleDateFormat(TIMESTAMP_FORMAT);
    }

    @Override
    public void run() {
        LOG.info("Running periodical device backup into {}", folder);
        try {
            backupAllDevices();
        } catch (InterruptedException | ExecutionException e) {
            LOG.warn("Unable to read netconf topology from the operational datastore", e);
        }
    }

    /**
     * Gets the folder.
     *
     * @return the folder
     */
    public String getFolder() {
        return folder;
    }

    /**
     * Sets the folder.
     *
     * @param folder the folder to set
     */
    public void setFolder(String folder) {
        this.folder = folder;
    }

    /**
     * Gets the filePrefix.
     *
     * @return the filePrefix
     */
    public String getFilePrefix() {
        return filePrefix;
    }

    /**
     * Sets the filePrefix.
     *
     * @param filePrefix the filePrefix to set
     */
    public void setFilePrefix(String filePrefix) {
        this.filePrefix = filePrefix;
    }

    /**
     * Stores all devices into files.
     *
     * @throws InterruptedException interrupted exception
     * @throws ExecutionException execution exception
     */
    private void backupAllDevices() throws InterruptedException, ExecutionException {
        ReadOnlyTransaction newReadOnlyTransaction = dataBroker.newReadOnlyTransaction();
        Optional<Topology> topology = newReadOnlyTransaction
                .read(LogicalDatastoreType.OPERATIONAL, InstanceIdentifiers.NETCONF_TOPOLOGY_II).get().toJavaUtil();
        if (!topology.isPresent()) {
            LOG.warn("Netconf topology was not found in datastore");
            return;
        }

        for (Node node : topology.get().getNode()) {
            String nodeId = getNodeId(node);
            if (Strings.isNullOrEmpty(nodeId)) {
                LOG.debug("{} node is not a roadm device");
                continue;
            }
            storeRoadmDevice(nodeId);
        }
    }

    /**
     * Stores a single ROADM device into a file.
     *
     * @param nodeId the node ID
     */
    private void storeRoadmDevice(String nodeId) {
        InstanceIdentifier<OrgOpenroadmDevice> deviceIi = InstanceIdentifier.create(OrgOpenroadmDevice.class);
        Optional<OrgOpenroadmDevice> deviceObject =
                deviceTransactionManager.getDataFromDevice(nodeId, LogicalDatastoreType.OPERATIONAL, deviceIi,
                        Timeouts.DEVICE_READ_TIMEOUT, Timeouts.DEVICE_READ_TIMEOUT_UNIT);
        if (!deviceObject.isPresent()) {
            LOG.warn("Device object {} is not present.", nodeId);
            return;
        }

        // TODO: Serialization should be done via XMLDataObjectConverter when devices use the same model as
        // this project
        /*
         * XMLDataObjectConverter createWithSchemaContext =
         * XMLDataObjectConverter.createWithSchemaContext(schemaContext, nodeSerializer); Writer
         * writerFromDataObject = createWithSchemaContext.writerFromDataObject(deviceObject.get(),
         * OrgOpenroadmDevice.class, createWithSchemaContext.dataContainer());
         */
        StringBuilder serializedDevice = new StringBuilder();
        try {
            serializedDevice.append(new ObjectMapper().writeValueAsString(deviceObject.get()));
        } catch (JsonProcessingException e1) {
            LOG.error(e1.getMessage(), e1);
        }

        String prepareFileName = prepareFileName(nodeId);
        File parent = new File(folder);
        if (!parent.exists() && !parent.isDirectory() && !parent.mkdirs()) {
            LOG.error("Could not create empty directory {}", folder);
            throw new IllegalStateException(String.format("Could not create empty directory %s", folder));
        }

        File file = new File(parent, filePrefix.concat(prepareFileName));
        if (file.exists()) {
            throw new IllegalStateException(String.format("The file %s already exists", file));
        }
        try {
            Files.asCharSink(file, StandardCharsets.UTF_8).write(serializedDevice.toString());
        } catch (IOException e) {
            LOG.error("Could not write device backup into file {}", file);
        }
    }

    private String prepareFileName(String nodeId) {
        String format = filenameFormatter.format(new Date());
        StringBuilder sb = new StringBuilder(format);
        sb.append("__").append(nodeId).append("__").append(".xml");
        return sb.toString();
    }

    /**
     * If the {@link Node} in the {@link Topology} is a {@link NetconfNode}, has
     * capabilities of {@link PeriodicDeviceBackupJob#ORG_OPENROADM_DEVICE} and the
     * key is not null returns the identifier of {@link OrgOpenroadmDevice} node.
     *
     * @param node inside the {@link Topology}
     * @return node key
     */
    private static String getNodeId(Node node) {
        if (node == null) {
            LOG.trace("The node is null");
            return "";
        }
        NetconfNode netconfNode = node.getAugmentation(NetconfNode.class);
        if (netconfNode == null) {
            LOG.trace("The node {} has not properties of NetconfNode", node);
            return "";
        }
        if (netconfNode.getAvailableCapabilities() == null
                || netconfNode.getAvailableCapabilities().getAvailableCapability() == null) {
            LOG.trace("No available capabilities");
            return "";
        }
        long count = netconfNode.getAvailableCapabilities().getAvailableCapability().stream()
                .filter(cp -> cp.getCapability() != null && cp.getCapability().contains(ORG_OPENROADM_DEVICE)).count();
        if (count < 1) {
            LOG.trace("The node {} has not capabilities of OpenROADMDevice", node);
            return "";
        }
        if (node.getKey() == null || node.getKey().getNodeId() == null) {
            LOG.trace("Node {} has invalid key", node);
            return "";
        }
        if ("controller-config".equalsIgnoreCase(node.getKey().getNodeId().getValue())) {
            LOG.info("Got controller-config instead of roadm-node");
            return "";
        }
        return node.getKey().getNodeId().getValue();
    }

}
