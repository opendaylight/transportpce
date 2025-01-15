/*
 * Copyright © 2025 Smartoptics and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.pce.config;

import org.opendaylight.transportpce.pce.spectrum.assignment.state.SpectrumAssignmentState;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(configurationPid = "org.opendaylight.transportpce.pce.config")
public class PCEConfig implements Config {

    @ObjectClassDefinition
    public @interface Configuration {
        @AttributeDefinition
        String availablespectrumiterationdirectionstrategy() default SpectrumAssignmentState.HIGH_TO_LOW_FREQUENCY;
    }
    private static final Logger LOG = LoggerFactory.getLogger(PCEConfig.class);

    private final String availablespectrumiterationdirectionstrategy;

    public PCEConfig() {
        this(SpectrumAssignmentState.HIGH_TO_LOW_FREQUENCY);
    }

    @Activate
    public PCEConfig(final Configuration configuration) {
        this(configuration.availablespectrumiterationdirectionstrategy());
        LOG.debug("PCEConfig ignored configuration files.");
    }

    public PCEConfig(String availableSpectrumIterationDirectionStrategy) {
        LOG.debug("Find available spectrum strategy {}", availableSpectrumIterationDirectionStrategy);
        this.availablespectrumiterationdirectionstrategy = availableSpectrumIterationDirectionStrategy;
    }

    @Override
    public String availableSpectrumIterationDirectionStrategy() {
        return availablespectrumiterationdirectionstrategy;
    }

    @Deactivate
    public void close() {
        LOG.info("Closing component PCEConfig");
    }
}
