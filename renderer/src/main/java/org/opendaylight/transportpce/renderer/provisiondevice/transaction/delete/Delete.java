/*
 * Copyright © 2024 Smartoptics and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.renderer.provisiondevice.transaction.delete;

import java.util.List;

/**
 * A class capable of deleting service connections/interfaces
 * may implement this interface.
 */
public interface Delete {

    /**
     * Delete cross connection.
     * Typically, deleted before interfaces.
     */
    List<String> deleteCrossConnect(String deviceId, String connectionNumber, boolean isOtn);

    /**
     * Delete an interface.
     * Typically, deleted after the cross connection.
     */
    boolean deleteInterface(String nodeId, String interfaceId);
}