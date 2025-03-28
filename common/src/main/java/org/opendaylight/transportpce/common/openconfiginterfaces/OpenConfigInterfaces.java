/*
 * Copyright © 2024 NTT and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.common.openconfiginterfaces;

public interface OpenConfigInterfaces {

    /**
     * This method does an edit-config operation on the openConfig device to
     * provision the service-path.
     *
     * @param nodeId node to be provisioned
     * @param componentBuilder builder containing the components and respective configuration
     * @param <T> specified type of object
     *
     * @throws OpenConfigInterfacesException open config exception
     */

    <T> void configureComponent(String nodeId, T componentBuilder) throws OpenConfigInterfacesException;

    /**
     * This method does an edit-config operation on the openConfig device to
     * provision the service-path.
     *
     * @param nodeId node to be provisioned
     * @param interfaceBuilder builder containing the interfaces and respective configuration
     * @param <T> specified type of object
     *
     * @throws OpenConfigInterfacesException open config exception
     */

    <T> void configureInterface(String nodeId, T interfaceBuilder) throws OpenConfigInterfacesException;
}
