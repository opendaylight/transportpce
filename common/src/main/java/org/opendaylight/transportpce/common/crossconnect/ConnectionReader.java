/*
 * Copyright © 2026 Smartoptics and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.common.crossconnect;

import java.util.Optional;
import org.eclipse.jdt.annotation.Nullable;
import org.opendaylight.transportpce.common.device.DeviceReadException;
import org.opendaylight.transportpce.common.device.DeviceTransaction;
import org.opendaylight.yangtools.binding.DataObjectIdentifier;

/**
 * Reads the cross connects of one OpenROADM device version, for {@link CrossConnectDelete}.
 */
interface ConnectionReader {

    /**
     * A cross connect read from the device: where it is, and the interfaces it names.
     *
     * @param path where the cross connect is in the device's configuration, for deleting it
     * @param srcIf the source interface, or null if the device left out src-if
     * @param dstIf the destination interface, or null if the device left out dst-if
     */
    record Connection(DataObjectIdentifier<?> path, @Nullable String srcIf, @Nullable String dstIf) {
    }

    /**
     * Reads a roadm-connection in the given transaction.
     *
     * @param deviceTx the transaction to read in
     * @param connectionName name of the roadm-connection
     * @return the roadm-connection, or an empty Optional if the device has none with this name
     * @throws DeviceReadException if the read failed or did not complete in time
     */
    Optional<Connection> readRoadmConnection(DeviceTransaction deviceTx, String connectionName)
        throws DeviceReadException;

    /**
     * Reads an odu-connection in the given transaction.
     *
     * @param deviceTx the transaction to read in
     * @param connectionName name of the odu-connection
     * @return the odu-connection, or an empty Optional if the device has none with this name
     * @throws DeviceReadException if the read failed or did not complete in time
     */
    Optional<Connection> readOduConnection(DeviceTransaction deviceTx, String connectionName)
        throws DeviceReadException;
}
