/*
 * Copyright © 2026 Smartoptics and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.common.mapping;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.binding.api.WriteTransaction;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.transportpce.common.device.DeviceTransactionManager;
import org.opendaylight.transportpce.common.mapping.connectionmap.Factory;
import org.opendaylight.transportpce.common.mapping.connectionmap.storage.Storage;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.types.rev181019.NodeTypes;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev181019.OrgOpenroadmDeviceData;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev181019.org.openroadm.device.container.OrgOpenroadmDevice;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev181019.org.openroadm.device.container.org.openroadm.device.Info;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev181019.org.openroadm.device.container.org.openroadm.device.InfoBuilder;
import org.opendaylight.yangtools.binding.DataObjectIdentifier;
import org.opendaylight.yangtools.util.concurrent.FluentFutures;

class PortMappingVersion221Test {

    private static final String NODE_ID = "node1";

    private DataBroker dataBroker;
    private DeviceTransactionManager deviceTransactionManager;
    private Factory connectionMapStorageFactory;
    private Storage connectionMapStorage;
    private PortMappingVersion221 portMapping;

    @BeforeEach
    void setUp() {
        dataBroker = mock(DataBroker.class);
        deviceTransactionManager = mock(DeviceTransactionManager.class);
        connectionMapStorageFactory = mock(Factory.class);
        connectionMapStorage = mock(Storage.class);
        when(connectionMapStorageFactory.storage(dataBroker)).thenReturn(connectionMapStorage);
        portMapping =
            new PortMappingVersion221(dataBroker, deviceTransactionManager, connectionMapStorageFactory);

        WriteTransaction writeTransaction = mock(WriteTransaction.class);
        when(writeTransaction.commit()).thenReturn(FluentFutures.immediateNullFluentFuture());
        when(dataBroker.newWriteOnlyTransaction()).thenReturn(writeTransaction);

        Info rdmInfo = new InfoBuilder().setNodeType(NodeTypes.Rdm).build();
        DataObjectIdentifier<Info> infoIID = DataObjectIdentifier
            .builderOfInherited(OrgOpenroadmDeviceData.class, OrgOpenroadmDevice.class)
            .child(Info.class)
            .build();
        when(deviceTransactionManager.getDataFromDevice(
                eq(NODE_ID), eq(LogicalDatastoreType.OPERATIONAL), eq(infoIID), anyLong(), any()))
            .thenReturn(Optional.of(rdmInfo));
    }

    private void stubDeviceRead(Optional<OrgOpenroadmDevice> device) {
        DataObjectIdentifier<OrgOpenroadmDevice> deviceIID = DataObjectIdentifier
            .builderOfInherited(OrgOpenroadmDeviceData.class, OrgOpenroadmDevice.class)
            .build();
        when(deviceTransactionManager.getDataFromDevice(
                eq(NODE_ID), eq(LogicalDatastoreType.OPERATIONAL), eq(deviceIID), anyLong(), any()))
            .thenReturn(device);
    }

    @Test
    void createMappingDataSavesConnectionMapWhenDeviceIsPresent() {
        stubDeviceRead(Optional.of(mock(OrgOpenroadmDevice.class)));
        when(connectionMapStorage.saveRev181019(eq(NODE_ID), any())).thenReturn(true);

        assertTrue(portMapping.createMappingData(NODE_ID));
        verify(connectionMapStorage, times(1)).saveRev181019(any(), any());
    }

    @Test
    void createMappingDataFailsWhenConnectionMapDeviceReadIsEmpty() {
        stubDeviceRead(Optional.empty());

        assertFalse(portMapping.createMappingData(NODE_ID));
        verify(connectionMapStorage, never()).saveRev181019(any(), any());
    }

    @Test
    void createMappingDataFailsWhenSavingConnectionMapFails() {
        stubDeviceRead(Optional.of(mock(OrgOpenroadmDevice.class)));
        when(connectionMapStorage.saveRev181019(eq(NODE_ID), any())).thenReturn(false);

        assertFalse(portMapping.createMappingData(NODE_ID));
        verify(connectionMapStorage, times(1)).saveRev181019(any(), any());
    }
}
