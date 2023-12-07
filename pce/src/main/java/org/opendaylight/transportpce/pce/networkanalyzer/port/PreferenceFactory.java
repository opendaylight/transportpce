/*
 * Copyright (c) 2023 Orange, Inc. and others.  All rights reserved.
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

    private final String portNamePattern = "(?i)SRG\\d+-PP\\d+-TXRX";

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

    @Override
    public Map<String, Set<String>> nodePortMap(PathComputationRequestInput pathComputationRequestInput) {

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

    @Override
    public boolean add(String node, String portName, Map<String, Set<String>> nodePortMap) {
        if (
            !node.isEmpty()
                && !node.trim().equalsIgnoreCase("N/A")
                && !portName.isEmpty()
                && !portName.trim().equalsIgnoreCase("N/A")
        ) {
            if (!portName.matches(portNamePattern)) {
                LOG.warn("Preferred port name '{}' on node {} doesn't match pattern '{}'",
                         portName,
                         node,
                         portNamePattern
                );
            }

            LOG.debug("Preferred port '{}' for node '{}' registered.", portName, node);
            if (nodePortMap.containsKey(node)) {
                return nodePortMap.get(node).add(portName);
            }

            nodePortMap.put(node, new HashSet<>(Arrays.asList(portName)));
            return true;

        }

        return false;
    }

}
