/*
 * Copyright © 2024 Smartoptics and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.pce.graph;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.DomDriver;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.jgrapht.Graph;
import org.jgrapht.GraphPath;
import org.jgrapht.alg.shortestpath.PathValidator;
import org.jgrapht.alg.shortestpath.YenKShortestPath;
import org.jgrapht.graph.DefaultDirectedWeightedGraph;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.opendaylight.transportpce.common.device.observer.Subscriber;
import org.opendaylight.transportpce.common.network.NetworkTransactionService;
import org.opendaylight.transportpce.pce.frequency.interval.EntireSpectrum;
import org.opendaylight.transportpce.pce.input.ClientInput;
import org.opendaylight.transportpce.pce.networkanalyzer.PceNode;
import org.opendaylight.transportpce.pce.networkanalyzer.PceOpticalNode;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev240205.SpectrumAssignment;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev240205.SpectrumAssignmentBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.NodeId;
import org.opendaylight.yangtools.yang.common.Uint16;

class PostAlgoPathValidatorTest {

    private final Map<NodeId, PceNode> nodes;

    private final Graph<String, PceGraphEdge> weightedGraph;

    private final PathValidator<String, PceGraphEdge> wpv = new InAlgoPathValidator();

    private final BitSet customerAvailableFrequencies = new BitSet();

    private final ClientInput clientInputMock;

    PostAlgoPathValidatorTest() throws IOException {
        String nodesDirectory = "src/test/resources/topology/nodes";
        weightedGraph = this.weightedGraph("src/test/resources/topology/links", this.nodeIds(nodesDirectory));
        nodes = this.allPceNodes(nodesDirectory);

        customerAvailableFrequencies.set(0, 768);

        clientInputMock = Mockito.mock(ClientInput.class);
        Mockito.when(clientInputMock.slotWidth(Mockito.anyInt())).thenAnswer(input -> input.getArgument(0));
        Mockito.when(clientInputMock.clientRangeWishListIntersection()).thenReturn(new EntireSpectrum(768));
        Mockito.when(clientInputMock.clientRangeWishListSubset()).thenReturn(new EntireSpectrum(768));
    }

    @Test
    void computeBestSpectrumAssignmentFixGrid() {

        NetworkTransactionService networkTransactionService = Mockito.mock(NetworkTransactionService.class);
        PostAlgoPathValidator postAlgoPathValidator = new PostAlgoPathValidator(
            networkTransactionService,
            new BitSet(),
            Mockito.mock(ClientInput.class)
        );

        BitSet available = new BitSet(768);
        available.set(12, 28);

        boolean isFlexGrid = true;
        SpectrumAssignment expectedFixGrid = new SpectrumAssignmentBuilder()
            .setBeginIndex(Uint16.valueOf(16))
            .setStopIndex(Uint16.valueOf(23))
            .setFlexGrid(isFlexGrid)
            .build();

        SpectrumAssignment fixGrid = postAlgoPathValidator.computeBestSpectrumAssignment(
            available, 8, 8, isFlexGrid, Mockito.mock(Subscriber.class));

        Assertions.assertEquals(expectedFixGrid, fixGrid);


    }

    @Test
    void computeBestSpectrumAssignmentFlexGrid() {

        NetworkTransactionService networkTransactionService = Mockito.mock(NetworkTransactionService.class);
        PostAlgoPathValidator postAlgoPathValidator = new PostAlgoPathValidator(
            networkTransactionService,
            new BitSet(),
            Mockito.mock(ClientInput.class)
        );

        BitSet available = new BitSet(768);
        available.set(12, 28);

        boolean isFlexGrid = true;
        SpectrumAssignment expectedFlexGrid = new SpectrumAssignmentBuilder()
            .setBeginIndex(Uint16.valueOf(12))
            .setStopIndex(Uint16.valueOf(19))
            .setFlexGrid(isFlexGrid)
            .build();

        SpectrumAssignment flexGrid = postAlgoPathValidator.computeBestSpectrumAssignment(
            available, 8, 1, isFlexGrid, Mockito.mock(Subscriber.class));

        Assertions.assertEquals(expectedFlexGrid, flexGrid);

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
     * The above settings are found in the folder pce/src/test/resources/topology/nodes.
     */
    @Test
    void spectrumAssignmentRoadmA_SRG4_to_RoadmB_SRG13_100GHzSucceeds() throws IOException {

        YenKShortestPath<String, PceGraphEdge> swp = new YenKShortestPath<>(weightedGraph, wpv);

        List<GraphPath<String, PceGraphEdge>> weightedPathList = swp
            .getPaths("ROADM-A-SRG4", "ROADM-B-SRG3", 15);

        Map<Integer, GraphPath<String, PceGraphEdge>> allWPaths = IntStream
            .range(0, weightedPathList.size())
            .boxed()
            .collect(Collectors.toMap(Function.identity(), weightedPathList::get));

        Assertions.assertEquals(1, allWPaths.size());

        NetworkTransactionService networkTransactionService = Mockito.mock(NetworkTransactionService.class);

        PostAlgoPathValidator postAlgoPathValidator = new PostAlgoPathValidator(
            networkTransactionService,
            customerAvailableFrequencies,
            clientInputMock
        );

        GraphPath<String, PceGraphEdge> entry = allWPaths.get(0);

        SpectrumAssignment expected = new SpectrumAssignmentBuilder()
            .setBeginIndex(Uint16.valueOf(4))
            .setStopIndex(Uint16.valueOf(19))
            .setFlexGrid(true)
            .build();

        Assertions.assertEquals(
            expected,
            postAlgoPathValidator
                .getSpectrumAssignment(entry, nodes, 16, Mockito.mock(Subscriber.class))
        );

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
     * The above settings are found in the folder pce/src/test/resources/topology/nodes.
     */
    @Test
    void spectrumAssignmentRoadmA_SRG4_to_RoadmB_SRG13_37_5GHzSucceeds() throws IOException {

        YenKShortestPath<String, PceGraphEdge> swp = new YenKShortestPath<>(weightedGraph, wpv);

        List<GraphPath<String, PceGraphEdge>> weightedPathList = swp
            .getPaths("ROADM-A-SRG4", "ROADM-B-SRG3", 15);

        Map<Integer, GraphPath<String, PceGraphEdge>> allWPaths = IntStream
            .range(0, weightedPathList.size())
            .boxed()
            .collect(Collectors.toMap(Function.identity(), weightedPathList::get));

        Assertions.assertEquals(1, allWPaths.size());

        NetworkTransactionService networkTransactionService = Mockito.mock(NetworkTransactionService.class);

        PostAlgoPathValidator postAlgoPathValidator = new PostAlgoPathValidator(
            networkTransactionService,
            customerAvailableFrequencies,
            clientInputMock
        );

        GraphPath<String, PceGraphEdge> entry = allWPaths.get(0);

        SpectrumAssignment expected = new SpectrumAssignmentBuilder()
            .setBeginIndex(Uint16.valueOf(9))
            .setStopIndex(Uint16.valueOf(14))
            .setFlexGrid(true)
            .build();

        Assertions.assertEquals(
            expected,
            postAlgoPathValidator
                .getSpectrumAssignment(entry, nodes, 6, Mockito.mock(Subscriber.class))
        );

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
     * The above settings are found in the folder pce/src/test/resources/topology/nodes.
     */
    @Test
    void spectrumAssignmentRoadmA_SRG4_to_RoadmC_SRG12_37_5GHzFails() throws IOException {

        YenKShortestPath<String, PceGraphEdge> swp = new YenKShortestPath<>(weightedGraph, wpv);

        List<GraphPath<String, PceGraphEdge>> weightedPathList = swp
            .getPaths("ROADM-A-SRG4", "ROADM-C-SRG12", 15);

        Map<Integer, GraphPath<String, PceGraphEdge>> allWPaths = IntStream
            .range(0, weightedPathList.size())
            .boxed()
            .collect(Collectors.toMap(Function.identity(), weightedPathList::get));

        Assertions.assertEquals(1, allWPaths.size());

        NetworkTransactionService networkTransactionService = Mockito.mock(NetworkTransactionService.class);

        PostAlgoPathValidator postAlgoPathValidator = new PostAlgoPathValidator(
            networkTransactionService,
            customerAvailableFrequencies,
            clientInputMock
        );

        GraphPath<String, PceGraphEdge> entry = allWPaths.get(0);

        SpectrumAssignment expected = new SpectrumAssignmentBuilder()
            .setBeginIndex(Uint16.valueOf(0))
            .setStopIndex(Uint16.valueOf(0))
            .setFlexGrid(true)
            .build();

        Assertions.assertEquals(
            expected,
            postAlgoPathValidator
                .getSpectrumAssignment(entry, nodes, 6, Mockito.mock(Subscriber.class))
        );

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
     * The above settings are found in the folder pce/src/test/resources/topology/nodes.
     */
    @Test
    void spectrumAssignmentRoadmA_SRG4_to_RoadmC_SRG12_50GHzSucceeds() throws IOException {

        YenKShortestPath<String, PceGraphEdge> swp = new YenKShortestPath<>(weightedGraph, wpv);

        List<GraphPath<String, PceGraphEdge>> weightedPathList = swp
            .getPaths("ROADM-A-SRG4", "ROADM-C-SRG12", 15);

        Map<Integer, GraphPath<String, PceGraphEdge>> allWPaths = IntStream
            .range(0, weightedPathList.size())
            .boxed()
            .collect(Collectors.toMap(Function.identity(), weightedPathList::get));

        Assertions.assertEquals(1, allWPaths.size());

        NetworkTransactionService networkTransactionService = Mockito.mock(NetworkTransactionService.class);

        PostAlgoPathValidator postAlgoPathValidator = new PostAlgoPathValidator(
            networkTransactionService,
            customerAvailableFrequencies,
            clientInputMock
        );

        GraphPath<String, PceGraphEdge> entry = allWPaths.get(0);

        //Possible center frequencies are [ 44, 92, 188, ... ], center frequency granularity = 300GHz.
        //Slot 36 - 51 is occupied on ROADM-B-DEG1, so first possible center frequency is 92 (well, between 91
        //and 92 really). A 50GHz service need 8 x 6.25GHz slots.
        SpectrumAssignment expected = new SpectrumAssignmentBuilder()
            .setBeginIndex(Uint16.valueOf(88))
            .setStopIndex(Uint16.valueOf(95))
            .setFlexGrid(false)
            .build();

        Assertions.assertEquals(
            expected,
            postAlgoPathValidator
                .getSpectrumAssignment(entry, nodes, 8, Mockito.mock(Subscriber.class))
        );

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
     * The above settings are found in the folder pce/src/test/resources/topology/nodes.
     */
    @Test
    void spectrumAssignmentRoadmA_SRG4_to_RoadmC_SRG12_100GHzFails() throws IOException {

        YenKShortestPath<String, PceGraphEdge> swp = new YenKShortestPath<>(weightedGraph, wpv);

        List<GraphPath<String, PceGraphEdge>> weightedPathList = swp
            .getPaths("ROADM-A-SRG4", "ROADM-C-SRG12", 15);

        Map<Integer, GraphPath<String, PceGraphEdge>> allWPaths = IntStream
            .range(0, weightedPathList.size())
            .boxed()
            .collect(Collectors.toMap(Function.identity(), weightedPathList::get));

        Assertions.assertEquals(1, allWPaths.size());

        NetworkTransactionService networkTransactionService = Mockito.mock(NetworkTransactionService.class);

        PostAlgoPathValidator postAlgoPathValidator = new PostAlgoPathValidator(
            networkTransactionService,
            customerAvailableFrequencies,
            clientInputMock
        );

        GraphPath<String, PceGraphEdge> entry = allWPaths.get(0);

        //There is no available spectrum, i.e. begin index = stop index = 0.
        SpectrumAssignment expected = new SpectrumAssignmentBuilder()
            .setBeginIndex(Uint16.valueOf(0))
            .setStopIndex(Uint16.valueOf(0))
            .setFlexGrid(true)
            .build();

        Assertions.assertEquals(
            expected,
            postAlgoPathValidator
                .getSpectrumAssignment(entry, nodes, 16, Mockito.mock(Subscriber.class))
        );

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
     * The above settings are found in the folder pce/src/test/resources/topology/nodes.
     */
    @Test
    void spectrumAssignmentRoadmB_SRG13_to_RoadmC_SRG13_62_5_GHzSucceeds() throws IOException {

        YenKShortestPath<String, PceGraphEdge> swp = new YenKShortestPath<>(weightedGraph, wpv);

        List<GraphPath<String, PceGraphEdge>> weightedPathList = swp
            .getPaths("ROADM-B-SRG13", "ROADM-C-SRG13", 15);

        Map<Integer, GraphPath<String, PceGraphEdge>> allWPaths = IntStream
            .range(0, weightedPathList.size())
            .boxed()
            .collect(Collectors.toMap(Function.identity(), weightedPathList::get));

        Assertions.assertEquals(1, allWPaths.size());

        NetworkTransactionService networkTransactionService = Mockito.mock(NetworkTransactionService.class);

        PostAlgoPathValidator postAlgoPathValidator = new PostAlgoPathValidator(
            networkTransactionService,
            customerAvailableFrequencies,
            clientInputMock
        );

        GraphPath<String, PceGraphEdge> entry = allWPaths.get(0);

        SpectrumAssignment expected = new SpectrumAssignmentBuilder()
            .setBeginIndex(Uint16.valueOf(3))
            .setStopIndex(Uint16.valueOf(12))
            .setFlexGrid(true)
            .build();

        Assertions.assertEquals(
            expected,
            postAlgoPathValidator
                .getSpectrumAssignment(entry, nodes, 10, Mockito.mock(Subscriber.class))
        );

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
     * The above settings are found in the folder pce/src/test/resources/topology/nodes.
     */
    @Test
    void spectrumAssignmentRoadmB_SRG12_to_RoadmC_SRG13_62_5_GHzFails() throws IOException {

        YenKShortestPath<String, PceGraphEdge> swp = new YenKShortestPath<>(weightedGraph, wpv);

        List<GraphPath<String, PceGraphEdge>> weightedPathList = swp
            .getPaths("ROADM-B-SRG12", "ROADM-C-SRG13", 15);

        Map<Integer, GraphPath<String, PceGraphEdge>> allWPaths = IntStream
            .range(0, weightedPathList.size())
            .boxed()
            .collect(Collectors.toMap(Function.identity(), weightedPathList::get));

        Assertions.assertEquals(1, allWPaths.size());

        NetworkTransactionService networkTransactionService = Mockito.mock(NetworkTransactionService.class);

        PostAlgoPathValidator postAlgoPathValidator = new PostAlgoPathValidator(
            networkTransactionService,
            customerAvailableFrequencies,
            clientInputMock
        );

        GraphPath<String, PceGraphEdge> entry = allWPaths.get(0);

        //There is no available spectrum, i.e. begin index = stop index = 0.
        SpectrumAssignment expected = new SpectrumAssignmentBuilder()
            .setBeginIndex(Uint16.valueOf(0))
            .setStopIndex(Uint16.valueOf(0))
            .setFlexGrid(true)
            .build();

        Assertions.assertEquals(
            expected,
            postAlgoPathValidator
                .getSpectrumAssignment(entry, nodes, 10, Mockito.mock(Subscriber.class))
        );

    }

    private Map<NodeId, PceNode> allPceNodes(String dir) throws IOException {
        XStream xstream = new XStream(new DomDriver());
        xstream.alias("PceOpticalNode", PceOpticalNode.class);
        xstream.omitField(PceOpticalNode.class, "availableSrgPp");
        xstream.omitField(PceOpticalNode.class, "availableSrgCp");
        xstream.allowTypesByWildcard(new String[] {
            "org.opendaylight.transportpce.pce.**"
        });

        Map<NodeId, PceNode> pceNodes = new HashMap<>();

        for (String xml : readFilesInDirectory(dir)) {
            PceNode pceNode = (PceNode) xstream.fromXML(xml);
            pceNodes.put(pceNode.getNodeId(), pceNode);
        }

        return pceNodes;
    }

    /**
     * Reading file names from a directory assuming the filename
     * is a node id suffixed with ".txt".
     * @return as list of node ids without the ".txt" suffix.
     */
    private List<String> nodeIds(String nodesDirectory) throws IOException {
        List<String> nodeIds = new ArrayList<>();

        Set<String> fileNames = fileNamesInFolder(nodesDirectory);

        for (String fileName : fileNames) {
            if (fileName.endsWith(".txt")) {
                nodeIds.add(fileName.replace(".txt", ""));
            }
        }

        return nodeIds;
    }

    /**
     * Creates a {@code Graph<String, PceGraphEdge>} object by reading
     * link data from text files in the directory linksDir. The
     * text-files are assumed to contain xml data matching the inner class {@code Edge}
     * defined in this test further down below.
     */
    private Graph<String, PceGraphEdge> weightedGraph(String linksDir, List<String> nodeIds) throws IOException {
        Graph<String, PceGraphEdge> graph = new DefaultDirectedWeightedGraph<>(PceGraphEdge.class);

        for (String node : nodeIds) {
            graph.addVertex(node);
        }

        XStream xstream = new XStream(new DomDriver());
        xstream.alias("edge", Edge.class);
        xstream.allowTypesByWildcard(new String[] {
            "org.opendaylight.transportpce.pce.**"
        });

        for (String xml : readFilesInDirectory(linksDir)) {
            Edge edge = (Edge) xstream.fromXML(xml);

            graph.addEdge(edge.getSource(), edge.getDestination(), edge.getGraphLink());
            graph.setEdgeWeight(edge.getGraphLink(), edge.getWeight());
        }

        return graph;
    }

    private List<String> readFilesInDirectory(String dir) throws IOException {

        Set<String> fileNames = fileNamesInFolder(dir);

        List<String> xml = new ArrayList<>(fileNames.size());
        for (String filename : fileNames) {

            StringBuilder stringBuilder = new StringBuilder();
            try (Stream<String> stream = Files.lines(Paths.get(dir + "/" + filename))) {
                stream.forEach(line -> {
                    stringBuilder.append(line);
                    stringBuilder.append(System.lineSeparator());
                });
            }

            xml.add(stringBuilder.toString());
        }

        return xml;
    }

    private Set<String> fileNamesInFolder(String dir) throws IOException {
        try (Stream<Path> stream = Files.list(Paths.get(dir))) {
            return stream
                .filter(file -> !Files.isDirectory(file))
                .map(Path::getFileName)
                .map(Path::toString)
                .collect(Collectors.toCollection(TreeSet::new));
        }
    }

    /**
     * This is an intermediate class containing all the bits and pieces
     * required for setting up a Graph object (i.e. {@code Graph<String, PceGraphEdge>}).
     * It is simply used as a container for the information.
     *
     * <p>The object matches the files in src/test/resources/topology/path</p>
     *
     * <p>The class is intended to be used together with XStream, i.e.:
     *         XStream xstream = new XStream(new DomDriver());
     *         xstream.alias("edge", Edge.class);
     *         xstream.allowTypesByWildcard(new String[] {
     *             "org.opendaylight.transportpce.pce.**"
     *         });
     *         String xml = {contents from a file in src/test/resources/topology/path}
     *         Edge edge = (Edge) xstream.fromXML(xml);
     * </p>
     */
    class Edge {

        private final PceGraphEdge graphLink;

        private final String source;

        private final String destination;

        private final double weight;

        Edge(PceGraphEdge graphLink, String source, String destination, double weight) {
            this.graphLink = graphLink;
            this.source = source;
            this.destination = destination;
            this.weight = weight;
        }

        public PceGraphEdge getGraphLink() {
            return graphLink;
        }

        public String getSource() {
            return source;
        }

        public String getDestination() {
            return destination;
        }

        public double getWeight() {
            return weight;
        }
    }
}
