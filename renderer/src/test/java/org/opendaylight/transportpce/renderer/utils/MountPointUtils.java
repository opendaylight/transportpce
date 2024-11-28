/*
 * Copyright © 2018 Orange Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.renderer.utils;


import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;

import com.google.common.util.concurrent.ListenableFuture;
import java.util.List;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.binding.api.RpcService;
import org.opendaylight.transportpce.test.stub.MountPointStub;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev240315.mapping.Mapping;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev240315.mapping.MappingBuilder;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev240315.mapping.MappingKey;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.GetConnectionPortTrail;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.GetConnectionPortTrailOutput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.GetConnectionPortTrailOutputBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.get.connection.port.trail.output.Ports;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;


public final class MountPointUtils {

    public static MountPointStub getMountPoint(List<Ports> ports, DataBroker dataBroker) {
        RpcService rpcService = spy(RpcService.class);
        GetConnectionPortTrail getConnectionPortTrail = spy(GetConnectionPortTrail.class);
        GetConnectionPortTrailOutputBuilder getConnectionPortTrailOutputBldr
                = new GetConnectionPortTrailOutputBuilder();
        getConnectionPortTrailOutputBldr.setPorts(ports);
        ListenableFuture<RpcResult<GetConnectionPortTrailOutput>> rpcResultFuture =
                RpcResultBuilder.success(getConnectionPortTrailOutputBldr.build()).buildFuture();
        doReturn(rpcResultFuture).when(getConnectionPortTrail.invoke(any()));
        doReturn(getConnectionPortTrail).when(rpcService).getRpc(any());
        MountPointStub mountPoint = new MountPointStub(dataBroker);
        mountPoint.setRpcService(rpcService);
        return mountPoint;
    }

    public static Mapping createMapping(String nodeId, String logicalConnPoint) {
        return new MappingBuilder()
            .withKey(new MappingKey(logicalConnPoint))
            .setLogicalConnectionPoint(logicalConnPoint)
            .setSupportingOts("supporting-OTS")
            .setSupportingCircuitPackName("2/0")
            .setSupportingOms("supporting-OMS")
            .setSupportingPort("port")
            .setSupportingCircuitPackName("circuit-pack")
            .build();
    }

    private MountPointUtils() {
    }

}
