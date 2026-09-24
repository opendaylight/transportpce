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
import java.util.BitSet;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import org.opendaylight.mdsal.binding.api.NotificationPublishService;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.transportpce.common.ResponseCodes;
import org.opendaylight.transportpce.common.network.NetworkTransactionService;
import org.opendaylight.transportpce.pce.service.PathComputationService;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev260910.PathComputationRequestInput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev260910.PathComputationRequestInputBuilder;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev260910.PathComputationRequestOutput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev260910.PathComputationRequestOutputBuilder;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev260910.SecondStepHybridPcResult;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev260910.SecondStepHybridPcResultBuilder;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev260910.second.step.hybrid.pc.result.CrossDomainServiceBuilder;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.servicehandler.rev201125.ServiceRpcResultSh;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.servicehandler.rev201125.ServiceRpcResultShBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev250530.ServiceNotificationTypes;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev250530.configuration.response.common.ConfigurationResponseCommon;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev250530.configuration.response.common.ConfigurationResponseCommonBuilder;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.service.types.rev260910.PceMetric;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.service.types.rev260910.RpcStatusEx;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.service.types.rev260910.ServicePathNotificationTypes;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.service.types.rev260910.response.parameters.sp.ResponseParameters;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.service.types.rev260910.response.parameters.sp.ResponseParametersBuilder;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.service.types.rev260910.service.handler.header.ServiceHandlerHeaderBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.NetworkId;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.Networks;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.NodeId;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.networks.Network;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.networks.NetworkKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.Node1;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.TpId;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.networks.network.node.TerminationPoint;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.networks.network.node.TerminationPointKey;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.Uuid;
import org.opendaylight.yangtools.binding.DataObjectIdentifier;
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
    private NetworkTransactionService networkTransactionService;
    private static ServiceRpcResultSh notification;
    private static int kpathOrder;
    private static final ListeningExecutorService EXECUTOR = MoreExecutors
        .listeningDecorator(Executors.newFixedThreadPool(5));


    private PceCrossDomainOrchestrator(PathComputationService pcs, NotificationPublishService nps,
            NetworkTransactionService nts) {

        this.notificationPublishService = nps;
        this.pathComputationService = pcs;
        this.networkTransactionService = nts;

    }

    public static synchronized PceCrossDomainOrchestrator getInstance(final PathComputationService pcs,
        final NotificationPublishService nps, final NetworkTransactionService nts) {
        if (instance == null) {
            instance = new PceCrossDomainOrchestrator(pcs, nps, nts);
        }
        return instance;
    }

    public static PathComputationRequestOutput performPCE(
            PathComputationRequestInput pathComputationRequestInput, String initialRequestId, String customerName,
            BitSet spectrumConstraint, PathComputationService pcs, NotificationPublishService nps) {
        LOG.info(PceCrossDomainOrchestrator.PERFORMING_PCE_CROSS_DOMAIN_MSG + "for K path order = {}",
            PceCrossDomainOrchestrator.kpathOrder);
        if (validateParams(pathComputationRequestInput.getServiceName(), initialRequestId)) {

            return performPCE(
                    pathComputationRequestInput,
                    initialRequestId,
                    customerName,
                    ServiceNotificationTypes.ServiceCreateResult,
                    ServicePathNotificationTypes.SecondStepHybridPcRequest,
                    spectrumConstraint,
                    PceCrossDomainOrchestrator.kpathOrder,
                    pcs,
                    nps
            );
        } else {
            return returnTapiPCRFailed();
        }
    }


    private static PathComputationRequestOutput performPCE(
            PathComputationRequestInput pathComputationRequestInput,
            String initialRequestId,
            String customerName,
            ServiceNotificationTypes servNotifType,
            ServicePathNotificationTypes servPathNotifType,
            BitSet spectrumConstraint,
            int kpathorder,
            PathComputationService pcs,
            NotificationPublishService nps
    ) {
        // TODO: define specific notification type so that nothing happens when recieved in regular PCE and so that
        // we handle where needed the notification associated with this specific tapi-domain PathComputationRequest
        LOG.info("Calling path computation.");
        notification = new ServiceRpcResultShBuilder()
                .setNotificationType(servNotifType)
                .setServiceName(pathComputationRequestInput.getServiceName())
                .setStatus(RpcStatusEx.Pending)
                .setStatusMessage("Service compliant, submitting PathComputation Request ...").build();
        try {
            nps.putNotification(notification);
        } catch (InterruptedException e) {
            LOG.info(NOTIFICATION_OFFER_REJECTED_MSG, e);
        }
        FutureCallback<PathComputationRequestOutput> pceCallback = PceCrossDomainOrchestrator
                .getInstance(null, null, null).new Pcro2ndStepCallback(
                        servPathNotifType, pathComputationRequestInput.getServiceName(), kpathorder);
        createPceRequestInput(pathComputationRequestInput, initialRequestId, customerName, spectrumConstraint);
        //TODO : find a way to set pceOperMode where appropriate
        ListenableFuture<PathComputationRequestOutput> pce = pcs.pathComputationRequest(pathComputationRequestInput);
        Futures.addCallback(pce, pceCallback, EXECUTOR);

        ConfigurationResponseCommon configurationResponseCommon = new ConfigurationResponseCommonBuilder()
            // TODO: define new Response code? so that nothing happens when recieved in regular PCE and so that
            // we handle where needed the notification associated with this specific tapi-domain PathComputationRequest
                .setAckFinalIndicator(ResponseCodes.FINAL_ACK_NO)
                .setRequestId(initialRequestId)
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
            PathComputationRequestInput pathComputationRequestInput, String initialRequestId, String customerName,
            BitSet spectrumConstraint) {
        // Do not use/implement  Spectrum constraint at that time
        LOG.info("Mapping ServiceCreateInput or ServiceFeasibilityCheckInput or serviceReconfigureInput to PCE"
                + "requests");
        ServiceHandlerHeaderBuilder serviceHandlerHeader = new ServiceHandlerHeaderBuilder();
        if (initialRequestId != null && !initialRequestId.isBlank()) {
            serviceHandlerHeader.setRequestId(initialRequestId + "2nd-Step-hybridPC");
        }
        return new PathComputationRequestInputBuilder()
            .setServiceName(pathComputationRequestInput.getServiceName())
            .setResourceReserve(pathComputationRequestInput.getResourceReserve())
            .setServiceHandlerHeader(serviceHandlerHeader.build())
            .setHardConstraints(pathComputationRequestInput.getHardConstraints())
            .setSoftConstraints(pathComputationRequestInput.getSoftConstraints())
            .setPceRoutingMetric(PceMetric.TEMetric)
            .setCustomerName(customerName)
            .setServiceAEnd(pathComputationRequestInput.getServiceAEnd())
            .setServiceZEnd(pathComputationRequestInput.getServiceZEnd())
            .build();
    }

    private static PathComputationRequestOutput returnTapiPCRFailed() {
        ConfigurationResponseCommon configurationResponseCommon = new ConfigurationResponseCommonBuilder()
                .setAckFinalIndicator(ResponseCodes.FINAL_ACK_YES).setResponseCode(ResponseCodes.RESPONSE_FAILED)
                .setResponseMessage("PCE calculation failed").build();
        ResponseParameters reponseParameters = new ResponseParametersBuilder().build();
        return new PathComputationRequestOutputBuilder().setConfigurationResponseCommon(configurationResponseCommon)
                .setResponseParameters(reponseParameters).build();
    }

    private static Boolean validateParams(String serviceName, String initialRequestId) {
        boolean result = true;
        if (!checkString(serviceName)) {
            result = false;
            LOG.error("Service Name (common-id for Temp service) is not set");
        } else if (initialRequestId == null || initialRequestId.isBlank()) {
            LOG.error("Service sdncRequestHeader 'request-id' is not set");
            result = false;
        }
        return result;
    }

    public TerminationPoint getTapiSbiNodeTPinORTopology(TpId tpId, String netLayer) {

        String tapiSBInode = "TAPI-SBI-ABS-NODE";
        TerminationPoint sbiTp = null;
        DataObjectIdentifier<TerminationPoint> orTopologyTpIID = DataObjectIdentifier.builder(Networks.class)
            .child(Network.class, new NetworkKey(new NetworkId(netLayer)))
            .child(
                org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226
                    .networks.network.Node.class,
                new org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226
                    .networks.network.NodeKey(new NodeId(tapiSBInode)))
            .augmentation(Node1.class)
            .child(TerminationPoint.class, new TerminationPointKey(tpId))
            .build();
        try {
            Optional<TerminationPoint> tpOptional = networkTransactionService
                .read(LogicalDatastoreType.CONFIGURATION, orTopologyTpIID).get();
            if (tpOptional.isPresent()) {
                sbiTp = tpOptional.orElseThrow();
            }
        } catch (InterruptedException | ExecutionException e) {

            LOG.error("readMdSal: Error reading TAPI-SBI-ABS-NODE tp {} , tp does not exist", orTopologyTpIID);
            throw new RuntimeException(
                "readMdSal: Error reading from operational store, Operational Mode Catalog : " + orTopologyTpIID + " :"
                    + e);
        }
        return sbiTp;
    }

    private static boolean checkString(String value) {
        return ((value != null) && (!value.isBlank()));
    }

    public PathComputationService getPathComputationService() {
        return this.pathComputationService;
    }

    public NotificationPublishService getnotificationPublishService() {
        return this.notificationPublishService;
    }

    public NetworkTransactionService getnetworkTransactionService() {
        return this.networkTransactionService;
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
                        .setCrossDomainService(new CrossDomainServiceBuilder().setCdServices(
                                PceCrossDomainPathAggregator.getInstance().buildCdService(kpathOrder)).build())
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
