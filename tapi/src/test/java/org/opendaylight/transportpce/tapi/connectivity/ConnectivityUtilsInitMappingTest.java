/*
 * Copyright (c) 2026 OpenTDT and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.tapi.connectivity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.format.rev250530.ServiceFormat;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.global._class.Name;

/**
 * Initial mapping of services that ride on other services (supporting-service-name).
 */
class ConnectivityUtilsInitMappingTest extends AbstractInitialMappingTest {

    @Test
    void serviceRidingOnAServiceMappedBeforeItIsMapped() throws Exception {
        // The ODU is mapped after the OTU it rides on, so it is looked up in a connection-vs-service
        // map that already holds the OTU but not the ODU itself.
        assertTrue(storeAndMap(List.of(
            stackService("1", "OTUC4", ServiceFormat.OTU, otuPath("1"), null, "SPDR-SA1", "SPDR-SC1"),
            stackService("1", "ODUC4", ServiceFormat.ODU, oduPath("1", "SPDR-SA1", "SPDR-SC1"),
                serviceName("1", "OTUC4"), "SPDR-SA1", "SPDR-SC1"))));

        assertEquals(Set.of(serviceName("1", "OTUC4"), serviceName("1", "ODUC4")), mappedServiceNames());
    }

    @Test
    void serviceWhoseSupportingServiceIsMissingIsMapped() throws Exception {
        // The service it rides on is not in the OpenROADM service list (any more).
        assertTrue(storeAndMap(List.of(
            stackService("1", "ODUC4", ServiceFormat.ODU, oduPath("1", "SPDR-SA1", "SPDR-SC1"),
                "deleted-OTUC4", "SPDR-SA1", "SPDR-SC1"))));

        assertEquals(Set.of(serviceName("1", "ODUC4")), mappedServiceNames());
    }

    @Test
    void serviceMissingFromTheServiceListIsMapped() throws Exception {
        // The ODU rides on the OTU but is not (any more) in the OpenROADM service list, so its own
        // supporting services cannot be read: it is mapped without them. (The datastore is shared by
        // the tests of this class, hence a service name no other test stores.)
        assertTrue(storeAndMap(
            List.of(stackService("1", "OTUC4", ServiceFormat.OTU, otuPath("1"), null, "SPDR-SA1", "SPDR-SC1")),
            List.of(stackService("1", "unstored-ODUC4", ServiceFormat.ODU, oduPath("1", "SPDR-SA1", "SPDR-SC1"),
                serviceName("1", "OTUC4"), "SPDR-SA1", "SPDR-SC1"))));

        assertEquals(Set.of(serviceName("1", "OTUC4"), serviceName("1", "unstored-ODUC4")), mappedServiceNames());
    }

    private Set<String> mappedServiceNames() {
        return mappedConnectivityServices().values().stream()
            .flatMap(serv -> serv.nonnullName().values().stream())
            .map(Name::getValue)
            .collect(Collectors.toSet());
    }
}
