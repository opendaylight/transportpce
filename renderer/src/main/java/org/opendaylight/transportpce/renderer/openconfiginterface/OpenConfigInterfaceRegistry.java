/*
 * Copyright © 2024 NTT and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.renderer.openconfiginterface;

import java.util.concurrent.CopyOnWriteArrayList;
import org.opendaylight.transportpce.common.openconfiginterfaces.OpenConfigInterfaceConfiguration;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * OpenConfigInterfaceRegistry - OSGI registry for discovering OpenConfigInterfaceConfiguration services.
 * Manages version-specific implementations (OC2.0.0, OC5.6.0) registered in separate bundles.
 * Uses dynamic service discovery with MULTIPLE cardinality and DYNAMIC policy to support
 * hot-plugging of version implementations.
 */
@Component(service = OpenConfigInterfaceRegistry.class, immediate = true)
public class OpenConfigInterfaceRegistry {

    private static final Logger LOG = LoggerFactory.getLogger(OpenConfigInterfaceRegistry.class);

    private final CopyOnWriteArrayList<OpenConfigInterfaceConfiguration> interfaceServices =
            new CopyOnWriteArrayList<>();

    @Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC)
    public void bindOpenConfigInterfaceService(OpenConfigInterfaceConfiguration service) {
        String version = service.getOpenConfigVersion();
        LOG.info("Binding OpenConfigInterfaceConfiguration service for OpenConfig version {}", version);
        interfaceServices.add(service);
    }

    public void unbindOpenConfigInterfaceService(OpenConfigInterfaceConfiguration service) {
        String version = service.getOpenConfigVersion();
        LOG.info("Unbinding OpenConfigInterfaceConfiguration service for OpenConfig version {}", version);
        interfaceServices.remove(service);
    }

    /**
     * Get OpenConfigInterfaceConfiguration service for specified OpenConfig version.
     *
     * @param version OpenConfig version (e.g., "2.0.0", "5.6.0")
     *
     * @return OpenConfigInterfaceConfiguration implementation for the version, or null if not found
     */
    public OpenConfigInterfaceConfiguration getService(String version) {
        for (OpenConfigInterfaceConfiguration service : interfaceServices) {
            if (service.getOpenConfigVersion().equals(version)) {
                LOG.debug("Found OpenConfigInterfaceConfiguration service for version {}", version);
                return service;
            }
        }
        LOG.warn("No OpenConfigInterfaceConfiguration service found for version {}", version);
        return null;
    }

    /**
     * Get all registered OpenConfigInterfaceConfiguration services.
     *
     * @return list of all registered services
     */
    public CopyOnWriteArrayList<OpenConfigInterfaceConfiguration> getAllServices() {
        return new CopyOnWriteArrayList<>(interfaceServices);
    }
}
