/*
 * Copyright © 2025 Smartoptics and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.tapi.sync.topology;


import com.google.common.util.concurrent.ListenableFuture;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.transportpce.common.InstanceIdentifiers;
import org.opendaylight.transportpce.common.network.NetworkTransactionService;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.networks.Network;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.networks.NetworkKey;
import org.opendaylight.yangtools.binding.DataObjectIdentifier;

public class OpenRoadmTopologyStorage implements Storage {

    private final NetworkTransactionService networkTransactionService;

    public OpenRoadmTopologyStorage(NetworkTransactionService networkTransactionService) {
        this.networkTransactionService = networkTransactionService;
    }

    @Override
    public Network openRoadmNetwork(
        DataObjectIdentifier.WithKey<Network, NetworkKey> openroadmTopologyIdentifier) {
        ListenableFuture<Optional<Network>> topologyFuture =
                this.networkTransactionService.read(
                        LogicalDatastoreType.CONFIGURATION,
                        InstanceIdentifiers.OPENROADM_TOPOLOGY_II
                );

        try {
            return topologyFuture.get().orElseThrow();
        } catch (ExecutionException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

}
