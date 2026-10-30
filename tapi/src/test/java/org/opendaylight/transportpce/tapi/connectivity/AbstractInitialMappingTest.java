/*
 * Copyright (c) 2026 OpenTDT and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.tapi.connectivity;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import org.junit.jupiter.api.BeforeEach;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.transportpce.common.InstanceIdentifiers;
import org.opendaylight.transportpce.common.network.NetworkTransactionImpl;
import org.opendaylight.transportpce.common.network.NetworkTransactionService;
import org.opendaylight.transportpce.servicehandler.service.ServiceDataStoreOperations;
import org.opendaylight.transportpce.tapi.TapiConstants;
import org.opendaylight.transportpce.tapi.topology.TopologyUtils;
import org.opendaylight.transportpce.tapi.utils.TapiContext;
import org.opendaylight.transportpce.tapi.utils.TapiInitialORMapping;
import org.opendaylight.transportpce.tapi.utils.TapiLinkImpl;
import org.opendaylight.transportpce.tapi.utils.TapiTopologyDataUtils;
import org.opendaylight.transportpce.test.AbstractTest;
import org.opendaylight.transportpce.test.utils.TopologyDataUtils;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.node.types.rev210528.NodeIdType;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.optical.channel.types.rev250328.FrequencyTHz;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev250530.ConnectionType;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev250530.service.ServiceAEndBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev250530.service.ServiceZEndBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev250530.service.endpoint.TxDirection;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev250530.service.endpoint.TxDirectionBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev250530.service.endpoint.TxDirectionKey;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev250530.service.port.PortBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.format.rev250530.ServiceFormat;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev250530.ServiceList;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev250530.ServiceListBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev250530.service.list.Services;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev250530.service.list.ServicesBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev250530.service.list.ServicesKey;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev260422.path.description.AToZDirectionBuilder;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev260422.path.description.ZToADirectionBuilder;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev260422.path.description.atoz.direction.AToZ;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev260422.path.description.atoz.direction.AToZBuilder;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev260422.path.description.atoz.direction.AToZKey;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev260422.path.description.ztoa.direction.ZToA;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev260422.path.description.ztoa.direction.ZToABuilder;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev260422.path.description.ztoa.direction.ZToAKey;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev260422.pce.resource.ResourceBuilder;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev260422.pce.resource.resource.Resource;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev260422.pce.resource.resource.resource.NodeBuilder;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev260422.pce.resource.resource.resource.TerminationPointBuilder;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.service.types.rev220118.service.path.PathDescription;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.service.types.rev220118.service.path.PathDescriptionBuilder;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.servicepath.rev171017.service.path.list.ServicePaths;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.servicepath.rev171017.service.path.list.ServicePathsBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.Uuid;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev221121.Context1;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev221121.connectivity.context.Connection;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev221121.connectivity.context.ConnectionKey;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev221121.connectivity.context.ConnectivityService;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev221121.connectivity.context.ConnectivityServiceKey;
import org.opendaylight.yangtools.binding.DataObjectIdentifier;
import org.opendaylight.yangtools.yang.common.Decimal64;
import org.opendaylight.yangtools.yang.common.Uint32;
import org.opendaylight.yangtools.yang.common.Uint8;

/**
 * Maps OpenROADM services to TAPI at initialization, with a real {@link ConnectivityUtils}, on the
 * topology of the test resources. Builds the services of OTN stacks (OTU, ODU on the OTU, Ethernet on
 * the ODU) between the muxponders SPDR-SA1-XPDRn and SPDR-SC1-XPDRn, on their own ROADM add/drop
 * ports and spectrum per stack.
 */
abstract class AbstractInitialMappingTest extends AbstractTest {

    private final Map<String, ServicePaths> servicePaths = new HashMap<>();
    private NetworkTransactionService networkTransactionService;
    private ServiceDataStoreOperations serviceDataStoreOperations;
    private TapiContext tapiContext;
    private TapiInitialORMapping tapiInitialORMapping;

    @BeforeEach
    void setUpInitialMapping() throws InterruptedException, ExecutionException {
        TopologyDataUtils.writeTopologyFromFileToDatastore(getDataStoreContextUtil(),
            TapiTopologyDataUtils.OPENROADM_TOPOLOGY_FILE, InstanceIdentifiers.OPENROADM_TOPOLOGY_II);
        TopologyDataUtils.writeTopologyFromFileToDatastore(getDataStoreContextUtil(),
            TapiTopologyDataUtils.OPENROADM_NETWORK_FILE, InstanceIdentifiers.OPENROADM_NETWORK_II);
        TopologyDataUtils.writeTopologyFromFileToDatastore(getDataStoreContextUtil(),
            TapiTopologyDataUtils.OTN_TOPOLOGY_FILE, InstanceIdentifiers.OTN_NETWORK_II);
        TopologyDataUtils.writePortmappingFromFileToDatastore(getDataStoreContextUtil(),
            TapiTopologyDataUtils.PORTMAPPING_FILE);
        networkTransactionService = new NetworkTransactionImpl(getDataBroker());
        tapiContext = new TapiContext(networkTransactionService);
        serviceDataStoreOperations = mock(ServiceDataStoreOperations.class);
        when(serviceDataStoreOperations.getServicePath(anyString()))
            .thenAnswer(inv -> Optional.ofNullable(servicePaths.get(inv.<String>getArgument(0))));
        TopologyUtils topologyUtils = new TopologyUtils(networkTransactionService, getDataBroker(),
            new TapiLinkImpl(networkTransactionService, tapiContext));
        ConnectivityUtils connectivityUtils = new ConnectivityUtils(serviceDataStoreOperations, new HashMap<>(),
            tapiContext, networkTransactionService, new Uuid(TapiConstants.T0_FULL_MULTILAYER_UUID), topologyUtils);
        tapiInitialORMapping = new TapiInitialORMapping(topologyUtils, connectivityUtils, tapiContext,
            serviceDataStoreOperations);
        tapiInitialORMapping.performTopoInitialMapping();
    }

    /**
     * Stores the services in the OpenROADM service list and maps them all to TAPI, as the TAPI
     * module does when it starts.
     *
     * @param services the services, in the order the service list returns them
     * @return whether every service was mapped
     */
    boolean storeAndMap(List<Services> services) throws ExecutionException, InterruptedException {
        return storeAndMap(services, List.of());
    }

    /**
     * Stores the services in the OpenROADM service list and maps them, together with services that
     * are offered for mapping but are not in the service list (e.g. deleted in the meantime).
     *
     * @param services the services to store, in the order the service list returns them
     * @param unstoredServices the services mapped after them without being stored
     * @return whether every service was mapped
     */
    boolean storeAndMap(List<Services> services, List<Services> unstoredServices)
            throws ExecutionException, InterruptedException {
        Map<ServicesKey, Services> serviceMap = new LinkedHashMap<>();
        for (Services serv : services) {
            networkTransactionService.put(LogicalDatastoreType.OPERATIONAL,
                DataObjectIdentifier.builder(ServiceList.class).child(Services.class, serv.key()).build(), serv);
            serviceMap.put(serv.key(), serv);
        }
        networkTransactionService.commit().get();
        for (Services serv : unstoredServices) {
            serviceMap.put(serv.key(), serv);
        }
        when(serviceDataStoreOperations.getServices())
            .thenReturn(Optional.of(new ServiceListBuilder().setServices(serviceMap).build()));
        return tapiInitialORMapping.performServInitialMapping();
    }

    Map<ConnectivityServiceKey, ConnectivityService> mappedConnectivityServices() {
        return Optional.ofNullable(tapiContext.getConnectivityServices()).orElse(Map.of());
    }

    Map<ConnectionKey, Connection> mappedConnections() {
        return tapiContext.getTapiContext().augmentation(Context1.class).getConnectivityContext()
            .nonnullConnection();
    }

    /**
     * Builds one service of the stack between SPDR-SA1-XPDRn and SPDR-SC1-XPDRn, and its service path.
     *
     * @param xpdrNum the muxponder number n, which identifies the stack
     * @param layer the layer, part of the service name
     * @param format the service format
     * @param path the path description of the service
     * @param supportingService the service it rides on, or null
     * @param sideA the node of the A end, SPDR-SA1 or SPDR-SC1
     * @param sideZ the node of the Z end
     * @return the service
     */
    Services stackService(String xpdrNum, String layer, ServiceFormat format, PathDescription path,
            String supportingService, String sideA, String sideZ) {
        String name = serviceName(xpdrNum, layer);
        String port = format == ServiceFormat.Ethernet ? "CLIENT1" : "NETWORK1";
        servicePaths.put(name, new ServicePathsBuilder().setServicePathName(name).setPathDescription(path).build());
        return new ServicesBuilder()
            .setServiceName(name)
            .setConnectionType(ConnectionType.Service)
            .setServiceAEnd(new ServiceAEndBuilder().setServiceFormat(format).setClli("NodeSA")
                .setNodeId(new NodeIdType(sideA)).setServiceRate(Uint32.valueOf(100))
                .setTxDirection(txDirection(sideA + "-XPDR" + xpdrNum, "XPDR" + xpdrNum + "-" + port)).build())
            .setServiceZEnd(new ServiceZEndBuilder().setServiceFormat(format).setClli("NodeSC")
                .setNodeId(new NodeIdType(sideZ)).setServiceRate(Uint32.valueOf(100))
                .setTxDirection(txDirection(sideZ + "-XPDR" + xpdrNum, "XPDR" + xpdrNum + "-" + port)).build())
            .setSupportingServiceName(supportingService == null ? null : Set.of(supportingService))
            .build();
    }

    static String serviceName(String xpdrNum, String layer) {
        return "stack" + xpdrNum + "-" + layer;
    }

    private static Map<TxDirectionKey, TxDirection> txDirection(String device, String port) {
        TxDirection txDirection = new TxDirectionBuilder()
            .setIndex(Uint8.ZERO)
            .setPort(new PortBuilder().setPortDeviceName(device).setPortName(port).build())
            .build();
        return Map.of(txDirection.key(), txDirection);
    }

    // ConnectivityUtils builds the connections from the A to Z direction of the path description, and
    // reads its first elements to find the service interface points.
    static PathDescription otuPath(String xpdrNum) {
        String pp = "SRG1-PP" + xpdrNum + "-TXRX";
        return path(xpdrNum, List.of(
            tp("SPDR-SA1-XPDR" + xpdrNum, "XPDR" + xpdrNum + "-NETWORK1"), node("SPDR-SA1-XPDR" + xpdrNum),
            tp("SPDR-SA1-XPDR" + xpdrNum, "XPDR" + xpdrNum + "-NETWORK1"),
            tp("ROADM-A1-SRG1", pp), node("ROADM-A1-SRG1"), tp("ROADM-A1-DEG2", "DEG2-TTP-TXRX"),
            tp("ROADM-C1-DEG1", "DEG1-TTP-TXRX"), node("ROADM-C1-DEG1"), tp("ROADM-C1-SRG1", pp),
            tp("SPDR-SC1-XPDR" + xpdrNum, "XPDR" + xpdrNum + "-NETWORK1"), node("SPDR-SC1-XPDR" + xpdrNum)),
            zEnd(xpdrNum, "SPDR-SC1", "NETWORK1"));
    }

    static PathDescription oduPath(String xpdrNum, String sideA, String sideZ) {
        return path(xpdrNum, List.of(
            tp(sideA + "-XPDR" + xpdrNum, "XPDR" + xpdrNum + "-NETWORK1"), node(sideA + "-XPDR" + xpdrNum),
            tp(sideA + "-XPDR" + xpdrNum, "XPDR" + xpdrNum + "-NETWORK1"),
            tp(sideZ + "-XPDR" + xpdrNum, "XPDR" + xpdrNum + "-NETWORK1"), node(sideZ + "-XPDR" + xpdrNum)),
            zEnd(xpdrNum, sideZ, "NETWORK1"));
    }

    static PathDescription dsrPath(String xpdrNum, String sideA, String sideZ) {
        return path(xpdrNum, List.of(
            tp(sideA + "-XPDR" + xpdrNum, "XPDR" + xpdrNum + "-CLIENT1"), node(sideA + "-XPDR" + xpdrNum),
            tp(sideA + "-XPDR" + xpdrNum, "XPDR" + xpdrNum + "-NETWORK1"),
            tp(sideZ + "-XPDR" + xpdrNum, "XPDR" + xpdrNum + "-NETWORK1"), node(sideZ + "-XPDR" + xpdrNum),
            tp(sideZ + "-XPDR" + xpdrNum, "XPDR" + xpdrNum + "-CLIENT1")),
            zEnd(xpdrNum, sideZ, "CLIENT1"));
    }

    private static List<Resource> zEnd(String xpdrNum, String sideZ, String firstPort) {
        return List.of(tp(sideZ + "-XPDR" + xpdrNum, "XPDR" + xpdrNum + "-" + firstPort),
            node(sideZ + "-XPDR" + xpdrNum),
            tp(sideZ + "-XPDR" + xpdrNum, "XPDR" + xpdrNum + "-NETWORK1"));
    }

    private static PathDescription path(String xpdrNum, List<Resource> atozResources,
            List<Resource> ztoaResources) {
        // A distinct 75 GHz band per stack.
        FrequencyTHz min = new FrequencyTHz(
            Decimal64.valueOf("1".equals(xpdrNum) ? "193.0625" : "193.1625").scaleTo(8));
        FrequencyTHz max = new FrequencyTHz(
            Decimal64.valueOf("1".equals(xpdrNum) ? "193.1375" : "193.2375").scaleTo(8));
        Map<AToZKey, AToZ> atoz = new HashMap<>();
        for (int i = 0; i < atozResources.size(); i++) {
            AToZ elem = new AToZBuilder().setId(String.valueOf(i))
                .setResource(new ResourceBuilder().setResource(atozResources.get(i)).build()).build();
            atoz.put(elem.key(), elem);
        }
        Map<ZToAKey, ZToA> ztoa = new HashMap<>();
        for (int i = 0; i < ztoaResources.size(); i++) {
            ZToA elem = new ZToABuilder().setId(String.valueOf(i))
                .setResource(new ResourceBuilder().setResource(ztoaResources.get(i)).build()).build();
            ztoa.put(elem.key(), elem);
        }
        return new PathDescriptionBuilder()
            .setAToZDirection(new AToZDirectionBuilder().setAToZ(atoz)
                .setAToZMinFrequency(min).setAToZMaxFrequency(max).build())
            .setZToADirection(new ZToADirectionBuilder().setZToA(ztoa)
                .setZToAMinFrequency(min).setZToAMaxFrequency(max).build())
            .build();
    }

    private static Resource tp(String nodeId, String tpId) {
        return new TerminationPointBuilder().setTpNodeId(nodeId).setTpId(tpId).build();
    }

    private static Resource node(String nodeId) {
        return new NodeBuilder().setNodeId(nodeId).build();
    }
}
