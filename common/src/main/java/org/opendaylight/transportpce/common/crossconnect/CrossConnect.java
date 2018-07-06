/*
 * Copyright © 2017 AT&T and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.common.crossconnect;

import java.util.List;
import java.util.Optional;

import org.opendaylight.transportpce.common.openroadminterfaces.OpenRoadmInterfaceException;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.get.connection.port.trail.output.Ports;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.org.openroadm.device.container.org.openroadm.device.RoadmConnections;

public interface CrossConnect {

    /**
     * This method return the RoadmConnection subtree for a given connection
     * number.
     *
     * @param deviceId
     *            Device id.
     * @param connectionNumber
     *            Name of the cross connect.
     *
     * @return Roadm connection subtree from the device.
     */
    Optional<RoadmConnections> getCrossConnect(String deviceId, String connectionNumber);

    /**
     * This method does a post(edit-config) on roadm connection subtree for a
     * given connection number.
     *
     * @param deviceId
     *            Device id.
     * @param waveNumber
     *            Wavelength number.
     * @param srcTp
     *            Name of source termination point.
     * @param destTp
     *            Name of destination termination point.
     * @return optional of connection number
     */
    Optional<String> postCrossConnect(String deviceId, Long waveNumber, String srcTp, String destTp);

    /**
     * This method does a delete(edit-config) on roadm connection subtree for a
     * given connection number.
     *
     * @param deviceId
     *            Device id.
     * @param connectionNumber
     *            Name of the cross connect.
     *
     * @return true/false based on status of operation.
     */

    boolean deleteCrossConnect(String deviceId, String connectionNumber);

    /**
     * This public method returns the list of ports (port-trail) for a roadm's
     * cross connect. It calls rpc get-port-trail on device. To be used store
     * detailed path description.
     *
     * @param nodeId
     *            node-id of NE.
     * @param waveNumber
     *            Wavelength number.
     * @param srcTp
     *            Source logical connection point.
     * @param destTp
     *            Destination logical connection point.
     * @throws OpenRoadmInterfaceException
     *            OpenRoadm Interface Exception.
     *
     * @return list of Ports object type.
     */
    List<Ports> getConnectionPortTrail(String nodeId, Long waveNumber, String srcTp, String destTp)
            throws OpenRoadmInterfaceException;
}
