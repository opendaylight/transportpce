/*
 * Copyright © 2021 Nokia, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.tapi.topology;

import static org.hamcrest.CoreMatchers.anyOf;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.either;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import com.google.common.util.concurrent.FluentFuture;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
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
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.CAPACITYUNITGBPS;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.Direction;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.ForwardingDirection;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.LAYERPROTOCOLQUALIFIER;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.LayerProtocolName;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.LifecycleState;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.OperationalState;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.PortRole;
//import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.TerminationDirection;
//import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.TerminationState;
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
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.photonic.media.rev221121.PHOTONICLAYERQUALIFIEROTSi;
//import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.ForwardingRule;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.FORWARDINGRULEMAYFORWARDACROSSGROUP;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.RuleType;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.node.NodeRuleGroup;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.node.OwnedNodeEdgePoint;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.node.OwnedNodeEdgePointKey;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.node.edge.point.SupportedCepLayerProtocolQualifierInstances;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.node.rule.group.NodeEdgePoint;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.node.rule.group.Rule;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.KeyedInstanceIdentifier;
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
            TapiTopologyDataUtils.OPENROADM_NETWORK_FILE, InstanceIdentifiers.UNDERLAY_NETWORK_II);
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
        otnMuxA = muxAFuture.get().orElseThrow();
        KeyedInstanceIdentifier<Node, NodeKey> muxCIID = InstanceIdentifier.create(Networks.class)
            .child(org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.networks.Network
                .class, new NetworkKey(new NetworkId("otn-topology")))
            .child(Node.class, new NodeKey(new NodeId("SPDR-SC1-XPDR1")));
        FluentFuture<Optional<Node>> muxCFuture = dataBroker.newReadOnlyTransaction()
            .read(LogicalDatastoreType.CONFIGURATION, muxCIID);
        muxCFuture.get().orElseThrow();
        KeyedInstanceIdentifier<Node, NodeKey> switchIID = InstanceIdentifier.create(Networks.class)
            .child(org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.networks.Network
                .class, new NetworkKey(new NetworkId("otn-topology")))
            .child(Node.class, new NodeKey(new NodeId("SPDR-SA1-XPDR2")));
        FluentFuture<Optional<Node>> switchFuture = dataBroker.newReadOnlyTransaction()
            .read(LogicalDatastoreType.CONFIGURATION, switchIID);
        otnSwitch = switchFuture.get().orElseThrow();
        KeyedInstanceIdentifier<Node, NodeKey> roadmaIID = InstanceIdentifier.create(Networks.class)
            .child(org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.networks.Network
                .class, new NetworkKey(new NetworkId("openroadm-network")))
            .child(Node.class, new NodeKey(new NodeId("ROADM-A1")));
        FluentFuture<Optional<Node>> roadmaFuture = dataBroker.newReadOnlyTransaction()
            .read(LogicalDatastoreType.CONFIGURATION, roadmaIID);
        roadmA = roadmaFuture.get().orElseThrow();
        KeyedInstanceIdentifier<Node, NodeKey> roadmcIID = InstanceIdentifier.create(Networks.class)
            .child(org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.networks.Network
                .class, new NetworkKey(new NetworkId("openroadm-network")))
            .child(Node.class, new NodeKey(new NodeId("ROADM-C1")));
        FluentFuture<Optional<Node>> roadmcFuture = dataBroker.newReadOnlyTransaction()
            .read(LogicalDatastoreType.CONFIGURATION, roadmcIID);
        roadmC = roadmcFuture.get().orElseThrow();

        KeyedInstanceIdentifier<Node, NodeKey> tpdrIID = InstanceIdentifier.create(Networks.class)
            .child(org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.networks.Network
                .class, new NetworkKey(new NetworkId("otn-topology")))
            .child(Node.class, new NodeKey(new NodeId("XPDR-A1-XPDR1")));
        FluentFuture<Optional<Node>> tpdrFuture = dataBroker.newReadOnlyTransaction()
            .read(LogicalDatastoreType.CONFIGURATION, tpdrIID);
        tpdr100G = tpdrFuture.get().orElseThrow();

        InstanceIdentifier<Network1> linksIID = InstanceIdentifier.create(Networks.class)
            .child(org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.networks.Network
                .class, new NetworkKey(new NetworkId("otn-topology")))
            .augmentation(Network1.class);
        FluentFuture<Optional<Network1>> linksFuture = dataBroker.newReadOnlyTransaction()
            .read(LogicalDatastoreType.CONFIGURATION, linksIID);
        linksFuture.get().orElseThrow().getLink();

        InstanceIdentifier<Network1> links1IID = InstanceIdentifier.create(Networks.class)
            .child(org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.networks.Network
                .class, new NetworkKey(new NetworkId("openroadm-topology")))
            .augmentation(Network1.class);
        FluentFuture<Optional<Network1>> links1Future = dataBroker.newReadOnlyTransaction()
            .read(LogicalDatastoreType.CONFIGURATION, links1IID);
        ortopoLinks = links1Future.get().orElseThrow().getLink();

        InstanceIdentifier<Network> ortopo1IID = InstanceIdentifier.create(Networks.class)
            .child(org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.networks.Network
                .class, new NetworkKey(new NetworkId("openroadm-topology")));
        FluentFuture<Optional<Network>> ortopoFuture = dataBroker.newReadOnlyTransaction()
            .read(LogicalDatastoreType.CONFIGURATION, ortopo1IID);
        openroadmNet = ortopoFuture.get().orElseThrow();

        topologyUuid = new Uuid(UUID.nameUUIDFromBytes(TapiStringConstants.T0_FULL_MULTILAYER.getBytes(
            Charset.forName("UTF-8"))).toString());
        networkTransactionService = new NetworkTransactionImpl(getDataBroker());
        tapiLink = new TapiLinkImpl(networkTransactionService);
        LOG.info("TEST SETUP READY");
    }

    @Test
    void convertNodeWhenNoStates() {
        Node tpdr = changeTerminationPointState(tpdr100G, "XPDR1-NETWORK1", "XPDR1-CLIENT1",  null, null);
        List<String> networkPortList = new ArrayList<>();
        for (TerminationPoint tp : tpdr100G.augmentation(Node1.class).getTerminationPoint().values()) {
            if (tp.augmentation(TerminationPoint1.class).getTpType().equals(OpenroadmTpType.XPONDERNETWORK)) {
                networkPortList.add(tp.getTpId().getValue());
            }
        }
        ConvertORTopoToTapiFullTopo tapiFactory = new ConvertORTopoToTapiFullTopo(topologyUuid, tapiLink);
        tapiFactory.convertNode(tpdr, networkPortList);

        Uuid dsrNodeUuid = new Uuid(UUID.nameUUIDFromBytes("XPDR-A1-XPDR1+DSR".getBytes(Charset.forName("UTF-8")))
            .toString());
        org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.topology.Node dsrNode = tapiFactory
            .getTapiNodes().get(new
                org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.topology.NodeKey(dsrNodeUuid));
        Uuid enetworkNepUuid = new Uuid(
            UUID.nameUUIDFromBytes(("XPDR-A1-XPDR1+eODU+XPDR1-CLIENT1").getBytes(Charset.forName("UTF-8")))
                .toString());
        Uuid inetworkNepUuid = new Uuid(
            UUID.nameUUIDFromBytes(("XPDR-A1-XPDR1+iODU+XPDR1-NETWORK1").getBytes(Charset.forName("UTF-8")))
                .toString());
        OwnedNodeEdgePoint enepN = dsrNode.nonnullOwnedNodeEdgePoint().get(new OwnedNodeEdgePointKey(enetworkNepUuid));
        assertNull(enepN.getAdministrativeState(), "Administrative State should not be present");
        assertNull(enepN.getOperationalState(), "Operational State should not be present");

        OwnedNodeEdgePoint inepN = dsrNode.nonnullOwnedNodeEdgePoint().get(new OwnedNodeEdgePointKey(inetworkNepUuid));
        assertNull(inepN.getAdministrativeState(), "Administrative State should not be present");
        assertNull(inepN.getOperationalState(), "Operational State should not be present");

        Uuid otsiNodeUuid = new Uuid(UUID.nameUUIDFromBytes("XPDR-A1-XPDR1+OTSi".getBytes(Charset.forName("UTF-8")))
            .toString());
        org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.topology.Node otsiNode = tapiFactory
            .getTapiNodes().get(new
                org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.topology.NodeKey(otsiNodeUuid));
        Uuid enepUuid = new Uuid(
            UUID.nameUUIDFromBytes(("XPDR-A1-XPDR1+eOTSi+XPDR1-NETWORK1").getBytes(Charset.forName("UTF-8")))
                .toString());
        OwnedNodeEdgePoint enep = otsiNode.nonnullOwnedNodeEdgePoint().get(new OwnedNodeEdgePointKey(enepUuid));
        assertNull(enep.getAdministrativeState(), "Administrative State should not be present");
        assertNull(enep.getOperationalState(), "Operational State should not be present");

        Uuid inepUuid = new Uuid(
            UUID.nameUUIDFromBytes(("XPDR-A1-XPDR1+iOTSi+XPDR1-NETWORK1").getBytes(Charset.forName("UTF-8")))
                .toString());
        OwnedNodeEdgePoint inep = otsiNode.nonnullOwnedNodeEdgePoint().get(new OwnedNodeEdgePointKey(inepUuid));
        assertNull(inep.getAdministrativeState(), "Administrative State should not be present");
        assertNull(inep.getOperationalState(), "Operational State should not be present");

        Uuid photnepUuid = new Uuid(
            UUID.nameUUIDFromBytes(("XPDR-A1-XPDR1+PHOTONIC_MEDIA+XPDR1-NETWORK1").getBytes(Charset.forName("UTF-8")))
                .toString());
        OwnedNodeEdgePoint photnep = otsiNode.nonnullOwnedNodeEdgePoint().get(new OwnedNodeEdgePointKey(photnepUuid));
        assertNull(photnep.getAdministrativeState(), "Administrative State should not be present");
        assertNull(photnep.getOperationalState(), "Operational State should not be present");
    }

    @Test
    void convertNodeWhenBadStates1() {
        Node tpdr = changeTerminationPointState(tpdr100G, "XPDR1-NETWORK1", "XPDR1-CLIENT1",
            AdminStates.OutOfService, State.OutOfService);
        List<String> networkPortList = new ArrayList<>();
        for (TerminationPoint tp : tpdr100G.augmentation(Node1.class).getTerminationPoint().values()) {
            if (tp.augmentation(TerminationPoint1.class).getTpType().equals(OpenroadmTpType.XPONDERNETWORK)) {
                networkPortList.add(tp.getTpId().getValue());
            }
        }
        ConvertORTopoToTapiFullTopo tapiFactory = new ConvertORTopoToTapiFullTopo(topologyUuid, tapiLink);
        tapiFactory.convertNode(tpdr, networkPortList);

        Uuid dsrNodeUuid = new Uuid(UUID.nameUUIDFromBytes("XPDR-A1-XPDR1+DSR".getBytes(Charset.forName("UTF-8")))
            .toString());
        org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.topology.Node dsrNode = tapiFactory
            .getTapiNodes().get(new
                org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.topology.NodeKey(dsrNodeUuid));
        Uuid enetworkNepUuid = new Uuid(
            UUID.nameUUIDFromBytes(("XPDR-A1-XPDR1+eODU+XPDR1-CLIENT1").getBytes(Charset.forName("UTF-8")))
                .toString());
        Uuid inetworkNepUuid = new Uuid(
            UUID.nameUUIDFromBytes(("XPDR-A1-XPDR1+iODU+XPDR1-NETWORK1").getBytes(Charset.forName("UTF-8")))
                .toString());
        OwnedNodeEdgePoint enepN = dsrNode.nonnullOwnedNodeEdgePoint().get(new OwnedNodeEdgePointKey(enetworkNepUuid));
        assertEquals(AdministrativeState.LOCKED, enepN.getAdministrativeState(),
            "Administrative State should be Locked");
        assertEquals(OperationalState.DISABLED, enepN.getOperationalState(), "Operational State should be Disabled");

        OwnedNodeEdgePoint inepN = dsrNode.nonnullOwnedNodeEdgePoint().get(new OwnedNodeEdgePointKey(inetworkNepUuid));
        assertEquals(AdministrativeState.LOCKED, inepN.getAdministrativeState(),
            "Administrative State should be Locked");
        assertEquals(OperationalState.DISABLED, inepN.getOperationalState(), "Operational State should be Disabled");

        Uuid otsiNodeUuid = new Uuid(UUID.nameUUIDFromBytes("XPDR-A1-XPDR1+OTSi".getBytes(Charset.forName("UTF-8")))
            .toString());
        org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.topology.Node otsiNode = tapiFactory
            .getTapiNodes().get(new
                org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.topology.NodeKey(otsiNodeUuid));
        Uuid enepUuid = new Uuid(
            UUID.nameUUIDFromBytes(("XPDR-A1-XPDR1+eOTSi+XPDR1-NETWORK1").getBytes(Charset.forName("UTF-8")))
                .toString());
        OwnedNodeEdgePoint enep = otsiNode.nonnullOwnedNodeEdgePoint().get(new OwnedNodeEdgePointKey(enepUuid));
        assertEquals(AdministrativeState.LOCKED, enep.getAdministrativeState(),
            "Administrative State should be Locked");
        assertEquals(OperationalState.DISABLED, enep.getOperationalState(), "Operational State should be Disabled");

        Uuid inepUuid = new Uuid(
            UUID.nameUUIDFromBytes(("XPDR-A1-XPDR1+iOTSi+XPDR1-NETWORK1").getBytes(Charset.forName("UTF-8")))
                .toString());
        OwnedNodeEdgePoint inep = otsiNode.nonnullOwnedNodeEdgePoint().get(new OwnedNodeEdgePointKey(inepUuid));
        assertEquals(AdministrativeState.LOCKED, inep.getAdministrativeState(),
            "Administrative State should be Locked");
        assertEquals(OperationalState.DISABLED, inep.getOperationalState(), "Operational State should be Disabled");

        Uuid photnepUuid = new Uuid(
            UUID.nameUUIDFromBytes(("XPDR-A1-XPDR1+PHOTONIC_MEDIA+XPDR1-NETWORK1").getBytes(Charset.forName("UTF-8")))
                .toString());
        OwnedNodeEdgePoint photnep = otsiNode.nonnullOwnedNodeEdgePoint().get(new OwnedNodeEdgePointKey(photnepUuid));
        assertEquals(AdministrativeState.LOCKED, photnep.getAdministrativeState(),
            "Administrative State should be Locked");
        assertEquals(OperationalState.DISABLED, photnep.getOperationalState(), "Operational State should be Disabled");
    }

    @Test
    void convertNodeWhenBadStates2() {
        Node tpdr = changeTerminationPointState(tpdr100G, "XPDR1-NETWORK1", "XPDR1-CLIENT1",
            AdminStates.Maintenance, State.Degraded);
        List<String> networkPortList = new ArrayList<>();
        for (TerminationPoint tp : tpdr100G.augmentation(Node1.class).getTerminationPoint().values()) {
            if (tp.augmentation(TerminationPoint1.class).getTpType().equals(OpenroadmTpType.XPONDERNETWORK)) {
                networkPortList.add(tp.getTpId().getValue());
            }
        }
        ConvertORTopoToTapiFullTopo tapiFactory = new ConvertORTopoToTapiFullTopo(topologyUuid, tapiLink);
        tapiFactory.convertNode(tpdr, networkPortList);

        Uuid dsrNodeUuid = new Uuid(UUID.nameUUIDFromBytes("XPDR-A1-XPDR1+DSR".getBytes(Charset.forName("UTF-8")))
            .toString());
        org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.topology.Node dsrNode = tapiFactory
            .getTapiNodes().get(new
                org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.topology.NodeKey(dsrNodeUuid));
        Uuid enetworkNepUuid = new Uuid(
            UUID.nameUUIDFromBytes(("XPDR-A1-XPDR1+eODU+XPDR1-CLIENT1").getBytes(Charset.forName("UTF-8")))
                .toString());
        Uuid inetworkNepUuid = new Uuid(
            UUID.nameUUIDFromBytes(("XPDR-A1-XPDR1+iODU+XPDR1-NETWORK1").getBytes(Charset.forName("UTF-8")))
                .toString());
        OwnedNodeEdgePoint enepN = dsrNode.nonnullOwnedNodeEdgePoint().get(new OwnedNodeEdgePointKey(enetworkNepUuid));
        assertEquals(AdministrativeState.LOCKED, enepN.getAdministrativeState(),
            "Administrative State should be Locked");
        assertEquals(OperationalState.DISABLED, enepN.getOperationalState(), "Operational State should be Disabled");

        OwnedNodeEdgePoint inepN = dsrNode.nonnullOwnedNodeEdgePoint().get(new OwnedNodeEdgePointKey(inetworkNepUuid));
        assertEquals(AdministrativeState.LOCKED, inepN.getAdministrativeState(),
            "Administrative State should be Locked");
        assertEquals(OperationalState.DISABLED, inepN.getOperationalState(), "Operational State should be Disabled");

        Uuid otsiNodeUuid = new Uuid(UUID.nameUUIDFromBytes("XPDR-A1-XPDR1+OTSi".getBytes(Charset.forName("UTF-8")))
            .toString());
        org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.topology.Node otsiNode = tapiFactory
            .getTapiNodes().get(new
                org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.topology.NodeKey(otsiNodeUuid));
        Uuid enepUuid = new Uuid(
            UUID.nameUUIDFromBytes(("XPDR-A1-XPDR1+eOTSi+XPDR1-NETWORK1").getBytes(Charset.forName("UTF-8")))
                .toString());
        OwnedNodeEdgePoint enep = otsiNode.nonnullOwnedNodeEdgePoint().get(new OwnedNodeEdgePointKey(enepUuid));
        assertEquals(AdministrativeState.LOCKED, enep.getAdministrativeState(),
            "Administrative State should be Locked");
        assertEquals(OperationalState.DISABLED, enep.getOperationalState(), "Operational State should be Disabled");

        Uuid inepUuid = new Uuid(
            UUID.nameUUIDFromBytes(("XPDR-A1-XPDR1+iOTSi+XPDR1-NETWORK1").getBytes(Charset.forName("UTF-8")))
                .toString());
        OwnedNodeEdgePoint inep = otsiNode.nonnullOwnedNodeEdgePoint().get(new OwnedNodeEdgePointKey(inepUuid));
        assertEquals(AdministrativeState.LOCKED, inep.getAdministrativeState(),
            "Administrative State should be Locked");
        assertEquals(OperationalState.DISABLED, inep.getOperationalState(), "Operational State should be Disabled");

        Uuid photnepUuid = new Uuid(
            UUID.nameUUIDFromBytes(("XPDR-A1-XPDR1+PHOTONIC_MEDIA+XPDR1-NETWORK1").getBytes(Charset.forName("UTF-8")))
                .toString());
        OwnedNodeEdgePoint photnep = otsiNode.nonnullOwnedNodeEdgePoint().get(new OwnedNodeEdgePointKey(photnepUuid));
        assertEquals(AdministrativeState.LOCKED, photnep.getAdministrativeState(),
            "Administrative State should be Locked");
        assertEquals(OperationalState.DISABLED, photnep.getOperationalState(), "Operational State should be Disabled");
    }

    @Test
    void convertNodeForTransponder100G() {
        ConvertORTopoToTapiFullTopo tapiFactory = new ConvertORTopoToTapiFullTopo(topologyUuid, tapiLink);
        List<String> networkPortList = new ArrayList<>();
        for (TerminationPoint tp : tpdr100G.augmentation(Node1.class).getTerminationPoint().values()) {
            if (tp.augmentation(TerminationPoint1.class).getTpType().equals(OpenroadmTpType.XPONDERNETWORK)) {
                networkPortList.add(tp.getTpId().getValue());
            }
        }
        tapiFactory.convertNode(tpdr100G, networkPortList);
        List<org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.topology.Node> tapiNodes
            = tapiFactory.getTapiNodes().values().stream()
            .sorted((n1, n2) -> n1.getUuid().getValue().compareTo(n2.getUuid().getValue()))
            .collect(Collectors.toList());

        assertEquals(2, tapiFactory.getTapiNodes().size(), "Node list size should be 2");
        assertEquals(2, tapiFactory.getTapiLinks().size(), "Link list size should be 2");

        Uuid dsrNodeUuid = new Uuid(UUID.nameUUIDFromBytes("XPDR-A1-XPDR1+DSR".getBytes(Charset.forName("UTF-8")))
            .toString());
        checkDsrNode(tapiNodes.get(1), dsrNodeUuid, "tpdr", "XPDR-A1-XPDR1");
        Uuid otsiNodeUuid = new Uuid(UUID.nameUUIDFromBytes("XPDR-A1-XPDR1+OTSi".getBytes(Charset.forName("UTF-8")))
            .toString());
        checkOtsiNode(tapiNodes.get(0), otsiNodeUuid, "tpdr", "XPDR-A1-XPDR1");

        List<org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.topology.Link> tapiLinks
            = tapiFactory.getTapiLinks().values().stream()
            .sorted((l1, l2) -> l1.getUuid().getValue().compareTo(l2.getUuid().getValue()))
            .collect(Collectors.toList());
        checkTransitionalLink(tapiLinks.get(1), dsrNodeUuid, otsiNodeUuid,
            "XPDR-A1-XPDR1+iODU+XPDR1-NETWORK1", "XPDR-A1-XPDR1+iOTSi+XPDR1-NETWORK1", "XPDR-A1-XPDR1");
    }

    @Test
    void convertNodeForOtnMuxponder() {
        ConvertORTopoToTapiFullTopo tapiFactory = new ConvertORTopoToTapiFullTopo(topologyUuid, tapiLink);
        List<String> networkPortList = new ArrayList<>();
        for (TerminationPoint tp : otnMuxA.augmentation(Node1.class).getTerminationPoint().values()) {
            if (tp.augmentation(TerminationPoint1.class).getTpType().equals(OpenroadmTpType.XPONDERNETWORK)) {
                networkPortList.add(tp.getTpId().getValue());
            }
        }
        tapiFactory.convertNode(otnMuxA, networkPortList);
        List<org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.topology.Node> tapiNodes
            = tapiFactory.getTapiNodes().values().stream()
            .sorted((n1, n2) -> n1.getUuid().getValue().compareTo(n2.getUuid().getValue()))
            .collect(Collectors.toList());

        assertEquals(2, tapiFactory.getTapiNodes().size(), "Node list size should be 2");
        assertEquals(1, tapiFactory.getTapiLinks().size(), "Link list size should be 1");
        Uuid dsrNodeUuid = new Uuid(UUID.nameUUIDFromBytes("SPDR-SA1-XPDR1+DSR".getBytes(Charset.forName("UTF-8")))
            .toString());
        checkDsrNode(tapiNodes.get(0), dsrNodeUuid, "mux", "SPDR-SA1-XPDR1");
        Uuid otsiNodeUuid = new Uuid(UUID.nameUUIDFromBytes("SPDR-SA1-XPDR1+OTSi".getBytes(Charset.forName("UTF-8")))
            .toString());
        checkOtsiNode(tapiNodes.get(1), otsiNodeUuid, "mux", "SPDR-SA1-XPDR1");

        List<org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.topology.Link> tapiLinks
            = tapiFactory.getTapiLinks().values().stream()
            .sorted((l1, l2) -> l1.getUuid().getValue().compareTo(l2.getUuid().getValue()))
            .collect(Collectors.toList());
        checkTransitionalLink(tapiLinks.get(0), dsrNodeUuid, otsiNodeUuid,
            "SPDR-SA1-XPDR1+iODU+XPDR1-NETWORK1", "SPDR-SA1-XPDR1+iOTSi+XPDR1-NETWORK1", "SPDR-SA1-XPDR1");
    }

    @Test
    void convertNodeForOtnSwitch() {
        ConvertORTopoToTapiFullTopo tapiFactory = new ConvertORTopoToTapiFullTopo(topologyUuid, tapiLink);
        List<String> networkPortList = new ArrayList<>();
        for (TerminationPoint tp : otnSwitch.augmentation(Node1.class).getTerminationPoint().values()) {
            if (tp.augmentation(TerminationPoint1.class).getTpType().equals(OpenroadmTpType.XPONDERNETWORK)) {
                networkPortList.add(tp.getTpId().getValue());
            }
        }
        tapiFactory.convertNode(otnSwitch, networkPortList);
        List<org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.topology.Node> tapiNodes
            = tapiFactory.getTapiNodes().values().stream()
            .sorted((n1, n2) -> n1.getUuid().getValue().compareTo(n2.getUuid().getValue()))
            .collect(Collectors.toList());

        assertEquals(2, tapiFactory.getTapiNodes().size(), "Node list size should be 2");
        assertEquals(4, tapiFactory.getTapiLinks().size(), "Link list size should be 4");

        Uuid dsrNodeUuid = new Uuid(UUID.nameUUIDFromBytes("SPDR-SA1-XPDR2+DSR".getBytes(Charset.forName("UTF-8")))
            .toString());
        checkDsrNode(tapiNodes.get(0), dsrNodeUuid, "switch", "SPDR-SA1-XPDR2");
        Uuid otsiNodeUuid = new Uuid(UUID.nameUUIDFromBytes("SPDR-SA1-XPDR2+OTSi".getBytes(Charset.forName("UTF-8")))
            .toString());
        checkOtsiNode(tapiNodes.get(1), otsiNodeUuid, "switch", "SPDR-SA1-XPDR2");

        List<org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.topology.Link> tapiLinks
            = tapiFactory.getTapiLinks().values().stream()
            .sorted((l1, l2) -> l1.getUuid().getValue().compareTo(l2.getUuid().getValue()))
            .collect(Collectors.toList());
        checkTransitionalLink(tapiLinks.get(2), dsrNodeUuid, otsiNodeUuid,
            "SPDR-SA1-XPDR2+iODU+XPDR2-NETWORK4", "SPDR-SA1-XPDR2+iOTSi+XPDR2-NETWORK4", "SPDR-SA1-XPDR2");
    }

    @Test
    void convertNodeForRoadmWhenNoOtnMuxAttached() {
        ConvertORTopoToTapiFullTopo tapiFactory = new ConvertORTopoToTapiFullTopo(topologyUuid, tapiLink);
        tapiFactory.convertRoadmNode(roadmA, openroadmNet);

        assertEquals(1, tapiFactory.getTapiNodes().size(), "Node list size should be 1");
        assertEquals(0, tapiFactory.getTapiLinks().size(), "Link list size should be empty");
        List<org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.topology.Node> tapiNodes
            = tapiFactory.getTapiNodes().values().stream().collect(Collectors.toList());
        Uuid roadmNodeUuid = new Uuid(UUID.nameUUIDFromBytes((roadmA.getNodeId().getValue() + "+PHOTONIC_MEDIA")
            .getBytes(Charset.forName("UTF-8"))).toString());
        checkOtsiNode(tapiNodes.get(0), roadmNodeUuid, "roadm", "ROADM-A1");
    }

    @Test
    void convertNodeForRoadmWhenRoadmNeighborAttached() {
        ConvertORTopoToTapiFullTopo tapiFactory = new ConvertORTopoToTapiFullTopo(topologyUuid, tapiLink);
        tapiFactory.convertRoadmNode(roadmA, openroadmNet);
        tapiFactory.convertRoadmNode(roadmC, openroadmNet);

        List<Link> rdmTordmLinkList = ortopoLinks.values().stream()
            .filter(lk -> lk.augmentation(Link1.class).getLinkType().equals(OpenroadmLinkType.ROADMTOROADM))
            .collect(Collectors.toList());
        tapiFactory.convertRdmToRdmLinks(rdmTordmLinkList);

        assertEquals(2, tapiFactory.getTapiNodes().size(), "Node list size should be 2");
        assertEquals(1, tapiFactory.getTapiLinks().size(), "Link list size should be 1");

        List<org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.topology.Node> tapiNodes
            = tapiFactory.getTapiNodes().values().stream().collect(Collectors.toList());
        Uuid roadmaNodeUuid = new Uuid(UUID.nameUUIDFromBytes((roadmA.getNodeId().getValue() + "+PHOTONIC_MEDIA")
            .getBytes(Charset.forName("UTF-8"))).toString());
        checkOtsiNode(tapiNodes.get(1), roadmaNodeUuid, "roadm", "ROADM-A1");

        List<org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.topology.Link> links
            = tapiFactory.getTapiLinks().values().stream()
            .sorted((l1, l2) -> l1.getUuid().getValue().compareTo(l2.getUuid().getValue()))
            .collect(Collectors.toList());
        Uuid node1Uuid = new Uuid(UUID.nameUUIDFromBytes("ROADM-A1+PHOTONIC_MEDIA".getBytes(Charset.forName("UTF-8")))
            .toString());
        Uuid node2Uuid = new Uuid(UUID.nameUUIDFromBytes("ROADM-C1+PHOTONIC_MEDIA".getBytes(Charset.forName("UTF-8")))
            .toString());
        Uuid tp1Uuid = new Uuid(UUID.nameUUIDFromBytes("ROADM-A1+PHOTONIC_MEDIA+DEG2-TTP-TXRX"
            .getBytes(Charset.forName("UTF-8"))).toString());
        Uuid tp2Uuid = new Uuid(UUID.nameUUIDFromBytes(("ROADM-C1+PHOTONIC_MEDIA+DEG1-TTP-TXRX")
            .getBytes(Charset.forName("UTF-8"))).toString());
        Uuid linkUuid =
            new Uuid(UUID.nameUUIDFromBytes(
                "ROADM-C1+PHOTONIC_MEDIA+DEG1-TTP-TXRXtoROADM-A1+PHOTONIC_MEDIA+DEG2-TTP-TXRX"
                    .getBytes(Charset.forName("UTF-8"))).toString());
        checkOmsLink(links.get(0), node1Uuid, node2Uuid, tp1Uuid, tp2Uuid, linkUuid,
            "ROADM-C1+PHOTONIC_MEDIA+DEG1-TTP-TXRXtoROADM-A1+PHOTONIC_MEDIA+DEG2-TTP-TXRX");
    }

    @Test
    void convertNodeForRoadmWhenOtnMuxAttached() {
        ConvertORTopoToTapiFullTopo tapiFactory = new ConvertORTopoToTapiFullTopo(topologyUuid, tapiLink);
        List<String> networkPortListA = new ArrayList<>();
        for (TerminationPoint tp : otnMuxA.augmentation(Node1.class).getTerminationPoint().values()) {
            if (tp.augmentation(TerminationPoint1.class).getTpType().equals(OpenroadmTpType.XPONDERNETWORK)) {
                networkPortListA.add(tp.getTpId().getValue());
            }
        }
        tapiFactory.convertNode(otnMuxA, networkPortListA);
        tapiFactory.convertRoadmNode(roadmA, openroadmNet);
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
        tapiFactory.convertXpdrToRdmLinks(xponderInLinkList);
        assertEquals(3, tapiFactory.getTapiNodes().size(), "Node list size should be 3");
        assertEquals(2, tapiFactory.getTapiLinks().size(), "Link list size should be 2");
        List<org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.topology.Node> tapiNodes
            = tapiFactory.getTapiNodes().values().stream()
            .sorted((n1, n2) -> n1.getUuid().getValue().compareTo(n2.getUuid().getValue()))
            .collect(Collectors.toList());
        Uuid roadmNodeUuid = new Uuid(UUID.nameUUIDFromBytes((roadmA.getNodeId().getValue() + "+PHOTONIC_MEDIA")
            .getBytes(Charset.forName("UTF-8"))).toString());
        checkOtsiNode(tapiNodes.get(1), roadmNodeUuid, "roadm", "ROADM-A1");

        List<org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.topology.Link> links
            = tapiFactory.getTapiLinks().values().stream()
            .sorted((l1, l2) -> l1.getUuid().getValue().compareTo(l2.getUuid().getValue()))
            .collect(Collectors.toList());
        Uuid node1Uuid = new Uuid(UUID.nameUUIDFromBytes("SPDR-SA1-XPDR1+OTSi".getBytes(Charset.forName("UTF-8")))
            .toString());
        Uuid node2Uuid = new Uuid(UUID.nameUUIDFromBytes("ROADM-A1+PHOTONIC_MEDIA".getBytes(Charset.forName("UTF-8")))
            .toString());
        Uuid tp1Uuid = new Uuid(UUID.nameUUIDFromBytes("SPDR-SA1-XPDR1+PHOTONIC_MEDIA+XPDR1-NETWORK1"
            .getBytes(Charset.forName("UTF-8"))).toString());
        Uuid tp2Uuid = new Uuid(UUID.nameUUIDFromBytes(("ROADM-A1+PHOTONIC_MEDIA+SRG1-PP2-TXRX")
            .getBytes(Charset.forName("UTF-8"))).toString());
        Uuid linkUuid =
            new Uuid(UUID.nameUUIDFromBytes(
                "ROADM-A1+PHOTONIC_MEDIA+SRG1-PP2-TXRXtoSPDR-SA1-XPDR1+PHOTONIC_MEDIA+XPDR1-NETWORK1"
                    .getBytes(Charset.forName("UTF-8"))).toString());
        checkXpdrRdmLink(links.get(1), node1Uuid, node2Uuid, tp1Uuid, tp2Uuid, linkUuid,
            "ROADM-A1+PHOTONIC_MEDIA+SRG1-PP2-TXRXtoSPDR-SA1-XPDR1+PHOTONIC_MEDIA+XPDR1-NETWORK1");
    }

    private void checkDsrNode(org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.topology.Node node,
            Uuid nodeUuid, String dsrNodeType, String nodeId) {
        assertEquals(nodeUuid, node.getUuid(), "incorrect node uuid");
        assertEquals(nodeId + "+DSR", node.getName().get(new NameKey("dsr/odu node name")).getValue(),
            "incorrect node name");
        assertEquals(AdministrativeState.UNLOCKED, node.getAdministrativeState(),
            "administrative state should be UNLOCKED");
        assertEquals(LifecycleState.INSTALLED, node.getLifecycleState(), "life-cycle state should be INSTALLED");
        assertEquals(OperationalState.ENABLED, node.getOperationalState(), "operational state should be ENABLED");
        assertThat("one value-name should be 'dsr/odu node name'",
            new ArrayList<>(node.nonnullName().keySet()), hasItem(new NameKey("dsr/odu node name")));
        assertEquals(2, node.getLayerProtocolName().size(), "dsr node should manage 2 protocol layers : dsr and odu");
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
                checkNodeRuleGroupForSwitchDSR(nrgList, client4NepUuid, enetworkNepUuid, nodeUuid);
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
                Uuid enetworkNepUuid2 = new Uuid(
                    UUID.nameUUIDFromBytes((nodeId + "+eODU+XPDR1-CLIENT3").getBytes(Charset.forName("UTF-8")))
                        .toString());
                Uuid inetworkNepUuid2 = new Uuid(
                    UUID.nameUUIDFromBytes((nodeId + "+iODU+XPDR1-NETWORK1").getBytes(Charset.forName("UTF-8")))
                        .toString());
                checkNepeODU4(enep4, enetworkNepUuid2, nodeId + "+eODU+XPDR1-CLIENT3", "eNodeEdgePoint_N", false);
                checkNepNetworkODU4(inep4, inetworkNepUuid2, nodeId + "+iODU+XPDR1-NETWORK1", "iNodeEdgePoint_N",
                    true);
                List<NodeRuleGroup> nrgList2 = node.nonnullNodeRuleGroup().values().stream()
                    .sorted((nrg1, nrg2) -> nrg1.getUuid().getValue().compareTo(nrg2.getUuid().getValue()))
                    .collect(Collectors.toList());
                checkNodeRuleGroupForMuxDSR(nrgList2, client3NepUuid, enetworkNepUuid2, nodeUuid);
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
                checkNodeRuleGroupForTpdrDSR(nrgList3, client1NepUuid, enetworkNepUuid3, nodeUuid);
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
        List<OwnedNodeEdgePoint> nepsI = null;
        List<OwnedNodeEdgePoint> nepsE = null;
        List<OwnedNodeEdgePoint> nepsP = null;
        List<OwnedNodeEdgePoint> nepsMc = null;
        List<OwnedNodeEdgePoint> nepsOtsimc = null;
        List<OwnedNodeEdgePoint> nepsPhot = null;
        if (!otsiNodeType.equals("roadm")) {
            assertEquals(nodeId + "+OTSi", node.getName().get(new NameKey("otsi node name")).getValue(),
                "incorrect node name");
            assertThat("one value-name should be 'dsr/odu node name'",
                new ArrayList<>(node.nonnullName().keySet()), hasItem(new NameKey("otsi node name")));
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
            nepsMc = node.nonnullOwnedNodeEdgePoint().values().stream()
                .filter(n -> n.getName().containsKey(new NameKey("MEDIA_CHANNELNodeEdgePoint")))
                .sorted((nep1, nep2) -> nep1.getUuid().getValue().compareTo(nep2.getUuid().getValue()))
                .collect(Collectors.toList());
            nepsOtsimc = node.nonnullOwnedNodeEdgePoint().values().stream()
                .filter(n -> n.getName().containsKey(new NameKey("OTSi_MEDIA_CHANNELNodeEdgePoint")))
                .sorted((nep1, nep2) -> nep1.getUuid().getValue().compareTo(nep2.getUuid().getValue()))
                .collect(Collectors.toList());
            nepsPhot = node.nonnullOwnedNodeEdgePoint().values().stream()
                .filter(n -> n.getName().containsKey(new NameKey("PHOTONIC_MEDIANodeEdgePoint")))
                .sorted((nep1, nep2) -> nep1.getUuid().getValue().compareTo(nep2.getUuid().getValue()))
                .collect(Collectors.toList());
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
                    UUID.nameUUIDFromBytes((nodeId + "+eOTSi+XPDR2-NETWORK2").getBytes(Charset.forName("UTF-8")))
                        .toString());
                checkNepOtsiNode(nep2, enepUuid, nodeId + "+eOTSi+XPDR2-NETWORK2", "eNodeEdgePoint", false);
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
                    UUID.nameUUIDFromBytes((nodeId + "+eOTSi+XPDR1-NETWORK1").getBytes(Charset.forName("UTF-8")))
                        .toString());
                checkNepOtsiNode(nep3, enepUuid2, nodeId + "+eOTSi+XPDR1-NETWORK1", "eNodeEdgePoint", false);
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
                    UUID.nameUUIDFromBytes((nodeId + "+eOTSi+XPDR1-NETWORK1").getBytes(Charset.forName("UTF-8")))
                        .toString());
                checkNepOtsiNode(nep5, enepUuid3, nodeId + "+eOTSi+XPDR1-NETWORK1", "eNodeEdgePoint", false);
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
                assertEquals(10, nepsMc.size(), "Roadm node should have 10 MC NEPs");
                assertEquals(10, nepsOtsimc.size(), "Roadm node should have 10 OTSiMC NEPs");
                assertEquals(10, nepsPhot.size(), "Roadm node should have 10 PHOT_MEDIA NEPs");
                // For Degree node
                OwnedNodeEdgePoint nep7 = nepsMc.get(6);
                Uuid mcnepUuid3 = new Uuid(
                    UUID.nameUUIDFromBytes((nodeId + "+MEDIA_CHANNEL+DEG1-TTP-TXRX").getBytes(Charset.forName("UTF-8")))
                        .toString());
                checkNepOtsiRdmNode(nep7, mcnepUuid3, nodeId + "+MEDIA_CHANNEL+DEG1-TTP-TXRX",
                    "MEDIA_CHANNELNodeEdgePoint", false);
                OwnedNodeEdgePoint nep8 = nepsOtsimc.get(0);
                Uuid otmcnepUuid3 = new Uuid(UUID.nameUUIDFromBytes((nodeId + "+OTSi_MEDIA_CHANNEL+DEG1-TTP-TXRX")
                    .getBytes(Charset.forName("UTF-8"))).toString());
                checkNepOtsiRdmNode(nep8, otmcnepUuid3, nodeId + "+OTSi_MEDIA_CHANNEL+DEG1-TTP-TXRX",
                    "OTSi_MEDIA_CHANNELNodeEdgePoint", false);
                OwnedNodeEdgePoint photNep3 = nepsPhot.get(3);
                Uuid pnep3Uuid = new Uuid(
                    UUID.nameUUIDFromBytes((nodeId + "+PHOTONIC_MEDIA+DEG1-TTP-TXRX")
                        .getBytes(Charset.forName("UTF-8"))).toString());
                checkNepOtsiRdmNode(photNep3, pnep3Uuid, nodeId + "+PHOTONIC_MEDIA+DEG1-TTP-TXRX",
                    "PHOTONIC_MEDIANodeEdgePoint", false);
                // For srg node
                OwnedNodeEdgePoint nep9 = nepsMc.get(0);
                Uuid mcnepUuid4 = new Uuid(UUID.nameUUIDFromBytes((nodeId + "+MEDIA_CHANNEL+SRG1-PP1-TXRX")
                    .getBytes(Charset.forName("UTF-8"))).toString());
                checkNepOtsiRdmNode(nep9, mcnepUuid4, nodeId + "+MEDIA_CHANNEL+SRG1-PP1-TXRX",
                    "MEDIA_CHANNELNodeEdgePoint", true);
                OwnedNodeEdgePoint nep10 = nepsOtsimc.get(9);
                Uuid otmcnepUuid4 = new Uuid(UUID.nameUUIDFromBytes((nodeId + "+OTSi_MEDIA_CHANNEL+SRG1-PP1-TXRX")
                    .getBytes(Charset.forName("UTF-8"))).toString());
                checkNepOtsiRdmNode(nep10, otmcnepUuid4, nodeId + "+OTSi_MEDIA_CHANNEL+SRG1-PP1-TXRX",
                    "OTSi_MEDIA_CHANNELNodeEdgePoint", false);
                OwnedNodeEdgePoint photNep4 = nepsPhot.get(4);
                Uuid pnep4Uuid = new Uuid(UUID.nameUUIDFromBytes((nodeId + "+PHOTONIC_MEDIA+SRG1-PP1-TXRX")
                    .getBytes(Charset.forName("UTF-8"))).toString());
                checkNepOtsiRdmNode(photNep4, pnep4Uuid, nodeId + "+PHOTONIC_MEDIA+SRG1-PP1-TXRX",
                    "PHOTONIC_MEDIANodeEdgePoint", false);
                List<NodeRuleGroup> nrgList4 = node.nonnullNodeRuleGroup().values().stream()
                    .sorted((nrg1, nrg2) -> nrg1.getUuid().getValue().compareTo(nrg2.getUuid().getValue()))
                    .collect(Collectors.toList());
                checkNodeRuleGroupForRdm(nrgList4, 30);
                break;
            default:
                fail();
                break;
        }
    }

    private void checkNepClient10G(OwnedNodeEdgePoint nep, Uuid nepUuid, String portName, String nepName) {
        assertEquals(nepUuid, nep.getUuid(), "bad uuid for " + portName);
        List<Name> nameList = new ArrayList<>(nep.nonnullName().values());
        Name name = nameList.get(0);
        assertEquals(portName, name.getValue(), "value of client nep should be '" + portName + "'");
        assertEquals(nepName, name.getValueName(),
            "value-name of client nep for '" + portName + "' should be '" + nepName + "'");
        assertEquals(3, nep.getSupportedCepLayerProtocolQualifierInstances()
            .size(), "Client nep should support 3 kind of cep");
        List<LAYERPROTOCOLQUALIFIER> lpql = new ArrayList<>();
        List<SupportedCepLayerProtocolQualifierInstances> lsclpqi = nep
                .getSupportedCepLayerProtocolQualifierInstances();
        for (SupportedCepLayerProtocolQualifierInstances entry : lsclpqi) {
            lpql.add(entry.getLayerProtocolQualifier());
        }
        assertEquals(3, lpql.size(), "Client nep should support 3 kind of cep");
        assertThat("client nep should support 3 kind of cep",
            lpql, hasItems(ODUTYPEODU2.VALUE, ODUTYPEODU2E.VALUE, DIGITALSIGNALTYPE10GigELAN.VALUE));

        assertEquals(LayerProtocolName.DSR, nep.getLayerProtocolName(), "client nep should be of DSR protocol type");
        checkCommonPartOfNep(nep, false);
    }

    private void checkNepeODU4(OwnedNodeEdgePoint nep, Uuid nepUuid, String portName, String nepName,
            boolean withSip) {
        assertEquals(nepUuid, nep.getUuid(), "bad uuid for " + portName);
        List<Name> nameList = new ArrayList<>(nep.nonnullName().values());
        Name name = nameList.get(0);
        assertEquals(portName, name.getValue(), "value of eODU nep should be '" + portName + "'");
        assertEquals(nepName, name.getValueName(),
            "value-name of eODU nep for '" + portName + "' should be '" + nepName + "'");
        // TODO: depending on the type of node there is one type or another
        List<LAYERPROTOCOLQUALIFIER> lpql = new ArrayList<>();
        List<SupportedCepLayerProtocolQualifierInstances> lsclpqi = nep
                .getSupportedCepLayerProtocolQualifierInstances();
        for (SupportedCepLayerProtocolQualifierInstances entry : lsclpqi) {
            lpql.add(entry.getLayerProtocolQualifier());
        }
        assertThat("eODU nep should support 1, 2 or 3 kind of cep, depending on client port",
            lpql.size(), anyOf(is(1), is(2), is(3)));
        assertTrue(
            lpql.stream().anyMatch(splc -> splc.equals(ODUTYPEODU0.VALUE)
                || splc.equals(ODUTYPEODU2.VALUE) || splc.equals(ODUTYPEODU2E.VALUE)
                || splc.equals(ODUTYPEODU4.VALUE)),
            "eODU nep should support 1 kind of cep");
        assertEquals(LayerProtocolName.ODU, nep.getLayerProtocolName(), "eODU nep should be of ODU protocol type");
        checkCommonPartOfNep(nep, withSip);
    }

    private void checkNepNetworkODU4(OwnedNodeEdgePoint nep, Uuid nepUuid, String portName, String nepName,
            boolean withSip) {
        assertEquals(nepUuid, nep.getUuid(), "bad uuid for " + portName);
        List<Name> nameList = new ArrayList<>(nep.nonnullName().values());
        Name name = nameList.get(0);
        assertEquals(portName, name.getValue(), "value of network nep should be '" + portName + "'");
        assertEquals(nepName, name.getValueName(),
            "value-name of network nep for '" + portName + "' should be '" + nepName + "'");
        List<LAYERPROTOCOLQUALIFIER> lpql = new ArrayList<>();
        List<SupportedCepLayerProtocolQualifierInstances> lsclpqi = nep
                .getSupportedCepLayerProtocolQualifierInstances();
        for (SupportedCepLayerProtocolQualifierInstances entry : lsclpqi) {
            lpql.add(entry.getLayerProtocolQualifier());
        }
        assertEquals(1, lpql.size(), "Network nep should support 1 kind of cep");
        assertThat("network nep should support 1 kind of cep", lpql, hasItem(ODUTYPEODU4.VALUE));
        assertEquals(LayerProtocolName.ODU, nep.getLayerProtocolName(), "network nep should be of ODU protocol type");
        checkCommonPartOfNep(nep, withSip);
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
        assertEquals(FORWARDINGRULEMAYFORWARDACROSSGROUP.VALUE, rule.get(0).getForwardingRule(),
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
        List<NodeEdgePoint> nodeEdgePointList = new ArrayList<>(nrgList.get(1).nonnullNodeEdgePoint().values());
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
        assertEquals(clientNepUuid, nrg.get(3).getNodeEdgePointUuid(),
            "in the sorted node-rule-group, nep number 4 should be XPDR2-CLIENT4");
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
        assertEquals(RuleType.FORWARDING, ruleList.get(0).getRuleType(), "the rule type should be 'FORWARDING'");
    }

    private void checkNodeRuleGroupForRdm(List<NodeRuleGroup> nrgList, int nbNeps) {
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
        assertEquals(FORWARDINGRULEMAYFORWARDACROSSGROUP.VALUE, ruleList.get(0).getForwardingRule(),
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
        assertEquals(FORWARDINGRULEMAYFORWARDACROSSGROUP.VALUE, ruleList.get(0).getForwardingRule(),
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
        assertEquals(FORWARDINGRULEMAYFORWARDACROSSGROUP.VALUE, ruleList.get(0).getForwardingRule(),
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
        assertEquals("forward", ruleList0.get(0).getLocalId(), "local-id of the rule should be 'forward'");
        assertEquals(FORWARDINGRULEMAYFORWARDACROSSGROUP.VALUE, ruleList0.get(0).getForwardingRule(),
            "the forwarding rule should be 'MAYFORWARDACROSSGROUP'");
        assertEquals(RuleType.FORWARDING, ruleList0.get(0).getRuleType(), "the rule type should be 'FORWARDING'");
    }

    private void checkNepClient100GSwitch(OwnedNodeEdgePoint nep, Uuid nepUuid, String portName, String nepName) {
        assertEquals(nepUuid, nep.getUuid(), "bad uuid for " + portName);
        List<Name> nameList = new ArrayList<>(nep.nonnullName().values());
        assertEquals(portName, nameList.get(0).getValue(),
            "value of client nep should be '" + portName + "'");
        assertEquals(nepName, nameList.get(0).getValueName(),
            "value-name of client nep for '" + portName + "' should be '" + nepName + "'");
        List<LAYERPROTOCOLQUALIFIER> lpql = new ArrayList<>();
        List<SupportedCepLayerProtocolQualifierInstances> lsclpqi = nep
                .getSupportedCepLayerProtocolQualifierInstances();
        for (SupportedCepLayerProtocolQualifierInstances entry : lsclpqi) {
            lpql.add(entry.getLayerProtocolQualifier());
        }
        assertEquals(2, lpql.size(), "Client nep should support 2 kind of cep");
        assertThat("client nep should support 2 kind of cep", lpql,
            hasItems(ODUTYPEODU4.VALUE, DIGITALSIGNALTYPE100GigE.VALUE));
        assertEquals(LayerProtocolName.DSR, nep.getLayerProtocolName(), "client nep should be of DSR protocol type");
        checkCommonPartOfNep(nep, false);
    }

    private void checkNepClient100GTpdr(OwnedNodeEdgePoint nep, Uuid nepUuid, String portName, String nepName) {
        assertEquals(nepUuid, nep.getUuid(), "bad uuid for " + portName);
        List<Name> nameList = new ArrayList<>(nep.nonnullName().values());
        assertEquals(portName, nameList.get(0).getValue(), "value of client nep should be '" + portName + "'");
        assertEquals(nepName, nameList.get(0).getValueName(),
            "value-name of client nep for '" + portName + "' should be 100G-tpdr'");
        List<LAYERPROTOCOLQUALIFIER> lpql = new ArrayList<>();
        List<SupportedCepLayerProtocolQualifierInstances> lsclpqi = nep
                .getSupportedCepLayerProtocolQualifierInstances();
        for (SupportedCepLayerProtocolQualifierInstances entry : lsclpqi) {
            lpql.add(entry.getLayerProtocolQualifier());
        }
        assertEquals(1, lpql.size(), "Client nep should support 1 kind of cep");
        assertThat("client nep should support 2 kind of cep", lpql, hasItems(DIGITALSIGNALTYPE100GigE.VALUE));
        assertEquals(LayerProtocolName.DSR, nep.getLayerProtocolName(), "client nep should be of DSR protocol type");
        checkCommonPartOfNep(nep, false);
    }

    private void checkNepOtsiNode(OwnedNodeEdgePoint nep, Uuid nepUuid, String portName, String nepName,
            boolean withSip) {
        assertEquals(nepUuid, nep.getUuid(), "bad uuid for " + portName);
        List<Name> nameList = new ArrayList<>(nep.nonnullName().values());
        assertEquals(portName, nameList.get(0).getValue(), "value of OTSi nep should be '" + portName + "'");
        assertEquals(nepName, nameList.get(0).getValueName(), "value-name of OTSi nep should be '" + nepName + "'");
        List<LAYERPROTOCOLQUALIFIER> lpql = new ArrayList<>();
        List<SupportedCepLayerProtocolQualifierInstances> lsclpqi = nep
                .getSupportedCepLayerProtocolQualifierInstances();
        for (SupportedCepLayerProtocolQualifierInstances entry : lsclpqi) {
            lpql.add(entry.getLayerProtocolQualifier());
        }
        assertEquals(2, lpql.size(), "OTSi nep should support 2 kind of cep");
        assertThat("OTSi nep should support 2 kind of cep",
            lpql, hasItems(PHOTONICLAYERQUALIFIEROMS.VALUE, PHOTONICLAYERQUALIFIEROTSi.VALUE));
        assertEquals(LayerProtocolName.PHOTONICMEDIA, nep.getLayerProtocolName(),
            "OTSi nep should be of PHOTONIC_MEDIA protocol type");
        checkCommonPartOfNep(nep, withSip);
    }

    private void checkNepOtsiRdmNode(OwnedNodeEdgePoint nep, Uuid nepUuid, String portName, String nepName,
            boolean withSip) {
        assertEquals(nepUuid, nep.getUuid(), "bad uuid for " + portName);
        List<Name> nameList = new ArrayList<>(nep.nonnullName().values());
        assertEquals(portName, nameList.get(0).getValue(),
            "value of OTSi nep should be '" + portName + "'");
        assertEquals(nepName, nameList.get(0).getValueName(),
            "value-name of OTSi nep should be '" + nepName + "'");
        List<LAYERPROTOCOLQUALIFIER> lpql = new ArrayList<>();
        List<SupportedCepLayerProtocolQualifierInstances> lsclpqi = nep
                .getSupportedCepLayerProtocolQualifierInstances();
        for (SupportedCepLayerProtocolQualifierInstances entry : lsclpqi) {
            lpql.add(entry.getLayerProtocolQualifier());
        }
        assertEquals(1, lpql.size(), "OTSi nep of RDM infra node should support only 1 kind of cep");
        assertThat("OTSi nep should support 2 kind of cep", lpql, hasItems(PHOTONICLAYERQUALIFIEROMS.VALUE));
        assertEquals(LayerProtocolName.PHOTONICMEDIA, nep.getLayerProtocolName(),
            "OTSi nep should be of PHOTONIC_MEDIA protocol type");
        checkCommonPartOfNep(nep, withSip);
    }

    private void checkCommonPartOfNep(OwnedNodeEdgePoint nep, boolean withSip) {
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

    private void checkTransitionalLink(
            org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.topology.Link link, Uuid node1Uuid,
            Uuid node2Uuid, String tp1, String tp2, String ietfNodeId) {
        Uuid linkUuid = new Uuid(UUID.nameUUIDFromBytes((tp1 + "to" + tp2).getBytes(Charset.forName("UTF-8")))
            .toString());
        assertEquals(linkUuid, link.getUuid(), "bad uuid for link between DSR node " + tp1 + " and iOTSI port " + tp2);
        assertEquals(CAPACITYUNITGBPS.VALUE, link.getAvailableCapacity().getTotalSize().getUnit(),
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
        List<org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121
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

    private void checkOmsLink(org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.topology.Link link,
            Uuid node1Uuid, Uuid node2Uuid, Uuid tp1Uuid, Uuid tp2Uuid, Uuid linkUuid, String linkName) {
        assertEquals(linkName, link.getName().get(new NameKey("OMS link name")).getValue(), "bad name for the link");
        assertEquals(linkUuid, link.getUuid(), "bad uuid for link");
        assertEquals(
            LayerProtocolName.PHOTONICMEDIA.getName(),
            link.getLayerProtocolName().stream().findFirst().orElseThrow().getName(),
            "oms link should be between 2 nodes of protocol layers PHOTONIC_MEDIA");
        assertEquals(ForwardingDirection.BIDIRECTIONAL, link.getDirection(),"otn tapi link should be BIDIRECTIONAL");
        List<org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121
            .link.NodeEdgePoint> nodeEdgePointList = new ArrayList<>(link.nonnullNodeEdgePoint().values());
        assertEquals(2 , nodeEdgePointList.size(), "oms link should be between 2 neps");
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
            nodeEdgePointList.get(1).getNodeEdgePointUuid().getValue(),
            either(containsString(tp1Uuid.getValue())).or(containsString(tp2Uuid.getValue())));
        assertThat("oms links should terminate on two distinct tps",
            nodeEdgePointList.get(0).getNodeEdgePointUuid().getValue(),
            either(containsString(tp1Uuid.getValue())).or(containsString(tp2Uuid.getValue())));
    }

    private void checkXpdrRdmLink(
            org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.topology.Link link, Uuid node1Uuid,
            Uuid node2Uuid, Uuid tp1Uuid, Uuid tp2Uuid, Uuid linkUuid, String linkName) {
        assertEquals(linkName, link.getName().get(new NameKey("XPDR-RDM link name")).getValue(),
            "bad name for the link");
        assertEquals(linkUuid, link.getUuid(), "bad uuid for link");
        assertEquals(
            LayerProtocolName.PHOTONICMEDIA.getName(),
            link.getLayerProtocolName().stream().findFirst().orElseThrow().getName(),
            "oms link should be between 2 nodes of protocol layers PHOTONIC_MEDIA");
        assertEquals(ForwardingDirection.BIDIRECTIONAL, link.getDirection(),
            "otn tapi link should be BIDIRECTIONAL");
        List<org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121
            .link.NodeEdgePoint> nodeEdgePointList = new ArrayList<>(link.nonnullNodeEdgePoint().values());
        assertEquals(2 , nodeEdgePointList.size(), "oms link should be between 2 neps");
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

    private Node changeTerminationPointState(Node initialNode, String tpid, String tpid1, AdminStates admin,
                                             State oper) {
        org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.Node1Builder tpdr1Bldr
            = new org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.Node1Builder(
                initialNode.augmentation(Node1.class));
        Map<TerminationPointKey, TerminationPoint> tps = new HashMap<>(tpdr1Bldr.getTerminationPoint());
        TerminationPointBuilder tpBldr = new TerminationPointBuilder(
            tps.get(new TerminationPointKey(new TpId(tpid))));
        tpBldr.addAugmentation(new TerminationPoint1Builder(tpBldr.augmentation(TerminationPoint1.class))
            .setAdministrativeState(admin)
            .setOperationalState(oper)
            .build());
        tps.replace(tpBldr.key(), tpBldr.build());
        TerminationPointBuilder tpBldr1 = new TerminationPointBuilder(
            tps.get(new TerminationPointKey(new TpId(tpid1))));
        tpBldr1.addAugmentation(new TerminationPoint1Builder(tpBldr1.augmentation(TerminationPoint1.class))
            .setAdministrativeState(admin)
            .setOperationalState(oper)
            .build());
        tps.replace(tpBldr1.key(), tpBldr1.build());
        tpdr1Bldr.setTerminationPoint(tps);
        return new NodeBuilder(initialNode).addAugmentation(tpdr1Bldr.build()).build();
    }
}