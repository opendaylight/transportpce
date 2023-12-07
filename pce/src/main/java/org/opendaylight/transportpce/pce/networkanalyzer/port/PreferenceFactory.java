/*
 * Copyright (c) 2024 Smartoptics and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.pce.networkanalyzer.port;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev230925.PathComputationRequestInput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev230925.path.computation.request.input.ServiceAEnd;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev230925.path.computation.request.input.ServiceZEnd;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev230526.service.port.Port;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.service.types.rev220118.service.endpoint.sp.RxDirection;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.service.types.rev220118.service.endpoint.sp.TxDirection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PreferenceFactory implements Factory {

    private static final Logger LOG = LoggerFactory.getLogger(PreferenceFactory.class);

    private final String portNamePattern = "(?i)SRG\\d+-PP\\d+-(TXRX|TX|RX)";

    @Override
    public Preference portPreference(PathComputationRequestInput pathComputationRequestInput) {

        Map<String, Set<String>> map = nodePortMap(pathComputationRequestInput);

        if (map.isEmpty()) {
            LOG.debug("No port preference found in path computation request.");
            return new NoPreference();
        }

        LOG.debug("Port preference in path computation request: {}." , map);
        return new ClientPreference(map);
    }

    /**
     * Create a key value mapper from PCRI where key is the node and the value is
     * a unique list of port names.
     *
     * @return Client port preference map
     */
    Map<String, Set<String>> nodePortMap(PathComputationRequestInput pathComputationRequestInput) {

        Map<String, Set<String>> mapper = new HashMap<>();

        ServiceAEnd serviceAEnd = pathComputationRequestInput.getServiceAEnd();
        if (serviceAEnd != null) {

            RxDirection rxAzDirection = serviceAEnd.getRxDirection();
            if (rxAzDirection != null) {

                Port rxAZport = rxAzDirection.getPort();
                if (rxAZport != null) {
                    add(rxAZport.getPortDeviceName(), rxAZport.getPortName(), mapper);
                }
            }

            TxDirection txAzDirection = serviceAEnd.getTxDirection();
            if (txAzDirection != null) {

                Port txAZport = txAzDirection.getPort();
                if (txAZport != null) {
                    add(txAZport.getPortDeviceName(), txAZport.getPortName(), mapper);
                }
            }
        }

        ServiceZEnd serviceZEnd = pathComputationRequestInput.getServiceZEnd();
        if (serviceZEnd != null) {

            RxDirection rxZaDirection = serviceZEnd.getRxDirection();
            if (rxZaDirection != null) {

                Port rxZAport = rxZaDirection.getPort();
                if (rxZAport != null) {
                    add(rxZAport.getPortDeviceName(), rxZAport.getPortName(), mapper);
                }
            }

            TxDirection txZaDirection = serviceZEnd.getTxDirection();
            if (txZaDirection != null) {

                Port txZAport = txZaDirection.getPort();
                if (txZAport != null) {
                    add(txZAport.getPortDeviceName(), txZAport.getPortName(), mapper);
                }
            }
        }

        return mapper;
    }

    /**
     * Add node/port name to key value map. Mutable method, modifies the argument nodePortMap.
     */
    boolean add(String node, String port, Map<String, Set<String>> nodePortMap) {

        if (node == null || port == null) {
            return false;
        }

        String nodeTrimmed = node.trim();
        String portTrimmed = port.trim();

        if (nodeTrimmed.isEmpty() || portTrimmed.isEmpty()) {
            return false;
        }

        if (!portTrimmed.matches(portNamePattern)) {
            LOG.warn("Preferred port name '{}' on node {} doesn't match pattern '{}'",
                portTrimmed,
                nodeTrimmed,
                portNamePattern
            );
        }

        if (nodePortMap.containsKey(nodeTrimmed)) {
            boolean added = nodePortMap.get(nodeTrimmed).add(portTrimmed);
            if (added) {
                LOG.debug("Preferred port '{}' for node '{}' registered.", portTrimmed, nodeTrimmed);
            } else {
                LOG.debug("Failed registering port '{}' for node '{}'.", portTrimmed, nodeTrimmed);
            }
            return added;
        }

        nodePortMap.put(nodeTrimmed, new HashSet<>(Arrays.asList(portTrimmed)));

        return true;
    }
}
