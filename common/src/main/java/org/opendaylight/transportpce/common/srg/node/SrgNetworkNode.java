/*
 * Copyright (c) 2023 Orange, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.common.srg.node;

import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.opendaylight.transportpce.common.srg.storage.Storage;
import org.opendaylight.yang.gen.v1.http.com.smartoptics.openroadm.srg.rev231110.Srg;
import org.opendaylight.yang.gen.v1.http.com.smartoptics.openroadm.srg.rev231110.network.network.nodes.SharedRiskGroup;
import org.opendaylight.yang.gen.v1.http.com.smartoptics.openroadm.srg.rev231110.network.network.nodes.SharedRiskGroupKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.NetworkId;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.NodeId;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.networks.network.Node;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.networks.network.node.SupportingNode;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.networks.network.node.SupportingNodeKey;
import org.opendaylight.yangtools.yang.common.Uint16;

public class SrgNetworkNode implements NetworkNode {

    private final Pattern pattern;

    public SrgNetworkNode() {
        pattern = Pattern.compile("(\\d+)$", Pattern.MULTILINE);
    }

    @Override
    public NodeId supportingNode(NetworkId networkId, Map<SupportingNodeKey, SupportingNode> supportingNodes) {

        for (Map.Entry<SupportingNodeKey, SupportingNode> entry : supportingNodes.entrySet()) {

            SupportingNode supportingNode = entry.getValue();
            if (supportingNode.getNetworkRef().equals(networkId)) {
                return supportingNode.getNodeRef();
            }
        }

        throw new NodeNotFoundException(
                String.format(
                        "Couldn't find a node id for NetworkId %s among supporting nodes %s",
                        networkId,
                        supportingNodes)
        );

    }

    @Override
    public int number(String node) {

        final Matcher matcher = pattern.matcher(node);

        if (matcher.find()) {
            return Integer.parseInt(matcher.group(0));
        }

        throw new IndexNotFoundException(
                String.format(
                        "Couldn't find a numeric index at the end if the string %s",
                        node
                )
        );
    }

    @Override
    public boolean contentionLess(NetworkId networkId, Node node, Storage storage) {
        NodeId nodeId = supportingNode(networkId, Objects.requireNonNull(node.getSupportingNode()));
        Map<SharedRiskGroupKey, SharedRiskGroup> map = storage.read(nodeId.getValue());
        SharedRiskGroupKey sharedRiskGroupKey = new SharedRiskGroupKey(
                Uint16.valueOf(number(node.getNodeId().getValue()))
        );

        if (map.containsKey(sharedRiskGroupKey)) {
            return map.get(sharedRiskGroupKey)
                    .getWavelengthDuplication()
                    .equals(Srg.WavelengthDuplication.OnePerDegree);
        }

        return false;
    }
}