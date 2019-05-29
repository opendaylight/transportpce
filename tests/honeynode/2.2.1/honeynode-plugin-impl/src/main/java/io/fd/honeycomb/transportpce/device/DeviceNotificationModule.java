/*
 * Copyright (c) 2018 Orange and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at:
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.fd.honeycomb.transportpce.device;

import com.google.inject.AbstractModule;
import com.google.inject.multibindings.Multibinder;

import io.fd.honeycomb.notification.ManagedNotificationProducer;
import io.fd.honeycomb.transportpce.device.notifications.DeviceNotificationProducer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Martial COULIBALY ( mcoulibaly.ext@orange.com ) on behalf of Orange
 */
public class DeviceNotificationModule extends AbstractModule {

    private static final Logger LOG = LoggerFactory.getLogger(DeviceNotificationModule.class);

    @Override
    protected void configure() {
        LOG.info("Initializing Device Notification Module");
        final Multibinder<ManagedNotificationProducer> notifFactoryBinder = Multibinder.newSetBinder(binder(),
                ManagedNotificationProducer.class);
        notifFactoryBinder.addBinding().to(DeviceNotificationProducer.class);
    }

}
