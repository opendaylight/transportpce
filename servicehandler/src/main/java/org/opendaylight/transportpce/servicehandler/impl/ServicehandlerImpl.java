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
import org.opendaylight.transportpce.renderer.provisiondevice.RendererServiceOperations;
import org.opendaylight.transportpce.servicehandler.DowngradeConstraints;
import org.opendaylight.transportpce.servicehandler.ModelMappingUtils;
import org.opendaylight.transportpce.servicehandler.ServiceInput;
import org.opendaylight.transportpce.servicehandler.listeners.NetworkModelListenerImpl;
import org.opendaylight.transportpce.servicehandler.listeners.PceListenerImpl;
import org.opendaylight.transportpce.servicehandler.listeners.RendererListenerImpl;
import org.opendaylight.transportpce.servicehandler.service.PCEServiceWrapper;
import org.opendaylight.transportpce.servicehandler.service.RendererServiceWrapper;
import org.opendaylight.transportpce.servicehandler.service.ServiceDataStoreOperations;
import org.opendaylight.transportpce.servicehandler.validation.ServiceCreateValidation;
import org.opendaylight.transportpce.servicehandler.validation.checks.ComplianceCheckResult;
import org.opendaylight.transportpce.servicehandler.validation.checks.ServicehandlerComplianceCheck;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev210701.PathComputationRequestOutput;
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
import org.opendaylight.yang.gen.v1.nbi.notifications.rev210628.PublishNotificationService;
import org.opendaylight.yang.gen.v1.nbi.notifications.rev210628.PublishNotificationServiceBuilder;
import org.opendaylight.yang.gen.v1.nbi.notifications.rev210628.notification.service.ServiceAEndBuilder;
import org.opendaylight.yang.gen.v1.nbi.notifications.rev210628.notification.service.ServiceZEndBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.DateAndTime;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Top level service interface providing main OpenROADM controller services.
 */
public class ServicehandlerImpl implements OrgOpenroadmServiceService {
    private static final Logger LOG = LoggerFactory.getLogger(ServicehandlerImpl.class);
    private static final String TEMP_SERVICE_CREATE_MSG = "tempServiceCreate: {}";
    private static final String TEMP_SERVICE_DELETE_MSG = "tempServiceDelete: {}";
    private static final String SERVICE_RESTORATION_MSG = "serviceRestoration: {}";
    private static final String SERVICE_RECONFIGURE_MSG = "serviceReconfigure: {}";
    private static final String SERVICE_FEASABILITY_CHECK_MSG = "serviceFeasabilityCheck: {}";
    private static final String SERVICE_DELETE_MSG = "serviceDelete: {}";
    private static final String SERVICE_CREATE_MSG = "serviceCreate: {}";

    private DataBroker db;
    private ServiceDataStoreOperations serviceDataStoreOperations;
    private PCEServiceWrapper pceServiceWrapper;
    private RendererServiceWrapper rendererServiceWrapper;
    private PceListenerImpl pceListenerImpl;
    private RendererListenerImpl rendererListenerImpl;
    private NetworkModelListenerImpl networkModelListenerImpl;
    private NotificationPublishService notificationPublishService;
    private final String topic;

    //TODO: remove private request fields as they are in global scope

    public ServicehandlerImpl(DataBroker databroker, PathComputationService pathComputationService,
            RendererServiceOperations rendererServiceOperations, NotificationPublishService notificationPublishService,
            PceListenerImpl pceListenerImpl, RendererListenerImpl rendererListenerImpl,
            NetworkModelListenerImpl networkModelListenerImpl, ServiceDataStoreOperations serviceDataStoreOperations,
            String topic) {
        this.db = databroker;
        this.serviceDataStoreOperations = serviceDataStoreOperations;
        this.pceServiceWrapper = new PCEServiceWrapper(pathComputationService, notificationPublishService);
        this.rendererServiceWrapper = new RendererServiceWrapper(rendererServiceOperations, notificationPublishService);
        this.pceListenerImpl = pceListenerImpl;
        this.rendererListenerImpl = rendererListenerImpl;
        this.networkModelListenerImpl = networkModelListenerImpl;
        this.notificationPublishService =  notificationPublishService;
        this.topic = topic;
    }


    // This is class is public so that these messages can be accessed from Junit (avoid duplications).
    public static final class LogMessages {

        public static final String PCE_CALLING;
        public static final String ABORT_PCE_FAILED;
        public static final String PCE_FAILED;
        public static final String ABORT_SERVICE_NON_COMPLIANT;
        public static final String SERVICE_NON_COMPLIANT;
        public static final String RENDERER_DELETE_FAILED;
        public static final String ABORT_VALID_FAILED;

        // Static blocks are generated once and spare memory.
        static {
            PCE_CALLING = "Calling PCE";
            ABORT_PCE_FAILED = "Aborting: PCE calculation failed ";
            PCE_FAILED = "PCE calculation failed";
            ABORT_SERVICE_NON_COMPLIANT = "Aborting: non-compliant service ";
            SERVICE_NON_COMPLIANT = "non-compliant service";
            RENDERER_DELETE_FAILED = "Renderer service delete failed";
            ABORT_VALID_FAILED = "Aborting: validation of service create request failed";
        }

        public static String serviceNotInDS(String serviceName) {
            return "Service '" + serviceName + "' does not exist in datastore";
        }

        public static String serviceInService(String serviceName) {
            return "Service '" + serviceName + "' is in 'inService' state";
        }

        private LogMessages() {
        }
    }

    @Override
    public ListenableFuture<RpcResult<ServiceCreateOutput>> serviceCreate(ServiceCreateInput input) {
        LOG.info("RPC serviceCreate received");
        // Validation
        OperationResult validationResult = ServiceCreateValidation.validateServiceCreateRequest(
                new ServiceInput(input), RpcActions.ServiceCreate);
        if (! validationResult.isSuccess()) {
            LOG.warn(SERVICE_CREATE_MSG, LogMessages.ABORT_VALID_FAILED);
            return ModelMappingUtils.createCreateServiceReply(
                    input, ResponseCodes.FINAL_ACK_YES,
                    validationResult.getResultMessage(), ResponseCodes.RESPONSE_FAILED);
        }
        this.pceListenerImpl.setInput(new ServiceInput(input));
        this.pceListenerImpl.setServiceReconfigure(false);
        this.pceListenerImpl.setserviceDataStoreOperations(this.serviceDataStoreOperations);
        this.rendererListenerImpl.setserviceDataStoreOperations(serviceDataStoreOperations);
        this.rendererListenerImpl.setServiceInput(new ServiceInput(input));
        this.networkModelListenerImpl.setserviceDataStoreOperations(serviceDataStoreOperations);
        LOG.debug(SERVICE_CREATE_MSG, LogMessages.PCE_CALLING);
        PathComputationRequestOutput output = this.pceServiceWrapper.performPCE(input, true);
        if (output == null) {
            LOG.warn(SERVICE_CREATE_MSG, LogMessages.ABORT_PCE_FAILED);
            sendNbiNotification(new PublishNotificationServiceBuilder()
                    .setServiceName(input.getServiceName())
                    .setServiceAEnd(new ServiceAEndBuilder(input.getServiceAEnd()).build())
                    .setServiceZEnd(new ServiceZEndBuilder(input.getServiceZEnd()).build())
                    .setCommonId(input.getCommonId()).setConnectionType(input.getConnectionType())
                    .setResponseFailed(LogMessages.ABORT_PCE_FAILED)
                    .setMessage("ServiceCreate request failed ...")
                    .setOperationalState(State.Degraded)
                    .build());
            return ModelMappingUtils.createCreateServiceReply(input, ResponseCodes.FINAL_ACK_YES,
                    LogMessages.PCE_FAILED, ResponseCodes.RESPONSE_FAILED);
        }
        LOG.info("RPC serviceCreate in progress...");
        ConfigurationResponseCommon common = output.getConfigurationResponseCommon();
        return ModelMappingUtils.createCreateServiceReply(
                input, common.getAckFinalIndicator(),
                common.getResponseMessage(), common.getResponseCode());
    }

    @Override
    public ListenableFuture<RpcResult<ServiceDeleteOutput>> serviceDelete(ServiceDeleteInput input) {
        String serviceName = input.getServiceDeleteReqInfo().getServiceName();
        LOG.info("RPC serviceDelete request received for {}", serviceName);

        /*
         * Upon receipt of service-deleteService RPC, service header and sdnc-request
         * header compliance are verified.
         */
        ComplianceCheckResult serviceHandlerCheckResult =
            ServicehandlerComplianceCheck.check(
                input.getServiceDeleteReqInfo().getServiceName(),
                input.getSdncRequestHeader(), null, RpcActions.ServiceDelete, false, true);
        if (!serviceHandlerCheckResult.hasPassed()) {
            LOG.warn(SERVICE_DELETE_MSG, LogMessages.ABORT_SERVICE_NON_COMPLIANT);
            return ModelMappingUtils.createDeleteServiceReply(
                    input, ResponseCodes.FINAL_ACK_YES,
                    LogMessages.SERVICE_NON_COMPLIANT, ResponseCodes.RESPONSE_FAILED);
        }

        //Check presence of service to be deleted
        Optional<Services> serviceOpt = this.serviceDataStoreOperations.getService(serviceName);
        Services service;
        if (!serviceOpt.isPresent()) {
            LOG.warn(SERVICE_DELETE_MSG, LogMessages.serviceNotInDS(serviceName));
            return ModelMappingUtils.createDeleteServiceReply(
                    input, ResponseCodes.FINAL_ACK_YES,
                    LogMessages.serviceNotInDS(serviceName), ResponseCodes.RESPONSE_FAILED);
        }
        service = serviceOpt.get();
        LOG.debug("serviceDelete: Service '{}' found in datastore", serviceName);
        this.pceListenerImpl.setInput(new ServiceInput(input));
        this.pceListenerImpl.setServiceReconfigure(false);
        this.pceListenerImpl.setserviceDataStoreOperations(this.serviceDataStoreOperations);
        this.rendererListenerImpl.setserviceDataStoreOperations(serviceDataStoreOperations);
        this.rendererListenerImpl.setServiceInput(new ServiceInput(input));
        this.networkModelListenerImpl.setserviceDataStoreOperations(serviceDataStoreOperations);
        org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.renderer.rev210618
                .ServiceDeleteInput serviceDeleteInput =
            ModelMappingUtils.createServiceDeleteInput(new ServiceInput(input));
        org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.renderer.rev210618.ServiceDeleteOutput output =
            this.rendererServiceWrapper.performRenderer(
                serviceDeleteInput, ServiceNotificationTypes.ServiceDeleteResult, service);

        if (output == null) {
            LOG.error(SERVICE_DELETE_MSG, LogMessages.RENDERER_DELETE_FAILED);
            sendNbiNotification(new PublishNotificationServiceBuilder()
                    .setServiceName(service.getServiceName())
                    .setServiceAEnd(new ServiceAEndBuilder(service.getServiceAEnd()).build())
                    .setServiceZEnd(new ServiceZEndBuilder(service.getServiceZEnd()).build())
                    .setCommonId(service.getCommonId()).setConnectionType(service.getConnectionType())
                    .setMessage("ServiceDelete request failed ...")
                    .setOperationalState(State.InService)
                    .setResponseFailed(LogMessages.RENDERER_DELETE_FAILED)
                    .build());
            return ModelMappingUtils.createDeleteServiceReply(
                    input, ResponseCodes.FINAL_ACK_YES,
                    LogMessages.RENDERER_DELETE_FAILED, ResponseCodes.RESPONSE_FAILED);
        }

        LOG.debug("RPC serviceDelete in progress...");
        ConfigurationResponseCommon common = output.getConfigurationResponseCommon();
        return ModelMappingUtils.createDeleteServiceReply(
                input, common.getAckFinalIndicator(),
                common.getResponseMessage(), common.getResponseCode());
    }

    @Override
    public ListenableFuture<RpcResult<ServiceFeasibilityCheckOutput>> serviceFeasibilityCheck(
            ServiceFeasibilityCheckInput input) {
        LOG.info("RPC serviceFeasibilityCheck received");
        // Validation
        ServiceInput serviceInput = new ServiceInput(input);
        OperationResult validationResult = ServiceCreateValidation.validateServiceCreateRequest(serviceInput,
                RpcActions.ServiceFeasibilityCheck);
        if (! validationResult.isSuccess()) {
            LOG.warn(SERVICE_FEASABILITY_CHECK_MSG, LogMessages.ABORT_VALID_FAILED);
            return ModelMappingUtils.createCreateServiceReply(
                    input, ResponseCodes.FINAL_ACK_YES,
                    validationResult.getResultMessage(), ResponseCodes.RESPONSE_FAILED);
        }
        this.pceListenerImpl.setInput(new ServiceInput(input));
        this.pceListenerImpl.setServiceReconfigure(false);
        this.pceListenerImpl.setServiceFeasiblity(true);
        this.pceListenerImpl.setserviceDataStoreOperations(this.serviceDataStoreOperations);
        this.rendererListenerImpl.setserviceDataStoreOperations(serviceDataStoreOperations);
        this.rendererListenerImpl.setServiceInput(new ServiceInput(input));
        this.networkModelListenerImpl.setserviceDataStoreOperations(serviceDataStoreOperations);
        LOG.debug(SERVICE_FEASABILITY_CHECK_MSG, LogMessages.PCE_CALLING);
        PathComputationRequestOutput output = this.pceServiceWrapper.performPCE(input, true);
        if (output == null) {
            LOG.warn(SERVICE_FEASABILITY_CHECK_MSG, LogMessages.ABORT_PCE_FAILED);
            return ModelMappingUtils.createCreateServiceReply(input, ResponseCodes.FINAL_ACK_YES,
                    LogMessages.PCE_FAILED, ResponseCodes.RESPONSE_FAILED);
        }
        LOG.info("RPC serviceFeasibilityCheck in progress...");
        ConfigurationResponseCommon common = output.getConfigurationResponseCommon();
        return ModelMappingUtils.createCreateServiceReply(
                input, common.getAckFinalIndicator(),
                common.getResponseMessage(), common.getResponseCode());
    }

    @Override
    public ListenableFuture<RpcResult<ServiceReconfigureOutput>> serviceReconfigure(ServiceReconfigureInput input) {
        String serviceName = input.getServiceName();
        LOG.info("RPC serviceReconfigure received for {}", serviceName);
        Optional<Services> servicesObject = this.serviceDataStoreOperations.getService(serviceName);
        if (!servicesObject.isPresent()) {
            LOG.warn(SERVICE_RECONFIGURE_MSG, LogMessages.serviceNotInDS(serviceName));
            return ModelMappingUtils.createCreateServiceReply(
                input,
                LogMessages.serviceNotInDS(serviceName), RpcStatus.Failed);
        }
        LOG.debug("Service '{}' found in datastore", serviceName);
        OperationResult validationResult = ServiceCreateValidation
                .validateServiceCreateRequest(new ServiceInput(input), RpcActions.ServiceReconfigure);
        if (!validationResult.isSuccess()) {
            LOG.warn(SERVICE_RECONFIGURE_MSG, LogMessages.ABORT_VALID_FAILED);
            return ModelMappingUtils.createCreateServiceReply(
                    input,
                    validationResult.getResultMessage(), RpcStatus.Failed);
        }
        this.pceListenerImpl.setInput(new ServiceInput(input));
        this.pceListenerImpl.setServiceReconfigure(true);
        this.pceListenerImpl.setserviceDataStoreOperations(this.serviceDataStoreOperations);
        this.rendererListenerImpl.setserviceDataStoreOperations(serviceDataStoreOperations);
        this.rendererListenerImpl.setServiceInput(new ServiceInput(input));
        this.networkModelListenerImpl.setserviceDataStoreOperations(serviceDataStoreOperations);
        org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.renderer.rev210618
                .ServiceDeleteInput serviceDeleteInput =
                        ModelMappingUtils.createServiceDeleteInput(new ServiceInput(input));
        org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.renderer.rev210618
                .ServiceDeleteOutput output = this.rendererServiceWrapper.performRenderer(serviceDeleteInput,
                        ServiceNotificationTypes.ServiceDeleteResult, null);
        if (output == null) {
            LOG.error(SERVICE_RECONFIGURE_MSG, LogMessages.RENDERER_DELETE_FAILED);
            return ModelMappingUtils.createCreateServiceReply(
                    input,
                    LogMessages.RENDERER_DELETE_FAILED, RpcStatus.Successful);
                    //TODO check if RpcStatus.Successful is really expected here
        }
        LOG.info("RPC serviceReconfigure in progress...");
        ConfigurationResponseCommon common = output.getConfigurationResponseCommon();
        return ModelMappingUtils.createCreateServiceReply(
                input,
                common.getResponseMessage(), RpcStatus.Successful);
    }

    @Override
    public ListenableFuture<RpcResult<ServiceRestorationOutput>> serviceRestoration(ServiceRestorationInput input) {
        String serviceName = input.getServiceName();
        LOG.info("RPC serviceRestoration received for {}", serviceName);
        Optional<Services> servicesObject = this.serviceDataStoreOperations.getService(serviceName);

        if (!servicesObject.isPresent()) {
            LOG.warn(SERVICE_RESTORATION_MSG, LogMessages.serviceNotInDS(serviceName));
            return ModelMappingUtils.createRestoreServiceReply(
                    LogMessages.serviceNotInDS(serviceName), RpcStatus.Failed);
        }

        Services service = servicesObject.get();
        State state = service.getOperationalState();

        if (state == State.InService) {
            LOG.error(SERVICE_RESTORATION_MSG, LogMessages.serviceInService(serviceName));
            return ModelMappingUtils.createRestoreServiceReply(
                    LogMessages.serviceInService(serviceName), RpcStatus.Failed);
        }

        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssxxx");
        OffsetDateTime offsetDateTime = OffsetDateTime.now(ZoneOffset.UTC);
        DateAndTime datetime = new DateAndTime(dtf.format(offsetDateTime));
        SdncRequestHeaderBuilder sdncBuilder = new SdncRequestHeaderBuilder()
                .setNotificationUrl(service.getSdncRequestHeader().getNotificationUrl())
                .setRequestId(service.getSdncRequestHeader().getRequestId())
                .setRequestSystemId(service.getSdncRequestHeader().getRequestSystemId())
                .setRpcAction(RpcActions.ServiceDelete);
        ServiceDeleteInputBuilder deleteInputBldr = new ServiceDeleteInputBuilder()
                .setServiceDeleteReqInfo(new ServiceDeleteReqInfoBuilder()
                    .setServiceName(serviceName)
                    .setDueDate(datetime)
                    .setTailRetention(TailRetention.No).build())
                .setSdncRequestHeader(sdncBuilder.build());
        ServiceInput serviceInput = new ServiceInput(deleteInputBldr.build());
        serviceInput.setServiceAEnd(service.getServiceAEnd());
        serviceInput.setServiceZEnd(service.getServiceZEnd());
        serviceInput.setConnectionType(service.getConnectionType());
        HardConstraints hardConstraints = service.getHardConstraints();
        if (hardConstraints == null) {
            LOG.warn("service '{}' HardConstraints is not set !", serviceName);
        } else {
            SoftConstraints softConstraints = service.getSoftConstraints();
            if (softConstraints == null) {
                LOG.warn("service '{}' SoftConstraints is not set !", serviceName);
                serviceInput.setSoftConstraints(DowngradeConstraints.convertToSoftConstraints(hardConstraints));
                serviceInput.setHardConstraints(DowngradeConstraints.downgradeHardConstraints(hardConstraints));
            } else {
                LOG.info("converting hard constraints to soft constraints ...");
                serviceInput.setSoftConstraints(
                        DowngradeConstraints.updateSoftConstraints(hardConstraints, softConstraints));
                serviceInput.setHardConstraints(DowngradeConstraints.downgradeHardConstraints(hardConstraints));
            }
        }
        this.pceListenerImpl.setInput(serviceInput);
        this.pceListenerImpl.setServiceReconfigure(true);
        this.pceListenerImpl.setserviceDataStoreOperations(this.serviceDataStoreOperations);
        this.rendererListenerImpl.setServiceInput(serviceInput);
        this.rendererListenerImpl.setserviceDataStoreOperations(this.serviceDataStoreOperations);
        this.networkModelListenerImpl.setserviceDataStoreOperations(serviceDataStoreOperations);
        org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.renderer.rev210618
            .ServiceDeleteInput serviceDeleteInput = ModelMappingUtils.createServiceDeleteInput(
                    new ServiceInput(deleteInputBldr.build()));
        org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.renderer.rev210618
            .ServiceDeleteOutput output = this.rendererServiceWrapper.performRenderer(serviceDeleteInput,
                ServiceNotificationTypes.ServiceDeleteResult, null);
        if (output == null) {
            LOG.error(SERVICE_RESTORATION_MSG, LogMessages.RENDERER_DELETE_FAILED);
            return ModelMappingUtils.createRestoreServiceReply(
                     LogMessages.RENDERER_DELETE_FAILED, RpcStatus.Failed);
        }
        LOG.info("RPC serviceRestore in progress...");
        ConfigurationResponseCommon common = output.getConfigurationResponseCommon();
        return ModelMappingUtils.createRestoreServiceReply(
                common.getResponseMessage(), RpcStatus.Successful);

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
        String serviceName = input.getServiceName();
        LOG.info("RPC serviceReroute received for {}", serviceName);
        Optional<Services> servicesObject = this.serviceDataStoreOperations.getService(serviceName);
        if (!servicesObject.isPresent()) {
            LOG.warn("serviceReroute: {}", LogMessages.serviceNotInDS(serviceName));
            return ModelMappingUtils.createRerouteServiceReply(
                    input, ResponseCodes.FINAL_ACK_NO,
                    LogMessages.serviceNotInDS(serviceName), RpcStatus.Failed);
        }
        Services service = servicesObject.get();
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssxxx");
        OffsetDateTime offsetDateTime = OffsetDateTime.now(ZoneOffset.UTC);
        DateAndTime datetime = new DateAndTime(dtf.format(offsetDateTime));
        SdncRequestHeaderBuilder sdncBuilder = new SdncRequestHeaderBuilder()
                .setNotificationUrl(service.getSdncRequestHeader().getNotificationUrl())
                .setRequestId(service.getSdncRequestHeader().getRequestId())
                .setRequestSystemId(service.getSdncRequestHeader().getRequestSystemId())
                .setRpcAction(RpcActions.ServiceDelete);
        ServiceDeleteInputBuilder deleteInputBldr = new ServiceDeleteInputBuilder()
                .setServiceDeleteReqInfo(new ServiceDeleteReqInfoBuilder()
                    .setServiceName(serviceName).setDueDate(datetime)
                    .setTailRetention(TailRetention.No).build())
                .setSdncRequestHeader(sdncBuilder.build());
        ServiceInput serviceInput = new ServiceInput(deleteInputBldr.build());
        serviceInput.setServiceAEnd(service.getServiceAEnd());
        serviceInput.setServiceZEnd(service.getServiceZEnd());
        serviceInput.setConnectionType(service.getConnectionType());
        this.pceListenerImpl.setInput(serviceInput);
        this.pceListenerImpl.setServiceReconfigure(true);
        this.pceListenerImpl.setserviceDataStoreOperations(this.serviceDataStoreOperations);
        this.rendererListenerImpl.setServiceInput(serviceInput);
        this.rendererListenerImpl.setserviceDataStoreOperations(this.serviceDataStoreOperations);
        this.networkModelListenerImpl.setserviceDataStoreOperations(serviceDataStoreOperations);
        org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.renderer.rev210618
            .ServiceDeleteInput serviceDeleteInput = ModelMappingUtils.createServiceDeleteInput(
                    new ServiceInput(deleteInputBldr.build()));
        org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.renderer.rev210618
            .ServiceDeleteOutput output = this.rendererServiceWrapper.performRenderer(serviceDeleteInput,
                ServiceNotificationTypes.ServiceDeleteResult, null);
        if (output == null) {
            LOG.error("serviceReroute: {}", LogMessages.RENDERER_DELETE_FAILED);
            return ModelMappingUtils.createRerouteServiceReply(
                    input, ResponseCodes.FINAL_ACK_YES,
                    LogMessages.RENDERER_DELETE_FAILED, RpcStatus.Failed);
        }
        LOG.info("RPC ServiceReroute in progress...");
        ConfigurationResponseCommon common = output.getConfigurationResponseCommon();
        return ModelMappingUtils.createRerouteServiceReply(
                input, common.getAckFinalIndicator(),
                common.getResponseMessage(), RpcStatus.Successful);
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
        String commonId = input.getCommonId();
        LOG.info("RPC temp serviceDelete request received for {}", commonId);

        /*
         * Upon receipt of service-deleteService RPC, service header and sdnc-request
         * header compliancy are verified.
         */
        LOG.debug("checking Service Compliance ...");
        ComplianceCheckResult serviceHandlerCheckResult = ServicehandlerComplianceCheck.check(
                commonId, null, null, RpcActions.ServiceDelete, false, false
            );
        if (!serviceHandlerCheckResult.hasPassed()) {
            LOG.warn(TEMP_SERVICE_DELETE_MSG, LogMessages.ABORT_SERVICE_NON_COMPLIANT);
            return ModelMappingUtils.createDeleteServiceReply(
                    input, ResponseCodes.FINAL_ACK_YES,
                    LogMessages.SERVICE_NON_COMPLIANT, ResponseCodes.RESPONSE_FAILED);
        }

        //Check presence of service to be deleted
        LOG.debug("service common-id '{}' is compliant", commonId);
        Optional<org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev190531.temp.service.list.Services>
                service =
            this.serviceDataStoreOperations.getTempService(commonId);
        if (!service.isPresent()) {
            LOG.error(TEMP_SERVICE_DELETE_MSG, LogMessages.serviceNotInDS(commonId));
            return ModelMappingUtils.createDeleteServiceReply(
                    input, ResponseCodes.FINAL_ACK_YES,
                    LogMessages.serviceNotInDS(commonId), ResponseCodes.RESPONSE_FAILED);
        }

        LOG.info("Service '{}' present in datastore !", commonId);
        this.pceListenerImpl.setInput(new ServiceInput(input));
        this.pceListenerImpl.setServiceReconfigure(false);
        this.pceListenerImpl.setserviceDataStoreOperations(this.serviceDataStoreOperations);
        this.rendererListenerImpl.setserviceDataStoreOperations(this.serviceDataStoreOperations);
        this.rendererListenerImpl.setServiceInput(new ServiceInput(input));
        this.rendererListenerImpl.setTempService(true);
        this.networkModelListenerImpl.setserviceDataStoreOperations(serviceDataStoreOperations);
        org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.renderer.rev210618.ServiceDeleteOutput output =
                this.rendererServiceWrapper.performRenderer(input, ServiceNotificationTypes.ServiceDeleteResult);
        if (output == null) {
            LOG.error(TEMP_SERVICE_DELETE_MSG, LogMessages.RENDERER_DELETE_FAILED);
            return ModelMappingUtils.createDeleteServiceReply(
                    input, ResponseCodes.FINAL_ACK_YES,
                    LogMessages.RENDERER_DELETE_FAILED, ResponseCodes.RESPONSE_FAILED);
        }
        LOG.info("RPC tempServiceDelete in progress...");
        ConfigurationResponseCommon common = output.getConfigurationResponseCommon();
        return ModelMappingUtils.createDeleteServiceReply(
                input, common.getAckFinalIndicator(),
                common.getResponseMessage(), common.getResponseCode());
    }

    @Override
    public ListenableFuture<RpcResult<TempServiceCreateOutput>> tempServiceCreate(TempServiceCreateInput input) {
        LOG.info("RPC tempServiceCreate received");
        // Validation
        OperationResult validationResult = ServiceCreateValidation.validateServiceCreateRequest(
                new ServiceInput(input), RpcActions.TempServiceCreate);
        if (! validationResult.isSuccess()) {
            LOG.warn(TEMP_SERVICE_CREATE_MSG, LogMessages.ABORT_VALID_FAILED);
            return ModelMappingUtils.createCreateServiceReply(
                    input, ResponseCodes.FINAL_ACK_YES,
                    validationResult.getResultMessage(), ResponseCodes.RESPONSE_FAILED);
        }

        // Starting service create operation
        LOG.debug(TEMP_SERVICE_CREATE_MSG, LogMessages.PCE_CALLING);
        this.pceListenerImpl.setInput(new ServiceInput(input));
        this.pceListenerImpl.setServiceReconfigure(false);
        this.pceListenerImpl.setserviceDataStoreOperations(this.serviceDataStoreOperations);
        this.pceListenerImpl.setTempService(true);
        this.rendererListenerImpl.setserviceDataStoreOperations(serviceDataStoreOperations);
        this.rendererListenerImpl.setServiceInput(new ServiceInput(input));
        this.rendererListenerImpl.setTempService(true);
        this.networkModelListenerImpl.setserviceDataStoreOperations(serviceDataStoreOperations);
        PathComputationRequestOutput output = this.pceServiceWrapper.performPCE(input, true);
        if (output == null) {
            LOG.warn(TEMP_SERVICE_CREATE_MSG, LogMessages.ABORT_PCE_FAILED);
            return ModelMappingUtils.createCreateServiceReply(
                    input, ResponseCodes.FINAL_ACK_YES,
                    LogMessages.PCE_FAILED, ResponseCodes.RESPONSE_FAILED);
        }
        LOG.info("RPC tempServiceCreate in progress...");
        ConfigurationResponseCommon common = output.getConfigurationResponseCommon();
        return ModelMappingUtils.createCreateServiceReply(
                input, common.getAckFinalIndicator(),
                common.getResponseMessage(), common.getResponseCode());
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

    /**
     * Send notification to NBI notification in order to publish message.
     * @param service PublishNotificationService
     */
    private void sendNbiNotification(PublishNotificationService service) {
        try {
            notificationPublishService.putNotification(service);
        } catch (InterruptedException e) {
            LOG.warn("Cannot send notification to nbi", e);
            Thread.currentThread().interrupt();
        }
    }
}
