/*
 * Copyright © 2024 Orange, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.tapi.impl.rpc;

import com.google.common.util.concurrent.ListenableFuture;
import java.util.HashMap;
import java.util.Map;
import org.opendaylight.transportpce.tapi.utils.TapiContext;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.GetNodeDetails;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.GetNodeDetailsInput;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.GetNodeDetailsOutput;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.GetNodeDetailsOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.node.OwnedNodeEdgePoint;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.node.OwnedNodeEdgePointBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.node.OwnedNodeEdgePointKey;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.topology.Node;
import org.opendaylight.yangtools.yang.common.ErrorType;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class GetNodeDetailsImpl implements GetNodeDetails {
    private static final Logger LOG = LoggerFactory.getLogger(GetNodeDetailsImpl.class);
    private final TapiContext tapiContext;

    public GetNodeDetailsImpl(TapiContext tapiContext) {
        this.tapiContext = tapiContext;
    }

    @Override
    public ListenableFuture<RpcResult<GetNodeDetailsOutput>> invoke(GetNodeDetailsInput input) {

        // Node id: if roadm -> ROADM+PHOTONIC_MEDIA. if xpdr -> XPDR-XPDR+DSR/OTSi

        Node node = this.tapiContext.getTapiNode(input.getTopologyId(), input.getNodeId());
        if (node == null) {
            LOG.error("Invalid TAPI node name");
            return RpcResultBuilder.<GetNodeDetailsOutput>failed()
                .withError(ErrorType.RPC, "Invalid Tapi Node name")
                .buildFuture();
        }
        // OwnedNodeEdgePoint1 augmentation not supported in get.node.details.output.Node
        // Cast as follows not tolerated by spotBugs
//        org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.get.node.details.output.Node gndNode
//            = org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.get.node.details.output.Node
//                .class.cast(node);
        // Recreate OwnedNodeEdgePoint and Node builders straping the unsupported OwnedNodeEdgePoint1 Augmentation
        Map<OwnedNodeEdgePointKey, OwnedNodeEdgePoint> onepmap = node.getOwnedNodeEdgePoint();
        Map<OwnedNodeEdgePointKey, OwnedNodeEdgePoint> gndonepmap = new HashMap<>();

        for (Map.Entry<OwnedNodeEdgePointKey, OwnedNodeEdgePoint> onep : onepmap.entrySet()) {
            gndonepmap.put(onep.getKey(), new OwnedNodeEdgePointBuilder()
                .setUuid(onep.getValue().getUuid())
                .setLayerProtocolName(onep.getValue().getLayerProtocolName())
                .setName(onep.getValue().getName())
                .setSupportedCepLayerProtocolQualifierInstances(
                    onep.getValue().getSupportedCepLayerProtocolQualifierInstances())
                .setAdministrativeState(onep.getValue().getAdministrativeState())
                .setOperationalState(onep.getValue().getOperationalState())
                .setLifecycleState(onep.getValue().getLifecycleState())
                .setDirection(onep.getValue().getDirection())
                .setLinkPortRole(onep.getValue().getLinkPortRole())
                .build());
        }
        org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.get.node.details.output.Node gndNode =
                 new org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.get.node.details.output
                 .NodeBuilder()
            .setUuid(node.getUuid())
            .setName(node.getName())
            .setLayerProtocolName(node.getLayerProtocolName())
            .setAdministrativeState(node.getAdministrativeState())
            .setOperationalState(node.getOperationalState())
            .setLifecycleState(node.getLifecycleState())
            .setOwnedNodeEdgePoint(gndonepmap)
            .setNodeRuleGroup(node.getNodeRuleGroup())
            .setInterRuleGroup(node.getInterRuleGroup())
            .setCostCharacteristic(node.getCostCharacteristic())
            .setLatencyCharacteristic(node.getLatencyCharacteristic())
            .setErrorCharacteristic(node.getErrorCharacteristic())
            .setLossCharacteristic(node.getLossCharacteristic())
            .setRepeatDeliveryCharacteristic(node.getRepeatDeliveryCharacteristic())
            .setDeliveryOrderCharacteristic(node.getDeliveryOrderCharacteristic())
            .setUnavailableTimeCharacteristic(node.getUnavailableTimeCharacteristic())
            .setServerIntegrityProcessCharacteristic(node.getServerIntegrityProcessCharacteristic())
            .build();

        return RpcResultBuilder
            .success(new GetNodeDetailsOutputBuilder()
                .setNode(gndNode)
                .build())
            .buildFuture();
    }

}
