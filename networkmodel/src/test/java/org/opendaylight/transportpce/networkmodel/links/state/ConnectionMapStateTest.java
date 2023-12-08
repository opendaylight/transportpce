/*
 * Copyright © 2026 Smartoptics and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.networkmodel.links.state;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.Set;
import org.junit.jupiter.api.Test;
import org.opendaylight.transportpce.networkmodel.links.ConnectionMap;
import org.opendaylight.transportpce.networkmodel.links.InterfaceLink;
import org.opendaylight.transportpce.networkmodel.links.Link;
import org.opendaylight.transportpce.networkmodel.links.strategy.AllDirectionsExists;

class ConnectionMapStateTest {

    @Test
    void fallsBackToAllDirectionsExistsWhenNoLinksAreAvailable() {
        ConnectionMap deviceConnectionMap = new AllDirectionsExists();
        State state = new ConnectionMapState(deviceConnectionMap);

        ConnectionMap result = state.connectionMap(Set.of());

        assertInstanceOf(AllDirectionsExists.class, result);
    }

    @Test
    void usesTheGivenConnectionMapWhenLinksAreAvailable() {
        ConnectionMap deviceConnectionMap = new AllDirectionsExists();
        State state = new ConnectionMapState(deviceConnectionMap);

        Link link = new InterfaceLink("ROADM-B-DEG1", "ROADM-B-SRG3");
        ConnectionMap result = state.connectionMap(Set.of(link));

        assertSame(deviceConnectionMap, result);
    }
}
