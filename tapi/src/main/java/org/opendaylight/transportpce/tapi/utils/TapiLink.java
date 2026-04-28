/*
 * Copyright © 2023 Orange, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.tapi.utils;

import java.util.Map;
import org.opendaylight.transportpce.tapi.openroadm.topology.link.LinkResolver;
import org.opendaylight.transportpce.tapi.openroadm.topology.link.LinkTerminationPointsFactory;
import org.opendaylight.transportpce.tapi.openroadm.topology.link.state.LinkStateResolver;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.networks.Network;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.Uuid;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev221121.cep.list.ConnectionEndPoint;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.topology.Link;

/**
 * Service for building TAPI links and resolving their associated state and CEP data.
 *
 * <p>This interface provides utility methods to:
 * <ul>
 *   <li>create a TAPI {@link Link} between two node edge points,</li>
 *   <li>translate OpenROADM administrative and operational states into TAPI states,</li>
 *   <li>retrieve the effective state of a link from its endpoint NEPs, and</li>
 *   <li>expose the map of generated TAPI connection end points (CEPs).</li>
 * </ul>
 */
@SuppressWarnings("checkstyle:LineLength")
public interface TapiLink {

    /**
     * Creates a TAPI link between a source and destination termination point.
     *
     * @param srcOpenRoadmTopologyNodeId node id (e.g. ROADM-A1-SRG1)
     * @param srcOpenRoadmTopologyTerminationPointId Relative to srcOpenRoadmTopologyNodeId (e.g. SRG1-PP2-TXRX)
     * @param destOpenRoadmTopologyNodeId node id (ROADM-B1-SRG2)
     * @param destOpenRoadmTopologyTerminationPointId Relative to destOpenRoadmTopologyNodeId (e.g. SRG2-PP3-TXRX)
     * @param network OpenROADM topology (i.e. openroadm-topology)
     * @param tapiTopoUuid UUID of the TAPI topology that will contain the link
     * @param linkResolver resolver used to find the OpenROADM link in the topology
     * @return the created TAPI link, or {@code null} if the link type is not recognized
     *         or the link cannot be created
     * @see #createTapiLink(
     *     org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.networks.network.Link,
     *     Network,
     *     Uuid,
     *     LinkTerminationPointsFactory)
     */
    Link createTapiLink(
            String srcOpenRoadmTopologyNodeId,
            String srcOpenRoadmTopologyTerminationPointId,
            String destOpenRoadmTopologyNodeId,
            String destOpenRoadmTopologyTerminationPointId,
            Network network,
            Uuid tapiTopoUuid,
            LinkResolver linkResolver);

    /**
     * Creates a TAPI link between a source and destination termination point.
     *
     * <p>The link is built from the supplied openroadm link,
     * and the UUID of the target TAPI topology.
     *
     * @see #createTapiLink(
     *     org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.networks.network.Link,
     *     Network,
     *     Uuid,
     *     LinkTerminationPointsFactory,
     *     LinkStateResolver)
     */
    Link createTapiLink(
            org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226
                    .networks.network.Link openRoadmLink,
            Network network,
            Uuid tapiTopoUuid,
            LinkTerminationPointsFactory linkTerminationPointsFactory);

    /**
     * Creates a TAPI link between a source and destination termination point.
     *
     * <p>The link is built from the supplied openroadm link,
     * and the UUID of the target TAPI topology.
     *
     * @param openRoadmLink The openroadm link being translated into a TAPI link
     * @param network OpenROADM topology
     * @param tapiTopoUuid UUID of the TAPI topology that will contain the link
     * @param linkTerminationPointsFactory Primarily used to validate the given link against the topology.
     * @param linkStateResolver Determine the administrative state and operational state for a link
     * @return the created TAPI link, or {@code null} if the link type is not recognized
     *         or the link cannot be created
     */
    Link createTapiLink(
            org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226
                    .networks.network.Link openRoadmLink,
            Network network,
            Uuid tapiTopoUuid,
            LinkTerminationPointsFactory linkTerminationPointsFactory,
            LinkStateResolver linkStateResolver);

    /**
     * Retrieves the effective operational state of a link from the corresponding source and destination NEPs.
     *
     * @param srcNodeId source node identifier
     * @param destNodeId destination node identifier
     * @param sourceTpId source termination point identifier
     * @param destTpId destination termination point identifier
     * @return the effective operational state name, or {@code null} if it cannot be resolved
     */
    String getOperState(String srcNodeId, String destNodeId, String sourceTpId, String destTpId);

    /**
     * Retrieves the effective administrative state of a link from the corresponding source and destination NEPs.
     *
     * @param srcNodeId source node identifier
     * @param destNodeId destination node identifier
     * @param sourceTpId source termination point identifier
     * @param destTpId destination termination point identifier
     * @return the effective administrative state name, or {@code null} if it cannot be resolved
     */
    String getAdminState(String srcNodeId, String destNodeId, String sourceTpId, String destTpId);

    /**
     * Returns the map of generated TAPI connection end points indexed by their associated UUID mapping.
     *
     * @return a map containing generated CEPs
     */
    Map<Map<String, String>, ConnectionEndPoint> getCepMap();
}
