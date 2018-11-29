/*
 * Copyright © 2017 AT&T, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.pce;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.opendaylight.controller.md.sal.binding.api.NotificationPublishService;
import org.opendaylight.transportpce.common.ResponseCodes;
import org.opendaylight.transportpce.pce.service.PathComputationService;
import org.opendaylight.transportpce.pce.service.PathComputationServiceImpl;
import org.opendaylight.transportpce.pce.utils.NotificationPublishServiceMock;
import org.opendaylight.transportpce.pce.utils.PceTestData;
import org.opendaylight.transportpce.pce.utils.PceTestUtils;
import org.opendaylight.transportpce.renderer.NetworkModelWavelengthService;
import org.opendaylight.transportpce.renderer.NetworkModelWavelengthServiceImpl;
import org.opendaylight.transportpce.test.AbstractTest;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev171017.PathComputationRequestInput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev171017.PathComputationRequestOutput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RunWith(value = Parameterized.class)
public class PceSingleTests extends AbstractTest {

    private static final Logger LOG = LoggerFactory.getLogger(PceSingleTests.class);

    private PathComputationRequestInput input;
    private PathComputationRequestOutput expectedOutput;
    private String topologyDataPath;
    private NetworkModelWavelengthService networkModelWavelengthService;
    private NotificationPublishService notificationPublishService;

    @Parameterized.Parameters(name = "parameters")
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
            { PceTestData.getEmptyPCERequest(), PceTestData.getFailedPCEResultYes(),
                "topologyData/NW-for-test-5-4.xml" },
            { PceTestData.getPCERequest(), PceTestData.getPCEResultOk((long)2),
                "topologyData/NW-for-test-5-4.xml" },
            { PceTestData.getPCE_test1_request_54(), PceTestData.getPCE_test_result_54((long)5),
                "topologyData/NW-for-test-5-4.xml" },
            { PceTestData.getPCE_test2_request_54(), PceTestData.getPCE_test_result_54((long)9),
                "topologyData/NW-for-test-5-4.xml" },
            { PceTestData.getPCE_test3_request_54(), PceTestData.getPCE_test_result_54((long)9),
                "topologyData/NW-for-test-5-4.xml" }
        });
    }

    /**
     * Input parameters for testPathCalculation.
     *
     * @param input
     *   input path computation request
     * @param expectedOutput
     *   expected path computation result
     * @param topologyDataPath
     *   path to topology data file to be used for DataStore population
     */
    public PceSingleTests(PathComputationRequestInput input, PathComputationRequestOutput expectedOutput,
        String topologyDataPath) {
        this.input = input;
        this.expectedOutput = expectedOutput;
        this.topologyDataPath = topologyDataPath;
        this.networkModelWavelengthService = new NetworkModelWavelengthServiceImpl(getDataBroker());
        this.notificationPublishService = new NotificationPublishServiceMock();
    }

    /**
     * This test runs single PCE calculation on the top one openroadm-topology.
     * @throws Exception exception throws by the function
     */
    @Test
    public void testPathCalculation() throws Exception {
        LOG.info("testPathCalculation");
        PceTestUtils.writeTopologyIntoDataStore(getDataBroker(), getDataStoreContextUtil(), this.topologyDataPath);

        PathComputationService pathComputationService =
            new PathComputationServiceImpl(getDataBroker(), this.notificationPublishService);
        PathComputationRequestOutput output = pathComputationService.pathComputationRequest(this.input).get();

        PceTestUtils.checkConfigurationResponse(output, this.expectedOutput);
        if (ResponseCodes.RESPONSE_OK.equals(output.getConfigurationResponseCommon().getResponseCode())) {
            PceTestUtils.checkCalculatedPath(output, this.expectedOutput);
        } else {
            Assert.fail("Path calculation failed !");
        }
        LOG.info("test done");
    }


}
