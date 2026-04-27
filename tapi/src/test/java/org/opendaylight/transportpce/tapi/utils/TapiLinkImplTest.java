/*
 * Copyright © 2026 Smartoptics and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.tapi.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import com.google.common.util.concurrent.FluentFuture;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.transportpce.common.InstanceIdentifiers;
import org.opendaylight.transportpce.common.network.NetworkTransactionImpl;
import org.opendaylight.transportpce.common.network.NetworkTransactionService;
import org.opendaylight.transportpce.tapi.TapiConstants;
import org.opendaylight.transportpce.tapi.openroadm.topology.link.OpenRoadmLinkTerminationPointsFactory;
import org.opendaylight.transportpce.tapi.openroadm.topology.terminationpoint.mapping.TopologyTerminationPointTypeResolver;
import org.opendaylight.transportpce.tapi.topology.TapiTopologyException;
import org.opendaylight.transportpce.tapi.topology.TopologyUtils;
import org.opendaylight.transportpce.test.AbstractTest;
import org.opendaylight.transportpce.test.utils.TopologyDataUtils;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.networkutils.rev250902.OtnLinkType;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev250110.Link1Builder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.state.types.rev191129.State;
import org.opendaylight.yang.gen.v1.http.org.openroadm.equipment.states.types.rev191129.AdminStates;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.types.rev250110.OpenroadmLinkType;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.NodeId;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.networks.Network;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.LinkId;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.TpId;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.networks.network.LinkKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.networks.network.link.DestinationBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.networks.network.link.SourceBuilder;
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

    private static final String NODE_QUAL = "PHOTONIC_MEDIA";
    private static final String TP_QUAL = "PHOTONIC_MEDIA_OTS";

    private static final String ADMIN_STATE = "UNLOCKED";
    private static final String OPER_STATE = "ENABLED";

    private static final Set<LayerProtocolName> LAYER_PROTOCOLS =
            Set.of(LayerProtocolName.PHOTONICMEDIA);

    private static final Set<String> TRANS_LAYER_PROTOCOLS =
            Set.of(LayerProtocolName.PHOTONICMEDIA.getName());

    private static final Uuid TOPOLOGY_UUID =
            new Uuid(UUID.nameUUIDFromBytes(
                    "393f09a4-0a0b-3d82-a4f6-1fbbc14ca1a7".getBytes(StandardCharsets.UTF_8)).toString());

    private static final Uuid OTN_TOPOLOGY_UUID =
            new Uuid("747c670e-7a07-3dab-b379-5b1cd17402a3");

    private NetworkTransactionService networkTransactionService;
    private TapiLinkImpl tapiLinkImpl;
    private TopologyUtils topologyUtils;

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

        topologyUtils = new TopologyUtils(networkTransactionService, getDataBroker(), tapiLinkImpl);
    }

    @Test
    void createTapiLink_shouldCreateOmsRoadmRoadmLink() {
        Link expected = buildExpectedLink(
                "ROADM-C1",
                "DEG1-TTP-TXRX",
                "ROADM-A1",
                "DEG2-TTP-TXRX",
                TapiConstants.PHTNC_MEDIA,
                TapiConstants.PHTNC_MEDIA,
                TP_QUAL,
                TP_QUAL,
                TapiConstants.VALUE_NAME_OMS_RDM_RDM_LINK,
                ADMIN_STATE,
                OPER_STATE,
                LAYER_PROTOCOLS,
                TOPOLOGY_UUID);

        Link actual = createDefaultTapiLink(
                "ROADM-C1",
                "DEG1-TTP-TXRX",
                "ROADM-A1",
                "DEG2-TTP-TXRX",
                TapiConstants.OMS_RDM_RDM_LINK);

        assertEquals(expected, actual);

        var equivalentInput = orLink(
                "ROADM-C1-DEG1",
                "DEG1-TTP-TXRX",
                "ROADM-A1-DEG2",
                "DEG2-TTP-TXRX",
                OpenroadmLinkType.ROADMTOROADM);

        assertEquals(
                tapiLinkImpl.createTapiLink(
                        equivalentInput,
                        readOpenRoadmTopology(),
                        TOPOLOGY_UUID,
                        new OpenRoadmLinkTerminationPointsFactory(new TopologyTerminationPointTypeResolver())),
                actual);
    }

    @Test
    void createTapiLink_shouldCreateOmsXpdrRdmLink() {
        Link expected = buildExpectedLink(
                "XPDR-C1-XPDR1",
                "XPDR1-NETWORK1",
                "ROADM-C1",
                "SRG1-PP1-TXRX",
                TapiConstants.XPDR,
                TapiConstants.PHTNC_MEDIA,
                TP_QUAL,
                TP_QUAL,
                "XPDR-RDM link name",
                ADMIN_STATE,
                OPER_STATE,
                LAYER_PROTOCOLS,
                TOPOLOGY_UUID);

        Link actual = createTapiLink(
                "XPDR-C1-XPDR1",
                "XPDR1-NETWORK1",
                "ROADM-C1",
                "SRG1-PP1-TXRX",
                TapiConstants.OMS_XPDR_RDM_LINK,
                TapiConstants.XPDR,
                TapiConstants.PHTNC_MEDIA,
                TP_QUAL,
                TP_QUAL,
                ADMIN_STATE,
                OPER_STATE,
                LAYER_PROTOCOLS,
                TRANS_LAYER_PROTOCOLS,
                TOPOLOGY_UUID);

        assertEquals(expected, actual);

        var equivalentInput = orLink(
                "XPDR-C1-XPDR1",
                "XPDR1-NETWORK1",
                "ROADM-C1-SRG1",
                "SRG1-PP1-TXRX",
                OpenroadmLinkType.XPONDEROUTPUT);

        assertEquals(
                tapiLinkImpl.createTapiLink(
                        equivalentInput,
                        readOpenRoadmTopology(),
                        TOPOLOGY_UUID,
                        new OpenRoadmLinkTerminationPointsFactory(new TopologyTerminationPointTypeResolver())),
                actual);
    }

    @Test
    void createTapiLink_shouldCreateOtnXpdrXpdrLink() {
        Link expected = buildExpectedLink(
                "SPDR-SC1-XPDR1",
                "XPDR1-NETWORK1",
                "SPDR-SA1-XPDR1",
                "XPDR1-NETWORK1",
                TapiConstants.XPDR,
                TapiConstants.XPDR,
                TapiConstants.ODU,
                TapiConstants.ODU,
                TapiConstants.VALUE_NAME_OTN_XPDR_XPDR_LINK,
                ADMIN_STATE,
                OPER_STATE,
                LAYER_PROTOCOLS,
                TOPOLOGY_UUID);

        Link actual = createTapiLink(
                "SPDR-SC1-XPDR1",
                "XPDR1-NETWORK1",
                "SPDR-SA1-XPDR1",
                "XPDR1-NETWORK1",
                TapiConstants.OTN_XPDR_XPDR_LINK,
                TapiConstants.XPDR,
                TapiConstants.XPDR,
                TapiConstants.ODU,
                TapiConstants.ODU,
                ADMIN_STATE,
                OPER_STATE,
                LAYER_PROTOCOLS,
                TRANS_LAYER_PROTOCOLS,
                TOPOLOGY_UUID);

        assertEquals(expected, actual);

        var equivalentInput = orOTNLink(
                "SPDR-SC1-XPDR1",
                "XPDR1-NETWORK1",
                "SPDR-SA1-XPDR1",
                "XPDR1-NETWORK1",
                OpenroadmLinkType.OTNLINK);

        assertEquals(
                expected,
                tapiLinkImpl.createTapiLink(
                        equivalentInput,
                        readOTNTopology(),
                        TOPOLOGY_UUID,
                        new OpenRoadmLinkTerminationPointsFactory(new TopologyTerminationPointTypeResolver())));
    }

    @Test
    void createTapiLink_shouldCreateOtu4XpdrXpdrLink() {
        Link expected = buildExpectedLink(
                "SPDR-SC1-XPDR1",
                "XPDR1-NETWORK1",
                "SPDR-SA1-XPDR1",
                "XPDR1-NETWORK1",
                TapiConstants.XPDR,
                TapiConstants.XPDR,
                TapiConstants.I_OTSI,
                TapiConstants.I_OTSI,
                TapiConstants.VALUE_NAME_OTN_XPDR_XPDR_LINK,
                ADMIN_STATE,
                OPER_STATE,
                LAYER_PROTOCOLS,
                TOPOLOGY_UUID);

        Link actual = createTapiLink(
                "SPDR-SC1-XPDR1",
                "XPDR1-NETWORK1",
                "SPDR-SA1-XPDR1",
                "XPDR1-NETWORK1",
                TapiConstants.OTN_XPDR_XPDR_LINK,
                TapiConstants.XPDR,
                TapiConstants.XPDR,
                TapiConstants.I_OTSI,
                TapiConstants.I_OTSI,
                ADMIN_STATE,
                OPER_STATE,
                LAYER_PROTOCOLS,
                TRANS_LAYER_PROTOCOLS,
                TOPOLOGY_UUID);

        assertEquals(expected, actual);

        var equivalentInput = orOTU4Link(
                "SPDR-SC1-XPDR1",
                "XPDR1-NETWORK1",
                "SPDR-SA1-XPDR1",
                "XPDR1-NETWORK1",
                OpenroadmLinkType.OTNLINK);

        assertEquals(
                expected,
                tapiLinkImpl.createTapiLink(
                        equivalentInput,
                        readOTNTopology(),
                        TOPOLOGY_UUID,
                        new OpenRoadmLinkTerminationPointsFactory(new TopologyTerminationPointTypeResolver())));
    }

    @Test
    void createTapiLink_shouldCreateODTU4XpdrXpdrLink() {

        Link expected = buildExpectedLink(
                "SPDR-SC1-XPDR1",
                "XPDR1-NETWORK1",
                "SPDR-SA1-XPDR1",
                "XPDR1-NETWORK1",
                TapiConstants.XPDR,
                TapiConstants.XPDR,
                TapiConstants.E_ODU,
                TapiConstants.E_ODU,
                "otn link name",
                ADMIN_STATE,
                OPER_STATE,
                Set.of(LayerProtocolName.ODU),
                TOPOLOGY_UUID);

        Link actual = createTapiLink(
                "SPDR-SC1-XPDR1",
                "XPDR1-NETWORK1",
                "SPDR-SA1-XPDR1",
                "XPDR1-NETWORK1",
                TapiConstants.OTN_XPDR_XPDR_LINK,
                TapiConstants.XPDR,
                TapiConstants.XPDR,
                TapiConstants.E_ODU,
                TapiConstants.E_ODU,
                ADMIN_STATE,
                OPER_STATE,
                Set.of(LayerProtocolName.ODU),
                TRANS_LAYER_PROTOCOLS,
                TOPOLOGY_UUID);

        assertEquals(expected, actual);

        var equivalentInput = orODTU4Link(
                "SPDR-SC1-XPDR1",
                "XPDR1-NETWORK1",
                "SPDR-SA1-XPDR1",
                "XPDR1-NETWORK1",
                OpenroadmLinkType.OTNLINK);

        assertEquals(
                expected,
                tapiLinkImpl.createTapiLink(
                        equivalentInput,
                        readOTNTopology(),
                        TOPOLOGY_UUID,
                        new OpenRoadmLinkTerminationPointsFactory(new TopologyTerminationPointTypeResolver())));
    }

    @Test
    void createTapiLink_shouldReturnNullForUnknownLinkType() {
        Link actual = createDefaultTapiLink(
                "ROADM-C1",
                "DEG1-TTP-TXRX",
                "ROADM-A1",
                "DEG2-TTP-TXRX",
                "unknown-link-type");

        assertNull(actual);
    }

    @Test
    void createTapiLink_shouldCreateXpdrRdmLink_roadmC1_pp4_to_spdrSc1_xpdr2_network3() {
        Link expected = buildExpectedLink(
                "ROADM-C1",
                "SRG1-PP4-TXRX",
                "SPDR-SC1-XPDR2",
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
                "SPDR-SC1-XPDR2",
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

        var equivalentInput = orLink(
                "ROADM-C1-SRG1",
                "SRG1-PP4-TXRX",
                "SPDR-SC1-XPDR2",
                "XPDR2-NETWORK3",
                OpenroadmLinkType.XPONDERINPUT);

        Link actualFromEquivalentInput = tapiLinkImpl.createTapiLink(
                equivalentInput,
                readOpenRoadmTopology(),
                TOPOLOGY_UUID,
                new OpenRoadmLinkTerminationPointsFactory(new TopologyTerminationPointTypeResolver())
        );

        assertEquals(actual, actualFromEquivalentInput);
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

        LinkId linkId = new LinkId("ROADM-A1-SRG1-SRG1-PP1-TXRXtoXPDR-A1-XPDR1-XPDR1-NETWORK1");
        LinkKey linkKey = new LinkKey(linkId);
        var link = new org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226
                .networks.network.LinkBuilder()
                .withKey(linkKey)
                .setLinkId(linkId)
                .setSource(
                        new SourceBuilder()
                                .setSourceNode(new NodeId("ROADM-A1-SRG1"))
                                .setSourceTp(new TpId("SRG1-PP1-TXRX"))
                                .build())
                .setDestination(
                        new DestinationBuilder()
                                .setDestNode(new NodeId("XPDR-A1-XPDR1"))
                                .setDestTp(new TpId("XPDR1-NETWORK1"))
                                .build())
                .addAugmentation(
                        new Link1Builder()
                                .setLinkType(OpenroadmLinkType.XPONDERINPUT)
                                .setOppositeLink(
                                        new LinkId("XPDR-A1-XPDR1-XPDR1-NETWORK1toROADM-A1-SRG1-SRG1-PP1-TXRX"))
                                .setOperationalState(State.InService)
                                .setAdministrativeState(AdminStates.InService)
                                .build()
                ).build();

        Link actualTwo = tapiLinkImpl.createTapiLink(
                link,
                readOpenRoadmTopology(),
                TOPOLOGY_UUID,
                new OpenRoadmLinkTerminationPointsFactory(new TopologyTerminationPointTypeResolver())
        );

        assertEquals(actual, actualTwo);
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

        var equivalentInput = orLink(
                "ROADM-A1-SRG1",
                "SRG1-PP2-TXRX",
                "SPDR-SA1-XPDR1",
                "XPDR1-NETWORK1",
                OpenroadmLinkType.XPONDERINPUT);

        Link actualFromEquivalentInput = tapiLinkImpl.createTapiLink(
                equivalentInput,
                readOpenRoadmTopology(),
                TOPOLOGY_UUID,
                new OpenRoadmLinkTerminationPointsFactory(new TopologyTerminationPointTypeResolver())
        );

        assertEquals(actual, actualFromEquivalentInput);
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

        var equivalentInput = orLink(
                "ROADM-C1-SRG1",
                "SRG1-PP1-TXRX",
                "XPDR-C1-XPDR1",
                "XPDR1-NETWORK1",
                OpenroadmLinkType.XPONDERINPUT);

        Link actualFromEquivalentInput = tapiLinkImpl.createTapiLink(
                equivalentInput,
                readOpenRoadmTopology(),
                TOPOLOGY_UUID,
                new OpenRoadmLinkTerminationPointsFactory(new TopologyTerminationPointTypeResolver())
        );

        assertEquals(actual, actualFromEquivalentInput);
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

        var equivalentInput = orLink(
                "ROADM-A1-SRG1",
                "SRG1-PP3-TXRX",
                "SPDR-SA1-XPDR2",
                "XPDR2-NETWORK2",
                OpenroadmLinkType.XPONDERINPUT);

        Link actualFromEquivalentInput = tapiLinkImpl.createTapiLink(
                equivalentInput,
                readOpenRoadmTopology(),
                TOPOLOGY_UUID,
                new OpenRoadmLinkTerminationPointsFactory(new TopologyTerminationPointTypeResolver())
        );

        assertEquals(actual, actualFromEquivalentInput);
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

        var equivalentInput = orLink(
                "ROADM-C1-SRG1",
                "SRG1-PP2-TXRX",
                "SPDR-SC1-XPDR1",
                "XPDR1-NETWORK1",
                OpenroadmLinkType.XPONDERINPUT);

        Link actualFromEquivalentInput = tapiLinkImpl.createTapiLink(
                equivalentInput,
                readOpenRoadmTopology(),
                TOPOLOGY_UUID,
                new OpenRoadmLinkTerminationPointsFactory(new TopologyTerminationPointTypeResolver())
        );

        assertEquals(actual, actualFromEquivalentInput);
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

        var equivalentInput = orLink(
                "ROADM-C1-SRG1",
                "SRG1-PP3-TXRX",
                "SPDR-SC1-XPDR2",
                "XPDR2-NETWORK2",
                OpenroadmLinkType.XPONDERINPUT);

        Link actualFromEquivalentInput = tapiLinkImpl.createTapiLink(
                equivalentInput,
                readOpenRoadmTopology(),
                TOPOLOGY_UUID,
                new OpenRoadmLinkTerminationPointsFactory(new TopologyTerminationPointTypeResolver())
        );

        assertEquals(actual, actualFromEquivalentInput);
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

        var equivalentInput = orLink(
                "ROADM-A1-SRG1",
                "SRG1-PP4-TXRX",
                "SPDR-SA1-XPDR2",
                "XPDR2-NETWORK3",
                OpenroadmLinkType.XPONDERINPUT);

        Link actualFromEquivalentInput = tapiLinkImpl.createTapiLink(
                equivalentInput,
                readOpenRoadmTopology(),
                TOPOLOGY_UUID,
                new OpenRoadmLinkTerminationPointsFactory(new TopologyTerminationPointTypeResolver())
        );

        assertEquals(actual, actualFromEquivalentInput);
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

        var equivalentInput = orODTU4Link(
                "SPDR-SA1-XPDR1",
                "XPDR1-NETWORK1",
                "SPDR-SC1-XPDR1",
                "XPDR1-NETWORK1",
                OpenroadmLinkType.OTNLINK);

        Link actualFromEquivalentInput = tapiLinkImpl.createTapiLink(
                equivalentInput,
                readOTNTopology(),
                OTN_TOPOLOGY_UUID,
                new OpenRoadmLinkTerminationPointsFactory(new TopologyTerminationPointTypeResolver())
        );

        assertEquals(actual, actualFromEquivalentInput);
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

        var equivalentInput = orOTU4Link(
                "SPDR-SA1-XPDR1",
                "XPDR1-NETWORK1",
                "SPDR-SC1-XPDR1",
                "XPDR1-NETWORK1",
                OpenroadmLinkType.OTNLINK);

        Link actualFromEquivalentInput = tapiLinkImpl.createTapiLink(
                equivalentInput,
                readOTNTopology(),
                OTN_TOPOLOGY_UUID,
                new OpenRoadmLinkTerminationPointsFactory(new TopologyTerminationPointTypeResolver())
        );

        assertEquals(actual, actualFromEquivalentInput);
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

        var equivalentInput = orLink(
                "SPDR-SA1-XPDR1",
                "XPDR1-NETWORK1",
                "ROADM-A1-SRG1",
                "SRG1-PP2-TXRX",
                OpenroadmLinkType.XPONDEROUTPUT);

        Link actualFromEquivalentInput = tapiLinkImpl.createTapiLink(
                equivalentInput,
                readOpenRoadmTopology(),
                TOPOLOGY_UUID,
                new OpenRoadmLinkTerminationPointsFactory(new TopologyTerminationPointTypeResolver())
        );

        assertEquals(actual, actualFromEquivalentInput);
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

    private Link createDefaultTapiLink(
            String srcNodeId,
            String srcTpId,
            String dstNodeId,
            String dstTpId,
            String linkType) {

        return createTapiLink(
                srcNodeId,
                srcTpId,
                dstNodeId,
                dstTpId,
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

    @SuppressWarnings("unchecked")
    @Test
    void createTapiLink_shouldBuildExpectedOmsRoadmRoadmLink() throws Exception {
        NetworkTransactionService nts = mock(NetworkTransactionService.class);
        TapiContext tapiContext = mock(TapiContext.class);

        TapiLinkImpl tapiLink = spy(new TapiLinkImpl(nts, tapiContext));

        // Avoid exercising CEP/span logic here. This is a unit test for Link creation.
        doNothing().when(tapiLink).createCepForLink(any());

        var orLink = mock(
                org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226
                        .networks.network.Link.class
        );


        FluentFuture<Optional<
                org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226
                        .networks.network.Link>> future =
                (FluentFuture<Optional<
                        org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226
                                .networks.network.Link>>) mock(FluentFuture.class);

        when(future.get()).thenReturn(Optional.of(orLink));
        doReturn(future).when(nts).read(eq(LogicalDatastoreType.CONFIGURATION), any());

        Link link = tapiLink.createTapiLink(
                "ROADM-C1",
                "DEG1-TTP-TXRX",
                "ROADM-A1",
                "DEG2-TTP-TXRX",
                TapiConstants.OMS_RDM_RDM_LINK,
                TapiConstants.PHTNC_MEDIA,
                TapiConstants.PHTNC_MEDIA,
                TapiConstants.PHTNC_MEDIA_OTS,
                TapiConstants.PHTNC_MEDIA_OTS,
                "UNLOCKED",
                "ENABLED",
                Set.of(LayerProtocolName.PHOTONICMEDIA),
                Set.of(LayerProtocolName.PHOTONICMEDIA.getName()),
                TapiConstants.T0_FULL_MULTILAYER_UUID
        );

        assertNotNull(link);

        // Stable scalar fields
        assertEquals(AdministrativeState.UNLOCKED, link.getAdministrativeState());
        assertEquals(OperationalState.ENABLED, link.getOperationalState());
        assertEquals(LifecycleState.INSTALLED, link.getLifecycleState());
        assertEquals(ForwardingDirection.BIDIRECTIONAL, link.getDirection());
        assertEquals(Set.of(LayerProtocolName.PHOTONICMEDIA), link.getLayerProtocolName());

        // Ignored input parameter transLayerNameList; implementation hardcodes this instead
        assertEquals(
                Set.of(TapiConstants.PHTNC_MEDIA_OMS, TapiConstants.PHTNC_MEDIA_OTS),
                link.getTransitionedLayerProtocolName()
        );

        // Name
        assertNotNull(link.getName());
        assertEquals(1, link.getName().size());
        assertEquals(
                TapiConstants.VALUE_NAME_OMS_RDM_RDM_LINK,
                link.getName().values().iterator().next().getValueName()
        );
        assertEquals(
                "ROADM-C1+PHOTONIC_MEDIA_OTS+DEG1-TTP-TXRXtoROADM-A1+PHOTONIC_MEDIA_OTS+DEG2-TTP-TXRX",
                link.getName().values().iterator().next().getValue()
        );

        // UUID
        assertEquals(
                new Uuid(uuidOf(
                        "ROADM-C1+PHOTONIC_MEDIA_OTS+DEG1-TTP-TXRXtoROADM-A1+PHOTONIC_MEDIA_OTS+DEG2-TTP-TXRX"
                )),
                link.getUuid()
        );
        assertEquals(link.getUuid(), link.key().getUuid());

        // NodeEdgePoints
        assertNotNull(link.getNodeEdgePoint());
        assertEquals(2, link.getNodeEdgePoint().size());

        Map<?, NodeEdgePoint> neps = link.getNodeEdgePoint();

        assertTrue(neps.values().stream().anyMatch(nep ->
                TapiConstants.T0_FULL_MULTILAYER_UUID.equals(nep.getTopologyUuid())
                        && new Uuid(uuidOf("ROADM-C1+PHOTONIC_MEDIA")).equals(nep.getNodeUuid())
                        && new Uuid(uuidOf("ROADM-C1+PHOTONIC_MEDIA_OTS+DEG1-TTP-TXRX"))
                        .equals(nep.getNodeEdgePointUuid())
        ));

        assertTrue(neps.values().stream().anyMatch(nep ->
                TapiConstants.T0_FULL_MULTILAYER_UUID.equals(nep.getTopologyUuid())
                        && new Uuid(uuidOf("ROADM-A1+PHOTONIC_MEDIA")).equals(nep.getNodeUuid())
                        && new Uuid(uuidOf("ROADM-A1+PHOTONIC_MEDIA_OTS+DEG2-TTP-TXRX"))
                        .equals(nep.getNodeEdgePointUuid())
        ));

        // Fixed text fields
        assertEquals("error", link.getErrorCharacteristic());
        assertEquals("loss", link.getLossCharacteristic());
        assertEquals("repeat delivery", link.getRepeatDeliveryCharacteristic());
        assertEquals("delivery order", link.getDeliveryOrderCharacteristic());
        assertEquals("unavailable time", link.getUnavailableTimeCharacteristic());
        assertEquals("server integrity process", link.getServerIntegrityProcessCharacteristic());

        // Fixed capacities
        assertNotNull(link.getAvailableCapacity());
        assertNotNull(link.getTotalPotentialCapacity());
        assertEquals("100.0", link.getAvailableCapacity().getTotalSize().getValue().toString());
        assertEquals("100.0", link.getTotalPotentialCapacity().getTotalSize().getValue().toString());

        // Single-entry maps
        assertNotNull(link.getCostCharacteristic());
        assertEquals(1, link.getCostCharacteristic().size());

        assertNotNull(link.getLatencyCharacteristic());
        assertEquals(1, link.getLatencyCharacteristic().size());

        assertNotNull(link.getRiskCharacteristic());
        assertEquals(1, link.getRiskCharacteristic().size());

        assertNotNull(link.getValidationMechanism());
        assertEquals(1, link.getValidationMechanism().size());
    }

    @Test
    void createTapiLink_shouldUseHardcodedTransitionedLayerProtocols() throws Exception {
        NetworkTransactionService nts = mock(NetworkTransactionService.class);
        TapiContext tapiContext = mock(TapiContext.class);

        TapiLinkImpl tapiLink = spy(new TapiLinkImpl(nts, tapiContext));
        doNothing().when(tapiLink).createCepForLink(any());

        var orLink = mock(
                org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226
                        .networks.network.Link.class
        );

        @SuppressWarnings("unchecked")
        FluentFuture<Optional<
                org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226
                        .networks.network.Link>> future =
                (FluentFuture<Optional<
                        org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226
                                .networks.network.Link>>) mock(FluentFuture.class);

        when(future.get()).thenReturn(Optional.of(orLink));
        doReturn(future).when(nts).read(eq(LogicalDatastoreType.CONFIGURATION), any());

        Link link = tapiLink.createTapiLink(
                "ROADM-C1",
                "DEG1-TTP-TXRX",
                "ROADM-A1",
                "DEG2-TTP-TXRX",
                TapiConstants.OMS_RDM_RDM_LINK,
                TapiConstants.PHTNC_MEDIA,
                TapiConstants.PHTNC_MEDIA,
                TapiConstants.PHTNC_MEDIA_OTS,
                TapiConstants.PHTNC_MEDIA_OTS,
                "UNLOCKED",
                "ENABLED",
                Set.of(LayerProtocolName.PHOTONICMEDIA),
                Set.of("SOMETHING_ELSE_ENTIRELY"),
                TapiConstants.T0_FULL_MULTILAYER_UUID
        );

        assertNotNull(link);
        assertEquals(
                Set.of(TapiConstants.PHTNC_MEDIA_OMS, TapiConstants.PHTNC_MEDIA_OTS),
                link.getTransitionedLayerProtocolName()
        );
    }

    @Test
    void setTapiAdminState_shouldMapStringsCorrectly() {
        NetworkTransactionService nts = mock(NetworkTransactionService.class);
        TapiContext tapiContext = mock(TapiContext.class);
        TapiLinkImpl tapiLink = new TapiLinkImpl(nts, tapiContext);

        assertEquals(AdministrativeState.UNLOCKED, tapiLink.setTapiAdminState("UNLOCKED"));
        assertEquals(AdministrativeState.UNLOCKED, tapiLink.setTapiAdminState("inService"));
        assertEquals(AdministrativeState.LOCKED, tapiLink.setTapiAdminState("LOCKED"));
        assertNull(tapiLink.setTapiAdminState((String)null));
    }

    @Test
    void setTapiOperationalState_shouldMapStringsCorrectly() {
        NetworkTransactionService nts = mock(NetworkTransactionService.class);
        TapiContext tapiContext = mock(TapiContext.class);
        TapiLinkImpl tapiLink = new TapiLinkImpl(nts, tapiContext);

        assertEquals(OperationalState.ENABLED, tapiLink.setTapiOperationalState("ENABLED"));
        assertEquals(OperationalState.ENABLED, tapiLink.setTapiOperationalState("inService"));
        assertEquals(OperationalState.DISABLED, tapiLink.setTapiOperationalState("DISABLED"));
        assertNull(tapiLink.setTapiOperationalState((String)null));
    }

    @Test
    void setTapiAdminState_shouldReturnUnlockedWhenBothEndpointsInService() {
        assertEquals(
                AdministrativeState.UNLOCKED,
                tapiLinkImpl.setTapiAdminState(AdminStates.InService, AdminStates.InService));
    }

    @Test
    void setTapiAdminState_shouldReturnLockedWhenBothEndpointsOutOfService() {
        assertEquals(
                AdministrativeState.LOCKED,
                tapiLinkImpl.setTapiAdminState(AdminStates.OutOfService, AdminStates.OutOfService));
    }

    @Test
    void setTapiAdminState_shouldReturnLockedWhenEndpointStatesDiffer() {
        assertEquals(
                AdministrativeState.LOCKED,
                tapiLinkImpl.setTapiAdminState(AdminStates.InService, AdminStates.OutOfService));
    }

    @Test
    void setTapiAdminState_shouldReturnNullWhenFirstEndpointStateIsNull() {
        assertNull(tapiLinkImpl.setTapiAdminState(null, AdminStates.InService));
    }

    @Test
    void setTapiAdminState_shouldReturnNullWhenSecondEndpointStateIsNull() {
        assertNull(tapiLinkImpl.setTapiAdminState(AdminStates.InService, null));
    }

    @Test
    void setTapiOperationalState_shouldReturnEnabledWhenBothEndpointsInService() {
        assertEquals(
                OperationalState.ENABLED,
                tapiLinkImpl.setTapiOperationalState(State.InService, State.InService));
    }

    @Test
    void setTapiOperationalState_shouldReturnDisabledWhenBothEndpointsOutOfService() {
        assertEquals(
                OperationalState.DISABLED,
                tapiLinkImpl.setTapiOperationalState(State.OutOfService, State.OutOfService));
    }

    @Test
    void setTapiOperationalState_shouldReturnDisabledWhenEndpointStatesDiffer() {
        assertEquals(
                OperationalState.DISABLED,
                tapiLinkImpl.setTapiOperationalState(State.InService, State.OutOfService));
    }

    @Test
    void setTapiOperationalState_shouldReturnNullWhenFirstEndpointStateIsNull() {
        assertNull(tapiLinkImpl.setTapiOperationalState(null, State.InService));
    }

    @Test
    void setTapiOperationalState_shouldReturnNullWhenSecondEndpointStateIsNull() {
        assertNull(tapiLinkImpl.setTapiOperationalState(State.InService, null));
    }

    private static String uuidOf(String input) {
        return UUID.nameUUIDFromBytes(input.getBytes(StandardCharsets.UTF_8)).toString();
    }

    private Network readOpenRoadmTopology() {
        Network openroadmTopo;
        try {
            openroadmTopo = topologyUtils.readTopology(InstanceIdentifiers.OPENROADM_TOPOLOGY_II);
        } catch (TapiTopologyException e) {
            throw new IllegalStateException("Failed to read OpenROADM topology", e);
        }

        if (openroadmTopo == null) {
            throw new IllegalStateException("OpenROADM topology could not be retrieved from datastore");
        }

        return openroadmTopo;
    }

    private Network readOTNTopology() {
        Network openroadmTopo;
        try {
            openroadmTopo = topologyUtils.readTopology(InstanceIdentifiers.OTN_NETWORK_II);
        } catch (TapiTopologyException e) {
            throw new IllegalStateException("Failed to read OTN topology", e);
        }

        if (openroadmTopo == null) {
            throw new IllegalStateException("OTN topology could not be retrieved from datastore");
        }

        return openroadmTopo;
    }

    private org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.networks.network
            .Link orOTU4Link(
            String srcNodeId,
            String srcTpId,
            String destNodeId,
            String destTpId,
            OpenroadmLinkType linkType) {

        LinkId srcLinkId = new LinkId("OTU4-%s-%sto%s-%s".formatted(srcNodeId, srcTpId, destNodeId, destTpId));
        LinkId oppLinkId = new LinkId("OTU4-%s-%sto%s-%s".formatted(destNodeId, destTpId, srcNodeId, srcTpId));

        var otuLink = orLinkBuilder(srcNodeId, srcTpId, destNodeId, destTpId, linkType, srcLinkId, oppLinkId);
        otuLink.addAugmentation(
                new org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.networkutils.rev250902
                        .Link1Builder()
                        .setOtnLinkType(OtnLinkType.OTU4)
                        .build()
        );

        return otuLink.build();
    }

    private org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.networks.network
            .Link orODTU4Link(
            String srcNodeId,
            String srcTpId,
            String destNodeId,
            String destTpId,
            OpenroadmLinkType linkType) {

        LinkId srcLinkId = new LinkId("ODTU4-%s-%sto%s-%s".formatted(srcNodeId, srcTpId, destNodeId, destTpId));
        LinkId oppLinkId = new LinkId("ODTU4-%s-%sto%s-%s".formatted(destNodeId, destTpId, srcNodeId, srcTpId));

        var otuLink = orLinkBuilder(srcNodeId, srcTpId, destNodeId, destTpId, linkType, srcLinkId, oppLinkId);
        otuLink.addAugmentation(
                new org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.networkutils.rev250902
                        .Link1Builder()
                        .setOtnLinkType(OtnLinkType.ODTU4)
                        .build()
        );

        return otuLink.build();
    }


    private org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.networks.network
            .Link orOTNLink(
            String srcNodeId,
            String srcTpId,
            String destNodeId,
            String destTpId,
            OpenroadmLinkType linkType) {

        LinkId srcLinkId = new LinkId("OTU4-%s-%sto%s-%s".formatted(srcNodeId, srcTpId, destNodeId, destTpId));
        LinkId oppLinkId = new LinkId("OTU4-%s-%sto%s-%s".formatted(destNodeId, destTpId, srcNodeId, srcTpId));

        var otnLink = orLinkBuilder(srcNodeId, srcTpId, destNodeId, destTpId, linkType, srcLinkId, oppLinkId);
        otnLink.addAugmentation(
                new org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.networkutils.rev250902
                        .Link1Builder()
                        .setOtnLinkType(OtnLinkType.ODU0)
                        .build()
        );

        return otnLink.build();
    }

    private org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.networks.network
            .Link orLink(
            String srcNodeId,
            String srcTpId,
            String destNodeId,
            String destTpId,
            OpenroadmLinkType linkType) {

        LinkId srcLinkId = new LinkId("%s-%sto%s-%s".formatted(srcNodeId, srcTpId, destNodeId, destTpId));
        LinkId oppLinkId = new LinkId("%s-%sto%s-%s".formatted(destNodeId, destTpId, srcNodeId, srcTpId));

        return orLink(srcNodeId, srcTpId, destNodeId, destTpId, linkType, srcLinkId, oppLinkId);
    }

    private org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.networks.network
            .Link orLink(
            String srcNodeId,
            String srcTpId,
            String destNodeId,
            String destTpId,
            OpenroadmLinkType linkType,
            LinkId srcLinkId,
            LinkId oppLinkId) {

        return orLinkBuilder(
                srcNodeId,
                srcTpId,
                destNodeId,
                destTpId,
                linkType,
                srcLinkId,
                oppLinkId
        ).build();
    }

    private org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.networks.network
            .LinkBuilder orLinkBuilder(
            String srcNodeId,
            String srcTpId,
            String destNodeId,
            String destTpId,
            OpenroadmLinkType linkType,
            LinkId srcLinkId,
            LinkId oppLinkId) {

        LinkKey srcLinkKey = new LinkKey(srcLinkId);

        return new org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226
                .networks.network.LinkBuilder()
                .withKey(srcLinkKey)
                .setLinkId(srcLinkId)
                .setSource(
                        new SourceBuilder()
                                .setSourceNode(new NodeId(srcNodeId))
                                .setSourceTp(new TpId(srcTpId))
                                .build())
                .setDestination(
                        new DestinationBuilder()
                                .setDestNode(new NodeId(destNodeId))
                                .setDestTp(new TpId(destTpId))
                                .build())
                .addAugmentation(
                        new Link1Builder()
                                .setLinkType(linkType)
                                .setOppositeLink(oppLinkId)
                                .setOperationalState(State.InService)
                                .setAdministrativeState(AdminStates.InService)
                                .build()
                );
    }
}
