/*
 * Copyright © 2016 AT&T and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.test;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.Maps;
import java.io.InputStream;
import java.util.Collections;
import java.util.Map;
import javax.annotation.Nonnull;
import org.opendaylight.yangtools.yang.common.QName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Base class for device tests, should be used, when device databroker is needed
 * in tests.
 *
 */
public abstract class AbstractDeviceTest {

    private static final Logger LOG = LoggerFactory.getLogger(AbstractDeviceTest.class);
    private final Map<String, DeviceWrapper> internalStorage;

    /**
     * Default constructor only initializes the inner
     * {@link AbstractDeviceTest#internalStorage} as asynchronized {@link Map}.
     */
    public AbstractDeviceTest() {
        this.internalStorage = Collections.synchronizedMap(Maps.newHashMap());
    }

    /**
     * Insert a created device into {@link AbstractDeviceTest#internalStorage}.
     *
     * @see DeviceWrapper#createDeviceWrapper(String, InputStream, QName)
     * @param key identifier of device simulator (wrapper)
     * @param initialDataXmlInputStream {@link InputStream} of xml with initial simulator data
     * @param intialDataQName {@link QName} of initial simulator data
     * @return device simulator (wrapper)
     */
    public DeviceWrapper createDeviceWrapper(@Nonnull String key, @Nonnull InputStream initialDataXmlInputStream,
            @Nonnull QName intialDataQName) {
        DeviceWrapper deviceWrapper =
                DeviceWrapper.createDeviceWrapper(key, initialDataXmlInputStream, intialDataQName);
        LOG.info("Creating a new device wrapper {}, {}", key, deviceWrapper);
        internalStorage.put(key, deviceWrapper);
        return deviceWrapper;
    }

    /**
     * Returns the {@link DeviceWrapper} identified by the key provided as param.
     *
     * @param deviceIdentifier identifier of device simulator
     * @return stored device or null if not found
     */
    public DeviceWrapper getDevice(@Nonnull String deviceIdentifier) {
        Preconditions.checkArgument(!Strings.isNullOrEmpty(deviceIdentifier));
        return internalStorage.get(deviceIdentifier);
    }
}
