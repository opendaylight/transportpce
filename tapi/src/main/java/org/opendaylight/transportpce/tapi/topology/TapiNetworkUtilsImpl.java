/*
 * Copyright © 2021 Nokia, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.tapi.topology;

import com.google.common.util.concurrent.ListenableFuture;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.transportpce.common.network.NetworkTransactionService;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.tapinetworkutils.rev210408.DeleteTapiLinkInput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.tapinetworkutils.rev210408.DeleteTapiLinkOutput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.tapinetworkutils.rev210408.DeleteTapiLinkOutputBuilder;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.tapinetworkutils.rev210408.InitRoadmRoadmTapiLinkInput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.tapinetworkutils.rev210408.InitRoadmRoadmTapiLinkOutput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.tapinetworkutils.rev210408.InitRoadmRoadmTapiLinkOutputBuilder;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.tapinetworkutils.rev210408.InitXpdrRdmTapiLinkInput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.tapinetworkutils.rev210408.InitXpdrRdmTapiLinkOutput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.tapinetworkutils.rev210408.InitXpdrRdmTapiLinkOutputBuilder;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.tapinetworkutils.rev210408.TransportpceTapinetworkutilsService;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev181210.AdministrativeState;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev181210.CapacityUnit;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev181210.Context;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev181210.ForwardingDirection;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev181210.LayerProtocolName;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev181210.LifecycleState;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev181210.OperationalState;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev181210.Uuid;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev181210.capacity.TotalSizeBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev181210.capacity.pac.AvailableCapacityBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev181210.capacity.pac.TotalPotentialCapacityBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev181210.global._class.Name;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev181210.global._class.NameBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.Context1;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.ProtectionType;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.RestorationPolicy;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.context.TopologyContext;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.link.NodeEdgePoint;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.link.NodeEdgePointBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.link.NodeEdgePointKey;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.link.ResilienceTypeBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.node.OwnedNodeEdgePoint;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.node.OwnedNodeEdgePointKey;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.risk.parameter.pac.RiskCharacteristic;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.risk.parameter.pac.RiskCharacteristicBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.topology.Link;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.topology.LinkBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.topology.LinkKey;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.topology.Node;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.topology.NodeKey;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.topology.context.Topology;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.topology.context.TopologyBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.topology.context.TopologyKey;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.transfer.cost.pac.CostCharacteristic;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.transfer.cost.pac.CostCharacteristicBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.transfer.timing.pac.LatencyCharacteristic;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.transfer.timing.pac.LatencyCharacteristicBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.validation.pac.ValidationMechanism;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.validation.pac.ValidationMechanismBuilder;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.RpcError;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;
import org.opendaylight.yangtools.yang.common.Uint64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TapiNetworkUtilsImpl implements TransportpceTapinetworkutilsService {

    private static final Logger LOG = LoggerFactory.getLogger(TapiNetworkUtilsImpl.class);
    private static final String PHTNC_MEDIA = "PHOTONIC_MEDIA";
    private static final String OTSI = "OTSi";
    private final Uuid tapiTopoUuid = new Uuid(UUID.nameUUIDFromBytes(TopologyUtils.T0_FULL_MULTILAYER
            .getBytes(Charset.forName("UTF-8"))).toString());
    private final NetworkTransactionService networkTransactionService;

    public TapiNetworkUtilsImpl(NetworkTransactionService networkTransactionService) {
        this.networkTransactionService = networkTransactionService;
    }

    @Override
    public ListenableFuture<RpcResult<InitRoadmRoadmTapiLinkOutput>> initRoadmRoadmTapiLink(
            InitRoadmRoadmTapiLinkInput input) {
        // TODO --> need to check if the nodes and neps exist in the topology
        String sourceNode = input.getRdmANode();
        String sourceTp = input.getDegATp();
        String destNode = input.getRdmZNode();
        String destTp = input.getDegZTp();
        String linkId = String.join("-", sourceNode, sourceTp.split("-")[0], sourceTp)
            + "to" + String.join("-", destNode, destTp.split("-")[0], destTp);
        Link link = createTapiLink(sourceNode, sourceTp, destNode, destTp, linkId, "OMS link name", PHTNC_MEDIA);
        InitRoadmRoadmTapiLinkOutputBuilder output = new InitRoadmRoadmTapiLinkOutputBuilder();
        if (link == null) {
            LOG.error("Error creating link object");
            return RpcResultBuilder.<InitRoadmRoadmTapiLinkOutput>failed().withError(RpcError.ErrorType.RPC,
                "Failed to create link in topology").buildFuture();
        }
        if (putLinkInTopology(link)) {
            output = new InitRoadmRoadmTapiLinkOutputBuilder()
                .setResult("Link created in tapi topology. Link-uuid = " + link.getUuid());
        }
        return RpcResultBuilder.success(output.build()).buildFuture();
    }

    @Override
    public ListenableFuture<RpcResult<InitXpdrRdmTapiLinkOutput>> initXpdrRdmTapiLink(InitXpdrRdmTapiLinkInput input) {
        // TODO --> need to check if the nodes and neps exist in the topology
        String destNode = input.getRdmNode();
        String destTp = input.getAddDropTp();
        String sourceNode = input.getXpdrNode();
        String sourceTp = input.getNetworkTp();
        String linkId = String.join("-", sourceNode, sourceTp)
            + "to" + String.join("-", destNode, destTp.split("-")[0], destTp);
        Link link = createTapiLink(sourceNode, sourceTp, destNode, destTp, linkId, "XPDR-RDM link name", OTSI);
        InitXpdrRdmTapiLinkOutputBuilder output = new InitXpdrRdmTapiLinkOutputBuilder();
        if (link == null) {
            LOG.error("Error creating link object");
            return RpcResultBuilder.<InitXpdrRdmTapiLinkOutput>failed().withError(RpcError.ErrorType.RPC,
                "Failed to create link in topology").buildFuture();
        }
        if (putLinkInTopology(link)) {
            output = new InitXpdrRdmTapiLinkOutputBuilder()
                .setResult("Link created in tapi topology. Link-uuid = " + link.getUuid());
        }
        return RpcResultBuilder.success(output.build()).buildFuture();
    }

    @Override
    public ListenableFuture<RpcResult<DeleteTapiLinkOutput>> deleteTapiLink(DeleteTapiLinkInput input) {
        // TODO: check if this IID is correct
        // TODO --> need to check if the link exists in the topology
        try {
            InstanceIdentifier<Link> linkIID = InstanceIdentifier.builder(Context.class)
                    .augmentation(Context1.class).child(TopologyContext.class).child(Topology.class,
                            new TopologyKey(tapiTopoUuid)).child(Link.class, new LinkKey(input.getUuid())).build();
            this.networkTransactionService.delete(LogicalDatastoreType.OPERATIONAL, linkIID);
            this.networkTransactionService.commit().get();
            LOG.info("TAPI link deleted successfully.");
            return RpcResultBuilder.success(new DeleteTapiLinkOutputBuilder()
                .setResult("Link successfully deleted from tapi topology").build()).buildFuture();
        } catch (InterruptedException | ExecutionException e) {
            LOG.error("Failed to delete TAPI link", e);
            return RpcResultBuilder.<DeleteTapiLinkOutput>failed().withError(RpcError.ErrorType.RPC,
                "Failed to delete link from topology").buildFuture();
        }
    }

    private Link createTapiLink(String sourceNode, String sourceTp, String destNode, String destTp, String linkId,
                                String valueName, String sourceNodeQual) {
        Map<NodeEdgePointKey, NodeEdgePoint> nepList = new HashMap<>();
        Uuid sourceUuidNode = new Uuid(UUID.nameUUIDFromBytes((String.join("+", sourceNode,
            sourceNodeQual)).getBytes(Charset.forName("UTF-8"))).toString());
        Uuid sourceUuidTp = new Uuid(UUID.nameUUIDFromBytes((String.join("+", sourceNode, PHTNC_MEDIA,
            sourceTp)).getBytes(Charset.forName("UTF-8"))).toString());
        Uuid destUuidNode = new Uuid(UUID.nameUUIDFromBytes((String.join("+", destNode,
            PHTNC_MEDIA)).getBytes(Charset.forName("UTF-8"))).toString());
        Uuid destUuidTp = new Uuid(UUID.nameUUIDFromBytes((String.join("+", destNode, PHTNC_MEDIA,
            destTp)).getBytes(Charset.forName("UTF-8"))).toString());
        if (!(nodeExists(sourceUuidNode) && nodeExists(destUuidNode)
                && nepExists(sourceUuidTp, sourceUuidNode) && nepExists(destUuidTp, destUuidNode))) {
            LOG.error("Verify the input data. No link can be created, "
                + "as either the node or the tp doesnt exist in the TAPI topology");
            return null;
        }

        NodeEdgePoint sourceNep = new NodeEdgePointBuilder()
            .setTopologyUuid(this.tapiTopoUuid)
            .setNodeUuid(sourceUuidNode)
            .setNodeEdgePointUuid(sourceUuidTp)
            .build();
        nepList.put(sourceNep.key(), sourceNep);
        NodeEdgePoint destNep = new NodeEdgePointBuilder()
            .setTopologyUuid(this.tapiTopoUuid)
            .setNodeUuid(destUuidNode)
            .setNodeEdgePointUuid(destUuidTp)
            .build();
        nepList.put(destNep.key(), destNep);
        OperationalState sourceOperState = getOperState(sourceUuidTp, sourceUuidNode);
        OperationalState destOperState = getOperState(destUuidTp, destUuidNode);
        if (sourceOperState == null || destOperState == null) {
            LOG.error("No link can be created, as the operational state was not found in the TAPI topology");
            return null;
        }
        AdministrativeState sourceAdminState = getAdminState(sourceUuidTp, sourceUuidNode);
        AdministrativeState destAdminState = getAdminState(destUuidTp, destUuidNode);
        if (sourceAdminState == null || destAdminState == null) {
            LOG.error("No link can be created, as the administrative state was not found in the TAPI topology");
            return null;
        }
        OperationalState operState = (OperationalState.ENABLED.equals(sourceOperState)
                && OperationalState.ENABLED.equals(destOperState))
                ? OperationalState.ENABLED : OperationalState.DISABLED;
        AdministrativeState adminState = (AdministrativeState.UNLOCKED.equals(sourceAdminState)
                && AdministrativeState.UNLOCKED.equals(destAdminState))
                ? AdministrativeState.UNLOCKED : AdministrativeState.LOCKED;
        Name linkName = new NameBuilder().setValueName(valueName)
            .setValue(linkId)
            .build();
        CostCharacteristic costCharacteristic = new CostCharacteristicBuilder()
            .setCostAlgorithm("Restricted Shortest Path - RSP")
            .setCostName("HOP_COUNT")
            .setCostValue("12345678")
            .build();
        LatencyCharacteristic latencyCharacteristic = new LatencyCharacteristicBuilder()
            .setFixedLatencyCharacteristic("12345678")
            .setQueingLatencyCharacteristic("12345678")
            .setJitterCharacteristic("12345678")
            .setWanderCharacteristic("12345678")
            .setTrafficPropertyName("FIXED_LATENCY")
            .build();
        RiskCharacteristic riskCharacteristic = new RiskCharacteristicBuilder()
            .setRiskCharacteristicName("risk characteristic")
            .setRiskIdentifierList(List.of("risk identifier1", "risk identifier2"))
            .build();
        ValidationMechanism validationMechanism = new ValidationMechanismBuilder()
            .setValidationMechanism("validation mechanism")
            .setValidationRobustness("validation robustness")
            .setLayerProtocolAdjacencyValidated("layer protocol adjacency")
            .build();
        return new LinkBuilder()
            .setUuid(new Uuid(
                UUID.nameUUIDFromBytes(linkId.getBytes(Charset.forName("UTF-8")))
                    .toString()))
            .setName(Map.of(linkName.key(), linkName))
            .setLayerProtocolName(List.of(LayerProtocolName.PHOTONICMEDIA))
            .setNodeEdgePoint(nepList)
            .setDirection(ForwardingDirection.BIDIRECTIONAL)
            .setTransitionedLayerProtocolName(new ArrayList<>())
            .setResilienceType(new ResilienceTypeBuilder().setProtectionType(ProtectionType.NOPROTECTON)
                .setRestorationPolicy(RestorationPolicy.NA)
                .build())
            .setAdministrativeState(adminState)
            .setOperationalState(operState)
            .setLifecycleState(LifecycleState.INSTALLED)
            .setTotalPotentialCapacity(new TotalPotentialCapacityBuilder().setTotalSize(
                new TotalSizeBuilder().setUnit(CapacityUnit.GBPS)
                    .setValue(Uint64.valueOf(100)).build()).build())
            .setAvailableCapacity(new AvailableCapacityBuilder().setTotalSize(
                new TotalSizeBuilder().setUnit(CapacityUnit.MBPS)
                    .setValue(Uint64.valueOf(100)).build())
                .build())
            .setCostCharacteristic(Map.of(costCharacteristic.key(), costCharacteristic))
            .setLatencyCharacteristic(Map.of(latencyCharacteristic.key(), latencyCharacteristic))
            .setRiskCharacteristic(Map.of(riskCharacteristic.key(), riskCharacteristic))
            .setErrorCharacteristic("error")
            .setLossCharacteristic("loss")
            .setRepeatDeliveryCharacteristic("repeat delivery")
            .setDeliveryOrderCharacteristic("delivery order")
            .setUnavailableTimeCharacteristic("unavailable time")
            .setServerIntegrityProcessCharacteristic("server integrity process")
            .setValidationMechanism(Map.of(validationMechanism.key(), validationMechanism))
            .build();
    }

    private boolean nepExists(Uuid nepUuid, Uuid nodeUuid) {
        LOG.info("Checking if nep with uuid {} existis in tapi topology", nepUuid);
        try {
            InstanceIdentifier<OwnedNodeEdgePoint> onepIID = InstanceIdentifier.builder(Context.class)
                .augmentation(Context1.class).child(TopologyContext.class)
                .child(Topology.class, new TopologyKey(tapiTopoUuid)).child(Node.class, new NodeKey(nodeUuid))
                .child(OwnedNodeEdgePoint.class, new OwnedNodeEdgePointKey(nepUuid))
                .build();
            Optional<OwnedNodeEdgePoint> optionalOnep = this.networkTransactionService.read(
                LogicalDatastoreType.OPERATIONAL, onepIID).get();
            if (!optionalOnep.isPresent()) {
                LOG.error("ONEP is not present in datastore");
                return false;
            }
            return true;
        } catch (InterruptedException | ExecutionException e) {
            LOG.error("Couldnt retrieve nep from datastore");
            return false;
        }
    }

    private boolean nodeExists(Uuid nodeUuid) {
        LOG.info("Checking if node with uuid {} existis in tapi topology", nodeUuid);
        try {
            InstanceIdentifier<Node> nodeIID = InstanceIdentifier.builder(Context.class)
                .augmentation(Context1.class).child(TopologyContext.class)
                .child(Topology.class, new TopologyKey(tapiTopoUuid)).child(Node.class, new NodeKey(nodeUuid))
                .build();
            Optional<Node> optionalNode = this.networkTransactionService.read(
                LogicalDatastoreType.OPERATIONAL, nodeIID).get();
            if (!optionalNode.isPresent()) {
                LOG.error("Node is not present in datastore");
                return false;
            }
            return true;
        } catch (InterruptedException | ExecutionException e) {
            LOG.error("Couldnt retrieve nep from datastore");
            return false;
        }
    }

    private OperationalState getOperState(Uuid nepUuid, Uuid nodeUuid) {
        try {
            InstanceIdentifier<OwnedNodeEdgePoint> onepIID = InstanceIdentifier.builder(Context.class)
                    .augmentation(Context1.class).child(TopologyContext.class)
                    .child(Topology.class, new TopologyKey(tapiTopoUuid)).child(Node.class, new NodeKey(nodeUuid))
                    .child(OwnedNodeEdgePoint.class, new OwnedNodeEdgePointKey(nepUuid))
                    .build();
            Optional<OwnedNodeEdgePoint> optionalOnep = this.networkTransactionService.read(
                    LogicalDatastoreType.OPERATIONAL, onepIID).get();
            if (optionalOnep.isPresent()) {
                return optionalOnep.get().getOperationalState();
            }
            return null;
        } catch (InterruptedException | ExecutionException e) {
            LOG.error("Couldnt retrieve nep from datastore");
            return null;
        }
    }

    private AdministrativeState getAdminState(Uuid nepUuid, Uuid nodeUuid) {
        try {
            InstanceIdentifier<OwnedNodeEdgePoint> onepIID = InstanceIdentifier.builder(Context.class)
                    .augmentation(Context1.class).child(TopologyContext.class)
                    .child(Topology.class, new TopologyKey(tapiTopoUuid)).child(Node.class, new NodeKey(nodeUuid))
                    .child(OwnedNodeEdgePoint.class, new OwnedNodeEdgePointKey(nepUuid))
                    .build();
            Optional<OwnedNodeEdgePoint> optionalOnep = this.networkTransactionService.read(
                    LogicalDatastoreType.OPERATIONAL, onepIID).get();
            if (optionalOnep.isPresent()) {
                return optionalOnep.get().getAdministrativeState();
            }
            return null;
        } catch (InterruptedException | ExecutionException e) {
            LOG.error("Couldnt retrieve nep from datastore");
            return null;
        }
    }

    private boolean putLinkInTopology(Link tapiLink) {
        // TODO is this merge correct? Should we just merge topology by changing the nodes map??
        // TODO: verify this is correct. Should we identify the context IID with the context UUID??
        LOG.info("Creating tapi node in TAPI topology context");
        InstanceIdentifier<Topology> topoIID = InstanceIdentifier.builder(Context.class)
            .augmentation(Context1.class).child(TopologyContext.class)
            .child(Topology.class, new TopologyKey(tapiTopoUuid))
            .build();

        Topology topology = new TopologyBuilder().setUuid(tapiTopoUuid)
            .setLink(Map.of(tapiLink.key(), tapiLink)).build();

        // merge in datastore
        this.networkTransactionService.merge(LogicalDatastoreType.OPERATIONAL, topoIID,
            topology);
        try {
            this.networkTransactionService.commit().get();

        } catch (InterruptedException | ExecutionException e) {
            LOG.error("Error populating TAPI topology: ", e);
            return false;
        }
        LOG.info("TAPI Link added succesfully.");
        return true;
    }
}
