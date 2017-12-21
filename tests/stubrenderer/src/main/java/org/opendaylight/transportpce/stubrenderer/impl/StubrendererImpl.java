/*
 * Copyright © 2017 Orange, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */


package org.opendaylight.transportpce.stubrenderer.impl;

import java.util.concurrent.Future;

import org.opendaylight.controller.md.sal.binding.api.NotificationPublishService;
import org.opendaylight.transportpce.stubrenderer.SendingRendererRPCs;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.stubrenderer.rev170426.ServiceDeleteInput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.stubrenderer.rev170426.ServiceDeleteOutput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.stubrenderer.rev170426.ServiceDeleteOutputBuilder;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.stubrenderer.rev170426.ServiceImplementationRequestInput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.stubrenderer.rev170426.ServiceImplementationRequestOutput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.stubrenderer.rev170426.ServiceImplementationRequestOutputBuilder;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.stubrenderer.rev170426.ServiceRpcResultSp;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.stubrenderer.rev170426.ServiceRpcResultSpBuilder;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.stubrenderer.rev170426.StubrendererService;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.stubrenderer.rev170426.service.rpc.result.sp.PathTopology;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.stubrenderer.rev170426.service.rpc.result.sp.PathTopologyBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev161014.configuration.response.common.ConfigurationResponseCommonBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev161014.service.TopologyBuilder;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.service.types.rev170426.RpcStatusEx;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.service.types.rev170426.ServicePathNotificationTypes;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Class to implement StubrendererService.
 * @author Martial Coulibaly ( martial.coulibaly@gfi.com ) on behalf of Orange
 *
 */
public class StubrendererImpl implements StubrendererService {
    /* Logging. */
    private static final Logger LOG = LoggerFactory.getLogger(StubrendererImpl.class);
    /* send notification. */
    private NotificationPublishService notificationPublishService;
    private ServiceRpcResultSp notification;

    public StubrendererImpl(NotificationPublishService notificationPublishService) {
        this.notificationPublishService = notificationPublishService;
    }

    @Override
    public Future<RpcResult<ServiceImplementationRequestOutput>> serviceImplementationRequest(
            ServiceImplementationRequestInput input) {
        String message = "";
        LOG.info("RPC  serviceImplementationRequest request received");

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

        SendingRendererRPCs sendingRenderer = new SendingRendererRPCs();
        sendingRenderer.serviceImplementation();
        if (sendingRenderer.getSuccess()) {
            message = "Service implemented !";
            ServiceRpcResultSpBuilder tmp = new ServiceRpcResultSpBuilder()
                .setNotificationType(ServicePathNotificationTypes.ServiceImplementationRequest)
                .setServiceName(input.getServiceName())
                .setStatus(RpcStatusEx.Successful)
                .setStatusMessage(message);
            TopologyBuilder topo = sendingRenderer.getTopology();
            if (topo != null) {
                PathTopology path = new PathTopologyBuilder()
                    .setAToZ(topo.getAToZ())
                    .setZToA(topo.getZToA())
                    .build();
                tmp.setPathTopology(path);
            }
            notification = tmp.build();
            try {
                notificationPublishService.putNotification(notification);
            } catch (InterruptedException e) {
                LOG.info("notification offer rejected : " + e);
            }

        } else {
            message = "Service not implemented !";
        }
        LOG.info(message);
        ConfigurationResponseCommonBuilder configurationResponseCommon = new ConfigurationResponseCommonBuilder()
            .setAckFinalIndicator("Yes")
            .setRequestId(input.getServiceHandlerHeader().getRequestId())
            .setResponseCode("200")
            .setResponseMessage(message);

        ServiceImplementationRequestOutput output = new ServiceImplementationRequestOutputBuilder()
            .setConfigurationResponseCommon(configurationResponseCommon.build())
            .build();

        return RpcResultBuilder.success(output).buildFuture();
    }

    @Override
    public Future<RpcResult<ServiceDeleteOutput>> serviceDelete(ServiceDeleteInput input) {
        String message = "";
        LOG.info("RPC  serviceDelete request received");

        notification = new ServiceRpcResultSpBuilder()
            .setNotificationType(ServicePathNotificationTypes.ServiceDelete)
            .setServiceName(input.getServiceName())
            .setStatus(RpcStatusEx.Pending)
            .setStatusMessage("Service compliant, submitting ServiceDelete Request ...")
            .build();
        try {
            notificationPublishService.putNotification(notification);
        } catch (InterruptedException e) {
            LOG.info("notification offer rejected : " + e);
        }

        SendingRendererRPCs sendingRenderer = new SendingRendererRPCs();
        sendingRenderer.serviceDelete();
        if (sendingRenderer.getSuccess()) {
            message = "Service deleted ! ";
            LOG.info(message);
            ServiceRpcResultSpBuilder tmp = new ServiceRpcResultSpBuilder()
                .setNotificationType(ServicePathNotificationTypes.ServiceDelete)
                .setServiceName(input.getServiceName())
                .setStatus(RpcStatusEx.Successful)
                .setStatusMessage(message);
            notification = tmp.build();
            try {
                notificationPublishService.putNotification(notification);
            } catch (InterruptedException e) {
                LOG.info("notification offer rejected : " + e);
            }
        } else {
            message = "Service not deleted !";
        }
        LOG.info(message);
        ConfigurationResponseCommonBuilder configurationResponseCommon = new ConfigurationResponseCommonBuilder()
            .setAckFinalIndicator("yes")
            .setRequestId(input.getServiceHandlerHeader().getRequestId())
            .setResponseCode("200")
            .setResponseMessage(message);
        ServiceDeleteOutput output = new ServiceDeleteOutputBuilder()
            .setConfigurationResponseCommon(configurationResponseCommon.build())
            .build();

        return RpcResultBuilder.success(output).buildFuture();

    }
}
