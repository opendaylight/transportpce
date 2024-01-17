/*
 * Copyright © 2024 Smartoptics and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.renderer.provisiondevice.transaction.delete;

import java.util.ArrayList;
import java.util.List;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.transportpce.common.crossconnect.CrossConnect;
import org.opendaylight.transportpce.common.openroadminterfaces.OpenRoadmInterfaceException;
import org.opendaylight.transportpce.common.openroadminterfaces.OpenRoadmInterfaces;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DeleteService implements Delete {

    private final CrossConnect crossConnect;
    private final OpenRoadmInterfaces openRoadmInterfaces;

    private final Subscriber subscriber;

    private static final Logger LOG = LoggerFactory.getLogger(DeleteService.class);

    public DeleteService(
            CrossConnect crossConnect,
            OpenRoadmInterfaces openRoadmInterfaces,
            Subscriber subscriber) {
        this.crossConnect = crossConnect;
        this.openRoadmInterfaces = openRoadmInterfaces;
        this.subscriber = subscriber;
    }

    @Override
    public @NonNull List<String> deleteCrossConnect(String deviceId, String connectionNumber,
                                                    boolean isOtn) {
        List<String> result = crossConnect.deleteCrossConnect(deviceId, connectionNumber, isOtn);

        subscriber.result(result != null, deviceId, connectionNumber);

        if (result == null) {
            return new ArrayList<>();
        }

        return result;
    }

    @Override
    public boolean deleteInterface(String nodeId, String interfaceId) {
        try {
            openRoadmInterfaces.deleteInterface(nodeId, interfaceId);

            subscriber.result(true, nodeId, interfaceId);
            return true;
        } catch (OpenRoadmInterfaceException e) {
            LOG.error("Failed rolling back {} {}", nodeId, interfaceId);
            subscriber.result(false, nodeId, interfaceId);
            return false;
        }
    }
}