/*
 * Copyright © 2018 Orange, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.servicehandler.utils;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev161014.ConnectionType;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev161014.RpcActions;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev161014.ServiceFormat;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev161014.sdnc.request.header.SdncRequestHeaderBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev161014.service.lgx.LgxBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev161014.service.port.PortBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev161014.ServiceCreateInput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev161014.ServiceCreateInputBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev161014.ServiceDeleteInput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev161014.ServiceDeleteInputBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev161014.ServiceRerouteInput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev161014.ServiceRerouteInputBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev161014.service.delete.input.ServiceDeleteReqInfo;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev161014.service.delete.input.ServiceDeleteReqInfoBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.DateAndTime;

public class ServiceDataUtils {

    public static ServiceCreateInput buildServiceCreateInput() {

        ServiceCreateInputBuilder builtInput = new ServiceCreateInputBuilder();
        org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev161014.service.create.input
            .ServiceAEnd serviceAEnd = getServiceAEndBuild()
            .build();
        org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev161014.service.create.input
            .ServiceZEnd serviceZEnd = new org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev161014.service
            .create.input.ServiceZEndBuilder()
            .setClli("clli").setServiceFormat(ServiceFormat.OC).setServiceRate((long) 1).setNodeId("XPONDER-3-2")
            .setTxDirection(
                new org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev161014.service
                    .endpoint.TxDirectionBuilder()
                    .setPort(new PortBuilder().setPortDeviceName("device name").setPortName("port name")
                        .setPortRack("port rack").setPortShelf("port shelf").setPortSlot("port slot")
                        .setPortSubSlot("port subslot").setPortType("port type").build())
                    .setLgx(new LgxBuilder().setLgxDeviceName("lgx device name")
                        .setLgxPortName("lgx port name").setLgxPortRack("lgx port rack")
                        .setLgxPortShelf("lgx port shelf").build())
                    .build())
            .setRxDirection(
                new org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev161014.service
                    .endpoint.RxDirectionBuilder()
                    .setPort(new PortBuilder().setPortDeviceName("device name").setPortName("port name")
                        .setPortRack("port rack").setPortShelf("port shelf").setPortSlot("port slot")
                        .setPortSubSlot("port subslot").setPortType("port type").build())
                    .setLgx(new LgxBuilder().setLgxDeviceName("lgx device name")
                        .setLgxPortName("lgx port name").setLgxPortRack("lgx port rack")
                        .setLgxPortShelf("lgx port shelf").build())
                    .build())
            .build();

        builtInput.setCommonId("commonId");
        builtInput.setConnectionType(ConnectionType.Service);
        builtInput.setCustomer("Customer");
        builtInput.setServiceName("service 1");
        builtInput.setServiceAEnd(serviceAEnd);
        builtInput.setServiceZEnd(serviceZEnd);
        builtInput.setSdncRequestHeader(new SdncRequestHeaderBuilder().setRequestId("request 1")
            .setRpcAction(RpcActions.ServiceCreate).setNotificationUrl("notification url").build());

        return builtInput.build();
    }

    public static org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev161014.service.create.input
        .ServiceAEndBuilder getServiceAEndBuild() {
        return new org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev161014.service.create.input
            .ServiceAEndBuilder()
            .setClli("clli").setServiceFormat(ServiceFormat.OC).setServiceRate((long) 1).setNodeId("XPONDER-1-2")
            .setTxDirection(
                new org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev161014.service
                    .endpoint.TxDirectionBuilder()
                    .setPort(new PortBuilder().setPortDeviceName("device name").setPortName("port name")
                        .setPortRack("port rack").setPortShelf("port shelf").setPortSlot("port slot")
                        .setPortSubSlot("port subslot").setPortType("port type").build())
                    .setLgx(new LgxBuilder().setLgxDeviceName("lgx device name")
                        .setLgxPortName("lgx port name").setLgxPortRack("lgx port rack")
                        .setLgxPortShelf("lgx port shelf").build())
                    .build())
            .setRxDirection(
                new org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev161014.service
                    .endpoint.RxDirectionBuilder()
                    .setPort(new PortBuilder().setPortDeviceName("device name").setPortName("port name")
                        .setPortRack("port rack").setPortShelf("port shelf").setPortSlot("port slot")
                        .setPortSubSlot("port subslot").setPortType("port type").build())
                    .setLgx(new LgxBuilder().setLgxDeviceName("lgx device name")
                        .setLgxPortName("lgx port name").setLgxPortRack("lgx port rack")
                        .setLgxPortShelf("lgx port shelf").build())
                    .build());
    }

    public static org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev161014.service.create.input
        .ServiceZEndBuilder getServiceZEndBuild() {
        return new org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev161014.service.create.input
            .ServiceZEndBuilder()
            .setClli("clli").setServiceFormat(ServiceFormat.OC).setServiceRate((long) 1).setNodeId("XPONDER-1-2")
            .setTxDirection(
                new org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev161014.service
                    .endpoint.TxDirectionBuilder()
                    .setPort(new PortBuilder().setPortDeviceName("device name").setPortName("port name")
                        .setPortRack("port rack").setPortShelf("port shelf").setPortSlot("port slot")
                        .setPortSubSlot("port subslot").setPortType("port type").build())
                    .setLgx(new LgxBuilder().setLgxDeviceName("lgx device name")
                        .setLgxPortName("lgx port name").setLgxPortRack("lgx port rack")
                        .setLgxPortShelf("lgx port shelf").build())
                    .build())
            .setRxDirection(
                new org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev161014.service
                    .endpoint.RxDirectionBuilder()
                    .setPort(new PortBuilder().setPortDeviceName("device name").setPortName("port name")
                        .setPortRack("port rack").setPortShelf("port shelf").setPortSlot("port slot")
                        .setPortSubSlot("port subslot").setPortType("port type").build())
                    .setLgx(new LgxBuilder().setLgxDeviceName("lgx device name")
                        .setLgxPortName("lgx port name").setLgxPortRack("lgx port rack")
                        .setLgxPortShelf("lgx port shelf").build())
                    .build());
    }

    public static ServiceDeleteInput buildServiceDeleteInput() {
        ServiceDeleteInputBuilder deleteInputBldr = new ServiceDeleteInputBuilder();
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssxxx");
        OffsetDateTime offsetDateTime = OffsetDateTime.now(ZoneOffset.UTC);
        DateAndTime datetime = new DateAndTime(dtf.format(offsetDateTime));
        deleteInputBldr.setServiceDeleteReqInfo(new ServiceDeleteReqInfoBuilder().setServiceName("service 1")
            .setDueDate(datetime).setTailRetention(ServiceDeleteReqInfo.TailRetention.No).build());
        SdncRequestHeaderBuilder sdncBuilder = new SdncRequestHeaderBuilder();
        sdncBuilder.setNotificationUrl("notification url");
        sdncBuilder.setRequestId("request 1");
        sdncBuilder.setRequestSystemId("request system 1");
        sdncBuilder.setRpcAction(RpcActions.ServiceDelete);
        deleteInputBldr.setSdncRequestHeader(sdncBuilder.build());
        return deleteInputBldr.build();
    }

    public static ServiceRerouteInput buildServiceRerouteInput() {
        ServiceRerouteInputBuilder builder = new ServiceRerouteInputBuilder();
        builder.setServiceName("service 1");
        return builder.build();
    }
}
