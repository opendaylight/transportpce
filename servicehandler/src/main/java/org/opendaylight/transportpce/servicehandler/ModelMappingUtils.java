/*
 * Copyright © 2017 Orange, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.servicehandler;

import com.google.common.util.concurrent.ListenableFuture;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev210701.PathComputationRequestOutput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev210701.path.computation.request.input.ServiceAEnd;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev210701.path.computation.request.input.ServiceAEndBuilder;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev210701.path.computation.request.input.ServiceZEnd;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev210701.path.computation.request.input.ServiceZEndBuilder;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev210701.service.path.rpc.result.PathDescription;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.renderer.rev210618.ServiceDeleteInputBuilder;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.renderer.rev210618.ServiceImplementationRequestInput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.renderer.rev210618.ServiceImplementationRequestInputBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.node.types.rev181130.NodeIdType;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev190531.configuration.response.common.ConfigurationResponseCommon;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev190531.configuration.response.common.ConfigurationResponseCommonBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev190531.response.parameters.ResponseParametersBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.state.types.rev181130.LifecycleState;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.state.types.rev181130.State;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.types.rev190531.RpcStatus;
import org.opendaylight.yang.gen.v1.http.org.openroadm.equipment.states.types.rev181130.AdminStates;
import org.opendaylight.yang.gen.v1.http.org.openroadm.otn.common.types.rev200327.ODU0;
import org.opendaylight.yang.gen.v1.http.org.openroadm.otn.common.types.rev200327.ODU1;
import org.opendaylight.yang.gen.v1.http.org.openroadm.otn.common.types.rev200327.ODU2;
import org.opendaylight.yang.gen.v1.http.org.openroadm.otn.common.types.rev200327.ODU2e;
import org.opendaylight.yang.gen.v1.http.org.openroadm.otn.common.types.rev200327.ODU3;
import org.opendaylight.yang.gen.v1.http.org.openroadm.otn.common.types.rev200327.ODU4;
import org.opendaylight.yang.gen.v1.http.org.openroadm.otn.common.types.rev200327.ODUCn;
import org.opendaylight.yang.gen.v1.http.org.openroadm.otn.common.types.rev200327.ODUflexCbr;
import org.opendaylight.yang.gen.v1.http.org.openroadm.otn.common.types.rev200327.ODUflexFlexe;
import org.opendaylight.yang.gen.v1.http.org.openroadm.otn.common.types.rev200327.ODUflexGfp;
import org.opendaylight.yang.gen.v1.http.org.openroadm.otn.common.types.rev200327.ODUflexImp;
import org.opendaylight.yang.gen.v1.http.org.openroadm.otn.common.types.rev200327.OTU0;
import org.opendaylight.yang.gen.v1.http.org.openroadm.otn.common.types.rev200327.OTU1;
import org.opendaylight.yang.gen.v1.http.org.openroadm.otn.common.types.rev200327.OTU2;
import org.opendaylight.yang.gen.v1.http.org.openroadm.otn.common.types.rev200327.OTU2e;
import org.opendaylight.yang.gen.v1.http.org.openroadm.otn.common.types.rev200327.OTU3;
import org.opendaylight.yang.gen.v1.http.org.openroadm.otn.common.types.rev200327.OTU4;
import org.opendaylight.yang.gen.v1.http.org.openroadm.otn.common.types.rev200327.OTUCn;
import org.opendaylight.yang.gen.v1.http.org.openroadm.otn.common.types.rev200327.OTUflex;
import org.opendaylight.yang.gen.v1.http.org.openroadm.otn.common.types.rev200327.OduRateIdentity;
import org.opendaylight.yang.gen.v1.http.org.openroadm.otn.common.types.rev200327.OtuRateIdentity;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev190531.ServiceCreateInput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev190531.ServiceCreateOutput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev190531.ServiceCreateOutputBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev190531.ServiceDeleteInput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev190531.ServiceDeleteOutput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev190531.ServiceDeleteOutputBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev190531.ServiceFeasibilityCheckInput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev190531.ServiceFeasibilityCheckOutput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev190531.ServiceFeasibilityCheckOutputBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev190531.ServiceReconfigureInput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev190531.ServiceReconfigureOutput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev190531.ServiceReconfigureOutputBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev190531.ServiceRerouteInput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev190531.ServiceRerouteOutput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev190531.ServiceRerouteOutputBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev190531.ServiceRestorationInput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev190531.ServiceRestorationOutput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev190531.ServiceRestorationOutputBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev190531.TempServiceCreateInput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev190531.TempServiceCreateOutput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev190531.TempServiceCreateOutputBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev190531.TempServiceDeleteInput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev190531.TempServiceDeleteOutput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev190531.TempServiceDeleteOutputBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev190531.service.list.Services;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev190531.service.list.ServicesBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev190531.service.list.ServicesKey;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.routing.constraints.rev171017.routing.constraints.sp.HardConstraintsBuilder;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.routing.constraints.rev171017.routing.constraints.sp.SoftConstraintsBuilder;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.service.types.rev200128.service.endpoint.sp.RxDirection;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.service.types.rev200128.service.endpoint.sp.RxDirectionBuilder;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.service.types.rev200128.service.endpoint.sp.TxDirection;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.service.types.rev200128.service.endpoint.sp.TxDirectionBuilder;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.service.types.rev200128.service.handler.header.ServiceHandlerHeaderBuilder;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.service.types.rev200128.service.path.PathDescriptionBuilder;
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
        ServiceImplementationRequestInputBuilder serviceImplementationRequestInputBuilder =
            new ServiceImplementationRequestInputBuilder().setConnectionType(input.getConnectionType());
        if (input.isServiceReconfigure()) {
            serviceImplementationRequestInputBuilder.setServiceName(input.getNewServiceName());
        } else {
            serviceImplementationRequestInputBuilder.setServiceName(input.getServiceName());
        }
        org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.renderer.rev210618.service.implementation
            .request.input.ServiceAEndBuilder serviceAEnd = new org.opendaylight.yang.gen.v1.http.org.opendaylight
            .transportpce.renderer.rev210618.service.implementation.request.input.ServiceAEndBuilder();

        serviceAEnd.setServiceFormat(input.getServiceAEnd().getServiceFormat())
            .setServiceRate(input.getServiceAEnd().getServiceRate())
            .setOtuServiceRate(getOtuServiceRate(input.getServiceAEnd().getOtuServiceRate()))
            .setOduServiceRate(getOduServiceRate(input.getServiceAEnd().getOduServiceRate()))
            .setClli(input.getServiceAEnd().getClli())
            .setNodeId(new NodeIdType(input.getServiceAEnd().getNodeId().getValue()).getValue())
            .setTxDirection(new org.opendaylight.yang.gen.v1.http.org
                .transportpce.b.c._interface.service.types.rev200128.service.endpoint.sp.TxDirectionBuilder()
                .setPort(input.getServiceAEnd().getTxDirection().getPort()).build())
            .setRxDirection(new org.opendaylight.yang.gen.v1.http.org
                .transportpce.b.c._interface.service.types.rev200128.service.endpoint.sp.RxDirectionBuilder()
                .setPort(input.getServiceAEnd().getRxDirection().getPort()).build());
        org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.renderer.rev210618.service.implementation
            .request.input.ServiceZEndBuilder serviceZEnd = new org.opendaylight.yang.gen.v1.http.org.opendaylight
            .transportpce.renderer.rev210618.service.implementation.request.input.ServiceZEndBuilder();
        serviceZEnd.setServiceFormat(input.getServiceZEnd().getServiceFormat())
            .setServiceRate(input.getServiceZEnd().getServiceRate())
            .setOtuServiceRate(getOtuServiceRate(input.getServiceZEnd().getOtuServiceRate()))
            .setOduServiceRate(getOduServiceRate(input.getServiceZEnd().getOduServiceRate()))
            .setClli(input.getServiceZEnd().getClli())
            .setNodeId(new NodeIdType(input.getServiceZEnd().getNodeId().getValue()).getValue())
            .setTxDirection(new org.opendaylight.yang.gen.v1.http.org
                .transportpce.b.c._interface.service.types.rev200128.service.endpoint.sp.TxDirectionBuilder()
                .setPort(input.getServiceZEnd().getTxDirection().getPort()).build())
            .setRxDirection(new org.opendaylight.yang.gen.v1.http.org
                .transportpce.b.c._interface.service.types.rev200128.service.endpoint.sp.RxDirectionBuilder()
                .setPort(input.getServiceZEnd().getRxDirection().getPort()).build());

        serviceImplementationRequestInputBuilder
            .setServiceAEnd(serviceAEnd.build())
            .setServiceZEnd(serviceZEnd.build())
            .setConnectionType(input.getConnectionType())
            .setServiceHandlerHeader(
                new ServiceHandlerHeaderBuilder().setRequestId(input.getSdncRequestHeader().getRequestId()).build());
        org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.renderer.rev210618.service.implementation
            .request.input.PathDescriptionBuilder pathDescBuilder = new org.opendaylight.yang.gen.v1.http.org
            .opendaylight.transportpce.renderer.rev210618.service.implementation.request.input
            .PathDescriptionBuilder();
        pathDescBuilder
            .setAToZDirection(pathDescription.getAToZDirection())
            .setZToADirection(pathDescription.getZToADirection());
        serviceImplementationRequestInputBuilder.setPathDescription(pathDescBuilder.build());
        return serviceImplementationRequestInputBuilder.build();
    }

    private static Class<? extends OduRateIdentity> getOduServiceRate(
            Class<? extends org.opendaylight.yang.gen.v1.http.org.openroadm.otn.common.types.rev181130.OduRateIdentity>
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
            Class<? extends org.opendaylight.yang.gen.v1.http.org.openroadm.otn.common.types.rev181130.OtuRateIdentity>
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

    public static org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.renderer.rev210618
            .ServiceDeleteInput createServiceDeleteInput(ServiceInput serviceInput) {
        ServiceDeleteInputBuilder builder = new ServiceDeleteInputBuilder()
            .setServiceName(serviceInput.getServiceName())
            .setServiceHandlerHeader(new ServiceHandlerHeaderBuilder()
                .setRequestId(serviceInput.getSdncRequestHeader().getRequestId()).build());
        return builder.build();
    }

    public static org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.renderer.rev210618
            .ServiceDeleteInput createServiceDeleteInput(ServiceRerouteInput serviceRerouteinput, Services services) {
        ServiceDeleteInputBuilder builder = new ServiceDeleteInputBuilder()
            .setServiceName(serviceRerouteinput.getServiceName())
            .setServiceHandlerHeader(new ServiceHandlerHeaderBuilder().setRequestId(
                services.getSdncRequestHeader().getRequestId()).build());
        return builder.build();
    }

    public static org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.renderer.rev210618
            .ServiceDeleteInput createServiceDeleteInput(ServiceRestorationInput serviceRestorationInput,
            Services services) {
        ServiceDeleteInputBuilder builder = new ServiceDeleteInputBuilder()
            .setServiceName(serviceRestorationInput.getServiceName())
            .setServiceHandlerHeader(new ServiceHandlerHeaderBuilder().setRequestId(
                services.getSdncRequestHeader().getRequestId()).build());
        return builder.build();
    }

    public static org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.renderer.rev210618.ServiceDeleteInput
            createServiceDeleteInput(ServiceReconfigureInput serviceReconfigureInput) {
        String serviceName = serviceReconfigureInput.getServiceName();
        ServiceDeleteInputBuilder builder = new ServiceDeleteInputBuilder()
            .setServiceName(serviceReconfigureInput.getServiceName())
            .setServiceHandlerHeader(
                new ServiceHandlerHeaderBuilder().setRequestId(serviceName + "-reconfigure").build());
        return builder.build();
    }

    public static ServiceAEnd createServiceAEnd(org.opendaylight.yang.gen.v1
            .http.org.openroadm.common.service.types.rev190531.ServiceEndpoint serviceAEnd) {
        ServiceAEndBuilder serviceAEndBuilder = new ServiceAEndBuilder()
            .setClli(serviceAEnd.getClli())
            .setNodeId(serviceAEnd.getNodeId().getValue())
            .setRxDirection(createRxDirection(serviceAEnd.getRxDirection()))
            .setServiceFormat(serviceAEnd.getServiceFormat())
            .setServiceRate(serviceAEnd.getServiceRate())
            .setTxDirection(createTxDirection(serviceAEnd.getTxDirection()));
        return serviceAEndBuilder.build();
    }

    public static ServiceZEnd createServiceZEnd(org.opendaylight.yang.gen.v1
            .http.org.openroadm.common.service.types.rev190531.ServiceEndpoint serviceZEnd) {
        ServiceZEndBuilder serviceZEndBuilder = new ServiceZEndBuilder()
            .setClli(serviceZEnd.getClli())
            .setNodeId(serviceZEnd.getNodeId().getValue())
            .setRxDirection(createRxDirection(serviceZEnd.getRxDirection()))
            .setServiceFormat(serviceZEnd.getServiceFormat())
            .setServiceRate(serviceZEnd.getServiceRate())
            .setTxDirection(createTxDirection(serviceZEnd.getTxDirection()));
        return serviceZEndBuilder.build();
    }

    public static RxDirection createRxDirection(org.opendaylight.yang.gen.v1
            .http.org.openroadm.common.service.types.rev190531.service.endpoint.RxDirection rxDirection) {
        RxDirectionBuilder rxDirectionBuilder = new RxDirectionBuilder().setPort(rxDirection.getPort());
        return rxDirectionBuilder.build();
    }

    public static TxDirection createTxDirection(org.opendaylight.yang.gen.v1
            .http.org.openroadm.common.service.types.rev190531.service.endpoint.TxDirection txDirection) {
        TxDirectionBuilder txDirectionBuilder = new TxDirectionBuilder().setPort(txDirection.getPort());
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

    public static ListenableFuture<RpcResult<TempServiceDeleteOutput>> createDeleteServiceReply(
            TempServiceDeleteInput input, String finalAck, String message, String responseCode) {
        ConfigurationResponseCommonBuilder builder = new ConfigurationResponseCommonBuilder()
            .setAckFinalIndicator(finalAck)
            .setResponseMessage(message)
            .setResponseCode(responseCode)
            .setRequestId(null);
        ConfigurationResponseCommon configurationResponseCommon = builder.build();
        TempServiceDeleteOutput output = new TempServiceDeleteOutputBuilder()
            .setConfigurationResponseCommon(configurationResponseCommon).build();
        return RpcResultBuilder.success(output).buildFuture();
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

    public static ListenableFuture<RpcResult<TempServiceCreateOutput>> createCreateServiceReply(
            TempServiceCreateInput input, String finalAck, String message, String responseCode) {
        ResponseParametersBuilder responseParameters = new ResponseParametersBuilder();
        ConfigurationResponseCommonBuilder configurationResponseCommon = new ConfigurationResponseCommonBuilder()
            .setAckFinalIndicator(finalAck).setResponseMessage(message).setResponseCode(responseCode);
        if (input.getSdncRequestHeader() != null) {
            configurationResponseCommon.setRequestId(input.getSdncRequestHeader().getRequestId());
        } else {
            configurationResponseCommon.setRequestId(null);
        }
        TempServiceCreateOutputBuilder output =
            new TempServiceCreateOutputBuilder().setConfigurationResponseCommon(configurationResponseCommon.build())
                .setResponseParameters(responseParameters.build());
        return RpcResultBuilder.success(output.build()).buildFuture();
    }

    public static ListenableFuture<RpcResult<ServiceFeasibilityCheckOutput>> createCreateServiceReply(
            ServiceFeasibilityCheckInput input, String finalAck, String message, String responseCode) {
        ResponseParametersBuilder responseParameters = new ResponseParametersBuilder();
        ConfigurationResponseCommonBuilder configurationResponseCommon = new ConfigurationResponseCommonBuilder()
            .setAckFinalIndicator(finalAck).setResponseMessage(message).setResponseCode(responseCode);
        if (input.getSdncRequestHeader() != null) {
            configurationResponseCommon.setRequestId(input.getSdncRequestHeader().getRequestId());
        } else {
            configurationResponseCommon.setRequestId(null);
        }
        ServiceFeasibilityCheckOutputBuilder output = new ServiceFeasibilityCheckOutputBuilder()
            .setConfigurationResponseCommon(configurationResponseCommon.build())
            .setResponseParameters(responseParameters.build());
        return RpcResultBuilder.success(output.build()).buildFuture();
    }

    public static ListenableFuture<RpcResult<ServiceReconfigureOutput>> createCreateServiceReply(
            ServiceReconfigureInput input, String message, RpcStatus rpcStatus) {
        ServiceReconfigureOutputBuilder output = new ServiceReconfigureOutputBuilder()
            .setStatus(rpcStatus)
            .setStatusMessage(message);
        return RpcResultBuilder.success(output.build()).buildFuture();
    }

    public static ListenableFuture<RpcResult<ServiceRerouteOutput>> createRerouteServiceReply(ServiceRerouteInput input,
            String finalAckYes, String message, RpcStatus status) {
        ServiceRerouteOutputBuilder output = new ServiceRerouteOutputBuilder()
            .setHardConstraints(null)
            .setSoftConstraints(null)
            .setStatus(status)
            .setStatusMessage(message);
        return RpcResultBuilder.success(output.build()).buildFuture();
    }

    public static ListenableFuture<RpcResult<ServiceRestorationOutput>> createRestoreServiceReply(String message,
            RpcStatus status) {
        ServiceRestorationOutputBuilder output = new ServiceRestorationOutputBuilder()
            .setStatus(status)
            .setStatusMessage(message);
        return RpcResultBuilder.success(output.build()).buildFuture();
    }

    public static Services mappingServices(ServiceCreateInput serviceCreateInput,
            ServiceReconfigureInput serviceReconfigureInput) {
        org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev190531.service.ServiceAEnd aend = null;
        org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev190531.service.ServiceZEnd zend = null;
        ServicesBuilder service = new ServicesBuilder();
        if (serviceCreateInput != null) {
            aend = new org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev190531.service
                .ServiceAEndBuilder(serviceCreateInput.getServiceAEnd()).build();
            zend = new org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev190531.service
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
            aend = new org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev190531.service
                .ServiceAEndBuilder(serviceReconfigureInput.getServiceAEnd()).build();
            zend = new org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev190531.service
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

    public static org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev190531.temp.service.list.Services
            mappingServices(TempServiceCreateInput tempServiceCreateInput) {
        org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev190531.service.ServiceAEnd aend = null;
        org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev190531.service.ServiceZEnd zend = null;
        org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev190531.temp.service.list
            .ServicesBuilder service = new org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev190531.temp
            .service.list.ServicesBuilder();
        aend = new org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev190531.service
            .ServiceAEndBuilder(tempServiceCreateInput.getServiceAEnd()).build();
        zend = new org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev190531.service
            .ServiceZEndBuilder(tempServiceCreateInput.getServiceZEnd()).build();
        service.setServiceName(tempServiceCreateInput.getCommonId()).setAdministrativeState(AdminStates.OutOfService)
            .setOperationalState(State.OutOfService).setCommonId(tempServiceCreateInput.getCommonId())
            .setConnectionType(tempServiceCreateInput.getConnectionType())
            .setCustomer(tempServiceCreateInput.getCustomer())
            .setCustomerContact(tempServiceCreateInput.getCustomerContact())
            .setHardConstraints(tempServiceCreateInput.getHardConstraints())
            .setSoftConstraints(tempServiceCreateInput.getSoftConstraints())
            .setSdncRequestHeader(tempServiceCreateInput.getSdncRequestHeader())
            .setLifecycleState(LifecycleState.Planned).setServiceAEnd(aend).setServiceZEnd(zend);
        return service.build();
    }

    public static ServicePaths mappingServicePaths(ServiceInput serviceInput, PathComputationRequestOutput output) {
        ServicePathsBuilder servicePathBuilder = new ServicePathsBuilder();
        if (serviceInput != null) {
            org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.service.types.rev200128
                .service.path.ServiceAEndBuilder serviceAEnd =
                new org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.service.types.rev200128
                    .service.path.ServiceAEndBuilder();
            serviceAEnd.setServiceFormat(serviceInput.getServiceAEnd().getServiceFormat())
                .setServiceRate(serviceInput.getServiceAEnd().getServiceRate())
                .setClli(serviceInput.getServiceAEnd().getClli())
                .setNodeId(new NodeIdType(serviceInput.getServiceAEnd().getNodeId()).getValue())
                .setTxDirection(new org.opendaylight.yang.gen.v1.http.org
                    .transportpce.b.c._interface.service.types.rev200128.service.endpoint.sp.TxDirectionBuilder()
                    .setPort(serviceInput.getServiceAEnd().getTxDirection().getPort()).build())
                .setRxDirection(new org.opendaylight.yang.gen.v1.http.org
                    .transportpce.b.c._interface.service.types.rev200128.service.endpoint.sp.RxDirectionBuilder()
                    .setPort(serviceInput.getServiceAEnd().getRxDirection().getPort()).build());
            org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.service.types.rev200128
                .service.path.ServiceZEndBuilder serviceZEnd =
                new org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.service.types.rev200128
                    .service.path.ServiceZEndBuilder();
            serviceZEnd.setServiceFormat(serviceInput.getServiceZEnd().getServiceFormat())
                .setServiceRate(serviceInput.getServiceZEnd().getServiceRate())
                .setClli(serviceInput.getServiceZEnd().getClli())
                .setNodeId(new NodeIdType(serviceInput.getServiceZEnd().getNodeId()).getValue())
                .setTxDirection(new org.opendaylight.yang.gen.v1.http.org
                    .transportpce.b.c._interface.service.types.rev200128.service.endpoint.sp.TxDirectionBuilder()
                    .setPort(serviceInput.getServiceZEnd().getTxDirection().getPort()).build())
                .setRxDirection(new org.opendaylight.yang.gen.v1.http.org
                    .transportpce.b.c._interface.service.types.rev200128.service.endpoint.sp.RxDirectionBuilder()
                    .setPort(serviceInput.getServiceZEnd().getRxDirection().getPort()).build());
            servicePathBuilder.setServiceAEnd(serviceAEnd.build());
            servicePathBuilder.setServiceZEnd(serviceZEnd.build());
            MappingConstraints mapConstraints = new MappingConstraints(serviceInput.getHardConstraints(),
                serviceInput.getSoftConstraints());
            mapConstraints.serviceToServicePathConstarints();
            if (mapConstraints.getServicePathHardConstraints() != null) {
                HardConstraintsBuilder hardConstraintBuilder = new HardConstraintsBuilder()
                    .setCustomerCode(serviceInput.getHardConstraints().getCustomerCode())
                    .setCoRoutingOrGeneral(mapConstraints.getServicePathHardConstraints().getCoRoutingOrGeneral());
                servicePathBuilder.setHardConstraints(hardConstraintBuilder.build());
            }
            if (mapConstraints.getServicePathSoftConstraints() != null) {
                SoftConstraintsBuilder softConstraintBuilder = new SoftConstraintsBuilder()
                    .setCustomerCode(mapConstraints.getServicePathSoftConstraints().getCustomerCode())
                    .setCoRoutingOrGeneral(mapConstraints.getServicePathSoftConstraints().getCoRoutingOrGeneral());
                servicePathBuilder.setSoftConstraints(softConstraintBuilder.build());
            }
            servicePathBuilder.setServicePathName(serviceInput.getServiceName());
            servicePathBuilder.setServiceHandlerHeader(new ServiceHandlerHeaderBuilder()
                .setRequestId(serviceInput.getSdncRequestHeader().getRequestId()).build());
            if (output.getResponseParameters().getPathDescription() != null) {
                PathDescriptionBuilder pathDescBuilder =
                    new PathDescriptionBuilder(output.getResponseParameters().getPathDescription());
                servicePathBuilder.setPathDescription(pathDescBuilder.build());
            }
        } else {
            //FIXME: Because of Silicon, we cannot have empty key.
            //it's this case possible ? There is a Junit test covering null
            //temporary workaround as now there is a null key check done by yangtools.
            //Functional review is needed
            LOG.warn("ServiceInput is null");
            servicePathBuilder.withKey(new ServicePathsKey("unknown"));
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
