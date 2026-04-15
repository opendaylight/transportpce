/*
 * Copyright © 2026 Orange, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.tapisbi.listener;

import java.util.Set;

import org.opendaylight.mdsal.binding.api.NotificationPublishService;
import org.opendaylight.mdsal.binding.api.NotificationService.CompositeListener;
import org.opendaylight.yang.gen.v1.nbi.notifications.rev230728.PublishTapiNotificationService;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.notification.rev221121.NOTIFICATIONTYPEATTRIBUTEVALUECHANGE;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.notification.rev221121.Notification;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.TOPOLOGYOBJECTTYPELINK;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.TOPOLOGYOBJECTTYPENODE;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(service = TapiSbiTopologyNotificationHandler.class)
public class TapiSbiTopologyNotificationHandler {

    private static final Logger LOG = LoggerFactory.getLogger(TapiSbiTopologyNotificationHandler.class);
    private final NotificationPublishService notificationPublishService;

    @Activate
    public TapiSbiTopologyNotificationHandler(@Reference NotificationPublishService notificationPublishService) {
        this.notificationPublishService = notificationPublishService;
    }

    public CompositeListener getCompositeListener() {
        return new CompositeListener(Set.of(
            new CompositeListener.Component<>(Notification.class, this::onNotification)));
    }

    private void onNotification(Notification notification) {
        LOG.info("Received network model notification {}", notification);
        if (notification.getNotificationType().equals(NOTIFICATIONTYPEATTRIBUTEVALUECHANGE.VALUE)
                && (notification.getTargetObjectType().equals(TOPOLOGYOBJECTTYPELINK.VALUE)
                && notification.getTargetObjectType().equals(TOPOLOGYOBJECTTYPENODE.VALUE))) {
            if (notification.getChangedAttributes() == null) {
                return;
            }
            // TODO: DEVELOP THIS METHOD to listen to any notification associated with TAPI-SBI Topology
        }
    }

    private void sendNbiNotification(PublishTapiNotificationService network) {
        try {
            this.notificationPublishService.putNotification(network);
        } catch (InterruptedException e) {
            LOG.warn("Cannot send notification to nbi", e);
            Thread.currentThread().interrupt();
        }
    }
}
