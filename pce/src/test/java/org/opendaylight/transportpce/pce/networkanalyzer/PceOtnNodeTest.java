/*
 * Copyright © 2020 Orange, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.pce.networkanalyzer;

import java.util.HashMap;
import java.util.Map;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.opendaylight.transportpce.common.NetworkUtils;
import org.opendaylight.transportpce.common.StringConstants;
import org.opendaylight.transportpce.common.fixedflex.GridUtils;
import org.opendaylight.transportpce.test.AbstractTest;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev200529.Node1;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev200529.TerminationPoint1Builder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.state.types.rev191129.State;
import org.opendaylight.yang.gen.v1.http.org.openroadm.equipment.states.types.rev191129.AdminStates;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev200529.networks.network.node.DegreeAttributes;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev200529.networks.network.node.DegreeAttributesBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev200529.networks.network.node.SrgAttributes;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev200529.networks.network.node.SrgAttributesBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev200529.networks.network.node.termination.point.CpAttributesBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev200529.networks.network.node.termination.point.CtpAttributesBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev200529.networks.network.node.termination.point.PpAttributesBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev200529.networks.network.node.termination.point.RxTtpAttributesBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev200529.networks.network.node.termination.point.TxTtpAttributesBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev200529.networks.network.node.termination.point.XpdrClientAttributesBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev200529.networks.network.node.termination.point.XpdrNetworkAttributesBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev200529.networks.network.node.termination.point.XpdrPortAttributesBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.types.rev200529.OpenroadmNodeType;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.types.rev200529.OpenroadmTpType;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.types.rev200529.xpdr.tp.supported.interfaces.SupportedInterfaceCapability;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.types.rev200529.xpdr.tp.supported.interfaces.SupportedInterfaceCapabilityBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.types.rev200529.xpdr.tp.supported.interfaces.SupportedInterfaceCapabilityKey;
import org.opendaylight.yang.gen.v1.http.org.openroadm.otn.network.topology.rev200529.networks.network.node.termination.point.TpSupportedInterfaces;
import org.opendaylight.yang.gen.v1.http.org.openroadm.otn.network.topology.rev200529.networks.network.node.termination.point.TpSupportedInterfacesBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.otn.network.topology.rev200529.networks.network.node.termination.point.XpdrTpPortConnectionAttributesBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.port.types.rev200327.If100GEODU4;
import org.opendaylight.yang.gen.v1.http.org.openroadm.port.types.rev200327.If10GEODU2e;
import org.opendaylight.yang.gen.v1.http.org.openroadm.port.types.rev200327.If1GEODU0;
import org.opendaylight.yang.gen.v1.http.org.openroadm.port.types.rev200327.IfOCHOTU4ODU4;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.format.rev190531.ServiceFormat;
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

    @Before
    public void setUp() {
        node = getNodeBuilder(geSupportingNodes(), OpenroadmTpType.XPONDERNETWORK).build();
    }

    @Test
    public void testInitXndrTpsODU4() {
        pceOtnNode = new PceOtnNode(node, OpenroadmNodeType.MUXPDR,
                new NodeId("optical"), ServiceFormat.OMS.getName(), StringConstants.SERVICE_TYPE_ODU4);
        pceOtnNode.initXndrTps("AZ");
        pceOtnNode.checkAvailableTribPort();
        pceOtnNode.checkAvailableTribSlot();
        Assert.assertTrue("valid otn service type " , pceOtnNode.isValid());
        Assert.assertNotNull("tpAvailableTribPort isn't null !" , pceOtnNode.getAvailableTribPorts());
    }

    @Test
    public void testInitXndrTps10GE() {
        pceOtnNode = new PceOtnNode(node, OpenroadmNodeType.MUXPDR,
                new NodeId("optical"), ServiceFormat.OMS.getName(), StringConstants.SERVICE_TYPE_10GE);
        pceOtnNode.initXndrTps("mode");
        pceOtnNode.checkAvailableTribPort();
        pceOtnNode.checkAvailableTribSlot();
        Assert.assertFalse("not valid otn service type" , pceOtnNode.isValid());
    }

    @Test
    public void testInitXndrTps10GXponderClient1() {
        node = getNodeBuilder(geSupportingNodes(), OpenroadmTpType.XPONDERCLIENT).build();
        pceOtnNode = new PceOtnNode(node, OpenroadmNodeType.ROADM,
                new NodeId("optical"), ServiceFormat.OMS.getName(), StringConstants.SERVICE_TYPE_10GE);
        pceOtnNode.initXndrTps("mode");
        pceOtnNode.checkAvailableTribPort();
        pceOtnNode.checkAvailableTribSlot();
        Assert.assertFalse("not valid otn service type", pceOtnNode.isValid());
        Assert.assertTrue("this.nodeType isn'tOpenroadmNodeType.TPDR" ,
                pceOtnNode.validateSwitchingPoolBandwidth(null,null,1L));

    }

    @Test
    public void testInitXndrTps1GXponderClient() {
        node = getNodeBuilder(geSupportingNodes(), OpenroadmTpType.XPONDERCLIENT).build();
        pceOtnNode = new PceOtnNode(node, OpenroadmNodeType.MUXPDR,
                new NodeId("optical"), ServiceFormat.OMS.getName(), StringConstants.SERVICE_TYPE_1GE);
        pceOtnNode.initXndrTps("mode");
        pceOtnNode.checkAvailableTribPort();
        pceOtnNode.checkAvailableTribSlot();
        Assert.assertFalse("not valid otn service type" , pceOtnNode.isValid());
    }

    @Test
    public void testInitXndrTps10GXponderClient() {
        pceOtnNode = new PceOtnNode(node, OpenroadmNodeType.MUXPDR,
                new NodeId("optical"), ServiceFormat.OMS.getName(), StringConstants.SERVICE_TYPE_10GE);
        pceOtnNode.validateXponder("optical", "sl");
        pceOtnNode.validateXponder("not optical", "sl");
        pceOtnNode.initXndrTps("AZ");
        pceOtnNode.checkAvailableTribPort();
        pceOtnNode.checkAvailableTribSlot();
        Assert.assertFalse("not valid otn service type" , pceOtnNode.isValid());
        Assert.assertFalse("checkTp returns false by default " , pceOtnNode.checkTP("tp"));

    }

    @Test
    public void testIsPceOtnNodeValid() {
        pceOtnNode = new PceOtnNode(node, OpenroadmNodeType.MUXPDR,
                new NodeId("optical"), ServiceFormat.OMS.getName(), StringConstants.SERVICE_TYPE_10GE);
        pceOtnNode.initXndrTps("AZ");
        pceOtnNode.checkAvailableTribPort();
        pceOtnNode.checkAvailableTribSlot();
        Assert.assertFalse("not valid otn service Type" , pceOtnNode.isPceOtnNodeValid(pceOtnNode));
    }

    @Test
    public void testIsPceOtnNodeValidNode() {
        pceOtnNode = new PceOtnNode(node, OpenroadmNodeType.DEGREE,
                new NodeId("optical"), ServiceFormat.OMS.getName(), StringConstants.SERVICE_TYPE_100GE_M);
        pceOtnNode.initXndrTps("AZ");
        pceOtnNode.checkAvailableTribPort();
        pceOtnNode.checkAvailableTribSlot();
        pceOtnNode = Mockito.spy(pceOtnNode);
        Mockito.when(pceOtnNode.getNodeId()).thenReturn(null);
        Assert.assertFalse("not valid node , nodeId is null" , pceOtnNode.isPceOtnNodeValid(pceOtnNode));

    }

    @Test
    public void testIsPceOtnNodeValidNodeTypeNull() {
        pceOtnNode = new PceOtnNode(node, null,
                new NodeId("optical"), ServiceFormat.OMS.getName(), StringConstants.SERVICE_TYPE_100GE_M);
        pceOtnNode.initXndrTps("AZ");
        pceOtnNode.checkAvailableTribPort();
        pceOtnNode.checkAvailableTribSlot();
        Assert.assertFalse("not valid type, nodeType is null " , pceOtnNode.isPceOtnNodeValid(pceOtnNode));
    }

    @Test
    public void testIsPceOtnNodeValidNodeTypeDeg() {
        pceOtnNode = new PceOtnNode(node, OpenroadmNodeType.DEGREE,
                new NodeId("optical"), ServiceFormat.OMS.getName(), StringConstants.SERVICE_TYPE_100GE_M);
        pceOtnNode.initXndrTps("AZ");
        Assert.assertFalse("not valid node , its type isn't one of MUXPDR or SWITCH or TPDR" ,
                pceOtnNode.isPceOtnNodeValid(pceOtnNode));
    }

    @Test
    public void testIsPceOtnNodeValidTrue() {
        pceOtnNode = new PceOtnNode(node, OpenroadmNodeType.MUXPDR,
                new NodeId("optical"), ServiceFormat.OMS.getName(), StringConstants.SERVICE_TYPE_ODU4);
        pceOtnNode.initXndrTps("AZ");
        pceOtnNode.checkAvailableTribPort();
        pceOtnNode.checkAvailableTribSlot();
        Assert.assertTrue("valid otn service type ", pceOtnNode.isPceOtnNodeValid(pceOtnNode));
    }

    @Test
    public void testIsPceOtnNodeValidChecksw() {
        node = getNodeBuilder(geSupportingNodes(), OpenroadmTpType.XPONDERCLIENT).build();
        pceOtnNode = new PceOtnNode(node, OpenroadmNodeType.MUXPDR,
                new NodeId("optical"), ServiceFormat.OMS.getName(), StringConstants.SERVICE_TYPE_1GE);
        pceOtnNode.initXndrTps("mode");
        Assert.assertFalse("not valid otn service Type" , pceOtnNode.isPceOtnNodeValid(pceOtnNode));
    }

    private Map<SupportingNodeKey, SupportingNode> geSupportingNodes() {
        Map<SupportingNodeKey, SupportingNode> supportingNodes1 = new HashMap<>();
        SupportingNode supportingNode1 = new SupportingNodeBuilder()
                .setNodeRef(new NodeId("node 1"))
                .setNetworkRef(new NetworkId(NetworkUtils.CLLI_NETWORK_ID))
                .build();
        supportingNodes1
                .put(supportingNode1.key(),supportingNode1);

        SupportingNode supportingNode2 = new SupportingNodeBuilder()
                .setNodeRef(new NodeId("node 2"))
                .setNetworkRef(new NetworkId(NetworkUtils.UNDERLAY_NETWORK_ID))
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

        org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev200529.Node1 node1 = getNode1();
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
        Node1 node11 = new org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev200529.Node1Builder()
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

    private NodeBuilder getNodeBuilderEmpty(Map<SupportingNodeKey,SupportingNode> supportingNodes1,
            OpenroadmTpType openroadmTpType) {

        TerminationPoint1Builder tp1Bldr = getTerminationPoint1Builder(openroadmTpType);
        TerminationPointBuilder xpdrTpBldr = getTerminationPointBuilder(openroadmTpType);
        xpdrTpBldr.addAugmentation(tp1Bldr.build());
        xpdrTpBldr.addAugmentation(createAnotherTerminationPoint(openroadmTpType).build());

        org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev200529.Node1 node1 = getNode1Empty();
        TerminationPoint xpdr = xpdrTpBldr.build();
        org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.Node1 node1Rev180226 =
                new Node1Builder()
                        .setTerminationPoint(Map.of(xpdr.key(),xpdr))
                        .build();
        Node1 node11 = new org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev200529.Node1Builder()
                .setOperationalState(State.InService).setAdministrativeState(AdminStates.InService).build();


        return new NodeBuilder()
                .setNodeId(new NodeId("node_test"))
                .withKey(new NodeKey(new NodeId("node 1")))
                .addAugmentation(node1Rev180226)
                .addAugmentation(node1)
                .addAugmentation(node11)
                .setSupportingNode(supportingNodes1);
    }

    private org.opendaylight
            .yang.gen.v1.http.org.openroadm.network.topology.rev200529.Node1 getNode1() {
        return new org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev200529.Node1Builder()
                .setSrgAttributes(getSrgAttributes())
                .setDegreeAttributes(getDegAttributes())
                .build();
    }

    private org.opendaylight
            .yang.gen.v1.http.org.openroadm.network.topology.rev200529.Node1 getNode1Empty() {
        return new org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev200529.Node1Builder()
                .setSrgAttributes(getEmptySrgAttributes())
                .setDegreeAttributes(getEmptyDegAttributes())
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

    private DegreeAttributes getEmptyDegAttributes() {
        return (new DegreeAttributesBuilder())
                .setAvailFreqMaps(Map.of())
                .build();
    }

    private SrgAttributes getEmptySrgAttributes() {
        return new SrgAttributesBuilder().setAvailFreqMaps(Map.of()).build();
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

    private org.opendaylight.yang.gen
            .v1.http.org.openroadm.network.topology.rev200529.TerminationPoint1Builder createAnotherTerminationPoint(
            OpenroadmTpType openroadmTpType
    ) {
        return new org.opendaylight
                .yang.gen.v1.http.org.openroadm.network.topology.rev200529.TerminationPoint1Builder()
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

    private org.opendaylight.yang.gen.v1.http.org.openroadm.otn.network.topology.rev200529
            .TerminationPoint1Builder createOTNTerminationPoint(OpenroadmTpType openroadmTpType) {

        SupportedInterfaceCapability supIfCapa = new SupportedInterfaceCapabilityBuilder()
                .setIfCapType(IfOCHOTU4ODU4.class)
                .build();

        SupportedInterfaceCapability supIfCapa1 = new SupportedInterfaceCapabilityBuilder()
                .setIfCapType(If100GEODU4.class)
                .build();
        SupportedInterfaceCapability supIfCapa2 = new SupportedInterfaceCapabilityBuilder()
                .setIfCapType(If10GEODU2e.class)
                .build();
        SupportedInterfaceCapability supIfCapa3 = new SupportedInterfaceCapabilityBuilder()
                .setIfCapType(If1GEODU0.class)
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

        return new org.opendaylight.yang.gen.v1.http.org.openroadm.otn.network.topology.rev200529
                .TerminationPoint1Builder()
                .setTpSupportedInterfaces(tpSupIf)
                .setXpdrTpPortConnectionAttributes(xtpcaBldr.build());
    }

    private org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev200529
            .TerminationPoint1Builder createAnother2TerminationPoint(OpenroadmTpType openroadmTpType) {
        return new org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev200529
                .TerminationPoint1Builder()
                .setTpType(openroadmTpType).setOperationalState(State.InService)
                .setAdministrativeState(AdminStates.InService);
    }

}
