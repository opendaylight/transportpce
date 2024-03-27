/*
 * Copyright © 2024 Orange, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.tapi.impl.rpc;

import com.google.common.util.concurrent.ListenableFuture;
import java.util.Map;
import org.opendaylight.transportpce.tapi.utils.TapiContext;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.GetServiceInterfacePointDetails;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.GetServiceInterfacePointDetailsInput;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.GetServiceInterfacePointDetailsOutput;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.GetServiceInterfacePointDetailsOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.get.service._interface.point.details.output.SipBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.tapi.context.ServiceInterfacePoint;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.tapi.context.ServiceInterfacePointKey;
import org.opendaylight.yangtools.yang.common.ErrorType;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class GetServiceInterfacePointDetailsImpl implements GetServiceInterfacePointDetails {
    private static final Logger LOG = LoggerFactory.getLogger(GetServiceInterfacePointDetailsImpl.class);
    private final TapiContext tapiContext;

    public GetServiceInterfacePointDetailsImpl(TapiContext tapiContext) {
        this.tapiContext = tapiContext;
    }

    @Override
    public ListenableFuture<RpcResult<GetServiceInterfacePointDetailsOutput>> invoke(
            GetServiceInterfacePointDetailsInput input) {
        Map<ServiceInterfacePointKey, ServiceInterfacePoint> sips =
            this.tapiContext.getTapiContext().getServiceInterfacePoint();
        if (sips == null || sips.isEmpty()) {
            return RpcResultBuilder.<GetServiceInterfacePointDetailsOutput>failed()
                .withError(ErrorType.RPC, "No sips in datastore")
                .buildFuture();
        }
        var sipKey = new ServiceInterfacePointKey(input.getUuid());
        if (!sips.containsKey(sipKey)) {
            return RpcResultBuilder.<GetServiceInterfacePointDetailsOutput>failed()
                .withError(ErrorType.RPC, "Sip doesnt exist in datastore")
                .buildFuture();
        }
        return RpcResultBuilder
                .success(new GetServiceInterfacePointDetailsOutputBuilder()
                    .setSip(new SipBuilder(sips.get(sipKey)).build())
                    .build())
                .buildFuture();
    }

}
