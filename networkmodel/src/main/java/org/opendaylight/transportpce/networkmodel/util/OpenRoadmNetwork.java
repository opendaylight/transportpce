/*
 * Copyright © 2016 AT&T and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.networkmodel.util;

import com.google.common.collect.ImmutableList;

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
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.types.rev161014.NodeTypes;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.org.openroadm.device.container.OrgOpenroadmDevice;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.org.openroadm.device.container.org.openroadm.device.Info;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.rev170929.NetworkTypes1;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.rev170929.NetworkTypes1Builder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.rev170929.Node1;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.rev170929.Node1Builder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.rev170929.network.network.types.OpenroadmNetworkBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.types.rev170929.OpenroadmNodeType;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev150608.Network;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev150608.NetworkBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev150608.NetworkId;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev150608.NetworkKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev150608.NodeId;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev150608.network.NetworkTypesBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev150608.network.Node;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev150608.network.NodeBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev150608.network.NodeKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev150608.network.node.SupportingNodeBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev150608.network.node.SupportingNodeKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier.InstanceIdentifierBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class OpenRoadmNetwork {

    private static final Logger LOG = LoggerFactory.getLogger(OpenRoadmNetwork.class);

    private OpenRoadmNetwork() {
        // utility class
    }

    /**
     * This public method creates the OpenRoadmNetwork Layer and posts it to the
     * controller.
     */
    public static void createOpenRoadmNetworkLayer(DataBroker controllerdb) {
        try {
            Network openRoadmNetwork = createOpenRoadmNetwork();
            InstanceIdentifierBuilder<Network> nwIID = InstanceIdentifier.builder(Network.class,
                    new NetworkKey(new NetworkId(NetworkUtils.UNDERLAY_NETWORK_ID)));
            WriteTransaction wrtx = controllerdb.newWriteOnlyTransaction();
            wrtx.put(LogicalDatastoreType.CONFIGURATION, nwIID.build(), openRoadmNetwork);
            wrtx.submit().get(1, TimeUnit.SECONDS);
            LOG.info("OpenRoadm-Network created successfully.");
        } catch (ExecutionException | TimeoutException | InterruptedException e) {
            LOG.warn("Failed to create OpenRoadm-Network", e);
        }
    }

    /**
     * Create single node entry for OpenRoadmNetwork.
     *
     */
    public static Node createNode(String nodeId, DeviceTransactionManager deviceTransactionManager) {
        // Fetches the info from the deviceInfo
        InstanceIdentifier<Info> infoIID = InstanceIdentifier.create(OrgOpenroadmDevice.class).child(Info.class);
        Optional<Info> deviceInfoOpt = deviceTransactionManager.getDataFromDevice(nodeId,
                LogicalDatastoreType.OPERATIONAL, infoIID, Timeouts.DEVICE_READ_TIMEOUT,
                Timeouts.DEVICE_READ_TIMEOUT_UNIT);
        Info deviceInfo;
        if (deviceInfoOpt.isPresent()) {
            deviceInfo = deviceInfoOpt.get();
        } else {
            LOG.error("Unable to get device info from device {}!", nodeId);
            return null;
        }
        NodeTypes nodeType = deviceInfo.getNodeType();

        // Uses the Node Builder to set the nodeId and Key
        NodeBuilder nodeBldr = new NodeBuilder();
        NodeId nwNodeId = new NodeId(nodeId);
        nodeBldr.setNodeId(nwNodeId);
        nodeBldr.setKey(new NodeKey(nwNodeId));
        Node1Builder node1bldr = new Node1Builder();

        /*
         * Recognize the node type: 1:ROADM, 2:XPONDER
         */
        switch (nodeType.getIntValue()) {
            case 1:
                node1bldr.setNodeType(OpenroadmNodeType.ROADM);
                break;
            case 2:
                node1bldr.setNodeType(OpenroadmNodeType.XPONDER);
                break;
            default:
                LOG.error("No correponsding type for the value: {}", nodeType.getIntValue());
                break;
        }

        String vendor = deviceInfo.getVendor();
        String model = deviceInfo.getModel();
        IpAddress ipAddress = deviceInfo.getIpAddress();
        // Sets IP, Model and Vendor information fetched from the deviceInfo
        node1bldr.setIp(ipAddress);
        node1bldr.setModel(model);
        node1bldr.setVendor(vendor);

        // Sets the value of Network-ref and Node-ref as a part of the supporting node
        // attribute
        String clli = deviceInfo.getClli();
        SupportingNodeBuilder supportbldr = new SupportingNodeBuilder();
        supportbldr.setKey(new SupportingNodeKey(new NetworkId(NetworkUtils.CLLI_NETWORK_ID), new NodeId(clli)));
        supportbldr.setNetworkRef(new NetworkId(NetworkUtils.CLLI_NETWORK_ID));
        supportbldr.setNodeRef(new NodeId(clli));
        nodeBldr.setSupportingNode(ImmutableList.of(supportbldr.build()));

        // Augment to the main node builder
        nodeBldr.addAugmentation(Node1.class, node1bldr.build());
        return nodeBldr.build();
    }

    /**
     * Create empty OpenROADM network.
     */
    private static Network createOpenRoadmNetwork() {
        NetworkBuilder openrdmnwBuilder = new NetworkBuilder();
        NetworkId nwId = new NetworkId(NetworkUtils.UNDERLAY_NETWORK_ID);
        openrdmnwBuilder.setNetworkId(nwId);
        openrdmnwBuilder.setKey(new NetworkKey(nwId));
        // sets network type to OpenRoadmNetwork
        NetworkTypes1Builder openRoadmNetworkTypesBldr = new NetworkTypes1Builder();
        openRoadmNetworkTypesBldr.setOpenroadmNetwork(new OpenroadmNetworkBuilder().build());
        NetworkTypesBuilder openrdmnwTypeBuilder = new NetworkTypesBuilder();
        openrdmnwTypeBuilder.addAugmentation(NetworkTypes1.class, openRoadmNetworkTypesBldr.build());
        openrdmnwBuilder.setNetworkTypes(openrdmnwTypeBuilder.build());
        return openrdmnwBuilder.build();
    }
}
