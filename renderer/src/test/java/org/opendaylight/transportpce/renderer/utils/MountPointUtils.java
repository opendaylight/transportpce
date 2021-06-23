/*
 * Copyright © 2018 Orange Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.renderer.utils;

import com.google.common.util.concurrent.ListenableFuture;
import java.util.List;
import java.util.concurrent.ExecutionException;
import org.mockito.Mockito;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.binding.api.RpcConsumerRegistry;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.transportpce.common.device.DeviceTransactionManager;
import org.opendaylight.transportpce.test.stub.MountPointStub;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev210425.Network;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev210425.mapping.Mapping;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev210425.mapping.MappingBuilder;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev210425.mapping.MappingKey;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev210425.network.Nodes;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev210425.network.NodesKey;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.GetConnectionPortTrailOutput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.GetConnectionPortTrailOutputBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.OrgOpenroadmDeviceService;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.get.connection.port.trail.output.Ports;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;


public final class MountPointUtils {

    public static MountPointStub getMountPoint(List<Ports> ports, DataBroker dataBroker) {
        RpcConsumerRegistry rpcConsumerRegistry = Mockito.spy(RpcConsumerRegistry.class);
        OrgOpenroadmDeviceService orgOpenroadmDeviceService = Mockito.spy(OrgOpenroadmDeviceService.class);
        GetConnectionPortTrailOutputBuilder getConnectionPortTrailOutputBldr
                = new GetConnectionPortTrailOutputBuilder();
        getConnectionPortTrailOutputBldr.setPorts(ports);
        ListenableFuture<RpcResult<GetConnectionPortTrailOutput>> rpcResultFuture =
                RpcResultBuilder.success(getConnectionPortTrailOutputBldr.build()).buildFuture();
        Mockito.doReturn(rpcResultFuture).when(orgOpenroadmDeviceService).getConnectionPortTrail(Mockito.any());
        Mockito.doReturn(orgOpenroadmDeviceService).when(rpcConsumerRegistry).getRpcService(Mockito.any());
        MountPointStub mountPoint = new MountPointStub(dataBroker);
        mountPoint.setRpcConsumerRegistry(rpcConsumerRegistry);
        return mountPoint;
    }

    public static boolean writeMapping(String nodeId, String logicalConnPoint,
                                       DeviceTransactionManager deviceTransactionManager) {
        MappingBuilder mappingBuilder = new MappingBuilder();
        mappingBuilder.withKey(new MappingKey(logicalConnPoint));
        mappingBuilder.setLogicalConnectionPoint(logicalConnPoint);
        mappingBuilder.setSupportingOts("OTS");
        mappingBuilder.setSupportingCircuitPackName("2/0");
        mappingBuilder.setSupportingOms("OMS");
        mappingBuilder.setSupportingPort("8080");
        mappingBuilder.setSupportingCircuitPackName("circuit1");
        InstanceIdentifier<Mapping> portMappingIID =
                InstanceIdentifier.builder(Network.class).child(Nodes.class, new NodesKey(nodeId))
                        .child(Mapping.class, new MappingKey(logicalConnPoint)).build();
        try {
            return TransactionUtils.writeTransaction(deviceTransactionManager,
                    nodeId, LogicalDatastoreType.CONFIGURATION, portMappingIID, mappingBuilder.build());
        } catch (ExecutionException | InterruptedException e) {
            return false;
        }
    }

    private MountPointUtils() {

    }

}
