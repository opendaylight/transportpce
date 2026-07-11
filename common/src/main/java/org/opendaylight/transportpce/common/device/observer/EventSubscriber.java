/*
 * Copyright © 2024 Smartoptics and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.common.device.observer;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.event.Level;

/**
 * Thread-safe implementation of {@link Subscriber}.
 *
 * <p>Messages are stored per {@link Level} in a {@link Collections#synchronizedSet(Set)}
 * wrapping a {@link LinkedHashSet}, preserving insertion order. Reads take a single
 * {@code toArray()} snapshot rather than iterating the set directly, since iteration is not
 * covered by the wrapper's per-call synchronization. Messages are only ever appended, never
 * removed.
 */
public class EventSubscriber implements Subscriber {

    private final Map<Level, Set<String>> messages = new ConcurrentHashMap<>();

    @Override
    public void event(Level level, String message) {
        if (message == null || message.isEmpty()) {
            return;
        }

        messages.computeIfAbsent(level, l -> Collections.synchronizedSet(new LinkedHashSet<>())).add(message);
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
        Set<String> strings = messages.get(level);

        if (strings == null || strings.isEmpty()) {
            return defaultMessage;
        }

        Object[] arr = strings.toArray();

        //Since objects are only added in the current contract, it's safe to assume there never will be a
        //concurrent modification by another thread removing an object from the set between the null/empty check and
        //the cast to array. Should that contract ever change, then this needs to be updated.
        return (String) arr[0];
    }

    @Override
    public String first(Level level, String defaultMessage, int count) {
        Set<String> strings = messages.get(level);

        if (strings == null || strings.isEmpty()) {
            return defaultMessage;
        }

        String[] arr = strings.toArray(String[]::new);

        //Since objects are only added in the current contract, it's safe to assume there never will be a
        //concurrent modification by another thread removing an object from the set between the null/empty check and
        //the cast to array. Should that contract ever change, then this needs to be updated.
        return String.join(", ", Arrays.copyOf(arr, Math.clamp(count, 1, arr.length)));
    }

    @Override
    public String last(Level level) {
        return last(level, "");
    }

    @Override
    public String last(Level level, String defaultMessage) {
        Set<String> strings = messages.get(level);

        if (strings == null || strings.isEmpty()) {
            return defaultMessage;
        }

        Object[] arr = strings.toArray();

        //Since objects are only added in the current contract, it's safe to assume there never will be a
        //concurrent modification by another thread removing an object from the set between the null/empty check and
        //the cast to array. Should that contract ever change, then this needs to be updated.
        return (String) arr[arr.length - 1];
    }

    @Override
    public String[] messages(Level level) {
        Set<String> strings = messages.get(level);
        if (strings == null) {
            return new String[0];
        }

        return strings.toArray(String[]::new);
    }
}
