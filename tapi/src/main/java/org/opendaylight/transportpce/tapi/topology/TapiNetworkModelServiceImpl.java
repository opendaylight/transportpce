/*
 * Copyright © 2021 Nokia, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.tapi.topology;

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
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;
import org.opendaylight.mdsal.binding.api.NotificationPublishService;
import org.opendaylight.mdsal.binding.api.ReadTransaction;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.transportpce.common.NetworkUtils;
import org.opendaylight.transportpce.common.Timeouts;
import org.opendaylight.transportpce.common.device.DeviceTransactionManager;
import org.opendaylight.transportpce.common.network.NetworkTransactionService;
import org.opendaylight.transportpce.tapi.R2RTapiLinkDiscovery;
import org.opendaylight.transportpce.tapi.TapiStringConstants;
import org.opendaylight.transportpce.tapi.utils.TapiLink;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev231221.mapping.Mapping;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev231221.network.Nodes;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.state.types.rev191129.State;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.types.rev181019.NodeTypes;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.types.rev191129.XpdrNodeTypes;
import org.opendaylight.yang.gen.v1.http.org.openroadm.equipment.states.types.rev191129.AdminStates;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.types.rev230526.xpdr.odu.switching.pools.OduSwitchingPools;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.types.rev230526.xpdr.odu.switching.pools.OduSwitchingPoolsBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.types.rev230526.xpdr.odu.switching.pools.odu.switching.pools.NonBlockingList;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.types.rev230526.xpdr.odu.switching.pools.odu.switching.pools.NonBlockingListBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.types.rev230526.xpdr.odu.switching.pools.odu.switching.pools.NonBlockingListKey;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.types.rev230526.OpenroadmNodeType;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.types.rev230526.xpdr.tp.supported.interfaces.SupportedInterfaceCapability;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.types.rev230526.xpdr.tp.supported.interfaces.SupportedInterfaceCapabilityBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.types.rev230526.xpdr.tp.supported.interfaces.SupportedInterfaceCapabilityKey;
import org.opendaylight.yang.gen.v1.http.org.openroadm.port.types.rev230526.SupportedIfCapability;
import org.opendaylight.yang.gen.v1.http.org.openroadm.switching.pool.types.rev191129.SwitchingPoolTypes;
import org.opendaylight.yang.gen.v1.http.org.openroadm.xponder.rev230526.xpdr.mode.attributes.supported.operational.modes.OperationalModeKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.NetworkId;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.Networks;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.NodeId;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.networks.Network;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.networks.NetworkKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.Node1;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.TpId;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.networks.network.node.TerminationPoint;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.AdministrativeState;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.Context;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.ContextBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.Direction;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.LayerProtocolName;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.LifecycleState;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.OperationalState;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.PortRole;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.Uuid;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.global._class.Name;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.global._class.NameBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.global._class.NameKey;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.tapi.context.ServiceInterfacePoint;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.tapi.context.ServiceInterfacePointBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.tapi.context.ServiceInterfacePointKey;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev221121.OwnedNodeEdgePoint1;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev221121.OwnedNodeEdgePoint1Builder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev221121.cep.list.ConnectionEndPoint;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev221121.cep.list.ConnectionEndPointBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev221121.cep.list.ConnectionEndPointKey;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev221121.connectivity.context.Connection;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev221121.connectivity.context.ConnectionBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev221121.connectivity.context.ConnectionKey;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev221121.connectivity.context.ConnectivityService;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev221121.connectivity.context.ConnectivityServiceBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev221121.connectivity.context.ConnectivityServiceKey;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev221121.connectivity.service.EndPoint;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev221121.connectivity.service.EndPointKey;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev221121.context.ConnectivityContext;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev221121.context.topology.context.topology.node.owned.node.edge.point.CepList;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev221121.context.topology.context.topology.node.owned.node.edge.point.CepListBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.notification.rev221121.NOTIFICATIONTYPEATTRIBUTEVALUECHANGE;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.notification.rev221121.NotificationBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.notification.rev221121.notification.ChangedAttributes;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.notification.rev221121.notification.ChangedAttributesBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.notification.rev221121.notification.ChangedAttributesKey;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.photonic.media.rev221121.PHOTONICLAYERQUALIFIEROMS;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.photonic.media.rev221121.PHOTONICLAYERQUALIFIEROTS;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.Context1;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.FORWARDINGRULEMAYFORWARDACROSSGROUP;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.NodeEdgePointRef;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.RuleType;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.TOPOLOGYOBJECTTYPENODEEDGEPOINT;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.context.TopologyContext;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.node.NodeRuleGroup;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.node.NodeRuleGroupBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.node.NodeRuleGroupKey;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.node.OwnedNodeEdgePoint;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.node.OwnedNodeEdgePointBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.node.OwnedNodeEdgePointKey;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.node.RiskParameterPac;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.node.RiskParameterPacBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.node.edge.point.MappedServiceInterfacePoint;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.node.edge.point.SupportedCepLayerProtocolQualifierInstances;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.node.edge.point.SupportedCepLayerProtocolQualifierInstancesBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.node.rule.group.NodeEdgePoint;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.node.rule.group.NodeEdgePointBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.node.rule.group.NodeEdgePointKey;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.node.rule.group.Rule;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.node.rule.group.RuleBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.node.rule.group.RuleKey;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.risk.parameter.pac.RiskCharacteristic;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.risk.parameter.pac.RiskCharacteristicBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.topology.Link;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.topology.LinkBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.topology.LinkKey;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.topology.Node;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.topology.NodeBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.topology.NodeKey;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.topology.context.Topology;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.topology.context.TopologyBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.topology.context.TopologyKey;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.transfer.cost.pac.CostCharacteristic;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.transfer.cost.pac.CostCharacteristicBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.transfer.timing.pac.LatencyCharacteristic;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.transfer.timing.pac.LatencyCharacteristicBuilder;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.Notification;
import org.opendaylight.yangtools.yang.common.Uint16;
import org.opendaylight.yangtools.yang.common.Uint32;
import org.opendaylight.yangtools.yang.common.Uint64;
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
//    private final TapiLink tapiLink;
    private final ConvertORToTapiTopology tapiFactory;
    private final NotificationPublishService notificationPublishService;
    private Map<ServiceInterfacePointKey, ServiceInterfacePoint> sipMap = new HashMap<>();

    @Activate
    public TapiNetworkModelServiceImpl(@Reference NetworkTransactionService networkTransactionService,
            @Reference DeviceTransactionManager deviceTransactionManager,
            @Reference TapiLink tapiLink,
            @Reference final NotificationPublishService notificationPublishService) {
        this.networkTransactionService = networkTransactionService;
        this.linkDiscovery = new R2RTapiLinkDiscovery(networkTransactionService, deviceTransactionManager, tapiLink);
        this.notificationPublishService = notificationPublishService;
        this.tapiFactory = new ConvertORToTapiTopology(tapiTopoUuid);
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
                String nodeId = node.getNodeId() + TapiStringConstants.XXPDR + xpdrNb;
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
            .setNotificationType(NOTIFICATIONTYPEATTRIBUTEVALUECHANGE.VALUE)
//            .setTargetObjectType(ObjectType.NODEEDGEPOINT)
            //TODO: Change this : modification in Models 2.4 does not provide for Object type Node EdgePoint
            .setTargetObjectType(TOPOLOGYOBJECTTYPENODEEDGEPOINT.VALUE)
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
            int nbAffectedLinks = 0;
            LOG.info("UUIDofAffectedONEPS = {} ", changedOneps.toString());
            for (Link link : optTopology.orElseThrow().nonnullLink().values()) {
                List<Uuid> linkNeps = Objects.requireNonNull(link.getNodeEdgePoint()).values().stream()
                        .map(NodeEdgePointRef::getNodeEdgePointUuid).collect(Collectors.toList());
                LOG.info("LinkEndPointsUUID = {} for link Name {}", linkNeps.toString(), link.getName().toString());
                if (!Collections.disjoint(changedOneps, linkNeps)) {
                    InstanceIdentifier<Link> linkIID = InstanceIdentifier.builder(Context.class)
                            .augmentation(Context1.class).child(TopologyContext.class)
                            .child(Topology.class, new TopologyKey(tapiTopoUuid))
                            .child(Link.class, new LinkKey(link.getUuid())).build();
                    Link linkblr = new LinkBuilder().setUuid(link.getUuid())
                            .setAdministrativeState(transformAdminState(mapping.getPortAdminState()))
                            .setOperationalState(transformOperState(mapping.getPortOperState())).build();
                    this.networkTransactionService.merge(LogicalDatastoreType.OPERATIONAL, linkIID, linkblr);
                    nbAffectedLinks++ ;
                }
            }
            LOG.info("AffectedLinksNb = {} ", nbAffectedLinks);
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
                        LOG.info("UpdatedNEP {} of UUID {} to ADMIN {} OPER {}",
                            onep.getName().toString(), onep.getUuid(),
                            transformAdminState(mapping.getPortAdminState()),
                            transformOperState(mapping.getPortOperState()));
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
        } else if (nodeId.contains("PDR")) {
            LOG.debug("ANALYSING change in {}", nodeId);
            int xpdrNb = Integer.parseInt(mapping.getLogicalConnectionPoint().split("XPDR")[1].split("-")[0]);
            String xpdrNodeId = nodeId + TapiStringConstants.XXPDR + xpdrNb;
            uuids.add(new Uuid(UUID.nameUUIDFromBytes((String.join("+", xpdrNodeId, TapiStringConstants.XPDR))
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
        Uuid nodeUuidDsr = new Uuid(UUID.nameUUIDFromBytes((String.join("+", nodeId, TapiStringConstants.XPDR))
            .getBytes(StandardCharsets.UTF_8)).toString());
        Name nameDsr = new NameBuilder().setValueName("dsr/odu node name").setValue(
            String.join("+", nodeId, TapiStringConstants.XPDR)).build();
        Name nameOtsi =  new NameBuilder().setValueName("otsi node name").setValue(
            String.join("+", nodeId, TapiStringConstants.XPDR)).build();
        Name nameNodeType = new NameBuilder().setValueName("Node Type")
            .setValue(getNodeType(xponderType)).build();
        Set<LayerProtocolName> dsrLayerProtocols = Set.of(LayerProtocolName.DSR, LayerProtocolName.ODU,
            LayerProtocolName.DIGITALOTN, LayerProtocolName.PHOTONICMEDIA);
        Node dsrNode = createTapiXpdrNode(Map.of(nameDsr.key(), nameDsr, nameOtsi.key(), nameOtsi, nameNodeType.key(),
            nameNodeType), dsrLayerProtocols, nodeId, nodeUuidDsr, xpdrClMaps, xpdrNetMaps, xponderType,
            oorOduSwitchingPool);

        nodeMap.put(dsrNode.key(), dsrNode);
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
                            transformOperState(m.getPortOperState()), transformAdminState(m.getPortAdminState()),
                            TapiStringConstants.PHTNC_MEDIA_OTS);
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
                            transformOperState(m.getPortOperState()), transformAdminState(m.getPortAdminState()),
                            TapiStringConstants.PHTNC_MEDIA_OTS);
                degNeps.putAll(createRoadmNeps(orNodeId, m.getLogicalConnectionPoint(), false,
                        transformOperState(m.getPortOperState()), transformAdminState(m.getPortAdminState()),
                        TapiStringConstants.PHTNC_MEDIA_OMS));
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

        Map<RuleKey, Rule> ruleList = new HashMap<>();
        Set<RuleType> ruleTypes = new HashSet<>();
        ruleTypes.add(RuleType.FORWARDING);
        Rule rule = new RuleBuilder()
                .setLocalId("forward")
                .setForwardingRule(FORWARDINGRULEMAYFORWARDACROSSGROUP.VALUE)
                .setRuleType(ruleTypes)
                .build();
        ruleList.put(rule.key(), rule);
        if (!(layerProtocols.contains(LayerProtocolName.DSR)
                && layerProtocols.contains(LayerProtocolName.PHOTONICMEDIA))) {
            LOG.error("Undefined LayerProtocolName for {} node {}", nameMap.get(nameMap.keySet().iterator().next())
                .getValueName(), nameMap.get(nameMap.keySet().iterator().next()).getValue());
        }
        Map<OwnedNodeEdgePointKey, OwnedNodeEdgePoint> onepl = new HashMap<>();
        onepl.putAll(createXpdrDsrOduNeps(nodeId, xpdrClMaps, xpdrNetMaps, xponderType));
        Map<NodeRuleGroupKey, NodeRuleGroup> nodeRuleGroupList = createNodeRuleGroupForDsrNode(
                    nodeId, oorOduSwitchingPool, ruleList, onepl);
        onepl.putAll(createXpdrPhtnMdNeps(nodeId, xpdrNetMaps));

        // Empty random creation of mandatory fields for avoiding errors....
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
        RiskParameterPac riskParamPac = new RiskParameterPacBuilder()
            .setRiskCharacteristic(Map.of(riskCharacteristic.key(), riskCharacteristic))
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
            .setRiskParameterPac(riskParamPac)
            .build();
    }

    private Map<OwnedNodeEdgePointKey, OwnedNodeEdgePoint> createXpdrPhtnMdNeps(String nodeId,
                                                                                List<Mapping> xpdrNetMaps) {
        Map<OwnedNodeEdgePointKey, OwnedNodeEdgePoint> onepl = new HashMap<>();

        // eNep creation on otsi node
        for (int i = 0; i < xpdrNetMaps.size(); i++) {
            Uuid nepUuid2 = new Uuid(UUID.nameUUIDFromBytes(
                (String.join("+", nodeId, TapiStringConstants.PHTNC_MEDIA_OTS,
                    xpdrNetMaps.get(i).getLogicalConnectionPoint())).getBytes(StandardCharsets.UTF_8)).toString());
            Name onedName = new NameBuilder()
                .setValueName("eNodeEdgePoint")
                .setValue(String.join("+", nodeId, TapiStringConstants.PHTNC_MEDIA_OTS,
                    xpdrNetMaps.get(i).getLogicalConnectionPoint()))
                .build();

            List<SupportedIfCapability> newSupIfCapList =
                new ArrayList<>(xpdrNetMaps.get(i).getSupportedInterfaceCapability());
            List<String> opModeList = new ArrayList<>();
            if (xpdrNetMaps.get(i).getSupportedOperationalMode() != null) {
                opModeList.addAll(xpdrNetMaps.get(i).getSupportedOperationalMode());
            }

            OwnedNodeEdgePoint onep = createNep(nodeId, nepUuid2, xpdrNetMaps.get(i).getLogicalConnectionPoint(),
                Map.of(onedName.key(), onedName), LayerProtocolName.PHOTONICMEDIA, LayerProtocolName.PHOTONICMEDIA,
                true, String.join("+", nodeId, TapiStringConstants.PHTNC_MEDIA_OTS), newSupIfCapList, opModeList,
                transformOperState(xpdrNetMaps.get(i).getPortOperState()),
                transformAdminState(xpdrNetMaps.get(i).getPortAdminState()));
            onepl.put(onep.key(), onep);
        }
        // OTSi_MC Nep creation on otsi node
        for (int i = 0; i < xpdrNetMaps.size(); i++) {
            Uuid nepUuid3 = new Uuid(UUID.nameUUIDFromBytes(
                (String.join("+", nodeId, TapiStringConstants.OTSI_MC,
                    xpdrNetMaps.get(i).getLogicalConnectionPoint())).getBytes(StandardCharsets.UTF_8)).toString());
            Name onedName = new NameBuilder()
                .setValueName("PhotMedNodeEdgePoint")
                .setValue(String.join("+", nodeId, TapiStringConstants.OTSI_MC,
                    xpdrNetMaps.get(i).getLogicalConnectionPoint()))
                .build();

            List<SupportedIfCapability> newSupIfCapList =
                    new ArrayList<>(xpdrNetMaps.get(i).getSupportedInterfaceCapability());
            List<String> opModeList = new ArrayList<>();
            if (xpdrNetMaps.get(i).getSupportedOperationalMode() != null) {
                opModeList.addAll(xpdrNetMaps.get(i).getSupportedOperationalMode());
            }

            OwnedNodeEdgePoint onep = createNep(nodeId, nepUuid3, xpdrNetMaps.get(i).getLogicalConnectionPoint(),
                Map.of(onedName.key(), onedName), LayerProtocolName.PHOTONICMEDIA, LayerProtocolName.PHOTONICMEDIA,
                false, String.join("+", nodeId, TapiStringConstants.OTSI_MC), newSupIfCapList, opModeList,
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

            OwnedNodeEdgePoint onep = createNep(nodeId, nepUuid, xpdrClMaps.get(i).getLogicalConnectionPoint(),
                Map.of(name.key(), name), LayerProtocolName.DSR, LayerProtocolName.DSR, true,
                String.join("+", nodeId, TapiStringConstants.DSR), newSupIfCapList, null,
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
            OwnedNodeEdgePoint onep = createNep(nodeId, nepUuid, xpdrNetMaps.get(i).getLogicalConnectionPoint(),
                Map.of(onedName.key(), onedName),
                LayerProtocolName.ODU, LayerProtocolName.DSR, true,
                String.join("+", nodeId, TapiStringConstants.I_ODU), newSupIfCapList, null,
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

            OwnedNodeEdgePoint onep = createNep(nodeId, nepUuid, xpdrClMaps.get(i).getLogicalConnectionPoint(),
                Map.of(onedName.key(), onedName),
                LayerProtocolName.ODU, LayerProtocolName.DSR, true,
                String.join("+", nodeId, TapiStringConstants.E_ODU), newSupIfCapList, null,
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

    private OwnedNodeEdgePoint createNep(String nodeId, Uuid nepUuid, String tpid, Map<NameKey, Name> nepNames,
            LayerProtocolName nepProtocol, LayerProtocolName nodeProtocol, boolean withSip, String keyword,
            List<SupportedIfCapability> sicList, List<String> opModeList,
            OperationalState operState, AdministrativeState adminState) {
        List<SupportedInterfaceCapability> sicListTemp = new ArrayList<>();
        List<OperationalModeKey> keyedOpModeList = new ArrayList<>();
        for (SupportedIfCapability supInterCapa : sicList) {
            SupportedInterfaceCapability supIfCapa = new SupportedInterfaceCapabilityBuilder()
                    .withKey(new SupportedInterfaceCapabilityKey(supInterCapa))
                    .setIfCapType(supInterCapa)
                    .build();
            sicListTemp.add(supIfCapa);
        }
        Collection<SupportedInterfaceCapability> sicColl = sicListTemp;
        OwnedNodeEdgePointBuilder onepBldr = new OwnedNodeEdgePointBuilder()
                .setUuid(nepUuid)
                .setLayerProtocolName(nepProtocol)
                .setName(nepNames);
        if (withSip) {
            onepBldr.setMappedServiceInterfacePoint(this.tapiFactory.createMSIP(1, nepProtocol, tpid, keyword,
                    sicColl, operState, adminState));
            this.sipMap.putAll(tapiFactory.getTapiSips());
        }
        LOG.debug("Node layer {}", nodeProtocol.getName());
        onepBldr.setSupportedCepLayerProtocolQualifierInstances(
                this.tapiFactory.createSupportedCepLayerProtocolQualifier(sicColl, nepProtocol));
        onepBldr.setDirection(Direction.BIDIRECTIONAL).setLinkPortRole(PortRole.SYMMETRIC)
                .setAdministrativeState(adminState).setOperationalState(operState)
                .setLifecycleState(LifecycleState.INSTALLED);
        if (keyword.contains(TapiStringConstants.OTSI_MC) || keyword.contains(TapiStringConstants.PHTNC_MEDIA_OTS)) {
            if (opModeList == null || opModeList.isEmpty()) {
                for (SupportedInterfaceCapability sic : sicColl) {
                    String ifCapType = sic.getIfCapType().toString().split("\\{")[0];
                    if (("IfOCHOTUCnODUCn").equals(ifCapType) || ("IfOCHOTUCnODUCnUniregen").equals(ifCapType)
                            || ("IfOCHOTUCnODUCnRegen").equals(ifCapType)) {
                        keyedOpModeList.add(new OperationalModeKey("400G"));
                        LOG.warn(TopologyUtils.NOOPMODEDECLARED + "400G rate available", tpid);
                        break;
                    }
                }
                keyedOpModeList.add(new OperationalModeKey("100G"));
                LOG.warn(TopologyUtils.NOOPMODEDECLARED + "100G rate available", tpid);
            } else {
                for (String opMode : opModeList) {
                    keyedOpModeList.add(new OperationalModeKey(opMode));
                }
            }
            Map<Double, Double> freqWidthMap = new HashMap<>();
            if (getNetworkTerminationPointFromDatastore(nodeId, tpid) != null) {
                freqWidthMap = tapiFactory.getXpdrUsedWavelength(getNetworkTerminationPointFromDatastore(nodeId, tpid));
            } else {
                LOG.error("CREATENEP, No Tp found in topology for LCP {}, of NodeId {} ", tpid, nodeId);
            }
            onepBldr = tapiFactory.addPayloadStructureAndPhotSpecToOnep(nodeId, freqWidthMap, keyedOpModeList, sicColl,
                onepBldr, keyword);
        }
        return onepBldr.build();
    }

    private Map<OwnedNodeEdgePointKey, OwnedNodeEdgePoint> createRoadmNeps(String orNodeId, String tpId,
            boolean withSip, OperationalState operState, AdministrativeState adminState, String nepPhotonicSublayer) {
        Map<OwnedNodeEdgePointKey, OwnedNodeEdgePoint> onepMap = new HashMap<>();
        // PHOTONIC MEDIA nep
        Uuid nepUuid = new Uuid(UUID.nameUUIDFromBytes((String.join("+", orNodeId,
            nepPhotonicSublayer, tpId)).getBytes(StandardCharsets.UTF_8)).toString());
        Name nepName = new NameBuilder()
                .setValueName(TapiStringConstants.PHTNC_MEDIA + "NodeEdgePoint")
                .setValue(String.join("+", orNodeId, nepPhotonicSublayer, tpId))
                .build();
        List<SupportedCepLayerProtocolQualifierInstances> sclpqiList = new ArrayList<>();
        sclpqiList.add(
            new SupportedCepLayerProtocolQualifierInstancesBuilder()
                .setLayerProtocolQualifier(
                    TapiStringConstants.PHTNC_MEDIA_OMS.equals(nepPhotonicSublayer)
                        ? PHOTONICLAYERQUALIFIEROMS.VALUE
                        : PHOTONICLAYERQUALIFIEROTS.VALUE)
                .setNumberOfCepInstances(Uint64.valueOf(1))
                .build());
        OwnedNodeEdgePoint onep = new OwnedNodeEdgePointBuilder()
            .setUuid(nepUuid)
            .setLayerProtocolName(LayerProtocolName.PHOTONICMEDIA)
            .setName(Map.of(nepName.key(), nepName))
            .setSupportedCepLayerProtocolQualifierInstances(sclpqiList)
            .setDirection(Direction.BIDIRECTIONAL)
            .setLinkPortRole(PortRole.SYMMETRIC)
            .setAdministrativeState(adminState).setOperationalState(operState)
            .setLifecycleState(LifecycleState.INSTALLED)
            .build();
        onepMap.put(onep.key(), onep);
        return onepMap;
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
            .setQueuingLatencyCharacteristic(TapiStringConstants.QUEING_LATENCY_VALUE)
            .setJitterCharacteristic(TapiStringConstants.JITTER_VALUE)
            .setWanderCharacteristic(TapiStringConstants.WANDER_VALUE)
            .setTrafficPropertyName("FIXED_LATENCY")
            .build();
        RiskCharacteristic riskCharacteristic = new RiskCharacteristicBuilder()
            .setRiskCharacteristicName("risk characteristic")
            .setRiskIdentifierList(Set.of("risk identifier1", "risk identifier2"))
            .build();
        RiskParameterPac riskParamPac = new RiskParameterPacBuilder()
            .setRiskCharacteristic(Map.of(riskCharacteristic.key(), riskCharacteristic))
            .build();
        return new NodeBuilder()
            .setUuid(nodeUuid)
            .setName(Map.of(nodeNames.key(), nodeNames, nameNodeType.key(), nameNodeType))
            .setLayerProtocolName(layerProtocols)
            .setAdministrativeState(AdministrativeState.UNLOCKED)
            .setOperationalState(OperationalState.ENABLED)
            .setLifecycleState(LifecycleState.INSTALLED)
            .setOwnedNodeEdgePoint(oneplist)
            .setNodeRuleGroup(this.tapiFactory
                    .createNodeRuleGroupForRdmNode("Full", nodeUuid, orNodeId, oneplist.values()))
            .setCostCharacteristic(Map.of(costCharacteristic.key(), costCharacteristic))
            .setLatencyCharacteristic(Map.of(latencyCharacteristic.key(), latencyCharacteristic))
            .setErrorCharacteristic("error")
            .setLossCharacteristic("loss")
            .setRepeatDeliveryCharacteristic("repeat delivery")
            .setDeliveryOrderCharacteristic("delivery order")
            .setUnavailableTimeCharacteristic("unavailable time")
            .setServerIntegrityProcessCharacteristic("server integrity process")
            .setRiskParameterPac(riskParamPac)
            .build();
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

    private Map<NodeRuleGroupKey, NodeRuleGroup> createNodeRuleGroupForDsrNode(String nodeId,
            OduSwitchingPools oorOduSwitchingPool, Map<RuleKey, Rule> ruleList,
            Map<OwnedNodeEdgePointKey, OwnedNodeEdgePoint> onepl) {
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
                .setQueuingLatencyCharacteristic(TapiStringConstants.QUEING_LATENCY_VALUE)
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
            .augmentation(org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev221121.Context1.class)
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
                .augmentation(org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev221121.Context1.class)
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
                .augmentation(org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev221121.Context1.class)
                .child(ConnectivityContext.class).child(ConnectivityService.class,
                        new ConnectivityServiceKey(updService.getUuid())).build();
        this.networkTransactionService.merge(LogicalDatastoreType.OPERATIONAL, connectivityserviceIID, updService);
        try {
            this.networkTransactionService.commit().get();
        } catch (InterruptedException | ExecutionException e) {
            LOG.error("Error committing into datastore", e);
        }
    }

    /**
     * Get a network termination point for nodeId and tpId.
     * @param nodeId String
     * @param tpId String
     * @return network termination point, null otherwise
     */
    private TerminationPoint getNetworkTerminationPointFromDatastore(String nodeId, String tpId) {
        InstanceIdentifier<TerminationPoint> tpIID = InstanceIdentifier.builder(Networks.class)
            .child(Network.class, new NetworkKey(new NetworkId(NetworkUtils.OVERLAY_NETWORK_ID)))
            .child(
                org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226
                    .networks.network.Node.class,
                new org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226
                    .networks.network.NodeKey(new NodeId(nodeId)))
            .augmentation(Node1.class)
            .child(
                org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226
                    .networks.network.node.TerminationPoint.class,
                new org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226
                    .networks.network.node.TerminationPointKey(new TpId(tpId)))
            .build();
        try (ReadTransaction readTx = this.networkTransactionService.getDataBroker().newReadOnlyTransaction()) {
            Optional<TerminationPoint> optionalTerminationPoint = readTx
                    .read(LogicalDatastoreType.CONFIGURATION, tpIID)
                    .get(Timeouts.DATASTORE_READ, TimeUnit.MILLISECONDS);
            return optionalTerminationPoint.isEmpty() ? null : optionalTerminationPoint.orElseThrow();
        } catch (ExecutionException | TimeoutException e) {
            LOG.warn("Exception while getting termination {} for node id {} point from {} topology",
                    tpId, nodeId, NetworkUtils.OVERLAY_NETWORK_ID, e);
            return null;
        } catch (InterruptedException e) {
            LOG.warn("Getting termination {} for node id {} point from {} topology was interrupted",
                    tpId, nodeId, NetworkUtils.OVERLAY_NETWORK_ID, e);
            Thread.currentThread().interrupt();
            return null;
        }
    }


}
