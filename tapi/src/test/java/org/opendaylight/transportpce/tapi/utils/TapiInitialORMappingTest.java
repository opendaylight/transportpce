/*
 * Copyright © 2026 Smartoptics and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.tapi.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
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
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev221121.connectivity.context.ConnectivityService;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev221121.connectivity.context.ConnectivityServiceBuilder;

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
    void sortByServiceFormat() {
        Services one = createService("service 1", ServiceFormat.ODU);
        Services two = createService("service 2", ServiceFormat.OTU);
        Services three = createService("service 3", ServiceFormat.Ethernet);
        Services four = createService("service 4", ServiceFormat.ODU);

        List<Services> services = TapiInitialORMapping.sortByServiceFormat(serviceList(one, two, three, four));

        assertEquals(List.of(two, one, four, three), services);
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

    private Services createService(String serviceName, ServiceFormat serviceFormat) {
        return new ServicesBuilder()
                .setServiceAEnd(
                        new ServiceAEndBuilder()
                                .setServiceFormat(serviceFormat)
                                .build())
                .setServiceName(serviceName)
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

    private static Uuid uuidOf(String serviceName) {
        return new Uuid(UUID.nameUUIDFromBytes(serviceName.getBytes(StandardCharsets.UTF_8)).toString());
    }
}
