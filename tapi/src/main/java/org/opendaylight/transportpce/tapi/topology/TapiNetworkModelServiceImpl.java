/*
 * Copyright © 2021 Nokia, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.tapi.topology;

import java.math.RoundingMode;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import org.opendaylight.mdsal.binding.api.NotificationPublishService;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.transportpce.common.NetworkUtils;
import org.opendaylight.transportpce.common.device.DeviceTransactionManager;
import org.opendaylight.transportpce.common.fixedflex.GridConstant;
import org.opendaylight.transportpce.common.network.NetworkTransactionService;
import org.opendaylight.transportpce.tapi.R2RTapiLinkDiscovery;
import org.opendaylight.transportpce.tapi.TapiStringConstants;
import org.opendaylight.transportpce.tapi.impl.TapiProvider;
import org.opendaylight.transportpce.tapi.utils.TapiLink;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev240315.mapping.Mapping;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev240315.mapping.MappingKey;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev240315.network.Nodes;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev240315.switching.pool.lcp.SwitchingPoolLcp;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev240315.switching.pool.lcp.SwitchingPoolLcpKey;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev230526.TerminationPoint1;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.state.types.rev191129.State;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.types.rev191129.XpdrNodeTypes;
import org.opendaylight.yang.gen.v1.http.org.openroadm.equipment.states.types.rev191129.AdminStates;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.types.rev230526.xpdr.odu.switching.pools.OduSwitchingPools;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.types.rev230526.xpdr.odu.switching.pools.OduSwitchingPoolsBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.types.rev230526.xpdr.odu.switching.pools.OduSwitchingPoolsKey;
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
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.CAPACITYUNITGBPS;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.Context;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.ContextBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.Direction;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.LAYERPROTOCOLQUALIFIER;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.LayerProtocolName;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.LifecycleState;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.OperationalState;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.PortRole;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.Uuid;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.capacity.TotalSize;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.capacity.TotalSizeBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.capacity.pac.AvailableCapacity;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.capacity.pac.AvailableCapacityBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.capacity.pac.TotalPotentialCapacity;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.capacity.pac.TotalPotentialCapacityBuilder;
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
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.digital.otn.rev221121.ODUTYPEODU4;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.digital.otn.rev221121.ODUTYPEODUCN;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.digital.otn.rev221121.OTUTYPEOTU4;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.digital.otn.rev221121.OTUTYPEOTUCN;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.notification.rev221121.NOTIFICATIONTYPEATTRIBUTEVALUECHANGE;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.notification.rev221121.NotificationBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.notification.rev221121.notification.ChangedAttributes;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.notification.rev221121.notification.ChangedAttributesBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.notification.rev221121.notification.ChangedAttributesKey;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.photonic.media.rev221121.PHOTONICLAYERQUALIFIERMC;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.photonic.media.rev221121.PHOTONICLAYERQUALIFIEROMS;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.photonic.media.rev221121.PHOTONICLAYERQUALIFIEROTS;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.photonic.media.rev221121.PHOTONICLAYERQUALIFIEROTSiMC;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.photonic.media.rev221121.context.topology.context.topology.node.owned.node.edge.point.PhotonicMediaNodeEdgePointSpec;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.Context1;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.FORWARDINGRULECANNOTFORWARDACROSSGROUP;
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
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.node.edge.point.Profile;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.node.edge.point.ProfileBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.node.edge.point.ProfileKey;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.node.edge.point.SinkProfile;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.node.edge.point.SinkProfileBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.node.edge.point.SinkProfileKey;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.node.edge.point.SourceProfile;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.node.edge.point.SourceProfileBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.node.edge.point.SourceProfileKey;
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
import org.opendaylight.yangtools.binding.DataObjectIdentifier;
import org.opendaylight.yangtools.yang.common.Decimal64;
import org.opendaylight.yangtools.yang.common.Uint16;
import org.opendaylight.yangtools.yang.common.Uint32;
import org.opendaylight.yangtools.yang.common.Uint64;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
//import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.Profile1;
//import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.ProfileRef;

@Component
public class TapiNetworkModelServiceImpl implements TapiNetworkModelService {

    private static final Logger LOG = LoggerFactory.getLogger(TapiNetworkModelServiceImpl.class);

    private final Uuid tapiTopoUuid = TapiProvider.TAPI_TOPO_UUID;
    private static final String TOPOLOGICAL_MODE = TapiProvider.TOPOLOGICAL_MODE;
    private final NetworkTransactionService networkTransactionService;
    private final R2RTapiLinkDiscovery linkDiscovery;
    private final TapiLink tapiLink;
    private final ORToTapiTopoConversionFactory tapiFactory;
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
        this.tapiFactory = new ORToTapiTopoConversionFactory(tapiTopoUuid);
        this.tapiLink = tapiLink;

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
        switch (node.getNodeInfo().getNodeType()) {
            case Rdm:
                // ROADM device
                // transform flat mapping list to per degree and per srg mapping lists
                Map<String, List<Mapping>> mapDeg = new HashMap<>();
                Map<String, List<Mapping>> mapSrg = new HashMap<>();
                List<Mapping> mappingList = new ArrayList<>(node.nonnullMapping().values());
                mappingList.sort(Comparator.comparing(Mapping::getLogicalConnectionPoint));
                // populate degree and srg LCP map
                for (String str : getRoadmNodelist(mappingList)) {
                    List<Mapping> interList = mappingList.stream()
                        .filter(x -> x.getLogicalConnectionPoint().contains(str))
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
                    new HashMap<>(transformSrgToOnep(orNodeId, mapSrg));
                LOG.debug("CreateTapiNode NetworkModelServiceImpl, TopologicalMode = {}", TOPOLOGICAL_MODE);
                LOG.debug("TAPINETWORKMODELSERVICEIMPL call transformSRGtoONEP (OrNodeId {} ", orNodeId);
                LOG.debug("TAPINETWORKMODELSERVICEIMPL SRG OTSNode of retrieved OnepMap {} ",
                    onepMap.entrySet().stream()
                        .filter(e -> e.getValue().getSupportedCepLayerProtocolQualifierInstances()
                            .contains(
                                new SupportedCepLayerProtocolQualifierInstancesBuilder()
                                    .setNumberOfCepInstances(Uint64.valueOf(1))
                                    .setLayerProtocolQualifier(PHOTONICLAYERQUALIFIEROTS.VALUE)
                                    .build()))
                        .collect(Collectors.toList()));
                if (!TOPOLOGICAL_MODE.equals("Full")) {
                    // create tapi Node
                    Node roadmNode = createRoadmTapiNode("ROADMINFRA", onepMap);
                    mergeNodeinTopology(Map.of(roadmNode.key(), roadmNode));
                    mergeSipsinContext(this.sipMap);
                    // TODO add states corresponding to device config -> based on mapping.
                    //  This should be possible after Gilles work is merged
                    LOG.info("TAPI node for or node {} successfully merged", orNodeId);
                    break;
                }
                onepMap.putAll(transformDegToOnep(orNodeId, mapDeg));
                LOG.debug("TAPINETWORKMODELSERVICEIMPL DEG+SRG OTSNode of retrieved OnepMap {} ",
                    onepMap.entrySet().stream()
                        .filter(e -> e.getValue().getSupportedCepLayerProtocolQualifierInstances()
                            .contains(
                                new SupportedCepLayerProtocolQualifierInstancesBuilder()
                                    .setNumberOfCepInstances(Uint64.valueOf(1))
                                    .setLayerProtocolQualifier(PHOTONICLAYERQUALIFIEROTS.VALUE)
                                    .build()))
                        .collect(Collectors.toList()));
                LOG.debug("TAPINETWORKMODELSERVICEIMPL DEG+SRG complete retrieved OnepMap {} ", onepMap);
                // create tapi Node
                Node roadmNode = createRoadmTapiNode(orNodeId, onepMap);
                mergeNodeinTopology(Map.of(roadmNode.key(), roadmNode));
                mergeSipsinContext(this.sipMap);
                // TODO add states corresponding to device config -> based on mapping.
                // This should be possible after Gilles work is merged

                // rdm to rdm link creation if neighbour roadm is mounted
                LOG.info("checking if neighbor roadm exists");
                Map<LinkKey, Link> rdm2rdmLinks =
                    this.linkDiscovery.readLLDP(new NodeId(orNodeId), orNodeVersion, this.tapiTopoUuid);
                if (!rdm2rdmLinks.isEmpty()) {
                    Map<Map<String, String>, ConnectionEndPoint> cepMap = this.tapiLink.getCepMap();
                    addCepToOnep(onepMap, cepMap);
                    mergeLinkinTopology(rdm2rdmLinks);
                }
                LOG.info("TAPI node for or node {} successfully merged", orNodeId);
                break;

            case Xpdr:
                Map<Integer, String> xpdrMap = new HashMap<>();
                for (Mapping mapping : node.nonnullMapping().values().stream()
                        .filter(k -> k.getLogicalConnectionPoint().contains("NETWORK"))
                        .collect(Collectors.toList())) {
                    Integer xpdrNb =
                        Integer.parseInt(mapping.getLogicalConnectionPoint().split("XPDR")[1].split("-")[0]);
                    String nodeId = node.getNodeId() + TapiStringConstants.XXPDR + xpdrNb;
                    if (xpdrMap.containsKey(xpdrNb)) {
                        continue;
                    }
                    List<Mapping> xpdrNetMaps = node.nonnullMapping().values().stream()
                        .filter(k -> k.getLogicalConnectionPoint()
                            .contains("XPDR" + xpdrNb + TapiStringConstants.NETWORK))
                        .collect(Collectors.toList());
                    List<Mapping> xpdrClMaps = node.nonnullMapping().values().stream()
                        .filter(k -> k.getLogicalConnectionPoint()
                            .contains("XPDR" + xpdrNb + TapiStringConstants.CLIENT))
                        .collect(Collectors.toList());
                    xpdrMap.put(xpdrNb, node.getNodeId());
                    // create switching pool
                    Map<OduSwitchingPoolsKey, OduSwitchingPools> oduSwPoolMap =
                        createSwitchPoolForAnyXpdr(node, mapping.getXpdrType(), xpdrNetMaps, xpdrNb);
                    // add nodes and sips to tapi context
                    mergeNodeinTopology(new HashMap<>(
                        // node transformation
                        transformXpdrToTapiNode(
                            node, nodeId, xpdrClMaps, xpdrNetMaps, mapping.getXpdrType(), oduSwPoolMap)));
                    mergeSipsinContext(this.sipMap);
                }
                LOG.info("TAPI node for or node {} successfully merged", orNodeId);
                break;

            default:
                break;
        }
        // Device not managed yet
    }

    @Override
    public void updateTapiTopology(String nodeId, Mapping mapping) {
        List<Uuid> changedOneps = updateNeps(mapping, getChangedNodeUuids(nodeId, mapping));
        updateLinks(changedOneps, mapping);
        sendNotification(changedOneps, mapping);
        LOG.info("Updated TAPI topology successfully.");
    }

    @SuppressWarnings("rawtypes")
    private void sendNotification(List<Uuid> changedOneps, Mapping mapping) {
        try {
            notificationPublishService.putNotification(
                new NotificationBuilder()
                    .setNotificationType(NOTIFICATIONTYPEATTRIBUTEVALUECHANGE.VALUE)
                    // .setTargetObjectType(ObjectType.NODEEDGEPOINT)
                    //TODO: Change this : modification in Models 2.4 does not provide for Object type Node EdgePoint
                    .setTargetObjectType(TOPOLOGYOBJECTTYPENODEEDGEPOINT.VALUE)
                    .setChangedAttributes(getChangedAttributes(changedOneps, mapping))
                    .setUuid(tapiTopoUuid)
                    .build());
        } catch (InterruptedException e) {
            LOG.error("Could not send notification");
        }
    }

    private void addCepToOnep(Map<OwnedNodeEdgePointKey, OwnedNodeEdgePoint> onepMap,
            Map<Map<String, String>, ConnectionEndPoint> cepMap) {
        LOG.debug("TAPINetModServImpl332, Entering addCepToOnep, with cepMap {} and onepMapKeyList {}", cepMap,
            onepMap.entrySet().stream().map(Map.Entry::getKey).collect(Collectors.toList()));
        for (Map.Entry<Map<String, String>, ConnectionEndPoint> cepEntry : cepMap.entrySet()) {
            if (!onepMap.entrySet().stream().map(onep -> onep.getKey().toString()).collect(Collectors.toList())
                    .contains(cepEntry.getKey().entrySet().stream().findFirst().orElseThrow().getKey())) {
                continue;
            }
            OwnedNodeEdgePoint ownedNep = onepMap.entrySet().stream()
                .filter(onep -> onep.getKey().getUuid().toString()
                    .equals(cepEntry.getKey().entrySet().stream().findAny().orElseThrow().getKey()))
                .map(Map.Entry::getValue).findFirst().orElseThrow();
            CepList cepList = new CepListBuilder()
                .setConnectionEndPoint(Map.of(cepEntry.getValue().key(), cepEntry.getValue())).build();
            OwnedNodeEdgePoint1 onep1Bldr = new OwnedNodeEdgePoint1Builder().setCepList(cepList).build();
            OwnedNodeEdgePoint newOnep = new OwnedNodeEdgePointBuilder(ownedNep)
                    .addAugmentation(onep1Bldr)
                    .build();
            onepMap.put(newOnep.key(), newOnep);
            LOG.info("TAPINetModServImpl345, getting out of addCepToOnep with no Exception");
        }

    }

    private Map<ChangedAttributesKey, ChangedAttributes> getChangedAttributes(List<Uuid> changedOneps,
            Mapping mapping) {
        Map<ChangedAttributesKey, ChangedAttributes> changedAttributes = new HashMap<>();
        String operState = mapping.getPortOperState();
        String oldState = operState.equals("InService") ? "OutOfService" : "InService";
        for (Uuid nep : changedOneps) {
            String nepVal = nep.getValue();
            changedAttributes.put(
                new ChangedAttributesKey(nepVal),
                new ChangedAttributesBuilder()
                    .setValueName(nepVal)
                    .setOldValue(oldState)
                    .setNewValue(operState)
                    .build());
        }
        return changedAttributes;
    }

    private void updateLinks(List<Uuid> changedOneps, Mapping mapping) {
        try {
            Optional<Topology> optTopology =
                this.networkTransactionService.read(
                        LogicalDatastoreType.OPERATIONAL,
                        DataObjectIdentifier.builder(Context.class)
                            .augmentation(Context1.class)
                            .child(TopologyContext.class)
                            .child(Topology.class, new TopologyKey(tapiTopoUuid))
                            .build())
                    .get();
            if (optTopology.isEmpty()) {
                LOG.error("Could not update TAPI links");
                return;
            }
            int nbAffectedLinks = 0;
            LOG.info("UUIDofAffectedONEPS = {} ", changedOneps);
            AdministrativeState newAdmState = transformAdminState(mapping.getPortAdminState());
            OperationalState newOprState = transformOperState(mapping.getPortOperState());
            for (Link link : optTopology.orElseThrow().nonnullLink().values()) {
                List<Uuid> linkNeps = Objects.requireNonNull(link.getNodeEdgePoint()).values().stream()
                        .map(NodeEdgePointRef::getNodeEdgePointUuid)
                        .collect(Collectors.toList());
                LOG.info("LinkEndPointsUUID = {} for link Name {}", linkNeps, link.getName());
                if (Collections.disjoint(changedOneps, linkNeps)) {
                    continue;
                }
                this.networkTransactionService.merge(
                    LogicalDatastoreType.OPERATIONAL,
                    DataObjectIdentifier.builder(Context.class)
                        .augmentation(Context1.class)
                        .child(TopologyContext.class)
                        .child(Topology.class, new TopologyKey(tapiTopoUuid))
                        .child(Link.class, new LinkKey(link.getUuid()))
                        .build(),
                    new LinkBuilder()
                        .setUuid(link.getUuid())
                        .setAdministrativeState(newAdmState)
                        .setOperationalState(newOprState)
                        .build());
                nbAffectedLinks++ ;
            }
            LOG.info("AffectedLinksNb = {} ", nbAffectedLinks);
            this.networkTransactionService.commit().get();
        } catch (InterruptedException | ExecutionException e) {
            LOG.error("Could not update TAPI links");
        }
    }

    private List<Uuid> updateNeps(Mapping mapping, List<Uuid> uuids) {
        List<Uuid> changedOneps = new ArrayList<>();
        AdministrativeState newAdmState = transformAdminState(mapping.getPortAdminState());
        OperationalState newOprState = transformOperState(mapping.getPortOperState());
        for (Uuid nodeUuid : uuids) {
            try {
                Optional<Node> optionalNode =
                    this.networkTransactionService.read(
                            LogicalDatastoreType.OPERATIONAL,
                            DataObjectIdentifier.builder(Context.class)
                                .augmentation(Context1.class)
                                .child(TopologyContext.class)
                                .child(Topology.class, new TopologyKey(tapiTopoUuid))
                                .child(Node.class, new NodeKey(nodeUuid))
                                .build())
                        .get();
                if (optionalNode.isEmpty()) {
                    continue;
                }
                for (OwnedNodeEdgePoint onep : optionalNode.orElseThrow().getOwnedNodeEdgePoint().values().stream()
                        .filter(onep -> ((Name) onep.getName().values().toArray()[0]).getValue()
                                .contains(mapping.getLogicalConnectionPoint()))
                        .collect(Collectors.toList())) {
                    changedOneps.add(onep.getUuid());
                    updateSips(mapping, onep);
                    this.networkTransactionService.merge(
                        LogicalDatastoreType.OPERATIONAL,
                        DataObjectIdentifier.builder(Context.class)
                            .augmentation(Context1.class)
                            .child(TopologyContext.class)
                            .child(Topology.class, new TopologyKey(tapiTopoUuid))
                            .child(Node.class, new NodeKey(nodeUuid))
                            .child(OwnedNodeEdgePoint.class, new OwnedNodeEdgePointKey(onep.getUuid()))
                            .build(),
                        new OwnedNodeEdgePointBuilder()
                            .setUuid(onep.getUuid())
                            .addAugmentation(
                                new OwnedNodeEdgePoint1Builder().setCepList(getUpdatedCeps(mapping, onep)).build())
                            .setAdministrativeState(newAdmState)
                            .setOperationalState(newOprState)
                            .build());
                    LOG.info("UpdatedNEP {} of UUID {} to ADMIN {} OPER {}",
                        onep.getName(), onep.getUuid(), newAdmState, newOprState);
                }
                this.networkTransactionService.commit().get();
            } catch (InterruptedException | ExecutionException e) {
                LOG.error("Could not update TAPI NEP");
            }
        }
        return changedOneps;
    }

    private CepList getUpdatedCeps(Mapping mapping, OwnedNodeEdgePoint onep) {
        OwnedNodeEdgePoint1 onep1 = onep.augmentation(OwnedNodeEdgePoint1.class);
        if (onep1 == null) {
            return new CepListBuilder().setConnectionEndPoint(new HashMap<>()).build();
        }
        CepList onep1CepList = onep1.getCepList();
        if (onep1CepList == null) {
            return new CepListBuilder().setConnectionEndPoint(new HashMap<>()).build();
        }
        var onep1CepListConnEndPoint = onep1CepList.getConnectionEndPoint();
        if (onep1CepListConnEndPoint == null) {
            return new CepListBuilder().setConnectionEndPoint(new HashMap<>()).build();
        }
        Map<ConnectionEndPointKey, ConnectionEndPoint> cepMap = new HashMap<>();
        OperationalState newOprState = transformOperState(mapping.getPortOperState());
        for (Map.Entry<ConnectionEndPointKey, ConnectionEndPoint> entry : onep1CepListConnEndPoint.entrySet()) {
            cepMap.put(
                entry.getKey(),
                new ConnectionEndPointBuilder(entry.getValue()).setOperationalState(newOprState).build());
        }
        return new CepListBuilder().setConnectionEndPoint(cepMap).build();
    }

    private List<Uuid> getChangedNodeUuids(String nodeId, Mapping mapping) {
        if (nodeId.contains("ROADM")) {
            return new ArrayList<>(List.of(new Uuid(
                UUID.nameUUIDFromBytes(
                        (String.join("+", nodeId, TapiStringConstants.PHTNC_MEDIA)).getBytes(StandardCharsets.UTF_8))
                    .toString())));
        }
        if (nodeId.contains("PDR")) {
            LOG.debug("ANALYSING change in {}", nodeId);
            return new ArrayList<>(List.of(new Uuid(
                UUID.nameUUIDFromBytes(
                        (String.join("+",
                                //xpdrNodeId,
                                nodeId + TapiStringConstants.XXPDR
                                    // + xpdrNb,
                                    + Integer.parseInt(
                                        mapping.getLogicalConnectionPoint().split("XPDR")[1].split("-")[0]),
                                TapiStringConstants.XPDR))
                            .getBytes(StandardCharsets.UTF_8))
                    .toString())));
        }
        LOG.error("Updating this device is currently not supported");
        return new ArrayList<>();
    }

    private void updateSips(Mapping mapping, OwnedNodeEdgePoint onep) {
        if (onep.getMappedServiceInterfacePoint() == null) {
            return;
        }
        AdministrativeState newAdmState = transformAdminState(mapping.getPortAdminState());
        OperationalState newOprState = transformOperState(mapping.getPortOperState());
        for (MappedServiceInterfacePoint msip : onep.getMappedServiceInterfacePoint().values()) {
            this.networkTransactionService.merge(
                LogicalDatastoreType.OPERATIONAL,
                DataObjectIdentifier.builder(Context.class)
                    .child(ServiceInterfacePoint.class,
                            new ServiceInterfacePointKey(msip.getServiceInterfacePointUuid()))
                    .build(),
                new ServiceInterfacePointBuilder()
                    .setUuid(msip.getServiceInterfacePointUuid())
                    .setAdministrativeState(newAdmState)
                    .setOperationalState(newOprState)
                    .build());
        }
    }

    private Map<NodeKey, Node> transformXpdrToTapiNode(Nodes node, String nodeId, List<Mapping> xpdrClMaps,
            List<Mapping> xpdrNetMaps, XpdrNodeTypes xponderType,
            Map<OduSwitchingPoolsKey, OduSwitchingPools> oorOduSwitchingPoolMap) {
        LOG.info("creation of a DSR/ODU node for {}", nodeId);
        String nameVal = String.join("+", nodeId, TapiStringConstants.XPDR);
        Name nameDsr = new NameBuilder().setValueName("dsr/odu node name").setValue(nameVal).build();
        Name nameOtsi =  new NameBuilder().setValueName("otsi node name").setValue(nameVal).build();
        Name nameNodeType = new NameBuilder().setValueName("Node Type").setValue(getNodeType(xponderType)).build();
        Node dsrNode = createTapiXpdrNode(node,
            Map.of(nameDsr.key(), nameDsr, nameOtsi.key(), nameOtsi, nameNodeType.key(), nameNodeType),
            Set.of(LayerProtocolName.DSR, LayerProtocolName.ODU,
                LayerProtocolName.DIGITALOTN, LayerProtocolName.PHOTONICMEDIA),
            nodeId, new Uuid(UUID.nameUUIDFromBytes(nameVal.getBytes(StandardCharsets.UTF_8)).toString()),
            xpdrClMaps, xpdrNetMaps, xponderType, oorOduSwitchingPoolMap);
        return new HashMap<>(Map.of(dsrNode.key(), dsrNode));
    }

    private Map<OduSwitchingPoolsKey,OduSwitchingPools> createSwitchPoolForAnyXpdr(Nodes node,
            XpdrNodeTypes xpdrType, List<Mapping> xpdrNetMaps, Integer xpdrNb) {
        Map<OduSwitchingPoolsKey,OduSwitchingPools> oduSwPoolMap = new HashMap<>();
        OduSwitchingPools oduswpool;
        switch (xpdrType) {
            case Tpdr:
                oduswpool = createTpdrSwitchPool(xpdrNetMaps);
                oduSwPoolMap.put(oduswpool.key(), oduswpool);
                return oduSwPoolMap;
            case Mpdr:
            case Switch:
                OduSwitchingPoolsBuilder oduSxPoolBd = new OduSwitchingPoolsBuilder();
                Map<SwitchingPoolLcpKey, SwitchingPoolLcp> swPool = node.getSwitchingPoolLcp();
                for (Map.Entry<SwitchingPoolLcpKey, SwitchingPoolLcp> entry : swPool.entrySet()) {
                    oduSxPoolBd
                        .setSwitchingPoolNumber(entry.getKey().getSwitchingPoolNumber())
                        .setSwitchingPoolType(entry.getValue().getSwitchingPoolType());
                    Map<NonBlockingListKey, NonBlockingList> nblMap = new HashMap<>();
                    for (Entry<org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev240315
                            .switching.pool.lcp.switching.pool.lcp.NonBlockingListKey, org.opendaylight.yang.gen.v1.http
                            .org.opendaylight.transportpce.portmapping.rev240315.switching.pool.lcp.switching.pool.lcp
                            .NonBlockingList> nblentry : entry.getValue().getNonBlockingList().entrySet()) {
                        Uint32 availBW = nblentry.getValue().getAvailableInterconnectBandwidth();
                        // TODO: Right now available BW is not in Device OR model. Correct next line when it will be
                        availBW = availBW == null ? nblentry.getValue().getInterconnectBandwidth() : availBW;
                        NonBlockingListBuilder nblBd = new NonBlockingListBuilder()
                            .setNblNumber(nblentry.getKey().getNblNumber())
                            .setAvailableInterconnectBandwidth(availBW)
                            .setCapableInterconnectBandwidth(nblentry.getValue().getInterconnectBandwidth())
                            .setInterconnectBandwidthUnit(nblentry.getValue().getInterconnectBandwidthUnit());
                        Set<TpId> tpIdList = new HashSet<>();
                        for (String tpid : nblentry.getValue().getLcpList()) {
                            tpIdList.add(new TpId(tpid));
                        }
                        NonBlockingList nbl = nblBd.setTpList(tpIdList).build();
                        nblMap.put(nbl.key(), nbl);
                    }
                    oduSxPoolBd.setNonBlockingList(nblMap);
                    oduswpool = oduSxPoolBd.build();
                    oduSwPoolMap.put(oduswpool.key(), oduswpool);
                }
                return oduSwPoolMap;
            // case Regen:
            // case RegenUni:
            default:
                LOG.warn("Xpdr type {} not supported", xpdrType);
        }
        return null;
    }

    private Map<OwnedNodeEdgePointKey, OwnedNodeEdgePoint> transformSrgToOnep(
                String orNodeId, Map<String, List<Mapping>> mapSrg) {
        LOG.debug("CREATENEP transformSrgToOnep, ListOfMapping {}, of NodeId {} ", mapSrg, orNodeId);
        Map<String, TerminationPoint1> tpMap = new HashMap<>();
        //List<TerminationPoint> tpList = new ArrayList<>();
        for (Map.Entry<String, List<Mapping>> entry : mapSrg.entrySet()) {
            // For each srg node. Loop through the LCPs and create neps and sips for PP
            for (Mapping m : entry.getValue()) {
                String tpId = m.getLogicalConnectionPoint();
                String overlayNodeId = String.join("-", orNodeId, tpId.split("\\-")[0]);
                if (!tpId.contains("PP")) {
                    LOG.info("LCP {} is not an external TP of SRG node", tpId);
                    continue;
                }
                int counter = 50;
                do {
                    var netTP1fromDS = getNetworkTerminationPoint1FromDatastore(overlayNodeId, tpId);
                    if (netTP1fromDS != null) {
                        //tpList.add(netTP1fromDS);
                        tpMap.put(tpId, netTP1fromDS);
                        LOG.debug("LCP {} is not empty for augmentation TP1", tpId);
                        break;
                    }
                    try {
                        Thread.sleep(1);
                    } catch (InterruptedException e) {
                        // TODO Auto-generated catch block
                        LOG.debug("Waiting until PP is backported in Topology, Exception raised", e);
                    }
                    counter--;
                } while (counter > 0);
                if (counter == 0) {
                    LOG.error("CREATENEP transformSrgToOnep, No Tp1 found in topology for LCP {}, of NodeId {} ",
                        tpId, overlayNodeId);
                }
                if (getNetworkTerminationPoint11FromDatastore(overlayNodeId, tpId) == null) {
                    LOG.error("CREATENEP transformSrgToOnep, No Tp11 found in topology for LCP {}, of NodeId {} ",
                        tpId, overlayNodeId);
                } else {
                    LOG.info("LCP {} is not empty for augmentation TP11", tpId);
                }
            }
        }
        LOG.debug("TransformSRGToONep for tps {}, of NodeId {} ",
            tpMap.entrySet().stream().map(tp -> tp.getKey()).collect(Collectors.toList()), orNodeId);
        return populateNepsForRdmNode(orNodeId, tpMap, false, TapiStringConstants.PHTNC_MEDIA_OTS);
    }

    private Map<OwnedNodeEdgePointKey, OwnedNodeEdgePoint> transformDegToOnep(
                String orNodeId, Map<String, List<Mapping>> mapDeg) {
        LOG.debug("CREATENEP transformDegToOnep, ListOfMapping {}, of NodeId {} ", mapDeg, orNodeId);
        Map<OwnedNodeEdgePointKey, OwnedNodeEdgePoint> degOnepMap = new HashMap<>();
        Map<String, TerminationPoint1> tpMap = new HashMap<>();
        //List<TerminationPoint> tpList = new ArrayList<>();
        for (Map.Entry<String, List<Mapping>> entry : mapDeg.entrySet()) {
            // For each degree node. Loop through the LCPs and create neps and sips for TTP
            for (Mapping m:entry.getValue()) {
                String tpId = m.getLogicalConnectionPoint();
                if (!tpId.contains("TTP")) {
                    LOG.info("LCP {} is not an external TP of DEGREE node", tpId);
                    continue;
                }
                String overlayNodeId = String.join("-", orNodeId, tpId.split("\\-")[0]);
                var netTP1fromDS = getNetworkTerminationPoint1FromDatastore(overlayNodeId, tpId);
                if (netTP1fromDS == null) {
                    LOG.error("CREATENEP transformDegToOnep, No Tp found in topology for LCP {}, of NodeId {} ",
                        tpId, overlayNodeId);
                    continue;
                }
                //tpList.add(getNetworkTerminationPointFromDatastore(overlayNodeId, tpId));
                tpMap.put(tpId, netTP1fromDS);
                LOG.info("LCP {} is not empty for augmentation TP1", tpId);
            }
        }
        degOnepMap.putAll(populateNepsForRdmNode(orNodeId, tpMap, false, TapiStringConstants.PHTNC_MEDIA_OTS));
        degOnepMap.putAll(populateNepsForRdmNode(orNodeId, tpMap, false, TapiStringConstants.PHTNC_MEDIA_OMS));
        return degOnepMap;
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
        DataObjectIdentifier<Topology> topologyIID = DataObjectIdentifier.builder(Context.class)
                .augmentation(Context1.class)
                .child(TopologyContext.class)
                .child(Topology.class, new TopologyKey(tapiTopoUuid))
                .build();
        Topology topology = null;
        try {
            Optional<Topology> optTopology =
                    this.networkTransactionService.read(LogicalDatastoreType.OPERATIONAL, topologyIID).get();
            if (optTopology.isEmpty()) {
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
            if (TOPOLOGICAL_MODE.equals("Full")) {
             // Node is in photonic media layer and UUID can be built from nodeId + PHTN_MEDIA
                Uuid nodeUuid = new Uuid(
                    UUID.nameUUIDFromBytes(
                            (String.join("+", nodeId,TapiStringConstants.PHTNC_MEDIA)).getBytes(StandardCharsets.UTF_8))
                        .toString());
                deleteNodeFromTopo(nodeUuid);
            } else {
                LOG.info("Abstracted Topo Mode in TAPI topology Datastore for OR topology representation. Node"
                    + " {} is not represented in the abstraction and will not be deleted", nodeId);
            }
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
        Context context = null;
        try {
            Optional<Context> optContext = this.networkTransactionService.read(
                    LogicalDatastoreType.OPERATIONAL,
                    DataObjectIdentifier.builder(Context.class).build())
                .get();
            if (optContext.isEmpty()) {
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
        if (sips == null) {
            return;
        }
        for (ServiceInterfacePoint sip:sips.values()) {
            if (sip.getName().values().stream().anyMatch(name -> name.getValue().contains(nodeId))) {
                // Update state of services that have this sip as an endpoint and also connections
                updateConnectivityServicesState(sip.getUuid(), nodeId);
                deleteSipFromTopo(sip.getUuid());
            }
        }
    }

    private Node createTapiXpdrNode(Nodes node,
            Map<NameKey, Name> nameMap, Set<LayerProtocolName> layerProtocols,
            String nodeId, Uuid nodeUuid,
            List<Mapping> xpdrClMaps, List<Mapping> xpdrNetMaps,
            XpdrNodeTypes xponderType, Map<OduSwitchingPoolsKey, OduSwitchingPools> oorOduSwPoolMap) {
        if (!layerProtocols.contains(LayerProtocolName.DSR)
                || !layerProtocols.contains(LayerProtocolName.PHOTONICMEDIA)) {
            LOG.error("Undefined LayerProtocolName for {} node {}",
                nameMap.get(nameMap.keySet().iterator().next()).getValueName(),
                nameMap.get(nameMap.keySet().iterator().next()).getValue());
        }
        Map<OwnedNodeEdgePointKey, OwnedNodeEdgePoint> onepl =
            new HashMap<>(createXpdrDsrOduNeps(nodeId, xpdrClMaps, xpdrNetMaps, xponderType));
        Map<NodeRuleGroupKey, NodeRuleGroup> nodeRuleGroupList =
            createNodeRuleGroupForXpdrNode(node,nodeId, oorOduSwPoolMap,onepl);
        onepl.putAll(createXpdrPhtnMdNeps(nodeId, xpdrNetMaps));
        LOG.debug("TapiNetworkModelServiceImpl line 842, total NEP map = {}", onepl);

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
        Node builtNode = new NodeBuilder()
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
        List<PhotonicMediaNodeEdgePointSpec> pmnepspecList = new ArrayList<>();
        for (Map.Entry<OwnedNodeEdgePointKey, OwnedNodeEdgePoint> entry :
                builtNode.getOwnedNodeEdgePoint().entrySet()) {
            LOG.info("TNMSILine886 analyzing NEP {}", entry.getValue().getName());
            if (entry.getValue().getSupportedCepLayerProtocolQualifierInstances().stream()
                        .filter(sclpqi -> sclpqi.getLayerProtocolQualifier().equals(PHOTONICLAYERQUALIFIEROTS.VALUE))
                        .collect(Collectors.toList()).isEmpty()) {
                continue;
            }
            var aug1 = entry.getValue().augmentationOrElseThrow(
                                org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.photonic.media.rev221121
                            .OwnedNodeEdgePoint1.class);
            if (aug1 == null) {
                continue;
            }
            var phMedNepSpec = aug1.getPhotonicMediaNodeEdgePointSpec();
            if (phMedNepSpec != null) {
                pmnepspecList.add(phMedNepSpec);
            }
        }
        LOG.debug("TapiNetworkModelServiceImpl line 902, List of non empty PMNEPSEC is = {}", pmnepspecList);
        return builtNode;
    }

    private Map<OwnedNodeEdgePointKey, OwnedNodeEdgePoint> createXpdrPhtnMdNeps(
            String nodeId, List<Mapping> xpdrNetMaps) {
        Map<OwnedNodeEdgePointKey, OwnedNodeEdgePoint> onepl = new HashMap<>();
        // eNep creation on otsi node
        for (Mapping mapping : xpdrNetMaps) {
            var lcp = mapping.getLogicalConnectionPoint();
            String onedNameVal = String.join("+", nodeId, TapiStringConstants.PHTNC_MEDIA_OTS, lcp);
            Name onedName = new NameBuilder().setValueName("eNodeEdgePoint").setValue(onedNameVal).build();
            var supOpModes = mapping.getSupportedOperationalMode();
            List<String> opModeList = supOpModes == null ? new ArrayList<>() : new ArrayList<>(supOpModes);
            AdministrativeState newAdmState = transformAdminState(mapping.getPortAdminState());
            OperationalState newOprState = transformOperState(mapping.getPortOperState());
            String rate = mapping.getRate();
            LOG.debug("TNSI.line919 : the rate declared in portMapping for LCP {} is {}", lcp, rate);
            List<OwnedNodeEdgePoint> onepList = new ArrayList<>();
            onepList.addAll(createNep(
                nodeId, rate,
                new Uuid(UUID.nameUUIDFromBytes(onedNameVal.getBytes(StandardCharsets.UTF_8)).toString()),
                lcp, Map.of(onedName.key(), onedName),
                LayerProtocolName.PHOTONICMEDIA, LayerProtocolName.PHOTONICMEDIA, true,
                String.join("+", nodeId, TapiStringConstants.PHTNC_MEDIA_OTS),
                new ArrayList<>(mapping.getSupportedInterfaceCapability()), mapping,
                opModeList, newOprState, newAdmState));
        // OTSi_MC Nep creation on otsi node
            String onedNameVal2 = String.join("+", nodeId, TapiStringConstants.OTSI_MC, lcp);
            Name onedName2 = new NameBuilder().setValueName("PhotMedNodeEdgePoint").setValue(onedNameVal2).build();
            onepList.addAll(createNep(
                nodeId, rate,
                new Uuid(UUID.nameUUIDFromBytes(onedNameVal2.getBytes(StandardCharsets.UTF_8)).toString()),
                lcp, Map.of(onedName2.key(), onedName2),
                LayerProtocolName.PHOTONICMEDIA, LayerProtocolName.PHOTONICMEDIA, false,
                String.join("+", nodeId, TapiStringConstants.OTSI_MC),
                new ArrayList<>(mapping.getSupportedInterfaceCapability()), mapping,
                opModeList, newOprState, newAdmState));
            for (OwnedNodeEdgePoint onep : onepList) {
                onepl.put(onep.key(), onep);
            }
        }
        return onepl;
    }

    private Map<OwnedNodeEdgePointKey, OwnedNodeEdgePoint> createXpdrDsrOduNeps(
            String nodeId, List<Mapping> xpdrClMaps, List<Mapping> xpdrNetMaps, XpdrNodeTypes xponderType) {
        Map<OwnedNodeEdgePointKey, OwnedNodeEdgePoint> onepl = new HashMap<>();
        // client nep creation on DSR node
        for (Mapping mapping : xpdrClMaps) {
            var lcp = mapping.getLogicalConnectionPoint();
            String nepvalue = String.join("+", nodeId, TapiStringConstants.DSR, lcp);
            LOG.info("Client NEP = {}", nepvalue);
            Name name = new NameBuilder()
                .setValue(nepvalue)
                .setValueName(OpenroadmNodeType.TPDR.getName().equalsIgnoreCase(xponderType.getName())
                        ? "100G-tpdr" : "NodeEdgePoint_C")
                .build();
            AdministrativeState newAdmState = transformAdminState(mapping.getPortAdminState());
            OperationalState newOprState = transformOperState(mapping.getPortOperState());
            String rate = mapping.getRate();
            LOG.debug("TNSI.line963 : the rate declared in portMapping for LCP {} is {}", lcp, rate);
            List<OwnedNodeEdgePoint> onepList = new ArrayList<>();
            onepList.addAll(createNep(
                nodeId, rate, new Uuid(UUID.nameUUIDFromBytes(nepvalue.getBytes(StandardCharsets.UTF_8)).toString()),
                lcp, Map.of(name.key(), name), LayerProtocolName.DSR, LayerProtocolName.DSR, true,
                String.join("+", nodeId, TapiStringConstants.DSR),
                new ArrayList<>(mapping.getSupportedInterfaceCapability()), mapping, null, newOprState, newAdmState));
        // network nep creation on E_ODU node
            String onedNameVal = String.join("+", nodeId, TapiStringConstants.E_ODU, lcp);
            LOG.info("eODU NEP = {}", onedNameVal);
            Name onedName = new NameBuilder().setValueName("eNodeEdgePoint_N").setValue(onedNameVal).build();
            onepList.addAll(createNep(
                nodeId, rate, new Uuid(UUID.nameUUIDFromBytes(onedNameVal.getBytes(StandardCharsets.UTF_8)).toString()),
                lcp, Map.of(onedName.key(), onedName), LayerProtocolName.ODU, LayerProtocolName.DSR, true,
                String.join("+", nodeId, TapiStringConstants.E_ODU),
                new ArrayList<>(mapping.getSupportedInterfaceCapability()), mapping, null, newOprState, newAdmState));
            for (OwnedNodeEdgePoint onep : onepList) {
                onepl.put(onep.key(), onep);
            }
        }
        // network nep creation on I_ODU node
        for (Mapping mapping : xpdrNetMaps) {
            var lcp = mapping.getLogicalConnectionPoint();
            String onedNameVal = String.join("+", nodeId, TapiStringConstants.I_ODU, lcp);
            LOG.info("iODU NEP = {}", onedNameVal);
            Name onedName = new NameBuilder().setValueName("iNodeEdgePoint_N").setValue(onedNameVal).build();
            String rate = mapping.getRate();
            LOG.debug("TNSI.line990 : the rate declared in portMapping for LCP {} is {}", lcp, rate);
            List<OwnedNodeEdgePoint> onepList = new ArrayList<>();
            onepList.addAll(createNep(
                nodeId, rate, new Uuid(UUID.nameUUIDFromBytes(onedNameVal.getBytes(StandardCharsets.UTF_8)).toString()),
                lcp, Map.of(onedName.key(), onedName), LayerProtocolName.ODU, LayerProtocolName.DSR, true,
                String.join("+", nodeId, TapiStringConstants.I_ODU),
                new ArrayList<>(mapping.getSupportedInterfaceCapability()), mapping,
                null, transformOperState(mapping.getPortOperState()),transformAdminState(mapping.getPortAdminState())));
            for (OwnedNodeEdgePoint onep : onepList) {
                onepl.put(onep.key(), onep);
            }
        }
        // network nep creation on I_OTU node
        for (Mapping mapping : xpdrNetMaps) {
            var lcp = mapping.getLogicalConnectionPoint();
            String onedNameVal = String.join("+", nodeId, TapiStringConstants.I_OTU, lcp);
            LOG.info("iODU NEP = {}", onedNameVal);
            Name onedName = new NameBuilder().setValueName("iNodeEdgePoint_OTU").setValue(onedNameVal).build();
            String rate = mapping.getRate();
            LOG.debug("TNSI.line1009 : the rate declared in portMapping for LCP {} is {}", lcp, rate);
            List<OwnedNodeEdgePoint> onepList = new ArrayList<>();
            onepList.addAll(createNep(
                nodeId, rate, new Uuid(UUID.nameUUIDFromBytes(onedNameVal.getBytes(StandardCharsets.UTF_8)).toString()),
                lcp, Map.of(onedName.key(), onedName), LayerProtocolName.DIGITALOTN, LayerProtocolName.DIGITALOTN, true,
                String.join("+", nodeId, TapiStringConstants.I_OTU),
                new ArrayList<>(mapping.getSupportedInterfaceCapability()), mapping,
                null, transformOperState(mapping.getPortOperState()),transformAdminState(mapping.getPortAdminState())));
            for (OwnedNodeEdgePoint onep : onepList) {
                onepl.put(onep.key(), onep);
            }
        }
        return onepl;
    }

    private OperationalState transformOperState(String operString) {
        return org.opendaylight.transportpce.networkmodel.util.TopologyUtils.setNetworkOperState(operString)
                .equals(State.InService) ? OperationalState.ENABLED : OperationalState.DISABLED;
    }

    private AdministrativeState transformAdminState(String adminString) {
        return org.opendaylight.transportpce.networkmodel.util.TopologyUtils.setNetworkAdminState(adminString)
                .equals(AdminStates.InService) ? AdministrativeState.UNLOCKED : AdministrativeState.LOCKED;
    }

    private List<OwnedNodeEdgePoint> createNep(String nodeId, String nepRate, Uuid nepUuid, String tpid,
            Map<NameKey, Name> nepNames, LayerProtocolName nepProtocol, LayerProtocolName nodeProtocol, boolean withSip,
            String keyword, List<SupportedIfCapability> sicList, Mapping mapping, List<String> opModeList,
            OperationalState operState, AdministrativeState adminState) {
        List<SupportedInterfaceCapability> sicListTemp = new ArrayList<>();
        for (SupportedIfCapability supInterCapa : sicList) {
            sicListTemp.add(new SupportedInterfaceCapabilityBuilder()
                    .withKey(new SupportedInterfaceCapabilityKey(supInterCapa))
                    .setIfCapType(supInterCapa)
                    .build());
        }
        Collection<SupportedInterfaceCapability> sicColl = sicListTemp;
        OwnedNodeEdgePointBuilder onepBldr = new OwnedNodeEdgePointBuilder()
                .setUuid(nepUuid)
                .setLayerProtocolName(nepProtocol)
                .setName(nepNames);

        if (withSip) {
            onepBldr.setMappedServiceInterfacePoint(
                this.tapiFactory.createMSIP(1, nepProtocol, tpid, keyword, sicColl, operState, adminState));
            this.sipMap.putAll(tapiFactory.getTapiSips());
        }
        LOG.debug("Node layer {}", nodeProtocol.getName());
        String key = keyword;
        if (keyword.contains(("ODU"))) {
            key = "ODU";
        } else if (keyword.contains(("OTU"))) {
            key = "OTU";
        }
        LOG.debug("TNMSI-LINE1063 Creating NEP of protocol {} and key {}", nepProtocol, key);
        onepBldr
            .setSupportedCepLayerProtocolQualifierInstances(
                this.tapiFactory.createSupportedCepLayerProtocolQualifier(sicColl, nepProtocol, key))
            .setDirection(Direction.BIDIRECTIONAL)
            .setLinkPortRole(PortRole.SYMMETRIC)
            .setAdministrativeState(adminState).setOperationalState(operState)
            .setLifecycleState(LifecycleState.INSTALLED);
        List<OwnedNodeEdgePoint> onepList = new ArrayList<>();
        String rate = (nepRate == null) ? "0" : nepRate;
        if (!keyword.contains(TapiStringConstants.OTSI_MC) && !keyword.contains(TapiStringConstants.PHTNC_MEDIA_OTS)) {
            if (nepProtocol.equals(LayerProtocolName.DSR)) {
                if (!sicColl.stream()
                        .filter(lp -> lp.getIfCapType().implementedInterface().getSimpleName().contains("GE"))
                        .findFirst().orElseThrow().toString().isEmpty()) {
                    Map<LAYERPROTOCOLQUALIFIER, Uint64> supInt = new HashMap<>();
                    supInt.putAll(ORToTapiTopoConversionFactory.LPN_MAP.get("ETH").get(sicColl.stream()
                        .filter(lp -> lp.getIfCapType().implementedInterface().getSimpleName().contains("GE"))
                        .findFirst().orElseThrow().getIfCapType().implementedInterface().getSimpleName()));
                    onepBldr.setSupportedPayloadStructure(this.tapiFactory.createSupportedPayloadStructureForCommonNeps(
                        false, Double.valueOf(rate), Integer.valueOf(1), supInt.keySet()));
                    if (mapping.getSupportingEthernet() != null && (operState == null
                            || operState.equals(OperationalState.ENABLED))) {
                        onepBldr.setAvailablePayloadStructure(this.tapiFactory
                            .createAvailablePayloadStructureForCommonNeps(
                            true, Double.valueOf(0), Integer.valueOf(0), supInt.keySet()));
                    } else {
                        onepBldr.setAvailablePayloadStructure(
                            this.tapiFactory.createAvailablePayloadStructureForCommonNeps(false, Double.valueOf(rate),
                                Integer.valueOf(1), supInt.keySet()));
                    }
                } else if (!sicColl.stream().filter(lp -> lp.getIfCapType().implementedInterface().getSimpleName()
                        .contains("OTU4")).findFirst().orElseThrow().toString().isEmpty()) {
                    Map<LAYERPROTOCOLQUALIFIER, Uint64> supInt = new HashMap<>();
                    supInt.putAll(ORToTapiTopoConversionFactory.LPN_MAP.get("ETH").get("IfOCH"));
                    onepBldr.setSupportedPayloadStructure(this.tapiFactory.createSupportedPayloadStructureForCommonNeps(
                        false, Double.valueOf(rate), Integer.valueOf(1), supInt.keySet()));
                    if (mapping.getSupportingOtu4() != null  && (operState == null
                        || operState.equals(OperationalState.ENABLED))) {
                        onepBldr.setAvailablePayloadStructure(this.tapiFactory
                            .createAvailablePayloadStructureForCommonNeps(
                            true, Double.valueOf(0), Integer.valueOf(0), supInt.keySet()));
                    } else {
                        onepBldr.setAvailablePayloadStructure(
                            this.tapiFactory.createAvailablePayloadStructureForCommonNeps(false, Double.valueOf(rate),
                                Integer.valueOf(1), supInt.keySet()));
                    }
                } else {
                    onepList.add(onepBldr.build());
                    return onepList;
                }
            } else if ((nepProtocol.equals(LayerProtocolName.ODU) || nepProtocol.equals(LayerProtocolName.DIGITALOTN))
                    && mapping.getPortQual() == "xpdr-network") {
                if (!sicColl.stream()
                        .filter(lp -> lp.getIfCapType().implementedInterface().getSimpleName().contains("ODU4"))
                        .findFirst().toString().isEmpty()) {
                    Map<LAYERPROTOCOLQUALIFIER, Uint64> supInt = new HashMap<>();
                    supInt.putAll(Map.of(ODUTYPEODU4.VALUE, Uint64.valueOf(0)));
                    onepBldr.setSupportedPayloadStructure(this.tapiFactory.createSupportedPayloadStructureForCommonNeps(
                        false, Double.valueOf(100), Integer.valueOf(1), supInt.keySet()));
                    if (mapping.getSupportingOdu4() == null && (operState == null
                        || operState.equals(OperationalState.ENABLED))) {
                        onepBldr.setAvailablePayloadStructure(this.tapiFactory
                            .createAvailablePayloadStructureForCommonNeps(
                            false, Double.valueOf(100), Integer.valueOf(0), supInt.keySet()));
                    } else {
                        onepBldr.setAvailablePayloadStructure(this.tapiFactory
                            .createAvailablePayloadStructureForCommonNeps(false, Double.valueOf(100),
                                Integer.valueOf(1), supInt.keySet()));
                    }
                } else {
                    // this is the case where SicColl does not contain ODU4 and nep Protocol is digital OTN
                    // meaning we create an OTU (OTU4 or OTUCn) Nep
                    Set<LAYERPROTOCOLQUALIFIER> lpqSet = new HashSet<>();
                    if (Integer.parseInt(rate) == 100) {
                        lpqSet.add(OTUTYPEOTU4.VALUE);
                        onepBldr.setAvailablePayloadStructure(this.tapiFactory
                            .createAvailablePayloadStructureForCommonNeps(false, Double.valueOf(100),
                                (mapping.getSupportingOtu4() == null) ? 1 : 0,
                                lpqSet));
                    } else {
                        if (key.equals("OTU")) {
                            lpqSet.add(OTUTYPEOTUCN.VALUE);
                            onepBldr.setAvailablePayloadStructure(this.tapiFactory
                                .createAvailablePayloadStructureForCommonNeps(false, Double.valueOf(rate),
                                    (mapping.getSupportingOtucn() == null) ? 1 : 0,
                                    lpqSet));
                            //Recursive call to create ODUCN NEP just after OTUCN one
                            var lcp = mapping.getLogicalConnectionPoint();
                            String onedNameVal = String.join("+", nodeId, TapiStringConstants.E_ODUCN, lcp);
                            LOG.info("Creating eODUCN NEP = {} recursivly after processing OTUCN NEP", onedNameVal);
                            Name onedName = new NameBuilder().setValueName("eNodeEdgePoint_N").setValue(onedNameVal)
                                .build();
                            onepList.addAll(createNep(nodeId, rate, new Uuid(UUID.nameUUIDFromBytes(
                                    onedNameVal.getBytes(StandardCharsets.UTF_8)).toString()),
                                lcp, Map.of(onedName.key(), onedName), LayerProtocolName.DIGITALOTN,
                                LayerProtocolName.DIGITALOTN, true,
                                String.join("+", nodeId, TapiStringConstants.E_ODUCN),
                                new ArrayList<>(mapping.getSupportedInterfaceCapability()),
                                mapping, null, transformOperState(mapping.getPortOperState()),
                                transformAdminState(mapping.getPortAdminState())));
                        } else {
                            lpqSet.add(ODUTYPEODUCN.VALUE);
                            onepBldr.setAvailablePayloadStructure(this.tapiFactory
                                .createAvailablePayloadStructureForCommonNeps(false, Double.valueOf(rate),
                                    (mapping.getSupportingOducn() == null) ? 1 : 0,
                                    lpqSet));
                        }
                    }
                    onepBldr.setSupportedPayloadStructure(this.tapiFactory.createSupportedPayloadStructureForCommonNeps(
                        false, Double.valueOf(rate), 1, lpqSet));
                }

            }
            onepList.add(onepBldr.build());
            return onepList;
        }
        // Case handled in following lines is OTSiMC/OTS, requiring operational mode information
        Boolean otucnDetected = false;
        List<OperationalModeKey> keyedOpModeList = new ArrayList<>();
        if (opModeList == null || opModeList.isEmpty()) {
            for (SupportedInterfaceCapability sic : sicColl) {
                switch (sic.getIfCapType().toString().split("\\{")[0]) {
                    case "IfOCHOTUCnODUCn":
                    case "IfOCHOTUCnODUCnUniregen":
                    case "IfOCHOTUCnODUCnRegen":
                        otucnDetected = true;
                        if (rate.contains("200")) {
                            keyedOpModeList.add(new OperationalModeKey("200G"));
                            LOG.warn(TopologyUtils.NOOPMODEDECLARED + "200G rate available", tpid);
                            rate = "200";
                        } else if (rate.contains("300")) {
                            keyedOpModeList.add(new OperationalModeKey("300G"));
                            LOG.warn(TopologyUtils.NOOPMODEDECLARED + "300G rate available", tpid);
                            rate = "300";
                        } else if (rate.contains("400")) {
                            keyedOpModeList.add(new OperationalModeKey("400G"));
                            rate = "400";
                            LOG.warn(TopologyUtils.NOOPMODEDECLARED + "400G rate available", tpid);
                        } else if (rate.contains("600")) {
                            keyedOpModeList.add(new OperationalModeKey("600G"));
                            rate = "600";
                            LOG.warn(TopologyUtils.NOOPMODEDECLARED + "600G rate available", tpid);
                        } else if (rate.contains("800")) {
                            keyedOpModeList.add(new OperationalModeKey("800G"));
                            LOG.warn(TopologyUtils.NOOPMODEDECLARED + "800G rate available", tpid);
                            rate = "800";
                        } else {
                            keyedOpModeList.add(new OperationalModeKey("400G"));
                            LOG.warn(TopologyUtils.NOOPMODEDECLARED + " The rate is undefined, assumes "
                                + "400G rate available", tpid);
                            rate = "400";
                        }
                        break;
                    default:
                        continue;
                }
                break;
            }
            keyedOpModeList.add(new OperationalModeKey("100G"));
            LOG.warn(TopologyUtils.NOOPMODEDECLARED + "100G rate available", tpid);
            if (!otucnDetected) {
                rate = "100";
            }
        } else {
            for (String opMode : opModeList) {
                keyedOpModeList.add(new OperationalModeKey(opMode));
            }
        }
        Map<SinkProfileKey, SinkProfile> sinkProfile = new HashMap<>();
        Map<SourceProfileKey, SourceProfile> sourceProfile = new HashMap<>();
        Map<ProfileKey, Profile> profile = new HashMap<>();
        Map<org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.tapi.context.ProfileKey,
            org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.tapi.context.Profile>
            profileMap = new HashMap<>();
        for (OperationalModeKey opMode : keyedOpModeList) {
            Uuid opModeUuid = new Uuid(UUID.nameUUIDFromBytes(opMode.toString().getBytes(StandardCharsets.UTF_8))
                .toString());
            SinkProfile sinkPf = new SinkProfileBuilder()
                .setProfileUuid(opModeUuid).build();
            sinkProfile.put(sinkPf.key(), sinkPf);
            Profile prof = new ProfileBuilder()
                .setProfileUuid(opModeUuid).build();
            profile.put(prof.key(), prof);
            SourceProfile srcPf = new SourceProfileBuilder()
                .setProfileUuid(opModeUuid)
                .build();
            sourceProfile.put(srcPf.key(), srcPf);
            Name opModeName = new NameBuilder().setValueName("operational-mode-name").setValue(opMode.toString())
                .build();
            org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.tapi.context.Profile contextProfile
                    = new org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.tapi.context
                    .ProfileBuilder()
                .setUuid(opModeUuid)
                .setName(Map.of(opModeName.key(), opModeName))
                .build();
            profileMap.put(contextProfile.key(), contextProfile);
        }
        mergeProfileInTapiContext(profileMap);
        Map<Double, Double> freqWidthMap = new HashMap<>();
        if (getNetworkTerminationPointFromDatastore(nodeId, tpid) == null) {
            LOG.error("CREATENEP, No Tp found in topology for LCP {}, of NodeId {} ", tpid, nodeId);
        } else {
            freqWidthMap = tapiFactory.getXpdrUsedWavelength(getNetworkTerminationPointFromDatastore(nodeId, tpid));
        }
        OwnedNodeEdgePoint onep = tapiFactory.addPayloadStructureAndPhotSpecToOnep(
                nodeId, rate, freqWidthMap, keyedOpModeList, sicColl, onepBldr, keyword)
            .setProfile(profile)
            .setSinkProfile(sinkProfile)
            .setSourceProfile(sourceProfile)
            .build();
        LOG.debug("TapiNetworkServiceImpl line1274, onep = {}", onep);
        onepList.add(onep);
        return onepList;
    }

    private Node createRoadmTapiNode(String orNodeId, Map<OwnedNodeEdgePointKey, OwnedNodeEdgePoint> onepMap) {
        // UUID and Node Names
        Uuid nodeUuid;
        Name nodeNames;
        if (orNodeId.equals("ROADMINFRA")) {
            nodeUuid = new Uuid(
                UUID.nameUUIDFromBytes(TapiStringConstants.RDM_INFRA.getBytes(Charset.forName("UTF-8")))
                    .toString());
            nodeNames =
                new NameBuilder().setValueName("roadm node name").setValue(TapiStringConstants.RDM_INFRA).build();
        } else {
            String nodeNamesVal = String.join("+", orNodeId, TapiStringConstants.PHTNC_MEDIA);
            nodeUuid = new Uuid(
                UUID.nameUUIDFromBytes(nodeNamesVal.getBytes(StandardCharsets.UTF_8))
                    .toString());
            nodeNames = new NameBuilder().setValueName("roadm node name").setValue(nodeNamesVal).build();
        }
        Name nameNodeType =
            new NameBuilder().setValueName("Node Type").setValue(OpenroadmNodeType.ROADM.getName()).build();
        // Protocol Layer
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
        Map<NodeRuleGroupKey, NodeRuleGroup> nodeRuleGroupMap
            = tapiFactory.createAllNodeRuleGroupForRdmNode(TOPOLOGICAL_MODE, nodeUuid, orNodeId, onepMap.values());
        return new NodeBuilder()
            .setUuid(nodeUuid)
            .setName(Map.of(nodeNames.key(), nodeNames, nameNodeType.key(), nameNodeType))
            .setLayerProtocolName(Set.of(LayerProtocolName.PHOTONICMEDIA))
            .setAdministrativeState(AdministrativeState.UNLOCKED)
            .setOperationalState(OperationalState.ENABLED)
            .setLifecycleState(LifecycleState.INSTALLED)
            .setOwnedNodeEdgePoint(onepMap)
            .setNodeRuleGroup(nodeRuleGroupMap)
            .setInterRuleGroup(
                tapiFactory.createInterRuleGroupForRdmNode(TOPOLOGICAL_MODE, nodeUuid, orNodeId,
                    nodeRuleGroupMap.entrySet().stream().map(e -> e.getKey()).collect(Collectors.toList())))
            .setCostCharacteristic(Map.of(costCharacteristic.key(), costCharacteristic))
            .setLatencyCharacteristic(Map.of(latencyCharacteristic.key(), latencyCharacteristic))
            .setErrorCharacteristic("error")
            .setLossCharacteristic("loss")
            .setRepeatDeliveryCharacteristic("repeat delivery")
            .setDeliveryOrderCharacteristic("delivery order")
            .setUnavailableTimeCharacteristic("unavailable time")
            .setServerIntegrityProcessCharacteristic("server integrity process")
            .setRiskParameterPac(
                new RiskParameterPacBuilder()
                    .setRiskCharacteristic(Map.of(riskCharacteristic.key(), riskCharacteristic))
                    .build())
            .build();
    }

    private OduSwitchingPools createTpdrSwitchPool(List<Mapping> xpdrNetMaps) {
        Map<NonBlockingListKey, NonBlockingList> nblMap = new HashMap<>();
        for (int i = 1; i <= xpdrNetMaps.size(); i++) {
            String netLCP = xpdrNetMaps.get(i - 1).getLogicalConnectionPoint();
            String netAssoLCP = xpdrNetMaps.get(i - 1).getConnectionMapLcp();
            LOG.info("XPDr net LCP = {}", netLCP);
            LOG.info("XPDr net associated LCP = {}", netAssoLCP);
            String lcpRate = (xpdrNetMaps.get(i - 1).getRate() == null) ? "0" : xpdrNetMaps.get(i - 1).getRate();
            NonBlockingList nbl = new NonBlockingListBuilder()
                .setNblNumber(Uint16.valueOf(i))
                .setCapableInterconnectBandwidth(Uint32.valueOf(lcpRate))
                .setAvailableInterconnectBandwidth(Uint32.valueOf(lcpRate))
                .setInterconnectBandwidthUnit(Uint32.valueOf(1000000000))
                .setTpList(new HashSet<>(Set.of(new TpId(netLCP), new TpId(netAssoLCP))))
                .build();
            nblMap.put(nbl.key(), nbl);
        }
        return new OduSwitchingPoolsBuilder()
            .setNonBlockingList(nblMap)
            .setSwitchingPoolNumber(Uint16.valueOf(1))
            .setSwitchingPoolType(SwitchingPoolTypes.forValue(1))
            .build();
    }

    private Map<NodeRuleGroupKey, NodeRuleGroup> createNodeRuleGroupForXpdrNode(Nodes node, String nodeId,
            Map<OduSwitchingPoolsKey, OduSwitchingPools> oorOduSwitchingPool,
            Map<OwnedNodeEdgePointKey, OwnedNodeEdgePoint> onepl) {
        // create NodeRuleGroup
        if (oorOduSwitchingPool == null) {
            LOG.info("No switching pool created for node = {}", node.getNodeId());
            return new HashMap<>();
        }
        LOG.debug("TNMSIline 1372 CreateNodeRuleGroupForXpdrNode, ONEPL = {}", onepl.values());
        Map<NodeRuleGroupKey, NodeRuleGroup> nodeRuleGroupMap = new HashMap<>();
        Uuid nodeUuid = new Uuid(
            UUID.nameUUIDFromBytes((String.join("+", nodeId, TapiStringConstants.XPDR))
                .getBytes(StandardCharsets.UTF_8)).toString());
        int count = 1;
        Rule rule;
        for (Map.Entry<OduSwitchingPoolsKey, OduSwitchingPools> oduSwPool : oorOduSwitchingPool.entrySet()) {
            if (oduSwPool.getValue().getSwitchingPoolType().equals(SwitchingPoolTypes.NonBlocking)) {
                rule = new RuleBuilder()
                    .setLocalId("forward" + count)
                    .setForwardingRule(FORWARDINGRULEMAYFORWARDACROSSGROUP.VALUE)
                    .setRuleType(new HashSet<>(Set.of(RuleType.FORWARDING)))
                    .build();
            } else {
                rule = new RuleBuilder()
                    .setLocalId("non-forward" + count)
                    .setForwardingRule(FORWARDINGRULECANNOTFORWARDACROSSGROUP.VALUE)
                    .setRuleType(new HashSet<>(Set.of(RuleType.FORWARDING)))
                    .build();
            }
            Map<RuleKey, Rule> ruleMap = new HashMap<>(Map.of(rule.key(), rule));
            for (Map.Entry<NonBlockingListKey, NonBlockingList> nblEntry :
                    oduSwPool.getValue().getNonBlockingList().entrySet()) {
                LOG.debug("TNMSIline 1396 CreateNodeRuleGroupForXpdrNode, Non blocking list = {}", nblEntry.getValue());
                Map<NodeEdgePointKey, NodeEdgePoint> nepList = new HashMap<>();
                for (TpId tp : nblEntry.getValue().getTpList()) {
                    String tpUuidSd;
                    Uuid tpUuid;
                    LOG.debug("TNMSIline1401 CreateNodeRuleGroupForXpdrNode, tp = {}", tp.getValue());
                    LOG.debug("TNMSIline1402 CreateNodeRuleGroupForXpdrNode, Mapping List of LCP = {}",
                        node.getMapping().entrySet().stream().map(Map.Entry::getKey).collect(Collectors.toList()));
                    switch (node.getMapping().entrySet().stream()
                            .filter(lcp -> lcp.getKey().equals(new MappingKey(tp.getValue())))
                            .collect(Collectors.toList()).iterator().next().getValue().getPortQual()) {
                        case "xpdr-client":
                        case "switch-client":
                            tpUuidSd = String.join("+", nodeId, TapiStringConstants.E_ODU, tp.getValue());
                            LOG.debug("TNMSIline 1410 CreateNodeRuleGroupForXpdrNode, EDOU TP {} added with Uuid {}",
                                tp, tpUuidSd);
                            tpUuid = new Uuid(UUID.nameUUIDFromBytes(tpUuidSd.getBytes(StandardCharsets.UTF_8))
                                .toString());
                            if (onepl.containsKey(new OwnedNodeEdgePointKey(tpUuid))) {
                                NodeEdgePoint nep = new NodeEdgePointBuilder()
                                    .setTopologyUuid(this.tapiTopoUuid)
                                    .setNodeUuid(nodeUuid)
                                    .setNodeEdgePointUuid(tpUuid)
                                    .build();
                                nepList.put(nep.key(), nep);
                            }
                            break;
                        case "xpdr-network":
                        case "switch-network":
                            tpUuidSd = String.join("+", nodeId, TapiStringConstants.I_ODU, tp.getValue());
                            LOG.debug("TNMSIline 1426 CreateNodeRuleGroupForXpdrNode, IDOU TP {} added with Uuid {}",
                                tp, tpUuidSd);
                            tpUuid = new Uuid(UUID.nameUUIDFromBytes(tpUuidSd.getBytes(StandardCharsets.UTF_8))
                                .toString());
                            if (onepl.containsKey(new OwnedNodeEdgePointKey(tpUuid))) {
                                NodeEdgePoint nep = new NodeEdgePointBuilder()
                                    .setTopologyUuid(this.tapiTopoUuid)
                                    .setNodeUuid(nodeUuid)
                                    .setNodeEdgePointUuid(tpUuid)
                                    .build();
                                nepList.put(nep.key(), nep);
                            }
                            break;

                        default:
                            LOG.error("CreateNodeRuleGroupForXpdrNode, Unrecognized tpType for Node"
                                + " {}, tp {}, processign OduSwitchingPool of portMapping", nodeId, tp);
                    }

                }
                // Empty random creation of mandatory fields for avoiding errors....
                TotalSize potentialTs = new TotalSizeBuilder()
                    .setValue(Decimal64.valueOf((nblEntry.getValue().getCapableInterconnectBandwidth().doubleValue()
                        * nblEntry.getValue().getInterconnectBandwidthUnit().doubleValue() / 1000000000),
                        RoundingMode.DOWN))
                    .setUnit(CAPACITYUNITGBPS.VALUE)
                    .build();
                TotalPotentialCapacity tpc = new TotalPotentialCapacityBuilder()
                    .setTotalSize(potentialTs).build();
                Uint32 availBW = nblEntry.getValue().getAvailableInterconnectBandwidth();
                // TODO: Right now available BW is not in Device OR model. Correct next line when it will be
                availBW = availBW == null ? nblEntry.getValue().getCapableInterconnectBandwidth() : availBW;
                TotalSize availableTs = new TotalSizeBuilder()
                    .setValue(Decimal64.valueOf((availBW.doubleValue()
                        * nblEntry.getValue().getInterconnectBandwidthUnit().doubleValue() / 1000000000),
                        RoundingMode.DOWN))
                    .setUnit(CAPACITYUNITGBPS.VALUE)
                    .build();
                AvailableCapacity avc = new AvailableCapacityBuilder()
                    .setTotalSize(availableTs).build();

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
                        UUID.nameUUIDFromBytes(("odu node rule group " + count).getBytes(StandardCharsets.UTF_8))
                            .toString()))
                    .setRule(ruleMap)
                    .setNodeEdgePoint(nepList)
                    .setRiskCharacteristic(Map.of(riskCharacteristic.key(), riskCharacteristic))
                    .setCostCharacteristic(Map.of(costCharacteristic.key(), costCharacteristic))
                    .setLatencyCharacteristic(Map.of(latencyCharacteristic.key(), latencyCharacteristic))
                    .setAvailableCapacity(avc)
                    .setTotalPotentialCapacity(tpc)
                    .build();
                nodeRuleGroupMap.put(nodeRuleGroup.key(), nodeRuleGroup);
                count++;
            }
        }
        return nodeRuleGroupMap;
    }

    private String getNodeType(XpdrNodeTypes xponderType) {
        switch (xponderType) {
            case Tpdr:
                return OpenroadmNodeType.TPDR.getName();
            case Mpdr:
                return OpenroadmNodeType.MUXPDR.getName();
            case Switch:
                return OpenroadmNodeType.SWITCH.getName();
            //case Regen:
            //case RegenUni:
            default:
                LOG.info("XpdrType {} not supported", xponderType);
                return null;
        }
    }

    private void mergeNodeinTopology(Map<NodeKey, Node> nodeMap) {
        // TODO is this merge correct? Should we just merge topology by changing the nodes map??
        // TODO: verify this is correct. Should we identify the context IID with the context UUID??
        LOG.info("Creating tapi node in TAPI topology context");
        // merge in datastore
        this.networkTransactionService.merge(
            LogicalDatastoreType.OPERATIONAL,
            DataObjectIdentifier.builder(Context.class)
                .augmentation(Context1.class)
                .child(TopologyContext.class)
                .child(Topology.class, new TopologyKey(this.tapiTopoUuid))
                .build(),
            new TopologyBuilder().setUuid(this.tapiTopoUuid).setNode(nodeMap).build());
        try {
            this.networkTransactionService.commit().get();
        } catch (InterruptedException | ExecutionException e) {
            LOG.error("Error populating TAPI topology: ", e);
        }
        LOG.info("Node added succesfully.");
    }


    private void mergeProfileInTapiContext(
            Map<org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.tapi.context.ProfileKey,
            org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.tapi.context.Profile> profileMap) {

        LOG.info("Registering Profiles in TAPI  context");
        // merge in datastore
        try {
            this.networkTransactionService.merge(
                LogicalDatastoreType.OPERATIONAL,
                DataObjectIdentifier.builder(Context.class).build(),
                new ContextBuilder().setProfile(profileMap).build());
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
        // merge in datastore
        this.networkTransactionService.merge(
            LogicalDatastoreType.OPERATIONAL,
            DataObjectIdentifier.builder(Context.class)
                .augmentation(Context1.class)
                .child(TopologyContext.class)
                .child(Topology.class, new TopologyKey(this.tapiTopoUuid))
                .build(),
            new TopologyBuilder().setUuid(this.tapiTopoUuid).setLink(linkMap).build());
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
            // merge in datastore
            this.networkTransactionService.merge(
                LogicalDatastoreType.OPERATIONAL,
                DataObjectIdentifier.builder(Context.class).build(),
                new ContextBuilder().setServiceInterfacePoint(sips).build());
            this.networkTransactionService.commit().get();
            LOG.info("TAPI SIPs merged successfully.");
        } catch (InterruptedException | ExecutionException e) {
            LOG.error("Failed to merge TAPI Sips", e);
        }
    }

    private void deleteLinkFromTopo(Uuid linkUuid) {
        try {
            this.networkTransactionService.delete(
                LogicalDatastoreType.OPERATIONAL,
                // TODO: check if this IID is correct
                DataObjectIdentifier.builder(Context.class)
                    .augmentation(Context1.class)
                    .child(TopologyContext.class)
                    .child(Topology.class, new TopologyKey(this.tapiTopoUuid))
                    .child(Link.class, new LinkKey(linkUuid)).build());
            this.networkTransactionService.commit().get();
            LOG.info("TAPI link deleted successfully.");
        } catch (InterruptedException | ExecutionException e) {
            LOG.error("Failed to delete TAPI link", e);
        }
    }

    private void deleteNodeFromTopo(Uuid nodeUuid) {
        try {
            this.networkTransactionService.delete(
                LogicalDatastoreType.OPERATIONAL,
                // TODO: check if this IID is correct
                DataObjectIdentifier.builder(Context.class)
                    .augmentation(Context1.class)
                    .child(TopologyContext.class)
                    .child(Topology.class, new TopologyKey(this.tapiTopoUuid))
                    .child(Node.class, new NodeKey(nodeUuid)).build());
            this.networkTransactionService.commit().get();
            LOG.info("TAPI Node deleted successfully.");
        } catch (InterruptedException | ExecutionException e) {
            LOG.error("Failed to delete TAPI Node", e);
        }
    }

    private void deleteSipFromTopo(Uuid sipUuid) {
        // TODO: check if this IID is correct
        try {
            this.networkTransactionService.delete(
                LogicalDatastoreType.OPERATIONAL,
                DataObjectIdentifier.builder(Context.class)
                    .child(ServiceInterfacePoint.class, new ServiceInterfacePointKey(sipUuid))
                    .build());
            this.networkTransactionService.commit().get();
            LOG.info("TAPI SIP deleted successfully.");
        } catch (InterruptedException | ExecutionException e) {
            LOG.error("Failed to delete TAPI SIP", e);
        }
    }

    private void updateConnectivityServicesState(Uuid sipUuid, String nodeId) {
        // TODO: check if this IID is correct
        ConnectivityContext connContext = null;
        try {
            Optional<ConnectivityContext> optConnContext =
                this.networkTransactionService.read(
                        LogicalDatastoreType.OPERATIONAL,
                        DataObjectIdentifier.builder(Context.class)
                            .augmentation(org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev221121
                                .Context1.class)
                            .child(ConnectivityContext.class)
                            .build())
                    .get();
            if (optConnContext.isEmpty()) {
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
            for (ConnectivityService service : connServMap.values()) {
                Map<EndPointKey, EndPoint> serviceEndPoints = service.getEndPoint();
                if (serviceEndPoints.values().stream()
                            .anyMatch(endPoint -> endPoint.getServiceInterfacePoint().getServiceInterfacePointUuid()
                        .equals(sipUuid))) {
                    LOG.info("Service using SIP of node {} identified. Update state of service", nodeId);
                    updateConnectivityService(
                        new ConnectivityServiceBuilder(service)
                            .setAdministrativeState(AdministrativeState.LOCKED)
                            .setOperationalState(OperationalState.DISABLED)
                            .setLifecycleState(LifecycleState.PENDINGREMOVAL)
                            .build());
                }
            }
        }
        // Update state of connections
        if (connMap == null) {
            return;
        }
        for (Connection connection:connMap.values()) {
            if (connection.getName().values().stream().anyMatch(name -> name.getValue().contains(nodeId))) {
                updateConnection(
                    new ConnectionBuilder(connection)
                        .setLifecycleState(LifecycleState.PENDINGREMOVAL)
                        .setOperationalState(OperationalState.DISABLED)
                        .build());
            }
        }
    }

    private void updateConnection(Connection updConn) {
        this.networkTransactionService.merge(
            LogicalDatastoreType.OPERATIONAL,
            // TODO: check if this IID is correct
            DataObjectIdentifier.builder(Context.class)
                .augmentation(org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev221121.Context1.class)
                .child(ConnectivityContext.class)
                .child(Connection.class, new ConnectionKey(updConn.getUuid()))
                .build(),
            updConn);
        try {
            this.networkTransactionService.commit().get();
        } catch (InterruptedException | ExecutionException e) {
            LOG.error("Error committing into datastore", e);
        }
    }

    private void updateConnectivityService(ConnectivityService updService) {
        this.networkTransactionService.merge(
                LogicalDatastoreType.OPERATIONAL,
                // TODO: check if this IID is correct
                DataObjectIdentifier.builder(Context.class)
                    .augmentation(org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev221121
                        .Context1.class)
                    .child(ConnectivityContext.class)
                    .child(ConnectivityService.class, new ConnectivityServiceKey(updService.getUuid()))
                    .build(),
                updService);
        try {
            this.networkTransactionService.commit().get();
        } catch (InterruptedException | ExecutionException e) {
            LOG.error("Error committing into datastore", e);
        }
    }

    private Map<OwnedNodeEdgePointKey, OwnedNodeEdgePoint> populateNepsForRdmNode(
            String nodeId, Map<String, TerminationPoint1> tpMap, boolean withSip, String nepPhotonicSublayer) {
        // create neps for MC and and Photonic Media OTS/OMS
        Map<OwnedNodeEdgePointKey, OwnedNodeEdgePoint> onepMap = new HashMap<>();
        for (Map.Entry<String, TerminationPoint1> entry : tpMap.entrySet()) {
            // Admin and oper state common for all tps
            // PHOTONIC MEDIA nep
            String nepNameValue = String.join("+", nodeId, nepPhotonicSublayer, entry.getKey());
            LOG.debug("PHOTO NEP = {}", nepNameValue);
            SupportedCepLayerProtocolQualifierInstancesBuilder sclpqiBd =
                new SupportedCepLayerProtocolQualifierInstancesBuilder().setNumberOfCepInstances(Uint64.valueOf(1));
            switch (nepPhotonicSublayer) {
                case TapiStringConstants.PHTNC_MEDIA_OMS:
                    sclpqiBd.setLayerProtocolQualifier(PHOTONICLAYERQUALIFIEROMS.VALUE);
                    break;
                case TapiStringConstants.PHTNC_MEDIA_OTS:
                    sclpqiBd.setLayerProtocolQualifier(PHOTONICLAYERQUALIFIEROTS.VALUE);
                    break;
                case TapiStringConstants.MC:
                    sclpqiBd.setLayerProtocolQualifier(PHOTONICLAYERQUALIFIERMC.VALUE);
                    break;
                case TapiStringConstants.OTSI_MC:
                    sclpqiBd.setLayerProtocolQualifier(PHOTONICLAYERQUALIFIEROTSiMC.VALUE);
                    break;
                default:
                    break;
            }
            List<SupportedCepLayerProtocolQualifierInstances> sclpqiList = new ArrayList<>(List.of(sclpqiBd.build()));
            OwnedNodeEdgePointBuilder onepBd = new OwnedNodeEdgePointBuilder();
            if (!nepPhotonicSublayer.equals(TapiStringConstants.MC)
                    && !nepPhotonicSublayer.equals(TapiStringConstants.OTSI_MC)) {
                Map<Double,Double> usedFreqMap = new HashMap<>();
                Map<Double,Double> availableFreqMap = new HashMap<>();
                switch (entry.getValue().getTpType()) {
                    // Whatever is the TP and its type we consider that it is handled in a bidirectional way :
                    // same wavelength(s) used in both direction.
                    case SRGRXPP:
                    case SRGTXPP:
                    case SRGTXRXPP:
                        usedFreqMap = tapiFactory.getPP11UsedWavelength(
                            getNetworkTerminationPoint11FromDatastore(nodeId, entry.getKey()));
                        if (usedFreqMap == null || usedFreqMap.isEmpty()) {
                            availableFreqMap.put(GridConstant.START_EDGE_FREQUENCY * 1E12,
                                GridConstant.START_EDGE_FREQUENCY * 1E12
                                + GridConstant.GRANULARITY * GridConstant.EFFECTIVE_BITS * 1E09);
                        } else {
                            LOG.debug("EnteringLOOPcreateOTSiMC & MC with usedFreqMap non empty {} for Node {}, tp {}",
                                usedFreqMap, nodeId, tpMap);
                            onepMap.putAll(populateNepsForRdmNode(nodeId,
                                new HashMap<>(Map.of(entry.getKey(), entry.getValue())),
                                true, TapiStringConstants.MC));
                            onepMap.putAll(populateNepsForRdmNode(nodeId,
                                new HashMap<>(Map.of(entry.getKey(), entry.getValue())),
                                true, TapiStringConstants.OTSI_MC));
                        }
                        break;
                    case DEGREERXTTP:
                    case DEGREETXTTP:
                    case DEGREETXRXTTP:
                        usedFreqMap = tapiFactory.getTTP11UsedFreqMap(
                            getNetworkTerminationPoint11FromDatastore(nodeId, entry.getKey()));
                        availableFreqMap = tapiFactory.getTTP11AvailableFreqMap(
                            getNetworkTerminationPoint11FromDatastore(nodeId, entry.getKey()));
                        break;
                    default:
                        break;
                }
                LOG.debug("calling add Photonic NEP spec for Roadm");
                onepBd = tapiFactory.addPhotSpecToRoadmOnep(nodeId, usedFreqMap, availableFreqMap, onepBd,
                    String.join("+", nodeId, nepPhotonicSublayer));
            }
            Name nepName =
                new NameBuilder().setValueName(nepPhotonicSublayer + "NodeEdgePoint").setValue(nepNameValue).build();
            OwnedNodeEdgePoint onep = onepBd
                .setUuid(new Uuid(UUID.nameUUIDFromBytes(nepNameValue.getBytes(Charset.forName("UTF-8"))).toString()))
                .setLayerProtocolName(LayerProtocolName.PHOTONICMEDIA)
                .setName(Map.of(nepName.key(), nepName))
                .setSupportedCepLayerProtocolQualifierInstances(sclpqiList)
                .setDirection(Direction.BIDIRECTIONAL)
                .setLinkPortRole(PortRole.SYMMETRIC)
                .setAdministrativeState(
                    this.tapiLink.setTapiAdminState(entry.getValue().getAdministrativeState().getName()))
                .setOperationalState(
                    this.tapiLink.setTapiOperationalState(entry.getValue().getOperationalState().getName()))
                .setLifecycleState(LifecycleState.INSTALLED)
                .build();
            LOG.debug("ROADMNEPPopulation TapiNetworkModelServiceImpl populate NEP {} for Node {}",
                onep.getName().entrySet(), nodeId);
            onepMap.put(onep.key(), onep);
        }
        LOG.debug("ROADMNEPPopulation FINISH for Node {}", nodeId);
        return onepMap;
    }

    /**
     * Get a network termination point for nodeId and tpId.
     * @param nodeId String
     * @param tpId String
     * @return network termination point, null otherwise
     */
    private TerminationPoint getNetworkTerminationPointFromDatastore(String nodeId, String tpId) {
        DataObjectIdentifier<TerminationPoint> tpIID = DataObjectIdentifier.builder(Networks.class)
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
        try {
            Optional<TerminationPoint> tpOptional =
                networkTransactionService.read(LogicalDatastoreType.CONFIGURATION, tpIID).get();
            if (tpOptional.isEmpty()) {
                LOG.debug("readMdSal: Error reading tp {} , empty list",tpIID);
                return null;
            }
            LOG.debug("SUCCES getting LCP TP for NodeId {} TpId {} while creating NEP in TapiNetworkModelServiceImpl",
                nodeId, tpId);
            LOG.debug(" The Tp in Datastore is as follows {}", tpOptional);
            return tpOptional.orElseThrow();
        } catch (ExecutionException | InterruptedException e) {
            LOG.warn("Exception while getting termination {} for node id {} point from {} topology",
                    tpId, nodeId, NetworkUtils.OVERLAY_NETWORK_ID, e);
            return null;
        }
    }

    /**
     * Get a network termination point with Common TerminationPoint1 augmentation for nodeId and tpId.
     * @param nodeId String
     * @param tpId String
     * @return network termination point, null otherwise
     */
    private TerminationPoint1 getNetworkTerminationPoint1FromDatastore(String nodeId, String tpId) {
        DataObjectIdentifier<TerminationPoint1> tpIID = DataObjectIdentifier.builder(Networks.class)
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
            .augmentation(org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev230526
                .TerminationPoint1.class)
            .build();
        try {
            Optional<TerminationPoint1> tpOptional =
                networkTransactionService.read(LogicalDatastoreType.CONFIGURATION, tpIID).get();
            if (tpOptional.isEmpty()) {
                LOG.debug("readMdSal: Error reading tp {} , empty list",tpIID);
                return null;
            }
            LOG.debug("SUCCES getting LCP TP1 for NodeId {} TpId {} while creating NEP in TapiNetworkModelServiceImpl",
                nodeId, tpId);
            LOG.debug(" The Tp in Datastore is as follows {}", tpOptional);
            return tpOptional.orElseThrow();
        } catch (ExecutionException | InterruptedException e) {
            LOG.warn("Exception while getting termination {} for node id {} point from {} topology",
                    tpId, nodeId, NetworkUtils.OVERLAY_NETWORK_ID, e);
            return null;
        }
    }

    private org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev230526
            .TerminationPoint1 getNetworkTerminationPoint11FromDatastore(String nodeId, String tpId) {
        DataObjectIdentifier<org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev230526
                .TerminationPoint1> tpIID = DataObjectIdentifier.builder(Networks.class)
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
            .augmentation(
                org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev230526.TerminationPoint1.class)
            .build();
        try {
            Optional<org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev230526.TerminationPoint1>
                tpOptional = networkTransactionService.read(LogicalDatastoreType.CONFIGURATION, tpIID).get();
            if (tpOptional.isEmpty()) {
                LOG.debug("readMdSal: Error reading tp {} , empty list",tpIID);
                return null;
            }
            LOG.debug("SUCCESS getting LCP TP11 for NodeId {} TpId {} while creating NEP in TapiNetworkModelServiceImpl"
                + " The Tp in Datastore is as follows {}", nodeId, tpId, tpOptional);
            return tpOptional.orElseThrow();
        } catch (ExecutionException | InterruptedException e) {
            LOG.warn("Exception while getting termination {} for node id {} point from {} topology",
                    tpId, nodeId, NetworkUtils.OVERLAY_NETWORK_ID, e);
            return null;
        }
    }

}
