/*
 * Copyright © 2020 Orange, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.nbinotifications.utils;

import java.util.Map;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.node.types.rev210528.NodeIdType;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev211210.ConnectionType;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev211210.service.endpoint.RxDirection;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev211210.service.endpoint.RxDirectionBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev211210.service.endpoint.RxDirectionKey;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev211210.service.endpoint.TxDirection;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev211210.service.endpoint.TxDirectionBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev211210.service.endpoint.TxDirectionKey;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev211210.service.lgx.LgxBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev211210.service.port.PortBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.state.types.rev191129.State;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.format.rev191129.ServiceFormat;
import org.opendaylight.yang.gen.v1.nbi.notifications.rev210813.NotificationProcessService;
import org.opendaylight.yang.gen.v1.nbi.notifications.rev210813.NotificationProcessServiceBuilder;
import org.opendaylight.yang.gen.v1.nbi.notifications.rev210813.get.notifications.alarm.service.output.NotificationsAlarmService;
import org.opendaylight.yang.gen.v1.nbi.notifications.rev210813.get.notifications.alarm.service.output.NotificationsAlarmServiceBuilder;
import org.opendaylight.yang.gen.v1.nbi.notifications.rev210813.get.notifications.process.service.output.NotificationsProcessService;
import org.opendaylight.yang.gen.v1.nbi.notifications.rev210813.get.notifications.process.service.output.NotificationsProcessServiceBuilder;
import org.opendaylight.yang.gen.v1.nbi.notifications.rev210813.notification.process.service.ServiceAEndBuilder;
import org.opendaylight.yang.gen.v1.nbi.notifications.rev210813.notification.process.service.ServiceZEndBuilder;
import org.opendaylight.yangtools.yang.common.Uint32;
import org.opendaylight.yangtools.yang.common.Uint8;

public final class NotificationServiceDataUtils {

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
}
