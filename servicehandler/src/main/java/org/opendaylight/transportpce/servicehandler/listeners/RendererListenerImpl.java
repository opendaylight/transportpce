/*
 * Copyright © 2017 Orange, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.servicehandler.listeners;

import org.opendaylight.controller.md.sal.binding.api.NotificationPublishService;
import org.opendaylight.transportpce.common.OperationResult;
import org.opendaylight.transportpce.pce.service.PathComputationService;
import org.opendaylight.transportpce.servicehandler.ServiceInput;
import org.opendaylight.transportpce.servicehandler.service.PCEServiceWrapper;
import org.opendaylight.transportpce.servicehandler.service.ServiceDataStoreOperations;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.renderer.rev171017.ServiceRpcResultSp;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.renderer.rev171017.TransportpceRendererListener;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev161014.ServiceNotificationTypes;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.types.rev161014.State;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.service.types.rev171016.RpcStatusEx;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Calls to listen to Renderer notifications.
 *
 * @author Martial Coulibaly ( martial.coulibaly@gfi.com ) on behalf of Orange
 *
 */
public class RendererListenerImpl implements TransportpceRendererListener {

    private static final Logger LOG = LoggerFactory.getLogger(RendererListenerImpl.class);
    private ServiceRpcResultSp serviceRpcResultSp;
    private ServiceDataStoreOperations serviceDataStoreOperations;
    private ServiceInput input;
    private PCEServiceWrapper pceServiceWrapper;
    private Boolean tempService;

    public RendererListenerImpl(PathComputationService pathComputationService,
            NotificationPublishService notificationPublishService) {
        this.pceServiceWrapper = new PCEServiceWrapper(pathComputationService, notificationPublishService);
        setServiceInput(null);
        setTempService(false);
    }

    @Override
    public void onServiceRpcResultSp(ServiceRpcResultSp notification) {
        if (!compareServiceRpcResultSp(notification)) {
            serviceRpcResultSp = notification;
            String serviceName = serviceRpcResultSp.getServiceName();
            int notifType = serviceRpcResultSp.getNotificationType().getIntValue();
            LOG.info("Renderer '{}' Notification received : {}", serviceRpcResultSp.getNotificationType().getName(),
                    notification);
            switch (notifType) {
                /* service-implementation-request. */
                case 3 :
                    if (serviceRpcResultSp.getStatus() == RpcStatusEx.Successful) {
                        LOG.info("Service implemented !");
                        OperationResult operationResult = null;
                        if (tempService) {
                            operationResult = this.serviceDataStoreOperations.modifyTempService(
                                    serviceRpcResultSp.getServiceName(),
                                    State.InService, State.InService);
                            if (!operationResult.isSuccess()) {
                                LOG.warn("Temp Service status not updated in datastore !");
                            }
                        } else {
                            operationResult = this.serviceDataStoreOperations.modifyService(
                                    serviceRpcResultSp.getServiceName(),
                                    State.InService, State.InService);
                            if (!operationResult.isSuccess()) {
                                LOG.warn("Service status not updated in datastore !");
                            }
                        }
                    } else if (serviceRpcResultSp.getStatus() == RpcStatusEx.Failed) {
                        LOG.error("Renderer implementation failed !");
                        OperationResult deleteServicePathOperationResult =
                                this.serviceDataStoreOperations.deleteServicePath(serviceName);
                        if (!deleteServicePathOperationResult.isSuccess()) {
                            LOG.warn("Service path was not removed from datastore!");
                        }
                        if (tempService) {
                            OperationResult deleteServiceOperationResult =
                                    this.serviceDataStoreOperations.deleteTempService(serviceName);
                            if (!deleteServiceOperationResult.isSuccess()) {
                                LOG.warn("Temp Service was not removed from datastore!");
                            }
                        } else {
                            OperationResult deleteServiceOperationResult =
                                    this.serviceDataStoreOperations.deleteService(serviceName);
                            if (!deleteServiceOperationResult.isSuccess()) {
                                LOG.warn("Service was not removed from datastore!");
                            }
                        }
                    }
                    break;
                /* service-delete. */
                case 4 :
                    if (serviceRpcResultSp.getStatus() == RpcStatusEx.Successful) {
                        LOG.info("Service '{}' deleted !", serviceName);
                        if (this.input != null) {
                            LOG.info("sending PCE cancel resource reserve for '{}'",  this.input.getServiceName());
                            this.pceServiceWrapper.cancelPCEResource(this.input.getServiceName(),
                                    ServiceNotificationTypes.ServiceDeleteResult);
                        } else {
                            LOG.error("ServiceInput parameter is null !");
                        }
                    } else if (serviceRpcResultSp.getStatus() == RpcStatusEx.Failed) {
                        LOG.error("Renderer service delete failed !");
                        return;
                    }
                    break;
                default:
                    break;
            }
        } else {
            LOG.warn("ServiceRpcResultSp already wired !");
        }
    }

    private Boolean compareServiceRpcResultSp(ServiceRpcResultSp notification) {
        Boolean result = true;
        if (serviceRpcResultSp == null) {
            result = false;
        } else {
            if (serviceRpcResultSp.getNotificationType() != notification.getNotificationType()) {
                result = false;
            }
            if (serviceRpcResultSp.getServiceName() != notification.getServiceName()) {
                result = false;
            }
            if (serviceRpcResultSp.getStatus() != notification.getStatus()) {
                result = false;
            }
            if (serviceRpcResultSp.getStatusMessage() != notification.getStatusMessage()) {
                result = false;
            }
        }
        return result;
    }

    public void setServiceInput(ServiceInput serviceInput) {
        this.input = serviceInput;
    }

    public void setserviceDataStoreOperations(ServiceDataStoreOperations serviceData) {
        this.serviceDataStoreOperations = serviceData;
    }

    public void setTempService(Boolean tempService) {
        this.tempService = tempService;
    }
}
