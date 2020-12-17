/*
 * Copyright © 2020 Orange, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.nbinotifications.utils;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import org.json.JSONObject;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.equipment.types.rev181130.OpticTypes;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.node.types.rev181130.NodeIdType;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev190531.ConnectionType;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev190531.service.endpoint.RxDirection;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev190531.service.endpoint.RxDirectionBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev190531.service.endpoint.TxDirection;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev190531.service.endpoint.TxDirectionBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev190531.service.lgx.Lgx;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev190531.service.lgx.LgxBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev190531.service.port.Port;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev190531.service.port.PortBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.state.types.rev181130.State;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.format.rev190531.ServiceFormat;
import org.opendaylight.yang.gen.v1.nbi.notifications.rev201130.NotificationService;
import org.opendaylight.yang.gen.v1.nbi.notifications.rev201130.get.notifications.service.output.NotificationServiceBuilder;
import org.opendaylight.yang.gen.v1.nbi.notifications.rev201130.notification.service.ServiceAEndBuilder;
import org.opendaylight.yang.gen.v1.nbi.notifications.rev201130.notification.service.ServiceZEndBuilder;
import org.opendaylight.yangtools.yang.common.Uint32;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class NbiNotificationsUtils {

    private static final Logger LOG = LoggerFactory.getLogger(NbiNotificationsUtils.class);

    private NbiNotificationsUtils() {
    }

    public static org.opendaylight.yang.gen.v1.nbi.notifications.rev201130.get.notifications.service.output
            .NotificationService deserializeNotificationService(JSONObject input) {
        JSONObject serviceAEndJSon = input.getJSONObject("service-a-end");
        ServiceAEndBuilder serviceAEnd = new ServiceAEndBuilder()
                .setClli(serviceAEndJSon.getString("clli"))
                .setServiceFormat(ServiceFormat.valueOf(serviceAEndJSon.getString("service-format")))
                .setNodeId(new NodeIdType(serviceAEndJSon.getString("node-id")))
                .setServiceRate(Uint32.valueOf((serviceAEndJSon.getInt("service-rate"))))
                .setOpticType(OpticTypes.valueOf(serviceAEndJSon.getString("optic-type")))
                .setTxDirection(deserializeTxDirections(serviceAEndJSon.getJSONObject("tx-direction")
                        .getJSONObject("port"), serviceAEndJSon.getJSONObject("tx-direction").getJSONObject("lgx")))
                .setRxDirection(deserializeRxDirections(serviceAEndJSon.getJSONObject("rx-direction")
                        .getJSONObject("port"), serviceAEndJSon.getJSONObject("rx-direction").getJSONObject("lgx")));
        JSONObject serviceZEndJSon = input.getJSONObject("service-z-end");
        ServiceZEndBuilder serviceZEnd = new ServiceZEndBuilder()
                .setClli(serviceZEndJSon.getString("clli"))
                .setServiceFormat(ServiceFormat.valueOf(serviceZEndJSon.getString("service-format")))
                .setNodeId(new NodeIdType(serviceZEndJSon.getString("node-id")))
                .setServiceRate(Uint32.valueOf((serviceZEndJSon.getInt("service-rate"))))
                .setOpticType(OpticTypes.valueOf(serviceZEndJSon.getString("optic-type")))
                .setTxDirection(deserializeTxDirections(serviceZEndJSon.getJSONObject("tx-direction")
                        .getJSONObject("port"), serviceZEndJSon.getJSONObject("tx-direction").getJSONObject("lgx")))
                .setRxDirection(deserializeRxDirections(serviceZEndJSon.getJSONObject("rx-direction")
                        .getJSONObject("port"), serviceZEndJSon.getJSONObject("rx-direction").getJSONObject("lgx")));
        NotificationServiceBuilder notificationService = new NotificationServiceBuilder()
                .setServiceName(input.getString("service-name"))
                .setConnectionType(ConnectionType.valueOf((input.getString("connection-type"))))
                .setCommonId(input.getString("common-id"))
                .setMessage(input.getString("message"))
                .setServiceAEnd(serviceAEnd.build())
                .setServiceZEnd(serviceZEnd.build())
                .setResponseFailed(input.getString("response-failed"))
                .setOperationalState(State.valueOf(input.getString("operational-state")));
        return notificationService.build();
    }

    public static JSONObject serializeNotificationService(NotificationService notificationService) {
        JSONObject eventJson = new JSONObject()
                .put("service-name", notificationService.getServiceName())
                .put("connection-type", notificationService.getConnectionType())
                .put("common-id", notificationService.getCommonId())
                .put("message", notificationService.getMessage())
                .put("response-failed", notificationService.getResponseFailed())
                .put("operational-state", notificationService.getOperationalState());
        JSONObject serviceAEndJSon = new JSONObject()
                .put("clli", notificationService.getServiceAEnd().getClli())
                .put("service-format", notificationService.getServiceAEnd().getServiceFormat())
                .put("node-id", notificationService.getServiceAEnd().getNodeId().getValue())
                .put("service-rate", notificationService.getServiceAEnd().getServiceRate())
                .put("optic-type", notificationService.getServiceAEnd().getOpticType())
                .put("tx-direction", serializeRxTxDirections(notificationService.getServiceAEnd().getTxDirection()
                        .getPort(), notificationService.getServiceAEnd().getTxDirection().getLgx()))
                .put("rx-direction", serializeRxTxDirections(notificationService.getServiceAEnd().getRxDirection()
                        .getPort(), notificationService.getServiceAEnd().getRxDirection().getLgx()));
        eventJson.put("service-a-end", serviceAEndJSon);
        JSONObject serviceZEndJson = new JSONObject()
                .put("clli", notificationService.getServiceZEnd().getClli())
                .put("service-format", notificationService.getServiceZEnd().getServiceFormat())
                .put("node-id", notificationService.getServiceZEnd().getNodeId().getValue())
                .put("service-rate", notificationService.getServiceZEnd().getServiceRate())
                .put("optic-type", notificationService.getServiceZEnd().getOpticType())
                .put("tx-direction", serializeRxTxDirections(notificationService.getServiceZEnd().getTxDirection()
                        .getPort(), notificationService.getServiceZEnd().getTxDirection().getLgx()))
                .put("rx-direction", serializeRxTxDirections(notificationService.getServiceZEnd().getRxDirection()
                        .getPort(), notificationService.getServiceZEnd().getRxDirection().getLgx()));
        eventJson.put("service-z-end", serviceZEndJson);
        return eventJson;
    }

    private static JSONObject serializeRxTxDirections(Port port, Lgx lgx) {
        JSONObject portJson = new JSONObject()
                .put("port-rack", port.getPortRack())
                .put("port-shelf", port.getPortShelf())
                .put("port-device-name", port.getPortDeviceName())
                .put("port-name", port.getPortName())
                .put("port-type", port.getPortType());
        JSONObject lgxJson = new JSONObject()
                .put("lgx-port-name", lgx.getLgxPortName())
                .put("lgx-port-shelf", lgx.getLgxPortShelf())
                .put("lgx-device-name", lgx.getLgxDeviceName())
                .put("lgx-port-rack", lgx.getLgxPortRack());
        return new JSONObject().put("port", portJson).put("lgx", lgxJson);
    }

    private static TxDirection deserializeTxDirections(JSONObject port, JSONObject lgx) {
        TxDirectionBuilder txDirection = new TxDirectionBuilder()
                .setPort(new PortBuilder()
                        .setPortRack(port.getString("port-rack"))
                        .setPortShelf(port.getString("port-shelf"))
                        .setPortDeviceName(port.getString("port-device-name"))
                        .setPortName(port.getString("port-name"))
                        .setPortType(port.getString("port-type"))
                        .build())
                .setLgx(new LgxBuilder()
                        .setLgxPortName(lgx.getString("lgx-port-name"))
                        .setLgxPortShelf(lgx.getString("lgx-port-shelf"))
                        .setLgxDeviceName(lgx.getString("lgx-device-name"))
                        .setLgxPortRack(lgx.getString("lgx-port-rack"))
                        .build());
        return txDirection.build();
    }

    private static RxDirection deserializeRxDirections(JSONObject port, JSONObject lgx) {
        RxDirectionBuilder rxDirection = new RxDirectionBuilder()
                .setPort(new PortBuilder()
                        .setPortRack(port.getString("port-rack"))
                        .setPortShelf(port.getString("port-shelf"))
                        .setPortDeviceName(port.getString("port-device-name"))
                        .setPortName(port.getString("port-name"))
                        .setPortType(port.getString("port-type"))
                        .build())
                .setLgx(new LgxBuilder()
                        .setLgxPortName(lgx.getString("lgx-port-name"))
                        .setLgxPortShelf(lgx.getString("lgx-port-shelf"))
                        .setLgxDeviceName(lgx.getString("lgx-device-name"))
                        .setLgxPortRack(lgx.getString("lgx-port-rack"))
                        .build());
        return rxDirection.build();
    }

    public static Properties loadProperties(String propertyFileName) {
        Properties props = new Properties();
        InputStream inputStream = NbiNotificationsUtils.class.getClassLoader()
                .getResourceAsStream(propertyFileName);
        try {
            if (inputStream != null) {
                props.load(inputStream);
            } else {
                LOG.warn("Kafka property file '{}' is empty", propertyFileName);
            }
        } catch (IOException e) {
            LOG.warn("Kafka property file '{}' was not found in the classpath", propertyFileName);
        }
        return props;
    }
}
