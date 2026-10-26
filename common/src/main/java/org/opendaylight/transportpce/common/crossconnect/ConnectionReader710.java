/*
 * Copyright © 2026 Smartoptics and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.common.crossconnect;

import java.util.Optional;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.transportpce.common.Timeouts;
import org.opendaylight.transportpce.common.device.DeviceReadException;
import org.opendaylight.transportpce.common.device.DeviceTransaction;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev200529.OrgOpenroadmDeviceData;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev200529.org.openroadm.device.container.OrgOpenroadmDevice;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev200529.org.openroadm.device.container.org.openroadm.device.OduConnection;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev200529.org.openroadm.device.container.org.openroadm.device.OduConnectionKey;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev200529.org.openroadm.device.container.org.openroadm.device.RoadmConnections;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev200529.org.openroadm.device.container.org.openroadm.device.RoadmConnectionsKey;
import org.opendaylight.yangtools.binding.DataObjectIdentifier;

/**
 * Reads the cross connects of OpenROADM 7.1 devices, and knows where they are.
 */
final class ConnectionReader710 implements ConnectionReader {

    // Where cross connects are read from. TODO Change roadm-connections to Operational later
    // for real device
    static final LogicalDatastoreType ROADM_CONNECTION_STORE = LogicalDatastoreType.CONFIGURATION;
    static final LogicalDatastoreType ODU_CONNECTION_STORE = LogicalDatastoreType.OPERATIONAL;

    @Override
    public Optional<Connection> readRoadmConnection(DeviceTransaction deviceTx, String connectionName)
            throws DeviceReadException {
        DataObjectIdentifier<RoadmConnections> path = roadmConnectionPath(connectionName);
        return deviceTx.read(ROADM_CONNECTION_STORE, path, Timeouts.DEVICE_READ_TIMEOUT,
                Timeouts.DEVICE_READ_TIMEOUT_UNIT)
            .map(xc -> new Connection(path,
                xc.getSource() == null ? null : xc.getSource().getSrcIf(),
                xc.getDestination() == null ? null : xc.getDestination().getDstIf()));
    }

    @Override
    public Optional<Connection> readOduConnection(DeviceTransaction deviceTx, String connectionName)
            throws DeviceReadException {
        DataObjectIdentifier<OduConnection> path = oduConnectionPath(connectionName);
        return deviceTx.read(ODU_CONNECTION_STORE, path, Timeouts.DEVICE_READ_TIMEOUT,
                Timeouts.DEVICE_READ_TIMEOUT_UNIT)
            .map(xc -> new Connection(path,
                xc.getSource() == null ? null : xc.getSource().getSrcIf(),
                xc.getDestination() == null ? null : xc.getDestination().getDstIf()));
    }

    /**
     * Returns where the roadm-connection with the given name is in the device's datastore.
     */
    DataObjectIdentifier<RoadmConnections> roadmConnectionPath(String connectionName) {
        return DataObjectIdentifier.builderOfInherited(OrgOpenroadmDeviceData.class, OrgOpenroadmDevice.class)
            .child(RoadmConnections.class, new RoadmConnectionsKey(connectionName))
            .build();
    }

    /**
     * Returns where the odu-connection with the given name is in the device's datastore.
     */
    DataObjectIdentifier<OduConnection> oduConnectionPath(String connectionName) {
        return DataObjectIdentifier.builderOfInherited(OrgOpenroadmDeviceData.class, OrgOpenroadmDevice.class)
            .child(OduConnection.class, new OduConnectionKey(connectionName))
            .build();
    }
}
