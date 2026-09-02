/*
 * Copyright © 2026 Smartoptics and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.tapi.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.opendaylight.transportpce.tapi.connectivity.ConnectivityUtils;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev250530.service.ServiceAEndBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.format.rev250530.ServiceFormat;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev250530.ServiceList;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev250530.ServiceListBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev250530.service.list.Services;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev250530.service.list.ServicesBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev250530.service.list.ServicesKey;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.Uuid;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.global._class.Name;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.global._class.NameBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev221121.connectivity.context.Connection;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev221121.connectivity.context.ConnectionBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev221121.connectivity.context.ConnectionKey;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev221121.connectivity.context.ConnectivityService;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev221121.connectivity.context.ConnectivityServiceBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev221121.connectivity.context.ConnectivityServiceKey;

class TapiInitialORMappingTest {

    @Test
    void performServInitialMapping() {
        Services one = createService("service 1", ServiceFormat.ODU);
        Services two = createService("service 2", ServiceFormat.OTU);
        Services three = createService("service 3", ServiceFormat.Ethernet);
        Services four = createService("service 4", ServiceFormat.ODU);

        ConnectivityUtils connectivityUtils = mock(ConnectivityUtils.class);
        Mockito.when(connectivityUtils.mapORServiceToTapiConnectivity(one))
                .thenReturn(connectivityService(one.getServiceName()));

        Mockito.when(connectivityUtils.mapORServiceToTapiConnectivity(two))
                .thenReturn(connectivityService(two.getServiceName()));

        Mockito.when(connectivityUtils.mapORServiceToTapiConnectivity(three))
                .thenReturn(connectivityService(three.getServiceName()));

        Mockito.when(connectivityUtils.mapORServiceToTapiConnectivity(four))
                .thenReturn(connectivityService(four.getServiceName()));

        TapiContext tapi = mock(TapiContext.class);

        TapiInitialORMapping tapiInitialORMapping = new TapiInitialORMapping(null, connectivityUtils, tapi, null);

        ServiceList serviceList = serviceList(one, two, three, four);

        assertTrue(tapiInitialORMapping.performServInitialMapping(serviceList));

        Mockito.verify(connectivityUtils, Mockito.times(4)).mapORServiceToTapiConnectivity(Mockito.any());
        Mockito.verify(tapi, Mockito.times(1)).updateConnectivityContext(Mockito.any(), Mockito.any());
    }

    @Test
    void performServInitialMappingKeepsTheConnectionsOfEveryService() {
        Services one = createService("service 1", ServiceFormat.ODU);
        Services two = createService("service 2", ServiceFormat.OTU);
        Services three = createService("service 3", ServiceFormat.Ethernet);
        Services four = createService("service 4", ServiceFormat.ODU);

        ConnectivityUtils connectivityUtils = mock(ConnectivityUtils.class);

        for (Services service : List.of(one, two, three, four)) {
            Mockito.when(connectivityUtils.mapORServiceToTapiConnectivity(service))
                    .thenReturn(connectivityService(service.getServiceName()));
        }

        // ConnectivityUtils clears its connection map on every service it maps.
        Mockito.when(connectivityUtils.getConnectionFullMap())
                .thenReturn(connections("connection 2"))
                .thenReturn(connections("connection 1"))
                .thenReturn(connections("connection 4"))
                .thenReturn(connections("connection 3"));

        TapiContext tapi = mock(TapiContext.class);

        new TapiInitialORMapping(null, connectivityUtils, tapi, null)
                .performServInitialMapping(serviceList(one, two, three, four));

        ArgumentCaptor<Map<ConnectionKey, Connection>> connectionMap = ArgumentCaptor.captor();
        Mockito.verify(tapi).updateConnectivityContext(Mockito.any(), connectionMap.capture());

        assertEquals(
                Set.of(
                        connectionOf("connection 1").key(),
                        connectionOf("connection 2").key(),
                        connectionOf("connection 3").key(),
                        connectionOf("connection 4").key()),
                connectionMap.getValue().keySet());
    }

    @Test
    void performServInitialMappingReportsAnUnmappedService() {
        Services one = createService("service 1", ServiceFormat.ODU);
        Services two = createService("service 2", ServiceFormat.OTU);

        ConnectivityUtils connectivityUtils = mock(ConnectivityUtils.class);
        Mockito.when(connectivityUtils.mapORServiceToTapiConnectivity(one))
                .thenReturn(connectivityService(one.getServiceName()));

        // No service path in the datastore for this service.
        Mockito.when(connectivityUtils.mapORServiceToTapiConnectivity(two))
                .thenReturn(null);

        TapiContext tapi = mock(TapiContext.class);

        assertFalse(
                new TapiInitialORMapping(null, connectivityUtils, tapi, null)
                        .performServInitialMapping(serviceList(one, two)));

        // The service that could be mapped is still written.
        ArgumentCaptor<Map<ConnectivityServiceKey, ConnectivityService>> connServMap = ArgumentCaptor.captor();
        Mockito.verify(tapi).updateConnectivityContext(connServMap.capture(), Mockito.any());

        assertEquals(
                Set.of(connectivityService(one.getServiceName()).key()),
                connServMap.getValue().keySet());
    }

    @Test
    void performServInitialMappingWritesNothingWhenNoServiceCouldBeMapped() {
        Services one = createService("service 1", ServiceFormat.ODU);
        Services two = createService("service 2", ServiceFormat.OTU);

        // No service path in the datastore for either service.
        ConnectivityUtils connectivityUtils = mock(ConnectivityUtils.class);
        Mockito.when(connectivityUtils.mapORServiceToTapiConnectivity(Mockito.any()))
                .thenReturn(null);

        TapiContext tapi = mock(TapiContext.class);

        assertFalse(
                new TapiInitialORMapping(null, connectivityUtils, tapi, null)
                        .performServInitialMapping(serviceList(one, two)));

        // An empty connectivity context is nothing to write, and writing it throws.
        Mockito.verify(tapi, Mockito.never()).updateConnectivityContext(Mockito.any(), Mockito.any());
    }

    @Test
    void sortBySupportingServicesFallsBackToTheServiceFormat() {
        Services one = createService("service 1", ServiceFormat.ODU);
        Services two = createService("service 2", ServiceFormat.OTU);
        Services three = createService("service 3", ServiceFormat.Ethernet);
        Services four = createService("service 4", ServiceFormat.ODU);

        List<Services> services = TapiInitialORMapping.sortBySupportingServices(serviceList(one, two, three, four));

        assertEquals(List.of(two, one, four, three), services);
    }

    @Test
    void sortBySupportingServicesKeepsEachStackTogether() {
        Services otuA = createService("otu a", ServiceFormat.OTU);
        Services oduA = createService("odu a", ServiceFormat.ODU, "otu a");
        Services dsrA = createService("dsr a", ServiceFormat.Ethernet, "odu a");
        Services otuB = createService("otu b", ServiceFormat.OTU);
        Services oduB = createService("odu b", ServiceFormat.ODU, "otu b");
        Services dsrB = createService("dsr b", ServiceFormat.Ethernet, "odu b");

        List<Services> services = TapiInitialORMapping.sortBySupportingServices(
                serviceList(dsrB, oduB, otuB, dsrA, oduA, otuA));

        assertEquals(List.of(otuA, oduA, dsrA, otuB, oduB, dsrB), services);
    }

    @Test
    void sortBySupportingServicesIgnoresASupportingServiceOutsideTheBatch() {
        // The single service mapping maps one service without the services it rides on.
        Services dsr = createService("dsr a", ServiceFormat.Ethernet, "odu a");

        assertEquals(List.of(dsr), TapiInitialORMapping.sortBySupportingServices(serviceList(dsr)));
    }

    @Test
    void sortBySupportingServicesMapsTheServicesOfACycleAnyway() {
        Services one = createService("service 1", ServiceFormat.OTU, "service 2");
        Services two = createService("service 2", ServiceFormat.ODU, "service 1");
        Services three = createService("service 3", ServiceFormat.OTU);

        List<Services> services = TapiInitialORMapping.sortBySupportingServices(serviceList(one, two, three));

        assertEquals(List.of(three, one, two), services);
    }

    private ServiceList serviceList(Services... services) {
        Map<ServicesKey, Services> servicesMap = new HashMap<>(services.length);

        for (Services service : services) {
            servicesMap.put(service.key(), service);
        }

        return new ServiceListBuilder()
                .setServices(servicesMap)
                .build();
    }

    private Services createService(String serviceName, ServiceFormat serviceFormat, String... supportingServices) {
        return new ServicesBuilder()
                .setServiceAEnd(
                        new ServiceAEndBuilder()
                                .setServiceFormat(serviceFormat)
                                .build())
                .setServiceName(serviceName)
                // A service that rides on nothing leaves supporting-service-name unset, as OpenROADM does.
                .setSupportingServiceName(supportingServices.length == 0 ? null : Set.of(supportingServices))
                .build();
    }

    private ConnectivityService connectivityService(String serviceName) {
        Name name = new NameBuilder()
                .setValueName("A service name")
                .setValue(serviceName)
                .build();

        return new ConnectivityServiceBuilder()
                .setUuid(uuidOf(serviceName))
                .setName(Map.of(name.key(), name))
                .build();
    }

    private Map<ConnectionKey, Connection> connections(String... connectionNames) {
        Map<ConnectionKey, Connection> connectionMap = new HashMap<>(connectionNames.length);

        for (String connectionName : connectionNames) {
            Connection connection = connectionOf(connectionName);
            connectionMap.put(connection.key(), connection);
        }

        return connectionMap;
    }

    private Connection connectionOf(String connectionName) {
        return new ConnectionBuilder()
                .setUuid(uuidOf(connectionName))
                .build();
    }

    private static Uuid uuidOf(String serviceName) {
        return new Uuid(UUID.nameUUIDFromBytes(serviceName.getBytes(StandardCharsets.UTF_8)).toString());
    }
}
