/*
 * Copyright © 2020 Orange, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.pce.networkanalyzer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import com.google.common.util.concurrent.ListenableFuture;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.BitSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import org.eclipse.jdt.annotation.NonNull;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.opendaylight.mdsal.binding.api.WriteTransaction;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.transportpce.common.fixedflex.GridConstant;
import org.opendaylight.transportpce.common.mapping.PortMapping;
import org.opendaylight.transportpce.common.network.NetworkTransactionImpl;
import org.opendaylight.transportpce.common.network.NetworkTransactionService;
import org.opendaylight.transportpce.test.AbstractTest;
import org.opendaylight.transportpce.test.converter.DataObjectConverter;
import org.opendaylight.transportpce.test.converter.XMLDataObjectConverter;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.node.types.rev210528.NodeTypes;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.types.rev230526.OpenroadmNodeType;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.format.rev191129.ServiceFormat;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.Context;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.Uuid;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.context.TopologyContext;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.topology.Node;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.topology.NodeKey;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.topology.context.Topology;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.topology.context.TopologyKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class PceTapiOpticalNodeTest extends AbstractTest {
    private static final Logger LOG = LoggerFactory.getLogger(TapiOpticalNodeTest.class);
    private static final String TOPOLOGY_FILE = "src/test/resources/topologyData/refTopoTapiFull.xml";
    private static Context tapiContext;
//    private TapiOpticalNode tapiONroadmA;
    private static String version = "2.4.0";
    private static BigDecimal slotWidthGranularity = BigDecimal.valueOf(6.25E09);
    private static BigDecimal centralFreqGranularity = BigDecimal.valueOf(12.0E09);
    private static ServiceFormat serviceFormat = ServiceFormat.Ethernet;
    private static String serviceType = "100GE";
    private Uuid anodeId;
    private Uuid znodeId;
    private Uuid aportId;
    private Uuid zportId;
    private static Uuid spdrSA1xpdr2Id = new Uuid("38c114ae-9c0e-3068-bb27-db2dbd81220b");
    //SPDR-SA1-XPDR3+XPONDER
    private static Uuid spdrSA1xpdr3Id = new Uuid("4582e51f-2b2d-3b70-b374-86c463062710");
    //SPDR-SA1-XPDR1+XPONDER
    private static Uuid spdrSA1xpdr1Id = new Uuid("4e44bcc5-08d3-3fee-8fac-f021489e5a61");
    //SPDR-SC1-XPDR1+XPONDER
    private static Uuid spdrSC1xpdr1Id = new Uuid("215ee18f-7869-3492-94d2-0f24ed0a3023");
    //SPDR-SC1-XPDR3+XPONDER
    private static Uuid spdrSC1xpdr3Id = new Uuid("c1f06957-c0b9-32be-8492-e278b2d4a3aa");
   //SPDR-SC1-XPDR2+XPONDER
    private static Uuid spdrSC1xpdr2Id = new Uuid("d852c340-77db-3f9a-96e8-cb4de8e1004a");
    //ROADM-C1+PHOTONIC_MEDIA
    private static Uuid roadmCId = new Uuid("4986dca9-2d59-3d79-b306-e11802bcf1e6");
    //ROADM-A1+PHOTONIC_MEDIA
    private static Uuid roadmAId = new Uuid("3b726367-6f2d-3e3f-9033-d99b61459075");
    private static Uuid topoUuid = new Uuid("393f09a4-0a0b-3d82-a4f6-1fbbc14ca1a7");
    private TapiOpticalNode tapiONroadmA;
    private TapiOpticalNode tapiONroadmC;
    private TapiOpticalNode tapiONspdrAx2;
    private TapiOpticalNode tapiONspdrCx2;
    private TapiOpticalNode tapiONspdrAx3;
    private TapiOpticalNode tapiONspdrAx1;
    private TapiOpticalNode tapiONspdrCx3;
    private TapiOpticalNode tapiONspdrCx1;

    @Mock
    private static PortMapping portMapping;

    @BeforeAll
    void setUp() {
        DataObjectConverter dataObjectConverter = XMLDataObjectConverter
            .createWithDataStoreUtil(getDataStoreContextUtil());
        try (Reader reader = new FileReader(TOPOLOGY_FILE, StandardCharsets.UTF_8)) {
            NormalizedNode normalizedNode = dataObjectConverter
                .transformIntoNormalizedNode(reader).orElseThrow();
            tapiContext = (Context) getDataStoreContextUtil()
                .getBindingDOMCodecServices().fromNormalizedNode(YangInstanceIdentifier
                    .of(Context.QNAME), normalizedNode)
                .getValue();
            @NonNull
            WriteTransaction newWriteOnlyTransaction = getDataBroker().newWriteOnlyTransaction();
            newWriteOnlyTransaction
                .put(LogicalDatastoreType.OPERATIONAL,
                    InstanceIdentifier.create(Context.class),
                    tapiContext);
            newWriteOnlyTransaction.commit().get();
        } catch (ExecutionException e) {
            LOG.error("Cannot load TapiContext including Full ML Topology ", e);
            fail("Cannot load Topology ");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            LOG.error("Cannot load TapiContext including Full ML Topology ", e);
            fail("Cannot load Topology ");
        } catch (IOException e) {
            LOG.error("Cannot load TapiContext including Full ML Topology ", e);
            fail("Cannot load Topology ");
        }
        anodeId = spdrSA1xpdr1Id;
        znodeId = spdrSC1xpdr1Id;
        //SPDR-SA1-XPDR1+DSR+XPDR1-CLIENT1
        aportId = new Uuid("c6cd334c-51a1-3995-bed3-5cf2b7445c04");
        //SPDR-SC1-XPDR1+DSR+XPDR1-CLIENT1"
        zportId = new Uuid("50b7521a-4a38-358f-9846-45c55813416a");
        try {
            tapiONroadmA = getTapiOpticalNodeFromId(roadmAId);
            tapiONroadmC = getTapiOpticalNodeFromId(roadmCId);
            tapiONspdrAx1 = getTapiOpticalNodeFromId(spdrSA1xpdr1Id);
            tapiONspdrCx1 = getTapiOpticalNodeFromId(spdrSC1xpdr1Id);
        } catch (ExecutionException e) {
            LOG.error("Unable to get node from mdsal: ", e);
        }

    }

    @Test
    void testInitializeForXPDR1() {
        tapiONroadmA.initialize();
        tapiONroadmC.initialize();
        tapiONspdrAx1.initialize();
        tapiONspdrCx1.initialize();

        //Testing private method qualifyNode()
        assertTrue(tapiONroadmA.getCommonNodeType().equals(NodeTypes.Rdm));
        assertTrue(tapiONspdrAx1.getCommonNodeType().equals(NodeTypes.Xpdr));
        assertTrue(tapiONspdrCx1.getCommonNodeType().equals(NodeTypes.Xpdr));
    }

        //Testing private method createNrgPartialMesh() / createIrgPartialMesh()

    @Test
    void testSplitRoadmNodes() {
        //Testing private method splitDegNodes()
        List<PceTapiOpticalNode> pceTapiDegNodes = tapiONroadmA.getPceNodeMap().entrySet().stream()
            .map(Map.Entry::getValue)
            .filter(pton -> pton.getORNodeType().equals(OpenroadmNodeType.DEGREE))
            .collect(Collectors.toList());
        assertEquals(2, pceTapiDegNodes.size(),
            "ROADM A shall includes 2 degree Nodes (DEG1&DEG2) part of the PceNodeMap");
        assertTrue(pceTapiDegNodes.stream()
            .filter(rdm -> rdm.getNodeId().toString().equals("ROADMA-A1-PHOTONIC_LAYER-DEG1"))
            .findFirst().orElseThrow() != null,
            "ROADM A shall includes ROADMA-A1-PHOTONIC_LAYER-DEG1");
        assertTrue(pceTapiDegNodes.stream()
            .filter(rdm -> rdm.getNodeId().toString().equals("ROADMA-A1-PHOTONIC_LAYER-DEG2"))
            .findFirst().orElseThrow() != null,
            "ROADM A shall includes ROADMA-A1-PHOTONIC_LAYER-DEG2");
        assertEquals(2, pceTapiDegNodes.stream()
            .filter(rdm -> rdm.getSupNetworkNodeId().equals("ROADMA-A1-PHOTONIC_LAYER"))
            .collect(Collectors.toList()).size(),
            "ROADM A Degrees shall have the supported TapiOpticalNode name as device Id: ROADMA-A1-PHOTONIC_LAYER");
        assertEquals(2, pceTapiDegNodes.stream()
            .filter(rdm -> rdm.getSupClliNodeId().equals("ROADMA-A1-PHOTONIC_LAYER"))
            .collect(Collectors.toList()).size(),
            "ROADM A Degrees shall have the supported TapiOpticalNode name as device Id: ROADMA-A1-PHOTONIC_LAYER");
        assertEquals(2, pceTapiDegNodes.stream()
            .filter(rdm -> rdm.getORNodeType().equals(OpenroadmNodeType.DEGREE))
            .collect(Collectors.toList()).size(),
            "ROADM A Degrees shall both be of DEGREE type");
        assertEquals(2, pceTapiDegNodes.stream()
            .filter(rdm -> rdm.getSlotWidthGranularity().equals(BigDecimal.valueOf(GridConstant.GRANULARITY)))
            .collect(Collectors.toList()).size(),
            "ROADM A Degrees shall both have a 6.25 GHz slotWidth Granularity");
        assertEquals(2, pceTapiDegNodes.stream()
            .filter(rdm -> rdm.getCentralFreqGranularity().equals(BigDecimal.valueOf(GridConstant.GRANULARITY * 2)))
            .collect(Collectors.toList()).size(),
            "ROADM A Degrees shall both have a 12.5 GHz slotWidth Granularity");
        //Testing private method splitSrgNodes()
        List<PceTapiOpticalNode> pceTapiSrgNodes = tapiONroadmA.getPceNodeMap().entrySet().stream()
            .map(Map.Entry::getValue)
            .filter(pton -> pton.getORNodeType().equals(OpenroadmNodeType.SRG))
            .collect(Collectors.toList());
        assertEquals(2, pceTapiSrgNodes.size(),
            "ROADM A shall includes 2 SRG Nodes (SRG1&SRG3) part of the PceNodeMap");
        assertTrue(pceTapiSrgNodes.stream()
            .filter(rdm -> rdm.getNodeId().toString().equals("ROADMA-A1-PHOTONIC_LAYER-SRG1"))
            .findFirst().orElseThrow() != null,
            "ROADM A shall includes ROADMA-A1-PHOTONIC_LAYER-SRG1");
        assertTrue(pceTapiSrgNodes.stream()
            .filter(rdm -> rdm.getNodeId().toString().equals("ROADMA-A1-PHOTONIC_LAYER-SRG2"))
            .findFirst().orElseThrow() != null,
            "ROADM A shall includes ROADMA-A1-PHOTONIC_LAYER-SRG2");
        assertEquals(2, pceTapiSrgNodes.stream()
            .filter(rdm -> rdm.getSupNetworkNodeId().equals("ROADMA-A1-PHOTONIC_LAYER"))
            .collect(Collectors.toList()).size(),
            "ROADM A Degrees shall have the supported TapiOpticalNode name as device Id: ROADMA-A1-PHOTONIC_LAYER");
        assertEquals(2, pceTapiSrgNodes.stream()
            .filter(rdm -> rdm.getSupClliNodeId().equals("ROADMA-A1-PHOTONIC_LAYER"))
            .collect(Collectors.toList()).size(),
            "ROADM A Degrees shall have the supported TapiOpticalNode name as device Id: ROADMA-A1-PHOTONIC_LAYER");
        assertEquals(2, pceTapiSrgNodes.stream()
            .filter(rdm -> rdm.getORNodeType().equals(OpenroadmNodeType.SRG))
            .collect(Collectors.toList()).size(),
            "ROADM A SRGs shall both be of SRG type");
        assertEquals(2, pceTapiSrgNodes.stream()
            .filter(rdm -> rdm.getSlotWidthGranularity().equals(BigDecimal.valueOf(GridConstant.GRANULARITY)))
            .collect(Collectors.toList()).size(),
            "ROADM A Degrees shall both have a 6.25 GHz slotWidth Granularity");
        assertEquals(2, pceTapiSrgNodes.stream()
            .filter(rdm -> rdm.getCentralFreqGranularity().equals(BigDecimal.valueOf(GridConstant.GRANULARITY * 2)))
            .collect(Collectors.toList()).size(),
            "ROADM A Degrees shall both have a 12.5 GHz slotWidth Granularity");
        var freqBitSet = new BitSet(GridConstant.EFFECTIVE_BITS);
        freqBitSet.set(0, GridConstant.EFFECTIVE_BITS, true);
        assertEquals(2, pceTapiSrgNodes.stream().filter(rdm -> rdm.getBitSetData().equals(freqBitSet))
            .collect(Collectors.toList()).size(),
            "Spectrum shall be fully available for both SRG nodes");
        assertEquals(2, pceTapiDegNodes.stream().filter(rdm -> rdm.getBitSetData().equals(freqBitSet))
            .collect(Collectors.toList()).size(),
            "Spectrum shall be fully available for both DEG nodes");

    }

    @Test
    void testvalidateAZxponderForXPDR1() {
        List<PceTapiOpticalNode> pceTapiXpdrNodesSpdrAx2 = tapiONspdrAx1.getPceNodeMap().entrySet().stream()
            .map(Map.Entry::getValue)
            .filter(pton -> pton.getORNodeType().equals(OpenroadmNodeType.MUXPDR))
            .collect(Collectors.toList());
        assertEquals(1, pceTapiXpdrNodesSpdrAx2.size(),
            "SPDR A is a Muxponder and shall be the unique element of the PceNodeMap");
        List<PceTapiOpticalNode> pceTapiXpdrNodesSpdrCx2 = tapiONspdrCx1.getPceNodeMap().entrySet().stream()
            .map(Map.Entry::getValue)
            .filter(pton -> pton.getORNodeType().equals(OpenroadmNodeType.MUXPDR))
            .collect(Collectors.toList());
        assertEquals(1, pceTapiXpdrNodesSpdrCx2.size(),
            "SPDR C is a Muxponder and shall be the unique element of the PceNodeMap");
        PceTapiOpticalNode pceONspdrSA1 = tapiONspdrAx1.getPceNodeMap().entrySet().iterator().next().getValue();
        assertEquals(1, pceONspdrSA1.getXpdrAvailNW().size(),
            "SPDR A Muxponder shall have 1 network Port available");
        assertEquals("21efd6a4-2d81-3cdb-aabb-b983fb61904e", pceONspdrSA1.getXpdrAvailNW().iterator().next(),
            "SPDR-SA1-XPDR1+PHOTONIC_MEDIA_OTS network Port shall be available");
        assertEquals("c6cd334c-51a1-3995-bed3-5cf2b7445c04", pceONspdrSA1
            .getXpdrClient("21efd6a4-2d81-3cdb-aabb-b983fb61904e"),
            "SPDR A Muxponder client port returned for the network Port shall be aportId");
        var freqBitSet = new BitSet(GridConstant.EFFECTIVE_BITS);
        freqBitSet.set(0, GridConstant.EFFECTIVE_BITS, true);
        assertTrue(pceONspdrSA1.getBitSetData().equals(freqBitSet),
            "Spectrum shall be fully available on SPDRSA1-XPDR1 network port");
    }

    @Test
    void testvalidateAZxponderForXPDR2() {
        //Reinitializing Xponders according to new anodeId, znodeId, aportId, zportId
        anodeId = spdrSA1xpdr2Id;
        znodeId = spdrSC1xpdr2Id;
        //SPDR-SA1-XPDR2+DSR+XPDR2-CLIENT1
        aportId = new Uuid("935d763e-297c-332f-8edc-65496a2a607c");
        //SPDR-SC1-XPDR2+DSR+XPDR2-CLIENT1"
        zportId = new Uuid("eddd56d6-0aa7-3583-9b1b-40470ed2cf96");
        try {
            tapiONspdrAx2 = getTapiOpticalNodeFromId(spdrSA1xpdr2Id);
            tapiONspdrCx2 = getTapiOpticalNodeFromId(spdrSC1xpdr2Id);
        } catch (ExecutionException e) {
            LOG.error("Unable to get node from mdsal: ", e);
        }
        tapiONspdrAx2.initialize();
        tapiONspdrCx2.initialize();

        //Testing private method qualifyNode()
        assertTrue(tapiONspdrAx2.getCommonNodeType().equals(NodeTypes.Xpdr));
        assertTrue(tapiONspdrCx2.getCommonNodeType().equals(NodeTypes.Xpdr));

        List<PceTapiOpticalNode> pceTapiXpdrNodesSpdrAx2 = tapiONspdrAx2.getPceNodeMap().entrySet().stream()
            .map(Map.Entry::getValue)
            .filter(pton -> pton.getORNodeType().equals(OpenroadmNodeType.SWITCH))
            .collect(Collectors.toList());
        assertEquals(1, pceTapiXpdrNodesSpdrAx2.size(),
            "SPDR A is a SWITCH and shall be the unique element of the PceNodeMap");
        List<PceTapiOpticalNode> pceTapiXpdrNodesSpdrCx2 = tapiONspdrCx2.getPceNodeMap().entrySet().stream()
            .map(Map.Entry::getValue)
            .filter(pton -> pton.getORNodeType().equals(OpenroadmNodeType.SWITCH))
            .collect(Collectors.toList());
        assertEquals(1, pceTapiXpdrNodesSpdrCx2.size(),
            "SPDR C is a SWITCH and shall be the unique element of the PceNodeMap");
        PceTapiOpticalNode pceONspdrSA2 = tapiONspdrAx2.getPceNodeMap().entrySet().iterator().next().getValue();
        assertEquals(4, pceONspdrSA2.getXpdrAvailNW().size(),
            "SPDR A Switchponder shall have 4 network Ports available");
        assertEquals(List.of("ff69716d-325d-35fc-baa9-0c4bcd992bbd",
            "4e4bf439-b457-3260-865c-db716e2647c2", "310c8df1-c81a-3813-8da3-6b6fdd82c0c6",
            "f87eda44-31ca-319a-be5c-3615216fb4b1"), pceONspdrSA2.getXpdrAvailNW(),
            "4 SPDR-SA1-XPDR2+PHOTONIC_MEDIA_OTS network Ports shall be available");
        assertEquals("935d763e-297c-332f-8edc-65496a2a607c", pceONspdrSA2
            .getXpdrClient("ff69716d-325d-35fc-baa9-0c4bcd992bbd"),
            "SPDR A Muxponder client port returned for the network Port shall be aportId");
        var freqBitSet = new BitSet(GridConstant.EFFECTIVE_BITS);
        freqBitSet.set(0, GridConstant.EFFECTIVE_BITS, true);
        assertTrue(pceONspdrSA2.getBitSetData().equals(freqBitSet),
            "Spectrum shall be fully available on SPDRSA1-XPDR2 network ports");
    }

    private TapiOpticalNode getTapiOpticalNodeFromId(Uuid nodeId) throws ExecutionException {
        InstanceIdentifier<Node> nodeIID = InstanceIdentifier.builder(Context.class)
            .augmentation(org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.Context1.class)
            .child(TopologyContext.class)
            .child(Topology.class, new TopologyKey(topoUuid))
            .child(Node.class, new NodeKey(nodeId))
            .build();
        NetworkTransactionService netTransServ = new NetworkTransactionImpl(getDataBroker());
        ListenableFuture<Optional<Node>> nodeFuture =
            netTransServ.read(LogicalDatastoreType.OPERATIONAL, nodeIID);
        Node node;
        try {
            node = nodeFuture.get().orElseThrow();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ExecutionException("Unable to get node from mdsal : " + nodeId, e);
        } catch (ExecutionException e) {
            throw new ExecutionException("Unable to get node from mdsal: " + nodeId, e);
        } catch (NoSuchElementException e) {
            return null;
        }
        return new TapiOpticalNode(serviceType, portMapping, node, version,slotWidthGranularity,
            centralFreqGranularity, anodeId, znodeId, aportId, zportId, serviceFormat);
    }



}
