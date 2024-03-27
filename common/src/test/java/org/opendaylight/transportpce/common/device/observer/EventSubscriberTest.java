/*
 * Copyright © 2024 Smartoptics and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.common.device.observer;

import org.junit.Assert;
import org.junit.jupiter.api.Test;
import org.slf4j.event.Level;

class EventSubscriberTest {

    @Test
    void event() {
        Subscriber subscriber = new EventSubscriber();
        subscriber.event(Level.ERROR, "");
        subscriber.event(Level.ERROR, "Last");

        Assert.assertEquals("Last", subscriber.last(Level.ERROR));
    }

    @Test
    void error() {
    }

    @Test
    void warn() {
    }

    @Test
    void first() {
    }

    @Test
    void testFirst() {
    }

    @Test
    void last() {
        Subscriber subscriber = new EventSubscriber();
        subscriber.event(Level.ERROR, "First");
        subscriber.event(Level.ERROR, "Last");

        Assert.assertEquals("Last", subscriber.last(Level.ERROR));
    }

    @Test
    void testLast() {
    }

    @Test
    void messages() {
    }
}