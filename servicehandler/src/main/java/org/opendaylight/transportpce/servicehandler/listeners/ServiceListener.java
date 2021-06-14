/*
 * Copyright © 2021 Orange and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.servicehandler.listeners;

import java.util.Collection;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.binding.api.DataObjectModification;
import org.opendaylight.mdsal.binding.api.DataTreeChangeListener;
import org.opendaylight.mdsal.binding.api.DataTreeModification;
import org.opendaylight.mdsal.binding.api.NotificationPublishService;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.state.types.rev181130.State;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev190531.service.list.Services;
import org.opendaylight.yang.gen.v1.nbi.notifications.rev210628.PublishNotificationAlarmService;
import org.opendaylight.yang.gen.v1.nbi.notifications.rev210628.PublishNotificationAlarmServiceBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ServiceListener implements DataTreeChangeListener<Services> {

    private static final Logger LOG = LoggerFactory.getLogger(ServiceListener.class);
    private static final String TOPIC = "ServiceListener";
    private final DataBroker dataBroker;
    private NotificationPublishService notificationPublishService;

    public ServiceListener(final DataBroker dataBroker, NotificationPublishService notificationPublishService) {
        this.dataBroker = dataBroker;
        this.notificationPublishService = notificationPublishService;
    }

    public void onDataTreeChanged(Collection<DataTreeModification<Services>> changes) {
        LOG.info("onDataTreeChanged - {}", this.getClass().getSimpleName());
        for (DataTreeModification<Services> change : changes) {
            DataObjectModification<Services> rootService = change.getRootNode();
            if (rootService.getDataBefore() == null) {
                continue;
            }
            String serviceName = rootService.getDataBefore().key().getServiceName();
            switch (rootService.getModificationType()) {
                case DELETE:
                    LOG.info("Service {} correctly deleted from controller", serviceName);
                    break;
                case WRITE:
                    Services input = rootService.getDataAfter();
                    if (rootService.getDataBefore().getOperationalState() == State.InService
                            && rootService.getDataAfter().getOperationalState() == State.OutOfService) {
                        LOG.info("Service {} is becoming outOfService", serviceName);
                        sendNbiNotification(new PublishNotificationAlarmServiceBuilder()
                                .setServiceName(input.getServiceName())
                                .setConnectionType(input.getConnectionType())
                                .setMessage("The service is now outOfService")
                                .setOperationalState(State.OutOfService)
                                .setTopic(TOPIC)
                                .build());
                    }
                    else if (rootService.getDataBefore().getOperationalState() == State.OutOfService
                            && rootService.getDataAfter().getOperationalState() == State.InService) {
                        LOG.info("Service {} is becoming InService", serviceName);
                        sendNbiNotification(new PublishNotificationAlarmServiceBuilder()
                                .setServiceName(input.getServiceName())
                                .setConnectionType(input.getConnectionType())
                                .setMessage("The service is now inService")
                                .setOperationalState(State.InService)
                                .setTopic(TOPIC)
                                .build());
                    }
                    break;
                default:
                    LOG.debug("Unknown modification type {}", rootService.getModificationType().name());
                    break;
            }
        }
    }

    /**
     * Send notification to NBI notification in order to publish message.
     *
     * @param service PublishNotificationAlarmService
     */
    private void sendNbiNotification(PublishNotificationAlarmService service) {
        try {
            notificationPublishService.putNotification(service);
        } catch (InterruptedException e) {
            LOG.warn("Cannot send notification to nbi", e);
            Thread.currentThread().interrupt();
        }
    }
}
