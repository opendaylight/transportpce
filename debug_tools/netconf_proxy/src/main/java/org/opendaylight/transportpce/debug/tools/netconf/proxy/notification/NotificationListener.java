/*
 * Copyright © 2026 1FINITY Inc.,  and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.debug.tools.netconf.proxy.notification;

/**
 * Interface for receiving NETCONF notifications.
 */
public interface NotificationListener {

    /**
     * Called when a NETCONF notification is received from the device.
     *
     * @param notificationXml the notification message as XML string
     */
    void onNotification(String notificationXml);

    /**
     * Called when there's an error receiving notifications.
     *
     * @param error the error that occurred
     */
    void onError(Exception error);
}
