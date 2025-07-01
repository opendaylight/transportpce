/*
 * Copyright © 2025 Smartoptics and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.tapi.openroadm.topology.terminationpoint;

import java.util.List;
import java.util.Map;
import java.util.Set;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.types.rev230526.OpenroadmNodeType;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.networks.network.node.TerminationPoint;

public interface Collection {

    /**
     * Adds a set of termination points to the collection, grouped by OpenROADM node type and supporting node name.
     *
     * @param openroadmNodeType The OpenROADM node type to which the termination points belong.
     * @param supportingNodeName The name of the supporting node for these termination points.
     * @param terminationPoint A set of termination points to be added.
     * @return true if the addition was successful, false otherwise.
     */
    boolean add(OpenroadmNodeType openroadmNodeType, String supportingNodeName, Set<TerminationPoint> terminationPoint);

    /**
     * Add all termination points from another collection to this collection.
     *
     * @param collection The collection of termination points to be added.
     * @return true if the addition was successful, false otherwise.
     */
    boolean add(Collection collection);

    /**
     * Returns a map of termination points grouped by OpenROADM node types.
     */
    Map<OpenroadmNodeType, Map<String, Set<TerminationPoint>>> terminationPoints();

    /**
     * Filter and returns a map of termination points grouped by OpenROADM node types.
     * Ensures a particular OpenroadmNodeType is present in the returned map. The list of OpenroamdNodeType
     * will always be present in the returned Map.
     *
     * @param openroadmNodeTypes List of OpenROADM node types to filter the termination points.
     * @return A list of OpenroadmNodeType one wishes to be returned.
     */
    Map<OpenroadmNodeType, Map<String, Set<TerminationPoint>>> terminationPoints(
            List<OpenroadmNodeType> openroadmNodeTypes);

}
