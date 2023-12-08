/*
 * Copyright © 2023  AT&T and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.networkmodel.links;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.slf4j.Logger;

class LinkMapFactoryTest {

    Logger logger;

    @BeforeEach
    void setUp() {
        logger = Mockito.mock(Logger.class);
    }

    @Test
    void connections() {

        Map<String, String> translation = new HashMap<>();
        translation.put("1/WSS", "ROADM-B-DEG1");
        translation.put("1/XC2", "ROADM-B-SRG3");
        translation.put("2/AWG", "ROADM-B-SRG10");
        translation.put("1/XC3", "ROADM-B-SRG4");
        translation.put("1/AWG", "ROADM-B-SRG1");
        translation.put("2/XC3", "ROADM-B-SRG13");
        translation.put("2/WSS", "ROADM-B-DEG2");
        translation.put("2/XC2", "ROADM-B-SRG12");

        Map<String, Set<String>> connections = new HashMap<>();
        connections.put("1/WSS", Set.of("1/XC2", "1/XC3", "1/AWG", "2/WSS"));
        connections.put("1/XC2", Set.of("1/WSS"));
        connections.put("2/AWG", Set.of("2/WSS"));
        connections.put("1/XC3", Set.of("1/WSS"));
        connections.put("1/AWG", Set.of("1/WSS"));
        connections.put("2/XC3", Set.of("2/WSS"));
        connections.put("2/XC2", Set.of("2/WSS"));
        connections.put("2/WSS", Set.of("1/WSS", "2/AWG", "2/XC3", "2/XC2"));

        Set<Link> expected = new HashSet<>();
        expected.add(new InterfaceLink("ROADM-B-SRG10", "ROADM-B-DEG2"));
        expected.add(new InterfaceLink("ROADM-B-DEG1",  "ROADM-B-SRG4"));
        expected.add(new InterfaceLink("ROADM-B-DEG2",  "ROADM-B-SRG13"));
        expected.add(new InterfaceLink("ROADM-B-DEG1",  "ROADM-B-SRG3"));
        expected.add(new InterfaceLink("ROADM-B-SRG13", "ROADM-B-DEG2"));
        expected.add(new InterfaceLink("ROADM-B-DEG1",  "ROADM-B-SRG1"));
        expected.add(new InterfaceLink("ROADM-B-DEG2",  "ROADM-B-SRG10"));
        expected.add(new InterfaceLink("ROADM-B-DEG2",  "ROADM-B-SRG12"));
        expected.add(new InterfaceLink("ROADM-B-SRG4",  "ROADM-B-DEG1"));
        expected.add(new InterfaceLink("ROADM-B-DEG1",  "ROADM-B-DEG2"));
        expected.add(new InterfaceLink("ROADM-B-SRG3",  "ROADM-B-DEG1"));
        expected.add(new InterfaceLink("ROADM-B-DEG2",  "ROADM-B-DEG1"));
        expected.add(new InterfaceLink("ROADM-B-SRG1",  "ROADM-B-DEG1"));
        expected.add(new InterfaceLink("ROADM-B-SRG12", "ROADM-B-DEG2"));

        Factory degreeFactory = new LinkMapFactory(logger);

        Set<Link> result = degreeFactory.connections(connections, translation);

        Assertions.assertEquals(result, expected);

    }

    @Test
    void connectionsAsString() {

        Map<String, String> translation = new HashMap<>();
        translation.put("1/WSS", "ROADM-B-DEG1");
        translation.put("1/XC2", "ROADM-B-SRG3");
        translation.put("2/AWG", "ROADM-B-SRG10");
        translation.put("1/XC3", "ROADM-B-SRG4");
        translation.put("1/AWG", "ROADM-B-SRG1");
        translation.put("2/XC3", "ROADM-B-SRG13");
        translation.put("2/WSS", "ROADM-B-DEG2");
        translation.put("2/XC2", "ROADM-B-SRG12");

        Map<String, Set<String>> connections = new HashMap<>();
        connections.put("1/WSS", Set.of("1/XC2", "1/XC3", "1/AWG", "2/WSS"));
        connections.put("1/XC2", Set.of("1/WSS"));
        connections.put("2/AWG", Set.of("2/WSS"));
        connections.put("1/XC3", Set.of("1/WSS"));
        connections.put("1/AWG", Set.of("1/WSS"));
        connections.put("2/XC3", Set.of("2/WSS"));
        connections.put("2/XC2", Set.of("2/WSS"));
        connections.put("2/WSS", Set.of("1/WSS", "2/AWG", "2/XC3", "2/XC2"));


        Factory linkMapFactory = new LinkMapFactory(logger);

        Set<Link> result = linkMapFactory.connections(connections, translation);

        String str = linkMapFactory.connections(result);
        Assertions.assertNotNull(str);
        Assertions.assertFalse(str.isEmpty());

    }
}