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
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import org.opendaylight.mdsal.binding.api.NotificationPublishService;
import org.opendaylight.transportpce.common.network.NetworkTransactionService;
import org.opendaylight.transportpce.pce.PceComplianceCheck;
import org.opendaylight.transportpce.pce.PceComplianceCheckResult;
import org.opendaylight.transportpce.pce.PceSendingPceRPCs;
import org.opendaylight.transportpce.pce.gnpy.GnpyResult;
import org.opendaylight.yang.gen.v1.gnpy.path.rev200202.result.Response;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev200128.CancelResourceReserveInput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev200128.CancelResourceReserveOutput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev200128.CancelResourceReserveOutputBuilder;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev200128.PathComputationRequestInput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev200128.PathComputationRequestOutput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev200128.PathComputationRequestOutputBuilder;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev200128.ServicePathRpcResult;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev200128.ServicePathRpcResultBuilder;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev200128.gnpy.GnpyResponse;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev200128.gnpy.GnpyResponseBuilder;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev200128.gnpy.gnpy.response.ResponseType;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev200128.gnpy.gnpy.response.response.type.NoPathCase;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev200128.gnpy.gnpy.response.response.type.NoPathCaseBuilder;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev200128.gnpy.gnpy.response.response.type.PathCase;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev200128.gnpy.gnpy.response.response.type.PathCaseBuilder;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev200128.path.performance.PathProperties;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev200128.path.performance.PathPropertiesBuilder;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev200128.path.performance.path.properties.PathMetric;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev200128.path.performance.path.properties.PathMetricBuilder;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev200128.service.path.rpc.result.PathDescription;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev200128.service.path.rpc.result.PathDescriptionBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev190531.configuration.response.common.ConfigurationResponseCommonBuilder;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev171017.path.description.AToZDirection;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev171017.path.description.ZToADirection;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.service.types.rev200128.RpcStatusEx;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.service.types.rev200128.ServicePathNotificationTypes;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.service.types.rev200128.response.parameters.sp.ResponseParametersBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PathComputationServiceImpl implements PathComputationService {

    private static final Logger LOG = LoggerFactory.getLogger(PathComputationServiceImpl.class);
    private final NotificationPublishService notificationPublishService;
    private NetworkTransactionService networkTransactionService;
    private final ListeningExecutorService executor;
    ServicePathRpcResult notification = null;

    public PathComputationServiceImpl(NetworkTransactionService networkTransactionService,
                                      NotificationPublishService notificationPublishService) {
        this.notificationPublishService = notificationPublishService;
        this.networkTransactionService = networkTransactionService;
        this.executor = MoreExecutors.listeningDecorator(Executors.newFixedThreadPool(5));
    }

    public void init() {
        LOG.info("init ...");
    }

    public void close() {
        LOG.info("close.");
    }

    private void sendNotifications(ServicePathNotificationTypes servicePathNotificationTypes, String serviceName,
            RpcStatusEx rpcStatusEx, String message, PathDescription pathDescription) {
        ServicePathRpcResultBuilder servicePathRpcResultBuilder =
                new ServicePathRpcResultBuilder().setNotificationType(servicePathNotificationTypes)
                        .setServiceName(serviceName).setStatus(rpcStatusEx).setStatusMessage(message);
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
                String message = "";
                sendNotifications(ServicePathNotificationTypes.CancelResourceReserve, input.getServiceName(),
                        RpcStatusEx.Pending, "Service compliant, submitting cancelResourceReserve Request ...", null);
                PceSendingPceRPCs sendingPCE = new PceSendingPceRPCs();
                sendingPCE.cancelResourceReserve();
                if (Boolean.TRUE.equals(sendingPCE.getSuccess())) {
                    message = "ResourceReserve cancelled !";
                } else {
                    message = "Cancelling ResourceReserve failed !";
                }
                LOG.info("in PathComputationServiceImpl : {}",message);
                sendNotifications(ServicePathNotificationTypes.CancelResourceReserve, input.getServiceName(),
                        RpcStatusEx.Successful, "cancel Resource Reserve successful!", null);
                ConfigurationResponseCommonBuilder configurationResponseCommon =
                        new ConfigurationResponseCommonBuilder();
                configurationResponseCommon.setAckFinalIndicator("Yes")
                        .setRequestId(input.getServiceHandlerHeader().getRequestId()).setResponseCode("200")
                        .setResponseMessage("");
                CancelResourceReserveOutputBuilder output = new CancelResourceReserveOutputBuilder();
                output.setConfigurationResponseCommon(configurationResponseCommon.build());
                return output.build();
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
                    sendNotifications(ServicePathNotificationTypes.PathComputationRequest, input.getServiceName(),
                            RpcStatusEx.Failed, "Path not calculated, service not compliant", null);
                    configurationResponseCommon.setAckFinalIndicator("Yes")
                            .setRequestId(input.getServiceHandlerHeader().getRequestId())
                            .setResponseCode("Path not calculated").setResponseMessage(check.getMessage());
                    output.setConfigurationResponseCommon(configurationResponseCommon.build())
                            .setResponseParameters(null);
                    return output.build();
                }
                sendNotifications(ServicePathNotificationTypes.PathComputationRequest, input.getServiceName(),
                        RpcStatusEx.Pending, "Service compliant, submitting pathComputation Request ...", null);
                String message = "";
                String responseCode = "";
                PceSendingPceRPCs sendingPCE = new PceSendingPceRPCs(input, networkTransactionService);
                sendingPCE.pathComputation();
                message = sendingPCE.getMessage();
                responseCode = sendingPCE.getResponseCode();
                PathDescriptionBuilder path = null;
                path = sendingPCE.getPathDescription();
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
                output.setGnpyResponse(listResponse);

                if (Boolean.FALSE.equals(sendingPCE.getSuccess()) || (path == null)) {
                    configurationResponseCommon.setAckFinalIndicator("Yes")
                            .setRequestId(input.getServiceHandlerHeader().getRequestId()).setResponseCode(responseCode)
                            .setResponseMessage(message);
                    output.setConfigurationResponseCommon(configurationResponseCommon.build());
                    sendNotifications(ServicePathNotificationTypes.PathComputationRequest, input.getServiceName(),
                            RpcStatusEx.Failed, "Path not calculated", null);
                    return output.build();
                }
                // Path calculator returned Success
                configurationResponseCommon.setAckFinalIndicator("Yes")
                        .setRequestId(input.getServiceHandlerHeader().getRequestId()).setResponseCode(responseCode)
                        .setResponseMessage(message);
                PathDescription pathDescription = new org.opendaylight.yang.gen.v1.http.org.opendaylight
                        .transportpce.pce.rev200128.service.path.rpc.result.PathDescriptionBuilder()
                                .setAToZDirection(path.getAToZDirection()).setZToADirection(path.getZToADirection())
                                .build();
                sendNotifications(ServicePathNotificationTypes.PathComputationRequest, input.getServiceName(),
                        RpcStatusEx.Successful, message, pathDescription);
                org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.service.types.rev200128.response
                    .parameters.sp.response.parameters.PathDescription pathDescription1 = new org.opendaylight.yang.gen
                        .v1.http.org.transportpce.b.c._interface.service.types.rev200128.response.parameters.sp
                        .response.parameters.PathDescriptionBuilder()
                                .setAToZDirection(path.getAToZDirection()).setZToADirection(path.getZToADirection())
                                .build();
                ResponseParametersBuilder rpb = new ResponseParametersBuilder().setPathDescription(pathDescription1);
                output.setConfigurationResponseCommon(configurationResponseCommon.build())
                        .setResponseParameters(rpb.build());

                //debug prints
                AToZDirection atoz = pathDescription.getAToZDirection();
                if ((atoz != null) && (atoz.getAToZ() != null)) {
                    LOG.debug("Impl AtoZ Notification: [{}] elements in description", atoz.getAToZ().size());
                    for (int i = 0; i < atoz.getAToZ().size(); i++) {
                        LOG.debug("Impl AtoZ Notification: [{}] {}", i, atoz.getAToZ().get(i));
                    }
                }
                ZToADirection ztoa = pathDescription.getZToADirection();
                if ((ztoa != null) && (ztoa.getZToA() != null)) {
                    LOG.debug("Impl ZtoA Notification: [{}] elements in description", ztoa.getZToA().size());
                    for (int i = 0; i < ztoa.getZToA().size(); i++) {
                        LOG.debug("Impl ZtoA Notification: [{}] {}", i, ztoa.getZToA().get(i));
                    }
                }
                return output.build();
            }
        });
    }

    public GnpyResponse generateGnpyResponse(Response responseGnpy, String pathDir) {
        ResponseType respType = null;
        boolean feasible = true;
        if (responseGnpy != null) {
            if (responseGnpy.getResponseType() instanceof org.opendaylight.yang.gen.v1.gnpy.path.rev200202.result
                    .response.response.type.NoPathCase) {
                LOG.info("GNPy : path is not feasible");
                org.opendaylight.yang.gen.v1.gnpy.path.rev200202.result.response.response.type.NoPathCase
                    noPathGnpy = (org.opendaylight.yang.gen.v1.gnpy.path.rev200202.result.response.response.type
                    .NoPathCase) responseGnpy.getResponseType();
                NoPathCase noPathCase = new NoPathCaseBuilder().setNoPath(noPathGnpy.getNoPath()).build();
                respType = noPathCase;
                feasible = false;
            } else if (responseGnpy.getResponseType() instanceof org.opendaylight.yang.gen.v1.gnpy.path.rev200202.result
                    .response.response.type.PathCase) {
                LOG.info("GNPy : path is feasible");
                org.opendaylight.yang.gen.v1.gnpy.path.rev200202.result.response.response.type.PathCase pathCase =
                        (org.opendaylight.yang.gen.v1.gnpy.path.rev200202.result.response.response.type.PathCase)
                        responseGnpy.getResponseType();
                List<org.opendaylight.yang.gen.v1.gnpy.path.rev200202.generic.path.properties.path.properties
                    .PathMetric> pathMetricList = pathCase.getPathProperties().getPathMetric();
                List<PathMetric> gnpyPathMetricList = new ArrayList<>();
                for (org.opendaylight.yang.gen.v1.gnpy.path.rev200202.generic.path.properties.path.properties.PathMetric
                        pathMetricGnpy : pathMetricList) {
                    PathMetric pathMetric = new PathMetricBuilder().setMetricType(pathMetricGnpy.getMetricType())
                            .setAccumulativeValue(pathMetricGnpy.getAccumulativeValue()).build();
                    gnpyPathMetricList.add(pathMetric);
                }
                PathProperties pathProperties = new PathPropertiesBuilder().setPathMetric(gnpyPathMetricList).build();
                PathCase gnpyPathCase = new PathCaseBuilder().setPathProperties(pathProperties).build();
                respType = gnpyPathCase;
                feasible = true;
            }
        }
        return new GnpyResponseBuilder().setPathDir(pathDir).setResponseType(respType).setFeasibility(feasible).build();
    }

}
