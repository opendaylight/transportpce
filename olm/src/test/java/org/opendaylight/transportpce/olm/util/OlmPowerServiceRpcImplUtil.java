/*
 * Copyright © 2018 Orange, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.olm.util;

import java.util.ArrayList;
import java.util.List;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.olm.rev170418.CalculateSpanlossBaseInput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.olm.rev170418.CalculateSpanlossBaseInputBuilder;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.olm.rev170418.CalculateSpanlossCurrentInput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.olm.rev170418.CalculateSpanlossCurrentInputBuilder;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.olm.rev170418.GetPmInput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.olm.rev170418.GetPmInputBuilder;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.olm.rev170418.ServicePowerResetInput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.olm.rev170418.ServicePowerResetInputBuilder;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.olm.rev170418.ServicePowerSetupInput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.olm.rev170418.ServicePowerSetupInputBuilder;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.olm.rev170418.ServicePowerTurndownInput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.olm.rev170418.ServicePowerTurndownInputBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev200529.Link1;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev200529.Link1Builder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.pm.types.rev161014.PmGranularity;
import org.opendaylight.yang.gen.v1.http.org.openroadm.resource.types.rev161014.ResourceTypeEnum;
import org.opendaylight.yang.gen.v1.http.org.transportpce.common.types.rev200615.olm.get.pm.input.ResourceIdentifierBuilder;
import org.opendaylight.yang.gen.v1.http.org.transportpce.common.types.rev200615.olm.renderer.input.Nodes;
import org.opendaylight.yang.gen.v1.http.org.transportpce.common.types.rev200615.olm.renderer.input.NodesBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.LinkId;
import org.opendaylight.yangtools.yang.common.Uint32;

public final class OlmPowerServiceRpcImplUtil {

    private OlmPowerServiceRpcImplUtil() {
    }

    public static GetPmInput  getGetPmInput() {
        GetPmInput input = new GetPmInputBuilder().setGranularity(PmGranularity._15min).setNodeId("node1")
            .setResourceIdentifier(new ResourceIdentifierBuilder().setCircuitPackName("circuit pack name")
                .setResourceName("resource name").build()).setResourceType(ResourceTypeEnum.Device).build();
        return input;
    }

    public static ServicePowerSetupInput getServicePowerSetupInput() {
        Nodes node1 = new NodesBuilder().setDestTp("dest").setSrcTp("src").setNodeId("node 1").build();
        Nodes node2 = new NodesBuilder().setDestTp("dest").setSrcTp("src").setNodeId("node 2").build();
        List<Nodes> nodes = new ArrayList<>();
        nodes.add(node1);
        nodes.add(node2);
        ServicePowerSetupInput input = new ServicePowerSetupInputBuilder().setNodes(nodes)
            .setServiceName("service 1")
            .setWaveNumber(Uint32.valueOf("1234")).build();
        return input;
    }

    public static ServicePowerSetupInput getServicePowerSetupInput2() {
        Nodes node1 = new NodesBuilder().setDestTp("network").setSrcTp("src").setNodeId("node 1").build();
        Nodes node2 = new NodesBuilder().setDestTp("network").setSrcTp("src").setNodeId("node 2").build();
        List<Nodes> nodes = new ArrayList<>();
        nodes.add(node1);
        nodes.add(node2);
        ServicePowerSetupInput input = new ServicePowerSetupInputBuilder().setNodes(nodes)
            .setServiceName("service 1")
            .setWaveNumber(Uint32.valueOf("1234")).build();
        return input;
    }

    public static ServicePowerSetupInput getServicePowerSetupInput3() {
        Nodes node1 = new NodesBuilder().setDestTp("deg").setSrcTp("src").setNodeId("node 1").build();
        Nodes node2 = new NodesBuilder().setDestTp("deg").setSrcTp("src").setNodeId("node 2").build();
        List<Nodes> nodes = new ArrayList<>();
        nodes.add(node1);
        nodes.add(node2);
        ServicePowerSetupInput input = new ServicePowerSetupInputBuilder().setNodes(nodes)
            .setServiceName("service 1")
            .setWaveNumber(Uint32.valueOf("1234")).build();
        return input;
    }

    public static ServicePowerSetupInput getServicePowerSetupInput4() {
        Nodes node1 = new NodesBuilder().setDestTp("srg").setSrcTp("src").setNodeId("node 1").build();
        Nodes node2 = new NodesBuilder().setDestTp("srg").setSrcTp("src").setNodeId("node 2").build();
        List<Nodes> nodes = new ArrayList<>();
        nodes.add(node1);
        nodes.add(node2);
        ServicePowerSetupInput input = new ServicePowerSetupInputBuilder().setNodes(nodes)
            .setServiceName("service 1")
            .setWaveNumber(Uint32.valueOf("1234")).build();
        return input;
    }

    public static ServicePowerTurndownInput getServicePowerTurndownInput() {
        Nodes node1 = new NodesBuilder().setDestTp("dest").setSrcTp("src").setNodeId("node 1").build();
        Nodes node2 = new NodesBuilder().setDestTp("dest").setSrcTp("src").setNodeId("node 2").build();
        List<Nodes> nodes = new ArrayList<>();
        nodes.add(node1);
        nodes.add(node2);
        ServicePowerTurndownInput input = new ServicePowerTurndownInputBuilder()
                .setNodes(nodes)
                .setServiceName("service 1")
                .setWaveNumber(Uint32.valueOf("1234")).build();

        return input;
    }

    public static ServicePowerTurndownInput getServicePowerTurndownInput2() {
        Nodes node1 = new NodesBuilder().setDestTp("destdeg").setSrcTp("src").setNodeId("node 1").build();
        Nodes node2 = new NodesBuilder().setDestTp("destdeg").setSrcTp("src").setNodeId("node 2").build();
        List<Nodes> nodes = new ArrayList<>();
        nodes.add(node1);
        nodes.add(node2);
        ServicePowerTurndownInput input = new ServicePowerTurndownInputBuilder()
                .setNodes(nodes)
                .setServiceName("service 1")
                .setWaveNumber(Uint32.valueOf("1234")).build();

        return input;
    }

    public static ServicePowerTurndownInput getServicePowerTurndownInput3() {
        Nodes node1 = new NodesBuilder().setDestTp("destsrg").setSrcTp("src").setNodeId("node 1").build();
        Nodes node2 = new NodesBuilder().setDestTp("destsrg").setSrcTp("src").setNodeId("node 2").build();
        List<Nodes> nodes = new ArrayList<>();
        nodes.add(node1);
        nodes.add(node2);
        ServicePowerTurndownInput input = new ServicePowerTurndownInputBuilder()
                .setNodes(nodes)
                .setServiceName("service 1")
                .setWaveNumber(Uint32.valueOf("1234")).build();

        return input;
    }

    public static ServicePowerTurndownInput getServicePowerTurndownInput4() {
        Nodes node1 = new NodesBuilder().setDestTp("destdeg").setSrcTp("src").setNodeId("node 1").build();
        Nodes node2 = new NodesBuilder().setDestTp("destdeg").setSrcTp("src").setNodeId("node 2").build();
        List<Nodes> nodes = new ArrayList<>();
        nodes.add(node1);
        nodes.add(node2);
        ServicePowerTurndownInput input = new ServicePowerTurndownInputBuilder()
                .setNodes(nodes)
                .setServiceName("service 1")
                .setWaveNumber(Uint32.valueOf("1234")).build();

        return input;
    }

    public static CalculateSpanlossBaseInput getCalculateSpanlossBaseInputLink() {
        CalculateSpanlossBaseInput input = new CalculateSpanlossBaseInputBuilder()
                .setLinkId(new LinkId("ROADM-A1-to-ROADM-C1"))
                .setSrcType(CalculateSpanlossBaseInput.SrcType.Link)
                .build();
        return input;
    }

    public static CalculateSpanlossBaseInput getCalculateSpanlossBaseInputAll() {
        CalculateSpanlossBaseInput input = new CalculateSpanlossBaseInputBuilder()
                .setLinkId(new LinkId("ROADM-A1-to-ROADM-C1"))
                .setSrcType(CalculateSpanlossBaseInput.SrcType.All)
                .build();
        return input;
    }

    public static CalculateSpanlossBaseInput getCalculateSpanlossBaseInput2() {
        Link1 link1 = new Link1Builder().build();
        CalculateSpanlossBaseInput input = new CalculateSpanlossBaseInputBuilder()
            .setLinkId(new LinkId("link 1"))
            .setSrcType(CalculateSpanlossBaseInput.SrcType.All).build();
        return input;
    }

    public static CalculateSpanlossCurrentInput getCalculateSpanlossCurrentInput() {
        CalculateSpanlossCurrentInput input = new CalculateSpanlossCurrentInputBuilder()
            .build();
        return input;
    }

    public static ServicePowerResetInput getServicePowerResetInput() {
        ServicePowerResetInput input = new ServicePowerResetInputBuilder()
            .setServiceName("service 1").build();
        return input;
    }
}
