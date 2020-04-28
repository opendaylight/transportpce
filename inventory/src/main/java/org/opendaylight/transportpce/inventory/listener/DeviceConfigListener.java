/*
 * Copyright © 2017 AT&T and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.inventory.listener;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import org.opendaylight.mdsal.binding.api.DataObjectModification;
import org.opendaylight.mdsal.binding.api.DataObjectModification.ModificationType;
import org.opendaylight.mdsal.binding.api.DataTreeChangeListener;
import org.opendaylight.mdsal.binding.api.DataTreeModification;
import org.opendaylight.transportpce.common.StringConstants;
import org.opendaylight.transportpce.inventory.DeviceInventory;
import org.opendaylight.yang.gen.v1.urn.opendaylight.netconf.node.topology.rev150114.NetconfNode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.netconf.node.topology.rev150114.NetconfNodeConnectionStatus;
import org.opendaylight.yang.gen.v1.urn.opendaylight.netconf.node.topology.rev150114.NetconfNodeConnectionStatus.ConnectionStatus;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class implements the {@link DataTreeChangeListener} on a {@link Node}.
 * This listener should be registered on a netconf topology node.
 */
public class DeviceConfigListener implements DataTreeChangeListener<Node> {

    private static final Logger LOG = LoggerFactory.getLogger(DeviceConfigListener.class);
    private final DeviceInventory deviceInventory;

    /**
     * Default constructor invoked by blueprint injects {@link DeviceInventory} as a persistence layer.
     *
     * @param deviceInventory reference to the {@link DeviceInventory}
     */
    public DeviceConfigListener(DeviceInventory deviceInventory) {
        this.deviceInventory = deviceInventory;
    }

    @Override
    public void onDataTreeChanged(Collection<DataTreeModification<Node>> changes) {

        //LOG.info("################testing np1:"+changes.toString());
        String openROADMversion = "";
        List<DataTreeModification<Node>> changesWithoutDefaultNetconfNode = getRealDevicesOnly(changes);
        for (DataTreeModification<Node> device : changesWithoutDefaultNetconfNode) {
            DataObjectModification<Node> rootNode = device.getRootNode();
            String nodeId = rootNode.getDataAfter().key().getNodeId().getValue();

            LOG.info("################nodeId {}", nodeId);

            NetconfNode netconfNode = rootNode.getDataAfter().augmentation(NetconfNode.class);
            NetconfNodeConnectionStatus.ConnectionStatus connectionStatus =
                    netconfNode.getConnectionStatus();
            long count = netconfNode.getAvailableCapabilities().getAvailableCapability().stream()
                    .filter(cp -> cp.getCapability().contains(StringConstants.OPENROADM_DEVICE_MODEL_NAME))
                    .count();
            LOG.info("################## DCL Modification Type {}",
                device.getRootNode().getModificationType().toString());
            LOG.info("################## DCL Capability Count {}", count);
            LOG.info("################## DCL Connection Status {}", connectionStatus);
            if (isCreate(device) || isUpdate(device)) {
                LOG.info("Node {} was modified", nodeId);
                try {
                    processModifiedSubtree(nodeId, netconfNode, openROADMversion);
                } catch (InterruptedException | ExecutionException e) {
                    LOG.error(e.getMessage(), e);
                }

            } else if (isDelete(device)) {
                LOG.info("Node {} was deleted", nodeId);
            }
        }
    }

    /**
     * Handles the {@link ModificationType#SUBTREE_MODIFIED} case.
     * If the changed node has.
     *
     * @param nodeId      device id
     * @param netconfNode netconf node
     * @throws InterruptedException may be thrown if there is a problem getting the device from
     *                              datastore
     * @throws ExecutionException   may be thrown if there is a problem getting the device from datastore
     */
    private void processModifiedSubtree(String nodeId, NetconfNode netconfNode, String openROADMversion)
            throws InterruptedException, ExecutionException {
        NetconfNodeConnectionStatus.ConnectionStatus connectionStatus = netconfNode.getConnectionStatus();
        /*long count = netconfNode.getAvailableCapabilities().getAvailableCapability().stream()
                .filter(cp -> cp.getCapability().contains(StringConstants.OPENROADM_DEVICE_MODEL_NAME)).count();
        if (count < 1) {
            LOG.info("No {} capable device was found", StringConstants.OPENROADM_DEVICE_MODEL_NAME);
            return;
        } */
        if (ConnectionStatus.Connected.equals(connectionStatus)) {
            LOG.info("DCL The device is in {} state", connectionStatus);
            deviceInventory.initializeDevice(nodeId, openROADMversion);
        } else if (ConnectionStatus.Connecting.equals(connectionStatus)
                || ConnectionStatus.UnableToConnect.equals(connectionStatus)) {
            LOG.info("DCL The device is in {} state", connectionStatus);
        } else {
            LOG.warn("DCL Invalid connection status {}", connectionStatus);
        }
    }

    /**
     * Filters the {@link StringConstants#DEFAULT_NETCONF_NODEID} nodes from the provided {@link Collection}.
     *
     */
    private static List<DataTreeModification<Node>> getRealDevicesOnly(Collection<DataTreeModification<Node>> changes) {
        return changes.stream()
                .filter(change -> (change.getRootNode().getDataAfter() != null
                        && !StringConstants.DEFAULT_NETCONF_NODEID
                        .equalsIgnoreCase(change.getRootNode().getDataAfter().key().getNodeId().getValue())
                        && change.getRootNode().getDataAfter().augmentation(NetconfNode.class) != null)
                        || (change.getRootNode().getDataBefore() != null
                        && !StringConstants.DEFAULT_NETCONF_NODEID.equalsIgnoreCase(
                        change.getRootNode().getDataBefore().key().getNodeId().getValue())
                        && change.getRootNode().getDataBefore().augmentation(NetconfNode.class) != null

                )).collect(Collectors.toList());
    }

    /**
     * In the filtered collection checks if the change is a new write.
     *
     */
    private static boolean isCreate(DataTreeModification<Node> change) {
        return change.getRootNode().getDataBefore() == null && change.getRootNode().getDataAfter() != null
                && ModificationType.WRITE.equals(change.getRootNode().getModificationType());
    }

    /**
     * In the filtered collection checks if the modification is update.
     *
     */
    private static boolean isUpdate(DataTreeModification<Node> change) {
        return ModificationType.SUBTREE_MODIFIED.equals(change.getRootNode().getModificationType());
    }

    /**
     * In the filtered collection checks if the node was deleted.
     *
     */
    private static boolean isDelete(DataTreeModification<Node> change) {
        return change.getRootNode().getDataBefore() != null && change.getRootNode().getDataAfter() == null
                && ModificationType.DELETE.equals(change.getRootNode().getModificationType());
    }
}
