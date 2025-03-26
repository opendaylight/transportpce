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
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.common.util.concurrent.ListenableFuture;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
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
import org.opendaylight.transportpce.pce.networkanalyzer.TapiOpticalNode.DirectionType;
import org.opendaylight.transportpce.pce.node.mccapabilities.McCapability;
import org.opendaylight.transportpce.pce.node.mccapabilities.NodeMcCapability;
import org.opendaylight.transportpce.test.AbstractTest;
import org.opendaylight.transportpce.test.converter.XMLDataObjectConverter;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.node.types.rev210528.NodeTypes;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.types.rev230526.OpenroadmTpType;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.format.rev191129.ServiceFormat;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.AdministrativeState;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.Context;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.OperationalState;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.Uuid;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.global._class.Name;
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

//@TestInstance(Lifecycle.PER_CLASS)
public class TapiOpticalNodeTest extends AbstractTest {

    private static final Logger LOG = LoggerFactory.getLogger(TapiOpticalNodeTest.class);
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
    private TapiOpticalNode tapiONroadmA;
    private TapiOpticalNode tapiONroadmC;
    private TapiOpticalNode tapiONspdrAx2;
    private TapiOpticalNode tapiONspdrCx2;
    private TapiOpticalNode tapiONspdrAx3;
    private TapiOpticalNode tapiONspdrAx1;
    private TapiOpticalNode tapiONspdrCx3;
    private TapiOpticalNode tapiONspdrCx1;
    private TapiOpticalNode tapiONxpdrCx1;
    private TapiOpticalNode tapiONxpdrAx1;

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
    void isValidTest() {
        this.anodeId = spdrSA1xpdr1Id;
        this.znodeId = spdrSC1xpdr1Id;
        //SPDR-SA1-XPDR1+DSR+XPDR1-CLIENT2
        this.aportId = new Uuid("82998ece-51cd-3c07-8b68-59d5ad5ff39e");
        //SPDR-SC1-XPDR1+DSR+XPDR1-CLIENT2"
        this.zportId = new Uuid("b877533a-99ce-3128-bfb2-dc7e0a2122dd");
        try {
            generalSetUp();
        } catch (ExecutionException e) {
            LOG.error("Unable to get node from mdsal: ", e);
        }
        // If serviceType not defined, none of the node shall be valid
        assertFalse(tapiONroadmA.isValid());
        assertFalse(tapiONroadmC.isValid());
        assertFalse(tapiONspdrAx2.isValid());
        assertFalse(tapiONspdrCx2.isValid());
        assertFalse(tapiONspdrAx1.isValid());
        assertFalse(tapiONspdrCx1.isValid());
        assertFalse(tapiONspdrAx3.isValid());
        assertFalse(tapiONspdrCx3.isValid());

        // If serviceType is defined, all nodes shall be valid until they have been initialized
        this.serviceType = "10GE";
        try {
            generalSetUp();
        } catch (ExecutionException e) {
            LOG.error("Unable to get node from mdsal: ", e);
        }
        assertTrue(tapiONroadmA.isValid());
        assertTrue(tapiONroadmC.isValid());
        assertTrue(tapiONspdrAx2.isValid());
        assertTrue(tapiONspdrCx2.isValid());
        assertTrue(tapiONspdrAx1.isValid());
        assertTrue(tapiONspdrCx1.isValid());
        assertTrue(tapiONspdrAx3.isValid());
        assertTrue(tapiONspdrCx3.isValid());
        LOG.info("TESTPERFORMED:1");
    }

    @Test
    void testQualifyNode() {
        //Testing private method qualifyNode()
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

        initializeAll();
        assertTrue(tapiONroadmA.getCommonNodeType().equals(NodeTypes.Rdm));
        assertTrue(tapiONroadmC.getCommonNodeType().equals(NodeTypes.Rdm));
        assertTrue(tapiONspdrAx2.getCommonNodeType().equals(NodeTypes.Xpdr));
        assertTrue(tapiONspdrCx2.getCommonNodeType().equals(NodeTypes.Xpdr));
        assertTrue(tapiONspdrAx1.getCommonNodeType().equals(NodeTypes.Xpdr));
        assertTrue(tapiONspdrCx1.getCommonNodeType().equals(NodeTypes.Xpdr));
        assertTrue(tapiONspdrAx3.getCommonNodeType().equals(NodeTypes.Xpdr));
        assertTrue(tapiONspdrCx3.getCommonNodeType().equals(NodeTypes.Xpdr));
        LOG.info("TESTPERFORMED:2");
    }

    @Test
    void testInitializeAndValidateAZxponders() {
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
        initializeAll();
        // If serviceType is defined, after initialization, all ROADM nodes shall be valid but only A and Z end TSP
        // shall be valid
        assertTrue(tapiONroadmA.isValid());
        assertTrue(tapiONroadmC.isValid());
        assertFalse(tapiONspdrAx2.isValid());
        assertFalse(tapiONspdrCx2.isValid());
        assertTrue(tapiONspdrAx1.isValid());
        assertTrue(tapiONspdrCx1.isValid());
        assertFalse(tapiONspdrAx3.isValid());
        assertFalse(tapiONspdrCx3.isValid());
        LOG.info("TESTPERFORMED:3");
    }

    @Test
    void testInitRoadmTps() {
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
        initializeAll();
        //Testing private method initRoadmTps
        List<BasePceNep> rdmAdegOTSNep = new ArrayList<>(tapiONroadmA.getDegOtsNep());
        //Streaming list on tp type checks everything is there
        assertEquals(4, rdmAdegOTSNep.size(), "ROADM A shall contain 2 degree x (1 OTS Nep + 1 virtual Nep (CTP)");
        assertEquals(4, rdmAdegOTSNep.stream()
            .filter(bpn -> bpn.getAdminState() != null && bpn.getAdminState().equals(AdministrativeState.UNLOCKED))
            .collect(Collectors.toList()).size(),
            "All ROADM A Degrees OTS Neps of TTP shall be unlocked");
        assertEquals(4, rdmAdegOTSNep.stream()
            .filter(bpn -> bpn.getOperationalState() != null
                && bpn.getOperationalState().equals(OperationalState.ENABLED))
            .collect(Collectors.toList()).size(),
            "All ROADM A Degrees OTS Neps of TTP shall be enabled");
        //Testing private method calculateDirection()
        assertEquals(4, rdmAdegOTSNep.stream()
            .filter(bpn -> bpn.getDirection().equals(DirectionType.BIDIRECTIONAL))
            .collect(Collectors.toList()).size(),
            "All ROADM A Degrees OTS Neps of TTP shall be bidirectional");
        var freqBitSet = new BitSet(GridConstant.EFFECTIVE_BITS);
        freqBitSet.set(0, GridConstant.EFFECTIVE_BITS, true);
        assertEquals(4, rdmAdegOTSNep.stream()
            .filter(bpn -> bpn.getFrequenciesBitSet() != null && bpn.getFrequenciesBitSet().equals(freqBitSet))
            .collect(Collectors.toList()).size(),
            "All ROADM A Degrees OTS Neps shall have the full spectrum available");
        //Testing private method buildDefaultVirtualCtps() and private method buildVirtualCpsAndCtps()
        assertEquals(2, rdmAdegOTSNep.stream()
                .filter(bpn -> bpn.getTpType().equals(OpenroadmTpType.DEGREETXRXTTP))
                .collect(Collectors.toList()).size(),
                "ROADM A shall contain 2 (DEG1&DEG2) OTS Neps of TTP type");
        assertEquals(2, rdmAdegOTSNep.stream()
            .filter(bpn -> bpn.getTpType().equals(OpenroadmTpType.DEGREETXRXCTP))
            .collect(Collectors.toList()).size(),
            "ROADM A shall contain 2 (DEG1&DEG2) OTS Virtual Neps of CTP type");
        List<BasePceNep> rdmAdegOMSNep = new ArrayList<>(tapiONroadmA.getDegOmsNep());
        LOG.info("TONTESTLine268 degOMSNEP contains following NEP {}", rdmAdegOMSNep);
        assertEquals(2, rdmAdegOMSNep.size(), "ROADM A shall contain 2 degree x (1 OMS Nep)");
        List<Uuid> clientOtsNep = rdmAdegOTSNep.stream()
            .filter(bpn -> bpn.getTpType().equals(OpenroadmTpType.DEGREETXRXTTP))
            .map(BasePceNep::getClientNep)
            .collect(Collectors.toList());
        //Testing private method addCpCtpOutgoingLink()
        List<Map<Uuid, Name>> connectedLink =
            rdmAdegOTSNep.stream()
                .filter(bpn -> bpn.getTpType().equals(OpenroadmTpType.DEGREETXRXCTP))
                .map(BasePceNep::getConnectedLink)
                .collect(Collectors.toList());
        int linkSize = 0;
        for (Map<Uuid, Name> clink : connectedLink) {
            linkSize = linkSize + clink.size();
        }
        assertEquals(6, linkSize,
            "ROADM A shall contain 2(DEG1&DEG2)x 3(to CP SRG1, CP SRG2, and other Degree) = 6 declared outgoing links");
        assertEquals(2, rdmAdegOMSNep.stream()
            .filter(bpn -> clientOtsNep.contains(bpn.getNepCepUuid()))
            .collect(Collectors.toList()).size(),
            "All ROADM A Degrees OMS Neps of TTP shall be referenced as client NEP of OTS basePceNeps");
        List<Uuid> omsParentNep = rdmAdegOMSNep.stream()
            .map(BasePceNep::getParentNep)
            .collect(Collectors.toList());
        LOG.debug("TONTESTLine293 omsParentNep contains following NEP {}", omsParentNep);
        assertEquals(2, rdmAdegOTSNep.stream()
            .filter(bpn -> omsParentNep.contains(bpn.getNepCepUuid()))
            .collect(Collectors.toList()).size(),
            "The 2 ROADM A Degrees OTS Neps of TTP shall be referenced as parent NEP of OMS basePceNeps");
        List<BasePceNep> rdmAsrgOTSNep = new ArrayList<>(tapiONroadmA.getSrgOtsNep());
        assertEquals(10, rdmAsrgOTSNep.size(), "ROADM A shall contain 2 SRG(1&3)x(4 OTS Nep (PPs)+ 1 virtual Nep (CP)");
        assertEquals(10, rdmAsrgOTSNep.stream()
            .filter(bpn -> bpn.getAdminState().equals(AdministrativeState.UNLOCKED))
            .collect(Collectors.toList()).size(),
            "All ROADM A SRGs OTS Neps of TTP shall be unlocked");
        assertEquals(10, rdmAsrgOTSNep.stream()
            .filter(bpn -> bpn.getOperationalState().equals(OperationalState.ENABLED))
            .collect(Collectors.toList()).size(),
            "All ROADM A SRG's OTS Neps of TTP shall be enabled");
        //Testing private method calculateDirection()
        assertEquals(10, rdmAsrgOTSNep.stream()
            .filter(bpn -> bpn.getDirection().equals(DirectionType.BIDIRECTIONAL))
            .collect(Collectors.toList()).size(),
            "All ROADM A SRG's OTS Neps of TTP shall be bidirectional");
        assertEquals(10, rdmAsrgOTSNep.stream()
            .filter(bpn -> bpn.getFrequenciesBitSet().equals(freqBitSet))
            .collect(Collectors.toList()).size(),
            "All ROADM A SRG's OTS Neps shall have the full spectrum available");
        //Testing private method buildDefaultVirtualCtps() and private method buildVirtualCpsAndCtps()
        assertEquals(2, rdmAsrgOTSNep.stream()
            .filter(bpn -> bpn.getTpType().equals(OpenroadmTpType.SRGTXRXCP))
            .collect(Collectors.toList()).size(),
            "ROADM A shall contain 2 (SRG1&SRG3) OTS Virtual Neps of CP type");
        assertEquals(8, rdmAsrgOTSNep.stream()
            .filter(bpn -> bpn.getTpType().equals(OpenroadmTpType.SRGTXRXPP))
            .collect(Collectors.toList()).size(),
            "ROADM A shall contain 2 (SRG1&SRG3)x 4 OTS Neps of PP type");
        //Testing private method addCpCtpOutgoingLink()
        connectedLink = rdmAsrgOTSNep.stream()
            .filter(bpn -> bpn.getTpType().equals(OpenroadmTpType.SRGTXRXCP))
            .map(BasePceNep::getConnectedLink)
            .collect(Collectors.toList());
        linkSize = 0;
        for (Map<Uuid, Name> clink : connectedLink) {
            linkSize = linkSize + clink.size();
        }
        assertEquals(4, linkSize,
            "ROADM A shall contain 2 (SRG1&SRG3)x 2 declared outgoing links to Degrees");
        //Testing private method buildBitsetFromSpectrum()
        for (BasePceNep bpn : rdmAsrgOTSNep) {
            assertTrue(bpn.getFrequenciesBitSet().equals(freqBitSet), "SRG OTSNep spectrum shall be fully available");
        }
        for (BasePceNep bpn : rdmAdegOTSNep) {
            assertTrue(bpn.getFrequenciesBitSet().equals(freqBitSet), "DEG OTSNep spectrum shall be fully available");
        }
        LOG.info("TESTPERFORMED:4");

    }

    @Test
    void testinitTapiXndrTpsAZxpdr1() {
        LOG.info("TONTESTLine350 ENtering test testInitTapiXndrTpsAZxpdr1");
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
        initializeXpdrs();
        List<BasePceNep> spdrAnwOTSNep = new ArrayList<>(tapiONspdrAx1.getnetOtsNep());
        LOG.info("TONtestLine385, spdrAnwOTSNep = {}",
            spdrAnwOTSNep.stream().map(bpn -> bpn.getName()).collect(Collectors.toList()));
        assertEquals(1, spdrAnwOTSNep.size(),
            "SPDRAX1 shall have 1 NW port available  : a wavelength is provisionned on the network port, but the port"
            + "is considered as valid as it is part of the intermediate path");
        List<BasePceNep> spdrAclientDsrNep = new ArrayList<>(tapiONspdrAx1.getClientDsrNep());
        assertEquals(1, spdrAclientDsrNep.size(),
            "SPDRAX1 shall have 4 Client port equiped, but only one corresponds to a/z portId");
        for (BasePceNep clientbpnNep : spdrAclientDsrNep) {
            testTransponderBpn(clientbpnNep, false);
        }
        List<BasePceNep> spdrAoduNep = new ArrayList<>(tapiONspdrAx1.getOduNep());
        assertEquals(4, spdrAoduNep.size(),
            "SPDRAX1 shall have 4 ODU port : 3 eODU (1 being used) + 1 iODU port");
        for (BasePceNep oduBpnNep : spdrAoduNep) {
            testTransponderBpn(oduBpnNep, false);
        }
        List<BasePceNep> spdrAotuNep = new ArrayList<>(tapiONspdrAx1.getOtuNep());
        assertEquals(0, spdrAotuNep.size(),
            "SPDRAX1 shall have 0 iOTU port available since an iODU connection is already provisioned over the iOTU");
        LOG.info("TESTPERFORMED:5");
    }

    @Test
    void testinitTapiXndrTpsAZxpdr1UsingSIP() {
        LOG.info("TONTESTLine350 ENtering test testInitTapiXndrTpsAZxpdr1");
        this.anodeId = spdrSA1xpdr1Id;
        this.znodeId = spdrSC1xpdr1Id;
        //SIP SPDR-SA1-XPDR1+DSR+XPDR1-CLIENT2
        this.aportId = new Uuid("8aae12fe-d1e8-3c2a-adba-285f837f6714");
        //SIP SPDR-SC1-XPDR1+DSR+XPDR1-CLIENT2"
        this.zportId = new Uuid("750719d1-626d-34de-8400-801171f6cd1d");
        this.serviceType = "10GE";
        try {
            generalSetUp();
        } catch (ExecutionException e) {
            LOG.error("Unable to get node from mdsal: ", e);
        }
        initializeXpdrs();
        List<BasePceNep> spdrAnwOTSNep = new ArrayList<>(tapiONspdrAx1.getnetOtsNep());
        LOG.info("TONtestLine385, spdrAnwOTSNep = {}",
            spdrAnwOTSNep.stream().map(bpn -> bpn.getName()).collect(Collectors.toList()));
        assertEquals(1, spdrAnwOTSNep.size(),
            "SPDRAX1 shall have 1 NW port available  : a wavelength is provisionned on the network port, but the port"
            + "is considered as valid as it is part of the intermediate path");
        List<BasePceNep> spdrAclientDsrNep = new ArrayList<>(tapiONspdrAx1.getClientDsrNep());
        assertEquals(1, spdrAclientDsrNep.size(),
            "SPDRAX1 shall have 4 Client port equiped, but only one corresponds to a/z portId");
        for (BasePceNep clientbpnNep : spdrAclientDsrNep) {
            testTransponderBpn(clientbpnNep, false);
        }
        List<BasePceNep> spdrAoduNep = new ArrayList<>(tapiONspdrAx1.getOduNep());
        assertEquals(4, spdrAoduNep.size(),
            "SPDRAX1 shall have 4 ODU port : 3 eODU (1 being used) + 1 iODU port");
        for (BasePceNep oduBpnNep : spdrAoduNep) {
            testTransponderBpn(oduBpnNep, false);
        }
        List<BasePceNep> spdrAotuNep = new ArrayList<>(tapiONspdrAx1.getOtuNep());
        assertEquals(0, spdrAotuNep.size(),
            "SPDRAX1 shall have 0 iOTU port available since an iODU connection is already provisioned over the iOTU");
        LOG.info("TESTPERFORMED:6");
    }

    @Test
    void testinitTapiXndrTpsAZxpdr1UsedPort() {
        LOG.info("TONTESTLine417 ENtering test testInitTapiXndrTpsAZxpdr1UsedPort");
        this.anodeId = spdrSA1xpdr1Id;
        this.znodeId = spdrSC1xpdr1Id;
        //SPDR-SA1-XPDR1+DSR+XPDR1-CLIENT1
        this.aportId = new Uuid("c6cd334c-51a1-3995-bed3-5cf2b7445c04");
        //SPDR-SC1-XPDR1+DSR+XPDR1-CLIENT1"
        this.zportId = new Uuid("50b7521a-4a38-358f-9846-45c55813416a");
        this.serviceType = "10GE";
        try {
            generalSetUp();
        } catch (ExecutionException e) {
            LOG.error("Unable to get node from mdsal: ", e);
        }
        initializeXpdrs();

        List<BasePceNep> spdrAclientDsrNep = new ArrayList<>(tapiONspdrAx1.getClientDsrNep());
        assertEquals(0, spdrAclientDsrNep.size(),
            "SPDRAX1 shall have 4 Client port equiped, but 0 available since a/z portId are already used");
        LOG.info("TESTPERFORMED:7");
    }

    @Test
    void testinitTapiXndrTpsAZxpdr2WdmServiceUsedPort() {
        // SPDR-SA1-XPDR2+OTS+XPDR2-NETWORK1 ff69716d-325d-35fc-baa9-0c4bcd992bbd
        // SPDR-SC1-XPDR2+OTS+XPDR2-NETWORK1 8557ddb5-4ee9-33e0-ba0f-adf713868625
        this.anodeId = spdrSA1xpdr2Id;
        this.znodeId = spdrSC1xpdr2Id;
        this.serviceType = "OTU4";
        this.aportId = new Uuid("ff69716d-325d-35fc-baa9-0c4bcd992bbd");
        this.zportId = new Uuid("8557ddb5-4ee9-33e0-ba0f-adf713868625");
        try {
            xpdrSetUp();
        } catch (ExecutionException e) {
            LOG.error("Unable to get node from mdsal: ", e);
        }
        initializeXpdrs();
        // If serviceType is defined, after initialization, only A and Z end TSP shall be valid
        assertTrue(tapiONspdrAx2.isValid());
        assertTrue(tapiONspdrCx2.isValid());
        assertFalse(tapiONspdrAx1.isValid());
        assertFalse(tapiONspdrCx1.isValid());
        assertFalse(tapiONspdrAx3.isValid());
        assertFalse(tapiONspdrCx3.isValid());
        List<BasePceNep> spdrAnwOTSNep = new ArrayList<>(tapiONspdrAx2.getnetOtsNep());
        assertEquals(0, spdrAnwOTSNep.size(),
            "SPDRAX2 shall have 0 NW ports and Neps (OTS) since N1 network port is already used");
        List<BasePceNep> spdrAclientDsrNep = new ArrayList<>(tapiONspdrAx1.getClientDsrNep());
        assertEquals(0, spdrAclientDsrNep.size(),
            "SPDRAX1 shall have 0 Client port eligible since it does not correspond to NodeId");
        spdrAclientDsrNep = new ArrayList<>(tapiONspdrAx2.getClientDsrNep());
        assertEquals(0, spdrAclientDsrNep.size(),
            "SPDRAX2 shall have 0 Client port eligible since the port is used");
        spdrAclientDsrNep = new ArrayList<>(tapiONspdrAx3.getClientDsrNep());
        assertEquals(0, spdrAclientDsrNep.size(),
            "SPDRAX3 shall have 0 Client port eligible since it does not correspond to NodeId");
        LOG.info("TESTPERFORMED:8");
    }

    @Test
    void testinitTapiXndrTpsAZxpdr2WdmServiceAvailablePort() {
        // SPDR-SA1-XPDR2+OTS+XPDR2-NETWORK2 4e4bf439-b457-3260-865c-db716e2647c2
        // SPDR-SC1-XPDR2+OTS+XPDR2-NETWORK2 ae235ae1-f17f-3619-aea9-e6b2a9e850c3
        this.anodeId = spdrSA1xpdr2Id;
        this.znodeId = spdrSC1xpdr2Id;
        this.serviceType = "OTU4";
        this.aportId = new Uuid("4e4bf439-b457-3260-865c-db716e2647c2");
        this.zportId = new Uuid("ae235ae1-f17f-3619-aea9-e6b2a9e850c3");
        try {
            xpdrSetUp();
        } catch (ExecutionException e) {
            LOG.error("Unable to get node from mdsal: ", e);
        }
        initializeXpdrs();
        // If serviceType is defined, after initialization, only A and Z end TSP shall be valid
        assertTrue(tapiONspdrAx2.isValid());
        assertTrue(tapiONspdrCx2.isValid());
        assertFalse(tapiONspdrAx1.isValid());
        assertFalse(tapiONspdrCx1.isValid());
        assertFalse(tapiONspdrAx3.isValid());
        assertFalse(tapiONspdrCx3.isValid());
        List<BasePceNep> spdrAnwOTSNep = new ArrayList<>(tapiONspdrAx2.getnetOtsNep());
        assertEquals(1, spdrAnwOTSNep.size(),
            "SPDRAX2 shall have 1 NW ports and Neps (OTS) since N2 network port is not used");
        List<BasePceNep> spdrAclientDsrNep = new ArrayList<>(tapiONspdrAx1.getClientDsrNep());
        assertEquals(0, spdrAclientDsrNep.size(),
            "SPDRAX1 shall have 0 Client port eligible since it does not correspond to NodeId");
        spdrAclientDsrNep = new ArrayList<>(tapiONspdrAx2.getClientDsrNep());
        assertEquals(8, spdrAclientDsrNep.size(),
            "SPDRAX2 shall have 8 ports equiped (OTS +OTU), and in visibility of the Network port");
        for (BasePceNep clientbpnNep : spdrAclientDsrNep) {
            testTransponderBpn(clientbpnNep, false);
        }
        spdrAclientDsrNep = new ArrayList<>(tapiONspdrAx3.getClientDsrNep());
        assertEquals(0, spdrAclientDsrNep.size(),
            "SPDRAX3 shall have 0 Client port eligible since it does not correspond to NodeId");
        LOG.info("TESTPERFORMED:9");
    }

    @Test
    void testinitTapiXndrTpsAZxpdr2WdmServiceAvailablePortUisngSIP() {
        // SIP SPDR-SA1-XPDR2+PHOTONIC_MEDIA_OTS+XPDR2-NETWORK2 e48d5774-fc91-30ba-9e89-01874ecd85db
        // SIP SPDR-SC1-XPDR2+PHOTONIC_MEDIA_OTS+XPDR2-NETWORK2 58109957-509d-3e06-ba06-c6c58e491698
        this.anodeId = spdrSA1xpdr2Id;
        this.znodeId = spdrSC1xpdr2Id;
        this.serviceType = "OTU4";
        this.aportId = new Uuid("e48d5774-fc91-30ba-9e89-01874ecd85db");
        this.zportId = new Uuid("58109957-509d-3e06-ba06-c6c58e491698");
        try {
            xpdrSetUp();
        } catch (ExecutionException e) {
            LOG.error("Unable to get node from mdsal: ", e);
        }
        initializeXpdrs();
        // If serviceType is defined, after initialization, only A and Z end TSP shall be valid
        assertTrue(tapiONspdrAx2.isValid());
        assertTrue(tapiONspdrCx2.isValid());
        assertFalse(tapiONspdrAx1.isValid());
        assertFalse(tapiONspdrCx1.isValid());
        assertFalse(tapiONspdrAx3.isValid());
        assertFalse(tapiONspdrCx3.isValid());
        List<BasePceNep> spdrAnwOTSNep = new ArrayList<>(tapiONspdrAx2.getnetOtsNep());
        assertEquals(1, spdrAnwOTSNep.size(),
            "SPDRAX2 shall have 1 NW ports and Neps (OTS) since N2 network port is not used");
        List<BasePceNep> spdrAclientDsrNep = new ArrayList<>(tapiONspdrAx2.getClientDsrNep());
        assertEquals(8, spdrAclientDsrNep.size(),
            "SPDRAX2 shall have 8 ports equiped (OTS +OTU), and in visibility of the Network port");
        for (BasePceNep clientbpnNep : spdrAclientDsrNep) {
            testTransponderBpn(clientbpnNep, false);
        }
        LOG.info("TESTPERFORMED:10");
    }

    @Test
    void testinitTapiXndrTpsAZxpdr2ODUServiceAvailablePort() {
        // SPDR-SA1-XPDR2+iODU+XPDR2-NETWORK1 6f4777d4-41f0-3833-b811-68b90903d834
        // SPDR-SC1-XPDR2+iODU+XPDR2-NETWORK1 8b82dac3-646c-301d-b455-b7a4802774f7
        this.anodeId = spdrSA1xpdr2Id;
        this.znodeId = spdrSC1xpdr2Id;
        this.serviceType = "ODU4";
        this.aportId = new Uuid("6f4777d4-41f0-3833-b811-68b90903d834");
        this.zportId = new Uuid("8b82dac3-646c-301d-b455-b7a4802774f7");
        try {
            xpdrSetUp();
        } catch (ExecutionException e) {
            LOG.error("Unable to get node from mdsal: ", e);
        }
        initializeXpdrs();
        // If serviceType is defined, after initialization, only A and Z end TSP shall be valid
        assertTrue(tapiONspdrAx2.isValid());
        assertTrue(tapiONspdrCx2.isValid());
        assertFalse(tapiONspdrAx1.isValid());
        assertFalse(tapiONspdrCx1.isValid());
        assertFalse(tapiONspdrAx3.isValid());
        assertFalse(tapiONspdrCx3.isValid());
        List<BasePceNep> spdrAnwOTSNep = new ArrayList<>(tapiONspdrAx2.getnetOtsNep());
        assertEquals(1, spdrAnwOTSNep.size(),
            "SPDRAX2 shall have 1 NW port available  : a wavelength is provisionned on the network port, but the port"
            + "is considered as valid as it is part of the intermediate path");
        assertFalse(tapiONspdrCx3.isValid());
        List<BasePceNep> spdrAoduNep = new ArrayList<>(tapiONspdrAx2.getOduNep());
        assertEquals(4, spdrAoduNep.size(),
            "SPDRAX2 shall have 4 ODU ports and Neps (4 iODU network ports) available");
        for (BasePceNep bpnNep : spdrAoduNep) {
            testTransponderBpn(bpnNep, false);
        }
        List<BasePceNep> spdrAotuNep = new ArrayList<>(tapiONspdrAx2.getOduNep());
        assertEquals(4, spdrAotuNep.size(),
            "SPDRAX2 shall have 4 iOTU port/Nep provisioned");
        for (BasePceNep bpnNep : spdrAotuNep) {
            testTransponderBpn(bpnNep, false);
        }
        List<BasePceNep> spdrAclientDsrNep = new ArrayList<>(tapiONspdrAx1.getClientDsrNep());
        assertEquals(0, spdrAclientDsrNep.size(),
            "SPDRAX1 shall have 0 Client ports available eligible since it does not correspond to NodeId");
        spdrAclientDsrNep = new ArrayList<>(tapiONspdrAx2.getClientDsrNep());
        assertEquals(1, spdrAclientDsrNep.size(),
            "SPDRAX2 shall have 1 Client port eligible for an ODU service as iODU are included in spdrAclientDsrNep"
            + "(When client port is on network side, it is handled in the client list)");
        LOG.info("TESTPERFORMED:11");
    }

    @Test
    void testinitTapiXndrTpsAZxpdr2ODUServiceAvailablePortUsingSIP() {
        // SIP SPDR-SA1-XPDR2+iODU+XPDR2-NETWORK1 86ed195a-c926-334a-8a64-9418ca096579
        // SIP SPDR-SC1-XPDR2+iODU+XPDR2-NETWORK1 97598b05-b5a2-383b-a663-66290ba5d3e7
        this.anodeId = spdrSA1xpdr2Id;
        this.znodeId = spdrSC1xpdr2Id;
        this.serviceType = "ODU4";
        this.aportId = new Uuid("86ed195a-c926-334a-8a64-9418ca096579CCCC");
        this.zportId = new Uuid("97598b05-b5a2-383b-a663-66290ba5d3e7");
        try {
            xpdrSetUp();
        } catch (ExecutionException e) {
            LOG.error("Unable to get node from mdsal: ", e);
        }
        initializeXpdrs();
        // If serviceType is defined, after initialization, only A and Z end TSP shall be valid
        assertTrue(tapiONspdrAx2.isValid());
        assertTrue(tapiONspdrCx2.isValid());
        assertFalse(tapiONspdrAx1.isValid());
        assertFalse(tapiONspdrCx1.isValid());
        assertFalse(tapiONspdrAx3.isValid());
        assertFalse(tapiONspdrCx3.isValid());
        List<BasePceNep> spdrAnwOTSNep = new ArrayList<>(tapiONspdrAx2.getnetOtsNep());
        assertEquals(0, spdrAnwOTSNep.size(),
            "SPDRAX2 shall have 0 OTS NW ports and Neps available for an ODU service");
        assertFalse(tapiONspdrCx3.isValid());
        List<BasePceNep> spdrAoduNep = new ArrayList<>(tapiONspdrAx2.getOduNep());
        assertEquals(4, spdrAoduNep.size(),
            "SPDRAX2 shall have 4 ODU ports and Neps (4 iODU network ports) available");
        for (BasePceNep clientbpnNep : spdrAoduNep) {
            testTransponderBpn(clientbpnNep, false);
        }
        List<BasePceNep> spdrAotuNep = new ArrayList<>(tapiONspdrAx2.getOduNep());
        assertEquals(4, spdrAotuNep.size(),
            "SPDRAX2 shall have 4 iOTU port/Nep provisioned");
        for (BasePceNep clientbpnNep : spdrAotuNep) {
            testTransponderBpn(clientbpnNep, false);
        }
        List<BasePceNep> spdrAclientDsrNep = new ArrayList<>(tapiONspdrAx2.getClientDsrNep());
        assertEquals(0, spdrAclientDsrNep.size(),
            "SPDRAX2 shall have 0 Client port eligible for an ODU service");
        LOG.info("TESTPERFORMED:12");
    }

    @Test
    void testinitTapiXndrTpsAZxpdr2DSRServiceAvailablePort() {

        this.anodeId = spdrSA1xpdr2Id;
        this.znodeId = spdrSC1xpdr2Id;
        this.serviceType = "100GEs";
        //SPDR-SA1-XPDR2+DSR+XPDR2-CLIENT1
        this.aportId = new Uuid("935d763e-297c-332f-8edc-65496a2a607c");
        //SPDR-SC1-XPDR2+DSR+XPDR2-CLIENT1"
        this.zportId = new Uuid("eddd56d6-0aa7-3583-9b1b-40470ed2cf96");
        try {
            xpdrSetUp();
        } catch (ExecutionException e) {
            LOG.error("Unable to get node from mdsal: ", e);
        }
        initializeXpdrs();
        // If serviceType is defined, after initialization, only A and Z end TSP shall be valid
        assertTrue(tapiONspdrAx2.isValid());
        assertTrue(tapiONspdrCx2.isValid());
        assertFalse(tapiONspdrAx1.isValid());
        assertFalse(tapiONspdrCx1.isValid());
        assertFalse(tapiONspdrAx3.isValid());
        assertFalse(tapiONspdrCx3.isValid());
        List<BasePceNep> spdrAnwOTSNep = new ArrayList<>(tapiONspdrAx2.getnetOtsNep());
        assertEquals(1, spdrAnwOTSNep.size(),
            "SPDRAX2 shall have 1 NW port available  : a wavelength is provisionned on the network port, but the port"
            + "is considered as valid as it is part of the intermediate path");
        List<BasePceNep> spdrAclientDsrNep = new ArrayList<>(tapiONspdrAx1.getClientDsrNep());
        assertEquals(0, spdrAclientDsrNep.size(),
            "SPDRAX1 shall have 0 Client port eligible since it does not correspond to NodeId");
        spdrAclientDsrNep = new ArrayList<>(tapiONspdrAx2.getClientDsrNep());
        assertEquals(1, spdrAclientDsrNep.size(),
            "SPDRAX2 shall have 4 Client port equiped, but only one corresponds to a/z portId");
        LOG.info("TESTPERFORMED:13");
    }

    @Test
    void testinitTapiXndrTpsAZxpdr2DSRServiceAvailablePortWithSIP() {
        this.anodeId = spdrSA1xpdr2Id;
        this.znodeId = spdrSC1xpdr2Id;
        this.serviceType = "100GEs";
        //SIP SPDR-SA1-XPDR2+DSR+XPDR2-CLIENT1
        this.aportId = new Uuid("6980f38d-a3a0-31ab-85b3-b98f9537fc41");
        //SIP SPDR-SC1-XPDR2+DSR+XPDR2-CLIENT1"
        this.zportId = new Uuid("43b47382-8192-36b3-89ff-641375bbf2c7");
        try {
            xpdrSetUp();
        } catch (ExecutionException e) {
            LOG.error("Unable to get node from mdsal: ", e);
        }
        initializeXpdrs();
        // If serviceType is defined, after initialization, only A and Z end TSP shall be valid
        assertTrue(tapiONspdrAx2.isValid());
        assertTrue(tapiONspdrCx2.isValid());
        assertFalse(tapiONspdrAx1.isValid());
        assertFalse(tapiONspdrCx1.isValid());
        assertFalse(tapiONspdrAx3.isValid());
        assertFalse(tapiONspdrCx3.isValid());
        List<BasePceNep> spdrAnwOTSNep = new ArrayList<>(tapiONspdrAx2.getnetOtsNep());
        assertEquals(1, spdrAnwOTSNep.size(),
            "SPDRAX2 shall have 1 OTS NW ports and Nep available as part of intermediate path for the DSR service");
        List<BasePceNep> spdrAclientDsrNep = new ArrayList<>(tapiONspdrAx2.getClientDsrNep());
        assertEquals(1, spdrAclientDsrNep.size(),
            "SPDRAX2 shall have 4 Client port equiped, but only one corresponds to a/z portId");
        LOG.info("TESTPERFORMED:14");
    }

    @Test
    void testinitTapiXndrTpsAZxpdr2NoPortIdSpecified() {

        this.anodeId = spdrSA1xpdr2Id;
        this.znodeId = spdrSC1xpdr2Id;
        this.aportId = null;
        this.zportId = null;
        this.serviceType = "100GEs";
        try {
            xpdrSetUp();
        } catch (ExecutionException e) {
            LOG.error("Unable to get node from mdsal: ", e);
        }
        initializeXpdrs();
        // If serviceType is defined, after initialization, only A and Z end TSP shall be valid
        assertTrue(tapiONspdrAx2.isValid());
        assertTrue(tapiONspdrCx2.isValid());
        assertFalse(tapiONspdrAx1.isValid());
        assertFalse(tapiONspdrCx1.isValid());
        assertFalse(tapiONspdrAx3.isValid());
        assertFalse(tapiONspdrCx3.isValid());
        List<BasePceNep> spdrAnwOTSNep = new ArrayList<>(tapiONspdrAx2.getnetOtsNep());
        LOG.info("TONTEST Line 521 : spdrAnwOTSNep is {} ", spdrAnwOTSNep.stream().map(BasePceNep::getNepCepUuid)
            .collect(Collectors.toList()));
        assertEquals(4, spdrAnwOTSNep.size(),
            "SPDRAX2 shall have 4 NW port available : a wavelength is provisionned on the network port N1, but the port"
            + "is considered as valid as it might be part of an intermediate path. Rejection criteria will be on BW");
        for (BasePceNep nwbpnNep : spdrAnwOTSNep) {
            if (!nwbpnNep.getNepCepUuid().getValue().equals("ff69716d-325d-35fc-baa9-0c4bcd992bbd")) {
                // SPDR-SA1-XPDR2+PHOTONIC_MEDIA_OTS+XPDR2-NETWORK1 appears in the list but is not available !
                testTransponderBpn(nwbpnNep, true);
            }
        }
        List<BasePceNep> spdrAclientDsrNep = new ArrayList<>(tapiONspdrAx1.getClientDsrNep());
        assertEquals(0, spdrAclientDsrNep.size(),
            "SPDRAX1 shall have 0 Client port eligible since it does not correspond to NodeId");
        spdrAclientDsrNep = new ArrayList<>(tapiONspdrAx2.getClientDsrNep());
        assertEquals(8, spdrAclientDsrNep.size(),
            "SPDRAX2 shall have 8 Client port equiped (DSR + eODU), all validated as no a/z portId specified");
        spdrAclientDsrNep = new ArrayList<>(tapiONspdrAx3.getClientDsrNep());
        assertEquals(0, spdrAclientDsrNep.size(),
            "SPDRAX3 shall have 0 Client port eligible since it does not correspond to NodeId");
        for (BasePceNep clientbpnNep : spdrAclientDsrNep) {
            testTransponderBpn(clientbpnNep, false);
        }
        LOG.info("TESTPERFORMED:15");

    }

    @Test
    void testinitTapiXndrTpsAZxpdr3DSRServiceAvailablePort() {

        this.anodeId = spdrSA1xpdr3Id;
        this.znodeId = spdrSC1xpdr3Id;
        this.serviceType = "1GE";
        //SPDR-SA1-XPDR3+DSR+XPDR3-CLIENT2
        this.aportId = new Uuid("217abe36-25ee-337a-b919-f051faf88b21");
        //SPDR-SC1-XPDR3+DSR+XPDR3-CLIENT2
        this.zportId = new Uuid("360b276b-4beb-3c96-8650-559e9934deaa");
        try {
            xpdrSetUp();
        } catch (ExecutionException e) {
            LOG.error("Unable to get node from mdsal: ", e);
        }
        initializeXpdrs();
        // If serviceType is defined, after initialization, only A and Z end TSP shall be valid
        assertTrue(tapiONspdrAx3.isValid());
        assertTrue(tapiONspdrCx3.isValid());
        assertFalse(tapiONspdrAx1.isValid());
        assertFalse(tapiONspdrCx1.isValid());
        assertFalse(tapiONspdrAx2.isValid());
        assertFalse(tapiONspdrCx2.isValid());
        List<BasePceNep> spdrAnwOTSNep = new ArrayList<>(tapiONspdrAx3.getnetOtsNep());
        assertEquals(1, spdrAnwOTSNep.size(),
            "SPDRAX3 shall have 1 OTS NW ports and Nep available as part of intermediate path for a DSR service");
        List<BasePceNep> spdrAclientDsrNep = new ArrayList<>(tapiONspdrAx1.getClientDsrNep());
        assertEquals(0, spdrAclientDsrNep.size(),
            "SPDRAX1 shall have 0 Client port eligible since it does not correspond to NodeId");
        spdrAclientDsrNep = new ArrayList<>(tapiONspdrAx3.getClientDsrNep());
        assertEquals(1, spdrAclientDsrNep.size(),
            "SPDRAX3 shall have 4 Client port equiped, but only one corresponds to a/z portId");
        for (BasePceNep clientbpnNep : spdrAclientDsrNep) {
            testTransponderBpn(clientbpnNep, false);
        }
        LOG.info("TESTPERFORMED:16");
    }


    @Test
    void testinitTapiXndrTpsAZxpdr100GEServiceAvailablePort() {
        this.anodeId = xpdrA1xpdr1Id;
        this.znodeId = xpdrC1xpdr1Id;
        this.serviceType = "100GEt";
        //XPDR-A1-XPDR1+DSR+XPDR1-CLIENT1
        this.aportId = new Uuid("5dc0f306-763d-34a7-9419-d15e23891edd");
      //XPDR-A1-XPDR1+DSR+XPDR1-CLIENT1
        this.zportId = new Uuid("126d66e8-5028-3bb0-9769-9f3f966c6a99");
        try {
            xpdrSetUp();
        } catch (ExecutionException e) {
            LOG.error("Unable to get node from mdsal: ", e);
        }
        initializeXpdrs();
        // If serviceType is defined, after initialization, only A and Z end TSP shall be valid
        assertTrue(tapiONxpdrAx1.isValid());
        assertTrue(tapiONxpdrCx1.isValid());
        assertFalse(tapiONspdrAx1.isValid());
        assertFalse(tapiONspdrCx1.isValid());
        assertFalse(tapiONspdrAx2.isValid());
        assertFalse(tapiONspdrCx2.isValid());
        List<BasePceNep> xpdrAnwOTSNep = new ArrayList<>(tapiONxpdrAx1.getnetOtsNep());
        assertEquals(2, xpdrAnwOTSNep.size(),
            "XPDRAX1 shall have 2 OTS NW ports and Neps available for a 100GE service");
        List<BasePceNep> xpdrAclientDsrNep = new ArrayList<>(tapiONspdrAx1.getClientDsrNep());
        assertEquals(0, xpdrAclientDsrNep.size(),
            "SPDRAX1 shall have 0 Client port eligible since it does not correspond to NodeId");
        xpdrAclientDsrNep = new ArrayList<>(tapiONxpdrAx1.getClientDsrNep());
        assertEquals(1, xpdrAclientDsrNep.size(),
            "XPDRAX1 shall have 2 Client port equiped, but only one corresponds to a/z portId");
        for (BasePceNep clientbpnNep : xpdrAclientDsrNep) {
            testTransponderBpn(clientbpnNep, false);
        }
        LOG.info("TESTPERFORMED:17");
    }

    @Test
    void testRoadmInternalLinkMap() {
        this.serviceType = "10GE";
        //Testing private method buildInternalLinksMap()
        try {
            tapiONroadmA = getTapiOpticalNodeFromId(roadmAId);
        } catch (ExecutionException e) {
            LOG.error("Unable to get node from mdsal: ", e);
        }
        tapiONroadmA.initialize();
        assertEquals(5, tapiONroadmA.getPceInternalLinkMap().size(),
            "ROADM internalLinkMap shall include 5 bidirectional links (2x(SRG Cp - DEG1&2 Ctp) + 1 optical bypass ");
        LOG.info("TESTPERFORMED:18");
    }

    @Test
    void testgetXpdrOperationalMode() {
        //TODO: To be developed when profile will be populated in the topology

    }

    private void testTransponderBpn(BasePceNep bpnNep, boolean isNwNep) {

        assertTrue(bpnNep.getAdminState().equals(AdministrativeState.UNLOCKED),
            "Each SPDR NW/client port/nep admin state shall be Unlock)");
        assertTrue(bpnNep.getOperationalState().equals(OperationalState.ENABLED),
            "Each SPDR NW/client port/nep operational state shall be enabled)");
        // Client NEP present even if no service for NW port. CEP present for OTS NW port,
        //  and for higher layers, CEP present only if service provisioned
        assertTrue(bpnNep.getDirection().equals(TapiOpticalNode.DirectionType.BIDIRECTIONAL),
            "Each SPDR port/nep shall be bidirectional");
        assertTrue(bpnNep.getNodeRuleGroupUuid() != null,
            "Each SPDR port/nep shall have an associated referenced Node Rule group");
        if (isNwNep) {
            if (!bpnNep.getName().entrySet().iterator().next().getValue().getValue().contains("OTSi")) {
                assertTrue(bpnNep.getcepOtsUuid() != null,
                    "SPDR OTS NW port/nep shall have an associated Cep");
            }
            LOG.info("TONTESTLine479 FreqBitset for NEP {} is {}", bpnNep.getName(), bpnNep.getFrequenciesBitSet());
            var freqBitSet = new BitSet(GridConstant.EFFECTIVE_BITS);
            freqBitSet.set(0, GridConstant.EFFECTIVE_BITS, true);
            assertTrue(bpnNep.getFrequenciesBitSet().equals(freqBitSet),
                "Each SPDR NW port/nep spectrum shall be fully available");
        }
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
        if (node == null) {
            LOG.info("Line554, null node");
        } else {
            LOG.info("Line559, node is {}", node.getName());
            LOG.info("Line560, slotwidthGranularity is  {}", slotWidthGranularity);
        }
        return new TapiOpticalNode(serviceType, portMapping, node, version, slotWidthGranularity,
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
            tapiONspdrAx2 = getTapiOpticalNodeFromId(spdrSA1xpdr2Id);
            tapiONspdrAx3 = getTapiOpticalNodeFromId(spdrSA1xpdr3Id);
            tapiONspdrCx1 = getTapiOpticalNodeFromId(spdrSC1xpdr1Id);
            tapiONspdrCx2 = getTapiOpticalNodeFromId(spdrSC1xpdr2Id);
            tapiONspdrCx3 = getTapiOpticalNodeFromId(spdrSC1xpdr3Id);
            tapiONxpdrAx1 = getTapiOpticalNodeFromId(xpdrA1xpdr1Id);
            tapiONxpdrCx1 = getTapiOpticalNodeFromId(xpdrC1xpdr1Id);
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
        tapiONspdrAx2.initialize();
        tapiONspdrCx2.initialize();
        tapiONspdrAx1.initialize();
        tapiONspdrCx1.initialize();
        tapiONspdrAx3.initialize();
        tapiONspdrCx3.initialize();
        tapiONxpdrAx1.initialize();
        tapiONxpdrCx1.initialize();
    }
}
