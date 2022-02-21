/*
 * Copyright © 2017 AT&T, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.pce.service;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import org.opendaylight.mdsal.binding.api.NotificationPublishService;
import org.opendaylight.transportpce.common.mapping.PortMapping;
import org.opendaylight.transportpce.common.network.NetworkTransactionService;
import org.opendaylight.transportpce.pce.PceComplianceCheck;
import org.opendaylight.transportpce.pce.PceComplianceCheckResult;
import org.opendaylight.transportpce.pce.PceSendingPceRPCs;
import org.opendaylight.transportpce.pce.gnpy.GnpyResult;
import org.opendaylight.transportpce.pce.gnpy.consumer.GnpyConsumer;
import org.opendaylight.yang.gen.v1.gnpy.path.rev200909.result.Response;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev220118.CancelResourceReserveInput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev220118.CancelResourceReserveOutput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev220118.CancelResourceReserveOutputBuilder;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev220118.PathComputationRequestInput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev220118.PathComputationRequestOutput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev220118.PathComputationRequestOutputBuilder;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev220118.ServicePathRpcResult;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev220118.ServicePathRpcResultBuilder;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev220118.gnpy.GnpyResponse;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev220118.gnpy.GnpyResponseBuilder;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev220118.gnpy.gnpy.response.response.type.NoPathCaseBuilder;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev220118.gnpy.gnpy.response.response.type.PathCaseBuilder;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev220118.path.performance.PathPropertiesBuilder;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev220118.path.performance.path.properties.PathMetric;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev220118.path.performance.path.properties.PathMetricBuilder;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev220118.service.path.rpc.result.PathDescription;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev220118.service.path.rpc.result.PathDescriptionBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev211210.configuration.response.common.ConfigurationResponseCommonBuilder;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev210705.path.description.AToZDirection;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev210705.path.description.ZToADirection;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.service.types.rev220118.RpcStatusEx;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.service.types.rev220118.ServicePathNotificationTypes;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.service.types.rev220118.response.parameters.sp.ResponseParametersBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PathComputationServiceImpl implements PathComputationService {

    private static final Logger LOG = LoggerFactory.getLogger(PathComputationServiceImpl.class);
    private final NotificationPublishService notificationPublishService;
    private NetworkTransactionService networkTransactionService;
    private final ListeningExecutorService executor;
    private ServicePathRpcResult notification = null;
    private final GnpyConsumer gnpyConsumer;
    private PortMapping portMapping;

    public PathComputationServiceImpl(NetworkTransactionService networkTransactionService,
                                      NotificationPublishService notificationPublishService,
                                      GnpyConsumer gnpyConsumer, PortMapping portMapping) {
        this.notificationPublishService = notificationPublishService;
        this.networkTransactionService = networkTransactionService;
        this.executor = MoreExecutors.listeningDecorator(Executors.newFixedThreadPool(5));
        this.gnpyConsumer = gnpyConsumer;
        this.portMapping = portMapping;
    }

    public void init() {
        LOG.info("init ...");
    }

    public void close() {
        LOG.info("close.");
    }

    @SuppressFBWarnings(
        value = "UPM_UNCALLED_PRIVATE_METHOD",
        justification = "false positive, this method is used by public method cancelResourceReserve")
    private void sendNotifications(
            ServicePathNotificationTypes servicePathNotificationTypes,
            String serviceName,
            RpcStatusEx rpcStatusEx,
            String message,
            PathDescription pathDescription) {
        ServicePathRpcResultBuilder servicePathRpcResultBuilder =
            new ServicePathRpcResultBuilder()
                .setNotificationType(servicePathNotificationTypes)
                .setServiceName(serviceName)
                .setStatus(rpcStatusEx)
                .setStatusMessage(message);
        if (pathDescription != null) {
            servicePathRpcResultBuilder.setPathDescription(pathDescription);
        }
        this.notification = servicePathRpcResultBuilder.build();
        try {
            notificationPublishService.putNotification(this.notification);
        } catch (InterruptedException e) {
            LOG.info("notification offer rejected: ", e);
        }
    }

    @Override
    public ListenableFuture<CancelResourceReserveOutput> cancelResourceReserve(CancelResourceReserveInput input) {
        LOG.info("cancelResourceReserve");
        return executor.submit(new Callable<CancelResourceReserveOutput>() {

            @Override
            public CancelResourceReserveOutput call() throws Exception {
                sendNotifications(
                        ServicePathNotificationTypes.CancelResourceReserve,
                        input.getServiceName(),
                        RpcStatusEx.Pending,
                        "Service compliant, submitting cancelResourceReserve Request ...",
                        null);
                PceSendingPceRPCs sendingPCE = new PceSendingPceRPCs(gnpyConsumer);
                sendingPCE.cancelResourceReserve();
                LOG.info("in PathComputationServiceImpl : {}",
                        Boolean.TRUE.equals(sendingPCE.getSuccess())
                            ? "ResourceReserve cancelled !"
                            : "Cancelling ResourceReserve failed !");
                sendNotifications(
                        ServicePathNotificationTypes.CancelResourceReserve,
                        input.getServiceName(),
                        RpcStatusEx.Successful,
                        "cancel Resource Reserve successful!",
                        null);
                return new CancelResourceReserveOutputBuilder()
                    .setConfigurationResponseCommon(
                        new ConfigurationResponseCommonBuilder()
                            .setAckFinalIndicator("Yes")
                            .setRequestId(input.getServiceHandlerHeader().getRequestId())
                            .setResponseCode("200")
                            .setResponseMessage("")
                            .build())
                    .build();
            }
        });
    }

    @Override
    public ListenableFuture<PathComputationRequestOutput> pathComputationRequest(PathComputationRequestInput input) {
        LOG.info("pathComputationRequest");
        return executor.submit(new Callable<PathComputationRequestOutput>() {

            @Override
            public PathComputationRequestOutput call() throws Exception {
                PathComputationRequestOutputBuilder output = new PathComputationRequestOutputBuilder();
                ConfigurationResponseCommonBuilder configurationResponseCommon =
                        new ConfigurationResponseCommonBuilder();
                PceComplianceCheckResult check = PceComplianceCheck.check(input);
                if (!check.hasPassed()) {
                    LOG.error("Path not calculated, service not compliant : {}", check.getMessage());
                    sendNotifications(
                        ServicePathNotificationTypes.PathComputationRequest,
                        input.getServiceName(),
                        RpcStatusEx.Failed,
                        "Path not calculated, service not compliant",
                        null);
                    configurationResponseCommon
                            .setAckFinalIndicator("Yes")
                            .setRequestId(input.getServiceHandlerHeader().getRequestId())
                            .setResponseCode("Path not calculated")
                            .setResponseMessage(check.getMessage());
                    return output
                        .setConfigurationResponseCommon(configurationResponseCommon.build())
                        .setResponseParameters(null)
                        .build();
                }
                sendNotifications(
                    ServicePathNotificationTypes.PathComputationRequest,
                    input.getServiceName(),
                    RpcStatusEx.Pending,
                    "Service compliant, submitting pathComputation Request ...",
                    null);
                PceSendingPceRPCs sendingPCE =
                    new PceSendingPceRPCs(input, networkTransactionService, gnpyConsumer, portMapping);
                sendingPCE.pathComputation();
                String message = sendingPCE.getMessage();
                String responseCode = sendingPCE.getResponseCode();
                LOG.info("PCE response: {} {}", message, responseCode);

                //add the GNPy result
                GnpyResult gnpyAtoZ = sendingPCE.getGnpyAtoZ();
                GnpyResult gnpyZtoA = sendingPCE.getGnpyZtoA();
                List<GnpyResponse> listResponse = new ArrayList<>();
                if (gnpyAtoZ != null) {
                    GnpyResponse respAtoZ = generateGnpyResponse(gnpyAtoZ.getResponse(),"A-to-Z");
                    listResponse.add(respAtoZ);
                }
                if (gnpyZtoA != null) {
                    GnpyResponse respZtoA = generateGnpyResponse(gnpyZtoA.getResponse(),"Z-to-A");
                    listResponse.add(respZtoA);
                }
                output
                    .setGnpyResponse(
                        listResponse.stream()
                            .collect(Collectors.toMap(GnpyResponse::key, gnpyResponse -> gnpyResponse)));

                PathDescriptionBuilder path = sendingPCE.getPathDescription();
                if (Boolean.FALSE.equals(sendingPCE.getSuccess()) || (path == null)) {
                    sendNotifications(
                        ServicePathNotificationTypes.PathComputationRequest,
                        input.getServiceName(),
                        RpcStatusEx.Failed,
                        "Path not calculated",
                        null);
                    return output
                        .setConfigurationResponseCommon(
                            configurationResponseCommon
                                .setAckFinalIndicator("Yes")
                                .setRequestId(input.getServiceHandlerHeader().getRequestId())
                                .setResponseCode(responseCode)
                                .setResponseMessage(message)
                                .build())
                        .build();
                }
                // Path calculator returned Success
                PathDescription pathDescription =
                    new org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev220118
                            .service.path.rpc.result.PathDescriptionBuilder()
                        .setAToZDirection(path.getAToZDirection())
                        .setZToADirection(path.getZToADirection())
                        .build();
                sendNotifications(
                    ServicePathNotificationTypes.PathComputationRequest,
                    input.getServiceName(),
                    RpcStatusEx.Successful,
                    message,
                    pathDescription);
                org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.service.types.rev220118
                        .response.parameters.sp.response.parameters.PathDescription pathDescription1 =
                    new org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.service.types.rev220118
                            .response.parameters.sp.response.parameters.PathDescriptionBuilder()
                        .setAToZDirection(path.getAToZDirection())
                        .setZToADirection(path.getZToADirection())
                        .build();
                output
                    .setConfigurationResponseCommon(
                        configurationResponseCommon
                            .setAckFinalIndicator("Yes")
                            .setRequestId(input.getServiceHandlerHeader().getRequestId())
                            .setResponseCode(responseCode)
                            .setResponseMessage(message)
                            .build())
                    .setResponseParameters(
                        new ResponseParametersBuilder().setPathDescription(pathDescription1).build());
                //debug prints
                AToZDirection atoz = pathDescription.getAToZDirection();
                if ((atoz != null) && (atoz.getAToZ() != null)) {
                    LOG.debug("Impl AtoZ Notification: [{}] elements in description", atoz.getAToZ().size());
                    for (org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev210705
                            .path.description.atoz.direction.AToZKey key : atoz.getAToZ().keySet()) {
                        LOG.debug("Impl AtoZ Notification: [{}] {}", key, atoz.getAToZ().get(key));
                    }
                }
                ZToADirection ztoa = pathDescription.getZToADirection();
                if ((ztoa != null) && (ztoa.getZToA() != null)) {
                    LOG.debug("Impl ZtoA Notification: [{}] elements in description", ztoa.getZToA().size());
                    for (org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev210705
                            .path.description.ztoa.direction.ZToAKey key : ztoa.getZToA().keySet()) {
                        LOG.debug("Impl ZtoA Notification: [{}] {}", key, ztoa.getZToA().get(key));
                    }
                }
                return output.build();
            }
        });
    }

    public GnpyResponse generateGnpyResponse(Response responseGnpy, String pathDir) {
        if (responseGnpy == null) {
            return new GnpyResponseBuilder()
                .setPathDir(pathDir)
                .setResponseType(null)
                .setFeasibility(true)
                .build();
        }
        if (responseGnpy.getResponseType()
                instanceof org.opendaylight.yang.gen.v1.gnpy.path.rev200909.result.response.response.type.NoPathCase) {
            LOG.info("GNPy : path is not feasible");
            org.opendaylight.yang.gen.v1.gnpy.path.rev200909.result.response.response.type.NoPathCase
                    noPathGnpy =
                (org.opendaylight.yang.gen.v1.gnpy.path.rev200909.result.response.response.type.NoPathCase)
                    responseGnpy.getResponseType();
            return new GnpyResponseBuilder()
                .setPathDir(pathDir)
                .setResponseType(
                    new NoPathCaseBuilder()
                        .setNoPath(noPathGnpy.getNoPath())
                        .build())
                .setFeasibility(false)
                .build();
        }
        if (responseGnpy.getResponseType()
                instanceof org.opendaylight.yang.gen.v1.gnpy.path.rev200909.result.response.response.type.PathCase) {
            LOG.info("GNPy : path is feasible");
            org.opendaylight.yang.gen.v1.gnpy.path.rev200909.result.response.response.type.PathCase
                    pathCase =
                (org.opendaylight.yang.gen.v1.gnpy.path.rev200909.result.response.response.type.PathCase)
                    responseGnpy.getResponseType();
            List<org.opendaylight.yang.gen.v1.gnpy.path.rev200909.generic.path.properties.path.properties.PathMetric>
                    pathMetricList =
                new ArrayList<>(pathCase.getPathProperties().getPathMetric().values());
            List<PathMetric> gnpyPathMetricList = new ArrayList<>();
            for (org.opendaylight.yang.gen.v1.gnpy.path.rev200909.generic.path.properties.path.properties.PathMetric
                    pathMetricGnpy : pathMetricList) {
                gnpyPathMetricList.add(
                    new PathMetricBuilder()
                        .setMetricType(pathMetricGnpy.getMetricType())
                        .setAccumulativeValue(pathMetricGnpy.getAccumulativeValue())
                        .build());
            }
            return new GnpyResponseBuilder()
                .setPathDir(pathDir)
                .setResponseType(
                    new PathCaseBuilder()
                        .setPathProperties(
                            new PathPropertiesBuilder()
                                .setPathMetric(gnpyPathMetricList.stream()
                                    .collect(Collectors.toMap(PathMetric::key, pathMetric -> pathMetric)))
                                .build())
                        .build())
                .setFeasibility(true)
                .build();
        }
        return new GnpyResponseBuilder()
            .setPathDir(pathDir)
            .setResponseType(null)
            .setFeasibility(true)
            .build();
    }

}
