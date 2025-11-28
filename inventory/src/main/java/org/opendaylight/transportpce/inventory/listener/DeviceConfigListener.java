/*
 * Copyright © 2017 AT&T and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.inventory.listener;

import java.util.List;
import org.opendaylight.mdsal.binding.api.DataObjectDeleted;
import org.opendaylight.mdsal.binding.api.DataObjectModification.WithDataAfter;
import org.opendaylight.mdsal.binding.api.DataTreeChangeListener;
import org.opendaylight.mdsal.binding.api.DataTreeModification;
import org.opendaylight.transportpce.common.StringConstants;
import org.opendaylight.transportpce.inventory.DeviceInventory;
import org.opendaylight.yang.gen.v1.urn.opendaylight.netconf.device.rev251028.ConnectionOper.ConnectionStatus;
import org.opendaylight.yang.gen.v1.urn.opendaylight.netconf.node.topology.rev251103.NetconfNodeAugment;
import org.opendaylight.yang.gen.v1.urn.opendaylight.netconf.node.topology.rev251103.netconf.node.augment.NetconfNode;
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
    public void onDataTreeChanged(List<DataTreeModification<Node>> changes) {
        for (DataTreeModification<Node> change : changes) {
            String nodeId = change.getRootNode().coerceKeyStep(Node.class).key().getNodeId().getValue();
            switch (change.getRootNode()) {
                case WithDataAfter<Node> present -> {
                    NetconfNode netconfNode = present.dataAfter().augmentation(NetconfNodeAugment.class)
                            .getNetconfNode();
                    LOG.info("Node {} was modified", nodeId);
                    processModifiedSubtree(nodeId, netconfNode);
                }
                case DataObjectDeleted<Node> deleted -> {
                    LOG.info("Node {} was deleted", nodeId);
                }
            }
        }
    }

    /**
     * Handles the {@link WithDataAfter} case.
     *
     * @param nodeId      device id
     * @param netconfNode netconf node
     */
    private void processModifiedSubtree(String nodeId, NetconfNode netconfNode) {
        ConnectionStatus connectionStatus = netconfNode.getConnectionStatus();
        if (netconfNode.getAvailableCapabilities().getAvailableCapability().stream()
                .noneMatch(cp -> cp.getCapability().contains(StringConstants.OPENROADM_DEVICE_MODEL_NAME))) {
            return;
        }
        LOG.info("DCL The device is in {} state", connectionStatus);
        if (ConnectionStatus.Connected.equals(connectionStatus)) {
            deviceInventory.initializeDevice(nodeId);
        }
    }
}
