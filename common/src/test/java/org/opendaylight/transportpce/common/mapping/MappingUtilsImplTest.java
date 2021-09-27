/*
 * Copyright © 2020 Orange.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.common.mapping;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutionException;
import org.junit.BeforeClass;
import org.junit.Test;
import org.opendaylight.mdsal.binding.api.WriteTransaction;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.transportpce.common.StringConstants;
import org.opendaylight.transportpce.test.AbstractTest;
import org.opendaylight.transportpce.test.converter.DataObjectConverter;
import org.opendaylight.transportpce.test.converter.JSONDataObjectConverter;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev210927.Network;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MappingUtilsImplTest extends AbstractTest {
    private static final Logger LOG = LoggerFactory.getLogger(MappingUtilsImplTest.class);
    private static MappingUtils mappingUtils;

    @BeforeClass
    public static void setUp() throws InterruptedException, ExecutionException, FileNotFoundException {
        DataObjectConverter dataObjectConverter = JSONDataObjectConverter
                .createWithDataStoreUtil(getDataStoreContextUtil());
        try (Reader reader = new FileReader("src/test/resources/network.json", StandardCharsets.UTF_8)) {
            NormalizedNode normalizedNode = dataObjectConverter
                    .transformIntoNormalizedNode(reader).get();
            Network network = (Network) getDataStoreContextUtil()
                    .getBindingDOMCodecServices().fromNormalizedNode(YangInstanceIdentifier
                            .of(Network.QNAME), normalizedNode).getValue();
            WriteTransaction writeNetworkTransaction = getDataBroker().newWriteOnlyTransaction();
            writeNetworkTransaction.put(LogicalDatastoreType.CONFIGURATION,
                    InstanceIdentifier.builder(Network.class).build(), network);
            writeNetworkTransaction.commit().get();
        } catch (IOException e) {
            LOG.error("Cannot load network ", e);
            fail("Cannot load network");
        }
        mappingUtils = new MappingUtilsImpl(getDataBroker());
    }

    @Test
    public void getOpenRoadmVersionTest() throws ExecutionException, InterruptedException {
        assertEquals("NodeInfo with ROADM-C1 as id should be 1.2.1 version",
                StringConstants.OPENROADM_DEVICE_VERSION_1_2_1,
                mappingUtils.getOpenRoadmVersion("ROADM-C1"));
        assertEquals("NodeInfo with ROADM-A1 as id should be 2.2.1 version",
                StringConstants.OPENROADM_DEVICE_VERSION_2_2_1,
                mappingUtils.getOpenRoadmVersion("ROADM-A1"));
        assertNull("NodeInfo with nodes3 as id should not exist", mappingUtils.getOpenRoadmVersion("nodes3"));
    }

    @Test
    public void getMcCapabilitiesForNodeTest() {
        assertEquals("Mc capabilities list size should be 2", 2,
                mappingUtils.getMcCapabilitiesForNode("ROADM-A1").size());
        assertTrue("Mc capabilities list size should be empty",
                mappingUtils.getMcCapabilitiesForNode("ROADM-A2").isEmpty());
    }


}
