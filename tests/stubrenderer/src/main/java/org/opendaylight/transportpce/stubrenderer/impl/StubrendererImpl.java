/*
 * Copyright © 2017 Orange, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */


package org.opendaylight.transportpce.stubrenderer.impl;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;

import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.opendaylight.controller.md.sal.binding.api.NotificationPublishService;
import org.opendaylight.transportpce.stubrenderer.SendingRendererRPCs;
import org.opendaylight.transportpce.stubrenderer.StubrendererCompliancyCheck;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev161014.configuration.response.common.ConfigurationResponseCommonBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev161014.service.TopologyBuilder;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.service.types.rev170426.RpcStatusEx;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.service.types.rev170426.ServicePathNotificationTypes;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.servicepath.rev170426.CancelResourceReserveInput;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.servicepath.rev170426.CancelResourceReserveOutput;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.servicepath.rev170426.PathComputationRequestInput;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.servicepath.rev170426.PathComputationRequestOutput;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.servicepath.rev170426.ServiceDeleteInput;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.servicepath.rev170426.ServiceDeleteOutput;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.servicepath.rev170426.ServiceDeleteOutputBuilder;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.servicepath.rev170426.ServiceImplementationRequestInput;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.servicepath.rev170426.ServiceImplementationRequestOutput;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.servicepath.rev170426.ServiceImplementationRequestOutputBuilder;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.servicepath.rev170426.ServiceRpcResultSp;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.servicepath.rev170426.ServiceRpcResultSpBuilder;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.servicepath.rev170426.TransportpceServicepathService;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.servicepath.rev170426.service.rpc.result.sp.PathTopology;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.servicepath.rev170426.service.rpc.result.sp.PathTopologyBuilder;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;



/**
 * Class to implement StubrendererService.
 * @author Martial Coulibaly ( martial.coulibaly@gfi.com ) on behalf of Orange
 *
 */
public class StubrendererImpl implements TransportpceServicepathService {
    /** Logging. */
    private static final Logger LOG = LoggerFactory.getLogger(StubrendererImpl.class);
    /** send notification. */
    private NotificationPublishService notificationPublishService;
    private ServiceRpcResultSp notification;
    private final ListeningExecutorService executor = MoreExecutors
            .listeningDecorator(Executors.newFixedThreadPool(10));
    /** check service sdnc-request-header compliancy. */
    private StubrendererCompliancyCheck compliancyCheck;

    public StubrendererImpl(NotificationPublishService notificationPublishService) {
        this.notificationPublishService = notificationPublishService;
    }

    @Override
    public Future<RpcResult<ServiceImplementationRequestOutput>> serviceImplementationRequest(
            ServiceImplementationRequestInput input) {
        LOG.info("RPC  serviceImplementationRequest request received");
        String responseCode = "";
        String message = "";
        ConfigurationResponseCommonBuilder configurationResponseCommon = null;

        compliancyCheck = new StubrendererCompliancyCheck(input.getServiceName(), input.getServiceHandlerHeader());
        if (compliancyCheck.check(false, true)) {
            LOG.info("Service compliant !");
            /**
             * If compliant, service-request parameters are verified in order to
             * check if there is no missing parameter that prevents calculating
             * a path and implement a service.
             */

            notification = new ServiceRpcResultSpBuilder()
                    .setNotificationType(ServicePathNotificationTypes.ServiceImplementationRequest)
                    .setServiceName(input.getServiceName())
                    .setStatus(RpcStatusEx.Pending)
                    .setStatusMessage("Service compliant, submitting serviceImplementation Request ...")
                    .build();
            try {
                notificationPublishService.putNotification(notification);
            } catch (InterruptedException e) {
                LOG.info("notification offer rejected : " + e);
            }

            SendingRendererRPCs sendingRenderer = new SendingRendererRPCs(executor);
            FutureCallback<Boolean> rendererCallback =
                    new FutureCallback<Boolean>() {
                String message = "";
                ServiceRpcResultSp notification = null;

                @Override
                public void onFailure(Throwable arg0) {
                    LOG.error("Failure message : " + arg0.toString());
                    LOG.error("Service implementation failed !");
                    notification = new ServiceRpcResultSpBuilder()
                            .setNotificationType(ServicePathNotificationTypes.ServiceImplementationRequest)
                            .setServiceName(input.getServiceName()).setStatus(RpcStatusEx.Failed)
                            .setStatusMessage("PCR Request failed  : " + arg0.getMessage()).build();
                    try {
                        notificationPublishService.putNotification(notification);
                    } catch (InterruptedException e) {
                        LOG.info("notification offer rejected : " + e);
                    }
                }

                @Override
                public void onSuccess(Boolean response) {
                    LOG.info("response : " + response);
                    if (response) {
                        message = "Service implemented !";
                        TopologyBuilder topo = sendingRenderer.getTopology();
                        ServiceRpcResultSpBuilder tmp = new ServiceRpcResultSpBuilder()
                                .setNotificationType(ServicePathNotificationTypes.ServiceImplementationRequest)
                                .setServiceName(input.getServiceName())
                                .setStatus(RpcStatusEx.Successful)
                                .setStatusMessage(message);
                        if (topo != null) {
                            PathTopology value = new PathTopologyBuilder()
                                    .setAToZ(topo.getAToZ())
                                    .setZToA(topo.getZToA())
                                    .build();
                            tmp.setPathTopology(value);
                        }
                        notification = tmp.build();
                    } else {
                        message = "Service implementation failed : " + sendingRenderer.getError();
                        notification = new ServiceRpcResultSpBuilder()
                                .setNotificationType(ServicePathNotificationTypes.ServiceImplementationRequest)
                                .setServiceName("")
                                .setStatus(RpcStatusEx.Failed).setStatusMessage(message)
                                .build();
                    }
                    LOG.info(notification.toString());
                    try {
                        notificationPublishService.putNotification(notification);
                    } catch (InterruptedException e) {
                        LOG.info("notification offer rejected : " + e);
                    }
                    LOG.info(message);
                }
            };
            ListenableFuture<Boolean> renderer = sendingRenderer.serviceImplementation();
            Futures.addCallback(renderer, rendererCallback, executor);
            LOG.info("Service implmentation Request in progress ");
            configurationResponseCommon = new ConfigurationResponseCommonBuilder()
                    .setAckFinalIndicator("Yes")
                    .setRequestId(input.getServiceHandlerHeader().getRequestId())
                    .setResponseCode("200")
                    .setResponseMessage("Service implementation Request in progress ");

            ServiceImplementationRequestOutput output = new ServiceImplementationRequestOutputBuilder()
                    .setConfigurationResponseCommon(configurationResponseCommon.build())
                    .build();
            return RpcResultBuilder.success(output).buildFuture();
        } else {
            message = compliancyCheck.getMessage();
            responseCode = "500";
            LOG.info("Service not compliant caused by : " + message);
            notification = new ServiceRpcResultSpBuilder()
                    .setNotificationType(ServicePathNotificationTypes.ServiceDelete)
                    .setServiceName(input.getServiceName()).setStatus(RpcStatusEx.Failed)
                    .setStatusMessage("Service not compliant caused by : " + message)
                    .build();
            try {
                notificationPublishService.putNotification(notification);
            } catch (InterruptedException e) {
                LOG.info("notification offer rejected : " + e);
            }
        }
        configurationResponseCommon = new ConfigurationResponseCommonBuilder()
                .setAckFinalIndicator("yes")
                .setRequestId(input.getServiceHandlerHeader().getRequestId())
                .setResponseCode(responseCode)
                .setResponseMessage(message);
        ServiceImplementationRequestOutput output = new ServiceImplementationRequestOutputBuilder()
                .setConfigurationResponseCommon(configurationResponseCommon.build())
                .build();

        return RpcResultBuilder.success(output).buildFuture();
    }

    @Override
    public Future<RpcResult<ServiceDeleteOutput>> serviceDelete(ServiceDeleteInput input) {
        String message = "";
        LOG.info("RPC serviceDelete request received");
        String responseCode = "";
        ConfigurationResponseCommonBuilder configurationResponseCommon = null;
        compliancyCheck = new StubrendererCompliancyCheck(input.getServiceName(), input.getServiceHandlerHeader());
        if (compliancyCheck.check(false, true)) {
            LOG.info("Service compliant !");
            /**
             * If compliant, service-request parameters are verified in order to
             * check if there is no missing parameter that prevents calculating
             * a path and implement a service.
             */

            notification = new ServiceRpcResultSpBuilder()
                    .setNotificationType(ServicePathNotificationTypes.ServiceDelete)
                    .setServiceName(input.getServiceName())
                    .setStatus(RpcStatusEx.Pending)
                    .setStatusMessage("Service compliant, submitting serviceDelete Request ...")
                    .build();
            try {
                notificationPublishService.putNotification(notification);
            } catch (InterruptedException e) {
                LOG.info("notification offer rejected : " + e);
            }
            SendingRendererRPCs sendingRenderer = new SendingRendererRPCs(executor);
            FutureCallback<Boolean> rendererCallback = new FutureCallback<Boolean>() {
                String message = "";
                ServiceRpcResultSp notification = null;

                @Override
                public void onFailure(Throwable arg0) {
                    LOG.error("Failure message : " + arg0.toString());
                    LOG.error("Service delete failed !");
                    notification = new ServiceRpcResultSpBuilder()
                            .setNotificationType(ServicePathNotificationTypes.ServiceDelete)
                            .setServiceName(input.getServiceName()).setStatus(RpcStatusEx.Failed)
                            .setStatusMessage("PCR Request failed  : " + arg0.getMessage()).build();
                    try {
                        notificationPublishService.putNotification(notification);
                    } catch (InterruptedException e) {
                        LOG.info("notification offer rejected : " + e);
                    }
                }

                @Override
                public void onSuccess(Boolean response) {
                    LOG.info("response : " + response);
                    if (response) {
                        message = "Service deleted !";
                        notification = new ServiceRpcResultSpBuilder()
                                .setNotificationType(ServicePathNotificationTypes.ServiceDelete)
                                .setServiceName(input.getServiceName()).setStatus(RpcStatusEx.Successful)
                                .setStatusMessage(message).build();
                    } else {
                        message = "Service delete failed : " + sendingRenderer.getError();
                        notification = new ServiceRpcResultSpBuilder()
                                .setNotificationType(ServicePathNotificationTypes.ServiceDelete)
                                .setServiceName("")
                                .setStatus(RpcStatusEx.Failed).setStatusMessage(message)
                                .build();
                    }
                    LOG.info(notification.toString());
                    try {
                        notificationPublishService.putNotification(notification);
                    } catch (InterruptedException e) {
                        LOG.info("notification offer rejected : " + e);
                    }
                    LOG.info(message);
                }
            };
            ListenableFuture<Boolean> renderer = sendingRenderer.serviceDelete();
            Futures.addCallback(renderer, rendererCallback, executor);
            message = "Service delete Request in progress ...";
            LOG.info(message);
            configurationResponseCommon = new ConfigurationResponseCommonBuilder()
                    .setAckFinalIndicator("Yes")
                    .setRequestId(input.getServiceHandlerHeader().getRequestId())
                    .setResponseCode("200")
                    .setResponseMessage(message);
            ServiceDeleteOutput output = new ServiceDeleteOutputBuilder()
                    .setConfigurationResponseCommon(configurationResponseCommon.build())
                    .build();
            return RpcResultBuilder.success(output).buildFuture();
        } else {
            message = compliancyCheck.getMessage();
            LOG.info("Service not compliant caused by : " + message);
            responseCode = "500";
            notification = new ServiceRpcResultSpBuilder()
                    .setNotificationType(ServicePathNotificationTypes.ServiceDelete)
                    .setServiceName(input.getServiceName()).setStatus(RpcStatusEx.Failed)
                    .setStatusMessage("Service not compliant caused by : " + message)
                    .build();
            try {
                notificationPublishService.putNotification(notification);
            } catch (InterruptedException e) {
                LOG.info("notification offer rejected : " + e);
            }
        }
        configurationResponseCommon = new ConfigurationResponseCommonBuilder()
                .setAckFinalIndicator("yes")
                .setRequestId(input.getServiceHandlerHeader().getRequestId())
                .setResponseCode(responseCode)
                .setResponseMessage(message);
        ServiceDeleteOutput output = new ServiceDeleteOutputBuilder()
                .setConfigurationResponseCommon(configurationResponseCommon.build())
                .build();
        return RpcResultBuilder.success(output).buildFuture();
    }

    @Override
    public Future<RpcResult<CancelResourceReserveOutput>> cancelResourceReserve(CancelResourceReserveInput input) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Future<RpcResult<PathComputationRequestOutput>> pathComputationRequest(PathComputationRequestInput input) {
        // TODO Auto-generated method stub
        return null;
    }
}
