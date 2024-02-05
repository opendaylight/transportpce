/*
 * Copyright © 2024 Orange, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.tapi.impl.rpc;

import com.google.common.util.concurrent.ListenableFuture;
import org.opendaylight.transportpce.tapi.utils.TapiContext;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.Uuid;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev221121.GetConnectivityServiceDetails;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev221121.GetConnectivityServiceDetailsInput;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev221121.GetConnectivityServiceDetailsOutput;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev221121.GetConnectivityServiceDetailsOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev221121.connectivity.context.ConnectivityService;
import org.opendaylight.yangtools.yang.common.ErrorType;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class GetConnectivityServiceDetailsImpl implements GetConnectivityServiceDetails {
    private static final Logger LOG = LoggerFactory.getLogger(GetConnectivityServiceDetailsImpl.class);

    private final TapiContext tapiContext;

    public GetConnectivityServiceDetailsImpl(TapiContext tapiContext) {
        this.tapiContext = tapiContext;
    }


    @Override
    public ListenableFuture<RpcResult<GetConnectivityServiceDetailsOutput>> invoke(
            GetConnectivityServiceDetailsInput input) {
        // TODO Auto-generated method stub
        Uuid serviceUuid = input.getUuid();
        ConnectivityService service = this.tapiContext.getConnectivityService(serviceUuid);
        if (service == null) {
            LOG.error("Service {} doesnt exist in tapi context", input.getUuid());
            return RpcResultBuilder.<GetConnectivityServiceDetailsOutput>failed()
                .withError(ErrorType.RPC, "Service doesnt exist in datastore")
                .buildFuture();
        }
        return RpcResultBuilder.success(new GetConnectivityServiceDetailsOutputBuilder().setService(
            new org.opendaylight.yang.gen.v1.urn
                .onf.otcc.yang.tapi.connectivity.rev221121.get.connectivity.service.details.output.ServiceBuilder(
                    service).build()).build()).buildFuture();
    }

}
