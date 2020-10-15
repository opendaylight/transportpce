/*
 * Copyright © 2017 Orange, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.servicehandler.listeners;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.opendaylight.mdsal.binding.api.NotificationPublishService;
import org.opendaylight.transportpce.common.OperationResult;
import org.opendaylight.transportpce.pce.service.PathComputationService;
import org.opendaylight.transportpce.renderer.provisiondevice.RendererServiceOperations;
import org.opendaylight.transportpce.servicehandler.ModelMappingUtils;
import org.opendaylight.transportpce.servicehandler.ServiceInput;
import org.opendaylight.transportpce.servicehandler.service.PCEServiceWrapper;
import org.opendaylight.transportpce.servicehandler.service.ServiceDataStoreOperations;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev200128.PathComputationRequestOutput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev200128.PathComputationRequestOutputBuilder;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev200128.ServicePathRpcResult;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev200128.TransportpcePceListener;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev200128.service.path.rpc.result.PathDescription;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev200128.service.path.rpc.result.PathDescriptionBuilder;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.renderer.rev200520.ServiceImplementationRequestInput;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.service.types.rev200128.RpcStatusEx;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.service.types.rev200128.response.parameters.sp.ResponseParameters;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.service.types.rev200128.response.parameters.sp.ResponseParametersBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PceListenerImpl implements TransportpcePceListener {

    private static final Logger LOG = LoggerFactory.getLogger(PceListenerImpl.class);

    private ServicePathRpcResult servicePathRpcResult;
    private RendererServiceOperations rendererServiceOperations;
    private ServiceDataStoreOperations serviceDataStoreOperations;
    private PCEServiceWrapper pceServiceWrapper;
    private ServiceInput input;
    private Boolean serviceReconfigure;
    private Boolean tempService;
    private Boolean serviceFeasiblity;

    public PceListenerImpl(RendererServiceOperations rendererServiceOperations,
            PathComputationService pathComputationService, NotificationPublishService notificationPublishService,
            ServiceDataStoreOperations serviceDataStoreOperations) {
        this.rendererServiceOperations = rendererServiceOperations;
        this.pceServiceWrapper = new PCEServiceWrapper(pathComputationService, notificationPublishService);
        this.serviceDataStoreOperations = serviceDataStoreOperations;
        setServiceReconfigure(false);
        setInput(null);
        setTempService(false);
        setServiceFeasiblity(false);
    }

    @Override
    public void onServicePathRpcResult(ServicePathRpcResult notification) {
        if (!compareServicePathRpcResult(notification)) {
            servicePathRpcResult = notification;
            switch (servicePathRpcResult.getNotificationType().getIntValue()) {
                /* path-computation-request. */
                case 1:
                    onPathComputationResult(notification);
                    break;
                /* cancel-resource-reserve. */
                case 2:
                    onCancelResourceResult();
                    break;
                default:
                    break;
            }
        } else {
            LOG.warn("ServicePathRpcResult already wired !");
        }
    }

    /**
     * Process path computation request result.
     * @param notification the result notification.
     */
    private void onPathComputationResult(ServicePathRpcResult notification) {
        LOG.info("PCE '{}' Notification received : {}",servicePathRpcResult.getNotificationType().getName(),
                notification);
        if (servicePathRpcResult.getStatus() == RpcStatusEx.Failed) {
            LOG.error("PCE path computation failed !");
            return;
        } else if (servicePathRpcResult.getStatus() == RpcStatusEx.Pending) {
            LOG.warn("PCE path computation returned a Penging RpcStatusEx code!");
            return;
        } else if (servicePathRpcResult.getStatus() != RpcStatusEx.Successful) {
            LOG.error("PCE path computation returned an unknown RpcStatusEx code!");
            return;
        }

        LOG.info("PCE calculation done OK !");
        if (servicePathRpcResult.getPathDescription() == null) {
            LOG.error("'PathDescription' parameter is null ");
            return;
        }
        PathDescription pathDescription = new PathDescriptionBuilder()
                .setAToZDirection(servicePathRpcResult.getPathDescription().getAToZDirection())
                .setZToADirection(servicePathRpcResult.getPathDescription().getZToADirection())
                .build();
        LOG.info("PathDescription gets : {}", pathDescription);
        if (serviceFeasiblity) {
            LOG.warn("service-feasibility-check RPC ");
            return;
        }
        if (input == null) {
            LOG.error("Input is null !");
            return;
        }
        OperationResult operationResult = null;
        if (tempService) {
            operationResult = this.serviceDataStoreOperations.createTempService(input.getTempServiceCreateInput());
            if (!operationResult.isSuccess()) {
                LOG.error("Temp Service not created in datastore !");
            }
        } else {
            operationResult = this.serviceDataStoreOperations.createService(input.getServiceCreateInput());
            if (!operationResult.isSuccess()) {
                LOG.error("Service not created in datastore !");
            }
        }
        ResponseParameters responseParameters = new ResponseParametersBuilder()
                .setPathDescription(new org.opendaylight.yang.gen.v1.http.org
                        .transportpce.b.c._interface.service.types.rev200128
                        .response.parameters.sp.response.parameters
                        .PathDescriptionBuilder(pathDescription).build())
                .build();
        PathComputationRequestOutput pceResponse = new PathComputationRequestOutputBuilder()
                .setResponseParameters(responseParameters).build();
        OperationResult operationServicePathSaveResult = this.serviceDataStoreOperations
                .createServicePath(input, pceResponse);
        if (!operationServicePathSaveResult.isSuccess()) {
            LOG.error("Service Path not created in datastore !");
        }
        ServiceImplementationRequestInput serviceImplementationRequest = ModelMappingUtils
                .createServiceImplementationRequest(input, pathDescription);
        LOG.info("Sending serviceImplementation request : {}", serviceImplementationRequest);
        this.rendererServiceOperations.serviceImplementation(serviceImplementationRequest);
    }

    /**
     * Process cancel resource result.
     */
    private void onCancelResourceResult() {
        if (servicePathRpcResult.getStatus() == RpcStatusEx.Failed) {
            LOG.info("PCE cancel resource failed !");
            return;
        } else if (servicePathRpcResult.getStatus() == RpcStatusEx.Pending) {
            LOG.warn("PCE cancel returned a Penging RpcStatusEx code!");
            return;
        } else if (servicePathRpcResult.getStatus() != RpcStatusEx.Successful) {
            LOG.error("PCE cancel returned an unknown RpcStatusEx code!");
            return;
        }
        LOG.info("PCE cancel resource done OK !");
        OperationResult deleteServicePathOperationResult =
                this.serviceDataStoreOperations.deleteServicePath(input.getServiceName());
        if (!deleteServicePathOperationResult.isSuccess()) {
            LOG.warn("Service path was not removed from datastore!");
        }
        OperationResult deleteServiceOperationResult = null;
        if (tempService) {
            deleteServiceOperationResult =
                    this.serviceDataStoreOperations.deleteTempService(input.getServiceName());
            if (!deleteServiceOperationResult.isSuccess()) {
                LOG.warn("Temp Service was not removed from datastore!");
            }
        } else {
            deleteServiceOperationResult =
                    this.serviceDataStoreOperations.deleteService(input.getServiceName());
            if (!deleteServiceOperationResult.isSuccess()) {
                LOG.warn("Service was not removed from datastore!");
            }
        }
        /**
         * if it was an RPC serviceReconfigure, re-launch PCR.
         */
        if (this.serviceReconfigure) {
            LOG.info("cancel resource reserve done, relaunching PCE path computation ...");
            this.pceServiceWrapper.performPCE(input.getServiceCreateInput(), true);
            this.serviceReconfigure = false;
        }
    }

    @SuppressFBWarnings(
        value = "ES_COMPARING_STRINGS_WITH_EQ",
        justification = "false positives, not strings but real object references comparisons")
    private Boolean compareServicePathRpcResult(ServicePathRpcResult notification) {
        if (servicePathRpcResult == null) {
            return false;
        }
        if (servicePathRpcResult.getNotificationType() != notification.getNotificationType()) {
            return false;
        }
        if (servicePathRpcResult.getServiceName() != notification.getServiceName()) {
            return false;
        }
        if (servicePathRpcResult.getStatus() != notification.getStatus()) {
            return false;
        }
        if (servicePathRpcResult.getStatusMessage() != notification.getStatusMessage()) {
            return false;
        }
        return true;
    }

    public void setInput(ServiceInput serviceInput) {
        this.input = serviceInput;
    }

    public void setServiceReconfigure(Boolean serv) {
        this.serviceReconfigure = serv;
    }

    public void setserviceDataStoreOperations(ServiceDataStoreOperations serviceData) {
        this.serviceDataStoreOperations = serviceData;
    }

    public void setTempService(Boolean tempService) {
        this.tempService = tempService;
    }

    public void setServiceFeasiblity(Boolean serviceFeasiblity) {
        this.serviceFeasiblity = serviceFeasiblity;
    }

}
