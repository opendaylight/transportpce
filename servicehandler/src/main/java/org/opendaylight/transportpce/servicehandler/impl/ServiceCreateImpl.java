/*
 * Copyright © 2024 Orange, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.servicehandler.impl;

import com.google.common.util.concurrent.ListenableFuture;
import org.opendaylight.mdsal.binding.api.NotificationPublishService;
import org.opendaylight.transportpce.common.OperationResult;
import org.opendaylight.transportpce.common.ResponseCodes;
import org.opendaylight.transportpce.servicehandler.ModelMappingUtils;
import org.opendaylight.transportpce.servicehandler.ServiceInput;
import org.opendaylight.transportpce.servicehandler.impl.ServicehandlerImpl.LogMessages;
import org.opendaylight.transportpce.servicehandler.listeners.NetworkListener;
import org.opendaylight.transportpce.servicehandler.listeners.PceListener;
import org.opendaylight.transportpce.servicehandler.listeners.RendererListener;
import org.opendaylight.transportpce.servicehandler.service.PCEServiceWrapper;
import org.opendaylight.transportpce.servicehandler.service.ServiceDataStoreOperations;
import org.opendaylight.transportpce.servicehandler.validation.ServiceCreateValidation;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev240205.PathComputationRequestOutput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev230526.RpcActions;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev230526.configuration.response.common.ConfigurationResponseCommon;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.state.types.rev191129.State;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev230526.ServiceCreate;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev230526.ServiceCreateInput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev230526.ServiceCreateOutput;
import org.opendaylight.yang.gen.v1.nbi.notifications.rev230728.PublishNotificationProcessService;
import org.opendaylight.yang.gen.v1.nbi.notifications.rev230728.PublishNotificationProcessServiceBuilder;
import org.opendaylight.yang.gen.v1.nbi.notifications.rev230728.notification.process.service.ServiceAEndBuilder;
import org.opendaylight.yang.gen.v1.nbi.notifications.rev230728.notification.process.service.ServiceZEndBuilder;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class ServiceCreateImpl implements ServiceCreate {
    private static final Logger LOG = LoggerFactory.getLogger(ServiceCreateImpl.class);
    private static final String SERVICE_CREATE_MSG = "serviceCreate: {}";
    private static final String PUBLISHER = "ServiceHandler";

    private ServiceDataStoreOperations serviceDataStoreOperations;
    private PceListener pceListener;
    private RendererListener rendererListener;
    private NetworkListener networkListener;
    private PCEServiceWrapper pceServiceWrapper;
    private NotificationPublishService notificationPublishService;

    public ServiceCreateImpl(final ServiceDataStoreOperations serviceDataStoreOperations,
            final PceListener pceListener, RendererListener rendererListener, NetworkListener networkListener,
            PCEServiceWrapper pceServiceWrapper, NotificationPublishService notificationPublishService) {
        this.serviceDataStoreOperations = serviceDataStoreOperations;
        this.pceListener = pceListener;
        this.rendererListener = rendererListener;
        this.networkListener = networkListener;
        this.pceServiceWrapper = pceServiceWrapper;
        this.notificationPublishService = notificationPublishService;
    }

    @Override
    public ListenableFuture<RpcResult<ServiceCreateOutput>> invoke(ServiceCreateInput input) {
        LOG.info("RPC serviceCreate received");
        // Validation
        OperationResult validationResult = ServiceCreateValidation.validateServiceCreateRequest(
                new ServiceInput(input), RpcActions.ServiceCreate);
        if (!validationResult.isSuccess()) {
            LOG.warn(SERVICE_CREATE_MSG, LogMessages.ABORT_VALID_FAILED);
            return ModelMappingUtils.createCreateServiceReply(
                    input, ResponseCodes.FINAL_ACK_YES,
                    validationResult.getResultMessage(), ResponseCodes.RESPONSE_FAILED);
        }
        //Check any presence of services with the same nameequipmentNotification
        String serviceName = input.getServiceName();
        if (this.serviceDataStoreOperations.getService(serviceName).isPresent()) {
            LOG.warn(SERVICE_CREATE_MSG, LogMessages.serviceInDS(serviceName));
            return ModelMappingUtils.createCreateServiceReply(input, ResponseCodes.FINAL_ACK_YES,
                    LogMessages.serviceInDS(serviceName), ResponseCodes.RESPONSE_FAILED);
        }
        // TODO: Here we also have to check if there is an associated temp-service.
        // TODO: If there is one, delete it from the temp-service-list??
        this.pceListener.setInput(new ServiceInput(input));
        this.pceListener.setServiceReconfigure(false);
        this.pceListener.setServiceFeasiblity(false);
        this.pceListener.setTempService(false);
        this.pceListener.setserviceDataStoreOperations(this.serviceDataStoreOperations);
        this.rendererListener.setserviceDataStoreOperations(serviceDataStoreOperations);
        this.rendererListener.setServiceInput(new ServiceInput(input));
        // This ensures that the temp-service boolean is false, especially, when
        // service-create is initiated after the temp-service-create
        this.rendererListener.setTempService(false);
        this.networkListener.setserviceDataStoreOperations(serviceDataStoreOperations);
        LOG.debug(SERVICE_CREATE_MSG, LogMessages.PCE_CALLING);
        PathComputationRequestOutput output = this.pceServiceWrapper.performPCE(input, true);
        if (output == null) {
            LOG.warn(SERVICE_CREATE_MSG, LogMessages.ABORT_PCE_FAILED);
            sendNbiNotification(new PublishNotificationProcessServiceBuilder()
                    .setServiceName(serviceName)
                    .setServiceAEnd(new ServiceAEndBuilder(input.getServiceAEnd()).build())
                    .setServiceZEnd(new ServiceZEndBuilder(input.getServiceZEnd()).build())
                    .setCommonId(input.getCommonId())
                    .setConnectionType(input.getConnectionType())
                    .setResponseFailed(LogMessages.ABORT_PCE_FAILED)
                    .setMessage("ServiceCreate request failed ...")
                    .setOperationalState(State.Degraded)
                    .setPublisherName(PUBLISHER)
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

    /**
     * Send notification to NBI notification in order to publish message.
     * @param service PublishNotificationService
     */
    private void sendNbiNotification(PublishNotificationProcessService service) {
        try {
            notificationPublishService.putNotification(service);
        } catch (InterruptedException e) {
            LOG.warn("Cannot send notification to nbi", e);
            Thread.currentThread().interrupt();
        }
    }

}
