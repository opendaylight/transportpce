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
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.device.renderer.rev200128.RendererRollbackInput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.device.renderer.rev200128.RendererRollbackInputBuilder;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.device.renderer.rev200128.ServicePathInput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.device.renderer.rev200128.ServicePathInputBuilder;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.renderer.rev201125.ServiceImplementationRequestInput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.renderer.rev201125.ServiceImplementationRequestInputBuilder;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.renderer.rev201125.service.implementation.request.input.PathDescription;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.renderer.rev201125.service.implementation.request.input.PathDescriptionBuilder;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.renderer.rev201125.service.implementation.request.input.ServiceAEndBuilder;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.renderer.rev201125.service.implementation.request.input.ServiceZEndBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev190531.ConnectionType;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev190531.service.port.PortBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.format.rev190531.ServiceFormat;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev210705.path.description.AToZDirection;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev210705.path.description.AToZDirectionBuilder;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev210705.path.description.ZToADirection;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev210705.path.description.ZToADirectionBuilder;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev210705.path.description.atoz.direction.AToZ;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev210705.path.description.atoz.direction.AToZBuilder;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev210705.path.description.atoz.direction.AToZKey;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev210705.path.description.ztoa.direction.ZToA;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev210705.path.description.ztoa.direction.ZToABuilder;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev210705.path.description.ztoa.direction.ZToAKey;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev210705.pce.resource.ResourceBuilder;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev210705.pce.resource.resource.resource.Link;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev210705.pce.resource.resource.resource.LinkBuilder;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev210705.pce.resource.resource.resource.Node;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev210705.pce.resource.resource.resource.NodeBuilder;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev210705.pce.resource.resource.resource.TerminationPoint;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev210705.pce.resource.resource.resource.TerminationPointBuilder;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.service.types.rev200128.service.endpoint.sp.RxDirectionBuilder;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.service.types.rev200128.service.endpoint.sp.TxDirectionBuilder;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.service.types.rev200128.service.handler.header.ServiceHandlerHeaderBuilder;
import org.opendaylight.yang.gen.v1.http.org.transportpce.common.types.rev201211.olm.renderer.input.Nodes;
import org.opendaylight.yang.gen.v1.http.org.transportpce.common.types.rev201211.olm.renderer.input.NodesBuilder;
import org.opendaylight.yang.gen.v1.http.org.transportpce.common.types.rev201211.olm.renderer.input.NodesKey;
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

    /*public static ServiceImplementationRequestInput buildServiceImplementationRequestInputInvalidResource() {
        ServiceImplementationRequestInputBuilder builder = new ServiceImplementationRequestInputBuilder()
            .setServiceName("service 1").setPathDescription(createPathDescriptionInvalidResource())
            .setServiceHandlerHeader(new ServiceHandlerHeaderBuilder().setRequestId("Request 1").build())
            .setServiceAEnd(getServiceAEndBuild().build())
            .setServiceZEnd(getServiceZEndBuild().build());
        return builder.build();
    }

    public static ServiceImplementationRequestInput buildServiceImplementationRequestInputLinkResource() {
        ServiceImplementationRequestInputBuilder builder = new ServiceImplementationRequestInputBuilder()
            .setServiceName("service 1").setPathDescription(createPathDescriptionLinkResource())
            .setServiceHandlerHeader(new ServiceHandlerHeaderBuilder().setRequestId("Request 1").build())
            .setServiceAEnd(getServiceAEndBuild().build())
            .setServiceZEnd(getServiceZEndBuild().build());
        return builder.build();
    }*/

    private static PathDescription createPathDescriptionInvalidResource() {
        Map<AToZKey,AToZ> atoZMap = new HashMap<>();
        Node node = new NodeBuilder().setNodeId("XPONDER-1-2").build();
        AToZ atoZ = new AToZBuilder().setId("1").withKey(new AToZKey("1")).setResource(new ResourceBuilder()
            .setResource(node).build()).build();
        atoZMap.put(atoZ.key(),atoZ);
        AToZDirection atozDirection = new AToZDirectionBuilder()
            .setRate(Uint32.valueOf(20))
            .setAToZWavelengthNumber(Uint32.valueOf(32))
            .setAToZ(atoZMap)
            .build();
        Map<ZToAKey,ZToA> ztoAMap = new HashMap<>();
        ZToA ztoA = new ZToABuilder().setId("1").withKey(new ZToAKey("1")).setResource(new ResourceBuilder()
            .setResource(node).build()).build();
        ztoAMap.put(ztoA.key(),ztoA);
        ZToADirection ztoaDirection = new ZToADirectionBuilder()
            .setRate(Uint32.valueOf(20))
            .setZToAWavelengthNumber(Uint32.valueOf(20))
            .setZToA(ztoAMap)
            .build();
        PathDescriptionBuilder builder = new PathDescriptionBuilder()
            .setAToZDirection(atozDirection)
            .setZToADirection(ztoaDirection);
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
            .setZToA(ztoAMap)
            .build();
        PathDescriptionBuilder builder = new PathDescriptionBuilder()
            .setAToZDirection(atozDirection)
            .setZToADirection(ztoaDirection);
        return builder.build();
    }

    private static PathDescription createPathDescriptionLinkResource() {
        Map<AToZKey,AToZ> atoZMap = new HashMap<>();
        Link link1 = new LinkBuilder().setLinkId("link 1").build();
        Link link2 = new LinkBuilder().setLinkId("link 2").build();
        AToZ atoZ = new AToZBuilder().setId("1").withKey(new AToZKey("1")).setResource(new ResourceBuilder()
            .setResource(link1).build()).build();
        AToZ atoZ2 = new AToZBuilder().setId("1").withKey(new AToZKey("1")).setResource(new ResourceBuilder()
            .setResource(link2).build()).build();
        atoZMap.put(atoZ.key(),atoZ);
        atoZMap.put(atoZ2.key(),atoZ2);
        AToZDirection atozDirection = new AToZDirectionBuilder()
            .setRate(Uint32.valueOf(20))
            .setAToZWavelengthNumber(Uint32.valueOf(20))
            .setAToZ(atoZMap)
            .build();
        Map<ZToAKey,ZToA> ztoAMap = new HashMap<>();
        ZToA ztoA = new ZToABuilder().setId("1").withKey(new ZToAKey("1")).setResource(new ResourceBuilder()
            .setResource(link1).build()).build();
        ZToA ztoA2 = new ZToABuilder().setId("1").withKey(new ZToAKey("1")).setResource(new ResourceBuilder()
            .setResource(link2).build()).build();
        ztoAMap.put(ztoA.key(),ztoA);
        ztoAMap.put(ztoA2.key(),ztoA2);
        ZToADirection ztoaDirection = new ZToADirectionBuilder()
            .setRate(Uint32.valueOf(20))
            .setZToAWavelengthNumber(Uint32.valueOf(20))
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
            .setServiceRate(Uint32.valueOf(1)).setNodeId("XPONDER-1-2-"
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
            .setServiceRate(Uint32.valueOf(1)).setNodeId("XPONDER-2-3-"
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
