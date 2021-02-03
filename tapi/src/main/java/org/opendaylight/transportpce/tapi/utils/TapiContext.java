/*
 * Copyright © 2021 Nokia, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.tapi.utils;

import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.transportpce.common.network.NetworkTransactionService;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev181210.Context;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev181210.ContextBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev181210.Uuid;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev181210.global._class.Name;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev181210.global._class.NameBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev181210.Context1;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev181210.Context1Builder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev181210.context.ConnectivityContextBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.context.TopologyContextBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.topology.context.NwTopologyServiceBuilder;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TapiContext {

    private static final Logger LOG = LoggerFactory.getLogger(TapiContext.class);
    public static final String TAPI_CONTEXT = "T-API context";
    private final NetworkTransactionService networkTransactionService;

    public TapiContext(NetworkTransactionService networkTransactionService) {
        this.networkTransactionService = networkTransactionService;
        createTapiContext();
    }

    private void createTapiContext() {
        try {
            ContextBuilder contextBuilder = new ContextBuilder();
            // Augmenting tapi context to include topology and connectivity contexts
            Context1 connectivityContext = new Context1Builder()
                    .setConnectivityContext(new ConnectivityContextBuilder()
                            .setConnection(new HashMap<>())
                            .setConnectivityService(new HashMap<>())
                            .build())
                    .build();
            org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.Context1 topologyContext
                    = new org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.Context1Builder()
                    .setTopologyContext(new TopologyContextBuilder()
                            .setNwTopologyService(new NwTopologyServiceBuilder().build())
                            .setTopology(new HashMap<>())
                            .build())
                    .build();
            Name contextName = new NameBuilder().setValue(TAPI_CONTEXT).setValueName("TAPI Context Name").build();
            Uuid contextUuid = new Uuid(UUID.nameUUIDFromBytes(TAPI_CONTEXT.getBytes(Charset.forName("UTF-8")))
                    .toString());
            contextBuilder.setName(Map.of(contextName.key(), contextName));
            contextBuilder.setUuid(contextUuid);
            contextBuilder.setServiceInterfacePoint(new HashMap<>());
            contextBuilder.addAugmentation(connectivityContext);
            contextBuilder.addAugmentation(topologyContext);
            // todo: add notification context
            InstanceIdentifier<Context> contextIID = InstanceIdentifier.builder(Context.class).build();
            // put in datastore
            this.networkTransactionService.put(LogicalDatastoreType.CONFIGURATION, contextIID, contextBuilder.build());
            this.networkTransactionService.commit().get();
            LOG.info("TAPI context created successfully.");
        } catch (InterruptedException | ExecutionException e) {
            LOG.error("Failed to create TAPI context", e);
        }
    }

    public Context getTapiContext() {
        // TODO: verify this is correct. Should we identify the context IID with the context UUID??
        //  There is no Identifiable in Context model
        InstanceIdentifier<Context> contextIID = InstanceIdentifier.builder(Context.class).build();
        try {
            Optional<Context> optionalContext = this.networkTransactionService.read(LogicalDatastoreType.CONFIGURATION,
                    contextIID).get();
            if (!optionalContext.isPresent()) {
                LOG.error("Tapi context is not present in datastore");
                return null;
            }
            return optionalContext.get();
        } catch (InterruptedException | ExecutionException e) {
            LOG.error("Couldnt read tapi context from datastore");
            return null;
        }
    }

    public void deleteTapiContext() {

    }

}
