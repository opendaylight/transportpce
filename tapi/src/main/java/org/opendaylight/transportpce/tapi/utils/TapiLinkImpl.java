/*
 * Copyright © 2021 Nokia.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.tapi.utils;

import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.transportpce.common.network.NetworkTransactionService;
import org.opendaylight.transportpce.tapi.TapiStringConstants;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.state.types.rev191129.State;
import org.opendaylight.yang.gen.v1.http.org.openroadm.equipment.states.types.rev191129.AdminStates;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.AdministrativeState;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.CAPACITYUNITGBPS;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.Context;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.ForwardingDirection;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.LayerProtocolName;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.LifecycleState;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.OperationalState;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.Uuid;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.capacity.TotalSizeBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.capacity.pac.AvailableCapacityBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.capacity.pac.TotalPotentialCapacityBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.global._class.NameBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.Context1;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.ProtectionType;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.RestorationPolicy;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.context.TopologyContext;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.link.NodeEdgePoint;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.link.NodeEdgePointBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.link.NodeEdgePointKey;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.link.ResilienceTypeBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.node.OwnedNodeEdgePoint;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.node.OwnedNodeEdgePointKey;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.risk.parameter.pac.RiskCharacteristic;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.risk.parameter.pac.RiskCharacteristicBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.topology.Link;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.topology.LinkBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.topology.Node;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.topology.NodeKey;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.topology.context.Topology;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.topology.context.TopologyKey;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.transfer.cost.pac.CostCharacteristic;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.transfer.cost.pac.CostCharacteristicBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.transfer.timing.pac.LatencyCharacteristic;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.transfer.timing.pac.LatencyCharacteristicBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.validation.pac.ValidationMechanism;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.validation.pac.ValidationMechanismBuilder;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.Decimal64;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
public class TapiLinkImpl implements TapiLink {

    private static final Logger LOG = LoggerFactory.getLogger(TapiLinkImpl.class);
    private final NetworkTransactionService networkTransactionService;

    @Activate
    public TapiLinkImpl(@Reference NetworkTransactionService networkTransactionService) {
        this.networkTransactionService = networkTransactionService;
    }

    public Link createTapiLink(String srcNodeid, String srcTpId, String dstNodeId, String dstTpId, String linkType,
                               String srcNodeQual, String dstNodeQual, String srcTpQual, String dstTpQual,
                               String adminState, String operState, Set<LayerProtocolName> layerProtoNameList,
                               Set<String> transLayerNameList, Uuid tapiTopoUuid) {
        Map<NodeEdgePointKey, NodeEdgePoint> nepList = new HashMap<>();
        String sourceNodeKey = String.join("+", srcNodeid, srcNodeQual);
        String sourceNepKey = String.join("+", srcNodeid, srcTpQual, srcTpId);
        Uuid sourceUuidNode = new Uuid(UUID.nameUUIDFromBytes(sourceNodeKey.getBytes(Charset.forName("UTF-8")))
            .toString());
        Uuid sourceUuidTp = new Uuid(UUID.nameUUIDFromBytes(sourceNepKey.getBytes(Charset.forName("UTF-8")))
            .toString());
        String destNodeKey = String.join("+", dstNodeId, dstNodeQual);
        String destNepKey = String.join("+", dstNodeId, dstTpQual, dstTpId);
        String linkKey = String.join("to", sourceNepKey, destNepKey);
        Uuid destUuidNode = new Uuid(UUID.nameUUIDFromBytes(destNodeKey.getBytes(Charset.forName("UTF-8"))).toString());
        Uuid destUuidTp = new Uuid(UUID.nameUUIDFromBytes(destNepKey.getBytes(Charset.forName("UTF-8"))).toString());
        NodeEdgePoint sourceNep = new NodeEdgePointBuilder()
            .setTopologyUuid(tapiTopoUuid)
            .setNodeUuid(sourceUuidNode)
            .setNodeEdgePointUuid(sourceUuidTp)
            .build();
        nepList.put(sourceNep.key(), sourceNep);
        NodeEdgePoint destNep = new NodeEdgePointBuilder()
            .setTopologyUuid(tapiTopoUuid)
            .setNodeUuid(destUuidNode)
            .setNodeEdgePointUuid(destUuidTp)
            .build();
        nepList.put(destNep.key(), destNep);
        NameBuilder linkName = new NameBuilder();
        // TODO: variables for each type
        switch (linkType) {
            case TapiStringConstants.OMS_RDM_RDM_LINK:
                LOG.info("Roadm to roadm link");
                linkName
                    .setValueName("OMS link name")
                    .setValue(linkKey);
                break;
            case TapiStringConstants.TRANSITIONAL_LINK:
                LOG.info("Transitional link");
                linkName
                    .setValueName("transitional link name")
                    .setValue(linkKey);
                break;
            case TapiStringConstants.OMS_XPDR_RDM_LINK:
                LOG.info("Xpdr to roadm link");
                linkName
                    .setValueName("XPDR-RDM link name")
                    .setValue(linkKey);
                break;
            case TapiStringConstants.OTN_XPDR_XPDR_LINK:
                LOG.info("OTN Xpdr to roadm link");
                linkName
                    .setValueName("otn link name")
                    .setValue(linkKey);
                break;
            default:
                LOG.warn("Type {} not recognized", linkType);
                return null;
        }
        // Todo: common aspects of links and set all attributes
        CostCharacteristic costCharacteristic = new CostCharacteristicBuilder()
            .setCostAlgorithm("Restricted Shortest Path - RSP")
            .setCostName("HOP_COUNT")
            .setCostValue(TapiStringConstants.COST_HOP_VALUE)
            .build();
        LatencyCharacteristic latencyCharacteristic = new LatencyCharacteristicBuilder()
            .setFixedLatencyCharacteristic(TapiStringConstants.FIXED_LATENCY_VALUE)
            .setQueuingLatencyCharacteristic(TapiStringConstants.QUEING_LATENCY_VALUE)
            .setJitterCharacteristic(TapiStringConstants.JITTER_VALUE)
            .setWanderCharacteristic(TapiStringConstants.WANDER_VALUE)
            .setTrafficPropertyName("FIXED_LATENCY")
            .build();
        RiskCharacteristic riskCharacteristic = new RiskCharacteristicBuilder()
            .setRiskCharacteristicName("risk characteristic")
            .setRiskIdentifierList(Set.of("risk identifier1", "risk identifier2"))
            .build();
        ValidationMechanism validationMechanism = new ValidationMechanismBuilder()
            .setValidationMechanism("validation mechanism")
            .setValidationRobustness("validation robustness")
            .setLayerProtocolAdjacencyValidated("layer protocol adjacency")
            .build();
        return new LinkBuilder()
            .setUuid(new Uuid(
                UUID.nameUUIDFromBytes(linkKey.getBytes(Charset.forName("UTF-8"))).toString()))
            .setName(Map.of(linkName.build().key(), linkName.build()))
            .setTransitionedLayerProtocolName(transLayerNameList)
            .setLayerProtocolName(layerProtoNameList)
            .setNodeEdgePoint(nepList)
            .setDirection(ForwardingDirection.BIDIRECTIONAL)
            .setAvailableCapacity(new AvailableCapacityBuilder().setTotalSize(
                    new TotalSizeBuilder().setUnit(CAPACITYUNITGBPS.VALUE).setValue(Decimal64.valueOf("100")).build())
                .build())
            .setResilienceType(new ResilienceTypeBuilder().setProtectionType(ProtectionType.NOPROTECTION)
                .setRestorationPolicy(RestorationPolicy.NA)
                .build())
            .setAdministrativeState(setTapiAdminState(adminState))
            .setOperationalState(setTapiOperationalState(operState))
            .setLifecycleState(LifecycleState.INSTALLED)
            .setTotalPotentialCapacity(new TotalPotentialCapacityBuilder().setTotalSize(
                    new TotalSizeBuilder().setUnit(CAPACITYUNITGBPS.VALUE).setValue(Decimal64.valueOf("100")).build())
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

    @Override
    public AdministrativeState setTapiAdminState(String adminState) {
        if (adminState == null) {
            return null;
        }
        return adminState.equals(AdminStates.InService.getName())
            || adminState.equals(AdministrativeState.UNLOCKED.getName()) ? AdministrativeState.UNLOCKED
                : AdministrativeState.LOCKED;
    }

    @Override
    public AdministrativeState setTapiAdminState(AdminStates adminState1, AdminStates adminState2) {
        if (adminState1 == null || adminState2 == null) {
            return null;
        }
        LOG.info("Admin state 1 = {}, andmin state 2 = {}", adminState1.getName(), adminState2.getName());
        return AdminStates.InService.equals(adminState1) && AdminStates.InService.equals(adminState2)
            ? AdministrativeState.UNLOCKED : AdministrativeState.LOCKED;
    }

    @Override
    public OperationalState setTapiOperationalState(String operState) {
        if (operState == null) {
            return null;
        }
        return operState.equals("inService") || operState.equals(OperationalState.ENABLED.getName())
            ? OperationalState.ENABLED : OperationalState.DISABLED;
    }

    @Override
    public OperationalState setTapiOperationalState(State operState1, State operState2) {
        if (operState1 == null || operState2 == null) {
            return null;
        }
        return State.InService.equals(operState1) && State.InService.equals(operState2)
            ? OperationalState.ENABLED : OperationalState.DISABLED;
    }

    @Override
    public String getOperState(String srcNodeId, String destNodeId, String sourceTpId, String destTpId) {
        Uuid tapiTopoUuid = new Uuid(UUID.nameUUIDFromBytes(TapiStringConstants.T0_FULL_MULTILAYER
            .getBytes(Charset.forName("UTF-8"))).toString());
        Uuid nodeUuid = new Uuid(UUID.nameUUIDFromBytes(String.join("+", srcNodeId,
            TapiStringConstants.PHTNC_MEDIA).getBytes(Charset.forName("UTF-8"))).toString());
        Uuid nepUuid = new Uuid(UUID.nameUUIDFromBytes(String.join("+", srcNodeId,
            TapiStringConstants.PHTNC_MEDIA_OTS, sourceTpId).getBytes(Charset.forName("UTF-8"))).toString());
        InstanceIdentifier<OwnedNodeEdgePoint> onepIID = InstanceIdentifier.builder(Context.class)
            .augmentation(Context1.class).child(TopologyContext.class)
            .child(Topology.class, new TopologyKey(tapiTopoUuid)).child(Node.class, new NodeKey(nodeUuid))
            .child(OwnedNodeEdgePoint.class, new OwnedNodeEdgePointKey(nepUuid))
            .build();
        Uuid node1Uuid = new Uuid(UUID.nameUUIDFromBytes(String.join("+", destNodeId,
            TapiStringConstants.PHTNC_MEDIA).getBytes(Charset.forName("UTF-8"))).toString());
        Uuid nep1Uuid = new Uuid(UUID.nameUUIDFromBytes(String.join("+", destNodeId,
            TapiStringConstants.PHTNC_MEDIA_OTS, destTpId).getBytes(Charset.forName("UTF-8"))).toString());
        InstanceIdentifier<OwnedNodeEdgePoint> onep1IID = InstanceIdentifier.builder(Context.class)
            .augmentation(Context1.class).child(TopologyContext.class)
            .child(Topology.class, new TopologyKey(tapiTopoUuid)).child(Node.class, new NodeKey(node1Uuid))
            .child(OwnedNodeEdgePoint.class, new OwnedNodeEdgePointKey(nep1Uuid))
            .build();
        try {
            Optional<OwnedNodeEdgePoint> optionalOnep = this.networkTransactionService.read(
                LogicalDatastoreType.OPERATIONAL, onepIID).get();
            Optional<OwnedNodeEdgePoint> optionalOnep1 = this.networkTransactionService.read(
                LogicalDatastoreType.OPERATIONAL, onep1IID).get();
            if (!optionalOnep.isPresent() || !optionalOnep1.isPresent()) {
                LOG.error("One of the 2 neps doesnt exist in the datastore: {} OR {}", nepUuid, nep1Uuid);
                return null;
            }
            return optionalOnep.orElseThrow().getOperationalState().equals(
                        optionalOnep1.orElseThrow().getOperationalState())
                ? optionalOnep.orElseThrow().getOperationalState().getName() : OperationalState.DISABLED.getName();
        } catch (InterruptedException | ExecutionException e) {
            LOG.error("Failed getting Mapping data from portMapping",e);
            return null;
        }
    }

    @Override
    public String getAdminState(String srcNodeId, String destNodeId, String sourceTpId, String destTpId) {
        Uuid tapiTopoUuid = new Uuid(UUID.nameUUIDFromBytes(TapiStringConstants.T0_FULL_MULTILAYER
            .getBytes(Charset.forName("UTF-8"))).toString());
        Uuid nodeUuid = new Uuid(UUID.nameUUIDFromBytes(String.join("+", srcNodeId,
            TapiStringConstants.PHTNC_MEDIA).getBytes(Charset.forName("UTF-8"))).toString());
        Uuid nepUuid = new Uuid(UUID.nameUUIDFromBytes(String.join("+", srcNodeId,
            TapiStringConstants.PHTNC_MEDIA_OTS, sourceTpId).getBytes(Charset.forName("UTF-8"))).toString());
        InstanceIdentifier<OwnedNodeEdgePoint> onepIID = InstanceIdentifier.builder(Context.class)
            .augmentation(Context1.class).child(TopologyContext.class)
            .child(Topology.class, new TopologyKey(tapiTopoUuid)).child(Node.class, new NodeKey(nodeUuid))
            .child(OwnedNodeEdgePoint.class, new OwnedNodeEdgePointKey(nepUuid))
            .build();
        Uuid node1Uuid = new Uuid(UUID.nameUUIDFromBytes(String.join("+", destNodeId,
            TapiStringConstants.PHTNC_MEDIA).getBytes(Charset.forName("UTF-8"))).toString());
        Uuid nep1Uuid = new Uuid(UUID.nameUUIDFromBytes(String.join("+", destNodeId,
            TapiStringConstants.PHTNC_MEDIA_OTS, destTpId).getBytes(Charset.forName("UTF-8"))).toString());
        InstanceIdentifier<OwnedNodeEdgePoint> onep1IID = InstanceIdentifier.builder(Context.class)
            .augmentation(Context1.class).child(TopologyContext.class)
            .child(Topology.class, new TopologyKey(tapiTopoUuid)).child(Node.class, new NodeKey(node1Uuid))
            .child(OwnedNodeEdgePoint.class, new OwnedNodeEdgePointKey(nep1Uuid))
            .build();
        try {
            Optional<OwnedNodeEdgePoint> optionalOnep = this.networkTransactionService.read(
                LogicalDatastoreType.OPERATIONAL, onepIID).get();
            Optional<OwnedNodeEdgePoint> optionalOnep1 = this.networkTransactionService.read(
                LogicalDatastoreType.OPERATIONAL, onep1IID).get();
            if (!optionalOnep.isPresent() || !optionalOnep1.isPresent()) {
                LOG.error("One of the 2 neps doesnt exist in the datastore: {} OR {}", nepUuid, nep1Uuid);
                return null;
            }
            return optionalOnep.orElseThrow().getAdministrativeState().equals(
                        optionalOnep1.orElseThrow().getAdministrativeState())
                ? optionalOnep.orElseThrow().getAdministrativeState().getName()
                : AdministrativeState.UNLOCKED.getName();
        } catch (InterruptedException | ExecutionException e) {
            LOG.error("Failed getting Mapping data from portMapping",e);
            return null;
        }
    }
}
