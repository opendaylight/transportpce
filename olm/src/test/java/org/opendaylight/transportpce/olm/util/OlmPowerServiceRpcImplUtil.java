/*
 * Copyright © 2018 Orange, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.olm.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.olm.rev210618.CalculateSpanlossBaseInput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.olm.rev210618.CalculateSpanlossBaseInputBuilder;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.olm.rev210618.GetPmInput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.olm.rev210618.GetPmInputBuilder;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.olm.rev210618.ServicePowerResetInput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.olm.rev210618.ServicePowerResetInputBuilder;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.olm.rev210618.ServicePowerSetupInput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.olm.rev210618.ServicePowerSetupInputBuilder;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.olm.rev210618.ServicePowerTurndownInput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.olm.rev210618.ServicePowerTurndownInputBuilder;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev250115.OpenroadmNodeVersion;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev250115.mapping.Mapping;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev250115.mapping.MappingBuilder;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev250115.mapping.MappingKey;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev250115.network.nodes.NodeInfoBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.types.rev191129.NodeTypes;
import org.opendaylight.yang.gen.v1.http.org.openroadm.pm.rev161014.CurrentPmlist;
import org.opendaylight.yang.gen.v1.http.org.openroadm.pm.rev161014.CurrentPmlistBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.pm.rev161014.current.pm.MeasurementsBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.pm.rev161014.current.pm.ResourceBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.pm.rev161014.current.pm.measurements.MeasurementBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.pm.rev161014.currentpmlist.CurrentPm;
import org.opendaylight.yang.gen.v1.http.org.openroadm.pm.rev161014.currentpmlist.CurrentPmBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.pm.rev161014.currentpmlist.CurrentPmKey;
import org.opendaylight.yang.gen.v1.http.org.openroadm.pm.types.rev161014.PmDataType;
import org.opendaylight.yang.gen.v1.http.org.openroadm.pm.types.rev161014.PmNamesEnum;
import org.opendaylight.yang.gen.v1.http.org.openroadm.pm.types.rev161014.pm.measurement.PmParameterNameBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.resource.rev161014.resource.ResourceTypeBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.resource.rev161014.resource.resource.resource.InterfaceBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.resource.types.rev161014.ResourceTypeEnum;
import org.opendaylight.yang.gen.v1.http.org.transportpce.common.types.rev220926.PmGranularity;
import org.opendaylight.yang.gen.v1.http.org.transportpce.common.types.rev220926.olm.get.pm.input.ResourceIdentifierBuilder;
import org.opendaylight.yang.gen.v1.http.org.transportpce.common.types.rev220926.optical.renderer.nodes.Nodes;
import org.opendaylight.yang.gen.v1.http.org.transportpce.common.types.rev220926.optical.renderer.nodes.NodesBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.LinkId;
import org.opendaylight.yangtools.yang.common.Decimal64;
import org.opendaylight.yangtools.yang.common.Uint32;

public final class OlmPowerServiceRpcImplUtil {

    private OlmPowerServiceRpcImplUtil() {
    }

    public static GetPmInput  getGetPmInput() {
        GetPmInput input = new GetPmInputBuilder()
                .setNodeId("node1")
                .setGranularity(PmGranularity._15min)
                .setResourceIdentifier(new ResourceIdentifierBuilder()
                        .setResourceName("ots-deg1").build())
                .setResourceType(ResourceTypeEnum.Interface).build();
        return input;
    }

    public static CurrentPmlist getCurrentPmList121() {
        CurrentPm currentPm = new CurrentPmBuilder()
                .setId("id")
                .setGranularity(org.opendaylight.yang.gen.v1.http.org.openroadm.pm.types.rev161014.PmGranularity._15min)
                .setResource(new ResourceBuilder()
                        .setResourceType(new ResourceTypeBuilder()
                                .setType(ResourceTypeEnum.Interface)
                                .build())
                        .setResource(new org.opendaylight.yang.gen.v1.http.org.openroadm.resource.rev161014.resource
                                .ResourceBuilder()
                                .setResource(new InterfaceBuilder()
                                        .setInterfaceName("ots-deg1")
                                        .build())
                                .build())
                        .build())
                .setMeasurements(List.of(
                        new MeasurementsBuilder()
                                .setMeasurement(new MeasurementBuilder()
                                        .setPmParameterName(new PmParameterNameBuilder()
                                                .setType(PmNamesEnum.OpticalPowerInput)
                                                .build())
                                        .setPmParameterValue(new PmDataType(Decimal64.valueOf("3")))
                                        .build())
                                .build()))
                .build();
        Map<CurrentPmKey, CurrentPm> currentPmMap = new HashMap<>();
        currentPmMap.put(currentPm.key(), currentPm);
        return new CurrentPmlistBuilder()
                .setCurrentPm(currentPmMap)
                .build();
    }

    public static ServicePowerSetupInput getServicePowerSetupInputForTransponder() {
        return new ServicePowerSetupInputBuilder()
                .setNodes(List.of(
                        new NodesBuilder().setNodeId("xpdr-A").setSrcTp("client-A").setDestTp("network-A").build(),
                        new NodesBuilder().setNodeId("roadm-A").setSrcTp("srg1-A").setDestTp("deg2-A").build()))
                .setServiceName("service 1")
                .setWaveNumber(Uint32.valueOf("1"))
                .setLowerSpectralSlotNumber(Uint32.valueOf(761))
                .setHigherSpectralSlotNumber(Uint32.valueOf(768))
                .build();
    }

    public static ServicePowerSetupInput getServicePowerSetupInputForOneNode(String nodeId, String srcTp,
            String destTp) {
        return new ServicePowerSetupInputBuilder()
                .setNodes(List.of(
                        new NodesBuilder().setNodeId(nodeId).setSrcTp(srcTp).setDestTp(destTp).build()))
                .setServiceName("service 1")
                .setWaveNumber(Uint32.valueOf("1"))
                .setLowerSpectralSlotNumber(Uint32.valueOf(761))
                .setHigherSpectralSlotNumber(Uint32.valueOf(768))
                .build();
    }

    public static ServicePowerSetupInput getServicePowerSetupInputWthoutNode() {
        return new ServicePowerSetupInputBuilder()
                .setNodes(List.of())
                .setServiceName("service 1")
                .setWaveNumber(Uint32.valueOf("1"))
                .setLowerSpectralSlotNumber(Uint32.valueOf(761))
                .setHigherSpectralSlotNumber(Uint32.valueOf(768))
                .build();
    }

    public static org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev250115.network
            .Nodes getMappingNodeTpdr(String nodeId, OpenroadmNodeVersion nodeVersion, List<String> lcps) {
        Map<MappingKey, Mapping> mappings = new HashMap<>();
        for (String lcp:lcps) {
            Mapping mapping = new MappingBuilder()
                    .setLogicalConnectionPoint(lcp)
                    .setSupportingCircuitPackName("circuit pack")
                    .setSupportingPort("port")
                    .build();
            mappings.put(mapping.key(), mapping);
        }
        return new org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev250115.network
                .NodesBuilder()
            .setNodeId(nodeId)
            .setNodeInfo(new NodeInfoBuilder()
                .setNodeType(NodeTypes.Xpdr)
                .setOpenroadmVersion(nodeVersion)
                .build())
            .setMapping(mappings)
            .build();
    }

    public static org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev250115.network
            .Nodes getMappingNodeRdm(String nodeId, OpenroadmNodeVersion nodeVersion, List<String> lcps) {
        Map<MappingKey, Mapping> mappings = new HashMap<>();
        for (String lcp:lcps) {
            MappingBuilder mappingBldr = new MappingBuilder()
                    .setLogicalConnectionPoint(lcp)
                    .setSupportingCircuitPackName("circuit pack")
                    .setSupportingPort("port");
            if (lcp.contains("deg")) {
                mappingBldr.setSupportingOts("interface ots")
                        .setSupportingOms("interface oms");
            }
            mappings.put(mappingBldr.build().key(), mappingBldr.build());
        }
        return new org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev250115.network
                    .NodesBuilder()
                .setNodeId(nodeId)
                .setNodeInfo(new NodeInfoBuilder()
                        .setNodeType(NodeTypes.Rdm)
                        .setOpenroadmVersion(nodeVersion)
                        .build())
                .setMapping(mappings)
                .build();
    }

    public static org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev250115.network
            .Nodes getMappingNodeIla() {
        return new org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev250115.network
                    .NodesBuilder()
                .setNodeId("ila node")
                .setNodeInfo(new NodeInfoBuilder()
                        .setNodeType(NodeTypes.Ila)
                        .setOpenroadmVersion(OpenroadmNodeVersion._121)
                        .build())
                .build();
    }

    public static ServicePowerSetupInput getServicePowerSetupInput() {
        Nodes node1 = new NodesBuilder().setDestTp("dest").setSrcTp("src").setNodeId("node 1").build();
        Nodes node2 = new NodesBuilder().setDestTp("dest").setSrcTp("src").setNodeId("node 2").build();
        List<Nodes> nodes = new ArrayList<>();
        nodes.add(node1);
        nodes.add(node2);
        ServicePowerSetupInput input = new ServicePowerSetupInputBuilder().setNodes(nodes)
            .setServiceName("service 1")
            .setWaveNumber(Uint32.valueOf("1"))
            .setLowerSpectralSlotNumber(Uint32.valueOf(761))
            .setHigherSpectralSlotNumber(Uint32.valueOf(768))
            .build();
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
            .setWaveNumber(Uint32.valueOf("1"))
            .setLowerSpectralSlotNumber(Uint32.valueOf(761))
            .setHigherSpectralSlotNumber(Uint32.valueOf(768)).build();
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
            .setWaveNumber(Uint32.valueOf("1"))
            .setLowerSpectralSlotNumber(Uint32.valueOf(761))
            .setHigherSpectralSlotNumber(Uint32.valueOf(768)).build();
        return input;
    }

    public static ServicePowerTurndownInput getServicePowerTurndownInput() {
        return new ServicePowerTurndownInputBuilder()
                .setNodes(List.of(
                        new NodesBuilder().setNodeId("roadm-A").setSrcTp("srg1-A").setDestTp("deg2-A").build(),
                        new NodesBuilder().setNodeId("roadm-C").setSrcTp("deg1-C").setDestTp("srg1-C").build())
                        )
                .setServiceName("service 1")
                .setWaveNumber(Uint32.valueOf("1"))
                .setLowerSpectralSlotNumber(Uint32.valueOf(761))
                .setHigherSpectralSlotNumber(Uint32.valueOf(768))
                .build();
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
                .setSrcType(CalculateSpanlossBaseInput.SrcType.All)
                .build();
        return input;
    }

    public static ServicePowerResetInput getServicePowerResetInput() {
        ServicePowerResetInput input = new ServicePowerResetInputBuilder()
            .setServiceName("service 1").build();
        return input;
    }
}
