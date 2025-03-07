/*
 * Copyright © 2025 Smartoptics and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.common.time;

import java.util.concurrent.TimeUnit;

public interface Time {

    /**
     * Time with the unit from {@link #unit()}. The value is positive.
     */
    long time();

    /**
     * UNIT of the time.
     */
    TimeUnit unit();

}
