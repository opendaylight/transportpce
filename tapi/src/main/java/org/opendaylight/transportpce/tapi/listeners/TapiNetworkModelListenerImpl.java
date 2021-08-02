/*
 * Copyright © 2021 Nokia, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.tapi.listeners;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.transportpce.common.network.NetworkTransactionService;
import org.opendaylight.transportpce.tapi.topology.TopologyUtils;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.state.types.rev191129.State;
import org.opendaylight.yang.gen.v1.nbi.notifications.rev201130.PublishTapiNotificationService;
import org.opendaylight.yang.gen.v1.nbi.notifications.rev201130.PublishTapiNotificationServiceBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev181210.AdministrativeState;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev181210.Context;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev181210.LayerProtocolName;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev181210.OperationalState;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev181210.Uuid;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev181210.ConnectionEndPoint;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev181210.OwnedNodeEdgePoint1;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev181210.connectivity.context.Connection;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev181210.connectivity.context.ConnectionBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev181210.connectivity.context.ConnectionKey;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev181210.connectivity.context.ConnectivityService;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev181210.connectivity.context.ConnectivityServiceBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev181210.connectivity.context.ConnectivityServiceKey;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev181210.context.ConnectivityContext;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.notification.rev181210.Notification;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.notification.rev181210.NotificationType;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.notification.rev181210.ObjectType;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.notification.rev181210.TapiNotificationListener;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.notification.rev181210.notification.ChangedAttributesKey;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.Context1;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.NodeEdgePointRef;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.context.TopologyContext;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.node.OwnedNodeEdgePoint;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.node.OwnedNodeEdgePointKey;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.topology.Node;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.topology.NodeKey;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.topology.context.Topology;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.topology.context.TopologyKey;
import org.opendaylight.yangtools.yang.binding.Enumeration;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TapiNetworkModelListenerImpl implements TapiNotificationListener {

    private static final Logger LOG = LoggerFactory.getLogger(TapiNetworkModelListenerImpl.class);
    private final NetworkTransactionService networkTransactionService;
    private final List<ConnectivityService> connectivityServiceChanges = new ArrayList<>();
    private final Uuid tapiTopoUuid = new Uuid(UUID.nameUUIDFromBytes(TopologyUtils.T0_FULL_MULTILAYER
            .getBytes(Charset.forName("UTF-8"))).toString());
    private final NotificationPublishService notificationPublishService;


    public TapiNetworkModelListenerImpl(NetworkTransactionService networkTransactionService) {
        this.networkTransactionService = networkTransactionService;
    }

    @Override
    public void onNotification(Notification notification) {
        if (notification.getNotificationType() == NotificationType.ATTRIBUTEVALUECHANGE
                && notification.getTargetObjectType() == ObjectType.TOPOLOGY) {
            if (notification.getChangedAttributes() == null) {
                return;
            }
            updateConnections(notification.getChangedAttributes().keySet().stream()
                    .map(changedAttributesKey -> new Uuid(changedAttributesKey.getValueName()))
                    .collect(Collectors.toList()),
                    notification.getChangedAttributes().get(new ChangedAttributesKey("operational"))
                            .getNewValue());
            updateConnectivityServices();
            // todo  create NotificationPublishSerivce (Tapi) object + send Notification
        }
    }

    private void updateConnectivityServices() {
        try {
            this.connectivityServiceChanges.clear();
            InstanceIdentifier<ConnectivityContext> connectivityContextIID =
                    InstanceIdentifier.builder(Context.class).augmentation(
                            org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev181210.Context1.class)
                            .child(ConnectivityContext.class)
                            .build();
            Optional<ConnectivityContext> optConnContext =
                    this.networkTransactionService.read(LogicalDatastoreType.OPERATIONAL, connectivityContextIID)
                            .get();
            if (optConnContext.isEmpty()) {
                LOG.error("Could not update TAPI connectifity services");
                return;
            }
            ConnectivityContext connContext = optConnContext.get();
            Map<Uuid, Enumeration[]> states = new HashMap<>();
            if (connContext.getConnectivityService() == null) {
                return;
            }
            for (ConnectivityService connService : connContext.getConnectivityService()
                    .values()) {
                states.put(connService.getUuid(), getStates(connService));
                AdministrativeState adminState = (AdministrativeState) states.get(connService.getUuid())[0];
                OperationalState operState = (OperationalState) states.get(connService.getUuid())[1];

                InstanceIdentifier<ConnectivityService> connServIID = InstanceIdentifier
                        .builder(Context.class).augmentation(org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi
                                .connectivity.rev181210.Context1.class)
                        .child(ConnectivityContext.class)
                        .child(ConnectivityService.class, new ConnectivityServiceKey(connService.getUuid()))
                        .build();
                ConnectivityService changedConnServ = new ConnectivityServiceBuilder()
                        .setUuid(connService.getUuid())
                        .setAdministrativeState(adminState)
                        .setOperationalState(operState)
                        .build();
                this.networkTransactionService.merge(LogicalDatastoreType.OPERATIONAL, connServIID,
                        changedConnServ);
                this.networkTransactionService.commit().get();

                if (connService.getAdministrativeState() != adminState
                        || connService.getOperationalState() != operState) {
                    this.connectivityServiceChanges.add(changedConnServ);
                }
            }
            for (ConnectivityService connService : connContext.getConnectivityService().values()) {
                AdministrativeState adminState = (AdministrativeState) states.get(connService.getUuid())[0];
                OperationalState operState = (OperationalState) states.get(connService.getUuid())[1];
                this.connectivityServiceChanges.addAll(updateSupportedConnectivityServices(connContext
                                .getConnectivityService().values(), connService.getUuid(), adminState, operState,
                        LayerProtocolName.ODU));
            }
        } catch (InterruptedException | ExecutionException e) {
            LOG.error("Could not update TAPI connectivity services");
        }
    }

    private Enumeration[] getStates(ConnectivityService connService) throws InterruptedException, ExecutionException {
        OperationalState operState = OperationalState.ENABLED;
        AdministrativeState adminState = AdministrativeState.UNLOCKED;
        for (org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev181210
                .connectivity.service.Connection connection : Objects.requireNonNull(connService.getConnection())
                .values()) {
            InstanceIdentifier<Connection> connIID =
                    InstanceIdentifier.builder(Context.class).augmentation(org.opendaylight.yang.gen.v1.urn
                            .onf.otcc.yang.tapi.connectivity.rev181210.Context1.class)
                            .child(ConnectivityContext.class)
                            .child(Connection.class, new ConnectionKey(connection.getConnectionUuid()))
                            .build();
            Optional<Connection> optConn = this.networkTransactionService.read(LogicalDatastoreType.OPERATIONAL,
                    connIID).get();
            if (optConn.isEmpty()) {
                LOG.error("Could not get state for a TAPI connection");
                continue;
            }
            if (optConn.get().getOperationalState() == OperationalState.DISABLED) {
                adminState = AdministrativeState.LOCKED;
                operState = OperationalState.DISABLED;
            }
        }
        Enumeration[] states;
        states = new Enumeration[]{adminState, operState};
        return states;
    }

    private void updateConnections(List<Uuid> changedOneps, String operState) {
        LOG.info("shuishan updating connections");
        try {
            //should it return a list of connections?
            InstanceIdentifier<org.opendaylight.yang.gen.v1.urn
                    .onf.otcc.yang.tapi.connectivity.rev181210.context.ConnectivityContext> connectivityContextIID =
                    InstanceIdentifier.builder(Context.class).augmentation(
                            org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev181210.Context1.class)
                            .child(org.opendaylight.yang.gen.v1.urn
                                    .onf.otcc.yang.tapi.connectivity.rev181210.context.ConnectivityContext.class)
                            .build();
            Optional<org.opendaylight.yang.gen.v1.urn
                    .onf.otcc.yang.tapi.connectivity.rev181210.context.ConnectivityContext> optConnContext =
                    this.networkTransactionService.read(LogicalDatastoreType.OPERATIONAL, connectivityContextIID)
                            .get();
            if (optConnContext.isEmpty()) {
                LOG.error("Could not update TAPI connections");
                return;
            }
            if (optConnContext.get().getConnectivityService() == null) {
                LOG.info("No TAPI connectivity service to update");
                return;
            }
            for (ConnectivityService connService : optConnContext.get().getConnectivityService().values()) {
                if (connService.getConnection() == null) {
                    continue;
                }
                for (org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev181210
                        .connectivity.service.Connection connection : connService.getConnection().values()) {
                    LOG.info("shuishan running connection loop");
                    InstanceIdentifier<Connection> connIID =
                            InstanceIdentifier.builder(Context.class).augmentation(org.opendaylight.yang.gen.v1.urn
                                    .onf.otcc.yang.tapi.connectivity.rev181210.Context1.class)
                                    .child(org.opendaylight.yang.gen.v1.urn
                                            .onf.otcc.yang.tapi.connectivity.rev181210.context
                                            .ConnectivityContext.class)
                                    .child(Connection.class, new ConnectionKey(connection.getConnectionUuid()))
                                    .build();
                    Optional<Connection> optConn =
                            this.networkTransactionService.read(LogicalDatastoreType.OPERATIONAL, connIID).get();
                    if (optConn.isEmpty()) {
                        LOG.error("Could not read connection data");
                        continue;
                    }
                    Connection newConn = optConn.get();
                    OperationalState operationalState = getConnectionState(changedOneps, operState, newConn);
                    Connection changedConn = new ConnectionBuilder(newConn)
                            .setOperationalState(operationalState).build();
                    this.networkTransactionService
                            .merge(LogicalDatastoreType.OPERATIONAL, connIID, changedConn);
                    this.networkTransactionService.commit().get();
                }

            }
        } catch (InterruptedException | ExecutionException e) {
            LOG.error("Could not update connections");
        }
    }

    private OperationalState getConnectionState(List<Uuid> changedOneps, String operState,
                                                Connection conn)
            throws InterruptedException, ExecutionException {
        LOG.info("shuishan getting ConnectionState");
        List<Uuid> connectionNeps = Objects.requireNonNull(conn.getConnectionEndPoint()).values().stream()
                .map(NodeEdgePointRef::getNodeEdgePointUuid).collect(Collectors.toList());
        if (!Collections.disjoint(changedOneps, connectionNeps)) {
            if (transformOperState(operState) == OperationalState.DISABLED) {
                return OperationalState.DISABLED;
            } else {
                for (Uuid connectionNep : connectionNeps) {
                    Optional<org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev181210.connection
                            .ConnectionEndPoint> ocep = conn.getConnectionEndPoint().values().stream()
                            .filter(connectionEndPoint -> connectionEndPoint.getNodeEdgePointUuid()
                                    == connectionNep).findFirst();
                    if (ocep.isEmpty()) {
                        continue;
                    }
                    InstanceIdentifier<OwnedNodeEdgePoint> onepIID = InstanceIdentifier.builder(Context.class)
                            .augmentation(Context1.class).child(TopologyContext.class)
                            .child(Topology.class, new TopologyKey(tapiTopoUuid))
                            .child(Node.class, new NodeKey(ocep.get().getNodeUuid()))
                            .child(OwnedNodeEdgePoint.class, new OwnedNodeEdgePointKey(connectionNep))
                            .build();
                    Optional<OwnedNodeEdgePoint> onep =
                            this.networkTransactionService.read(LogicalDatastoreType.OPERATIONAL, onepIID)
                                    .get();
                    if (onep.isEmpty() || onep.get().augmentation(OwnedNodeEdgePoint1.class) == null
                            || onep.get().augmentation(OwnedNodeEdgePoint1.class).getCepList() == null) {
                        continue;
                    }
                    for (ConnectionEndPoint cep : onep.get().augmentation(OwnedNodeEdgePoint1.class)
                            .getCepList().getConnectionEndPoint().values()) {
                        LOG.info("shuishan cep: {}-", cep);
                    }
                    if (onep.get().getOperationalState() == OperationalState.DISABLED
                            && !changedOneps.contains(onep.get().getUuid())) {
                        return OperationalState.DISABLED;
                    }
                }
                return OperationalState.ENABLED;
            }
        }
        return conn.getOperationalState();
    }

    private List<ConnectivityService> updateSupportedConnectivityServices(Collection<ConnectivityService> connServices,
                                                                          Uuid supportingConnService,
                                                                          AdministrativeState adminState,
                                                                          OperationalState operState,
                                                                          LayerProtocolName layer) {
        List<ConnectivityService> changedServices = new ArrayList<>();
        if (adminState != AdministrativeState.LOCKED && operState != OperationalState.DISABLED) {
            return changedServices;
        }
        try {
            for (ConnectivityService supportedConnService : connServices) {
                // TODO currently supporting service uuid is saved in service layer, replace with name as soon
                // as name is implemented
                if (supportedConnService.getServiceLayer() != layer) {
                    continue;
                }
                InstanceIdentifier<ConnectivityService> supportedConnServIID = InstanceIdentifier
                        .builder(Context.class).augmentation(org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi
                                .connectivity.rev181210.Context1.class)
                        .child(org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity
                                .rev181210.context.ConnectivityContext.class)
                        .child(ConnectivityService.class, new ConnectivityServiceKey(supportedConnService
                                .getUuid()))
                        .build();
                Optional<ConnectivityService> optNewConnService = this.networkTransactionService.read(
                        LogicalDatastoreType.OPERATIONAL, supportedConnServIID).get();
                if (optNewConnService.isEmpty()) {
                    LOG.error("Could not update a connectivity service");
                    continue;
                }
                ConnectivityService newConnService = optNewConnService.get();
                if (supportedConnService.getServiceLevel() != null
                        && supportedConnService.getServiceLevel().equals(supportingConnService.getValue())
                        && newConnService.getAdministrativeState() != AdministrativeState.LOCKED
                        && newConnService.getOperationalState() != OperationalState.DISABLED) {

                    ConnectivityService changedSupportedConnServ = new ConnectivityServiceBuilder()
                            .setUuid(supportedConnService.getUuid())
                            .setAdministrativeState(adminState)
                            .setOperationalState(operState)
                            .build();
                    this.networkTransactionService.merge(LogicalDatastoreType.OPERATIONAL, supportedConnServIID,
                            changedSupportedConnServ);
                    this.networkTransactionService.commit().get();
                    changedServices.add(changedSupportedConnServ);
                    if (layer == LayerProtocolName.ODU) {
                        changedServices.addAll(updateSupportedConnectivityServices(connServices,
                                supportedConnService.getUuid(), adminState,
                                operState, LayerProtocolName.DSR));
                    }
                }
            }
        } catch (InterruptedException | ExecutionException e) {
            LOG.error("Could not write updated connectivity service");
        }
        return changedServices;
    }

    private OperationalState transformOperState(String operString) {
        State operState = org.opendaylight.transportpce.networkmodel.util.TopologyUtils.setNetworkOperState(operString);
        return operState.equals(State.InService) ? OperationalState.ENABLED : OperationalState.DISABLED;
    }

}
