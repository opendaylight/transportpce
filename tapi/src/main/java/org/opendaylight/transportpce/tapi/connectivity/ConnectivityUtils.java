/*
 * Copyright © 2018 Orange & 2021 Nokia, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.tapi.connectivity;

import java.math.BigDecimal;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.transportpce.common.network.NetworkTransactionService;
import org.opendaylight.transportpce.servicehandler.service.ServiceDataStoreOperations;
import org.opendaylight.transportpce.tapi.TapiStringConstants;
import org.opendaylight.transportpce.tapi.utils.GenericServiceEndpoint;
import org.opendaylight.transportpce.tapi.utils.ServiceEndpointType;
import org.opendaylight.transportpce.tapi.utils.TapiContext;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev231221.Network;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev231221.mapping.Mapping;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev231221.mapping.MappingKey;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev231221.network.Nodes;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev231221.network.NodesKey;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.equipment.types.rev191129.OpticTypes;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.node.types.rev210528.NodeIdType;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev230526.ConnectionType;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev230526.RpcActions;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev230526.Service;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev230526.ethernet.subrate.attributes.grp.EthernetAttributesBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev230526.sdnc.request.header.SdncRequestHeaderBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev230526.service.endpoint.RxDirectionBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev230526.service.endpoint.RxDirectionKey;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev230526.service.endpoint.TxDirectionBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev230526.service.endpoint.TxDirectionKey;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev230526.service.lgx.LgxBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev230526.service.port.PortBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev230526.subrate.eth.sla.SubrateEthSlaBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.types.rev230526.OpenroadmNodeType;
import org.opendaylight.yang.gen.v1.http.org.openroadm.otn.common.types.rev210924.ODU4;
import org.opendaylight.yang.gen.v1.http.org.openroadm.otn.common.types.rev210924.OTU4;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.format.rev191129.ServiceFormat;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev230526.ServiceCreateInput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev230526.ServiceCreateInputBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev230526.service.create.input.ServiceAEnd;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev230526.service.create.input.ServiceAEndBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev230526.service.create.input.ServiceZEnd;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev230526.service.create.input.ServiceZEndBuilder;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev230501.PathDescription;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev230501.path.description.atoz.direction.AToZ;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev230501.path.description.atoz.direction.AToZKey;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev230501.path.description.ztoa.direction.ZToA;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev230501.path.description.ztoa.direction.ZToAKey;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev230501.pce.resource.resource.resource.Node;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev230501.pce.resource.resource.resource.TerminationPoint;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.servicepath.rev171017.service.path.list.ServicePaths;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.DateAndTime;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.AdministrativeState;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.CAPACITYUNITGBPS;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.Context;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.Direction;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.ForwardingDirection;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.LayerProtocolName;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.LifecycleState;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.OperationalState;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.PortRole;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.Uuid;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.capacity.TotalSizeBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.global._class.Name;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.global._class.NameBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.global._class.NameKey;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.tapi.context.ServiceInterfacePoint;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.tapi.context.ServiceInterfacePointKey;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev221121.CreateConnectivityServiceInput;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev221121.ProtectionRole;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev221121.ServiceType;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev221121.cep.list.ConnectionEndPoint;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev221121.cep.list.ConnectionEndPointBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev221121.connection.ConnectionEndPointKey;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev221121.connection.LowerConnection;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev221121.connection.LowerConnectionBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev221121.connection.LowerConnectionKey;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev221121.connection.end.point.ClientNodeEdgePoint;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev221121.connection.end.point.ClientNodeEdgePointBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev221121.connection.end.point.ParentNodeEdgePoint;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev221121.connection.end.point.ParentNodeEdgePointBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev221121.connectivity.context.ConnectivityService;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev221121.connectivity.context.ConnectivityServiceBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev221121.connectivity.service.Connection;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev221121.connectivity.service.ConnectionBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev221121.connectivity.service.ConnectionKey;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev221121.connectivity.service.ConnectivityConstraint;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev221121.connectivity.service.ConnectivityConstraintBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev221121.connectivity.service.EndPoint;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev221121.connectivity.service.EndPointBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev221121.connectivity.service.EndPointKey;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev221121.connectivity.service.end.point.CapacityBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev221121.connectivity.service.end.point.ServiceInterfacePointBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.photonic.media.rev221121.PHOTONICLAYERQUALIFIERMC;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.photonic.media.rev221121.PHOTONICLAYERQUALIFIEROTSiMC;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.context.TopologyContext;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.node.OwnedNodeEdgePoint;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.node.OwnedNodeEdgePointBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.node.OwnedNodeEdgePointKey;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.node.edge.point.MappedServiceInterfacePointKey;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.node.edge.point.SupportedCepLayerProtocolQualifierInstances;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.node.edge.point.SupportedCepLayerProtocolQualifierInstancesBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.topology.NodeKey;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.topology.context.Topology;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.topology.context.TopologyKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.Decimal64;
import org.opendaylight.yangtools.yang.common.Uint16;
import org.opendaylight.yangtools.yang.common.Uint32;
import org.opendaylight.yangtools.yang.common.Uint64;
import org.opendaylight.yangtools.yang.common.Uint8;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ConnectivityUtils {

    private final Uuid tapiTopoUuid = new Uuid(UUID.nameUUIDFromBytes(TapiStringConstants.T0_FULL_MULTILAYER
        .getBytes(StandardCharsets.UTF_8)).toString());
    private static final Logger LOG = LoggerFactory.getLogger(ConnectivityUtils.class);

    private final ServiceDataStoreOperations serviceDataStoreOperations;
    private final TapiContext tapiContext;
    private Map<ServiceInterfacePointKey, ServiceInterfacePoint> sipMap;
    private final Map<
        org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev221121.connectivity.context.ConnectionKey,
        org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev221121.connectivity.context.Connection>
        connectionFullMap; // this variable is for complete connection objects
    private final NetworkTransactionService networkTransactionService;
    private Connection topConnRdmRdm;
    private Connection topConnXpdrXpdrPhtn;
    private Connection topConnXpdrXpdrOdu;

    // TODO -> handle cases for which node id is ROADM-A1 and not ROADMA01 or XPDR-A1 and not XPDRA01
    public ConnectivityUtils(ServiceDataStoreOperations serviceDataStoreOperations,
                             Map<ServiceInterfacePointKey, ServiceInterfacePoint> sipMap, TapiContext tapiContext,
                             NetworkTransactionService networkTransactionService) {
        this.serviceDataStoreOperations = serviceDataStoreOperations;
        this.tapiContext = tapiContext;
        this.sipMap = sipMap;
        this.connectionFullMap = new HashMap<>();
        this.networkTransactionService = networkTransactionService;
        this.topConnRdmRdm = null;
        this.topConnXpdrXpdrPhtn = null;
        this.topConnXpdrXpdrOdu = null;
    }

    public static ServiceCreateInput buildServiceCreateInput(GenericServiceEndpoint sepA, GenericServiceEndpoint sepZ) {
        ServiceAEnd serviceAEnd = getServiceAEnd(sepA, sepZ);
        ServiceZEnd serviceZEnd = getServiceZEnd(sepA, sepZ);
        if (serviceAEnd == null || serviceZEnd == null) {
            LOG.warn("One of the endpoints could not be identified");
            return null;
        }
        return new ServiceCreateInputBuilder()
            .setCommonId("commonId")
            .setConnectionType(ConnectionType.Service)
            .setCustomer("Customer")
            .setServiceName("service test")
            .setServiceAEnd(serviceAEnd)
            .setServiceZEnd(serviceZEnd)
            .setSdncRequestHeader(new SdncRequestHeaderBuilder().setRequestId("request-1")
                .setRpcAction(RpcActions.ServiceCreate).setNotificationUrl("notification url").setRequestSystemId(
                    "appname")
                .build())
            .build();
    }

    public static ServiceAEnd buildServiceAEnd(String nodeid, String clli, String txPortDeviceName,
                                               String txPortName, String rxPortDeviceName, String rxPortName) {
        return new ServiceAEndBuilder()
            .setClli(clli)
            .setNodeId(new NodeIdType(nodeid))
            .setOpticType(OpticTypes.Gray)
            .setServiceFormat(ServiceFormat.Ethernet)
            .setServiceRate(Uint32.valueOf(100))
            .setTxDirection(Map.of(new TxDirectionKey(Uint8.ZERO), new TxDirectionBuilder()
                .setPort(new PortBuilder()
                    .setPortDeviceName(txPortDeviceName)
                    .setPortName(txPortName)
                    .setPortRack(TapiStringConstants.PORT_RACK_VALUE)
                    .setPortShelf("00")
                    .setPortType(TapiStringConstants.PORT_TYPE)
                    .build())
                .setLgx(new LgxBuilder()
                    .setLgxDeviceName(TapiStringConstants.LGX_DEVICE_NAME)
                    .setLgxPortName(TapiStringConstants.LGX_PORT_NAME)
                    .setLgxPortRack(TapiStringConstants.PORT_RACK_VALUE)
                    .setLgxPortShelf("00")
                    .build())
                .build()))
            .setRxDirection(Map.of(new RxDirectionKey(Uint8.ZERO), new RxDirectionBuilder()
                .setPort(new PortBuilder()
                    .setPortDeviceName(rxPortDeviceName)
                    .setPortName(rxPortName)
                    .setPortRack(TapiStringConstants.PORT_RACK_VALUE)
                    .setPortShelf("00")
                    .setPortType(TapiStringConstants.PORT_TYPE)
                    .build())
                .setLgx(new LgxBuilder()
                    .setLgxDeviceName(TapiStringConstants.LGX_DEVICE_NAME)
                    .setLgxPortName(TapiStringConstants.LGX_PORT_NAME)
                    .setLgxPortRack(TapiStringConstants.PORT_RACK_VALUE)
                    .setLgxPortShelf("00")
                    .build())
                .build()))
            .build();
    }

    public static ServiceZEnd buildServiceZEnd(String nodeid, String clli, String txPortDeviceName,
                                               String txPortName, String rxPortDeviceName, String rxPortName) {
        return  new ServiceZEndBuilder().setClli(clli).setNodeId(new NodeIdType(nodeid))
            .setOpticType(OpticTypes.Gray)
            .setServiceFormat(ServiceFormat.Ethernet)
            .setServiceRate(Uint32.valueOf(100))
            .setTxDirection(Map.of(new TxDirectionKey(Uint8.ZERO), new TxDirectionBuilder()
                .setPort(new PortBuilder()
                    .setPortDeviceName(txPortDeviceName)
                    .setPortName(txPortName)
                    .setPortRack(TapiStringConstants.PORT_RACK_VALUE)
                    .setPortShelf("00")
                    .setPortType(TapiStringConstants.PORT_TYPE)
                    .build())
                .setLgx(new LgxBuilder()
                    .setLgxDeviceName(TapiStringConstants.LGX_DEVICE_NAME)
                    .setLgxPortName(TapiStringConstants.LGX_PORT_NAME)
                    .setLgxPortRack(TapiStringConstants.PORT_RACK_VALUE)
                    .setLgxPortShelf("00")
                    .build())
                .build()))
            .setRxDirection(Map.of(new RxDirectionKey(Uint8.ZERO), new RxDirectionBuilder()
                .setPort(new PortBuilder()
                    .setPortDeviceName(rxPortDeviceName)
                    .setPortName(rxPortName)
                    .setPortRack(TapiStringConstants.PORT_RACK_VALUE)
                    .setPortShelf("00")
                    .setPortType(TapiStringConstants.PORT_TYPE)
                    .build())
                .setLgx(new LgxBuilder()
                    .setLgxDeviceName(TapiStringConstants.LGX_DEVICE_NAME)
                    .setLgxPortName(TapiStringConstants.LGX_PORT_NAME)
                    .setLgxPortRack(TapiStringConstants.PORT_RACK_VALUE)
                    .setLgxPortShelf("00")
                    .build())
                .build()))
            .build();
    }

    public void setSipMap(Map<ServiceInterfacePointKey, ServiceInterfacePoint> sips) {
        this.sipMap = sips;
    }

    public ConnectivityService mapORServiceToTapiConnectivity(Service service) {
        // Get service path with the description in OR based models.
        LOG.info("Service = {}", service);
        Optional<ServicePaths> optServicePaths =
            this.serviceDataStoreOperations.getServicePath(service.getServiceName());
        if (!optServicePaths.isPresent()) {
            LOG.error("No service path found for service {}", service.getServiceName());
            return null;
        }
        ServicePaths servicePaths = optServicePaths.orElseThrow();
        PathDescription pathDescription = servicePaths.getPathDescription();
        LOG.info("Path description of service = {}", pathDescription);
        org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev230526.service.ServiceAEnd serviceAEnd
            = service.getServiceAEnd();
        // Endpoint creation
        EndPoint endPoint1 = mapServiceAEndPoint(serviceAEnd, pathDescription);
        org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev230526.service.ServiceZEnd serviceZEnd
            = service.getServiceZEnd();
        EndPoint endPoint2 = mapServiceZEndPoint(serviceZEnd, pathDescription);
        Map<EndPointKey, EndPoint> endPointMap = new HashMap<>();
        endPointMap.put(endPoint1.key(), endPoint1);
        endPointMap.put(endPoint2.key(), endPoint2);
        LOG.info("EndPoints of connectivity services = {}", endPointMap);
        // Services Names
        Name name = new NameBuilder().setValueName("Connectivity Service Name").setValue(service.getServiceName())
            .build();
        // Connection creation
        Map<ConnectionKey, Connection> connMap =
            createConnectionsFromService(pathDescription, mapServiceLayerToAend(serviceAEnd));
        LOG.debug("connectionMap for service {} = {} ", name.toString(), connMap.toString());
        ConnectivityConstraint conConstr =
            new ConnectivityConstraintBuilder().setServiceType(ServiceType.POINTTOPOINTCONNECTIVITY).build();
        // TODO: full connectivity service?? With constraints and the rest of fields...
        return new ConnectivityServiceBuilder()
            .setAdministrativeState(AdministrativeState.UNLOCKED)
            .setOperationalState(OperationalState.ENABLED)
            .setLifecycleState(LifecycleState.INSTALLED)
            .setUuid(new Uuid(UUID.nameUUIDFromBytes(service.getServiceName().getBytes(StandardCharsets.UTF_8))
                .toString()))
            .setLayerProtocolName(mapServiceLayer(serviceAEnd.getServiceFormat(), endPoint1, endPoint2))
            .setConnectivityConstraint(conConstr)
            .setDirection(ForwardingDirection.BIDIRECTIONAL)
            .setName(Map.of(name.key(), name))
            .setConnection(connMap)
            .setEndPoint(endPointMap)
            .build();
    }

    public Map<ConnectionKey, Connection> createConnectionsFromService(PathDescription pathDescription,
            LayerProtocolName lpn) {
        Map<ConnectionKey, Connection> connectionServMap = new HashMap<>();
        // build lists with ROADM nodes, XPDR/MUX/SWITCH nodes, ROADM DEG TTPs, ROADM SRG TTPs, XPDR CLIENT TTPs
        //  and XPDR NETWORK TTPs (if any). From the path description. This will help to build the uuid of the CEPs
        //  and the connections
        String resourceType;
        List<String> xpdrClientTplist = new ArrayList<>();
        List<String> xpdrNetworkTplist = new ArrayList<>();
        List<String> rdmAddDropTplist = new ArrayList<>();
        List<String> rdmDegTplist = new ArrayList<>();
        List<String> rdmNodelist = new ArrayList<>();
        List<String> xpdrNodelist = new ArrayList<>();
        for (AToZ elem:pathDescription.getAToZDirection().getAToZ().values().stream()
            .sorted((Comparator.comparing(atoz -> Integer.valueOf(atoz.getId())))).collect(Collectors.toList())) {
            resourceType = elem.getResource().getResource().implementedInterface().getSimpleName();
            switch (resourceType) {
                case TapiStringConstants.TP:
                    TerminationPoint tp = (TerminationPoint) elem.getResource().getResource();
                    String tpID = tp.getTpId();
                    String tpNode;
                    if (tpID.contains("CLIENT")) {
                        tpNode = tp.getTpNodeId();
                        if (!xpdrClientTplist.contains(String.join("+", tpNode, tpID))) {
                            xpdrClientTplist.add(String.join("+", tpNode, tpID));
                        }
                    }
                    if (tpID.contains("NETWORK")) {
                        tpNode = tp.getTpNodeId();
                        if (!xpdrNetworkTplist.contains(String.join("+", tpNode, tpID))) {
                            xpdrNetworkTplist.add(String.join("+", tpNode, tpID));
                        }
                    }
                    if (tpID.contains("PP")) {
                        tpNode = getIdBasedOnModelVersion(tp.getTpNodeId());
                        LOG.info("ROADM Node of tp = {}", tpNode);
                        if (!rdmAddDropTplist.contains(String.join("+", tpNode, tpID))) {
                            rdmAddDropTplist.add(String.join("+", tpNode, tpID));
                        }
                    }
                    if (tpID.contains("TTP")) {
                        tpNode = getIdBasedOnModelVersion(tp.getTpNodeId());
                        LOG.info("ROADM Node of tp = {}", tpNode);
                        if (!rdmDegTplist.contains(String.join("+", tpNode, tpID))) {
                            rdmDegTplist.add(String.join("+", tpNode, tpID));
                        }
                    }
                    break;
                case TapiStringConstants.NODE:
                    Node node = (Node) elem.getResource().getResource();
                    String nodeId = node.getNodeId();
                    if (nodeId.contains("XPDR") || nodeId.contains("SPDR") || nodeId.contains("MXPDR")) {
                        LOG.info("Node id = {}", nodeId);
                        if (!xpdrNodelist.contains(nodeId)) {
                            xpdrNodelist.add(nodeId); // should contain only 2
                        }
                    }
                    if (nodeId.contains("ROADM")) {
                        nodeId = getIdBasedOnModelVersion(nodeId);
                        LOG.info("Node id = {}", nodeId);
                        if (!rdmNodelist.contains(nodeId)) {
                            rdmNodelist.add(nodeId);
                        }
                    }
                    break;
                default:
                    LOG.warn("Resource is a {}", resourceType);
            }
        }
        LOG.info("ROADM node list = {}", rdmNodelist);
        LOG.info("ROADM degree list = {}", rdmDegTplist);
        LOG.info("ROADM addrop list = {}", rdmAddDropTplist);
        LOG.info("XPDR node list = {}", xpdrNodelist);
        LOG.info("XPDR network list = {}", xpdrNetworkTplist);
        LOG.info("XPDR client list = {}", xpdrClientTplist);
        // TODO -> for 10GB eth and ODU services there are no ROADMs in path description as they use the OTU link,
        //  but for 100GB eth all is created at once. Check if the roadm list is empty to determine whether we need
        //  to trigger all the steps or not
        String edgeRoadm1 = "";
        String edgeRoadm2 = "";
        if (!rdmNodelist.isEmpty()) {
            edgeRoadm1 = rdmNodelist.get(0);
            edgeRoadm2 = rdmNodelist.get(rdmNodelist.size() - 1);
            LOG.info("edgeRoadm1 = {}", edgeRoadm1);
            LOG.info("edgeRoadm2 = {}", edgeRoadm2);
        }
        // create corresponding CEPs and Connections. Connections should be added to the corresponding context
        // CEPs must be included in the topology context as an augmentation for each ONEP!!
        // TODO -> Maybe we dont need to create the connections and ceps if the previous service doesnt exist??
        //  As mentioned above, for 100GbE service creation there are ROADMs in the path description.
        //  What are the configurations needed here? No OTU, ODU... what kind of cross connections is needed?
        //  this needs to be changed
        // TODO: OpenROADM getNodeType from the NamesList to verify what needs to be created
        OpenroadmNodeType openroadmNodeType = getOpenRoadmNodeType(xpdrNodelist);
        switch (lpn) {
            case PHOTONICMEDIA:
                // Identify number of ROADMs
                // - XC Connection between MC CEPs mapped from MC NEPs (within a roadm)
                // - XC Connection between OTSiMC CEPs mapped from OTSiMC NEPs (within a roadm)
                // - Top Connection MC betwwen MC CEPs of different roadms
                // - Top Connection OTSiMC betwwen OTSiMC CEPs of extreme roadms
                connectionServMap.putAll(createRoadmCepsAndConnections(rdmAddDropTplist, rdmDegTplist, rdmNodelist,
                    edgeRoadm1, edgeRoadm2));
                if (!pathDescription.getAToZDirection().getAToZ().values().stream().findFirst().orElseThrow().getId()
                    .contains("ROADM")) {
                    // - XC Connection OTSi betwwen iOTSi y eOTSi of xpdr
                    // - Top connection OTSi between network ports of xpdrs in the Photonic media layer -> i_OTSi
                    connectionServMap.putAll(createXpdrCepsAndConnectionsPht(xpdrNetworkTplist, xpdrNodelist));
                }
                this.topConnRdmRdm = null;
                break;
            case ODU:
                // TODO: verify if this is correct
                // - XC Connection OTSi betwwen iODU and eODU of xpdr
                // - Top connection in the ODU layer, between xpdr eODU ports (?)
                if (openroadmNodeType.equals(OpenroadmNodeType.MUXPDR)) {
                    connectionServMap.putAll(createXpdrCepsAndConnectionsOdu(xpdrNetworkTplist, xpdrNodelist));
                    this.topConnXpdrXpdrPhtn = null;
                }
                break;
            case ETH:
                // Check if OC, OTU and ODU are created
                if (openroadmNodeType.equals(OpenroadmNodeType.TPDR)) {
                    LOG.info("WDM ETH service");
                    connectionServMap.putAll(createRoadmCepsAndConnections(rdmAddDropTplist, rdmDegTplist, rdmNodelist,
                        edgeRoadm1, edgeRoadm2));
                    connectionServMap.putAll(createXpdrCepsAndConnectionsPht(xpdrNetworkTplist, xpdrNodelist));
                    this.topConnRdmRdm = null;
                    xpdrClientTplist = getAssociatedClientsPort(xpdrNetworkTplist);
                    LOG.info("Associated client ports = {}", xpdrClientTplist);
                    connectionServMap.putAll(createXpdrCepsAndConnectionsEth(xpdrClientTplist, xpdrNodelist,
                        connectionServMap));
                    this.topConnXpdrXpdrPhtn = null;
                }
                break;
            case DSR:
                LOG.info("OTN XGE/ODUe service");
                // - XC connection between iODU and eODU
                // - Top connection between eODU ports
                // - Top connection between DSR ports
                if (openroadmNodeType.equals(OpenroadmNodeType.SWITCH)) {
                    // TODO: We create both ODU and DSR because there is no ODU service creation for the switch
                    // - XC Connection OTSi between iODU and eODU of xpdr
                    // - Top connection in the ODU layer, between xpdr eODU ports (?)
                    connectionServMap.putAll(createXpdrCepsAndConnectionsDsr(xpdrClientTplist, xpdrNetworkTplist,
                        xpdrNodelist));
                    this.topConnXpdrXpdrPhtn = null;
                }
                if (openroadmNodeType.equals(OpenroadmNodeType.MUXPDR)) {
                    // TODO: OTN service but mux has 3 steps at rendering. Verify that things exist
                    connectionServMap.putAll(createXpdrCepsAndConnectionsDsr(xpdrClientTplist, xpdrNetworkTplist,
                        xpdrNodelist));
                    this.topConnXpdrXpdrOdu = null;
                }
                break;
            default:
                LOG.error("Service type format not supported");
        }
        LOG.debug("CONNSERVERMAP = {}", connectionServMap.toString());
        return connectionServMap;
    }

    public void updateTopologyWithNep(Uuid topoUuid, Uuid nodeUuid, Uuid nepUuid, OwnedNodeEdgePoint onep) {
        // TODO: verify this is correct. Should we identify the context IID with the context UUID??
        InstanceIdentifier<OwnedNodeEdgePoint> onepIID = InstanceIdentifier.builder(Context.class)
            .augmentation(org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.Context1.class)
            .child(org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.context.TopologyContext.class)
            .child(Topology.class, new TopologyKey(topoUuid))
            .child(org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.topology.Node.class,
                new NodeKey(nodeUuid))
            .child(OwnedNodeEdgePoint.class, new OwnedNodeEdgePointKey(nepUuid))
            .build();
        try {
            Optional<OwnedNodeEdgePoint> optionalOnep = this.networkTransactionService.read(
                LogicalDatastoreType.OPERATIONAL, onepIID).get();
            if (optionalOnep.isPresent()) {
                LOG.error("ONEP is already present in datastore");
                return;
            }
            // merge in datastore
            this.networkTransactionService.merge(LogicalDatastoreType.OPERATIONAL, onepIID,
                onep);
            this.networkTransactionService.commit().get();
            LOG.info("NEP {} added successfully.", onep.getName().toString());
        } catch (InterruptedException | ExecutionException e) {
            LOG.error("Couldnt put NEP {} in topology, error = ", onep.getName().toString(), e);
        }
    }

    public Map<org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev221121
                .connectivity.context.ConnectionKey,
            org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev221121
                .connectivity.context.Connection> getConnectionFullMap() {
        return this.connectionFullMap;
    }

    public ServiceCreateInput createORServiceInput(CreateConnectivityServiceInput input, Uuid serviceUuid) {
        // TODO: not taking into account all the constraints. Only using EndPoints and Connectivity Constraint.
        Map<org.opendaylight.yang.gen.v1.urn
                .onf.otcc.yang.tapi.connectivity.rev221121.create.connectivity.service.input.EndPointKey,
            org.opendaylight.yang.gen.v1.urn
                .onf.otcc.yang.tapi.connectivity.rev221121.create.connectivity.service.input.EndPoint>
            endPointMap = input.getEndPoint();
        ConnectionType connType = null;
        ServiceFormat serviceFormat = null;
        String nodeAid = String.join("+", endPointMap.values().stream().findFirst().orElseThrow().getLocalId(),
            TapiStringConstants.XPDR);
        String nodeZid = String.join("+", endPointMap.values().stream().skip(1).findFirst().orElseThrow().getLocalId(),
            TapiStringConstants.XPDR);
        LOG.info("NodeAid = {}", nodeAid);
        LOG.info("NodeZid = {}", nodeZid);
        //switch (constraint.getServiceLayer().getIntValue()) {
        switch (input.getLayerProtocolName().getIntValue()) {
            case 0:
                LOG.info("ODU");
                connType = ConnectionType.Infrastructure;
                serviceFormat = ServiceFormat.ODU;
                break;
            case 1:
                LOG.info("ETH, no need to create OTU and ODU");
                connType = ConnectionType.Service;
                serviceFormat = ServiceFormat.Ethernet;
                break;
            case 2:
                LOG.info("DSR, need to create OTU and ODU");
                connType = ConnectionType.Service;
                serviceFormat = ServiceFormat.Ethernet;
                break;
            case 3:
                LOG.info("PHOTONIC");
                connType = getConnectionTypePhtnc(endPointMap.values());
                serviceFormat = getServiceFormatPhtnc(endPointMap.values());
                LOG.debug("Node a photonic = {}", nodeAid);
                LOG.debug("Node z photonic = {}", nodeZid);
                break;
            default:
                LOG.info("Service type {} not supported", input.getLayerProtocolName().getName());
        }
        // Requested Capacity for connectivity service
        Uint64 capacity = Uint64.valueOf(Math.abs(
            input.getConnectivityConstraint().getRequestedCapacity().getTotalSize().getValue().intValue()));
        // map endpoints into service end points. Map the type of service from TAPI to OR
        ServiceAEnd serviceAEnd = tapiEndPointToServiceAPoint(endPointMap.values().stream().findFirst().orElseThrow(),
            serviceFormat, nodeAid, capacity, input.getLayerProtocolName());
        ServiceZEnd serviceZEnd = tapiEndPointToServiceZPoint(endPointMap.values().stream().skip(1).findFirst()
                .orElseThrow(), serviceFormat, nodeZid, capacity, input.getLayerProtocolName());
        if (serviceAEnd == null || serviceZEnd == null) {
            LOG.error("Couldnt map endpoints to service end");
            return null;
        }
        LOG.info("Service a end = {}", serviceAEnd);
        LOG.info("Service z end = {}", serviceZEnd);
        return new ServiceCreateInputBuilder()
            .setServiceAEnd(serviceAEnd)
            .setServiceZEnd(serviceZEnd)
            .setConnectionType(connType)
            .setServiceName(serviceUuid.getValue())
            .setCommonId("common id")
            .setSdncRequestHeader(new SdncRequestHeaderBuilder().setRequestId("request-1")
                .setRpcAction(RpcActions.ServiceCreate).setNotificationUrl("notification url")
                .setRequestSystemId("appname")
                .build())
            .setCustomer("customer")
            .setDueDate(DateAndTime.getDefaultInstance("2018-06-15T00:00:01Z"))
            .setOperatorContact("pw1234")
            .build();
    }

    private static ServiceAEnd getServiceAEnd(GenericServiceEndpoint sepA, GenericServiceEndpoint sepZ) {
        if (sepA.getType().equals(ServiceEndpointType.SERVICEAEND)) {
            return new ServiceAEndBuilder(sepA.getValue()).build();
        }
        if (sepZ.getType().equals(ServiceEndpointType.SERVICEAEND)) {
            return new ServiceAEndBuilder(sepZ.getValue()).build();
        }
        return null;
    }

    private static ServiceZEnd getServiceZEnd(GenericServiceEndpoint sepA, GenericServiceEndpoint sepZ) {
        if (sepA.getType().equals(ServiceEndpointType.SERVICEZEND)) {
            return new ServiceZEndBuilder(sepA.getValue()).build();
        }
        if (sepZ.getType().equals(ServiceEndpointType.SERVICEZEND)) {
            return new ServiceZEndBuilder(sepZ.getValue()).build();
        }
        return null;
    }

    private LayerProtocolName mapServiceLayerToAend(
            org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev230526.service.ServiceAEnd
            serviceAEnd) {
        ServiceFormat serviceFormat = serviceAEnd.getServiceFormat();
        switch (serviceFormat) {
            case OC:
            case OTU:
                return LayerProtocolName.PHOTONICMEDIA;
            case ODU:
                return LayerProtocolName.ODU;
            case Ethernet:
                if (getOpenroadmType(serviceAEnd.getTxDirection().values().stream().findFirst().orElseThrow().getPort()
                        .getPortDeviceName()).equals(OpenroadmNodeType.TPDR)) {
                    return LayerProtocolName.ETH;
                }
                return LayerProtocolName.DSR;
            default:
                LOG.info("Service layer mapping not supported for {}", serviceFormat.getName());
        }
        return null;
    }

    private LayerProtocolName mapServiceLayer(ServiceFormat serviceFormat, EndPoint endPoint1, EndPoint endPoint2) {
        switch (serviceFormat) {
            case OC:
            case OTU:
                return LayerProtocolName.PHOTONICMEDIA;
            case ODU:
                return LayerProtocolName.ODU;
            case Ethernet:
                String node1 = endPoint1.getLocalId();
                String node2 = endPoint2.getLocalId();
                if (getOpenroadmType(node1).equals(OpenroadmNodeType.TPDR)
                        && getOpenroadmType(node2).equals(OpenroadmNodeType.TPDR)) {
                    return LayerProtocolName.ETH;
                }
                return LayerProtocolName.DSR;
            default:
                LOG.info("Service layer mapping not supported for {}", serviceFormat.getName());
        }
        return null;
    }

    private OpenroadmNodeType getOpenroadmType(String nodeName) {
        LOG.info("Node name = {}", nodeName);
        Uuid nodeUuid = new Uuid(UUID.nameUUIDFromBytes((String.join("+",nodeName, TapiStringConstants.XPDR))
            .getBytes(StandardCharsets.UTF_8)).toString());
        org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.topology.Node tapiNode
            = this.tapiContext.getTapiNode(this.tapiTopoUuid, nodeUuid);
        if (tapiNode != null) {
            return OpenroadmNodeType.forName(tapiNode.getName().get(new NameKey("Node Type"))
                .getValue());
        }
        return null;
    }

    private Map<ConnectionKey, Connection> createXpdrCepsAndConnectionsEth(List<String> xpdrClientTplist,
                                                                           List<String> xpdrNodelist,
                                                                           Map<ConnectionKey, Connection> lowerConn) {
        // TODO: do we need to create cross connection between iODU and eODU??
        // add the lower connections of the previous steps for this kind of service
        Map<LowerConnectionKey, LowerConnection> xcMap = new HashMap<>();
        for (Connection lowConn: lowerConn.values()) {
            LowerConnection conn = new LowerConnectionBuilder().setConnectionUuid(lowConn.getConnectionUuid()).build();
            xcMap.put(conn.key(), conn);
        }
        Map<ConnectionKey, Connection> connServMap = new HashMap<>();
        Map<org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev221121.cep.list.ConnectionEndPointKey,
            ConnectionEndPoint> cepMapDsr = new HashMap<>();
        // Create 1 cep per Xpdr in the CLIENT
        // 1 top connection DSR between the CLIENT xpdrs
        for (String xpdr:xpdrNodelist) {
            LOG.info("Creating ceps and xc for xpdr {}", xpdr);
            String spcXpdrClient = xpdrClientTplist.stream().filter(netp -> netp.contains(xpdr)).findFirst()
                .orElseThrow();
            ConnectionEndPoint netCep1 = createCepXpdr(spcXpdrClient, TapiStringConstants.DSR, TapiStringConstants.XPDR,
                LayerProtocolName.DSR);
            putXpdrCepInTopologyContext(xpdr, spcXpdrClient, TapiStringConstants.DSR,
                TapiStringConstants.XPDR, netCep1);

            cepMapDsr.put(netCep1.key(), netCep1);
        }
        String spcXpdr1 = xpdrClientTplist.stream().filter(adp -> adp.contains(xpdrNodelist
            .get(0))).findFirst().orElseThrow();
        String spcXpdr2 = xpdrClientTplist.stream().filter(adp -> adp.contains(xpdrNodelist
            .get(xpdrNodelist.size() - 1))).findFirst().orElseThrow();

        // DSR top connection between edge xpdr CLIENT DSR
        org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev221121.connectivity.context.Connection
            connectionDsr = createTopConnection(spcXpdr1, spcXpdr2, cepMapDsr, TapiStringConstants.DSR,
            LayerProtocolName.DSR, xcMap, this.topConnXpdrXpdrPhtn);
        this.connectionFullMap.put(connectionDsr.key(), connectionDsr);

        // DSR top connection that will be added to the service object
        Connection conn1 = new ConnectionBuilder().setConnectionUuid(connectionDsr.getUuid()).build();
        connServMap.put(conn1.key(), conn1);

        return connServMap;
    }

    private Map<ConnectionKey,Connection> createXpdrCepsAndConnectionsDsr(List<String> xpdrClientTplist,
                                                                          List<String> xpdrNetworkTplist,
                                                                          List<String> xpdrNodelist) {
        Map<ConnectionKey, Connection> connServMap = new HashMap<>();
        Map<org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev221121.cep.list.ConnectionEndPointKey,
            ConnectionEndPoint> cepMapDsr = new HashMap<>();
        Map<org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev221121.cep.list.ConnectionEndPointKey,
            ConnectionEndPoint> cepMapOdu = new HashMap<>();
        // TODO: when upgrading the models to 2.1.3, get the connection inclusion because those connections will
        //  be added to the lower connection of a top connection
        Map<LowerConnectionKey, LowerConnection> xcMap = new HashMap<>();

        // Create 1 cep per Xpdr in the CLIENT, 1 cep per Xpdr eODU, 1 XC between eODU and iODE,
        // 1 top connection between eODU and a top connection DSR between the CLIENT xpdrs
        for (String xpdr:xpdrNodelist) {
            LOG.info("Creating ceps and xc for xpdr {}", xpdr);
            String spcXpdrClient = xpdrClientTplist.stream().filter(netp -> netp.contains(xpdr)).findFirst()
                .orElseThrow();
            ConnectionEndPoint clientCep1 = createCepXpdr(spcXpdrClient, TapiStringConstants.DSR,
                TapiStringConstants.XPDR, LayerProtocolName.DSR);
            putXpdrCepInTopologyContext(xpdr, spcXpdrClient, TapiStringConstants.DSR, TapiStringConstants.XPDR,
                clientCep1);

            ConnectionEndPoint clientCep2 = createCepXpdr(spcXpdrClient, TapiStringConstants.E_ODU,
                TapiStringConstants.XPDR, LayerProtocolName.ODU);
            putXpdrCepInTopologyContext(xpdr, spcXpdrClient, TapiStringConstants.E_ODU, TapiStringConstants.XPDR,
                clientCep2);

            String spcXpdrNetwork = getAssociatedNetworkPort(spcXpdrClient, xpdrNetworkTplist);
            ConnectionEndPoint netCep3 = getAssociatediODUCep(spcXpdrNetwork);

            cepMapDsr.put(clientCep1.key(), clientCep1);
            cepMapOdu.put(clientCep2.key(), clientCep2);
            // Create x connection between I_ODU and E_ODU within xpdr
            org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev221121.connectivity.context.Connection
                connection = createXCBetweenCeps(clientCep2, netCep3, spcXpdrClient, spcXpdrNetwork,
                TapiStringConstants.ODU, LayerProtocolName.ODU);
            this.connectionFullMap.put(connection.key(), connection);

            // Create X connection that will be added to the service object
            LowerConnection conn = new LowerConnectionBuilder().setConnectionUuid(connection.getUuid()).build();
            xcMap.put(conn.key(), conn);
        }

        // DSR top connection between edge xpdr CLIENT DSR
        String spcXpdr1 = xpdrClientTplist.stream().filter(adp -> adp.contains(xpdrNodelist
            .get(0))).findFirst().orElseThrow();
        String spcXpdr2 = xpdrClientTplist.stream().filter(adp -> adp.contains(xpdrNodelist
            .get(xpdrNodelist.size() - 1))).findFirst().orElseThrow();

        org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev221121.connectivity.context.Connection
            connectionOdu = createTopConnection(spcXpdr1, spcXpdr2, cepMapOdu, TapiStringConstants.E_ODU,
            LayerProtocolName.ODU, xcMap, this.topConnXpdrXpdrOdu);
        this.connectionFullMap.put(connectionOdu.key(), connectionOdu);

        // ODU top connection that will be added to the service object
        Connection conn = new ConnectionBuilder().setConnectionUuid(connectionOdu.getUuid()).build();
        connServMap.put(conn.key(), conn);
        LowerConnection lowerConn = new LowerConnectionBuilder().setConnectionUuid(connectionOdu.getUuid()).build();
        xcMap.put(lowerConn.key(), lowerConn);

        org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev221121.connectivity.context.Connection
            connectionDsr = createTopConnection(spcXpdr1, spcXpdr2, cepMapDsr, TapiStringConstants.DSR,
            LayerProtocolName.DSR, xcMap, this.topConnXpdrXpdrPhtn);
        this.connectionFullMap.put(connectionDsr.key(), connectionDsr);

        // DSR top connection that will be added to the service object
        Connection conn1 = new ConnectionBuilder().setConnectionUuid(connectionDsr.getUuid()).build();
        connServMap.put(conn1.key(), conn1);

        return connServMap;
    }

    private Map<ConnectionKey, Connection> createXpdrCepsAndConnectionsOdu(List<String> xpdrNetworkTplist,
                                                                           List<String> xpdrNodelist) {
        Map<ConnectionKey, Connection> connServMap = new HashMap<>();
        Map<org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev221121.cep.list.ConnectionEndPointKey,
            ConnectionEndPoint> cepMap = new HashMap<>();
        // TODO: when upgrading the models to 2.1.3, get the connection inclusion because those connections will
        //  be added to the lower connection of a top connection
        Map<LowerConnectionKey, LowerConnection> xcMap = new HashMap<>();

        // Create 1 cep per Xpdr in the I_ODU and a top
        // connection iODU between the xpdrs
        for (String xpdr:xpdrNodelist) {
            LOG.info("Creating ceps and xc for xpdr {}", xpdr);
            String spcXpdrNetwork = xpdrNetworkTplist.stream().filter(netp -> netp.contains(xpdr)).findFirst()
                .orElseThrow();
            ConnectionEndPoint netCep1 = createCepXpdr(spcXpdrNetwork, TapiStringConstants.I_ODU,
                TapiStringConstants.XPDR, LayerProtocolName.ODU);
            putXpdrCepInTopologyContext(xpdr, spcXpdrNetwork, TapiStringConstants.I_ODU, TapiStringConstants.XPDR,
                netCep1);

            cepMap.put(netCep1.key(), netCep1);
        }

        // ODU top connection between edge xpdr i_ODU
        String spcXpdr1 = xpdrNetworkTplist.stream().filter(adp -> adp.contains(xpdrNodelist
            .get(0))).findFirst().orElseThrow();
        String spcXpdr2 = xpdrNetworkTplist.stream().filter(adp -> adp.contains(xpdrNodelist
            .get(xpdrNodelist.size() - 1))).findFirst().orElseThrow();
        org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev221121.connectivity.context.Connection
            connection = createTopConnection(spcXpdr1, spcXpdr2, cepMap, TapiStringConstants.I_ODU,
            LayerProtocolName.ODU, xcMap, this.topConnXpdrXpdrPhtn);
        this.connectionFullMap.put(connection.key(), connection);

        // ODU top connection that will be added to the service object
        Connection conn = new ConnectionBuilder().setConnectionUuid(connection.getUuid()).build();
        connServMap.put(conn.key(), conn);
        this.topConnXpdrXpdrOdu = conn;

        return connServMap;
    }

    private Map<ConnectionKey, Connection> createXpdrCepsAndConnectionsPht(List<String> xpdrNetworkTplist,
                                                                           List<String> xpdrNodelist) {

        // TODO: when upgrading the models to 2.1.3, get the connection inclusion because those connections will
        //  be added to the lower connection of a top connection
        Map<org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev221121.cep.list.ConnectionEndPointKey,
            ConnectionEndPoint> cepMap = new HashMap<>();
        // create ceps and x connections within xpdr
        for (String xpdr:xpdrNodelist) {
            LOG.info("Creating ceps and xc for xpdr {}", xpdr);
            String spcXpdrNetwork = xpdrNetworkTplist.stream().filter(netp -> netp.contains(xpdr)).findFirst()
                .orElseThrow();
            // There should be 1 network tp per xpdr
            //   Just create 2 different CEPs (1 OTS + 1 OTSI_MC)
            ConnectionEndPoint netCep1 = createCepXpdr(spcXpdrNetwork, TapiStringConstants.PHTNC_MEDIA_OTS,
                TapiStringConstants.XPDR, LayerProtocolName.PHOTONICMEDIA);
            putXpdrCepInTopologyContext(xpdr, spcXpdrNetwork, TapiStringConstants.PHTNC_MEDIA_OTS,
                TapiStringConstants.XPDR, netCep1);
            ConnectionEndPoint netCep2 = createCepXpdr(spcXpdrNetwork, TapiStringConstants.OTSI_MC,
                TapiStringConstants.XPDR, LayerProtocolName.PHOTONICMEDIA);
            putXpdrCepInTopologyContext(xpdr, spcXpdrNetwork, TapiStringConstants.OTSI_MC, TapiStringConstants.XPDR,
                netCep2);
//            ConnectionEndPoint netCep3 = createCepXpdr(spcXpdrNetwork, TapiStringConstants.I_OTSI,
//                TapiStringConstants.XPDR, LayerProtocolName.PHOTONICMEDIA);
//            putXpdrCepInTopologyContext(xpdr, spcXpdrNetwork, TapiStringConstants.I_OTSI, TapiStringConstants.OTSI,
//                netCep3);

            cepMap.put(netCep1.key(), netCep1);
            cepMap.put(netCep2.key(), netCep2);
//            cepMap.put(netCep3.key(), netCep3);

        }

        // OTSi top connection between edge OTSI_MC Xpdr
        Map<LowerConnectionKey, LowerConnection> xcMap = new HashMap<>();
        String spcXpdr1 = xpdrNetworkTplist.stream().filter(adp -> adp.contains(xpdrNodelist
            .get(0))).findFirst().orElseThrow();
        String spcXpdr2 = xpdrNetworkTplist.stream().filter(adp -> adp.contains(xpdrNodelist
            .get(xpdrNodelist.size() - 1))).findFirst().orElseThrow();
        org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev221121.connectivity.context.Connection
            connection = createTopConnection(spcXpdr1, spcXpdr2, cepMap, TapiStringConstants.OTSI_MC,
            LayerProtocolName.PHOTONICMEDIA, xcMap, this.topConnRdmRdm);
        this.connectionFullMap.put(connection.key(), connection);

      // OTSi top connection that will be added to the service object
        Map<ConnectionKey, Connection> connServMap = new HashMap<>();
        Connection conn = new ConnectionBuilder().setConnectionUuid(connection.getUuid()).build();
        connServMap.put(conn.key(), conn);
        this.topConnXpdrXpdrPhtn = conn;

        return connServMap;
    }

    private Map<ConnectionKey, Connection> createRoadmCepsAndConnections(List<String> rdmAddDropTplist,
                                                                         List<String> rdmDegTplist,
                                                                         List<String> rdmNodelist,
                                                                         String edgeRoadm1, String edgeRoadm2) {
        // TODO: will need to check if things exist already or not
        Map<org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev221121.cep.list.ConnectionEndPointKey,
            ConnectionEndPoint> cepMap = new HashMap<>();
        // create ceps and x connections within roadm
        Map<LowerConnectionKey, LowerConnection> xcLowerMap = new HashMap<>();
        for (String roadm : rdmNodelist) {
            LOG.info("Creating ceps and xc for roadm {}", roadm);
            if (roadm.equals(edgeRoadm1) || roadm.equals(edgeRoadm2)) {
                LOG.info("EDGE ROADM, cross connections needed between SRG and DEG");
                String spcRdmAD = rdmAddDropTplist.stream().filter(adp -> adp.contains(roadm)).findFirst()
                    .orElseThrow();
                LOG.info("AD port of ROADm {} = {}", roadm, spcRdmAD);
                // There should be only 1 AD and 1 DEG per roadm
                // TODO photonic media model should be updated to have the corresponding CEPs. I will just create
                //  3 different MC CEPs giving different IDs to show that they are different
                // Create 3 CEPs for each AD and DEG and the corresponding cross connections, matching the NEPs
                // created in the topology creation
                // add CEPs to the topology to the corresponding ONEP
                ConnectionEndPoint adCep1 = createCepRoadm(spcRdmAD, TapiStringConstants.PHTNC_MEDIA_OTS);
                putRdmCepInTopologyContext(roadm, spcRdmAD, TapiStringConstants.PHTNC_MEDIA_OTS, adCep1);
                ConnectionEndPoint adCep2 = createCepRoadm(spcRdmAD, TapiStringConstants.MC);
                putRdmCepInTopologyContext(roadm, spcRdmAD, TapiStringConstants.MC, adCep2);
                ConnectionEndPoint adCep3 = createCepRoadm(spcRdmAD, TapiStringConstants.OTSI_MC);
                putRdmCepInTopologyContext(roadm, spcRdmAD, TapiStringConstants.OTSI_MC, adCep3);
                cepMap.put(adCep1.key(), adCep1);
                cepMap.put(adCep2.key(), adCep2);
                cepMap.put(adCep3.key(), adCep3);

                String spcRdmDEG = rdmDegTplist.stream().filter(adp -> adp.contains(roadm)).findFirst().orElseThrow();
                LOG.info("Degree port of ROADm {} = {}", roadm, spcRdmDEG);

                ConnectionEndPoint degCep0 = createCepRoadm(spcRdmDEG, TapiStringConstants.PHTNC_MEDIA_OTS);
                putRdmCepInTopologyContext(roadm, spcRdmDEG, TapiStringConstants.PHTNC_MEDIA_OTS, degCep0);
                ConnectionEndPoint degCep1 = createCepRoadm(spcRdmDEG, TapiStringConstants.PHTNC_MEDIA_OMS);
                putRdmCepInTopologyContext(roadm, spcRdmDEG, TapiStringConstants.PHTNC_MEDIA_OMS, degCep1);
                ConnectionEndPoint degCep2 = createCepRoadm(spcRdmDEG, TapiStringConstants.MC);
                putRdmCepInTopologyContext(roadm, spcRdmDEG, TapiStringConstants.MC, degCep2);
                ConnectionEndPoint degCep3 = createCepRoadm(spcRdmDEG, TapiStringConstants.OTSI_MC);
                putRdmCepInTopologyContext(roadm, spcRdmDEG, TapiStringConstants.OTSI_MC, degCep3);
                cepMap.put(degCep0.key(), degCep0);
                cepMap.put(degCep1.key(), degCep1);
                cepMap.put(degCep2.key(), degCep2);
                cepMap.put(degCep3.key(), degCep3);

                LOG.info("Going to create cross connections for ROADM {}", roadm);
                // Create X connections between MC and OTSi_MC for full map
                org.opendaylight.yang.gen.v1.urn
                        .onf.otcc.yang.tapi.connectivity.rev221121.connectivity.context.Connection connection1
                    = createXCBetweenCeps(adCep2, degCep2, spcRdmAD, spcRdmDEG, TapiStringConstants.MC,
                        LayerProtocolName.PHOTONICMEDIA);
                LOG.info("Cross connection 1 created = {}", connection1.toString());
                org.opendaylight.yang.gen.v1.urn
                    .onf.otcc.yang.tapi.connectivity.rev221121.connectivity.context.Connection connection2
                    = createXCBetweenCeps(adCep3, degCep3, spcRdmAD, spcRdmDEG, TapiStringConstants.OTSI_MC,
                    LayerProtocolName.PHOTONICMEDIA);
                LOG.info("Cross connection 2 created = {}", connection2.toString());
                this.connectionFullMap.put(connection1.key(), connection1);
                this.connectionFullMap.put(connection2.key(), connection2);

                // Create X connections that will be added to the service object
                LowerConnection conn1 = new LowerConnectionBuilder().setConnectionUuid(connection1.getUuid()).build();
                LowerConnection conn2 = new LowerConnectionBuilder().setConnectionUuid(connection2.getUuid()).build();

                xcLowerMap.put(conn1.key(), conn1);
                xcLowerMap.put(conn2.key(), conn2);
            } else {
                LOG.info("MIDDLE ROADM, cross connections needed between DEG and DEG");
                String spcRdmDEG1 = rdmDegTplist.stream().filter(adp -> adp.contains(roadm)).findFirst().orElseThrow();
                LOG.info("Degree 1 port of ROADm {} = {}", roadm, spcRdmDEG1);

                ConnectionEndPoint deg1Cep0 = createCepRoadm(spcRdmDEG1, TapiStringConstants.PHTNC_MEDIA_OTS);
                putRdmCepInTopologyContext(roadm, spcRdmDEG1, TapiStringConstants.PHTNC_MEDIA_OTS, deg1Cep0);
                ConnectionEndPoint deg1Cep1 = createCepRoadm(spcRdmDEG1, TapiStringConstants.PHTNC_MEDIA_OMS);
                putRdmCepInTopologyContext(roadm, spcRdmDEG1, TapiStringConstants.PHTNC_MEDIA_OMS, deg1Cep1);
                ConnectionEndPoint deg1Cep2 = createCepRoadm(spcRdmDEG1, TapiStringConstants.MC);
                putRdmCepInTopologyContext(roadm, spcRdmDEG1, TapiStringConstants.MC, deg1Cep2);
                ConnectionEndPoint deg1Cep3 = createCepRoadm(spcRdmDEG1, TapiStringConstants.OTSI_MC);
                putRdmCepInTopologyContext(roadm, spcRdmDEG1, TapiStringConstants.OTSI_MC, deg1Cep3);
                cepMap.put(deg1Cep0.key(), deg1Cep0);
                cepMap.put(deg1Cep1.key(), deg1Cep1);
                cepMap.put(deg1Cep2.key(), deg1Cep2);
                cepMap.put(deg1Cep3.key(), deg1Cep3);

                String spcRdmDEG2 = rdmDegTplist.stream().filter(adp -> adp.contains(roadm)).skip(1).findFirst()
                    .orElseThrow();
                LOG.info("Degree 2 port of ROADm {} = {}", roadm, spcRdmDEG2);

                ConnectionEndPoint deg2Cep0 = createCepRoadm(spcRdmDEG2, TapiStringConstants.PHTNC_MEDIA_OTS);
                putRdmCepInTopologyContext(roadm, spcRdmDEG2, TapiStringConstants.PHTNC_MEDIA_OTS, deg2Cep0);
                ConnectionEndPoint deg2Cep1 = createCepRoadm(spcRdmDEG2, TapiStringConstants.PHTNC_MEDIA_OMS);
                putRdmCepInTopologyContext(roadm, spcRdmDEG2, TapiStringConstants.PHTNC_MEDIA_OMS, deg2Cep1);
                ConnectionEndPoint deg2Cep2 = createCepRoadm(spcRdmDEG2, TapiStringConstants.MC);
                putRdmCepInTopologyContext(roadm, spcRdmDEG2, TapiStringConstants.MC, deg2Cep2);
                ConnectionEndPoint deg2Cep3 = createCepRoadm(spcRdmDEG2, TapiStringConstants.OTSI_MC);
                putRdmCepInTopologyContext(roadm, spcRdmDEG2, TapiStringConstants.OTSI_MC, deg2Cep3);
                cepMap.put(deg2Cep0.key(), deg2Cep0);
                cepMap.put(deg2Cep1.key(), deg2Cep1);
                cepMap.put(deg2Cep2.key(), deg2Cep2);
                cepMap.put(deg2Cep3.key(), deg2Cep3);

                LOG.info("Going to create cross connections for ROADM {}", roadm);
                // Create X connections between MC and OTSi_MC for full map
                org.opendaylight.yang.gen.v1.urn
                    .onf.otcc.yang.tapi.connectivity.rev221121.connectivity.context.Connection connection1
                    = createXCBetweenCeps(deg1Cep2, deg2Cep2, spcRdmDEG1, spcRdmDEG2,
                    TapiStringConstants.MC, LayerProtocolName.PHOTONICMEDIA);
                LOG.info("Cross connection 1 created = {}", connection1.toString());
                org.opendaylight.yang.gen.v1.urn
                    .onf.otcc.yang.tapi.connectivity.rev221121.connectivity.context.Connection connection2
                    = createXCBetweenCeps(deg1Cep3, deg2Cep3, spcRdmDEG1, spcRdmDEG2,
                    TapiStringConstants.OTSI_MC, LayerProtocolName.PHOTONICMEDIA);
                LOG.info("Cross connection 2 created = {}", connection2.toString());
                this.connectionFullMap.put(connection1.key(), connection1);
                this.connectionFullMap.put(connection2.key(), connection2);

                // Create X connections that will be added to the service object
                LowerConnection conn1 = new LowerConnectionBuilder().setConnectionUuid(connection1.getUuid()).build();
                LowerConnection conn2 = new LowerConnectionBuilder().setConnectionUuid(connection2.getUuid()).build();

                xcLowerMap.put(conn1.key(), conn1);
                xcLowerMap.put(conn2.key(), conn2);
            }
        }
        LOG.info("Going to create top connections between roadms");
        String spcRdmAD1 = rdmAddDropTplist.stream().filter(adp -> adp.contains(edgeRoadm1)).findFirst().orElseThrow();
        String spcRdmAD2 = rdmAddDropTplist.stream().filter(adp -> adp.contains(edgeRoadm2)).findFirst().orElseThrow();
        // MC top connection between edge roadms
        LOG.info("Going to created top connection between MC");
        org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev221121.connectivity.context.Connection
            connection = createTopConnection(spcRdmAD1, spcRdmAD2, cepMap, TapiStringConstants.MC,
            LayerProtocolName.PHOTONICMEDIA, xcLowerMap, null);
        this.connectionFullMap.put(connection.key(), connection);
        LOG.info("Top connection created = {}", connection.toString());

        Map<ConnectionKey, Connection> connServMap = new HashMap<>();
        // OTSiMC top connections that will be added to the service object
        Connection conn = new ConnectionBuilder().setConnectionUuid(connection.getUuid()).build();
        connServMap.put(conn.key(), conn);
        LowerConnection conn1 = new LowerConnectionBuilder().setConnectionUuid(connection.getUuid()).build();
        Map<LowerConnectionKey, LowerConnection> topLowerMap = new HashMap<>();
        topLowerMap.put(conn1.key(), conn1);

        // OTSiMC top connection between edge roadms
        LOG.info("Going to created top connection between OTSiMC");
        org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev221121.connectivity.context.Connection
            connection1 = createTopConnection(spcRdmAD1, spcRdmAD2, cepMap, TapiStringConstants.OTSI_MC,
            LayerProtocolName.PHOTONICMEDIA, topLowerMap, null);
        this.connectionFullMap.put(connection1.key(), connection1);
        LOG.info("Top connection created = {}", connection1.toString());

        // OTSiMC top connections that will be added to the service object
        Connection conn2 = new ConnectionBuilder().setConnectionUuid(connection1.getUuid()).build();
        connServMap.put(conn2.key(), conn2);
        this.topConnRdmRdm = conn2;
        return connServMap;
    }

    private org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev221121.connectivity.context.Connection
            createTopConnection(String tp1, String tp2,
                        Map<org.opendaylight.yang.gen.v1.urn
                            .onf.otcc.yang.tapi.connectivity.rev221121.cep.list.ConnectionEndPointKey,
                            ConnectionEndPoint> cepMap, String qual, LayerProtocolName topPortocol,
                        Map<LowerConnectionKey, LowerConnection> xcMap, Connection additionalLowerConn) {
        // find cep for each AD MC of roadm 1 and 2
        LOG.info("Top connection name = {}", String.join("+", "TOP", tp1, tp2, qual));
        org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev221121.ConnectionEndPoint adCep1 =
            cepMap.get(new org.opendaylight.yang.gen.v1.urn
                .onf.otcc.yang.tapi.connectivity.rev221121.cep.list.ConnectionEndPointKey(
                new Uuid(UUID.nameUUIDFromBytes((String.join("+", "CEP", tp1.split("\\+")[0],
                        qual, tp1.split("\\+")[1])).getBytes(StandardCharsets.UTF_8))
                    .toString())));
        LOG.info("ADCEP1 = {}", adCep1);
        org.opendaylight.yang.gen.v1.urn
            .onf.otcc.yang.tapi.connectivity.rev221121.connection.ConnectionEndPoint cep1 =
            new org.opendaylight.yang.gen.v1.urn
                    .onf.otcc.yang.tapi.connectivity.rev221121.connection.ConnectionEndPointBuilder()
                .setNodeEdgePointUuid(adCep1.getParentNodeEdgePoint().getNodeEdgePointUuid())
                .setNodeUuid(adCep1.getParentNodeEdgePoint().getNodeUuid())
                .setTopologyUuid(adCep1.getParentNodeEdgePoint().getTopologyUuid())
                .setConnectionEndPointUuid(adCep1.getUuid())
                .build();
        org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev221121.ConnectionEndPoint adCep2 =
            cepMap.get(new org.opendaylight.yang.gen.v1.urn
                .onf.otcc.yang.tapi.connectivity.rev221121.cep.list.ConnectionEndPointKey(
                new Uuid(UUID.nameUUIDFromBytes((String.join("+", "CEP", tp2.split("\\+")[0],
                        qual, tp2.split("\\+")[1])).getBytes(StandardCharsets.UTF_8))
                    .toString())));
        LOG.info("ADCEP2 = {}", adCep2);
        org.opendaylight.yang.gen.v1.urn
            .onf.otcc.yang.tapi.connectivity.rev221121.connection.ConnectionEndPoint cep2 =
            new org.opendaylight.yang.gen.v1.urn
                    .onf.otcc.yang.tapi.connectivity.rev221121.connection.ConnectionEndPointBuilder()
                .setNodeEdgePointUuid(adCep2.getParentNodeEdgePoint().getNodeEdgePointUuid())
                .setNodeUuid(adCep2.getParentNodeEdgePoint().getNodeUuid())
                .setTopologyUuid(adCep2.getParentNodeEdgePoint().getTopologyUuid())
                .setConnectionEndPointUuid(adCep1.getUuid())
                .build();
        Map<ConnectionEndPointKey, org.opendaylight.yang.gen.v1.urn
            .onf.otcc.yang.tapi.connectivity.rev221121.connection.ConnectionEndPoint> ceps = new HashMap<>();
        ceps.put(cep1.key(), cep1);
        ceps.put(cep2.key(), cep2);
        Name connName = new NameBuilder()
            .setValueName("Connection name")
            .setValue(String.join("+", "TOP", tp1, tp2, qual))
            .build();
        // TODO: lower connection, supported link.......
        if (additionalLowerConn != null) {
            xcMap.putIfAbsent(new LowerConnectionKey(additionalLowerConn.getConnectionUuid()),
                new LowerConnectionBuilder().setConnectionUuid(additionalLowerConn.getConnectionUuid()).build());
        }
        return new org.opendaylight.yang.gen.v1.urn
                .onf.otcc.yang.tapi.connectivity.rev221121.connectivity.context.ConnectionBuilder()
            .setUuid(new Uuid(UUID.nameUUIDFromBytes((String.join("+", "TOP", tp1, tp2, qual))
                .getBytes(StandardCharsets.UTF_8)).toString()))
            .setName(Map.of(connName.key(), connName))
            .setConnectionEndPoint(ceps)
            .setOperationalState(OperationalState.ENABLED)
            .setLayerProtocolName(topPortocol)
            .setLifecycleState(LifecycleState.INSTALLED)
            .setDirection(ForwardingDirection.BIDIRECTIONAL)
            .setLowerConnection(xcMap)
            .build();
    }

    private org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev221121.connectivity.context.Connection
            createXCBetweenCeps(ConnectionEndPoint cep1, ConnectionEndPoint cep2, String tp1, String tp2, String qual,
                        LayerProtocolName xcProtocol) {
        LOG.info("Creation cross connection between: {} and {}", tp1, tp2);
        LOG.info("Cross connection name = {}", String.join("+", "XC", tp1, tp2, qual));
        LOG.debug("Parent NEP of CEP1 = {}", cep1.getParentNodeEdgePoint().toString());
        LOG.debug("Parent NEP CEP2 = {}", cep2.getParentNodeEdgePoint().toString());
        org.opendaylight.yang.gen.v1.urn
                .onf.otcc.yang.tapi.connectivity.rev221121.connection.ConnectionEndPoint cepServ1 =
            new org.opendaylight.yang.gen.v1.urn
                .onf.otcc.yang.tapi.connectivity.rev221121.connection.ConnectionEndPointBuilder()
            .setNodeEdgePointUuid(cep1.getParentNodeEdgePoint().getNodeEdgePointUuid())
            .setNodeUuid(cep1.getParentNodeEdgePoint().getNodeUuid())
            .setTopologyUuid(cep1.getParentNodeEdgePoint().getTopologyUuid())
            .setConnectionEndPointUuid(cep1.getUuid())
            .build();
        org.opendaylight.yang.gen.v1.urn
                .onf.otcc.yang.tapi.connectivity.rev221121.connection.ConnectionEndPoint cepServ2 =
            new org.opendaylight.yang.gen.v1.urn
                .onf.otcc.yang.tapi.connectivity.rev221121.connection.ConnectionEndPointBuilder()
            .setNodeEdgePointUuid(cep2.getParentNodeEdgePoint().getNodeEdgePointUuid())
            .setNodeUuid(cep2.getParentNodeEdgePoint().getNodeUuid())
            .setTopologyUuid(cep2.getParentNodeEdgePoint().getTopologyUuid())
            .setConnectionEndPointUuid(cep2.getUuid())
            .build();
        Map<ConnectionEndPointKey, org.opendaylight.yang.gen.v1.urn
            .onf.otcc.yang.tapi.connectivity.rev221121.connection.ConnectionEndPoint> ceps = new HashMap<>();
        ceps.put(cepServ1.key(), cepServ1);
        ceps.put(cepServ2.key(), cepServ2);
        Name connName = new NameBuilder()
            .setValueName("Connection name")
            .setValue(String.join("+", "XC", tp1, tp2, qual))
            .build();
        // TODO: lower connection, supported link.......
        return new org.opendaylight.yang.gen.v1.urn
                .onf.otcc.yang.tapi.connectivity.rev221121.connectivity.context.ConnectionBuilder()
            .setUuid(new Uuid(UUID.nameUUIDFromBytes((String.join("+", "XC", tp1, tp2, qual))
                .getBytes(StandardCharsets.UTF_8)).toString()))
            .setName(Map.of(connName.key(), connName))
            .setConnectionEndPoint(ceps)
            .setOperationalState(OperationalState.ENABLED)
            .setLayerProtocolName(xcProtocol)
            .setLifecycleState(LifecycleState.INSTALLED)
            .setDirection(ForwardingDirection.BIDIRECTIONAL)
            .build();
    }

    private ConnectionEndPoint createCepRoadm(String id, String qualifier) {
        LOG.info("NEP = {}", String.join("+", id.split("\\+")[0], qualifier, id.split("\\+")[1]));
        Name cepName = new NameBuilder()
            .setValueName("ConnectionEndPoint name")
            .setValue(String.join("+", id.split("\\+")[0], qualifier,
                id.split("\\+")[1]))
            .build();
        ParentNodeEdgePoint pnep = new ParentNodeEdgePointBuilder()
            .setNodeEdgePointUuid(new Uuid(UUID.nameUUIDFromBytes((String.join("+", id.split("\\+")[0],
                    qualifier, id.split("\\+")[1])).getBytes(StandardCharsets.UTF_8))
                .toString()))
            .setNodeUuid(new Uuid(UUID.nameUUIDFromBytes((String.join("+",id.split("\\+")[0],
                    TapiStringConstants.PHTNC_MEDIA)).getBytes(StandardCharsets.UTF_8))
                .toString()))
            .setTopologyUuid(new Uuid(UUID.nameUUIDFromBytes(TapiStringConstants.T0_FULL_MULTILAYER
                .getBytes(StandardCharsets.UTF_8)).toString()))
            .build();
        String clientQualifier = "";
        switch (qualifier) {
            case TapiStringConstants.PHTNC_MEDIA_OTS:
                clientQualifier = TapiStringConstants.PHTNC_MEDIA_OMS;
                break;
            case TapiStringConstants.PHTNC_MEDIA_OMS:
                clientQualifier = TapiStringConstants.MC;
                OwnedNodeEdgePoint onepMC = createRoadmNep(id.split("\\+")[0], id.split("\\+")[1],
                    false, OperationalState.ENABLED, AdministrativeState.UNLOCKED, clientQualifier);
                putRdmNepInTopologyContext(id.split("\\+")[0], id.split("\\+")[1], TapiStringConstants.MC, onepMC);
                break;
            case TapiStringConstants.MC:
                clientQualifier = TapiStringConstants.OTSI_MC;
                OwnedNodeEdgePoint onepOTSiMC = createRoadmNep(id.split("\\+")[0], id.split("\\+")[1],
                    false, OperationalState.ENABLED, AdministrativeState.UNLOCKED, clientQualifier);
                putRdmNepInTopologyContext(id.split("\\+")[0], id.split("\\+")[1],
                    TapiStringConstants.OTSI_MC, onepOTSiMC);
                break;
            default:
                LOG.debug("not currently handling client NEP for OTSiMC CEP {}",
                    String.join("+", id.split("\\+")[0], qualifier, id.split("\\+")[1]));
                break;
        }
        ClientNodeEdgePoint cnep = new ClientNodeEdgePointBuilder()
            .setNodeEdgePointUuid(new Uuid(UUID.nameUUIDFromBytes((String.join("+", id.split("\\+")[0],
                    clientQualifier, id.split("\\+")[1])).getBytes(StandardCharsets.UTF_8))
                .toString()))
            .setNodeUuid(new Uuid(UUID.nameUUIDFromBytes((String.join("+",id.split("\\+")[0],
                    TapiStringConstants.PHTNC_MEDIA)).getBytes(StandardCharsets.UTF_8))
                .toString()))
            .setTopologyUuid(new Uuid(UUID.nameUUIDFromBytes(TapiStringConstants.T0_FULL_MULTILAYER
                .getBytes(StandardCharsets.UTF_8)).toString()))
            .build();
        // TODO: add augmentation with the corresponding cep-spec (i.e. MC, OTSiMC...)
        // TODO: add parent ONEP??
        ConnectionEndPointBuilder cepBldr = new ConnectionEndPointBuilder()
            .setUuid(new Uuid(UUID.nameUUIDFromBytes((String.join("+", "CEP", id.split("\\+")[0],
                    qualifier, id.split("\\+")[1])).getBytes(StandardCharsets.UTF_8))
                .toString()))
            .setParentNodeEdgePoint(pnep)
            .setName(Map.of(cepName.key(), cepName))
            .setConnectionPortRole(PortRole.SYMMETRIC)
            .setDirection(Direction.BIDIRECTIONAL)
            .setOperationalState(OperationalState.ENABLED)
            .setLifecycleState(LifecycleState.INSTALLED)
            .setLayerProtocolName(LayerProtocolName.PHOTONICMEDIA);
        if (!(TapiStringConstants.OTSI_MC.equals(qualifier))) {
            cepBldr.setClientNodeEdgePoint(Map.of(cnep.key(), cnep));
        }
        return cepBldr.build();
    }

    private ConnectionEndPoint createCepXpdr(String id, String qualifier, String nodeLayer,
                                             LayerProtocolName cepProtocol) {
        Name cepName = new NameBuilder()
            .setValueName("ConnectionEndPoint name")
            .setValue(String.join("+", id.split("\\+")[0], qualifier,
                id.split("\\+")[1]))
            .build();
        ParentNodeEdgePoint pnep = new ParentNodeEdgePointBuilder()
            .setNodeEdgePointUuid(new Uuid(UUID.nameUUIDFromBytes((String.join("+", id.split("\\+")[0],
                    qualifier, id.split("\\+")[1])).getBytes(StandardCharsets.UTF_8))
                .toString()))
            .setNodeUuid(new Uuid(UUID.nameUUIDFromBytes((String.join("+",id.split("\\+")[0],
                    TapiStringConstants.XPDR)).getBytes(StandardCharsets.UTF_8))
                .toString()))
            .setTopologyUuid(new Uuid(UUID.nameUUIDFromBytes(TapiStringConstants.T0_FULL_MULTILAYER
                .getBytes(StandardCharsets.UTF_8)).toString()))
            .build();
        String clientQualifier = "";
        String clientNodeLayer = TapiStringConstants.PHTNC_MEDIA;
        switch (qualifier) {
            case TapiStringConstants.PHTNC_MEDIA_OTS:
                clientQualifier = TapiStringConstants.OTSI_MC;
                break;
            case TapiStringConstants.OTSI_MC:
                clientQualifier = TapiStringConstants.E_ODU;
                clientNodeLayer = TapiStringConstants.ODU;
                break;
            case TapiStringConstants.E_ODU:
                clientQualifier = TapiStringConstants.DSR;
                clientNodeLayer = TapiStringConstants.DSR;
                break;
            default :
                LOG.debug("no client CEP for DSR NEP {}",
                    String.join("+", id.split("\\+")[0], qualifier, id.split("\\+")[1]));
                break;
        }
        ClientNodeEdgePoint cnep = new ClientNodeEdgePointBuilder()
            .setNodeEdgePointUuid(new Uuid(UUID.nameUUIDFromBytes((String.join("+", id.split("\\+")[0],
                    clientQualifier, id.split("\\+")[1])).getBytes(StandardCharsets.UTF_8))
                .toString()))
            .setNodeUuid(new Uuid(UUID.nameUUIDFromBytes((String.join("+",id.split("\\+")[0],
                    TapiStringConstants.XPDR)).getBytes(StandardCharsets.UTF_8))
                .toString()))
            .setTopologyUuid(new Uuid(UUID.nameUUIDFromBytes(TapiStringConstants.T0_FULL_MULTILAYER
                .getBytes(StandardCharsets.UTF_8)).toString()))
            .build();
        // TODO: add augmentation with the corresponding cep-spec (i.e. MC, OTSiMC...)
        // TODO: add parent ONEP??
        ConnectionEndPointBuilder cepBldr = new ConnectionEndPointBuilder()
            .setUuid(new Uuid(UUID.nameUUIDFromBytes((String.join("+", "CEP", id.split("\\+")[0],
                    qualifier, id.split("\\+")[1])).getBytes(StandardCharsets.UTF_8))
                .toString()))
            .setParentNodeEdgePoint(pnep)
            .setName(Map.of(cepName.key(), cepName))
            .setConnectionPortRole(PortRole.SYMMETRIC)
            .setDirection(Direction.BIDIRECTIONAL)
            .setOperationalState(OperationalState.ENABLED)
            .setLifecycleState(LifecycleState.INSTALLED)
            .setLayerProtocolName(cepProtocol);
        if (!(TapiStringConstants.DSR.equals(qualifier))) {
            cepBldr.setClientNodeEdgePoint(Map.of(cnep.key(), cnep));
        }
        return cepBldr.build();
    }

    private EndPoint mapServiceZEndPoint(
        org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev230526.service.ServiceZEnd
            serviceZEnd, PathDescription pathDescription) {
        EndPointBuilder endPointBuilder = new EndPointBuilder();
        // 1. Service Format: ODU, OTU, ETH
        ServiceFormat serviceFormat = serviceZEnd.getServiceFormat();
        String serviceNodeId = serviceZEnd.getNodeId().getValue();
        // Identify SIP name
        Uuid sipUuid = getSipIdFromZend(pathDescription.getZToADirection().getZToA(), serviceNodeId, serviceFormat);
        LOG.info("Uuid of z end {}", sipUuid);
        LayerProtocolName layerProtocols = null;
        // Layer protocol name
        switch (serviceFormat) {
            case Ethernet:
                layerProtocols = LayerProtocolName.DSR;
                break;
            case OTU:
            case OC:
                layerProtocols = LayerProtocolName.PHOTONICMEDIA;
                break;
            case ODU:
                layerProtocols = LayerProtocolName.ODU;
                break;
            default:
                LOG.error("Service Format not supported");
        }
        org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.local._class.Name name =
            new org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.local._class.NameBuilder()
                .setValueName("OpenROADM info")
                .setValue(String.join("-", serviceZEnd.getClli(),
                    serviceZEnd.getTxDirection().values().stream().findFirst().orElseThrow().getPort()
                        .getPortDeviceName(),
                    serviceZEnd.getTxDirection().values().stream().findFirst().orElseThrow().getPort().getPortName()))
                .build();
        return endPointBuilder
            .setServiceInterfacePoint(new ServiceInterfacePointBuilder()
                .setServiceInterfacePointUuid(sipUuid)
                .build())
            .setName(Map.of(name.key(), name))
            .setAdministrativeState(AdministrativeState.UNLOCKED)
            .setDirection(Direction.BIDIRECTIONAL)
            .setLifecycleState(LifecycleState.INSTALLED)
            .setOperationalState(OperationalState.ENABLED)
            .setLayerProtocolName(layerProtocols)
            .setCapacity(new CapacityBuilder()
                .setTotalSize(new TotalSizeBuilder()
                    .setValue(Decimal64.valueOf(BigDecimal.valueOf(serviceZEnd.getServiceRate().doubleValue())))
                    .setUnit(CAPACITYUNITGBPS.VALUE)
                    .build())
//                .setBandwidthProfile(new BandwidthProfileBuilder().build())
                .build())
            .setProtectionRole(ProtectionRole.WORK)
            .setRole(PortRole.SYMMETRIC)
            .setLocalId(serviceZEnd.getTxDirection().values().stream().findFirst().orElseThrow()
                .getPort().getPortDeviceName())
            .build();
    }

    private EndPoint mapServiceAEndPoint(
        org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev230526.service.ServiceAEnd
            serviceAEnd, PathDescription pathDescription) {
        EndPointBuilder endPointBuilder = new EndPointBuilder();
        // 1. Service Format: ODU, OTU, ETH
        ServiceFormat serviceFormat = serviceAEnd.getServiceFormat();
        String serviceNodeId = serviceAEnd.getNodeId().getValue();
        // Identify SIP name
        Uuid sipUuid = getSipIdFromAend(pathDescription.getAToZDirection().getAToZ(), serviceNodeId, serviceFormat);
        LOG.info("Uuid of a end {}", sipUuid);
        LayerProtocolName layerProtocols = null;
        // Layer protocol name
        switch (serviceFormat) {
            case Ethernet:
                layerProtocols = LayerProtocolName.DSR;
                break;
            case OTU:
            case OC:
                layerProtocols = LayerProtocolName.PHOTONICMEDIA;
                break;
            case ODU:
                layerProtocols = LayerProtocolName.ODU;
                break;
            default:
                LOG.error("Service Format not supported");
        }
        org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.local._class.Name name =
            new org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.local._class.NameBuilder()
                .setValueName("OpenROADM info")
                .setValue(String.join("-", serviceAEnd.getClli(),
                    serviceAEnd.getTxDirection().values().stream().findFirst().orElseThrow().getPort()
                        .getPortDeviceName(),
                    serviceAEnd.getTxDirection().values().stream().findFirst().orElseThrow().getPort().getPortName()))
                .build();
        return endPointBuilder
            .setServiceInterfacePoint(new ServiceInterfacePointBuilder()
                .setServiceInterfacePointUuid(sipUuid)
                .build())
            .setName(Map.of(name.key(), name))
            .setAdministrativeState(AdministrativeState.UNLOCKED)
            .setDirection(Direction.BIDIRECTIONAL)
            .setLifecycleState(LifecycleState.INSTALLED)
            .setOperationalState(OperationalState.ENABLED)
            .setLayerProtocolName(layerProtocols)
            .setCapacity(new CapacityBuilder()
                .setTotalSize(new TotalSizeBuilder()
                    .setValue(Decimal64.valueOf(BigDecimal.valueOf(serviceAEnd.getServiceRate().doubleValue())))
                    .setUnit(CAPACITYUNITGBPS.VALUE)
                    .build())
//                .setBandwidthProfile(new BandwidthProfileBuilder().build()) // TODO: implement bandwidth profile
                .build())
            .setProtectionRole(ProtectionRole.WORK)
            .setRole(PortRole.SYMMETRIC)
            .setLocalId(serviceAEnd.getTxDirection().values().stream().findFirst().orElseThrow().getPort()
                    .getPortDeviceName())
            .build();
    }

    private Uuid getSipIdFromZend(Map<ZToAKey, ZToA> mapztoa, String serviceNodeId, ServiceFormat serviceFormat) {
        Uuid zendUuid = null;
        if (serviceNodeId.contains("ROADM")) {
            // Service from ROADM to ROADM
            // AddDrop-AddDrop ports --> MC layer SIPs
            ZToA firstElement = mapztoa.values().stream().filter(ztoa -> ztoa.getId().equals("0")).findFirst()
                    .orElseThrow();
            TerminationPoint tp = (TerminationPoint) firstElement.getResource().getResource();
            Uuid sipUuid = new Uuid(UUID.nameUUIDFromBytes((String.join("+", "SIP",
                    tp.getTpNodeId(), TapiStringConstants.MC, tp.getTpId())).getBytes(StandardCharsets.UTF_8))
                .toString());
            LOG.info("SIP name = {}", String.join("+", tp.getTpNodeId(), TapiStringConstants.MC, tp.getTpId()));
            for (ServiceInterfacePoint sip:this.sipMap.values()) {
                if (!sip.getUuid().equals(sipUuid)) {
                    LOG.info("SIP {} doesn match sipname {}", sip.getUuid().getValue(), sipUuid.getValue());
                    continue;
                }
                zendUuid = sip.getUuid();
                break;
            }
        } else {
            // Service from XPDR to XPDR
            ZToA firstElement;
            TerminationPoint tp;
            Uuid sipUuid;
            switch (serviceFormat) {
                case ODU:
                    firstElement = mapztoa.values().stream().filter(ztoa -> ztoa.getId().equals("2")).findFirst()
                            .orElseThrow();
                    tp = (TerminationPoint) firstElement.getResource().getResource();
                    // Network-Network ports --> iODU layer SIPs TODO --> updated to E_ODU
                    sipUuid = new Uuid(UUID.nameUUIDFromBytes((String.join("+", "SIP", tp.getTpNodeId(),
                            TapiStringConstants.I_ODU, tp.getTpId())).getBytes(StandardCharsets.UTF_8)).toString());
                    LOG.info("SIP name = {}", String.join("+", tp.getTpNodeId(), TapiStringConstants.I_ODU,
                        tp.getTpId()));
                    break;
                case OTU:
                    firstElement = mapztoa.values().stream().filter(ztoa -> ztoa.getId().equals("2")).findFirst()
                            .orElseThrow();
                    tp = (TerminationPoint) firstElement.getResource().getResource();
                    // Network-Network ports --> iOTSi layer SIPs
                    sipUuid = new Uuid(UUID.nameUUIDFromBytes((String.join("+", "SIP", tp.getTpNodeId(),
                            TapiStringConstants.I_OTSI, tp.getTpId())).getBytes(StandardCharsets.UTF_8)).toString());
                    LOG.info("SIP name = {}", String.join("+", tp.getTpNodeId(), TapiStringConstants.I_OTSI,
                        tp.getTpId()));
                    break;
                case Ethernet:
                    LOG.info("Elements ZA = {}", mapztoa.values().toString());
                    firstElement = mapztoa.values().stream().filter(ztoa -> ztoa.getId().equals("0")).findFirst()
                            .orElseThrow();
                    tp = (TerminationPoint) firstElement.getResource().getResource();
                    // Client-client ports --> DSR layer SIPs
                    sipUuid = new Uuid(UUID.nameUUIDFromBytes((String.join("+", "SIP", tp.getTpNodeId(),
                            TapiStringConstants.DSR, tp.getTpId())).getBytes(StandardCharsets.UTF_8)).toString());
                    LOG.info("SIP name = {}", String.join("+", tp.getTpNodeId(), TapiStringConstants.DSR,
                        tp.getTpId()));
                    break;
                default:
                    sipUuid = null;
                    LOG.warn("Service format {} not supported (?)", serviceFormat.getName());
            }
            for (ServiceInterfacePoint sip:this.sipMap.values()) {
                if (!sip.getUuid().equals(sipUuid)) {
                    LOG.info("SIP {} doesn match sipname {}", sip.getUuid().getValue(), sipUuid.getValue());
                    continue;
                }
                zendUuid = sip.getUuid();
                break;
            }
        }
        return zendUuid;
    }

    private Uuid getSipIdFromAend(Map<AToZKey, AToZ> mapatoz, String serviceNodeId, ServiceFormat serviceFormat) {
        Uuid aendUuid = null;
        LOG.info("ServiceNode = {} and ServiceFormat = {}", serviceNodeId, serviceFormat.getName());
        LOG.info("Map a to z = {}", mapatoz);
        if (serviceNodeId.contains("ROADM")) {
            // Service from ROADM to ROADM
            // AddDrop-AddDrop ports --> MC layer SIPs
            AToZ firstElement = mapatoz.values().stream().filter(atoz -> atoz.getId().equals("0")).findFirst()
                    .orElseThrow();
            LOG.info("First element of service path = {}", firstElement.getResource().getResource());
            TerminationPoint tp = (TerminationPoint) firstElement.getResource().getResource();
            Uuid sipUuid = new Uuid(UUID.nameUUIDFromBytes((String.join("+", "SIP",
                    tp.getTpNodeId(), TapiStringConstants.MC, tp.getTpId())).getBytes(StandardCharsets.UTF_8))
                .toString());
            LOG.info("ROADM SIP name = {}", String.join("+", tp.getTpNodeId(), TapiStringConstants.MC,
                tp.getTpId()));
            for (ServiceInterfacePoint sip:this.sipMap.values()) {
                if (!sip.getUuid().equals(sipUuid)) {
                    LOG.info("SIP {} doesn match sipname {}", sip.getUuid().getValue(), sipUuid.getValue());
                    continue;
                }
                aendUuid = sip.getUuid();
                break;
            }
        } else {
            // Service from XPDR to XPDR
            AToZ firstElement;
            TerminationPoint tp;
            Uuid sipUuid;
            switch (serviceFormat) {
                case ODU:
                    firstElement = mapatoz.values().stream().filter(atoz -> atoz.getId().equals("2")).findFirst()
                            .orElseThrow();
                    tp = (TerminationPoint) firstElement.getResource().getResource();
                    // Network-Network ports --> iODU layer SIPs. TODO -> updated to eODU
                    sipUuid = new Uuid(UUID.nameUUIDFromBytes((String.join("+", "SIP", tp.getTpNodeId(),
                            TapiStringConstants.I_ODU, tp.getTpId())).getBytes(StandardCharsets.UTF_8)).toString());
                    LOG.info("ODU XPDR SIP name = {}", String.join("+", tp.getTpNodeId(),
                        TapiStringConstants.I_ODU, tp.getTpId()));
                    break;
                case OTU:
                    firstElement = mapatoz.values().stream().filter(atoz -> atoz.getId().equals("2")).findFirst()
                            .orElseThrow();
                    tp = (TerminationPoint) firstElement.getResource().getResource();
                    // Network-Network ports --> iOTSi layer SIPs
                    sipUuid = new Uuid(UUID.nameUUIDFromBytes((String.join("+", "SIP", tp.getTpNodeId(),
                            TapiStringConstants.I_OTSI, tp.getTpId())).getBytes(StandardCharsets.UTF_8)).toString());
                    LOG.info("OTU XPDR SIP name = {}", String.join("+", tp.getTpNodeId(),
                        TapiStringConstants.I_OTSI, tp.getTpId()));
                    break;
                case Ethernet:
                    LOG.info("Elements AZ = {}", mapatoz.values().toString());
                    firstElement = mapatoz.values().stream().filter(atoz -> atoz.getId().equals("0")).findFirst()
                            .orElseThrow();
                    tp = (TerminationPoint) firstElement.getResource().getResource();
                    // Client-client ports --> DSR layer SIPs
                    sipUuid = new Uuid(UUID.nameUUIDFromBytes((String.join("+", "SIP", tp.getTpNodeId(),
                            TapiStringConstants.DSR, tp.getTpId())).getBytes(StandardCharsets.UTF_8)).toString());
                    LOG.info("DSR XPDR SIP name = {}", String.join("+", tp.getTpNodeId(),
                        TapiStringConstants.DSR, tp.getTpId()));
                    break;
                default:
                    sipUuid = null;
                    LOG.warn("Service format {} not supported (?)", serviceFormat.getName());
            }
            for (ServiceInterfacePoint sip:this.sipMap.values()) {
                if (!sip.getUuid().equals(sipUuid)) {
                    LOG.info("SIP {} doesn match sipname {}", sip.getUuid().getValue(), sipUuid.getValue());
                    continue;
                }
                aendUuid = sip.getUuid();
                break;
            }
        }
        return aendUuid;
    }

    private void putRdmCepInTopologyContext(String node, String spcRdmAD, String qual, ConnectionEndPoint cep) {
        LOG.info("NEP id before Merge = {}", String.join("+", node, qual, spcRdmAD.split("\\+")[1]));
        LOG.info("Node of NEP id before Merge = {}", String.join("+", node, TapiStringConstants.PHTNC_MEDIA));
        // Give uuids so that it is easier to look for things: topology uuid, node uuid, nep uuid, cep
        Uuid topoUuid = new Uuid(UUID.nameUUIDFromBytes(TapiStringConstants.T0_FULL_MULTILAYER
            .getBytes(StandardCharsets.UTF_8)).toString());
        Uuid nodeUuid = new Uuid(UUID.nameUUIDFromBytes(String.join("+", node, TapiStringConstants.PHTNC_MEDIA)
            .getBytes(StandardCharsets.UTF_8)).toString());
        Uuid nepUuid = new Uuid(UUID.nameUUIDFromBytes(String.join("+", node, qual, spcRdmAD.split("\\+")[1])
            .getBytes(StandardCharsets.UTF_8)).toString());
        this.tapiContext.updateTopologyWithCep(topoUuid, nodeUuid, nepUuid, cep);
    }

    private void putXpdrCepInTopologyContext(String node, String spcXpdrNet, String qual, String nodeLayer,
                                             ConnectionEndPoint cep) {
        // Give uuids so that it is easier to look for things: topology uuid, node uuid, nep uuid, cep
        Uuid topoUuid = new Uuid(UUID.nameUUIDFromBytes(TapiStringConstants.T0_FULL_MULTILAYER
            .getBytes(StandardCharsets.UTF_8)).toString());
        Uuid nodeUuid = new Uuid(UUID.nameUUIDFromBytes(String.join("+", node, nodeLayer)
            .getBytes(StandardCharsets.UTF_8)).toString());
        Uuid nepUuid = new Uuid(UUID.nameUUIDFromBytes(String.join("+", node, qual, spcXpdrNet.split("\\+")[1])
            .getBytes(StandardCharsets.UTF_8)).toString());
        this.tapiContext.updateTopologyWithCep(topoUuid, nodeUuid, nepUuid, cep);
    }


    private void putRdmNepInTopologyContext(String orNodeId, String orTpId, String qual, OwnedNodeEdgePoint onep) {
        LOG.info("NEP id before Merge = {}", String.join("+", orNodeId, qual, orTpId));
        LOG.info("Node of NEP id before Merge = {}", String.join("+", orNodeId, TapiStringConstants.PHTNC_MEDIA));
        // Give uuids so that it is easier to look for things: topology uuid, node uuid, nep uuid, cep
        Uuid topoUuid = new Uuid(UUID.nameUUIDFromBytes(TapiStringConstants.T0_FULL_MULTILAYER
            .getBytes(Charset.forName("UTF-8"))).toString());
        Uuid nodeUuid = new Uuid(UUID.nameUUIDFromBytes(String.join("+", orNodeId, TapiStringConstants.PHTNC_MEDIA)
            .getBytes(Charset.forName("UTF-8"))).toString());
        Uuid nepUuid = new Uuid(UUID.nameUUIDFromBytes(String.join("+", orNodeId, qual, orTpId)
            .getBytes(Charset.forName("UTF-8"))).toString());
        updateTopologyWithNep(topoUuid, nodeUuid, nepUuid, onep);
    }

    private String getIdBasedOnModelVersion(String nodeid) {
        return nodeid.matches("[A-Z]{5}-[A-Z0-9]{2}-.*") ? String.join("-", nodeid.split("-")[0],
            nodeid.split("-")[1]) : nodeid.split("-")[0];
    }

    private ServiceZEnd tapiEndPointToServiceZPoint(
        org.opendaylight.yang.gen.v1.urn
            .onf.otcc.yang.tapi.connectivity.rev221121.create.connectivity.service.input.EndPoint endPoint,
        ServiceFormat serviceFormat, String nodeZid, Uint64 capacity, LayerProtocolName serviceLayer) {
        // TODO -> change way this is being created. The name includes only SPDR-SA1-XPDR1.
        //  Not the rest which is needed in the txPortDeviceName.
        //  It could be obtained from the SIP which has the NEP and includes all the OR name.
        Uuid sipUuid = endPoint.getServiceInterfacePoint().getServiceInterfacePointUuid();
        // Todo -> need to find the NEP associated to that SIP
        Uuid nodeUuid = new Uuid(UUID.nameUUIDFromBytes(nodeZid.getBytes(StandardCharsets.UTF_8)).toString());
        org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.topology.Node node =
            this.tapiContext.getTapiNode(this.tapiTopoUuid, nodeUuid);
        if (node == null) {
            LOG.error("Node not found in datastore");
            return null;
        }
        // TODO -> in case of a DSR service, for some requests we need the NETWORK PORT and not the CLIENT although the
        //  connection is between 2 CLIENT ports. Otherwise it will not work...
        OwnedNodeEdgePoint nep = null;
        for (OwnedNodeEdgePoint onep : node.getOwnedNodeEdgePoint().values()) {
            if (onep.getMappedServiceInterfacePoint() == null) {
                continue;
            }
            if (onep.getMappedServiceInterfacePoint().containsKey(new MappedServiceInterfacePointKey(sipUuid))) {
                nep = onep;
                break;
            }
        }
        if (nep == null) {
            LOG.error("Nep not found in datastore");
            return null;
        }
        String nodeName = "";
        for (Map.Entry<org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.local._class.NameKey,
                org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.local._class.Name> entry:
                endPoint.getName().entrySet()) {
            if (!("Node Type").equals(entry.getValue().getValueName())) {
                nodeName = entry.getValue().getValue();
            }
        }
//        String nodeName = endPoint.getName().values().stream().findFirst().orElseThrow().getValue();
        String nodeid = String.join("-", nodeName.split("-")[0], nodeName.split("-")[1]);
        String nepName = nep.getName().values().stream().findFirst().orElseThrow().getValue();
        String txPortDeviceName = nepName.split("\\+")[0];
        String txPortName = nepName.split("\\+")[2];
        String rxPortDeviceName = txPortDeviceName;
        String rxPortName = txPortName;
        LOG.debug("Node z id = {}, txportDeviceName = {}, txPortName = {}", nodeid, txPortDeviceName, txPortName);
        LOG.debug("Node z id = {}, rxportDeviceName = {}, rxPortName = {}", nodeid, rxPortDeviceName, rxPortName);
        // TODO --> get clli from datastore?
        String clli = "NodeSC";
        LOG.info("Node z id = {}, txportDeviceName = {}, txPortName = {}", nodeid, txPortDeviceName, txPortName);
        LOG.info("Node z id = {}, rxportDeviceName = {}, rxPortName = {}", nodeid, rxPortDeviceName, rxPortName);
        ServiceZEndBuilder serviceZEndBuilder = new ServiceZEndBuilder()
            .setClli(clli)
            .setNodeId(new NodeIdType(nodeid))
            .setOpticType(OpticTypes.Gray)
            .setServiceFormat(serviceFormat)
            .setServiceRate(Uint32.valueOf(capacity))
            .setEthernetAttributes(new EthernetAttributesBuilder().setSubrateEthSla(new SubrateEthSlaBuilder()
                    .setCommittedBurstSize(Uint16.valueOf(64))
                    .setCommittedInfoRate(Uint32.valueOf(100000))
                    .build())
                .build())
            .setTxDirection(Map.of(new TxDirectionKey(Uint8.ZERO), new TxDirectionBuilder()
                .setPort(new PortBuilder()
                    .setPortDeviceName(txPortDeviceName)
                    .setPortName(txPortName)
                    .setPortRack(TapiStringConstants.PORT_RACK_VALUE)
                    .setPortShelf("00")
                    .setPortType(TapiStringConstants.PORT_TYPE)
                    .build())
                .setLgx(new LgxBuilder()
                    .setLgxDeviceName(TapiStringConstants.LGX_DEVICE_NAME)
                    .setLgxPortName(TapiStringConstants.LGX_PORT_NAME)
                    .setLgxPortRack(TapiStringConstants.PORT_RACK_VALUE)
                    .setLgxPortShelf("00")
                    .build())
                .setIndex(Uint8.ZERO)
                .build()))
            .setRxDirection(Map.of(new RxDirectionKey(Uint8.ZERO), new RxDirectionBuilder()
                .setPort(new PortBuilder()
                    .setPortDeviceName(rxPortDeviceName)
                    .setPortName(rxPortName)
                    .setPortRack(TapiStringConstants.PORT_RACK_VALUE)
                    .setPortShelf("00")
                    .setPortType(TapiStringConstants.PORT_TYPE)
                    .build())
                .setLgx(new LgxBuilder()
                    .setLgxDeviceName(TapiStringConstants.LGX_DEVICE_NAME)
                    .setLgxPortName(TapiStringConstants.LGX_PORT_NAME)
                    .setLgxPortRack(TapiStringConstants.PORT_RACK_VALUE)
                    .setLgxPortShelf("00")
                    .build())
                .setIndex(Uint8.ZERO)
                .build()));
        if (serviceFormat.equals(ServiceFormat.ODU)) {
            serviceZEndBuilder.setOduServiceRate(ODU4.VALUE);
        }
        if (serviceFormat.equals(ServiceFormat.OTU)) {
            serviceZEndBuilder.setOtuServiceRate(OTU4.VALUE);
        }
        if (!serviceLayer.equals(LayerProtocolName.ETH)) {
            serviceZEndBuilder
                .setEthernetAttributes(new EthernetAttributesBuilder().setSubrateEthSla(new SubrateEthSlaBuilder()
                        .setCommittedBurstSize(Uint16.valueOf(64))
                        .setCommittedInfoRate(Uint32.valueOf(100000))
                        .build())
                    .build());
        }
        return serviceZEndBuilder.build();
    }

    private ServiceAEnd tapiEndPointToServiceAPoint(
        org.opendaylight.yang.gen.v1.urn
            .onf.otcc.yang.tapi.connectivity.rev221121.create.connectivity.service.input.EndPoint endPoint,
        ServiceFormat serviceFormat, String nodeAid, Uint64 capacity, LayerProtocolName serviceLayer) {
        // TODO -> change way this is being created. The name includes only SPDR-SA1-XPDR1.
        //  Not the rest which is needed in the txPortDeviceName.
        //  It could be obtained from the SIP which has the NEP and includes all the OR name.
        Uuid sipUuid = endPoint.getServiceInterfacePoint().getServiceInterfacePointUuid();
        // Todo -> need to find the NEP associated to that SIP
        Uuid nodeUuid = new Uuid(UUID.nameUUIDFromBytes(nodeAid.getBytes(StandardCharsets.UTF_8)).toString());
        LOG.info("NodeA {} Uuid is {}", nodeAid, nodeUuid);
        org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.topology.Node node =
            this.tapiContext.getTapiNode(this.tapiTopoUuid, nodeUuid);
        if (node == null) {
            LOG.error("Node not found in datastore");
            return null;
        }
        // TODO -> in case of a DSR service, for some requests we need the NETWORK PORT and not the CLIENT although the
        //  connection is between 2 CLIENT ports. Otherwise it will not work...
        OwnedNodeEdgePoint nep = null;
        for (OwnedNodeEdgePoint onep : node.getOwnedNodeEdgePoint().values()) {
            if (onep.getMappedServiceInterfacePoint() == null) {
                continue;
            }
            if (onep.getMappedServiceInterfacePoint().containsKey(new MappedServiceInterfacePointKey(sipUuid))) {
                nep = onep;
                break;
            }
        }
        if (nep == null) {
            LOG.error("Nep not found in datastore");
            return null;
        }
        String nodeName = "";
        for (Map.Entry<org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.local._class.NameKey,
                org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.local._class.Name> entry:
                endPoint.getName().entrySet()) {
            if (!("Node Type").equals(entry.getValue().getValueName())) {
                nodeName = entry.getValue().getValue();
            }
        }
//        String nodeName = endPoint.getName().values().stream().findFirst().orElseThrow().getValue();
        String nodeid = String.join("-", nodeName.split("-")[0], nodeName.split("-")[1]);
        String nepName = nep.getName().values().stream().findFirst().orElseThrow().getValue();
        String txPortDeviceName = nepName.split("\\+")[0];
        String txPortName = nepName.split("\\+")[2];
        String rxPortDeviceName = txPortDeviceName;
        String rxPortName = txPortName;
        LOG.debug("Node a id = {}, txportDeviceName = {}, txPortName = {}", nodeid, txPortDeviceName, txPortName);
        LOG.debug("Node a id = {}, rxportDeviceName = {}, rxPortName = {}", nodeid, rxPortDeviceName, rxPortName);
        // TODO --> get clli from datastore?
        String clli = "NodeSA";
        LOG.info("Node a id = {}, txportDeviceName = {}, txPortName = {}", nodeid, txPortDeviceName, txPortName);
        LOG.info("Node a id = {}, rxportDeviceName = {}, rxPortName = {}", nodeid, rxPortDeviceName, rxPortName);
        ServiceAEndBuilder serviceAEndBuilder = new ServiceAEndBuilder()
            .setClli(clli)
            .setNodeId(new NodeIdType(nodeid))
            .setOpticType(OpticTypes.Gray)
            .setServiceFormat(serviceFormat)
            .setServiceRate(Uint32.valueOf(capacity))
            .setEthernetAttributes(new EthernetAttributesBuilder().setSubrateEthSla(
                    new org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev230526.subrate.eth
                        .sla.SubrateEthSlaBuilder()
                        .setCommittedBurstSize(Uint16.valueOf(64))
                        .setCommittedInfoRate(Uint32.valueOf(100000))
                        .build())
                .build())
            .setTxDirection(Map.of(new TxDirectionKey(Uint8.ZERO), new TxDirectionBuilder()
                .setPort(new PortBuilder()
                    .setPortDeviceName(txPortDeviceName)
                    .setPortName(txPortName)
                    .setPortRack(TapiStringConstants.PORT_RACK_VALUE)
                    .setPortShelf("00")
                    .setPortType(TapiStringConstants.PORT_TYPE)
                    .build())
                .setLgx(new LgxBuilder()
                    .setLgxDeviceName(TapiStringConstants.LGX_DEVICE_NAME)
                    .setLgxPortName(TapiStringConstants.LGX_PORT_NAME)
                    .setLgxPortRack(TapiStringConstants.PORT_RACK_VALUE)
                    .setLgxPortShelf("00")
                    .build())
                .setIndex(Uint8.ZERO)
                .build()))
            .setRxDirection(Map.of(new RxDirectionKey(Uint8.ZERO), new RxDirectionBuilder()
                .setPort(new PortBuilder()
                    .setPortDeviceName(rxPortDeviceName)
                    .setPortName(rxPortName)
                    .setPortRack(TapiStringConstants.PORT_RACK_VALUE)
                    .setPortShelf("00")
                    .setPortType(TapiStringConstants.PORT_TYPE)
                    .build())
                .setLgx(new LgxBuilder()
                    .setLgxDeviceName(TapiStringConstants.LGX_DEVICE_NAME)
                    .setLgxPortName(TapiStringConstants.LGX_PORT_NAME)
                    .setLgxPortRack(TapiStringConstants.PORT_RACK_VALUE)
                    .setLgxPortShelf("00")
                    .build())
                .setIndex(Uint8.ZERO)
                .build()));
        if (serviceFormat.equals(ServiceFormat.ODU)) {
            serviceAEndBuilder.setOduServiceRate(ODU4.VALUE);
        } else if (serviceFormat.equals(ServiceFormat.OTU)) {
            serviceAEndBuilder.setOtuServiceRate(OTU4.VALUE);
        }
        if (!serviceLayer.equals(LayerProtocolName.ETH)) {
            serviceAEndBuilder
                .setEthernetAttributes(new EthernetAttributesBuilder().setSubrateEthSla(new SubrateEthSlaBuilder()
                        .setCommittedBurstSize(Uint16.valueOf(64))
                        .setCommittedInfoRate(Uint32.valueOf(100000))
                        .build())
                    .build());
        }
        return serviceAEndBuilder.build();
    }

    private ConnectionType getConnectionTypePhtnc(Collection<org.opendaylight.yang.gen.v1.urn
            .onf.otcc.yang.tapi.connectivity.rev221121.create.connectivity.service.input.EndPoint> endPoints) {
        if (endPoints.stream().anyMatch(ep -> ep.getName().values().stream()
                .anyMatch(name -> name.getValue().contains("ROADM")))) {
            // EndPoints are ROADMs
            return ConnectionType.RoadmLine;
        }
        // EndPoints ar not ROADMs -> XPDR, MUXPDR, SWTICHPDR
        return ConnectionType.Infrastructure;
    }

    private ServiceFormat getServiceFormatPhtnc(Collection<org.opendaylight.yang.gen.v1.urn
            .onf.otcc.yang.tapi.connectivity.rev221121.create.connectivity.service.input.EndPoint> endPoints) {
        if (endPoints.stream().anyMatch(ep -> ep.getName().values().stream()
                .anyMatch(name -> name.getValue().contains("ROADM")))) {
            // EndPoints are ROADMs
            return ServiceFormat.OC;
        }
        // EndPoints ar not ROADMs -> XPDR, MUXPDR, SWTICHPDR
        return ServiceFormat.OTU;
    }

    private ConnectionEndPoint getAssociatediODUCep(String spcXpdrNetwork) {
        Uuid nodeUuid = new Uuid(UUID.nameUUIDFromBytes((String.join("+", spcXpdrNetwork.split("\\+")[0],
            TapiStringConstants.XPDR).getBytes(StandardCharsets.UTF_8))).toString());
        Uuid nepUuid = new Uuid(UUID.nameUUIDFromBytes((String.join("+", spcXpdrNetwork.split("\\+")[0],
                TapiStringConstants.I_ODU, spcXpdrNetwork.split("\\+")[1]).getBytes(StandardCharsets.UTF_8)))
            .toString());
        Uuid cepUuid = new Uuid(UUID.nameUUIDFromBytes((String.join("+", "CEP",
            spcXpdrNetwork.split("\\+")[0], TapiStringConstants.I_ODU, spcXpdrNetwork.split("\\+")[1]))
            .getBytes(StandardCharsets.UTF_8)).toString());
        return this.tapiContext.getTapiCEP(this.tapiTopoUuid, nodeUuid, nepUuid, cepUuid);
    }

    private String getAssociatedNetworkPort(String spcXpdrClient, List<String> xpdrNetworkTplist) {
        for (String networkPort:xpdrNetworkTplist) {
            if (networkPort.split("\\+")[0].equals(spcXpdrClient.split("\\+")[0])) {
                return networkPort;
            }
        }
        return null;
    }

    private List<String> getAssociatedClientsPort(List<String> xpdrNetworkTplist) {
        List<String> clientPortList = new ArrayList<>();
        for (String networkPort:xpdrNetworkTplist) {
            String nodeId = String.join("-", networkPort.split("\\+")[0].split("-")[0],
                networkPort.split("\\+")[0].split("-")[1]);
            String tpId = networkPort.split("\\+")[1];
            InstanceIdentifier<Mapping> mapIID = InstanceIdentifier.builder(Network.class)
                .child(Nodes.class, new NodesKey(nodeId))
                .child(Mapping.class, new MappingKey(tpId)).build();
            try {
                Optional<Mapping> optMapping = this.networkTransactionService.read(LogicalDatastoreType.CONFIGURATION,
                    mapIID).get();
                if (!optMapping.isPresent()) {
                    LOG.error("Couldnt find mapping for port {} of node {}", tpId, nodeId);
                }
                Mapping mapping = optMapping.orElseThrow();
                LOG.info("Mapping for node+port {}+{} = {}", nodeId, tpId, mapping);
                String key = String.join("+", String.join("-", nodeId, tpId.split("\\-")[0]),
                    mapping.getConnectionMapLcp());
                LOG.info("Key to be added to list = {}", key);
                if (!clientPortList.contains(key)) {
                    clientPortList.add(key);
                }
            } catch (InterruptedException | ExecutionException e) {
                LOG.error("Couldnt read mapping from datastore", e);
                return null;
            }

        }
        return clientPortList;
    }

    private OpenroadmNodeType getOpenRoadmNodeType(List<String> xpdrNodelist) {
        List<OpenroadmNodeType> openroadmNodeTypeList = new ArrayList<>();
        for (String xpdrNode:xpdrNodelist) {
            Uuid nodeUuid = new Uuid(UUID.nameUUIDFromBytes((String.join("+",xpdrNode, TapiStringConstants.XPDR))
                .getBytes(StandardCharsets.UTF_8)).toString());
            InstanceIdentifier<org.opendaylight.yang.gen.v1.urn
                .onf.otcc.yang.tapi.topology.rev221121.topology.Node> nodeIID = InstanceIdentifier.builder(
                    Context.class).augmentation(org.opendaylight.yang.gen.v1.urn
                    .onf.otcc.yang.tapi.topology.rev221121.Context1.class).child(TopologyContext.class)
                .child(Topology.class, new TopologyKey(this.tapiTopoUuid))
                .child(org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.topology.Node.class,
                    new NodeKey(nodeUuid)).build();
            try {
                Optional<org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.topology.Node> optNode
                    = this.networkTransactionService.read(LogicalDatastoreType.OPERATIONAL, nodeIID).get();
                if (optNode.isEmpty()) {
                    return null;
                }
                OpenroadmNodeType openroadmNodeType = OpenroadmNodeType.forName(optNode.orElseThrow().getName().get(
                    new NameKey("Node Type")).getValue());
                if (!openroadmNodeTypeList.contains(openroadmNodeType)) {
                    openroadmNodeTypeList.add(openroadmNodeType);
                }
            } catch (InterruptedException | ExecutionException e) {
                LOG.error("Couldnt read node in topology", e);
                return null;
            }
        }
        // TODO for now check that there is only one type, otherwise error
        if (openroadmNodeTypeList.size() != 1) {
            LOG.error("More than one xpdr type. List = {}", openroadmNodeTypeList);
            return null;
        }
        return openroadmNodeTypeList.get(0);
    }

    private OwnedNodeEdgePoint createRoadmNep(String orNodeId, String tpId,
        boolean withSip, OperationalState operState, AdministrativeState adminState, String nepPhotonicSublayer) {

        //TODO : complete implementation with SIP
        Uuid nepUuid = new Uuid(UUID.nameUUIDFromBytes((String.join("+", orNodeId, nepPhotonicSublayer,
                tpId)).getBytes(StandardCharsets.UTF_8)).toString());
        Name nepName = new NameBuilder()
                .setValueName(TapiStringConstants.PHTNC_MEDIA + "NodeEdgePoint")
                .setValue(String.join("+", orNodeId, nepPhotonicSublayer, tpId))
                .build();
        List<SupportedCepLayerProtocolQualifierInstances> sclpqiList = new ArrayList<>();
        if (TapiStringConstants.MC.equals(nepPhotonicSublayer)) {
            sclpqiList.add(new SupportedCepLayerProtocolQualifierInstancesBuilder()
                .setLayerProtocolQualifier(PHOTONICLAYERQUALIFIERMC.VALUE)
                .setNumberOfCepInstances(Uint64.valueOf(1))
                .build());
        } else {
            sclpqiList.add(new SupportedCepLayerProtocolQualifierInstancesBuilder()
                .setLayerProtocolQualifier(PHOTONICLAYERQUALIFIEROTSiMC.VALUE)
                .setNumberOfCepInstances(Uint64.valueOf(1))
                .build());
        }
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

        return onep;
    }

}
