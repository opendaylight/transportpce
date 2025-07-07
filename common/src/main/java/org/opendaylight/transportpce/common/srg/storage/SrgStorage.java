/*
 * Copyright © 2025 Smartoptics and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.common.srg.storage;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.binding.api.ReadTransaction;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev250905.Network;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev250905.network.Nodes;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev250905.network.NodesKey;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev250905.shared.risk.group.SharedRiskGroup;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev250905.shared.risk.group.SharedRiskGroupKey;
import org.opendaylight.yangtools.binding.DataObjectIdentifier;
import org.slf4j.Logger;

public class SrgStorage implements Storage {

    private final DataBroker dataBroker;

    private final Logger log;

    public SrgStorage(DataBroker dataBroker, Logger log) {
        this.dataBroker = dataBroker;
        this.log = log;
    }

    @Override
    public Map<SharedRiskGroupKey, SharedRiskGroup> read(String nodeId) {
        return read(new NodesKey(nodeId));
    }

    @Override
    public Map<SharedRiskGroupKey, SharedRiskGroup> read(NodesKey nodeId) {
        DataObjectIdentifier.WithKey<Nodes, NodesKey> nodesIID = DataObjectIdentifier
                .builder(Network.class)
                .child(Nodes.class, nodeId)
                .build();

        log.debug("Reading SRG info using network node id {}...", nodeId);
        try (ReadTransaction readTransaction = dataBroker.newReadOnlyTransaction()) {
            Optional<Nodes> optionalNetwork =
                    readTransaction.read(LogicalDatastoreType.CONFIGURATION, nodesIID).get();

            if (optionalNetwork.isPresent()) {
                Collection<Nodes> values = List.of(optionalNetwork.orElseThrow());
                log.debug("Read {} nodes using node id {}.", values.size(), nodeId);
                return findAndReturnSharedRiskGroups(values);
            } else {
                log.debug("No srg info found for node {}", nodeId);
            }
        } catch (InterruptedException | ExecutionException | NoSuchElementException ex) {
            log.warn("Exception thrown while reading SRG info for node: {}", nodeId, ex);
        }

        return new HashMap<>();
    }

    private Map<SharedRiskGroupKey, SharedRiskGroup> findAndReturnSharedRiskGroups(Collection<Nodes> nodes) {
        Map<SharedRiskGroupKey, SharedRiskGroup> temp = new HashMap<>();

        for (Nodes networkNode : nodes) {
            Map<SharedRiskGroupKey, SharedRiskGroup> sharedRiskGroups =
                    Objects.requireNonNullElse(networkNode.getSharedRiskGroup(),
                            new HashMap<>());

            log.debug("Read {} srg:s for node {}: {}.",
                    sharedRiskGroups.size(),
                    networkNode.getNodeId(),
                    sharedRiskGroups
            );
            temp.putAll(sharedRiskGroups);
        }

        log.info("Found srg:s in storage: {}", temp);

        return temp;
    }
}
