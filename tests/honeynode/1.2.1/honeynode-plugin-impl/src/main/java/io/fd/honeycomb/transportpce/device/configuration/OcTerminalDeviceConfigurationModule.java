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
package io.fd.honeycomb.transportpce.device.configuration;

import com.google.inject.AbstractModule;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.jmob.guice.conf.core.ConfigurationModule;

/**
 * @authors Gilles THOUENON and Christophe BETOULE ( gilles.thouenon@orange.com, christophe.betoule@orange.com )
 */
public final class OcTerminalDeviceConfigurationModule extends AbstractModule {

    private static final Logger LOG = LoggerFactory.getLogger(OcTerminalDeviceConfigurationModule.class);

    @Override
    protected void configure() {
        LOG.info("Initializing openconfig-terminal-device Readers Module");
        install(ConfigurationModule.create());
        requestInjection(OcTerminalDeviceConfiguration.class);
    }

}
