/*
 * Copyright © 2018 Orange Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.renderer.utils;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import org.mockito.Mockito;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.sal.binding.api.RpcConsumerRegistry;
import org.opendaylight.transportpce.common.device.DeviceTransactionManager;
import org.opendaylight.transportpce.renderer.stub.MountPointStub;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.GetConnectionPortTrailOutput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.GetConnectionPortTrailOutputBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.OrgOpenroadmDeviceService;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.get.connection.port.trail.output.Ports;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.portmapping.rev170228.Network;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.portmapping.rev170228.network.Nodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.portmapping.rev170228.network.NodesKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.portmapping.rev170228.network.nodes.Mapping;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.portmapping.rev170228.network.nodes.MappingBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.portmapping.rev170228.network.nodes.MappingKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;

public class MountPointUtils {

    public static MountPointStub getMountPoint(List<Ports> ports, DataBroker dataBroker) {
        RpcConsumerRegistry rpcConsumerRegistry = Mockito.spy(RpcConsumerRegistry.class);
        OrgOpenroadmDeviceService orgOpenroadmDeviceService = Mockito.spy(OrgOpenroadmDeviceService.class);
        GetConnectionPortTrailOutputBuilder getConnectionPortTrailOutputBldr
                = new GetConnectionPortTrailOutputBuilder();
        getConnectionPortTrailOutputBldr.setPorts(ports);
        RpcResultBuilder<GetConnectionPortTrailOutput> rpcResultBuilder =
                RpcResultBuilder.success(getConnectionPortTrailOutputBldr.build());
        Future<RpcResult<GetConnectionPortTrailOutput>> rpcResultFuture =
                CompletableFuture.completedFuture(rpcResultBuilder.build());
        Mockito.doReturn(rpcResultFuture).when(orgOpenroadmDeviceService).getConnectionPortTrail(Mockito.any());
        Mockito.doReturn(orgOpenroadmDeviceService).when(rpcConsumerRegistry).getRpcService(Mockito.any());
        MountPointStub mountPoint = new MountPointStub(dataBroker);
        mountPoint.setRpcConsumerRegistry(rpcConsumerRegistry);
        return mountPoint;
    }

    public static boolean writeMapping(String nodeId, String logicalConnPoint,
                                       DeviceTransactionManager deviceTransactionManager) {
        MappingBuilder mappingBuilder = new MappingBuilder();
        mappingBuilder.setKey(new MappingKey(logicalConnPoint));
        mappingBuilder.setLogicalConnectionPoint(logicalConnPoint);
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
}
