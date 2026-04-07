/*
 * Copyright © 2026 Smartoptics and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.tapi.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opendaylight.transportpce.common.InstanceIdentifiers;
import org.opendaylight.transportpce.common.network.NetworkTransactionImpl;
import org.opendaylight.transportpce.common.network.NetworkTransactionService;
import org.opendaylight.transportpce.tapi.TapiConstants;
import org.opendaylight.transportpce.test.AbstractTest;
import org.opendaylight.transportpce.test.utils.TopologyDataUtils;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.AdministrativeState;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.CAPACITYUNITGBPS;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.ForwardingDirection;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.LayerProtocolName;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.LifecycleState;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.OperationalState;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.Uuid;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.capacity.TotalSizeBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.capacity.pac.AvailableCapacityBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.capacity.pac.TotalPotentialCapacityBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.global._class.Name;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.global._class.NameBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.ProtectionType;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.RestorationPolicy;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.link.NodeEdgePoint;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.link.NodeEdgePointBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.link.ResilienceTypeBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.risk.parameter.pac.RiskCharacteristic;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.risk.parameter.pac.RiskCharacteristicBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.topology.Link;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.topology.LinkBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.transfer.cost.pac.CostCharacteristic;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.transfer.cost.pac.CostCharacteristicBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.transfer.timing.pac.LatencyCharacteristic;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.transfer.timing.pac.LatencyCharacteristicBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.validation.pac.ValidationMechanism;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.validation.pac.ValidationMechanismBuilder;
import org.opendaylight.yangtools.yang.common.Decimal64;

class TapiLinkImplTest extends AbstractTest {

    private static final String SRC_NODE_ID = "ROADM-C1";
    private static final String SRC_TP_ID = "DEG1-TTP-TXRX";
    private static final String DST_NODE_ID = "ROADM-A1";
    private static final String DST_TP_ID = "DEG2-TTP-TXRX";

    private static final String NODE_QUAL = "PHOTONIC_MEDIA";
    private static final String TP_QUAL = "PHOTONIC_MEDIA_OTS";

    private static final String ADMIN_STATE = "UNLOCKED";
    private static final String OPER_STATE = "ENABLED";

    private static final Set<LayerProtocolName> LAYER_PROTOCOLS =
            Set.of(LayerProtocolName.PHOTONICMEDIA);

    private static final Set<String> TRANS_LAYER_PROTOCOLS =
            Set.of(String.valueOf(LayerProtocolName.PHOTONICMEDIA));

    private static final Uuid TOPOLOGY_UUID =
            new Uuid(UUID.nameUUIDFromBytes(
                    "393f09a4-0a0b-3d82-a4f6-1fbbc14ca1a7".getBytes(StandardCharsets.UTF_8)).toString());

    private static final Uuid OTN_TOPOLOGY_UUID =
            new Uuid("747c670e-7a07-3dab-b379-5b1cd17402a3");

    private NetworkTransactionService networkTransactionService;
    private TapiLinkImpl tapiLinkImpl;

    @BeforeEach
    void setUp() throws ExecutionException, InterruptedException {
        TopologyDataUtils.writeTopologyFromFileToDatastore(
                getDataStoreContextUtil(),
                TapiTopologyDataUtils.OPENROADM_TOPOLOGY_FILE,
                InstanceIdentifiers.OPENROADM_TOPOLOGY_II);

        TopologyDataUtils.writeTopologyFromFileToDatastore(
                getDataStoreContextUtil(),
                TapiTopologyDataUtils.OPENROADM_NETWORK_FILE,
                InstanceIdentifiers.OPENROADM_NETWORK_II);

        TopologyDataUtils.writeTopologyFromFileToDatastore(
                getDataStoreContextUtil(),
                TapiTopologyDataUtils.OTN_TOPOLOGY_FILE,
                InstanceIdentifiers.OTN_NETWORK_II);

        TopologyDataUtils.writePortmappingFromFileToDatastore(
                getDataStoreContextUtil(),
                TapiTopologyDataUtils.PORTMAPPING_FILE);

        networkTransactionService = new NetworkTransactionImpl(getDataBroker());
        tapiLinkImpl = new TapiLinkImpl(
                networkTransactionService,
                new TapiContext(networkTransactionService));
    }

    @Test
    void createTapiLink_shouldCreateOmsRoadmRoadmLink() {
        Link expected = buildDefaultExpectedLink(TapiConstants.VALUE_NAME_OMS_RDM_RDM_LINK);

        Link actual = createDefaultTapiLink(TapiConstants.OMS_RDM_RDM_LINK);

        assertEquals(expected, actual);
    }

    @Test
    void createTapiLink_shouldCreateTransitionalLink() {
        Link expected = buildDefaultExpectedLink("transitional link name");

        Link actual = createDefaultTapiLink(TapiConstants.TRANSITIONAL_LINK);

        assertEquals(expected, actual);
    }

    @Test
    void createTapiLink_shouldCreateOmsXpdrRdmLink() {
        Link expected = buildDefaultExpectedLink("XPDR-RDM link name");

        Link actual = createDefaultTapiLink(TapiConstants.OMS_XPDR_RDM_LINK);

        assertEquals(expected, actual);
    }

    @Test
    void createTapiLink_shouldCreateOtnXpdrXpdrLink() {
        Link expected = buildDefaultExpectedLink(TapiConstants.VALUE_NAME_OTN_XPDR_XPDR_LINK);

        Link actual = createDefaultTapiLink(TapiConstants.OTN_XPDR_XPDR_LINK);

        assertEquals(expected, actual);
    }

    @Test
    void createTapiLink_shouldReturnNullForUnknownLinkType() {
        Link actual = createDefaultTapiLink("unknown-link-type");

        assertNull(actual);
    }

    @Test
    void createTapiLink_shouldCreateXpdrRdmLink_roadmC1_pp4_to_spdrSc1_xpdr2_network3() {
        Link expected = buildExpectedLink(
                "ROADM-C1",
                "SRG1-PP4-TXRX",
                "SPDR-SC1",
                "XPDR2-NETWORK3",
                "PHOTONIC_MEDIA",
                "XPONDER",
                "PHOTONIC_MEDIA_OTS",
                "PHOTONIC_MEDIA_OTS",
                "XPDR-RDM link name",
                "UNLOCKED",
                "ENABLED",
                Set.of(LayerProtocolName.PHOTONICMEDIA),
                TOPOLOGY_UUID);

        Link actual = createTapiLink(
                "ROADM-C1",
                "SRG1-PP4-TXRX",
                "SPDR-SC1",
                "XPDR2-NETWORK3",
                TapiConstants.OMS_XPDR_RDM_LINK,
                "PHOTONIC_MEDIA",
                "XPONDER",
                "PHOTONIC_MEDIA_OTS",
                "PHOTONIC_MEDIA_OTS",
                "UNLOCKED",
                "ENABLED",
                Set.of(LayerProtocolName.PHOTONICMEDIA),
                Set.of("PHOTONIC_MEDIA"),
                TOPOLOGY_UUID);

        assertEquals(expected, actual);
    }

    @Test
    void createTapiLink_shouldCreateXpdrRdmLink_roadmA1_pp1_to_xpdrA1_xpdr1_network1() {
        Link expected = buildExpectedLink(
                "ROADM-A1",
                "SRG1-PP1-TXRX",
                "XPDR-A1-XPDR1",
                "XPDR1-NETWORK1",
                "PHOTONIC_MEDIA",
                "XPONDER",
                "PHOTONIC_MEDIA_OTS",
                "PHOTONIC_MEDIA_OTS",
                "XPDR-RDM link name",
                "UNLOCKED",
                "ENABLED",
                Set.of(LayerProtocolName.PHOTONICMEDIA),
                TOPOLOGY_UUID);

        Link actual = createTapiLink(
                "ROADM-A1",
                "SRG1-PP1-TXRX",
                "XPDR-A1-XPDR1",
                "XPDR1-NETWORK1",
                TapiConstants.OMS_XPDR_RDM_LINK,
                "PHOTONIC_MEDIA",
                "XPONDER",
                "PHOTONIC_MEDIA_OTS",
                "PHOTONIC_MEDIA_OTS",
                "UNLOCKED",
                "ENABLED",
                Set.of(LayerProtocolName.PHOTONICMEDIA),
                Set.of("PHOTONIC_MEDIA"),
                TOPOLOGY_UUID);

        assertEquals(expected, actual);
    }

    @Test
    void createTapiLink_shouldCreateXpdrRdmLink_roadmA1_pp2_to_spdrSa1_xpdr1_network1() {
        Link expected = buildExpectedLink(
                "ROADM-A1",
                "SRG1-PP2-TXRX",
                "SPDR-SA1-XPDR1",
                "XPDR1-NETWORK1",
                "PHOTONIC_MEDIA",
                "XPONDER",
                "PHOTONIC_MEDIA_OTS",
                "PHOTONIC_MEDIA_OTS",
                "XPDR-RDM link name",
                "UNLOCKED",
                "ENABLED",
                Set.of(LayerProtocolName.PHOTONICMEDIA),
                TOPOLOGY_UUID);

        Link actual = createTapiLink(
                "ROADM-A1",
                "SRG1-PP2-TXRX",
                "SPDR-SA1-XPDR1",
                "XPDR1-NETWORK1",
                TapiConstants.OMS_XPDR_RDM_LINK,
                "PHOTONIC_MEDIA",
                "XPONDER",
                "PHOTONIC_MEDIA_OTS",
                "PHOTONIC_MEDIA_OTS",
                "UNLOCKED",
                "ENABLED",
                Set.of(LayerProtocolName.PHOTONICMEDIA),
                Set.of("PHOTONIC_MEDIA"),
                TOPOLOGY_UUID);

        assertEquals(expected, actual);
    }

    @Test
    void createTapiLink_shouldCreateXpdrRdmLink_roadmC1_pp1_to_xpdrC1_xpdr1_network1() {
        Link expected = buildExpectedLink(
                "ROADM-C1",
                "SRG1-PP1-TXRX",
                "XPDR-C1-XPDR1",
                "XPDR1-NETWORK1",
                "PHOTONIC_MEDIA",
                "XPONDER",
                "PHOTONIC_MEDIA_OTS",
                "PHOTONIC_MEDIA_OTS",
                "XPDR-RDM link name",
                "UNLOCKED",
                "ENABLED",
                Set.of(LayerProtocolName.PHOTONICMEDIA),
                TOPOLOGY_UUID);

        Link actual = createTapiLink(
                "ROADM-C1",
                "SRG1-PP1-TXRX",
                "XPDR-C1-XPDR1",
                "XPDR1-NETWORK1",
                TapiConstants.OMS_XPDR_RDM_LINK,
                "PHOTONIC_MEDIA",
                "XPONDER",
                "PHOTONIC_MEDIA_OTS",
                "PHOTONIC_MEDIA_OTS",
                "UNLOCKED",
                "ENABLED",
                Set.of(LayerProtocolName.PHOTONICMEDIA),
                Set.of("PHOTONIC_MEDIA"),
                TOPOLOGY_UUID);

        assertEquals(expected, actual);
    }

    @Test
    void createTapiLink_shouldCreateXpdrRdmLink_roadmA1_pp3_to_spdrSa1_xpdr2_network2() {
        Link expected = buildExpectedLink(
                "ROADM-A1",
                "SRG1-PP3-TXRX",
                "SPDR-SA1-XPDR2",
                "XPDR2-NETWORK2",
                "PHOTONIC_MEDIA",
                "XPONDER",
                "PHOTONIC_MEDIA_OTS",
                "PHOTONIC_MEDIA_OTS",
                "XPDR-RDM link name",
                "UNLOCKED",
                "ENABLED",
                Set.of(LayerProtocolName.PHOTONICMEDIA),
                TOPOLOGY_UUID);

        Link actual = createTapiLink(
                "ROADM-A1",
                "SRG1-PP3-TXRX",
                "SPDR-SA1-XPDR2",
                "XPDR2-NETWORK2",
                TapiConstants.OMS_XPDR_RDM_LINK,
                "PHOTONIC_MEDIA",
                "XPONDER",
                "PHOTONIC_MEDIA_OTS",
                "PHOTONIC_MEDIA_OTS",
                "UNLOCKED",
                "ENABLED",
                Set.of(LayerProtocolName.PHOTONICMEDIA),
                Set.of("PHOTONIC_MEDIA"),
                TOPOLOGY_UUID);

        assertEquals(expected, actual);
    }

    @Test
    void createTapiLink_shouldCreateXpdrRdmLink_roadmC1_pp2_to_spdrSc1_xpdr1_network1() {
        Link expected = buildExpectedLink(
                "ROADM-C1",
                "SRG1-PP2-TXRX",
                "SPDR-SC1-XPDR1",
                "XPDR1-NETWORK1",
                "PHOTONIC_MEDIA",
                "XPONDER",
                "PHOTONIC_MEDIA_OTS",
                "PHOTONIC_MEDIA_OTS",
                "XPDR-RDM link name",
                "UNLOCKED",
                "ENABLED",
                Set.of(LayerProtocolName.PHOTONICMEDIA),
                TOPOLOGY_UUID);

        Link actual = createTapiLink(
                "ROADM-C1",
                "SRG1-PP2-TXRX",
                "SPDR-SC1-XPDR1",
                "XPDR1-NETWORK1",
                TapiConstants.OMS_XPDR_RDM_LINK,
                "PHOTONIC_MEDIA",
                "XPONDER",
                "PHOTONIC_MEDIA_OTS",
                "PHOTONIC_MEDIA_OTS",
                "UNLOCKED",
                "ENABLED",
                Set.of(LayerProtocolName.PHOTONICMEDIA),
                Set.of("PHOTONIC_MEDIA"),
                TOPOLOGY_UUID);

        assertEquals(expected, actual);
    }

    @Test
    void createTapiLink_shouldCreateXpdrRdmLink_roadmC1_pp3_to_spdrSc1_xpdr2_network2() {
        Link expected = buildExpectedLink(
                "ROADM-C1",
                "SRG1-PP3-TXRX",
                "SPDR-SC1-XPDR2",
                "XPDR2-NETWORK2",
                "PHOTONIC_MEDIA",
                "XPONDER",
                "PHOTONIC_MEDIA_OTS",
                "PHOTONIC_MEDIA_OTS",
                "XPDR-RDM link name",
                "UNLOCKED",
                "ENABLED",
                Set.of(LayerProtocolName.PHOTONICMEDIA),
                TOPOLOGY_UUID);

        Link actual = createTapiLink(
                "ROADM-C1",
                "SRG1-PP3-TXRX",
                "SPDR-SC1-XPDR2",
                "XPDR2-NETWORK2",
                TapiConstants.OMS_XPDR_RDM_LINK,
                "PHOTONIC_MEDIA",
                "XPONDER",
                "PHOTONIC_MEDIA_OTS",
                "PHOTONIC_MEDIA_OTS",
                "UNLOCKED",
                "ENABLED",
                Set.of(LayerProtocolName.PHOTONICMEDIA),
                Set.of("PHOTONIC_MEDIA"),
                TOPOLOGY_UUID);

        assertEquals(expected, actual);
    }

    @Test
    void createTapiLink_shouldCreateXpdrRdmLink_roadmA1_pp4_to_spdrSa1_xpdr2_network3() {
        Link expected = buildExpectedLink(
                "ROADM-A1",
                "SRG1-PP4-TXRX",
                "SPDR-SA1-XPDR2",
                "XPDR2-NETWORK3",
                "PHOTONIC_MEDIA",
                "XPONDER",
                "PHOTONIC_MEDIA_OTS",
                "PHOTONIC_MEDIA_OTS",
                "XPDR-RDM link name",
                "UNLOCKED",
                "ENABLED",
                Set.of(LayerProtocolName.PHOTONICMEDIA),
                TOPOLOGY_UUID);

        Link actual = createTapiLink(
                "ROADM-A1",
                "SRG1-PP4-TXRX",
                "SPDR-SA1-XPDR2",
                "XPDR2-NETWORK3",
                TapiConstants.OMS_XPDR_RDM_LINK,
                "PHOTONIC_MEDIA",
                "XPONDER",
                "PHOTONIC_MEDIA_OTS",
                "PHOTONIC_MEDIA_OTS",
                "UNLOCKED",
                "ENABLED",
                Set.of(LayerProtocolName.PHOTONICMEDIA),
                Set.of("PHOTONIC_MEDIA"),
                TOPOLOGY_UUID);

        assertEquals(expected, actual);
    }

    @Test
    void createTapiLink_shouldCreateOtnXpdrXpdrLink_forEodu() {
        Link expected = buildExpectedLink(
                "SPDR-SA1-XPDR1",
                "XPDR1-NETWORK1",
                "SPDR-SC1-XPDR1",
                "XPDR1-NETWORK1",
                "XPONDER",
                "XPONDER",
                "eODU",
                "eODU",
                TapiConstants.VALUE_NAME_OTN_XPDR_XPDR_LINK,
                "UNLOCKED",
                "ENABLED",
                Set.of(LayerProtocolName.ODU),
                OTN_TOPOLOGY_UUID);

        Link actual = createTapiLink(
                "SPDR-SA1-XPDR1",
                "XPDR1-NETWORK1",
                "SPDR-SC1-XPDR1",
                "XPDR1-NETWORK1",
                TapiConstants.OTN_XPDR_XPDR_LINK,
                "XPONDER",
                "XPONDER",
                "eODU",
                "eODU",
                "UNLOCKED",
                "ENABLED",
                Set.of(LayerProtocolName.ODU),
                Set.of("ODU"),
                OTN_TOPOLOGY_UUID);

        assertEquals(expected, actual);
    }

    @Test
    void createTapiLink_shouldCreateOtnXpdrXpdrLink_forIotsi() {
        Link expected = buildExpectedLink(
                "SPDR-SA1-XPDR1",
                "XPDR1-NETWORK1",
                "SPDR-SC1-XPDR1",
                "XPDR1-NETWORK1",
                "XPONDER",
                "XPONDER",
                "iOTSi",
                "iOTSi",
                TapiConstants.VALUE_NAME_OTN_XPDR_XPDR_LINK,
                "UNLOCKED",
                "ENABLED",
                Set.of(LayerProtocolName.PHOTONICMEDIA),
                OTN_TOPOLOGY_UUID);

        Link actual = createTapiLink(
                "SPDR-SA1-XPDR1",
                "XPDR1-NETWORK1",
                "SPDR-SC1-XPDR1",
                "XPDR1-NETWORK1",
                TapiConstants.OTN_XPDR_XPDR_LINK,
                "XPONDER",
                "XPONDER",
                "iOTSi",
                "iOTSi",
                "UNLOCKED",
                "ENABLED",
                Set.of(LayerProtocolName.PHOTONICMEDIA),
                Set.of("PHOTONIC_MEDIA"),
                OTN_TOPOLOGY_UUID);

        assertEquals(expected, actual);
    }

    @Test
    void createTapiLink_shouldCreateXpdrRdmLink_spdrSa1_xpdr1_network1_to_roadmA1_pp2() {
        Link expected = buildExpectedLink(
                "SPDR-SA1-XPDR1",
                "XPDR1-NETWORK1",
                "ROADM-A1",
                "SRG1-PP2-TXRX",
                "XPONDER",
                "PHOTONIC_MEDIA",
                "PHOTONIC_MEDIA_OTS",
                "PHOTONIC_MEDIA_OTS",
                "XPDR-RDM link name",
                "UNLOCKED",
                "ENABLED",
                Set.of(LayerProtocolName.PHOTONICMEDIA),
                TOPOLOGY_UUID);

        Link actual = createTapiLink(
                "SPDR-SA1-XPDR1",
                "XPDR1-NETWORK1",
                "ROADM-A1",
                "SRG1-PP2-TXRX",
                TapiConstants.OMS_XPDR_RDM_LINK,
                "XPONDER",
                "PHOTONIC_MEDIA",
                "PHOTONIC_MEDIA_OTS",
                "PHOTONIC_MEDIA_OTS",
                "UNLOCKED",
                "ENABLED",
                Set.of(LayerProtocolName.PHOTONICMEDIA),
                Set.of("PHOTONIC_MEDIA"),
                TOPOLOGY_UUID);

        assertEquals(expected, actual);
    }

    private Link createTapiLink(
            String srcNodeId,
            String srcTpId,
            String dstNodeId,
            String dstTpId,
            String linkType,
            String srcNodeQual,
            String dstNodeQual,
            String srcTpQual,
            String dstTpQual,
            String adminState,
            String operState,
            Set<LayerProtocolName> layerProtocols,
            Set<String> transLayerProtocols,
            Uuid topologyUuid) {

        return tapiLinkImpl.createTapiLink(
                srcNodeId,
                srcTpId,
                dstNodeId,
                dstTpId,
                linkType,
                srcNodeQual,
                dstNodeQual,
                srcTpQual,
                dstTpQual,
                adminState,
                operState,
                layerProtocols,
                transLayerProtocols,
                topologyUuid);
    }

    private AdministrativeState mapAdminState(String adminState) {
        return "UNLOCKED".equals(adminState)
                ? AdministrativeState.UNLOCKED
                : AdministrativeState.LOCKED;
    }

    private OperationalState mapOperationalState(String operState) {
        return "ENABLED".equals(operState)
                ? OperationalState.ENABLED
                : OperationalState.DISABLED;
    }

    private Link buildExpectedLink(
            String srcNodeId,
            String srcTpId,
            String dstNodeId,
            String dstTpId,
            String srcNodeQual,
            String dstNodeQual,
            String srcTpQual,
            String dstTpQual,
            String valueName,
            String adminState,
            String operState,
            Set<LayerProtocolName> layerProtocols,
            Uuid topologyUuid) {

        String sourceNepKey = String.join("+", srcNodeId, srcTpQual, srcTpId);
        String destNepKey = String.join("+", dstNodeId, dstTpQual, dstTpId);
        String linkKey = String.join("to", sourceNepKey, destNepKey);

        NodeEdgePoint sourceNep = buildNodeEdgePoint(srcNodeId, srcNodeQual, sourceNepKey, topologyUuid);
        NodeEdgePoint destNep = buildNodeEdgePoint(dstNodeId, dstNodeQual, destNepKey, topologyUuid);

        Name linkName = new NameBuilder()
                .setValueName(valueName)
                .setValue(linkKey)
                .build();

        CostCharacteristic costCharacteristic = new CostCharacteristicBuilder()
                .setCostAlgorithm("Restricted Shortest Path - RSP")
                .setCostName("HOP_COUNT")
                .setCostValue(TapiConstants.COST_HOP_VALUE)
                .build();

        LatencyCharacteristic latencyCharacteristic = new LatencyCharacteristicBuilder()
                .setFixedLatencyCharacteristic(TapiConstants.FIXED_LATENCY_VALUE)
                .setQueuingLatencyCharacteristic(TapiConstants.QUEING_LATENCY_VALUE)
                .setJitterCharacteristic(TapiConstants.JITTER_VALUE)
                .setWanderCharacteristic(TapiConstants.WANDER_VALUE)
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

        return new LinkBuilder()
                .setUuid(new Uuid(UUID.nameUUIDFromBytes(linkKey.getBytes(StandardCharsets.UTF_8)).toString()))
                .setName(Map.of(linkName.key(), linkName))
                .setTransitionedLayerProtocolName(Set.of(
                        TapiConstants.PHTNC_MEDIA_OMS,
                        TapiConstants.PHTNC_MEDIA_OTS))
                .setLayerProtocolName(layerProtocols)
                .setNodeEdgePoint(new HashMap<>(Map.of(
                        sourceNep.key(), sourceNep,
                        destNep.key(), destNep)))
                .setDirection(ForwardingDirection.BIDIRECTIONAL)
                .setAvailableCapacity(new AvailableCapacityBuilder()
                        .setTotalSize(new TotalSizeBuilder()
                                .setUnit(CAPACITYUNITGBPS.VALUE)
                                .setValue(Decimal64.valueOf("100"))
                                .build())
                        .build())
                .setResilienceType(new ResilienceTypeBuilder()
                        .setProtectionType(ProtectionType.NOPROTECTION)
                        .setRestorationPolicy(RestorationPolicy.NA)
                        .build())
                .setAdministrativeState(mapAdminState(adminState))
                .setOperationalState(mapOperationalState(operState))
                .setLifecycleState(LifecycleState.INSTALLED)
                .setTotalPotentialCapacity(new TotalPotentialCapacityBuilder()
                        .setTotalSize(new TotalSizeBuilder()
                                .setUnit(CAPACITYUNITGBPS.VALUE)
                                .setValue(Decimal64.valueOf("100"))
                                .build())
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

    private NodeEdgePoint buildNodeEdgePoint(String nodeId, String nodeQual, String nepKey, Uuid topologyUuid) {
        return new NodeEdgePointBuilder()
                .setTopologyUuid(topologyUuid)
                .setNodeUuid(new Uuid(UUID.nameUUIDFromBytes(
                        String.join("+", nodeId, nodeQual).getBytes(StandardCharsets.UTF_8)).toString()))
                .setNodeEdgePointUuid(new Uuid(UUID.nameUUIDFromBytes(
                        nepKey.getBytes(StandardCharsets.UTF_8)).toString()))
                .build();
    }

    private Link createDefaultTapiLink(String linkType) {
        return createTapiLink(
                SRC_NODE_ID,
                SRC_TP_ID,
                DST_NODE_ID,
                DST_TP_ID,
                linkType,
                NODE_QUAL,
                NODE_QUAL,
                TP_QUAL,
                TP_QUAL,
                ADMIN_STATE,
                OPER_STATE,
                LAYER_PROTOCOLS,
                TRANS_LAYER_PROTOCOLS,
                TOPOLOGY_UUID);
    }

    private Link buildDefaultExpectedLink(String valueName) {
        return buildExpectedLink(
                SRC_NODE_ID,
                SRC_TP_ID,
                DST_NODE_ID,
                DST_TP_ID,
                NODE_QUAL,
                NODE_QUAL,
                TP_QUAL,
                TP_QUAL,
                valueName,
                ADMIN_STATE,
                OPER_STATE,
                LAYER_PROTOCOLS,
                TOPOLOGY_UUID);
    }
}
