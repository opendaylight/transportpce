/*
 * Copyright © 2020 Orange, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.pce.constraints;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opendaylight.transportpce.pce.networkanalyzer.PceOpticalNode;
import org.opendaylight.transportpce.test.AbstractTest;

public class PceConstraintsTest extends AbstractTest {
    private static PceConstraints pceConstraints = new PceConstraints();

    @BeforeEach
    void setup() {
        pceConstraints = new PceConstraints();
    }

    @Test
    void setAndGetMaxLatencyTest() {
        assertEquals(-1, pceConstraints.getMaxLatency().intValue());
        pceConstraints.setMaxLatency(-2L);
        assertEquals(-2, pceConstraints.getMaxLatency().intValue());
    }

    @Test
    void setAndGetExcludeSupNodesTest() {
        assertEquals(0, pceConstraints.getExcludeSupNodes().size());
        List<String> nodes = new ArrayList<>();
        nodes.add("test");
        pceConstraints.setExcludeSupNodes(nodes);
        assertEquals(1, pceConstraints.getExcludeSupNodes().size());
    }

    @Test
    void setAndGetExcludeSRLGTest() {
        assertEquals(0, pceConstraints.getExcludeSRLG().size());
        List<Long> nodes = new ArrayList<>();
        nodes.add(1L);
        pceConstraints.setExcludeSRLG(nodes);
        assertEquals(1, pceConstraints.getExcludeSRLG().size());
    }

    @Test
    void setAndGetExcludeCLLITest() {
        assertEquals(0, pceConstraints.getExcludeCLLI().size());
        List<String> nodes = new ArrayList<>();
        nodes.add("test");
        pceConstraints.setExcludeCLLI(nodes);
        assertEquals(1, pceConstraints.getExcludeCLLI().size());
    }

    @Test
    void setAndGetExcludeClliNodesTest() {
        assertEquals(0, pceConstraints.getExcludeClliNodes().size());
        List<String> nodes = new ArrayList<>();
        nodes.add("test");
        pceConstraints.setExcludeClliNodes(nodes);
        assertEquals(1, pceConstraints.getExcludeClliNodes().size());
    }

    @Test
    void setAndGetExcludeSrlgLinksTest() {
        assertEquals(0, pceConstraints.getExcludeSrlgLinks().size());
        List<String> nodes = new ArrayList<>();
        nodes.add("test");
        pceConstraints.setExcludeSrlgLinks(nodes);
        assertEquals(1, pceConstraints.getExcludeSrlgLinks().size());
    }

    @Test
    void setAndGetExcludeNodesTest() {
        assertEquals(0, pceConstraints.getExcludeNodes().size());
        List<String> nodes = new ArrayList<>();
        nodes.add("test");
        pceConstraints.setExcludeNodes(nodes);
        assertEquals(1, pceConstraints.getExcludeNodes().size());
    }

    @Test
    void setAndGetIncludeNodesTest() {
        assertEquals(0, pceConstraints.getIncludeNodes().size());
        List<String> nodes = new ArrayList<>();
        nodes.add("test");
        pceConstraints.setIncludeNodes(nodes);
        assertEquals(1, pceConstraints.getIncludeNodes().size());
    }

    @Test
    void getTypeAndNameOfResourcePairTest() {
        PceConstraints.ResourcePair resourcePair = new PceConstraints
                .ResourcePair(PceConstraints.ResourceType.CLLI, "test");
        assertEquals(resourcePair.getType(), PceConstraints.ResourceType.CLLI);
        assertEquals("test", resourcePair.getName());
    }


    @Test
    void getIncludePceNodesTest() {
        assertTrue(pceConstraints.getIncludePceNodes().size() == 0);
        pceConstraints.setIncludePceNode(new PceOpticalNode(null, null, null, null, null, null, null));
        assertTrue(pceConstraints.getIncludePceNodes().size() == 1);
    }

    @Test
    void getListToIncludeTest() {
        assertTrue(pceConstraints.getListToInclude().size() == 0);
        PceConstraints.ResourcePair resourcePair = new PceConstraints
                .ResourcePair(PceConstraints.ResourceType.SRLG, "test");
        pceConstraints.setListToInclude(resourcePair);
        assertTrue(pceConstraints.getListToInclude().size() == 1);
        assertTrue(pceConstraints.getSRLGnames().size() == 1);
    }
}
