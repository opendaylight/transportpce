/*
 * Copyright © 2017 AT&T, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.pce.utils;

import java.io.InputStream;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import org.junit.Assert;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.binding.api.WriteTransaction;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.transportpce.common.DataStoreContext;
import org.opendaylight.transportpce.common.NetworkUtils;
import org.opendaylight.transportpce.common.converter.XMLDataObjectConverter;
import org.opendaylight.transportpce.common.converter.api.DataObjectConverter;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev200128.PathComputationRequestOutput;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev171017.path.description.atoz.direction.AToZ;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev171017.pce.resource.resource.resource.Node;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.NetworkId;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.Networks;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.networks.Network;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.networks.NetworkKey;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
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
        Optional<NormalizedNode<? extends YangInstanceIdentifier.PathArgument, ?>> normalizedNode
                = dataObjectConverter.transformIntoNormalizedNode(resourceAsStream);
        DataContainerChild<? extends YangInstanceIdentifier.PathArgument, ?> next
                = ((ContainerNode) normalizedNode.get()).getValue().iterator().next();
        MapEntryNode mapNode = ((MapNode) next).getValue().iterator().next();
        Optional<DataObject> dataObject = dataObjectConverter.getDataObject(mapNode, Network.QNAME);
        InstanceIdentifier<Network> nwInstanceIdentifier = InstanceIdentifier.builder(Networks.class)
            .child(Network.class, new NetworkKey(new NetworkId(NetworkUtils.OVERLAY_NETWORK_ID)))
            .build();
        WriteTransaction dataWriteTransaction = dataBroker.newWriteOnlyTransaction();
        dataWriteTransaction.put(LogicalDatastoreType.CONFIGURATION, nwInstanceIdentifier, (Network) dataObject.get());
        dataWriteTransaction.commit().get();
    }

    public static void checkConfigurationResponse(PathComputationRequestOutput output,
                                                  PathComputationRequestOutput expectedOutput) {
        Assert.assertNotNull(output);
        Assert.assertEquals(
                expectedOutput.getConfigurationResponseCommon().getAckFinalIndicator(),
                output.getConfigurationResponseCommon().getAckFinalIndicator());
        Assert.assertEquals(
                expectedOutput.getConfigurationResponseCommon().getResponseMessage(),
                output.getConfigurationResponseCommon().getResponseMessage());
        Assert.assertEquals(
                expectedOutput.getConfigurationResponseCommon().getResponseCode(),
                output.getConfigurationResponseCommon().getResponseCode());
    }

    public static void checkCalculatedPath(PathComputationRequestOutput output,
                                           PathComputationRequestOutput expectedOutput) {
        Assert.assertNotNull(output.getResponseParameters().getPathDescription());
        Assert.assertNotNull(output.getResponseParameters().getPathDescription().getAToZDirection());
        Assert.assertNotNull(output.getResponseParameters().getPathDescription().getZToADirection());
        Assert.assertNotNull(output.getResponseParameters().getPathDescription().getAToZDirection().getAToZ());
        Assert.assertNotNull(output.getResponseParameters().getPathDescription().getZToADirection().getZToA());

        int atozSize = output.getResponseParameters().getPathDescription().getAToZDirection().getAToZ().size();
        int ztoaSize = output.getResponseParameters().getPathDescription().getZToADirection().getZToA().size();
        Assert.assertEquals(atozSize, ztoaSize);

        Long actualAToZWavel = output.getResponseParameters().getPathDescription().getAToZDirection()
            .getAToZWavelengthNumber().toJava();
        Long expectedAToZWavel = expectedOutput.getResponseParameters().getPathDescription().getAToZDirection()
            .getAToZWavelengthNumber().toJava();
        Assert.assertEquals(actualAToZWavel, expectedAToZWavel);

        Long actualZtoAWavel = output.getResponseParameters().getPathDescription().getZToADirection()
            .getZToAWavelengthNumber().toJava();
        Long expectedZtoAWavel = expectedOutput.getResponseParameters().getPathDescription().getZToADirection()
            .getZToAWavelengthNumber().toJava();
        Assert.assertEquals(actualZtoAWavel, expectedZtoAWavel);

        Long actualAToZRate = output.getResponseParameters().getPathDescription().getAToZDirection().getRate().toJava();
        Long expectedAToZRate = expectedOutput.getResponseParameters().getPathDescription().getAToZDirection()
            .getRate().toJava();
        Assert.assertEquals(expectedAToZRate, actualAToZRate);

        Long actualZToARate = output.getResponseParameters().getPathDescription().getZToADirection().getRate().toJava();
        Long expectedZToARate = expectedOutput.getResponseParameters().getPathDescription().getZToADirection()
            .getRate().toJava();
        Assert.assertEquals(actualZToARate, expectedZToARate);
    }

    private static List<String> getNodesFromPath(PathComputationRequestOutput output) {
        List<AToZ> atozList = output.getResponseParameters().getPathDescription().getAToZDirection().getAToZ();
        return atozList.stream()
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
