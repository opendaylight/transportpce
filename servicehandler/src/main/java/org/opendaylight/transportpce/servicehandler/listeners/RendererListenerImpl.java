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
import org.opendaylight.transportpce.common.StringConstants;
import org.opendaylight.transportpce.networkmodel.service.NetworkModelService;
import org.opendaylight.transportpce.pce.service.PathComputationService;
import org.opendaylight.transportpce.servicehandler.ServiceInput;
import org.opendaylight.transportpce.servicehandler.service.PCEServiceWrapper;
import org.opendaylight.transportpce.servicehandler.service.ServiceDataStoreOperations;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.renderer.rev210618.RendererRpcResultSp;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.renderer.rev210618.TransportpceRendererListener;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.renderer.rev210618.renderer.rpc.result.sp.Link;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.servicehandler.rev201125.ServiceRpcResultSh;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.servicehandler.rev201125.ServiceRpcResultShBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev190531.ServiceNotificationTypes;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.state.types.rev191129.State;
import org.opendaylight.yang.gen.v1.http.org.openroadm.equipment.states.types.rev191129.AdminStates;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev190531.service.list.Services;
import org.opendaylight.yang.gen.v1.http.transportpce.topology.rev210511.OtnLinkType;
import org.opendaylight.yang.gen.v1.nbi.notifications.rev210628.PublishNotificationService;
import org.opendaylight.yang.gen.v1.nbi.notifications.rev210628.PublishNotificationServiceBuilder;
import org.opendaylight.yang.gen.v1.nbi.notifications.rev210628.notification.service.ServiceAEndBuilder;
import org.opendaylight.yang.gen.v1.nbi.notifications.rev210628.notification.service.ServiceZEndBuilder;
import org.opendaylight.yangtools.yang.common.Uint32;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Calls to listen to Renderer notifications.
 *
 * @author Martial Coulibaly ( martial.coulibaly@gfi.com ) on behalf of Orange
 *
 */
public class RendererListenerImpl implements TransportpceRendererListener {

    private static final String PUBLISHER = "RendererListener";
    private static final Logger LOG = LoggerFactory.getLogger(RendererListenerImpl.class);
    private RendererRpcResultSp serviceRpcResultSp;
    private ServiceDataStoreOperations serviceDataStoreOperations;
    private ServiceInput input;
    private PCEServiceWrapper pceServiceWrapper;
    private Boolean tempService;
    private NotificationPublishService notificationPublishService;
    private final NetworkModelService networkModelService;


    public RendererListenerImpl(PathComputationService pathComputationService,
            NotificationPublishService notificationPublishService, NetworkModelService networkModelService) {
        this.pceServiceWrapper = new PCEServiceWrapper(pathComputationService, notificationPublishService);
        setServiceInput(null);
        setTempService(false);
        this.notificationPublishService = notificationPublishService;
        this.networkModelService = networkModelService;
    }

    @Override
    public void onRendererRpcResultSp(RendererRpcResultSp notification) {
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
                String serviceType = notification.getServiceType();
                switch (serviceType) {
                    case StringConstants.SERVICE_TYPE_1GE:
                    case StringConstants.SERVICE_TYPE_10GE:
                    case StringConstants.SERVICE_TYPE_100GE_M:
                        Short tribPort = Short.valueOf(notification.getAToZDirection().getMinTribSlot().getValue()
                            .split("\\.")[0]);
                        Short minTribSlot = Short.valueOf(notification.getAToZDirection().getMinTribSlot().getValue()
                            .split("\\.")[1]);
                        Short maxTribSlot = Short.valueOf(notification.getAToZDirection().getMaxTribSlot().getValue()
                            .split("\\.")[1]);
                        updateOtnTopology(notification.getLink(), true, notification.getServiceType(),
                            notification.getAToZDirection().getRate(), tribPort, minTribSlot, maxTribSlot);
                        break;
                    case StringConstants.SERVICE_TYPE_OTU4:
                    case StringConstants.SERVICE_TYPE_OTUC4:
                    case StringConstants.SERVICE_TYPE_ODU4:
                    case StringConstants.SERVICE_TYPE_ODUC4:
                        updateOtnTopology(notification.getLink(), true, notification.getServiceType(), null, null,
                            null, null);
                        break;
                    default:
                        break;
                }
                break;
            case Failed:
                LOG.error("Renderer service delete failed !");
                Services service = serviceDataStoreOperations.getService(input.getServiceName()).get();
                sendNbiNotification(new PublishNotificationServiceBuilder()
                        .setServiceName(service.getServiceName())
                        .setServiceAEnd(new ServiceAEndBuilder(service.getServiceAEnd()).build())
                        .setServiceZEnd(new ServiceZEndBuilder(service.getServiceZEnd()).build())
                        .setCommonId(service.getCommonId())
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
        String serviceType = notification.getServiceType();
        switch (serviceType) {
            case StringConstants.SERVICE_TYPE_1GE:
            case StringConstants.SERVICE_TYPE_10GE:
            case StringConstants.SERVICE_TYPE_100GE_M:
                Short tribPort = Short.valueOf(notification.getAToZDirection().getMinTribSlot().getValue()
                    .split("\\.")[0]);
                Short minTribSlot = Short.valueOf(notification.getAToZDirection().getMinTribSlot().getValue()
                    .split("\\.")[1]);
                Short maxTribSlot = Short.valueOf(notification.getAToZDirection().getMaxTribSlot().getValue()
                    .split("\\.")[1]);
                updateOtnTopology(notification.getLink(), false, notification.getServiceType(),
                    notification.getAToZDirection().getRate(), tribPort, minTribSlot, maxTribSlot);
                break;
            case StringConstants.SERVICE_TYPE_OTU4:
            case StringConstants.SERVICE_TYPE_OTUC4:
            case StringConstants.SERVICE_TYPE_ODU4:
            case StringConstants.SERVICE_TYPE_ODUC4:
                updateOtnTopology(notification.getLink(), false, notification.getServiceType(), null, null, null, null);
                break;
            default:
                break;
        }
        PublishNotificationServiceBuilder nbiNotificationBuilder = new PublishNotificationServiceBuilder()
                .setServiceName(input.getServiceName())
                .setServiceAEnd(new ServiceAEndBuilder(input.getServiceAEnd()).build())
                .setServiceZEnd(new ServiceZEndBuilder(input.getServiceZEnd()).build())
                .setCommonId(input.getCommonId()).setConnectionType(input.getConnectionType())
                .setPublisherName(PUBLISHER);
        OperationResult operationResult;
        String serviceTemp = "";
        if (tempService) {
            operationResult = this.serviceDataStoreOperations.modifyTempService(
                    serviceRpcResultSp.getServiceName(), State.InService, AdminStates.InService);
            serviceTemp = "Temp ";
        } else {
            operationResult = this.serviceDataStoreOperations.modifyService(
                    serviceRpcResultSp.getServiceName(), State.InService, AdminStates.InService);
        }
        if (operationResult.isSuccess()) {
            sendNbiNotification(nbiNotificationBuilder
                    .setResponseFailed("")
                    .setMessage("Service implemented !")
                    .setOperationalState(org.opendaylight.yang.gen.v1.http
                            .org.openroadm.common.state.types.rev181130.State.InService)
                    .build());
            if (!tempService) {
                sendServiceHandlerNotification(notification, ServiceNotificationTypes.ServiceCreateResult);
            }
        } else {
            LOG.warn("{}Service status not updated in datastore !", serviceTemp);
            sendNbiNotification(nbiNotificationBuilder
                    .setResponseFailed(serviceTemp + "Service status not updated in datastore !")
                    .setMessage("ServiceCreate request failed ...")
                    .setOperationalState(org.opendaylight.yang.gen.v1.http
                            .org.openroadm.common.state.types.rev181130.State.OutOfService)
                    .build());
        }
    }

    /**
     * Create and send service handler notification.
     * @param notification RendererRpcResultSp
     * @param type ServiceNotificationTypes
     */
    private void sendServiceHandlerNotification(RendererRpcResultSp notification, ServiceNotificationTypes type) {
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
            notificationPublishService.putNotification(
                    serviceHandlerNotification);
        } catch (InterruptedException e) {
            LOG.warn("Something went wrong while sending notification for service {}",
                    serviceRpcResultSp.getServiceName(), e);
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Process failed service implementation for serviceName.
     * @param serviceName String
     */
    private void onFailedServiceImplementation(String serviceName) {
        LOG.error("Renderer implementation failed !");
        Services service = serviceDataStoreOperations.getService(input.getServiceName()).get();
        sendNbiNotification(new PublishNotificationServiceBuilder()
                .setServiceName(service.getServiceName())
                .setServiceAEnd(new ServiceAEndBuilder(service.getServiceAEnd()).build())
                .setServiceZEnd(new ServiceZEndBuilder(service.getServiceZEnd()).build())
                .setCommonId(service.getCommonId())
                .setConnectionType(service.getConnectionType())
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
        if (deleteServiceOperationResult.isSuccess()) {
            LOG.warn("{}Service was not removed from datastore!", serviceType);
        }
    }

    @SuppressFBWarnings(
        value = "ES_COMPARING_STRINGS_WITH_EQ",
        justification = "false positives, not strings but real object references comparisons")
    private Boolean compareServiceRpcResultSp(RendererRpcResultSp notification) {
        if (serviceRpcResultSp == null) {
            return false;
        }
        if (serviceRpcResultSp.getNotificationType() != notification.getNotificationType()) {
            return false;
        }
        if (serviceRpcResultSp.getServiceName() != notification.getServiceName()) {
            return false;
        }
        if (serviceRpcResultSp.getStatus() != notification.getStatus()) {
            return false;
        }
        if (serviceRpcResultSp.getStatusMessage() != notification.getStatusMessage()) {
            return false;
        }
        return true;
    }

    public void setServiceInput(ServiceInput serviceInput) {
        this.input = serviceInput;
    }

    public void setserviceDataStoreOperations(ServiceDataStoreOperations serviceData) {
        this.serviceDataStoreOperations = serviceData;
    }

    public void setTempService(Boolean tempService) {
        this.tempService = tempService;
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

    private void updateOtnTopology(Link link, boolean isDeletion, String serviceType, Uint32 rate, Short portNb,
            Short minSlotNb, Short maxSlotNb) {
        if (link == null) {
            return;
        }
        OtnLinkType otnLinkType;
        switch (serviceType) {
            case StringConstants.SERVICE_TYPE_OTU4:
                otnLinkType = OtnLinkType.OTU4;
                break;
            case StringConstants.SERVICE_TYPE_OTUC4:
                otnLinkType = OtnLinkType.OTUC4;
                break;
            case StringConstants.SERVICE_TYPE_ODU4:
                otnLinkType = OtnLinkType.ODTU4;
                break;
            case StringConstants.SERVICE_TYPE_ODUC4:
                otnLinkType = OtnLinkType.ODUC4;
                break;
            default:
                otnLinkType = null;
                LOG.warn("No otn-link-type corresponds to service-type {}", serviceType);
                break;
        }
        switch (serviceType) {
            case StringConstants.SERVICE_TYPE_OTU4:
            case StringConstants.SERVICE_TYPE_OTUC4:
            case StringConstants.SERVICE_TYPE_ODU4:
            case StringConstants.SERVICE_TYPE_ODUC4:
                if (isDeletion) {
                    LOG.info("updating otn-topology removing links");
                    this.networkModelService.deleteOtnLinks(link.getATermination().getNodeId(),
                        link.getATermination().getTpId(), link.getZTermination().getNodeId(),
                        link.getZTermination().getTpId(), otnLinkType);
                } else {
                    LOG.info("updating otn-topology adding links");
                    this.networkModelService.createOtnLinks(link.getATermination().getNodeId(),
                        link.getATermination().getTpId(), link.getZTermination().getNodeId(),
                        link.getZTermination().getTpId(), otnLinkType);
                }
                break;
            case StringConstants.SERVICE_TYPE_1GE:
            case StringConstants.SERVICE_TYPE_10GE:
            case StringConstants.SERVICE_TYPE_100GE_M:
                LOG.info("updating otn-topology node tps -tps and tpn pools");
                this.networkModelService.updateOtnLinks(link, rate, portNb, minSlotNb, maxSlotNb, isDeletion);
                break;
            default:
                break;
        }
    }

}
