/*
 * Copyright © 2025 Smartoptics and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.tapi.connectivity;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.google.common.util.concurrent.ListenableFuture;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.transportpce.common.InstanceIdentifiers;
import org.opendaylight.transportpce.common.mapping.PortMapping;
import org.opendaylight.transportpce.common.mapping.PortMappingImpl;
import org.opendaylight.transportpce.common.network.NetworkTransactionImpl;
import org.opendaylight.transportpce.common.network.NetworkTransactionService;
import org.opendaylight.transportpce.servicehandler.service.ServiceDataStoreOperations;
import org.opendaylight.transportpce.tapi.TapiConstants;
import org.opendaylight.transportpce.tapi.topology.TapiTopologyException;
import org.opendaylight.transportpce.tapi.topology.TopologyUtils;
import org.opendaylight.transportpce.tapi.utils.TapiContext;
import org.opendaylight.transportpce.test.AbstractTest;
import org.opendaylight.transportpce.test.utils.TopologyDataUtils;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev240205.service.path.rpc.result.PathDescriptionBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.state.types.rev191129.State;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev230501.PathDescription;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev230501.path.description.AToZDirectionBuilder;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev230501.path.description.atoz.direction.AToZ;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev230501.path.description.atoz.direction.AToZBuilder;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev230501.path.description.atoz.direction.AToZKey;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev230501.pce.resource.ResourceBuilder;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev230501.pce.resource.resource.resource.LinkBuilder;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev230501.pce.resource.resource.resource.NodeBuilder;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev230501.pce.resource.resource.resource.TerminationPointBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.networks.Network;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.Uuid;
import org.opendaylight.yangtools.binding.DataObjectIdentifier;


class ConnectivityUtilsTest extends AbstractTest {

    private PortMapping portMapping;

    private NetworkTransactionService networkTransactionService;

    @Mock
    private ServiceDataStoreOperations serviceDataStoreOperations;

    @Mock
    private TapiContext tapiContext;

    @Mock
    private TopologyUtils topologyUtils;

    @BeforeEach
    void setUp() throws InterruptedException, ExecutionException {
        DataBroker dataBroker = getDataStoreContextUtil().getDataBroker();

        networkTransactionService = new NetworkTransactionImpl(getDataBroker());

        portMapping = new PortMappingImpl(dataBroker, null, null, null, null);

        TopologyDataUtils.writeTopologyFromFileToDatastore(getDataStoreContextUtil(),
                "src/test/resources/connectivity-utils/openroadm-network.xml",
                InstanceIdentifiers.OPENROADM_NETWORK_II);

        TopologyDataUtils.writeTopologyFromFileToDatastore(getDataStoreContextUtil(),
                "src/test/resources/connectivity-utils/openroadm-topology.xml",
                InstanceIdentifiers.OPENROADM_TOPOLOGY_II);

        TopologyDataUtils.writeTopologyFromFileToDatastore(getDataStoreContextUtil(),
                "src/test/resources/connectivity-utils/otn-topology.xml",
                InstanceIdentifiers.OTN_NETWORK_II);

        TopologyDataUtils.writePortmappingFromFileToDatastore(getDataStoreContextUtil(),
                "src/test/resources/connectivity-utils/portmapping.xml");
    }

    @Test
    void testRoadmToRoadmConnectivity() throws TapiTopologyException {
        Map<AToZKey, AToZ> atoZMap = new HashMap<>();

        // ID 0 – TerminationPoint
        AToZ atoZItem0 = new AToZBuilder()
                .setResource(new ResourceBuilder()
                        .setResource(new TerminationPointBuilder()
                                .setTpId("")
                                .setTpNodeId("SPDR-SA1-XPDR1")
                                .build())
                        .setState(State.InService)
                        .build())
                .setId("0")
                .build();
        atoZMap.put(atoZItem0.key(), atoZItem0);

        // ID 1 – Node
        AToZ atoZItem1 = new AToZBuilder()
                .setResource(new ResourceBuilder()
                        .setResource(new NodeBuilder()
                                .setNodeId("SPDR-SA1-XPDR1")
                                .build())
                        .setState(State.InService)
                        .build())
                .setId("1")
                .build();
        atoZMap.put(atoZItem1.key(), atoZItem1);

        // ID 2 – TerminationPoint
        AToZ atoZItem2 = new AToZBuilder()
                .setResource(new ResourceBuilder()
                        .setResource(new TerminationPointBuilder()
                                .setTpId("XPDR1-NETWORK1")
                                .setTpNodeId("SPDR-SA1-XPDR1")
                                .build())
                        .setState(State.InService)
                        .build())
                .setId("2")
                .build();
        atoZMap.put(atoZItem2.key(), atoZItem2);

        // ID 3 – Link
        AToZ atoZItem3 = new AToZBuilder()
                .setResource(new ResourceBuilder()
                        .setResource(new LinkBuilder()
                                .setLinkId("SPDR-SA1-XPDR1-XPDR1-NETWORK1toROADM-A1-SRG1-SRG1-PP1-TXRX")
                                .build())
                        .setState(State.InService)
                        .build())
                .setId("3")
                .build();
        atoZMap.put(atoZItem3.key(), atoZItem3);

        // ID 4 – TerminationPoint
        AToZ atoZItem4 = new AToZBuilder()
                .setResource(new ResourceBuilder()
                        .setResource(new TerminationPointBuilder()
                                .setTpId("SRG1-PP1-TXRX")
                                .setTpNodeId("ROADM-A1-SRG1")
                                .build())
                        .setState(State.InService)
                        .build())
                .setId("4")
                .build();
        atoZMap.put(atoZItem4.key(), atoZItem4);

        // ID 5 – Node
        AToZ atoZItem5 = new AToZBuilder()
                .setResource(new ResourceBuilder()
                        .setResource(new NodeBuilder()
                                .setNodeId("ROADM-A1-SRG1")
                                .build())
                        .setState(State.InService)
                        .build())
                .setId("5")
                .build();
        atoZMap.put(atoZItem5.key(), atoZItem5);

        // ID 6 – TerminationPoint
        AToZ atoZItem6 = new AToZBuilder()
                .setResource(new ResourceBuilder()
                        .setResource(new TerminationPointBuilder()
                                .setTpId("SRG1-CP-TXRX")
                                .setTpNodeId("ROADM-A1-SRG1")
                                .build())
                        .setState(State.InService)
                        .build())
                .setId("6")
                .build();
        atoZMap.put(atoZItem6.key(), atoZItem6);

        // ID 7 – Link
        AToZ atoZItem7 = new AToZBuilder()
                .setResource(new ResourceBuilder()
                        .setResource(new LinkBuilder()
                                .setLinkId("ROADM-A1-SRG1-SRG1-CP-TXRXtoROADM-A1-DEG2-DEG2-CTP-TXRX")
                                .build())
                        .setState(State.InService)
                        .build())
                .setId("7")
                .build();
        atoZMap.put(atoZItem7.key(), atoZItem7);

        // ID 8 – TerminationPoint
        AToZ atoZItem8 = new AToZBuilder()
                .setResource(new ResourceBuilder()
                        .setResource(new TerminationPointBuilder()
                                .setTpId("DEG2-CTP-TXRX")
                                .setTpNodeId("ROADM-A1-DEG2")
                                .build())
                        .setState(State.InService)
                        .build())
                .setId("8")
                .build();
        atoZMap.put(atoZItem8.key(), atoZItem8);

        // ID 9 – Node
        AToZ atoZItem9 = new AToZBuilder()
                .setResource(new ResourceBuilder()
                        .setResource(new NodeBuilder()
                                .setNodeId("ROADM-A1-DEG2")
                                .build())
                        .setState(State.InService)
                        .build())
                .setId("9")
                .build();
        atoZMap.put(atoZItem9.key(), atoZItem9);

        // ID 10 – TerminationPoint
        AToZ atoZItem10 = new AToZBuilder()
                .setResource(new ResourceBuilder()
                        .setResource(new TerminationPointBuilder()
                                .setTpId("DEG2-TTP-TXRX")
                                .setTpNodeId("ROADM-A1-DEG2")
                                .build())
                        .setState(State.InService)
                        .build())
                .setId("10")
                .build();
        atoZMap.put(atoZItem10.key(), atoZItem10);

        // ID 11 – Link
        AToZ atoZItem11 = new AToZBuilder()
                .setResource(new ResourceBuilder()
                        .setResource(new LinkBuilder()
                                .setLinkId("ROADM-A1-DEG2-DEG2-TTP-TXRXtoROADM-C1-DEG1-DEG1-TTP-TXRX")
                                .build())
                        .setState(State.InService)
                        .build())
                .setId("11")
                .build();
        atoZMap.put(atoZItem11.key(), atoZItem11);

        // ID 12 – TerminationPoint
        AToZ atoZItem12 = new AToZBuilder()
                .setResource(new ResourceBuilder()
                        .setResource(new TerminationPointBuilder()
                                .setTpId("DEG1-TTP-TXRX")
                                .setTpNodeId("ROADM-C1-DEG1")
                                .build())
                        .setState(State.InService)
                        .build())
                .setId("12")
                .build();
        atoZMap.put(atoZItem12.key(), atoZItem12);

        // ID 13 – Node
        AToZ atoZItem13 = new AToZBuilder()
                .setResource(new ResourceBuilder()
                        .setResource(new NodeBuilder()
                                .setNodeId("ROADM-C1-DEG1")
                                .build())
                        .setState(State.InService)
                        .build())
                .setId("13")
                .build();
        atoZMap.put(atoZItem13.key(), atoZItem13);

        // ID 14 – TerminationPoint
        AToZ atoZItem14 = new AToZBuilder()
                .setResource(new ResourceBuilder()
                        .setResource(new TerminationPointBuilder()
                                .setTpId("DEG1-CTP-TXRX")
                                .setTpNodeId("ROADM-C1-DEG1")
                                .build())
                        .setState(State.InService)
                        .build())
                .setId("14")
                .build();
        atoZMap.put(atoZItem14.key(), atoZItem14);

        // ID 15 – Link
        AToZ atoZItem15 = new AToZBuilder()
                .setResource(new ResourceBuilder()
                        .setResource(new LinkBuilder()
                                .setLinkId("ROADM-C1-DEG1-DEG1-CTP-TXRXtoROADM-C1-SRG1-SRG1-CP-TXRX")
                                .build())
                        .setState(State.InService)
                        .build())
                .setId("15")
                .build();
        atoZMap.put(atoZItem15.key(), atoZItem15);

        // ID 16 – TerminationPoint
        AToZ atoZItem16 = new AToZBuilder()
                .setResource(new ResourceBuilder()
                        .setResource(new TerminationPointBuilder()
                                .setTpId("SRG1-CP-TXRX")
                                .setTpNodeId("ROADM-C1-SRG1")
                                .build())
                        .setState(State.InService)
                        .build())
                .setId("16")
                .build();
        atoZMap.put(atoZItem16.key(), atoZItem16);

        // ID 17 – Node
        AToZ atoZItem17 = new AToZBuilder()
                .setResource(new ResourceBuilder()
                        .setResource(new NodeBuilder()
                                .setNodeId("ROADM-C1-SRG1")
                                .build())
                        .setState(State.InService)
                        .build())
                .setId("17")
                .build();
        atoZMap.put(atoZItem17.key(), atoZItem17);

        // ID 18 – TerminationPoint
        AToZ atoZItem18 = new AToZBuilder()
                .setResource(new ResourceBuilder()
                        .setResource(new TerminationPointBuilder()
                                .setTpId("SRG1-PP1-TXRX")
                                .setTpNodeId("ROADM-C1-SRG1")
                                .build())
                        .setState(State.InService)
                        .build())
                .setId("18")
                .build();
        atoZMap.put(atoZItem18.key(), atoZItem18);

        // ID 19 – Link
        AToZ atoZItem19 = new AToZBuilder()
                .setResource(new ResourceBuilder()
                        .setResource(new LinkBuilder()
                                .setLinkId("ROADM-C1-SRG1-SRG1-PP1-TXRXtoSPDR-SC1-XPDR1-XPDR1-NETWORK1")
                                .build())
                        .setState(State.InService)
                        .build())
                .setId("19")
                .build();
        atoZMap.put(atoZItem19.key(), atoZItem19);

        // ID 20 – TerminationPoint
        AToZ atoZItem20 = new AToZBuilder()
                .setResource(new ResourceBuilder()
                        .setResource(new TerminationPointBuilder()
                                .setTpId("XPDR1-NETWORK1")
                                .setTpNodeId("SPDR-SC1-XPDR1")
                                .build())
                        .setState(State.InService)
                        .build())
                .setId("20")
                .build();
        atoZMap.put(atoZItem20.key(), atoZItem20);

        // ID 21 – Node
        AToZ atoZItem21 = new AToZBuilder()
                .setResource(new ResourceBuilder()
                        .setResource(new NodeBuilder()
                                .setNodeId("SPDR-SC1-XPDR1")
                                .build())
                        .setState(State.InService)
                        .build())
                .setId("21")
                .build();
        atoZMap.put(atoZItem21.key(), atoZItem21);

        // ID 22 – TerminationPoint
        AToZ atoZItem22 = new AToZBuilder()
                .setResource(new ResourceBuilder()
                        .setResource(new TerminationPointBuilder()
                                .setTpId("")
                                .setTpNodeId("SPDR-SC1-XPDR1")
                                .build())
                        .setState(State.InService)
                        .build())
                .setId("22")
                .build();
        atoZMap.put(atoZItem22.key(), atoZItem22);

        // Build the AToZDirection and PathDescription
        AToZDirectionBuilder atoZDirectionBuilder = new AToZDirectionBuilder()
                .setAToZ(atoZMap);

        PathDescription pathDescription = new PathDescriptionBuilder()
                .setAToZDirection(atoZDirectionBuilder.build())
                .build();

        ConnectivityUtils connectivityUtils = new ConnectivityUtils(
                serviceDataStoreOperations,
                new HashMap<>(),
                tapiContext,
                networkTransactionService,
                new Uuid(TapiConstants.T0_FULL_MULTILAYER_UUID),
                topologyUtils,
                portMapping
        );
        Connectivity connectivity = connectivityUtils.connectivityMap(pathDescription,
                readTopology(InstanceIdentifiers.OPENROADM_TOPOLOGY_II));
        ConnectivityMap connectivityMap = (ConnectivityMap) connectivity;

        List<String> expectedRoadmNodeList = List.of("ROADM-A1", "ROADM-C1");
        assertEquals(expectedRoadmNodeList, connectivityMap.rdmNodelist(), "ROADM node list mismatch");

        List<String> expectedRoadmDegreeList = List.of(
                "ROADM-A1+DEG2-TTP-TXRX",
                "ROADM-C1+DEG1-TTP-TXRX"
        );
        assertEquals(expectedRoadmDegreeList, connectivityMap.rdmDegTplist(), "ROADM degree list mismatch");

        List<String> expectedRoadmAddDropList = List.of(
                "ROADM-A1+SRG1-PP1-TXRX",
                "ROADM-C1+SRG1-PP1-TXRX"
        );
        assertEquals(expectedRoadmAddDropList, connectivityMap.rdmAddDropTplist(), "ROADM add/drop list mismatch");

        List<String> expectedXpdrNodeList = List.of(
                "SPDR-SA1-XPDR1",
                "SPDR-SC1-XPDR1"
        );
        assertEquals(expectedXpdrNodeList, connectivityMap.xpdrNodelist(), "XPDR node list mismatch");

        List<String> expectedXpdrNetworkList = List.of(
                "SPDR-SA1-XPDR1+XPDR1-NETWORK1",
                "SPDR-SC1-XPDR1+XPDR1-NETWORK1"
        );
        assertEquals(expectedXpdrNetworkList, connectivityMap.xpdrNetworkTplist(), "XPDR network list mismatch");

        List<String> expectedXpdrClientList = List.of(); // empty
        assertEquals(expectedXpdrClientList, connectivityMap.xpdrClientTplist(), "XPDR client list mismatch");

    }

    @Test
    void testXpdrToXpdrConnectivity() throws TapiTopologyException {
        // --- Build the PathDescription for this scenario ---
        Map<AToZKey, AToZ> atoZMap = new HashMap<>();

        // ID 0 – XPDR-SA1 TerminationPoint
        AToZ atoZItem0 = new AToZBuilder()
                .setResource(new ResourceBuilder()
                        .setResource(new TerminationPointBuilder()
                                .setTpId("")
                                .setTpNodeId("SPDR-SA1-XPDR1")
                                .build())
                        .setState(State.InService)
                        .build())
                .setId("0")
                .build();
        atoZMap.put(atoZItem0.key(), atoZItem0);

        // ID 1 – Node SPDR-SA1-XPDR1
        AToZ atoZItem1 = new AToZBuilder()
                .setResource(new ResourceBuilder()
                        .setResource(new NodeBuilder()
                                .setNodeId("SPDR-SA1-XPDR1")
                                .build())
                        .setState(State.InService)
                        .build())
                .setId("1")
                .build();
        atoZMap.put(atoZItem1.key(), atoZItem1);

        // ID 2 – XPDR-SA1 TerminationPoint (NETWORK)
        AToZ atoZItem2 = new AToZBuilder()
                .setResource(new ResourceBuilder()
                        .setResource(new TerminationPointBuilder()
                                .setTpId("XPDR1-NETWORK1")
                                .setTpNodeId("SPDR-SA1-XPDR1")
                                .build())
                        .setState(State.InService)
                        .build())
                .setId("2")
                .build();
        atoZMap.put(atoZItem2.key(), atoZItem2);

        // ID 3 – Link between transponders
        AToZ atoZItem3 = new AToZBuilder()
                .setResource(new ResourceBuilder()
                        .setResource(new LinkBuilder()
                                .setLinkId("OTU4-SPDR-SA1-XPDR1-XPDR1-NETWORK1toSPDR-SC1-XPDR1-XPDR1-NETWORK1")
                                .build())
                        .setState(State.InService)
                        .build())
                .setId("3")
                .build();
        atoZMap.put(atoZItem3.key(), atoZItem3);

        // ID 4 – XPDR-SC1 TerminationPoint (NETWORK)
        AToZ atoZItem4 = new AToZBuilder()
                .setResource(new ResourceBuilder()
                        .setResource(new TerminationPointBuilder()
                                .setTpId("XPDR1-NETWORK1")
                                .setTpNodeId("SPDR-SC1-XPDR1")
                                .build())
                        .setState(State.InService)
                        .build())
                .setId("4")
                .build();
        atoZMap.put(atoZItem4.key(), atoZItem4);

        // ID 5 – Node SPDR-SC1-XPDR1
        AToZ atoZItem5 = new AToZBuilder()
                .setResource(new ResourceBuilder()
                        .setResource(new NodeBuilder()
                                .setNodeId("SPDR-SC1-XPDR1")
                                .build())
                        .setState(State.InService)
                        .build())
                .setId("5")
                .build();
        atoZMap.put(atoZItem5.key(), atoZItem5);

        // ID 6 – XPDR-SC1 TerminationPoint
        AToZ atoZItem6 = new AToZBuilder()
                .setResource(new ResourceBuilder()
                        .setResource(new TerminationPointBuilder()
                                .setTpId("")
                                .setTpNodeId("SPDR-SC1-XPDR1")
                                .build())
                        .setState(State.InService)
                        .build())
                .setId("6")
                .build();
        atoZMap.put(atoZItem6.key(), atoZItem6);

        // Build PathDescription
        AToZDirectionBuilder atoZDirectionBuilder = new AToZDirectionBuilder().setAToZ(atoZMap);
        PathDescription pathDescription = new PathDescriptionBuilder()
                .setAToZDirection(atoZDirectionBuilder.build())
                .build();

        ConnectivityUtils connectivityUtils = new ConnectivityUtils(
                serviceDataStoreOperations,
                new HashMap<>(),
                tapiContext,
                networkTransactionService,
                new Uuid(TapiConstants.T0_FULL_MULTILAYER_UUID),
                topologyUtils,
                portMapping
        );

        Connectivity connectivity = connectivityUtils.connectivityMap(pathDescription,
                readTopology(InstanceIdentifiers.OPENROADM_TOPOLOGY_II));
        ConnectivityMap connectivityMap = (ConnectivityMap) connectivity;

        // Assertions
        List<String> expectedRoadmNodeList = List.of();
        assertEquals(expectedRoadmNodeList, connectivityMap.rdmNodelist(), "ROADM node list mismatch");

        List<String> expectedRoadmDegreeList = List.of();
        assertEquals(expectedRoadmDegreeList, connectivityMap.rdmDegTplist(), "ROADM degree list mismatch");

        List<String> expectedRoadmAddDropList = List.of();
        assertEquals(expectedRoadmAddDropList, connectivityMap.rdmAddDropTplist(), "ROADM add/drop list mismatch");

        List<String> expectedXpdrNodeList = List.of("SPDR-SA1-XPDR1", "SPDR-SC1-XPDR1");
        assertEquals(expectedXpdrNodeList, connectivityMap.xpdrNodelist(), "XPDR node list mismatch");

        List<String> expectedXpdrNetworkList = List.of(
                "SPDR-SA1-XPDR1+XPDR1-NETWORK1",
                "SPDR-SC1-XPDR1+XPDR1-NETWORK1"
        );
        assertEquals(expectedXpdrNetworkList, connectivityMap.xpdrNetworkTplist(), "XPDR network list mismatch");

        List<String> expectedXpdrClientList = List.of();
        assertEquals(expectedXpdrClientList, connectivityMap.xpdrClientTplist(), "XPDR client list mismatch");
    }

    public Network readTopology(DataObjectIdentifier<Network> networkIID) throws TapiTopologyException {
        ListenableFuture<Optional<Network>> topologyFuture =
                this.networkTransactionService.read(LogicalDatastoreType.CONFIGURATION, networkIID);
        try {
            return topologyFuture.get().orElseThrow();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new TapiTopologyException(
                    "Unable to get from mdsal topology: " + networkIID.firstKeyOf(Network.class).getNetworkId()
                            .getValue(), e);
        } catch (ExecutionException e) {
            throw new TapiTopologyException(
                    "Unable to get from mdsal topology: " + networkIID.firstKeyOf(Network.class).getNetworkId()
                            .getValue(), e);
        } catch (NoSuchElementException e) {
            return null;
        }
    }
}
