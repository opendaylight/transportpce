/*
 * Copyright © 2017 Orange, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.common;

import java.util.concurrent.TimeUnit;
import org.opendaylight.transportpce.common.config.Config;
import org.opendaylight.transportpce.common.time.Timeout;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(configurationPid = "org.opendaylight.transportpce.common")
public final class Timeouts implements Config {

    @ObjectClassDefinition
    public @interface Configuration {
        @AttributeDefinition (min = "1")
        long devicereadtimeout() default 240;

        @AttributeDefinition
        TimeUnit devicereadtimeunit() default TimeUnit.SECONDS;

        @AttributeDefinition (min = "1")
        long devicewritetimeout() default 240;

        @AttributeDefinition
        TimeUnit devicewritetimeunit() default TimeUnit.SECONDS;
    }

    private static final Logger LOG = LoggerFactory.getLogger(Timeouts.class);

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

    private final Timeout deviceReadTimeout;

    private final Timeout deviceWriteTimeout;

    @Activate
    public Timeouts(final Configuration configuration) {
        Timeout tmpDeviceReadTimeout = new Timeout(
                configuration.devicereadtimeout(), configuration.devicereadtimeunit());
        Timeout tmpDeviceWriteTimeout = new Timeout(
                configuration.devicewritetimeout(), configuration.devicewritetimeunit());
        Timeout minimumTimeout = new Timeout(1, TimeUnit.SECONDS);

        if (tmpDeviceReadTimeout.compareTo(minimumTimeout) < 0) {
            LOG.warn("Device read timeout is too low, setting to minimum value of 1 second");
            tmpDeviceReadTimeout = minimumTimeout;
        }

        if (tmpDeviceWriteTimeout.compareTo(minimumTimeout) < 0) {
            LOG.warn("Device write timeout is too low, setting to minimum value of 1 second");
            tmpDeviceWriteTimeout = minimumTimeout;
        }

        this.deviceReadTimeout = tmpDeviceReadTimeout;
        this.deviceWriteTimeout = tmpDeviceWriteTimeout;

        LOG.info("Device read timeout {}", this.deviceReadTimeout);
        LOG.info("Device write timeout {}", this.deviceWriteTimeout);
    }

    @Deactivate
    public void close() {
        LOG.info("Closing component Timeouts config");
    }

    @Override
    public Timeout deviceReadTimeout() {
        return deviceReadTimeout;
    }

    @Override
    public Timeout deviceWriteTimeout() {
        return deviceWriteTimeout;
    }
}
