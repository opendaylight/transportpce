/*
 * Copyright © 2024 Orange, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.servicehandler.impl;

import com.google.common.util.concurrent.ListenableFuture;
import java.util.Optional;
import org.opendaylight.mdsal.binding.api.NotificationPublishService;
import org.opendaylight.transportpce.common.ResponseCodes;
import org.opendaylight.transportpce.servicehandler.ModelMappingUtils;
import org.opendaylight.transportpce.servicehandler.ServiceInput;
import org.opendaylight.transportpce.servicehandler.impl.ServicehandlerImpl.LogMessages;
import org.opendaylight.transportpce.servicehandler.listeners.NetworkListener;
import org.opendaylight.transportpce.servicehandler.listeners.PceListener;
import org.opendaylight.transportpce.servicehandler.listeners.RendererListener;
import org.opendaylight.transportpce.servicehandler.service.RendererServiceWrapper;
import org.opendaylight.transportpce.servicehandler.service.ServiceDataStoreOperations;
import org.opendaylight.transportpce.servicehandler.validation.checks.ComplianceCheckResult;
import org.opendaylight.transportpce.servicehandler.validation.checks.ServicehandlerComplianceCheck;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev230526.RpcActions;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev230526.ServiceNotificationTypes;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev230526.configuration.response.common.ConfigurationResponseCommon;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.state.types.rev191129.State;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev230526.ServiceDelete;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev230526.ServiceDeleteInput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev230526.ServiceDeleteOutput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev230526.service.list.Services;
import org.opendaylight.yang.gen.v1.nbi.notifications.rev230728.PublishNotificationProcessService;
import org.opendaylight.yang.gen.v1.nbi.notifications.rev230728.PublishNotificationProcessServiceBuilder;
import org.opendaylight.yang.gen.v1.nbi.notifications.rev230728.notification.process.service.ServiceAEndBuilder;
import org.opendaylight.yang.gen.v1.nbi.notifications.rev230728.notification.process.service.ServiceZEndBuilder;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class ServiceDeleteImpl implements ServiceDelete {
    private static final Logger LOG = LoggerFactory.getLogger(ServiceDeleteImpl.class);
    private static final String SERVICE_DELETE_MSG = "serviceDelete: {}";
    private static final String PUBLISHER = "ServiceHandler";

    private ServiceDataStoreOperations serviceDataStoreOperations;
    private PceListener pceListener;
    private RendererListener rendererListener;
    private NetworkListener networkListener;
    private RendererServiceWrapper rendererServiceWrapper;
    private NotificationPublishService notificationPublishService;

    public ServiceDeleteImpl(final ServiceDataStoreOperations serviceDataStoreOperations,
            final PceListener pceListener, RendererListener rendererListener, NetworkListener networkListener,
            RendererServiceWrapper rendererServiceWrapper,
            NotificationPublishService notificationPublishService) {
        this.serviceDataStoreOperations = serviceDataStoreOperations;
        this.pceListener = pceListener;
        this.rendererListener = rendererListener;
        this.networkListener = networkListener;
        this.rendererServiceWrapper = rendererServiceWrapper;
        this.notificationPublishService = notificationPublishService;
    }

    @Override
    public ListenableFuture<RpcResult<ServiceDeleteOutput>> invoke(ServiceDeleteInput input) {
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
        if (serviceOpt.isEmpty()) {
            LOG.warn(SERVICE_DELETE_MSG, LogMessages.serviceNotInDS(serviceName));
            return ModelMappingUtils.createDeleteServiceReply(
                    input, ResponseCodes.FINAL_ACK_YES,
                    LogMessages.serviceNotInDS(serviceName), ResponseCodes.RESPONSE_FAILED);
        }
        service = serviceOpt.orElseThrow();
        LOG.debug("serviceDelete: Service '{}' found in datastore", serviceName);
        this.pceListener.setInput(new ServiceInput(input));
        this.pceListener.setServiceReconfigure(false);
        this.pceListener.setTempService(false);
        this.pceListener.setserviceDataStoreOperations(this.serviceDataStoreOperations);
        this.rendererListener.setTempService(false);
        this.rendererListener.setserviceDataStoreOperations(serviceDataStoreOperations);
        this.rendererListener.setServiceInput(new ServiceInput(input));
        this.networkListener.setserviceDataStoreOperations(serviceDataStoreOperations);
        org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.renderer.rev210915.ServiceDeleteInput
                serviceDeleteInput = ModelMappingUtils.createServiceDeleteInput(new ServiceInput(input));
        org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.renderer.rev210915.ServiceDeleteOutput output =
            this.rendererServiceWrapper.performRenderer(
                serviceDeleteInput, ServiceNotificationTypes.ServiceDeleteResult, service);
        if (output == null) {
            LOG.error(SERVICE_DELETE_MSG, LogMessages.RENDERER_DELETE_FAILED);
            sendNbiNotification(new PublishNotificationProcessServiceBuilder()
                    .setServiceName(service.getServiceName())
                    .setServiceAEnd(new ServiceAEndBuilder(service.getServiceAEnd()).build())
                    .setServiceZEnd(new ServiceZEndBuilder(service.getServiceZEnd()).build())
                    .setCommonId(service.getCommonId())
                    .setConnectionType(service.getConnectionType())
                    .setMessage("ServiceDelete request failed ...")
                    .setOperationalState(State.InService)
                    .setResponseFailed(LogMessages.RENDERER_DELETE_FAILED)
                    .setPublisherName(PUBLISHER)
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
