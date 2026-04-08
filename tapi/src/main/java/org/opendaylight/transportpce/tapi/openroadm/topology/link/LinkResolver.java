/*
 * Copyright © 2026 Smartoptics and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.tapi.openroadm.topology.link;

import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.networks.Network;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.networks.network.Link;

public interface LinkResolver {

    /**
     * Find the link in the openroadm-topology.
     *
     * @param srcOpenRoadmTopologyNodeId node id (e.g. ROADM-A1-SRG1)
     * @param srcOpenRoadmTopologyTerminationPointId Relative to srcOpenRoadmTopologyNodeId (e.g. SRG1-PP2-TXRX)
     * @param destOpenRoadmTopologyNodeId node id (ROADM-B1-SRG2)
     * @param destOpenRoadmTopologyTerminationPointId Relative to destOpenRoadmTopologyNodeId (e.g. SRG2-PP3-TXRX)
     * @param network OpenROADM topology (i.e. openroadm-topology)
     * @return OpenROADM link
     * @throws LinkNotFoundException If no link is found in the topology.
     */
    Link resolveLink(
            String srcOpenRoadmTopologyNodeId,
            String srcOpenRoadmTopologyTerminationPointId,
            String destOpenRoadmTopologyNodeId,
            String destOpenRoadmTopologyTerminationPointId,
            Network network);

}
