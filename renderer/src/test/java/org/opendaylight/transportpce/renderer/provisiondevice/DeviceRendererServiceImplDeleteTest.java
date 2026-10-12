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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.binding.api.WriteTransaction;
import org.opendaylight.mdsal.common.api.CommitInfo;
import org.opendaylight.transportpce.common.crossconnect.CrossConnect;
import org.opendaylight.transportpce.common.device.DeviceReadException;
import org.opendaylight.transportpce.common.device.DeviceTransactionManager;
import org.opendaylight.transportpce.common.mapping.MappingUtils;
import org.opendaylight.transportpce.common.mapping.PortMapping;
import org.opendaylight.transportpce.common.openroadminterfaces.OpenRoadmInterfaceException;
import org.opendaylight.transportpce.common.openroadminterfaces.OpenRoadmInterfaces;
import org.opendaylight.transportpce.renderer.utils.ServiceDataUtils;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.device.renderer.rev260212.ServicePathInput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.device.renderer.rev260212.ServicePathInputBuilder;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.device.renderer.rev260212.ServicePathOutput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev260612.OpenroadmNodeVersion;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev260612.network.NodesBuilder;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev260612.network.nodes.NodeInfoBuilder;
import org.opendaylight.yangtools.yang.common.Uint32;

/**
 * Tests that deleting a service path tells a device that cannot be read apart from a device without the service's
 * cross connect or interfaces.
 */
@ExtendWith(MockitoExtension.class)
class DeviceRendererServiceImplDeleteTest {

    private static final String NODE = "ROADM-A1";
    private static final String XPDR = "XPDR-A1";

    @Mock
    private DataBroker dataBroker;
    @Mock
    private WriteTransaction writeTransaction;
    @Mock
    private DeviceTransactionManager deviceTransactionManager;
    @Mock
    private OpenRoadmInterfaces openRoadmInterfaces;
    @Mock
    private CrossConnect crossConnect;
    @Mock
    private MappingUtils mappingUtils;
    @Mock
    private PortMapping portMapping;

    private DeviceRendererService deviceRendererService;
    private ServicePathInput input;

    @BeforeEach
    void setup() {
        deviceRendererService = new DeviceRendererServiceImpl(dataBroker, deviceTransactionManager,
                openRoadmInterfaces, crossConnect, mappingUtils, portMapping, null);
        // Registering and removing the nodes for alarm suppression writes to the datastore
        when(dataBroker.newWriteOnlyTransaction()).thenReturn(writeTransaction);
        doReturn(CommitInfo.emptyFluentFuture()).when(writeTransaction).commit();
        // Only the ROADM tests use this node
        lenient().when(deviceTransactionManager.isDeviceMounted(NODE)).thenReturn(true);
        // A ROADM path: neither end is a client or network port
        input = new ServicePathInputBuilder(ServiceDataUtils.buildServicePathInputs(
                List.of(ServiceDataUtils.createNode(NODE, "DEG1-TTP-TXRX", "SRG1-PP1-TXRX"))))
            .setLowerSpectralSlotNumber(Uint32.valueOf(761))
            .setHigherSpectralSlotNumber(Uint32.valueOf(768))
            .build();
    }

    @Test
    void deleteServicePathFailsWhenTheNodeCannotBeRead() throws Exception {
        when(crossConnect.deleteCrossConnect(eq(NODE), anyString(), eq(false)))
            .thenThrow(new DeviceReadException("Timed out waiting for device " + NODE));

        ServicePathOutput output = deviceRendererService.deleteServicePath(input);

        // Before, this reported success: the unreadable cross connect looked like one that did not exist
        assertFalse(output.getSuccess());
        assertTrue(output.getResult().contains("Could not read node " + NODE), output.getResult());
        assertTrue(output.getResult().contains("Timed out"), output.getResult());
        verify(openRoadmInterfaces, never()).deleteInterface(any(), any());
    }

    @Test
    void deleteServicePathFailsWhenTheCrossConnectCannotBeDeleted() throws Exception {
        when(crossConnect.deleteCrossConnect(eq(NODE), anyString(), eq(false)))
            .thenThrow(new OpenRoadmInterfaceException("Failed to delete cross connect on node " + NODE));

        ServicePathOutput output = deviceRendererService.deleteServicePath(input);

        // Before, this reported success: a failed delete looked like a cross connect that did not exist
        assertFalse(output.getSuccess());
        assertTrue(output.getResult().contains("Failed to delete cross connect on node " + NODE), output.getResult());
        verify(openRoadmInterfaces, never()).deleteInterface(any(), any());
    }

    @Test
    void deleteServicePathSucceedsWhenTheCrossConnectDoesNotExist() throws Exception {
        when(crossConnect.deleteCrossConnect(eq(NODE), anyString(), eq(false))).thenReturn(null);

        ServicePathOutput output = deviceRendererService.deleteServicePath(input);

        // Nothing to delete is still a successful delete, so deleting twice stays harmless
        assertTrue(output.getSuccess());
        assertEquals("Request processed", output.getResult());
        verify(openRoadmInterfaces, never()).deleteInterface(any(), any());
    }

    @Test
    void deleteServicePathFailsWhenTheXponderCannotBeRead() throws Exception {
        ServicePathInput xpdrInput = xponderInput();
        when(openRoadmInterfaces.readInterface(eq(XPDR), anyString()))
            .thenThrow(new OpenRoadmInterfaceException("Could not read interface on node " + XPDR + ": Timed out"));

        ServicePathOutput output = deviceRendererService.deleteServicePath(xpdrInput);

        // Before, this reported success: the unreadable network interfaces looked like ones that did not exist
        assertFalse(output.getSuccess());
        assertTrue(output.getResult().contains("Could not read interface on node " + XPDR), output.getResult());
        assertTrue(output.getResult().contains("Timed out"), output.getResult());
        verify(openRoadmInterfaces, never()).deleteInterface(any(), any());
    }

    @Test
    void deleteServicePathSucceedsWhenTheXponderHasNoInterfaces() throws Exception {
        ServicePathInput xpdrInput = xponderInput();
        when(openRoadmInterfaces.readInterface(eq(XPDR), anyString())).thenReturn(Optional.empty());

        ServicePathOutput output = deviceRendererService.deleteServicePath(xpdrInput);

        // Nothing to delete is still a successful delete, so deleting twice stays harmless
        assertTrue(output.getSuccess());
        assertEquals("Request processed", output.getResult());
        verify(openRoadmInterfaces, never()).deleteInterface(any(), any());
    }

    private ServicePathInput xponderInput() {
        when(deviceTransactionManager.isDeviceMounted(XPDR)).thenReturn(true);
        when(portMapping.getNode(XPDR)).thenReturn(new NodesBuilder()
            .setNodeId(XPDR)
            .setNodeInfo(new NodeInfoBuilder().setOpenroadmVersion(OpenroadmNodeVersion._221).build())
            .build());
        // A transponder path: the network port has the interfaces looked up before deleting
        return new ServicePathInputBuilder(ServiceDataUtils.buildServicePathInputs(
                List.of(ServiceDataUtils.createNode(XPDR, "XPDR1-NETWORK1", "XPDR1-NETWORK1"))))
            .setLowerSpectralSlotNumber(Uint32.valueOf(761))
            .setHigherSpectralSlotNumber(Uint32.valueOf(768))
            .build();
    }
}
