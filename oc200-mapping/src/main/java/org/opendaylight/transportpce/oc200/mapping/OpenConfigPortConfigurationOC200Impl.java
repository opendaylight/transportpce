/*
 * Copyright © 2024 Orange and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.oc200.mapping;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Set;
import org.opendaylight.transportpce.common.catalog.CatalogUtils;
import org.opendaylight.transportpce.common.mapping.PortMapping;
import org.opendaylight.transportpce.common.network.NetworkTransactionService;
import org.opendaylight.transportpce.common.openconfiginterfaces.OpenConfigInterfaces;
import org.opendaylight.transportpce.common.openconfiginterfaces.OpenConfigInterfacesException;
import org.opendaylight.transportpce.common.openconfiginterfaces.OpenConfigPortConfiguration;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.platform.rev221220.platform.component.top.components.ComponentBuilder;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.terminal.device.rev210729.Component1Builder;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.terminal.device.rev210729.terminal.optical.channel.top.OpticalChannelBuilder;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.terminal.device.rev210729.terminal.optical.channel.top.optical.channel.ConfigBuilder;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.transport.types.rev230208.AdminStateType;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev260908.mapping.Mapping;
import org.opendaylight.yangtools.yang.common.Decimal64;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * OpenConfig 2.0.0 implementation of port configuration service.
 * Registers as an OSGI service to provide OC200-specific port configuration.
 */
@Component(service = OpenConfigPortConfiguration.class,
           property = {"version=2.0.0", "openconfig-version=200"})
public class OpenConfigPortConfigurationOC200Impl implements OpenConfigPortConfiguration {

    private static final Logger LOG = LoggerFactory.getLogger(OpenConfigPortConfigurationOC200Impl.class);

    @Reference(cardinality = ReferenceCardinality.OPTIONAL)
    private PortMapping portMapping;

    @Reference(target = "(openconfig.version=2.0.0)")
    private OpenConfigInterfaces openConfigInterfaces;

    @Reference(cardinality = ReferenceCardinality.OPTIONAL)
    private NetworkTransactionService networkTransactionService;

    @Override
    public String getOpenConfigVersion() {
        return "2.0.0";
    }

    @Override
    public void init(PortMapping portMappingService, OpenConfigInterfaces openConfigInterfacesService) {
        this.portMapping = portMappingService;
        this.openConfigInterfaces = openConfigInterfacesService;
    }

    @Override
    public String configurePortAdminState(String nodeId, String logicalConnPoint, boolean enable)
            throws OpenConfigInterfacesException {
        LOG.info("Configuring OpenConfig 200 port admin state to {} on node {}",
                 enable ? "ENABLED" : "DISABLED", nodeId);

        try {
            Mapping mapping = portMapping.getMapping(nodeId, logicalConnPoint);
            AdminStateType adminStateType = enable ? AdminStateType.ENABLED : AdminStateType.DISABLED;
            ComponentBuilder portComponentBuilder = createPortComponentBuilder(mapping.getSupportingPort(),
                                                                               adminStateType);
            openConfigInterfaces.configureComponent(nodeId, portComponentBuilder);
            LOG.info("Successfully updated port admin state to {} at port {} on node {}",
                    adminStateType.name(), mapping.getSupportingPort(), nodeId);
            return portComponentBuilder.getName();
        } catch (OpenConfigInterfacesException e) {
            LOG.error("Failed to configure port admin state on node {}: {}", nodeId, e.getMessage());
            throw e;
        }
    }

    @Override
    public void configureTargetOutputPower(String nodeId, Set<String> opticalChannels, BigDecimal power)
            throws OpenConfigInterfacesException {
        var config = new ConfigBuilder().setTargetOutputPower(Decimal64.valueOf(power)).build();
        var opticalChannel = new OpticalChannelBuilder().setConfig(config).build();
        var augmentation = new Component1Builder().setOpticalChannel(opticalChannel).build();
        for (String name : opticalChannels) {
            openConfigInterfaces.configureComponent(nodeId,
                    new ComponentBuilder().setName(name).addAugmentation(augmentation));
        }
    }

    @Override
    public void configurePortFrequency(String nodeId, String logicalConnPoint, long frequency)
            throws OpenConfigInterfacesException {
        LOG.info("OC200 does not support explicit frequency configuration");
    }

    @Override
    public Map<String, Double> getPortPowerParameters(String operationalMode) {
        if (networkTransactionService == null) {
            LOG.warn("NetworkTransactionService not available, returning empty power parameters");
            return Map.of();
        }
        CatalogUtils catalogUtils = new CatalogUtils(networkTransactionService);
        return catalogUtils.getMinMaxOutputPower(operationalMode);
    }

    /**
     * Creates a ComponentBuilder for port admin state configuration.
     *
     * @param supportingPort the physical port identifier
     * @param adminStateType the desired admin state (ENABLED or DISABLED)
     * @return configured ComponentBuilder
     */
    private ComponentBuilder createPortComponentBuilder(String supportingPort, AdminStateType adminStateType) {
        ComponentBuilder componentBuilder = new ComponentBuilder();
        componentBuilder.setName(supportingPort);
        // OC200 specific configuration would go here
        return componentBuilder;
    }
}
