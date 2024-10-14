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
import org.opendaylight.yang.gen.v1.http.com.smartoptics.openroadm.service.rev230907.ServiceAEnd2;
import org.opendaylight.yang.gen.v1.http.com.smartoptics.openroadm.service.rev230907.ServiceZEnd2;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev240205.PathComputationRequestInput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev240205.PathComputationRequestInputBuilder;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev240205.ServicePathRpcResult;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev240205.ServicePathRpcResultBuilder;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev240205.service.path.rpc.result.PathDescription;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev240205.service.path.rpc.result.PathDescriptionBuilder;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.renderer.rev210915.RendererRpcResultSp;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.renderer.rev210915.RendererRpcResultSpBuilder;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.servicehandler.rev201125.ServiceRpcResultSh;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.servicehandler.rev201125.ServiceRpcResultShBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.node.types.rev210528.NodeIdType;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev230526.ConnectionType;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev230526.RpcActions;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev230526.ServiceNotificationTypes;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev230526.sdnc.request.header.SdncRequestHeader;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev230526.sdnc.request.header.SdncRequestHeaderBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev230526.service.endpoint.RxDirection;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev230526.service.endpoint.RxDirectionKey;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev230526.service.endpoint.TxDirection;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev230526.service.endpoint.TxDirectionKey;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev230526.service.lgx.LgxBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev230526.service.port.PortBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.routing.constraints.rev221209.routing.constraints.HardConstraintsBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.routing.constraints.rev221209.routing.constraints.SoftConstraintsBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.format.rev191129.ServiceFormat;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev230526.ServiceCreateInput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev230526.ServiceCreateInputBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev230526.ServiceDeleteInput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev230526.ServiceDeleteInputBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev230526.ServiceFeasibilityCheckInput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev230526.ServiceFeasibilityCheckInputBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev230526.ServiceReconfigureInput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev230526.ServiceReconfigureInputBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev230526.ServiceRerouteInput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev230526.ServiceRerouteInputBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev230526.ServiceRestorationInput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev230526.ServiceRestorationInput.Option;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev230526.ServiceRestorationInputBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev230526.TempServiceCreateInput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev230526.TempServiceCreateInputBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev230526.TempServiceDeleteInput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev230526.TempServiceDeleteInputBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev230526.service.delete.input.ServiceDeleteReqInfo;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev230526.service.delete.input.ServiceDeleteReqInfoBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev230526.service.feasibility.check.inputs.ServiceAEndBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev230526.service.feasibility.check.inputs.ServiceZEndBuilder;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev230501.path.description.AToZDirectionBuilder;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev230501.path.description.ZToADirectionBuilder;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.service.types.rev220118.PceMetric;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.service.types.rev220118.RpcStatusEx;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.service.types.rev220118.ServicePathNotificationTypes;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.service.types.rev220118.service.handler.header.ServiceHandlerHeaderBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.DateAndTime;
import org.opendaylight.yangtools.yang.common.Uint32;
import org.opendaylight.yangtools.yang.common.Uint8;

public final class ServiceDataUtils {

    public static ServiceCreateInput buildServiceCreateInput() {
        return new ServiceCreateInputBuilder()
            .setCommonId("commonId")
            .setConnectionType(ConnectionType.Service)
            .setCustomer("Customer")
            .setServiceName("service 1")
            .setServiceAEnd(getServiceAEndBuild().build())
            .setServiceZEnd(
                new org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev230526
                        .service.create.input.ServiceZEndBuilder()
                    .setClli("clli").setServiceFormat(ServiceFormat.OC).setServiceRate(Uint32.valueOf(1))
                    .setNodeId(new NodeIdType("XPONDER-3-2"))
                    .setTxDirection(Map.of(new TxDirectionKey(getTxDirection().key()),getTxDirection()))
                    .setRxDirection(Map.of(new RxDirectionKey(getRxDirection().key()), getRxDirection()))
                    .build())
            .setSdncRequestHeader(
                new SdncRequestHeaderBuilder()
                    .setRequestId("request 1")
                    .setRpcAction(RpcActions.ServiceCreate)
                    .setNotificationUrl("notification url")
                    .build())
            .build();
    }

    public static ServiceCreateInput buildServiceCreateInputWithHardConstraints() {
        return new ServiceCreateInputBuilder()
            .setCommonId("commonId")
            .setConnectionType(ConnectionType.Service)
            .setCustomer("Customer")
            .setServiceName("service 1")
            .setServiceAEnd(getServiceAEndBuild().build())
            .setServiceZEnd(
                new org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev230526
                        .service.create.input.ServiceZEndBuilder()
                    .setClli("clli")
                    .setServiceFormat(ServiceFormat.OC)
                    .setServiceRate(Uint32.valueOf(1))
                    .setNodeId(new NodeIdType("XPONDER-3-2"))
                    .setTxDirection(Map.of(new TxDirectionKey(getTxDirection().key()),getTxDirection()))
                    .setRxDirection(Map.of(new RxDirectionKey(getRxDirection().key()), getRxDirection()))
                    .build())
            .setSdncRequestHeader(
                new SdncRequestHeaderBuilder()
                    .setRequestId("request 1")
                    .setRpcAction(RpcActions.ServiceCreate)
                    .setNotificationUrl("notification url")
                    .build())
            .setHardConstraints(new HardConstraintsBuilder().build())
            .build();
    }

    public static ServiceCreateInput buildServiceCreateInputWithSoftConstraints() {
        return new ServiceCreateInputBuilder()
            .setCommonId("commonId")
            .setConnectionType(ConnectionType.Service)
            .setCustomer("Customer")
            .setServiceName("service 1")
            .setServiceAEnd(getServiceAEndBuild().build())
            .setServiceZEnd(
                new org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev230526
                        .service.create.input.ServiceZEndBuilder()
                    .setClli("clli")
                    .setServiceFormat(ServiceFormat.OC)
                    .setServiceRate(Uint32.valueOf(1))
                    .setNodeId(new NodeIdType("XPONDER-3-2"))
                    .setTxDirection(Map.of(new TxDirectionKey(getTxDirection().key()),getTxDirection()))
                    .setRxDirection(Map.of(new RxDirectionKey(getRxDirection().key()), getRxDirection()))
                    .build())
            .setSdncRequestHeader(
                new SdncRequestHeaderBuilder()
                    .setRequestId("request 1")
                    .setRpcAction(RpcActions.ServiceCreate)
                    .setNotificationUrl("notification url")
                    .build())
            .setSoftConstraints(new SoftConstraintsBuilder().build())
            .build();
    }

    public static PathComputationRequestInput createPceRequestInput(ServiceCreateInput input) {
        SdncRequestHeader serviceHandler = input.getSdncRequestHeader();
        return new PathComputationRequestInputBuilder()
            .setServiceName(input.getServiceName())
            .setResourceReserve(true)
            .setServiceHandlerHeader(
                serviceHandler == null
                    ? new ServiceHandlerHeaderBuilder().build()
                    : new ServiceHandlerHeaderBuilder().setRequestId(serviceHandler.getRequestId()).build())
            .setHardConstraints(input.getHardConstraints())
            .setSoftConstraints(input.getSoftConstraints())
            .setPceRoutingMetric(PceMetric.TEMetric)
            .setServiceAEnd(ModelMappingUtils.createServiceAEnd(
                    input.getServiceAEnd(),
                    input.getServiceAEnd().augmentation(ServiceAEnd2.class)
            ))
            .setServiceZEnd(ModelMappingUtils.createServiceZEnd(
                    input.getServiceZEnd(),
                    input.getServiceZEnd().augmentation(ServiceZEnd2.class)
            ))
            .build();
    }

    public static TempServiceCreateInput buildTempServiceCreateInput() {
        return new TempServiceCreateInputBuilder()
            .setCommonId("commonId")
            .setConnectionType(ConnectionType.Service)
            .setCustomer("Customer")
            .setServiceAEnd(
                new org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev230526
                        .temp.service.create.input.ServiceAEndBuilder()
                    .setClli("clli").setServiceFormat(ServiceFormat.OC).setServiceRate(Uint32.valueOf(1))
                    .setNodeId(new NodeIdType("XPONDER-1-2"))
                    .setTxDirection(Map.of(new TxDirectionKey(getTxDirection().key()),getTxDirection()))
                    .setRxDirection(Map.of(new RxDirectionKey(getRxDirection().key()), getRxDirection()))
                    .build())
            .setServiceZEnd(
                new org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev230526
                        .temp.service.create.input.ServiceZEndBuilder()
                    .setClli("clli").setServiceFormat(ServiceFormat.OC).setServiceRate(Uint32.valueOf(1))
                    .setNodeId(new NodeIdType("XPONDER-3-2"))
                    .setTxDirection(Map.of(new TxDirectionKey(getTxDirection().key()),getTxDirection()))
                    .setRxDirection(Map.of(new RxDirectionKey(getRxDirection().key()), getRxDirection()))
                    .build())
            .setSdncRequestHeader(
                new SdncRequestHeaderBuilder()
                    .setRequestId("request 1")
                    .setRpcAction(RpcActions.TempServiceCreate)
                    .setNotificationUrl("notification url")
                    .build())
            .build();
    }

    public static ServiceFeasibilityCheckInput buildServiceFeasibilityCheckInput() {
        return new ServiceFeasibilityCheckInputBuilder()
            .setCommonId("commonId")
            .setConnectionType(ConnectionType.Service)
            .setCustomer("Customer")
            .setServiceAEnd(
                new ServiceAEndBuilder()
                    .setClli("clli").setServiceFormat(ServiceFormat.OC).setServiceRate(Uint32.valueOf(1))
                    .setNodeId(new NodeIdType("XPONDER-1-2"))
                    .setTxDirection(Map.of(new TxDirectionKey(getTxDirection().key()),getTxDirection()))
                    .setRxDirection(Map.of(new RxDirectionKey(getRxDirection().key()), getRxDirection()))
                    .build())
            .setServiceZEnd(
                new ServiceZEndBuilder()
                    .setClli("clli").setServiceFormat(ServiceFormat.OC).setServiceRate(Uint32.valueOf(1))
                    .setNodeId(new NodeIdType("XPONDER-3-2"))
                    .setTxDirection(Map.of(new TxDirectionKey(getTxDirection().key()),getTxDirection()))
                    .setRxDirection(Map.of(new RxDirectionKey(getRxDirection().key()), getRxDirection()))
                    .build())
            .setSdncRequestHeader(
                new SdncRequestHeaderBuilder()
                    .setRequestId("request 1")
                    .setRpcAction(RpcActions.ServiceFeasibilityCheck)
                    .setNotificationUrl("notification url")
                    .build())
            .build();
    }

    public static org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev230526
            .service.create.input.ServiceAEndBuilder getServiceAEndBuild() {
        return new org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev230526
                .service.create.input.ServiceAEndBuilder()
            .setClli("clli")
            .setServiceFormat(ServiceFormat.OC)
            .setServiceRate(Uint32.valueOf(1))
            .setNodeId(new NodeIdType("XPONDER-1-2"))
            .setTxDirection(Map.of(new TxDirectionKey(getTxDirection().key()),getTxDirection()))
            .setRxDirection(Map.of(new RxDirectionKey(getRxDirection().key()), getRxDirection()));
    }

    public static org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev230526
            .service.create.input.ServiceZEndBuilder getServiceZEndBuild() {
        return new org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev230526
                .service.create.input.ServiceZEndBuilder()
            .setClli("clli")
            .setServiceFormat(ServiceFormat.OC)
            .setServiceRate(Uint32.valueOf(1))
            .setNodeId(new NodeIdType("XPONDER-1-2"))
            .setTxDirection(Map.of(new TxDirectionKey(getTxDirection().key()),getTxDirection()))
            .setRxDirection(Map.of(new RxDirectionKey(getRxDirection().key()), getRxDirection()));
    }

    private static TxDirection getTxDirection() {
        return new org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev230526
                .service.endpoint.TxDirectionBuilder()
            .setPort(
                new PortBuilder()
                    .setPortDeviceName("device name")
                    .setPortName("port name")
                    .setPortRack("port rack")
                    .setPortShelf("port shelf")
                    .setPortSlot("port slot")
                    .setPortSubSlot("port subslot")
                    .setPortType("port type")
                    .build())
            .setLgx(
                new LgxBuilder()
                    .setLgxDeviceName("lgx device name")
                    .setLgxPortName("lgx port name")
                    .setLgxPortRack("lgx port rack")
                    .setLgxPortShelf("lgx port shelf")
                    .build())
            .setIndex(Uint8.ZERO)
            .build();
    }

    private static RxDirection getRxDirection() {
        return new org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev230526
                .service.endpoint.RxDirectionBuilder()
            .setPort(
                new PortBuilder()
                    .setPortDeviceName("device name")
                    .setPortName("port name")
                    .setPortRack("port rack")
                    .setPortShelf("port shelf")
                    .setPortSlot("port slot")
                    .setPortSubSlot("port subslot")
                    .setPortType("port type")
                    .build())
            .setLgx(
                new LgxBuilder()
                    .setLgxDeviceName("lgx device name")
                    .setLgxPortName("lgx port name")
                    .setLgxPortRack("lgx port rack")
                    .setLgxPortShelf("lgx port shelf")
                    .build())
            .setIndex(Uint8.ZERO)
            .build();
    }

    public static ServiceDeleteInput buildServiceDeleteInput() {
        return new ServiceDeleteInputBuilder()
            .setServiceDeleteReqInfo(
                new ServiceDeleteReqInfoBuilder().setServiceName("service 1")
                    .setDueDate(
                        new DateAndTime(
                            DateTimeFormatter
                                .ofPattern("yyyy-MM-dd'T'HH:mm:ssxxx")
                                .format(OffsetDateTime.now(ZoneOffset.UTC))))
                    .setTailRetention(ServiceDeleteReqInfo.TailRetention.No)
                    .build())
            .setSdncRequestHeader(
                new SdncRequestHeaderBuilder()
                    .setNotificationUrl("notification url")
                    .setRequestId("request 1")
                    .setRequestSystemId("request system 1")
                    .setRpcAction(RpcActions.ServiceDelete)
                    .build())
            .build();
    }

    public static TempServiceDeleteInput buildTempServiceDeleteInput() {
        return new TempServiceDeleteInputBuilder().setCommonId("service 1").build();
    }

    public static TempServiceDeleteInput buildTempServiceDeleteInput(String commonId) {
        return new TempServiceDeleteInputBuilder().setCommonId(commonId).build();
    }

    public static ServiceRerouteInput buildServiceRerouteInput() {
        return new ServiceRerouteInputBuilder().setServiceName("service 1").build();
    }

    public static ServiceRestorationInput buildServiceRestorationInput() {
        return new ServiceRestorationInputBuilder()
            .setServiceName("service 1")
            .setOption(Option.Permanent)
            .build();
    }

    public static ServiceReconfigureInput buildServiceReconfigureInput() {
        return new ServiceReconfigureInputBuilder()
            .setCommonId("commonId")
            .setConnectionType(ConnectionType.Service)
            .setCustomer("Customer")
            .setServiceName("service 1")
            .setServiceAEnd(
                new org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev230526
                        .service.reconfigure.input.ServiceAEndBuilder()
                    .setClli("clli")
                    .setServiceFormat(ServiceFormat.OC)
                    .setServiceRate(Uint32.valueOf(1))
                    .setNodeId(new NodeIdType("XPONDER-1-2"))
                    .setTxDirection(Map.of(new TxDirectionKey(getTxDirection().key()),getTxDirection()))
                    .setRxDirection(Map.of(new RxDirectionKey(getRxDirection().key()), getRxDirection()))
                    .build())
            .setServiceZEnd(
                new org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev230526
                        .service.reconfigure.input.ServiceZEndBuilder()
                    .setClli("clli")
                    .setServiceFormat(ServiceFormat.OC)
                    .setServiceRate(Uint32.valueOf(1))
                    .setNodeId(new NodeIdType("XPONDER-3-2"))
                    .setTxDirection(Map.of(new TxDirectionKey(getTxDirection().key()),getTxDirection()))
                    .setRxDirection(Map.of(new RxDirectionKey(getRxDirection().key()), getRxDirection()))
                    .build())
            .build();
    }

    public static ServicePathRpcResult buildServicePathRpcResult() {
        return new ServicePathRpcResultBuilder()
            .setActualDate(
                new DateAndTime(
                    DateTimeFormatter
                        .ofPattern("yyyy-MM-dd'T'HH:mm:ssxxx")
                        .format(OffsetDateTime.now(ZoneOffset.UTC))))
            .setNotificationType(ServicePathNotificationTypes.PathComputationRequest)
            .setPathDescription(createPathDescription(0,1, 0, 1))
            .setServiceName("service 1")
            .setStatus(RpcStatusEx.Successful).setStatusMessage("success")
            .build();
    }

    public static ServicePathRpcResult buildServicePathRpcResult(
            ServicePathNotificationTypes servicePathNotificationTypes, String serviceName, RpcStatusEx rpcStatusEx,
            String message, Boolean pathDescription) {
        ServicePathRpcResultBuilder builder = new ServicePathRpcResultBuilder()
            .setNotificationType(servicePathNotificationTypes)
            .setServiceName(serviceName)
            .setStatus(rpcStatusEx)
            .setStatusMessage(message);
        if (pathDescription) {
            builder.setPathDescription(createPathDescription(0L, 5L, 0L, 5L));
        }
        return builder.build();
    }

    public static ServicePathRpcResult buildFailedPceServicePathRpcResult() {
        return new ServicePathRpcResultBuilder()
            .setActualDate(
                new DateAndTime(
                    DateTimeFormatter
                        .ofPattern("yyyy-MM-dd'T'HH:mm:ssxxx")
                        .format(OffsetDateTime.now(ZoneOffset.UTC))))
            .setNotificationType(ServicePathNotificationTypes.PathComputationRequest)
            .setPathDescription(createPathDescription(0, 1, 0, 1))
            .setServiceName("service 1")
            .setStatus(RpcStatusEx.Failed).setStatusMessage("failed")
        .build();
    }

    public static ServicePathRpcResult buildFailedServicePathRpcResult() {
        return new ServicePathRpcResultBuilder()
            .setActualDate(
                new DateAndTime(
                    DateTimeFormatter
                        .ofPattern("yyyy-MM-dd'T'HH:mm:ssxxx")
                        .format(OffsetDateTime.now(ZoneOffset.UTC))))
            .setNotificationType(ServicePathNotificationTypes.ServiceImplementationRequest)
            .setPathDescription(createPathDescription(0,1, 0, 1))
            .setServiceName("service 1")
            .setStatus(RpcStatusEx.Failed)
            .setStatusMessage("failed")
            .build();
    }

    public static RendererRpcResultSp buildRendererRpcResultSp() {
        return new RendererRpcResultSpBuilder()
            .setActualDate(
                new DateAndTime(
                    DateTimeFormatter
                        .ofPattern("yyyy-MM-dd'T'HH:mm:ssxxx")
                        .format(OffsetDateTime.now(ZoneOffset.UTC))))
            .setNotificationType(ServicePathNotificationTypes.ServiceImplementationRequest)
            .setServiceName("service 1")
            .setStatus(RpcStatusEx.Successful)
            .setStatusMessage("success")
            .build();
    }

    public static RendererRpcResultSp buildRendererRpcResultSp(
            ServicePathNotificationTypes servicePathNotificationTypes,
            String serviceName, RpcStatusEx rpcStatusEx, String message) {
        return new RendererRpcResultSpBuilder()
            .setNotificationType(servicePathNotificationTypes)
            .setServiceName(serviceName)
            .setStatus(rpcStatusEx)
            .setStatusMessage(message)
            .build();
    }

    public static ServiceRpcResultSh buildServiceRpcResultSh(
            ServiceNotificationTypes serviceNotificationTypes,
            String serviceName,
            org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.service.types.rev220118
                .RpcStatusEx rpcStatusEx,
            String message) {
        return new ServiceRpcResultShBuilder()
            .setNotificationType(serviceNotificationTypes)
            .setServiceName(serviceName)
            .setStatus(rpcStatusEx)
            .setStatusMessage(message)
            .build();
    }

    public static RendererRpcResultSp buildFailedRendererRpcResultSp() {
        return new RendererRpcResultSpBuilder()
            .setActualDate(
                new DateAndTime(
                    DateTimeFormatter
                        .ofPattern("yyyy-MM-dd'T'HH:mm:ssxxx")
                        .format(OffsetDateTime.now(ZoneOffset.UTC))))
            .setNotificationType(ServicePathNotificationTypes.ServiceImplementationRequest)
            .setServiceName("service 1")
            .setStatus(RpcStatusEx.Failed)
            .setStatusMessage("failed")
            .build();
    }

    public static PathDescription createPathDescription(
            long azRate, long azWaveLength, long zaRate, long zaWaveLength) {
        return new PathDescriptionBuilder()
            .setAToZDirection(
                new AToZDirectionBuilder()
                    .setRate(Uint32.valueOf(azRate))
                    .setAToZWavelengthNumber(Uint32.valueOf(azWaveLength))
                    .build())
            .setZToADirection(
                new ZToADirectionBuilder()
                    .setRate(Uint32.valueOf(zaRate))
                    .setZToAWavelengthNumber(Uint32.valueOf(zaWaveLength))
                    .build())
            .build();
    }

    public static org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev230526
            .service.reconfigure.input.ServiceAEndBuilder getServiceAEndBuildReconfigure() {
        return new org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev230526
                .service.reconfigure.input.ServiceAEndBuilder()
            .setClli("clli")
            .setServiceFormat(ServiceFormat.OC)
            .setServiceRate(Uint32.valueOf(1))
            .setNodeId(new NodeIdType("XPONDER-1-2"))
            .setTxDirection(Map.of(new TxDirectionKey(getTxDirection().key()),getTxDirection()))
            .setRxDirection(Map.of(new RxDirectionKey(getRxDirection().key()), getRxDirection()));
    }

    public static org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev230526
            .service.reconfigure.input.ServiceZEndBuilder getServiceZEndBuildReconfigure() {
        return new org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev230526
                .service.reconfigure.input.ServiceZEndBuilder()
            .setClli("clli")
            .setServiceFormat(ServiceFormat.OC)
            .setServiceRate(Uint32.valueOf(1))
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
