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
import org.opendaylight.transportpce.servicehandler.ServiceInput;
import org.opendaylight.transportpce.servicehandler.service.PCEServiceWrapper;
import org.opendaylight.transportpce.servicehandler.service.ServiceDataStoreOperations;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.renderer.rev200520.ServiceRpcResultSp;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.renderer.rev200520.TransportpceRendererListener;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev190531.ServiceNotificationTypes;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.state.types.rev181130.State;
import org.opendaylight.yang.gen.v1.http.org.openroadm.equipment.states.types.rev181130.AdminStates;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.service.types.rev200128.RpcStatusEx;
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
                    onServiceImplementationResult(serviceName);
                    break;
                /* service-delete. */
                case 4 :
                    onServiceDeleteResult(serviceName);
                    break;
                default:
                    break;
            }
        } else {
            LOG.warn("ServiceRpcResultSp already wired !");
        }
    }

    /**
     * Process service delete result for serviceName.
     * @param serviceName String
     */
    private void onServiceDeleteResult(String serviceName) {
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
        }
    }

    /**
     * Process service implementation result for serviceName.
     * @param serviceName String
     * @param serviceName String
     */
    private void onServiceImplementationResult(String serviceName) {
        if (serviceRpcResultSp.getStatus() == RpcStatusEx.Successful) {
            onSuccededServiceImplementation();
        } else if (serviceRpcResultSp.getStatus() == RpcStatusEx.Failed) {
            onFailedServiceImplementation(serviceName);
        }
    }

    /**
     * Process succeeded service implementation for service.
     */
    private void onSuccededServiceImplementation() {
        LOG.info("Service implemented !");
        if (serviceDataStoreOperations != null) {
            OperationResult operationResult = null;
            if (tempService) {
                operationResult = this.serviceDataStoreOperations.modifyTempService(
                        serviceRpcResultSp.getServiceName(), State.InService, AdminStates.InService);
                if (!operationResult.isSuccess()) {
                    LOG.warn("Temp Service status not updated in datastore !");
                }
            } else {
                operationResult = this.serviceDataStoreOperations
                        .modifyService(serviceRpcResultSp.getServiceName(),
                                State.InService, AdminStates.InService);
                if (!operationResult.isSuccess()) {
                    LOG.warn("Service status not updated in datastore !");
                }
            }
        }
    }

    /**
     * Process failed service implementation for serviceName.
     * @param serviceName String
     */
    private void onFailedServiceImplementation(String serviceName) {
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

    @SuppressFBWarnings(
        value = "ES_COMPARING_STRINGS_WITH_EQ",
        justification = "false positives, not strings but real object references comparisons")
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
