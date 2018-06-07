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

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.LinkedListMultimap;
import com.google.common.collect.Multimap;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Stream;
import javax.annotation.Nonnull;

/**
 * Index from guice module to yang module providers
 */
class YangModuleMappingIndex implements ResourceLoader {

    private static final String G_MODULE_TOKEN = "GUICE_MODULE:";
    private static final String KEY_VALUE_SEPARATOR = "|";
    private static final String Y_MODULE_TOKEN = "YANG_MODULES:";
    private static final String Y_MODULE_SEPARATOR = ",";

    /**
     * key - module class name
     * value  - yang module provider
     */
    private final Multimap<String, String> index;

    YangModuleMappingIndex(final String indexPath) {
        this.index = LinkedListMultimap.create();
        loadResourceContentsOnPath(indexPath)
                .forEach(line -> {
                    final String moduleName = parseModuleName(line);
                    parseYangModules(line).forEach(yModuleProvider -> index.put(moduleName, yModuleProvider));
                });
    }

    Set<String> getByModuleName(@Nonnull final String moduleName) {
        return ImmutableSet.copyOf(index.get(moduleName));
    }

    int applicationModulesCount() {
        return index.keySet().size();
    }

    private static String parseModuleName(final String rawLine) {
        return rawLine.substring(rawLine.indexOf(G_MODULE_TOKEN) + G_MODULE_TOKEN.length(),
                rawLine.indexOf(KEY_VALUE_SEPARATOR));
    }

    private static Stream<String> parseYangModules(final String rawLine) {
        return Arrays.stream(rawLine.substring(rawLine.indexOf(Y_MODULE_TOKEN) + Y_MODULE_TOKEN.length())
                .split(Y_MODULE_SEPARATOR));
    }
}
