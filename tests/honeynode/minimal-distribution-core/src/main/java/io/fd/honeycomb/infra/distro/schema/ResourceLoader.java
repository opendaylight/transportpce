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

package io.fd.honeycomb.infra.distro.schema;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static java.lang.String.format;

import com.google.common.base.Charsets;
import com.google.common.base.Strings;
import com.google.common.io.Resources;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Collectors;
import org.apache.commons.io.IOUtils;

/**
 * Allows loading content of X amount of files from filesystem or archive
 */
public interface ResourceLoader {

    default Set<String> loadResourceContentsOnPath(final String path) {
        final URL folderUrl = getClass().getClassLoader().getResource(path);
        checkState(folderUrl != null, "Resources %s not found", path);

        if (ResourceLoaderIml.urlToUri(folderUrl).getScheme().equals("jar")) {
            return ResourceLoaderIml.readFromJar(path, folderUrl);
        } else {
            return ResourceLoaderIml.readFromFolder(folderUrl);
        }

    }

    final class ResourceLoaderIml {

        private static Set<String> readFromFolder(final URL folderUrl) {
            final File folder = new File(folderUrl.getPath());
            final File[] files = checkNotNull(folder.listFiles(), "No files present on path %s", folderUrl);
            return Arrays.stream(files)
                    .map(ResourceLoaderIml::fileToUrl)
                    .map(ResourceLoaderIml::urlToContentString)
                    .flatMap(content -> Arrays.stream(content.split(System.lineSeparator())))
                    .filter(ResourceLoaderIml::filterNonEmpty)
                    .collect(Collectors.toSet());
        }

        private static Set<String> readFromJar(final String path, final URL url) {
            final String uriString = urlToUri(url).toString();
            final String fileReference = extractJarFilePath(uriString);
            try (JarFile jar = new JarFile(new File(fileReference))) {
                return Collections.list(jar.entries())
                        .stream()
                        .filter(jarEntry -> jarEntry.getName().contains(path))
                        .map(jarEntry -> getJarEntryStream(jar, jarEntry))
                        .map(ResourceLoaderIml::readJarEntryStream)
                        .flatMap(content -> Arrays.stream(content.split(System.lineSeparator())))
                        .filter(ResourceLoaderIml::filterNonEmpty)
                        .collect(Collectors.toSet());
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }
        }

        private static String extractJarFilePath(final String uriString) {
            return uriString.substring(0, uriString.indexOf("!")).replace("jar:file:", "");
        }

        private static boolean filterNonEmpty(final String line) {
            return !Strings.isNullOrEmpty(line.trim());
        }

        private static String readJarEntryStream(final InputStream inputStream) {
            try {
                final String value = IOUtils.toString(inputStream, StandardCharsets.UTF_8);
                IOUtils.closeQuietly(inputStream);
                return value;
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }
        }

        private static InputStream getJarEntryStream(final JarFile jar, final JarEntry jarEntry) {
            try {
                return jar.getInputStream(jarEntry);
            } catch (IOException e) {
                throw new IllegalStateException(format("Unable to get stream for entry %s | jar %s", jar, jarEntry), e);
            }
        }

        private static URI urlToUri(final URL url) {
            try {
                return url.toURI();
            } catch (URISyntaxException e) {
                throw new IllegalStateException(format("Unable to convert URL %s to URI", url), e);
            }
        }

        private static String urlToContentString(final URL url) {
            try {
                return Resources.toString(url, Charsets.UTF_8);
            } catch (IOException e) {
                throw new IllegalArgumentException("Unable to read resource from: " + url, e);
            }
        }

        private static URL fileToUrl(final File file) {
            try {
                return file.toURI().toURL();
            } catch (MalformedURLException e) {
                throw new IllegalStateException(e);
            }
        }
    }
}
