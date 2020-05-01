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
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public final class Main {

    private static final Logger LOG = LoggerFactory.getLogger(Main.class);

    private Main() {
    }

    public static void main(String[] args) {
        String sid = UUID.randomUUID().toString();
        if (!Main.isAlreadyRunning()) {
            Main.init(new ActivationModule());
        }
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

    @SuppressWarnings("resource")
    public static boolean isAlreadyRunning() {
        File file;
        FileChannel fileChannel;
        File userDir = new File(System.getProperty("user.home"));
        file = new File(userDir, myLockName());
        LOG.info("File = {}", file.getAbsolutePath());
        if (!file.exists()) {
            try {
                file.createNewFile();
                file.deleteOnExit();
            } catch (IOException e) {
                throw new RuntimeException("Unable to create Single Instance lock file!", e);
            }
        }

        try {
            fileChannel = new RandomAccessFile(file, "rw").getChannel();
        } catch (FileNotFoundException e) {
            throw new RuntimeException("Single Instance lock file vanished!", e);
        }
        try {
            if (fileChannel.tryLock() != null) {
                return false;
            }
        } catch (Exception e) {
        }
        try {
            fileChannel.close();
        } catch (IOException e1) {
        }
        return true;
    }

    private static String myLockName() {
        String firstPart = null;
        String secondPart = null;
        boolean foundFirstPart = false;
        boolean foundSecondPart = false;
        Path classPath = Paths.get(Main.class.getProtectionDomain().getCodeSource().getLocation().getPath());
        Path configPath = classPath.getParent().getParent().getParent().getParent().getParent().getParent().getParent().getParent();
        File configFolder = new File(configPath.toString());
        File[] listOfFiles = configFolder.listFiles();
        for (File file:listOfFiles) {
            if (file.getName().equals("config")) {
                File config = new File(configPath.toString() + "/" + file.getName());
                File[] listOfFilesinConfig = config.listFiles();
                for (File f:listOfFilesinConfig) {
                    if (f.getName().equals("device")) {
                        File device = new File(config.toString() + "/" + f.getName());
                        File[] listOfFilesinDevice = device.listFiles();
                        for (File lastF:listOfFilesinDevice) {
                            if (lastF.getName().contains(".xml")) {
                                firstPart = lastF.getName();
                                foundFirstPart = true;
                                break;
                            }
                        }
                    }
                    if (f.getName().equals("netconf.json")) {
                        //JSON parser object to parse read file
                        JSONParser jsonParser = new JSONParser();
                        try (FileReader reader = new FileReader(f))
                        {
                            //Read JSON file
                            Object obj = jsonParser.parse(reader);
                            JSONObject content = (JSONObject) obj;
                            secondPart = content.get("netconf-ssh-binding-port").toString();
                            foundSecondPart = true;

                        } catch (FileNotFoundException e) {
                            e.printStackTrace();
                        } catch (IOException e) {
                            e.printStackTrace();
                        } catch (ParseException e) {
                            e.printStackTrace();
                        }
                    }
                    if (foundSecondPart && foundFirstPart) {
                        break;
                    }
                }
            }
            if (foundSecondPart && foundFirstPart) {
                break;
            }
        }
        return "." + firstPart + "-" + secondPart;
    }
}
