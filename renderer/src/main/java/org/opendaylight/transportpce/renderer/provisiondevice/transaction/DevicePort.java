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
 * Device Port class is used during the rollback operation of line-port component.
 */

public class DevicePort implements Transaction {

    private final String nodeId;

    private final String portId;

    public DevicePort(String nodeId, String portId) {
        this.nodeId = nodeId;
        this.portId = portId;
    }

    @Override
    public boolean rollback(Delete delete) {
        return delete.disablePort(nodeId, portId);
    }

    @Override
    public String description() {
        return String.format("Node: %s port id: %s", nodeId, portId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(nodeId, portId);
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (!(object instanceof DevicePort)) {
            return false;
        }
        DevicePort that = (DevicePort) object;
        return Objects.equals(nodeId, that.nodeId) && Objects.equals(portId,
                that.portId);
    }
}