/*
 * Copyright © 2017 AT&T, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.pce.service;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.NotificationPublishService;
import org.opendaylight.transportpce.pce.PceComplianceCheck;
import org.opendaylight.transportpce.pce.PceComplianceCheckResult;
import org.opendaylight.transportpce.pce.PceSendingPceRPCs;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev170426.CancelResourceReserveInput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev170426.CancelResourceReserveOutput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev170426.CancelResourceReserveOutputBuilder;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev170426.PathComputationRequestInput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev170426.PathComputationRequestOutput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev170426.PathComputationRequestOutputBuilder;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev170426.ServicePathRpcResult;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev170426.ServicePathRpcResultBuilder;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev170426.service.path.rpc.result.PathDescription;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev161014.configuration.response.common.ConfigurationResponseCommonBuilder;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev170426.path.description.AToZDirection;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev170426.path.description.ZToADirection;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.service.types.rev170426.RpcStatusEx;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.service.types.rev170426.ServicePathNotificationTypes;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.service.types.rev170426.response.parameters.sp.ResponseParametersBuilder;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.service.types.rev170426.response.parameters.sp.response.parameters.PathDescriptionBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PathComputationServiceImpl implements PathComputationService {

    private static final Logger LOG = LoggerFactory.getLogger(PathComputationServiceImpl.class);

    private final NotificationPublishService notificationPublishService;
    private final DataBroker dataBroker;

    public PathComputationServiceImpl(DataBroker dataBroker,
                                      NotificationPublishService notificationPublishService) {
        this.notificationPublishService = notificationPublishService;
        this.dataBroker = dataBroker;
    }

    public void init() {
        LOG.info("init ...");
    }

    public void close() {
        LOG.info("close.");
    }

    @Override
    public CancelResourceReserveOutput cancelResourceReserve(CancelResourceReserveInput input) {
        LOG.info("cancelResourceReserve");
        String message = "";

        ServicePathRpcResult notification = new ServicePathRpcResultBuilder()
                .setNotificationType(ServicePathNotificationTypes.CancelResourceReserve)
                .setServiceName(input.getServiceName())
                .setStatus(RpcStatusEx.Pending)
                .setStatusMessage("Service compliant, submitting cancelResourceReserve Request ...")
                .build();
        try {
            notificationPublishService.putNotification(notification);
        } catch (InterruptedException e) {
            LOG.info("notification offer rejected : ", e.getMessage());
        }

        PceSendingPceRPCs sendingPCE = new PceSendingPceRPCs();
        sendingPCE.cancelResourceReserve();
        if (sendingPCE.getSuccess()) {
            message = "ResourceReserve cancelled !";
        } else {
            message = "Cancelling ResourceReserve failed !";
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
        output.setConfigurationResponseCommon(configurationResponseCommon.build());
        return output.build();
    }

    @Override
    public PathComputationRequestOutput pathComputationRequest(PathComputationRequestInput input) {
        LOG.info("pathComputationRequest");

        PathComputationRequestOutputBuilder output = new PathComputationRequestOutputBuilder();
        ConfigurationResponseCommonBuilder configurationResponseCommon = new ConfigurationResponseCommonBuilder();

        PceComplianceCheckResult check = PceComplianceCheck.check(input);
        if (!check.hasPassed()) {
            configurationResponseCommon
                    .setAckFinalIndicator("Yes")
                    .setRequestId(input.getServiceHandlerHeader().getRequestId())
                    .setResponseCode("Path not calculated")
                    .setResponseMessage(check.getMessage());

            output.setConfigurationResponseCommon(configurationResponseCommon.build())
                    .setResponseParameters(null);

            return output.build();
        }
        ServicePathRpcResult notification = new ServicePathRpcResultBuilder()
                .setNotificationType(ServicePathNotificationTypes.PathComputationRequest)
                .setServiceName(input.getServiceName())
                .setStatus(RpcStatusEx.Pending)
                .setStatusMessage("Service compliant, submitting pathComputation Request ...")
                .build();
        try {
            notificationPublishService.putNotification(notification);
        } catch (InterruptedException e) {
            LOG.info("notification offer rejected : ", e.getMessage());
        }

        String message = "";
        String responseCode = "";
        PceSendingPceRPCs sendingPCE = new PceSendingPceRPCs(input, dataBroker);
        sendingPCE.pathComputation();
        message = sendingPCE.getMessage();
        responseCode = sendingPCE.getResponseCode();
        PathDescriptionBuilder path = null;
        path = sendingPCE.getPathDescription();

        LOG.info("PCE response: {} {}", message, responseCode);
        if ((sendingPCE.getSuccess() == false) || (path == null)) {
            configurationResponseCommon
                    .setAckFinalIndicator("Yes")
                    .setRequestId(input.getServiceHandlerHeader().getRequestId())
                    .setResponseCode(responseCode)
                    .setResponseMessage(message);

            output.setConfigurationResponseCommon(configurationResponseCommon.build());
            return output.build();
        }

        // Path calculator returned Success
        configurationResponseCommon
                .setAckFinalIndicator("Yes")
                .setRequestId(input.getServiceHandlerHeader().getRequestId())
                .setResponseCode(responseCode)
                .setResponseMessage(message);

        ServicePathRpcResultBuilder tmp = new ServicePathRpcResultBuilder()
                .setNotificationType(ServicePathNotificationTypes.PathComputationRequest)
                .setServiceName(input.getServiceName())
                .setStatus(RpcStatusEx.Successful)
                .setStatusMessage(message);
        PathDescription pathDescription = new org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce
                .pce.rev170426.service.path.rpc.result.PathDescriptionBuilder()
                .setAToZDirection(path.getAToZDirection())
                .setZToADirection(path.getZToADirection())
                .build();
        tmp.setPathDescription(pathDescription);

        notification = tmp.build();
        try {
            notificationPublishService.putNotification(notification);
        } catch (InterruptedException e) {
            LOG.error("notification offer rejected : {}", e.getMessage());
        }

        org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.service.types
                .rev170426.response.parameters.sp.response.parameters.PathDescription pathDescription1
                = new org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.service.types
                .rev170426.response.parameters.sp.response.parameters.PathDescriptionBuilder()
                .setAToZDirection(path.getAToZDirection())
                .setZToADirection(path.getZToADirection())
                .build();
        ResponseParametersBuilder rpb  = new ResponseParametersBuilder()
                .setPathDescription(pathDescription1);

        output.setConfigurationResponseCommon(configurationResponseCommon.build())
              .setResponseParameters(rpb.build());

        //debug prints
        AToZDirection atoz = pathDescription.getAToZDirection();
        if (atoz != null && atoz.getAToZ() != null) {
            LOG.info("Impl AtoZ Notification: [{}] elements in description", atoz.getAToZ().size());
            for (int i = 0; i < atoz.getAToZ().size(); i++) {
                LOG.info("Impl AtoZ Notification: [{}] {}", i, atoz.getAToZ().get(i));
            }
        }
        ZToADirection ztoa = pathDescription.getZToADirection();
        if (ztoa != null && ztoa.getZToA() != null) {
            LOG.info("Impl ZtoA Notification: [{}] elements in description", ztoa.getZToA().size());
            for (int i = 0; i < ztoa.getZToA().size(); i++) {
                LOG.info("Impl ZtoA Notification: [{}] {}", i, ztoa.getZToA().get(i));
            }
        }
        //debug prints
        return output.build();
    }

}
