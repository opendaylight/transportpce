/*
 * Copyright © 2021 Nokia, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.tapi.listeners;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.transportpce.common.network.NetworkTransactionImpl;
import org.opendaylight.transportpce.common.network.NetworkTransactionService;
import org.opendaylight.transportpce.common.network.RequestProcessor;
import org.opendaylight.transportpce.tapi.TapiStringConstants;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev210701.ServicePathRpcResult;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev210701.TransportpcePceListener;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev210701.service.path.rpc.result.PathDescription;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev210701.service.path.rpc.result.PathDescriptionBuilder;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev220114.Network;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev220114.mapping.Mapping;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev220114.mapping.MappingKey;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev220114.network.Nodes;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev220114.network.NodesKey;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.types.rev200529.OpenroadmNodeType;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev210705.path.description.atoz.direction.AToZ;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev210705.pce.resource.resource.resource.Node;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev210705.pce.resource.resource.resource.TerminationPoint;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.service.types.rev200128.RpcStatusEx;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev181210.Context;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev181210.ForwardingDirection;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev181210.LayerProtocolName;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev181210.LifecycleState;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev181210.OperationalState;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev181210.PortDirection;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev181210.PortRole;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev181210.Uuid;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev181210.global._class.Name;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev181210.global._class.NameBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev181210.global._class.NameKey;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev181210.Context1;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev181210.CreateConnectivityServiceInput;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev181210.OwnedNodeEdgePoint1;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev181210.OwnedNodeEdgePoint1Builder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev181210.cep.list.ConnectionEndPoint;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev181210.cep.list.ConnectionEndPointBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev181210.connection.ConnectionEndPointKey;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev181210.connection.LowerConnection;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev181210.connection.LowerConnectionBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev181210.connection.LowerConnectionKey;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev181210.connection.end.point.ClientNodeEdgePoint;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev181210.connection.end.point.ClientNodeEdgePointBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev181210.connectivity.context.ConnectivityService;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev181210.connectivity.context.ConnectivityServiceBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev181210.connectivity.context.ConnectivityServiceKey;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev181210.connectivity.service.Connection;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev181210.connectivity.service.ConnectionBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev181210.connectivity.service.ConnectionKey;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev181210.context.ConnectivityContextBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev181210.context.topology.context.topology.node.owned.node.edge.point.CepList;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev181210.context.topology.context.topology.node.owned.node.edge.point.CepListBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.context.TopologyContext;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.node.OwnedNodeEdgePoint;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.node.OwnedNodeEdgePointBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.node.OwnedNodeEdgePointKey;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.topology.NodeKey;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.topology.context.Topology;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.topology.context.TopologyKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TapiPceListenerImpl implements TransportpcePceListener {

    private static final Logger LOG = LoggerFactory.getLogger(TapiPceListenerImpl.class);

    private final Uuid tapiTopoUuid = new Uuid(UUID.nameUUIDFromBytes(TapiStringConstants.T0_FULL_MULTILAYER
        .getBytes(Charset.forName("UTF-8"))).toString());
    private ServicePathRpcResult servicePathRpcResult;
    private CreateConnectivityServiceInput input;
    private Uuid serviceUuid;
    private final DataBroker dataBroker;
    private final NetworkTransactionService networkTransactionService;
    private final Map<org.opendaylight.yang.gen.v1.urn
        .onf.otcc.yang.tapi.connectivity.rev181210.connectivity.context.ConnectionKey,
        org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev181210.connectivity.context.Connection>
        connectionFullMap; // this variable is for complete connection objects

    public TapiPceListenerImpl(DataBroker dataBroker) {
        this.connectionFullMap = new HashMap<>();
        this.dataBroker = dataBroker;
        this.networkTransactionService = new NetworkTransactionImpl(new RequestProcessor(this.dataBroker));
    }

    @Override
    public void onServicePathRpcResult(ServicePathRpcResult notification) {
        if (compareServicePathRpcResult(notification)) {
            LOG.warn("ServicePathRpcResult already wired !");
            return;
        }
        servicePathRpcResult = notification;
        switch (servicePathRpcResult.getNotificationType().getIntValue()) {
            /* path-computation-request. */
            case 1:
                onPathComputationResult(notification);
                break;
            /* cancel-resource-reserve. */
            case 2:
                onCancelResourceResult(notification.getServiceName());
                break;
            default:
                break;
        }
    }

    /**
     * Process path computation request result.
     * @param notification the result notification.
     */
    private void onPathComputationResult(ServicePathRpcResult notification) {
        this.connectionFullMap.clear();
        LOG.info("PCE '{}' Notification received : {}",servicePathRpcResult.getNotificationType().getName(),
                notification);
        if (servicePathRpcResult.getStatus() == RpcStatusEx.Failed) {
            LOG.error("PCE path computation failed !");
            return;
        } else if (servicePathRpcResult.getStatus() == RpcStatusEx.Pending) {
            LOG.warn("PCE path computation returned a Penging RpcStatusEx code!");
            return;
        } else if (servicePathRpcResult.getStatus() != RpcStatusEx.Successful) {
            LOG.error("PCE path computation returned an unknown RpcStatusEx code!");
            return;
        }

        LOG.info("PCE calculation done OK !");
        if (servicePathRpcResult.getPathDescription() == null) {
            LOG.error("'PathDescription' parameter is null ");
            return;
        }
        PathDescription pathDescription = new PathDescriptionBuilder()
            .setAToZDirection(servicePathRpcResult.getPathDescription().getAToZDirection())
            .setZToADirection(servicePathRpcResult.getPathDescription().getZToADirection())
            .build();
        LOG.info("PathDescription for TAPI gets : {}", pathDescription);
        if (input == null) {
            LOG.error("Input is null !");
            return;
        }
        // TODO: check kind of service: based on the device Id of the input,
        //  verify the type of XPDR and the capacity and determine if it is an OTN service or pure WDM service
        // Create connections and ceps for the connectivity service.
        //  Connections must be with a locked stated. As the renderer hasnt implemented yet the oc's
        Map<ConnectionKey, Connection> connectionMap = createConnectionsAndCepsForService(pathDescription,
                input.getConnectivityConstraint().getServiceLayer());
        // add connections to connection context and to connectivity context
        updateConnectionContextWithConn(this.connectionFullMap, connectionMap, serviceUuid);
    }

    private Map<ConnectionKey, Connection> createConnectionsAndCepsForService(PathDescription pathDescription,
                                                                              LayerProtocolName serviceProtName) {
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
                .sorted(Comparator.comparing(AToZ::getId)).collect(Collectors.toList())) {
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
        LOG.info("ROADM node list = {}", rdmNodelist.toString());
        LOG.info("ROADM degree list = {}", rdmDegTplist.toString());
        LOG.info("ROADM addrop list = {}", rdmAddDropTplist.toString());
        LOG.info("XPDR node list = {}", xpdrNodelist.toString());
        LOG.info("XPDR network list = {}", xpdrNetworkTplist.toString());
        LOG.info("XPDR client list = {}", xpdrClientTplist.toString());
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
        switch (serviceProtName) {
            case PHOTONICMEDIA:
                // Identify number of ROADMs
                // - XC Connection between MC CEPs mapped from MC NEPs (within a roadm)
                // - XC Connection between OTSiMC CEPs mapped from OTSiMC NEPs (within a roadm)
                // - Top Connection MC betwwen MC CEPs of different roadms
                // - Top Connection OTSiMC betwwen OTSiMC CEPs of extreme roadms
                connectionServMap.putAll(createRoadmCepsAndConnections(rdmAddDropTplist, rdmDegTplist, rdmNodelist,
                        edgeRoadm1, edgeRoadm2));
                if (!pathDescription.getAToZDirection().getAToZ().values().stream().findFirst().get().getId()
                        .contains("ROADM")) {
                    // - XC Connection OTSi betwwen iOTSi y eOTSi of xpdr
                    // - Top connection OTSi between network ports of xpdrs in the Photonic media layer -> i_OTSi
                    connectionServMap.putAll(createXpdrCepsAndConnectionsPht(xpdrNetworkTplist, xpdrNodelist));
                }
                break;
            case ODU:
                LOG.info("OTN ODU service");
                // - XC Connection OTSi between iODU and eODU of xpdr
                // - Top connection in the ODU layer, between xpdr iODU ports
                connectionServMap.putAll(createXpdrCepsAndConnectionsOdu(xpdrNetworkTplist, xpdrNodelist));
                break;
            case ETH:
                LOG.info("WDM service");
                if (openroadmNodeType.equals(OpenroadmNodeType.TPDR)) {
                    // TODO: WDM service. Only och/otu4 needed and then directly DSR top connection.
                    //  Need to find the associated client ports of the network port
                    // - Same as for PHOTONIC MEDIA service
                    // - Do we nedd cross connection in the ODU layer??
                    // - Top connection DSR between client ports of the transponder
                    connectionServMap.putAll(createRoadmCepsAndConnections(rdmAddDropTplist, rdmDegTplist, rdmNodelist,
                        edgeRoadm1, edgeRoadm2));
                    connectionServMap.putAll(createXpdrCepsAndConnectionsPht(xpdrNetworkTplist, xpdrNodelist));
                    xpdrClientTplist = getAssociatedClientsPort(xpdrNetworkTplist);
                    LOG.info("Associated client ports = {}", xpdrClientTplist);
                    connectionServMap.putAll(createXpdrCepsAndConnectionsEth(xpdrClientTplist, xpdrNodelist,
                        connectionServMap));
                }
                break;
            case DSR:
                LOG.info("OTN XGE/ODUe service");
                // - XC connection between iODU and eODU
                // - Top connection between eODU ports
                // - Top connection between DSR ports
                if (openroadmNodeType.equals(OpenroadmNodeType.SWITCH)) {
                    // TODO: We create both ODU and DSR because there is no ODU service creation for the switch
                    // - XC Connection OTSi betwwen iODU and eODU of xpdr
                    // - Top connection in the ODU layer, between xpdr eODU ports (?)
                    connectionServMap.putAll(createXpdrCepsAndConnectionsOdu(xpdrNetworkTplist, xpdrNodelist));
                    connectionServMap.putAll(createXpdrCepsAndConnectionsDsr(xpdrClientTplist, xpdrNetworkTplist,
                        xpdrNodelist));
                }
                if (openroadmNodeType.equals(OpenroadmNodeType.MUXPDR)) {
                    // TODO: OTN service but mux has 3 steps at rendering. Verify that things exist
                    connectionServMap.putAll(createXpdrCepsAndConnectionsDsr(xpdrClientTplist, xpdrNetworkTplist,
                        xpdrNodelist));
                }
                break;
            default:
                LOG.error("Service type format {} not supported", serviceProtName.getName());
        }
        return connectionServMap;
    }

    /**
     * Process cancel resource result.
     * @param serviceName Service name to build uuid.
     */
    private void onCancelResourceResult(String serviceName) {
        if (servicePathRpcResult.getStatus() == RpcStatusEx.Failed) {
            LOG.info("PCE cancel resource failed !");
            return;
        } else if (servicePathRpcResult.getStatus() == RpcStatusEx.Pending) {
            LOG.warn("PCE cancel returned a Penging RpcStatusEx code!");
            return;
        } else if (servicePathRpcResult.getStatus() != RpcStatusEx.Successful) {
            LOG.error("PCE cancel returned an unknown RpcStatusEx code!");
            return;
        }
        LOG.info("PCE cancel resource done OK !");
        Uuid suuid = new Uuid(UUID.nameUUIDFromBytes(serviceName.getBytes(Charset.forName("UTF-8")))
                .toString());
        // get connections of connectivity service and remove them from tapi context and then remove
        //  service from context. The CEPs are maintained as they could be reused by another service
        ConnectivityService connService = getConnectivityService(suuid);
        if (connService == null) {
            LOG.error("Service doesnt exist in tapi context");
            return;
        }
        for (Connection connection:connService.getConnection().values()) {
            deleteConnection(connection.getConnectionUuid());
        }
        deleteConnectivityService(suuid);
    }

    @SuppressFBWarnings(
            value = "ES_COMPARING_STRINGS_WITH_EQ",
            justification = "false positives, not strings but real object references comparisons")
    private Boolean compareServicePathRpcResult(ServicePathRpcResult notification) {
        if (servicePathRpcResult == null) {
            return false;
        }
        if (servicePathRpcResult.getNotificationType() != notification.getNotificationType()) {
            return false;
        }
        if (servicePathRpcResult.getServiceName() != notification.getServiceName()) {
            return false;
        }
        if (servicePathRpcResult.getStatus() != notification.getStatus()) {
            return false;
        }
        if (servicePathRpcResult.getStatusMessage() != notification.getStatusMessage()) {
            return false;
        }
        return true;
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
        Map<org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev181210.cep.list.ConnectionEndPointKey,
            ConnectionEndPoint> cepMapDsr = new HashMap<>();
        // Create 1 cep per Xpdr in the CLIENT
        // 1 top connection DSR between the CLIENT xpdrs
        for (String xpdr:xpdrNodelist) {
            LOG.info("Creating ceps and xc for xpdr {}", xpdr);
            String spcXpdrClient = xpdrClientTplist.stream().filter(netp -> netp.contains(xpdr)).findFirst().get();

            ConnectionEndPoint netCep1 = createCepXpdr(spcXpdrClient, TapiStringConstants.DSR, TapiStringConstants.DSR,
                LayerProtocolName.DSR);
            putXpdrCepInTopologyContext(xpdr, spcXpdrClient, TapiStringConstants.DSR, TapiStringConstants.DSR, netCep1);

            cepMapDsr.put(netCep1.key(), netCep1);
        }
        String spcXpdr1 = xpdrClientTplist.stream().filter(adp -> adp.contains(xpdrNodelist
            .get(0))).findFirst().get();
        String spcXpdr2 = xpdrClientTplist.stream().filter(adp -> adp.contains(xpdrNodelist
            .get(xpdrNodelist.size() - 1))).findFirst().get();

        // DSR top connection between edge xpdr CLIENT DSR
        org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev181210.connectivity.context.Connection
            connectionDsr = createTopConnection(spcXpdr1, spcXpdr2, cepMapDsr, TapiStringConstants.DSR,
            LayerProtocolName.DSR, xcMap);
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
        Map<org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev181210.cep.list.ConnectionEndPointKey,
            ConnectionEndPoint> cepMapDsr = new HashMap<>();
        Map<org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev181210.cep.list.ConnectionEndPointKey,
            ConnectionEndPoint> cepMapOdu = new HashMap<>();
        // TODO: when upgrading the models to 2.1.3, get the connection inclusion because those connections will
        //  be added to the lower connection of a top connection
        Map<LowerConnectionKey, LowerConnection> xcMap = new HashMap<>();

        // Create 1 cep per Xpdr in the CLIENT, 1 cep per Xpdr eODU, 1 XC between eODU and iODE,
        // 1 top connection between eODU and a top connection DSR between the CLIENT xpdrs
        for (String xpdr:xpdrNodelist) {
            LOG.info("Creating ceps and xc for xpdr {}", xpdr);
            String spcXpdrClient = xpdrClientTplist.stream().filter(netp -> netp.contains(xpdr)).findFirst().get();

            ConnectionEndPoint netCep1 = createCepXpdr(spcXpdrClient, TapiStringConstants.DSR, TapiStringConstants.DSR,
                LayerProtocolName.DSR);
            putXpdrCepInTopologyContext(xpdr, spcXpdrClient, TapiStringConstants.DSR, TapiStringConstants.DSR, netCep1);

            ConnectionEndPoint netCep2 = createCepXpdr(spcXpdrClient, TapiStringConstants.E_ODU,
                TapiStringConstants.DSR, LayerProtocolName.ODU);
            putXpdrCepInTopologyContext(xpdr, spcXpdrClient, TapiStringConstants.E_ODU, TapiStringConstants.DSR,
                netCep2);

            String spcXpdrNetwork = getAssociatedNetworkPort(spcXpdrClient, xpdrNetworkTplist);
            ConnectionEndPoint netCep3 = getAssociatediODUCep(spcXpdrNetwork);

            cepMapDsr.put(netCep1.key(), netCep1);
            cepMapOdu.put(netCep2.key(), netCep2);
            // Create x connection between I_ODU and E_ODU within xpdr
            org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev181210.connectivity.context.Connection
                connection = createXCBetweenCeps(netCep2, netCep3, spcXpdrClient, spcXpdrNetwork,
                TapiStringConstants.ODU, LayerProtocolName.ODU);
            this.connectionFullMap.put(connection.key(), connection);

            // Create X connection that will be added to the service object
            LowerConnection conn = new LowerConnectionBuilder().setConnectionUuid(connection.getUuid()).build();
            xcMap.put(conn.key(), conn);
        }

        String spcXpdr1 = xpdrClientTplist.stream().filter(adp -> adp.contains(xpdrNodelist
            .get(0))).findFirst().get();
        String spcXpdr2 = xpdrClientTplist.stream().filter(adp -> adp.contains(xpdrNodelist
            .get(xpdrNodelist.size() - 1))).findFirst().get();

        // eODU top connection between edge xpdr CLIENT eODU
        org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev181210.connectivity.context.Connection
            connectionOdu = createTopConnection(spcXpdr1, spcXpdr2, cepMapOdu, TapiStringConstants.E_ODU,
            LayerProtocolName.ODU, xcMap);
        this.connectionFullMap.put(connectionOdu.key(), connectionOdu);

        // ODU top connection that will be added to the service object and also lower connection
        Connection conn = new ConnectionBuilder().setConnectionUuid(connectionOdu.getUuid()).build();
        connServMap.put(conn.key(), conn);
        LowerConnection lowerConn = new LowerConnectionBuilder().setConnectionUuid(connectionOdu.getUuid()).build();
        xcMap.put(lowerConn.key(), lowerConn);

        // DSR top connection between edge xpdr CLIENT DSR
        org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev181210.connectivity.context.Connection
            connectionDsr = createTopConnection(spcXpdr1, spcXpdr2, cepMapDsr, TapiStringConstants.DSR,
            LayerProtocolName.DSR, xcMap);
        this.connectionFullMap.put(connectionDsr.key(), connectionDsr);

        // DSR top connection that will be added to the service object
        Connection conn1 = new ConnectionBuilder().setConnectionUuid(connectionDsr.getUuid()).build();
        connServMap.put(conn1.key(), conn1);

        return connServMap;
    }

    private Map<ConnectionKey, Connection> createXpdrCepsAndConnectionsOdu(List<String> xpdrNetworkTplist,
                                                                           List<String> xpdrNodelist) {
        Map<ConnectionKey, Connection> connServMap = new HashMap<>();
        Map<org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev181210.cep.list.ConnectionEndPointKey,
            ConnectionEndPoint> cepMap = new HashMap<>();
        // TODO: when upgrading the models to 2.1.3, get the connection inclusion because those connections will
        //  be added to the lower connection of a top connection
        Map<LowerConnectionKey, LowerConnection> xcMap = new HashMap<>();
        // Create 1 cep per Xpdr in the I_ODU and a top
        // connection iODU between the xpdrs
        for (String xpdr:xpdrNodelist) {
            LOG.info("Creating ceps and xc for xpdr {}", xpdr);
            String spcXpdrNetwork = xpdrNetworkTplist.stream().filter(netp -> netp.contains(xpdr)).findFirst().get();

            ConnectionEndPoint netCep1 = createCepXpdr(spcXpdrNetwork, TapiStringConstants.I_ODU,
                TapiStringConstants.DSR, LayerProtocolName.ODU);
            putXpdrCepInTopologyContext(xpdr, spcXpdrNetwork, TapiStringConstants.I_ODU, TapiStringConstants.DSR,
                netCep1);

            cepMap.put(netCep1.key(), netCep1);
        }

        // ODU top connection between edge xpdr i_ODU
        String spcXpdr1 = xpdrNetworkTplist.stream().filter(adp -> adp.contains(xpdrNodelist
            .get(0))).findFirst().get();
        String spcXpdr2 = xpdrNetworkTplist.stream().filter(adp -> adp.contains(xpdrNodelist
            .get(xpdrNodelist.size() - 1))).findFirst().get();
        org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev181210.connectivity.context.Connection
            connection = createTopConnection(spcXpdr1, spcXpdr2, cepMap, TapiStringConstants.I_ODU,
            LayerProtocolName.ODU, xcMap);
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
        // TODO: when upgrading the models to 2.1.3, get the connection inclusion because those connections will
        //  be added to the lower connection of a top connection
        Map<LowerConnectionKey, LowerConnection> xcMap = new HashMap<>();

        // create ceps and x connections within xpdr
        for (String xpdr:xpdrNodelist) {
            LOG.info("Creating ceps and xc for xpdr {}", xpdr);
            String spcXpdrNetwork = xpdrNetworkTplist.stream().filter(netp -> netp.contains(xpdr)).findFirst().get();
            // There should be 1 network tp per xpdr
            // TODO photonic media model should be updated to have the corresponding CEPs. I will just create
            //  3 different MC CEPs giving different IDs to show that they are different
            // Create 3 CEPs for each xpdr otsi node and the corresponding cross connection matchin the NEPs
            ConnectionEndPoint netCep1 = createCepXpdr(spcXpdrNetwork, TapiStringConstants.PHTNC_MEDIA,
                TapiStringConstants.OTSI, LayerProtocolName.PHOTONICMEDIA);
            putXpdrCepInTopologyContext(xpdr, spcXpdrNetwork, TapiStringConstants.PHTNC_MEDIA, TapiStringConstants.OTSI,
                netCep1);
            ConnectionEndPoint netCep2 = createCepXpdr(spcXpdrNetwork, TapiStringConstants.E_OTSI,
                TapiStringConstants.OTSI, LayerProtocolName.PHOTONICMEDIA);
            putXpdrCepInTopologyContext(xpdr, spcXpdrNetwork, TapiStringConstants.E_OTSI, TapiStringConstants.OTSI,
                netCep2);
            ConnectionEndPoint netCep3 = createCepXpdr(spcXpdrNetwork, TapiStringConstants.I_OTSI,
                TapiStringConstants.OTSI, LayerProtocolName.PHOTONICMEDIA);
            putXpdrCepInTopologyContext(xpdr, spcXpdrNetwork, TapiStringConstants.I_OTSI, TapiStringConstants.OTSI,
                netCep3);
            cepMap.put(netCep1.key(), netCep1);
            cepMap.put(netCep2.key(), netCep2);
            cepMap.put(netCep3.key(), netCep3);

            // Create x connection between I_OTSi and E_OTSi within xpdr
            org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev181210.connectivity.context.Connection
                connection = createXCBetweenCeps(netCep2, netCep3, spcXpdrNetwork, spcXpdrNetwork,
                TapiStringConstants.OTSI, LayerProtocolName.PHOTONICMEDIA);
            this.connectionFullMap.put(connection.key(), connection);

            // Create X connection that will be added to the service object
            LowerConnection conn = new LowerConnectionBuilder().setConnectionUuid(connection.getUuid()).build();
            xcMap.put(conn.key(), conn);
        }
        // OTSi top connection between edge I_OTSI Xpdr
        String spcXpdr1 = xpdrNetworkTplist.stream().filter(adp -> adp.contains(xpdrNodelist
            .get(0))).findFirst().get();
        String spcXpdr2 = xpdrNetworkTplist.stream().filter(adp -> adp.contains(xpdrNodelist
            .get(xpdrNodelist.size() - 1))).findFirst().get();
        org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev181210.connectivity.context.Connection
            connection = createTopConnection(spcXpdr1, spcXpdr2, cepMap, TapiStringConstants.I_OTSI,
            LayerProtocolName.PHOTONICMEDIA, xcMap);
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
        Map<LowerConnectionKey, LowerConnection> xcMap = new HashMap<>();
        for (String roadm : rdmNodelist) {
            LOG.info("Creating ceps and xc for roadm {}", roadm);
            String spcRdmAD = rdmAddDropTplist.stream().filter(adp -> adp.contains(roadm)).findFirst().get();
            LOG.info("AD port of ROADm {} = {}", roadm, spcRdmAD);
            // There should be only 1 AD and 1 DEG per roadm
            // TODO photonic media model should be updated to have the corresponding CEPs. I will just create
            //  3 different MC CEPs giving different IDs to show that they are different
            // Create 3 CEPs for each AD and DEG and the corresponding cross connections, matching the NEPs
            // created in the topology creation
            // add CEPs to the topology to the corresponding ONEP
            ConnectionEndPoint adCep1 = createCepRoadm(spcRdmAD, TapiStringConstants.PHTNC_MEDIA);
            putRdmCepInTopologyContext(roadm, spcRdmAD, TapiStringConstants.PHTNC_MEDIA, adCep1);
            ConnectionEndPoint adCep2 = createCepRoadm(spcRdmAD, TapiStringConstants.MC);
            putRdmCepInTopologyContext(roadm, spcRdmAD, TapiStringConstants.MC, adCep2);
            ConnectionEndPoint adCep3 = createCepRoadm(spcRdmAD, TapiStringConstants.OTSI_MC);
            putRdmCepInTopologyContext(roadm, spcRdmAD, TapiStringConstants.OTSI_MC, adCep3);
            cepMap.put(adCep1.key(), adCep1);
            cepMap.put(adCep2.key(), adCep2);
            cepMap.put(adCep3.key(), adCep3);

            String spcRdmDEG = rdmDegTplist.stream().filter(adp -> adp.contains(roadm)).findFirst().get();
            LOG.info("Degree port of ROADm {} = {}", roadm, spcRdmDEG);

            ConnectionEndPoint degCep1 = createCepRoadm(spcRdmDEG, TapiStringConstants.PHTNC_MEDIA);
            putRdmCepInTopologyContext(roadm, spcRdmDEG, TapiStringConstants.PHTNC_MEDIA, degCep1);
            ConnectionEndPoint degCep2 = createCepRoadm(spcRdmDEG, TapiStringConstants.MC);
            putRdmCepInTopologyContext(roadm, spcRdmDEG, TapiStringConstants.MC, degCep2);
            ConnectionEndPoint degCep3 = createCepRoadm(spcRdmDEG, TapiStringConstants.OTSI_MC);
            putRdmCepInTopologyContext(roadm, spcRdmDEG, TapiStringConstants.OTSI_MC, degCep3);
            cepMap.put(degCep1.key(), degCep1);
            cepMap.put(degCep2.key(), degCep2);
            cepMap.put(degCep3.key(), degCep3);

            LOG.info("Going to create cross connections for ROADM {}", roadm);
            // Create X connections between MC and OTSi_MC for full map
            org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev181210.connectivity.context.Connection
                connection1 = createXCBetweenCeps(adCep2, degCep2, spcRdmAD, spcRdmDEG, TapiStringConstants.MC,
                LayerProtocolName.PHOTONICMEDIA);
            LOG.info("Cross connection 1 created = {}", connection1.toString());
            org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev181210.connectivity.context.Connection
                connection2 = createXCBetweenCeps(adCep3, degCep3, spcRdmAD, spcRdmDEG, TapiStringConstants.OTSI_MC,
                LayerProtocolName.PHOTONICMEDIA);
            LOG.info("Cross connection 2 created = {}", connection2.toString());
            this.connectionFullMap.put(connection1.key(), connection1);
            this.connectionFullMap.put(connection2.key(), connection2);

            // Create X connections that will be added to the service object
            LowerConnection conn1 = new LowerConnectionBuilder().setConnectionUuid(connection1.getUuid()).build();
            LowerConnection conn2 = new LowerConnectionBuilder().setConnectionUuid(connection2.getUuid()).build();

            xcMap.put(conn1.key(), conn1);
            xcMap.put(conn2.key(), conn2);
        }
        LOG.info("Going to create top connections between roadms");
        // create top connections between roadms: MC connections between AD MC CEPs of roadms
        for (int i = 0; i < rdmNodelist.size(); i++) {
            if (rdmNodelist.size() <= (i + 1)) {
                LOG.info("Reached last roadm. No more MC connections");
                break;
            }
            // Current roadm with roadm i + 1 --> MC
            String roadm1 = rdmNodelist.get(i);
            String spcRdmAD1 = rdmAddDropTplist.stream().filter(adp -> adp.contains(roadm1)).findFirst().get();
            String roadm2 = rdmNodelist.get(i + 1);
            String spcRdmAD2 = rdmAddDropTplist.stream().filter(adp -> adp.contains(roadm2)).findFirst().get();
            LOG.info("Creating top connection from {} to {} between tps: {}-{}", roadm1, roadm2, spcRdmAD1, spcRdmAD2);

            // Create top connections between MC for full map
            org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev181210.connectivity.context.Connection
                connection = createTopConnection(spcRdmAD1, spcRdmAD2, cepMap, TapiStringConstants.MC,
                LayerProtocolName.PHOTONICMEDIA, xcMap);
            this.connectionFullMap.put(connection.key(), connection);
            LOG.info("Top connection created = {}", connection.toString());

            // Create top connections that will be added to the service object and lower
            // connection to the next top connection
            Connection conn = new ConnectionBuilder().setConnectionUuid(connection.getUuid()).build();
            connServMap.put(conn.key(), conn);
            LowerConnection conn1 = new LowerConnectionBuilder().setConnectionUuid(connection.getUuid()).build();
            xcMap.put(conn1.key(), conn1);
        }

        // OTSiMC top connection between edge roadms
        LOG.info("Going to created top connection between OTSiMC");
        String spcRdmAD1 = rdmAddDropTplist.stream().filter(adp -> adp.contains(edgeRoadm1)).findFirst().get();
        String spcRdmAD2 = rdmAddDropTplist.stream().filter(adp -> adp.contains(edgeRoadm2)).findFirst().get();
        org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev181210.connectivity.context.Connection
            connection = createTopConnection(spcRdmAD1, spcRdmAD2, cepMap, TapiStringConstants.OTSI_MC,
            LayerProtocolName.PHOTONICMEDIA, xcMap);
        this.connectionFullMap.put(connection.key(), connection);
        LOG.info("Top connection created = {}", connection.toString());

        // OTSiMC top connections that will be added to the service object
        Connection conn = new ConnectionBuilder().setConnectionUuid(connection.getUuid()).build();
        connServMap.put(conn.key(), conn);
        return connServMap;
    }

    private org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev181210.connectivity.context.Connection
            createTopConnection(String tp1, String tp2,
                        Map<org.opendaylight.yang.gen.v1.urn
                                .onf.otcc.yang.tapi.connectivity.rev181210.cep.list.ConnectionEndPointKey,
                                ConnectionEndPoint> cepMap, String qual, LayerProtocolName topPortocol,
                                Map<LowerConnectionKey, LowerConnection> xcMap) {
        // find cep for each AD MC of roadm 1 and 2
        LOG.info("Top connection name = {}", String.join("+", "TOP", tp1, tp2, qual));
        org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev181210.ConnectionEndPoint adCep1 =
            cepMap.get(new org.opendaylight.yang.gen.v1.urn
                .onf.otcc.yang.tapi.connectivity.rev181210.cep.list.ConnectionEndPointKey(
                new Uuid(UUID.nameUUIDFromBytes((String.join("+", "CEP", tp1.split("\\+")[0],
                    qual, tp1.split("\\+")[1])).getBytes(Charset.forName("UTF-8")))
                    .toString())));
        LOG.info("ADCEP1 = {}", adCep1.toString());
        org.opendaylight.yang.gen.v1.urn
            .onf.otcc.yang.tapi.connectivity.rev181210.connection.ConnectionEndPoint cep1 =
            new org.opendaylight.yang.gen.v1.urn
                .onf.otcc.yang.tapi.connectivity.rev181210.connection.ConnectionEndPointBuilder()
                .setNodeEdgePointUuid(adCep1.getClientNodeEdgePoint()
                    .values().stream().findFirst().get().getNodeEdgePointUuid())
                .setTopologyUuid(adCep1.getClientNodeEdgePoint()
                    .values().stream().findFirst().get().getTopologyUuid())
                .setNodeUuid(adCep1.getClientNodeEdgePoint()
                    .values().stream().findFirst().get().getNodeUuid())
                .setConnectionEndPointUuid(adCep1.getUuid())
                .build();
        org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev181210.ConnectionEndPoint adCep2 =
            cepMap.get(new org.opendaylight.yang.gen.v1.urn
                .onf.otcc.yang.tapi.connectivity.rev181210.cep.list.ConnectionEndPointKey(
                new Uuid(UUID.nameUUIDFromBytes((String.join("+", "CEP", tp2.split("\\+")[0],
                    qual, tp2.split("\\+")[1])).getBytes(Charset.forName("UTF-8")))
                    .toString())));
        LOG.info("ADCEP2 = {}", adCep2.toString());
        org.opendaylight.yang.gen.v1.urn
            .onf.otcc.yang.tapi.connectivity.rev181210.connection.ConnectionEndPoint cep2 =
            new org.opendaylight.yang.gen.v1.urn
                .onf.otcc.yang.tapi.connectivity.rev181210.connection.ConnectionEndPointBuilder()
                .setNodeEdgePointUuid(adCep2.getClientNodeEdgePoint()
                    .values().stream().findFirst().get().getNodeEdgePointUuid())
                .setTopologyUuid(adCep2.getClientNodeEdgePoint()
                    .values().stream().findFirst().get().getTopologyUuid())
                .setNodeUuid(adCep2.getClientNodeEdgePoint()
                    .values().stream().findFirst().get().getNodeUuid())
                .setConnectionEndPointUuid(adCep1.getUuid())
                .build();
        Map<ConnectionEndPointKey, org.opendaylight.yang.gen.v1.urn
            .onf.otcc.yang.tapi.connectivity.rev181210.connection.ConnectionEndPoint> ceps = new HashMap<>();
        ceps.put(cep1.key(), cep1);
        ceps.put(cep2.key(), cep2);
        Name connName = new NameBuilder()
            .setValueName("Connection name")
            .setValue(String.join("+", "TOP", tp1, tp2, qual))
            .build();
        // TODO: lower connection, supported link.......
        return new org.opendaylight.yang.gen.v1.urn
            .onf.otcc.yang.tapi.connectivity.rev181210.connectivity.context.ConnectionBuilder()
            .setUuid(new Uuid(UUID.nameUUIDFromBytes((String.join("+", "TOP", tp1, tp2, qual))
                .getBytes(Charset.forName("UTF-8"))).toString()))
            .setName(Map.of(connName.key(), connName))
            .setConnectionEndPoint(ceps)
            .setOperationalState(OperationalState.DISABLED)
            .setLayerProtocolName(topPortocol)
            .setLifecycleState(LifecycleState.POTENTIALAVAILABLE)
            .setDirection(ForwardingDirection.BIDIRECTIONAL)
            .setLowerConnection(xcMap)
            .build();
    }

    private org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev181210.connectivity.context.Connection
            createXCBetweenCeps(ConnectionEndPoint cep1, ConnectionEndPoint cep2, String tp1, String tp2, String qual,
                        LayerProtocolName xcProtocol) {
        LOG.info("Creation cross connection between: {} and {}", tp1, tp2);
        LOG.info("Cross connection name = {}", String.join("+", "XC", tp1, tp2, qual));
        LOG.info("CEP1 = {}", cep1.getClientNodeEdgePoint().toString());
        LOG.info("CEP2 = {}", cep2.getClientNodeEdgePoint().toString());
        org.opendaylight.yang.gen.v1.urn
            .onf.otcc.yang.tapi.connectivity.rev181210.connection.ConnectionEndPoint cepServ1 =
            new org.opendaylight.yang.gen.v1.urn
                .onf.otcc.yang.tapi.connectivity.rev181210.connection.ConnectionEndPointBuilder()
                .setNodeEdgePointUuid(cep1.getClientNodeEdgePoint()
                    .values().stream().findFirst().get().getNodeEdgePointUuid())
                .setTopologyUuid(cep1.getClientNodeEdgePoint()
                    .values().stream().findFirst().get().getTopologyUuid())
                .setNodeUuid(cep1.getClientNodeEdgePoint()
                    .values().stream().findFirst().get().getNodeUuid())
                .setConnectionEndPointUuid(cep1.getUuid())
                .build();
        org.opendaylight.yang.gen.v1.urn
            .onf.otcc.yang.tapi.connectivity.rev181210.connection.ConnectionEndPoint cepServ2 =
            new org.opendaylight.yang.gen.v1.urn
                .onf.otcc.yang.tapi.connectivity.rev181210.connection.ConnectionEndPointBuilder()
                .setNodeEdgePointUuid(cep2.getClientNodeEdgePoint()
                    .values().stream().findFirst().get().getNodeEdgePointUuid())
                .setTopologyUuid(cep2.getClientNodeEdgePoint()
                    .values().stream().findFirst().get().getTopologyUuid())
                .setNodeUuid(cep2.getClientNodeEdgePoint()
                    .values().stream().findFirst().get().getNodeUuid())
                .setConnectionEndPointUuid(cep2.getUuid())
                .build();
        Map<ConnectionEndPointKey, org.opendaylight.yang.gen.v1.urn
            .onf.otcc.yang.tapi.connectivity.rev181210.connection.ConnectionEndPoint> ceps = new HashMap<>();
        ceps.put(cepServ1.key(), cepServ1);
        ceps.put(cepServ2.key(), cepServ2);
        Name connName = new NameBuilder()
            .setValueName("Connection name")
            .setValue(String.join("+", "XC", tp1, tp2, qual))
            .build();
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
        LOG.info("NEP = {}", String.join("+", id.split("\\+")[0], qualifier, id.split("\\+")[1]));
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
            .setTopologyUuid(new Uuid(UUID.nameUUIDFromBytes(TapiStringConstants.T0_FULL_MULTILAYER
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
            .setTopologyUuid(new Uuid(UUID.nameUUIDFromBytes(TapiStringConstants.T0_FULL_MULTILAYER
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

    private void putRdmCepInTopologyContext(String node, String spcRdmAD, String qual, ConnectionEndPoint cep) {
        LOG.info("NEP id before Merge = {}", String.join("+", node, qual, spcRdmAD.split("\\+")[1]));
        LOG.info("Node of NEP id before Merge = {}", String.join("+", node, TapiStringConstants.PHTNC_MEDIA));
        // Give uuids so that it is easier to look for things: topology uuid, node uuid, nep uuid, cep
        Uuid topoUuid = new Uuid(UUID.nameUUIDFromBytes(TapiStringConstants.T0_FULL_MULTILAYER
            .getBytes(Charset.forName("UTF-8"))).toString());
        Uuid nodeUuid = new Uuid(UUID.nameUUIDFromBytes(String.join("+", node, TapiStringConstants.PHTNC_MEDIA)
            .getBytes(Charset.forName("UTF-8"))).toString());
        Uuid nepUuid = new Uuid(UUID.nameUUIDFromBytes(String.join("+", node, qual, spcRdmAD.split("\\+")[1])
            .getBytes(Charset.forName("UTF-8"))).toString());
        updateTopologyWithCep(topoUuid, nodeUuid, nepUuid, cep);
    }

    private void putXpdrCepInTopologyContext(String node, String spcXpdrNet, String qual, String nodeLayer,
                                             ConnectionEndPoint cep) {
        // Give uuids so that it is easier to look for things: topology uuid, node uuid, nep uuid, cep
        Uuid topoUuid = new Uuid(UUID.nameUUIDFromBytes(TapiStringConstants.T0_FULL_MULTILAYER
            .getBytes(Charset.forName("UTF-8"))).toString());
        Uuid nodeUuid = new Uuid(UUID.nameUUIDFromBytes(String.join("+", node, nodeLayer)
            .getBytes(Charset.forName("UTF-8"))).toString());
        Uuid nepUuid = new Uuid(UUID.nameUUIDFromBytes(String.join("+", node, qual, spcXpdrNet.split("\\+")[1])
            .getBytes(Charset.forName("UTF-8"))).toString());
        updateTopologyWithCep(topoUuid, nodeUuid, nepUuid, cep);
    }

    public void updateTopologyWithCep(Uuid topoUuid, Uuid nodeUuid, Uuid nepUuid, ConnectionEndPoint cep) {
        // TODO: verify this is correct. Should we identify the context IID with the context UUID??
        InstanceIdentifier<OwnedNodeEdgePoint> onepIID = InstanceIdentifier.builder(Context.class)
            .augmentation(org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.Context1.class)
            .child(org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.context.TopologyContext.class)
            .child(Topology.class, new TopologyKey(topoUuid))
            .child(org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.topology.Node.class,
                new NodeKey(nodeUuid))
            .child(OwnedNodeEdgePoint.class, new OwnedNodeEdgePointKey(nepUuid))
            .build();
        try {
            Optional<OwnedNodeEdgePoint> optionalOnep = this.networkTransactionService.read(
                LogicalDatastoreType.OPERATIONAL, onepIID).get();
            if (!optionalOnep.isPresent()) {
                LOG.error("ONEP is not present in datastore");
                return;
            }
            OwnedNodeEdgePoint onep = optionalOnep.get();
            LOG.info("ONEP found = {}", onep.toString());
            // TODO -> If cep exists -> skip merging to datasore
            OwnedNodeEdgePoint1 onep1 = onep.augmentation(OwnedNodeEdgePoint1.class);
            if (onep1 != null && onep1.getCepList() != null && onep1.getCepList().getConnectionEndPoint() != null) {
                if (onep1.getCepList().getConnectionEndPoint().containsKey(
                    new org.opendaylight.yang.gen.v1
                        .urn.onf.otcc.yang.tapi.connectivity.rev181210.cep.list.ConnectionEndPointKey(cep.key()))) {
                    LOG.info("CEP already in topology, skipping merge");
                    return;
                }
            }
            // Updated ONEP
            CepList cepList = new CepListBuilder().setConnectionEndPoint(Map.of(cep.key(), cep)).build();
            OwnedNodeEdgePoint1 onep1Bldr = new OwnedNodeEdgePoint1Builder().setCepList(cepList).build();
            OwnedNodeEdgePoint newOnep = new OwnedNodeEdgePointBuilder(onep)
                .addAugmentation(onep1Bldr)
                .build();
            LOG.info("New ONEP is {}", newOnep.toString());
            // merge in datastore
            this.networkTransactionService.merge(LogicalDatastoreType.OPERATIONAL, onepIID,
                newOnep);
            this.networkTransactionService.commit().get();
            LOG.info("CEP added successfully.");
        } catch (InterruptedException | ExecutionException e) {
            LOG.error("Couldnt update cep in topology", e);
        }
    }

    private void updateConnectionContextWithConn(
            Map<org.opendaylight.yang.gen.v1.urn
                .onf.otcc.yang.tapi.connectivity.rev181210.connectivity.context.ConnectionKey,
                org.opendaylight.yang.gen.v1.urn
                    .onf.otcc.yang.tapi.connectivity.rev181210.connectivity.context.Connection> connFullMap,
            Map<ConnectionKey, Connection> connMap, Uuid suuid) {
        // TODO: verify this is correct. Should we identify the context IID with the context UUID??
        try {
            ConnectivityService connServ = getConnectivityService(suuid);
            ConnectivityService updtConnServ = new ConnectivityServiceBuilder(connServ)
                    .setConnection(connMap)
                    .build();

            // Perform the merge operation with the new conn service and the connection context updated
            org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev181210.context.ConnectivityContext
                    connectivityContext = new ConnectivityContextBuilder()
                    .setConnectivityService(Map.of(updtConnServ.key(), updtConnServ))
                    .setConnection(connFullMap)
                    .build();
            InstanceIdentifier<org.opendaylight.yang.gen.v1.urn
                    .onf.otcc.yang.tapi.connectivity.rev181210.context.ConnectivityContext> connectivitycontextIID =
                    InstanceIdentifier.builder(Context.class).augmentation(Context1.class)
                            .child(org.opendaylight.yang.gen.v1.urn
                                    .onf.otcc.yang.tapi.connectivity.rev181210.context.ConnectivityContext.class)
                            .build();
            // merge in datastore
            this.networkTransactionService.merge(LogicalDatastoreType.OPERATIONAL, connectivitycontextIID,
                    connectivityContext);
            this.networkTransactionService.commit().get();
            LOG.info("TAPI connectivity merged successfully.");
        } catch (InterruptedException | ExecutionException e) {
            LOG.error("Failed to merge TAPI connectivity", e);
        }
    }

    private ConnectivityService getConnectivityService(Uuid suuid) {
        try {
        // First read connectivity service with service uuid and update info
            InstanceIdentifier<ConnectivityService> connectivityServIID =
                InstanceIdentifier.builder(Context.class).augmentation(Context1.class)
                        .child(org.opendaylight.yang.gen.v1.urn
                                .onf.otcc.yang.tapi.connectivity.rev181210.context.ConnectivityContext.class)
                        .child(ConnectivityService.class, new ConnectivityServiceKey(suuid))
                        .build();

            Optional<ConnectivityService> optConnServ =
                this.networkTransactionService.read(LogicalDatastoreType.OPERATIONAL, connectivityServIID).get();
            if (!optConnServ.isPresent()) {
                LOG.error("Connectivity service not found in tapi context");
                return null;
            }
            return optConnServ.get();
        } catch (InterruptedException | ExecutionException e) {
            LOG.error("Connectivity service not found in tapi context. Error:", e);
            return null;
        }
    }

    private void deleteConnectivityService(Uuid suuid) {
        // First read connectivity service with service uuid and update info
        InstanceIdentifier<ConnectivityService> connectivityServIID =
                InstanceIdentifier.builder(Context.class).augmentation(Context1.class)
                        .child(org.opendaylight.yang.gen.v1.urn
                                .onf.otcc.yang.tapi.connectivity.rev181210.context.ConnectivityContext.class)
                        .child(ConnectivityService.class, new ConnectivityServiceKey(suuid))
                        .build();
        try {
            this.networkTransactionService.delete(LogicalDatastoreType.OPERATIONAL, connectivityServIID);
            this.networkTransactionService.commit().get();
        } catch (InterruptedException | ExecutionException e) {
            LOG.error("Failed to delete TAPI connectivity service", e);
        }
    }

    private void deleteConnection(Uuid connectionUuid) {
        // First read connectivity service with service uuid and update info
        InstanceIdentifier<org.opendaylight.yang.gen.v1
                .urn.onf.otcc.yang.tapi.connectivity.rev181210.connectivity.context.Connection> connectionIID =
                InstanceIdentifier.builder(Context.class).augmentation(Context1.class)
                        .child(org.opendaylight.yang.gen.v1.urn
                                .onf.otcc.yang.tapi.connectivity.rev181210.context.ConnectivityContext.class)
                        .child(org.opendaylight.yang.gen.v1.urn
                                .onf.otcc.yang.tapi.connectivity.rev181210.connectivity.context.Connection.class,
                                new org.opendaylight.yang.gen.v1.urn
                                        .onf.otcc.yang.tapi.connectivity.rev181210.connectivity.context.ConnectionKey(
                                                connectionUuid))
                        .build();
        try {
            this.networkTransactionService.delete(LogicalDatastoreType.OPERATIONAL, connectionIID);
            this.networkTransactionService.commit().get();
        } catch (InterruptedException | ExecutionException e) {
            LOG.error("Failed to delete TAPI connection", e);
        }
    }

    private String getIdBasedOnModelVersion(String nodeid) {
        return nodeid.matches("[A-Z]{5}-[A-Z0-9]{2}-.*")
            ? String.join("-", nodeid.split("-")[0], nodeid.split("-")[1]) : nodeid.split("-")[0];
    }

    private ConnectionEndPoint getAssociatediODUCep(String spcXpdrNetwork) {
        Uuid topoUuid = new Uuid(UUID.nameUUIDFromBytes(TapiStringConstants.T0_FULL_MULTILAYER
            .getBytes(Charset.forName("UTF-8"))).toString());
        Uuid nodeUuid = new Uuid(UUID.nameUUIDFromBytes((String.join("+", spcXpdrNetwork.split("\\+")[0],
            TapiStringConstants.DSR).getBytes(Charset.forName("UTF-8")))).toString());
        Uuid nepUuid = new Uuid(UUID.nameUUIDFromBytes((String.join("+", spcXpdrNetwork.split("\\+")[0],
                TapiStringConstants.I_ODU, spcXpdrNetwork.split("\\+")[1]).getBytes(Charset.forName("UTF-8"))))
            .toString());
        Uuid cepUuid = new Uuid(UUID.nameUUIDFromBytes((String.join("+", "CEP",
            spcXpdrNetwork.split("\\+")[0], TapiStringConstants.I_ODU, spcXpdrNetwork.split("\\+")[1]))
            .getBytes(Charset.forName("UTF-8"))).toString());
        InstanceIdentifier<OwnedNodeEdgePoint> nepIID = InstanceIdentifier.builder(Context.class)
            .augmentation(org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.Context1.class)
            .child(org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.context.TopologyContext.class)
            .child(Topology.class, new TopologyKey(topoUuid))
            .child(org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.topology.Node.class,
                new NodeKey(nodeUuid)).child(OwnedNodeEdgePoint.class, new OwnedNodeEdgePointKey(nepUuid)).build();
        try {
            Optional<OwnedNodeEdgePoint> optNode = this.networkTransactionService
                .read(LogicalDatastoreType.OPERATIONAL, nepIID).get();
            if (!optNode.isPresent()) {
                LOG.error("Node is not present in datastore");
                return null;
            }
            if (optNode.get().augmentation(OwnedNodeEdgePoint1.class) == null) {
                LOG.error("Node doesnt have ceps");
                return null;
            }
            return optNode.get().augmentation(OwnedNodeEdgePoint1.class).getCepList().getConnectionEndPoint()
                .get(new org.opendaylight.yang.gen.v1.urn
                    .onf.otcc.yang.tapi.connectivity.rev181210.cep.list.ConnectionEndPointKey(cepUuid));
        } catch (InterruptedException | ExecutionException e) {
            LOG.error("Couldnt read node in topology", e);
            return null;
        }
    }

    private String getAssociatedNetworkPort(String spcXpdrClient, List<String> xpdrNetworkTplist) {
        for (String networkPort:xpdrNetworkTplist) {
            if (networkPort.split("\\+")[0].equals(spcXpdrClient.split("\\+")[0])) {
                return networkPort;
            }
        }
        return null;
    }

    private OpenroadmNodeType getOpenRoadmNodeType(List<String> xpdrNodelist) {
        List<OpenroadmNodeType> openroadmNodeTypeList = new ArrayList<>();
        for (String xpdrNode:xpdrNodelist) {
            Uuid nodeUuid = new Uuid(UUID.nameUUIDFromBytes((String.join("+",xpdrNode, TapiStringConstants.DSR))
                .getBytes(Charset.forName("UTF-8"))).toString());
            InstanceIdentifier<org.opendaylight.yang.gen.v1.urn
                .onf.otcc.yang.tapi.topology.rev181210.topology.Node> nodeIID = InstanceIdentifier.builder(
                    Context.class).augmentation(org.opendaylight.yang.gen.v1.urn
                    .onf.otcc.yang.tapi.topology.rev181210.Context1.class).child(TopologyContext.class)
                .child(Topology.class, new TopologyKey(this.tapiTopoUuid))
                .child(org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.topology.Node.class,
                    new NodeKey(nodeUuid)).build();
            try {
                Optional<org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.topology.Node> optNode
                    = this.networkTransactionService.read(LogicalDatastoreType.OPERATIONAL, nodeIID).get();
                if (!optNode.isPresent()) {
                    return null;
                }
                OpenroadmNodeType openroadmNodeType = OpenroadmNodeType.forName(optNode.get().getName().get(
                    new NameKey("Node Type")).getValue()).get();
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
                Mapping mapping = optMapping.get();
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

    public void setInput(CreateConnectivityServiceInput input) {
        this.input = input;
    }

    public void setServiceUuid(Uuid serviceUuid) {
        this.serviceUuid = serviceUuid;
    }
}
