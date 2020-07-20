/*
 * Copyright © 2019 AT&T and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.renderer.provisiondevice.otn;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev181019.CircuitPacks;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev181019.circuit.pack.Ports;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev181019.org.openroadm.device.container.org.openroadm.device.OduSwitchingPools;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev181019.org.openroadm.device.container.org.openroadm.device.odu.switching.pools.NonBlockingList;
import org.opendaylight.yang.gen.v1.http.org.openroadm.port.capability.rev181019.port.group.restriction.grp.PortGroupRestriction;
import org.opendaylight.yang.gen.v1.http.org.openroadm.port.capability.rev181019.port.group.restriction.grp.port.group.restriction.PortBandwidthSharing;
import org.opendaylight.yang.gen.v1.http.org.openroadm.port.capability.rev181019.port.group.restriction.grp.port.group.restriction.port.bandwidth.sharing.PortList;
import org.opendaylight.yang.gen.v1.http.org.openroadm.port.capability.rev181019.port.group.restriction.grp.port.group.restriction.port.bandwidth.sharing.PossiblePortConfig;
import org.opendaylight.yang.gen.v1.http.org.openroadm.port.capability.rev181019.port.group.restriction.grp.port.group.restriction.port.bandwidth.sharing.possible.port.config.PortIfTypeConfig;


public class OtnDeviceOperationsImpl implements OtnDeviceOperations {

    @Override
    public String validateClientPort(String circuitPackName, String portName, String capacity,
            PortGroupRestriction portGroupRestriction) {

        Collection<PortBandwidthSharing> portBandwidthSharingList = portGroupRestriction
                .nonnullPortBandwidthSharing().values();
        if (portGroupRestriction.getPortBandwidthSharing() != null) {
            for (PortBandwidthSharing portBandwidthSharing : portBandwidthSharingList) {
                Collection<PortList> portLists = portBandwidthSharing.nonnullPortList().values();
                for (PortList portList : portLists) {
                    if (portList.getCircuitPackName().equals(circuitPackName)
                            && portList.getPortName().equals(portName)) {
                        if (!usageOfOtherPorts(portBandwidthSharing,
                                getConfigID(portBandwidthSharing,
                                        circuitPackName, portName))) {
                            return "valid port";
                        } else {
                            return "not a valid port";
                        }
                    }
                }
            }
        }
     // if the client port is not found at all throw exception
        return "not valid circuitPackName or portName";
    }

    private Integer getConfigID(PortBandwidthSharing portBandwidthSharing, String circuitPackName, String portName) {
        Collection<PossiblePortConfig> possiblePortConfigList = portBandwidthSharing
                .nonnullPossiblePortConfig().values();
        for (PossiblePortConfig possiblePortConfig: possiblePortConfigList
             ) {
            Collection<PortIfTypeConfig> portIfTypeConfigList = possiblePortConfig.nonnullPortIfTypeConfig().values();
            for (PortIfTypeConfig portIfTypeConfig : portIfTypeConfigList) {
                if (portIfTypeConfig.getCircuitPackName().equals(circuitPackName)
                        && portIfTypeConfig.getPortName().equals(portName)) {
                    return possiblePortConfig.getConfigId().toJava();
                }
            }
        }
        // throw exception if not able to get config id
        return null;
    }


    private boolean usageOfOtherPorts(PortBandwidthSharing portBandwidthSharing, Integer configId) {
        return false;
    }

    @Override
    public List<org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev181019.org
                .openroadm.device.container.org.openroadm.device.odu.switching.pools.non.blocking.list.PortList>
                getPossibleNetworkPorts(String circuitPackName, String portName, OduSwitchingPools oduSwitchingPools,
                        CircuitPacks circuitPacks) {
        List<org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev181019.org
                .openroadm.device.container.org.openroadm.device.odu.switching
                .pools.non.blocking.list.PortList> networkPortList = new ArrayList<>();
        Collection<NonBlockingList> nonBlockingLists = oduSwitchingPools.nonnullNonBlockingList().values();


        for (NonBlockingList nonBlockingList: nonBlockingLists) {

            for (org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev181019.org.openroadm.device
                    .container.org.openroadm.device.odu.switching.pools.non.blocking.list.PortList port:
                 nonBlockingList.nonnullPortList().values()) {

                if (port.getCircuitPackName().equals(circuitPackName) && port.getPortName().equals(portName)) {
                    org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev181019.org.openroadm.device
                            .container.org.openroadm.device.odu.switching.pools.non.blocking.list
                            .PortList networkPort = checkNetworkPorts(nonBlockingList
                                    .nonnullPortList().values(), circuitPacks);
                    if (networkPort != null) {
                        networkPortList.add(networkPort);
                    }
                }
            }
        }
        return networkPortList;
    }


    private org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev181019.org.openroadm.device
            .container.org.openroadm.device.odu.switching.pools.non.blocking.list.PortList
        checkNetworkPorts(Collection<org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev181019.org
            .openroadm.device.container.org.openroadm.device.odu.switching.pools.non.blocking.list.PortList> portList,
                CircuitPacks circuitPacks) {
        Collection<org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev181019.circuit.packs
            .CircuitPacks> circuitPackList = circuitPacks.nonnullCircuitPacks().values();

        for (org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev181019.org.openroadm.device.container
            .org.openroadm.device.odu.switching.pools.non.blocking.list.PortList port: portList) {
            for (org.opendaylight.yang.gen.v1.http.org.openroadm.device
                .rev181019.circuit.packs.CircuitPacks circuitPack: circuitPackList) {
                return searchNetworkPort(port, circuitPack);
            }
        }
     // TODO: throw exception if no available network ports
        return null;
    }

    private org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev181019.org.openroadm.device.container
        .org.openroadm.device.odu.switching.pools.non.blocking.list.PortList
        searchNetworkPort(org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev181019.org.openroadm.device
            .container.org.openroadm.device.odu.switching.pools.non.blocking.list.PortList port,
            org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev181019.circuit.packs.CircuitPacks circuitPack) {
        if (port.getCircuitPackName().equals(circuitPack.getCircuitPackName())) {
            for (Ports prt : circuitPack.nonnullPorts().values()) {
                if (prt.getPortQual() != null
                        && port.getPortName().equals(prt.getPortName())
                        && "xpdr-network".equals(prt.getPortQual().getName())) {
                    return port;
                }
            }
        }
     // TODO: throw exception
        return null;
    }


    //check supported interface types

}
