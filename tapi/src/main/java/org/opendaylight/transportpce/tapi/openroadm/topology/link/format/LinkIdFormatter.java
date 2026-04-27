/*
 * Copyright © 2026 Smartoptics and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.tapi.openroadm.topology.link.format;

/**
 * Formats the TAPI link identifier for a link derived from an OpenROADM topology link.
 *
 * <p>Implementations define the naming strategy used to build the identifier from the
 * source and destination node and termination point identifiers of the underlying
 * OpenROADM topology link.</p>
 */
public interface LinkIdFormatter {

    /**
     * Builds the link identifier from the given OpenROADM topology node and
     * termination point identifiers.
     *
     * @param srcOpenRoadmTopologyNodeId the source node identifier in the OpenROADM topology
     * @param srcOpenRoadmTopologyTerminationPointId the source termination point identifier
     *                                               in the OpenROADM topology
     * @param destOpenRoadmTopologyNodeId the destination node identifier in the OpenROADM topology
     * @param destOpenRoadmTopologyTerminationPointId the destination termination point identifier
     *                                                in the OpenROADM topology
     * @return the formatted link identifier
     */
    String linkId(
            String srcOpenRoadmTopologyNodeId,
            String srcOpenRoadmTopologyTerminationPointId,
            String destOpenRoadmTopologyNodeId,
            String destOpenRoadmTopologyTerminationPointId);
}
