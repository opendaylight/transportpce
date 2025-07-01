/*
 * Copyright © 2025 Smartoptics and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.tapi.openroadm.topology.storage;

import com.google.common.util.concurrent.ListenableFuture;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.transportpce.common.network.NetworkTransactionService;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.networks.Network;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.networks.NetworkKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.networks.network.Node;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.networks.network.NodeKey;
import org.opendaylight.yangtools.binding.DataObjectIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OpenRoadmStorage implements OpenROADM {

    private static final Logger LOG = LoggerFactory.getLogger(OpenRoadmStorage.class);

    private final NetworkTransactionService networkTransactionService;

    public OpenRoadmStorage(NetworkTransactionService networkTransactionService) {
        this.networkTransactionService = networkTransactionService;
    }

    @Override
    public Set<Map.Entry<NodeKey, Node>> openRoadmTopology(
            LogicalDatastoreType configuration,
            DataObjectIdentifier.WithKey<Network, NetworkKey> openroadmTopologId) {

        ListenableFuture<Optional<Network>> topologyFuture =
                this.networkTransactionService.read(
                        configuration,
                        openroadmTopologId
                );

        Set<Map.Entry<NodeKey, Node>> openRoadmTopology;
        try {
            openRoadmTopology = Optional.of(Optional.ofNullable(topologyFuture.get()
                    .orElseThrow().getNode()).orElseThrow()).orElseThrow().entrySet();
            LOG.debug("OpenROADM topology: {}", openRoadmTopology.stream().map(Map.Entry::getKey).toList());
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }

        return openRoadmTopology;
    }
}
