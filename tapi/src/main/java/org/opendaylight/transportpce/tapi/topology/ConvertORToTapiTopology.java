/*
 * Copyright © 2023 Orange, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.tapi.topology;

import java.math.RoundingMode;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.opendaylight.transportpce.common.fixedflex.GridConstant;
import org.opendaylight.transportpce.common.fixedflex.GridUtils;
import org.opendaylight.transportpce.tapi.TapiStringConstants;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev230526.TerminationPoint1;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.state.types.rev191129.State;
import org.opendaylight.yang.gen.v1.http.org.openroadm.degree.rev230526.degree.used.wavelengths.UsedWavelengths;
import org.opendaylight.yang.gen.v1.http.org.openroadm.degree.rev230526.degree.used.wavelengths.UsedWavelengthsKey;
import org.opendaylight.yang.gen.v1.http.org.openroadm.equipment.states.types.rev191129.AdminStates;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev230526.networks.network.node.termination.point.PpAttributes;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev230526.networks.network.node.termination.point.TxTtpAttributes;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev230526.networks.network.node.termination.point.XpdrNetworkAttributes;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.types.rev230526.xpdr.odu.switching.pools.OduSwitchingPools;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.types.rev230526.xpdr.odu.switching.pools.OduSwitchingPoolsBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.types.rev230526.xpdr.odu.switching.pools.odu.switching.pools.NonBlockingList;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.types.rev230526.xpdr.odu.switching.pools.odu.switching.pools.NonBlockingListBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.types.rev230526.xpdr.odu.switching.pools.odu.switching.pools.NonBlockingListKey;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.types.rev230526.OpenroadmNodeType;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.types.rev230526.OpenroadmTpType;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.types.rev230526.xpdr.tp.supported.interfaces.SupportedInterfaceCapability;
import org.opendaylight.yang.gen.v1.http.org.openroadm.otn.network.topology.rev230526.Node1;
import org.opendaylight.yang.gen.v1.http.org.openroadm.xponder.rev230526.xpdr.mode.attributes.supported.operational.modes.OperationalModeKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.networks.network.Node;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.TpId;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.networks.network.node.TerminationPoint;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.AdministrativeState;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.CAPACITYUNITGBPS;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.Direction;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.LayerProtocolName;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.LifecycleState;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.OperationalState;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.PortRole;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.Uuid;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.capacity.pac.AvailableCapacityBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.capacity.pac.TotalPotentialCapacityBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.global._class.Name;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.global._class.NameBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.global._class.NameKey;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.payload.structure.CapacityBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.tapi.context.ServiceInterfacePoint;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.tapi.context.ServiceInterfacePointBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.tapi.context.ServiceInterfacePointKey;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.digital.otn.rev221121.ODUTYPEODU0;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.digital.otn.rev221121.ODUTYPEODU2;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.digital.otn.rev221121.ODUTYPEODU2E;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.digital.otn.rev221121.ODUTYPEODU4;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.digital.otn.rev221121.ODUTYPEODUCN;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.digital.otn.rev221121.OTUTYPEOTUCN;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.dsr.rev221121.DIGITALSIGNALTYPE100GigE;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.dsr.rev221121.DIGITALSIGNALTYPE10GigELAN;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.dsr.rev221121.DIGITALSIGNALTYPEGigE;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.photonic.media.rev221121.PHOTONICLAYERQUALIFIEROTS;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.photonic.media.rev221121.PHOTONICLAYERQUALIFIEROTSi;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.photonic.media.rev221121.PHOTONICLAYERQUALIFIEROTSiMC;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.photonic.media.rev221121.context.topology.context.topology.node.owned.node.edge.point.PhotonicMediaNodeEdgePointSpecBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.photonic.media.rev221121.photonic.media.node.edge.point.spec.SpectrumCapabilityPacBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.photonic.media.rev221121.spectrum.capability.pac.AvailableSpectrum;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.photonic.media.rev221121.spectrum.capability.pac.AvailableSpectrumBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.photonic.media.rev221121.spectrum.capability.pac.AvailableSpectrumKey;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.photonic.media.rev221121.spectrum.capability.pac.OccupiedSpectrum;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.photonic.media.rev221121.spectrum.capability.pac.OccupiedSpectrumBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.photonic.media.rev221121.spectrum.capability.pac.OccupiedSpectrumKey;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.photonic.media.rev221121.spectrum.capability.pac.SupportableSpectrum;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.photonic.media.rev221121.spectrum.capability.pac.SupportableSpectrumBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.photonic.media.rev221121.spectrum.capability.pac.SupportableSpectrumKey;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.FORWARDINGRULE;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.FORWARDINGRULECANNOTFORWARDACROSSGROUP;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.FORWARDINGRULEMAYFORWARDACROSSGROUP;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.RuleType;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.inter.rule.group.AssociatedNodeRuleGroup;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.inter.rule.group.AssociatedNodeRuleGroupBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.inter.rule.group.AssociatedNodeRuleGroupKey;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.node.InterRuleGroup;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.node.InterRuleGroupBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.node.InterRuleGroupKey;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.node.NodeRuleGroup;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.node.NodeRuleGroupBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.node.NodeRuleGroupKey;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.node.OwnedNodeEdgePoint;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.node.OwnedNodeEdgePointBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.node.OwnedNodeEdgePointKey;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.node.RiskParameterPacBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.node.edge.point.AvailablePayloadStructure;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.node.edge.point.AvailablePayloadStructureBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.node.edge.point.MappedServiceInterfacePoint;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.node.edge.point.MappedServiceInterfacePointBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.node.edge.point.MappedServiceInterfacePointKey;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.node.edge.point.SupportedCepLayerProtocolQualifierInstances;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.node.edge.point.SupportedCepLayerProtocolQualifierInstancesBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.node.edge.point.SupportedPayloadStructure;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.node.edge.point.SupportedPayloadStructureBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.node.rule.group.Rule;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.node.rule.group.RuleBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.node.rule.group.RuleKey;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.risk.parameter.pac.RiskCharacteristic;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.risk.parameter.pac.RiskCharacteristicBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.topology.Link;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.topology.LinkKey;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.topology.NodeBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.topology.NodeKey;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.transfer.cost.pac.CostCharacteristic;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.transfer.cost.pac.CostCharacteristicBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.transfer.timing.pac.LatencyCharacteristic;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.transfer.timing.pac.LatencyCharacteristicBuilder;
import org.opendaylight.yangtools.yang.common.Decimal64;
import org.opendaylight.yangtools.yang.common.Uint16;
import org.opendaylight.yangtools.yang.common.Uint64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class ConvertORToTapiTopology {

    private static final Logger LOG = LoggerFactory.getLogger(ConvertORToTapiTopology.class);
    private String ietfNodeId;
    private OpenroadmNodeType ietfNodeType;
    private AdminStates ietfNodeAdminState;
    private State ietfNodeOperState;
    private List<TerminationPoint> oorClientPortList;
    private List<TerminationPoint> oorNetworkPortList;
    private OduSwitchingPools oorOduSwitchingPool;
    private Uuid tapiTopoUuid;
    private Map<NodeKey, org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.topology.Node>
        tapiNodes;
    private Map<LinkKey, Link> tapiLinks;
    private Map<ServiceInterfacePointKey, ServiceInterfacePoint> tapiSips;
    private Map<String, Uuid> uuidMap;


    public ConvertORToTapiTopology(Uuid tapiTopoUuid) {
        this.tapiTopoUuid = tapiTopoUuid;
        this.tapiNodes = new HashMap<>();
        this.tapiLinks = new HashMap<>();
        this.uuidMap = new HashMap<>();
        this.tapiSips = new HashMap<>();
    }

    public void convertNode(Node ietfNode, List<String> networkPorts) {
        this.ietfNodeId = ietfNode.getNodeId().getValue();
        var ietfAug =
            ietfNode.augmentation(org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev230526.Node1.class);
        if (ietfAug == null) {
            return;
        }
        this.ietfNodeType = ietfAug.getNodeType();
        this.ietfNodeAdminState = ietfAug.getAdministrativeState();
        this.ietfNodeOperState = ietfAug.getOperationalState();
        var ietfAugTopo =
            ietfNode.augmentation(
                org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.Node1.class);
        this.oorNetworkPortList = ietfAugTopo.getTerminationPoint().values().stream()
            .filter(tp -> tp.augmentation(TerminationPoint1.class).getTpType().getIntValue()
                    == OpenroadmTpType.XPONDERNETWORK.getIntValue()
                && networkPorts.contains(tp.getTpId().getValue()))
            .sorted((tp1, tp2) -> tp1.getTpId().getValue().compareTo(tp2.getTpId().getValue()))
            .collect(Collectors.toList());
        if (this.ietfNodeType.equals(OpenroadmNodeType.TPDR)) {
            this.oorOduSwitchingPool = createOduSwitchingPoolForTp100G();
            List<TpId> tpList = this.oorOduSwitchingPool.getNonBlockingList().values().stream()
                .flatMap(nbl -> nbl.getTpList().stream())
                .collect(Collectors.toList());
            this.oorClientPortList = ietfAugTopo.getTerminationPoint().values().stream()
                .filter(tp -> tp.augmentation(TerminationPoint1.class).getTpType().getIntValue()
                        == OpenroadmTpType.XPONDERCLIENT.getIntValue()
                    && tpList.contains(tp.getTpId()))
                .sorted((tp1, tp2) -> tp1.getTpId().getValue().compareTo(tp2.getTpId().getValue()))
                .collect(Collectors.toList());
            this.oorClientPortList.forEach(tp -> LOG.info("tp = {}", tp.getTpId()));
        } else {
            this.oorOduSwitchingPool = ietfNode.augmentation(Node1.class).getSwitchingPools().getOduSwitchingPools()
                .values().stream().findFirst().orElseThrow();
            this.oorClientPortList = ietfAugTopo.getTerminationPoint().values().stream()
                .filter(tp -> tp.augmentation(TerminationPoint1.class).getTpType().getIntValue()
                    == OpenroadmTpType.XPONDERCLIENT.getIntValue())
                .sorted((tp1, tp2) -> tp1.getTpId().getValue().compareTo(tp2.getTpId().getValue()))
                .collect(Collectors.toList());
        }

        // node creation [DSR/ODU] ([DSR/ODU] and OTSI merged in R 2.4.X)
        LOG.info("creation of a DSR/ODU node for {}", this.ietfNodeId);
        String nodeIdXpdr = String.join("+", this.ietfNodeId, TapiStringConstants.XPDR);
        this.uuidMap.put(nodeIdXpdr,
                //nodeUuid
                new Uuid(UUID.nameUUIDFromBytes(nodeIdXpdr.getBytes(Charset.forName("UTF-8"))).toString()));
        Name nameDsr = new NameBuilder().setValueName("dsr/odu node name").setValue(nodeIdXpdr).build();
        Name namePhot = new NameBuilder().setValueName("otsi node name").setValue(nodeIdXpdr).build();
        Name nameNodeType = new NameBuilder().setValueName("Node Type").setValue(this.ietfNodeType.getName()).build();
        org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.topology.Node dsrNode =
            createTapiNode(
                Map.of(nameDsr.key(), nameDsr, namePhot.key(), namePhot, nameNodeType.key(), nameNodeType),
                //dsrLayerProtocols
                Set.of(LayerProtocolName.DSR, LayerProtocolName.ODU,
                       LayerProtocolName.DIGITALOTN, LayerProtocolName.PHOTONICMEDIA));
        LOG.debug("XPDR Node {} should have {} NEPs and SIPs",
            this.ietfNodeId, this.oorClientPortList.size() + this.oorNetworkPortList.size());
        LOG.info("XPDR Node {} has {} NEPs and {} SIPs",
            this.ietfNodeId,
            dsrNode.getOwnedNodeEdgePoint().values().size(),
            dsrNode.getOwnedNodeEdgePoint().values().stream()
                .filter(nep -> nep.getMappedServiceInterfacePoint() != null).count());
        tapiNodes.put(dsrNode.key(), dsrNode);
    }

    public Map<NodeRuleGroupKey, NodeRuleGroup> createNodeRuleGroupForRdmNode(String topoType, Uuid nodeUuid,
            String subNodeName, List<OwnedNodeEdgePointKey> onepl, FORWARDINGRULE forwardingRule, int index) {
        Map<org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.node.rule.group.NodeEdgePointKey,
            org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.node.rule.group.NodeEdgePoint>
                nepMap = new HashMap<>();
        for (OwnedNodeEdgePointKey onepKey : onepl) {
            var nep = new org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121
                    .node.rule.group.NodeEdgePointBuilder()
                .setTopologyUuid(tapiTopoUuid)
                .setNodeUuid(nodeUuid)
                .setNodeEdgePointUuid(onepKey.getUuid())
                .build();
            nepMap.put(nep.key(), nep);
        }
        String nrgNameValue = String.join("-", subNodeName, "node-rule-group-" + index);
        Rule rule = new RuleBuilder()
            .setLocalId("forward")
            .setForwardingRule(forwardingRule)
            .setRuleType(new HashSet<RuleType>(Set.of(RuleType.FORWARDING)))
            .build();
        Name nrgName = new NameBuilder().setValueName("nrg name").setValue(nrgNameValue).build();
        NodeRuleGroup nodeRuleGroup = new NodeRuleGroupBuilder()
            .setName(Map.of(nrgName.key(), nrgName))
            .setUuid(new Uuid(UUID.nameUUIDFromBytes((nrgNameValue).getBytes(Charset.forName("UTF-8"))).toString()))
            .setRule(new HashMap<RuleKey, Rule>(Map.of(rule.key(), rule)))
            .setNodeEdgePoint(nepMap)
            .build();
        return new HashMap<>(Map.of(nodeRuleGroup.key(), nodeRuleGroup));
    }

    public Map<NodeRuleGroupKey, NodeRuleGroup> createAllNodeRuleGroupForRdmNode(
            String topoType, Uuid nodeUuid, String orNodeId, Collection<OwnedNodeEdgePoint> onepl) {
        List<OwnedNodeEdgePoint> otsNepList = topoType.equals("T0ML") ? onepl.stream().collect(Collectors.toList())
                : onepl.stream()
                    .filter(onep -> onep.getName().keySet().contains(new NameKey("PHOTONIC_MEDIA_OTSNodeEdgePoint")))
                    .collect(Collectors.toList());
        LOG.info("Creating NRG for {} {}", topoType, otsNepList);
        List<OwnedNodeEdgePointKey> degOnepKeyList = new ArrayList<>();
        List<String> srgNames = new ArrayList<>();
        Map<OwnedNodeEdgePointKey,String> srgMap = new HashMap<>();
        for (OwnedNodeEdgePoint onep : otsNepList) {
            String onepName = onep.getName()
                .get(new NameKey(topoType.equals("T0ML") ? "NodeEdgePoint name" : "PHOTONIC_MEDIA_OTSNodeEdgePoint"))
                .getValue();
            String subNodeName = topoType.equals("T0ML") ? "ROADMINFRA-SRG-PP"
                : String.join("-", onepName.split("\\+")[0], onepName.split("\\+")[2]);
            if (subNodeName.contains("DEG")) {
                subNodeName = subNodeName.split("\\-TTP")[0];
                degOnepKeyList.add(onep.key());
            } else if (subNodeName.contains("SRG")) {
                subNodeName = subNodeName.split("\\-PP")[0];
                srgMap.put(onep.key(), subNodeName);
                if (!srgNames.contains(subNodeName)) {
                    srgNames.add(subNodeName);
                }
            }
        }
        int index = 0;
        Map<NodeRuleGroupKey, NodeRuleGroup> globalNrgMap = new  HashMap<>();
        if (topoType.equals("Full")) {
            globalNrgMap.putAll(createNodeRuleGroupForRdmNode(
                    topoType, nodeUuid, String.join("-", orNodeId, "DEG"),
                    degOnepKeyList, FORWARDINGRULEMAYFORWARDACROSSGROUP.VALUE, index));
            index++;
        }
        for (String srgName : srgNames) {
            globalNrgMap.putAll(createNodeRuleGroupForRdmNode(
                topoType,
                nodeUuid,
                srgName,
                srgMap.entrySet().stream()
                    .filter(item -> item.getValue().equals(srgName))
                    .map(item -> item.getKey())
                    .collect(Collectors.toList()),
                // For T0ML we consider any port of ROADM INFRA can connect to potentially any other port
                //topoType.equals("T0ML") ? FORWARDINGRULEMAYFORWARDACROSSGROUP.VALUE
                topoType.equals("T0ML") ? FORWARDINGRULEMAYFORWARDACROSSGROUP.VALUE
                    : FORWARDINGRULECANNOTFORWARDACROSSGROUP.VALUE,
                // Whereas for Abstracted or Full Topology we consider any port of the same SRG can not forward to
                // another port of the same SRG. Connectivity between SRGS will be defined through inter-rule-group
                index));
            index++;
            LOG.debug("AllNodeRuleGroup : creating a NRG for {}", srgName);
        }
        return globalNrgMap;
    }

    public Map<InterRuleGroupKey, InterRuleGroup> createInterRuleGroupForRdmNode(
            String topoType, Uuid nodeUuid,String orNodeId, List<NodeRuleGroupKey> nrgList) {
        Map<AssociatedNodeRuleGroupKey, AssociatedNodeRuleGroup> associatedNrgMap = new HashMap<>();
        for (NodeRuleGroupKey nrgKey : nrgList) {
            AssociatedNodeRuleGroup associatedNrg = new AssociatedNodeRuleGroupBuilder()
                .setTopologyUuid(tapiTopoUuid)
                .setNodeUuid(nodeUuid)
                .setNodeRuleGroupUuid(nrgKey.getUuid())
                .build();
            associatedNrgMap.put(associatedNrg.key(), associatedNrg);
        }
        var rule = new org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121
                .inter.rule.group.RuleBuilder()
            .setLocalId("forward")
            .setForwardingRule(FORWARDINGRULEMAYFORWARDACROSSGROUP.VALUE)
            .setRuleType(new HashSet<RuleType>(Set.of(RuleType.FORWARDING)))
            .build();
        String irgNameValue = topoType.equals("Full") ? orNodeId + " inter rule group-" : "rdm infra inter rule group-";
        Name irgName = new NameBuilder().setValueName("irg name").setValue(irgNameValue).build();
        InterRuleGroup interRuleGroup = new InterRuleGroupBuilder()
            .setUuid(new Uuid(UUID.nameUUIDFromBytes((irgNameValue).getBytes(Charset.forName("UTF-8"))).toString()))
            .setName(Map.of(irgName.key(), irgName))
            .setRule(new HashMap<>(Map.of(rule.key(), rule)))
            .setAssociatedNodeRuleGroup(associatedNrgMap)
            .build();
        return new HashMap<>(Map.of(new InterRuleGroupKey(interRuleGroup.getUuid()), interRuleGroup));
    }

    public Map<MappedServiceInterfacePointKey, MappedServiceInterfacePoint> createMSIP(
            int nb, LayerProtocolName layerProtocol, String tpId, String nodeid,
            Collection<SupportedInterfaceCapability> supportedInterfaceCapability,
            OperationalState operState, AdministrativeState adminState) {
        // add them to SIP context
        Map<MappedServiceInterfacePointKey, MappedServiceInterfacePoint> msipl = new HashMap<>();
        for (int i = 0; i < nb; i++) {
            String sipName = nb == 1 ? String.join("+", "SIP", nodeid, tpId)
                    : String.join("+", "SIP", nodeid, tpId, "Nber", String.valueOf(i));
            LOG.info("SIP = {}", sipName);
            Uuid sipUuid = new Uuid(UUID.nameUUIDFromBytes(sipName.getBytes(Charset.forName("UTF-8"))).toString());
            MappedServiceInterfacePoint msip =
                new MappedServiceInterfacePointBuilder().setServiceInterfacePointUuid(sipUuid).build();
            ServiceInterfacePoint sip =
                createSIP(sipUuid, layerProtocol, tpId, nodeid, supportedInterfaceCapability, operState, adminState);
            this.tapiSips.put(sip.key(), sip);
            msipl.put(msip.key(), msip);
            LOG.debug("SIP created {}", sip.getUuid());
            LOG.debug("This SIP corresponds to SIP+nodeId {} + TpId {}", nodeid, tpId);
        }
        return msipl;
    }

    public List<AvailablePayloadStructure> createAvailablePayloadStructureForPhtncMedia(
            Boolean otsiProvisioned,
            Collection<SupportedInterfaceCapability> sicList,
            List<OperationalModeKey> supportedOpModes) {
        if (supportedOpModes == null || supportedOpModes.isEmpty()) {
            return null;
        }
        Integer nepRate = 0;
        Integer loopRate = 0;
        for (OperationalModeKey operationalMode : supportedOpModes) {
            // TODO a map w/ a for loop might be more indicated here
            if (operationalMode.toString().contains("800G")) {
                loopRate = 8;
            } else if (operationalMode.toString().contains("600G")) {
                loopRate = 6;
            } else if (operationalMode.toString().contains("400G")) {
                loopRate = 4;
            } else if (operationalMode.toString().contains("300G")) {
                loopRate = 3;
            } else if (operationalMode.toString().contains("200G")) {
                loopRate = 2;
            } else if (operationalMode.toString().contains("100G")) {
                loopRate = 1;
            }
            if (loopRate >= nepRate) {
                nepRate = loopRate;
            }
        }
        List<AvailablePayloadStructure> aps = new ArrayList<>();
        Integer cepInstanceNber = otsiProvisioned ? 0 : 1;
        for (SupportedInterfaceCapability sic : sicList) {
            switch (sic.getIfCapType().toString().split("\\{")[0]) {
                case "IfOCHOTU4ODU4":
                case "IfOCHOTU4ODU4Regen":
                case "IfOCHOTU4ODU4Uniregen":
                    aps.add(new AvailablePayloadStructureBuilder()
                        .setMultiplexingSequence(Set.of(PHOTONICLAYERQUALIFIEROTSi.VALUE, ODUTYPEODU4.VALUE))
                        .setNumberOfCepInstances(Uint64.valueOf(cepInstanceNber))
                        .setCapacity(
                            new CapacityBuilder()
                                .setUnit(CAPACITYUNITGBPS.VALUE)
                                .setValue(Decimal64.valueOf(100.0 * cepInstanceNber, RoundingMode.DOWN))
                                .build())
                        .build());
                    break;
                case "IfOCHOTUCnODUCn":
                case "IfOtsiOtucnOducn":
                case "IfOCHOTUCnODUCnRegen":
                case "IfOCHOTUCnODUCnUniregen":
                    aps.add(new AvailablePayloadStructureBuilder()
                        .setMultiplexingSequence(Set.of(PHOTONICLAYERQUALIFIEROTSi.VALUE, OTUTYPEOTUCN.VALUE,
                            ODUTYPEODUCN.VALUE, ODUTYPEODU4.VALUE))
                        .setNumberOfCepInstances(Uint64.valueOf(nepRate * cepInstanceNber))
                        .setCapacity(
                            new CapacityBuilder()
                                .setUnit(CAPACITYUNITGBPS.VALUE)
                                .setValue(Decimal64.valueOf(nepRate * 100.0 * cepInstanceNber, RoundingMode.DOWN))
                                .build())
                        .build());
                    break;
                default:
                    break;
            }
        }
        return aps.stream().distinct().toList();
    }

    public List<SupportedPayloadStructure> createSupportedPayloadStructureForPhtncMedia(
            Collection<SupportedInterfaceCapability> sicList, List<OperationalModeKey> supportedOpModes) {
        if (supportedOpModes == null || supportedOpModes.isEmpty()) {
            return null;
        }
        Integer nepRate = 0;
        Integer loopRate = 0;
        for (OperationalModeKey operationalMode : supportedOpModes) {
            // TODO a map w/ a for loop might be more indicated here
            if (operationalMode.toString().contains("800G")) {
                loopRate = 8;
            } else if (operationalMode.toString().contains("600G")) {
                loopRate = 6;
            } else if (operationalMode.toString().contains("400G")) {
                loopRate = 4;
            } else if (operationalMode.toString().contains("300G")) {
                loopRate = 3;
            } else if (operationalMode.toString().contains("200G")) {
                loopRate = 2;
            } else if (operationalMode.toString().contains("100G")) {
                loopRate = 1;
            }
            if (loopRate >= nepRate) {
                nepRate = loopRate;
            }
        }
        List<SupportedPayloadStructure> sps = new ArrayList<>();
        for (SupportedInterfaceCapability sic : sicList) {
            String ifCapType = sic.getIfCapType().toString().split("\\{")[0];
            switch (ifCapType) {
                case "IfOCHOTU4ODU4":
                case "IfOCHOTU4ODU4Regen":
                case "IfOCHOTU4ODU4Uniregen":
                    sps.add(new SupportedPayloadStructureBuilder()
                        .setMultiplexingSequence(Set.of(PHOTONICLAYERQUALIFIEROTSi.VALUE, ODUTYPEODU4.VALUE))
                        .setNumberOfCepInstances(Uint64.valueOf(1))
                        .setCapacity(
                            new CapacityBuilder()
                                .setUnit(CAPACITYUNITGBPS.VALUE)
                                .setValue(Decimal64.valueOf(100.0, RoundingMode.DOWN))
                                .build())
                        .build());
                    break;
                case "IfOCHOTUCnODUCn":
                case "IfOtsiOtucnOducn":
                case "IfOCHOTUCnODUCnRegen":
                case "IfOCHOTUCnODUCnUniregen":
                    sps.add(new SupportedPayloadStructureBuilder()
                        .setMultiplexingSequence(Set.of(
                            PHOTONICLAYERQUALIFIEROTSi.VALUE, OTUTYPEOTUCN.VALUE,
                            ODUTYPEODUCN.VALUE, ODUTYPEODU4.VALUE))
                        .setNumberOfCepInstances(Uint64.valueOf(nepRate))
                        .setCapacity(
                            new CapacityBuilder()
                                .setUnit(CAPACITYUNITGBPS.VALUE)
                                .setValue(Decimal64.valueOf(nepRate * 100.0, RoundingMode.DOWN))
                                .build())
                        .build());
                    break;
                default:
                    break;
            }
        }
        return sps.stream().distinct().toList();
    }

    @edu.umd.cs.findbugs.annotations.SuppressFBWarnings(value = "SF_SWITCH_FALLTHROUGH",
        justification = "Voluntarily No break in switchcase where comment is inserted in following method")
    public List<SupportedCepLayerProtocolQualifierInstances> createSupportedCepLayerProtocolQualifier(
            Collection<SupportedInterfaceCapability> sicList, LayerProtocolName lpn) {
        if (sicList == null) {
            return new ArrayList<>(List.of(
                new SupportedCepLayerProtocolQualifierInstancesBuilder()
                    .setLayerProtocolQualifier(PHOTONICLAYERQUALIFIEROTS.VALUE)
                    .setNumberOfCepInstances(Uint64.valueOf(1))
                    .build()));
        }
        LOG.debug("SIC list = {}", sicList);
        List<SupportedCepLayerProtocolQualifierInstances> sclpqiList = new ArrayList<>();
        for (SupportedInterfaceCapability sic : sicList) {
            String ifCapType = sic.getIfCapType().toString().split("\\{")[0];
            //TODO a map is probably more indicated than switch/case here
            switch (lpn.getName()) {
                case "ETH":
                case "DSR":
                    switch (ifCapType) {
                        case "If1GEODU0":
                            sclpqiList.add(new SupportedCepLayerProtocolQualifierInstancesBuilder()
                                .setLayerProtocolQualifier(ODUTYPEODU0.VALUE)
                                .setNumberOfCepInstances(Uint64.valueOf(1))
                                .build());
                            sclpqiList.add(new SupportedCepLayerProtocolQualifierInstancesBuilder()
                                .setLayerProtocolQualifier(DIGITALSIGNALTYPEGigE.VALUE)
                                .setNumberOfCepInstances(Uint64.valueOf(1))
                                .build());
                            break;
                        case "If10GEODU2e":
                            sclpqiList.add(new SupportedCepLayerProtocolQualifierInstancesBuilder()
                                .setLayerProtocolQualifier(ODUTYPEODU2E.VALUE)
                                .setNumberOfCepInstances(Uint64.valueOf(1))
                                .build());
                            sclpqiList.add(new SupportedCepLayerProtocolQualifierInstancesBuilder()
                                .setLayerProtocolQualifier(DIGITALSIGNALTYPE10GigELAN.VALUE)
                                .setNumberOfCepInstances(Uint64.valueOf(1))
                                .build());
                            break;
                        case "If10GEODU2":
                            sclpqiList.add(new SupportedCepLayerProtocolQualifierInstancesBuilder()
                                .setLayerProtocolQualifier(ODUTYPEODU2.VALUE)
                                .setNumberOfCepInstances(Uint64.valueOf(1))
                                .build());
                            sclpqiList.add(new SupportedCepLayerProtocolQualifierInstancesBuilder()
                                .setLayerProtocolQualifier(DIGITALSIGNALTYPE10GigELAN.VALUE)
                                .setNumberOfCepInstances(Uint64.valueOf(1))
                                .build());
                            break;
                        case "If10GE":
                            sclpqiList.add(new SupportedCepLayerProtocolQualifierInstancesBuilder()
                                .setLayerProtocolQualifier(DIGITALSIGNALTYPE10GigELAN.VALUE)
                                .setNumberOfCepInstances(Uint64.valueOf(1))
                                .build());
                            break;
                        case "If100GEODU4":
                            sclpqiList.add(new SupportedCepLayerProtocolQualifierInstancesBuilder()
                                .setLayerProtocolQualifier(DIGITALSIGNALTYPE100GigE.VALUE)
                                .setNumberOfCepInstances(Uint64.valueOf(1))
                                .build());
                            sclpqiList.add(new SupportedCepLayerProtocolQualifierInstancesBuilder()
                                .setLayerProtocolQualifier(ODUTYPEODU4.VALUE)
                                .setNumberOfCepInstances(Uint64.valueOf(1))
                                .build());
                            break;
                        case "If100GE":
                            sclpqiList.add(new SupportedCepLayerProtocolQualifierInstancesBuilder()
                                .setLayerProtocolQualifier(DIGITALSIGNALTYPE100GigE.VALUE)
                                .setNumberOfCepInstances(Uint64.valueOf(1))
                                .build());
                            break;
                        case "IfOCHOTU4ODU4":
                        case "IfOCH":
                            sclpqiList.add(new SupportedCepLayerProtocolQualifierInstancesBuilder()
                                .setLayerProtocolQualifier(ODUTYPEODU4.VALUE)
                                .setNumberOfCepInstances(Uint64.valueOf(1))
                                .build());
                            break;
                        default:
                            LOG.error("IfCapability type not managed");
                            break;
                    }
                    break;
                case "ODU":
                    switch (ifCapType) {
                        case "If1GEODU0":
                            sclpqiList.add(new SupportedCepLayerProtocolQualifierInstancesBuilder()
                                .setLayerProtocolQualifier(ODUTYPEODU0.VALUE)
                                .setNumberOfCepInstances(Uint64.valueOf(1))
                                .build());
                            break;
                        case "If10GEODU2e":
                            sclpqiList.add(new SupportedCepLayerProtocolQualifierInstancesBuilder()
                                .setLayerProtocolQualifier(ODUTYPEODU2E.VALUE)
                                .setNumberOfCepInstances(Uint64.valueOf(1))
                                .build());
                            break;
                        case "If10GEODU2":
                        case "If10GE":
                            sclpqiList.add(new SupportedCepLayerProtocolQualifierInstancesBuilder()
                                .setLayerProtocolQualifier(ODUTYPEODU2.VALUE)
                                .setNumberOfCepInstances(Uint64.valueOf(1))
                                .build());
                            break;
                        case "If100GEODU4":
                        case "If100GE":
                        case "IfOCHOTU4ODU4":
                        case "IfOCH":
                            sclpqiList.add(new SupportedCepLayerProtocolQualifierInstancesBuilder()
                                .setLayerProtocolQualifier(ODUTYPEODU4.VALUE)
                                .setNumberOfCepInstances(Uint64.valueOf(1))
                                .build());
                            break;
                        default:
                            LOG.error("IfCapability type not managed");
                            break;
                    }
                    break;
                case "PHOTONIC_MEDIA":
                    switch (ifCapType) {
                        case "IfOCHOTUCnODUCn":
                        case "IfOtsiOtucnOducn":
                        case "IfOCHOTUCnODUCnRegen":
                        case "IfOCHOTUCnODUCnUniregen":
                            sclpqiList.add(new SupportedCepLayerProtocolQualifierInstancesBuilder()
                                .setLayerProtocolQualifier(ODUTYPEODUCN.VALUE)
                                .setNumberOfCepInstances(Uint64.valueOf(1))
                                .build());
                            sclpqiList.add(new SupportedCepLayerProtocolQualifierInstancesBuilder()
                                .setLayerProtocolQualifier(OTUTYPEOTUCN.VALUE)
                                .setNumberOfCepInstances(Uint64.valueOf(1))
                                .build());
                        //fallthrough
                        case "IfOCH":
                        case "IfOCHOTU4ODU4":
                        case "IfOCHOTU4ODU4Regen":
                        case "IfOCHOTU4ODU4Uniregen":
                            sclpqiList.add(new SupportedCepLayerProtocolQualifierInstancesBuilder()
                                .setLayerProtocolQualifier(PHOTONICLAYERQUALIFIEROTSiMC.VALUE)
                                .setNumberOfCepInstances(Uint64.valueOf(1))
                                .build());
                            sclpqiList.add(new SupportedCepLayerProtocolQualifierInstancesBuilder()
                                .setLayerProtocolQualifier(PHOTONICLAYERQUALIFIEROTS.VALUE)
                                .setNumberOfCepInstances(Uint64.valueOf(1))
                                .build());
                            break;
                        default:
                            break;
                    }
                    break;
                default:
                    LOG.error("Layer Protocol Name is unknown {}", lpn.getName());
                    break;
            }
        }
        return sclpqiList.stream().distinct().toList();
    }

    public Map<Double, Double> getXpdrUsedWavelength(TerminationPoint tp) {
        var tpAug = tp.augmentation(
            org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev230526.TerminationPoint1.class);
        if (tpAug == null) {
            return null;
        }
        XpdrNetworkAttributes xnatt = tpAug.getXpdrNetworkAttributes();
        if (xnatt == null) {
            return null;
        }
        var xnattWvlgth = xnatt.getWavelength();
        if (xnattWvlgth == null) {
            return null;
        }
        var freq = xnattWvlgth.getFrequency();
        if (freq == null) {
            return null;
        }
        var width = xnattWvlgth.getWidth();
        if (width == null) {
            return null;
        }
        Double freqValue = freq.getValue().doubleValue();
        Double widthValue = freq.getValue().doubleValue();
        Double value = freqValue - widthValue * 0.001 / 2;
        return new HashMap<>(Map.of(value, value));
    }

    public Map<Double, Double> getPPUsedWavelength(TerminationPoint tp) {
        var tpAug = tp.augmentation(
            org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev230526.TerminationPoint1.class);
        if (tpAug == null) {
            return null;
        }
        PpAttributes ppAtt = tpAug.getPpAttributes();
        if (ppAtt == null) {
            return null;
        }
        var usedWvl = ppAtt.getUsedWavelength();
        if (usedWvl == null || usedWvl.isEmpty()) {
            return null;
        }
        var usedWvlfirstValue = usedWvl.entrySet().iterator().next().getValue();
        Double centFreq = usedWvlfirstValue.getFrequency().getValue().doubleValue();
        Double width = usedWvlfirstValue.getWidth().getValue().doubleValue();
        return  new HashMap<>(Map.of(centFreq - width * 0.001 / 2, centFreq + width * 0.001 / 2));
    }

    public Map<Double, Double> getTTPUsedFreqMap(TerminationPoint tp) {
        byte[] byteArray = new byte[GridConstant.NB_OCTECTS];
        Arrays.fill(byteArray, (byte) GridConstant.AVAILABLE_SLOT_VALUE);
        var termPoint1 = tp.augmentation(org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev230526
            .TerminationPoint1.class);
        if (termPoint1 == null) {
            return null;
        }
        TxTtpAttributes txttpAtt = termPoint1.getTxTtpAttributes();
        if (txttpAtt  == null) {
            return null;
        }
        var txttpAttUsedWvl = txttpAtt.getUsedWavelengths();
        if (txttpAttUsedWvl == null || txttpAttUsedWvl.isEmpty()) {
            var txttpAttAvlFreqMaps = txttpAtt.getAvailFreqMaps();
            if (txttpAttAvlFreqMaps == null || !txttpAttAvlFreqMaps.keySet().toString().contains(GridConstant.C_BAND)) {
                return null;
            }
            byte[] freqBitSet = new byte[GridConstant.NB_OCTECTS];
            LOG.debug("Creation of Bitset {}", freqBitSet);
            freqBitSet = txttpAttAvlFreqMaps.entrySet().stream()
                .filter(afm -> afm.getKey().toString().equals(GridConstant.C_BAND))
                .findFirst().orElseThrow().getValue().getFreqMap();
            for (int i = 0; i < GridConstant.EFFECTIVE_BITS; i++) {
                if (freqBitSet[i] == 0) {
                    freqBitSet[i] = 1;
                } else {
                    freqBitSet[i] = 0;
                }
            }
            return getFreqMapFromBitSet(freqBitSet);
        }
        Map<Double,Double> freqMap = new HashMap<>();
        for (Map.Entry<UsedWavelengthsKey, UsedWavelengths> usedLambdas : txttpAttUsedWvl.entrySet()) {
            Double centFreq = usedLambdas.getValue().getFrequency().getValue().doubleValue();
            Double width = usedLambdas.getValue().getWidth().getValue().doubleValue();
            freqMap.put(centFreq - width * 0.001 / 2, centFreq + width * 0.001 / 2);
        }
        return freqMap;
    }

    public Map<Double, Double> getTTPAvailableFreqMap(TerminationPoint tp) {
        var termPoint1 = tp.augmentation(org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev230526
            .TerminationPoint1.class);
        if (termPoint1 == null) {
            return null;
        }
        TxTtpAttributes txttpAtt = termPoint1.getTxTtpAttributes();
        if (txttpAtt == null) {
            return null;
        }
        var avlFreqMaps = txttpAtt.getAvailFreqMaps();
        if (avlFreqMaps == null || !avlFreqMaps.keySet().toString().contains(GridConstant.C_BAND)) {
            return null;
        }
        byte[] byteArray = new byte[GridConstant.NB_OCTECTS];
        LOG.debug("Creation of Bitset {}", byteArray);
        return getFreqMapFromBitSet(
            avlFreqMaps.entrySet().stream()
                .filter(afm -> afm.getKey().toString().equals(GridConstant.C_BAND))
                .findFirst().orElseThrow().getValue().getFreqMap());
    }

    public Map<Double, Double> getTTP11AvailableFreqMap(
            org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev230526.TerminationPoint1 tp) {
        if (tp == null) {
            return null;
        }
        TxTtpAttributes txttpAtt = tp.getTxTtpAttributes();
        if (txttpAtt == null) {
            return null;
        }
        var avlFreqMaps = txttpAtt.getAvailFreqMaps();
        if (avlFreqMaps == null || !avlFreqMaps.keySet().toString().contains(GridConstant.C_BAND)) {
            return null;
        }
        byte[] byteArray = new byte[GridConstant.NB_OCTECTS];
        LOG.debug("Creation of Bitset {}", byteArray);
        return getFreqMapFromBitSet(
            avlFreqMaps.entrySet().stream()
                .filter(afm -> afm.getKey().toString().equals(GridConstant.C_BAND))
                .findFirst().orElseThrow().getValue().getFreqMap());
    }

    public Map<Double, Double> getPP11UsedWavelength(
            org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev230526.TerminationPoint1 tp) {
        if (tp == null) {
            return null;
        }
        PpAttributes ppAtt = tp.getPpAttributes();
        if (ppAtt == null) {
            return null;
        }
        var usedWvl = ppAtt.getUsedWavelength();
        if (usedWvl == null || usedWvl.isEmpty()) {
            return null;
        }
        var usedWvlFirstValue = usedWvl.entrySet().iterator().next().getValue();
        Double centFreq = usedWvlFirstValue.getFrequency().getValue().doubleValue();
        Double width = usedWvlFirstValue.getWidth().getValue().doubleValue();
        return new HashMap<>(Map.of(centFreq - width * 0.001 / 2, centFreq + width * 0.001 / 2));
    }

    public Map<Double, Double> getTTP11UsedFreqMap(
            org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev230526.TerminationPoint1 tp) {
        byte[] byteArray = new byte[GridConstant.NB_OCTECTS];
        Arrays.fill(byteArray, (byte) GridConstant.AVAILABLE_SLOT_VALUE);
        if (tp == null) {
            return null;
        }
        TxTtpAttributes txttpAtt = tp.getTxTtpAttributes();
        if (txttpAtt == null) {
            return null;
        }
        var txttpAttUsedWvl = txttpAtt.getUsedWavelengths();
        if (txttpAttUsedWvl == null || txttpAttUsedWvl.isEmpty()) {
            var txttpAttAvlFreqMaps = txttpAtt.getAvailFreqMaps();
            if (txttpAttAvlFreqMaps == null || !txttpAttAvlFreqMaps.keySet().toString().contains(GridConstant.C_BAND)) {
                return null;
            }
            byte[] freqBitSet = new byte[GridConstant.NB_OCTECTS];
            LOG.debug("Creation of Bitset {}", freqBitSet);
            freqBitSet = txttpAttAvlFreqMaps.entrySet().stream()
                .filter(afm -> afm.getKey().toString().equals(GridConstant.C_BAND))
                .findFirst().orElseThrow().getValue().getFreqMap();
            for (int i = 0; i < GridConstant.EFFECTIVE_BITS; i++) {
                if (freqBitSet[i] == 0) {
                    freqBitSet[i] = 1;
                } else {
                    freqBitSet[i] = 0;
                }
            }
            return getFreqMapFromBitSet(freqBitSet);
        }
        Map<Double,Double> freqMap = new HashMap<>();
        for (Map.Entry<UsedWavelengthsKey, UsedWavelengths> usedLambdas : txttpAttUsedWvl.entrySet()) {
            var usedLambdasValue = usedLambdas.getValue();
            Double centFreq = usedLambdasValue.getFrequency().getValue().doubleValue();
            Double width = usedLambdasValue.getWidth().getValue().doubleValue();
            freqMap.put(centFreq - width * 0.001 / 2, centFreq + width * 0.001 / 2);
        }
        return freqMap;
    }

    public Map<Double, Double> getFreqMapFromBitSet(byte[] byteArray) {
        // Provides a Map <LowerFreq, HigherFreq> describing start and stop frequencies of all slots that are available
        // in the ByteArray describing the spectrum : bit sets initially sets to 1/true
        // In case the byte array has been inverted before calling this method, it provides respectively a map
        // describing all occupied slots!
        Map<Double,Double> freqMap = new HashMap<>();
        Double startFreq = GridConstant.START_EDGE_FREQUENCY;
        Double stopFreq = 0.0;
        boolean occupied = false;
        if (byteArray[0] == 0) {
            occupied = true;
        }
        for (int index = 0 ; index < GridConstant.EFFECTIVE_BITS ; index++) {
            if (occupied) {
                if (byteArray[index] == 1) {
                    startFreq = GridUtils.getStartFrequencyFromIndex(index).doubleValue();
                    stopFreq = GridUtils.getStartFrequencyFromIndex(index).doubleValue();
                    occupied = false;
                }
            } else {
                if (byteArray[index] == 0) {
                    stopFreq = GridUtils.getStartFrequencyFromIndex(index).doubleValue();
                    occupied = true;
                }
            }
            if (occupied) {
                if (stopFreq.doubleValue() > startFreq.doubleValue()) {
                    freqMap.put(startFreq, stopFreq);
                    startFreq = stopFreq;
                }
            } else {
                if (index == GridConstant.EFFECTIVE_BITS - 1 && startFreq.doubleValue() == stopFreq.doubleValue()) {
                    stopFreq = GridUtils.getStopFrequencyFromIndex(index).doubleValue();
                    freqMap.put(startFreq, stopFreq);
                }
            }
        }
        return freqMap;
    }

    public OwnedNodeEdgePointBuilder addPayloadStructureAndPhotSpecToOnep(String nodeId,
            Map<Double, Double> freqMap, List<OperationalModeKey> operModeList,
            Collection<SupportedInterfaceCapability> sicColl, OwnedNodeEdgePointBuilder onepBldr, String keyword) {
        if (!String.join("+", nodeId, TapiStringConstants.OTSI_MC).equals(keyword)
                && !String.join("+", nodeId, TapiStringConstants.PHTNC_MEDIA_OTS).equals(keyword)) {
            return onepBldr;
        }
        LOG.debug("Entering LOOP Step1");
        double naz = 0.01;
        Double lowSupFreq = GridConstant.START_EDGE_FREQUENCY * 1E09 ;
        Double upSupFreq = lowSupFreq + GridConstant.GRANULARITY * GridConstant.EFFECTIVE_BITS * 1E06 + naz;
        lowSupFreq += naz;
        // Creating OTS & OTSI_MC NEP specific attributes
        onepBldr.setSupportedPayloadStructure(
            createSupportedPayloadStructureForPhtncMedia(sicColl,operModeList));
        SpectrumCapabilityPacBuilder spectrumPac = new SpectrumCapabilityPacBuilder();
        OccupiedSpectrumBuilder ospecBd = new OccupiedSpectrumBuilder();
        if (freqMap == null || freqMap.isEmpty()) {
//                TODO: verify if we need to fill OcupiedSpectrum as follows when no lambda provisioned
//                ospecBd
//                    .setUpperFrequency(Uint64.valueOf(0))
//                    .setLowerFrequency(Uint64.valueOf(0));
            onepBldr.setAvailablePayloadStructure(
                createAvailablePayloadStructureForPhtncMedia(false, sicColl,operModeList));
            AvailableSpectrum  aspec = new AvailableSpectrumBuilder()
                .setLowerFrequency(Uint64.valueOf(Math.round(lowSupFreq)))
                .setUpperFrequency(Uint64.valueOf(Math.round(upSupFreq)))
                .build();
            spectrumPac.setAvailableSpectrum(
                new HashMap<AvailableSpectrumKey, AvailableSpectrum>(Map.of(
                    new AvailableSpectrumKey(aspec.getLowerFrequency(), aspec.getUpperFrequency()), aspec)));
        } else {
            LOG.debug("Entering LOOP Step2");
            onepBldr.setAvailablePayloadStructure(
                createAvailablePayloadStructureForPhtncMedia(true, sicColl,operModeList));
            for (Map.Entry<Double, Double> frequency : freqMap.entrySet()) {
                ospecBd
                    .setLowerFrequency(Uint64.valueOf(Math.round(frequency.getKey().doubleValue() * 1E09)))
                    .setUpperFrequency(Uint64.valueOf(Math.round(frequency.getValue().doubleValue() * 1E09)));
            }
            OccupiedSpectrum ospec = ospecBd.build();
            spectrumPac.setOccupiedSpectrum(
                new HashMap<OccupiedSpectrumKey, OccupiedSpectrum>(Map.of(
                    new OccupiedSpectrumKey(ospec.getLowerFrequency(), ospec.getUpperFrequency()), ospec)));
        }
        LOG.debug("Entering LOOP Step3");
        SupportableSpectrum  sspec = new SupportableSpectrumBuilder()
            .setLowerFrequency(Uint64.valueOf(Math.round(lowSupFreq)))
            .setUpperFrequency(Uint64.valueOf(Math.round(upSupFreq)))
            .build();
        spectrumPac.setSupportableSpectrum(
            new HashMap<SupportableSpectrumKey, SupportableSpectrum>(Map.of(
                new SupportableSpectrumKey(sspec.getLowerFrequency(), sspec.getUpperFrequency()), sspec)));
        LOG.debug("Entering LOOP Step4");
        var onep1 = new org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.photonic.media.rev221121
                .OwnedNodeEdgePoint1Builder()
            .setPhotonicMediaNodeEdgePointSpec(
                new PhotonicMediaNodeEdgePointSpecBuilder().setSpectrumCapabilityPac(spectrumPac.build()).build())
            .build();
        LOG.debug("creating Photonic NEP SPEC for node {} and nep {}", nodeId, onep1);
        onepBldr.addAugmentation(onep1);
        LOG.debug("Entering LOOP Step5");
        return onepBldr;
    }


    public OwnedNodeEdgePointBuilder addPhotSpecToRoadmOnep(String nodeId,
            Map<Double, Double> usedFreqMap, Map<Double, Double> availableFreqMap,
            OwnedNodeEdgePointBuilder onepBldr, String keyword) {
        if (!String.join("+", nodeId, TapiStringConstants.PHTNC_MEDIA_OTS).equals(keyword)
                && !String.join("+", nodeId, TapiStringConstants.PHTNC_MEDIA_OMS).equals(keyword)) {
            return onepBldr;
        }
        // Creating OTS/OMS NEP specific attributes
        double naz = 0.01;
        Double lowSupFreq = GridConstant.START_EDGE_FREQUENCY * 1E09 ;
        Double upSupFreq = lowSupFreq + GridConstant.GRANULARITY * GridConstant.EFFECTIVE_BITS * 1E06 + naz;
        lowSupFreq += naz;
        SpectrumCapabilityPacBuilder spectrumPac = new SpectrumCapabilityPacBuilder();
        Map<Double, Double> freqMap = null;
        if (usedFreqMap == null || usedFreqMap.isEmpty()) {
            freqMap = Map.of(lowSupFreq, upSupFreq);
        } else {
            Map<OccupiedSpectrumKey, OccupiedSpectrum> ospecMap = new HashMap<>();
            OccupiedSpectrumBuilder ospecBd = new OccupiedSpectrumBuilder();
            for (Map.Entry<Double, Double> frequency : usedFreqMap.entrySet()) {
                ospecBd
                    .setLowerFrequency(Uint64.valueOf(Math.round(frequency.getKey().doubleValue() * 1E09)))
                    .setUpperFrequency(Uint64.valueOf(Math.round(frequency.getValue().doubleValue() * 1E09)));
                OccupiedSpectrum ospec = ospecBd.build();
                ospecMap.put(new OccupiedSpectrumKey(ospec.getLowerFrequency(), ospec.getUpperFrequency()), ospec);
            }
            spectrumPac.setOccupiedSpectrum(ospecMap);
        }
        if (availableFreqMap != null && !availableFreqMap.isEmpty()) {
            freqMap = availableFreqMap;
        }
        if (freqMap != null) {
            Map<AvailableSpectrumKey, AvailableSpectrum> aspecMap = new HashMap<>();
            AvailableSpectrumBuilder aspecBd = new AvailableSpectrumBuilder();
            for (Map.Entry<Double, Double> frequency : availableFreqMap.entrySet()) {
                aspecBd
                    .setLowerFrequency(Uint64.valueOf(Math.round(frequency.getKey().doubleValue() * 1E09)))
                    .setUpperFrequency(Uint64.valueOf(Math.round(frequency.getValue().doubleValue() * 1E09)));
                AvailableSpectrum aspec = aspecBd.build();
                aspecMap.put(new AvailableSpectrumKey(aspec.getLowerFrequency(), aspec.getUpperFrequency()), aspec);
            }
            spectrumPac.setAvailableSpectrum(aspecMap);
        }
        SupportableSpectrum  sspec = new SupportableSpectrumBuilder()
            .setLowerFrequency(Uint64.valueOf(Math.round(lowSupFreq)))
            .setUpperFrequency(Uint64.valueOf(Math.round(upSupFreq)))
            .build();
        spectrumPac.setSupportableSpectrum(
            new HashMap<SupportableSpectrumKey, SupportableSpectrum>(Map.of(
                new SupportableSpectrumKey(sspec.getLowerFrequency(), sspec.getUpperFrequency()), sspec)));
        var onep1 = new org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.photonic.media.rev221121
                .OwnedNodeEdgePoint1Builder()
            .setPhotonicMediaNodeEdgePointSpec(
                new PhotonicMediaNodeEdgePointSpecBuilder().setSpectrumCapabilityPac(spectrumPac.build()).build())
            .build();
        onepBldr.addAugmentation(onep1);
        LOG.debug("Add Photonic Node Edge point Spec to {} including available Spectrum {} = ",
            onepBldr.getName(),
            onep1.getPhotonicMediaNodeEdgePointSpec().getSpectrumCapabilityPac().getAvailableSpectrum());
        return onepBldr;
    }

    private OduSwitchingPools createOduSwitchingPoolForTp100G() {
        Map<NonBlockingListKey, NonBlockingList> nblMap = new HashMap<>();
        int count = 1;
        for (TerminationPoint tp : this.oorNetworkPortList) {
            NonBlockingList nbl = new NonBlockingListBuilder()
                .setNblNumber(Uint16.valueOf(count))
                .setTpList(new HashSet<>(Set.of(
                    tp.getTpId(),
                    tp.augmentation(org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev230526
                            .TerminationPoint1.class)
                        .getAssociatedConnectionMapTp().iterator().next())))
                .build();
            nblMap.put(nbl.key(), nbl);
            count++;
        }
        return new OduSwitchingPoolsBuilder()
            .setNonBlockingList(nblMap)
            .setSwitchingPoolNumber(Uint16.valueOf(1))
            .build();
    }

    private org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.topology.Node createTapiNode(
            Map<NameKey, Name> nodeNames, Set<LayerProtocolName> layerProtocols) {
        Uuid nodeUuid = null;
        Map<OwnedNodeEdgePointKey, OwnedNodeEdgePoint> onepl = new HashMap<>();
        Map<NodeRuleGroupKey, NodeRuleGroup> nodeRuleGroupList = new HashMap<>();
        if (layerProtocols.contains(LayerProtocolName.DSR)
                || layerProtocols.contains(LayerProtocolName.PHOTONICMEDIA)) {
            Rule rule = new RuleBuilder()
                .setLocalId("forward")
                .setForwardingRule(FORWARDINGRULEMAYFORWARDACROSSGROUP.VALUE)
                .setRuleType(new HashSet<>(Set.of(RuleType.FORWARDING)))
                .build();
            nodeUuid = getNodeUuid4Dsr(onepl, nodeRuleGroupList, new HashMap<>(Map.of(rule.key(), rule)));
        } else {
            var nodeName = nodeNames.get(nodeNames.keySet().iterator().next());
            LOG.error("Undefined LayerProtocolName for {} node {}", nodeName.getValueName(), nodeName.getValue());
        }
     // Empty random creation of mandatory fields for avoiding errors....
        CostCharacteristic costCharacteristic = new CostCharacteristicBuilder()
            .setCostAlgorithm("Restricted Shortest Path - RSP")
            .setCostName("HOP_COUNT")
            .setCostValue(TapiStringConstants.COST_HOP_VALUE)
            .build();
        LatencyCharacteristic latencyCharacteristic = new LatencyCharacteristicBuilder()
            .setFixedLatencyCharacteristic(TapiStringConstants.FIXED_LATENCY_VALUE)
            .setQueuingLatencyCharacteristic(TapiStringConstants.QUEING_LATENCY_VALUE)
            .setJitterCharacteristic(TapiStringConstants.JITTER_VALUE)
            .setWanderCharacteristic(TapiStringConstants.WANDER_VALUE)
            .setTrafficPropertyName("FIXED_LATENCY")
            .build();
        RiskCharacteristic riskCharacteristic = new RiskCharacteristicBuilder()
            .setRiskCharacteristicName("risk characteristic")
            .setRiskIdentifierList(Set.of("risk identifier1", "risk identifier2"))
            .build();
        return new NodeBuilder()
            .setUuid(nodeUuid)
            .setName(nodeNames)
            .setLayerProtocolName(layerProtocols)
            .setAdministrativeState(transformAsToTapiAdminState(this.ietfNodeAdminState.getName()))
            .setOperationalState(transformOsToTapiOperationalState(this.ietfNodeOperState.getName()))
            .setLifecycleState(LifecycleState.INSTALLED)
            .setOwnedNodeEdgePoint(onepl)
            .setNodeRuleGroup(nodeRuleGroupList)
            .setCostCharacteristic(Map.of(costCharacteristic.key(), costCharacteristic))
            .setLatencyCharacteristic(Map.of(latencyCharacteristic.key(), latencyCharacteristic))
            .setRiskParameterPac(
                new RiskParameterPacBuilder()
                    .setRiskCharacteristic(Map.of(riskCharacteristic.key(), riskCharacteristic))
                    .build())
            .setErrorCharacteristic("error")
            .setLossCharacteristic("loss")
            .setRepeatDeliveryCharacteristic("repeat delivery")
            .setDeliveryOrderCharacteristic("delivery order")
            .setUnavailableTimeCharacteristic("unavailable time")
            .setServerIntegrityProcessCharacteristic("server integrity process")
            .build();
    }

    private Uuid getNodeUuid4Dsr(
            Map<OwnedNodeEdgePointKey, OwnedNodeEdgePoint> onepl,
            Map<NodeRuleGroupKey, NodeRuleGroup> nodeRuleGroupList,
            Map<RuleKey, Rule> ruleList) {
        // client NEP DSR creation on DSR/ODU node
        for (int i = 0; i < oorClientPortList.size(); i++) {
            String nodeIdDsr = String.join("+",
                this.ietfNodeId, TapiStringConstants.DSR, oorClientPortList.get(i).getTpId().getValue());
            Uuid nepUuid = new Uuid(UUID.nameUUIDFromBytes(nodeIdDsr.getBytes(Charset.forName("UTF-8"))).toString());
            LOG.info("NEP = {} has Uuid {} ", nodeIdDsr, nepUuid);
            this.uuidMap.put(nodeIdDsr, nepUuid);
            Name name = new NameBuilder()
                .setValue(nodeIdDsr)
                .setValueName(OpenroadmNodeType.TPDR.equals(this.ietfNodeType) ? "100G-tpdr" : "NodeEdgePoint_C")
                .build();
            OwnedNodeEdgePoint onep = createNep(
                oorClientPortList.get(i),
                Map.of(name.key(), name),
                LayerProtocolName.DSR, LayerProtocolName.DSR, true,
                String.join("+", this.ietfNodeId, TapiStringConstants.DSR));
            onepl.put(onep.key(), onep);
        }
        // CLIENT NEP E_ODU creation on DSR/ODU node
        for (int i = 0; i < oorClientPortList.size(); i++) {
            String nodeIdEodu = String.join("+",
                this.ietfNodeId, TapiStringConstants.E_ODU, oorClientPortList.get(i).getTpId().getValue());
            Uuid nepUuid1 = new Uuid(UUID.nameUUIDFromBytes(nodeIdEodu.getBytes(Charset.forName("UTF-8"))).toString());
            LOG.info("NEP = {} has Uuid {} ", nodeIdEodu, nepUuid1);
            this.uuidMap.put(nodeIdEodu, nepUuid1);
            Name onedName = new NameBuilder()
                .setValueName("eNodeEdgePoint_N")
                .setValue(nodeIdEodu)
                .build();
            OwnedNodeEdgePoint onep = createNep(
                oorClientPortList.get(i),
                Map.of(onedName.key(), onedName),
                LayerProtocolName.ODU, LayerProtocolName.DSR, false,
                String.join("+", this.ietfNodeId, TapiStringConstants.E_ODU));
            onepl.put(onep.key(), onep);
        }
        // NETWORK NEPs I_ODU creation on DSR/ODU node
        for (int i = 0; i < oorNetworkPortList.size(); i++) {
            String nodeIdIodu = String.join("+",
                this.ietfNodeId, TapiStringConstants.I_ODU, oorNetworkPortList.get(i).getTpId().getValue());
            Uuid nepUuid1 = new Uuid(UUID.nameUUIDFromBytes(nodeIdIodu.getBytes(Charset.forName("UTF-8"))).toString());
            LOG.info("NEP = {} has Uuid {} ", nodeIdIodu, nepUuid1);
            this.uuidMap.put(nodeIdIodu, nepUuid1);
            Name onedName = new NameBuilder()
                .setValueName("iNodeEdgePoint_N")
                .setValue(nodeIdIodu)
                .build();
            OwnedNodeEdgePoint onep = createNep(
                oorNetworkPortList.get(i),
                Map.of(onedName.key(), onedName),
                LayerProtocolName.ODU, LayerProtocolName.DSR, true,
                String.join("+", this.ietfNodeId, TapiStringConstants.I_ODU));
            onepl.put(onep.key(), onep);
        }
        // NETWORK NEP OTS network on DSR/ODU node
        for (int i = 0; i < oorNetworkPortList.size(); i++) {
            String nodeIdPmOts = String.join("+",
                this.ietfNodeId, TapiStringConstants.PHTNC_MEDIA_OTS, oorNetworkPortList.get(i).getTpId().getValue());
            Uuid nepUuid2 = new Uuid(UUID.nameUUIDFromBytes(nodeIdPmOts.getBytes(Charset.forName("UTF-8"))).toString());
            LOG.info("NEP = {} has Uuid {} ", nodeIdPmOts, nepUuid2);
            this.uuidMap.put(nodeIdPmOts, nepUuid2);
            Name onedName = new NameBuilder()
                .setValueName("eNodeEdgePoint")
                .setValue(nodeIdPmOts)
                .build();
            OwnedNodeEdgePoint onep = createNep(
                oorNetworkPortList.get(i),
                Map.of(onedName.key(), onedName),
                LayerProtocolName.PHOTONICMEDIA, LayerProtocolName.PHOTONICMEDIA, true,
                String.join("+", this.ietfNodeId, TapiStringConstants.PHTNC_MEDIA_OTS));
            onepl.put(onep.key(), onep);
        }
        for (int i = 0; i < oorNetworkPortList.size(); i++) {
            String nodeIdOtMc = String.join("+",
                this.ietfNodeId, TapiStringConstants.OTSI_MC, oorNetworkPortList.get(i).getTpId().getValue());
            Uuid nepUuid3 = new Uuid(UUID.nameUUIDFromBytes(nodeIdOtMc.getBytes(Charset.forName("UTF-8"))).toString());
            LOG.info("NEP = {} has Uuid {} ", nodeIdOtMc, nepUuid3);
            this.uuidMap.put(nodeIdOtMc, nepUuid3);
            Name onedName = new NameBuilder()
                .setValueName("PhotMedNodeEdgePoint")
                .setValue(nodeIdOtMc)
                .build();
            OwnedNodeEdgePoint onep = createNep(
                oorNetworkPortList.get(i),
                Map.of(onedName.key(), onedName),
                LayerProtocolName.PHOTONICMEDIA, LayerProtocolName.PHOTONICMEDIA, true,
                String.join("+", this.ietfNodeId, TapiStringConstants.OTSI_MC));
            onepl.put(onep.key(), onep);
        }
        // create NodeRuleGroup
        int count = 1;
        LOG.debug("ODU switching pool = {}", this.oorOduSwitchingPool.nonnullNonBlockingList().values());
        String ietfXpdr = String.join("+", this.ietfNodeId, TapiStringConstants.XPDR);
        for (NonBlockingList nbl : this.oorOduSwitchingPool.nonnullNonBlockingList().values()) {
            Map<org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.node.rule.group.NodeEdgePointKey,
                org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.node.rule.group.NodeEdgePoint>
                    nepList = new HashMap<>();
            Map<org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.node.rule.group.NodeEdgePointKey,
                org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.node.rule.group.NodeEdgePoint>
                    oduNepList = new HashMap<>();
            LOG.debug("UUidMap={}", this.uuidMap.keySet());
            LOG.debug("TP list = {}", nbl.getTpList());
            for (TpId tp : nbl.getTpList()) {
                String tpValue = tp.getValue();
                LOG.debug("TP={}", tpValue);
                String ietfEoduTp = String.join("+", this.ietfNodeId, TapiStringConstants.E_ODU, tpValue);
                LOG.debug("UuidKey={}", ietfEoduTp);
                String ietfIoduTp = String.join("+", this.ietfNodeId, TapiStringConstants.I_ODU, tpValue);
                if (this.uuidMap.containsKey(String.join("+", this.ietfNodeId, TapiStringConstants.DSR, tpValue))
                        || this.uuidMap.containsKey(ietfIoduTp)) {
                    var nep = new org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121
                                .node.rule.group.NodeEdgePointBuilder()
                            .setTopologyUuid(tapiTopoUuid)
                            .setNodeUuid(this.uuidMap.get(ietfXpdr))
                            .setNodeEdgePointUuid(this.uuidMap.get(
                                String.join("+",
                                    this.ietfNodeId,
                                    tpValue.contains("CLIENT") ? TapiStringConstants.DSR : TapiStringConstants.I_ODU,
                                    tpValue)))
                            .build();
                    nepList.put(nep.key(), nep);
                }
                if (this.uuidMap.containsKey(ietfEoduTp)) {
                    var nep1 = new org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121
                                .node.rule.group.NodeEdgePointBuilder()
                            .setTopologyUuid(tapiTopoUuid)
                            .setNodeUuid(this.uuidMap.get(ietfXpdr))
                            .setNodeEdgePointUuid(this.uuidMap.get(ietfEoduTp))
                            .build();
                    oduNepList.put(nep1.key(), nep1);
                }
                if (this.uuidMap.containsKey(ietfIoduTp)) {
                // TODO already checked with DSR above -> potential factorization ?
                    var nep2 = new org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121
                                .node.rule.group.NodeEdgePointBuilder()
                            .setTopologyUuid(tapiTopoUuid)
                            .setNodeUuid(this.uuidMap.get(ietfXpdr))
                            .setNodeEdgePointUuid(this.uuidMap.get(ietfIoduTp))
                            .build();
                    oduNepList.put(nep2.key(), nep2);
                }
            }
            LOG.debug("NEPLIST (DSR/I_ODU) of [dsr node rule group] is {}", nepList);
            LOG.debug("NEPLIST (E_ODU/I_ODU) of [odu node rule group] is {}", nepList);
            // Empty random creation of mandatory fields for avoiding errors....
            CostCharacteristic costCharacteristic = new CostCharacteristicBuilder()
                .setCostAlgorithm("Restricted Shortest Path - RSP")
                .setCostName("HOP_COUNT")
                .setCostValue(TapiStringConstants.COST_HOP_VALUE)
                .build();
            LatencyCharacteristic latencyCharacteristic = new LatencyCharacteristicBuilder()
                .setFixedLatencyCharacteristic(TapiStringConstants.FIXED_LATENCY_VALUE)
                .setQueuingLatencyCharacteristic(TapiStringConstants.QUEING_LATENCY_VALUE)
                .setJitterCharacteristic(TapiStringConstants.JITTER_VALUE)
                .setWanderCharacteristic(TapiStringConstants.WANDER_VALUE)
                .setTrafficPropertyName("FIXED_LATENCY")
                .build();
            RiskCharacteristic riskCharacteristic = new RiskCharacteristicBuilder()
                .setRiskCharacteristicName("risk characteristic")
                .setRiskIdentifierList(Set.of("risk identifier1", "risk identifier2"))
                .build();
            Name nrgName = new NameBuilder()
                .setValueName("nrg name")
                .setValue("odu node rule group " + count)
                .build();
            NodeRuleGroup nodeRuleGroup1 = new NodeRuleGroupBuilder()
                .setName(Map.of(nrgName.key(), nrgName))
                .setUuid(new Uuid(UUID.nameUUIDFromBytes(("dsr node rule group " + count)
                    .getBytes(Charset.forName("UTF-8"))).toString()))
                .setRule(ruleList)
                .setNodeEdgePoint(nepList)
                .setRiskCharacteristic(Map.of(riskCharacteristic.key(), riskCharacteristic))
                .setCostCharacteristic(Map.of(costCharacteristic.key(), costCharacteristic))
                .setLatencyCharacteristic(Map.of(latencyCharacteristic.key(), latencyCharacteristic))
                .build();
            nodeRuleGroupList.put(nodeRuleGroup1.key(), nodeRuleGroup1);
            NodeRuleGroup nodeRuleGroup2 = new NodeRuleGroupBuilder()
                .setUuid(new Uuid(UUID.nameUUIDFromBytes(("odu node rule group " + count)
                    .getBytes(Charset.forName("UTF-8"))).toString()))
                .setRule(ruleList)
                .setNodeEdgePoint(oduNepList)
                .setRiskCharacteristic(Map.of(riskCharacteristic.key(), riskCharacteristic))
                .setCostCharacteristic(Map.of(costCharacteristic.key(), costCharacteristic))
                .setLatencyCharacteristic(Map.of(latencyCharacteristic.key(), latencyCharacteristic))
                .build();
            nodeRuleGroupList.put(nodeRuleGroup2.key(), nodeRuleGroup2);
            count++;
        }
        return this.uuidMap.get(ietfXpdr);
    }

    private OwnedNodeEdgePoint createNep(TerminationPoint oorTp, Map<NameKey, Name> nepNames,
            LayerProtocolName nepProtocol, LayerProtocolName nodeProtocol, boolean withSip, String keyword) {
        var tp1 = oorTp.augmentation(
            org.opendaylight.yang.gen.v1.http.org.openroadm.otn.network.topology.rev230526.TerminationPoint1.class);
        var oorTpId = oorTp.getTpId();
        var oorTpIdValue = oorTpId.getValue();
        if (tp1.getTpSupportedInterfaces() == null) {
            LOG.warn("Tp supported interface doesnt exist on TP {}", oorTpIdValue);
            return null;
        }
        TerminationPoint1 oorTpAug = oorTp.augmentation(TerminationPoint1.class);
        var oorTpAugAdmState = oorTpAug.getAdministrativeState();
        AdministrativeState adminState =
            oorTpAugAdmState == null ? null : transformAsToTapiAdminState(oorTpAugAdmState.getName());
        var oorTpAugOprState = oorTpAug.getOperationalState();
        OperationalState operState =
            oorTpAugOprState == null ? null : transformOsToTapiOperationalState(oorTpAugOprState.getName());
        Collection<SupportedInterfaceCapability> sicColl =
            tp1.getTpSupportedInterfaces().getSupportedInterfaceCapability().values();
        OwnedNodeEdgePointBuilder onepBldr = new OwnedNodeEdgePointBuilder()
            .setUuid(this.uuidMap.get(String.join("+", keyword, oorTpIdValue)))
            .setLayerProtocolName(nepProtocol)
            .setName(nepNames)
            .setSupportedCepLayerProtocolQualifierInstances(
                createSupportedCepLayerProtocolQualifier(sicColl, nepProtocol))
            .setDirection(Direction.BIDIRECTIONAL)
            .setLinkPortRole(PortRole.SYMMETRIC)
            .setAdministrativeState(adminState)
            .setOperationalState(operState)
            .setLifecycleState(LifecycleState.INSTALLED);
        if (withSip) {
            onepBldr.setMappedServiceInterfacePoint(
                createMSIP(1, nepProtocol, oorTpIdValue, keyword, sicColl, operState, adminState));
        }
        if (oorTpAug.getTpType().equals(OpenroadmTpType.XPONDERNETWORK)) {
            List<OperationalModeKey> opModeList = new ArrayList<>();
            var tp11 = oorTp.augmentation(
                org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev230526.TerminationPoint1.class);
            if (tp11 == null || tp11.getXpdrNetworkAttributes() == null) {
                for (SupportedInterfaceCapability sic : sicColl) {
                    String ifCapType = sic.getIfCapType().toString().split("\\{")[0];
                    switch (ifCapType) {
                        case "IfOCHOTUCnODUCn":
                        case "IfOCHOTUCnODUCnUniregen":
                        case "IfOCHOTUCnODUCnRegen":
                            opModeList.add(new OperationalModeKey("400G"));
                            LOG.warn(TopologyUtils.NOOPMODEDECLARED + "400G rate available", oorTpId);
                            break;
                        default:
                            break;
                    }
                }
                opModeList.add(new OperationalModeKey("100G"));
                LOG.warn(TopologyUtils.NOOPMODEDECLARED + "100G rate available", oorTpId);
            } else {
                opModeList = tp11.getXpdrNetworkAttributes().getSupportedOperationalModes().getOperationalMode()
                    .keySet().stream().toList();
            }
            onepBldr = addPayloadStructureAndPhotSpecToOnep(
                this.ietfNodeId, getXpdrUsedWavelength(oorTp), opModeList, sicColl, onepBldr, keyword);
        }
        OwnedNodeEdgePoint onep = onepBldr.build();
        LOG.debug("ConvertORToTapiTopology 1360, onep = {}", onep);
        return onep;
    }

    private ServiceInterfacePoint createSIP(Uuid sipUuid, LayerProtocolName layerProtocol, String tpId,
        String nodeid, Collection<SupportedInterfaceCapability> supportedInterfaceCapability,
        OperationalState operState, AdministrativeState adminState) {
    // TODO: what value should be set in total capacity and available capacity??
        LOG.debug("SIP name = {}", String.join("+", nodeid, tpId));
        Name sipName = new NameBuilder()
            .setValueName("SIP name")
            .setValue(String.join("+", nodeid, tpId))
            .build();
        return new ServiceInterfacePointBuilder()
            .setUuid(sipUuid)
            .setName(Map.of(sipName.key(), sipName))
            .setLayerProtocolName(layerProtocol)
            .setAdministrativeState(adminState)
            .setOperationalState(operState)
            .setLifecycleState(LifecycleState.INSTALLED)
            .setAvailableCapacity(new AvailableCapacityBuilder().build())
            .setTotalPotentialCapacity(new TotalPotentialCapacityBuilder().build())
            .setSupportedCepLayerProtocolQualifierInstances(createSipSupportedLayerProtocolQualifier(
                supportedInterfaceCapability, layerProtocol))
            .build();
    }

    @edu.umd.cs.findbugs.annotations.SuppressFBWarnings(value = "SF_SWITCH_FALLTHROUGH",
        justification = "Voluntarily No break in switchcase where comment is inserted in following method")
    private List<org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121
                .service._interface.point.SupportedCepLayerProtocolQualifierInstances>
             createSipSupportedLayerProtocolQualifier(
                Collection<SupportedInterfaceCapability> supportedInterfaceCapability, LayerProtocolName lpn) {
        if (supportedInterfaceCapability == null) {
            return new ArrayList<>(List.of(new org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121
                    .service._interface.point.SupportedCepLayerProtocolQualifierInstancesBuilder()
                .setLayerProtocolQualifier(PHOTONICLAYERQUALIFIEROTS.VALUE)
                .setNumberOfCepInstances(Uint64.valueOf(1))
                .build()));
        }
        List<org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121
            .service._interface.point.SupportedCepLayerProtocolQualifierInstances> sclpqiList = new ArrayList<>();
        for (SupportedInterfaceCapability sic : supportedInterfaceCapability) {
            String ifCapType = sic.getIfCapType().toString().split("\\{")[0];
            //TODO a map is probably more indicated than switch/case here
            switch (lpn.getName()) {
                case "ETH":
                case "DSR":
                    switch (ifCapType) {
                        case "If1GEODU0":
                            sclpqiList.add(new org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121
                                    .service._interface.point.SupportedCepLayerProtocolQualifierInstancesBuilder()
                                .setLayerProtocolQualifier(ODUTYPEODU0.VALUE)
                                .setNumberOfCepInstances(Uint64.valueOf(0))
                                .build());
                            sclpqiList.add(new org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121
                                    .service._interface.point.SupportedCepLayerProtocolQualifierInstancesBuilder()
                                .setLayerProtocolQualifier(DIGITALSIGNALTYPEGigE.VALUE)
                                .setNumberOfCepInstances(Uint64.valueOf(0))
                                .build());
                            break;
                        case "If10GEODU2e":
                            sclpqiList.add(new org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121
                                    .service._interface.point.SupportedCepLayerProtocolQualifierInstancesBuilder()
                                .setLayerProtocolQualifier(ODUTYPEODU2E.VALUE)
                                .setNumberOfCepInstances(Uint64.valueOf(0))
                                .build());
                            sclpqiList.add(new org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121
                                    .service._interface.point.SupportedCepLayerProtocolQualifierInstancesBuilder()
                                .setLayerProtocolQualifier(DIGITALSIGNALTYPE10GigELAN.VALUE)
                                .setNumberOfCepInstances(Uint64.valueOf(0))
                                .build());
                            break;
                        case "If10GEODU2":
                            sclpqiList.add(new org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121
                                    .service._interface.point.SupportedCepLayerProtocolQualifierInstancesBuilder()
                                .setLayerProtocolQualifier(ODUTYPEODU2.VALUE)
                                .setNumberOfCepInstances(Uint64.valueOf(0))
                                .build());
                            sclpqiList.add(new org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121
                                    .service._interface.point.SupportedCepLayerProtocolQualifierInstancesBuilder()
                                .setLayerProtocolQualifier(DIGITALSIGNALTYPE10GigELAN.VALUE)
                                .setNumberOfCepInstances(Uint64.valueOf(0))
                                .build());
                            break;
                        case "If10GE":
                            sclpqiList.add(new org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121
                                    .service._interface.point.SupportedCepLayerProtocolQualifierInstancesBuilder()
                                .setLayerProtocolQualifier(DIGITALSIGNALTYPE10GigELAN.VALUE)
                                .setNumberOfCepInstances(Uint64.valueOf(0))
                                .build());
                            break;
                        case "If100GEODU4":
                            sclpqiList.add(new org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121
                                    .service._interface.point.SupportedCepLayerProtocolQualifierInstancesBuilder()
                                .setLayerProtocolQualifier(DIGITALSIGNALTYPE100GigE.VALUE)
                                .setNumberOfCepInstances(Uint64.valueOf(0))
                                .build());
                            sclpqiList.add(new org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121
                                    .service._interface.point.SupportedCepLayerProtocolQualifierInstancesBuilder()
                                .setLayerProtocolQualifier(ODUTYPEODU4.VALUE)
                                .setNumberOfCepInstances(Uint64.valueOf(0))
                                .build());
                            break;
                        case "If100GE":
                            sclpqiList.add(new org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121
                                    .service._interface.point.SupportedCepLayerProtocolQualifierInstancesBuilder()
                                .setLayerProtocolQualifier(DIGITALSIGNALTYPE100GigE.VALUE)
                                .setNumberOfCepInstances(Uint64.valueOf(0))
                                .build());
                            break;
                        case "IfOCHOTU4ODU4":
                        case "IfOCH":
                            sclpqiList.add(new org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121
                                    .service._interface.point.SupportedCepLayerProtocolQualifierInstancesBuilder()
                                .setLayerProtocolQualifier(ODUTYPEODU4.VALUE)
                                .setNumberOfCepInstances(Uint64.valueOf(0))
                                .build());
                            break;
                        default:
                            LOG.error("IfCapability type not managed");
                            break;
                    }
                    break;
                case "ODU":
                    switch (ifCapType) {
                        case "If1GEODU0":
                            sclpqiList.add(new org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121
                                    .service._interface.point.SupportedCepLayerProtocolQualifierInstancesBuilder()
                                .setLayerProtocolQualifier(ODUTYPEODU0.VALUE)
                                .setNumberOfCepInstances(Uint64.valueOf(0))
                                .build());
                            break;
                        case "If10GEODU2e":
                            sclpqiList.add(new org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121
                                    .service._interface.point.SupportedCepLayerProtocolQualifierInstancesBuilder()
                                .setLayerProtocolQualifier(ODUTYPEODU2E.VALUE)
                                .setNumberOfCepInstances(Uint64.valueOf(0))
                                .build());
                            break;
                        case "If10GEODU2":
                        case "If10GE":
                            sclpqiList.add(new org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121
                                    .service._interface.point.SupportedCepLayerProtocolQualifierInstancesBuilder()
                                .setLayerProtocolQualifier(ODUTYPEODU2.VALUE)
                                .setNumberOfCepInstances(Uint64.valueOf(0))
                                .build());
                            break;
                        case "If100GEODU4":
                        case "If100GE":
                        case "IfOCHOTU4ODU4":
                        case "IfOCH":
                            sclpqiList.add(new org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121
                                    .service._interface.point.SupportedCepLayerProtocolQualifierInstancesBuilder()
                                .setLayerProtocolQualifier(ODUTYPEODU4.VALUE)
                                .setNumberOfCepInstances(Uint64.valueOf(0))
                                .build());
                            break;
                        default:
                            LOG.error("IfCapability type not managed");
                            break;
                    }
                    break;
                case "PHOTONIC_MEDIA":
                    switch (ifCapType) {
                        case "IfOCHOTUCnODUCn":
                        case "IfOtsiOtucnOducn":
                        case "IfOCHOTUCnODUCnRegen":
                        case "IfOCHOTUCnODUCnUniregen":
                            sclpqiList.add(new org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121
                                    .service._interface.point.SupportedCepLayerProtocolQualifierInstancesBuilder()
                                .setLayerProtocolQualifier(ODUTYPEODUCN.VALUE)
                                .setNumberOfCepInstances(Uint64.valueOf(1))
                                .build());
                            sclpqiList.add(new org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121
                                    .service._interface.point.SupportedCepLayerProtocolQualifierInstancesBuilder()
                                .setLayerProtocolQualifier(OTUTYPEOTUCN.VALUE)
                                .setNumberOfCepInstances(Uint64.valueOf(1))
                                .build());
                         //fallthrough
                        case "IfOCH":
                        case "IfOCHOTU4ODU4":
                        case "IfOCHOTU4ODU4Regen":
                        case "IfOCHOTU4ODU4Uniregen":
                            sclpqiList.add(new org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121
                                    .service._interface.point.SupportedCepLayerProtocolQualifierInstancesBuilder()
                                .setLayerProtocolQualifier(PHOTONICLAYERQUALIFIEROTSiMC.VALUE)
                                .setNumberOfCepInstances(Uint64.valueOf(1))
                                .build());
                            sclpqiList.add(new org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121
                                    .service._interface.point.SupportedCepLayerProtocolQualifierInstancesBuilder()
                                .setLayerProtocolQualifier(PHOTONICLAYERQUALIFIEROTS.VALUE)
                                .setNumberOfCepInstances(Uint64.valueOf(1))
                                .build());
                            break;
                        default:
                            break;
                    }
                    break;
                default:
                    LOG.error("Layer Protocol Name is unknown");
                    break;
            }
        }
        return sclpqiList.stream().distinct().toList();
    }

    public Map<NodeKey, org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.topology.Node>
            getTapiNodes() {
        return tapiNodes;
    }

    public Map<LinkKey, Link> getTapiLinks() {
        return tapiLinks;
    }

    public AdministrativeState transformAsToTapiAdminState(String adminState) {
        return adminState == null ? null
            : adminState.equals(AdminStates.InService.getName())
                    || adminState.equals(AdministrativeState.UNLOCKED.getName())
                ? AdministrativeState.UNLOCKED : AdministrativeState.LOCKED;
    }

    public OperationalState transformOsToTapiOperationalState(String operState) {
        return operState == null ? null
            : operState.equals("inService") || operState.equals(OperationalState.ENABLED.getName())
                ? OperationalState.ENABLED : OperationalState.DISABLED;
    }

    public Map<ServiceInterfacePointKey, ServiceInterfacePoint> getTapiSips() {
        return tapiSips;
    }

    public void setTapiSips(Map<ServiceInterfacePointKey, ServiceInterfacePoint> tapiSip) {
        this.tapiSips.putAll(tapiSip);
    }

}
