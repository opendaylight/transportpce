/*
 * Copyright © 2017 AT&T and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.renderer.listeners;

import org.opendaylight.yang.gen.v1.http.org.openroadm.de.operations.rev161014.OrgOpenroadmDeOperationsListener;
import org.opendaylight.yang.gen.v1.http.org.openroadm.de.operations.rev161014.RestartNotification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DeOperationsListener implements OrgOpenroadmDeOperationsListener {

    private static final Logger LOG = LoggerFactory.getLogger(DeOperationsListener.class);

    /**
     * Callback for restart-notification.
     *
     * @param notification RestartNotification object
     */
    @Override
    public void onRestartNotification(RestartNotification notification) {
        LOG.info("Notification {} received {}", RestartNotification.QNAME, notification);
    }
}