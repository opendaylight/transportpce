/*
 * Copyright © 2024 Smartoptics and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.common.device.observer;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.SequencedSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.event.Level;

/**
 * Thread-safe implementation of {@link Subscriber}.
 *
 * <p>Messages are stored per {@link Level} in a {@link LinkedHashSet}, preserving insertion order.
 * Messages are only ever appended, never removed.
 *
 * <p>Each {@link Level} has its own {@link SequencedSet} instance, and every read or write of that
 * set must hold its monitor ({@code synchronized (strings)}) for the duration of the access,
 * including check-then-act sequences such as {@code isEmpty()} followed by {@code getFirst()}.
 * Any new accessor must follow the same pattern or it will reintroduce the races this class was
 * fixed to avoid.
 */
public class EventSubscriber implements Subscriber {

    private final Map<Level, SequencedSet<String>> messages = new ConcurrentHashMap<>();

    @Override
    public void event(Level level, String message) {
        if (message == null || message.isEmpty()) {
            return;
        }

        SequencedSet<String> strings = messages.computeIfAbsent(level, l -> new LinkedHashSet<>());

        synchronized (strings) {
            strings.add(message);
        }
    }

    @Override
    public void error(String message) {
        event(Level.ERROR, message);
    }

    @Override
    public void warn(String message) {
        event(Level.WARN, message);
    }

    @Override
    public String first(Level level) {
        return first(level, "");
    }

    @Override
    public String first(Level level, String defaultMessage) {
        SequencedSet<String> strings = messages.get(level);

        if (strings == null) {
            return defaultMessage;
        }

        synchronized (strings) {
            if (strings.isEmpty()) {
                return defaultMessage;
            }

            return strings.getFirst();
        }
    }

    @Override
    public String first(Level level, String defaultMessage, int count) {
        SequencedSet<String> strings = messages.get(level);

        if (strings == null) {
            return defaultMessage;
        }

        synchronized (strings) {
            if (strings.isEmpty()) {
                return defaultMessage;
            }

            String[] arr = strings.toArray(String[]::new);

            return String.join(", ", Arrays.copyOf(arr, Math.clamp(count, 1, arr.length)));
        }
    }

    @Override
    public String last(Level level) {
        return last(level, "");
    }

    @Override
    public String last(Level level, String defaultMessage) {
        SequencedSet<String> strings = messages.get(level);

        if (strings == null) {
            return defaultMessage;
        }

        synchronized (strings) {
            if (strings.isEmpty()) {
                return defaultMessage;
            }

            return strings.getLast();
        }
    }

    @Override
    public String[] messages(Level level) {
        Set<String> strings = messages.get(level);
        if (strings == null) {
            return new String[0];
        }

        synchronized (strings) {
            return strings.toArray(String[]::new);
        }
    }
}
