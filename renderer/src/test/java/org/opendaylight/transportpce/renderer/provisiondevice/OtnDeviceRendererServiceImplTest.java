/*
 * Copyright © 2020 NTT and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.renderer.provisiondevice;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.HashSet;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opendaylight.transportpce.common.crossconnect.CrossConnect;
import org.opendaylight.transportpce.common.device.DeviceTransactionManager;
import org.opendaylight.transportpce.common.mapping.MappingUtils;
import org.opendaylight.transportpce.common.mapping.PortMapping;
import org.opendaylight.transportpce.common.openconfiginterfaces.OpenConfigInterfaces;
import org.opendaylight.transportpce.common.openconfiginterfaces.OpenConfigInterfacesException;
import org.opendaylight.transportpce.common.openroadminterfaces.OpenRoadmInterfaces;
import org.opendaylight.transportpce.test.AbstractTest;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev250714.mapping.Mapping;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev250714.mapping.MappingBuilder;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev250714.mapping.MappingKey;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev250714.mapping.mapping.OpenconfigInfoBuilder;
import org.opendaylight.yang.gen.v1.http.org.transportpce.common.types.rev250325.link.tp.LinkTp;
import org.opendaylight.yang.gen.v1.http.org.transportpce.common.types.rev250325.otn.renderer.nodes.Nodes;
import org.opendaylight.yang.gen.v1.http.org.transportpce.common.types.rev250325.otn.renderer.nodes.NodesBuilder;

@ExtendWith(MockitoExtension.class)
public class OtnDeviceRendererServiceImplTest  extends AbstractTest {

    @Mock
    private CrossConnect crossConnect;
    @Mock
    private OpenRoadmInterfaces openRoadmInterfaces;
    @Mock
    DeviceTransactionManager deviceTransactionManager;
    @Mock
    private MappingUtils mappingUtils;
    private final OpenConfigInterfaces openConfigInterfaces = mock(OpenConfigInterfaces.class);
    private final PortMapping portMapping = mock(PortMapping.class);
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
        assertEquals("XPDR1-CLIENT1", linkTp.getTpId());
        assertTrue(results.contains("successfully disabled entities on node node1!"));
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
        assertEquals("XPDR1-CLIENT1", linkTp.getTpId());
        assertTrue(results.contains("Failed to disable entities on node node1!"));
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
