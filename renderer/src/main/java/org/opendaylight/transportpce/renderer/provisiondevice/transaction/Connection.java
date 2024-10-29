/*
 * Copyright © 2024 Smartoptics and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.renderer.provisiondevice.transaction;

import java.util.List;
import java.util.Objects;
import org.opendaylight.transportpce.renderer.provisiondevice.transaction.delete.Delete;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Connection transaction.
 *
 * <p>i.e. a class tracking a connection.
 */
public class Connection implements Transaction {

    private static final Logger LOG = LoggerFactory.getLogger(Connection.class);
    private final String deviceId;
    private final String connectionNumber;
    private final boolean isOtn;

    public Connection(String deviceId, String connectionNumber, boolean isOtn) {
        this.deviceId = deviceId;
        this.connectionNumber = connectionNumber;
        this.isOtn = isOtn;
    }

    @Override
    public boolean rollback(Delete delete) {
        List<String> supportingInterfaces = delete.deleteCrossConnect(deviceId, connectionNumber, isOtn);

        if (supportingInterfaces == null || supportingInterfaces.size() == 0) {
            return false;
        }

        LOG.info("Supporting interfaces {} affected by rollback on {} {}",
                String.join(", ", supportingInterfaces), deviceId, connectionNumber);

        return true;

    }

    @Override
    public String description() {
        return String.format("Connection %s connection number %s isOtn %s", deviceId,
                connectionNumber, isOtn);
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (!(object instanceof Connection)) {
            return false;
        }
        Connection that = (Connection) object;
        return isOtn == that.isOtn && Objects.equals(deviceId, that.deviceId)
                && Objects.equals(connectionNumber, that.connectionNumber);
    }

    @Override
    public int hashCode() {
        return Objects.hash(deviceId, connectionNumber, isOtn);
    }
}
