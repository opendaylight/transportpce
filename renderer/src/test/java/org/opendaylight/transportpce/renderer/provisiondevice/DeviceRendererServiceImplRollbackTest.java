/*
 * Copyright © 2026 Smartoptics and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.renderer.provisiondevice;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.transportpce.common.crossconnect.CrossConnect;
import org.opendaylight.transportpce.common.device.DeviceTransactionManager;
import org.opendaylight.transportpce.common.mapping.MappingUtils;
import org.opendaylight.transportpce.common.mapping.PortMapping;
import org.opendaylight.transportpce.common.openroadminterfaces.OpenRoadmInterfaces;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.device.renderer.rev260212.RendererRollbackInput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.device.renderer.rev260212.RendererRollbackInputBuilder;
import org.opendaylight.yang.gen.v1.http.org.transportpce.common.types.rev260707.node.interfaces.NodeInterface;
import org.opendaylight.yang.gen.v1.http.org.transportpce.common.types.rev260707.node.interfaces.NodeInterfaceBuilder;

/**
 * Tests that renderer rollback deletes the cross connects it is given, whether they are roadm-connections or
 * odu-connections.
 */
@ExtendWith(MockitoExtension.class)
class DeviceRendererServiceImplRollbackTest {

    private static final String NODE = "XPDR-A1";
    private static final String ROADM_CONNECTION = "SRG1-PP1-TXRX-DEG1-TTP-TXRX-761:768";
    private static final String ODU_CONNECTION = "XPDR1-CLIENT1-ODU2e-x-XPDR1-NETWORK1-ODU2e";

    @Mock
    private DataBroker dataBroker;
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

    @BeforeEach
    void setup() {
        deviceRendererService = new DeviceRendererServiceImpl(dataBroker, deviceTransactionManager,
                openRoadmInterfaces, crossConnect, mappingUtils, portMapping, null);
    }

    @Test
    void rollbackDeletesRoadmConnection() throws Exception {
        when(crossConnect.deleteCrossConnect(NODE, ROADM_CONNECTION, false)).thenReturn(List.of("SRG1-PP1-TXRX"));

        assertTrue(deviceRendererService.rendererRollback(rollbackInput(ROADM_CONNECTION)).getSuccess());
        verify(crossConnect, never()).deleteCrossConnect(NODE, ROADM_CONNECTION, true);
    }

    @Test
    void rollbackDeletesOduConnection() throws Exception {
        when(crossConnect.deleteCrossConnect(NODE, ODU_CONNECTION, false)).thenReturn(null);
        when(crossConnect.deleteCrossConnect(NODE, ODU_CONNECTION, true))
            .thenReturn(List.of("XPDR1-CLIENT1-ODU2e", "XPDR1-NETWORK1-ODU2e"));

        // OTN rendering rolls back its odu-connections through the same input as roadm-connections
        assertTrue(deviceRendererService.rendererRollback(rollbackInput(ODU_CONNECTION)).getSuccess());
    }

    @Test
    void rollbackFailsWhenNoCrossConnectExists() throws Exception {
        when(crossConnect.deleteCrossConnect(NODE, ODU_CONNECTION, false)).thenReturn(null);
        when(crossConnect.deleteCrossConnect(NODE, ODU_CONNECTION, true)).thenReturn(null);

        assertFalse(deviceRendererService.rendererRollback(rollbackInput(ODU_CONNECTION)).getSuccess());
    }

    private static RendererRollbackInput rollbackInput(String connectionId) {
        NodeInterface nodeInterface = new NodeInterfaceBuilder()
            .setNodeId(NODE)
            .setConnectionId(Set.of(connectionId))
            .build();
        return new RendererRollbackInputBuilder()
            .setNodeInterface(Map.of(nodeInterface.key(), nodeInterface))
            .build();
    }
}
