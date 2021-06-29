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
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

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
import org.junit.BeforeClass;
import org.junit.Test;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.transportpce.common.InstanceIdentifiers;
import org.opendaylight.transportpce.tapi.utils.TapiTopologyDataUtils;
import org.opendaylight.transportpce.test.AbstractTest;
import org.opendaylight.transportpce.test.utils.TopologyDataUtils;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev200529.Link1;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev200529.Link1Builder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev200529.TerminationPoint1;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev200529.TerminationPoint1Builder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.state.types.rev191129.State;
import org.opendaylight.yang.gen.v1.http.org.openroadm.equipment.states.types.rev191129.AdminStates;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.types.rev200529.OpenroadmLinkType;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.types.rev200529.OpenroadmTpType;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.NetworkId;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.Networks;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.NodeId;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.networks.Network;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.networks.NetworkKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.networks.network.Node;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.networks.network.NodeBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.networks.network.NodeKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.LinkId;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.Network1;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.Node1;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.TpId;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.networks.network.Link;
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
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.node.rule.group.NodeEdgePoint;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.node.rule.group.Rule;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.KeyedInstanceIdentifier;
import org.opendaylight.yangtools.yang.common.Uint64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConvertORTopoToFullTapiTopoTest extends AbstractTest {
    private static final Logger LOG = LoggerFactory.getLogger(ConvertORTopoToFullTapiTopoTest.class);

    private static Node otnMuxA;
    private static Node otnMuxC;
    private static Node otnSwitch;
    private static Node tpdr100G;
    private static Node roadmA;
    private static Node roadmC;
    private static Network openroadmNet;
    private static Map<LinkKey, org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226
        .networks.network.Link> otnLinks;
    private static Map<LinkKey, org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226
        .networks.network.Link> ortopoLinks;
    private static Uuid topologyUuid;
    private static DataBroker dataBroker = getDataBroker();

    @BeforeClass
    public static void setUp() throws InterruptedException, ExecutionException {
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
        otnMuxA = muxAFuture.get().get();
        KeyedInstanceIdentifier<Node, NodeKey> muxCIID = InstanceIdentifier.create(Networks.class)
            .child(org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.networks.Network
                .class, new NetworkKey(new NetworkId("otn-topology")))
            .child(Node.class, new NodeKey(new NodeId("SPDR-SC1-XPDR1")));
        FluentFuture<Optional<Node>> muxCFuture = dataBroker.newReadOnlyTransaction()
            .read(LogicalDatastoreType.CONFIGURATION, muxCIID);
        otnMuxC = muxCFuture.get().get();
        KeyedInstanceIdentifier<Node, NodeKey> switchIID = InstanceIdentifier.create(Networks.class)
            .child(org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.networks.Network
                .class, new NetworkKey(new NetworkId("otn-topology")))
            .child(Node.class, new NodeKey(new NodeId("SPDR-SA1-XPDR2")));
        FluentFuture<Optional<Node>> switchFuture = dataBroker.newReadOnlyTransaction()
            .read(LogicalDatastoreType.CONFIGURATION, switchIID);
        otnSwitch = switchFuture.get().get();
        KeyedInstanceIdentifier<Node, NodeKey> roadmaIID = InstanceIdentifier.create(Networks.class)
            .child(org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.networks.Network
                .class, new NetworkKey(new NetworkId("openroadm-network")))
            .child(Node.class, new NodeKey(new NodeId("ROADM-A1")));
        FluentFuture<Optional<Node>> roadmaFuture = dataBroker.newReadOnlyTransaction()
            .read(LogicalDatastoreType.CONFIGURATION, roadmaIID);
        roadmA = roadmaFuture.get().get();
        KeyedInstanceIdentifier<Node, NodeKey> roadmcIID = InstanceIdentifier.create(Networks.class)
            .child(org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.networks.Network
                .class, new NetworkKey(new NetworkId("openroadm-network")))
            .child(Node.class, new NodeKey(new NodeId("ROADM-C1")));
        FluentFuture<Optional<Node>> roadmcFuture = dataBroker.newReadOnlyTransaction()
            .read(LogicalDatastoreType.CONFIGURATION, roadmcIID);
        roadmC = roadmcFuture.get().get();

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

        InstanceIdentifier<Network1> links1IID = InstanceIdentifier.create(Networks.class)
            .child(org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.networks.Network
                .class, new NetworkKey(new NetworkId("openroadm-topology")))
            .augmentation(Network1.class);
        FluentFuture<Optional<Network1>> links1Future = dataBroker.newReadOnlyTransaction()
            .read(LogicalDatastoreType.CONFIGURATION, links1IID);
        ortopoLinks = links1Future.get().get().getLink();

        InstanceIdentifier<Network> ortopo1IID = InstanceIdentifier.create(Networks.class)
            .child(org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.networks.Network
                .class, new NetworkKey(new NetworkId("openroadm-topology")));
        FluentFuture<Optional<Network>> ortopoFuture = dataBroker.newReadOnlyTransaction()
            .read(LogicalDatastoreType.CONFIGURATION, ortopo1IID);
        openroadmNet = ortopoFuture.get().get();

        topologyUuid = new Uuid(UUID.nameUUIDFromBytes(TopologyUtils.T0_FULL_MULTILAYER.getBytes(
            Charset.forName("UTF-8"))).toString());
        LOG.info("TEST SETUP READY");
    }

    @Test
    public void convertNodeWhenNoStates() {
        Node tpdr = changeTerminationPointState(tpdr100G, "XPDR1-NETWORK1", null, null);
        List<String> networkPortList = new ArrayList<>();
        for (TerminationPoint tp : tpdr100G.augmentation(Node1.class).getTerminationPoint().values()) {
            if (tp.augmentation(TerminationPoint1.class).getTpType().equals(OpenroadmTpType.XPONDERNETWORK)) {
                networkPortList.add(tp.getTpId().getValue());
            }
        }
        ConvertORTopoToTapiFullTopo tapiFactory = new ConvertORTopoToTapiFullTopo(topologyUuid);
        tapiFactory.convertNode(tpdr, networkPortList);

        Uuid dsrNodeUuid = new Uuid(UUID.nameUUIDFromBytes("XPDR-A1-XPDR1+DSR".getBytes(Charset.forName("UTF-8")))
            .toString());
        org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.topology.Node dsrNode = tapiFactory
            .getTapiNodes().get(new
                org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.topology.NodeKey(dsrNodeUuid));
        Uuid enetworkNepUuid = new Uuid(
            UUID.nameUUIDFromBytes(("XPDR-A1-XPDR1+eODU+XPDR1-NETWORK1").getBytes(Charset.forName("UTF-8")))
                .toString());
        Uuid inetworkNepUuid = new Uuid(
            UUID.nameUUIDFromBytes(("XPDR-A1-XPDR1+iODU+XPDR1-NETWORK1").getBytes(Charset.forName("UTF-8")))
                .toString());
        OwnedNodeEdgePoint enepN = dsrNode.nonnullOwnedNodeEdgePoint().get(new OwnedNodeEdgePointKey(enetworkNepUuid));
        assertNull("Administrative State should not be present", enepN.getAdministrativeState());
        assertNull("Operational State should not be present", enepN.getOperationalState());

        OwnedNodeEdgePoint inepN = dsrNode.nonnullOwnedNodeEdgePoint().get(new OwnedNodeEdgePointKey(inetworkNepUuid));
        assertNull("Administrative State should not be present", inepN.getAdministrativeState());
        assertNull("Operational State should not be present", inepN.getOperationalState());

        Uuid otsiNodeUuid = new Uuid(UUID.nameUUIDFromBytes("XPDR-A1-XPDR1+OTSi".getBytes(Charset.forName("UTF-8")))
            .toString());
        org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.topology.Node otsiNode = tapiFactory
            .getTapiNodes().get(new
                org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.topology.NodeKey(otsiNodeUuid));
        Uuid enepUuid = new Uuid(
            UUID.nameUUIDFromBytes(("XPDR-A1-XPDR1+eOTSi+XPDR1-NETWORK1").getBytes(Charset.forName("UTF-8")))
                .toString());
        OwnedNodeEdgePoint enep = otsiNode.nonnullOwnedNodeEdgePoint().get(new OwnedNodeEdgePointKey(enepUuid));
        assertNull("Administrative State should not be present", enep.getAdministrativeState());
        assertNull("Operational State should not be present", enep.getOperationalState());

        Uuid inepUuid = new Uuid(
            UUID.nameUUIDFromBytes(("XPDR-A1-XPDR1+iOTSi+XPDR1-NETWORK1").getBytes(Charset.forName("UTF-8")))
                .toString());
        OwnedNodeEdgePoint inep = otsiNode.nonnullOwnedNodeEdgePoint().get(new OwnedNodeEdgePointKey(inepUuid));
        assertNull("Administrative State should not be present", inep.getAdministrativeState());
        assertNull("Operational State should not be present", inep.getOperationalState());

        Uuid photnepUuid = new Uuid(
            UUID.nameUUIDFromBytes(("XPDR-A1-XPDR1+PHOTONIC_MEDIA+XPDR1-NETWORK1").getBytes(Charset.forName("UTF-8")))
                .toString());
        OwnedNodeEdgePoint photnep = otsiNode.nonnullOwnedNodeEdgePoint().get(new OwnedNodeEdgePointKey(photnepUuid));
        assertNull("Administrative State should not be present", photnep.getAdministrativeState());
        assertNull("Operational State should not be present", photnep.getOperationalState());
    }

    @Test
    public void convertNodeWhenBadStates1() {
        Node tpdr = changeTerminationPointState(tpdr100G, "XPDR1-NETWORK1", AdminStates.OutOfService,
            State.OutOfService);
        List<String> networkPortList = new ArrayList<>();
        for (TerminationPoint tp : tpdr100G.augmentation(Node1.class).getTerminationPoint().values()) {
            if (tp.augmentation(TerminationPoint1.class).getTpType().equals(OpenroadmTpType.XPONDERNETWORK)) {
                networkPortList.add(tp.getTpId().getValue());
            }
        }
        ConvertORTopoToTapiFullTopo tapiFactory = new ConvertORTopoToTapiFullTopo(topologyUuid);
        tapiFactory.convertNode(tpdr, networkPortList);

        Uuid dsrNodeUuid = new Uuid(UUID.nameUUIDFromBytes("XPDR-A1-XPDR1+DSR".getBytes(Charset.forName("UTF-8")))
            .toString());
        org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.topology.Node dsrNode = tapiFactory
            .getTapiNodes().get(new
                org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.topology.NodeKey(dsrNodeUuid));
        Uuid enetworkNepUuid = new Uuid(
            UUID.nameUUIDFromBytes(("XPDR-A1-XPDR1+eODU+XPDR1-NETWORK1").getBytes(Charset.forName("UTF-8")))
                .toString());
        Uuid inetworkNepUuid = new Uuid(
            UUID.nameUUIDFromBytes(("XPDR-A1-XPDR1+iODU+XPDR1-NETWORK1").getBytes(Charset.forName("UTF-8")))
                .toString());
        OwnedNodeEdgePoint enepN = dsrNode.nonnullOwnedNodeEdgePoint().get(new OwnedNodeEdgePointKey(enetworkNepUuid));
        assertEquals("Administrative State should be Locked",
            AdministrativeState.LOCKED, enepN.getAdministrativeState());
        assertEquals("Operational State should be Disabled",
            OperationalState.DISABLED, enepN.getOperationalState());

        OwnedNodeEdgePoint inepN = dsrNode.nonnullOwnedNodeEdgePoint().get(new OwnedNodeEdgePointKey(inetworkNepUuid));
        assertEquals("Administrative State should be Locked",
            AdministrativeState.LOCKED, inepN.getAdministrativeState());
        assertEquals("Operational State should be Disabled",
            OperationalState.DISABLED, inepN.getOperationalState());

        Uuid otsiNodeUuid = new Uuid(UUID.nameUUIDFromBytes("XPDR-A1-XPDR1+OTSi".getBytes(Charset.forName("UTF-8")))
            .toString());
        org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.topology.Node otsiNode = tapiFactory
            .getTapiNodes().get(new
                org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.topology.NodeKey(otsiNodeUuid));
        Uuid enepUuid = new Uuid(
            UUID.nameUUIDFromBytes(("XPDR-A1-XPDR1+eOTSi+XPDR1-NETWORK1").getBytes(Charset.forName("UTF-8")))
                .toString());
        OwnedNodeEdgePoint enep = otsiNode.nonnullOwnedNodeEdgePoint().get(new OwnedNodeEdgePointKey(enepUuid));
        assertEquals("Administrative State should be Locked",
            AdministrativeState.LOCKED, enep.getAdministrativeState());
        assertEquals("Operational State should be Disabled",
            OperationalState.DISABLED, enep.getOperationalState());

        Uuid inepUuid = new Uuid(
            UUID.nameUUIDFromBytes(("XPDR-A1-XPDR1+iOTSi+XPDR1-NETWORK1").getBytes(Charset.forName("UTF-8")))
                .toString());
        OwnedNodeEdgePoint inep = otsiNode.nonnullOwnedNodeEdgePoint().get(new OwnedNodeEdgePointKey(inepUuid));
        assertEquals("Administrative State should be Locked",
            AdministrativeState.LOCKED, inep.getAdministrativeState());
        assertEquals("Operational State should be Disabled",
            OperationalState.DISABLED, inep.getOperationalState());

        Uuid photnepUuid = new Uuid(
            UUID.nameUUIDFromBytes(("XPDR-A1-XPDR1+PHOTONIC_MEDIA+XPDR1-NETWORK1").getBytes(Charset.forName("UTF-8")))
                .toString());
        OwnedNodeEdgePoint photnep = otsiNode.nonnullOwnedNodeEdgePoint().get(new OwnedNodeEdgePointKey(photnepUuid));
        assertEquals("Administrative State should be Locked",
            AdministrativeState.LOCKED, photnep.getAdministrativeState());
        assertEquals("Operational State should be Disabled",
            OperationalState.DISABLED, photnep.getOperationalState());
    }

    @Test
    public void convertNodeWhenBadStates2() {
        Node tpdr = changeTerminationPointState(tpdr100G, "XPDR1-NETWORK1", AdminStates.Maintenance,
            State.Degraded);
        List<String> networkPortList = new ArrayList<>();
        for (TerminationPoint tp : tpdr100G.augmentation(Node1.class).getTerminationPoint().values()) {
            if (tp.augmentation(TerminationPoint1.class).getTpType().equals(OpenroadmTpType.XPONDERNETWORK)) {
                networkPortList.add(tp.getTpId().getValue());
            }
        }
        ConvertORTopoToTapiFullTopo tapiFactory = new ConvertORTopoToTapiFullTopo(topologyUuid);
        tapiFactory.convertNode(tpdr, networkPortList);

        Uuid dsrNodeUuid = new Uuid(UUID.nameUUIDFromBytes("XPDR-A1-XPDR1+DSR".getBytes(Charset.forName("UTF-8")))
            .toString());
        org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.topology.Node dsrNode = tapiFactory
            .getTapiNodes().get(new
                org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.topology.NodeKey(dsrNodeUuid));
        Uuid enetworkNepUuid = new Uuid(
            UUID.nameUUIDFromBytes(("XPDR-A1-XPDR1+eODU+XPDR1-NETWORK1").getBytes(Charset.forName("UTF-8")))
                .toString());
        Uuid inetworkNepUuid = new Uuid(
            UUID.nameUUIDFromBytes(("XPDR-A1-XPDR1+iODU+XPDR1-NETWORK1").getBytes(Charset.forName("UTF-8")))
                .toString());
        OwnedNodeEdgePoint enepN = dsrNode.nonnullOwnedNodeEdgePoint().get(new OwnedNodeEdgePointKey(enetworkNepUuid));
        assertEquals("Administrative State should be Locked",
            AdministrativeState.LOCKED, enepN.getAdministrativeState());
        assertEquals("Operational State should be Disabled",
            OperationalState.DISABLED, enepN.getOperationalState());

        OwnedNodeEdgePoint inepN = dsrNode.nonnullOwnedNodeEdgePoint().get(new OwnedNodeEdgePointKey(inetworkNepUuid));
        assertEquals("Administrative State should be Locked",
            AdministrativeState.LOCKED, inepN.getAdministrativeState());
        assertEquals("Operational State should be Disabled",
            OperationalState.DISABLED, inepN.getOperationalState());

        Uuid otsiNodeUuid = new Uuid(UUID.nameUUIDFromBytes("XPDR-A1-XPDR1+OTSi".getBytes(Charset.forName("UTF-8")))
            .toString());
        org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.topology.Node otsiNode = tapiFactory
            .getTapiNodes().get(new
                org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.topology.NodeKey(otsiNodeUuid));
        Uuid enepUuid = new Uuid(
            UUID.nameUUIDFromBytes(("XPDR-A1-XPDR1+eOTSi+XPDR1-NETWORK1").getBytes(Charset.forName("UTF-8")))
                .toString());
        OwnedNodeEdgePoint enep = otsiNode.nonnullOwnedNodeEdgePoint().get(new OwnedNodeEdgePointKey(enepUuid));
        assertEquals("Administrative State should be Locked",
            AdministrativeState.LOCKED, enep.getAdministrativeState());
        assertEquals("Operational State should be Disabled",
            OperationalState.DISABLED, enep.getOperationalState());

        Uuid inepUuid = new Uuid(
            UUID.nameUUIDFromBytes(("XPDR-A1-XPDR1+iOTSi+XPDR1-NETWORK1").getBytes(Charset.forName("UTF-8")))
                .toString());
        OwnedNodeEdgePoint inep = otsiNode.nonnullOwnedNodeEdgePoint().get(new OwnedNodeEdgePointKey(inepUuid));
        assertEquals("Administrative State should be Locked",
            AdministrativeState.LOCKED, inep.getAdministrativeState());
        assertEquals("Operational State should be Disabled",
            OperationalState.DISABLED, inep.getOperationalState());

        Uuid photnepUuid = new Uuid(
            UUID.nameUUIDFromBytes(("XPDR-A1-XPDR1+PHOTONIC_MEDIA+XPDR1-NETWORK1").getBytes(Charset.forName("UTF-8")))
                .toString());
        OwnedNodeEdgePoint photnep = otsiNode.nonnullOwnedNodeEdgePoint().get(new OwnedNodeEdgePointKey(photnepUuid));
        assertEquals("Administrative State should be Locked",
            AdministrativeState.LOCKED, photnep.getAdministrativeState());
        assertEquals("Operational State should be Disabled",
            OperationalState.DISABLED, photnep.getOperationalState());
    }

    @Test
    public void convertOtnLinkWhenNoState() {
        HashMap<LinkKey, org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226
            .networks.network.Link> otnLinksAlt = new HashMap<>(otnLinks);
        org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.networks.network.Link
            link = changeOtnLinkState(otnLinks.get(new LinkKey(
                new LinkId("ODU4-SPDR-SA1-XPDR1-XPDR1-NETWORK1toSPDR-SC1-XPDR1-XPDR1-NETWORK1"))), null, null);
        otnLinksAlt.replace(link.key(), link);

        ConvertORTopoToTapiFullTopo tapiFactory = new ConvertORTopoToTapiFullTopo(topologyUuid);
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

        List<org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.topology.Link> tapiLinks
            = tapiFactory.getTapiLinks().values().stream()
            .sorted((l1, l2) -> l1.getUuid().getValue().compareTo(l2.getUuid().getValue()))
            .collect(Collectors.toList());
        assertNull("Administrative State should not be present", tapiLinks.get(2).getAdministrativeState());
        assertEquals("Administrative state should be UNLOCKED",
            AdministrativeState.UNLOCKED, tapiLinks.get(0).getAdministrativeState());
        assertNull("Operational State should not be present", tapiLinks.get(2).getOperationalState());
        assertEquals("Operational state should be ENABLED",
            OperationalState.ENABLED, tapiLinks.get(0).getOperationalState());
    }

    @Test
    public void convertOtnLinkWhenNoStateOnOppositeLink() {
        HashMap<LinkKey, org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226
            .networks.network.Link> otnLinksAlt = new HashMap<>(otnLinks);
        org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.networks.network.Link
            link = changeOtnLinkState(otnLinks.get(new LinkKey(
                new LinkId("ODU4-SPDR-SC1-XPDR1-XPDR1-NETWORK1toSPDR-SA1-XPDR1-XPDR1-NETWORK1"))), null, null);
        otnLinksAlt.replace(link.key(), link);

        ConvertORTopoToTapiFullTopo tapiFactory = new ConvertORTopoToTapiFullTopo(topologyUuid);
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

        List<org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.topology.Link> tapiLinks
            = tapiFactory.getTapiLinks().values().stream()
            .sorted((l1, l2) -> l1.getUuid().getValue().compareTo(l2.getUuid().getValue()))
            .collect(Collectors.toList());
        assertNull("Administrative State should not be present", tapiLinks.get(2).getAdministrativeState());
        assertEquals("Administrative state should be UNLOCKED",
            AdministrativeState.UNLOCKED, tapiLinks.get(0).getAdministrativeState());
        assertNull("Operational State should not be present", tapiLinks.get(2).getOperationalState());
        assertEquals("Operational state should be ENABLED",
            OperationalState.ENABLED, tapiLinks.get(0).getOperationalState());
    }

    @Test
    public void convertOtnLinkWhenBadState1() {
        HashMap<LinkKey, org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226
            .networks.network.Link> otnLinksAlt = new HashMap<>(otnLinks);
        org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.networks.network.Link
            link = changeOtnLinkState(otnLinks.get(new LinkKey(
                new LinkId("ODU4-SPDR-SA1-XPDR1-XPDR1-NETWORK1toSPDR-SC1-XPDR1-XPDR1-NETWORK1"))),
            AdminStates.OutOfService, State.OutOfService);
        otnLinksAlt.replace(link.key(), link);

        ConvertORTopoToTapiFullTopo tapiFactory = new ConvertORTopoToTapiFullTopo(topologyUuid);
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

        List<org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.topology.Link> tapiLinks
            = tapiFactory.getTapiLinks().values().stream()
            .sorted((l1, l2) -> l1.getUuid().getValue().compareTo(l2.getUuid().getValue()))
            .collect(Collectors.toList());
        assertEquals("Administrative state should be LOCKED",
            AdministrativeState.LOCKED, tapiLinks.get(2).getAdministrativeState());
        assertEquals("Administrative state should be UNLOCKED",
            AdministrativeState.UNLOCKED, tapiLinks.get(0).getAdministrativeState());
        assertEquals("Operational state should be DISABLED",
            OperationalState.DISABLED, tapiLinks.get(2).getOperationalState());
        assertEquals("Operational state should be ENABLED",
            OperationalState.ENABLED, tapiLinks.get(0).getOperationalState());
    }

    @Test
    public void convertOtnLinkWhenBadState2() {
        HashMap<LinkKey, org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226
            .networks.network.Link> otnLinksAlt = new HashMap<>(otnLinks);
        org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.networks.network.Link
            link = changeOtnLinkState(otnLinks.get(new LinkKey(
                new LinkId("ODU4-SPDR-SA1-XPDR1-XPDR1-NETWORK1toSPDR-SC1-XPDR1-XPDR1-NETWORK1"))),
            AdminStates.Maintenance, State.Degraded);
        otnLinksAlt.replace(link.key(), link);

        ConvertORTopoToTapiFullTopo tapiFactory = new ConvertORTopoToTapiFullTopo(topologyUuid);
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

        List<org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.topology.Link> tapiLinks
            = tapiFactory.getTapiLinks().values().stream()
            .sorted((l1, l2) -> l1.getUuid().getValue().compareTo(l2.getUuid().getValue()))
            .collect(Collectors.toList());
        assertEquals("Administrative state should be LOCKED",
            AdministrativeState.LOCKED, tapiLinks.get(2).getAdministrativeState());
        assertEquals("Administrative state should be UNLOCKED",
            AdministrativeState.UNLOCKED, tapiLinks.get(0).getAdministrativeState());
        assertEquals("Operational state should be DISABLED",
            OperationalState.DISABLED, tapiLinks.get(2).getOperationalState());
        assertEquals("Operational state should be ENABLED",
            OperationalState.ENABLED, tapiLinks.get(0).getOperationalState());
    }

    @Test
    public void convertOtnLinkWhenBadStateOnOppositeLink() {
        HashMap<LinkKey, org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226
            .networks.network.Link> otnLinksAlt = new HashMap<>(otnLinks);
        org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.networks.network.Link
            link = changeOtnLinkState(otnLinks.get(new LinkKey(
                new LinkId("ODU4-SPDR-SC1-XPDR1-XPDR1-NETWORK1toSPDR-SA1-XPDR1-XPDR1-NETWORK1"))),
            AdminStates.OutOfService, State.OutOfService);
        otnLinksAlt.replace(link.key(), link);

        ConvertORTopoToTapiFullTopo tapiFactory = new ConvertORTopoToTapiFullTopo(topologyUuid);
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

        List<org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.topology.Link> tapiLinks
            = tapiFactory.getTapiLinks().values().stream()
            .sorted((l1, l2) -> l1.getUuid().getValue().compareTo(l2.getUuid().getValue()))
            .collect(Collectors.toList());
        assertEquals("Administrative state should be LOCKED",
            AdministrativeState.LOCKED, tapiLinks.get(2).getAdministrativeState());
        assertEquals("Administrative state should be UNLOCKED",
            AdministrativeState.UNLOCKED, tapiLinks.get(0).getAdministrativeState());
        assertEquals("Operational state should be DISABLED",
            OperationalState.DISABLED, tapiLinks.get(2).getOperationalState());
        assertEquals("Operational state should be ENABLED",
            OperationalState.ENABLED, tapiLinks.get(0).getOperationalState());
    }

    @Test
    public void convertNodeForTransponder100G() {
        ConvertORTopoToTapiFullTopo tapiFactory = new ConvertORTopoToTapiFullTopo(topologyUuid);
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

        assertEquals("Node list size should be 2", 2, tapiFactory.getTapiNodes().size());
        assertEquals("Link list size should be 2", 2, tapiFactory.getTapiLinks().size());

        Uuid dsrNodeUuid = new Uuid(UUID.nameUUIDFromBytes("XPDR-A1-XPDR1+DSR".getBytes(Charset.forName("UTF-8")))
            .toString());
        checkDsrNode(tapiNodes.get(1), dsrNodeUuid, "tpdr", "XPDR-A1-XPDR1");
        Uuid otsiNodeUuid = new Uuid(UUID.nameUUIDFromBytes("XPDR-A1-XPDR1+OTSi".getBytes(Charset.forName("UTF-8")))
            .toString());
        checkOtsiNode(tapiNodes.get(0), otsiNodeUuid, "tpdr", "XPDR-A1-XPDR1");

        List<org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.topology.Link> tapiLinks
            = tapiFactory.getTapiLinks().values().stream()
            .sorted((l1, l2) -> l1.getUuid().getValue().compareTo(l2.getUuid().getValue()))
            .collect(Collectors.toList());
        checkTransitionalLink(tapiLinks.get(1), dsrNodeUuid, otsiNodeUuid,
            "XPDR-A1-XPDR1+iODU+XPDR1-NETWORK1", "XPDR-A1-XPDR1+iOTSi+XPDR1-NETWORK1", "XPDR-A1-XPDR1");
    }

    @Test
    public void convertNodeForOtnMuxponder() {
        ConvertORTopoToTapiFullTopo tapiFactory = new ConvertORTopoToTapiFullTopo(topologyUuid);
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

        assertEquals("Node list size should be 2", 2, tapiFactory.getTapiNodes().size());
        assertEquals("Link list size should be 1", 1, tapiFactory.getTapiLinks().size());
        Uuid dsrNodeUuid = new Uuid(UUID.nameUUIDFromBytes("SPDR-SA1-XPDR1+DSR".getBytes(Charset.forName("UTF-8")))
            .toString());
        checkDsrNode(tapiNodes.get(0), dsrNodeUuid, "mux", "SPDR-SA1-XPDR1");
        Uuid otsiNodeUuid = new Uuid(UUID.nameUUIDFromBytes("SPDR-SA1-XPDR1+OTSi".getBytes(Charset.forName("UTF-8")))
            .toString());
        checkOtsiNode(tapiNodes.get(1), otsiNodeUuid, "mux", "SPDR-SA1-XPDR1");

        List<org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.topology.Link> tapiLinks
            = tapiFactory.getTapiLinks().values().stream()
            .sorted((l1, l2) -> l1.getUuid().getValue().compareTo(l2.getUuid().getValue()))
            .collect(Collectors.toList());
        checkTransitionalLink(tapiLinks.get(0), dsrNodeUuid, otsiNodeUuid,
            "SPDR-SA1-XPDR1+iODU+XPDR1-NETWORK1", "SPDR-SA1-XPDR1+iOTSi+XPDR1-NETWORK1", "SPDR-SA1-XPDR1");
    }

    @Test
    public void convertNodeForOtnSwitch() {
        ConvertORTopoToTapiFullTopo tapiFactory = new ConvertORTopoToTapiFullTopo(topologyUuid);
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

        assertEquals("Node list size should be 2", 2, tapiFactory.getTapiNodes().size());
        assertEquals("Link list size should be 4", 4, tapiFactory.getTapiLinks().size());

        Uuid dsrNodeUuid = new Uuid(UUID.nameUUIDFromBytes("SPDR-SA1-XPDR2+DSR".getBytes(Charset.forName("UTF-8")))
            .toString());
        checkDsrNode(tapiNodes.get(0), dsrNodeUuid, "switch", "SPDR-SA1-XPDR2");
        Uuid otsiNodeUuid = new Uuid(UUID.nameUUIDFromBytes("SPDR-SA1-XPDR2+OTSi".getBytes(Charset.forName("UTF-8")))
            .toString());
        checkOtsiNode(tapiNodes.get(1), otsiNodeUuid, "switch", "SPDR-SA1-XPDR2");

        List<org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.topology.Link> tapiLinks
            = tapiFactory.getTapiLinks().values().stream()
            .sorted((l1, l2) -> l1.getUuid().getValue().compareTo(l2.getUuid().getValue()))
            .collect(Collectors.toList());
        checkTransitionalLink(tapiLinks.get(1), dsrNodeUuid, otsiNodeUuid,
            "SPDR-SA1-XPDR2+iODU+XPDR2-NETWORK4", "SPDR-SA1-XPDR2+iOTSi+XPDR2-NETWORK4", "SPDR-SA1-XPDR2");
    }

    @Test
    public void convertOtnLink() {
        ConvertORTopoToTapiFullTopo tapiFactory = new ConvertORTopoToTapiFullTopo(topologyUuid);
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
        assertEquals("Link list size should be 4", 4, tapiFactory.getTapiLinks().size());

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
            new Uuid(UUID.nameUUIDFromBytes("ODU4-SPDR-SA1-XPDR1-XPDR1-NETWORK1toSPDR-SC1-XPDR1-XPDR1-NETWORK1"
                .getBytes(Charset.forName("UTF-8"))).toString());
        Uuid link2Uuid =
            new Uuid(UUID.nameUUIDFromBytes("OTU4-SPDR-SA1-XPDR1-XPDR1-NETWORK1toSPDR-SC1-XPDR1-XPDR1-NETWORK1"
                .getBytes(Charset.forName("UTF-8"))).toString());

        List<org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.topology.Link> links
            = tapiFactory.getTapiLinks().values().stream()
            .sorted((l1, l2) -> l1.getUuid().getValue().compareTo(l2.getUuid().getValue()))
            .collect(Collectors.toList());
        checkOtnLink(links.get(2), node1Uuid, node2Uuid, tp1Uuid, tp2Uuid, link1Uuid,
            "ODU4-SPDR-SA1-XPDR1-XPDR1-NETWORK1toSPDR-SC1-XPDR1-XPDR1-NETWORK1");
        checkOtnLink(links.get(3), node3Uuid, node4Uuid, tp3Uuid, tp4Uuid, link2Uuid,
            "OTU4-SPDR-SA1-XPDR1-XPDR1-NETWORK1toSPDR-SC1-XPDR1-XPDR1-NETWORK1");
    }

    @Test
    public void convertNodeForRoadmWhenNoOtnMuxAttached() {
        ConvertORTopoToTapiFullTopo tapiFactory = new ConvertORTopoToTapiFullTopo(topologyUuid);
        tapiFactory.convertRoadmNode(roadmA, openroadmNet);

        assertEquals("Node list size should be 1", 1, tapiFactory.getTapiNodes().size());
        assertEquals("Link list size should be empty", 0, tapiFactory.getTapiLinks().size());
        List<org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.topology.Node> tapiNodes
            = tapiFactory.getTapiNodes().values().stream().collect(Collectors.toList());
        Uuid roadmNodeUuid = new Uuid(UUID.nameUUIDFromBytes((roadmA.getNodeId().getValue() + "+PHOTONIC_MEDIA")
            .getBytes(Charset.forName("UTF-8"))).toString());
        checkOtsiNode(tapiNodes.get(0), roadmNodeUuid, "roadm", "ROADM-A1");
    }

    @Test
    public void convertNodeForRoadmWhenRoadmNeighborAttached() {
        ConvertORTopoToTapiFullTopo tapiFactory = new ConvertORTopoToTapiFullTopo(topologyUuid);
        tapiFactory.convertRoadmNode(roadmA, openroadmNet);
        tapiFactory.convertRoadmNode(roadmC, openroadmNet);

        List<Link> rdmTordmLinkList = ortopoLinks.values().stream()
            .filter(lk -> lk.augmentation(Link1.class).getLinkType().equals(OpenroadmLinkType.ROADMTOROADM))
            .collect(Collectors.toList());
        tapiFactory.convertRdmToRdmLinks(rdmTordmLinkList);

        assertEquals("Node list size should be 1", 2, tapiFactory.getTapiNodes().size());
        assertEquals("Link list size should be empty", 1, tapiFactory.getTapiLinks().size());

        List<org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.topology.Node> tapiNodes
            = tapiFactory.getTapiNodes().values().stream().collect(Collectors.toList());
        Uuid roadmaNodeUuid = new Uuid(UUID.nameUUIDFromBytes((roadmA.getNodeId().getValue() + "+PHOTONIC_MEDIA")
            .getBytes(Charset.forName("UTF-8"))).toString());
        checkOtsiNode(tapiNodes.get(1), roadmaNodeUuid, "roadm", "ROADM-A1");

        List<org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.topology.Link> links
            = tapiFactory.getTapiLinks().values().stream()
            .sorted((l1, l2) -> l1.getUuid().getValue().compareTo(l2.getUuid().getValue()))
            .collect(Collectors.toList());
        LOG.info(links.toString());
        Uuid node1Uuid = new Uuid(UUID.nameUUIDFromBytes("ROADM-A1+PHOTONIC_MEDIA".getBytes(Charset.forName("UTF-8")))
            .toString());
        Uuid node2Uuid = new Uuid(UUID.nameUUIDFromBytes("ROADM-C1+PHOTONIC_MEDIA".getBytes(Charset.forName("UTF-8")))
            .toString());
        Uuid tp1Uuid = new Uuid(UUID.nameUUIDFromBytes("ROADM-A1+PHOTONIC_MEDIA+DEG2-TTP-TXRX"
            .getBytes(Charset.forName("UTF-8"))).toString());
        Uuid tp2Uuid = new Uuid(UUID.nameUUIDFromBytes(("ROADM-C1+PHOTONIC_MEDIA+DEG1-TTP-TXRX")
            .getBytes(Charset.forName("UTF-8"))).toString());
        Uuid linkUuid =
            new Uuid(UUID.nameUUIDFromBytes("ROADM-A1-DEG2-DEG2-TTP-TXRXtoROADM-C1-DEG1-DEG1-TTP-TXRX"
                .getBytes(Charset.forName("UTF-8"))).toString());
        checkOmsLink(links.get(0), node1Uuid, node2Uuid, tp1Uuid, tp2Uuid, linkUuid,
            "ROADM-A1-DEG2-DEG2-TTP-TXRXtoROADM-C1-DEG1-DEG1-TTP-TXRX");
    }

    @Test
    public void convertNodeForRoadmWhenOtnMuxAttached() {
        ConvertORTopoToTapiFullTopo tapiFactory = new ConvertORTopoToTapiFullTopo(topologyUuid);
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
        assertEquals("Node list size should be 3", 3, tapiFactory.getTapiNodes().size());
        assertEquals("Link list size should be 2", 2, tapiFactory.getTapiLinks().size());
        List<org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.topology.Node> tapiNodes
            = tapiFactory.getTapiNodes().values().stream()
            .sorted((n1, n2) -> n1.getUuid().getValue().compareTo(n2.getUuid().getValue()))
            .collect(Collectors.toList());
        Uuid roadmNodeUuid = new Uuid(UUID.nameUUIDFromBytes((roadmA.getNodeId().getValue() + "+PHOTONIC_MEDIA")
            .getBytes(Charset.forName("UTF-8"))).toString());
        LOG.info(tapiNodes.toString());
        checkOtsiNode(tapiNodes.get(1), roadmNodeUuid, "roadm", "ROADM-A1");

        List<org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.topology.Link> links
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
            new Uuid(UUID.nameUUIDFromBytes("ROADM-A1-SRG1-SRG1-PP2-TXRXtoSPDR-SA1-XPDR1-XPDR1-NETWORK1"
                .getBytes(Charset.forName("UTF-8"))).toString());
        checkXpdrRdmLink(links.get(0), node1Uuid, node2Uuid, tp1Uuid, tp2Uuid, linkUuid,
            "ROADM-A1-SRG1-SRG1-PP2-TXRXtoSPDR-SA1-XPDR1-XPDR1-NETWORK1");
    }

    private void checkDsrNode(org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.topology.Node node,
                              Uuid nodeUuid, String dsrNodeType, String nodeId) {
        assertEquals("incorrect node uuid", nodeUuid, node.getUuid());
        assertEquals("incorrect node name", nodeId + "+DSR", node.getName().get(
            new NameKey("dsr/odu node name")).getValue());
        assertEquals("administrative state should be UNLOCKED",
            AdministrativeState.UNLOCKED, node.getAdministrativeState());
        assertEquals("life-cycle state should be INSTALLED", LifecycleState.INSTALLED, node.getLifecycleState());
        assertEquals("operational state should be ENABLED", OperationalState.ENABLED, node.getOperationalState());
        assertEquals("value-name should be 'dsr/odu node name'",
            "dsr/odu node name", node.nonnullName().values().stream().findFirst().get().getValueName());
        assertEquals("dsr node should manage 2 protocol layers : dsr and odu",
            2, node.getLayerProtocolName().size());
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
                assertEquals("Switch-DSR node should have 4 eNEPs network", 4, enepsN.size());
                assertEquals("Switch-DSR node should have 4 iNEPs network", 4, inepsN.size());
                assertEquals("Switch-DSR node should have 4 NEPs client", 4, nepsC.size());
                OwnedNodeEdgePoint nep1 = nepsC.get(2);
                Uuid client4NepUuid = new Uuid(
                    UUID.nameUUIDFromBytes((nodeId + "+DSR+XPDR2-CLIENT4").getBytes(Charset.forName("UTF-8")))
                        .toString());
                checkNepClient100GSwitch(nep1, client4NepUuid, nodeId + "+DSR+XPDR2-CLIENT4", "NodeEdgePoint_C");
                OwnedNodeEdgePoint enep2 = enepsN.get(3);
                OwnedNodeEdgePoint inep2 = inepsN.get(3);
                Uuid enetworkNepUuid = new Uuid(
                    UUID.nameUUIDFromBytes((nodeId + "+eODU+XPDR2-NETWORK1").getBytes(Charset.forName("UTF-8")))
                        .toString());
                Uuid inetworkNepUuid = new Uuid(
                    UUID.nameUUIDFromBytes((nodeId + "+iODU+XPDR2-NETWORK1").getBytes(Charset.forName("UTF-8")))
                        .toString());
                checkNepNetworkODU4(enep2, enetworkNepUuid, nodeId + "+eODU+XPDR2-NETWORK1", "eNodeEdgePoint_N", true);
                checkNepNetworkODU4(inep2, inetworkNepUuid, nodeId + "+iODU+XPDR2-NETWORK1", "iNodeEdgePoint_N", false);
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
                assertEquals("Mux-DSR node should have 1 eNEP network", 1, enepsN.size());
                assertEquals("Mux-DSR node should have 1 iNEP network", 1, inepsN.size());
                assertEquals("Mux-DSR node should have 4 NEPs client", 4, nepsC.size());
                OwnedNodeEdgePoint nep3 = nepsC.get(2);
                Uuid client3NepUuid = new Uuid(
                    UUID.nameUUIDFromBytes((nodeId + "+DSR+XPDR1-CLIENT3").getBytes(Charset.forName("UTF-8")))
                        .toString());
                checkNepClient10G(nep3, client3NepUuid, nodeId + "+DSR+XPDR1-CLIENT3", "NodeEdgePoint_C");

                OwnedNodeEdgePoint enep4 = enepsN.get(0);
                OwnedNodeEdgePoint inep4 = inepsN.get(0);
                Uuid enetworkNepUuid2 = new Uuid(
                    UUID.nameUUIDFromBytes((nodeId + "+eODU+XPDR1-NETWORK1").getBytes(Charset.forName("UTF-8")))
                        .toString());
                Uuid inetworkNepUuid2 = new Uuid(
                    UUID.nameUUIDFromBytes((nodeId + "+iODU+XPDR1-NETWORK1").getBytes(Charset.forName("UTF-8")))
                        .toString());
                checkNepNetworkODU4(enep4, enetworkNepUuid2, nodeId + "+eODU+XPDR1-NETWORK1", "eNodeEdgePoint_N", true);
                checkNepNetworkODU4(inep4, inetworkNepUuid2, nodeId + "+iODU+XPDR1-NETWORK1", "iNodeEdgePoint_N",
                    false);
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
                assertEquals("Tpdr-DSR node should have 2 eNEPs network", 2, enepsN.size());
                assertEquals("Tpdr-DSR node should have 2 iNEPs network", 2, inepsN.size());
                assertEquals("Tpdr-DSR node should have 2 NEPs client", 2, nepsC.size());
                OwnedNodeEdgePoint nep5 = nepsC.get(0);
                Uuid client1NepUuid = new Uuid(
                    UUID.nameUUIDFromBytes((nodeId + "+DSR+XPDR1-CLIENT1").getBytes(Charset.forName("UTF-8")))
                        .toString());
                checkNepClient100GTpdr(nep5, client1NepUuid, nodeId + "+DSR+XPDR1-CLIENT1", "100G-tpdr");

                OwnedNodeEdgePoint enep6 = enepsN.get(0);
                OwnedNodeEdgePoint inep6 = inepsN.get(1);
                Uuid enetworkNepUuid3 = new Uuid(
                    UUID.nameUUIDFromBytes((nodeId + "+eODU+XPDR1-NETWORK1").getBytes(Charset.forName("UTF-8")))
                        .toString());
                Uuid inetworkNepUuid3 = new Uuid(
                    UUID.nameUUIDFromBytes((nodeId + "+iODU+XPDR1-NETWORK1").getBytes(Charset.forName("UTF-8")))
                        .toString());
                checkNepNetworkODU4(enep6, enetworkNepUuid3, nodeId + "+eODU+XPDR1-NETWORK1", "eNodeEdgePoint_N", true);
                checkNepNetworkODU4(inep6, inetworkNepUuid3, nodeId + "+iODU+XPDR1-NETWORK1", "iNodeEdgePoint_N",
                    false);
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
            org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.topology.Node node,
            Uuid nodeUuid, String otsiNodeType, String nodeId) {
        assertEquals("incorrect node uuid", nodeUuid, node.getUuid());
        List<OwnedNodeEdgePoint> nepsI = null;
        List<OwnedNodeEdgePoint> nepsE = null;
        List<OwnedNodeEdgePoint> nepsP = null;
        List<OwnedNodeEdgePoint> nepsMc = null;
        List<OwnedNodeEdgePoint> nepsOtsimc = null;
        List<OwnedNodeEdgePoint> nepsPhot = null;
        if (!otsiNodeType.equals("roadm")) {
            assertEquals("incorrect node name", nodeId + "+OTSi", node.getName().get(
                new NameKey("otsi node name")).getValue());
            assertEquals("value-name should be 'dsr/odu node name'",
                "otsi node name", node.nonnullName().values().stream().findFirst().get().getValueName());
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
            assertEquals("incorrect node name", nodeId + "+PHOTONIC_MEDIA", node.getName().get(
                new NameKey("roadm node name")).getValue());
            assertEquals("value-name should be 'dsr/odu node name'",
                "roadm node name", node.nonnullName().values().stream().findFirst().get().getValueName());
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
        assertEquals("administrative state should be UNLOCKED",
            AdministrativeState.UNLOCKED, node.getAdministrativeState());
        assertEquals("life-cycle state should be INSTALLED", LifecycleState.INSTALLED, node.getLifecycleState());
        assertEquals("operational state should be ENABLED", OperationalState.ENABLED, node.getOperationalState());
        assertEquals("otsi node should manage a single protocol layer : PHOTONIC_MEDIA",
            1, node.getLayerProtocolName().size());
        assertEquals("otsi node should manage a single protocol layer : PHOTONIC_MEDIA",
            LayerProtocolName.PHOTONICMEDIA, node.getLayerProtocolName().get(0));

        switch (otsiNodeType) {
            case "switch":
                assertEquals("Switch-OTSi node should have 4 eNEPs", 4, nepsE.size());
                assertEquals("Switch-OTSi node should have 4 iNEPs", 4, nepsI.size());
                assertEquals("Switch-OTSi node should have 4 photNEPs", 4, nepsP.size());
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
                assertEquals("Mux-OTSi node should have 1 eNEP", 1, nepsE.size());
                assertEquals("Mux-OTSi node should have 1 iNEPs", 1, nepsI.size());
                assertEquals("Mux-OTSi node should have 1 photNEPs", 1, nepsP.size());
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
                assertEquals("Tpdr-OTSi node should have 2 eNEPs", 2, nepsE.size());
                assertEquals("Tpdr-OTSi node should have 2 iNEPs", 2, nepsI.size());
                assertEquals("Tpdr-OTSi node should have 2 photNEPs", 2, nepsP.size());
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
                assertEquals("Roadm node should have 10 MC NEPs", 10, nepsMc.size());
                assertEquals("Roadm node should have 10 OTSiMC NEPs", 10, nepsOtsimc.size());
                assertEquals("Roadm node should have 10 PHOT_MEDIA NEPs", 10, nepsPhot.size());
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
        assertEquals("bad uuid for " + portName, nepUuid, nep.getUuid());
        List<Name> nameList = new ArrayList<>(nep.nonnullName().values());
        Name name = nameList.get(0);
        assertEquals("value of client nep should be '" + portName + "'",
            portName, name.getValue());
        assertEquals("value-name of client nep for '" + portName + "' should be '" + nepName + "'",
            nepName, name.getValueName());
        assertEquals("Client nep should support 3 kind of cep",
            3, nep.getSupportedCepLayerProtocolQualifier().size());
        assertThat("client nep should support 3 kind of cep",
            nep.getSupportedCepLayerProtocolQualifier(),
            hasItems(ODUTYPEODU2.class, ODUTYPEODU2E.class, DIGITALSIGNALTYPE10GigELAN.class));
        assertEquals("client nep should be of DSR protocol type", LayerProtocolName.DSR, nep.getLayerProtocolName());
        checkCommonPartOfNep(nep, false);
    }

    private void checkNepNetworkODU4(OwnedNodeEdgePoint nep, Uuid nepUuid, String portName, String nepName,
                                     boolean withSip) {
        assertEquals("bad uuid for " + portName, nepUuid, nep.getUuid());
        List<Name> nameList = new ArrayList<>(nep.nonnullName().values());
        Name name = nameList.get(0);
        assertEquals("value of network nep should be '" + portName + "'",
            portName, name.getValue());
        assertEquals("value-name of network nep for '" + portName + "' should be '" + nepName + "'",
            nepName, name.getValueName());
        assertEquals("Network nep should support 1 kind of cep",
            1, nep.getSupportedCepLayerProtocolQualifier().size());
        assertThat("network nep should support 1 kind of cep",
            nep.getSupportedCepLayerProtocolQualifier(),
            hasItem(ODUTYPEODU4.class));
        assertEquals("network nep should be of ODU protocol type", LayerProtocolName.ODU, nep.getLayerProtocolName());
        checkCommonPartOfNep(nep, withSip);
    }

    private void checkNodeRuleGroupForTpdrDSR(List<NodeRuleGroup> nrgList, Uuid clientNepUuid, Uuid networkNepUuid,
                                              Uuid nodeUuid) {
        assertEquals("transponder DSR should contain 2 node rule group", 2, nrgList.size());
        for (NodeRuleGroup nodeRuleGroup : nrgList) {
            assertEquals("each node-rule-group should contain 2 NEP for transponder DSR",
                2, nodeRuleGroup.getNodeEdgePoint().size());
        }
        List<NodeEdgePoint> nodeEdgePointList = new ArrayList<>(nrgList.get(0).nonnullNodeEdgePoint().values());
        assertThat("node-rule-group nb 1 should be between nep-client1 and nep-network1",
            nodeEdgePointList.get(0).getNodeEdgePointUuid().getValue(),
            either(containsString(networkNepUuid.getValue())).or(containsString(clientNepUuid.getValue())));
        assertThat("node-rule-group nb 1 should be between nep-client1 and nep-network1",
            nodeEdgePointList.get(1).getNodeEdgePointUuid().getValue(),
            either(containsString(networkNepUuid.getValue())).or(containsString(clientNepUuid.getValue())));
        assertEquals("node-rule-group nb 1 should be between nep-client1 and nep-network1 of the same node",
            nodeEdgePointList.get(0).getNodeUuid(), nodeUuid);
        assertEquals("node-rule-group nb 1 should be between nep-client1 and nep-network1 of the same node",
            nodeEdgePointList.get(1).getNodeUuid(), nodeUuid);
        List<Rule> rule = new ArrayList<>(nrgList.get(1).nonnullRule().values());
        assertEquals("node-rule-group nb 1 should contain a single rule", 1, rule.size());
        assertEquals("local-id of the rule should be 'forward'",
            "forward", rule.get(0).getLocalId());
        assertEquals("the forwarding rule should be 'MAYFORWARDACROSSGROUP'",
            ForwardingRule.MAYFORWARDACROSSGROUP, rule.get(0).getForwardingRule());
        assertEquals("the rule type should be 'FORWARDING'",
            RuleType.FORWARDING, rule.get(0).getRuleType());
    }

    private void checkNodeRuleGroupForMuxDSR(List<NodeRuleGroup> nrgList, Uuid clientNepUuid, Uuid networkNepUuid,
                                             Uuid nodeUuid) {
        assertEquals("muxponder DSR should contain 4 node rule group", 4, nrgList.size());
        for (NodeRuleGroup nodeRuleGroup : nrgList) {
            assertEquals("each node-rule-group should contain 2 NEP for muxponder DSR",
                2, nodeRuleGroup.getNodeEdgePoint().size());
        }
        List<NodeEdgePoint> nodeEdgePointList = new ArrayList<>(nrgList.get(0).nonnullNodeEdgePoint().values());
        assertThat("node-rule-group nb 2 should be between nep-client4 and nep-network1",
            nodeEdgePointList.get(1).getNodeEdgePointUuid().getValue(),
            either(containsString(networkNepUuid.getValue())).or(containsString(clientNepUuid.getValue())));
        assertThat("node-rule-group nb 2 should be between nep-client4 and nep-network1",
            nodeEdgePointList.get(1).getNodeEdgePointUuid().getValue(),
            either(containsString(networkNepUuid.getValue())).or(containsString(clientNepUuid.getValue())));
        assertEquals("node-rule-group nb 2 should be between nep-client4 and nep-network1 of the same node",
            nodeEdgePointList.get(0).getNodeUuid(), nodeUuid);
        assertEquals("node-rule-group nb 2 should be between nep-client4 and nep-network1 of the same node",
            nodeEdgePointList.get(1).getNodeUuid(), nodeUuid);
        List<Rule> rule = new ArrayList<>(nrgList.get(1).nonnullRule().values());
        assertEquals("node-rule-group nb 2 should contain a single rule", 1, rule.size());
        assertEquals("local-id of the rule should be 'forward'",
            "forward", rule.get(0).getLocalId());
        assertEquals("the forwarding rule should be 'MAYFORWARDACROSSGROUP'",
            ForwardingRule.MAYFORWARDACROSSGROUP, rule.get(0).getForwardingRule());
        assertEquals("the rule type should be 'FORWARDING'",
            RuleType.FORWARDING, rule.get(0).getRuleType());
    }

    private void checkNodeRuleGroupForSwitchDSR(List<NodeRuleGroup> nrgList, Uuid clientNepUuid, Uuid networkNepUuid,
                                                Uuid nodeUuid) {
        assertEquals("Switch-DSR should contain a single node rule group", 1, nrgList.size());
        assertEquals("Switch-DSR node-rule-group should contain 8 NEP", 8, nrgList.get(0).getNodeEdgePoint().size());
        List<NodeEdgePoint> nrg = nrgList.get(0).nonnullNodeEdgePoint().values().stream()
            .sorted((nrg1, nrg2) -> nrg1.getNodeEdgePointUuid().getValue()
                .compareTo(nrg2.getNodeEdgePointUuid().getValue()))
            .collect(Collectors.toList());
        assertEquals("in the sorted node-rule-group, nep number 7 should be XPDR2-NETWORK1",
            networkNepUuid, nrg.get(7).getNodeEdgePointUuid());
        assertEquals("in the sorted node-rule-group, nep number 4 should be XPDR2-CLIENT4",
            clientNepUuid, nrg.get(4).getNodeEdgePointUuid());
        assertEquals("any item of the node-rule-group should have the same nodeUuid",
            nodeUuid, nrg.get(4).getNodeUuid());
        assertEquals("any item of the node-rule-group should have the same nodeUuid",
            nodeUuid, nrg.get(3).getNodeUuid());
        @Nullable
        List<Rule> ruleList = new ArrayList<>(nrgList.get(0).nonnullRule().values());
        assertEquals("node-rule-group should contain a single rule", 1, ruleList.size());
        assertEquals("local-id of the rule should be 'forward'",
            "forward", ruleList.get(0).getLocalId());
        assertEquals("the forwarding rule should be 'MAYFORWARDACROSSGROUP'",
            ForwardingRule.MAYFORWARDACROSSGROUP, ruleList.get(0).getForwardingRule());
        assertEquals("the rule type should be 'FORWARDING'",
            RuleType.FORWARDING, ruleList.get(0).getRuleType());
    }

    private void checkNodeRuleGroupForRdm(List<NodeRuleGroup> nrgList, int nbNeps) {
        assertEquals("RDM infra node - OTSi should contain a single node rule groups", 1, nrgList.size());
        if (nbNeps > 0) {
            List<NodeEdgePoint> nodeEdgePointList = new ArrayList<>(nrgList.get(0).getNodeEdgePoint().values());
            assertEquals("RDM infra node -rule-group should contain " + nbNeps + " NEP",
                nbNeps, nodeEdgePointList.size());
        } else {
            assertNull("RDM infra node -rule-group should contain no NEP", nrgList.get(0).getNodeEdgePoint());
        }
        List<Rule> ruleList = new ArrayList<>(nrgList.get(0).nonnullRule().values());
        assertEquals("node-rule-group should contain a single rule", 1, ruleList.size());
        assertEquals("local-id of the rule should be 'forward'",
            "forward", ruleList.get(0).getLocalId());
        assertEquals("the forwarding rule should be 'MAYFORWARDACROSSGROUP'",
            ForwardingRule.MAYFORWARDACROSSGROUP, ruleList.get(0).getForwardingRule());
        assertEquals("the rule type should be 'FORWARDING'",
            RuleType.FORWARDING, ruleList.get(0).getRuleType());
    }

    private void checkNodeRuleGroupForTpdrOTSi(List<NodeRuleGroup> nrgList, Uuid enepUuid, Uuid inepUuid,
                                               Uuid nodeUuid) {
        assertEquals("Tpdr-OTSi should contain two node rule groups", 2, nrgList.size());
        List<NodeEdgePoint> nodeEdgePointList = new ArrayList<>(nrgList.get(0).getNodeEdgePoint().values());
        assertEquals("Tpdr-OTSi node-rule-group should contain 2 NEP", 2, nodeEdgePointList.size());
        assertThat("Tpdr-OTSi node-rule-group should be between eNEP and iNEP of XPDR1-NETWORK1",
            nodeEdgePointList.get(0).getNodeEdgePointUuid().getValue(),
            either(containsString(enepUuid.getValue())).or(containsString(inepUuid.getValue())));
        assertThat("Tpdr-OTSi node-rule-group should be between eNEP and iNEP of XPDR1-NETWORK1",
            nodeEdgePointList.get(1).getNodeEdgePointUuid().getValue(),
            either(containsString(enepUuid.getValue())).or(containsString(inepUuid.getValue())));
        assertEquals("any item of the node-rule-group should have the same nodeUuid",
            nodeUuid, nodeEdgePointList.get(0).getNodeUuid());
        assertEquals("any item of the node-rule-group should have the same nodeUuid",
            nodeUuid, nodeEdgePointList.get(1).getNodeUuid());
        List<Rule> ruleList = new ArrayList<>(nrgList.get(0).nonnullRule().values());
        assertEquals("node-rule-group should contain a single rule", 1, ruleList.size());
        assertEquals("local-id of the rule should be 'forward'",
            "forward", ruleList.get(0).getLocalId());
        assertEquals("the forwarding rule should be 'MAYFORWARDACROSSGROUP'",
            ForwardingRule.MAYFORWARDACROSSGROUP, ruleList.get(0).getForwardingRule());
        assertEquals("the rule type should be 'FORWARDING'",
            RuleType.FORWARDING, ruleList.get(0).getRuleType());
    }

    private void checkNodeRuleGroupForMuxOTSi(List<NodeRuleGroup> nrgList, Uuid enepUuid, Uuid inepUuid,
                                              Uuid nodeUuid) {
        assertEquals("Mux-OTSi should contain a single node rule group", 1, nrgList.size());
        List<NodeEdgePoint> nodeEdgePointList = new ArrayList<>(nrgList.get(0).getNodeEdgePoint().values());
        assertEquals("Mux-OTSi node-rule-group should contain 2 NEP", 2, nodeEdgePointList.size());
        assertThat("Mux-OTSi node-rule-group should be between eNEP and iNEP of XPDR1-NETWORK1",
            nodeEdgePointList.get(0).getNodeEdgePointUuid().getValue(),
            either(containsString(enepUuid.getValue())).or(containsString(inepUuid.getValue())));
        assertThat("Mux-OTSi node-rule-group should be between eNEP and iNEP of XPDR1-NETWORK1",
            nodeEdgePointList.get(1).getNodeEdgePointUuid().getValue(),
            either(containsString(enepUuid.getValue())).or(containsString(inepUuid.getValue())));
        assertEquals("any item of the node-rule-group should have the same nodeUuid",
            nodeUuid, nodeEdgePointList.get(0).getNodeUuid());
        assertEquals("any item of the node-rule-group should have the same nodeUuid",
            nodeUuid, nodeEdgePointList.get(1).getNodeUuid());
        List<Rule> ruleList = new ArrayList<>(nrgList.get(0).nonnullRule().values());
        assertEquals("node-rule-group should contain a single rule", 1, ruleList.size());
        assertEquals("local-id of the rule should be 'forward'",
            "forward", ruleList.get(0).getLocalId());
        assertEquals("the forwarding rule should be 'MAYFORWARDACROSSGROUP'",
            ForwardingRule.MAYFORWARDACROSSGROUP, ruleList.get(0).getForwardingRule());
        assertEquals("the rule type should be 'FORWARDING'",
            RuleType.FORWARDING, ruleList.get(0).getRuleType());
    }

    private void checkNodeRuleGroupForSwitchOTSi(List<NodeRuleGroup> nrgList, Uuid enepUuid, Uuid inepUuid,
                                                 Uuid nodeUuid) {
        assertEquals("Switch-OTSi should contain 4 node rule group", 4, nrgList.size());
        for (NodeRuleGroup nodeRuleGroup : nrgList) {
            assertEquals("each node-rule-group should contain 2 NEP for Switch-OTSi",
                2, nodeRuleGroup.getNodeEdgePoint().size());
        }
        List<NodeEdgePoint> nodeEdgePointList1 = new ArrayList<>(nrgList.get(3).nonnullNodeEdgePoint().values());
        assertThat("Switch-OTSi node-rule-group nb 4 should be between eNEP and iNEP of XPDR2-NETWORK2",
            nodeEdgePointList1.get(0).getNodeEdgePointUuid().getValue(),
            either(containsString(enepUuid.getValue())).or(containsString(inepUuid.getValue())));
        assertThat("Switch-OTSi node-rule-group nb 4 should be between eNEP and iNEP of XPDR2-NETWORK2",
            nodeEdgePointList1.get(1).getNodeEdgePointUuid().getValue(),
            either(containsString(enepUuid.getValue())).or(containsString(inepUuid.getValue())));
        List<NodeEdgePoint> nodeEdgePointList0 = new ArrayList<>(nrgList.get(0).getNodeEdgePoint().values());
        assertEquals("any item of the node-rule-group should have the same nodeUuid",
            nodeUuid, nodeEdgePointList0.get(0).getNodeUuid());
        assertEquals("any item of the node-rule-group should have the same nodeUuid",
            nodeUuid, nodeEdgePointList0.get(1).getNodeUuid());
        List<Rule> ruleList0 = new ArrayList<>(nrgList.get(0).nonnullRule().values());
        assertEquals("node-rule-group should contain a single rule", 1, ruleList0.size());
        assertEquals("local-id of the rule should be 'forward'",
            "forward", ruleList0.get(0).getLocalId());
        assertEquals("the forwarding rule should be 'MAYFORWARDACROSSGROUP'",
            ForwardingRule.MAYFORWARDACROSSGROUP, ruleList0.get(0).getForwardingRule());
        assertEquals("the rule type should be 'FORWARDING'",
            RuleType.FORWARDING, ruleList0.get(0).getRuleType());
    }

    private void checkNepClient100GSwitch(OwnedNodeEdgePoint nep, Uuid nepUuid, String portName, String nepName) {
        assertEquals("bad uuid for " + portName, nepUuid, nep.getUuid());
        List<Name> nameList = new ArrayList<>(nep.nonnullName().values());
        assertEquals("value of client nep should be '" + portName + "'",
            portName, nameList.get(0).getValue());
        assertEquals("value-name of client nep for '" + portName + "' should be '" + nepName + "'",
            nepName, nameList.get(0).getValueName());
        assertEquals("Client nep should support 2 kind of cep",
            2, nep.getSupportedCepLayerProtocolQualifier().size());
        assertThat("client nep should support 2 kind of cep",
            nep.getSupportedCepLayerProtocolQualifier(),
            hasItems(ODUTYPEODU4.class, DIGITALSIGNALTYPE100GigE.class));
        assertEquals("client nep should be of DSR protocol type", LayerProtocolName.DSR, nep.getLayerProtocolName());
        checkCommonPartOfNep(nep, false);
    }

    private void checkNepClient100GTpdr(OwnedNodeEdgePoint nep, Uuid nepUuid, String portName, String nepName) {
        assertEquals("bad uuid for " + portName, nepUuid, nep.getUuid());
        List<Name> nameList = new ArrayList<>(nep.nonnullName().values());
        assertEquals("value of client nep should be '" + portName + "'",
            portName, nameList.get(0).getValue());
        assertEquals("value-name of client nep for '" + portName + "' should be 100G-tpdr'",
            nepName, nameList.get(0).getValueName());
        assertEquals("Client nep should support 1 kind of cep",
            1, nep.getSupportedCepLayerProtocolQualifier().size());
        assertThat("client nep should support 2 kind of cep",
            nep.getSupportedCepLayerProtocolQualifier(),
            hasItems(DIGITALSIGNALTYPE100GigE.class));
        assertEquals("client nep should be of DSR protocol type", LayerProtocolName.DSR, nep.getLayerProtocolName());
        checkCommonPartOfNep(nep, false);
    }

    private void checkNepOtsiNode(OwnedNodeEdgePoint nep, Uuid nepUuid, String portName, String nepName,
                                  boolean withSip) {
        assertEquals("bad uuid for " + portName, nepUuid, nep.getUuid());
        List<Name> nameList = new ArrayList<>(nep.nonnullName().values());
        assertEquals("value of OTSi nep should be '" + portName + "'",
            portName, nameList.get(0).getValue());
        assertEquals("value-name of OTSi nep should be '" + nepName + "'",
            nepName, nameList.get(0).getValueName());
        assertEquals("OTSi nep should support 2 kind of cep",
            2, nep.getSupportedCepLayerProtocolQualifier().size());
        assertThat("OTSi nep should support 2 kind of cep",
            nep.getSupportedCepLayerProtocolQualifier(),
            hasItems(PHOTONICLAYERQUALIFIEROMS.class, PHOTONICLAYERQUALIFIEROTSi.class));
        assertEquals("OTSi nep should be of PHOTONIC_MEDIA protocol type",
            LayerProtocolName.PHOTONICMEDIA, nep.getLayerProtocolName());
        checkCommonPartOfNep(nep, withSip);
    }

    private void checkNepOtsiRdmNode(OwnedNodeEdgePoint nep, Uuid nepUuid, String portName, String nepName,
                                     boolean withSip) {
        assertEquals("bad uuid for " + portName, nepUuid, nep.getUuid());
        List<Name> nameList = new ArrayList<>(nep.nonnullName().values());
        assertEquals("value of OTSi nep should be '" + portName + "'",
            portName, nameList.get(0).getValue());
        assertEquals("value-name of OTSi nep should be '" + nepName + "'",
            nepName, nameList.get(0).getValueName());
        assertEquals("OTSi nep of RDM infra node should support only 1 kind of cep",
            1, nep.getSupportedCepLayerProtocolQualifier().size());
        assertThat("OTSi nep should support 2 kind of cep",
            nep.getSupportedCepLayerProtocolQualifier(),
            hasItems(PHOTONICLAYERQUALIFIEROMS.class));
        assertEquals("OTSi nep should be of PHOTONIC_MEDIA protocol type",
            LayerProtocolName.PHOTONICMEDIA, nep.getLayerProtocolName());
        checkCommonPartOfNep(nep, withSip);
    }

    private void checkCommonPartOfNep(OwnedNodeEdgePoint nep, boolean withSip) {
        assertEquals("link port direction should be DIRECTIONAL",
            PortDirection.BIDIRECTIONAL, nep.getLinkPortDirection());
        assertEquals("administrative state should be UNLOCKED",
            AdministrativeState.UNLOCKED, nep.getAdministrativeState());
        assertEquals("termination state should be TERMINATED BIDIRECTIONAL",
            TerminationState.TERMINATEDBIDIRECTIONAL, nep.getTerminationState());
        assertEquals("life-cycle state should be INSTALLED", LifecycleState.INSTALLED, nep.getLifecycleState());
        if (withSip) {
            assertEquals("Given nep should support 1 SIP", 1, nep.getMappedServiceInterfacePoint().size());
        }
        assertEquals("termination direction should be BIDIRECTIONAL",
            TerminationDirection.BIDIRECTIONAL, nep.getTerminationDirection());
        assertEquals("operational state of client nep should be ENABLED",
            OperationalState.ENABLED, nep.getOperationalState());
        assertEquals("link-port-role of client nep should be SYMMETRIC",
            PortRole.SYMMETRIC, nep.getLinkPortRole());
    }

    private void checkTransitionalLink(org.opendaylight.yang.gen.v1
                                           .urn.onf.otcc.yang.tapi.topology.rev181210.topology.Link link,
                                       Uuid node1Uuid, Uuid node2Uuid, String tp1, String tp2, String ietfNodeId) {
        Uuid linkUuid = new Uuid(UUID.nameUUIDFromBytes((ietfNodeId + "--" + tp1 + "--" + tp2)
            .getBytes(Charset.forName("UTF-8"))).toString());
        assertEquals("bad uuid for link between DSR node " + tp1 + " and iOTSI port " + tp2, linkUuid, link.getUuid());
        assertEquals("Available capacity unit should be GBPS",
            CapacityUnit.GBPS, link.getAvailableCapacity().getTotalSize().getUnit());
        assertEquals("Available capacity -total size value should be 100",
            Uint64.valueOf(100), link.getAvailableCapacity().getTotalSize().getValue());
        assertEquals("transitional link should be between 2 nodes of protocol layers ODU and PHOTONIC_MEDIA",
            2, link.getTransitionedLayerProtocolName().size());
        assertThat("transitional link should be between 2 nodes of protocol layers ODU and PHOTONIC_MEDIA",
            link.getTransitionedLayerProtocolName(),
            hasItems(LayerProtocolName.ODU.getName(), LayerProtocolName.PHOTONICMEDIA.getName()));
        assertEquals("transitional link should be BIDIRECTIONAL",
            ForwardingDirection.BIDIRECTIONAL, link.getDirection());
        List<org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210
            .link.NodeEdgePoint> nodeEdgePointList = new ArrayList<>(link.nonnullNodeEdgePoint().values());
        assertEquals("topology uuid should be the same for the two termination point of the link",
            topologyUuid, nodeEdgePointList.get(0).getTopologyUuid());
        assertEquals("topology uuid should be the same for the two termination point of the link",
            topologyUuid, nodeEdgePointList.get(1).getTopologyUuid());
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

    private void checkOtnLink(org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.topology.Link link,
                              Uuid node1Uuid, Uuid node2Uuid, Uuid tp1Uuid, Uuid tp2Uuid, Uuid linkUuid,
                              String linkName) {
        assertEquals("bad name for the link", linkName, link.getName().get(
            new NameKey("otn link name")).getValue());
        assertEquals("bad uuid for link", linkUuid, link.getUuid());
        assertEquals("Available capacity unit should be MBPS",
            CapacityUnit.MBPS, link.getAvailableCapacity().getTotalSize().getUnit());
        String prefix = linkName.split("-")[0];
        if ("OTU4".equals(prefix)) {
            assertEquals("Available capacity -total size value should be 0",
                Uint64.valueOf(0), link.getAvailableCapacity().getTotalSize().getValue());
        } else if ("ODU4".equals(prefix)) {
            assertEquals("Available capacity -total size value should be 100 000",
                Uint64.valueOf(100000), link.getAvailableCapacity().getTotalSize().getValue());
        }
        assertEquals("Total capacity unit should be GBPS",
            CapacityUnit.GBPS, link.getTotalPotentialCapacity().getTotalSize().getUnit());
        assertEquals("Total capacity -total size value should be 100",
            Uint64.valueOf(100), link.getTotalPotentialCapacity().getTotalSize().getValue());
        if ("OTU4".equals(prefix)) {
            assertEquals("otn link should be between 2 nodes of protocol layers PHOTONIC_MEDIA",
                LayerProtocolName.PHOTONICMEDIA.getName(), link.getLayerProtocolName().get(0).getName());
        } else if ("ODU4".equals(prefix)) {
            assertEquals("otn link should be between 2 nodes of protocol layers ODU",
                LayerProtocolName.ODU.getName(), link.getLayerProtocolName().get(0).getName());
        }
        assertEquals("otn tapi link should be BIDIRECTIONAL",
            ForwardingDirection.BIDIRECTIONAL, link.getDirection());
        List<org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210
            .link.NodeEdgePoint> nodeEdgePointList = new ArrayList<>(link.nonnullNodeEdgePoint().values());
        assertEquals("topology uuid should be the same for the two termination point of the link",
            topologyUuid, nodeEdgePointList.get(0).getTopologyUuid());
        assertEquals("topology uuid should be the same for the two termination point of the link",
            topologyUuid, nodeEdgePointList.get(1).getTopologyUuid());
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
        assertEquals("operational state should be ENABLED",
            OperationalState.ENABLED, link.getOperationalState());
        assertEquals("administrative state should be UNLOCKED",
            AdministrativeState.UNLOCKED, link.getAdministrativeState());
    }

    private void checkOmsLink(org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.topology.Link link,
                              Uuid node1Uuid, Uuid node2Uuid, Uuid tp1Uuid, Uuid tp2Uuid, Uuid linkUuid,
                              String linkName) {
        assertEquals("bad name for the link", linkName, link.getName().get(
            new NameKey("OMS link name")).getValue());
        assertEquals("bad uuid for link", linkUuid, link.getUuid());
        assertEquals("oms link should be between 2 nodes of protocol layers PHOTONIC_MEDIA",
            LayerProtocolName.PHOTONICMEDIA.getName(), link.getLayerProtocolName().get(0).getName());
        assertEquals("otn tapi link should be BIDIRECTIONAL",
            ForwardingDirection.BIDIRECTIONAL, link.getDirection());
        List<org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210
            .link.NodeEdgePoint> nodeEdgePointList = new ArrayList<>(link.nonnullNodeEdgePoint().values());
        assertEquals("oms link should be between 2 neps",2 , nodeEdgePointList.size());
        assertEquals("topology uuid should be the same for the two termination point of the link",
            topologyUuid, nodeEdgePointList.get(0).getTopologyUuid());
        assertEquals("topology uuid should be the same for the two termination point of the link",
            topologyUuid, nodeEdgePointList.get(1).getTopologyUuid());
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

    private void checkXpdrRdmLink(org.opendaylight.yang.gen.v1.urn
                                      .onf.otcc.yang.tapi.topology.rev181210.topology.Link link,
                              Uuid node1Uuid, Uuid node2Uuid, Uuid tp1Uuid, Uuid tp2Uuid, Uuid linkUuid,
                              String linkName) {
        assertEquals("bad name for the link", linkName, link.getName().get(
            new NameKey("XPDR-RDM link name")).getValue());
        assertEquals("bad uuid for link", linkUuid, link.getUuid());
        assertEquals("oms link should be between 2 nodes of protocol layers PHOTONIC_MEDIA",
            LayerProtocolName.PHOTONICMEDIA.getName(), link.getLayerProtocolName().get(0).getName());
        assertEquals("otn tapi link should be BIDIRECTIONAL",
            ForwardingDirection.BIDIRECTIONAL, link.getDirection());
        List<org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210
            .link.NodeEdgePoint> nodeEdgePointList = new ArrayList<>(link.nonnullNodeEdgePoint().values());
        assertEquals("oms link should be between 2 neps",2 , nodeEdgePointList.size());
        assertEquals("topology uuid should be the same for the two termination point of the link",
            topologyUuid, nodeEdgePointList.get(0).getTopologyUuid());
        assertEquals("topology uuid should be the same for the two termination point of the link",
            topologyUuid, nodeEdgePointList.get(1).getTopologyUuid());
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

    private org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang
            .ietf.network.topology.rev180226.networks.network.Link changeOtnLinkState(
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
