/*
 * Copyright © 2020 Orange, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.pce.networkanalyzer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.common.util.concurrent.ListenableFuture;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
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
import org.opendaylight.transportpce.common.mapping.PortMapping;
import org.opendaylight.transportpce.common.network.NetworkTransactionImpl;
import org.opendaylight.transportpce.common.network.NetworkTransactionService;
import org.opendaylight.transportpce.pce.node.mccapabilities.McCapability;
import org.opendaylight.transportpce.pce.node.mccapabilities.NodeMcCapability;
import org.opendaylight.transportpce.test.AbstractTest;
import org.opendaylight.transportpce.test.converter.XMLDataObjectConverter;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.types.rev250110.OpenroadmLinkType;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.format.rev191129.ServiceFormat;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.AdministrativeState;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.Context;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.OperationalState;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.Uuid;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev221121.connectivity.context.Connection;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev221121.connectivity.context.ConnectionKey;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev221121.context.ConnectivityContext;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.context.TopologyContext;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.topology.Link;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.topology.LinkKey;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.topology.Node;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.topology.NodeKey;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.topology.context.Topology;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.topology.context.TopologyKey;
import org.opendaylight.yangtools.binding.DataObject;
import org.opendaylight.yangtools.binding.DataObjectIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PceTapiLinkTest  extends AbstractTest {

    private static final Logger LOG = LoggerFactory.getLogger(PceTapiLinkTest.class);
    private static final String TOPOLOGY_FILE = "src/test/resources/topologyData/refTopoTapiFull.xml";
    private static Context tapiContext;
    private String serviceType;
    private static String version = "2.4.0";
    private static McCapability mcCapability = new NodeMcCapability(
        BigDecimal.valueOf(6.25E09), BigDecimal.valueOf(12.0E09), 1, 768);
    private static ServiceFormat serviceFormat = ServiceFormat.Ethernet;
    private Uuid anodeId;
    private Uuid znodeId;
    //SPDR-SA1-XPDR2+XPONDER
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
    //XPDR-A1-XPDR1+XPONDER
    private static Uuid xpdrA1xpdr1Id = new Uuid("4378fc29-6408-39ec-8737-5008c3dc49e5");
    //XPDR-C1-XPDR1+XPONDER
    private static Uuid xpdrC1xpdr1Id = new Uuid("1770bea4-b1da-3b20-abce-7d182c0ec0df");

    private Uuid aportId;
    private Uuid zportId;
    //ROADM-C1+PHOTONIC_MEDIA
    private static Uuid roadmCId = new Uuid("4986dca9-2d59-3d79-b306-e11802bcf1e6");
    //ROADM-A1+PHOTONIC_MEDIA
    private static Uuid roadmAId = new Uuid("3b726367-6f2d-3e3f-9033-d99b61459075");
    private static Uuid topoUuid = new Uuid("393f09a4-0a0b-3d82-a4f6-1fbbc14ca1a7");
    private Map<Uuid, PceTapiOpticalNode> tapiONroadmA;
    private Map<Uuid, PceTapiOpticalNode> tapiONroadmC;
    private PceNode tapiONspdrAx2;
    private PceNode tapiONspdrCx2;
    private PceNode tapiONspdrAx3;
    private PceNode tapiONspdrAx1;
    private PceNode tapiONspdrCx3;
    private PceNode tapiONspdrCx1;
    private PceNode tapiONxpdrCx1;
    private PceNode tapiONxpdrAx1;

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
    void roadm2roadmLinkTest() {
        //ROADM-C1+PHOTONIC_MEDIA_OTS+DEG1-TTP-TXRXtoROADM-A1+PHOTONIC_MEDIA_OTS+DEG2-TTP-TXRX Uuid
        Uuid linkUuid = new Uuid("2f9d34e5-de00-3992-b6fd-6ba5c0e46bef");
        //Generic settings required to get source and dest node
        this.anodeId = spdrSA1xpdr1Id;
        this.znodeId = spdrSC1xpdr1Id;
        //SPDR-SA1-XPDR1+DSR+XPDR1-CLIENT2
        this.aportId = new Uuid("82998ece-51cd-3c07-8b68-59d5ad5ff39e");
        //SPDR-SC1-XPDR1+DSR+XPDR1-CLIENT2"
        this.zportId = new Uuid("b877533a-99ce-3128-bfb2-dc7e0a2122dd");
        //ROADM-C1+PHOTONIC_MEDIA_OTS+DEG1-TTP-TXRX
        Uuid nepCUuid = new Uuid("15a1c5e3-b9bb-38e1-aac0-c28f554fa433");
        //ROADM-A1+PHOTONIC_MEDIA_OTS+DEG2-TTP-TXRX
        Uuid nepAUuid = new Uuid("d2902a80-c8e5-39f1-b470-9c34f7afdc99");
        this.serviceType = "100GEt";
        try {
            generalSetUp();
        } catch (ExecutionException e) {
            LOG.error("Unable to get node from mdsal: ", e);
        }
        PceTapiLink rdm2rdmLink = null;
        try {
            rdm2rdmLink = getTapiOpticalLinkFromId(linkUuid, roadmCId, roadmAId, nepCUuid, nepAUuid);
        } catch (ExecutionException e) {
            LOG.error("Unable to get link {} from mdsal: ", linkUuid, e);
        }
        assertNotNull(rdm2rdmLink);
        assertTrue(rdm2rdmLink.isValid(), "RDM to RDM Link shall be valid)");
        assertEquals(AdministrativeState.UNLOCKED, rdm2rdmLink.getAdministrativeState(),
                "RDM to RDM Link admin state shall be Unlock)");
        assertEquals(OperationalState.ENABLED, rdm2rdmLink.getOperationalState(),
                "RDM to RDM Link operational state shall be enabled)");
        assertEquals((long) rdm2rdmLink.getAvailableBandwidth(), Double.valueOf(96.0).longValue(),
                "RDM to RDM Link available bandwidth shall be 96.0, based on a 50 GHz Grid (Not used)");
        assertEquals(16.0, rdm2rdmLink.getpmd2(), "RDM to RDM LinkPMD2 shall be 16.0 ps/km)");
        assertEquals(1650.0, rdm2rdmLink.getcd(), "RDM to RDM Link Chromatic dispersion shall be 1600 ps)");
        assertEquals(12.0, rdm2rdmLink.getspanLoss(), "RDM to RDM link span loss shall be 12.0 dB)");
        assertEquals(100.0, rdm2rdmLink.getLength(), "RDM to RDM Link Length shall be 100.0 km)");
        assertEquals(501.0, rdm2rdmLink.getLatency(), "RDM to RDM Link Latency shall be 501.0 micro seconds)");
        assertNull(rdm2rdmLink.getsrlgList(), "No SRLG declared for RDM to RDM )");
        assertEquals(OpenroadmLinkType.ROADMTOROADM, rdm2rdmLink.getlinkType(),
                "RDM to RDM Link link type shall be ROADMTOROADM)");
        assertEquals("OMS link name", rdm2rdmLink.getLinkName().getValueName(),
                "RDM to RDM Link Name (Value-name) shall be OMS link name)");
        assertEquals(
                "ROADM-C1+PHOTONIC_MEDIA_OTS+DEG1-TTP-TXRXtoROADM-A1+PHOTONIC_MEDIA_OTS+DEG2-TTP-TXRX",
                rdm2rdmLink.getLinkName().getValue(),
                "RDM to RDM Link Name (value-name) shall be ROADM-C1+PHOTONIC_MEDIA_OTS+DEG1-TTP-TXRX"
                + "toROADM-A1+PHOTONIC_MEDIA_OTS+DEG2-TTP-TXRX");
        assertEquals("2f9d34e5-de00-3992-b6fd-6ba5c0e46bef", rdm2rdmLink.getLinkId(),
                "RDM to RDM Link Id shall be 2f9d34e5-de00-3992-b6fd-6ba5c0e46bef)");
        assertEquals("9830675c-71a5-33a3-8c5c-1730dee9f4f0", rdm2rdmLink.getSourceId(),
                "RDM to RDM Source NodeId shall be 9830675c-71a5-33a3-8c5c-1730dee9f4f0)");
        assertEquals("ROADM-C1+PHOTONIC_MEDIA", rdm2rdmLink.getsourceNetworkSupNodeId(),
                "RDM to RDM Source Supporting Node Id shall be ROADM-C1+PHOTONIC_MEDIA");
        assertEquals("15a1c5e3-b9bb-38e1-aac0-c28f554fa433", rdm2rdmLink.getSourceTP(),
                "RDM to RDM Destination TPId shall be 15a1c5e3-b9bb-38e1-aac0-c28f554fa433");
        assertEquals("b3e98baf-ef7d-3814-8fd2-5c89d06214a7", rdm2rdmLink.getDestId(),
                "RDM to RDM Destination NodeId shall be b3e98baf-ef7d-3814-8fd2-5c89d06214a7");
        assertEquals("ROADM-A1+PHOTONIC_MEDIA", rdm2rdmLink.getdestNetworkSupNodeId(),
                "RDM to RDM Destination Supporting Node Id shall be ROADM-A1+PHOTONIC_MEDIA");
        assertEquals(rdm2rdmLink.getUsedBandwidth(), Long.valueOf(0), "RDM to RDM Link Used bandwidth shall be 0");
        assertEquals(0.0, rdm2rdmLink.getpowerCorrection(),
                "RDM to RDM Link being by default G.652, Power correction shall be 0.0");
        assertEquals("984c8390-962d-392f-9a3f-9d5da3d37666", rdm2rdmLink.getOppositeLinkId(),
                "RDM to RDM Link opposite Link shall be 984c8390-962d-392f-9a3f-9d5da3d37666");
    }



    @Test
    void roadm2TspLinkTest() {
        //ROADM-A1+PHOTONIC_MEDIA_OTS+SRG1-PP1-TXRXtoSPDR-SA1-XPDR1+PHOTONIC_MEDIA_OTS+XPDR1-NETWORK1 Uuid
        Uuid linkUuid = new Uuid("c72c7995-8f3a-30ee-940f-309880898d57");
        //Generic settings required to get source and dest node
        this.anodeId = spdrSA1xpdr1Id;
        this.znodeId = spdrSC1xpdr1Id;
        //SPDR-SA1-XPDR1+DSR+XPDR1-CLIENT2
        this.aportId = new Uuid("82998ece-51cd-3c07-8b68-59d5ad5ff39e");
        //SPDR-SC1-XPDR1+DSR+XPDR1-CLIENT2"
        this.zportId = new Uuid("b877533a-99ce-3128-bfb2-dc7e0a2122dd");
        //SPDR-SA1-XPDR1+PHOTONIC_MEDIA_OTS+XPDR1-NETWORK1
        Uuid nepSpdrSA1Uuid = new Uuid("21efd6a4-2d81-3cdb-aabb-b983fb61904e");
        //ROADM-A1+PHOTONIC_MEDIA_OTS+SRG1-PP1-TXRX
        Uuid nepRoadmAUuid = new Uuid("affbbabc-1f42-31ee-92fc-ea5311f0c2f9");
        this.serviceType = "100GEt";
        try {
            generalSetUp();
        } catch (ExecutionException e) {
            LOG.error("Unable to get node from mdsal: ", e);
        }
        PceTapiLink rdm2tspLink = null;
        try {
            rdm2tspLink = getTapiOpticalLinkFromId(linkUuid, roadmAId, spdrSA1xpdr1Id, nepRoadmAUuid, nepSpdrSA1Uuid);
        } catch (ExecutionException e) {
            LOG.error("Unable to get link {} from mdsal: ", linkUuid, e);
        }
        // As port NETWORK1 already used on SPDR-SA1, it is not available for WDM service creation. As a result
        // The corresponding OTS NEP does not appear as a valid port in SPDR-SA1 and the Link from it to
        // the ROADM A-SRG can not be validated because.
        assertNotNull(rdm2tspLink);
        assertFalse(rdm2tspLink.isValid(), "RDM to RDM Link shall not be valid)");
    }

    @Test
    void roadm2TspLink2Test() {
        //ROADM-A1+PHOTONIC_MEDIA_OTS+SRG1-PP4-TXRXtoSPDR-SA1-XPDR2+PHOTONIC_MEDIA_OTS+XPDR2-NETWORK2 Uuid
        Uuid linkUuid = new Uuid("79b23827-48eb-33ed-b110-fbeca32c4125");
        //Generic settings required to get source and dest node
        this.anodeId = spdrSA1xpdr2Id;
        this.znodeId = spdrSC1xpdr2Id;
        // SPDR-SA1-XPDR2+OTS+XPDR2-NETWORK2 4e4bf439-b457-3260-865c-db716e2647c2
        this.aportId = new Uuid("4e4bf439-b457-3260-865c-db716e2647c2");
        // SPDR-SC1-XPDR2+OTS+XPDR2-NETWORK2 ae235ae1-f17f-3619-aea9-e6b2a9e850c3
        this.zportId = new Uuid("ae235ae1-f17f-3619-aea9-e6b2a9e850c3");
        //SPDR-SA1-XPDR1+PHOTONIC_MEDIA_OTS+XPDR1-NETWORK1
        Uuid nepSpdrSA1Uuid = new Uuid("4e4bf439-b457-3260-865c-db716e2647c2");
        //ROADM-A1+PHOTONIC_MEDIA_OTS+SRG1-PP1-TXRX
        Uuid nepRoadmAUuid = new Uuid("cd2619fb-1ae0-3785-a6fd-2b71f468cb6c");
        this.serviceType = "OTU4";
        try {
            generalSetUp();
        } catch (ExecutionException e) {
            LOG.error("Unable to get node from mdsal: ", e);
        }
        PceTapiLink rdm2tspLink = null;
        try {
            rdm2tspLink = getTapiOpticalLinkFromId(linkUuid, roadmAId, spdrSA1xpdr2Id, nepRoadmAUuid, nepSpdrSA1Uuid);
        } catch (ExecutionException e) {
            LOG.error("Unable to get link {} from mdsal: ", linkUuid, e);
        }
        assertNotNull(rdm2tspLink);
        assertTrue(rdm2tspLink.isValid(), "RDM to TSP Link shall be valid)");
        assertEquals(OperationalState.ENABLED, rdm2tspLink.getOperationalState(),
                "RDM to TSP Link operational state shall be enabled)");
        assertEquals(AdministrativeState.UNLOCKED, rdm2tspLink.getAdministrativeState(),
                "RDM to TSP Link admin state shall be Unlock)");
        assertEquals(0.0, rdm2tspLink.getpmd2(), "RDM to TSP LinkPMD shall be 0.0)");
        assertEquals(0.0, rdm2tspLink.getcd(), "RDM to TSP Link Chromatic dispersion shall be 0.0)");
        assertEquals(0.0, rdm2tspLink.getspanLoss(), "RDM to TSP Link span loss shall be 0.0)");
        assertEquals(0.0, rdm2tspLink.getLength(), "RDM to TSP Link Length shall be 0.0)");
        assertEquals(0.0, rdm2tspLink.getLatency(), "RDM to TSP Link Latency shall be 0.0)");
        assertNull(rdm2tspLink.getsrlgList(), "No SRLG declared for RDM to RDM )");
        assertEquals(OpenroadmLinkType.XPONDERINPUT, rdm2tspLink.getlinkType(),
                "RDM to TSP Link link type shall be XPONDERINPUT)");
        assertEquals("XPDR-RDM link name", rdm2tspLink.getLinkName().getValueName(),
                "RDM to TSP Link Name (Value-name) shall be XPDR-RDM link name)");
        assertEquals(
                "ROADM-A1+PHOTONIC_MEDIA_OTS+SRG1-PP4-TXRXtoSPDR-SA1-XPDR2+PHOTONIC_MEDIA_OTS+XPDR2-NETWORK2",
                rdm2tspLink.getLinkName().getValue(),
                "RDM to TSP Link Name (value-name) shall be ROADM-A1+PHOTONIC_MEDIA_OTS+SRG1-PP4-TXRXto"
                + "SPDR-SA1-XPDR2+PHOTONIC_MEDIA_OTS+XPDR2-NETWORK2");
        assertEquals("79b23827-48eb-33ed-b110-fbeca32c4125", rdm2tspLink.getLinkId(),
                "RDM to TSP Link Id shall be 79b23827-48eb-33ed-b110-fbeca32c4125)");
        assertEquals("63a5b34b-02da-390f-835b-177e7bc7e1a8", rdm2tspLink.getSourceId(),
                "RDM to TSP Source NodeId shall be 63a5b34b-02da-390f-835b-177e7bc7e1a8");
        assertEquals("ROADM-A1+PHOTONIC_MEDIA", rdm2tspLink.getsourceNetworkSupNodeId(),
                "RDM to TSP Source Supporting Node Id shall be ROADM-A1+PHOTONIC_MEDIA");
        assertEquals("cd2619fb-1ae0-3785-a6fd-2b71f468cb6c", rdm2tspLink.getSourceTP(),
                "RDM to TSP Destination TPId shall be cd2619fb-1ae0-3785-a6fd-2b71f468cb6c");
        assertEquals("38c114ae-9c0e-3068-bb27-db2dbd81220b", rdm2tspLink.getDestId(),
                "RDM to TSP Destination NodeId shall be 38c114ae-9c0e-3068-bb27-db2dbd81220b7");
        assertEquals("38c114ae-9c0e-3068-bb27-db2dbd81220b", rdm2tspLink.getdestNetworkSupNodeId(),
                "RDM to TSP Destination Supporting Node Id shall be 38c114ae-9c0e-3068-bb27-db2dbd81220b");
        assertEquals(rdm2tspLink.getUsedBandwidth(), Long.valueOf(0), "RDM to TSP Link Used bandwidth shall be 0");
        assertEquals(rdm2tspLink.getAvailableBandwidth(), Long.valueOf(1600),
                "RDM to TSP Link Available bandwidth shall be 1600.0");
        assertEquals(0.0, rdm2tspLink.getpowerCorrection(),
                "RDM to TSP Link being by default G.652, Power correction shall be 0.0");
        assertEquals("0f58cca7-87ac-368e-a526-49e47227b917", rdm2tspLink.getOppositeLinkId(),
                "RDM to TSP Link opposite Link shall be 0f58cca7-87ac-368e-a526-49e47227b917");
    }

    @Test
    void otuLinkBetweenXpdr2andXpdr2Test() {
        // TOP+SPDR-SA1-XPDR2+XPDR2-NETWORK1+SPDR-SC1-XPDR2+XPDR2-NETWORK1+iOTU Uuid
        Uuid linkOtu4Uuid = new Uuid("07df4edd-4408-310d-a820-5f34b0524900");
        Uuid iodunepSpdrSA1x2 = new Uuid("6f4777d4-41f0-3833-b811-68b90903d8");
        Uuid iodunepSpdrSC1x2 = new Uuid("8b82dac3-646c-301d-b455-b7a4802774f7");
        //Generic settings required to get source and dest node
        //SPDR-SA1-XPDR2+iODU+XPDR2-NETWORK1
        this.aportId = iodunepSpdrSA1x2;
        //SPDR-SC1-XPDR2+iODU+XPDR2-NETWORK1
        this.zportId = iodunepSpdrSC1x2;
        this.anodeId = spdrSA1xpdr2Id;
        this.znodeId = spdrSC1xpdr2Id;
        this.serviceType = "ODU4";
        try {
            generalSetUp();
        } catch (ExecutionException e) {
            LOG.error("Unable to get node from mdsal: ", e);
        }
        PceTapiLink otu4Link = null;
        try {
            otu4Link = getTapiOtnLinkFromId(linkOtu4Uuid, spdrSA1xpdr2Id, spdrSC1xpdr2Id);
        } catch (ExecutionException e) {
            LOG.error("Unable to get connection {} from mdsal: ", linkOtu4Uuid, e);
        }
        assertNotNull(otu4Link);
        assertTrue(otu4Link.isValid(), "OTU4 Link (connection) shall not be valid for ODU4 services)");
        assertEquals(OperationalState.ENABLED, otu4Link.getOperationalState(),
                "OTU4 Link (connection) Link operational state shall be enabled)");
        assertEquals(AdministrativeState.UNLOCKED, otu4Link.getAdministrativeState(),
                "OTU4 Link (connection) admin state shall be Unlock)");
        assertEquals(otu4Link.getAvailableBandwidth(), Long.valueOf(100000),
                "OTU4 Link (connection) available bandwidth shall 100000 in Mbps)");
        assertEquals(0.0, otu4Link.getpmd2(), "OTU4 Link (connection)PMD shall be 0.0)");
        assertEquals(0.0, otu4Link.getcd(), "OTU4 Link (connection) Chromatic dispersion shall be 0.0)");
        assertEquals(0.0, otu4Link.getspanLoss(), "OTU4 Link (connection) span loss shall be 0.0)");
        assertEquals(0.0, otu4Link.getLength(), "OTU4 Link (connection) Length shall be 0.0)");
        assertEquals(0.0, otu4Link.getLatency(), "OTU4 Link (connection) Latency shall be 0.0)");
        assertNull(otu4Link.getsrlgList(), "No SRLG declared for OTN Links )");
        assertEquals(OpenroadmLinkType.OTNLINK, otu4Link.getlinkType(),
                "OTU4 Link (connection) link type shall be OTN-LINK)");
        assertEquals("Connection name", otu4Link.getLinkName().getValueName(),
                "OTU4 Link (connection) Name (Value-name) shall be Connection name)");
        assertEquals(
                "TOP+SPDR-SA1-XPDR2+XPDR2-NETWORK1+SPDR-SC1-XPDR2+XPDR2-NETWORK1+iOTU",
                otu4Link.getLinkName().getValue(),
                "OTU4 Link (connection) Name (value-name) shall be TOP+SPDR-SA1-XPDR2+XPDR2-NETWORK1+SPDR-SC1-XPDR2"
                + "+XPDR2-NETWORK1+iOTU");
        assertEquals("07df4edd-4408-310d-a820-5f34b0524900", otu4Link.getLinkId(),
                "OTU4 Link (connection) Id shall be 07df4edd-4408-310d-a820-5f34b0524900)");
        assertEquals("38c114ae-9c0e-3068-bb27-db2dbd81220b", otu4Link.getDestId(),
                "TSP Source NodeId shall be 38c114ae-9c0e-3068-bb27-db2dbd81220b");
        assertEquals("38c114ae-9c0e-3068-bb27-db2dbd81220b", otu4Link.getdestNetworkSupNodeId(),
                "TSP Source Supporting Node Id shall be 38c114ae-9c0e-3068-bb27-db2dbd81220b");
        assertEquals("bb58ebb0-ca7c-3518-8ffd-808abfca54e5", otu4Link.getDestTP(),
                "TSP Source TPId shall be bb58ebb0-ca7c-3518-8ffd-808abfca54e5");
        assertEquals("d852c340-77db-3f9a-96e8-cb4de8e1004a", otu4Link.getSourceId(),
                "TSP Destination NodeId shall be d852c340-77db-3f9a-96e8-cb4de8e1004a");
        assertEquals("d852c340-77db-3f9a-96e8-cb4de8e1004a", otu4Link.getsourceNetworkSupNodeId(),
                "TSP Destination Supporting Node Id shall be d852c340-77db-3f9a-96e8-cb4de8e1004a");
        assertEquals("b9dbee10-faa9-3947-94c1-3c023646a2df", otu4Link.getSourceTP(),
                "TSP Destination TPId shall be b9dbee10-faa9-3947-94c1-3c023646a2df");
        assertEquals(0.0, otu4Link.getpowerCorrection(),
                "OTU4 Link (connection) being by default G.652, Power correction shall be 0.0");
        assertEquals("37475213-26ca-3bcd-a9f6-f2f4d8deec5f", otu4Link.getOppositeLinkId(),
                "OTU4 Link (connection) opposite Link shall be 37475213-26ca-3bcd-a9f6-f2f4d8deec5f");
    }

    @Test
    void iOdu4LinkBetweenXpdr1andXpdr1Test() {
        // TOP+SPDR-SA1-XPDR1+XPDR1-NETWORK1+SPDR-SC1-XPDR1+XPDR1-NETWORK1+iOTU Uuid
        Uuid linkOtu4Uuid = new Uuid("13b8ac31-56d7-36e9-814b-5d91f10ced16");
        // TOP+SPDR-SA1-XPDR1+XPDR1-NETWORK1+SPDR-SC1-XPDR1+XPDR1-NETWORK1+iODU Uuid
        Uuid linkiOdu4Uuid = new Uuid("b90f7b96-4fe0-390c-8ef2-41942196f19e");
        // TOP+SPDR-SA1-XPDR1+XPDR1-CLIENT1+SPDR-SC1-XPDR1+XPDR1-CLIENT1+eODU Uuid
        Uuid linkeOdu4Uuid = new Uuid("0fbf2cf1-4456-3271-af1f-32be6b3b4f40");
        // TOP+SPDR-SA1-XPDR1+XPDR1-CLIENT1+SPDR-SC1-XPDR1+XPDR1-CLIENT1+DSR Uuid

        //Generic settings required to get source and dest node
        this.anodeId = spdrSA1xpdr1Id;
        this.znodeId = spdrSC1xpdr1Id;
        //SPDR-SA1-XPDR1+DSR+XPDR1-CLIENT2
        this.aportId = new Uuid("82998ece-51cd-3c07-8b68-59d5ad5ff39e");
        //SPDR-SC1-XPDR1+DSR+XPDR1-CLIENT2"
        this.zportId = new Uuid("b877533a-99ce-3128-bfb2-dc7e0a2122dd");
        this.serviceType = "10GE";
        try {
            generalSetUp();
        } catch (ExecutionException e) {
            LOG.error("Unable to get node from mdsal: ", e);
        }
        PceTapiLink otu4Link = null;
        PceTapiLink iodu4Link = null;
        PceTapiLink eodu4Link = null;
        try {
            otu4Link = getTapiOtnLinkFromId(linkOtu4Uuid, spdrSA1xpdr1Id, spdrSC1xpdr1Id);
        } catch (ExecutionException e) {
            LOG.error("Unable to get connection {} from mdsal: ", linkOtu4Uuid, e);
        }
        try {
            iodu4Link = getTapiOtnLinkFromId(linkiOdu4Uuid, spdrSA1xpdr1Id, spdrSC1xpdr1Id);
        } catch (ExecutionException e) {
            LOG.error("Unable to get connection {} from mdsal: ", linkiOdu4Uuid, e);
        }
        try {
            eodu4Link = getTapiOtnLinkFromId(linkeOdu4Uuid, spdrSA1xpdr1Id, spdrSC1xpdr1Id);
        } catch (ExecutionException e) {
            LOG.error("Unable to get connection {} from mdsal: ", linkeOdu4Uuid, e);
        }

        assertNotNull(otu4Link);
        assertFalse(otu4Link.isValid(), "OTU4 Link (connection) shall not be valid for DSR/ODU2e services)");
        assertNotNull(eodu4Link);
        assertFalse(eodu4Link.isValid(),
                "eODU4 Link (connection) from Client C1 ports shall not be valid since used for DSR/ODU2e services)");
        assertNotNull(iodu4Link);
        assertTrue(iodu4Link.isValid(), "iODU4 Link (connection) shall be valid for DSR/ODU2e services)");
        assertEquals(OperationalState.ENABLED, iodu4Link.getOperationalState(),
                "ODU4 Link (connection) Link operational state shall be enabled)");
        assertEquals(AdministrativeState.UNLOCKED, iodu4Link.getAdministrativeState(),
                "ODU4 Link (connection) admin state shall be Unlock)");
        assertEquals(iodu4Link.getAvailableBandwidth(), Long.valueOf(100000),
                "ODU4 Link (connection) available bandwidth shall 100000 Mbps)");
        assertEquals(0.0, iodu4Link.getpmd2(), "ODU4 Link (connection)PMD shall be 0.0)");
        assertEquals(0.0, iodu4Link.getcd(), "ODU4 Link (connection) Chromatic dispersion shall be 0.0)");
        assertEquals(0.0, iodu4Link.getspanLoss(), "ODU4 Link (connection) span loss shall be 0.0)");
        assertEquals(0.0, iodu4Link.getLength(), "ODU4 Link (connection) Length shall be 0.0)");
        assertEquals(0.0, iodu4Link.getLatency(), "ODU4 Link (connection) Latency shall be 0.0)");
        assertNull(iodu4Link.getsrlgList(), "No SRLG declared for OTN Links )");
        assertEquals(OpenroadmLinkType.OTNLINK, iodu4Link.getlinkType(),
                "ODU4 Link (connection) link type shall be OTN-LINK)");
        assertEquals("Connection name", iodu4Link.getLinkName().getValueName(),
                "ODU4 Link (connection) Name (Value-name) shall be Connection name)");
        assertEquals(
                "TOP+SPDR-SA1-XPDR1+XPDR1-NETWORK1+SPDR-SC1-XPDR1+XPDR1-NETWORK1+iODU",
                iodu4Link.getLinkName().getValue(),
                "ODU4 Link (connection) Name (value-name) shall be TOP+SPDR-SA1-XPDR1+XPDR1-NETWORK1"
                + "+SPDR-SC1-XPDR1+XPDR1-NETWORK1+iODU");
        assertEquals("b90f7b96-4fe0-390c-8ef2-41942196f19e", iodu4Link.getLinkId(),
                "ODU4 Link (connection) Id shall be b90f7b96-4fe0-390c-8ef2-41942196f19e)");
        assertEquals("215ee18f-7869-3492-94d2-0f24ed0a3023", iodu4Link.getSourceId(),
                "TSP Source NodeId shall be 215ee18f-7869-3492-94d2-0f24ed0a3023");
        assertEquals("215ee18f-7869-3492-94d2-0f24ed0a3023", iodu4Link.getsourceNetworkSupNodeId(),
                "TSP Source Supporting Node Id shall be 215ee18f-7869-3492-94d2-0f24ed0a3023"
                + "(SPDR-SC1-XPDR1+XPONDER)");
        assertEquals("a2f84255-2775-34c9-ab0a-7c17a4249703", iodu4Link.getSourceTP(),
                "TSP Destination TPId shall be a2f84255-2775-34c9-ab0a-7c17a4249703");
        assertEquals("4e44bcc5-08d3-3fee-8fac-f021489e5a61", iodu4Link.getDestId(),
                "TSP Destination NodeId shall be 4e44bcc5-08d3-3fee-8fac-f021489e5a61");
        assertEquals("4e44bcc5-08d3-3fee-8fac-f021489e5a61", iodu4Link.getdestNetworkSupNodeId(),
                "TSP Destination Supporting Node Id shall be 4e44bcc5-08d3-3fee-8fac-f021489e5a61"
                + "(SPDR-SA1-XPDR1+XPONDER)");
        assertEquals("d6e08276-b5a9-3960-a3e1-14f8f72c280b", iodu4Link.getDestTP(),
                "TSP Destination TPId shall be d6e08276-b5a9-3960-a3e1-14f8f72c280b");
        assertEquals(0.0, iodu4Link.getpowerCorrection(),
                "ODU4 Link (connection) being by default G.652, Power correction shall be 0.0");
        assertEquals("606ade82-de21-3e95-b2cd-cdb2b03ef78b", iodu4Link.getOppositeLinkId(),
                "ODU4 Link (connection) opposite Link shall be 606ade82-de21-3e95-b2cd-cdb2b03ef78b");
    }

    @Test
    void iOdu4LinkBetweenXpdr3andXpdr3Test() {
        // TOP+SPDR-SA1-XPDR3+XPDR3-NETWORK1+SPDR-SC1-XPDR3+XPDR3-NETWORK1+iOTU Uuid
        Uuid linkOtu4Uuid = new Uuid("29b6a4ac-b5d3-33aa-aba1-52763259b838");
        // TOP+SPDR-SA1-XPDR3+XPDR3-NETWORK1+SPDR-SC1-XPDR3+XPDR3-NETWORK1+iODU Uuid
        Uuid linkiOdu4Uuid = new Uuid("9e237eea-ce80-3490-9a66-34f7cbdac55f");

        //Generic settings required to get source and dest node
        this.anodeId = spdrSA1xpdr3Id;
        this.znodeId = spdrSC1xpdr3Id;
        //SPDR-SA1-XPDR3+DSR+XPDR3-CLIENT2
        this.aportId = new Uuid("217abe36-25ee-337a-b919-f051faf88b21");
        //SPDR-SC1-XPDR3+DSR+XPDR3-CLIENT2
        this.zportId = new Uuid("360b276b-4beb-3c96-8650-559e9934deaa");
        this.serviceType = "1GE";
        try {
            generalSetUp();
        } catch (ExecutionException e) {
            LOG.error("Unable to get node from mdsal: ", e);
        }
        PceTapiLink otu4Link = null;
        PceTapiLink iodu4Link = null;
        try {
            otu4Link = getTapiOtnLinkFromId(linkOtu4Uuid, spdrSA1xpdr3Id, spdrSC1xpdr3Id);
        } catch (ExecutionException e) {
            LOG.error("Unable to get connection {} from mdsal: ", linkOtu4Uuid, e);
        }
        try {
            iodu4Link = getTapiOtnLinkFromId(linkiOdu4Uuid, spdrSA1xpdr3Id, spdrSC1xpdr3Id);
        } catch (ExecutionException e) {
            LOG.error("Unable to get connection {} from mdsal: ", linkiOdu4Uuid, e);
        }
        assertNotNull(otu4Link);
        assertFalse(otu4Link.isValid(), "OTU4 Link (connection) shall not be valid for DSR/ODU2e services)");
        assertNotNull(iodu4Link);
        assertTrue(iodu4Link.isValid(), "iODU4 Link (connection) shall be valid for DSR/ODU2e services)");
        assertEquals(OperationalState.ENABLED, iodu4Link.getOperationalState(),
                "ODU4 Link (connection) Link operational state shall be enabled)");
        assertEquals(AdministrativeState.UNLOCKED, iodu4Link.getAdministrativeState(),
                "ODU4 Link (connection) admin state shall be Unlock)");
        assertEquals(iodu4Link.getAvailableBandwidth(), Long.valueOf(100000),
                "ODU4 Link (connection) available bandwidth shall 100000 Mbps)");
        assertEquals(0.0, iodu4Link.getpmd2(), "ODU4 Link (connection) PMD shall be 0.0)");
        assertEquals(0.0, iodu4Link.getcd(), "ODU4 Link (connection) Chromatic dispersion shall be 0.0)");
        assertEquals(0.0, iodu4Link.getspanLoss(), "ODU4 Link (connection) span loss shall be 0.0)");
        assertEquals(0.0, iodu4Link.getLength(), "ODU4 Link (connection) Length shall be 0.0)");
        assertEquals(0.0, iodu4Link.getLatency(), "ODU4 Link (connection) Latency shall be 0.0)");
        assertNull(iodu4Link.getsrlgList(), "No SRLG declared for OTN Links )");
        assertEquals(OpenroadmLinkType.OTNLINK, iodu4Link.getlinkType(),
                "ODU4 Link (connection) link type shall be OTN-LINK)");
        assertEquals("Connection name", iodu4Link.getLinkName().getValueName(),
                "ODU4 Link (connection) Name (Value-name) shall be Connection name)");
        assertEquals(
                "TOP+SPDR-SA1-XPDR3+XPDR3-NETWORK1+SPDR-SC1-XPDR3+XPDR3-NETWORK1+iODU",
                iodu4Link.getLinkName().getValue(),
                "ODU4 Link (connection) Name (value-name) shall be TOP+SPDR-SA1-XPDR3+XPDR3-NETWORK1+SPDR-SC1-XPDR3"
                + "+XPDR3-NETWORK1+iODU");
        assertEquals("9e237eea-ce80-3490-9a66-34f7cbdac55f", iodu4Link.getLinkId(),
                "ODU4 Link (connection) Id shall be 9e237eea-ce80-3490-9a66-34f7cbdac55f)");
        assertEquals("4582e51f-2b2d-3b70-b374-86c463062710", iodu4Link.getDestId(),
                "TSP Destination NodeId shall be 4582e51f-2b2d-3b70-b374-86c463062710");
        assertEquals("4582e51f-2b2d-3b70-b374-86c463062710", iodu4Link.getdestNetworkSupNodeId(),
                "TSP Destination Supporting Node Id shall be 4582e51f-2b2d-3b70-b374-86c463062710");
        assertEquals("ced2adb2-e1fd-338c-8b64-a02106d48b45", iodu4Link.getDestTP(),
                "TSP Destination TPId shall be ced2adb2-e1fd-338c-8b64-a02106d48b45");
        assertEquals("c1f06957-c0b9-32be-8492-e278b2d4a3aa", iodu4Link.getSourceId(),
                "TSP Source NodeId shall be c1f06957-c0b9-32be-8492-e278b2d4a3aa");
        assertEquals("c1f06957-c0b9-32be-8492-e278b2d4a3aa", iodu4Link.getsourceNetworkSupNodeId(),
                "TSP Source Supporting Node Id shall be c1f06957-c0b9-32be-8492-e278b2d4a3aa");
        assertEquals("74b4b605-379f-3700-bb46-97b578cb2c7d", iodu4Link.getSourceTP(),
                "TSP Source TPId shall be 74b4b605-379f-3700-bb46-97b578cb2c7d");
        assertEquals(0.0, iodu4Link.getpowerCorrection(),
                "ODU4 Link (connection) being by default G.652, Power correction shall be 0.0");
        assertEquals("1a9dea0a-0e90-3a72-98b7-2dc04d2ea115", iodu4Link.getOppositeLinkId(),
                "ODU4 Link (connection) opposite Link shall be 1a9dea0a-0e90-3a72-98b7-2dc04d2ea115");
    }

    @Test
    void iOdu4LinkX1toX1TestNoPortSpecified() {
        LOG.info("PceTapiLInkTest Line 684 Entering Test6");
        // TOP+SPDR-SA1-XPDR1+XPDR1-NETWORK1+SPDR-SC1-XPDR1+XPDR1-NETWORK1+iODU Uuid
        Uuid linkiOdu4Uuid = new Uuid("b90f7b96-4fe0-390c-8ef2-41942196f19e");

        //Generic settings required to get source and dest node
        this.anodeId = spdrSA1xpdr1Id;
        this.znodeId = spdrSC1xpdr1Id;
        this.serviceType = "10GE";
        try {
            generalSetUp();
        } catch (ExecutionException e) {
            LOG.error("Unable to get node from mdsal: ", e);
        }
        PceTapiLink iodu4Link = null;
        try {
            iodu4Link = getTapiOtnLinkFromId(linkiOdu4Uuid, spdrSA1xpdr1Id, spdrSC1xpdr1Id);
        } catch (ExecutionException e) {
            LOG.error("Unable to get connection {} from mdsal: ", linkiOdu4Uuid, e);
        }

        assertNotNull(iodu4Link);
        assertTrue(iodu4Link.isValid(), "iODU4 Link (connection) shall be valid for DSR/ODU2e services)");
        assertEquals(OperationalState.ENABLED, iodu4Link.getOperationalState(),
                "ODU4 Link (connection) Link operational state shall be enabled)");
        assertEquals(AdministrativeState.UNLOCKED, iodu4Link.getAdministrativeState(),
                "ODU4 Link (connection) admin state shall be Unlock)");
        assertEquals(iodu4Link.getAvailableBandwidth(), Long.valueOf(100000),
                "ODU4 Link (connection) available bandwidth shall 100000 Mbps)");
        assertEquals(0.0, iodu4Link.getpmd2(), "ODU4 Link (connection)PMD shall be 0.0)");
        assertEquals(0.0, iodu4Link.getcd(), "ODU4 Link (connection) Chromatic dispersion shall be 0.0)");
        assertEquals(0.0, iodu4Link.getspanLoss(), "ODU4 Link (connection) span loss shall be 0.0)");
        assertEquals(0.0, iodu4Link.getLength(), "ODU4 Link (connection) Length shall be 0.0)");
        assertEquals(0.0, iodu4Link.getLatency(), "ODU4 Link (connection) Latency shall be 0.0)");
        assertNull(iodu4Link.getsrlgList(), "No SRLG declared for OTN Links )");
        assertEquals(OpenroadmLinkType.OTNLINK, iodu4Link.getlinkType(),
                "ODU4 Link (connection) link type shall be OTN-LINK)");
        assertEquals("Connection name", iodu4Link.getLinkName().getValueName(),
                "ODU4 Link (connection) Name (Value-name) shall be Connection name)");
        assertEquals(
                "TOP+SPDR-SA1-XPDR1+XPDR1-NETWORK1+SPDR-SC1-XPDR1+XPDR1-NETWORK1+iODU",
                iodu4Link.getLinkName().getValue(),
                "ODU4 Link (connection) Name (value-name) shall be TOP+SPDR-SA1-XPDR1+XPDR1-NETWORK1"
                + "+SPDR-SC1-XPDR1+XPDR1-NETWORK1+iODU");
        assertEquals("b90f7b96-4fe0-390c-8ef2-41942196f19e", iodu4Link.getLinkId(),
                "ODU4 Link (connection) Id shall be b90f7b96-4fe0-390c-8ef2-41942196f19e)");
        assertEquals("215ee18f-7869-3492-94d2-0f24ed0a3023", iodu4Link.getSourceId(),
                "TSP Source NodeId shall be 215ee18f-7869-3492-94d2-0f24ed0a3023");
        assertEquals("215ee18f-7869-3492-94d2-0f24ed0a3023", iodu4Link.getsourceNetworkSupNodeId(),
                "TSP Source Supporting Node Id shall be 215ee18f-7869-3492-94d2-0f24ed0a3023"
                + "(SPDR-SC1-XPDR1+XPONDER)");
        assertEquals("a2f84255-2775-34c9-ab0a-7c17a4249703", iodu4Link.getSourceTP(),
                "TSP Destination TPId shall be a2f84255-2775-34c9-ab0a-7c17a4249703");
        assertEquals("4e44bcc5-08d3-3fee-8fac-f021489e5a61", iodu4Link.getDestId(),
                "TSP Destination NodeId shall be 4e44bcc5-08d3-3fee-8fac-f021489e5a61");
        assertEquals("4e44bcc5-08d3-3fee-8fac-f021489e5a61", iodu4Link.getdestNetworkSupNodeId(),
                "TSP Destination Supporting Node Id shall be 4e44bcc5-08d3-3fee-8fac-f021489e5a61"
                + "(SPDR-SA1-XPDR1+XPONDER)");
        assertEquals("d6e08276-b5a9-3960-a3e1-14f8f72c280b", iodu4Link.getDestTP(),
                "TSP Destination TPId shall be d6e08276-b5a9-3960-a3e1-14f8f72c280b");
        assertEquals(0.0, iodu4Link.getpowerCorrection(),
                "ODU4 Link (connection) being by default G.652, Power correction shall be 0.0");
        assertEquals("606ade82-de21-3e95-b2cd-cdb2b03ef78b", iodu4Link.getOppositeLinkId(),
                "ODU4 Link (connection) opposite Link shall be 606ade82-de21-3e95-b2cd-cdb2b03ef78b");
    }

    @Test
    void otuX2toX2LinkTestNoPortSpecified() {
        LOG.info("PceTapiLInkTest Line 814 Entering Test7");
        // TOP+SPDR-SA1-XPDR2+XPDR2-NETWORK1+SPDR-SC1-XPDR2+XPDR2-NETWORK1+iOTU Uuid
        Uuid linkOtu4Uuid = new Uuid("07df4edd-4408-310d-a820-5f34b0524900");

        //Generic settings required to get source and dest node
        this.anodeId = spdrSA1xpdr2Id;
        this.znodeId = spdrSC1xpdr2Id;
        this.serviceType = "ODU4";
        try {
            generalSetUp();
        } catch (ExecutionException e) {
            LOG.error("Unable to get node from mdsal: ", e);
        }
        PceTapiLink otu4Link = null;
        try {
            otu4Link = getTapiOtnLinkFromId(linkOtu4Uuid, spdrSA1xpdr2Id, spdrSC1xpdr2Id);
        } catch (ExecutionException e) {
            LOG.error("Unable to get connection {} from mdsal: ", linkOtu4Uuid, e);
        }
        assertNotNull(otu4Link);
        assertTrue(otu4Link.isValid(), "OTU4 Link (connection) shall not be valid for ODU4 services)");
        assertEquals(OperationalState.ENABLED, otu4Link.getOperationalState(),
                "OTU4 Link (connection) Link operational state shall be enabled)");
        assertEquals(AdministrativeState.UNLOCKED, otu4Link.getAdministrativeState(),
                "OTU4 Link (connection) admin state shall be Unlock)");
        assertEquals(otu4Link.getAvailableBandwidth(), Long.valueOf(100000),
                "OTU4 Link (connection) available bandwidth shall 100000 in Mbps)");
        assertEquals(0.0, otu4Link.getpmd2(), "OTU4 Link (connection)PMD shall be 0.0)");
        assertEquals(0.0, otu4Link.getcd(), "OTU4 Link (connection) Chromatic dispersion shall be 0.0)");
        assertEquals(0.0, otu4Link.getspanLoss(), "OTU4 Link (connection) span loss shall be 0.0)");
        assertEquals(0.0, otu4Link.getLength(), "OTU4 Link (connection) Length shall be 0.0)");
        assertEquals(0.0, otu4Link.getLatency(), "OTU4 Link (connection) Latency shall be 0.0)");
        assertNull(otu4Link.getsrlgList(), "No SRLG declared for OTN Links )");
        assertEquals(OpenroadmLinkType.OTNLINK, otu4Link.getlinkType(),
                "OTU4 Link (connection) link type shall be OTN-LINK)");
        assertEquals("Connection name", otu4Link.getLinkName().getValueName(),
                "OTU4 Link (connection) Name (Value-name) shall be Connection name)");
        assertEquals(
                "TOP+SPDR-SA1-XPDR2+XPDR2-NETWORK1+SPDR-SC1-XPDR2+XPDR2-NETWORK1+iOTU",
                otu4Link.getLinkName().getValue(),
                "OTU4 Link (connection) Name (value-name) shall be TOP+SPDR-SA1-XPDR2+XPDR2-NETWORK1+SPDR-SC1-XPDR2"
                + "+XPDR2-NETWORK1+iOTU");
        assertEquals("07df4edd-4408-310d-a820-5f34b0524900", otu4Link.getLinkId(),
                "OTU4 Link (connection) Id shall be 07df4edd-4408-310d-a820-5f34b0524900)");
        assertEquals("38c114ae-9c0e-3068-bb27-db2dbd81220b", otu4Link.getDestId(),
                "TSP Source NodeId shall be 38c114ae-9c0e-3068-bb27-db2dbd81220b");
        assertEquals("38c114ae-9c0e-3068-bb27-db2dbd81220b", otu4Link.getdestNetworkSupNodeId(),
                "TSP Source Supporting Node Id shall be 38c114ae-9c0e-3068-bb27-db2dbd81220b");
        assertEquals("bb58ebb0-ca7c-3518-8ffd-808abfca54e5", otu4Link.getDestTP(),
                "TSP Source TPId shall be bb58ebb0-ca7c-3518-8ffd-808abfca54e5");
        assertEquals("d852c340-77db-3f9a-96e8-cb4de8e1004a", otu4Link.getSourceId(),
                "TSP Destination NodeId shall be d852c340-77db-3f9a-96e8-cb4de8e1004a");
        assertEquals("d852c340-77db-3f9a-96e8-cb4de8e1004a", otu4Link.getsourceNetworkSupNodeId(),
                "TSP Destination Supporting Node Id shall be d852c340-77db-3f9a-96e8-cb4de8e1004a");
        assertEquals("b9dbee10-faa9-3947-94c1-3c023646a2df", otu4Link.getSourceTP(),
                "TSP Destination TPId shall be b9dbee10-faa9-3947-94c1-3c023646a2df");
        assertEquals(0.0, otu4Link.getpowerCorrection(),
                "OTU4 Link (connection) being by default G.652, Power correction shall be 0.0");
        assertEquals("37475213-26ca-3bcd-a9f6-f2f4d8deec5f", otu4Link.getOppositeLinkId(),
                "OTU4 Link (connection) opposite Link shall be 37475213-26ca-3bcd-a9f6-f2f4d8deec5f");
    }

    private PceTapiLink getTapiOpticalLinkFromId(Uuid linkId, Uuid nodeXuuid, Uuid nodeYuuid,
            Uuid nepXUuid, Uuid nepYUuid)
            throws ExecutionException {
        Link link = null;

        DataObjectIdentifier<Link> linkIID = DataObjectIdentifier.builder(Context.class)
            .augmentation(org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.Context1.class)
            .child(TopologyContext.class)
            .child(Topology.class, new TopologyKey(topoUuid))
            .child(Link.class, new LinkKey(linkId))
            .build();
        NetworkTransactionService netTransServ = new NetworkTransactionImpl(getDataBroker());
        ListenableFuture<Optional<Link>> linkFuture =
            netTransServ.read(LogicalDatastoreType.OPERATIONAL, linkIID);
        try {
            link = linkFuture.get().orElseThrow();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ExecutionException("Unable to get node from mdsal : " + linkId, e);
        } catch (ExecutionException e) {
            throw new ExecutionException("Unable to get node from mdsal: " + linkId, e);
        } catch (NoSuchElementException e) {
            return null;
        }
        LOG.info("PceTapiLInkTest: getTapiOpticalLinkFromId, link is {}", link.getName());

        TapiOpticalNode ton = getTapiOpticalNodeFromId(nodeXuuid);
        Map<Uuid, PceTapiOpticalNode> nodeXpceNodeMap = getPceTapiOpticalNodeFromId(nodeXuuid, ton);
        Uuid disagNodeXUuid = null;
        if (nodeXpceNodeMap.size() == 1) {
            // This is the case of a Xponder
            disagNodeXUuid = nodeXpceNodeMap.entrySet().iterator().next().getKey();
        } else {
            for (Map.Entry<Uuid, PceTapiOpticalNode> entry : nodeXpceNodeMap.entrySet()) {
                if (entry.getValue().getListOfNep().stream()
                        .map(BasePceNep:: getNepCepUuid)
                        .toList()
                        .contains(nepXUuid)) {
                    LOG.info("PceTapiLInkTest Line 293, PTON is  {}", entry.getValue().getNodeId());
                    disagNodeXUuid = entry.getKey();
                }
            }
        }

        ton = getTapiOpticalNodeFromId(nodeYuuid);
        Map<Uuid, PceTapiOpticalNode> nodeYpceNodeMap = getPceTapiOpticalNodeFromId(nodeYuuid, ton);
        Uuid disagNodeYUuid = null;
        if (nodeYpceNodeMap.size() == 1) {
            // This is the case of a Xponder
            disagNodeYUuid = nodeYpceNodeMap.entrySet().iterator().next().getKey();
        } else {
            for (Map.Entry<Uuid, PceTapiOpticalNode> entry : nodeYpceNodeMap.entrySet()) {
                if (entry.getValue().getListOfNep().stream()
                        .map(BasePceNep:: getNepCepUuid)
                        .toList()
                        .contains(nepYUuid)) {
                    LOG.info("PceTapiLInkTest Line 301, PTON is  {}", entry.getValue().getNodeId());
                    disagNodeYUuid = entry.getKey();
                }
            }
        }

        LOG.info("TapiLinkTest getTapiOpticalLinkFromId Line 242, DisagNodeXUuid = {}, DisagNodeYUuid = {}",
            disagNodeXUuid, disagNodeYUuid);
        if (disagNodeXUuid == null || disagNodeYUuid == null) {
            LOG.info("TapiLinkTest getTapiOpticalLinkFromId Line 245, unable to find disagregated PceNode for Node"
                + "X {} or Node Y {}", nodeXuuid, nodeYuuid);
            return null;
        }

        return new PceTapiLink(new TopologyKey(topoUuid), link, nodeXpceNodeMap.get(disagNodeXUuid),
            nodeYpceNodeMap.get(disagNodeYUuid));
    }

    private PceTapiLink getTapiOtnLinkFromId(Uuid linkId, Uuid nodeXuuid, Uuid nodeYuuid)
            throws ExecutionException {
        Connection conn = null;

        DataObjectIdentifier<Connection> conIID = DataObjectIdentifier.builder(Context.class)
            .augmentation(org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev221121.Context1.class)
            .child(ConnectivityContext.class)
            .child(Connection.class, new ConnectionKey(linkId))
            .build();
        NetworkTransactionService netTransServ = new NetworkTransactionImpl(getDataBroker());
        ListenableFuture<Optional<Connection>> conFuture =
            netTransServ.read(LogicalDatastoreType.OPERATIONAL, conIID);
        try {
            conn = conFuture.get().orElseThrow();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ExecutionException("Unable to get node from mdsal : " + linkId, e);
        } catch (ExecutionException e) {
            throw new ExecutionException("Unable to get node from mdsal: " + linkId, e);
        } catch (NoSuchElementException e) {
            return null;
        }
        LOG.info("PceTapiLInkTest: getTapiOtnLinkFromId, connection is {}", conn.getName());

        TapiOpticalNode ton = getTapiOpticalNodeFromId(nodeXuuid);
        LOG.info("TapiLinkTest Line 886 TON  for NodeX is {} ", ton == null ? null : nodeXuuid);
        PceTapiOtnNode nodeX = getPceTapiOtnNodeXpdrFromId(nodeXuuid, ton);
        LOG.info("TapiLinkTest Line 888 PceOtnNode  for NodeX is {} ", nodeX == null ? null : nodeX.getNodeId());
        ton = getTapiOpticalNodeFromId(nodeYuuid);
        LOG.info("TapiLinkTest Line 890 TON  for NodeY is {} ", ton == null ? null : nodeXuuid);
        PceTapiOtnNode nodeY = getPceTapiOtnNodeXpdrFromId(nodeYuuid, ton);
        LOG.info("TapiLinkTest Line 892 PceOtnNode  for NodeY is {} ", nodeY == null ? null : nodeY.getNodeId());

        if (nodeX == null || nodeY == null) {
            LOG.info("TapiLinkTest getTapiOtnLinkFromId Line 587, unable to find PceOtnNode for Node X {} or Node Y {}",
                nodeX == null ? null : nodeX.getNodeId(),
                nodeY == null ? null : nodeY.getNodeId());
            return null;
        }
        return new PceTapiLink(new TopologyKey(topoUuid), conn, nodeX, nodeY, serviceType);
    }

    private TapiOpticalNode getTapiOpticalNodeFromId(Uuid nodeId)
            throws ExecutionException {
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
        TapiOpticalNode ton = new TapiOpticalNode(serviceType, node, version, anodeId, znodeId, aportId, zportId,
                mcCapability);
        return ton;
    }

    private Map<Uuid, PceTapiOpticalNode> getPceTapiOpticalNodeFromId(Uuid nodeId,TapiOpticalNode ton) {

        ton.initialize();
        Map<Uuid, PceTapiOpticalNode> pceNodeMap = new HashMap<>();
        if (ton.getXpdrOpticalNode() != null) {
            LOG.info("PceTapiLInkTest Line 351, XPDR PTON is  {}", ton.getXpdrOpticalNode().getNodeId());
            pceNodeMap.put(nodeId, ton.getXpdrOpticalNode());
        } else {
            LOG.info("PceTapiLInkTest Line 354, ROADM PTON is includes {}", ton.getPceNodeMap().entrySet().stream()
                .map(Map.Entry::getKey).collect(Collectors.toList()));
            pceNodeMap.putAll(ton.getPceNodeMap());
        }
        return pceNodeMap;
    }

    private PceTapiOpticalNode getPceTapiOptNodeXpdrFromId(Uuid nodeId, TapiOpticalNode ton) {
        ton.initialize();
        return ton.getXpdrOpticalNode();
    }

    private PceTapiOtnNode getPceTapiOtnNodeXpdrFromId(Uuid nodeId, TapiOpticalNode ton) {
        ton.initialize();
        return ton.getXpdrOtnNode();
    }

    private void generalSetUp() throws ExecutionException {
        try {
            tapiONroadmA = getPceTapiOpticalNodeFromId(roadmAId, getTapiOpticalNodeFromId(roadmAId));
            LOG.info("TapiLinkTest line 273: roadmA Map of Pce Node includes {}",
                tapiONroadmA.entrySet().stream().map(Entry::getKey).collect(Collectors.toList()));
            tapiONroadmC = getPceTapiOpticalNodeFromId(roadmCId, getTapiOpticalNodeFromId(roadmCId));
            LOG.info("TapiLinkTest line 279: roadmC Map of Pce Node includes {}",
                tapiONroadmA.entrySet().stream().map(Entry::getKey).collect(Collectors.toList()));
            xpdrSetUp();
        } catch (ExecutionException e) {
            LOG.error("Unable to get node from mdsal: ", e);
        }
    }

    private void xpdrSetUp() throws ExecutionException {
        try {
            tapiONspdrAx1 = getPceTapiOptNodeXpdrFromId(spdrSA1xpdr1Id, getTapiOpticalNodeFromId(spdrSA1xpdr1Id));
            tapiONspdrAx2 = getPceTapiOptNodeXpdrFromId(spdrSA1xpdr2Id, getTapiOpticalNodeFromId(spdrSA1xpdr2Id));
            tapiONspdrAx2 = getPceTapiOptNodeXpdrFromId(spdrSA1xpdr3Id, getTapiOpticalNodeFromId(spdrSA1xpdr3Id));
            tapiONspdrCx1 = getPceTapiOptNodeXpdrFromId(spdrSC1xpdr1Id, getTapiOpticalNodeFromId(spdrSC1xpdr1Id));
            tapiONspdrCx2 = getPceTapiOptNodeXpdrFromId(spdrSC1xpdr2Id, getTapiOpticalNodeFromId(spdrSC1xpdr2Id));
            tapiONspdrCx2 = getPceTapiOptNodeXpdrFromId(spdrSC1xpdr3Id, getTapiOpticalNodeFromId(spdrSC1xpdr3Id));
            tapiONxpdrAx1 = getPceTapiOptNodeXpdrFromId(xpdrA1xpdr1Id, getTapiOpticalNodeFromId(xpdrA1xpdr1Id));
            tapiONxpdrCx1 = getPceTapiOptNodeXpdrFromId(xpdrC1xpdr1Id, getTapiOpticalNodeFromId(xpdrC1xpdr1Id));
        } catch (ExecutionException e) {
            LOG.error("Unable to get node from mdsal: ", e);
        }
    }

}
