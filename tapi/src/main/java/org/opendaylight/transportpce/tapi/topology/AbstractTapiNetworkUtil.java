/*
 * Copyright © 2025 Orange, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.tapi.topology;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.transportpce.common.network.NetworkTransactionService;
import org.opendaylight.transportpce.tapi.TapiStringConstants;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.Context;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.Uuid;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.Context1;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.context.TopologyContext;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.topology.Link;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.topology.context.Topology;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.topology.context.TopologyBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.topology.context.TopologyKey;
import org.opendaylight.yangtools.binding.DataObjectIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractTapiNetworkUtil {

    private static final Logger LOG = LoggerFactory.getLogger(AbstractTapiNetworkUtil.class);
    protected final Uuid tapiTopoUuid = new Uuid(UUID.nameUUIDFromBytes(TapiStringConstants.T0_FULL_MULTILAYER
            .getBytes(StandardCharsets.UTF_8)).toString());
    private final NetworkTransactionService networkTransactionService;

    protected AbstractTapiNetworkUtil(NetworkTransactionService networkTransactionService) {
        this.networkTransactionService = networkTransactionService;
    }

    public boolean putLinkInTopology(Link tapLink) {
        // TODO is this merge correct? Should we just merge topology by changing the nodes map??
        // TODO: verify this is correct. Should we identify the context IID with the context UUID??
        LOG.info("Creating tapi node in TAPI topology context");
        DataObjectIdentifier<Topology> topoIID = DataObjectIdentifier.builder(Context.class)
            .augmentation(Context1.class).child(TopologyContext.class)
            .child(Topology.class, new TopologyKey(tapiTopoUuid))
            .build();

        Topology topology = new TopologyBuilder().setUuid(tapiTopoUuid)
            .setLink(Map.of(tapLink.key(), tapLink)).build();

        // merge in datastore
        this.networkTransactionService.merge(LogicalDatastoreType.OPERATIONAL, topoIID,
            topology);
        try {
            this.networkTransactionService.commit().get();

        } catch (InterruptedException | ExecutionException e) {
            LOG.error("Error populating TAPI topology: ", e);
            return false;
        }
        LOG.info("TAPI Link added succesfully.");
        return true;
    }
}
