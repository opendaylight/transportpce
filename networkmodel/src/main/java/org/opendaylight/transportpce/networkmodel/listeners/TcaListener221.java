/*
 * Copyright © 2017 AT&T and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.networkmodel.listeners;

import java.util.Set;
import org.opendaylight.mdsal.binding.api.NotificationService;
import org.opendaylight.yang.gen.v1.http.org.openroadm.tca.rev181019.TcaNotification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TcaListener221 {

    private static final Logger LOG = LoggerFactory.getLogger(TcaListener221.class);
    private final NotificationService.CompositeListener compositeListener;

    public TcaListener221() {
        compositeListener = new NotificationService.CompositeListener(Set.of(
            new NotificationService.CompositeListener.Component<>(TcaNotification.class, this::onTcaNotification)
        ));
    }

    public NotificationService.CompositeListener getCompositeListener() {
        return compositeListener;
    }

    /**
     * Callback for tca-notification.
     * @param notification TcaNotification object
     */
    private void onTcaNotification(TcaNotification notification) {
        LOG.info("Notification {} received {}", TcaNotification.QNAME, notification);
    }

}