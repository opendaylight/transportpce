/*
 * Copyright © 2019 Orange, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.renderer.openroadminterface;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Arrays;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opendaylight.transportpce.common.fixedflex.SpectrumInformation;
import org.opendaylight.transportpce.common.mapping.PortMapping;
import org.opendaylight.transportpce.common.openroadminterfaces.OpenRoadmInterfaceException;
import org.opendaylight.transportpce.common.openroadminterfaces.OpenRoadmInterfaces;
import org.opendaylight.transportpce.test.AbstractTest;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev250115.mapping.MappingBuilder;
import org.opendaylight.yangtools.yang.common.Uint32;

public class OpenRoadMInterface221Test extends AbstractTest {

    private final PortMapping portMapping = mock(PortMapping.class);
    private OpenRoadmInterface221 openRoadMInterface221;
    private final String nodeId = "node1";

    @BeforeEach
    void setup() {
        OpenRoadmInterfaces openRoadmInterfaces = spy(OpenRoadmInterfaces.class);
        this.openRoadMInterface221 = new OpenRoadmInterface221(portMapping, openRoadmInterfaces);
    }

    @Test
    void testCreateOpenRoadmEthInterface() throws OpenRoadmInterfaceException {

        String logicalConnPoint = "logicalConnPoint";
        when(portMapping.getMapping(nodeId, logicalConnPoint))
            .thenReturn(new MappingBuilder().setSupportingCircuitPackName("circit-pack").setSupportingPort("port")
                .setLogicalConnectionPoint(logicalConnPoint).build());
        assertEquals(openRoadMInterface221.createOpenRoadmEthInterface(nodeId, logicalConnPoint),
            logicalConnPoint + "-ETHERNET");
    }

    @Test
    void testCreateOpenRoadmEthInterfaceThrowsExcpetion() throws OpenRoadmInterfaceException {
        when(portMapping.getMapping(eq(nodeId), any())).thenReturn(null);
        Exception exception = assertThrows(OpenRoadmInterfaceException.class, () -> {
            openRoadMInterface221.createOpenRoadmEthInterface(nodeId, "logicalConnPoint");
        });
        assertEquals(
            "Unable to get mapping from PortMapping for node node1 and logical connection port logicalConnPoint",
            exception.getMessage());
    }

    @Test
    void testCreateFlexOCH() throws OpenRoadmInterfaceException {
        String logicalConnPoint = "logicalConnPoint";
        when(portMapping.getMapping(nodeId, logicalConnPoint))
            .thenReturn(new MappingBuilder().setSupportingCircuitPackName("circit-pack").setSupportingPort("port")
                .setLogicalConnectionPoint(logicalConnPoint).build());
        SpectrumInformation spectrumInformation = new SpectrumInformation();
        spectrumInformation.setWaveLength(Uint32.valueOf(1));
        spectrumInformation.setLowerSpectralSlotNumber(761);
        spectrumInformation.setHigherSpectralSlotNumber(768);
        spectrumInformation.setCenterFrequency(BigDecimal.valueOf(195.8));
        assertNotNull(openRoadMInterface221.createFlexOCH(nodeId, logicalConnPoint, spectrumInformation));
        assertEquals(openRoadMInterface221.createFlexOCH(nodeId, logicalConnPoint, spectrumInformation),
            Arrays.asList(logicalConnPoint + "-nmc-761:768"));
    }

    @Test
    void testCreateFlexOCHReturnsMoreThanOneElement() throws OpenRoadmInterfaceException {

        String logicalConnPoint = "logicalConnPointDEG";
        when(portMapping.getMapping(nodeId, logicalConnPoint))
            .thenReturn(new MappingBuilder().setSupportingCircuitPackName("circit-pack").setSupportingPort("port")
                .setLogicalConnectionPoint(logicalConnPoint).build());
        SpectrumInformation spectrumInformation = new SpectrumInformation();
        spectrumInformation.setWaveLength(Uint32.valueOf(1));
        spectrumInformation.setLowerSpectralSlotNumber(761);
        spectrumInformation.setHigherSpectralSlotNumber(768);
        spectrumInformation.setCenterFrequency(BigDecimal.valueOf(195.8));
        spectrumInformation.setMinFrequency(BigDecimal.valueOf(195.775));
        spectrumInformation.setMaxFrequency(BigDecimal.valueOf(195.825));
        assertNotNull(openRoadMInterface221.createFlexOCH(nodeId, logicalConnPoint,spectrumInformation));
        assertEquals(openRoadMInterface221.createFlexOCH(nodeId, logicalConnPoint, spectrumInformation),
            Arrays.asList(logicalConnPoint + "-mc-761:768", logicalConnPoint + "-nmc-761:768"));
    }

    @Test
    void testCreateOpenRoadmOchInterface() throws OpenRoadmInterfaceException {
        String logicalConnPoint = "logicalConnPoint";
        when(portMapping.getMapping(nodeId, logicalConnPoint))
            .thenReturn(new MappingBuilder().setSupportingCircuitPackName("circit-pack").setSupportingPort("port")
                .setLogicalConnectionPoint(logicalConnPoint).build());
        SpectrumInformation spectrumInformation = new SpectrumInformation();
        spectrumInformation.setWaveLength(Uint32.valueOf(1));
        spectrumInformation.setLowerSpectralSlotNumber(761);
        spectrumInformation.setHigherSpectralSlotNumber(768);
        spectrumInformation.setCenterFrequency(BigDecimal.valueOf(195.8));
        assertEquals(openRoadMInterface221.createOpenRoadmOchInterface(nodeId, logicalConnPoint,
            spectrumInformation), logicalConnPoint + "-761:768");
    }

    @Test
    void testCreateOpenRoadmOtu4Interface() throws OpenRoadmInterfaceException {
        String logicalConnPoint = "logicalConnPoint";
        String supportOchInterface = "supportOchInterface";
        when(portMapping.getMapping(nodeId, logicalConnPoint))
            .thenReturn(new MappingBuilder().setSupportingCircuitPackName("circit-pack").setSupportingPort("port")
                .setLogicalConnectionPoint(logicalConnPoint).build());
        assertEquals(
            openRoadMInterface221.createOpenRoadmOtu4Interface(nodeId, logicalConnPoint, supportOchInterface, null,
                null),
            logicalConnPoint + "-OTU");
    }

    @Test
    void testCreateOpenRoadmOchInterfaceName() {
        String logicalConnPoint = "logicalConnPoint";
        assertEquals(openRoadMInterface221.createOpenRoadmOchInterfaceName(logicalConnPoint, "761:768"),
            logicalConnPoint + "-761:768");
    }

    @Test
    void testCreateOpenRoadmOmsInterfaceSupportingOtsNotNull() throws OpenRoadmInterfaceException {
        String logicalConnPoint = "logicalConnPoint";
        String supportingOts = "supportingOts";
        assertEquals(
            openRoadMInterface221.createOpenRoadmOmsInterface(nodeId, new MappingBuilder()
                .setSupportingCircuitPackName("circit-pack").setSupportingPort("port")
                .setLogicalConnectionPoint(logicalConnPoint).setSupportingOts(supportingOts).build()),
            "OMS-" + logicalConnPoint);
    }

    @Test
    void testCreateOpenRoadmOmsInterfaceSupportingOmsNotNullException() throws OpenRoadmInterfaceException {
        assertNull(openRoadMInterface221.createOpenRoadmOmsInterface(nodeId, new MappingBuilder()
            .setLogicalConnectionPoint("logicalConnPoint").build()));
    }

    @Test
    void testCreateOpenRoadmOmsInterfaceSupportingOmsNull() throws OpenRoadmInterfaceException {
        String supportingOts = "supportingOts";
        String logicalConnPoint = "logicalConnPoint";
        assertEquals(
            openRoadMInterface221.createOpenRoadmOmsInterface(nodeId, new MappingBuilder()
                .setLogicalConnectionPoint(logicalConnPoint).setSupportingOts(supportingOts).build()),
            "OMS-" + logicalConnPoint);
    }
}
