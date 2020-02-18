/*
 * Copyright (c) 2016 Cisco and/or its affiliates.
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

package io.fd.honeycomb.infra.distro;

import static com.google.inject.Guice.createInjector;

import com.google.common.collect.ImmutableSet;
import com.google.inject.ConfigurationException;
import com.google.inject.CreationException;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.Module;
import com.google.inject.ProvisionException;
import com.google.inject.name.Names;
import io.fd.honeycomb.data.init.DataTreeInitializer;
import io.fd.honeycomb.data.init.InitializerRegistry;
import io.fd.honeycomb.infra.distro.activation.ActivationModule;
import io.fd.honeycomb.infra.distro.activation.ActiveModules;
import io.fd.honeycomb.infra.distro.initializer.InitializerPipelineModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public final class Main {

    private static final Logger LOG = LoggerFactory.getLogger(Main.class);

    private Main() {
    }

    public static void main(String[] args) {
        init(new ActivationModule());
    }

    /**
     * Initialize the Honeycomb with provided modules
     */
    public static Injector init(final ActivationModule activationModule) {
        try {
            LOG.info("Starting honeycomb");
            // creating child injector does not work in this case, so just create injector, and does not store ref
            // to it, or its active modules instance
            Injector injector = createInjector(ImmutableSet.<Module>builder()
                    .add(activationModule)
                    .addAll(createInjector(activationModule).getInstance(ActiveModules.class).createModuleInstances())
                    .build());

            // Log all bindings
            injector.getAllBindings().entrySet().stream()
                    .forEach(e -> LOG.trace("Component available under: {} is {}", e.getKey(), e.getValue()));

            try {
                LOG.info("Initializing configuration");
                injector.getInstance(Key.get(InitializerRegistry.class,
                        Names.named(InitializerPipelineModule.HONEYCOMB_INITIALIZER))).initialize();
                LOG.info("Configuration initialized successfully");
            } catch (DataTreeInitializer.InitializeException e) {
                LOG.error("Unable to initialize configuration", e);
            }

            LOG.info("Honeycomb started successfully!");

            return injector;
        } catch (CreationException | ProvisionException | ConfigurationException e) {
            LOG.error("Failed to initialize Honeycomb components", e);
            throw e;
        } catch (RuntimeException e) {
            LOG.error("Unexpected initialization failure", e);
            throw e;
        } finally {
            // Trigger gc to force collect initial garbage + dedicated classloader
            System.gc();
        }
    }

}
