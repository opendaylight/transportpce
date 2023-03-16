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
import static org.junit.jupiter.api.Assertions.fail;

import com.google.common.util.concurrent.FluentFuture;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
import org.opendaylight.transportpce.tapi.utils.TapiLink;
import org.opendaylight.transportpce.tapi.utils.TapiLinkImpl;
import org.opendaylight.transportpce.tapi.utils.TapiTopologyDataUtils;
import org.opendaylight.transportpce.test.AbstractTest;
import org.opendaylight.transportpce.test.utils.TopologyDataUtils;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev211210.Link1;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev211210.Link1Builder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev211210.TerminationPoint1;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev211210.TerminationPoint1Builder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.state.types.rev191129.State;
import org.opendaylight.yang.gen.v1.http.org.openroadm.equipment.states.types.rev191129.AdminStates;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.types.rev211210.OpenroadmTpType;
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
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev181210.AdministrativeState;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev181210.CapacityUnit;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev181210.ForwardingDirection;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev181210.LayerProtocolName;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev181210.LifecycleState;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev181210.OperationalState;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev181210.PortDirection;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev181210.PortRole;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev181210.TerminationDirection;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev181210.TerminationState;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev181210.Uuid;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev181210.global._class.Name;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev181210.global._class.NameKey;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.dsr.rev181210.DIGITALSIGNALTYPE100GigE;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.dsr.rev181210.DIGITALSIGNALTYPE10GigELAN;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.odu.rev181210.ODUTYPEODU2;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.odu.rev181210.ODUTYPEODU2E;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.odu.rev181210.ODUTYPEODU4;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.photonic.media.rev181210.PHOTONICLAYERQUALIFIEROMS;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.photonic.media.rev181210.PHOTONICLAYERQUALIFIEROTSi;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.ForwardingRule;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.RuleType;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.node.NodeRuleGroup;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.node.OwnedNodeEdgePoint;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.node.OwnedNodeEdgePointKey;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.node.edge.point.MappedServiceInterfacePointKey;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.node.rule.group.NodeEdgePoint;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.node.rule.group.Rule;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.topology.Link;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.KeyedInstanceIdentifier;
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
        TopologyDataUtils.writeTopologyFromFileToDatastore(getDataStoreContextUtil(),
            TapiTopologyDataUtils.OPENROADM_TOPOLOGY_FILE, InstanceIdentifiers.OVERLAY_NETWORK_II);
        TopologyDataUtils.writeTopologyFromFileToDatastore(getDataStoreContextUtil(),
            TapiTopologyDataUtils.OTN_TOPOLOGY_FILE, InstanceIdentifiers.OTN_NETWORK_II);
        TopologyDataUtils.writePortmappingFromFileToDatastore(getDataStoreContextUtil(),
            TapiTopologyDataUtils.PORTMAPPING_FILE);

        KeyedInstanceIdentifier<Node, NodeKey> muxAIID = InstanceIdentifier.create(Networks.class)
            .child(org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.networks.Network
                .class, new NetworkKey(new NetworkId("otn-topology")))
            .child(Node.class, new NodeKey(new NodeId("SPDR-SA1-XPDR1")));
        FluentFuture<Optional<Node>> muxAFuture = dataBroker.newReadOnlyTransaction()
            .read(LogicalDatastoreType.CONFIGURATION, muxAIID);
        KeyedInstanceIdentifier<Node, NodeKey> muxCIID = InstanceIdentifier.create(Networks.class)
            .child(org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.networks.Network
                .class, new NetworkKey(new NetworkId("otn-topology")))
            .child(Node.class, new NodeKey(new NodeId("SPDR-SC1-XPDR1")));
        FluentFuture<Optional<Node>> muxCFuture = dataBroker.newReadOnlyTransaction()
            .read(LogicalDatastoreType.CONFIGURATION, muxCIID);
        KeyedInstanceIdentifier<Node, NodeKey> switchIID = InstanceIdentifier.create(Networks.class)
            .child(org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.networks.Network
                .class, new NetworkKey(new NetworkId("otn-topology")))
            .child(Node.class, new NodeKey(new NodeId("SPDR-SA1-XPDR2")));
        FluentFuture<Optional<Node>> switchFuture = dataBroker.newReadOnlyTransaction()
            .read(LogicalDatastoreType.CONFIGURATION, switchIID);

        otnMuxA = muxAFuture.get().get();
        otnMuxC = muxCFuture.get().get();
        otnSwitch = switchFuture.get().get();

        KeyedInstanceIdentifier<Node, NodeKey> tpdrIID = InstanceIdentifier.create(Networks.class)
            .child(org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.networks.Network
                .class, new NetworkKey(new NetworkId("otn-topology")))
            .child(Node.class, new NodeKey(new NodeId("XPDR-A1-XPDR1")));
        FluentFuture<Optional<Node>> tpdrFuture = dataBroker.newReadOnlyTransaction()
            .read(LogicalDatastoreType.CONFIGURATION, tpdrIID);
        tpdr100G = tpdrFuture.get().get();

        InstanceIdentifier<Network1> linksIID = InstanceIdentifier.create(Networks.class)
            .child(org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.networks.Network
                .class, new NetworkKey(new NetworkId("otn-topology")))
            .augmentation(Network1.class);
        FluentFuture<Optional<Network1>> linksFuture = dataBroker.newReadOnlyTransaction()
            .read(LogicalDatastoreType.CONFIGURATION, linksIID);
        otnLinks = linksFuture.get().get().getLink();

        topologyUuid = new Uuid(UUID.nameUUIDFromBytes(TapiStringConstants.T0_MULTILAYER.getBytes(
            Charset.forName("UTF-8"))).toString());
        networkTransactionService = new NetworkTransactionImpl(getDataBroker());
        tapiLink = new TapiLinkImpl(networkTransactionService);
        LOG.info("TEST SETUP READY");
    }

    @Test
    void convertNodeWhenNoStates() {
        Node tpdr = changeTerminationPointState(tpdr100G, "XPDR1-NETWORK1", null, null);
        List<String> networkPortList = new ArrayList<>();
        for (TerminationPoint tp : tpdr100G.augmentation(Node1.class).getTerminationPoint().values()) {
            if (tp.augmentation(TerminationPoint1.class).getTpType().equals(OpenroadmTpType.XPONDERNETWORK)) {
                networkPortList.add(tp.getTpId().getValue());
            }
        }
        ConvertORTopoToTapiTopo tapiFactory = new ConvertORTopoToTapiTopo(topologyUuid, tapiLink);
        tapiFactory.convertNode(tpdr, networkPortList);

        Uuid dsrNodeUuid = new Uuid(UUID.nameUUIDFromBytes("XPDR-A1-XPDR1+DSR".getBytes(Charset.forName("UTF-8")))
            .toString());
        org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.topology.Node dsrNode = tapiFactory
            .getTapiNodes().get(new
                org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.topology.NodeKey(dsrNodeUuid));
        Uuid networkNepUuid = new Uuid(
            UUID.nameUUIDFromBytes(("XPDR-A1-XPDR1+iODU+XPDR1-NETWORK1").getBytes(Charset.forName("UTF-8")))
                .toString());
        OwnedNodeEdgePoint nepN = dsrNode.nonnullOwnedNodeEdgePoint().get(new OwnedNodeEdgePointKey(networkNepUuid));
        assertNull(nepN.getAdministrativeState(), "Administrative State should not be present");
        assertNull(nepN.getOperationalState(), "Operational State should not be present");

        Uuid otsiNodeUuid = new Uuid(UUID.nameUUIDFromBytes("XPDR-A1-XPDR1+OTSi".getBytes(Charset.forName("UTF-8")))
            .toString());
        org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.topology.Node otsiNode = tapiFactory
            .getTapiNodes().get(new
                org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.topology.NodeKey(otsiNodeUuid));
        Uuid enepUuid = new Uuid(
            UUID.nameUUIDFromBytes(("XPDR-A1-XPDR1+eOTSi+XPDR1-NETWORK1").getBytes(Charset.forName("UTF-8")))
                .toString());
        Uuid inepUuid = new Uuid(
            UUID.nameUUIDFromBytes(("XPDR-A1-XPDR1+iOTSi+XPDR1-NETWORK1").getBytes(Charset.forName("UTF-8")))
                .toString());
        OwnedNodeEdgePoint enep = otsiNode.nonnullOwnedNodeEdgePoint().get(new OwnedNodeEdgePointKey(enepUuid));
        assertNull(enep.getAdministrativeState(), "Administrative State should not be present");
        assertNull(enep.getOperationalState(), "Operational State should not be present");

        OwnedNodeEdgePoint inep = otsiNode.nonnullOwnedNodeEdgePoint().get(new OwnedNodeEdgePointKey(inepUuid));
        assertNull(inep.getAdministrativeState(), "Administrative State should not be present");
        assertNull(inep.getOperationalState(), "Operational State should not be present");
    }

    @Test
    void convertNodeWhenBadStates1() {
        Node tpdr = changeTerminationPointState(tpdr100G, "XPDR1-NETWORK1", AdminStates.OutOfService,
            State.OutOfService);
        List<String> networkPortList = new ArrayList<>();
        for (TerminationPoint tp : tpdr100G.augmentation(Node1.class).getTerminationPoint().values()) {
            if (tp.augmentation(TerminationPoint1.class).getTpType().equals(OpenroadmTpType.XPONDERNETWORK)) {
                networkPortList.add(tp.getTpId().getValue());
            }
        }
        ConvertORTopoToTapiTopo tapiFactory = new ConvertORTopoToTapiTopo(topologyUuid, tapiLink);
        tapiFactory.convertNode(tpdr, networkPortList);

        Uuid dsrNodeUuid = new Uuid(UUID.nameUUIDFromBytes("XPDR-A1-XPDR1+DSR".getBytes(Charset.forName("UTF-8")))
            .toString());
        org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.topology.Node dsrNode = tapiFactory
            .getTapiNodes().get(new
                org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.topology.NodeKey(dsrNodeUuid));
        Uuid networkNepUuid = new Uuid(
            UUID.nameUUIDFromBytes(("XPDR-A1-XPDR1+iODU+XPDR1-NETWORK1").getBytes(Charset.forName("UTF-8")))
                .toString());
        OwnedNodeEdgePoint nepN = dsrNode.nonnullOwnedNodeEdgePoint().get(new OwnedNodeEdgePointKey(networkNepUuid));
        assertEquals(AdministrativeState.LOCKED, nepN.getAdministrativeState(),
            "Administrative State should be Locked");
        assertEquals(OperationalState.DISABLED, nepN.getOperationalState(), "Operational State should be Disabled");

        Uuid otsiNodeUuid = new Uuid(UUID.nameUUIDFromBytes("XPDR-A1-XPDR1+OTSi".getBytes(Charset.forName("UTF-8")))
            .toString());
        org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.topology.Node otsiNode = tapiFactory
            .getTapiNodes().get(new
                org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.topology.NodeKey(otsiNodeUuid));
        Uuid enepUuid = new Uuid(
            UUID.nameUUIDFromBytes(("XPDR-A1-XPDR1+eOTSi+XPDR1-NETWORK1").getBytes(Charset.forName("UTF-8")))
                .toString());
        Uuid inepUuid = new Uuid(
            UUID.nameUUIDFromBytes(("XPDR-A1-XPDR1+iOTSi+XPDR1-NETWORK1").getBytes(Charset.forName("UTF-8")))
                .toString());
        OwnedNodeEdgePoint enep = otsiNode.nonnullOwnedNodeEdgePoint().get(new OwnedNodeEdgePointKey(enepUuid));
        assertEquals(AdministrativeState.LOCKED, enep.getAdministrativeState(),
            "Administrative State should be Locked");
        assertEquals(OperationalState.DISABLED, enep.getOperationalState(), "Operational State should be Disabled");

        OwnedNodeEdgePoint inep = otsiNode.nonnullOwnedNodeEdgePoint().get(new OwnedNodeEdgePointKey(inepUuid));
        assertEquals(AdministrativeState.LOCKED, inep.getAdministrativeState(),
            "Administrative State should be Locked");
        assertEquals(OperationalState.DISABLED, inep.getOperationalState(), "Operational State should be Disabled");
    }

    @Test
    void convertNodeWhenBadStates2() {
        Node tpdr = changeTerminationPointState(tpdr100G, "XPDR1-NETWORK1", AdminStates.Maintenance,
            State.Degraded);
        List<String> networkPortList = new ArrayList<>();
        for (TerminationPoint tp : tpdr100G.augmentation(Node1.class).getTerminationPoint().values()) {
            if (tp.augmentation(TerminationPoint1.class).getTpType().equals(OpenroadmTpType.XPONDERNETWORK)) {
                networkPortList.add(tp.getTpId().getValue());
            }
        }
        ConvertORTopoToTapiTopo tapiFactory = new ConvertORTopoToTapiTopo(topologyUuid, tapiLink);
        tapiFactory.convertNode(tpdr, networkPortList);

        Uuid dsrNodeUuid = new Uuid(UUID.nameUUIDFromBytes("XPDR-A1-XPDR1+DSR".getBytes(Charset.forName("UTF-8")))
            .toString());
        org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.topology.Node dsrNode = tapiFactory
            .getTapiNodes().get(new
                org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.topology.NodeKey(dsrNodeUuid));
        Uuid networkNepUuid = new Uuid(
            UUID.nameUUIDFromBytes(("XPDR-A1-XPDR1+iODU+XPDR1-NETWORK1").getBytes(Charset.forName("UTF-8")))
                .toString());
        OwnedNodeEdgePoint nepN = dsrNode.nonnullOwnedNodeEdgePoint().get(new OwnedNodeEdgePointKey(networkNepUuid));
        assertEquals(AdministrativeState.LOCKED, nepN.getAdministrativeState(),
            "Administrative State should be Locked");
        assertEquals(OperationalState.DISABLED, nepN.getOperationalState(), "Operational State should be Disabled");

        Uuid otsiNodeUuid = new Uuid(UUID.nameUUIDFromBytes("XPDR-A1-XPDR1+OTSi".getBytes(Charset.forName("UTF-8")))
            .toString());
        org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.topology.Node otsiNode = tapiFactory
            .getTapiNodes().get(new
                org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.topology.NodeKey(otsiNodeUuid));
        Uuid enepUuid = new Uuid(
            UUID.nameUUIDFromBytes(("XPDR-A1-XPDR1+eOTSi+XPDR1-NETWORK1").getBytes(Charset.forName("UTF-8")))
                .toString());
        Uuid inepUuid = new Uuid(
            UUID.nameUUIDFromBytes(("XPDR-A1-XPDR1+iOTSi+XPDR1-NETWORK1").getBytes(Charset.forName("UTF-8")))
                .toString());
        OwnedNodeEdgePoint enep = otsiNode.nonnullOwnedNodeEdgePoint().get(new OwnedNodeEdgePointKey(enepUuid));
        assertEquals(AdministrativeState.LOCKED, enep.getAdministrativeState(),
            "Administrative State should be Locked");
        assertEquals(OperationalState.DISABLED, enep.getOperationalState(), "Operational State should be Disabled");

        OwnedNodeEdgePoint inep = otsiNode.nonnullOwnedNodeEdgePoint().get(new OwnedNodeEdgePointKey(inepUuid));
        assertEquals(AdministrativeState.LOCKED, inep.getAdministrativeState(),
            "Administrative State should be Locked");
        assertEquals(OperationalState.DISABLED, inep.getOperationalState(), "Operational State should be Disabled");
    }

    @Test
    void convertOtnLinkWhenNoState() {
        HashMap<LinkKey, org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226
            .networks.network.Link> otnLinksAlt = new HashMap<>(otnLinks);
        org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.networks.network.Link
            link = changeOtnLinkState(otnLinks.get(new LinkKey(
                new LinkId("ODTU4-SPDR-SA1-XPDR1-XPDR1-NETWORK1toSPDR-SC1-XPDR1-XPDR1-NETWORK1"))), null, null);
        otnLinksAlt.replace(link.key(), link);

        ConvertORTopoToTapiTopo tapiFactory = new ConvertORTopoToTapiTopo(topologyUuid, tapiLink);
        List<String> networkPortListA = new ArrayList<>();
        for (TerminationPoint tp : otnMuxA.augmentation(Node1.class).getTerminationPoint().values()) {
            if (tp.augmentation(TerminationPoint1.class).getTpType().equals(OpenroadmTpType.XPONDERNETWORK)) {
                networkPortListA.add(tp.getTpId().getValue());
            }
        }
        tapiFactory.convertNode(otnMuxA, networkPortListA);
        List<String> networkPortListC = new ArrayList<>();
        for (TerminationPoint tp : otnMuxC.augmentation(Node1.class).getTerminationPoint().values()) {
            if (tp.augmentation(TerminationPoint1.class).getTpType().equals(OpenroadmTpType.XPONDERNETWORK)) {
                networkPortListC.add(tp.getTpId().getValue());
            }
        }
        tapiFactory.convertNode(otnMuxC, networkPortListC);
        tapiFactory.convertLinks(otnLinksAlt);

        List<Link> tapiLinks = tapiFactory.getTapiLinks().values().stream()
            .sorted((l1, l2) -> l1.getUuid().getValue().compareTo(l2.getUuid().getValue()))
            .collect(Collectors.toList());
        assertNull(tapiLinks.get(3).getAdministrativeState(), "Administrative State should not be present");
        assertEquals(AdministrativeState.UNLOCKED, tapiLinks.get(0).getAdministrativeState(),
            "Administrative state should be UNLOCKED");
        assertNull(tapiLinks.get(3).getOperationalState(), "Operational State should not be present");
        assertEquals(OperationalState.ENABLED, tapiLinks.get(0).getOperationalState(),
            "Operational state should be ENABLED");
    }

    @Test
    void convertOtnLinkWhenNoStateOnOppositeLink() {
        HashMap<LinkKey, org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226
            .networks.network.Link> otnLinksAlt = new HashMap<>(otnLinks);
        org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.networks.network.Link
            link = changeOtnLinkState(otnLinks.get(new LinkKey(
                new LinkId("ODTU4-SPDR-SC1-XPDR1-XPDR1-NETWORK1toSPDR-SA1-XPDR1-XPDR1-NETWORK1"))), null, null);
        otnLinksAlt.replace(link.key(), link);

        ConvertORTopoToTapiTopo tapiFactory = new ConvertORTopoToTapiTopo(topologyUuid, tapiLink);
        List<String> networkPortListA = new ArrayList<>();
        for (TerminationPoint tp : otnMuxA.augmentation(Node1.class).getTerminationPoint().values()) {
            if (tp.augmentation(TerminationPoint1.class).getTpType().equals(OpenroadmTpType.XPONDERNETWORK)) {
                networkPortListA.add(tp.getTpId().getValue());
            }
        }
        tapiFactory.convertNode(otnMuxA, networkPortListA);
        List<String> networkPortListC = new ArrayList<>();
        for (TerminationPoint tp : otnMuxC.augmentation(Node1.class).getTerminationPoint().values()) {
            if (tp.augmentation(TerminationPoint1.class).getTpType().equals(OpenroadmTpType.XPONDERNETWORK)) {
                networkPortListC.add(tp.getTpId().getValue());
            }
        }
        tapiFactory.convertNode(otnMuxC, networkPortListC);
        tapiFactory.convertLinks(otnLinksAlt);

        List<Link> tapiLinks = tapiFactory.getTapiLinks().values().stream()
            .sorted((l1, l2) -> l1.getUuid().getValue().compareTo(l2.getUuid().getValue()))
            .collect(Collectors.toList());
        assertNull(tapiLinks.get(3).getAdministrativeState(), "Administrative State should not be present");
        assertEquals(AdministrativeState.UNLOCKED, tapiLinks.get(0).getAdministrativeState(),
            "Administrative state should be UNLOCKED");
        assertNull(tapiLinks.get(3).getOperationalState(), "Operational State should not be present");
        assertEquals(OperationalState.ENABLED, tapiLinks.get(0).getOperationalState(),
            "Operational state should be ENABLED");
    }

    @Test
    void convertOtnLinkWhenBadState1() {
        HashMap<LinkKey, org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226
            .networks.network.Link> otnLinksAlt = new HashMap<>(otnLinks);
        org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.networks.network.Link
            link = changeOtnLinkState(otnLinks.get(new LinkKey(
                new LinkId("ODTU4-SPDR-SA1-XPDR1-XPDR1-NETWORK1toSPDR-SC1-XPDR1-XPDR1-NETWORK1"))),
                AdminStates.OutOfService, State.OutOfService);
        otnLinksAlt.replace(link.key(), link);

        ConvertORTopoToTapiTopo tapiFactory = new ConvertORTopoToTapiTopo(topologyUuid, tapiLink);
        List<String> networkPortListA = new ArrayList<>();
        for (TerminationPoint tp : otnMuxA.augmentation(Node1.class).getTerminationPoint().values()) {
            if (tp.augmentation(TerminationPoint1.class).getTpType().equals(OpenroadmTpType.XPONDERNETWORK)) {
                networkPortListA.add(tp.getTpId().getValue());
            }
        }
        tapiFactory.convertNode(otnMuxA, networkPortListA);
        List<String> networkPortListC = new ArrayList<>();
        for (TerminationPoint tp : otnMuxC.augmentation(Node1.class).getTerminationPoint().values()) {
            if (tp.augmentation(TerminationPoint1.class).getTpType().equals(OpenroadmTpType.XPONDERNETWORK)) {
                networkPortListC.add(tp.getTpId().getValue());
            }
        }
        tapiFactory.convertNode(otnMuxC, networkPortListC);
        tapiFactory.convertLinks(otnLinksAlt);

        List<Link> tapiLinks = tapiFactory.getTapiLinks().values().stream()
            .sorted((l1, l2) -> l1.getUuid().getValue().compareTo(l2.getUuid().getValue()))
            .collect(Collectors.toList());
        assertEquals(AdministrativeState.LOCKED, tapiLinks.get(3).getAdministrativeState(),
            "Administrative state should be LOCKED");
        assertEquals(AdministrativeState.UNLOCKED, tapiLinks.get(0).getAdministrativeState(),
            "Administrative state should be UNLOCKED");
        assertEquals(OperationalState.DISABLED, tapiLinks.get(3).getOperationalState(),
            "Operational state should be DISABLED");
        assertEquals(OperationalState.ENABLED, tapiLinks.get(0).getOperationalState(),
            "Operational state should be ENABLED");
    }

    @Test
    void convertOtnLinkWhenBadState2() {
        HashMap<LinkKey, org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226
            .networks.network.Link> otnLinksAlt = new HashMap<>(otnLinks);
        org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.networks.network.Link
            link = changeOtnLinkState(otnLinks.get(new LinkKey(
                new LinkId("ODTU4-SPDR-SA1-XPDR1-XPDR1-NETWORK1toSPDR-SC1-XPDR1-XPDR1-NETWORK1"))),
                AdminStates.Maintenance, State.Degraded);
        otnLinksAlt.replace(link.key(), link);

        ConvertORTopoToTapiTopo tapiFactory = new ConvertORTopoToTapiTopo(topologyUuid, tapiLink);
        List<String> networkPortListA = new ArrayList<>();
        for (TerminationPoint tp : otnMuxA.augmentation(Node1.class).getTerminationPoint().values()) {
            if (tp.augmentation(TerminationPoint1.class).getTpType().equals(OpenroadmTpType.XPONDERNETWORK)) {
                networkPortListA.add(tp.getTpId().getValue());
            }
        }
        tapiFactory.convertNode(otnMuxA, networkPortListA);
        List<String> networkPortListC = new ArrayList<>();
        for (TerminationPoint tp : otnMuxC.augmentation(Node1.class).getTerminationPoint().values()) {
            if (tp.augmentation(TerminationPoint1.class).getTpType().equals(OpenroadmTpType.XPONDERNETWORK)) {
                networkPortListC.add(tp.getTpId().getValue());
            }
        }
        tapiFactory.convertNode(otnMuxC, networkPortListC);
        tapiFactory.convertLinks(otnLinksAlt);

        List<Link> tapiLinks = tapiFactory.getTapiLinks().values().stream()
            .sorted((l1, l2) -> l1.getUuid().getValue().compareTo(l2.getUuid().getValue()))
            .collect(Collectors.toList());
        assertEquals(AdministrativeState.LOCKED, tapiLinks.get(3).getAdministrativeState(),
            "Administrative state should be LOCKED");
        assertEquals(AdministrativeState.UNLOCKED, tapiLinks.get(0).getAdministrativeState(),
            "Administrative state should be UNLOCKED");
        assertEquals(OperationalState.DISABLED, tapiLinks.get(3).getOperationalState(),
            "Operational state should be DISABLED");
        assertEquals(OperationalState.ENABLED, tapiLinks.get(0).getOperationalState(),
            "Operational state should be ENABLED");
    }

    @Test
    void convertOtnLinkWhenBadStateOnOppositeLink() {
        HashMap<LinkKey, org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226
            .networks.network.Link> otnLinksAlt = new HashMap<>(otnLinks);
        org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.networks.network.Link
            link = changeOtnLinkState(otnLinks.get(new LinkKey(
                new LinkId("ODTU4-SPDR-SC1-XPDR1-XPDR1-NETWORK1toSPDR-SA1-XPDR1-XPDR1-NETWORK1"))),
                AdminStates.OutOfService, State.OutOfService);
        otnLinksAlt.replace(link.key(), link);

        ConvertORTopoToTapiTopo tapiFactory = new ConvertORTopoToTapiTopo(topologyUuid, tapiLink);
        List<String> networkPortListA = new ArrayList<>();
        for (TerminationPoint tp : otnMuxA.augmentation(Node1.class).getTerminationPoint().values()) {
            if (tp.augmentation(TerminationPoint1.class).getTpType().equals(OpenroadmTpType.XPONDERNETWORK)) {
                networkPortListA.add(tp.getTpId().getValue());
            }
        }
        tapiFactory.convertNode(otnMuxA, networkPortListA);
        List<String> networkPortListC = new ArrayList<>();
        for (TerminationPoint tp : otnMuxC.augmentation(Node1.class).getTerminationPoint().values()) {
            if (tp.augmentation(TerminationPoint1.class).getTpType().equals(OpenroadmTpType.XPONDERNETWORK)) {
                networkPortListC.add(tp.getTpId().getValue());
            }
        }
        tapiFactory.convertNode(otnMuxC, networkPortListC);
        tapiFactory.convertLinks(otnLinksAlt);

        List<Link> tapiLinks = tapiFactory.getTapiLinks().values().stream()
            .sorted((l1, l2) -> l1.getUuid().getValue().compareTo(l2.getUuid().getValue()))
            .collect(Collectors.toList());
        assertEquals(AdministrativeState.LOCKED, tapiLinks.get(3).getAdministrativeState(),
            "Administrative state should be LOCKED");
        assertEquals(AdministrativeState.UNLOCKED, tapiLinks.get(0).getAdministrativeState(),
            "Administrative state should be UNLOCKED");
        assertEquals(OperationalState.DISABLED, tapiLinks.get(3).getOperationalState(),
            "Operational state should be DISABLED");
        assertEquals(OperationalState.ENABLED, tapiLinks.get(0).getOperationalState(),
            "Operational state should be ENABLED");
    }

    @Test
    void convertNodeForTransponder100G() {
        ConvertORTopoToTapiTopo tapiFactory = new ConvertORTopoToTapiTopo(topologyUuid, tapiLink);
        List<String> networkPortList = new ArrayList<>();
        for (TerminationPoint tp : tpdr100G.augmentation(Node1.class).getTerminationPoint().values()) {
            if (tp.augmentation(TerminationPoint1.class).getTpType().equals(OpenroadmTpType.XPONDERNETWORK)) {
                networkPortList.add(tp.getTpId().getValue());
            }
        }
        tapiFactory.convertNode(tpdr100G, networkPortList);
        List<org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.topology.Node> tapiNodes
            = tapiFactory.getTapiNodes().values().stream()
            .sorted((n1, n2) -> n1.getUuid().getValue().compareTo(n2.getUuid().getValue()))
            .collect(Collectors.toList());

        assertEquals(2, tapiFactory.getTapiNodes().size(), "Node list size should be 2");
        assertEquals(2, tapiFactory.getTapiLinks().size(), "Link list size should be 2");

        Uuid dsrNodeUuid = new Uuid(UUID.nameUUIDFromBytes("XPDR-A1-XPDR1+DSR".getBytes(Charset.forName("UTF-8")))
            .toString());
        checkDsrNode(tapiNodes.get(1), dsrNodeUuid, "tpdr", "XPDR-A1-XPDR1+DSR");
        Uuid otsiNodeUuid = new Uuid(UUID.nameUUIDFromBytes("XPDR-A1-XPDR1+OTSi".getBytes(Charset.forName("UTF-8")))
            .toString());
        checkOtsiNode(tapiNodes.get(0), otsiNodeUuid, "tpdr", "XPDR-A1-XPDR1+OTSi");

        List<Link> tapiLinks = tapiFactory.getTapiLinks().values().stream()
            .sorted((l1, l2) -> l1.getUuid().getValue().compareTo(l2.getUuid().getValue()))
            .collect(Collectors.toList());
        checkTransitionalLink(tapiLinks.get(1), dsrNodeUuid, otsiNodeUuid,
            "XPDR-A1-XPDR1+iODU+XPDR1-NETWORK1", "XPDR-A1-XPDR1+iOTSi+XPDR1-NETWORK1", "XPDR-A1-XPDR1");
    }

    @Test
    void convertNodeForOtnMuxponder() {
        ConvertORTopoToTapiTopo tapiFactory = new ConvertORTopoToTapiTopo(topologyUuid, tapiLink);
        List<String> networkPortList = new ArrayList<>();
        for (TerminationPoint tp : otnMuxA.augmentation(Node1.class).getTerminationPoint().values()) {
            if (tp.augmentation(TerminationPoint1.class).getTpType().equals(OpenroadmTpType.XPONDERNETWORK)) {
                networkPortList.add(tp.getTpId().getValue());
            }
        }
        tapiFactory.convertNode(otnMuxA, networkPortList);
        List<org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.topology.Node> tapiNodes
            = tapiFactory.getTapiNodes().values().stream()
            .sorted((n1, n2) -> n1.getUuid().getValue().compareTo(n2.getUuid().getValue()))
            .collect(Collectors.toList());

        assertEquals(2, tapiFactory.getTapiNodes().size(), "Node list size should be 2");
        assertEquals(1, tapiFactory.getTapiLinks().size(), "Link list size should be 1");
        Uuid dsrNodeUuid = new Uuid(UUID.nameUUIDFromBytes("SPDR-SA1-XPDR1+DSR".getBytes(Charset.forName("UTF-8")))
            .toString());
        checkDsrNode(tapiNodes.get(0), dsrNodeUuid, "mux", "SPDR-SA1-XPDR1+DSR");
        Uuid otsiNodeUuid = new Uuid(UUID.nameUUIDFromBytes("SPDR-SA1-XPDR1+OTSi".getBytes(Charset.forName("UTF-8")))
            .toString());
        checkOtsiNode(tapiNodes.get(1), otsiNodeUuid, "mux", "SPDR-SA1-XPDR1+OTSi");

        List<Link> tapiLinks = tapiFactory.getTapiLinks().values().stream()
            .sorted((l1, l2) -> l1.getUuid().getValue().compareTo(l2.getUuid().getValue()))
            .collect(Collectors.toList());
        checkTransitionalLink(tapiLinks.get(0), dsrNodeUuid, otsiNodeUuid,
            "SPDR-SA1-XPDR1+iODU+XPDR1-NETWORK1", "SPDR-SA1-XPDR1+iOTSi+XPDR1-NETWORK1", "SPDR-SA1-XPDR1");
    }

    @Test
    void convertNodeForOtnSwitch() {
        ConvertORTopoToTapiTopo tapiFactory = new ConvertORTopoToTapiTopo(topologyUuid, tapiLink);
        List<String> networkPortList = new ArrayList<>();
        for (TerminationPoint tp : otnSwitch.augmentation(Node1.class).getTerminationPoint().values()) {
            if (tp.augmentation(TerminationPoint1.class).getTpType().equals(OpenroadmTpType.XPONDERNETWORK)) {
                networkPortList.add(tp.getTpId().getValue());
            }
        }
        tapiFactory.convertNode(otnSwitch, networkPortList);
        List<org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.topology.Node> tapiNodes
            = tapiFactory.getTapiNodes().values().stream()
            .sorted((n1, n2) -> n1.getUuid().getValue().compareTo(n2.getUuid().getValue()))
            .collect(Collectors.toList());

        assertEquals(2, tapiFactory.getTapiNodes().size(), "Node list size should be 2");
        assertEquals(4, tapiFactory.getTapiLinks().size(), "Link list size should be 4");

        Uuid dsrNodeUuid = new Uuid(UUID.nameUUIDFromBytes("SPDR-SA1-XPDR2+DSR".getBytes(Charset.forName("UTF-8")))
            .toString());
        checkDsrNode(tapiNodes.get(0), dsrNodeUuid, "switch", "SPDR-SA1-XPDR2+DSR");
        Uuid otsiNodeUuid = new Uuid(UUID.nameUUIDFromBytes("SPDR-SA1-XPDR2+OTSi".getBytes(Charset.forName("UTF-8")))
            .toString());
        checkOtsiNode(tapiNodes.get(1), otsiNodeUuid, "switch", "SPDR-SA1-XPDR2+OTSi");

        List<Link> tapiLinks = tapiFactory.getTapiLinks().values().stream()
            .sorted((l1, l2) -> l1.getUuid().getValue().compareTo(l2.getUuid().getValue()))
            .collect(Collectors.toList());
        checkTransitionalLink(tapiLinks.get(2), dsrNodeUuid, otsiNodeUuid,
            "SPDR-SA1-XPDR2+iODU+XPDR2-NETWORK4", "SPDR-SA1-XPDR2+iOTSi+XPDR2-NETWORK4", "SPDR-SA1-XPDR2");
    }

    @Test
    void convertOtnLink() {
        ConvertORTopoToTapiTopo tapiFactory = new ConvertORTopoToTapiTopo(topologyUuid, tapiLink);
        List<String> networkPortListA = new ArrayList<>();
        for (TerminationPoint tp : otnMuxA.augmentation(Node1.class).getTerminationPoint().values()) {
            if (tp.augmentation(TerminationPoint1.class).getTpType().equals(OpenroadmTpType.XPONDERNETWORK)) {
                networkPortListA.add(tp.getTpId().getValue());
            }
        }
        tapiFactory.convertNode(otnMuxA, networkPortListA);
        List<String> networkPortListC = new ArrayList<>();
        for (TerminationPoint tp : otnMuxC.augmentation(Node1.class).getTerminationPoint().values()) {
            if (tp.augmentation(TerminationPoint1.class).getTpType().equals(OpenroadmTpType.XPONDERNETWORK)) {
                networkPortListC.add(tp.getTpId().getValue());
            }
        }
        tapiFactory.convertNode(otnMuxC, networkPortListC);
        tapiFactory.convertLinks(otnLinks);
        assertEquals(4, tapiFactory.getTapiLinks().size(), "Link list size should be 4");

        Uuid node1Uuid = new Uuid(UUID.nameUUIDFromBytes("SPDR-SA1-XPDR1+DSR".getBytes(Charset.forName("UTF-8")))
            .toString());
        Uuid node2Uuid = new Uuid(UUID.nameUUIDFromBytes("SPDR-SC1-XPDR1+DSR".getBytes(Charset.forName("UTF-8")))
            .toString());
        Uuid node3Uuid = new Uuid(UUID.nameUUIDFromBytes("SPDR-SA1-XPDR1+OTSi".getBytes(Charset.forName("UTF-8")))
            .toString());
        Uuid node4Uuid = new Uuid(UUID.nameUUIDFromBytes("SPDR-SC1-XPDR1+OTSi".getBytes(Charset.forName("UTF-8")))
            .toString());
        Uuid tp1Uuid = new Uuid(UUID.nameUUIDFromBytes("SPDR-SA1-XPDR1+eODU+XPDR1-NETWORK1"
            .getBytes(Charset.forName("UTF-8"))).toString());
        Uuid tp2Uuid = new Uuid(UUID.nameUUIDFromBytes("SPDR-SC1-XPDR1+eODU+XPDR1-NETWORK1"
            .getBytes(Charset.forName("UTF-8"))).toString());
        Uuid tp3Uuid = new Uuid(UUID.nameUUIDFromBytes("SPDR-SA1-XPDR1+iOTSi+XPDR1-NETWORK1"
            .getBytes(Charset.forName("UTF-8"))).toString());
        Uuid tp4Uuid = new Uuid(UUID.nameUUIDFromBytes("SPDR-SC1-XPDR1+iOTSi+XPDR1-NETWORK1"
            .getBytes(Charset.forName("UTF-8"))).toString());
        Uuid link1Uuid =
            new Uuid(UUID.nameUUIDFromBytes("SPDR-SA1-XPDR1+eODU+XPDR1-NETWORK1toSPDR-SC1-XPDR1+eODU+XPDR1-NETWORK1"
                .getBytes(Charset.forName("UTF-8"))).toString());
        Uuid link2Uuid =
            new Uuid(UUID.nameUUIDFromBytes("SPDR-SA1-XPDR1+iOTSi+XPDR1-NETWORK1toSPDR-SC1-XPDR1+iOTSi+XPDR1-NETWORK1"
                .getBytes(Charset.forName("UTF-8"))).toString());

        List<Link> links = tapiFactory.getTapiLinks().values().stream()
            .sorted((l1, l2) -> l1.getUuid().getValue().compareTo(l2.getUuid().getValue()))
            .collect(Collectors.toList());
        checkOtnLink(links.get(3), node1Uuid, node2Uuid, tp1Uuid, tp2Uuid, link1Uuid,
            "SPDR-SA1-XPDR1+eODU+XPDR1-NETWORK1toSPDR-SC1-XPDR1+eODU+XPDR1-NETWORK1");
        checkOtnLink(links.get(2), node3Uuid, node4Uuid, tp3Uuid, tp4Uuid, link2Uuid,
            "SPDR-SA1-XPDR1+iOTSi+XPDR1-NETWORK1toSPDR-SC1-XPDR1+iOTSi+XPDR1-NETWORK1");
    }

    @Test
    void convertRoadmInfrastructureWhenNoXponderAttached() {
        ConvertORTopoToTapiTopo tapiFactory = new ConvertORTopoToTapiTopo(topologyUuid, tapiLink);
        tapiFactory.convertRoadmInfrastructure();

        assertEquals(1, tapiFactory.getTapiNodes().size(), "Node list size should be 1");
        assertEquals(0, tapiFactory.getTapiLinks().size(), "Link list size should be empty");
        List<org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.topology.Node> tapiNodes
            = tapiFactory.getTapiNodes().values().stream().collect(Collectors.toList());
        Uuid otsiNodeUuid = new Uuid(UUID.nameUUIDFromBytes("ROADM-infra".getBytes(Charset.forName("UTF-8")))
            .toString());
        checkOtsiNode(tapiNodes.get(0), otsiNodeUuid, "infra", "ROADM-infra");
    }

    @Test
    void convertRoadmInfrastructureWhenOtnMuxAttached() {
        ConvertORTopoToTapiTopo tapiFactory = new ConvertORTopoToTapiTopo(topologyUuid, tapiLink);
        List<String> networkPortListA = new ArrayList<>();
        for (TerminationPoint tp : otnMuxA.augmentation(Node1.class).getTerminationPoint().values()) {
            if (tp.augmentation(TerminationPoint1.class).getTpType().equals(OpenroadmTpType.XPONDERNETWORK)) {
                networkPortListA.add(tp.getTpId().getValue());
            }
        }
        tapiFactory.convertNode(otnMuxA, networkPortListA);
        tapiFactory.convertRoadmInfrastructure();

        assertEquals(3, tapiFactory.getTapiNodes().size(), "Node list size should be 3");
        assertEquals(2, tapiFactory.getTapiLinks().size(), "Link list size should be 2");
        List<org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.topology.Node> tapiNodes
            = tapiFactory.getTapiNodes().values().stream()
            .sorted((n1, n2) -> n1.getUuid().getValue().compareTo(n2.getUuid().getValue()))
            .collect(Collectors.toList());
        Uuid otsiNodeUuid = new Uuid(UUID.nameUUIDFromBytes("ROADM-infra".getBytes(Charset.forName("UTF-8")))
            .toString());
        checkOtsiNode(tapiNodes.get(2), otsiNodeUuid, "infra", "ROADM-infra");

        List<Link> links = tapiFactory.getTapiLinks().values().stream()
            .sorted((l1, l2) -> l1.getUuid().getValue().compareTo(l2.getUuid().getValue()))
            .collect(Collectors.toList());
        Uuid node1Uuid = new Uuid(UUID.nameUUIDFromBytes("SPDR-SA1-XPDR1+OTSi".getBytes(Charset.forName("UTF-8")))
            .toString());
        Uuid node2Uuid = new Uuid(UUID.nameUUIDFromBytes("ROADM-infra".getBytes(Charset.forName("UTF-8")))
            .toString());
        Uuid tp1Uuid = new Uuid(UUID.nameUUIDFromBytes("SPDR-SA1-XPDR1+eOTSi+XPDR1-NETWORK1"
            .getBytes(Charset.forName("UTF-8"))).toString());
        Uuid tp2Uuid = new Uuid(UUID.nameUUIDFromBytes(("roadm node+nep+1")
            .getBytes(Charset.forName("UTF-8"))).toString());
        Uuid linkUuid =
            new Uuid(UUID.nameUUIDFromBytes(
                "SPDR-SA1-XPDR1+OTSi--SPDR-SA1-XPDR1+eOTSi+XPDR1-NETWORK1 and ROADM-infra--NodeEdgePoint_1"
                    .getBytes(Charset.forName("UTF-8"))).toString());
        checkOmsLink(links.get(1), node1Uuid, node2Uuid, tp1Uuid, tp2Uuid, linkUuid,
            "SPDR-SA1-XPDR1+OTSi--SPDR-SA1-XPDR1+eOTSi+XPDR1-NETWORK1 and ROADM-infra--NodeEdgePoint_1");
    }

    private void checkDsrNode(org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.topology.Node node,
                              Uuid nodeUuid, String dsrNodeType, String nodeId) {
        assertEquals(nodeUuid, node.getUuid(), "incorrect node uuid");
        assertEquals(nodeId, node.getName().get(new NameKey("dsr/odu node name")).getValue(), "incorrect node name");
        assertEquals(AdministrativeState.UNLOCKED, node.getAdministrativeState(),
            "administrative state should be UNLOCKED");
        assertEquals(LifecycleState.INSTALLED, node.getLifecycleState(), "life-cycle state should be INSTALLED");
        assertEquals(OperationalState.ENABLED, node.getOperationalState(), "operational state should be ENABLED");
        assertThat("one value-name should be 'dsr/odu node name'",
            new ArrayList<>(node.nonnullName().keySet()), hasItem(new NameKey("dsr/odu node name")));
        assertEquals(2, node.getLayerProtocolName().size(),
            "dsr node should manage 2 protocol layers : dsr and odu");
        assertThat("dsr node should manage 2 protocol layers : dsr and odu",
            node.getLayerProtocolName(), hasItems(LayerProtocolName.DSR, LayerProtocolName.ODU));
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
        org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.topology.Node node,
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
        assertEquals(LayerProtocolName.PHOTONICMEDIA, node.getLayerProtocolName().stream().findFirst().get(),
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
                    UUID.nameUUIDFromBytes((nodeId.split("\\+")[0] + "+eOTSi+XPDR2-NETWORK2")
                        .getBytes(Charset.forName("UTF-8"))).toString());
                checkNepOtsiNode(nep2, enepUuid, "XPDR2-NETWORK2", "eNodeEdgePoint",
                    otnSwitch.getNodeId().getValue(), TapiStringConstants.E_OTSI);
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
                    UUID.nameUUIDFromBytes((nodeId.split("\\+")[0] + "+eOTSi+XPDR1-NETWORK1")
                        .getBytes(Charset.forName("UTF-8"))).toString());
                checkNepOtsiNode(nep3, enepUuid2, "XPDR1-NETWORK1", "eNodeEdgePoint",
                    otnMuxA.getNodeId().getValue(), TapiStringConstants.E_OTSI);
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
                    UUID.nameUUIDFromBytes((nodeId.split("\\+")[0] + "+eOTSi+XPDR1-NETWORK1")
                        .getBytes(Charset.forName("UTF-8"))).toString());
                checkNepOtsiNode(nep5, enepUuid3, "XPDR1-NETWORK1", "eNodeEdgePoint",
                    tpdr100G.getNodeId().getValue(), TapiStringConstants.E_OTSI);
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

    private void checkNepClient10G(OwnedNodeEdgePoint nep, Uuid nepUuid, String portName, String nepName,
                                   String nodeId, String extension) {
        assertEquals(nepUuid, nep.getUuid(), "bad uuid for " + portName);
        List<Name> nameList = new ArrayList<>(nep.nonnullName().values());
        Name name = nameList.get(0);
        assertEquals(String.join("+", nodeId, extension, portName), name.getValue(),
            "value of client nep should be '" + portName + "'");
        assertEquals(nepName, name.getValueName(),
            "value-name of client nep for '" + portName + "' should be '" + nepName + "'");
        assertEquals(3, nep.getSupportedCepLayerProtocolQualifier().size(),
            "Client nep should support 3 kind of cep");
        assertThat("client nep should support 3 kind of cep",
            nep.getSupportedCepLayerProtocolQualifier(),
            hasItems(ODUTYPEODU2.VALUE, ODUTYPEODU2E.VALUE, DIGITALSIGNALTYPE10GigELAN.VALUE));
        assertEquals(LayerProtocolName.ETH, nep.getLayerProtocolName(), "client nep should be of ETH protocol type");
        checkCommonPartOfNep(nep, false);
        checkSIP(nep, portName, nodeId, extension);
    }

    private void checkNepNetworkODU4(OwnedNodeEdgePoint nep, Uuid nepUuid, String portName, String nepName,
                                     String nodeId, String extension) {
        assertEquals(nepUuid, nep.getUuid(), "bad uuid for " + portName);
        List<Name> nameList = new ArrayList<>(nep.nonnullName().values());
        Name name = nameList.get(0);
        assertEquals(String.join("+", nodeId, extension, portName), name.getValue(),
            "value of network nep should be '" + portName + "'");
        assertEquals(nepName, name.getValueName(),
            "value-name of network nep for '" + portName + "' should be '" + nepName + "'");
        assertEquals(1, nep.getSupportedCepLayerProtocolQualifier().size(),
            "Network nep should support 1 kind of cep");
        assertThat("network nep should support 1 kind of cep",
            nep.getSupportedCepLayerProtocolQualifier(),
            hasItem(ODUTYPEODU4.VALUE));
        assertEquals(LayerProtocolName.ODU, nep.getLayerProtocolName(), "network nep should be of ODU protocol type");
        checkCommonPartOfNep(nep, false);
        checkSIP(nep, portName, nodeId, extension);
    }

    private void checkNodeRuleGroupForTpdrDSR(List<NodeRuleGroup> nrgList, Uuid clientNepUuid, Uuid networkNepUuid,
                                              Uuid nodeUuid) {
        assertEquals(2, nrgList.size(), "transponder DSR should contain 2 node rule group");
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
        assertEquals(ForwardingRule.MAYFORWARDACROSSGROUP, rule.get(0).getForwardingRule(),
            "the forwarding rule should be 'MAYFORWARDACROSSGROUP'");
        assertEquals(RuleType.FORWARDING, rule.get(0).getRuleType(), "the rule type should be 'FORWARDING'");
    }

    private void checkNodeRuleGroupForMuxDSR(List<NodeRuleGroup> nrgList, Uuid clientNepUuid, Uuid networkNepUuid,
                                             Uuid nodeUuid) {
        assertEquals(4, nrgList.size(), "muxponder DSR should contain 4 node rule group");
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
        assertEquals(ForwardingRule.MAYFORWARDACROSSGROUP, rule.get(0).getForwardingRule(),
            "the forwarding rule should be 'MAYFORWARDACROSSGROUP'");
        assertEquals(RuleType.FORWARDING, rule.get(0).getRuleType(), "the rule type should be 'FORWARDING'");
    }

    private void checkNodeRuleGroupForSwitchDSR(List<NodeRuleGroup> nrgList, Uuid clientNepUuid, Uuid networkNepUuid,
                                                Uuid nodeUuid) {
        assertEquals(1, nrgList.size(), "Switch-DSR should contain a single node rule group");
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
        assertEquals(ForwardingRule.MAYFORWARDACROSSGROUP, ruleList.get(0).getForwardingRule(),
            "the forwarding rule should be 'MAYFORWARDACROSSGROUP'");
        assertEquals(RuleType.FORWARDING, ruleList.get(0).getRuleType(), "the rule type should be 'FORWARDING'");
    }

    private void checkNodeRuleGroupForRdmInfra(List<NodeRuleGroup> nrgList, int nbNeps) {
        assertEquals(1, nrgList.size(), "RDM infra node - OTSi should contain a single node rule groups");
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
        assertEquals(ForwardingRule.MAYFORWARDACROSSGROUP, ruleList.get(0).getForwardingRule(),
            "the forwarding rule should be 'MAYFORWARDACROSSGROUP'");
        assertEquals(RuleType.FORWARDING, ruleList.get(0).getRuleType(), "the rule type should be 'FORWARDING'");
    }

    private void checkNodeRuleGroupForTpdrOTSi(List<NodeRuleGroup> nrgList, Uuid enepUuid, Uuid inepUuid,
                                               Uuid nodeUuid) {
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
        assertEquals(ForwardingRule.MAYFORWARDACROSSGROUP, ruleList.get(0).getForwardingRule(),
            "the forwarding rule should be 'MAYFORWARDACROSSGROUP'");
        assertEquals(RuleType.FORWARDING, ruleList.get(0).getRuleType(), "the rule type should be 'FORWARDING'");
    }

    private void checkNodeRuleGroupForMuxOTSi(List<NodeRuleGroup> nrgList, Uuid enepUuid, Uuid inepUuid,
                                              Uuid nodeUuid) {
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
        assertEquals(ForwardingRule.MAYFORWARDACROSSGROUP, ruleList.get(0).getForwardingRule(),
            "the forwarding rule should be 'MAYFORWARDACROSSGROUP'");
        assertEquals(RuleType.FORWARDING, ruleList.get(0).getRuleType(), "the rule type should be 'FORWARDING'");
    }

    private void checkNodeRuleGroupForSwitchOTSi(List<NodeRuleGroup> nrgList, Uuid enepUuid, Uuid inepUuid,
                                                 Uuid nodeUuid) {
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
        assertEquals(ForwardingRule.MAYFORWARDACROSSGROUP, ruleList0.get(0).getForwardingRule(),
            "the forwarding rule should be 'MAYFORWARDACROSSGROUP'");
        assertEquals(RuleType.FORWARDING, ruleList0.get(0).getRuleType(), "the rule type should be 'FORWARDING'");
    }

    private void checkNepClient100GSwitch(OwnedNodeEdgePoint nep, Uuid nepUuid, String portName, String nepName,
                                          String nodeId, String extension) {
        assertEquals(nepUuid, nep.getUuid(), "bad uuid for " + portName);
        List<Name> nameList = new ArrayList<>(nep.nonnullName().values());
        assertEquals(String.join("+", nodeId, extension, portName), nameList.get(0).getValue(),
            "value of client nep should be '" + portName + "'");
        assertEquals(nepName, nameList.get(0).getValueName(),
            "value-name of client nep for '" + portName + "' should be '" + nepName + "'");
        assertEquals(2, nep.getSupportedCepLayerProtocolQualifier().size(), "Client nep should support 2 kind of cep");
        assertThat("client nep should support 2 kind of cep",
            nep.getSupportedCepLayerProtocolQualifier(),
            hasItems(ODUTYPEODU4.VALUE, DIGITALSIGNALTYPE100GigE.VALUE));
        assertEquals(LayerProtocolName.ETH, nep.getLayerProtocolName(), "client nep should be of ETH protocol type");
        checkCommonPartOfNep(nep, false);
        checkSIP(nep, portName, nodeId, extension);
    }

    private void checkNepClient100GTpdr(OwnedNodeEdgePoint nep, Uuid nepUuid, String portName, String nepName,
                                        String nodeId, String extension) {
        assertEquals(nepUuid, nep.getUuid(), "bad uuid for " + portName);
        List<Name> nameList = new ArrayList<>(nep.nonnullName().values());
        assertEquals(String.join("+", nodeId, extension, portName), nameList.get(0).getValue(),
            "value of client nep should be '" + portName + "'");
        assertEquals(nepName, nameList.get(0).getValueName(),
            "value-name of client nep for '" + portName + "' should be 100G-tpdr'");
        assertEquals(1, nep.getSupportedCepLayerProtocolQualifier().size(), "Client nep should support 1 kind of cep");
        assertThat("client nep should support 2 kind of cep",
            nep.getSupportedCepLayerProtocolQualifier(),
            hasItems(DIGITALSIGNALTYPE100GigE.VALUE));
        assertEquals(LayerProtocolName.ETH, nep.getLayerProtocolName(), "client nep should be of ETH protocol type");
        checkCommonPartOfNep(nep, false);
        checkSIP(nep, portName, nodeId, extension);
    }

    private void checkNepOtsiNode(OwnedNodeEdgePoint nep, Uuid nepUuid, String portName, String nepName,
                                  String nodeId, String extension) {
        assertEquals(nepUuid, nep.getUuid(), "bad uuid for " + portName);
        List<Name> nameList = new ArrayList<>(nep.nonnullName().values());
        assertEquals(String.join("+", nodeId, extension, portName), nameList.get(0).getValue(),
            "value of OTSi nep should be '" + portName + "'");
        assertEquals(nepName, nameList.get(0).getValueName(), "value-name of OTSi nep should be '" + nepName + "'");
        assertEquals(2, nep.getSupportedCepLayerProtocolQualifier().size(), "OTSi nep should support 2 kind of cep");
        assertThat("OTSi nep should support 2 kind of cep",
            nep.getSupportedCepLayerProtocolQualifier(),
            hasItems(PHOTONICLAYERQUALIFIEROMS.VALUE, PHOTONICLAYERQUALIFIEROTSi.VALUE));
        assertEquals(LayerProtocolName.PHOTONICMEDIA, nep.getLayerProtocolName(),
            "OTSi nep should be of PHOTONIC_MEDIA protocol type");
        assertEquals(1, nep.getMappedServiceInterfacePoint().size(), "OTSi nep should support one SIP");
        checkCommonPartOfNep(nep, false);
        checkSIP(nep, portName, nodeId, extension);
    }

    private void checkSIP(OwnedNodeEdgePoint nep, String portName, String nodeId, String extension) {
        Uuid sipUuid = new Uuid(UUID.nameUUIDFromBytes((String.join("+", "SIP", nodeId, extension, portName))
            .getBytes(Charset.forName("UTF-8"))).toString());
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
        assertEquals(1, nep.getSupportedCepLayerProtocolQualifier().size(),
            "OTSi nep of RDM infra node should support only 1 kind of cep");
        assertThat("OTSi nep should support 2 kind of cep",
            nep.getSupportedCepLayerProtocolQualifier(),
            hasItems(PHOTONICLAYERQUALIFIEROMS.VALUE));
        assertEquals(LayerProtocolName.PHOTONICMEDIA, nep.getLayerProtocolName(),
            "OTSi nep should be of PHOTONIC_MEDIA protocol type");
        assertEquals(0, nep.nonnullMappedServiceInterfacePoint().size(), "OTSi nep of RDM infra should support no SIP");
        checkCommonPartOfNep(nep, true);
    }

    private void checkCommonPartOfNep(OwnedNodeEdgePoint nep, boolean isRdm) {
        assertEquals(PortDirection.BIDIRECTIONAL, nep.getLinkPortDirection(),
            "link port direction should be DIRECTIONAL");
        assertEquals(AdministrativeState.UNLOCKED, nep.getAdministrativeState(),
            "administrative state should be UNLOCKED");
        assertEquals(TerminationState.TERMINATEDBIDIRECTIONAL, nep.getTerminationState(),
            "termination state should be TERMINATED BIDIRECTIONAL");
        assertEquals(LifecycleState.INSTALLED, nep.getLifecycleState(),
            "life-cycle state should be INSTALLED");
        if (!isRdm) {
            assertEquals(1, nep.getMappedServiceInterfacePoint().size(), "client nep should support 1 SIP");
        }
        assertEquals(TerminationDirection.BIDIRECTIONAL, nep.getTerminationDirection(),
            "termination direction should be BIDIRECTIONAL");
        assertEquals(OperationalState.ENABLED, nep.getOperationalState(),
            "operational state of client nep should be ENABLED");
        assertEquals(PortRole.SYMMETRIC, nep.getLinkPortRole(), "link-port-role of client nep should be SYMMETRIC");
    }

    private void checkTransitionalLink(Link link, Uuid node1Uuid, Uuid node2Uuid, String tp1, String tp2,
                                       String ietfNodeId) {
        Uuid linkUuid = new Uuid(UUID.nameUUIDFromBytes((tp1 + "to" + tp2)
            .getBytes(Charset.forName("UTF-8"))).toString());
        assertEquals(linkUuid, link.getUuid(), "bad uuid for link between DSR node " + tp1 + " and iOTSI port " + tp2);
        assertEquals(CapacityUnit.GBPS, link.getAvailableCapacity().getTotalSize().getUnit(),
            "Available capacity unit should be GBPS");
        assertEquals(Uint64.valueOf(100), link.getAvailableCapacity().getTotalSize().getValue(),
            "Available capacity -total size value should be 100");
        assertEquals(2, link.getTransitionedLayerProtocolName().size(),
            "transitional link should be between 2 nodes of protocol layers ODU and PHOTONIC_MEDIA");
        assertThat("transitional link should be between 2 nodes of protocol layers ODU and PHOTONIC_MEDIA",
            link.getTransitionedLayerProtocolName(),
            hasItems(LayerProtocolName.ODU.getName(), LayerProtocolName.PHOTONICMEDIA.getName()));
        assertEquals(ForwardingDirection.BIDIRECTIONAL, link.getDirection(),
            "transitional link should be BIDIRECTIONAL");
        List<org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210
            .link.NodeEdgePoint> nodeEdgePointList = new ArrayList<>(link.nonnullNodeEdgePoint().values());
        assertEquals(topologyUuid, nodeEdgePointList.get(0).getTopologyUuid(),
            "topology uuid should be the same for the two termination point of the link");
        assertEquals(topologyUuid, nodeEdgePointList.get(1).getTopologyUuid(),
            "topology uuid should be the same for the two termination point of the link");
        assertThat("transitional links should terminate on DSR node and Photonic node",
            nodeEdgePointList.get(0).getNodeUuid().getValue(),
            either(containsString(node1Uuid.getValue())).or(containsString(node2Uuid.getValue())));
        assertThat("transitional links should terminate on DSR node and Photonic node",
            nodeEdgePointList.get(1).getNodeUuid().getValue(),
            either(containsString(node1Uuid.getValue())).or(containsString(node2Uuid.getValue())));
        Uuid nep1Uuid = new Uuid(UUID.nameUUIDFromBytes(tp1.getBytes(Charset.forName("UTF-8"))).toString());
        Uuid nep2Uuid = new Uuid(UUID.nameUUIDFromBytes(tp2.getBytes(Charset.forName("UTF-8"))).toString());
        assertThat("transitional links should terminate on " + tp1 + " and " + tp2 + " neps",
            nodeEdgePointList.get(0).getNodeEdgePointUuid().getValue(),
            either(containsString(nep1Uuid.getValue())).or(containsString(nep2Uuid.getValue())));
        assertThat("transitional links should terminate on DSR node and Photonic node",
            nodeEdgePointList.get(1).getNodeEdgePointUuid().getValue(),
            either(containsString(nep1Uuid.getValue())).or(containsString(nep2Uuid.getValue())));
    }

    private void checkOtnLink(Link link, Uuid node1Uuid, Uuid node2Uuid, Uuid tp1Uuid, Uuid tp2Uuid, Uuid linkUuid,
                              String linkName) {
        assertEquals(linkName, link.getName().get(new NameKey("otn link name")).getValue(), "bad name for the link");
        assertEquals(linkUuid, link.getUuid(), "bad uuid for link");
        assertEquals(CapacityUnit.GBPS, link.getAvailableCapacity().getTotalSize().getUnit(),
            "Available capacity unit should be MBPS");
        String prefix = linkName.split("-")[0];
        if ("OTU4".equals(prefix)) {
            assertEquals(Uint64.valueOf(0), link.getAvailableCapacity().getTotalSize().getValue(),
                "Available capacity -total size value should be 0");
        } else if ("ODTU4".equals(prefix)) {
            assertEquals(Uint64.valueOf(100000), link.getAvailableCapacity().getTotalSize().getValue(),
                "Available capacity -total size value should be 100 000");
        }
        assertEquals(CapacityUnit.GBPS, link.getTotalPotentialCapacity().getTotalSize().getUnit(),
            "Total capacity unit should be GBPS");
        assertEquals(Uint64.valueOf(100), link.getTotalPotentialCapacity().getTotalSize().getValue(),
            "Total capacity -total size value should be 100");
        if ("OTU4".equals(prefix)) {
            assertEquals(
                LayerProtocolName.PHOTONICMEDIA.getName(),
                link.getLayerProtocolName().stream().findFirst().get().getName(),
                "otn link should be between 2 nodes of protocol layers PHOTONIC_MEDIA");
        } else if ("ODTU4".equals(prefix)) {
            assertEquals(
                LayerProtocolName.ODU.getName(),
                link.getLayerProtocolName().stream().findFirst().get().getName(),
                "otn link should be between 2 nodes of protocol layers ODU");
        }
        assertEquals(ForwardingDirection.BIDIRECTIONAL, link.getDirection(), "otn tapi link should be BIDIRECTIONAL");
        List<org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210
            .link.NodeEdgePoint> nodeEdgePointList = new ArrayList<>(link.nonnullNodeEdgePoint().values());
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

    private void checkOmsLink(Link link, Uuid node1Uuid, Uuid node2Uuid, Uuid tp1Uuid, Uuid tp2Uuid, Uuid linkUuid,
                              String linkName) {
        assertEquals(linkName, link.getName().get(new NameKey("OMS link name")).getValue(), "bad name for the link");
        assertEquals(linkUuid, link.getUuid(), "bad uuid for link");
        assertEquals(
            LayerProtocolName.PHOTONICMEDIA.getName(),
            link.getLayerProtocolName().stream().findFirst().get().getName(),
            "oms link should be between 2 nodes of protocol layers PHOTONIC_MEDIA");
        assertEquals(ForwardingDirection.BIDIRECTIONAL, link.getDirection(), "otn tapi link should be BIDIRECTIONAL");
        List<org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210
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
        org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.Node1Builder tpdr1Bldr
            = new org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.Node1Builder(
            initialNode.augmentation(Node1.class));
        Map<TerminationPointKey, TerminationPoint> tps = new HashMap<>(tpdr1Bldr.getTerminationPoint());
        TerminationPointBuilder tpBldr = new TerminationPointBuilder(
            tps.get(new TerminationPointKey(new TpId(tpid))));
        TerminationPoint1Builder tp1Bldr = new TerminationPoint1Builder(tpBldr.augmentation(TerminationPoint1.class));
        tp1Bldr.setAdministrativeState(admin)
            .setOperationalState(oper);
        tpBldr.addAugmentation(tp1Bldr.build());
        tps.replace(tpBldr.key(), tpBldr.build());
        tpdr1Bldr.setTerminationPoint(tps);
        return new NodeBuilder(initialNode).addAugmentation(tpdr1Bldr.build()).build();
    }

    private org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.networks.network
            .Link changeOtnLinkState(
            org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.networks.network
            .Link initiallink, AdminStates admin, State oper) {

        org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.networks.network
            .LinkBuilder linkBldr = new
            org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.networks.network
            .LinkBuilder(initiallink);
        Link1Builder link1Bldr = new Link1Builder(linkBldr.augmentation(Link1.class));
        link1Bldr.setAdministrativeState(admin)
            .setOperationalState(oper);
        linkBldr.addAugmentation(link1Bldr.build());
        return linkBldr.build();
    }
}