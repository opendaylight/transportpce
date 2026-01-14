/*
 * Copyright © 2026 Smartoptics and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.tapi.openroadm.topology.datastore;

import java.util.Optional;
import org.opendaylight.transportpce.tapi.openroadm.TopologyNodeId;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev250110.TerminationPoint1;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.TpId;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.networks.network.node.TerminationPoint;

/**
 * Reads termination point data from an OpenROADM topology source.
 *
 * <p>This reader provides access to:
 * <ul>
 *   <li>the base IETF {@link TerminationPoint}</li>
 *   <li>the OpenROADM "common network"
 *       {@link org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev250110.TerminationPoint1}
 *       augmentation</li>
 *   <li>the OpenROADM "network topology"
 *       {@link org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev250110.TerminationPoint1}
 *       augmentation</li>
 * </ul>
 *
 * <p>Methods return {@link Optional#empty()} when the termination point (or augmentation) is not present
 * or could not be read.
 */
public interface OpenRoadmTerminationPointReader {

    /**
     * Reads the base IETF termination point from the OpenROADM topology.
     *
     * @see #readTerminationPoint(TopologyNodeId, TpId)
     */
    Optional<TerminationPoint> readTerminationPoint(TopologyNodeId nodeId, String tpId);

    /**
     * Reads the base IETF termination point from the OpenROADM topology.
     *
     * @param nodeId OpenROADM topology node identifier
     * @param tpId termination point identifier on the node
     * @return the {@link TerminationPoint} if present
     */
    Optional<TerminationPoint> readTerminationPoint(TopologyNodeId nodeId, TpId tpId);

    /**
     * Reads the OpenROADM "common network" {@code TerminationPoint1} augmentation for a termination point.
     *
     * @see #readCommonTerminationPoint1(TopologyNodeId, TpId)
     */
    Optional<TerminationPoint1> readCommonTerminationPoint1(TopologyNodeId nodeId, String tpId);

    /**
     * Reads the OpenROADM "common network" {@code TerminationPoint1} augmentation for a termination point.
     *
     * @param nodeId OpenROADM topology node identifier
     * @param tpId termination point identifier on the node
     * @return the common-network {@code TerminationPoint1} augmentation if present
     */
    Optional<TerminationPoint1> readCommonTerminationPoint1(TopologyNodeId nodeId, TpId tpId);

    /**
     * Reads the OpenROADM "network topology" {@code TerminationPoint1} augmentation for a termination point.
     *
     * @see #readTopologyTerminationPoint1(TopologyNodeId, TpId)
     */
    Optional<org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev250110
            .TerminationPoint1> readTopologyTerminationPoint1(TopologyNodeId nodeId, String tpId);

    /**
     * Reads the OpenROADM "network topology" {@code TerminationPoint1} augmentation for a termination point.
     *
     * @param nodeId OpenROADM topology node identifier
     * @param tpId termination point identifier on the node
     * @return the network-topology {@code TerminationPoint1} augmentation if present
     */
    Optional<org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev250110
            .TerminationPoint1> readTopologyTerminationPoint1(TopologyNodeId nodeId, TpId tpId);

}
