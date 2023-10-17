/*
 * Copyright © 2017 Orange, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.servicehandler.listeners;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Set;
import org.opendaylight.mdsal.binding.api.NotificationPublishService;
import org.opendaylight.mdsal.binding.api.NotificationService.CompositeListener;
import org.opendaylight.transportpce.common.OperationResult;
import org.opendaylight.transportpce.pce.service.PathComputationService;
import org.opendaylight.transportpce.renderer.provisiondevice.RendererServiceOperations;
import org.opendaylight.transportpce.servicehandler.ModelMappingUtils;
import org.opendaylight.transportpce.servicehandler.ServiceInput;
import org.opendaylight.transportpce.servicehandler.service.PCEServiceWrapper;
import org.opendaylight.transportpce.servicehandler.service.ServiceDataStoreOperations;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev220808.PathComputationRequestOutputBuilder;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev220808.ServicePathRpcResult;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev220808.service.path.rpc.result.PathDescription;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev220808.service.path.rpc.result.PathDescriptionBuilder;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.renderer.rev210915.ServiceImplementationRequestInput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.state.types.rev191129.State;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev230526.service.list.Services;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.service.types.rev220118.RpcStatusEx;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.service.types.rev220118.response.parameters.sp.ResponseParametersBuilder;
import org.opendaylight.yang.gen.v1.nbi.notifications.rev230726.PublishNotificationProcessService;
import org.opendaylight.yang.gen.v1.nbi.notifications.rev230726.PublishNotificationProcessServiceBuilder;
import org.opendaylight.yang.gen.v1.nbi.notifications.rev230726.notification.process.service.ServiceAEndBuilder;
import org.opendaylight.yang.gen.v1.nbi.notifications.rev230726.notification.process.service.ServiceZEndBuilder;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(service = {PceNotificationHandler.class, PceListener.class})
public class PceNotificationHandler implements PceListener {

    private static final Logger LOG = LoggerFactory.getLogger(PceNotificationHandler.class);
    private static final String PUBLISHER = "PceListener";

    private ServicePathRpcResult servicePathRpcResult;
    private RendererServiceOperations rendererServiceOperations;
    private ServiceDataStoreOperations serviceDataStoreOperations;
    private PCEServiceWrapper pceServiceWrapper;
    private ServiceInput input;
    private Boolean serviceReconfigure;
    private Boolean tempService;
    private Boolean serviceFeasiblity;
    private NotificationPublishService notificationPublishService;

    @Activate
    public PceNotificationHandler(
            @Reference RendererServiceOperations rendererServiceOperations,
            @Reference PathComputationService pathComputationService,
            @Reference NotificationPublishService notificationPublishService,
            @Reference ServiceDataStoreOperations serviceDataStoreOperations) {
        this.rendererServiceOperations = rendererServiceOperations;
        this.pceServiceWrapper = new PCEServiceWrapper(pathComputationService, notificationPublishService);
        this.serviceDataStoreOperations = serviceDataStoreOperations;
        setServiceReconfigure(false);
        setInput(null);
        setTempService(false);
        setServiceFeasiblity(false);
        this.notificationPublishService = notificationPublishService;
    }

    public CompositeListener getCompositeListener() {
        return new CompositeListener(Set.of(
            new CompositeListener.Component<>(ServicePathRpcResult.class, this::onServicePathRpcResult)));
    }

    private void onServicePathRpcResult(ServicePathRpcResult notification) {
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
        PathDescription pathDescription =
            new PathDescriptionBuilder()
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
            operationResult =
                    this.serviceDataStoreOperations.createTempService(
                            input.getTempServiceCreateInput(), pathDescription);
            if (!operationResult.isSuccess()) {
                LOG.error("Temp Service not created in datastore !");
            }
        } else {
            operationResult = this.serviceDataStoreOperations.createService(input.getServiceCreateInput());
            if (!operationResult.isSuccess()) {
                LOG.error("Service not created in datastore !");
            }
        }
        if (!this.serviceDataStoreOperations
                .createServicePath(
                    input,
                    //pceResponse
                    new PathComputationRequestOutputBuilder()
                        .setResponseParameters(
                            new ResponseParametersBuilder()
                                .setPathDescription(
                                    new org.opendaylight.yang.gen.v1
                                            .http.org.transportpce.b.c._interface.service.types.rev220118
                                                .response.parameters.sp.response.parameters
                                                    .PathDescriptionBuilder(pathDescription)
                                        .build())
                                .build())
                        .build())
                .isSuccess()) {
            LOG.error("Service Path not created in datastore !");
        }
        ServiceImplementationRequestInput serviceImplementationRequest =
            ModelMappingUtils.createServiceImplementationRequest(input, pathDescription);
        LOG.info("Sending serviceImplementation request : {}", serviceImplementationRequest);
        LOG.debug("Temp-service value is {}", tempService);
        this.rendererServiceOperations.serviceImplementation(serviceImplementationRequest, tempService);
    }

    /**
     * Check status of notification and send nbi notification.
     * @param notification ServicePathRpcResult the notification to check.
     * @return true is status is Successful, false otherwise.
     */
    private boolean checkStatus(ServicePathRpcResult notification) {
        PublishNotificationProcessService nbiNotification = getPublishNotificationProcessService(notification);
        PublishNotificationProcessServiceBuilder publishNotificationProcessServiceBuilder =
                new PublishNotificationProcessServiceBuilder(nbiNotification);
        //TODO is it worth to instantiate the 2 variables above if status is 'Pending' or 'Successful' ?
        switch (servicePathRpcResult.getStatus()) {
            case Failed:
                LOG.error("PCE path computation failed !");
                nbiNotification = publishNotificationProcessServiceBuilder
                        .setMessage("ServiceCreate request failed ...")
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
                nbiNotification = publishNotificationProcessServiceBuilder
                        .setMessage("ServiceCreate request failed ...")
                        .setResponseFailed("PCE path computation returned an unknown RpcStatusEx code!")
                        .setOperationalState(State.Degraded).build();
                sendNbiNotification(nbiNotification);
                return false;
        }
    }

    private PublishNotificationProcessService getPublishNotificationProcessService(ServicePathRpcResult notification) {
        if (input == null) {
            return new PublishNotificationProcessServiceBuilder()
                .setServiceName(notification.getServiceName())
                .setPublisherName(PUBLISHER)
                .build();
        }
        return new PublishNotificationProcessServiceBuilder()
            .setServiceName(input.getServiceName())
            .setServiceAEnd(new ServiceAEndBuilder(input.getServiceAEnd()).build())
            .setServiceZEnd(new ServiceZEndBuilder(input.getServiceZEnd()).build())
            .setCommonId(input.getCommonId())
            .setConnectionType(input.getConnectionType())
            .setPublisherName(PUBLISHER)
            .build();
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
        PublishNotificationProcessServiceBuilder nbiNotificationBuilder;
        State serviceOpState;
        if (tempService) {
            org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev230526.temp.service.list.Services
                    tempServiceList = serviceDataStoreOperations.getTempService(input.getServiceName()).orElseThrow();
            serviceOpState = tempServiceList.getOperationalState();
            nbiNotificationBuilder =
                    new PublishNotificationProcessServiceBuilder()
                            .setServiceAEnd(new ServiceAEndBuilder(tempServiceList.getServiceAEnd()).build())
                            .setServiceZEnd(new ServiceZEndBuilder(tempServiceList.getServiceZEnd()).build())
                            .setCommonId(tempServiceList.getCommonId())
                            .setConnectionType(tempServiceList.getConnectionType())
                            .setIsTempService(true)
                            .setPublisherName(PUBLISHER);
        } else {
            Services service = serviceDataStoreOperations.getService(input.getServiceName()).orElseThrow();
            serviceOpState = service.getOperationalState();
            nbiNotificationBuilder =
                    new PublishNotificationProcessServiceBuilder()
                            .setServiceName(service.getServiceName())
                            .setServiceAEnd(new ServiceAEndBuilder(service.getServiceAEnd()).build())
                            .setServiceZEnd(new ServiceZEndBuilder(service.getServiceZEnd()).build())
                            .setCommonId(service.getCommonId())
                            .setIsTempService(false)
                            .setConnectionType(service.getConnectionType())
                            .setPublisherName(PUBLISHER);

        }

        if (servicePathRpcResult.getStatus() == RpcStatusEx.Failed) {
            LOG.info("PCE cancel resource failed !");
            sendNbiNotification(
                nbiNotificationBuilder
                    .setResponseFailed("PCE cancel resource failed !")
                    .setMessage("ServiceDelete request failed ...")
                    .setOperationalState(serviceOpState)
                    .build());
            return;
        }
        LOG.info("PCE cancel resource done OK !");
        // Here the input refers to the transportPCE API and the serviceName will be commonId for temp-service
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
            sendNbiNotification(
                nbiNotificationBuilder
                    .setResponseFailed("")
                    .setMessage("{} Service deleted !")
                    .setOperationalState(State.Degraded)
                    .build());
        } else {
            LOG.warn("{} Service was not removed from datastore !", serviceType);
            sendNbiNotification(
                nbiNotificationBuilder
                    .setResponseFailed(serviceType + "Service was not removed from datastore !")
                    .setMessage("ServiceDelete request failed ...")
                    .setOperationalState(serviceOpState)
                    .build());
        }
        // TODO: should we re-initialize the temp-service boolean to false?
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

    @Override
    public void setInput(ServiceInput serviceInput) {
        this.input = serviceInput;
    }

    @Override
    public void setServiceReconfigure(Boolean serv) {
        this.serviceReconfigure = serv;
    }

    @Override
    public void setserviceDataStoreOperations(ServiceDataStoreOperations serviceData) {
        this.serviceDataStoreOperations = serviceData;
    }

    @Override
    public void setTempService(Boolean tempService) {
        this.tempService = tempService;
    }

    @Override
    public void setServiceFeasiblity(Boolean serviceFeasiblity) {
        this.serviceFeasiblity = serviceFeasiblity;
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
