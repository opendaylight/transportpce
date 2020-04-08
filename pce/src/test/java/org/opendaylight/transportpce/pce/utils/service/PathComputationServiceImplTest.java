/*
 * Copyright © 2020 Orange Labs, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.pce.utils.service;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.transportpce.common.network.NetworkTransactionImpl;
import org.opendaylight.transportpce.common.network.RequestProcessor;
import org.opendaylight.transportpce.pce.service.PathComputationServiceImpl;
import org.opendaylight.transportpce.pce.utils.PceTestData;
import org.opendaylight.transportpce.test.AbstractTest;

public class PathComputationServiceImplTest extends AbstractTest {

    private PathComputationServiceImpl pathComputationServiceImpl;

    @Before
    public void setUp() {
        pathComputationServiceImpl = new PathComputationServiceImpl(
                new NetworkTransactionImpl(new RequestProcessor(this.getDataBroker())),
                this.getNotificationPublishService());
        pathComputationServiceImpl.init();
    }

    @Test
    public void pathComputationRequestTest() {
        Assert.assertNotNull(pathComputationServiceImpl.pathComputationRequest(PceTestData.getPCERequest()));
        pathComputationServiceImpl.close();
    }
}
