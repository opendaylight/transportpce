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

import com.google.common.util.concurrent.ListenableFuture;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
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
import org.opendaylight.yangtools.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
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
    private String serviceType;
    private static Uuid anodeId;
    private static Uuid znodeId;
    private static Uuid aportId;
    private static Uuid zportId;
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
    private static TapiOpticalNode tapiONroadmA;
    private static TapiOpticalNode tapiONroadmC;
    private static TapiOpticalNode tapiONspdrAx2;
    private static TapiOpticalNode tapiONspdrCx2;
    private static TapiOpticalNode tapiONspdrAx3;
    private static TapiOpticalNode tapiONspdrAx1;
    private static TapiOpticalNode tapiONspdrCx3;
    private static TapiOpticalNode tapiONspdrCx1;

    @Mock
    private static PortMapping portMapping;

    @BeforeAll
    static void setUp() throws ExecutionException, InterruptedException {
        File topoFile = new File(TOPOLOGY_FILE);
        if (topoFile.exists()) {
            String fileName = topoFile.getName();
            try (InputStream targetStream = new FileInputStream(topoFile)) {
                Optional<NormalizedNode> transformIntoNormalizedNode = XMLDataObjectConverter
                        .createWithDataStoreUtil(getDataStoreContextUtil()).transformIntoNormalizedNode(targetStream);
                if (!transformIntoNormalizedNode.isPresent()) {
                    throw new IllegalStateException(String.format(
                        "Could not transform the input %s into normalized nodes", fileName));
                }
                Optional<DataObject> dataObject = XMLDataObjectConverter
                    .createWithDataStoreUtil(getDataStoreContextUtil())
                    .getDataObject(transformIntoNormalizedNode.orElseThrow(), Context.QNAME);
                if (!dataObject.isPresent()) {
                    throw new IllegalStateException("Could not transform normalized nodes into data object");
                } else {
                    tapiContext = (Context) dataObject.orElseThrow();
                    @NonNull
                    WriteTransaction newWriteOnlyTransaction = getDataBroker().newWriteOnlyTransaction();
                    newWriteOnlyTransaction
                        .put(LogicalDatastoreType.OPERATIONAL,
                            InstanceIdentifier.create(Context.class),
                            tapiContext);
                    newWriteOnlyTransaction.commit().get();
                }
            } catch (IOException e) {
                LOG.error("An error occured while reading file {}", TOPOLOGY_FILE, e);
            }
        } else {
            LOG.error("xml file {} not found at {}", topoFile.getName(), topoFile.getAbsolutePath());
        }
        if (tapiContext == null) {
            throw new IllegalStateException("tapiContext is null cannot write it to datastore");
        }

    }

    @Test
    void testSplitRoadmNodes() {
        serviceType = "10GE";
        anodeId = spdrSA1xpdr1Id;
        znodeId = spdrSC1xpdr1Id;
        //SPDR-SA1-XPDR1+DSR+XPDR1-CLIENT1
        aportId = new Uuid("c6cd334c-51a1-3995-bed3-5cf2b7445c04");
        //SPDR-SC1-XPDR1+DSR+XPDR1-CLIENT1"
        zportId = new Uuid("50b7521a-4a38-358f-9846-45c55813416a");
        try {
            generalSetUp();
        } catch (ExecutionException e) {
            LOG.error("Unable to get node from mdsal: ", e);
        }
        initializeAll();
        //Testing private method splitDegNodes()
        List<PceTapiOpticalNode> pceTapiDegNodes = tapiONroadmA.getPceNodeMap().entrySet().stream()
            .map(Map.Entry::getValue)
            .filter(pton -> pton.getORNodeType().equals(OpenroadmNodeType.DEGREE))
            .collect(Collectors.toList());
        assertEquals(2, pceTapiDegNodes.size(),
            "ROADM A shall includes 2 degree Nodes (DEG1&DEG2) part of the PceNodeMap");
        LOG.info("pceTON-Line159 : pceTapiDegNodesMap include {}", pceTapiDegNodes.stream()
            .map(PceTapiOpticalNode::getNodeId).collect(Collectors.toList()));
        assertTrue(pceTapiDegNodes.stream()
            .filter(rdm -> rdm.getNodeId().getValue().equals("ROADM-A1+PHOTONIC_MEDIA+DEG1"))
            .findFirst().orElseThrow() != null,
            "ROADM A shall includes ROADM-A1+PHOTONIC_MEDIA+DEG1");
        assertTrue(pceTapiDegNodes.stream()
            .filter(rdm -> rdm.getNodeId().getValue().equals("ROADM-A1+PHOTONIC_MEDIA+DEG2"))
            .findFirst().orElseThrow() != null,
            "ROADM A shall includes ROADM-A1+PHOTONIC_MEDIA+DEG2");
        assertEquals(2, pceTapiDegNodes.stream()
            .filter(rdm -> rdm.getSupNetworkNodeId().equals("ROADM-A1+PHOTONIC_MEDIA"))
            .collect(Collectors.toList()).size(),
            "ROADM A Degrees shall have the supported TapiOpticalNode name as device Id: ROADM-A1+PHOTONIC_MEDIA");
        assertEquals(2, pceTapiDegNodes.stream()
            .filter(rdm -> rdm.getSupClliNodeId().equals("ROADM-A1+PHOTONIC_MEDIA"))
            .collect(Collectors.toList()).size(),
            "ROADM A Degrees shall have the supported TapiOpticalNode name as device Id: ROADM-A1+PHOTONIC_MEDIA");
        assertEquals(2, pceTapiDegNodes.stream()
            .filter(rdm -> rdm.getORNodeType().equals(OpenroadmNodeType.DEGREE))
            .collect(Collectors.toList()).size(),
            "ROADM A Degrees shall both be of DEGREE type");
        LOG.info("pceTON-Line181 : pceTapiDegNodesMap include {}", pceTapiDegNodes.stream()
            .map(PceTapiOpticalNode::getSlotWidthGranularity).collect(Collectors.toList()));
        assertEquals(2, pceTapiDegNodes.stream()
            .filter(rdm -> rdm.getSlotWidthGranularity().equals(BigDecimal.valueOf(6.25E+9)))
            .collect(Collectors.toList()).size(),
            "ROADM A Degrees shall both have a 6.25 GHz slotWidth Granularity");
        LOG.info("pceTON-Line187 : pceTapiDegNodesMap include {}", pceTapiDegNodes.stream()
            .map(PceTapiOpticalNode::getCentralFreqGranularity).collect(Collectors.toList()));
        assertEquals(2, pceTapiDegNodes.stream()
            .filter(rdm -> rdm.getCentralFreqGranularity().equals(BigDecimal.valueOf(1.2E+10)))
            .collect(Collectors.toList()).size(),
            "ROADM A Degrees shall both have a 12.5 GHz slotWidth Granularity");
        //Testing private method splitSrgNodes()
        List<PceTapiOpticalNode> pceTapiSrgNodes = tapiONroadmA.getPceNodeMap().entrySet().stream()
            .map(Map.Entry::getValue)
            .filter(pton -> pton.getORNodeType().equals(OpenroadmNodeType.SRG))
            .collect(Collectors.toList());
        assertEquals(2, pceTapiSrgNodes.size(),
            "ROADM A shall includes 2 SRG Nodes (SRG1&SRG3) part of the PceNodeMap");
        LOG.info("pceTON-Line200 : pceTapiDegNodesMap include {}", pceTapiSrgNodes.stream()
            .map(PceTapiOpticalNode::getNodeId).collect(Collectors.toList()));
        assertTrue(pceTapiSrgNodes.stream()
            .filter(rdm -> rdm.getNodeId().getValue().equals("ROADM-A1+PHOTONIC_MEDIA+SRG1"))
            .findFirst().orElseThrow() != null,
            "ROADM A shall includes ROADM-A1+PHOTONIC_MEDIA+SRG1");
        assertTrue(pceTapiSrgNodes.stream()
            .filter(rdm -> rdm.getNodeId().getValue().equals("ROADM-A1+PHOTONIC_MEDIA+SRG3"))
            .findFirst().orElseThrow() != null,
            "ROADM A shall includes ROADM-A1+PHOTONIC_MEDIA+SRG2");
        assertEquals(2, pceTapiSrgNodes.stream()
            .filter(rdm -> rdm.getSupNetworkNodeId().equals("ROADM-A1+PHOTONIC_MEDIA"))
            .collect(Collectors.toList()).size(),
            "ROADM A Degrees shall have the supported TapiOpticalNode name as device Id: ROADM-A1+PHOTONIC_MEDIA");
        assertEquals(2, pceTapiSrgNodes.stream()
            .filter(rdm -> rdm.getSupClliNodeId().equals("ROADM-A1+PHOTONIC_MEDIA"))
            .collect(Collectors.toList()).size(),
            "ROADM A Degrees shall have the supported TapiOpticalNode name as device Id: ROADM-A1+PHOTONIC_MEDIA");
        assertEquals(2, pceTapiSrgNodes.stream()
            .filter(rdm -> rdm.getORNodeType().equals(OpenroadmNodeType.SRG))
            .collect(Collectors.toList()).size(),
            "ROADM A SRGs shall both be of SRG type");
        assertEquals(2, pceTapiSrgNodes.stream()
            .filter(rdm -> rdm.getSlotWidthGranularity().equals(BigDecimal.valueOf(6.25E+9)))
            .collect(Collectors.toList()).size(),
            "ROADM A Degrees shall both have a 6.25 GHz slotWidth Granularity");
        assertEquals(2, pceTapiSrgNodes.stream()
            .filter(rdm -> rdm.getCentralFreqGranularity().equals(BigDecimal.valueOf(1.2E+10)))
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
        serviceType = "10GE";
        anodeId = spdrSA1xpdr1Id;
        znodeId = spdrSC1xpdr1Id;
        //SPDR-SA1-XPDR1+DSR+XPDR1-CLIENT1
        aportId = new Uuid("c6cd334c-51a1-3995-bed3-5cf2b7445c04");
        //SPDR-SC1-XPDR1+DSR+XPDR1-CLIENT1"
        zportId = new Uuid("50b7521a-4a38-358f-9846-45c55813416a");
        try {
            generalSetUp();
        } catch (ExecutionException e) {
            LOG.error("Unable to get node from mdsal: ", e);
        }
        initializeAll();
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
        return new TapiOpticalNode(this.serviceType, portMapping, node, version,slotWidthGranularity,
            centralFreqGranularity, anodeId, znodeId, aportId, zportId, serviceFormat);
    }


    private void generalSetUp() throws ExecutionException {
        try {
            tapiONroadmA = getTapiOpticalNodeFromId(roadmAId);
            tapiONroadmC = getTapiOpticalNodeFromId(roadmCId);
            xpdrSetUp();
        } catch (ExecutionException e) {
            LOG.error("Unable to get node from mdsal: ", e);
        }
    }

    private void xpdrSetUp() throws ExecutionException {
        try {
            tapiONspdrAx1 = getTapiOpticalNodeFromId(spdrSA1xpdr1Id);
            tapiONspdrCx1 = getTapiOpticalNodeFromId(spdrSC1xpdr1Id);
            tapiONspdrAx2 = getTapiOpticalNodeFromId(spdrSA1xpdr2Id);
            tapiONspdrCx2 = getTapiOpticalNodeFromId(spdrSC1xpdr2Id);
        } catch (ExecutionException e) {
            LOG.error("Unable to get node from mdsal: ", e);
        }
    }

    private void initializeAll() {
        tapiONroadmA.initialize();
        tapiONroadmC.initialize();
        initializeXpdrs();
    }


    private void initializeXpdrs() {
        tapiONspdrAx1.initialize();
        tapiONspdrCx1.initialize();
        tapiONspdrAx2.initialize();
        tapiONspdrCx2.initialize();
    }

}
