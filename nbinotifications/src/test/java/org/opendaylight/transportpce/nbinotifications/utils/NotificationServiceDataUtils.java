/*
 * Copyright © 2020 Orange, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.nbinotifications.utils;

import java.nio.charset.Charset;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.transportpce.common.network.NetworkTransactionService;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.node.types.rev210528.NodeIdType;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev230526.ConnectionType;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev230526.service.endpoint.RxDirection;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev230526.service.endpoint.RxDirectionBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev230526.service.endpoint.RxDirectionKey;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev230526.service.endpoint.TxDirection;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev230526.service.endpoint.TxDirectionBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev230526.service.endpoint.TxDirectionKey;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev230526.service.lgx.LgxBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev230526.service.port.PortBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.state.types.rev191129.State;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.format.rev191129.ServiceFormat;
import org.opendaylight.yang.gen.v1.nbi.notifications.rev230728.NotificationProcessService;
import org.opendaylight.yang.gen.v1.nbi.notifications.rev230728.NotificationProcessServiceBuilder;
import org.opendaylight.yang.gen.v1.nbi.notifications.rev230728.get.notifications.alarm.service.output.NotificationsAlarmService;
import org.opendaylight.yang.gen.v1.nbi.notifications.rev230728.get.notifications.alarm.service.output.NotificationsAlarmServiceBuilder;
import org.opendaylight.yang.gen.v1.nbi.notifications.rev230728.get.notifications.process.service.output.NotificationsProcessService;
import org.opendaylight.yang.gen.v1.nbi.notifications.rev230728.get.notifications.process.service.output.NotificationsProcessServiceBuilder;
import org.opendaylight.yang.gen.v1.nbi.notifications.rev230728.notification.process.service.ServiceAEndBuilder;
import org.opendaylight.yang.gen.v1.nbi.notifications.rev230728.notification.process.service.ServiceZEndBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.AdministrativeState;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.Context;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.ContextBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.DateAndTime;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.LayerProtocolName;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.OperationalState;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.Uuid;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.local._class.Name;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.local._class.NameBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev221121.CONNECTIVITYOBJECTTYPECONNECTIVITYSERVICE;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev221121.Context1;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev221121.Context1Builder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev221121.context.ConnectivityContextBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.notification.rev221121.CreateNotificationSubscriptionServiceInputBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.notification.rev221121.GetNotificationListInputBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.notification.rev221121.NOTIFICATIONTYPEATTRIBUTEVALUECHANGE;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.notification.rev221121.NOTIFICATIONTYPEOBJECTCREATION;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.notification.rev221121.NOTIFICATIONTYPEOBJECTDELETION;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.notification.rev221121.SubscriptionState;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.notification.rev221121.context.NotificationContextBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.notification.rev221121.create.notification.subscription.service.input.SubscriptionFilter;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.notification.rev221121.create.notification.subscription.service.input.SubscriptionFilterBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.notification.rev221121.get.notification.list.output.Notification;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.notification.rev221121.get.notification.list.output.NotificationBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.notification.rev221121.notification.ChangedAttributes;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.notification.rev221121.notification.ChangedAttributesBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.notification.rev221121.notification.TargetObjectName;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.notification.rev221121.notification.TargetObjectNameBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.context.TopologyContextBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.topology.context.NwTopologyServiceBuilder;
import org.opendaylight.yangtools.binding.DataObjectIdentifier;
import org.opendaylight.yangtools.yang.common.Uint32;
import org.opendaylight.yangtools.yang.common.Uint8;

public final class NotificationServiceDataUtils {

    public static final String TAPI_CONTEXT = "T-API context";

    private NotificationServiceDataUtils() {
    }

    public static NotificationProcessService buildSendEventInput() {
        return new NotificationProcessServiceBuilder()
                .setMessage("message")
                .setServiceName("service1")
                .setOperationalState(State.InService)
                .setResponseFailed("")
                .setCommonId("commond-id")
                .setConnectionType(ConnectionType.Service)
                .setServiceZEnd(getServiceZEndBuild().build())
                .setServiceAEnd(getServiceAEndBuild().build())
                .build();
    }

    public static NotificationsProcessService buildReceivedEvent() {
        return new NotificationsProcessServiceBuilder()
                .setMessage("message")
                .setServiceName("service1")
                .setOperationalState(State.InService)
                .setResponseFailed("")
                .setCommonId("commond-id")
                .setConnectionType(ConnectionType.Service)
                .setServiceZEnd(getServiceZEndBuild().build())
                .setServiceAEnd(getServiceAEndBuild().build())
                .build();
    }

    public static NotificationsAlarmService buildReceivedAlarmEvent() {
        return new NotificationsAlarmServiceBuilder()
                .setMessage("message")
                .setServiceName("service1")
                .setOperationalState(State.InService)
                .setConnectionType(ConnectionType.Service)
                .build();
    }

    public static Notification buildReceivedTapiAlarmEvent() {
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssxxx");
        OffsetDateTime offsetDateTime = OffsetDateTime.now(ZoneOffset.UTC);
        DateAndTime datetime = new DateAndTime(dtf.format(offsetDateTime));
        Uuid targetObjectId = new Uuid(UUID.randomUUID().toString());
        TargetObjectName objectName = new TargetObjectNameBuilder()
            .setValue(targetObjectId.getValue())
            .setValueName("Connectivity Service Name")
            .build();
        ChangedAttributes adminStateChange = new ChangedAttributesBuilder()
            .setValueName("administrativeState")
            .setOldValue(AdministrativeState.LOCKED.getName())
            .setNewValue(AdministrativeState.UNLOCKED.getName())
            .build();
        ChangedAttributes operStateChange = new ChangedAttributesBuilder()
            .setValueName("operationalState")
            .setOldValue(OperationalState.DISABLED.getName())
            .setNewValue(OperationalState.ENABLED.getName())
            .build();
        return new NotificationBuilder()
            .setNotificationType(NOTIFICATIONTYPEATTRIBUTEVALUECHANGE.VALUE)
            .setLayerProtocolName(LayerProtocolName.ETH)
            .setTargetObjectType(CONNECTIVITYOBJECTTYPECONNECTIVITYSERVICE.VALUE)
            .setEventTimeStamp(datetime)
            .setUuid(new Uuid(UUID.randomUUID().toString()))
            .setTargetObjectIdentifier(targetObjectId)
            .setTargetObjectName(Map.of(objectName.key(), objectName))
            .setChangedAttributes(Map.of(adminStateChange.key(), adminStateChange,
                operStateChange.key(), operStateChange))
            .build();
    }

    public static ServiceAEndBuilder getServiceAEndBuild() {
        return new ServiceAEndBuilder()
                .setClli("clli")
                .setServiceFormat(ServiceFormat.OC)
                .setServiceRate(Uint32.valueOf(1))
                .setNodeId(new NodeIdType("XPONDER-1-2"))
                .setTxDirection(getTxDirection())
                .setRxDirection(getRxDirection());
    }

    public static ServiceZEndBuilder getServiceZEndBuild() {
        return new ServiceZEndBuilder()
                .setClli("clli")
                .setServiceFormat(ServiceFormat.OC)
                .setServiceRate(Uint32.valueOf(1))
                .setNodeId(new NodeIdType("XPONDER-1-2"))
                .setTxDirection(getTxDirection())
                .setRxDirection(getRxDirection());
    }

    public static CreateNotificationSubscriptionServiceInputBuilder buildNotificationSubscriptionServiceInputBuilder() {
        Name name = new NameBuilder()
            .setValue("test subscription")
            .setValueName("Subscription name")
            .build();
        SubscriptionFilter subscriptionFilter = new SubscriptionFilterBuilder()
            .setRequestedObjectTypes(new HashSet<>(List.of(CONNECTIVITYOBJECTTYPECONNECTIVITYSERVICE.VALUE)))
            .setRequestedNotificationTypes(new HashSet<>(List.of(NOTIFICATIONTYPEATTRIBUTEVALUECHANGE.VALUE,
                NOTIFICATIONTYPEOBJECTCREATION.VALUE, NOTIFICATIONTYPEOBJECTDELETION.VALUE)))
            .setRequestedLayerProtocols(new HashSet<>(List.of(LayerProtocolName.ETH)))
            .setRequestedObjectIdentifier(new HashSet<>(List.of(new Uuid(UUID.randomUUID().toString()))))
            .setIncludeContent(true)
            .setLocalId("localId")
            .setName(Map.of(name.key(), name))
            .build();
        return new CreateNotificationSubscriptionServiceInputBuilder()
            .setSubscriptionFilter(subscriptionFilter)
            .setSubscriptionState(SubscriptionState.ACTIVE);
    }

    private static Map<TxDirectionKey, TxDirection> getTxDirection() {
        return Map.of(new TxDirectionKey(Uint8.ZERO), new TxDirectionBuilder()
                .setPort(new PortBuilder()
                        .setPortDeviceName("device name")
                        .setPortName("port name")
                        .setPortRack("port rack")
                        .setPortShelf("port shelf")
                        .setPortSlot("port slot")
                        .setPortSubSlot("port subslot")
                        .setPortType("port type")
                        .build())
                .setLgx(new LgxBuilder()
                        .setLgxDeviceName("lgx device name")
                        .setLgxPortName("lgx port name")
                        .setLgxPortRack("lgx port rack")
                        .setLgxPortShelf("lgx port shelf")
                        .build())
                .setIndex(Uint8.ZERO)
                .build());
    }

    private static Map<RxDirectionKey, RxDirection> getRxDirection() {
        return Map.of(new RxDirectionKey(Uint8.ZERO), new RxDirectionBuilder()
                .setPort(new PortBuilder()
                        .setPortDeviceName("device name")
                        .setPortName("port name")
                        .setPortRack("port rack")
                        .setPortShelf("port shelf")
                        .setPortSlot("port slot")
                        .setPortSubSlot("port subslot")
                        .setPortType("port type")
                        .build())
                .setLgx(new LgxBuilder()
                        .setLgxDeviceName("lgx device name")
                        .setLgxPortName("lgx port name")
                        .setLgxPortRack("lgx port rack")
                        .setLgxPortShelf("lgx port shelf")
                        .build())
                .setIndex(Uint8.ZERO)
                .build());
    }

    public static void createTapiContext(
            NetworkTransactionService networkTransactionService) throws ExecutionException, InterruptedException {
        // Augmenting tapi context to include topology and connectivity contexts
        org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.global._class.Name contextName
            = new org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.global._class.NameBuilder()
            .setValue(TAPI_CONTEXT).setValueName("TAPI Context Name").build();

        Context1 connectivityContext =
            new Context1Builder()
                .setConnectivityContext(
                    new ConnectivityContextBuilder()
                        .setConnection(new HashMap<>())
                        .setConnectivityService(new HashMap<>())
                        .build())
                .build();

        org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.global._class.Name nwTopoServiceName =
            new org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.global._class.NameBuilder()
                .setValue("Network Topo Service")
                .setValueName("Network Topo Service Name")
                .build();

        org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.Context1 topologyContext
            = new org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.Context1Builder()
                .setTopologyContext(new TopologyContextBuilder()
                    .setNwTopologyService(new NwTopologyServiceBuilder()
                        .setTopology(new HashMap<>())
                        .setUuid(
                            new Uuid(
                                UUID.nameUUIDFromBytes("Network Topo Service".getBytes(Charset.forName("UTF-8")))
                                    .toString()))
                    .setName(Map.of(nwTopoServiceName.key(), nwTopoServiceName))
                    .build())
                .setTopology(new HashMap<>())
                .build())
            .build();

        org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.notification.rev221121.Context1 notificationContext
            = new org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.notification.rev221121.Context1Builder()
                .setNotificationContext(new NotificationContextBuilder()
                    .setNotification(new HashMap<>())
                    .setNotifSubscription(new HashMap<>())
                    .build())
                .build();

        // todo: add notification context
        // put in datastore
        networkTransactionService.put(
                LogicalDatastoreType.OPERATIONAL,
                DataObjectIdentifier.builder(Context.class).build(),
                new ContextBuilder()
                    .setName(Map.of(contextName.key(), contextName))
                    .setUuid(
                        new Uuid(UUID.nameUUIDFromBytes(TAPI_CONTEXT.getBytes(Charset.forName("UTF-8"))).toString()))
                    .setServiceInterfacePoint(new HashMap<>())
                    .addAugmentation(connectivityContext)
                    .addAugmentation(topologyContext)
                    .addAugmentation(notificationContext)
                    .build());
        networkTransactionService.commit().get();
    }

    public static GetNotificationListInputBuilder buildGetNotificationListInputBuilder(String subscriptionUuid) {
        return new GetNotificationListInputBuilder()
                .setSubscriptionId(new Uuid(UUID.fromString(subscriptionUuid).toString()))
                .setTimeRange(null);
    }
}
