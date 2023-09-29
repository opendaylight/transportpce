/*
 * Copyright © 2021 AT&T and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.networkmodel.listeners;

import java.util.Set;
import org.opendaylight.mdsal.binding.api.NotificationService;
import org.opendaylight.yang.gen.v1.http.org.openroadm.de.operations.rev200529.RestartNotification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DeOperationsListener710 {

    private static final Logger LOG = LoggerFactory.getLogger(DeOperationsListener710.class);
    private final NotificationService.CompositeListener compositeListener;

    public DeOperationsListener710() {
        compositeListener = new NotificationService.CompositeListener(Set.of(
            new NotificationService.CompositeListener
                .Component<>(RestartNotification.class, this::onRestartNotification)
        ));
    }

    public NotificationService.CompositeListener getCompositeListener() {
        return compositeListener;
    }

    /**
     * Callback for restart-notification.
     *
     * @param notification RestartNotification object
     */
    private void onRestartNotification(RestartNotification notification) {
        LOG.info("Notification {} received {}", RestartNotification.QNAME, notification);
    }

}