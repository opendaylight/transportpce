/*
 * Copyright © 2017 Orange, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.servicehandler.listeners;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.opendaylight.mdsal.binding.api.NotificationPublishService;
import org.opendaylight.transportpce.common.OperationResult;
import org.opendaylight.transportpce.pce.service.PathComputationService;
import org.opendaylight.transportpce.renderer.provisiondevice.RendererServiceOperations;
import org.opendaylight.transportpce.servicehandler.ModelMappingUtils;
import org.opendaylight.transportpce.servicehandler.ServiceInput;
import org.opendaylight.transportpce.servicehandler.service.PCEServiceWrapper;
import org.opendaylight.transportpce.servicehandler.service.ServiceDataStoreOperations;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev200128.PathComputationRequestOutput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev200128.PathComputationRequestOutputBuilder;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev200128.ServicePathRpcResult;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev200128.TransportpcePceListener;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev200128.service.path.rpc.result.PathDescription;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev200128.service.path.rpc.result.PathDescriptionBuilder;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.renderer.rev201125.ServiceImplementationRequestInput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.state.types.rev181130.State;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev190531.service.list.Services;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.service.types.rev200128.RpcStatusEx;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.service.types.rev200128.response.parameters.sp.ResponseParameters;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.service.types.rev200128.response.parameters.sp.ResponseParametersBuilder;
import org.opendaylight.yang.gen.v1.nbi.notifications.rev201130.PublishNotificationService;
import org.opendaylight.yang.gen.v1.nbi.notifications.rev201130.PublishNotificationServiceBuilder;
import org.opendaylight.yang.gen.v1.nbi.notifications.rev201130.notification.service.ServiceAEndBuilder;
import org.opendaylight.yang.gen.v1.nbi.notifications.rev201130.notification.service.ServiceZEndBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PceListenerImpl implements TransportpcePceListener {

    private static final Logger LOG = LoggerFactory.getLogger(PceListenerImpl.class);
    private static final String TOPIC = "PceListener";

    private ServicePathRpcResult servicePathRpcResult;
    private RendererServiceOperations rendererServiceOperations;
    private ServiceDataStoreOperations serviceDataStoreOperations;
    private PCEServiceWrapper pceServiceWrapper;
    private ServiceInput input;
    private Boolean serviceReconfigure;
    private Boolean tempService;
    private Boolean serviceFeasiblity;
    private NotificationPublishService notificationPublishService;

    public PceListenerImpl(RendererServiceOperations rendererServiceOperations,
            PathComputationService pathComputationService, NotificationPublishService notificationPublishService,
            ServiceDataStoreOperations serviceDataStoreOperations) {
        this.rendererServiceOperations = rendererServiceOperations;
        this.pceServiceWrapper = new PCEServiceWrapper(pathComputationService, notificationPublishService);
        this.serviceDataStoreOperations = serviceDataStoreOperations;
        setServiceReconfigure(false);
        setInput(null);
        setTempService(false);
        setServiceFeasiblity(false);
        this.notificationPublishService = notificationPublishService;
    }

    @Override
    public void onServicePathRpcResult(ServicePathRpcResult notification) {
        if (compareServicePathRpcResult(notification)) {
            LOG.warn("ServicePathRpcResult already wired !");
            return;
        }
        servicePathRpcResult = notification;
        switch (servicePathRpcResult.getNotificationType().getIntValue()) {
            /* path-computation-request. */
            case 1:
                onPathComputationResult(notification);
                break;
            /* cancel-resource-reserve. */
            case 2:
                onCancelResourceResult();
                break;
            default:
                break;
        }
    }

    /**
     * Process path computation request result.
     * @param notification the result notification.
     */
    private void onPathComputationResult(ServicePathRpcResult notification) {
        LOG.info("PCE '{}' Notification received : {}", servicePathRpcResult.getNotificationType().getName(),
                notification);
        if (!checkStatus(notification)) {
            return;
        }
        if (servicePathRpcResult.getPathDescription() == null) {
            LOG.error("'PathDescription' parameter is null ");
            return;
        }
        PathDescription pathDescription = new PathDescriptionBuilder()
                .setAToZDirection(servicePathRpcResult.getPathDescription().getAToZDirection())
                .setZToADirection(servicePathRpcResult.getPathDescription().getZToADirection())
                .build();
        LOG.info("PathDescription gets : {}", pathDescription);
        if (serviceFeasiblity) {
            LOG.warn("service-feasibility-check RPC ");
            return;
        }
        if (input == null) {
            LOG.error("Input is null !");
            return;
        }
        OperationResult operationResult = null;
        if (tempService) {
            operationResult = this.serviceDataStoreOperations.createTempService(input.getTempServiceCreateInput());
            if (!operationResult.isSuccess()) {
                LOG.error("Temp Service not created in datastore !");
            }
        } else {
            operationResult = this.serviceDataStoreOperations.createService(input.getServiceCreateInput());
            if (!operationResult.isSuccess()) {
                LOG.error("Service not created in datastore !");
            }
        }
        ResponseParameters responseParameters = new ResponseParametersBuilder()
                .setPathDescription(new org.opendaylight.yang.gen.v1.http
                        .org.transportpce.b.c._interface.service.types.rev200128
                        .response.parameters.sp.response.parameters.PathDescriptionBuilder(pathDescription).build())
                .build();
        PathComputationRequestOutput pceResponse = new PathComputationRequestOutputBuilder()
                .setResponseParameters(responseParameters).build();
        OperationResult operationServicePathSaveResult = this.serviceDataStoreOperations
                .createServicePath(input, pceResponse);
        if (!operationServicePathSaveResult.isSuccess()) {
            LOG.error("Service Path not created in datastore !");
        }
        ServiceImplementationRequestInput serviceImplementationRequest = ModelMappingUtils
                .createServiceImplementationRequest(input, pathDescription);
        LOG.info("Sending serviceImplementation request : {}", serviceImplementationRequest);
        this.rendererServiceOperations.serviceImplementation(serviceImplementationRequest);
    }

    /**
     * Check status of notification and send nbi notification.
     * @param notification ServicePathRpcResult the notification to check.
     * @return true is status is Successful, false otherwise.
     */
    private boolean checkStatus(ServicePathRpcResult notification) {
        PublishNotificationService nbiNotification = getPublishNotificationService(notification);
        PublishNotificationServiceBuilder publishNotificationServiceBuilder = new PublishNotificationServiceBuilder(
                nbiNotification);
        switch (servicePathRpcResult.getStatus()) {
            case Failed:
                LOG.error("PCE path computation failed !");
                nbiNotification = publishNotificationServiceBuilder.setMessage("ServiceCreate request failed ...")
                        .setResponseFailed("PCE path computation failed !")
                        .setOperationalState(State.Degraded).build();
                sendNbiNotification(nbiNotification);
                return false;
            case Pending:
                LOG.warn("PCE path computation returned a Pending RpcStatusEx code!");
                return false;
            case Successful:
                LOG.info("PCE calculation done OK !");
                return true;
            default:
                LOG.error("PCE path computation returned an unknown RpcStatusEx code {}",
                        servicePathRpcResult.getStatus());
                nbiNotification = publishNotificationServiceBuilder.setMessage("ServiceCreate request failed ...")
                        .setResponseFailed("PCE path computation returned an unknown RpcStatusEx code!")
                        .setOperationalState(State.Degraded).build();
                sendNbiNotification(nbiNotification);
                return false;
        }
    }

    private PublishNotificationService getPublishNotificationService(ServicePathRpcResult notification) {
        PublishNotificationServiceBuilder nbiNotificationBuilder = new PublishNotificationServiceBuilder();
        if (input != null) {
            nbiNotificationBuilder.setServiceName(input.getServiceName())
                    .setServiceAEnd(new ServiceAEndBuilder(input.getServiceAEnd()).build())
                    .setServiceZEnd(new ServiceZEndBuilder(input.getServiceZEnd()).build())
                    .setCommonId(input.getCommonId()).setConnectionType(input.getConnectionType());
        } else {
            nbiNotificationBuilder.setServiceName(notification.getServiceName());
        }
        nbiNotificationBuilder.setTopic(TOPIC);
        return nbiNotificationBuilder.build();
    }

    /**
     * Process cancel resource result.
     */
    private void onCancelResourceResult() {
        if (servicePathRpcResult.getStatus() == RpcStatusEx.Pending) {
            LOG.warn("PCE cancel returned a Pending RpcStatusEx code !");
            return;
        } else if (servicePathRpcResult.getStatus() != RpcStatusEx.Successful
                && servicePathRpcResult.getStatus() != RpcStatusEx.Failed) {
            LOG.error("PCE cancel returned an unknown RpcStatusEx code !");
            return;
        }
        Services service = serviceDataStoreOperations.getService(input.getServiceName()).get();
        PublishNotificationServiceBuilder nbiNotificationBuilder = new PublishNotificationServiceBuilder()
                .setServiceName(service.getServiceName())
                .setServiceAEnd(new ServiceAEndBuilder(service.getServiceAEnd()).build())
                .setServiceZEnd(new ServiceZEndBuilder(service.getServiceZEnd()).build())
                .setCommonId(service.getCommonId())
                .setConnectionType(service.getConnectionType())
                .setTopic(TOPIC);
        if (servicePathRpcResult.getStatus() == RpcStatusEx.Failed) {
            LOG.info("PCE cancel resource failed !");
            sendNbiNotification(nbiNotificationBuilder
                    .setResponseFailed("PCE cancel resource failed !")
                    .setMessage("ServiceDelete request failed ...")
                    .setOperationalState(service.getOperationalState())
                    .build());
            return;
        }
        LOG.info("PCE cancel resource done OK !");
        OperationResult deleteServicePathOperationResult =
                this.serviceDataStoreOperations.deleteServicePath(input.getServiceName());
        if (!deleteServicePathOperationResult.isSuccess()) {
            LOG.warn("Service path was not removed from datastore !");
        }
        OperationResult deleteServiceOperationResult;
        String serviceType = "";
        if (tempService) {
            deleteServiceOperationResult = this.serviceDataStoreOperations.deleteTempService(input.getServiceName());
            serviceType = "Temp ";
        } else {
            deleteServiceOperationResult = this.serviceDataStoreOperations.deleteService(input.getServiceName());
        }
        if (deleteServiceOperationResult.isSuccess()) {
            sendNbiNotification(nbiNotificationBuilder
                    .setResponseFailed("")
                    .setMessage("Service deleted !")
                    .setOperationalState(State.Degraded)
                    .build());
        } else {
            LOG.warn("{}Service was not removed from datastore !", serviceType);
            sendNbiNotification(nbiNotificationBuilder
                    .setResponseFailed(serviceType + "Service was not removed from datastore !")
                    .setMessage("ServiceDelete request failed ...")
                    .setOperationalState(service.getOperationalState())
                    .build());
        }
        /**
         * if it was an RPC serviceReconfigure, re-launch PCR.
         */
        if (this.serviceReconfigure) {
            LOG.info("cancel resource reserve done, relaunching PCE path computation ...");
            this.pceServiceWrapper.performPCE(input.getServiceCreateInput(), true);
            this.serviceReconfigure = false;
        }
    }

    @SuppressFBWarnings(
        value = "ES_COMPARING_STRINGS_WITH_EQ",
        justification = "false positives, not strings but real object references comparisons")
    private Boolean compareServicePathRpcResult(ServicePathRpcResult notification) {
        if (servicePathRpcResult == null) {
            return false;
        }
        if (servicePathRpcResult.getNotificationType() != notification.getNotificationType()) {
            return false;
        }
        if (servicePathRpcResult.getServiceName() != notification.getServiceName()) {
            return false;
        }
        if (servicePathRpcResult.getStatus() != notification.getStatus()) {
            return false;
        }
        if (servicePathRpcResult.getStatusMessage() != notification.getStatusMessage()) {
            return false;
        }
        return true;
    }

    public void setInput(ServiceInput serviceInput) {
        this.input = serviceInput;
    }

    public void setServiceReconfigure(Boolean serv) {
        this.serviceReconfigure = serv;
    }

    public void setserviceDataStoreOperations(ServiceDataStoreOperations serviceData) {
        this.serviceDataStoreOperations = serviceData;
    }

    public void setTempService(Boolean tempService) {
        this.tempService = tempService;
    }

    public void setServiceFeasiblity(Boolean serviceFeasiblity) {
        this.serviceFeasiblity = serviceFeasiblity;
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
