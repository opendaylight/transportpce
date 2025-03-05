/*
 * Copyright © 2020 NTT and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.renderer.provisiondevice;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

import java.util.HashSet;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import org.opendaylight.transportpce.common.crossconnect.CrossConnect;
import org.opendaylight.transportpce.common.device.DeviceTransactionManager;
import org.opendaylight.transportpce.common.mapping.MappingUtils;
import org.opendaylight.transportpce.common.mapping.PortMapping;
import org.opendaylight.transportpce.common.openconfiginterfaces.OpenConfigInterfaces;
import org.opendaylight.transportpce.common.openconfiginterfaces.OpenConfigInterfacesException;
import org.opendaylight.transportpce.common.openroadminterfaces.OpenRoadmInterfaces;
import org.opendaylight.transportpce.renderer.openconfiginterface.OpenConfigInterfaceFactory;
import org.opendaylight.transportpce.renderer.openroadminterface.OpenRoadmInterfaceFactory;
import org.opendaylight.transportpce.test.AbstractTest;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev240315.mapping.Mapping;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev240315.mapping.MappingBuilder;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev240315.mapping.MappingKey;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev240315.mapping.mapping.OpenconfigInfoBuilder;
import org.opendaylight.yang.gen.v1.http.org.transportpce.common.types.rev220926.link.tp.LinkTp;
import org.opendaylight.yang.gen.v1.http.org.transportpce.common.types.rev220926.otn.renderer.nodes.Nodes;
import org.opendaylight.yang.gen.v1.http.org.transportpce.common.types.rev220926.otn.renderer.nodes.NodesBuilder;

public class OtnDeviceRendererServiceImplTest  extends AbstractTest {

    private  OpenRoadmInterfaceFactory openRoadmInterfaceFactory = Mockito.mock(OpenRoadmInterfaceFactory.class);
    private  CrossConnect crossConnect = Mockito.mock(CrossConnect.class);
    private  OpenRoadmInterfaces openRoadmInterfaces = Mockito.mock(OpenRoadmInterfaces.class);
    DeviceTransactionManager deviceTransactionManager = Mockito.mock(DeviceTransactionManager.class);
    private  PortMapping portMapping = Mockito.mock(PortMapping.class);
    private  OpenConfigInterfaceFactory openConfigInterfaceFactory = Mockito.mock(OpenConfigInterfaceFactory.class);
    private  OpenConfigInterfaces openConfigInterfaces = Mockito.mock(OpenConfigInterfaces.class);
    private  MappingUtils mappingUtils = Mockito.mock(MappingUtils.class);
    private OtnDeviceRendererServiceImpl otnDeviceRendererServiceImplTest;

    @Test
    public void disableOpenConfigClientTest() throws OpenConfigInterfacesException {
        otnDeviceRendererServiceImplTest = new OtnDeviceRendererServiceImpl(crossConnect, openRoadmInterfaces,
                deviceTransactionManager, mappingUtils, portMapping, openConfigInterfaces);
        when(portMapping.getMapping(anyString(), anyString())).thenReturn(getMappingTestData());
        Nodes node = new NodesBuilder().setNodeId("node1").setClientTp("XPDR1-CLIENT1").build();
        ConcurrentLinkedQueue<String> results = new ConcurrentLinkedQueue<>();
        LinkTp linkTp = otnDeviceRendererServiceImplTest.disableOpenConfigClient(node,
                new AtomicBoolean(true), results);
        Assert.assertTrue(linkTp.getTpId().equals("XPDR1-CLIENT1"));
        Assert.assertTrue(results.contains("successfully disabled entities on node node1!"));
    }

    @Test
    public void disableOpenConfigClientFailureTest() throws OpenConfigInterfacesException {
        otnDeviceRendererServiceImplTest = new OtnDeviceRendererServiceImpl(crossConnect, openRoadmInterfaces,
                deviceTransactionManager, mappingUtils, portMapping, openConfigInterfaces);
        when(portMapping.getMapping(anyString(), anyString())).thenReturn(getMappingTestData());
        doThrow(new OpenConfigInterfacesException("failed")).when(openConfigInterfaces)
                .configureComponent(anyString(), any());
        Nodes node = new NodesBuilder().setNodeId("node1").setClientTp("XPDR1-CLIENT1").build();
        ConcurrentLinkedQueue<String> results = new ConcurrentLinkedQueue<>();
        LinkTp linkTp = otnDeviceRendererServiceImplTest.disableOpenConfigClient(node,
                new AtomicBoolean(true), results);
        Assert.assertTrue(linkTp.getTpId().equals("XPDR1-CLIENT1"));
        Assert.assertTrue(results.contains("Failed to disable entities on node node1!"));
    }

    private Mapping getMappingTestData() {
        OpenconfigInfoBuilder openconfigInfoBuilder = new OpenconfigInfoBuilder();
        HashSet<String> opticalChannels = new HashSet<>();
        opticalChannels.add("qsfp-opt-1-1");
        HashSet<String> supportedInterfaces = new HashSet<>();
        supportedInterfaces.add("logical-channel-23300101");
        Mapping mapping = new MappingBuilder().withKey(new MappingKey("XPDR1-CLIENT1"))
                .setLogicalConnectionPoint("XPDR1-CLIENT1")
                .setSupportingCircuitPackName("qsfp-transceiver-1")
                .setSupportingPort("qsfp-1")
                .setOpenconfigInfo(openconfigInfoBuilder.setSupportedOpticalChannels(opticalChannels).build())
                .setOpenconfigInfo(openconfigInfoBuilder.setSupportedInterfaces(supportedInterfaces).build()).build();
        return mapping;
    }
}
