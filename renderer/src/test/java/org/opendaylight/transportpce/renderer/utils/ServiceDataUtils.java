/*
 * Copyright © 2018 Orange Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.renderer.utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.device.renderer.rev211004.RendererRollbackInput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.device.renderer.rev211004.RendererRollbackInputBuilder;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.device.renderer.rev211004.ServicePathInput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.device.renderer.rev211004.ServicePathInputBuilder;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.renderer.rev210915.ServiceImplementationRequestInput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.renderer.rev210915.ServiceImplementationRequestInputBuilder;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.renderer.rev210915.service.implementation.request.input.PathDescription;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.renderer.rev210915.service.implementation.request.input.PathDescriptionBuilder;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.renderer.rev210915.service.implementation.request.input.ServiceAEndBuilder;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.renderer.rev210915.service.implementation.request.input.ServiceZEndBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.optical.channel.types.rev230526.FrequencyTHz;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev230526.ConnectionType;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev230526.service.port.PortBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.format.rev191129.ServiceFormat;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev230501.path.description.AToZDirection;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev230501.path.description.AToZDirectionBuilder;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev230501.path.description.ZToADirection;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev230501.path.description.ZToADirectionBuilder;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev230501.path.description.atoz.direction.AToZ;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev230501.path.description.atoz.direction.AToZBuilder;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev230501.path.description.atoz.direction.AToZKey;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev230501.path.description.ztoa.direction.ZToA;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev230501.path.description.ztoa.direction.ZToABuilder;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev230501.path.description.ztoa.direction.ZToAKey;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev230501.pce.resource.ResourceBuilder;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev230501.pce.resource.resource.resource.TerminationPoint;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev230501.pce.resource.resource.resource.TerminationPointBuilder;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.service.types.rev220118.service.endpoint.sp.RxDirectionBuilder;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.service.types.rev220118.service.endpoint.sp.TxDirectionBuilder;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.service.types.rev220118.service.handler.header.ServiceHandlerHeaderBuilder;
import org.opendaylight.yang.gen.v1.http.org.transportpce.common.types.rev220926.optical.renderer.nodes.Nodes;
import org.opendaylight.yang.gen.v1.http.org.transportpce.common.types.rev220926.optical.renderer.nodes.NodesBuilder;
import org.opendaylight.yang.gen.v1.http.org.transportpce.common.types.rev220926.optical.renderer.nodes.NodesKey;
import org.opendaylight.yangtools.yang.common.Uint32;


public final class ServiceDataUtils {

    private ServiceDataUtils() {
    }

    public static Nodes createNode(String nodeId, String srcTp, String dstTp) {
        return new NodesBuilder().setNodeId(nodeId).withKey(new NodesKey(nodeId)).setSrcTp(srcTp)
                .setDestTp(dstTp).build();
    }

    public static ServicePathInput buildServicePathInputs(List<Nodes> nodes) {
        ServicePathInputBuilder servicePathInputBuilder = new ServicePathInputBuilder();
        servicePathInputBuilder.setNodes(nodes);
        servicePathInputBuilder.setServiceName("Service 1").setWaveNumber(Uint32.valueOf(20));
        return servicePathInputBuilder.build();
    }

    public static ServicePathInput buildServicePathInputs() {
        ServicePathInputBuilder servicePathInputBuilder = new ServicePathInputBuilder();
        List<Nodes> nodes = new ArrayList<>();
        nodes.add(ServiceDataUtils.createNode("node1", "src", "dst"));
        servicePathInputBuilder.setNodes(nodes);
        servicePathInputBuilder.setServiceName("Service 1").setWaveNumber(Uint32.valueOf(20));
        return servicePathInputBuilder.build();
    }

    public static RendererRollbackInput buildRendererRollbackInput() {
        RendererRollbackInputBuilder rendererRollbackInputBuilder = new RendererRollbackInputBuilder();
        rendererRollbackInputBuilder.setNodeInterface(Map.of());
        return rendererRollbackInputBuilder.build();
    }

    public static ServiceImplementationRequestInput buildServiceImplementationRequestInputTerminationPointResource(
        String tpId) {
        ServiceImplementationRequestInputBuilder builder = new ServiceImplementationRequestInputBuilder()
            .setServiceName("service 1").setPathDescription(createPathDescriptionTerminationPointResource(tpId))
            .setServiceHandlerHeader(new ServiceHandlerHeaderBuilder().setRequestId("Request 1").build())
            .setServiceAEnd(getServiceAEndBuild(tpId).build())
            .setServiceZEnd(getServiceZEndBuild(tpId).build())
            .setConnectionType(ConnectionType.Service);
        return builder.build();
    }

    public static PathDescription createPathDescriptionTerminationPointResource(String tpId) {
        Map<AToZKey,AToZ> atoZMap = new HashMap<>();
        TerminationPointBuilder terminationPointBuilder = new TerminationPointBuilder();
        List<String> nodeIds = Arrays.asList("XPONDER-1-2", "XPONDER-2-3");
        Integer atozId = 1;
        for (String nodeId : nodeIds) {
            for (String otherNodeId : nodeIds) {
                TerminationPoint terminationPoint = terminationPointBuilder
                    .setTpNodeId(nodeId + '-'
                            + tpId)
                        .setTpId(tpId).build();
                AToZ atoZ = new AToZBuilder().setId(atozId.toString())
                    .withKey(new AToZKey(atozId.toString())).setResource(new ResourceBuilder()
                        .setResource(terminationPoint).build()).build();
                atozId++;
                atoZMap.put(atoZ.key(),atoZ);
            }
        }
        AToZDirection atozDirection = new AToZDirectionBuilder()
            .setRate(Uint32.valueOf(20))
            .setAToZWavelengthNumber(Uint32.valueOf(20))
            .setAToZMinFrequency(FrequencyTHz.getDefaultInstance("196.125"))
            .setAToZMaxFrequency(FrequencyTHz.getDefaultInstance("196.075"))
            .setAToZ(atoZMap)
            .build();

        Collections.reverse(nodeIds);
        Map<ZToAKey,ZToA> ztoAMap = new HashMap<>();
        Integer ztoaId = 1;
        for (String nodeId : nodeIds) {
            for (String otherNodeId : nodeIds) {
                TerminationPoint terminationPoint = terminationPointBuilder
                    .setTpNodeId(nodeId + '-'
                            + tpId)
                        .setTpId(tpId).build();
                ZToA ztoA = new ZToABuilder().setId(ztoaId.toString())
                    .withKey(new ZToAKey(ztoaId.toString())).setResource(new ResourceBuilder()
                        .setResource(terminationPoint).build()).build();
                ztoaId++;
                ztoAMap.put(ztoA.key(),ztoA);
            }
        }
        ZToADirection ztoaDirection = new ZToADirectionBuilder()
            .setRate(Uint32.valueOf(20))
            .setZToAWavelengthNumber(Uint32.valueOf(20))
            .setZToAMinFrequency(FrequencyTHz.getDefaultInstance("196.125"))
            .setZToAMaxFrequency(FrequencyTHz.getDefaultInstance("196.075"))
            .setZToA(ztoAMap)
            .build();
        PathDescriptionBuilder builder = new PathDescriptionBuilder()
            .setAToZDirection(atozDirection)
            .setZToADirection(ztoaDirection);
        return builder.build();
    }

    public static ServiceAEndBuilder getServiceAEndBuild(String tpId) {
        return new ServiceAEndBuilder()
            .setClli("clli").setServiceFormat(ServiceFormat.OC)
            .setServiceRate(Uint32.ONE).setNodeId("XPONDER-1-2-"
                    + tpId)
            .setTxDirection(
                new TxDirectionBuilder()
                    .setPort(new PortBuilder().setPortDeviceName("device name").setPortName("port name")
                        .setPortRack("port rack").setPortShelf("port shelf").setPortSlot("port slot")
                        .setPortSubSlot("port subslot").setPortType("port type").build())
                    .build())
            .setRxDirection(
                new RxDirectionBuilder()
                    .setPort(new PortBuilder().setPortDeviceName("device name").setPortName("port name")
                        .setPortRack("port rack").setPortShelf("port shelf").setPortSlot("port slot")
                        .setPortSubSlot("port subslot").setPortType("port type").build())
                    .build())
            .setServiceRate(Uint32.valueOf(100))
                .setServiceFormat(ServiceFormat.Ethernet);
    }

    public static ServiceZEndBuilder getServiceZEndBuild(String tpId) {
        return new ServiceZEndBuilder()
            .setClli("clli").setServiceFormat(ServiceFormat.OC)
            .setServiceRate(Uint32.ONE).setNodeId("XPONDER-2-3-"
                    + tpId)
            .setTxDirection(
                new TxDirectionBuilder()
                    .setPort(new PortBuilder().setPortDeviceName("device name").setPortName("port name")
                        .setPortRack("port rack").setPortShelf("port shelf").setPortSlot("port slot")
                        .setPortSubSlot("port subslot").setPortType("port type").build())
                    .build())
            .setRxDirection(
                new RxDirectionBuilder()
                    .setPort(new PortBuilder().setPortDeviceName("device name").setPortName("port name")
                        .setPortRack("port rack").setPortShelf("port shelf").setPortSlot("port slot")
                        .setPortSubSlot("port subslot").setPortType("port type").build())
                    .build());
    }
}
