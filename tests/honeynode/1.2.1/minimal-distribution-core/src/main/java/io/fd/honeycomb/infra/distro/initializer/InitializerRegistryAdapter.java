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

package io.fd.honeycomb.infra.distro.initializer;

import io.fd.honeycomb.data.init.DataTreeInitializer;
import io.fd.honeycomb.data.init.InitializerRegistry;
import io.fd.honeycomb.translate.MappingContext;
import io.fd.honeycomb.translate.ModificationCache;
import io.fd.honeycomb.translate.read.ReadContext;
import io.fd.honeycomb.translate.read.registry.InitRegistry;
import javax.annotation.Nonnull;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class InitializerRegistryAdapter implements InitializerRegistry {

    private static final Logger LOG = LoggerFactory.getLogger(InitializerRegistryAdapter.class);

    private final DataTreeInitializer configInitializer;
    private final DataTreeInitializer contextInitializer;
    private final InitRegistry initRegistry;
    private final DataBroker dataBroker;
    private final MappingContext realtimeMappingContext;

    InitializerRegistryAdapter(final DataTreeInitializer configInitializer,
                               final DataTreeInitializer contextInitializer,
                               final InitRegistry initRegistry,
                               final DataBroker noopConfigDataBroker,
                               final MappingContext realtimeMappingContext) {
        this.configInitializer = configInitializer;
        this.contextInitializer = contextInitializer;
        this.initRegistry = initRegistry;
        this.dataBroker = noopConfigDataBroker;
        this.realtimeMappingContext = realtimeMappingContext;
    }

    @Override
    public void initialize() throws DataTreeInitializer.InitializeException {
        LOG.info("Config initialization started");

        try {
            // Initialize contexts first so that other initializers can find any relevant mapping before initializing
            // configuration to what is already in VPP
            contextInitializer.initialize();
            LOG.info("Persisted context restored successfully");
            // Initialize all registered initializers
            initRegistry.initAll(dataBroker, new InitReadContext(realtimeMappingContext));
            LOG.info("Configuration initialized successfully");
            // Initialize stored configuration on top
            configInitializer.initialize();
            LOG.info("Persisted configuration restored successfully");
        } catch (Exception e) {
            LOG.warn("Failed to initialize config", e);
        }

        LOG.info("Honeycomb initialized");
    }

    private static final class InitReadContext implements ReadContext {

        private final ModificationCache modificationCache;
        private final MappingContext realtimeMappingContext;

        InitReadContext(final MappingContext realtimeMappingContext) {
            modificationCache = new ModificationCache();
            this.realtimeMappingContext = realtimeMappingContext;
        }

        @Nonnull
        @Override
        public ModificationCache getModificationCache() {
            return modificationCache;
        }

        @Nonnull
        @Override
        public MappingContext getMappingContext() {
            return realtimeMappingContext;
        }

        @Override
        public void close() {
            modificationCache.close();
        }
    }
}
