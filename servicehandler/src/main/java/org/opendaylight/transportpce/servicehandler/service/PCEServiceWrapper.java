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
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import org.opendaylight.mdsal.binding.api.NotificationPublishService;
import org.opendaylight.transportpce.common.ResponseCodes;
import org.opendaylight.transportpce.pce.service.PathComputationService;
import org.opendaylight.transportpce.servicehandler.ModelMappingUtils;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev240205.CancelResourceReserveInput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev240205.CancelResourceReserveInputBuilder;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev240205.CancelResourceReserveOutput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev240205.CancelResourceReserveOutputBuilder;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev240205.PathComputationRequestInput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev240205.PathComputationRequestInputBuilder;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev240205.PathComputationRequestOutput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev240205.PathComputationRequestOutputBuilder;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev240205.PathComputationRerouteRequestInput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev240205.PathComputationRerouteRequestInputBuilder;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev240205.PathComputationRerouteRequestOutput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev240205.PathComputationRerouteRequestOutputBuilder;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev240205.path.computation.reroute.request.input.Endpoints;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev240205.path.computation.reroute.request.input.EndpointsBuilder;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.service.spectrum.constraint.rev230907.ServiceAEnd2;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.service.spectrum.constraint.rev230907.ServiceAEnd3;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.service.spectrum.constraint.rev230907.ServiceAEnd5;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.service.spectrum.constraint.rev230907.ServiceZEnd2;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.service.spectrum.constraint.rev230907.ServiceZEnd3;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.service.spectrum.constraint.rev230907.ServiceZEnd5;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.service.spectrum.constraint.rev230907.SpectrumAllocation;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.servicehandler.rev201125.ServiceRpcResultSh;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.servicehandler.rev201125.ServiceRpcResultShBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev230526.ServiceEndpoint;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev230526.ServiceNotificationTypes;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev230526.configuration.response.common.ConfigurationResponseCommon;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev230526.configuration.response.common.ConfigurationResponseCommonBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev230526.sdnc.request.header.SdncRequestHeader;
import org.opendaylight.yang.gen.v1.http.org.openroadm.routing.constraints.rev221209.routing.constraints.HardConstraints;
import org.opendaylight.yang.gen.v1.http.org.openroadm.routing.constraints.rev221209.routing.constraints.SoftConstraints;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev230526.ServiceCreateInput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev230526.ServiceFeasibilityCheckInput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev230526.TempServiceCreateInput;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.service.types.rev220118.PceMetric;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.service.types.rev220118.RpcStatusEx;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.service.types.rev220118.response.parameters.sp.ResponseParameters;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.service.types.rev220118.response.parameters.sp.ResponseParametersBuilder;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.service.types.rev220118.service.handler.header.ServiceHandlerHeaderBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PCEServiceWrapper {

    private static final String NOTIFICATION_OFFER_REJECTED_MSG = "notification offer rejected : ";

    private static final String PERFORMING_PCE_MSG = "performing PCE ...";

    private static final Logger LOG = LoggerFactory.getLogger(PCEServiceWrapper.class);

    private final PathComputationService pathComputationService;
    private final NotificationPublishService notificationPublishService;
    private ServiceRpcResultSh notification = null;
    private final ListeningExecutorService executor;

    public PCEServiceWrapper(PathComputationService pathComputationService,
            NotificationPublishService notificationPublishService) {
        this.pathComputationService = pathComputationService;
        this.notificationPublishService = notificationPublishService;
        executor = MoreExecutors.listeningDecorator(Executors.newFixedThreadPool(5));
    }

    public PathComputationRequestOutput performPCE(ServiceCreateInput serviceCreateInput, boolean reserveResource) {
        LOG.info(PERFORMING_PCE_MSG);
        if (validateParams(serviceCreateInput.getServiceName(), serviceCreateInput.getSdncRequestHeader())) {

            return performPCE(
                    serviceCreateInput.getHardConstraints(),
                    serviceCreateInput.getSoftConstraints(),
                    serviceCreateInput.getServiceName(),
                    serviceCreateInput.getSdncRequestHeader(),
                    serviceCreateInput.getServiceAEnd(),
                    serviceCreateInput.getServiceZEnd(),
                    ServiceNotificationTypes.ServiceCreateResult,
                    reserveResource,
                    serviceCreateInput.getCustomer(),
                    serviceCreateInput
                            .getServiceAEnd()
                            .augmentation(ServiceAEnd2.class),
                    serviceCreateInput
                            .getServiceZEnd()
                            .augmentation(ServiceZEnd2.class)
            );
        } else {
            return returnPCEFailed();
        }
    }

    public PathComputationRequestOutput performPCE(TempServiceCreateInput tempServiceCreateInput,
            boolean reserveResource) {

        LOG.info(PERFORMING_PCE_MSG);
        if (validateParams(tempServiceCreateInput.getCommonId(), tempServiceCreateInput.getSdncRequestHeader())) {
            return performPCE(
                    tempServiceCreateInput.getHardConstraints(),
                    tempServiceCreateInput.getSoftConstraints(),
                    tempServiceCreateInput.getCommonId(),
                    tempServiceCreateInput.getSdncRequestHeader(),
                    tempServiceCreateInput.getServiceAEnd(),
                    tempServiceCreateInput.getServiceZEnd(),
                    ServiceNotificationTypes.ServiceCreateResult,
                    reserveResource,
                    tempServiceCreateInput.getCustomer(),
                    tempServiceCreateInput.getServiceAEnd().augmentation(ServiceAEnd5.class),
                    tempServiceCreateInput.getServiceZEnd().augmentation(ServiceZEnd5.class)
            );
        } else {
            return returnPCEFailed();
        }
    }

    public PathComputationRequestOutput performPCE(ServiceFeasibilityCheckInput serviceFeasibilityCheckInput,
            boolean reserveResource) {
        LOG.info(PERFORMING_PCE_MSG);


        if (validateParams(serviceFeasibilityCheckInput.getCommonId(),
                serviceFeasibilityCheckInput.getSdncRequestHeader())) {
            return performPCE(serviceFeasibilityCheckInput.getHardConstraints(),
                    serviceFeasibilityCheckInput.getSoftConstraints(), serviceFeasibilityCheckInput.getCommonId(),
                    serviceFeasibilityCheckInput.getSdncRequestHeader(), serviceFeasibilityCheckInput.getServiceAEnd(),
                    serviceFeasibilityCheckInput.getServiceZEnd(),
                    ServiceNotificationTypes.ServiceCreateResult, reserveResource,
                    serviceFeasibilityCheckInput.getCustomer(),
                    serviceFeasibilityCheckInput.getServiceAEnd().augmentation(ServiceAEnd3.class),
                    serviceFeasibilityCheckInput.getServiceZEnd().augmentation(ServiceZEnd3.class)
            );
        } else {
            return returnPCEFailed();
        }
    }

    private PathComputationRequestOutput performPCE(
            HardConstraints hardConstraints,
            SoftConstraints softConstraints,
            String serviceName,
            SdncRequestHeader sdncRequestHeader,
            ServiceEndpoint serviceAEnd,
            ServiceEndpoint serviceZEnd,
            ServiceNotificationTypes notifType,
            boolean reserveResource,
            String customerName,
            SpectrumAllocation spectrumAEndAllocation,
            SpectrumAllocation spectrumZEndAllocation
    ) {

        LOG.info("Calling path computation.");
        notification = new ServiceRpcResultShBuilder().setNotificationType(notifType).setServiceName(serviceName)
                .setStatus(RpcStatusEx.Pending)
                .setStatusMessage("Service compliant, submitting PathComputation Request ...").build();
        try {
            notificationPublishService.putNotification(notification);
        } catch (InterruptedException e) {
            LOG.info(NOTIFICATION_OFFER_REJECTED_MSG, e);
        }
        FutureCallback<PathComputationRequestOutput> pceCallback =
                new PathComputationRequestOutputCallback(notifType, serviceName);
        PathComputationRequestInput pathComputationRequestInput = createPceRequestInput(
                serviceName,
                sdncRequestHeader,
                hardConstraints,
                softConstraints,
                reserveResource,
                serviceAEnd,
                serviceZEnd,
                customerName,
                spectrumAEndAllocation,
                spectrumZEndAllocation
        );
        ListenableFuture<PathComputationRequestOutput> pce = this.pathComputationService
                .pathComputationRequest(pathComputationRequestInput);
        Futures.addCallback(pce, pceCallback, executor);

        ConfigurationResponseCommon configurationResponseCommon = new ConfigurationResponseCommonBuilder()
                .setAckFinalIndicator(ResponseCodes.FINAL_ACK_NO)
                .setRequestId(sdncRequestHeader.getRequestId())
                .setResponseCode(ResponseCodes.RESPONSE_OK)
                .setResponseMessage("PCE calculation in progress")
                .build();
        ResponseParameters reponseParameters = new ResponseParametersBuilder().build();
        return new PathComputationRequestOutputBuilder()
                .setConfigurationResponseCommon(configurationResponseCommon)
                .setResponseParameters(reponseParameters)
                .build();
    }

    public PathComputationRerouteRequestOutput performPCEReroute(HardConstraints hardConstraints,
           SoftConstraints softConstraints, SdncRequestHeader serviceHandler, ServiceEndpoint serviceAEnd,
           ServiceEndpoint serviceZEnd,
           Endpoints endpoints) {
        // TODO: Make it asynchronous
        LOG.info("Calling path computation reroute");
        PathComputationRerouteRequestInput inputPCE = createPceRerouteRequestInput(hardConstraints, softConstraints,
                serviceHandler, serviceAEnd, serviceZEnd, endpoints);
        ListenableFuture<PathComputationRerouteRequestOutput> res =
                pathComputationService.pathComputationRerouteRequest(inputPCE);
        try {
            return res.get();
        } catch (ExecutionException | InterruptedException e) {
            LOG.warn("PerformPCEReroute FAILED ! ", e);
            return new PathComputationRerouteRequestOutputBuilder()
                    .setConfigurationResponseCommon(new ConfigurationResponseCommonBuilder()
                            .setAckFinalIndicator(ResponseCodes.FINAL_ACK_YES)
                            .setRequestId("None")
                            .setResponseCode(ResponseCodes.RESPONSE_OK)
                            .setResponseMessage("PCE calculation FAILED")
                            .build())
                    .build();
        }
    }

    private PathComputationRequestInput createPceRequestInput(
            String serviceName,
            SdncRequestHeader serviceHandler,
            HardConstraints hardConstraints,
            SoftConstraints softConstraints,
            Boolean reserveResource,
            ServiceEndpoint serviceAEnd,
            ServiceEndpoint serviceZEnd,
            String customerName,
            SpectrumAllocation spectrumAEndAllocation,
            SpectrumAllocation spectrumZEndAllocation) {

        LOG.info("Mapping ServiceCreateInput or ServiceFeasibilityCheckInput or serviceReconfigureInput to PCE"
                + "requests");
        ServiceHandlerHeaderBuilder serviceHandlerHeader = new ServiceHandlerHeaderBuilder();
        if (serviceHandler != null) {
            serviceHandlerHeader.setRequestId(serviceHandler.getRequestId());
        }
        return new PathComputationRequestInputBuilder()
            .setServiceName(serviceName)
            .setResourceReserve(reserveResource)
            .setServiceHandlerHeader(serviceHandlerHeader.build())
            .setHardConstraints(hardConstraints)
            .setSoftConstraints(softConstraints)
            .setPceRoutingMetric(PceMetric.TEMetric)
            .setCustomerName(customerName)
            .setServiceAEnd(ModelMappingUtils.createServiceAEnd(serviceAEnd, spectrumAEndAllocation))
            .setServiceZEnd(ModelMappingUtils.createServiceZEnd(serviceZEnd, spectrumZEndAllocation))
            .build();
    }

    private PathComputationRerouteRequestInput createPceRerouteRequestInput(HardConstraints hardConstraints,
            SoftConstraints softConstraints, SdncRequestHeader serviceHandler, ServiceEndpoint serviceAEnd,
            ServiceEndpoint serviceZEnd, Endpoints endpoints) {
        LOG.info("Mapping Service-reroute to PCE requests");
        return new PathComputationRerouteRequestInputBuilder()
                .setServiceHandlerHeader(serviceHandler == null
                        ? new ServiceHandlerHeaderBuilder().build()
                        : new ServiceHandlerHeaderBuilder().setRequestId(serviceHandler.getRequestId()).build())
                .setHardConstraints(hardConstraints)
                .setSoftConstraints(softConstraints)
                .setPceRoutingMetric(PceMetric.TEMetric)
                .setEndpoints(new EndpointsBuilder()
                        .setAEndTp(endpoints.getAEndTp())
                        .setZEndTp(endpoints.getZEndTp())
                        .build())
                .setServiceAEnd(ModelMappingUtils.createServiceAEndReroute(serviceAEnd))
                .setServiceZEnd(ModelMappingUtils.createServiceZEndReroute(serviceZEnd))
                .build();
    }

    private CancelResourceReserveInput mappingCancelResourceReserve(String serviceName,
                                                                    SdncRequestHeader sdncRequestHeader) {
        LOG.info("Mapping to PCE Cancel resource request input");
        CancelResourceReserveInputBuilder cancelResourceReserveInput = new CancelResourceReserveInputBuilder();
        if (serviceName != null) {
            ServiceHandlerHeaderBuilder serviceHandlerHeader = new ServiceHandlerHeaderBuilder();
            if (sdncRequestHeader != null) {
                serviceHandlerHeader.setRequestId(sdncRequestHeader.getRequestId());
            }
            cancelResourceReserveInput.setServiceName(serviceName)
                    .setServiceHandlerHeader(serviceHandlerHeader.build());
            return cancelResourceReserveInput.build();
        } else {
            LOG.error("Service Name (common-id for Temp service) is not set");
            return null;
        }
    }

    public CancelResourceReserveOutput cancelPCEResource(String serviceName, ServiceNotificationTypes notifType) {
        LOG.info("Calling cancel resource reserve computation.");
        notification = new ServiceRpcResultShBuilder().setNotificationType(notifType).setServiceName(serviceName)
                .setStatus(RpcStatusEx.Pending)
                .setStatusMessage("submitting Cancel resource reserve Request ...").build();
        try {
            notificationPublishService.putNotification(notification);
        } catch (InterruptedException e) {
            LOG.info(NOTIFICATION_OFFER_REJECTED_MSG, e);
        }
        FutureCallback<CancelResourceReserveOutput> pceCallback =
                new CancelResourceReserveOutputFutureCallback(notifType, serviceName);
        CancelResourceReserveInput cancelResourceReserveInput = mappingCancelResourceReserve(serviceName, null);
        ConfigurationResponseCommonBuilder configurationResponseCommon = new ConfigurationResponseCommonBuilder();
        if (cancelResourceReserveInput != null) {
            String requestId = cancelResourceReserveInput.getServiceHandlerHeader().getRequestId();
            ListenableFuture<CancelResourceReserveOutput> pce =
                    this.pathComputationService.cancelResourceReserve(cancelResourceReserveInput);
            Futures.addCallback(pce, pceCallback, executor);
            if (requestId != null) {
                configurationResponseCommon.setRequestId(requestId);
            }
            configurationResponseCommon.setAckFinalIndicator(ResponseCodes.FINAL_ACK_NO)
                    .setResponseCode(ResponseCodes.RESPONSE_OK).setResponseMessage("PCE calculation in progress");
            return new CancelResourceReserveOutputBuilder()
                    .setConfigurationResponseCommon(configurationResponseCommon.build()).build();
        } else {
            configurationResponseCommon.setAckFinalIndicator(ResponseCodes.FINAL_ACK_YES)
                    .setResponseCode(ResponseCodes.RESPONSE_FAILED).setResponseMessage("PCE failed !");
            return new CancelResourceReserveOutputBuilder()
                    .setConfigurationResponseCommon(configurationResponseCommon.build()).build();
        }
    }

    private static PathComputationRequestOutput returnPCEFailed() {
        ConfigurationResponseCommon configurationResponseCommon = new ConfigurationResponseCommonBuilder()
                .setAckFinalIndicator(ResponseCodes.FINAL_ACK_YES).setResponseCode(ResponseCodes.RESPONSE_FAILED)
                .setResponseMessage("PCE calculation failed").build();
        ResponseParameters reponseParameters = new ResponseParametersBuilder().build();
        return new PathComputationRequestOutputBuilder().setConfigurationResponseCommon(configurationResponseCommon)
                .setResponseParameters(reponseParameters).build();
    }

    private Boolean validateParams(String serviceName, SdncRequestHeader sdncRequestHeader) {
        boolean result = true;
        if (!checkString(serviceName)) {
            result = false;
            LOG.error("Service Name (common-id for Temp service) is not set");
        } else if (sdncRequestHeader == null) {
            LOG.error("Service sdncRequestHeader 'request-id' is not set");
            result = false;
        }
        return result;
    }

    private static boolean checkString(String value) {
        return ((value != null) && (value.compareTo("") != 0));
    }

    private final class CancelResourceReserveOutputFutureCallback
            implements FutureCallback<CancelResourceReserveOutput> {
        private final ServiceNotificationTypes notifType;
        private final String serviceName;
        String message = "";
        ServiceRpcResultSh notification = null;

        private CancelResourceReserveOutputFutureCallback(ServiceNotificationTypes notifType, String serviceName) {
            this.notifType = notifType;
            this.serviceName = serviceName;
        }

        @Override
        public void onSuccess(CancelResourceReserveOutput response) {
            if (response != null) {
                /**
                 * If PCE reply is received before timer expiration with a positive result, a
                 * service is created with admin and operational status 'down'.
                 */
                message = "PCE replied to CRR Request !";
                LOG.info("PCE replied to CRR Request : {}", response);
                notification =
                        new ServiceRpcResultShBuilder().setNotificationType(notifType).setServiceName(serviceName)
                                .setStatus(RpcStatusEx.Successful).setStatusMessage(message).build();
                try {
                    notificationPublishService.putNotification(notification);
                } catch (InterruptedException e) {
                    LOG.info(NOTIFICATION_OFFER_REJECTED_MSG, e);
                }
            } else {
                message = "PCE failed ";
                notification = new ServiceRpcResultShBuilder().setNotificationType(notifType).setServiceName("")
                        .setStatus(RpcStatusEx.Failed).setStatusMessage(message).build();
                try {
                    notificationPublishService.putNotification(notification);
                } catch (InterruptedException e) {
                    LOG.info(NOTIFICATION_OFFER_REJECTED_MSG, e);
                }
            }
        }

        @Override
        public void onFailure(Throwable arg0) {
            LOG.error("Cancel resource failed !");
            notification = new ServiceRpcResultShBuilder().setNotificationType(notifType)
                    .setServiceName(serviceName).setStatus(RpcStatusEx.Failed)
                    .setStatusMessage("CRR Request failed  : " + arg0.getMessage()).build();
            try {
                notificationPublishService.putNotification(notification);
            } catch (InterruptedException e) {
                LOG.info(NOTIFICATION_OFFER_REJECTED_MSG, e);
            }
        }
    }

    private final class PathComputationRequestOutputCallback implements FutureCallback<PathComputationRequestOutput> {
        private final ServiceNotificationTypes notifType;
        private final String serviceName;
        String message = "";
        ServiceRpcResultSh notification = null;

        private PathComputationRequestOutputCallback(ServiceNotificationTypes notifType, String serviceName) {
            this.notifType = notifType;
            this.serviceName = serviceName;
        }

        @Override
        public void onSuccess(PathComputationRequestOutput response) {
            if (response != null) {
                /**
                 * If PCE reply is received before timer expiration with a positive result, a
                 * service is created with admin and operational status 'down'.
                 */
                message = "PCE replied to PCR Request !";
                LOG.info("PCE replied to PCR Request : {}", response);
                notification = new ServiceRpcResultShBuilder().setNotificationType(notifType)
                        .setServiceName(serviceName)
                        .setStatus(RpcStatusEx.Successful).setStatusMessage(message).build();
                try {
                    notificationPublishService.putNotification(notification);
                } catch (InterruptedException e) {
                    LOG.info(NOTIFICATION_OFFER_REJECTED_MSG, e);
                }
            } else {
                message = "PCE failed ";
                notification = new ServiceRpcResultShBuilder().setNotificationType(notifType).setServiceName("")
                        .setStatus(RpcStatusEx.Failed).setStatusMessage(message).build();
                try {
                    notificationPublishService.putNotification(notification);
                } catch (InterruptedException e) {
                    LOG.info(NOTIFICATION_OFFER_REJECTED_MSG, e);
                }
            }
        }

        @Override
        public void onFailure(Throwable arg0) {
            LOG.error("Path not calculated..");
            notification = new ServiceRpcResultShBuilder().setNotificationType(notifType)
                    .setServiceName(serviceName)
                    .setStatus(RpcStatusEx.Failed).setStatusMessage("PCR Request failed  : " + arg0.getMessage())
                    .build();
            try {
                notificationPublishService.putNotification(notification);
            } catch (InterruptedException e) {
                LOG.info(NOTIFICATION_OFFER_REJECTED_MSG, e);
            }
        }
    }
}
