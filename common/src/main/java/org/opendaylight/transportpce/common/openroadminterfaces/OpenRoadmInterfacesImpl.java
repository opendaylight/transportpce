/*
 * Copyright © 2017 AT&T and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.common.openroadminterfaces;

import static org.opendaylight.transportpce.common.StringConstants.OPENROADM_DEVICE_VERSION_1_2_1;
import static org.opendaylight.transportpce.common.StringConstants.OPENROADM_DEVICE_VERSION_2_2;

import java.util.Optional;
import org.opendaylight.transportpce.common.device.DeviceTransactionManager;
import org.opendaylight.transportpce.common.mapping.MappingUtils;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.interfaces.grp.InterfaceBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class OpenRoadmInterfacesImpl implements OpenRoadmInterfaces {

    private static final Logger LOG = LoggerFactory.getLogger(OpenRoadmInterfacesImpl.class);

    private final DeviceTransactionManager deviceTransactionManager;
    OpenRoadmInterfacesImpl121 openRoadmInterfacesImpl121;
    OpenRoadmInterfacesImpl22 openRoadmInterfacesImpl22;
    MappingUtils mappingUtils;

    public OpenRoadmInterfacesImpl(DeviceTransactionManager deviceTransactionManager,MappingUtils mappingUtils,
        OpenRoadmInterfacesImpl121 openRoadmInterfacesImpl121,OpenRoadmInterfacesImpl22 openRoadmInterfacesImpl22) {
        this.deviceTransactionManager = deviceTransactionManager;
        this.mappingUtils = mappingUtils;
        this.openRoadmInterfacesImpl121 = openRoadmInterfacesImpl121;
        this.openRoadmInterfacesImpl22 = openRoadmInterfacesImpl22;
    }

    @Override
    public <T> void postInterface(String nodeId, T ifBuilder) throws OpenRoadmInterfaceException {

        String openRoadmVersion = mappingUtils.getOpenRoadmVersion(nodeId);
        LOG.info("Interface post request received for node {} with version {}",nodeId,openRoadmVersion);
        if (openRoadmVersion.equals(OPENROADM_DEVICE_VERSION_1_2_1)) {
            LOG.info("Device Version is 1.2.1");
            InterfaceBuilder ifBuilder121 = convertInstanceOfInterface(ifBuilder, InterfaceBuilder.class);
            openRoadmInterfacesImpl121.postInterface(nodeId,ifBuilder121);
        }
        else if (openRoadmVersion.equals(OPENROADM_DEVICE_VERSION_2_2)) {
            org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev171215.interfaces.grp.InterfaceBuilder
                ifBuilder22 = convertInstanceOfInterface(ifBuilder, org.opendaylight.yang.gen.v1.http.org.openroadm
                .device.rev171215.interfaces.grp.InterfaceBuilder.class);
            openRoadmInterfacesImpl22.postInterface(nodeId,ifBuilder22);
        }
    }

    @Override
    public <T> Optional<T>  getInterface(String nodeId, String interfaceName) throws OpenRoadmInterfaceException {

        String openRoadmVersion = mappingUtils.getOpenRoadmVersion(nodeId);
        LOG.info("Interface get request received for node {} with version {}",nodeId,openRoadmVersion);
        if (openRoadmVersion.equals(OPENROADM_DEVICE_VERSION_1_2_1)) {
            LOG.info("Device Version is 1.2.1");
            return (Optional<T>) openRoadmInterfacesImpl121.getInterface(nodeId,interfaceName);
        }
        else if (openRoadmVersion.equals(OPENROADM_DEVICE_VERSION_2_2)) {
            return (Optional<T>) openRoadmInterfacesImpl22.getInterface(nodeId,interfaceName);
        }
        return null;
    }

    @Override
    public void deleteInterface(String nodeId, String interfaceName)
        throws OpenRoadmInterfaceException {

        String openRoadmVersion = mappingUtils.getOpenRoadmVersion(nodeId);
        LOG.info("Interface delete request received for node {} with version {}",nodeId,openRoadmVersion);
        if (openRoadmVersion.equals(OPENROADM_DEVICE_VERSION_1_2_1)) {
            LOG.info("Device Version is 1.2.1");
            openRoadmInterfacesImpl121.deleteInterface(nodeId,interfaceName);
        }
        else if (openRoadmVersion.equals(OPENROADM_DEVICE_VERSION_2_2)) {
            openRoadmInterfacesImpl22.deleteInterface(nodeId,interfaceName);
        }
    }

    @Override
    public void postEquipmentState(String nodeId, String circuitPackName, boolean activate)
        throws OpenRoadmInterfaceException {
        String openRoadmVersion = mappingUtils.getOpenRoadmVersion(nodeId);

        LOG.info("Interface delete request received for node {} with version {}",nodeId,openRoadmVersion);
        if (openRoadmVersion.equals(OPENROADM_DEVICE_VERSION_1_2_1)) {
            LOG.info("Device Version is 1.2.1");
            openRoadmInterfacesImpl121.postEquipmentState(nodeId, circuitPackName, activate);
        }
        else if (openRoadmVersion.equals(OPENROADM_DEVICE_VERSION_2_2)) {
            openRoadmInterfacesImpl22.postEquipmentState(nodeId, circuitPackName, activate);
        }

    }

    private <T> T convertInstanceOfInterface(Object object, Class<T> classToCast) {
        try {
            return classToCast.cast(object);
        } catch (ClassCastException e) {
            return null;
        }
    }
}
