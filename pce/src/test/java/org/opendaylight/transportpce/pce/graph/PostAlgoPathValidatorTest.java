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

    private org.opendaylight.transportpce.pce.input.ClientInput clientInputMock;

    PostAlgoPathValidatorTest() throws IOException {
        weightedGraph = this.weightedGraph("src/test/resources/topology/path");
        nodes = this.allPceNodes("src/test/resources/topology/pcenodes");

        customerAvailableFrequencies.set(0, 772);

        clientInputMock = Mockito.mock(ClientInput.class);
        Mockito.when(clientInputMock.slotWidth(Mockito.anyInt())).thenAnswer(input -> input.getArgument(0));
        Mockito.when(clientInputMock.clientRangeWishListIntersection()).thenReturn(new EntireSpectrum(772));
        Mockito.when(clientInputMock.clientRangeWishListSubset()).thenReturn(new EntireSpectrum(772));
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
            available, 8, 8, isFlexGrid);

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
            available, 8, 1, isFlexGrid);

        Assertions.assertEquals(expectedFlexGrid, flexGrid);

    }

    /**
     * Setting up a service of 100GHz should be possible given these mc capabilities.
     *
     * <p>Path: [(ROADM-A-SRG4 : ROADM-A-DEG1), (ROADM-A-DEG1 : ROADM-B-DEG1), (ROADM-B-DEG1 : ROADM-B-SRG3)]</p>
     * <pre>
     * {@code
     * ROADM-A-SRG4
     *     <slotWidthGranularityGHz>12.5
     *     <centerFrequencyGranularityGHz>6.25
     *     <minSlots>3
     *     <maxSlots>16
     *
     * ROADM-A-DEG1
     *     <slotWidthGranularityGHz>12.5
     *     <centerFrequencyGranularityGHz>6.25
     *     <minSlots>3
     *     <maxSlots>16
     *
     * ROADM-B-SRG3
     *     <slotWidthGranularityGHz>12.5
     *     <centerFrequencyGranularityGHz>6.25
     *     <minSlots>3
     *     <maxSlots>16
     *
     * ROADM-B-DEG1
     *     <slotWidthGranularityGHz>12.5
     *     <centerFrequencyGranularityGHz>100.0
     *     <minSlots>3
     *     <maxSlots>16
     * }
     * </pre>
     */
    @Test
    void roadmAtoRoadmB100GHzSucceeds() throws IOException {

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
     * Setting up a service of 100GHz should NOT be possible given these mc capabilities (ROADM-F-SRG12
     * is limited to 75GHz).
     *
     * <p>Path: [(ROADM-A-SRG13 : ROADM-A-DEG2), (ROADM-A-DEG2 : ROADM-F-DEG2), (ROADM-F-DEG2 : ROADM-F-SRG12)]</p>
     * <pre>
     * {@code
     * ROADM-A-SRG13
     *     <slotWidthGranularityGHz>12.5
     *     <centerFrequencyGranularityGHz>6.25
     *     <minSlots>3
     *     <maxSlots>16
     * ROADM-A-DEG2
     *     <slotWidthGranularityGHz>12.5
     *     <centerFrequencyGranularityGHz>6.25
     *     <minSlots>3
     *     <maxSlots>16
     * ROADM-F-DEG2
     *     <slotWidthGranularityGHz>12.5
     *     <centerFrequencyGranularityGHz>6.25
     *     <minSlots>3
     *     <maxSlots>8
     * ROADM-F-SRG12
     *     <slotWidthGranularityGHz>12.5
     *     <centerFrequencyGranularityGHz>6.25
     *     <minSlots>3
     *     <maxSlots>6
     * }
     * </pre>
     */
    @Test
    void roadmAtoRoadmF100GHzFails() throws IOException {

        YenKShortestPath<String, PceGraphEdge> swp = new YenKShortestPath<>(weightedGraph, wpv);

        List<GraphPath<String, PceGraphEdge>> weightedPathList = swp
            .getPaths("ROADM-A-SRG13", "ROADM-F-SRG12", 15);

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
                .getSpectrumAssignment(entry, nodes, 16, Mockito.mock(Subscriber.class))
        );

    }

    /**
     * Setting up a service of 75GHz should be possible given these mc capabilities.
     *
     * <p>Path: [(ROADM-A-SRG13 : ROADM-A-DEG2), (ROADM-A-DEG2 : ROADM-F-DEG2), (ROADM-F-DEG2 : ROADM-F-SRG12)]</p>
     * <pre>
     * {@code
     * ROADM-A-SRG13
     *     <slotWidthGranularityGHz>12.5
     *     <centerFrequencyGranularityGHz>6.25
     *     <minSlots>3
     *     <maxSlots>16
     * ROADM-A-DEG2
     *     <slotWidthGranularityGHz>12.5
     *     <centerFrequencyGranularityGHz>6.25
     *     <minSlots>3
     *     <maxSlots>16
     * ROADM-F-DEG2
     *     <slotWidthGranularityGHz>12.5
     *     <centerFrequencyGranularityGHz>6.25
     *     <minSlots>3
     *     <maxSlots>8
     * ROADM-F-SRG12
     *     <slotWidthGranularityGHz>12.5
     *     <centerFrequencyGranularityGHz>6.25
     *     <minSlots>3
     *     <maxSlots>6
     * }
     * </pre>
     */
    @Test
    void roadmAtoRoadmF75GHzSucceeds() throws IOException {

        YenKShortestPath<String, PceGraphEdge> swp = new YenKShortestPath<>(weightedGraph, wpv);

        List<GraphPath<String, PceGraphEdge>> weightedPathList = swp
            .getPaths("ROADM-A-SRG13", "ROADM-F-SRG12", 15);

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
            .setStopIndex(Uint16.valueOf(11))
            .setFlexGrid(true)
            .build();

        Assertions.assertEquals(
            expected,
            postAlgoPathValidator
                .getSpectrumAssignment(entry, nodes, 12, Mockito.mock(Subscriber.class))
        );

    }

    /**
     * Setting up a service of 75GHz should be possible given these mc capabilities.
     *
     * <p>Path: [(ROADM-F-SRG3 : ROADM-F-DEG1), (ROADM-F-DEG1 : ROADM-E-DEG1), (ROADM-E-DEG1 : ROADM-E-SRG3)]</p>
     * <pre>
     * {@code
     * ROADM-F-SRG3
     *     <slotWidthGranularityGHz>12.5
     *     <centerFrequencyGranularityGHz>100.0
     *     <minSlots>1
     *     <maxSlots>16
     *
     * ROADM-F-DEG1
     *     <slotWidthGranularityGHz>12.5
     *     <centerFrequencyGranularityGHz>6.25
     *     <minSlots>3
     *     <maxSlots>8
     *
     * ROADM-E-DEG1
     *     <slotWidthGranularityGHz>12.5
     *     <centerFrequencyGranularityGHz>100.0
     *     <minSlots>1
     *     <maxSlots>16
     *
     * ROADM-E-SRG3
     *     <slotWidthGranularityGHz>12.5
     *     <centerFrequencyGranularityGHz>50.0
     *     <minSlots>1
     *     <maxSlots>16
     * }
     * </pre>
     */
    @Test
    void roadmFtoRoadmE75GHzSucceeds() throws IOException {

        YenKShortestPath<String, PceGraphEdge> swp = new YenKShortestPath<>(weightedGraph, wpv);

        List<GraphPath<String, PceGraphEdge>> weightedPathList = swp
            .getPaths("ROADM-F-SRG3", "ROADM-E-SRG3", 15);

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
            .setBeginIndex(Uint16.valueOf(6))
            .setStopIndex(Uint16.valueOf(17))
            .setFlexGrid(true)
            .build();

        Assertions.assertEquals(
            expected,
            postAlgoPathValidator
                .getSpectrumAssignment(entry, nodes, 12, Mockito.mock(Subscriber.class))
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

    private Graph<String, PceGraphEdge> weightedGraph(String dir) throws IOException {
        Graph<String, PceGraphEdge> graph =
            new DefaultDirectedWeightedGraph<>(PceGraphEdge.class);

        String[] nodeIds = {
            "ILA-B-C-DEG1", "ROADM-G-SRG1", "ROADM-C-SRG13", "ROADM-A-SRG8", "ROADM-A-SRG9",
            "ROADM-A-SRG4", "ROADM-A-SRG5", "ROADM-A-SRG6", "ROADM-A-SRG7", "ROADM-G-SRG3",
            "ROADM-A-SRG1", "ROADM-B-SRG10", "ROADM-C-DEG2", "ROADM-G-SRG2", "ROADM-A-SRG2",
            "ROADM-B-SRG13", "ROADM-C-DEG1", "ROADM-G-SRG5", "ROADM-A-SRG3", "ROADM-B-SRG12",
            "ROADM-G-SRG4", "ROADM-G-SRG7", "ROADM-G-SRG6", "ROADM-A-SRG26", "ROADM-G-SRG9",
            "ILA-B-C-DEG2", "ROADM-A-SRG27", "ROADM-G-SRG8", "ROADM-A-SRG24", "ROADM-C-SRG4",
            "ROADM-A-SRG25", "ROADM-A-SRG22", "ROADM-A-SRG23", "ROADM-C-SRG3", "ROADM-A-SRG20",
            "ROADM-F-SRG1", "ROADM-A-SRG21", "ROADM-C-SRG1", "ROADM-F-SRG3", "ROADM-F-SRG4",
            "ROADM-E-SRG10", "ROADM-D-SRG13", "ROADM-D-SRG12", "ROADM-A-DEG1", "ROADM-G-DEG1",
            "ROADM-D-DEG2", "ROADM-D-DEG1", "ROADM-A-SRG19", "ROADM-A-DEG3", "ROADM-A-SRG17",
            "ROADM-A-DEG2", "ROADM-A-SRG18", "ROADM-A-SRG15", "ROADM-A-SRG16", "ROADM-A-SRG13",
            "ROADM-A-SRG14", "ROADM-A-SRG11", "ROADM-A-SRG12", "ROADM-A-SRG10", "ROADM-E-SRG4",
            "ROADM-E-SRG3", "ROADM-E-SRG1", "ROADM-D-SRG10", "ROADM-E-SRG13", "ROADM-E-SRG12",
            "ROADM-D-SRG1", "ROADM-D-SRG3", "ROADM-F-DEG2", "ROADM-D-SRG4", "ROADM-F-DEG1",
            "ROADM-E-DEG1", "ROADM-B-DEG1", "ROADM-E-DEG2", "ROADM-B-DEG2", "ROADM-C-SRG12",
            "ROADM-B-SRG1", "ROADM-B-SRG4", "ROADM-C-SRG10", "ROADM-B-SRG3", "ROADM-F-SRG12",
            "ROADM-F-SRG13", "ROADM-F-SRG10"};

        for (String node : nodeIds) {
            graph.addVertex(node);
        }

        XStream xstream = new XStream(new DomDriver());
        xstream.alias("edge", Edge.class);
        xstream.allowTypesByWildcard(new String[] {
            "org.opendaylight.transportpce.pce.**"
        });

        for (String xml : readFilesInDirectory(dir)) {
            Edge edge = (Edge) xstream.fromXML(xml);

            graph.addEdge(edge.getSource(), edge.getDestination(), edge.getGraphLink());
            graph.setEdgeWeight(edge.getGraphLink(), edge.getWeight());
        }

        return graph;
    }

    private List<String> readFilesInDirectory(String dir) throws IOException {

        Set<String> fileNames = listFilesUsingFilesList(dir);

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

    private Set<String> listFilesUsingFilesList(String dir) throws IOException {
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
