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
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

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
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.types.rev200529.OpenroadmTpType;
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
    private static DataBroker dataBroker = getDataBroker();

    @BeforeClass
    public static void setUp() throws InterruptedException, ExecutionException {
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

        topologyUuid = new Uuid(UUID.nameUUIDFromBytes(TopologyUtils.T0_MULTILAYER.getBytes(Charset.forName("UTF-8")))
            .toString());
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
        ConvertORTopoToTapiTopo tapiFactory = new ConvertORTopoToTapiTopo(topologyUuid);
        tapiFactory.convertNode(tpdr, networkPortList);

        Uuid dsrNodeUuid = new Uuid(UUID.nameUUIDFromBytes("XPDR-A1-XPDR1+DSR".getBytes(Charset.forName("UTF-8")))
            .toString());
        org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.topology.Node dsrNode = tapiFactory
            .getTapiNodes().get(new
                org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.topology.NodeKey(dsrNodeUuid));
        Uuid networkNepUuid = new Uuid(
            UUID.nameUUIDFromBytes(("XPDR-A1-XPDR1+DSR+XPDR1-NETWORK1").getBytes(Charset.forName("UTF-8")))
            .toString());
        OwnedNodeEdgePoint nepN = dsrNode.nonnullOwnedNodeEdgePoint().get(new OwnedNodeEdgePointKey(networkNepUuid));
        assertNull("Administrative State should not be present", nepN.getAdministrativeState());
        assertNull("Operational State should not be present", nepN.getOperationalState());

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
        assertNull("Administrative State should not be present", enep.getAdministrativeState());
        assertNull("Operational State should not be present", enep.getOperationalState());

        OwnedNodeEdgePoint inep = otsiNode.nonnullOwnedNodeEdgePoint().get(new OwnedNodeEdgePointKey(inepUuid));
        assertNull("Administrative State should not be present", inep.getAdministrativeState());
        assertNull("Operational State should not be present", inep.getOperationalState());
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
        ConvertORTopoToTapiTopo tapiFactory = new ConvertORTopoToTapiTopo(topologyUuid);
        tapiFactory.convertNode(tpdr, networkPortList);

        Uuid dsrNodeUuid = new Uuid(UUID.nameUUIDFromBytes("XPDR-A1-XPDR1+DSR".getBytes(Charset.forName("UTF-8")))
            .toString());
        org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.topology.Node dsrNode = tapiFactory
            .getTapiNodes().get(new
                org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.topology.NodeKey(dsrNodeUuid));
        Uuid networkNepUuid = new Uuid(
            UUID.nameUUIDFromBytes(("XPDR-A1-XPDR1+DSR+XPDR1-NETWORK1").getBytes(Charset.forName("UTF-8")))
            .toString());
        OwnedNodeEdgePoint nepN = dsrNode.nonnullOwnedNodeEdgePoint().get(new OwnedNodeEdgePointKey(networkNepUuid));
        assertEquals("Administrative State should be Locked",
            AdministrativeState.LOCKED, nepN.getAdministrativeState());
        assertEquals("Operational State should be Disabled",
            OperationalState.DISABLED, nepN.getOperationalState());

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
        assertEquals("Administrative State should be Locked",
            AdministrativeState.LOCKED, enep.getAdministrativeState());
        assertEquals("Operational State should be Disabled",
            OperationalState.DISABLED, enep.getOperationalState());

        OwnedNodeEdgePoint inep = otsiNode.nonnullOwnedNodeEdgePoint().get(new OwnedNodeEdgePointKey(inepUuid));
        assertEquals("Administrative State should be Locked",
            AdministrativeState.LOCKED, inep.getAdministrativeState());
        assertEquals("Operational State should be Disabled",
            OperationalState.DISABLED, inep.getOperationalState());
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
        ConvertORTopoToTapiTopo tapiFactory = new ConvertORTopoToTapiTopo(topologyUuid);
        tapiFactory.convertNode(tpdr, networkPortList);

        Uuid dsrNodeUuid = new Uuid(UUID.nameUUIDFromBytes("XPDR-A1-XPDR1+DSR".getBytes(Charset.forName("UTF-8")))
            .toString());
        org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.topology.Node dsrNode = tapiFactory
            .getTapiNodes().get(new
                org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.topology.NodeKey(dsrNodeUuid));
        Uuid networkNepUuid = new Uuid(
            UUID.nameUUIDFromBytes(("XPDR-A1-XPDR1+DSR+XPDR1-NETWORK1").getBytes(Charset.forName("UTF-8")))
            .toString());
        OwnedNodeEdgePoint nepN = dsrNode.nonnullOwnedNodeEdgePoint().get(new OwnedNodeEdgePointKey(networkNepUuid));
        assertEquals("Administrative State should be Locked",
            AdministrativeState.LOCKED, nepN.getAdministrativeState());
        assertEquals("Operational State should be Disabled",
            OperationalState.DISABLED, nepN.getOperationalState());

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
        assertEquals("Administrative State should be Locked",
            AdministrativeState.LOCKED, enep.getAdministrativeState());
        assertEquals("Operational State should be Disabled",
            OperationalState.DISABLED, enep.getOperationalState());

        OwnedNodeEdgePoint inep = otsiNode.nonnullOwnedNodeEdgePoint().get(new OwnedNodeEdgePointKey(inepUuid));
        assertEquals("Administrative State should be Locked",
            AdministrativeState.LOCKED, inep.getAdministrativeState());
        assertEquals("Operational State should be Disabled",
            OperationalState.DISABLED, inep.getOperationalState());
    }

    @Test
    public void convertOtnLinkWhenNoState() {
        HashMap<LinkKey, org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226
            .networks.network.Link> otnLinksAlt = new HashMap<>(otnLinks);
        org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.networks.network.Link
            link = changeOtnLinkState(otnLinks.get(new LinkKey(
                new LinkId("ODTU4-SPDR-SA1-XPDR1-XPDR1-NETWORK1toSPDR-SC1-XPDR1-XPDR1-NETWORK1"))), null, null);
        otnLinksAlt.replace(link.key(), link);

        ConvertORTopoToTapiTopo tapiFactory = new ConvertORTopoToTapiTopo(topologyUuid);
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
        assertNull("Administrative State should not be present", tapiLinks.get(0).getAdministrativeState());
        assertEquals("Administrative state should be UNLOCKED",
            AdministrativeState.UNLOCKED, tapiLinks.get(2).getAdministrativeState());
        assertNull("Operational State should not be present", tapiLinks.get(0).getOperationalState());
        assertEquals("Operational state should be ENABLED",
            OperationalState.ENABLED, tapiLinks.get(2).getOperationalState());
    }

    @Test
    public void convertOtnLinkWhenNoStateOnOppositeLink() {
        HashMap<LinkKey, org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226
            .networks.network.Link> otnLinksAlt = new HashMap<>(otnLinks);
        org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.networks.network.Link
            link = changeOtnLinkState(otnLinks.get(new LinkKey(
                new LinkId("ODTU4-SPDR-SC1-XPDR1-XPDR1-NETWORK1toSPDR-SA1-XPDR1-XPDR1-NETWORK1"))), null, null);
        otnLinksAlt.replace(link.key(), link);

        ConvertORTopoToTapiTopo tapiFactory = new ConvertORTopoToTapiTopo(topologyUuid);
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
        assertNull("Administrative State should not be present", tapiLinks.get(0).getAdministrativeState());
        assertEquals("Administrative state should be UNLOCKED",
            AdministrativeState.UNLOCKED, tapiLinks.get(2).getAdministrativeState());
        assertNull("Operational State should not be present", tapiLinks.get(0).getOperationalState());
        assertEquals("Operational state should be ENABLED",
            OperationalState.ENABLED, tapiLinks.get(2).getOperationalState());
    }

    @Test
    public void convertOtnLinkWhenBadState1() {
        HashMap<LinkKey, org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226
            .networks.network.Link> otnLinksAlt = new HashMap<>(otnLinks);
        org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.networks.network.Link
            link = changeOtnLinkState(otnLinks.get(new LinkKey(
                new LinkId("ODTU4-SPDR-SA1-XPDR1-XPDR1-NETWORK1toSPDR-SC1-XPDR1-XPDR1-NETWORK1"))),
                AdminStates.OutOfService, State.OutOfService);
        otnLinksAlt.replace(link.key(), link);

        ConvertORTopoToTapiTopo tapiFactory = new ConvertORTopoToTapiTopo(topologyUuid);
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
        assertEquals("Administrative state should be LOCKED",
            AdministrativeState.LOCKED, tapiLinks.get(0).getAdministrativeState());
        assertEquals("Administrative state should be UNLOCKED",
            AdministrativeState.UNLOCKED, tapiLinks.get(2).getAdministrativeState());
        assertEquals("Operational state should be DISABLED",
            OperationalState.DISABLED, tapiLinks.get(0).getOperationalState());
        assertEquals("Operational state should be ENABLED",
            OperationalState.ENABLED, tapiLinks.get(2).getOperationalState());
    }

    @Test
    public void convertOtnLinkWhenBadState2() {
        HashMap<LinkKey, org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226
            .networks.network.Link> otnLinksAlt = new HashMap<>(otnLinks);
        org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.networks.network.Link
            link = changeOtnLinkState(otnLinks.get(new LinkKey(
                new LinkId("ODTU4-SPDR-SA1-XPDR1-XPDR1-NETWORK1toSPDR-SC1-XPDR1-XPDR1-NETWORK1"))),
                AdminStates.Maintenance, State.Degraded);
        otnLinksAlt.replace(link.key(), link);

        ConvertORTopoToTapiTopo tapiFactory = new ConvertORTopoToTapiTopo(topologyUuid);
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
        assertEquals("Administrative state should be LOCKED",
            AdministrativeState.LOCKED, tapiLinks.get(0).getAdministrativeState());
        assertEquals("Administrative state should be UNLOCKED",
            AdministrativeState.UNLOCKED, tapiLinks.get(2).getAdministrativeState());
        assertEquals("Operational state should be DISABLED",
            OperationalState.DISABLED, tapiLinks.get(0).getOperationalState());
        assertEquals("Operational state should be ENABLED",
            OperationalState.ENABLED, tapiLinks.get(2).getOperationalState());
    }

    @Test
    public void convertOtnLinkWhenBadStateOnOppositeLink() {
        HashMap<LinkKey, org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226
            .networks.network.Link> otnLinksAlt = new HashMap<>(otnLinks);
        org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.networks.network.Link
            link = changeOtnLinkState(otnLinks.get(new LinkKey(
                new LinkId("ODTU4-SPDR-SC1-XPDR1-XPDR1-NETWORK1toSPDR-SA1-XPDR1-XPDR1-NETWORK1"))),
                AdminStates.OutOfService, State.OutOfService);
        otnLinksAlt.replace(link.key(), link);

        ConvertORTopoToTapiTopo tapiFactory = new ConvertORTopoToTapiTopo(topologyUuid);
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
        assertEquals("Administrative state should be LOCKED",
            AdministrativeState.LOCKED, tapiLinks.get(0).getAdministrativeState());
        assertEquals("Administrative state should be UNLOCKED",
            AdministrativeState.UNLOCKED, tapiLinks.get(2).getAdministrativeState());
        assertEquals("Operational state should be DISABLED",
            OperationalState.DISABLED, tapiLinks.get(0).getOperationalState());
        assertEquals("Operational state should be ENABLED",
            OperationalState.ENABLED, tapiLinks.get(2).getOperationalState());
    }

    @Test
    public void convertNodeForTransponder100G() {
        ConvertORTopoToTapiTopo tapiFactory = new ConvertORTopoToTapiTopo(topologyUuid);
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

        List<Link> tapiLinks = tapiFactory.getTapiLinks().values().stream()
            .sorted((l1, l2) -> l1.getUuid().getValue().compareTo(l2.getUuid().getValue()))
            .collect(Collectors.toList());
        checkTransitionalLink(tapiLinks.get(1), dsrNodeUuid, otsiNodeUuid,
            "XPDR-A1-XPDR1+DSR+XPDR1-NETWORK1", "XPDR-A1-XPDR1+iOTSi+XPDR1-NETWORK1", "XPDR-A1-XPDR1");
    }

    @Test
    public void convertNodeForOtnMuxponder() {
        ConvertORTopoToTapiTopo tapiFactory = new ConvertORTopoToTapiTopo(topologyUuid);
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

        List<Link> tapiLinks = tapiFactory.getTapiLinks().values().stream()
            .sorted((l1, l2) -> l1.getUuid().getValue().compareTo(l2.getUuid().getValue()))
            .collect(Collectors.toList());
        checkTransitionalLink(tapiLinks.get(0), dsrNodeUuid, otsiNodeUuid,
            "SPDR-SA1-XPDR1+DSR+XPDR1-NETWORK1", "SPDR-SA1-XPDR1+iOTSi+XPDR1-NETWORK1", "SPDR-SA1-XPDR1");
    }

    @Test
    public void convertNodeForOtnSwitch() {
        ConvertORTopoToTapiTopo tapiFactory = new ConvertORTopoToTapiTopo(topologyUuid);
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

        List<Link> tapiLinks = tapiFactory.getTapiLinks().values().stream()
            .sorted((l1, l2) -> l1.getUuid().getValue().compareTo(l2.getUuid().getValue()))
            .collect(Collectors.toList());
        checkTransitionalLink(tapiLinks.get(0), dsrNodeUuid, otsiNodeUuid,
            "SPDR-SA1-XPDR2+DSR+XPDR2-NETWORK4", "SPDR-SA1-XPDR2+iOTSi+XPDR2-NETWORK4", "SPDR-SA1-XPDR2");
    }

    @Test
    public void convertOtnLink() {
        ConvertORTopoToTapiTopo tapiFactory = new ConvertORTopoToTapiTopo(topologyUuid);
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
        Uuid tp1Uuid = new Uuid(UUID.nameUUIDFromBytes("SPDR-SA1-XPDR1+DSR+XPDR1-NETWORK1"
            .getBytes(Charset.forName("UTF-8"))).toString());
        Uuid tp2Uuid = new Uuid(UUID.nameUUIDFromBytes("SPDR-SC1-XPDR1+DSR+XPDR1-NETWORK1"
            .getBytes(Charset.forName("UTF-8"))).toString());
        Uuid tp3Uuid = new Uuid(UUID.nameUUIDFromBytes("SPDR-SA1-XPDR1+iOTSi+XPDR1-NETWORK1"
            .getBytes(Charset.forName("UTF-8"))).toString());
        Uuid tp4Uuid = new Uuid(UUID.nameUUIDFromBytes("SPDR-SC1-XPDR1+iOTSi+XPDR1-NETWORK1"
            .getBytes(Charset.forName("UTF-8"))).toString());
        Uuid link1Uuid =
            new Uuid(UUID.nameUUIDFromBytes("ODTU4-SPDR-SA1-XPDR1-XPDR1-NETWORK1toSPDR-SC1-XPDR1-XPDR1-NETWORK1"
                .getBytes(Charset.forName("UTF-8"))).toString());
        Uuid link2Uuid =
            new Uuid(UUID.nameUUIDFromBytes("OTU4-SPDR-SA1-XPDR1-XPDR1-NETWORK1toSPDR-SC1-XPDR1-XPDR1-NETWORK1"
                .getBytes(Charset.forName("UTF-8"))).toString());

        List<Link> links = tapiFactory.getTapiLinks().values().stream()
            .sorted((l1, l2) -> l1.getUuid().getValue().compareTo(l2.getUuid().getValue()))
            .collect(Collectors.toList());
        checkOtnLink(links.get(0), node1Uuid, node2Uuid, tp1Uuid, tp2Uuid, link1Uuid,
            "ODTU4-SPDR-SA1-XPDR1-XPDR1-NETWORK1toSPDR-SC1-XPDR1-XPDR1-NETWORK1");
        checkOtnLink(links.get(2), node3Uuid, node4Uuid, tp3Uuid, tp4Uuid, link2Uuid,
            "OTU4-SPDR-SA1-XPDR1-XPDR1-NETWORK1toSPDR-SC1-XPDR1-XPDR1-NETWORK1");
    }

    @Test
    public void convertRoadmInfrastructureWhenNoXponderAttached() {
        ConvertORTopoToTapiTopo tapiFactory = new ConvertORTopoToTapiTopo(topologyUuid);
        tapiFactory.convertRoadmInfrastructure();

        assertEquals("Node list size should be 1", 1, tapiFactory.getTapiNodes().size());
        assertEquals("Link list size should be empty", 0, tapiFactory.getTapiLinks().size());
        List<org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.topology.Node> tapiNodes
            = tapiFactory.getTapiNodes().values().stream().collect(Collectors.toList());
        Uuid otsiNodeUuid = new Uuid(UUID.nameUUIDFromBytes("ROADM-infra".getBytes(Charset.forName("UTF-8")))
            .toString());
        checkOtsiNode(tapiNodes.get(0), otsiNodeUuid, "infra", "ROADM-infra");
    }

    @Test
    public void convertRoadmInfrastructureWhenOtnMuxAttached() {
        ConvertORTopoToTapiTopo tapiFactory = new ConvertORTopoToTapiTopo(topologyUuid);
        List<String> networkPortListA = new ArrayList<>();
        for (TerminationPoint tp : otnMuxA.augmentation(Node1.class).getTerminationPoint().values()) {
            if (tp.augmentation(TerminationPoint1.class).getTpType().equals(OpenroadmTpType.XPONDERNETWORK)) {
                networkPortListA.add(tp.getTpId().getValue());
            }
        }
        tapiFactory.convertNode(otnMuxA, networkPortListA);
        tapiFactory.convertRoadmInfrastructure();

        assertEquals("Node list size should be 3", 3, tapiFactory.getTapiNodes().size());
        assertEquals("Link list size should be 2", 2, tapiFactory.getTapiLinks().size());
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
            new Uuid(UUID.nameUUIDFromBytes("SPDR-SA1-XPDR1--XPDR1-NETWORK1 and ROADM-infra--NodeEdgePoint_1"
                .getBytes(Charset.forName("UTF-8"))).toString());
        checkOmsLink(links.get(0), node1Uuid, node2Uuid, tp1Uuid, tp2Uuid, linkUuid,
            "SPDR-SA1-XPDR1--XPDR1-NETWORK1 and ROADM-infra--NodeEdgePoint_1");
    }

    private void checkDsrNode(org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.topology.Node node,
        Uuid nodeUuid, String dsrNodeType, String nodeId) {
        assertEquals("incorrect node uuid", nodeUuid, node.getUuid());
        assertEquals("incorrect node name", nodeId, node.getName().get(new NameKey("dsr/odu node name")).getValue());
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
        List<OwnedNodeEdgePoint> nepsN = node.nonnullOwnedNodeEdgePoint().values().stream()
            .filter(n -> n.getName().containsKey(new NameKey("NodeEdgePoint_N")))
            .sorted((nep1, nep2) -> nep1.getUuid().getValue().compareTo(nep2.getUuid().getValue()))
            .collect(Collectors.toList());
        List<OwnedNodeEdgePoint> nepsC;
        switch (dsrNodeType) {
            case "switch":
                nepsC = node.nonnullOwnedNodeEdgePoint().values().stream()
                    .filter(n -> n.getName().containsKey(new NameKey("NodeEdgePoint_C")))
                    .sorted((nep1, nep2) -> nep1.getUuid().getValue().compareTo(nep2.getUuid().getValue()))
                    .collect(Collectors.toList());
                assertEquals("Switch-DSR node should have 4 NEPs network", 4, nepsN.size());
                assertEquals("Switch-DSR node should have 4 NEPs client", 4, nepsC.size());
                OwnedNodeEdgePoint nep1 = nepsC.get(2);
                Uuid client4NepUuid = new Uuid(
                        UUID.nameUUIDFromBytes((nodeId + "+DSR+XPDR2-CLIENT4").getBytes(Charset.forName("UTF-8")))
                        .toString());
                checkNepClient100GSwitch(nep1, client4NepUuid, "XPDR2-CLIENT4", "NodeEdgePoint_C");
                OwnedNodeEdgePoint nep2 = nepsN.get(1);
                Uuid networkNepUuid = new Uuid(
                        UUID.nameUUIDFromBytes((nodeId + "+DSR+XPDR2-NETWORK1").getBytes(Charset.forName("UTF-8")))
                        .toString());
                checkNepNetworkODU4(nep2, networkNepUuid, "XPDR2-NETWORK1", "NodeEdgePoint_N");
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
                assertEquals("Mux-DSR node should have 1 NEP network", 1, nepsN.size());
                assertEquals("Mux-DSR node should have 4 NEPs client", 4, nepsC.size());
                OwnedNodeEdgePoint nep3 = nepsC.get(2);
                Uuid client3NepUuid = new Uuid(
                        UUID.nameUUIDFromBytes((nodeId + "+DSR+XPDR1-CLIENT3").getBytes(Charset.forName("UTF-8")))
                        .toString());
                checkNepClient10G(nep3, client3NepUuid, "XPDR1-CLIENT3", "NodeEdgePoint_C");

                OwnedNodeEdgePoint nep4 = nepsN.get(0);
                Uuid networkNepUuid2 = new Uuid(
                        UUID.nameUUIDFromBytes((nodeId + "+DSR+XPDR1-NETWORK1").getBytes(Charset.forName("UTF-8")))
                        .toString());
                checkNepNetworkODU4(nep4, networkNepUuid2, "XPDR1-NETWORK1", "NodeEdgePoint_N");
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
                assertEquals("Tpdr-DSR node should have 2 NEPs network", 2, nepsN.size());
                assertEquals("Tpdr-DSR node should have 2 NEPs client", 2, nepsC.size());
                OwnedNodeEdgePoint nep5 = nepsC.get(0);
                Uuid client1NepUuid = new Uuid(
                        UUID.nameUUIDFromBytes((nodeId + "+DSR+XPDR1-CLIENT1").getBytes(Charset.forName("UTF-8")))
                        .toString());
                checkNepClient100GTpdr(nep5, client1NepUuid, "XPDR1-CLIENT1", "100G-tpdr");

                OwnedNodeEdgePoint nep6 = nepsN.get(0);
                Uuid networkNepUuid3 = new Uuid(
                        UUID.nameUUIDFromBytes((nodeId + "+DSR+XPDR1-NETWORK1").getBytes(Charset.forName("UTF-8")))
                        .toString());
                checkNepNetworkODU4(nep6, networkNepUuid3, "XPDR1-NETWORK1", "NodeEdgePoint_N");
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
        assertEquals("incorrect node uuid", nodeUuid, node.getUuid());
        assertEquals("incorrect node name", nodeId, node.getName().get(new NameKey("otsi node name")).getValue());
        assertEquals("administrative state should be UNLOCKED",
            AdministrativeState.UNLOCKED, node.getAdministrativeState());
        assertEquals("life-cycle state should be INSTALLED", LifecycleState.INSTALLED, node.getLifecycleState());
        assertEquals("operational state should be ENABLED", OperationalState.ENABLED, node.getOperationalState());
        assertEquals("value-name should be 'dsr/odu node name'",
             "otsi node name", node.nonnullName().values().stream().findFirst().get().getValueName());
        assertEquals("otsi node should manage a single protocol layer : PHOTONIC_MEDIA",
            1, node.getLayerProtocolName().size());
        assertEquals("otsi node should manage a single protocol layer : PHOTONIC_MEDIA",
            LayerProtocolName.PHOTONICMEDIA, node.getLayerProtocolName().get(0));
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
                assertEquals("Switch-OTSi node should have 4 eNEPs", 4, nepsE.size());
                assertEquals("Switch-OTSi node should have 4 iNEPs", 4, nepsI.size());
                OwnedNodeEdgePoint nep1 = nepsI.get(1);
                Uuid inepUuid = new Uuid(
                        UUID.nameUUIDFromBytes((nodeId + "+iOTSi+XPDR2-NETWORK2").getBytes(Charset.forName("UTF-8")))
                        .toString());
                checkNepOtsiNode(nep1, inepUuid, "XPDR2-NETWORK2", "iNodeEdgePoint");
                OwnedNodeEdgePoint nep2 = nepsE.get(0);
                Uuid enepUuid = new Uuid(
                        UUID.nameUUIDFromBytes((nodeId + "+eOTSi+XPDR2-NETWORK2").getBytes(Charset.forName("UTF-8")))
                        .toString());
                checkNepOtsiNode(nep2, enepUuid, "XPDR2-NETWORK2", "eNodeEdgePoint");
                List<NodeRuleGroup> nrgList = node.nonnullNodeRuleGroup().values().stream()
                    .sorted((nrg1, nrg2) -> nrg1.getUuid().getValue().compareTo(nrg2.getUuid().getValue()))
                    .collect(Collectors.toList());
                checkNodeRuleGroupForSwitchOTSi(nrgList, enepUuid, inepUuid, nodeUuid);
                break;
            case "mux":
                assertEquals("Mux-OTSi node should have 1 eNEP", 1, nepsE.size());
                assertEquals("Mux-OTSi node should have 1 iNEPs", 1, nepsI.size());
                OwnedNodeEdgePoint nep3 = nepsE.get(0);
                Uuid enepUuid2 = new Uuid(
                        UUID.nameUUIDFromBytes((nodeId + "+eOTSi+XPDR1-NETWORK1").getBytes(Charset.forName("UTF-8")))
                        .toString());
                checkNepOtsiNode(nep3, enepUuid2, "XPDR1-NETWORK1", "eNodeEdgePoint");
                OwnedNodeEdgePoint nep4 = nepsI.get(0);
                Uuid inepUuid2 = new Uuid(
                        UUID.nameUUIDFromBytes((nodeId + "+iOTSi+XPDR1-NETWORK1").getBytes(Charset.forName("UTF-8")))
                        .toString());
                checkNepOtsiNode(nep4, inepUuid2, "XPDR1-NETWORK1", "iNodeEdgePoint");
                List<NodeRuleGroup> nrgList2 = node.nonnullNodeRuleGroup().values().stream()
                    .sorted((nrg1, nrg2) -> nrg1.getUuid().getValue().compareTo(nrg2.getUuid().getValue()))
                    .collect(Collectors.toList());
                checkNodeRuleGroupForMuxOTSi(nrgList2, enepUuid2, inepUuid2, nodeUuid);
                break;
            case "tpdr":
                assertEquals("Tpdr-OTSi node should have 2 eNEPs", 2, nepsE.size());
                assertEquals("Tpdr-OTSi node should have 2 iNEPs", 2, nepsI.size());
                OwnedNodeEdgePoint nep5 = nepsE.get(0);
                Uuid enepUuid3 = new Uuid(
                        UUID.nameUUIDFromBytes((nodeId + "+eOTSi+XPDR1-NETWORK1").getBytes(Charset.forName("UTF-8")))
                        .toString());
                checkNepOtsiNode(nep5, enepUuid3, "XPDR1-NETWORK1", "eNodeEdgePoint");
                OwnedNodeEdgePoint nep6 = nepsI.get(0);
                Uuid inepUuid3 = new Uuid(
                        UUID.nameUUIDFromBytes((nodeId + "+iOTSi+XPDR1-NETWORK1").getBytes(Charset.forName("UTF-8")))
                        .toString());
                checkNepOtsiNode(nep6, inepUuid3, "XPDR1-NETWORK1", "iNodeEdgePoint");
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
        assertEquals("client nep should be of ETH protocol type", LayerProtocolName.ETH, nep.getLayerProtocolName());
        checkCommonPartOfNep(nep, false);
    }

    private void checkNepNetworkODU4(OwnedNodeEdgePoint nep, Uuid nepUuid, String portName, String nepName) {
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
        checkCommonPartOfNep(nep, false);
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
        assertEquals("in the sorted node-rule-group, nep number 2 should be XPDR2-NETWORK1",
            networkNepUuid, nrg.get(4).getNodeEdgePointUuid());
        assertEquals("in the sorted node-rule-group, nep number 6 should be XPDR2-CLIENT4",
            clientNepUuid, nrg.get(3).getNodeEdgePointUuid());
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

    private void checkNodeRuleGroupForRdmInfra(List<NodeRuleGroup> nrgList, int nbNeps) {
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
        assertEquals("client nep should be of ETH protocol type", LayerProtocolName.ETH, nep.getLayerProtocolName());
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
        assertEquals("client nep should be of ETH protocol type", LayerProtocolName.ETH, nep.getLayerProtocolName());
        checkCommonPartOfNep(nep, false);
    }

    private void checkNepOtsiNode(OwnedNodeEdgePoint nep, Uuid nepUuid, String portName, String nepName) {
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
        assertEquals("OTSi nep should support one SIP", 1, nep.getMappedServiceInterfacePoint().size());
        checkCommonPartOfNep(nep, false);
    }

    private void checkNepOtsiRdmNode(OwnedNodeEdgePoint nep, Uuid nepUuid, String portName, String nepName) {
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
        assertEquals("OTSi nep of RDM infra should support no SIP", 0, nep.nonnullMappedServiceInterfacePoint().size());
        checkCommonPartOfNep(nep, true);
    }

    private void checkCommonPartOfNep(OwnedNodeEdgePoint nep, boolean isRdm) {
        assertEquals("link port direction should be DIRECTIONAL",
            PortDirection.BIDIRECTIONAL, nep.getLinkPortDirection());
        assertEquals("administrative state should be UNLOCKED",
            AdministrativeState.UNLOCKED, nep.getAdministrativeState());
        assertEquals("termination state should be TERMINATED BIDIRECTIONAL",
            TerminationState.TERMINATEDBIDIRECTIONAL, nep.getTerminationState());
        assertEquals("life-cycle state should be INSTALLED", LifecycleState.INSTALLED, nep.getLifecycleState());
        if (!isRdm) {
            assertEquals("client nep should support 1 SIP", 1, nep.getMappedServiceInterfacePoint().size());
        }
        assertEquals("termination direction should be BIDIRECTIONAL",
            TerminationDirection.BIDIRECTIONAL, nep.getTerminationDirection());
        assertEquals("operational state of client nep should be ENABLED",
            OperationalState.ENABLED, nep.getOperationalState());
        assertEquals("link-port-role of client nep should be SYMMETRIC",
            PortRole.SYMMETRIC, nep.getLinkPortRole());
    }

    private void checkTransitionalLink(Link link, Uuid node1Uuid, Uuid node2Uuid, String tp1, String tp2,
        String ietfNodeId) {
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

    private void checkOtnLink(Link link, Uuid node1Uuid, Uuid node2Uuid, Uuid tp1Uuid, Uuid tp2Uuid, Uuid linkUuid,
        String linkName) {
        assertEquals("bad name for the link", linkName, link.getName().get(new NameKey("otn link name")).getValue());
        assertEquals("bad uuid for link", linkUuid, link.getUuid());
        assertEquals("Available capacity unit should be MBPS",
            CapacityUnit.MBPS, link.getAvailableCapacity().getTotalSize().getUnit());
        String prefix = linkName.split("-")[0];
        if ("OTU4".equals(prefix)) {
            assertEquals("Available capacity -total size value should be 0",
                Uint64.valueOf(0), link.getAvailableCapacity().getTotalSize().getValue());
        } else if ("ODTU4".equals(prefix)) {
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
        } else if ("ODTU4".equals(prefix)) {
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

    private void checkOmsLink(Link link, Uuid node1Uuid, Uuid node2Uuid, Uuid tp1Uuid, Uuid tp2Uuid, Uuid linkUuid,
        String linkName) {
        assertEquals("bad name for the link", linkName, link.getName().get(new NameKey("OMS link name")).getValue());
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
