/*
 * Copyright © 2021 Nokia.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.tapi.utils;

import java.math.RoundingMode;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.transportpce.common.NetworkUtils;
import org.opendaylight.transportpce.common.network.NetworkTransactionService;
import org.opendaylight.transportpce.networkmodel.util.LinkIdUtil;
import org.opendaylight.transportpce.tapi.TapiStringConstants;
import org.opendaylight.transportpce.tapi.topology.ORToTapiTopoConversionFactory;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.state.types.rev191129.State;
import org.opendaylight.yang.gen.v1.http.org.openroadm.equipment.states.types.rev191129.AdminStates;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.NetworkId;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.NodeId;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.networks.NetworkKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.LinkId;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.Network1;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.AdministrativeState;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.CAPACITYUNITGBPS;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.Context;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.ForwardingDirection;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.LayerProtocolName;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.LifecycleState;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.OperationalState;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.Uuid;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.capacity.TotalSizeBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.capacity.pac.AvailableCapacityBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.capacity.pac.TotalPotentialCapacityBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.global._class.NameBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.photonic.media.rev221121.context.topology.context.topology.node.owned.node.edge.point.cep.list.connection.end.point.OtsMediaConnectionEndPointSpec;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.photonic.media.rev221121.context.topology.context.topology.node.owned.node.edge.point.cep.list.connection.end.point.OtsMediaConnectionEndPointSpecBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.photonic.media.rev221121.impairment.route.entry.OtsConcentratedLossBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.photonic.media.rev221121.impairment.route.entry.OtsFiberSpanImpairments;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.photonic.media.rev221121.impairment.route.entry.OtsFiberSpanImpairmentsBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.photonic.media.rev221121.ots.impairments.ImpairmentRouteEntry;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.photonic.media.rev221121.ots.impairments.ImpairmentRouteEntryBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.photonic.media.rev221121.ots.media.connection.end.point.spec.OtsImpairments;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.photonic.media.rev221121.ots.media.connection.end.point.spec.OtsImpairmentsBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.Context1;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.ProtectionType;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.RestorationPolicy;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.context.TopologyContext;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.link.NodeEdgePoint;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.link.NodeEdgePointBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.link.ResilienceTypeBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.node.OwnedNodeEdgePoint;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.node.OwnedNodeEdgePointKey;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.risk.parameter.pac.RiskCharacteristic;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.risk.parameter.pac.RiskCharacteristicBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.topology.Link;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.topology.LinkBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.topology.Node;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.topology.NodeKey;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.topology.context.Topology;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.topology.context.TopologyKey;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.transfer.cost.pac.CostCharacteristic;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.transfer.cost.pac.CostCharacteristicBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.transfer.timing.pac.LatencyCharacteristic;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.transfer.timing.pac.LatencyCharacteristicBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.validation.pac.ValidationMechanism;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.validation.pac.ValidationMechanismBuilder;
import org.opendaylight.yangtools.binding.DataObjectIdentifier;
import org.opendaylight.yangtools.yang.common.Decimal64;
import org.opendaylight.yangtools.yang.common.Uint64;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
public class TapiLinkImpl implements TapiLink {

    private static final Logger LOG = LoggerFactory.getLogger(TapiLinkImpl.class);
    private final NetworkTransactionService networkTransactionService;
    private final TapiContext tapiContext;
    private Map<Map<String, String>, org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev221121.cep.list
        .ConnectionEndPoint> cepMap;

    @Activate
    public TapiLinkImpl(@Reference NetworkTransactionService networkTransactionService,
            @Reference TapiContext tapiContext) {
        this.networkTransactionService = networkTransactionService;
        this.tapiContext = tapiContext;
        this.cepMap = new HashMap<>();
    }

    public Link createTapiLink(String srcNodeId, String srcTpId, String dstNodeId, String dstTpId, String linkType,
            String srcNodeQual, String dstNodeQual, String srcTpQual, String dstTpQual,
            String adminState, String operState, Set<LayerProtocolName> layerProtoNameList,
            Set<String> transLayerNameList, Uuid tapiTopoUuid) {

        LOG.info("LINKIMPL111, entering create tapiLink from {} to {}", srcNodeId, dstNodeId);
        String sourceNepKey = String.join("+", srcNodeId, srcTpQual, srcTpId);
        String destNepKey = String.join("+", dstNodeId, dstTpQual, dstTpId);
        String linkKey = String.join("to", sourceNepKey, destNepKey);
        NodeEdgePoint sourceNep = new NodeEdgePointBuilder()
            .setTopologyUuid(tapiTopoUuid)
            .setNodeUuid(
                new Uuid(UUID.nameUUIDFromBytes(
                    String.join("+", srcNodeId, srcNodeQual).getBytes(Charset.forName("UTF-8"))).toString()))
            .setNodeEdgePointUuid(
                new Uuid(UUID.nameUUIDFromBytes(sourceNepKey.getBytes(Charset.forName("UTF-8"))).toString()))
            .build();
        NodeEdgePoint destNep = new NodeEdgePointBuilder()
            .setTopologyUuid(tapiTopoUuid)
            .setNodeUuid(
                new Uuid(UUID.nameUUIDFromBytes(
                    String.join("+", dstNodeId, dstNodeQual).getBytes(Charset.forName("UTF-8"))).toString()))
            .setNodeEdgePointUuid(
                new Uuid(UUID.nameUUIDFromBytes(destNepKey.getBytes(Charset.forName("UTF-8"))).toString()))
            .build();
        NameBuilder linkName = new NameBuilder();
        // TODO: variables for each type
        switch (linkType) {
            case TapiStringConstants.OMS_RDM_RDM_LINK:
                LOG.info("Roadm to roadm link");
                LOG.info("TAPILinkImpl Building LinkId {}", buildORLinkId(
                    String.join("-", srcNodeId, srcTpId.split("\\-")[0]), srcTpId,
                    String.join("-", dstNodeId, dstTpId.split("\\-")[0]),dstTpId)
                    .toString());
                linkName
                    .setValueName(TapiStringConstants.VALUE_NAME_OMS_RDM_RDM_LINK)
                    .setValue(linkKey);
                createCepForLink(getORLinkFromLinkId(buildORLinkId(
                    String.join("-", srcNodeId, srcTpId.split("\\-")[0]), srcTpId,
                    String.join("-", dstNodeId, dstTpId.split("\\-")[0]),dstTpId)));
                break;
            case TapiStringConstants.TRANSITIONAL_LINK:
                LOG.info("Transitional link");
                linkName
                    .setValueName("transitional link name")
                    .setValue(linkKey);
                break;
            case TapiStringConstants.OMS_XPDR_RDM_LINK:
                LOG.info(TapiStringConstants.VALUE_NAME_OTS_XPDR_RDM_LINK);
                linkName
                    .setValueName("XPDR-RDM link name")
                    .setValue(linkKey);
                break;
            case TapiStringConstants.OTN_XPDR_XPDR_LINK:
                LOG.info("OTN Xpdr to roadm link");
                linkName
                    .setValueName(TapiStringConstants.VALUE_NAME_OTN_XPDR_XPDR_LINK)
                    .setValue(linkKey);
                break;
            default:
                LOG.warn("Type {} not recognized", linkType);
                return null;
        }
        // Todo: common aspects of links and set all attributes
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
        ValidationMechanism validationMechanism = new ValidationMechanismBuilder()
            .setValidationMechanism("validation mechanism")
            .setValidationRobustness("validation robustness")
            .setLayerProtocolAdjacencyValidated("layer protocol adjacency")
            .build();
        LOG.info("LINKIMPL195, successfully created tapiLink {} of type {}", linkKey, linkType);
        return new LinkBuilder()
            .setUuid(new Uuid(
                UUID.nameUUIDFromBytes(linkKey.getBytes(Charset.forName("UTF-8"))).toString()))
            .setName(Map.of(linkName.build().key(), linkName.build()))
            //Bug in TAPI : transitioned layer protocol name is mandatory (whether this concept has disappeared)
            // Additionally, the grouping defining it requires at least 2 elements.
            // Seems that yang tools check has been enforced and check this --> set translayerNameList arbitrary
            .setTransitionedLayerProtocolName(Set.of(TapiStringConstants.PHTNC_MEDIA_OMS,
                TapiStringConstants.PHTNC_MEDIA_OTS))
            .setLayerProtocolName(layerProtoNameList)
            .setNodeEdgePoint(
                new HashMap<>(Map.of(sourceNep.key(), sourceNep, destNep.key(), destNep)))
            .setDirection(ForwardingDirection.BIDIRECTIONAL)
            .setAvailableCapacity(new AvailableCapacityBuilder().setTotalSize(
                    new TotalSizeBuilder().setUnit(CAPACITYUNITGBPS.VALUE).setValue(Decimal64.valueOf("100")).build())
                .build())
            .setResilienceType(new ResilienceTypeBuilder().setProtectionType(ProtectionType.NOPROTECTION)
                .setRestorationPolicy(RestorationPolicy.NA)
                .build())
            .setAdministrativeState(setTapiAdminState(adminState))
            .setOperationalState(setTapiOperationalState(operState))
            .setLifecycleState(LifecycleState.INSTALLED)
            .setTotalPotentialCapacity(new TotalPotentialCapacityBuilder().setTotalSize(
                    new TotalSizeBuilder().setUnit(CAPACITYUNITGBPS.VALUE).setValue(Decimal64.valueOf("100")).build())
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

    public LinkId buildORLinkId(String srcNode, String srcTp, String destNode, String destTp) {
        LOG.info("InTapiLinkImpl, retrieves link ID {} from source and destination Nodes & tps",
            LinkIdUtil.buildLinkId(srcNode, srcTp, destNode, destTp));
        return LinkIdUtil.buildLinkId(srcNode, srcTp, destNode, destTp);
    }

    private org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network
            .topology.rev180226.networks.network.Link getORLinkFromLinkId(LinkId linkId) {

        DataObjectIdentifier<org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226
            .networks.network.Link> linkIID = DataObjectIdentifier.builder(
                org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.Networks.class)
                    .child(org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226
                            .networks.Network.class,
                        new NetworkKey(new NetworkId(NetworkUtils.OVERLAY_NETWORK_ID)))
                    .augmentation(Network1.class)
                    .child(org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226
                        .networks.network.Link.class, new org.opendaylight.yang.gen.v1.urn.ietf.params
                        .xml.ns.yang.ietf.network.topology.rev180226
                        .networks.network.LinkKey(linkId))
                    .build();
        try {
            Optional<org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network
                .topology.rev180226.networks.network.Link> link = this.networkTransactionService.read(
                LogicalDatastoreType.CONFIGURATION, linkIID).get();
            if (link.isEmpty()) {
                LOG.error("Link {} not present in the datastore", linkId);
                return null;
            }
            //return link
            return link.orElseThrow();
        } catch (InterruptedException | ExecutionException e) {
            LOG.error("Failed getting Link data from Datastore",e);
        }
        return null;
    }

    public void createCepForLink(org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network
            .topology.rev180226.networks.network.Link link) {
        //Retrieve OMS from OR link for both end
        //Build OTS media connection End Point spec
        //Build Cep and put them in DataStore
        LOG.info("In TapiLinkImpl, creating CEP");
        Map<String,Double> lossPoutcorrect = NetworkUtils.calcSpanLoss(link);
        Decimal64 linkLoss = Decimal64.valueOf("9999");
        if (lossPoutcorrect != null && lossPoutcorrect.containsKey("SpanLoss")) {
            linkLoss = Decimal64.valueOf(lossPoutcorrect.entrySet().stream()
                .filter(res -> res.getKey().equals("SpanLoss")).findFirst().orElseThrow().getValue().doubleValue(),
                RoundingMode.UP);
        }
        Map<String, Double> pmd = NetworkUtils.calcCDandPMD(link);
        Decimal64 pmdValue = Decimal64.valueOf("0");
        if (pmd != null && pmd.containsKey("PMD")) {
            pmdValue = Decimal64.valueOf(pmd.entrySet().stream().filter(res -> res.getKey()
                .equals("PMD")).findFirst().orElseThrow().getValue().doubleValue(),RoundingMode.UP);
        }
        Decimal64 oppLinkLoss;
        OtsFiberSpanImpairments otsFSimp = new OtsFiberSpanImpairmentsBuilder()
            .setConnectorIn(Decimal64.valueOf("0"))
            .setConnectorOut(Decimal64.valueOf("0"))
            .setLength(
                NetworkUtils.calcLength(link) != null
                    ? Uint64.valueOf(Math.round(NetworkUtils.calcLength(link)))
                    : Uint64.valueOf(9999))
            .setPmd(pmdValue)
            .setTotalLoss(linkLoss)
            .build();
        OtsFiberSpanImpairments otsFSimpOppLink;
        if (link.augmentation(org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev230526
                .Link1.class).getOppositeLink() == null) {
            otsFSimpOppLink = otsFSimp;
            oppLinkLoss = linkLoss;
        } else {
            org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226
                .networks.network.Link oppLink = getORLinkFromLinkId(
                    link.augmentation(org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev230526
                    .Link1.class).getOppositeLink());
            Map<String,Double> opplossPoutcorrect = NetworkUtils.calcSpanLoss(oppLink);
            oppLinkLoss = Decimal64.valueOf("9999");
            if (opplossPoutcorrect != null && opplossPoutcorrect.containsKey("SpanLoss")) {
                oppLinkLoss = Decimal64.valueOf(opplossPoutcorrect.entrySet().stream()
                    .filter(res -> res.getKey().equals("SpanLoss")).findFirst().orElseThrow().getValue().doubleValue(),
                    RoundingMode.UP);
            }
            Map<String, Double> opppmd = NetworkUtils.calcCDandPMD(link);
            Decimal64 opppmdValue = Decimal64.valueOf("0");
            if (opppmd != null && opppmd.containsKey("PMD")) {
                opppmdValue = Decimal64.valueOf(opppmd.entrySet().stream().filter(res -> res.getKey()
                    .equals("PMD")).findFirst().orElseThrow().getValue().doubleValue(),RoundingMode.UP);
            }
            otsFSimpOppLink = new OtsFiberSpanImpairmentsBuilder()
                .setConnectorIn(Decimal64.valueOf("0"))
                .setConnectorOut(Decimal64.valueOf("0"))
                .setLength(NetworkUtils.calcLength(oppLink) != null
                        ? Uint64.valueOf(Math.round(NetworkUtils.calcLength(oppLink)))
                        : Uint64.valueOf(9999))
                .setPmd(opppmdValue)
                .setTotalLoss(oppLinkLoss)
                .build();
        }
        LOG.info("In TapiLinkImpl, building Impairments for CEP");
        ImpairmentRouteEntry ire = new ImpairmentRouteEntryBuilder()
            .setOtsConcentratedLoss(new OtsConcentratedLossBuilder()
                .setConcentratedLoss(linkLoss).build())
            .setOtsFiberSpanImpairments(otsFSimp)
            .build();
        ImpairmentRouteEntry ire2 = new ImpairmentRouteEntryBuilder()
            .setOtsConcentratedLoss(new OtsConcentratedLossBuilder()
                .setConcentratedLoss(oppLinkLoss).build())
            .setOtsFiberSpanImpairments(otsFSimpOppLink)
            .build();
        List<OtsImpairments> otsImpairmentListA = new ArrayList<>(List.of(
            new OtsImpairmentsBuilder()
                .setImpairmentRouteEntry(List.of(ire))
                .setIngressDirection(true)
                .build(),
            new OtsImpairmentsBuilder()
                .setImpairmentRouteEntry(List.of(ire2))
                .setIngressDirection(false)
                .build()));
        List<OtsImpairments> otsImpairmentListZ = new ArrayList<>(List.of(
            new OtsImpairmentsBuilder()
                .setImpairmentRouteEntry(List.of(ire))
                .setIngressDirection(false)
                .build(),
            new OtsImpairmentsBuilder()
                .setImpairmentRouteEntry(List.of(ire2))
                .setIngressDirection(true)
                .build()));
        OtsMediaConnectionEndPointSpec otsMCmCepSpecA = new OtsMediaConnectionEndPointSpecBuilder()
            .setOtsImpairments(otsImpairmentListA).build();
        OtsMediaConnectionEndPointSpec otsMCCepSpecZ = new OtsMediaConnectionEndPointSpecBuilder()
            .setOtsImpairments(otsImpairmentListZ).build();
        LOG.debug("LINKIMPL365 OtsMediaConnectionEndSpec for link {} on A end is {}",link.getLinkId(), otsMCmCepSpecA);
        LOG.debug("LINKIMPL366 OtsMediaConnectionEndSpec for link {} on Z end is {}}",link.getLinkId(), otsMCCepSpecZ);


        ORToTapiTopoConversionFactory tapiFactory = new ORToTapiTopoConversionFactory(new Uuid(UUID.nameUUIDFromBytes(
            TapiStringConstants.T0_FULL_MULTILAYER.getBytes(StandardCharsets.UTF_8)).toString()));

        String intermediateSupNodeId = getSupportingNodeFromNodeId(link.getSource().getSourceNode().getValue());
        String intermediateTp = link.getSource().getSourceTp().getValue();

        org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev221121.cep.list.ConnectionEndPoint
                cepNodeAots = tapiFactory.createCepRoadm(0, 0, String.join("+", intermediateSupNodeId,
            intermediateTp), TapiStringConstants.PHTNC_MEDIA_OTS, otsMCmCepSpecA);
        LOG.debug("TAPILINKIMPLLINE378 CepSpec is {}", otsMCmCepSpecA);
        LOG.debug("TAPILINKIMPLLINE379 Cep Node A OTS is {}", cepNodeAots);

        putRdmCepInTopoContextAndAddToCepList(intermediateSupNodeId, intermediateTp,
            TapiStringConstants.PHTNC_MEDIA_OTS, cepNodeAots);
        LOG.info("In TapiLinkImpl create Cep {} with otsCepSpec {}", cepNodeAots.getName(), otsMCmCepSpecA);
        org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev221121.cep.list.ConnectionEndPoint
                cepNodeAoms = tapiFactory.createCepRoadm(0, 0, String.join("+", intermediateSupNodeId,
            intermediateTp), TapiStringConstants.PHTNC_MEDIA_OMS, null);
        putRdmCepInTopoContextAndAddToCepList(intermediateSupNodeId, intermediateTp,
            TapiStringConstants.PHTNC_MEDIA_OMS, cepNodeAoms);
        LOG.info("In TapiLinkImpl create Cep {} ", cepNodeAoms.getName());
        intermediateSupNodeId = getSupportingNodeFromNodeId(link.getDestination().getDestNode().getValue());
        intermediateTp = link.getDestination().getDestTp().getValue();
        org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev221121.cep.list.ConnectionEndPoint
                cepNodeZots = tapiFactory.createCepRoadm(0, 0, String.join("+", intermediateSupNodeId,
            intermediateTp), TapiStringConstants.PHTNC_MEDIA_OTS, otsMCCepSpecZ);
        putRdmCepInTopoContextAndAddToCepList(intermediateSupNodeId, intermediateTp,
            TapiStringConstants.PHTNC_MEDIA_OTS, cepNodeZots);
        LOG.info("In TapiLinkImpl create Cep {} with otsCepSpec {}", cepNodeZots.getName(), otsMCCepSpecZ);
        org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev221121.cep.list.ConnectionEndPoint
                cepNodeZoms = tapiFactory.createCepRoadm(0, 0, String.join("+", intermediateSupNodeId,
            intermediateTp), TapiStringConstants.PHTNC_MEDIA_OMS, null);
        putRdmCepInTopoContextAndAddToCepList(intermediateSupNodeId, intermediateTp,
            TapiStringConstants.PHTNC_MEDIA_OMS, cepNodeZoms);
        LOG.info("In TapiLinkImpl create Cep {} ", cepNodeZoms.getName());

    }

    private String getSupportingNodeFromNodeId(String overlayNodeId) {
        DataObjectIdentifier<org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network
                .rev180226.networks.network.Node> nodeIID = DataObjectIdentifier.builder(
            org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network
                    .rev180226.Networks.class)
                .child(org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226
                    .networks.Network.class,
                    new NetworkKey(new NetworkId(NetworkUtils.OVERLAY_NETWORK_ID)))
                .child(org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226
                    .networks.network.Node.class,
                        new org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.networks
                        .network.NodeKey(new NodeId(overlayNodeId)))
            .build();
        try {
            Optional<org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226
                .networks.network.Node> node = this.networkTransactionService.read(
                LogicalDatastoreType.CONFIGURATION, nodeIID).get();
            if (node.isEmpty()) {
                LOG.error("TAPILINKIMPL Node {} not present in the datastore", node);
                return null;
            }
            //return node
            return node.orElseThrow().getSupportingNode().entrySet().stream()
                .filter(supn -> supn.getKey().getNetworkRef().getValue().equals(NetworkUtils.UNDERLAY_NETWORK_ID))
                .findFirst().orElseThrow().getKey().getNodeRef().getValue();
        } catch (InterruptedException | ExecutionException e) {
            LOG.error("TAPILINKIMPL Failed getting Mapping data from portMapping",e);
        }
        return null;
    }

    @Override
    public AdministrativeState setTapiAdminState(String adminState) {
        if (adminState == null) {
            return null;
        }
        return adminState.equals(AdminStates.InService.getName())
            || adminState.equals(AdministrativeState.UNLOCKED.getName()) ? AdministrativeState.UNLOCKED
                : AdministrativeState.LOCKED;
    }

    @Override
    public AdministrativeState setTapiAdminState(AdminStates adminState1, AdminStates adminState2) {
        if (adminState1 == null || adminState2 == null) {
            return null;
        }
        LOG.info("Admin state 1 = {}, andmin state 2 = {}", adminState1.getName(), adminState2.getName());
        return AdminStates.InService.equals(adminState1) && AdminStates.InService.equals(adminState2)
            ? AdministrativeState.UNLOCKED : AdministrativeState.LOCKED;
    }

    @Override
    public OperationalState setTapiOperationalState(String operState) {
        if (operState == null) {
            return null;
        }
        return operState.equals("inService") || operState.equals(OperationalState.ENABLED.getName())
            ? OperationalState.ENABLED : OperationalState.DISABLED;
    }

    @Override
    public OperationalState setTapiOperationalState(State operState1, State operState2) {
        if (operState1 == null || operState2 == null) {
            return null;
        }
        return State.InService.equals(operState1) && State.InService.equals(operState2)
            ? OperationalState.ENABLED : OperationalState.DISABLED;
    }

    @Override
    public Map<Map<String, String>, org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev221121.cep.list
            .ConnectionEndPoint> getCepMap() {
        return this.cepMap;
    }

    @Override
    public String getOperState(String srcNodeId, String destNodeId, String sourceTpId, String destTpId) {
        Uuid tapiTopoUuid = new Uuid(UUID.nameUUIDFromBytes(
                TapiStringConstants.T0_FULL_MULTILAYER.getBytes(Charset.forName("UTF-8")))
            .toString());
        Uuid nepUuid = new Uuid(UUID.nameUUIDFromBytes(
                String.join("+", srcNodeId, TapiStringConstants.PHTNC_MEDIA_OTS, sourceTpId)
                    .getBytes(Charset.forName("UTF-8")))
            .toString());
        try {
            Optional<OwnedNodeEdgePoint> optionalOnep = this.networkTransactionService.read(
                    LogicalDatastoreType.OPERATIONAL,
                    DataObjectIdentifier.builder(Context.class)
                        .augmentation(Context1.class)
                        .child(TopologyContext.class)
                        .child(Topology.class, new TopologyKey(tapiTopoUuid))
                        .child(Node.class, new NodeKey(
                            new Uuid(UUID.nameUUIDFromBytes(
                                    String.join("+", srcNodeId, TapiStringConstants.PHTNC_MEDIA)
                                        .getBytes(Charset.forName("UTF-8")))
                                .toString())))
                        .child(OwnedNodeEdgePoint.class, new OwnedNodeEdgePointKey(nepUuid))
                        .build())
                .get();
            if (optionalOnep.isEmpty()) {
                LOG.error("Nep {} does not exist in the datastore", nepUuid);
                return null;
            }
            Uuid nep1Uuid = new Uuid(UUID.nameUUIDFromBytes(
                    String.join("+", destNodeId, TapiStringConstants.PHTNC_MEDIA_OTS, destTpId)
                        .getBytes(Charset.forName("UTF-8")))
                .toString());
            Optional<OwnedNodeEdgePoint> optionalOnep1 = this.networkTransactionService.read(
                    LogicalDatastoreType.OPERATIONAL,
                    DataObjectIdentifier.builder(Context.class)
                        .augmentation(Context1.class)
                        .child(TopologyContext.class)
                        .child(Topology.class, new TopologyKey(tapiTopoUuid))
                        .child(Node.class, new NodeKey(
                            new Uuid(UUID.nameUUIDFromBytes(
                                    String.join("+", destNodeId, TapiStringConstants.PHTNC_MEDIA)
                                        .getBytes(Charset.forName("UTF-8")))
                                .toString())))
                        .child(OwnedNodeEdgePoint.class, new OwnedNodeEdgePointKey(nep1Uuid))
                        .build())
                .get();
            if (optionalOnep1.isEmpty()) {
                LOG.error("Nep {} does not exist in the datastore", nep1Uuid);
                return null;
            }
            OperationalState onepOperState = optionalOnep.orElseThrow().getOperationalState();
            return onepOperState.equals(optionalOnep1.orElseThrow().getOperationalState())
                ? onepOperState.getName() : OperationalState.DISABLED.getName();
        } catch (InterruptedException | ExecutionException e) {
            LOG.error("Failed getting Mapping data from portMapping",e);
            return null;
        }
    }

    @Override
    public String getAdminState(String srcNodeId, String destNodeId, String sourceTpId, String destTpId) {
        Uuid tapiTopoUuid = new Uuid(UUID.nameUUIDFromBytes(
                TapiStringConstants.T0_FULL_MULTILAYER.getBytes(Charset.forName("UTF-8")))
            .toString());
        Uuid nepUuid = new Uuid(UUID.nameUUIDFromBytes(
                String.join("+", srcNodeId, TapiStringConstants.PHTNC_MEDIA_OTS, sourceTpId)
                    .getBytes(Charset.forName("UTF-8")))
            .toString());
        try {
            Optional<OwnedNodeEdgePoint> optionalOnep = this.networkTransactionService.read(
                    LogicalDatastoreType.OPERATIONAL,
                    DataObjectIdentifier.builder(Context.class)
                        .augmentation(Context1.class)
                        .child(TopologyContext.class)
                        .child(Topology.class, new TopologyKey(tapiTopoUuid))
                        .child(Node.class, new NodeKey(
                            //nodeUuid
                            new Uuid(UUID.nameUUIDFromBytes(
                                    String.join("+", srcNodeId, TapiStringConstants.PHTNC_MEDIA)
                                        .getBytes(Charset.forName("UTF-8")))
                                .toString())))
                        .child(OwnedNodeEdgePoint.class, new OwnedNodeEdgePointKey(nepUuid))
                        .build())
                .get();
            if (optionalOnep.isEmpty()) {
                LOG.error("Nep {} does not exist in the datastore", nepUuid);
                return null;
            }
            Uuid nep1Uuid = new Uuid(UUID.nameUUIDFromBytes(
                    String.join("+", destNodeId, TapiStringConstants.PHTNC_MEDIA_OTS, destTpId)
                        .getBytes(Charset.forName("UTF-8")))
                .toString());
            Optional<OwnedNodeEdgePoint> optionalOnep1 = this.networkTransactionService.read(
                    LogicalDatastoreType.OPERATIONAL,
                    DataObjectIdentifier.builder(Context.class)
                        .augmentation(Context1.class)
                        .child(TopologyContext.class)
                        .child(Topology.class, new TopologyKey(tapiTopoUuid))
                        .child(Node.class, new NodeKey(
                            //node1Uuid
                            new Uuid(UUID.nameUUIDFromBytes(
                                    String.join("+", destNodeId, TapiStringConstants.PHTNC_MEDIA)
                                        .getBytes(Charset.forName("UTF-8")))
                                .toString())))
                        .child(OwnedNodeEdgePoint.class, new OwnedNodeEdgePointKey(nep1Uuid))
                        .build())
                .get();
            if (optionalOnep1.isEmpty()) {
                LOG.error("Nep {} does not exist in the datastore", nep1Uuid);
                return null;
            }
            AdministrativeState onepAdminState = optionalOnep.orElseThrow().getAdministrativeState();
            return onepAdminState.equals(optionalOnep1.orElseThrow().getAdministrativeState())
                ? onepAdminState.getName() : AdministrativeState.UNLOCKED.getName();
        } catch (InterruptedException | ExecutionException e) {
            LOG.error("Failed getting Mapping data from portMapping",e);
            return null;
        }
    }

    public void putRdmCepInTopoContextAndAddToCepList(String nodeId, String tpId, String qual,
            org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev221121.cep.list.ConnectionEndPoint
            cep) {
        LOG.debug("TAPILINKIMPLLINE566 nodeId {}, tpId {}, qual {}", nodeId, tpId, qual);
        String nepId = String.join("+", nodeId, qual, tpId);
        String nodeNepId = String.join("+", nodeId, TapiStringConstants.PHTNC_MEDIA);
        var uuidMap = new HashMap<>(Map.of(
            new Uuid(UUID.nameUUIDFromBytes(nepId.getBytes(StandardCharsets.UTF_8)).toString()).toString(),
            new Uuid(UUID.nameUUIDFromBytes(nodeNepId.getBytes(StandardCharsets.UTF_8)).toString()).toString()));

        LOG.debug("TAPILINKIMPL569, CEP is {}", cep);
        this.cepMap.put(uuidMap, cep);
        LOG.debug("TAPILINKIMPL570, Before calling TapiContext.updateTopologyWith CEP cepMap is {}", this.cepMap);
        tapiContext.updateTopologyWithCep(
            //TopoUuid
            new Uuid(UUID.nameUUIDFromBytes(
                TapiStringConstants.T0_FULL_MULTILAYER.getBytes(StandardCharsets.UTF_8)).toString()),
            //nodeUuid,
            new Uuid(UUID.nameUUIDFromBytes(nodeNepId.getBytes(StandardCharsets.UTF_8)).toString()),
            //nepUuid,
            new Uuid(UUID.nameUUIDFromBytes(nepId.getBytes(StandardCharsets.UTF_8)).toString()), cep);
    }

}
