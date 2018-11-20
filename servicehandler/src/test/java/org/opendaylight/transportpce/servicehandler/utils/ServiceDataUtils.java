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

import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev171017.ServicePathRpcResult;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev171017.ServicePathRpcResultBuilder;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev171017.service.path.rpc.result.PathDescription;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev171017.service.path.rpc.result.PathDescriptionBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev161014.ConnectionType;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev161014.RpcActions;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev161014.ServiceFormat;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev161014.sdnc.request.header.SdncRequestHeaderBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev161014.service.endpoint.RxDirection;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev161014.service.endpoint.TxDirection;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev161014.service.lgx.LgxBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev161014.service.port.PortBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev161014.ServiceCreateInput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev161014.ServiceCreateInputBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev161014.ServiceDeleteInput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev161014.ServiceDeleteInputBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev161014.ServiceRerouteInput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev161014.ServiceRerouteInputBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev161014.TempServiceCreateInput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev161014.TempServiceCreateInputBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev161014.TempServiceDeleteInput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev161014.TempServiceDeleteInputBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev161014.service.delete.input.ServiceDeleteReqInfo;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev161014.service.delete.input.ServiceDeleteReqInfoBuilder;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev171017.path.description.AToZDirection;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev171017.path.description.AToZDirectionBuilder;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev171017.path.description.ZToADirection;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev171017.path.description.ZToADirectionBuilder;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.service.types.rev171016.RpcStatusEx;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.service.types.rev171016.ServicePathNotificationTypes;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.DateAndTime;

public final class ServiceDataUtils {

    public static ServiceCreateInput buildServiceCreateInput() {

        ServiceCreateInputBuilder builtInput = new ServiceCreateInputBuilder();
        org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev161014.service.create.input
            .ServiceAEnd serviceAEnd = getServiceAEndBuild()
            .build();
        org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev161014.service.create.input
            .ServiceZEnd serviceZEnd = new org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev161014.service
            .create.input.ServiceZEndBuilder()
            .setClli("clli").setServiceFormat(ServiceFormat.OC).setServiceRate((long) 1).setNodeId("XPONDER-3-2")
            .setTxDirection(getTxDirection())
            .setRxDirection(getRxDirection())
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

    public static TempServiceCreateInput buildTempServiceCreateInput() {

        TempServiceCreateInputBuilder builtInput = new TempServiceCreateInputBuilder();
        org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev161014.temp.service.create.input
            .ServiceAEnd serviceAEnd = new org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev161014
            .temp.service.create.input.ServiceAEndBuilder()
            .setClli("clli").setServiceFormat(ServiceFormat.OC).setServiceRate((long) 1).setNodeId("XPONDER-1-2")
            .setTxDirection(getTxDirection())
            .setRxDirection(getRxDirection())
            .build();
        org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev161014.temp.service.create.input
            .ServiceZEnd serviceZEnd = new org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev161014.temp
            .service.create.input.ServiceZEndBuilder()
            .setClli("clli").setServiceFormat(ServiceFormat.OC).setServiceRate((long) 1).setNodeId("XPONDER-3-2")
            .setTxDirection(getTxDirection())
            .setRxDirection(getRxDirection())
            .build();

        builtInput.setCommonId("commonId");
        builtInput.setConnectionType(ConnectionType.Service);
        builtInput.setCustomer("Customer");
        builtInput.setServiceAEnd(serviceAEnd);
        builtInput.setServiceZEnd(serviceZEnd);
        builtInput.setSdncRequestHeader(new SdncRequestHeaderBuilder().setRequestId("request 1")
                .setRpcAction(RpcActions.TempServiceCreate).setNotificationUrl("notification url").build());

        return builtInput.build();
    }

    public static org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev161014.service.create.input
        .ServiceAEndBuilder getServiceAEndBuild() {
        return new org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev161014.service.create.input
            .ServiceAEndBuilder()
            .setClli("clli").setServiceFormat(ServiceFormat.OC).setServiceRate((long) 1).setNodeId("XPONDER-1-2")
            .setTxDirection(getTxDirection())
            .setRxDirection(getRxDirection());
    }

    public static org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev161014.service.create.input
        .ServiceZEndBuilder getServiceZEndBuild() {
        return new org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev161014.service.create.input
            .ServiceZEndBuilder()
            .setClli("clli").setServiceFormat(ServiceFormat.OC).setServiceRate((long) 1).setNodeId("XPONDER-1-2")
            .setTxDirection(getTxDirection())
            .setRxDirection(getRxDirection());
    }

    private static TxDirection getTxDirection() {
        return new org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev161014.service
                .endpoint.TxDirectionBuilder().setPort(new PortBuilder().setPortDeviceName("device name")
                        .setPortName("port name").setPortRack("port rack").setPortShelf("port shelf")
                        .setPortSlot("port slot").setPortSubSlot("port subslot").setPortType("port type").build())
                        .setLgx(new LgxBuilder().setLgxDeviceName("lgx device name").setLgxPortName("lgx port name")
                                .setLgxPortRack("lgx port rack").setLgxPortShelf("lgx port shelf").build())
                        .build();
    }

    private static RxDirection getRxDirection() {
        return new org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev161014.service
                .endpoint.RxDirectionBuilder()
                .setPort(new PortBuilder().setPortDeviceName("device name").setPortName("port name")
                    .setPortRack("port rack").setPortShelf("port shelf").setPortSlot("port slot")
                    .setPortSubSlot("port subslot").setPortType("port type").build())
                .setLgx(new LgxBuilder().setLgxDeviceName("lgx device name")
                    .setLgxPortName("lgx port name").setLgxPortRack("lgx port rack")
                    .setLgxPortShelf("lgx port shelf").build())
                        .build();
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

    public static TempServiceDeleteInput buildTempServiceDeleteInput() {
        TempServiceDeleteInputBuilder deleteInputBldr = new TempServiceDeleteInputBuilder();
        deleteInputBldr.setCommonId("service 1");
        return deleteInputBldr.build();
    }

    public static ServiceRerouteInput buildServiceRerouteInput() {
        ServiceRerouteInputBuilder builder = new ServiceRerouteInputBuilder();
        builder.setServiceName("service 1");
        return builder.build();
    }

    public static ServicePathRpcResult buildServicePathRpcResult() {
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssxxx");
        OffsetDateTime offsetDateTime = OffsetDateTime.now(ZoneOffset.UTC);
        DateAndTime datetime = new DateAndTime(dtf.format(offsetDateTime));
        ServicePathRpcResultBuilder builder = new ServicePathRpcResultBuilder();
        builder.setActualDate(datetime).setNotificationType(ServicePathNotificationTypes.PathComputationRequest)
            .setPathDescription(createPathDescription(0,1, 0, 1))
            .setServiceName("service 1")
            .setStatus(RpcStatusEx.Successful).setStatusMessage("success");
        return builder.build();
    }

    public static ServicePathRpcResult buildFailedServicePathRpcResult() {
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssxxx");
        OffsetDateTime offsetDateTime = OffsetDateTime.now(ZoneOffset.UTC);
        DateAndTime datetime = new DateAndTime(dtf.format(offsetDateTime));
        ServicePathRpcResultBuilder builder = new ServicePathRpcResultBuilder();
        builder.setActualDate(datetime).setNotificationType(ServicePathNotificationTypes.ServiceImplementationRequest)
            .setPathDescription(createPathDescription(0,1, 0, 1))
            .setServiceName("service 1")
            .setStatus(RpcStatusEx.Failed).setStatusMessage("success");
        return builder.build();
    }

    private static PathDescription createPathDescription(long azRate, long azWaveLength, long zaRate,
        long zaWaveLength) {
        AToZDirection atozDirection = new AToZDirectionBuilder()
            .setRate(azRate)
            .setAToZWavelengthNumber(azWaveLength)
            .setAToZ(null)
            .build();
        ZToADirection ztoaDirection = new ZToADirectionBuilder()
            .setRate(zaRate)
            .setZToAWavelengthNumber(zaWaveLength)
            .setZToA(null)
            .build();
        PathDescription pathDescription = new PathDescriptionBuilder()
            .setAToZDirection(atozDirection)
            .setZToADirection(ztoaDirection)
            .build();
        return pathDescription;
    }

    public static org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev161014.service.reconfigure.input
        .ServiceAEndBuilder getServiceAEndBuildReconfigure() {
        return new org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev161014.service.reconfigure.input
            .ServiceAEndBuilder()
            .setClli("clli").setServiceFormat(ServiceFormat.OC).setServiceRate((long) 1).setNodeId("XPONDER-1-2")
            .setTxDirection(getTxDirection())
            .setRxDirection(getRxDirection());
    }

    public static org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev161014.service.reconfigure.input
        .ServiceZEndBuilder getServiceZEndBuildReconfigure() {
        return new org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev161014.service.reconfigure.input
            .ServiceZEndBuilder()
            .setClli("clli").setServiceFormat(ServiceFormat.OC).setServiceRate((long) 1).setNodeId("XPONDER-1-2")
            .setTxDirection(getTxDirection())
            .setRxDirection(getRxDirection());
    }

    private ServiceDataUtils() {
    }

}
