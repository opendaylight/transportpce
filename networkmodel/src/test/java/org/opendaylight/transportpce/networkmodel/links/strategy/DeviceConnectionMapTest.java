/*
 * Copyright © 2026 Smartoptics and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.networkmodel.links.strategy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;
import org.junit.jupiter.api.Test;
import org.opendaylight.transportpce.networkmodel.links.ConnectionMap;
import org.opendaylight.transportpce.networkmodel.links.InterfaceLink;
import org.opendaylight.transportpce.networkmodel.links.Link;

class DeviceConnectionMapTest {

    @Test
    void containsReturnsTrueForALinkInTheMap() {
        Link link = new InterfaceLink("ROADM-B-DEG1", "ROADM-B-SRG3");
        ConnectionMap connectionMap = new DeviceConnectionMap(Set.of(link));

        assertTrue(connectionMap.contains("ROADM-B-DEG1", "ROADM-B-SRG3"));
    }

    @Test
    void containsReturnsFalseForALinkNotInTheMap() {
        Link link = new InterfaceLink("ROADM-B-DEG1", "ROADM-B-SRG3");
        ConnectionMap connectionMap = new DeviceConnectionMap(Set.of(link));

        assertFalse(connectionMap.contains("ROADM-B-DEG1", "ROADM-B-SRG1"));
    }

    @Test
    void containsIsDirectional() {
        Link link = new InterfaceLink("ROADM-B-DEG1", "ROADM-B-SRG3");
        ConnectionMap connectionMap = new DeviceConnectionMap(Set.of(link));

        assertFalse(connectionMap.contains("ROADM-B-SRG3", "ROADM-B-DEG1"));
    }

    @Test
    void sizeReflectsTheNumberOfLinks() {
        Link link1 = new InterfaceLink("ROADM-B-DEG1", "ROADM-B-SRG3");
        Link link2 = new InterfaceLink("ROADM-B-DEG1", "ROADM-B-SRG1");
        ConnectionMap connectionMap = new DeviceConnectionMap(Set.of(link1, link2));

        assertEquals(2, connectionMap.size());
    }
}
