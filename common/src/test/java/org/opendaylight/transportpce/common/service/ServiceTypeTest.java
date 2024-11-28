/*
 * Copyright © 2021 Orange.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.common.service;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev240315.mapping.Mapping;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev240315.mapping.MappingBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.types.rev191129.PortQual;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.types.rev191129.XpdrNodeTypes;
import org.opendaylight.yangtools.yang.common.Uint32;

public class ServiceTypeTest {

    @Test
    void getServiceTypeForServiceFormatUnknownTest() {
        String serviceType = ServiceTypes.getServiceType("toto", null, null);
        assertNull(serviceType, "service-type should be null");
    }

    @Test
    void getServiceTypeForServiceFormatOCTest() {
        String serviceType = ServiceTypes.getServiceType("OC", Uint32.valueOf(100), null);
        assertEquals("100GEt", serviceType, "service-type should be 100GEt");
        serviceType = ServiceTypes.getServiceType("OC", null, null);
        assertNull(serviceType, "service-type should be null");
    }

    @Test
    void getServiceTypeForServiceFormatEthernetTest() {
        String serviceType = ServiceTypes.getServiceType("Ethernet", Uint32.valueOf(400), null);
        assertEquals("400GE", serviceType, "service-type should be 400GE");
        serviceType = ServiceTypes.getServiceType("Ethernet", Uint32.valueOf(100), null);
        assertEquals("100GEt", serviceType, "service-type should be 100GEt");
        Mapping mapping = new MappingBuilder()
            .setLogicalConnectionPoint("logicalConnectionPoint")
            .setPortQual(PortQual.XpdrClient.getName())
            .build();
        serviceType = ServiceTypes.getServiceType("Ethernet", Uint32.valueOf(100), mapping);
        assertEquals("100GEt", serviceType, "service-type should be 100GEt");

        mapping = new MappingBuilder()
            .setLogicalConnectionPoint("logicalConnectionPoint")
            .setPortQual(PortQual.SwitchClient.getName())
            .build();
        serviceType = ServiceTypes.getServiceType("Ethernet", Uint32.valueOf(100), mapping);
        assertEquals("100GEm", serviceType, "service-type should be 100GEm");
        serviceType = ServiceTypes.getServiceType("Ethernet", Uint32.valueOf(10), mapping);
        assertEquals("10GE", serviceType, "service-type should be 10GE");
        serviceType = ServiceTypes.getServiceType("Ethernet", Uint32.valueOf(1), mapping);
        assertEquals("1GE", serviceType, "service-type should be 1GE");

        mapping = new MappingBuilder()
            .setLogicalConnectionPoint("logicalConnectionPoint")
            .setPortQual(PortQual.SwitchClient.getName())
            .setXpdrType(XpdrNodeTypes.Switch)
            .build();
        serviceType = ServiceTypes.getServiceType("Ethernet", Uint32.valueOf(100), mapping);
        assertEquals("100GEs", serviceType, "service-type should be 100GEs");
    }

    @Test
    void getOtnServiceTypeForServiceFormatEthernetTest() {
        String serviceType = ServiceTypes.getOtnServiceType("toto", Uint32.valueOf(123));
        assertNull(serviceType, "service-type should be null");
        serviceType = ServiceTypes.getOtnServiceType("Ethernet", Uint32.valueOf(123));
        assertNull(serviceType, "service-type should be null");
        serviceType = ServiceTypes.getOtnServiceType("Ethernet", Uint32.valueOf(1));
        assertEquals("1GE", serviceType, "service-type should be 1GE");
        serviceType = ServiceTypes.getOtnServiceType("Ethernet", Uint32.valueOf(10));
        assertEquals("10GE", serviceType, "service-type should be 10GE");
        serviceType = ServiceTypes.getOtnServiceType("Ethernet", Uint32.valueOf(100));
        assertEquals("100GEm", serviceType, "service-type should be 100GEm");

        serviceType = ServiceTypes.getOtnServiceType("OTU", Uint32.valueOf(100));
        assertEquals("OTU4", serviceType, "service-type should be OTU4");
        serviceType = ServiceTypes.getOtnServiceType("OTU", Uint32.valueOf(400));
        assertEquals("OTUC4", serviceType, "service-type should be OTUC4");

        serviceType = ServiceTypes.getOtnServiceType("ODU", Uint32.valueOf(100));
        assertEquals("ODU4", serviceType, "service-type should be ODU4");
        serviceType = ServiceTypes.getOtnServiceType("ODU", Uint32.valueOf(400));
        assertEquals("ODUC4", serviceType, "service-type should be ODUC4");
    }
}