/*
 * Copyright © 2017 Orange, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.servicehandler;

import com.google.common.util.concurrent.ListenableFuture;
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
import org.opendaylight.yang.gen.v1.http.org.openroadm.otn.common.types.rev210924.ODU0;
import org.opendaylight.yang.gen.v1.http.org.openroadm.otn.common.types.rev210924.ODU1;
import org.opendaylight.yang.gen.v1.http.org.openroadm.otn.common.types.rev210924.ODU2;
import org.opendaylight.yang.gen.v1.http.org.openroadm.otn.common.types.rev210924.ODU2e;
import org.opendaylight.yang.gen.v1.http.org.openroadm.otn.common.types.rev210924.ODU3;
import org.opendaylight.yang.gen.v1.http.org.openroadm.otn.common.types.rev210924.ODU4;
import org.opendaylight.yang.gen.v1.http.org.openroadm.otn.common.types.rev210924.ODUCn;
import org.opendaylight.yang.gen.v1.http.org.openroadm.otn.common.types.rev210924.ODUflexCbr;
import org.opendaylight.yang.gen.v1.http.org.openroadm.otn.common.types.rev210924.ODUflexFlexe;
import org.opendaylight.yang.gen.v1.http.org.openroadm.otn.common.types.rev210924.ODUflexGfp;
import org.opendaylight.yang.gen.v1.http.org.openroadm.otn.common.types.rev210924.ODUflexImp;
import org.opendaylight.yang.gen.v1.http.org.openroadm.otn.common.types.rev210924.OTU0;
import org.opendaylight.yang.gen.v1.http.org.openroadm.otn.common.types.rev210924.OTU1;
import org.opendaylight.yang.gen.v1.http.org.openroadm.otn.common.types.rev210924.OTU2;
import org.opendaylight.yang.gen.v1.http.org.openroadm.otn.common.types.rev210924.OTU2e;
import org.opendaylight.yang.gen.v1.http.org.openroadm.otn.common.types.rev210924.OTU3;
import org.opendaylight.yang.gen.v1.http.org.openroadm.otn.common.types.rev210924.OTU4;
import org.opendaylight.yang.gen.v1.http.org.openroadm.otn.common.types.rev210924.OTUCn;
import org.opendaylight.yang.gen.v1.http.org.openroadm.otn.common.types.rev210924.OTUflex;
import org.opendaylight.yang.gen.v1.http.org.openroadm.otn.common.types.rev210924.OduRateIdentity;
import org.opendaylight.yang.gen.v1.http.org.openroadm.otn.common.types.rev210924.OtuRateIdentity;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev211210.ServiceCreateInput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev211210.ServiceCreateOutput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev211210.ServiceCreateOutputBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev211210.ServiceDeleteInput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev211210.ServiceDeleteOutput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev211210.ServiceDeleteOutputBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev211210.ServiceFeasibilityCheckInput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev211210.ServiceFeasibilityCheckOutput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev211210.ServiceFeasibilityCheckOutputBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev211210.ServiceReconfigureInput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev211210.ServiceReconfigureOutput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev211210.ServiceReconfigureOutputBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev211210.ServiceRerouteInput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev211210.ServiceRerouteOutput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev211210.ServiceRerouteOutputBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev211210.ServiceRestorationInput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev211210.ServiceRestorationOutput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev211210.ServiceRestorationOutputBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev211210.TempServiceCreateInput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev211210.TempServiceCreateOutput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev211210.TempServiceCreateOutputBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev211210.TempServiceDeleteInput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev211210.TempServiceDeleteOutput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev211210.TempServiceDeleteOutputBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev211210.service.list.Services;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev211210.service.list.ServicesBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev211210.service.list.ServicesKey;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.service.types.rev220118.service.endpoint.sp.RxDirection;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.service.types.rev220118.service.endpoint.sp.RxDirectionBuilder;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.service.types.rev220118.service.endpoint.sp.TxDirection;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.service.types.rev220118.service.endpoint.sp.TxDirectionBuilder;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.service.types.rev220118.service.handler.header.ServiceHandlerHeaderBuilder;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.service.types.rev220118.service.path.PathDescriptionBuilder;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.servicepath.rev171017.service.path.list.ServicePaths;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.servicepath.rev171017.service.path.list.ServicePathsBuilder;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.servicepath.rev171017.service.path.list.ServicePathsKey;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ModelMappingUtils {
    private static final Logger LOG = LoggerFactory.getLogger(ModelMappingUtils.class);

    private ModelMappingUtils() {
    }

    public static ServiceImplementationRequestInput createServiceImplementationRequest(ServiceInput input,
            PathDescription pathDescription) {
        return new ServiceImplementationRequestInputBuilder()
            .setConnectionType(input.getConnectionType())
            .setServiceName(
                input.isServiceReconfigure()
                    ? input.getNewServiceName()
                    : input.getServiceName())
            .setServiceAEnd(
                new org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.renderer.rev210915
                        .service.implementation.request.input.ServiceAEndBuilder()
                    .setServiceFormat(input.getServiceAEnd().getServiceFormat())
                    .setServiceRate(input.getServiceAEnd().getServiceRate())
                    .setOtuServiceRate(getOtuServiceRate(input.getServiceAEnd().getOtuServiceRate()))
                    .setOduServiceRate(getOduServiceRate(input.getServiceAEnd().getOduServiceRate()))
                    .setClli(input.getServiceAEnd().getClli())
                    .setNodeId(new NodeIdType(input.getServiceAEnd().getNodeId().getValue()).getValue())
                    .setTxDirection(
                        new org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.service.types.rev220118
                                .service.endpoint.sp.TxDirectionBuilder()
                            .setPort(
                                input.getServiceAEnd().getTxDirection().values().stream().findFirst().get().getPort())
                            .build())
                    .setRxDirection(
                        new org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.service.types.rev220118
                                .service.endpoint.sp.RxDirectionBuilder()
                            .setPort(
                                input.getServiceAEnd().getRxDirection().values().stream().findFirst().get().getPort())
                            .build())
                    .build())
            .setServiceZEnd(
                new org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.renderer.rev210915
                        .service.implementation.request.input.ServiceZEndBuilder()
                    .setServiceFormat(input.getServiceZEnd().getServiceFormat())
                    .setServiceRate(input.getServiceZEnd().getServiceRate())
                    .setOtuServiceRate(getOtuServiceRate(input.getServiceZEnd().getOtuServiceRate()))
                    .setOduServiceRate(getOduServiceRate(input.getServiceZEnd().getOduServiceRate()))
                    .setClli(input.getServiceZEnd().getClli())
                    .setNodeId(new NodeIdType(input.getServiceZEnd().getNodeId().getValue()).getValue())
                    .setTxDirection(
                        new org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.service.types.rev220118
                                .service.endpoint.sp.TxDirectionBuilder()
                            .setPort(
                                input.getServiceZEnd().getTxDirection().values().stream().findFirst().get().getPort())
                            .build())
                    .setRxDirection(
                        new org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.service.types.rev220118
                                .service.endpoint.sp.RxDirectionBuilder()
                            .setPort(
                                input.getServiceZEnd().getRxDirection().values().stream().findFirst().get().getPort())
                            .build())
                    .build())
            .setConnectionType(input.getConnectionType())
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

    private static Class<? extends OduRateIdentity> getOduServiceRate(
            Class<? extends org.opendaylight.yang.gen.v1.http.org.openroadm.otn.common.types.rev210924.OduRateIdentity>
                oduServiceRate) {
        if (oduServiceRate == null) {
            return null;
        }
        String oduRate = oduServiceRate.getSimpleName();
        LOG.info("ODU rate = {}", oduRate);
        switch (oduRate) {
            case "ODU0":
                return ODU0.class;
            case "ODU1":
                return ODU1.class;
            case "ODU2":
                return ODU2.class;
            case "ODU2e":
                return ODU2e.class;
            case "ODU3":
                return ODU3.class;
            case "ODU4":
                return ODU4.class;
            case "ODUCn":
                return ODUCn.class;
            case "ODUflexCbr":
                return ODUflexCbr.class;
            case "ODUflexFlexe":
                return ODUflexFlexe.class;
            case "ODUflexGfp":
                return ODUflexGfp.class;
            case "ODUflexImp":
                return ODUflexImp.class;
            default:
                LOG.error("OTU rate {} not recognized", oduRate);
        }
        return null;
    }

    private static Class<? extends OtuRateIdentity> getOtuServiceRate(
            Class<? extends org.opendaylight.yang.gen.v1.http.org.openroadm.otn.common.types.rev210924.OtuRateIdentity>
                otuServiceRate) {
        if (otuServiceRate == null) {
            return null;
        }
        String otuRate = otuServiceRate.getSimpleName();
        LOG.info("OTU rate = {}", otuRate);
        switch (otuRate) {
            case "OTU0":
                return OTU0.class;
            case "OTU1":
                return OTU1.class;
            case "OTU2":
                return OTU2.class;
            case "OTU2e":
                return OTU2e.class;
            case "OTU3":
                return OTU3.class;
            case "OTU4":
                return OTU4.class;
            case "OTUCn":
                return OTUCn.class;
            case "OTUflex":
                return OTUflex.class;
            default:
                LOG.error("OTU rate {} not recognized", otuRate);
        }
        return null;
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
            .ServiceDeleteInput createServiceDeleteInput(ServiceRerouteInput serviceRerouteinput, Services services) {
        return new ServiceDeleteInputBuilder()
            .setServiceName(serviceRerouteinput.getServiceName())
            .setServiceHandlerHeader(
                new ServiceHandlerHeaderBuilder()
                    .setRequestId(services.getSdncRequestHeader().getRequestId())
                    .build())
            .build();
    }

    public static org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.renderer.rev210915
            .ServiceDeleteInput createServiceDeleteInput(ServiceRestorationInput serviceRestorationInput,
            Services services) {
        return new ServiceDeleteInputBuilder()
            .setServiceName(serviceRestorationInput.getServiceName())
            .setServiceHandlerHeader(
                new ServiceHandlerHeaderBuilder()
                    .setRequestId(services.getSdncRequestHeader().getRequestId())
                    .build())
            .build();
    }

    public static org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.renderer.rev210915
            .ServiceDeleteInput createServiceDeleteInput(ServiceReconfigureInput serviceReconfigureInput) {
        String serviceName = serviceReconfigureInput.getServiceName();
        return new ServiceDeleteInputBuilder()
            .setServiceName(serviceName)
            .setServiceHandlerHeader(
                new ServiceHandlerHeaderBuilder().setRequestId(serviceName + "-reconfigure").build())
            .build();
    }

    public static ServiceAEnd createServiceAEnd(ServiceEndpoint serviceAEnd) {
        return new ServiceAEndBuilder()
            .setClli(serviceAEnd.getClli())
            .setNodeId(serviceAEnd.getNodeId().getValue())
            .setRxDirection(createRxDirection(serviceAEnd.getRxDirection().values().stream().findFirst().get()))
            .setServiceFormat(serviceAEnd.getServiceFormat())
            .setServiceRate(serviceAEnd.getServiceRate())
            .setTxDirection(createTxDirection(serviceAEnd.getTxDirection().values().stream().findFirst().get()))
            .build();
    }

    public static ServiceZEnd createServiceZEnd(ServiceEndpoint serviceZEnd) {
        return new ServiceZEndBuilder()
            .setClli(serviceZEnd.getClli())
            .setNodeId(serviceZEnd.getNodeId().getValue())
            .setRxDirection(createRxDirection(serviceZEnd.getRxDirection().values().stream().findFirst().get()))
            .setServiceFormat(serviceZEnd.getServiceFormat())
            .setServiceRate(serviceZEnd.getServiceRate())
            .setTxDirection(createTxDirection(serviceZEnd.getTxDirection().values().stream().findFirst().get()))
            .build();
    }

    public static RxDirection createRxDirection(
            org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev211210
                .service.endpoint.RxDirection rxDirection) {
        return new RxDirectionBuilder().setPort(rxDirection.getPort()).build();
    }

    public static TxDirection createTxDirection(
            org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev211210
                .service.endpoint.TxDirection txDirection) {
        return new TxDirectionBuilder().setPort(txDirection.getPort()).build();
    }

    public static ListenableFuture<RpcResult<ServiceDeleteOutput>> createDeleteServiceReply(ServiceDeleteInput input,
            String finalAck, String message, String responseCode) {
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

    public static ListenableFuture<RpcResult<ServiceCreateOutput>> createCreateServiceReply(ServiceCreateInput input,
            String finalAck, String message, String responseCode) {
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

    public static ListenableFuture<RpcResult<ServiceFeasibilityCheckOutput>> createCreateServiceReply(
            ServiceFeasibilityCheckInput input, String finalAck, String message, String responseCode) {
        return RpcResultBuilder
            .success(
                new ServiceFeasibilityCheckOutputBuilder()
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

    public static ListenableFuture<RpcResult<ServiceReconfigureOutput>> createCreateServiceReply(
            ServiceReconfigureInput input, String message) {
        return RpcResultBuilder
            .success(
                new ServiceReconfigureOutputBuilder()
                    .setConfigurationResponseCommon(
                        new ConfigurationResponseCommonBuilder()
                            .setResponseMessage(message)
                            .build())
                    .build())
            .buildFuture();
    }

    public static ListenableFuture<RpcResult<ServiceRerouteOutput>> createRerouteServiceReply(ServiceRerouteInput input,
            String finalAckYes, String message) {
        return RpcResultBuilder
            .success(
                new ServiceRerouteOutputBuilder()
                    .setConfigurationResponseCommon(
                        new ConfigurationResponseCommonBuilder()
                            .setResponseMessage(message)
                            .build())
                    .setHardConstraints(null)
                    .setSoftConstraints(null)
                    .build())
            .buildFuture();
    }

    public static ListenableFuture<RpcResult<ServiceRestorationOutput>> createRestoreServiceReply(String message) {
        return RpcResultBuilder
            .success(
                new ServiceRestorationOutputBuilder()
                    .setConfigurationResponseCommon(
                        new ConfigurationResponseCommonBuilder()
                            .setResponseMessage(message)
                            .build())
                    .build())
            .buildFuture();
    }

    public static Services mappingServices(ServiceCreateInput serviceCreateInput,
            ServiceReconfigureInput serviceReconfigureInput) {
        org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev211210.service.ServiceAEnd aend = null;
        org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev211210.service.ServiceZEnd zend = null;
        ServicesBuilder service = new ServicesBuilder();
        if (serviceCreateInput != null) {
            aend = new org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev211210.service
                .ServiceAEndBuilder(serviceCreateInput.getServiceAEnd()).build();
            zend = new org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev211210.service
                .ServiceZEndBuilder(serviceCreateInput.getServiceZEnd()).build();
            service.setServiceName(serviceCreateInput.getServiceName()).setAdministrativeState(AdminStates.OutOfService)
                .setOperationalState(State.OutOfService).setCommonId(serviceCreateInput.getCommonId())
                .setConnectionType(serviceCreateInput.getConnectionType())
                .setCustomer(serviceCreateInput.getCustomer())
                .setCustomerContact(serviceCreateInput.getCustomerContact())
                .setHardConstraints(serviceCreateInput.getHardConstraints())
                .setSoftConstraints(serviceCreateInput.getSoftConstraints())
                .setSdncRequestHeader(serviceCreateInput.getSdncRequestHeader())
                .setLifecycleState(LifecycleState.Planned).setServiceAEnd(aend).setServiceZEnd(zend);
        } else if (serviceReconfigureInput != null) {
            aend = new org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev211210.service
                .ServiceAEndBuilder(serviceReconfigureInput.getServiceAEnd()).build();
            zend = new org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev211210.service
                .ServiceZEndBuilder(serviceReconfigureInput.getServiceZEnd()).build();
            service.setServiceName(serviceReconfigureInput.getServiceName())
                .setAdministrativeState(AdminStates.OutOfService)
                .setOperationalState(State.OutOfService).setCommonId(serviceReconfigureInput.getCommonId())
                .setConnectionType(serviceReconfigureInput.getConnectionType())
                .setCustomer(serviceReconfigureInput.getCustomer())
                .setCustomerContact(serviceReconfigureInput.getCustomerContact())
                .setHardConstraints(serviceReconfigureInput.getHardConstraints())
                .setSoftConstraints(serviceReconfigureInput.getSoftConstraints())
                .setLifecycleState(LifecycleState.Planned).setServiceAEnd(aend).setServiceZEnd(zend);
        } else {
            //FIXME: Because of Silicon, we cannot have empty key.
            //it's this case possible ? There is a Junit test covering null
            //temporary workaround as now there is a null key check done by yangtools.
            //Functional review is needed
            LOG.warn("ServiceCreateInput and ServiceReconfigureInput are null");
            service.withKey(new ServicesKey("unknown"));
        }
        return service.build();
    }

    public static org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev211210.temp.service.list.Services
            mappingServices(TempServiceCreateInput tempServiceCreateInput) {
        return new org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev211210.temp.service.list.ServicesBuilder()
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
            //FIXME: Because of Silicon, we cannot have empty key.
            //it's this case possible ? There is a Junit test covering null
            //temporary workaround as now there is a null key check done by yangtools.
            //Functional review is needed
            LOG.warn("ServiceInput is null");
            return new ServicePathsBuilder().withKey(new ServicePathsKey("unknown")).build();
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
                .setServiceHandlerHeader(
                    new ServiceHandlerHeaderBuilder()
                        .setRequestId(serviceInput.getSdncRequestHeader()
                        .getRequestId())
                        .build());
        if (serviceInput.getHardConstraints() != null) {
            servicePathBuilder.setHardConstraints(serviceInput.getHardConstraints());
        }
        if (serviceInput.getSoftConstraints() != null) {
            servicePathBuilder.setSoftConstraints(serviceInput.getSoftConstraints());
        }

        if (output.getResponseParameters().getPathDescription() != null) {
            servicePathBuilder.setPathDescription(
                new PathDescriptionBuilder(output.getResponseParameters().getPathDescription()).build());
        }
        return servicePathBuilder.build();
    }


    @edu.umd.cs.findbugs.annotations.SuppressFBWarnings(
        value = "PZLA_PREFER_ZERO_LENGTH_ARRAYS",
        justification = "not relevant to return and zero length array as we need real pos")
    public static int[] findTheLongestSubstring(String s1, String s2) {
        if ((s1 == null) || (s2 == null)) {
            return null;
        }
        int[][] dp = new int[s1.length() + 1][s2.length() + 1];
        int maxLen = 0;
        int endPos = 0;
        for (int i = 1; i < dp.length; i++) {
            for (int j = 1; j < dp[0].length; j++) {
                char ch1 = s1.charAt(i - 1);
                char ch2 = s2.charAt(j - 1);
                if (ch1 == ch2) {
                    dp[i][j] = dp[i - 1][j - 1] + 1;
                    if (dp[i][j] >= maxLen) {
                        maxLen = dp[i][j];
                        endPos = i;
                    }
                }
            }
        }
        return new int[] { endPos - maxLen, endPos };
    }
}
