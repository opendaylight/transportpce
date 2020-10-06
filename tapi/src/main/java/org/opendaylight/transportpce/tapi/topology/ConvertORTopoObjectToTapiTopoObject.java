/*
 * Copyright © 2019 Orange, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.tapi.topology;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev181130.TerminationPoint1;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.types.rev181130.xpdr.odu.switching.pools.OduSwitchingPools;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.types.rev181130.xpdr.odu.switching.pools.odu.switching.pools.NonBlockingList;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.types.rev181130.OpenroadmTpType;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.types.rev181130.xpdr.tp.supported.interfaces.SupportedInterfaceCapability;
import org.opendaylight.yang.gen.v1.http.org.openroadm.otn.network.topology.rev181130.Link1;
import org.opendaylight.yang.gen.v1.http.org.openroadm.otn.network.topology.rev181130.Node1;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.networks.network.Node;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.TpId;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.networks.network.node.TerminationPoint;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev181210.AdministrativeState;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev181210.CapacityUnit;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev181210.ForwardingDirection;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev181210.LAYERPROTOCOLQUALIFIER;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev181210.LayerProtocolName;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev181210.LifecycleState;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev181210.OperationalState;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev181210.PortDirection;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev181210.PortRole;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev181210.TerminationDirection;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev181210.TerminationState;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev181210.Uuid;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev181210.capacity.TotalSizeBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev181210.capacity.pac.AvailableCapacityBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev181210.global._class.Name;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev181210.global._class.NameBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev181210.global._class.NameKey;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.dsr.rev181210.DIGITALSIGNALTYPE100GigE;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.dsr.rev181210.DIGITALSIGNALTYPE10GigELAN;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.odu.rev181210.ODUTYPEODU2E;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.odu.rev181210.ODUTYPEODU4;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.photonic.media.rev181210.PHOTONICLAYERQUALIFIEROMS;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.photonic.media.rev181210.PHOTONICLAYERQUALIFIEROTSi;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.ForwardingRule;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.RuleType;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.link.NodeEdgePoint;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.link.NodeEdgePointBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.link.NodeEdgePointKey;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.node.NodeRuleGroup;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.node.NodeRuleGroupBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.node.NodeRuleGroupKey;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.node.OwnedNodeEdgePoint;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.node.OwnedNodeEdgePointBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.node.OwnedNodeEdgePointKey;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.node.edge.point.MappedServiceInterfacePoint;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.node.edge.point.MappedServiceInterfacePointBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.node.edge.point.MappedServiceInterfacePointKey;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.node.rule.group.Rule;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.node.rule.group.RuleBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.node.rule.group.RuleKey;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.topology.Link;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.topology.LinkBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.topology.LinkKey;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.topology.NodeBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.topology.NodeKey;
import org.opendaylight.yangtools.yang.common.Uint64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConvertORTopoObjectToTapiTopoObject {

    private static final String DSR_PLUS = "DSR+";
    private static final String PLUS_DSR = "+DSR";
    private static final String OT_SI = "+OTSi";
    private static final String E_OT_SI = "eOTSi+";
    private static final String I_OT_SI = "iOTSi+";
    private static final Logger LOG = LoggerFactory.getLogger(ConvertORTopoObjectToTapiTopoObject.class);
    private String ietfNodeId;
    private List<TerminationPoint> oorClientPortList;
    private List<TerminationPoint> oorNetworkPortList;
    private OduSwitchingPools oorOduSwitchingPool;
    private Uuid tapiTopoUuid;
    private Map<NodeKey, org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.topology.Node>
        tapiNodes;
    private Map<LinkKey, Link> tapiLinks;
    private Map<String, Uuid> uuidMap;

    public ConvertORTopoObjectToTapiTopoObject(Node ietfNode, Link1 otnLink, Uuid tapiTopoUuid) {
        this.ietfNodeId = ietfNode.getNodeId().getValue();
        this.oorClientPortList = ietfNode.augmentation(
                org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.Node1.class)
            .getTerminationPoint().values().stream()
            .filter(tp -> tp.augmentation(TerminationPoint1.class).getTpType().getIntValue()
            == OpenroadmTpType.XPONDERCLIENT.getIntValue())
            .sorted((tp1, tp2) -> tp1.getTpId().getValue().compareTo(tp2.getTpId().getValue()))
            .collect(Collectors.toList());
        this.oorNetworkPortList = ietfNode.augmentation(
                org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.Node1.class)
            .getTerminationPoint().values().stream()
            .filter(tp -> tp.augmentation(TerminationPoint1.class).getTpType().getIntValue()
            == OpenroadmTpType.XPONDERNETWORK.getIntValue())
            .sorted((tp1, tp2) -> tp1.getTpId().getValue().compareTo(tp2.getTpId().getValue()))
            .collect(Collectors.toList());
        this.oorOduSwitchingPool = ietfNode.augmentation(Node1.class).getSwitchingPools().getOduSwitchingPools()
            .values().stream().findFirst().get();
        this.tapiTopoUuid = tapiTopoUuid;
        this.tapiNodes = new HashMap<>();
        this.tapiLinks = new HashMap<>();
        this.uuidMap = new HashMap<>();
    }

    public void convertNode() {
        // node creation [DSR/ODU]
        LOG.info("creation of a DSR/ODU node");
        Uuid nodeUuid = new Uuid(UUID.nameUUIDFromBytes((this.ietfNodeId + PLUS_DSR).getBytes(Charset.forName("UTF-8")))
            .toString());
        this.uuidMap.put(this.ietfNodeId + PLUS_DSR, nodeUuid);
        Name nameDsr = new NameBuilder().setValueName("dsr/odu node name").setValue(this.ietfNodeId).build();
        List<LayerProtocolName> dsrLayerProtocols = Arrays.asList(LayerProtocolName.DSR, LayerProtocolName.ODU);
        org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.topology
            .Node dsrNode = createTapiNode(Map.of(nameDsr.key(), nameDsr), dsrLayerProtocols);
        tapiNodes.put(dsrNode.key(), dsrNode);

        // node creation [otsi]
        LOG.info("creation of an OTSi node");
        nodeUuid = new Uuid(UUID.nameUUIDFromBytes((this.ietfNodeId + OT_SI).getBytes(Charset.forName("UTF-8")))
            .toString());
        this.uuidMap.put(this.ietfNodeId + OT_SI, nodeUuid);
        Name nameOtsi =  new NameBuilder().setValueName("otsi node name").setValue(this.ietfNodeId).build();
        List<LayerProtocolName> otsiLayerProtocols = Arrays.asList(LayerProtocolName.PHOTONICMEDIA);
        org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.topology
            .Node otsiNode = createTapiNode(Map.of(nameOtsi.key(), nameOtsi), otsiLayerProtocols);
        tapiNodes.put(otsiNode.key(), otsiNode);

        // transitional link cration between network nep of DSR/ODU node and iNep of otsi node
        LOG.info("creation of transitional links between DSR/ODU and OTSi nodes");
        createTapiTransitionalLinks();
    }

    private org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.topology
        .Node createTapiNode(Map<NameKey, Name> nodeNames, List<LayerProtocolName> layerProtocols) {
        Uuid nodeUuid = null;
        Map<OwnedNodeEdgePointKey, OwnedNodeEdgePoint> onepl = new HashMap<>();
        Map<NodeRuleGroupKey, NodeRuleGroup> nodeRuleGroupList = new HashMap<>();
        Map<RuleKey, Rule> ruleList = new HashMap<>();
        Rule rule = new RuleBuilder()
            .setLocalId("forward")
            .setForwardingRule(ForwardingRule.MAYFORWARDACROSSGROUP)
            .setRuleType(RuleType.FORWARDING)
            .build();
        ruleList.put(rule.key(), rule);
        if (layerProtocols.contains(LayerProtocolName.DSR)) {
            nodeUuid = getNodeUuid4Dsr(onepl, nodeRuleGroupList, ruleList);
        } else if (layerProtocols.contains(LayerProtocolName.PHOTONICMEDIA)) {
            nodeUuid = getNodeUuid4Phonic(onepl, nodeRuleGroupList, ruleList);
        } else {
            LOG.error("Undefined LayerProtocolName for {} node {}", nodeNames.get(nodeNames.keySet().iterator().next())
                .getValueName(), nodeNames.get(nodeNames.keySet().iterator().next()).getValue());
        }

        // create tapi node

        return new NodeBuilder()
                .setUuid(nodeUuid)
                .setName(nodeNames)
                .setLayerProtocolName(layerProtocols)
                .setAdministrativeState(AdministrativeState.UNLOCKED)
                .setOperationalState(OperationalState.ENABLED)
                .setLifecycleState(LifecycleState.INSTALLED)
                .setOwnedNodeEdgePoint(onepl)
                .setNodeRuleGroup(nodeRuleGroupList)
                .build();
    }

    private Uuid getNodeUuid4Phonic(Map<OwnedNodeEdgePointKey, OwnedNodeEdgePoint> onepl,
        Map<NodeRuleGroupKey, NodeRuleGroup> nodeRuleGroupList, Map<RuleKey, Rule> ruleList) {
        Uuid nodeUuid;
        nodeUuid = this.uuidMap.get(this.ietfNodeId + OT_SI);
        // iNep creation on otsi node
        for (int i = 0; i < oorNetworkPortList.size(); i++) {
            Uuid nepUuid1 = new Uuid(UUID.nameUUIDFromBytes(
                    (I_OT_SI + oorNetworkPortList.get(i).getTpId().getValue()).getBytes(Charset.forName("UTF-8")))
                .toString());
            this.uuidMap.put(I_OT_SI + oorNetworkPortList.get(i).getTpId().getValue(), nepUuid1);
            Name onedName = new NameBuilder()
                .setValueName(new StringBuilder("iNodeEdgePoint_").append(i + 1).toString())
                .setValue(oorNetworkPortList.get(i).getTpId().getValue())
                .build();

            OwnedNodeEdgePoint onep = createNep(oorNetworkPortList.get(i), Map.of(onedName.key(), onedName),
                LayerProtocolName.PHOTONICMEDIA, LayerProtocolName.PHOTONICMEDIA, true, I_OT_SI);
            onepl.put(onep.key(), onep);
        }
        // eNep creation on otsi node
        for (int i = 0; i < oorNetworkPortList.size(); i++) {
            Uuid nepUuid2 = new Uuid(UUID.nameUUIDFromBytes(
                    (E_OT_SI + oorNetworkPortList.get(i).getTpId().getValue()).getBytes(Charset.forName("UTF-8")))
                .toString());
            this.uuidMap.put(E_OT_SI + oorNetworkPortList.get(i).getTpId().getValue(), nepUuid2);
            Name onedName = new NameBuilder()
                .setValueName(new StringBuilder("eNodeEdgePoint_").append(i + 1).toString())
                .setValue(oorNetworkPortList.get(i).getTpId().getValue())
                .build();

            OwnedNodeEdgePoint onep = createNep(oorNetworkPortList.get(i), Map.of(onedName.key(), onedName),
                LayerProtocolName.PHOTONICMEDIA, LayerProtocolName.PHOTONICMEDIA, true, E_OT_SI);
            onepl.put(onep.key(), onep);
        }
        // create NodeRuleGroup
        int count = 1;
        for (TerminationPoint tp : this.oorNetworkPortList) {
            Map<org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.node.rule.group.NodeEdgePointKey,
                org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.node.rule.group.NodeEdgePoint>
                nepList = new HashMap<>();
            org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.node.rule.group
                .NodeEdgePoint inep = new org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210
                .node.rule.group.NodeEdgePointBuilder()
                .setTopologyUuid(tapiTopoUuid)
                .setNodeUuid(this.uuidMap.get(this.ietfNodeId + OT_SI))
                .setNodeEdgePointUuid(this.uuidMap.get(I_OT_SI + tp.getTpId().getValue()))
                .build();
            org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.node.rule.group
                .NodeEdgePoint enep = new org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210
                .node.rule.group.NodeEdgePointBuilder()
                .setTopologyUuid(tapiTopoUuid)
                .setNodeUuid(this.uuidMap.get(this.ietfNodeId + OT_SI))
                .setNodeEdgePointUuid(this.uuidMap.get(E_OT_SI + tp.getTpId().getValue()))
                .build();
            nepList.put(inep.key(), inep);
            nepList.put(enep.key(), enep);
            NodeRuleGroup nodeRuleGroup = new NodeRuleGroupBuilder()
                .setUuid(new Uuid(
                        UUID.nameUUIDFromBytes(("otsi node rule group " + count).getBytes(Charset.forName("UTF-8")))
                    .toString()))
                .setRule(ruleList)
                .setNodeEdgePoint(nepList)
                .build();
            nodeRuleGroupList.put(nodeRuleGroup.key(), nodeRuleGroup);
            count++;
        }
        return nodeUuid;
    }

    private Uuid getNodeUuid4Dsr(Map<OwnedNodeEdgePointKey, OwnedNodeEdgePoint> onepl,
        Map<NodeRuleGroupKey, NodeRuleGroup> nodeRuleGroupList, Map<RuleKey, Rule> ruleList) {
        Uuid nodeUuid;
        nodeUuid = this.uuidMap.get(this.ietfNodeId + PLUS_DSR);
        // client nep creation on DSR/ODU node
        for (int i = 0; i < oorClientPortList.size(); i++) {
            Uuid nepUuid = new Uuid(UUID.nameUUIDFromBytes((DSR_PLUS + oorClientPortList.get(i).getTpId().getValue())
                .getBytes(Charset.forName("UTF-8"))).toString());
            this.uuidMap.put(DSR_PLUS + oorClientPortList.get(i).getTpId().getValue(), nepUuid);
            Name name = new NameBuilder()
                .setValueName(new StringBuilder("NodeEdgePoint_C").append(i + 1).toString())
                .setValue(oorClientPortList.get(i).getTpId().getValue())
                .build();

            OwnedNodeEdgePoint onep = createNep(oorClientPortList.get(i), Map.of(name.key(), name),
                LayerProtocolName.ETH, LayerProtocolName.DSR, true, DSR_PLUS);
            onepl.put(onep.key(), onep);
        }
        // network nep creation on DSR/ODU node
        for (int i = 0; i < oorNetworkPortList.size(); i++) {
            Uuid nepUuid = new Uuid(UUID.nameUUIDFromBytes((DSR_PLUS + oorNetworkPortList.get(i).getTpId().getValue())
                .getBytes(Charset.forName("UTF-8"))).toString());
            this.uuidMap.put(DSR_PLUS + oorNetworkPortList.get(i).getTpId().getValue(), nepUuid);
            Name onedName = new NameBuilder()
                .setValueName(new StringBuilder("NodeEdgePoint_N").append(i + 1).toString())
                .setValue(oorNetworkPortList.get(i).getTpId().getValue())
                .build();

            OwnedNodeEdgePoint onep = createNep(oorNetworkPortList.get(i), Map.of(onedName.key(), onedName),
                LayerProtocolName.ODU, LayerProtocolName.DSR, true, DSR_PLUS);
            onepl.put(onep.key(), onep);
        }
        // create NodeRuleGroup
        int count = 1;
        for (NonBlockingList nbl : this.oorOduSwitchingPool.getNonBlockingList().values()) {
            Map<org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.node.rule.group.NodeEdgePointKey,
                org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.node.rule.group.NodeEdgePoint>
                nepList = new HashMap<>();
            for (TpId tp : nbl.getTpList()) {
                org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.node.rule.group
                    .NodeEdgePoint nep = new org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210
                    .node.rule.group.NodeEdgePointBuilder()
                    .setTopologyUuid(tapiTopoUuid)
                    .setNodeUuid(this.uuidMap.get(this.ietfNodeId + PLUS_DSR))
                    .setNodeEdgePointUuid(this.uuidMap.get(DSR_PLUS + tp.getValue()))
                    .build();
                nepList.put(nep.key(), nep);
            }
            NodeRuleGroup nodeRuleGroup = new NodeRuleGroupBuilder()
                .setUuid(new Uuid(
                        UUID.nameUUIDFromBytes(("dsr node rule group " + count).getBytes(Charset.forName("UTF-8")))
                    .toString()))
                .setRule(ruleList)
                .setNodeEdgePoint(nepList)
                .build();
            nodeRuleGroupList.put(nodeRuleGroup.key(), nodeRuleGroup);
            count++;
        }
        return nodeUuid;
    }

    private OwnedNodeEdgePoint createNep(TerminationPoint oorTp, Map<NameKey, Name> nepNames,
        LayerProtocolName nepProtocol, LayerProtocolName nodeProtocol, boolean withSip, String keyword) {
        String key = keyword + oorTp.getTpId().getValue();
        OwnedNodeEdgePointBuilder onepBldr = new OwnedNodeEdgePointBuilder()
            .setUuid(this.uuidMap.get(key))
            .setLayerProtocolName(nepProtocol)
            .setName(nepNames);
        if (withSip) {
            onepBldr.setMappedServiceInterfacePoint(createSIP(1));
        }
        onepBldr.setSupportedCepLayerProtocolQualifier(createSupportedCepLayerProtocolQualifier(oorTp, nodeProtocol));
        onepBldr.setLinkPortDirection(PortDirection.BIDIRECTIONAL).setLinkPortRole(PortRole.SYMMETRIC)
                .setAdministrativeState(AdministrativeState.UNLOCKED).setOperationalState(OperationalState.ENABLED)
                .setLifecycleState(LifecycleState.INSTALLED).setTerminationDirection(TerminationDirection.BIDIRECTIONAL)
                .setTerminationState(TerminationState.TERMINATEDBIDIRECTIONAL);
        return onepBldr.build();
    }

    private Map<MappedServiceInterfacePointKey, MappedServiceInterfacePoint> createSIP(int nb) {
        Map<MappedServiceInterfacePointKey, MappedServiceInterfacePoint> msipl = new HashMap<>();
        for (int i = 0; i < nb; i++) {
            MappedServiceInterfacePoint msip = new MappedServiceInterfacePointBuilder()
                .setServiceInterfacePointUuid(new Uuid(UUID.randomUUID().toString())).build();
            msipl.put(msip.key(), msip);
        }
        return msipl;
    }

    private List<Class<? extends LAYERPROTOCOLQUALIFIER>> createSupportedCepLayerProtocolQualifier(TerminationPoint tp,
        LayerProtocolName lpn) {
        List<Class<? extends LAYERPROTOCOLQUALIFIER>> sclpqList = new ArrayList<>();
        List<SupportedInterfaceCapability> sicList = new ArrayList<>(
            tp.augmentation(org.opendaylight.yang.gen.v1.http.org.openroadm
            .otn.network.topology.rev181130.TerminationPoint1.class).getTpSupportedInterfaces()
            .getSupportedInterfaceCapability().values());
        for (SupportedInterfaceCapability sic : sicList) {
            switch (lpn.getName()) {
                case "DSR":
                    if (sic.getIfCapType().getSimpleName().equals("If10GEODU2e")) {
                        sclpqList.add(DIGITALSIGNALTYPE10GigELAN.class);
                        sclpqList.add(ODUTYPEODU2E.class);
                    } else if (sic.getIfCapType().getSimpleName().equals("IfOCHOTU4ODU4")) {
                        sclpqList.add(ODUTYPEODU4.class);
                    } else if (sic.getIfCapType().getSimpleName().equals("If100GEODU4")) {
                        sclpqList.add(DIGITALSIGNALTYPE100GigE.class);
                        sclpqList.add(ODUTYPEODU4.class);
                    }
                    break;
                case "PHOTONIC_MEDIA":
                    if (sic.getIfCapType().getSimpleName().equals("IfOCHOTU4ODU4")) {
                        sclpqList.add(PHOTONICLAYERQUALIFIEROTSi.class);
                        sclpqList.add(PHOTONICLAYERQUALIFIEROMS.class);
                    }
                    break;
                default:
                    LOG.error("Layer Protocol Name is unknown");
                    break;
            }
        }
        return sclpqList;
    }

    private void createTapiTransitionalLinks() {
        for (TerminationPoint tp : this.oorNetworkPortList) {
            Map<NodeEdgePointKey, NodeEdgePoint> nepList = new HashMap<>();
            String sourceKey = DSR_PLUS + tp.getTpId().getValue();
            Uuid sourceUuidTp = this.uuidMap.get(sourceKey);
            String destKey = I_OT_SI + tp.getTpId().getValue();
            Uuid destUuidTp = this.uuidMap.get(destKey);
            NodeEdgePoint sourceNep = new NodeEdgePointBuilder()
                .setTopologyUuid(this.tapiTopoUuid)
                .setNodeUuid(this.uuidMap.get(this.ietfNodeId + PLUS_DSR))
                .setNodeEdgePointUuid(sourceUuidTp)
                .build();
            nepList.put(sourceNep.key(), sourceNep);
            NodeEdgePoint destNep = new NodeEdgePointBuilder()
                .setTopologyUuid(this.tapiTopoUuid)
                .setNodeUuid(this.uuidMap.get(this.ietfNodeId + OT_SI))
                .setNodeEdgePointUuid(destUuidTp)
                .build();
            nepList.put(destNep.key(), destNep);
            Name linkName = new NameBuilder().setValueName("transitional link name")
                .setValue(this.ietfNodeId + "-" + sourceKey + "--" + destKey)
                .build();
            Link transiLink = new LinkBuilder()
                .setUuid(new Uuid(
                        UUID.nameUUIDFromBytes((this.ietfNodeId + "-" + sourceKey + "--" + destKey)
                            .getBytes(Charset.forName("UTF-8")))
                    .toString()))
                .setName(Map.of(linkName.key(), linkName))
                .setTransitionedLayerProtocolName(Arrays.asList(LayerProtocolName.ODU.getName(),
                    LayerProtocolName.PHOTONICMEDIA.getName()))
                .setNodeEdgePoint(nepList)
                .setDirection(ForwardingDirection.BIDIRECTIONAL)
                .setAvailableCapacity(new AvailableCapacityBuilder().setTotalSize(
                    new TotalSizeBuilder().setUnit(CapacityUnit.GBPS).setValue(Uint64.valueOf(100)).build()).build())
                .build();
            this.tapiLinks.put(transiLink.key(), transiLink);
        }
    }

    public Map<NodeKey, org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.topology.Node>
        getTapiNodes() {
        return tapiNodes;
    }

    public Map<LinkKey, Link> getTapiLinks() {
        return tapiLinks;
    }
}
