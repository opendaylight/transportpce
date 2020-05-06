/*
 * Copyright © 2017 Orange, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.servicehandler.impl;

import com.google.common.util.concurrent.ListenableFuture;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.binding.api.NotificationPublishService;
import org.opendaylight.transportpce.common.OperationResult;
import org.opendaylight.transportpce.common.ResponseCodes;
import org.opendaylight.transportpce.pce.service.PathComputationService;
import org.opendaylight.transportpce.renderer.NetworkModelWavelengthService;
import org.opendaylight.transportpce.renderer.provisiondevice.RendererServiceOperations;
import org.opendaylight.transportpce.servicehandler.DowngradeConstraints;
import org.opendaylight.transportpce.servicehandler.ModelMappingUtils;
import org.opendaylight.transportpce.servicehandler.ServiceInput;
import org.opendaylight.transportpce.servicehandler.listeners.PceListenerImpl;
import org.opendaylight.transportpce.servicehandler.listeners.RendererListenerImpl;
import org.opendaylight.transportpce.servicehandler.service.PCEServiceWrapper;
import org.opendaylight.transportpce.servicehandler.service.RendererServiceWrapper;
import org.opendaylight.transportpce.servicehandler.service.ServiceDataStoreOperations;
import org.opendaylight.transportpce.servicehandler.service.ServiceDataStoreOperationsImpl;
import org.opendaylight.transportpce.servicehandler.validation.ServiceCreateValidation;
import org.opendaylight.transportpce.servicehandler.validation.checks.ComplianceCheckResult;
import org.opendaylight.transportpce.servicehandler.validation.checks.ServicehandlerCompliancyCheck;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev200128.PathComputationRequestOutput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev190531.RpcActions;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev190531.ServiceNotificationTypes;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev190531.configuration.response.common.ConfigurationResponseCommon;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev190531.sdnc.request.header.SdncRequestHeaderBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.state.types.rev181130.State;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.types.rev190531.RpcStatus;
import org.opendaylight.yang.gen.v1.http.org.openroadm.routing.constrains.rev190329.routing.constraints.HardConstraints;
import org.opendaylight.yang.gen.v1.http.org.openroadm.routing.constrains.rev190329.routing.constraints.SoftConstraints;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev190531.EquipmentNotificationInput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev190531.EquipmentNotificationOutput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev190531.NetworkReOptimizationInput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev190531.NetworkReOptimizationOutput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev190531.OrgOpenroadmServiceService;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev190531.ServiceCreateComplexResultNotificationRequestInput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev190531.ServiceCreateComplexResultNotificationRequestOutput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev190531.ServiceCreateInput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev190531.ServiceCreateOutput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev190531.ServiceCreateResultNotificationRequestInput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev190531.ServiceCreateResultNotificationRequestOutput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev190531.ServiceDeleteComplexResultNotificationRequestInput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev190531.ServiceDeleteComplexResultNotificationRequestOutput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev190531.ServiceDeleteInput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev190531.ServiceDeleteInputBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev190531.ServiceDeleteOutput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev190531.ServiceDeleteResultNotificationRequestInput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev190531.ServiceDeleteResultNotificationRequestOutput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev190531.ServiceFeasibilityCheckBulkInput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev190531.ServiceFeasibilityCheckBulkOutput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev190531.ServiceFeasibilityCheckInput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev190531.ServiceFeasibilityCheckOutput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev190531.ServiceReconfigureInput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev190531.ServiceReconfigureOutput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev190531.ServiceRerouteConfirmInput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev190531.ServiceRerouteConfirmOutput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev190531.ServiceRerouteInput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev190531.ServiceRerouteOutput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev190531.ServiceRestorationInput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev190531.ServiceRestorationOutput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev190531.ServiceReversionInput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev190531.ServiceReversionOutput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev190531.ServiceRollInput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev190531.ServiceRollOutput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev190531.TempServiceCreateInput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev190531.TempServiceCreateOutput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev190531.TempServiceDeleteInput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev190531.TempServiceDeleteOutput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev190531.service.delete.input.ServiceDeleteReqInfo.TailRetention;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev190531.service.delete.input.ServiceDeleteReqInfoBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev190531.service.list.Services;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.DateAndTime;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Top level service interface providing main OpenROADM controller services.
 */
public class ServicehandlerImpl implements OrgOpenroadmServiceService {

    private static final Logger LOG = LoggerFactory.getLogger(ServicehandlerImpl.class);

    private DataBroker db;
    private ServiceDataStoreOperations serviceDataStoreOperations;
    private PCEServiceWrapper pceServiceWrapper;
    private RendererServiceWrapper rendererServiceWrapper;
    private PceListenerImpl pceListenerImpl;
    private RendererListenerImpl rendererListenerImpl;

    //TODO: remove private request fields as they are in global scope

    public ServicehandlerImpl(DataBroker databroker, PathComputationService pathComputationService,
            RendererServiceOperations rendererServiceOperations, NotificationPublishService notificationPublishService,
            PceListenerImpl pceListenerImpl, RendererListenerImpl rendererListenerImpl,
            NetworkModelWavelengthService networkModelWavelengthService) {
        this.db = databroker;
        this.serviceDataStoreOperations = new ServiceDataStoreOperationsImpl(this.db);
        this.serviceDataStoreOperations.initialize();
        this.pceServiceWrapper = new PCEServiceWrapper(pathComputationService, notificationPublishService);
        this.rendererServiceWrapper = new RendererServiceWrapper(rendererServiceOperations, notificationPublishService);
        this.pceListenerImpl = pceListenerImpl;
        this.rendererListenerImpl = rendererListenerImpl;
    }

    @Override
    public ListenableFuture<RpcResult<ServiceCreateOutput>> serviceCreate(ServiceCreateInput input) {
        LOG.info("RPC service creation received");
        // Validation
        OperationResult validationResult = ServiceCreateValidation.validateServiceCreateRequest(
                new ServiceInput(input), RpcActions.ServiceCreate);
        if (! validationResult.isSuccess()) {
            LOG.warn("Aborting service create because validation of service create request failed: {}",
                    validationResult.getResultMessage());
            return ModelMappingUtils.createCreateServiceReply(input, ResponseCodes.FINAL_ACK_YES,
                    validationResult.getResultMessage(), ResponseCodes.RESPONSE_FAILED);
        }
        this.pceListenerImpl.setInput(new ServiceInput(input));
        this.pceListenerImpl.setServiceReconfigure(false);
        this.pceListenerImpl.setserviceDataStoreOperations(this.serviceDataStoreOperations);
        this.rendererListenerImpl.setserviceDataStoreOperations(serviceDataStoreOperations);
        this.rendererListenerImpl.setServiceInput(new ServiceInput(input));
        LOG.info("Commencing PCE");
        PathComputationRequestOutput output = this.pceServiceWrapper.performPCE(input, true);
        if (output != null) {
            LOG.info("Service compliant, serviceCreate in progress...");
            ConfigurationResponseCommon common = output.getConfigurationResponseCommon();
            return ModelMappingUtils.createCreateServiceReply(input, common.getAckFinalIndicator(),
                    common.getResponseMessage(), common.getResponseCode());
        } else {
            return ModelMappingUtils.createCreateServiceReply(input, ResponseCodes.FINAL_ACK_YES,
                    "PCE calculation failed", ResponseCodes.RESPONSE_FAILED);
        }
    }

    @Override
    public ListenableFuture<RpcResult<ServiceDeleteOutput>> serviceDelete(ServiceDeleteInput input) {
        LOG.info("RPC serviceDelete request received for {}", input.getServiceDeleteReqInfo().getServiceName());
        String message = "";

        /*
         * Upon receipt of service-deleteService RPC, service header and sdnc-request
         * header compliancy are verified.
         */
        LOG.info("checking Service Compliancy ...");
        ComplianceCheckResult serviceHandlerCheckResult =
            ServicehandlerCompliancyCheck.check(
                input.getServiceDeleteReqInfo().getServiceName(),
                input.getSdncRequestHeader(), null, RpcActions.ServiceDelete, false, true);
        if (serviceHandlerCheckResult.hasPassed()) {
            LOG.info("Service compliant !");
        } else {
            LOG.info("Service is not compliant !");
            return ModelMappingUtils
                    .createDeleteServiceReply(input, ResponseCodes.FINAL_ACK_YES,
                            "Service not compliant !", ResponseCodes.RESPONSE_FAILED);
        }

        //Check presence of service to be deleted
        String serviceName = input.getServiceDeleteReqInfo().getServiceName();
        LOG.info("serviceName : {}", serviceName);
        try {
            Optional<Services> service = this.serviceDataStoreOperations.getService(serviceName);
            if (!service.isPresent()) {
                message = "Service '" + serviceName + "' does not exist in datastore";
                LOG.error("serviceDelete: {}", message);
                return ModelMappingUtils.createDeleteServiceReply(input, ResponseCodes.FINAL_ACK_YES,
                        message, ResponseCodes.RESPONSE_FAILED);
            }
        } catch (NullPointerException e) {
            LOG.error("failed to get service '{}' from datastore : ", serviceName, e);
            message = "Service '" + serviceName + "' does not exist in datastore";
            LOG.error("serviceDelete: {}", message);
            return ModelMappingUtils.createDeleteServiceReply(input, ResponseCodes.FINAL_ACK_YES, message,
                    ResponseCodes.RESPONSE_FAILED);
        }
        LOG.info("Service '{}' present in datastore !", serviceName);
        this.pceListenerImpl.setInput(new ServiceInput(input));
        this.pceListenerImpl.setServiceReconfigure(false);
        this.pceListenerImpl.setserviceDataStoreOperations(this.serviceDataStoreOperations);
        this.rendererListenerImpl.setserviceDataStoreOperations(serviceDataStoreOperations);
        this.rendererListenerImpl.setServiceInput(new ServiceInput(input));
        org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.renderer.rev171017
            .ServiceDeleteInput serviceDeleteInput = ModelMappingUtils.createServiceDeleteInput(
                    new ServiceInput(input));
        org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.renderer.rev171017.ServiceDeleteOutput output =
                this.rendererServiceWrapper.performRenderer(serviceDeleteInput,
                ServiceNotificationTypes.ServiceDeleteResult);
        if (output != null) {
            LOG.info("Service present in datastore, service-delete in progress...");
            ConfigurationResponseCommon common = output.getConfigurationResponseCommon();
            return ModelMappingUtils.createDeleteServiceReply(input, common.getAckFinalIndicator(),
                    common.getResponseMessage(), common.getResponseCode());
        } else {
            return ModelMappingUtils.createDeleteServiceReply(input, ResponseCodes.FINAL_ACK_YES,
                    "Renderer service delete failed !", ResponseCodes.RESPONSE_FAILED);
        }
    }

    @Override
    public ListenableFuture<RpcResult<ServiceFeasibilityCheckOutput>> serviceFeasibilityCheck(
            ServiceFeasibilityCheckInput input) {
        LOG.info("RPC service feasibility check received");
        // Validation
        ServiceInput serviceInput = new ServiceInput(input);
        OperationResult validationResult = ServiceCreateValidation.validateServiceCreateRequest(serviceInput,
                RpcActions.ServiceFeasibilityCheck);
        if (! validationResult.isSuccess()) {
            LOG.warn("Aborting service feasibility check because validation of service create request failed: {}",
                    validationResult.getResultMessage());
            return ModelMappingUtils.createCreateServiceReply(input, ResponseCodes.FINAL_ACK_YES,
                    validationResult.getResultMessage(), ResponseCodes.RESPONSE_FAILED);
        }
        this.pceListenerImpl.setInput(new ServiceInput(input));
        this.pceListenerImpl.setServiceReconfigure(false);
        this.pceListenerImpl.setServiceFeasiblity(true);
        this.pceListenerImpl.setserviceDataStoreOperations(this.serviceDataStoreOperations);
        this.rendererListenerImpl.setserviceDataStoreOperations(serviceDataStoreOperations);
        this.rendererListenerImpl.setServiceInput(new ServiceInput(input));
        LOG.info("Commencing PCE");
        PathComputationRequestOutput output = this.pceServiceWrapper.performPCE(input, true);
        if (output != null) {
            LOG.info("Service compliant, serviceFeasibilityCheck in progress...");
            ConfigurationResponseCommon common = output.getConfigurationResponseCommon();
            return ModelMappingUtils.createCreateServiceReply(input, common.getAckFinalIndicator(),
                    common.getResponseMessage(), common.getResponseCode());
        } else {
            return ModelMappingUtils.createCreateServiceReply(input, ResponseCodes.FINAL_ACK_YES,
                    "PCE calculation failed", ResponseCodes.RESPONSE_FAILED);
        }
    }

    @Override
    public ListenableFuture<RpcResult<ServiceReconfigureOutput>> serviceReconfigure(ServiceReconfigureInput input) {
        LOG.info("RPC service reconfigure received");
        String message = "";
        Optional<Services> servicesObject = this.serviceDataStoreOperations.getService(input.getServiceName());
        if (servicesObject.isPresent()) {
            LOG.info("Service '{}' is present", input.getServiceName());
            OperationResult validationResult = ServiceCreateValidation
                    .validateServiceCreateRequest(new ServiceInput(input), RpcActions.ServiceReconfigure);
            if (!validationResult.isSuccess()) {
                LOG.warn("Aborting service reconfigure because validation of service create request failed: {}",
                        validationResult.getResultMessage());
                return ModelMappingUtils.createCreateServiceReply(input, validationResult.getResultMessage(),
                        RpcStatus.Failed);
            }
            this.pceListenerImpl.setInput(new ServiceInput(input));
            this.pceListenerImpl.setServiceReconfigure(true);
            this.pceListenerImpl.setserviceDataStoreOperations(this.serviceDataStoreOperations);
            this.rendererListenerImpl.setserviceDataStoreOperations(serviceDataStoreOperations);
            this.rendererListenerImpl.setServiceInput(new ServiceInput(input));
            org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.renderer.rev171017
                    .ServiceDeleteInput serviceDeleteInput =
                            ModelMappingUtils.createServiceDeleteInput(new ServiceInput(input));
            org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.renderer.rev171017
                    .ServiceDeleteOutput output = this.rendererServiceWrapper.performRenderer(serviceDeleteInput,
                            ServiceNotificationTypes.ServiceDeleteResult);
            if (output != null) {
                LOG.info("Service compliant, service reconfigure in progress...");
                ConfigurationResponseCommon common = output.getConfigurationResponseCommon();
                return ModelMappingUtils.createCreateServiceReply(input, common.getResponseMessage(),
                        RpcStatus.Successful);
            } else {
                return ModelMappingUtils.createCreateServiceReply(input, "Renderer service delete failed !",
                        RpcStatus.Successful);
            }
        } else {
            LOG.error("Service '{}' is not present", input.getServiceName());
            message = "Service '" + input.getServiceName() + "' is not present";
            return ModelMappingUtils.createCreateServiceReply(input, message, RpcStatus.Failed);
        }
    }

    @Override
    public ListenableFuture<RpcResult<ServiceRestorationOutput>> serviceRestoration(ServiceRestorationInput input) {
        LOG.info("RPC service restoration received");
        String message = "";
        String serviceName = input.getServiceName();
        Optional<Services> servicesObject = this.serviceDataStoreOperations.getService(serviceName);
        if (servicesObject.isPresent()) {
            Services service = servicesObject.get();
            State state = service.getOperationalState();
            if (state != State.InService) {
                ServiceDeleteInputBuilder deleteInputBldr = new ServiceDeleteInputBuilder();
                DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssxxx");
                OffsetDateTime offsetDateTime = OffsetDateTime.now(ZoneOffset.UTC);
                DateAndTime datetime = new DateAndTime(dtf.format(offsetDateTime));
                deleteInputBldr.setServiceDeleteReqInfo(new ServiceDeleteReqInfoBuilder()
                    .setServiceName(serviceName).setDueDate(datetime)
                    .setTailRetention(TailRetention.No).build());
                SdncRequestHeaderBuilder sdncBuilder = new SdncRequestHeaderBuilder()
                    .setNotificationUrl(service.getSdncRequestHeader().getNotificationUrl())
                    .setRequestId(service.getSdncRequestHeader().getRequestId())
                    .setRequestSystemId(service.getSdncRequestHeader().getRequestSystemId())
                    .setRpcAction(RpcActions.ServiceDelete);
                deleteInputBldr.setSdncRequestHeader(sdncBuilder.build());
                ServiceInput serviceInput = new ServiceInput(deleteInputBldr.build());
                serviceInput.setServiceAEnd(service.getServiceAEnd());
                serviceInput.setServiceZEnd(service.getServiceZEnd());
                serviceInput.setConnectionType(service.getConnectionType());
                HardConstraints hardConstraints = service.getHardConstraints();
                if (hardConstraints != null) {
                    SoftConstraints softConstraints = service.getSoftConstraints();
                    if (softConstraints != null) {
                        LOG.info("converting hard constraints to soft constraints ...");
                        serviceInput.setSoftConstraints(
                                DowngradeConstraints.updateSoftConstraints(hardConstraints, softConstraints));
                        serviceInput.setHardConstraints(DowngradeConstraints.downgradeHardConstraints(hardConstraints));
                    } else {
                        LOG.warn("service '{}' SoftConstraints is not set !", serviceName);
                        serviceInput.setSoftConstraints(DowngradeConstraints.convertToSoftConstraints(hardConstraints));
                        serviceInput.setHardConstraints(DowngradeConstraints.downgradeHardConstraints(hardConstraints));
                    }
                } else {
                    LOG.warn("service '{}' HardConstraints is not set !", serviceName);
                }
                this.pceListenerImpl.setInput(serviceInput);
                this.pceListenerImpl.setServiceReconfigure(true);
                this.pceListenerImpl.setserviceDataStoreOperations(this.serviceDataStoreOperations);
                this.rendererListenerImpl.setServiceInput(serviceInput);
                this.rendererListenerImpl.setserviceDataStoreOperations(this.serviceDataStoreOperations);
                org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.renderer.rev171017
                    .ServiceDeleteInput serviceDeleteInput = ModelMappingUtils.createServiceDeleteInput(
                            new ServiceInput(deleteInputBldr.build()));
                org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.renderer.rev171017
                    .ServiceDeleteOutput output = this.rendererServiceWrapper.performRenderer(serviceDeleteInput,
                        ServiceNotificationTypes.ServiceDeleteResult);
                if (output != null) {
                    LOG.info("Service present in datastore, service-restore in progress...");
                    ConfigurationResponseCommon common = output.getConfigurationResponseCommon();
                    return ModelMappingUtils.createRestoreServiceReply(common.getResponseMessage(),
                            RpcStatus.Successful);
                } else {
                    return ModelMappingUtils.createRestoreServiceReply("Renderer service delete failed !",
                            RpcStatus.Failed);
                }
            } else {
                LOG.error("Service '{}' is in 'inService' state", input.getServiceName());
                message = "Service '" + input.getServiceName() + "' is in 'inService' state";
                return ModelMappingUtils.createRestoreServiceReply(message, RpcStatus.Failed);
            }
        } else {
            LOG.error("Service '{}' is not present", input.getServiceName());
            message = "Service '" + input.getServiceName() + "' is not present";
            return ModelMappingUtils.createRestoreServiceReply(message, RpcStatus.Failed);
        }
    }

    @Override
    public ListenableFuture<RpcResult<EquipmentNotificationOutput>>
            equipmentNotification(EquipmentNotificationInput input) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public ListenableFuture<RpcResult<ServiceRerouteConfirmOutput>>
            serviceRerouteConfirm(ServiceRerouteConfirmInput input) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public ListenableFuture<RpcResult<ServiceRerouteOutput>> serviceReroute(ServiceRerouteInput input) {
        LOG.info("RPC service reroute received");
        String message = "";
        Optional<Services> servicesObject = this.serviceDataStoreOperations.getService(input.getServiceName());
        if (servicesObject.isPresent()) {
            Services service = servicesObject.get();
            ServiceDeleteInputBuilder deleteInputBldr = new ServiceDeleteInputBuilder();
            DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssxxx");
            OffsetDateTime offsetDateTime = OffsetDateTime.now(ZoneOffset.UTC);
            DateAndTime datetime = new DateAndTime(dtf.format(offsetDateTime));
            deleteInputBldr.setServiceDeleteReqInfo(new ServiceDeleteReqInfoBuilder()
                .setServiceName(input.getServiceName()).setDueDate(datetime)
                .setTailRetention(TailRetention.No).build());
            SdncRequestHeaderBuilder sdncBuilder = new SdncRequestHeaderBuilder()
                .setNotificationUrl(service.getSdncRequestHeader().getNotificationUrl())
                .setRequestId(service.getSdncRequestHeader().getRequestId())
                .setRequestSystemId(service.getSdncRequestHeader().getRequestSystemId())
                .setRpcAction(RpcActions.ServiceDelete);
            deleteInputBldr.setSdncRequestHeader(sdncBuilder.build());
            ServiceInput serviceInput = new ServiceInput(deleteInputBldr.build());
            serviceInput.setServiceAEnd(service.getServiceAEnd());
            serviceInput.setServiceZEnd(service.getServiceZEnd());
            serviceInput.setConnectionType(service.getConnectionType());
            this.pceListenerImpl.setInput(serviceInput);
            this.pceListenerImpl.setServiceReconfigure(true);
            this.pceListenerImpl.setserviceDataStoreOperations(this.serviceDataStoreOperations);
            this.rendererListenerImpl.setServiceInput(serviceInput);
            this.rendererListenerImpl.setserviceDataStoreOperations(this.serviceDataStoreOperations);
            org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.renderer.rev171017
                .ServiceDeleteInput serviceDeleteInput = ModelMappingUtils.createServiceDeleteInput(
                        new ServiceInput(deleteInputBldr.build()));
            org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.renderer.rev171017
                .ServiceDeleteOutput output = this.rendererServiceWrapper.performRenderer(serviceDeleteInput,
                    ServiceNotificationTypes.ServiceDeleteResult);
            if (output != null) {
                LOG.info("Service present in datastore, service-reroute in progress...");
                ConfigurationResponseCommon common = output.getConfigurationResponseCommon();
                return ModelMappingUtils.createRerouteServiceReply(input, common.getAckFinalIndicator(),
                        common.getResponseMessage(), RpcStatus.Successful);
            } else {
                return ModelMappingUtils.createRerouteServiceReply(input, ResponseCodes.FINAL_ACK_YES,
                        "Renderer service delete failed !", RpcStatus.Failed);
            }
        } else {
            LOG.error("Service '{}' is not present", input.getServiceName());
            message = "Service '" + input.getServiceName() + "' is not present";
            return ModelMappingUtils.createRerouteServiceReply(input, ResponseCodes.FINAL_ACK_NO, message,
                    RpcStatus.Failed);
        }
    }

    @Override
    public ListenableFuture<RpcResult<ServiceReversionOutput>> serviceReversion(ServiceReversionInput input) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public ListenableFuture<RpcResult<ServiceRollOutput>> serviceRoll(ServiceRollInput input) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public ListenableFuture<RpcResult<NetworkReOptimizationOutput>>
            networkReOptimization(NetworkReOptimizationInput input) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public ListenableFuture<RpcResult<TempServiceDeleteOutput>> tempServiceDelete(TempServiceDeleteInput input) {
        LOG.info("RPC temp serviceDelete request received for {}", input.getCommonId());
        String message = "";

        /*
         * Upon receipt of service-deleteService RPC, service header and sdnc-request
         * header compliancy are verified.
         */
        LOG.info("checking Service Compliancy ...");
        ComplianceCheckResult serviceHandlerCheckResult = ServicehandlerCompliancyCheck.check(input.getCommonId(),
                null, null, RpcActions.ServiceDelete, false, false);
        if (serviceHandlerCheckResult.hasPassed()) {
            LOG.info("Service compliant !");
        } else {
            LOG.info("Service is not compliant !");
            return ModelMappingUtils.createDeleteServiceReply(input, ResponseCodes.FINAL_ACK_YES,
                    "Service not compliant !", ResponseCodes.RESPONSE_FAILED);
        }

        //Check presence of service to be deleted
        String commonId = input.getCommonId();
        LOG.info("service common-id : {}", commonId);
        try {
            Optional<org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev190531.temp.service.list
                .Services> service = this.serviceDataStoreOperations.getTempService(commonId);
            if (!service.isPresent()) {
                message = "Service '" + commonId + "' does not exist in datastore";
                LOG.error("tempServiceDelete: {}", message);
                return ModelMappingUtils.createDeleteServiceReply(input, ResponseCodes.FINAL_ACK_YES,
                        message, ResponseCodes.RESPONSE_FAILED);
            }
        } catch (NullPointerException e) {
            LOG.info("failed to get service '{}' from datastore : ", commonId, e);
        }

        LOG.info("Service '{}' present in datastore !", commonId);
        this.pceListenerImpl.setInput(new ServiceInput(input));
        this.pceListenerImpl.setServiceReconfigure(false);
        this.pceListenerImpl.setserviceDataStoreOperations(this.serviceDataStoreOperations);
        this.rendererListenerImpl.setserviceDataStoreOperations(this.serviceDataStoreOperations);
        this.rendererListenerImpl.setServiceInput(new ServiceInput(input));
        this.rendererListenerImpl.setTempService(true);
        org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.renderer.rev171017.ServiceDeleteOutput output =
                this.rendererServiceWrapper.performRenderer(input, ServiceNotificationTypes.ServiceDeleteResult);
        if (output != null) {
            LOG.info("Temp Service present in datastore, service-delete in progress...");
            ConfigurationResponseCommon common = output.getConfigurationResponseCommon();
            return ModelMappingUtils.createDeleteServiceReply(input, common.getAckFinalIndicator(),
                    common.getResponseMessage(), common.getResponseCode());
        } else {
            return ModelMappingUtils.createDeleteServiceReply(input, ResponseCodes.FINAL_ACK_YES,
                    "Renderer service delete failed !", ResponseCodes.RESPONSE_FAILED);
        }
    }

    @Override
    public ListenableFuture<RpcResult<TempServiceCreateOutput>> tempServiceCreate(TempServiceCreateInput input) {
        LOG.info("RPC temp service creation received");
        // Validation
        OperationResult validationResult = ServiceCreateValidation.validateServiceCreateRequest(
                new ServiceInput(input), RpcActions.TempServiceCreate);
        if (! validationResult.isSuccess()) {
            LOG.warn("Aborting service create because validation of service create request failed: {}",
                    validationResult.getResultMessage());
            return ModelMappingUtils.createCreateServiceReply(input, ResponseCodes.FINAL_ACK_YES,
                    validationResult.getResultMessage(), ResponseCodes.RESPONSE_FAILED);
        }

        // Starting service create operation
        LOG.info("Commencing PCE");
        this.pceListenerImpl.setInput(new ServiceInput(input));
        this.pceListenerImpl.setServiceReconfigure(false);
        this.pceListenerImpl.setserviceDataStoreOperations(this.serviceDataStoreOperations);
        this.pceListenerImpl.setTempService(true);
        this.rendererListenerImpl.setserviceDataStoreOperations(serviceDataStoreOperations);
        this.rendererListenerImpl.setServiceInput(new ServiceInput(input));
        this.rendererListenerImpl.setTempService(true);
        PathComputationRequestOutput output = this.pceServiceWrapper.performPCE(input, true);
        if (output != null) {
            LOG.info("Service compliant, temp serviceCreate in progress...");
            ConfigurationResponseCommon common = output.getConfigurationResponseCommon();
            return ModelMappingUtils.createCreateServiceReply(input, common.getAckFinalIndicator(),
                    common.getResponseMessage(), common.getResponseCode());
        } else {
            return ModelMappingUtils.createCreateServiceReply(input, ResponseCodes.FINAL_ACK_YES,
                    "PCE calculation failed", ResponseCodes.RESPONSE_FAILED);
        }
    }

    @Override
    public ListenableFuture<RpcResult<
        ServiceDeleteComplexResultNotificationRequestOutput>> serviceDeleteComplexResultNotificationRequest(
            ServiceDeleteComplexResultNotificationRequestInput input) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public ListenableFuture<RpcResult<
        ServiceCreateResultNotificationRequestOutput>> serviceCreateResultNotificationRequest(
            ServiceCreateResultNotificationRequestInput input) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public ListenableFuture<RpcResult<
        ServiceDeleteResultNotificationRequestOutput>> serviceDeleteResultNotificationRequest(
            ServiceDeleteResultNotificationRequestInput input) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public ListenableFuture<RpcResult<
        ServiceCreateComplexResultNotificationRequestOutput>> serviceCreateComplexResultNotificationRequest(
            ServiceCreateComplexResultNotificationRequestInput input) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public ListenableFuture<RpcResult<ServiceFeasibilityCheckBulkOutput>> serviceFeasibilityCheckBulk(
        ServiceFeasibilityCheckBulkInput input) {
        // TODO Auto-generated method stub
        return null;
    }

}
