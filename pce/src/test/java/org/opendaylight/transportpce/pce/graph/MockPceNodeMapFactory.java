/*
 * Copyright © 2025 Smartoptics and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.pce.graph;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.BitSet;
import java.util.HashMap;
import java.util.Map;
import org.opendaylight.transportpce.pce.networkanalyzer.PceNode;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.NodeId;

public class MockPceNodeMapFactory {

    protected MockPceNodeMapFactory() {
    }

    public static Map<NodeId, PceNode> createMockPceNodeMap() {
        Map<NodeId, PceNode> allPceNodes = new HashMap<>();

        // Helper lambda to build BitSet from integer ranges
        java.util.function.Function<int[], BitSet> bits = (ints) -> {
            BitSet bs = new BitSet();
            for (int i : ints) {
                bs.set(i);
            }
            return bs;
        };

        // -- ROADM-A-DEG1 -------------------------------------------------
        {
            NodeId id = new NodeId("ROADM-A-DEG1");
            PceNode node = mock(PceNode.class);

            when(node.getNodeId()).thenReturn(id);
            when(node.isContentionLessSrg()).thenReturn(false);
            when(node.getBitSetData()).thenReturn(bits.apply(range(0, 772)));
            when(node.getCentralFreqGranularity()).thenReturn(BigDecimal.valueOf(6.25));
            when(node.getSlotWidthGranularity()).thenReturn(BigDecimal.valueOf(12.5));
            when(node.getMinSlots()).thenReturn(3);
            when(node.getMaxSlots()).thenReturn(16);
            when(node.getVersion()).thenReturn("(http://org/openroadm/device?revision=2018-10-19)org-openroadm-device");

            allPceNodes.put(id, node);
        }

        // -- ROADM-A-SRG4 -------------------------------------------------
        {
            NodeId id = new NodeId("ROADM-A-SRG4");
            PceNode node = mock(PceNode.class);

            when(node.getNodeId()).thenReturn(id);
            when(node.isContentionLessSrg()).thenReturn(false);
            when(node.getBitSetData()).thenReturn(bits.apply(range(0, 772)));
            when(node.getCentralFreqGranularity()).thenReturn(BigDecimal.valueOf(6.25));
            when(node.getSlotWidthGranularity()).thenReturn(BigDecimal.valueOf(12.5));
            when(node.getMinSlots()).thenReturn(3);
            when(node.getMaxSlots()).thenReturn(16);
            when(node.getVersion()).thenReturn("(http://org/openroadm/device?revision=2018-10-19)org-openroadm-device");

            allPceNodes.put(id, node);
        }

        // -- ROADM-C-SRG12 -----------------------------------------------
        {
            NodeId id = new NodeId("ROADM-C-SRG12");
            PceNode node = mock(PceNode.class);

            when(node.getNodeId()).thenReturn(id);
            when(node.isContentionLessSrg()).thenReturn(false);
            when(node.getBitSetData()).thenReturn(bits.apply(range(0, 772)));
            when(node.getCentralFreqGranularity()).thenReturn(BigDecimal.valueOf(50.0));
            when(node.getSlotWidthGranularity()).thenReturn(BigDecimal.valueOf(50.0));
            when(node.getMinSlots()).thenReturn(1);
            when(node.getMaxSlots()).thenReturn(1);
            when(node.getVersion()).thenReturn("(http://org/openroadm/device?revision=2018-10-19)org-openroadm-device");

            allPceNodes.put(id, node);
        }

        // -- ROADM-B-SRG3 -------------------------------------------------
        {
            NodeId id = new NodeId("ROADM-B-SRG3");
            PceNode node = mock(PceNode.class);

            when(node.getNodeId()).thenReturn(id);
            when(node.isContentionLessSrg()).thenReturn(false);
            when(node.getBitSetData()).thenReturn(bits.apply(range(0, 772)));
            when(node.getCentralFreqGranularity()).thenReturn(BigDecimal.valueOf(6.25));
            when(node.getSlotWidthGranularity()).thenReturn(BigDecimal.valueOf(12.5));
            when(node.getMinSlots()).thenReturn(3);
            when(node.getMaxSlots()).thenReturn(16);
            when(node.getVersion()).thenReturn("(http://org/openroadm/device?revision=2018-10-19)org-openroadm-device");

            allPceNodes.put(id, node);
        }

        // -- ROADM-C-DEG2 -------------------------------------------------
        {
            NodeId id = new NodeId("ROADM-C-DEG2");
            PceNode node = mock(PceNode.class);

            when(node.getNodeId()).thenReturn(id);
            when(node.isContentionLessSrg()).thenReturn(false);
            when(node.getBitSetData()).thenReturn(bits.apply(range(0, 772)));
            when(node.getCentralFreqGranularity()).thenReturn(BigDecimal.valueOf(6.25));
            when(node.getSlotWidthGranularity()).thenReturn(BigDecimal.valueOf(12.5));
            when(node.getMinSlots()).thenReturn(1);
            when(node.getMaxSlots()).thenReturn(20);
            when(node.getVersion()).thenReturn("(http://org/openroadm/device?revision=2018-10-19)org-openroadm-device");

            allPceNodes.put(id, node);
        }

        // -- ROADM-B-SRG13 -----------------------------------------------
        {
            NodeId id = new NodeId("ROADM-B-SRG13");
            PceNode node = mock(PceNode.class);

            when(node.getNodeId()).thenReturn(id);
            when(node.isContentionLessSrg()).thenReturn(false);
            when(node.getBitSetData()).thenReturn(bits.apply(range(0, 772)));
            when(node.getCentralFreqGranularity()).thenReturn(BigDecimal.valueOf(6.25));
            when(node.getSlotWidthGranularity()).thenReturn(BigDecimal.valueOf(12.5));
            when(node.getMinSlots()).thenReturn(1);
            when(node.getMaxSlots()).thenReturn(20);
            when(node.getVersion()).thenReturn("(http://org/openroadm/device?revision=2018-10-19)org-openroadm-device");

            allPceNodes.put(id, node);
        }

        // -- ROADM-B-SRG12 -----------------------------------------------
        {
            NodeId id = new NodeId("ROADM-B-SRG12");
            PceNode node = mock(PceNode.class);

            when(node.getNodeId()).thenReturn(id);
            when(node.isContentionLessSrg()).thenReturn(false);
            when(node.getBitSetData()).thenReturn(bits.apply(range(0, 772)));
            when(node.getCentralFreqGranularity()).thenReturn(BigDecimal.valueOf(100.0));
            when(node.getSlotWidthGranularity()).thenReturn(BigDecimal.valueOf(12.5));
            when(node.getMinSlots()).thenReturn(1);
            when(node.getMaxSlots()).thenReturn(4);
            when(node.getVersion()).thenReturn("(http://org/openroadm/device?revision=2018-10-19)org-openroadm-device");

            allPceNodes.put(id, node);
        }

        // -- ROADM-C-SRG13 -----------------------------------------------
        {
            NodeId id = new NodeId("ROADM-C-SRG13");
            PceNode node = mock(PceNode.class);

            when(node.getNodeId()).thenReturn(id);
            when(node.isContentionLessSrg()).thenReturn(false);
            when(node.getBitSetData()).thenReturn(bits.apply(range(0, 772)));
            when(node.getCentralFreqGranularity()).thenReturn(BigDecimal.valueOf(6.25));
            when(node.getSlotWidthGranularity()).thenReturn(BigDecimal.valueOf(12.5));
            when(node.getMinSlots()).thenReturn(1);
            when(node.getMaxSlots()).thenReturn(20);
            when(node.getVersion()).thenReturn("(http://org/openroadm/device?revision=2018-10-19)org-openroadm-device");

            allPceNodes.put(id, node);
        }

        // -- ROADM-B-DEG1 -------------------------------------------------
        {
            NodeId id = new NodeId("ROADM-B-DEG1");
            PceNode node = mock(PceNode.class);

            when(node.getNodeId()).thenReturn(id);
            when(node.isContentionLessSrg()).thenReturn(false);
            when(node.getBitSetData()).thenReturn(bits.apply(range(0, 772)));
            when(node.getCentralFreqGranularity()).thenReturn(BigDecimal.valueOf(100.0));
            when(node.getSlotWidthGranularity()).thenReturn(BigDecimal.valueOf(12.5));
            when(node.getMinSlots()).thenReturn(3);
            when(node.getMaxSlots()).thenReturn(16);
            when(node.getVersion()).thenReturn("(http://org/openroadm/device?revision=2018-10-19)org-openroadm-device");

            allPceNodes.put(id, node);
        }

        // -- ROADM-B-DEG2 -------------------------------------------------
        {
            NodeId id = new NodeId("ROADM-B-DEG2");
            PceNode node = mock(PceNode.class);

            when(node.getNodeId()).thenReturn(id);
            when(node.isContentionLessSrg()).thenReturn(false);
            when(node.getBitSetData()).thenReturn(bits.apply(range(0, 772)));
            when(node.getCentralFreqGranularity()).thenReturn(BigDecimal.valueOf(75.0));
            when(node.getSlotWidthGranularity()).thenReturn(BigDecimal.valueOf(12.5));
            when(node.getMinSlots()).thenReturn(4);
            when(node.getMaxSlots()).thenReturn(8);
            when(node.getVersion()).thenReturn("(http://org/openroadm/device?revision=2018-10-19)org-openroadm-device");

            allPceNodes.put(id, node);
        }

        return allPceNodes;
    }

    // Utility to generate integer ranges (inclusive of start, exclusive of end)
    private static int[] range(int start, int end) {
        int[] range = new int[end - start];
        for (int i = 0; i < range.length; i++) {
            range[i] = start + i;
        }
        return range;
    }
}
