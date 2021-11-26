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

//    @Test
//    public void testUpdateSoftConstraintsBothGeneral() {
//        HardConstraints initialHardConstraints = buildHardConstraintWithGeneral();
//        SoftConstraints initialSoftConstraints = buildSoftConstraintWithGeneral();
//        SoftConstraints generatedSoftConstraints = DowngradeConstraints.updateSoftConstraints(
//                initialHardConstraints, initialSoftConstraints);
//
//        assertEquals(
//                generatedSoftConstraints.getCustomerCode().get(0),
//                initialHardConstraints.getCustomerCode().get(0));
//        assertEquals(
//                ((General)generatedSoftConstraints.getCoRoutingOrGeneral())
//                        .getDiversity().getExistingService().get(0),
//                ((General)initialHardConstraints.getCoRoutingOrGeneral())
//                        .getDiversity().getExistingService().get(0));
//
//        assertEquals(
//                ((General)generatedSoftConstraints.getCoRoutingOrGeneral())
//                        .getExclude().getSupportingServiceName().get(0),
//                ((General)initialHardConstraints.getCoRoutingOrGeneral())
//                        .getExclude().getSupportingServiceName().get(0));
//        assertEquals(
//                ((General)generatedSoftConstraints.getCoRoutingOrGeneral())
//                        .getExclude().getNodeId(),
//                ((General)initialHardConstraints.getCoRoutingOrGeneral())
//                        .getExclude().getNodeId());
//        assertEquals(
//                ((General)generatedSoftConstraints.getCoRoutingOrGeneral())
//                        .getInclude().getSupportingServiceName().get(0),
//                ((General)initialHardConstraints.getCoRoutingOrGeneral())
//                        .getInclude().getSupportingServiceName().get(0));
//
//        assertEquals(
//                ((General)generatedSoftConstraints.getCoRoutingOrGeneral())
//                        .getInclude().getNodeId(),
//                ((General)initialHardConstraints.getCoRoutingOrGeneral())
//                        .getInclude().getNodeId());
//    }
//
//    @Test
//    public void testUpdateSoftConstraintsHardGeneralAndSoftCoRouting() {
//        HardConstraints initialHardConstraints = buildHardConstraintWithGeneral();
//        SoftConstraints initialSoftConstraints = buildSoftConstraintWithCoRouting();
//
//
//        SoftConstraints generatedSoftConstraints = DowngradeConstraints.updateSoftConstraints(
//                initialHardConstraints, initialSoftConstraints);
//
//        assertEquals(
//                generatedSoftConstraints.getCustomerCode().get(0),
//                initialHardConstraints.getCustomerCode().get(0));
//        assertTrue(
//                generatedSoftConstraints.getCoRoutingOrGeneral() instanceof  General);
//
//
//    }
//
//
//    @Test
//    public void testUpdateSoftConstraintsHardCoRoutingAndSoftCoGeneral() {
//        HardConstraints initialHardConstraints = buildHardConstraintWithCoRouting();
//        SoftConstraints initialSoftConstraints = buildSoftConstraintWithGeneral();
//
//
//        SoftConstraints generatedSoftConstraints = DowngradeConstraints.updateSoftConstraints(
//                initialHardConstraints, initialSoftConstraints);
//
//        assertEquals(
//                generatedSoftConstraints.getCustomerCode().get(0),
//                initialHardConstraints.getCustomerCode().get(0));
//        assertTrue(
//                generatedSoftConstraints.getCoRoutingOrGeneral() instanceof  CoRouting);
//
//    }
//
//    @Test
//    public void testDowngradeHardConstraintsWithHardGeneralConstraintsSuccess() {
//        HardConstraints initialHardConstraints =
//                buildHardConstraintWithGeneral();
//
//
//        HardConstraints generatedHardConstraints =
//                DowngradeConstraints.downgradeHardConstraints(initialHardConstraints);
//
//        assertTrue(generatedHardConstraints.getCoRoutingOrGeneral() instanceof  General);
//
//        assertNotNull((General)generatedHardConstraints.getCoRoutingOrGeneral());
//
//        assertEquals(
//                ((General) generatedHardConstraints.getCoRoutingOrGeneral()).getLatency().getMaxLatency(),
//                ((General) initialHardConstraints.getCoRoutingOrGeneral()).getLatency().getMaxLatency());
//
//    }
//
//    @Test
//    public void testDowngradeHardConstraintsWithNullGeneralHardConstraints() {
//        HardConstraints initialHardConstraints =
//                buildHardConstraintWithNullGeneral();
//
//        HardConstraints generatedHardConstraints =
//                DowngradeConstraints.downgradeHardConstraints(initialHardConstraints);
//
//        assertNull(generatedHardConstraints.getCoRoutingOrGeneral());
//
//    }
//
//    @Test
//    public void testDowngradeHardConstraintsWithHardCoRoutingConstraints() {
//        HardConstraints initialHardConstraints =
//                buildHardConstraintWithCoRouting();
//
//        HardConstraints generatedHardConstraints =
//                DowngradeConstraints.downgradeHardConstraints(initialHardConstraints);
//
//        assertNull(generatedHardConstraints.getCoRoutingOrGeneral());
//
//    }
//
//
//    @Test
//    public void testConvertToSoftConstraintsFromGeneralHardSuccess() {
//        HardConstraints initialHardConstraints = buildHardConstraintWithGeneral();
//
//
//        SoftConstraints generatedSoftConstraints =
//                DowngradeConstraints.convertToSoftConstraints(initialHardConstraints);
//
//        assertEquals(
//                generatedSoftConstraints.getCustomerCode().get(0),
//                initialHardConstraints.getCustomerCode().get(0));
//        assertTrue(
//                generatedSoftConstraints.getCoRoutingOrGeneral() instanceof  General);
//
//        assertEquals(
//                ((General)generatedSoftConstraints.getCoRoutingOrGeneral())
//                        .getDiversity().getExistingService().get(0),
//                ((General)initialHardConstraints.getCoRoutingOrGeneral())
//                        .getDiversity().getExistingService().get(0));
//
//    }
//
//    @Test
//    public void testConvertToSoftConstraintsFromCoRoutingHardSuccess() {
//        HardConstraints initialHardConstraints = buildHardConstraintWithCoRouting();
//
//
//        SoftConstraints generatedSoftConstraints =
//                DowngradeConstraints.convertToSoftConstraints(initialHardConstraints);
//
//        assertEquals(
//                generatedSoftConstraints.getCustomerCode().get(0),
//                initialHardConstraints.getCustomerCode().get(0));
//        assertTrue(
//                generatedSoftConstraints.getCoRoutingOrGeneral() instanceof  CoRouting);
//
//        assertEquals(
//                ((CoRouting)generatedSoftConstraints.getCoRoutingOrGeneral())
//                        .getCoRouting().getExistingService().get(0),
//                ((CoRouting)initialHardConstraints.getCoRoutingOrGeneral())
//                        .getCoRouting().getExistingService().get(0));
//
//    }
//
//    @Test
//    public void testConvertToSoftConstraintsFromHardNull() {
//        HardConstraints initialHardConstraints = buildHardConstraintWithNullGeneral();
//
//        SoftConstraints generatedSoftConstraints =
//                DowngradeConstraints.convertToSoftConstraints(initialHardConstraints);
//
//        assertNull(generatedSoftConstraints.getCoRoutingOrGeneral());
//
//    }

}
