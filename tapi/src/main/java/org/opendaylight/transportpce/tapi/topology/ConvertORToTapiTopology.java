/*
 * Copyright © 2023 Orange, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.tapi.topology;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.opendaylight.transportpce.tapi.TapiStringConstants;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev230526.TerminationPoint1;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.state.types.rev191129.State;
import org.opendaylight.yang.gen.v1.http.org.openroadm.equipment.states.types.rev191129.AdminStates;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.types.rev230526.xpdr.odu.switching.pools.OduSwitchingPools;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.types.rev230526.xpdr.odu.switching.pools.OduSwitchingPoolsBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.types.rev230526.xpdr.odu.switching.pools.odu.switching.pools.NonBlockingList;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.types.rev230526.xpdr.odu.switching.pools.odu.switching.pools.NonBlockingListBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.types.rev230526.xpdr.odu.switching.pools.odu.switching.pools.NonBlockingListKey;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.types.rev230526.OpenroadmNodeType;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.types.rev230526.OpenroadmTpType;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.types.rev230526.xpdr.tp.supported.interfaces.SupportedInterfaceCapability;
import org.opendaylight.yang.gen.v1.http.org.openroadm.otn.network.topology.rev230526.Node1;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.networks.network.Node;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.TpId;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.networks.network.node.TerminationPoint;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.AdministrativeState;
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
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.tapi.context.ServiceInterfacePoint;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.tapi.context.ServiceInterfacePointBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.tapi.context.ServiceInterfacePointKey;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.digital.otn.rev221121.ODUTYPEODU0;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.digital.otn.rev221121.ODUTYPEODU2;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.digital.otn.rev221121.ODUTYPEODU2E;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.digital.otn.rev221121.ODUTYPEODU4;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.dsr.rev221121.DIGITALSIGNALTYPE100GigE;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.dsr.rev221121.DIGITALSIGNALTYPE10GigELAN;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.dsr.rev221121.DIGITALSIGNALTYPEGigE;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.photonic.media.rev221121.PHOTONICLAYERQUALIFIEROTS;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.photonic.media.rev221121.PHOTONICLAYERQUALIFIEROTSiMC;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.FORWARDINGRULEMAYFORWARDACROSSGROUP;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.RuleType;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.node.NodeRuleGroup;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.node.NodeRuleGroupBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.node.NodeRuleGroupKey;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.node.OwnedNodeEdgePoint;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.node.OwnedNodeEdgePointBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.node.OwnedNodeEdgePointKey;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.node.RiskParameterPac;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.node.RiskParameterPacBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.node.edge.point.MappedServiceInterfacePoint;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.node.edge.point.MappedServiceInterfacePointBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.node.edge.point.MappedServiceInterfacePointKey;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.node.edge.point.SupportedCepLayerProtocolQualifierInstances;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.node.edge.point.SupportedCepLayerProtocolQualifierInstancesBuilder;
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
        if (ietfNode.augmentation(org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev230526.Node1.class)
                == null) {
            return;
        }
        this.ietfNodeType = ietfNode.augmentation(
            org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev230526.Node1.class).getNodeType();
        this.ietfNodeAdminState = ietfNode.augmentation(
                org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev230526.Node1.class)
            .getAdministrativeState();
        this.ietfNodeOperState = ietfNode.augmentation(
                org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev230526.Node1.class)
            .getOperationalState();
        this.oorNetworkPortList = ietfNode.augmentation(
                org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.Node1.class)
            .getTerminationPoint().values().stream()
            .filter(tp -> tp.augmentation(TerminationPoint1.class).getTpType().getIntValue()
                == OpenroadmTpType.XPONDERNETWORK.getIntValue()
                && networkPorts.contains(tp.getTpId().getValue()))
            .sorted((tp1, tp2) -> tp1.getTpId().getValue().compareTo(tp2.getTpId().getValue()))
            .collect(Collectors.toList());
        if (!OpenroadmNodeType.TPDR.equals(this.ietfNodeType)) {
            this.oorOduSwitchingPool = ietfNode.augmentation(Node1.class).getSwitchingPools().getOduSwitchingPools()
                .values().stream().findFirst().orElseThrow();
            this.oorClientPortList = ietfNode.augmentation(
                org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.Node1.class)
                .getTerminationPoint().values().stream()
                .filter(tp -> tp.augmentation(TerminationPoint1.class).getTpType().getIntValue()
                    == OpenroadmTpType.XPONDERCLIENT.getIntValue())
                .sorted((tp1, tp2) -> tp1.getTpId().getValue().compareTo(tp2.getTpId().getValue()))
                .collect(Collectors.toList());
        } else {
            this.oorOduSwitchingPool = createOduSwitchingPoolForTp100G();
            List<TpId> tpList = this.oorOduSwitchingPool.getNonBlockingList().values().stream()
                .flatMap(nbl -> nbl.getTpList().stream())
                .collect(Collectors.toList());
            this.oorClientPortList = ietfNode.augmentation(
                org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.Node1.class)
                .getTerminationPoint().values().stream()
                .filter(tp -> tp.augmentation(TerminationPoint1.class).getTpType().getIntValue()
                    == OpenroadmTpType.XPONDERCLIENT.getIntValue() && tpList.contains(tp.getTpId()))
                .sorted((tp1, tp2) -> tp1.getTpId().getValue().compareTo(tp2.getTpId().getValue()))
                .collect(Collectors.toList());
            this.oorClientPortList.forEach(tp -> LOG.info("tp = {}", tp.getTpId()));
        }

        // node creation [DSR/ODU] ([DSR/ODU] and OTSI merged in R 2.4.X)
        LOG.info("creation of a DSR/ODU node for {}", this.ietfNodeId);
        Uuid nodeUuid = new Uuid(UUID.nameUUIDFromBytes((String.join("+", this.ietfNodeId,
            TapiStringConstants.XPDR)).getBytes(Charset.forName("UTF-8"))).toString());
        this.uuidMap.put(String.join("+", this.ietfNodeId, TapiStringConstants.XPDR), nodeUuid);
        Name nameDsr = new NameBuilder().setValueName("dsr/odu node name")
            .setValue(String.join("+", this.ietfNodeId, TapiStringConstants.XPDR)).build();
        Name namePhot = new NameBuilder().setValueName("otsi node name")
            .setValue(String.join("+", this.ietfNodeId, TapiStringConstants.XPDR)).build();
        Name nameNodeType = new NameBuilder().setValueName("Node Type")
            .setValue(this.ietfNodeType.getName()).build();
        Set<LayerProtocolName> dsrLayerProtocols = Set.of(LayerProtocolName.DSR, LayerProtocolName.ODU,
            LayerProtocolName.DIGITALOTN,LayerProtocolName.PHOTONICMEDIA);
        org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.topology
            .Node dsrNode = createTapiNode(Map.of(nameDsr.key(), nameDsr, namePhot.key(), namePhot,nameNodeType.key(),
                nameNodeType), dsrLayerProtocols);
        LOG.debug("XPDR Node {} should have {} NEPs and SIPs", this.ietfNodeId,
            this.oorClientPortList.size() + this.oorNetworkPortList.size());
        LOG.info("XPDR Node {} has {} NEPs and {} SIPs", this.ietfNodeId,
            dsrNode.getOwnedNodeEdgePoint().values().size(), dsrNode.getOwnedNodeEdgePoint().values().stream()
                .filter(nep -> nep.getMappedServiceInterfacePoint() != null).count());
        tapiNodes.put(dsrNode.key(), dsrNode);
    }

    public Map<NodeRuleGroupKey, NodeRuleGroup> createNodeRuleGroupForRdmNode(String topoType, Uuid nodeUuid,
            String orNodeId, Collection<OwnedNodeEdgePoint> onepl) {
        Map<org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.node.rule.group.NodeEdgePointKey,
            org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.node.rule.group.NodeEdgePoint>
            nepMap = new HashMap<>();
        for (OwnedNodeEdgePoint onep : onepl) {
            org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.node.rule.group.NodeEdgePoint
                nep = new org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.node.rule.group
                    .NodeEdgePointBuilder()
                .setTopologyUuid(tapiTopoUuid)
                .setNodeUuid(nodeUuid)
                .setNodeEdgePointUuid(onep.key().getUuid())
                .build();
            nepMap.put(nep.key(), nep);
        }
        String rdmName =
            topoType.equals("Abstracted")
                ? "rdm infra node rule group"
                : orNodeId + " node rule group";
        Map<NodeRuleGroupKey, NodeRuleGroup> nodeRuleGroupMap = new HashMap<>();
        Set<RuleType> ruleTypes = new HashSet<>();
        ruleTypes.add(RuleType.FORWARDING);
        Map<RuleKey, Rule> ruleList = new HashMap<>();
        Rule rule = new RuleBuilder()
            .setLocalId("forward")
            .setForwardingRule(FORWARDINGRULEMAYFORWARDACROSSGROUP.VALUE)
            .setRuleType(ruleTypes)
            .build();
        ruleList.put(rule.key(), rule);
        NodeRuleGroup nodeRuleGroup = new NodeRuleGroupBuilder()
            .setUuid(new Uuid(UUID.nameUUIDFromBytes((rdmName)
                .getBytes(Charset.forName("UTF-8"))).toString()))
            .setRule(ruleList)
            .setNodeEdgePoint(nepMap)
            .build();
        nodeRuleGroupMap.put(nodeRuleGroup.key(), nodeRuleGroup);
        return nodeRuleGroupMap;
    }

    public Map<MappedServiceInterfacePointKey, MappedServiceInterfacePoint> createMSIP(int nb,
            LayerProtocolName layerProtocol, String tpId, String nodeid,
            Collection<SupportedInterfaceCapability> supportedInterfaceCapability,
            OperationalState operState, AdministrativeState adminState) {
        // add them to SIP context
        Map<MappedServiceInterfacePointKey, MappedServiceInterfacePoint> msipl = new HashMap<>();
        for (int i = 0; i < nb; i++) {
            String sipName =
                nb == 1
                    ? String.join("+", "SIP", nodeid, tpId)
                    : String.join("+", "SIP", nodeid, tpId,"Nber", String.valueOf(i));
            LOG.info("SIP = {}", sipName);
            Uuid sipUuid = new Uuid(UUID.nameUUIDFromBytes(sipName.getBytes(Charset.forName("UTF-8"))).toString());
            MappedServiceInterfacePoint msip = new MappedServiceInterfacePointBuilder()
                .setServiceInterfacePointUuid(sipUuid).build();
            ServiceInterfacePoint sip = createSIP(sipUuid, layerProtocol, tpId, nodeid, supportedInterfaceCapability,
                operState, adminState);
            this.tapiSips.put(sip.key(), sip);
            msipl.put(msip.key(), msip);
            LOG.debug("SIP created {}", sip.getUuid());
            LOG.debug("This SIP corresponds to SIP+nodeId {} + TpId {}", nodeid, tpId);
        }
        return msipl;
    }

    public List<SupportedCepLayerProtocolQualifierInstances> createSupportedCepLayerProtocolQualifier(
        Collection<SupportedInterfaceCapability> sicList, LayerProtocolName lpn) {
        List<SupportedCepLayerProtocolQualifierInstances> sclpqiList = new ArrayList<>();
        if (sicList == null) {
            sclpqiList.add(new SupportedCepLayerProtocolQualifierInstancesBuilder()
                .setLayerProtocolQualifier(PHOTONICLAYERQUALIFIEROTS.VALUE)
                .setNumberOfCepInstances(Uint64.valueOf(1))
                .build());
            return sclpqiList;
        }
        LOG.debug("SIC list = {}", sicList);
        for (SupportedInterfaceCapability sic : sicList) {
            String ifCapType = sic.getIfCapType().toString().split("\\{")[0];
            switch (lpn.getName()) {
                case "ETH":
                case "DSR":
                    switch (ifCapType) {
                        // TODO: it may be needed to add more cases clauses if the interface capabilities of a
                        //  port are extended in the config file
                        case "If1GEODU0":
                            sclpqiList.add(new SupportedCepLayerProtocolQualifierInstancesBuilder()
                                .setLayerProtocolQualifier(ODUTYPEODU0.VALUE)
                                .setNumberOfCepInstances(Uint64.valueOf(0))
                                .build());
                            sclpqiList.add(new SupportedCepLayerProtocolQualifierInstancesBuilder()
                                .setLayerProtocolQualifier(DIGITALSIGNALTYPEGigE.VALUE)
                                .setNumberOfCepInstances(Uint64.valueOf(0))
                                .build());
                            break;
                        case "If10GEODU2e":
                            sclpqiList.add(new SupportedCepLayerProtocolQualifierInstancesBuilder()
                                .setLayerProtocolQualifier(ODUTYPEODU2E.VALUE)
                                .setNumberOfCepInstances(Uint64.valueOf(0))
                                .build());
                            sclpqiList.add(new SupportedCepLayerProtocolQualifierInstancesBuilder()
                                .setLayerProtocolQualifier(DIGITALSIGNALTYPE10GigELAN.VALUE)
                                .setNumberOfCepInstances(Uint64.valueOf(0))
                                .build());
                            break;
                        case "If10GEODU2":
                            sclpqiList.add(new SupportedCepLayerProtocolQualifierInstancesBuilder()
                                .setLayerProtocolQualifier(ODUTYPEODU2.VALUE)
                                .setNumberOfCepInstances(Uint64.valueOf(0))
                                .build());
                            sclpqiList.add(new SupportedCepLayerProtocolQualifierInstancesBuilder()
                                .setLayerProtocolQualifier(DIGITALSIGNALTYPE10GigELAN.VALUE)
                                .setNumberOfCepInstances(Uint64.valueOf(0))
                                .build());
                            break;
                        case "If10GE":
                            sclpqiList.add(new SupportedCepLayerProtocolQualifierInstancesBuilder()
                                .setLayerProtocolQualifier(DIGITALSIGNALTYPE10GigELAN.VALUE)
                                .setNumberOfCepInstances(Uint64.valueOf(0))
                                .build());
                            break;
                        case "If100GEODU4":
                            sclpqiList.add(new SupportedCepLayerProtocolQualifierInstancesBuilder()
                                .setLayerProtocolQualifier(DIGITALSIGNALTYPE100GigE.VALUE)
                                .setNumberOfCepInstances(Uint64.valueOf(0))
                                .build());
                            sclpqiList.add(new SupportedCepLayerProtocolQualifierInstancesBuilder()
                                .setLayerProtocolQualifier(ODUTYPEODU4.VALUE)
                                .setNumberOfCepInstances(Uint64.valueOf(0))
                                .build());
                            break;
                        case "If100GE":
                            sclpqiList.add(new SupportedCepLayerProtocolQualifierInstancesBuilder()
                                .setLayerProtocolQualifier(DIGITALSIGNALTYPE100GigE.VALUE)
                                .setNumberOfCepInstances(Uint64.valueOf(0))
                                .build());
                            break;
                        case "IfOCHOTU4ODU4":
                        case "IfOCH":
                            sclpqiList.add(new SupportedCepLayerProtocolQualifierInstancesBuilder()
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
                        // TODO: it may be needed to add more cases clauses if the interface capabilities of a
                        //  port are extended in the config file
                        case "If1GEODU0":
                            sclpqiList.add(new SupportedCepLayerProtocolQualifierInstancesBuilder()
                                .setLayerProtocolQualifier(ODUTYPEODU0.VALUE)
                                .setNumberOfCepInstances(Uint64.valueOf(0))
                                .build());
                            break;
                        case "If10GEODU2e":
                            sclpqiList.add(new SupportedCepLayerProtocolQualifierInstancesBuilder()
                                .setLayerProtocolQualifier(ODUTYPEODU2E.VALUE)
                                .setNumberOfCepInstances(Uint64.valueOf(0))
                                .build());
                            break;
                        case "If10GEODU2":
                        case "If10GE":
                            sclpqiList.add(new SupportedCepLayerProtocolQualifierInstancesBuilder()
                                .setLayerProtocolQualifier(ODUTYPEODU2.VALUE)
                                .setNumberOfCepInstances(Uint64.valueOf(0))
                                .build());
                            break;
                        case "If100GEODU4":
                        case "If100GE":
                        case "IfOCHOTU4ODU4":
                        case "IfOCH":
                            sclpqiList.add(new SupportedCepLayerProtocolQualifierInstancesBuilder()
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
                    if (ifCapType.equals("IfOCHOTU4ODU4") || ifCapType.equals("IfOCH")) {
                        sclpqiList.add(new SupportedCepLayerProtocolQualifierInstancesBuilder()
                            .setLayerProtocolQualifier(PHOTONICLAYERQUALIFIEROTSiMC.VALUE)
                            .setNumberOfCepInstances(Uint64.valueOf(0))
                            .build());
                        sclpqiList.add(new SupportedCepLayerProtocolQualifierInstancesBuilder()
                            .setLayerProtocolQualifier(PHOTONICLAYERQUALIFIEROTS.VALUE)
                            .setNumberOfCepInstances(Uint64.valueOf(0))
                            .build());
                    }
                    break;
                default:
                    LOG.error("Layer Protocol Name is unknown {}", lpn.getName());
                    break;
            }
        }
        return sclpqiList.stream().distinct().toList();
    }

    private OduSwitchingPools createOduSwitchingPoolForTp100G() {
        Map<NonBlockingListKey, NonBlockingList> nblMap = new HashMap<>();
        int count = 1;
        for (TerminationPoint tp : this.oorNetworkPortList) {
            TpId tpid1 = tp.getTpId();
            TpId tpid2 = tp.augmentation(
                    org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev230526.TerminationPoint1.class)
                .getAssociatedConnectionMapTp().iterator().next();
            Set<TpId> tpList = new HashSet<>();
            tpList.add(tpid1);
            tpList.add(tpid2);
            NonBlockingList nbl = new NonBlockingListBuilder()
                .setNblNumber(Uint16.valueOf(count))
                .setTpList(tpList)
                .build();
            nblMap.put(nbl.key(), nbl);
            count++;
        }
        return new OduSwitchingPoolsBuilder()
            .setNonBlockingList(nblMap)
            .setSwitchingPoolNumber(Uint16.valueOf(1))
            .build();
    }

    private org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.topology.Node
            createTapiNode(Map<NameKey, Name> nodeNames, Set<LayerProtocolName> layerProtocols) {
        Uuid nodeUuid = null;
        Map<OwnedNodeEdgePointKey, OwnedNodeEdgePoint> onepl = new HashMap<>();
        Map<NodeRuleGroupKey, NodeRuleGroup> nodeRuleGroupList = new HashMap<>();
        Map<RuleKey, Rule> ruleList = new HashMap<>();
        Set<RuleType> ruleTypes = new HashSet<>();
        ruleTypes.add(RuleType.FORWARDING);
        Rule rule = new RuleBuilder()
            .setLocalId("forward")
            .setForwardingRule(FORWARDINGRULEMAYFORWARDACROSSGROUP.VALUE)
            .setRuleType(ruleTypes)
            .build();
        ruleList.put(rule.key(), rule);
        if (layerProtocols.contains(LayerProtocolName.DSR)
                || layerProtocols.contains(LayerProtocolName.PHOTONICMEDIA)) {
            nodeUuid = getNodeUuid4Dsr(onepl, nodeRuleGroupList, ruleList);
        } else {
            LOG.error("Undefined LayerProtocolName for {} node {}", nodeNames.get(nodeNames.keySet().iterator().next())
                .getValueName(), nodeNames.get(nodeNames.keySet().iterator().next()).getValue());
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
        RiskParameterPac riskParamPac = new RiskParameterPacBuilder()
            .setRiskCharacteristic(Map.of(riskCharacteristic.key(), riskCharacteristic))
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
            .setRiskParameterPac(riskParamPac)
            .setErrorCharacteristic("error")
            .setLossCharacteristic("loss")
            .setRepeatDeliveryCharacteristic("repeat delivery")
            .setDeliveryOrderCharacteristic("delivery order")
            .setUnavailableTimeCharacteristic("unavailable time")
            .setServerIntegrityProcessCharacteristic("server integrity process")
            .build();
    }

    private Uuid getNodeUuid4Dsr(Map<OwnedNodeEdgePointKey, OwnedNodeEdgePoint> onepl,
                                 Map<NodeRuleGroupKey, NodeRuleGroup> nodeRuleGroupList, Map<RuleKey, Rule> ruleList) {
        Uuid nodeUuid;
        nodeUuid = this.uuidMap.get(String.join("+", this.ietfNodeId, TapiStringConstants.XPDR));
        // client NEP DSR creation on DSR/ODU node
        for (int i = 0; i < oorClientPortList.size(); i++) {
            Uuid nepUuid = new Uuid(UUID.nameUUIDFromBytes(
                (String.join("+", this.ietfNodeId, TapiStringConstants.DSR,
                    oorClientPortList.get(i).getTpId().getValue())).getBytes(Charset.forName("UTF-8"))).toString());
            LOG.info("NEP = {} has Uuid {} ", String.join("+", this.ietfNodeId, TapiStringConstants.DSR,
                oorClientPortList.get(i).getTpId().getValue()), nepUuid);
            this.uuidMap.put(String.join("+", this.ietfNodeId, TapiStringConstants.DSR,
                    oorClientPortList.get(i).getTpId().getValue()), nepUuid);
            NameBuilder nameBldr = new NameBuilder().setValue(
                String.join("+", this.ietfNodeId, TapiStringConstants.DSR,
                    oorClientPortList.get(i).getTpId().getValue()));
            Name name;
            if (OpenroadmNodeType.TPDR.equals(this.ietfNodeType)) {
                name = nameBldr.setValueName("100G-tpdr").build();
            } else {
                name = nameBldr.setValueName("NodeEdgePoint_C").build();
            }

            OwnedNodeEdgePoint onep = createNep(oorClientPortList.get(i), Map.of(name.key(), name),
                LayerProtocolName.DSR, LayerProtocolName.DSR, true, String.join("+", this.ietfNodeId,
                    TapiStringConstants.DSR));
            onepl.put(onep.key(), onep);
        }
        // CLIENT NEP E_ODU creation on DSR/ODU node
        for (int i = 0; i < oorClientPortList.size(); i++) {
            Uuid nepUuid1 = new Uuid(UUID.nameUUIDFromBytes(
                (String.join("+", this.ietfNodeId, TapiStringConstants.E_ODU,
                    oorClientPortList.get(i).getTpId().getValue())).getBytes(Charset.forName("UTF-8"))).toString());
            LOG.info("NEP = {} has Uuid {} ", String.join("+", this.ietfNodeId, TapiStringConstants.E_ODU,
                oorClientPortList.get(i).getTpId().getValue()), nepUuid1);
            this.uuidMap.put(String.join("+", this.ietfNodeId, TapiStringConstants.E_ODU,
                oorClientPortList.get(i).getTpId().getValue()), nepUuid1);
            Name onedName = new NameBuilder()
                .setValueName("eNodeEdgePoint_N")
                .setValue(String.join("+", this.ietfNodeId, TapiStringConstants.E_ODU,
                    oorClientPortList.get(i).getTpId().getValue()))
                .build();

            OwnedNodeEdgePoint onep = createNep(oorClientPortList.get(i), Map.of(onedName.key(), onedName),
                LayerProtocolName.ODU, LayerProtocolName.DSR, false, String.join("+", this.ietfNodeId,
                    TapiStringConstants.E_ODU));
            onepl.put(onep.key(), onep);
        }
        // NETWORK NEPs I_ODU creation on DSR/ODU node
        for (int i = 0; i < oorNetworkPortList.size(); i++) {
            Uuid nepUuid1 = new Uuid(UUID.nameUUIDFromBytes(
                (String.join("+", this.ietfNodeId, TapiStringConstants.I_ODU,
                    oorNetworkPortList.get(i).getTpId().getValue())).getBytes(Charset.forName("UTF-8"))).toString());
            LOG.info("NEP = {} has Uuid {} ", String.join("+", this.ietfNodeId, TapiStringConstants.I_ODU,
                oorNetworkPortList.get(i).getTpId().getValue()), nepUuid1);
            this.uuidMap.put(String.join("+", this.ietfNodeId, TapiStringConstants.I_ODU,
                oorNetworkPortList.get(i).getTpId().getValue()), nepUuid1);
            Name onedName = new NameBuilder()
                .setValueName("iNodeEdgePoint_N")
                .setValue(String.join("+", this.ietfNodeId, TapiStringConstants.I_ODU,
                    oorNetworkPortList.get(i).getTpId().getValue()))
                .build();

            OwnedNodeEdgePoint onep = createNep(oorNetworkPortList.get(i), Map.of(onedName.key(), onedName),
                LayerProtocolName.ODU, LayerProtocolName.DSR, true, String.join("+", this.ietfNodeId,
                    TapiStringConstants.I_ODU));
            onepl.put(onep.key(), onep);
        }
        // NETWORK NEP OTS network on DSR/ODU node
        for (int i = 0; i < oorNetworkPortList.size(); i++) {
            Uuid nepUuid2 = new Uuid(UUID.nameUUIDFromBytes(
                (String.join("+", this.ietfNodeId, TapiStringConstants.PHTNC_MEDIA_OTS,
                    oorNetworkPortList.get(i).getTpId().getValue())).getBytes(Charset.forName("UTF-8")))
                .toString());
            LOG.info("NEP = {} has Uuid {} ", String.join("+", this.ietfNodeId, TapiStringConstants.PHTNC_MEDIA_OTS,
                oorNetworkPortList.get(i).getTpId().getValue()), nepUuid2);
            this.uuidMap.put(String.join("+", this.ietfNodeId, TapiStringConstants.PHTNC_MEDIA_OTS,
                oorNetworkPortList.get(i).getTpId().getValue()), nepUuid2);
            Name onedName = new NameBuilder()
                .setValueName("eNodeEdgePoint")
                .setValue(String.join("+", this.ietfNodeId, TapiStringConstants.PHTNC_MEDIA_OTS,
                    oorNetworkPortList.get(i).getTpId().getValue()))
                .build();

            OwnedNodeEdgePoint onep = createNep(oorNetworkPortList.get(i), Map.of(onedName.key(), onedName),
                LayerProtocolName.PHOTONICMEDIA, LayerProtocolName.PHOTONICMEDIA, true,
                String.join("+", this.ietfNodeId, TapiStringConstants.PHTNC_MEDIA_OTS));
            onepl.put(onep.key(), onep);
        }
        // NETWORK NEP OTSI_MC network nep creation
        //TODO: add test to see if wavelength is provionned and condition this creation to this!
        for (int i = 0; i < oorNetworkPortList.size(); i++) {
            Uuid nepUuid3 = new Uuid(UUID.nameUUIDFromBytes(
                (String.join("+", this.ietfNodeId, TapiStringConstants.OTSI_MC,
                    oorNetworkPortList.get(i).getTpId().getValue())).getBytes(Charset.forName("UTF-8")))
                .toString());
            LOG.info("NEP = {} has Uuid {} ", String.join("+", this.ietfNodeId, TapiStringConstants.OTSI_MC,
                oorNetworkPortList.get(i).getTpId().getValue()), nepUuid3);
            this.uuidMap.put(String.join("+", this.ietfNodeId, TapiStringConstants.OTSI_MC,
                oorNetworkPortList.get(i).getTpId().getValue()), nepUuid3);
            Name onedName = new NameBuilder()
                .setValueName("PhotMedNodeEdgePoint")
                .setValue(String.join("+", this.ietfNodeId, TapiStringConstants.OTSI_MC,
                    oorNetworkPortList.get(i).getTpId().getValue()))
                .build();

            OwnedNodeEdgePoint onep = createNep(oorNetworkPortList.get(i), Map.of(onedName.key(), onedName),
                LayerProtocolName.PHOTONICMEDIA, LayerProtocolName.PHOTONICMEDIA, true,
                String.join("+", this.ietfNodeId, TapiStringConstants.OTSI_MC));
            onepl.put(onep.key(), onep);
        }
        // create NodeRuleGroup
        int count = 1;
        LOG.debug("ODU switching pool = {}", this.oorOduSwitchingPool.nonnullNonBlockingList().values());
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
                LOG.debug("TP={}", tp.getValue());
                LOG.debug("UuidKey={}", String.join("+", this.ietfNodeId,
                    TapiStringConstants.E_ODU, tp.getValue()));
                if (this.uuidMap.containsKey(String.join("+", this.ietfNodeId, TapiStringConstants.DSR,
                    tp.getValue())) || this.uuidMap.containsKey(String.join(
                        "+", this.ietfNodeId, TapiStringConstants.I_ODU, tp.getValue()))) {
                    String qual = tp.getValue().contains("CLIENT") ? TapiStringConstants.DSR
                        : TapiStringConstants.I_ODU;
                    org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.node.rule.group.NodeEdgePoint
                            nep = new org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121
                            .node.rule.group.NodeEdgePointBuilder()
                        .setTopologyUuid(tapiTopoUuid)
                        .setNodeUuid(this.uuidMap.get(String.join("+", this.ietfNodeId,
                            TapiStringConstants.XPDR)))
                        .setNodeEdgePointUuid(this.uuidMap.get(String.join("+", this.ietfNodeId,
                            qual, tp.getValue())))
                        .build();
                    nepList.put(nep.key(), nep);
                }
                if (this.uuidMap.containsKey(String.join("+", this.ietfNodeId,
                        TapiStringConstants.E_ODU, tp.getValue()))) {
                    org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.node.rule.group.NodeEdgePoint
                            nep1 = new org.opendaylight.yang.gen.v1.urn
                            .onf.otcc.yang.tapi.topology.rev221121.node.rule.group.NodeEdgePointBuilder()
                        .setTopologyUuid(tapiTopoUuid)
                        .setNodeUuid(this.uuidMap.get(String.join("+", this.ietfNodeId,
                            TapiStringConstants.XPDR)))
                        .setNodeEdgePointUuid(this.uuidMap.get(String.join(
                            "+", this.ietfNodeId, TapiStringConstants.E_ODU, tp.getValue())))
                        .build();
                    oduNepList.put(nep1.key(), nep1);
                }
                if (this.uuidMap.containsKey(String.join("+", this.ietfNodeId,
                        TapiStringConstants.I_ODU, tp.getValue()))) {
                    org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.node.rule.group.NodeEdgePoint
                            nep2 = new org.opendaylight.yang.gen.v1.urn
                            .onf.otcc.yang.tapi.topology.rev221121.node.rule.group.NodeEdgePointBuilder()
                        .setTopologyUuid(tapiTopoUuid)
                        .setNodeUuid(this.uuidMap.get(String.join("+", this.ietfNodeId,
                            TapiStringConstants.XPDR)))
                        .setNodeEdgePointUuid(this.uuidMap.get(String.join(
                            "+", this.ietfNodeId, TapiStringConstants.I_ODU, tp.getValue())))
                        .build();
                    oduNepList.put(nep2.key(), nep2);
                }
            }
            LOG.debug("NEPLIST (DSR/I_ODU) of [dsr node rule group] is {}", nepList.toString());
            LOG.debug("NEPLIST (E_ODU/I_ODU) of [odu node rule group] is {}", nepList.toString());
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
            NodeRuleGroup nodeRuleGroup1 = new NodeRuleGroupBuilder()
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
        return nodeUuid;
    }

    private OwnedNodeEdgePoint createNep(TerminationPoint oorTp, Map<NameKey, Name> nepNames,
            LayerProtocolName nepProtocol, LayerProtocolName nodeProtocol, boolean withSip, String keyword) {
        String key = String.join("+", keyword, oorTp.getTpId().getValue());
        AdministrativeState adminState = (oorTp.augmentation(TerminationPoint1.class).getAdministrativeState() != null)
            ? transformAsToTapiAdminState(oorTp.augmentation(TerminationPoint1.class).getAdministrativeState()
                .getName())
            : null;
        OperationalState operState = (oorTp.augmentation(TerminationPoint1.class).getOperationalState() != null)
            ? transformOsToTapiOperationalState(oorTp.augmentation(TerminationPoint1.class).getOperationalState()
                .getName())
            : null;
        org.opendaylight.yang.gen.v1.http.org.openroadm.otn.network.topology.rev230526.TerminationPoint1 tp1 =
            oorTp.augmentation(org.opendaylight.yang.gen.v1.http
                .org.openroadm.otn.network.topology.rev230526.TerminationPoint1.class);
        if (tp1.getTpSupportedInterfaces() == null) {
            LOG.warn("Tp supported interface doesnt exist on TP {}", oorTp.getTpId().getValue());
            return null;
        }
//        Collection<SupportedInterfaceCapability> sicList = tp1.getTpSupportedInterfaces()
        Collection<SupportedInterfaceCapability> sicColl = tp1.getTpSupportedInterfaces()
            .getSupportedInterfaceCapability().values();
        OwnedNodeEdgePointBuilder onepBldr = new OwnedNodeEdgePointBuilder()
            .setUuid(this.uuidMap.get(key))
            .setLayerProtocolName(nepProtocol)
            .setName(nepNames);
        onepBldr.setSupportedCepLayerProtocolQualifierInstances(createSupportedCepLayerProtocolQualifier(sicColl,
                nepProtocol))
            .setDirection(Direction.BIDIRECTIONAL)
            .setLinkPortRole(PortRole.SYMMETRIC)
            .setAdministrativeState(adminState)
            .setOperationalState(operState)
            .setLifecycleState(LifecycleState.INSTALLED);
        if (withSip) {
            onepBldr.setMappedServiceInterfacePoint(
                createMSIP(1, nepProtocol, oorTp.getTpId().getValue(), keyword, sicColl, operState, adminState));
        }
        return onepBldr.build();
    }

    private ServiceInterfacePoint createSIP(Uuid sipUuid, LayerProtocolName layerProtocol, String tpId,
        String nodeid, Collection<SupportedInterfaceCapability> supportedInterfaceCapability,
        OperationalState operState, AdministrativeState adminState) {
    // TODO: what value should be set in total capacity and available capacity??
    // LOG.info("SIP name = {}", String.join("+", nodeid, tp.getTpId().getValue()));
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

    private List<org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121
            .service._interface.point.SupportedCepLayerProtocolQualifierInstances>
            createSipSupportedLayerProtocolQualifier(
            Collection<SupportedInterfaceCapability> supportedInterfaceCapability,
            LayerProtocolName lpn) {
        List<org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121
            .service._interface.point.SupportedCepLayerProtocolQualifierInstances> sclpqiList = new ArrayList<>();
        if (supportedInterfaceCapability == null) {
            sclpqiList.add(new org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121
                .service._interface.point.SupportedCepLayerProtocolQualifierInstancesBuilder()
                .setLayerProtocolQualifier(PHOTONICLAYERQUALIFIEROTS.VALUE)
                .setNumberOfCepInstances(Uint64.valueOf(1))
                .build());
            return sclpqiList;
        }
        for (SupportedInterfaceCapability sic : supportedInterfaceCapability) {
            String ifCapType = sic.getIfCapType().toString().split("\\{")[0];
            switch (lpn.getName()) {
                case "ETH":
                case "DSR":
                    switch (ifCapType) {
                        // TODO: it may be needed to add more cases clauses if the interface capabilities of a
                        //  port are extended in the config file
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
                        // TODO: it may be needed to add more cases clauses if the interface capabilities of a
                        //  port are extended in the config file
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
                    if (ifCapType.equals("IfOCHOTU4ODU4") || ifCapType.equals("IfOCH")) {
                        sclpqiList.add(new org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121
                                .service._interface.point.SupportedCepLayerProtocolQualifierInstancesBuilder()
                            .setLayerProtocolQualifier(PHOTONICLAYERQUALIFIEROTSiMC.VALUE)
                            .setNumberOfCepInstances(Uint64.valueOf(0))
                            .build());
                        sclpqiList.add(new org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121
                            .service._interface.point.SupportedCepLayerProtocolQualifierInstancesBuilder()
                                .setLayerProtocolQualifier(PHOTONICLAYERQUALIFIEROTS.VALUE)
                            .setNumberOfCepInstances(Uint64.valueOf(0))
                            .build());
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
        if (adminState == null) {
            return null;
        }
        return adminState.equals(AdminStates.InService.getName())
            || adminState.equals(AdministrativeState.UNLOCKED.getName()) ? AdministrativeState.UNLOCKED
                : AdministrativeState.LOCKED;
    }

    public OperationalState transformOsToTapiOperationalState(String operState) {
        if (operState == null) {
            return null;
        }
        return operState.equals("inService") || operState.equals(OperationalState.ENABLED.getName())
            ? OperationalState.ENABLED : OperationalState.DISABLED;
    }

    public Map<ServiceInterfacePointKey, ServiceInterfacePoint> getTapiSips() {
        return tapiSips;
    }

    public void setTapiSips(Map<ServiceInterfacePointKey, ServiceInterfacePoint> tapiSip) {
        this.tapiSips.putAll(tapiSip);
    }

}
