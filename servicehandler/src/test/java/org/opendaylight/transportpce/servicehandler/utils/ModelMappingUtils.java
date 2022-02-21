/*
 * Copyright © 2017 Orange, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.servicehandler.utils;

import com.google.common.util.concurrent.ListenableFuture;
import org.opendaylight.transportpce.servicehandler.ServiceInput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev220118.PathComputationRequestOutput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev220118.path.computation.request.input.ServiceAEnd;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev220118.path.computation.request.input.ServiceAEndBuilder;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev220118.path.computation.request.input.ServiceZEnd;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev220118.path.computation.request.input.ServiceZEndBuilder;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev220118.service.path.rpc.result.PathDescription;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.renderer.rev210915.ServiceDeleteInputBuilder;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.renderer.rev210915.ServiceImplementationRequestInput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.renderer.rev210915.ServiceImplementationRequestInputBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.node.types.rev181130.NodeIdType;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev211210.ServiceEndpoint;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev211210.configuration.response.common.ConfigurationResponseCommonBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev211210.response.parameters.ResponseParametersBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.state.types.rev191129.LifecycleState;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.state.types.rev191129.State;
import org.opendaylight.yang.gen.v1.http.org.openroadm.equipment.states.types.rev191129.AdminStates;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev211210.ServiceCreateInput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev211210.ServiceCreateOutput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev211210.ServiceCreateOutputBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev211210.ServiceDeleteInput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev211210.ServiceDeleteOutput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev211210.ServiceDeleteOutputBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev211210.ServiceReconfigureInput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev211210.ServiceRerouteInput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev211210.ServiceRerouteOutput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev211210.ServiceRerouteOutputBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev211210.TempServiceCreateInput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev211210.TempServiceCreateOutput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev211210.TempServiceCreateOutputBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev211210.TempServiceDeleteInput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev211210.TempServiceDeleteOutput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev211210.TempServiceDeleteOutputBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev211210.service.list.Services;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev211210.service.list.ServicesBuilder;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.service.types.rev220118.service.endpoint.sp.RxDirection;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.service.types.rev220118.service.endpoint.sp.RxDirectionBuilder;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.service.types.rev220118.service.endpoint.sp.TxDirection;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.service.types.rev220118.service.endpoint.sp.TxDirectionBuilder;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.service.types.rev220118.service.handler.header.ServiceHandlerHeaderBuilder;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.service.types.rev220118.service.path.PathDescriptionBuilder;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.servicepath.rev171017.service.path.list.ServicePaths;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.servicepath.rev171017.service.path.list.ServicePathsBuilder;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;


public final class ModelMappingUtils {
    private ModelMappingUtils() {
    }

    public static ServiceImplementationRequestInput createServiceImplementationRequest(
            ServiceInput input,
            PathDescription pathDescription) {
        ServiceImplementationRequestInputBuilder serviceImplementationRequestInputBuilder =
                new ServiceImplementationRequestInputBuilder().setServiceName(input.getServiceName());
        org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.renderer.rev210915
                .service.implementation.request.input.ServiceAEndBuilder serviceAEnd =
            new org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.renderer.rev210915
                    .service.implementation.request.input.ServiceAEndBuilder()
                .setServiceFormat(input.getServiceAEnd().getServiceFormat())
                .setServiceRate(input.getServiceAEnd().getServiceRate()).setClli(input.getServiceAEnd().getClli())
                .setNodeId(input.getServiceAEnd().getNodeId().getValue())
                .setTxDirection(
                    new org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.service.types.rev220118
                            .service.endpoint.sp.TxDirectionBuilder()
                        .setPort(input.getServiceAEnd().getTxDirection().values().stream().findFirst().get().getPort())
                        .build())
                .setRxDirection(
                    new org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.service.types.rev220118
                            .service.endpoint.sp.RxDirectionBuilder()
                        .setPort(input.getServiceAEnd().getRxDirection().values().stream().findFirst().get().getPort())
                        .build());
        org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.renderer.rev210915
                .service.implementation.request.input.ServiceZEndBuilder serviceZEnd =
            new org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.renderer.rev210915
                    .service.implementation.request.input.ServiceZEndBuilder()
                .setServiceFormat(input.getServiceZEnd().getServiceFormat())
                .setServiceRate(input.getServiceZEnd().getServiceRate()).setClli(input.getServiceZEnd().getClli())
                .setNodeId(input.getServiceZEnd().getNodeId().getValue())
                .setTxDirection(
                    new org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.service.types.rev220118
                            .service.endpoint.sp.TxDirectionBuilder()
                        .setPort(input.getServiceZEnd().getTxDirection().values().stream().findFirst().get().getPort())
                        .build())
                .setRxDirection(
                    new org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.service.types.rev220118
                            .service.endpoint.sp.RxDirectionBuilder()
                        .setPort(input.getServiceZEnd().getRxDirection().values().stream().findFirst().get().getPort())
                        .build());
        return serviceImplementationRequestInputBuilder
            .setServiceAEnd(serviceAEnd.build())
            .setServiceZEnd(serviceZEnd.build())
            .setServiceHandlerHeader(
                new ServiceHandlerHeaderBuilder().setRequestId(input.getSdncRequestHeader().getRequestId()).build())
            .setPathDescription(
                new org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.renderer.rev210915
                        .service.implementation.request.input.PathDescriptionBuilder()
                    .setAToZDirection(pathDescription.getAToZDirection())
                    .setZToADirection(pathDescription.getZToADirection())
                    .build())
            .build();
    }

    public static org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.renderer.rev210915
            .ServiceDeleteInput createServiceDeleteInput(ServiceInput serviceInput) {
        return new ServiceDeleteInputBuilder()
            .setServiceName(serviceInput.getServiceName())
            .setServiceHandlerHeader(
                new ServiceHandlerHeaderBuilder()
                    .setRequestId(serviceInput.getSdncRequestHeader().getRequestId())
                    .build())
            .build();
    }

    public static org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.renderer.rev210915
            .ServiceDeleteInput createServiceDeleteInput(
                ServiceRerouteInput serviceRerouteinput,
                Services services) {
        return new ServiceDeleteInputBuilder()
            .setServiceName(serviceRerouteinput.getServiceName())
            .setServiceHandlerHeader(
                new ServiceHandlerHeaderBuilder().setRequestId(services.getSdncRequestHeader().getRequestId()).build())
            .build();
    }

    public static ServiceAEnd createServiceAEnd(ServiceEndpoint serviceAEnd) {
        return new ServiceAEndBuilder()
            .setClli(serviceAEnd.getClli())
            .setNodeId(new NodeIdType(serviceAEnd.getNodeId().getValue()).getValue())
            .setRxDirection(
                createRxDirection(serviceAEnd.getRxDirection().values().stream().findFirst().get()))
            .setServiceFormat(serviceAEnd.getServiceFormat())
            .setServiceRate(serviceAEnd.getServiceRate())
            .setTxDirection(
                createTxDirection(serviceAEnd.getTxDirection().values().stream().findFirst().get()))
            .build();
    }

    public static ServiceZEnd createServiceZEnd(ServiceEndpoint serviceZEnd) {
        return new ServiceZEndBuilder()
            .setClli(serviceZEnd.getClli())
            .setNodeId(new NodeIdType(serviceZEnd.getNodeId().getValue()).getValue())
            .setRxDirection(
                createRxDirection(serviceZEnd.getRxDirection().values().stream().findFirst().get()))
            .setServiceFormat(serviceZEnd.getServiceFormat())
            .setServiceRate(serviceZEnd.getServiceRate())
            .setTxDirection(
                createTxDirection(serviceZEnd.getTxDirection().values().stream().findFirst().get()))
        .build();
    }

    public static RxDirection createRxDirection(
            org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev211210
                .service.endpoint.RxDirection rxDirection) {
        return new RxDirectionBuilder()
            .setPort(rxDirection.getPort())
            .build();
    }

    public static TxDirection createTxDirection(
            org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev211210
                .service.endpoint.TxDirection txDirection) {
        return new TxDirectionBuilder()
            .setPort(txDirection.getPort())
            .build();
    }

    public static ListenableFuture<RpcResult<ServiceDeleteOutput>> createDeleteServiceReply(
            ServiceDeleteInput input, String finalAck, String message, String responseCode) {
        return RpcResultBuilder
            .success(
                new ServiceDeleteOutputBuilder()
                    .setConfigurationResponseCommon(
                        new ConfigurationResponseCommonBuilder()
                            .setAckFinalIndicator(finalAck)
                            .setResponseMessage(message)
                            .setResponseCode(responseCode)
                            .setRequestId(
                                input.getSdncRequestHeader() == null
                                    ? null
                                    : input.getSdncRequestHeader().getRequestId())
                            .build())
                    .build())
            .buildFuture();
    }

    public static ListenableFuture<RpcResult<TempServiceDeleteOutput>> createDeleteServiceReply(
            TempServiceDeleteInput input, String finalAck, String message, String responseCode) {
        return RpcResultBuilder
            .success(
                new TempServiceDeleteOutputBuilder()
                    .setConfigurationResponseCommon(
                        new ConfigurationResponseCommonBuilder()
                            .setAckFinalIndicator(finalAck)
                            .setResponseMessage(message)
                            .setResponseCode(responseCode)
                            .setRequestId(null)
                            .build())
                    .build())
            .buildFuture();
    }

    public static ListenableFuture<RpcResult<ServiceCreateOutput>> createCreateServiceReply(
            ServiceCreateInput input, String finalAck, String message, String responseCode) {
        return RpcResultBuilder
            .success(
                new ServiceCreateOutputBuilder()
                    .setConfigurationResponseCommon(
                        new ConfigurationResponseCommonBuilder()
                            .setAckFinalIndicator(finalAck)
                            .setResponseMessage(message)
                            .setResponseCode(responseCode)
                            .setRequestId(
                                input.getSdncRequestHeader() == null
                                    ? null
                                    : input.getSdncRequestHeader().getRequestId())
                            .build())
                    .setResponseParameters(new ResponseParametersBuilder().build())
                    .build())
            .buildFuture();
    }

    public static ListenableFuture<RpcResult<TempServiceCreateOutput>> createCreateServiceReply(
            TempServiceCreateInput input, String finalAck, String message, String responseCode) {
        return RpcResultBuilder
            .success(
                new TempServiceCreateOutputBuilder()
                    .setConfigurationResponseCommon(
                        new ConfigurationResponseCommonBuilder()
                            .setAckFinalIndicator(finalAck)
                            .setResponseMessage(message)
                            .setResponseCode(responseCode)
                            .setRequestId(
                                input.getSdncRequestHeader() == null
                                    ? null
                                    : input.getSdncRequestHeader().getRequestId())
                            .build())
                    .setResponseParameters(new ResponseParametersBuilder().build())
                    .build())
            .buildFuture();
    }

    public static ListenableFuture<RpcResult<ServiceRerouteOutput>> createRerouteServiceReply(
            ServiceRerouteInput input, String finalAckYes, String message) {
        return RpcResultBuilder
            .success(
                new ServiceRerouteOutputBuilder()
                    .setConfigurationResponseCommon(
                        new ConfigurationResponseCommonBuilder().setResponseMessage(message).build())
                    .setHardConstraints(null)
                    .setSoftConstraints(null)
                    .build())
            .buildFuture();
    }

    public static Services mappingServices(
            ServiceCreateInput serviceCreateInput, ServiceReconfigureInput serviceReconfigureInput) {
        if (serviceCreateInput != null) {
            return new ServicesBuilder()
                .setServiceName(serviceCreateInput.getServiceName())
                .setAdministrativeState(AdminStates.OutOfService)
                .setOperationalState(State.OutOfService)
                .setCommonId(serviceCreateInput.getCommonId())
                .setConnectionType(serviceCreateInput.getConnectionType())
                .setCustomer(serviceCreateInput.getCustomer())
                .setCustomerContact(serviceCreateInput.getCustomerContact())
                .setHardConstraints(serviceCreateInput.getHardConstraints())
                .setSoftConstraints(serviceCreateInput.getSoftConstraints())
                .setSdncRequestHeader(serviceCreateInput.getSdncRequestHeader())
                .setLifecycleState(LifecycleState.Planned)
                .setServiceAEnd(
                    new org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev211210
                            .service.ServiceAEndBuilder(serviceCreateInput.getServiceAEnd())
                        .build())
                .setServiceZEnd(
                    new org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev211210
                            .service.ServiceZEndBuilder(serviceCreateInput.getServiceZEnd())
                        .build())
                .build();
        }
        if (serviceReconfigureInput != null) {
            return new ServicesBuilder()
                .setServiceName(serviceReconfigureInput.getServiceName())
                .setAdministrativeState(AdminStates.OutOfService)
                .setOperationalState(State.OutOfService)
                .setCommonId(serviceReconfigureInput.getCommonId())
                .setConnectionType(serviceReconfigureInput.getConnectionType())
                .setCustomer(serviceReconfigureInput.getCustomer())
                .setCustomerContact(serviceReconfigureInput.getCustomerContact())
                .setHardConstraints(serviceReconfigureInput.getHardConstraints())
                .setSoftConstraints(serviceReconfigureInput.getSoftConstraints())
                .setLifecycleState(LifecycleState.Planned)
                .setServiceAEnd(
                    new org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev211210
                            .service.ServiceAEndBuilder(serviceReconfigureInput.getServiceAEnd())
                        .build())
                .setServiceZEnd(
                    new org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev211210
                            .service.ServiceZEndBuilder(serviceReconfigureInput.getServiceZEnd())
                        .build())
                .build();
        }
        return new ServicesBuilder().build();
    }

    public static org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev211210
            .temp.service.list.Services mappingServices(TempServiceCreateInput tempServiceCreateInput) {
        return new org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev211210
                .temp.service.list.ServicesBuilder()
            .setServiceName(tempServiceCreateInput.getCommonId())
            .setAdministrativeState(AdminStates.OutOfService)
            .setOperationalState(State.OutOfService)
            .setCommonId(tempServiceCreateInput.getCommonId())
            .setConnectionType(tempServiceCreateInput.getConnectionType())
            .setCustomer(tempServiceCreateInput.getCustomer())
            .setCustomerContact(tempServiceCreateInput.getCustomerContact())
            .setHardConstraints(tempServiceCreateInput.getHardConstraints())
            .setSoftConstraints(tempServiceCreateInput.getSoftConstraints())
            .setSdncRequestHeader(tempServiceCreateInput.getSdncRequestHeader())
            .setLifecycleState(LifecycleState.Planned)
            .setServiceAEnd(
                new org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev211210
                    .service.ServiceAEndBuilder(tempServiceCreateInput.getServiceAEnd()).build())
            .setServiceZEnd(
                new org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev211210
                    .service.ServiceZEndBuilder(tempServiceCreateInput.getServiceZEnd()).build())
            .build();
    }

    public static ServicePaths mappingServicePaths(ServiceInput serviceInput, PathComputationRequestOutput output) {
        if (serviceInput == null) {
            return new ServicePathsBuilder().build();
        }
        ServicePathsBuilder servicePathBuilder =
            new ServicePathsBuilder()
                .setServiceAEnd(
                    new org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.service.types.rev220118
                            .service.path.ServiceAEndBuilder()
                        .setServiceFormat(serviceInput.getServiceAEnd().getServiceFormat())
                        .setServiceRate(serviceInput.getServiceAEnd().getServiceRate())
                        .setClli(serviceInput.getServiceAEnd().getClli())
                        .setNodeId(new NodeIdType(serviceInput.getServiceAEnd().getNodeId().getValue()).getValue())
                        .setTxDirection(
                            new org.opendaylight.yang.gen.v1
                                    .http.org.transportpce.b.c._interface.service.types.rev220118
                                        .service.endpoint.sp.TxDirectionBuilder()
                                .setPort(
                                    serviceInput.getServiceAEnd().getTxDirection()
                                        .values().stream().findFirst().get().getPort())
                                .build())
                        .setRxDirection(
                            new org.opendaylight.yang.gen.v1
                                    .http.org.transportpce.b.c._interface.service.types.rev220118
                                        .service.endpoint.sp.RxDirectionBuilder()
                                .setPort(
                                    serviceInput.getServiceAEnd().getRxDirection()
                                        .values().stream().findFirst().get().getPort())
                                .build())
                        .build())
                .setServiceZEnd(
                    new org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.service.types.rev220118
                            .service.path.ServiceZEndBuilder()
                        .setServiceFormat(serviceInput.getServiceZEnd().getServiceFormat())
                        .setServiceRate(serviceInput.getServiceZEnd().getServiceRate())
                        .setClli(serviceInput.getServiceZEnd().getClli())
                        .setNodeId(new NodeIdType(serviceInput.getServiceZEnd().getNodeId().getValue()).getValue())
                        .setTxDirection(
                            new org.opendaylight.yang.gen.v1
                                    .http.org.transportpce.b.c._interface.service.types.rev220118
                                        .service.endpoint.sp.TxDirectionBuilder()
                                .setPort(
                                    serviceInput.getServiceZEnd().getTxDirection()
                                        .values().stream().findFirst().get().getPort())
                                .build())
                        .setRxDirection(
                            new org.opendaylight.yang.gen.v1
                                    .http.org.transportpce.b.c._interface.service.types.rev220118
                                        .service.endpoint.sp.RxDirectionBuilder()
                                .setPort(
                                    serviceInput.getServiceZEnd().getRxDirection()
                                        .values().stream().findFirst().get().getPort())
                                .build())
                        .build())
                .setServicePathName(serviceInput.getServiceName())
                .setServiceHandlerHeader(new ServiceHandlerHeaderBuilder()
                .setRequestId(serviceInput.getSdncRequestHeader().getRequestId()).build())
                .setPathDescription(
                    new PathDescriptionBuilder(output.getResponseParameters().getPathDescription()).build());
        if (serviceInput.getHardConstraints() != null) {
            servicePathBuilder.setHardConstraints(serviceInput.getHardConstraints());
        }
        if (serviceInput.getSoftConstraints() != null) {
            servicePathBuilder.setSoftConstraints(serviceInput.getSoftConstraints());
        }
        return servicePathBuilder.build();
    }
}
