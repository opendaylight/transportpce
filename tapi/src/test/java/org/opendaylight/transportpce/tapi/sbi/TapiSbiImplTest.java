/*
 * Copyright © 2019 Orange, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.tapi.sbi;

import static org.assertj.core.api.Assertions.fail;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.opendaylight.transportpce.tapi.TapiConstants;
import org.opendaylight.transportpce.tapi.utils.TapiTopologyDataUtils;
import org.opendaylight.transportpce.test.AbstractTest;
import org.opendaylight.transportpce.test.converter.JsonDataConverter;
import org.opendaylight.transportpce.test.converter.ProcessingException;
import org.opendaylight.transportpce.test.utils.TopologyDataUtils;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.Context;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.Context1;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.context.TopologyContext;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.topology.Node;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.topology.NodeKey;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.topology.context.Topology;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.topology.context.TopologyKey;
import org.opendaylight.yangtools.binding.DataObjectIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TapiSbiImplTest extends AbstractTest {
    private static final Logger LOG = LoggerFactory.getLogger(TapiSbiImplTest.class);

    @BeforeAll
    static void setUp() throws InterruptedException, ExecutionException {
        TopologyDataUtils.writeTapiTopologyFromFileToDatastore(getDataStoreContextUtil(),
                TapiTopologyDataUtils.TAPI_SBI_TOPOLOGY_FILE, TapiConstants.TAPI_TOPOLOGY_II);
        LOG.info("setup done");
    }

    @Test
    void getTapiContext() throws InterruptedException, ExecutionException {
        LOG.info("test getTapiContext");
        Context context = (Context) TopologyDataUtils.readTapiTopologyFromDatastore(getDataStoreContextUtil(),
                TapiConstants.TAPI_CONTEXT_II);
        assertNotNull(context, "TAPI Context should not be null");
        LOG.info(context.toString());
    }

    @Test
    void getTapiTopologyContext() throws InterruptedException, ExecutionException {
        LOG.info("test getTapiTopologyContext");
        TopologyContext topologyContext = (TopologyContext) TopologyDataUtils.readTapiTopologyFromDatastore(
                getDataStoreContextUtil(), TapiConstants.TAPI_TOPOLOGY_CONTEXT_II);
        assertNotNull(topologyContext, "TAPI Topology Context should not be null");
        LOG.info(topologyContext.toString());
    }

    @Test
    void getTapiTopology() throws InterruptedException, ExecutionException {
        LOG.info("test getTapiTopologyContext");
        DataObjectIdentifier.WithKey<Topology, TopologyKey> key = DataObjectIdentifier.builder(Context.class)
                .augmentation(Context1.class)
                .child(TopologyContext.class)
                .child(Topology.class, new TopologyKey(TapiConstants.T0_FULL_MULTILAYER_UUID))
                .build();

        Topology topology = (Topology) TopologyDataUtils.readTapiTopologyFromDatastore(getDataStoreContextUtil(), key);

        Map<NodeKey, Node> node = topology.getNode();
        if (node != null || ! node.isEmpty()) {
            for (Map.Entry<NodeKey, Node> entry : node.entrySet()) {
                LOG.info("Node: {}", entry.getValue().getOwnedNodeEdgePoint());
            }
        }
        assertNotNull(topology, "TAPI Topology should not be null");
        LOG.info(topology.toString());
    }

    @Test
    void saveTapiContextToFile() throws InterruptedException, ExecutionException {
        LOG.info("test save Tapi Topology to JSON file");
        final Path filePath = Path.of("testSerializeContextToJSONFile.json");
        JsonDataConverter converter = new JsonDataConverter(null);
        try {
            converter.serializeToFile(
                    TapiConstants.TAPI_CONTEXT_II, TopologyDataUtils.readTapiTopologyFromDatastore(
                            getDataStoreContextUtil(), TapiConstants.TAPI_CONTEXT_II),
                    filePath);
            assertTrue(Files.exists(filePath));
        } catch (ProcessingException e) {
            fail("Cannot serialise object to json file");
        } finally {
            try {
                Files.deleteIfExists(filePath);
            } catch (IOException e) {
                fail("Failed to delete the test file: " + e.getMessage());
            }
        }
    }
}
