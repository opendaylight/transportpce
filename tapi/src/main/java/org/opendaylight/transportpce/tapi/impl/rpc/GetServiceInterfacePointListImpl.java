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
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.GetServiceInterfacePointList;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.GetServiceInterfacePointListInput;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.GetServiceInterfacePointListOutput;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.GetServiceInterfacePointListOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.get.service._interface.point.list.output.Sip;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.get.service._interface.point.list.output.SipBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.get.service._interface.point.list.output.SipKey;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.tapi.context.ServiceInterfacePoint;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.tapi.context.ServiceInterfacePointKey;
import org.opendaylight.yangtools.yang.common.ErrorType;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class GetServiceInterfacePointListImpl implements GetServiceInterfacePointList {
    private static final Logger LOG = LoggerFactory.getLogger(GetServiceInterfacePointListImpl.class);
    private final TapiContext tapiContext;

    public GetServiceInterfacePointListImpl(TapiContext tapiContext) {
        this.tapiContext = tapiContext;
    }

    @Override
    public ListenableFuture<RpcResult<GetServiceInterfacePointListOutput>> invoke(
            GetServiceInterfacePointListInput input) {
        Map<ServiceInterfacePointKey, ServiceInterfacePoint> sips =
                this.tapiContext.getTapiContext().getServiceInterfacePoint();
        if (sips == null || sips.isEmpty()) {
            return RpcResultBuilder.<GetServiceInterfacePointListOutput>failed()
                .withError(ErrorType.RPC, "No sips in datastore")
                .buildFuture();
        }
        Map<SipKey, Sip> outSipMap = new HashMap<>();
        for (ServiceInterfacePoint sip : sips.values()) {
            Sip si = new SipBuilder(sip).build();
            outSipMap.put(si.key(), si);
        }
        return RpcResultBuilder
            .success(new GetServiceInterfacePointListOutputBuilder().setSip(outSipMap).build())
            .buildFuture();
    }

}
