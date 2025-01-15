/*
 * Copyright © 2020 Orange, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.pce.graph;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;

import com.google.common.collect.ImmutableMap;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import org.eclipse.jdt.annotation.NonNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.binding.api.MountPoint;
import org.opendaylight.mdsal.binding.api.MountPointService;
import org.opendaylight.mdsal.binding.api.WriteTransaction;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.transportpce.common.StringConstants;
import org.opendaylight.transportpce.common.device.DeviceTransactionManager;
import org.opendaylight.transportpce.common.device.DeviceTransactionManagerImpl;
import org.opendaylight.transportpce.common.mapping.PortMapping;
import org.opendaylight.transportpce.common.mapping.PortMappingImpl;
import org.opendaylight.transportpce.common.mapping.PortMappingVersion121;
import org.opendaylight.transportpce.common.mapping.PortMappingVersion221;
import org.opendaylight.transportpce.common.mapping.PortMappingVersion710;
import org.opendaylight.transportpce.common.network.NetworkTransactionImpl;
import org.opendaylight.transportpce.common.network.NetworkTransactionService;
import org.opendaylight.transportpce.pce.constraints.PceConstraints;
import org.opendaylight.transportpce.pce.frequency.interval.EntireSpectrum;
import org.opendaylight.transportpce.pce.input.ClientInput;
import org.opendaylight.transportpce.pce.networkanalyzer.PceCalculation;
import org.opendaylight.transportpce.pce.networkanalyzer.PceLink;
import org.opendaylight.transportpce.pce.networkanalyzer.PceNode;
import org.opendaylight.transportpce.pce.networkanalyzer.PceOtnNode;
import org.opendaylight.transportpce.pce.networkanalyzer.PceResult;
import org.opendaylight.transportpce.pce.utils.NodeUtils;
import org.opendaylight.transportpce.test.AbstractTest;
import org.opendaylight.transportpce.test.converter.DataObjectConverter;
import org.opendaylight.transportpce.test.converter.JSONDataObjectConverter;
import org.opendaylight.transportpce.test.converter.JsonDataConverter;
import org.opendaylight.transportpce.test.stub.MountPointServiceStub;
import org.opendaylight.transportpce.test.stub.MountPointStub;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev240205.PathComputationRequestInput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev240205.PathComputationRequestInputBuilder;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev240205.PceConstraintMode;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev240205.path.computation.request.input.ServiceAEndBuilder;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev240205.path.computation.request.input.ServiceZEndBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev230526.service.port.PortBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.state.types.rev191129.State;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.types.rev230526.OpenroadmVersionType;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.rev230526.Node1Builder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev230526.TerminationPoint1Builder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev230526.networks.network.node.termination.point.XpdrNetworkAttributesBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.types.rev230526.OpenroadmNodeType;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.types.rev230526.OpenroadmTpType;
import org.opendaylight.yang.gen.v1.http.org.openroadm.routing.constraints.rev221209.constraints.CoRoutingBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.routing.constraints.rev221209.constraints.co.routing.ServiceIdentifierListBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.routing.constraints.rev221209.routing.constraints.HardConstraintsBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.routing.constraints.rev221209.routing.constraints.SoftConstraintsBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.format.rev191129.ServiceFormat;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev230526.OperationalModeCatalog;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.service.types.rev220118.PceMetric;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.service.types.rev220118.service.endpoint.sp.RxDirectionBuilder;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.service.types.rev220118.service.endpoint.sp.TxDirectionBuilder;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.service.types.rev220118.service.handler.header.ServiceHandlerHeaderBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.NetworkId;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.Networks;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.NodeId;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.networks.Network;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.networks.NetworkKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.networks.network.Node;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.networks.network.NodeBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.networks.network.NodeKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.networks.network.node.SupportingNode;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.networks.network.node.SupportingNodeBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.networks.network.node.SupportingNodeKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.LinkId;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.TpId;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.networks.network.Link;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.networks.network.node.TerminationPoint;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.networks.network.node.TerminationPointBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.networks.network.node.TerminationPointKey;
import org.opendaylight.yangtools.binding.DataObjectIdentifier;
import org.opendaylight.yangtools.yang.common.Uint32;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PceGraphTest extends AbstractTest {
    private static final Logger LOG = LoggerFactory.getLogger(PceGraphTest.class);
    private Link link1 = null;
    private Node node = null;
    private PceLink pceLink1 = null;
    private PceGraph pceGraph = null;
    private PceConstraints pceHardConstraints = null;
    private PceResult rc = null;
    private Map<NodeId, PceNode> allPceNodes = null;
    private Map<LinkId, PceLink> allPceLinks = null;
    private static final String CATALOG_FILE = "src/test/resources/apidocCatalog12_0-OptSpecV5_1.json";
    private static final String MAPPING_FILE = "src/test/resources/topologyData/portMapping2.json";
    private static OperationalModeCatalog omCatalog;
    private static org.opendaylight.yang.gen.v1.http.org.opendaylight
            .transportpce.portmapping.rev250115.Network networkNode;
    private DataBroker dataBroker;
    private MountPoint mountPoint;
    private MountPointService mountPointService;
    private DeviceTransactionManager deviceTransactionManager;
    private PortMappingVersion710 portMappingVersion710;
    private PortMappingVersion221 portMappingVersion22;
    private PortMappingVersion121 portMappingVersion121;
    private PortMapping portMapping;
    private NetworkTransactionService netTransServ;
    private ClientInput clientInput;

    // Test of integration for PceGraph

    @BeforeEach
    void setUp() throws InterruptedException, ExecutionException {
        // PortMapping is instantiated to create the mapping of the different nodes in the topology
        this.dataBroker =  getNewDataBroker();
        this.mountPoint = new MountPointStub(dataBroker);
        this.mountPointService = new MountPointServiceStub(mountPoint);
        this.deviceTransactionManager = new DeviceTransactionManagerImpl(mountPointService, 3000);
        this.portMappingVersion22 = new PortMappingVersion221(dataBroker, deviceTransactionManager);
        this.portMappingVersion121 = new PortMappingVersion121(dataBroker, deviceTransactionManager);
        this.portMappingVersion710 = new PortMappingVersion710(dataBroker, deviceTransactionManager);
        this.portMapping = new PortMappingImpl(dataBroker, this.portMappingVersion710,
            this.portMappingVersion22, this.portMappingVersion121);
        this.clientInput = Mockito.mock(ClientInput.class);
        Mockito.when(this.clientInput.clientRangeWishListIntersection()).thenReturn(new EntireSpectrum(768));
        Mockito.when(this.clientInput.clientRangeWishListSubset()).thenReturn(new EntireSpectrum(768));
        Mockito.when(this.clientInput.slotWidth(Mockito.anyInt())).thenAnswer(i -> i.getArguments()[0]);

        //  The catalog of operational mode needs to be loaded so that Ctalog primitives (CatlogUtils)
        // can retrieve physical parameters of the nodes of the path
        DataObjectConverter dataObjectConverter = JSONDataObjectConverter
            .createWithDataStoreUtil(getDataStoreContextUtil());
        try (Reader reader = new FileReader(CATALOG_FILE, StandardCharsets.UTF_8)) {
            NormalizedNode normalizedNode = dataObjectConverter
                .transformIntoNormalizedNode(reader)
                .orElseThrow();
            omCatalog = (OperationalModeCatalog) getDataStoreContextUtil()
                .getBindingDOMCodecServices()
                .fromNormalizedNode(
                    YangInstanceIdentifier.of(OperationalModeCatalog.QNAME), normalizedNode)
                .getValue();
            @NonNull
            WriteTransaction newWriteOnlyTransaction = dataBroker.newWriteOnlyTransaction();
            newWriteOnlyTransaction
                .put(LogicalDatastoreType.CONFIGURATION,
                    DataObjectIdentifier.builder(OperationalModeCatalog.class).build(),
                    omCatalog);
            newWriteOnlyTransaction.commit().get();
        } catch (IOException e) {
            LOG.error("Cannot load OpenROADM part of Operational Mode Catalog ", e);
            fail("Cannot load openROADM operational modes ");
        }
        // The mapping corresponding to the topology is directly populated from a file in the Dta Store
        try (Reader reader = new FileReader(MAPPING_FILE, StandardCharsets.UTF_8)) {
            NormalizedNode normalizedNode = dataObjectConverter.transformIntoNormalizedNode(reader).orElseThrow();
            networkNode = (org.opendaylight.yang.gen.v1.http.org.opendaylight
                    .transportpce.portmapping.rev250115.Network) getDataStoreContextUtil()
                .getBindingDOMCodecServices()
                .fromNormalizedNode(
                    YangInstanceIdentifier.of(org.opendaylight.yang.gen.v1.http.org.opendaylight
                        .transportpce.portmapping.rev250115.Network.QNAME), normalizedNode)
                .getValue();
            @NonNull
            WriteTransaction newWriteOnlyTransaction = dataBroker.newWriteOnlyTransaction();
            newWriteOnlyTransaction
                .put(LogicalDatastoreType.CONFIGURATION,
                    DataObjectIdentifier.builder(org.opendaylight.yang.gen.v1.http.org.opendaylight
                        .transportpce.portmapping.rev250115.Network.class).build(),
                    networkNode);
            newWriteOnlyTransaction.commit().get();
        } catch (IOException e) {
            LOG.error("Cannot load OpenROADM part of Operational Mode Catalog ", e);
            fail("Cannot load openROADM operational modes ");
        }

        MockitoAnnotations.openMocks(this);
        // The topology (openROADM-Network and openROADM-topology layers) is loaded from a file
        try {
            // load openroadm-network
            Reader gnpyNetwork = new FileReader("src/test/resources/gnpy/gnpy_network.json", StandardCharsets.UTF_8);
            Networks networks = (Networks) new JsonDataConverter(null).deserialize(gnpyNetwork, Networks.QNAME);
            saveOpenRoadmNetwork(networks.getNetwork().values().iterator().next(), StringConstants.OPENROADM_NETWORK);
            // load openroadm-topology
            Reader gnpyTopo = new FileReader("src/test/resources/topologyData/or-base-topology.json",
                    StandardCharsets.UTF_8);
            networks = (Networks) new JsonDataConverter(null).deserialize(gnpyTopo, Networks.QNAME);
            saveOpenRoadmNetwork(networks.getNetwork().values().iterator().next(), StringConstants.OPENROADM_TOPOLOGY);
        } catch (IOException | InterruptedException | ExecutionException e) {
            LOG.error("Cannot init test ", e);
            fail("Cannot init test ");
        }
        // init PceHardContraints
        pceHardConstraints = new PceConstraints();
        this.rc = new PceResult();
        this.netTransServ = new NetworkTransactionImpl(dataBroker);
        LOG.info("The value of the mapping is {}", portMapping);
    }

//                       TOPOLOGY ON WHICH TEST ARE BASED
//           _____                      _____                       _____
//          |     | 20dB, 100km,PMD 2  |     | 20dB,100km, PMD 2   |     |
//          |  1  |____________________|  2  |_____________________|  5  |
//          |     |                    |     |                     |     |
//          |_____|                    |_____|                     |_____|
//              |___________      10km    |   20dB,100km,PMD32    /   |  100 km
//                          |      5dB    |   _________|_________/    |  25 dB
//                          |     PMD32 __|__/                      __|__PMD 2.0
//        28dB, 100km,PMD 0 |          |     | 25dB,100km, PMD 2   |     |
//                          |__________|  3  |_____________________|  4  |
//                                     |     |                     |     |
//                                     |_____|                     |_____|
//
    @Test
    void clacPath100GE() {
        PceCalculation pceCalc = new PceCalculation(getPCE1Request(Uint32.valueOf(100), ServiceFormat.Ethernet,
            "XPONDER-1", "Node1", "Client-1", "XPONDER-3", "Node3", "Client-1"),
            netTransServ, pceHardConstraints, null, rc, portMapping);
        pceCalc.retrievePceNetwork();
        pceGraph = new PceGraph(pceCalc.getaendPceNode(), pceCalc.getzendPceNode(),
            pceCalc.getAllPceNodes(), pceCalc.getAllPceLinks(), pceHardConstraints,
            rc, StringConstants.SERVICE_TYPE_100GE_T, netTransServ, PceConstraintMode.Loose, null,
            clientInput);
        assertEquals(pceGraph.calcPath(), true);
        assertEquals(Optional.ofNullable(pceGraph.getmargin()), Optional.ofNullable(3.0919881995992924));
    }

    @Test
    void clacPathOTUC2() {
        PceCalculation pceCalc = new PceCalculation(getPCE1Request(Uint32.valueOf(200), ServiceFormat.Ethernet,
            "XPONDER-1", "Node1", "XPDR-NW1-TX", "XPONDER-4", "Node4", "XPDR-NW1-RX"),
            netTransServ, pceHardConstraints, null, rc, portMapping);
        pceCalc.retrievePceNetwork();
        pceGraph = new PceGraph(pceCalc.getaendPceNode(), pceCalc.getzendPceNode(),
            pceCalc.getAllPceNodes(), pceCalc.getAllPceLinks(), pceHardConstraints,
            rc, StringConstants.SERVICE_TYPE_OTUC2, netTransServ, PceConstraintMode.Loose, null,
            clientInput);
        assertEquals(pceGraph.calcPath(), true);
        assertEquals(Optional.ofNullable(pceGraph.getmargin()), Optional.ofNullable(1.1559963686478447));
    }

    @Test
    void clacPathOTUC3() {
        PceCalculation pceCalc = new PceCalculation(getPCE1Request(Uint32.valueOf(300), ServiceFormat.Ethernet,
            "XPONDER-1", "Node1", "XPDR-NW1-TX", "XPONDER-3", "Node3", "XPDR-NW1-RX"),
            netTransServ, pceHardConstraints, null, rc, portMapping);
        pceCalc.retrievePceNetwork();
        pceGraph = new PceGraph(pceCalc.getaendPceNode(), pceCalc.getzendPceNode(),
            pceCalc.getAllPceNodes(), pceCalc.getAllPceLinks(), pceHardConstraints,
            rc, StringConstants.SERVICE_TYPE_OTUC3, netTransServ, PceConstraintMode.Loose, null,
            clientInput);
        assertEquals(pceGraph.calcPath(), true);
        assertEquals(Optional.ofNullable(pceGraph.getmargin()), Optional.ofNullable(0.3351048800367167));
    }

    @Test
    void clacUnfeasiblePath400GE() {
        PceCalculation pceCalc = new PceCalculation(getPCE1Request(Uint32.valueOf(400), ServiceFormat.Ethernet,
            "XPONDER-1", "Node1", "Client-1", "XPONDER-3", "Node3", "Client-1"),
            netTransServ, pceHardConstraints, null, rc, portMapping);
        pceCalc.retrievePceNetwork();
        pceGraph = new PceGraph(pceCalc.getaendPceNode(), pceCalc.getzendPceNode(),
            pceCalc.getAllPceNodes(), pceCalc.getAllPceLinks(), pceHardConstraints,
            rc, StringConstants.SERVICE_TYPE_400GE, netTransServ, PceConstraintMode.Loose, null,
            clientInput);
        assertEquals(pceGraph.calcPath(), false);
        assertEquals(Optional.ofNullable(pceGraph.getmargin()), Optional.ofNullable(0.0));
    }

    @Test
    void clacPath400GE() {
        PceCalculation pceCalc = new PceCalculation(getPCE1Request(Uint32.valueOf(400), ServiceFormat.Ethernet,
            "XPONDER-1", "Node1", "Client-1", "XPONDER-5", "Node5", "Client-1"),
            netTransServ, pceHardConstraints, null, rc, portMapping);
        pceCalc.retrievePceNetwork();
        pceGraph = new PceGraph(pceCalc.getaendPceNode(), pceCalc.getzendPceNode(),
            pceCalc.getAllPceNodes(), pceCalc.getAllPceLinks(), pceHardConstraints,
            rc, StringConstants.SERVICE_TYPE_400GE, netTransServ, PceConstraintMode.Loose, null,
            clientInput);
        assertEquals(pceGraph.calcPath(), true);
        assertEquals(Optional.ofNullable(pceGraph.getmargin()), Optional.ofNullable(1.4432381874659086));
    }

    @Test
    void clacPathOTUC4() {
        PceCalculation pceCalc = new PceCalculation(getPCE1Request(Uint32.valueOf(400), ServiceFormat.Ethernet,
            "XPONDER-1", "Node1", "XPDR-NW1-TX", "XPONDER-5", "Node5", "XPDR-NW1-RX"),
            netTransServ, pceHardConstraints, null, rc, portMapping);
        pceCalc.retrievePceNetwork();
        pceGraph = new PceGraph(pceCalc.getaendPceNode(), pceCalc.getzendPceNode(),
            pceCalc.getAllPceNodes(), pceCalc.getAllPceLinks(), pceHardConstraints,
            rc, StringConstants.SERVICE_TYPE_OTUC4, netTransServ, PceConstraintMode.Loose, null,
            clientInput);
        assertEquals(pceGraph.calcPath(), true);
        assertEquals(Optional.ofNullable(pceGraph.getmargin()), Optional.ofNullable(1.4432381874659086));
    }

    @Test
    void clacOpticalTunnelOTUC4() {
        PceCalculation pceCalc = new PceCalculation(getPCE1Request(Uint32.valueOf(400), ServiceFormat.OC,
            "OpenROADM-1", "Node1", "DEG1-PP-TX", "OpenROADM-5", "Node5", "DEG3-PP-TX"),
            netTransServ, pceHardConstraints, null, rc, portMapping);
        pceCalc.retrievePceNetwork();
        pceGraph = new PceGraph(pceCalc.getaendPceNode(), pceCalc.getzendPceNode(),
            pceCalc.getAllPceNodes(), pceCalc.getAllPceLinks(), pceHardConstraints,
            rc, StringConstants.SERVICE_TYPE_OTUC4, netTransServ, PceConstraintMode.Loose, null,
            clientInput);
        assertEquals(pceGraph.calcPath(), true);
        assertEquals(Optional.ofNullable(pceGraph.getmargin()), Optional.ofNullable(0.0));
    }

    @Test
    void clacPath100GEnoPort() {
        PceCalculation pceCalc = new PceCalculation(getPCE2Request(Uint32.valueOf(100), ServiceFormat.Ethernet,
            "XPONDER-1", "Node1", "Client-1", "XPONDER-3", "Node3", "Client-1"),
            netTransServ, pceHardConstraints, null, rc, portMapping);
        pceCalc.retrievePceNetwork();
        pceGraph = new PceGraph(pceCalc.getaendPceNode(), pceCalc.getzendPceNode(),
            pceCalc.getAllPceNodes(), pceCalc.getAllPceLinks(), pceHardConstraints,
            rc, StringConstants.SERVICE_TYPE_100GE_T, netTransServ, PceConstraintMode.Loose, null,
            clientInput);
        assertEquals(pceGraph.calcPath(), true);
        assertEquals(Optional.ofNullable(pceGraph.getmargin()), Optional.ofNullable(3.0919881995992924));
    }

    @Test
    void clacPathPropagationDelay() {
        PceCalculation pceCalc = new PceCalculation(getPCE1Request(Uint32.valueOf(100), ServiceFormat.Ethernet,
            "XPONDER-1", "Node1", "Client-1", "XPONDER-3", "Node3", "Client-1"),
            netTransServ, pceHardConstraints, null, rc, portMapping);
        pceCalc.retrievePceNetwork();
        pceHardConstraints.setPceMetrics(PceMetric.PropagationDelay);
        pceGraph = new PceGraph(pceCalc.getaendPceNode(), pceCalc.getzendPceNode(),
            pceCalc.getAllPceNodes(), pceCalc.getAllPceLinks(), pceHardConstraints,
            rc, StringConstants.SERVICE_TYPE_100GE_T, netTransServ, PceConstraintMode.Loose, null,
            clientInput);
        pceGraph.setConstrains(pceHardConstraints);

        assertEquals(pceGraph.calcPath(), true);
        assertEquals(Optional.ofNullable(pceGraph.getPathAtoZ().get(2).getLatency()),
            Optional.ofNullable(1.0));
        assertEquals(pceGraph.getReturnStructure().getRate(), 100);
    }

    //FIXME: Review this test. Getting NPE is never normal!
    @Test
    void clacPath10GE2() {
        assertThrows(NullPointerException.class, () -> {
            getOtnPceGraph(StringConstants.SERVICE_TYPE_10GE);
        });
//        assertEquals(pceGraph.calcPath(), false);
    }

    //FIXME: Review this test. Getting NPE is never normal!
    @Test
    void clacPath1GE() {
        assertThrows(NullPointerException.class, () -> {
            getOtnPceGraph(StringConstants.SERVICE_TYPE_1GE);
        });
//        assertEquals(pceGraph.calcPath(), false);
    }

    private PceGraph getOtnPceGraph(String type) {
        // Build Link
        link1 = NodeUtils.createRoadmToRoadm("optical", "optical2", "DEG1-TTP-TX", "DEG1-TTP-RX").build();

        node = NodeUtils.getOTNNodeBuilder(NodeUtils.geSupportingNodes(), OpenroadmTpType.XPONDERNETWORK).build();

        PceOtnNode pceOtnNode = new PceOtnNode(node, OpenroadmNodeType.MUXPDR,
            new NodeId("optical"), ServiceFormat.OTU.getName(), "serviceType", null);
        pceOtnNode.validateXponder("optical", "sl");
        pceOtnNode.validateXponder("not optical", "sl");
        pceOtnNode.initXndrTps("AZ");
        pceOtnNode.checkAvailableTribPort();
        pceOtnNode.checkAvailableTribSlot();

        PceOtnNode pceOtnNode2 = new PceOtnNode(node, OpenroadmNodeType.MUXPDR,
            new NodeId("optical2"), ServiceFormat.OTU.getName(), "serviceType", null);
        pceOtnNode2.validateXponder("optical", "sl");
        pceOtnNode2.validateXponder("not optical", "sl");
        pceOtnNode2.initXndrTps("AZ");
        pceOtnNode2.initXndrTps("mode");
        pceOtnNode2.checkAvailableTribPort();
        pceOtnNode2.checkAvailableTribSlot();

        pceLink1 = new PceLink(link1, pceOtnNode, pceOtnNode2);
        pceLink1.setClientA("XPONDER-CLIENT");

        pceLink1.getDestId();
        pceOtnNode.addOutgoingLink(pceLink1);

        // init PceHardContraints
        pceHardConstraints = new PceConstraints();
        // pceHardConstraints.setp
        allPceNodes = Map.of(
            new NodeId("optical"), pceOtnNode,
            new NodeId("optical2"), pceOtnNode2);
        return new PceGraph(pceOtnNode, pceOtnNode2, allPceNodes, allPceLinks, pceHardConstraints,
                new PceResult(), type, null, PceConstraintMode.Loose, null,
                clientInput);
    }

    private void saveOpenRoadmNetwork(Network network, String networkId)
            throws InterruptedException, ExecutionException {
        DataObjectIdentifier<Network> nwInstanceIdentifier = DataObjectIdentifier.builder(Networks.class)
            .child(Network.class, new NetworkKey(new NetworkId(networkId)))
            .build();
        WriteTransaction dataWriteTransaction = dataBroker.newWriteOnlyTransaction();
        dataWriteTransaction.put(LogicalDatastoreType.CONFIGURATION, nwInstanceIdentifier, network);
        dataWriteTransaction.commit().get();
    }

    public static Node createNetworkNode(String nodeId, OpenroadmNodeType nodeType) {
        SupportingNode supportingNode = new SupportingNodeBuilder()
            .setNetworkRef(new NetworkId(StringConstants.CLLI_NETWORK))
            .setNodeRef(new NodeId("node1"))
            .withKey(new SupportingNodeKey(new NetworkId(StringConstants.CLLI_NETWORK),
                new NodeId("node1")))
            .build();
        return new NodeBuilder()
            .setNodeId(new NodeId(nodeId))
            .withKey(new NodeKey(new NodeId(nodeId)))
            .setSupportingNode(ImmutableMap.of(supportingNode.key(), supportingNode))
            .addAugmentation(
                new Node1Builder().setOpenroadmVersion(OpenroadmVersionType._221).build())
            .addAugmentation(
                new org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev230526.Node1Builder()
                    .setNodeType(nodeType).build())
            .build();
    }

    public static TerminationPoint createNetworkTp(String nodeId, String tpId) {
        var nwTpId = new TpId(tpId);
        return new TerminationPointBuilder()
            .setTpId(nwTpId)
            .withKey(new TerminationPointKey(nwTpId))
            .addAugmentation(new TerminationPoint1Builder()
                .setXpdrNetworkAttributes(new XpdrNetworkAttributesBuilder().setState(State.InService).build())
                .build())
            .build();
    }

    public static Node createTopologyNode(String nodeId, OpenroadmNodeType nodeType) {
        SupportingNode supportingNode = new SupportingNodeBuilder()
            .setNetworkRef(new NetworkId(StringConstants.OPENROADM_NETWORK))
            .setNodeRef(new NodeId("node1"))
            .withKey(new SupportingNodeKey(new NetworkId(StringConstants.OPENROADM_NETWORK),
                new NodeId("node1")))
            .build();
        return new NodeBuilder()
            .setNodeId(new NodeId(nodeId))
            .withKey(new NodeKey(new NodeId(nodeId)))
            .setSupportingNode(ImmutableMap.of(supportingNode.key(), supportingNode))
            .addAugmentation(
                new org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev230526.Node1Builder()
                    .setXpdrAttributes(null).build())
            .addAugmentation(
                new org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev230526.Node1Builder()
                    .setNodeType(nodeType).build())
            .build();
    }

    public static PathComputationRequestInput getPCE1Request(Uint32 rate, ServiceFormat serviceFormat, String aaNodeId,
            String aaClliId, String aaPortName, String zzNodeId, String zzClliId, String zzPortName) {
        return new PathComputationRequestInputBuilder()
            .setServiceName("service1")
            .setResourceReserve(true)
            .setPceRoutingMetric(PceMetric.HopCount)
            .setServiceHandlerHeader(new ServiceHandlerHeaderBuilder()
                .setRequestId("request1")
                .build())
            .setServiceAEnd(new ServiceAEndBuilder()
                .setServiceFormat(serviceFormat)
                .setServiceRate(Uint32.valueOf(100))
                .setClli(aaClliId)
                .setNodeId(aaNodeId)
                .setTxDirection(new TxDirectionBuilder()
                    .setPort(new PortBuilder()
                        .setPortDeviceName(aaNodeId)
                        .setPortType("fixed")
                        .setPortName(aaPortName)
                        .setPortRack("Some port-rack")
                        .setPortShelf("Some port-shelf")
                        .setPortSlot("Some port-slot")
                        .setPortSubSlot("Some port-sub-slot")
                        .build())
                    .build())
                .setRxDirection(new RxDirectionBuilder()
                    .setPort(new PortBuilder()
                        .setPortDeviceName(aaNodeId)
                        .setPortType("fixed")
                        .setPortName(aaPortName)
                        .setPortRack("Some port-rack")
                        .setPortShelf("Some port-shelf")
                        .setPortSlot("Some port-slot")
                        .setPortSubSlot("Some port-sub-slot")
                        .build())
                    .build())
                .build())
            .setServiceZEnd(new ServiceZEndBuilder()
                .setServiceFormat(serviceFormat)
                .setServiceRate(Uint32.valueOf(0))
                .setClli(zzClliId)
                .setNodeId(zzNodeId)
                .setTxDirection(new TxDirectionBuilder()
                    .setPort(new PortBuilder()
                        .setPortDeviceName(zzNodeId)
                        .setPortType("fixed")
                        .setPortName(zzPortName)
                        .setPortRack("Some port-rack")
                        .setPortShelf("Some port-shelf")
                        .setPortSlot("Some port-slot")
                        .setPortSubSlot("Some port-sub-slot")
                        .build())
                    .build())
                .setRxDirection(new RxDirectionBuilder()
                    .setPort(new PortBuilder()
                        .setPortDeviceName(zzNodeId)
                        .setPortType("fixed")
                        .setPortName(zzPortName)
                        .setPortRack("Some port-rack")
                        .setPortShelf("Some port-shelf")
                        .setPortSlot("Some port-slot")
                        .setPortSubSlot("Some port-sub-slot")
                        .build())
                    .build())
                .build())
            .setHardConstraints(new HardConstraintsBuilder()
                .setCustomerCode(Set.of("Some customer-code"))
                .setCoRouting(new CoRoutingBuilder()
                    .setServiceIdentifierList(Map.of(
                        new org.opendaylight.yang.gen.v1.http.org.openroadm.routing.constraints.rev221209
                                .constraints.co.routing.ServiceIdentifierListKey("Some existing-service"),
                        new ServiceIdentifierListBuilder().setServiceIdentifier("Some existing-service").build()))
                    .build())
                .build())
            .setSoftConstraints(new SoftConstraintsBuilder()
                .setCustomerCode(Set.of("Some customer-code"))
                .setCoRouting(new CoRoutingBuilder()
                    .setServiceIdentifierList(Map.of(
                        new org.opendaylight.yang.gen.v1.http.org.openroadm.routing.constraints.rev221209
                                .constraints.co.routing.ServiceIdentifierListKey("Some existing-service"),
                        new ServiceIdentifierListBuilder().setServiceIdentifier("Some existing-service").build()))
                    .build())
                .build())
            .build();
    }

    public static PathComputationRequestInput getPCE2Request(Uint32 rate, ServiceFormat serviceFormat, String aaNodeId,
            String aaClliId, String aaPortName, String zzNodeId, String zzClliId, String zzPortName) {
        return new PathComputationRequestInputBuilder()
            .setServiceName("service1")
            .setResourceReserve(true)
            .setPceRoutingMetric(PceMetric.HopCount)
            .setServiceHandlerHeader(new ServiceHandlerHeaderBuilder()
                .setRequestId("request1")
                .build())
            .setServiceAEnd(new ServiceAEndBuilder()
                .setServiceFormat(serviceFormat)
                .setServiceRate(Uint32.valueOf(100))
                .setClli(aaClliId)
                .setNodeId(aaNodeId)
                .setTxDirection(new TxDirectionBuilder()
                  .build())
                .setRxDirection(new RxDirectionBuilder()
                    .build())
                .build())
            .setServiceZEnd(new ServiceZEndBuilder()
                .setServiceFormat(serviceFormat)
                .setServiceRate(Uint32.valueOf(0))
                .setClli(zzClliId)
                .setNodeId(zzNodeId)
                .setTxDirection(new TxDirectionBuilder()
                    .build())
                .setRxDirection(new RxDirectionBuilder()
                    .build())
                .build())
            .setHardConstraints(new HardConstraintsBuilder()
                .setCustomerCode(Set.of("Some customer-code"))
                .setCoRouting(new CoRoutingBuilder()
                    .setServiceIdentifierList(Map.of(
                        new org.opendaylight.yang.gen.v1.http.org.openroadm.routing.constraints.rev221209
                                .constraints.co.routing.ServiceIdentifierListKey("Some existing-service"),
                        new ServiceIdentifierListBuilder().setServiceIdentifier("Some existing-service").build()))
                    .build())
                .build())
            .setSoftConstraints(new SoftConstraintsBuilder()
                .setCustomerCode(Set.of("Some customer-code"))
                .setCoRouting(new CoRoutingBuilder()
                    .setServiceIdentifierList(Map.of(
                        new org.opendaylight.yang.gen.v1.http.org.openroadm.routing.constraints.rev221209
                                .constraints.co.routing.ServiceIdentifierListKey("Some existing-service"),
                        new ServiceIdentifierListBuilder().setServiceIdentifier("Some existing-service").build()))
                    .build())
                .build())
                    .build();
    }
}
