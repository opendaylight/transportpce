/*
 * Copyright © 2026 Orange, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.servicehandler.listeners;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.opendaylight.mdsal.binding.api.NotificationPublishService;
import org.opendaylight.mdsal.binding.api.NotificationService.CompositeListener;
import org.opendaylight.transportpce.common.OperationResult;
import org.opendaylight.transportpce.pce.service.PathComputationService;
import org.opendaylight.transportpce.pce.service.PathComputationServiceImpl;
import org.opendaylight.transportpce.renderer.provisiondevice.RendererServiceOperations;
import org.opendaylight.transportpce.servicehandler.ModelMappingUtils;
import org.opendaylight.transportpce.servicehandler.ServiceInput;
import org.opendaylight.transportpce.servicehandler.service.ServiceDataStoreOperations;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev260910.PathComputationRequestOutputBuilder;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev260910.SecondStepHybridPcResult;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev260910.second.step.hybrid.pc.result.AggregatedPathDescription;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev260910.second.step.hybrid.pc.result.aggregated.path.description.CdServicePathDescription;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev260910.second.step.hybrid.pc.result.aggregated.path.description.CdServicePathDescriptionKey;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev260910.service.path.rpc.result.PathDescription;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev260910.service.path.rpc.result.PathDescriptionBuilder;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.tapisbi.rev260410.TapiSbiServiceImplementationRequestInput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.state.types.rev191129.State;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev250530.service.list.Services;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev260422.DomainTypeEnum;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.service.types.rev260910.RpcStatusEx;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.service.types.rev260910.response.parameters.sp.ResponseParametersBuilder;
import org.opendaylight.yang.gen.v1.nbi.notifications.rev230728.PublishNotificationProcessService;
import org.opendaylight.yang.gen.v1.nbi.notifications.rev230728.PublishNotificationProcessServiceBuilder;
import org.opendaylight.yang.gen.v1.nbi.notifications.rev230728.notification.process.service.ServiceAEndBuilder;
import org.opendaylight.yang.gen.v1.nbi.notifications.rev230728.notification.process.service.ServiceZEndBuilder;
import org.opendaylight.yangtools.yang.common.Uint8;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
public class Pce2ndStepPCNotificationHandler {

    private static final Logger LOG = LoggerFactory.getLogger(Pce2ndStepPCNotificationHandler.class);
    private static final String PUBLISHER = "PceListener";

//    private ServicePathRpcResult servicePathRpcResult;
    private SecondStepHybridPcResult secondStepHybridPcResult;
//    private RendererServiceOperations rendererServiceOperations;
    private ServiceDataStoreOperations serviceDataStoreOperations;
//    private ServiceInput input;
    private Map<Integer, ServiceInput> serviceInputMap = new HashMap<>();
    private NotificationPublishService notificationPublishService;

    @Activate
    public Pce2ndStepPCNotificationHandler(
            @Reference RendererServiceOperations rendererServiceOperations,
            @Reference PathComputationService pathComputationService,
            @Reference NotificationPublishService notificationPublishService,
            @Reference ServiceDataStoreOperations serviceDataStoreOperations) {
//        this.rendererServiceOperations = rendererServiceOperations;
        this.serviceDataStoreOperations = serviceDataStoreOperations;
//        this.input = null;
        this.notificationPublishService = notificationPublishService;
    }

    public CompositeListener getCompositeListener() {
        return new CompositeListener(Set.of(
            new CompositeListener.Component<>(SecondStepHybridPcResult.class, this::secondStepHybridPcResult)));
        //    new CompositeListener.Component<>(ServicePathRpcResult.class, this::onServicePathRpcResult)));
    }

    private void secondStepHybridPcResult(SecondStepHybridPcResult notification) {
        if (comparesecondStepHybridPcResult(notification)) {
            LOG.warn("ServicePathRpcResult already wired !");
            return;
        }
        secondStepHybridPcResult = notification;
        switch (secondStepHybridPcResult.getNotificationType().getIntValue()) {
            /* second-step-hybrid-pc-request. */
            case 5:
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
     * Process 2nd Step Hybrid PC request result.
     * @param notification the result notification.
     */
    private void onPathComputationResult(SecondStepHybridPcResult notification) {
        LOG.info("PCE '{}' Notification received : {}", secondStepHybridPcResult.getNotificationType().getName(),
                notification);

        if (!checkStatus(notification)) {
            return;
        }
        AggregatedPathDescription aggPathDescription = secondStepHybridPcResult.getAggregatedPathDescription();
        if (aggPathDescription == null) {
            LOG.error("'AggregatedPathDescription' parameter is null ");
            return;
        }
        LOG.info("AggregatedPathDescription gets : {}", secondStepHybridPcResult.getAggregatedPathDescription());

        if (serviceInputMap == null || serviceInputMap.isEmpty()) {
            LOG.error("Input is null !");
            return;
        }
        Uint8 tapiServiceId = Uint8.ZERO;

        Map<Uint8, PathDescription> pathDescMap = new HashMap<>();
        for (Map.Entry<CdServicePathDescriptionKey, CdServicePathDescription> entry :
                aggPathDescription.getCdServicePathDescription().entrySet()) {
            Uint8 serviceId = entry.getValue().getCdServiceId();
            if (!entry.getValue().getCrossDomainService().getCdServices().values().stream()
                .filter(serv -> serv.getCdServiceId().equals(serviceId))
                .filter(serv -> serv.getTopologyDomain().equals(DomainTypeEnum.TapiSbi)).toList().isEmpty()) {
                tapiServiceId = serviceId;
            }
            PathDescription pathDescription =
                new PathDescriptionBuilder()
                    .setAToZDirection(entry.getValue().getAToZDirection())
                    .setZToADirection(entry.getValue().getZToADirection())
                    .setCrossDomain(true)
                    .setCrossDomainService(entry.getValue().getCrossDomainService())
                    .build();
            pathDescMap.put(serviceId, pathDescription);

            OperationResult operationResult = null;
            // Writing any sub services building E2E cross-domain service and their associated pathDescription
            // in DataStore (since process is different from regular service)
            for (Map.Entry<Integer, ServiceInput> serviceInputEntry : serviceInputMap.entrySet()) {
                if (serviceInputEntry.getKey().equals(serviceId.intValue())) {
                    operationResult = this.serviceDataStoreOperations.createService(
                            serviceInputEntry.getValue().getServiceCreateInput());
                    if (!operationResult.isSuccess()) {
                        LOG.error("Service {} not created in datastore !", serviceId);
                    }
                    if (!this.serviceDataStoreOperations
                            .createServicePath(
                                serviceInputEntry.getValue(),
                                //pceResponse
                                new PathComputationRequestOutputBuilder()
                                    .setResponseParameters(
                                        new ResponseParametersBuilder()
                                            .setPathDescription(
                                                new org.opendaylight.yang.gen.v1
                                                        .http.org.transportpce.b.c._interface.service.types.rev260910
                                                            .response.parameters.sp.response.parameters
                                                                .PathDescriptionBuilder(pathDescription)
                                                    .build())
                                            .build())
                                    .build()
                               )
                            .isSuccess()) {
                        LOG.error("Service Path for service {} not created in datastore!",
                            entry.getKey().getCdServiceId());
                    }
                }
            }
        }

        // Only launch TapiSbiServiceImplementationRequest for service spanning accross TAPI domain
        // Other sub service creation will be launched from the regular 1step path computation process.
        for (Map.Entry<Integer, ServiceInput> entry : serviceInputMap.entrySet()) {
            if (entry.getKey().equals(tapiServiceId.intValue())) {
                TapiSbiServiceImplementationRequestInput tsir = ModelMappingUtils
                    .createTapiServiceImplementationRequest(
                        entry.getValue(),
                        pathDescMap.entrySet().stream()
                            .filter(pathdesc -> pathdesc.getKey().intValue() == entry.getKey())
                            .findFirst().orElseThrow()
                            .getValue()
                            );
                LOG.info("Sending tapiSbiServiceImplementation request : {}", tsir);
                // TODO: Activate following line after we have solved the issue of having cyclic redundancy if creating
                // in the POM of the serviceHandler a dependency to tapi-sbi which depends on tapi (which has dependency
                //  to service handler)
                //  this.tapiSbirendererServiceOperations.serviceImplementation(tsir, false);
                PathComputationServiceImpl.setIs2ndStepFinished(true);
            }
        }

    }

    /**
     * Check status of notification and send nbi notification.
     * @param notification ServicePathRpcResult the notification to check.
     * @return true is status is Successful, false otherwise.
     */
    private boolean checkStatus(SecondStepHybridPcResult notification) {
        Map<String, PublishNotificationProcessService> nbiNotifMap = getPublishNotificationProcessService(notification);
        Boolean returnStatus = true;
        for (Map.Entry<String, PublishNotificationProcessService> nbiNotif : nbiNotifMap.entrySet()) {
            PublishNotificationProcessService nbiNotification = nbiNotif.getValue();
            PublishNotificationProcessServiceBuilder publishNotificationProcessServiceBuilder =
                    new PublishNotificationProcessServiceBuilder(nbiNotification);
            //TODO is it worth to instantiate the 2 variables above if status is 'Pending' or 'Successful' ?
            switch (secondStepHybridPcResult.getStatus()) {
                case Failed:
                    LOG.error("PCE path computation failed !");
                    nbiNotification = publishNotificationProcessServiceBuilder
                            .setMessage("ServiceCreate request failed ...")
                            .setResponseFailed("PCE path computation failed !")
                            .setOperationalState(State.Degraded).build();
                    sendNbiNotification(nbiNotification);
                    returnStatus = false;
                    break;
                case Pending:
                    LOG.warn("PCE path computation returned a Pending RpcStatusEx code for {} !", nbiNotif.getKey());
                    returnStatus = false;
                    break;
                case Successful:
                    LOG.info("PCE calculation done OK !");
                    break;
                default:
                    LOG.error("PCE path computation returned an unknown RpcStatusEx code {} for {} !",
                        secondStepHybridPcResult.getStatus(), nbiNotif.getKey());
                    nbiNotification = publishNotificationProcessServiceBuilder
                            .setMessage("ServiceCreate request failed ...")
                            .setResponseFailed("PCE path computation returned an unknown RpcStatusEx code!")
                            .setOperationalState(State.Degraded).build();
                    sendNbiNotification(nbiNotification);
                    returnStatus = false;
                    break;
            }
        }
        return returnStatus;
    }

    private Map<String, PublishNotificationProcessService> getPublishNotificationProcessService(
            SecondStepHybridPcResult notification) {
        Map<String, PublishNotificationProcessService> pnpsMap = new HashMap<>();
        if (serviceInputMap == null || serviceInputMap.isEmpty()) {
            pnpsMap.put(notification.getServiceName(), new PublishNotificationProcessServiceBuilder()
                .setServiceName(notification.getServiceName())
                .setPublisherName(PUBLISHER)
                .build());
            return pnpsMap;
        }
        for (Map.Entry<Integer, ServiceInput> entry : serviceInputMap.entrySet()) {
            pnpsMap.put(entry.getValue().getServiceName(), new PublishNotificationProcessServiceBuilder()
                .setServiceName(entry.getValue().getServiceName())
                .setServiceAEnd(new ServiceAEndBuilder(entry.getValue().getServiceAEnd()).build())
                .setServiceZEnd(new ServiceZEndBuilder(entry.getValue().getServiceZEnd()).build())
                .setCommonId(entry.getValue().getCommonId())
                .setConnectionType(entry.getValue().getConnectionType())
                .setPublisherName(PUBLISHER)
                .build());
        }
        return pnpsMap;
    }

    /**
     * Process cancel resource result.
     */
    private void onCancelResourceResult() {
        if (secondStepHybridPcResult.getStatus() == RpcStatusEx.Pending) {
            LOG.warn("PCE cancel returned a Pending RpcStatusEx code !");
            return;
        } else if (secondStepHybridPcResult.getStatus() != RpcStatusEx.Successful
                && secondStepHybridPcResult.getStatus() != RpcStatusEx.Failed) {
            LOG.error("PCE cancel returned an unknown RpcStatusEx code !");
            return;
        }
        PublishNotificationProcessServiceBuilder nbiNotificationBuilder;
        State serviceOpState;
        for (Map.Entry<Integer, ServiceInput> entry : serviceInputMap.entrySet()) {
            ServiceInput servInput = entry.getValue();
            Services service = serviceDataStoreOperations.getService(servInput.getServiceName()).orElseThrow();
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

            if (secondStepHybridPcResult.getStatus() == RpcStatusEx.Failed) {
                LOG.info("PCE cancel resource failed !");
                sendNbiNotification(
                    nbiNotificationBuilder
                        .setResponseFailed("PCE cancel resource failed !")
                        .setMessage("ServiceDelete request failed ...")
                        .setOperationalState(serviceOpState)
                        .build());
            }
            LOG.info("PCE cancel resource done OK for {} !", servInput.getServiceName());
            // Here the input refers to the transportPCE API and the serviceName will be commonId for temp-service
            OperationResult deleteServicePathOperationResult =
                    this.serviceDataStoreOperations.deleteServicePath(servInput.getServiceName());
            if (!deleteServicePathOperationResult.isSuccess()) {
                LOG.warn("Service path was not removed from datastore for {}!",servInput.getServiceName());
            }
            OperationResult deleteServiceOperationResult;
            String serviceType = "";
            deleteServiceOperationResult = this.serviceDataStoreOperations.deleteService(servInput.getServiceName());

            if (deleteServiceOperationResult.isSuccess()) {
                sendNbiNotification(
                    nbiNotificationBuilder
                        .setResponseFailed("")
                        .setMessage("Service deleted !")
                        .setOperationalState(State.Degraded)
                        .build());
            } else {
                LOG.warn("{} Service  {} was not removed from datastore !", serviceType, servInput.getServiceName());
                sendNbiNotification(
                    nbiNotificationBuilder
                        .setResponseFailed(serviceType + "Service was not removed from datastore !")
                        .setMessage("ServiceDelete request failed ...")
                        .setOperationalState(serviceOpState)
                        .build());
            }
        }
    }

    @SuppressFBWarnings(
        value = "ES_COMPARING_STRINGS_WITH_EQ",
        justification = "false positives, not strings but real object references comparisons")
    private Boolean comparesecondStepHybridPcResult(SecondStepHybridPcResult notification) {
        if (secondStepHybridPcResult == null) {
            return false;
        }
        if (secondStepHybridPcResult.getNotificationType() != notification.getNotificationType()) {
            return false;
        }
        if (secondStepHybridPcResult.getServiceName() != notification.getServiceName()) {
            return false;
        }
        if (secondStepHybridPcResult.getStatus() != notification.getStatus()) {
            return false;
        }
        return secondStepHybridPcResult.getStatusMessage() != notification.getStatusMessage();
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

    public void setInput(Map<Integer, ServiceInput> serviceInput) {
        this.serviceInputMap = serviceInput;
    }

}
