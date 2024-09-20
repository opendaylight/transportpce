/*
 * Copyright © 2021 Nokia, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.tapi.topology;

import java.util.List;
import java.util.Optional;
import org.opendaylight.mdsal.binding.api.DataObjectModification;
import org.opendaylight.mdsal.binding.api.DataTreeChangeListener;
import org.opendaylight.mdsal.binding.api.DataTreeModification;
import org.opendaylight.transportpce.common.StringConstants;
import org.opendaylight.yang.gen.v1.urn.opendaylight.netconf.device.rev240611.ConnectionOper.ConnectionStatus;
import org.opendaylight.yang.gen.v1.urn.opendaylight.netconf.device.rev240611.connection.oper.available.capabilities.AvailableCapability;
import org.opendaylight.yang.gen.v1.urn.opendaylight.netconf.node.topology.rev240911.NetconfNodeAugment;
import org.opendaylight.yang.gen.v1.urn.opendaylight.netconf.node.topology.rev240911.netconf.node.augment.NetconfNode;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TapiNetconfTopologyListener implements DataTreeChangeListener<Node> {

    private static final Logger LOG = LoggerFactory.getLogger(TapiNetconfTopologyListener.class);
    private final TapiNetworkModelService tapiNetworkModelService;

    public TapiNetconfTopologyListener(final TapiNetworkModelService tapiNetworkModelService) {
        this.tapiNetworkModelService = tapiNetworkModelService;
    }

    @Override
    public void onDataTreeChanged(List<DataTreeModification<Node>> changes) {
        LOG.info("onDataTreeChanged - {}", this.getClass().getSimpleName());
        for (DataTreeModification<Node> change : changes) {
            DataObjectModification<Node> rootNode = change.getRootNode();
            if (rootNode.dataBefore() == null) {
                continue;
            }
            String nodeId = rootNode.dataBefore().key().getNodeId().getValue();
            NetconfNode netconfNodeBefore = rootNode.dataBefore().augmentation(NetconfNodeAugment.class)
                    .getNetconfNode();
            switch (rootNode.modificationType()) {
                case DELETE:
                    this.tapiNetworkModelService.deleteTapinode(nodeId);
                    // TODO -> unregistration to NETCONF stream not yet supported
                    // onDeviceDisConnected(nodeId);
                    LOG.info("Device {} correctly disconnected from controller", nodeId);
                    break;
                case SUBTREE_MODIFIED:
                    NetconfNode netconfNodeAfter = rootNode.dataAfter().augmentation(NetconfNodeAugment.class)
                            .getNetconfNode();
                    if (ConnectionStatus.Connecting.equals(netconfNodeBefore.getConnectionStatus())
                            && ConnectionStatus.Connected.equals(netconfNodeAfter.getConnectionStatus())) {
                        LOG.info("Connecting Node: {}", nodeId);
                        Optional<AvailableCapability> deviceCapabilityOpt = netconfNodeAfter
                            .getAvailableCapabilities().getAvailableCapability().stream()
                            .filter(cp -> cp.getCapability().contains(StringConstants.OPENROADM_DEVICE_MODEL_NAME))
                            .sorted((c1, c2) -> c2.getCapability().compareTo(c1.getCapability()))
                            .findFirst();
                        if (deviceCapabilityOpt.isEmpty()) {
                            LOG.error("Unable to get openroadm-device-capability");
                            return;
                        }
                        // TODO -> subscription to NETCONF stream not yet supported... no listeners implementation
                        // onDeviceConnected(nodeId,deviceCapabilityOpt.get().getCapability());
                        LOG.info("Device {} waiting for portmapping to be populated", nodeId);
                    }
                    if (ConnectionStatus.Connected.equals(netconfNodeBefore.getConnectionStatus())
                            && ConnectionStatus.Connecting.equals(netconfNodeAfter.getConnectionStatus())) {
                        LOG.warn("Node: {} is being disconnected", nodeId);
                    }
                    break;
                default:
                    LOG.debug("Unknown modification type {}", rootNode.modificationType().name());
                    break;
            }
        }
    }
}
