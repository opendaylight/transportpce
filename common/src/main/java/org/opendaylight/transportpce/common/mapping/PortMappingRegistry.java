/*
 * Copyright © 2026 Orange and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.common.mapping;

import java.util.HashMap;
import java.util.Map;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev260908.mapping.Mapping;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Registry for OpenConfig Port Mapping Services.
 * This component discovers and manages available OCPortMappingVersionService
 * implementations for OC200 and OC560 and routes mapping requests to the
 * appropriate service based on device version.
 * Services are discovered dynamically using OSGi Service Registry with
 * cardinality MULTIPLE to handle multiple simultaneous versions.
 */
@Component(immediate = true, service = PortMappingRegistry.class)
public class PortMappingRegistry {

    private static final Logger LOG = LoggerFactory.getLogger(PortMappingRegistry.class);

    private static final Map<String, String> YANG_TO_VERSION_MAP = Map.ofEntries(
        Map.entry("(http://openconfig.net/yang/terminal-device?revision=2021-07-29)openconfig-terminal-device", "2.0.0"),
        Map.entry("(http://openconfig.net/yang/terminal-device?revision=2026-01-14)openconfig-terminal-device", "5.6.0")
    );

    private final Map<String, OCPortMappingVersionService> services = new HashMap<>();

    /**
     * Bind a new OCPortMappingVersionService implementation.
     *
     * @param service the OCPortMappingVersionService to bind
     */
    @Reference(
            cardinality = ReferenceCardinality.MULTIPLE,
            policy = ReferencePolicy.DYNAMIC
    )
    public void bindOCPortMappingVersionService(OCPortMappingVersionService service) {
        String version = service.getVersion();
        services.put(version, service);
        LOG.info("OCPortMappingVersionService bound for version: {} [Total: {}]",
            version, services.size());
    }

    /**
     * Unbind an OCPortMappingVersionService implementation.
     *
     * @param service the OCPortMappingVersionService to unbind
     */
    public void unbindOCPortMappingVersionService(OCPortMappingVersionService service) {
        String version = service.getVersion();
        services.remove(version);
        LOG.info("OCPortMappingVersionService unbound for version: {} [Remaining: {}]",
            version, services.size());
    }

    /**
     * Get a service for the specified version.
     *
     * @param version the device version identifier (YANG module or service version)
     * @return the OCPortMappingVersionService, or null if not found
     */
    public OCPortMappingVersionService getService(String version) {
        String ocVersion = YANG_TO_VERSION_MAP.getOrDefault(version, version);

        OCPortMappingVersionService service = services.get(ocVersion);
        if (service == null) {
            LOG.error("No OCPortMappingVersionService found for version: {} (mapped from: {}) [Available: {}]",
                ocVersion, version, services.keySet());
        } else {
            LOG.debug("Found OCPortMappingVersionService for version: {}", ocVersion);
        }
        return service;
    }

    /**
     * Create mapping data for a device with specified version.
     *
     * @param version the device version identifier
     * @param nodeId the node identifier
     * @param ipAddress the device IP address
     * @return true if mapping creation succeeded
     */
    public boolean createMappingData(String version, String nodeId, IpAddress ipAddress) {
        OCPortMappingVersionService service = getService(version);
        if (service != null) {
            LOG.debug("Creating mapping for {} using service {}", nodeId, version);
            return service.createMappingData(nodeId, ipAddress);
        }
        LOG.error("Unable to create mapping for {}: no service for version {} [Available versions: {}]",
            nodeId, version, services.keySet());
        return false;
    }

    /**
     * Update mapping data for a device with specified version.
     *
     * @param version the device version identifier
     * @param nodeId the node identifier
     * @param oldMapping the existing mapping to update
     * @return true if mapping update succeeded
     */
    public boolean updateMapping(String version, String nodeId, Mapping oldMapping) {
        OCPortMappingVersionService service = getService(version);
        if (service != null) {
            LOG.debug("Updating mapping for {} using service for version {}", nodeId, version);
            return service.updateMapping(nodeId, oldMapping);
        }
        LOG.error("Unable to update mapping for {}: no service for version {} [Available versions: {}]",
            nodeId, version, services.keySet());
        return false;
    }
}
