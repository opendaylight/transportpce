/*
 * Copyright © 2024 Smartoptics and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.renderer.provisiondevice.transaction;

import java.util.Objects;
import org.opendaylight.transportpce.renderer.provisiondevice.transaction.delete.Delete;

/**
 * Device transceiver class is used during the rollback operation of line and client transceiver.
 */

public class DeviceTransceiver implements Transaction {

    private final String nodeId;

    private final String transceiverId;

    public DeviceTransceiver(String nodeId, String transceiverId) {
        this.nodeId = nodeId;
        this.transceiverId = transceiverId;
    }

    @Override
    public boolean rollback(Delete delete) {
        return delete.disableTransceiversTxLaser(nodeId, transceiverId);
    }

    @Override
    public String description() {
        return String.format("Node: %s port id: %s", nodeId, transceiverId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(nodeId, transceiverId);
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (!(object instanceof DeviceTransceiver)) {
            return false;
        }
        DeviceTransceiver that = (DeviceTransceiver) object;
        return Objects.equals(nodeId, that.nodeId) && Objects.equals(transceiverId,
                that.transceiverId);
    }
}