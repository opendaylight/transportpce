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
import java.util.Set;
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
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev260612.NodeDatamodelType;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev260612.OpenconfigNodeVersion;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev260612.OpenroadmNodeVersion;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev260612.mapping.Mapping;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev260612.mapping.MappingBuilder;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev260612.mapping.MappingKey;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev260612.mapping.mapping.OpenconfigInfoBuilder;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev260612.network.nodes.NodeInfoBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.node.types.rev210528.NodeTypes;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.types.rev181019.Direction;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.types.rev181019.Location;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev181019.OrgOpenroadmDeviceData;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev181019.interfaces.grp.Interface;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev181019.interfaces.grp.InterfaceKey;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev181019.org.openroadm.device.container.OrgOpenroadmDevice;
import org.opendaylight.yang.gen.v1.http.org.openroadm.pm.rev181019.CurrentPmList;
import org.opendaylight.yang.gen.v1.http.org.openroadm.pm.rev181019.CurrentPmListBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.pm.rev181019.current.pm.group.CurrentPm;
import org.opendaylight.yang.gen.v1.http.org.openroadm.pm.rev181019.current.pm.group.CurrentPmBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.pm.rev181019.current.pm.group.CurrentPmKey;
import org.opendaylight.yang.gen.v1.http.org.openroadm.pm.rev181019.current.pm.list.CurrentPmEntry;
import org.opendaylight.yang.gen.v1.http.org.openroadm.pm.rev181019.current.pm.list.CurrentPmEntryBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.pm.rev181019.current.pm.list.CurrentPmEntryKey;
import org.opendaylight.yang.gen.v1.http.org.openroadm.pm.rev181019.current.pm.val.group.Measurement;
import org.opendaylight.yang.gen.v1.http.org.openroadm.pm.rev181019.current.pm.val.group.MeasurementBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.pm.rev181019.current.pm.val.group.MeasurementKey;
import org.opendaylight.yang.gen.v1.http.org.openroadm.pm.types.rev171215.PmDataType;
import org.opendaylight.yang.gen.v1.http.org.openroadm.pm.types.rev171215.PmNamesEnum;
import org.opendaylight.yang.gen.v1.http.org.openroadm.pm.types.rev171215.Validity;
import org.opendaylight.yang.gen.v1.http.org.openroadm.resource.types.rev181019.ResourceTypeEnum;
import org.opendaylight.yang.gen.v1.http.org.transportpce.common.types.rev260707.PmGranularity;
import org.opendaylight.yang.gen.v1.http.org.transportpce.common.types.rev260707.olm.get.pm.input.ResourceIdentifierBuilder;
import org.opendaylight.yang.gen.v1.http.org.transportpce.common.types.rev260707.optical.renderer.nodes.Nodes;
import org.opendaylight.yang.gen.v1.http.org.transportpce.common.types.rev260707.optical.renderer.nodes.NodesBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.LinkId;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.DateAndTime;
import org.opendaylight.yangtools.binding.DataObjectIdentifier;
import org.opendaylight.yangtools.yang.common.Decimal64;
import org.opendaylight.yangtools.yang.common.Uint32;

public final class OlmPowerServiceRpcImplUtil {

    private OlmPowerServiceRpcImplUtil() {
    }

    public static GetPmInput  getGetPmInput() {
        return new GetPmInputBuilder()
                .setDirection(org.opendaylight.yang.gen.v1.http.org.transportpce.common.types.rev260707.Direction.Tx)
                .setPmExtension("extension")
                .setPmNameType(org.opendaylight.yang.gen.v1.http.org.transportpce.common.types.rev260707.PmNamesEnum
                        .OpticalPowerInput)
                .setLocation(org.opendaylight.yang.gen.v1.http.org.transportpce.common.types.rev260707.Location.NearEnd)
                .setDirection(org.opendaylight.yang.gen.v1.http.org.transportpce.common.types.rev260707.Direction.Rx)
                .setNodeId("node1")
                .setGranularity(PmGranularity._15min)
                .setResourceIdentifier(new ResourceIdentifierBuilder()
                        .setResourceName("ots-deg1").build())
                .setResourceType(ResourceTypeEnum.Interface).build();
    }

    public static CurrentPmList getCurrentPmList221() {
        Map<MeasurementKey, Measurement> measureList = new HashMap<>();
        Measurement measure = new MeasurementBuilder()
                .setGranularity(org.opendaylight.yang.gen.v1.http.org.openroadm.pm.types.rev171215.PmGranularity._15min)
                .setValidity(Validity.Complete)
                .setPmParameterValue(new PmDataType(Decimal64.valueOf(2, 3)))
                .setPmParameterUnit("pm_unit")
                .build();
        measureList.put(measure.key(), measure);
        Map<CurrentPmKey, CurrentPm> currentPmList = new HashMap<>();
        CurrentPm currentPm = new CurrentPmBuilder()
                .setMeasurement(measureList)
                .setType(PmNamesEnum.OpticalPowerInput)
                .setDirection(Direction.Rx)
                .setExtension("extension")
                .setLocation(Location.NearEnd)
                .build();
        currentPmList.put(currentPm.key(), currentPm);

        DataObjectIdentifier device = DataObjectIdentifier
                .builderOfInherited(OrgOpenroadmDeviceData.class, OrgOpenroadmDevice.class)
                    .child(Interface.class, new InterfaceKey("ots-deg1"))
                .build();

        CurrentPmEntry currentPmEntry =
            new CurrentPmEntryBuilder()
                .setCurrentPm(currentPmList)
                .setPmResourceType(ResourceTypeEnum.Interface)
                .setPmResourceInstance(device)
                .setPmResourceTypeExtension("pm_resource_type_extension")
                .setRetrievalTime(new DateAndTime("2023-01-01T00:00:00Z"))
                .build();
        Map<CurrentPmEntryKey, CurrentPmEntry> currentPmMap = new HashMap<>();
        currentPmMap.put(currentPmEntry.key(), currentPmEntry);
        return new CurrentPmListBuilder()
                .setCurrentPmEntry(currentPmMap)
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

    public static org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev260612.network
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
        return new org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev260612.network
                .NodesBuilder()
            .setNodeId(nodeId)
            .setNodeInfo(new NodeInfoBuilder()
                .setNodeType(NodeTypes.Xpdr)
                .setOpenroadmVersion(nodeVersion)
                .build())
            .setMapping(mappings)
            .build();
    }

    public static org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev260612.network
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
        return new org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev260612.network
                    .NodesBuilder()
                .setNodeId(nodeId)
                .setNodeInfo(new NodeInfoBuilder()
                        .setNodeType(NodeTypes.Rdm)
                        .setOpenroadmVersion(nodeVersion)
                        .build())
                .setMapping(mappings)
                .build();
    }

    public static org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev260612.network
            .Nodes getMappingNodeIla() {
        return new org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev260612.network
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

    public static org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev260612.network
            .Nodes getMappingNodeTpdrOpenConfig(String nodeId, OpenconfigNodeVersion nodeVersion, List<String> lcps,
                    Set<String> opticalChannels, Set<String> operationalModes) {
        Map<MappingKey, Mapping> mappings = new HashMap<>();
        for (String lcp : lcps) {
            OpenconfigInfoBuilder openconfigInfoBuilder = new OpenconfigInfoBuilder();
            if (opticalChannels != null && !opticalChannels.isEmpty()) {
                openconfigInfoBuilder.setSupportedOpticalChannels(opticalChannels);
            }
            MappingBuilder mappingBuilder = new MappingBuilder()
                    .setLogicalConnectionPoint(lcp)
                    .setSupportingCircuitPackName("circuit pack")
                    .setSupportingPort("port")
                    .setOpenconfigInfo(openconfigInfoBuilder.build());
            if (operationalModes != null && !operationalModes.isEmpty()) {
                mappingBuilder.setSupportedOperationalMode(operationalModes);
            }
            Mapping mapping = mappingBuilder.build();
            mappings.put(mapping.key(), mapping);
        }
        return new org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev260612.network
                .NodesBuilder()
            .setNodeId(nodeId)
            .setDatamodelType(NodeDatamodelType.OPENCONFIG)
            .setNodeInfo(new NodeInfoBuilder()
                .setNodeType(NodeTypes.Xpdr)
                .setOpenconfigVersion(nodeVersion)
                .build())
            .setMapping(mappings)
            .build();
    }

    public static ServicePowerSetupInput getServicePowerSetupInputForOpenConfigTransponder() {
        return new ServicePowerSetupInputBuilder()
                .setNodes(List.of(
                        new NodesBuilder().setNodeId("xpdr-OC").setSrcTp("client-OC")
                                .setDestTp("network-OC").build(),
                        new NodesBuilder().setNodeId("next-node").setSrcTp("srg1")
                                .setDestTp("deg1").build()))
                .setServiceName("service OC")
                .setWaveNumber(Uint32.valueOf("1"))
                .setLowerSpectralSlotNumber(Uint32.valueOf(761))
                .setHigherSpectralSlotNumber(Uint32.valueOf(768))
                .build();
    }
}
