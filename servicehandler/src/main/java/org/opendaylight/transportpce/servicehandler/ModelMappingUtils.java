/*
 * Copyright © 2017 Orange, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.servicehandler;

import com.google.common.util.concurrent.ListenableFuture;

import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev170426.PathComputationRequestOutput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev170426.path.computation.request.input.ServiceAEnd;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev170426.path.computation.request.input.ServiceAEndBuilder;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev170426.path.computation.request.input.ServiceZEnd;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev170426.path.computation.request.input.ServiceZEndBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev161014.configuration.response.common.ConfigurationResponseCommon;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev161014.configuration.response.common.ConfigurationResponseCommonBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev161014.response.parameters.ResponseParametersBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.types.rev161014.LifecycleState;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.types.rev161014.RpcStatus;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.types.rev161014.State;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev161014.ServiceCreateInput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev161014.ServiceCreateOutput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev161014.ServiceCreateOutputBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev161014.ServiceDeleteInput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev161014.ServiceDeleteOutput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev161014.ServiceDeleteOutputBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev161014.ServiceReconfigureInput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev161014.ServiceRerouteInput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev161014.ServiceRerouteOutput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev161014.ServiceRerouteOutputBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev161014.service.list.Services;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev161014.service.list.ServicesBuilder;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.routing.constraints.rev170426.routing.constraints.sp.HardConstraintsBuilder;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.routing.constraints.rev170426.routing.constraints.sp.SoftConstraintsBuilder;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.service.types.rev170426.service.endpoint.sp.RxDirection;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.service.types.rev170426.service.endpoint.sp.RxDirectionBuilder;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.service.types.rev170426.service.endpoint.sp.TxDirection;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.service.types.rev170426.service.endpoint.sp.TxDirectionBuilder;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.service.types.rev170426.service.handler.header.ServiceHandlerHeaderBuilder;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.service.types.rev170426.service.path.PathDescriptionBuilder;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.servicepath.rev170426.ServiceDeleteInputBuilder;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.servicepath.rev170426.ServiceImplementationRequestInput;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.servicepath.rev170426.ServiceImplementationRequestInputBuilder;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.servicepath.rev170426.service.path.list.ServicePaths;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.servicepath.rev170426.service.path.list.ServicePathsBuilder;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;

public final class ModelMappingUtils {
    private ModelMappingUtils() {
    }

    public static ServiceImplementationRequestInput createServiceImplementationRequest(ServiceCreateInput input,
            PathComputationRequestOutput pceResponse) {
        ServiceImplementationRequestInputBuilder serviceImplementationRequestInputBuilder =
                new ServiceImplementationRequestInputBuilder();
        serviceImplementationRequestInputBuilder.setServiceName(input.getServiceName());
        org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.servicepath.rev170426.service.implementation
            .request.input.ServiceAEndBuilder serviceAEnd =
                new org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.servicepath.rev170426.service
                .implementation.request.input.ServiceAEndBuilder();
        serviceAEnd.setServiceFormat(input.getServiceAEnd().getServiceFormat())
                .setServiceRate(input.getServiceAEnd().getServiceRate()).setClli(input.getServiceAEnd().getClli())
                .setNodeId(input.getServiceAEnd().getNodeId())
                .setTxDirection(
                        new org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.service.types.rev170426
                        .service.endpoint.sp.TxDirectionBuilder()
                                .setPort(input.getServiceAEnd().getTxDirection().getPort()).build())
                .setRxDirection(
                        new org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.service.types.rev170426
                        .service.endpoint.sp.RxDirectionBuilder()
                                .setPort(input.getServiceAEnd().getRxDirection().getPort()).build());
        org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.servicepath.rev170426.service.implementation
            .request.input.ServiceZEndBuilder serviceZEnd =
                new org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.servicepath.rev170426.service
                .implementation.request.input.ServiceZEndBuilder();
        serviceZEnd.setServiceFormat(input.getServiceZEnd().getServiceFormat())
                .setServiceRate(input.getServiceZEnd().getServiceRate()).setClli(input.getServiceZEnd().getClli())
                .setNodeId(input.getServiceZEnd().getNodeId())
                .setTxDirection(
                        new org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.service.types.rev170426
                        .service.endpoint.sp.TxDirectionBuilder()
                                .setPort(input.getServiceZEnd().getTxDirection().getPort()).build())
                .setRxDirection(
                        new org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.service.types.rev170426
                        .service.endpoint.sp.RxDirectionBuilder()
                                .setPort(input.getServiceZEnd().getRxDirection().getPort()).build());
        serviceImplementationRequestInputBuilder.setServiceAEnd(serviceAEnd.build());
        serviceImplementationRequestInputBuilder.setServiceZEnd(serviceZEnd.build());
        serviceImplementationRequestInputBuilder.setServiceHandlerHeader(
                new ServiceHandlerHeaderBuilder().setRequestId(input.getSdncRequestHeader().getRequestId()).build());
        org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.servicepath.rev170426.service.implementation
            .request.input.PathDescriptionBuilder pathDescBuilder = new org.opendaylight.yang.gen.v1.http.org
            .transportpce.b.c._interface.servicepath.rev170426.service.implementation.request.input
            .PathDescriptionBuilder();
        pathDescBuilder.setAToZDirection(pceResponse.getResponseParameters().getPathDescription().getAToZDirection());
        pathDescBuilder.setZToADirection(pceResponse.getResponseParameters().getPathDescription().getZToADirection());
        serviceImplementationRequestInputBuilder.setPathDescription(pathDescBuilder.build());
        return serviceImplementationRequestInputBuilder.build();
    }

    public static
            org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.servicepath.rev170426.ServiceDeleteInput
            createServiceDeleteInput(ServiceDeleteInput serviceDeleteInput) {
        ServiceDeleteInputBuilder builder = new ServiceDeleteInputBuilder();
        builder.setServiceName(serviceDeleteInput.getServiceDeleteReqInfo().getServiceName());
        builder.setServiceHandlerHeader(new ServiceHandlerHeaderBuilder()
                .setRequestId(serviceDeleteInput.getSdncRequestHeader().getRequestId()).build());
        return builder.build();
    }

    public static
            org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.servicepath.rev170426.ServiceDeleteInput
            createServiceDeleteInput(ServiceRerouteInput serviceRerouteInput, Services service) {
        ServiceDeleteInputBuilder builder = new ServiceDeleteInputBuilder();
        builder.setServiceName(serviceRerouteInput.getServiceName());
        builder.setServiceHandlerHeader(
                new ServiceHandlerHeaderBuilder().setRequestId(service.getSdncRequestHeader().getRequestId()).build());
        return builder.build();
    }

    public static ServiceAEnd createServiceAEnd(org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev161014
            .service.create.input.ServiceAEnd serviceAEnd) {
        ServiceAEndBuilder serviceAEndBuilder = new ServiceAEndBuilder();
        serviceAEndBuilder.setClli(serviceAEnd.getClli());
        serviceAEndBuilder.setNodeId(serviceAEnd.getNodeId());
        serviceAEndBuilder.setRxDirection(createRxDirection(serviceAEnd.getRxDirection()));
        serviceAEndBuilder.setServiceFormat(serviceAEnd.getServiceFormat());
        serviceAEndBuilder.setServiceRate(serviceAEnd.getServiceRate());
        serviceAEndBuilder.setTxDirection(createTxDirection(serviceAEnd.getTxDirection()));
        return serviceAEndBuilder.build();
    }

    public static ServiceZEnd createServiceZEnd(org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev161014
            .service.create.input.ServiceZEnd serviceZEnd) {
        ServiceZEndBuilder serviceZEndBuilder = new ServiceZEndBuilder();
        serviceZEndBuilder.setClli(serviceZEnd.getClli());
        serviceZEndBuilder.setNodeId(serviceZEnd.getNodeId());
        serviceZEndBuilder.setRxDirection(createRxDirection(serviceZEnd.getRxDirection()));
        serviceZEndBuilder.setServiceFormat(serviceZEnd.getServiceFormat());
        serviceZEndBuilder.setServiceRate(serviceZEnd.getServiceRate());
        serviceZEndBuilder.setTxDirection(createTxDirection(serviceZEnd.getTxDirection()));
        return serviceZEndBuilder.build();
    }

    public static RxDirection createRxDirection(org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types
            .rev161014.service.endpoint.RxDirection rxDirection) {
        RxDirectionBuilder rxDirectionBuilder = new RxDirectionBuilder();
        rxDirectionBuilder.setPort(rxDirection.getPort());
        return rxDirectionBuilder.build();
    }

    public static TxDirection createTxDirection(org.opendaylight.yang.gen.v1.http.org.openroadm.common.service
            .types.rev161014.service.endpoint.TxDirection txDirection) {
        TxDirectionBuilder txDirectionBuilder = new TxDirectionBuilder();
        txDirectionBuilder.setPort(txDirection.getPort());
        return txDirectionBuilder.build();
    }

    public static ListenableFuture<RpcResult<ServiceDeleteOutput>> createDeleteServiceReply(ServiceDeleteInput input,
            String finalAck, String message, String responseCode) {
        ConfigurationResponseCommonBuilder builder = new ConfigurationResponseCommonBuilder()
                .setAckFinalIndicator(finalAck).setResponseMessage(message).setResponseCode(responseCode);
        if (input.getSdncRequestHeader() != null) {
            builder.setRequestId(input.getSdncRequestHeader().getRequestId());
        } else {
            builder.setRequestId(null);
        }
        ConfigurationResponseCommon configurationResponseCommon = builder.build();
        ServiceDeleteOutput output =
                new ServiceDeleteOutputBuilder().setConfigurationResponseCommon(configurationResponseCommon).build();
        return RpcResultBuilder.success(output).buildFuture();
    }

    public static ListenableFuture<RpcResult<ServiceRerouteOutput>> createRerouteServiceReply(ServiceRerouteInput input,
            String finalAck, String message, RpcStatus rpcStatus) {
        ServiceRerouteOutputBuilder output = new ServiceRerouteOutputBuilder().setHardConstraints(null)
                .setSoftConstraints(null).setStatusMessage(message).setStatus(rpcStatus);
        return RpcResultBuilder.success(output.build()).buildFuture();
    }

    public static ListenableFuture<RpcResult<ServiceCreateOutput>> createCreateServiceReply(ServiceCreateInput input,
            String finalAck, String message, String responseCode) {
        ResponseParametersBuilder responseParameters = new ResponseParametersBuilder();
        ConfigurationResponseCommonBuilder configurationResponseCommon = new ConfigurationResponseCommonBuilder()
                .setAckFinalIndicator(finalAck).setResponseMessage(message).setResponseCode(responseCode);
        if (input.getSdncRequestHeader() != null) {
            configurationResponseCommon.setRequestId(input.getSdncRequestHeader().getRequestId());
        } else {
            configurationResponseCommon.setRequestId(null);
        }
        ServiceCreateOutputBuilder output =
                new ServiceCreateOutputBuilder().setConfigurationResponseCommon(configurationResponseCommon.build())
                        .setResponseParameters(responseParameters.build());
        return RpcResultBuilder.success(output.build()).buildFuture();
    }

    /*
     * Map Input (ServiceCreateInmput, ServiceReconfigureInput) & output
     * (PathComputationRequestOutput) to Service.
     *
     * @param serviceCreateInput ServiceCreateInput parameter
     *
     * @param serviceReconfigureInput serviceReconfigureInput parameter
     *
     * @param output PathComputationRequestOutput parameter
     *
     * @return Services Service data
     */
    public static Services mappingServices(ServiceCreateInput serviceCreateInput,
            ServiceReconfigureInput serviceReconfigureInput) {
        org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev161014.service.ServiceAEnd aend = null;
        org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev161014.service.ServiceZEnd zend = null;
        ServicesBuilder service = new ServicesBuilder();
        if (serviceCreateInput != null) {
            aend = new org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev161014.service
                    .ServiceAEndBuilder(serviceCreateInput.getServiceAEnd()).build();
            zend = new org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev161014.service
                    .ServiceZEndBuilder(serviceCreateInput.getServiceZEnd()).build();
            service.setServiceName(serviceCreateInput.getServiceName()).setAdministrativeState(State.OutOfService)
                    .setOperationalState(State.OutOfService).setCommonId(serviceCreateInput.getCommonId())
                    .setConnectionType(serviceCreateInput.getConnectionType())
                    .setCustomer(serviceCreateInput.getCustomer())
                    .setCustomerContact(serviceCreateInput.getCustomerContact())
                    .setHardConstraints(serviceCreateInput.getHardConstraints())
                    .setSoftConstraints(serviceCreateInput.getSoftConstraints())
                    .setSdncRequestHeader(serviceCreateInput.getSdncRequestHeader())
                    .setLifecycleState(LifecycleState.Planned).setServiceAEnd(aend).setServiceZEnd(zend);
        } else if (serviceReconfigureInput != null) {
            aend = new org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev161014
                    .service.ServiceAEndBuilder(serviceReconfigureInput.getServiceAEnd()).build();
            zend = new org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev161014.service
                    .ServiceZEndBuilder(serviceReconfigureInput.getServiceZEnd()).build();
            service.setServiceName(serviceReconfigureInput.getNewServiceName())
                    .setAdministrativeState(State.OutOfService).setOperationalState(State.OutOfService)
                    .setCommonId(serviceReconfigureInput.getCommonId())
                    .setConnectionType(serviceReconfigureInput.getConnectionType())
                    .setCustomer(serviceReconfigureInput.getCustomer())
                    .setCustomerContact(serviceReconfigureInput.getCustomerContact())
                    .setHardConstraints(serviceReconfigureInput.getHardConstraints())
                    .setSoftConstraints(serviceReconfigureInput.getSoftConstraints())
                    .setLifecycleState(LifecycleState.Planned).setServiceAEnd(aend).setServiceZEnd(zend);
        }
        return service.build();
    }

    /*
     * Map Input (ServiceCreateInmput, ServiceReconfigureInput) & output
     * (PathComputationRequestOutput) to ServicePath.
     *
     * @param serviceCreateInput ServiceCreateInput parameter
     *
     * @param serviceReconfigureInput serviceReconfigureInput parameter
     *
     * @param output PathComputationRequestOutput parameter
     *
     * @return ServicesPaths Service Path data
     */
    public static ServicePaths mappingServicePaths(ServiceCreateInput serviceCreateInput,
            ServiceReconfigureInput serviceReconfigureInput, PathComputationRequestOutput output) {
        ServicePathsBuilder servicePathBuilder = new ServicePathsBuilder();
        if (serviceCreateInput != null) {
            org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.service.types.rev170426.service.path
                .ServiceAEndBuilder serviceAEnd = new org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface
                .service.types.rev170426.service.path.ServiceAEndBuilder();
            serviceAEnd.setServiceFormat(serviceCreateInput.getServiceAEnd().getServiceFormat())
                    .setServiceRate(serviceCreateInput.getServiceAEnd().getServiceRate())
                    .setClli(serviceCreateInput.getServiceAEnd().getClli())
                    .setNodeId(serviceCreateInput.getServiceAEnd().getNodeId())
                    .setTxDirection(
                            new org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.service.types
                            .rev170426.service.endpoint.sp.TxDirectionBuilder()
                                    .setPort(serviceCreateInput.getServiceAEnd().getTxDirection().getPort()).build())
                    .setRxDirection(
                            new org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.service.types
                            .rev170426.service.endpoint.sp.RxDirectionBuilder()
                                    .setPort(serviceCreateInput.getServiceAEnd().getRxDirection().getPort()).build());
            org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.service.types.rev170426.service.path
                .ServiceZEndBuilder serviceZEnd =
                    new org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.service.types.rev170426
                        .service.path.ServiceZEndBuilder();
            serviceZEnd.setServiceFormat(serviceCreateInput.getServiceZEnd().getServiceFormat())
                    .setServiceRate(serviceCreateInput.getServiceZEnd().getServiceRate())
                    .setClli(serviceCreateInput.getServiceZEnd().getClli())
                    .setNodeId(serviceCreateInput.getServiceZEnd().getNodeId())
                    .setTxDirection(
                            new org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.service.types
                                .rev170426.service.endpoint.sp.TxDirectionBuilder()
                                    .setPort(serviceCreateInput.getServiceZEnd().getTxDirection().getPort()).build())
                    .setRxDirection(
                            new org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.service.types
                                .rev170426.service.endpoint.sp.RxDirectionBuilder()
                                    .setPort(serviceCreateInput.getServiceZEnd().getRxDirection().getPort()).build());
            servicePathBuilder.setServiceAEnd(serviceAEnd.build());
            servicePathBuilder.setServiceZEnd(serviceZEnd.build());
            MappingConstraints mapConstraints = new MappingConstraints(serviceCreateInput.getHardConstraints(),
                    serviceCreateInput.getSoftConstraints());
            mapConstraints.serviceToServicePathConstarints();
            if (mapConstraints.getServicePathHardConstraints() != null) {
                HardConstraintsBuilder hardConstraintBuilder = new HardConstraintsBuilder();
                hardConstraintBuilder.setCustomerCode(serviceCreateInput.getHardConstraints().getCustomerCode());
                hardConstraintBuilder
                        .setCoRoutingOrGeneral(mapConstraints.getServicePathHardConstraints().getCoRoutingOrGeneral());
                servicePathBuilder.setHardConstraints(hardConstraintBuilder.build());
            }
            if (mapConstraints.getServicePathSoftConstraints() != null) {
                SoftConstraintsBuilder softConstraintBuilder = new SoftConstraintsBuilder();
                softConstraintBuilder.setCustomerCode(mapConstraints.getServicePathSoftConstraints().getCustomerCode());
                softConstraintBuilder
                        .setCoRoutingOrGeneral(mapConstraints.getServicePathSoftConstraints().getCoRoutingOrGeneral());
                servicePathBuilder.setSoftConstraints(softConstraintBuilder.build());
            }
            servicePathBuilder.setServicePathName(serviceCreateInput.getServiceName());
            servicePathBuilder.setServiceHandlerHeader(new ServiceHandlerHeaderBuilder()
                    .setRequestId(serviceCreateInput.getSdncRequestHeader().getRequestId()).build());
            PathDescriptionBuilder pathDescBuilder =
                    new PathDescriptionBuilder(output.getResponseParameters().getPathDescription());
            servicePathBuilder.setPathDescription(pathDescBuilder.build());
        }
        return servicePathBuilder.build();
    }
}
