/*
 * Copyright © 2025 Smartoptics and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.tapi.openroadm.topology;

import java.util.Map;
import java.util.Set;
import org.opendaylight.transportpce.tapi.openroadm.topology.terminationpoint.NodeTerminationPoint;
import org.opendaylight.transportpce.tapi.openroadm.topology.terminationpoint.NodeTypeCollection;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.networks.network.Node;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.networks.network.NodeKey;

public interface ORTopology {

    Set<Map.Entry<NodeKey, Node>> topology();

    NodeTypeCollection terminationPoints(NodeTerminationPoint nodeTerminationPoint);

}
