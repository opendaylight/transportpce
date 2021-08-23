/*
 * Copyright © 2020 Orange Labs, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.pce.service;

import java.math.BigDecimal;
import java.util.Map;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.transportpce.common.network.NetworkTransactionService;
import org.opendaylight.transportpce.pce.gnpy.GnpyResult;
import org.opendaylight.transportpce.pce.gnpy.GnpyTopoImpl;
import org.opendaylight.transportpce.pce.utils.PceTestData;
import org.opendaylight.transportpce.test.AbstractTest;
import org.opendaylight.transportpce.test.DataStoreContext;
import org.opendaylight.yang.gen.v1.gnpy.path.rev200909.PathBandwidth;
import org.opendaylight.yang.gen.v1.gnpy.path.rev200909.generic.path.properties.PathPropertiesBuilder;
import org.opendaylight.yang.gen.v1.gnpy.path.rev200909.generic.path.properties.path.properties.PathMetric;
import org.opendaylight.yang.gen.v1.gnpy.path.rev200909.generic.path.properties.path.properties.PathMetricBuilder;
import org.opendaylight.yang.gen.v1.gnpy.path.rev200909.no.path.info.NoPathBuilder;
import org.opendaylight.yang.gen.v1.gnpy.path.rev200909.result.Response;
import org.opendaylight.yang.gen.v1.gnpy.path.rev200909.result.ResponseBuilder;
import org.opendaylight.yang.gen.v1.gnpy.path.rev200909.result.ResponseKey;
import org.opendaylight.yang.gen.v1.gnpy.path.rev200909.result.response.response.type.NoPathCaseBuilder;
import org.opendaylight.yang.gen.v1.gnpy.path.rev200909.result.response.response.type.PathCaseBuilder;
import org.opendaylight.yangtools.yang.common.Uint32;

public class PathComputationServiceImplTest extends AbstractTest {

    private PathComputationServiceImpl pathComputationServiceImpl;
    private static NetworkTransactionService networkTransactionService = null;
    private static GnpyTopoImpl gnpyTopoImpl = null;
    private static GnpyResult gnpyResult = null;
    private DataStoreContext dataStoreContext = getDataStoreContextUtil();
    private DataBroker dataBroker = getDataBroker();

    @Before
    public void setUp() {
        networkTransactionService = Mockito.mock(NetworkTransactionService.class);
        gnpyTopoImpl = Mockito.mock(GnpyTopoImpl.class);
        gnpyResult = Mockito.mock(GnpyResult.class);
        pathComputationServiceImpl = new PathComputationServiceImpl(
                networkTransactionService,
                this.getNotificationPublishService(), null, null);
        pathComputationServiceImpl.init();
    }

    @Test
    public void pathComputationRequestTest() {
        pathComputationServiceImpl.generateGnpyResponse(null,"path");
        Assert.assertNotNull(
                pathComputationServiceImpl.pathComputationRequest(PceTestData.getPCE_simpletopology_test1_request()));

    }

    @Test
    public void testPathComputationRequestNoPath() {
        Response response = new ResponseBuilder()
                .withKey(new ResponseKey(Uint32.valueOf(1))).setResponseType(new NoPathCaseBuilder()
                .setNoPath(new NoPathBuilder().setNoPath("no path").build()).build()).build();

        pathComputationServiceImpl.generateGnpyResponse(response,"path");
        Assert.assertNotNull(
                pathComputationServiceImpl.pathComputationRequest(PceTestData.getPCE_test3_request_54()));

    }

    @Test
    public void testPathComputationRequestPathCase() {
        PathMetric pathMetric = new PathMetricBuilder()
                .setAccumulativeValue(new BigDecimal(21))
                .setMetricType(PathBandwidth.class).build();
        Response response = new ResponseBuilder()
                .withKey(new ResponseKey(Uint32.valueOf(1))).setResponseType(new PathCaseBuilder()
                .setPathProperties(new PathPropertiesBuilder().setPathMetric(Map.of(pathMetric.key(),pathMetric))
                .build()).build()).build();

        pathComputationServiceImpl.generateGnpyResponse(response,"path");
        Assert.assertNotNull(
                pathComputationServiceImpl.pathComputationRequest(PceTestData.getPCE_test3_request_54()));

    }

    @After
    public void destroy() {
        pathComputationServiceImpl.close();
    }
}
