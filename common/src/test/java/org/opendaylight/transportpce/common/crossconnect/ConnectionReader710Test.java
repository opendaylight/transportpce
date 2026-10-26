/*
 * Copyright © 2026 Smartoptics and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.common.crossconnect;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.transportpce.common.crossconnect.ConnectionReader.Connection;
import org.opendaylight.transportpce.common.device.DeviceTransaction;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev200529.org.openroadm.device.container.org.openroadm.device.OduConnection;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev200529.org.openroadm.device.container.org.openroadm.device.RoadmConnections;

class ConnectionReader710Test {

    private final ConnectionReader710 connectionReader = new ConnectionReader710();

    private final DeviceTransaction deviceTx = mock(DeviceTransaction.class);

    @Test
    void readRoadmConnectionReadsItsInterfacesAndPath() throws Exception {
        RoadmConnections xc = mock(RoadmConnections.class, RETURNS_DEEP_STUBS);
        when(xc.getSource().getSrcIf()).thenReturn("SRG1-PP1-TXRX-nmc-761:768");
        when(xc.getDestination().getDstIf()).thenReturn("DEG1-TTP-TXRX-nmc-761:768");
        doReturn(Optional.of(xc)).when(deviceTx).read(eq(LogicalDatastoreType.CONFIGURATION),
            eq(connectionReader.roadmConnectionPath("1")), anyLong(), any());

        assertEquals(
            Optional.of(new Connection(connectionReader.roadmConnectionPath("1"),
                "SRG1-PP1-TXRX-nmc-761:768", "DEG1-TTP-TXRX-nmc-761:768")),
            connectionReader.readRoadmConnection(deviceTx, "1"));
    }

    @Test
    void readOduConnectionReadsItsInterfacesAndPath() throws Exception {
        OduConnection xc = mock(OduConnection.class, RETURNS_DEEP_STUBS);
        when(xc.getSource().getSrcIf()).thenReturn("XPDR1-CLIENT1-ODU2e");
        when(xc.getDestination().getDstIf()).thenReturn("XPDR1-NETWORK1-ODU2e");
        doReturn(Optional.of(xc)).when(deviceTx).read(eq(LogicalDatastoreType.OPERATIONAL),
            eq(connectionReader.oduConnectionPath("1")), anyLong(), any());

        assertEquals(
            Optional.of(new Connection(connectionReader.oduConnectionPath("1"),
                "XPDR1-CLIENT1-ODU2e", "XPDR1-NETWORK1-ODU2e")),
            connectionReader.readOduConnection(deviceTx, "1"));
    }

    @Test
    void readRoadmConnectionIsEmptyWhenTheDeviceHasNone() throws Exception {
        doReturn(Optional.empty()).when(deviceTx).read(any(), any(), anyLong(), any());

        assertTrue(connectionReader.readRoadmConnection(deviceTx, "1").isEmpty());
    }

    @Test
    void readOduConnectionKeepsAMissingSourceAsNull() throws Exception {
        // As a device may report it: without a source
        OduConnection xc = mock(OduConnection.class, RETURNS_DEEP_STUBS);
        when(xc.getSource()).thenReturn(null);
        when(xc.getDestination().getDstIf()).thenReturn("XPDR1-NETWORK1-ODU2e");
        doReturn(Optional.of(xc)).when(deviceTx).read(any(), any(), anyLong(), any());

        assertEquals(
            Optional.of(new Connection(connectionReader.oduConnectionPath("1"), null, "XPDR1-NETWORK1-ODU2e")),
            connectionReader.readOduConnection(deviceTx, "1"));
    }
}
