/*
 * Copyright © 2018 Orange, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.tapi.connectivity;

import com.google.common.collect.ImmutableClassToInstanceMap;
import org.opendaylight.mdsal.binding.api.RpcService;
import org.opendaylight.transportpce.common.network.NetworkTransactionService;
import org.opendaylight.transportpce.tapi.impl.rpc.CreateConnectivityServiceImpl;
import org.opendaylight.transportpce.tapi.impl.rpc.DeleteConnectivityServiceImpl;
import org.opendaylight.transportpce.tapi.impl.rpc.GetConnectionDetailsImpl;
import org.opendaylight.transportpce.tapi.impl.rpc.GetConnectivityServiceDetailsImpl;
import org.opendaylight.transportpce.tapi.impl.rpc.GetConnectivityServiceListImpl;
import org.opendaylight.transportpce.tapi.listeners.TapiPceNotificationHandler;
import org.opendaylight.transportpce.tapi.listeners.TapiRendererNotificationHandler;
import org.opendaylight.transportpce.tapi.utils.TapiContext;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev221121.CreateConnectivityService;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev221121.DeleteConnectivityService;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev221121.GetConnectionDetails;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev221121.GetConnectivityServiceDetails;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev221121.GetConnectivityServiceList;
import org.opendaylight.yangtools.yang.binding.Rpc;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Top level service interface providing main TAPI Connectivity services.
 */
public class TapiConnectivityImpl {
    private static final Logger LOG = LoggerFactory.getLogger(TapiConnectivityImpl.class);

    private RpcService rpcService;
    private final TapiContext tapiContext;
    private final ConnectivityUtils connectivityUtils;
    private TapiPceNotificationHandler pceListenerImpl;
    private TapiRendererNotificationHandler rendererListenerImpl;
    private final NetworkTransactionService networkTransactionService;

    public TapiConnectivityImpl(RpcService rpcService, TapiContext tapiContext,
                                ConnectivityUtils connectivityUtils, TapiPceNotificationHandler pceListenerImpl,
                                TapiRendererNotificationHandler rendererListenerImpl,
                                NetworkTransactionService networkTransactionService) {
        LOG.info("inside TapiImpl constructor");
        this.rpcService = rpcService;
        this.tapiContext = tapiContext;
        this.connectivityUtils = connectivityUtils;
        this.pceListenerImpl = pceListenerImpl;
        this.rendererListenerImpl = rendererListenerImpl;
        this.networkTransactionService = networkTransactionService;
    }


    public ImmutableClassToInstanceMap<Rpc<?, ?>> registerRPCs() {
        return ImmutableClassToInstanceMap.<Rpc<?, ?>>builder()
            .put(CreateConnectivityService.class, new CreateConnectivityServiceImpl(rpcService, tapiContext,
                    connectivityUtils, pceListenerImpl, rendererListenerImpl))
            .put(GetConnectivityServiceDetails.class, new GetConnectivityServiceDetailsImpl(tapiContext))
            .put(GetConnectionDetails.class, new GetConnectionDetailsImpl(tapiContext))
            .put(DeleteConnectivityService.class, new DeleteConnectivityServiceImpl(rpcService, tapiContext,
                    networkTransactionService))
            .put(GetConnectivityServiceList.class, new GetConnectivityServiceListImpl(tapiContext))
            .build();
    }

}
