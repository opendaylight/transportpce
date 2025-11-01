/*
 * Copyright © 2024 Smartoptics and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.pce.graph;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.BitSet;
import java.util.List;
import java.util.Map;
import org.jgrapht.GraphPath;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.opendaylight.transportpce.common.device.observer.Subscriber;
import org.opendaylight.transportpce.common.network.NetworkTransactionService;
import org.opendaylight.transportpce.pce.frequency.interval.EntireSpectrum;
import org.opendaylight.transportpce.pce.input.ClientInput;
import org.opendaylight.transportpce.pce.networkanalyzer.PceLink;
import org.opendaylight.transportpce.pce.networkanalyzer.PceNode;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev240205.SpectrumAssignment;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev240205.SpectrumAssignmentBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.NodeId;
import org.opendaylight.yangtools.yang.common.Uint16;

class PostAlgoPathValidatorTest {

    private final Map<NodeId, PceNode> nodes;

    private final BitSet customerAvailableFrequencies = new BitSet();

    private final ClientInput clientInputMock;

    @Mock
    NetworkTransactionService networkTransactionService;

    PostAlgoPathValidatorTest() throws IOException {
        nodes = MockPceNodeMapFactory.createMockPceNodeMap();

        customerAvailableFrequencies.set(0, 768);

        clientInputMock = mock(ClientInput.class);
        when(clientInputMock.slotWidth(anyInt())).thenAnswer(input -> input.getArgument(0));
        when(clientInputMock.clientRangeWishListIntersection()).thenReturn(new EntireSpectrum(768));
        when(clientInputMock.clientRangeWishListSubset()).thenReturn(new EntireSpectrum(768));
    }

    @Test
    void computeBestSpectrumAssignmentFixGrid50CenterFreqGranularity() {
        PostAlgoPathValidator postAlgoPathValidator = new PostAlgoPathValidator(networkTransactionService, new BitSet(),
                mock(ClientInput.class));

        BitSet available = new BitSet(768);
        available.set(12, 28);

        boolean isFlexGrid = false;
        SpectrumAssignment expected = new SpectrumAssignmentBuilder()
                .setBeginIndex(Uint16.valueOf(16))
                .setStopIndex(Uint16.valueOf(23))
                .setFlexGrid(isFlexGrid)
                .build();

        SpectrumAssignment fixGrid = postAlgoPathValidator.computeBestSpectrumAssignment(
                available, 8, 8, isFlexGrid, mock(Subscriber.class));

        assertEquals(expected, fixGrid);
    }

    @Test
    void computeBestSpectrumAssignmentFlexGrid6point25CenterFreqGranularity() {
        PostAlgoPathValidator postAlgoPathValidator = new PostAlgoPathValidator(networkTransactionService, new BitSet(),
                mock(ClientInput.class));

        BitSet available = new BitSet(768);
        available.set(12, 28);

        boolean isFlexGrid = true;
        SpectrumAssignment expected = new SpectrumAssignmentBuilder()
                .setBeginIndex(Uint16.valueOf(20))
                .setStopIndex(Uint16.valueOf(27))
                .setFlexGrid(isFlexGrid)
                .build();

        SpectrumAssignment flexGrid = postAlgoPathValidator.computeBestSpectrumAssignment(
                available, 8, 1, isFlexGrid, mock(Subscriber.class));

        assertEquals(expected, flexGrid);
    }

    /**
     * Setting up a service of 100GHz should be possible given this path and the following mc capabilities.
     *
     * <p>Path: [(ROADM-A-SRG4 : ROADM-A-DEG1), (ROADM-A-DEG1 : ROADM-B-DEG1), (ROADM-B-DEG1 : ROADM-B-SRG3)]</p>
     * <pre>
     * {@code
     * ROADM-A-SRG4
     *     <slotWidthGranularityGHz>12.5
     *     <centerFrequencyGranularityGHz>6.25
     *     <minSlots>3
     *     <maxSlots>16
     * ROADM-A-DEG1
     *     <slotWidthGranularityGHz>12.5
     *     <centerFrequencyGranularityGHz>6.25
     *     <minSlots>3
     *     <maxSlots>16
     * ROADM-B-SRG3
     *     <slotWidthGranularityGHz>12.5
     *     <centerFrequencyGranularityGHz>6.25
     *     <minSlots>3
     *     <maxSlots>16
     * ROADM-B-DEG1
     *     <slotWidthGranularityGHz>12.5
     *     <centerFrequencyGranularityGHz>100.0
     *     <minSlots>3
     *     <maxSlots>16
     * }
     * </pre>
     * The above settings are found in the class MockPceNodeMapFactory.
     * @see MockPceNodeMapFactory
     */
    @Test
    void spectrumAssignmentRoadmA_SRG4_to_RoadmB_SRG13_100GHzSucceeds() {
        List<PceGraphEdge> edges = List.of(
                mockEdge("ROADM-A-SRG4", "ROADM-A-DEG1", "(ROADM-A-SRG4 : ROADM-A-DEG1)"),
                mockEdge("ROADM-A-DEG1", "ROADM-B-DEG1", "(ROADM-A-DEG1 : ROADM-B-DEG1)"),
                mockEdge("ROADM-B-DEG1", "ROADM-B-SRG3", "(ROADM-B-DEG1 : ROADM-B-SRG3)")
        );
        GraphPath<String, PceGraphEdge> path = mockGraphPath(edges, 3.0, 3);

        PostAlgoPathValidator postAlgoPathValidator = new PostAlgoPathValidator(networkTransactionService,
                customerAvailableFrequencies, clientInputMock);

        SpectrumAssignment expected = new SpectrumAssignmentBuilder()
                .setBeginIndex(Uint16.valueOf(740))
                .setStopIndex(Uint16.valueOf(755))
                .setFlexGrid(true)
                .build();

        assertEquals(expected, postAlgoPathValidator.getSpectrumAssignment(path, nodes, 16, mock(Subscriber.class)));
    }

    /**
     * Setting up a service of 37.5GHz should be possible given this path and the following mc capabilities.
     *
     * <p>Path: [(ROADM-A-SRG4 : ROADM-A-DEG1), (ROADM-A-DEG1 : ROADM-B-DEG1), (ROADM-B-DEG1 : ROADM-B-SRG3)]</p>
     * <pre>
     * {@code
     * ROADM-A-SRG4
     *     <slotWidthGranularityGHz>12.5
     *     <centerFrequencyGranularityGHz>6.25
     *     <minSlots>3
     *     <maxSlots>16
     * ROADM-A-DEG1
     *     <slotWidthGranularityGHz>12.5
     *     <centerFrequencyGranularityGHz>6.25
     *     <minSlots>3
     *     <maxSlots>16
     * ROADM-B-SRG3
     *     <slotWidthGranularityGHz>12.5
     *     <centerFrequencyGranularityGHz>6.25
     *     <minSlots>3
     *     <maxSlots>16
     * ROADM-B-DEG1
     *     <slotWidthGranularityGHz>12.5
     *     <centerFrequencyGranularityGHz>100.0
     *     <minSlots>3
     *     <maxSlots>16
     * }
     * </pre>
     * The above settings are found in the class MockPceNodeMapFactory.
     * @see MockPceNodeMapFactory
     */
    @Test
    void spectrumAssignmentRoadmA_SRG4_to_RoadmB_SRG13_37_5GHzSucceeds() {
        List<PceGraphEdge> edges = List.of(
                mockEdge("ROADM-A-SRG4", "ROADM-A-DEG1", "(ROADM-A-SRG4 : ROADM-A-DEG1)"),
                mockEdge("ROADM-A-DEG1", "ROADM-B-DEG1", "(ROADM-A-DEG1 : ROADM-B-DEG1)"),
                mockEdge("ROADM-B-DEG1", "ROADM-B-SRG3", "(ROADM-B-DEG1 : ROADM-B-SRG3)")
        );
        GraphPath<String, PceGraphEdge> path = mockGraphPath(edges, 3.0, 3);

        SpectrumAssignment expected = new SpectrumAssignmentBuilder()
                .setBeginIndex(Uint16.valueOf(761))
                .setStopIndex(Uint16.valueOf(766))
                .setFlexGrid(true)
                .build();

        PostAlgoPathValidator postAlgoPathValidator = new PostAlgoPathValidator(networkTransactionService,
                customerAvailableFrequencies, clientInputMock);

        assertEquals(expected, postAlgoPathValidator.getSpectrumAssignment(path, nodes, 6, mock(Subscriber.class)));
    }


    /**
     * Setting up a service of 37.5GHz should NOT be possible given this path and the following mc capabilities.
     * Note: ROADM-B-DEG2 is limited to 50 - 1000GHz.
     *
     * <p>Path: [(ROADM-A-SRG4 : ROADM-A-DEG1), (ROADM-A-DEG1 : ROADM-B-DEG1), (ROADM-B-DEG1 : ROADM-B-DEG2),
     *               (ROADM-B-DEG2 : ROADM-C-DEG2), ( ROADM-C-DEG2 :  ROADM-C-SRG22)]</p>
     * <pre>
     * {@code
     * ROADM-A-SRG4
     *     <slotWidthGranularityGHz>12.5
     *     <centerFrequencyGranularityGHz>6.25
     *     <minSlots>3
     *     <maxSlots>16
     * ROADM-A-DEG1
     *     <slotWidthGranularityGHz>12.5
     *     <centerFrequencyGranularityGHz>6.25
     *     <minSlots>3
     *     <maxSlots>16
     * ROADM-B-DEG1
     *     <frequenciesBitSet>0,1,2,3,...,34,35,52,53...
     *     <slotWidthGranularityGHz>12.5
     *     <centerFrequencyGranularityGHz>100.0
     *     <minSlots>3
     *     <maxSlots>16
     * ROADM-B-DEG2
     *     <slotWidthGranularityGHz>12.5
     *     <centerFrequencyGranularityGHz>75.0
     *     <minSlots>4
     *     <maxSlots>8
     *  ROADM-C-DEG2
     *     <slotWidthGranularityGHz>12.5
     *     <centerFrequencyGranularityGHz>6.25
     *     <minSlots>1
     *     <maxSlots>20
     *  ROADM-C-SRG12
     *     <slotWidthGranularityGHz>50.0
     *     <centerFrequencyGranularityGHz>50.0
     *     <minSlots>1
     *     <maxSlots>1
     * }
     * </pre>
     * The above settings are found in the class MockPceNodeMapFactory.
     * @see MockPceNodeMapFactory
     */
    @Test
    void spectrumAssignmentRoadmA_SRG4_to_RoadmC_SRG12_37_5GHzFails() {
        List<PceGraphEdge> edges = List.of(
                mockEdge("ROADM-A-SRG4", "ROADM-A-DEG1", "(ROADM-A-SRG4 : ROADM-A-DEG1)"),
                mockEdge("ROADM-A-DEG1", "ROADM-B-DEG1", "(ROADM-A-DEG1 : ROADM-B-DEG1)"),
                mockEdge("ROADM-B-DEG1", "ROADM-B-DEG2", "(ROADM-B-DEG1 : ROADM-B-DEG2)"),
                mockEdge("ROADM-B-DEG2", "ROADM-C-DEG2", "(ROADM-B-DEG2 : ROADM-C-DEG2)"),
                mockEdge("ROADM-C-DEG2", "ROADM-C-SRG12", "(ROADM-C-DEG2 : ROADM-C-SRG12)")
        );
        GraphPath<String, PceGraphEdge> path = mockGraphPath(edges, 5.0, 5);

        PostAlgoPathValidator postAlgoPathValidator = new PostAlgoPathValidator(networkTransactionService,
                customerAvailableFrequencies, clientInputMock);

        SpectrumAssignment expected = new SpectrumAssignmentBuilder()
                .setBeginIndex(Uint16.valueOf(0))
                .setStopIndex(Uint16.valueOf(0))
                .setFlexGrid(true)
                .build();

        assertEquals(expected, postAlgoPathValidator.getSpectrumAssignment(path, nodes, 6, mock(Subscriber.class)));
    }

    /**
     * Setting up a service of 50GHz should be possible given this path and the following mc capabilities.
     *
     * <p>Path: [(ROADM-A-SRG4 : ROADM-A-DEG1), (ROADM-A-DEG1 : ROADM-B-DEG1), (ROADM-B-DEG1 : ROADM-B-DEG2),
     *               (ROADM-B-DEG2 : ROADM-C-DEG2), ( ROADM-C-DEG2 :  ROADM-C-SRG22)]</p>
     * <pre>
     * {@code
     * ROADM-A-SRG4
     *     <slotWidthGranularityGHz>12.5
     *     <centerFrequencyGranularityGHz>6.25
     *     <minSlots>3
     *     <maxSlots>16
     * ROADM-A-DEG1
     *     <slotWidthGranularityGHz>12.5
     *     <centerFrequencyGranularityGHz>6.25
     *     <minSlots>3
     *     <maxSlots>16
     * ROADM-B-DEG1
     *     <frequenciesBitSet>0,1,2,3,...,34,35,52,53...
     *     <slotWidthGranularityGHz>12.5
     *     <centerFrequencyGranularityGHz>100.0
     *     <minSlots>3
     *     <maxSlots>16
     * ROADM-B-DEG2
     *     <slotWidthGranularityGHz>12.5
     *     <centerFrequencyGranularityGHz>75.0
     *     <minSlots>4
     *     <maxSlots>8
     * ROADM-C-DEG2
     *     <slotWidthGranularityGHz>12.5
     *     <centerFrequencyGranularityGHz>6.25
     *     <minSlots>1
     *     <maxSlots>20
     * ROADM-C-SRG12
     *     <slotWidthGranularityGHz>50.0
     *     <centerFrequencyGranularityGHz>50.0
     *     <minSlots>1
     *     <maxSlots>1
     * }
     * </pre>
     * The above settings are found in the class MockPceNodeMapFactory.
     * @see MockPceNodeMapFactory
     */
    @Test
    void spectrumAssignmentRoadmA_SRG4_to_RoadmC_SRG12_50GHzSucceeds() {
        List<PceGraphEdge> edges = List.of(
                mockEdge("ROADM-A-SRG4", "ROADM-A-DEG1", "(ROADM-A-SRG4 : ROADM-A-DEG1)"),
                mockEdge("ROADM-A-DEG1", "ROADM-B-DEG1", "(ROADM-A-DEG1 : ROADM-B-DEG1)"),
                mockEdge("ROADM-B-DEG1", "ROADM-B-DEG2", "(ROADM-B-DEG1 : ROADM-B-DEG2)"),
                mockEdge("ROADM-B-DEG2", "ROADM-C-DEG2", "(ROADM-B-DEG2 : ROADM-C-DEG2)"),
                mockEdge("ROADM-C-DEG2", "ROADM-C-SRG12", "(ROADM-C-DEG2 : ROADM-C-SRG12)")
        );
        GraphPath<String, PceGraphEdge> path = mockGraphPath(edges, 5.0, 5);

        PostAlgoPathValidator postAlgoPathValidator = new PostAlgoPathValidator(networkTransactionService,
                customerAvailableFrequencies, clientInputMock);

        //Possible center frequencies are [ 44, 92, 188, ... ], center frequency granularity = 300GHz.
        //Slot 36 - 51 is occupied on ROADM-B-DEG1, so first possible center frequency is 92 (well, between 91
        //and 92 really). A 50GHz service need 8 x 6.25GHz slots.
        SpectrumAssignment expected = new SpectrumAssignmentBuilder()
                .setBeginIndex(Uint16.valueOf(760))
                .setStopIndex(Uint16.valueOf(767))
                .setFlexGrid(true)
                .build();

        assertEquals(expected, postAlgoPathValidator.getSpectrumAssignment(path, nodes, 8, mock(Subscriber.class)));
    }

    /**
     * Setting up a service of 100GHz should NOT be possible given this path and the following mc capabilities.
     * Note: ROADM-C-SRG12 is limited to 50GHz.
     *
     * <p>Path: [(ROADM-A-SRG4 : ROADM-A-DEG1), (ROADM-A-DEG1 : ROADM-B-DEG1), (ROADM-B-DEG1 : ROADM-B-DEG2),
     *               (ROADM-B-DEG2 : ROADM-C-DEG2), ( ROADM-C-DEG2 :  ROADM-C-SRG22)]</p>
     * <pre>
     * {@code
     * ROADM-A-SRG4
     *     <slotWidthGranularityGHz>12.5
     *     <centerFrequencyGranularityGHz>6.25
     *     <minSlots>3
     *     <maxSlots>16
     * ROADM-A-DEG1
     *     <slotWidthGranularityGHz>12.5
     *     <centerFrequencyGranularityGHz>6.25
     *     <minSlots>3
     *     <maxSlots>16
     * ROADM-B-DEG1
     *     <slotWidthGranularityGHz>12.5
     *     <centerFrequencyGranularityGHz>100.0
     *     <minSlots>3
     *     <maxSlots>16
     * ROADM-B-DEG2
     *     <slotWidthGranularityGHz>12.5
     *     <centerFrequencyGranularityGHz>75.0
     *     <minSlots>4
     *     <maxSlots>8
     * ROADM-C-DEG2
     *     <slotWidthGranularityGHz>12.5
     *     <centerFrequencyGranularityGHz>6.25
     *     <minSlots>1
     *     <maxSlots>20
     * ROADM-C-SRG12
     *     <slotWidthGranularityGHz>50.0
     *     <centerFrequencyGranularityGHz>50.0
     *     <minSlots>1
     *     <maxSlots>1
     * }
     * </pre>
     * The above settings are found in the class MockPceNodeMapFactory.
     * @see MockPceNodeMapFactory
     */
    @Test
    void spectrumAssignmentRoadmA_SRG4_to_RoadmC_SRG12_100GHzFails() {
        List<PceGraphEdge> edges = List.of(
                mockEdge("ROADM-A-SRG4", "ROADM-A-DEG1", "(ROADM-A-SRG4 : ROADM-A-DEG1)"),
                mockEdge("ROADM-A-DEG1", "ROADM-B-DEG1", "(ROADM-A-DEG1 : ROADM-B-DEG1)"),
                mockEdge("ROADM-B-DEG1", "ROADM-B-DEG2", "(ROADM-B-DEG1 : ROADM-B-DEG2)"),
                mockEdge("ROADM-B-DEG2", "ROADM-C-DEG2", "(ROADM-B-DEG2 : ROADM-C-DEG2)"),
                mockEdge("ROADM-C-DEG2", "ROADM-C-SRG12", "(ROADM-C-DEG2 : ROADM-C-SRG12)")
        );
        GraphPath<String, PceGraphEdge> path = mockGraphPath(edges, 5.0, 5);

        PostAlgoPathValidator postAlgoPathValidator = new PostAlgoPathValidator(networkTransactionService,
                customerAvailableFrequencies, clientInputMock);

        //There is no available spectrum, i.e. begin index = stop index = 0.
        SpectrumAssignment expected = new SpectrumAssignmentBuilder()
                .setBeginIndex(Uint16.valueOf(0))
                .setStopIndex(Uint16.valueOf(0))
                .setFlexGrid(true)
                .build();

        assertEquals(expected, postAlgoPathValidator.getSpectrumAssignment(path, nodes, 16, mock(Subscriber.class)));
    }

    /**
     * Setting up a service of 62.5GHz should be possible given this path and the following mc capabilities.
     *
     * <p>Path: [(ROADM-B-SRG13 : ROADM-B-DEG2), (ROADM-B-DEG2 : ROADM-C-DEG2), (ROADM-C-DEG2 : ROADM-C-SRG13)]</p>
     *
     * <pre>
     * {@code
     * ROADM-B-SRG13
     *     <slotWidthGranularityGHz>12.5
     *     <centerFrequencyGranularityGHz>6.25
     *     <minSlots>1
     *     <maxSlots>20
     * ROADM-B-DEG2
     *     <slotWidthGranularityGHz>12.5
     *     <centerFrequencyGranularityGHz>75.0
     *     <minSlots>4
     *     <maxSlots>8
     * ROADM-C-DEG2
     *     <slotWidthGranularityGHz>12.5
     *     <centerFrequencyGranularityGHz>6.25
     *     <minSlots>1
     *     <maxSlots>20
     * ROADM-C-SRG13
     *     <slotWidthGranularityGHz>12.5
     *     <centerFrequencyGranularityGHz>6.25
     *     <minSlots>1
     *     <maxSlots>20
     * }
     * </pre>
     * The above settings are found in the class MockPceNodeMapFactory.
     * @see MockPceNodeMapFactory
     */
    @Test
    void spectrumAssignmentRoadmB_SRG13_to_RoadmC_SRG13_62_5_GHzSucceeds() {
        List<PceGraphEdge> edges = List.of(
                mockEdge("ROADM-B-SRG13", "ROADM-B-DEG2", "(ROADM-B-SRG13 : ROADM-B-DEG2)"),
                mockEdge("ROADM-B-DEG2", "ROADM-C-DEG2", "(ROADM-B-DEG2 : ROADM-C-DEG2)"),
                mockEdge("ROADM-C-DEG2", "ROADM-C-SRG13", "(ROADM-C-DEG2 : ROADM-C-SRG13)")
        );
        GraphPath<String, PceGraphEdge> path = mockGraphPath(edges, 3.0, 3);

        PostAlgoPathValidator postAlgoPathValidator = new PostAlgoPathValidator(networkTransactionService,
                customerAvailableFrequencies, clientInputMock);

        SpectrumAssignment expected = new SpectrumAssignmentBuilder()
                .setBeginIndex(Uint16.valueOf(747))
                .setStopIndex(Uint16.valueOf(756))
                .setFlexGrid(true)
                .build();

        assertEquals(expected, postAlgoPathValidator.getSpectrumAssignment(path, nodes, 10, mock(Subscriber.class)));
    }

    /**
     * Setting up a service of 62.5GHz should NOT be possible given this path and the following mc capabilities.
     * Note: ROADM-B-SRG12 is limited to 12.5 - 50GHz.
     *
     * <p>Path: [(ROADM-B-SRG12 : ROADM-B-DEG2), (ROADM-B-DEG2 : ROADM-C-DEG2), (ROADM-C-DEG2 : ROADM-C-SRG13)]</p>
     *
     * <pre>
     * {@code
     * ROADM-B-SRG12
     *     <slotWidthGranularityGHz>12.5
     *     <centerFrequencyGranularityGHz>100.0
     *     <minSlots>1
     *     <maxSlots>4
     * ROADM-B-DEG2
     *     <slotWidthGranularityGHz>12.5
     *     <centerFrequencyGranularityGHz>75.0
     *     <minSlots>4
     *     <maxSlots>8
     * ROADM-C-DEG2
     *     <slotWidthGranularityGHz>12.5
     *     <centerFrequencyGranularityGHz>6.25
     *     <minSlots>1
     *     <maxSlots>20
     * ROADM-C-SRG13
     *     <slotWidthGranularityGHz>12.5
     *     <centerFrequencyGranularityGHz>6.25
     *     <minSlots>1
     *     <maxSlots>20
     * }
     * </pre>
     * The above settings are found in the class MockPceNodeMapFactory.
     * @see MockPceNodeMapFactory
     */
    @Test
    void spectrumAssignmentRoadmB_SRG12_to_RoadmC_SRG13_62_5_GHzFails() {
        List<PceGraphEdge> edges = List.of(
                mockEdge("ROADM-B-SRG12", "ROADM-B-DEG2", "(ROADM-B-SRG12 : ROADM-B-DEG2)"),
                mockEdge("ROADM-B-DEG2", "ROADM-C-DEG2", "(ROADM-B-DEG2 : ROADM-C-DEG2)"),
                mockEdge("ROADM-C-DEG2", "ROADM-C-SRG13", "(ROADM-C-DEG2 : ROADM-C-SRG13)")
        );
        GraphPath<String, PceGraphEdge> path = mockGraphPath(edges, 3.0, 3);

        PostAlgoPathValidator postAlgoPathValidator = new PostAlgoPathValidator(networkTransactionService,
                customerAvailableFrequencies, clientInputMock);

        //There is no available spectrum, i.e. begin index = stop index = 0.
        SpectrumAssignment expected = new SpectrumAssignmentBuilder()
                .setBeginIndex(Uint16.valueOf(0))
                .setStopIndex(Uint16.valueOf(0))
                .setFlexGrid(true)
                .build();

        assertEquals(expected, postAlgoPathValidator.getSpectrumAssignment(path, nodes, 10, mock(Subscriber.class)));
    }

    private PceGraphEdge mockEdge(String sourceId, String destId, String edgeString) {
        PceLink link = mock(PceLink.class);
        when(link.getSourceId()).thenReturn(sourceId);
        when(link.getDestId()).thenReturn(destId);

        PceGraphEdge edge = mock(PceGraphEdge.class);
        when(edge.link()).thenReturn(link);
        when(edge.toString()).thenReturn(edgeString);

        return edge;
    }

    private GraphPath<String, PceGraphEdge> mockGraphPath(List<PceGraphEdge> edges, double weight, int length) {
        @SuppressWarnings("unchecked")
        GraphPath<String, PceGraphEdge> path = mock(GraphPath.class);

        when(path.getEdgeList()).thenReturn(edges);
        when(path.getWeight()).thenReturn(weight);
        when(path.getLength()).thenReturn(length);

        if (!edges.isEmpty()) {
            String startVertex = edges.getFirst().link().getSourceId();
            String endVertex = edges.getLast().link().getDestId();

            when(path.getStartVertex()).thenReturn(startVertex);
            when(path.getEndVertex()).thenReturn(endVertex);
        }

        return path;
    }
}
