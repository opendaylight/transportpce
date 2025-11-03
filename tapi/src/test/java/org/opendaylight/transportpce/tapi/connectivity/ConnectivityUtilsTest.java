/*
 * Copyright © 2025 Smartoptics and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.tapi.connectivity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.opendaylight.transportpce.common.InstanceIdentifiers;
import org.opendaylight.transportpce.common.network.NetworkTransactionImpl;
import org.opendaylight.transportpce.common.network.NetworkTransactionService;
import org.opendaylight.transportpce.servicehandler.service.ServiceDataStoreOperations;
import org.opendaylight.transportpce.tapi.TapiConstants;
import org.opendaylight.transportpce.tapi.topology.TapiTopologyException;
import org.opendaylight.transportpce.tapi.topology.TopologyUtils;
import org.opendaylight.transportpce.tapi.utils.TapiContext;
import org.opendaylight.transportpce.tapi.utils.TapiLink;
import org.opendaylight.transportpce.test.AbstractTest;
import org.opendaylight.transportpce.test.utils.TopologyDataUtils;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev240205.service.path.rpc.result.PathDescriptionBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev250110.ConnectionType;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.state.types.rev191129.State;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.format.rev191129.ServiceFormat;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev230501.PathDescription;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev230501.path.description.AToZDirectionBuilder;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev230501.path.description.atoz.direction.AToZ;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev230501.path.description.atoz.direction.AToZBuilder;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev230501.path.description.atoz.direction.AToZKey;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev230501.path.description.ztoa.direction.ZToA;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev230501.path.description.ztoa.direction.ZToABuilder;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev230501.path.description.ztoa.direction.ZToAKey;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev230501.pce.resource.ResourceBuilder;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev230501.pce.resource.resource.resource.LinkBuilder;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev230501.pce.resource.resource.resource.NodeBuilder;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev230501.pce.resource.resource.resource.TerminationPointBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.networks.Network;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.Uuid;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.local._class.Name;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.local._class.NameBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.tapi.context.ServiceInterfacePoint;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.tapi.context.ServiceInterfacePointBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev221121.create.connectivity.service.input.EndPoint;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev221121.create.connectivity.service.input.EndPointBuilder;


class ConnectivityUtilsTest extends AbstractTest {

    private NetworkTransactionService networkTransactionService;

    @Mock
    private ServiceDataStoreOperations serviceDataStoreOperations;

    @Mock
    private TapiContext tapiContext;

    @Mock
    private TapiLink tapiLink;

    private TopologyUtils topologyUtils;

    @BeforeEach
    void setUp() throws InterruptedException, ExecutionException {
        networkTransactionService = new NetworkTransactionImpl(getDataBroker());

        TopologyDataUtils.writeTopologyFromFileToDatastore(getDataStoreContextUtil(),
                "src/test/resources/connectivity-utils/openroadm-topology.xml",
                InstanceIdentifiers.OPENROADM_TOPOLOGY_II);

        topologyUtils = new TopologyUtils(networkTransactionService, getDataBroker(), tapiLink);
    }

    @Test
    public void getConnectionTypePhtncXponder() throws TapiTopologyException {
        Name nameOne = new NameBuilder()
                .setValueName("OpenROADM node id")
                .setValue("SPDR-SA1-XPDR1")
                .build();
        EndPoint endPointOne = new EndPointBuilder()
                .setLocalId("SPDR-SA1-XPDR1")
                .setName(Map.of(nameOne.key(), nameOne))
                .build();

        Name nameTwo = new NameBuilder()
                .setValueName("OpenROADM node id")
                .setValue("SPDR-SC1-XPDR1")
                .build();
        EndPoint endPointTwo = new EndPointBuilder()
                .setLocalId("SPDR-SC1-XPDR1")
                .setName(Map.of(nameTwo.key(), nameTwo))
                .build();

        ConnectivityUtils connectivityUtils = new ConnectivityUtils(
                serviceDataStoreOperations,
                new HashMap<>(),
                tapiContext,
                networkTransactionService,
                new Uuid(TapiConstants.T0_FULL_MULTILAYER_UUID),
                topologyUtils
        );

        Network openroadmTopo = topologyUtils.readTopology(InstanceIdentifiers.OPENROADM_TOPOLOGY_II);

        assertEquals(ConnectionType.Infrastructure, connectivityUtils.getConnectionTypePhtnc(
                Collections.checkedList(List.of(endPointOne, endPointTwo), EndPoint.class),
                openroadmTopo
        ));
    }

    @Test
    public void getServiceFormatPhtncXponder() throws TapiTopologyException {
        Name nameOne = new NameBuilder()
                .setValueName("OpenROADM node id")
                .setValue("SPDR-SA1-XPDR1")
                .build();
        EndPoint endPointOne = new EndPointBuilder()
                .setLocalId("SPDR-SA1-XPDR1")
                .setName(Map.of(nameOne.key(), nameOne))
                .build();

        Name nameTwo = new NameBuilder()
                .setValueName("OpenROADM node id")
                .setValue("SPDR-SC1-XPDR1")
                .build();
        EndPoint endPointTwo = new EndPointBuilder()
                .setLocalId("SPDR-SC1-XPDR1")
                .setName(Map.of(nameTwo.key(), nameTwo))
                .build();

        ConnectivityUtils connectivityUtils = new ConnectivityUtils(
                serviceDataStoreOperations,
                new HashMap<>(),
                tapiContext,
                networkTransactionService,
                new Uuid(TapiConstants.T0_FULL_MULTILAYER_UUID),
                topologyUtils
        );

        Network openroadmTopo = topologyUtils.readTopology(InstanceIdentifiers.OPENROADM_TOPOLOGY_II);

        assertEquals(ServiceFormat.OTU, connectivityUtils.getServiceFormatPhtnc(
                Collections.checkedList(List.of(endPointOne, endPointTwo), EndPoint.class),
                openroadmTopo
        ));
    }

    @Test
    public void getConnectionTypePhtncRoadm() throws TapiTopologyException {
        Name nameOne = new NameBuilder()
                .setValueName("OpenROADM node id")
                .setValue("ROADM-A1-SRG1")
                .build();
        EndPoint endPointOne = new EndPointBuilder()
                .setLocalId("ROADM-A1-SRG1")
                .setName(Map.of(nameOne.key(), nameOne))
                .build();

        Name nameTwo = new NameBuilder()
                .setValueName("OpenROADM node id")
                .setValue("ROADM-C1-SRG1")
                .build();
        EndPoint endPointTwo = new EndPointBuilder()
                .setLocalId("ROADM-C1-SRG1")
                .setName(Map.of(nameTwo.key(), nameTwo))
                .build();

        ConnectivityUtils connectivityUtils = new ConnectivityUtils(
                serviceDataStoreOperations,
                new HashMap<>(),
                tapiContext,
                networkTransactionService,
                new Uuid(TapiConstants.T0_FULL_MULTILAYER_UUID),
                topologyUtils
        );

        Network openroadmTopo = topologyUtils.readTopology(InstanceIdentifiers.OPENROADM_TOPOLOGY_II);

        assertEquals(ConnectionType.RoadmLine, connectivityUtils.getConnectionTypePhtnc(
                Collections.checkedList(List.of(endPointOne, endPointTwo), EndPoint.class),
                openroadmTopo
        ));
    }

    @Test
    public void getServiceFormatPhtncRoadm() throws TapiTopologyException {
        Name nameOne = new NameBuilder()
                .setValueName("OpenROADM node id")
                .setValue("ROADM-A1-SRG1")
                .build();
        EndPoint endPointOne = new EndPointBuilder()
                .setLocalId("ROADM-A1-SRG1")
                .setName(Map.of(nameOne.key(), nameOne))
                .build();

        Name nameTwo = new NameBuilder()
                .setValueName("OpenROADM node id")
                .setValue("ROADM-C1-SRG1")
                .build();
        EndPoint endPointTwo = new EndPointBuilder()
                .setLocalId("ROADM-C1-SRG1")
                .setName(Map.of(nameTwo.key(), nameTwo))
                .build();

        ConnectivityUtils connectivityUtils = new ConnectivityUtils(
                serviceDataStoreOperations,
                new HashMap<>(),
                tapiContext,
                networkTransactionService,
                new Uuid(TapiConstants.T0_FULL_MULTILAYER_UUID),
                topologyUtils
        );

        Network openroadmTopo = topologyUtils.readTopology(InstanceIdentifiers.OPENROADM_TOPOLOGY_II);

        assertEquals(ServiceFormat.OC, connectivityUtils.getServiceFormatPhtnc(
                Collections.checkedList(List.of(endPointOne, endPointTwo), EndPoint.class),
                openroadmTopo
        ));
    }

    @Test
    void testAtoZXponderType() throws TapiTopologyException {
        ServiceInterfacePoint serviceInterfacePoint = new ServiceInterfacePointBuilder()
                .setUuid(Uuid.getDefaultInstance("5efda776-f8de-3e0b-9bbd-2c702e210946"))
                .build();

        ConnectivityUtils connectivityUtils = new ConnectivityUtils(
                serviceDataStoreOperations,
                Map.of(serviceInterfacePoint.key(), serviceInterfacePoint),
                tapiContext,
                networkTransactionService,
                new Uuid(TapiConstants.T0_FULL_MULTILAYER_UUID),
                topologyUtils
        );

        Network openroadmTopo = topologyUtils.readTopology(InstanceIdentifiers.OPENROADM_TOPOLOGY_II);

        assertNotNull(connectivityUtils.getSipIdFromAend(
                getAToZRoadmKeyAToZMap(roadmPathElements()),
                "SPDR-SA1",
                ServiceFormat.ODU,
                openroadmTopo));
    }

    @Test
    void testZtoAXponderType() throws TapiTopologyException {
        ServiceInterfacePoint serviceInterfacePoint = new ServiceInterfacePointBuilder()
                .setUuid(Uuid.getDefaultInstance("5efda776-f8de-3e0b-9bbd-2c702e210946"))
                .build();

        ConnectivityUtils connectivityUtils = new ConnectivityUtils(
                serviceDataStoreOperations,
                Map.of(serviceInterfacePoint.key(), serviceInterfacePoint),
                tapiContext,
                networkTransactionService,
                new Uuid(TapiConstants.T0_FULL_MULTILAYER_UUID),
                topologyUtils
        );

        Network openroadmTopo = topologyUtils.readTopology(InstanceIdentifiers.OPENROADM_TOPOLOGY_II);

        assertNotNull(connectivityUtils.getSipIdFromZend(
                getZToARoadmKeyZToAMap(),
                "SPDR-SA1",
                ServiceFormat.ODU,
                openroadmTopo));
    }

    @Test
    void testAtoZRoadmType() throws TapiTopologyException {
        ServiceInterfacePoint serviceInterfacePoint = new ServiceInterfacePointBuilder()
                .setUuid(Uuid.getDefaultInstance("abbf1503-11aa-3618-8bbd-e33916678dd3"))
                .build();

        ConnectivityUtils connectivityUtils = new ConnectivityUtils(
                serviceDataStoreOperations,
                Map.of(serviceInterfacePoint.key(), serviceInterfacePoint),
                tapiContext,
                networkTransactionService,
                new Uuid(TapiConstants.T0_FULL_MULTILAYER_UUID),
                topologyUtils
        );

        Network openroadmTopo = topologyUtils.readTopology(InstanceIdentifiers.OPENROADM_TOPOLOGY_II);

        assertNotNull(connectivityUtils.getSipIdFromAend(
                getAToZRoadmKeyAToZMap(roadmPathElements()),
                "ROADM-A1",
                ServiceFormat.ODU,
                openroadmTopo));
    }

    @Test
    void testZtoARoadmType() throws TapiTopologyException {
        ServiceInterfacePoint serviceInterfacePoint = new ServiceInterfacePointBuilder()
                .setUuid(Uuid.getDefaultInstance("abbf1503-11aa-3618-8bbd-e33916678dd3"))
                .build();

        ConnectivityUtils connectivityUtils = new ConnectivityUtils(
                serviceDataStoreOperations,
                Map.of(serviceInterfacePoint.key(), serviceInterfacePoint),
                tapiContext,
                networkTransactionService,
                new Uuid(TapiConstants.T0_FULL_MULTILAYER_UUID),
                topologyUtils
        );

        Network openroadmTopo = topologyUtils.readTopology(InstanceIdentifiers.OPENROADM_TOPOLOGY_II);

        assertNotNull(connectivityUtils.getSipIdFromZend(
                getZToARoadmKeyZToAMap(),
                "ROADM-A1",
                ServiceFormat.ODU,
                openroadmTopo));
    }

    @Test
    void testRoadmToRoadmConnectivity() throws TapiTopologyException {
        Map<AToZKey, AToZ> atoZMap = getAToZRoadmKeyAToZMap(roadmPathElements());

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
                topologyUtils
        );
        IDCollection idCollection = connectivityUtils.extractTPandNodeIds(pathDescription,
                topologyUtils.readTopology(InstanceIdentifiers.OPENROADM_TOPOLOGY_II));

        List<String> expectedRoadmNodeList = List.of("ROADM-A1", "ROADM-C1");
        assertEquals(expectedRoadmNodeList, idCollection.rdmNodelist(), "ROADM node list mismatch");

        List<String> expectedRoadmDegreeList = List.of(
                "ROADM-A1+DEG2-TTP-TXRX",
                "ROADM-C1+DEG1-TTP-TXRX"
        );
        assertEquals(expectedRoadmDegreeList, idCollection.rdmDegTplist(), "ROADM degree list mismatch");

        List<String> expectedRoadmAddDropList = List.of(
                "ROADM-A1+SRG1-PP1-TXRX",
                "ROADM-C1+SRG1-PP1-TXRX"
        );
        assertEquals(expectedRoadmAddDropList, idCollection.rdmAddDropTplist(), "ROADM add/drop list mismatch");

        List<String> expectedXpdrNodeList = List.of(
                "SPDR-SA1-XPDR1",
                "SPDR-SC1-XPDR1"
        );
        assertEquals(expectedXpdrNodeList, idCollection.xpdrNodelist(), "XPDR node list mismatch");

        List<String> expectedXpdrNetworkList = List.of(
                "SPDR-SA1-XPDR1+XPDR1-NETWORK1",
                "SPDR-SC1-XPDR1+XPDR1-NETWORK1"
        );
        assertEquals(expectedXpdrNetworkList, idCollection.xpdrNetworkTplist(), "XPDR network list mismatch");

        List<String> expectedXpdrClientList = List.of(); // empty
        assertEquals(expectedXpdrClientList, idCollection.xpdrClientTplist(), "XPDR client list mismatch");
    }

    @Test
    void testXpdrToXpdrConnectivity() throws TapiTopologyException {
        // --- Build the PathDescription for this scenario ---
        Map<AToZKey, AToZ> atoZMap = getXpdrToXpdrAtoZMap();

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
                topologyUtils
        );

        IDCollection idCollection = connectivityUtils.extractTPandNodeIds(pathDescription,
                topologyUtils.readTopology(InstanceIdentifiers.OPENROADM_TOPOLOGY_II));

        // Assertions
        List<String> expectedRoadmNodeList = List.of();
        assertEquals(expectedRoadmNodeList, idCollection.rdmNodelist(), "ROADM node list mismatch");

        List<String> expectedRoadmDegreeList = List.of();
        assertEquals(expectedRoadmDegreeList, idCollection.rdmDegTplist(), "ROADM degree list mismatch");

        List<String> expectedRoadmAddDropList = List.of();
        assertEquals(expectedRoadmAddDropList, idCollection.rdmAddDropTplist(), "ROADM add/drop list mismatch");

        List<String> expectedXpdrNodeList = List.of("SPDR-SA1-XPDR1", "SPDR-SC1-XPDR1");
        assertEquals(expectedXpdrNodeList, idCollection.xpdrNodelist(), "XPDR node list mismatch");

        List<String> expectedXpdrNetworkList = List.of(
                "SPDR-SA1-XPDR1+XPDR1-NETWORK1",
                "SPDR-SC1-XPDR1+XPDR1-NETWORK1"
        );
        assertEquals(expectedXpdrNetworkList, idCollection.xpdrNetworkTplist(), "XPDR network list mismatch");

        List<String> expectedXpdrClientList = List.of();
        assertEquals(expectedXpdrClientList, idCollection.xpdrClientTplist(), "XPDR client list mismatch");
    }

    @Test
    void testXpdrToXpdrWithClientConnectivity() throws TapiTopologyException {
        Map<AToZKey, AToZ> atoZMap = getXpdrToXpdrClienmtAtoZMap();

        // Build the AToZDirection and PathDescription
        AToZDirectionBuilder atoZDirectionBuilder = new AToZDirectionBuilder()
                .setAToZ(atoZMap);

        PathDescription pathDescription = new PathDescriptionBuilder()
                .setAToZDirection(atoZDirectionBuilder.build())
                .build();

        // --- Create ConnectivityUtils instance ---
        ConnectivityUtils connectivityUtils = new ConnectivityUtils(
                serviceDataStoreOperations,
                new HashMap<>(),
                tapiContext,
                networkTransactionService,
                new Uuid(TapiConstants.T0_FULL_MULTILAYER_UUID),
                topologyUtils
        );

        IDCollection idCollection = connectivityUtils.extractTPandNodeIds(pathDescription,
                topologyUtils.readTopology(InstanceIdentifiers.OPENROADM_TOPOLOGY_II));

        List<String> expectedRoadmNodeList = List.of();
        assertEquals(expectedRoadmNodeList, idCollection.rdmNodelist(), "ROADM node list mismatch");

        List<String> expectedRoadmDegreeList = List.of();
        assertEquals(expectedRoadmDegreeList, idCollection.rdmDegTplist(), "ROADM degree list mismatch");

        List<String> expectedRoadmAddDropList = List.of();
        assertEquals(expectedRoadmAddDropList, idCollection.rdmAddDropTplist(), "ROADM add/drop list mismatch");

        List<String> expectedXpdrNodeList = List.of("SPDR-SA1-XPDR1", "SPDR-SC1-XPDR1");
        assertEquals(expectedXpdrNodeList, idCollection.xpdrNodelist(), "XPDR node list mismatch");

        List<String> expectedXpdrNetworkList = List.of(
                "SPDR-SA1-XPDR1+XPDR1-NETWORK1",
                "SPDR-SC1-XPDR1+XPDR1-NETWORK1");
        assertEquals(expectedXpdrNetworkList, idCollection.xpdrNetworkTplist(), "XPDR network list mismatch");

        List<String> expectedXpdrClientList = List.of(
                "SPDR-SA1-XPDR1+XPDR1-CLIENT1",
                "SPDR-SC1-XPDR1+XPDR1-CLIENT1");
        assertEquals(expectedXpdrClientList, idCollection.xpdrClientTplist(), "XPDR client list mismatch");
    }

    @Test
    void testPathDescriptionStartsWithXponder() {
        Map<AToZKey, AToZ> atoZMap = getAToZRoadmKeyAToZMap(roadmPathElements());

        // Build the AToZDirection and PathDescription
        AToZDirectionBuilder atoZDirectionBuilder = new AToZDirectionBuilder().setAToZ(atoZMap);

        PathDescription pathDescription = new PathDescriptionBuilder()
                .setAToZDirection(atoZDirectionBuilder.build())
                .build();

        // --- Create ConnectivityUtils instance ---
        ConnectivityUtils connectivityUtils = new ConnectivityUtils(
                serviceDataStoreOperations,
                new HashMap<>(),
                tapiContext,
                networkTransactionService,
                new Uuid(TapiConstants.T0_FULL_MULTILAYER_UUID),
                topologyUtils
        );

        assertFalse(connectivityUtils.pathStartsWithROADM(pathDescription));
    }

    @Test
    void testPathDescriptionStartsWithRoadm() throws TapiTopologyException {
        Map<AToZKey, AToZ> atoZMap = getAToZRoadmKeyAToZMap(startsWithRoadm());

        // Build the AToZDirection and PathDescription
        AToZDirectionBuilder atoZDirectionBuilder = new AToZDirectionBuilder().setAToZ(atoZMap);

        PathDescription pathDescription = new PathDescriptionBuilder()
                .setAToZDirection(atoZDirectionBuilder.build())
                .build();

        // --- Create ConnectivityUtils instance ---
        ConnectivityUtils connectivityUtils = new ConnectivityUtils(
                serviceDataStoreOperations,
                new HashMap<>(),
                tapiContext,
                networkTransactionService,
                new Uuid(TapiConstants.T0_FULL_MULTILAYER_UUID),
                topologyUtils
        );

        //@todo This should actually be true, since the first node in the path description is a ROADM in this scenario.
        assertFalse(connectivityUtils.pathStartsWithROADM(pathDescription));
    }

    private Map<AToZKey, AToZ> getAToZRoadmKeyAToZMap(List<PathElement> elements) {
        return buildAtoZMap(elements);
    }

    private Map<ZToAKey, ZToA> getZToARoadmKeyZToAMap() {
        return buildZtoAMap(roadmPathElements());
    }

    private Map<AToZKey, AToZ> getXpdrToXpdrAtoZMap() {
        List<PathElement> elements = List.of(
                tp("0", "", "SPDR-SA1-XPDR1"),
                node("1", "SPDR-SA1-XPDR1"),
                tp("2", "XPDR1-NETWORK1", "SPDR-SA1-XPDR1"),
                link("3", "OTU4-SPDR-SA1-XPDR1-XPDR1-NETWORK1toSPDR-SC1-XPDR1-XPDR1-NETWORK1"),
                tp("4", "XPDR1-NETWORK1", "SPDR-SC1-XPDR1"),
                node("5", "SPDR-SC1-XPDR1"),
                tp("6", "", "SPDR-SC1-XPDR1")
        );

        Map<AToZKey, AToZ> map = new HashMap<>();
        for (PathElement e : elements) {
            AToZ item = new AToZBuilder()
                    .setId(e.id)
                    .setResource(e.resource)
                    .build();
            map.put(item.key(), item);
        }
        return map;
    }

    private Map<AToZKey, AToZ> getXpdrToXpdrClienmtAtoZMap() {
        List<PathElement> elements = List.of(
                tp("0", "XPDR1-CLIENT1", "SPDR-SA1-XPDR1"),
                node("1", "SPDR-SA1-XPDR1"),
                tp("2", "XPDR1-NETWORK1", "SPDR-SA1-XPDR1"),
                link("3", "ODTU4-SPDR-SA1-XPDR1-XPDR1-NETWORK1toSPDR-SC1-XPDR1-XPDR1-NETWORK1"),
                tp("4", "XPDR1-NETWORK1", "SPDR-SC1-XPDR1"),
                node("5", "SPDR-SC1-XPDR1"),
                tp("6", "XPDR1-CLIENT1", "SPDR-SC1-XPDR1")
        );

        return buildAtoZMap(elements);
    }

    private Map<AToZKey, AToZ> buildAtoZMap(List<PathElement> elements) {
        Map<AToZKey, AToZ> map = new HashMap<>();
        for (PathElement e : elements) {
            AToZ item = new AToZBuilder().setId(e.id).setResource(e.resource).build();
            map.put(item.key(), item);
        }
        return map;
    }

    private Map<ZToAKey, ZToA> buildZtoAMap(List<PathElement> elements) {
        Map<ZToAKey, ZToA> map = new HashMap<>();
        for (PathElement e : elements) {
            ZToA item = new ZToABuilder().setId(e.id).setResource(e.resource).build();
            map.put(item.key(), item);
        }
        return map;
    }

    private record PathElement(
            String id,
            org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev230501
                    .pce.resource.Resource resource) {}

    private List<PathElement> roadmPathElements() {
        return List.of(
                tp("0", "", "SPDR-SA1-XPDR1"),
                node("1", "SPDR-SA1-XPDR1"),
                tp("2", "XPDR1-NETWORK1", "SPDR-SA1-XPDR1"),
                link("3", "SPDR-SA1-XPDR1-XPDR1-NETWORK1toROADM-A1-SRG1-SRG1-PP1-TXRX"),
                tp("4", "SRG1-PP1-TXRX", "ROADM-A1-SRG1"),
                node("5", "ROADM-A1-SRG1"),
                tp("6", "SRG1-CP-TXRX", "ROADM-A1-SRG1"),
                link("7", "ROADM-A1-SRG1-SRG1-CP-TXRXtoROADM-A1-DEG2-DEG2-CTP-TXRX"),
                tp("8", "DEG2-CTP-TXRX", "ROADM-A1-DEG2"),
                node("9", "ROADM-A1-DEG2"),
                tp("10", "DEG2-TTP-TXRX", "ROADM-A1-DEG2"),
                link("11", "ROADM-A1-DEG2-DEG2-TTP-TXRXtoROADM-C1-DEG1-DEG1-TTP-TXRX"),
                tp("12", "DEG1-TTP-TXRX", "ROADM-C1-DEG1"),
                node("13", "ROADM-C1-DEG1"),
                tp("14", "DEG1-CTP-TXRX", "ROADM-C1-DEG1"),
                link("15", "ROADM-C1-DEG1-DEG1-CTP-TXRXtoROADM-C1-SRG1-SRG1-CP-TXRX"),
                tp("16", "SRG1-CP-TXRX", "ROADM-C1-SRG1"),
                node("17", "ROADM-C1-SRG1"),
                tp("18", "SRG1-PP1-TXRX", "ROADM-C1-SRG1"),
                link("19", "ROADM-C1-SRG1-SRG1-PP1-TXRXtoSPDR-SC1-XPDR1-XPDR1-NETWORK1"),
                tp("20", "XPDR1-NETWORK1", "SPDR-SC1-XPDR1"),
                node("21", "SPDR-SC1-XPDR1"),
                tp("22", "", "SPDR-SC1-XPDR1")
        );
    }

    private List<PathElement> startsWithRoadm() {
        return List.of(
                tp("4", "SRG1-PP1-TXRX", "ROADM-A1-SRG1"),
                node("5", "ROADM-A1-SRG1"),
                tp("6", "SRG1-CP-TXRX", "ROADM-A1-SRG1"),
                link("7", "ROADM-A1-SRG1-SRG1-CP-TXRXtoROADM-A1-DEG2-DEG2-CTP-TXRX"),
                tp("8", "DEG2-CTP-TXRX", "ROADM-A1-DEG2"),
                node("9", "ROADM-A1-DEG2"),
                tp("10", "DEG2-TTP-TXRX", "ROADM-A1-DEG2"),
                link("11", "ROADM-A1-DEG2-DEG2-TTP-TXRXtoROADM-C1-DEG1-DEG1-TTP-TXRX"),
                tp("12", "DEG1-TTP-TXRX", "ROADM-C1-DEG1"),
                node("13", "ROADM-C1-DEG1"),
                tp("14", "DEG1-CTP-TXRX", "ROADM-C1-DEG1"),
                link("15", "ROADM-C1-DEG1-DEG1-CTP-TXRXtoROADM-C1-SRG1-SRG1-CP-TXRX"),
                tp("16", "SRG1-CP-TXRX", "ROADM-C1-SRG1"),
                node("17", "ROADM-C1-SRG1"),
                tp("18", "SRG1-PP1-TXRX", "ROADM-C1-SRG1"),
                link("19", "ROADM-C1-SRG1-SRG1-PP1-TXRXtoSPDR-SC1-XPDR1-XPDR1-NETWORK1"),
                tp("20", "XPDR1-NETWORK1", "SPDR-SC1-XPDR1"),
                node("21", "SPDR-SC1-XPDR1"),
                tp("22", "", "SPDR-SC1-XPDR1")
        );
    }

    private PathElement node(String id, String nodeId) {
        return new PathElement(id, new ResourceBuilder()
                .setResource(new NodeBuilder().setNodeId(nodeId).build())
                .setState(State.InService)
                .build());
    }

    private PathElement tp(String id, String tpId, String tpNodeId) {
        return new PathElement(id, new ResourceBuilder()
                .setResource(new TerminationPointBuilder()
                        .setTpId(tpId)
                        .setTpNodeId(tpNodeId)
                        .build())
                .setState(State.InService)
                .build());
    }

    private PathElement link(String id, String linkId) {
        return new PathElement(id, new ResourceBuilder()
                .setResource(new LinkBuilder().setLinkId(linkId).build())
                .setState(State.InService)
                .build());
    }
}
