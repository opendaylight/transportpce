/*
 * Copyright © 2017 AT&T and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.common.openroadminterfaces;

import java.util.Optional;

import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.interfaces.grp.Interface;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.interfaces.grp.InterfaceBuilder;

public interface OpenRoadmInterfaces {

    /**
     * This methods does an edit-config operation on the openROADM device in
     * order to create the given interface.
     *
     * <p>
     * Before posting the interface it checks if: 1. Interface with same name
     * does not exist 2. If exists then admin state of interface is
     * outOfState/Maintenance
     * </p>
     *
     * @param nodeId node ID
     *
     * @param ifBuilder Builder object containing the data to post.
     *
     * @throws OpenRoadmInterfaceException OpenRoadm Interface Exception
     *
     */
    void postInterface(String nodeId, InterfaceBuilder ifBuilder) throws OpenRoadmInterfaceException;

    /**
     * This methods does an edit-config operation on the openROADM device in
     * order to manages the equipment-state status of the circuit pack on which
     * OpenRoadmEthernet or OpenRoadmOch interfaces are created, according to
     * OpenRoadm whitepaper. Concerns only XPDR node.
     *
     * @param nodeId node ID
     * @param circuitPackName Circtuit-Pack name
     * @param activate activate or not
     */
    void postEquipmentState(String nodeId, String circuitPackName, boolean activate) throws OpenRoadmInterfaceException;

    /**
     * This private does a get on the interface subtree of the device with the
     * interface name as the key and return the class corresponding to the
     * interface type.
     *
     * @param nodeId node ID
     *
     * @param interfaceName
     *            Name of the interface
     *
     * @return Optional of Interface from datastore
     *
     * @throws OpenRoadmInterfaceException OpenRoadm Interface Exception
     */
    Optional<Interface> getInterface(String nodeId, String interfaceName) throws OpenRoadmInterfaceException;

    /**
     * This methods does an edit-config operation on the openROADM device in
     * order to delete the given interface.
     *
     * <p>
     * Before deleting the method: 1. Checks if interface exists 2. If exists
     * then changes the state of interface to outOfService
     * </p>
     *
     * @param nodeId node ID
     *
     * @param interfaceName
     *            Name of the interface to delete.
     *
     * @throws OpenRoadmInterfaceException OpenRoadm Interface Exception
     */
    void deleteInterface(String nodeId, String interfaceName) throws OpenRoadmInterfaceException;

}
