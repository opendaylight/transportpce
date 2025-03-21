/*
 * Copyright © 2017 AT&T, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.pce.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import org.eclipse.jdt.annotation.Nullable;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.binding.api.WriteTransaction;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.transportpce.common.StringConstants;
import org.opendaylight.transportpce.test.DataStoreContext;
import org.opendaylight.transportpce.test.converter.DataObjectConverter;
import org.opendaylight.transportpce.test.converter.JsonDataConverter;
import org.opendaylight.transportpce.test.converter.XMLDataObjectConverter;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev240205.PathComputationRequestOutput;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev230501.path.description.atoz.direction.AToZ;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev230501.path.description.atoz.direction.AToZKey;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev230501.pce.resource.resource.resource.Node;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.NetworkId;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.Networks;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.networks.Network;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.networks.NetworkKey;
import org.opendaylight.yangtools.binding.DataObject;
import org.opendaylight.yangtools.binding.DataObjectIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.data.api.schema.DataContainerChild;
import org.opendaylight.yangtools.yang.data.api.schema.MapEntryNode;
import org.opendaylight.yangtools.yang.data.api.schema.MapNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class PceTestUtils {

    private static final Logger LOG = LoggerFactory.getLogger(PceTestUtils.class);

    private PceTestUtils() {
    }

    public static void writeTopologyIntoDataStore(DataBroker dataBroker,
                                                  DataStoreContext dataStoreContext, String topologyDataPath)
            throws ExecutionException, InterruptedException {

        DataObjectConverter dataObjectConverter = XMLDataObjectConverter.createWithDataStoreUtil(dataStoreContext);
        InputStream resourceAsStream = PceTestUtils.class.getClassLoader().getResourceAsStream(topologyDataPath);
        Optional<NormalizedNode> normalizedNode
                = dataObjectConverter.transformIntoNormalizedNode(resourceAsStream);
        DataContainerChild next
                = ((ContainerNode) normalizedNode.orElseThrow()).body().iterator().next();
        MapEntryNode mapNode = ((MapNode) next).body().iterator().next();
        Optional<DataObject> dataObject = dataObjectConverter.getDataObject(mapNode, Network.QNAME);
        DataObjectIdentifier<Network> nwInstanceIdentifier = DataObjectIdentifier.builder(Networks.class)
                .child(Network.class, new NetworkKey(new NetworkId(StringConstants.OPENROADM_TOPOLOGY)))
                .build();
        WriteTransaction dataWriteTransaction = dataBroker.newWriteOnlyTransaction();
        dataWriteTransaction.put(LogicalDatastoreType.CONFIGURATION, nwInstanceIdentifier,
                (Network) dataObject.orElseThrow());
        dataWriteTransaction.commit().get();
    }

    public static void writeNetworkInDataStore(DataBroker dataBroker) {

        try (
                // load openroadm-network
                Reader gnpyNetwork = Files.newBufferedReader(Path.of("src/test/resources/gnpy/gnpy_network.json"),
                        StandardCharsets.UTF_8);
                // load openroadm-topology
                Reader gnpyTopo = Files.newBufferedReader(Path.of("src/test/resources/gnpy/gnpy_topology.json"),
                        StandardCharsets.UTF_8);
        ) {

            Networks networks = (Networks) new JsonDataConverter(null).deserialize(gnpyNetwork, Networks.QNAME);
            List<Network> networkMap = new ArrayList<>(networks.nonnullNetwork().values());
            saveOpenRoadmNetwork(networkMap.get(0), StringConstants.OPENROADM_NETWORK, dataBroker);
            networks = (Networks) new JsonDataConverter(null).deserialize(gnpyTopo,  Networks.QNAME);
            saveOpenRoadmNetwork(networkMap.get(0), StringConstants.OPENROADM_NETWORK, dataBroker);
        } catch (IOException | ExecutionException | InterruptedException e) {
            LOG.error("Cannot init test ", e);
            fail("Cannot init test ");
        }
    }

    private static void saveOpenRoadmNetwork(Network network, String networkId, DataBroker dataBroker)
            throws InterruptedException, ExecutionException {
        DataObjectIdentifier<Network> nwInstanceIdentifier = DataObjectIdentifier.builder(Networks.class)
                .child(Network.class, new NetworkKey(new NetworkId(networkId)))
                .build();
        WriteTransaction dataWriteTransaction = dataBroker.newWriteOnlyTransaction();
        dataWriteTransaction.put(LogicalDatastoreType.CONFIGURATION, nwInstanceIdentifier, network);
        dataWriteTransaction.commit().get();
    }

    public static void writeNetworkIntoDataStore(DataBroker dataBroker,
                                                 DataStoreContext dataStoreContext, Network network)
            throws ExecutionException, InterruptedException {

        DataObjectIdentifier<Network> nwInstanceIdentifier = DataObjectIdentifier.builder(Networks.class)
                .child(Network.class, new NetworkKey(new NetworkId(StringConstants.OPENROADM_TOPOLOGY)))
                .build();
        WriteTransaction dataWriteTransaction = dataBroker.newWriteOnlyTransaction();
        dataWriteTransaction.put(LogicalDatastoreType.CONFIGURATION, nwInstanceIdentifier, network);
        dataWriteTransaction.commit().get();
    }

    public static void checkConfigurationResponse(PathComputationRequestOutput output,
                                                  PathComputationRequestOutput expectedOutput) {
        assertNotNull(output);
        assertEquals(
                expectedOutput.getConfigurationResponseCommon().getAckFinalIndicator(),
                output.getConfigurationResponseCommon().getAckFinalIndicator());
        assertEquals(
                expectedOutput.getConfigurationResponseCommon().getResponseMessage(),
                output.getConfigurationResponseCommon().getResponseMessage());
        assertEquals(
                expectedOutput.getConfigurationResponseCommon().getResponseCode(),
                output.getConfigurationResponseCommon().getResponseCode());
    }

    public static void checkCalculatedPath(PathComputationRequestOutput output,
                                           PathComputationRequestOutput expectedOutput) {
        assertNotNull(output.getResponseParameters().getPathDescription());
        assertNotNull(output.getResponseParameters().getPathDescription().getAToZDirection());
        assertNotNull(output.getResponseParameters().getPathDescription().getZToADirection());
        assertNotNull(output.getResponseParameters().getPathDescription().getAToZDirection().getAToZ());
        assertNotNull(output.getResponseParameters().getPathDescription().getZToADirection().getZToA());

        int atozSize = output.getResponseParameters().getPathDescription().getAToZDirection().getAToZ().size();
        int ztoaSize = output.getResponseParameters().getPathDescription().getZToADirection().getZToA().size();
        assertEquals(atozSize, ztoaSize);

        Long actualAToZWavel = output.getResponseParameters().getPathDescription().getAToZDirection()
                .getAToZWavelengthNumber().toJava();
        Long expectedAToZWavel = expectedOutput.getResponseParameters().getPathDescription().getAToZDirection()
                .getAToZWavelengthNumber().toJava();
        assertEquals(actualAToZWavel, expectedAToZWavel);

        Long actualZtoAWavel = output.getResponseParameters().getPathDescription().getZToADirection()
                .getZToAWavelengthNumber().toJava();
        Long expectedZtoAWavel = expectedOutput.getResponseParameters().getPathDescription().getZToADirection()
                .getZToAWavelengthNumber().toJava();
        assertEquals(actualZtoAWavel, expectedZtoAWavel);

        Long actualAToZRate = output.getResponseParameters().getPathDescription().getAToZDirection().getRate().toJava();
        Long expectedAToZRate = expectedOutput.getResponseParameters().getPathDescription().getAToZDirection()
                .getRate().toJava();
        assertEquals(expectedAToZRate, actualAToZRate);

        Long actualZToARate = output.getResponseParameters().getPathDescription().getZToADirection().getRate().toJava();
        Long expectedZToARate = expectedOutput.getResponseParameters().getPathDescription().getZToADirection()
                .getRate().toJava();
        assertEquals(actualZToARate, expectedZToARate);
    }

    private static List<String> getNodesFromPath(PathComputationRequestOutput output) {
        @Nullable Map<AToZKey, AToZ> atozList = output.getResponseParameters()
                .getPathDescription().getAToZDirection().getAToZ();
        return atozList.values().stream()
                .filter(aToZ -> {
                    if ((aToZ.getResource() == null) || (aToZ.getResource().getResource() == null)) {
                        LOG.debug("Diversity constraint: Resource of AToZ node {} is null! Skipping this node!",
                                aToZ.getId());
                        return false;
                    }
                    return aToZ.getResource().getResource() instanceof Node;
                }).map(aToZ -> {
                    Node node = (Node) aToZ.getResource().getResource();
                    if (node.getNodeId() == null) {
                        LOG.warn("Node in AToZ node {} contains null! Skipping this node!", aToZ.getId());
                        return null;
                    }
                    return node.getNodeId().toString();
                }).collect(Collectors.toList());
    }

    public static boolean comparePath(PathComputationRequestOutput output1, PathComputationRequestOutput output2) {
        // return true if paths are different
        List<String> nodes1 = getNodesFromPath(output1);
        LOG.info("nodes1: {}", nodes1.toString());
        List<String> nodes2 = getNodesFromPath(output2);
        LOG.info("nodes2: {}", nodes2.toString());
        nodes1.retainAll(nodes2);
        LOG.info("nodes after intersection: {}", nodes1.toString());
        return nodes1.isEmpty();
    }

    public static boolean checkPCECalculationConflicts(PathComputationRequestOutput[] outputs,
                                                       int iterationOrdinal, DataBroker dataBroker) {
        return true;
    }

}
