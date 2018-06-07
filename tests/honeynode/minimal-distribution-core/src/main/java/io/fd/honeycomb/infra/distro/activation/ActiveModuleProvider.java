/*
 * Copyright (c) 2017 Cisco and/or its affiliates.
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

package io.fd.honeycomb.infra.distro.activation;

import com.google.inject.Inject;
import com.google.inject.Module;
import com.google.inject.Provider;
import io.fd.honeycomb.infra.distro.schema.ResourceLoader;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Provides list of active modules for distribution
 */
public class ActiveModuleProvider implements Provider<ActiveModules>, ResourceLoader {

    private static final Logger LOG = LoggerFactory.getLogger(ActiveModuleProvider.class);

    @Inject
    private ActivationConfig config;

    @Override
    public ActiveModules get() {
        return new ActiveModules(loadActiveModules(
                aggregateResources(config.getModulesResourcePath())));
    }

    /**
     * Provide unique set of active modules filtered from provided resources
     */
    static Set<Class<? extends Module>> loadActiveModules(@Nonnull final List<String> moduleNames) {
        final ClassLoader classLoader = ActiveModuleProvider.class.getClassLoader();
        LOG.info("Reading active modules configuration for distribution");

        // process resources to resource modules
        return moduleNames.stream()
                .map(String::trim)
                .filter(trimmedLine -> trimmedLine.length() != 0)
                // filter out commented lines
                .filter(nonEmptyLine -> !nonEmptyLine.startsWith("//"))
                // filter duplicates
                .distinct()
                .map(validLine -> moduleNameToClass(validLine, classLoader))
                // filters out classes that are not modules
                .filter(ActiveModuleProvider::filterNonModules)
                .collect(Collectors.toSet());
    }

    /**
     * Aggregate all resources from provided relative path into a {@code List<String>}
     */
    public List<String> aggregateResources(final String relativePath) {
        // must use universal approach of loading from folder/jar
        // because of memory footprint benchmark
        return new ArrayList<>(loadResourceContentsOnPath(relativePath));
    }

    private static boolean filterNonModules(final Class<?> clazz) {
        final boolean isModule = Module.class.isAssignableFrom(clazz);
        if (!isModule) {
            LOG.warn("Class {} is provided in modules configuration, but is not a Module and will be ignored", clazz);
        }
        return isModule;
    }

    /**
     * Loads class by provided name
     */
    private static Class<? extends Module> moduleNameToClass(final String name,
                                                             final ClassLoader classLoader) {
        try {
            LOG.debug("Loading module class {}", name);
            return (Class<? extends Module>) classLoader.loadClass(name);
        } catch (ClassNotFoundException e) {
            LOG.error("Unable to convert {} to class, make sure you've provided sources to classpath", name);
            throw new IllegalStateException(
                    "Unable to convert " + name + " to class, make sure you've provided sources to classpath", e);
        }
    }
}
