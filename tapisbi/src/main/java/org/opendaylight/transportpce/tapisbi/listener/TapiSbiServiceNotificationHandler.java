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
import org.opendaylight.transportpce.tapi.TapiConstants;
import org.opendaylight.yang.gen.v1.nbi.notifications.rev230728.PublishTapiNotificationService;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.Uuid;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev221121.CONNECTIVITYOBJECTTYPECONNECTIVITYSERVICE;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.notification.rev221121.NOTIFICATIONTYPEATTRIBUTEVALUECHANGE;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.notification.rev221121.Notification;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(service = TapiSbiServiceNotificationHandler.class)
public class TapiSbiServiceNotificationHandler {

    private static final Logger LOG = LoggerFactory.getLogger(TapiSbiServiceNotificationHandler.class);
    private final NotificationPublishService notificationPublishService;
    private final Uuid tapiTopoUuid = new Uuid(TapiConstants.SBI_TAPI_TOPOLOGY_UUID);

    private final Uuid serviceNotificationUuid = new Uuid(TapiConstants.SBISERVICENOTIFICATIONUUID);

    @Activate
    public TapiSbiServiceNotificationHandler(@Reference NotificationPublishService notificationPublishService) {
        this.notificationPublishService = notificationPublishService;
        LOG.debug("TapiSbiServiceNotificationHandler instantiated with TopoUuid {}", tapiTopoUuid);
    }

    public CompositeListener getCompositeListener() {
        return new CompositeListener(Set.of(
            new CompositeListener.Component<>(Notification.class, this::onNotification)));
    }

    private void onNotification(Notification notification) {
        LOG.info("Received network model notification {}", notification);
        if (notification.getNotificationType().equals(NOTIFICATIONTYPEATTRIBUTEVALUECHANGE.VALUE)
                && notification.getTargetObjectType().equals(CONNECTIVITYOBJECTTYPECONNECTIVITYSERVICE.VALUE)
                && notification.getUuid().equals(serviceNotificationUuid)) {
            if (notification.getChangedAttributes() == null) {
                return;
            }
            // TODO: DEVELOP THIS METHOD to listen to any notification associated with TAPI-SBI Connectivity service
            // provided that their Uuid has been recorded in a specific list of Services To Be Monitored, and that
            // have been created by TransportPCE through TAPI-SBI

        }
    }

    private void sendNbiNotification(PublishTapiNotificationService service) {
        try {
            this.notificationPublishService.putNotification(service);
        } catch (InterruptedException e) {
            LOG.warn("Cannot send notification to nbi", e);
            Thread.currentThread().interrupt();
        }
    }
}
