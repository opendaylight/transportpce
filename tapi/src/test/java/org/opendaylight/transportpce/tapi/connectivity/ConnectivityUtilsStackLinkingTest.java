/*
 * Copyright (c) 2026 OpenTDT and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.tapi.connectivity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.opendaylight.transportpce.tapi.TapiConstants;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.format.rev250530.ServiceFormat;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev250530.service.list.Services;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.Uuid;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev221121.connection.LowerConnection;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev221121.connectivity.context.Connection;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev221121.connectivity.context.ConnectionKey;

/**
 * Cross-layer linking of the TAPI connections of several OTN stacks (OTU, ODU on the OTU, 100GE on
 * the ODU, on muxponders) mapped together at initialization. Each ODU must ride on the iOTU of its own
 * stack, and each 100GE on the iODU and OTSi of its own stack, whatever order the services are mapped in.
 *
 * <p>The services are listed as a service list may return them: every OTU, then every ODU, then every
 * 100GE. Without supporting-service-name, nothing but the ports tells which stack a service belongs to.
 */
class ConnectivityUtilsStackLinkingTest extends AbstractInitialMappingTest {

    private static final List<String> STACKS = List.of("1", "3");

    @Test
    void eachStackRidesOnItsOwnLowerConnectionsWithoutSupportingServiceNames() throws Exception {
        mapStacksAndCheckLinking(false, "SPDR-SA1", "SPDR-SC1", "SPDR-SA1", "SPDR-SC1");
    }

    @Test
    void eachStackRidesOnItsOwnLowerConnectionsWithSupportingServiceNames() throws Exception {
        mapStacksAndCheckLinking(true, "SPDR-SA1", "SPDR-SC1", "SPDR-SA1", "SPDR-SC1");
    }

    @Test
    void upperLayersRunningTheOtherWayRideOnTheirOwnStack() throws Exception {
        // The lower connection is found whatever direction its top connection was created in.
        mapStacksAndCheckLinking(false, "SPDR-SC1", "SPDR-SA1", "SPDR-SC1", "SPDR-SA1");
    }

    @Test
    void ethernetRunningTheOtherWayThanItsOduRidesOnItsOwnStack() throws Exception {
        mapStacksAndCheckLinking(false, "SPDR-SA1", "SPDR-SC1", "SPDR-SC1", "SPDR-SA1");
    }

    private void mapStacksAndCheckLinking(boolean withSupportingNames, String oduSideA, String oduSideZ,
            String ethSideA, String ethSideZ) throws Exception {
        List<Services> services = new ArrayList<>();
        for (String xpdrNum : STACKS) {
            services.add(stackService(xpdrNum, "OTUC4", ServiceFormat.OTU, otuPath(xpdrNum), null,
                "SPDR-SA1", "SPDR-SC1"));
        }
        for (String xpdrNum : STACKS) {
            services.add(stackService(xpdrNum, "ODUC4", ServiceFormat.ODU, oduPath(xpdrNum, oduSideA, oduSideZ),
                withSupportingNames ? serviceName(xpdrNum, "OTUC4") : null, oduSideA, oduSideZ));
        }
        for (String xpdrNum : STACKS) {
            services.add(stackService(xpdrNum, "100GE", ServiceFormat.Ethernet, dsrPath(xpdrNum, ethSideA, ethSideZ),
                withSupportingNames ? serviceName(xpdrNum, "ODUC4") : null, ethSideA, ethSideZ));
        }

        assertTrue(storeAndMap(services));

        Map<ConnectionKey, Connection> connections = mappedConnections();
        for (String xpdrNum : STACKS) {
            final String other = STACKS.stream().filter(num -> !num.equals(xpdrNum)).findFirst().orElseThrow();
            Connection iodu = topConnection(connections, network(xpdrNum), TapiConstants.I_ODU);
            assertEquals(Set.of(uuidOf(topConnection(connections, network(xpdrNum), TapiConstants.I_OTU))),
                lowerTopConnections(iodu, connections, TapiConstants.I_OTU),
                "iODU of stack " + xpdrNum + " must ride on its own iOTU");

            Connection eodu = topConnection(connections, client(xpdrNum), TapiConstants.E_ODU);
            assertEquals(Set.of(uuidOf(iodu)), lowerTopConnections(eodu, connections, TapiConstants.I_ODU),
                "eODU of stack " + xpdrNum + " must ride on its own iODU");

            Connection dsr = topConnection(connections, client(xpdrNum), TapiConstants.DSR);
            Set<String> dsrOtsi = lowerTopConnections(dsr, connections, TapiConstants.OTSI_MC);
            assertTrue(dsrOtsi.contains(uuidOf(otsiTopConnection(connections, xpdrNum))),
                "DSR of stack " + xpdrNum + " must ride on its own OTSi");
            assertFalse(dsrOtsi.contains(uuidOf(otsiTopConnection(connections, other))),
                "DSR of stack " + xpdrNum + " must not ride on the OTSi of stack " + other);
        }
    }

    private static List<String> network(String xpdrNum) {
        return List.of("SPDR-SA1-XPDR" + xpdrNum + "+XPDR" + xpdrNum + "-NETWORK1",
            "SPDR-SC1-XPDR" + xpdrNum + "+XPDR" + xpdrNum + "-NETWORK1");
    }

    private static List<String> client(String xpdrNum) {
        return List.of("SPDR-SA1-XPDR" + xpdrNum + "+XPDR" + xpdrNum + "-CLIENT1",
            "SPDR-SC1-XPDR" + xpdrNum + "+XPDR" + xpdrNum + "-CLIENT1");
    }

    private static Connection topConnection(Map<ConnectionKey, Connection> connections, List<String> ends,
            String qual) {
        // In whichever direction it was created.
        List<Connection> found = new ArrayList<>();
        for (String name : List.of(String.join("+", "TOP", ends.get(0), ends.get(1), qual),
                String.join("+", "TOP", ends.get(1), ends.get(0), qual))) {
            Connection conn = connections.get(new ConnectionKey(new Uuid(
                UUID.nameUUIDFromBytes(name.getBytes(StandardCharsets.UTF_8)).toString())));
            if (conn != null) {
                found.add(conn);
            }
        }
        assertEquals(1, found.size(), "top connection " + qual + " " + ends + " among " + names(connections));
        return found.get(0);
    }

    private static Connection otsiTopConnection(Map<ConnectionKey, Connection> connections, String xpdrNum) {
        // Its name carries the spectrum: TOP+<A network port>-[n-m]+<Z network port>-[n-m]+OTSi_MC.
        List<String> ends = network(xpdrNum);
        List<Connection> found = connections.values().stream()
            .filter(conn -> isTopConnection(conn, TapiConstants.OTSI_MC)
                && nameOf(conn).contains(ends.get(0) + "-[") && nameOf(conn).contains(ends.get(1) + "-["))
            .collect(Collectors.toList());
        assertEquals(1, found.size(), "OTSi of stack " + xpdrNum + " among " + names(connections));
        return found.get(0);
    }

    private static Set<String> lowerTopConnections(Connection conn, Map<ConnectionKey, Connection> connections,
            String qual) {
        return conn.nonnullLowerConnection().values().stream()
            .map(LowerConnection::getConnectionUuid)
            .filter(uuid -> {
                Connection lower = connections.get(new ConnectionKey(uuid));
                return lower != null && isTopConnection(lower, qual);
            })
            .map(Uuid::getValue)
            .collect(Collectors.toSet());
    }

    private static boolean isTopConnection(Connection conn, String qual) {
        return nameOf(conn).startsWith("TOP+") && nameOf(conn).endsWith("+" + qual);
    }

    private static String uuidOf(Connection conn) {
        return conn.getUuid().getValue();
    }

    private static String nameOf(Connection conn) {
        return conn.nonnullName().values().iterator().next().getValue();
    }

    private static List<String> names(Map<ConnectionKey, Connection> connections) {
        return connections.values().stream().map(ConnectivityUtilsStackLinkingTest::nameOf).sorted()
            .collect(Collectors.toList());
    }
}
