/*
 * Copyright © 2020 Orange, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.networkmodel.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.BitSet;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.junit.BeforeClass;
import org.junit.Test;
import org.opendaylight.mdsal.binding.api.ReadTransaction;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.transportpce.common.InstanceIdentifiers;
import org.opendaylight.transportpce.common.NetworkUtils;
import org.opendaylight.transportpce.common.Timeouts;
import org.opendaylight.transportpce.common.fixedflex.GridConstant;
import org.opendaylight.transportpce.networkmodel.util.OpenRoadmTopology;
import org.opendaylight.transportpce.test.AbstractTest;
import org.opendaylight.transportpce.test.converter.DataObjectConverter;
import org.opendaylight.transportpce.test.converter.JSONDataObjectConverter;
import org.opendaylight.transportpce.test.utils.TopologyDataUtils;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev200529.Node1;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev200529.TerminationPoint1;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.types.rev200529.available.freq.map.AvailFreqMapsKey;
import org.opendaylight.yang.gen.v1.pathdescription.stub.rev201211.PathDescription;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FrequenciesServiceTest extends AbstractTest {
    private static final Logger LOG = LoggerFactory.getLogger(FrequenciesServiceTest.class);
    private static final String OPENROADM_TOPOLOGY_FILE = "src/test/resources/openroadm-topology.xml";
    private static final String PATH_DESCRIPTION_FILE = "src/test/resources/path_description.json";
    private static PathDescription pathDescription;
    private final AvailFreqMapsKey availFreqMapKey = new AvailFreqMapsKey(GridConstant.C_BAND);
    private final BitSet usedBits = new BitSet(8);
    private static BitSet availableBits = new BitSet(8);

    @BeforeClass
    public static void setUp() throws InterruptedException, ExecutionException, FileNotFoundException {
        availableBits.set(0, 8, true);
        TopologyDataUtils.writeTopologyFromFileToDatastore(getDataStoreContextUtil(), OPENROADM_TOPOLOGY_FILE,
                InstanceIdentifiers.OVERLAY_NETWORK_II);
        DataObjectConverter dataObjectConverter = JSONDataObjectConverter
                .createWithDataStoreUtil(getDataStoreContextUtil());
        try (Reader reader = new FileReader(PATH_DESCRIPTION_FILE, StandardCharsets.UTF_8)) {
            NormalizedNode normalizedNode = dataObjectConverter
                    .transformIntoNormalizedNode(reader).get();
            pathDescription = (PathDescription) getDataStoreContextUtil()
                    .getBindingDOMCodecServices().fromNormalizedNode(YangInstanceIdentifier
                            .of(PathDescription.QNAME), normalizedNode).getValue();
        } catch (IOException e) {
            LOG.error("Cannot load path description ", e);
            fail("Cannot load path description ");
        }
    }

    @Test
    public void allocateFrequenciesTest() throws IOException {
        FrequenciesService service = new FrequenciesServiceImpl(getDataBroker());
        service.allocateFrequencies(pathDescription.getAToZDirection(), pathDescription.getZToADirection());
        TerminationPoint1 terminationPoint = getNetworkTerminationPointFromDatastore("ROADM-A1-DEG2", "DEG2-CTP-TXRX");
        assertEquals("Lambda 1 should not be available for ctp-attributes",
                BitSet.valueOf(terminationPoint.getCtpAttributes().getAvailFreqMaps().get(availFreqMapKey)
                .getFreqMap()).get(760, 768),usedBits);
        assertNull("cp-attributes should be null", terminationPoint.getCpAttributes());
        terminationPoint = getNetworkTerminationPointFromDatastore("ROADM-A1-SRG1", "SRG1-PP1-TXRX");
        assertEquals("Lambda 1 should not be available for pp-attributes",
                BitSet.valueOf(terminationPoint.getPpAttributes().getAvailFreqMaps().get(availFreqMapKey)
                .getFreqMap()).get(760, 768),usedBits);
        Node1 node = getNetworkNodeFromDatastore("ROADM-A1-SRG1");
        assertEquals("Lambda 1 should not be available for srg-attributes",
                BitSet.valueOf(node.getSrgAttributes().getAvailFreqMaps().get(availFreqMapKey)
                .getFreqMap()).get(760, 768),usedBits);
    }

    @Test
    public void releaseFrequenciesTest() throws IOException {
        FrequenciesService service = new FrequenciesServiceImpl(getDataBroker());
        service.allocateFrequencies(pathDescription.getAToZDirection(), pathDescription.getZToADirection());
        service.releaseFrequencies(pathDescription.getAToZDirection(), pathDescription.getZToADirection());
        TerminationPoint1 terminationPoint = getNetworkTerminationPointFromDatastore("ROADM-A1-DEG2", "DEG2-CTP-TXRX");
        assertEquals("Lambda 1 should be available for ctp-attributes",
                BitSet.valueOf(terminationPoint.getCtpAttributes().getAvailFreqMaps().get(availFreqMapKey)
                .getFreqMap()).get(760, 768),availableBits);
        terminationPoint = getNetworkTerminationPointFromDatastore("ROADM-A1-SRG1", "SRG1-PP1-TXRX");
        assertEquals("Lambda 1 should be available for pp-attributes",
                BitSet.valueOf(terminationPoint.getPpAttributes().getAvailFreqMaps().get(availFreqMapKey)
                .getFreqMap()).get(760, 768),availableBits);
        Node1 node = getNetworkNodeFromDatastore("ROADM-A1-SRG1");
        assertEquals("Lambda 1 should be available for srg-attributes",
                BitSet.valueOf(node.getSrgAttributes().getAvailFreqMaps().get(availFreqMapKey)
                .getFreqMap()).get(760, 768),availableBits);
    }

    private TerminationPoint1 getNetworkTerminationPointFromDatastore(String nodeId, String tpId) {
        InstanceIdentifier<TerminationPoint1> tpIID = OpenRoadmTopology
                .createNetworkTerminationPointIIDBuilder(nodeId, tpId).build();
        try (ReadTransaction readTx = getDataBroker().newReadOnlyTransaction()) {
            Optional<TerminationPoint1> optionalTerminationPoint = readTx
                    .read(LogicalDatastoreType.CONFIGURATION, tpIID)
                    .get(Timeouts.DATASTORE_READ, TimeUnit.MILLISECONDS);
            if (optionalTerminationPoint.isPresent()) {
                return optionalTerminationPoint.get();
            } else {
                return null;
            }
        } catch (ExecutionException | TimeoutException e) {
            LOG.warn("Exception while getting termination {} for node id {} point from {} topology", tpId, nodeId,
                    NetworkUtils.OVERLAY_NETWORK_ID, e);
            return null;
        } catch (InterruptedException e) {
            LOG.warn("Getting termination {} for node id {} point from {} topology was interrupted", tpId, nodeId,
                    NetworkUtils.OVERLAY_NETWORK_ID, e);
            Thread.currentThread().interrupt();
            return null;
        }
    }

    private Node1 getNetworkNodeFromDatastore(String nodeId) {
        InstanceIdentifier<Node1> nodeIID = OpenRoadmTopology.createNetworkNodeIID(nodeId);
        try (ReadTransaction nodeReadTx = getDataBroker().newReadOnlyTransaction()) {
            Optional<Node1> optionalNode = nodeReadTx.read(LogicalDatastoreType.CONFIGURATION, nodeIID)
                    .get(Timeouts.DATASTORE_READ, TimeUnit.MILLISECONDS);
            if (optionalNode.isPresent()) {
                return optionalNode.get();
            } else {
                LOG.error("Unable to get network node for node id {}from topology {}", nodeId,
                        NetworkUtils.OVERLAY_NETWORK_ID);
                return null;
            }
        } catch (ExecutionException | TimeoutException e) {
            LOG.warn("Exception while getting network node for node id {} from {} topology", nodeId,
                    NetworkUtils.OVERLAY_NETWORK_ID, e);
            return null;
        } catch (InterruptedException e) {
            LOG.warn("Getting network node for node id {} from {} topology was interrupted", nodeId,
                    NetworkUtils.OVERLAY_NETWORK_ID, e);
            Thread.currentThread().interrupt();
            return null;
        }
    }

}
