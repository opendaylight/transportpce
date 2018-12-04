/*
 * Copyright © 2016 AT&T and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.networkmodel.util;

import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.transportpce.common.NetworkUtils;
import org.opendaylight.transportpce.common.Timeouts;
import org.opendaylight.transportpce.common.device.DeviceTransactionManager;
import org.opendaylight.yang.gen.v1.http.org.openroadm.clli.network.rev170626.NetworkTypes1;
import org.opendaylight.yang.gen.v1.http.org.openroadm.clli.network.rev170626.NetworkTypes1Builder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.clli.network.rev170626.Node1;
import org.opendaylight.yang.gen.v1.http.org.openroadm.clli.network.rev170626.Node1Builder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.clli.network.rev170626.network.network.types.ClliNetworkBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.org.openroadm.device.container.OrgOpenroadmDevice;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.org.openroadm.device.container.org.openroadm.device.Info;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev150608.Network;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev150608.NetworkBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev150608.NetworkId;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev150608.NetworkKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev150608.NodeId;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev150608.network.NetworkTypesBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev150608.network.Node;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev150608.network.NodeBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev150608.network.NodeKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier.InstanceIdentifierBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ClliNetwork {

    private static final Logger LOG = LoggerFactory.getLogger(ClliNetwork.class);

    private ClliNetwork() {
        // utility class
    }

    /**
     * This public method creates the CLLI Layer and posts it to the controller.
     *
     * @param controllerdb   controller Databroker
     */
    public static void createClliLayer(DataBroker controllerdb) {
        try {
            Network clliNetwork = createNetwork();
            InstanceIdentifierBuilder<Network> nwIID = InstanceIdentifier.builder(
                       Network.class,new NetworkKey(new NetworkId(NetworkUtils.CLLI_NETWORK_ID)));
            WriteTransaction wrtx = controllerdb.newWriteOnlyTransaction();
            wrtx.put(LogicalDatastoreType.CONFIGURATION, nwIID.build(), clliNetwork);
            wrtx.submit().get(1, TimeUnit.SECONDS);
            LOG.info("CLLI-Network created successfully.");
        } catch (ExecutionException | TimeoutException | InterruptedException e) {
            LOG.warn("Failed to create CLLI-Network", e);
        }
    }

    /**
     * Create single node entry for CLLI topology.
     *
     * @param deviceTransactionManager device transation manager
     * @param deviceId device ID
     *
     * @return node builder status
     */
    public static Node createNode(DeviceTransactionManager deviceTransactionManager, String deviceId) {
        //Read clli from the device
        InstanceIdentifier<Info> infoIID = InstanceIdentifier.create(OrgOpenroadmDevice.class).child(Info.class);
        Optional<Info> deviceInfo = deviceTransactionManager.getDataFromDevice(deviceId,
                LogicalDatastoreType.OPERATIONAL, infoIID, Timeouts.DEVICE_READ_TIMEOUT,
                Timeouts.DEVICE_READ_TIMEOUT_UNIT);
        String clli;
        if (deviceInfo.isPresent()) {
            clli = deviceInfo.get().getClli();
        } else {
            return null;
        }
        /*
         * Create node in the CLLI layer of the network model
         * with nodeId equal to the clli attribute in the device
         * model's info subtree
         */
        NodeBuilder nodeBldr = new NodeBuilder();
        NodeId nwNodeId = new NodeId(clli);
        nodeBldr.setNodeId(nwNodeId);
        nodeBldr.withKey(new NodeKey(nwNodeId));
        /*
         * create clli node augmentation
         * defined in openroadm-clli-network.yang
         */
        Node1Builder clliAugmentationBldr = new Node1Builder();
        clliAugmentationBldr.setClli(clli);
        nodeBldr.addAugmentation(Node1.class, clliAugmentationBldr.build());
        return nodeBldr.build();
    }

    /**
     * Create empty CLLI network.
     */
    private static Network createNetwork() {
        NetworkBuilder nwBuilder = new NetworkBuilder();
        NetworkId nwId = new NetworkId(NetworkUtils.CLLI_NETWORK_ID);
        nwBuilder.setNetworkId(nwId);
        nwBuilder.withKey(new NetworkKey(nwId));
        //set network type to clli
        NetworkTypes1Builder clliNetworkTypesBldr = new NetworkTypes1Builder();
        clliNetworkTypesBldr.setClliNetwork(new ClliNetworkBuilder().build());
        NetworkTypesBuilder nwTypeBuilder = new NetworkTypesBuilder();
        nwTypeBuilder.addAugmentation(NetworkTypes1.class, clliNetworkTypesBldr.build());
        nwBuilder.setNetworkTypes(nwTypeBuilder.build());
        return nwBuilder.build();
    }
}
