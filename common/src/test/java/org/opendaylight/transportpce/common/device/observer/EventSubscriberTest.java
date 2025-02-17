/*
 * Copyright © 2024 Smartoptics and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.common.device.observer;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.slf4j.event.Level;

class EventSubscriberTest {

    @Test
    void last() {
        Subscriber subscriber = new EventSubscriber();
        subscriber.event(Level.ERROR, "First");
        subscriber.event(Level.ERROR, "Last");

        assertEquals("Last", subscriber.last(Level.ERROR));
    }

    @Test
    void empty() {
        Subscriber subscriber = new EventSubscriber();

        assertEquals("", subscriber.last(Level.ERROR));
    }

    @Test
    void lastDefault() {
        Subscriber subscriber = new EventSubscriber();

        assertEquals("Error", subscriber.last(Level.ERROR, "Error"));
    }

    @Test
    void firstNumber() {
        Subscriber subscriber = new EventSubscriber();
        subscriber.event(Level.ERROR, "First error");
        subscriber.event(Level.WARN, "First warn");
        subscriber.event(Level.ERROR, "Second error");
        subscriber.event(Level.ERROR, "Third error");


        assertEquals("First error, Second error, Third error", subscriber.first(Level.ERROR, "", 3));
    }

    @Test
    void firstNumberContainsLessThanThree() {
        Subscriber subscriber = new EventSubscriber();
        subscriber.event(Level.ERROR, "First error");
        subscriber.event(Level.WARN, "First warn");
        subscriber.event(Level.ERROR, "Second error");


        assertEquals("First error, Second error", subscriber.first(Level.ERROR, "", 3));
    }

    @Test
    void firstNumberEmpty() {
        Subscriber subscriber = new EventSubscriber();

        assertEquals("Asdf", subscriber.first(Level.ERROR, "Asdf", 3));
    }

    @Test
    void repetitiveMessages() {
        Subscriber subscriber = new EventSubscriber();
        subscriber.event(Level.ERROR, "First error");
        subscriber.event(Level.ERROR, "Second error");
        subscriber.event(Level.ERROR, "First error");

        assertEquals("First error, Second error", subscriber.first(Level.ERROR, "Error", 3));
    }
}
