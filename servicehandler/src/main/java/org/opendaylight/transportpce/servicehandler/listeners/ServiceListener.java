/*
 * Copyright © 2021 Orange and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.servicehandler.listeners;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import org.opendaylight.mdsal.binding.api.DataObjectDeleted;
import org.opendaylight.mdsal.binding.api.DataObjectModification;
import org.opendaylight.mdsal.binding.api.DataTreeChangeListener;
import org.opendaylight.mdsal.binding.api.DataTreeModification;
import org.opendaylight.mdsal.binding.api.NotificationPublishService;
import org.opendaylight.mdsal.binding.api.RpcService;
import org.opendaylight.transportpce.common.ResponseCodes;
import org.opendaylight.transportpce.servicehandler.ServiceInput;
import org.opendaylight.transportpce.servicehandler.service.ServiceDataStoreOperations;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev250110.Restorable;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev250110.RpcActions;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev250110.routing.metric.RoutingMetric;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev250110.sdnc.request.header.SdncRequestHeaderBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev250110.service.resiliency.ServiceResiliency;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.state.types.rev191129.State;
import org.opendaylight.yang.gen.v1.http.org.openroadm.equipment.states.types.rev191129.AdminStates;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev250110.ServiceCreate;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev250110.ServiceCreateInputBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev250110.ServiceCreateOutput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev250110.ServiceDelete;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev250110.ServiceDeleteInputBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev250110.ServiceDeleteOutput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev250110.ServiceReroute;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev250110.ServiceRerouteInput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev250110.ServiceRerouteInputBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev250110.ServiceRerouteOutput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev250110.service.create.input.ServiceAEndBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev250110.service.create.input.ServiceZEndBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev250110.service.delete.input.ServiceDeleteReqInfo;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev250110.service.delete.input.ServiceDeleteReqInfoBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev250110.service.list.Services;
import org.opendaylight.yang.gen.v1.nbi.notifications.rev230728.PublishNotificationAlarmService;
import org.opendaylight.yang.gen.v1.nbi.notifications.rev230728.PublishNotificationAlarmServiceBuilder;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
public class ServiceListener implements DataTreeChangeListener<Services> {

    private static final Logger LOG = LoggerFactory.getLogger(ServiceListener.class);
    private static final String PUBLISHER = "ServiceListener";
    private final RpcService rpcService;
    private ServiceDataStoreOperations serviceDataStoreOperations;
    private NotificationPublishService notificationPublishService;
    private Map<String, ServiceInput> mapServiceInputReroute;
    private final ScheduledExecutorService executor;

    @Activate
    public ServiceListener(@Reference RpcService rpcService,
            @Reference ServiceDataStoreOperations serviceDataStoreOperations,
            @Reference NotificationPublishService notificationPublishService) {
        this.rpcService = rpcService;
        this.serviceDataStoreOperations = serviceDataStoreOperations;
        this.notificationPublishService = notificationPublishService;
        this.executor = MoreExecutors.getExitingScheduledExecutorService(new ScheduledThreadPoolExecutor(4));
        mapServiceInputReroute = new HashMap<>();
    }

    @Override
    public void onDataTreeChanged(List<DataTreeModification<Services>> changes) {
        LOG.info("onDataTreeChanged - {}", this.getClass().getSimpleName());
        for (DataTreeModification<Services> change : changes) {
            DataObjectModification<Services> rootService = change.getRootNode();
            switch (rootService) {
                case DataObjectDeleted<Services> deleted -> {
                    String serviceName = deleted.dataBefore().getServiceName();
                    LOG.info("Service {} correctly deleted from controller", serviceName);
                    if (mapServiceInputReroute.get(serviceName) != null) {
                        serviceRerouteStep2(serviceName);
                    }
                }
                case DataObjectModification.WithDataAfter<Services> present -> {
                    Services serviceBefore = present.dataBefore();
                    Services serviceAfter = present.dataAfter();
                    String serviceName = serviceAfter.getServiceName();
                    if (serviceBefore.getOperationalState() == State.InService
                            && serviceAfter.getOperationalState() == State.OutOfService) {
                        LOG.info("Service {} is becoming outOfService", serviceName);
                        sendNbiNotification(new PublishNotificationAlarmServiceBuilder()
                                .setServiceName(serviceAfter.getServiceName())
                                .setConnectionType(serviceAfter.getConnectionType())
                                .setMessage("The service is now outOfService")
                                .setOperationalState(State.OutOfService)
                                .setPublisherName(PUBLISHER)
                                .build());
                        if (serviceAfter.getAdministrativeState() == AdminStates.InService
                                && serviceAfter.getServiceResiliency() != null
                                && serviceAfter.getServiceResiliency().getResiliency() != null
                                && serviceAfter.getServiceResiliency().getResiliency().equals(Restorable.VALUE)) {
                            LOG.info("Attempting to reroute the service '{}'...", serviceName);
                            if (!serviceRerouteCheck(serviceName, serviceAfter.getServiceResiliency(),
                                    serviceAfter.getRoutingMetric())) {
                                LOG.info("No other path available, cancelling reroute process of service '{}'...",
                                        serviceName);
                                continue;
                            }
                            mapServiceInputReroute.put(serviceName, null);
                            if (serviceAfter.getServiceResiliency().getHoldoffTime() != null) {
                                LOG.info("Waiting hold off time before rerouting...");
                                executor.schedule(() -> {
                                    if (mapServiceInputReroute.containsKey(serviceName)
                                            && mapServiceInputReroute.get(serviceName) == null) {
                                        serviceRerouteStep1(serviceName);
                                    } else {
                                        LOG.info("Cancelling reroute process of service '{}'...", serviceName);
                                    }
                                },
                                    Long.parseLong(
                                            String.valueOf(serviceAfter.getServiceResiliency().getHoldoffTime())),
                                            TimeUnit.MILLISECONDS);
                            } else {
                                serviceRerouteStep1(serviceName);
                            }
                        }
                    } else if (serviceAfter.getAdministrativeState() == AdminStates.InService
                            && serviceBefore.getOperationalState() == State.OutOfService
                            && serviceAfter.getOperationalState() == State.InService) {
                        LOG.info("Service {} is becoming InService", serviceName);
                        sendNbiNotification(new PublishNotificationAlarmServiceBuilder()
                                .setServiceName(serviceAfter.getServiceName())
                                .setConnectionType(serviceAfter.getConnectionType())
                                .setMessage("The service is now inService")
                                .setOperationalState(State.InService)
                                .setPublisherName(PUBLISHER)
                                .build());
                        if (mapServiceInputReroute.containsKey(serviceName)
                                && mapServiceInputReroute.get(serviceName) == null) {
                            mapServiceInputReroute.remove(serviceName);
                        }
                    }
                }
            }
        }
    }

    /**
     * First step of the reroute : apply a service-delete RPC to the service.
     *
     * @param serviceNameToReroute Name of the service
     */
    private void serviceRerouteStep1(String serviceNameToReroute) {
        mapServiceInputReroute.remove(serviceNameToReroute);
        Optional<Services> serviceOpt = serviceDataStoreOperations.getService(serviceNameToReroute);
        if (serviceOpt.isEmpty()) {
            LOG.warn("Service '{}' does not exist in datastore", serviceNameToReroute);
            return;
        }
        Services service = serviceOpt.orElseThrow();
        ListenableFuture<RpcResult<ServiceDeleteOutput>> res = rpcService.getRpc(ServiceDelete.class).invoke(
                new ServiceDeleteInputBuilder()
                        .setSdncRequestHeader(new SdncRequestHeaderBuilder(service.getSdncRequestHeader())
                                .setRpcAction(RpcActions.ServiceDelete)
                                .build())
                        .setServiceDeleteReqInfo(new ServiceDeleteReqInfoBuilder()
                                .setServiceName(serviceNameToReroute)
                                .setTailRetention(ServiceDeleteReqInfo.TailRetention.No)
                                .build())
                        .build());
        try {
            String httpResponseCode = res.get().getResult().getConfigurationResponseCommon().getResponseCode();
            if (httpResponseCode.equals(ResponseCodes.RESPONSE_OK)) {
                mapServiceInputReroute.put(serviceNameToReroute, new ServiceInput(
                        new ServiceCreateInputBuilder()
                                .setServiceName(serviceNameToReroute)
                                .setCommonId(service.getCommonId())
                                .setConnectionType(service.getConnectionType())
                                .setServiceAEnd(new ServiceAEndBuilder(service.getServiceAEnd()).build())
                                .setServiceZEnd(new ServiceZEndBuilder(service.getServiceZEnd()).build())
                                .setHardConstraints(service.getHardConstraints())
                                .setSoftConstraints(service.getSoftConstraints())
                                .setSdncRequestHeader(service.getSdncRequestHeader())
                                .setCustomer(service.getCustomer())
                                .setCustomerContact(service.getCustomerContact())
                                .setServiceResiliency(service.getServiceResiliency())
                                .setDueDate(service.getDueDate())
                                .setOperatorContact(service.getOperatorContact())
                                .build()));
                LOG.info("ServiceRerouteStep1 (deletion of the service) in progress");
            } else {
                LOG.warn("ServiceRerouteStep1 (deletion of the service) failed '{}' http code ", httpResponseCode);
            }
        } catch (ExecutionException | InterruptedException e) {
            LOG.warn("ServiceRerouteStep1 FAILED ! ", e);
        }
    }

    /**
     * Second step of the reroute : apply a service-create RPC. This method is called after the first step of reroute
     * when the service has been successfully deleted.
     *
     * @param serviceNameToReroute Name of the service
     */
    private void serviceRerouteStep2(String serviceNameToReroute) {
        ListenableFuture<RpcResult<ServiceCreateOutput>> res = rpcService.getRpc(ServiceCreate.class).invoke(
                mapServiceInputReroute.get(serviceNameToReroute).getServiceCreateInput());
        try {
            String httpResponseCode = res.get().getResult().getConfigurationResponseCommon().getResponseCode();
            if (httpResponseCode.equals(ResponseCodes.RESPONSE_OK)) {
                LOG.info("ServiceRerouteStep2 (creation of the new service) in progress");
            } else {
                LOG.warn("ServiceRerouteStep2 (creation of the new service) failed '{}' http code ", httpResponseCode);
            }
        } catch (ExecutionException | InterruptedException e) {
            LOG.warn("ServiceRerouteStep2 FAILED ! ", e);
        }
        mapServiceInputReroute.remove(serviceNameToReroute);
    }

    /**
     * Prior to the reroute steps: check that an alternative route of the service is possible.
     *
     * @param serviceNameToReroute Name of the service
     * @param serviceResiliency Resiliency of the service
     * @param routingMetric Metric of the routing
     */
    private boolean serviceRerouteCheck(String serviceNameToReroute, ServiceResiliency serviceResiliency,
                                        RoutingMetric routingMetric) {
        ServiceRerouteInput serviceRerouteInput = new ServiceRerouteInputBuilder()
                .setServiceName(serviceNameToReroute)
                .setServiceResiliency(serviceResiliency)
                .setRoutingMetric(routingMetric)
                .setSdncRequestHeader(new SdncRequestHeaderBuilder()
                        .setRpcAction(RpcActions.ServiceReroute)
                        .build())
                .build();
        ListenableFuture<RpcResult<ServiceRerouteOutput>> res = rpcService.getRpc(ServiceReroute.class).invoke(
                serviceRerouteInput);
        try {
            return res.get().getResult().getConfigurationResponseCommon().getResponseCode()
                    .equals(ResponseCodes.RESPONSE_OK);
        } catch (ExecutionException | InterruptedException e) {
            LOG.warn("ServiceRerouteCheck FAILED ! ", e);
            return false;
        }
    }

    /**
     * Send notification to NBI notification in order to publish message.
     *
     * @param service PublishNotificationAlarmService
     */
    private void sendNbiNotification(PublishNotificationAlarmService service) {
        try {
            notificationPublishService.putNotification(service);
        } catch (InterruptedException e) {
            LOG.warn("Cannot send notification to nbi", e);
            Thread.currentThread().interrupt();
        }
    }

}
