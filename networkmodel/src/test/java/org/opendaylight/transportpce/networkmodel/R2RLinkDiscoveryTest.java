/*
 * Copyright © 2026 Smartoptics and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.networkmodel;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.opendaylight.mdsal.binding.api.ReadTransaction;
import org.opendaylight.mdsal.binding.api.WriteTransaction;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.transportpce.common.InstanceIdentifiers;
import org.opendaylight.transportpce.common.StringConstants;
import org.opendaylight.transportpce.common.device.DeviceTransactionManager;
import org.opendaylight.transportpce.common.network.NetworkTransactionService;
import org.opendaylight.transportpce.networkmodel.util.LinkIdUtil;
import org.opendaylight.transportpce.test.AbstractTest;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev250905.Network;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev250905.cp.to.degree.CpToDegree;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev250905.cp.to.degree.CpToDegreeBuilder;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev250905.cp.to.degree.CpToDegreeKey;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev250905.mapping.Mapping;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev250905.mapping.MappingBuilder;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev250905.mapping.MappingKey;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev250905.network.Nodes;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev250905.network.NodesBuilder;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev250905.network.NodesKey;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev250530.TerminationPoint1Builder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.state.types.rev191129.State;
import org.opendaylight.yang.gen.v1.http.org.openroadm.equipment.states.types.rev191129.AdminStates;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.NetworkId;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.NodeId;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.networks.NetworkBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.networks.network.Node;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.networks.network.NodeBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.Network1;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.Node1Builder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.TpId;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.networks.network.Link;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.networks.network.LinkKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.networks.network.node.TerminationPoint;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.networks.network.node.TerminationPointBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.networks.network.node.TerminationPointKey;
import org.opendaylight.yangtools.binding.DataObjectIdentifier;
import org.opendaylight.yangtools.yang.common.Uint32;

class R2RLinkDiscoveryTest extends AbstractTest {

    private static final String RDM_12DEG = "R2R-ROADM-12DEG";
    private static final String RDM_1DEG = "R2R-ROADM-1DEG";
    private static final String RDM_UNIDIR = "R2R-ROADM-UNIDIR";
    private static final String RDM_NO_RX = "R2R-ROADM-NO-RX";

    private final R2RLinkDiscovery linkDiscovery = new R2RLinkDiscovery(getDataBroker(),
        Mockito.mock(DeviceTransactionManager.class), Mockito.mock(NetworkTransactionService.class));

    @BeforeAll
    static void setUp() throws InterruptedException, ExecutionException {
        WriteTransaction tx = getDataBroker().newWriteOnlyTransaction();
        tx.merge(LogicalDatastoreType.CONFIGURATION, InstanceIdentifiers.OPENROADM_TOPOLOGY_II,
            new NetworkBuilder().setNetworkId(new NetworkId(StringConstants.OPENROADM_TOPOLOGY)).build());
        writeRoadm(tx, RDM_12DEG, 12, List.of("TXRX"));
        writeRoadm(tx, RDM_1DEG, 1, List.of("TXRX"));
        writeRoadm(tx, RDM_UNIDIR, 1, List.of("TX", "RX"));
        writeRoadm(tx, RDM_NO_RX, 1, List.of("TX"));
        tx.commit().get();
    }

    @Test
    void createR2RLinkOnDegree1OfNodeWithMoreThan9Degrees() throws InterruptedException, ExecutionException {
        assertTrue(linkDiscovery.createR2RLink(new NodeId(RDM_12DEG), ethIf(1), RDM_1DEG, ethIf(1)));
        assertTrue(linkExists(RDM_12DEG + "-DEG1", "DEG1-TTP-TXRX", RDM_1DEG + "-DEG1", "DEG1-TTP-TXRX"));
        assertTrue(linkExists(RDM_1DEG + "-DEG1", "DEG1-TTP-TXRX", RDM_12DEG + "-DEG1", "DEG1-TTP-TXRX"));
    }

    @Test
    void createR2RLinkToUnidirectionalDegree() throws InterruptedException, ExecutionException {
        assertTrue(linkDiscovery.createR2RLink(new NodeId(RDM_12DEG), ethIf(2), RDM_UNIDIR, ethIf(1)));
        assertTrue(linkExists(RDM_12DEG + "-DEG2", "DEG2-TTP-TXRX", RDM_UNIDIR + "-DEG1", "DEG1-TTP-RX"));
        assertTrue(linkExists(RDM_UNIDIR + "-DEG1", "DEG1-TTP-TX", RDM_12DEG + "-DEG2", "DEG2-TTP-TXRX"));
    }

    @Test
    void createR2RLinkFailsWhenDegreeHasIncompleteTtps() {
        assertFalse(linkDiscovery.createR2RLink(new NodeId(RDM_12DEG), ethIf(3), RDM_NO_RX, ethIf(1)));
    }

    private static String ethIf(int deg) {
        return "ETH-DEG" + deg;
    }

    /**
     * Write the port mapping and the openroadm-topology degree nodes of a ROADM whose degrees all have the TTPs
     * of the given directions, and whose LLDP interface on degree n is named ETH-DEGn.
     */
    private static void writeRoadm(WriteTransaction tx, String nodeId, int nbDegrees, List<String> ttpDirections) {
        Map<MappingKey, Mapping> mappings = new HashMap<>();
        Map<CpToDegreeKey, CpToDegree> cpToDegrees = new HashMap<>();
        for (int deg = 1; deg <= nbDegrees; deg++) {
            CpToDegree cpToDegree = new CpToDegreeBuilder()
                .setCircuitPackName("CP-DEG" + deg)
                .setDegreeNumber(Uint32.valueOf(deg))
                .setInterfaceName(ethIf(deg))
                .build();
            cpToDegrees.put(cpToDegree.key(), cpToDegree);
            Map<TerminationPointKey, TerminationPoint> tps = new HashMap<>();
            for (String direction : ttpDirections) {
                String ttp = "DEG" + deg + "-TTP-" + direction;
                Mapping mapping = new MappingBuilder().setLogicalConnectionPoint(ttp).build();
                mappings.put(mapping.key(), mapping);
                TerminationPoint tp = new TerminationPointBuilder()
                    .setTpId(new TpId(ttp))
                    .addAugmentation(new TerminationPoint1Builder()
                        .setAdministrativeState(AdminStates.InService)
                        .setOperationalState(State.InService)
                        .build())
                    .build();
                tps.put(tp.key(), tp);
            }
            Node degNode = new NodeBuilder()
                .setNodeId(new NodeId(nodeId + "-DEG" + deg))
                .addAugmentation(new Node1Builder().setTerminationPoint(tps).build())
                .build();
            tx.put(LogicalDatastoreType.CONFIGURATION,
                InstanceIdentifiers.OPENROADM_TOPOLOGY_II.toBuilder().child(Node.class, degNode.key()).build(),
                degNode);
        }
        tx.put(LogicalDatastoreType.CONFIGURATION,
            DataObjectIdentifier.builder(Network.class).child(Nodes.class, new NodesKey(nodeId)).build(),
            new NodesBuilder().setNodeId(nodeId).setMapping(mappings).setCpToDegree(cpToDegrees).build());
    }

    private static boolean linkExists(String srcNode, String srcTp, String destNode, String destTp)
            throws InterruptedException, ExecutionException {
        DataObjectIdentifier<Link> linkIID = InstanceIdentifiers.OPENROADM_TOPOLOGY_II.toBuilder()
            .augmentation(Network1.class)
            .child(Link.class, new LinkKey(LinkIdUtil.buildLinkId(srcNode, srcTp, destNode, destTp)))
            .build();
        try (ReadTransaction readTx = getDataBroker().newReadOnlyTransaction()) {
            return readTx.read(LogicalDatastoreType.CONFIGURATION, linkIID).get().isPresent();
        }
    }
}
