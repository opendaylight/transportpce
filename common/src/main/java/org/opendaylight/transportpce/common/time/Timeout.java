/*
 * Copyright © 2025 Smartoptics and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.common.time;

import java.util.concurrent.TimeUnit;
import org.eclipse.jdt.annotation.NonNull;

/**
 * Timeout with a time and a unit.
 *
 * @param time Time with the unit from {@link #unit()}. The value is positive.
 * @param unit Unit of the time.
 */
public record Timeout(long time, TimeUnit unit) implements Comparable<Timeout> {

    @Override
    public int compareTo(@NonNull Timeout timeout) {
        if (timeout.unit().toMillis(timeout.time()) > this.unit.toMillis(this.time)) {
            return -1;
        } else if (timeout.unit().toMillis(timeout.time()) < this.unit.toMillis(this.time)) {
            return 1;
        }

        return 0;
    }
}
