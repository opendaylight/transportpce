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
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.event.Level;

public class EventSubscriber implements Subscriber {

    private final Map<Level, Set<String>> messages = new ConcurrentHashMap<>();

    @Override
    public void event(Level level, String message) {
        if (message == null || message.isEmpty()) {
            return;
        }

        if (!messages.containsKey(level)) {
            messages.put(level, new LinkedHashSet<>());
        }

        messages.get(level).add(message);
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
        if (!messages.containsKey(level)) {
            return defaultMessage;
        }

        return messages.get(level).iterator().next();
    }

    @Override
    public String last(Level level) {
        return last(level, "");
    }

    @Override
    public String last(Level level, String defaultMessage) {
        if (!messages.containsKey(level)) {
            return defaultMessage;
        }

        Set<String> strings = messages.get(level);

        return strings.stream().skip(strings.size() - 1).findFirst().orElse(defaultMessage);
    }

    @Override
    public String[] messages(Level level) {

        if (!messages.containsKey(level)) {
            return new String[0];
        }

        Object[] arr = messages.get(level).toArray();

        return Arrays.copyOf(arr, arr.length, String[].class);

    }
}
