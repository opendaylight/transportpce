/*
 * Copyright © 2017 AT&T and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.networkmodel.listeners;

import java.util.Set;
import org.opendaylight.mdsal.binding.api.NotificationService.CompositeListener;
import org.opendaylight.yang.gen.v1.http.org.openroadm.tca.rev161014.TcaNotification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of the org.opendaylight.yang.gen.v1.http.org.openroadm.tca.rev161014.TcaNotification
 * notification.
 * This implementation is dedicated to yang model 1.2.1 revision.
 */
public class TcaListener {

    private static final Logger LOG = LoggerFactory.getLogger(TcaListener.class);

    /**
     * Get instances of a CompositeListener that could be used to unregister listeners.
     * @return a Composite listener containing listener implementations that will receive notifications
     */
    public CompositeListener getCompositeListener() {
        return new CompositeListener(Set.of(
            new CompositeListener.Component<>(TcaNotification.class, this::onTcaNotification)));
    }

    /**
     * Callback for tca-notification.
     * @param notification TcaNotification object
     */
    private void onTcaNotification(TcaNotification notification) {
        LOG.info("Notification {} received {}", TcaNotification.QNAME, notification);
    }

}