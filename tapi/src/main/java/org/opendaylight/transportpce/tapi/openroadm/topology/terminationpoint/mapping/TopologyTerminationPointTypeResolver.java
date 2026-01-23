/*
 * Copyright © 2026 Smartoptics and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.tapi.openroadm.topology.terminationpoint.mapping;

import java.util.Map;
import org.opendaylight.transportpce.tapi.openroadm.topology.terminationpoint.mapping.TpTypeResolutionException.Reason;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev250110.TerminationPoint1;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.types.rev250110.OpenroadmTpType;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.NetworkId;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.NodeId;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.networks.Network;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.networks.network.Node;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.networks.network.NodeKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.networks.network.node.SupportingNode;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.Node1;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.TpId;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.networks.network.node.TerminationPoint;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.networks.network.node.TerminationPointKey;

/**
 * {@link OpenRoadmTpTypeResolver} implementation that resolves termination point type information
 * by inspecting an in-memory OpenROADM {@link Network} topology model.
 *
 * <p>This resolver:
 * <ul>
 *   <li>locates the OpenROADM node by {@code nodeId}</li>
 *   <li>locates the termination point by {@code tpId}</li>
 *   <li>reads OpenROADM augmentation {@link TerminationPoint1} to obtain {@link OpenroadmTpType}</li>
 *   <li>derives the OpenROADM supporting node reference from {@link SupportingNode}</li>
 * </ul>
 *
 * <p>If expected nodes, termination points, augmentations, or the type itself are missing,
 * this class throws a {@link TpTypeResolutionException} with a specific {@link Reason}.
 */
public class TopologyTerminationPointTypeResolver implements OpenRoadmTpTypeResolver {

    private static final String OPENROADM_NETWORK_ID = "openroadm-network";
    private static final NetworkId OPENROADM_NETWORK_REF =
            NetworkId.getDefaultInstance(OPENROADM_NETWORK_ID);

    /**
     * {@inheritDoc}
     *
     * @throws NullPointerException if {@code network} is {@code null}
     */
    @Override
    public TerminationPointId terminationPointId(String nodeId, String tpId, Network network) {
        Map<NodeKey, Node> nodes = network.nonnullNode();
        NodeKey key = new NodeKey(new NodeId(nodeId));

        Node node = nodes.get(key);
        if (node == null) {
            throw new TpTypeResolutionException(
                    Reason.NODE_NOT_FOUND, nodeId, tpId, "Node " + nodeId + " does not exist in provided topology"
            );
        }

        Node1 augmentation = node.augmentation(Node1.class);
        if (augmentation == null) {
            throw new TpTypeResolutionException(
                    Reason.NODE_AUGMENTATION_MISSING, nodeId, tpId, "Augmentation 'Node1' not found for node: " + nodeId
            );
        }
        Map<TerminationPointKey, TerminationPoint> terminationPointKeyTerminationPointMap =
                augmentation.nonnullTerminationPoint();

        TerminationPointKey tpIdKey = new TerminationPointKey(new TpId(tpId));
        TerminationPoint terminationPoint = terminationPointKeyTerminationPointMap.get(tpIdKey);
        if (terminationPoint == null) {
            throw new TpTypeResolutionException(
                    Reason.TP_NOT_FOUND, nodeId, tpId, "TerminationPoint " + tpIdKey + " does not exist"
            );
        }

        TerminationPoint1 augmentation1 = terminationPoint.augmentation(TerminationPoint1.class);
        if (augmentation1 == null) {
            throw new TpTypeResolutionException(
                    Reason.TP_AUGMENTATION_MISSING, nodeId, tpId, "Augmentation 'TerminationPoint1' not found for"
                    + " TerminationPoint: " + tpIdKey
            );
        }

        OpenroadmTpType tpType = augmentation1.getTpType();
        if (tpType == null) {
            throw new TpTypeResolutionException(
                    TpTypeResolutionException.Reason.TP_TYPE_MISSING, nodeId, tpId,
                    "OpenROADM TP type is null for TP " + tpId + " on node " + nodeId
            );
        }

        SupportingNode supportingNode = node.nonnullSupportingNode()
                .entrySet()
                .stream()
                .filter(s -> OPENROADM_NETWORK_REF
                        .equals(s.getValue().getNetworkRef()))
                .findFirst()
                .orElseThrow(() -> new TpTypeResolutionException(
                        TpTypeResolutionException.Reason.SUPPORTING_NODE_NOT_FOUND, nodeId, tpId,
                        "Supporting node for node: " + nodeId + " tp: " + tpId
                ))
                .getValue();

        return new TerminationPointId(
                supportingNode.getNodeRef().getValue(),
                nodeId,
                tpId,
                tpType);
    }
}
