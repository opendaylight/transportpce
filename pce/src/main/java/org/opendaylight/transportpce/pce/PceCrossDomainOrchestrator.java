/*
 * Copyright © 2026 Orange, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.pce;

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
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev240205.PathComputationRequestInput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev240205.PathComputationRequestInputBuilder;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev240205.PathComputationRequestOutput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev240205.PathComputationRequestOutputBuilder;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev240205.path.computation.request.input.ServiceAEnd;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev240205.path.computation.request.input.ServiceAEndBuilder;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev240205.path.computation.request.input.ServiceZEnd;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev240205.path.computation.request.input.ServiceZEndBuilder;
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
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.service.types.rev220118.PceMetric;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.service.types.rev220118.RpcStatusEx;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.service.types.rev220118.response.parameters.sp.ResponseParameters;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.service.types.rev220118.response.parameters.sp.ResponseParametersBuilder;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.service.types.rev220118.service.endpoint.sp.RxDirection;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.service.types.rev220118.service.endpoint.sp.RxDirectionBuilder;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.service.types.rev220118.service.endpoint.sp.TxDirection;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.service.types.rev220118.service.endpoint.sp.TxDirectionBuilder;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.service.types.rev220118.service.handler.header.ServiceHandlerHeaderBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.Uuid;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
public class PceCrossDomainOrchestrator {

    /* Logging. */
    private static final Logger LOG = LoggerFactory.getLogger(PceCrossDomainOrchestrator.class);
    private static final String NOTIFICATION_OFFER_REJECTED_MSG = "notification offer rejected : ";
    private static final String PERFORMING_PCE_CROSS_DOMAIN_MSG = "performing TAPI PCE (Cross-Domain-Service...";
    private PathComputationService pathComputationService;
    private NotificationPublishService notificationPublishService;
    private ServiceRpcResultSh notification = null;
    private final ListeningExecutorService executor;

    @Activate
    public PceCrossDomainOrchestrator(
//            @Reference RpcProviderService rpcProviderService,
            @Reference PathComputationService pathComputationService,
            @Reference NotificationPublishService notificationPublishService) {

        this.notificationPublishService = notificationPublishService;
        this.pathComputationService = pathComputationService;
        executor = MoreExecutors.listeningDecorator(Executors.newFixedThreadPool(5));
    }

    public PathComputationRequestOutput performPCE(ServiceCreateInput serviceCreateInput, boolean reserveResource) {
        LOG.info(PERFORMING_PCE_CROSS_DOMAIN_MSG);
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
            return returnTapiPCRFailed();
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
        // TODO: define specific notification type so that nothing happens when recieved in regular PCE and so that
        // we handle where needed the notification associated with this specific tapi-domain PathComputationRequest
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
        //TODO : find a way to set pceOperMode where appropriate
        ListenableFuture<PathComputationRequestOutput> pce = this.pathComputationService
                .pathComputationRequest(pathComputationRequestInput);
        Futures.addCallback(pce, pceCallback, executor);

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

    private record EndPoint(String nodeId, Uuid nodeUuid, String tpName, Uuid tpUuid, Uuid topoUuid) {}

    private record AzEndPoint(EndPoint aendPoint, EndPoint zendPoint) {}

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
           // TODO: define specific notification type so that nothing happens when recieved in regular PCE and so that
           // we handle where needed the notification associated with this specific tapi-domain PathComputationRequest
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
