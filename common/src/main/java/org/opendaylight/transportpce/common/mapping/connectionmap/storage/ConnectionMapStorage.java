/*
 * Copyright © 2024 Smartoptics and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.common.mapping.connectionmap.storage;

import java.util.Collection;
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
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConnectionMapStorage implements Storage {

    private static final Logger LOG = LoggerFactory.getLogger(ConnectionMapStorage.class);

    private final DataBroker dataBroker;

    private final Translate openRoadm;

    private final Rev170206 rev170206;

    private final Rev181019 rev181019;

    private final Rev200529 rev200529;

    public ConnectionMapStorage(DataBroker dataBroker,
                                Translate openRoadm,
                                Rev170206 rev170206,
                                Rev181019 rev181019,
                                Rev200529 rev200529) {

        this.dataBroker = dataBroker;
        this.openRoadm = openRoadm;
        this.rev170206 = rev170206;
        this.rev181019 = rev181019;
        this.rev200529 = rev200529;
    }

    @Override
    public boolean saveRev170206(String nodeId,
                                 Map<org.opendaylight.yang.gen.v1.http.org.openroadm.device
                                            .rev170206.org.openroadm.device.container.org.openroadm.device
                                            .ConnectionMapKey,
                                     org.opendaylight.yang.gen.v1.http.org.openroadm.device
                                            .rev170206.org.openroadm.device.container.org.openroadm.device
                                            .ConnectionMap> connectionMap) {

        return rev170206.save(dataBroker, openRoadm, nodeId, connectionMap);
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
        InstanceIdentifier<OpenroadmConnectionMap> nodesIID =
            InstanceIdentifier.builder(OpenroadmConnectionMap.class).build();

        try (ReadTransaction readTransaction = dataBroker.newReadOnlyTransaction()) {
            Optional<OpenroadmConnectionMap> connectionMap =
                readTransaction.read(LogicalDatastoreType.OPERATIONAL, nodesIID).get();

            OpenroadmConnectionMap openroadmConnectionMap = connectionMap.orElseThrow();
            Map<NetworkNodesKey, NetworkNodes> connectionMapNodes = openroadmConnectionMap.getNetworkNodes();

            if (connectionMapNodes != null) {

                Collection<NetworkNodes> values = connectionMapNodes.values();

                LOG.debug("Read {} connection maps from storage for node {}.", values.size(), nodeId);

                return connectionMapNodes;
            } else {
                LOG.debug("No connection map found for node {}", nodeId);
            }
        } catch (InterruptedException | ExecutionException | NoSuchElementException ex) {
            LOG.warn("Exception thrown while reading Logical Connection Point value", ex);
        }

        return new HashMap<>();
    }
}
