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

package io.fd.honeycomb.infra.distro.schema;


import com.google.inject.Inject;
import com.google.inject.Provider;
import io.fd.honeycomb.infra.distro.activation.ActivationConfig;
import io.fd.honeycomb.infra.distro.activation.ActiveModules;
import org.opendaylight.yangtools.yang.binding.YangModelBindingProvider;

import javax.annotation.Nonnull;
import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

import static java.lang.String.format;

/**
 * Loads active yang modules
 * Relying on generate yang-module-index
 */
public class YangModulesProvider implements Provider<YangModulesProvider.YangModules> {

    @Inject
    private ActiveModules activeModules;

    @Inject
    private ActivationConfig config;

    @Override
    public YangModules get() {
        // no need to bind this, pretty big map and its needed just here
        final YangModuleMappingIndex index = new YangModuleMappingIndex(config.getYangModulesIndexPath());

        return new YangModules(activeModules.getActiveModulesClasses().stream()
                .map(Class::getName)
                .map(index::getByModuleName)
                .flatMap(Collection::stream)
                .map(YangModulesProvider::loadClass)
                .map(aClass -> (Class<? extends YangModelBindingProvider>) aClass)
                .collect(Collectors.toSet()));
    }

    static class YangModules {
        private final Set<Class<? extends YangModelBindingProvider>> yangBindings;

        YangModules(final Set<Class<? extends YangModelBindingProvider>> yangBindings) {
            this.yangBindings = yangBindings;
        }

        Set<YangModelBindingProvider> getYangBindings() {
            return yangBindings.stream()
                    .map(providerClass -> {
                        try {
                            return providerClass.newInstance();
                        } catch (InstantiationException | IllegalAccessException e) {
                            throw new IllegalStateException(format("Unable to create instance of %s", providerClass),
                                    e);
                        }
                    }).collect(Collectors.toSet());
        }
    }

    private static Class<?> loadClass(@Nonnull final String className) {
        try {
            return Class.forName(className);
        } catch (ClassNotFoundException e) {
            throw new IllegalArgumentException("Unable to load class: " + className, e);
        }
    }
}
