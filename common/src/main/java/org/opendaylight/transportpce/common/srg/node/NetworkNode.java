/*
 * Copyright (c) 2023 Orange, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.common.srg.node;

import java.util.Map;
import org.opendaylight.transportpce.common.srg.storage.Storage;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.NetworkId;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.NodeId;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.networks.network.Node;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.networks.network.node.SupportingNode;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.networks.network.node.SupportingNodeKey;


public interface NetworkNode {

    /**
     * Iterates through a map of supporting nodes. Return the first node
     * matching the networkId.
     * @throws NodeNotFoundException if a supporting node on the network is found
     */
    NodeId supportingNode(NetworkId networkId, Map<SupportingNodeKey, SupportingNode> supportingNodes);

    /**
     * Processes the node string and returns the numeric number
     * e.g. SRG1 returns '1'
     * @throws IndexNotFoundException if no numeric value is found
     */
    int number(String node);

    boolean contentionLess(NetworkId networkId, Node node, Storage storage);

}