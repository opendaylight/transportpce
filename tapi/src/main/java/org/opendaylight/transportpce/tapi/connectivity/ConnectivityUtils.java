/*
 * Copyright © 2018 Orange & 2021 Nokia, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.tapi.connectivity;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import org.opendaylight.transportpce.servicehandler.service.ServiceDataStoreOperations;
import org.opendaylight.transportpce.tapi.topology.TopologyUtils;
import org.opendaylight.transportpce.tapi.utils.GenericServiceEndpoint;
import org.opendaylight.transportpce.tapi.utils.ServiceEndpointType;
import org.opendaylight.transportpce.tapi.utils.TapiContext;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.equipment.types.rev181130.OpticTypes;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.node.types.rev181130.NodeIdType;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev190531.ConnectionType;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev190531.RpcActions;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev190531.Service;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev190531.sdnc.request.header.SdncRequestHeaderBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev190531.service.endpoint.RxDirectionBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev190531.service.endpoint.TxDirectionBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev190531.service.lgx.LgxBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev190531.service.port.PortBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.otn.common.types.rev181130.ODU4;
import org.opendaylight.yang.gen.v1.http.org.openroadm.otn.common.types.rev181130.OTU4;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.format.rev190531.ServiceFormat;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev190531.ServiceCreateInput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev190531.ServiceCreateInputBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev190531.service.create.input.ServiceAEnd;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev190531.service.create.input.ServiceAEndBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev190531.service.create.input.ServiceZEnd;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev190531.service.create.input.ServiceZEndBuilder;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev201210.PathDescription;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev201210.path.description.atoz.direction.AToZ;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev201210.path.description.atoz.direction.AToZKey;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev201210.path.description.ztoa.direction.ZToA;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev201210.path.description.ztoa.direction.ZToAKey;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev201210.pce.resource.resource.resource.Node;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev201210.pce.resource.resource.resource.TerminationPoint;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.servicepath.rev171017.service.path.list.ServicePaths;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev181210.AdministrativeState;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev181210.CapacityUnit;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev181210.ForwardingDirection;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev181210.LayerProtocolName;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev181210.LifecycleState;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev181210.OperationalState;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev181210.PortDirection;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev181210.PortRole;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev181210.Uuid;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev181210.capacity.BandwidthProfileBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev181210.capacity.TotalSizeBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev181210.global._class.Name;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev181210.global._class.NameBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev181210.tapi.context.ServiceInterfacePoint;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev181210.tapi.context.ServiceInterfacePointKey;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev181210.CreateConnectivityServiceInput;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev181210.ProtectionRole;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev181210.ServiceType;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev181210.cep.list.ConnectionEndPoint;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev181210.cep.list.ConnectionEndPointBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev181210.connection.ConnectionEndPointKey;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev181210.connection.end.point.ClientNodeEdgePoint;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev181210.connection.end.point.ClientNodeEdgePointBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev181210.connectivity.context.ConnectivityService;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev181210.connectivity.context.ConnectivityServiceBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev181210.connectivity.service.Connection;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev181210.connectivity.service.ConnectionBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev181210.connectivity.service.ConnectionKey;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev181210.connectivity.service.EndPoint;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev181210.connectivity.service.EndPointBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev181210.connectivity.service.EndPointKey;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev181210.connectivity.service.end.point.CapacityBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev181210.connectivity.service.end.point.ServiceInterfacePointBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev181210.create.connectivity.service.input.ConnectivityConstraint;
import org.opendaylight.yangtools.yang.common.Uint32;
import org.opendaylight.yangtools.yang.common.Uint64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ConnectivityUtils {

    private static final String LGX_PORT_NAME = "Some lgx-port-name";
    private static final String PORT_TYPE = "some port type";
    private static final String LGX_DEVICE_NAME = "Some lgx-device-name";
    private static final String PORT_RACK_VALUE = "000000.00";
    private static final String DSR = "DSR";
    private static final String ODU = "ODU";
    private static final String E_ODU = "eODU";
    private static final String I_ODU = "iODU";
    private static final String OTSI = "OTSi";
    private static final String E_OTSI = "eOTSi";
    private static final String I_OTSI = "iOTSi";
    private static final String PHTNC_MEDIA = "PHOTONIC_MEDIA";
    private static final String MC = "MEDIA_CHANNEL";
    private static final String OTSI_MC = "OTSi_MEDIA_CHANNEL";
    private static final String TP = "TerminationPoint";
    private static final String NODE = "Node";
    private static final Logger LOG = LoggerFactory.getLogger(ConnectivityUtils.class);

    private final ServiceDataStoreOperations serviceDataStoreOperations;
    private final TapiContext tapiContext;
    private Map<ServiceInterfacePointKey, ServiceInterfacePoint> sipMap;
    private final Map<org.opendaylight.yang.gen.v1.urn
            .onf.otcc.yang.tapi.connectivity.rev181210.connectivity.context.ConnectionKey,
            org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev181210.connectivity.context.Connection>
            connectionFullMap; // this variable is for complete connection objects

    public ConnectivityUtils(ServiceDataStoreOperations serviceDataStoreOperations,
                             Map<ServiceInterfacePointKey, ServiceInterfacePoint> sipMap, TapiContext tapiContext) {
        this.serviceDataStoreOperations = serviceDataStoreOperations;
        this.tapiContext = tapiContext;
        this.sipMap = sipMap;
        this.connectionFullMap = new HashMap<>();
    }

    public static ServiceCreateInput buildServiceCreateInput(GenericServiceEndpoint sepA, GenericServiceEndpoint sepZ) {
        ServiceCreateInputBuilder builtInput = new ServiceCreateInputBuilder();
        ServiceAEnd serviceAEnd = getServiceAEnd(sepA, sepZ);
        ServiceZEnd serviceZEnd = getServiceZEnd(sepA, sepZ);
        if (serviceAEnd != null && serviceZEnd != null) {
            builtInput.setCommonId("commonId");
            builtInput.setConnectionType(ConnectionType.Service);
            builtInput.setCustomer("Customer");
            builtInput.setServiceName("service test");
            builtInput.setServiceAEnd(serviceAEnd);
            builtInput.setServiceZEnd(serviceZEnd);
            builtInput.setSdncRequestHeader(new SdncRequestHeaderBuilder().setRequestId("request-1")
                .setRpcAction(RpcActions.ServiceCreate).setNotificationUrl("notification url").setRequestSystemId(
                    "appname")
                .build());
        } else {
            return null;
        }
        return builtInput.build();
    }

    public static ServiceAEnd buildServiceAEnd(String nodeid, String clli, String txPortDeviceName,
        String txPortName, String rxPortDeviceName, String rxPortName) {
        return new ServiceAEndBuilder()
                .setClli(clli)
                .setNodeId(new NodeIdType(nodeid))
                .setOpticType(OpticTypes.Gray)
                .setServiceFormat(ServiceFormat.Ethernet)
                .setServiceRate(Uint32.valueOf(100))
                .setTxDirection(new TxDirectionBuilder()
                        .setPort(new PortBuilder()
                                .setPortDeviceName(txPortDeviceName)
                                .setPortName(txPortName)
                                .setPortRack(PORT_RACK_VALUE)
                                .setPortShelf("00")
                                .setPortType(PORT_TYPE)
                                .build())
                        .setLgx(new LgxBuilder()
                                .setLgxDeviceName(LGX_DEVICE_NAME)
                                .setLgxPortName(LGX_PORT_NAME)
                                .setLgxPortRack(PORT_RACK_VALUE)
                                .setLgxPortShelf("00")
                                .build())
                        .build())
                .setRxDirection(new RxDirectionBuilder()
                        .setPort(new PortBuilder()
                                .setPortDeviceName(rxPortDeviceName)
                                .setPortName(rxPortName)
                        .setPortRack(PORT_RACK_VALUE)
                        .setPortShelf("00")
                        .setPortType(PORT_TYPE)
                        .build())
                        .setLgx(new LgxBuilder()
                                .setLgxDeviceName(LGX_DEVICE_NAME)
                                .setLgxPortName(LGX_PORT_NAME)
                                .setLgxPortRack(PORT_RACK_VALUE)
                                .setLgxPortShelf("00")
                                .build())
                        .build())
                .build();
    }

    public static ServiceZEnd buildServiceZEnd(String nodeid, String clli, String txPortDeviceName,
        String txPortName, String rxPortDeviceName, String rxPortName) {
        return  new ServiceZEndBuilder().setClli(clli).setNodeId(new NodeIdType(nodeid))
                .setOpticType(OpticTypes.Gray)
                .setServiceFormat(ServiceFormat.Ethernet)
                .setServiceRate(Uint32.valueOf(100))
                .setTxDirection(new TxDirectionBuilder()
                        .setPort(new PortBuilder()
                                .setPortDeviceName(txPortDeviceName)
                                .setPortName(txPortName)
                                .setPortRack(PORT_RACK_VALUE)
                                .setPortShelf("00")
                                .setPortType(PORT_TYPE)
                                .build())
                        .setLgx(new LgxBuilder()
                                .setLgxDeviceName(LGX_DEVICE_NAME)
                                .setLgxPortName(LGX_PORT_NAME)
                                .setLgxPortRack(PORT_RACK_VALUE)
                                .setLgxPortShelf("00")
                                .build())
                        .build())
                .setRxDirection(new RxDirectionBuilder()
                        .setPort(new PortBuilder()
                                .setPortDeviceName(rxPortDeviceName)
                                .setPortName(rxPortName)
                                .setPortRack(PORT_RACK_VALUE)
                                .setPortShelf("00")
                                .setPortType(PORT_TYPE)
                                .build())
                        .setLgx(new LgxBuilder()
                                .setLgxDeviceName(LGX_DEVICE_NAME)
                                .setLgxPortName(LGX_PORT_NAME)
                                .setLgxPortRack(PORT_RACK_VALUE)
                                .setLgxPortShelf("00")
                                .build())
                        .build())
                .build();
    }

    private static ServiceAEnd getServiceAEnd(GenericServiceEndpoint sepA, GenericServiceEndpoint sepZ) {
        if (sepA.getType().equals(ServiceEndpointType.SERVICEAEND)) {
            return new ServiceAEndBuilder(sepA.getValue()).build();
        } else if (sepZ.getType().equals(ServiceEndpointType.SERVICEAEND)) {
            return new ServiceAEndBuilder(sepZ.getValue()).build();
        } else {
            return null;
        }
    }

    private static ServiceZEnd getServiceZEnd(GenericServiceEndpoint sepA, GenericServiceEndpoint sepZ) {
        if (sepA.getType().equals(ServiceEndpointType.SERVICEZEND)) {
            return new ServiceZEndBuilder(sepA.getValue()).build();
        } else if (sepZ.getType().equals(ServiceEndpointType.SERVICEZEND)) {
            return new ServiceZEndBuilder(sepZ.getValue()).build();
        } else {
            return null;
        }
    }

    public void setSipMap(Map<ServiceInterfacePointKey, ServiceInterfacePoint> sips) {
        this.sipMap = sips;
    }

    public ConnectivityService mapORServiceToTapiConnectivity(Service service) {
        // Get service path with the description in OR based models.
        Optional<ServicePaths> optServicePaths =
                this.serviceDataStoreOperations.getServicePath(service.getServiceName());
        if (!optServicePaths.isPresent()) {
            LOG.error("No service path found for service {}", service.getServiceName());
            return null;
        }
        ServicePaths servicePaths = optServicePaths.get();
        PathDescription pathDescription = servicePaths.getPathDescription();
        org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev190531.service.ServiceAEnd serviceAEnd
                = service.getServiceAEnd();
        // Endpoint creation
        EndPoint endPoint1 = mapServiceAEndPoint(serviceAEnd, pathDescription);
        org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev190531.service.ServiceZEnd serviceZEnd
                = service.getServiceZEnd();
        EndPoint endPoint2 = mapServiceZEndPoint(serviceZEnd, pathDescription);
        Map<EndPointKey, EndPoint> endPointMap = new HashMap<>();
        endPointMap.put(endPoint1.key(), endPoint1);
        endPointMap.put(endPoint2.key(), endPoint2);
        // Services Names
        Name name = new NameBuilder().setValueName("Connectivity Service Name").setValue(service.getServiceName())
                .build();
        // Connection creation
        Map<ConnectionKey, Connection> connMap =
                createConnectionsFromService(serviceAEnd, serviceZEnd, pathDescription);
        // TODO: full connectivity service?? With constraints and the rest of fields...
        return new ConnectivityServiceBuilder()
                .setAdministrativeState(AdministrativeState.UNLOCKED)
                .setOperationalState(OperationalState.ENABLED)
                .setLifecycleState(LifecycleState.INSTALLED)
                .setUuid(new Uuid(UUID.nameUUIDFromBytes(service.getServiceName().getBytes(Charset.forName("UTF-8")))
                        .toString()))
                .setServiceLayer(mapServiceLayer(serviceAEnd.getServiceFormat()))
                .setServiceType(ServiceType.POINTTOPOINTCONNECTIVITY)
                .setConnectivityDirection(ForwardingDirection.BIDIRECTIONAL)
                .setName(Map.of(name.key(), name))
                .setConnection(connMap)
                .setEndPoint(endPointMap)
                .build();
    }

    private LayerProtocolName mapServiceLayer(ServiceFormat serviceFormat) {
        LayerProtocolName serviceLayer = null;
        switch (serviceFormat) {
            case OC:
            case OTU:
                serviceLayer = LayerProtocolName.PHOTONICMEDIA;
                break;
            case ODU:
                serviceLayer = LayerProtocolName.ODU;
                break;
            case Ethernet:
                serviceLayer = LayerProtocolName.DSR;
                break;
            default:
                LOG.info("Service layer mapping not supported for {}", serviceFormat.getName());
        }
        return serviceLayer;
    }

    private Map<ConnectionKey, Connection> createConnectionsFromService(
            org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev190531.service.ServiceAEnd
                    serviceAEnd,
            org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev190531.service.ServiceZEnd
                    serviceZEnd,
            PathDescription pathDescription) {
        Map<ConnectionKey, Connection> connectionServMap = new HashMap<>();
        ServiceFormat serviceFormat = serviceAEnd.getServiceFormat(); // should be equal to serviceZEnd
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
                .sorted(Comparator.comparing(AToZ::getId)).collect(Collectors.toList())) {
            resourceType = elem.getResource().getResource().implementedInterface().getSimpleName();
            switch (resourceType) {
                case TP:
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
                        tpNode = tp.getTpNodeId().split("-")[0];
                        if (!rdmAddDropTplist.contains(String.join("+", tpNode, tpID))) {
                            rdmAddDropTplist.add(String.join("+", tpNode, tpID));
                        }
                    }
                    if (tpID.contains("TTP")) {
                        tpNode = tp.getTpNodeId().split("-")[0];
                        if (!rdmDegTplist.contains(String.join("+", tpNode, tpID))) {
                            rdmDegTplist.add(String.join("+", tpNode, tpID));
                        }
                    }
                    break;
                case NODE:
                    Node node = (Node) elem.getResource().getResource();
                    String nodeId = node.getNodeId();
                    if (nodeId.contains("XPDR") || nodeId.contains("SPDR") || nodeId.contains("MXPDR")) {
                        if (!xpdrNodelist.contains(nodeId)) {
                            xpdrNodelist.add(nodeId); // should contain only 2
                        }
                    }
                    if (nodeId.contains("ROADM")) {
                        if (!rdmNodelist.contains(nodeId.split("-")[0])) {
                            rdmNodelist.add(nodeId.split("-")[0]);
                        }
                    }
                    break;
                default:
                    LOG.warn("Resource is a {}", resourceType);
            }
        }
        String edgeRoadm1 = rdmNodelist.get(0);
        String edgeRoadm2 = rdmNodelist.get(rdmNodelist.size() - 1);
        // create corresponding CEPs and Connections. Connections should be added to the corresponding context
        // CEPs must be included in the topology context as an augmentation for each ONEP!!
        switch (serviceFormat) {
            case OC:
                // Identify number of ROADMs
                // - XC Connection between MC CEPs mapped from MC NEPs (within a roadm)
                // - XC Connection between OTSiMC CEPs mapped from OTSiMC NEPs (within a roadm)
                // - Top Connection MC betwwen MC CEPs of different roadms
                // - Top Connection OTSiMC betwwen OTSiMC CEPs of extreme roadms
                connectionServMap.putAll(createRoadmCepsAndConnections(rdmAddDropTplist, rdmDegTplist, rdmNodelist,
                        edgeRoadm1, edgeRoadm2));
                break;
            case OTU:
                // Identify number of ROADMs between XPDRs and check if OC is created
                // - XC Connection between MC CEPs mapped from MC NEPs (within a roadm)
                // - Top Connection MC betwwen MC CEPs of different roadms
                // - XC Connection between OTSiMC CEPs mapped from OTSiMC NEPs (within a roadm)
                // - Top Connection OTSiMC betwwen OTSiMC CEPs of different roadms
                connectionServMap.putAll(createRoadmCepsAndConnections(rdmAddDropTplist, rdmDegTplist, rdmNodelist,
                        edgeRoadm1, edgeRoadm2));
                // - XC Connection OTSi betwwen iOTSi y eOTSi of xpdr
                // - Top connection OTSi between network ports of xpdrs in the Photonic media layer -> i_OTSi
                connectionServMap.putAll(createXpdrCepsAndConnectionsPht(xpdrNetworkTplist, xpdrNodelist));
                break;
            case ODU:
                // Check if OC and OTU are created
                connectionServMap.putAll(createRoadmCepsAndConnections(rdmAddDropTplist, rdmDegTplist, rdmNodelist,
                        edgeRoadm1, edgeRoadm2));
                connectionServMap.putAll(createXpdrCepsAndConnectionsPht(xpdrNetworkTplist, xpdrNodelist));
                // - XC Connection OTSi betwwen iODU and eODU of xpdr
                // - Top connection in the ODU layer, between xpdr iODU ports (?)
                connectionServMap.putAll(createXpdrCepsAndConnectionsOdu(xpdrNetworkTplist, xpdrNodelist));
                break;
            case Ethernet:
                // Check if OC, OTU and ODU are created
                connectionServMap.putAll(createRoadmCepsAndConnections(rdmAddDropTplist, rdmDegTplist, rdmNodelist,
                        edgeRoadm1, edgeRoadm2));
                connectionServMap.putAll(createXpdrCepsAndConnectionsPht(xpdrNetworkTplist, xpdrNodelist));
                connectionServMap.putAll(createXpdrCepsAndConnectionsOdu(xpdrNetworkTplist, xpdrNodelist));
                // Top connection in the DSR layer, between client ports of the xpdrs
                connectionServMap.putAll(createXpdrCepsAndConnectionsDsr(xpdrClientTplist, xpdrNodelist));
                break;
            default:
                LOG.error("Service type format not supported");
        }
        return connectionServMap;
    }

    private Map<ConnectionKey,Connection> createXpdrCepsAndConnectionsDsr(List<String> xpdrClientTplist,
                                                                          List<String> xpdrNodelist) {
        Map<ConnectionKey, Connection> connServMap = new HashMap<>();
        Map<org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev181210.cep.list.ConnectionEndPointKey,
                ConnectionEndPoint> cepMap = new HashMap<>();

        // Create 1 cep per Xpdr in the CLIENT and a top connection DSR between the CLIENT xpdrs
        for (String xpdr:xpdrNodelist) {
            LOG.info("Creating ceps and xc for xpdr {}", xpdr);
            String spcXpdrClient = xpdrClientTplist.stream().filter(netp -> netp.contains(xpdr)).findFirst().get();

            ConnectionEndPoint netCep1 = createCepXpdr(spcXpdrClient, DSR, DSR, LayerProtocolName.DSR);
            putXpdrCepInTopologyContext(xpdr, spcXpdrClient, DSR, DSR, netCep1);

            cepMap.put(netCep1.key(), netCep1);
        }

        // DSR top connection between edge xpdr CLIENT DSR
        String spcXpdr1 = xpdrClientTplist.stream().filter(adp -> adp.contains(xpdrNodelist
                .get(0))).findFirst().get();
        String spcXpdr2 = xpdrClientTplist.stream().filter(adp -> adp.contains(xpdrNodelist
                .get(xpdrNodelist.size() - 1))).findFirst().get();
        org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev181210.connectivity.context.Connection
                connection = createTopConnection(spcXpdr1, spcXpdr2, cepMap, DSR, LayerProtocolName.DSR);
        this.connectionFullMap.put(connection.key(), connection);

        // ODU top connection that will be added to the service object
        Connection conn = new ConnectionBuilder().setConnectionUuid(connection.getUuid()).build();
        connServMap.put(conn.key(), conn);

        return connServMap;
    }

    private Map<ConnectionKey, Connection> createXpdrCepsAndConnectionsOdu(List<String> xpdrNetworkTplist,
                                                                           List<String> xpdrNodelist) {
        Map<ConnectionKey, Connection> connServMap = new HashMap<>();
        Map<org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev181210.cep.list.ConnectionEndPointKey,
                ConnectionEndPoint> cepMap = new HashMap<>();
        // Create 1 cep per Xpdr in the I_ODU and E_ODU, X connection between iODU and eODU and a top
        // connection iODU between the xpdrs
        for (String xpdr:xpdrNodelist) {
            LOG.info("Creating ceps and xc for xpdr {}", xpdr);
            String spcXpdrNetwork = xpdrNetworkTplist.stream().filter(netp -> netp.contains(xpdr)).findFirst().get();

            ConnectionEndPoint netCep1 = createCepXpdr(spcXpdrNetwork, E_ODU, DSR, LayerProtocolName.ODU);
            putXpdrCepInTopologyContext(xpdr, spcXpdrNetwork, PHTNC_MEDIA, OTSI, netCep1);
            ConnectionEndPoint netCep2 = createCepXpdr(spcXpdrNetwork, I_ODU, DSR, LayerProtocolName.ODU);
            putXpdrCepInTopologyContext(xpdr, spcXpdrNetwork, E_OTSI, OTSI, netCep2);

            cepMap.put(netCep1.key(), netCep1);
            cepMap.put(netCep2.key(), netCep2);

            // Create x connection between I_ODU and E_ODU within xpdr
            org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev181210.connectivity.context.Connection
                    connection = createXCBetweenCeps(netCep1, netCep2, spcXpdrNetwork, spcXpdrNetwork, ODU,
                    LayerProtocolName.ODU);
            this.connectionFullMap.put(connection.key(), connection);

            // Create X connection that will be added to the service object
            Connection conn = new ConnectionBuilder().setConnectionUuid(connection.getUuid()).build();
            connServMap.put(conn.key(), conn);
        }

        // ODU top connection between edge xpdr I_ODU
        String spcXpdr1 = xpdrNetworkTplist.stream().filter(adp -> adp.contains(xpdrNodelist
                .get(0))).findFirst().get();
        String spcXpdr2 = xpdrNetworkTplist.stream().filter(adp -> adp.contains(xpdrNodelist
                .get(xpdrNodelist.size() - 1))).findFirst().get();
        org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev181210.connectivity.context.Connection
                connection = createTopConnection(spcXpdr1, spcXpdr2, cepMap, I_ODU, LayerProtocolName.ODU);
        this.connectionFullMap.put(connection.key(), connection);

        // ODU top connection that will be added to the service object
        Connection conn = new ConnectionBuilder().setConnectionUuid(connection.getUuid()).build();
        connServMap.put(conn.key(), conn);

        return connServMap;
    }

    private Map<ConnectionKey, Connection> createXpdrCepsAndConnectionsPht(List<String> xpdrNetworkTplist,
                                                                           List<String> xpdrNodelist) {
        Map<ConnectionKey, Connection> connServMap = new HashMap<>();
        Map<org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev181210.cep.list.ConnectionEndPointKey,
                ConnectionEndPoint> cepMap = new HashMap<>();

        // create ceps and x connections within xpdr
        for (String xpdr:xpdrNodelist) {
            LOG.info("Creating ceps and xc for xpdr {}", xpdr);
            String spcXpdrNetwork = xpdrNetworkTplist.stream().filter(netp -> netp.contains(xpdr)).findFirst().get();
            // There should be 1 network tp per xpdr
            // TODO photonic media model should be updated to have the corresponding CEPs. I will just create
            //  3 different MC CEPs giving different IDs to show that they are different
            // Create 3 CEPs for each xpdr otsi node and the corresponding cross connection matchin the NEPs
            ConnectionEndPoint netCep1 = createCepXpdr(spcXpdrNetwork, PHTNC_MEDIA, OTSI,
                    LayerProtocolName.PHOTONICMEDIA);
            putXpdrCepInTopologyContext(xpdr, spcXpdrNetwork, PHTNC_MEDIA, OTSI, netCep1);
            ConnectionEndPoint netCep2 = createCepXpdr(spcXpdrNetwork, E_OTSI, OTSI, LayerProtocolName.PHOTONICMEDIA);
            putXpdrCepInTopologyContext(xpdr, spcXpdrNetwork, E_OTSI, OTSI, netCep2);
            ConnectionEndPoint netCep3 = createCepXpdr(spcXpdrNetwork, I_OTSI, OTSI, LayerProtocolName.PHOTONICMEDIA);
            putXpdrCepInTopologyContext(xpdr, spcXpdrNetwork, I_OTSI, OTSI, netCep3);
            cepMap.put(netCep1.key(), netCep1);
            cepMap.put(netCep2.key(), netCep2);
            cepMap.put(netCep3.key(), netCep3);

            // Create x connection between I_OTSi and E_OTSi within xpdr
            org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev181210.connectivity.context.Connection
                    connection = createXCBetweenCeps(netCep2, netCep3, spcXpdrNetwork, spcXpdrNetwork, OTSI,
                    LayerProtocolName.PHOTONICMEDIA);
            this.connectionFullMap.put(connection.key(), connection);

            // Create X connection that will be added to the service object
            Connection conn = new ConnectionBuilder().setConnectionUuid(connection.getUuid()).build();
            connServMap.put(conn.key(), conn);
        }
        // OTSi top connection between edge I_OTSI Xpdr
        String spcXpdr1 = xpdrNetworkTplist.stream().filter(adp -> adp.contains(xpdrNodelist
                .get(0))).findFirst().get();
        String spcXpdr2 = xpdrNetworkTplist.stream().filter(adp -> adp.contains(xpdrNodelist
                .get(xpdrNodelist.size() - 1))).findFirst().get();
        org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev181210.connectivity.context.Connection
                connection = createTopConnection(spcXpdr1, spcXpdr2, cepMap, I_OTSI, LayerProtocolName.PHOTONICMEDIA);
        this.connectionFullMap.put(connection.key(), connection);

        // OTSi top connection that will be added to the service object
        Connection conn = new ConnectionBuilder().setConnectionUuid(connection.getUuid()).build();
        connServMap.put(conn.key(), conn);


        return connServMap;
    }

    private Map<ConnectionKey, Connection> createRoadmCepsAndConnections(List<String> rdmAddDropTplist,
                                                                         List<String> rdmDegTplist,
                                                                         List<String> rdmNodelist,
                                                                         String edgeRoadm1, String edgeRoadm2) {
        // TODO: will need to check if things exist already or not
        Map<ConnectionKey, Connection> connServMap = new HashMap<>();
        Map<org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev181210.cep.list.ConnectionEndPointKey,
                ConnectionEndPoint> cepMap = new HashMap<>();
        // create ceps and x connections within roadm
        for (String roadm : rdmNodelist) {
            LOG.info("Creating ceps and xc for roadm {}", roadm);
            String spcRdmAD = rdmAddDropTplist.stream().filter(adp -> adp.contains(roadm)).findFirst().get();
            // There should be only 1 AD and 1 DEG per roadm
            // TODO photonic media model should be updated to have the corresponding CEPs. I will just create
            //  3 different MC CEPs giving different IDs to show that they are different
            // Create 3 CEPs for each AD and DEG and the corresponding cross connections, matching the NEPs
            // created in the topology creation
            // add CEPs to the topology to the corresponding ONEP
            ConnectionEndPoint adCep1 = createCepRoadm(spcRdmAD, PHTNC_MEDIA);
            putRdmCepInTopologyContext(roadm, spcRdmAD, PHTNC_MEDIA, adCep1);
            ConnectionEndPoint adCep2 = createCepRoadm(spcRdmAD, MC);
            putRdmCepInTopologyContext(roadm, spcRdmAD, MC, adCep2);
            ConnectionEndPoint adCep3 = createCepRoadm(spcRdmAD, OTSI_MC);
            putRdmCepInTopologyContext(roadm, spcRdmAD, OTSI_MC, adCep3);
            cepMap.put(adCep1.key(), adCep1);
            cepMap.put(adCep2.key(), adCep2);
            cepMap.put(adCep3.key(), adCep3);

            String spcRdmDEG = rdmDegTplist.stream().filter(adp -> adp.contains(roadm)).findFirst().get();

            ConnectionEndPoint degCep1 = createCepRoadm(spcRdmDEG, PHTNC_MEDIA);
            putRdmCepInTopologyContext(roadm, spcRdmDEG, PHTNC_MEDIA, degCep1);
            ConnectionEndPoint degCep2 = createCepRoadm(spcRdmDEG, MC);
            putRdmCepInTopologyContext(roadm, spcRdmDEG, MC, degCep2);
            ConnectionEndPoint degCep3 = createCepRoadm(spcRdmDEG, OTSI_MC);
            putRdmCepInTopologyContext(roadm, spcRdmDEG, OTSI_MC, degCep3);
            cepMap.put(degCep1.key(), degCep1);
            cepMap.put(degCep2.key(), degCep2);
            cepMap.put(degCep3.key(), degCep3);

            // Create X connections between MC and OTSi_MC for full map
            org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev181210.connectivity.context.Connection
                    connection1 = createXCBetweenCeps(adCep2, degCep2, spcRdmAD, spcRdmDEG, MC,
                    LayerProtocolName.PHOTONICMEDIA);
            org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev181210.connectivity.context.Connection
                    connection2 = createXCBetweenCeps(adCep3, degCep3, spcRdmAD, spcRdmDEG, OTSI_MC,
                    LayerProtocolName.PHOTONICMEDIA);
            this.connectionFullMap.put(connection1.key(), connection1);
            this.connectionFullMap.put(connection2.key(), connection2);

            // Create X connections that will be added to the service object
            Connection conn1 = new ConnectionBuilder().setConnectionUuid(connection1.getUuid()).build();
            Connection conn2 = new ConnectionBuilder().setConnectionUuid(connection2.getUuid()).build();
            connServMap.put(conn1.key(), conn1);
            connServMap.put(conn2.key(), conn2);
        }
        // create top connections between roadms: MC connections between AD MC CEPs of roadms
        for (int i = 0; i < rdmNodelist.size(); i++) {
            if (rdmNodelist.size() < (i + 1)) {
                LOG.info("Reached last roadm. No more MC connections");
                break;
            }
            // Current roadm with roadm i + 1 --> MC
            String roadm1 = rdmNodelist.get(i);
            String spcRdmAD1 = rdmAddDropTplist.stream().filter(adp -> adp.contains(roadm1)).findFirst().get();
            String roadm2 = rdmNodelist.get(i + 1);
            String spcRdmAD2 = rdmAddDropTplist.stream().filter(adp -> adp.contains(roadm2)).findFirst().get();

            // Create top connections between MC for full map
            org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev181210.connectivity.context.Connection
                    connection = createTopConnection(spcRdmAD1, spcRdmAD2, cepMap, MC, LayerProtocolName.PHOTONICMEDIA);
            this.connectionFullMap.put(connection.key(), connection);

            // Create top connections that will be added to the service object
            Connection conn = new ConnectionBuilder().setConnectionUuid(connection.getUuid()).build();
            connServMap.put(conn.key(), conn);
        }

        // OTSiMC top connection between edge roadms
        String spcRdmAD1 = rdmAddDropTplist.stream().filter(adp -> adp.contains(edgeRoadm1)).findFirst().get();
        String spcRdmAD2 = rdmAddDropTplist.stream().filter(adp -> adp.contains(edgeRoadm2)).findFirst().get();
        org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev181210.connectivity.context.Connection
                connection = createTopConnection(spcRdmAD1, spcRdmAD2, cepMap, OTSI_MC,
                LayerProtocolName.PHOTONICMEDIA);
        this.connectionFullMap.put(connection.key(), connection);

        // OTSiMC top connections that will be added to the service object
        Connection conn = new ConnectionBuilder().setConnectionUuid(connection.getUuid()).build();
        connServMap.put(conn.key(), conn);
        return connServMap;
    }

    private org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev181210.connectivity.context.Connection
            createTopConnection(String tp1, String tp2,
                                Map<org.opendaylight.yang.gen.v1.urn
                                        .onf.otcc.yang.tapi.connectivity.rev181210.cep.list.ConnectionEndPointKey,
                                ConnectionEndPoint> cepMap, String qual, LayerProtocolName topPortocol) {
        // find cep for each AD MC of roadm 1 and 2
        Name connName = new NameBuilder()
                .setValueName("Connection name")
                .setValue(String.join("+", "TOP", tp1, tp2, qual))
                .build();
        org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev181210.ConnectionEndPoint adCep1 =
                cepMap.get(new org.opendaylight.yang.gen.v1.urn
                        .onf.otcc.yang.tapi.connectivity.rev181210.cep.list.ConnectionEndPointKey(
                        new Uuid(UUID.nameUUIDFromBytes((String.join("+", "CEP", tp1.split("\\+")[0],
                                qual, tp1.split("\\+")[1])).getBytes(Charset.forName("UTF-8")))
                                .toString())));
        org.opendaylight.yang.gen.v1.urn
                .onf.otcc.yang.tapi.connectivity.rev181210.connection.ConnectionEndPoint cep1 =
                new org.opendaylight.yang.gen.v1.urn
                        .onf.otcc.yang.tapi.connectivity.rev181210.connection.ConnectionEndPointBuilder()
                        .setNodeEdgePointUuid(adCep1.getClientNodeEdgePoint().entrySet().iterator()
                                .next().getValue().getNodeEdgePointUuid())
                        .setTopologyUuid(adCep1.getClientNodeEdgePoint().entrySet().iterator()
                                .next().getValue().getTopologyUuid())
                        .setNodeUuid(adCep1.getClientNodeEdgePoint().entrySet().iterator()
                                .next().getValue().getNodeUuid())
                        .setConnectionEndPointUuid(adCep1.getUuid())
                        .build();
        org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev181210.ConnectionEndPoint adCep2 =
                cepMap.get(new org.opendaylight.yang.gen.v1.urn
                        .onf.otcc.yang.tapi.connectivity.rev181210.cep.list.ConnectionEndPointKey(
                        new Uuid(UUID.nameUUIDFromBytes((String.join("+", "CEP", tp2.split("\\+")[0],
                                qual, tp2.split("\\+")[1])).getBytes(Charset.forName("UTF-8")))
                                .toString())));
        org.opendaylight.yang.gen.v1.urn
                .onf.otcc.yang.tapi.connectivity.rev181210.connection.ConnectionEndPoint cep2 =
                new org.opendaylight.yang.gen.v1.urn
                        .onf.otcc.yang.tapi.connectivity.rev181210.connection.ConnectionEndPointBuilder()
                        .setNodeEdgePointUuid(adCep2.getClientNodeEdgePoint().entrySet().iterator()
                                .next().getValue().getNodeEdgePointUuid())
                        .setTopologyUuid(adCep2.getClientNodeEdgePoint().entrySet().iterator()
                                .next().getValue().getTopologyUuid())
                        .setNodeUuid(adCep2.getClientNodeEdgePoint().entrySet().iterator()
                                .next().getValue().getNodeUuid())
                        .setConnectionEndPointUuid(adCep2.getUuid())
                        .build();
        Map<ConnectionEndPointKey, org.opendaylight.yang.gen.v1.urn
                .onf.otcc.yang.tapi.connectivity.rev181210.connection.ConnectionEndPoint> ceps = new HashMap<>();
        ceps.put(cep1.key(), cep1);
        ceps.put(cep2.key(), cep2);
        // TODO: lower connection, supported link.......
        return new org.opendaylight.yang.gen.v1.urn
                .onf.otcc.yang.tapi.connectivity.rev181210.connectivity.context.ConnectionBuilder()
                .setUuid(new Uuid(UUID.nameUUIDFromBytes((String.join("+", "TOP", tp1, tp2, qual))
                        .getBytes(Charset.forName("UTF-8"))).toString()))
                .setName(Map.of(connName.key(), connName))
                .setConnectionEndPoint(ceps)
                .setOperationalState(OperationalState.ENABLED)
                .setLayerProtocolName(topPortocol)
                .setLifecycleState(LifecycleState.INSTALLED)
                .setDirection(ForwardingDirection.BIDIRECTIONAL)
                .build();
    }

    private org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev181210.connectivity.context.Connection
            createXCBetweenCeps(ConnectionEndPoint cep1, ConnectionEndPoint cep2, String tp1, String tp2, String qual,
                                LayerProtocolName xcProtocol) {

        Name connName = new NameBuilder()
                .setValueName("Connection name")
                .setValue(String.join("+", "XC", tp1, tp2, qual))
                .build();
        org.opendaylight.yang.gen.v1.urn
                .onf.otcc.yang.tapi.connectivity.rev181210.connection.ConnectionEndPoint cepServ1 =
                new org.opendaylight.yang.gen.v1.urn
                        .onf.otcc.yang.tapi.connectivity.rev181210.connection.ConnectionEndPointBuilder()
                        .setNodeEdgePointUuid(cep1.getClientNodeEdgePoint().entrySet().iterator()
                                .next().getValue().getNodeEdgePointUuid())
                        .setTopologyUuid(cep1.getClientNodeEdgePoint().entrySet().iterator()
                                .next().getValue().getTopologyUuid())
                        .setNodeUuid(cep1.getClientNodeEdgePoint().entrySet().iterator()
                                .next().getValue().getNodeUuid())
                        .setConnectionEndPointUuid(cep1.getUuid())
                        .build();
        org.opendaylight.yang.gen.v1.urn
                .onf.otcc.yang.tapi.connectivity.rev181210.connection.ConnectionEndPoint cepServ2 =
                new org.opendaylight.yang.gen.v1.urn
                        .onf.otcc.yang.tapi.connectivity.rev181210.connection.ConnectionEndPointBuilder()
                        .setNodeEdgePointUuid(cep2.getClientNodeEdgePoint().entrySet().iterator()
                                .next().getValue().getNodeEdgePointUuid())
                        .setTopologyUuid(cep2.getClientNodeEdgePoint().entrySet().iterator()
                                .next().getValue().getTopologyUuid())
                        .setNodeUuid(cep2.getClientNodeEdgePoint().entrySet().iterator()
                                .next().getValue().getNodeUuid())
                        .setConnectionEndPointUuid(cep2.getUuid())
                        .build();
        Map<ConnectionEndPointKey, org.opendaylight.yang.gen.v1.urn
                .onf.otcc.yang.tapi.connectivity.rev181210.connection.ConnectionEndPoint> ceps = new HashMap<>();
        ceps.put(cepServ1.key(), cepServ1);
        ceps.put(cepServ2.key(), cepServ2);
        // TODO: lower connection, supported link.......
        return new org.opendaylight.yang.gen.v1.urn
                .onf.otcc.yang.tapi.connectivity.rev181210.connectivity.context.ConnectionBuilder()
                .setUuid(new Uuid(UUID.nameUUIDFromBytes((String.join("+", "XC", tp1, tp2, qual))
                        .getBytes(Charset.forName("UTF-8"))).toString()))
                .setName(Map.of(connName.key(), connName))
                .setConnectionEndPoint(ceps)
                .setOperationalState(OperationalState.ENABLED)
                .setLayerProtocolName(xcProtocol)
                .setLifecycleState(LifecycleState.INSTALLED)
                .setDirection(ForwardingDirection.BIDIRECTIONAL)
                .build();
    }

    private ConnectionEndPoint createCepRoadm(String id, String qualifier) {
        Name cepName = new NameBuilder()
                .setValueName("ConnectionEndPoint name")
                .setValue(String.join("+", id.split("\\+")[0], qualifier,
                        id.split("\\+")[1]))
                .build();
        ClientNodeEdgePoint cnep = new ClientNodeEdgePointBuilder()
                .setNodeEdgePointUuid(new Uuid(UUID.nameUUIDFromBytes((String.join("+", id.split("\\+")[0],
                        qualifier, id.split("\\+")[1])).getBytes(Charset.forName("UTF-8")))
                        .toString()))
                .setNodeUuid(new Uuid(UUID.nameUUIDFromBytes((String.join("+",id.split("\\+")[0],
                        qualifier)).getBytes(Charset.forName("UTF-8")))
                        .toString()))
                .setTopologyUuid(new Uuid(UUID.nameUUIDFromBytes(TopologyUtils.T0_MULTILAYER
                        .getBytes(Charset.forName("UTF-8"))).toString()))
                .build();
        // TODO: add augmentation with the corresponding cep-spec (i.e. MC, OTSiMC...)
        // TODO: add parent ONEP??
        ConnectionEndPointBuilder cepBldr = new ConnectionEndPointBuilder()
                .setUuid(new Uuid(UUID.nameUUIDFromBytes((String.join("+", "CEP", id.split("\\+")[0],
                        qualifier, id.split("\\+")[1])).getBytes(Charset.forName("UTF-8")))
                        .toString()))
                .setClientNodeEdgePoint(Map.of(cnep.key(), cnep))
                .setName(Map.of(cepName.key(), cepName))
                .setConnectionPortRole(PortRole.SYMMETRIC)
                .setConnectionPortDirection(PortDirection.BIDIRECTIONAL)
                .setOperationalState(OperationalState.ENABLED)
                .setLifecycleState(LifecycleState.INSTALLED)
                .setLayerProtocolName(LayerProtocolName.PHOTONICMEDIA);
        return cepBldr.build();
    }

    private ConnectionEndPoint createCepXpdr(String id, String qualifier, String nodeLayer,
                                             LayerProtocolName cepProtocol) {
        Name cepName = new NameBuilder()
                .setValueName("ConnectionEndPoint name")
                .setValue(String.join("+", id.split("\\+")[0], qualifier,
                        id.split("\\+")[1]))
                .build();
        ClientNodeEdgePoint cnep = new ClientNodeEdgePointBuilder()
                .setNodeEdgePointUuid(new Uuid(UUID.nameUUIDFromBytes((String.join("+", id.split("\\+")[0],
                        qualifier, id.split("\\+")[1])).getBytes(Charset.forName("UTF-8")))
                        .toString()))
                .setNodeUuid(new Uuid(UUID.nameUUIDFromBytes((String.join("+",id.split("\\+")[0],
                        nodeLayer)).getBytes(Charset.forName("UTF-8")))
                        .toString()))
                .setTopologyUuid(new Uuid(UUID.nameUUIDFromBytes(TopologyUtils.T0_MULTILAYER
                        .getBytes(Charset.forName("UTF-8"))).toString()))
                .build();
        // TODO: add augmentation with the corresponding cep-spec (i.e. MC, OTSiMC...)
        // TODO: add parent ONEP??
        ConnectionEndPointBuilder cepBldr = new ConnectionEndPointBuilder()
                .setUuid(new Uuid(UUID.nameUUIDFromBytes((String.join("+", "CEP", id.split("\\+")[0],
                        qualifier, id.split("\\+")[1])).getBytes(Charset.forName("UTF-8")))
                        .toString()))
                .setClientNodeEdgePoint(Map.of(cnep.key(), cnep))
                .setName(Map.of(cepName.key(), cepName))
                .setConnectionPortRole(PortRole.SYMMETRIC)
                .setConnectionPortDirection(PortDirection.BIDIRECTIONAL)
                .setOperationalState(OperationalState.ENABLED)
                .setLifecycleState(LifecycleState.INSTALLED)
                .setLayerProtocolName(cepProtocol);
        return cepBldr.build();
    }

    private EndPoint mapServiceZEndPoint(
            org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev190531.service.ServiceZEnd
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
        org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev181210.local._class.Name name =
                new org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev181210.local._class.NameBuilder()
                        .setValueName("OpenROADM info").setValue(String.join("-",
                        serviceZEnd.getClli(), serviceZEnd.getTxDirection().getPort().getPortDeviceName(),
                        serviceZEnd.getTxDirection().getPort().getPortName()))
                        .build();
        return endPointBuilder
                .setServiceInterfacePoint(new ServiceInterfacePointBuilder()
                        .setServiceInterfacePointUuid(sipUuid)
                        .build())
                .setName(Map.of(name.key(), name))
                .setAdministrativeState(AdministrativeState.UNLOCKED)
                .setDirection(PortDirection.BIDIRECTIONAL)
                .setLifecycleState(LifecycleState.INSTALLED)
                .setOperationalState(OperationalState.ENABLED)
                .setLayerProtocolName(layerProtocols)
                .setCapacity(new CapacityBuilder()
                        .setTotalSize(new TotalSizeBuilder()
                                .setValue(Uint64.valueOf(serviceZEnd.getServiceRate()))
                                .setUnit(CapacityUnit.GBPS)
                                .build())
                        .setBandwidthProfile(new BandwidthProfileBuilder().build()) // TODO: implement bandwidth profile
                        .build())
                .setProtectionRole(ProtectionRole.WORK)
                .setRole(PortRole.SYMMETRIC)
                .setLocalId(serviceNodeId)
                .build();
    }

    private EndPoint mapServiceAEndPoint(
            org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev190531.service.ServiceAEnd
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
        org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev181210.local._class.Name name =
                new org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev181210.local._class.NameBuilder()
                        .setValueName("OpenROADM info").setValue(String.join("-",
                        serviceAEnd.getClli(), serviceAEnd.getTxDirection().getPort().getPortDeviceName(),
                        serviceAEnd.getTxDirection().getPort().getPortName()))
                        .build();
        return endPointBuilder
                .setServiceInterfacePoint(new ServiceInterfacePointBuilder()
                        .setServiceInterfacePointUuid(sipUuid)
                        .build())
                .setName(Map.of(name.key(), name))
                .setAdministrativeState(AdministrativeState.UNLOCKED)
                .setDirection(PortDirection.BIDIRECTIONAL)
                .setLifecycleState(LifecycleState.INSTALLED)
                .setOperationalState(OperationalState.ENABLED)
                .setLayerProtocolName(layerProtocols)
                .setCapacity(new CapacityBuilder()
                        .setTotalSize(new TotalSizeBuilder()
                                .setValue(Uint64.valueOf(serviceAEnd.getServiceRate()))
                                .setUnit(CapacityUnit.GBPS)
                                .build())
                        .setBandwidthProfile(new BandwidthProfileBuilder().build()) // TODO: implement bandwidth profile
                        .build())
                .setProtectionRole(ProtectionRole.WORK)
                .setRole(PortRole.SYMMETRIC)
                .setLocalId(serviceNodeId)
                .build();
    }

    private Uuid getSipIdFromZend(Map<ZToAKey, ZToA> mapztoa, String serviceNodeId, ServiceFormat serviceFormat) {
        Uuid zendUuid = null;
        if (serviceNodeId.contains("ROADM")) {
            // Service from ROADM to ROADM
            // AddDrop-AddDrop ports --> MC layer SIPs
            ZToA firstElement = mapztoa.values().stream().filter(ztoa -> ztoa.getId().equals("0")).findFirst().get();
            TerminationPoint tp = (TerminationPoint) firstElement.getResource().getResource();
            Uuid sipUuid = new Uuid(UUID.nameUUIDFromBytes((String.join("+", "SIP",
                    tp.getTpNodeId().split("-")[0], MC, tp.getTpId())).getBytes(Charset.forName("UTF-8")))
                    .toString());
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
            ZToA firstElement = mapztoa.values().stream().filter(ztoa -> ztoa.getId().equals("0")).findFirst().get();
            TerminationPoint tp = (TerminationPoint) firstElement.getResource().getResource();
            Uuid sipUuid;
            switch (serviceFormat) {
                case ODU:
                    // Network-Network ports --> iODU layer SIPs
                    sipUuid = new Uuid(UUID.nameUUIDFromBytes((String.join("+", "SIP",
                            tp.getTpNodeId(), I_ODU, tp.getTpId())).getBytes(Charset.forName("UTF-8")))
                            .toString());
                    break;
                case OTU:
                    // Network-Network ports --> iOTSi layer SIPs
                    sipUuid = new Uuid(UUID.nameUUIDFromBytes((String.join("+", "SIP",
                            tp.getTpNodeId(), I_OTSI, tp.getTpId())).getBytes(Charset.forName("UTF-8")))
                            .toString());
                    break;
                case Ethernet:
                    // Client-client ports --> DSR layer SIPs
                    sipUuid = new Uuid(UUID.nameUUIDFromBytes((String.join("+", "SIP",
                            tp.getTpNodeId(), DSR, tp.getTpId())).getBytes(Charset.forName("UTF-8")))
                            .toString());
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
        if (serviceNodeId.contains("ROADM")) {
            // Service from ROADM to ROADM
            // AddDrop-AddDrop ports --> MC layer SIPs
            AToZ firstElement = mapatoz.values().stream().filter(ztoa -> ztoa.getId().equals("0")).findFirst().get();
            TerminationPoint tp = (TerminationPoint) firstElement.getResource().getResource();
            Uuid sipUuid = new Uuid(UUID.nameUUIDFromBytes((String.join("+", "SIP",
                    tp.getTpNodeId().split("-")[0], MC, tp.getTpId())).getBytes(Charset.forName("UTF-8")))
                    .toString());
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
            AToZ firstElement = mapatoz.values().stream().filter(ztoa -> ztoa.getId().equals("0")).findFirst().get();
            TerminationPoint tp = (TerminationPoint) firstElement.getResource().getResource();
            Uuid sipUuid;
            switch (serviceFormat) {
                case ODU:
                    // Network-Network ports --> iODU layer SIPs
                    sipUuid = new Uuid(UUID.nameUUIDFromBytes((String.join("+", "SIP",
                            tp.getTpNodeId(), I_ODU, tp.getTpId())).getBytes(Charset.forName("UTF-8")))
                            .toString());
                    break;
                case OTU:
                    // Network-Network ports --> iOTSi layer SIPs
                    sipUuid = new Uuid(UUID.nameUUIDFromBytes((String.join("+", "SIP",
                            tp.getTpNodeId(), I_OTSI, tp.getTpId())).getBytes(Charset.forName("UTF-8")))
                            .toString());
                    break;
                case Ethernet:
                    // Client-client ports --> DSR layer SIPs
                    sipUuid = new Uuid(UUID.nameUUIDFromBytes((String.join("+", "SIP",
                            tp.getTpNodeId(), DSR, tp.getTpId())).getBytes(Charset.forName("UTF-8")))
                            .toString());
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
        // Give uuids so that it is easier to look for things: topology uuid, node uuid, nep uuid, cep
        Uuid topoUuid = new Uuid(UUID.nameUUIDFromBytes(TopologyUtils.T0_MULTILAYER.getBytes(Charset.forName("UTF-8")))
                .toString());
        Uuid nodeUuid = new Uuid(UUID.nameUUIDFromBytes(String.join("+", node, PHTNC_MEDIA)
                .getBytes(Charset.forName("UTF-8"))).toString());
        Uuid nepUuid = new Uuid(UUID.nameUUIDFromBytes(String.join("+", node, qual, spcRdmAD.split("\\+")[1])
                .getBytes(Charset.forName("UTF-8"))).toString());
        this.tapiContext.updateTopologyWithCep(topoUuid, nodeUuid, nepUuid, cep);
    }

    private void putXpdrCepInTopologyContext(String node, String spcXpdrNet, String qual, String nodeLayer,
                                             ConnectionEndPoint cep) {
        // Give uuids so that it is easier to look for things: topology uuid, node uuid, nep uuid, cep
        Uuid topoUuid = new Uuid(UUID.nameUUIDFromBytes(TopologyUtils.T0_MULTILAYER.getBytes(Charset.forName("UTF-8")))
                .toString());
        Uuid nodeUuid = new Uuid(UUID.nameUUIDFromBytes(String.join("+", node, nodeLayer)
                .getBytes(Charset.forName("UTF-8"))).toString());
        Uuid nepUuid = new Uuid(UUID.nameUUIDFromBytes(String.join("+", node, qual, spcXpdrNet.split("\\+")[1])
                .getBytes(Charset.forName("UTF-8"))).toString());
        this.tapiContext.updateTopologyWithCep(topoUuid, nodeUuid, nepUuid, cep);
    }

    public Map<org.opendaylight.yang.gen.v1.urn
            .onf.otcc.yang.tapi.connectivity.rev181210.connectivity.context.ConnectionKey,
            org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev181210.connectivity.context.Connection>
            getConnectionFullMap() {
        return this.connectionFullMap;
    }

    public ServiceCreateInput createORServiceInput(CreateConnectivityServiceInput input, Uuid serviceUuid) {
        // TODO: not taking into account all the constraints. Only using EndPoints and Connectivity Constraint.
        Map<org.opendaylight.yang.gen.v1.urn
                .onf.otcc.yang.tapi.connectivity.rev181210.create.connectivity.service.input.EndPointKey,
                org.opendaylight.yang.gen.v1.urn
                        .onf.otcc.yang.tapi.connectivity.rev181210.create.connectivity.service.input.EndPoint>
                endPointMap = input.getEndPoint();
        ConnectivityConstraint constraint = input.getConnectivityConstraint();
        ConnectionType connType = null;
        ServiceFormat serviceFormat = null;
        switch (constraint.getServiceLayer().getIntValue()) {
            case 0:
                LOG.info("ODU");
                connType = ConnectionType.Infrastructure;
                serviceFormat = ServiceFormat.ODU;
                break;
            case 2:
                LOG.info("DSR");
                connType = ConnectionType.Service;
                serviceFormat = ServiceFormat.Ethernet;
                break;
            case 3:
                LOG.info("PHOTONIC");
                connType = getConnectionTypePhtnc(endPointMap.values());
                serviceFormat = getServiceFormatPhtnc(endPointMap.values());
                break;
            default:
                LOG.info("Service type {} not supported", constraint.getServiceLayer().getName());
        }
        // map endpoints into service end points. Map the type of service from TAPI to OR
        ServiceAEnd serviceAEnd = tapiEndPointToServiceAPoint(endPointMap.values().stream().findFirst().get(),
                serviceFormat);
        ServiceZEnd serviceZEnd = tapiEndPointToServiceZPoint(endPointMap.values().stream().skip(1).findFirst().get(),
                serviceFormat);
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
                .build();
    }

    private ServiceZEnd tapiEndPointToServiceZPoint(
            org.opendaylight.yang.gen.v1.urn
                    .onf.otcc.yang.tapi.connectivity.rev181210.create.connectivity.service.input.EndPoint endPoint,
            ServiceFormat serviceFormat) {
        String name = endPoint.getName().values().stream().findFirst().get().getValue();
        String clli = name.split("-")[0];
        String nodeid = name.split("-")[1];
        String txPortDeviceName = String.join("-", name.split("-")[1], name.split("-")[2]);
        String txPortName = "";
        if (nodeid.contains("ROADM")) {
            txPortName = String.join("-", name.split("-")[3], name.split("-")[4],
                    name.split("-")[5]);
        } else {
            txPortName = String.join("-", name.split("-")[3], name.split("-")[4]);
        }
        String rxPortDeviceName = txPortDeviceName;
        String rxPortName = txPortName;
        ServiceZEndBuilder serviceZEndBuilder = new ServiceZEndBuilder()
                .setClli(clli)
                .setNodeId(new NodeIdType(nodeid))
                .setOpticType(OpticTypes.Gray)
                .setServiceFormat(serviceFormat)
                .setServiceRate(Uint32.valueOf(100))
                .setTxDirection(new TxDirectionBuilder()
                        .setPort(new PortBuilder()
                                .setPortDeviceName(txPortDeviceName)
                                .setPortName(txPortName)
                                .setPortRack(PORT_RACK_VALUE)
                                .setPortShelf("00")
                                .setPortType(PORT_TYPE)
                                .build())
                        .setLgx(new LgxBuilder()
                                .setLgxDeviceName(LGX_DEVICE_NAME)
                                .setLgxPortName(LGX_PORT_NAME)
                                .setLgxPortRack(PORT_RACK_VALUE)
                                .setLgxPortShelf("00")
                                .build())
                        .build())
                .setRxDirection(new RxDirectionBuilder()
                        .setPort(new PortBuilder()
                                .setPortDeviceName(rxPortDeviceName)
                                .setPortName(rxPortName)
                                .setPortRack(PORT_RACK_VALUE)
                                .setPortShelf("00")
                                .setPortType(PORT_TYPE)
                                .build())
                        .setLgx(new LgxBuilder()
                                .setLgxDeviceName(LGX_DEVICE_NAME)
                                .setLgxPortName(LGX_PORT_NAME)
                                .setLgxPortRack(PORT_RACK_VALUE)
                                .setLgxPortShelf("00")
                                .build())
                        .build());
        if (serviceFormat.equals(ServiceFormat.ODU)) {
            serviceZEndBuilder.setOduServiceRate(ODU4.class);
        }
        if (serviceFormat.equals(ServiceFormat.OTU)) {
            serviceZEndBuilder.setOtuServiceRate(OTU4.class);
        }
        return serviceZEndBuilder.build();
    }

    private ServiceAEnd tapiEndPointToServiceAPoint(
            org.opendaylight.yang.gen.v1.urn
                    .onf.otcc.yang.tapi.connectivity.rev181210.create.connectivity.service.input.EndPoint endPoint,
            ServiceFormat serviceFormat) {
        String name = endPoint.getName().values().stream().findFirst().get().getValue();
        String clli = name.split("-")[0];
        String nodeid = name.split("-")[1];
        String txPortDeviceName = String.join("-", name.split("-")[1], name.split("-")[2]);
        String txPortName = "";
        if (nodeid.contains("ROADM")) {
            txPortName = String.join("-", name.split("-")[3], name.split("-")[4],
                    name.split("-")[5]);
        } else {
            txPortName = String.join("-", name.split("-")[3], name.split("-")[4]);
        }
        String rxPortDeviceName = txPortDeviceName;
        String rxPortName = txPortName;
        ServiceAEndBuilder serviceAEndBuilder = new ServiceAEndBuilder()
                .setClli(clli)
                .setNodeId(new NodeIdType(nodeid))
                .setOpticType(OpticTypes.Gray)
                .setServiceFormat(serviceFormat)
                .setServiceRate(Uint32.valueOf(100))
                .setTxDirection(new TxDirectionBuilder()
                        .setPort(new PortBuilder()
                                .setPortDeviceName(txPortDeviceName)
                                .setPortName(txPortName)
                                .setPortRack(PORT_RACK_VALUE)
                                .setPortShelf("00")
                                .setPortType(PORT_TYPE)
                                .build())
                        .setLgx(new LgxBuilder()
                                .setLgxDeviceName(LGX_DEVICE_NAME)
                                .setLgxPortName(LGX_PORT_NAME)
                                .setLgxPortRack(PORT_RACK_VALUE)
                                .setLgxPortShelf("00")
                                .build())
                        .build())
                .setRxDirection(new RxDirectionBuilder()
                        .setPort(new PortBuilder()
                                .setPortDeviceName(rxPortDeviceName)
                                .setPortName(rxPortName)
                                .setPortRack(PORT_RACK_VALUE)
                                .setPortShelf("00")
                                .setPortType(PORT_TYPE)
                                .build())
                        .setLgx(new LgxBuilder()
                                .setLgxDeviceName(LGX_DEVICE_NAME)
                                .setLgxPortName(LGX_PORT_NAME)
                                .setLgxPortRack(PORT_RACK_VALUE)
                                .setLgxPortShelf("00")
                                .build())
                        .build());
        if (serviceFormat.equals(ServiceFormat.ODU)) {
            serviceAEndBuilder.setOduServiceRate(ODU4.class);
        }
        if (serviceFormat.equals(ServiceFormat.OTU)) {
            serviceAEndBuilder.setOtuServiceRate(OTU4.class);
        }
        return serviceAEndBuilder.build();
    }

    private ConnectionType getConnectionTypePhtnc(Collection<org.opendaylight.yang.gen.v1.urn
            .onf.otcc.yang.tapi.connectivity.rev181210.create.connectivity.service.input.EndPoint> endPoints) {
        if (endPoints.stream().anyMatch(ep -> ep.getName().values().stream()
                .anyMatch(name -> name.getValue().contains("ROADM")))) {
            // EndPoints are ROADMs
            return ConnectionType.RoadmLine;
        }
        // EndPoints ar not ROADMs -> XPDR, MUXPDR, SWTICHPDR
        return ConnectionType.Infrastructure;
    }

    private ServiceFormat getServiceFormatPhtnc(
            Collection<org.opendaylight.yang.gen.v1.urn
                    .onf.otcc.yang.tapi.connectivity.rev181210.create.connectivity.service.input.EndPoint> endPoints) {
        if (endPoints.stream().anyMatch(ep -> ep.getName().values().stream()
                .anyMatch(name -> name.getValue().contains("ROADM")))) {
            // EndPoints are ROADMs
            return ServiceFormat.OC;
        }
        // EndPoints ar not ROADMs -> XPDR, MUXPDR, SWTICHPDR
        return ServiceFormat.OTU;
    }
}
