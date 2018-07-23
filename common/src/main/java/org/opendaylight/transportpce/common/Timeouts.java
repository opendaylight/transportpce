/*
 * Copyright © 2017 Orange, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.common;

import java.util.concurrent.TimeUnit;

public final class Timeouts {
    public static final long DATASTORE_READ = 1000;
    public static final long DATASTORE_WRITE = 1000;
    public static final long DATASTORE_DELETE = 1000;

    // TODO remove '* 2' when renderer and olm is running in parallel
    public static final long RENDERING_TIMEOUT = 240000 * 2;
    public static final long OLM_TIMEOUT = 240000 * 2;

    public static final long SERVICE_ACTIVATION_TEST_RETRY_TIME = 20000;

    /**
     * Device read timeout in seconds.
     */
    public static final long DEVICE_READ_TIMEOUT = 240;
    public static final TimeUnit DEVICE_READ_TIMEOUT_UNIT = TimeUnit.SECONDS;

    public static final long DEVICE_WRITE_TIMEOUT = 240;
    public static final TimeUnit DEVICE_WRITE_TIMEOUT_UNIT = TimeUnit.SECONDS;

    //TODO add timeouts for device setup (olm power setup etc.)

    private Timeouts() {
    }
}
