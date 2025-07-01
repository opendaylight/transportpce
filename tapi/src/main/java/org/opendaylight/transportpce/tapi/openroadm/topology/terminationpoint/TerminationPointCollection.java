/*
 * Copyright © 2025 Smartoptics and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.tapi.openroadm.topology.terminationpoint;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev250110.Node1;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.types.rev250110.OpenroadmNodeType;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.networks.network.Node;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.networks.network.node.TerminationPoint;

public class TerminationPointCollection implements NodeTypeCollection {

    private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(TerminationPointCollection.class);

    private final Map<OpenroadmNodeType, Map<String, Set<TerminationPoint>>> terminationPoints;

    public TerminationPointCollection() {
        terminationPoints = new HashMap<>();
    }

    @Override
    public boolean add(OpenroadmNodeType openroadmNodeType, String supportingNodeName,
            Set<TerminationPoint> terminationPoint) {

        if (openroadmNodeType == null || supportingNodeName == null || terminationPoint == null) {
            LOG.error("Invalid null parameter(s): openroadmNodeType={}, supportingNodeName={}, terminationPoint={}",
                openroadmNodeType, supportingNodeName, terminationPoint);
            return false;
        } else if (!terminationPoints.containsKey(openroadmNodeType)) {
            terminationPoints.put(openroadmNodeType, new HashMap<>());
        }

        //This song and dance routine is an attempt to avoid trying to modify an immutable set.
        Set<TerminationPoint> temp = new HashSet<>(terminationPoint);
        if (terminationPoints.get(openroadmNodeType).containsKey(supportingNodeName)) {
            temp.addAll(terminationPoints.get(openroadmNodeType).get(supportingNodeName));
        }
        terminationPoints.get(openroadmNodeType).put(supportingNodeName, temp);
        return true;
    }

    @Override
    public boolean add(Node node, String supportingNodeName, Set<TerminationPoint> terminationPoint) {
        OpenroadmNodeType nodeType = node.augmentationOrElseThrow(Node1.class).getNodeType();
        return add(nodeType, supportingNodeName, terminationPoint);
    }

    @Override
    public boolean addAll(NodeTypeCollection collection) {
        collection.terminationPoints().forEach((type, supportingNodeMap) -> {
            supportingNodeMap.forEach((supportingNodeName, terminationPointSet) -> {
                add(type, supportingNodeName, terminationPointSet);
            });
        });

        return true;
    }

    @Override
    public Map<OpenroadmNodeType, Map<String, Set<TerminationPoint>>> terminationPoints() {
        return terminationPoints ;
    }

    @Override
    public Map<OpenroadmNodeType, Map<String, Set<TerminationPoint>>> terminationPoints(List<OpenroadmNodeType>
            openroadmNodeTypes) {

        Map<OpenroadmNodeType, Map<String, Set<TerminationPoint>>> temp = new HashMap<>();

        for (OpenroadmNodeType type : openroadmNodeTypes) {
            if (!terminationPoints.containsKey(type)) {
                temp.put(type, new HashMap<>());
            } else {
                temp.put(type, terminationPoints.get(type));
            }
        }

        return temp;
    }

    @Override
    public Map<OpenroadmNodeType, Map<String, Set<TerminationPoint>>> terminationPoints(
            OpenroadmNodeType openroadmNodeType) {

        return terminationPoints(List.of(openroadmNodeType));
    }
}
