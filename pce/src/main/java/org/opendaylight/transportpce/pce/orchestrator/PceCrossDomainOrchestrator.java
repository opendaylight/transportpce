/*
 * Copyright © 2026 Orange, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.pce.orchestrator;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.regex.Pattern;
import org.opendaylight.mdsal.binding.api.NotificationPublishService;
import org.opendaylight.transportpce.common.ResponseCodes;
import org.opendaylight.transportpce.pce.service.PathComputationService;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev260910.PathComputationRequestInput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev260910.PathComputationRequestInputBuilder;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev260910.PathComputationRequestOutput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev260910.PathComputationRequestOutputBuilder;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev260910.SecondStepHybridPcResult;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev260910.SecondStepHybridPcResultBuilder;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev260910.path.computation.request.input.ServiceAEnd;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev260910.path.computation.request.input.ServiceAEndBuilder;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev260910.path.computation.request.input.ServiceZEnd;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev260910.path.computation.request.input.ServiceZEndBuilder;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.service.spectrum.constraint.rev230907.ServiceAEnd2;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.service.spectrum.constraint.rev230907.ServiceZEnd2;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.service.spectrum.constraint.rev230907.SpectrumAllocation;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.servicehandler.rev201125.ServiceRpcResultSh;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.servicehandler.rev201125.ServiceRpcResultShBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev250530.ServiceEndpoint;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev250530.ServiceNotificationTypes;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev250530.configuration.response.common.ConfigurationResponseCommon;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev250530.configuration.response.common.ConfigurationResponseCommonBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev250530.sdnc.request.header.SdncRequestHeader;
import org.opendaylight.yang.gen.v1.http.org.openroadm.routing.constraints.rev240329.routing.constraints.HardConstraints;
import org.opendaylight.yang.gen.v1.http.org.openroadm.routing.constraints.rev240329.routing.constraints.SoftConstraints;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev250530.ServiceCreateInput;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.service.types.rev260910.PceMetric;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.service.types.rev260910.RpcStatusEx;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.service.types.rev260910.ServicePathNotificationTypes;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.service.types.rev260910.response.parameters.sp.ResponseParameters;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.service.types.rev260910.response.parameters.sp.ResponseParametersBuilder;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.service.types.rev260910.service.endpoint.sp.RxDirection;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.service.types.rev260910.service.endpoint.sp.RxDirectionBuilder;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.service.types.rev260910.service.endpoint.sp.TxDirection;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.service.types.rev260910.service.endpoint.sp.TxDirectionBuilder;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.service.types.rev260910.service.handler.header.ServiceHandlerHeaderBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.Uuid;
import org.opendaylight.yangtools.yang.common.Uint8;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public final class PceCrossDomainOrchestrator {

    private static PceCrossDomainOrchestrator instance;
    /* Logging. */
    private static final Logger LOG = LoggerFactory.getLogger(PceCrossDomainOrchestrator.class);
    private static final String NOTIFICATION_OFFER_REJECTED_MSG = "notification offer rejected : ";
    private static final String PERFORMING_PCE_CROSS_DOMAIN_MSG = "performing TAPI PCE (Cross-Domain-Service...";
    private PathComputationService pathComputationService;
    private NotificationPublishService notificationPublishService;
    private static ServiceRpcResultSh notification;
    private static int kpathOrder;
    private static final ListeningExecutorService EXECUTOR = MoreExecutors
        .listeningDecorator(Executors.newFixedThreadPool(5));


    private PceCrossDomainOrchestrator(PathComputationService pcs, NotificationPublishService nps) {

        this.notificationPublishService = nps;
        this.pathComputationService = pcs;

    }

    public static synchronized PceCrossDomainOrchestrator getInstance(final PathComputationService pcs,
        final NotificationPublishService nps) {
        if (instance == null) {
            instance = new PceCrossDomainOrchestrator(pcs, nps);
        }
        return instance;
    }

    public static PathComputationRequestOutput performPCE(
            ServiceCreateInput serviceCreateInput, boolean reserveResource,
            PathComputationService pcs, NotificationPublishService nps) {
        LOG.info(PceCrossDomainOrchestrator.PERFORMING_PCE_CROSS_DOMAIN_MSG + "for K path order = {}",
            PceCrossDomainOrchestrator.kpathOrder);
        if (validateParams(serviceCreateInput.getServiceName(), serviceCreateInput.getSdncRequestHeader())) {

            return performPCE(
                    serviceCreateInput.getHardConstraints(),
                    serviceCreateInput.getSoftConstraints(),
                    serviceCreateInput.getServiceName(),
                    serviceCreateInput.getSdncRequestHeader(),
                    serviceCreateInput.getServiceAEnd(),
                    serviceCreateInput.getServiceZEnd(),
                    ServiceNotificationTypes.ServiceCreateResult,
                    ServicePathNotificationTypes.SecondStepHybridPcRequest,
                    reserveResource,
                    serviceCreateInput.getCustomer(),
                    serviceCreateInput
                            .getServiceAEnd()
                            .augmentation(ServiceAEnd2.class),
                    serviceCreateInput
                            .getServiceZEnd()
                            .augmentation(ServiceZEnd2.class),
                    PceCrossDomainOrchestrator.kpathOrder,
                    pcs,
                    nps
            );
        } else {
            return returnTapiPCRFailed();
        }
    }


    private static PathComputationRequestOutput performPCE(
            HardConstraints hardConstraints,
            SoftConstraints softConstraints,
            String serviceName,
            SdncRequestHeader sdncRequestHeader,
            ServiceEndpoint serviceAEnd,
            ServiceEndpoint serviceZEnd,
            ServiceNotificationTypes servNotifType,
            ServicePathNotificationTypes servPathNotifType,
            boolean reserveResource,
            String customerName,
            SpectrumAllocation spectrumAEndAllocation,
            SpectrumAllocation spectrumZEndAllocation,
            int kpathorder,
            PathComputationService pcs,
            NotificationPublishService nps
    ) {
        // TODO: define specific notification type so that nothing happens when recieved in regular PCE and so that
        // we handle where needed the notification associated with this specific tapi-domain PathComputationRequest
        LOG.info("Calling path computation.");
        notification = new ServiceRpcResultShBuilder().setNotificationType(servNotifType).setServiceName(serviceName)
                .setStatus(RpcStatusEx.Pending)
                .setStatusMessage("Service compliant, submitting PathComputation Request ...").build();
        try {
            nps.putNotification(notification);
        } catch (InterruptedException e) {
            LOG.info(NOTIFICATION_OFFER_REJECTED_MSG, e);
        }
        FutureCallback<PathComputationRequestOutput> pceCallback = PceCrossDomainOrchestrator.getInstance(null, null)
                    .new Pcro2ndStepCallback(servPathNotifType, serviceName, kpathorder);
        //TODO: PreProcess the input parameters so that service name is correctly formated and triggers Path computation
        // Using TAPI PCE (or change Algo of PCE so that it detects the 2nd step PC and slect this criteria rather
        // than serviceName format == Uuid to use TAPI flavor of the PCE
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
        //TODO : find a way to set pceOperMode where appropriate
        ListenableFuture<PathComputationRequestOutput> pce = pcs.pathComputationRequest(pathComputationRequestInput);
        Futures.addCallback(pce, pceCallback, EXECUTOR);

        ConfigurationResponseCommon configurationResponseCommon = new ConfigurationResponseCommonBuilder()
            // TODO: define new Response code? so that nothing happens when recieved in regular PCE and so that
            // we handle where needed the notification associated with this specific tapi-domain PathComputationRequest
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


    private static PathComputationRequestInput createPceRequestInput(
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
            .setServiceAEnd(createServiceAEnd(serviceAEnd))
            .setServiceZEnd(createServiceZEnd(serviceZEnd))
            .build();
    }

    public static ServiceAEnd createServiceAEnd(ServiceEndpoint serviceAEnd) {

        String nodeAid = serviceAEnd.getNodeId().getValue();
        String intermediateNodeAid = nodeAid.substring(2, nodeAid.length());
        if (getUuidFromInput(intermediateNodeAid).getValue().equals(intermediateNodeAid)) {
            // For request exercised through Tapi, the provided NodeId in the ServiceAend is a Uuid that has been
            // modified adding "aa" at the begining to fit with OR NodeIdType pattern : will use the initial Uuid
            // as the Node Id. Otherwise use as is.
            nodeAid = intermediateNodeAid;
        }
        ServiceAEndBuilder serviceAEndBuilder = new ServiceAEndBuilder()
                .setClli(serviceAEnd.getClli())
                .setNodeId(nodeAid)
                .setRxDirection(
                        createRxDirection(serviceAEnd.getRxDirection().values().stream().findFirst().orElseThrow()))
                .setServiceFormat(serviceAEnd.getServiceFormat())
                .setServiceRate(serviceAEnd.getServiceRate())
                .setTxDirection(
                        createTxDirection(serviceAEnd.getTxDirection().values().stream().findFirst().orElseThrow()));

        return serviceAEndBuilder.build();
    }

    public static ServiceZEnd createServiceZEnd(ServiceEndpoint serviceZEnd) {

        String nodeZid = serviceZEnd.getNodeId().getValue();
        String intermediateNodeZid = nodeZid.substring(2, nodeZid.length());
        if (getUuidFromInput(intermediateNodeZid).getValue().equals(intermediateNodeZid)) {
            // For request exercised through Tapi, the provided NodeId in the ServiceAend is a Uuid that has been
            // modified adding "aa" at the begining to fit with OR NodeIdType pattern : will use the initial Uuid
            // as the Node Id. Otherwise use as is.
            nodeZid = intermediateNodeZid;
        }
        ServiceZEndBuilder serviceZEndBuilder = new ServiceZEndBuilder()
                .setClli(serviceZEnd.getClli())
                .setNodeId(nodeZid)
                .setRxDirection(
                        createRxDirection(serviceZEnd.getRxDirection().values().stream().findFirst().orElseThrow()))
                .setServiceFormat(serviceZEnd.getServiceFormat())
                .setServiceRate(serviceZEnd.getServiceRate())
                .setTxDirection(
                        createTxDirection(serviceZEnd.getTxDirection().values().stream().findFirst().orElseThrow()));

        return serviceZEndBuilder.build();
    }

    private static RxDirection createRxDirection(
            org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev250530
                .service.endpoint.RxDirection rxDirection) {
        return new RxDirectionBuilder().setPort(rxDirection.getPort()).build();
    }

    private static TxDirection createTxDirection(
            org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev250530
                .service.endpoint.TxDirection txDirection) {
        return new TxDirectionBuilder().setPort(txDirection.getPort()).build();
    }

    private static Uuid getUuidFromInput(String inString) {
        if (inString == null) {
            return null;
        }
        Uuid outUuid;
        Pattern uuidRegex =
            Pattern.compile("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$");
        if (uuidRegex.matcher(inString).matches()) {
            outUuid = new Uuid(inString);
        } else {
            outUuid = new Uuid(UUID.nameUUIDFromBytes(inString.getBytes(StandardCharsets.UTF_8)).toString());
        }
        return outUuid;
    }

    private static PathComputationRequestOutput returnTapiPCRFailed() {
        ConfigurationResponseCommon configurationResponseCommon = new ConfigurationResponseCommonBuilder()
                .setAckFinalIndicator(ResponseCodes.FINAL_ACK_YES).setResponseCode(ResponseCodes.RESPONSE_FAILED)
                .setResponseMessage("PCE calculation failed").build();
        ResponseParameters reponseParameters = new ResponseParametersBuilder().build();
        return new PathComputationRequestOutputBuilder().setConfigurationResponseCommon(configurationResponseCommon)
                .setResponseParameters(reponseParameters).build();
    }

    private static Boolean validateParams(String serviceName, SdncRequestHeader sdncRequestHeader) {
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

    public PathComputationService getPathComputationService() {
        return this.pathComputationService;
    }

    public NotificationPublishService getnotificationPublishService() {
        return this.notificationPublishService;
    }

    public static void setKpathOrder(int kpathorder) {
        PceCrossDomainOrchestrator.kpathOrder = kpathorder;
    }

    private record EndPoint(String nodeId, Uuid nodeUuid, String tpName, Uuid tpUuid, Uuid topoUuid) {}

    private record AzEndPoint(EndPoint aendPoint, EndPoint zendPoint) {}

    private final class Pcro2ndStepCallback implements FutureCallback<PathComputationRequestOutput> {
        private final ServicePathNotificationTypes notifType;
        private final String serviceName;
        private final int kpathOrder;
        String message = "";
        SecondStepHybridPcResult notification = null;

        private Pcro2ndStepCallback(ServicePathNotificationTypes notifType, String serviceName, int kpathorder) {
            this.notifType = notifType;
            this.serviceName = serviceName;
            this.kpathOrder = kpathorder;
        }

        @Override
        public void onSuccess(PathComputationRequestOutput response) {
            if (response != null) {
                /**
                 * If PCE reply, in 2nd step of Hybrid path computation, is received before timer expiration with a
                 * positive result, the second step path computation is successfully terminated and the notification
                 * triggers first step path computation ending.
                 */

                message = "PCE replied to PCR Request in second step of hybrid path computation!";
                LOG.info("PCE replied to PCR Request in second step of hybrid path computation: {}", response);
                notification = new SecondStepHybridPcResultBuilder()
                        .setServiceName(serviceName)
                        .setNotificationType(notifType)
                        .setSelectedKpathOrder(Uint8.valueOf(kpathOrder))
                        .setAggregatedPathDescription(PceCrossDomainPathAggregator.getInstance().aggPathDescription)
                        .setCrossDomainService(PceCrossDomainPathAggregator.getInstance().crossDomainService)
                        .setStatus(RpcStatusEx.Successful).setStatusMessage(message).build();
                try {
                    notificationPublishService.putNotification(notification);
                } catch (InterruptedException e) {
                    LOG.info(NOTIFICATION_OFFER_REJECTED_MSG, e);
                }
            } else {
                message = "PCE failed ";
                notification = new SecondStepHybridPcResultBuilder()
                        .setServiceName(serviceName)
                        .setNotificationType(notifType)
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
            notification = new SecondStepHybridPcResultBuilder()
                    .setServiceName(serviceName)
                    .setNotificationType(notifType)
                    .setSelectedKpathOrder(Uint8.valueOf(kpathOrder))
                    //.setAggregatedPathDescription(null)
                    //.setCrossDomainService(null)
                    .setStatus(RpcStatusEx.Failed).setStatusMessage("2nd step PC failed for K = " + kpathOrder
                        + "path, " + arg0.getMessage())
                    .build();
            try {
                notificationPublishService.putNotification(notification);
            } catch (InterruptedException e) {
                LOG.info(NOTIFICATION_OFFER_REJECTED_MSG, e);
            }
        }
    }

}