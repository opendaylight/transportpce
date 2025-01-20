/*
 * Copyright © 2020 Orange, Inc. and others.  All rights reserved.
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
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import org.eclipse.jdt.annotation.Nullable;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.transportpce.common.InstanceIdentifiers;
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
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev230526.Link1Builder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev230526.TerminationPoint1;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev230526.TerminationPoint1Builder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.state.types.rev191129.State;
import org.opendaylight.yang.gen.v1.http.org.openroadm.equipment.states.types.rev191129.AdminStates;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.types.rev230526.OpenroadmTpType;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.NetworkId;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.Networks;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.NodeId;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.networks.NetworkKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.networks.network.Node;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.networks.network.NodeBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.networks.network.NodeKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.LinkId;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.Network1;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.Node1;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.TpId;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.networks.network.LinkKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.networks.network.node.TerminationPoint;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.networks.network.node.TerminationPointBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.networks.network.node.TerminationPointKey;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.AdministrativeState;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.CAPACITYUNITGBPS;
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
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.digital.otn.rev221121.ODUTYPEODU2;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.digital.otn.rev221121.ODUTYPEODU2E;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.digital.otn.rev221121.ODUTYPEODU4;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.dsr.rev221121.DIGITALSIGNALTYPE100GigE;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.dsr.rev221121.DIGITALSIGNALTYPE10GigELAN;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.photonic.media.rev221121.PHOTONICLAYERQUALIFIEROMS;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.photonic.media.rev221121.PHOTONICLAYERQUALIFIEROTS;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.photonic.media.rev221121.PHOTONICLAYERQUALIFIEROTSi;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.FORWARDINGRULEMAYFORWARDACROSSGROUP;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.RuleType;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.node.NodeRuleGroup;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.node.OwnedNodeEdgePoint;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.node.OwnedNodeEdgePointKey;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.node.edge.point.MappedServiceInterfacePointKey;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.node.rule.group.NodeEdgePoint;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.node.rule.group.Rule;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.topology.Link;
import org.opendaylight.yangtools.binding.DataObjectIdentifier;
import org.opendaylight.yangtools.yang.common.Decimal64;
import org.opendaylight.yangtools.yang.common.Uint64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class ConvertORTopoToTapiTopoTest extends AbstractTest {
    private static final Logger LOG = LoggerFactory.getLogger(ConvertORTopoToTapiTopoTest.class);

    private static Node otnMuxA;
    private static Node otnMuxC;
    private static Node otnSwitch;
    private static Node tpdr100G;
    private static Map<LinkKey,org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226
        .networks.network.Link> otnLinks;
    private static Uuid topologyUuid;
    private static NetworkTransactionService networkTransactionService;
    private static TapiLink tapiLink;
    private static DataBroker dataBroker = getDataBroker();

    @BeforeAll
    static void setUp() throws InterruptedException, ExecutionException {
        TopologyDataUtils.writeTopologyFromFileToDatastore(
            getDataStoreContextUtil(),
            TapiTopologyDataUtils.OPENROADM_TOPOLOGY_FILE,
            InstanceIdentifiers.OVERLAY_NETWORK_II);
        TopologyDataUtils.writeTopologyFromFileToDatastore(
            getDataStoreContextUtil(),
            TapiTopologyDataUtils.OTN_TOPOLOGY_FILE,
            InstanceIdentifiers.OTN_NETWORK_II);
        TopologyDataUtils.writePortmappingFromFileToDatastore(
            getDataStoreContextUtil(),
            TapiTopologyDataUtils.PORTMAPPING_FILE);
        otnMuxA = dataBroker.newReadOnlyTransaction()
            .read(
                LogicalDatastoreType.CONFIGURATION,
                //muxAIID
                DataObjectIdentifier.builder(Networks.class)
                    .child(org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226
                            .networks.Network.class,
                        new NetworkKey(new NetworkId("otn-topology")))
                    .child(Node.class, new NodeKey(new NodeId("SPDR-SA1-XPDR1")))
                    .build())
            .get().orElseThrow();
        otnMuxC = dataBroker.newReadOnlyTransaction()
            .read(
                LogicalDatastoreType.CONFIGURATION,
                //muxCIID
                DataObjectIdentifier.builder(Networks.class)
                    .child(org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226
                            .networks.Network.class,
                        new NetworkKey(new NetworkId("otn-topology")))
                    .child(Node.class, new NodeKey(new NodeId("SPDR-SC1-XPDR1")))
                    .build())
            .get().orElseThrow();
        otnSwitch = dataBroker.newReadOnlyTransaction()
            .read(
                LogicalDatastoreType.CONFIGURATION,
                //switchIID
                DataObjectIdentifier.builder(Networks.class)
                    .child(org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226
                            .networks.Network.class,
                        new NetworkKey(new NetworkId("otn-topology")))
                    .child(Node.class, new NodeKey(new NodeId("SPDR-SA1-XPDR2")))
                    .build())
            .get().orElseThrow();
        tpdr100G = dataBroker.newReadOnlyTransaction()
            .read(
                LogicalDatastoreType.CONFIGURATION,
                //tpdrIID
                DataObjectIdentifier.builder(Networks.class)
                    .child(org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226
                            .networks.Network.class,
                        new NetworkKey(new NetworkId("otn-topology")))
                    .child(Node.class, new NodeKey(new NodeId("XPDR-A1-XPDR1")))
                    .build())
            .get().orElseThrow();
        otnLinks = dataBroker.newReadOnlyTransaction()
            .read(
                LogicalDatastoreType.CONFIGURATION,
                //linksIID
                DataObjectIdentifier.builder(Networks.class)
                    .child(org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226
                            .networks.Network.class,
                        new NetworkKey(new NetworkId("otn-topology")))
                    .augmentation(Network1.class)
                    .build())
            .get().orElseThrow().getLink();
        topologyUuid = new Uuid(
            UUID.nameUUIDFromBytes(TapiStringConstants.T0_MULTILAYER.getBytes(Charset.forName("UTF-8"))).toString());
        networkTransactionService = new NetworkTransactionImpl(getDataBroker());
        tapiLink = new TapiLinkImpl(networkTransactionService, new TapiContext(networkTransactionService));
        LOG.info("TEST SETUP READY");
    }

    @Test
    void convertNodeWhenNoStates() {
        Node tpdr = changeTerminationPointState(tpdr100G, "XPDR1-NETWORK1", null, null);
        ConvertORToTapiTopology tapiFactory = new ConvertORToTapiTopology();
        tapiFactory.convertNode(tpdr,
            tpdr100G.augmentation(Node1.class).getTerminationPoint().values().stream()
                .filter(tp -> tp.augmentation(TerminationPoint1.class).getTpType()
                                .equals(OpenroadmTpType.XPONDERNETWORK))
                .map(tp -> tp.getTpId().getValue())
                .collect(Collectors.toList()));
        OwnedNodeEdgePoint nepN = tapiFactory.getTapiNodes()
            .get(new org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121
                    .topology.NodeKey(new Uuid(
                UUID.nameUUIDFromBytes("XPDR-A1-XPDR1+XPONDER".getBytes(Charset.forName("UTF-8"))).toString())))
            .nonnullOwnedNodeEdgePoint()
            .get(new OwnedNodeEdgePointKey(new Uuid(
                UUID.nameUUIDFromBytes(("XPDR-A1-XPDR1+iODU+XPDR1-NETWORK1").getBytes(Charset.forName("UTF-8")))
                    .toString())));
        assertNull(nepN.getAdministrativeState(), "Administrative State should not be present");
        assertNull(nepN.getOperationalState(), "Operational State should not be present");

    }

    @Test
    void convertNodeWhenBadStates1() {
        Node tpdr =
            changeTerminationPointState(tpdr100G, "XPDR1-NETWORK1", AdminStates.OutOfService, State.OutOfService);
        ConvertORToTapiTopology tapiFactory = new ConvertORToTapiTopology();
        tapiFactory.convertNode(tpdr,
            tpdr100G.augmentation(Node1.class).getTerminationPoint().values().stream()
                .filter(tp -> tp.augmentation(TerminationPoint1.class).getTpType()
                                .equals(OpenroadmTpType.XPONDERNETWORK))
                .map(tp -> tp.getTpId().getValue())
                .collect(Collectors.toList()));
        OwnedNodeEdgePoint nepN = tapiFactory.getTapiNodes()
            .get(new org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121
                    .topology.NodeKey(new Uuid(
                UUID.nameUUIDFromBytes("XPDR-A1-XPDR1+XPONDER".getBytes(Charset.forName("UTF-8"))).toString())))
            .nonnullOwnedNodeEdgePoint()
            .get(new OwnedNodeEdgePointKey(new Uuid(
                UUID.nameUUIDFromBytes(("XPDR-A1-XPDR1+iODU+XPDR1-NETWORK1").getBytes(Charset.forName("UTF-8")))
                    .toString())));
        assertEquals(AdministrativeState.LOCKED, nepN.getAdministrativeState(),
            "Administrative State should be Locked");
        assertEquals(OperationalState.DISABLED, nepN.getOperationalState(), "Operational State should be Disabled");
    }

    @Test
    void convertNodeWhenBadStates2() {
        Node tpdr = changeTerminationPointState(tpdr100G, "XPDR1-NETWORK1", AdminStates.Maintenance, State.Degraded);
        ConvertORToTapiTopology tapiFactory = new ConvertORToTapiTopology();
        tapiFactory.convertNode(tpdr,
            tpdr100G.augmentation(Node1.class).getTerminationPoint().values().stream()
                .filter(tp -> tp.augmentation(TerminationPoint1.class).getTpType()
                                .equals(OpenroadmTpType.XPONDERNETWORK))
                .map(tp -> tp.getTpId().getValue())
                .collect(Collectors.toList()));
        OwnedNodeEdgePoint nepN = tapiFactory.getTapiNodes()
            .get(new org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121
                    .topology.NodeKey(new Uuid(
                UUID.nameUUIDFromBytes("XPDR-A1-XPDR1+XPONDER".getBytes(Charset.forName("UTF-8"))).toString())))
            .nonnullOwnedNodeEdgePoint()
            .get(new OwnedNodeEdgePointKey(new Uuid(
                UUID.nameUUIDFromBytes(("XPDR-A1-XPDR1+iODU+XPDR1-NETWORK1").getBytes(Charset.forName("UTF-8")))
                    .toString())));
        assertEquals(AdministrativeState.LOCKED, nepN.getAdministrativeState(),
            "Administrative State should be Locked");
        assertEquals(OperationalState.DISABLED, nepN.getOperationalState(), "Operational State should be Disabled");
    }

    @Test
    void convertOtnLinkWhenNoState() {
        HashMap<LinkKey,
            org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226
                .networks.network.Link> otnLinksAlt = new HashMap<>(otnLinks);
        org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226
                .networks.network.Link link =
            changeOtnLinkState(otnLinks.get(new LinkKey(
                new LinkId("ODTU4-SPDR-SA1-XPDR1-XPDR1-NETWORK1toSPDR-SC1-XPDR1-XPDR1-NETWORK1"))), null, null);
        otnLinksAlt.replace(link.key(), link);
        ConvertORToTapiTopology tapiFactory = new ConvertORToTapiTopology();
        ConvertORTopoToTapiTopo tapiAbsFactory = new ConvertORTopoToTapiTopo(topologyUuid, tapiLink);
        tapiFactory.convertNode(otnMuxA,
            otnMuxA.augmentation(Node1.class).getTerminationPoint().values().stream()
                .filter(tp -> tp.augmentation(TerminationPoint1.class).getTpType()
                                .equals(OpenroadmTpType.XPONDERNETWORK))
                .map(tp -> tp.getTpId().getValue())
                .collect(Collectors.toList()));
        tapiAbsFactory.setTapiNodes(tapiFactory.getTapiNodes());
        tapiFactory.convertNode(otnMuxC,
            otnMuxC.augmentation(Node1.class).getTerminationPoint().values().stream()
                .filter(tp -> tp.augmentation(TerminationPoint1.class).getTpType()
                                .equals(OpenroadmTpType.XPONDERNETWORK))
                .map(tp -> tp.getTpId().getValue())
                .collect(Collectors.toList()));
        tapiAbsFactory.setTapiNodes(tapiFactory.getTapiNodes());
        tapiAbsFactory.convertLinks(otnLinksAlt);

        List<Link> tapiLinks = tapiAbsFactory.getTapiLinks().values().stream()
            .sorted((l1, l2) -> l1.getUuid().getValue().compareTo(l2.getUuid().getValue()))
            .collect(Collectors.toList());
        assertNull(tapiLinks.get(1).getAdministrativeState(), "Administrative State should not be present");
        assertEquals(AdministrativeState.UNLOCKED, tapiLinks.get(0).getAdministrativeState(),
            "Administrative state should be UNLOCKED");
        assertNull(tapiLinks.get(1).getOperationalState(), "Operational State should not be present");
        assertEquals(OperationalState.ENABLED, tapiLinks.get(0).getOperationalState(),
            "Operational state should be ENABLED");
    }

    @Test
    void convertOtnLinkWhenNoStateOnOppositeLink() {
        HashMap<LinkKey,
            org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226
                .networks.network.Link> otnLinksAlt = new HashMap<>(otnLinks);
        org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226
                .networks.network.Link link =
            changeOtnLinkState(otnLinks.get(new LinkKey(
                new LinkId("ODTU4-SPDR-SC1-XPDR1-XPDR1-NETWORK1toSPDR-SA1-XPDR1-XPDR1-NETWORK1"))), null, null);
        otnLinksAlt.replace(link.key(), link);
        ConvertORToTapiTopology tapiFactory = new ConvertORToTapiTopology();
        ConvertORTopoToTapiTopo tapiAbsFactory = new ConvertORTopoToTapiTopo(topologyUuid, tapiLink);
        tapiFactory.convertNode(otnMuxA,
            otnMuxA.augmentation(Node1.class).getTerminationPoint().values().stream()
                .filter(tp -> tp.augmentation(TerminationPoint1.class).getTpType()
                                .equals(OpenroadmTpType.XPONDERNETWORK))
                .map(tp -> tp.getTpId().getValue())
                .collect(Collectors.toList()));
        tapiAbsFactory.setTapiNodes(tapiFactory.getTapiNodes());
        tapiFactory.convertNode(otnMuxC,
            otnMuxC.augmentation(Node1.class).getTerminationPoint().values().stream()
                .filter(tp -> tp.augmentation(TerminationPoint1.class).getTpType()
                                .equals(OpenroadmTpType.XPONDERNETWORK))
                .map(tp -> tp.getTpId().getValue())
                .collect(Collectors.toList()));
        tapiAbsFactory.setTapiNodes(tapiFactory.getTapiNodes());
        tapiAbsFactory.convertLinks(otnLinksAlt);

        List<Link> tapiLinks = tapiAbsFactory.getTapiLinks().values().stream()
            .sorted((l1, l2) -> l1.getUuid().getValue().compareTo(l2.getUuid().getValue()))
            .collect(Collectors.toList());
        assertNull(tapiLinks.get(1).getAdministrativeState(), "Administrative State should not be present");
        assertEquals(AdministrativeState.UNLOCKED, tapiLinks.get(0).getAdministrativeState(),
            "Administrative state should be UNLOCKED");
        assertNull(tapiLinks.get(1).getOperationalState(), "Operational State should not be present");
        assertEquals(OperationalState.ENABLED, tapiLinks.get(0).getOperationalState(),
            "Operational state should be ENABLED");
    }

    @Test
    void convertOtnLinkWhenBadState1() {
        HashMap<LinkKey,
            org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226
                .networks.network.Link> otnLinksAlt = new HashMap<>(otnLinks);
        org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226
                .networks.network.Link link =
            changeOtnLinkState(otnLinks.get(new LinkKey(
                new LinkId("ODTU4-SPDR-SA1-XPDR1-XPDR1-NETWORK1toSPDR-SC1-XPDR1-XPDR1-NETWORK1"))),
                AdminStates.OutOfService, State.OutOfService);
        otnLinksAlt.replace(link.key(), link);
        ConvertORToTapiTopology tapiFactory = new ConvertORToTapiTopology();
        ConvertORTopoToTapiTopo tapiAbsFactory = new ConvertORTopoToTapiTopo(topologyUuid, tapiLink);
        tapiFactory.convertNode(otnMuxA,
            otnMuxA.augmentation(Node1.class).getTerminationPoint().values().stream()
                .filter(tp -> tp.augmentation(TerminationPoint1.class).getTpType()
                                .equals(OpenroadmTpType.XPONDERNETWORK))
                .map(tp -> tp.getTpId().getValue())
                .collect(Collectors.toList()));
        tapiAbsFactory.setTapiNodes(tapiFactory.getTapiNodes());
        tapiFactory.convertNode(otnMuxC,
            otnMuxC.augmentation(Node1.class).getTerminationPoint().values().stream()
                .filter(tp -> tp.augmentation(TerminationPoint1.class).getTpType()
                                .equals(OpenroadmTpType.XPONDERNETWORK))
                .map(tp -> tp.getTpId().getValue())
                .collect(Collectors.toList()));
        tapiAbsFactory.setTapiNodes(tapiFactory.getTapiNodes());
        tapiAbsFactory.convertLinks(otnLinksAlt);
        List<Link> tapiLinks = tapiAbsFactory.getTapiLinks().values().stream()
            .sorted((l1, l2) -> l1.getUuid().getValue().compareTo(l2.getUuid().getValue()))
            .collect(Collectors.toList());
        LOG.info("TapiLinks are as follow : {}", tapiLinks);
        assertEquals(AdministrativeState.LOCKED, tapiLinks.get(1).getAdministrativeState(),
            "Administrative state should be LOCKED");
        assertEquals(AdministrativeState.UNLOCKED, tapiLinks.get(0).getAdministrativeState(),
            "Administrative state should be UNLOCKED");
        assertEquals(OperationalState.DISABLED, tapiLinks.get(1).getOperationalState(),
            "Operational state should be DISABLED");
        assertEquals(OperationalState.ENABLED, tapiLinks.get(0).getOperationalState(),
            "Operational state should be ENABLED");
    }

    @Test
    void convertOtnLinkWhenBadState2() {
        HashMap<LinkKey,
            org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226
                .networks.network.Link> otnLinksAlt = new HashMap<>(otnLinks);
        org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226
                .networks.network.Link link =
            changeOtnLinkState(otnLinks.get(new LinkKey(
                new LinkId("ODTU4-SPDR-SA1-XPDR1-XPDR1-NETWORK1toSPDR-SC1-XPDR1-XPDR1-NETWORK1"))),
                AdminStates.Maintenance, State.Degraded);
        otnLinksAlt.replace(link.key(), link);
        ConvertORToTapiTopology tapiFactory = new ConvertORToTapiTopology();
        ConvertORTopoToTapiTopo tapiAbsFactory = new ConvertORTopoToTapiTopo(topologyUuid, tapiLink);
        tapiFactory.convertNode(otnMuxA,
            otnMuxA.augmentation(Node1.class).getTerminationPoint().values().stream()
                .filter(tp -> tp.augmentation(TerminationPoint1.class).getTpType()
                                .equals(OpenroadmTpType.XPONDERNETWORK))
                .map(tp -> tp.getTpId().getValue())
                .collect(Collectors.toList()));
        tapiAbsFactory.setTapiNodes(tapiFactory.getTapiNodes());
        tapiFactory.convertNode(otnMuxC,
            otnMuxC.augmentation(Node1.class).getTerminationPoint().values().stream()
                .filter(tp -> tp.augmentation(TerminationPoint1.class).getTpType()
                                .equals(OpenroadmTpType.XPONDERNETWORK))
                .map(tp -> tp.getTpId().getValue())
                .collect(Collectors.toList()));
        tapiAbsFactory.setTapiNodes(tapiFactory.getTapiNodes());
        tapiAbsFactory.convertLinks(otnLinksAlt);
        List<Link> tapiLinks = tapiAbsFactory.getTapiLinks().values().stream()
            .sorted((l1, l2) -> l1.getUuid().getValue().compareTo(l2.getUuid().getValue()))
            .collect(Collectors.toList());
        assertEquals(AdministrativeState.LOCKED, tapiLinks.get(1).getAdministrativeState(),
            "Administrative state should be LOCKED");
        assertEquals(AdministrativeState.UNLOCKED, tapiLinks.get(0).getAdministrativeState(),
            "Administrative state should be UNLOCKED");
        assertEquals(OperationalState.DISABLED, tapiLinks.get(1).getOperationalState(),
            "Operational state should be DISABLED");
        assertEquals(OperationalState.ENABLED, tapiLinks.get(0).getOperationalState(),
            "Operational state should be ENABLED");
    }

    @Test
    void convertOtnLinkWhenBadStateOnOppositeLink() {
        HashMap<LinkKey,
            org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226
                .networks.network.Link> otnLinksAlt = new HashMap<>(otnLinks);
        org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226
                .networks.network.Link link =
            changeOtnLinkState(otnLinks.get(new LinkKey(
                new LinkId("ODTU4-SPDR-SC1-XPDR1-XPDR1-NETWORK1toSPDR-SA1-XPDR1-XPDR1-NETWORK1"))),
                AdminStates.OutOfService, State.OutOfService);
        otnLinksAlt.replace(link.key(), link);
        ConvertORToTapiTopology tapiFactory = new ConvertORToTapiTopology();
        ConvertORTopoToTapiTopo tapiAbsFactory = new ConvertORTopoToTapiTopo(topologyUuid, tapiLink);
        tapiFactory.convertNode(otnMuxA,
            otnMuxA.augmentation(Node1.class).getTerminationPoint().values().stream()
                .filter(tp -> tp.augmentation(TerminationPoint1.class).getTpType()
                                .equals(OpenroadmTpType.XPONDERNETWORK))
                .map(tp -> tp.getTpId().getValue())
                .collect(Collectors.toList()));
        tapiAbsFactory.setTapiNodes(tapiFactory.getTapiNodes());
        tapiFactory.convertNode(otnMuxC,
            otnMuxC.augmentation(Node1.class).getTerminationPoint().values().stream()
                .filter(tp -> tp.augmentation(TerminationPoint1.class).getTpType()
                                .equals(OpenroadmTpType.XPONDERNETWORK))
                .map(tp -> tp.getTpId().getValue())
                .collect(Collectors.toList()));
        tapiAbsFactory.setTapiNodes(tapiFactory.getTapiNodes());
        tapiAbsFactory.convertLinks(otnLinksAlt);

        List<Link> tapiLinks = tapiAbsFactory.getTapiLinks().values().stream()
            .sorted((l1, l2) -> l1.getUuid().getValue().compareTo(l2.getUuid().getValue()))
            .collect(Collectors.toList());
        assertEquals(AdministrativeState.LOCKED, tapiLinks.get(1).getAdministrativeState(),
            "Administrative state should be LOCKED");
        assertEquals(AdministrativeState.UNLOCKED, tapiLinks.get(0).getAdministrativeState(),
            "Administrative state should be UNLOCKED");
        assertEquals(OperationalState.DISABLED, tapiLinks.get(1).getOperationalState(),
            "Operational state should be DISABLED");
        assertEquals(OperationalState.ENABLED, tapiLinks.get(0).getOperationalState(),
            "Operational state should be ENABLED");
    }

    @Test
    void convertNodeForTransponder100G() {
        ConvertORToTapiTopology tapiFactory = new ConvertORToTapiTopology();
        tapiFactory.convertNode(tpdr100G,
            tpdr100G.augmentation(Node1.class).getTerminationPoint().values().stream()
                .filter(tp -> tp.augmentation(TerminationPoint1.class).getTpType()
                                .equals(OpenroadmTpType.XPONDERNETWORK))
                .map(tp -> tp.getTpId().getValue())
                .collect(Collectors.toList()));
        List<org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.topology.Node> tapiNodes =
            tapiFactory.getTapiNodes().values().stream()
                .sorted((n1, n2) -> n1.getUuid().getValue().compareTo(n2.getUuid().getValue()))
                .collect(Collectors.toList());

        assertEquals(1, tapiFactory.getTapiNodes().size(), "Node list size should be 1 (DSR-ODU merged)");
        assertEquals(0, tapiFactory.getTapiLinks().size(), "Link list size should be 0 (no more transitional links)");

        checkDsrNode(getNode("SPDR-SA1", tapiNodes),
            new Uuid(UUID.nameUUIDFromBytes("XPDR-A1-XPDR1+XPONDER".getBytes(Charset.forName("UTF-8"))).toString()),
            "tpdr", "XPDR-A1-XPDR1+XPONDER");
    }

    @Test
    void convertNodeForOtnMuxponder() {
        ConvertORToTapiTopology tapiFactory = new ConvertORToTapiTopology();
        tapiFactory.convertNode(otnMuxA,
            otnMuxA.augmentation(Node1.class).getTerminationPoint().values().stream()
                .filter(tp -> tp.augmentation(TerminationPoint1.class).getTpType()
                                .equals(OpenroadmTpType.XPONDERNETWORK))
                .map(tp -> tp.getTpId().getValue())
                .collect(Collectors.toList()));
        List<org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.topology.Node> tapiNodes =
            tapiFactory.getTapiNodes().values().stream()
                .sorted((n1, n2) -> n1.getUuid().getValue().compareTo(n2.getUuid().getValue()))
                .collect(Collectors.toList());

        assertEquals(1, tapiFactory.getTapiNodes().size(), "Node list size should be 1 (DSR & ODU merged");
        assertEquals(0, tapiFactory.getTapiLinks().size(), "Link list size should be 0, no more transitional links");
        checkDsrNode(getNode("SPDR-SA1", tapiNodes),
            new Uuid(UUID.nameUUIDFromBytes("SPDR-SA1-XPDR1+XPONDER".getBytes(Charset.forName("UTF-8"))).toString()),
            "mux", "SPDR-SA1-XPDR1+XPONDER");
    }

    @Test
    void convertNodeForOtnSwitch() {
        ConvertORToTapiTopology tapiFactory = new ConvertORToTapiTopology();
        tapiFactory.convertNode(otnSwitch,
            otnSwitch.augmentation(Node1.class).getTerminationPoint().values().stream()
                .filter(tp -> tp.augmentation(TerminationPoint1.class).getTpType()
                                .equals(OpenroadmTpType.XPONDERNETWORK))
                .map(tp -> tp.getTpId().getValue())
                .collect(Collectors.toList()));
        List<org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.topology.Node> tapiNodes =
            tapiFactory.getTapiNodes().values().stream()
                .sorted((n1, n2) -> n1.getUuid().getValue().compareTo(n2.getUuid().getValue()))
                .collect(Collectors.toList());
        assertEquals(1, tapiFactory.getTapiNodes().size(), "Node list size should be 1 (DSR/ODU merged)");
        assertEquals(0, tapiFactory.getTapiLinks().size(), "Link list size should be 0 : no more transitional link");
        checkDsrNode(getNode("SPDR-SA1", tapiNodes),
            new Uuid(UUID.nameUUIDFromBytes("SPDR-SA1-XPDR2+XPONDER".getBytes(Charset.forName("UTF-8"))).toString()),
            "switch", "SPDR-SA1-XPDR2+XPONDER");
    }

    @Test
    void convertOtnLink() {
        ConvertORToTapiTopology tapiFactory = new ConvertORToTapiTopology();
        ConvertORTopoToTapiTopo tapiAbsFactory = new ConvertORTopoToTapiTopo(topologyUuid, tapiLink);
        tapiFactory.convertNode(otnMuxA,
            otnMuxA.augmentation(Node1.class).getTerminationPoint().values().stream()
                .filter(tp -> tp.augmentation(TerminationPoint1.class).getTpType()
                                .equals(OpenroadmTpType.XPONDERNETWORK))
                .map(tp -> tp.getTpId().getValue())
                .collect(Collectors.toList()));
        tapiAbsFactory.setTapiNodes(tapiFactory.getTapiNodes());
        tapiFactory.convertNode(otnMuxC,
            otnMuxC.augmentation(Node1.class).getTerminationPoint().values().stream()
                .filter(tp -> tp.augmentation(TerminationPoint1.class).getTpType()
                                .equals(OpenroadmTpType.XPONDERNETWORK))
                .map(tp -> tp.getTpId().getValue())
                .collect(Collectors.toList()));
        tapiAbsFactory.setTapiNodes(tapiFactory.getTapiNodes());

        tapiAbsFactory.convertLinks(otnLinks);
        assertEquals(2, tapiAbsFactory.getTapiLinks().size(), "Link list size should be 2 : no transitional link");

        Uuid node1Uuid = new Uuid(
            UUID.nameUUIDFromBytes("SPDR-SA1-XPDR1+XPONDER".getBytes(Charset.forName("UTF-8"))).toString());
        Uuid node2Uuid = new Uuid(
            UUID.nameUUIDFromBytes("SPDR-SC1-XPDR1+XPONDER".getBytes(Charset.forName("UTF-8"))).toString());
        Uuid node3Uuid = new Uuid(
            UUID.nameUUIDFromBytes("SPDR-SA1-XPDR1+XPONDER".getBytes(Charset.forName("UTF-8"))).toString());
        Uuid node4Uuid = new Uuid(
            UUID.nameUUIDFromBytes("SPDR-SC1-XPDR1+XPONDER".getBytes(Charset.forName("UTF-8"))).toString());
        Uuid tp1Uuid = new Uuid(
            UUID.nameUUIDFromBytes("SPDR-SA1-XPDR1+eODU+XPDR1-NETWORK1".getBytes(Charset.forName("UTF-8"))).toString());
        Uuid tp2Uuid = new Uuid(
            UUID.nameUUIDFromBytes("SPDR-SC1-XPDR1+eODU+XPDR1-NETWORK1".getBytes(Charset.forName("UTF-8"))).toString());
        Uuid tp3Uuid = new Uuid(
            UUID.nameUUIDFromBytes("SPDR-SA1-XPDR1+iOTSi+XPDR1-NETWORK1".getBytes(Charset.forName("UTF-8")))
                .toString());
        Uuid tp4Uuid = new Uuid(
            UUID.nameUUIDFromBytes("SPDR-SC1-XPDR1+iOTSi+XPDR1-NETWORK1".getBytes(Charset.forName("UTF-8")))
                .toString());
        Uuid link1Uuid = new Uuid(
            UUID.nameUUIDFromBytes("SPDR-SA1-XPDR1+eODU+XPDR1-NETWORK1toSPDR-SC1-XPDR1+eODU+XPDR1-NETWORK1"
                    .getBytes(Charset.forName("UTF-8")))
                .toString());
        Uuid link2Uuid = new Uuid(
            UUID.nameUUIDFromBytes("SPDR-SA1-XPDR1+iOTSi+XPDR1-NETWORK1toSPDR-SC1-XPDR1+iOTSi+XPDR1-NETWORK1"
                    .getBytes(Charset.forName("UTF-8")))
                .toString());
        List<Link> links = tapiAbsFactory.getTapiLinks().values().stream()
            .sorted((l1, l2) -> l1.getUuid().getValue().compareTo(l2.getUuid().getValue()))
            .collect(Collectors.toList());
        LOG.info("Node3 {}, Node4 = {},", node3Uuid, node4Uuid);
        checkOtnLink(links.get(1), node1Uuid, node2Uuid, tp1Uuid, tp2Uuid, link1Uuid,
            "SPDR-SA1-XPDR1+eODU+XPDR1-NETWORK1toSPDR-SC1-XPDR1+eODU+XPDR1-NETWORK1");
        checkOtnLink(links.get(0), node3Uuid, node4Uuid, tp3Uuid, tp4Uuid, link2Uuid,
            "SPDR-SA1-XPDR1+iOTSi+XPDR1-NETWORK1toSPDR-SC1-XPDR1+iOTSi+XPDR1-NETWORK1");
        LOG.info("The link we check  has name {}", links.get(0).getName());
    }

    @Test
    void convertRoadmInfrastructureWhenNoXponderAttached() {
        ConvertORTopoToTapiTopo tapiAbsFactory = new ConvertORTopoToTapiTopo(topologyUuid, tapiLink);
        tapiAbsFactory.convertRoadmInfrastructure();

        assertEquals(1, tapiAbsFactory.getTapiNodes().size(), "Node list size should be 1");
        assertEquals(0, tapiAbsFactory.getTapiLinks().size(), "Link list size should be empty");
        List<org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.topology.Node> tapiNodes =
            tapiAbsFactory.getTapiNodes().values().stream().collect(Collectors.toList());
        Uuid photNodeUuid = new Uuid(
            UUID.nameUUIDFromBytes("ROADM-infra".getBytes(Charset.forName("UTF-8"))).toString());
        checkOtsiNode(tapiNodes.get(0), photNodeUuid, "infra", "ROADM-infra");
    }

    @Test
    void convertRoadmInfrastructureWhenOtnMuxAttached() {
        ConvertORTopoToTapiTopo tapiAbsFactory = new ConvertORTopoToTapiTopo(topologyUuid, tapiLink);
        ConvertORToTapiTopology tapiFactory = new ConvertORToTapiTopology();
        tapiFactory.convertNode(otnMuxA,
            otnMuxA.augmentation(Node1.class).getTerminationPoint().values().stream()
                .filter(tp -> tp.augmentation(TerminationPoint1.class).getTpType()
                                .equals(OpenroadmTpType.XPONDERNETWORK))
                .map(tp -> tp.getTpId().getValue())
                .collect(Collectors.toList()));
        tapiAbsFactory.setTapiNodes(tapiFactory.getTapiNodes());
        tapiAbsFactory.convertRoadmInfrastructure();
        LOG.info("ERRORLINK List of link = {}", tapiAbsFactory.getTapiLinks());
        assertEquals(2, tapiAbsFactory.getTapiNodes().size(), "Node list size should be 2");
        assertEquals(1, tapiAbsFactory.getTapiLinks().size(), "Link list size should be 1");

        Map<org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.topology.NodeKey,
            org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.topology.Node> nodeMap =
                tapiFactory.getTapiNodes();
        nodeMap.putAll(tapiAbsFactory.getTapiNodes());
        List<org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.topology.Node> tapiNodes =
            nodeMap.values().stream()
                .sorted((n1, n2) -> n1.getUuid().getValue().compareTo(n2.getUuid().getValue()))
                .collect(Collectors.toList());
        checkOtsiNode(
            getNode("ROADM", tapiNodes),
            //otsiNodeUuid,
            new Uuid(UUID.nameUUIDFromBytes("ROADM-infra".getBytes(Charset.forName("UTF-8"))).toString()),
            "infra", "ROADM-infra");
        List<Link> links = tapiAbsFactory.getTapiLinks().values().stream()
            .sorted((l1, l2) -> l1.getUuid().getValue().compareTo(l2.getUuid().getValue()))
            .collect(Collectors.toList());
        String str1 =
            "SPDR-SA1-XPDR1+XPONDER--SPDR-SA1-XPDR1+PHOTONIC_MEDIA_OTS+XPDR1-NETWORK1 and ROADM-infra--NodeEdgePoint_1";
        LOG.info("LinksCheck 0 = {} ", links.get(0).getName());
        checkOmsLink(links.get(0),
            //node1Uuid,
            new Uuid(UUID.nameUUIDFromBytes("SPDR-SA1-XPDR1+XPONDER".getBytes(Charset.forName("UTF-8"))).toString()),
            //node2Uuid,
            new Uuid(UUID.nameUUIDFromBytes("ROADM-infra".getBytes(Charset.forName("UTF-8"))).toString()),
            //tp1Uuid,
            new Uuid(
                UUID.nameUUIDFromBytes(
                        "SPDR-SA1-XPDR1+PHOTONIC_MEDIA_OTS+XPDR1-NETWORK1".getBytes(Charset.forName("UTF-8")))
                    .toString()),
            //tp2Uuid,
            new Uuid(UUID.nameUUIDFromBytes(("roadm node+nep+1").getBytes(Charset.forName("UTF-8"))).toString()),
            //linkUuid,
            new Uuid(UUID.nameUUIDFromBytes(str1.getBytes(Charset.forName("UTF-8"))).toString()),
            str1);
    }

    private void checkDsrNode(
            org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.topology.Node node,
            Uuid nodeUuid, String dsrNodeType, String nodeId) {
        assertEquals(nodeUuid, node.getUuid(), "incorrect node uuid");
        assertEquals(nodeId, node.getName().get(new NameKey("dsr/odu node name")).getValue(), "incorrect node name");
        assertEquals(AdministrativeState.UNLOCKED, node.getAdministrativeState(),
            "administrative state should be UNLOCKED");
        assertEquals(LifecycleState.INSTALLED, node.getLifecycleState(), "life-cycle state should be INSTALLED");
        assertEquals(OperationalState.ENABLED, node.getOperationalState(), "operational state should be ENABLED");
        assertThat("one value-name should be 'dsr/odu node name'",
            new ArrayList<>(node.nonnullName().keySet()), hasItem(new NameKey("dsr/odu node name")));
        assertEquals(4, node.getLayerProtocolName().size(),
            "dsr node should manage 4 protocol layers : dsr, odu, DIGITALOTN and photonic");
        assertThat("dsr node should manage 3 protocol layers : dsr, odu and photonic",
            node.getLayerProtocolName(), hasItems(LayerProtocolName.DSR, LayerProtocolName.ODU,
                LayerProtocolName.PHOTONICMEDIA));
        List<OwnedNodeEdgePoint> nepsN = node.nonnullOwnedNodeEdgePoint().values().stream()
            .filter(n -> n.getName().containsKey(new NameKey("iNodeEdgePoint_N")))
            .sorted((nep1, nep2) -> nep1.getUuid().getValue().compareTo(nep2.getUuid().getValue()))
            .collect(Collectors.toList());
        List<OwnedNodeEdgePoint> nepsC;
        switch (dsrNodeType) {
            case "switch":
                nepsC = node.nonnullOwnedNodeEdgePoint().values().stream()
                    .filter(n -> n.getName().containsKey(new NameKey("NodeEdgePoint_C")))
                    .sorted((nep1, nep2) -> nep1.getUuid().getValue().compareTo(nep2.getUuid().getValue()))
                    .collect(Collectors.toList());
                assertEquals(4, nepsN.size(), "Switch-DSR node should have 4 NEPs network");
                assertEquals(4, nepsC.size(), "Switch-DSR node should have 4 NEPs client");
                OwnedNodeEdgePoint nep1 = nepsC.get(2);
                Uuid client4NepUuid = new Uuid(
                    UUID.nameUUIDFromBytes((nodeId.split("\\+")[0] + "+DSR+XPDR2-CLIENT4")
                        .getBytes(Charset.forName("UTF-8"))).toString());
                checkNepClient100GSwitch(nep1, client4NepUuid, "XPDR2-CLIENT4", "NodeEdgePoint_C",
                    otnSwitch.getNodeId().getValue(), TapiStringConstants.DSR);
                OwnedNodeEdgePoint nep2 = nepsN.get(3);
                Uuid networkNepUuid = new Uuid(
                    UUID.nameUUIDFromBytes((nodeId.split("\\+")[0] + "+iODU+XPDR2-NETWORK1")
                        .getBytes(Charset.forName("UTF-8"))).toString());
                checkNepNetworkODU4(nep2, networkNepUuid, "XPDR2-NETWORK1", "iNodeEdgePoint_N",
                    otnSwitch.getNodeId().getValue(), TapiStringConstants.I_ODU);
                List<NodeRuleGroup> nrgList = node.nonnullNodeRuleGroup().values().stream()
                    .sorted((nrg1, nrg2) -> nrg1.getUuid().getValue().compareTo(nrg2.getUuid().getValue()))
                    .collect(Collectors.toList());
                checkNodeRuleGroupForSwitchDSR(nrgList, client4NepUuid, networkNepUuid, nodeUuid);
                break;
            case "mux":
                nepsC = node.nonnullOwnedNodeEdgePoint().values().stream()
                    .filter(n -> n.getName().containsKey(new NameKey("NodeEdgePoint_C")))
                    .sorted((nep3, nep4) -> nep3.getUuid().getValue().compareTo(nep4.getUuid().getValue()))
                    .collect(Collectors.toList());
                assertEquals(1, nepsN.size(), "Mux-DSR node should have 1 NEP network");
                assertEquals(4, nepsC.size(), "Mux-DSR node should have 4 NEPs client");
                OwnedNodeEdgePoint nep3 = nepsC.get(2);
                Uuid client3NepUuid = new Uuid(
                    UUID.nameUUIDFromBytes((nodeId.split("\\+")[0] + "+DSR+XPDR1-CLIENT3")
                        .getBytes(Charset.forName("UTF-8"))).toString());
                checkNepClient10G(nep3, client3NepUuid, "XPDR1-CLIENT3", "NodeEdgePoint_C",
                    otnMuxA.getNodeId().getValue(), TapiStringConstants.DSR);

                OwnedNodeEdgePoint nep4 = nepsN.get(0);
                Uuid networkNepUuid2 = new Uuid(
                    UUID.nameUUIDFromBytes((nodeId.split("\\+")[0] + "+iODU+XPDR1-NETWORK1")
                        .getBytes(Charset.forName("UTF-8"))).toString());
                checkNepNetworkODU4(nep4, networkNepUuid2, "XPDR1-NETWORK1", "iNodeEdgePoint_N",
                    otnMuxA.getNodeId().getValue(), TapiStringConstants.I_ODU);
                List<NodeRuleGroup> nrgList2 = node.nonnullNodeRuleGroup().values().stream()
                    .sorted((nrg1, nrg2) -> nrg1.getUuid().getValue().compareTo(nrg2.getUuid().getValue()))
                    .collect(Collectors.toList());
                checkNodeRuleGroupForMuxDSR(nrgList2, client3NepUuid, networkNepUuid2, nodeUuid);
                break;
            case "tpdr":
                nepsC = node.nonnullOwnedNodeEdgePoint().values().stream()
                    .filter(n -> n.getName().containsKey(new NameKey("100G-tpdr")))
                    .sorted((nep5, nep6) -> nep5.getUuid().getValue().compareTo(nep6.getUuid().getValue()))
                    .collect(Collectors.toList());
                assertEquals(2, nepsN.size(), "Tpdr-DSR node should have 2 NEPs network");
                assertEquals(2, nepsC.size(), "Tpdr-DSR node should have 2 NEPs client");
                OwnedNodeEdgePoint nep5 = nepsC.get(0);
                Uuid client1NepUuid = new Uuid(
                    UUID.nameUUIDFromBytes((nodeId.split("\\+")[0] + "+DSR+XPDR1-CLIENT1")
                        .getBytes(Charset.forName("UTF-8"))).toString());
                checkNepClient100GTpdr(nep5, client1NepUuid, "XPDR1-CLIENT1", "100G-tpdr",
                    tpdr100G.getNodeId().getValue(), TapiStringConstants.DSR);

                OwnedNodeEdgePoint nep6 = nepsN.get(1);
                Uuid networkNepUuid3 = new Uuid(
                    UUID.nameUUIDFromBytes((nodeId.split("\\+")[0] + "+iODU+XPDR1-NETWORK1")
                        .getBytes(Charset.forName("UTF-8"))).toString());
                checkNepNetworkODU4(nep6, networkNepUuid3, "XPDR1-NETWORK1", "iNodeEdgePoint_N",
                    tpdr100G.getNodeId().getValue(), TapiStringConstants.I_ODU);
                List<NodeRuleGroup> nrgList3 = node.nonnullNodeRuleGroup().values().stream()
                    .sorted((nrg1, nrg2) -> nrg1.getUuid().getValue().compareTo(nrg2.getUuid().getValue()))
                    .collect(Collectors.toList());
                checkNodeRuleGroupForTpdrDSR(nrgList3, client1NepUuid, networkNepUuid3, nodeUuid);
                break;
            default:
                fail();
                break;
        }
    }

    private void checkOtsiNode(
            org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.topology.Node node,
            Uuid nodeUuid, String otsiNodeType, String nodeId) {
        assertEquals(nodeUuid, node.getUuid(), "incorrect node uuid");
        assertEquals(nodeId, node.getName().get(new NameKey("otsi node name")).getValue(), "incorrect node name");
        assertEquals(AdministrativeState.UNLOCKED, node.getAdministrativeState(),
            "administrative state should be UNLOCKED");
        assertEquals(LifecycleState.INSTALLED, node.getLifecycleState(), "life-cycle state should be INSTALLED");
        assertEquals(OperationalState.ENABLED, node.getOperationalState(), "operational state should be ENABLED");
        assertThat("one value-name should be 'dsr/odu node name'",
            new ArrayList<>(node.nonnullName().keySet()), hasItem(new NameKey("otsi node name")));
        assertEquals(1, node.getLayerProtocolName().size(),
            "otsi node should manage a single protocol layer : PHOTONIC_MEDIA");
        assertEquals(LayerProtocolName.PHOTONICMEDIA, node.getLayerProtocolName().stream().findFirst().orElseThrow(),
            "otsi node should manage a single protocol layer : PHOTONIC_MEDIA");
        List<OwnedNodeEdgePoint> nepsI = node.nonnullOwnedNodeEdgePoint().values().stream()
            .filter(n -> n.getName().containsKey(new NameKey("iNodeEdgePoint")))
            .sorted((nep1, nep2) -> nep1.getUuid().getValue().compareTo(nep2.getUuid().getValue()))
            .collect(Collectors.toList());
        List<OwnedNodeEdgePoint> nepsE = node.nonnullOwnedNodeEdgePoint().values().stream()
            .filter(n -> n.getName().containsKey(new NameKey("eNodeEdgePoint")))
            .sorted((nep1, nep2) -> nep1.getUuid().getValue().compareTo(nep2.getUuid().getValue()))
            .collect(Collectors.toList());
        switch (otsiNodeType) {
            case "switch":
                assertEquals(4, nepsE.size(), "Switch-OTSi node should have 4 eNEPs");
                assertEquals(4, nepsI.size(), "Switch-OTSi node should have 4 iNEPs");
                OwnedNodeEdgePoint nep1 = nepsI.get(1);
                Uuid inepUuid = new Uuid(
                    UUID.nameUUIDFromBytes((nodeId.split("\\+")[0] + "+iOTSi+XPDR2-NETWORK2")
                        .getBytes(Charset.forName("UTF-8"))).toString());
                checkNepOtsiNode(nep1, inepUuid, "XPDR2-NETWORK2", "iNodeEdgePoint",
                    otnSwitch.getNodeId().getValue(), TapiStringConstants.I_OTSI);
                OwnedNodeEdgePoint nep2 = nepsE.get(0);
                Uuid enepUuid = new Uuid(
                    UUID.nameUUIDFromBytes((nodeId.split("\\+")[0] + "+PHOTONIC_MEDIA_OTS+XPDR2-NETWORK2")
                        .getBytes(Charset.forName("UTF-8"))).toString());
                checkNepOtsiNode(nep2, enepUuid, "XPDR2-NETWORK2", "eNodeEdgePoint",
                    otnSwitch.getNodeId().getValue(), TapiStringConstants.PHTNC_MEDIA_OTS);
                List<NodeRuleGroup> nrgList = node.nonnullNodeRuleGroup().values().stream()
                    .sorted((nrg1, nrg2) -> nrg1.getUuid().getValue().compareTo(nrg2.getUuid().getValue()))
                    .collect(Collectors.toList());
                checkNodeRuleGroupForSwitchOTSi(nrgList, enepUuid, inepUuid, nodeUuid);
                break;
            case "mux":
                assertEquals(1, nepsE.size(), "Mux-OTSi node should have 1 eNEP");
                assertEquals(1, nepsI.size(), "Mux-OTSi node should have 1 iNEPs");
                OwnedNodeEdgePoint nep3 = nepsE.get(0);
                Uuid enepUuid2 = new Uuid(
                    UUID.nameUUIDFromBytes((nodeId.split("\\+")[0] + "+PHOTONIC_MEDIA_OTS+XPDR1-NETWORK1")
                        .getBytes(Charset.forName("UTF-8"))).toString());
                checkNepOtsiNode(nep3, enepUuid2, "XPDR1-NETWORK1", "eNodeEdgePoint",
                    otnMuxA.getNodeId().getValue(), TapiStringConstants.PHTNC_MEDIA_OTS);
                OwnedNodeEdgePoint nep4 = nepsI.get(0);
                Uuid inepUuid2 = new Uuid(
                    UUID.nameUUIDFromBytes((nodeId.split("\\+")[0] + "+iOTSi+XPDR1-NETWORK1")
                        .getBytes(Charset.forName("UTF-8"))).toString());
                checkNepOtsiNode(nep4, inepUuid2, "XPDR1-NETWORK1", "iNodeEdgePoint",
                    otnMuxA.getNodeId().getValue(), TapiStringConstants.I_OTSI);
                List<NodeRuleGroup> nrgList2 = node.nonnullNodeRuleGroup().values().stream()
                    .sorted((nrg1, nrg2) -> nrg1.getUuid().getValue().compareTo(nrg2.getUuid().getValue()))
                    .collect(Collectors.toList());
                checkNodeRuleGroupForMuxOTSi(nrgList2, enepUuid2, inepUuid2, nodeUuid);
                break;
            case "tpdr":
                assertEquals(2, nepsE.size(), "Tpdr-OTSi node should have 2 eNEPs");
                assertEquals(2, nepsI.size(), "Tpdr-OTSi node should have 2 iNEPs");
                OwnedNodeEdgePoint nep5 = nepsE.get(0);
                Uuid enepUuid3 = new Uuid(
                    UUID.nameUUIDFromBytes((nodeId.split("\\+")[0] + "+PHOTONIC_MEDIA_OTS+XPDR1-NETWORK1")
                        .getBytes(Charset.forName("UTF-8"))).toString());
                checkNepOtsiNode(nep5, enepUuid3, "XPDR1-NETWORK1", "eNodeEdgePoint",
                    tpdr100G.getNodeId().getValue(), TapiStringConstants.PHTNC_MEDIA_OTS);
                OwnedNodeEdgePoint nep6 = nepsI.get(0);
                Uuid inepUuid3 = new Uuid(
                    UUID.nameUUIDFromBytes((nodeId.split("\\+")[0] + "+iOTSi+XPDR1-NETWORK1")
                        .getBytes(Charset.forName("UTF-8"))).toString());
                checkNepOtsiNode(nep6, inepUuid3, "XPDR1-NETWORK1", "iNodeEdgePoint",
                    tpdr100G.getNodeId().getValue(), TapiStringConstants.I_OTSI);
                List<NodeRuleGroup> nrgList3 = node.nonnullNodeRuleGroup().values().stream()
                    .sorted((nrg1, nrg2) -> nrg1.getUuid().getValue().compareTo(nrg2.getUuid().getValue()))
                    .collect(Collectors.toList());
                checkNodeRuleGroupForTpdrOTSi(nrgList3, enepUuid3, inepUuid3, nodeUuid);
                break;
            default:
                Iterator<OwnedNodeEdgePoint> nepIterator = node.nonnullOwnedNodeEdgePoint().values().iterator();
                int count = 1;
                while (nepIterator.hasNext()) {
                    OwnedNodeEdgePoint nep = nepIterator.next();
                    Uuid nepUuid = new Uuid(
                        UUID.nameUUIDFromBytes((String.join("+", "roadm node", "nep", String.valueOf(count)))
                            .getBytes(Charset.forName("UTF-8"))).toString());
                    checkNepOtsiRdmNode(nep, nepUuid, new StringBuilder("NodeEdgePoint_").append(count).toString(),
                        "NodeEdgePoint name");
                    count++;
                }
                List<NodeRuleGroup> nrgList4 = node.nonnullNodeRuleGroup().values().stream()
                    .sorted((nrg1, nrg2) -> nrg1.getUuid().getValue().compareTo(nrg2.getUuid().getValue()))
                    .collect(Collectors.toList());
                checkNodeRuleGroupForRdmInfra(nrgList4, count - 1);
                break;
        }
    }

    private void checkNepClient10G(
            OwnedNodeEdgePoint nep, Uuid nepUuid, String portName, String nepName, String nodeId, String extension) {
        assertEquals(nepUuid, nep.getUuid(), "bad uuid for " + portName);
        Name name = new ArrayList<>(nep.nonnullName().values()).get(0);
        assertEquals(
            String.join("+", nodeId, extension, portName),
            name.getValue(),
            "value of client nep should be '" + portName + "'");
        assertEquals(nepName, name.getValueName(),
            "value-name of client nep for '" + portName + "' should be '" + nepName + "'");
        List<LAYERPROTOCOLQUALIFIER> lpql = nep.getSupportedCepLayerProtocolQualifierInstances().stream()
                .map(lpqi -> lpqi.getLayerProtocolQualifier())
                .collect(Collectors.toList());
        assertEquals(3, lpql.size(), "Client nep should support 3 kind of cep");
        assertThat("client nep should support 3 kind of cep", lpql,
            hasItems(ODUTYPEODU2.VALUE, ODUTYPEODU2E.VALUE, DIGITALSIGNALTYPE10GigELAN.VALUE));
        assertEquals(LayerProtocolName.DSR, nep.getLayerProtocolName(), "client nep should be of DSR(ETH) protocol ");
        checkCommonPartOfNep(nep, false);
        checkSIP(nep, portName, nodeId, extension);
    }

    private void checkNepNetworkODU4(
            OwnedNodeEdgePoint nep, Uuid nepUuid, String portName, String nepName, String nodeId, String extension) {
        assertEquals(nepUuid, nep.getUuid(), "bad uuid for " + portName);
        List<Name> nameList = new ArrayList<>(nep.nonnullName().values());
        Name name = nameList.get(0);
        assertEquals(String.join("+", nodeId, extension, portName), name.getValue(),
            "value of network nep should be '" + portName + "'");
        assertEquals(nepName, name.getValueName(),
            "value-name of network nep for '" + portName + "' should be '" + nepName + "'");
        List<LAYERPROTOCOLQUALIFIER> lpql = nep.getSupportedCepLayerProtocolQualifierInstances().stream()
                .map(lpqi -> lpqi.getLayerProtocolQualifier())
                .collect(Collectors.toList());
        assertEquals(1, lpql.size(), "Network nep should support 1 kind of cep");
        assertThat("network nep should support ODU4 kind of cep", lpql, hasItem(ODUTYPEODU4.VALUE));
        assertEquals(LayerProtocolName.ODU, nep.getLayerProtocolName(), "network nep should be of ODU protocol type");
        checkCommonPartOfNep(nep, false);
        checkSIP(nep, portName, nodeId, extension);
    }

    private void checkNodeRuleGroupForTpdrDSR(
            List<NodeRuleGroup> nrgList, Uuid clientNepUuid, Uuid networkNepUuid, Uuid nodeUuid) {
        assertEquals(4, nrgList.size(), "transponder DSR should contain 4 node rule group (2*DSR/I_ODU+2*E_ODU/I_ODU)");
        for (NodeRuleGroup nodeRuleGroup : nrgList) {
            assertEquals(2, nodeRuleGroup.getNodeEdgePoint().size(),
                "each node-rule-group should contain 2 NEP for transponder DSR");
        }
        List<NodeEdgePoint> nodeEdgePointList = new ArrayList<>(nrgList.get(0).nonnullNodeEdgePoint().values());
        assertThat("node-rule-group nb 1 should be between nep-client1 and nep-network1",
            nodeEdgePointList.get(0).getNodeEdgePointUuid().getValue(),
            either(containsString(networkNepUuid.getValue())).or(containsString(clientNepUuid.getValue())));
        assertThat("node-rule-group nb 1 should be between nep-client1 and nep-network1",
            nodeEdgePointList.get(1).getNodeEdgePointUuid().getValue(),
            either(containsString(networkNepUuid.getValue())).or(containsString(clientNepUuid.getValue())));
        assertEquals(nodeEdgePointList.get(0).getNodeUuid(), nodeUuid,
            "node-rule-group nb 1 should be between nep-client1 and nep-network1 of the same node");
        assertEquals(nodeEdgePointList.get(1).getNodeUuid(), nodeUuid,
            "node-rule-group nb 1 should be between nep-client1 and nep-network1 of the same node");
        List<Rule> rule = new ArrayList<>(nrgList.get(1).nonnullRule().values());
        assertEquals(1, rule.size(), "node-rule-group nb 1 should contain a single rule");
        assertEquals("forward", rule.get(0).getLocalId(), "local-id of the rule should be 'forward'");
        assertEquals(FORWARDINGRULEMAYFORWARDACROSSGROUP.VALUE, rule.get(0).getForwardingRule(),
            "the forwarding rule should be 'MAYFORWARDACROSSGROUP'");
        assertEquals(RuleType.FORWARDING, rule.get(0).getRuleType().iterator().next(),
            "the rule type should be 'FORWARDING'");
    }

    private void checkNodeRuleGroupForMuxDSR(
            List<NodeRuleGroup> nrgList, Uuid clientNepUuid, Uuid networkNepUuid, Uuid nodeUuid) {
        assertEquals(8, nrgList.size(), "muxponder DSR should contain 8 node rule group (4*DSR/I_ODU + 4*E_ODU/I_ODU)");
        for (NodeRuleGroup nodeRuleGroup : nrgList) {
            assertEquals(2, nodeRuleGroup.getNodeEdgePoint().size(),
                "each node-rule-group should contain 2 NEP for muxponder DSR");
        }
        List<NodeEdgePoint> nodeEdgePointList = new ArrayList<>(nrgList.get(0).nonnullNodeEdgePoint().values());
        assertThat("node-rule-group nb 2 should be between nep-client4 and nep-network1",
            nodeEdgePointList.get(1).getNodeEdgePointUuid().getValue(),
            either(containsString(networkNepUuid.getValue())).or(containsString(clientNepUuid.getValue())));
        assertThat("node-rule-group nb 2 should be between nep-client4 and nep-network1",
            nodeEdgePointList.get(1).getNodeEdgePointUuid().getValue(),
            either(containsString(networkNepUuid.getValue())).or(containsString(clientNepUuid.getValue())));
        assertEquals(nodeEdgePointList.get(0).getNodeUuid(), nodeUuid,
            "node-rule-group nb 2 should be between nep-client4 and nep-network1 of the same node");
        assertEquals(nodeEdgePointList.get(1).getNodeUuid(), nodeUuid,
            "node-rule-group nb 2 should be between nep-client4 and nep-network1 of the same node");
        List<Rule> rule = new ArrayList<>(nrgList.get(1).nonnullRule().values());
        assertEquals(1, rule.size(), "node-rule-group nb 2 should contain a single rule");
        assertEquals("forward", rule.get(0).getLocalId(), "local-id of the rule should be 'forward'");
        assertEquals(FORWARDINGRULEMAYFORWARDACROSSGROUP.VALUE, rule.get(0).getForwardingRule(),
            "the forwarding rule should be 'MAYFORWARDACROSSGROUP'");
        assertEquals(RuleType.FORWARDING, rule.get(0).getRuleType().iterator().next(),
            "the rule type should be 'FORWARDING'");
    }

    private void checkNodeRuleGroupForSwitchDSR(
            List<NodeRuleGroup> nrgList, Uuid clientNepUuid, Uuid networkNepUuid, Uuid nodeUuid) {
        assertEquals(2, nrgList.size(), "Switch-DSR should contain 2 node rule groups (DSR/I_ODU + E_ODU/I_ODU)");
        assertEquals(8, nrgList.get(0).getNodeEdgePoint().size(), "Switch-DSR node-rule-group should contain 8 NEP");
        List<NodeEdgePoint> nrg = nrgList.get(0).nonnullNodeEdgePoint().values().stream()
            .sorted((nrg1, nrg2) -> nrg1.getNodeEdgePointUuid().getValue()
                .compareTo(nrg2.getNodeEdgePointUuid().getValue()))
            .collect(Collectors.toList());
        assertEquals(networkNepUuid, nrg.get(6).getNodeEdgePointUuid(),
            "in the sorted node-rule-group, nep number 7 should be XPDR2-NETWORK1");
        assertEquals(clientNepUuid, nrg.get(5).getNodeEdgePointUuid(),
            "in the sorted node-rule-group, nep number 6 should be XPDR2-CLIENT4");
        assertEquals(nodeUuid, nrg.get(4).getNodeUuid(),
            "any item of the node-rule-group should have the same nodeUuid");
        assertEquals(nodeUuid, nrg.get(3).getNodeUuid(),
            "any item of the node-rule-group should have the same nodeUuid");
        @Nullable
        List<Rule> ruleList = new ArrayList<>(nrgList.get(0).nonnullRule().values());
        assertEquals(1, ruleList.size(), "node-rule-group should contain a single rule");
        assertEquals("forward", ruleList.get(0).getLocalId(), "local-id of the rule should be 'forward'");
        assertEquals(FORWARDINGRULEMAYFORWARDACROSSGROUP.VALUE, ruleList.get(0).getForwardingRule(),
            "the forwarding rule should be 'MAYFORWARDACROSSGROUP'");
        assertEquals(RuleType.FORWARDING, ruleList.get(0).getRuleType().iterator().next(),
            "the rule type should be 'FORWARDING'");
    }

    private void checkNodeRuleGroupForRdmInfra(List<NodeRuleGroup> nrgList, int nbNeps) {
        // if no XPDR is connected, no OTS is created and no NodeRuleGroup is created
        assertTrue(nrgList.size() <= 1, "RDM infra node - OTSi should contain maximum one node rule groups");
        // When a nrg is present it shall respect the following
        if (nrgList.isEmpty()) {
            return;
        }
        if (nbNeps > 0) {
            List<NodeEdgePoint> nodeEdgePointList = new ArrayList<>(nrgList.get(0).getNodeEdgePoint().values());
            assertEquals(nbNeps, nodeEdgePointList.size(),
                "RDM infra node -rule-group should contain " + nbNeps + " NEP");
        } else {
            assertNull(nrgList.get(0).getNodeEdgePoint(), "RDM infra node -rule-group should contain no NEP");
        }
        List<Rule> ruleList = new ArrayList<>(nrgList.get(0).nonnullRule().values());
        assertEquals(1, ruleList.size(), "node-rule-group should contain a single rule");
        assertEquals("forward", ruleList.get(0).getLocalId(), "local-id of the rule should be 'forward'");
        assertEquals(FORWARDINGRULEMAYFORWARDACROSSGROUP.VALUE, ruleList.get(0).getForwardingRule(),
            "the forwarding rule should be 'MAYFORWARDACROSSGROUP'");
        assertEquals(RuleType.FORWARDING, ruleList.get(0).getRuleType().iterator().next(),
            "the rule type should be 'FORWARDING'");
    }

    private void checkNodeRuleGroupForTpdrOTSi(
            List<NodeRuleGroup> nrgList, Uuid enepUuid, Uuid inepUuid, Uuid nodeUuid) {
        assertEquals(2, nrgList.size(), "Tpdr-OTSi should contain two node rule groups");
        List<NodeEdgePoint> nodeEdgePointList = new ArrayList<>(nrgList.get(0).getNodeEdgePoint().values());
        assertEquals(2, nodeEdgePointList.size(), "Tpdr-OTSi node-rule-group should contain 2 NEP");
        assertThat("Tpdr-OTSi node-rule-group should be between eNEP and iNEP of XPDR1-NETWORK1",
            nodeEdgePointList.get(0).getNodeEdgePointUuid().getValue(),
            either(containsString(enepUuid.getValue())).or(containsString(inepUuid.getValue())));
        assertThat("Tpdr-OTSi node-rule-group should be between eNEP and iNEP of XPDR1-NETWORK1",
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
        assertEquals(RuleType.FORWARDING, ruleList.get(0).getRuleType().iterator().next(),
            "the rule type should be 'FORWARDING'");
    }

    private void checkNodeRuleGroupForMuxOTSi(
            List<NodeRuleGroup> nrgList, Uuid enepUuid, Uuid inepUuid, Uuid nodeUuid) {
        assertEquals(1, nrgList.size(), "Mux-OTSi should contain a single node rule group");
        List<NodeEdgePoint> nodeEdgePointList = new ArrayList<>(nrgList.get(0).getNodeEdgePoint().values());
        assertEquals(2, nodeEdgePointList.size(), "Mux-OTSi node-rule-group should contain 2 NEP");
        assertThat("Mux-OTSi node-rule-group should be between eNEP and iNEP of XPDR1-NETWORK1",
            nodeEdgePointList.get(0).getNodeEdgePointUuid().getValue(),
            either(containsString(enepUuid.getValue())).or(containsString(inepUuid.getValue())));
        assertThat("Mux-OTSi node-rule-group should be between eNEP and iNEP of XPDR1-NETWORK1",
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
        assertEquals(RuleType.FORWARDING, ruleList.get(0).getRuleType().iterator().next(),
            "the rule type should be 'FORWARDING'");
    }

    private void checkNodeRuleGroupForSwitchOTSi(
            List<NodeRuleGroup> nrgList, Uuid enepUuid, Uuid inepUuid, Uuid nodeUuid) {
        assertEquals(4, nrgList.size(), "Switch-OTSi should contain 4 node rule group");
        for (NodeRuleGroup nodeRuleGroup : nrgList) {
            assertEquals(2, nodeRuleGroup.getNodeEdgePoint().size(),
                "each node-rule-group should contain 2 NEP for Switch-OTSi");
        }
        List<NodeEdgePoint> nodeEdgePointList1 = new ArrayList<>(nrgList.get(3).nonnullNodeEdgePoint().values());
        assertThat("Switch-OTSi node-rule-group nb 4 should be between eNEP and iNEP of XPDR2-NETWORK2",
            nodeEdgePointList1.get(0).getNodeEdgePointUuid().getValue(),
            either(containsString(enepUuid.getValue())).or(containsString(inepUuid.getValue())));
        assertThat("Switch-OTSi node-rule-group nb 4 should be between eNEP and iNEP of XPDR2-NETWORK2",
            nodeEdgePointList1.get(1).getNodeEdgePointUuid().getValue(),
            either(containsString(enepUuid.getValue())).or(containsString(inepUuid.getValue())));
        List<NodeEdgePoint> nodeEdgePointList0 = new ArrayList<>(nrgList.get(0).getNodeEdgePoint().values());
        assertEquals(nodeUuid, nodeEdgePointList0.get(0).getNodeUuid(),
            "any item of the node-rule-group should have the same nodeUuid");
        assertEquals(nodeUuid, nodeEdgePointList0.get(1).getNodeUuid(),
            "any item of the node-rule-group should have the same nodeUuid");
        List<Rule> ruleList0 = new ArrayList<>(nrgList.get(0).nonnullRule().values());
        assertEquals(1, ruleList0.size(), "node-rule-group should contain a single rule");
        assertEquals("forward", ruleList0.get(0).getLocalId(),"local-id of the rule should be 'forward'");
        assertEquals(FORWARDINGRULEMAYFORWARDACROSSGROUP.VALUE, ruleList0.get(0).getForwardingRule(),
            "the forwarding rule should be 'MAYFORWARDACROSSGROUP'");
        assertEquals(RuleType.FORWARDING, ruleList0.get(0).getRuleType().iterator().next(),
            "the rule type should be 'FORWARDING'");
    }

    private void checkNepClient100GSwitch(
            OwnedNodeEdgePoint nep, Uuid nepUuid, String portName, String nepName, String nodeId, String extension) {
        assertEquals(nepUuid, nep.getUuid(), "bad uuid for " + portName);
        List<Name> nameList = new ArrayList<>(nep.nonnullName().values());
        assertEquals(String.join("+", nodeId, extension, portName), nameList.get(0).getValue(),
            "value of client nep should be '" + portName + "'");
        assertEquals(nepName, nameList.get(0).getValueName(),
            "value-name of client nep for '" + portName + "' should be '" + nepName + "'");
        List<LAYERPROTOCOLQUALIFIER> lpql = nep.getSupportedCepLayerProtocolQualifierInstances().stream()
                .map(lpqi -> lpqi.getLayerProtocolQualifier())
                .collect(Collectors.toList());
        assertEquals(2, lpql.size(), "Client nep should support 2 kind of cep");
        assertThat("client nep should support 2 kind of cep", lpql,
            hasItems(ODUTYPEODU4.VALUE, DIGITALSIGNALTYPE100GigE.VALUE));
        assertEquals(LayerProtocolName.DSR, nep.getLayerProtocolName(), "client nep should be of DSR(ETH) protocol");
        checkCommonPartOfNep(nep, false);
        checkSIP(nep, portName, nodeId, extension);
    }

    private void checkNepClient100GTpdr(
            OwnedNodeEdgePoint nep, Uuid nepUuid, String portName, String nepName, String nodeId, String extension) {
        assertEquals(nepUuid, nep.getUuid(), "bad uuid for " + portName);
        List<Name> nameList = new ArrayList<>(nep.nonnullName().values());
        assertEquals(String.join("+", nodeId, extension, portName), nameList.get(0).getValue(),
            "value of client nep should be '" + portName + "'");
        assertEquals(nepName, nameList.get(0).getValueName(),
            "value-name of client nep for '" + portName + "' should be 100G-tpdr'");
        List<LAYERPROTOCOLQUALIFIER> lpql = nep.getSupportedCepLayerProtocolQualifierInstances().stream()
                .map(lpqi -> lpqi.getLayerProtocolQualifier())
                .collect(Collectors.toList());
        assertEquals(1, lpql.size(), "Client nep should support 1 kind of cep");
        assertThat("client nep should support 2 kind of cep", lpql, hasItems(DIGITALSIGNALTYPE100GigE.VALUE));
        assertEquals(LayerProtocolName.DSR, nep.getLayerProtocolName(), "client nep should be of DSR(ETH) protocol");
        checkCommonPartOfNep(nep, false);
        checkSIP(nep, portName, nodeId, extension);
    }

    private void checkNepOtsiNode(
            OwnedNodeEdgePoint nep, Uuid nepUuid, String portName, String nepName, String nodeId, String extension) {
        assertEquals(nepUuid, nep.getUuid(), "bad uuid for " + portName);
        List<Name> nameList = new ArrayList<>(nep.nonnullName().values());
        assertEquals(String.join("+", nodeId, extension, portName), nameList.get(0).getValue(),
            "value of OTSi nep should be '" + portName + "'");
        assertEquals(nepName, nameList.get(0).getValueName(), "value-name of OTSi nep should be '" + nepName + "'");
        List<LAYERPROTOCOLQUALIFIER> lpql = nep.getSupportedCepLayerProtocolQualifierInstances().stream()
                .map(lpqi -> lpqi.getLayerProtocolQualifier())
                .collect(Collectors.toList());
        assertEquals(2, lpql.size(), "OTSi nep should support 2 kind of cep");
        assertThat("OTSi nep should support 2 kind of cep", lpql,
            hasItems(PHOTONICLAYERQUALIFIEROMS.VALUE, PHOTONICLAYERQUALIFIEROTSi.VALUE));
        assertEquals(LayerProtocolName.PHOTONICMEDIA, nep.getLayerProtocolName(),
            "OTSi nep should be of PHOTONIC_MEDIA protocol type");
        assertEquals(1, nep.getMappedServiceInterfacePoint().size(), "OTSi nep should support one SIP");
        checkCommonPartOfNep(nep, false);
        checkSIP(nep, portName, nodeId, extension);
    }

    private void checkSIP(OwnedNodeEdgePoint nep, String portName, String nodeId, String extension) {
        Uuid sipUuid = new Uuid(
            UUID.nameUUIDFromBytes((String.join("+", "SIP", nodeId, extension, portName))
                    .getBytes(Charset.forName("UTF-8")))
                .toString());
        assertEquals(
            sipUuid,
            nep.getMappedServiceInterfacePoint().get(new MappedServiceInterfacePointKey(sipUuid))
                .getServiceInterfacePointUuid(),
            "service-interface-point-uuid of network nep for '" + portName + "' should be '"
                + String.join("+", "SIP", portName) + "'");
    }

    private void checkNepOtsiRdmNode(OwnedNodeEdgePoint nep, Uuid nepUuid, String portName, String nepName) {
        assertEquals(nepUuid, nep.getUuid(), "bad uuid for " + portName);
        List<Name> nameList = new ArrayList<>(nep.nonnullName().values());
        assertEquals(portName, nameList.get(0).getValue(), "value of OTSi nep should be '" + portName + "'");
        assertEquals(nepName, nameList.get(0).getValueName(), "value-name of OTSi nep should be '" + nepName + "'");
        List<LAYERPROTOCOLQUALIFIER> lpql = nep.getSupportedCepLayerProtocolQualifierInstances().stream()
                .map(lpqi -> lpqi.getLayerProtocolQualifier())
                .collect(Collectors.toList());
        assertEquals(1, lpql.size(), "OTSi nep of RDM infra node should support only 1 kind of cep");
        assertThat("OTSi nep should support OTS cep", lpql, hasItems(PHOTONICLAYERQUALIFIEROTS.VALUE));
        assertEquals(LayerProtocolName.PHOTONICMEDIA, nep.getLayerProtocolName(),
            "OTSi nep should be of PHOTONIC_MEDIA protocol type");
        assertEquals(0, nep.nonnullMappedServiceInterfacePoint().size(), "OTSi nep of RDM infra should support no SIP");
        checkCommonPartOfNep(nep, true);
    }

    private void checkCommonPartOfNep(OwnedNodeEdgePoint nep, boolean isRdm) {
        assertEquals(Direction.BIDIRECTIONAL, nep.getDirection(),
            "link port direction should be DIRECTIONAL");
        assertEquals(AdministrativeState.UNLOCKED, nep.getAdministrativeState(),
            "administrative state should be UNLOCKED");
//      TODO: convert this test since terminationState is migrated to CEP attribute in TAPI 2.4
//        assertEquals(TerminationState.TERMINATEDBIDIRECTIONAL, nep.getTerminationState(),
//            "termination state should be TERMINATED BIDIRECTIONAL");
        assertEquals(LifecycleState.INSTALLED, nep.getLifecycleState(),
            "life-cycle state should be INSTALLED");
        if (!isRdm) {
            assertEquals(1, nep.getMappedServiceInterfacePoint().size(), "client nep should support 1 SIP");
        }
//      TODO: convert this test since terminationState is migrated to CEP attribute in TAPI 2.4
//        assertEquals(TerminationDirection.BIDIRECTIONAL, nep.getTerminationDirection(),
//            "termination direction should be BIDIRECTIONAL");
        assertEquals(OperationalState.ENABLED, nep.getOperationalState(),
            "operational state of client nep should be ENABLED");
        assertEquals(PortRole.SYMMETRIC, nep.getLinkPortRole(), "link-port-role of client nep should be SYMMETRIC");
    }

    private void checkOtnLink(
            Link link, Uuid node1Uuid, Uuid node2Uuid, Uuid tp1Uuid, Uuid tp2Uuid, Uuid linkUuid, String linkName) {
        assertEquals(linkName, link.getName().get(new NameKey("otn link name")).getValue(), "bad name for the link");
        assertEquals(linkUuid, link.getUuid(), "bad uuid for link");
        assertEquals(CAPACITYUNITGBPS.VALUE, link.getAvailableCapacity().getTotalSize().getUnit(),
            "Available capacity unit should be MBPS");
        String prefix = linkName.split("-")[0];
        if ("OTU4".equals(prefix)) {
            assertEquals(Uint64.valueOf(0), link.getAvailableCapacity().getTotalSize().getValue(),
                "Available capacity -total size value should be 0");
        } else if ("ODTU4".equals(prefix)) {
            assertEquals(Uint64.valueOf(100000), link.getAvailableCapacity().getTotalSize().getValue(),
                "Available capacity -total size value should be 100 000");
        }
        assertEquals(CAPACITYUNITGBPS.VALUE, link.getTotalPotentialCapacity().getTotalSize().getUnit(),
            "Total capacity unit should be GBPS");
        assertEquals(Decimal64.valueOf("100"), link.getTotalPotentialCapacity().getTotalSize().getValue(),
            "Total capacity -total size value should be 100");
        if ("OTU4".equals(prefix)) {
            assertEquals(
                LayerProtocolName.PHOTONICMEDIA.getName(),
                link.getLayerProtocolName().stream().findFirst().orElseThrow().getName(),
                "otn link should be between 2 nodes of protocol layers PHOTONIC_MEDIA");
        } else if ("ODTU4".equals(prefix)) {
            assertEquals(
                LayerProtocolName.ODU.getName(),
                link.getLayerProtocolName().stream().findFirst().orElseThrow().getName(),
                "otn link should be between 2 nodes of protocol layers ODU");
        }
        assertEquals(ForwardingDirection.BIDIRECTIONAL, link.getDirection(), "otn tapi link should be BIDIRECTIONAL");
        List<org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121
                .link.NodeEdgePoint> nodeEdgePointList = new ArrayList<>(link.nonnullNodeEdgePoint().values());
        LOG.info("OUTPUT: Node1UUID = {}, Node2UU2D = {},", node1Uuid, node2Uuid);
        LOG.info("NEPLIST = {}", nodeEdgePointList);
        assertEquals(topologyUuid, nodeEdgePointList.get(0).getTopologyUuid(),
            "topology uuid should be the same for the two termination point of the link");
        assertEquals(topologyUuid, nodeEdgePointList.get(1).getTopologyUuid(),
            "topology uuid should be the same for the two termination point of the link");
        assertThat("otn links should terminate on two distinct nodes",
            nodeEdgePointList.get(0).getNodeUuid().getValue(),
            either(containsString(node1Uuid.getValue())).or(containsString(node2Uuid.getValue())));
        assertThat("otn links should terminate on two distinct nodes",
            nodeEdgePointList.get(1).getNodeUuid().getValue(),
            either(containsString(node1Uuid.getValue())).or(containsString(node2Uuid.getValue())));
        assertThat("otn links should terminate on two distinct tps",
            nodeEdgePointList.get(0).getNodeEdgePointUuid().getValue(),
            either(containsString(tp1Uuid.getValue())).or(containsString(tp2Uuid.getValue())));
        assertThat("otn links should terminate on two distinct tps",
            nodeEdgePointList.get(1).getNodeEdgePointUuid().getValue(),
            either(containsString(tp1Uuid.getValue())).or(containsString(tp2Uuid.getValue())));
        assertEquals(OperationalState.ENABLED, link.getOperationalState(), "operational state should be ENABLED");
        assertEquals(AdministrativeState.UNLOCKED, link.getAdministrativeState(),
            "administrative state should be UNLOCKED");
    }

    private void checkOmsLink(
            Link link, Uuid node1Uuid, Uuid node2Uuid, Uuid tp1Uuid, Uuid tp2Uuid, Uuid linkUuid, String linkName) {
        assertEquals(linkName, link.getName().get(new NameKey("OTS link name")).getValue(), "bad name for the link");
        assertEquals(linkUuid, link.getUuid(), "bad uuid for link");
        assertEquals(
            LayerProtocolName.PHOTONICMEDIA.getName(),
            link.getLayerProtocolName().stream().findFirst().orElseThrow().getName(),
            "oms link should be between 2 nodes of protocol layers PHOTONIC_MEDIA");
        assertEquals(ForwardingDirection.BIDIRECTIONAL, link.getDirection(), "otn tapi link should be BIDIRECTIONAL");
        List<org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121
                .link.NodeEdgePoint> nodeEdgePointList = new ArrayList<>(link.nonnullNodeEdgePoint().values());
        assertEquals(2, nodeEdgePointList.size(), "oms link should be between 2 neps");
        assertEquals(topologyUuid, nodeEdgePointList.get(0).getTopologyUuid(),
            "topology uuid should be the same for the two termination point of the link");
        assertEquals(topologyUuid, nodeEdgePointList.get(1).getTopologyUuid(),
            "topology uuid should be the same for the two termination point of the link");
        assertThat("oms links should terminate on two distinct nodes",
            nodeEdgePointList.get(0).getNodeUuid().getValue(),
            either(containsString(node1Uuid.getValue())).or(containsString(node2Uuid.getValue())));
        assertThat("oms links should terminate on two distinct nodes",
            nodeEdgePointList.get(1).getNodeUuid().getValue(),
            either(containsString(node1Uuid.getValue())).or(containsString(node2Uuid.getValue())));
        assertThat("oms links should terminate on two distinct tps",
            nodeEdgePointList.get(0).getNodeEdgePointUuid().getValue(),
            either(containsString(tp1Uuid.getValue())).or(containsString(tp2Uuid.getValue())));
        assertThat("oms links should terminate on two distinct tps",
            nodeEdgePointList.get(1).getNodeEdgePointUuid().getValue(),
            either(containsString(tp1Uuid.getValue())).or(containsString(tp2Uuid.getValue())));
    }

    private Node changeTerminationPointState(Node initialNode, String tpid, AdminStates admin, State oper) {
        var tpdr1Bldr = new org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226
                .Node1Builder(initialNode.augmentation(Node1.class));
        Map<TerminationPointKey, TerminationPoint> tps = new HashMap<>(tpdr1Bldr.getTerminationPoint());
        TerminationPointBuilder tpBldr = new TerminationPointBuilder(tps.get(new TerminationPointKey(new TpId(tpid))));
        TerminationPoint1Builder tp1Bldr =
            new TerminationPoint1Builder(tpBldr.augmentation(TerminationPoint1.class))
                .setAdministrativeState(admin)
                .setOperationalState(oper);
        tpBldr.addAugmentation(tp1Bldr.build());
        tps.replace(tpBldr.key(), tpBldr.build());
        return new NodeBuilder(initialNode).addAugmentation(tpdr1Bldr.setTerminationPoint(tps).build()).build();
    }

    private org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226
                .networks.network.Link changeOtnLinkState(
            org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226
                .networks.network.Link initiallink,
            AdminStates admin,
            State oper) {
        var linkBldr = new org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226
                .networks.network.LinkBuilder(initiallink);
        return linkBldr
            .addAugmentation(
                new Link1Builder(linkBldr.augmentation(Link1.class))
                    .setAdministrativeState(admin)
                    .setOperationalState(oper)
                    .build())
            .build();
    }

    private org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.topology.Node getNode(
            String searchedChar,
            List<org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.topology.Node> nodeList) {
        org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.topology.Node lastNode = null;
        for (var node : nodeList) {
            for (Name name : node.getName().values()) {
                if (name.getValue().contains(searchedChar)) {
                    return node;
                }
            }
            lastNode = node;
        }
        LOG.info("pattern '{}' not found in list of nodes", searchedChar);
        return lastNode;
    }

}
