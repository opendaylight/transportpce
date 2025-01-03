/*
 * Copyright © 2021 Nokia, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.tapi.listeners;

import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import org.opendaylight.mdsal.binding.api.NotificationPublishService;
import org.opendaylight.mdsal.binding.api.NotificationService.CompositeListener;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.transportpce.common.network.NetworkTransactionService;
import org.opendaylight.transportpce.tapi.TapiStringConstants;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.state.types.rev191129.State;
import org.opendaylight.yang.gen.v1.nbi.notifications.rev230728.PublishTapiNotificationService;
import org.opendaylight.yang.gen.v1.nbi.notifications.rev230728.PublishTapiNotificationServiceBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.AdministrativeState;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.Context;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.DateAndTime;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.LayerProtocolName;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.OperationalState;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.Uuid;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.global._class.Name;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.global._class.NameKey;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev221121.CONNECTIVITYOBJECTTYPECONNECTIVITYSERVICE;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev221121.OwnedNodeEdgePoint1;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev221121.connection.LowerConnection;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev221121.connectivity.context.Connection;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev221121.connectivity.context.ConnectionBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev221121.connectivity.context.ConnectionKey;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev221121.connectivity.context.ConnectivityService;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev221121.connectivity.context.ConnectivityServiceBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev221121.connectivity.context.ConnectivityServiceKey;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev221121.context.ConnectivityContext;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.notification.rev221121.NOTIFICATIONTYPEATTRIBUTEVALUECHANGE;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.notification.rev221121.NameAndValueChange;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.notification.rev221121.Notification;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.notification.rev221121.notification.ChangedAttributes;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.notification.rev221121.notification.ChangedAttributesBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.notification.rev221121.notification.ChangedAttributesKey;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.notification.rev221121.notification.TargetObjectName;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.notification.rev221121.notification.TargetObjectNameBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.notification.rev221121.notification.TargetObjectNameKey;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.Context1;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.NodeEdgePointRef;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.TOPOLOGYOBJECTTYPENODEEDGEPOINT;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.context.TopologyContext;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.node.OwnedNodeEdgePoint;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.node.OwnedNodeEdgePointKey;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.topology.Node;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.topology.NodeKey;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.topology.context.Topology;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.topology.context.TopologyKey;
import org.opendaylight.yangtools.binding.DataObjectIdentifier;
import org.opendaylight.yangtools.binding.EnumTypeObject;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(service = TapiNetworkModelNotificationHandler.class)
public class TapiNetworkModelNotificationHandler {

    private static final Logger LOG = LoggerFactory.getLogger(TapiNetworkModelNotificationHandler.class);
    private final NetworkTransactionService networkTransactionService;
    private final NotificationPublishService notificationPublishService;
    private final List<ConnectivityService> connectivityServiceChanges = new ArrayList<>();
    private final Uuid tapiTopoUuid = new Uuid(UUID.nameUUIDFromBytes(TapiStringConstants.T0_FULL_MULTILAYER
            .getBytes(StandardCharsets.UTF_8)).toString());
    private final List<LayerProtocolName> orderedServiceLayerList;

    @Activate
    public TapiNetworkModelNotificationHandler(@Reference NetworkTransactionService networkTransactionService,
            @Reference NotificationPublishService notificationPublishService) {
        this.networkTransactionService = networkTransactionService;
        this.notificationPublishService = notificationPublishService;
        this.orderedServiceLayerList = List.of(LayerProtocolName.PHOTONICMEDIA, LayerProtocolName.ODU,
            LayerProtocolName.DSR, LayerProtocolName.ETH);
        LOG.debug("TapiNetworkModelListenerImpl instantiated");
    }

    public CompositeListener getCompositeListener() {
        return new CompositeListener(Set.of(
            new CompositeListener.Component<>(Notification.class, this::onNotification)));
    }

    private void onNotification(Notification notification) {
        LOG.info("Received network model notification {}", notification);
        if (notification.getNotificationType().equals(NOTIFICATIONTYPEATTRIBUTEVALUECHANGE.VALUE)
                && notification.getTargetObjectType().equals(TOPOLOGYOBJECTTYPENODEEDGEPOINT.VALUE)) {
            if (notification.getChangedAttributes() == null) {
                return;
            }
            // TODO: need to re-think this to update first the connections from roadm to roadm and then the others
            updateConnections(notification.getChangedAttributes().keySet().stream()
                    .map(changedAttributesKey -> new Uuid(changedAttributesKey.getValueName()))
                    .collect(Collectors.toList()), notification.getChangedAttributes().values().stream()
                    .map(NameAndValueChange::getNewValue)
                    .collect(Collectors.toList()));
            updateConnectivityServices();
            // todo set attributes
            for (ConnectivityService connService : this.connectivityServiceChanges) {
                sendNbiNotification(createNbiNotification(connService));
            }
        }
    }

//    @Override
//    public void onEventNotification(EventNotification notification) {
//        LOG.info("Received network model notification {}", notification);
//        //TODO: see if implementation needed and if in TAPI2.4, something supersedes this method
//    }

    private PublishTapiNotificationService createNbiNotification(ConnectivityService connService) {
        if (connService == null) {
            LOG.error("ConnService is null");
            return null;
        }
        Map<ChangedAttributesKey, ChangedAttributes> changedStates = new HashMap<>();
        changedStates.put(new ChangedAttributesKey("administrativeState"),
            new ChangedAttributesBuilder()
                .setNewValue(connService.getAdministrativeState().getName())
                .setOldValue(connService.getAdministrativeState().equals(AdministrativeState.UNLOCKED)
                    ? AdministrativeState.LOCKED.getName() : AdministrativeState.UNLOCKED.getName())
                .setValueName("administrativeState").build());
        changedStates.put(new ChangedAttributesKey("operationalState"),
            new ChangedAttributesBuilder()
                .setNewValue(connService.getOperationalState().getName())
                .setOldValue(connService.getOperationalState().equals(OperationalState.ENABLED)
                    ? OperationalState.DISABLED.getName() : OperationalState.ENABLED.getName())
                .setValueName("operationalState").build());
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssxxx");
        OffsetDateTime offsetDateTime = OffsetDateTime.now(ZoneOffset.UTC);
        DateAndTime datetime = new DateAndTime(dtf.format(offsetDateTime));
        Map<TargetObjectNameKey, TargetObjectName> targetObjectNames = new HashMap<>();
        if (connService.getName() != null) {
            for (Map.Entry<NameKey, Name> entry : connService.getName().entrySet()) {
                targetObjectNames.put(new TargetObjectNameKey(entry.getKey().getValueName()),
                    new TargetObjectNameBuilder()
                        .setValueName(entry.getValue().getValueName())
                        .setValue(entry.getValue().getValue())
                        .build());
            }
        }

        return new PublishTapiNotificationServiceBuilder()
            .setUuid(new Uuid(UUID.randomUUID().toString()))
            .setTopic(connService.getUuid().getValue())
            .setTargetObjectIdentifier(connService.getUuid())
            .setNotificationType(NOTIFICATIONTYPEATTRIBUTEVALUECHANGE.VALUE)
            .setChangedAttributes(changedStates)
            .setEventTimeStamp(datetime)
            .setTargetObjectName(targetObjectNames)
            .setTargetObjectType(CONNECTIVITYOBJECTTYPECONNECTIVITYSERVICE.VALUE)
            .setLayerProtocolName(connService.getLayerProtocolName())
            .build();
    }

    private void sendNbiNotification(PublishTapiNotificationService service) {
        try {
            this.notificationPublishService.putNotification(service);
        } catch (InterruptedException e) {
            LOG.warn("Cannot send notification to nbi", e);
            Thread.currentThread().interrupt();
        }
    }

    private void updateConnectivityServices() {
        try {
            this.connectivityServiceChanges.clear();
            DataObjectIdentifier<ConnectivityContext> connectivityContextIID = DataObjectIdentifier
                    .builder(Context.class)
                    .augmentation(
                        org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev221121.Context1.class)
                    .child(ConnectivityContext.class)
                    .build();
            Optional<ConnectivityContext> optConnContext =
                this.networkTransactionService.read(LogicalDatastoreType.OPERATIONAL, connectivityContextIID).get();
            if (optConnContext.isEmpty()) {
                LOG.error("Could not update TAPI connectivity services");
                return;
            }
            ConnectivityContext connContext = optConnContext.orElseThrow();
            Map<Uuid, EnumTypeObject[]> states = new HashMap<>();
            if (connContext.getConnectivityService() == null) {
                return;
            }
            for (ConnectivityService connService : connContext.getConnectivityService().values()) {
                LOG.info("Connectivity service = {}", connService);
                // TODO: maybe we need to check lower connections if my new code doesnt work
                states.put(connService.getUuid(), getStates(connService));
                AdministrativeState adminState = (AdministrativeState) states.get(connService.getUuid())[0];
                OperationalState operState = (OperationalState) states.get(connService.getUuid())[1];

                DataObjectIdentifier<ConnectivityService> connServIID = DataObjectIdentifier.builder(Context.class)
                    .augmentation(
                            org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev221121.Context1.class)
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
            // TODO: this last function may need some refactoring... if the PHOT_MEDIA goes down,
            //  then ODU goes down and then DSR should also go down
            for (ConnectivityService connService : connContext.getConnectivityService().values()) {
                AdministrativeState adminState = (AdministrativeState) states.get(connService.getUuid())[0];
                OperationalState operState = (OperationalState) states.get(connService.getUuid())[1];
                this.connectivityServiceChanges.addAll(updateSupportedConnectivityServices(
                    connContext.getConnectivityService().values(), connService.getUuid(), adminState, operState,
                    LayerProtocolName.ODU));
            }
        } catch (InterruptedException | ExecutionException e) {
            LOG.error("Could not update TAPI connectivity services");
        }
    }

    private EnumTypeObject[] getStates(ConnectivityService connService)
            throws InterruptedException, ExecutionException {
        OperationalState operState = OperationalState.ENABLED;
        AdministrativeState adminState = AdministrativeState.UNLOCKED;
        if (connService.getConnection() == null) {
            LOG.info("No connections on service = {}", connService);
            return new EnumTypeObject[]{null, null};
        }
        for (org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev221121
                .connectivity.service.Connection connection : connService.getConnection().values()) {
            DataObjectIdentifier<Connection> connIID = DataObjectIdentifier.builder(Context.class)
                    .augmentation(
                            org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev221121.Context1.class)
                    .child(ConnectivityContext.class)
                    .child(Connection.class, new ConnectionKey(connection.getConnectionUuid()))
                    .build();
            Optional<Connection> optConn = this.networkTransactionService.read(LogicalDatastoreType.OPERATIONAL,
                connIID).get();
            if (optConn.isEmpty()) {
                LOG.error("Could not get state for a TAPI connection");
                continue;
            }
            LOG.info("State of connection {} of connectivity service {} = {}", optConn.orElseThrow().getUuid()
                .getValue(), connService.getUuid().getValue(), optConn.orElseThrow().getOperationalState().getName());
            if (optConn.orElseThrow().getOperationalState() == OperationalState.DISABLED) {
                adminState = AdministrativeState.LOCKED;
                operState = OperationalState.DISABLED;
            }
        }
        return new EnumTypeObject[]{adminState, operState};
    }

    private void updateConnections(List<Uuid> changedOneps, List<String> onepStates) {
        LOG.info("Updating TAPI connections");
        LOG.info("Change in oneps = {}, new states = {}", changedOneps, onepStates);
        try {
            //should it return a list of connections?
            DataObjectIdentifier<org.opendaylight.yang.gen.v1.urn
                    .onf.otcc.yang.tapi.connectivity.rev221121.context.ConnectivityContext> connectivityContextIID =
                DataObjectIdentifier.builder(Context.class)
                    .augmentation(
                            org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev221121.Context1.class)
                    .child(org.opendaylight.yang.gen.v1.urn
                            .onf.otcc.yang.tapi.connectivity.rev221121.context.ConnectivityContext.class)
                    .build();
            Optional<org.opendaylight.yang.gen.v1.urn
                    .onf.otcc.yang.tapi.connectivity.rev221121.context.ConnectivityContext> optConnContext =
                this.networkTransactionService.read(LogicalDatastoreType.OPERATIONAL, connectivityContextIID).get();
            if (optConnContext.isEmpty()) {
                LOG.error(TapiStringConstants.TAPI_CONNECTION_UPDATE_ERROR);
                return;
            }
            if (optConnContext.orElseThrow().getConnectivityService() == null) {
                LOG.info("No TAPI connectivity service to update");
                return;
            }
            // TODO: order services from lower layer to upper layer
            Map<ConnectivityServiceKey, ConnectivityService> connServMap
                    = optConnContext.orElseThrow().getConnectivityService();
            if (connServMap == null) {
                LOG.info("No connections to update");
                return;
            }
            connServMap = orderConnServiceMap(connServMap);
            for (ConnectivityService connService : connServMap.values()) {
                LOG.info("Looping through connectivity service = {}", connService.getUuid().getValue());
                if (connService.getConnection() == null) {
                    continue;
                }
                for (org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev221121
                        .connectivity.service.Connection connection : connService.getConnection().values()) {
                    DataObjectIdentifier<Connection> connIID = DataObjectIdentifier.builder(Context.class)
                        .augmentation(org.opendaylight.yang.gen.v1
                                .urn.onf.otcc.yang.tapi.connectivity.rev221121.Context1.class)
                        .child(org.opendaylight.yang.gen.v1
                                .urn.onf.otcc.yang.tapi.connectivity.rev221121.context.ConnectivityContext.class)
                        .child(Connection.class, new ConnectionKey(connection.getConnectionUuid()))
                        .build();
                    Optional<Connection> optConn =
                        this.networkTransactionService.read(LogicalDatastoreType.OPERATIONAL, connIID).get();
                    if (optConn.isEmpty()) {
                        LOG.error(TapiStringConstants.TAPI_CONNECTION_READ_ERROR);
                        continue;
                    }
                    Connection newConn = optConn.orElseThrow();
                    // Check LowerConnection states and if any of the lower connection is disabled then we can put
                    // the connection out of service. And based on the connection previous state we decide
                    // the update necessary
                    OperationalState newConnState = newConn.getOperationalState();
                    if (newConn.getLowerConnection() != null) {
                        newConnState = updateLowerConnections(changedOneps, onepStates,
                                newConn.getLowerConnection().values(), newConn.getOperationalState());
                    }
                    if (newConnState.equals(newConn.getOperationalState())) {
                        // To check if the oneps are from the original Top connection
                        newConnState = getConnectionState(changedOneps, onepStates, newConn);
                    }
                    LOG.debug("Analysing connection = {} ", newConn.getName().toString());
                    LOG.info("Previous connection state = {} & New connection state = {}",
                            newConn.getOperationalState().getName(), newConnState.getName());
                    Connection changedConn = new ConnectionBuilder(newConn).setOperationalState(newConnState).build();
                    // TODO: the changed NEP is a DEG port which is not in any connection,
                    //  therefore we also need to change the cross connections,
                    //  the lower connections uuid and check the states.
                    //  If any of the lower connections of a connection is DISABLED then the top connection is DISABLED
                    this.networkTransactionService.merge(LogicalDatastoreType.OPERATIONAL, connIID, changedConn);
                    this.networkTransactionService.commit().get();
                }
            }
        } catch (InterruptedException | ExecutionException e) {
            LOG.error(TapiStringConstants.TAPI_CONNECTION_UPDATE_ERROR);
        }
    }

    private Map<ConnectivityServiceKey, ConnectivityService> orderConnServiceMap(
            Map<ConnectivityServiceKey, ConnectivityService> connServMap) {
        Map<ConnectivityServiceKey, ConnectivityService> orderedServiceMap = new HashMap<>();
        for (LayerProtocolName lpn:this.orderedServiceLayerList) {
            for (ConnectivityService connServ:connServMap.values()) {
                if (connServ.getLayerProtocolName().equals(lpn)) {
                    LOG.info("Layer of service is equal to entry of lpn = {}", lpn);
                    orderedServiceMap.put(connServ.key(), connServ);
                }
            }
        }
        LOG.info("Ordered map of services = {}", orderedServiceMap);
        return orderedServiceMap;
    }

    private OperationalState updateLowerConnections(List<Uuid> changedOneps, List<String> onepStates,
                                                    Collection<LowerConnection> lowerConnections,
                                                    OperationalState uppConnState) {
        LOG.info("Updating lower connections");
        OperationalState topConnectionState = uppConnState;
        Boolean allLowerConnEna = true;
        try {
            for (LowerConnection lowerConn:lowerConnections) {
                DataObjectIdentifier<Connection> connIID = DataObjectIdentifier.builder(Context.class)
                    .augmentation(
                            org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev221121.Context1.class)
                    .child(org.opendaylight.yang.gen.v1.urn
                            .onf.otcc.yang.tapi.connectivity.rev221121.context.ConnectivityContext.class)
                    .child(Connection.class, new ConnectionKey(lowerConn.getConnectionUuid()))
                    .build();
                Optional<Connection> optConn =
                        this.networkTransactionService.read(LogicalDatastoreType.OPERATIONAL, connIID).get();
                if (optConn.isEmpty()) {
                    LOG.error(TapiStringConstants.TAPI_CONNECTION_READ_ERROR);
                    continue;
                }
                Connection newConn = optConn.orElseThrow(); // Current state of connection
                if (newConn.getLowerConnection() != null) {
                    // TODO: we can receive disable here because the lower connection haven been yet looped through and
                    //  therefore it is disabled but it has to be changed to enable before returning disable.
                    //  We need to recall the update Lower Connections with the lower connection we are checking now
                    //  and all its lower connections, until no lower connections are found in the connection
                    updateLowerConnections(changedOneps, onepStates, newConn.getLowerConnection().values(),
                            newConn.getOperationalState());
                }
                OperationalState newConnState = getConnectionState(changedOneps, onepStates, newConn);
                // updated connection state if it contains a nep that has changed
                if (!newConn.getOperationalState().equals(newConnState)) {
                    Connection changedConn = new ConnectionBuilder(newConn).setOperationalState(newConnState).build();
                    this.networkTransactionService.merge(LogicalDatastoreType.OPERATIONAL, connIID, changedConn);
                    this.networkTransactionService.commit().get();
                }
                if (newConnState.equals(OperationalState.DISABLED)) {
                    LOG.info("LowerConnection state is disable");
                    allLowerConnEna = false;
                    topConnectionState = OperationalState.DISABLED;
                }
            }
            if (allLowerConnEna) {
                return OperationalState.ENABLED;
            }
            return OperationalState.DISABLED;
        } catch (InterruptedException | ExecutionException e) {
            LOG.error(TapiStringConstants.TAPI_CONNECTION_UPDATE_ERROR);
            return topConnectionState;
        }
    }

    private OperationalState getConnectionState(List<Uuid> changedOneps, List<String> operState, Connection conn)
            throws InterruptedException, ExecutionException {
        LOG.info("Getting TAPI connectionState");
        List<Uuid> connectionNeps = Objects.requireNonNull(conn.getConnectionEndPoint()).values().stream()
            .map(NodeEdgePointRef::getNodeEdgePointUuid).collect(Collectors.toList());
        LOG.info("Changed neps = {}", changedOneps);
        LOG.info("Connection NEPs = {}", connectionNeps);
        if (!Collections.disjoint(changedOneps, connectionNeps)) {
            LOG.info("Connection neps {} are included in changed oneps {}", connectionNeps, changedOneps);
            if ((changedOneps.contains(connectionNeps.get(0)) ? transformOperState(operState.get(
                    changedOneps.indexOf(connectionNeps.get(0)))) : null) == OperationalState.DISABLED
                        || (changedOneps.contains(connectionNeps.get(1)) ? transformOperState(operState.get(
                                changedOneps.indexOf(connectionNeps.get(1)))) : null) == OperationalState.DISABLED) {
                return OperationalState.DISABLED;
            }
            LOG.info("Didnt transform correctly the states");
            for (Uuid connectionNep : connectionNeps) {
                Optional<org.opendaylight.yang.gen.v1.urn
                        .onf.otcc.yang.tapi.connectivity.rev221121.connection.ConnectionEndPoint> ocep
                    = conn.getConnectionEndPoint().values().stream()
                        .filter(connectionEndPoint -> connectionEndPoint.getNodeEdgePointUuid() == connectionNep)
                        .findFirst();
                if (ocep.isEmpty()) {
                    continue;
                }
                DataObjectIdentifier<OwnedNodeEdgePoint> onepIID = DataObjectIdentifier.builder(Context.class)
                    .augmentation(Context1.class).child(TopologyContext.class)
                    .child(Topology.class, new TopologyKey(tapiTopoUuid))
                    .child(Node.class, new NodeKey(ocep.orElseThrow().getNodeUuid()))
                    .child(OwnedNodeEdgePoint.class, new OwnedNodeEdgePointKey(connectionNep))
                    .build();
                Optional<OwnedNodeEdgePoint> onep =
                    this.networkTransactionService.read(LogicalDatastoreType.OPERATIONAL, onepIID).get();
                if (onep.isEmpty() || onep.orElseThrow().augmentation(OwnedNodeEdgePoint1.class) == null
                        || onep.orElseThrow().augmentation(OwnedNodeEdgePoint1.class).getCepList() == null) {
                    continue;
                }
                if (onep.orElseThrow().getOperationalState() == OperationalState.DISABLED
                        && !changedOneps.contains(onep.orElseThrow().getUuid())) {
                    return OperationalState.DISABLED;
                }
            }
            return OperationalState.ENABLED;
        }
        LOG.info("Connection state = {}. Going to check lower connections", conn.getOperationalState());
        // TODO --> check all lower connections state and if all of them are enabled we return enable, otherwise disable
        if (conn.getLowerConnection() != null && allLowerConEnabled(conn.getLowerConnection().values())) {
            return OperationalState.ENABLED;
        }
        return conn.getOperationalState();
    }

    private boolean allLowerConEnabled(Collection<LowerConnection> lowerConnections) {
        try {
            for (LowerConnection lowerConn:lowerConnections) {
                DataObjectIdentifier<Connection> connIID = DataObjectIdentifier.builder(Context.class)
                    .augmentation(
                            org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev221121.Context1.class)
                    .child(org.opendaylight.yang.gen.v1
                            .urn.onf.otcc.yang.tapi.connectivity.rev221121.context.ConnectivityContext.class)
                    .child(Connection.class, new ConnectionKey(lowerConn.getConnectionUuid()))
                    .build();
                Optional<Connection> optConn =
                    this.networkTransactionService.read(LogicalDatastoreType.OPERATIONAL, connIID).get();
                if (optConn.isEmpty()) {
                    LOG.error(TapiStringConstants.TAPI_CONNECTION_READ_ERROR);
                    continue;
                }
                Connection newConn = optConn.orElseThrow(); // Current state of connection
                // updated connection state if it contains a nep that has changed
                if (newConn.getOperationalState().equals(OperationalState.DISABLED)) {
                    LOG.info("LowerConnection state is disable");
                    return false;
                }
            }
            return true;
        } catch (InterruptedException | ExecutionException e) {
            LOG.error(TapiStringConstants.TAPI_CONNECTION_UPDATE_ERROR);
            return false;
        }
    }

    private List<ConnectivityService> updateSupportedConnectivityServices(Collection<ConnectivityService> connServices,
                                                                          Uuid supportingConnService,
                                                                          AdministrativeState adminState,
                                                                          OperationalState operState,
                                                                          LayerProtocolName layer) {
        // TODO we need to check this function
        List<ConnectivityService> changedServices = new ArrayList<>();
        if (adminState != AdministrativeState.LOCKED && operState != OperationalState.DISABLED) {
            return changedServices;
        }
        try {
            for (ConnectivityService supportedConnService : connServices) {
                // TODO currently supporting service uuid is saved in service layer, replace with name as soon
                // as name is implemented
                if (supportedConnService.getLayerProtocolName() != layer) {
                    continue;
                }
                DataObjectIdentifier<ConnectivityService> supportedConnServIID = DataObjectIdentifier
                    .builder(Context.class)
                    .augmentation(
                            org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev221121.Context1.class)
                    .child(org.opendaylight.yang.gen.v1
                            .urn.onf.otcc.yang.tapi.connectivity.rev221121.context.ConnectivityContext.class)
                    .child(ConnectivityService.class, new ConnectivityServiceKey(supportedConnService.getUuid()))
                    .build();
                Optional<ConnectivityService> optNewConnService = this.networkTransactionService.read(
                    LogicalDatastoreType.OPERATIONAL, supportedConnServIID).get();
                if (optNewConnService.isEmpty()) {
                    LOG.error("Could not update TAPI connectivity service");
                    continue;
                }
                ConnectivityService newConnService = optNewConnService.orElseThrow();
                if (supportedConnService.getConnectivityConstraint().getServiceLevel() != null
                        && supportedConnService.getConnectivityConstraint().getServiceLevel()
                            .equals(supportingConnService.getValue())
                        && newConnService.getAdministrativeState() != AdministrativeState.LOCKED
                        && newConnService.getOperationalState() != OperationalState.DISABLED) {

                    ConnectivityService changedSupportedConnServ = new ConnectivityServiceBuilder()
                        .setUuid(supportedConnService.getUuid())
                        .setAdministrativeState(adminState)
                        .setOperationalState(operState)
                        .build();
                    // TODO: may need to update connections...
                    this.networkTransactionService.merge(LogicalDatastoreType.OPERATIONAL, supportedConnServIID,
                        changedSupportedConnServ);
                    this.networkTransactionService.commit().get();
                    changedServices.add(changedSupportedConnServ);
                    if (layer == LayerProtocolName.ODU) {
                        changedServices.addAll(updateSupportedConnectivityServices(connServices,
                            supportedConnService.getUuid(), adminState, operState, LayerProtocolName.DSR));
                    }
                }
            }
        } catch (InterruptedException | ExecutionException e) {
            LOG.error("Could not update TAPI connectivity service");
        }
        return changedServices;
    }

    private OperationalState transformOperState(String operString) {
        LOG.debug("Operstring to be converted = {}", operString);
        State operState = org.opendaylight.transportpce.networkmodel.util.TopologyUtils.setNetworkOperState(operString);
        LOG.debug("State received from topologyutils = {}", operState);
        return operState.equals(State.InService) ? OperationalState.ENABLED : OperationalState.DISABLED;
    }

}
