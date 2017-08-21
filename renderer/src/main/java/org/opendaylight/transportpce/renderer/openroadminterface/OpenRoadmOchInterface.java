/*
 * Copyright © 2017 AT&T and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.renderer.openroadminterface;

import java.math.BigDecimal;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.MountPointService;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.types.rev161014.PowerDBm;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.interfaces.grp.InterfaceBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.interfaces.grp.InterfaceKey;
import org.opendaylight.yang.gen.v1.http.org.openroadm.interfaces.rev161014.OpticalChannel;
import org.opendaylight.yang.gen.v1.http.org.openroadm.optical.channel.interfaces.rev161014.Interface1;
import org.opendaylight.yang.gen.v1.http.org.openroadm.optical.channel.interfaces.rev161014.Interface1Builder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.optical.channel.interfaces.rev161014.OchAttributes.ModulationFormat;
import org.opendaylight.yang.gen.v1.http.org.openroadm.optical.channel.interfaces.rev161014.RateIdentity;
import org.opendaylight.yang.gen.v1.http.org.openroadm.optical.channel.interfaces.rev161014.och.container.OchBuilder;

public class OpenRoadmOchInterface extends OpenRoadmInterfaces {

    public OpenRoadmOchInterface(DataBroker db, MountPointService mps, String nodeId, String logicalConnPoint,
            String serviceName) {
        super(db, mps, nodeId, logicalConnPoint, serviceName);
    }

    /**
     * This methods creates an OCH interface on the given termination point on
     * Roadm.
     *
     * @param waveNumber
     *            wavelength number of the OCH interface.
     *
     * @return Name of the interface if successful, otherwise return null.
     */

    public String createInterface(Long waveNumber) {

        // Create generic interface
        InterfaceBuilder ochInterfaceBldr = getIntfBuilder(portMap);
        ochInterfaceBldr.setType(OpticalChannel.class);
        ochInterfaceBldr.setName(logicalConnPoint + "-" + waveNumber);
        ochInterfaceBldr.setKey(new InterfaceKey(logicalConnPoint + "-" + waveNumber));

        // OCH interface specific data
        OchBuilder ocIfBuilder = new OchBuilder();
        ocIfBuilder.setWavelengthNumber(waveNumber);

        // Add supporting OMS interface
        if (portMap.getSupportingOms() != null) {
            ochInterfaceBldr.setSupportingInterface(portMap.getSupportingOms());
        }
        // Create Interface1 type object required for adding as augmentation
        Interface1Builder ochIf1Builder = new Interface1Builder();
        ochInterfaceBldr.addAugmentation(Interface1.class, ochIf1Builder.setOch(ocIfBuilder.build()).build());

        // Post interface on the device
        if (postInterface(ochInterfaceBldr)) {
            return ochInterfaceBldr.getName();
        } else {
            return null;
        }
    }

    public String createInterface(Long waveNumber, Class<? extends RateIdentity> rate, ModulationFormat format) {

        // Create generic interface
        InterfaceBuilder ochInterfaceBldr = getIntfBuilder(portMap);
        ochInterfaceBldr.setType(OpticalChannel.class);
        ochInterfaceBldr.setName(logicalConnPoint + "-" + waveNumber);
        ochInterfaceBldr.setKey(new InterfaceKey(logicalConnPoint + "-" + waveNumber));

        // OCH interface specific data
        OchBuilder ocIfBuilder = new OchBuilder();
        ocIfBuilder.setWavelengthNumber(waveNumber);
        ocIfBuilder.setModulationFormat(format);
        ocIfBuilder.setRate(rate);
        ocIfBuilder.setTransmitPower(new PowerDBm(new BigDecimal("-5")));

        // Create Interface1 type object required for adding as augmentation
        Interface1Builder ochIf1Builder = new Interface1Builder();
        ochInterfaceBldr.addAugmentation(Interface1.class, ochIf1Builder.setOch(ocIfBuilder.build()).build());

        // Post interface on the device
        if (postInterface(ochInterfaceBldr)) {
            return ochInterfaceBldr.getName();
        } else {
            return null;
        }
    }

}
