/*
 * Copyright © 2020 ORANGE.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.networkmodel.util;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.transportpce.common.NetworkUtils;
import org.opendaylight.transportpce.common.network.NetworkTransactionService;
import org.opendaylight.yang.gen.v1.http.org.openroadm.clli.network.rev191129.NetworkTypes1Builder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.clli.network.rev191129.networks.network.network.types.ClliNetworkBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev230526.networks.network.network.types.OpenroadmCommonNetworkBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.NetworkId;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.Networks;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.networks.Network;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.networks.NetworkBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.networks.NetworkKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.networks.network.NetworkTypes;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.networks.network.NetworkTypesBuilder;
import org.opendaylight.yangtools.binding.Augmentation;
import org.opendaylight.yangtools.binding.DataObjectIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TpceNetwork {

    private static final Logger LOG = LoggerFactory.getLogger(TpceNetwork.class);

    private NetworkTransactionService networkTransactionService;

    public TpceNetwork(NetworkTransactionService networkTransactionService) {
        this.networkTransactionService = networkTransactionService;
    }

    /**
     * This public method creates the adequate openroadm layer and posts it to the controller.
     *
     * @param networkId   defines the network layer
     */
    public void createLayer(String networkId) {
        try {
            Network network = createNetwork(networkId);
            DataObjectIdentifier<Network> nwIID = DataObjectIdentifier.builder(Networks.class)
                    .child(Network.class, new NetworkKey(new NetworkId(networkId)))
                    .build();
            networkTransactionService.merge(LogicalDatastoreType.CONFIGURATION, nwIID, network);
            this.networkTransactionService.commit().get(1, TimeUnit.SECONDS);
            LOG.info("{} network layer created successfully.", networkId);
        } catch (ExecutionException | TimeoutException | InterruptedException e) {
            LOG.error("Failed to create {} network layer", networkId, e);
        }
    }


    /**
     * Create empty CLLI network.
     */
    private static Network createNetwork(String networkId) {
        NetworkTypesBuilder networkTypesBldr = new NetworkTypesBuilder();
        switch (networkId) {
            case NetworkUtils.CLLI_NETWORK_ID:
                Augmentation<NetworkTypes> ordClli = new NetworkTypes1Builder()
                    .setClliNetwork(new ClliNetworkBuilder().build())
                    .build();
                networkTypesBldr.addAugmentation(ordClli);
                break;
            case NetworkUtils.UNDERLAY_NETWORK_ID:
            case NetworkUtils.OVERLAY_NETWORK_ID:
            case NetworkUtils.OTN_NETWORK_ID:
                Augmentation<NetworkTypes> ordTopology = new org.opendaylight.yang.gen.v1.http
                    .org.openroadm.common.network.rev230526.NetworkTypes1Builder()
                    .setOpenroadmCommonNetwork(new OpenroadmCommonNetworkBuilder().build())
                    .build();
                networkTypesBldr.addAugmentation(ordTopology);
                break;
            default:
                LOG.error("Unknown network type");
                break;
        }
        return new NetworkBuilder()
            .setNetworkId(new NetworkId(networkId))
            .withKey(new NetworkKey(new NetworkId(networkId)))
            .setNetworkTypes(networkTypesBldr.build())
            .build();
    }
}
