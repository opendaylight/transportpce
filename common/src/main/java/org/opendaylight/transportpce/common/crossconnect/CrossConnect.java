/*
 * Copyright © 2017 AT&T and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.common.crossconnect;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.opendaylight.transportpce.common.openroadminterfaces.OpenRoadmInterfaceException;
import org.opendaylight.yang.gen.v1.http.org.transportpce.common.types.rev200128.otn.renderer.input.Nodes;

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
     * @return optional of Roadm connection subtree from the device.
     */
    Optional<?> getCrossConnect(String deviceId, String connectionNumber);

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
     * @param isOtn
     *            True for odu-connection, False for roadm-connection.
     *
     * @return true/false based on status of operation.
     */
    List<String> deleteCrossConnect(String deviceId, String connectionNumber, Boolean isOtn);

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
     *
     * @return list of Ports object type.
     *
     * @throws OpenRoadmInterfaceException
     *            an exception at OpenRoadm interface.
     */
    List<?> getConnectionPortTrail(String nodeId, Long waveNumber, String srcTp, String destTp)
            throws OpenRoadmInterfaceException;

    /**
     * This method does an edit-config on roadm connection subtree for a given
     * connection number in order to set power level for use by the optical
     * power control.
     *
     * @param deviceId
     *            Device id.
     * @param mode
     *            Optical control modelcan be off, power or gainLoss.
     * @param powerValue
     *            Power value in DBm.
     * @param connectionNumber
     *            Name of the cross connect.
     * @return true/false based on status of operation.
     */
    boolean setPowerLevel(String deviceId, String mode, BigDecimal powerValue,
                          String connectionNumber);

    Optional<String> postOtnCrossConnect(List<String> createdOduInterfaces, Nodes node) throws
            OpenRoadmInterfaceException;

}
