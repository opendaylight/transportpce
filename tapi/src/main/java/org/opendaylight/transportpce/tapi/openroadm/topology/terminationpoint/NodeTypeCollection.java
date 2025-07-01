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
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.types.rev250110.OpenroadmNodeType;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.networks.network.Node;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.networks.network.node.TerminationPoint;

public interface NodeTypeCollection {

    /**
     * Adds a set of termination points to the collection, grouped by OpenROADM node type and supporting node name.
     *
     * @param openroadmNodeType The OpenROADM node type to which the termination points belong
     * @param supportingNodeName The name of the supporting node for these termination points
     * @param terminationPoints A set of termination points to add
     * @return {@code true} if the collection changed as a result of this call
     */
    boolean add(
            OpenroadmNodeType openroadmNodeType,
            String supportingNodeName,
            Set<TerminationPoint> terminationPoints);

    /**
     * Adds a set of termination points to the collection, grouped by OpenROADM node type and supporting node name.
     *
     * @param node The OpenROADM node to which the termination points belong
     * @param supportingNodeName The name of the supporting node for these termination points
     * @param terminationPoints A set of termination points to add
     * @return {@code true} if the collection changed as a result of this call
     * @see #add(OpenroadmNodeType, String, Set)
     */
    boolean add(Node node, String supportingNodeName, Set<TerminationPoint> terminationPoints);

    /**
     * Add all termination points from another collection to this collection.
     *
     * @param collection The collection of termination points to add
     * @return true if the addition was successful
     */
    boolean addAll(NodeTypeCollection collection);

    /**
     * Returns a map of termination points grouped by OpenROADM node types and supporting node name.
     *
     * @return the termination point map
     */
    Map<OpenroadmNodeType, Map<String, Set<TerminationPoint>>> terminationPoints();

    /**
     * Filters and returns termination points grouped by OpenROADM node type and supporting
     * node name. The returned map contains an entry for each requested node type (possibly
     * empty).
     *
     * @param openroadmNodeTypes the node types to include
     * @return the filtered termination point map
     */
    Map<OpenroadmNodeType, Map<String, Set<TerminationPoint>>> terminationPoints(
            List<OpenroadmNodeType> openroadmNodeTypes);

    /**
     * Filters and returns termination points grouped by OpenROADM node type and supporting
     * node name. The returned map contains an entry for the requested node type (possibly
     * empty).
     *
     * @param openroadmNodeType the node type to include
     * @return the filtered termination point map
     */
    Map<OpenroadmNodeType, Map<String, Set<TerminationPoint>>> terminationPoints(OpenroadmNodeType openroadmNodeType);

}
