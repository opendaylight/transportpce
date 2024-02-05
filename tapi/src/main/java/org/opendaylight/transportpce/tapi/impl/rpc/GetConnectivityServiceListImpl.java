/*
 * Copyright © 2024 Orange, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.tapi.impl.rpc;

import com.google.common.util.concurrent.ListenableFuture;
import java.util.HashMap;
import java.util.Map;
import org.opendaylight.transportpce.tapi.utils.TapiContext;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev221121.GetConnectivityServiceList;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev221121.GetConnectivityServiceListInput;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev221121.GetConnectivityServiceListOutput;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev221121.GetConnectivityServiceListOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev221121.connectivity.context.ConnectivityService;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev221121.connectivity.context.ConnectivityServiceKey;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev221121.get.connectivity.service.list.output.Service;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev221121.get.connectivity.service.list.output.ServiceKey;
import org.opendaylight.yangtools.yang.common.ErrorType;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class GetConnectivityServiceListImpl implements GetConnectivityServiceList {
    private static final Logger LOG = LoggerFactory.getLogger(GetConnectivityServiceListImpl.class);

    private final TapiContext tapiContext;

    public GetConnectivityServiceListImpl(TapiContext tapiContext) {
        this.tapiContext = tapiContext;
    }

    @Override
    public ListenableFuture<RpcResult<GetConnectivityServiceListOutput>> invoke(GetConnectivityServiceListInput input) {
        // TODO Auto-generated method stub
        Map<ConnectivityServiceKey, ConnectivityService> connMap = this.tapiContext.getConnectivityServices();
        if (connMap == null) {
            LOG.error("No services in tapi context");
            return RpcResultBuilder.<GetConnectivityServiceListOutput>failed()
                .withError(ErrorType.RPC, "No services exist in datastore")
                .buildFuture();
        }

        Map<ServiceKey, Service> serviceMap = new HashMap<>();
        for (ConnectivityService connectivityService: connMap.values()) {
            Service service = new org.opendaylight.yang.gen.v1.urn
                .onf.otcc.yang.tapi.connectivity.rev221121.get.connectivity.service.list.output.ServiceBuilder(
                    connectivityService).build();
            serviceMap.put(service.key(), service);
        }
        return RpcResultBuilder.success(new GetConnectivityServiceListOutputBuilder().setService(serviceMap)
            .build()).buildFuture();
    }

}
