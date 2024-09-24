/*
 * Copyright © 2017 Orange, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.servicehandler.listeners;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.opendaylight.mdsal.binding.api.NotificationPublishService;
import org.opendaylight.mdsal.binding.api.NotificationService.CompositeListener;
import org.opendaylight.transportpce.common.OperationResult;
import org.opendaylight.transportpce.common.StringConstants;
import org.opendaylight.transportpce.networkmodel.service.NetworkModelService;
import org.opendaylight.transportpce.pce.service.PathComputationService;
import org.opendaylight.transportpce.servicehandler.ServiceInput;
import org.opendaylight.transportpce.servicehandler.service.PCEServiceWrapper;
import org.opendaylight.transportpce.servicehandler.service.ServiceDataStoreOperations;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.networkutils.rev240923.OtnLinkType;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.renderer.rev210915.RendererRpcResultSp;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.renderer.rev210915.renderer.rpc.result.sp.Link;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.servicehandler.rev201125.ServiceRpcResultSh;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.servicehandler.rev201125.ServiceRpcResultShBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev230526.ServiceNotificationTypes;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.state.types.rev191129.State;
import org.opendaylight.yang.gen.v1.http.org.openroadm.equipment.states.types.rev191129.AdminStates;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev230526.ServiceRpcResult;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev230526.ServiceRpcResultBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev230526.service.list.Services;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev230526.service.rpc.result.PathComputationResultBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev230526.service.rpc.result.path.computation.result.AToZBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev230526.service.rpc.result.path.computation.result.ZToABuilder;
import org.opendaylight.yang.gen.v1.nbi.notifications.rev230728.PublishNotificationProcessService;
import org.opendaylight.yang.gen.v1.nbi.notifications.rev230728.PublishNotificationProcessServiceBuilder;
import org.opendaylight.yang.gen.v1.nbi.notifications.rev230728.notification.process.service.ServiceAEndBuilder;
import org.opendaylight.yang.gen.v1.nbi.notifications.rev230728.notification.process.service.ServiceZEndBuilder;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Calls to listen to Renderer notifications.
 *
 * @author Martial Coulibaly ( martial.coulibaly@gfi.com ) on behalf of Orange
 *
 */
@Component(service = {RendererNotificationHandler.class, RendererListener.class})
public class RendererNotificationHandler implements RendererListener {

    private static final String PUBLISHER = "RendererListener";
    private static final Logger LOG = LoggerFactory.getLogger(RendererNotificationHandler.class);
    private RendererRpcResultSp serviceRpcResultSp;
    private ServiceDataStoreOperations serviceDataStoreOperations;
    private ServiceInput input;
    private PCEServiceWrapper pceServiceWrapper;
    private Boolean tempService;
    private NotificationPublishService notificationPublishService;
    private final NetworkModelService networkModelService;

    @Activate
    public RendererNotificationHandler(@Reference PathComputationService pathComputationService,
            @Reference NotificationPublishService notificationPublishService,
            @Reference NetworkModelService networkModelService) {
        this.pceServiceWrapper = new PCEServiceWrapper(pathComputationService, notificationPublishService);
        setServiceInput(null);
        setTempService(false);
        this.notificationPublishService = notificationPublishService;
        this.networkModelService = networkModelService;
    }

    public CompositeListener getCompositeListener() {
        return new CompositeListener(Set.of(
            new CompositeListener.Component<>(RendererRpcResultSp.class, this::onRendererRpcResultSp)));
    }

    private void onRendererRpcResultSp(RendererRpcResultSp notification) {
        if (compareServiceRpcResultSp(notification)) {
            LOG.warn("ServiceRpcResultSp already wired !");
            return;
        }
        serviceRpcResultSp = notification;
        int notifType = serviceRpcResultSp.getNotificationType().getIntValue();
        LOG.info("Renderer '{}' Notification received : {}", serviceRpcResultSp.getNotificationType().getName(),
                notification);
        switch (notifType) {
            /* service-implementation-request. */
            case 3:
                onServiceImplementationResult(notification);
                break;
            /* service-delete. */
            case 4:
                onServiceDeleteResult(notification);
                break;
            default:
                break;
        }
    }

    /**
     * Process service delete result for serviceName.
     * @param notification RendererRpcResultSp
     */
    private void onServiceDeleteResult(RendererRpcResultSp notification) {
        switch (serviceRpcResultSp.getStatus()) {
            case Successful:
                updateOtnTopology(notification, true);
                break;
            case Failed:
                LOG.error("Renderer service delete failed !");
                Services service = serviceDataStoreOperations.getService(input.getServiceName()).orElseThrow();
                sendNbiNotification(new PublishNotificationProcessServiceBuilder()
                        .setServiceName(service.getServiceName())
                        .setServiceAEnd(new ServiceAEndBuilder(service.getServiceAEnd()).build())
                        .setServiceZEnd(new ServiceZEndBuilder(service.getServiceZEnd()).build())
                        .setCommonId(service.getCommonId())
                        .setIsTempService(false)
                        .setConnectionType(service.getConnectionType())
                        .setResponseFailed("Renderer service delete failed !")
                        .setMessage("ServiceDelete request failed ...")
                        .setOperationalState(service.getOperationalState())
                        .setPublisherName(PUBLISHER)
                        .build());
                return;
            case Pending:
                LOG.warn("Renderer service delete returned a Pending RpcStatusEx code!");
                return;
            default:
                LOG.error("Renderer service delete returned an unknown RpcStatusEx code!");
                return;
        }
        LOG.info("Service '{}' deleted !", notification.getServiceName());
        if (this.input == null) {
            LOG.error("ServiceInput parameter is null !");
            return;
        }
        LOG.info("sending PCE cancel resource reserve for '{}'", this.input.getServiceName());
        this.pceServiceWrapper.cancelPCEResource(this.input.getServiceName(),
                ServiceNotificationTypes.ServiceDeleteResult);
        sendServiceHandlerNotification(notification, ServiceNotificationTypes.ServiceDeleteResult);
    }

    /**
     * Process service implementation result for serviceName.
     * @param notification RendererRpcResultSp
     */
    private void onServiceImplementationResult(RendererRpcResultSp notification) {
        switch (serviceRpcResultSp.getStatus()) {
            case Successful:
                onSuccededServiceImplementation(notification);
                break;
            case Failed:
                onFailedServiceImplementation(notification.getServiceName());
                break;
            case Pending:
                LOG.warn("Service Implementation still pending according to RpcStatusEx");
                break;
            default:
                LOG.warn("Service Implementation has an unknown RpcStatusEx code");
                break;
        }
    }

    /**
     * Process succeeded service implementation for service.
     * @param notification RendererRpcResultSp
     */
    private void onSuccededServiceImplementation(RendererRpcResultSp notification) {
        LOG.info("Service implemented !");
        if (serviceDataStoreOperations == null) {
            LOG.debug("serviceDataStoreOperations is null");
            return;
        }
        updateOtnTopology(notification, false);
        PublishNotificationProcessServiceBuilder nbiNotificationBuilder =
            new PublishNotificationProcessServiceBuilder()
                .setServiceAEnd(new ServiceAEndBuilder(input.getServiceAEnd()).build())
                .setServiceZEnd(new ServiceZEndBuilder(input.getServiceZEnd()).build())
                .setPublisherName(PUBLISHER);
        String serviceTemp = "";
        if (tempService) {
            nbiNotificationBuilder.setCommonId(input.getCommonId()).setConnectionType(input.getConnectionType());
            nbiNotificationBuilder.setIsTempService(true);
            if (input.getServiceName() != null) {
                nbiNotificationBuilder.setServiceName(input.getServiceName());
            }
            OperationResult operationResult = this.serviceDataStoreOperations.modifyTempService(
                    serviceRpcResultSp.getServiceName(), State.InService, AdminStates.InService);
            serviceTemp = "Temp ";
            if (operationResult.isSuccess()) {
                ServiceRpcResult serviceRpcResult =
                    sendServiceRpcResultNotification(notification, ServiceNotificationTypes.ServiceCreateResult);
                sendNbiNotification(nbiNotificationBuilder
                    .setResponseFailed("")
                    .setMessage("Temp Service implemented")
                    .setAToZ(
                        new org.opendaylight.yang.gen.v1.nbi.notifications.rev230728.notification.process.service
                                .AToZBuilder()
                            .setFrequency(serviceRpcResult.getPathComputationResult().getAToZ().getFrequency())
                            .setWidth(serviceRpcResult.getPathComputationResult().getAToZ().getWidth())
                            .setOpticalOperationalMode(serviceRpcResult.getPathComputationResult()
                                        .getAToZ().getOpticalOperationalMode())
                            // TODO: add GNSR, OSNR, min/max output powers
                            .build())
                    .setZToA(
                        new org.opendaylight.yang.gen.v1.nbi.notifications.rev230728.notification.process.service
                                .ZToABuilder()
                            .setFrequency(serviceRpcResult.getPathComputationResult().getZToA().getFrequency())
                            .setWidth(serviceRpcResult.getPathComputationResult().getZToA().getWidth())
                            .setOpticalOperationalMode(serviceRpcResult.getPathComputationResult()
                                        .getZToA().getOpticalOperationalMode())
                            // TODO: add GNSR, OSNR, min/max output powers
                            .build())
                    .setOperationalState(State.InService)
                    .build());
                LOG.debug("For the Temp service, sending notification on service-result-rpc");
                return;
            }
        } else {
            // Here the service is implemented and the tempService has to be deleted if present
            nbiNotificationBuilder.setServiceName(input.getServiceName()).setConnectionType(input.getConnectionType());
            // Make sure temp-service is false
            nbiNotificationBuilder.setIsTempService(false);
            String commonId = input.getCommonId();
            if (commonId != null) {
                nbiNotificationBuilder.setCommonId(commonId);
                if (this.serviceDataStoreOperations.getTempService(commonId).isPresent()) {
                    LOG.info("Temp-service exists with the common-Id {}", commonId);
                    // Delete the common-id from this temp-service-list here
                    OperationResult tempServiceListDelete = serviceDataStoreOperations.deleteTempService(commonId);
                    //TODO: Also need to delete the service-path from the transportpce-service-path list
                    this.serviceDataStoreOperations.deleteServicePath(commonId);
                    LOG.info("Result for temp-service-list with {} is {}", commonId, tempServiceListDelete);
                }
            }
            OperationResult operationResult = this.serviceDataStoreOperations.modifyService(
                    serviceRpcResultSp.getServiceName(), State.InService, AdminStates.InService);
            if (operationResult.isSuccess()) {
                sendNbiNotification(nbiNotificationBuilder
                    .setResponseFailed("")
                    .setMessage("Service implemented !")
                    .setOperationalState(State.InService)
                    .build());
                sendServiceHandlerNotification(notification, ServiceNotificationTypes.ServiceCreateResult);
                return;
            }
        }
        LOG.warn("{}Service status not updated in datastore !", serviceTemp);
        sendNbiNotification(nbiNotificationBuilder
            .setResponseFailed(serviceTemp + "Service status not updated in datastore !")
            .setMessage("ServiceCreate request failed ...")
            .setOperationalState(State.OutOfService)
            .build());
    }

    /**
     * Create and send service handler notification.
     * @param notification RendererRpcResultSp
     * @param type ServiceNotificationTypes
     */
    private void sendServiceHandlerNotification(
            RendererRpcResultSp notification, ServiceNotificationTypes type) {
        try {
            ServiceRpcResultSh serviceHandlerNotification = new ServiceRpcResultShBuilder()
                    .setAToZDirection(notification.getAToZDirection())
                    .setZToADirection(notification.getZToADirection())
                    .setServiceName(notification.getServiceName())
                    .setStatus(notification.getStatus())
                    .setStatusMessage(notification.getStatusMessage())
                    .setNotificationType(type)
                    .build();
            LOG.debug("Service update in datastore OK, sending notification {}", serviceHandlerNotification);
            notificationPublishService.putNotification(serviceHandlerNotification);
        } catch (InterruptedException e) {
            LOG.warn("Something went wrong while sending notification for service {}",
                    serviceRpcResultSp.getServiceName(), e);
            Thread.currentThread().interrupt();
        }
    }

    private ServiceRpcResult sendServiceRpcResultNotification(
            RendererRpcResultSp notification, ServiceNotificationTypes type) {
        try {
            ServiceRpcResult serviceRpcResult = new ServiceRpcResultBuilder()
                    .setServiceName(notification.getServiceName())
                    .setNotificationType(type)
                    .setStatusMessage(notification.getStatusMessage())
                    .setCommonId(notification.getCommonId())
                    .setPathComputationResult(new PathComputationResultBuilder()
                            .setAToZ(new AToZBuilder()
                                            .setFrequency(notification
                                                            .getAToZDirection()
                                                            .getCentralFrequency())
                                            .setWidth(notification
                                                            .getAToZDirection()
                                                            .getWidth())
                                            // TODO: here the optical operational mode should be set
                                            // A default value is set here
                                            .setOpticalOperationalMode("OR-W-400G-oFEC-63.1Gbd")
                                            // TODO: also set the GNSR, OSNR, power values
                                            .build())
                            .setZToA(new ZToABuilder()
                                            .setFrequency(notification
                                                            .getZToADirection()
                                                            .getCentralFrequency())
                                            .setWidth(notification
                                                            .getZToADirection()
                                                            .getWidth())
                                            // TODO: here the optical operational mode should be set
                                            // A default value is set here
                                            .setOpticalOperationalMode("OR-W-400G-oFEC-63.1Gbd")
                                            // TODO: also set the GNSR, OSNR, power values
                                            .build())
                            .build())
                    .build();
            LOG.info("Sending the notification for service-rpc-result {}", serviceRpcResult);
            notificationPublishService.putNotification(serviceRpcResult);
            return serviceRpcResult;
        } catch (InterruptedException e) {
            LOG.warn("Something went wrong while sending notification for service {}",
                    serviceRpcResultSp.getServiceName(), e);
            Thread.currentThread().interrupt();
        }
        return null;
    }



    /**
     * Process failed service implementation for serviceName.
     * @param serviceName String
     */
    private void onFailedServiceImplementation(String serviceName) {
        LOG.error("Renderer implementation failed !");
        Services service = serviceDataStoreOperations.getService(input.getServiceName()).orElseThrow();
        sendNbiNotification(new PublishNotificationProcessServiceBuilder()
                .setServiceName(service.getServiceName())
                .setServiceAEnd(new ServiceAEndBuilder(service.getServiceAEnd()).build())
                .setServiceZEnd(new ServiceZEndBuilder(service.getServiceZEnd()).build())
                .setCommonId(service.getCommonId())
                .setConnectionType(service.getConnectionType())
                .setIsTempService(false)
                .setResponseFailed("Renderer implementation failed !")
                .setMessage("ServiceCreate request failed ...")
                .setOperationalState(service.getOperationalState())
                .setPublisherName(PUBLISHER)
                .build());
        OperationResult deleteServicePathOperationResult =
                this.serviceDataStoreOperations.deleteServicePath(serviceName);
        if (!deleteServicePathOperationResult.isSuccess()) {
            LOG.warn("Service path was not removed from datastore!");
        }
        OperationResult deleteServiceOperationResult;
        String serviceType = "";
        if (tempService) {
            deleteServiceOperationResult = this.serviceDataStoreOperations.deleteTempService(serviceName);
            serviceType = "Temp ";
        } else {
            deleteServiceOperationResult = this.serviceDataStoreOperations.deleteService(serviceName);
        }
        if (!deleteServiceOperationResult.isSuccess()) {
            LOG.warn("{}Service was not removed from datastore!", serviceType);
        }
    }

    @SuppressFBWarnings(
        value = "ES_COMPARING_STRINGS_WITH_EQ",
        justification = "false positives, not strings but real object references comparisons")
    private Boolean compareServiceRpcResultSp(RendererRpcResultSp notification) {
        if (serviceRpcResultSp == null
                || serviceRpcResultSp.getNotificationType() != notification.getNotificationType()
                || serviceRpcResultSp.getServiceName() != notification.getServiceName()
                || serviceRpcResultSp.getStatus() != notification.getStatus()
                || serviceRpcResultSp.getStatusMessage() != notification.getStatusMessage()) {
            return false;
        }
        return true;
    }

    @Override
    public void setServiceInput(ServiceInput serviceInput) {
        this.input = serviceInput;
    }

    @Override
    public void setserviceDataStoreOperations(ServiceDataStoreOperations serviceData) {
        this.serviceDataStoreOperations = serviceData;
    }

    @Override
    public void setTempService(Boolean tempService) {
        this.tempService = tempService;
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


    private void updateOtnTopology(RendererRpcResultSp notification, boolean isDeletion) {
        Link link = notification.getLink();
        if (link == null && notification.getLinkId() == null) {
            return;
        }
        List<String> supportedLinkIds = null;
        if (notification.getLinkId() != null) {
            supportedLinkIds = new ArrayList<>(notification.getLinkId());
        }
        String serviceType = notification.getServiceType();
        switch (serviceType) {
            case StringConstants.SERVICE_TYPE_OTU4:
            case StringConstants.SERVICE_TYPE_OTUC2:
            case StringConstants.SERVICE_TYPE_OTUC3:
            case StringConstants.SERVICE_TYPE_OTUC4:
            case StringConstants.SERVICE_TYPE_ODU4:
            case StringConstants.SERVICE_TYPE_ODUC2:
            case StringConstants.SERVICE_TYPE_ODUC3:
            case StringConstants.SERVICE_TYPE_ODUC4:
                Map<String, OtnLinkType> otnLinkTypeMap = Map.of(
                    StringConstants.SERVICE_TYPE_OTU4, OtnLinkType.OTU4,
                    // TODO: need to change it when OtnLinkType is updated with enum
                    StringConstants.SERVICE_TYPE_OTUC2, OtnLinkType.OTUC4,
                    StringConstants.SERVICE_TYPE_OTUC3, OtnLinkType.OTUC4,
                    StringConstants.SERVICE_TYPE_OTUC4, OtnLinkType.OTUC4,
                    StringConstants.SERVICE_TYPE_ODU4, OtnLinkType.ODTU4,
                    // TODO: need to change it when OtnLinkType is updated with enum
                    StringConstants.SERVICE_TYPE_ODUC2, OtnLinkType.ODUC4,
                    StringConstants.SERVICE_TYPE_ODUC3, OtnLinkType.ODUC4,
                    StringConstants.SERVICE_TYPE_ODUC4, OtnLinkType.ODUC4);
                if (isDeletion) {
                    LOG.info("updating otn-topology removing links");
                    this.networkModelService.deleteOtnLinks(link, supportedLinkIds, otnLinkTypeMap.get(serviceType));
                } else {
                    LOG.info("updating otn-topology adding links");
                    this.networkModelService.createOtnLinks(link, supportedLinkIds, otnLinkTypeMap.get(serviceType));
                }
                break;
            case StringConstants.SERVICE_TYPE_1GE:
            case StringConstants.SERVICE_TYPE_10GE:
            case StringConstants.SERVICE_TYPE_100GE_M:
                Short tribPort = Short.valueOf(notification.getAToZDirection().getMinTribSlot().getValue()
                    .split("\\.")[0]);
                Short minTribSlot = Short.valueOf(notification.getAToZDirection().getMinTribSlot().getValue()
                    .split("\\.")[1]);
                Short maxTribSlot = Short.valueOf(notification.getAToZDirection().getMaxTribSlot().getValue()
                    .split("\\.")[1]);
                LOG.info("updating otn-topology node tps -tps and tpn pools");
                this.networkModelService.updateOtnLinks(link, supportedLinkIds,
                    notification.getAToZDirection().getRate(), tribPort, minTribSlot, maxTribSlot, isDeletion);
                break;
            case StringConstants.SERVICE_TYPE_100GE_S:
                this.networkModelService.updateOtnLinks(supportedLinkIds, isDeletion);
                break;
            default:
                LOG.warn("service-type {} not managed yet", serviceType);
                break;
        }
    }

}
