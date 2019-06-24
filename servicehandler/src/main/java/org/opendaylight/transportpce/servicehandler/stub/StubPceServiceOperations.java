/*
 * Copyright © 2017 Orange, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.servicehandler.stub;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;

import java.util.concurrent.Callable;
import java.util.concurrent.Executors;

import org.opendaylight.controller.md.sal.binding.api.NotificationPublishService;
import org.opendaylight.transportpce.common.ResponseCodes;
import org.opendaylight.transportpce.pce.service.PathComputationService;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev190624.CancelResourceReserveInput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev190624.CancelResourceReserveOutput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev190624.CancelResourceReserveOutputBuilder;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev190624.PathComputationRequestInput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev190624.PathComputationRequestOutput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev190624.PathComputationRequestOutputBuilder;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev190624.ServicePathRpcResult;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev190624.ServicePathRpcResultBuilder;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev190624.service.path.rpc.result.PathDescription;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev190624.service.path.rpc.result.PathDescriptionBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev161014.configuration.response.common.ConfigurationResponseCommon;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev161014.configuration.response.common.ConfigurationResponseCommonBuilder;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev171017.path.description.AToZDirection;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev171017.path.description.AToZDirectionBuilder;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev171017.path.description.ZToADirection;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev171017.path.description.ZToADirectionBuilder;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.service.types.rev171016.RpcStatusEx;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.service.types.rev171016.ServicePathNotificationTypes;
import org.opendaylight.yangtools.yang.binding.Notification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StubPceServiceOperations implements PathComputationService {

    private static final Logger LOG = LoggerFactory.getLogger(StubPceServiceOperations.class);
    private final NotificationPublishService notificationPublishService;
    private Boolean pceFailed;
    private final ListeningExecutorService executor;

    public StubPceServiceOperations(NotificationPublishService notificationPublishService) {
        this.notificationPublishService = notificationPublishService;
        executor = MoreExecutors.listeningDecorator(Executors.newFixedThreadPool(2));
        this.pceFailed = false;
    }

    private void sendNotifications(Notification notif) {
        try {
            LOG.info("putting notification : {}", notif);
            notificationPublishService.putNotification(notif);
        } catch (InterruptedException e) {
            LOG.info("notification offer rejected : ", e.getMessage());
        }
    }

    @Override
    public ListenableFuture<CancelResourceReserveOutput> cancelResourceReserve(CancelResourceReserveInput input) {
        return executor.submit(new Callable<CancelResourceReserveOutput>() {
            @Override
            public CancelResourceReserveOutput call() throws Exception {
                String serviceName = input.getServiceName();
                RpcStatusEx rpcStatusEx = RpcStatusEx.Pending;
                ServicePathRpcResult notification = new ServicePathRpcResultBuilder()
                        .setNotificationType(ServicePathNotificationTypes.CancelResourceReserve)
                        .setServiceName(serviceName).setStatus(rpcStatusEx)
                        .setStatusMessage("Service compliant, submitting cancel resource Request ...").build();
                sendNotifications(notification);
                String message = "";
                String responseCode = null;
                ConfigurationResponseCommon configurationResponseCommon = null;
                CancelResourceReserveOutput output = null;
                try {
                    LOG.info("Wait for 5s til beginning the PCE cancel resource request");
                    Thread.sleep(5000); // sleep for 1s
                } catch (InterruptedException e) {
                    message = "path computation service failed !";
                    LOG.error("path computation service failed !", e);
                    responseCode = ResponseCodes.RESPONSE_FAILED;
                    rpcStatusEx = RpcStatusEx.Failed;
                    notification = new ServicePathRpcResultBuilder()
                            .setNotificationType(ServicePathNotificationTypes.CancelResourceReserve)
                            .setStatus(rpcStatusEx).setStatusMessage(message).setServiceName(serviceName).build();
                    sendNotifications(notification);
                    configurationResponseCommon =
                            new ConfigurationResponseCommonBuilder().setAckFinalIndicator(ResponseCodes.FINAL_ACK_YES)
                                    .setRequestId(input.getServiceHandlerHeader().getRequestId())
                                    .setResponseCode(responseCode).setResponseMessage(message).build();
                    output = new CancelResourceReserveOutputBuilder()
                            .setConfigurationResponseCommon(configurationResponseCommon).build();
                }
                if (pceFailed) {
                    LOG.info("forcing pce to fail");
                    message = "pce failed !";
                    rpcStatusEx = RpcStatusEx.Failed;
                    LOG.error(message);
                    responseCode = ResponseCodes.RESPONSE_FAILED;
                } else {
                    message = "path computated !";
                    rpcStatusEx = RpcStatusEx.Successful;
                    LOG.error(message);
                    responseCode = ResponseCodes.RESPONSE_OK;
                }
                notification = new ServicePathRpcResultBuilder()
                        .setNotificationType(ServicePathNotificationTypes.CancelResourceReserve)
                        .setStatus(RpcStatusEx.Successful).setStatusMessage(message)
                        .setServiceName(serviceName).build();
                sendNotifications(notification);
                configurationResponseCommon =
                        new ConfigurationResponseCommonBuilder().setAckFinalIndicator(ResponseCodes.FINAL_ACK_YES)
                                .setRequestId(input.getServiceHandlerHeader().getRequestId())
                                .setResponseCode(responseCode).setResponseMessage(message).build();
                output = new CancelResourceReserveOutputBuilder()
                        .setConfigurationResponseCommon(configurationResponseCommon).build();
                return output;
            }
        });

    }

    @Override
    public ListenableFuture<PathComputationRequestOutput> pathComputationRequest(PathComputationRequestInput input) {
        return executor.submit(new Callable<PathComputationRequestOutput>() {
            @Override
            public PathComputationRequestOutput call() throws Exception {
                String serviceName = input.getServiceName();
                RpcStatusEx rpcStatusEx = RpcStatusEx.Pending;
                ServicePathRpcResult notification = new ServicePathRpcResultBuilder()
                        .setNotificationType(ServicePathNotificationTypes.PathComputationRequest)
                        .setServiceName(serviceName).setStatus(rpcStatusEx)
                        .setStatusMessage("Service compliant, submitting pathComputation Request ...").build();
                sendNotifications(notification);
                String message = "";
                String responseCode = null;
                ConfigurationResponseCommon configurationResponseCommon = null;
                PathComputationRequestOutput output = null;
                try {
                    LOG.info("Wait for 5s til beginning the PCE pathComputation request");
                    Thread.sleep(5000); // sleep for 1s
                } catch (InterruptedException e) {
                    message = "path computation service failed !";
                    LOG.error("path computation service failed !", e);
                    responseCode = ResponseCodes.RESPONSE_FAILED;
                    rpcStatusEx = RpcStatusEx.Failed;
                    notification = new ServicePathRpcResultBuilder()
                            .setNotificationType(ServicePathNotificationTypes.PathComputationRequest)
                            .setStatus(rpcStatusEx).setStatusMessage(message).setServiceName(serviceName).build();
                    sendNotifications(notification);
                    configurationResponseCommon =
                            new ConfigurationResponseCommonBuilder().setAckFinalIndicator(ResponseCodes.FINAL_ACK_YES)
                                    .setRequestId(input.getServiceHandlerHeader().getRequestId())
                                    .setResponseCode(responseCode).setResponseMessage(message).build();
                    output = new PathComputationRequestOutputBuilder()
                            .setConfigurationResponseCommon(configurationResponseCommon).build();
                }
                PathDescription value;
                if (pceFailed) {
                    value = null;
                    LOG.info("forcing pce to fail");
                    message = "pce failed !";
                    rpcStatusEx = RpcStatusEx.Failed;
                    LOG.error(message);
                    responseCode = ResponseCodes.RESPONSE_FAILED;
                } else {
                    value = createPathDescription(0L, 5L, 0L, 5L);
                    message = "path computated !";
                    rpcStatusEx = RpcStatusEx.Successful;
                    LOG.error(message);
                    responseCode = ResponseCodes.RESPONSE_OK;
                }
                notification = new ServicePathRpcResultBuilder()
                        .setNotificationType(ServicePathNotificationTypes.PathComputationRequest)
                        .setPathDescription(value)
                        .setStatus(rpcStatusEx).setStatusMessage(message)
                        .setServiceName(serviceName).build();
                sendNotifications(notification);
                configurationResponseCommon =
                        new ConfigurationResponseCommonBuilder().setAckFinalIndicator(ResponseCodes.FINAL_ACK_YES)
                                .setRequestId(input.getServiceHandlerHeader().getRequestId())
                                .setResponseCode(responseCode)
                                .setResponseMessage(message).build();
                output = new PathComputationRequestOutputBuilder()
                        .setConfigurationResponseCommon(configurationResponseCommon).build();
                return output;
            }
        });
    }

    private static PathDescription createPathDescription(long azRate, long azWaveLength, long zaRate,
            long zaWaveLength) {
        AToZDirection atozDirection =
                new AToZDirectionBuilder().setRate(azRate).setAToZWavelengthNumber(azWaveLength).setAToZ(null).build();
        ZToADirection ztoaDirection =
                new ZToADirectionBuilder().setRate(zaRate).setZToAWavelengthNumber(zaWaveLength).setZToA(null).build();
        PathDescription pathDescription =
                new PathDescriptionBuilder().setAToZDirection(atozDirection).setZToADirection(ztoaDirection).build();
        return pathDescription;
    }

    public void setPceFailed(Boolean pceFailed) {
        this.pceFailed = pceFailed;
    }
}
