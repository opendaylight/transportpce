/*
 * Copyright © 2025 Smartoptics and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.common.config;

import java.util.concurrent.TimeUnit;
import org.opendaylight.transportpce.common.time.Time;
import org.opendaylight.transportpce.common.time.Timeout;

public class CommonConfig implements Config {

    private final Time deviceRead;

    private final Time deviceWrite;

    public CommonConfig(long timeoutSeconds) {
        this(timeoutSeconds, timeoutSeconds);
    }

    public CommonConfig(long readTimeoutSeconds, long writeTimeOutSeconds) {
        deviceRead = new Timeout(readTimeoutSeconds, TimeUnit.SECONDS);
        deviceWrite = new Timeout(writeTimeOutSeconds, TimeUnit.SECONDS);
    }

    @Override
    public Time deviceReadTimeout() {
        return deviceRead;
    }

    @Override
    public Time deviceWriteTimeout() {
        return deviceWrite;
    }

}
