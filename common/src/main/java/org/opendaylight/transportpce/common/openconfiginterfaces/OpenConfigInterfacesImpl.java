/*
 * Copyright © 2024 NTT and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.common.openconfiginterfaces;

import org.opendaylight.transportpce.common.device.DeviceTransactionManager;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.interfaces.rev210406.interfaces.top.interfaces.InterfaceBuilder;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.platform.rev220610.platform.component.top.components.ComponentBuilder;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
public class OpenConfigInterfacesImpl implements OpenConfigInterfaces {

    private static final Logger LOG = LoggerFactory.getLogger(OpenConfigInterfacesImpl.class);

    OpenConfigInterfacesImpl190 openConfigInterfacesImpl190;

    @Activate
    public OpenConfigInterfacesImpl(@Reference DeviceTransactionManager deviceTransactionManager) {
        this(new OpenConfigInterfacesImpl190(deviceTransactionManager));
    }

    public OpenConfigInterfacesImpl(OpenConfigInterfacesImpl190 openConfigInterfacesImpl190) {
        this.openConfigInterfacesImpl190 = openConfigInterfacesImpl190;
    }

    @Override
    public <T> void configureComponent(String nodeId, T componentBuilder) throws OpenConfigInterfacesException {
        ComponentBuilder compBuilder = convertInstanceOfInterface(componentBuilder, ComponentBuilder.class);
        openConfigInterfacesImpl190.configureComponents(nodeId, compBuilder);

    }

    @Override
    public <T> void configureInterface(String nodeId, T interfaceBuilder) throws OpenConfigInterfacesException {
        InterfaceBuilder interfBuilder = convertInstanceOfInterface(interfaceBuilder, InterfaceBuilder.class);
        openConfigInterfacesImpl190.configureInterface(nodeId, interfBuilder);
    }

    private <T> T convertInstanceOfInterface(Object object, Class<T> classToCast) {
        try {
            return classToCast.cast(object);
        } catch (ClassCastException e) {
            LOG.error("Error while casting the component/interface", e);
            return null;
        }
    }
}
