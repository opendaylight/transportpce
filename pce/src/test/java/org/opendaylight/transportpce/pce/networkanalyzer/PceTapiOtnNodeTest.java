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
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import org.eclipse.jdt.annotation.NonNull;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
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
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.types.rev230526.OpenroadmNodeType;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.format.rev191129.ServiceFormat;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.AdministrativeState;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.Context;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.OperationalState;
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

@Execution(ExecutionMode.SAME_THREAD)
public class PceTapiOtnNodeTest extends AbstractTest {
    private static final Logger LOG = LoggerFactory.getLogger(PceTapiOtnNodeTest.class);
    private static final String TOPOLOGY_FILE = "src/test/resources/topologyData/refTopoTapiFull.xml";
    private static Context tapiContext;
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
    void testvalidateAZxponderForSPDRA1X1() {
        // SPDR-XPDR1 already supports One wavelength, One ODU4, and one DSR/ETh service
        LOG.info("Entering Test1 ");
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
        PceTapiOtnNode pceONspdrSA1 = tapiONspdrAx1.getXpdrOtnNode();

        assertTrue(pceONspdrSA1.isValid(),
            "SPDR-SA1 Node shall be a valid OTN node");
        assertTrue(pceONspdrSA1.getNodeId().getValue().toString().equals("4e44bcc5-08d3-3fee-8fac-f021489e5a61"),
            "SPDR-SA1 Node shall have SPDR-SA1-XPDR1+XPONDER NodeId");
        assertTrue(pceONspdrSA1.getSupNetworkNodeId().equals("4e44bcc5-08d3-3fee-8fac-f021489e5a61"),
            "SPDR-SA1 Node shall have SPDR-SA1-XPDR1 Uuid as supporting Network NodeId");
        assertTrue(pceONspdrSA1.getSupClliNodeId().equals("4e44bcc5-08d3-3fee-8fac-f021489e5a61"),
            "SPDR-SA1 Node shall have SPDR-SA1-XPDR1 Uuid as supporting CLLI NodeId");
        assertTrue(pceONspdrSA1.getORNodeType().equals(OpenroadmNodeType.XPONDER),
            "SPDR-SA1 Node shall be of Xponder Type");
        assertTrue(pceONspdrSA1.getPceNodeType().equals("otn"),
            "SPDR-SA1 Node shall be of PceNodeType otn");
        assertTrue(pceONspdrSA1.getAdminState().equals(AdministrativeState.UNLOCKED),
            "SPDR-SA1 Node AdminState shall be UNLOCKED");
        assertTrue(pceONspdrSA1.getAdminStates() == null,
            "SPDR-SA1 Node AdminStates shall be null");
        assertTrue(pceONspdrSA1.getOperationalState().equals(OperationalState.ENABLED),
            "SPDR-SA1 Node Operational State shall be ENABLED");
        assertTrue(pceONspdrSA1.getState() == null,
            "SPDR-SA1 Node State shall be null");
        LOG.info("TEST OTN Line 184 SPDR-SA1 has the following NEPS in list of NEp {} ",
            pceONspdrSA1.getTotalListOfNep().stream().map(BasePceNep::getName).collect(Collectors.toList()));
        // Check NEP of the current Node
        assertEquals(10, pceONspdrSA1.getTotalListOfNep().size(),
            "SPDR-SA1 has 10 NEPs prefiltered NEP : 1 OTS, 2 (CEP+NEP)iODU, 3x2 (CEP+NEP) eODU, 1DSR");
        assertEquals(6, pceONspdrSA1.getTotalListOfNep().stream().filter(bpn -> bpn.getName().entrySet().stream()
            .findFirst().orElseThrow().getValue().getValue().contains("eODU")).collect(Collectors.toList()).size(),
            "SPDR-SA1 has 6 (CEP +NEP) eODU NEPs C1 eODU being already provisioned");
        assertEquals(1, pceONspdrSA1.getTotalListOfNep().stream().filter(bpn -> bpn.getName().entrySet().stream()
            .findFirst().orElseThrow().getValue().getValue().contains("DSR")).collect(Collectors.toList()).size(),
            "SPDR-SA1 has 1 DSR NEPs which corresponds to aPortId");
        assertEquals(2, pceONspdrSA1.getTotalListOfNep().stream().filter(bpn -> bpn.getName().entrySet().stream()
            .findFirst().orElseThrow().getValue().getValue().contains("iODU")).collect(Collectors.toList()).size(),
            "SPDR-SA1 has 2 (CEP+NEP) iODU NEPs (Network1)");
        assertEquals(1, pceONspdrSA1.getTotalListOfNep().stream().filter(bpn -> bpn.getName().entrySet().stream()
            .findFirst().orElseThrow().getValue().getValue().contains("OTS")).collect(Collectors.toList()).size(),
            "SPDR-SA1 has 1 OTS NEPs");
        // Check Valid (of Interest) NEP of the current Node
        LOG.info("TEST OTN Line 202 SPDR-SA1 has the following NEPS in list of NEp {} ",
            pceONspdrSA1.getListOfNep().stream().map(BasePceNep::getName).collect(Collectors.toList()));
        assertEquals(8, pceONspdrSA1.getListOfNep().size(),
            "SPDR-SA1 includes 8 NEp and Ceps at ODU/OTU level /OTU : 2x3eODU, 2 iODU");
        assertEquals(6, pceONspdrSA1.getListOfNep().stream().filter(bpn -> bpn.getName().entrySet().stream()
            .findFirst().orElseThrow().getValue().getValue().contains("eODU")).collect(Collectors.toList()).size(),
            "SPDR-SA1 has 6 eODU NEPs/CEP C1 eODU being already provisioned");
        assertEquals(1, pceONspdrSA1.getUsableXpdrClientTps().stream().filter(bpn -> bpn.getName().entrySet().stream()
            .findFirst().orElseThrow().getValue().getValue().contains("DSR")).collect(Collectors.toList()).size(),
            "SPDR-SA1 has 1 DSR NEPs which corresponds to aPortId");
        assertEquals(2, pceONspdrSA1.getUsableXpdrNWTps().stream().filter(bpn -> bpn.getName().entrySet().stream()
            .findFirst().orElseThrow().getValue().getValue().contains("iODU")).collect(Collectors.toList()).size(),
            "SPDR-SA1 has 2 iODU NEPs/CEP (Network1)");
        assertEquals(2, pceONspdrSA1.getUsableXpdrClientTps().size(),
            "SPDR-SA1 has 2 selectable NEP (eODU NEP never considered as potential service endpoint)  at ODU/DSR level"
            + "after Nep have been pruned : 1 DSR, 1eODU");
        assertEquals(2, pceONspdrSA1.getUsableXpdrNWTps().size(),
                "SPDR-SA1 has 2 selectable NEP/CEP at iODU level after Cep/Nep have been pruned : 2 iODU");
        assertTrue(pceONspdrSA1.getSlotWidthGranularity() == null,
            "An Otn Node returns null slotWidth Granularity");
        assertTrue(pceONspdrSA1.getCentralFreqGranularity() == null,
            "An Otn Node returns null Central Frequency Granularity");
        assertTrue(pceONspdrSA1.getBitSetData() == null, "Spectrum is not defined at OTN level");

    }

    @Test
    void testvalidateAZxponderForSPDRA1X1NoPortSpecified() {
        // SPDR-XPDR1 already supports One wavelength, One ODU4, and one DSR/ETh service
        LOG.info("Entering Test2 ");
        serviceType = "10GE";
        anodeId = spdrSA1xpdr1Id;
        znodeId = spdrSC1xpdr1Id;
        try {
            generalSetUp();
        } catch (ExecutionException e) {
            LOG.error("Unable to get node from mdsal: ", e);
        }
        initializeAll();

        PceTapiOtnNode pceONspdrSA1 = tapiONspdrAx1.getXpdrOtnNode();
        assertTrue(pceONspdrSA1.isValid(),
            "SPDR-SA1 Node shall be a valid OTN node");
        assertTrue(pceONspdrSA1.getNodeId().getValue().toString().equals("4e44bcc5-08d3-3fee-8fac-f021489e5a61"),
            "SPDR-SA1 Node shall have SPDR-SA1-XPDR1+XPONDER NodeId");
        assertTrue(pceONspdrSA1.getORNodeType().equals(OpenroadmNodeType.XPONDER),
            "SPDR-SA1 Node shall be of Xponder Type");
        assertTrue(pceONspdrSA1.getPceNodeType().equals("otn"),
            "SPDR-SA1 Node shall be of PceNodeType otn");
        assertTrue(pceONspdrSA1.getAdminState().equals(AdministrativeState.UNLOCKED),
            "SPDR-SA1 Node AdminState shall be UNLOCKED");
        assertTrue(pceONspdrSA1.getOperationalState().equals(OperationalState.ENABLED),
            "SPDR-SA1 Node Operational State shall be ENABLED");
        LOG.info("TEST OTN Line 250 SPDR-SA1 has the following NEPS in list of NEp {} ",
            pceONspdrSA1.getTotalListOfNep().stream().map(BasePceNep::getName).collect(Collectors.toList()));
        // Check NEP of the current Node
        assertEquals(12, pceONspdrSA1.getTotalListOfNep().size(),
            "SPDR-SA1 has 12 NEPs prefiltered NEP");
        assertEquals(6, pceONspdrSA1.getTotalListOfNep().stream().filter(bpn -> bpn.getName().entrySet().stream()
            .findFirst().orElseThrow().getValue().getValue().contains("eODU")).collect(Collectors.toList()).size(),
            "SPDR-SA1 has 3x2 eODU NEPs/CEPs C1 eODU being already provisioned");
        assertEquals(3, pceONspdrSA1.getTotalListOfNep().stream().filter(bpn -> bpn.getName().entrySet().stream()
            .findFirst().orElseThrow().getValue().getValue().contains("DSR")).collect(Collectors.toList()).size(),
            "SPDR-SA1 has 3 DSR NEPs which corresponds to aPortId");
        assertEquals(2, pceONspdrSA1.getTotalListOfNep().stream().filter(bpn -> bpn.getName().entrySet().stream()
            .findFirst().orElseThrow().getValue().getValue().contains("iODU")).collect(Collectors.toList()).size(),
            "SPDR-SA1 has 2 iODU NEPs/CEPs (Network1)");
        assertEquals(1, pceONspdrSA1.getTotalListOfNep().stream().filter(bpn -> bpn.getName().entrySet().stream()
            .findFirst().orElseThrow().getValue().getValue().contains("OTS")).collect(Collectors.toList()).size(),
            "SPDR-SA1 has 1 OTS NEPs");
        // Check Valid (of Interest) NEP of the current Node
        LOG.info("TEST OTN Line 283 SPDR-SA1 has the following NEPS in list of NEp {} ",
            pceONspdrSA1.getListOfNep().stream().map(BasePceNep::getName).collect(Collectors.toList()));
        assertEquals(8, pceONspdrSA1.getListOfNep().size(),
            "SPDR-SA1 has 8 selectable NEP/CEP at ODU level after Nep have been pruned : 3x2 eODU, 1x2 iODU");
        assertEquals(3, pceONspdrSA1.getUsableXpdrClientTps().stream().filter(bpn -> bpn.getName().entrySet().stream()
            .findFirst().orElseThrow().getValue().getValue().contains("DSR")).collect(Collectors.toList()).size(),
            "3 DSR ports are eligible");
    }

    @Test
    void testvalidateAZxponderForSPDRA1X2NetworkService() {
        // SPDR-XPDR2 already supports One wavelength + One OTU4 services. ODU4 not created
        LOG.info("Entering Test3 ");
        //Reinitializing Xponders according to new anodeId, znodeId, aportId, zportId
        anodeId = spdrSA1xpdr2Id;
        znodeId = spdrSC1xpdr2Id;
        serviceType = "ODU4";
        //SPDR-SA1-XPDR2+iODU+XPDR2-NETWORK1 = "6f4777d4-41f0-3833-b811-68b90903d834"
        aportId = new Uuid("6f4777d4-41f0-3833-b811-68b90903d834");
        // SPDR-SC1-XPDR2+iODU+XPDR2-NETWORK1 = ""
        zportId = new Uuid("8b82dac3-646c-301d-b455-b7a4802774f7");
        try {
            generalSetUp();
        } catch (ExecutionException e) {
            LOG.error("Unable to get node from mdsal: ", e);
        }
        initializeAll();

        PceTapiOtnNode pceONspdrSA2 = tapiONspdrAx2.getXpdrOtnNode();
        assertTrue(pceONspdrSA2.isValid(),
            "SPDR-SA1 Node shall be a valid OTN node");
        assertTrue(pceONspdrSA2.getNodeId().getValue().toString().equals("38c114ae-9c0e-3068-bb27-db2dbd81220b"),
            "SPDR-SA1 Node shall have SPDR-SA2-XPDR1+XPONDER NodeId");
        assertTrue(pceONspdrSA2.getSupNetworkNodeId().equals("38c114ae-9c0e-3068-bb27-db2dbd81220b"),
            "SPDR-SA1 Node shall have SPDR-SA1-XPDR1 Uuid as supporting Network NodeId");
        assertTrue(pceONspdrSA2.getSupClliNodeId().equals("38c114ae-9c0e-3068-bb27-db2dbd81220b"),
            "SPDR-SA1 Node shall have SPDR-SA1-XPDR1 Uuid as supporting CLLI NodeId");
        assertTrue(pceONspdrSA2.getORNodeType().equals(OpenroadmNodeType.XPONDER),
            "SPDR-SA1 Node shall be of Xponder Type");
        assertTrue(pceONspdrSA2.getPceNodeType().equals("otn"),
            "SPDR-SA1 Node shall be of PceNodeType otn");
        assertTrue(pceONspdrSA2.getAdminState().equals(AdministrativeState.UNLOCKED),
            "SPDR-SA1 Node AdminState shall be UNLOCKED");
        assertTrue(pceONspdrSA2.getOperationalState().equals(OperationalState.ENABLED),
            "SPDR-SA1 Node Operational State shall be ENABLED");
        LOG.info("TEST OTN Line 313 SPDR-SA1 has the following NEPS in list of NEp {} ",
            pceONspdrSA2.getTotalListOfNep().stream().map(BasePceNep::getName).collect(Collectors.toList()));
        // Check NEP of the current Node
        assertEquals(10, pceONspdrSA2.getTotalListOfNep().size(),
            "SPDR-SA1 has 9 NEPs (4 iODU + 4 iOTU + 1 OTS) and 1 CEP (iOTU N1) available");
        assertEquals(4, pceONspdrSA2.getTotalListOfNep().stream().filter(bpn -> bpn.getName().entrySet().stream()
            .findFirst().orElseThrow().getValue().getValue().contains("iODU")).collect(Collectors.toList()).size(),
            "SPDR-SA1 has 4 iODU NEPs (Network1/2/3/4)");
        assertEquals(5, pceONspdrSA2.getTotalListOfNep().stream().filter(bpn -> bpn.getName().entrySet().stream()
            .findFirst().orElseThrow().getValue().getValue().contains("iOTU")).collect(Collectors.toList()).size(),
            "SPDR-SA1 has 4 iODU NEPs (Network1/2/3/4) + 1 iOTU CEP (N1)");
        assertEquals(1, pceONspdrSA2.getTotalListOfNep().stream().filter(bpn -> bpn.getName().entrySet().stream()
            .findFirst().orElseThrow().getValue().getValue().contains("OTS")).collect(Collectors.toList()).size(),
            "SPDR-SA1 has 1 OTS NEPs, only one wavelength being provisioned");
        assertEquals("ff69716d-325d-35fc-baa9-0c4bcd992bbd",
            pceONspdrSA2.getTotalListOfNep().stream().filter(bpn -> bpn.getName().entrySet().stream()
                .findFirst().orElseThrow().getValue().getValue().contains("OTS")).collect(Collectors.toList())
                .stream().findFirst().orElseThrow().getNepCepUuid().getValue(),
            "SPDR-SA1 has 1 OTS NEPs, only one wavelength being provisioned");
        // Check Valid (of Interest) NEP of the current Node
        LOG.info("TEST OTN Line 348 SPDR-SA1 has the following NEPS in list of NEp {} ",
            pceONspdrSA2.getListOfNep().stream().map(BasePceNep::getName).collect(Collectors.toList()));
        assertEquals(0, pceONspdrSA2.getUsableXpdrNWTps().size(),
            "SPDR-SA1 as a switch has 1 selectable NEP at iODU level after Nep have been pruned : the specified iODU"
            + "which is, for switchPonder, considered as client and not included in AvailableNWPorts");
        assertEquals(1, pceONspdrSA2.getUsableXpdrClientTps().size(),
            "SPDR-SA1 as a switch has 1 selectable NEP at iODU level wich also appears in ClientXpdrAvailable tps");
        assertEquals(0, pceONspdrSA2.getListOfNep().stream().filter(bpn -> bpn.getName().entrySet().stream()
            .findFirst().orElseThrow().getValue().getValue().contains("DSR")).collect(Collectors.toList()).size(),
            "Only One DSR from 3 was kept, selecting the first available port");
    }


    @Test
    void testvalidateAZxponderForSPDRA1X2NetworkServiceNoPortSpecified() {
        // SPDR-XPDR2 already supports One wavelength + One OTU4 services. ODU4 not created
        LOG.info("Entering Test4 ");
        //Reinitializing Xponders according to new anodeId, znodeId, aportId, zportId
        anodeId = spdrSA1xpdr2Id;
        znodeId = spdrSC1xpdr2Id;
        serviceType = "ODU4";
        try {
            generalSetUp();
        } catch (ExecutionException e) {
            LOG.error("Unable to get node from mdsal: ", e);
        }
        initializeAll();

        PceTapiOtnNode pceONspdrSA2 = tapiONspdrAx2.getXpdrOtnNode();
        assertTrue(pceONspdrSA2.isValid(),
            "SPDR-SA1 Node shall be a valid OTN node");
        assertTrue(pceONspdrSA2.getNodeId().getValue().toString().equals("38c114ae-9c0e-3068-bb27-db2dbd81220b"),
            "SPDR-SA1 Node shall have SPDR-SA2-XPDR1+XPONDER NodeId");
        assertTrue(pceONspdrSA2.getORNodeType().equals(OpenroadmNodeType.XPONDER),
            "SPDR-SA1 Node shall be of Xponder Type");
        assertTrue(pceONspdrSA2.getPceNodeType().equals("otn"),
            "SPDR-SA1 Node shall be of PceNodeType otn");
        assertTrue(pceONspdrSA2.getAdminState().equals(AdministrativeState.UNLOCKED),
            "SPDR-SA1 Node AdminState shall be UNLOCKED");
        assertTrue(pceONspdrSA2.getOperationalState().equals(OperationalState.ENABLED),
            "SPDR-SA1 Node Operational State shall be ENABLED");
        LOG.info("TEST OTN Line 384 SPDR-SA1 has the following NEPS in list of NEp {} ",
            pceONspdrSA2.getTotalListOfNep().stream().map(BasePceNep::getName).collect(Collectors.toList()));
        // Check NEP of the current Node
        assertEquals(17, pceONspdrSA2.getTotalListOfNep().size(),
            "SPDR-SA1 has 17 NEPs prefiltered NEP");
        assertEquals(4, pceONspdrSA2.getTotalListOfNep().stream().filter(bpn -> bpn.getName().entrySet().stream()
            .findFirst().orElseThrow().getValue().getValue().contains("iODU")).collect(Collectors.toList()).size(),
            "SPDR-SA1 has 4 iODU NEP (Network1/2/3/4)");
        assertEquals(5, pceONspdrSA2.getTotalListOfNep().stream().filter(bpn -> bpn.getName().entrySet().stream()
            .findFirst().orElseThrow().getValue().getValue().contains("iOTU")).collect(Collectors.toList()).size(),
            "SPDR-SA1 has 4 iOTU NEPs and 1 iOTU CEP (Network1)");
        assertEquals(4, pceONspdrSA2.getTotalListOfNep().stream().filter(bpn -> bpn.getName().entrySet().stream()
            .findFirst().orElseThrow().getValue().getValue().contains("DSR")).collect(Collectors.toList()).size(),
            "SPDR-SA1 has 4 DSR NEPs, (Client  1/2/3/4");
        assertEquals(4, pceONspdrSA2.getTotalListOfNep().stream().filter(bpn -> bpn.getName().entrySet().stream()
            .findFirst().orElseThrow().getValue().getValue().contains("OTS")).collect(Collectors.toList()).size(),
            "SPDR-SA1 has 4 OTS NEPs, only one wavelength being provisioned");
        // Check Valid (of Interest) NEP of the current Node
        LOG.info("TEST OTN Line 399 SPDR-SA1 has the following NEPS in list of NEp {} ",
            pceONspdrSA2.getListOfNep().stream().map(BasePceNep::getName).collect(Collectors.toList()));
        assertEquals(9, pceONspdrSA2.getListOfNep().size(),
            "SPDR-SA1 has 8 NEP (4 OTS + 4 iOTU) and 1 NEP (1 iOTU) at OTN level after Nep have been pruned");
    }

    @Test
    void testvalidateAZxponderForSPDRA1X3() {
        // SPDR-XPDR1 already supports One wavelength, One ODU4, but no DSR/ETh service
        LOG.info("Entering Test5 ");
        serviceType = "1GE";
        anodeId = spdrSA1xpdr3Id;
        znodeId = spdrSC1xpdr3Id;
        //SPDR-SA1-XPDR3+DSR+XPDR3-CLIENT2 UUID = "217abe36-25ee-337a-b919-f051faf88b21"
        aportId = new Uuid("217abe36-25ee-337a-b919-f051faf88b21");
        ///SPDR-SC1-XPDR3+DSR+XPDR3-CLIENT2 UUID = "360b276b-4beb-3c96-8650-559e9934deaa"
        zportId = new Uuid("360b276b-4beb-3c96-8650-559e9934deaa");
        try {
            generalSetUp();
        } catch (ExecutionException e) {
            LOG.error("Unable to get node from mdsal: ", e);
        }
        initializeAll();
        PceTapiOtnNode pceONspdrSA1 = tapiONspdrAx3.getXpdrOtnNode();

        assertTrue(pceONspdrSA1.isValid(),
            "SPDR-SA1 Node shall be a valid OTN node");
        assertTrue(pceONspdrSA1.getNodeId().getValue().toString().equals("4582e51f-2b2d-3b70-b374-86c463062710"),
            "SPDR-SA1 Node shall have SPDR-SA1-XPDR1+XPONDER NodeId");
        assertTrue(pceONspdrSA1.getSupNetworkNodeId().equals("4582e51f-2b2d-3b70-b374-86c463062710"),
            "SPDR-SA1 Node shall have SPDR-SA1-XPDR1 Uuid as supporting Network NodeId");
        assertTrue(pceONspdrSA1.getSupClliNodeId().equals("4582e51f-2b2d-3b70-b374-86c463062710"),
            "SPDR-SA1 Node shall have SPDR-SA1-XPDR1 Uuid as supporting CLLI NodeId");
        assertTrue(pceONspdrSA1.getORNodeType().equals(OpenroadmNodeType.XPONDER),
            "SPDR-SA1 Node shall be of Xponder Type");
        assertTrue(pceONspdrSA1.getPceNodeType().equals("otn"),
            "SPDR-SA1 Node shall be of PceNodeType otn");
        assertTrue(pceONspdrSA1.getAdminState().equals(AdministrativeState.UNLOCKED),
            "SPDR-SA1 Node AdminState shall be UNLOCKED");
        assertTrue(pceONspdrSA1.getAdminStates() == null,
            "SPDR-SA1 Node AdminStates shall be null");
        assertTrue(pceONspdrSA1.getOperationalState().equals(OperationalState.ENABLED),
            "SPDR-SA1 Node Operational State shall be ENABLED");
        assertTrue(pceONspdrSA1.getState() == null,
            "SPDR-SA1 Node State shall be null");
        LOG.info("TEST OTN Line 429 SPDR-SA1 has the following NEPS in list of NEp {} ",
            pceONspdrSA1.getTotalListOfNep().stream().map(BasePceNep::getName).collect(Collectors.toList()));
        // Check NEP of the current Node
        assertEquals(12, pceONspdrSA1.getTotalListOfNep().size(),
            "SPDR-SA1 has 12 NEPs prefiltered NEP : 1 OTS, 1x2 (CEP+NEP)iODU, 4x2 (CEP+NEP) eODU, 1DSR");
        assertEquals(8, pceONspdrSA1.getTotalListOfNep().stream().filter(bpn -> bpn.getName().entrySet().stream()
            .findFirst().orElseThrow().getValue().getValue().contains("eODU")).collect(Collectors.toList()).size(),
            "SPDR-SA1 has 4X2 eODU NEP/CEPs eODU available");
        assertEquals(1, pceONspdrSA1.getTotalListOfNep().stream().filter(bpn -> bpn.getName().entrySet().stream()
            .findFirst().orElseThrow().getValue().getValue().contains("DSR")).collect(Collectors.toList()).size(),
            "SPDR-SA1 has 1 DSR NEPs which corresponds to aPortId");
        assertEquals(2, pceONspdrSA1.getTotalListOfNep().stream().filter(bpn -> bpn.getName().entrySet().stream()
            .findFirst().orElseThrow().getValue().getValue().contains("iODU")).collect(Collectors.toList()).size(),
            "SPDR-SA1 has 2x1 iODU NEP/CEPs (Network1)");
        assertEquals(1, pceONspdrSA1.getTotalListOfNep().stream().filter(bpn -> bpn.getName().entrySet().stream()
            .findFirst().orElseThrow().getValue().getValue().contains("OTS")).collect(Collectors.toList()).size(),
            "SPDR-SA1 has 1 OTS NEPs");
        // Check Valid (of Interest) NEP of the current Node
        assertEquals(10, pceONspdrSA1.getListOfNep().size(),
            "SPDR-SA1 has 10 selectable NEP/CEP at ODU level after Nep have been pruned : 2x 4 eODU, 2x1 iODU");
        assertEquals(8, pceONspdrSA1.getListOfNep().stream().filter(bpn -> bpn.getName().entrySet().stream()
            .findFirst().orElseThrow().getValue().getValue().contains("eODU")).collect(Collectors.toList()).size(),
            "SPDR-SA1 has 4 eODU NEP/CEPs available");
        assertEquals(2, pceONspdrSA1.getListOfNep().stream().filter(bpn -> bpn.getName().entrySet().stream()
            .findFirst().orElseThrow().getValue().getValue().contains("iODU")).collect(Collectors.toList()).size(),
            "SPDR-SA1 has 2x1 iODU NEP/CEP (Network1)");
        assertEquals(1, pceONspdrSA1.getUsableXpdrClientTps().stream().filter(bpn -> bpn.getName().entrySet().stream()
            .findFirst().orElseThrow().getValue().getValue().contains("DSR")).collect(Collectors.toList()).size(),
            "SPDR-SA1 has 1 DSR NEPs which corresponds to aPortId");
        assertTrue(pceONspdrSA1.getSlotWidthGranularity() == null,
            "An Otn Node returns null slotWidth Granularity");
        assertTrue(pceONspdrSA1.getCentralFreqGranularity() == null,
            "An Otn Node returns null Central Frequency Granularity");
        assertTrue(pceONspdrSA1.getBitSetData() == null, "Spectrum is not defined at OTN level");
    }

    @Test
    void testvalidateAZxponderForSPDRA1X3NoPortSpecified() {
        // SPDR-XPDR1 already supports One wavelength, One ODU4, and one DSR/ETh service
        LOG.info("Entering Test6 ");
        serviceType = "1GE";
        anodeId = spdrSA1xpdr3Id;
        znodeId = spdrSC1xpdr3Id;
        try {
            generalSetUp();
        } catch (ExecutionException e) {
            LOG.error("Unable to get node from mdsal: ", e);
        }
        initializeAll();
        PceTapiOtnNode pceONspdrSA1 = tapiONspdrAx3.getXpdrOtnNode();

        assertTrue(pceONspdrSA1.isValid(),
            "SPDR-SA1 Node shall be a valid OTN node");
        assertTrue(pceONspdrSA1.getNodeId().getValue().toString().equals("4582e51f-2b2d-3b70-b374-86c463062710"),
            "SPDR-SA1 Node shall have SPDR-SA1-XPDR1+XPONDER NodeId");
        assertTrue(pceONspdrSA1.getSupNetworkNodeId().equals("4582e51f-2b2d-3b70-b374-86c463062710"),
            "SPDR-SA1 Node shall have SPDR-SA1-XPDR1 Uuid as supporting Network NodeId");
        assertTrue(pceONspdrSA1.getSupClliNodeId().equals("4582e51f-2b2d-3b70-b374-86c463062710"),
            "SPDR-SA1 Node shall have SPDR-SA1-XPDR1 Uuid as supporting CLLI NodeId");
        assertTrue(pceONspdrSA1.getORNodeType().equals(OpenroadmNodeType.XPONDER),
            "SPDR-SA1 Node shall be of Xponder Type");
        assertTrue(pceONspdrSA1.getPceNodeType().equals("otn"),
            "SPDR-SA1 Node shall be of PceNodeType otn");
        assertTrue(pceONspdrSA1.getAdminState().equals(AdministrativeState.UNLOCKED),
            "SPDR-SA1 Node AdminState shall be UNLOCKED");
        assertTrue(pceONspdrSA1.getAdminStates() == null,
            "SPDR-SA1 Node AdminStates shall be null");
        assertTrue(pceONspdrSA1.getOperationalState().equals(OperationalState.ENABLED),
            "SPDR-SA1 Node Operational State shall be ENABLED");
        assertTrue(pceONspdrSA1.getState() == null,
            "SPDR-SA1 Node State shall be null");
        LOG.info("TEST OTN Line 545 SPDR-SA1 has the following NEPS in list of NEp {} ",
            pceONspdrSA1.getTotalListOfNep().stream().map(BasePceNep::getName).collect(Collectors.toList()));
        // Check NEP of the current Node
        assertEquals(15, pceONspdrSA1.getTotalListOfNep().size(),
            "SPDR-SA1 has 15 NEPs prefiltered NEP");
        assertEquals(8, pceONspdrSA1.getTotalListOfNep().stream().filter(bpn -> bpn.getName().entrySet().stream()
            .findFirst().orElseThrow().getValue().getValue().contains("eODU")).collect(Collectors.toList()).size(),
            "SPDR-SA1 has 4x2 eODU NEP/CEPs available");
        assertEquals(4, pceONspdrSA1.getTotalListOfNep().stream().filter(bpn -> bpn.getName().entrySet().stream()
            .findFirst().orElseThrow().getValue().getValue().contains("DSR")).collect(Collectors.toList()).size(),
            "SPDR-SA1 has 4 DSR NEPs which corresponds to aPortId");
        assertEquals(2, pceONspdrSA1.getTotalListOfNep().stream().filter(bpn -> bpn.getName().entrySet().stream()
            .findFirst().orElseThrow().getValue().getValue().contains("iODU")).collect(Collectors.toList()).size(),
            "SPDR-SA1 has 1 iODU NEPs + 1 iODU CEP (Network1)");
        assertEquals(1, pceONspdrSA1.getTotalListOfNep().stream().filter(bpn -> bpn.getName().entrySet().stream()
            .findFirst().orElseThrow().getValue().getValue().contains("OTS")).collect(Collectors.toList()).size(),
            "SPDR-SA1 has 1 OTS NEPs");
        // Check Valid (of Interest) NEP of the current Node
        LOG.info("TEST OTN Line 563 SPDR-SA1 has the following NEPS in list of NEp {} ",
            pceONspdrSA1.getListOfNep().stream().map(BasePceNep::getName).collect(Collectors.toList()));
        assertEquals(10, pceONspdrSA1.getListOfNep().size(),
            "SPDR-SA1 has 10 selectable NEP/CEP at ODU level after Nep have been pruned : 4x2 eODU, 2x1 iODU");
        assertEquals(4, pceONspdrSA1.getUsableXpdrClientTps().stream().filter(bpn -> bpn.getName().entrySet().stream()
            .findFirst().orElseThrow().getValue().getValue().contains("DSR")).collect(Collectors.toList()).size(),
            "4 DSR were kept, no port being specified");
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
            tapiONspdrAx3 = getTapiOpticalNodeFromId(spdrSA1xpdr3Id);
            tapiONspdrCx3 = getTapiOpticalNodeFromId(spdrSC1xpdr3Id);
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
        tapiONspdrAx3.initialize();
        tapiONspdrCx3.initialize();
    }

}
