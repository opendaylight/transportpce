/*
 * Copyright © 2020 Orange, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.pce.networkanalyzer;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opendaylight.transportpce.common.StringConstants;
import org.opendaylight.transportpce.common.fixedflex.GridUtils;
import org.opendaylight.transportpce.test.AbstractTest;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev230526.Node1;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev230526.TerminationPoint1Builder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.state.types.rev191129.State;
import org.opendaylight.yang.gen.v1.http.org.openroadm.equipment.states.types.rev191129.AdminStates;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev230526.networks.network.node.DegreeAttributes;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev230526.networks.network.node.DegreeAttributesBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev230526.networks.network.node.SrgAttributes;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev230526.networks.network.node.SrgAttributesBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev230526.networks.network.node.termination.point.CpAttributesBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev230526.networks.network.node.termination.point.CtpAttributesBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev230526.networks.network.node.termination.point.PpAttributesBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev230526.networks.network.node.termination.point.RxTtpAttributesBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev230526.networks.network.node.termination.point.TxTtpAttributesBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev230526.networks.network.node.termination.point.XpdrClientAttributesBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev230526.networks.network.node.termination.point.XpdrNetworkAttributesBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev230526.networks.network.node.termination.point.XpdrPortAttributesBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.types.rev230526.OpenroadmNodeType;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.types.rev230526.OpenroadmTpType;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.types.rev230526.xpdr.tp.supported.interfaces.SupportedInterfaceCapability;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.types.rev230526.xpdr.tp.supported.interfaces.SupportedInterfaceCapabilityBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.types.rev230526.xpdr.tp.supported.interfaces.SupportedInterfaceCapabilityKey;
import org.opendaylight.yang.gen.v1.http.org.openroadm.otn.network.topology.rev230526.networks.network.node.termination.point.TpSupportedInterfaces;
import org.opendaylight.yang.gen.v1.http.org.openroadm.otn.network.topology.rev230526.networks.network.node.termination.point.TpSupportedInterfacesBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.otn.network.topology.rev230526.networks.network.node.termination.point.XpdrTpPortConnectionAttributesBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.port.types.rev230526.If100GEODU4;
import org.opendaylight.yang.gen.v1.http.org.openroadm.port.types.rev230526.If10GEODU2e;
import org.opendaylight.yang.gen.v1.http.org.openroadm.port.types.rev230526.If1GEODU0;
import org.opendaylight.yang.gen.v1.http.org.openroadm.port.types.rev230526.IfOCHOTU4ODU4;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.format.rev191129.ServiceFormat;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.NetworkId;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.NodeId;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.networks.network.Node;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.networks.network.NodeBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.networks.network.NodeKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.networks.network.node.SupportingNode;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.networks.network.node.SupportingNodeBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.networks.network.node.SupportingNodeKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.Node1Builder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.TpId;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.networks.network.node.TerminationPoint;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.networks.network.node.TerminationPointBuilder;


public class PceOtnNodeTest extends AbstractTest {

    private PceOtnNode pceOtnNode;
    private Node node;

    @BeforeEach
    void setUp() {
        node = getNodeBuilder(geSupportingNodes(), OpenroadmTpType.XPONDERNETWORK).build();
    }

    @Test
    void testInitXndrTpsODU4() {
        pceOtnNode = new PceOtnNode(node, OpenroadmNodeType.MUXPDR,
                new NodeId("optical"), ServiceFormat.OMS.getName(), StringConstants.SERVICE_TYPE_ODU4, null);
        pceOtnNode.initXndrTps("AZ");
        pceOtnNode.checkAvailableTribPort();
        pceOtnNode.checkAvailableTribSlot();
        assertTrue(pceOtnNode.isValid(), "valid otn service type ");
        assertNotNull(pceOtnNode.getAvailableTribPorts(), "tpAvailableTribPort isn't null !");
    }

    @Test
    void testInitXndrTps10GE() {
        pceOtnNode = new PceOtnNode(node, OpenroadmNodeType.MUXPDR,
                new NodeId("optical"), ServiceFormat.OMS.getName(), StringConstants.SERVICE_TYPE_10GE, null);
        pceOtnNode.initXndrTps("mode");
        pceOtnNode.checkAvailableTribPort();
        pceOtnNode.checkAvailableTribSlot();
        assertFalse(pceOtnNode.isValid(), "not valid otn service type");
    }

    @Test
    void testInitXndrTps10GXponderClient1() {
        node = getNodeBuilder(geSupportingNodes(), OpenroadmTpType.XPONDERCLIENT).build();
        pceOtnNode = new PceOtnNode(node, OpenroadmNodeType.ROADM,
                new NodeId("optical"), ServiceFormat.OMS.getName(), StringConstants.SERVICE_TYPE_10GE, null);
        pceOtnNode.initXndrTps("mode");
        pceOtnNode.checkAvailableTribPort();
        pceOtnNode.checkAvailableTribSlot();
        assertFalse(pceOtnNode.isValid(), "not valid otn service type");
        assertTrue(pceOtnNode.validateSwitchingPoolBandwidth(null, null, 1L),
            "this.nodeType isn'tOpenroadmNodeType.TPDR");
    }

    @Test
    void testInitXndrTps1GXponderClient() {
        node = getNodeBuilder(geSupportingNodes(), OpenroadmTpType.XPONDERCLIENT).build();
        pceOtnNode = new PceOtnNode(node, OpenroadmNodeType.MUXPDR,
                new NodeId("optical"), ServiceFormat.OMS.getName(), StringConstants.SERVICE_TYPE_1GE, null);
        pceOtnNode.initXndrTps("mode");
        pceOtnNode.checkAvailableTribPort();
        pceOtnNode.checkAvailableTribSlot();
        assertFalse(pceOtnNode.isValid(), "not valid otn service type");
    }

    @Test
    void testInitXndrTps10GXponderClient() {
        pceOtnNode = new PceOtnNode(node, OpenroadmNodeType.MUXPDR,
                new NodeId("optical"), ServiceFormat.OMS.getName(), StringConstants.SERVICE_TYPE_10GE, null);
        pceOtnNode.validateXponder("optical", "sl");
        pceOtnNode.validateXponder("not optical", "sl");
        pceOtnNode.initXndrTps("AZ");
        pceOtnNode.checkAvailableTribPort();
        pceOtnNode.checkAvailableTribSlot();
        assertFalse(pceOtnNode.isValid(), "not valid otn service type");
        assertFalse(pceOtnNode.checkTP("tp"), "checkTp returns false by default");
    }

    private Map<SupportingNodeKey, SupportingNode> geSupportingNodes() {
        Map<SupportingNodeKey, SupportingNode> supportingNodes1 = new HashMap<>();
        SupportingNode supportingNode1 = new SupportingNodeBuilder()
                .setNodeRef(new NodeId("node 1"))
                .setNetworkRef(new NetworkId(StringConstants.CLLI_NETWORK))
                .build();
        supportingNodes1
                .put(supportingNode1.key(),supportingNode1);

        SupportingNode supportingNode2 = new SupportingNodeBuilder()
                .setNodeRef(new NodeId("node 2"))
                .setNetworkRef(new NetworkId(StringConstants.OPENROADM_NETWORK))
                .build();
        supportingNodes1
                .put(supportingNode2.key(),supportingNode2);
        return supportingNodes1;
    }

    private NodeBuilder getNodeBuilder(Map<SupportingNodeKey,SupportingNode> supportingNodes1,
            OpenroadmTpType openroadmTpType) {

        TerminationPoint1Builder tp1Bldr = getTerminationPoint1Builder(openroadmTpType);
        TerminationPointBuilder xpdrTpBldr = getTerminationPointBuilder(openroadmTpType);
        xpdrTpBldr.addAugmentation(tp1Bldr.build());

        xpdrTpBldr.addAugmentation(createAnother2TerminationPoint(openroadmTpType).build());
        xpdrTpBldr.addAugmentation(createAnotherTerminationPoint(openroadmTpType).build());

        org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev230526.Node1 node1 = getNode1();
        TerminationPoint xpdr = xpdrTpBldr.build();
        org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.Node1 node1Rev180226 =
                new Node1Builder()
                        .setTerminationPoint(Map.of(xpdr.key(),xpdr))
                        .build();


        org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.Node1 nodeIetf =
                new org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226
                        .Node1Builder()
                        .setTerminationPoint(Map.of(xpdr.key(),xpdr))
                        .build();
        Node1 node11 = new org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev230526.Node1Builder()
                .setOperationalState(State.InService).setAdministrativeState(AdminStates.InService).build();
        return new NodeBuilder()
                .setNodeId(new NodeId("node_test"))
                .withKey(new NodeKey(new NodeId("node 1")))
                .addAugmentation(node1Rev180226)
                .addAugmentation(node1)
                .addAugmentation(nodeIetf)
                .addAugmentation(node11)
                .setSupportingNode(supportingNodes1);
    }

    private org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev230526.Node1 getNode1() {
        return new org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev230526.Node1Builder()
                .setSrgAttributes(getSrgAttributes())
                .setDegreeAttributes(getDegAttributes())
                .build();
    }

    private DegreeAttributes getDegAttributes() {
        return new DegreeAttributesBuilder()
                .setAvailFreqMaps(GridUtils.initFreqMaps4FixedGrid2Available())
                .build();
    }

    private SrgAttributes getSrgAttributes() {
        return new SrgAttributesBuilder().setAvailFreqMaps(GridUtils.initFreqMaps4FixedGrid2Available()).build();
    }

    private TerminationPointBuilder getTerminationPointBuilder(OpenroadmTpType openroadmTpType) {
        return new TerminationPointBuilder()
                .setTpId(new TpId("2"))
                .addAugmentation(createOTNTerminationPoint(openroadmTpType).build());
    }

    private TerminationPoint1Builder getTerminationPoint1Builder(OpenroadmTpType openroadmTpType) {

        return new TerminationPoint1Builder()
                .setTpType(openroadmTpType).setAdministrativeState(AdminStates.InService)
                .setOperationalState(State.InService);

    }

    private org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev230526.TerminationPoint1Builder
            createAnotherTerminationPoint(OpenroadmTpType openroadmTpType) {
        return new org.opendaylight
                .yang.gen.v1.http.org.openroadm.network.topology.rev230526.TerminationPoint1Builder()
                .setCtpAttributes((new CtpAttributesBuilder()).build())
                .setCpAttributes((new CpAttributesBuilder()).build())
                .setTxTtpAttributes((new TxTtpAttributesBuilder()).setUsedWavelengths(Map.of()).build())
                .setRxTtpAttributes((new RxTtpAttributesBuilder()).setUsedWavelengths(Map.of()).build())
                .setPpAttributes((new PpAttributesBuilder()).setUsedWavelength(Map.of()).build())
                .setXpdrClientAttributes((new XpdrClientAttributesBuilder()).build())
                .setXpdrPortAttributes((new XpdrPortAttributesBuilder()).build())
                .setXpdrNetworkAttributes(new XpdrNetworkAttributesBuilder()
                        .setTailEquipmentId("destNode" + "--" + "destTp").build());
    }

    private org.opendaylight.yang.gen.v1.http.org.openroadm.otn.network.topology.rev230526
            .TerminationPoint1Builder createOTNTerminationPoint(OpenroadmTpType openroadmTpType) {

        SupportedInterfaceCapability supIfCapa = new SupportedInterfaceCapabilityBuilder()
                .setIfCapType(IfOCHOTU4ODU4.VALUE)
                .build();

        SupportedInterfaceCapability supIfCapa1 = new SupportedInterfaceCapabilityBuilder()
                .setIfCapType(If100GEODU4.VALUE)
                .build();
        SupportedInterfaceCapability supIfCapa2 = new SupportedInterfaceCapabilityBuilder()
                .setIfCapType(If10GEODU2e.VALUE)
                .build();
        SupportedInterfaceCapability supIfCapa3 = new SupportedInterfaceCapabilityBuilder()
                .setIfCapType(If1GEODU0.VALUE)
                .build();

        Map<SupportedInterfaceCapabilityKey,SupportedInterfaceCapability> supIfCapaList = new HashMap<>();
        supIfCapaList.put(supIfCapa.key(),supIfCapa);
        supIfCapaList.put(supIfCapa1.key(),supIfCapa1);
        supIfCapaList.put(supIfCapa2.key(),supIfCapa2);
        supIfCapaList.put(supIfCapa3.key(),supIfCapa3);

        TpSupportedInterfaces tpSupIf = new TpSupportedInterfacesBuilder()
                .setSupportedInterfaceCapability(supIfCapaList)
                .build();

        XpdrTpPortConnectionAttributesBuilder xtpcaBldr = new XpdrTpPortConnectionAttributesBuilder();

        return new org.opendaylight.yang.gen.v1.http.org.openroadm.otn.network.topology.rev230526
                .TerminationPoint1Builder()
            .setTpSupportedInterfaces(tpSupIf)
            .setXpdrTpPortConnectionAttributes(xtpcaBldr.build());
    }

    private org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev230526.TerminationPoint1Builder
            createAnother2TerminationPoint(OpenroadmTpType openroadmTpType) {
        return new org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev230526.TerminationPoint1Builder()
            .setTpType(openroadmTpType).setOperationalState(State.InService)
            .setAdministrativeState(AdminStates.InService);
    }
}
