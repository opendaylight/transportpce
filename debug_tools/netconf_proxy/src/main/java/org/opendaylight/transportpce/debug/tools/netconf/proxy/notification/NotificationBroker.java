/*
 * Copyright © 2026 1FINITY Inc.,  and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.debug.tools.netconf.proxy.notification;

import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Broker for distributing NETCONF notifications from the device to all connected client sessions.
 * Thread-safe for concurrent access from multiple sessions.
 */
public class NotificationBroker {

    private static final Logger LOG = LoggerFactory.getLogger(NotificationBroker.class);

    private final Set<NotificationListener> listeners = new CopyOnWriteArraySet<>();

    /**
     * Register a listener to receive notifications.
     *
     * @param listener the listener to register
     */
    public void registerListener(NotificationListener listener) {
        listeners.add(listener);
        LOG.debug("Registered notification listener, total listeners: {}", listeners.size());
    }

    /**
     * Unregister a listener.
     *
     * @param listener the listener to unregister
     */
    public void unregisterListener(NotificationListener listener) {
        listeners.remove(listener);
        LOG.debug("Unregistered notification listener, total listeners: {}", listeners.size());
    }

    /**
     * Broadcast a notification to all registered listeners.
     *
     * @param notificationXml the notification message as XML string
     */
    public void broadcastNotification(String notificationXml) {
        if (listeners.isEmpty()) {
            LOG.debug("Received notification but no listeners registered, discarding");
            return;
        }

        LOG.debug("Broadcasting notification to {} listeners", listeners.size());

        for (NotificationListener listener : listeners) {
            try {
                listener.onNotification(notificationXml);
            } catch (Exception e) {
                LOG.error("Error delivering notification to listener", e);
                // Continue delivering to other listeners
            }
        }
    }

    /**
     * Broadcast an error to all registered listeners.
     *
     * @param error the error that occurred
     */
    public void broadcastError(Exception error) {
        LOG.warn("Broadcasting error to {} listeners: {}", listeners.size(), error.getMessage());

        for (NotificationListener listener : listeners) {
            try {
                listener.onError(error);
            } catch (Exception e) {
                LOG.error("Error delivering error notification to listener", e);
            }
        }
    }

    /**
     * Get the number of registered listeners.
     *
     * @return the number of listeners
     */
    public int getListenerCount() {
        return listeners.size();
    }

    /**
     * Clear all listeners.
     */
    public void clearListeners() {
        LOG.info("Clearing all notification listeners");
        listeners.clear();
    }
}
