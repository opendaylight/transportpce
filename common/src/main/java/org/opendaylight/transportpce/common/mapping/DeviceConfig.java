/*
 * Copyright © 2017 Orange, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.common.mapping;

import java.util.List;

import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.CircuitPack;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.circuit.pack.Ports;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.org.openroadm.device.container.org.openroadm.device.ConnectionMap;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.org.openroadm.device.container.org.openroadm.device.Degree;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.org.openroadm.device.container.org.openroadm.device.Info;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.org.openroadm.device.container.org.openroadm.device.SharedRiskGroup;

public interface DeviceConfig {

    /**
     * This method retrieves info container from operational datastore of a
     * given device.
     *
     * @param nodeId
     *            node ID
     *
     * @return Info container or null if info container is absent
     */
    Info getDeviceInfo(String nodeId);

    /**
     * This method retrieves given ports container of a given circuit-pack from
     * operational datastore of a given device.
     *
     * @param nodeId
     *            node ID
     * @param circuitPackName
     *            String
     * @param portName
     *            String
     *
     * @return Ports container or null if Ports container is absent
     */
    Ports getDevicePorts(String nodeId, String circuitPackName, String portName);

    /**
     * This method retrieves given degree container from operational datastore of a
     * given device.
     *
     * @param nodeId
     *            node ID
     * @param degreeNb
     *            degree number
     *
     * @return degree container or null if container is absent
     */
    Degree getDeviceDegree(String nodeId, int degreeNb);

    /**
     * This method retrieves given srg container from operational datastore of a
     * given device.
     *
     * @param nodeId
     *            node ID
     * @param srgNb
     *            String
     *
     * @return shared-risk-group container or null if container is absent
     */
    SharedRiskGroup getDeviceSrg(String nodeId, int srgNb);

    /**
     * This method retrieves given circuit-pack container from operational datastore of a
     * given device.
     *
     * @param nodeId
     *            node ID
     * @param cpName
     *            String
     *
     * @return circuit-pack container or null if container is absent
     */
    CircuitPack getDeviceCp(String nodeId, String cpName);

    /**
     * This method retrieves the connection-map list from operational datastore of a
     * given device.
     *
     * @param NodeId
     *            node ID
     *
     * @return connection-map list or null if container is absent
     */
    List<ConnectionMap> getConnectionMap(String NodeId);

}
