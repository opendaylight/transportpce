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

/**
 * Factory class that creates a mock {@code Map<NodeId, PceNode>} for use in
 * unit tests. Each {@link PceNode} is mocked using Mockito and initialized with
 * predefined parameters such as frequency granularity, slot width, and slot limits.
 *
 * <p>This factory is primarily intended for use with {@link PostAlgoPathValidatorTest}</p>
 */
public class MockPceNodeMapFactory {

    private static final String DEVICE_VERSION =
            "(http://org/openroadm/device?revision=2018-10-19)org-openroadm-device";

    /**
     * Protected constructor needed to prevent Checkstyle error:
     * {@code Utility classes should not have a public or default constructor. [HideUtilityClassConstructor]}.
     */
    protected MockPceNodeMapFactory() {
    }

    /**
     * Creates a map of mocked {@link PceNode} instances keyed by their {@link NodeId}.
     *
     * @return a {@code Map<NodeId, PceNode>} containing mocked ROADM nodes.
     */
    public static Map<NodeId, PceNode> createMockPceNodeMap() {
        Map<NodeId, PceNode> allPceNodes = new HashMap<>();

        allPceNodes.put(new NodeId("ROADM-A-DEG1"),
                createMockNode("ROADM-A-DEG1", 6.25, 12.5, 3, 16));

        allPceNodes.put(new NodeId("ROADM-A-SRG4"),
                createMockNode("ROADM-A-SRG4", 6.25, 12.5, 3, 16));

        allPceNodes.put(new NodeId("ROADM-C-SRG12"),
                createMockNode("ROADM-C-SRG12", 50.0, 50.0, 1, 1));

        allPceNodes.put(new NodeId("ROADM-B-SRG3"),
                createMockNode("ROADM-B-SRG3", 6.25, 12.5, 3, 16));

        allPceNodes.put(new NodeId("ROADM-C-DEG2"),
                createMockNode("ROADM-C-DEG2", 6.25, 12.5, 1, 20));

        allPceNodes.put(new NodeId("ROADM-B-SRG13"),
                createMockNode("ROADM-B-SRG13", 6.25, 12.5, 1, 20));

        allPceNodes.put(new NodeId("ROADM-B-SRG12"),
                createMockNode("ROADM-B-SRG12", 100.0, 12.5, 1, 4));

        allPceNodes.put(new NodeId("ROADM-C-SRG13"),
                createMockNode("ROADM-C-SRG13", 6.25, 12.5, 1, 20));

        allPceNodes.put(new NodeId("ROADM-B-DEG1"),
                createMockNode("ROADM-B-DEG1", 100.0, 12.5, 3, 16));

        allPceNodes.put(new NodeId("ROADM-B-DEG2"),
                createMockNode("ROADM-B-DEG2", 75.0, 12.5, 4, 8));

        return allPceNodes;
    }

    /**
     * Creates a mocked {@link PceNode} wired up to return the given properties.
     *
     * @param nodeName the unique name (and NodeId) of the node
     * @param centralFreqGranularity the central frequency granularity (in GHz)
     * @param slotWidthGranularity the slot width granularity (in GHz)
     * @param minSlots the minimum slot count supported
     * @param maxSlots the maximum slot count supported
     * @return a configured mocked {@link PceNode} instance
     */
    private static PceNode createMockNode(
            String nodeName,
            double centralFreqGranularity,
            double slotWidthGranularity,
            int minSlots,
            int maxSlots) {

        NodeId id = new NodeId(nodeName);
        PceNode node = mock(PceNode.class);

        when(node.getNodeId()).thenReturn(id);
        when(node.isContentionLessSrg()).thenReturn(false);
        when(node.getBitSetData()).thenReturn(range(0, 772));
        when(node.getCentralFreqGranularity()).thenReturn(BigDecimal.valueOf(centralFreqGranularity));
        when(node.getSlotWidthGranularity()).thenReturn(BigDecimal.valueOf(slotWidthGranularity));
        when(node.getMinSlots()).thenReturn(minSlots);
        when(node.getMaxSlots()).thenReturn(maxSlots);
        when(node.getVersion()).thenReturn(DEVICE_VERSION);

        return node;
    }

    /**
     * Creates a {@link BitSet} with all bits between {@code start} (inclusive)
     * and {@code end} (exclusive) set to true.
     *
     * <p>The nr of bits is assumed to be equal to {@code end}.</p>
     *
     * @param start the starting index (inclusive)
     * @param end   the ending index (exclusive)
     * @return a {@link BitSet} with bits set in the specified range
     */
    private static BitSet range(int start, int end) {
        BitSet bitSet = new BitSet(end);
        bitSet.set(start, end);
        return bitSet;
    }
}
