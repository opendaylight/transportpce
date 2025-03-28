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
import org.opendaylight.transportpce.common.openconfiginterfaces.OpenConfigInterfacesException;
import org.opendaylight.transportpce.common.openroadminterfaces.OpenRoadmInterfaceException;
import org.opendaylight.transportpce.common.openroadminterfaces.OpenRoadmInterfaces;
import org.opendaylight.transportpce.renderer.openconfiginterface.OpenConfigInterfaceFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DeleteService implements Delete {

    private final CrossConnect crossConnect;
    private final OpenRoadmInterfaces openRoadmInterfaces;
    private final OpenConfigInterfaceFactory openConfigInterfaceFactory;

    private final Subscriber subscriber;

    private static final Logger LOG = LoggerFactory.getLogger(DeleteService.class);

    public DeleteService(
            CrossConnect crossConnect,
            OpenRoadmInterfaces openRoadmInterfaces,
            OpenConfigInterfaceFactory openConfigInterfaceFactory,
            Subscriber subscriber) {
        this.crossConnect = crossConnect;
        this.openRoadmInterfaces = openRoadmInterfaces;
        this.openConfigInterfaceFactory = openConfigInterfaceFactory;
        this.subscriber = subscriber;
    }

    @Override
    public @NonNull List<String> deleteCrossConnect(String deviceId, String connectionNumber,
                                                    boolean isOtn) {
        List<String> result = crossConnect.deleteCrossConnect(deviceId, connectionNumber, isOtn);

        if (result == null) {
            subscriber.result(false, deviceId, connectionNumber);
            return new ArrayList<>();
        }

        subscriber.result(true, deviceId, connectionNumber);

        return result;
    }

    @Override
    public boolean deleteInterface(String nodeId, String interfaceId) {
        try {
            openRoadmInterfaces.deleteInterface(nodeId, interfaceId);

            subscriber.result(true, nodeId, interfaceId);
            return true;
        } catch (OpenRoadmInterfaceException e) {
            LOG.error("Failed rolling back {} {}", nodeId, interfaceId, e);
            subscriber.result(false, nodeId, interfaceId);
            return false;
        }
    }

    /**
     * Disable the admin-state of a network/client component during rollback operation.
     *
     * @param nodeId nodeId in which port is to be disabled.
     * @param portId portId to be disabled
     * @return true/false
     */

    @Override
    public boolean disablePort(String nodeId, String portId) {
        try {
            openConfigInterfaceFactory.disablePortAdminState(nodeId, portId);
            LOG.info("Successfully disabled port {} on node {}", portId, nodeId);
            subscriber.result(true, nodeId, portId);
            return true;
        } catch (OpenConfigInterfacesException e) {
            LOG.error("Failed rolling back {} on node {}", portId, nodeId, e);
            subscriber.result(false, nodeId, portId);
            return false;
        }
    }

}