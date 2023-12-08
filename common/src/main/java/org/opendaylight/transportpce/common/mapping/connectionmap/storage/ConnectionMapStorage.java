/*
 * Copyright © 2024 Smartoptics and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.common.mapping.connectionmap.storage;

import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.binding.api.ReadTransaction;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.transportpce.common.mapping.connectionmap.translate.Translate;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.connection.map.rev231026.OpenroadmConnectionMap;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.connection.map.rev231026.openroadm.connection.map.NetworkNodes;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.connection.map.rev231026.openroadm.connection.map.NetworkNodesKey;
import org.opendaylight.yangtools.binding.DataObjectIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@link Storage} implementation that translates OpenROADM device connection maps into the
 * TransportPCE connection-map model and persists them to the operational datastore.
 */
public class ConnectionMapStorage implements Storage {

    private static final Logger LOG = LoggerFactory.getLogger(ConnectionMapStorage.class);

    private final DataBroker dataBroker;

    private final Translate openRoadm;

    private final Rev181019 rev181019;

    private final Rev200529 rev200529;

    public ConnectionMapStorage(DataBroker dataBroker,
                                Translate openRoadm,
                                Rev181019 rev181019,
                                Rev200529 rev200529) {

        this.dataBroker = dataBroker;
        this.openRoadm = openRoadm;
        this.rev181019 = rev181019;
        this.rev200529 = rev200529;
    }

    @Override
    public boolean saveRev181019(String nodeId,
                                 Map<org.opendaylight.yang.gen.v1.http.org.openroadm.device
                                            .rev181019.org.openroadm.device.container.org.openroadm.device
                                            .ConnectionMapKey,
                                     org.opendaylight.yang.gen.v1.http.org.openroadm.device
                                            .rev181019.org.openroadm.device.container.org.openroadm.device
                                            .ConnectionMap> connectionMap) {

        return rev181019.save(dataBroker, openRoadm, nodeId, connectionMap);
    }

    @Override
    public boolean saveRev200529(String nodeId,
                                 Map<org.opendaylight.yang.gen.v1.http.org.openroadm.device
                                            .rev200529.org.openroadm.device.container.org.openroadm.device
                                            .ConnectionMapKey,
                                     org.opendaylight.yang.gen.v1.http.org.openroadm.device
                                            .rev200529.org.openroadm.device.container.org.openroadm.device
                                            .ConnectionMap> connectionMap) {

        return rev200529.save(dataBroker, openRoadm, nodeId, connectionMap);
    }

    @Override
    public Map<NetworkNodesKey, NetworkNodes> read(String nodeId) {
        DataObjectIdentifier<NetworkNodes> nodesIID = DataObjectIdentifier
                .builder(OpenroadmConnectionMap.class)
                .child(NetworkNodes.class, new NetworkNodesKey(nodeId))
                .build();

        try (ReadTransaction readTransaction = dataBroker.newReadOnlyTransaction()) {
            Optional<NetworkNodes> node = Optional.ofNullable(
                            readTransaction
                                    .read(LogicalDatastoreType.OPERATIONAL, nodesIID)
                                    .get())
                    .orElse(Optional.empty());

            if (node.isPresent()) {
                return Map.of(new NetworkNodesKey(nodeId), node.orElseThrow());
            }

        } catch (InterruptedException | ExecutionException | NoSuchElementException ex) {
            LOG.warn("Exception thrown while reading Logical Connection Point value", ex);
        }

        return new HashMap<>();
    }
}
