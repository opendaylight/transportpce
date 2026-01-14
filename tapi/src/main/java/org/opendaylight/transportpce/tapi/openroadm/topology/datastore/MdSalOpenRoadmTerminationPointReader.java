/*
 * Copyright © 2026 Smartoptics and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.tapi.openroadm.topology.datastore;

import java.util.Optional;
import java.util.concurrent.ExecutionException;
import org.jspecify.annotations.NonNull;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.transportpce.common.StringConstants;
import org.opendaylight.transportpce.common.network.NetworkTransactionService;
import org.opendaylight.transportpce.tapi.openroadm.TopologyNodeId;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev250110.TerminationPoint1;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.NetworkId;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.Networks;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.NodeId;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.networks.Network;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.networks.NetworkKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.networks.network.Node;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.networks.network.NodeKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.Node1;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.TpId;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.networks.network.node.TerminationPoint;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.networks.network.node.TerminationPointKey;
import org.opendaylight.yangtools.binding.DataObject;
import org.opendaylight.yangtools.binding.DataObjectIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * MD-SAL-backed implementation of {@link OpenRoadmTerminationPointReader}.
 *
 * <p>Reads termination points from the CONFIGURATION datastore under the
 * OpenROADM topology network ({@link StringConstants#OPENROADM_TOPOLOGY}).
 */
public class MdSalOpenRoadmTerminationPointReader implements OpenRoadmTerminationPointReader {

    private static final Logger LOG = LoggerFactory.getLogger(MdSalOpenRoadmTerminationPointReader.class);

    private final NetworkTransactionService networkTransactionService;

    public MdSalOpenRoadmTerminationPointReader(NetworkTransactionService networkTransactionService) {
        this.networkTransactionService = networkTransactionService;
    }

    @Override
    public Optional<TerminationPoint> readTerminationPoint(TopologyNodeId nodeId, String tpId) {
        return readTerminationPoint(nodeId, new TpId(tpId));
    }

    @Override
    public Optional<TerminationPoint> readTerminationPoint(TopologyNodeId nodeId, TpId tpId) {
        DataObjectIdentifier<TerminationPoint> tpIID = dataObjectIdentifierBuilder(nodeId, tpId).build();

        return readOptional(tpIID, "network", nodeId, tpId);
    }

    @Override
    public Optional<TerminationPoint1> readCommonTerminationPoint1(TopologyNodeId nodeId, String tpId) {
        return readCommonTerminationPoint1(nodeId, new TpId(tpId));
    }

    @Override
    public Optional<TerminationPoint1> readCommonTerminationPoint1(TopologyNodeId nodeId, TpId tpId) {
        DataObjectIdentifier<TerminationPoint1> tpIID = dataObjectIdentifierBuilder(nodeId, tpId)
                .augmentation(org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev250110
                        .TerminationPoint1.class)
                .build();

        return readOptional(tpIID, "common augmentation", nodeId, tpId);
    }

    @Override
    public Optional<org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev250110
            .TerminationPoint1> readTopologyTerminationPoint1(TopologyNodeId nodeId, String tpId) {

        return  readTopologyTerminationPoint1(nodeId, new TpId(tpId));
    }

    @Override
    public Optional<org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev250110
            .TerminationPoint1> readTopologyTerminationPoint1(TopologyNodeId nodeId, TpId tpId) {

        DataObjectIdentifier<org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev250110
                .TerminationPoint1> tpIID = dataObjectIdentifierBuilder(nodeId, new TpId(tpId))
                .augmentation(org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev250110
                                .TerminationPoint1.class)
                .build();

        return readOptional(tpIID, "topology augmentation", nodeId, tpId);
    }

    private static DataObjectIdentifier.Builder.@NonNull WithKey<TerminationPoint, TerminationPointKey>
            dataObjectIdentifierBuilder(TopologyNodeId nodeId, TpId tpId) {

        return DataObjectIdentifier.builder(Networks.class)
                .child(
                        Network.class,
                        new NetworkKey(new NetworkId(StringConstants.OPENROADM_TOPOLOGY)))
                .child(
                        Node.class,
                        new NodeKey(new NodeId(nodeId.value())))
                .augmentation(Node1.class)
                .child(
                        TerminationPoint.class,
                        new TerminationPointKey(tpId));
    }

    /**
     * Reads a data object from the OpenROADM topology in the CONFIGURATION datastore.
     *
     * <p>This helper performs a synchronous MD-SAL read using the supplied
     * {@link DataObjectIdentifier} and returns the result as an {@link Optional}.
     *
     * <p>If the data object is not present in the datastore, {@link Optional#empty()}
     * is returned and a debug-level log entry is emitted.
     *
     * <p>If the read operation is interrupted, the thread interruption status is
     * restored and {@link Optional#empty()} is returned.
     *
     * <p>If the read fails due to an execution error, the exception is logged and
     * {@link Optional#empty()} is returned.
     *
     * @param <T>
     *     the type of the data object to read
     * @param iid
     *     the {@link DataObjectIdentifier} identifying the data object in the datastore
     * @param what
     *     a human-readable description of the object being read, used for logging
     *     (e.g. {@code "network"}, {@code "common augmentation"})
     * @param nodeId
     *     the OpenROADM topology node identifier associated with the read
     * @param tpId
     *     the termination point identifier associated with the read
     * @return
     *     an {@link Optional} containing the data object if present, or
     *     {@link Optional#empty()} if the object is not present or the read fails
     */
    private <T extends DataObject> Optional<T>
            readOptional(DataObjectIdentifier<T> iid, String what, TopologyNodeId nodeId, TpId tpId) {

        try {
            Optional<T> result = networkTransactionService.read(LogicalDatastoreType.CONFIGURATION, iid).get();
            if (result.isEmpty()) {
                LOG.debug("{} not present in datastore: {}", what, iid);
            }
            return result;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            LOG.warn("Interrupted while reading {} {} for node {} from {} topology", what, tpId, nodeId,
                    StringConstants.OPENROADM_TOPOLOGY, e);
            return Optional.empty();
        } catch (ExecutionException e) {
            LOG.warn("Exception while reading {} {} for node {} from {} topology", what, tpId, nodeId,
                    StringConstants.OPENROADM_TOPOLOGY, e);
            return Optional.empty();
        }
    }
}
