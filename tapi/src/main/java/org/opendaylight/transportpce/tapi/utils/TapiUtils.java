/*
 * Copyright © 2018 Orange, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.tapi.utils;

import org.opendaylight.yang.gen.v1.http.org.openroadm.common.equipment.types.rev181130.OpticTypes;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.node.types.rev181130.NodeIdType;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev190531.ConnectionType;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev190531.RpcActions;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev190531.sdnc.request.header.SdncRequestHeaderBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev190531.service.endpoint.RxDirectionBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev190531.service.endpoint.TxDirectionBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev190531.service.lgx.LgxBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev190531.service.port.PortBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.format.rev190531.ServiceFormat;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev190531.ServiceCreateInput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev190531.ServiceCreateInputBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev190531.service.create.input.ServiceAEnd;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev190531.service.create.input.ServiceAEndBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev190531.service.create.input.ServiceZEnd;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev190531.service.create.input.ServiceZEndBuilder;
import org.opendaylight.yangtools.yang.common.Uint32;

public final class TapiUtils {

    private static final String LGX_PORT_NAME = "Some lgx-port-name";
    private static final String PORT_TYPE = "some port type";
    private static final String LGX_DEVICE_NAME = "Some lgx-device-name";
    private static final String PORT_RACK_VALUE = "000000.00";

    private TapiUtils() {
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
        ServiceAEnd serviceAEnd = new ServiceAEndBuilder().setClli(clli).setNodeId(new NodeIdType(nodeid)).setOpticType(
            OpticTypes.Gray).setServiceFormat(
                ServiceFormat.Ethernet).setServiceRate(Uint32.valueOf(100))
            .setTxDirection(
                new TxDirectionBuilder()
                    .setPort(new PortBuilder().setPortDeviceName(txPortDeviceName).setPortName(txPortName)
                        .setPortRack(PORT_RACK_VALUE).setPortShelf("00").setPortType(PORT_TYPE).build())
                    .setLgx(new LgxBuilder().setLgxDeviceName(LGX_DEVICE_NAME).setLgxPortName(
                        LGX_PORT_NAME).setLgxPortRack(PORT_RACK_VALUE).setLgxPortShelf("00").build())
                    .build())
            .setRxDirection(
                new RxDirectionBuilder()
                    .setPort(new PortBuilder().setPortDeviceName(rxPortDeviceName).setPortName(rxPortName)
                        .setPortRack(PORT_RACK_VALUE).setPortShelf("00").setPortType(PORT_TYPE).build())
                    .setLgx(new LgxBuilder().setLgxDeviceName(LGX_DEVICE_NAME).setLgxPortName(
                        LGX_PORT_NAME).setLgxPortRack(PORT_RACK_VALUE).setLgxPortShelf("00").build()).build())
            .build();
        return serviceAEnd;
    }

    public static ServiceZEnd buildServiceZEnd(String nodeid, String clli, String txPortDeviceName,
        String txPortName, String rxPortDeviceName, String rxPortName) {
        ServiceZEnd serviceZEnd = new ServiceZEndBuilder().setClli(clli).setNodeId(new NodeIdType(nodeid)).setOpticType(
            OpticTypes.Gray).setServiceFormat(
                ServiceFormat.Ethernet).setServiceRate(Uint32.valueOf(100))
            .setTxDirection(
                new TxDirectionBuilder()
                    .setPort(new PortBuilder().setPortDeviceName(txPortDeviceName).setPortName(txPortName)
                        .setPortRack(PORT_RACK_VALUE).setPortShelf("00").setPortType(PORT_TYPE).build())
                    .setLgx(new LgxBuilder().setLgxDeviceName(LGX_DEVICE_NAME).setLgxPortName(
                        LGX_PORT_NAME).setLgxPortRack(PORT_RACK_VALUE).setLgxPortShelf("00").build())
                    .build())
            .setRxDirection(
                new RxDirectionBuilder()
                    .setPort(new PortBuilder().setPortDeviceName(rxPortDeviceName).setPortName(rxPortName)
                        .setPortRack(PORT_RACK_VALUE).setPortShelf("00").setPortType(PORT_TYPE).build())
                    .setLgx(new LgxBuilder().setLgxDeviceName(LGX_DEVICE_NAME).setLgxPortName(
                        LGX_PORT_NAME).setLgxPortRack(PORT_RACK_VALUE).setLgxPortShelf("00").build()).build())
            .build();
        return serviceZEnd;
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
}
