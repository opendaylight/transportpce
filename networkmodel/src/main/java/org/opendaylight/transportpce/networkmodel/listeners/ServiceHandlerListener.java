/*
 * Copyright © 2020 Orange, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.networkmodel.listeners;

import org.opendaylight.transportpce.networkmodel.service.FrequenciesService;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.servicehandler.rev201125.ServiceRpcResultSh;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.servicehandler.rev201125.TransportpceServicehandlerListener;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev190531.ServiceNotificationTypes;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.service.types.rev200128.RpcStatusEx;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ServiceHandlerListener implements TransportpceServicehandlerListener {
    private static final Logger LOG = LoggerFactory.getLogger(ServiceHandlerListener.class);
    private final FrequenciesService service;

    public ServiceHandlerListener(FrequenciesService service) {
        LOG.info("Init service handler listener for network");
        this.service = service;
    }

    @Override
    public void onServiceRpcResultSh(ServiceRpcResultSh notification) {
        if (notification.getStatus() != RpcStatusEx.Successful) {
            LOG.info("RpcStatusEx of notification not equals successful. Nothing to do for notification {}",
                    notification);
            return;
        }
        ServiceNotificationTypes notificationType = notification.getNotificationType();
        if (notificationType == null) {
            LOG.warn("No information about the type of the notification for {}", notification);
            return;
        }
        switch (notificationType) {
            case ServiceCreateResult:
            case ServiceReconfigureResult:
            case ServiceRestorationResult:
                LOG.info("Service creation or reconfiguration or restoration notification received {}", notification);
                onServiceCreation(notification);
                break;
            case ServiceDeleteResult:
                LOG.info("Service delete notification received {}", notification);
                onServiceDeletion(notification);
                break;
            default:
                LOG.warn("This type of notification is not managed at this time {} for notification {}",
                        notificationType, notification);
                break;

        }
    }

    /**
     * Allocate frequencies in topology.
     * @param notification ServiceRpcResultSh
     */
    private void onServiceCreation(ServiceRpcResultSh notification) {
        if (notification.getAToZDirection() != null || notification.getZToADirection() != null) {
            LOG.info("Update topology with used frequency by service {}", notification.getServiceName());
            service.allocateFrequencies(notification.getAToZDirection(), notification.getZToADirection());
        }
    }

    /**
     * Release frequencies in topology.
     * @param notification ServiceRpcResultSh
     */
    private void onServiceDeletion(ServiceRpcResultSh notification) {
        if (notification.getAToZDirection() != null || notification.getZToADirection() != null) {
            LOG.info("Update topology with no more used frequency by deleted service {}",
                    notification.getServiceName());
            service.releaseFrequencies(notification.getAToZDirection(), notification.getZToADirection());
        }
    }

}
