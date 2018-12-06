/*
 * Copyright © 2018 Orange Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */


package org.opendaylight.transportpce.pce;

import org.junit.Before;
import org.opendaylight.transportpce.pce.utils.PceTestData;
import org.opendaylight.transportpce.test.AbstractTest;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev170426.PathComputationRequestInput;

public class PceCalculationTest extends AbstractTest {

    private PathComputationRequestInput pathComputationRequestInput;

    @Before
    public void setUp() {
        pathComputationRequestInput = PceTestData.getPCE_test1_request_54();
    }
}
