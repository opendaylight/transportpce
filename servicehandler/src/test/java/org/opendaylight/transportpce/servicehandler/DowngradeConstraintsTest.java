/*
 * Copyright © 2018 Orange, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.servicehandler;

import static org.junit.Assert.assertEquals;
import static org.opendaylight.transportpce.servicehandler.utils.ConstraintsUtils.buildHardConstraintWithCoRouting;
import static org.opendaylight.transportpce.servicehandler.utils.ConstraintsUtils.buildSoftConstraintWithCoRouting;

import org.junit.Test;
import org.opendaylight.yang.gen.v1.http.org.openroadm.routing.constraints.rev211210.routing.constraints.HardConstraints;
import org.opendaylight.yang.gen.v1.http.org.openroadm.routing.constraints.rev211210.routing.constraints.SoftConstraints;

/**
 * Class to test downgrading and updating Constraints .
 *
 * @author Ahmed Helmy ( ahmad.helmy@orange.com )
 *
 */
public class DowngradeConstraintsTest {



    @Test
    public void testUpdateSoftConstraintsBothCoRouting() {
        HardConstraints initialHardConstraints = buildHardConstraintWithCoRouting();
        SoftConstraints initialSoftConstraints = buildSoftConstraintWithCoRouting();
        SoftConstraints generatedSoftConstraints = DowngradeConstraints.updateSoftConstraints(
                  initialHardConstraints, initialSoftConstraints);
        assertEquals(
                generatedSoftConstraints.getCustomerCode().get(0),
                initialHardConstraints.getCustomerCode().get(0));
    }
}
