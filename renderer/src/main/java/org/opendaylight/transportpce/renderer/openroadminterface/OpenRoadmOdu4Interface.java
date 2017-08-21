/*
 * Copyright © 2017 AT&T and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.renderer.openroadminterface;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.MountPointService;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.interfaces.grp.InterfaceBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.interfaces.grp.InterfaceKey;
import org.opendaylight.yang.gen.v1.http.org.openroadm.interfaces.rev161014.OtnOdu;
import org.opendaylight.yang.gen.v1.http.org.openroadm.otn.odu.interfaces.rev161014.Interface1;
import org.opendaylight.yang.gen.v1.http.org.openroadm.otn.odu.interfaces.rev161014.Interface1Builder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.otn.odu.interfaces.rev161014.ODU4;
import org.opendaylight.yang.gen.v1.http.org.openroadm.otn.odu.interfaces.rev161014.OduAttributes.MonitoringMode;
import org.opendaylight.yang.gen.v1.http.org.openroadm.otn.odu.interfaces.rev161014.odu.container.OduBuilder;

public class OpenRoadmOdu4Interface extends OpenRoadmInterfaces {

    public OpenRoadmOdu4Interface(DataBroker db, MountPointService mps, String nodeId, String logicalConnPoint,
            String serviceName) {
        super(db, mps, nodeId, logicalConnPoint, serviceName);

    }

    /**
     * This methods creates an ODU interface on the given termination point.
     *
     * @param supportingOtuInterface
     *          supporting Otu Interface
     *
     * @return Name of the interface if successful, otherwise return null.
     */

    public String createInterface(String supportingOtuInterface) {

        InterfaceBuilder oduInterfaceBldr = getIntfBuilder(portMap);
        oduInterfaceBldr.setType(OtnOdu.class);
        oduInterfaceBldr.setSupportingInterface(supportingOtuInterface);
        oduInterfaceBldr.setName(logicalConnPoint + "-ODU");
        oduInterfaceBldr.setKey(new InterfaceKey(logicalConnPoint + "-ODU"));

        // ODU interface specific data
        OduBuilder oduIfBuilder = new OduBuilder();
        oduIfBuilder.setRate(ODU4.class);
        oduIfBuilder.setMonitoringMode(MonitoringMode.Monitored);

        // Create Interface1 type object required for adding as augmentation
        Interface1Builder oduIf1Builder = new Interface1Builder();
        oduInterfaceBldr.addAugmentation(Interface1.class, oduIf1Builder.setOdu(oduIfBuilder.build()).build());

        // Post interface on the device
        if (postInterface(oduInterfaceBldr)) {
            return oduInterfaceBldr.getName();
        } else {
            return null;
        }
    }
}
