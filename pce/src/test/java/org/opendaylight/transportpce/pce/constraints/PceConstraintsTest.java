/*
 * Copyright © 2020 Orange, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.pce.constraints;

import java.util.ArrayList;
import java.util.List;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.transportpce.pce.networkanalyzer.PceOpticalNode;
import org.opendaylight.transportpce.test.AbstractTest;

public class PceConstraintsTest extends AbstractTest {
    private static PceConstraints pceConstraints = new PceConstraints();

    @Before
    public void setup() {
        pceConstraints = new PceConstraints();
    }

    @Test
    public void setAndGetMaxLatencyTest() {
        Assert.assertEquals(-1, this.pceConstraints.getMaxLatency().intValue());
        pceConstraints.setMaxLatency(-2L);
        Assert.assertEquals(-2, this.pceConstraints.getMaxLatency().intValue());
    }

    @Test
    public void setAndGetExcludeSupNodesTest() {
        Assert.assertEquals(0, this.pceConstraints.getExcludeSupNodes().size());
        List<String> nodes = new ArrayList<>();
        nodes.add("test");
        this.pceConstraints.setExcludeSupNodes(nodes);
        Assert.assertEquals(1, this.pceConstraints.getExcludeSupNodes().size());
    }

    @Test
    public void setAndGetExcludeSRLGTest() {
        Assert.assertEquals(0, this.pceConstraints.getExcludeSRLG().size());
        List<Long> nodes = new ArrayList<>();
        nodes.add(1L);
        this.pceConstraints.setExcludeSRLG(nodes);
        Assert.assertEquals(1, this.pceConstraints.getExcludeSRLG().size());
    }

    @Test
    public void setAndGetExcludeCLLITest() {
        Assert.assertEquals(0, this.pceConstraints.getExcludeCLLI().size());
        List<String> nodes = new ArrayList<>();
        nodes.add("test");
        this.pceConstraints.setExcludeCLLI(nodes);
        Assert.assertEquals(1, this.pceConstraints.getExcludeCLLI().size());
    }

    @Test
    public void setAndGetExcludeClliNodesTest() {
        Assert.assertEquals(0, this.pceConstraints.getExcludeClliNodes().size());
        List<String> nodes = new ArrayList<>();
        nodes.add("test");
        this.pceConstraints.setExcludeClliNodes(nodes);
        Assert.assertEquals(1, this.pceConstraints.getExcludeClliNodes().size());
    }

    @Test
    public void setAndGetExcludeSrlgLinksTest() {
        Assert.assertEquals(0, this.pceConstraints.getExcludeSrlgLinks().size());
        List<String> nodes = new ArrayList<>();
        nodes.add("test");
        this.pceConstraints.setExcludeSrlgLinks(nodes);
        Assert.assertEquals(1, this.pceConstraints.getExcludeSrlgLinks().size());
    }

    @Test
    public void setAndGetExcludeNodesTest() {
        Assert.assertEquals(0, this.pceConstraints.getExcludeNodes().size());
        List<String> nodes = new ArrayList<>();
        nodes.add("test");
        this.pceConstraints.setExcludeNodes(nodes);
        Assert.assertEquals(1, this.pceConstraints.getExcludeNodes().size());
    }

    @Test
    public void setAndGetIncludeNodesTest() {
        Assert.assertEquals(0, this.pceConstraints.getIncludeNodes().size());
        List<String> nodes = new ArrayList<>();
        nodes.add("test");
        this.pceConstraints.setIncludeNodes(nodes);
        Assert.assertEquals(1, this.pceConstraints.getIncludeNodes().size());
    }

    @Test
    public void getTypeAndNameOfResourcePairTest() {
        PceConstraints.ResourcePair resourcePair = new PceConstraints
                .ResourcePair(PceConstraints.ResourceType.CLLI, "test");
        Assert.assertEquals(resourcePair.getType(), PceConstraints.ResourceType.CLLI);
        Assert.assertEquals("test", resourcePair.getName());

    }


    @Test
    public void getIncludePceNodesTest() {
        Assert.assertTrue(pceConstraints.getIncludePceNodes().size() == 0);
        pceConstraints.setIncludePceNode(new PceOpticalNode(null, null, null, null, null, null, null, null));
        Assert.assertTrue(pceConstraints.getIncludePceNodes().size() == 1);

    }

    @Test
    public void getListToIncludeTest() {
        Assert.assertTrue(pceConstraints.getListToInclude().size() == 0);
        PceConstraints.ResourcePair resourcePair = new PceConstraints
                .ResourcePair(PceConstraints.ResourceType.SRLG, "test");
        pceConstraints.setListToInclude(resourcePair);
        Assert.assertTrue(pceConstraints.getListToInclude().size() == 1);
        Assert.assertTrue(pceConstraints.getSRLGnames().size() == 1);

    }

}


