/*
 * Copyright © 2020 Orange, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.pce.networkanalyzer;

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
    private static BigDecimal slotWidthGranularity = BigDecimal.valueOf(6.25E09);
    private static BigDecimal centralFreqGranularity = BigDecimal.valueOf(12.0E09);
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
        LOG.info("PceTapiLInkTest Line 151 Entering Test1");
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
        assertTrue(rdm2rdmLink.isValid() == true, "RDM to RDM Link shall be valid)");
        assertTrue(rdm2rdmLink.getAdministrativeState().equals(AdministrativeState.UNLOCKED),
            "RDM to RDM Link admin state shall be Unlock)");
        assertTrue(rdm2rdmLink.getOperationalState().equals(OperationalState.ENABLED),
            "RDM to RDM Link operational state shall be enabled)");
        assertTrue(rdm2rdmLink.getAvailableBandwidth().equals(Double.valueOf(96.0).longValue()),
            "RDM to RDM Link available bandwidth shall be 96.0, based on a 50 GHz Grid (Not used)");
        assertTrue(rdm2rdmLink.getpmd2().equals(16.0),"RDM to RDM LinkPMD2 shall be 16.0 ps/km)");
        assertTrue(rdm2rdmLink.getcd().equals(1650.0),"RDM to RDM Link Chromatic dispersion shall be 1600 ps)");
        assertTrue(rdm2rdmLink.getspanLoss().equals(12.0),"RDM to RDM link span loss shall be 12.0 dB)");
        assertTrue(rdm2rdmLink.getLength().equals(100.0),"RDM to RDM Link Length shall be 100.0 km)");
        assertTrue(rdm2rdmLink.getLatency().equals(501.0),"RDM to RDM Link Latency shall be 501.0 micro seconds)");
        assertTrue(rdm2rdmLink.getsrlgList() == null,"No SRLG declared for RDM to RDM )");
        assertTrue(rdm2rdmLink.getlinkType().equals(OpenroadmLinkType.ROADMTOROADM),
            "RDM to RDM Link link type shall be ROADMTOROADM)");
        assertTrue(rdm2rdmLink.getLinkName().getValueName().equals("OMS link name"),"RDM to RDM Link Name (Value-name)"
            + "shall be OMS link name)");
        assertTrue(rdm2rdmLink.getLinkName().getValue()
            .equals("ROADM-C1+PHOTONIC_MEDIA_OTS+DEG1-TTP-TXRXtoROADM-A1+PHOTONIC_MEDIA_OTS+DEG2-TTP-TXRX"),
            "RDM to RDM Link Name (value-name) shall be ROADM-C1+PHOTONIC_MEDIA_OTS+DEG1-TTP-TXRX"
            + "toROADM-A1+PHOTONIC_MEDIA_OTS+DEG2-TTP-TXRX");
        assertTrue(rdm2rdmLink.getLinkUuid().getValue().equals("2f9d34e5-de00-3992-b6fd-6ba5c0e46bef"),
            "RDM to RDM Link Id shall be 2f9d34e5-de00-3992-b6fd-6ba5c0e46bef)");
        assertTrue(rdm2rdmLink.getSourceUuid().getValue().equals("9830675c-71a5-33a3-8c5c-1730dee9f4f0"),
            "RDM to RDM Source NodeId shall be 9830675c-71a5-33a3-8c5c-1730dee9f4f0)");
        assertTrue(rdm2rdmLink.getsourceNetworkSupNodeId().equals("ROADM-C1+PHOTONIC_MEDIA"),
            "RDM to RDM Source Supporting Node Id shall be ROADM-C1+PHOTONIC_MEDIA");
        assertTrue(rdm2rdmLink.getSourceTPUuid().getValue().equals("15a1c5e3-b9bb-38e1-aac0-c28f554fa433"),
            "RDM to RDM Destination TPId shall be 15a1c5e3-b9bb-38e1-aac0-c28f554fa433");
        assertTrue(rdm2rdmLink.getDestUuid().getValue().equals("b3e98baf-ef7d-3814-8fd2-5c89d06214a7"),
            "RDM to RDM Destination NodeId shall be b3e98baf-ef7d-3814-8fd2-5c89d06214a7");
        assertTrue(rdm2rdmLink.getdestNetworkSupNodeId().equals("ROADM-A1+PHOTONIC_MEDIA"),
            "RDM to RDM Destination Supporting Node Id shall be ROADM-A1+PHOTONIC_MEDIA");
        assertTrue(rdm2rdmLink.getUsedBandwidth().equals(Long.valueOf(0)), "RDM to RDM Link Used bandwidth shall be 0");
        assertTrue(rdm2rdmLink.getpowerCorrection().equals(0.0),
            "RDM to RDM Link being by default G.652, Power correction shall be 0.0");
        assertTrue(rdm2rdmLink.getOppositeLinkUuid().getValue().equals("2f9d34e5-de00-3992-b6fd-6ba5c0e46bef"),
            "RDM to RDM Link opposite Link shall be itself");
    }



    @Test
    void roadm2TspLinkTest() {
        LOG.info("PceTapiLInkTest Line 207 Entering Test2");
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
        assertTrue(rdm2tspLink.isValid() == false, "RDM to RDM Link shall not be valid)");

    }

    @Test
    void roadm2TspLink2Test() {
        LOG.info("PceTapiLInkTest Line 264 Entering Test3");
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
        assertTrue(rdm2tspLink.isValid() == true,
            "RDM to TSP Link shall be valid)");
        assertTrue(rdm2tspLink.getOperationalState().equals(OperationalState.ENABLED),
            "RDM to TSP Link operational state shall be enabled)");
        assertTrue(rdm2tspLink.getAdministrativeState().equals(AdministrativeState.UNLOCKED),
            "RDM to TSP Link admin state shall be Unlock)");
        assertTrue(rdm2tspLink.getAvailableBandwidth().equals(Long.valueOf(100)),
            "RDM to TSP Link available bandwidth shall be 100)");
        assertTrue(rdm2tspLink.getpmd2().equals(0.0),"RDM to TSP LinkPMD shall be 0.0)");
        assertTrue(rdm2tspLink.getcd().equals(0.0),"RDM to TSP Link Chromatic dispersion shall be 0.0)");
        assertTrue(rdm2tspLink.getspanLoss().equals(0.0),"RDM to TSP Link span loss shall be 0.0)");
        assertTrue(rdm2tspLink.getLength().equals(0.0),"RDM to TSP Link Length shall be 0.0)");
        assertTrue(rdm2tspLink.getLatency().equals(0.0),"RDM to TSP Link Latency shall be 0.0)");
        assertTrue(rdm2tspLink.getsrlgList() == null,"No SRLG declared for RDM to RDM )");
        assertTrue(rdm2tspLink.getlinkType().equals(OpenroadmLinkType.XPONDERINPUT),
            "RDM to TSP Link link type shall be XPONDERINPUT)");
        assertTrue(rdm2tspLink.getLinkName().getValueName().equals("XPDR-RDM link name"),"RDM to TSP Link Name"
            + "(Value-name) shall be XPDR-RDM link name)");
        assertTrue(rdm2tspLink.getLinkName().getValue()
            .equals("ROADM-A1+PHOTONIC_MEDIA_OTS+SRG1-PP4-TXRXtoSPDR-SA1-XPDR2+PHOTONIC_MEDIA_OTS+XPDR2-NETWORK2"),
            "RDM to TSP Link Name (value-name) shall be ROADM-A1+PHOTONIC_MEDIA_OTS+SRG1-PP4-TXRXto"
            + "SPDR-SA1-XPDR2+PHOTONIC_MEDIA_OTS+XPDR2-NETWORK2");
        assertTrue(rdm2tspLink.getLinkUuid().getValue().equals("79b23827-48eb-33ed-b110-fbeca32c4125"),
            "RDM to TSP Link Id shall be 79b23827-48eb-33ed-b110-fbeca32c4125)");
        assertTrue(rdm2tspLink.getSourceUuid().getValue().equals("63a5b34b-02da-390f-835b-177e7bc7e1a8"),
            "RDM to TSP Source NodeId shall be 63a5b34b-02da-390f-835b-177e7bc7e1a8");
        assertTrue(rdm2tspLink.getsourceNetworkSupNodeId().equals("ROADM-A1+PHOTONIC_MEDIA"),
            "RDM to TSP Source Supporting Node Id shall be ROADM-A1+PHOTONIC_MEDIA");
        assertTrue(rdm2tspLink.getSourceTPUuid().getValue().equals("cd2619fb-1ae0-3785-a6fd-2b71f468cb6c"),
            "RDM to TSP Destination TPId shall be cd2619fb-1ae0-3785-a6fd-2b71f468cb6c");
        assertTrue(rdm2tspLink.getDestUuid().getValue().equals("38c114ae-9c0e-3068-bb27-db2dbd81220b"),
            "RDM to TSP Destination NodeId shall be 38c114ae-9c0e-3068-bb27-db2dbd81220b7");
        assertTrue(rdm2tspLink.getdestNetworkSupNodeId().equals("38c114ae-9c0e-3068-bb27-db2dbd81220b"),
            "RDM to TSP Destination Supporting Node Id shall be 38c114ae-9c0e-3068-bb27-db2dbd81220b");
        assertTrue(rdm2tspLink.getUsedBandwidth().equals(Long.valueOf(0)), "RDM to TSP Link Used bandwidth shall be 0");
        assertTrue(rdm2tspLink.getAvailableBandwidth().equals(Long.valueOf(100)),
            "RDM to TSP Link Available bandwidth shall be 0.0");
        assertTrue(rdm2tspLink.getpowerCorrection().equals(0.0),
            "RDM to TSP Link being by default G.652, Power correction shall be 0.0");
        assertTrue(rdm2tspLink.getOppositeLinkUuid().getValue().equals("79b23827-48eb-33ed-b110-fbeca32c4125"),
            "RDM to TSP Link opposite Link shall be itself");
    }

    @Test
    void otuX2toX2LinkTest() {
        LOG.info("PceTapiLInkTest Line 352 Entering Test4");
        // TOP+SPDR-SA1-XPDR2+XPDR2-NETWORK1+SPDR-SC1-XPDR2+XPDR2-NETWORK1+iOTU Uuid
        Uuid linkOtu4Uuid = new Uuid("07df4edd-4408-310d-a820-5f34b0524900");
        Uuid iotucepSpdrSA1x2 = new Uuid("bb58ebb0-ca7c-3518-8ffd-808abfca54e5");
        Uuid iotucepSpdrSC1x2 = new Uuid("b9dbee10-faa9-3947-94c1-3c023646a2df");
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
            otu4Link = getTapiOtnLinkFromId(linkOtu4Uuid, spdrSA1xpdr2Id, spdrSC1xpdr2Id, iotucepSpdrSA1x2,
                iotucepSpdrSC1x2);
        } catch (ExecutionException e) {
            LOG.error("Unable to get connection {} from mdsal: ", linkOtu4Uuid, e);
        }
        assertTrue(otu4Link.isValid() == true,
            "OTU4 Link (connection) shall not be valid for ODU4 services)");
        assertTrue(otu4Link.getOperationalState().equals(OperationalState.ENABLED),
            "OTU4 Link (connection) Link operational state shall be enabled)");
        assertTrue(otu4Link.getAdministrativeState().equals(AdministrativeState.UNLOCKED),
            "OTU4 Link (connection) admin state shall be Unlock)");
        assertTrue(otu4Link.getAvailableBandwidth().equals(Long.valueOf(100000)),
            "OTU4 Link (connection) available bandwidth shall 100000 in Mbps)");
        assertTrue(otu4Link.getpmd2().equals(0.0),"OTU4 Link (connection)PMD shall be 0.0)");
        assertTrue(otu4Link.getcd().equals(0.0),"OTU4 Link (connection) Chromatic dispersion shall be 0.0)");
        assertTrue(otu4Link.getspanLoss().equals(0.0),"OTU4 Link (connection) span loss shall be 0.0)");
        assertTrue(otu4Link.getLength().equals(0.0),"OTU4 Link (connection) Length shall be 0.0)");
        assertTrue(otu4Link.getLatency().equals(0.0),"OTU4 Link (connection) Latency shall be 0.0)");
        assertTrue(otu4Link.getsrlgList() == null,"No SRLG declared for OTN Links )");
        assertTrue(otu4Link.getlinkType().equals(OpenroadmLinkType.OTNLINK),
            "OTU4 Link (connection) link type shall be OTN-LINK)");
        assertTrue(otu4Link.getLinkName().getValueName().equals("Connection name"),"OTU4 Link (connection) Name"
            + "(Value-name) shall be Connection name)");
        assertTrue(otu4Link.getLinkName().getValue()
            .equals("TOP+SPDR-SA1-XPDR2+XPDR2-NETWORK1+SPDR-SC1-XPDR2+XPDR2-NETWORK1+iOTU"),
            "OTU4 Link (connection) Name (value-name) shall be TOP+SPDR-SA1-XPDR2+XPDR2-NETWORK1+SPDR-SC1-XPDR2"
            + "+XPDR2-NETWORK1+iOTU");
        assertTrue(otu4Link.getLinkUuid().getValue().equals("07df4edd-4408-310d-a820-5f34b0524900"),
            "OTU4 Link (connection) Id shall be 07df4edd-4408-310d-a820-5f34b0524900)");
        assertTrue(otu4Link.getDestUuid().getValue().equals("38c114ae-9c0e-3068-bb27-db2dbd81220b"),
            "TSP Source NodeId shall be 38c114ae-9c0e-3068-bb27-db2dbd81220b");
        assertTrue(otu4Link.getdestNetworkSupNodeId().equals("38c114ae-9c0e-3068-bb27-db2dbd81220b"),
            "TSP Source Supporting Node Id shall be 38c114ae-9c0e-3068-bb27-db2dbd81220b");
        assertTrue(otu4Link.getDestTPUuid().getValue().equals("bb58ebb0-ca7c-3518-8ffd-808abfca54e5"),
            "TSP Source TPId shall be bb58ebb0-ca7c-3518-8ffd-808abfca54e5");
        assertTrue(otu4Link.getSourceUuid().getValue().equals("d852c340-77db-3f9a-96e8-cb4de8e1004a"),
            "TSP Destination NodeId shall be d852c340-77db-3f9a-96e8-cb4de8e1004a");
        assertTrue(otu4Link.getsourceNetworkSupNodeId().equals("d852c340-77db-3f9a-96e8-cb4de8e1004a"),
            "TSP Destination Supporting Node Id shall be d852c340-77db-3f9a-96e8-cb4de8e1004a");
        assertTrue(otu4Link.getSourceTPUuid().getValue().equals("b9dbee10-faa9-3947-94c1-3c023646a2df"),
            "TSP Destination TPId shall be b9dbee10-faa9-3947-94c1-3c023646a2df");
        assertTrue(otu4Link.getpowerCorrection().equals(0.0),
            "OTU4 Link (connection) being by default G.652, Power correction shall be 0.0");
        assertTrue(otu4Link.getOppositeLinkUuid().getValue().equals("07df4edd-4408-310d-a820-5f34b0524900"),
            "OTU4 Link (connection) opposite Link shall be itself");
    }

    @Test
    void iOdu4LinkX1toX1Test() {
        LOG.info("PceTapiLInkTest Line 435 Entering Test5");
        // TOP+SPDR-SA1-XPDR1+XPDR1-NETWORK1+SPDR-SC1-XPDR1+XPDR1-NETWORK1+iOTU Uuid
        Uuid linkOtu4Uuid = new Uuid("13b8ac31-56d7-36e9-814b-5d91f10ced16");
        // TOP+SPDR-SA1-XPDR1+XPDR1-NETWORK1+SPDR-SC1-XPDR1+XPDR1-NETWORK1+iODU Uuid
        Uuid linkiOdu4Uuid = new Uuid("b90f7b96-4fe0-390c-8ef2-41942196f19e");
        // TOP+SPDR-SA1-XPDR1+XPDR1-CLIENT1+SPDR-SC1-XPDR1+XPDR1-CLIENT1+eODU Uuid
        Uuid linkeOdu4Uuid = new Uuid("0fbf2cf1-4456-3271-af1f-32be6b3b4f40");
        // TOP+SPDR-SA1-XPDR1+XPDR1-CLIENT1+SPDR-SC1-XPDR1+XPDR1-CLIENT1+DSR Uuid
        Uuid linkDsrUuid = new Uuid("902ef32e-2573-3a8d-b3e9-635926c38c38");
        Uuid ioducepSpdrSA1x1 = new Uuid("d6e08276-b5a9-3960-a3e1-14f8f72c280b");
        Uuid ioducepSpdrSC1x1 = new Uuid("a2f84255-2775-34c9-ab0a-7c17a4249703");
        Uuid eoducepSpdrSA1x1 = new Uuid("de16e16e-eb69-3819-8f59-4378b12a36ec");
        Uuid eoducepSpdrSC1x1 = new Uuid("2a7cd883-4017-3f69-b39a-12b81d57d955");
        Uuid iotucepSpdrSA1x1 = new Uuid("a24840c1-a968-3e94-9689-0bc4e538528a");
        Uuid iotucepSpdrSC1x1 = new Uuid("37b8a89d-6bcd-359f-9432-9dd06e4ff462");
        Uuid dsrcepSpdrSA1x1 = new Uuid("12bc1201-bb84-3280-b4bf-df58b3cf057c");
        Uuid dsrcepSpdrSC1x1 = new Uuid("a7ef6781-c149-394b-b21c-48b324f68c98");
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
        PceTapiLink dsrLink = null;
        try {
            otu4Link = getTapiOtnLinkFromId(linkOtu4Uuid, spdrSA1xpdr1Id, spdrSC1xpdr1Id, iotucepSpdrSA1x1,
                iotucepSpdrSC1x1);
        } catch (ExecutionException e) {
            LOG.error("Unable to get connection {} from mdsal: ", linkOtu4Uuid, e);
        }
        try {
            iodu4Link = getTapiOtnLinkFromId(linkiOdu4Uuid, spdrSA1xpdr1Id, spdrSC1xpdr1Id, ioducepSpdrSA1x1,
                ioducepSpdrSC1x1);
        } catch (ExecutionException e) {
            LOG.error("Unable to get connection {} from mdsal: ", linkiOdu4Uuid, e);
        }
        try {
            eodu4Link = getTapiOtnLinkFromId(linkeOdu4Uuid, spdrSA1xpdr1Id, spdrSC1xpdr1Id, eoducepSpdrSA1x1,
                eoducepSpdrSC1x1);
        } catch (ExecutionException e) {
            LOG.error("Unable to get connection {} from mdsal: ", linkeOdu4Uuid, e);
        }
        try {
            dsrLink = getTapiOtnLinkFromId(linkDsrUuid, spdrSA1xpdr1Id, spdrSC1xpdr1Id, dsrcepSpdrSA1x1,
                dsrcepSpdrSC1x1);
        } catch (ExecutionException e) {
            LOG.error("Unable to get connection {} from mdsal: ", linkDsrUuid, e);
        }
        assertTrue(otu4Link.isValid() == false,
            "OTU4 Link (connection) shall not be valid for DSR/ODU2e services)");
        assertTrue(eodu4Link.isValid() == false,
            "eODU4 Link (connection) from Client C1 ports shall not be valid since used for DSR/ODU2e services)");
        assertTrue(iodu4Link.isValid() == true,
            "iODU4 Link (connection) shall be valid for DSR/ODU2e services)");
        assertTrue(iodu4Link.getOperationalState().equals(OperationalState.ENABLED),
            "ODU4 Link (connection) Link operational state shall be enabled)");
        assertTrue(iodu4Link.getAdministrativeState().equals(AdministrativeState.UNLOCKED),
            "ODU4 Link (connection) admin state shall be Unlock)");
        assertTrue(iodu4Link.getAvailableBandwidth().equals(Long.valueOf(100000)),
            "ODU4 Link (connection) available bandwidth shall 100000 Mbps)");
        assertTrue(iodu4Link.getpmd2().equals(0.0),"ODU4 Link (connection)PMD shall be 0.0)");
        assertTrue(iodu4Link.getcd().equals(0.0),"ODU4 Link (connection) Chromatic dispersion shall be 0.0)");
        assertTrue(iodu4Link.getspanLoss().equals(0.0),"ODU4 Link (connection) span loss shall be 0.0)");
        assertTrue(iodu4Link.getLength().equals(0.0),"ODU4 Link (connection) Length shall be 0.0)");
        assertTrue(iodu4Link.getLatency().equals(0.0),"ODU4 Link (connection) Latency shall be 0.0)");
        assertTrue(iodu4Link.getsrlgList() == null,"No SRLG declared for OTN Links )");
        assertTrue(iodu4Link.getlinkType().equals(OpenroadmLinkType.OTNLINK),
            "ODU4 Link (connection) link type shall be OTN-LINK)");
        assertTrue(iodu4Link.getLinkName().getValueName().equals("Connection name"),"ODU4 Link (connection) Name"
            + "(Value-name) shall be Connection name)");
        assertTrue(iodu4Link.getLinkName().getValue()
            .equals("TOP+SPDR-SA1-XPDR1+XPDR1-NETWORK1+SPDR-SC1-XPDR1+XPDR1-NETWORK1+iODU"),
            "ODU4 Link (connection) Name (value-name) shall be TOP+SPDR-SA1-XPDR1+XPDR1-NETWORK1"
            + "+SPDR-SC1-XPDR1+XPDR1-NETWORK1+iODU");
        assertTrue(iodu4Link.getLinkUuid().getValue().equals("b90f7b96-4fe0-390c-8ef2-41942196f19e"),
            "ODU4 Link (connection) Id shall be b90f7b96-4fe0-390c-8ef2-41942196f19e)");
        assertTrue(iodu4Link.getSourceUuid().getValue().equals("215ee18f-7869-3492-94d2-0f24ed0a3023"),
            "TSP Source NodeId shall be 215ee18f-7869-3492-94d2-0f24ed0a3023");
        assertTrue(iodu4Link.getsourceNetworkSupNodeId().equals("215ee18f-7869-3492-94d2-0f24ed0a3023"),
            "TSP Source Supporting Node Id shall be 215ee18f-7869-3492-94d2-0f24ed0a3023"
            + "(SPDR-SC1-XPDR1+XPONDER)");
        assertTrue(iodu4Link.getSourceTPUuid().getValue().equals("a2f84255-2775-34c9-ab0a-7c17a4249703"),
            "TSP Destination TPId shall be a2f84255-2775-34c9-ab0a-7c17a4249703");
        assertTrue(iodu4Link.getDestUuid().getValue().equals("4e44bcc5-08d3-3fee-8fac-f021489e5a61"),
            "TSP Destination NodeId shall be 4e44bcc5-08d3-3fee-8fac-f021489e5a61");
        assertTrue(iodu4Link.getdestNetworkSupNodeId().equals("4e44bcc5-08d3-3fee-8fac-f021489e5a61"),
            "TSP Destination Supporting Node Id shall be 4e44bcc5-08d3-3fee-8fac-f021489e5a61"
            + "(SPDR-SA1-XPDR1+XPONDER)");
        assertTrue(iodu4Link.getDestTPUuid().getValue().equals("d6e08276-b5a9-3960-a3e1-14f8f72c280b"),
            "TSP Destination TPId shall be d6e08276-b5a9-3960-a3e1-14f8f72c280b");
        assertTrue(iodu4Link.getpowerCorrection().equals(0.0),
            "ODU4 Link (connection) being by default G.652, Power correction shall be 0.0");
        assertTrue(iodu4Link.getOppositeLinkUuid().getValue().equals("b90f7b96-4fe0-390c-8ef2-41942196f19e"),
            "ODU4 Link (connection) opposite Link shall be itself");
    }

    @Test
    void iOdu4LinkX3toX3Test() {
        LOG.info("PceTapiLInkTest Line 561 Entering Test6");
        // TOP+SPDR-SA1-XPDR3+XPDR3-NETWORK1+SPDR-SC1-XPDR3+XPDR3-NETWORK1+iOTU Uuid
        Uuid linkOtu4Uuid = new Uuid("29b6a4ac-b5d3-33aa-aba1-52763259b838");
        // TOP+SPDR-SA1-XPDR3+XPDR3-NETWORK1+SPDR-SC1-XPDR3+XPDR3-NETWORK1+iODU Uuid
        Uuid linkiOdu4Uuid = new Uuid("9e237eea-ce80-3490-9a66-34f7cbdac55f");
        Uuid ioducepSpdrSA1x3 = new Uuid("ced2adb2-e1fd-338c-8b64-a02106d48b45");
        Uuid ioducepSpdrSC1x3 = new Uuid("74b4b605-379f-3700-bb46-97b578cb2c7d");
        Uuid iotucepSpdrSA1x3 = new Uuid("7c72d50f-d830-3e29-928d-4fdbb6e1b9a1");
        Uuid iotucepSpdrSC1x3 = new Uuid("7e241215-6bbe-37eb-8729-bdefc909b97d");
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
            otu4Link = getTapiOtnLinkFromId(linkOtu4Uuid, spdrSA1xpdr3Id, spdrSC1xpdr3Id, iotucepSpdrSA1x3,
                iotucepSpdrSC1x3);
        } catch (ExecutionException e) {
            LOG.error("Unable to get connection {} from mdsal: ", linkOtu4Uuid, e);
        }
        try {
            iodu4Link = getTapiOtnLinkFromId(linkiOdu4Uuid, spdrSA1xpdr3Id, spdrSC1xpdr3Id, ioducepSpdrSA1x3,
                ioducepSpdrSC1x3);
        } catch (ExecutionException e) {
            LOG.error("Unable to get connection {} from mdsal: ", linkiOdu4Uuid, e);
        }
        assertTrue(otu4Link.isValid() == false,
            "OTU4 Link (connection) shall not be valid for DSR/ODU2e services)");
        assertTrue(iodu4Link.isValid() == true,
            "iODU4 Link (connection) shall be valid for DSR/ODU2e services)");
        assertTrue(iodu4Link.getOperationalState().equals(OperationalState.ENABLED),
            "ODU4 Link (connection) Link operational state shall be enabled)");
        assertTrue(iodu4Link.getAdministrativeState().equals(AdministrativeState.UNLOCKED),
            "ODU4 Link (connection) admin state shall be Unlock)");
        assertTrue(iodu4Link.getAvailableBandwidth().equals(Long.valueOf(100000)),
            "ODU4 Link (connection) available bandwidth shall 100000 Mbps)");
        assertTrue(iodu4Link.getpmd2().equals(0.0),"ODU4 Link (connection) PMD shall be 0.0)");
        assertTrue(iodu4Link.getcd().equals(0.0),"ODU4 Link (connection) Chromatic dispersion shall be 0.0)");
        assertTrue(iodu4Link.getspanLoss().equals(0.0),"ODU4 Link (connection) span loss shall be 0.0)");
        assertTrue(iodu4Link.getLength().equals(0.0),"ODU4 Link (connection) Length shall be 0.0)");
        assertTrue(iodu4Link.getLatency().equals(0.0),"ODU4 Link (connection) Latency shall be 0.0)");
        assertTrue(iodu4Link.getsrlgList() == null,"No SRLG declared for OTN Links )");
        assertTrue(iodu4Link.getlinkType().equals(OpenroadmLinkType.OTNLINK),
            "ODU4 Link (connection) link type shall be OTN-LINK)");
        assertTrue(iodu4Link.getLinkName().getValueName().equals("Connection name"),"ODU4 Link (connection) Name"
            + "(Value-name) shall be Connection name)");
        assertTrue(iodu4Link.getLinkName().getValue()
            .equals("TOP+SPDR-SA1-XPDR3+XPDR3-NETWORK1+SPDR-SC1-XPDR3+XPDR3-NETWORK1+iODU"),
            "ODU4 Link (connection) Name (value-name) shall be TOP+SPDR-SA1-XPDR3+XPDR3-NETWORK1+SPDR-SC1-XPDR3"
            + "+XPDR3-NETWORK1+iODU");
        assertTrue(iodu4Link.getLinkUuid().getValue().equals("9e237eea-ce80-3490-9a66-34f7cbdac55f"),
            "ODU4 Link (connection) Id shall be 9e237eea-ce80-3490-9a66-34f7cbdac55f)");
        assertTrue(iodu4Link.getDestUuid().getValue().equals("4582e51f-2b2d-3b70-b374-86c463062710"),
            "TSP Destination NodeId shall be 4582e51f-2b2d-3b70-b374-86c463062710");
        assertTrue(iodu4Link.getdestNetworkSupNodeId().equals("4582e51f-2b2d-3b70-b374-86c463062710"),
            "TSP Destination Supporting Node Id shall be 4582e51f-2b2d-3b70-b374-86c463062710");
        assertTrue(iodu4Link.getDestTPUuid().getValue().equals("ced2adb2-e1fd-338c-8b64-a02106d48b45"),
            "TSP Destination TPId shall be ced2adb2-e1fd-338c-8b64-a02106d48b45");
        assertTrue(iodu4Link.getSourceUuid().getValue().equals("c1f06957-c0b9-32be-8492-e278b2d4a3aa"),
            "TSP Source NodeId shall be c1f06957-c0b9-32be-8492-e278b2d4a3aa");
        assertTrue(iodu4Link.getsourceNetworkSupNodeId().equals("c1f06957-c0b9-32be-8492-e278b2d4a3aa"),
            "TSP Source Supporting Node Id shall be c1f06957-c0b9-32be-8492-e278b2d4a3aa");
        assertTrue(iodu4Link.getSourceTPUuid().getValue().equals("74b4b605-379f-3700-bb46-97b578cb2c7d"),
            "TSP Source TPId shall be 74b4b605-379f-3700-bb46-97b578cb2c7d");
        assertTrue(iodu4Link.getpowerCorrection().equals(0.0),
            "ODU4 Link (connection) being by default G.652, Power correction shall be 0.0");
        assertTrue(iodu4Link.getOppositeLinkUuid().getValue().equals("9e237eea-ce80-3490-9a66-34f7cbdac55f"),
            "ODU4 Link (connection) opposite Link shall be itself");
    }

    @Test
    void iOdu4LinkX1toX1TestNoPortSpecified() {
        LOG.info("PceTapiLInkTest Line 684 Entering Test6");
        // TOP+SPDR-SA1-XPDR1+XPDR1-NETWORK1+SPDR-SC1-XPDR1+XPDR1-NETWORK1+iODU Uuid
        Uuid linkiOdu4Uuid = new Uuid("b90f7b96-4fe0-390c-8ef2-41942196f19e");
        Uuid ioducepSpdrSA1x1 = new Uuid("d6e08276-b5a9-3960-a3e1-14f8f72c280b");
        Uuid ioducepSpdrSC1x1 = new Uuid("a2f84255-2775-34c9-ab0a-7c17a4249703");
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
            iodu4Link = getTapiOtnLinkFromId(linkiOdu4Uuid, spdrSA1xpdr1Id, spdrSC1xpdr1Id, ioducepSpdrSA1x1,
                ioducepSpdrSC1x1);
        } catch (ExecutionException e) {
            LOG.error("Unable to get connection {} from mdsal: ", linkiOdu4Uuid, e);
        }

        assertTrue(iodu4Link.isValid() == true,
            "iODU4 Link (connection) shall be valid for DSR/ODU2e services)");
        assertTrue(iodu4Link.getOperationalState().equals(OperationalState.ENABLED),
            "ODU4 Link (connection) Link operational state shall be enabled)");
        assertTrue(iodu4Link.getAdministrativeState().equals(AdministrativeState.UNLOCKED),
            "ODU4 Link (connection) admin state shall be Unlock)");
        assertTrue(iodu4Link.getAvailableBandwidth().equals(Long.valueOf(100000)),
            "ODU4 Link (connection) available bandwidth shall 100000 Mbps)");
        assertTrue(iodu4Link.getpmd2().equals(0.0),"ODU4 Link (connection)PMD shall be 0.0)");
        assertTrue(iodu4Link.getcd().equals(0.0),"ODU4 Link (connection) Chromatic dispersion shall be 0.0)");
        assertTrue(iodu4Link.getspanLoss().equals(0.0),"ODU4 Link (connection) span loss shall be 0.0)");
        assertTrue(iodu4Link.getLength().equals(0.0),"ODU4 Link (connection) Length shall be 0.0)");
        assertTrue(iodu4Link.getLatency().equals(0.0),"ODU4 Link (connection) Latency shall be 0.0)");
        assertTrue(iodu4Link.getsrlgList() == null,"No SRLG declared for OTN Links )");
        assertTrue(iodu4Link.getlinkType().equals(OpenroadmLinkType.OTNLINK),
            "ODU4 Link (connection) link type shall be OTN-LINK)");
        assertTrue(iodu4Link.getLinkName().getValueName().equals("Connection name"),"ODU4 Link (connection) Name"
            + "(Value-name) shall be Connection name)");
        assertTrue(iodu4Link.getLinkName().getValue()
            .equals("TOP+SPDR-SA1-XPDR1+XPDR1-NETWORK1+SPDR-SC1-XPDR1+XPDR1-NETWORK1+iODU"),
            "ODU4 Link (connection) Name (value-name) shall be TOP+SPDR-SA1-XPDR1+XPDR1-NETWORK1"
            + "+SPDR-SC1-XPDR1+XPDR1-NETWORK1+iODU");
        assertTrue(iodu4Link.getLinkUuid().getValue().equals("b90f7b96-4fe0-390c-8ef2-41942196f19e"),
            "ODU4 Link (connection) Id shall be b90f7b96-4fe0-390c-8ef2-41942196f19e)");
        assertTrue(iodu4Link.getSourceUuid().getValue().equals("215ee18f-7869-3492-94d2-0f24ed0a3023"),
            "TSP Source NodeId shall be 215ee18f-7869-3492-94d2-0f24ed0a3023");
        assertTrue(iodu4Link.getsourceNetworkSupNodeId().equals("215ee18f-7869-3492-94d2-0f24ed0a3023"),
            "TSP Source Supporting Node Id shall be 215ee18f-7869-3492-94d2-0f24ed0a3023"
            + "(SPDR-SC1-XPDR1+XPONDER)");
        assertTrue(iodu4Link.getSourceTPUuid().getValue().equals("a2f84255-2775-34c9-ab0a-7c17a4249703"),
            "TSP Destination TPId shall be a2f84255-2775-34c9-ab0a-7c17a4249703");
        assertTrue(iodu4Link.getDestUuid().getValue().equals("4e44bcc5-08d3-3fee-8fac-f021489e5a61"),
            "TSP Destination NodeId shall be 4e44bcc5-08d3-3fee-8fac-f021489e5a61");
        assertTrue(iodu4Link.getdestNetworkSupNodeId().equals("4e44bcc5-08d3-3fee-8fac-f021489e5a61"),
            "TSP Destination Supporting Node Id shall be 4e44bcc5-08d3-3fee-8fac-f021489e5a61"
            + "(SPDR-SA1-XPDR1+XPONDER)");
        assertTrue(iodu4Link.getDestTPUuid().getValue().equals("d6e08276-b5a9-3960-a3e1-14f8f72c280b"),
            "TSP Destination TPId shall be d6e08276-b5a9-3960-a3e1-14f8f72c280b");
        assertTrue(iodu4Link.getpowerCorrection().equals(0.0),
            "ODU4 Link (connection) being by default G.652, Power correction shall be 0.0");
        assertTrue(iodu4Link.getOppositeLinkUuid().getValue().equals("b90f7b96-4fe0-390c-8ef2-41942196f19e"),
            "ODU4 Link (connection) opposite Link shall be itself");
    }

    @Test
    void otuX2toX2LinkTestNoPortSpecified() {
        LOG.info("PceTapiLInkTest Line 814 Entering Test7");
        // TOP+SPDR-SA1-XPDR2+XPDR2-NETWORK1+SPDR-SC1-XPDR2+XPDR2-NETWORK1+iOTU Uuid
        Uuid linkOtu4Uuid = new Uuid("07df4edd-4408-310d-a820-5f34b0524900");
        Uuid iotucepSpdrSA1x2 = new Uuid("bb58ebb0-ca7c-3518-8ffd-808abfca54e5");
        Uuid iotucepSpdrSC1x2 = new Uuid("b9dbee10-faa9-3947-94c1-3c023646a2df");
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
            otu4Link = getTapiOtnLinkFromId(linkOtu4Uuid, spdrSA1xpdr2Id, spdrSC1xpdr2Id, iotucepSpdrSA1x2,
                iotucepSpdrSC1x2);
        } catch (ExecutionException e) {
            LOG.error("Unable to get connection {} from mdsal: ", linkOtu4Uuid, e);
        }
        assertTrue(otu4Link.isValid() == true,
            "OTU4 Link (connection) shall not be valid for ODU4 services)");
        assertTrue(otu4Link.getOperationalState().equals(OperationalState.ENABLED),
            "OTU4 Link (connection) Link operational state shall be enabled)");
        assertTrue(otu4Link.getAdministrativeState().equals(AdministrativeState.UNLOCKED),
            "OTU4 Link (connection) admin state shall be Unlock)");
        assertTrue(otu4Link.getAvailableBandwidth().equals(Long.valueOf(100000)),
            "OTU4 Link (connection) available bandwidth shall 100000 in Mbps)");
        assertTrue(otu4Link.getpmd2().equals(0.0),"OTU4 Link (connection)PMD shall be 0.0)");
        assertTrue(otu4Link.getcd().equals(0.0),"OTU4 Link (connection) Chromatic dispersion shall be 0.0)");
        assertTrue(otu4Link.getspanLoss().equals(0.0),"OTU4 Link (connection) span loss shall be 0.0)");
        assertTrue(otu4Link.getLength().equals(0.0),"OTU4 Link (connection) Length shall be 0.0)");
        assertTrue(otu4Link.getLatency().equals(0.0),"OTU4 Link (connection) Latency shall be 0.0)");
        assertTrue(otu4Link.getsrlgList() == null,"No SRLG declared for OTN Links )");
        assertTrue(otu4Link.getlinkType().equals(OpenroadmLinkType.OTNLINK),
            "OTU4 Link (connection) link type shall be OTN-LINK)");
        assertTrue(otu4Link.getLinkName().getValueName().equals("Connection name"),"OTU4 Link (connection) Name"
            + "(Value-name) shall be Connection name)");
        assertTrue(otu4Link.getLinkName().getValue()
            .equals("TOP+SPDR-SA1-XPDR2+XPDR2-NETWORK1+SPDR-SC1-XPDR2+XPDR2-NETWORK1+iOTU"),
            "OTU4 Link (connection) Name (value-name) shall be TOP+SPDR-SA1-XPDR2+XPDR2-NETWORK1+SPDR-SC1-XPDR2"
            + "+XPDR2-NETWORK1+iOTU");
        assertTrue(otu4Link.getLinkUuid().getValue().equals("07df4edd-4408-310d-a820-5f34b0524900"),
            "OTU4 Link (connection) Id shall be 07df4edd-4408-310d-a820-5f34b0524900)");
        assertTrue(otu4Link.getDestUuid().getValue().equals("38c114ae-9c0e-3068-bb27-db2dbd81220b"),
            "TSP Source NodeId shall be 38c114ae-9c0e-3068-bb27-db2dbd81220b");
        assertTrue(otu4Link.getdestNetworkSupNodeId().equals("38c114ae-9c0e-3068-bb27-db2dbd81220b"),
            "TSP Source Supporting Node Id shall be 38c114ae-9c0e-3068-bb27-db2dbd81220b");
        assertTrue(otu4Link.getDestTPUuid().getValue().equals("bb58ebb0-ca7c-3518-8ffd-808abfca54e5"),
            "TSP Source TPId shall be bb58ebb0-ca7c-3518-8ffd-808abfca54e5");
        assertTrue(otu4Link.getSourceUuid().getValue().equals("d852c340-77db-3f9a-96e8-cb4de8e1004a"),
            "TSP Destination NodeId shall be d852c340-77db-3f9a-96e8-cb4de8e1004a");
        assertTrue(otu4Link.getsourceNetworkSupNodeId().equals("d852c340-77db-3f9a-96e8-cb4de8e1004a"),
            "TSP Destination Supporting Node Id shall be d852c340-77db-3f9a-96e8-cb4de8e1004a");
        assertTrue(otu4Link.getSourceTPUuid().getValue().equals("b9dbee10-faa9-3947-94c1-3c023646a2df"),
            "TSP Destination TPId shall be b9dbee10-faa9-3947-94c1-3c023646a2df");
        assertTrue(otu4Link.getpowerCorrection().equals(0.0),
            "OTU4 Link (connection) being by default G.652, Power correction shall be 0.0");
        assertTrue(otu4Link.getOppositeLinkUuid().getValue().equals("07df4edd-4408-310d-a820-5f34b0524900"),
            "OTU4 Link (connection) opposite Link shall be itself");
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
        if (link == null) {
            LOG.info("PceTapiLInkTest Line 496, null node");
        } else {
            LOG.info("PceTapiLInkTest Line 498, link is {}", link.getName());
            LOG.info("PceTapiLInkTest Line 499, slotwidthGranularity is  {}", slotWidthGranularity);
        }

        TapiOpticalNode ton = getTapiOpticalNodeFromId(nodeXuuid);
        Map<Uuid, PceTapiOpticalNode> nodeXpceNodeMap = getPceTapiOpticalNodeFromId(nodeXuuid, ton);
        Uuid disagNodeXUuid = null;
        if (nodeXpceNodeMap.size() == 1) {
            // This is the case of a Xponder
            disagNodeXUuid = nodeXpceNodeMap.entrySet().iterator().next().getKey();
        } else {
            for (Map.Entry<Uuid, PceTapiOpticalNode> entry : nodeXpceNodeMap.entrySet()) {
                if (entry.getValue().getListOfNep().stream().map(BasePceNep:: getNepCepUuid)
                        .collect(Collectors.toList()).contains(nepXUuid)) {
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
                if (entry.getValue().getListOfNep().stream().map(BasePceNep:: getNepCepUuid)
                    .collect(Collectors.toList()).contains(nepYUuid)) {
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

    private PceTapiLink getTapiOtnLinkFromId(Uuid linkId, Uuid nodeXuuid, Uuid nodeYuuid,
            Uuid nepXUuid, Uuid nepYUuid)
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
        if (conn == null) {
            LOG.info("PceTapiLInkTest Line 573, null node");
        } else {
            LOG.info("PceTapiLInkTest Line 575, connection is {}", conn.getName());
            LOG.info("PceTapiLInkTest Line 576, slotwidthGranularity is  {}", slotWidthGranularity);
        }


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
        TapiOpticalNode ton = new TapiOpticalNode(serviceType, portMapping, node, version, slotWidthGranularity,
            centralFreqGranularity, anodeId, znodeId, aportId, zportId, serviceFormat, mcCapability);
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
