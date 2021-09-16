/*
 * Copyright © 2017 Orange, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.servicehandler.service;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import java.util.concurrent.Executors;
import org.opendaylight.mdsal.binding.api.NotificationPublishService;
import org.opendaylight.transportpce.common.ResponseCodes;
import org.opendaylight.transportpce.renderer.provisiondevice.RendererServiceOperations;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.renderer.rev210915.ServiceDeleteInput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.renderer.rev210915.ServiceDeleteInputBuilder;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.renderer.rev210915.ServiceDeleteOutput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.renderer.rev210915.ServiceDeleteOutputBuilder;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.servicehandler.rev201125.ServiceRpcResultSh;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.servicehandler.rev201125.ServiceRpcResultShBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev190531.ServiceNotificationTypes;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev190531.configuration.response.common.ConfigurationResponseCommon;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev190531.configuration.response.common.ConfigurationResponseCommonBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev190531.TempServiceDeleteInput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev190531.service.list.Services;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.service.types.rev200128.RpcStatusEx;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.service.types.rev200128.service.handler.header.ServiceHandlerHeader;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.service.types.rev200128.service.handler.header.ServiceHandlerHeaderBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class to call RendererServiceOperations.
 *
 * @author Martial Coulibaly ( martial.coulibaly@gfi.com ) on behalf of Orange
 *
 */
public class RendererServiceWrapper {

    private static final Logger LOG = LoggerFactory.getLogger(RendererServiceWrapper.class);
    private final RendererServiceOperations rendererServiceOperations;
    private final NotificationPublishService notificationPublishService;
    private ServiceRpcResultSh notification = null;
    private final ListeningExecutorService executor;

    public RendererServiceWrapper(RendererServiceOperations rendererServiceOperations,
            NotificationPublishService notificationPublishService) {
        this.rendererServiceOperations = rendererServiceOperations;
        this.notificationPublishService = notificationPublishService;
        this.executor = MoreExecutors.listeningDecorator(Executors.newFixedThreadPool(5));
    }

    private void sendNotifications(ServiceRpcResultSh notif) {
        try {
            notificationPublishService.putNotification(notif);
        } catch (InterruptedException e) {
            LOG.info("notification offer rejected : ", e);
        }
    }

    public ServiceDeleteOutput performRenderer(ServiceDeleteInput serviceDeleteInput,
            ServiceNotificationTypes notifType, Services service) {
        if (validateParams(serviceDeleteInput.getServiceName(), serviceDeleteInput.getServiceHandlerHeader(), false)) {
            return performRenderer(serviceDeleteInput.getServiceName(), serviceDeleteInput.getServiceHandlerHeader(),
                    ServiceNotificationTypes.ServiceDeleteResult, service);
        } else {
            return returnRendererFailed();
        }
    }

    public ServiceDeleteOutput performRenderer(TempServiceDeleteInput tempServiceDeleteInput,
            ServiceNotificationTypes notifType) {
        String commonId = tempServiceDeleteInput.getCommonId();
        if (validateParams(commonId, null, true)) {
            ServiceHandlerHeader serviceHandler = new ServiceHandlerHeaderBuilder().setRequestId(commonId).build();
            return performRenderer(tempServiceDeleteInput.getCommonId(), serviceHandler,
                    ServiceNotificationTypes.ServiceDeleteResult, null);
        } else {
            return returnRendererFailed();
        }
    }

    private ServiceDeleteOutput performRenderer(String serviceName, ServiceHandlerHeader serviceHandlerHeader,
            ServiceNotificationTypes notifType, Services service) {
        notification = new ServiceRpcResultShBuilder().setNotificationType(notifType).setServiceName(serviceName)
                .setStatus(RpcStatusEx.Pending)
                .setStatusMessage("Service compliant, submitting temp service delete Request ...").build();
        sendNotifications(notification);
        FutureCallback<ServiceDeleteOutput> rendererCallback =
                new ServiceDeleteOutputFutureCallback(notifType, serviceName);
        ServiceDeleteInput serviceDeleteInput = createRendererRequestInput(serviceName, serviceHandlerHeader);
        ListenableFuture<ServiceDeleteOutput> renderer =
                this.rendererServiceOperations.serviceDelete(serviceDeleteInput, service);
        Futures.addCallback(renderer, rendererCallback, executor);
        ConfigurationResponseCommon value =
                new ConfigurationResponseCommonBuilder().setAckFinalIndicator(ResponseCodes.FINAL_ACK_NO)
                        .setRequestId(serviceDeleteInput.getServiceHandlerHeader().getRequestId())
                        .setResponseCode(ResponseCodes.RESPONSE_OK)
                        .setResponseMessage("Renderer service delete in progress").build();
        return new ServiceDeleteOutputBuilder().setConfigurationResponseCommon(value).build();
    }

    private ServiceDeleteInput createRendererRequestInput(String serviceName,
            ServiceHandlerHeader serviceHandlerHeader) {
        LOG.info("Mapping ServiceDeleteInput or TempServiceDelete to Renderer requests");
        return new ServiceDeleteInputBuilder().setServiceHandlerHeader(serviceHandlerHeader).setServiceName(serviceName)
                .build();
    }

    private static ServiceDeleteOutput returnRendererFailed() {
        ConfigurationResponseCommon configurationResponseCommon = new ConfigurationResponseCommonBuilder()
                .setAckFinalIndicator(ResponseCodes.FINAL_ACK_YES).setResponseCode(ResponseCodes.RESPONSE_FAILED)
                .setResponseMessage("Renderer service delete failed !").build();
        return new ServiceDeleteOutputBuilder().setConfigurationResponseCommon(configurationResponseCommon).build();
    }

    private Boolean validateParams(String serviceName, ServiceHandlerHeader serviceHandlerHeader, boolean temp) {
        boolean result = true;
        if (!checkString(serviceName)) {
            result = false;
            LOG.error("Service Name (common-id for Temp service) is not set");
        } else if (!temp && (serviceHandlerHeader == null)) {
            LOG.error("Service serviceHandlerHeader 'request-id' is not set");
            result = false;
        }
        return result;
    }

    private static boolean checkString(String value) {
        return ((value != null) && (value.compareTo("") != 0));
    }

    private final class ServiceDeleteOutputFutureCallback implements FutureCallback<ServiceDeleteOutput> {
        private final ServiceNotificationTypes notifType;
        private final String serviceName;
        String message = "";
        ServiceRpcResultSh notification = null;

        private ServiceDeleteOutputFutureCallback(ServiceNotificationTypes notifType, String serviceName) {
            this.notifType = notifType;
            this.serviceName = serviceName;
        }

        @Override
        public void onSuccess(ServiceDeleteOutput response) {
            if (response != null) {
                /**
                 * If PCE reply is received before timer expiration with a positive result, a
                 * service is created with admin and operational status 'down'.
                 */
                message = "Renderer replied to service delete Request !";
                LOG.info("Renderer replied to service delete Request : {}", response);
                notification = new ServiceRpcResultShBuilder().setNotificationType(notifType)
                        .setServiceName(serviceName).setStatus(RpcStatusEx.Successful).setStatusMessage(message)
                        .build();
                sendNotifications(notification);
            } else {
                message = "Renderer service delete failed ";
                notification = new ServiceRpcResultShBuilder().setNotificationType(notifType).setServiceName("")
                        .setStatus(RpcStatusEx.Failed).setStatusMessage(message).build();
                sendNotifications(notification);
            }
        }

        @Override
        public void onFailure(Throwable arg0) {
            LOG.error("Renderer service delete failed !");
            notification = new ServiceRpcResultShBuilder().setNotificationType(notifType).setServiceName(serviceName)
                    .setStatus(RpcStatusEx.Failed)
                    .setStatusMessage("Renderer service delete request failed  : " + arg0.getMessage()).build();
            sendNotifications(notification);
        }
    }
}

