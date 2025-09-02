/*
 * Copyright © 2017 AT&T and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.renderer.provisiondevice.servicepath;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.node.types.rev210528.NodeIdType;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev250110.service.Topology;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev250110.service.TopologyBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.get.connection.port.trail.output.Ports;
import org.opendaylight.yang.gen.v1.http.org.openroadm.resource.rev250110.resource.DeviceBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.resource.rev250110.resource.ResourceBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.resource.rev250110.resource.ResourceTypeBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.resource.rev250110.resource.resource.resource.port.PortBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.resource.types.rev250110.ResourceTypeEnum;
import org.opendaylight.yang.gen.v1.http.org.openroadm.topology.rev250110.Hop.HopType;
import org.opendaylight.yang.gen.v1.http.org.openroadm.topology.rev250110.topology.AToZ;
import org.opendaylight.yang.gen.v1.http.org.openroadm.topology.rev250110.topology.AToZBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.topology.rev250110.topology.AToZKey;
import org.opendaylight.yang.gen.v1.http.org.openroadm.topology.rev250110.topology.ZToA;
import org.opendaylight.yang.gen.v1.http.org.openroadm.topology.rev250110.topology.ZToABuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.topology.rev250110.topology.ZToAKey;

public class ServiceListTopology {


    private Map<AToZKey,AToZ> a2zTopologyList = new HashMap<>();
    private Map<ZToAKey,ZToA> z2aTopologyList = new HashMap<>();
    private TopologyBuilder serviceTopology = new TopologyBuilder();

    public void updateAtoZTopologyList(List<Ports> ports, String nodeId) {

        int id = this.a2zTopologyList.size();
        for (Ports port : ports) {
            id = id + 1;
            //Add port resource to the list
            AToZ a2z = new AToZBuilder()
                .setId(Integer.toString(id))
                .setDevice(new DeviceBuilder()
                    .setNodeId(new NodeIdType(nodeId))
                    .build())
                .setHopType(HopType.NodeInternal)
                .setResourceType(new ResourceTypeBuilder()
                    .setType(ResourceTypeEnum.Port)
                    .build())
                .setResource(new ResourceBuilder()
                    .setResource(new org.opendaylight.yang.gen.v1.http.org.openroadm.resource.rev250110
                            .resource.resource.resource.PortBuilder()
                        .setPort(new PortBuilder()
                            .setCircuitPackName(port.getCircuitPackName())
                            .setPortName(port.getPortName())
                            .build())
                        .build())
                    .build())
                .build();
            this.a2zTopologyList.put(a2z.key(), a2z);
        }

        //update Topology
        this.serviceTopology.setAToZ(this.a2zTopologyList);

    }

    public void updateZtoATopologyList(List<Ports> ports, String nodeId) {

        int id = this.z2aTopologyList.size();

        for (Ports port : ports) {
            id = id + 1;
            //Add port resource to the list
            ZToA z2a = new ZToABuilder()
                .setId(Integer.toString(id))
                .setDevice(new DeviceBuilder()
                    .setNodeId(new NodeIdType(nodeId))
                    .build())
                .setHopType(HopType.NodeInternal)
                .setResourceType(new ResourceTypeBuilder()
                    .setType(ResourceTypeEnum.Port)
                    .build())
                .setResource(new ResourceBuilder()
                    .setResource(new org.opendaylight.yang.gen.v1.http.org.openroadm.resource.rev250110
                            .resource.resource.resource.PortBuilder()
                        .setPort(new PortBuilder()
                            .setCircuitPackName(port.getCircuitPackName())
                            .setPortName(port.getPortName())
                            .build())
                        .build())
                    .build())
                .build();
            this.z2aTopologyList.put(z2a.key(),z2a);
        }

        //update Topology
        this.serviceTopology.setZToA(this.z2aTopologyList);

    }

    public Topology getTopology() {
        this.serviceTopology.setAToZ(this.a2zTopologyList);
        this.serviceTopology.setZToA(this.z2aTopologyList);
        return this.serviceTopology.build();
    }

}
