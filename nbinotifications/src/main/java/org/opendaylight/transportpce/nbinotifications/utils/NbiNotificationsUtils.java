/*
 * Copyright © 2020 Orange, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.nbinotifications.utils;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class NbiNotificationsUtils {

    private static final Logger LOG = LoggerFactory.getLogger(NbiNotificationsUtils.class);

    private NbiNotificationsUtils() {
    }

    public static Properties loadProperties(String propertyFileName) {
        Properties props = new Properties();
        InputStream inputStream = NbiNotificationsUtils.class.getClassLoader().getResourceAsStream(propertyFileName);
        try {
            if (inputStream != null) {
                props.load(inputStream);
            } else {
                LOG.warn("Kafka property file '{}' is empty", propertyFileName);
            }
        } catch (IOException e) {
            LOG.warn("Kafka property file '{}' was not found in the classpath", propertyFileName);
        }
        return props;
    }
}
