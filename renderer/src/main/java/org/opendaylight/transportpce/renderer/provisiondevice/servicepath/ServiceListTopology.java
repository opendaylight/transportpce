/*
 * Copyright © 2017 AT&T and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.renderer.provisiondevice.servicepath;

import java.util.ArrayList;
import java.util.List;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.node.types.rev191129.NodeIdType;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev191129.service.Topology;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev191129.service.TopologyBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.get.connection.port.trail.output.Ports;
import org.opendaylight.yang.gen.v1.http.org.openroadm.resource.rev191129.resource.DeviceBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.resource.rev191129.resource.ResourceBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.resource.rev191129.resource.ResourceTypeBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.resource.rev191129.resource.resource.resource.port.PortBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.resource.types.rev191129.ResourceTypeEnum;
import org.opendaylight.yang.gen.v1.http.org.openroadm.topology.rev191129.Hop.HopType;
import org.opendaylight.yang.gen.v1.http.org.openroadm.topology.rev191129.topology.AToZ;
import org.opendaylight.yang.gen.v1.http.org.openroadm.topology.rev191129.topology.AToZBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.topology.rev191129.topology.ZToA;
import org.opendaylight.yang.gen.v1.http.org.openroadm.topology.rev191129.topology.ZToABuilder;

public class ServiceListTopology {


    private List<AToZ> a2zTopologyList;
    private List<ZToA> z2aTopologyList;
    private TopologyBuilder serviceTopology;

    public ServiceListTopology() {

        this.a2zTopologyList = new ArrayList<>();
        this.z2aTopologyList = new ArrayList<>();
        this.serviceTopology = new TopologyBuilder();

    }

    public void updateAtoZTopologyList(List<Ports> ports, String nodeId) {

        String circuitPackName = "";
        String portName = "";

        int id = this.a2zTopologyList.size();

        DeviceBuilder deviceBldr = new DeviceBuilder();
        deviceBldr.setNodeId(new NodeIdType(nodeId));


        for (Ports port : ports) {

            id = id + 1;

            //Get circuitpack name
            circuitPackName = port.getCircuitPackName();

            //Get port name
            portName = port.getPortName().toString();

            AToZBuilder a2zBldr = new AToZBuilder();

            //Set Resource Id
            a2zBldr.setId(Integer.toString(id));

            //Set device Node-id
            a2zBldr.setDevice(deviceBldr.build());

            //Set hop type to internal
            a2zBldr.setHopType(HopType.NodeInternal);

            //Set Resource Type to port
            ResourceTypeBuilder rsrcTypeBldr = new ResourceTypeBuilder();
            rsrcTypeBldr.setType(ResourceTypeEnum.Port);
            a2zBldr.setResourceType(rsrcTypeBldr.build());

            //building port resource
            PortBuilder portBldr = new PortBuilder();
            portBldr.setCircuitPackName(circuitPackName);
            portBldr.setPortName(portName);
            org.opendaylight.yang.gen.v1.http.org.openroadm.resource.rev191129.resource.resource.resource
                    .PortBuilder portCase =
                    new org.opendaylight.yang.gen.v1.http.org.openroadm.resource.rev191129.resource.resource.resource
                            .PortBuilder();
            portCase.setPort(portBldr.build());
            ResourceBuilder rsrcBldr = new ResourceBuilder();
            rsrcBldr.setResource(portCase.build());
            a2zBldr.setResource(rsrcBldr.build());

            //Add port resource to the list
            this.a2zTopologyList.add(a2zBldr.build());

        }

        //update Topology
        this.serviceTopology.setAToZ(this.a2zTopologyList);

    }

    public void updateZtoATopologyList(List<Ports> ports, String nodeId) {

        String circuitPackName = "";
        String portName = "";

        int id = this.z2aTopologyList.size();

        DeviceBuilder deviceBldr = new DeviceBuilder();
        deviceBldr.setNodeId(new NodeIdType(nodeId));

        for (Ports port : ports) {

            id = id + 1;

            //Get circuitpack name
            circuitPackName = port.getCircuitPackName();

            //Get port name
            portName = port.getPortName().toString();

            ZToABuilder z2aBldr = new ZToABuilder();

            //Set Resource Id
            z2aBldr.setId(Integer.toString(id));

            //Set device Node-id
            z2aBldr.setDevice(deviceBldr.build());

            //Set hop type to internal
            z2aBldr.setHopType(HopType.NodeInternal);

            //Set Resource Type to port
            ResourceTypeBuilder rsrcTypeBldr = new ResourceTypeBuilder();
            rsrcTypeBldr.setType(ResourceTypeEnum.Port);
            z2aBldr.setResourceType(rsrcTypeBldr.build());

            //building port resource
            PortBuilder portBldr = new PortBuilder();
            portBldr.setCircuitPackName(circuitPackName);
            portBldr.setPortName(portName);
            org.opendaylight.yang.gen.v1.http.org.openroadm.resource.rev191129.resource.resource.resource
                    .PortBuilder portCase =
                    new org.opendaylight.yang.gen.v1.http.org.openroadm.resource.rev191129.resource.resource.resource
                            .PortBuilder();
            portCase.setPort(portBldr.build());
            ResourceBuilder rsrcBldr = new ResourceBuilder();
            rsrcBldr.setResource(portCase.build());
            z2aBldr.setResource(rsrcBldr.build());

            //Add port resource to the list
            this.z2aTopologyList.add(z2aBldr.build());

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
