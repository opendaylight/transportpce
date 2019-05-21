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
package io.fd.honeycomb.transportpce.device.test;

import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_DELETE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Martial COULIBALY ( mcoulibaly.ext@orange.com ) on behalf of Orange
 */
public class DirectoryWatchTest {
    private static final Logger LOG = LoggerFactory.getLogger(DirectoryWatchTest.class);
    private static final String DATA_JSON = "/var/lib/honeycomb/persist/config/data.json";

    public static void main(String[] args) {
        File data = new File(DATA_JSON);
        String parent = data.getParent();
        LOG.info("data.json directory : {}",parent);
        try {
            WatchService watcher = FileSystems.getDefault().newWatchService();
            Path dir = Paths.get(data.getParent());
            dir.register(watcher, ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY);
            LOG.info("Watch Service registered for dir: {}", dir.getFileName());
            while (true) {
                WatchKey key;
                try {
                    key = watcher.take();
                } catch (InterruptedException ex) {
                    return;
                }
                for (WatchEvent<?> event : key.pollEvents()) {
                    WatchEvent.Kind<?> kind = event.kind();
                    @SuppressWarnings("unchecked")
                    WatchEvent<Path> ev = (WatchEvent<Path>) event;
                    Path fileName = ev.context();
                    if (kind == ENTRY_MODIFY && fileName.toString().equals("data.json")) {
                        LOG.info("My source file has changed ...");
                        /**
                         * merge honeycomb-config datastore to  device oper
                         */
                    }
                }
                boolean valid = key.reset();
                if (!valid) {
                    LOG.error("Key has been unregisterede");
                    break;
                }
            }
        } catch (IOException ex) {
            LOG.error("WatchService Error",ex);
        }
    }

}
