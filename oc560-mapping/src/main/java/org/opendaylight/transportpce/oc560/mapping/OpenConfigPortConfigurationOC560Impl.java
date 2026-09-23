/*
 * Copyright © 2024 Orange and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.oc560.mapping;

import java.util.HashMap;
import java.util.Map;
import org.opendaylight.transportpce.common.mapping.PortMapping;
import org.opendaylight.transportpce.common.openconfiginterfaces.OpenConfigInterfaces;
import org.opendaylight.transportpce.common.openconfiginterfaces.OpenConfigInterfacesException;
import org.opendaylight.transportpce.common.openconfiginterfaces.OpenConfigPortConfiguration;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.platform.rev250715.platform.component.top.components.ComponentBuilder;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev260908.mapping.Mapping;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * OpenConfig 5.6.0 minimal implementation of port configuration service.
 * Registers as an OSGI service to provide OC560-specific port configuration.
 * Note: Full OC560 functionality requires additional OpenConfig 5.6.0 YANG model artifacts.
 */
@Component(service = OpenConfigPortConfiguration.class,
           property = "version=5.6.0")
public class OpenConfigPortConfigurationOC560Impl implements OpenConfigPortConfiguration {

    private static final Logger LOG = LoggerFactory.getLogger(OpenConfigPortConfigurationOC560Impl.class);

    @Reference(cardinality = ReferenceCardinality.OPTIONAL)
    private PortMapping portMapping;

    @Reference(cardinality = ReferenceCardinality.OPTIONAL, target = "(openconfig.version=5.6.0)")
    private OpenConfigInterfaces openConfigInterfaces;

    @Override
    public String getOpenConfigVersion() {
        return "5.6.0";
    }

    @Override
    public void init(PortMapping portMappingService, OpenConfigInterfaces openConfigInterfacesService) {
        this.portMapping = portMappingService;
        this.openConfigInterfaces = openConfigInterfacesService;
    }

    @Override
    public String configurePortAdminState(String nodeId, String logicalConnPoint, boolean enable)
            throws OpenConfigInterfacesException {
        if (openConfigInterfaces == null) {
            throw new OpenConfigInterfacesException("OpenConfig 5.6.0 device configuration service is unavailable");
        }
        LOG.info("Configuring OpenConfig 5.6.0 port admin state to {} on node {}",
                 enable ? "ENABLED" : "DISABLED", nodeId);

        try {
            Mapping mapping = portMapping.getMapping(nodeId, logicalConnPoint);
            ComponentBuilder portComponentBuilder = new ComponentBuilder();
            portComponentBuilder.setName(mapping.getSupportingPort());
            openConfigInterfaces.configureComponent(nodeId, portComponentBuilder);
            LOG.info("Successfully updated port on node {}", nodeId);
            return portComponentBuilder.getName();
        } catch (OpenConfigInterfacesException e) {
            LOG.error("Failed to configure port admin state on node {}: {}", nodeId, e.getMessage());
            throw e;
        }
    }

    @Override
    public void configurePortFrequency(String nodeId, String logicalConnPoint, long frequency)
            throws OpenConfigInterfacesException {
        LOG.info("OC5.6.0: Configuring port frequency to {} on node {}", frequency, nodeId);
        // Minimal stub - full frequency configuration requires extended OpenConfig models
    }

    @Override
    public Map<String, Double> getPortPowerParameters(String operationalMode) {
        LOG.debug("Getting port power parameters for OC5.6.0 operational mode: {}", operationalMode);
        // Return empty map - full implementation requires extended OpenConfig models
        return new HashMap<>();
    }
}
