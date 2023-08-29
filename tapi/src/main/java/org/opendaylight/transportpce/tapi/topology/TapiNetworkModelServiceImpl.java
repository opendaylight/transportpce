/*
 * Copyright © 2021 Nokia, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.tapi.topology;

import com.google.common.collect.ImmutableMap;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import org.opendaylight.mdsal.binding.api.NotificationPublishService;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.transportpce.common.device.DeviceTransactionManager;
import org.opendaylight.transportpce.common.network.NetworkTransactionService;
import org.opendaylight.transportpce.tapi.R2RTapiLinkDiscovery;
import org.opendaylight.transportpce.tapi.TapiStringConstants;
import org.opendaylight.transportpce.tapi.utils.TapiLink;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev220922.mapping.Mapping;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev220922.network.Nodes;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.state.types.rev191129.State;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.types.rev181019.NodeTypes;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.types.rev191129.XpdrNodeTypes;
import org.opendaylight.yang.gen.v1.http.org.openroadm.equipment.states.types.rev191129.AdminStates;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.types.rev201211.xpdr.odu.switching.pools.OduSwitchingPools;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.types.rev201211.xpdr.odu.switching.pools.OduSwitchingPoolsBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.types.rev201211.xpdr.odu.switching.pools.odu.switching.pools.NonBlockingList;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.types.rev201211.xpdr.odu.switching.pools.odu.switching.pools.NonBlockingListBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.types.rev201211.xpdr.odu.switching.pools.odu.switching.pools.NonBlockingListKey;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.types.rev211210.OpenroadmNodeType;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.types.rev211210.xpdr.tp.supported.interfaces.SupportedInterfaceCapability;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.types.rev211210.xpdr.tp.supported.interfaces.SupportedInterfaceCapabilityBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.types.rev211210.xpdr.tp.supported.interfaces.SupportedInterfaceCapabilityKey;
import org.opendaylight.yang.gen.v1.http.org.openroadm.port.types.rev201211.If100GE;
import org.opendaylight.yang.gen.v1.http.org.openroadm.port.types.rev201211.If100GEODU4;
import org.opendaylight.yang.gen.v1.http.org.openroadm.port.types.rev201211.If10GE;
import org.opendaylight.yang.gen.v1.http.org.openroadm.port.types.rev201211.If10GEODU2;
import org.opendaylight.yang.gen.v1.http.org.openroadm.port.types.rev201211.If10GEODU2e;
import org.opendaylight.yang.gen.v1.http.org.openroadm.port.types.rev201211.If1GEODU0;
import org.opendaylight.yang.gen.v1.http.org.openroadm.port.types.rev201211.If400GE;
import org.opendaylight.yang.gen.v1.http.org.openroadm.port.types.rev201211.IfOCH;
import org.opendaylight.yang.gen.v1.http.org.openroadm.port.types.rev201211.IfOCHOTU4ODU4;
import org.opendaylight.yang.gen.v1.http.org.openroadm.port.types.rev201211.IfOTU4ODU4;
import org.opendaylight.yang.gen.v1.http.org.openroadm.port.types.rev201211.IfOtsiOtsigroup;
import org.opendaylight.yang.gen.v1.http.org.openroadm.port.types.rev230526.SupportedIfCapability;
import org.opendaylight.yang.gen.v1.http.org.openroadm.switching.pool.types.rev191129.SwitchingPoolTypes;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.NodeId;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.TpId;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev181210.AdministrativeState;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev181210.Context;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev181210.ContextBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev181210.LAYERPROTOCOLQUALIFIER;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev181210.LayerProtocolName;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev181210.LifecycleState;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev181210.OperationalState;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev181210.PortDirection;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev181210.PortRole;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev181210.TerminationDirection;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev181210.TerminationState;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev181210.Uuid;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev181210.capacity.pac.AvailableCapacityBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev181210.capacity.pac.TotalPotentialCapacityBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev181210.global._class.Name;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev181210.global._class.NameBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev181210.global._class.NameKey;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev181210.tapi.context.ServiceInterfacePoint;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev181210.tapi.context.ServiceInterfacePointBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev181210.tapi.context.ServiceInterfacePointKey;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev181210.OwnedNodeEdgePoint1;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev181210.OwnedNodeEdgePoint1Builder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev181210.cep.list.ConnectionEndPoint;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev181210.cep.list.ConnectionEndPointBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev181210.cep.list.ConnectionEndPointKey;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev181210.connectivity.context.Connection;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev181210.connectivity.context.ConnectionBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev181210.connectivity.context.ConnectionKey;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev181210.connectivity.context.ConnectivityService;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev181210.connectivity.context.ConnectivityServiceBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev181210.connectivity.context.ConnectivityServiceKey;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev181210.connectivity.service.EndPoint;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev181210.connectivity.service.EndPointKey;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev181210.context.ConnectivityContext;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev181210.context.topology.context.topology.node.owned.node.edge.point.CepList;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev181210.context.topology.context.topology.node.owned.node.edge.point.CepListBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.dsr.rev181210.DIGITALSIGNALTYPE100GigE;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.dsr.rev181210.DIGITALSIGNALTYPE10GigELAN;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.dsr.rev181210.DIGITALSIGNALTYPEGigE;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.notification.rev181210.NotificationBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.notification.rev181210.NotificationType;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.notification.rev181210.ObjectType;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.notification.rev181210.notification.ChangedAttributes;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.notification.rev181210.notification.ChangedAttributesBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.notification.rev181210.notification.ChangedAttributesKey;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.odu.rev181210.ODUTYPEODU0;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.odu.rev181210.ODUTYPEODU2;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.odu.rev181210.ODUTYPEODU2E;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.odu.rev181210.ODUTYPEODU4;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.photonic.media.rev181210.PHOTONICLAYERQUALIFIEROMS;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.photonic.media.rev181210.PHOTONICLAYERQUALIFIEROTSi;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.Context1;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.ForwardingRule;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.NodeEdgePointRef;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.RuleType;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.context.TopologyContext;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.node.NodeRuleGroup;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.node.NodeRuleGroupBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.node.NodeRuleGroupKey;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.node.OwnedNodeEdgePoint;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.node.OwnedNodeEdgePointBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.node.OwnedNodeEdgePointKey;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.node.edge.point.MappedServiceInterfacePoint;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.node.edge.point.MappedServiceInterfacePointBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.node.edge.point.MappedServiceInterfacePointKey;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.node.rule.group.NodeEdgePoint;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.node.rule.group.NodeEdgePointBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.node.rule.group.NodeEdgePointKey;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.node.rule.group.Rule;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.node.rule.group.RuleBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.node.rule.group.RuleKey;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.risk.parameter.pac.RiskCharacteristic;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.risk.parameter.pac.RiskCharacteristicBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.topology.Link;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.topology.LinkBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.topology.LinkKey;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.topology.Node;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.topology.NodeBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.topology.NodeKey;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.topology.context.Topology;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.topology.context.TopologyBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.topology.context.TopologyKey;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.transfer.cost.pac.CostCharacteristic;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.transfer.cost.pac.CostCharacteristicBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.transfer.timing.pac.LatencyCharacteristic;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.transfer.timing.pac.LatencyCharacteristicBuilder;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.Notification;
import org.opendaylight.yangtools.yang.common.Uint16;
import org.opendaylight.yangtools.yang.common.Uint32;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


@Component
public class TapiNetworkModelServiceImpl implements TapiNetworkModelService {

    private static final Logger LOG = LoggerFactory.getLogger(TapiNetworkModelServiceImpl.class);

    private final Uuid tapiTopoUuid = new Uuid(UUID.nameUUIDFromBytes(TapiStringConstants.T0_FULL_MULTILAYER
            .getBytes(StandardCharsets.UTF_8)).toString());
    private final NetworkTransactionService networkTransactionService;
    private final R2RTapiLinkDiscovery linkDiscovery;
    private final TapiLink tapiLink;
    private final NotificationPublishService notificationPublishService;
    private Map<ServiceInterfacePointKey, ServiceInterfacePoint> sipMap = new HashMap<>();

    @Activate
    public TapiNetworkModelServiceImpl(@Reference NetworkTransactionService networkTransactionService,
            @Reference DeviceTransactionManager deviceTransactionManager,
            @Reference TapiLink tapiLink,
            @Reference final NotificationPublishService notificationPublishService) {
        this.networkTransactionService = networkTransactionService;
        this.linkDiscovery = new R2RTapiLinkDiscovery(networkTransactionService, deviceTransactionManager, tapiLink);
        this.tapiLink = tapiLink;
        this.notificationPublishService = notificationPublishService;
    }

    @Override
    public void createTapiNode(String orNodeId, int orNodeVersion, Nodes node) {
        // TODO -> Implementation with PortMappingListener
        // check if port mapping exists or not...
        if (node.getMapping() == null) {
            LOG.warn("Could not generate port mapping for {} skipping network model creation", orNodeId);
            return;
        }
        this.sipMap.clear();
        LOG.info("Mapping of node {}: {}", orNodeId, node.getMapping().values());

        // check type of device, check version and create node mapping
        if (NodeTypes.Rdm.getIntValue() == node.getNodeInfo().getNodeType().getIntValue()) {
            // ROADM device
            // transform flat mapping list to per degree and per srg mapping lists
            Map<String, List<Mapping>> mapDeg = new HashMap<>();
            Map<String, List<Mapping>> mapSrg = new HashMap<>();
            List<Mapping> mappingList = new ArrayList<>(node.nonnullMapping().values());
            mappingList.sort(Comparator.comparing(Mapping::getLogicalConnectionPoint));

            List<String> nodeShardList = getRoadmNodelist(mappingList);

            // populate degree and srg LCP map
            for (String str : nodeShardList) {
                List<Mapping> interList = mappingList.stream().filter(x -> x.getLogicalConnectionPoint().contains(str))
                        .collect(Collectors.toList());
                if (str.contains("DEG")) {
                    mapDeg.put(str, interList);
                } else if (str.contains("SRG")) {
                    mapSrg.put(str, interList);
                } else {
                    LOG.error("unknown element");
                }
            }
            // Transform LCPs into ONEP
            Map<OwnedNodeEdgePointKey, OwnedNodeEdgePoint> onepMap =
                new HashMap<>(transformDegToOnep(orNodeId, mapDeg));
            onepMap.putAll(transformSrgToOnep(orNodeId, mapSrg));

            // create tapi Node
            Node roadmNode = createRoadmTapiNode(orNodeId, onepMap);
            mergeNodeinTopology(Map.of(roadmNode.key(), roadmNode));
            mergeSipsinContext(this.sipMap);
            // TODO add states corresponding to device config -> based on mapping.
            //  This should be possible after Gilles work is merged

            // rdm to rdm link creation if neighbour roadm is mounted
            LOG.info("checking if neighbor roadm exists");
            Map<LinkKey, Link> rdm2rdmLinks = this.linkDiscovery.readLLDP(new NodeId(orNodeId), orNodeVersion,
                this.tapiTopoUuid);
            if (!rdm2rdmLinks.isEmpty()) {
                mergeLinkinTopology(rdm2rdmLinks);
            }
            LOG.info("TAPI node for or node {} successfully merged", orNodeId);
        } else if (NodeTypes.Xpdr.getIntValue() ==  node.getNodeInfo().getNodeType().getIntValue()) {
            List<Mapping> networkMappings = node.nonnullMapping().values()
                    .stream().filter(k -> k.getLogicalConnectionPoint()
                            .contains("NETWORK")).collect(Collectors.toList());
            Map<Integer, String> xpdrMap = new HashMap<>();
            for (Mapping mapping : networkMappings) {
                Integer xpdrNb = Integer.parseInt(mapping.getLogicalConnectionPoint().split("XPDR")[1].split("-")[0]);
                String nodeId = node.getNodeId() + TapiStringConstants.XPDR + xpdrNb;
                if (!xpdrMap.containsKey(xpdrNb)) {
                    List<Mapping> xpdrNetMaps = node.nonnullMapping().values()
                        .stream().filter(k -> k.getLogicalConnectionPoint()
                            .contains("XPDR" + xpdrNb + TapiStringConstants.NETWORK)).collect(Collectors.toList());
                    List<Mapping> xpdrClMaps = node.nonnullMapping().values()
                        .stream().filter(k -> k.getLogicalConnectionPoint()
                            .contains("XPDR" + xpdrNb + TapiStringConstants.CLIENT)).collect(Collectors.toList());
                    xpdrMap.put(xpdrNb, node.getNodeId());

                    // create switching pool
                    OduSwitchingPools oorOduSwitchingPool = createSwitchPoolForXpdr(
                        mapping.getXpdrType().getIntValue(), xpdrClMaps, xpdrNetMaps, xpdrNb);

                    // node transformation
                    Map<NodeKey, Node> nodeMap = new HashMap<>(transformXpdrToTapiNode(
                        nodeId, xpdrClMaps, xpdrNetMaps, mapping.getXpdrType(), oorOduSwitchingPool));
                    // add nodes and sips to tapi context
                    mergeNodeinTopology(nodeMap);
                    mergeSipsinContext(this.sipMap);
                }
            }
            LOG.info("TAPI node for or node {} successfully merged", orNodeId);
        }
        // Device not managed yet
    }

    @Override
    public void updateTapiTopology(String nodeId, Mapping mapping) {
        List<Uuid> uuids = getChangedNodeUuids(nodeId, mapping);

        List<Uuid> changedOneps = updateNeps(mapping, uuids);
        updateLinks(changedOneps, mapping);
        sendNotification(changedOneps, mapping);

        LOG.info("Updated TAPI topology successfully.");
    }

    @SuppressWarnings("rawtypes")
    private void sendNotification(List<Uuid> changedOneps, Mapping mapping) {
        Notification notification = new NotificationBuilder()
            .setNotificationType(NotificationType.ATTRIBUTEVALUECHANGE)
            .setTargetObjectType(ObjectType.NODEEDGEPOINT)
            .setChangedAttributes(getChangedAttributes(changedOneps, mapping))
            .setUuid(tapiTopoUuid)
            .build();
        try {
            notificationPublishService.putNotification(notification);
        } catch (InterruptedException e) {
            LOG.error("Could not send notification");
        }
    }

    private Map<ChangedAttributesKey, ChangedAttributes> getChangedAttributes(List<Uuid> changedOneps,
                                                                              Mapping mapping) {
        Map<ChangedAttributesKey, ChangedAttributes> changedAttributes = new HashMap<>();
        for (Uuid nep : changedOneps) {
            changedAttributes.put(new ChangedAttributesKey(nep.getValue()),
                new ChangedAttributesBuilder().setValueName(nep.getValue())
                    .setOldValue(mapping.getPortOperState().equals("InService") ? "OutOfService" : "InService")
                    .setNewValue(mapping.getPortOperState())
                    .build());
        }
        return changedAttributes;
    }

    private void updateLinks(List<Uuid> changedOneps, Mapping mapping) {
        try {
            InstanceIdentifier<Topology> topoIID = InstanceIdentifier.builder(Context.class)
                    .augmentation(Context1.class).child(TopologyContext.class)
                    .child(Topology.class, new TopologyKey(tapiTopoUuid))
                    .build();
            Optional<Topology> optTopology = this.networkTransactionService
                    .read(LogicalDatastoreType.OPERATIONAL, topoIID).get();
            if (optTopology.isEmpty()) {
                LOG.error("Could not update TAPI links");
                return;
            }
            for (Link link : optTopology.orElseThrow().nonnullLink().values()) {
                List<Uuid> linkNeps = Objects.requireNonNull(link.getNodeEdgePoint()).values().stream()
                        .map(NodeEdgePointRef::getNodeEdgePointUuid).collect(Collectors.toList());
                if (!Collections.disjoint(changedOneps, linkNeps)) {
                    InstanceIdentifier<Link> linkIID = InstanceIdentifier.builder(Context.class)
                            .augmentation(Context1.class).child(TopologyContext.class)
                            .child(Topology.class, new TopologyKey(tapiTopoUuid))
                            .child(Link.class, new LinkKey(link.getUuid())).build();
                    Link linkblr = new LinkBuilder().setUuid(link.getUuid())
                            .setAdministrativeState(transformAdminState(mapping.getPortAdminState()))
                            .setOperationalState(transformOperState(mapping.getPortOperState())).build();
                    this.networkTransactionService.merge(LogicalDatastoreType.OPERATIONAL, linkIID, linkblr);
                }
            }
            this.networkTransactionService.commit().get();
        } catch (InterruptedException | ExecutionException e) {
            LOG.error("Could not update TAPI links");
        }
    }

    private List<Uuid> updateNeps(Mapping mapping, List<Uuid> uuids) {
        List<Uuid> changedOneps = new ArrayList<>();
        for (Uuid nodeUuid : uuids) {
            try {
                InstanceIdentifier<Node> nodeIID = InstanceIdentifier.builder(Context.class)
                        .augmentation(Context1.class).child(TopologyContext.class)
                        .child(Topology.class, new TopologyKey(tapiTopoUuid)).child(Node.class, new NodeKey(nodeUuid))
                        .build();
                Optional<Node> optionalNode = this.networkTransactionService.read(
                        LogicalDatastoreType.OPERATIONAL, nodeIID).get();
                if (optionalNode.isPresent()) {
                    Node node = optionalNode.orElseThrow();
                    List<OwnedNodeEdgePoint> oneps = node.getOwnedNodeEdgePoint().values().stream()
                            .filter(onep -> ((Name) onep.getName().values().toArray()[0]).getValue()
                                    .contains(mapping.getLogicalConnectionPoint())).collect(Collectors.toList());
                    for (OwnedNodeEdgePoint onep : oneps) {
                        changedOneps.add(onep.getUuid());
                        updateSips(mapping, onep);
                        CepList cepList = getUpdatedCeps(mapping, onep);
                        InstanceIdentifier<OwnedNodeEdgePoint> onepIID = InstanceIdentifier.builder(Context.class)
                                .augmentation(Context1.class).child(TopologyContext.class)
                                .child(Topology.class, new TopologyKey(tapiTopoUuid))
                                .child(Node.class, new NodeKey(nodeUuid))
                                .child(OwnedNodeEdgePoint.class, new OwnedNodeEdgePointKey(onep.getUuid()))
                                .build();
                        OwnedNodeEdgePoint onepblr = new OwnedNodeEdgePointBuilder().setUuid(onep.getUuid())
                                .addAugmentation(new OwnedNodeEdgePoint1Builder().setCepList(cepList).build())
                                .setAdministrativeState(transformAdminState(mapping.getPortAdminState()))
                                .setOperationalState(transformOperState(mapping.getPortOperState())).build();
                        this.networkTransactionService.merge(LogicalDatastoreType.OPERATIONAL, onepIID, onepblr);
                    }
                    this.networkTransactionService.commit().get();
                }
            } catch (InterruptedException | ExecutionException e) {
                LOG.error("Could not update TAPI NEP");
            }
        }
        return changedOneps;
    }

    private CepList getUpdatedCeps(Mapping mapping, OwnedNodeEdgePoint onep) {
        OwnedNodeEdgePoint1 onep1 = onep.augmentation(OwnedNodeEdgePoint1.class);
        Map<ConnectionEndPointKey, ConnectionEndPoint> cepMap = new HashMap<>();
        if (onep1 != null && onep1.getCepList() != null && onep1.getCepList().getConnectionEndPoint() != null) {
            for (Map.Entry<ConnectionEndPointKey, ConnectionEndPoint> entry : onep1.getCepList().getConnectionEndPoint()
                    .entrySet()) {
                ConnectionEndPoint cep = new ConnectionEndPointBuilder(entry.getValue())
                        .setOperationalState(transformOperState(mapping.getPortOperState())).build();
                cepMap.put(entry.getKey(), cep);
            }
        }
        return new CepListBuilder().setConnectionEndPoint(cepMap).build();
    }

    private List<Uuid> getChangedNodeUuids(String nodeId, Mapping mapping) {
        List<Uuid> uuids = new ArrayList<>();
        if (nodeId.contains("ROADM")) {
            uuids.add(new Uuid(UUID.nameUUIDFromBytes((String.join("+", nodeId, TapiStringConstants.PHTNC_MEDIA))
                    .getBytes(StandardCharsets.UTF_8)).toString()));
        } else if (nodeId.contains("PDR") && mapping.getLogicalConnectionPoint().contains("CLIENT")) {
            int xpdrNb = Integer.parseInt(mapping.getLogicalConnectionPoint().split("XPDR")[1].split("-")[0]);
            String xpdrNodeId = nodeId + TapiStringConstants.XPDR + xpdrNb;
            uuids.add(new Uuid(UUID.nameUUIDFromBytes((String.join("+", xpdrNodeId, TapiStringConstants.DSR))
                    .getBytes(StandardCharsets.UTF_8)).toString()));
        } else if (nodeId.contains("PDR") && mapping.getLogicalConnectionPoint().contains("NETWORK")) {
            int xpdrNb = Integer.parseInt(mapping.getLogicalConnectionPoint().split("XPDR")[1].split("-")[0]);
            String xpdrNodeId = nodeId + TapiStringConstants.XPDR + xpdrNb;
            uuids.add(new Uuid(UUID.nameUUIDFromBytes((String.join("+", xpdrNodeId, TapiStringConstants.DSR))
                    .getBytes(StandardCharsets.UTF_8)).toString()));
            uuids.add(new Uuid(UUID.nameUUIDFromBytes((String.join("+", xpdrNodeId, TapiStringConstants.OTSI))
                    .getBytes(StandardCharsets.UTF_8)).toString()));
        } else {
            LOG.error("Updating this device is currently not supported");
            return uuids;
        }
        return uuids;
    }

    private void updateSips(Mapping mapping, OwnedNodeEdgePoint onep) {
        if (onep.getMappedServiceInterfacePoint() == null
                || onep.getMappedServiceInterfacePoint().size() == 0) {
            return;
        }
        for (MappedServiceInterfacePoint msip : onep.getMappedServiceInterfacePoint().values()) {
            InstanceIdentifier<ServiceInterfacePoint> sipIID = InstanceIdentifier
                    .builder(Context.class)
                    .child(ServiceInterfacePoint.class,
                            new ServiceInterfacePointKey(msip.getServiceInterfacePointUuid()))
                    .build();
            ServiceInterfacePoint sipblr = new ServiceInterfacePointBuilder()
                    .setUuid(msip.getServiceInterfacePointUuid())
                    .setAdministrativeState(transformAdminState(mapping.getPortAdminState()))
                    .setOperationalState(transformOperState(mapping.getPortOperState())).build();
            this.networkTransactionService.merge(LogicalDatastoreType.OPERATIONAL, sipIID, sipblr);
        }

    }

    private Map<NodeKey, Node> transformXpdrToTapiNode(String nodeId, List<Mapping> xpdrClMaps,
                                                       List<Mapping> xpdrNetMaps, XpdrNodeTypes xponderType,
                                                       OduSwitchingPools oorOduSwitchingPool) {
        Map<NodeKey, Node> nodeMap = new HashMap<>();
        LOG.info("creation of a DSR/ODU node for {}", nodeId);
        Uuid nodeUuidDsr = new Uuid(UUID.nameUUIDFromBytes((String.join("+", nodeId, TapiStringConstants.DSR))
            .getBytes(StandardCharsets.UTF_8)).toString());
        Name nameDsr = new NameBuilder().setValueName("dsr/odu node name").setValue(
            String.join("+", nodeId, TapiStringConstants.DSR)).build();
        Name nameNodeType = new NameBuilder().setValueName("Node Type")
            .setValue(getNodeType(xponderType)).build();
        Set<LayerProtocolName> dsrLayerProtocols = Set.of(LayerProtocolName.DSR, LayerProtocolName.ODU);
        Node dsrNode = createTapiXpdrNode(Map.of(nameDsr.key(), nameDsr, nameNodeType.key(), nameNodeType),
            dsrLayerProtocols, nodeId, nodeUuidDsr, xpdrClMaps, xpdrNetMaps, xponderType, oorOduSwitchingPool);

        nodeMap.put(dsrNode.key(), dsrNode);

        // node creation [otsi]
        LOG.info("creation of an OTSi node for {}", nodeId);
        Uuid nodeUuidOtsi = new Uuid(UUID.nameUUIDFromBytes((String.join("+", nodeId, TapiStringConstants.OTSI))
            .getBytes(StandardCharsets.UTF_8)).toString());
        Name nameOtsi =  new NameBuilder().setValueName("otsi node name").setValue(
            String.join("+", nodeId, TapiStringConstants.OTSI)).build();
        Set<LayerProtocolName> otsiLayerProtocols = Set.of(LayerProtocolName.PHOTONICMEDIA);
        Node otsiNode = createTapiXpdrNode(Map.of(nameOtsi.key(), nameOtsi, nameNodeType.key(), nameNodeType),
            otsiLayerProtocols, nodeId, nodeUuidOtsi, xpdrClMaps, xpdrNetMaps, xponderType, null);

        nodeMap.put(otsiNode.key(), otsiNode);

        // transitional link cration between network nep of DSR/ODU node and iNep of otsi node
        LOG.info("creation of transitional links between DSR/ODU and OTSi nodes");
        Map<LinkKey, Link> linkMap = createTapiTransitionalLinks(nodeId, xpdrNetMaps);
        mergeLinkinTopology(linkMap);

        return nodeMap;
    }

    private OduSwitchingPools createSwitchPoolForXpdr(int xpdrType, List<Mapping> xpdrClMaps, List<Mapping> xpdrNetMaps,
                                                      Integer xpdrNb) {
        // todo: are switching pool correct here??
        switch (xpdrType) {
            case 1:
                // Tpdr
                return createTpdrSwitchPool(xpdrNetMaps);
            case 2:
                // Mux
                return createMuxSwitchPool(xpdrClMaps, xpdrNetMaps, xpdrNb);
            case 3:
                // Switch
                return createSwtchSwitchPool(xpdrClMaps, xpdrNetMaps, xpdrNb);
            default:
                LOG.warn("Xpdr type {} not supported", xpdrType);
        }
        return null;
    }

    private Map<OwnedNodeEdgePointKey, OwnedNodeEdgePoint> transformSrgToOnep(String orNodeId,
                                                                              Map<String, List<Mapping>> mapSrg) {
        Map<OwnedNodeEdgePointKey, OwnedNodeEdgePoint> onepMap = new HashMap<>();
        for (Map.Entry<String, List<Mapping>> entry : mapSrg.entrySet()) {
            // For each srg node. Loop through the LCPs and create neps and sips for PP
            for (Mapping m:entry.getValue()) {
                if (!m.getLogicalConnectionPoint().contains("PP")) {
                    LOG.info("LCP {} is not an external TP of SRG node", m.getLogicalConnectionPoint());
                    continue;
                }
                Map<OwnedNodeEdgePointKey, OwnedNodeEdgePoint> srgNeps =
                    createRoadmNeps(orNodeId, m.getLogicalConnectionPoint(), true,
                            transformOperState(m.getPortOperState()), transformAdminState(m.getPortAdminState()));
                onepMap.putAll(srgNeps);
            }
        }
        return onepMap;
    }

    private Map<OwnedNodeEdgePointKey, OwnedNodeEdgePoint> transformDegToOnep(String orNodeId,
                                                                              Map<String, List<Mapping>> mapDeg) {
        Map<OwnedNodeEdgePointKey, OwnedNodeEdgePoint> onepMap = new HashMap<>();
        for (Map.Entry<String, List<Mapping>> entry : mapDeg.entrySet()) {
            // For each degree node. Loop through the LCPs and create neps and sips for TTP
            for (Mapping m:entry.getValue()) {
                if (!m.getLogicalConnectionPoint().contains("TTP")) {
                    LOG.info("LCP {} is not an external TP of DEGREE node", m.getLogicalConnectionPoint());
                    continue;
                }
                Map<OwnedNodeEdgePointKey, OwnedNodeEdgePoint> degNeps =
                    createRoadmNeps(orNodeId, m.getLogicalConnectionPoint(), false,
                            transformOperState(m.getPortOperState()), transformAdminState(m.getPortAdminState()));
                onepMap.putAll(degNeps);
            }
        }
        return onepMap;
    }

    private List<String> getRoadmNodelist(List<Mapping> mappingList) {
        List<String> nodeShardList = new ArrayList<>();
        for (Mapping mapping : mappingList) {
            // TODO -> maybe we need to check the id based on the version
            String str = mapping.getLogicalConnectionPoint().split("-")[0];
            LOG.info("LCP = {}", str);
            if (!nodeShardList.contains(str)) {
                nodeShardList.add(str);
            }
        }
        return nodeShardList;
    }

    @Override
    public void deleteTapinode(String nodeId) {
        // TODO: check for null objects
        // Check if it is ROADM or XPDR --> create the uuids of the node and delete from topology the node.
        // This will delete NEPs. Then check for links that have this node and delete them.
        // Then check SIPs and delete them. Then services and connections with SIPs and put them to another state.
        LOG.info("Deleting node {} from TAPI topology", nodeId);
        InstanceIdentifier<Topology> topologyIID = InstanceIdentifier.builder(Context.class)
                .augmentation(Context1.class).child(TopologyContext.class).child(Topology.class,
                        new TopologyKey(tapiTopoUuid)).build();
        Topology topology = null;
        try {
            Optional<Topology> optTopology =
                    this.networkTransactionService.read(LogicalDatastoreType.OPERATIONAL, topologyIID).get();
            if (!optTopology.isPresent()) {
                LOG.error("No topology object present. Error deleting node {}", nodeId);
                return;
            }
            topology = optTopology.orElseThrow();
        } catch (InterruptedException | ExecutionException e) {
            LOG.error("Couldnt read tapi topology from datastore", e);
        }
        if (topology == null) {
            LOG.error("Topology is null, nothing to delete");
            return;
        }
        if (topology.getNode() == null) {
            LOG.error("No nodes in topology");
            return;
        }
        if (nodeId.contains("ROADM")) {
            // Node is in photonic media layer and UUID can be built from nodeId + PHTN_MEDIA
            Uuid nodeUuid = new Uuid(UUID.nameUUIDFromBytes((String.join("+", nodeId,
                TapiStringConstants.PHTNC_MEDIA)).getBytes(StandardCharsets.UTF_8)).toString());
            deleteNodeFromTopo(nodeUuid);
        }
        if (nodeId.contains("XPDR") || nodeId.contains("SPDR") || nodeId.contains("MXPDR")) {
            // Node is either XPDR, MXPDR or SPDR. Retrieve nodes from topology and check names
            for (Node tapiNode:topology.getNode().values()) {
                if (tapiNode.getName().values().stream().anyMatch(name -> name.getValue().contains(nodeId))) {
                    // Found node we need to delete
                    deleteNodeFromTopo(tapiNode.getUuid());
                }
            }
        }
        // Delete links of topology
        Map<LinkKey, Link> linkMap = topology.getLink();
        if (linkMap != null) {
            for (Link link:linkMap.values()) {
                if (link.getName().values().stream().anyMatch(name -> name.getValue().contains(nodeId))) {
                    deleteLinkFromTopo(link.getUuid());
                }
            }
        }
        // Delete sips of sip map
        InstanceIdentifier<Context> contextIID = InstanceIdentifier.builder(Context.class).build();
        Context context = null;
        try {
            Optional<Context> optContext = this.networkTransactionService.read(LogicalDatastoreType.OPERATIONAL,
                    contextIID).get();
            if (!optContext.isPresent()) {
                LOG.error("No context object present in datastore.");
                return;
            }
            context = optContext.orElseThrow();
        } catch (InterruptedException | ExecutionException e) {
            LOG.error("Couldnt read tapi context from datastore", e);
        }
        if (context == null) {
            LOG.error("Context is null, nothing to delete");
            return;
        }
        Map<ServiceInterfacePointKey, ServiceInterfacePoint> sips = context.getServiceInterfacePoint();
        if (sips != null) {
            for (ServiceInterfacePoint sip:sips.values()) {
                if (sip.getName().values().stream().anyMatch(name -> name.getValue().contains(nodeId))) {
                    // Update state of services that have this sip as an endpoint and also connections
                    updateConnectivityServicesState(sip.getUuid(), nodeId);
                    deleteSipFromTopo(sip.getUuid());
                }
            }
        }
    }

    private Node createTapiXpdrNode(Map<NameKey, Name> nameMap, Set<LayerProtocolName> layerProtocols,
                                    String nodeId, Uuid nodeUuid, List<Mapping> xpdrClMaps, List<Mapping> xpdrNetMaps,
                                    XpdrNodeTypes xponderType, OduSwitchingPools oorOduSwitchingPool) {
        Map<OwnedNodeEdgePointKey, OwnedNodeEdgePoint> onepl = new HashMap<>();
        Map<NodeRuleGroupKey, NodeRuleGroup> nodeRuleGroupList = new HashMap<>();
        Map<RuleKey, Rule> ruleList = new HashMap<>();
        Rule rule = new RuleBuilder()
                .setLocalId("forward")
                .setForwardingRule(ForwardingRule.MAYFORWARDACROSSGROUP)
                .setRuleType(RuleType.FORWARDING)
                .build();
        ruleList.put(rule.key(), rule);
        if (layerProtocols.contains(LayerProtocolName.DSR)) {
            // neps for dsr/odu layer
            Map<OwnedNodeEdgePointKey, OwnedNodeEdgePoint> dsroduNeps =
                    createXpdrDsrOduNeps(nodeId, xpdrClMaps, xpdrNetMaps, xponderType);
            onepl.putAll(dsroduNeps);
            nodeRuleGroupList = createNodeRuleGroupForDsrNode(nodeId, oorOduSwitchingPool, ruleList, onepl);
        } else if (layerProtocols.contains(LayerProtocolName.PHOTONICMEDIA)) {
            // neps for photonic layer
            Map<OwnedNodeEdgePointKey, OwnedNodeEdgePoint> phtmdNeps =
                    createXpdrPhtnMdNeps(nodeId, xpdrNetMaps);
            onepl.putAll(phtmdNeps);
            nodeRuleGroupList = createNodeRuleGroupForOtsiNode(nodeId, xpdrNetMaps, ruleList);
        } else {
            LOG.error("Undefined LayerProtocolName for {} node {}", nameMap.get(nameMap.keySet().iterator().next())
                    .getValueName(), nameMap.get(nameMap.keySet().iterator().next()).getValue());
        }
        // Empty random creation of mandatory fields for avoiding errors....
        CostCharacteristic costCharacteristic = new CostCharacteristicBuilder()
            .setCostAlgorithm("Restricted Shortest Path - RSP")
            .setCostName("HOP_COUNT")
            .setCostValue(TapiStringConstants.COST_HOP_VALUE)
            .build();
        LatencyCharacteristic latencyCharacteristic = new LatencyCharacteristicBuilder()
            .setFixedLatencyCharacteristic(TapiStringConstants.FIXED_LATENCY_VALUE)
            .setQueingLatencyCharacteristic(TapiStringConstants.QUEING_LATENCY_VALUE)
            .setJitterCharacteristic(TapiStringConstants.JITTER_VALUE)
            .setWanderCharacteristic(TapiStringConstants.WANDER_VALUE)
            .setTrafficPropertyName("FIXED_LATENCY")
            .build();
        return new NodeBuilder()
            .setUuid(nodeUuid)
            .setName(nameMap)
            .setLayerProtocolName(layerProtocols)
            .setAdministrativeState(AdministrativeState.UNLOCKED)
            .setOperationalState(OperationalState.ENABLED)
            .setLifecycleState(LifecycleState.INSTALLED)
            .setOwnedNodeEdgePoint(onepl)
            .setNodeRuleGroup(nodeRuleGroupList)
            .setCostCharacteristic(Map.of(costCharacteristic.key(), costCharacteristic))
            .setLatencyCharacteristic(Map.of(latencyCharacteristic.key(), latencyCharacteristic))
            .setErrorCharacteristic("error")
            .setLossCharacteristic("loss")
            .setRepeatDeliveryCharacteristic("repeat delivery")
            .setDeliveryOrderCharacteristic("delivery order")
            .setUnavailableTimeCharacteristic("unavailable time")
            .setServerIntegrityProcessCharacteristic("server integrity process")
            .build();
    }

    private Map<OwnedNodeEdgePointKey, OwnedNodeEdgePoint> createXpdrPhtnMdNeps(String nodeId,
                                                                                List<Mapping> xpdrNetMaps) {
        Map<OwnedNodeEdgePointKey, OwnedNodeEdgePoint> onepl = new HashMap<>();

        // iNep creation on otsi node
        for (int i = 0; i < xpdrNetMaps.size(); i++) {
            Uuid nepUuid1 = new Uuid(UUID.nameUUIDFromBytes(
                (String.join("+", nodeId, TapiStringConstants.I_OTSI,
                    xpdrNetMaps.get(i).getLogicalConnectionPoint())).getBytes(StandardCharsets.UTF_8)).toString());
            Name onedName = new NameBuilder()
                .setValueName("iNodeEdgePoint")
                .setValue(String.join("+", nodeId, TapiStringConstants.I_OTSI,
                    xpdrNetMaps.get(i).getLogicalConnectionPoint()))
                .build();

            List<SupportedIfCapability> newSupIfCapList =
                    new ArrayList<>(xpdrNetMaps.get(i).getSupportedInterfaceCapability());

            OwnedNodeEdgePoint onep = createNep(nepUuid1, xpdrNetMaps.get(i).getLogicalConnectionPoint(),
                Map.of(onedName.key(), onedName), LayerProtocolName.PHOTONICMEDIA, LayerProtocolName.PHOTONICMEDIA,
                true, String.join("+", nodeId, TapiStringConstants.I_OTSI), newSupIfCapList,
                transformOperState(xpdrNetMaps.get(i).getPortOperState()),
                transformAdminState(xpdrNetMaps.get(i).getPortAdminState()));
            onepl.put(onep.key(), onep);
        }
        // eNep creation on otsi node
        for (int i = 0; i < xpdrNetMaps.size(); i++) {
            Uuid nepUuid2 = new Uuid(UUID.nameUUIDFromBytes(
                (String.join("+", nodeId, TapiStringConstants.E_OTSI,
                    xpdrNetMaps.get(i).getLogicalConnectionPoint())).getBytes(StandardCharsets.UTF_8)).toString());
            Name onedName = new NameBuilder()
                .setValueName("eNodeEdgePoint")
                .setValue(String.join("+", nodeId, TapiStringConstants.E_OTSI,
                    xpdrNetMaps.get(i).getLogicalConnectionPoint()))
                .build();

            List<SupportedIfCapability> newSupIfCapList =
                    new ArrayList<>(xpdrNetMaps.get(i).getSupportedInterfaceCapability());

            OwnedNodeEdgePoint onep = createNep(nepUuid2, xpdrNetMaps.get(i).getLogicalConnectionPoint(),
                Map.of(onedName.key(), onedName), LayerProtocolName.PHOTONICMEDIA, LayerProtocolName.PHOTONICMEDIA,
                false, String.join("+", nodeId, TapiStringConstants.E_OTSI), newSupIfCapList,
                transformOperState(xpdrNetMaps.get(i).getPortOperState()),
                transformAdminState(xpdrNetMaps.get(i).getPortAdminState()));
            onepl.put(onep.key(), onep);
        }
        // Photonic Media Nep creation on otsi node
        for (int i = 0; i < xpdrNetMaps.size(); i++) {
            Uuid nepUuid3 = new Uuid(UUID.nameUUIDFromBytes(
                (String.join("+", nodeId, TapiStringConstants.PHTNC_MEDIA,
                    xpdrNetMaps.get(i).getLogicalConnectionPoint())).getBytes(StandardCharsets.UTF_8)).toString());
            Name onedName = new NameBuilder()
                .setValueName("PhotMedNodeEdgePoint")
                .setValue(String.join("+", nodeId, TapiStringConstants.PHTNC_MEDIA,
                    xpdrNetMaps.get(i).getLogicalConnectionPoint()))
                .build();

            List<SupportedIfCapability> newSupIfCapList =
                    new ArrayList<>(xpdrNetMaps.get(i).getSupportedInterfaceCapability());

            OwnedNodeEdgePoint onep = createNep(nepUuid3, xpdrNetMaps.get(i).getLogicalConnectionPoint(),
                Map.of(onedName.key(), onedName), LayerProtocolName.PHOTONICMEDIA, LayerProtocolName.PHOTONICMEDIA,
                false, String.join("+", nodeId, TapiStringConstants.PHTNC_MEDIA), newSupIfCapList,
                transformOperState(xpdrNetMaps.get(i).getPortOperState()),
                transformAdminState(xpdrNetMaps.get(i).getPortAdminState()));
            onepl.put(onep.key(), onep);
        }
        return onepl;
    }

    private Map<OwnedNodeEdgePointKey, OwnedNodeEdgePoint> createXpdrDsrOduNeps(String nodeId, List<Mapping> xpdrClMaps,
                                                                                List<Mapping> xpdrNetMaps,
                                                                                XpdrNodeTypes xponderType) {
        Map<OwnedNodeEdgePointKey, OwnedNodeEdgePoint> onepl = new HashMap<>();
        // client nep creation on DSR node
        for (int i = 0; i < xpdrClMaps.size(); i++) {
            LOG.info("Client NEP = {}", String.join("+", nodeId, TapiStringConstants.DSR,
                xpdrClMaps.get(i).getLogicalConnectionPoint()));
            Uuid nepUuid = new Uuid(UUID.nameUUIDFromBytes(
                (String.join("+", nodeId, TapiStringConstants.DSR,
                    xpdrClMaps.get(i).getLogicalConnectionPoint())).getBytes(StandardCharsets.UTF_8)).toString());
            NameBuilder nameBldr = new NameBuilder().setValue(String.join("+", nodeId,
                TapiStringConstants.DSR, xpdrClMaps.get(i).getLogicalConnectionPoint()));
            Name name;
            if (OpenroadmNodeType.TPDR.getName().equalsIgnoreCase(xponderType.getName())) {
                name = nameBldr.setValueName("100G-tpdr").build();
            } else {
                name = nameBldr.setValueName("NodeEdgePoint_C").build();
            }

            List<SupportedIfCapability> newSupIfCapList =
                    new ArrayList<>(xpdrClMaps.get(i).getSupportedInterfaceCapability());

            OwnedNodeEdgePoint onep = createNep(nepUuid, xpdrClMaps.get(i).getLogicalConnectionPoint(),
                Map.of(name.key(), name), LayerProtocolName.DSR, LayerProtocolName.DSR, true,
                String.join("+", nodeId, TapiStringConstants.DSR), newSupIfCapList,
                transformOperState(xpdrClMaps.get(i).getPortOperState()),
                transformAdminState(xpdrClMaps.get(i).getPortAdminState()));
            onepl.put(onep.key(), onep);
        }
        // network nep creation on I_ODU node
        for (int i = 0; i < xpdrNetMaps.size(); i++) {
            LOG.info("iODU NEP = {}", String.join("+", nodeId, TapiStringConstants.I_ODU,
                xpdrNetMaps.get(i).getLogicalConnectionPoint()));
            Uuid nepUuid = new Uuid(UUID.nameUUIDFromBytes(
                (String.join("+", nodeId, TapiStringConstants.I_ODU,
                    xpdrNetMaps.get(i).getLogicalConnectionPoint())).getBytes(StandardCharsets.UTF_8)).toString());
            Name onedName = new NameBuilder()
                .setValueName("iNodeEdgePoint_N")
                .setValue(String.join("+", nodeId, TapiStringConstants.I_ODU,
                    xpdrNetMaps.get(i).getLogicalConnectionPoint()))
                .build();

            List<SupportedIfCapability> newSupIfCapList =
                    new ArrayList<>(xpdrNetMaps.get(i).getSupportedInterfaceCapability());

            OwnedNodeEdgePoint onep = createNep(nepUuid, xpdrNetMaps.get(i).getLogicalConnectionPoint(),
                Map.of(onedName.key(), onedName),
                LayerProtocolName.ODU, LayerProtocolName.DSR, true,
                String.join("+", nodeId, TapiStringConstants.I_ODU), newSupIfCapList,
                transformOperState(xpdrNetMaps.get(i).getPortOperState()),
                transformAdminState(xpdrNetMaps.get(i).getPortAdminState()));
            onepl.put(onep.key(), onep);
        }
        // network nep creation on E_ODU node
        for (int i = 0; i < xpdrClMaps.size(); i++) {
            LOG.info("eODU NEP = {}", String.join("+", nodeId, TapiStringConstants.E_ODU,
                xpdrClMaps.get(i).getLogicalConnectionPoint()));
            Uuid nepUuid = new Uuid(UUID.nameUUIDFromBytes(
                (String.join("+", nodeId, TapiStringConstants.E_ODU,
                    xpdrClMaps.get(i).getLogicalConnectionPoint())).getBytes(StandardCharsets.UTF_8)).toString());
            Name onedName = new NameBuilder()
                .setValueName("eNodeEdgePoint_N")
                .setValue(String.join("+", nodeId, TapiStringConstants.E_ODU,
                    xpdrClMaps.get(i).getLogicalConnectionPoint()))
                .build();

            List<SupportedIfCapability> newSupIfCapList =
                    new ArrayList<>(xpdrClMaps.get(i).getSupportedInterfaceCapability());

            OwnedNodeEdgePoint onep = createNep(nepUuid, xpdrClMaps.get(i).getLogicalConnectionPoint(),
                Map.of(onedName.key(), onedName),
                LayerProtocolName.ODU, LayerProtocolName.DSR, false,
                String.join("+", nodeId, TapiStringConstants.E_ODU), newSupIfCapList,
                transformOperState(xpdrClMaps.get(i).getPortOperState()),
                transformAdminState(xpdrClMaps.get(i).getPortAdminState()));
            onepl.put(onep.key(), onep);
        }
        return onepl;
    }

    private OperationalState transformOperState(String operString) {
        State operState = org.opendaylight.transportpce.networkmodel.util.TopologyUtils.setNetworkOperState(operString);
        return operState.equals(State.InService) ? OperationalState.ENABLED : OperationalState.DISABLED;
    }

    private AdministrativeState transformAdminState(String adminString) {
        AdminStates adminState = org.opendaylight.transportpce.networkmodel.util.TopologyUtils
            .setNetworkAdminState(adminString);
        return adminState.equals(AdminStates.InService) ? AdministrativeState.UNLOCKED : AdministrativeState.LOCKED;
    }

    private OwnedNodeEdgePoint createNep(Uuid nepUuid, String tpid, Map<NameKey, Name> nepNames,
                                         LayerProtocolName nepProtocol, LayerProtocolName nodeProtocol, boolean withSip,
                                         String keyword,
                                         List<SupportedIfCapability> supportedInterfaceCapability,
                                         OperationalState operState, AdministrativeState adminState) {
        OwnedNodeEdgePointBuilder onepBldr = new OwnedNodeEdgePointBuilder()
                .setUuid(nepUuid)
                .setLayerProtocolName(nepProtocol)
                .setName(nepNames);
        if (withSip) {
            onepBldr.setMappedServiceInterfacePoint(createMSIP(1, nepProtocol, tpid, keyword,
                    supportedInterfaceCapability, operState, adminState));
        }
        LOG.debug("Node layer {}", nodeProtocol.getName());
        onepBldr.setSupportedCepLayerProtocolQualifier(createSupportedLayerProtocolQualifier(
                supportedInterfaceCapability, nepProtocol));
        onepBldr.setLinkPortDirection(PortDirection.BIDIRECTIONAL).setLinkPortRole(PortRole.SYMMETRIC)
                .setAdministrativeState(adminState).setOperationalState(operState)
                .setLifecycleState(LifecycleState.INSTALLED).setTerminationDirection(TerminationDirection.BIDIRECTIONAL)
                .setTerminationState(TerminationState.TERMINATEDBIDIRECTIONAL);
        return onepBldr.build();
    }

    private Map<OwnedNodeEdgePointKey, OwnedNodeEdgePoint> createRoadmNeps(String orNodeId, String tpId,
                                                                           boolean withSip, OperationalState operState,
                                                                           AdministrativeState adminState) {
        Map<OwnedNodeEdgePointKey, OwnedNodeEdgePoint> onepMap = new HashMap<>();
        // PHOTONIC MEDIA nep
        Uuid nepUuid = new Uuid(UUID.nameUUIDFromBytes((String.join("+", orNodeId,
                TapiStringConstants.PHTNC_MEDIA, tpId)).getBytes(StandardCharsets.UTF_8)).toString());
        Name nepName = new NameBuilder()
                .setValueName(TapiStringConstants.PHTNC_MEDIA + "NodeEdgePoint")
                .setValue(String.join("+", orNodeId, TapiStringConstants.PHTNC_MEDIA, tpId))
                .build();
        OwnedNodeEdgePoint onep = new OwnedNodeEdgePointBuilder()
            .setUuid(nepUuid)
            .setLayerProtocolName(LayerProtocolName.PHOTONICMEDIA)
            .setName(Map.of(nepName.key(), nepName))
            .setSupportedCepLayerProtocolQualifier(Set.of(PHOTONICLAYERQUALIFIEROMS.VALUE))
            .setLinkPortDirection(PortDirection.BIDIRECTIONAL).setLinkPortRole(PortRole.SYMMETRIC)
            .setAdministrativeState(adminState).setOperationalState(operState)
            .setLifecycleState(LifecycleState.INSTALLED).setTerminationDirection(TerminationDirection.BIDIRECTIONAL)
            .setTerminationState(TerminationState.TERMINATEDBIDIRECTIONAL)
            .build();
        onepMap.put(onep.key(), onep);

        // MC nep
        Uuid nepUuid1 = new Uuid(UUID.nameUUIDFromBytes((String.join("+", orNodeId,
                TapiStringConstants.MC, tpId)).getBytes(StandardCharsets.UTF_8)).toString());
        Name nepName1 = new NameBuilder()
                .setValueName(TapiStringConstants.MC + "NodeEdgePoint")
                .setValue(String.join("+", orNodeId, TapiStringConstants.MC, tpId))
                .build();
        OwnedNodeEdgePointBuilder onepBldr1 = new OwnedNodeEdgePointBuilder()
                .setUuid(nepUuid1)
                .setLayerProtocolName(LayerProtocolName.PHOTONICMEDIA)
                .setName(Map.of(nepName1.key(), nepName1))
                .setSupportedCepLayerProtocolQualifier(Set.of(PHOTONICLAYERQUALIFIEROMS.VALUE))
                .setLinkPortDirection(PortDirection.BIDIRECTIONAL).setLinkPortRole(PortRole.SYMMETRIC)
                .setAdministrativeState(adminState).setOperationalState(operState)
                .setLifecycleState(LifecycleState.INSTALLED).setTerminationDirection(TerminationDirection.BIDIRECTIONAL)
                .setTerminationState(TerminationState.TERMINATEDBIDIRECTIONAL);
        if (withSip) {
            onepBldr1.setMappedServiceInterfacePoint(createMSIP(1, LayerProtocolName.PHOTONICMEDIA,
                tpId, String.join("+", orNodeId, TapiStringConstants.MC), null,
                operState, adminState));
        }
        OwnedNodeEdgePoint onep1 = onepBldr1.build();
        onepMap.put(onep1.key(), onep1);

        // OTSiMC nep
        Uuid nepUuid2 = new Uuid(UUID.nameUUIDFromBytes((String.join("+", orNodeId, TapiStringConstants.OTSI_MC,
                tpId)).getBytes(StandardCharsets.UTF_8)).toString());
        Name nepName2 = new NameBuilder()
                .setValueName(TapiStringConstants.OTSI_MC + "NodeEdgePoint")
                .setValue(String.join("+", orNodeId, TapiStringConstants.OTSI_MC, tpId))
                .build();

        OwnedNodeEdgePoint onep2 = new OwnedNodeEdgePointBuilder()
            .setUuid(nepUuid2)
            .setLayerProtocolName(LayerProtocolName.PHOTONICMEDIA)
            .setName(Map.of(nepName2.key(), nepName2))
            .setSupportedCepLayerProtocolQualifier(Set.of(PHOTONICLAYERQUALIFIEROMS.VALUE))
            .setLinkPortDirection(PortDirection.BIDIRECTIONAL).setLinkPortRole(PortRole.SYMMETRIC)
            .setAdministrativeState(adminState).setOperationalState(operState)
            .setLifecycleState(LifecycleState.INSTALLED).setTerminationDirection(TerminationDirection.BIDIRECTIONAL)
            .setTerminationState(TerminationState.TERMINATEDBIDIRECTIONAL)
            .build();
        onepMap.put(onep2.key(), onep2);
        return onepMap;
    }

    private Map<MappedServiceInterfacePointKey, MappedServiceInterfacePoint>
            createMSIP(int nb, LayerProtocolName layerProtocol, String tpid, String nodeid,
                   List<SupportedIfCapability> supportedInterfaceCapability,
                   OperationalState operState, AdministrativeState adminState) {
        Map<MappedServiceInterfacePointKey, MappedServiceInterfacePoint> msipl = new HashMap<>();
        for (int i = 0; i < nb; i++) {
            Uuid sipUuid = new Uuid(UUID.nameUUIDFromBytes((String.join("+", "SIP", nodeid,
                    tpid)).getBytes(StandardCharsets.UTF_8)).toString());
            MappedServiceInterfacePoint msip = new MappedServiceInterfacePointBuilder()
                    .setServiceInterfacePointUuid(sipUuid).build();
            ServiceInterfacePoint sip = createSIP(sipUuid, layerProtocol, tpid, nodeid, supportedInterfaceCapability,
                operState, adminState);
            this.sipMap.put(sip.key(), sip);
            LOG.info("SIP created {}", sip.getUuid());
            // this.tapiSips.put(sip.key(), sip);
            msipl.put(msip.key(), msip);
        }
        return msipl;
    }

    private ServiceInterfacePoint createSIP(Uuid sipUuid, LayerProtocolName layerProtocol, String tpid, String nodeid,
                                            List<SupportedIfCapability> supportedInterfaceCapability,
                                            OperationalState operState, AdministrativeState adminState) {
        // TODO: what value should be set in total capacity and available capacity
        LOG.info("SIP name = {}", String.join("+", nodeid, tpid));
        Name sipName = new NameBuilder()
                .setValueName("SIP name")
                .setValue(String.join("+", nodeid, tpid))
                .build();
        return new ServiceInterfacePointBuilder()
                .setUuid(sipUuid)
                .setName(Map.of(sipName.key(), sipName))
                .setLayerProtocolName(layerProtocol)
                .setAdministrativeState(adminState)
                .setOperationalState(operState)
                .setLifecycleState(LifecycleState.INSTALLED)
                .setAvailableCapacity(new AvailableCapacityBuilder().build())
                .setTotalPotentialCapacity(new TotalPotentialCapacityBuilder().build())
                .setSupportedLayerProtocolQualifier(createSupportedLayerProtocolQualifier(supportedInterfaceCapability,
                        layerProtocol))
                .build();
    }

    private Node createRoadmTapiNode(String orNodeId, Map<OwnedNodeEdgePointKey, OwnedNodeEdgePoint> oneplist) {
        // UUID
        Uuid nodeUuid = new Uuid(UUID.nameUUIDFromBytes((String.join("+", orNodeId,
            TapiStringConstants.PHTNC_MEDIA)).getBytes(StandardCharsets.UTF_8)).toString());
        // Names
        Name nodeNames =  new NameBuilder().setValueName("roadm node name")
            .setValue(String.join("+", orNodeId, TapiStringConstants.PHTNC_MEDIA)).build();
        Name nameNodeType = new NameBuilder().setValueName("Node Type")
            .setValue(OpenroadmNodeType.ROADM.getName()).build();
        // Protocol Layer
        Set<LayerProtocolName> layerProtocols = Set.of(LayerProtocolName.PHOTONICMEDIA);
        // Empty random creation of mandatory fields for avoiding errors....
        CostCharacteristic costCharacteristic = new CostCharacteristicBuilder()
            .setCostAlgorithm("Restricted Shortest Path - RSP")
            .setCostName("HOP_COUNT")
            .setCostValue(TapiStringConstants.COST_HOP_VALUE)
            .build();
        LatencyCharacteristic latencyCharacteristic = new LatencyCharacteristicBuilder()
            .setFixedLatencyCharacteristic(TapiStringConstants.COST_HOP_VALUE)
            .setQueingLatencyCharacteristic(TapiStringConstants.QUEING_LATENCY_VALUE)
            .setJitterCharacteristic(TapiStringConstants.JITTER_VALUE)
            .setWanderCharacteristic(TapiStringConstants.WANDER_VALUE)
            .setTrafficPropertyName("FIXED_LATENCY")
            .build();
        return new NodeBuilder()
            .setUuid(nodeUuid)
            .setName(Map.of(nodeNames.key(), nodeNames, nameNodeType.key(), nameNodeType))
            .setLayerProtocolName(layerProtocols)
            .setAdministrativeState(AdministrativeState.UNLOCKED)
            .setOperationalState(OperationalState.ENABLED)
            .setLifecycleState(LifecycleState.INSTALLED)
            .setOwnedNodeEdgePoint(oneplist)
            .setNodeRuleGroup(createNodeRuleGroupForRdmNode(orNodeId, nodeUuid, oneplist.values()))
            .setCostCharacteristic(Map.of(costCharacteristic.key(), costCharacteristic))
            .setLatencyCharacteristic(Map.of(latencyCharacteristic.key(), latencyCharacteristic))
            .setErrorCharacteristic("error")
            .setLossCharacteristic("loss")
            .setRepeatDeliveryCharacteristic("repeat delivery")
            .setDeliveryOrderCharacteristic("delivery order")
            .setUnavailableTimeCharacteristic("unavailable time")
            .setServerIntegrityProcessCharacteristic("server integrity process")
            .build();
    }

    private Map<NodeRuleGroupKey, NodeRuleGroup> createNodeRuleGroupForRdmNode(String orNodeId, Uuid nodeUuid,
                                                                               Collection<OwnedNodeEdgePoint> onepl) {
        Map<NodeEdgePointKey, NodeEdgePoint>
                nepMap = new HashMap<>();
        for (OwnedNodeEdgePoint onep : onepl) {
            NodeEdgePoint nep = new NodeEdgePointBuilder()
                .setTopologyUuid(this.tapiTopoUuid)
                .setNodeUuid(nodeUuid)
                .setNodeEdgePointUuid(onep.key().getUuid())
                .build();
            nepMap.put(nep.key(), nep);
        }
        Map<NodeRuleGroupKey, NodeRuleGroup> nodeRuleGroupMap = new HashMap<>();
        Map<RuleKey, Rule> ruleList = new HashMap<>();
        Rule rule = new RuleBuilder()
                .setLocalId("forward")
                .setForwardingRule(ForwardingRule.MAYFORWARDACROSSGROUP)
                .setRuleType(RuleType.FORWARDING)
                .build();
        ruleList.put(rule.key(), rule);
        NodeRuleGroup nodeRuleGroup = new NodeRuleGroupBuilder()
                .setUuid(new Uuid(UUID.nameUUIDFromBytes((orNodeId + " node rule group")
                        .getBytes(StandardCharsets.UTF_8)).toString()))
                .setRule(ruleList)
                .setNodeEdgePoint(nepMap)
                .build();
        nodeRuleGroupMap.put(nodeRuleGroup.key(), nodeRuleGroup);
        return nodeRuleGroupMap;
    }

    private Map<LinkKey, Link> createTapiTransitionalLinks(String nodeId, List<Mapping> xpdrNetMaps) {
        Map<LinkKey, Link> linkMap = new HashMap<>();
        for (Mapping mapping : xpdrNetMaps) {
            Link transiLink = tapiLink.createTapiLink(nodeId, mapping.getLogicalConnectionPoint(), nodeId,
                mapping.getLogicalConnectionPoint(), TapiStringConstants.TRANSITIONAL_LINK, TapiStringConstants.DSR,
                TapiStringConstants.OTSI, TapiStringConstants.I_ODU, TapiStringConstants.I_OTSI,
                "inService", "inService", Set.of(LayerProtocolName.ODU, LayerProtocolName.PHOTONICMEDIA),
                Set.of(LayerProtocolName.ODU.getName(), LayerProtocolName.PHOTONICMEDIA.getName()),
                this.tapiTopoUuid);
            linkMap.put(transiLink.key(), transiLink);
        }
        // return a map of links and then we can do merge the corresponding link map into the topology context
        return linkMap;
    }

    private OduSwitchingPools createTpdrSwitchPool(List<Mapping> xpdrNetMaps) {
        Map<NonBlockingListKey, NonBlockingList> nblMap = new HashMap<>();
        int count = 1;
        for (int i = 1; i <= xpdrNetMaps.size(); i++) {
            LOG.info("XPDr net LCP = {}", xpdrNetMaps.get(i - 1).getLogicalConnectionPoint());
            LOG.info("XPDr net associated LCP = {}", xpdrNetMaps.get(i - 1).getConnectionMapLcp());
            TpId tpid1 = new TpId(xpdrNetMaps.get(i - 1).getLogicalConnectionPoint());
            TpId tpid2 = new TpId(xpdrNetMaps.get(i - 1).getConnectionMapLcp());
            Set<TpId> tpList = new HashSet<>();
            tpList.add(tpid1);
            tpList.add(tpid2);
            NonBlockingList nbl = new NonBlockingListBuilder()
                .setNblNumber(Uint16.valueOf(count))
                .setTpList(tpList)
                .build();
            nblMap.put(nbl.key(), nbl);
            count++;
        }
        return new OduSwitchingPoolsBuilder()
            .setNonBlockingList(nblMap)
            .setSwitchingPoolNumber(Uint16.valueOf(1))
            .build();
    }

    private OduSwitchingPools createSwtchSwitchPool(List<Mapping> xpdrClMaps, List<Mapping> xpdrNetMaps,
                                                    Integer xpdrNb) {
        Set<TpId> tpl = new HashSet<>();
        TpId tpId = null;
        for (int i = 1; i <= xpdrClMaps.size(); i++) {
            tpId = new TpId("XPDR" + xpdrNb + TapiStringConstants.CLIENT + i);
            tpl.add(tpId);
        }
        for (int i = 1; i <= xpdrNetMaps.size(); i++) {
            tpId = new TpId("XPDR" + xpdrNb + TapiStringConstants.NETWORK + i);
            tpl.add(tpId);
        }
        Map<NonBlockingListKey, NonBlockingList> nbMap = new HashMap<>();
        NonBlockingList nbl = new NonBlockingListBuilder()
            .setNblNumber(Uint16.valueOf(1))
            .setTpList(tpl)
            .build();
        nbMap.put(nbl.key(),nbl);

        return new OduSwitchingPoolsBuilder()
            .setSwitchingPoolNumber(Uint16.valueOf(1))
            .setSwitchingPoolType(SwitchingPoolTypes.NonBlocking)
            .setNonBlockingList(nbMap)
            .build();
    }

    private OduSwitchingPools createMuxSwitchPool(List<Mapping> xpdrClMaps, List<Mapping> xpdrNetMaps, Integer xpdrNb) {
        Map<NonBlockingListKey, NonBlockingList> nbMap = new HashMap<>();
        for (int i = 1; i <= xpdrClMaps.size(); i++) {
            Set<TpId> tpList = new HashSet<>();
            TpId tpId = new TpId("XPDR" + xpdrNb + TapiStringConstants.CLIENT + i);
            tpList.add(tpId);
            tpId = new TpId("XPDR" + xpdrNb + "-NETWORK1");
            tpList.add(tpId);
            NonBlockingList nbl = new NonBlockingListBuilder()
                .setNblNumber(Uint16.valueOf(i))
                .setTpList(tpList)
                .setAvailableInterconnectBandwidth(Uint32.valueOf(xpdrNetMaps.size() * 10L))
                .setInterconnectBandwidthUnit(Uint32.valueOf(1000000000))
                .build();
            nbMap.put(nbl.key(),nbl);
        }
        return new OduSwitchingPoolsBuilder()
                .setSwitchingPoolNumber(Uint16.valueOf(1))
                .setSwitchingPoolType(SwitchingPoolTypes.NonBlocking)
                .setNonBlockingList(nbMap)
                .build();
    }

    private Map<NodeRuleGroupKey, NodeRuleGroup> createNodeRuleGroupForOtsiNode(String nodeId,
                                                                                List<Mapping> xpdrNetMaps,
                                                                                Map<RuleKey, Rule> ruleList) {
        Map<NodeRuleGroupKey, NodeRuleGroup> nodeRuleGroupMap = new HashMap<>();
        // create NodeRuleGroup
        int count = 1;
        for (Mapping tpMapping : xpdrNetMaps) {
            Map<NodeEdgePointKey, NodeEdgePoint> nepList = new HashMap<>();
            NodeEdgePoint inep = new NodeEdgePointBuilder()
                .setTopologyUuid(this.tapiTopoUuid)
                .setNodeUuid(new Uuid(UUID.nameUUIDFromBytes((String.join("+", nodeId,
                        TapiStringConstants.OTSI)).getBytes(StandardCharsets.UTF_8)).toString()))
                .setNodeEdgePointUuid(new Uuid(UUID.nameUUIDFromBytes((String.join("+", nodeId,
                    TapiStringConstants.I_OTSI, tpMapping.getLogicalConnectionPoint()))
                    .getBytes(StandardCharsets.UTF_8)).toString()))
                .build();
            NodeEdgePoint enep = new NodeEdgePointBuilder()
                .setTopologyUuid(this.tapiTopoUuid)
                .setNodeUuid(new Uuid(UUID.nameUUIDFromBytes((String.join("+", nodeId,
                    TapiStringConstants.OTSI)).getBytes(StandardCharsets.UTF_8)).toString()))
                .setNodeEdgePointUuid(new Uuid(UUID.nameUUIDFromBytes(
                    (String.join("+", nodeId, TapiStringConstants.E_OTSI,
                        tpMapping.getLogicalConnectionPoint())).getBytes(StandardCharsets.UTF_8)).toString()))
                .build();
            nepList.put(inep.key(), inep);
            nepList.put(enep.key(), enep);
            // Empty random creation of mandatory fields for avoiding errors....
            CostCharacteristic costCharacteristic = new CostCharacteristicBuilder()
                .setCostAlgorithm("Restricted Shortest Path - RSP")
                .setCostName("HOP_COUNT")
                .setCostValue(TapiStringConstants.COST_HOP_VALUE)
                .build();
            LatencyCharacteristic latencyCharacteristic = new LatencyCharacteristicBuilder()
                .setFixedLatencyCharacteristic(TapiStringConstants.FIXED_LATENCY_VALUE)
                .setQueingLatencyCharacteristic(TapiStringConstants.QUEING_LATENCY_VALUE)
                .setJitterCharacteristic(TapiStringConstants.JITTER_VALUE)
                .setWanderCharacteristic(TapiStringConstants.WANDER_VALUE)
                .setTrafficPropertyName("FIXED_LATENCY")
                .build();
            RiskCharacteristic riskCharacteristic = new RiskCharacteristicBuilder()
                .setRiskCharacteristicName("risk characteristic")
                .setRiskIdentifierList(Set.of("risk identifier1", "risk identifier2"))
                .build();
            NodeRuleGroup nodeRuleGroup = new NodeRuleGroupBuilder()
                .setUuid(new Uuid(
                    UUID.nameUUIDFromBytes(("otsi node rule group " + count).getBytes(StandardCharsets.UTF_8))
                        .toString()))
                .setRule(ruleList)
                .setNodeEdgePoint(nepList)
                .setRiskCharacteristic(Map.of(riskCharacteristic.key(), riskCharacteristic))
                .setCostCharacteristic(Map.of(costCharacteristic.key(), costCharacteristic))
                .setLatencyCharacteristic(Map.of(latencyCharacteristic.key(), latencyCharacteristic))
                .build();
            nodeRuleGroupMap.put(nodeRuleGroup.key(), nodeRuleGroup);
            count++;
        }
        return nodeRuleGroupMap;
    }

    private Map<NodeRuleGroupKey, NodeRuleGroup> createNodeRuleGroupForDsrNode(String nodeId,
                                                                               OduSwitchingPools oorOduSwitchingPool,
                                                                               Map<RuleKey, Rule> ruleList,
                                                                               Map<OwnedNodeEdgePointKey,
                                                                                       OwnedNodeEdgePoint> onepl) {
        // create NodeRuleGroup
        if (oorOduSwitchingPool == null) {
            LOG.info("No switching pool created for node = {}", nodeId);
            return new HashMap<>();
        }
        LOG.info("ONEPL = {}", onepl.values());
        Map<NodeRuleGroupKey, NodeRuleGroup> nodeRuleGroupMap = new HashMap<>();
        int count = 1;
        for (NonBlockingList nbl : oorOduSwitchingPool.nonnullNonBlockingList().values()) {
            LOG.info("Non blocking list = {}", nbl);
            Map<NodeEdgePointKey, NodeEdgePoint> nepList = new HashMap<>();
            for (TpId tp : nbl.getTpList()) {
                LOG.info("EDOU TP = {}", String.join("+", nodeId, TapiStringConstants.E_ODU, tp.getValue()));
                LOG.info("DSR TP = {}", String.join("+", nodeId, TapiStringConstants.DSR, tp.getValue()));
                Uuid tpUuid = new Uuid(UUID.nameUUIDFromBytes((String.join("+", nodeId,
                    TapiStringConstants.E_ODU, tp.getValue())).getBytes(StandardCharsets.UTF_8)).toString());
                Uuid tp1Uuid = new Uuid(UUID.nameUUIDFromBytes((String.join("+", nodeId,
                    TapiStringConstants.DSR, tp.getValue())).getBytes(StandardCharsets.UTF_8)).toString());
                if (onepl.containsKey(new OwnedNodeEdgePointKey(tpUuid))
                        && onepl.containsKey(new OwnedNodeEdgePointKey(tp1Uuid))) {
                    NodeEdgePoint nep1 = new NodeEdgePointBuilder()
                        .setTopologyUuid(this.tapiTopoUuid)
                        .setNodeUuid(new Uuid(UUID.nameUUIDFromBytes(
                            (String.join("+", nodeId,TapiStringConstants. DSR))
                                .getBytes(StandardCharsets.UTF_8)).toString()))
                        .setNodeEdgePointUuid(tp1Uuid)
                        .build();
                    NodeEdgePoint nep2 = new NodeEdgePointBuilder()
                        .setTopologyUuid(this.tapiTopoUuid)
                        .setNodeUuid(new Uuid(UUID.nameUUIDFromBytes(
                            (String.join("+", nodeId,TapiStringConstants. DSR))
                                .getBytes(StandardCharsets.UTF_8)).toString()))
                        .setNodeEdgePointUuid(tpUuid)
                        .build();
                    nepList.put(nep1.key(), nep1);
                    nepList.put(nep2.key(), nep2);
                }
            }
            // Empty random creation of mandatory fields for avoiding errors....
            CostCharacteristic costCharacteristic = new CostCharacteristicBuilder()
                .setCostAlgorithm("Restricted Shortest Path - RSP")
                .setCostName("HOP_COUNT")
                .setCostValue(TapiStringConstants.COST_HOP_VALUE)
                .build();
            LatencyCharacteristic latencyCharacteristic = new LatencyCharacteristicBuilder()
                .setFixedLatencyCharacteristic(TapiStringConstants.FIXED_LATENCY_VALUE)
                .setQueingLatencyCharacteristic(TapiStringConstants.QUEING_LATENCY_VALUE)
                .setJitterCharacteristic(TapiStringConstants.JITTER_VALUE)
                .setWanderCharacteristic(TapiStringConstants.WANDER_VALUE)
                .setTrafficPropertyName("FIXED_LATENCY")
                .build();
            RiskCharacteristic riskCharacteristic = new RiskCharacteristicBuilder()
                .setRiskCharacteristicName("risk characteristic")
                .setRiskIdentifierList(Set.of("risk identifier1", "risk identifier2"))
                .build();
            NodeRuleGroup nodeRuleGroup = new NodeRuleGroupBuilder()
                .setUuid(new Uuid(
                    UUID.nameUUIDFromBytes(("dsr node rule group " + count).getBytes(StandardCharsets.UTF_8))
                        .toString()))
                .setRule(ruleList)
                .setNodeEdgePoint(nepList)
                .setRiskCharacteristic(Map.of(riskCharacteristic.key(), riskCharacteristic))
                .setCostCharacteristic(Map.of(costCharacteristic.key(), costCharacteristic))
                .setLatencyCharacteristic(Map.of(latencyCharacteristic.key(), latencyCharacteristic))
                .build();
            nodeRuleGroupMap.put(nodeRuleGroup.key(), nodeRuleGroup);
            count++;
        }
        return nodeRuleGroupMap;
    }

    private Set<LAYERPROTOCOLQUALIFIER> createSupportedLayerProtocolQualifier(
            List<SupportedIfCapability> sicList, LayerProtocolName lpn) {
        if (sicList == null) {
            return Set.of(PHOTONICLAYERQUALIFIEROMS.VALUE);
        }
        Map<SupportedInterfaceCapabilityKey, SupportedInterfaceCapability> supIfMap = new HashMap<>();
        LOG.info("SIC list = {}", sicList);
        for (SupportedIfCapability supInterCapa : sicList) {
            SupportedInterfaceCapability supIfCapa = new SupportedInterfaceCapabilityBuilder()
                    .withKey(new SupportedInterfaceCapabilityKey(convertSupIfCapa(supInterCapa.toString())))
                    .setIfCapType(convertSupIfCapa(supInterCapa.toString()))
                    .build();
            supIfMap.put(supIfCapa.key(), supIfCapa);
        }
        Set<LAYERPROTOCOLQUALIFIER> sclpqList = new HashSet<>();
        for (SupportedInterfaceCapability sic : supIfMap.values()) {
            String ifCapType = sic.getIfCapType().toString().split("\\{")[0];
            switch (lpn.getName()) {
                case "DSR":
                    switch (ifCapType) {
                        // TODO: it may be needed to add more cases clauses if the interface capabilities of a
                        //  port are extended in the config file
                        case "If1GEODU0":
                            sclpqList.add(ODUTYPEODU0.VALUE);
                            sclpqList.add(DIGITALSIGNALTYPEGigE.VALUE);
                            break;
                        case "If10GEODU2e":
                            sclpqList.add(ODUTYPEODU2E.VALUE);
                            sclpqList.add(DIGITALSIGNALTYPE10GigELAN.VALUE);
                            break;
                        case "If10GEODU2":
                            sclpqList.add(ODUTYPEODU2.VALUE);
                            sclpqList.add(DIGITALSIGNALTYPE10GigELAN.VALUE);
                            break;
                        case "If10GE":
                            sclpqList.add(DIGITALSIGNALTYPE10GigELAN.VALUE);
                            break;
                        case "If100GEODU4":
                            sclpqList.add(DIGITALSIGNALTYPE100GigE.VALUE);
                            sclpqList.add(ODUTYPEODU4.VALUE);
                            break;
                        case "If100GE":
                            sclpqList.add(DIGITALSIGNALTYPE100GigE.VALUE);
                            break;
                        case "IfOCHOTU4ODU4":
                        case "IfOCH":
                            sclpqList.add(ODUTYPEODU4.VALUE);
                            break;
                        default:
                            LOG.error("IfCapability type not managed");
                            break;
                    }
                    break;
                case "ODU":
                    switch (ifCapType) {
                        // TODO: it may be needed to add more cases clauses if the interface capabilities of a
                        //  port are extended in the config file
                        case "If1GEODU0":
                            sclpqList.add(ODUTYPEODU0.VALUE);
                            break;
                        case "If10GEODU2e":
                            sclpqList.add(ODUTYPEODU2E.VALUE);
                            break;
                        case "If10GEODU2":
                        case "If10GE":
                            sclpqList.add(ODUTYPEODU2.VALUE);
                            break;
                        case "If100GEODU4":
                        case "If100GE":
                        case "IfOCHOTU4ODU4":
                        case "IfOCH":
                            sclpqList.add(ODUTYPEODU4.VALUE);
                            break;
                        default:
                            LOG.error("IfCapability type not managed");
                            break;
                    }
                    break;
                case "PHOTONIC_MEDIA":
                    if (ifCapType.equals("IfOCHOTU4ODU4") || ifCapType.equals("IfOCH")) {
                        sclpqList.add(PHOTONICLAYERQUALIFIEROTSi.VALUE);
                        sclpqList.add(PHOTONICLAYERQUALIFIEROMS.VALUE);
                    }
                    break;
                default:
                    LOG.error("Layer Protocol Name is unknown {}", lpn.getName());
                    break;
            }
        }
        return sclpqList;
    }

    private String getNodeType(XpdrNodeTypes xponderType) {
        switch (xponderType.getIntValue()) {
            case 1:
                return OpenroadmNodeType.TPDR.getName();
            case 2:
                return OpenroadmNodeType.MUXPDR.getName();
            case 3:
                return OpenroadmNodeType.SWITCH.getName();
            default:
                LOG.info("XpdrType {} not supported", xponderType);
                break;
        }
        return null;
    }

    private void mergeNodeinTopology(Map<NodeKey, Node> nodeMap) {
        // TODO is this merge correct? Should we just merge topology by changing the nodes map??
        // TODO: verify this is correct. Should we identify the context IID with the context UUID??
        LOG.info("Creating tapi node in TAPI topology context");
        InstanceIdentifier<Topology> topoIID = InstanceIdentifier.builder(Context.class)
            .augmentation(Context1.class).child(TopologyContext.class)
            .child(Topology.class, new TopologyKey(this.tapiTopoUuid))
            .build();

        Topology topology = new TopologyBuilder().setUuid(this.tapiTopoUuid).setNode(nodeMap).build();

        // merge in datastore
        this.networkTransactionService.merge(LogicalDatastoreType.OPERATIONAL, topoIID,
                topology);
        try {
            this.networkTransactionService.commit().get();
        } catch (InterruptedException | ExecutionException e) {
            LOG.error("Error populating TAPI topology: ", e);
        }
        LOG.info("Node added succesfully.");
    }

    private void mergeLinkinTopology(Map<LinkKey, Link> linkMap) {
        // TODO is this merge correct? Should we just merge topology by changing the nodes map??
        // TODO: verify this is correct. Should we identify the context IID with the context UUID??
        LOG.info("Creating tapi node in TAPI topology context");
        InstanceIdentifier<Topology> topoIID = InstanceIdentifier.builder(Context.class)
            .augmentation(Context1.class).child(TopologyContext.class)
            .child(Topology.class, new TopologyKey(this.tapiTopoUuid))
            .build();

        Topology topology = new TopologyBuilder().setUuid(this.tapiTopoUuid).setLink(linkMap).build();

        // merge in datastore
        this.networkTransactionService.merge(LogicalDatastoreType.OPERATIONAL, topoIID,
                topology);
        try {
            this.networkTransactionService.commit().get();
        } catch (InterruptedException | ExecutionException e) {
            LOG.error("Error populating TAPI topology: ", e);
        }
        LOG.info("Roadm Link added succesfully.");
    }

    private void mergeSipsinContext(Map<ServiceInterfacePointKey, ServiceInterfacePoint> sips) {
        // TODO is this merge correct? Should we just merge topology by changing the nodes map??
        // TODO: verify this is correct. Should we identify the context IID with the context UUID??
        try {
            ContextBuilder contextBuilder = new ContextBuilder();
            contextBuilder.setServiceInterfacePoint(sips);
            InstanceIdentifier<Context> contextIID = InstanceIdentifier.builder(Context.class).build();
            // merge in datastore
            this.networkTransactionService.merge(LogicalDatastoreType.OPERATIONAL, contextIID,
                    contextBuilder.build());
            this.networkTransactionService.commit().get();
            LOG.info("TAPI SIPs merged successfully.");
        } catch (InterruptedException | ExecutionException e) {
            LOG.error("Failed to merge TAPI Sips", e);
        }
    }

    private void deleteLinkFromTopo(Uuid linkUuid) {
        // TODO: check if this IID is correct
        try {
            InstanceIdentifier<Link> linkIID = InstanceIdentifier.builder(Context.class)
                .augmentation(Context1.class).child(TopologyContext.class).child(Topology.class,
                    new TopologyKey(this.tapiTopoUuid)).child(Link.class, new LinkKey(linkUuid)).build();
            this.networkTransactionService.delete(LogicalDatastoreType.OPERATIONAL, linkIID);
            this.networkTransactionService.commit().get();
            LOG.info("TAPI link deleted successfully.");
        } catch (InterruptedException | ExecutionException e) {
            LOG.error("Failed to delete TAPI link", e);
        }
    }

    private void deleteNodeFromTopo(Uuid nodeUuid) {
        // TODO: check if this IID is correct
        try {
            InstanceIdentifier<Node> nodeIDD = InstanceIdentifier.builder(Context.class)
                .augmentation(Context1.class).child(TopologyContext.class).child(Topology.class,
                    new TopologyKey(this.tapiTopoUuid)).child(Node.class, new NodeKey(nodeUuid)).build();
            this.networkTransactionService.delete(LogicalDatastoreType.OPERATIONAL, nodeIDD);
            this.networkTransactionService.commit().get();
            LOG.info("TAPI Node deleted successfully.");
        } catch (InterruptedException | ExecutionException e) {
            LOG.error("Failed to delete TAPI Node", e);
        }
    }

    private void deleteSipFromTopo(Uuid sipUuid) {
        // TODO: check if this IID is correct
        try {
            InstanceIdentifier<ServiceInterfacePoint> sipIID = InstanceIdentifier.builder(Context.class)
                    .child(ServiceInterfacePoint.class, new ServiceInterfacePointKey(sipUuid)).build();
            this.networkTransactionService.delete(LogicalDatastoreType.OPERATIONAL, sipIID);
            this.networkTransactionService.commit().get();
            LOG.info("TAPI SIP deleted successfully.");
        } catch (InterruptedException | ExecutionException e) {
            LOG.error("Failed to delete TAPI SIP", e);
        }
    }

    private void updateConnectivityServicesState(Uuid sipUuid, String nodeId) {
        // TODO: check if this IID is correct
        InstanceIdentifier<ConnectivityContext> connectivitycontextIID = InstanceIdentifier.builder(Context.class)
            .augmentation(org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev181210.Context1.class)
            .child(ConnectivityContext.class)
            .build();
        ConnectivityContext connContext = null;
        try {
            Optional<ConnectivityContext> optConnContext =
                    this.networkTransactionService.read(LogicalDatastoreType.OPERATIONAL, connectivitycontextIID)
                            .get();
            if (!optConnContext.isPresent()) {
                LOG.error("Couldnt retrieve connectivity context from datastore");
                return;
            }
            connContext = optConnContext.orElseThrow();
        } catch (InterruptedException | ExecutionException e) {
            LOG.error("Couldnt read connectivity context from datastore", e);
        }
        if (connContext == null) {
            LOG.error("Connectivity context is empty");
            return;
        }
        // Loop through services, check if the endpoint uuid is equal to the sip.
        // If so update state.
        Map<ConnectivityServiceKey, ConnectivityService> connServMap = connContext.getConnectivityService();
        Map<ConnectionKey, Connection> connMap = connContext.getConnection();
        if (connServMap != null) {
            for (ConnectivityService service:connServMap.values()) {
                Map<EndPointKey, EndPoint> serviceEndPoints = service.getEndPoint();
                if (serviceEndPoints.values().stream().anyMatch(endPoint -> endPoint.getServiceInterfacePoint()
                    .getServiceInterfacePointUuid().equals(sipUuid))) {
                    LOG.info("Service using SIP of node {} identified. Update state of service", nodeId);
                    ConnectivityService updService = new ConnectivityServiceBuilder(service)
                        .setAdministrativeState(AdministrativeState.LOCKED)
                        .setOperationalState(OperationalState.DISABLED)
                        .setLifecycleState(LifecycleState.PENDINGREMOVAL)
                        .build();
                    updateConnectivityService(updService);
                }
            }
        }
        // Update state of connections
        if (connMap != null) {
            for (Connection connection:connMap.values()) {
                if (connection.getName().values().stream().anyMatch(name -> name.getValue().contains(nodeId))) {
                    Connection updConn = new ConnectionBuilder(connection)
                        .setLifecycleState(LifecycleState.PENDINGREMOVAL)
                        .setOperationalState(OperationalState.DISABLED)
                        .build();
                    updateConnection(updConn);
                }
            }
        }
    }

    private void updateConnection(Connection updConn) {
        // TODO: check if this IID is correct
        InstanceIdentifier<Connection> connectionIID = InstanceIdentifier.builder(Context.class)
                .augmentation(org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev181210.Context1.class)
                .child(ConnectivityContext.class).child(Connection.class,
                        new ConnectionKey(updConn.getUuid())).build();
        this.networkTransactionService.merge(LogicalDatastoreType.OPERATIONAL, connectionIID, updConn);
        try {
            this.networkTransactionService.commit().get();
        } catch (InterruptedException | ExecutionException e) {
            LOG.error("Error committing into datastore", e);
        }
    }

    private void updateConnectivityService(ConnectivityService updService) {
        // TODO: check if this IID is correct
        InstanceIdentifier<ConnectivityService> connectivityserviceIID = InstanceIdentifier.builder(Context.class)
                .augmentation(org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev181210.Context1.class)
                .child(ConnectivityContext.class).child(ConnectivityService.class,
                        new ConnectivityServiceKey(updService.getUuid())).build();
        this.networkTransactionService.merge(LogicalDatastoreType.OPERATIONAL, connectivityserviceIID, updService);
        try {
            this.networkTransactionService.commit().get();
        } catch (InterruptedException | ExecutionException e) {
            LOG.error("Error committing into datastore", e);
        }
    }

    private static org.opendaylight.yang.gen.v1.http.org.openroadm.port.types.rev201211.SupportedIfCapability
            convertSupIfCapa(String ifCapType) {
        ImmutableMap<String, org.opendaylight.yang.gen.v1.http.org.openroadm.port.types.rev201211.SupportedIfCapability>
                capTypeMap = ImmutableMap.<String,
                    org.opendaylight.yang.gen.v1.http.org.openroadm.port.types.rev201211.SupportedIfCapability>builder()
                .put("If400GE{qname=(http://org/openroadm/port/types?revision=2023-05-26)if-400GE}", If400GE.VALUE)
                .put("IfOTU4ODU4{qname=(http://org/openroadm/port/types?revision=2023-05-26)if-OTU4-ODU4}",
                        IfOTU4ODU4.VALUE)
                .put("IfOtsiOtsigroup{qname=(http://org/openroadm/port/types?revision=2023-05-26)if-otsi-otsigroup}",
                    IfOtsiOtsigroup.VALUE)
                .put("IfOCH{qname=(http://org/openroadm/port/types?revision=2023-05-26)if-OCH}", IfOCH.VALUE)
                .put("IfOCHOTU4ODU4{qname=(http://org/openroadm/port/types?revision=2023-05-26)if-OCH-OTU4-ODU4}",
                    IfOCHOTU4ODU4.VALUE)
                .put("If1GEODU0{qname=(http://org/openroadm/port/types?revision=2023-05-26)if-1GE-ODU0}",
                    If1GEODU0.VALUE)
                .put("If10GE{qname=(http://org/openroadm/port/types?revision=2023-05-26)if-10GE}", If10GE.VALUE)
                .put("If10GEODU2{qname=(http://org/openroadm/port/types?revision=2023-05-26)if-10GE-ODU2}",
                    If10GEODU2.VALUE)
                .put("If10GEODU2e{qname=(http://org/openroadm/port/types?revision=2023-05-26)if-10GE-ODU2e}",
                    If10GEODU2e.VALUE)
                .put("If100GE{qname=(http://org/openroadm/port/types?revision=2023-05-26)if-100GE}", If100GE.VALUE)
                .put("If100GEODU4{qname=(http://org/openroadm/port/types?revision=2023-05-26)if-100GE-ODU4}",
                    If100GEODU4.VALUE)
                .build();
        if (!capTypeMap.containsKey(ifCapType)) {
            LOG.error("supported-if-capability {} not supported", ifCapType);
            return null;
        }
        return capTypeMap.get(ifCapType);
    }

}
