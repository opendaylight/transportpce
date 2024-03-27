/*
 * Copyright © 2024 Smartoptics and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.common.device.observer;

import org.slf4j.event.Level;

/**
 * A class wishing to 'subscribe' to, i.e. be notified about, 'events'
 * as they happen should implement this interface.
 *
 * <p>A client may wish to interrogate this subscriber for the first/last
 * event received with a particular 'level'. As an example a client
 * may wish to know what the first or last received error message.
 */
public interface Subscriber {

    /**
     * Send a message to this subscriber.
     */
    void event(Level level, String message);

    /**
     * Send an error message to this subscriber.
     *
     * <p>The same as calling {@link #event(Level, String)} Level.ERROR.
     * @param message error message
     */
    void error(String message);

    /**
     * Send a warning message to this subscriber.
     *
     * <p>The same as calling {@link #event(Level, String)} Level.WARN.
     * @param message warning message
     */
    void warn(String message);

    /**
     * The first message with level caught by this subscriber.
     */
    String first(Level level);

    /**
     * The first message with level caught by this subscriber.
     *
     * <p>Return defaultMessage if no message with the specified level is found.
     */
    String first(Level level, String defaultMessage);

    /**
     * The last message with level caught by this subscriber.
     */
    String last(Level level);

    /**
     * The last message with level caught by this subscriber.
     *
     * <p>Return defaultMessage if no message with the specified level is found.
     */
    String last(Level level, String defaultMessage);

    /**
     * All messages with level caught by subscriber
     * in the order they were caught.
     */
    String[] messages(Level level);

}
