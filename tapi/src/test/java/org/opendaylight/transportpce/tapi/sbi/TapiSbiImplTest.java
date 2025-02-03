/*
 * Copyright © 2019 Orange, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.tapi.sbi;

import static org.assertj.core.api.Assertions.fail;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.common.util.concurrent.MoreExecutors;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opendaylight.mdsal.binding.api.RpcService;
import org.opendaylight.transportpce.common.InstanceIdentifiers;
import org.opendaylight.transportpce.tapi.utils.TapiTopologyDataUtils;
import org.opendaylight.transportpce.test.AbstractTest;
import org.opendaylight.transportpce.test.converter.JsonDataConverter;
import org.opendaylight.transportpce.test.converter.ProcessingException;
import org.opendaylight.transportpce.test.utils.TopologyDataUtils;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.Context;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.Uuid;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.Context1;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.context.TopologyContext;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.topology.context.Topology;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.topology.context.TopologyKey;
import org.opendaylight.yangtools.binding.DataObjectIdentifier;
import org.opendaylight.yangtools.binding.DataObjectIdentifier.WithKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ExtendWith(MockitoExtension.class)
public class TapiSbiImplTest extends AbstractTest {
    private static final Logger LOG = LoggerFactory.getLogger(TapiSbiImplTest.class);

    @Mock
    private RpcService rpcService;
    private static final int NUM_THREADS = 3;

    @BeforeAll
    static void setUp() throws InterruptedException, ExecutionException {
        MoreExecutors.listeningDecorator(Executors.newFixedThreadPool(NUM_THREADS));
        new CountDownLatch(1);
        TopologyDataUtils.writeTapiTopologyFromFileToDatastore(getDataStoreContextUtil(),
            TapiTopologyDataUtils.TAPI_SBI_TOPOLOGY_FILE, InstanceIdentifiers.TAPI_NETWORK_II);
        LOG.info("setup done");
    }

    @Test
    void getTapiContext() throws InterruptedException, ExecutionException {
        LOG.info("test getTapiContext");
        DataObjectIdentifier<Context> tapiContextII = DataObjectIdentifier.builder(Context.class).build();
        LOG.info(TopologyDataUtils.readTapiTopologyFromDatastore(getDataStoreContextUtil(), tapiContextII).toString());
    }

    @Test
    void getTapiTopologyContext() throws InterruptedException, ExecutionException {
        LOG.info("test getTapiTopologyContext");
        DataObjectIdentifier<TopologyContext> tapiNetworkII = DataObjectIdentifier.builder(Context.class)
            .augmentation(Context1.class).child(TopologyContext.class).build();
        LOG.info(TopologyDataUtils.readTapiTopologyFromDatastore(getDataStoreContextUtil(), tapiNetworkII).toString());
    }

    @Test
    void getTapiTopology() throws InterruptedException, ExecutionException {
        LOG.info("test getTapiTopologyContext");
        WithKey<Topology, TopologyKey> tapiNetworkII = DataObjectIdentifier.builder(Context.class)
            .augmentation(Context1.class).child(TopologyContext.class)
            .child(Topology.class, new TopologyKey(new Uuid("393f09a4-0a0b-3d82-a4f6-1fbbc14ca1a7"))).build();
        LOG.info(TopologyDataUtils.readTapiTopologyFromDatastore(getDataStoreContextUtil(), tapiNetworkII).toString());
    }

    @Test
    void saveTapiContextToFile() throws InterruptedException, ExecutionException {
        LOG.info("test save Tapi Topology to JSON file");
        final Path filePath = Path.of("testSerializeContextToJSONFile.json");
        JsonDataConverter converter = new JsonDataConverter(null);
        DataObjectIdentifier<Context> tapiContextII = DataObjectIdentifier.builder(Context.class).build();
        try {
            converter.serializeToFile(
                tapiContextII, TopologyDataUtils.readTapiTopologyFromDatastore(getDataStoreContextUtil(),
                    tapiContextII), filePath);
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
