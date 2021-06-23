/*
 * Copyright © 2019 Orange, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.renderer.openroadminterface;

import java.util.Arrays;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.Mockito;
import org.opendaylight.transportpce.common.fixedflex.SpectrumInformation;
import org.opendaylight.transportpce.common.mapping.PortMapping;
import org.opendaylight.transportpce.common.openroadminterfaces.OpenRoadmInterfaceException;
import org.opendaylight.transportpce.common.openroadminterfaces.OpenRoadmInterfaces;
import org.opendaylight.transportpce.test.AbstractTest;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev210426.mapping.MappingBuilder;
import org.opendaylight.yangtools.yang.common.Uint32;

@Ignore
public class OpenRoadMInterface221Test extends AbstractTest {

    private final PortMapping portMapping = Mockito.mock(PortMapping.class);
    private OpenRoadmInterface221 openRoadMInterface221;
    private final String nodeId = "node1";

    @Before
    public void setup() {

        OpenRoadmInterfaces openRoadmInterfaces = Mockito.spy(OpenRoadmInterfaces.class);
        this.openRoadMInterface221 = new OpenRoadmInterface221(portMapping, openRoadmInterfaces);
    }

    @Test
    public void testCreateOpenRoadmEthInterface() throws OpenRoadmInterfaceException {

        String logicalConnPoint = "logicalConnPoint";
        Mockito.when(portMapping.getMapping(nodeId, logicalConnPoint)).thenReturn(new MappingBuilder().build());
        Assert.assertEquals(openRoadMInterface221.createOpenRoadmEthInterface(nodeId, logicalConnPoint),
                logicalConnPoint + "-ETHERNET");
    }

    @Test(expected = Exception.class)
    public void testCreateOpenRoadmEthInterfaceThrowsExcpetion() throws OpenRoadmInterfaceException {

        String logicalConnPoint = "logicalConnPoint";
        Mockito.when(portMapping.getMapping(nodeId, logicalConnPoint)).thenReturn(null);
        openRoadMInterface221.createOpenRoadmEthInterface(nodeId, logicalConnPoint);
    }

    @Test
    public void testCreateFlexOCH() throws OpenRoadmInterfaceException {

        String logicalConnPoint = "logicalConnPoint";
        Mockito.when(portMapping.getMapping(nodeId, logicalConnPoint)).thenReturn(new MappingBuilder().build());
        SpectrumInformation spectrumInformation = new SpectrumInformation();
        spectrumInformation.setWaveLength(Uint32.valueOf(1));
        spectrumInformation.setLowerSpectralSlotNumber(761);
        spectrumInformation.setHigherSpectralSlotNumber(768);
        Assert.assertNotNull(openRoadMInterface221.createFlexOCH(nodeId, logicalConnPoint, spectrumInformation));
        Assert.assertEquals(openRoadMInterface221.createFlexOCH(nodeId, logicalConnPoint, spectrumInformation),
                Arrays.asList(logicalConnPoint + "-nmc-761:768"));
    }

    @Test
    public void testCreateFlexOCHReturnsMoreThanOneElement() throws OpenRoadmInterfaceException {

        String logicalConnPoint = "logicalConnPointDEG";
        Mockito.when(portMapping.getMapping(nodeId, logicalConnPoint)).thenReturn(new MappingBuilder().build());
        SpectrumInformation spectrumInformation = new SpectrumInformation();
        spectrumInformation.setWaveLength(Uint32.valueOf(1));
        spectrumInformation.setLowerSpectralSlotNumber(761);
        spectrumInformation.setHigherSpectralSlotNumber(768);
        Assert.assertNotNull(openRoadMInterface221.createFlexOCH(nodeId, logicalConnPoint,spectrumInformation));
        Assert.assertEquals(openRoadMInterface221.createFlexOCH(nodeId, logicalConnPoint, spectrumInformation),
                Arrays.asList(logicalConnPoint + "-mc-761:768", logicalConnPoint + "-nmc-761:768"));
    }

    @Test
    public void testCreateOpenRoadmOchInterface() throws OpenRoadmInterfaceException {

        String logicalConnPoint = "logicalConnPoint";
        Mockito.when(portMapping.getMapping(nodeId, logicalConnPoint))
                .thenReturn(new MappingBuilder().setLogicalConnectionPoint(logicalConnPoint).build());
        SpectrumInformation spectrumInformation = new SpectrumInformation();
        spectrumInformation.setWaveLength(Uint32.valueOf(1));
        spectrumInformation.setLowerSpectralSlotNumber(761);
        spectrumInformation.setHigherSpectralSlotNumber(768);
        Assert.assertEquals(openRoadMInterface221.createOpenRoadmOchInterface(nodeId, logicalConnPoint,
                spectrumInformation), logicalConnPoint + "-761:768");
    }

    @Test
    public void testCreateOpenRoadmOtu4Interface() throws OpenRoadmInterfaceException {

        String logicalConnPoint = "logicalConnPoint";
        String supportOchInterface = "supportOchInterface";
        Mockito.when(portMapping.getMapping(nodeId, logicalConnPoint))
                .thenReturn(new MappingBuilder().setLogicalConnectionPoint(logicalConnPoint).build());
        Assert.assertEquals(
                openRoadMInterface221.createOpenRoadmOtu4Interface(nodeId, logicalConnPoint, supportOchInterface),
                logicalConnPoint + "-OTU");

    }

    @Test
    public void testCreateOpenRoadmOchInterfaceName() {

        String logicalConnPoint = "logicalConnPoint";
        Assert.assertEquals(openRoadMInterface221.createOpenRoadmOchInterfaceName(logicalConnPoint, "761:768"),
                logicalConnPoint + "-761:768");

    }

    @Test
    public void testCreateOpenRoadmOmsInterfaceSupportingOmsNotNull() throws OpenRoadmInterfaceException {

        String supportingOms = "supportingOms";
        Assert.assertEquals(openRoadMInterface221.createOpenRoadmOmsInterface(nodeId,
                new MappingBuilder().setSupportingOms(supportingOms).build()), supportingOms);
    }

    @Test
    public void testCreateOpenRoadmOmsInterfaceSupportingOmsNotNullException() throws OpenRoadmInterfaceException {

        Assert.assertNull(openRoadMInterface221.createOpenRoadmOmsInterface(nodeId, new MappingBuilder().build()));
    }

    @Test
    public void testCreateOpenRoadmOmsInterfaceSupportingOmsNull() throws OpenRoadmInterfaceException {

        String supportingOts = "supportingOts";
        String logicalConnPoint = "logicalConnPoint";
        Assert.assertEquals(
                openRoadMInterface221.createOpenRoadmOmsInterface(nodeId, new MappingBuilder()
                        .setLogicalConnectionPoint(logicalConnPoint).setSupportingOts(supportingOts).build()),
                "OMS-" + logicalConnPoint);
    }

}
