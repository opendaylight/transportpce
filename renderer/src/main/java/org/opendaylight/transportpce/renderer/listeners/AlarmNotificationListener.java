/*
 * Copyright © 2017 AT&T and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.renderer.listeners;

import org.opendaylight.yang.gen.v1.http.org.openroadm.alarm.rev161014.AlarmNotification;
import org.opendaylight.yang.gen.v1.http.org.openroadm.alarm.rev161014.OrgOpenroadmAlarmListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AlarmNotificationListener implements OrgOpenroadmAlarmListener {

    private static final Logger LOG = LoggerFactory.getLogger(AlarmNotificationListener.class);

    /**
     * Callback for alarm-notification.
     *
     * @param notification AlarmNotification object
     */
    @Override
    public void onAlarmNotification(AlarmNotification notification) {
        LOG.info("Notification {} received {}", AlarmNotification.QNAME, notification);
    }
}