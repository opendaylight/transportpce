/*
 * Copyright © 2017 Orange, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */


package org.opendaylight.transportpce.stubpce.impl;

import java.util.concurrent.Future;

import org.opendaylight.controller.md.sal.binding.api.NotificationPublishService;
import org.opendaylight.transportpce.stubpce.CompliancyCheck;
import org.opendaylight.transportpce.stubpce.SendingPceRPCs;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.stubpce.rev170426.CancelResourceReserveInput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.stubpce.rev170426.CancelResourceReserveOutput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.stubpce.rev170426.CancelResourceReserveOutputBuilder;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.stubpce.rev170426.PathComputationRequestInput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.stubpce.rev170426.PathComputationRequestOutput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.stubpce.rev170426.PathComputationRequestOutputBuilder;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.stubpce.rev170426.ServicePathRpcResult;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.stubpce.rev170426.ServicePathRpcResultBuilder;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.stubpce.rev170426.StubpceService;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.stubpce.rev170426.service.path.rpc.result.PathDescription;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev161014.configuration.response.common.ConfigurationResponseCommonBuilder;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.service.types.rev170426.RpcStatusEx;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.service.types.rev170426.ServicePathNotificationTypes;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.service.types.rev170426.response.parameters.sp.response.parameters.PathDescriptionBuilder;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Class to implement
 * StubpceService
 * StubpceListener.
 *
 * @author Martial Coulibaly ( martial.coulibaly@gfi.com ) on behalf of Orange
 *
 */

public class StubpceImpl implements StubpceService {

    /* Logging. */
    private static final Logger LOG = LoggerFactory.getLogger(StubpceImpl.class);

    private CompliancyCheck compliancyCheck;
    /* send notification. */
    private NotificationPublishService notificationPublishService;
    private ServicePathRpcResult notification;

    public StubpceImpl(NotificationPublishService notificationPublishService) {
        this.notificationPublishService = notificationPublishService;
    }

    @Override
    public Future<RpcResult<CancelResourceReserveOutput>> cancelResourceReserve(CancelResourceReserveInput input) {
        LOG.info("RPC cancelResourceReserve  request received");
        String message = "";

        notification = new ServicePathRpcResultBuilder()
            .setNotificationType(ServicePathNotificationTypes.CancelResourceReserve)
            .setServiceName(input.getServiceName())
            .setStatus(RpcStatusEx.Pending)
            .setStatusMessage("Service compliant, submitting cancelResourceReserve Request ...")
            .build();
        try {
            notificationPublishService.putNotification(notification);
        } catch (InterruptedException e) {
            LOG.info("notification offer rejected : " + e);
        }

        SendingPceRPCs sendingPCE = new SendingPceRPCs();
        sendingPCE.cancelResourceReserve();
        if (sendingPCE.getSuccess()) {
            message = "ResourceReserve cancelled ! ";
        } else {
            message = "Cancelling ResourceReserve failed ! ";
        }
        LOG.info(message);
        ConfigurationResponseCommonBuilder configurationResponseCommon = new ConfigurationResponseCommonBuilder();
        configurationResponseCommon
            .setAckFinalIndicator("Yes")
            .setRequestId(input.getServiceHandlerHeader().getRequestId())
            .setResponseCode("200")
            .setResponseMessage("")
            .setResponseMessage(message);
        CancelResourceReserveOutputBuilder output  = new CancelResourceReserveOutputBuilder();
        output
            .setConfigurationResponseCommon(configurationResponseCommon.build());
        return RpcResultBuilder.success(output.build()).buildFuture();
    }


    @Override
    public Future<RpcResult<PathComputationRequestOutput>> pathComputationRequest(PathComputationRequestInput input) {
        LOG.info("RPC pathcomputation request received");
        String message = "";
        PathComputationRequestOutputBuilder output = new PathComputationRequestOutputBuilder();
        ConfigurationResponseCommonBuilder configurationResponseCommon = new ConfigurationResponseCommonBuilder();

        compliancyCheck = new CompliancyCheck(input);
        if (!compliancyCheck.check()) {
            configurationResponseCommon
                .setAckFinalIndicator("Yes")
                .setRequestId(input.getServiceHandlerHeader().getRequestId())
                .setResponseCode("Path not calculated")
                .setResponseMessage(compliancyCheck.getMessage());

            output
                .setConfigurationResponseCommon(configurationResponseCommon.build())
                .setResponseParameters(null);

            return RpcResultBuilder.success(output.build()).buildFuture();
        }
        notification = new ServicePathRpcResultBuilder()
            .setNotificationType(ServicePathNotificationTypes.PathComputationRequest)
            .setServiceName(input.getServiceName())
            .setStatus(RpcStatusEx.Pending)
            .setStatusMessage("Service compliant, submitting pathComputation Request ...")
            .build();
        try {
            notificationPublishService.putNotification(notification);
        } catch (InterruptedException e) {
            LOG.info("notification offer rejected : " + e);
        }

        SendingPceRPCs sendingPCE = new SendingPceRPCs();
        sendingPCE.pathComputation();
        if (sendingPCE.getSuccess()) {
            message = "Path Computated !";
            ServicePathRpcResultBuilder tmp = new ServicePathRpcResultBuilder()
                .setNotificationType(ServicePathNotificationTypes.PathComputationRequest)
                .setServiceName(input.getServiceName())
                .setStatus(RpcStatusEx.Successful)
                .setStatusMessage(message);
            PathDescriptionBuilder path = sendingPCE.getPathDescription();
            if (path != null) {
                PathDescription pathDescription = new org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce
                        .stubpce.rev170426.service.path.rpc.result.PathDescriptionBuilder()
                    .setAToZDirection(path.getAToZDirection())
                    .setZToADirection(path.getZToADirection())
                    .build();
                tmp.setPathDescription(pathDescription);
            }
            notification = tmp.build();
            try {
                notificationPublishService.putNotification(notification);
            } catch (InterruptedException e) {
                LOG.info("notification offer rejected : " + e);
            }
        } else {
            message = "Path Computating failed !";
        }
        LOG.info(message);
        configurationResponseCommon
            .setAckFinalIndicator("Yes")
            .setRequestId(input.getServiceHandlerHeader().getRequestId())
            .setResponseCode("200")
            .setResponseMessage(message);

        output
            .setConfigurationResponseCommon(configurationResponseCommon.build());
        return RpcResultBuilder.success(output.build()).buildFuture();

    }
}
