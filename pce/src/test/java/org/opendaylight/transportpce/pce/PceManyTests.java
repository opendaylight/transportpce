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
public class PceManyTests extends AbstractTest {

    private static final Logger LOG = LoggerFactory.getLogger(PceManyTests.class);

    private PathComputationRequestInput input;
    private PathComputationRequestOutput expectedOutput;
    private NetworkModelWavelengthService networkModelWavelengthService;
    private NotificationPublishService notificationPublishService;

    /**
     * Input parameters for testPathCalculation.
     *
     * @param input
     *   input path computation request
     * @param expectedOutput
     *   expected path computation result
     * @param topologyDataPath
     *   path to topology data file to be used for DataStore population
     *
     * @throws Exception exception throws by the function
     */
    public PceManyTests(PathComputationRequestInput input,
                        PathComputationRequestOutput expectedOutput, String topologyDataPath) throws Exception {
        this.input = input;
        this.expectedOutput = expectedOutput;
        this.networkModelWavelengthService = new NetworkModelWavelengthServiceImpl(getDataBroker());
        this.notificationPublishService = new NotificationPublishServiceMock();
        PceTestUtils.writeTopologyIntoDataStore(getDataBroker(), getDataStoreContextUtil(), topologyDataPath);
    }

    @Parameterized.Parameters(name = "parameters")
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
                { PceTestData.getPCE_simpletopology_test1_request(),
                    PceTestData.getPCE_simpletopology_test1_result((long)5),  "topologyData/NW-simple-topology.xml" }
        });
    }

   /**
    * This test runs single PCE calculation on the top one openroadm-topology.
    * @throws Exception exception throws by the function
    */
    @Test
    public void testPathCalculations() throws Exception {
        LOG.info("testPathCalculations");

        PathComputationService pathComputationService =
            new PathComputationServiceImpl(getDataBroker(), notificationPublishService);
        PathComputationRequestOutput output = pathComputationService.pathComputationRequest(input).get();

        PceTestUtils.checkConfigurationResponse(output, expectedOutput);

        if (ResponseCodes.RESPONSE_OK.equals(output.getConfigurationResponseCommon().getResponseCode())) {
            //networkModelWavelengthService.useWavelengths(output.getResponseParameters().getPathDescription());
            PceTestUtils.checkCalculatedPath(output, expectedOutput);
        }
    }

}
