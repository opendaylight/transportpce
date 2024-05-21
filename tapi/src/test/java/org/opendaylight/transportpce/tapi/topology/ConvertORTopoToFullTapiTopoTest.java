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
import java.util.Collection;
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
import org.opendaylight.transportpce.common.network.NetworkTransactionImpl;
import org.opendaylight.transportpce.common.network.NetworkTransactionService;
import org.opendaylight.transportpce.tapi.TapiStringConstants;
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
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.networks.network.Link;
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
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.digital.otn.rev221121.ODUTYPEODU0;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.digital.otn.rev221121.ODUTYPEODU2;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.digital.otn.rev221121.ODUTYPEODU2E;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.digital.otn.rev221121.ODUTYPEODU4;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.dsr.rev221121.DIGITALSIGNALTYPE100GigE;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.dsr.rev221121.DIGITALSIGNALTYPE10GigELAN;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.photonic.media.rev221121.PHOTONICLAYERQUALIFIEROMS;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.photonic.media.rev221121.PHOTONICLAYERQUALIFIEROTS;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.photonic.media.rev221121.PHOTONICLAYERQUALIFIEROTSi;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.FORWARDINGRULECANNOTFORWARDACROSSGROUP;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.FORWARDINGRULEMAYFORWARDACROSSGROUP;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.RuleType;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.node.NodeRuleGroup;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.node.OwnedNodeEdgePoint;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.node.OwnedNodeEdgePointKey;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.node.rule.group.NodeEdgePoint;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.node.rule.group.Rule;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
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
            TapiTopologyDataUtils.OPENROADM_NETWORK_FILE, InstanceIdentifiers.UNDERLAY_NETWORK_II);
        TopologyDataUtils.writeTopologyFromFileToDatastore(getDataStoreContextUtil(),
            TapiTopologyDataUtils.OTN_TOPOLOGY_FILE, InstanceIdentifiers.OTN_NETWORK_II);
        TopologyDataUtils.writePortmappingFromFileToDatastore(getDataStoreContextUtil(),
            TapiTopologyDataUtils.PORTMAPPING_FILE);

        otnMuxA  = dataBroker.newReadOnlyTransaction()
            .read(
                LogicalDatastoreType.CONFIGURATION,
                InstanceIdentifier.create(Networks.class)
                    .child(org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226
                            .networks.Network.class,
                        new NetworkKey(new NetworkId("otn-topology")))
                .child(Node.class, new NodeKey(new NodeId("SPDR-SA1-XPDR1"))))
            .get().orElseThrow();

        /*KeyedInstanceIdentifier<Node, NodeKey> muxCIID = InstanceIdentifier.create(Networks.class)
            .child(org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.networks.Network
                .class, new NetworkKey(new NetworkId("otn-topology")))
            .child(Node.class, new NodeKey(new NodeId("SPDR-SC1-XPDR1")));
        FluentFuture<Optional<Node>> muxCFuture = dataBroker.newReadOnlyTransaction()
            .read(LogicalDatastoreType.CONFIGURATION, muxCIID);
        muxCFuture.get().orElseThrow();*/

        otnSwitch = dataBroker.newReadOnlyTransaction()
            .read(
                LogicalDatastoreType.CONFIGURATION,
                InstanceIdentifier.create(Networks.class)
                    .child(org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226
                            .networks.Network.class,
                        new NetworkKey(new NetworkId("otn-topology")))
                .child(Node.class, new NodeKey(new NodeId("SPDR-SA1-XPDR2"))))
            .get().orElseThrow();
        roadmA = dataBroker.newReadOnlyTransaction()
            .read(
                LogicalDatastoreType.CONFIGURATION,
                InstanceIdentifier.create(Networks.class)
                    .child(org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226
                            .networks.Network.class,
                        new NetworkKey(new NetworkId("openroadm-network")))
                    .child(Node.class, new NodeKey(new NodeId("ROADM-A1"))))
            .get().orElseThrow();
        roadmC = dataBroker.newReadOnlyTransaction()
            .read(
                LogicalDatastoreType.CONFIGURATION,
                InstanceIdentifier.create(Networks.class)
                    .child(org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226
                            .networks.Network.class,
                        new NetworkKey(new NetworkId("openroadm-network")))
                    .child(Node.class, new NodeKey(new NodeId("ROADM-C1"))))
            .get().orElseThrow();

        tpdr100G = dataBroker.newReadOnlyTransaction()
            .read(
                LogicalDatastoreType.CONFIGURATION,
                InstanceIdentifier.create(Networks.class)
                    .child(org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226
                            .networks.Network.class,
                        new NetworkKey(new NetworkId("otn-topology")))
                    .child(Node.class, new NodeKey(new NodeId("XPDR-A1-XPDR1"))))
            .get().orElseThrow();

        /*InstanceIdentifier<Network1> linksIID = InstanceIdentifier.create(Networks.class)
            .child(org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.networks.Network
                .class, new NetworkKey(new NetworkId("otn-topology")))
            .augmentation(Network1.class);
        FluentFuture<Optional<Network1>> linksFuture = dataBroker.newReadOnlyTransaction()
            .read(LogicalDatastoreType.CONFIGURATION, linksIID);
        linksFuture.get().orElseThrow().getLink();*/

        ortopoLinks = dataBroker.newReadOnlyTransaction()
            .read(
                LogicalDatastoreType.CONFIGURATION,
                InstanceIdentifier.create(Networks.class)
                    .child(org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226
                            .networks.Network.class,
                        new NetworkKey(new NetworkId("openroadm-topology")))
                .augmentation(Network1.class))
            .get().orElseThrow().getLink();
        openroadmNet =  dataBroker.newReadOnlyTransaction()
            .read(
                LogicalDatastoreType.CONFIGURATION,
                InstanceIdentifier.create(Networks.class)
                    .child(org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226
                            .networks.Network.class,
                        new NetworkKey(new NetworkId("openroadm-topology"))))
            .get().orElseThrow();

        topologyUuid = new Uuid(UUID.nameUUIDFromBytes(
                TapiStringConstants.T0_FULL_MULTILAYER.getBytes(Charset.forName("UTF-8")))
            .toString());
        networkTransactionService = new NetworkTransactionImpl(getDataBroker());
        tapiLink = new TapiLinkImpl(networkTransactionService);
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
        ConvertORToTapiTopology tapiFactory = new ConvertORToTapiTopology(topologyUuid);
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
        List<org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.topology.Node> tapiNodes
            = tapiFullFactory.getTapiNodes().values().stream().collect(Collectors.toList());
        Uuid roadmNodeUuid = new Uuid(UUID.nameUUIDFromBytes((roadmA.getNodeId().getValue() + "+PHOTONIC_MEDIA")
            .getBytes(Charset.forName("UTF-8"))).toString());
        checkOtsiNode(tapiNodes.get(getNodeRank("ROADM-A1", tapiNodes)), roadmNodeUuid, "roadm", "ROADM-A1");
    }

    @Test
    void convertNodeForRoadmWhenRoadmNeighborAttached() {
        ConvertORTopoToTapiFullTopo tapiFullFactory = new ConvertORTopoToTapiFullTopo(topologyUuid, tapiLink);
        tapiFullFactory.convertRoadmNode(roadmA, openroadmNet, "Full");
        tapiFullFactory.convertRoadmNode(roadmC, openroadmNet, "Full");

        List<Link> rdmTordmLinkList = ortopoLinks.values().stream()
            .filter(lk -> lk.augmentation(Link1.class).getLinkType().equals(OpenroadmLinkType.ROADMTOROADM))
            .collect(Collectors.toList());
        tapiFullFactory.convertRdmToRdmLinks(rdmTordmLinkList);

        assertEquals(2, tapiFullFactory.getTapiNodes().size(), "Node list size should be 2");
        assertEquals(1, tapiFullFactory.getTapiLinks().size(), "Link list size should be 1");

        List<org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.topology.Node> tapiNodes
            = tapiFullFactory.getTapiNodes().values().stream().collect(Collectors.toList());
        int myInt = 0;
        for (org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.topology.Node node : tapiNodes) {
            if (node.getLayerProtocolName().contains(LayerProtocolName.PHOTONICMEDIA)
                    && !node.getLayerProtocolName().contains(LayerProtocolName.DSR)) {
                LOG.info("LOOP ROADM node found at rank {}, with Name {} and Uuid {}",
                    myInt, node.getName(), node.getUuid());
            }
            myInt++;
        }
        Uuid roadmaNodeUuid = new Uuid(UUID.nameUUIDFromBytes((roadmA.getNodeId().getValue() + "+PHOTONIC_MEDIA")
            .getBytes(Charset.forName("UTF-8"))).toString());
        LOG.info("ROADM node found at rank {} from getrank", getNodeRank("ROADM-A1", tapiNodes));
        checkOtsiNode(tapiNodes.get(getNodeRank("ROADM-A1", tapiNodes)), roadmaNodeUuid, "roadm", "ROADM-A1");

        List<org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.topology.Link> links
            = tapiFullFactory.getTapiLinks().values().stream()
            .sorted((l1, l2) -> l1.getUuid().getValue().compareTo(l2.getUuid().getValue()))
            .collect(Collectors.toList());
        Uuid node1Uuid = new Uuid(UUID.nameUUIDFromBytes("ROADM-A1+PHOTONIC_MEDIA".getBytes(Charset.forName("UTF-8")))
            .toString());
        Uuid node2Uuid = new Uuid(UUID.nameUUIDFromBytes("ROADM-C1+PHOTONIC_MEDIA".getBytes(Charset.forName("UTF-8")))
            .toString());
        Uuid tp1Uuid = new Uuid(UUID.nameUUIDFromBytes("ROADM-A1+PHOTONIC_MEDIA_OTS+DEG2-TTP-TXRX"
            .getBytes(Charset.forName("UTF-8"))).toString());
        Uuid tp2Uuid = new Uuid(UUID.nameUUIDFromBytes(("ROADM-C1+PHOTONIC_MEDIA_OTS+DEG1-TTP-TXRX")
            .getBytes(Charset.forName("UTF-8"))).toString());
        Uuid linkUuid =
            new Uuid(UUID.nameUUIDFromBytes(
                "ROADM-C1+PHOTONIC_MEDIA_OTS+DEG1-TTP-TXRXtoROADM-A1+PHOTONIC_MEDIA_OTS+DEG2-TTP-TXRX"
                    .getBytes(Charset.forName("UTF-8"))).toString());
        checkOmsLink(links.get(0), node1Uuid, node2Uuid, tp1Uuid, tp2Uuid, linkUuid,
            "ROADM-C1+PHOTONIC_MEDIA_OTS+DEG1-TTP-TXRXtoROADM-A1+PHOTONIC_MEDIA_OTS+DEG2-TTP-TXRX");
    }

    @Test
    void convertNodeForRoadmWhenOtnMuxAttached() {
        ConvertORTopoToTapiFullTopo tapiFullFactory = new ConvertORTopoToTapiFullTopo(topologyUuid, tapiLink);
        ConvertORToTapiTopology tapiFactory = new ConvertORToTapiTopology(topologyUuid);
        List<String> networkPortListA = new ArrayList<>();
        for (TerminationPoint tp : otnMuxA.augmentation(Node1.class).getTerminationPoint().values()) {
            if (tp.augmentation(TerminationPoint1.class).getTpType().equals(OpenroadmTpType.XPONDERNETWORK)) {
                networkPortListA.add(tp.getTpId().getValue());
            }
        }
        tapiFactory.convertNode(otnMuxA, networkPortListA);
        tapiFullFactory.setTapiNodes(tapiFactory.getTapiNodes());
        tapiFullFactory.convertRoadmNode(roadmA, openroadmNet, "Full");
        List<Link> xponderOutLinkList = ortopoLinks.values().stream()
            .filter(lk -> lk.augmentation(Link1.class).getLinkType().equals(OpenroadmLinkType.XPONDEROUTPUT))
            .filter(lk1 -> ((lk1.getSource().getSourceNode().equals(otnMuxA.getNodeId())
                    || lk1.getSource().getSourceNode().getValue().contains(roadmA.getNodeId().getValue()))
                && (lk1.getDestination().getDestNode().equals(otnMuxA.getNodeId())
                    || lk1.getDestination().getDestNode().getValue().contains(roadmA.getNodeId().getValue()))))
            .collect(Collectors.toList());
        List<Link> xponderInLinkList = ortopoLinks.values().stream()
            .filter(lk -> lk.augmentation(Link1.class).getLinkType().equals(OpenroadmLinkType.XPONDERINPUT))
            .filter(lk1 -> ((lk1.getSource().getSourceNode().equals(otnMuxA.getNodeId())
                    || lk1.getSource().getSourceNode().getValue().contains(roadmA.getNodeId().getValue()))
                && (lk1.getDestination().getDestNode().equals(otnMuxA.getNodeId())
                    || lk1.getDestination().getDestNode().getValue().contains(roadmA.getNodeId().getValue()))))
            .collect(Collectors.toList());
        xponderInLinkList.addAll(xponderOutLinkList);
        tapiFullFactory.convertXpdrToRdmLinks(xponderInLinkList);
        assertEquals(2, tapiFullFactory.getTapiNodes().size(),
            "Node list size should be 2 (XPDR, DSR-ODU merged; ROADM)");
        assertEquals(1, tapiFullFactory.getTapiLinks().size(),
            "Link list size should be 1 : no more transitional link");
        Map<org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.topology.NodeKey,
            org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.topology.Node> nodeMap =
            tapiFactory.getTapiNodes();
        nodeMap.putAll(tapiFullFactory.getTapiNodes());
        List<org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.topology.Node> tapiNodes
            = nodeMap.values().stream()
            .sorted((n1, n2) -> n1.getUuid().getValue().compareTo(n2.getUuid().getValue()))
            .collect(Collectors.toList());
        Uuid roadmNodeUuid = new Uuid(UUID.nameUUIDFromBytes((roadmA.getNodeId().getValue() + "+PHOTONIC_MEDIA")
            .getBytes(Charset.forName("UTF-8"))).toString());
        checkOtsiNode(tapiNodes.get(getNodeRank("ROADM-A1", tapiNodes)), roadmNodeUuid, "roadm", "ROADM-A1");

        Uuid node1Uuid = new Uuid(UUID.nameUUIDFromBytes("SPDR-SA1-XPDR1+XPONDER".getBytes(Charset.forName("UTF-8")))
            .toString());
        Uuid node2Uuid = new Uuid(UUID.nameUUIDFromBytes("ROADM-A1+PHOTONIC_MEDIA".getBytes(Charset.forName("UTF-8")))
            .toString());
        LOG.info("ROADM-A1+PHOTONIC_MEDIA UUID is {}", node2Uuid);
        Uuid tp1Uuid = new Uuid(UUID.nameUUIDFromBytes("SPDR-SA1-XPDR1+PHOTONIC_MEDIA_OTS+XPDR1-NETWORK1"
            .getBytes(Charset.forName("UTF-8"))).toString());
        Uuid tp2Uuid = new Uuid(UUID.nameUUIDFromBytes(("ROADM-A1+PHOTONIC_MEDIA_OTS+SRG1-PP2-TXRX")
            .getBytes(Charset.forName("UTF-8"))).toString());
        Uuid linkUuid =
            new Uuid(UUID.nameUUIDFromBytes(
                "ROADM-A1+PHOTONIC_MEDIA_OTS+SRG1-PP2-TXRXtoSPDR-SA1-XPDR1+PHOTONIC_MEDIA_OTS+XPDR1-NETWORK1"
                    .getBytes(Charset.forName("UTF-8"))).toString());
        List<org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.topology.Link> links
            = tapiFullFactory.getTapiLinks().values().stream()
            .sorted((l1, l2) -> l1.getUuid().getValue().compareTo(l2.getUuid().getValue()))
            .collect(Collectors.toList());
        checkXpdrRdmLink(links.get(0), node1Uuid, node2Uuid, tp1Uuid, tp2Uuid, linkUuid,
            "ROADM-A1+PHOTONIC_MEDIA_OTS+SRG1-PP2-TXRXtoSPDR-SA1-XPDR1+PHOTONIC_MEDIA_OTS+XPDR1-NETWORK1");
    }

    private void rawConvertNode(Node node0, String dsrNodeType, String nodeId) {
        ConvertORToTapiTopology tapiFactory = new ConvertORToTapiTopology(topologyUuid);
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
        //    Uuid node9Uuid, String dsrNodeType, String nodeId) {
        Uuid node9Uuid =
            new Uuid(UUID.nameUUIDFromBytes((nodeId + "+XPONDER").getBytes(Charset.forName("UTF-8"))).toString());
        var node =
            tapiFactory.getTapiNodes().values().stream()
                .sorted((n1, n2) -> n1.getUuid().getValue().compareTo(n2.getUuid().getValue()))
                .findFirst().orElseThrow();
        assertEquals(node9Uuid, node.getUuid(), "incorrect node uuid");
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
        List<OwnedNodeEdgePoint> inepsN = node.nonnullOwnedNodeEdgePoint().values().stream()
            .filter(n -> n.getName().containsKey(new NameKey("iNodeEdgePoint_N")))
            .sorted((nep1, nep2) -> nep1.getUuid().getValue().compareTo(nep2.getUuid().getValue()))
            .collect(Collectors.toList());
        List<OwnedNodeEdgePoint> enepsN = node.nonnullOwnedNodeEdgePoint().values().stream()
            .filter(n -> n.getName().containsKey(new NameKey("eNodeEdgePoint_N")))
            .sorted((nep1, nep2) -> nep1.getUuid().getValue().compareTo(nep2.getUuid().getValue()))
            .collect(Collectors.toList());
        List<OwnedNodeEdgePoint> nepsC;
        switch (dsrNodeType) {
            case "switch":
                nepsC = node.nonnullOwnedNodeEdgePoint().values().stream()
                    .filter(n -> n.getName().containsKey(new NameKey("NodeEdgePoint_C")))
                    .sorted((nep1, nep2) -> nep1.getUuid().getValue().compareTo(nep2.getUuid().getValue()))
                    .collect(Collectors.toList());
                assertEquals(4, enepsN.size(), "Switch-DSR node should have 4 eNEPs network");
                assertEquals(4, inepsN.size(), "Switch-DSR node should have 4 iNEPs network");
                assertEquals(4, nepsC.size(), "Switch-DSR node should have 4 NEPs client");
                OwnedNodeEdgePoint nep1 = nepsC.get(2);
                Uuid client4NepUuid = new Uuid(
                    UUID.nameUUIDFromBytes((nodeId + "+DSR+XPDR2-CLIENT4").getBytes(Charset.forName("UTF-8")))
                        .toString());
                checkNepClient100GSwitch(nep1, client4NepUuid, nodeId + "+DSR+XPDR2-CLIENT4", "NodeEdgePoint_C");
                OwnedNodeEdgePoint enep2 = enepsN.get(2);
                OwnedNodeEdgePoint inep2 = inepsN.get(3);
                Uuid enetworkNepUuid = new Uuid(
                    UUID.nameUUIDFromBytes((nodeId + "+eODU+XPDR2-CLIENT4").getBytes(Charset.forName("UTF-8")))
                        .toString());
                Uuid inetworkNepUuid = new Uuid(
                    UUID.nameUUIDFromBytes((nodeId + "+iODU+XPDR2-NETWORK1").getBytes(Charset.forName("UTF-8")))
                        .toString());
                checkNepeODU4(enep2, enetworkNepUuid, nodeId + "+eODU+XPDR2-CLIENT4", "eNodeEdgePoint_N", false);
                checkNepNetworkODU4(inep2, inetworkNepUuid, nodeId + "+iODU+XPDR2-NETWORK1", "iNodeEdgePoint_N", true);
                List<NodeRuleGroup> nrgList = node.nonnullNodeRuleGroup().values().stream()
                    .sorted((nrg1, nrg2) -> nrg1.getUuid().getValue().compareTo(nrg2.getUuid().getValue()))
                    .collect(Collectors.toList());
// keep trace of the previous test performed before the structure of the NRG was modified
//                checkNodeRuleGroupForSwitchDSR(nrgList, client4NepUuid, enetworkNepUuid, node9Uuid);
                checkNodeRuleGroupForSwitchDSR(nrgList, client4NepUuid, inetworkNepUuid, node9Uuid);
                break;
            case "mux":
                nepsC = node.nonnullOwnedNodeEdgePoint().values().stream()
                    .filter(n -> n.getName().containsKey(new NameKey("NodeEdgePoint_C")))
                    .sorted((nep3, nep4) -> nep3.getUuid().getValue().compareTo(nep4.getUuid().getValue()))
                    .collect(Collectors.toList());
                assertEquals(4, enepsN.size(), "Mux-DSR node should have 4 eNEP network");
                assertEquals(1, inepsN.size(), "Mux-DSR node should have 1 iNEP network");
                assertEquals(4, nepsC.size(), "Mux-DSR node should have 4 NEPs client");
                OwnedNodeEdgePoint nep3 = nepsC.get(2);
                Uuid client3NepUuid = new Uuid(
                    UUID.nameUUIDFromBytes((nodeId + "+DSR+XPDR1-CLIENT3").getBytes(Charset.forName("UTF-8")))
                        .toString());
                checkNepClient10G(nep3, client3NepUuid, nodeId + "+DSR+XPDR1-CLIENT3", "NodeEdgePoint_C");
                OwnedNodeEdgePoint enep4 = enepsN.get(3);
                OwnedNodeEdgePoint inep4 = inepsN.get(0);
                Uuid eclientNepUuid2 = new Uuid(
                    UUID.nameUUIDFromBytes((nodeId + "+eODU+XPDR1-CLIENT3").getBytes(Charset.forName("UTF-8")))
                        .toString());
                Uuid inetworkNepUuid2 = new Uuid(
                    UUID.nameUUIDFromBytes((nodeId + "+iODU+XPDR1-NETWORK1").getBytes(Charset.forName("UTF-8")))
                        .toString());
                checkNepeODU4(enep4, eclientNepUuid2, nodeId + "+eODU+XPDR1-CLIENT3", "eNodeEdgePoint_N", false);
                checkNepNetworkODU4(inep4, inetworkNepUuid2, nodeId + "+iODU+XPDR1-NETWORK1", "iNodeEdgePoint_N",
                    true);
                List<NodeRuleGroup> nrgList2 = node.nonnullNodeRuleGroup().values().stream()
                    .sorted((nrg1, nrg2) -> nrg1.getUuid().getValue().compareTo(nrg2.getUuid().getValue()))
                    .collect(Collectors.toList());
// keep trace of the previous test performed before the structure of the NRG was modified
//                checkNodeRuleGroupForMuxDSR(nrgList2, client3NepUuid, eclientNepUuid2, node9Uuid);
                checkNodeRuleGroupForMuxDSR(nrgList2, client3NepUuid, inetworkNepUuid2, node9Uuid);
                break;
            case "tpdr":
                nepsC = node.nonnullOwnedNodeEdgePoint().values().stream()
                    .filter(n -> n.getName().containsKey(new NameKey("100G-tpdr")))
                    .sorted((nep5, nep6) -> nep5.getUuid().getValue().compareTo(nep6.getUuid().getValue()))
                    .collect(Collectors.toList());
                assertEquals(2, enepsN.size(), "Tpdr-DSR node should have 2 eNEPs network");
                assertEquals(2, inepsN.size(), "Tpdr-DSR node should have 2 iNEPs network");
                assertEquals(2, nepsC.size(), "Tpdr-DSR node should have 2 NEPs client");
                OwnedNodeEdgePoint nep5 = nepsC.get(0);
                Uuid client1NepUuid = new Uuid(
                    UUID.nameUUIDFromBytes((nodeId + "+DSR+XPDR1-CLIENT1").getBytes(Charset.forName("UTF-8")))
                        .toString());
                checkNepClient100GTpdr(nep5, client1NepUuid, nodeId + "+DSR+XPDR1-CLIENT1", "100G-tpdr");
                OwnedNodeEdgePoint enep6 = enepsN.get(0);
                OwnedNodeEdgePoint inep6 = inepsN.get(1);
                Uuid enetworkNepUuid3 = new Uuid(
                    UUID.nameUUIDFromBytes((nodeId + "+eODU+XPDR1-CLIENT1").getBytes(Charset.forName("UTF-8")))
                        .toString());
                Uuid inetworkNepUuid3 = new Uuid(
                    UUID.nameUUIDFromBytes((nodeId + "+iODU+XPDR1-NETWORK1").getBytes(Charset.forName("UTF-8")))
                        .toString());
                checkNepeODU4(enep6, enetworkNepUuid3, nodeId + "+eODU+XPDR1-CLIENT1", "eNodeEdgePoint_N", false);
                checkNepNetworkODU4(inep6, inetworkNepUuid3, nodeId + "+iODU+XPDR1-NETWORK1", "iNodeEdgePoint_N",
                    true);
                List<NodeRuleGroup> nrgList3 = node.nonnullNodeRuleGroup().values().stream()
                    .sorted((nrg1, nrg2) -> nrg1.getUuid().getValue().compareTo(nrg2.getUuid().getValue()))
                    .collect(Collectors.toList());
// keep trace of the previous test performed before the structure of the NRG was modified
//                checkNodeRuleGroupForTpdrDSR(nrgList3, client1NepUuid, enetworkNepUuid3, node9Uuid);
                checkNodeRuleGroupForTpdrDSR(nrgList3, client1NepUuid, inetworkNepUuid3, node9Uuid);
                break;
            default:
                fail();
                break;
        }
    }

    private void checkOtsiNode(
            org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.topology.Node node,
            Uuid nodeUuid, String otsiNodeType, String nodeId) {
        if (!node.getUuid().equals(nodeUuid)) {
            LOG.info("ERRORUUID on Node.getNodeId {}, NodeId {}", node.getName(), nodeId);
            LOG.info("ERRORUUID TapiUuid {}, transmitted Node Uuid {}", node.getUuid(), nodeUuid);
        }
        assertEquals(nodeUuid, node.getUuid(), "incorrect node uuid");
        List<OwnedNodeEdgePoint> nepsI = null;
        List<OwnedNodeEdgePoint> nepsE = null;
        List<OwnedNodeEdgePoint> nepsP = null;
        List<OwnedNodeEdgePoint> nepsOMS = null;
        List<OwnedNodeEdgePoint> nepsOTS = null;
        List<OwnedNodeEdgePoint> nepsPhot = null;
        if (!otsiNodeType.equals("roadm")) {
            assertEquals(nodeId + "+XPONDER", node.getName().get(new NameKey("dsr/odu node name")).getValue(),
                "incorrect node name");
            assertThat("one value-name should be 'dsr/odu node name'",
                new ArrayList<>(node.nonnullName().keySet()), hasItem(new NameKey("dsr/odu node name")));
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
        } else {
            assertEquals(nodeId + "+PHOTONIC_MEDIA", node.getName().get(new NameKey("roadm node name")).getValue(),
                "incorrect node name");
            assertThat("one value-name should be 'dsr/odu node name'",
                new ArrayList<>(node.nonnullName().keySet()), hasItem(new NameKey("roadm node name")));
            //TODO this variable are only accessed in switch/case block -> report this in the right place
            nepsOMS = node.nonnullOwnedNodeEdgePoint().values().stream()
                .filter(n -> n.getName().containsKey(new NameKey("PHOTONIC_MEDIA_OMSNodeEdgePoint")))
                .sorted((nep1, nep2) -> nep1.getUuid().getValue().compareTo(nep2.getUuid().getValue()))
                .collect(Collectors.toList());
            nepsOTS = node.nonnullOwnedNodeEdgePoint().values().stream()
                .filter(n -> n.getName().containsKey(new NameKey("PHOTONIC_MEDIA_OTSNodeEdgePoint")))
                .sorted((nep1, nep2) -> nep1.getUuid().getValue().compareTo(nep2.getUuid().getValue()))
                .collect(Collectors.toList());
            nepsPhot = node.nonnullOwnedNodeEdgePoint().values().stream()
                .filter(n -> n.getName().containsKey(new NameKey("PHOTONIC_MEDIA_OMSNodeEdgePoint")))
                .sorted((nep1, nep2) -> nep1.getUuid().getValue().compareTo(nep2.getUuid().getValue()))
                .collect(Collectors.toList());
            nepsPhot.addAll(node.nonnullOwnedNodeEdgePoint().values().stream()
                .filter(n -> n.getName().containsKey(new NameKey("PHOTONIC_MEDIA_OTSNodeEdgePoint")))
                .sorted((nep1, nep2) -> nep1.getUuid().getValue().compareTo(nep2.getUuid().getValue()))
                .collect(Collectors.toList()));
        }
        assertEquals(AdministrativeState.UNLOCKED, node.getAdministrativeState(),
            "administrative state should be UNLOCKED");
        assertEquals(LifecycleState.INSTALLED, node.getLifecycleState(), "life-cycle state should be INSTALLED");
        assertEquals(OperationalState.ENABLED, node.getOperationalState(), "operational state should be ENABLED");
        assertEquals(1, node.getLayerProtocolName().size(),
            "otsi node should manage a single protocol layer : PHOTONIC_MEDIA");
        assertEquals(LayerProtocolName.PHOTONICMEDIA, node.getLayerProtocolName().stream().findFirst().orElseThrow(),
            "otsi node should manage a single protocol layer : PHOTONIC_MEDIA");

        switch (otsiNodeType) {
            case "switch":
                assertEquals(4, nepsE.size(), "Switch-OTSi node should have 4 eNEPs");
                assertEquals(4, nepsI.size(), "Switch-OTSi node should have 4 iNEPs");
                assertEquals(4, nepsP.size(), "Switch-OTSi node should have 4 photNEPs");
                OwnedNodeEdgePoint nep1 = nepsI.get(1);
                Uuid inepUuid = new Uuid(
                    UUID.nameUUIDFromBytes((nodeId + "+iOTSi+XPDR2-NETWORK2").getBytes(Charset.forName("UTF-8")))
                        .toString());
                checkNepOtsiNode(nep1, inepUuid, nodeId + "+iOTSi+XPDR2-NETWORK2", "iNodeEdgePoint", true);
                OwnedNodeEdgePoint nep2 = nepsE.get(0);
                Uuid enepUuid = new Uuid(
                    UUID.nameUUIDFromBytes((nodeId + "+PHOTONIC_MEDIA_OTS+XPDR2-NETWORK2")
                        .getBytes(Charset.forName("UTF-8")))
                        .toString());
                checkNepOtsiNode(nep2, enepUuid, nodeId + "+PHOTONIC_MEDIA_OTS+XPDR2-NETWORK2",
                    "eNodeEdgePoint", false);
                OwnedNodeEdgePoint photNep = nepsP.get(1);
                Uuid pnepUuid = new Uuid(
                    UUID.nameUUIDFromBytes((nodeId + "+PHOTONIC_MEDIA+XPDR2-NETWORK2")
                        .getBytes(Charset.forName("UTF-8"))).toString());
                checkNepOtsiNode(photNep, pnepUuid, nodeId + "+PHOTONIC_MEDIA+XPDR2-NETWORK2", "PhotMedNodeEdgePoint",
                    false);
                List<NodeRuleGroup> nrgList = node.nonnullNodeRuleGroup().values().stream()
                    .sorted((nrg1, nrg2) -> nrg1.getUuid().getValue().compareTo(nrg2.getUuid().getValue()))
                    .collect(Collectors.toList());
                checkNodeRuleGroupForSwitchOTSi(nrgList, enepUuid, inepUuid, nodeUuid);
                break;
            case "mux":
                assertEquals(1, nepsE.size(), "Mux-OTSi node should have 1 eNEP");
                assertEquals(1, nepsI.size(), "Mux-OTSi node should have 1 iNEPs");
                assertEquals(1, nepsP.size(), "Mux-OTSi node should have 1 photNEPs");
                OwnedNodeEdgePoint nep3 = nepsE.get(0);
                Uuid enepUuid2 = new Uuid(
                    UUID.nameUUIDFromBytes((nodeId + "+PHOTONIC_MEDIA_OTS+XPDR1-NETWORK1")
                        .getBytes(Charset.forName("UTF-8")))
                        .toString());
                checkNepOtsiNode(nep3, enepUuid2, nodeId + "+PHOTONIC_MEDIA_OTS+XPDR1-NETWORK1",
                    "eNodeEdgePoint", false);
                OwnedNodeEdgePoint nep4 = nepsI.get(0);
                Uuid inepUuid2 = new Uuid(
                    UUID.nameUUIDFromBytes((nodeId + "+iOTSi+XPDR1-NETWORK1").getBytes(Charset.forName("UTF-8")))
                        .toString());
                checkNepOtsiNode(nep4, inepUuid2, nodeId + "+iOTSi+XPDR1-NETWORK1", "iNodeEdgePoint", true);
                OwnedNodeEdgePoint photNep1 = nepsP.get(0);
                Uuid pnep1Uuid = new Uuid(
                    UUID.nameUUIDFromBytes((nodeId + "+PHOTONIC_MEDIA+XPDR1-NETWORK1")
                        .getBytes(Charset.forName("UTF-8"))).toString());
                checkNepOtsiNode(photNep1, pnep1Uuid, nodeId + "+PHOTONIC_MEDIA+XPDR1-NETWORK1", "PhotMedNodeEdgePoint",
                    false);
                List<NodeRuleGroup> nrgList2 = node.nonnullNodeRuleGroup().values().stream()
                    .sorted((nrg1, nrg2) -> nrg1.getUuid().getValue().compareTo(nrg2.getUuid().getValue()))
                    .collect(Collectors.toList());
                checkNodeRuleGroupForMuxOTSi(nrgList2, enepUuid2, inepUuid2, nodeUuid);
                break;
            case "tpdr":
                assertEquals(2, nepsE.size(), "Tpdr-OTSi node should have 2 eNEPs");
                assertEquals(2, nepsI.size(), "Tpdr-OTSi node should have 2 iNEPs");
                assertEquals(2, nepsP.size(), "Tpdr-OTSi node should have 2 photNEPs");
                OwnedNodeEdgePoint nep5 = nepsE.get(0);
                Uuid enepUuid3 = new Uuid(
                    UUID.nameUUIDFromBytes((nodeId + "+PHOTONIC_MEDIA_OTS+XPDR1-NETWORK1")
                        .getBytes(Charset.forName("UTF-8")))
                        .toString());
                checkNepOtsiNode(nep5, enepUuid3, nodeId + "+PHOTONIC_MEDIA_OTS+XPDR1-NETWORK1",
                    "eNodeEdgePoint", false);
                OwnedNodeEdgePoint nep6 = nepsI.get(0);
                Uuid inepUuid3 = new Uuid(
                    UUID.nameUUIDFromBytes((nodeId + "+iOTSi+XPDR1-NETWORK1").getBytes(Charset.forName("UTF-8")))
                        .toString());
                checkNepOtsiNode(nep6, inepUuid3, nodeId + "+iOTSi+XPDR1-NETWORK1", "iNodeEdgePoint", true);
                OwnedNodeEdgePoint photNep2 = nepsP.get(0);
                Uuid pnep2Uuid = new Uuid(
                    UUID.nameUUIDFromBytes((nodeId + "+PHOTONIC_MEDIA+XPDR1-NETWORK1")
                        .getBytes(Charset.forName("UTF-8"))).toString());
                checkNepOtsiNode(photNep2, pnep2Uuid, nodeId + "+PHOTONIC_MEDIA+XPDR1-NETWORK1", "PhotMedNodeEdgePoint",
                    false);
                List<NodeRuleGroup> nrgList3 = node.nonnullNodeRuleGroup().values().stream()
                    .sorted((nrg1, nrg2) -> nrg1.getUuid().getValue().compareTo(nrg2.getUuid().getValue()))
                    .collect(Collectors.toList());
                checkNodeRuleGroupForTpdrOTSi(nrgList3, enepUuid3, inepUuid3, nodeUuid);
                break;
            case "roadm":
// Keep trace of MC NEP test to be restored after the new policy for creating NEP is applied
//                assertEquals(0, nepsMc.size(), "MC NEP no more configured, Roadm node should have 0 MC NEPs");
//                assertEquals(0, nepsOtsimc.size(), "Roadm node should have 10 OTSiMC NEPs");
                assertEquals(12, nepsPhot.size(), "Roadm node should have 12 PHOT_MEDIA NEPs (2x4 OTS +2x(OTS+OMS)");
                // For Degree node
                OwnedNodeEdgePoint nep7 = nepsOMS.get(getRank("DEG1-TTP", nepsOMS));
                Uuid mcnepUuid3 = new Uuid(
                    UUID.nameUUIDFromBytes((nodeId + "+PHOTONIC_MEDIA_OMS+DEG1-TTP-TXRX").getBytes(Charset
                        .forName("UTF-8"))).toString());
                checkNepOtsiRdmNode(nep7, mcnepUuid3, nodeId + "+PHOTONIC_MEDIA_OMS+DEG1-TTP-TXRX",
                    "PHOTONIC_MEDIA_OMSNodeEdgePoint", false);
                OwnedNodeEdgePoint nep8 = nepsOTS.get(getRank("DEG1-TTP", nepsOTS));
                Uuid otmcnepUuid3 = new Uuid(UUID.nameUUIDFromBytes((nodeId + "+PHOTONIC_MEDIA_OTS+DEG1-TTP-TXRX")
                    .getBytes(Charset.forName("UTF-8"))).toString());
                checkNepOtsiRdmNode(nep8, otmcnepUuid3, nodeId + "+PHOTONIC_MEDIA_OTS+DEG1-TTP-TXRX",
                    "PHOTONIC_MEDIA_OTSNodeEdgePoint", false);
                OwnedNodeEdgePoint omsNep3 = nepsOMS.get(getRank("DEG1-TTP", nepsOMS));
                Uuid omsNep3Uuid = new Uuid(
                    UUID.nameUUIDFromBytes((nodeId + "+PHOTONIC_MEDIA_OMS+DEG1-TTP-TXRX")
                        .getBytes(Charset.forName("UTF-8"))).toString());
                checkNepOtsiRdmNode(omsNep3, omsNep3Uuid, nodeId + "+PHOTONIC_MEDIA_OMS+DEG1-TTP-TXRX",
                    "PHOTONIC_MEDIA_OMSNodeEdgePoint", false);
                // For srg node
                OwnedNodeEdgePoint nep10 = nepsOTS.get(getRank("SRG1-PP1", nepsOTS));
                Uuid otsnepUuid4 = new Uuid(UUID.nameUUIDFromBytes((nodeId + "+PHOTONIC_MEDIA_OTS+SRG1-PP1-TXRX")
                    .getBytes(Charset.forName("UTF-8"))).toString());
                checkNepOtsiRdmNode(nep10, otsnepUuid4, nodeId + "+PHOTONIC_MEDIA_OTS+SRG1-PP1-TXRX",
                    "PHOTONIC_MEDIA_OTSNodeEdgePoint", false);
                OwnedNodeEdgePoint otsNep4 = nepsOTS.get(getRank("SRG1-PP3", nepsOTS));
                Uuid otsNep4Uuid = new Uuid(UUID.nameUUIDFromBytes((nodeId + "+PHOTONIC_MEDIA_OTS+SRG1-PP3-TXRX")
                    .getBytes(Charset.forName("UTF-8"))).toString());
                checkNepOtsiRdmNode(otsNep4, otsNep4Uuid, nodeId + "+PHOTONIC_MEDIA_OTS+SRG1-PP3-TXRX",
                    "PHOTONIC_MEDIA_OTSNodeEdgePoint", false);
                List<NodeRuleGroup> nrgList4 = node.nonnullNodeRuleGroup().values().stream()
                    .sorted((nrg1, nrg2) -> nrg1.getName().entrySet().iterator().next().getValue().toString()
                        .compareTo(nrg2.getName().entrySet().iterator().next().getValue().toString()))
                    .collect(Collectors.toList());
                LOG.info("NODERULEGROUP List nrgLIst4 is as follows {}", nrgList4);
                List<Integer> nepNumber = new ArrayList<>(List.of(2, 4, 4));
                checkNodeRuleGroupForRdm(nrgList4, nepNumber);
                break;
            default:
                fail();
                break;
        }
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
        Integer indNrg = nrgContainsClientAndNetwork(nrgList, clientNepUuid, networkNepUuid);
        assertNotNull("One node-rule-group shall contains client and network Neps", indNrg);
        List<NodeEdgePoint> nodeEdgePointList;
        if (nodeType.equals("Switch")) {
            assertEquals(8, nrgList.get(indNrg).getNodeEdgePoint().size(), "Switch-DSR nrg should contain 8 NEP");
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
                "in the sorted node-rule-group, nep number 4 should be XPDR2-CLIENT4");
                //TODO nep number 6 rather ?
            //TODO regroup with else condition ?
        } else {
            nodeEdgePointList = new ArrayList<>(nrgList.get(indNrg).nonnullNodeEdgePoint().values());
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

    private void checkNepOtsiRdmNode(
            OwnedNodeEdgePoint nep, Uuid nepUuid, String portName, String nepName, boolean withSip) {
        if (!nep.getUuid().equals(nepUuid)) {
            LOG.info("ERRORUUIDNEP on Nep {}, expected {}", nep.getName(), portName);
        }
        rawCheckNep(nepName.contains("OMS")
                ? List.of(PHOTONICLAYERQUALIFIEROMS.VALUE)
                : nepName.contains("OTS") ? List.of(PHOTONICLAYERQUALIFIEROTS.VALUE) : null,
            LayerProtocolName.PHOTONICMEDIA, false, nep, nepUuid, portName, nepName, withSip);
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

    private void checkOmsLink(
            org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.topology.Link link,
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

    private int getRank(String searchedChar, List<OwnedNodeEdgePoint> onepList) {
        return rawRank(
            searchedChar, onepList.stream().map(entry -> entry.getName().values()).collect(Collectors.toList()));
    }

    private int getNodeRank(String searchedChar,
        List<org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.topology.Node> nodeList) {
        return rawRank(
            searchedChar, nodeList.stream().map(entry -> entry.getName().values()).collect(Collectors.toList()));
    }

    private int rawRank(String searchedChar, List<Collection<Name>> nameCL) {
        int foundAtRank = 0;
        int rank = 0;
        for (var nameC: nameCL) {
            for (Name name: nameC) {
                if (name.getValue().contains(searchedChar)) {
                    foundAtRank = rank;
                    //TODO should we really pursue once it is found ?
                }
            }
            rank++;
        }
        LOG.info("searched Char {} found at rank {}", searchedChar, foundAtRank);
        return foundAtRank;
    }

    private Integer nrgContainsClientAndNetwork(List<NodeRuleGroup> nrgList, Uuid clientNepUuid, Uuid networkNepUuid) {
        // 1 NRG should at least contain the NEP of interest in the NEP List
        Integer indexNrg = 0;
        for (NodeRuleGroup nrg : nrgList) {
            Boolean foundClient = false;
            Boolean foundNetwork = false;
            for (NodeEdgePoint nep : nrg.nonnullNodeEdgePoint().values()) {
                foundClient = foundClient || nep.getNodeEdgePointUuid().equals(clientNepUuid);
                foundNetwork = foundNetwork || nep.getNodeEdgePointUuid().equals(networkNepUuid);
            }
            if (foundClient && foundNetwork) {
                return indexNrg;
            }
            indexNrg++;
        }
        return null;
    }

}
