/*
 * Copyright © 2018 Orange, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.servicehandler.utils;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import org.opendaylight.transportpce.servicehandler.ModelMappingUtils;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev210701.PathComputationRequestInput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev210701.PathComputationRequestInputBuilder;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev210701.ServicePathRpcResult;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev210701.ServicePathRpcResultBuilder;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev210701.service.path.rpc.result.PathDescription;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev210701.service.path.rpc.result.PathDescriptionBuilder;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.renderer.rev210915.RendererRpcResultSp;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.renderer.rev210915.RendererRpcResultSpBuilder;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.servicehandler.rev201125.ServiceRpcResultSh;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.servicehandler.rev201125.ServiceRpcResultShBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.node.types.rev210528.NodeIdType;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev211210.ConnectionType;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev211210.RpcActions;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev211210.ServiceNotificationTypes;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev211210.sdnc.request.header.SdncRequestHeader;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev211210.sdnc.request.header.SdncRequestHeaderBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev211210.service.endpoint.RxDirection;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev211210.service.endpoint.RxDirectionKey;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev211210.service.endpoint.TxDirection;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev211210.service.endpoint.TxDirectionKey;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev211210.service.lgx.LgxBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev211210.service.port.PortBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.routing.constraints.rev211210.routing.constraints.HardConstraintsBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.routing.constraints.rev211210.routing.constraints.SoftConstraintsBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.format.rev191129.ServiceFormat;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev211210.ServiceCreateInput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev211210.ServiceCreateInputBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev211210.ServiceDeleteInput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev211210.ServiceDeleteInputBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev211210.ServiceFeasibilityCheckInput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev211210.ServiceFeasibilityCheckInputBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev211210.ServiceReconfigureInput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev211210.ServiceReconfigureInputBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev211210.ServiceRerouteInput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev211210.ServiceRerouteInputBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev211210.ServiceRestorationInput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev211210.ServiceRestorationInput.Option;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev211210.ServiceRestorationInputBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev211210.TempServiceCreateInput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev211210.TempServiceCreateInputBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev211210.TempServiceDeleteInput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev211210.TempServiceDeleteInputBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev211210.service.delete.input.ServiceDeleteReqInfo;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev211210.service.delete.input.ServiceDeleteReqInfoBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev211210.service.feasibility.check.inputs.ServiceAEnd;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev211210.service.feasibility.check.inputs.ServiceAEndBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev211210.service.feasibility.check.inputs.ServiceZEnd;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev211210.service.feasibility.check.inputs.ServiceZEndBuilder;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev210705.path.description.AToZDirection;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev210705.path.description.AToZDirectionBuilder;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev210705.path.description.ZToADirection;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev210705.path.description.ZToADirectionBuilder;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.service.types.rev200128.PceMetric;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.service.types.rev200128.RpcStatusEx;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.service.types.rev200128.ServicePathNotificationTypes;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.service.types.rev200128.service.handler.header.ServiceHandlerHeaderBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.DateAndTime;
import org.opendaylight.yangtools.yang.common.Uint32;
import org.opendaylight.yangtools.yang.common.Uint8;

public final class ServiceDataUtils {

    public static ServiceCreateInput buildServiceCreateInput() {

        org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev211210.service.create.input
            .ServiceAEnd serviceAEnd = getServiceAEndBuild()
            .build();
        org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev211210.service.create.input
            .ServiceZEnd serviceZEnd = new org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev211210.service
            .create.input.ServiceZEndBuilder()
            .setClli("clli").setServiceFormat(ServiceFormat.OC).setServiceRate(Uint32.valueOf(1))
            .setNodeId(new NodeIdType("XPONDER-3-2"))
            .setTxDirection(Map.of(new TxDirectionKey(getTxDirection().key()),getTxDirection()))
            .setRxDirection(Map.of(new RxDirectionKey(getRxDirection().key()), getRxDirection()))
            .build();

        ServiceCreateInputBuilder builtInput = new ServiceCreateInputBuilder()
            .setCommonId("commonId")
            .setConnectionType(ConnectionType.Service)
            .setCustomer("Customer")
            .setServiceName("service 1")
            .setServiceAEnd(serviceAEnd)
            .setServiceZEnd(serviceZEnd)
            .setSdncRequestHeader(new SdncRequestHeaderBuilder().setRequestId("request 1")
                .setRpcAction(RpcActions.ServiceCreate).setNotificationUrl("notification url").build());

        return builtInput.build();
    }

    public static ServiceCreateInput buildServiceCreateInputWithHardConstraints() {

        org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev211210.service.create.input
                .ServiceAEnd serviceAEnd = getServiceAEndBuild()
                .build();
        org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev211210.service.create.input
                .ServiceZEnd serviceZEnd = new org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev211210.service
                .create.input.ServiceZEndBuilder()
                .setClli("clli").setServiceFormat(ServiceFormat.OC).setServiceRate(Uint32.valueOf(1))
                .setNodeId(new NodeIdType("XPONDER-3-2"))
                .setTxDirection(Map.of(new TxDirectionKey(getTxDirection().key()),getTxDirection()))
                .setRxDirection(Map.of(new RxDirectionKey(getRxDirection().key()), getRxDirection()))
                .build();

        ServiceCreateInputBuilder builtInput = new ServiceCreateInputBuilder()
            .setCommonId("commonId")
            .setConnectionType(ConnectionType.Service)
            .setCustomer("Customer")
            .setServiceName("service 1")
            .setServiceAEnd(serviceAEnd)
            .setServiceZEnd(serviceZEnd)
            .setSdncRequestHeader(new SdncRequestHeaderBuilder().setRequestId("request 1")
                .setRpcAction(RpcActions.ServiceCreate).setNotificationUrl("notification url").build())
            .setHardConstraints(new HardConstraintsBuilder().build());

        return builtInput.build();
    }

    public static ServiceCreateInput buildServiceCreateInputWithSoftConstraints() {

        org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev211210.service.create.input
                .ServiceAEnd serviceAEnd = getServiceAEndBuild()
                .build();
        org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev211210.service.create.input
                .ServiceZEnd serviceZEnd = new org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev211210.service
                .create.input.ServiceZEndBuilder()
                .setClli("clli").setServiceFormat(ServiceFormat.OC).setServiceRate(Uint32.valueOf(1))
                .setNodeId(new NodeIdType("XPONDER-3-2"))
                .setTxDirection(Map.of(new TxDirectionKey(getTxDirection().key()),getTxDirection()))
                .setRxDirection(Map.of(new RxDirectionKey(getRxDirection().key()), getRxDirection()))
                .build();

        ServiceCreateInputBuilder builtInput = new ServiceCreateInputBuilder()
            .setCommonId("commonId")
            .setConnectionType(ConnectionType.Service)
            .setCustomer("Customer")
            .setServiceName("service 1")
            .setServiceAEnd(serviceAEnd)
            .setServiceZEnd(serviceZEnd)
            .setSdncRequestHeader(new SdncRequestHeaderBuilder().setRequestId("request 1")
                .setRpcAction(RpcActions.ServiceCreate).setNotificationUrl("notification url").build())
            .setSoftConstraints(new SoftConstraintsBuilder().build());

        return builtInput.build();
    }

    public static PathComputationRequestInput createPceRequestInput(ServiceCreateInput input) {
        ServiceHandlerHeaderBuilder serviceHandlerHeader = new ServiceHandlerHeaderBuilder();
        SdncRequestHeader serviceHandler = input.getSdncRequestHeader();
        if (serviceHandler != null) {
            serviceHandlerHeader.setRequestId(serviceHandler.getRequestId());
        }
        return new PathComputationRequestInputBuilder()
            .setServiceName(input.getServiceName())
            .setResourceReserve(true)
            .setServiceHandlerHeader(serviceHandlerHeader.build())
            .setHardConstraints(input.getHardConstraints())
            .setSoftConstraints(input.getSoftConstraints())
            .setPceRoutingMetric(PceMetric.TEMetric)
            .setServiceAEnd(ModelMappingUtils.createServiceAEnd(input.getServiceAEnd()))
            .setServiceZEnd(ModelMappingUtils.createServiceZEnd(input.getServiceZEnd()))
            .build();
    }

    public static TempServiceCreateInput buildTempServiceCreateInput() {

        org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev211210.temp.service.create.input
            .ServiceAEnd serviceAEnd = new org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev211210.temp
                .service.create.input.ServiceAEndBuilder()
            .setClli("clli").setServiceFormat(ServiceFormat.OC).setServiceRate(Uint32.valueOf(1))
            .setNodeId(new NodeIdType("XPONDER-1-2"))
            .setTxDirection(Map.of(new TxDirectionKey(getTxDirection().key()),getTxDirection()))
            .setRxDirection(Map.of(new RxDirectionKey(getRxDirection().key()), getRxDirection()))
            .build();
        org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev211210.temp.service.create.input
            .ServiceZEnd serviceZEnd = new org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev211210.temp
            .service.create.input.ServiceZEndBuilder()
            .setClli("clli").setServiceFormat(ServiceFormat.OC).setServiceRate(Uint32.valueOf(1))
            .setNodeId(new NodeIdType("XPONDER-3-2"))
            .setTxDirection(Map.of(new TxDirectionKey(getTxDirection().key()),getTxDirection()))
            .setRxDirection(Map.of(new RxDirectionKey(getRxDirection().key()), getRxDirection()))
            .build();

        TempServiceCreateInputBuilder builtInput = new TempServiceCreateInputBuilder()
            .setCommonId("commonId")
            .setConnectionType(ConnectionType.Service)
            .setCustomer("Customer")
            .setServiceAEnd(serviceAEnd)
            .setServiceZEnd(serviceZEnd)
            .setSdncRequestHeader(new SdncRequestHeaderBuilder().setRequestId("request 1")
                .setRpcAction(RpcActions.TempServiceCreate).setNotificationUrl("notification url").build());

        return builtInput.build();
    }

    public static ServiceFeasibilityCheckInput buildServiceFeasibilityCheckInput() {

        ServiceAEnd serviceAEnd = new ServiceAEndBuilder()
                    .setClli("clli").setServiceFormat(ServiceFormat.OC).setServiceRate(Uint32.valueOf(1))
                    .setNodeId(new NodeIdType("XPONDER-1-2"))
                    .setTxDirection(Map.of(new TxDirectionKey(getTxDirection().key()),getTxDirection()))
                    .setRxDirection(Map.of(new RxDirectionKey(getRxDirection().key()), getRxDirection())).build();
        ServiceZEnd serviceZEnd = new ServiceZEndBuilder()
                    .setClli("clli").setServiceFormat(ServiceFormat.OC).setServiceRate(Uint32.valueOf(1))
                    .setNodeId(new NodeIdType("XPONDER-3-2"))
                    .setTxDirection(Map.of(new TxDirectionKey(getTxDirection().key()),getTxDirection()))
                    .setRxDirection(Map.of(new RxDirectionKey(getRxDirection().key()), getRxDirection())).build();

        ServiceFeasibilityCheckInputBuilder builtInput = new ServiceFeasibilityCheckInputBuilder()
            .setCommonId("commonId")
            .setConnectionType(ConnectionType.Service)
            .setCustomer("Customer")
            .setServiceAEnd(serviceAEnd)
            .setServiceZEnd(serviceZEnd)
            .setSdncRequestHeader(new SdncRequestHeaderBuilder().setRequestId("request 1")
                .setRpcAction(RpcActions.ServiceFeasibilityCheck).setNotificationUrl("notification url").build());
        return builtInput.build();
    }

    public static org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev211210.service.create.input
        .ServiceAEndBuilder getServiceAEndBuild() {
        return new org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev211210.service.create.input
            .ServiceAEndBuilder()
            .setClli("clli").setServiceFormat(ServiceFormat.OC).setServiceRate(Uint32.valueOf(1))
            .setNodeId(new NodeIdType("XPONDER-1-2"))
            .setTxDirection(Map.of(new TxDirectionKey(getTxDirection().key()),getTxDirection()))
            .setRxDirection(Map.of(new RxDirectionKey(getRxDirection().key()), getRxDirection()));
    }

    public static org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev211210.service.create.input
        .ServiceZEndBuilder getServiceZEndBuild() {
        return new org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev211210.service.create.input
            .ServiceZEndBuilder()
            .setClli("clli").setServiceFormat(ServiceFormat.OC).setServiceRate(Uint32.valueOf(1))
            .setNodeId(new NodeIdType("XPONDER-1-2"))
            .setTxDirection(Map.of(new TxDirectionKey(getTxDirection().key()),getTxDirection()))
            .setRxDirection(Map.of(new RxDirectionKey(getRxDirection().key()), getRxDirection()));
    }

    private static TxDirection getTxDirection() {
        return new org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev211210.service
                .endpoint.TxDirectionBuilder().setPort(new PortBuilder().setPortDeviceName("device name")
                        .setPortName("port name").setPortRack("port rack").setPortShelf("port shelf")
                        .setPortSlot("port slot").setPortSubSlot("port subslot").setPortType("port type").build())
                        .setLgx(new LgxBuilder().setLgxDeviceName("lgx device name").setLgxPortName("lgx port name")
                                .setLgxPortRack("lgx port rack").setLgxPortShelf("lgx port shelf").build())
                        .setIndex(Uint8.ZERO)
                        .build();
    }

    private static RxDirection getRxDirection() {
        return new org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev211210.service
                .endpoint.RxDirectionBuilder()
                    .setPort(new PortBuilder().setPortDeviceName("device name").setPortName("port name")
                        .setPortRack("port rack").setPortShelf("port shelf").setPortSlot("port slot")
                        .setPortSubSlot("port subslot").setPortType("port type").build())
                    .setLgx(new LgxBuilder().setLgxDeviceName("lgx device name")
                        .setLgxPortName("lgx port name").setLgxPortRack("lgx port rack")
                        .setLgxPortShelf("lgx port shelf").build())
                    .setIndex(Uint8.ZERO)
                    .build();
    }

    public static ServiceDeleteInput buildServiceDeleteInput() {

        ServiceDeleteInputBuilder deleteInputBldr = new ServiceDeleteInputBuilder();
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssxxx");
        OffsetDateTime offsetDateTime = OffsetDateTime.now(ZoneOffset.UTC);
        DateAndTime datetime = new DateAndTime(dtf.format(offsetDateTime));
        deleteInputBldr.setServiceDeleteReqInfo(new ServiceDeleteReqInfoBuilder().setServiceName("service 1")
            .setDueDate(datetime).setTailRetention(ServiceDeleteReqInfo.TailRetention.No).build());
        SdncRequestHeaderBuilder sdncBuilder = new SdncRequestHeaderBuilder()
            .setNotificationUrl("notification url")
            .setRequestId("request 1")
            .setRequestSystemId("request system 1")
            .setRpcAction(RpcActions.ServiceDelete);
        deleteInputBldr.setSdncRequestHeader(sdncBuilder.build());
        return deleteInputBldr.build();
    }

    public static TempServiceDeleteInput buildTempServiceDeleteInput() {
        TempServiceDeleteInputBuilder deleteInputBldr = new TempServiceDeleteInputBuilder()
            .setCommonId("service 1");
        return deleteInputBldr.build();
    }

    public static TempServiceDeleteInput buildTempServiceDeleteInput(String commonId) {
        TempServiceDeleteInputBuilder deleteInputBldr = new TempServiceDeleteInputBuilder()
            .setCommonId(commonId);
        return deleteInputBldr.build();
    }

    public static ServiceRerouteInput buildServiceRerouteInput() {
        ServiceRerouteInputBuilder builder = new ServiceRerouteInputBuilder()
            .setServiceName("service 1");
        return builder.build();
    }

    public static ServiceRestorationInput buildServiceRestorationInput() {
        ServiceRestorationInputBuilder builder = new ServiceRestorationInputBuilder()
            .setServiceName("service 1")
            .setOption(Option.Permanent);
        return builder.build();
    }

    public static ServiceReconfigureInput buildServiceReconfigureInput() {

        org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev211210.service.reconfigure.input
            .ServiceAEnd serviceAEnd = new org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev211210.service
                .reconfigure.input.ServiceAEndBuilder()
                .setClli("clli").setServiceFormat(ServiceFormat.OC).setServiceRate(Uint32.valueOf(1))
                .setNodeId(new NodeIdType("XPONDER-1-2"))
                .setTxDirection(Map.of(new TxDirectionKey(getTxDirection().key()),getTxDirection()))
                .setRxDirection(Map.of(new RxDirectionKey(getRxDirection().key()), getRxDirection()))
                .build();
        org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev211210.service.reconfigure.input
            .ServiceZEnd serviceZEnd = new org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev211210.service
                .reconfigure.input.ServiceZEndBuilder()
                .setClli("clli").setServiceFormat(ServiceFormat.OC).setServiceRate(Uint32.valueOf(1))
                .setNodeId(new NodeIdType("XPONDER-3-2"))
                .setTxDirection(Map.of(new TxDirectionKey(getTxDirection().key()),getTxDirection()))
                .setRxDirection(Map.of(new RxDirectionKey(getRxDirection().key()), getRxDirection()))
                .build();

        ServiceReconfigureInputBuilder builtInput = new ServiceReconfigureInputBuilder()
            .setCommonId("commonId")
            .setConnectionType(ConnectionType.Service)
            .setCustomer("Customer")
            .setServiceName("service 1")
            .setServiceAEnd(serviceAEnd)
            .setServiceZEnd(serviceZEnd);
        return builtInput.build();
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

    public static ServicePathRpcResult buildServicePathRpcResult(
            ServicePathNotificationTypes servicePathNotificationTypes, String serviceName, RpcStatusEx rpcStatusEx,
            String message, Boolean pathDescription) {
        ServicePathRpcResultBuilder builder = new ServicePathRpcResultBuilder();
        builder.setNotificationType(servicePathNotificationTypes)
                .setServiceName(serviceName)
                .setStatus(rpcStatusEx).setStatusMessage(message);
        if (pathDescription) {
            builder.setPathDescription(createPathDescription(0L, 5L, 0L, 5L));
        }
        return builder.build();
    }

    public static ServicePathRpcResult buildFailedPceServicePathRpcResult() {
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssxxx");
        OffsetDateTime offsetDateTime = OffsetDateTime.now(ZoneOffset.UTC);
        DateAndTime datetime = new DateAndTime(dtf.format(offsetDateTime));
        ServicePathRpcResultBuilder builder = new ServicePathRpcResultBuilder();
        builder.setActualDate(datetime).setNotificationType(ServicePathNotificationTypes.PathComputationRequest)
                .setPathDescription(createPathDescription(0, 1, 0, 1)).setServiceName("service 1")
                .setStatus(RpcStatusEx.Failed).setStatusMessage("failed");
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
                .setStatus(RpcStatusEx.Failed).setStatusMessage("failed");
        return builder.build();
    }

    public static RendererRpcResultSp buildRendererRpcResultSp() {
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssxxx");
        OffsetDateTime offsetDateTime = OffsetDateTime.now(ZoneOffset.UTC);
        DateAndTime datetime = new DateAndTime(dtf.format(offsetDateTime));
        RendererRpcResultSpBuilder builder = new RendererRpcResultSpBuilder();
        builder.setActualDate(datetime).setNotificationType(ServicePathNotificationTypes.ServiceImplementationRequest)
                .setServiceName("service 1").setStatus(RpcStatusEx.Successful).setStatusMessage("success");
        return builder.build();
    }

    public static RendererRpcResultSp buildRendererRpcResultSp(
            ServicePathNotificationTypes servicePathNotificationTypes,
            String serviceName, RpcStatusEx rpcStatusEx, String message) {
        RendererRpcResultSpBuilder builder = new RendererRpcResultSpBuilder();
        builder.setNotificationType(servicePathNotificationTypes).setServiceName(serviceName)
                .setStatus(rpcStatusEx).setStatusMessage(message);
        return builder.build();
    }

    public static ServiceRpcResultSh buildServiceRpcResultSh(
            ServiceNotificationTypes serviceNotificationTypes,
            String serviceName,
            org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.service.types.rev200128.RpcStatusEx
                rpcStatusEx,
            String message) {
        ServiceRpcResultShBuilder builder = new ServiceRpcResultShBuilder();
        builder.setNotificationType(serviceNotificationTypes).setServiceName(serviceName)
                .setStatus(rpcStatusEx).setStatusMessage(message);
        return builder.build();
    }

    public static RendererRpcResultSp buildFailedRendererRpcResultSp() {
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssxxx");
        OffsetDateTime offsetDateTime = OffsetDateTime.now(ZoneOffset.UTC);
        DateAndTime datetime = new DateAndTime(dtf.format(offsetDateTime));
        RendererRpcResultSpBuilder builder = new RendererRpcResultSpBuilder();
        builder.setActualDate(datetime).setNotificationType(ServicePathNotificationTypes.ServiceImplementationRequest)
                .setServiceName("service 1").setStatus(RpcStatusEx.Failed).setStatusMessage("failed");
        return builder.build();
    }

    public static PathDescription createPathDescription(long azRate, long azWaveLength, long zaRate,
        long zaWaveLength) {
        AToZDirection atozDirection = new AToZDirectionBuilder()
            .setRate(Uint32.valueOf(azRate))
            .setAToZWavelengthNumber(Uint32.valueOf(azWaveLength))
            .build();
        ZToADirection ztoaDirection = new ZToADirectionBuilder()
            .setRate(Uint32.valueOf(zaRate))
            .setZToAWavelengthNumber(Uint32.valueOf(zaWaveLength))
            .build();
        PathDescription pathDescription = new PathDescriptionBuilder()
            .setAToZDirection(atozDirection)
            .setZToADirection(ztoaDirection)
            .build();
        return pathDescription;
    }

    public static org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev211210.service.reconfigure.input
        .ServiceAEndBuilder getServiceAEndBuildReconfigure() {
        return new org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev211210.service.reconfigure.input
            .ServiceAEndBuilder()
            .setClli("clli").setServiceFormat(ServiceFormat.OC).setServiceRate(Uint32.valueOf(1))
            .setNodeId(new NodeIdType("XPONDER-1-2"))
            .setTxDirection(Map.of(new TxDirectionKey(getTxDirection().key()),getTxDirection()))
            .setRxDirection(Map.of(new RxDirectionKey(getRxDirection().key()), getRxDirection()));
    }

    public static org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev211210.service.reconfigure.input
        .ServiceZEndBuilder getServiceZEndBuildReconfigure() {
        return new org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev211210.service.reconfigure.input
            .ServiceZEndBuilder()
            .setClli("clli").setServiceFormat(ServiceFormat.OC).setServiceRate(Uint32.valueOf(1))
            .setNodeId(new NodeIdType("XPONDER-1-2"))
            .setTxDirection(Map.of(new TxDirectionKey(getTxDirection().key()),getTxDirection()))
            .setRxDirection(Map.of(new RxDirectionKey(getRxDirection().key()), getRxDirection()));
    }

    public static <T> ListenableFuture<T> returnFuture(T output) {
        final ListeningExecutorService executor =
                MoreExecutors.listeningDecorator(Executors.newFixedThreadPool(1));
        return executor.submit(new Callable<T>() {
            @Override
            public T call() throws Exception {
                return output;
            }
        });
    }

    private ServiceDataUtils() {
    }

}
