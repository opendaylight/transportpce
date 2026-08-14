/*
 * Copyright © 2026 Smartoptics and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.tapi.topology;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.transportpce.common.network.NetworkTransactionService;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.Context;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.Uuid;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.global._class.Name;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.Context1;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.context.TopologyContext;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.topology.Link;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.topology.LinkKey;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.topology.context.Topology;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.topology.context.TopologyBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.topology.context.TopologyKey;
import org.opendaylight.yangtools.binding.DataObjectIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Shared datastore-write logic for merging TAPI content into a topology, in the OPERATIONAL datastore.
 *
 * <p>{@link #mergeInTopology} holds the merge-target construction that the XPDR-RDM and inter-domain
 * link writes triggered by openroadm-topology link events ({@code TapiOrLinkListener
 * #putTapiLinkInTopology} and {@code TapiOrLinkListener#putTapiInterDomainLinkInTopology}), and
 * {@link TapiNetworkModelServiceImpl}'s node write ({@code mergeNodeinTopology}), independently
 * duplicated; a future change to it (e.g. switching datastore, changing the merge target) now only
 * needs to be made once. The commit and its error handling stay with each caller, since those
 * genuinely differ: the link writer reports failure by returning {@code false} after logging it,
 * while the node writer interleaves a rule-group write between the merge and the commit and raises
 * failure as an {@link IllegalStateException}.
 */
public final class TapiLinkTopologyWriter {

    private static final Logger LOG = LoggerFactory.getLogger(TapiLinkTopologyWriter.class);

    private TapiLinkTopologyWriter() {
        // utility class
    }

    /**
     * Merges {@code payload} into the topology identified by {@code topoUuid}, in the OPERATIONAL
     * datastore. Does not commit; the caller decides when to commit and how to handle failure.
     */
    static void mergeInTopology(NetworkTransactionService networkTransactionService,
            Uuid topoUuid, Topology payload) {
        networkTransactionService.merge(
            LogicalDatastoreType.OPERATIONAL,
            DataObjectIdentifier.builder(Context.class)
                .augmentation(Context1.class)
                .child(TopologyContext.class)
                .child(Topology.class, new TopologyKey(topoUuid))
                .build(),
            payload);
    }

    /**
     * Merges {@code linkMap} into the link list of the topology identified by {@code topoUuid}, in the
     * OPERATIONAL datastore, and commits.
     *
     * @return {@code true} if the commit succeeded, {@code false} if it failed. On failure this
     *     already logs the error with its stack trace; the caller does not need to log it again.
     */
    public static boolean mergeLinksInTopology(
            NetworkTransactionService networkTransactionService,
            Uuid topoUuid,
            Map<LinkKey, Link> linkMap) {

        Topology topology = new TopologyBuilder().setUuid(topoUuid).setLink(linkMap).build();
        mergeInTopology(networkTransactionService, topoUuid, topology);
        try {
            networkTransactionService.commit().get();
            return true;
        } catch (InterruptedException | ExecutionException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            List<String> linkNames = linkMap.values().stream()
                .flatMap(link -> link.nonnullName().values().stream())
                .map(Name::getValue)
                .collect(Collectors.toList());
            LOG.error("Error while merging link(s) {} into TAPI topology {}: ", linkNames, topoUuid, e);
            return false;
        }
    }
}
