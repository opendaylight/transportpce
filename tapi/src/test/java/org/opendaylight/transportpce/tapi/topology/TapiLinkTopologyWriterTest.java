/*
 * Copyright © 2026 Smartoptics and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.tapi.topology;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import org.junit.jupiter.api.Test;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.transportpce.common.network.NetworkTransactionImpl;
import org.opendaylight.transportpce.common.network.NetworkTransactionService;
import org.opendaylight.transportpce.tapi.TapiConstants;
import org.opendaylight.transportpce.test.AbstractTest;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.Context;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.LayerProtocolName;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.Uuid;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.Context1;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.context.TopologyContext;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.topology.Link;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.topology.LinkBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.topology.context.Topology;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.topology.context.TopologyBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.topology.context.TopologyKey;
import org.opendaylight.yangtools.binding.DataObjectIdentifier;

public class TapiLinkTopologyWriterTest extends AbstractTest {

    @Test
    void mergeLinksInTopologyWritesLinkAndReturnsTrue() throws InterruptedException, ExecutionException {
        NetworkTransactionService networkTransactionService = new NetworkTransactionImpl(getDataBroker());
        Uuid topoUuid = TapiConstants.T0_FULL_MULTILAYER_UUID;
        Link link = new LinkBuilder()
                .setUuid(new Uuid("11111111-1111-1111-1111-111111111111"))
                .build();

        // In production, the topology entry always already exists (created during TAPI context
        // bootstrap) with its mandatory layer-protocol-name populated by the time any link is
        // merged into it. Recreate that precondition here rather than merging into an empty topology.
        networkTransactionService.merge(
                LogicalDatastoreType.OPERATIONAL,
                DataObjectIdentifier.builder(Context.class)
                        .augmentation(Context1.class)
                        .child(TopologyContext.class)
                        .child(Topology.class, new TopologyKey(topoUuid))
                        .build(),
                new TopologyBuilder()
                        .setUuid(topoUuid)
                        .setLayerProtocolName(Set.of(LayerProtocolName.PHOTONICMEDIA))
                        .build());
        networkTransactionService.commit().get();

        boolean result = TapiLinkTopologyWriter.mergeLinksInTopology(
                networkTransactionService, topoUuid, Map.of(link.key(), link));

        assertTrue(result, "Merging a link into a valid topology path must succeed");

        Topology topology = getDataBroker().newReadOnlyTransaction()
                .read(LogicalDatastoreType.OPERATIONAL,
                        DataObjectIdentifier.builder(Context.class)
                                .augmentation(Context1.class)
                                .child(TopologyContext.class)
                                .child(Topology.class, new TopologyKey(topoUuid))
                                .build())
                .get()
                .orElseThrow();

        assertTrue(topology.nonnullLink().containsKey(link.key()),
                "The merged link must be present in the topology afterward");
    }
}
