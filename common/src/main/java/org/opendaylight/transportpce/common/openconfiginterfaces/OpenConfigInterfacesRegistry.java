/*
 * Copyright © 2026 Orange and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.common.openconfiginterfaces;

import java.util.HashMap;
import java.util.Map;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * OSGi service registry for managing OpenConfig interface implementations.
 * Dynamically discovers and routes to version-specific implementations.
 *
 * <p>This follows the same pattern as PortMappingRegistry, using @Reference
 * with MULTIPLE cardinality to support multiple OpenConfig versions.
 */
@Component(immediate = true, service = OpenConfigInterfacesRegistry.class)
public class OpenConfigInterfacesRegistry {

    private static final Logger LOG = LoggerFactory.getLogger(OpenConfigInterfacesRegistry.class);

    private final Map<String, OpenConfigInterfacesVersionService> services = new HashMap<>();

    /**
     * Bind a new OpenConfigInterfacesVersionService implementation.
     *
     * @param service the OpenConfigInterfacesVersionService to bind
     */
    @Reference(
            cardinality = ReferenceCardinality.MULTIPLE,
            policy = ReferencePolicy.DYNAMIC
    )
    public void bindOpenConfigInterfacesVersionService(OpenConfigInterfacesVersionService service) {
        String version = service.getVersion();
        services.put(version, service);
        LOG.info("OpenConfigInterfacesVersionService bound for version: {} [Total: {}]",
            version, services.size());
    }

    /**
     * Unbind an OpenConfigInterfacesVersionService implementation.
     *
     * @param service the OpenConfigInterfacesVersionService to unbind
     */
    public void unbindOpenConfigInterfacesVersionService(OpenConfigInterfacesVersionService service) {
        String version = service.getVersion();
        services.remove(version);
        LOG.info("OpenConfigInterfacesVersionService unbound for version: {} [Remaining: {}]",
            version, services.size());
    }

    /**
     * Get a service for the specified version.
     *
     * @param version the version identifier (e.g., "openconfig-200", "openconfig-560")
     * @return the OpenConfigInterfacesVersionService, or null if not found
     */
    public OpenConfigInterfacesVersionService getService(String version) {
        OpenConfigInterfacesVersionService service = services.get(version);
        if (service == null) {
            LOG.error("No OpenConfigInterfacesVersionService found for version: {} [Available: {}]",
                version, services.keySet());
        } else {
            LOG.debug("Found OpenConfigInterfacesVersionService for version: {}", version);
        }
        return service;
    }

    /**
     * Check if a version is supported.
     *
     * @param version the version identifier
     * @return true if a service is registered for this version
     */
    public boolean isVersionSupported(String version) {
        return services.containsKey(version);
    }

    /**
     * Get all registered versions.
     *
     * @return array of version strings
     */
    public String[] getRegisteredVersions() {
        return services.keySet().toArray(new String[0]);
    }
}
