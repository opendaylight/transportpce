/*
 * Copyright (c) 2023 Orange, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.common.srg.storage;

import com.google.common.util.concurrent.FluentFuture;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.binding.api.ReadTransaction;
import org.opendaylight.mdsal.binding.api.WriteTransaction;
import org.opendaylight.mdsal.common.api.CommitInfo;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.yang.gen.v1.http.com.smartoptics.openroadm.srg.rev231110.Network;
import org.opendaylight.yang.gen.v1.http.com.smartoptics.openroadm.srg.rev231110.NetworkBuilder;
import org.opendaylight.yang.gen.v1.http.com.smartoptics.openroadm.srg.rev231110.network.NetworkNodes;
import org.opendaylight.yang.gen.v1.http.com.smartoptics.openroadm.srg.rev231110.network.NetworkNodesBuilder;
import org.opendaylight.yang.gen.v1.http.com.smartoptics.openroadm.srg.rev231110.network.NetworkNodesKey;
import org.opendaylight.yang.gen.v1.http.com.smartoptics.openroadm.srg.rev231110.network.network.nodes.SharedRiskGroup;
import org.opendaylight.yang.gen.v1.http.com.smartoptics.openroadm.srg.rev231110.network.network.nodes.SharedRiskGroupKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;

public class SrgStorage implements Storage {

    private final DataBroker dataBroker;

    private final Logger log;

    public SrgStorage(DataBroker dataBroker, Logger log) {
        this.dataBroker = dataBroker;
        this.log = log;
    }

    @Override
    public boolean save(String nodeId, Map<SharedRiskGroupKey, SharedRiskGroup> sharedRiskGroupMap) {
        NetworkNodesBuilder networkNodesBuilder = new NetworkNodesBuilder();
        networkNodesBuilder
                .setNodeId(nodeId)
                .setSharedRiskGroup(sharedRiskGroupMap);
        NetworkNodes networkNodes = networkNodesBuilder.build();

        NetworkBuilder networkBuilder = new NetworkBuilder();
        networkBuilder.setNetworkNodes(Map.of(networkNodes.key(), networkNodes));

        Network network = networkBuilder.build();

        InstanceIdentifier<Network> nodesIID =
                InstanceIdentifier.builder(Network.class).build();
        WriteTransaction writeTransaction = dataBroker.newWriteOnlyTransaction();
        writeTransaction.merge(LogicalDatastoreType.OPERATIONAL, nodesIID, network);
        FluentFuture<? extends @NonNull CommitInfo> commit = writeTransaction.commit();

        log.debug("Shared risk group info for node {}: {}", nodeId, network);

        try {
            commit.get();
            log.info("Saved shared risk group for node {} to storage.", nodeId);
            return true;
        } catch (InterruptedException | ExecutionException e) {
            log.error("Failed saving share risk group for node {} to storage: {}", nodeId, e.getMessage());
            return false;
        }
    }

    @Override
    public Map<SharedRiskGroupKey, SharedRiskGroup> read(String nodeId) {
        return read(new NetworkNodesKey(nodeId));
    }

    @Override
    public Map<SharedRiskGroupKey, SharedRiskGroup> read(NetworkNodesKey nodeId) {
        InstanceIdentifier<NetworkNodes> nodesIID =
                InstanceIdentifier
                        .builder(Network.class)
                        .child(NetworkNodes.class, nodeId)
                        .build();

        Map<SharedRiskGroupKey, SharedRiskGroup> temp = new HashMap<>();
        log.debug("Reading SRG info using network node id {}...", nodeId);
        try (ReadTransaction readTransaction = dataBroker.newReadOnlyTransaction()) {
            Optional<NetworkNodes> optionalNetwork =
                    readTransaction.read(LogicalDatastoreType.OPERATIONAL, nodesIID).get();

            if (optionalNetwork.isPresent()) {
                Collection<NetworkNodes> values = List.of(optionalNetwork.orElseThrow());

                log.debug("Read {} nodes using node id {}.", values.size(), nodeId);
                for (NetworkNodes networkNodes : values) {
                    Map<SharedRiskGroupKey, SharedRiskGroup> sharedRiskGroups =
                            Objects.requireNonNull(networkNodes.getSharedRiskGroup());

                    log.debug("Read {} srg:s for node {}: {}.",
                            sharedRiskGroups.size(),
                            networkNodes.getNodeId(),
                            sharedRiskGroups
                    );
                    temp.putAll(sharedRiskGroups);
                }
                log.debug("Returning {} srg:s {}", temp.size(), temp);
                return temp;
            } else {
                log.debug("No srg info found for node {}", nodeId);
            }
        } catch (InterruptedException | ExecutionException | NoSuchElementException ex) {
            log.warn("Exception thrown while reading SRG info for node: {}", nodeId, ex);
        }

        log.info("Found srg:s in storage: {}", temp);
        return temp;
    }
}