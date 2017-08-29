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
import org.opendaylight.yang.gen.v1.http.org.openroadm.interfaces.rev161014.OtnOtu;

import org.opendaylight.yang.gen.v1.http.org.openroadm.otn.otu.interfaces.rev161014.Interface1;
import org.opendaylight.yang.gen.v1.http.org.openroadm.otn.otu.interfaces.rev161014.Interface1Builder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.otn.otu.interfaces.rev161014.OTU4;
import org.opendaylight.yang.gen.v1.http.org.openroadm.otn.otu.interfaces.rev161014.OtuAttributes.Fec;
import org.opendaylight.yang.gen.v1.http.org.openroadm.otn.otu.interfaces.rev161014.otu.container.OtuBuilder;

public class OpenRoadmOtu4Interface extends OpenRoadmInterfaces {

    public OpenRoadmOtu4Interface(DataBroker db, MountPointService mps, String nodeId, String logicalConnPoint,
            String serviceName) {
        super(db, mps, nodeId, logicalConnPoint, serviceName);

    }

    /**
     * This methods creates an OTU interface on the given termination point.
     *
     * @param supportOchInterface
     *          support Och Interface
     *
     * @return Name of the interface if successful, otherwise return null.
     */

    public String createInterface(String supportOchInterface) {
        // Create generic interface
        InterfaceBuilder otuInterfaceBldr = getIntfBuilder(portMap);
        otuInterfaceBldr.setType(OtnOtu.class);
        otuInterfaceBldr.setSupportingInterface(supportOchInterface);
        otuInterfaceBldr.setName(logicalConnPoint + "-OTU");
        otuInterfaceBldr.setKey(new InterfaceKey(logicalConnPoint + "-OTU"));

        // OTU interface specific data
        OtuBuilder otuIfBuilder = new OtuBuilder();
        otuIfBuilder.setFec(Fec.Scfec);
        otuIfBuilder.setRate(OTU4.class);

        // Create Interface1 type object required for adding as augmentation
        Interface1Builder otuIf1Builder = new Interface1Builder();
        otuInterfaceBldr.addAugmentation(Interface1.class, otuIf1Builder.setOtu(otuIfBuilder.build()).build());

        // Post interface on the device
        if (postInterface(otuInterfaceBldr)) {
            return otuInterfaceBldr.getName();
        } else {
            return null;
        }
    }
}
