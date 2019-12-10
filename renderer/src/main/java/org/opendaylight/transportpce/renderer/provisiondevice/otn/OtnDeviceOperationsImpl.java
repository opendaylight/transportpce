/*
 * Copyright © 2019 AT&T and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.renderer.provisiondevice.otn;

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
    public String validateClientPort(String nodeID, String circuitPackName, String portName, String capacity) {

        PortGroupRestriction portGroupRestriction = null; //should not be initiated as null
        List<PortBandwidthSharing> portBandwidthSharingList = portGroupRestriction.getPortBandwidthSharing();
        for (PortBandwidthSharing portBandwidthSharing: portBandwidthSharingList) {
            List<PortList> portLists = portBandwidthSharing.getPortList();
            for (PortList portList: portLists) {
                if (portList.getCircuitPackName().equals(circuitPackName) && portList.getPortName().equals(portName)) {
                    if (!usageOfOtherPorts(portBandwidthSharing,
                            getConfigID(portBandwidthSharing, circuitPackName, portName))) {
                        return "valid port";
                    }
                    else {
                        return "not a valid port";
                    }
                }
            }
        }
        return "not valid circuitPackName or portName"; // if the client port is not found at all throw exception
    }

    private Integer getConfigID(PortBandwidthSharing portBandwidthSharing, String circuitPackName, String portName) {
        List<PossiblePortConfig> possiblePortConfigList = portBandwidthSharing.getPossiblePortConfig();
        for (PossiblePortConfig possiblePortConfig: possiblePortConfigList
             ) {
            List<PortIfTypeConfig> portIfTypeConfigList = possiblePortConfig.getPortIfTypeConfig();
            for (PortIfTypeConfig portIfTypeConfig : portIfTypeConfigList) {
                if (portIfTypeConfig.getCircuitPackName().equals(circuitPackName)
                        && portIfTypeConfig.getPortName().equals(portName)) {
                    return possiblePortConfig.getConfigId();
                }
            }
        }
        return null; // throw exception if not able to get config id
    }


    private boolean usageOfOtherPorts(PortBandwidthSharing portBandwidthSharing, Integer configId) {
        return false;
    }

    @Override
    public List<org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev181019.org
                .openroadm.device.container.org.openroadm.device.odu.switching.pools.non.blocking.list.PortList>
                getPossibleNetworkPorts(String circuitPackName, String portName) {
        List<org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev181019.org
                .openroadm.device.container.org.openroadm.device.odu.switching
                .pools.non.blocking.list.PortList> networkPortList = null;

        //need a method to get swtiching pool object from device

        OduSwitchingPools oduSwitchingPools = null; // should not be initiated as null
        List<NonBlockingList> nonBlockingLists = oduSwitchingPools.getNonBlockingList();


        for (NonBlockingList nonBlockingList: nonBlockingLists) {

            for (org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev181019.org.openroadm.device
                    .container.org.openroadm.device.odu.switching.pools.non.blocking.list.PortList port:
                 nonBlockingList.getPortList()) {

                if (port.getCircuitPackName().equals(circuitPackName) && port.getPortName().equals(portName)) {
                    org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev181019.org.openroadm.device
                            .container.org.openroadm.device.odu.switching.pools.non.blocking.list
                            .PortList networkPort = checkNetworkPorts(nonBlockingList.getPortList());
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
        checkNetworkPorts(List<org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev181019.org
            .openroadm.device.container.org.openroadm.device.odu.switching.pools.non.blocking.list.PortList> portList) {
        CircuitPacks circuitPacks = null;

        List<org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev181019.circuit.packs
            .CircuitPacks> circuitPackList = circuitPacks.getCircuitPacks();

        for (org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev181019.org.openroadm.device.container
            .org.openroadm.device.odu.switching.pools.non.blocking.list.PortList port: portList) {
            for (org.opendaylight.yang.gen.v1.http.org.openroadm.device
                .rev181019.circuit.packs.CircuitPacks circuitPack: circuitPackList) {
                return searchNetworkPort(port, circuitPack);
            }
        }
        return null; // throw exception if no available network ports
    }

    private org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev181019.org.openroadm.device.container
        .org.openroadm.device.odu.switching.pools.non.blocking.list.PortList
        searchNetworkPort(org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev181019.org.openroadm.device
            .container.org.openroadm.device.odu.switching.pools.non.blocking.list.PortList port,
            org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev181019.circuit.packs.CircuitPacks circuitPack) {
        if (port.getCircuitPackName().equals(circuitPack.getCircuitPackName())) {
            for (Ports prt : circuitPack.getPorts()) {
                if (port.getPortName().equals(prt.getPortName()) && prt.getPortQual().equals("xpdr-network")) {
                    return port;
                }
            }
        }
        return null; // throw exception
    }


    //check supported interface types

}