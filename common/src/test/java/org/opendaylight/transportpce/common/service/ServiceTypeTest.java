/*
 * Copyright © 2021 Orange.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.common.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev220316.mapping.Mapping;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev220316.mapping.MappingBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.types.rev191129.PortQual;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.types.rev191129.XpdrNodeTypes;
import org.opendaylight.yangtools.yang.common.Uint32;

public class ServiceTypeTest {

    @Test
    public void getServiceTypeForServiceFormatUnknownTest() {
        String serviceType = ServiceTypes.getServiceType("toto", null, null);
        assertNull("service-type should be null", serviceType);
    }

    @Test
    public void getServiceTypeForServiceFormatOCTest() {
        String serviceType = ServiceTypes.getServiceType("OC", Uint32.valueOf(100), null);
        assertEquals("service-type should be 100GEt", "100GEt", serviceType);
        serviceType = ServiceTypes.getServiceType("OC", null, null);
        assertNull("service-type should be null", serviceType);
    }

    @Test
    public void getServiceTypeForServiceFormatEthernetTest() {
        String serviceType = ServiceTypes.getServiceType("Ethernet", Uint32.valueOf(400), null);
        assertEquals("service-type should be 400GE", "400GE", serviceType);
        serviceType = ServiceTypes.getServiceType("Ethernet", Uint32.valueOf(100), null);
        assertEquals("service-type should be 100GEt", "100GEt", serviceType);
        Mapping mapping = new MappingBuilder()
            .setLogicalConnectionPoint("logicalConnectionPoint")
            .setPortQual(PortQual.XpdrClient.getName())
            .build();
        serviceType = ServiceTypes.getServiceType("Ethernet", Uint32.valueOf(100), mapping);
        assertEquals("service-type should be 100GEt", "100GEt", serviceType);

        mapping = new MappingBuilder()
            .setLogicalConnectionPoint("logicalConnectionPoint")
            .setPortQual(PortQual.SwitchClient.getName())
            .build();
        serviceType = ServiceTypes.getServiceType("Ethernet", Uint32.valueOf(100), mapping);
        assertEquals("service-type should be 100GEm", "100GEm", serviceType);
        serviceType = ServiceTypes.getServiceType("Ethernet", Uint32.valueOf(10), mapping);
        assertEquals("service-type should be 10GE", "10GE", serviceType);
        serviceType = ServiceTypes.getServiceType("Ethernet", Uint32.valueOf(1), mapping);
        assertEquals("service-type should be 1GE", "1GE", serviceType);

        mapping = new MappingBuilder()
            .setLogicalConnectionPoint("logicalConnectionPoint")
            .setPortQual(PortQual.SwitchClient.getName())
            .setXponderType(XpdrNodeTypes.Switch)
            .build();
        serviceType = ServiceTypes.getServiceType("Ethernet", Uint32.valueOf(100), mapping);
        assertEquals("service-type should be 100GEs", "100GEs", serviceType);
    }

    @Test
    public void getOtnServiceTypeForServiceFormatEthernetTest() {
        String serviceType = ServiceTypes.getOtnServiceType("toto", Uint32.valueOf(123));
        assertNull("service-type should be null", serviceType);
        serviceType = ServiceTypes.getOtnServiceType("Ethernet", Uint32.valueOf(123));
        assertNull("service-type should be null", serviceType);
        serviceType = ServiceTypes.getOtnServiceType("Ethernet", Uint32.valueOf(1));
        assertEquals("service-type should be 1GE", "1GE", serviceType);
        serviceType = ServiceTypes.getOtnServiceType("Ethernet", Uint32.valueOf(10));
        assertEquals("service-type should be 10GE", "10GE", serviceType);
        serviceType = ServiceTypes.getOtnServiceType("Ethernet", Uint32.valueOf(100));
        assertEquals("service-type should be 100GEm", "100GEm", serviceType);

        serviceType = ServiceTypes.getOtnServiceType("OTU", Uint32.valueOf(100));
        assertEquals("service-type should be OTU4", "OTU4", serviceType);
        serviceType = ServiceTypes.getOtnServiceType("OTU", Uint32.valueOf(400));
        assertEquals("service-type should be OTUC4", "OTUC4", serviceType);

        serviceType = ServiceTypes.getOtnServiceType("ODU", Uint32.valueOf(100));
        assertEquals("service-type should be ODU4", "ODU4", serviceType);
        serviceType = ServiceTypes.getOtnServiceType("ODU", Uint32.valueOf(400));
        assertEquals("service-type should be ODUC4", "ODUC4", serviceType);
    }

}