/*
 * Copyright © 2020 Orange, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.networkmodel.service;

import static org.junit.Assert.fail;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutionException;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.opendaylight.transportpce.common.InstanceIdentifiers;
import org.opendaylight.transportpce.test.AbstractTest;
import org.opendaylight.transportpce.test.converter.DataObjectConverter;
import org.opendaylight.transportpce.test.converter.JSONDataObjectConverter;
import org.opendaylight.transportpce.test.utils.TopologyDataUtils;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev200128.service.path.rpc.result.PathDescription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Ignore
//FIXME: disabled because some updates are needed on test-common
public class NetworkModelWavelengthServiceTest extends AbstractTest {
    private static final Logger LOG = LoggerFactory.getLogger(NetworkModelWavelengthServiceTest.class);
    private static final String OPENROADM_TOPOLOGY_FILE = "src/test/resources/openroadm-topology.xml";
    private static final String PATH_DESCRIPTION_FILE = "src/test/resources/path_description.json";
    private static DataObjectConverter dataObjectConverter;

    @BeforeClass
    public static void setUp() throws InterruptedException, ExecutionException, FileNotFoundException {
        TopologyDataUtils.writeTopologyFromFileToDatastore(getDataStoreContextUtil(), OPENROADM_TOPOLOGY_FILE,
                InstanceIdentifiers.OVERLAY_NETWORK_II);
        dataObjectConverter = JSONDataObjectConverter.createWithDataStoreUtil(getDataStoreContextUtil());
    }

    @Test
    public void allocateFrequenciesTest() throws IOException {
        try (Reader reader = new FileReader(PATH_DESCRIPTION_FILE, StandardCharsets.UTF_8)) {
            PathDescription pathDescription = (PathDescription) dataObjectConverter.transformIntoNormalizedNode(reader)
                    .get().getValue();
            NetworkModelWavelengthService service = new NetworkModelWavelengthServiceImpl(getDataBroker());
            service.allocateFrequencies(pathDescription.getAToZDirection(), pathDescription.getZToADirection());
        } catch (IOException e) {
            LOG.error("Cannot load path description ", e);
            fail("Cannot load path description ");
        }
    }
}
