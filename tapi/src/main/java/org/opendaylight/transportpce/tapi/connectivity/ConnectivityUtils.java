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
import org.opendaylight.transportpce.common.fixedflex.GridUtils;
import org.opendaylight.transportpce.common.network.NetworkTransactionService;
import org.opendaylight.transportpce.servicehandler.service.ServiceDataStoreOperations;
import org.opendaylight.transportpce.tapi.TapiStringConstants;
import org.opendaylight.transportpce.tapi.topology.ConvertORToTapiTopology;
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
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev221121.cep.list.ConnectionEndPointKey;
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
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.node.OwnedNodeEdgePointKey;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.node.edge.point.MappedServiceInterfacePointKey;
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

    private static final Logger LOG = LoggerFactory.getLogger(ConnectivityUtils.class);
    private final Uuid tapiTopoUuid;

    private final ServiceDataStoreOperations serviceDataStoreOperations;
    private final TapiContext tapiContext;
    private Map<ServiceInterfacePointKey, ServiceInterfacePoint> sipMap;
    // this variable is for complete connection objects
    private final Map<
        org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev221121.connectivity.context.ConnectionKey,
        org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev221121.connectivity.context.Connection>
            connectionFullMap;
    private final NetworkTransactionService networkTransactionService;
    private Connection topConnRdmRdm;
    private Connection topConnXpdrXpdrPhtn;
    private Connection topConnXpdrXpdrOdu;
    private ConvertORToTapiTopology tapiFactory;

    // TODO -> handle cases for which node id is ROADM-A1 and not ROADMA01 or XPDR-A1 and not XPDRA01
    public ConnectivityUtils(ServiceDataStoreOperations serviceDataStoreOperations,
                             Map<ServiceInterfacePointKey, ServiceInterfacePoint> sipMap, TapiContext tapiContext,
                             NetworkTransactionService networkTransactionService, Uuid tapiTopoUuid) {
        this.serviceDataStoreOperations = serviceDataStoreOperations;
        this.tapiContext = tapiContext;
        this.sipMap = sipMap;
        this.connectionFullMap = new HashMap<>();
        this.networkTransactionService = networkTransactionService;
        this.topConnRdmRdm = null;
        this.topConnXpdrXpdrPhtn = null;
        this.topConnXpdrXpdrOdu = null;
        this.tapiTopoUuid = tapiTopoUuid;
        this.tapiFactory = new ConvertORToTapiTopology(tapiTopoUuid);
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
            .setSdncRequestHeader(
                new SdncRequestHeaderBuilder()
                    .setRequestId("request-1")
                    .setRpcAction(RpcActions.ServiceCreate)
                    .setNotificationUrl("notification url")
                    .setRequestSystemId("appname")
                    .build())
            .build();
    }

    public static ServiceAEnd buildServiceAEnd(
            String nodeid, String clli, String txPortDeviceName,
            String txPortName, String rxPortDeviceName, String rxPortName) {
        return new ServiceAEndBuilder()
            .setClli(clli)
            .setNodeId(new NodeIdType(nodeid))
            .setOpticType(OpticTypes.Gray)
            .setServiceFormat(ServiceFormat.Ethernet)
            .setServiceRate(Uint32.valueOf(100))
            .setTxDirection(Map.of(
                new TxDirectionKey(Uint8.ZERO),
                new TxDirectionBuilder()
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
            .setRxDirection(Map.of(
                new RxDirectionKey(Uint8.ZERO),
                new RxDirectionBuilder()
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

    public static ServiceZEnd buildServiceZEnd(
            String nodeid, String clli, String txPortDeviceName,
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
        if (optServicePaths.isEmpty()) {
            LOG.error("No service path found for service {}", service.getServiceName());
            return null;
        }
        PathDescription pathDescription = optServicePaths.orElseThrow().getPathDescription();
        LOG.info("Path description of service = {}", pathDescription);
        org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev230526.service.ServiceAEnd serviceAEnd =
            service.getServiceAEnd();
        // Endpoint creation
        EndPoint endPoint1 = mapServiceAEndPoint(serviceAEnd, pathDescription);
        EndPoint endPoint2 = mapServiceZEndPoint(service.getServiceZEnd(), pathDescription);
        Map<EndPointKey, EndPoint> endPointMap = new HashMap<>(Map.of(
                endPoint1.key(), endPoint1,
                endPoint2.key(), endPoint2));
        LOG.info("EndPoints of connectivity services = {}", endPointMap);
        // Services Names
        Name name =
            new NameBuilder().setValueName("Connectivity Service Name").setValue(service.getServiceName()).build();
        // Connection creation
        Map<ConnectionKey, Connection> connMap =
            createConnectionsFromService(pathDescription, mapServiceLayerToAend(serviceAEnd));
        LOG.debug("connectionMap for service {} = {} ", name, connMap);
        ConnectivityConstraint conConstr =
            new ConnectivityConstraintBuilder().setServiceType(ServiceType.POINTTOPOINTCONNECTIVITY).build();
        return new ConnectivityServiceBuilder()
            .setAdministrativeState(AdministrativeState.UNLOCKED)
            .setOperationalState(OperationalState.ENABLED)
            .setLifecycleState(LifecycleState.INSTALLED)
            .setUuid(new Uuid(
                UUID.nameUUIDFromBytes(service.getServiceName().getBytes(StandardCharsets.UTF_8)).toString()))
            .setLayerProtocolName(mapServiceLayer(serviceAEnd.getServiceFormat(), endPoint1, endPoint2))
            .setConnectivityConstraint(conConstr)
            .setDirection(ForwardingDirection.BIDIRECTIONAL)
            .setName(Map.of(name.key(), name))
            .setConnection(connMap)
            .setEndPoint(endPointMap)
            .build();
    }

    public Map<ConnectionKey, Connection> createConnectionsFromService(
            PathDescription pathDescription, LayerProtocolName lpn) {
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
        //  As mentioned above, for 100GbE service creation there are ROADMs in the path description.
        // TODO: OpenROADM getNodeType from the NamesList to verify what needs to be created
        OpenroadmNodeType openroadmNodeType = getOpenRoadmNodeType(xpdrNodelist);
        Map<ConnectionKey, Connection> connectionServMap = new HashMap<>();
        switch (lpn) {
            case PHOTONICMEDIA:
                // Identify number of ROADMs
                // - XC Connection between MC CEPs mapped from MC NEPs (within a roadm)
                // - XC Connection between OTSiMC CEPs mapped from OTSiMC NEPs (within a roadm)
                // - In Roadms, only one NEP modeled for both OTSi_MC and MC on both TTP and PPs.
                // - Changed naming convention of CEPs and connections, to include the frequency and allow several
                // - OTSiMC and MC Ceps to be attached to the same NEP and support all related connections (1/Lambda)
                // - Top Connection MC between MC CEPs of different roadms
                // - Top Connection OTSiMC between OTSiMC CEPs of extreme roadms
                LOG.debug("CONNECTIVITYUTILS 422 SpectralIndexLow = {}, High = {}",
                    GridUtils.getLowerSpectralIndexFromFrequency(pathDescription.getAToZDirection()
                        .getAToZMinFrequency().getValue()),
                    GridUtils.getHigherSpectralIndexFromFrequency(pathDescription.getAToZDirection()
                        .getAToZMinFrequency().getValue()));
                connectionServMap.putAll(
                    createRoadmCepsAndConnections(
                        GridUtils.getLowerSpectralIndexFromFrequency(pathDescription.getAToZDirection()
                            .getAToZMinFrequency().getValue()),
                        GridUtils.getHigherSpectralIndexFromFrequency(pathDescription.getAToZDirection()
                            .getAToZMaxFrequency().getValue()),
                        rdmAddDropTplist, rdmDegTplist, rdmNodelist, edgeRoadm1, edgeRoadm2));
                LOG.debug("CONNECTIVITYUTILS 434 Connservmap = {}", connectionServMap);
                if (!pathDescription.getAToZDirection().getAToZ().values().stream().findFirst().orElseThrow().getId()
                        .contains("ROADM")) {
                    // - XC Connection OTSi betwwen iOTSi y eOTSi of xpdr
                    // - Top connection OTSi between network ports of xpdrs in the Photonic media layer -> i_OTSi
                    connectionServMap.putAll(createXpdrCepsAndConnectionsPht(
                        GridUtils.getLowerSpectralIndexFromFrequency(pathDescription.getAToZDirection()
                            .getAToZMinFrequency().getValue()),
                        GridUtils.getHigherSpectralIndexFromFrequency(pathDescription.getAToZDirection()
                            .getAToZMaxFrequency().getValue()),
                        xpdrNetworkTplist, xpdrNodelist));
                    LOG.debug("CONNECTIVITYUTILS 445 Connservmap = {}", connectionServMap);
                }
                this.topConnRdmRdm = null;
                break;
            case ODU:
                // TODO: verify if this is correct
                // - XC Connection OTSi between iODU and eODU of xpdr
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
                    connectionServMap.putAll(
                        createRoadmCepsAndConnections(
                            GridUtils.getLowerSpectralIndexFromFrequency(pathDescription.getAToZDirection()
                                .getAToZMinFrequency().getValue()),
                            GridUtils.getHigherSpectralIndexFromFrequency(pathDescription.getAToZDirection()
                                .getAToZMaxFrequency().getValue()),
                            rdmAddDropTplist, rdmDegTplist, rdmNodelist, edgeRoadm1,
                            edgeRoadm2));
                    connectionServMap.putAll(
                        createXpdrCepsAndConnectionsPht(
                            GridUtils.getLowerSpectralIndexFromFrequency(pathDescription.getAToZDirection()
                                .getAToZMinFrequency().getValue()),
                            GridUtils.getHigherSpectralIndexFromFrequency(pathDescription.getAToZDirection()
                                .getAToZMaxFrequency().getValue()),
                        xpdrNetworkTplist, xpdrNodelist));
                    this.topConnRdmRdm = null;
                    xpdrClientTplist = getAssociatedClientsPort(xpdrNetworkTplist);
                    LOG.info("Associated client ports = {}", xpdrClientTplist);
                    connectionServMap.putAll(
                        createXpdrCepsAndConnectionsEth(xpdrClientTplist, xpdrNodelist, connectionServMap));
                    this.topConnXpdrXpdrPhtn = null;
                }
                break;
            case DSR:
                LOG.info("OTN XGE/ODUe service");
                // - XC connection between iODU and eODU
                // - Top connection between eODU ports
                // - Top connection between DSR ports
                if (openroadmNodeType.equals(OpenroadmNodeType.SWITCH)) {
                    // - XC Connection OTSi between iODU and eODU of xpdr
                    // - Top connection in the ODU layer, between xpdr eODU ports (?)
                    connectionServMap.putAll(
                        createXpdrCepsAndConnectionsDsr(xpdrClientTplist, xpdrNetworkTplist, xpdrNodelist));
                    this.topConnXpdrXpdrPhtn = null;
                }
                if (openroadmNodeType.equals(OpenroadmNodeType.MUXPDR)) {
                    connectionServMap.putAll(
                        createXpdrCepsAndConnectionsDsr(xpdrClientTplist, xpdrNetworkTplist, xpdrNodelist));
                    this.topConnXpdrXpdrOdu = null;
                }
                break;
            default:
                LOG.error("Service type format not supported");
        }
        LOG.debug("CONNSERVERMAP 508 = {}", connectionServMap);
        return connectionServMap;
    }

    private void updateTopologyWithNep(Uuid topoUuid, Uuid nodeUuid, Uuid nepUuid, OwnedNodeEdgePoint onep) {
        InstanceIdentifier<OwnedNodeEdgePoint> onepIID = InstanceIdentifier.builder(Context.class)
            .augmentation(org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.Context1.class)
            .child(org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.context.TopologyContext.class)
            .child(Topology.class, new TopologyKey(topoUuid))
            .child(org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.topology.Node.class,
                new NodeKey(nodeUuid))
            .child(OwnedNodeEdgePoint.class, new OwnedNodeEdgePointKey(nepUuid))
            .build();
        try {
            Optional<OwnedNodeEdgePoint> optionalOnep =
                this.networkTransactionService.read(LogicalDatastoreType.OPERATIONAL, onepIID).get();
            if (optionalOnep.isPresent()) {
                LOG.error("ONEP is already present in datastore");
                return;
            }
            // merge in datastore
            this.networkTransactionService.merge(LogicalDatastoreType.OPERATIONAL, onepIID, onep);
            this.networkTransactionService.commit().get();
            LOG.info("NEP {} added successfully.", onep.getName());
        } catch (InterruptedException | ExecutionException e) {
            LOG.error("Couldnt put NEP {} in topology, error = ", onep.getName(), e);
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
        Map<org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev221121
                .create.connectivity.service.input.EndPointKey,
            org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev221121
                .create.connectivity.service.input.EndPoint> endPointMap = input.getEndPoint();
        ConnectionType connType = null;
        ServiceFormat serviceFormat = null;
        String nodeAid = String.join("+",
            endPointMap.values().stream().findFirst().orElseThrow().getLocalId(),
            TapiStringConstants.XPDR);
        String nodeZid = String.join("+",
            endPointMap.values().stream().skip(1).findFirst().orElseThrow().getLocalId(),
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
        ServiceAEnd serviceAEnd = tapiEndPointToServiceAPoint(
            endPointMap.values().stream().findFirst().orElseThrow(),
            serviceFormat, nodeAid, capacity, input.getLayerProtocolName());
        ServiceZEnd serviceZEnd = tapiEndPointToServiceZPoint(
            endPointMap.values().stream().skip(1).findFirst().orElseThrow(),
            serviceFormat, nodeZid, capacity, input.getLayerProtocolName());
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
            org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev230526
                .service.ServiceAEnd serviceAEnd) {
        ServiceFormat serviceFormat = serviceAEnd.getServiceFormat();
        switch (serviceFormat) {
            case OC:
            case OTU:
                return LayerProtocolName.PHOTONICMEDIA;
            case ODU:
                return LayerProtocolName.ODU;
            case Ethernet:
                if (getOpenroadmType(
                            serviceAEnd.getTxDirection().values().stream().findFirst().orElseThrow()
                                .getPort().getPortDeviceName())
                        .equals(OpenroadmNodeType.TPDR)) {
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
        org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.topology.Node tapiNode =
            this.tapiContext.getTapiNode(
                this.tapiTopoUuid,
                new Uuid(UUID.nameUUIDFromBytes(
                        (String.join("+",nodeName, TapiStringConstants.XPDR)).getBytes(StandardCharsets.UTF_8))
                    .toString()));
        return tapiNode == null
            ? null
            : OpenroadmNodeType.forName(tapiNode.getName().get(new NameKey("Node Type")).getValue());
    }

    private Map<ConnectionKey, Connection> createXpdrCepsAndConnectionsEth(
            List<String> xpdrClientTplist, List<String> xpdrNodelist, Map<ConnectionKey, Connection> lowerConn) {
        // TODO: do we need to create cross connection between iODU and eODU??
        // add the lower connections of the previous steps for this kind of service
        Map<LowerConnectionKey, LowerConnection> xcMap = new HashMap<>();
        for (Connection lowConn: lowerConn.values()) {
            LowerConnection conn = new LowerConnectionBuilder().setConnectionUuid(lowConn.getConnectionUuid()).build();
            xcMap.put(conn.key(), conn);
        }
        Map<org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev221121
                .cep.list.ConnectionEndPointKey, ConnectionEndPoint> cepMapDsr = new HashMap<>();
        // Create 1 cep per Xpdr in the CLIENT
        // 1 top connection DSR between the CLIENT xpdrs
        for (String xpdr:xpdrNodelist) {
            LOG.info("Creating ceps and xc for xpdr {}", xpdr);
            String spcXpdrClient =
                xpdrClientTplist.stream().filter(netp -> netp.contains(xpdr)).findFirst().orElseThrow();
            ConnectionEndPoint netCep1 =
                createCepXpdr(0, 0, spcXpdrClient, TapiStringConstants.DSR, TapiStringConstants.XPDR,
                    LayerProtocolName.DSR);
            putXpdrCepInTopologyContext(
                xpdr, spcXpdrClient, TapiStringConstants.DSR, TapiStringConstants.XPDR, netCep1);
            cepMapDsr.put(netCep1.key(), netCep1);
        }
        // DSR top connection between edge xpdr CLIENT DSR
        org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev221121
                .connectivity.context.Connection connectionDsr =
            createTopConnection(
                //spcXpdr1,
                xpdrClientTplist.stream().filter(adp -> adp.contains(xpdrNodelist.get(0)))
                    .findFirst().orElseThrow(),
                //spcXpdr2,
                xpdrClientTplist.stream().filter(adp -> adp.contains(xpdrNodelist.get(xpdrNodelist.size() - 1)))
                    .findFirst().orElseThrow(),
                cepMapDsr, TapiStringConstants.DSR, LayerProtocolName.DSR, xcMap, this.topConnXpdrXpdrPhtn);
        this.connectionFullMap.put(connectionDsr.key(), connectionDsr);
        // DSR top connection that will be added to the service object
        Connection conn1 = new ConnectionBuilder().setConnectionUuid(connectionDsr.getUuid()).build();
        return new HashMap<>(Map.of(conn1.key(), conn1));
    }

    private Map<ConnectionKey,Connection> createXpdrCepsAndConnectionsDsr(
            List<String> xpdrClientTplist, List<String> xpdrNetworkTplist, List<String> xpdrNodelist) {
        Map<ConnectionKey, Connection> connServMap = new HashMap<>();
        Map<org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev221121
                .cep.list.ConnectionEndPointKey, ConnectionEndPoint> cepMapDsr = new HashMap<>();
        Map<org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev221121
                .cep.list.ConnectionEndPointKey, ConnectionEndPoint> cepMapOdu = new HashMap<>();
        // TODO: when upgrading the models to 2.1.3, get the connection inclusion because those connections will
        //  be added to the lower connection of a top connection
        Map<LowerConnectionKey, LowerConnection> xcMap = new HashMap<>();

        // Create 1 cep per Xpdr in the CLIENT, 1 cep per Xpdr eODU, 1 XC between eODU and iODE,
        // 1 top connection between eODU and a top connection DSR between the CLIENT xpdrs
        for (String xpdr:xpdrNodelist) {
            LOG.info("Creating ceps and xc for xpdr {}", xpdr);
            String spcXpdrClient =
                xpdrClientTplist.stream().filter(netp -> netp.contains(xpdr)).findFirst().orElseThrow();
            ConnectionEndPoint clientCep1 =
                createCepXpdr(0, 0,
                    spcXpdrClient, TapiStringConstants.DSR, TapiStringConstants.XPDR, LayerProtocolName.DSR);
            putXpdrCepInTopologyContext(
                xpdr, spcXpdrClient, TapiStringConstants.DSR, TapiStringConstants.XPDR, clientCep1);
            ConnectionEndPoint clientCep2 = createCepXpdr(0, 0, spcXpdrClient, TapiStringConstants.E_ODU,
                    TapiStringConstants.XPDR, LayerProtocolName.ODU);
            putXpdrCepInTopologyContext(
                xpdr, spcXpdrClient, TapiStringConstants.E_ODU, TapiStringConstants.XPDR, clientCep2);
            cepMapDsr.put(clientCep1.key(), clientCep1);
            cepMapOdu.put(clientCep2.key(), clientCep2);

            String spcXpdrNetwork = getAssociatedNetworkPort(spcXpdrClient, xpdrNetworkTplist);
            // Create x connection between I_ODU and E_ODU within xpdr
            org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev221121
                    .connectivity.context.Connection connection =
                createXCBetweenCeps(
                    clientCep2, getAssociatediODUCep(spcXpdrNetwork),
                     spcXpdrClient, spcXpdrNetwork, TapiStringConstants.ODU, LayerProtocolName.ODU);
            this.connectionFullMap.put(connection.key(), connection);

            // Create X connection that will be added to the service object
            LowerConnection conn = new LowerConnectionBuilder().setConnectionUuid(connection.getUuid()).build();
            xcMap.put(conn.key(), conn);
        }

        // DSR top connection between edge xpdr CLIENT DSR
        String spcXpdr1 =
            xpdrClientTplist.stream().filter(adp -> adp.contains(xpdrNodelist.get(0)))
                .findFirst().orElseThrow();
        String spcXpdr2 =
            xpdrClientTplist.stream().filter(adp -> adp.contains(xpdrNodelist.get(xpdrNodelist.size() - 1)))
                .findFirst().orElseThrow();

        org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev221121
                .connectivity.context.Connection connectionOdu =
            createTopConnection(
                spcXpdr1, spcXpdr2, cepMapOdu, TapiStringConstants.E_ODU,
                LayerProtocolName.ODU, xcMap, this.topConnXpdrXpdrOdu);
        this.connectionFullMap.put(connectionOdu.key(), connectionOdu);

        // ODU top connection that will be added to the service object
        Connection conn = new ConnectionBuilder().setConnectionUuid(connectionOdu.getUuid()).build();
        connServMap.put(conn.key(), conn);
        LowerConnection lowerConn = new LowerConnectionBuilder().setConnectionUuid(connectionOdu.getUuid()).build();
        xcMap.put(lowerConn.key(), lowerConn);

        org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev221121
                .connectivity.context.Connection connectionDsr =
            createTopConnection(
                spcXpdr1, spcXpdr2, cepMapDsr, TapiStringConstants.DSR,
                LayerProtocolName.DSR, xcMap, this.topConnXpdrXpdrPhtn);
        this.connectionFullMap.put(connectionDsr.key(), connectionDsr);
        // DSR top connection that will be added to the service object
        Connection conn1 = new ConnectionBuilder().setConnectionUuid(connectionDsr.getUuid()).build();
        connServMap.put(conn1.key(), conn1);
        return connServMap;
    }

    private Map<ConnectionKey, Connection> createXpdrCepsAndConnectionsOdu(
            List<String> xpdrNetworkTplist, List<String> xpdrNodelist) {
        Map<ConnectionKey, Connection> connServMap = new HashMap<>();
        Map<org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev221121
                .cep.list.ConnectionEndPointKey, ConnectionEndPoint> cepMap = new HashMap<>();
        // TODO: when upgrading the models to 2.1.3, get the connection inclusion because those connections will
        //  be added to the lower connection of a top connection
        // Create 1 cep per Xpdr in the I_ODU and a top
        // connection iODU between the xpdrs
        for (String xpdr:xpdrNodelist) {
            LOG.info("Creating iODU ceps and xc for xpdr {}", xpdr);
            String spcXpdrNetwork =
                xpdrNetworkTplist.stream().filter(netp -> netp.contains(xpdr)).findFirst().orElseThrow();
            ConnectionEndPoint netCep1 =
                createCepXpdr(0, 0,
                    spcXpdrNetwork, TapiStringConstants.I_ODU, TapiStringConstants.XPDR, LayerProtocolName.ODU);
            putXpdrCepInTopologyContext(
                xpdr, spcXpdrNetwork, TapiStringConstants.I_ODU, TapiStringConstants.XPDR, netCep1);
            cepMap.put(netCep1.key(), netCep1);
        }
        // ODU top connection between edge xpdr i_ODU
        org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev221121
                .connectivity.context.Connection connection =
            createTopConnection(
                //spcXpdr1,
                xpdrNetworkTplist.stream().filter(adp -> adp.contains(xpdrNodelist.get(0)))
                    .findFirst().orElseThrow(),
                //spcXpdr2,
                xpdrNetworkTplist.stream().filter(adp -> adp.contains(xpdrNodelist.get(xpdrNodelist.size() - 1)))
                    .findFirst().orElseThrow(),
                cepMap, TapiStringConstants.I_ODU,
                LayerProtocolName.ODU, new HashMap<>(), this.topConnXpdrXpdrPhtn);
        this.connectionFullMap.put(connection.key(), connection);
        // ODU top connection that will be added to the service object
        Connection conn = new ConnectionBuilder().setConnectionUuid(connection.getUuid()).build();
        connServMap.put(conn.key(), conn);
        this.topConnXpdrXpdrOdu = conn;
        return connServMap;
    }

    private Map<ConnectionKey, Connection> createXpdrCepsAndConnectionsPht(int lowerFreqIndex, int higherFreqIndex,
                List<String> xpdrNetworkTplist, List<String> xpdrNodelist) {
        // TODO: when upgrading the models to 2.1.3, get the connection inclusion because those connections will
        //  be added to the lower connection of a top connection
        Map<org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev221121.cep.list.ConnectionEndPointKey,
            ConnectionEndPoint> cepMap = new HashMap<>();
        // create ceps and x connections within xpdr
        LOG.debug("CONNECTIVITYUTILS 866 CreateXpdrCep1ConnPht");
        if (xpdrNodelist.isEmpty()) {
            LOG.warn("Xpdr nodes not found, skipping XPDR CEP connection");
            return new HashMap<>();
        }
        String slotFreqExtension = "";
        for (String xpdr:xpdrNodelist) {
            LOG.info("Creating ceps and xc for xpdr {}", xpdr);
            String spcXpdrNetwork =
                xpdrNetworkTplist.stream().filter(netp -> netp.contains(xpdr)).findFirst().orElseThrow();
            // There should be 1 network tp per xpdr
            //   Just create 2 different CEPs (1 OTS + 1 OTSI_MC)
            ConnectionEndPoint netCep1 = createCepXpdr(0, 0,
                    spcXpdrNetwork, TapiStringConstants.PHTNC_MEDIA_OTS,
                    TapiStringConstants.XPDR, LayerProtocolName.PHOTONICMEDIA);
            putXpdrCepInTopologyContext(
                xpdr, spcXpdrNetwork, TapiStringConstants.PHTNC_MEDIA_OTS, TapiStringConstants.XPDR, netCep1);
            ConnectionEndPoint netCep2 = createCepXpdr(lowerFreqIndex, higherFreqIndex,
                    spcXpdrNetwork, TapiStringConstants.OTSI_MC,
                    TapiStringConstants.XPDR, LayerProtocolName.PHOTONICMEDIA);
            putXpdrCepInTopologyContext(
                xpdr, spcXpdrNetwork, TapiStringConstants.OTSI_MC, TapiStringConstants.XPDR, netCep2);
            cepMap.put(netCep1.key(), netCep1);
            cepMap.put(netCep2.key(), netCep2);
            slotFreqExtension = "[" + netCep2.getName().entrySet().iterator().next().getValue().getValue()
                .split("\\[")[1];

        }
        LOG.debug("CONNECTIVITYUTILS 894 CreateXpdrCep1ConnPht");
        // OTSi top connection between edge OTSI_MC Xpdr
        org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev221121
                .connectivity.context.Connection connection =
            createTopConnection(
                String.join("-", xpdrNetworkTplist.stream().filter(adp -> adp.contains(xpdrNodelist.get(0)))
                    .findFirst().orElseThrow(), slotFreqExtension),
                String.join("-", xpdrNetworkTplist.stream().filter(adp -> adp.contains(xpdrNodelist
                    .get(xpdrNodelist.size() - 1))).findFirst().orElseThrow(), slotFreqExtension),
                cepMap, TapiStringConstants.OTSI_MC,
                LayerProtocolName.PHOTONICMEDIA, new HashMap<>(), this.topConnRdmRdm);
        this.connectionFullMap.put(connection.key(), connection);
        // OTSi top connection that will be added to the service object
        Connection conn = new ConnectionBuilder().setConnectionUuid(connection.getUuid()).build();
        this.topConnXpdrXpdrPhtn = conn;
        LOG.info("ReturnedMap904 {}", new HashMap<>(Map.of(conn.key(), conn)));
        return new HashMap<>(Map.of(conn.key(), conn));
    }

    private Map<org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev221121
            .cep.list.ConnectionEndPointKey, ConnectionEndPoint> createRoadmCepsAndClientNeps(String roadm,
                int lowerFreqIndex, int higherFreqIndex, String rdmTp, boolean withOMS) {
        String clientQualifier = "";
        // Create 3 CEPs (OTS, MC, OTSi_MC) for ADD and (OMS, MC, OTSi_MC) for DEG and add them to CepMap

        if (withOMS) {
            // WithOMS is true for degree for which OTS Cep is created during link discovery/update
            clientQualifier = TapiStringConstants.MC;
            OwnedNodeEdgePoint onepMC = tapiFactory.createRoadmNep(rdmTp.split("\\+")[0], rdmTp.split("\\+")[1],
                false, OperationalState.ENABLED, AdministrativeState.UNLOCKED, clientQualifier);
            putRdmNepInTopologyContext(rdmTp.split("\\+")[0], rdmTp.split("\\+")[1], TapiStringConstants.MC, onepMC);
        } else {
            // WithOMS is false for Add/drop ports for which no OTS was created during initial mapping
            // With new process of CEP creation, OTS CEP are created also on PPs at initialization.
            // Don't need to recreate it
//            ConnectionEndPoint rdmCep0 = tapiFactory.createCepRoadm(0, 0, rdmTp,
//                TapiStringConstants.PHTNC_MEDIA_OTS, null, true);
//            putRdmCepInTopologyContext(roadm, rdmTp, TapiStringConstants.PHTNC_MEDIA_OTS, rdmCep0);
//            cepMap.put(rdmCep0.key(), rdmCep0);
        }

        clientQualifier = TapiStringConstants.OTSI_MC;
        OwnedNodeEdgePoint onepOTSiMC = tapiFactory.createRoadmNep(rdmTp.split("\\+")[0], rdmTp.split("\\+")[1],
            false, OperationalState.ENABLED, AdministrativeState.UNLOCKED, clientQualifier);
        putRdmNepInTopologyContext(rdmTp.split("\\+")[0], rdmTp.split("\\+")[1],
            TapiStringConstants.OTSI_MC, onepOTSiMC);
        // For MC Cep creation process last parameter (boolean srg) is not checked so that we don't care whether we
        //create this Cep for PP or for TTP that srg is set to false or true
        Map<org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev221121
            .cep.list.ConnectionEndPointKey, ConnectionEndPoint> cepMap = new HashMap<>();
        ConnectionEndPoint rdmCep2 = tapiFactory.createCepRoadm(lowerFreqIndex, higherFreqIndex, rdmTp,
            TapiStringConstants.MC, null, false);
        putRdmCepInTopologyContext(roadm, rdmTp, TapiStringConstants.MC, rdmCep2);
        cepMap.put(rdmCep2.key(), rdmCep2);

        ConnectionEndPoint rdmCep3 = tapiFactory.createCepRoadm(lowerFreqIndex, higherFreqIndex, rdmTp,
            TapiStringConstants.OTSI_MC, null, false);
        putRdmCepInTopologyContext(roadm, rdmTp, TapiStringConstants.OTSI_MC, rdmCep3);
        cepMap.put(rdmCep3.key(), rdmCep3);
        return cepMap;
    }

    private Map<ConnectionKey, Connection> createRoadmCepsAndConnections(int lowerFreqIndex, int higherFreqIndex,
            List<String> rdmAddDropTplist, List<String> rdmDegTplist, List<String> rdmNodelist,
            String edgeRoadm1, String edgeRoadm2) {

        Map<ConnectionEndPointKey, ConnectionEndPoint> intermediateCepMap = new HashMap<>();
        LOG.debug("IntermediateCepMap {}", intermediateCepMap);
        Map<org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev221121
            .cep.list.ConnectionEndPointKey, ConnectionEndPoint> cepMap = new HashMap<>();
        // create ceps and x connections within roadm
        Map<LowerConnectionKey, LowerConnection> xcLowerMap = new HashMap<>();
        String slotFreqExtension = "[" + lowerFreqIndex + "-" + higherFreqIndex + "]";
        LOG.debug("slotFreqExtension in createRoadmCepsAndConnections is {}", slotFreqExtension);
        for (String roadm : rdmNodelist) {
            LOG.info("Creating ceps and xc for roadm {}", roadm);
            if (roadm.equals(edgeRoadm1) || roadm.equals(edgeRoadm2)) {
                LOG.info("EDGE ROADM, cross connections needed between SRG and DEG");
                String spcRdmAD = rdmAddDropTplist.stream().filter(adp -> adp.contains(roadm))
                    .findFirst().orElseThrow();
                LOG.info("AD port of ROADm {} = {}", roadm, spcRdmAD);
                // Create CEPs for each AD and DEG and the corresponding cross connections, matching the NEPs
                // created in the topology creation
                // add CEPs to the topology to the corresponding ONEP
                intermediateCepMap = createRoadmCepsAndClientNeps(
                    roadm, lowerFreqIndex, higherFreqIndex, spcRdmAD, false);
                ConnectionEndPoint adCepMC = intermediateCepMap.entrySet().stream().filter(
                        cep -> cep.getValue().getLayerProtocolQualifier().equals(PHOTONICLAYERQUALIFIERMC.VALUE))
                    .findFirst().orElseThrow().getValue();
                LOG.debug("adCepMC is {}", adCepMC);
                ConnectionEndPoint adCepOTSiMC = intermediateCepMap.entrySet().stream().filter(
                        cep -> cep.getValue().getLayerProtocolQualifier().equals(PHOTONICLAYERQUALIFIEROTSiMC.VALUE))
                    .findFirst().orElseThrow().getValue();
                LOG.debug("adCepOTSiMC is {}", adCepOTSiMC);
                cepMap.putAll(intermediateCepMap);
                String spcRdmDEG = rdmDegTplist.stream().filter(adp -> adp.contains(roadm)).findFirst().orElseThrow();
                LOG.info("Degree port of ROADm {} = {}", roadm, spcRdmDEG);
                intermediateCepMap = createRoadmCepsAndClientNeps(
                    roadm, lowerFreqIndex, higherFreqIndex, spcRdmDEG, true);
                ConnectionEndPoint degCepMC = intermediateCepMap.entrySet().stream().filter(
                    cep -> cep.getValue().getLayerProtocolQualifier().equals(PHOTONICLAYERQUALIFIERMC.VALUE))
                    .findFirst().orElseThrow().getValue();
                LOG.debug("degCepMC is {}", degCepMC);
                ConnectionEndPoint degCepOTSiMC = intermediateCepMap.entrySet().stream().filter(
                    cep -> cep.getValue().getLayerProtocolQualifier().equals(PHOTONICLAYERQUALIFIEROTSiMC.VALUE))
                    .findFirst().orElseThrow().getValue();
                LOG.debug("degCepOTSiMC is {}", degCepOTSiMC);
                cepMap.putAll(intermediateCepMap);
                LOG.info("Going to create cross connections for ROADM {}", roadm);
                // Create X connections between MC and OTSi_MC for full map
                org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev221121
                        .connectivity.context.Connection connection1 =
                    createXCBetweenCeps(
                        adCepMC, degCepMC, spcRdmAD, spcRdmDEG, TapiStringConstants.MC,
                        LayerProtocolName.PHOTONICMEDIA);
                LOG.info("Cross connection 1 created = {}", connection1);
                org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev221121
                        .connectivity.context.Connection connection2 =
                    createXCBetweenCeps(
                        adCepOTSiMC, degCepOTSiMC, spcRdmAD, spcRdmDEG, TapiStringConstants.OTSI_MC,
                        LayerProtocolName.PHOTONICMEDIA);
                LOG.info("Cross connection 2 created = {}", connection2);
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
                intermediateCepMap = createRoadmCepsAndClientNeps(
                    roadm, lowerFreqIndex, higherFreqIndex, spcRdmDEG1, true);
                ConnectionEndPoint deg1CepMC = intermediateCepMap.entrySet().stream().filter(
                    cep -> cep.getValue().getLayerProtocolQualifier().equals(PHOTONICLAYERQUALIFIERMC.VALUE))
                    .findFirst().orElseThrow().getValue();
                LOG.debug("deg1CepMC is {}", deg1CepMC);
                ConnectionEndPoint deg1CepOTSiMC = intermediateCepMap.entrySet().stream().filter(
                    cep -> cep.getValue().getLayerProtocolQualifier().equals(PHOTONICLAYERQUALIFIEROTSiMC.VALUE))
                    .findFirst().orElseThrow().getValue();
                LOG.debug("deg1CepOTSiMC is {}", deg1CepOTSiMC);
                cepMap.putAll(intermediateCepMap);

                String spcRdmDEG2 =
                    rdmDegTplist.stream().filter(adp -> adp.contains(roadm)).skip(1).findFirst().orElseThrow();
                LOG.info("Degree 2 port of ROADm {} = {}", roadm, spcRdmDEG2);
                intermediateCepMap = createRoadmCepsAndClientNeps(
                    roadm, lowerFreqIndex, higherFreqIndex, spcRdmDEG2, true);
                ConnectionEndPoint deg2CepMC = intermediateCepMap.entrySet().stream().filter(
                    cep -> cep.getValue().getLayerProtocolQualifier().equals(PHOTONICLAYERQUALIFIERMC.VALUE))
                    .findFirst().orElseThrow().getValue();
                LOG.debug("deg2CepMC is {}", deg2CepMC);
                ConnectionEndPoint deg2CepOTSiMC = intermediateCepMap.entrySet().stream().filter(
                    cep -> cep.getValue().getLayerProtocolQualifier().equals(PHOTONICLAYERQUALIFIEROTSiMC.VALUE))
                    .findFirst().orElseThrow().getValue();
                LOG.debug("deg2CepOTSiMC is {}", deg2CepOTSiMC);
                cepMap.putAll(intermediateCepMap);

                LOG.info("Going to create cross connections for ROADM {}", roadm);
                // Create X connections between MC and OTSi_MC for full map
                org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev221121
                        .connectivity.context.Connection connection1 =
                    createXCBetweenCeps(
                        deg1CepMC, deg2CepMC, spcRdmDEG1, spcRdmDEG2,
                        TapiStringConstants.MC, LayerProtocolName.PHOTONICMEDIA);
                LOG.info("Cross connection 1 created = {}", connection1);
                org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev221121
                        .connectivity.context.Connection connection2 =
                    createXCBetweenCeps(
                        deg1CepOTSiMC, deg2CepOTSiMC, spcRdmDEG1, spcRdmDEG2,
                        TapiStringConstants.OTSI_MC, LayerProtocolName.PHOTONICMEDIA);
                LOG.info("Cross connection 2 created = {}", connection2.toString());
                this.connectionFullMap.put(connection1.key(), connection1);
                this.connectionFullMap.put(connection2.key(), connection2);

                // Create X connections that will be added to the service object
                LowerConnection conn1 = new LowerConnectionBuilder().setConnectionUuid(connection1.getUuid()).build();
                xcLowerMap.put(conn1.key(), conn1);
                LowerConnection conn2 = new LowerConnectionBuilder().setConnectionUuid(connection2.getUuid()).build();
                xcLowerMap.put(conn2.key(), conn2);
            }
        }
        LOG.info("Going to create top connections between roadms");
        String spcRdmAD1 = rdmAddDropTplist.stream().filter(adp -> adp.contains(edgeRoadm1)).findFirst().orElseThrow();
        String spcRdmAD2 = rdmAddDropTplist.stream().filter(adp -> adp.contains(edgeRoadm2)).findFirst().orElseThrow();
        // MC top connection between edge roadms
        LOG.info("Going to created top connection between MC");
        org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev221121
                .connectivity.context.Connection connection =
            createTopConnection(String.join("-", spcRdmAD1, slotFreqExtension),
                String.join("-", spcRdmAD2, slotFreqExtension), cepMap, TapiStringConstants.MC,
                LayerProtocolName.PHOTONICMEDIA, xcLowerMap, null);
        this.connectionFullMap.put(connection.key(), connection);
        LOG.info("Top connection created = {}", connection);

        LowerConnection conn1 = new LowerConnectionBuilder().setConnectionUuid(connection.getUuid()).build();
        // OTSiMC top connection between edge roadms
        LOG.debug("Going to created top connection between OTSiMC");
        org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev221121
                .connectivity.context.Connection connection1 =
            createTopConnection(String.join("-", spcRdmAD1, slotFreqExtension),
                String.join("-", spcRdmAD2, slotFreqExtension), cepMap, TapiStringConstants.OTSI_MC,
                LayerProtocolName.PHOTONICMEDIA,
                //topLowerMap,
                new HashMap<>(Map.of(conn1.key(), conn1)),
                null);
        this.connectionFullMap.put(connection1.key(), connection1);
        LOG.info("Top connection OTSiMC created = {}", connection1);
        LOG.debug("Map of All connections = {}", this.connectionFullMap);

        // OTSiMC top connections that will be added to the service object
        Connection conn = new ConnectionBuilder().setConnectionUuid(connection.getUuid()).build();
        Connection conn2 = new ConnectionBuilder().setConnectionUuid(connection1.getUuid()).build();
        this.topConnRdmRdm = conn2;
        LOG.debug("ReturnedMap1102 {}", new HashMap<>(Map.of(conn.key(), conn, conn2.key(), conn2)));
        return new HashMap<>(Map.of(conn.key(), conn, conn2.key(), conn2));
    }

    private org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev221121
                .connectivity.context.Connection createTopConnection(
            String tp1,
            String tp2,
            Map<org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev221121
                    .cep.list.ConnectionEndPointKey, ConnectionEndPoint> cepMap,
            String qual, LayerProtocolName topPortocol,
            Map<LowerConnectionKey, LowerConnection> xcMap,
            Connection additionalLowerConn) {
        // find cep for each AD MC of roadm 1 and 2
        String topConnName = String.join("+", "TOP", tp1, tp2, qual);
        LOG.info("Creation of Top connection, name = {}", topConnName);
        org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev221121.ConnectionEndPoint adCep1 =
            cepMap.get(
                new org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev221121
                        .cep.list.ConnectionEndPointKey(new Uuid(UUID.nameUUIDFromBytes(
                    (String.join("+", "CEP", tp1.split("\\+")[0], qual, tp1.split("\\+")[1]))
                        .getBytes(StandardCharsets.UTF_8)).toString())));
        LOG.debug("ADCEP1 = {}", adCep1);
        org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev221121
                .connection.ConnectionEndPoint cep1 =
            new org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev221121
                    .connection.ConnectionEndPointBuilder()
                .setNodeEdgePointUuid(adCep1.getParentNodeEdgePoint().getNodeEdgePointUuid())
                .setNodeUuid(adCep1.getParentNodeEdgePoint().getNodeUuid())
                .setTopologyUuid(adCep1.getParentNodeEdgePoint().getTopologyUuid())
                .setConnectionEndPointUuid(adCep1.getUuid())
                .build();
        org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev221121.ConnectionEndPoint adCep2 =
            cepMap.get(
                new org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev221121
                        .cep.list.ConnectionEndPointKey(
                    new Uuid(UUID.nameUUIDFromBytes(
                        (String.join("+", "CEP", tp2.split("\\+")[0], qual, tp2.split("\\+")[1]))
                            .getBytes(StandardCharsets.UTF_8)).toString())));
        LOG.debug("ADCEP2 = {}", adCep2);
        org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev221121
                .connection.ConnectionEndPoint cep2 =
            new org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev221121
                    .connection.ConnectionEndPointBuilder()
                .setNodeEdgePointUuid(adCep2.getParentNodeEdgePoint().getNodeEdgePointUuid())
                .setNodeUuid(adCep2.getParentNodeEdgePoint().getNodeUuid())
                .setTopologyUuid(adCep2.getParentNodeEdgePoint().getTopologyUuid())
                .setConnectionEndPointUuid(adCep1.getUuid())
                .build();
        Name connName = new NameBuilder()
            .setValueName("Connection name")
            .setValue(topConnName)
            .build();
        // TODO: lower connection, supported link.......
        if (additionalLowerConn != null) {
            xcMap.putIfAbsent(new LowerConnectionKey(additionalLowerConn.getConnectionUuid()),
                new LowerConnectionBuilder().setConnectionUuid(additionalLowerConn.getConnectionUuid()).build());
        }
        return new org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev221121
                .connectivity.context.ConnectionBuilder()
            .setUuid(new Uuid(UUID.nameUUIDFromBytes(topConnName.getBytes(StandardCharsets.UTF_8)).toString()))
            .setName(Map.of(connName.key(), connName))
            .setConnectionEndPoint(new HashMap<>(Map.of(cep1.key(), cep1, cep2.key(), cep2)))
            .setOperationalState(OperationalState.ENABLED)
            .setLayerProtocolName(topPortocol)
            .setLifecycleState(LifecycleState.INSTALLED)
            .setDirection(ForwardingDirection.BIDIRECTIONAL)
            .setLowerConnection(xcMap)
            .build();
    }

    private org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev221121
                .connectivity.context.Connection createXCBetweenCeps(
            ConnectionEndPoint cep1, ConnectionEndPoint cep2,
            String tp1, String tp2, String qual, LayerProtocolName xcProtocol) {
        String crossConnName = String.join("+", "XC", cep1.getName().entrySet().iterator().next().getValue().getValue(),
            cep2.getName().entrySet().iterator().next().getValue().getValue());
        LOG.info("Creation cross connection between: {} and {}", tp1, tp2);
        LOG.info("CONNECTIVITYUTILS 1145 : Cross connection name = {}", crossConnName);
        LOG.debug("Parent NEP of CEP1 = {}", cep1.getParentNodeEdgePoint());
        LOG.debug("Parent NEP CEP2 = {}", cep2.getParentNodeEdgePoint());
        org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev221121
                .connection.ConnectionEndPoint cepServ1 =
            new org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev221121
                    .connection.ConnectionEndPointBuilder()
                .setNodeEdgePointUuid(cep1.getParentNodeEdgePoint().getNodeEdgePointUuid())
                .setNodeUuid(cep1.getParentNodeEdgePoint().getNodeUuid())
                .setTopologyUuid(cep1.getParentNodeEdgePoint().getTopologyUuid())
                .setConnectionEndPointUuid(cep1.getUuid())
                .build();
        org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev221121
                .connection.ConnectionEndPoint cepServ2 =
            new org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev221121
                    .connection.ConnectionEndPointBuilder()
                .setNodeEdgePointUuid(cep2.getParentNodeEdgePoint().getNodeEdgePointUuid())
                .setNodeUuid(cep2.getParentNodeEdgePoint().getNodeUuid())
                .setTopologyUuid(cep2.getParentNodeEdgePoint().getTopologyUuid())
                .setConnectionEndPointUuid(cep2.getUuid())
                .build();
        Name connName = new NameBuilder()
            .setValueName("Connection name")
            .setValue(crossConnName)
            .build();
        // TODO: lower connection, supported link.......
        return new org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev221121
                .connectivity.context.ConnectionBuilder()
            .setUuid(new Uuid(UUID.nameUUIDFromBytes(crossConnName.getBytes(StandardCharsets.UTF_8)).toString()))
            .setName(Map.of(connName.key(), connName))
            .setConnectionEndPoint(new HashMap<>(Map.of(cepServ1.key(), cepServ1, cepServ2.key(), cepServ2)))
            .setOperationalState(OperationalState.ENABLED)
            .setLayerProtocolName(xcProtocol)
            .setLifecycleState(LifecycleState.INSTALLED)
            .setDirection(ForwardingDirection.BIDIRECTIONAL)
            .build();
    }

    private ConnectionEndPoint createCepXpdr(int lowerFreqIndex, int higherFreqIndex,
            String id, String qualifier, String nodeLayer, LayerProtocolName cepProtocol) {
        String nepId = String.join("+", id.split("\\+")[0], qualifier, id.split("\\+")[1]);
        String nepNodeId = String.join("+",id.split("\\+")[0], TapiStringConstants.XPDR);
        String extendedNepId = (lowerFreqIndex == 0 && higherFreqIndex == 0)
            ? nepId
            : String.join("-",nepId, ("[" + lowerFreqIndex + "-" + higherFreqIndex + "]"));
        LOG.debug("CONNECTIVITYUTILS 1307 CreateCepXpdr {}", extendedNepId);
        Name cepName = new NameBuilder()
            .setValueName("ConnectionEndPoint name")
            .setValue(extendedNepId)
            .build();
        ParentNodeEdgePoint pnep = new ParentNodeEdgePointBuilder()
            .setNodeEdgePointUuid(new Uuid(UUID.nameUUIDFromBytes(
                    nepId.getBytes(StandardCharsets.UTF_8)).toString()))
            .setNodeUuid(new Uuid(UUID.nameUUIDFromBytes(
                    nepNodeId.getBytes(StandardCharsets.UTF_8)).toString()))
            .setTopologyUuid(this.tapiTopoUuid)
            .build();
        String clientQualifier = "";
        switch (qualifier) {
            case TapiStringConstants.PHTNC_MEDIA_OTS:
                clientQualifier = TapiStringConstants.OTSI_MC;
                break;
            case TapiStringConstants.OTSI_MC:
                clientQualifier = TapiStringConstants.E_ODU;
                break;
            case TapiStringConstants.E_ODU:
                clientQualifier = TapiStringConstants.DSR;
                break;
            default :
                LOG.debug("no client CEP for DSR NEP {}", nepId);
                break;
        }
        ClientNodeEdgePoint cnep = new ClientNodeEdgePointBuilder()
            .setNodeEdgePointUuid(new Uuid(UUID.nameUUIDFromBytes(
                (String.join("+", id.split("\\+")[0], clientQualifier, id.split("\\+")[1]))
                    .getBytes(StandardCharsets.UTF_8)).toString()))
            .setNodeUuid(new Uuid(UUID.nameUUIDFromBytes(
                    nepNodeId.getBytes(StandardCharsets.UTF_8)).toString()))
            .setTopologyUuid(this.tapiTopoUuid)
            .build();
        // TODO: add augmentation with the corresponding cep-spec (i.e. MC, OTSiMC...)
        ConnectionEndPointBuilder cepBldr = new ConnectionEndPointBuilder()
            .setUuid(new Uuid(UUID.nameUUIDFromBytes(
                (String.join("+", "CEP", extendedNepId))
                    .getBytes(StandardCharsets.UTF_8)).toString()))
            .setParentNodeEdgePoint(pnep)
            .setName(Map.of(cepName.key(), cepName))
            .setConnectionPortRole(PortRole.SYMMETRIC)
            .setDirection(Direction.BIDIRECTIONAL)
            .setOperationalState(OperationalState.ENABLED)
            .setLifecycleState(LifecycleState.INSTALLED)
            .setLayerProtocolName(cepProtocol);
        return TapiStringConstants.DSR.equals(qualifier)
            ? cepBldr.build()
            : cepBldr.setClientNodeEdgePoint(Map.of(cnep.key(), cnep)).build();
    }

    private EndPoint mapServiceZEndPoint(
            org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev230526
                .service.ServiceZEnd serviceZEnd, PathDescription pathDescription) {
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
        var portZEnd = serviceZEnd.getTxDirection().values().stream().findFirst().orElseThrow().getPort();
        org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.local._class.Name name =
            new org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.local._class.NameBuilder()
                .setValueName("OpenROADM info")
                .setValue(String.join("-", serviceZEnd.getClli(), portZEnd.getPortDeviceName(), portZEnd.getPortName()))
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
//                .setBandwidthProfile(new BandwidthProfileBuilder().build()) // TODO: implement bandwidth profile
                .build())
            .setProtectionRole(ProtectionRole.WORK)
            .setRole(PortRole.SYMMETRIC)
            .setLocalId(portZEnd.getPortDeviceName())
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
        var portAEnd = serviceAEnd.getTxDirection().values().stream().findFirst().orElseThrow().getPort();
        org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.local._class.Name name =
            new org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.local._class.NameBuilder()
                .setValueName("OpenROADM info")
                .setValue(String.join("-", serviceAEnd.getClli(), portAEnd.getPortDeviceName(), portAEnd.getPortName()))
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
        if (serviceNodeId.contains("ROADM")) {
            // Service from ROADM to ROADM
            // AddDrop-AddDrop ports --> MC layer SIPs
            ZToA firstElement =
                mapztoa.values().stream().filter(ztoa -> ztoa.getId().equals("0")).findFirst().orElseThrow();
            TerminationPoint tp = (TerminationPoint) firstElement.getResource().getResource();
            Uuid sipUuid = new Uuid(UUID.nameUUIDFromBytes(
                (String.join("+", "SIP", tp.getTpNodeId(), TapiStringConstants.MC, tp.getTpId()))
                    .getBytes(StandardCharsets.UTF_8)).toString());
            LOG.info("SIP name = {}", String.join("+", tp.getTpNodeId(), TapiStringConstants.MC, tp.getTpId()));
            for (ServiceInterfacePoint sip:this.sipMap.values()) {
                if (sip.getUuid().equals(sipUuid)) {
                    return sip.getUuid();
                }
                LOG.debug("SIP {} does not match sipname {}", sip.getUuid().getValue(), sipUuid.getValue());
            }
            return null;
        }
        // Service from XPDR to XPDR
        ZToA firstElement;
        TerminationPoint tp;
        Uuid sipUuid;
        switch (serviceFormat) {
            case ODU:
                firstElement =
                    mapztoa.values().stream().filter(ztoa -> ztoa.getId().equals("2")).findFirst().orElseThrow();
                tp = (TerminationPoint) firstElement.getResource().getResource();
                // Network-Network ports --> iODU layer SIPs TODO --> updated to E_ODU
                sipUuid = new Uuid(UUID.nameUUIDFromBytes(
                    (String.join("+", "SIP", tp.getTpNodeId(), TapiStringConstants.I_ODU, tp.getTpId()))
                        .getBytes(StandardCharsets.UTF_8)).toString());
                LOG.info("SIP name = {}", String.join("+", tp.getTpNodeId(), TapiStringConstants.I_ODU, tp.getTpId()));
                break;
            case OTU:
                firstElement =
                    mapztoa.values().stream().filter(ztoa -> ztoa.getId().equals("2")).findFirst().orElseThrow();
                tp = (TerminationPoint) firstElement.getResource().getResource();
                // Network-Network ports --> iOTSi layer SIPs
                sipUuid = new Uuid(UUID.nameUUIDFromBytes(
                    (String.join("+", "SIP", tp.getTpNodeId(), TapiStringConstants.I_OTSI, tp.getTpId()))
                        .getBytes(StandardCharsets.UTF_8)).toString());
                LOG.info("SIP name = {}", String.join("+", tp.getTpNodeId(), TapiStringConstants.I_OTSI, tp.getTpId()));
                break;
            case Ethernet:
                LOG.info("Elements ZA = {}", mapztoa.values());
                firstElement =
                    mapztoa.values().stream().filter(ztoa -> ztoa.getId().equals("0")).findFirst().orElseThrow();
                tp = (TerminationPoint) firstElement.getResource().getResource();
                // Client-client ports --> DSR layer SIPs
                sipUuid = new Uuid(UUID.nameUUIDFromBytes(
                    (String.join("+", "SIP", tp.getTpNodeId(), TapiStringConstants.DSR, tp.getTpId()))
                        .getBytes(StandardCharsets.UTF_8)).toString());
                LOG.info("SIP name = {}", String.join("+", tp.getTpNodeId(), TapiStringConstants.DSR, tp.getTpId()));
                break;
            default:
                sipUuid = null;
                LOG.warn("Service format {} not supported (?)", serviceFormat.getName());
        }
        for (ServiceInterfacePoint sip:this.sipMap.values()) {
            if (sip.getUuid().equals(sipUuid)) {
                return sip.getUuid();
            }
            LOG.debug("SIP {} does not match sipname {}", sip.getUuid().getValue(), sipUuid.getValue());
        }
        return null;
    }

    private Uuid getSipIdFromAend(Map<AToZKey, AToZ> mapatoz, String serviceNodeId, ServiceFormat serviceFormat) {
        LOG.info("ServiceNode = {} and ServiceFormat = {}", serviceNodeId, serviceFormat.getName());
        LOG.info("Map a to z = {}", mapatoz);
        if (serviceNodeId.contains("ROADM")) {
            // Service from ROADM to ROADM
            // AddDrop-AddDrop ports --> MC layer SIPs
            AToZ firstElement =
                mapatoz.values().stream().filter(atoz -> atoz.getId().equals("0")).findFirst().orElseThrow();
            LOG.info("First element of service path = {}", firstElement.getResource().getResource());
            TerminationPoint tp = (TerminationPoint) firstElement.getResource().getResource();
            Uuid sipUuid = new Uuid(UUID.nameUUIDFromBytes(
                (String.join("+", "SIP", tp.getTpNodeId(), TapiStringConstants.MC, tp.getTpId()))
                    .getBytes(StandardCharsets.UTF_8)).toString());
            LOG.info("ROADM SIP name = {}",
                String.join("+", tp.getTpNodeId(), TapiStringConstants.MC, tp.getTpId()));
            for (ServiceInterfacePoint sip:this.sipMap.values()) {
                if (sip.getUuid().equals(sipUuid)) {
                    return sip.getUuid();
                }
                LOG.debug("SIP {} does not match sipname {}", sip.getUuid().getValue(), sipUuid.getValue());
            }
            return null;
        }
        // Service from XPDR to XPDR
        AToZ firstElement;
        TerminationPoint tp;
        Uuid sipUuid;
        switch (serviceFormat) {
            case ODU:
                firstElement =
                    mapatoz.values().stream().filter(atoz -> atoz.getId().equals("2")).findFirst().orElseThrow();
                tp = (TerminationPoint) firstElement.getResource().getResource();
                // Network-Network ports --> iODU layer SIPs. TODO -> updated to eODU
                sipUuid = new Uuid(UUID.nameUUIDFromBytes(
                    (String.join("+", "SIP", tp.getTpNodeId(), TapiStringConstants.I_ODU, tp.getTpId()))
                        .getBytes(StandardCharsets.UTF_8)).toString());
                LOG.info("ODU XPDR SIP name = {}",
                    String.join("+", tp.getTpNodeId(), TapiStringConstants.I_ODU, tp.getTpId()));
                break;
            case OTU:
                firstElement =
                    mapatoz.values().stream().filter(atoz -> atoz.getId().equals("2")).findFirst().orElseThrow();
                tp = (TerminationPoint) firstElement.getResource().getResource();
                // Network-Network ports --> iOTSi layer SIPs
                sipUuid = new Uuid(UUID.nameUUIDFromBytes(
                    (String.join("+", "SIP", tp.getTpNodeId(), TapiStringConstants.I_OTSI, tp.getTpId()))
                        .getBytes(StandardCharsets.UTF_8)).toString());
                LOG.info("OTU XPDR SIP name = {}",
                    String.join("+", tp.getTpNodeId(), TapiStringConstants.I_OTSI, tp.getTpId()));
                break;
            case Ethernet:
                LOG.info("Elements AZ = {}", mapatoz.values());
                firstElement =
                    mapatoz.values().stream().filter(atoz -> atoz.getId().equals("0")).findFirst().orElseThrow();
                tp = (TerminationPoint) firstElement.getResource().getResource();
                // Client-client ports --> DSR layer SIPs
                sipUuid = new Uuid(UUID.nameUUIDFromBytes(
                    (String.join("+", "SIP", tp.getTpNodeId(), TapiStringConstants.DSR, tp.getTpId()))
                        .getBytes(StandardCharsets.UTF_8)).toString());
                LOG.info("DSR XPDR SIP name = {}",
                    String.join("+", tp.getTpNodeId(), TapiStringConstants.DSR, tp.getTpId()));
                break;
            default:
                sipUuid = null;
                LOG.warn("Service format {} not supported (?)", serviceFormat.getName());
        }
        for (ServiceInterfacePoint sip:this.sipMap.values()) {
            if (sip.getUuid().equals(sipUuid)) {
                return sip.getUuid();
            }
            LOG.debug("SIP {} does not match sipname {}", sip.getUuid().getValue(), sipUuid.getValue());
        }
        return null;
    }

    public void putRdmCepInTopologyContext(String node, String spcRdmAD, String qual, ConnectionEndPoint cep) {
        String nepId = String.join("+", node, qual, spcRdmAD.split("\\+")[1]);
        String nodeNepId = String.join("+", node, TapiStringConstants.PHTNC_MEDIA);
        LOG.info("NEP id before Merge = {}", nepId);
        LOG.info("Node of NEP id before Merge = {}", nodeNepId);
        // Give uuids so that it is easier to look for things: topology uuid, node uuid, nep uuid, cep
        this.tapiContext.updateTopologyWithCep(
            //topoUuid,
            this.tapiTopoUuid,
            //nodeUuid,
            new Uuid(UUID.nameUUIDFromBytes(nodeNepId.getBytes(StandardCharsets.UTF_8)).toString()),
            //nepUuid,
            new Uuid(UUID.nameUUIDFromBytes(nepId.getBytes(StandardCharsets.UTF_8)).toString()),
            cep);
    }

    private void putXpdrCepInTopologyContext(
            String node, String spcXpdrNet, String qual, String nodeLayer, ConnectionEndPoint cep) {
        // Give uuids so that it is easier to look for things: topology uuid, node uuid, nep uuid, cep
        this.tapiContext.updateTopologyWithCep(
            //topoUuid,
            this.tapiTopoUuid,
            //nodeUuid,
            new Uuid(UUID.nameUUIDFromBytes(
                    String.join("+", node, nodeLayer).getBytes(StandardCharsets.UTF_8))
                .toString()),
            //nepUuid,
            new Uuid(UUID.nameUUIDFromBytes(
                    String.join("+", node, qual, spcXpdrNet.split("\\+")[1]).getBytes(StandardCharsets.UTF_8))
                .toString()),
            cep);
    }

    public void putRdmNepInTopologyContext(String orNodeId, String orTpId, String qual, OwnedNodeEdgePoint onep) {
        String nepId = String.join("+", orNodeId, qual, orTpId);
        String nepNodeId = String.join("+", orNodeId, TapiStringConstants.PHTNC_MEDIA);
        LOG.info("NEP id before Merge = {}", nepId);
        LOG.info("Node of NEP id before Merge = {}", nepNodeId);
        // Give uuids so that it is easier to look for things: topology uuid, node uuid, nep uuid, cep
        updateTopologyWithNep(
            //topoUuid,
            this.tapiTopoUuid,
            //nodeUuid,
            new Uuid(UUID.nameUUIDFromBytes(
                    nepNodeId.getBytes(Charset.forName("UTF-8")))
                .toString()),
            //nepUuid,
            new Uuid(UUID.nameUUIDFromBytes(
                    nepId.getBytes(Charset.forName("UTF-8")))
                .toString()),
            onep);
    }

    private String getIdBasedOnModelVersion(String nodeid) {
        return nodeid.matches("[A-Z]{5}-[A-Z0-9]{2}-.*")
            ? String.join("-", nodeid.split("-")[0], nodeid.split("-")[1])
            : nodeid.substring(0, nodeid.lastIndexOf("-"));
    }

    private ServiceZEnd tapiEndPointToServiceZPoint(
            org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev221121
                .create.connectivity.service.input.EndPoint endPoint,
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
            if (onep.getMappedServiceInterfacePoint() != null
                    && onep.getMappedServiceInterfacePoint().containsKey(new MappedServiceInterfacePointKey(sipUuid))) {
                nep = onep;
                break;
            }
        }
        if (nep == null) {
            LOG.error("Nep not found in datastore");
            return null;
        }
        String nodeName = "";
        for (Map.Entry<
                    org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.local._class.NameKey,
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
        return serviceLayer.equals(LayerProtocolName.ETH)
            ? serviceZEndBuilder.build()
            : serviceZEndBuilder
                .setEthernetAttributes(new EthernetAttributesBuilder()
                    .setSubrateEthSla(new SubrateEthSlaBuilder()
                        .setCommittedBurstSize(Uint16.valueOf(64))
                        .setCommittedInfoRate(Uint32.valueOf(100000))
                        .build())
                    .build())
                .build();
    }

    private ServiceAEnd tapiEndPointToServiceAPoint(
            org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev221121
                .create.connectivity.service.input.EndPoint endPoint,
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
            if (onep.getMappedServiceInterfacePoint() != null
                    && onep.getMappedServiceInterfacePoint().containsKey(new MappedServiceInterfacePointKey(sipUuid))) {
                nep = onep;
                break;
            }
        }
        if (nep == null) {
            LOG.error("Nep not found in datastore");
            return null;
        }
        String nodeName = "";
        for (Map.Entry<
                org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.local._class.NameKey,
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
        ServiceAEndBuilder serviceAEndBuilder = new ServiceAEndBuilder()
            .setClli(clli)
            .setNodeId(new NodeIdType(nodeid))
            .setOpticType(OpticTypes.Gray)
            .setServiceFormat(serviceFormat)
            .setServiceRate(Uint32.valueOf(capacity))
            .setEthernetAttributes(new EthernetAttributesBuilder().setSubrateEthSla(
                    new org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev230526
                            .subrate.eth.sla.SubrateEthSlaBuilder()
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
        return serviceLayer.equals(LayerProtocolName.ETH)
            ? serviceAEndBuilder.build()
            : serviceAEndBuilder
                .setEthernetAttributes(new EthernetAttributesBuilder()
                    .setSubrateEthSla(new SubrateEthSlaBuilder()
                        .setCommittedBurstSize(Uint16.valueOf(64))
                        .setCommittedInfoRate(Uint32.valueOf(100000))
                        .build())
                    .build())
                .build();
    }

    private ConnectionType getConnectionTypePhtnc(
            Collection<org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev221121
                .create.connectivity.service.input.EndPoint> endPoints) {
        return endPoints.stream()
                .anyMatch(ep -> ep.getName().values().stream().anyMatch(name -> name.getValue().contains("ROADM")))
            // EndPoints are ROADMs
            ? ConnectionType.RoadmLine
            // EndPoints are not ROADMs -> XPDR, MUXPDR, SWTICHPDR
            : ConnectionType.Infrastructure;
    }

    private ServiceFormat getServiceFormatPhtnc(
            Collection<org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev221121
                .create.connectivity.service.input.EndPoint> endPoints) {
        return endPoints.stream()
                .anyMatch(ep -> ep.getName().values().stream().anyMatch(name -> name.getValue().contains("ROADM")))
            // EndPoints are ROADMs
            ? ServiceFormat.OC
            // EndPoints ar not ROADMs -> XPDR, MUXPDR, SWTICHPDR
            : ServiceFormat.OTU;
    }

    private ConnectionEndPoint getAssociatediODUCep(String spcXpdrNetwork) {
        return this.tapiContext.getTapiCEP(
            this.tapiTopoUuid,
            //nodeUuid,
            new Uuid(UUID.nameUUIDFromBytes(
                (String.join("+",
                        spcXpdrNetwork.split("\\+")[0],
                        TapiStringConstants.XPDR)
                    .getBytes(StandardCharsets.UTF_8))).toString()),
            //nepUuid,
            new Uuid(UUID.nameUUIDFromBytes(
                (String.join("+",
                        spcXpdrNetwork.split("\\+")[0],
                        TapiStringConstants.I_ODU,
                        spcXpdrNetwork.split("\\+")[1])
                    .getBytes(StandardCharsets.UTF_8))).toString()),
            //cepUuid,
            new Uuid(UUID.nameUUIDFromBytes(
                (String.join("+",
                        "CEP",
                        spcXpdrNetwork.split("\\+")[0],
                        TapiStringConstants.I_ODU,
                        spcXpdrNetwork.split("\\+")[1]))
                    .getBytes(StandardCharsets.UTF_8)).toString()));
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
            String nodeId = String.join("-",
                networkPort.split("\\+")[0].split("-")[0],
                networkPort.split("\\+")[0].split("-")[1]);
            String tpId = networkPort.split("\\+")[1];
            InstanceIdentifier<Mapping> mapIID = InstanceIdentifier.builder(Network.class)
                .child(Nodes.class, new NodesKey(nodeId))
                .child(Mapping.class, new MappingKey(tpId)).build();
            try {
                Optional<Mapping> optMapping =
                    this.networkTransactionService.read(LogicalDatastoreType.CONFIGURATION, mapIID).get();
                if (optMapping.isEmpty()) {
                    LOG.error("Couldnt find mapping for port {} of node {}", tpId, nodeId);
                }
                Mapping mapping = optMapping.orElseThrow();
                LOG.info("Mapping for node+port {}+{} = {}", nodeId, tpId, mapping);
                String key = String.join("+",
                    String.join("-", nodeId, tpId.split("\\-")[0]),
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
            InstanceIdentifier<org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121
                    .topology.Node> nodeIID =
                InstanceIdentifier.builder(Context.class)
                    .augmentation(org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.Context1.class)
                    .child(TopologyContext.class)
                    .child(Topology.class, new TopologyKey(this.tapiTopoUuid))
                    .child(
                        org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.topology.Node.class,
                        new NodeKey(
                            //nodeUUID
                            new Uuid(UUID.nameUUIDFromBytes(
                                (String.join("+",xpdrNode, TapiStringConstants.XPDR))
                                    .getBytes(StandardCharsets.UTF_8)).toString())))
                    .build();
            try {
                Optional<org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.topology.Node> optNode =
                    this.networkTransactionService.read(LogicalDatastoreType.OPERATIONAL, nodeIID).get();
                if (optNode.isEmpty()) {
                    return null;
                }
                OpenroadmNodeType openroadmNodeType =
                    OpenroadmNodeType.forName(optNode.orElseThrow().getName().get(new NameKey("Node Type")).getValue());
                if (!openroadmNodeTypeList.contains(openroadmNodeType)) {
                    openroadmNodeTypeList.add(openroadmNodeType);
                }
            } catch (InterruptedException | ExecutionException e) {
                LOG.error("Couldnt read node in topology", e);
                return null;
            }
        }
        // TODO for now check that there is only one type, otherwise error
        if (openroadmNodeTypeList.size() == 1) {
            return openroadmNodeTypeList.get(0);
        }
        LOG.error("More than one xpdr type. List = {}", openroadmNodeTypeList);
        return null;
    }

}
