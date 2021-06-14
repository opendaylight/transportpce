/*
 * Copyright © 2021 Orange and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.networkmodel;

import java.util.Collection;
import javax.annotation.Nonnull;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.binding.api.DataObjectModification;
import org.opendaylight.mdsal.binding.api.DataTreeChangeListener;
import org.opendaylight.mdsal.binding.api.DataTreeModification;
import org.opendaylight.mdsal.binding.api.NotificationPublishService;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.state.types.rev181130.State;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev190531.service.list.Services;
import org.opendaylight.yang.gen.v1.nbi.notifications.rev201130.PublishNotificationService;
import org.opendaylight.yang.gen.v1.nbi.notifications.rev201130.PublishNotificationServiceBuilder;
import org.opendaylight.yang.gen.v1.nbi.notifications.rev201130.notification.service.ServiceAEndBuilder;
import org.opendaylight.yang.gen.v1.nbi.notifications.rev201130.notification.service.ServiceZEndBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NetConfServiceListener implements DataTreeChangeListener<Services> {

    private static final Logger LOG = LoggerFactory.getLogger(NetConfServiceListener.class);
    private static final String TOPIC = "NetConfServiceListener";
    private final DataBroker dataBroker;
    private NotificationPublishService notificationPublishService;

    public NetConfServiceListener(final DataBroker dataBroker, NotificationPublishService notificationPublishService) {
        this.dataBroker = dataBroker;
        this.notificationPublishService = notificationPublishService;
    }

    public void onDataTreeChanged(@Nonnull Collection<DataTreeModification<Services>> changes) {
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
                        LOG.info("Service {} is becoming Out of Service", serviceName);
                        sendNbiNotification(new PublishNotificationServiceBuilder()
                                .setServiceName(input.getServiceName())
                                .setServiceAEnd(new ServiceAEndBuilder(input.getServiceAEnd()).build())
                                .setServiceZEnd(new ServiceZEndBuilder(input.getServiceZEnd()).build())
                                .setCommonId(input.getCommonId())
                                .setConnectionType(input.getConnectionType())
                                .setResponseFailed("")
                                .setMessage("The service is now Out of Service")
                                .setOperationalState(State.OutOfService)
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
     * @param service PublishNotificationService
     */
    private void sendNbiNotification(PublishNotificationService service) {
        try {
            notificationPublishService.putNotification(service);
        } catch (InterruptedException e) {
            LOG.warn("Cannot send notification to nbi", e);
            Thread.currentThread().interrupt();
        }
    }
}
