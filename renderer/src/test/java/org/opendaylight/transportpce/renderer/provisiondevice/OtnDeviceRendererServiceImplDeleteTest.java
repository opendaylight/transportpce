/*
 * Copyright © 2026 Smartoptics and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.renderer.provisiondevice;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opendaylight.transportpce.common.StringConstants;
import org.opendaylight.transportpce.common.crossconnect.CrossConnect;
import org.opendaylight.transportpce.common.device.DeviceTransactionManager;
import org.opendaylight.transportpce.common.mapping.MappingUtils;
import org.opendaylight.transportpce.common.mapping.PortMapping;
import org.opendaylight.transportpce.common.openconfiginterfaces.OpenConfigInterfaces;
import org.opendaylight.transportpce.common.openroadminterfaces.OpenRoadmInterfaceException;
import org.opendaylight.transportpce.common.openroadminterfaces.OpenRoadmInterfaces;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.device.renderer.rev260212.OtnServicePathInput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.device.renderer.rev260212.OtnServicePathInputBuilder;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.device.renderer.rev260212.OtnServicePathOutput;
import org.opendaylight.yang.gen.v1.http.org.transportpce.common.types.rev260707.otn.renderer.nodes.NodesBuilder;
import org.opendaylight.yangtools.yang.common.Uint32;

/**
 * Tests that deleting an OTN service path tells a device that cannot be read apart from a device without the
 * interfaces to delete.
 */
@ExtendWith(MockitoExtension.class)
class OtnDeviceRendererServiceImplDeleteTest {

    private static final String NODE = "XPDR-A1";
    private static final String CLIENT_INTERFACE = "XPDR1-CLIENT1-ODU2e";
    private static final String NETWORK_INTERFACE = "XPDR1-NETWORK1-ODU2e";

    @Mock
    private CrossConnect crossConnect;
    @Mock
    private OpenRoadmInterfaces openRoadmInterfaces;
    @Mock
    private DeviceTransactionManager deviceTransactionManager;
    @Mock
    private MappingUtils mappingUtils;
    @Mock
    private PortMapping portMapping;
    @Mock
    private OpenConfigInterfaces openConfigInterfaces;

    private OtnDeviceRendererService otnDeviceRendererService;
    private OtnServicePathInput input;

    @BeforeEach
    void setup() throws Exception {
        otnDeviceRendererService = new OtnDeviceRendererServiceImpl(crossConnect, openRoadmInterfaces,
                deviceTransactionManager, mappingUtils, portMapping, openConfigInterfaces);
        when(deviceTransactionManager.isDeviceMounted(NODE)).thenReturn(true);
        when(crossConnect.deleteCrossConnect(eq(NODE), anyString(), eq(true)))
            .thenReturn(List.of(
                CLIENT_INTERFACE,
                NETWORK_INTERFACE));
        input = new OtnServicePathInputBuilder()
            .setServiceFormat("Ethernet")
            .setServiceRate(Uint32.valueOf(10))
            .setNodes(List.of(new NodesBuilder()
                .setNodeId(NODE)
                .setClientTp("XPDR1-CLIENT1")
                .setNetworkTp("XPDR1-NETWORK1")
                .build()))
            .build();
    }

    @Test
    void deleteOtnServicePathFailsWhenTheSupportingInterfaceCannotBeRead() throws Exception {
        when(openRoadmInterfaces.getSupportedInterface(NODE, CLIENT_INTERFACE)).thenReturn(null);
        when(openRoadmInterfaces.getSupportedInterface(NODE, NETWORK_INTERFACE))
            .thenThrow(new OpenRoadmInterfaceException("Could not read interface on node " + NODE + ": Timed out"));

        OtnServicePathOutput output =
                otnDeviceRendererService.deleteOtnServicePath(input, StringConstants.SERVICE_TYPE_10GE);

        // Before, this reported success: the unreadable interface looked like one without a supporting interface
        assertFalse(output.getSuccess());
        assertTrue(output.getResult().contains("Could not find the interface supporting " + NETWORK_INTERFACE),
                output.getResult());
        assertTrue(output.getResult().contains("Timed out"), output.getResult());
        // The interfaces from the deleted cross connect are still deleted, since a retry would not find them again
        verify(openRoadmInterfaces).deleteInterface(NODE, CLIENT_INTERFACE);
        verify(openRoadmInterfaces).deleteInterface(NODE, NETWORK_INTERFACE);
    }

    @Test
    void deleteOtnServicePathSucceedsWhenThereIsNoSupportingInterface() throws Exception {
        when(openRoadmInterfaces.getSupportedInterface(eq(NODE), anyString())).thenReturn(null);

        OtnServicePathOutput output =
                otnDeviceRendererService.deleteOtnServicePath(input, StringConstants.SERVICE_TYPE_10GE);

        assertTrue(output.getSuccess());
        assertEquals("Request processed", output.getResult());
        verify(openRoadmInterfaces).deleteInterface(NODE, CLIENT_INTERFACE);
        verify(openRoadmInterfaces).deleteInterface(NODE, NETWORK_INTERFACE);
        verify(openRoadmInterfaces, never()).deleteInterface(NODE, null);
    }

    @Test
    void deleteOtnServicePathKeepsTheInterruptStatus() throws Exception {
        CountDownLatch deleting = new CountDownLatch(1);
        CountDownLatch releaseDelete = new CountDownLatch(1);
        when(openRoadmInterfaces.getSupportedInterface(eq(NODE), anyString())).thenAnswer(invocation -> {
            deleting.countDown();
            releaseDelete.await();
            return null;
        });
        AtomicReference<OtnServicePathOutput> output = new AtomicReference<>();
        AtomicBoolean interruptStatus = new AtomicBoolean();
        Thread caller = new Thread(() -> {
            output.set(otnDeviceRendererService.deleteOtnServicePath(input, StringConstants.SERVICE_TYPE_10GE));
            interruptStatus.set(Thread.currentThread().isInterrupted());
        });
        caller.start();
        try {
            assertTrue(deleting.await(10, TimeUnit.SECONDS));
            // As at shutdown
            caller.interrupt();
            caller.join(10_000);
        } finally {
            releaseDelete.countDown();
        }

        assertFalse(caller.isAlive());
        assertFalse(output.get().getSuccess());
        assertTrue(output.get().getResult().contains("Interrupted"), output.get().getResult());
        assertTrue(interruptStatus.get());
    }
}
