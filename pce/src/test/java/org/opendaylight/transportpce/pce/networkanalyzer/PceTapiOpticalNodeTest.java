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
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
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
import org.opendaylight.transportpce.pce.node.mccapabilities.McCapability;
import org.opendaylight.transportpce.pce.node.mccapabilities.NodeMcCapability;
import org.opendaylight.transportpce.test.AbstractTest;
import org.opendaylight.transportpce.test.converter.XMLDataObjectConverter;
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
import org.opendaylight.yangtools.binding.DataObjectIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class PceTapiOpticalNodeTest extends AbstractTest {
    private static final Logger LOG = LoggerFactory.getLogger(PceTapiOpticalNodeTest.class);
    private static final String TOPOLOGY_FILE = "src/test/resources/topologyData/refTopoTapiFull.xml";
    private static Context tapiContext;
//    private TapiOpticalNode tapiONroadmA;
    private static String version = "2.4.0";
    private static BigDecimal slotWidthGranularity = BigDecimal.valueOf(6.25E09);
    private static BigDecimal centralFreqGranularity = BigDecimal.valueOf(12.0E09);
    private static McCapability mcCapability = new NodeMcCapability(
        BigDecimal.valueOf(6.25E09), BigDecimal.valueOf(12.0E09), 1, 768);
    private static ServiceFormat serviceFormat = ServiceFormat.Ethernet;
    private String serviceType;
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
    //XPDR-A1-XPDR1+XPONDER
    private static Uuid xpdrA1xpdr1Id = new Uuid("4378fc29-6408-39ec-8737-5008c3dc49e5");
    //XPDR-C1-XPDR1+XPONDER
    private static Uuid xpdrC1xpdr1Id = new Uuid("1770bea4-b1da-3b20-abce-7d182c0ec0df");
    private static Uuid topoUuid = new Uuid("393f09a4-0a0b-3d82-a4f6-1fbbc14ca1a7");
    private static TapiOpticalNode tapiONroadmA;
    private static TapiOpticalNode tapiONroadmC;
    private static TapiOpticalNode tapiONspdrAx2;
    private static TapiOpticalNode tapiONspdrCx2;
    private static TapiOpticalNode tapiONspdrAx3;
    private static TapiOpticalNode tapiONspdrAx1;
    private static TapiOpticalNode tapiONspdrCx3;
    private static TapiOpticalNode tapiONspdrCx1;
    private static TapiOpticalNode tapiONxpdrAx1;
    private static TapiOpticalNode tapiONxpdrCx1;

    @Mock
    private static PortMapping portMapping;

    @BeforeAll
    static void setUp() throws ExecutionException, InterruptedException {
        Path topoFilePath = Path.of(TOPOLOGY_FILE);
        if (Files.exists(topoFilePath)) {
            String fileName = topoFilePath.getFileName().toString();
            try (InputStream targetStream = Files.newInputStream(topoFilePath)) {
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
                            DataObjectIdentifier.builder(Context.class).build(),
                            tapiContext);
                    newWriteOnlyTransaction.commit().get();
                }
            } catch (IOException e) {
                LOG.error("An error occured while reading file {}", TOPOLOGY_FILE, e);
            }
        } else {
            LOG.error("xml file {} not found at {}", topoFilePath.getFileName(), topoFilePath.toAbsolutePath());
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
        LOG.info("TESTPERFORMED:1");

    }


    @Test
    void testvalidateAZxponderForSPDR1andOtnBasedService() {
        serviceType = "10GE";
        anodeId = spdrSA1xpdr1Id;
        znodeId = spdrSC1xpdr1Id;
        //SPDR-SA1-XPDR1+DSR+XPDR1-CLIENT2 UUID = "82998ece-51cd-3c07-8b68-59d5ad5ff39e"
        aportId = new Uuid("82998ece-51cd-3c07-8b68-59d5ad5ff39e");
       //SPDR-SC1-XPDR1+DSR+XPDR1-CLIENT2 UUID = "b877533a-99ce-3128-bfb2-dc7e0a2122dd"
        zportId = new Uuid("b877533a-99ce-3128-bfb2-dc7e0a2122dd");
        try {
            generalSetUp();
        } catch (ExecutionException e) {
            LOG.error("Unable to get node from mdsal: ", e);
        }
        initializeAll();
        // For OTn Service no Optical Node generated
        PceTapiOpticalNode pceONspdrSA1 = tapiONspdrAx1.getXpdrOpticalNode();
        assertTrue(pceONspdrSA1 == null,
            "SPDR-SA1 Node is not a PceTapiOpticalNode, but a PceTapiOtnNode");
        LOG.info("TESTPERFORMED:2");
    }

    @Test
    void testvalidateAZxponderForSPDR1andPhtnService() {
        serviceType = "OTU4";
        anodeId = spdrSA1xpdr1Id;
        znodeId = spdrSC1xpdr1Id;
        //SPDR-SA1-XPDR1+PHOTONIC_MEDIA_OTS+XPDR1-NETWORK1 UUID = "21efd6a4-2d81-3cdb-aabb-b983fb61904e"
        aportId = new Uuid("21efd6a4-2d81-3cdb-aabb-b983fb61904e");
        //SPDR-SC1-XPDR1+PHOTONIC_MEDIA_OTS+XPDR1-NETWORK1 UUID = "ff10784b-3da2-3b88-88c3-27abc02b66fe"
        zportId = new Uuid("ff10784b-3da2-3b88-88c3-27abc02b66fe");
        try {
            generalSetUp();
        } catch (ExecutionException e) {
            LOG.error("Unable to get node from mdsal: ", e);
        }
        initializeAll();
        // For OTn Service no Optical Node generated
        PceTapiOpticalNode pceONspdrSA1 = tapiONspdrAx1.getXpdrOpticalNode();
        assertTrue(pceONspdrSA1 != null,
            "with OTU4 service, SPDR-SA1 Node is a PceTapiOpticalNode");
        assertTrue(pceONspdrSA1.getNodeId().getValue().toString().equals("SPDR-SA1-XPDR1+XPONDER"),
            "SPDR-SA1 Node shall have SPDR-SA1-XPDR1+XPONDER NodeId");
        assertTrue(pceONspdrSA1.getSupNetworkNodeId().equals("4e44bcc5-08d3-3fee-8fac-f021489e5a61"),
            "SPDR-SA1 Node shall have SPDR-SA1-XPDR1 Uuid as supporting Network NodeId");
        assertTrue(pceONspdrSA1.getSupClliNodeId().equals("4e44bcc5-08d3-3fee-8fac-f021489e5a61"),
            "SPDR-SA1 Node shall have SPDR-SA1-XPDR1 Uuid as supporting CLLI NodeId");
        assertTrue(pceONspdrSA1.getORNodeType().equals(OpenroadmNodeType.XPONDER),
            "SPDR-SA1 Node shall be of xponder OR Node Type");
        assertTrue(pceONspdrSA1.getPceNodeType().equals("optical"),
            "SPDR-SA1 Node shall be of PceNodeType optical");
        assertTrue(pceONspdrSA1.getSlotWidthGranularity().equals(BigDecimal.valueOf(6.25E+9)),
            "SPDR-SA1 Node shall have a 6.25 GHz slotWidth Granularity");
        assertTrue(pceONspdrSA1.getCentralFreqGranularity().equals(BigDecimal.valueOf(1.2E+10)),
            "SPDR-SA1 Node shall have a 12.5 GHz slotWidth Granularity");
        assertEquals(0, pceONspdrSA1.getXpdrAvailNW().size(),
            "SPDR A Muxponder shall have no OTS network Port available");
        assertTrue(pceONspdrSA1.getAvailableTribPorts() == null, "PceTapiOpticalNode does not return trib ports");
        assertTrue(pceONspdrSA1.getAvailableTribSlots() == null, "PceTapiOpticalNode does not return trib slots");
        LOG.info("TESTPERFORMED:3");
    }

    @Test
    void testvalidateAZxponderForSPDR1() {
        serviceType = "10GE";
        anodeId = spdrSA1xpdr1Id;
        znodeId = spdrSC1xpdr1Id;
        //SPDR-SA1-XPDR1+DSR+XPDR1-CLIENT2 UUID = "82998ece-51cd-3c07-8b68-59d5ad5ff39e"
        aportId = new Uuid("82998ece-51cd-3c07-8b68-59d5ad5ff39e");
       //SPDR-SC1-XPDR1+DSR+XPDR1-CLIENT2 UUID = "b877533a-99ce-3128-bfb2-dc7e0a2122dd"
        zportId = new Uuid("b877533a-99ce-3128-bfb2-dc7e0a2122dd");
        try {
            generalSetUp();
        } catch (ExecutionException e) {
            LOG.error("Unable to get node from mdsal: ", e);
        }
        initializeAll();

        PceTapiOpticalNode pceONspdrSA1 = tapiONspdrAx1.getXpdrOpticalNode();
        assertTrue(pceONspdrSA1 == null,
            "SPDR-SA1 Node is not a PceTapiOpticalNode, but a PceTapiOtnNode");
        LOG.info("TESTPERFORMED:4");
    }


    @Test
    void testvalidateAZxponderForXPDR1() {
        this.anodeId = xpdrA1xpdr1Id;
        this.znodeId = xpdrC1xpdr1Id;
        this.serviceType = "100GEt";
        //XPDR-A1-XPDR1+DSR+XPDR1-CLIENT1
        this.aportId = new Uuid("5dc0f306-763d-34a7-9419-d15e23891edd");
      //XPDR-A1-XPDR1+DSR+XPDR1-CLIENT1
        this.zportId = new Uuid("126d66e8-5028-3bb0-9769-9f3f966c6a99");
        try {
            generalSetUp();
        } catch (ExecutionException e) {
            LOG.error("Unable to get node from mdsal: ", e);
        }
        initializeAll();

        PceTapiOpticalNode pceONxpdrA1 = tapiONxpdrAx1.getXpdrOpticalNode();
        LOG.info("PTONLine272 Node Id = {}, SUpNetworkNodeId = {}, CLLI = {} NodeType = {}, slotwidthGran = {}"
            + " PceNodeType = {}, CentralFreqGranularity = {}, AvailableTribPorts = {}, AvailableTribSlots = {}",
            pceONxpdrA1.getNodeId().getValue(), pceONxpdrA1.getSupNetworkNodeId(), pceONxpdrA1.getSupClliNodeId(),
            pceONxpdrA1.getORNodeType(), pceONxpdrA1.getSlotWidthGranularity(), pceONxpdrA1.getPceNodeType(),
            pceONxpdrA1.getCentralFreqGranularity(), pceONxpdrA1.getAvailableTribPorts(),
            pceONxpdrA1.getAvailableTribSlots());
        assertTrue(pceONxpdrA1.getNodeId().getValue().toString().equals("XPDR-A1-XPDR1+XPONDER"),
            "XPDR-A1 Node shall have XPDR-A1-XPDR1+XPONDER NodeId");
        assertTrue(pceONxpdrA1.getSupNetworkNodeId().equals("4378fc29-6408-39ec-8737-5008c3dc49e5"),
            "XPDR-A1 Node shall have XPDR-A1-XPDR1 Uuid as supporting Network NodeId");
        assertTrue(pceONxpdrA1.getSupClliNodeId().equals("4378fc29-6408-39ec-8737-5008c3dc49e5"),
            "XPDR-A1 Node shall have XPDR-A1-XPDR1 Uuid as supporting CLLI NodeId");
        assertTrue(pceONxpdrA1.getORNodeType().equals(OpenroadmNodeType.XPONDER),
            "XPDR-A1 Node shall be of Xponder Type");
        assertTrue(pceONxpdrA1.getPceNodeType().equals("optical"),
            "XPDR-A1 Node shall be of PceNodeType optical");
        assertTrue(pceONxpdrA1.getSlotWidthGranularity().equals(BigDecimal.valueOf(6.25E+9)),
            "XPDR-A1 Node shall both have a 6.25 GHz slotWidth Granularity");
        assertTrue(pceONxpdrA1.getCentralFreqGranularity().equals(BigDecimal.valueOf(1.2E+10)),
            "XPDR-A1 Node shall both have a 12.5 GHz slotWidth Granularity");
        assertEquals(2, pceONxpdrA1.getXpdrAvailNW().size(),
            "XPDR A Transponder shall have 2 OTS network Port available N1 & N2");
        assertEquals(List.of("a1e0ffdf-3b42-3159-b66d-495aab187095", "082adca4-dfe8-3f52-b300-bea7112e1c2f"),
            pceONxpdrA1.getXpdrAvailNW(), "XPDR-A1-XPDR1+PHOTONIC_MEDIA_OTS+XPDR1-NETWORK1 & 2 shall be available");
        assertEquals("082adca4-dfe8-3f52-b300-bea7112e1c2f", pceONxpdrA1
            .getXpdrNWfromClient("5dc0f306-763d-34a7-9419-d15e23891edd"),
            "XPDR A Transponder network port returned for the client Port C1 shall be NETWORK1 portId");
        var freqBitSet = new BitSet(GridConstant.EFFECTIVE_BITS);
        freqBitSet.set(0, GridConstant.EFFECTIVE_BITS, true);
        assertTrue(pceONxpdrA1.getBitSetData().equals(freqBitSet),
            "Spectrum shall be fully available on XPDRA1-XPDR1 network port");
        assertTrue(pceONxpdrA1.getListOfNep().size() == 3,
            "XPDR A Transponder List of NEP shall contains 2 Network Neps (N1+N2) and 1 client NEP (C1)");
        LOG.info("TESTPERFORMED:5");
    }

    @Test
    void testvalidateXpdrForSPDRA_XPDR2_PortSpecified() {
        //Reinitializing Xponders according to new anodeId, znodeId, aportId, zportId
        anodeId = spdrSA1xpdr2Id;
        znodeId = spdrSC1xpdr2Id;
        serviceType = "OTU4";
        // SPDR-SA1-XPDR2+OTS+XPDR2-NETWORK2 4e4bf439-b457-3260-865c-db716e2647c2
        aportId = new Uuid("4e4bf439-b457-3260-865c-db716e2647c2");
        // SPDR-SC1-XPDR2+OTS+XPDR2-NETWORK2 ae235ae1-f17f-3619-aea9-e6b2a9e850c3
        zportId = new Uuid("ae235ae1-f17f-3619-aea9-e6b2a9e850c3");
        try {
            generalSetUp();
        } catch (ExecutionException e) {
            LOG.error("Unable to get node from mdsal: ", e);
        }
        initializeAll();
        PceTapiOpticalNode pceONspdrSA2 = tapiONspdrAx2.getXpdrOpticalNode();
        LOG.info("PTONTEST Line 407 : pceONspdrSA2 includes {} {} ", pceONspdrSA2.getXpdrAvailNW());
        assertEquals(5, pceONspdrSA2.getXpdrAvailNW().size(),
            "SPDR A Switchponder port OTS_N2 + All IODU port (N1/2/3/4) port shall be available");
        assertEquals(List.of("4e4bf439-b457-3260-865c-db716e2647c2", "4cd7d50f-3e0b-3a4b-a140-475912d5de7b",
            "6f4777d4-41f0-3833-b811-68b90903d834", "167043f5-0a0d-365b-ad81-5afdbcce29d5",
            "63860973-f9fb-3e4c-acd9-9bbe6898c643"), pceONspdrSA2.getXpdrAvailNW(),
            "SPDR A Switchponder port OTS_N2 + All IODU port (N1/2/3/4) port shall be available");
        var freqBitSet = new BitSet(GridConstant.EFFECTIVE_BITS);
        freqBitSet.set(0, GridConstant.EFFECTIVE_BITS, true);
        assertTrue(pceONspdrSA2.getBitSetData().equals(freqBitSet),
            "Spectrum shall be fully available on SPDRSA1-XPDR2-NETWORK2 network port");
        LOG.info("TESTPERFORMED:6");
    }

    @Test
    void testvalidateXpdrForSPDRA_XPDR2_NOPortSpecified() {
        //Reinitializing Xponders according to new anodeId, znodeId, aportId, zportId
        anodeId = spdrSA1xpdr2Id;
        znodeId = spdrSC1xpdr2Id;
        serviceType = "OTU4";
        try {
            generalSetUp();
        } catch (ExecutionException e) {
            LOG.error("Unable to get node from mdsal: ", e);
        }
        initializeAll();
        PceTapiOpticalNode pceONspdrSA2 = tapiONspdrAx2.getXpdrOpticalNode();
        LOG.info("PTONTEST Line 436 : pceONspdrSA2 includes {} ", pceONspdrSA2.getXpdrAvailNW());
        assertEquals(7, pceONspdrSA2.getXpdrAvailNW().size(), "SPDR A Switchponder has 3 OTS network Ports available as"
            + "far as no specific port is specified in the request and the OTS Network port N1 is used"
            + " +  4 iODU port corresponding to the network ports ");
        assertEquals(List.of(
            "4e4bf439-b457-3260-865c-db716e2647c2", "310c8df1-c81a-3813-8da3-6b6fdd82c0c6",
            "f87eda44-31ca-319a-be5c-3615216fb4b1",
            "4cd7d50f-3e0b-3a4b-a140-475912d5de7b", "6f4777d4-41f0-3833-b811-68b90903d834",
            "167043f5-0a0d-365b-ad81-5afdbcce29d5", "63860973-f9fb-3e4c-acd9-9bbe6898c643"),
            pceONspdrSA2.getXpdrAvailNW(),
            "3 OTS network Ports + 4 iODU NW ports shall be available");
        var freqBitSet = new BitSet(GridConstant.EFFECTIVE_BITS);
        freqBitSet.set(0, GridConstant.EFFECTIVE_BITS, true);
        assertTrue(pceONspdrSA2.getBitSetData().equals(freqBitSet),
            "Spectrum shall be fully available on valid SPDR-SA1-XPDR2 network ports");
        LOG.info("TESTPERFORMED:7");
    }

    private TapiOpticalNode getTapiOpticalNodeFromId(Uuid nodeId) throws ExecutionException {
        DataObjectIdentifier<Node> nodeIID = DataObjectIdentifier.builder(Context.class)
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
            centralFreqGranularity, anodeId, znodeId, aportId, zportId, serviceFormat, mcCapability);
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
            tapiONxpdrAx1 = getTapiOpticalNodeFromId(xpdrA1xpdr1Id);
            tapiONxpdrCx1 = getTapiOpticalNodeFromId(xpdrC1xpdr1Id);
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
        tapiONxpdrAx1.initialize();
        tapiONxpdrCx1.initialize();
    }

}
