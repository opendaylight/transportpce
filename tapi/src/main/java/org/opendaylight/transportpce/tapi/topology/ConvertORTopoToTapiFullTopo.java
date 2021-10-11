/*
 * Copyright © 2021 Nokia.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.tapi.topology;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev200529.TerminationPoint1;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.state.types.rev191129.State;
import org.opendaylight.yang.gen.v1.http.org.openroadm.equipment.states.types.rev191129.AdminStates;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.types.rev200327.xpdr.odu.switching.pools.OduSwitchingPools;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.types.rev200327.xpdr.odu.switching.pools.OduSwitchingPoolsBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.types.rev200327.xpdr.odu.switching.pools.odu.switching.pools.NonBlockingList;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.types.rev200327.xpdr.odu.switching.pools.odu.switching.pools.NonBlockingListBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.types.rev200327.xpdr.odu.switching.pools.odu.switching.pools.NonBlockingListKey;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.types.rev200529.OpenroadmNodeType;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.types.rev200529.OpenroadmTpType;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.types.rev200529.xpdr.tp.supported.interfaces.SupportedInterfaceCapability;
import org.opendaylight.yang.gen.v1.http.org.openroadm.otn.network.topology.rev200529.Link1;
import org.opendaylight.yang.gen.v1.http.org.openroadm.otn.network.topology.rev200529.Node1;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.networks.Network;
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
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev181210.capacity.pac.TotalPotentialCapacityBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev181210.global._class.Name;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev181210.global._class.NameBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev181210.global._class.NameKey;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev181210.tapi.context.ServiceInterfacePoint;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev181210.tapi.context.ServiceInterfacePointBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev181210.tapi.context.ServiceInterfacePointKey;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.dsr.rev181210.DIGITALSIGNALTYPE100GigE;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.dsr.rev181210.DIGITALSIGNALTYPE10GigELAN;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.dsr.rev181210.DIGITALSIGNALTYPEGigE;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.odu.rev181210.ODUTYPEODU0;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.odu.rev181210.ODUTYPEODU2;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.odu.rev181210.ODUTYPEODU2E;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.odu.rev181210.ODUTYPEODU4;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.photonic.media.rev181210.PHOTONICLAYERQUALIFIEROMS;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.photonic.media.rev181210.PHOTONICLAYERQUALIFIEROTSi;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.ForwardingRule;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.ProtectionType;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.RestorationPolicy;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.RuleType;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.link.NodeEdgePoint;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.link.NodeEdgePointBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.link.NodeEdgePointKey;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.link.ResilienceTypeBuilder;
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
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.risk.parameter.pac.RiskCharacteristic;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.risk.parameter.pac.RiskCharacteristicBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.topology.Link;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.topology.LinkBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.topology.LinkKey;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.topology.NodeBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.topology.NodeKey;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.transfer.cost.pac.CostCharacteristic;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.transfer.cost.pac.CostCharacteristicBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.transfer.timing.pac.LatencyCharacteristic;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.transfer.timing.pac.LatencyCharacteristicBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.validation.pac.ValidationMechanism;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.validation.pac.ValidationMechanismBuilder;
import org.opendaylight.yangtools.yang.common.Uint16;
import org.opendaylight.yangtools.yang.common.Uint64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class ConvertORTopoToTapiFullTopo {

    private static final String DSR = "DSR";
    private static final String I_ODU = "iODU";
    private static final String E_ODU = "eODU";
    private static final String OTSI = "OTSi";
    private static final String E_OTSI = "eOTSi";
    private static final String I_OTSI = "iOTSi";
    private static final String PHTNC_MEDIA = "PHOTONIC_MEDIA";
    private static final String MC = "MEDIA_CHANNEL";
    private static final String OTSI_MC = "OTSi_MEDIA_CHANNEL";
    private static final Logger LOG = LoggerFactory.getLogger(ConvertORTopoToTapiFullTopo.class);
    private String ietfNodeId;
    private OpenroadmNodeType ietfNodeType;
    private AdminStates ietfNodeAdminState;
    private State ietfNodeOperState;
    private List<TerminationPoint> oorClientPortList;
    private List<TerminationPoint> oorNetworkPortList;
    private OduSwitchingPools oorOduSwitchingPool;
    private Uuid tapiTopoUuid;
    private Map<NodeKey, org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.topology.Node>
        tapiNodes;
    private Map<LinkKey, Link> tapiLinks;
    private Map<ServiceInterfacePointKey, ServiceInterfacePoint> tapiSips;
    private Map<String, Uuid> uuidMap;


    public ConvertORTopoToTapiFullTopo(Uuid tapiTopoUuid) {
        this.tapiTopoUuid = tapiTopoUuid;
        this.tapiNodes = new HashMap<>();
        this.tapiLinks = new HashMap<>();
        this.uuidMap = new HashMap<>();
        this.tapiSips = new HashMap<>();
    }

    public void convertNode(Node ietfNode, List<String> networkPorts) {
        this.ietfNodeId = ietfNode.getNodeId().getValue();
        if (ietfNode.augmentation(org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev200529.Node1.class)
                == null) {
            return;
        }
        this.ietfNodeType = ietfNode.augmentation(
            org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev200529.Node1.class).getNodeType();
        this.ietfNodeAdminState = ietfNode.augmentation(
            org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev200529.Node1.class)
                .getAdministrativeState();
        this.ietfNodeOperState = ietfNode.augmentation(
            org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev200529.Node1.class)
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
                .values().stream().findFirst().get();
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

        // node creation [DSR/ODU]
        LOG.info("creation of a DSR/ODU node for {}", this.ietfNodeId);
        Uuid nodeUuid = new Uuid(UUID.nameUUIDFromBytes((String.join("+", this.ietfNodeId, DSR))
            .getBytes(Charset.forName("UTF-8"))).toString());
        this.uuidMap.put(String.join("+", this.ietfNodeId, DSR), nodeUuid);
        Name nameDsr = new NameBuilder().setValueName("dsr/odu node name").setValue(
            String.join("+", this.ietfNodeId, DSR)).build();
        List<LayerProtocolName> dsrLayerProtocols = Arrays.asList(LayerProtocolName.DSR, LayerProtocolName.ODU);
        org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.topology
            .Node dsrNode = createTapiNode(Map.of(nameDsr.key(), nameDsr), dsrLayerProtocols);
        LOG.info("DSR Node {} should have {} NEPs and {} SIPs", this.ietfNodeId,
            this.oorClientPortList.size() + 2 * this.oorNetworkPortList.size(),
            this.oorClientPortList.size() + this.oorNetworkPortList.size());
        LOG.info("DSR Node {} has {} NEPs and {} SIPs", this.ietfNodeId,
            dsrNode.getOwnedNodeEdgePoint().values().size(), dsrNode.getOwnedNodeEdgePoint().values().stream()
                .filter(nep -> nep.getMappedServiceInterfacePoint() != null).count());
        tapiNodes.put(dsrNode.key(), dsrNode);

        // node creation [otsi]
        LOG.info("creation of an OTSi node for {}", this.ietfNodeId);
        nodeUuid = new Uuid(UUID.nameUUIDFromBytes((String.join("+", this.ietfNodeId, OTSI))
            .getBytes(Charset.forName("UTF-8"))).toString());
        this.uuidMap.put(String.join("+", this.ietfNodeId, OTSI), nodeUuid);
        Name nameOtsi =  new NameBuilder().setValueName("otsi node name").setValue(
            String.join("+", this.ietfNodeId, OTSI)).build();
        List<LayerProtocolName> otsiLayerProtocols = Arrays.asList(LayerProtocolName.PHOTONICMEDIA);
        org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.topology
            .Node otsiNode = createTapiNode(Map.of(nameOtsi.key(), nameOtsi), otsiLayerProtocols);
        LOG.info("OTSi Node {} should have {} NEPs and {} SIPs", this.ietfNodeId, 3 * this.oorNetworkPortList.size(),
            this.oorNetworkPortList.size());
        LOG.info("OTSi Node {} has {} NEPs and {} SIPs", this.ietfNodeId,
            otsiNode.getOwnedNodeEdgePoint().values().size(), otsiNode.getOwnedNodeEdgePoint().values().stream()
                .filter(nep -> nep.getMappedServiceInterfacePoint() != null).count());
        tapiNodes.put(otsiNode.key(), otsiNode);

        // transitional link cration between network nep of DSR/ODU node and iNep of otsi node
        LOG.info("creation of transitional links between DSR/ODU and OTSi nodes");
        createTapiTransitionalLinks();
    }

    public void convertLinks(
            Map<org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang
                .ietf.network.topology.rev180226.networks.network.LinkKey,
            org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang
                .ietf.network.topology.rev180226.networks.network.Link> otnLinkMap) {
        List<org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.networks.network
            .Link> otnLinkList = new ArrayList<>(otnLinkMap.values());
        Collections.sort(otnLinkList, (l1, l2) -> l1.getLinkId().getValue()
            .compareTo(l2.getLinkId().getValue()));
        List<String> linksToNotConvert = new ArrayList<>();
        LOG.info("creation of {} otn links", otnLinkMap.size() / 2);
        for (org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.networks.network
            .Link otnlink : otnLinkList) {
            if (!linksToNotConvert.contains(otnlink.getLinkId().getValue())) {
                org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.networks
                    .network.Link oppositeLink = otnLinkMap.get(new org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns
                    .yang.ietf.network.topology.rev180226.networks.network.LinkKey(otnlink.augmentation(
                    org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev200529.Link1.class)
                    .getOppositeLink()));
                Link tapiLink = createTapiLink(otnlink, oppositeLink);
                linksToNotConvert.add(oppositeLink.getLinkId().getValue());
                tapiLinks.put(tapiLink.key(), tapiLink);
            }
        }
    }

    public void convertRdmToRdmLinks(List<org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang
            .ietf.network.topology.rev180226.networks.network.Link> rdmTordmLinkList) {
        List<String> linksToNotConvert = new ArrayList<>();
        LOG.info("creation of {} roadm to roadm links", rdmTordmLinkList.size() / 2);
        for (org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.networks.network
            .Link link : rdmTordmLinkList) {
            if (!linksToNotConvert.contains(link.getLinkId().getValue())) {
                Link tapiLink = createTapiOmsLink(link, rdmTordmLinkList.stream()
                    .filter(l -> l.getLinkId().equals(link.augmentation(org.opendaylight.yang.gen.v1.http
                        .org.openroadm.common.network.rev200529.Link1.class).getOppositeLink()))
                    .findAny().orElse(null));
                linksToNotConvert.add(link
                    .augmentation(org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev200529.Link1.class)
                    .getOppositeLink().getValue());
                tapiLinks.put(tapiLink.key(), tapiLink);
            }
        }
    }

    public void convertRoadmNode(Node roadm, Network openroadmTopo) {
        this.ietfNodeId = roadm.getNodeId().getValue();
        Map<OwnedNodeEdgePointKey, OwnedNodeEdgePoint> oneplist = new HashMap<>();
        // 1. Get degree and srg nodes to map TPs into NEPs
        if (openroadmTopo.getNode() == null) {
            LOG.warn("Openroadm-topology is null.");
            return;
        }
        int numNeps = 0;
        int numSips = 0;
        List<Node> nodeList = new ArrayList<Node>(openroadmTopo.getNode().values());
        for (Node node:nodeList) {
            if (node.getSupportingNode().values().stream().noneMatch(sp -> sp.getNodeRef().getValue()
                .equals(this.ietfNodeId))) {
                LOG.warn("Abstracted node {} is not part of {}",
                    node.getNodeId().getValue(), this.ietfNodeId);
                continue;
            }
            if (node.augmentation(Node1.class) == null
                && node.augmentation(org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang
                .ietf.network.topology.rev180226.Node1.class) == null) {
                LOG.warn("Abstracted node {} doesnt have type of node or is not disaggregated",
                    node.getNodeId().getValue());
                continue;
            }
            OpenroadmNodeType nodeType = node.augmentation(org.opendaylight.yang.gen.v1.http
                .org.openroadm.common.network.rev200529.Node1.class).getNodeType();
            org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.Node1 node1 =
                node.augmentation(org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang
                    .ietf.network.topology.rev180226.Node1.class);
            LOG.info("TPs of node: {}", node1.getTerminationPoint().values());
            switch (nodeType.getIntValue()) {
                case 11:
                    LOG.info("Degree node");
                    // Get only external TPs of the degree
                    List<TerminationPoint> degPortList = node1.getTerminationPoint().values().stream()
                        .filter(tp -> tp.augmentation(TerminationPoint1.class).getTpType().getIntValue()
                            == OpenroadmTpType.DEGREETXRXTTP.getIntValue()
                            || tp.augmentation(TerminationPoint1.class).getTpType().getIntValue()
                            == OpenroadmTpType.DEGREERXTTP.getIntValue()
                            || tp.augmentation(TerminationPoint1.class).getTpType().getIntValue()
                            == OpenroadmTpType.DEGREETXTTP.getIntValue())
                        .collect(Collectors.toList());
                    // Convert TP List in NEPs and put it in onepl
                    LOG.info("Degree port List: {}", degPortList);
                    // TODO: deg port could be sip. e.g. MDONS
                    oneplist.putAll(populateNepsForRdmNode(degPortList, false));
                    // oneplist.putAll(populateNepsForRdmNode(node.getNodeId().getValue(), degPortList, false));
                    numNeps += degPortList.size() * 3;
                    break;
                case 12:
                    LOG.info("SRG node");
                    // Get only external TPs of the srg
                    List<TerminationPoint> srgPortList = node1.getTerminationPoint().values().stream()
                        .filter(tp -> tp.augmentation(TerminationPoint1.class).getTpType().getIntValue()
                            == OpenroadmTpType.SRGTXRXPP.getIntValue()
                            || tp.augmentation(TerminationPoint1.class).getTpType().getIntValue()
                            == OpenroadmTpType.SRGRXPP.getIntValue()
                            || tp.augmentation(TerminationPoint1.class).getTpType().getIntValue()
                            == OpenroadmTpType.SRGTXPP.getIntValue())
                        .collect(Collectors.toList());
                    // Convert TP List in NEPs and put it in onepl
                    LOG.info("Srg port List: {}", srgPortList);
                    oneplist.putAll(populateNepsForRdmNode(srgPortList, true));
                    // oneplist.putAll(populateNepsForRdmNode(node.getNodeId().getValue(), srgPortList, true));
                    numNeps += srgPortList.size() * 3;
                    numSips += srgPortList.size();
                    break;
                default:
                    LOG.error("Node {} tyoe not supported", nodeType.getName());
            }
        }
        // create tapi Node
        // UUID
        LOG.info("Creation of PHOTONIC node for {}", roadm.getNodeId().getValue());
        Uuid nodeUuid = new Uuid(UUID.nameUUIDFromBytes((String.join("+", roadm.getNodeId().getValue(),
            PHTNC_MEDIA)).getBytes(Charset.forName("UTF-8"))).toString());
        // Names
        Name nodeNames =  new NameBuilder().setValueName("roadm node name")
            .setValue(String.join("+", roadm.getNodeId().getValue(), PHTNC_MEDIA)).build();
        // Protocol Layer
        List<LayerProtocolName> layerProtocols = Arrays.asList(LayerProtocolName.PHOTONICMEDIA);
        // Build tapi node
        org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.topology
            .Node roadmNode = createRoadmTapiNode(nodeUuid, nodeNames, layerProtocols, oneplist);
        // TODO add states corresponding to device config
        LOG.info("ROADM node {} should have {} NEPs and {} SIPs", roadm.getNodeId().getValue(), numNeps, numSips);
        LOG.info("ROADM node {} has {} NEPs and {} SIPs", roadm.getNodeId().getValue(),
            roadmNode.getOwnedNodeEdgePoint().values().size(),
            roadmNode.getOwnedNodeEdgePoint().values().stream()
                .filter(nep -> nep.getMappedServiceInterfacePoint() != null).count());

        tapiNodes.put(roadmNode.key(), roadmNode);
    }

    private OduSwitchingPools createOduSwitchingPoolForTp100G() {
        Map<NonBlockingListKey, NonBlockingList> nblMap = new HashMap<>();
        int count = 1;
        for (TerminationPoint tp : this.oorNetworkPortList) {
            TpId tpid1 = tp.getTpId();
            TpId tpid2 = new TpId(tp.augmentation(
                org.opendaylight.yang.gen.v1.http.transportpce.topology.rev210511.TerminationPoint1.class)
                .getAssociatedConnectionMapPort());
            List<TpId> tpList = new ArrayList<>();
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

    private org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.topology.Node
            createTapiNode(Map<NameKey, Name> nodeNames, List<LayerProtocolName> layerProtocols) {
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
            nodeUuid = getNodeUuid4Photonic(onepl, nodeRuleGroupList, ruleList);
        } else {
            LOG.error("Undefined LayerProtocolName for {} node {}", nodeNames.get(nodeNames.keySet().iterator().next())
                .getValueName(), nodeNames.get(nodeNames.keySet().iterator().next()).getValue());
        }
        // Empty random creation of mandatory fields for avoiding errors....
        CostCharacteristic costCharacteristic = new CostCharacteristicBuilder()
            .setCostAlgorithm("Restricted Shortest Path - RSP")
            .setCostName("HOP_COUNT")
            .setCostValue("12345678")
            .build();
        LatencyCharacteristic latencyCharacteristic = new LatencyCharacteristicBuilder()
            .setFixedLatencyCharacteristic("12345678")
            .setQueingLatencyCharacteristic("12345678")
            .setJitterCharacteristic("12345678")
            .setWanderCharacteristic("12345678")
            .setTrafficPropertyName("FIXED_LATENCY")
            .build();
        return new NodeBuilder()
            .setUuid(nodeUuid)
            .setName(nodeNames)
            .setLayerProtocolName(layerProtocols)
            .setAdministrativeState(setTapiAdminState(this.ietfNodeAdminState))
            .setOperationalState(setTapiOperationalState(this.ietfNodeOperState))
            .setLifecycleState(LifecycleState.INSTALLED)
            .setOwnedNodeEdgePoint(onepl)
            .setNodeRuleGroup(nodeRuleGroupList)
            .setCostCharacteristic(Map.of(costCharacteristic.key(), costCharacteristic))
            .setLatencyCharacteristic(Map.of(latencyCharacteristic.key(), latencyCharacteristic))
            .setErrorCharacteristic("error")
            .setLossCharacteristic("loss")
            .setRepeatDeliveryCharacteristic("repeat delivery")
            .setDeliveryOrderCharacteristic("delivery order")
            .setUnavailableTimeCharacteristic("unavailable time")
            .setServerIntegrityProcessCharacteristic("server integrity process")
            .build();
    }

    private AdministrativeState setTapiAdminState(AdminStates adminState) {
        if (adminState == null) {
            return null;
        }
        return adminState.equals(AdminStates.InService)
            ? AdministrativeState.UNLOCKED : AdministrativeState.LOCKED;
    }

    private AdministrativeState setTapiAdminState(AdminStates adminState1, AdminStates adminState2) {
        if (adminState1 == null || adminState2 == null) {
            return null;
        }
        LOG.info("Admin state 1 = {}, andmin state 2 = {}", adminState1.getName(), adminState2.getName());
        if (AdminStates.InService.equals(adminState1) && AdminStates.InService.equals(adminState2)) {
            return AdministrativeState.UNLOCKED;
        } else {
            return AdministrativeState.LOCKED;
        }
    }

    private OperationalState setTapiOperationalState(State operState) {
        if (operState == null) {
            return null;
        }
        return operState.getName().equals("inService") ? OperationalState.ENABLED : OperationalState.DISABLED;
    }

    private OperationalState setTapiOperationalState(State operState1, State operState2) {
        if (operState1 == null || operState2 == null) {
            return null;
        }
        LOG.info("Oper state 1 = {}, Oper state 2 = {}", operState1.getName(), operState2.getName());
        if (State.InService.equals(operState1) && State.InService.equals(operState2)) {
            return OperationalState.ENABLED;
        } else {
            return OperationalState.DISABLED;
        }
    }

    private org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.topology.Node
            createRoadmTapiNode(Uuid nodeUuid, Name nodeNames, List<LayerProtocolName> layerProtocols,
                        Map<OwnedNodeEdgePointKey, OwnedNodeEdgePoint> oneplist) {
        // Empty random creation of mandatory fields for avoiding errors....
        CostCharacteristic costCharacteristic = new CostCharacteristicBuilder()
            .setCostAlgorithm("Restricted Shortest Path - RSP")
            .setCostName("HOP_COUNT")
            .setCostValue("12345678")
            .build();
        LatencyCharacteristic latencyCharacteristic = new LatencyCharacteristicBuilder()
            .setFixedLatencyCharacteristic("12345678")
            .setQueingLatencyCharacteristic("12345678")
            .setJitterCharacteristic("12345678")
            .setWanderCharacteristic("12345678")
            .setTrafficPropertyName("FIXED_LATENCY")
            .build();
        return new NodeBuilder()
            .setUuid(nodeUuid)
            .setName(Map.of(nodeNames.key(), nodeNames))
            .setLayerProtocolName(layerProtocols)
            .setAdministrativeState(AdministrativeState.UNLOCKED)
            .setOperationalState(OperationalState.ENABLED)
            .setLifecycleState(LifecycleState.INSTALLED)
            .setOwnedNodeEdgePoint(oneplist)
            .setNodeRuleGroup(createNodeRuleGroupForRdmNode(nodeUuid, oneplist.values()))
            .setCostCharacteristic(Map.of(costCharacteristic.key(), costCharacteristic))
            .setLatencyCharacteristic(Map.of(latencyCharacteristic.key(), latencyCharacteristic))
            .setErrorCharacteristic("error")
            .setLossCharacteristic("loss")
            .setRepeatDeliveryCharacteristic("repeat delivery")
            .setDeliveryOrderCharacteristic("delivery order")
            .setUnavailableTimeCharacteristic("unavailable time")
            .setServerIntegrityProcessCharacteristic("server integrity process")
            .build();
    }

    private Uuid getNodeUuid4Photonic(Map<OwnedNodeEdgePointKey, OwnedNodeEdgePoint> onepl,
                                      Map<NodeRuleGroupKey, NodeRuleGroup> nodeRuleGroupList,
                                      Map<RuleKey, Rule> ruleList) {
        Uuid nodeUuid;
        nodeUuid = this.uuidMap.get(String.join("+", this.ietfNodeId, OTSI));
        // iNep creation on otsi node
        for (int i = 0; i < oorNetworkPortList.size(); i++) {
            LOG.info("NEP = {}", String.join("+", this.ietfNodeId, I_OTSI,
                oorNetworkPortList.get(i).getTpId().getValue()));
            Uuid nepUuid1 = new Uuid(UUID.nameUUIDFromBytes(
                (String.join("+", this.ietfNodeId, I_OTSI, oorNetworkPortList.get(i).getTpId().getValue()))
                    .getBytes(Charset.forName("UTF-8")))
                .toString());
            this.uuidMap.put(String.join("+", this.ietfNodeId, I_OTSI,
                oorNetworkPortList.get(i).getTpId().getValue()), nepUuid1);
            Name onedName = new NameBuilder()
                .setValueName("iNodeEdgePoint")
                .setValue(String.join("+", this.ietfNodeId, I_OTSI, oorNetworkPortList.get(i).getTpId().getValue()))
                .build();

            OwnedNodeEdgePoint onep = createNep(oorNetworkPortList.get(i), Map.of(onedName.key(), onedName),
                LayerProtocolName.PHOTONICMEDIA, LayerProtocolName.PHOTONICMEDIA, true,
                String.join("+", this.ietfNodeId, I_OTSI));
            onepl.put(onep.key(), onep);
        }
        // eNep creation on otsi node
        for (int i = 0; i < oorNetworkPortList.size(); i++) {
            LOG.info("NEP = {}", String.join("+", this.ietfNodeId, E_OTSI,
                oorNetworkPortList.get(i).getTpId().getValue()));
            Uuid nepUuid2 = new Uuid(UUID.nameUUIDFromBytes(
                (String.join("+", this.ietfNodeId, E_OTSI, oorNetworkPortList.get(i).getTpId().getValue()))
                    .getBytes(Charset.forName("UTF-8")))
                .toString());
            this.uuidMap.put(String.join("+", this.ietfNodeId, E_OTSI,
                oorNetworkPortList.get(i).getTpId().getValue()), nepUuid2);
            Name onedName = new NameBuilder()
                .setValueName("eNodeEdgePoint")
                .setValue(String.join("+", this.ietfNodeId, E_OTSI, oorNetworkPortList.get(i).getTpId().getValue()))
                .build();

            OwnedNodeEdgePoint onep = createNep(oorNetworkPortList.get(i), Map.of(onedName.key(), onedName),
                LayerProtocolName.PHOTONICMEDIA, LayerProtocolName.PHOTONICMEDIA, false,
                String.join("+", this.ietfNodeId, E_OTSI));
            onepl.put(onep.key(), onep);
        }
        // Photonic Media Nep creation on otsi node
        for (int i = 0; i < oorNetworkPortList.size(); i++) {
            LOG.info("NEP = {}", String.join("+", this.ietfNodeId, PHTNC_MEDIA,
                oorNetworkPortList.get(i).getTpId().getValue()));
            Uuid nepUuid3 = new Uuid(UUID.nameUUIDFromBytes(
                (String.join("+", this.ietfNodeId, PHTNC_MEDIA,
                    oorNetworkPortList.get(i).getTpId().getValue())).getBytes(Charset.forName("UTF-8")))
                .toString());
            this.uuidMap.put(String.join("+", this.ietfNodeId, PHTNC_MEDIA,
                oorNetworkPortList.get(i).getTpId().getValue()), nepUuid3);
            Name onedName = new NameBuilder()
                .setValueName("PhotMedNodeEdgePoint")
                .setValue(String.join("+", this.ietfNodeId, PHTNC_MEDIA,
                    oorNetworkPortList.get(i).getTpId().getValue()))
                .build();

            OwnedNodeEdgePoint onep = createNep(oorNetworkPortList.get(i), Map.of(onedName.key(), onedName),
                LayerProtocolName.PHOTONICMEDIA, LayerProtocolName.PHOTONICMEDIA, false,
                String.join("+", this.ietfNodeId, PHTNC_MEDIA));
            onepl.put(onep.key(), onep);
        }
        // create NodeRuleGroup
        int count = 1;
        LOG.info("OTSi switching pool = {}", this.oorOduSwitchingPool.nonnullNonBlockingList().values());
        for (TerminationPoint tp : this.oorNetworkPortList) {
            Map<org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.node.rule.group.NodeEdgePointKey,
                org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.node.rule.group.NodeEdgePoint>
                nepList = new HashMap<>();
            org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.node.rule.group
                .NodeEdgePoint inep = new org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210
                .node.rule.group.NodeEdgePointBuilder()
                .setTopologyUuid(tapiTopoUuid)
                .setNodeUuid(this.uuidMap.get(String.join("+", this.ietfNodeId, OTSI)))
                .setNodeEdgePointUuid(
                    this.uuidMap.get(String.join("+", this.ietfNodeId, I_OTSI, tp.getTpId().getValue())))
                .build();
            org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.node.rule.group
                .NodeEdgePoint enep = new org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210
                .node.rule.group.NodeEdgePointBuilder()
                .setTopologyUuid(tapiTopoUuid)
                .setNodeUuid(this.uuidMap.get(String.join("+", this.ietfNodeId, OTSI)))
                .setNodeEdgePointUuid(
                    this.uuidMap.get(String.join("+", this.ietfNodeId, E_OTSI, tp.getTpId().getValue())))
                .build();
            nepList.put(inep.key(), inep);
            nepList.put(enep.key(), enep);
            // Empty random creation of mandatory fields for avoiding errors....
            CostCharacteristic costCharacteristic = new CostCharacteristicBuilder()
                .setCostAlgorithm("Restricted Shortest Path - RSP")
                .setCostName("HOP_COUNT")
                .setCostValue("12345678")
                .build();
            LatencyCharacteristic latencyCharacteristic = new LatencyCharacteristicBuilder()
                .setFixedLatencyCharacteristic("12345678")
                .setQueingLatencyCharacteristic("12345678")
                .setJitterCharacteristic("12345678")
                .setWanderCharacteristic("12345678")
                .setTrafficPropertyName("FIXED_LATENCY")
                .build();
            RiskCharacteristic riskCharacteristic = new RiskCharacteristicBuilder()
                .setRiskCharacteristicName("risk characteristic")
                .setRiskIdentifierList(List.of("risk identifier1", "risk identifier2"))
                .build();
            NodeRuleGroup nodeRuleGroup = new NodeRuleGroupBuilder()
                .setUuid(new Uuid(
                    UUID.nameUUIDFromBytes(("otsi node rule group " + count).getBytes(Charset.forName("UTF-8")))
                        .toString()))
                .setRule(ruleList)
                .setNodeEdgePoint(nepList)
                .setRiskCharacteristic(Map.of(riskCharacteristic.key(), riskCharacteristic))
                .setCostCharacteristic(Map.of(costCharacteristic.key(), costCharacteristic))
                .setLatencyCharacteristic(Map.of(latencyCharacteristic.key(), latencyCharacteristic))
                .build();
            nodeRuleGroupList.put(nodeRuleGroup.key(), nodeRuleGroup);
            count++;
        }
        return nodeUuid;
    }

    private Uuid getNodeUuid4Dsr(Map<OwnedNodeEdgePointKey, OwnedNodeEdgePoint> onepl,
                                 Map<NodeRuleGroupKey, NodeRuleGroup> nodeRuleGroupList, Map<RuleKey, Rule> ruleList) {
        Uuid nodeUuid;
        nodeUuid = this.uuidMap.get(String.join("+", this.ietfNodeId, DSR));
        // client nep creation on DSR node
        for (int i = 0; i < oorClientPortList.size(); i++) {
            LOG.info("NEP = {}", String.join("+", this.ietfNodeId, DSR,
                oorClientPortList.get(i).getTpId().getValue()));
            Uuid nepUuid = new Uuid(UUID.nameUUIDFromBytes(
                (String.join("+", this.ietfNodeId, DSR, oorClientPortList.get(i).getTpId().getValue()))
                    .getBytes(Charset.forName("UTF-8"))).toString());
            this.uuidMap.put(String.join("+", this.ietfNodeId, DSR, oorClientPortList.get(i).getTpId().getValue()),
                nepUuid);
            NameBuilder nameBldr = new NameBuilder().setValue(
                String.join("+", this.ietfNodeId, DSR, oorClientPortList.get(i).getTpId().getValue()));
            Name name;
            if (OpenroadmNodeType.TPDR.equals(this.ietfNodeType)) {
                name = nameBldr.setValueName("100G-tpdr").build();
            } else {
                name = nameBldr.setValueName("NodeEdgePoint_C").build();
            }

            OwnedNodeEdgePoint onep = createNep(oorClientPortList.get(i), Map.of(name.key(), name),
                LayerProtocolName.DSR, LayerProtocolName.DSR, true, String.join("+", this.ietfNodeId, DSR));
            onepl.put(onep.key(), onep);
        }
        // network nep creation on I_ODU node
        for (int i = 0; i < oorNetworkPortList.size(); i++) {
            LOG.info("NEP = {}", String.join("+", this.ietfNodeId, I_ODU,
                oorNetworkPortList.get(i).getTpId().getValue()));
            Uuid nepUuid = new Uuid(UUID.nameUUIDFromBytes(
                (String.join("+", this.ietfNodeId, I_ODU, oorNetworkPortList.get(i).getTpId().getValue()))
                    .getBytes(Charset.forName("UTF-8"))).toString());
            this.uuidMap.put(String.join("+", this.ietfNodeId, I_ODU, oorNetworkPortList.get(i).getTpId().getValue()),
                nepUuid);
            Name onedName = new NameBuilder()
                .setValueName("iNodeEdgePoint_N")
                .setValue(String.join("+", this.ietfNodeId, I_ODU, oorNetworkPortList.get(i).getTpId().getValue()))
                .build();

            OwnedNodeEdgePoint onep = createNep(oorNetworkPortList.get(i), Map.of(onedName.key(), onedName),
                LayerProtocolName.ODU, LayerProtocolName.DSR, false, String.join("+", this.ietfNodeId, I_ODU));
            onepl.put(onep.key(), onep);
        }
        // network nep creation on E_ODU node
        for (int i = 0; i < oorNetworkPortList.size(); i++) {
            LOG.info("NEP = {}", String.join("+", this.ietfNodeId, E_ODU,
                oorNetworkPortList.get(i).getTpId().getValue()));
            Uuid nepUuid = new Uuid(UUID.nameUUIDFromBytes(
                (String.join("+", this.ietfNodeId, E_ODU, oorNetworkPortList.get(i).getTpId().getValue()))
                    .getBytes(Charset.forName("UTF-8"))).toString());
            this.uuidMap.put(String.join("+", this.ietfNodeId, E_ODU, oorNetworkPortList.get(i).getTpId().getValue()),
                nepUuid);
            Name onedName = new NameBuilder()
                .setValueName("eNodeEdgePoint_N")
                .setValue(String.join("+", this.ietfNodeId, E_ODU, oorNetworkPortList.get(i).getTpId().getValue()))
                .build();

            OwnedNodeEdgePoint onep = createNep(oorNetworkPortList.get(i), Map.of(onedName.key(), onedName),
                LayerProtocolName.ODU, LayerProtocolName.DSR, true, String.join("+", this.ietfNodeId, E_ODU));
            onepl.put(onep.key(), onep);
        }
        // create NodeRuleGroup
        int count = 1;
        LOG.info("ODU switching pool = {}", this.oorOduSwitchingPool.nonnullNonBlockingList().values());
        for (NonBlockingList nbl : this.oorOduSwitchingPool.nonnullNonBlockingList().values()) {
            Map<org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.node.rule.group.NodeEdgePointKey,
                org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.node.rule.group.NodeEdgePoint>
                nepList = new HashMap<>();
            LOG.info("UUidMap={}", this.uuidMap.keySet());
            LOG.info("TP list = {}", nbl.getTpList());
            for (TpId tp : nbl.getTpList()) {
                LOG.info("TP={}", tp.getValue());
                LOG.info("UuidKey={}", String.join("+", this.ietfNodeId, E_ODU, tp.getValue()));
                if (this.uuidMap.containsKey(String.join("+", this.ietfNodeId, E_ODU, tp.getValue()))
                    || this.uuidMap.containsKey(String.join("+", this.ietfNodeId, DSR, tp.getValue()))) {
                    org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.node.rule.group.NodeEdgePoint
                        nep = new org.opendaylight.yang.gen.v1.urn
                        .onf.otcc.yang.tapi.topology.rev181210.node.rule.group.NodeEdgePointBuilder()
                        .setTopologyUuid(tapiTopoUuid)
                        .setNodeUuid(this.uuidMap.get(String.join("+", this.ietfNodeId, DSR)))
                        .setNodeEdgePointUuid((tp.getValue().contains("CLIENT")) ? this.uuidMap.get(String.join(
                            "+", this.ietfNodeId, DSR, tp.getValue())) : this.uuidMap.get(String.join(
                            "+", this.ietfNodeId, E_ODU, tp.getValue())))
                        .build();
                    nepList.put(nep.key(), nep);
                }
            }
            // Empty random creation of mandatory fields for avoiding errors....
            CostCharacteristic costCharacteristic = new CostCharacteristicBuilder()
                .setCostAlgorithm("Restricted Shortest Path - RSP")
                .setCostName("HOP_COUNT")
                .setCostValue("12345678")
                .build();
            LatencyCharacteristic latencyCharacteristic = new LatencyCharacteristicBuilder()
                .setFixedLatencyCharacteristic("12345678")
                .setQueingLatencyCharacteristic("12345678")
                .setJitterCharacteristic("12345678")
                .setWanderCharacteristic("12345678")
                .setTrafficPropertyName("FIXED_LATENCY")
                .build();
            RiskCharacteristic riskCharacteristic = new RiskCharacteristicBuilder()
                .setRiskCharacteristicName("risk characteristic")
                .setRiskIdentifierList(List.of("risk identifier1", "risk identifier2"))
                .build();
            NodeRuleGroup nodeRuleGroup = new NodeRuleGroupBuilder()
                .setUuid(new Uuid(UUID.nameUUIDFromBytes(("dsr node rule group " + count)
                    .getBytes(Charset.forName("UTF-8"))).toString()))
                .setRule(ruleList)
                .setNodeEdgePoint(nepList)
                .setRiskCharacteristic(Map.of(riskCharacteristic.key(), riskCharacteristic))
                .setCostCharacteristic(Map.of(costCharacteristic.key(), costCharacteristic))
                .setLatencyCharacteristic(Map.of(latencyCharacteristic.key(), latencyCharacteristic))
                .build();
            nodeRuleGroupList.put(nodeRuleGroup.key(), nodeRuleGroup);
            count++;
        }
        return nodeUuid;
    }

    private OwnedNodeEdgePoint createNep(TerminationPoint oorTp, Map<NameKey, Name> nepNames,
                                         LayerProtocolName nepProtocol, LayerProtocolName nodeProtocol, boolean withSip,
                                         String keyword) {
        String key = String.join("+", keyword, oorTp.getTpId().getValue());
        OwnedNodeEdgePointBuilder onepBldr = new OwnedNodeEdgePointBuilder()
            .setUuid(this.uuidMap.get(key))
            .setLayerProtocolName(nepProtocol)
            .setName(nepNames);
        if (withSip) {
            onepBldr.setMappedServiceInterfacePoint(createMSIP(1, nepProtocol, oorTp, keyword));
        }
        onepBldr.setSupportedCepLayerProtocolQualifier(createSupportedLayerProtocolQualifier(oorTp, nodeProtocol))
            .setLinkPortDirection(PortDirection.BIDIRECTIONAL)
            .setLinkPortRole(PortRole.SYMMETRIC)
            .setAdministrativeState(setTapiAdminState(
                oorTp.augmentation(TerminationPoint1.class).getAdministrativeState()))
            .setOperationalState(setTapiOperationalState(
                oorTp.augmentation(TerminationPoint1.class).getOperationalState()))
            .setLifecycleState(LifecycleState.INSTALLED)
            .setTerminationDirection(TerminationDirection.BIDIRECTIONAL)
            .setTerminationState(TerminationState.TERMINATEDBIDIRECTIONAL);
        return onepBldr.build();
    }

    private Map<OwnedNodeEdgePointKey, OwnedNodeEdgePoint> populateNepsForRdmNode(List<TerminationPoint> tpList,
                                                                                  boolean withSip) {
        // create neps for MC and OTSiMC and Photonic Media
        Map<OwnedNodeEdgePointKey, OwnedNodeEdgePoint> onepMap = new HashMap<>();
        for (TerminationPoint tp:tpList) {
            // Admin and oper state common for all tps
            AdminStates admin = tp.augmentation(TerminationPoint1.class).getAdministrativeState();
            State oper = tp.augmentation(TerminationPoint1.class).getOperationalState();
            // PHOTONIC MEDIA nep
            LOG.info("PHOTO NEP = {}", String.join("+", this.ietfNodeId, PHTNC_MEDIA, tp.getTpId().getValue()));
            Name nepName = new NameBuilder()
                .setValueName(PHTNC_MEDIA + "NodeEdgePoint")
                .setValue(String.join("+", this.ietfNodeId, PHTNC_MEDIA, tp.getTpId().getValue()))
                .build();

            OwnedNodeEdgePoint onep = new OwnedNodeEdgePointBuilder()
                .setUuid(new Uuid(UUID.nameUUIDFromBytes((String.join("+", this.ietfNodeId, PHTNC_MEDIA,
                    tp.getTpId().getValue())).getBytes(Charset.forName("UTF-8"))).toString()))
                .setLayerProtocolName(LayerProtocolName.PHOTONICMEDIA)
                .setName(Map.of(nepName.key(), nepName))
                .setSupportedCepLayerProtocolQualifier(List.of(PHOTONICLAYERQUALIFIEROMS.class))
                .setLinkPortDirection(PortDirection.BIDIRECTIONAL)
                .setLinkPortRole(PortRole.SYMMETRIC)
                .setAdministrativeState(setTapiAdminState(admin))
                .setOperationalState(setTapiOperationalState(oper))
                .setLifecycleState(LifecycleState.INSTALLED)
                .setTerminationDirection(TerminationDirection.BIDIRECTIONAL)
                .setTerminationState(TerminationState.TERMINATEDBIDIRECTIONAL)
                .build();
            onepMap.put(onep.key(), onep);

            // MC nep
            LOG.info("MC NEP = {}", String.join("+", this.ietfNodeId, MC, tp.getTpId().getValue()));
            Name nepName1 = new NameBuilder()
                .setValueName(MC + "NodeEdgePoint")
                .setValue(String.join("+", this.ietfNodeId, MC, tp.getTpId().getValue()))
                .build();
            OwnedNodeEdgePointBuilder onepBldr1 = new OwnedNodeEdgePointBuilder()
                .setUuid(new Uuid(UUID.nameUUIDFromBytes((String.join("+", this.ietfNodeId, MC,
                    tp.getTpId().getValue())).getBytes(Charset.forName("UTF-8"))).toString()))
                .setLayerProtocolName(LayerProtocolName.PHOTONICMEDIA)
                .setName(Map.of(nepName1.key(), nepName1))
                .setSupportedCepLayerProtocolQualifier(List.of(PHOTONICLAYERQUALIFIEROMS.class))
                .setLinkPortDirection(PortDirection.BIDIRECTIONAL)
                .setLinkPortRole(PortRole.SYMMETRIC)
                .setAdministrativeState(setTapiAdminState(admin))
                .setOperationalState(setTapiOperationalState(oper))
                .setLifecycleState(LifecycleState.INSTALLED)
                .setTerminationDirection(TerminationDirection.BIDIRECTIONAL)
                .setTerminationState(TerminationState.TERMINATEDBIDIRECTIONAL);
            if (withSip) {
                onepBldr1.setMappedServiceInterfacePoint(createMSIP(1, LayerProtocolName.PHOTONICMEDIA,
                    tp, String.join("+", this.ietfNodeId, MC)));
            }
            OwnedNodeEdgePoint onep1 = onepBldr1.build();
            onepMap.put(onep1.key(), onep1);

            // OTSiMC nep
            LOG.info("OTSi NEP = {}", String.join("+", this.ietfNodeId, OTSI_MC, tp.getTpId().getValue()));
            Name nepName2 = new NameBuilder()
                .setValueName(OTSI_MC + "NodeEdgePoint")
                .setValue(String.join("+", this.ietfNodeId, OTSI_MC, tp.getTpId().getValue()))
                .build();

            OwnedNodeEdgePoint onep2 = new OwnedNodeEdgePointBuilder()
                .setUuid(new Uuid(UUID.nameUUIDFromBytes((String.join("+", this.ietfNodeId, OTSI_MC,
                    tp.getTpId().getValue())).getBytes(Charset.forName("UTF-8"))).toString()))
                .setLayerProtocolName(LayerProtocolName.PHOTONICMEDIA)
                .setName(Map.of(nepName2.key(), nepName2))
                .setSupportedCepLayerProtocolQualifier(List.of(PHOTONICLAYERQUALIFIEROMS.class))
                .setLinkPortDirection(PortDirection.BIDIRECTIONAL)
                .setLinkPortRole(PortRole.SYMMETRIC)
                .setAdministrativeState(setTapiAdminState(admin))
                .setOperationalState(setTapiOperationalState(oper))
                .setLifecycleState(LifecycleState.INSTALLED)
                .setTerminationDirection(TerminationDirection.BIDIRECTIONAL)
                .setTerminationState(TerminationState.TERMINATEDBIDIRECTIONAL)
                .build();
            onepMap.put(onep2.key(), onep2);
        }
        return onepMap;
    }

    private Map<NodeRuleGroupKey, NodeRuleGroup> createNodeRuleGroupForRdmNode(Uuid nodeUuid,
                                                                               Collection<OwnedNodeEdgePoint> onepl) {
        Map<org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.node.rule.group.NodeEdgePointKey,
            org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.node.rule.group.NodeEdgePoint>
            nepMap = new HashMap<>();
        for (OwnedNodeEdgePoint onep : onepl) {
            org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.node.rule.group.NodeEdgePoint
                nep = new org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.node.rule.group
                .NodeEdgePointBuilder()
                .setTopologyUuid(tapiTopoUuid)
                .setNodeUuid(nodeUuid)
                .setNodeEdgePointUuid(onep.key().getUuid())
                .build();
            nepMap.put(nep.key(), nep);
        }
        Map<NodeRuleGroupKey, NodeRuleGroup> nodeRuleGroupMap = new HashMap<>();
        Map<RuleKey, Rule> ruleList = new HashMap<>();
        Rule rule = new RuleBuilder()
            .setLocalId("forward")
            .setForwardingRule(ForwardingRule.MAYFORWARDACROSSGROUP)
            .setRuleType(RuleType.FORWARDING)
            .build();
        ruleList.put(rule.key(), rule);
        NodeRuleGroup nodeRuleGroup = new NodeRuleGroupBuilder()
            .setUuid(new Uuid(UUID.nameUUIDFromBytes((this.ietfNodeId + " node rule group")
                .getBytes(Charset.forName("UTF-8"))).toString()))
            .setRule(ruleList)
            .setNodeEdgePoint(nepMap)
            .build();
        nodeRuleGroupMap.put(nodeRuleGroup.key(), nodeRuleGroup);
        return nodeRuleGroupMap;
    }

    private Map<MappedServiceInterfacePointKey, MappedServiceInterfacePoint> createMSIP(int nb,
                                                                                        LayerProtocolName layerProtocol,
                                                                                        TerminationPoint tp,
                                                                                        String nodeid) {
        // add them to SIP context
        Map<MappedServiceInterfacePointKey, MappedServiceInterfacePoint> msipl = new HashMap<>();
        for (int i = 0; i < nb; i++) {
            LOG.info("SIP = {}", String.join("+", "SIP", nodeid, tp.getTpId().getValue()));
            Uuid sipUuid = new Uuid(UUID.nameUUIDFromBytes((String.join("+", "SIP", nodeid,
                tp.getTpId().getValue())).getBytes(Charset.forName("UTF-8"))).toString());
            MappedServiceInterfacePoint msip = new MappedServiceInterfacePointBuilder()
                .setServiceInterfacePointUuid(sipUuid).build();
            ServiceInterfacePoint sip = createSIP(sipUuid, layerProtocol, tp, nodeid);
            this.tapiSips.put(sip.key(), sip);
            msipl.put(msip.key(), msip);
        }
        return msipl;
    }

    private ServiceInterfacePoint createSIP(Uuid sipUuid, LayerProtocolName layerProtocol, TerminationPoint tp,
                                            String nodeid) {
        // TODO: what value should be set in total capacity and available capacity??
        // LOG.info("SIP name = {}", String.join("+", nodeid, tp.getTpId().getValue()));
        Name sipName = new NameBuilder()
            .setValueName("SIP name")
            .setValue(String.join("+", nodeid, tp.getTpId().getValue()))
            .build();
        return new ServiceInterfacePointBuilder()
            .setUuid(sipUuid)
            .setName(Map.of(sipName.key(), sipName))
            .setLayerProtocolName(layerProtocol)
            .setAdministrativeState(setTapiAdminState(
                tp.augmentation(TerminationPoint1.class).getAdministrativeState()))
            .setOperationalState(setTapiOperationalState(
                tp.augmentation(TerminationPoint1.class).getOperationalState()))
            .setLifecycleState(LifecycleState.INSTALLED)
            .setAvailableCapacity(new AvailableCapacityBuilder().build())
            .setTotalPotentialCapacity(new TotalPotentialCapacityBuilder().build())
            .setSupportedLayerProtocolQualifier(createSupportedLayerProtocolQualifier(tp, layerProtocol))
            .build();
    }

    private List<Class<? extends LAYERPROTOCOLQUALIFIER>> createSupportedLayerProtocolQualifier(TerminationPoint tp,
                                                                                                LayerProtocolName lpn) {
        Set<Class<? extends LAYERPROTOCOLQUALIFIER>> sclpqSet = new HashSet<>();
        List<SupportedInterfaceCapability> sicList;
        org.opendaylight.yang.gen.v1.http.org.openroadm.otn.network.topology.rev200529.TerminationPoint1 tp1 =
            tp.augmentation(org.opendaylight.yang.gen.v1.http
                .org.openroadm.otn.network.topology.rev200529.TerminationPoint1.class);
        if (tp1 == null) {
            return new ArrayList<>(sclpqSet);
        }
        if (tp1.getTpSupportedInterfaces() == null) {
            LOG.warn("Tp supported interface doesnt exist on TP {}", tp.getTpId().getValue());
            return new ArrayList<>(sclpqSet);
        }
        sicList = new ArrayList<>(tp1.getTpSupportedInterfaces().getSupportedInterfaceCapability().values());
        for (SupportedInterfaceCapability sic : sicList) {
            switch (lpn.getName()) {
                case "DSR":
                case "ODU":
                    switch (sic.getIfCapType().getSimpleName()) {
                        // TODO: it may be needed to add more cases clauses if the interface capabilities of a
                        //  port are extended in the config file
                        case "If1GEODU0":
                            sclpqSet.add(ODUTYPEODU0.class);
                            sclpqSet.add(DIGITALSIGNALTYPEGigE.class);
                            break;
                        case "If10GEODU2e":
                            sclpqSet.add(ODUTYPEODU2E.class);
                            sclpqSet.add(DIGITALSIGNALTYPE10GigELAN.class);
                            break;
                        case "If10GEODU2":
                            sclpqSet.add(ODUTYPEODU2.class);
                            sclpqSet.add(DIGITALSIGNALTYPE10GigELAN.class);
                            break;
                        case "If10GE":
                            sclpqSet.add(DIGITALSIGNALTYPE10GigELAN.class);
                            break;
                        case "If100GEODU4":
                            sclpqSet.add(DIGITALSIGNALTYPE100GigE.class);
                            sclpqSet.add(ODUTYPEODU4.class);
                            break;
                        case "If100GE":
                            sclpqSet.add(DIGITALSIGNALTYPE100GigE.class);
                            break;
                        case "IfOCHOTU4ODU4":
                        case "IfOCH":
                            sclpqSet.add(ODUTYPEODU4.class);
                            break;
                        default:
                            LOG.error("IfCapability type not managed");
                            break;
                    }
                    break;
                case "PHOTONIC_MEDIA":
                    if (sic.getIfCapType().getSimpleName().equals("IfOCHOTU4ODU4")
                        || sic.getIfCapType().getSimpleName().equals("IfOCH")) {
                        sclpqSet.add(PHOTONICLAYERQUALIFIEROTSi.class);
                        sclpqSet.add(PHOTONICLAYERQUALIFIEROMS.class);
                    }
                    break;
                default:
                    LOG.error("Layer Protocol Name is unknown");
                    break;
            }
        }
        return new ArrayList<>(sclpqSet);
    }

    private void createTapiTransitionalLinks() {
        for (TerminationPoint tp : this.oorNetworkPortList) {
            Map<NodeEdgePointKey, NodeEdgePoint> nepList = new HashMap<>();
            String sourceKey = String.join("+", this.ietfNodeId, I_ODU, tp.getTpId().getValue());
            Uuid sourceUuidTp = this.uuidMap.get(sourceKey);
            String destKey = String.join("+", this.ietfNodeId, I_OTSI, tp.getTpId().getValue());
            Uuid destUuidTp = this.uuidMap.get(destKey);
            NodeEdgePoint sourceNep = new NodeEdgePointBuilder()
                .setTopologyUuid(this.tapiTopoUuid)
                .setNodeUuid(this.uuidMap.get(String.join("+", this.ietfNodeId, DSR)))
                .setNodeEdgePointUuid(sourceUuidTp)
                .build();
            nepList.put(sourceNep.key(), sourceNep);
            NodeEdgePoint destNep = new NodeEdgePointBuilder()
                .setTopologyUuid(this.tapiTopoUuid)
                .setNodeUuid(this.uuidMap.get(String.join("+", this.ietfNodeId, OTSI)))
                .setNodeEdgePointUuid(destUuidTp)
                .build();
            nepList.put(destNep.key(), destNep);
            Name linkName = new NameBuilder().setValueName("transitional link name")
                .setValue(String.join("--", this.ietfNodeId, sourceKey, destKey))
                .build();
            CostCharacteristic costCharacteristic = new CostCharacteristicBuilder()
                .setCostAlgorithm("Restricted Shortest Path - RSP")
                .setCostName("HOP_COUNT")
                .setCostValue("12345678")
                .build();
            LatencyCharacteristic latencyCharacteristic = new LatencyCharacteristicBuilder()
                .setFixedLatencyCharacteristic("12345678")
                .setQueingLatencyCharacteristic("12345678")
                .setJitterCharacteristic("12345678")
                .setWanderCharacteristic("12345678")
                .setTrafficPropertyName("FIXED_LATENCY")
                .build();
            RiskCharacteristic riskCharacteristic = new RiskCharacteristicBuilder()
                .setRiskCharacteristicName("risk characteristic")
                .setRiskIdentifierList(List.of("risk identifier1", "risk identifier2"))
                .build();
            ValidationMechanism validationMechanism = new ValidationMechanismBuilder()
                .setValidationMechanism("validation mechanism")
                .setValidationRobustness("validation robustness")
                .setLayerProtocolAdjacencyValidated("layer protocol adjacency")
                .build();
            Link transiLink = new LinkBuilder()
                .setUuid(new Uuid(
                    UUID.nameUUIDFromBytes((String.join("--", this.ietfNodeId, sourceKey, destKey))
                        .getBytes(Charset.forName("UTF-8")))
                        .toString()))
                .setName(Map.of(linkName.key(), linkName))
                .setTransitionedLayerProtocolName(Arrays.asList(LayerProtocolName.ODU.getName(),
                    LayerProtocolName.PHOTONICMEDIA.getName()))
                .setLayerProtocolName(Arrays.asList(LayerProtocolName.ODU, LayerProtocolName.PHOTONICMEDIA))
                .setNodeEdgePoint(nepList)
                .setDirection(ForwardingDirection.BIDIRECTIONAL)
                .setAvailableCapacity(new AvailableCapacityBuilder().setTotalSize(
                    new TotalSizeBuilder().setUnit(CapacityUnit.GBPS).setValue(Uint64.valueOf(100)).build())
                    .build())
                .setResilienceType(new ResilienceTypeBuilder().setProtectionType(ProtectionType.NOPROTECTON)
                    .setRestorationPolicy(RestorationPolicy.NA)
                    .build())
                .setAdministrativeState(AdministrativeState.UNLOCKED)
                .setOperationalState(OperationalState.ENABLED)
                .setLifecycleState(LifecycleState.INSTALLED)
                .setTotalPotentialCapacity(new TotalPotentialCapacityBuilder().setTotalSize(
                    new TotalSizeBuilder().setUnit(CapacityUnit.GBPS).setValue(Uint64.valueOf(100)).build())
                    .build())
                .setCostCharacteristic(Map.of(costCharacteristic.key(), costCharacteristic))
                .setLatencyCharacteristic(Map.of(latencyCharacteristic.key(), latencyCharacteristic))
                .setRiskCharacteristic(Map.of(riskCharacteristic.key(), riskCharacteristic))
                .setErrorCharacteristic("error")
                .setLossCharacteristic("loss")
                .setRepeatDeliveryCharacteristic("repeat delivery")
                .setDeliveryOrderCharacteristic("delivery order")
                .setUnavailableTimeCharacteristic("unavailable time")
                .setServerIntegrityProcessCharacteristic("server integrity process")
                .setValidationMechanism(Map.of(validationMechanism.key(), validationMechanism))
                .build();
            this.tapiLinks.put(transiLink.key(), transiLink);
        }
    }

    private Link createTapiLink(org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang
                                    .ietf.network.topology.rev180226.networks.network.Link link,
                                org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang
                                    .ietf.network.topology.rev180226.networks.network.Link oppositeLink) {
        String prefix = link.getLinkId().getValue().split("-")[0];
        String sourceNode = link.getSource().getSourceNode().getValue();
        String sourceTp = link.getSource().getSourceTp().getValue();
        String destNode = link.getDestination().getDestNode().getValue();
        String destTp = link.getDestination().getDestTp().getValue();
        LOG.info("prefix = {}, sourceNode = {}, sourceTp = {}, destNode = {}, destTp = {}",
                prefix, sourceNode, sourceTp, destNode, destTp);
        Map<NodeEdgePointKey, NodeEdgePoint> nepList = new HashMap<>();
        Uuid sourceUuidTp;
        Uuid sourceUuidNode;
        Uuid destUuidTp;
        Uuid destUuidNode;
        Name linkName;
        AdminStates oppositeLinkAdminState = null;
        State oppositeLinkOperState = null;
        if (oppositeLink != null) {
            oppositeLinkAdminState = oppositeLink.augmentation(
                org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev200529.Link1.class)
                .getAdministrativeState();
            oppositeLinkOperState = oppositeLink.augmentation(
                org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev200529.Link1.class)
                .getOperationalState();
        }
        CostCharacteristic costCharacteristic = new CostCharacteristicBuilder()
            .setCostAlgorithm("Restricted Shortest Path - RSP")
            .setCostName("HOP_COUNT")
            .setCostValue("12345678")
            .build();
        LatencyCharacteristic latencyCharacteristic = new LatencyCharacteristicBuilder()
            .setFixedLatencyCharacteristic("12345678")
            .setQueingLatencyCharacteristic("12345678")
            .setJitterCharacteristic("12345678")
            .setWanderCharacteristic("12345678")
            .setTrafficPropertyName("FIXED_LATENCY")
            .build();
        RiskCharacteristic riskCharacteristic = new RiskCharacteristicBuilder()
            .setRiskCharacteristicName("risk characteristic")
            .setRiskIdentifierList(List.of("risk identifier1", "risk identifier2"))
            .build();
        ValidationMechanism validationMechanism = new ValidationMechanismBuilder()
            .setValidationMechanism("validation mechanism")
            .setValidationRobustness("validation robustness")
            .setLayerProtocolAdjacencyValidated("layer protocol adjacency")
            .build();
        switch (prefix) {
            case "OTU4":
                sourceUuidTp = this.uuidMap.get(String.join("+", sourceNode, I_OTSI, sourceTp));
                sourceUuidNode = this.uuidMap.get(String.join("+", sourceNode, OTSI));
                NodeEdgePoint sourceNep = new NodeEdgePointBuilder()
                    .setTopologyUuid(this.tapiTopoUuid)
                    .setNodeUuid(sourceUuidNode)
                    .setNodeEdgePointUuid(sourceUuidTp)
                    .build();
                nepList.put(sourceNep.key(), sourceNep);
                destUuidTp = this.uuidMap.get(String.join("+", destNode, I_OTSI, destTp));
                destUuidNode = this.uuidMap.get(String.join("+", destNode, OTSI));
                NodeEdgePoint destNep = new NodeEdgePointBuilder()
                    .setTopologyUuid(this.tapiTopoUuid)
                    .setNodeUuid(destUuidNode)
                    .setNodeEdgePointUuid(destUuidTp)
                    .build();
                nepList.put(destNep.key(), destNep);
                linkName = new NameBuilder().setValueName("otn link name")
                    .setValue(link.getLinkId().getValue())
                    .build();
                return new LinkBuilder()
                    .setUuid(new Uuid(UUID.nameUUIDFromBytes((link.getLinkId().getValue())
                        .getBytes(Charset.forName("UTF-8"))).toString()))
                    .setName(Map.of(linkName.key(), linkName))
                    .setLayerProtocolName(Arrays.asList(LayerProtocolName.PHOTONICMEDIA))
                    .setTransitionedLayerProtocolName(new ArrayList<>())
                    .setAdministrativeState(setTapiAdminState(link
                        .augmentation(
                            org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev200529.Link1.class)
                        .getAdministrativeState(), oppositeLinkAdminState))
                    .setOperationalState(setTapiOperationalState(link
                        .augmentation(
                            org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev200529.Link1.class)
                        .getOperationalState(), oppositeLinkOperState))
                    .setDirection(ForwardingDirection.BIDIRECTIONAL)
                    .setNodeEdgePoint(nepList)
                    .setTotalPotentialCapacity(new TotalPotentialCapacityBuilder().setTotalSize(
                        new TotalSizeBuilder().setUnit(CapacityUnit.GBPS)
                            .setValue(Uint64.valueOf(100)).build()).build())
                    .setAvailableCapacity(new AvailableCapacityBuilder().setTotalSize(
                        new TotalSizeBuilder().setUnit(CapacityUnit.MBPS)
                            .setValue(Uint64.valueOf(link.augmentation(Link1.class).getAvailableBandwidth())).build())
                        .build())
                    .setResilienceType(new ResilienceTypeBuilder().setProtectionType(ProtectionType.NOPROTECTON)
                        .setRestorationPolicy(RestorationPolicy.NA)
                        .build())
                    .setLifecycleState(LifecycleState.INSTALLED)
                    .setCostCharacteristic(Map.of(costCharacteristic.key(), costCharacteristic))
                    .setLatencyCharacteristic(Map.of(latencyCharacteristic.key(), latencyCharacteristic))
                    .setRiskCharacteristic(Map.of(riskCharacteristic.key(), riskCharacteristic))
                    .setErrorCharacteristic("error")
                    .setLossCharacteristic("loss")
                    .setRepeatDeliveryCharacteristic("repeat delivery")
                    .setDeliveryOrderCharacteristic("delivery order")
                    .setUnavailableTimeCharacteristic("unavailable time")
                    .setServerIntegrityProcessCharacteristic("server integrity process")
                    .setValidationMechanism(Map.of(validationMechanism.key(), validationMechanism))
                    .build();
            case "ODTU4":
                sourceUuidTp = this.uuidMap.get(String.join("+", sourceNode, E_ODU, sourceTp));
                sourceUuidNode = this.uuidMap.get(String.join("+", sourceNode, DSR));
                NodeEdgePoint sourceNep2 = new NodeEdgePointBuilder()
                    .setTopologyUuid(this.tapiTopoUuid)
                    .setNodeUuid(sourceUuidNode)
                    .setNodeEdgePointUuid(sourceUuidTp)
                    .build();
                nepList.put(sourceNep2.key(), sourceNep2);
                destUuidTp = this.uuidMap.get(String.join("+", destNode, E_ODU, destTp));
                destUuidNode = this.uuidMap.get(String.join("+", destNode, DSR));
                NodeEdgePoint destNep2 = new NodeEdgePointBuilder()
                    .setTopologyUuid(this.tapiTopoUuid)
                    .setNodeUuid(destUuidNode)
                    .setNodeEdgePointUuid(destUuidTp)
                    .build();
                nepList.put(destNep2.key(), destNep2);
                linkName = new NameBuilder().setValueName("otn link name")
                    .setValue(link.getLinkId().getValue())
                    .build();
                return new LinkBuilder()
                    .setUuid(new Uuid(UUID.nameUUIDFromBytes((link.getLinkId().getValue())
                        .getBytes(Charset.forName("UTF-8"))).toString()))
                    .setName(Map.of(linkName.key(), linkName))
                    .setLayerProtocolName(Arrays.asList(LayerProtocolName.ODU))
                    .setTransitionedLayerProtocolName(new ArrayList<>())
                    .setAdministrativeState(setTapiAdminState(link
                        .augmentation(
                            org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev200529.Link1.class)
                        .getAdministrativeState(), oppositeLinkAdminState))
                    .setOperationalState(setTapiOperationalState(link
                        .augmentation(
                            org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev200529.Link1.class)
                        .getOperationalState(), oppositeLinkOperState))
                    .setDirection(ForwardingDirection.BIDIRECTIONAL)
                    .setNodeEdgePoint(nepList)
                    .setTotalPotentialCapacity(new TotalPotentialCapacityBuilder().setTotalSize(
                        new TotalSizeBuilder().setUnit(CapacityUnit.GBPS)
                            .setValue(Uint64.valueOf(100)).build()).build())
                    .setAvailableCapacity(new AvailableCapacityBuilder().setTotalSize(
                        new TotalSizeBuilder().setUnit(CapacityUnit.MBPS)
                            .setValue(Uint64.valueOf(link.augmentation(Link1.class).getAvailableBandwidth())).build())
                        .build())
                    .setResilienceType(new ResilienceTypeBuilder().setProtectionType(ProtectionType.NOPROTECTON)
                        .setRestorationPolicy(RestorationPolicy.NA)
                        .build())
                    .setLifecycleState(LifecycleState.INSTALLED)
                    .setCostCharacteristic(Map.of(costCharacteristic.key(), costCharacteristic))
                    .setLatencyCharacteristic(Map.of(latencyCharacteristic.key(), latencyCharacteristic))
                    .setRiskCharacteristic(Map.of(riskCharacteristic.key(), riskCharacteristic))
                    .setErrorCharacteristic("error")
                    .setLossCharacteristic("loss")
                    .setRepeatDeliveryCharacteristic("repeat delivery")
                    .setDeliveryOrderCharacteristic("delivery order")
                    .setUnavailableTimeCharacteristic("unavailable time")
                    .setServerIntegrityProcessCharacteristic("server integrity process")
                    .setValidationMechanism(Map.of(validationMechanism.key(), validationMechanism))
                    .build();
            default:
                LOG.error("OTN link of type {} not managed yet", prefix);
                return null;
        }
    }

    private Link createTapiOmsLink(org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang
                                       .ietf.network.topology.rev180226.networks.network.Link link,
                                   org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang
                                       .ietf.network.topology.rev180226.networks.network.Link oppositeLink) {
        String sourceNode = getIdBasedOnModelVersion(link.getSource().getSourceNode().getValue());
        String sourceTp = link.getSource().getSourceTp().getValue();
        String destNode = getIdBasedOnModelVersion(link.getDestination().getDestNode().getValue());
        String destTp = link.getDestination().getDestTp().getValue();
        AdminStates oppositeLinkAdminState = null;
        State oppositeLinkOperState = null;
        if (oppositeLink != null) {
            oppositeLinkAdminState = oppositeLink.augmentation(
                org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev200529.Link1.class)
                .getAdministrativeState();
            oppositeLinkOperState = oppositeLink.augmentation(
                org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev200529.Link1.class)
                .getOperationalState();
        }
        Map<NodeEdgePointKey, NodeEdgePoint> nepList = new HashMap<>();
        Uuid sourceUuidNode = new Uuid(UUID.nameUUIDFromBytes((String.join("+", sourceNode,
            PHTNC_MEDIA)).getBytes(Charset.forName("UTF-8"))).toString());
        Uuid sourceUuidTp = new Uuid(UUID.nameUUIDFromBytes((String.join("+", sourceNode, PHTNC_MEDIA, sourceTp))
            .getBytes(Charset.forName("UTF-8"))).toString());
        Uuid destUuidNode = new Uuid(UUID.nameUUIDFromBytes((String.join("+", destNode,
            PHTNC_MEDIA)).getBytes(Charset.forName("UTF-8"))).toString());
        Uuid destUuidTp = new Uuid(UUID.nameUUIDFromBytes((String.join("+", destNode, PHTNC_MEDIA, destTp))
            .getBytes(Charset.forName("UTF-8"))).toString());
        NodeEdgePoint sourceNep = new NodeEdgePointBuilder()
            .setTopologyUuid(this.tapiTopoUuid)
            .setNodeUuid(sourceUuidNode)
            .setNodeEdgePointUuid(sourceUuidTp)
            .build();
        nepList.put(sourceNep.key(), sourceNep);
        NodeEdgePoint destNep = new NodeEdgePointBuilder()
            .setTopologyUuid(this.tapiTopoUuid)
            .setNodeUuid(destUuidNode)
            .setNodeEdgePointUuid(destUuidTp)
            .build();
        nepList.put(destNep.key(), destNep);
        Name linkName = new NameBuilder().setValueName("OMS link name")
            .setValue(link.getLinkId().getValue())
            .build();
        CostCharacteristic costCharacteristic = new CostCharacteristicBuilder()
            .setCostAlgorithm("Restricted Shortest Path - RSP")
            .setCostName("HOP_COUNT")
            .setCostValue("12345678")
            .build();
        LatencyCharacteristic latencyCharacteristic = new LatencyCharacteristicBuilder()
            .setFixedLatencyCharacteristic("12345678")
            .setQueingLatencyCharacteristic("12345678")
            .setJitterCharacteristic("12345678")
            .setWanderCharacteristic("12345678")
            .setTrafficPropertyName("FIXED_LATENCY")
            .build();
        RiskCharacteristic riskCharacteristic = new RiskCharacteristicBuilder()
            .setRiskCharacteristicName("risk characteristic")
            .setRiskIdentifierList(List.of("risk identifier1", "risk identifier2"))
            .build();
        ValidationMechanism validationMechanism = new ValidationMechanismBuilder()
            .setValidationMechanism("validation mechanism")
            .setValidationRobustness("validation robustness")
            .setLayerProtocolAdjacencyValidated("layer protocol adjacency")
            .build();
        return new LinkBuilder()
            .setUuid(new Uuid(
                UUID.nameUUIDFromBytes((link.getLinkId().getValue()).getBytes(Charset.forName("UTF-8")))
                    .toString()))
            .setName(Map.of(linkName.key(), linkName))
            .setLayerProtocolName(List.of(LayerProtocolName.PHOTONICMEDIA))
            .setTransitionedLayerProtocolName(new ArrayList<>())
            .setNodeEdgePoint(nepList)
            .setDirection(ForwardingDirection.BIDIRECTIONAL)
            .setResilienceType(new ResilienceTypeBuilder().setProtectionType(ProtectionType.NOPROTECTON)
                .setRestorationPolicy(RestorationPolicy.NA)
                .build())
            .setAdministrativeState(setTapiAdminState(link
                .augmentation(
                    org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev200529.Link1.class)
                .getAdministrativeState(), oppositeLinkAdminState))
            .setOperationalState(setTapiOperationalState(link
                .augmentation(
                    org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev200529.Link1.class)
                .getOperationalState(), oppositeLinkOperState))
            .setLifecycleState(LifecycleState.INSTALLED)
            .setTotalPotentialCapacity(new TotalPotentialCapacityBuilder().setTotalSize(
                new TotalSizeBuilder().setUnit(CapacityUnit.GBPS)
                    .setValue(Uint64.valueOf(100)).build()).build())
            .setAvailableCapacity(new AvailableCapacityBuilder().setTotalSize(
                new TotalSizeBuilder().setUnit(CapacityUnit.MBPS)
                    .setValue(Uint64.valueOf(100)).build())
                .build())
            .setCostCharacteristic(Map.of(costCharacteristic.key(), costCharacteristic))
            .setLatencyCharacteristic(Map.of(latencyCharacteristic.key(), latencyCharacteristic))
            .setRiskCharacteristic(Map.of(riskCharacteristic.key(), riskCharacteristic))
            .setErrorCharacteristic("error")
            .setLossCharacteristic("loss")
            .setRepeatDeliveryCharacteristic("repeat delivery")
            .setDeliveryOrderCharacteristic("delivery order")
            .setUnavailableTimeCharacteristic("unavailable time")
            .setServerIntegrityProcessCharacteristic("server integrity process")
            .setValidationMechanism(Map.of(validationMechanism.key(), validationMechanism))
            .build();
    }

    public void convertXpdrToRdmLinks(List<org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang
            .ietf.network.topology.rev180226.networks.network.Link> xpdrRdmLinkList) {
        List<String> linksToNotConvert = new ArrayList<>();
        LOG.info("creation of {} xpdr to roadm links", xpdrRdmLinkList.size() / 2);
        // LOG.info("Link list = {}", xpdrRdmLinkList.toString());
        for (org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang
            .ietf.network.topology.rev180226.networks.network.Link link:xpdrRdmLinkList) {
            if (!linksToNotConvert.contains(link.getLinkId().getValue())) {
                org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang
                    .ietf.network.topology.rev180226.networks.network.Link oppositeLink = xpdrRdmLinkList.stream()
                    .filter(l -> l.getLinkId().equals(link.augmentation(org.opendaylight.yang.gen.v1.http
                        .org.openroadm.common.network.rev200529.Link1.class).getOppositeLink())).findAny().orElse(null);

                AdminStates oppositeLinkAdminState = null;
                State oppositeLinkOperState = null;
                if (oppositeLink != null) {
                    oppositeLinkAdminState = oppositeLink.augmentation(
                        org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev200529.Link1.class)
                        .getAdministrativeState();
                    oppositeLinkOperState = oppositeLink.augmentation(
                        org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev200529.Link1.class)
                        .getOperationalState();
                }

                String sourceNode = (link.getSource().getSourceNode().getValue().contains("ROADM"))
                    ? getIdBasedOnModelVersion(link.getSource().getSourceNode().getValue())
                    : link.getSource().getSourceNode().getValue();
                String sourceTp = link.getSource().getSourceTp().getValue();
                String destNode = (link.getDestination().getDestNode().getValue().contains("ROADM"))
                    ? getIdBasedOnModelVersion(link.getDestination().getDestNode().getValue())
                    : link.getDestination().getDestNode().getValue();
                String destTp = link.getDestination().getDestTp().getValue();
                Map<NodeEdgePointKey, NodeEdgePoint> nepList = new HashMap<>();
                Uuid sourceUuidNode = (sourceNode.contains("ROADM")) ? new Uuid(UUID.nameUUIDFromBytes((
                    String.join("+", sourceNode, PHTNC_MEDIA)).getBytes(Charset.forName("UTF-8"))).toString())
                    : new Uuid(UUID.nameUUIDFromBytes((String.join("+", sourceNode, OTSI))
                        .getBytes(Charset.forName("UTF-8"))).toString());
                Uuid sourceUuidTp = new Uuid(UUID.nameUUIDFromBytes(
                    (String.join("+", sourceNode, PHTNC_MEDIA, sourceTp))
                        .getBytes(Charset.forName("UTF-8"))).toString());
                Uuid destUuidNode = (destNode.contains("ROADM")) ? new Uuid(UUID.nameUUIDFromBytes((
                    String.join("+", destNode, PHTNC_MEDIA)).getBytes(Charset.forName("UTF-8"))).toString())
                    : new Uuid(UUID.nameUUIDFromBytes((String.join("+", destNode, OTSI))
                        .getBytes(Charset.forName("UTF-8"))).toString());
                Uuid destUuidTp = new Uuid(UUID.nameUUIDFromBytes((String.join("+", destNode,
                    PHTNC_MEDIA, destTp)).getBytes(Charset.forName("UTF-8"))).toString());
                NodeEdgePoint sourceNep = new NodeEdgePointBuilder()
                    .setTopologyUuid(this.tapiTopoUuid)
                    .setNodeUuid(sourceUuidNode)
                    .setNodeEdgePointUuid(sourceUuidTp)
                    .build();
                nepList.put(sourceNep.key(), sourceNep);
                NodeEdgePoint destNep = new NodeEdgePointBuilder()
                    .setTopologyUuid(this.tapiTopoUuid)
                    .setNodeUuid(destUuidNode)
                    .setNodeEdgePointUuid(destUuidTp)
                    .build();
                nepList.put(destNep.key(), destNep);
                Name linkName = new NameBuilder().setValueName("XPDR-RDM link name")
                    .setValue(link.getLinkId().getValue())
                    .build();
                CostCharacteristic costCharacteristic = new CostCharacteristicBuilder()
                    .setCostAlgorithm("Restricted Shortest Path - RSP")
                    .setCostName("HOP_COUNT")
                    .setCostValue("12345678")
                    .build();
                LatencyCharacteristic latencyCharacteristic = new LatencyCharacteristicBuilder()
                    .setFixedLatencyCharacteristic("12345678")
                    .setQueingLatencyCharacteristic("12345678")
                    .setJitterCharacteristic("12345678")
                    .setWanderCharacteristic("12345678")
                    .setTrafficPropertyName("FIXED_LATENCY")
                    .build();
                RiskCharacteristic riskCharacteristic = new RiskCharacteristicBuilder()
                    .setRiskCharacteristicName("risk characteristic")
                    .setRiskIdentifierList(List.of("risk identifier1", "risk identifier2"))
                    .build();
                ValidationMechanism validationMechanism = new ValidationMechanismBuilder()
                    .setValidationMechanism("validation mechanism")
                    .setValidationRobustness("validation robustness")
                    .setLayerProtocolAdjacencyValidated("layer protocol adjacency")
                    .build();
                Link tapiLink = new LinkBuilder()
                    .setUuid(new Uuid(
                        UUID.nameUUIDFromBytes((link.getLinkId().getValue()).getBytes(Charset.forName("UTF-8")))
                            .toString()))
                    .setName(Map.of(linkName.key(), linkName))
                    .setLayerProtocolName(List.of(LayerProtocolName.PHOTONICMEDIA))
                    .setTransitionedLayerProtocolName(new ArrayList<>())
                    .setNodeEdgePoint(nepList)
                    .setDirection(ForwardingDirection.BIDIRECTIONAL)
                    .setResilienceType(new ResilienceTypeBuilder().setProtectionType(ProtectionType.NOPROTECTON)
                        .setRestorationPolicy(RestorationPolicy.NA)
                        .build())
                    .setAdministrativeState(setTapiAdminState(link
                        .augmentation(
                            org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev200529.Link1.class)
                        .getAdministrativeState(), oppositeLinkAdminState))
                    .setOperationalState(setTapiOperationalState(link
                        .augmentation(
                            org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev200529.Link1.class)
                        .getOperationalState(), oppositeLinkOperState))
                    .setLifecycleState(LifecycleState.INSTALLED)
                    .setTotalPotentialCapacity(new TotalPotentialCapacityBuilder().setTotalSize(
                        new TotalSizeBuilder().setUnit(CapacityUnit.GBPS)
                            .setValue(Uint64.valueOf(100)).build()).build())
                    .setAvailableCapacity(new AvailableCapacityBuilder().setTotalSize(
                        new TotalSizeBuilder().setUnit(CapacityUnit.MBPS)
                            .setValue(Uint64.valueOf(100)).build())
                        .build())
                    .setCostCharacteristic(Map.of(costCharacteristic.key(), costCharacteristic))
                    .setLatencyCharacteristic(Map.of(latencyCharacteristic.key(), latencyCharacteristic))
                    .setRiskCharacteristic(Map.of(riskCharacteristic.key(), riskCharacteristic))
                    .setErrorCharacteristic("error")
                    .setLossCharacteristic("loss")
                    .setRepeatDeliveryCharacteristic("repeat delivery")
                    .setDeliveryOrderCharacteristic("delivery order")
                    .setUnavailableTimeCharacteristic("unavailable time")
                    .setServerIntegrityProcessCharacteristic("server integrity process")
                    .setValidationMechanism(Map.of(validationMechanism.key(), validationMechanism))
                    .build();
                linksToNotConvert.add(link
                    .augmentation(org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev200529.Link1.class)
                    .getOppositeLink().getValue());
                this.tapiLinks.put(tapiLink.key(), tapiLink);
            }
        }
    }

    private String getIdBasedOnModelVersion(String linknodeid) {
        if (linknodeid.matches("[A-Z]{5}-[A-Z0-9]{2}-.*")) {
            LOG.info("OpenROADM version > 1.2.1 {}", linknodeid);
            return String.join("-", linknodeid.split("-")[0], linknodeid.split("-")[1]);
        } else {
            LOG.info("OpenROADM version <= 1.2.1 {}", linknodeid);
            return linknodeid.split("-")[0];
        }
    }

    public Map<NodeKey, org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.topology.Node>
            getTapiNodes() {
        return tapiNodes;
    }

    public Map<LinkKey, Link> getTapiLinks() {
        return tapiLinks;
    }

    public Map<ServiceInterfacePointKey, ServiceInterfacePoint> getTapiSips() {
        return tapiSips;
    }
}
