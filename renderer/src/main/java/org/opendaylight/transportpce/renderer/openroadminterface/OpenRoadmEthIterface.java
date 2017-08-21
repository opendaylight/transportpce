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
import org.opendaylight.yang.gen.v1.http.org.openroadm.ethernet.interfaces.rev161014.EthAttributes.AutoNegotiation;
import org.opendaylight.yang.gen.v1.http.org.openroadm.ethernet.interfaces.rev161014.EthAttributes.Duplex;
import org.opendaylight.yang.gen.v1.http.org.openroadm.ethernet.interfaces.rev161014.EthAttributes.Fec;
import org.opendaylight.yang.gen.v1.http.org.openroadm.ethernet.interfaces.rev161014.Interface1;
import org.opendaylight.yang.gen.v1.http.org.openroadm.ethernet.interfaces.rev161014.Interface1Builder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.ethernet.interfaces.rev161014.ethernet.container.EthernetBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.interfaces.rev161014.EthernetCsmacd;

public class OpenRoadmEthIterface extends OpenRoadmInterfaces {

    public OpenRoadmEthIterface(DataBroker db, MountPointService mps, String nodeId, String logicalConnPoint,
            String serviceName) {
        super(db, mps, nodeId, logicalConnPoint, serviceName);

    }

    public String createInterface() {
        InterfaceBuilder ethInterfaceBldr = getIntfBuilder(portMap);
        ethInterfaceBldr.setType(EthernetCsmacd.class);
        ethInterfaceBldr.setName(logicalConnPoint + "-ETHERNET");
        ethInterfaceBldr.setKey(new InterfaceKey(logicalConnPoint + "-ETHERNET"));

        // Ethernet interface specific data
        EthernetBuilder ethIfBuilder = new EthernetBuilder();
        ethIfBuilder.setAutoNegotiation(AutoNegotiation.Enabled);
        ethIfBuilder.setDuplex(Duplex.Full);
        ethIfBuilder.setFec(Fec.Off);
        ethIfBuilder.setSpeed(new Long(100000));
        ethIfBuilder.setMtu(new Long(9000));

        // Create Interface1 type object required for adding as augmentation
        Interface1Builder ethIf1Builder = new Interface1Builder();
        ethInterfaceBldr.addAugmentation(Interface1.class, ethIf1Builder.setEthernet(ethIfBuilder.build()).build());

        // Post interface on the device
        if (postInterface(ethInterfaceBldr)) {
            return ethInterfaceBldr.getName();
        } else {
            return null;
        }

    }

}
