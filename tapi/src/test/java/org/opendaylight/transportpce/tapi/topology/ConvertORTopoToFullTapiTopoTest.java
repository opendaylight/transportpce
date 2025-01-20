/*
 * Copyright © 2021 Nokia, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.tapi.topology;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.either;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.transportpce.common.InstanceIdentifiers;
import org.opendaylight.transportpce.common.fixedflex.GridConstant;
import org.opendaylight.transportpce.common.network.NetworkTransactionImpl;
import org.opendaylight.transportpce.common.network.NetworkTransactionService;
import org.opendaylight.transportpce.tapi.TapiStringConstants;
import org.opendaylight.transportpce.tapi.utils.TapiContext;
import org.opendaylight.transportpce.tapi.utils.TapiLink;
import org.opendaylight.transportpce.tapi.utils.TapiLinkImpl;
import org.opendaylight.transportpce.tapi.utils.TapiTopologyDataUtils;
import org.opendaylight.transportpce.test.AbstractTest;
import org.opendaylight.transportpce.test.utils.TopologyDataUtils;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev230526.Link1;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev230526.TerminationPoint1;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev230526.TerminationPoint1Builder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.state.types.rev191129.State;
import org.opendaylight.yang.gen.v1.http.org.openroadm.equipment.states.types.rev191129.AdminStates;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.types.rev230526.OpenroadmLinkType;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.types.rev230526.OpenroadmTpType;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.NetworkId;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.Networks;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.NodeId;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.networks.Network;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.networks.NetworkKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.networks.network.Node;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.networks.network.NodeBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.networks.network.NodeKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.Network1;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.Node1;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.TpId;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.networks.network.LinkKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.networks.network.node.TerminationPoint;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.networks.network.node.TerminationPointBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.networks.network.node.TerminationPointKey;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.AdministrativeState;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.Direction;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.ForwardingDirection;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.LAYERPROTOCOLQUALIFIER;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.LayerProtocolName;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.LifecycleState;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.OperationalState;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.PortRole;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.Uuid;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.global._class.Name;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.global._class.NameKey;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev221121.OwnedNodeEdgePoint1;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev221121.cep.list.ConnectionEndPoint;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev221121.cep.list.ConnectionEndPointKey;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.digital.otn.rev221121.ODUTYPEODU0;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.digital.otn.rev221121.ODUTYPEODU2;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.digital.otn.rev221121.ODUTYPEODU2E;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.digital.otn.rev221121.ODUTYPEODU4;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.dsr.rev221121.DIGITALSIGNALTYPE100GigE;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.dsr.rev221121.DIGITALSIGNALTYPE10GigELAN;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.photonic.media.rev221121.ConnectionEndPoint2;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.photonic.media.rev221121.PHOTONICLAYERQUALIFIEROMS;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.photonic.media.rev221121.PHOTONICLAYERQUALIFIEROTS;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.photonic.media.rev221121.PHOTONICLAYERQUALIFIEROTSi;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.photonic.media.rev221121.context.topology.context.topology.node.owned.node.edge.point.cep.list.connection.end.point.OtsMediaConnectionEndPointSpec;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.photonic.media.rev221121.ots.impairments.ImpairmentRouteEntry;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.photonic.media.rev221121.spectrum.capability.pac.AvailableSpectrum;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.photonic.media.rev221121.spectrum.capability.pac.AvailableSpectrumKey;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.photonic.media.rev221121.spectrum.capability.pac.OccupiedSpectrum;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.photonic.media.rev221121.spectrum.capability.pac.OccupiedSpectrumKey;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.photonic.media.rev221121.spectrum.capability.pac.SupportableSpectrum;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.photonic.media.rev221121.spectrum.capability.pac.SupportableSpectrumKey;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.FORWARDINGRULECANNOTFORWARDACROSSGROUP;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.FORWARDINGRULEMAYFORWARDACROSSGROUP;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.RuleType;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.inter.rule.group.AssociatedNodeRuleGroup;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.node.InterRuleGroup;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.node.NodeRuleGroup;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.node.OwnedNodeEdgePoint;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.node.OwnedNodeEdgePointKey;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.node.rule.group.NodeEdgePoint;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.node.rule.group.Rule;
import org.opendaylight.yangtools.binding.DataObjectIdentifier;
import org.opendaylight.yangtools.yang.common.Uint64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConvertORTopoToFullTapiTopoTest extends AbstractTest {
    private static final Logger LOG = LoggerFactory.getLogger(ConvertORTopoToFullTapiTopoTest.class);

    private static Node otnMuxA;
    private static Node otnSwitch;
    private static Node tpdr100G;
    private static Node roadmA;
    private static Node roadmC;
    private static Network openroadmNet;
    private static Map<LinkKey, org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226
        .networks.network.Link> ortopoLinks;
    private static Uuid topologyUuid;
    private static NetworkTransactionService networkTransactionService;
    private static TapiLink tapiLink;
    private static DataBroker dataBroker = getDataBroker();

    @BeforeAll
    static void setUp() throws InterruptedException, ExecutionException {
        TopologyDataUtils.writeTopologyFromFileToDatastore(getDataStoreContextUtil(),
            TapiTopologyDataUtils.OPENROADM_TOPOLOGY_FILE, InstanceIdentifiers.OVERLAY_NETWORK_II);
        TopologyDataUtils.writeTopologyFromFileToDatastore(getDataStoreContextUtil(),
            TapiTopologyDataUtils.OPENROADM_NETWORK_FILE, InstanceIdentifiers.UNDERLAY_NETWORK_II.toIdentifier());
        TopologyDataUtils.writeTopologyFromFileToDatastore(getDataStoreContextUtil(),
            TapiTopologyDataUtils.OTN_TOPOLOGY_FILE, InstanceIdentifiers.OTN_NETWORK_II);
        TopologyDataUtils.writePortmappingFromFileToDatastore(getDataStoreContextUtil(),
            TapiTopologyDataUtils.PORTMAPPING_FILE);

        otnMuxA  = dataBroker.newReadOnlyTransaction()
            .read(
                LogicalDatastoreType.CONFIGURATION,
                DataObjectIdentifier.builder(Networks.class)
                    .child(org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226
                            .networks.Network.class,
                        new NetworkKey(new NetworkId("otn-topology")))
                .child(Node.class, new NodeKey(new NodeId("SPDR-SA1-XPDR1")))
                .build())
            .get().orElseThrow();

        otnSwitch = dataBroker.newReadOnlyTransaction()
            .read(
                LogicalDatastoreType.CONFIGURATION,
                DataObjectIdentifier.builder(Networks.class)
                    .child(org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226
                            .networks.Network.class,
                        new NetworkKey(new NetworkId("otn-topology")))
                .child(Node.class, new NodeKey(new NodeId("SPDR-SA1-XPDR2")))
                .build())
            .get().orElseThrow();
        roadmA = dataBroker.newReadOnlyTransaction()
            .read(
                LogicalDatastoreType.CONFIGURATION,
                DataObjectIdentifier.builder(Networks.class)
                    .child(org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226
                            .networks.Network.class,
                        new NetworkKey(new NetworkId("openroadm-network")))
                    .child(Node.class, new NodeKey(new NodeId("ROADM-A1")))
                    .build())
            .get().orElseThrow();
        roadmC = dataBroker.newReadOnlyTransaction()
            .read(
                LogicalDatastoreType.CONFIGURATION,
                DataObjectIdentifier.builder(Networks.class)
                    .child(org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226
                            .networks.Network.class,
                        new NetworkKey(new NetworkId("openroadm-network")))
                    .child(Node.class, new NodeKey(new NodeId("ROADM-C1")))
                    .build())
            .get().orElseThrow();

        tpdr100G = dataBroker.newReadOnlyTransaction()
            .read(
                LogicalDatastoreType.CONFIGURATION,
                DataObjectIdentifier.builder(Networks.class)
                    .child(org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226
                            .networks.Network.class,
                        new NetworkKey(new NetworkId("otn-topology")))
                    .child(Node.class, new NodeKey(new NodeId("XPDR-A1-XPDR1")))
                    .build())
            .get().orElseThrow();

        ortopoLinks = dataBroker.newReadOnlyTransaction()
            .read(
                LogicalDatastoreType.CONFIGURATION,
                DataObjectIdentifier.builder(Networks.class)
                    .child(org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226
                            .networks.Network.class,
                        new NetworkKey(new NetworkId("openroadm-topology")))
                .augmentation(Network1.class)
                .build())
            .get().orElseThrow().getLink();
        openroadmNet =  dataBroker.newReadOnlyTransaction()
            .read(
                LogicalDatastoreType.CONFIGURATION,
                DataObjectIdentifier.builder(Networks.class)
                    .child(org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226
                            .networks.Network.class,
                        new NetworkKey(new NetworkId("openroadm-topology")))
                    .build())
            .get().orElseThrow();

        topologyUuid = new Uuid(UUID.nameUUIDFromBytes(
                TapiStringConstants.T0_FULL_MULTILAYER.getBytes(Charset.forName("UTF-8")))
            .toString());
        networkTransactionService = new NetworkTransactionImpl(getDataBroker());
        tapiLink = new TapiLinkImpl(networkTransactionService, new TapiContext(networkTransactionService));
        LOG.info("TEST SETUP READY");
    }

    @Test
    void convertNodeWhenNoStates() {
        rawConvertNodeWhenBadStates(
            "XPDR-A1-XPDR1", "XPDR1-NETWORK1", "XPDR1-CLIENT1", null, null);
    }

    @Test
    void convertNodeWhenBadStates1() {
        rawConvertNodeWhenBadStates(
            "XPDR-A1-XPDR1", "XPDR1-NETWORK1", "XPDR1-CLIENT1", AdminStates.OutOfService, State.OutOfService);
    }

    @Test
    void convertNodeWhenBadStates2() {
        rawConvertNodeWhenBadStates(
            "XPDR-A1-XPDR1", "XPDR1-NETWORK1", "XPDR1-CLIENT1", AdminStates.Maintenance, State.Degraded);
    }

    private void rawConvertNodeWhenBadStates(
            String nodeId, String networkId, String clientId, AdminStates admState, State rawState) {
        ConvertORToTapiTopology tapiFactory = new ConvertORToTapiTopology();
        tapiFactory.convertNode(
            changeTerminationPointState(tpdr100G, networkId, clientId , admState, rawState),
            tpdr100G.augmentation(Node1.class).getTerminationPoint().values().stream()
                .filter(tp -> tp.augmentation(TerminationPoint1.class).getTpType()
                    .equals(OpenroadmTpType.XPONDERNETWORK))
                .map(tp -> tp.getTpId().getValue())
                .collect(Collectors.toList()));
        var dsrNodeNnOnep = tapiFactory
            .getTapiNodes()
            .get(new org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.topology.NodeKey(new Uuid(
                UUID.nameUUIDFromBytes((nodeId + "+XPONDER").getBytes(Charset.forName("UTF-8"))).toString())))
            .nonnullOwnedNodeEdgePoint();
        OwnedNodeEdgePoint enepN = dsrNodeNnOnep.get(new OwnedNodeEdgePointKey(new Uuid(
            UUID.nameUUIDFromBytes(String.join("+", nodeId, "eODU", clientId).getBytes(Charset.forName("UTF-8")))
                .toString())));
        OwnedNodeEdgePoint inepN = dsrNodeNnOnep.get(new OwnedNodeEdgePointKey(new Uuid(
            UUID.nameUUIDFromBytes(String.join("+", nodeId, "iODU", networkId).getBytes(Charset.forName("UTF-8")))
                .toString())));
        if (admState == null) {
            assertNull(enepN.getAdministrativeState(), "Administrative State should not be present");
            assertNull(inepN.getAdministrativeState(), "Administrative State should not be present");
        } else {
            assertEquals(AdministrativeState.LOCKED, enepN.getAdministrativeState(),
                "Administrative State should be Locked");
            assertEquals(AdministrativeState.LOCKED, inepN.getAdministrativeState(),
                "Administrative State should be Locked");
        }
        if (rawState == null) {
            assertNull(enepN.getOperationalState(), "Operational State should not be present");
            assertNull(inepN.getOperationalState(), "Operational State should not be present");
        } else {
            assertEquals(OperationalState.DISABLED, enepN.getOperationalState(),
                "Operational State should be Disabled");
            assertEquals(OperationalState.DISABLED, inepN.getOperationalState(),
                "Operational State should be Disabled");
        }
        //TODO verify if null checks above make sense - potential inversion
    }

    @Test
    void convertNodeForTransponder100G() {
        rawConvertNode(tpdr100G, "tpdr", "XPDR-A1-XPDR1");
    }

    @Test
    void convertNodeForOtnMuxponder() {
        rawConvertNode(otnMuxA, "mux", "SPDR-SA1-XPDR1");
    }

    @Test
    void convertNodeForOtnSwitch() {
        rawConvertNode(otnSwitch, "switch", "SPDR-SA1-XPDR2");
    }


    @Test
    void convertNodeForRoadmWhenNoOtnMuxAttached() {
        ConvertORTopoToTapiFullTopo tapiFullFactory = new ConvertORTopoToTapiFullTopo(topologyUuid, tapiLink);
        tapiFullFactory.convertRoadmNode(roadmA, openroadmNet, "Full");
        assertEquals(1, tapiFullFactory.getTapiNodes().size(), "Node list size should be 1");
        assertEquals(0, tapiFullFactory.getTapiLinks().size(), "Link list size should be empty");
        List<org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.topology.Node> tapiNodes =
            tapiFullFactory.getTapiNodes().values().stream().collect(Collectors.toList());
        checkOtsiNode(
            getNode("ROADM-A1", tapiNodes),
            new Uuid(UUID.nameUUIDFromBytes(
                    (roadmA.getNodeId().getValue() + "+PHOTONIC_MEDIA").getBytes(Charset.forName("UTF-8")))
                .toString()),
            "roadm", "ROADM-A1",
            false);
    }

    @Test
    void convertNodeForRoadmWhenRoadmNeighborAttached() {
        ConvertORTopoToTapiFullTopo tapiFullFactory = new ConvertORTopoToTapiFullTopo(topologyUuid, tapiLink);
        tapiFullFactory.convertRoadmNode(roadmA, openroadmNet, "Full");
        tapiFullFactory.convertRoadmNode(roadmC, openroadmNet, "Full");
        tapiFullFactory.convertRdmToRdmLinks(
            ortopoLinks.values().stream()
                .filter(lk -> lk.augmentation(Link1.class).getLinkType().equals(OpenroadmLinkType.ROADMTOROADM))
                .collect(Collectors.toList()));
        assertEquals(2, tapiFullFactory.getTapiNodes().size(), "Node list size should be 2");
        assertEquals(1, tapiFullFactory.getTapiLinks().size(), "Link list size should be 1");
        List<org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.topology.Node> tapiNodes =
            tapiFullFactory.getTapiNodes().values().stream().collect(Collectors.toList());
        int myInt = -1;
        for (var node : tapiNodes) {
            myInt++;
            if (node.getLayerProtocolName().contains(LayerProtocolName.DSR)) {
                continue;
            }
            if (node.getLayerProtocolName().contains(LayerProtocolName.PHOTONICMEDIA)) {
                LOG.info("LOOP ROADM node found at rank {}, with Name {} and Uuid {}",
                    myInt, node.getName(), node.getUuid());
            }
        }
        checkOtsiNode(
            getNode("ROADM-A1", tapiNodes),
            new Uuid(UUID.nameUUIDFromBytes((roadmA.getNodeId().getValue() + "+PHOTONIC_MEDIA")
                    .getBytes(Charset.forName("UTF-8")))
                .toString()),
            "roadm", "ROADM-A1",
            true);
        String roadmA1seed = "ROADM-A1+PHOTONIC_MEDIA";
        String roadmC1seed = "ROADM-C1+PHOTONIC_MEDIA";
        String roadmA1deg2seed = roadmA1seed + "_OTS+DEG2-TTP-TXRX";
        String roadmC1deg1seed = roadmC1seed + "_OTS+DEG1-TTP-TXRX";
        String linkseed = roadmC1deg1seed + "to" + roadmA1deg2seed;
        checkOmsLink(
            tapiFullFactory.getTapiLinks().values().stream()
                .sorted((l1, l2) -> l1.getUuid().getValue().compareTo(l2.getUuid().getValue()))
                .findFirst().orElseThrow(),
            new Uuid(UUID.nameUUIDFromBytes(roadmA1seed.getBytes(Charset.forName("UTF-8"))).toString()),
            new Uuid(UUID.nameUUIDFromBytes(roadmC1seed.getBytes(Charset.forName("UTF-8"))).toString()),
            new Uuid(UUID.nameUUIDFromBytes(roadmA1deg2seed.getBytes(Charset.forName("UTF-8"))).toString()),
            new Uuid(UUID.nameUUIDFromBytes(roadmC1deg1seed.getBytes(Charset.forName("UTF-8"))).toString()),
            new Uuid(UUID.nameUUIDFromBytes(linkseed.getBytes(Charset.forName("UTF-8"))).toString()),
            linkseed);
    }

    @Test
    void convertNodeForRoadmWhenOtnMuxAttached() {
        ConvertORTopoToTapiFullTopo tapiFullFactory = new ConvertORTopoToTapiFullTopo(topologyUuid, tapiLink);
        ConvertORToTapiTopology tapiFactory = new ConvertORToTapiTopology();
        tapiFactory.convertNode(
            otnMuxA,
            otnMuxA.augmentation(Node1.class).getTerminationPoint().values().stream()
                .filter(tp -> tp.augmentation(TerminationPoint1.class).getTpType()
                        .equals(OpenroadmTpType.XPONDERNETWORK))
                .map(tp -> tp.getTpId().getValue())
                .collect(Collectors.toList()));
        tapiFullFactory.setTapiNodes(tapiFactory.getTapiNodes());
        tapiFullFactory.convertRoadmNode(roadmA, openroadmNet, "Full");
        tapiFullFactory.convertXpdrToRdmLinks(
            ortopoLinks.values().stream()
                .filter(lk -> lk.augmentation(Link1.class).getLinkType().equals(OpenroadmLinkType.XPONDEROUTPUT)
                    || lk.augmentation(Link1.class).getLinkType().equals(OpenroadmLinkType.XPONDERINPUT))
                .filter(lk1 -> ((lk1.getSource().getSourceNode().equals(otnMuxA.getNodeId())
                        || lk1.getSource().getSourceNode().getValue().contains(roadmA.getNodeId().getValue()))
                    && (lk1.getDestination().getDestNode().equals(otnMuxA.getNodeId())
                        || lk1.getDestination().getDestNode().getValue().contains(roadmA.getNodeId().getValue()))))
                .collect(Collectors.toList()));
        assertEquals(2, tapiFullFactory.getTapiNodes().size(),
            "Node list size should be 2 (XPDR, DSR-ODU merged; ROADM)");
        assertEquals(1, tapiFullFactory.getTapiLinks().size(),
            "Link list size should be 1 : no more transitional link");
        Map<org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.topology.NodeKey,
            org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.topology.Node> nodeMap =
                tapiFactory.getTapiNodes();
        nodeMap.putAll(tapiFullFactory.getTapiNodes());
        List<org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.topology.Node> tapiNodes =
            nodeMap.values().stream()
                .sorted((n1, n2) -> n1.getUuid().getValue().compareTo(n2.getUuid().getValue()))
                .collect(Collectors.toList());
        checkOtsiNode(getNode("ROADM-A1", tapiNodes),
             new Uuid(UUID.nameUUIDFromBytes((roadmA.getNodeId().getValue() + "+PHOTONIC_MEDIA")
                    .getBytes(Charset.forName("UTF-8")))
                .toString()),
            "roadm", "ROADM-A1",
            false);
        String spdrSA1seed = "SPDR-SA1-XPDR1";
        String roadmA1seed = "ROADM-A1+PHOTONIC_MEDIA";
        String spdrSA1tpseed = spdrSA1seed + "+PHOTONIC_MEDIA_OTS+XPDR1-NETWORK1";
        String roadmA1tpseed = roadmA1seed + "_OTS+SRG1-PP2-TXRX";
        String linkseed = spdrSA1tpseed + "to" + roadmA1tpseed;
        Uuid node2Uuid = new Uuid(UUID.nameUUIDFromBytes(roadmA1seed.getBytes(Charset.forName("UTF-8"))).toString());
        LOG.info("{} UUID is {}",roadmA1seed, node2Uuid);
        checkXpdrRdmLink(
            tapiFullFactory.getTapiLinks().values().stream()
                .sorted((l1, l2) -> l1.getUuid().getValue().compareTo(l2.getUuid().getValue()))
                .findFirst().orElseThrow(),
            new Uuid(UUID.nameUUIDFromBytes((spdrSA1seed + "+XPONDER").getBytes(Charset.forName("UTF-8"))).toString()),
            node2Uuid,
            new Uuid(UUID.nameUUIDFromBytes(spdrSA1tpseed.getBytes(Charset.forName("UTF-8"))).toString()),
            new Uuid(UUID.nameUUIDFromBytes(roadmA1tpseed.getBytes(Charset.forName("UTF-8"))).toString()),
            new Uuid(UUID.nameUUIDFromBytes(linkseed.getBytes(Charset.forName("UTF-8"))).toString()),
            linkseed);
    }

    private void rawConvertNode(Node node0, String dsrNodeType, String nodeId) {
        ConvertORToTapiTopology tapiFactory = new ConvertORToTapiTopology();
        tapiFactory.convertNode(
            node0,
            node0.augmentation(Node1.class).getTerminationPoint().values().stream()
                .filter(tp -> tp.augmentation(TerminationPoint1.class).getTpType()
                    .equals(OpenroadmTpType.XPONDERNETWORK))
                .map(tp -> tp.getTpId().getValue())
                .collect(Collectors.toList()));
        assertEquals(1, tapiFactory.getTapiNodes().size(), "Node list size should be 1 (DSR-ODU merged)");
        assertEquals(0, tapiFactory.getTapiLinks().size(), "Link list size should be 0 : no more transitional link");
        //checkDsrNode(org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.topology.Node node,
        //    Uuid nodeUuid, String dsrNodeType, String nodeId) {
        Uuid nodeUuid =
            new Uuid(UUID.nameUUIDFromBytes((nodeId + "+XPONDER").getBytes(Charset.forName("UTF-8"))).toString());
        var node =
            tapiFactory.getTapiNodes().values().stream()
                .sorted((n1, n2) -> n1.getUuid().getValue().compareTo(n2.getUuid().getValue()))
                .findFirst().orElseThrow();
        assertEquals(nodeUuid, node.getUuid(), "incorrect node uuid");
        assertEquals(nodeId + "+XPONDER", node.getName().get(new NameKey("dsr/odu node name")).getValue(),
            "incorrect node name");
        assertEquals(AdministrativeState.UNLOCKED, node.getAdministrativeState(),
            "administrative state should be UNLOCKED");
        assertEquals(LifecycleState.INSTALLED, node.getLifecycleState(), "life-cycle state should be INSTALLED");
        assertEquals(OperationalState.ENABLED, node.getOperationalState(), "operational state should be ENABLED");
        assertThat("one value-name should be 'dsr/odu node name'",
            new ArrayList<>(node.nonnullName().keySet()), hasItem(new NameKey("dsr/odu node name")));
        assertEquals(4, node.getLayerProtocolName().size(), "dsr node should manage 4 protocol layers : dsr and odu"
            + " DIGITALOTN, PHOTONICMEDIA");
        assertThat("dsr node should manage 2 protocol layers : dsr and odu",
            node.getLayerProtocolName(), hasItems(LayerProtocolName.DSR, LayerProtocolName.ODU));
        int enepsNsize = 4;
        int inepsNsize = 1;
        int nepsCsize = 4;
        String namekey = "NodeEdgePoint_C";
        int nepsCindex = 2;
        int xpdrNb = 1;
        int clientNb = 3;
        int enepsNindex = 3;
        int inepsNindex = 0;
        switch (dsrNodeType) {
            case "switch":
                inepsNsize = 4;
                xpdrNb = 2;
                clientNb = 4;
                enepsNindex = 2;
                inepsNindex = 3;
                break;
            case "mux":
                break;
            case "tpdr":
                enepsNsize = 2;
                inepsNsize = 2;
                nepsCsize = 2;
                namekey = "100G-tpdr";
                nepsCindex = 0;
                clientNb = 1;
                enepsNindex = 0;
                inepsNindex = 1;
                break;
            default:
                fail();
                break;
        }
        List<OwnedNodeEdgePoint> inepsN = node.nonnullOwnedNodeEdgePoint().values().stream()
            .filter(n -> n.getName().containsKey(new NameKey("iNodeEdgePoint_N")))
            .sorted((nep1, nep2) -> nep1.getUuid().getValue().compareTo(nep2.getUuid().getValue()))
            .collect(Collectors.toList());
        List<OwnedNodeEdgePoint> enepsN = node.nonnullOwnedNodeEdgePoint().values().stream()
            .filter(n -> n.getName().containsKey(new NameKey("eNodeEdgePoint_N")))
            .sorted((nep1, nep2) -> nep1.getUuid().getValue().compareTo(nep2.getUuid().getValue()))
            .collect(Collectors.toList());
        var keyfilter = new NameKey(namekey);
        List<OwnedNodeEdgePoint> nepsC = node.nonnullOwnedNodeEdgePoint().values().stream()
            .filter(n -> n.getName().containsKey(keyfilter))
            .sorted((nep5, nep6) -> nep5.getUuid().getValue().compareTo(nep6.getUuid().getValue()))
            .collect(Collectors.toList());
        assertEquals(enepsNsize, enepsN.size(), dsrNodeType + "-DSR node should have " + enepsNsize
            + " eNEPs network");
        assertEquals(inepsNsize, inepsN.size(), dsrNodeType + "-DSR node should have " + inepsNsize
            + " iNEPs network");
        assertEquals(nepsCsize, nepsC.size(), dsrNodeType + "-DSR node should have " + nepsCsize
            + " NEPs client");
        OwnedNodeEdgePoint nep5 = nepsC.get(nepsCindex);
        String cl1NepUuidSeed = nodeId + "+DSR+XPDR" + xpdrNb + "-CLIENT" + clientNb;
        Uuid client1NepUuid =
            new Uuid(UUID.nameUUIDFromBytes(cl1NepUuidSeed.getBytes(Charset.forName("UTF-8"))).toString());
        switch (dsrNodeType) {
            case "switch":
                checkNepClient100GSwitch(nep5, client1NepUuid, cl1NepUuidSeed, namekey);
                break;
            case "mux":
                checkNepClient10G(nep5, client1NepUuid, cl1NepUuidSeed, namekey);
                break;
            case "tpdr":
            default:
                checkNepClient100GTpdr(nep5, client1NepUuid, cl1NepUuidSeed, namekey);
                break;
        }
        String enetNepUuidSeed = nodeId + "+eODU+XPDR" + xpdrNb + "-CLIENT" + clientNb;
        Uuid enetworkNepUuid =
            new Uuid(UUID.nameUUIDFromBytes(enetNepUuidSeed.getBytes(Charset.forName("UTF-8"))).toString());
        checkNepeODU4(enepsN.get(enepsNindex), enetworkNepUuid, enetNepUuidSeed, "eNodeEdgePoint_N", false);
        String inetNepUuidSeed = nodeId + "+iODU+XPDR" + xpdrNb + "-NETWORK1";
        Uuid inetworkNepUuid =
            new Uuid(UUID.nameUUIDFromBytes(inetNepUuidSeed.getBytes(Charset.forName("UTF-8"))).toString());
        checkNepNetworkODU4(inepsN.get(inepsNindex), inetworkNepUuid, inetNepUuidSeed, "iNodeEdgePoint_N", true);
        List<NodeRuleGroup> nrgList3 = node.nonnullNodeRuleGroup().values().stream()
            .sorted((nrg1, nrg2) -> nrg1.getUuid().getValue().compareTo(nrg2.getUuid().getValue()))
            .collect(Collectors.toList());
        switch (dsrNodeType) {
            case "switch":
                checkNodeRuleGroupForSwitchDSR(nrgList3, client1NepUuid, inetworkNepUuid, nodeUuid);
                break;
            case "mux":
                checkNodeRuleGroupForMuxDSR(nrgList3, client1NepUuid, inetworkNepUuid, nodeUuid);
                break;
            case "tpdr":
            default:
                checkNodeRuleGroupForTpdrDSR(nrgList3, client1NepUuid, inetworkNepUuid, nodeUuid);
                break;
            // keep trace of the previous test performed before the structure of the NRG was modified
            //enetworkNepUuid,
        }
    }

    private void checkOtsiNode(
            org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.topology.Node node,
            Uuid nodeUuid, String otsiNodeType, String nodeId, boolean includingCep) {
        if (!node.getUuid().equals(nodeUuid)) {
            LOG.info("ERRORUUID on Node.getNodeId {}, NodeId {}", node.getName(), nodeId);
            LOG.info("ERRORUUID TapiUuid {}, transmitted Node Uuid {}", node.getUuid(), nodeUuid);
        }
        assertEquals(nodeUuid, node.getUuid(), "incorrect node uuid");
        List<OwnedNodeEdgePoint> nepsI = null;
        List<OwnedNodeEdgePoint> nepsE = null;
        List<OwnedNodeEdgePoint> nepsP = null;
        String suffix = "+XPONDER";
        String namekey = "dsr/odu node name";
        if (otsiNodeType.equals("roadm")) {
            suffix = "+PHOTONIC_MEDIA";
            namekey = "roadm node name";
        } else {
            nepsI = node.nonnullOwnedNodeEdgePoint().values().stream()
                .filter(n -> n.getName().containsKey(new NameKey("iNodeEdgePoint")))
                .sorted((nep1, nep2) -> nep1.getUuid().getValue().compareTo(nep2.getUuid().getValue()))
                .collect(Collectors.toList());
            nepsE = node.nonnullOwnedNodeEdgePoint().values().stream()
                .filter(n -> n.getName().containsKey(new NameKey("eNodeEdgePoint")))
                .sorted((nep1, nep2) -> nep1.getUuid().getValue().compareTo(nep2.getUuid().getValue()))
                .collect(Collectors.toList());
            nepsP = node.nonnullOwnedNodeEdgePoint().values().stream()
                .filter(n -> n.getName().containsKey(new NameKey("PhotMedNodeEdgePoint")))
                .sorted((nep1, nep2) -> nep1.getUuid().getValue().compareTo(nep2.getUuid().getValue()))
                .collect(Collectors.toList());
        }
        assertEquals(nodeId + suffix, node.getName().get(new NameKey(namekey)).getValue(), "incorrect node name");
        assertThat("one value-name should be 'dsr/odu node name'",
            new ArrayList<>(node.nonnullName().keySet()), hasItem(new NameKey(namekey)));
        assertEquals(AdministrativeState.UNLOCKED, node.getAdministrativeState(),
            "administrative state should be UNLOCKED");
        assertEquals(LifecycleState.INSTALLED, node.getLifecycleState(), "life-cycle state should be INSTALLED");
        assertEquals(OperationalState.ENABLED, node.getOperationalState(), "operational state should be ENABLED");
        assertEquals(1, node.getLayerProtocolName().size(),
            "otsi node should manage a single protocol layer : PHOTONIC_MEDIA");
        assertEquals(LayerProtocolName.PHOTONICMEDIA, node.getLayerProtocolName().stream().findFirst().orElseThrow(),
            "otsi node should manage a single protocol layer : PHOTONIC_MEDIA");
        int nepSize = 1;
        int xpdrOrNetNb = 1;
        int indexNepsIorP = 0;

        switch (otsiNodeType) {
            case "roadm":
                String pmOMSnep = "PHOTONIC_MEDIA_OMSNodeEdgePoint";
                String pmOTSnep = "PHOTONIC_MEDIA_OTSNodeEdgePoint";
                List<OwnedNodeEdgePoint> nepsOMS = node.nonnullOwnedNodeEdgePoint().values().stream()
                    .filter(n -> n.getName().containsKey(new NameKey(pmOMSnep)))
                    .sorted((n1, n2) -> n1.getUuid().getValue().compareTo(n2.getUuid().getValue()))
                    .collect(Collectors.toList());
                List<OwnedNodeEdgePoint> nepsOTS = node.nonnullOwnedNodeEdgePoint().values().stream()
                    .filter(n -> n.getName().containsKey(new NameKey(pmOTSnep)))
                    .sorted((n1, n2) -> n1.getUuid().getValue().compareTo(n2.getUuid().getValue()))
                    .collect(Collectors.toList());
                List<OwnedNodeEdgePoint> nepsPhot = new ArrayList<>(nepsOMS);
                nepsPhot.addAll(nepsOTS);
// Keep trace of MC NEP test to be restored after the new policy for creating NEP is applied
//                assertEquals(0, nepsMc.size(), "MC NEP no more configured, Roadm node should have 0 MC NEPs");
//                assertEquals(0, nepsOtsimc.size(), "Roadm node should have 10 OTSiMC NEPs");
                assertEquals(12, nepsPhot.size(), "Roadm node should have 12 PHOT_MEDIA NEPs (2x4 OTS +2x(OTS+OMS)");
                // For Degree node
//<<<<<<< HEAD
//                String mcnepUuidSeed = nodeId + "+PHOTONIC_MEDIA_OMS+DEG1-TTP-TXRX";
//                checkNepOtsiRdmNode(
//                    getOnep("DEG1-TTP", nepsOMS),
//                    new Uuid(UUID.nameUUIDFromBytes(mcnepUuidSeed.getBytes(Charset.forName("UTF-8"))).toString()),
//                    mcnepUuidSeed, pmOMSnep , false);
//                String otmcnepUuidSeed = nodeId + "+PHOTONIC_MEDIA_OTS+DEG1-TTP-TXRX";
//                checkNepOtsiRdmNode(
//                    getOnep("DEG1-TTP", nepsOTS),
//                    new Uuid(UUID.nameUUIDFromBytes(otmcnepUuidSeed.getBytes(Charset.forName("UTF-8"))).toString()),
//                    otmcnepUuidSeed, pmOTSnep, false);
//                // For srg node
//                String otscnepUuidSeed = nodeId + "+PHOTONIC_MEDIA_OTS+SRG1-PP1-TXRX";
//                checkNepOtsiRdmNode(
//                    getOnep("SRG1-PP1", nepsOTS),
//                    new Uuid(UUID.nameUUIDFromBytes(otscnepUuidSeed.getBytes(Charset.forName("UTF-8"))).toString()),
//                    otscnepUuidSeed, pmOTSnep, false);
//                String otscnep4UuidSeed = nodeId + "+PHOTONIC_MEDIA_OTS+SRG1-PP3-TXRX";
//                checkNepOtsiRdmNode(
//                    getOnep("SRG1-PP3", nepsOTS),
//                    new Uuid(UUID.nameUUIDFromBytes(otscnep4UuidSeed.getBytes(Charset.forName("UTF-8"))).toString()),
//                    otscnep4UuidSeed, pmOTSnep, false);
//=======
                OwnedNodeEdgePoint nepOmsDeg = getOnep("DEG2-TTP", nepsOMS);
                Uuid omsNepDegUuid = new Uuid(
                    UUID.nameUUIDFromBytes((nodeId + "+PHOTONIC_MEDIA_OMS+DEG2-TTP-TXRX").getBytes(Charset
                        .forName("UTF-8"))).toString());
                checkNepOtsiRdmNode(nepOmsDeg, omsNepDegUuid, nodeId + "+PHOTONIC_MEDIA_OMS+DEG2-TTP-TXRX",
                    "PHOTONIC_MEDIA_OMSNodeEdgePoint", false);
                OwnedNodeEdgePoint nepOtsDeg = getOnep("DEG2-TTP", nepsOTS);
                Uuid otsNepDegUuid = new Uuid(UUID.nameUUIDFromBytes((nodeId + "+PHOTONIC_MEDIA_OTS+DEG2-TTP-TXRX")
                    .getBytes(Charset.forName("UTF-8"))).toString());
                LOG.info("Line797, NEP {} with UUID put in checkCepOtsi Rdm Node {}",
                    String.join("+", nodeId, "PHOTONIC_MEDIA_OTS+DEG2-TTP-TXRX"), otsNepDegUuid);
                checkNepOtsiRdmNode(nepOtsDeg, otsNepDegUuid, nodeId + "+PHOTONIC_MEDIA_OTS+DEG2-TTP-TXRX",
                    "PHOTONIC_MEDIA_OTSNodeEdgePoint", false);
                OwnedNodeEdgePoint omsNep3 = getOnep("DEG2-TTP", nepsOMS);
                LOG.info("Node tested in 800 is Node {}", node);
                if (includingCep) {
                    checkCepOtsiRdmNode(nepOmsDeg, omsNepDegUuid, nodeId + "+PHOTONIC_MEDIA_OMS+DEG2-TTP-TXRX",
                        "PHOTONIC_MEDIA_OMSNodeEdgePoint", false);
                    LOG.info("Calling CheckCepOtsiRdmNode for NodeId {}, NepUuid {}", nodeId, otsNepDegUuid);
                    checkCepOtsiRdmNode(nepOtsDeg, otsNepDegUuid, nodeId + "+PHOTONIC_MEDIA_OTS+DEG2-TTP-TXRX",
                        "PHOTONIC_MEDIA_OTSNodeEdgePoint", false);
                }

                // For srg node
                OwnedNodeEdgePoint nepOtsSrg = getOnep("SRG1-PP1", nepsOTS);
                Uuid otsNepUuidSrg = new Uuid(UUID.nameUUIDFromBytes((nodeId + "+PHOTONIC_MEDIA_OTS+SRG1-PP1-TXRX")
                    .getBytes(Charset.forName("UTF-8"))).toString());
                checkNepOtsiRdmNode(nepOtsSrg, otsNepUuidSrg, nodeId + "+PHOTONIC_MEDIA_OTS+SRG1-PP1-TXRX",
                    "PHOTONIC_MEDIA_OTSNodeEdgePoint", false);
                OwnedNodeEdgePoint nepOtsSrg2 = getOnep("SRG1-PP3", nepsOTS);
                Uuid otsNepUuidSrg2 = new Uuid(UUID.nameUUIDFromBytes((nodeId + "+PHOTONIC_MEDIA_OTS+SRG1-PP3-TXRX")
                    .getBytes(Charset.forName("UTF-8"))).toString());
                checkNepOtsiRdmNode(nepOtsSrg2, otsNepUuidSrg2, nodeId + "+PHOTONIC_MEDIA_OTS+SRG1-PP3-TXRX",
                    "PHOTONIC_MEDIA_OTSNodeEdgePoint", false);
//>>>>>>> 381b4f2d (Consolidate ConnectivityUtils)
                List<NodeRuleGroup> nrgList4 = node.nonnullNodeRuleGroup().values().stream()
                    .sorted((nrg1, nrg2) -> nrg1.getName().entrySet().iterator().next().getValue().toString()
                        .compareTo(nrg2.getName().entrySet().iterator().next().getValue().toString()))
                    .collect(Collectors.toList());
                LOG.info("NODERULEGROUP List nrgLIst4 is as follows {}", nrgList4);
                List<Integer> nepNumber = new ArrayList<>(List.of(2, 4, 4));
                checkNodeRuleGroupForRdm(nrgList4, nepNumber);
                List<InterRuleGroup> irgList = node.nonnullInterRuleGroup().values().stream()
                    .collect(Collectors.toList());
                checkInterRuleGroupForRdm(irgList);
                return;
            case "switch":
                nepSize = 4;
                xpdrOrNetNb = 2;
                indexNepsIorP = 1;
                break;
            case "tpdr":
                nepSize = 2;
                break;
            case "mux":
                break;
            default:
                fail();
                break;
        }
        assertEquals(nepSize, nepsE.size(), otsiNodeType + "-OTSi node should have " + nepSize + " eNEPs");
        assertEquals(nepSize, nepsI.size(), otsiNodeType + "-OTSi node should have " + nepSize + " iNEPs");
        assertEquals(nepSize, nepsP.size(), otsiNodeType + "-OTSi node should have " + nepSize + " photNEPs");
        OwnedNodeEdgePoint enep = nepsE.get(0);
        String enepUuidSeed = nodeId + "+PHOTONIC_MEDIA_OTS+XPDR" + xpdrOrNetNb + "-NETWORK" + xpdrOrNetNb;
        Uuid enepUuid =
            new Uuid(UUID.nameUUIDFromBytes(enepUuidSeed.getBytes(Charset.forName("UTF-8"))).toString());
        checkNepOtsiNode(enep, enepUuid, enepUuidSeed, "eNodeEdgePoint", false);
        OwnedNodeEdgePoint inep = nepsI.get(indexNepsIorP);
        String inepUuidSeed = nodeId + "+OTSi+XPDR" + xpdrOrNetNb + "-NETWORK" + xpdrOrNetNb;
        Uuid inepUuid =
            new Uuid(UUID.nameUUIDFromBytes(inepUuidSeed.getBytes(Charset.forName("UTF-8"))).toString());
        checkNepOtsiNode(inep, inepUuid, inepUuidSeed, "iNodeEdgePoint", true);
        OwnedNodeEdgePoint photNep = nepsP.get(indexNepsIorP);
        String pnepUuidSeed = nodeId + "+PHOTONIC_MEDIA+XPDR" + xpdrOrNetNb + "-NETWORK" + xpdrOrNetNb;
        Uuid pnepUuid =
            new Uuid(UUID.nameUUIDFromBytes(pnepUuidSeed.getBytes(Charset.forName("UTF-8"))).toString());
        checkNepOtsiNode(photNep, pnepUuid, pnepUuidSeed, "PhotMedNodeEdgePoint", false);
        List<NodeRuleGroup> nrgList = node.nonnullNodeRuleGroup().values().stream()
            .sorted((nrg1, nrg2) -> nrg1.getUuid().getValue().compareTo(nrg2.getUuid().getValue()))
            .collect(Collectors.toList());
        checkNodeRuleGroupForTpdrOTSi(nrgList, enepUuid, inepUuid, nodeUuid);
    }

    private void checkNepClient10G(OwnedNodeEdgePoint nep, Uuid nepUuid, String portName, String nepName) {
        rawCheckNep(
            List.of(ODUTYPEODU2.VALUE, ODUTYPEODU2E.VALUE, DIGITALSIGNALTYPE10GigELAN.VALUE), LayerProtocolName.DSR,
            false, nep, nepUuid, portName, nepName, false);
    }

    private void checkNepeODU4(
            OwnedNodeEdgePoint nep, Uuid nepUuid, String portName, String nepName, boolean withSip) {
        rawCheckNep(
            List.of(ODUTYPEODU0.VALUE, ODUTYPEODU2.VALUE, ODUTYPEODU2E.VALUE, ODUTYPEODU4.VALUE), LayerProtocolName.ODU,
            true, nep, nepUuid, portName, nepName, withSip);
    }

    private void checkNepNetworkODU4(
            OwnedNodeEdgePoint nep, Uuid nepUuid, String portName, String nepName, boolean withSip) {
        rawCheckNep(
            List.of(ODUTYPEODU4.VALUE), LayerProtocolName.ODU, false,
            nep, nepUuid, portName, nepName, withSip);
    }

    private void checkNodeRuleGroupForTpdrDSR(
            List<NodeRuleGroup> nrgList, Uuid clientNepUuid, Uuid networkNepUuid, Uuid nodeUuid) {
        rawCheckNodeRuleGroupDSR("Transponder", nrgList, clientNepUuid, networkNepUuid, nodeUuid);
    }

    private void checkNodeRuleGroupForMuxDSR(
            List<NodeRuleGroup> nrgList, Uuid clientNepUuid, Uuid networkNepUuid, Uuid nodeUuid) {
        rawCheckNodeRuleGroupDSR("Muxponder", nrgList, clientNepUuid, networkNepUuid, nodeUuid);
    }

    private void checkNodeRuleGroupForSwitchDSR(
            List<NodeRuleGroup> nrgList, Uuid clientNepUuid, Uuid networkNepUuid, Uuid nodeUuid) {
        rawCheckNodeRuleGroupDSR("Switch", nrgList, clientNepUuid, networkNepUuid, nodeUuid);
    }

    private void rawCheckNodeRuleGroupDSR(String nodeType,
            List<NodeRuleGroup> nrgList, Uuid clientNepUuid, Uuid networkNepUuid, Uuid nodeUuid) {
        int nrgListSize;
        int nrgNb;
        int nepClNb;
        List<Integer> nepUuidCheckList;
        List<Integer> nodeIdCheckList;
        switch (nodeType) {
            case "Switch":
                nrgListSize = 2;
                nrgNb = 1;
                nepClNb = 4;
                nepUuidCheckList = List.of(5,6);
                nodeIdCheckList = List.of(3,4);
                break;
            case "Transponder":
                nrgListSize = 4;
                nrgNb = 1;
                nepClNb = 1;
                nepUuidCheckList = List.of(0,1);
                nodeIdCheckList = List.of(0,1);
                break;
            case "Muxponder":
            default:
                nrgListSize = 8;
                nrgNb = 2;
                nepClNb = 4;
                nepUuidCheckList = List.of(1);
                nodeIdCheckList = List.of(0,1);
                break;
        }
        assertEquals(nrgListSize, nrgList.size(),
            nodeType + " DSR should contain " + nrgListSize + " node rule group (DSR-I_ODU/I-ODU-E_ODU)");
        NodeRuleGroup nrgCltAndNet = nrgList.stream()
                .filter(nrg -> nrg.nonnullNodeEdgePoint().values().stream()
                        .map(nep -> nep.getNodeEdgePointUuid())
                        .collect(Collectors.toList())
                        .containsAll(List.of(clientNepUuid, networkNepUuid)))
                .findFirst().orElseThrow();
        assertNotNull("One node-rule-group shall contains client and network Neps", nrgCltAndNet);
        List<NodeEdgePoint> nodeEdgePointList;
        if (nodeType.equals("Switch")) {
            assertEquals(8, nrgCltAndNet.getNodeEdgePoint().size(), "Switch-DSR nrg should contain 8 NEP");
            nodeEdgePointList = nrgList.get(0).nonnullNodeEdgePoint().values().stream()
                .sorted((nrg1, nrg2) -> nrg1.getNodeEdgePointUuid().getValue()
                    .compareTo(nrg2.getNodeEdgePointUuid().getValue()))
                .collect(Collectors.toList());
            Integer xxxxx = 0;
            for (NodeEdgePoint nep : nodeEdgePointList) {
                LOG.info("nep number {} UUID is {} ", xxxxx, nep.getNodeEdgePointUuid());
                xxxxx++;
            }
            LOG.info("nep SPDR-SA1-XPDR2+iODU+XPDR2-NETWORK1 UUID is {} ",
                UUID.nameUUIDFromBytes(("SPDR-SA1-XPDR2" + "+iODU+XPDR2-NETWORK1").getBytes(Charset.forName("UTF-8"))));
            LOG.info("nep SPDR-SA1-XPDR2+DSR+XPDR2-CLIENT4 UUID is {} ",
                UUID.nameUUIDFromBytes(("SPDR-SA1-XPDR2" + "+DSR+XPDR2-CLIENT4").getBytes(Charset.forName("UTF-8"))));
            assertEquals(networkNepUuid, nodeEdgePointList.get(6).getNodeEdgePointUuid(),
                "in the sorted node-rule-group, nep number 7 should be XPDR2-NETWORK1");
            assertEquals(clientNepUuid, nodeEdgePointList.get(5).getNodeEdgePointUuid(),
                "in the sorted node-rule-group, nep number 6 should be XPDR2-CLIENT4");
            //TODO regroup with else condition ?
        } else {
            nodeEdgePointList = new ArrayList<>(nrgCltAndNet.nonnullNodeEdgePoint().values());
            for (int i : nepUuidCheckList) {
                assertThat("node-rule-group nb " + nrgNb + " should be between nep-client" + nepClNb
                        + " and nep-network1",
                    nodeEdgePointList.get(i).getNodeEdgePointUuid().getValue(),
                    either(containsString(networkNepUuid.getValue())).or(containsString(clientNepUuid.getValue())));
            }
        }
        for (int i : nodeIdCheckList) {
            assertEquals(nodeEdgePointList.get(i).getNodeUuid(), nodeUuid,
                "any item of the node-rule-group should have the same nodeUuid");
        }
        List<Rule> rule = new ArrayList<>(nrgList.get(1).nonnullRule().values());
        assertEquals(1, rule.size(), "node-rule-group should contain a single rule");
        assertEquals("forward", rule.get(0).getLocalId(), "local-id of the rule should be 'forward'");
        assertEquals(FORWARDINGRULEMAYFORWARDACROSSGROUP.VALUE, rule.get(0).getForwardingRule(),
            "the forwarding rule should be 'MAYFORWARDACROSSGROUP'");
        assertEquals(RuleType.FORWARDING, rule.get(0).getRuleType().stream().findFirst().orElseThrow(),
            "the rule type should be 'FORWARDING'");
        //TODO regroup with rawCheckNodeRuleGroupOTsi ?
    }

    private void checkNodeRuleGroupForRdm(List<NodeRuleGroup> nrgList, List<Integer> nbNeps) {
        assertEquals(3, nrgList.size(), "RDM infra node - OTS should contain 3 node rule groups");
        int index = 0;
        for (NodeRuleGroup nrg : nrgList) {
            List<NodeEdgePoint> nodeEdgePointList = new ArrayList<>(nrg.getNodeEdgePoint().values());
            assertEquals(nbNeps.get(index), nodeEdgePointList.size(),
                "RDM infra node -rule-group should contain " + nbNeps.get(index) + " NEP");
            List<Rule> ruleList = new ArrayList<>(nrg.nonnullRule().values());
            assertEquals(1, ruleList.size(), "node-rule-group should contain a single rule");
            assertEquals("forward", ruleList.get(0).getLocalId(), "local-id of the rule should be 'forward'");
            assertEquals(RuleType.FORWARDING, ruleList.get(0).getRuleType().stream().findFirst().orElseThrow(),
                "the rule type should be 'FORWARDING'");
            if (nrg.getName().entrySet().iterator().next().getValue().toString().contains("DEG")) {
                assertEquals(FORWARDINGRULEMAYFORWARDACROSSGROUP.VALUE, ruleList.get(0).getForwardingRule(),
                    "the forwarding rule should be 'MAYFORWARDACROSSGROUP'");
            } else {
                assertEquals(FORWARDINGRULECANNOTFORWARDACROSSGROUP.VALUE, ruleList.get(0).getForwardingRule(),
                    "the forwarding rule should be 'CANNOTFORWARDACROSSGROUP'");
            }
            index++;
        }
    }

    private void checkNodeRuleGroupForTpdrOTSi(
            List<NodeRuleGroup> nrgList, Uuid enepUuid, Uuid inepUuid, Uuid nodeUuid) {
        rawCheckNodeRuleGroupOTsi("Tpdr", nrgList, enepUuid, inepUuid, nodeUuid);
    }

    private void checkInterRuleGroupForRdm(List<InterRuleGroup> irgList) {
        assertEquals(1, irgList.size(), "RDM infra node - OTS should contain 1 inter rule group");
        List<AssociatedNodeRuleGroup> anrgList = new ArrayList<>(irgList.get(0).getAssociatedNodeRuleGroup().values());
        assertEquals(3, anrgList.size(), "RDM infra node inter-rule-group should contain 3 associated nrg");
        List<org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.inter.rule.group.Rule>
            ruleList = new ArrayList<>(irgList.get(0).nonnullRule().values());
        assertEquals(1, ruleList.size(), "inter-rule-group should contain a single rule");
        assertEquals("forward", ruleList.get(0).getLocalId(), "local-id of the rule should be 'forward'");
        assertEquals(RuleType.FORWARDING, ruleList.get(0).getRuleType().stream().findFirst().orElseThrow(),
            "the rule type should be 'FORWARDING'");
        assertEquals(FORWARDINGRULEMAYFORWARDACROSSGROUP.VALUE, ruleList.get(0).getForwardingRule(),
                    "the forwarding rule should be 'MAYFORWARDACROSSGROUP'");
    }

    private void checkNodeRuleGroupForMuxOTSi(
            List<NodeRuleGroup> nrgList, Uuid enepUuid, Uuid inepUuid, Uuid nodeUuid) {
        rawCheckNodeRuleGroupOTsi("Mux", nrgList, enepUuid, inepUuid, nodeUuid);
    }

    private void checkNodeRuleGroupForSwitchOTSi(
            List<NodeRuleGroup> nrgList, Uuid enepUuid, Uuid inepUuid, Uuid nodeUuid) {
        rawCheckNodeRuleGroupOTsi("Switch", nrgList, enepUuid, inepUuid, nodeUuid);
    }

    private void rawCheckNodeRuleGroupOTsi(String nodeType,
            List<NodeRuleGroup> nrgList, Uuid enepUuid, Uuid inepUuid, Uuid nodeUuid) {
        int nrgListSize;
        String network;
        switch (nodeType) {
            case "Switch":
                nrgListSize = 4;
                network = "XPDR2-NETWORK2";
                break;
            case "Tpdr":
            case "Mux":
            default:
                nrgListSize = 1;
                network = "XPDR1-NETWORK1";
                break;
        }
        assertEquals(nrgListSize, nrgList.size(),
            nodeType + "-OTSi should contain " + nrgListSize + " node rule group");
        List<NodeEdgePoint> nodeEdgePointList = new ArrayList<>(nrgList.get(0).getNodeEdgePoint().values());
        for (NodeRuleGroup nodeRuleGroup : nrgList) {
            assertEquals(2, nodeRuleGroup.getNodeEdgePoint().size(),
                "each node-rule-group should contain 2 NEP for " + nodeType + "-OTSi");
        }
        assertThat(nodeType + "-OTSi node-rule-group should be between eNEP and iNEP of " + network,
            nodeEdgePointList.get(0).getNodeEdgePointUuid().getValue(),
            either(containsString(enepUuid.getValue())).or(containsString(inepUuid.getValue())));
        assertThat(nodeType + "-OTSi node-rule-group should be between eNEP and iNEP of " + network,
            nodeEdgePointList.get(1).getNodeEdgePointUuid().getValue(),
            either(containsString(enepUuid.getValue())).or(containsString(inepUuid.getValue())));
        assertEquals(nodeUuid, nodeEdgePointList.get(0).getNodeUuid(),
            "any item of the node-rule-group should have the same nodeUuid");
        assertEquals(nodeUuid, nodeEdgePointList.get(1).getNodeUuid(),
            "any item of the node-rule-group should have the same nodeUuid");
        List<Rule> ruleList = new ArrayList<>(nrgList.get(0).nonnullRule().values());
        assertEquals(1, ruleList.size(), "node-rule-group should contain a single rule");
        assertEquals("forward", ruleList.get(0).getLocalId(), "local-id of the rule should be 'forward'");
        assertEquals(FORWARDINGRULEMAYFORWARDACROSSGROUP.VALUE, ruleList.get(0).getForwardingRule(),
            "the forwarding rule should be 'MAYFORWARDACROSSGROUP'");
        assertEquals(RuleType.FORWARDING, ruleList.get(0).getRuleType().stream().findFirst().orElseThrow(),
            "the rule type should be 'FORWARDING'");
    }

    private void checkNepClient100GSwitch(OwnedNodeEdgePoint nep, Uuid nepUuid, String portName, String nepName) {
        rawCheckNep(
            List.of(ODUTYPEODU4.VALUE, DIGITALSIGNALTYPE100GigE.VALUE), LayerProtocolName.DSR, false,
            nep, nepUuid, portName, nepName, false);
    }

    private void checkNepClient100GTpdr(OwnedNodeEdgePoint nep, Uuid nepUuid, String portName, String nepName) {
        rawCheckNep(
            List.of(DIGITALSIGNALTYPE100GigE.VALUE), LayerProtocolName.DSR, false,
            nep, nepUuid, portName, nepName, false);
    }

    private void checkNepOtsiNode(
            OwnedNodeEdgePoint nep, Uuid nepUuid, String portName, String nepName, boolean withSip) {
        rawCheckNep(
            List.of(PHOTONICLAYERQUALIFIEROMS.VALUE, PHOTONICLAYERQUALIFIEROTSi.VALUE), LayerProtocolName.PHOTONICMEDIA,
            false, nep, nepUuid, portName, nepName, withSip);
    }

//    private void checkNepOtsiNode(OwnedNodeEdgePoint nep, Uuid nepUuid, String portName, String nepName,
//            boolean withSip) {
//        assertEquals(nepUuid, nep.getUuid(), "bad uuid for " + portName);
//        List<Name> nameList = new ArrayList<>(nep.nonnullName().values());
//        assertEquals(portName, nameList.get(0).getValue(), "value of OTSi nep should be '" + portName + "'");
//        assertEquals(nepName, nameList.get(0).getValueName(), "value-name of OTSi nep should be '" + nepName + "'");
//        List<LAYERPROTOCOLQUALIFIER> lpql = new ArrayList<>();
//        List<SupportedCepLayerProtocolQualifierInstances> lsclpqi = nep
//                .getSupportedCepLayerProtocolQualifierInstances();
//        for (SupportedCepLayerProtocolQualifierInstances entry : lsclpqi) {
//            lpql.add(entry.getLayerProtocolQualifier());
//        }
//        assertEquals(2, lpql.size(), "OTSi nep should support 2 kind of cep");
//        assertThat("OTSi nep should support 2 kind of cep",
//            lpql, hasItems(PHOTONICLAYERQUALIFIEROMS.VALUE, PHOTONICLAYERQUALIFIEROTSi.VALUE));
//        assertEquals(LayerProtocolName.PHOTONICMEDIA, nep.getLayerProtocolName(),
//            "OTSi nep should be of PHOTONIC_MEDIA protocol type");
//        checkCommonPartOfNep(nep, withSip);
//        checkPhotPartOfNep(nep);
//    }

    private void checkNepOtsiRdmNode(
            OwnedNodeEdgePoint nep, Uuid nepUuid, String portName, String nepName, boolean withSip) {
        if (!nep.getUuid().equals(nepUuid)) {
            LOG.info("ERRORUUIDNEP on Nep {}, expected {}", nep.getName(), portName);
        }
        rawCheckNep(nepName.contains("OMS")
                ? List.of(PHOTONICLAYERQUALIFIEROMS.VALUE)
                : nepName.contains("OTS") ? List.of(PHOTONICLAYERQUALIFIEROTS.VALUE) : null,
            LayerProtocolName.PHOTONICMEDIA, false, nep, nepUuid, portName, nepName, withSip);
//=======
//        assertEquals(nepUuid, nep.getUuid(), "bad uuid for " + portName);
//        List<Name> nameList = new ArrayList<>(nep.nonnullName().values());
//        assertEquals(portName, nameList.get(0).getValue(),
//            "value of OTSi nep should be '" + portName + "'");
//        assertEquals(nepName, nameList.get(0).getValueName(),
//            "value-name of OTSi nep should be '" + nepName + "'");
//        List<LAYERPROTOCOLQUALIFIER> lpql = new ArrayList<>();
//        List<SupportedCepLayerProtocolQualifierInstances> lsclpqi = nep
//                .getSupportedCepLayerProtocolQualifierInstances();
//        for (SupportedCepLayerProtocolQualifierInstances entry : lsclpqi) {
//            lpql.add(entry.getLayerProtocolQualifier());
//        }
//        if (nepName.contains("OMS")) {
//            assertEquals(1, lpql.size(), "OTSi nep of RDM infra node should support only 1 kind of cep");
//            assertThat("OTSi nep should support 1 kind of cep", lpql, hasItems(PHOTONICLAYERQUALIFIEROMS.VALUE));
//            assertEquals(LayerProtocolName.PHOTONICMEDIA, nep.getLayerProtocolName(),
//                "OTSi nep should be of PHOTONIC_MEDIA protocol type");
//            checkPhotPartOfNep(nep);
//        } else if (nepName.contains("OTS")) {
//            assertEquals(1, lpql.size(), "OTSi nep of RDM infra node should support only 1 kind of cep");
//            assertThat("OTSi nep should support 1 kind of cep", lpql, hasItems(PHOTONICLAYERQUALIFIEROTS.VALUE));
//            assertEquals(LayerProtocolName.PHOTONICMEDIA, nep.getLayerProtocolName(),
//                "OTSi nep should be of PHOTONIC_MEDIA protocol type");
//            checkPhotPartOfNep(nep);
//        }
//        checkCommonPartOfNep(nep, withSip);
    }


    private void checkCepOtsiRdmNode(OwnedNodeEdgePoint nep, Uuid nepUuid, String portName, String nepName,
            boolean withSip) {
        if (!nep.getUuid().equals(nepUuid)) {
            LOG.info("ERRORUUIDNEP on Nep {}, expected {}", nep.getName(), portName);
        }
        if (nep.augmentation(OwnedNodeEdgePoint1.class) == null) {
            LOG.info("checkCepOtsiRdmNode1211 No CEPList augmentation found for Nep UUID {}, {}", nepUuid,
                nep.getName());
            return;
        }
        Map<ConnectionEndPointKey, ConnectionEndPoint> cepMap = new HashMap<>(
            nep.augmentation(OwnedNodeEdgePoint1.class).getCepList().getConnectionEndPoint());
        assertEquals(nepUuid, cepMap.entrySet().iterator().next().getValue().getParentNodeEdgePoint()
            .getNodeEdgePointUuid(), "Cep parent NodeEdgePoint shall be the tested Nep");
        if (nepName.contains("OMS")) {
            assertEquals(1, cepMap.size(), "OMS nep of RDM infra node should support only 1 OMS cep");
            assertEquals(LayerProtocolName.PHOTONICMEDIA, cepMap.entrySet().iterator().next().getValue()
                .getLayerProtocolName(), "OMS cep LPN shall be PHOTONIC_MEDIA");
            assertEquals(PHOTONICLAYERQUALIFIEROMS.VALUE, cepMap.entrySet().iterator().next().getValue()
                .getLayerProtocolQualifier(), "OMS cep LPQ shall be PHOTONIC_MEDIA_OMS");
        } else if (nepName.contains("OTS")) {
            assertEquals(1, cepMap.size(), "OTS nep of RDM infra node should support only 1 OTS cep");
            assertEquals(LayerProtocolName.PHOTONICMEDIA, cepMap.entrySet().iterator().next().getValue()
                .getLayerProtocolName(), "OTS cep LPN shall be PHOTONIC_MEDIA");
            assertEquals(PHOTONICLAYERQUALIFIEROTS.VALUE, cepMap.entrySet().iterator().next().getValue()
                .getLayerProtocolQualifier(), "OTS cep LPQ shall be PHOTONIC_MEDIA_OTS");
            OtsMediaConnectionEndPointSpec otsMcSpec = cepMap.entrySet().iterator().next().getValue()
                .augmentationOrElseThrow(ConnectionEndPoint2.class).getOtsMediaConnectionEndPointSpec();

            List<ImpairmentRouteEntry> iroIngress = otsMcSpec.getOtsImpairments().stream()
                .filter(imp -> imp.getIngressDirection()).collect(Collectors.toList())
                .iterator().next().getImpairmentRouteEntry();
            assertEquals(9999, iroIngress.iterator().next().getOtsConcentratedLoss().getConcentratedLoss()
                .doubleValue(), "OTS-media-CEP-spec shall be present with 9999 concentrated loss");
            assertEquals(0.0, iroIngress.iterator().next().getOtsFiberSpanImpairments().getConnectorIn().doubleValue(),
                "OTS-media-CEP-spec shall be present with 0 connectorInLoss loss");
            assertEquals(0.0, iroIngress.iterator().next().getOtsFiberSpanImpairments().getConnectorOut().doubleValue(),
                "OTS-media-CEP-spec shall be present with 0 connectorOutLoss loss");
            assertEquals(9999, iroIngress.iterator().next().getOtsFiberSpanImpairments().getLength().intValue(),
                "OTS-media-CEP-spec shall be present with 9999 length");
            assertEquals(0, iroIngress.iterator().next().getOtsFiberSpanImpairments().getPmd().doubleValue(),
                "OTS-media-CEP-spec shall be present with 0 pmd");
            assertEquals(9999.0, iroIngress.iterator().next().getOtsFiberSpanImpairments().getTotalLoss().doubleValue(),
                "OTS-media-CEP-spec shall be present with 9999 Total loss");

            List<ImpairmentRouteEntry> iroEgress = otsMcSpec.getOtsImpairments().stream()
                .filter(imp -> !imp.getIngressDirection()).collect(Collectors.toList())
                .iterator().next().getImpairmentRouteEntry();
            assertEquals(9999, iroEgress.iterator().next().getOtsConcentratedLoss().getConcentratedLoss()
                .doubleValue(), "OTS-media-CEP-spec shall be present with 9999 concentrated loss");
            assertEquals(0.0, iroEgress.iterator().next().getOtsFiberSpanImpairments().getConnectorIn().doubleValue(),
                "OTS-media-CEP-spec shall be present with 0 connectorInLoss loss");
            assertEquals(0.0, iroEgress.iterator().next().getOtsFiberSpanImpairments().getConnectorOut().doubleValue(),
                "OTS-media-CEP-spec shall be present with 0 connectorOutLoss loss");
            assertEquals(9999, iroEgress.iterator().next().getOtsFiberSpanImpairments().getLength().intValue(),
                "OTS-media-CEP-spec shall be present with 9999 length");
            assertEquals(0, iroEgress.iterator().next().getOtsFiberSpanImpairments().getPmd().doubleValue(),
                "OTS-media-CEP-spec shall be present with 0 pmd");
            assertEquals(9999.0, iroEgress.iterator().next().getOtsFiberSpanImpairments().getTotalLoss().doubleValue(),
                "OTS-media-CEP-spec shall be present with 9999 concentrated loss");
        }
//        checkCommonPartOfNep(nep, withSip);
//>>>>>>> 381b4f2d (Consolidate ConnectivityUtils)
    }

    private void rawCheckNep(List<LAYERPROTOCOLQUALIFIER> lpqList, LayerProtocolName lpn, boolean anyInList,
            OwnedNodeEdgePoint nep, Uuid nepUuid, String portName, String nepName, boolean withSip) {
        assertEquals(nepUuid, nep.getUuid(), "bad uuid for " + portName);
        Name name0 = nep.nonnullName().values().stream().findFirst().orElseThrow();
        assertEquals(portName, name0.getValue(), "Value of nep port should be '" + portName + "'");
        assertEquals(nepName, name0.getValueName(), "value-name of nep should be '" + nepName + "'");
        if (lpqList != null) {
            List<LAYERPROTOCOLQUALIFIER> lpql = nep.getSupportedCepLayerProtocolQualifierInstances().stream()
                .map(entry -> entry.getLayerProtocolQualifier())
                .collect(Collectors.toList());
            if (anyInList) {
                assertTrue(
                    lpql.size() < lpqList.size(),
                    //TODO lpqList.size() = 4 here -> check if this is the correct formula from an optical standpoint
                    "eODU nep should support less than " + lpqList.size() + " kind of cep, it depends on client port");
                assertTrue(
                    lpqList.stream().anyMatch(splc -> lpql.contains(splc)),
                    "eODU nep should support 1 kind of cep");
            } else {
                assertEquals(lpqList.size(), lpql.size(), "nep should support " + lpqList.size() + " kind of cep(s)");
                for (LAYERPROTOCOLQUALIFIER lpq: lpqList) {
                    assertThat("nep should support " + lpq +  " cep", lpql, hasItem(lpq));
                }
                assertEquals(lpn, nep.getLayerProtocolName(), "nep should be of " + lpn.toString() + " protocol type");
            }
        }
        // CommonPartOfNep(OwnedNodeEdgePoint nep, boolean withSip)
        assertEquals(Direction.BIDIRECTIONAL, nep.getDirection(), "link port direction should be DIRECTIONAL");
        assertEquals(AdministrativeState.UNLOCKED, nep.getAdministrativeState(),
            "administrative state should be UNLOCKED");
//       TODO: convert this test since terminationState is migrated to CEP attribute in TAPI 2.4
//        assertEquals(TerminationState.TERMINATEDBIDIRECTIONAL, nep.getTerminationState(),
//            "termination state should be TERMINATED BIDIRECTIONAL");
        assertEquals(LifecycleState.INSTALLED, nep.getLifecycleState(), "life-cycle state should be INSTALLED");
        if (withSip) {
            assertEquals(1, nep.getMappedServiceInterfacePoint().size(), "Given nep should support 1 SIP");
        }
//      TODO: convert this test since terminationState is migrated to CEP attribute in TAPI 2.4
//        assertEquals(TerminationDirection.BIDIRECTIONAL, nep.getTerminationDirection(),
//            "termination direction should be BIDIRECTIONAL");
        assertEquals(OperationalState.ENABLED, nep.getOperationalState(),
            "operational state of client nep should be ENABLED");
        assertEquals(PortRole.SYMMETRIC, nep.getLinkPortRole(),
            "link-port-role of client nep should be SYMMETRIC");
    }

    private void checkPhotPartOfNep(OwnedNodeEdgePoint nep) {

        var onep1 = nep.augmentation(
            org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.photonic.media.rev221121.OwnedNodeEdgePoint1.class);

        Map<SupportableSpectrumKey, SupportableSpectrum> supSpectrum = onep1.getPhotonicMediaNodeEdgePointSpec()
            .getSpectrumCapabilityPac().getSupportableSpectrum();

        double naz = 0.01;
        assertEquals(supSpectrum.entrySet().stream().findFirst().orElseThrow().getValue().getLowerFrequency()
            .doubleValue(), (Uint64.valueOf(Math.round(GridConstant.START_EDGE_FREQUENCY * 1E12 + naz))).doubleValue(),
            "Lower Freq of supportable spectrum shall be 191.325 THz");
        assertEquals(supSpectrum.entrySet().stream().findFirst().orElseThrow().getValue().getUpperFrequency()
            .doubleValue(), (Uint64.valueOf(Math.round(GridConstant.START_EDGE_FREQUENCY * 1E12
                + GridConstant.GRANULARITY * GridConstant.EFFECTIVE_BITS * 1E09 + naz))).doubleValue(),
            "Higher Freq of supportable spectrum shall be 196.100 THz");

        Map<AvailableSpectrumKey, AvailableSpectrum> availSpectrum = onep1.getPhotonicMediaNodeEdgePointSpec()
            .getSpectrumCapabilityPac().getAvailableSpectrum();
        assertEquals(availSpectrum.entrySet().stream().findFirst().orElseThrow().getValue().getLowerFrequency()
            .doubleValue(), (Uint64.valueOf(Math.round(GridConstant.START_EDGE_FREQUENCY * 1E12 + naz))).doubleValue(),
            "In absence of service provisionning Lower Freq of available spectrum shall be 191.325 THz");
        assertEquals(availSpectrum.entrySet().stream().findFirst().orElseThrow().getValue().getUpperFrequency()
            .doubleValue(), (Uint64.valueOf(Math.round(GridConstant.START_EDGE_FREQUENCY * 1E12
                + GridConstant.GRANULARITY * GridConstant.EFFECTIVE_BITS * 1E09 + naz))).doubleValue(),
            "In absence of service provisionning Higher Freq of available spectrum shall be 191.325 THz");

        Map<OccupiedSpectrumKey, OccupiedSpectrum> occSpectrum = onep1.getPhotonicMediaNodeEdgePointSpec()
            .getSpectrumCapabilityPac().getOccupiedSpectrum();
        assertEquals(occSpectrum, null,
            "In absence of service provisionning occupied spectrum shall be null");

    }

    private void checkOmsLink(org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.topology.Link link,
            Uuid node1Uuid, Uuid node2Uuid, Uuid tp1Uuid, Uuid tp2Uuid, Uuid linkUuid, String linkName) {
        assertEquals(linkName, link.getName().get(new NameKey("OMS link name")).getValue(), "bad name for the link");
        linkNepsCheck(link, node1Uuid, node2Uuid, tp1Uuid, tp2Uuid, linkUuid);
    }

    private void checkXpdrRdmLink(
            org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.topology.Link link,
            Uuid node1Uuid, Uuid node2Uuid, Uuid tp1Uuid, Uuid tp2Uuid, Uuid linkUuid, String linkName) {
        assertEquals(linkName, link.getName().get(new NameKey("XPDR-RDM link name")).getValue(),
            "bad name for the link");
        linkNepsCheck(link, node1Uuid, node2Uuid, tp1Uuid, tp2Uuid, linkUuid);
    }

    private void linkNepsCheck(
            org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.topology.Link link,
            Uuid node1Uuid, Uuid node2Uuid, Uuid tp1Uuid, Uuid tp2Uuid, Uuid linkUuid) {
        assertEquals(linkUuid, link.getUuid(), "bad uuid for link");
        assertEquals(
            LayerProtocolName.PHOTONICMEDIA.getName(),
            link.getLayerProtocolName().stream().findFirst().orElseThrow().getName(),
            "oms link should be between 2 nodes of protocol layers PHOTONIC_MEDIA");
        assertEquals(ForwardingDirection.BIDIRECTIONAL, link.getDirection(),
            "otn tapi link should be BIDIRECTIONAL");
        var nodeEdgePointList = link.nonnullNodeEdgePoint().values().stream().collect(Collectors.toList());
        assertEquals(2 , nodeEdgePointList.size(), "oms link should be between 2 neps");
        var nep0 = nodeEdgePointList.get(0);
        var nep1 = nodeEdgePointList.get(1);
        assertEquals(topologyUuid, nep0.getTopologyUuid(),
            "topology uuid should be the same for the two termination point of the link");
        assertEquals(topologyUuid, nep1.getTopologyUuid(),
            "topology uuid should be the same for the two termination point of the link");
        String node1UuidVal = node1Uuid.getValue();
        String node2UuidVal = node2Uuid.getValue();
        assertThat("oms links should terminate on two distinct nodes",
            nep0.getNodeUuid().getValue(), either(containsString(node1UuidVal)).or(containsString(node2UuidVal)));
        assertThat("oms links should terminate on two distinct nodes",
            nep1.getNodeUuid().getValue(), either(containsString(node1UuidVal)).or(containsString(node2UuidVal)));
        String tp1UuidVal = tp1Uuid.getValue();
        String tp2UuidVal = tp2Uuid.getValue();
        assertThat("oms links should terminate on two distinct tps",
            nep0.getNodeEdgePointUuid().getValue(), either(containsString(tp1UuidVal)).or(containsString(tp2UuidVal)));
        assertThat("oms links should terminate on two distinct tps",
            nep1.getNodeEdgePointUuid().getValue(), either(containsString(tp1UuidVal)).or(containsString(tp2UuidVal)));
    }

    private Node changeTerminationPointState(
            Node initialNode, String tpid, String tpid1, AdminStates admin, State oper) {
        var tpdr1Bldr = new org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226
                .Node1Builder(initialNode.augmentation(Node1.class));
        Map<TerminationPointKey, TerminationPoint> tps = new HashMap<>(tpdr1Bldr.getTerminationPoint());
        TerminationPointBuilder tpBldr = new TerminationPointBuilder(tps.get(new TerminationPointKey(new TpId(tpid))));
        tpBldr.addAugmentation(
            new TerminationPoint1Builder(tpBldr.augmentation(TerminationPoint1.class))
                .setAdministrativeState(admin)
                .setOperationalState(oper)
                .build());
        tps.replace(tpBldr.key(), tpBldr.build());
        TerminationPointBuilder tpBldr1 =
            new TerminationPointBuilder(tps.get(new TerminationPointKey(new TpId(tpid1))));
        tpBldr1.addAugmentation(
            new TerminationPoint1Builder(tpBldr1.augmentation(TerminationPoint1.class))
                .setAdministrativeState(admin)
                .setOperationalState(oper)
                .build());
        tps.replace(tpBldr1.key(), tpBldr1.build());
        return new NodeBuilder(initialNode).addAugmentation(tpdr1Bldr.setTerminationPoint(tps).build()).build();
    }

    private OwnedNodeEdgePoint getOnep(String searchedChar, List<OwnedNodeEdgePoint> onepList) {
        for (OwnedNodeEdgePoint onep : onepList) {
            for (Name name : onep.getName().values()) {
                if (name.getValue().contains(searchedChar)) {
                    return onep;
                }
            }
        }
        LOG.info("pattern '{}' not found in list of OwnedNodeEdgePoint", searchedChar);
        return null;
    }

    private org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.topology.Node getNode(
            String searchedChar,
            List<org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.topology.Node> nodeList) {
        for (var node : nodeList) {
            for (Name name : node.getName().values()) {
                if (name.getValue().contains(searchedChar)) {
                    return node;
                }
            }
        }
        LOG.info("pattern '{}' not found in list of nodes", searchedChar);
        return null;
    }

    @Test
    void getIdBasedOnModelVersion() {
        ConvertORTopoToTapiFullTopo convertORTopoToTapiFullTopo = new ConvertORTopoToTapiFullTopo(
                topologyUuid,
                tapiLink);

        assertTrue(
                "ROADM-A".equals(convertORTopoToTapiFullTopo.getIdBasedOnModelVersion("ROADM-A-SRG1"))
        );

        assertTrue(
                "ROADMA".equals(convertORTopoToTapiFullTopo.getIdBasedOnModelVersion("ROADMA-SRG1"))
        );
    }

}
