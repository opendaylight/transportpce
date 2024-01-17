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

public class DeviceInterface implements Transaction {

    private final String nodeId;

    private final String interfaceId;

    public DeviceInterface(String nodeId, String interfaceId) {
        this.nodeId = nodeId;
        this.interfaceId = interfaceId;
    }

    @Override
    public boolean rollback(Delete delete) {
        return delete.deleteInterface(nodeId, interfaceId);
    }

    @Override
    public String description() {
        return String.format("Node: %s interface id: %s", nodeId, interfaceId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(nodeId, interfaceId);
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (!(object instanceof DeviceInterface)) {
            return false;
        }
        DeviceInterface that = (DeviceInterface) object;
        return Objects.equals(nodeId, that.nodeId) && Objects.equals(interfaceId,
                that.interfaceId);
    }
}