/*
 * Copyright © 2020 Orange Labs, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.pce.service;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.opendaylight.transportpce.common.network.NetworkTransactionService;
import org.opendaylight.transportpce.pce.utils.PceTestData;
import org.opendaylight.transportpce.test.AbstractTest;
import org.opendaylight.yang.gen.v1.gnpy.path.rev220615.PathBandwidth;
import org.opendaylight.yang.gen.v1.gnpy.path.rev220615.generic.path.properties.PathPropertiesBuilder;
import org.opendaylight.yang.gen.v1.gnpy.path.rev220615.generic.path.properties.path.properties.PathMetric;
import org.opendaylight.yang.gen.v1.gnpy.path.rev220615.generic.path.properties.path.properties.PathMetricBuilder;
import org.opendaylight.yang.gen.v1.gnpy.path.rev220615.no.path.info.NoPathBuilder;
import org.opendaylight.yang.gen.v1.gnpy.path.rev220615.result.Response;
import org.opendaylight.yang.gen.v1.gnpy.path.rev220615.result.ResponseBuilder;
import org.opendaylight.yang.gen.v1.gnpy.path.rev220615.result.ResponseKey;
import org.opendaylight.yang.gen.v1.gnpy.path.rev220615.result.response.response.type.NoPathCaseBuilder;
import org.opendaylight.yang.gen.v1.gnpy.path.rev220615.result.response.response.type.PathCaseBuilder;
import org.opendaylight.yangtools.yang.common.Decimal64;

public class PathComputationServiceImplTest extends AbstractTest {

    private PathComputationServiceImpl pathComputationServiceImpl;
    private static NetworkTransactionService networkTransactionService = null;

    @BeforeEach
    void setUp() {
        networkTransactionService = Mockito.mock(NetworkTransactionService.class);
        pathComputationServiceImpl = new PathComputationServiceImpl(
                networkTransactionService,
                this.getNotificationPublishService(), null, null);
        pathComputationServiceImpl.init();
    }

    @Test
    void pathComputationRequestTest() {
        pathComputationServiceImpl.generateGnpyResponse(null,"path");
        assertNotNull(
            pathComputationServiceImpl.pathComputationRequest(PceTestData.getPCE_simpletopology_test1_request()));
    }

    @Test
    void testPathComputationRequestNoPath() {
        Response response = new ResponseBuilder()
                .withKey(new ResponseKey("responseId")).setResponseType(new NoPathCaseBuilder()
                .setNoPath(new NoPathBuilder().setNoPath("no path").build()).build()).build();

        pathComputationServiceImpl.generateGnpyResponse(response,"path");
        assertNotNull(pathComputationServiceImpl.pathComputationRequest(PceTestData.getPCE_test3_request_54()));
    }

    @Test
    void testPathComputationRequestPathCase() {
        PathMetric pathMetric = new PathMetricBuilder()
                .setAccumulativeValue(Decimal64.valueOf("21"))
                .setMetricType(PathBandwidth.VALUE).build();
        Response response = new ResponseBuilder()
                .withKey(new ResponseKey("responseId")).setResponseType(new PathCaseBuilder()
                .setPathProperties(new PathPropertiesBuilder().setPathMetric(Map.of(pathMetric.key(),pathMetric))
                .build()).build()).build();

        pathComputationServiceImpl.generateGnpyResponse(response,"path");
        assertNotNull(pathComputationServiceImpl.pathComputationRequest(PceTestData.getPCE_test3_request_54()));
    }

    @Test
    void pathComputationRerouteRequestTest() {
        pathComputationServiceImpl.generateGnpyResponse(null,"path");
        assertNotNull(pathComputationServiceImpl.pathComputationRerouteRequest(PceTestData.getPCEReroute()));
    }

    @AfterEach
    void destroy() {
        pathComputationServiceImpl.close();
    }
}
